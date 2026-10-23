package rene.zirkel.tools;

import java.awt.event.*;

import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelCanvas;
import rene.zirkel.constructors.*;
import rene.zirkel.objects.ConstructionObject;
import rene.zirkel.objects.IntersectionObject;

/**
 * A tool to set the the point an intersection point should stay away from
 * or should come close to.  
 */

public class SetAwayTool extends ObjectConstructor
{	ObjectConstructor OC;
	IntersectionObject P;
	boolean Away;
	
	public SetAwayTool (ZirkelCanvas zc, IntersectionObject p, boolean away,
		ObjectConstructor oc)
	{	P=p; OC=oc;
		P.setSelected(true);
		Away=away;
		zc.repaint();
	}
	
	public void mousePressed (MouseEvent e, ZirkelCanvas zc)
	{	ConstructionObject o=zc.selectPoint(e.getX(),e.getY());
		if (o==null) return;
		if (zc.getConstruction().dependsOn(o,P))
		{	zc.warning(ConstructionObject.text1(Zirkel.name("error.depends"),P.getText()));
			return;
		}
		P.setAway(o.getName(),Away);
		P.setUseAlpha(e.isShiftDown());
		zc.validate();
		reset(zc);
	}
	
	public void showStatus (ZirkelCanvas zc)
	{	if (Away) zc.showStatus(
			Zirkel.name("message.setaway.away"));
		else zc.showStatus(
			Zirkel.name("message.setaway.close"));
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
