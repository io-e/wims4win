package rene.zirkel.constructors;

// file: Cicle3Constructor.java

import java.awt.event.*;

import rene.util.xml.*;
import rene.zirkel.*;
import rene.zirkel.construction.*;
import rene.zirkel.objects.*;

public class Circle3Constructor extends ObjectConstructor
{	PointObject P1=null,P2=null,P3=null;
	public void mousePressed (MouseEvent e, ZirkelCanvas zc)
	{	if (!zc.Visual) return;
		if (P1==null)
		{	P1=select(e.getX(),e.getY(),zc);
			if (P1!=null)
			{	P1.setSelected(true);
				zc.repaint();
			}
			showStatus(zc);
		}
		else if (P2==null)
		{	P2=select(e.getX(),e.getY(),zc);
			if (P2!=null && P2!=P1)
			{	P2.setSelected(true);
				zc.repaint();
			}
			showStatus(zc);
		}
		else
		{	P3=select(e.getX(),e.getY(),zc);
			if (P3!=null)
			{	Circle3Object c=new Circle3Object(zc.getConstruction(),P1,P2,P3);
				zc.addObject(c);
				c.setDefaults();
				c.updateCircleDep();
				P1=P2=P3=null;
				zc.clearSelected();
				showStatus(zc);
			}
		}
	}
	public boolean waitForLastPoint ()
	{	return P1!=null && P2!=null;
	}
	public void finishConstruction (MouseEvent e, ZirkelCanvas zc)
	{	P3=select(e.getX(),e.getY(),zc);
		if (P3!=null)
		{	Circle3Object c=new Circle3Object(zc.getConstruction(),P1,P2,P3);
			zc.addObject(c);
			c.setDefaults();
			zc.validate();
			zc.repaint();
			P3=null;
		}	
	}
	public PointObject select (int x, int y, ZirkelCanvas zc)
	{	return zc.selectCreatePoint(x,y);
	}
	public void reset (ZirkelCanvas zc)
	{	super.reset(zc);
		if (zc.Visual)
		{	P1=P2=P3=null;
			showStatus(zc);
		}
		else
		{	zc.setPrompt(Zirkel.name("prompt.circle3"));
		}
	}
	public void showStatus (ZirkelCanvas zc)
	{	if (P1==null) zc.showStatus(
			Zirkel.name("message.circle3.first","Circle: Choose the first radius point!"));
		else if (P2==null) zc.showStatus(
			Zirkel.name("message.circle3.second","Circle: Choose the second radius point!"));
		else zc.showStatus(
			Zirkel.name("message.circle3.midpoint","Circle: Choose the midpoint!"));
	}

	public boolean construct (XmlTree tree, Construction c)
		throws ConstructionException
	{	if (!testTree(tree,"Circle3")) return false;
		XmlTag tag=tree.getTag();
		if (!tag.hasParam("midpoint") || !tag.hasParam("from") ||
				!tag.hasParam("to"))
			throw new ConstructionException("Circle3 parameters missing!");
		try
		{	PointObject p1=(PointObject)c.find(tag.getValue("midpoint")); 
			PointObject p2=(PointObject)c.find(tag.getValue("from"));
			PointObject p3=(PointObject)c.find(tag.getValue("to"));
			Circle3Object o=new Circle3Object(c,p2,p3,p1);
			if (tag.hasParam("partial")) o.setPartial(true);
			if (tag.hasParam("start") && tag.hasParam("end"))
				o.setRange(tag.getValue("start"),tag.getValue("end"));
			setName(tag,o);
			set(tree,o);
			c.add(o);
			setConditionals(tree,c,o);
		}
		catch (ConstructionException e)
		{	throw e;
		}
		catch (Exception e)
		{	throw new ConstructionException("Circle3 parameters illegal!");
		}
		return true;
	}
}
