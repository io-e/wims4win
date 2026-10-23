package rene.zirkel.objects;

// file: SegmentObject.java

import java.util.Enumeration;

import rene.zirkel.ZirkelCanvas;
import rene.zirkel.construction.Construction;


public class TwoPointLineObject extends PrimitiveLineObject
	implements MoveableObject
{	protected PointObject P2;
	double X2,Y2,R;

	public TwoPointLineObject (Construction c, PointObject p1, PointObject p2)
	{	super(c);
		P1=p1; P2=p2;
	}
	
	public PointObject getP2 () { return P2; }

	public Enumeration depending ()
	{	super.depending();
		return depset(P1,P2);
	}
	
	public double getLength () { return R; }

	public void translate ()
	{	P1=(PointObject)P1.getTranslation();
		P2=(PointObject)P2.getTranslation();
	}
	
	public boolean contains (PointObject p)
	{	return P1==p || P2==p;
	}

	public Enumeration secondaryParams ()
	{	DL.reset();
		return depset(P1,P2);
	}

	public void dragTo (double x, double y) 
	{	P1.move(x1+(x-x3),y1+(y-y3));
		P2.move(x2+(x-x3),y2+(y-y3));
	}

	public void move (double x, double y) 
	{
	}

	public boolean moveable () 
	{	if (P1.moveable() && P2.moveable()) return true;
		return false;
	}

	double x1,y1,x2,y2,x3,y3;
	
	public void startDrag (double x, double y) 
	{	x1=P1.getX(); y1=P1.getY();
		x2=P2.getX(); y2=P2.getY();
		x3=x; y3=y;
	}

	public void snap (ZirkelCanvas zc)
	{	if (moveable())
		{	P1.snap(zc); P2.snap(zc);
		}
	}
}
