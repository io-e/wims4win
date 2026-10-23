package rene.zirkel.objects;

// file: IntersectionObject.java

import java.util.*;

import rene.util.xml.*;
import rene.zirkel.*;
import rene.zirkel.construction.*;
import rene.zirkel.expression.*;

/**
 * This is the parent class of various intersection objects.
 * Intersections are points. Problems are multiple intersections
 * and restrictions to keep one of them away or close to some
 * other point. The most problematic intersections are between
 * objects of type PointonObject.
 * 
 * In case of two possible intersection points, there is also 
 * the option to switch from one object to the other in
 * automatic tracks. This allows to run through all possible
 * states of a construction.  
 * 
 * @author Rene
 */

public class IntersectionObject extends PointObject
{	protected ConstructionObject P1,P2;
	private static Count N=new Count();
	protected Expression Away;
	protected boolean StayAway=true;
	protected boolean First;
	protected boolean Switched;
	protected boolean Restricted;
	protected boolean Alternate;

	public IntersectionObject (Construction c,
		ConstructionObject p1, ConstructionObject p2)
	{	super(c,0,0);
		Moveable=false;
		P1=p1; P2=p2;
		updateText();
		First=true;
		Switched=false;
		Restricted=true;
		Alternate=false;
	}
	
	public String getTag () { return "Intersection"; }
	public int getN () { return N.next(); }
	
	public void updateText()
	{	try
		{	setText(text2(Zirkel.name("text.intersection"),P1.getName(),P2.getName()));
		}
		catch (Exception e) {}
	}
	
	public void setFirst (boolean flag)
	{	First=flag;
	}
	public boolean isFirst ()
	{	return First;
	}
	
	public void validate ()
	{	if (!P1.valid() || !P2.valid()) Valid=false;
		else Valid=true;
	}
	
	public void validate (double x, double y)
	{
	}
	
	public void printArgs (XmlWriter xml)
	{	xml.printArg("first",P1.getName());
		xml.printArg("second",P2.getName());
		if (getAway()!=null)
		{	if (StayAway) xml.printArg("awayfrom",getAway().getName());
			else xml.printArg("closeto",getAway().getName());
		}
		printType(xml);
		if (!Restricted) xml.printArg("valid","true");
		if (Alternate) xml.printArg("alternate","true");
	}

	public String away ()
	{	if (getAway()!=null) return getAway().getName();
		else return "";
	}
	
	public boolean stayAway ()
	{	return StayAway;
	}

	public boolean setAway (String s, boolean flag)
	{	Away=null;
		if (s.equals("")) return true;
		if (Cn==null) return true;
		Away=new Expression("@\""+s+"\"",Cn,this);
		StayAway=flag;
		return getAway()!=null;
	}
	
	public boolean setAway (String s)
	{	return setAway(s,true);
	}

	public Enumeration depending ()
	{	super.depending();
		return depset(P1,P2);
	}
	
	public void translate ()
	{	P1=P1.getTranslation();
		P2=P2.getTranslation();
		if (getAway()!=null) 
		{	setAway(getAway().getName(),StayAway);
			Away.translate();
		}
	}
	
	public boolean isSwitchable ()
	{	return false;
	}

	/**
	* Check, if the other intersection is already visible and defined.
	* In this case, we want to keep the intersection different from
	* the other intersection point.
	*/
	public void autoAway ()
	{	if (!autoAway(P1,P2)) autoAway(P2,P1);
	}
	
	boolean autoAway (ConstructionObject o1, ConstructionObject o2)
	{	if (o1 instanceof CircleObject)
		{	PointObject p1=((CircleObject)o1).getP2();
			if (p1.isHidden()) return false;
			if (p1.dependsOn(o2) && !nearto(p1))
			{	setAway(p1.getName());
				return true;
			}
			else if (o2 instanceof CircleObject)
			{	PointObject p2=((CircleObject)o2).getP2();
				if (p2.isHidden()) return false;
				if (p1==p2 && !nearto(p1))
				{	setAway(p1.getName());
					return true;
				}
				return false;
			}
			else if (o2 instanceof PrimitiveLineObject)
			{	Enumeration en=((PrimitiveLineObject)o2).points();
				while (en.hasMoreElements())
				{	ConstructionObject oo=(ConstructionObject)en.nextElement();
					if (oo instanceof PointObject)
					{	PointObject o=(PointObject)oo;
						if (o.isHidden()) return false;
						if (p1==o && !nearto(p1))
						{	setAway(p1.getName());
							return true;
						}
					}
				}
			}
		}
		else if (o1 instanceof TwoPointLineObject)
		{	PointObject p1=((TwoPointLineObject)o1).getP1();
			if (!p1.isHidden() && p1.dependsOn(o2) && !nearto(p1))
			{	setAway(p1.getName());
				return true;
			}
			PointObject p2=((TwoPointLineObject)o1).getP2();
			if (!p2.isHidden() && p2.dependsOn(o2) && !nearto(p2))
			{	setAway(p2.getName());
				return true;
			}
		}
		return false;
	}
	
	public void switchBack ()
	{	if (Switched) First=!First;
		Switched=false;
	}
	
	public void doSwitch ()
	{	Switched=!Switched;
		First=!First;
	}
	
	public boolean isSwitched ()
	{	return Switched;
	}

	public boolean isRestricted ()
	{	return Restricted;
	}
	
	public void setRestricted (boolean flag)
	{	Restricted=flag;
	}

	public PointObject getAway ()
	{	return getPointObject(Away);
	}

	public void setAlternate (boolean flag)
	{	Alternate=flag;
	}
	public boolean isAlternate ()
	{	return Alternate;
	}

	/**
	 * Returns, if this intersection can alternate between two states,
	 * like CircleIntersection and LineCircleIntersection. Used
	 * by the dialog.
	 */
	public boolean canAlternate ()
	{	return false;
	}
}
