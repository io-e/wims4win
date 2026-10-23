package rene.zirkel.tools;


import java.awt.*;
import java.awt.event.*;
import java.util.*;

import rene.gui.Global;
import rene.util.*;
import rene.zirkel.*;
import rene.zirkel.objects.*;
import rene.zirkel.constructors.*;

/**
 * @author Rene
 * Tool to move objects around. Can move several objects at once.
 */
public class MoverTool extends ObjectConstructor
{	ConstructionObject P;
	boolean Selected=false;
	boolean ShowsValue,ShowsName;
	boolean Grab;
	boolean WasDrawable,ChangedDrawable;
	MyVector V=null;
	double OldX,OldY;
	Graphics ZCG;

	public void mousePressed (MouseEvent e, ZirkelCanvas zc)
	{	if (!Selected && V!=null) zc.clearSelected();
		if (e.isControlDown() && V==null)
			// force a fixed angle or fixed circle to become drawable!
		{	P=zc.selectObject(e.getX(),e.getY());
			ChangedDrawable=false;
			if (P instanceof FixedCircleObject &&
				((FixedCircleObject)P).fixedByNumber())
			{	WasDrawable=((FixedCircleObject)P).isDragable();
				ChangedDrawable=true;
				((FixedCircleObject)P).setDragable(true);
			}
			else if (P instanceof FixedAngleObject &&
				((FixedAngleObject)P).fixedByNumber())
			{	WasDrawable=((FixedAngleObject)P).isDragable();
				ChangedDrawable=true;
				((FixedAngleObject)P).setDragable(true);
			}
			else P=null;
		}
		if (P==null) // no forced moveable fixed circle or angle
		{	if (V!=null) // try to select another point 
				P=zc.selectMoveablePoint(e.getX(),e.getY());
			else // try to select any moveable object 
				P=zc.selectMoveableObject(e.getX(),e.getY());
		}
		if (P!=null && V!=null) 
			// Check, if we have that point already:
		{	Enumeration en=V.elements();
			while (en.hasMoreElements())
			{	if (P==en.nextElement()) // point found
				{	if (e.isShiftDown()) P=null; 
						// selected a point twice, but want still more points 
					else V.removeElement(P);
						// remove the point from the list and start dragging
					break;
				}
			}
		}			
		if (P!=null) // point is selected for movement
		{	P.setSelected(true);
			ShowsName=P.showName();
			ShowsValue=P.showValue();
			zc.setCursor(new Cursor(Cursor.MOVE_CURSOR));
		}
		else // point was chosen from a list of objects
		{	Selected=(zc.findSelectedObject()!=null);
		}
		// Highlight all selected points:
		if (V!=null)
		{	Enumeration en=V.elements();
			while (en.hasMoreElements())
			{	((PointObject)en.nextElement()).setSelected(true);
			}
		}
		if (P!=null)
		{	if (P instanceof PointObject)
			{	if (e.isShiftDown()) // want more points!
				{	if (V==null) V=new MyVector();
					if (P!=null) V.addElement(P);
					P=null;
				}
				else if (e.isControlDown())
					// show current construction while moving
				{	zc.grab(true);
					Grab=true;
				}
				else
					// remember old point position 
				{	OldX=((PointObject)P).getX(); OldY=((PointObject)P).getY();
				}
			}
			else if (P instanceof MoveableObject)
				// call startDrag method of the object
			{	((MoveableObject)P).startDrag(
					zc.x(e.getX()),zc.y(e.getY()));
				// System.out.println("start dragging "+P.getName());
				V=null;
			}
			if (ZCG!=null)
			{	ZCG.dispose();
				ZCG=null;
			}
			ZCG=zc.getGraphics();
		}
		zc.repaint();
		showStatus(zc);
	}
	
	public void mouseMoved (MouseEvent e, ZirkelCanvas zc, boolean simple)
	{	zc.indicateMoveableObjects(e.getX(),e.getY(),e.isControlDown());
	}
	
	boolean IntersectionBecameInvalid=false;
	
	/**
	 * Drag a point in a new location and recompute the construction.
	 * Can show the name and the value of the object to be dragged.
	 * Take care not to move the point while ZirkelCanvas.paint is active.
	 * ZirkelCanvas.paint is synchronized!
	 * @see rene.zirkel.ObjectConstructor#mouseDragged(java.awt.event.MouseEvent, rene.zirkel.ZirkelCanvas)
	 */
	public void mouseDragged (MouseEvent e, ZirkelCanvas zc)
	{	if (P==null) return;
		// System.out.println("dragging "+P.getName());
		zc.getConstruction().haveChanged();
		zc.getConstruction().dontAlternate(false);
		synchronized (zc)
		{	double oldx=0,oldy=0;
			if ((Global.getParameter("restrictedmove",false) || e.isShiftDown()) 
					&& P instanceof PointObject)
			{	if (IntersectionBecameInvalid)
				{	zc.getConstruction().dontAlternate(true);
					IntersectionBecameInvalid=false;
				}
				oldx=((PointObject)P).getX(); oldy=((PointObject)P).getY();
			}
			((MoveableObject)P).dragTo(zc.x(e.getX()),zc.y(e.getY()));
			if ((Global.getParameter("options.movename",false) && !P.isFixed())
				||
				(Global.getParameter("options.movefixname",true) && P.isFixed()))
			{	P.setShowValue(true);
				P.setShowName(true);
			}
			P.updateText();
			if (V!=null && (P instanceof PointObject))
			{	double dx=((PointObject)P).getX()-OldX,dy=((PointObject)P).getY()-OldY;
				Enumeration en=V.elements();
				while (en.hasMoreElements())
				{	PointObject p=(PointObject)en.nextElement();
					p.move(p.getX()+dx,p.getY()+dy);
					p.updateText();
				}
				OldX=((PointObject)P).getX(); OldY=((PointObject)P).getY();
			}
			if (P instanceof PointObject)
			{	PointObject Po=(PointObject)P;
				if (Po.dependsOnItselfOnly())
				{	Po.dontUpdate(true);
					zc.dovalidate();
					Po.dontUpdate(false);
				}
			}
			zc.validate();
			if ((Global.getParameter("restrictedmove",false) || e.isShiftDown()) 
					&& P instanceof PointObject 
					&& zc.getConstruction().intersectionBecameInvalid())
			{	((PointObject)P).dragTo(oldx,oldy);
				IntersectionBecameInvalid=true;
				zc.validate();
			}
		}
		if (ZCG==null) ZCG=zc.getGraphics();
		zc.paint(ZCG);
	}
	
	/**
	 * Release the mouse after dragging.
	 * @param e
	 * @param zc
	 * @param rightmouse Call comes from a right mouse drag!
	 */
	public void mouseReleased (MouseEvent e, ZirkelCanvas zc, boolean rightmouse)
	{	if (P==null) return;
		if (ZCG!=null)
		{	ZCG.dispose();
			ZCG=null;
		}	
		zc.setCursor(Cursor.getDefaultCursor());
		P.setSelected(false);
		P.setShowValue(ShowsValue);
		P.setShowName(ShowsName);
		if (zc.showGrid() && !rightmouse && 
				Global.getParameter("grid.leftsnap",false))
		{	if (P instanceof PointObject && V!=null)
			{	double dx=((PointObject)P).getX(),dy=((PointObject)P).getY();
				P.snap(zc);
				dx=((PointObject)P).getX()-dx; 
				dy=((PointObject)P).getY()-dy;
				P.round();
				P.updateText();
				Enumeration en=V.elements();
				while (en.hasMoreElements())
				{	PointObject p=(PointObject)en.nextElement();
					p.move(p.getX()+dx,p.getY()+dy);
					p.updateText();
				}
			}
			else
			{	P.snap(zc);
				P.round();
				P.updateText();
			}
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
		showStatus(zc);
	}
	
	public void mouseReleased (MouseEvent e, ZirkelCanvas zc)
	{	mouseReleased(e,zc,false);
	}
	
	public void reset (ZirkelCanvas zc)
	{	V=null;
		P=null;
		zc.clearSelected();
		zc.repaint(); 
		Selected=false;
	}
	
	public void resetFirstTime (ZirkelCanvas zc)
	{	if (V!=null) V.removeAllElements();
		zc.clearSelected();
		zc.selectAllMoveableVisibleObjects();
		Graphics g=zc.getGraphics();
		if (g!=null)
		{	zc.paint(g);
			g.dispose();
			try
			{	Thread.sleep(400);
			}
			catch (Exception e) {}
			zc.clearSelected();
		}
		zc.repaint(); 
		Selected=false;
		P=null;
		V=null;
	}
	
	public void showStatus (ZirkelCanvas zc)
	{	if (P==null && !Selected) zc.showStatus(
			Zirkel.name("message.move.select","Move: Select a point"));
		else if (Selected) 
			zc.showStatus(
			Zirkel.name("message.move.move","Move: Move the point"));
		else zc.showStatus(
			Zirkel.name("message.move.move","Move: Move the point")+
			" ("+P.getName()+")");
	}
	
	public boolean useSmartBoard ()
	{	return false;
	}
	
}
