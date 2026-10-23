package rene.zirkel.tools;

// file: Hider.java

import java.awt.event.*;

import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelCanvas;
import rene.zirkel.constructors.*;
import rene.zirkel.graphics.Drawing;

public class DrawerTool extends ObjectConstructor
{	Drawing D;
	boolean Dragging=false;

	public void mousePressed (MouseEvent e, ZirkelCanvas zc)
	{	double x=zc.x(e.getX()),y=zc.y(e.getY());
		D=new Drawing();
		D.addXY(x,y);
		Dragging=true;
		zc.addDrawing(D);
		D.setColor(zc.getDefaultColor());
	}
	
	public void mouseDragged (MouseEvent e, ZirkelCanvas zc)
	{	if (!Dragging) return;
		double x=zc.x(e.getX()),y=zc.y(e.getY());
		D.addXY(x,y);
		zc.repaint();		
	}
	
	public void mouseReleased (MouseEvent e, ZirkelCanvas zc)
	{	Dragging=false;
		zc.repaint();
	}

	public void showStatus (ZirkelCanvas zc)
	{	zc.showStatus(Zirkel.name("message.draw"));
	}
	
	public void reset (ZirkelCanvas zc)
	{	zc.clearSelected();
	}

	public boolean useSmartBoard ()
	{	return false;
	}
}
