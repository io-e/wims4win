package rene.zirkel.tools;

// file: Hider.java

import java.awt.event.MouseEvent;

import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelCanvas;
import rene.zirkel.constructors.*;
import rene.zirkel.objects.ConstructionObject;

public class RenamerTool extends ObjectConstructor
{	boolean Enforce=false,Started=false;
	
	public void mousePressed (MouseEvent e, ZirkelCanvas zc)
	{	ConstructionObject o=zc.selectObject(e.getX(),e.getY());
		if (o==null) return;
		if (o.isKeep()) return;
		if (e.isShiftDown())
		{	zc.renameABC(o,true,!Started);
			Started=true;
		}
		else zc.renameABC(o,false,false);
	}
	
	public void mouseMoved (MouseEvent e, ZirkelCanvas zc, boolean simple)
	{	zc.indicateObjects(e.getX(),e.getY(),true);
	}

	public void showStatus (ZirkelCanvas zc)
	{	zc.showStatus(Zirkel.name("message.rename"));
	}
	
	public boolean useSmartBoard ()
	{	return false;
	}
	
	public void reset (ZirkelCanvas zc)
	{	Started=false;
	}
}
