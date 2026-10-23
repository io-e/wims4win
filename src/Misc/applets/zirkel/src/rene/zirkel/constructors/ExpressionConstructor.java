package rene.zirkel.constructors;

// file: PointConstructor.java

import java.awt.event.*;

import rene.util.xml.*;
import rene.zirkel.*;
import rene.zirkel.construction.*;
import rene.zirkel.expression.*;
import rene.zirkel.objects.*;

public class ExpressionConstructor extends ObjectConstructor
{	ExpressionObject O;
	public void mousePressed (MouseEvent e, ZirkelCanvas zc)
	{	double x=zc.x(e.getX()),y=zc.y(e.getY());
		ExpressionObject o=new ExpressionObject(zc.getConstruction(),x,y);
		zc.addObject(o);
		o.setShowName(false);
		o.setDefaults();
		zc.repaint();
		Dragging=true;
		O=o;
	}
	public void mouseDragged (MouseEvent e, ZirkelCanvas zc)
	{	if (!Dragging) return;
		O.move(zc.x(e.getX()),zc.y(e.getY()));
		zc.repaint();
	}
	public void mouseReleased (MouseEvent e, ZirkelCanvas zc)
	{	if (!Dragging) return;
		Dragging=false;
		O.edit(zc);
	}
	public boolean waitForPoint ()
	{	return false;
	}
	public void showStatus (ZirkelCanvas zc)
	{	zc.showStatus(
			Zirkel.name("message.expression","Expression: Choose a place!"));
	}
	public boolean construct (XmlTree tree, Construction c)
		throws ConstructionException
	{	if (!testTree(tree,"Expression")) return false;
		XmlTag tag=tree.getTag();
		if (!tag.hasParam("x") || !tag.hasParam("y"))
			throw new ConstructionException("Expression coordinates missing!");
		if (!tag.hasParam("value"))
			throw new ConstructionException(
				Zirkel.name("exception.expression"));
		ExpressionObject p=new ExpressionObject(c,0,0);
		double x,y;
		try
		{	x=new Expression(tag.getValue("x"),c,p).getValue();
			y=new Expression(tag.getValue("y"),c,p).getValue();
			p.move(x,y);
		}
		catch (Exception e) {}
		p.setDefaults();
		if (tag.hasParam("prompt")) p.setPrompt(tag.getValue("prompt"));
		if (tag.hasParam("fixed"))
		{	p.setFixed(tag.getValue("x"),tag.getValue("y"));
		}
		p.setShowValue(tag.hasParam("showvalue"));
		setName(tag,p);
		set(tree,p);
		c.add(p);
		try
		{	p.setExpression(tag.getValue("value"),c);
		}
		catch (Exception e)
		{	throw new ConstructionException(
				Zirkel.name("exception.expression"));
		}
		setConditionals(tree,c,p);
		if (tag.hasTrueParam("slider"))
		{	try
			{	p.setSlider(tag.getValue("min"),
					tag.getValue("max"));
			}
			catch (Exception e)
			{	throw new ConstructionException(
					Zirkel.name("exception.expression"));
			}
		}
		return true;
	}

	public String getTag () { return "Expression"; }
	public void construct (Construction c, 
		String name, String params[], int nparams)
		throws ConstructionException
	{	if (nparams==1)
		{	ExpressionObject o=new ExpressionObject(c,
				c.getX()+(Math.random()-0.5)*c.getW(),
				c.getY()+(Math.random()-0.5)*c.getW());
			if (!name.equals("")) o.setNameCheck(name);
			c.add(o);
			o.setDefaults();
			try
			{	o.setExpression(params[0],c);
			}
			catch (Exception e)
			{	throw new ConstructionException(
					Zirkel.name("exception.expression"));
			}
		}
		else
			throw new ConstructionException(Zirkel.name("exception.nparams"));
	}

	public boolean useSmartBoard ()
	{	return false;
	}

}