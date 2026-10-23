/*
 * Created on 19.11.2005
 *
 */
package rene.zirkel.objects;

import java.awt.Image;
import java.util.Enumeration;

import rene.util.FileName;
import rene.util.xml.XmlWriter;
import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelCanvas;
import rene.zirkel.construction.Construction;
import rene.zirkel.construction.Count;
import rene.zirkel.dialogs.ObjectEditDialog;
import rene.zirkel.graphics.MyGraphics;

public class ImageObject extends ConstructionObject
{	PointObject P[];
	static Count N=new Count();
	String Filename;
	Image I;
	
	public ImageObject (Construction c, PointObject p[], String filename)
	{	super(c);
		P=p;
		Filename=FileName.filename(filename);
		updateText();
	}

	public String getTag () { return "Image"; }
	public int getN () { return N.next(); }
	
	public void updateText ()
	{	try
		{	String Names[]=new String[P.length];
			for (int i=0; i<P.length; i++) Names[i]=P[i].getName();
			setText(textAny(Zirkel.name("text.image"),Names));
		} 
		catch (Exception e) {}
	}
	
	public void validate ()
	{
	}
	
	public void paint (MyGraphics g, ZirkelCanvas zc)
	{	if (!Valid || mustHide(zc)) return;
		if (I==null)
		{	I=zc.doLoadImage(Filename);
			if (I==null || I.getWidth(zc)==0 || I.getHeight(zc)==0)
			{	Valid=false; return;
			}
			else Valid=true;
		}
		if (P[1]==P[2] || P[0]==P[2])
		{	int w=I.getWidth(zc),h=I.getWidth(zc);
			double dx=P[1].getX()-P[0].getX(),dy=P[1].getY()-P[0].getY();
			g.drawImage(I,
				zc.col(P[0].getX()),zc.row(P[0].getY()),
				zc.col(P[1].getX()),zc.row(P[1].getY()),
				zc.col(P[0].getX()+dy),zc.row(P[0].getY()-dx),
				zc);
		}
		g.drawImage(I,
			zc.col(P[0].getX()),zc.row(P[0].getY()),
			zc.col(P[1].getX()),zc.row(P[1].getY()),
			zc.col(P[2].getX()),zc.row(P[2].getY()),
			zc);
	}
	
	public boolean nearto (int cc, int rr, ZirkelCanvas zc)
	{	if (!displays(zc)) return false;
		return P[0].nearto(cc,rr,zc) || P[1].nearto(cc,rr,zc) || P[2].nearto(cc,rr,zc);
	}
		
	public void edit (ZirkelCanvas zc)
	{	new ObjectEditDialog(zc.getFrame(),"",this).setVisible(true);
		zc.repaint();
	}

	public void printArgs (XmlWriter xml)
	{	for (int i=0; i<P.length; i++)
			xml.printArg("point"+(i+1),P[i].getName());
		xml.printArg("filename",FileName.filename(Filename));
	}

	public Enumeration depending ()
	{	DL.reset();
		for (int i=0; i<P.length; i++) DL.add(P[i]);
		return DL.elements();
	}

	public void translate ()
	{	for (int i=0; i<P.length; i++)
			P[i]=(PointObject)P[i].getTranslation();
	}

	public ConstructionObject copy ()
	{	try
		{	QuadricObject o=(QuadricObject)clone();
			setTranslation(o);
			o.P=new PointObject[P.length];
			for (int i=0; i<P.length; i++) o.P[i]=P[i];
			o.translateConditionals();
			o.translate();
			o.setName();
			o.updateText();
			o.setBreak(false);
			o.setTarget(false);
			return o;
		}
		catch (Exception e)
		{	return null; 
		}
	}
	
	public boolean onlynearto (int x, int y, ZirkelCanvas zc)
	{	return nearto(x,y,zc);
	}

	public boolean hasUnit ()
	{	return false;
	}
}
