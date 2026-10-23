package rene.zirkel.objects;

// file: CircleIntersectionObject.java

import rene.util.xml.*;
import rene.zirkel.construction.*;
import rene.zirkel.structures.*;

public class CircleIntersectionObject extends IntersectionObject
{	public CircleIntersectionObject (
		Construction c, PrimitiveCircleObject P1, PrimitiveCircleObject P2,
		boolean first)
	{	super(c,P1,P2);
		First=first;
		validate();
	}

	public void updateCircleDep ()
	{	((PrimitiveCircleObject)P1).addDep(this);
		((PrimitiveCircleObject)P2).addDep(this);
	}

	public void validate ()
	{	boolean oldvalid=Valid;
		if (!P1.valid() || !P2.valid()) Valid=false;
		else Valid=true; 
		if (!Valid) return;
		Coordinates c=PrimitiveCircleObject.intersect(
			(PrimitiveCircleObject)P1,(PrimitiveCircleObject)P2);
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
		PointObject oa=getAway();
		if (oa!=null)
		{	double x=oa.getX(),y=oa.getY();
			double r=(x-c.X)*(x-c.X)+(y-c.Y)*(y-c.Y);
			double r1=(x-c.X1)*(x-c.X1)+(y-c.Y1)*(y-c.Y1);
			boolean flag=(r>r1);
			if (!StayAway) flag=!flag;
			if (flag)
			{	X=c.X; Y=c.Y;
			}
			else
			{	X=c.X1; Y=c.Y1;
			}
		}
		else
		{	if (First) { X=c.X; Y=c.Y; }
			else { X=c.X1; Y=c.Y1; }
		}
		if (Restricted)
		{	if (!(((PrimitiveCircleObject)P1).getStart()==this ||
				((PrimitiveCircleObject)P1).getEnd()==this) &&
				!((PrimitiveCircleObject)P1).contains(X,Y)) Valid=false;
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
