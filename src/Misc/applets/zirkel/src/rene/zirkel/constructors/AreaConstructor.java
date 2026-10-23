package rene.zirkel.constructors;

// file: PointConstructor.java

import java.awt.event.*;
import java.util.*;

import rene.util.xml.*;
import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelCanvas;
import rene.zirkel.construction.Construction;
import rene.zirkel.construction.ConstructionException;
import rene.zirkel.objects.AreaObject;
import rene.zirkel.objects.ConstructionObject;
import rene.zirkel.objects.PointObject;

public class AreaConstructor extends ObjectConstructor
{	Vector Points=new Vector();
	
	public void mousePressed (MouseEvent e, ZirkelCanvas zc)
	{	if (!zc.Visual) return;
		double x=zc.x(e.getX()),y=zc.y(e.getY());
		PointObject P=zc.selectCreatePoint(e.getX(),e.getY());
		if (P!=null)
		{	P.setSelected(true);
			Enumeration en=Points.elements();
			while (en.hasMoreElements())
			{	if (en.nextElement()==P)
				{	if (Points.size()>=3)
					{	AreaObject o=new AreaObject(zc.getConstruction(),Points);
						zc.addObject(o);
						o.setDefaults();		
						if (!o.isSolid()) o.setBack(true);
					}
					reset(zc);
					return;
				}
			}
			Points.addElement(P);
			zc.repaint();
		}
	}

	public String getTag () { return "Polygon"; }

	public void construct (Construction c, 
		String name, String params[], int nparams)
		throws ConstructionException
	{	if (nparams<3)
			throw new ConstructionException(Zirkel.name("exception.nparams"));
		Vector v=new Vector();
		for (int i=0; i<nparams; i++)
		{	ConstructionObject o=c.find(params[i]);
			if (o==null)
				throw new ConstructionException(Zirkel.name("exception.notfound")+" "+
					params[i]);
			if (!(o instanceof PointObject))
				throw new ConstructionException(Zirkel.name("exception.type")+" "+
					params[i]);
			v.addElement(o);
		}
		AreaObject o=new AreaObject(c,v);
		if (!name.equals("")) o.setNameCheck(name);
		c.add(o);
		o.setDefaults();
		o.setBack(true);
	}

	public boolean construct (XmlTree tree, Construction c)
		throws ConstructionException
	{	if (!testTree(tree,"Polygon")) return false;
		XmlTag tag=tree.getTag();
		try
		{	int i=1;
			Vector v=new Vector();
			while (true)
			{	PointObject p=(PointObject)c.find(tag.getValue("point"+i));
				if (p==null) break;
				v.addElement(p);
				i++;
			}
			AreaObject o=new AreaObject(c,v);
			o.setBack(true);
			setName(tag,o);
			set(tree,o);
			c.add(o);
			setConditionals(tree,c,o);
		}
		catch (ConstructionException e)
		{	throw e;
		}
		catch (Exception e)
		{	throw new ConstructionException("Polygon parameters illegal!");
		}
		return true;
	}


	public void showStatus (ZirkelCanvas zc)
	{	zc.showStatus(Zirkel.name("message.area"));
		zc.setPrompt("="+Zirkel.name("prompt.area"));
	}

	public void reset (ZirkelCanvas zc)
	{	super.reset(zc);
		Points=new Vector();
		zc.showStatus(Zirkel.name("message.area"));
	}
	
}
