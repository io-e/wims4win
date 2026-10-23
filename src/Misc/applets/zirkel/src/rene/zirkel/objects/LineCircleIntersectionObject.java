package rene.zirkel.objects;

// file: LineCircleIntersectionObject.java

import rene.util.xml.*;
import rene.zirkel.construction.Construction;
import rene.zirkel.structures.Coordinates;


public class LineCircleIntersectionObject extends IntersectionObject
{	public LineCircleIntersectionObject (Construction c,
		PrimitiveLineObject P1, PrimitiveCircleObject P2,
		boolean first)
	{	super(c,P1,P2);
		First=first;
		validate();
	}

	public void updateCircleDep ()
	{	((PrimitiveCircleObject)P2).addDep(this);
		((PrimitiveLineObject)P1).addDep(this);
	}

	public void validate ()
	{	boolean oldvalid=Valid;
		if (!P1.valid() || !P2.valid()) Valid=false;
		else Valid=true; 
		if (!Valid) return;
		Coordinates c=PrimitiveLineObject.intersect(
			(PrimitiveLineObject)P1,(PrimitiveCircleObject)P2);
		if (c==null)
		{	if (oldvalid && getConstruction().shouldSwitch())
			{	doSwitch();
				if (!getConstruction().noteSwitch()) Switched=false;
			}
			else if (oldvalid && Alternate && Away==null && getConstruction().canAlternate())
			{	First=!First;
			}
			Valid=false; return; 
		}
		double X1,Y1;
		if (getAway()!=null)
		{	double x=getAway().getX(),y=getAway().getY();
			double r=(x-c.X)*(x-c.X)+(y-c.Y)*(y-c.Y);
			double r1=(x-c.X1)*(x-c.X1)+(y-c.Y1)*(y-c.Y1);
			boolean flag=(r>r1);
			if (!StayAway) flag=!flag;
			if (flag)
			{	X=c.X; Y=c.Y;
				X1=c.X1; Y1=c.Y1;
			}
			else
			{	X=c.X1; Y=c.Y1;
				X1=c.X; Y1=c.Y;
			}
		}
		else
		{	if (First) { X=c.X; Y=c.Y; X1=c.X1; Y1=c.Y1; }
			else { X=c.X1; Y=c.Y1; X1=c.X; Y1=c.Y; }
		}
		if (Restricted)
		{	if (!((PrimitiveLineObject)P1).contains(X,Y)) Valid=false;
			if (!(((PrimitiveCircleObject)P2).getStart()==this ||
					((PrimitiveCircleObject)P2).getEnd()==this) &&
				!((PrimitiveCircleObject)P2).contains(X,Y)) Valid=false;
		}
	}
	
	public void printArgs (XmlWriter xml)
	{	super.printArgs(xml);
		if (First) xml.printArg("which","first");
		else xml.printArg("which","second");
	}

	public boolean isSwitchable ()
	{	return true;
	}

	public boolean canAlternate ()
	{	return true;
	}
}
