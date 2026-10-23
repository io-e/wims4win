/*
 * Created on 19.11.2005
 *
 */
package rene.zirkel.constructors;

import java.awt.event.MouseEvent;
import rene.util.xml.XmlTag;
import rene.util.xml.XmlTree;
import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelCanvas;
import rene.zirkel.construction.Construction;
import rene.zirkel.construction.ConstructionException;
import rene.zirkel.objects.ConstructionObject;
import rene.zirkel.objects.ImageObject;
import rene.zirkel.objects.PointObject;

public class ImageConstructor extends ObjectConstructor
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
		if (NPoints==3)
		{	String filename=zc.loadImage();
			if (filename.equals(""))
			{	reset(zc);
				return;
			}
			ImageObject o=new ImageObject(zc.getConstruction(),P,filename);
			zc.addObject(o);
			zc.clearSelected();
			reset(zc);
			zc.repaint();
		}
	}

	public void showStatus (ZirkelCanvas zc)
	{	if (NPoints<=1) zc.showStatus(ConstructionObject.text1(
			Zirkel.name("message.image"),""+(NPoints+1)));
		else zc.showStatus(Zirkel.name("message.image.last"));
	}
	
	public void reset (ZirkelCanvas zc)
	{	super.reset(zc);
		if (zc.Visual)
		{	P=new PointObject[3]; NPoints=0;
			showStatus(zc);
		}
		else
		{	zc.setPrompt(Zirkel.name("prompt.image"));
		}
	}

	public boolean construct (XmlTree tree, Construction c)
		throws ConstructionException
	{	if (!testTree(tree,"Image")) return false;
		XmlTag tag=tree.getTag();
		for (int i=0; i<3; i++)
			if (!tag.hasParam("point"+(i+1)))
				throw new ConstructionException("Image points missing!");
		if (!tag.hasParam("filename"))
			throw new ConstructionException("Image filename missing!");
		try
		{	PointObject P[]=new PointObject[3];
			for (int i=0; i<3; i++)
				P[i]=(PointObject)c.find(tag.getValue("point"+(i+1))); 
			String filename=tag.getValue("filename");
			ImageObject p=new ImageObject(c,P,filename);
			setName(tag,p);
			set(tree,p);
			c.add(p);
			setConditionals(tree,c,p);
		}
		catch (ConstructionException e)
		{	throw e;
		}
		catch (Exception e)
		{	throw new ConstructionException("Image points illegal!");
		}
		return true;
	}

	public String getPrompt ()
	{	return Zirkel.name("prompt.image");
	}

	public String getTag () { return "Image"; }

	public void construct (Construction c, 
		String name, String params[], int nparams)
		throws ConstructionException
	{	
	}

}

