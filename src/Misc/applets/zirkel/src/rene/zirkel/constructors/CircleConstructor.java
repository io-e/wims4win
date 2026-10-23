package rene.zirkel.constructors;

// file: PointConstructor.java

import java.awt.event.*;

import rene.util.xml.*;
import rene.zirkel.*;
import rene.zirkel.construction.*;
import rene.zirkel.expression.*;
import rene.zirkel.macro.*;
import rene.zirkel.objects.*;
import rene.gui.Global;

public class CircleConstructor extends ObjectConstructor
{	PointObject P1=null,P2=null;
	boolean Fixed=false;
	public CircleConstructor (boolean fixed)
	{	Fixed=fixed;
	}
	public CircleConstructor ()
	{	this(false);
	}
	FixedCircleObject C;
	ConstructionObject O;
	boolean ShowsValue,ShowsName;
	int Moved;
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
		{	if (e.isShiftDown() || Fixed)
			{	FixedCircleObject c=new FixedCircleObject(zc.getConstruction(),
					P1,zc.x(e.getX()),zc.y(e.getY()));
				zc.addObject(c);
				c.setDefaults();
				zc.repaint();
				O=C=c;
				ShowsValue=c.showValue();
				ShowsName=c.showName();
				if (Global.getParameter("options.movefixname",true))
				{	C.setShowValue(true);
					C.setShowName(true);
				}
				Dragging=true;
				Moved=0;
				P2=null;
			}
			else
			{	P2=select(e.getX(),e.getY(),zc);
				if (P2!=null)
				{	if (P2==P1)
					{	P2=null; return;
					}
					CircleObject c=new CircleObject(zc.getConstruction(),P1,P2);
					zc.addObject(c);
					c.setDefaults();
					c.validate();
					zc.repaint();
					if (P2.moveable() && !P2.isPointOn() && zc.isNewPoint())
					{	ShowsValue=c.showValue();
						ShowsName=c.showName();
						if (Global.getParameter("options.movename",false))
						{	c.setShowValue(true);
							c.setShowName(true);
						}
						O=c;
						Dragging=true;
						Moved=0;
					}
					else
					{	P1.setSelected(false);
						P1=P2=null;
						showStatus(zc);
					}
				}
			}
		}
	}
	
	public boolean waitForLastPoint ()
	{	return P1!=null && P2==null;
	}
	
	public void finishConstruction (MouseEvent e, ZirkelCanvas zc)
	{	P2=select(e.getX(),e.getY(),zc);
		if (P2!=null)
		{	CircleObject c=new CircleObject(zc.getConstruction(),P1,P2);
			zc.addObject(c);
			c.setDefaults();
			c.validate();
			zc.repaint();
		}
		P2=null;
	}
	
	public boolean waitForPoint ()
	{	return P1==null || !Fixed;
	}
	
	public void mouseDragged (MouseEvent e, ZirkelCanvas zc)
	{	if (!Dragging) return;
		Moved++;
		if (P2==null)
		{	C.init(zc.getConstruction(),zc.x(e.getX()),zc.y(e.getY()));
			if (C instanceof FixedCircleObject)
				((FixedCircleObject)C).setDragable(Moved>5);
		}
		else
		{	P2.move(zc.x(e.getX()),zc.y(e.getY()));
			zc.validate();
		}
		zc.repaint();
	}
	
	public void mouseReleased (MouseEvent e, ZirkelCanvas zc)
	{	if (!Dragging) return;
		Dragging=false;
		O.setShowValue(ShowsValue);
		O.setShowName(ShowsName);
		zc.repaint();
		if (P2==null)
		{	P1.setSelected(false);
			P1=null;
			C.round();
			if (Moved<=5)
			{	C.edit(zc);
				if (C instanceof FixedCircleObject &&
					((FixedCircleObject)C).isEditAborted())
				{	zc.delete(C);
					zc.repaint();
					reset(zc);
				}
			}
			C.validate();
			zc.check();
		}
		else
		{	P1.setSelected(false);
			P2.updateText();
			P1=P2=null;
		}
		O.updateCircleDep();
		zc.repaint();
		zc.showStatus();	
	}
	
	public PointObject select (int x, int y, ZirkelCanvas zc)
	{	return zc.selectCreatePoint(x,y);
	}
	
	public void reset (ZirkelCanvas zc)
	{	super.reset(zc);
		if (zc.Visual)
		{	P1=P2=null;
			showStatus(zc);
		}
		else
		{	zc.setPrompt(Zirkel.name("prompt.circle"));
		}
	}
	public void showStatus (ZirkelCanvas zc)
	{	if (Fixed)
		{	if (P1==null) zc.showStatus(
				Zirkel.name("message.fixedcircle.midpoint"));
			else zc.showStatus(
				Zirkel.name("message.fixedcircle.radius"));
		}
		else
		{	if (P1==null) zc.showStatus(
				Zirkel.name("message.circle.midpoint"));
			else zc.showStatus(
				Zirkel.name("message.circle.radius"));
		}
	}
	public boolean construct (XmlTree tree, Construction c)
		throws ConstructionException
	{	if (!testTree(tree,"Circle")) return false;
		XmlTag tag=tree.getTag();
		try
		{	if (!tag.hasParam("midpoint"))
					throw new ConstructionException("Circle parameters missing!");
			if (!tag.hasParam("through"))
			{	if (tag.hasParam("fixed"))
				{	PointObject p1=(PointObject)c.find(tag.getValue("midpoint")); 
					FixedCircleObject o=new FixedCircleObject(c,p1,0,0);
					c.add(o);
					setConditionals(tree,c,o);
					setName(tag,o);
					set(tree,o);
					if (tag.hasParam("partial")) o.setPartial(true);
					if (tag.hasParam("filled")) o.setFilled(true);
					if (tag.hasTrueParam("dragable")) o.setDragable(true);
					if (tag.hasTrueParam("drawable")) o.setDragable(true); // downward compatibility
					o.setFixed(tag.getValue("fixed"));
					if (tag.hasParam("start") && tag.hasParam("end"))
						o.setRange(tag.getValue("start"),tag.getValue("end"));
					if (tag.hasParam("acute")) o.setObtuse(false);
					if (tag.hasParam("chord")) o.setArc(false);
				}
				else
				{	if (!(c instanceof Macro))
						throw new ConstructionException("Circle parameters missing!");
					PointObject p1=(PointObject)c.find(tag.getValue("midpoint")); 
					PrimitiveCircleObject o=new PrimitiveCircleObject(c,p1);
					setName(tag,o);
					set(tree,o);
					c.add(o);
					setConditionals(tree,c,o);
				}
			}
			else
			{	PointObject p1=(PointObject)c.find(tag.getValue("midpoint")); 
				PointObject p2=(PointObject)c.find(tag.getValue("through"));
				CircleObject o=new CircleObject(c,p1,p2);
				setName(tag,o);
				set(tree,o);
				c.add(o);
				setConditionals(tree,c,o);
				if (tag.hasParam("partial")) o.setPartial(true);
				if (tag.hasParam("filled")) o.setFilled(true);
				if (tag.hasParam("start") && tag.hasParam("end"))
					o.setRange(tag.getValue("start"),tag.getValue("end"));
				if (tag.hasParam("acute")) o.setObtuse(false);
				if (tag.hasParam("chord")) o.setArc(false);
				if (tag.hasParam("fixed"))
				{	try
					{	o.setFixed(true,tag.getValue("fixed"));
					}
					catch (Exception e)
					{	throw new ConstructionException("Fixed value illegal!");
					}
				}
			}
		}
		catch (ConstructionException e)
		{	throw e;
		}
		catch (Exception e)
		{	throw new ConstructionException("Circle parameters illegal!");
		}
		return true;
	}
	
	public String getTag () { return "Circle"; }
	public void construct (Construction c, 
		String name, String params[], int nparams)
		throws ConstructionException
	{	if (nparams>3 || nparams==0)
			throw new ConstructionException(Zirkel.name("exception.nparams"));
		ConstructionObject
			P1=c.find(params[0]);
		if (P1==null)
			throw new ConstructionException(Zirkel.name("exception.notfound")+" "+
				params[0]);
		if (!(P1 instanceof PointObject))
			throw new ConstructionException(Zirkel.name("exception.type")+" "+
				params[0]);
		if (nparams==1)
		{	PrimitiveCircleObject s=new PrimitiveCircleObject(c,(PointObject)P1);
			c.add(s);
			s.setDefaults();
			if (!name.equals("")) s.setNameCheck(name);
			return;
		}		
		ConstructionObject
			P2=c.find(params[1]);
		if (P2==null)
		{	Expression ex=new Expression(params[1],c,null);
			if (!ex.isValid())
				throw new ConstructionException(Zirkel.name("exception.expression"));
			FixedCircleObject s=new FixedCircleObject(c,(PointObject)P1,0,0);
			c.add(s);
			s.setDefaults();
			s.setFixed(params[1]);
			s.validate();
			if (!name.equals("")) s.setNameCheck(name);
			return;
		}
		if (P2 instanceof SegmentObject)
		{	Circle3Object s=new Circle3Object(c,
				((SegmentObject)P2).getP1(),
				((SegmentObject)P2).getP2(),
				(PointObject)P1);
			c.add(s);
			s.setDefaults();
			if (!name.equals("")) s.setNameCheck(name);
			return;
		}
		if (!(P2 instanceof PointObject))
			throw new ConstructionException(Zirkel.name("exception.type")+" "+
				params[1]);
		if (nparams==3)
		{	ConstructionObject P3=c.find(params[2]);
			if (P3==null || !(P3 instanceof PointObject))
			{	CircleObject s=new CircleObject(c,(PointObject)P1,(PointObject)P2);
				if (!s.canFix())
					throw new ConstructionException(Zirkel.name("exception.canfix"));
				s.setFixed(true,params[2]);
				if (!s.isValidFix())
					throw new ConstructionException(Zirkel.name("exception.fix")+" "+
						params[2]);
				c.add(s);
				s.validate();
				if (!name.equals("")) s.setNameCheck(name);
				s.setDefaults();
				return;
			}
			else
			{	Circle3Object cr=new Circle3Object(c,(PointObject)P2,(PointObject)P3,
					(PointObject)P1);
				c.add(cr);
				cr.setDefaults();
				if (!name.equals("")) cr.setNameCheck(name);
				return;
			}		
		}
		CircleObject s=new CircleObject(c,(PointObject)P1,(PointObject)P2);
		c.add(s);
		s.setDefaults();
		if (!name.equals("")) s.setName(name);
	}

}
