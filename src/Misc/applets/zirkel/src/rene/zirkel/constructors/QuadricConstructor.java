package rene.zirkel.constructors;

// file: QuadricConstructor.java

import java.awt.event.*;

import rene.util.xml.*;
import rene.zirkel.*;
import rene.zirkel.construction.*;
import rene.zirkel.objects.*;

public class QuadricConstructor extends ObjectConstructor
{	PointObject P[];
	int NPoints;
	public void mousePressed (MouseEvent e, ZirkelCanvas zc)
	{	if (!zc.Visual) return;
		PointObject p=zc.selectCreatePoint(e.getX(),e.getY());
		if (p!=null)
		{	P[NPoints++]=p;
			p.setSelected(true);
			zc.repaint();
		}
		showStatus(zc);
		if (NPoints==5)
		{	QuadricObject Q=new QuadricObject(zc.getConstruction(),P);
			zc.addObject(Q);
			Q.setDefaults();
			zc.clearSelected();
			reset(zc);
			zc.repaint();
		}
	}

	public void showStatus (ZirkelCanvas zc)
	{	zc.showStatus(Zirkel.name("message.quadric")+" "+(NPoints+1));
	}
	
	public void reset (ZirkelCanvas zc)
	{	super.reset(zc);
		if (zc.Visual)
		{	P=new PointObject[5]; NPoints=0;
			showStatus(zc);
		}
		else
		{	zc.setPrompt(Zirkel.name("prompt.quadric"));
		}
	}

	public boolean construct (XmlTree tree, Construction c)
		throws ConstructionException
	{	if (!testTree(tree,"Quadric")) return false;
		XmlTag tag=tree.getTag();
		for (int i=0; i<5; i++)
		if (!tag.hasParam("point"+(i+1)))
			throw new ConstructionException("Quadric points missing!");
		try
		{	PointObject P[]=new PointObject[5];
			for (int i=0; i<5; i++)
				P[i]=(PointObject)c.find(tag.getValue("point"+(i+1))); 
			QuadricObject p=new QuadricObject(c,P);
			setName(tag,p);
			set(tree,p);
			c.add(p);
			setConditionals(tree,c,p);
		}
		catch (ConstructionException e)
		{	throw e;
		}
		catch (Exception e)
		{	throw new ConstructionException("Quadric points illegal!");
		}
		return true;
	}

	public String getPrompt ()
	{	return Zirkel.name("prompt.quadric");
	}

	public String getTag () { return "Quadric"; }

	public void construct (Construction c, 
		String name, String params[], int nparams)
		throws ConstructionException
	{	if (nparams!=5)
			throw new ConstructionException(Zirkel.name("exception.nparams"));
		ConstructionObject P[]=new PointObject[5];
		for (int i=0; i<5; i++)
		{
			P[i]=c.find(params[i]);
			if (P[i]==null)
				throw new ConstructionException(Zirkel.name("exception.notfound")+" "+
					params[i]);
			if (!(P[i] instanceof PointObject))
				throw new ConstructionException(Zirkel.name("exception.type")+" "+
					params[i]);
		}
		QuadricObject s=new QuadricObject(c,(PointObject[])P);
		if (!name.equals("")) s.setNameCheck(name);
		c.add(s);
		s.setDefaults();
	}

}
