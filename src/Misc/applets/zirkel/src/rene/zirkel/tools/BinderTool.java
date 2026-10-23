package rene.zirkel.tools;

// file: Binder.java

import java.awt.event.*;

import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelCanvas;
import rene.zirkel.construction.*;
import rene.zirkel.constructors.*;
import rene.zirkel.objects.*;

public class BinderTool extends ObjectConstructor
	implements Selector
{	ObjectConstructor OC;
	PointObject P;
	
	public BinderTool (ZirkelCanvas zc, PointObject p, ObjectConstructor oc)
	{	P=p; OC=oc;
		P.setSelected(true);
		zc.repaint();
	}

	boolean Control;
	
	public void mousePressed (MouseEvent e, ZirkelCanvas zc)
	{	Control=e.isControlDown();
		ConstructionObject o=zc.selectWithSelector(e.getX(),e.getY(),this);
		if (o==null) return;
		if (Control && !(o instanceof InsideObject)) return;
		if (zc.getConstruction().dependsOn(o,P)) return;
		P.setBound(o.getName());
		if (o instanceof InsideObject)
		{	if (Control || !(o instanceof PointonObject)) P.setInside(true);
		}
		else P.setUseAlpha(!e.isShiftDown());
		zc.getConstruction().updateCircleDep();
		zc.repaint();
		reset(zc);
	}
	
	public void mouseMoved (MouseEvent e, ZirkelCanvas zc, boolean simple)
	{	Control=e.isControlDown();
		zc.indicateWithSelector(e.getX(),e.getY(),this);
	}

	public boolean isAdmissible (ZirkelCanvas zc, ConstructionObject o) 
	{	if ((o instanceof InsideObject || o instanceof PointonObject) && 
			!zc.getConstruction().dependsOn(o,P)) return true;
		return false;
	}
	
	public void showStatus (ZirkelCanvas zc)
	{	zc.showStatus(
			Zirkel.name("message.bindpoint"));
	}
	public void reset (ZirkelCanvas zc)
	{	super.reset(zc);
		zc.setTool(OC);
		zc.validate();
		zc.repaint();
	}
	public boolean useSmartBoard ()
	{	return false;
	}
}
