package rene.zirkel.tools;

// file: Hider.java

import java.awt.event.*;

import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelCanvas;
import rene.zirkel.constructors.*;
import rene.zirkel.objects.ConstructionObject;

public class HiderTool extends ObjectConstructor
{	public void mousePressed (MouseEvent e, ZirkelCanvas zc)
	{	double x=zc.x(e.getX()),y=zc.y(e.getY());
		ConstructionObject o=zc.selectObject(e.getX(),e.getY());
		if (o==null) return;
		if (e.isShiftDown()) o.setSuperHidden(true);
		else o.setHidden(!o.isHidden());
		zc.repaint();
		zc.reloadCD();
	}
	public void mouseMoved (MouseEvent e, ZirkelCanvas zc, boolean simple)
	{	zc.indicateObjects(e.getX(),e.getY());
	}

	public void showStatus (ZirkelCanvas zc)
	{	zc.showStatus(
			Zirkel.name("message.hide","Hide: Select an object!"));
	}
}
