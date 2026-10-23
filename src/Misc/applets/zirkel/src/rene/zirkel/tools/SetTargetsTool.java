package rene.zirkel.tools;

// file: SetParameter.java

import java.awt.event.*;

import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelCanvas;
import rene.zirkel.constructors.*;
import rene.zirkel.objects.ConstructionObject;

public class SetTargetsTool extends ObjectConstructor
{	public void mousePressed (MouseEvent e, ZirkelCanvas zc)
	{	double x=zc.x(e.getX()),y=zc.y(e.getY());
		ConstructionObject o=zc.selectConstructableObject(e.getX(),e.getY());
		if (o==null || !o.isFlag()) return;
		o.setTarget(true);
		if (o.isTarget())
		{	o.setSelected(true);
			zc.getConstruction().addTarget(o);
			zc.repaint();
		}
	}
	public void mouseMoved (MouseEvent e, ZirkelCanvas zc, boolean simple)
	{	zc.indicateConstructableObjects(e.getX(),e.getY());
	}
	public void reset (ZirkelCanvas zc)
	{	super.reset(zc);
		zc.clearSelected();
		zc.getConstruction().clearTargets();
		zc.getConstruction().determineConstructables();
	}
	public void showStatus (ZirkelCanvas zc)
	{	zc.showStatus(
			Zirkel.name("message.targets",
				"Macro Targets: Select the Targets!"));
	}
	public boolean useSmartBoard ()
	{	return false;
	}
}
