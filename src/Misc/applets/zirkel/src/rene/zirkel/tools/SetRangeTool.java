package rene.zirkel.tools;

// file: SetRange.java

import java.awt.event.MouseEvent;

import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelCanvas;
import rene.zirkel.constructors.*;
import rene.zirkel.objects.PointObject;
import rene.zirkel.objects.PrimitiveCircleObject;

public class SetRangeTool extends ObjectConstructor
{	ObjectConstructor OC;
	PrimitiveCircleObject C;
	PointObject P1,P2;
	public SetRangeTool (ZirkelCanvas zc, PrimitiveCircleObject c,
			ObjectConstructor oc)
	{	C=c; OC=oc;
		C.setSelected(true);
		zc.repaint();
	}
	public void mousePressed (MouseEvent e, ZirkelCanvas zc)
	{	if (P1==null)
		{	P1=zc.selectPoint(e.getX(),e.getY());
			if (P1!=null)
			{	P1.setSelected(true);
				showStatus(zc);
				zc.repaint();
			}
		}
		else
		{	P2=zc.selectPoint(e.getX(),e.getY());
			if (P2==null) return;
			C.setRange(P1.getName(),P2.getName());
			reset(zc);
		}
	}
	public void mouseMoved (MouseEvent e, ZirkelCanvas zc, boolean simple)
	{	zc.indicatePointObjects(e.getX(),e.getY());
	}
	public void showStatus (ZirkelCanvas zc)
	{	if (P1==null) zc.showStatus(
			Zirkel.name("message.range.first"));
		else zc.showStatus(
			Zirkel.name("message.range.second"));
	}
	public void reset (ZirkelCanvas zc)
	{	super.reset(zc);
		zc.setTool(OC);
		zc.validate();
		zc.repaint();
	}
}
