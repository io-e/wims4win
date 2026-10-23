package rene.zirkel.constructors;

// file: LineConstructor.java

import java.awt.event.*;

import rene.util.xml.*;
import rene.zirkel.*;
import rene.zirkel.construction.*;
import rene.zirkel.macro.*;
import rene.zirkel.objects.*;
import rene.gui.Global;

public class LineConstructor extends ObjectConstructor
{	PointObject P1=null,P2=null;
	ConstructionObject O;
	boolean Fix;
	boolean ShowsValue,ShowsName,Moved;

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
		else
		{	P2=select(e.getX(),e.getY(),zc);
			if (P2!=null)
			{	if (P2==P1)
				{	P2=null; return;
				}
				ConstructionObject o=create(zc.getConstruction(),P1,P2);
				zc.addObject(o);
				o.setDefaults();
				Fix=e.isShiftDown() || isFixed();
				if (P2.moveable() && !P2.isPointOn() && zc.isNewPoint()) 
				{	Dragging=true; Moved=false;
					O=o;
					ShowsValue=o.showValue();
					ShowsName=o.showName();
					if ((Fix && Global.getParameter("options.movefixname",true))
						||
						(!Fix && Global.getParameter("options.movename",false)))
					{	o.setShowValue(true);
						o.setShowName(true);
					}
				}
				else
				{	Dragging=false;
					if (Fix) setFixed(zc,o);
					P1=P2=null;
					zc.clearSelected();
					showStatus(zc);
				}
			}
			else Dragging=false;
		}
	}
	
	public boolean waitForLastPoint ()
	{	return P1!=null && P2==null;
	}
	
	public void finishConstruction (MouseEvent e, ZirkelCanvas zc)
	{	P2=select(e.getX(),e.getY(),zc);
		if (P2!=null)
		{	ConstructionObject o=create(zc.getConstruction(),P1,P2);
			zc.addObject(o);
			o.setDefaults();
			zc.validate();
			zc.repaint();
			P2=null;
		}	
	}
	
	public void mouseDragged (MouseEvent e, ZirkelCanvas zc)
	{	if (!Dragging) return;
		else
		{	Moved=true;
			P2.move(zc.x(e.getX()),zc.y(e.getY()));
			zc.validate();
			zc.repaint();
		}
	}
	
	public void mouseReleased (MouseEvent e, ZirkelCanvas zc)
	{	if (!Dragging) return;
		Dragging=false;
		O.setShowValue(ShowsValue);
		O.setShowName(ShowsName);
		if (Fix) O.round();
		zc.repaint();
		if (Fix && !Moved) setFixed(zc,O);
		reset(zc);
	}
	
	public boolean isFixed ()
	{	return false;
	}
	public void setFixed (ZirkelCanvas zc, ConstructionObject o)
	{
	}
	public PointObject select (int x, int y, ZirkelCanvas zc)
	{	return zc.selectCreatePoint(x,y);
	}
	public ConstructionObject create (Construction c,
		PointObject p1, PointObject p2)
	{	return new LineObject(c,p1,p2);
	}
	public void reset (ZirkelCanvas zc)
	{	if (!zc.Visual)
		{	zc.setPrompt(getPrompt());
		}
		else
		{	zc.clearSelected();
			P1=P2=null;
			showStatus(zc);
		}
	}
	public String getPrompt()
	{	return Zirkel.name("prompt.line");
	}
	public void showStatus (ZirkelCanvas zc)
	{	if (P1==null) zc.showStatus(
			Zirkel.name("message.line.first","Line: Set the first point!"));
		else zc.showStatus(
			Zirkel.name("message.line.second","Line: Set the second point!"));
	}

	public boolean construct (XmlTree tree, Construction c)
		throws ConstructionException
	{	if (!testTree(tree,"Line")) return false;
		XmlTag tag=tree.getTag();
		if (!tag.hasParam("from") || !tag.hasParam("to"))
		{	if (!(c instanceof Macro))
				throw new ConstructionException("Line points missing!");
			PrimitiveLineObject o=new PrimitiveLineObject(c);
			setName(tag,o);
			set(tree,o);
			c.add(o);
			setConditionals(tree,c,o);
		}
		else try
		{	PointObject p1=(PointObject)c.find(tag.getValue("from")); 
			PointObject p2=(PointObject)c.find(tag.getValue("to"));
			LineObject o=new LineObject(c,p1,p2);
			if (tag.hasParam("partial")) o.setPartial(true);
			setName(tag,o);
			set(tree,o);
			c.add(o);
			setConditionals(tree,c,o);
		}
		catch (ConstructionException e)
		{	throw e;
		}
		catch (Exception e)
		{	throw new ConstructionException("Line points illegal!");
		}
		return true;
	}

	public String getTag () { return "Line"; }
	public void construct (Construction c, 
		String name, String params[], int nparams)
		throws ConstructionException
	{	if (nparams!=2)
			throw new ConstructionException(Zirkel.name("exception.nparams"));
		ConstructionObject
			P1=c.find(params[0]);
		if (P1==null)
			throw new ConstructionException(Zirkel.name("exception.notfound")+" "+
				params[0]);
		ConstructionObject
			P2=c.find(params[1]);
		if (P2==null)
			throw new ConstructionException(Zirkel.name("exception.notfound")+" "+
				params[1]);
		if (!(P1 instanceof PointObject))
			throw new ConstructionException(Zirkel.name("exception.type")+" "+
				params[0]);
		if (!(P2 instanceof PointObject))
			throw new ConstructionException(Zirkel.name("exception.type")+" "+
				params[1]);
		LineObject s=new LineObject(c,(PointObject)P1,(PointObject)P2);
		c.add(s);
		s.setDefaults();
		if (!name.equals("")) s.setNameCheck(name);
	}
}
