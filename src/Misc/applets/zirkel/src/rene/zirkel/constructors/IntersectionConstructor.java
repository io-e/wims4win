package rene.zirkel.constructors;

// file: InersectionConstructor.java

import java.awt.event.*;

import rene.util.xml.*;
import rene.util.MyVector;
import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelCanvas;
import rene.zirkel.construction.Construction;
import rene.zirkel.construction.ConstructionException;
import rene.zirkel.objects.CircleIntersectionObject;
import rene.zirkel.objects.ConstructionObject;
import rene.zirkel.objects.IntersectionObject;
import rene.zirkel.objects.LineCircleIntersectionObject;
import rene.zirkel.objects.LineIntersectionObject;
import rene.zirkel.objects.PointonObjectIntersectionObject;
import rene.zirkel.objects.PrimitiveCircleObject;
import rene.zirkel.objects.PrimitiveLineObject;

public class IntersectionConstructor extends ObjectConstructor
{	ConstructionObject P1=null,P2=null;

	public void mousePressed (MouseEvent e, ZirkelCanvas zc)
	{	if (!zc.Visual) return;
		boolean immediate=false;
		if (P1==null)
		{	MyVector v=zc.selectPointonObjects(e.getX(),e.getY());
			if (v.size()==2)
			{	P1=(ConstructionObject)v.elementAt(0);
				P2=(ConstructionObject)v.elementAt(1);
				if (P1.equals(P2) ||
					(P1.isFilled() && P2.isFilled()))
				{	P1=P2=null;
				}
				else immediate=true;
			}
		}
		if (P1==null)
		{	P1=select(e.getX(),e.getY(),zc);
			if (P1!=null)
			{	P1.setSelected(true);
				zc.repaint();
				showStatus(zc);
			}
		}
		else
		{	if (P2==null) P2=select(e.getX(),e.getY(),zc);
			if (P2!=null)
			{	if (P2==P1)
				{	P2=null; return;
				}
				IntersectionObject o[]=construct(P1,P2,zc.getConstruction());
				if (o!=null)
				{	IntersectionObject oc=null;
					if (immediate && o.length>1)
					{	if (o[1].nearto(e.getX(),e.getY(),zc))
						{	o[0]=null; oc=o[1];
						}
						else
						{	o[1]=null; oc=o[0];
						}
					}
					for (int i=0; i<o.length; i++)
					{	if (o[i]!=null)
						{	o[i].setDefaults();
							zc.addObject(o[i]);
							o[i].validate(zc.x(e.getX()),zc.y(e.getY()));
						}
					}
					/**
					See, if the other intersection is visible and already a
					point of both objects.
					*/
					if (oc!=null) oc.autoAway();
				}
				P1=P2=null;
				zc.clearSelected();
				showStatus(zc);
			}
		}
	}
	
	public void mouseMoved (MouseEvent e, ZirkelCanvas zc, boolean simple)
	{	zc.indicateIntersectedObjects(e.getX(),e.getY());
	}

	public static IntersectionObject[] construct (
		ConstructionObject P1, ConstructionObject P2,
		Construction c)
	{	IntersectionObject o[]=null;
		if (P1 instanceof PrimitiveLineObject)
		{	if (P2 instanceof PrimitiveLineObject)
			{	o=new IntersectionObject[1];
				o[0]=new LineIntersectionObject(c,
						(PrimitiveLineObject)P1,(PrimitiveLineObject)P2);
			}
			else if (P2 instanceof PrimitiveCircleObject)
			{	o=new IntersectionObject[2];
				o[0]=new LineCircleIntersectionObject(c,
						(PrimitiveLineObject)P1,(PrimitiveCircleObject)P2,
						true);
				o[1]=new LineCircleIntersectionObject(c,
						(PrimitiveLineObject)P1,(PrimitiveCircleObject)P2,
						false);
			}
			else return construct(P2,P1,c);
		}
		else if (P1 instanceof PrimitiveCircleObject)
		{	if (P2 instanceof PrimitiveCircleObject)
			{	o=new IntersectionObject[2];
				o[0]=new CircleIntersectionObject(c,
						(PrimitiveCircleObject)P1,(PrimitiveCircleObject)P2,
						true);
				o[1]=new CircleIntersectionObject(c,
						(PrimitiveCircleObject)P1,(PrimitiveCircleObject)P2,
						false);
			}
			else if (P2 instanceof PrimitiveLineObject)
			{	o=new IntersectionObject[2];
				o[0]=new LineCircleIntersectionObject(c,
						(PrimitiveLineObject)P2,(PrimitiveCircleObject)P1,
						true);
				o[1]=new LineCircleIntersectionObject(c,
						(PrimitiveLineObject)P2,(PrimitiveCircleObject)P1,
						false);
			}
			else return construct(P2,P1,c);
		}
		else
		{	o=new PointonObjectIntersectionObject[1];
			o[0]=new PointonObjectIntersectionObject(c,P1,P2);
		}
		return o;
	}
	
	public ConstructionObject select (int x, int y, ZirkelCanvas zc)
	// select a line or circle at x,y
	{	return zc.selectPointonObject(x,y,false);
	}
	
	public void reset (ZirkelCanvas zc)
	// reset the tool
	{	super.reset(zc);
		if (zc.Visual)
		{	P1=P2=null;
			showStatus(zc);
		}
		else
		{	zc.setPrompt(Zirkel.name("prompt.intersection"));
		}
	}
	public void showStatus (ZirkelCanvas zc)
	{	if (P1==null) zc.showStatus(
			Zirkel.name("message.intersection.first","Intersection: Select first object!"));
		else zc.showStatus(
			Zirkel.name("message.intersection.second","Intersection: Select second object!"));
	}
	
	public boolean construct (XmlTree tree, Construction c)
		throws ConstructionException
	{	if (!testTree(tree,"Intersection")) return constructOther(tree,c);
		XmlTag tag=tree.getTag();
		if (!tag.hasParam("first") || !tag.hasParam("second"))
			throw new ConstructionException("Intersection parameters missing!");
		try
		{	ConstructionObject o1=c.find(tag.getValue("first")); 
			ConstructionObject o2=c.find(tag.getValue("second"));
			IntersectionObject o[]=construct(o1,o2,c);
			if (o==null) throw new Exception("");
			String name="",nameOther="";
			if (tag.hasParam("name")) name=tag.getValue("name");
			if (tag.hasParam("other")) nameOther=tag.getValue("other");
			if (o.length>1)
			{	if (tag.hasParam("which"))
				{	IntersectionObject oo;
					if (tag.getValue("which").equals("second")) oo=o[1];
					else oo=o[0];
					if (!name.equals("")) oo.setName(name);
					PointConstructor.setType(tag,oo);
					setName(tag,oo);
					set(tree,oo);
					c.add(oo);
					setConditionals(tree,c,oo);
					if (tag.hasParam("awayfrom"))
						oo.setAway(tag.getValue("awayfrom"),true);
					else if (tag.hasParam("closeto"))
						oo.setAway(tag.getValue("closeto"),false);
					if (tag.hasParam("valid")) oo.setRestricted(false);
					if (tag.hasParam("alternate")) oo.setAlternate(true);
				}
				else if (tag.hasParam("other"))
				{	if (!name.equals("")) o[0].setName(name);
					if (!nameOther.equals("")) o[1].setName(nameOther);
					if (tag.hasParam("awayfrom"))
					{	o[0].setAway(tag.getValue("awayfrom"),true);
						o[1].setAway(tag.getValue("awayfrom"),false);
					}
					else if (tag.hasParam("closeto"))
					{	o[1].setAway(tag.getValue("awayfrom"),true);
						o[0].setAway(tag.getValue("awayfrom"),false);
					}
					for (int i=0; i<o.length; i++)
					{	if (o[i]==null) continue;
						PointConstructor.setType(tag,o[i]);
						set(tree,o[i]);
						c.add(o[i]);
						setConditionals(tree,c,o[i]);
					}					
				}
			}
			else
			{	if (!name.equals("")) o[0].setName(name);
				PointConstructor.setType(tag,o[0]);
				setName(tag,o[0]);
				set(tree,o[0]);
				c.add(o[0]);
				setConditionals(tree,c,o[0]);
				if (tag.hasParam("valid")) o[0].setRestricted(false);
				try
				{	double x=new Double(tag.getValue("x")).doubleValue();
					double y=new Double(tag.getValue("y")).doubleValue();
					o[0].setXY(x,y);
				}
				catch (Exception e) {}
			}
		}
		catch (ConstructionException e)
		{	throw e;
		}
		catch (Exception e)
		{	e.printStackTrace();
			throw new ConstructionException("Intersection parameters illegal!");
		}
		return true;
	}

	public boolean constructOther (XmlTree tree, Construction c)
		throws ConstructionException
	{	if (!testTree(tree,"OtherIntersection")) return false;
		XmlTag tag=tree.getTag();
		if (tag.hasParam("name"))
		{	ConstructionObject o=c.find(tag.getValue("name"));	
			if (o==null || !(o instanceof IntersectionObject))
				throw new ConstructionException("OtherIntersection not found!");
			IntersectionObject oo=(IntersectionObject)o;
			PointConstructor.setType(tag,oo);
			o.setDefaults();
			set(tree,o);
			ConstructionObject ol=c.lastButOne();
			if (tag.hasParam("awayfrom"))
			{	oo.setAway(tag.getValue("awayfrom"),true);
				if (ol!=null && (ol instanceof IntersectionObject))
					((IntersectionObject)ol).setAway(tag.getValue("awayfrom"),
						false);
			}
			else if (tag.hasParam("closeto"))
			{	oo.setAway(tag.getValue("closeto"),false);
				if (ol!=null && (ol instanceof IntersectionObject))
					((IntersectionObject)ol).setAway(tag.getValue("awayfrom"),
						true);
			}
			if (tag.hasParam("valid"))
			{	oo.setRestricted(false);
			}
		}
		else
			throw new ConstructionException("OtherIntersection must have a name!");			
		return true;
	}

	public String getTag () { return "Intersection"; }
	public void construct (Construction c, 
		String name, String params[], int nparams)
		throws ConstructionException
	{	if (nparams!=2 && nparams!=3)
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
				params[0]);
		IntersectionObject o[]=construct(P1,P2,c);
		if (o==null)
			throw new ConstructionException(Zirkel.name("exception.type"));
		if (o.length==1)
		{	c.add(o[0]);
			o[0].setDefaults();
			if (!name.equals("")) o[0].setName(name);
		}
		else
		{	if (name.equals(""))
			{	for (int i=0; i<o.length; i++)
				{	c.add(o[i]);
					o[i].setDefaults();
				}
			}
			else
			{	String names[]=new String[2];
				int n;
				if ((n=name.indexOf(','))>=0)
				{	names[0]=name.substring(n+1).trim();
					names[1]=name.substring(0,n).trim();
				}
				else
				{	names[0]=name;
					names[1]="";
				}
				for (int i=0; i<o.length; i++)
				{	if (names[i].equals("")) continue;
					c.add(o[i]);
					o[i].setDefaults();
					o[i].setName(names[i]);
				}
			}
		}
	}

}
