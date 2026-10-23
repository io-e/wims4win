package rene.zirkel.objects;

// file: LineObject.java

import java.awt.*;
import java.util.*;

import rene.gui.*;
import rene.util.xml.*;
import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelCanvas;
import rene.zirkel.construction.Construction;
import rene.zirkel.construction.ConstructionException;
import rene.zirkel.construction.Count;
import rene.zirkel.dialogs.EditConditionals;
import rene.zirkel.dialogs.ObjectEditDialog;
import rene.zirkel.expression.InvalidException;
import rene.zirkel.graphics.MyGraphics;
import rene.zirkel.graphics.MyGraphics13;

class AreaObjectDialog extends ObjectEditDialog
{	
	public AreaObjectDialog (Frame f, String title, ConstructionObject o)
	{	super(f, title, o);
	}
	public void addFirst (Panel P)
	{	AreaObject AO=(AreaObject)O;
		
		MyTextField A=new MyTextField(""+AO.area(),30);
		P.add(new MyLabel(Zirkel.name("edit.area.area"))); P.add(A);
		A.setEditable(false);
	}
	
	
}

public class AreaObject extends ConstructionObject
	implements InsideObject, PointonObject, MoveableObject
{	static Count N=new Count();
	double x[]=new double[3],y[]=new double[3];
	double R,X,Y,A;
	Vector V;

	public AreaObject (Construction c, Vector v)
	{	super(c);
		V=v;
		validate();
		updateText();
		Global.getParameter("unit.area","");
	}

	public String getTag () { return "Polygon"; }

	public void updateText()
	{	String Text=Zirkel.name("text.area");
		Enumeration en=V.elements();
		boolean first=true;
		while (en.hasMoreElements())
		{	PointObject p=(PointObject)en.nextElement();
			if (!first) Text=Text+", ";
			else Text=Text+" ";
			first=false;
			Text=Text+p.getName();
		}
		setText(Text);
	}
	
	public void validate ()
	{	Enumeration e=V.elements();
		while (e.hasMoreElements())
		{	if (!((ConstructionObject)e.nextElement()).valid())
			{	Valid=false; return;
			}
		}
		if (V.size()<3)
		{	Valid=false;
			return;
		}
		Enumeration en=V.elements();
		double x=0,y=0;
		while (en.hasMoreElements())
		{	PointObject p=(PointObject)en.nextElement();
			x+=p.getX();
			y+=p.getY();
		}
		X=x/V.size(); Y=y/V.size();
		A=area();
		Valid=true;
	}

	public void edit (ZirkelCanvas zc)
	{	ObjectEditDialog d=new AreaObjectDialog(zc.getFrame(),Zirkel.name("edit.area.title"),
			this);
		d.setVisible(true);
		Global.setParameter("unit.area",Unit);
		zc.repaint();
		if (d.wantsMore())
		{	new EditConditionals(zc.getFrame(),this);
			validate();
		}
	}
	
	public void paint (MyGraphics g, ZirkelCanvas zc)
	{	if (!Valid || mustHide(zc)) return;
		int n=V.size();
		if (x.length!=n)
		{	x=new double[n]; y=new double[n];
		}
		if (visible(zc))
		{	Enumeration e=V.elements();
			int i=0;
			while (e.hasMoreElements())
			{	PointObject p=(PointObject)e.nextElement();
				x[i]=zc.col(p.getX());
				y[i]=zc.row(p.getY());
				if (i>0)
				{	if (isStrongSelected() && g instanceof MyGraphics13)
					{	((MyGraphics13)g).drawMarkerLine(x[i-1],y[i-1],x[i],y[i]);
					}
				}
				i++;
			}
			if (i>1)
			{	if (isStrongSelected() && g instanceof MyGraphics13)
				{	((MyGraphics13)g).drawMarkerLine(x[i-1],y[i-1],x[0],y[0]);
				}
			}	
			g.fillPolygon(x,y,n,Indicated||Selected||getColorType()!=THIN,
				getColorType()!=THICK,this);
		}
		String s=getDisplayText();
		if (!s.equals(""))
		{	if (getColorIndex()==0 && getColorType()==THICK)
				g.setColor(Color.gray.brighter());
			else g.setColor(Color.black);
			DisplaysText=true;
			TX1=zc.col(X+XcOffset);
			TY1=zc.row(Y+YcOffset);
			setFont(g);
			drawLabel(g,s);
		}		
	}

	public String getDisplayValue ()
	{	return ""+round(Math.abs(A),ZirkelCanvas.LengthsFactor);
	}
	
	public void printArgs (XmlWriter xml)
	{	Enumeration e=V.elements();
		int n=1;
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			xml.printArg("point"+n,o.getName());
			n++;
		}
		super.printArgs(xml);
	}

	public Enumeration depending ()
	{	super.depending();
		Enumeration e=V.elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			DL.add(o);
		}
		return DL.elements();
	}

	public void translate ()
	{	Enumeration e=V.elements();
		Vector w=new Vector();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			w.addElement(o.getTranslation());
		}
		V=w;
	}
	
	public boolean nearto (int c, int r, ZirkelCanvas zc)
	{	if (!displays(zc)) return false;
		return contains(zc.x(c),zc.y(r));
	}
	
	public boolean nearto (int c, int r, boolean ignorefill, ZirkelCanvas zc)
	{	if (!displays(zc)) return false;
		if (contains(zc.x(c),zc.y(r),zc.dx(zc.selectionSize())))
		{	if (ignorefill) return OnBoundary;
			else return true; 
		}
		return false;
	}
	
	boolean OnBoundary;
	
	public boolean contains (double x, double y, double eps)
	{	if (!Valid) return false;
		OnBoundary=false;
		PointObject First=(PointObject)V.elementAt(0);
		PointObject P=First;
		if (Math.max(Math.abs(P.getX()-x),Math.abs(P.getY()-y))<eps)
		{	OnBoundary=true;
			return true;
		}
		Enumeration e=V.elements();
		double a=Math.atan2(P.getX()-x,P.getY()-y);
		double sum=0;
		PointObject Q;
		while (e.hasMoreElements())
		{	Q=(PointObject)e.nextElement();
			if (Math.max(Math.abs(Q.getX()-x),Math.abs(Q.getY()-y))<eps)
			{	OnBoundary=true;
				return true;
			}
			double b=Math.atan2(Q.getX()-x,Q.getY()-y);
			double d=b-a;
			if (d>Math.PI) d=d-2*Math.PI;
			else if (d<-Math.PI) d=d+2*Math.PI;
			if (Math.abs(Math.abs(d)-Math.PI)<0.1)
			{	OnBoundary=true;
				return true;
			}
			a=b; P=Q;
			sum+=d;
		}
		Q=First;
		double b=Math.atan2(Q.getX()-x,Q.getY()-y);
		double d=b-a;
		if (d>Math.PI) d=d-2*Math.PI;
		else if (d<-Math.PI) d=d+2*Math.PI;
		if (Math.abs(Math.abs(d)-Math.PI)<0.1)
		{	OnBoundary=true;
			return true;
		}
		sum+=d;
		return Math.abs(sum)>=Math.PI/2;
	}
	
	public boolean contains (double x, double y)
	{	return contains(x,y,1e-4);
	}

	public double area ()
	{	if (!Valid) return -1;
		PointObject First=(PointObject)V.elementAt(0);
		PointObject P=First;
		Enumeration e=V.elements();
		double sum=0;
		PointObject Q;
		while (e.hasMoreElements())
		{	Q=(PointObject)e.nextElement();
			sum+=(Q.getX()-X)*(P.getY()-Y)-(Q.getY()-Y)*(P.getX()-X);
			P=Q;
		}
		Q=First;
		sum+=(Q.getX()-X)*(P.getY()-Y)-(Q.getY()-Y)*(P.getX()-X);
		return sum/2;
	}

	public boolean equals (ConstructionObject o)
	{	if (!(o instanceof AreaObject) || !o.valid()) return false;
		AreaObject a=(AreaObject)o;
		int n=V.size(),m=a.V.size();
		PointObject p[]=new PointObject[n];
		V.copyInto(p);
		PointObject pa[]=new PointObject[m];
		a.V.copyInto(pa);
		double x0=0,y0=0;
		for (int i=0; i<m; i++)
		{	boolean r=true;
			int j=0,kj=0;
			while (true)
			{	int k=i+kj;
				if (k>=m) k-=m;
				if (!p[j].equals(pa[k]))
				{	if (j==0 || !between(x0,y0,p[j].getX(),p[j].getY(),
						pa[k].getX(),pa[k].getY()))
					{	r=false; break;
					}
				}
				else
				{	x0=p[j].getX(); y0=p[j].getY();
					j++;
				}
				kj++;
				if (j>=n || kj>=m) break;
			}
			if (r && kj>=m) return true;
		}
		for (int i=0; i<m; i++)
		{	boolean r=true;
			int j=0,kj=0;
			while (true)
			{	int k=i+kj;
				if (k>=m) k-=m;
				if (!p[n-j-1].equals(pa[k]))
				{	if (j==0 || !between(x0,y0,p[n-j-1].getX(),p[n-j-1].getY(),
						pa[k].getX(),pa[k].getY()))
					{	r=false; break;
					}
				}
				else
				{	x0=p[n-j-1].getX(); y0=p[n-j-1].getY();
					j++;
				}
				kj++;
				if (j>=n || kj>=m) break;
			}
			if (r && kj>=m) return true;
		}
		return false;
	}

	public boolean between (double x0, double y0, double x1, double y1, 
		double x, double y)
	{	double lambda;
		if (Math.abs(x1-x0)>Math.abs(y1-y0))
			lambda=(x-x0)/(x1-x0);
		else
			lambda=(y-y0)/(y1-y0);
		return
			Math.abs(x0+lambda*(x1-x0)-x)<1e-10 &&
			Math.abs(y0+lambda*(y1-y0)-y)<1e-10;
	}

	public double getValue ()
		throws ConstructionException
	{	if (!Valid) throw new InvalidException("exception.invalid");
		else return A;
	}	
	
	public boolean maybeTransparent ()
	{	return true;
	}
	
	public boolean isFilled ()
	{	return true;
	}

	public boolean onlynearto (int x, int y, ZirkelCanvas zc)
	{	return false;
	}

	public void project (PointObject P) 
	{	double x=P.getX(),y=P.getY();
		Enumeration e=V.elements();
		PointObject p=(PointObject)e.nextElement();
		double x1=p.getX(),y1=p.getY();
		double xstart=x1,ystart=y1;
		int count=0;
		double xmin=x1,ymin=y1,dmin=1e20,hmin=0;
		while (e.hasMoreElements())
		{	p=(PointObject)e.nextElement();
			double x2=p.getX(),y2=p.getY();
			double dx=x2-x1,dy=y2-y1;
			double r=dx*dx+dy*dy;
			if (r>1e-5)
			{	double h=dx*(x-x1)/r+dy*(y-y1)/r;
				if (h>1) h=1;
				else if (h<0) h=0;
				double xh=x1+h*dx,yh=y1+h*dy;
				double dist=Math.sqrt((x-xh)*(x-xh)+(y-yh)*(y-yh));
				if (dist<dmin)
				{	dmin=dist;
					xmin=xh; ymin=yh;
					hmin=count+h;
				}
			}
			count++;
			x1=x2; y1=y2;
		}
		double x2=xstart,y2=ystart;
		double dx=x2-x1,dy=y2-y1;
		double r=dx*dx+dy*dy;
		if (r>1e-5)
		{	double h=dx*(x-x1)/r+dy*(y-y1)/r;
			if (h>1) h=1;
			else if (h<0) h=0;
			double xh=x1+h*dx,yh=y1+h*dy;
			double dist=Math.sqrt((x-xh)*(x-xh)+(y-yh)*(y-yh));
			if (dist<dmin)
			{	dmin=dist;
				xmin=xh; ymin=yh;
				hmin=count+h;
			}
		}
		P.move(xmin,ymin);
		P.setA(hmin);
	}

	public void project (PointObject P, double alpha) 
	{	int i=(int)Math.floor(alpha);
		double h=alpha-i;
		if (i<0 || i>=V.size())
		{	project(P); return;
		}
		PointObject P1=(PointObject)V.elementAt(i);
		PointObject P2;
		if (i==V.size()-1) P2=(PointObject)V.elementAt(0);
		else P2=(PointObject)V.elementAt(i+1);
		P.setXY(P1.getX()+h*(P2.getX()-P1.getX()),P1.getY()+h*(P2.getY()-P1.getY()));
	}
	
	public double containsInside(PointObject P) 
	{	boolean inside=contains(P.getX(),P.getY());
		if (inside && OnBoundary) return 0.5;
		else if (inside) return 1;
		return 0;
	}

	public boolean keepInside (PointObject P) 
	{	if (containsInside(P)>0) return true;
		project(P);
		return false;
	}

	public void dragTo (double x, double y) 
	{	Enumeration e=V.elements();
		int i=0;
		while (e.hasMoreElements())
		{	PointObject p=(PointObject)e.nextElement();
			p.move(xd[i]+(x-x1),yd[i]+(y-y1));
			i++;
		}
	}

	public void move (double x, double y) 
	{
	}

	public boolean moveable () 
	{	if (V==null) return false;
		Enumeration e=V.elements();
		while (e.hasMoreElements())
		{	if (!((PointObject)e.nextElement()).moveable())
				return false;
		}
		return true;
	}
	
	double xd[],yd[],x1,y1;

	public void startDrag (double x, double y) 
	{	if (xd==null || xd.length!=V.size())
		{	xd=new double[V.size()];
			yd=new double[V.size()];
		}
		Enumeration e=V.elements();
		int i=0;
		while (e.hasMoreElements())
		{	PointObject p=(PointObject)e.nextElement();
			xd[i]=p.getX(); yd[i]=p.getY();
			i++;
		}
		x1=x; y1=y;
	}

	public void snap (ZirkelCanvas zc)
	{	if (moveable())
		{	Enumeration e=V.elements();
			while (e.hasMoreElements())
			{	PointObject p=(PointObject)e.nextElement();
				p.snap(zc);
			}
		}
	}

	public boolean canInteresectWith (ConstructionObject o) 
	{	if (o instanceof PointonObject)
		{	ConstructionObject line=(ConstructionObject)o;
			Enumeration e=V.elements();
			{	PointObject P=(PointObject)e.nextElement();
				if (line.contains(P)) return false;
			}
		}
		return true;
	}

}
