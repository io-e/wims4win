package rene.zirkel.tools;

// file: SetRange.java

import java.awt.event.MouseEvent;

import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelCanvas;
import rene.zirkel.construction.Selector;
import rene.zirkel.constructors.*;
import rene.zirkel.objects.*;

public class SetFixedAngle extends ObjectConstructor
	implements Selector
{	ObjectConstructor OC;
	FixedAngleObject A;
	PointObject P1,P2,P3;
	public SetFixedAngle (ZirkelCanvas zc, FixedAngleObject a,
			ObjectConstructor oc)
	{	A=a; OC=oc;
		a.setSelected(true);
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
			}
			else if (o instanceof AngleObject || o instanceof FixedAngleObject
					|| o instanceof ExpressionObject)
			{	A.setFixed(o.getName());
				A.setDragable(false);
				A.updateText();
				reset(zc);
			}
		}
		else if (P2==null)
		{	P2=zc.selectPoint(e.getX(),e.getY());
			if (P2!=null)
			{	P2.setSelected(true);
				showStatus(zc);
				zc.repaint();
			}
		}
		else
		{	P3=zc.selectPoint(e.getX(),e.getY());
			if (P3==null) return;
			A.setFixed("a("+P1.getName()+","+P2.getName()+","+P3.getName()+")");
			reset(zc);
		}
	}
	public void mouseMoved (MouseEvent e, ZirkelCanvas zc, boolean simple)
	{	zc.indicateWithSelector(e.getX(),e.getY(),this);
	}
	public void showStatus (ZirkelCanvas zc)
	{	if (P1==null) zc.showStatus(
			Zirkel.name("message.setfixedangle.first"));
		else if (P2==null) zc.showStatus(
			Zirkel.name("message.setfixedangle.second"));
		else zc.showStatus(
			Zirkel.name("message.setfixedangle.third"));
	}
	public void reset (ZirkelCanvas zc)
	{	super.reset(zc);
		zc.setTool(OC);
		zc.validate();
		zc.repaint();
		zc.check();
	}
	public boolean useSmartBoard ()
	{	return P2!=null;
	}
	public boolean isAdmissible(ZirkelCanvas zc, ConstructionObject o)
	{	if ((o instanceof PointObject || o instanceof AngleObject 
			|| o instanceof FixedAngleObject || o instanceof ExpressionObject) &&
			!zc.getConstruction().dependsOn(o,A)) 
				return true;
		return false;
	}
}
