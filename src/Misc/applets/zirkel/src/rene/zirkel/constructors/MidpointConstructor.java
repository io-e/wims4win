package rene.zirkel.constructors;

// file: MidpointConstructor.java

import rene.util.xml.XmlTag;
import rene.util.xml.XmlTree;
import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelCanvas;
import rene.zirkel.construction.Construction;
import rene.zirkel.construction.ConstructionException;
import rene.zirkel.objects.ConstructionObject;
import rene.zirkel.objects.MidpointObject;
import rene.zirkel.objects.PointObject;

public class MidpointConstructor extends LineConstructor
{	public ConstructionObject create (Construction c, PointObject p1, PointObject p2)
	{	return new MidpointObject(c,p1,p2);
	}
	public void showStatus (ZirkelCanvas zc)
	{	if (P1==null) zc.showStatus(
			Zirkel.name("message.midpoint.first","Midpoint: Set the first point!"));
		else zc.showStatus(
			Zirkel.name("message.midpoint.second","Midpoint: Set the second point!"));
	}
	
	public boolean construct (XmlTree tree, Construction c)
		throws ConstructionException
	{	if (!testTree(tree,"Midpoint")) return false;
		XmlTag tag=tree.getTag();
		if (!tag.hasParam("first") || !tag.hasParam("second"))
			throw new ConstructionException("Line points missing!");
		try
		{	PointObject p1=(PointObject)c.find(tag.getValue("first")); 
			PointObject p2=(PointObject)c.find(tag.getValue("second"));
			MidpointObject p=new MidpointObject(c,p1,p2);
			PointConstructor.setType(tag,p);
			setName(tag,p);
			set(tree,p);
			c.add(p);
			setConditionals(tree,c,p);
		}
		catch (ConstructionException e)
		{	throw e;
		}
		catch (Exception e)
		{	throw new ConstructionException("Midpoint points illegal!");
		}
		return true;
	}

	public String getPrompt ()
	{	return Zirkel.name("prompt.midpoint");
	}

	public String getTag () { return "Midpoint"; }
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
		if (!(P1 instanceof PointObject))
			throw new ConstructionException(Zirkel.name("exception.type")+" "+
				params[0]);
		ConstructionObject
			P2=c.find(params[1]);
		if (P2==null)
			throw new ConstructionException(Zirkel.name("exception.notfound")+" "+
				params[1]);
		if (!(P2 instanceof PointObject))
			throw new ConstructionException(Zirkel.name("exception.type")+" "+
				params[1]);
		MidpointObject s=new MidpointObject(c,(PointObject)P1,(PointObject)P2);
		if (!name.equals("")) s.setNameCheck(name);
		c.add(s);
		s.setDefaults();
	}

}
