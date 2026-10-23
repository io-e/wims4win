package rene.zirkel.tools;

// file: MetaMover.java

import java.awt.Cursor;
import java.awt.event.MouseEvent;

import rene.gui.Global;
import rene.zirkel.ZirkelCanvas;
import rene.zirkel.constructors.*;
import rene.zirkel.objects.ConstructionObject;
import rene.zirkel.objects.FixedAngleObject;
import rene.zirkel.objects.FixedCircleObject;
import rene.zirkel.objects.MoveableObject;

public class MetaMover extends MoverTool
{	ObjectConstructor OC;
	ConstructionObject PP;
	
	public MetaMover (ObjectConstructor oc, ZirkelCanvas zc, ConstructionObject p,
			MouseEvent e)
	{	OC=oc; PP=P=p;
		oc.pause(true);
		if (P!=null)
		{	P.setSelected(true);
			ShowsValue=P.showValue();
			ShowsName=P.showName();
			zc.repaint();
			showStatus(zc);
			zc.setCursor(new Cursor(Cursor.MOVE_CURSOR));
			if (P instanceof MoveableObject)
			{	((MoveableObject)P).startDrag(zc.x(e.getX()),zc.y(e.getY()));
			}
			if (ZCG!=null)
			{	ZCG.dispose();
				ZCG=null;
			}
			ZCG=zc.getGraphics();
		}
	}
	
	public void mouseReleased (MouseEvent e, ZirkelCanvas zc)
	{	if (P==null) return;
		if (ZCG!=null)
		{	ZCG.dispose();
			ZCG=null;
		}		
		zc.setCursor(Cursor.getDefaultCursor());
		P.setSelected(false);
		P.setShowValue(ShowsValue);
		P.setShowName(ShowsName);
		if (zc.showGrid() && !Global.getParameter("grid.leftsnap",false))
		{	PP.snap(zc);
			// PP.round();
			PP.updateText();
		}
		zc.validate();
		if (Grab)
		{	zc.grab(false);
			Grab=false;
		}
		if (ChangedDrawable)
		{	if (P instanceof FixedCircleObject)
			{	((FixedCircleObject)P).setDragable(WasDrawable);
			}
			else if (P instanceof FixedAngleObject)
			{	((FixedAngleObject)P).setDragable(WasDrawable);
			}
		}
		zc.clearSelected();
		zc.repaint();
		P=null;
		V=null;
		Selected=false;
		zc.setTool(OC);
		OC.pause(false);
		zc.validate();
		zc.repaint();
	}
}
