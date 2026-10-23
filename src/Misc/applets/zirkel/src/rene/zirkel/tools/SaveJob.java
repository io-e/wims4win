package rene.zirkel.tools;

// file: SetParameter.java

import java.awt.event.*;

import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelCanvas;
import rene.zirkel.constructors.*;
import rene.zirkel.objects.*;

public class SaveJob extends ObjectConstructor
{	public ConstructionObject Last;
	public void mousePressed (MouseEvent e, ZirkelCanvas zc)
	{	double x=zc.x(e.getX()),y=zc.y(e.getY());
		ConstructionObject o=zc.selectObject(e.getX(),e.getY());
		if (o==null) return;
		o.setSelected(true);
		if (Last==null) 
		{	Last=o;
			zc.setJob(Last);
			showStatus(zc);
		}
		else
		{	if (e.isShiftDown()) zc.addJobTarget(o,true);
			else zc.addJobTarget(o,false);
		}
		zc.repaint();
	}
	public void mouseMoved (MouseEvent e, ZirkelCanvas zc, boolean simple)
	{	zc.indicateObjects(e.getX(),e.getY());
	}

	public void reset (ZirkelCanvas zc)
	{	super.reset(zc);
		zc.clearSelected();
		zc.getConstruction().clearTargets();
		zc.clearTargets();
		Last=null;
		showStatus(zc);
		zc.clearJob();
	}
	public void showStatus (ZirkelCanvas zc)
	{	if (Last==null)
			zc.showStatus(
				Zirkel.name("message.savejob.first"));
		else
			zc.showStatus(
				Zirkel.name("message.savejob.second"));
	}
	public boolean useSmartBoard ()
	{	return false;
	}
}
