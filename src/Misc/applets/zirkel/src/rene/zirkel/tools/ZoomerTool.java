package rene.zirkel.tools;

// file: Hider.java

import java.awt.Cursor;
import java.awt.event.MouseEvent;

import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelCanvas;
import rene.zirkel.construction.Construction;
import rene.zirkel.constructors.*;

/**
 * @author Rene
 * Werkzeug zum Zoomen des Fensters und zum Verschieben.
 * Verschieben funktioniert im Zentrum des Fensters. 
 */
public class ZoomerTool extends ObjectConstructor
{	boolean Dragging=false,Zoom=false;
	double X,Y,W,X0,Y0;
	ObjectConstructor OC;
	
	public ZoomerTool ()
	{	super();
	}
	
	public ZoomerTool (ObjectConstructor oc, MouseEvent e, ZirkelCanvas zc)
	{	super();
		OC=oc;
		X0=zc.x(e.getX()); Y0=zc.y(e.getY());
		Construction c=zc.getConstruction();
		X=c.getX(); Y=c.getY(); W=c.getW();
		Zoom=false;
		zc.setCursor(new Cursor(Cursor.MOVE_CURSOR));
		Dragging=true;
	}
	
	public void mousePressed (MouseEvent e, ZirkelCanvas zc)
	{	X0=zc.x(e.getX()); Y0=zc.y(e.getY());
		Construction c=zc.getConstruction();
		X=c.getX(); Y=c.getY(); W=c.getW();
		Zoom=(Math.abs(X-X0)>W/4 || Math.abs(Y-Y0)>W/4);
		if (!Zoom) zc.setCursor(new Cursor(Cursor.MOVE_CURSOR));
		Dragging=true;
		OC=null;
	}
	
	public void mouseDragged (MouseEvent e, ZirkelCanvas zc)
	{	if (!Dragging) return;
		Construction c=zc.getConstruction();
		c.setXYW(X,Y,W);
		zc.recompute();
		double x=zc.x(e.getX()),y=zc.y(e.getY());
		if (Zoom) c.setXYW(X,Y,
			Math.sqrt((X0-X)*(X0-X)+(Y0-Y)*(Y0-Y))/
			Math.sqrt((x-X)*(x-X)+(y-Y)*(y-Y))*W);
		else c.setXYW(X-(x-X0),Y-(y-Y0),W);
		zc.recompute();
		zc.validate();
		zc.repaint();
	}
	
	public void mouseReleased (MouseEvent e, ZirkelCanvas zc)
	{	Dragging=Zoom=false;
		zc.setCursor(Cursor.getDefaultCursor());
		zc.repaint();
		if (OC!=null) zc.setTool(OC);
	}

	public void showStatus (ZirkelCanvas zc)
	{	zc.showStatus(Zirkel.name("message.zoom"));
	}
	
	public void reset (ZirkelCanvas zc)
	{	zc.clearSelected();
		zc.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		Zoom=Dragging=false;
	}
	
	public void invalidate (ZirkelCanvas zc)
	{	zc.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}
	
	public void mouseMoved (MouseEvent e, ZirkelCanvas zc, boolean flag)
	{	X0=zc.x(e.getX()); Y0=zc.y(e.getY());
		Construction c=zc.getConstruction();
		X=c.getX(); Y=c.getY(); W=c.getW();
		Zoom=(Math.abs(X-X0)>W/4 || Math.abs(Y-Y0)>W/4);
		if (!Zoom) zc.setCursor(new Cursor(Cursor.MOVE_CURSOR));
		else zc.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}

	public boolean useSmartBoard ()
	{	return false;
	}
}
