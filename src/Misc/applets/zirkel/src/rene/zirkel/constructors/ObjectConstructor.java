package rene.zirkel.constructors;

// file: ObjectConstructor.java

import java.awt.event.*;
import java.util.*;

import rene.util.xml.*;
import rene.zirkel.*;
import rene.zirkel.construction.*;
import rene.zirkel.expression.*;
import rene.zirkel.objects.*;

public class ObjectConstructor
{	protected boolean Dragging=false;

	public void mousePressed (MouseEvent e, ZirkelCanvas zc) {}
	public void mouseReleased (MouseEvent e, ZirkelCanvas zc) {}
	public void mouseDragged (MouseEvent e, ZirkelCanvas zc) {}

	public void mouseMoved (MouseEvent e, ZirkelCanvas zc, boolean simple)
	{	if (simple && (waitForPoint() || waitForLastPoint()))
		{	zc.indicateCreatePoint(e.getX(),e.getY(),false);
			return;
		}
		if (waitForPoint())
			zc.indicateCreatePoint(e.getX(),e.getY(),false);
		if (waitForLastPoint())
		{	if (zc.isPreview())
			{	zc.movePreview(e);
			}
			else
			{	zc.prepareForPreview(e);
				finishConstruction(e,zc);
				return;
			}
		}
	}

	public void finishConstruction (MouseEvent e, ZirkelCanvas zc)
	{
	}
	
	public boolean waitForLastPoint ()
	{	return false;
	}
	
	public boolean waitForPoint ()
	{	return true;
	}
	
	public void reset (ZirkelCanvas zc)
	{	zc.validate();
		zc.clearSelected();
	}
	public void resetFirstTime (ZirkelCanvas zc)
	{	reset(zc);
	}
	public void invalidate (ZirkelCanvas zc) {}
	public void showStatus (ZirkelCanvas zc) {}
	public boolean construct (XmlTree tree, Construction c) 
		throws ConstructionException
	{	return false;
	}
	public boolean testTree (XmlTree t, String tag)
	{	return t.getTag().name().equals(tag);
	}
	public void setName (XmlTag tag, ConstructionObject o)
	{	if (tag.hasParam("name"))
		{	o.setName(tag.getValue("name"));
		}
		if (tag.hasParam("alias"))
		{	o.setAlias(tag.getValue("alias"));
		}
	}
	public void set (XmlTree tree, ConstructionObject o)
		throws ConstructionException
	{	XmlTag tag=tree.getTag();
		if (tag.hasParam("n"))
		{	try
			{	o.setNCount(new Integer(tag.getValue("n")).intValue());
				o.setGotNCount(true);
			}
			catch (Exception ex)
			{	throw new ConstructionException("Illegal count!");
			}
		}
		if (tag.hasParam("hidden"))
		{	if (tag.getValue("hidden").equals("super")) o.setSuperHidden(true);
			else o.setHidden(true);
		}
		if (tag.hasTrueParam("showvalue"))
		{	o.setShowValue(true);
		}
		if (tag.hasTrueParam("showname"))
		{	o.setShowName(true);
		}
		if (tag.hasTrueParam("background"))
		{	o.setBack(true);
		}
		else o.setBack(false);
		if (tag.hasTrueParam("parameter"))
		{	o.setParameter();
		}
		if (tag.hasTrueParam("mainparameter"))
		{	o.setMainParameter();
		}
		if (tag.hasTrueParam("target"))
		{	o.setTarget(true);
		}
		if (tag.hasTrueParam("break"))
		{	o.setBreak(true);
		}
		if (tag.hasTrueParam("hidebreak"))
		{	o.setHideBreak(true);
		}
		if (tag.hasTrueParam("solid"))
		{	o.setSolid(true);
		}
		if (tag.hasTrueParam("bold"))
		{	o.setBold(true);
		}
		if (tag.hasTrueParam("large"))
		{	o.setLarge(true);
		}
		if (tag.hasParam("xoffset") || tag.hasParam("yoffset"))
		{	int x=0,y=0;
			try
			{	if (tag.hasParam("xoffset"))
					x=Integer.parseInt(tag.getValue("xoffset"));
				if (tag.hasParam("yoffset"))
					y=Integer.parseInt(tag.getValue("yoffset"));
				o.setOffset(x,y);
			}
			catch (Exception e)
			{	throw new ConstructionException("Illegal offset value");
			}
		}
		if (tag.hasTrueParam("keepclose"))
		{	o.setKeepClose(true);
		}
		if (tag.hasParam("xcoffset") || tag.hasParam("ycoffset"))
		{	double x=0,y=0;
			try
			{	if (tag.hasParam("xcoffset"))
					x=new Double(tag.getValue("xcoffset")).doubleValue();
				if (tag.hasParam("ycoffset"))
					y=new Double(tag.getValue("ycoffset")).doubleValue();
				o.setcOffset(x,y);
			}
			catch (Exception e)
			{	throw new ConstructionException("Illegal offset value");
			}
		}
		if (tag.hasParam("color"))
		{	try
			{	String s=tag.getValue("color");
				int n=-1;
				for (int i=0; i<ZirkelFrame.ColorStrings.length; i++)
				{	if (s.equals(ZirkelFrame.ColorStrings[i]))
					{	o.setColor(i); n=i; break;
					}
				}
				if (n<0)
				{	n=Integer.parseInt(s);
					if (n<0 || n>=ZirkelFrame.Colors.length)
						throw new Exception("");
					o.setColor(n);
				}
			}
			catch (Exception ex)
			{	throw new ConstructionException("Illegal color index (1-"+
					(ZirkelFrame.Colors.length-1)+")");
			}
		}
		if (tag.hasParam("type"))
		{	String type=tag.getValue("type");
			if (type.equals("thick")) o.setColorType(ConstructionObject.THICK);
			if (type.equals("thin")) o.setColorType(ConstructionObject.THIN);
			if (type.equals("invisible")) o.setColorType(ConstructionObject.INVISIBLE);
		}
		if (tag.hasParam("unit"))
			o.setUnit(tag.getValue("unit"));
		else
			o.setUnit("");
		Enumeration e=tree.getContent();
		while (e.hasMoreElements())
		{	tree=(XmlTree)e.nextElement();
			if (tree.getTag() instanceof XmlTagText)
			{	o.setText(((XmlTagText)tree.getTag()).getContent(),true);
			}
		}
	}
	public void setConditionals (XmlTree tree, Construction c, ConstructionObject o)
	{	o.clearConditionals();
		int i=0;
		XmlTag tag=tree.getTag();
		while (tag.hasParam("ctag"+i) && tag.hasParam("cexpr"+i))
		{	String t=tag.getValue("ctag"+i);
			String e=tag.getValue("cexpr"+i);
			Expression ex=new Expression(e,c,o);
			o.addConditional(t,ex);
			i++;
		}
	}
	public String getTag ()
	{	return "???";
	}
	public void construct (Construction c, 
		String name, String params[], int nparams)
		throws ConstructionException
	{	throw new ConstructionException("");
	}
	
	// for MetaMover :
	public void pause (boolean flag)
	{
	}
	
	public boolean useSmartBoard ()
	{	return true;
	}
}

