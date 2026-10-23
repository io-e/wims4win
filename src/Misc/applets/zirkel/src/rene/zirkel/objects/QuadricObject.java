package rene.zirkel.objects;

// file: QuadricObject.java

import java.util.Enumeration;

import rene.util.xml.XmlWriter;
import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelCanvas;
import rene.zirkel.construction.Construction;
import rene.zirkel.construction.Count;
import rene.zirkel.dialogs.EditConditionals;
import rene.zirkel.dialogs.ObjectEditDialog;
import rene.zirkel.expression.Quartic;
import rene.zirkel.graphics.MyGraphics;
import rene.zirkel.graphics.PolygonDrawer;

public class QuadricObject extends ConstructionObject
	implements PointonObject, MoveableObject
{	PointObject P[];
	static Count N=new Count();
	double X[];
	
	public QuadricObject (Construction c, PointObject p[])
	{	super(c);
		P=p;
		validate();
		updateText();
	}

	public String getTag () { return "Quadric"; }
	public int getN () { return N.next(); }
	
	public void updateText ()
	{	try
		{	String Names[]=new String[P.length];
			for (int i=0; i<P.length; i++) Names[i]=P[i].getName();
			setText(textAny(Zirkel.name("text.quadric"),Names));
		} 
		catch (Exception e) {}
	}
	
	public void validate ()
	{	for (int i=0; i<P.length; i++)
			if (!P[i].valid())
			{	Valid=false; return;
			}
		Valid=true;
		
		// Baue Koeffizientenmatrix auf (x^2,y^2,x,y,xy,1):
		double A[][]=new double[5][6];
		for (int i=0; i<5; i++)
		{	double x=P[i].getX(),y=P[i].getY();
			A[i][0]=x*x; A[i][1]=y*y;
			A[i][2]=x; A[i][3]=y;
			A[i][4]=x*y; A[i][5]=1;
			double sum=0;
			for (int j=0; j<6; j++) sum+=A[i][j]*A[i][j];
			sum=Math.sqrt(sum);
			for (int j=0; j<6; j++) A[i][j]/=sum;
		}
		
		// Gaußverfahren, um auf untere Dreiecksmatrix zu kommen
		int r=0;
		int colindex[]=new int[6]; // Index der Stufe oder -1 (keine Stufe)
		// Iteration über alle Spalten:
		for (int c=0; c<6; c++)
		{	if (r>=5) // Schema schon fertig
			{	colindex[c]=-1; continue;
			}
			// Berechne Pivotelement mit spaltenweiser Maximumssuche
			double max=Math.abs(A[r][c]);
			int imax=r;
			for (int i=r+1; i<5; i++)
			{	double h=Math.abs(A[i][c]);
				if (h>max)
				{	max=h; imax=i;
				}
			}
			if (max>1e-13)
			{	// Vertausche Zeilen:
				if (imax!=r)
				{	double h[]=A[imax];
					A[imax]=A[r];
					A[r]=h;
				}
				// Mache restliche Spalte zu 0:
				for (int i=r+1; i<5; i++)
				{	double lambda=A[i][c]/A[r][c];
					for (int j=c+1; j<6; j++)
						A[i][j]-=lambda*A[r][j];
				}
				colindex[c]=r;
				r++;
			}
			else
			{	colindex[c]=-1;
			}
		}
		// Berechne die x-Werte:
		X=new double[6];
		for (int j=5; j>=0; j--)
		{	if (colindex[j]<0)
			{	X[j]=1;
			}
			else
			{	double h=0;
				int i=colindex[j];
				for (int k=j+1; k<6; k++)
					h+=A[i][k]*X[k];
				X[j]=-h/A[i][j];
			}
		}
		// Normalisiere
		double sum=0;
		for (int i=0; i<=5; i++) sum+=Math.abs(X[i]);
		if (sum<1e-10) Valid=false;
		for (int i=0; i<=5; i++) X[i]/=sum;
	}
	
	public void paint (MyGraphics g, ZirkelCanvas zc)
	{	if (!Valid || mustHide(zc)) return;
		g.setColor(this);
		// Draw the lower part of the quadrik (minus the root):
		double start=zc.minX(),x=start;
		double end=zc.maxX();
		double h=zc.dx(1);
		boolean valid=false,ptext=false;
		int c0=0,r0=0,ctext=20,rtext=20;
		PolygonDrawer pd=new PolygonDrawer(g,this);
		// Draw the lower part of the quadric (plus the root):
		while (x<=end)
		{	try
			{	double y=computeLower(x);
				int c=(int)zc.col(x),r=(int)zc.row(y);
				if (valid)
				{	pd.drawTo(c,r);
					if (!ptext && r0-r>c-c0 && zc.isInside(x,y))
					{	ctext=c; rtext=r; ptext=true; 
					}
				}
				else pd.startPolygon(c,r);
				c0=c; r0=r;
				valid=true;
			}
			catch (RuntimeException e)
			{	valid=false;
			}
			x+=h;
		}
		pd.finishPolygon();
		// Draw the upper part of the quadric (plus the root):
		x=start-2*h;
		valid=false;
		while (x<=end+2*h)
		{	try
			{	double y=computeUpper(x);
				int c=(int)zc.col(x),r=(int)zc.row(y);
				if (valid)
				{	pd.drawTo(c,r);
					// Try to find a position for the label:
					if (!ptext && r0-r>c-c0 && zc.isInside(x,y))
					{	ctext=c; rtext=r; ptext=true; 
					}
				}
				else // left edge of quadric, connect with lower part
				{	try
					{	double y1=computeLower(x);
						if (x>=start-h && x<=end+h)
							g.drawLine(c,zc.row(y1),c,r,this);
					}
					catch (RuntimeException e) {}
					pd.startPolygon(c,r);
				}
				c0=c; r0=r;
				valid=true;
			}
			catch (RuntimeException e) // no points in that range
			{	if (valid) // we just left the right edge of the quadric
				{	try
					{	double y1=computeLower(x-h);
						if (x-h>=start-h && x-h<=end+h)
							g.drawLine(c0,zc.row(y1),c0,r0,this);
					}
					catch (RuntimeException ex) {}
				}
				valid=false;
			}
			x+=h;
		}
		pd.finishPolygon();
		String s=getDisplayText();
		if (!s.equals(""))
		{	g.setLabelColor(this);
			setFont(g);
			DisplaysText=true;
			TX1=ctext+zc.col(XcOffset)-zc.col(0);
			TY1=rtext+zc.row(YcOffset)-zc.row(0);
			drawLabel(g,s);
		}
	}
	
	static public String Tags[]={"x^2","y^2","x","y","xy"};
	
	public String getDisplayValue ()
	{	String s="";
		for (int i=0; i<5; i++)
			s=s+helpDisplayValue(i==0,-X[i],Tags[i]);
		return s+"="+roundDisplay(X[5]);
	}
	
	public String getEquation ()
	{	return getDisplayValue();
	}
	
	public boolean nearto (int cc, int rr, ZirkelCanvas zc)
	{	if (!displays(zc)) return false;
		int size=(int)zc.selectionSize();
		double start=zc.minX(),x=start;
		double end=zc.maxX();
		double h=zc.dx(1);
		while (x<=end)
		{	try
			{	double y=computeUpper(x);
				double c=zc.col(x),r=zc.row(y);
				if (Math.abs(cc-c)<=size*3/2 && Math.abs(rr-r)<=size*3/2)
					return true;
			}
			catch (Exception e) {}
			try
			{	double y=computeLower(x);
				double c=zc.col(x),r=zc.row(y);
				if (Math.abs(cc-c)<=size*3/2 && Math.abs(rr-r)<=size*3/2)
					return true;
			}
			catch (Exception e) {}
			x+=h;
		}
		return false;
	}
		
	public void edit (ZirkelCanvas zc)
	{	ObjectEditDialog d=new ObjectEditDialog(zc.getFrame(),"",this);
		d.setVisible(true);
		zc.repaint();
		if (d.wantsMore())
		{	new EditConditionals(zc.getFrame(),this);
			validate();
		}
	}

	double computeUpper (double x)
	{	if (Math.abs(X[1])>1e-13)
		{	double p=(X[3]+x*X[4])/X[1],q=(X[0]*x*x+X[2]*x+X[5])/X[1];
			double h=p*p/4-q;
			if (h<0) throw new RuntimeException("");
			return -p/2+Math.sqrt(h);
		}
		else
		{	return -(X[0]*x*x+X[2]*x+X[5])/(X[3]+X[4]*x);
		}
	}
	
	double computeLower (double x)
	{	if (Math.abs(X[1])>1e-13)
		{	double p=(X[3]+x*X[4])/X[1],q=(X[0]*x*x+X[2]*x+X[5])/X[1];
			double h=p*p/4-q;
			if (h<0) throw new RuntimeException("");
			return -p/2-Math.sqrt(h);
		}
		else throw new RuntimeException("");
	}
	
	public void printArgs (XmlWriter xml)
	{	for (int i=0; i<P.length; i++)
			xml.printArg("point"+(i+1),P[i].getName());
	}

	public Enumeration depending ()
	{	DL.reset();
		for (int i=0; i<P.length; i++) DL.add(P[i]);
		return DL.elements();
	}

	public void translate ()
	{	for (int i=0; i<P.length; i++)
			P[i]=(PointObject)P[i].getTranslation();
	}

	public ConstructionObject copy ()
	{	try
		{	QuadricObject o=(QuadricObject)clone();
			setTranslation(o);
			o.P=new PointObject[P.length];
			for (int i=0; i<P.length; i++) o.P[i]=P[i];
			o.translateConditionals();
			o.translate();
			o.setName();
			o.updateText();
			o.setBreak(false);
			o.setTarget(false);
			return o;
		}
		catch (Exception e)
		{	return null; 
		}
	}

	public boolean onlynearto (int x, int y, ZirkelCanvas zc)
	{	return false;
	}

	public boolean equals (ConstructionObject o)
	{	if (!(o instanceof QuadricObject) || !o.valid()) return false;
		try
		{	for (int i=0; i<6; i++)
			{	if (!equals(X[i],((QuadricObject)o).X[i]))
					return false;
			}
		}
		catch (RuntimeException e)
		{	return false;
		}
		return true;
	}
	
	public boolean hasUnit ()
	{	return false;
	}

	public void project (PointObject P) 
	{	double a=X[0],b=X[1],c=X[2],d=X[3],e=X[4],r=X[5];
		double xc=P.getX(),yc=P.getY();
		if (Math.abs(a*xc*xc+b*yc*yc+c*xc+d*yc+e*xc*yc+r)<1e-13) // close enough
			return;
		double t[]=new double[5],s[]=new double[5],si[]=new double[5];
		// Coefficients for fourth order polynomial for lambda (Lagrange factor)
		// Minimize (x-xc)^2+(y-yc)^2 with a*x^2+b*y^2+c*x+d*y+e*x*y+r=0
		// Computed with Maple
		t[0] = a*e*e*d*d-4*a*b*b*c*c+4*a*e*d*b*c-4*b*a*a*d*d+b*c*c*e*e
			-c*Math.pow(e,3)*d+r*Math.pow(e,4)-8*r*e*e*b*a+16*r*b*b*a*a;
		t[1] = 8*b*b*c*c+8*a*a*d*d-8*e*d*b*c-8*a*d*c*e+8*r*e*e*b+8*a*b*c*c
			+8*b*a*d*d+8*r*e*e*a-32*r*b*b*a-32*r*b*a*a;
		t[2] = 12*e*d*c+16*r*b*b-4*b*d*d-8*r*e*e+4*e*e*d*yc+16*b*b*xc*c-16*b*c*c
			-16*a*d*d-4*a*c*c+16*r*a*a+16*a*a*d*yc+4*xc*e*e*c-8*e*d*b*xc-8*e*yc*b*c
			-8*a*d*xc*e-8*a*yc*c*e+16*a*b*b*xc*xc-4*a*e*e*yc*yc+16*b*a*a*yc*yc
			-4*b*xc*xc*e*e+4*Math.pow(e,3)*yc*xc+64*r*b*a-16*a*b*xc*e*yc;
		t[3] = -32*r*b+8*d*d+8*c*c+16*e*d*xc+8*e*e*yc*yc+8*xc*xc*e*e-32*r*a
			-32*b*xc*c+16*e*yc*c-32*a*d*yc-32*a*b*xc*xc-32*b*a*yc*yc;
		t[4] = 16*b*yc*yc+16*d*yc+16*c*xc+16*xc*e*yc+16*r+16*a*xc*xc;
		int k=Quartic.solve(t,s);
		// System.out.println(k+"Solutions found.");
		double dmin=1e30,xmin=xc,ymin=yc;
		for (int i=0; i<k; i++) // Choose closest solution of Lagrange equation
		{	double l=s[i];
			// Solve for x,y when lambda is known.
			// Computed with Maple
			double px = -(-e*d+4*b*l*xc-2*e*l*yc-4*l*l*xc+2*b*c-2*l*c)/(-e*e+4*b*a-4*b*l-4*l*a+4*l*l);
			double py = -(2*a*d+4*a*l*yc-2*l*d-4*l*l*yc-2*l*xc*e-c*e)/(-e*e+4*b*a-4*b*l-4*l*a+4*l*l);
			double dist=(px-xc)*(px-xc)+(py-yc)*(py-yc);
			if (dist<dmin)
			{	dmin=dist; xmin=px; ymin=py;
			}
		}
		P.move(xmin,ymin);
	}

	public void project (PointObject P, double alpha) 
	{	project(P);
	}

	public void dragTo (double x, double y) 
	{	for (int i=0; i<5; i++)
		{	P[i].move(xd[i]+(x-x1),yd[i]+(y-y1));
		}
	}

	public void move (double x, double y) 
	{
	}

	public boolean moveable () 
	{	for (int i=0; i<5; i++)
		{	if (!P[i].moveable())
				return false;
		}
		return true;
	}
	
	double xd[],yd[],x1,y1;

	public void startDrag (double x, double y) 
	{	if (xd==null)
		{	xd=new double[5];
			yd=new double[5];
		}
		for (int i=0; i<5; i++)
		{	xd[i]=P[i].getX(); yd[i]=P[i].getY();
		}
		x1=x; y1=y;
	}

	public void snap (ZirkelCanvas zc)
	{	if (moveable())
		{	for (int i=0; i<5; i++)
			{	P[i].snap(zc);
			}
		}
	}

	public boolean canInteresectWith(ConstructionObject o) 
	{	return true;
	}


}
