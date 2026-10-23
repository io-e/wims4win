package rene.zirkel.tools;

// file: MoveConstructor.java

import java.awt.event.*;

import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelCanvas;
import rene.zirkel.constructors.*;
import rene.zirkel.objects.*;

public class LabelMover extends ObjectConstructor
{	ConstructionObject P;
	ObjectConstructor OC;
	int X,Y;
	double OX,OY;
	public LabelMover (ObjectConstructor oc, ZirkelCanvas zc, 
		int x, int y, ConstructionObject p, boolean shift)
	{	OC=oc; P=p; 
		if (shift || !P.canKeepClose())
		{	X=x; Y=y; 
			P.setKeepClose(false);
			OX=P.xcOffset(); OY=P.ycOffset();
		}
		else
		{	P.setKeepClose(zc.x(x),zc.y(y));
		}
		if (P!=null)
		{	P.setLabelSelected(true);
			zc.repaint();
			showStatus(zc);
		}
	}
	public void mouseMoved (MouseEvent e, ZirkelCanvas zc, boolean simple)
	{
	}

	public void mouseDragged (MouseEvent e, ZirkelCanvas zc)
	{	if (P==null) return;
		if (P.isKeepClose())
		{	P.setKeepClose(zc.x(e.getX()),zc.y(e.getY()));
		}
		else
		{	P.setcOffset(OX+zc.dx(e.getX()-X),OY+zc.dy(Y-e.getY()));
		}
		zc.validate(); zc.repaint();
	}
	public void mouseReleased (MouseEvent e, ZirkelCanvas zc)
	{	if (P==null) return;
		P.setLabelSelected(false);
		zc.repaint();
		P=null;
		showStatus(zc);
		zc.setTool(OC);
	}
	
	public void reset (ZirkelCanvas zc)
	{	zc.clearSelected(); P.setLabelSelected(false); zc.repaint();
	}
	
	public void resetPoint ()
	{	if (P!=null)
		{	P.setKeepClose(false);
			P.setcOffset(0,0);
		}
	}
	
	public void showStatus (ZirkelCanvas zc)
	{	if (P==null) zc.showStatus(
			Zirkel.name("message.label.select","Move Label: Select a label"));
		else zc.showStatus(
			Zirkel.name("message.label.move","Move Label: Move the label"));
	}
	
	public boolean useSmartBoard ()
	{	return false;
	}
}
