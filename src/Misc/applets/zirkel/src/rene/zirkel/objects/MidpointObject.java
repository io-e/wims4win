package rene.zirkel.objects;

// file: MidpointObject.java

import java.util.Enumeration;

import rene.util.xml.XmlWriter;
import rene.zirkel.*;
import rene.zirkel.construction.*;

public class MidpointObject extends PointObject
{	PointObject P1,P2;
	static Count N=new Count();
	
	public MidpointObject (Construction c, PointObject p1, PointObject p2)
	{	super(c,0,0);
		P1=p1; P2=p2;
		Moveable=false;
		validate();
		updateText();
	}

	public String getTag () { return "Midpoint"; }
	public int getN () { return N.next(); }
	
	public void updateText ()
	{	try
		{	setText(text2(Zirkel.name("text.midpoint"),P1.getName(),P2.getName()));
		}
		catch (Exception e) {}
	}
	public void validate ()
	{	if (!P1.valid() || !P2.valid()) Valid=false;
		else
		{	Valid=true;
			X=(P1.getX()+P2.getX())/2;
			Y=(P1.getY()+P2.getY())/2;
		}
	}
	public void printArgs (XmlWriter xml)
	{	xml.printArg("first",P1.getName());
		xml.printArg("second",P2.getName());
		printType(xml);
	}

	public Enumeration depending ()
	{	super.depending();
		DL.add(P1); DL.add(P2);
		return DL.elements();
	}

	public void translate ()
	{	P1=(PointObject)P1.getTranslation();
		P2=(PointObject)P2.getTranslation();
	}

}
