package rene.zirkel.constructors;

// file: ParallelConstructor.java

import java.awt.event.*;

import rene.util.xml.*;
import rene.zirkel.*;
import rene.zirkel.construction.*;
import rene.zirkel.objects.*;

public class ParallelConstructor extends ObjectConstructor
{	PointObject P=null;
	PrimitiveLineObject L=null;
	public void mousePressed (MouseEvent e, ZirkelCanvas zc)
	{	if (!zc.Visual) return;
		if (L==null)
		{	L=selectline(e.getX(),e.getY(),zc);
			if (L!=null)
			{	L.setSelected(true);
				zc.repaint();
			}
			showStatus(zc);
		}
		else
		{	P=select(e.getX(),e.getY(),zc);
			if (P!=null)
			{	ConstructionObject o=create(zc.getConstruction(),L,P);
				zc.addObject(o);
				o.setDefaults();
				P=null; L=null;
				zc.clearSelected();
				showStatus(zc);
			}
		}
	}
	public boolean waitForLastPoint ()
	{	return L!=null;
	}
	public void finishConstruction (MouseEvent e, ZirkelCanvas zc)
	{	P=select(e.getX(),e.getY(),zc);
		if (P!=null)
		{	ConstructionObject o=create(zc.getConstruction(),L,P);
			zc.addObject(o);
			o.setDefaults();
			zc.validate();
			zc.repaint();
			P=null;
		}	
	}
	public void mouseMoved (MouseEvent e, ZirkelCanvas zc, boolean simple)
	{	if (!simple && waitForLastPoint())
		{	if (zc.isPreview())
			{	zc.movePreview(e);
			}
			else
			{	zc.prepareForPreview(e);
				finishConstruction(e,zc);
			}
		}
		if (L==null) zc.indicateLineObjects(e.getX(),e.getY());
		else if (P==null) zc.indicateCreatePoint(e.getX(),e.getY(),false);
	}
	public PointObject select (int x, int y, ZirkelCanvas zc)
	{	return zc.selectCreatePoint(x,y);
	}
	public PrimitiveLineObject selectline (int x, int y, ZirkelCanvas zc)
	{	return zc.selectLine(x,y);
	}
	public PrimitiveLineObject create (Construction c, PrimitiveLineObject l, PointObject p)
	{	return new ParallelObject(c,l,p);
	}
	public void reset (ZirkelCanvas zc)
	{	super.reset(zc);
		if (zc.Visual)
		{	P=null; L=null;
			showStatus(zc);
		}
		else
		{	zc.setPrompt(getPrompt());
		}
	}
	public String getPrompt ()
	{	return Zirkel.name("prompt.parallel");
	}
	public void showStatus (ZirkelCanvas zc)
	{	if (L==null) zc.showStatus(
			Zirkel.name("message.parallel.first","Parallel: Choose a line!"));
		else zc.showStatus(
			Zirkel.name("message.parallel.second","Parallel: Choose a Point!"));
	}
	public boolean construct (XmlTree tree, Construction c)
		throws ConstructionException
	{	if (!testTree(tree,"Parallel")) return false;
		XmlTag tag=tree.getTag();
		if (!tag.hasParam("point") || !tag.hasParam("line"))
			throw new ConstructionException("Parallel parameters missing!");
		try
		{	PointObject p1=(PointObject)c.find(tag.getValue("point"));
			PrimitiveLineObject p2=(PrimitiveLineObject)c.find(tag.getValue("line"));
			ParallelObject o=new ParallelObject(c,p2,p1);
			setName(tag,o);
			set(tree,o);
			if (tag.hasParam("partial")) o.setPartial(true);
			c.add(o);
			setConditionals(tree,c,o);
		}
		catch (ConstructionException e)
		{	throw e;
		}
		catch (Exception e)
		{	throw new ConstructionException("Parallel parameters illegal!");
		}
		return true;
	}

	public String getTag () { return "Parallel"; }
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
		if (!(P1 instanceof PrimitiveLineObject))
			throw new ConstructionException(Zirkel.name("exception.type")+" "+
				params[0]);
		if (!(P2 instanceof PointObject))
			throw new ConstructionException(Zirkel.name("exception.type")+" "+
				params[1]);
		ParallelObject s=new ParallelObject(c,
			(PrimitiveLineObject)P1,(PointObject)P2);
		if (!name.equals("")) s.setNameCheck(name);
		c.add(s);
		s.setDefaults();
	}

}
