package rene.zirkel.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.util.Enumeration;
import java.util.Vector;

import rene.gui.ButtonAction;
import rene.gui.CloseDialog;
import rene.gui.Global;
import rene.gui.IconBar;
import rene.gui.IconBarListener;
import rene.gui.Panel3D;
import rene.zirkel.ZirkelCanvas;
import rene.zirkel.construction.Construction;
import rene.zirkel.objects.*;

public class Replay extends CloseDialog
	implements IconBarListener
{	ZirkelCanvas ZC;
	int Last;
	Construction C;
	Vector V; // Copied vector of visible objects
	IconBar IB;
	boolean HaveBreaks;

	public Replay (Frame f, ZirkelCanvas zc)
	{	super(f,Global.name("replay.title"),true);
		ZC=zc;
		add(new ButtonAction(this,"Vor"));
		
		setLayout(new BorderLayout());
		
		IB=new IconBar(f);
		IB.addLeft("allback");
		IB.addLeft("fastback");
		IB.addLeft("oneback");
		IB.addLeft("oneforward");
		IB.addLeft("fastforward");
		IB.addLeft("allforward");
		IB.addSeparatorLeft();
		IB.addLeft("nextbreak");
		IB.addToggleLeft("setbreak");
		IB.addSeparatorLeft();
		IB.addLeft("close");
		IB.setIconBarListener(this);
		
		add("Center",new Panel3D(IB));
		
		C=zc.getConstruction();
		
		// Collect all visible Elements:
		V=new Vector();
		Enumeration e=C.elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (!o.mustHide(ZC) || o.isBreakHide() || 
					(o instanceof TextObject && o.valid() && !o.isSuperHidden()))
			{	V.addElement(o);
			}
		}

		HaveBreaks=haveBreaks();

		Last=0;
		setLast();
		setEnabled(true);
		
		pack();
		Dimension d=f.getSize();
		Dimension ds=getSize();
		Point l=f.getLocation();
		setLocation(l.x+d.width-ds.width-5,l.y+d.height/5);
	}
	
	public void iconPressed (String o)
	{	if (o.equals("close"))
		{	doclose();
		}
		else if (o.equals("allback"))
		{	Last=0; setLast();
		}
		else if (o.equals("allforward"))
		{	Last=V.size(); setLast();
		}
		else if (o.equals("fastback"))
		{	Last-=10; if (Last<0) Last=0;
			setLast();
		}
		else if (o.equals("fastforward"))
		{	Last+=10; if (Last>V.size()) Last=V.size();
			setLast();
		}
		else if (o.equals("oneforward"))
		{	Last++; if (Last>V.size()) Last=V.size();
			setLast();
		}
		else if (o.equals("nextbreak"))
		{	while (true)
			{	Last++;
				if (Last>V.size()) { Last=V.size(); break; }
				if (Last>=V.size() || 
					(Last>0 && ((ConstructionObject)V.elementAt(Last)).isBreak())) 
						break;
			}
			setLast();
		}
		else if (o.equals("setbreak"))
		{	if (Last>0)
			{	ConstructionObject ob=(ConstructionObject)V.elementAt(Last);
				ob.setBreak(!ob.isBreak());
				IB.setState("setbreak",ob.isBreak());
				if (ob.isBreak() && IB.isShiftPressed())
					ob.setHideBreak(true);
			}
			HaveBreaks=haveBreaks();
			IB.setEnabled("nextbreak",HaveBreaks);
		}
		else if (o.equals("oneback"))
		{	Last--; if (Last<0) Last=0;
			setLast();
		}
		start();
	}

	public void start ()
	{	if (Last==0)
		{	IB.setEnabled("allback",false);
			IB.setEnabled("fastback",false);
			IB.setEnabled("oneback",false);
		}
		else
		{	IB.setEnabled("allback",true);
			IB.setEnabled("fastback",true);
			IB.setEnabled("oneback",true);
		}
		if (Last<V.size())
		{	IB.setEnabled("allforward",true);
			IB.setEnabled("fastforward",true);
			IB.setEnabled("oneforward",true);
		}
		else
		{	IB.setEnabled("allforward",false);
			IB.setEnabled("fastforward",false);
			IB.setEnabled("oneforward",false);
		}
	}	
	
	public void doclose ()
	{	ZC.paintUntil(null);
		ZC.showStatus();
		Enumeration e=V.elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o instanceof TextObject) ((TextObject)o).setDoShow(false);
		}
		ZC.repaint();
		super.doclose();
	}
	
	ConstructionObject O=null;
	
	public void checkLast ()
	{	if (O!=null) ((TextObject)O).setDoShow(false); 
		O=null;
		if (Last>V.size()-1) return;
		O=(ConstructionObject)V.elementAt(Last);
		if (O!=null && (O instanceof TextObject))
			((TextObject)O).setDoShow(true);
		else O=null;
	}
	
	public void setLast ()
	{	if (Last<V.size())
		{	ConstructionObject o=(ConstructionObject)V.elementAt(Last);
			checkLast();
			ZC.paintUntil(o);
		}
		else
		{	Last=V.size();
			checkLast();
			ZC.paintUntil(null);
		}
		if (Last>=0 && Last<V.size())
		{	ConstructionObject o=(ConstructionObject)V.elementAt(Last);
			ZC.showStatus(o.getName()+" : "+o.getText());
			IB.setState("setbreak",o.isBreak());
		}
		else
		{	IB.setState("setbreak",false);
		}
		if (Last>=V.size() || Last==0) IB.setEnabled("setbreak",false);
		else IB.setEnabled("setbreak",true);
		if (Last>=V.size() || !HaveBreaks) IB.setEnabled("nextbreak",false);
		else IB.setEnabled("nextbreak",true);
	}
	
	public boolean haveBreaks ()
	{	Enumeration e=V.elements();
		while (e.hasMoreElements())
		{	if (((ConstructionObject)e.nextElement()).isBreak())
				return true;
		}
		return false;
	}
}
