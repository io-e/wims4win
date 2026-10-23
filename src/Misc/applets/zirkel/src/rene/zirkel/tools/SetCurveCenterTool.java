/*
 * Created on 06.11.2005
 *
 */
package rene.zirkel.tools;

import java.awt.event.MouseEvent;

import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelCanvas;
import rene.zirkel.constructors.ObjectConstructor;
import rene.zirkel.objects.*;

public class SetCurveCenterTool extends ObjectConstructor
{	ObjectConstructor OC;
	FunctionObject P;
	public SetCurveCenterTool (ZirkelCanvas zc, FunctionObject p,
		ObjectConstructor oc)
	{	P=p; OC=oc;
		P.setSelected(true);
		zc.repaint();
	}
	public void mousePressed (MouseEvent e, ZirkelCanvas zc)
	{	ConstructionObject o=zc.selectPoint(e.getX(),e.getY());
		if (o==null) return;
		P.setCenter(o.getName());
		reset(zc);
	}
	public void showStatus (ZirkelCanvas zc)
	{	zc.showStatus(
			Zirkel.name("message.setcenter"));
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

