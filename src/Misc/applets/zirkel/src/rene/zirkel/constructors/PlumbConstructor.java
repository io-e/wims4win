package rene.zirkel.constructors;

// file: PlumbConstructor.java

import rene.util.xml.XmlTag;
import rene.util.xml.XmlTree;
import rene.zirkel.*;
import rene.zirkel.construction.*;
import rene.zirkel.objects.*;

public class PlumbConstructor extends ParallelConstructor
{	public PrimitiveLineObject create (Construction c, PrimitiveLineObject l, PointObject p)
	{	return new PlumbObject(c,l,p);
	}
	public void showStatus (ZirkelCanvas zc)
	{	if (L==null) zc.showStatus(
			Zirkel.name("message.plumb.first","Plumb Line: Choose a line!"));
		else zc.showStatus(
			Zirkel.name("message.plumb.second","Plumb Line: Choose a Point!"));
	}
	public boolean construct (XmlTree tree, Construction c)
		throws ConstructionException
	{	if (!testTree(tree,"Plumb")) return false;
		XmlTag tag=tree.getTag();
		if (!tag.hasParam("point") || !tag.hasParam("line"))
			throw new ConstructionException("Plumb parameters missing!");
		try
		{	PointObject p1=(PointObject)c.find(tag.getValue("point"));
			PrimitiveLineObject p2=(PrimitiveLineObject)c.find(tag.getValue("line"));
			PlumbObject o=new PlumbObject(c,p2,p1);
			if (tag.hasParam("valid"))
					o.setRestricted(false);
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
		{	throw new ConstructionException("Plumb parameters illegal!");
		}
		return true;
	}
	public String getPrompt ()
	{	return Zirkel.name("prompt.plumb");
	}

	public String getTag () { return "Plumb"; }
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
		PlumbObject s=new PlumbObject(c,
			(PrimitiveLineObject)P1,(PointObject)P2);
		if (!name.equals("")) s.setNameCheck(name);
		c.add(s);
		s.setDefaults();
	}

}
