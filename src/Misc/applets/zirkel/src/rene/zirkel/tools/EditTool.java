package rene.zirkel.tools;

// file: Hider.java

import java.awt.event.*;
import java.util.*;

import rene.zirkel.*;
import rene.zirkel.constructors.*;
import rene.zirkel.dialogs.*;
import rene.zirkel.objects.ConstructionObject;

public class EditTool extends ObjectConstructor
{	boolean Other;
	Vector V;
	
	public void mousePressed (MouseEvent e, ConstructionObject o, 
		ZirkelCanvas zc)
	{	double x=zc.x(e.getX()),y=zc.y(e.getY());
		Other=(e.isShiftDown() && o==null);
		if (o==null)
		{	o=zc.selectObject(e.getX(),e.getY());
			if (o==null) return;
		}
		if (o.isKeep()) return;
		if (Other)
		{	if (V==null) V=new Vector();
			V.addElement(o);
			o.setSelected(true);
			zc.repaint();
			return;
		}
		if (V!=null)
		{	V.addElement(o);
			o.setSelected(true);
			if (e.isControlDown())
			{	EditConditionals d=new EditConditionals(zc.getFrame(),V);
				zc.clearSelected();
				zc.repaint();
			}
			else
			{	ObjectsEditDialog d=new ObjectsEditDialog(zc.getFrame(),V);
				zc.clearSelected();
				d.setVisible(true);
			}
		}
		else if(e.isControlDown())
		{	new EditConditionals(zc.getFrame(),o);
			zc.validate();
			zc.repaint();
		}
		else
		{	String oldname=o.getName();
			o.edit(zc);
			if (!oldname.equals(o.getName())) zc.updateTexts(o,oldname);
		}
		V=null; Other=false;
		zc.validate();
		zc.repaint();
	}
	
	public void mouseMoved (MouseEvent e, ZirkelCanvas zc, boolean simple)
	{	zc.indicateObjects(e.getX(),e.getY(),true);
	}

	public void mousePressed (MouseEvent e, ZirkelCanvas zc)
	{	mousePressed(e,null,zc);
	}
	
	public void showStatus (ZirkelCanvas zc)
	{	zc.showStatus(Zirkel.name("message.edit"));
	}
	
	public void reset (ZirkelCanvas zc)
	{	zc.clearSelected();
		V=null; Other=false;
	}
}
