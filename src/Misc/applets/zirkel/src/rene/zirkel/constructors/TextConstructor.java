package rene.zirkel.constructors;

// file: PointConstructor.java

import java.awt.event.*;

import rene.util.xml.*;
import rene.zirkel.*;
import rene.zirkel.construction.*;
import rene.zirkel.expression.*;
import rene.zirkel.objects.*;

public class TextConstructor extends ObjectConstructor
{	public void mousePressed (MouseEvent e, ZirkelCanvas zc)
	{	double x=zc.x(e.getX()),y=zc.y(e.getY());
		TextObject p=new TextObject(zc.getConstruction(),x,y);
		zc.addObject(p);
		p.setDefaults();
		p.edit(zc);
		zc.repaint();
	}
	
	public boolean waitForPoint ()
	{	return false;
	}
	
	public void showStatus (ZirkelCanvas zc)
	{	zc.showStatus(Zirkel.name("message.text"));
	}
	
	public boolean construct (XmlTree tree, Construction c)
		throws ConstructionException
	{	if (!testTree(tree,"Text")) return false;
		XmlTag tag=tree.getTag();
		if (!tag.hasParam("x") || !tag.hasParam("y"))
			throw new ConstructionException("Point coordinates missing!");
		TextObject p=new TextObject(c,0,0);
		double x,y;
		try
		{	x=new Expression(tag.getValue("x"),c,p).getValue();
			y=new Expression(tag.getValue("y"),c,p).getValue();
			p.move(x,y);
		}
		catch (Exception e) {}
		setName(tag,p);
		set(tree,p);
		c.add(p);
		setConditionals(tree,c,p);
		p.setLines(p.getText());
		if (tag.hasParam("fixed"))
		{	p.setFixed(tag.getValue("x"),tag.getValue("y"));
		}
		return true;
	}


	public void reset (ZirkelCanvas zc)
	{	super.reset(zc);
		showStatus(zc);
	}
}
