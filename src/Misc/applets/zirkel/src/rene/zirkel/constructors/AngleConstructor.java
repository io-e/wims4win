package rene.zirkel.constructors;

// file: Cicle3Constructor.java

import java.awt.event.*;

import rene.util.xml.*;
import rene.zirkel.*;
import rene.zirkel.construction.*;
import rene.zirkel.expression.*;
import rene.zirkel.objects.*;
import rene.gui.Global;

public class AngleConstructor extends ObjectConstructor
{	PointObject P1=null,P2=null,P3=null;
	boolean Fixed=false;
	public AngleConstructor (boolean fixed)
	{	Fixed=fixed;
	}
	public AngleConstructor ()
	{	this(false);
	}
	FixedAngleObject A;
	ConstructionObject O;
	boolean ShowsValue;
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
		else if (P2==null)
		{	P2=select(e.getX(),e.getY(),zc);
			if (P2!=null)
			{	P2.setSelected(true);
				zc.repaint();
			}
			showStatus(zc);
		}
		else
		{	if (!e.isShiftDown() && !Fixed)
			{	P3=select(e.getX(),e.getY(),zc);
				if (P3!=null)
				{	if (P3==P1 || P3==P2)
					{	P3=null; return;
					}
					AngleObject a=new AngleObject(zc.getConstruction(),P1,P2,P3);
					zc.addObject(a);
					a.setDefaults();
					if (P3.moveable() && !P3.isPointOn() && zc.isNewPoint())
					{	ShowsValue=a.showValue();
						if (Global.getParameter("options.movefixname",true))
							a.setShowValue(true);
						O=a;
						Dragging=true;
						a.validate();
						zc.repaint();
					}
					else
					{	Dragging=false;
						P1=P2=P3=null;
						reset(zc);
						zc.validate();
						zc.repaint();
					}
				}
			}
			else
			{	FixedAngleObject a=new FixedAngleObject(zc.getConstruction(),
					P1,P2,zc.x(e.getX()),zc.y(e.getY()));
				zc.addObject(a);
				a.setDefaults();
				a.init(zc.getConstruction(),zc.x(e.getX()),zc.y(e.getY()));
				Moved=0;
				Dragging=true;
				ShowsValue=a.showValue();
				if (Global.getParameter("options.movefixname",true))
					a.setShowValue(true);
				O=A=a;
				P3=null;
				zc.repaint();
			}
		}
	}
	
	public boolean waitForLastPoint ()
	{	return P1!=null && P2!=null;
	}

	public boolean waitForPoint ()
	{	return !Fixed || (P1==null || P2==null);
	}

	public void finishConstruction (MouseEvent e, ZirkelCanvas zc)
	{	if (!Fixed)
		{	P3=select(e.getX(),e.getY(),zc);
			if (P3!=null)
			{	AngleObject a=new AngleObject(zc.getConstruction(),P1,P2,P3);
				zc.addObject(a);
				a.setDefaults();
			}
			zc.repaint();
			P3=null;
		}
		else
		{	FixedAngleObject a=new FixedAngleObject(zc.getConstruction(),
				P1,P2,zc.x(e.getX()),zc.y(e.getY()));
			zc.addObject(a);
			a.setDefaults();
			a.init(zc.getConstruction(),zc.x(e.getX()),zc.y(e.getY()));
			zc.setPreviewObject(a);
			zc.repaint();
		}
	}

	public void mouseDragged (MouseEvent e, ZirkelCanvas zc)
	{	if (!Dragging) return;
		Moved++;
		if (P3==null)
		{	A.init(zc.getConstruction(),zc.x(e.getX()),zc.y(e.getY()));
			if (A instanceof FixedAngleObject)
				((FixedAngleObject)A).setDragable(Moved>5);
		}
		else
		{	P3.move(zc.x(e.getX()),zc.y(e.getY()));
			zc.validate();
		}
		zc.repaint();
	}
	public void mouseReleased (MouseEvent e, ZirkelCanvas zc)
	{	if (!Dragging) return;
		Dragging=false;
		O.setShowValue(ShowsValue);
		zc.repaint();
		if (P3==null)
		{	zc.clearSelected();
			A.round();
			if (Moved<5)
			{	A.edit(zc);
				if (A instanceof FixedAngleObject &&
					((FixedAngleObject)A).isEditAborted())
				{	zc.delete(A);
					reset(zc);
					return;
				}
			}
			A.validate();
			zc.check();
		}
		else P3.updateText();
		reset(zc);
		zc.showStatus();
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
		{	zc.setPrompt(Zirkel.name("prompt.angle"));
		}
	}
	public void showStatus (ZirkelCanvas zc)
	{	if (Fixed)
		{	if (P1==null) zc.showStatus(
				Zirkel.name("message.fixedangle.first"));
			else if (P2==null) zc.showStatus(
				Zirkel.name("message.fixedangle.root"));
			else zc.showStatus(
				Zirkel.name("message.fixedangle.second"));
		}
		else
		{	if (P1==null) zc.showStatus(
				Zirkel.name("message.angle.first"));
			else if (P2==null) zc.showStatus(
				Zirkel.name("message.angle.root"));
			else zc.showStatus(
				Zirkel.name("message.angle.second"));
		}
	}

	public boolean construct (XmlTree tree, Construction c)
		throws ConstructionException
	{	if (!testTree(tree,"Angle")) return false;
		XmlTag tag=tree.getTag();
		if (!tag.hasParam("first"))
		{	AngleObject o=new AngleObject(c);
			try
			{	if (tag.hasParam("display"))
				{	String type=tag.getValue("display");
					if (type.equals("small")) o.setDisplaySize(AngleObject.SMALL);
					if (type.equals("large")) o.setDisplaySize(AngleObject.LARGE);
					if (type.equals("larger")) o.setDisplaySize(AngleObject.LARGER);
					if (type.equals("rectangle")) o.setDisplaySize(AngleObject.RECT);
				}
				setName(tag,o);
				set(tree,o);
				c.add(o);
				setConditionals(tree,c,o);
				if (tag.hasTrueParam("filled")) o.setFilled(true);
				if (tag.hasParam("fixed")) o.setFixed(tag.getValue("fixed"));
				if (tag.hasTrueParam("acute")) o.setObtuse(false);
				else o.setObtuse(true);
			}
			catch (Exception e) {}
		}
		if (!tag.hasParam("first") || !tag.hasParam("root"))
			throw new ConstructionException("Angle parameters missing!");
		if (tag.hasParam("second"))
		{	try
			{	PointObject p1=(PointObject)c.find(tag.getValue("first")); 
				PointObject p2=(PointObject)c.find(tag.getValue("root"));
				PointObject p3=(PointObject)c.find(tag.getValue("second"));
				AngleObject o=new AngleObject(c,p1,p2,p3);
				if (tag.hasParam("display"))
				{	String type=tag.getValue("display");
					if (type.equals("small")) o.setDisplaySize(AngleObject.SMALL);
					if (type.equals("large")) o.setDisplaySize(AngleObject.LARGE);
					if (type.equals("larger")) o.setDisplaySize(AngleObject.LARGER);
					if (type.equals("rectangle")) o.setDisplaySize(AngleObject.RECT);
				}
				setName(tag,o);
				set(tree,o);
				c.add(o);
				setConditionals(tree,c,o);
				if (tag.hasTrueParam("filled")) o.setFilled(true);
				if (tag.hasParam("fixed")) o.setFixed(tag.getValue("fixed"));
				if (tag.hasTrueParam("acute")) o.setObtuse(false);
				else o.setObtuse(true);
			}
			catch (ConstructionException e)
			{	throw e;
			}
			catch (Exception e)
			{	throw new ConstructionException("Angle parameters illegal!");
			}
		}
		else
		{	try
			{	PointObject p1=(PointObject)c.find(tag.getValue("first")); 
				PointObject p2=(PointObject)c.find(tag.getValue("root"));
				FixedAngleObject o=new FixedAngleObject(c,p1,p2,0,0);
				if (tag.hasParam("display"))
				{	String type=tag.getValue("display");
					if (type.equals("small")) o.setDisplaySize(FixedAngleObject.SMALL);
					if (type.equals("large")) o.setDisplaySize(FixedAngleObject.LARGE);
					if (type.equals("larger")) o.setDisplaySize(FixedAngleObject.LARGER);
					if (type.equals("rectangle")) o.setDisplaySize(FixedAngleObject.RECT);
				}
				setName(tag,o);
				set(tree,o);
				c.add(o);
				setConditionals(tree,c,o);
				if (tag.hasTrueParam("filled")) o.setFilled(true);
				if (tag.hasTrueParam("acute")) o.setObtuse(false);
				else o.setObtuse(true);
				if (tag.hasTrueParam("inverse")) o.setInverse(true);
				else o.setInverse(false);
				if (tag.hasTrueParam("reduced")) o.setReduced(true);
				else o.setReduced(false);
				if (tag.hasTrueParam("dragable")) o.setDragable(true);
				if (tag.hasTrueParam("drawable")) o.setDragable(true); // downward compatibility
				if (tag.hasParam("fixed")) o.setFixed(tag.getValue("fixed"));
				else throw new ConstructionException("");
			}
			catch (ConstructionException e)
			{	throw e;
			}
			catch (Exception e)
			{	throw new ConstructionException("Angle parameters illegal!");
			}
		}
		return true;
	}

	public String getTag () { return "Angle"; }
	public void construct (Construction c, 
		String name, String params[], int nparams)
		throws ConstructionException
	{	if (nparams!=3)
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
		ConstructionObject
			P3=c.find(params[2]);
		if (P3==null || !(P3 instanceof PointObject))
		{	Expression ex=new Expression(params[2],c,null);
			if (!ex.isValid())
				throw new ConstructionException(
					Zirkel.name("exception.expression"));
			FixedAngleObject s=new FixedAngleObject(c,
				(PointObject)P1,(PointObject)P2,
				0,0);
			s.setFixed(params[2]);
			if (!name.equals("")) s.setNameCheck(name);
			c.add(s);
			s.setDefaults();
			s.setObtuse(true);
			s.setFixed(params[2]);
			s.validate();
			return;
		}
		if (!(P1 instanceof PointObject))
			throw new ConstructionException(Zirkel.name("exception.type")+" "+
				params[0]);
		if (!(P2 instanceof PointObject))
			throw new ConstructionException(Zirkel.name("exception.type")+" "+
				params[1]);
		if (!(P3 instanceof PointObject))
			throw new ConstructionException(Zirkel.name("exception.type")+" "+
				params[2]);
		if (P1==P2 || P2==P3)
			throw new ConstructionException(Zirkel.name("exception.parameter"));
		AngleObject s=new AngleObject(c,
			(PointObject)P1,(PointObject)P2,(PointObject)P3);
		if (!name.equals("")) s.setNameCheck(name);
		c.add(s);
		s.setDefaults();
	}

}
