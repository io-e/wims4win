package rene.zirkel.constructors;

// file: SegmentConstructor.java

import rene.util.xml.XmlTag;
import rene.util.xml.XmlTree;
import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelCanvas;
import rene.zirkel.construction.Construction;
import rene.zirkel.construction.ConstructionException;
import rene.zirkel.expression.Expression;
import rene.zirkel.objects.ConstructionObject;
import rene.zirkel.objects.PointObject;
import rene.zirkel.objects.SegmentObject;

public class SegmentConstructor extends LineConstructor
{	boolean Fixed=false;
	public SegmentConstructor ()
	{	this(false);	
	}
	public SegmentConstructor (boolean fixed)
	{	Fixed=fixed;
	}
	public ConstructionObject create (Construction c, PointObject p1, PointObject p2)
	{	return new SegmentObject(c,p1,p2);
	}
	public boolean isFixed ()
	{	return Fixed;
	}
	public void setFixed (ZirkelCanvas zc, ConstructionObject o)
	{	if (o instanceof SegmentObject)
		{	SegmentObject s=(SegmentObject)o;
			if (s.canFix())
				try
				{	s.validate();
					s.setFixed(true,""+s.getLength());
					s.edit(zc);
					s.validate();
					zc.repaint();
				}
				catch (Exception e) {}
			else
			{	zc.warning(Zirkel.name("error.fixedsegment"));
			}
		}
	}
	public void showStatus (ZirkelCanvas zc)
	{	if (!Fixed)
		{	if (P1==null) zc.showStatus(
				Zirkel.name("message.segment.first"));
			else zc.showStatus(
				Zirkel.name("message.segment.second"));
		}
		else
		{	if (P1==null) zc.showStatus(
				Zirkel.name("message.fixedsegment.first"));
			else zc.showStatus(
				Zirkel.name("message.fixedsegment.second"));
		}
	}
	public boolean construct (XmlTree tree, Construction c)
		throws ConstructionException
	{	if (!testTree(tree,"Segment")) return false;
		XmlTag tag=tree.getTag();
		if (!tag.hasParam("from") || !tag.hasParam("to"))
			throw new ConstructionException("Segment endpoints missing!");
		try
		{	PointObject p1=(PointObject)c.find(tag.getValue("from")); 
			PointObject p2=(PointObject)c.find(tag.getValue("to"));
			SegmentObject o=new SegmentObject(c,p1,p2);
			setName(tag,o);
			set(tree,o);
			c.add(o);
			setConditionals(tree,c,o);
			o.setArrow(tag.hasParam("arrow"));
			if (tag.hasParam("fixed"))
			{	try
				{	o.setFixed(true,tag.getValue("fixed"));
				}
				catch (Exception e)
				{	throw new ConstructionException("Fixed value illegal!");
				}
			}
		}
		catch (ConstructionException e)
		{	throw e;
		}
		catch (Exception e)
		{	throw new ConstructionException("Segment endpoints illegal!");
		}
		return true;
	}
	
	public String getPrompt ()
	{	return Zirkel.name("prompt.segment");
	}
	
	public String getTag () { return "Segment"; }
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
		if (!(P1 instanceof PointObject))
			throw new ConstructionException(Zirkel.name("exception.type")+" "+
				params[0]);
		ConstructionObject
			P2=c.find(params[1]);
		if (P2==null)
		{	Expression ex=new Expression(params[1],c,null);
			if (!ex.isValid())
				throw new ConstructionException(Zirkel.name("exception.expression"));
			double x=ex.getValue();
			P2=new PointObject(c,((PointObject)P1).getX()+x,
				((PointObject)P1).getY());
			c.add(P2);
			P2.setDefaults();
			SegmentObject s=new SegmentObject(c,(PointObject)P1,(PointObject)P2);
			s.setDefaults();
			s.setFixed(true,params[1]);
			c.add(s);
			if (!name.equals("")) s.setNameCheck(name);
			return;
		}
		if (!(P2 instanceof PointObject))
			throw new ConstructionException(Zirkel.name("exception.type")+" "+
				params[1]);
		SegmentObject s=new SegmentObject(c,(PointObject)P1,(PointObject)P2);
		if (nparams==3)
		{	if (!s.canFix())
				throw new ConstructionException(Zirkel.name("exception.canfix"));
			s.setFixed(true,params[2]);
			if (!s.isValidFix())
				throw new ConstructionException(Zirkel.name("exception.fix")+" "+
					params[2]);
			s.validate();				
		}
		c.add(s);
		s.setDefaults();
		if (!name.equals("")) s.setNameCheck(name);
	}
}
