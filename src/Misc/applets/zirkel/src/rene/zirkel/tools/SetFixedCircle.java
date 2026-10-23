package rene.zirkel.tools;

// file: SetFixedCircle.java

import java.awt.event.MouseEvent;

import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelCanvas;
import rene.zirkel.construction.Selector;
import rene.zirkel.constructors.*;
import rene.zirkel.objects.*;

public class SetFixedCircle extends ObjectConstructor
	implements Selector
{	ObjectConstructor OC;
	FixedCircleObject C;
	PointObject P1,P2;
	public SetFixedCircle (ZirkelCanvas zc, FixedCircleObject c,
			ObjectConstructor oc)
	{	C=c; OC=oc;
		c.setSelected(true);
		zc.repaint();
	}
	public void mousePressed (MouseEvent e, ZirkelCanvas zc)
	{	if (P1==null)
		{	ConstructionObject o=
				zc.selectWithSelector(e.getX(),e.getY(),this);
			if (o==null) return;
			if (o instanceof PointObject)
			{	P1=(PointObject)o;
				P1.setSelected(true);
				showStatus(zc);
				zc.repaint();
				return;
			}
			C.setFixed(o.getName());
			C.setDragable(false);
			C.updateText();
			reset(zc);
		}
		else
		{	P2=zc.selectPoint(e.getX(),e.getY());
			if (P2==null) return;
			C.setFixed("d("+P1.getName()+","+P2.getName()+")");
			reset(zc);
		}
	}
	public void mouseMoved (MouseEvent e, ZirkelCanvas zc, boolean simple)
	{	zc.indicateWithSelector(e.getX(),e.getY(),this);
	}
	public void showStatus (ZirkelCanvas zc)
	{	if (P1==null) zc.showStatus(
			Zirkel.name("message.setfixedcircle.first"));
		else zc.showStatus(
			Zirkel.name("message.setfixedcircle.second"));
	}
	public void reset (ZirkelCanvas zc)
	{	super.reset(zc);
		zc.setTool(OC);
		zc.validate();
		showStatus(zc);
		zc.repaint();
		zc.check();
	}
	public boolean useSmartBoard ()
	{	return P1!=null;
	}
	public boolean isAdmissible(ZirkelCanvas zc, ConstructionObject o)
	{	if ((o instanceof PointObject || o instanceof SegmentObject 
			|| o instanceof FixedCircleObject ||
			o instanceof CircleObject || o instanceof ExpressionObject) &&
			!zc.getConstruction().dependsOn(o,C)) return true;
		return false;
	}
}
