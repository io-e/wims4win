package rene.zirkel.tools;

import java.awt.event.*;
import java.util.*;

import rene.util.xml.*;
import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelCanvas;
import rene.zirkel.construction.Selector;
import rene.zirkel.constructors.*;
import rene.zirkel.objects.*;

/**
 * Animator is a class to animate a point along a sequence
 * of segments and/or circles. The animation may either go
 * back and forth or always in the same direction. Shift-Click
 * schaltet während der Animation um.
 * 
 * @author Rene
 *
 */
public class AnimatorTool extends ObjectConstructor
	implements Runnable, Selector
{	PointObject P; 
	Vector V; // Vector of segments or circles
	ZirkelCanvas ZC; 
	boolean Running=false,Interactive=true,Complete=false; 
	boolean Negative=false; 
	boolean Original=false;
	double Delay=50;
	
	public AnimatorTool ()
	{	V=new Vector(); 
		P=null; 
	}
	
	public AnimatorTool (PointObject p, Vector v, ZirkelCanvas zc, boolean negative,
			boolean original, String delay)
	{	P=p; 
		if (!P.moveable()) return; 
		V=new Vector(); 
		Enumeration e=v.elements(); 
		while (e.hasMoreElements())
		{	ConstructionObject o=zc.getConstruction().find(
			(String)e.nextElement()); 
			if (!(o instanceof SegmentObject ||
				o instanceof PrimitiveCircleObject ||
				o instanceof PointObject
				))
				return; 
			V.addElement(o); 
		}
		Stopped=false; 
		ZC=zc; 
		Complete=true; 
		Negative=negative; 
		Original=original;
		try
		{	Delay=50;
			Delay=new Double(delay).doubleValue();
		}
		catch (Exception ex) {}
		
		new Thread(this).start(); 
	}
	
	public void setInteractive (boolean flag)
	{	Interactive=flag; 
	}
	
	public boolean isAdmissible (ZirkelCanvas zc, ConstructionObject o)
	{	if ((o instanceof CircleObject) && 
			((CircleObject)o).getP2()==P) return true;
		if (zc.depends(o,P)) return false;
		return true;
	}
	
	public void mousePressed (MouseEvent e, ZirkelCanvas zc)
	{	double x=zc.x(e.getX()),y=zc.y(e.getY()); 
		if (P==null)
		{	P=zc.selectPoint(e.getX(),e.getY()); 
			Complete=false; 
			if (P!=null && P.moveable())
			{	P.setSelected(true); 
				zc.repaint(); 
				showStatus(zc); 
			}
		}
		else
		{	if (!Complete && Interactive)
			{	ConstructionObject o=zc.selectWithSelector(e.getX(),e.getY(),this); 
				if (o==null) return; 
				boolean Have=(o==P); 
				if (!Have)
				{	Enumeration en=V.elements(); 
					while (en.hasMoreElements())
					{	if (o==en.nextElement())
						{	Have=true; break; 
						}
					}
				}
				if (!Have)
				{	if (!((o instanceof SegmentObject) ||
					(o instanceof PrimitiveCircleObject) ||
						(o instanceof PointObject)
						))
						return; 
					o.setSelected(true); 
					V.addElement(o); 
					zc.repaint(); 
					showStatus(zc); 
					return; 
				}
			}
			if (!Running)
			{	Stopped=false; 
				ZC=zc;
				zc.clearSelected(); 
				Complete=true; 
				zc.clearIndicated(); 
				showStatus(zc);
				new Thread(this).start(); 
			}
			else if (e.isShiftDown())
			{	Negative=!Negative; 
			}
			else
			{	Stopped=true; 
			}
		}
	}
	
	public void mouseMoved (MouseEvent e, ZirkelCanvas zc, boolean simple)
	{	if (Complete || !Interactive) return; 
		if (P==null)
			zc.indicatePointObjects(e.getX(),e.getY()); 
		else
			zc.indicateObjects(e.getX(),e.getY()); 
	}
	
	public synchronized void reset (ZirkelCanvas zc)
	{	Stopped=true; Complete=false;
		super.reset(zc); 
		P=null; 
		Delay=50;
		V=new Vector(); showStatus(zc); zc.repaint(); 
	}
	
	public void showStatus (ZirkelCanvas zc)
	{	if (P==null) zc.showStatus(
			Zirkel.name("message.animator.point")); 
		else if (!Complete) zc.showStatus(
			Zirkel.name("message.animator.segment")); 
		else zc.showStatus(
			Zirkel.name("message.animator.running")); 
	}
	
	public void save (XmlWriter xml)
	{	if (P==null) return; 
		xml.startTagStart("Animate"); 
		xml.printArg("animate",P.getName()); 
		int k=0; 
		Enumeration e=V.elements(); 
		while (e.hasMoreElements())
		{	String s=((ConstructionObject)e.nextElement()).getName(); 
			xml.printArg("via"+k,s); 
			k++; 
		}
		if (Negative) xml.printArg("negative","true"); 
		if (Delay!=50) xml.printArg("delay",""+Delay);
		xml.finishTagNewLine(); 
	}
	
	boolean Stopped=false; 
	
	public void run ()
	{	ZC.resetSum();
		if (Original) ZC.getConstruction().setOriginalOrder(true);
		Running=true; 
		showStatus(ZC); 
		Enumeration e=V.elements(); 
		double x=0.001; 
		boolean full=true; 
		ConstructionObject o=null; 
		long time=System.currentTimeMillis(); 
		long stoptime=time; 
		double end=2*Math.PI;
		boolean back=false;
		while (true)
		{	if (Stopped) break; 
			try
			{	long t=System.currentTimeMillis(); 
				int h=(int)(t-time); 
				if (h<0) h=0; 
				if (h>Delay) h=(int)Delay; 
				Thread.sleep((int)(Delay-h)); 
				time=System.currentTimeMillis(); 
			}
			catch (Exception ex) {	}
			if (Stopped) break; 
			if (ZC.I==null) continue; 
			synchronized (this)
			{	if (full)
				{	if (!e.hasMoreElements())
					{	e=V.elements(); 
					}
					o=(ConstructionObject)e.nextElement(); 
					full=false; 
					x=0.0001; 
					if (o instanceof SegmentObject)
						x=0.001*((SegmentObject)o).getLength(); 
					else if (o instanceof PrimitiveCircleObject)
					{	PrimitiveCircleObject c=(PrimitiveCircleObject)o;
						if (c.hasRange())
						{	c.computeA1A2();
							x=c.getA1();
							end=c.getA2();
							if (end<x) end=end+2*Math.PI;
							back=false;
						}
						else 
							x=0.0001*c.getR(); 
					}
					else if (o instanceof PointObject)
					{	PointObject p=(PointObject)o; 
						synchronized (ZC)
						{	P.move(p.getX(),p.getY()); 
							ZC.dovalidate(); 
						}
						ZC.repaint(); 
						stoptime=System.currentTimeMillis(); 
					}
				}
				if (o instanceof SegmentObject)
				{	SegmentObject s=(SegmentObject)o;
					synchronized (ZC) 
					{	if (back)
							P.move(s.getP1().getX()+(s.getLength()-x)*s.getDX(),
								s.getP1().getY()+(s.getLength()-x)*s.getDY()); 
						else
							P.move(s.getP1().getX()+x*s.getDX(),
								s.getP1().getY()+x*s.getDY()); 
						ZC.dovalidate(); 
					}
					ZC.repaint(); 
					x+=ZC.dx(2); 
					if (x>0.99*s.getLength()) 
					{	if (Negative && !back)
						{	x=0.00001; back=true;
						}
						else
						{	back=false; full=true; 
						}
					}
				}
				else if (o instanceof PrimitiveCircleObject)
				{	PrimitiveCircleObject c=(PrimitiveCircleObject)o; 
					if (c.getR()<1e-10)
					{	full=true; 
					}
					else
					{	synchronized (ZC)
						{	if (Negative && !c.hasRange())
								P.move(c.getP1().getX()+Math.cos(x)*c.getR(),
									c.getP1().getY()-Math.sin(x)*c.getR()); 
							else
								P.move(c.getP1().getX()+Math.cos(x)*c.getR(),
									c.getP1().getY()+Math.sin(x)*c.getR()); 
							ZC.dovalidate(); 
						}
						ZC.repaint(); 
						if (back) x-=ZC.dx(2)/c.getR(); 
						else x+=ZC.dx(2)/c.getR(); 
						if (x>end && !back)
						{	if (!Negative || back || !c.hasRange())
							{	full=true; back=false;
							}
							else
							{	back=true; end=c.getA1();
							}
						} 
						if (back && x<end)
						{	full=true; back=false;
						}
					}
				}
				else if (o instanceof PointObject)
				{	if (System.currentTimeMillis()-stoptime>5000)
					full=true; 
				}
			}
		}
		Running=false; 
		if (Original) ZC.getConstruction().setOriginalOrder(false);
	}
	
	public synchronized void invalidate (ZirkelCanvas zc)
	{	Stopped=true; 
	}
	
	public boolean useSmartBoard ()
	{	return false; 
	}

	public void increaseSpeed ()
	{	if (Delay>1) Delay=Delay/1.5;
	}
	
	public void decreaseSpeed ()
	{	if (Delay<500) Delay=Delay*1.5;
	}

}
