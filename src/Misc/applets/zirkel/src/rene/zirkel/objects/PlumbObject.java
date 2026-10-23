package rene.zirkel.objects;

// file: PlumbObject.java

import java.util.Enumeration;

import rene.util.xml.XmlWriter;
import rene.zirkel.*;
import rene.zirkel.construction.*;

public class PlumbObject extends PrimitiveLineObject
	implements MoveableObject
{	PrimitiveLineObject L;
	static Count N=new Count();
	protected boolean Restricted=false;
	public PlumbObject (Construction c, PrimitiveLineObject l, PointObject p)
	{	super(c);
		P1=p; L=l;
		validate();
		updateText();
	}
	public String getTag () { return "Plumb"; }
	public int getN () { return N.next(); }
	
	public void updateText()
	{	setText(text2(Zirkel.name("text.plumb"),P1.getName(),L.getName()));
	}
	public void validate ()
	{	if (!P1.valid() || !L.valid()) Valid=false;
		else
		{	Valid=true;
			X1=P1.getX(); Y1=P1.getY();
			DX=-L.getDY(); DY=L.getDX();
			if (L instanceof SegmentObject &&
				((SegmentObject)L).getLength()==0)
			{	Valid=false;
			}
			if (Restricted)
			{	if (!L.contains(X1,Y1)) Valid=false;
			}
		}
	}
	public void printArgs (XmlWriter xml)
	{	xml.printArg("point",P1.getName());
		xml.printArg("line",L.getName());
		if (!Restricted) xml.printArg("valid","true");
		super.printArgs(xml);
	}	
	public boolean isRestricted () { return Restricted; }
	public void setRestricted (boolean flag) { Restricted=flag; }
	public boolean canRestrict ()
	{	return (L instanceof SegmentObject) ||
			(L instanceof RayObject);
	}
	public void setMainParameter ()
	{	MainParameter=true;
	}	
	public Enumeration depending ()
	{	super.depending();
		return depset(P1,L);
	}
	public void translate ()
	{	P1=(PointObject)P1.getTranslation();
		L=(PrimitiveLineObject)L.getTranslation();
	}
	public boolean contains (PointObject p)
	{	return p==P1;
	}
	public boolean hasUnit ()
	{	return false;
	}
	public void dragTo (double x, double y) 
	{	P1.move(x1+(x-x3),y1+(y-y3));
	}

	public void move (double x, double y) 
	{
	}

	public boolean moveable () 
	{	if (P1.moveable()) return true;
		return false;
	}

	double x1,y1,x2,y2,x3,y3;
	
	public void startDrag (double x, double y) 
	{	x1=P1.getX(); y1=P1.getY();
		x3=x; y3=y;
	}

	public void snap (ZirkelCanvas zc)
	{	if (moveable())
		{	P1.snap(zc);
		}
	}
}
