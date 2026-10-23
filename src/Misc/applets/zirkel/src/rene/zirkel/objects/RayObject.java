package rene.zirkel.objects;

// file: SegmentObject.java

import rene.util.xml.XmlWriter;
import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelCanvas;
import rene.zirkel.construction.Construction;
import rene.zirkel.construction.Count;
import rene.zirkel.graphics.MyGraphics;
import rene.zirkel.graphics.MyGraphics13;
import rene.zirkel.structures.Coordinates;

public class RayObject extends TwoPointLineObject
{	static Count N=new Count();
	public RayObject (Construction c, PointObject p1, PointObject p2)
	{	super(c,p1,p2);
		validate();
		updateText();
	}
	public String getTag () { return "Ray"; }
	public int getN () { return N.next(); }
	
	public void updateText ()
	{	setText(text2(Zirkel.name("text.ray"),P1.getName(),P2.getName()));
	}
	public void validate ()
	{	if (!P1.valid() || !P2.valid()) { Valid=false; return; }
		else
		{	Valid=true;
			X1=P1.getX(); Y1=P1.getY();
			X2=P2.getX(); Y2=P2.getY();
			// compute normalized vector in the direction of the line:
			DX=X2-X1; DY=Y2-Y1; 
			R=Math.sqrt(DX*DX+DY*DY);
			if (R<1e-10) { Valid=false; return; }
			DX/=R; DY/=R;
		}
	}
	
	public void paint (MyGraphics g, ZirkelCanvas zc)
	{	if (!Valid || mustHide(zc)) return;
		//compute middle of the screen:
		double xm=(zc.minX()+zc.maxX())/2,ym=(zc.minY()+zc.maxY())/2;
		// compute distance from middle to line:
		double d=(xm-X1)*DY-(ym-Y1)*DX;
		// compute point with minimal distance
		double x=xm-d*DY,y=ym+d*DX;
		// compute size of the screen
		double a=Math.max(zc.maxX()-zc.minX(),zc.maxY()-zc.minY());
		if (Math.abs(d)>a) return;
		// compute distance from closest point to source
		double b=(x-X1)*DX+(y-Y1)*DY;
		// compute the two visible endpoints
		k1=b-a; k2=b+a; k12valid=true;
		if (k1<0) k1=0;
		if (k1>=k2) return;
		if (Partial && !zc.showHidden() && Dep!=null)
		{	double dd=(zc.maxX()-zc.minX())/20;
			double dmin=-dd,dmax=R+dd;
			for (int i=0; i<NDep; i++)
			{	if (!Dep[i].valid() || Dep[i].mustHide(zc)) continue;
				double s=project(Dep[i].getX(),Dep[i].getY());
				if (s-dd<dmin) dmin=s-dd;
				else if (s+dd>dmax) dmax=s+dd;
			}
			if (k1<dmin) k1=dmin;
			if (k2>dmax) k2=dmax;
		}
		double c1=zc.col(X1+k1*DX),c2=zc.col(X1+k2*DX),
			r1=zc.row(Y1+k1*DY),r2=zc.row(Y1+k2*DY);
		// paint:
		if (isStrongSelected() && g instanceof MyGraphics13)
		{	((MyGraphics13)g).drawMarkerLine(c1,r1,c2,r2);
		}
		g.setColor(this);
		if (visible(zc)) g.drawLine(c1,r1,c2,r2,this);
		String s=getDisplayText();
		if (!s.equals(""))
		{	g.setLabelColor(this);
			DisplaysText=true;
			double c=-b+a/5;
			if (c<-a/5) c=-a/5;
			else if (c>a/5) c=a/5;
			if (c<-b+a/10) c=-b+a/10;
			if (KeepClose)
			{	double side=(YcOffset<0)?1:-1;
				drawLabel(g,s,zc,X1+XcOffset*(X2-X1),Y1+XcOffset*(Y2-Y1),
					side*DX,side*DY,0,0);
			}
			else
				drawLabel(g,s,zc,x+c*DX,y+c*DY,DX,DY,XcOffset,YcOffset);
		}		
	}
	
	public boolean canKeepClose ()
	{	return true;
	}
	
	public void setKeepClose (double x, double y)
	{	KeepClose=true;
		XcOffset=(x-X1)/R*DX+(y-Y1)/R*DY;
		YcOffset=(x-X1)/R*DY-(y-Y1)/R*DX;
	}

	public String getDisplayValue ()
	{	return ""+round(R,ZirkelCanvas.LengthsFactor);
	}

	public boolean nearto (int c, int r, ZirkelCanvas zc)
	{	if (!displays(zc)) return false;
		//compute point at c,r
		double x=zc.x(c),y=zc.y(r);
		// compute distance from line
		double d=(x-X1)*DY-(y-Y1)*DX;
		// compute offset
		double o=(x-X1)*DX+(y-Y1)*DY,o1=(X2-X1)*DX+(Y2-Y1)*DY;
		if (o1>0)
		{	if (o<0) d=Math.sqrt((x-X1)*(x-X1)+(y-Y1)*(y-Y1));
		}
		else
		{	if (o>0) d=Math.sqrt((x-X1)*(x-X1)+(y-Y1)*(y-Y1));
		}
		// test, if on visible part
		double s=project(x,y);
		if (s<k1 || s>k2) return false;
		// scale in screen coordinates
		Value=Math.abs(zc.col(zc.minX()+d)-zc.col(zc.minX()));
		return Value<zc.selectionSize()*2;
	}

	public void printArgs (XmlWriter xml)
	{	xml.printArg("from",P1.getName());
		xml.printArg("to",P2.getName());
		super.printArgs(xml);
	}

	public boolean contains (double x, double y)
	{	double a=(x-X1)*DX+(y-Y1)*DY;
		if (a<1e-9) return false;
		return true;
	}
	public double project (double x, double y)
	{	double h=super.project(x,y);
		if (h<0) return 0;
		return h;
	}

	public boolean equals (ConstructionObject o)
	{	if (!(o instanceof RayObject) || !o.valid()) return false;
		RayObject l=(RayObject)o;
		return equals(X1,l.X1) && equals(Y1,l.Y1) &&
			equals(DX,l.DX) && equals(DY,l.DY);
	}

	public static Coordinates intersect 
		(PrimitiveLineObject l1, PrimitiveLineObject l2)
	// compute the intersection coordinates of two lines
	{	double det=-l1.DX*l2.DY+l1.DY*l2.DX;
		if (Math.abs(det)<1e-10) return null;
		double a=(-(l2.X1-l1.X1)*l2.DY+(l2.Y1-l1.Y1)*l2.DX)/det;
		return new Coordinates(l1.X1+a*l1.DX,l1.Y1+a*l1.DY);
	}

	public static Coordinates intersect
		(PrimitiveLineObject l, PrimitiveCircleObject c)
	// compute the intersection coordinates of a line with a circle
	{	double x=c.getX(),y=c.getY(),r=c.getR();
		double d=(x-l.X1)*l.DY-(y-l.Y1)*l.DX;
		if (Math.abs(d)>r+1e-10) return null;
		x-=d*l.DY; y+=d*l.DX;
		double h=r*r-d*d;
		if (h>0) h=Math.sqrt(h);
		else h=0;
		return new Coordinates(x+h*l.DX,y+h*l.DY,x-h*l.DX,y-h*l.DY);
	}

}
