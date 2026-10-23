package rene.zirkel.tools;

// file: ObjectTracker.java

import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.Vector;

import rene.util.xml.XmlTag;
import rene.util.xml.XmlTree;
import rene.util.xml.XmlWriter;
import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelCanvas;
import rene.zirkel.construction.Construction;
import rene.zirkel.construction.ConstructionException;
import rene.zirkel.construction.Selector;
import rene.zirkel.constructors.*;
import rene.zirkel.graphics.*;
import rene.zirkel.objects.*;
import rene.zirkel.structures.Coordinates;

public class ObjectTracker extends ObjectConstructor
	implements TrackPainter, Runnable, Selector
{	PointObject PM;
	ConstructionObject O,P;
	int PMax=16,PN;
	ConstructionObject PO[]=new ConstructionObject[PMax];
	Vector V=new Vector(),VPM=new Vector();
	Vector VO[]=new Vector[PMax];
	boolean Animate,Paint;
	public boolean Interactive=true;
	boolean Other=false;
	ZirkelCanvas ZC;
	
	public ObjectTracker ()
	{
	}

	public ObjectTracker (ConstructionObject p, PointObject pm, ConstructionObject o,
		ZirkelCanvas zc, boolean animate, boolean paint,
		ConstructionObject po[])
	{	P=p; PM=pm; O=o;
		if (P!=null && O!=null && (PM!=null || (O instanceof ExpressionObject)))
		{	Animate=animate;
			Paint=paint;
			if (PM!=null) PM.project(O);
			zc.validate();
			zc.repaint();
			showStatus(zc);
			PN=0;
			for (int i=0; i<po.length; i++)
			{	if (i>=PMax || po[i]==null) break;
				PO[PN]=po[i];
				VO[i]=new Vector();
				PN++;
			}
		}
	}

	public boolean isAdmissible (ZirkelCanvas zc, ConstructionObject o)
	{	if (O==null)
		{	if (o instanceof ExpressionObject && ((ExpressionObject)o).isSlider()) return true;
			if (o instanceof PrimitiveLineObject) return true;
			if (o instanceof PrimitiveCircleObject) return true;
			if (o instanceof PointObject && ((PointObject)o).isPointOn()) return true;
			return false;
		}
		else
		{	if (!(o instanceof PointObject)) return false;
			if (!((MoveableObject)o).moveable()) return false;
			if ((O instanceof CircleObject) && 
				((CircleObject)O).getP2()==o) return true;
			if (zc.depends(O,o)) return false;
			ConstructionObject bound=((PointObject)o).getBound();
			if (bound!=null && bound!=O) return false;
			return true;
		}
	}
	
	public void mousePressed (MouseEvent e, ZirkelCanvas zc)
	{	double x=zc.x(e.getX()),y=zc.y(e.getY());
		if (P==null) // no point selected yet
		{	P=zc.selectPoint(e.getX(),e.getY());
			if (P==null) P=zc.selectLine(e.getX(),e.getY());
			if (P!=null)
			{	P.setSelected(true);
				zc.repaint();
			}
			if (e.isShiftDown())
			{	Other=true; PN=0;
			}
			else Other=false;
			showStatus(zc);
		}
		else if (Other) // want more points to track
		{	ConstructionObject P=zc.selectPoint(e.getX(),e.getY());
			if (P==null) P=zc.selectLine(e.getX(),e.getY());
			if (P!=null)
			{	P.setSelected(true);
				zc.repaint();
				PO[PN]=P;
				VO[PN]=new Vector();
				PN++;
			}
			if (!e.isShiftDown() || PN>=PMax) Other=false;
			showStatus(zc);
		}		
		else if (O==null) // no object to track on yet
		{	O=zc.selectWithSelector(e.getX(),e.getY(),this);
			if (O instanceof ExpressionObject)
			{	zc.clearSelected();
				zc.clearIndicated();
				Animate=!e.isShiftDown();
				Paint=true;
				compute(zc);
				ZC=zc;
				if (Animate) zc.validate();
				zc.repaint();
			}
			else if (O instanceof PointObject && ((PointObject)O).isPointOn())
			{	PM=(PointObject)O;
				O=PM.getBound();
				zc.clearSelected();
				zc.clearIndicated();
				Animate=!e.isShiftDown();
				Paint=true;
				compute(zc);
				ZC=zc;
				if (Animate) zc.validate();
				zc.repaint();
			}
			else
			{	if (O!=null)
				{	O.setSelected(true);
					zc.repaint();
				}
				showStatus(zc);
			}
		}
		else if (PM==null && !(O instanceof ExpressionObject))
		{	ConstructionObject pm=
				zc.selectWithSelector(e.getX(),e.getY(),this);
			if (pm==null) return;
			PM=(PointObject)pm;
			zc.clearSelected();
			zc.clearIndicated();
			Animate=!e.isShiftDown();
			Paint=true;
			compute(zc);
			ZC=zc;
			if (Animate) zc.validate();
			zc.repaint();
		}
		else if (!e.isControlDown() && !e.isShiftDown() && !e.isAltDown())
		{	if (!Running && Interactive && PM!=null && PM.nearto(e.getX(),e.getY(),zc))
			{	Dragging=true;
				zc.getConstruction().shouldSwitch(true);
			}
			else if (Animate)
			{	if (Paint)
				{	Paint=false;
				}
				else
				{	Animate=false;
					Paint=true;
					if (Running) stop();
					showStatus(zc);
				}
			}
			else
			{	if (Running) return;
				Paint=true;
				Animate=true;
				compute(zc);
				zc.validate();
				zc.repaint();
				showStatus(zc);
			}
		}
	}
	
	public void mouseMoved (MouseEvent e, ZirkelCanvas zc, boolean simple)
	{	if (PM!=null) return;
		if (P==null || Other)
			zc.indicatePointsOrLines(e.getX(),e.getY());
		else if (O==null)
			zc.indicateWithSelector(e.getX(),e.getY(),this);
		else
			zc.indicateWithSelector(e.getX(),e.getY(),this);
	}

	public void mouseDragged (MouseEvent e, ZirkelCanvas zc)
	{	if (!Dragging || PM==null) return;
		double oldx=PM.getX(),oldy=PM.getY();
		PM.move(zc.x(e.getX()),zc.y(e.getY()));
		zc.dovalidate();
		if (P.valid()) zc.repaint();
	}

	public void mouseReleased (MouseEvent e, ZirkelCanvas zc)
	{	if (!Dragging || PM==null) return;
		zc.getConstruction().shouldSwitch(false);
		Dragging=false;
		zc.validate();
	}

	public void reset (ZirkelCanvas zc)
	{	zc.clearSelected();
		if (Running) stop();
		PM=null; P=O=null; V=new Vector(); PN=0;
		for (int i=0; i<PMax; i++) VO[i]=null;
		zc.repaint();
		showStatus(zc);
		Omit=1;
	}

	public void showStatus (ZirkelCanvas zc)
	{	if (P==null || Other) zc.showStatus(
			Zirkel.name("message.objecttracker.select"));
		else if (O==null) zc.showStatus(
			Zirkel.name("message.objecttracker.object"));
		else if (PM==null && !(O instanceof ExpressionObject)) zc.showStatus(
			Zirkel.name("message.objecttracker.selectpoint"));
		else if (Running) zc.showStatus(
			Zirkel.name("message.objecttracker.stop"));
		else zc.showStatus(
			Zirkel.name("message.objecttracker.start"));
	}

	public Enumeration elements ()
	{	return V.elements();
	}
	
	public void paint (MyGraphics g, ZirkelCanvas zc)
	{	if (!isComplete()) return;
		if (P==null || !Paint) return;
		Coordinates C;
		Enumeration e=V.elements();
		double c0,r0,c,r;
		g.setColor(P);
		PolygonDrawer pd=new PolygonDrawer(g,P);
		if (e.hasMoreElements())
		{	C=(Coordinates)e.nextElement();
			c0=zc.col(C.X); r0=zc.row(C.Y);
			pd.startPolygon(c0,r0);
			while (e.hasMoreElements())
			{	C=(Coordinates)e.nextElement();
				c=zc.col(C.X); r=zc.row(C.Y);
				if (Math.abs(c0-c)<100 && Math.abs(r0-r)<100) 
					pd.drawTo(c,r);
				else
					pd.finishPolygon();
				c0=c; r0=r;
			}
			pd.finishPolygon();
		}
		for (int i=0; i<PN; i++)
		{	e=VO[i].elements();
			g.setColor(PO[i]);
			pd=new PolygonDrawer(g,PO[i]);
			if (e.hasMoreElements())
			{	C=(Coordinates)e.nextElement();
				c0=zc.col(C.X); r0=zc.row(C.Y);
				pd.startPolygon(c0,r0);
				while (e.hasMoreElements())
				{	C=(Coordinates)e.nextElement();
					c=zc.col(C.X); r=zc.row(C.Y);
					if (Math.abs(c0-c)<100 && Math.abs(r0-r)<100) 
						pd.drawTo(c,r);
					else
						pd.finishPolygon();
					c0=c; r0=r;
				}
			}
			pd.finishPolygon();
		}
	}

	boolean Running=false,Stopped=false;
	
	public void validate (ZirkelCanvas zc)
	{	if (!isComplete()) return;
		if (O instanceof ExpressionObject && !((ExpressionObject)O).isSlider()) return;
		if (Running) return;
		if (PM!=null) 
		{	PM.project(O);
			oldx=PM.getX();
			oldy=PM.getY();
		}
		else if (O instanceof ExpressionObject)
		{	oldx=((ExpressionObject)O).getSliderPosition();
		}
		compute(zc);
		if (Animate)
		{	ZC=zc;
			new Thread(this).start();
		}
		else
		{	if (PM!=null)
			{	PM.move(oldx,oldy);
			}
			zc.dovalidate();
			zc.repaint();
		}
	}
	
	double da=0;
	double oldx,oldy;
	double X,Y,DX,DY;
	double XO[]=new double[PMax],YO[]=new double[PMax],
		DXO[]=new double[PMax],DYO[]=new double[PMax];
	int Omit=1;

	public void animate (ZirkelCanvas zc)
	{	if (!isComplete()) return;
		zc.getConstruction().clearSwitches();
		zc.getConstruction().shouldSwitch(true);
		Enumeration e=VPM.elements();
		Enumeration ev=V.elements();
		long time=System.currentTimeMillis();
		int i=0;
		boolean start=false;
		Graphics ZCG=ZC.getGraphics();
		while (e.hasMoreElements())
		{	Coordinates c=(Coordinates)e.nextElement();
			Coordinates cv=(Coordinates)ev.nextElement();
			if (PM==null && !(O instanceof ExpressionObject)) break;
			synchronized(zc)
			{	if (PM!=null)
					PM.move(c.X,c.Y);
				else if (O instanceof ExpressionObject) 
					((ExpressionObject)O).setSliderPosition(c.X);
				if (!start)
				{	zc.resetSum();
					start=true;
				}
			}
			zc.dovalidate();
			i++;
			if (i<Omit) continue;
			i=0;
			if (zc.isInside(cv.X,cv.Y))
			{	if (P!=null && P.valid()) ZC.paint(ZCG);
				try
				{	long t=System.currentTimeMillis();
					int h=(int)(t-time);
					if (h<0) h=0;
					if (h>50) h=50;
					Thread.sleep(50-h);
					time=System.currentTimeMillis();
				}
				catch (Exception ex) {}
			}
			if (Stopped) break;
		}
		ZCG.dispose();
		zc.getConstruction().shouldSwitch(false);
		zc.getConstruction().clearSwitches();
	}

	public ConstructionObject getObject () { return O; }
	
	double mod (double x)
	{	if (x>=Math.PI) return x-2*Math.PI;
		if (x<-Math.PI) return x+2*Math.PI;
		return x;
	}
	
	public void run ()
	{	if (O instanceof ExpressionObject && !((ExpressionObject)O).isSlider()) return;
		if (V.size()==0) return;
		if (!Animate) return;
		Running=true; Stopped=false;
		showStatus(ZC);
		while (true)
		{	try
			{	Thread.sleep(100);
			}
			catch (Exception ex) {}
			if (ZC.I!=null) break;
		}
		while (true)
		{	if (PM!=null) PM.move(oldx,oldy);
			else if (O instanceof ExpressionObject)
			{	((ExpressionObject)O).setSliderPosition(oldx);
			}
			animate(ZC);
			if (Stopped) break;
		}
		if (PM!=null && !(O instanceof ExpressionObject))
		{	if (PM!=null) PM.move(oldx,oldy);
			else if (O instanceof ExpressionObject)
			{	((ExpressionObject)O).setSliderPosition(oldx);
			}
			ZC.getConstruction().switchBack();
			ZC.dovalidate();
			ZC.repaint();
		}
		synchronized (this)
		{	notify();
		}
		Running=false;
		showStatus(ZC);
	}
	
	public void invalidate (ZirkelCanvas zc)
	{	stop();
	}
	
	public void stop ()
	{	if (!Running) return;
		Stopped=true;
		try { wait(); } catch (Exception e) {}
		ZC.repaint();
		showStatus(ZC);
	}
	
	public void save (XmlWriter xml)
	{	if (P==null || O==null || (PM==null && !(O instanceof ExpressionObject))) return;
		xml.startTagStart("Track");
		xml.printArg("track",P.getName());
		for (int i=0; i<PN; i++)
		{	xml.printArg("track"+i,PO[i].getName());
		}
		xml.printArg("on",O.getName());
		if (PM!=null) xml.printArg("move",PM.getName());
		if (Animate && Paint) xml.printArg("animate","true");
		else if (Animate && !Paint) xml.printArg("animate","nopaint");
		if (Omit>1) xml.printArg("omit",""+(Omit-1));
		xml.finishTagNewLine();
	}
	
	public void pause (boolean flag)
	{	if (flag)
		{	if (Running) stop();
		}
	}

	/** This is the update function, calling docompute.
	 * @param zc
	 */
	public synchronized void compute (ZirkelCanvas zc)
	{	if (!isComplete()) return;
		double x=0,y=0;
		if (PM!=null)
		{	x=PM.getX(); y=PM.getY();
		}
		else if (O instanceof ExpressionObject)
		{	x=((ExpressionObject)O).getSliderPosition();
		}
		zc.getConstruction().clearSwitches();
		zc.getConstruction().shouldSwitch(true);
		docompute(zc);
		zc.getConstruction().shouldSwitch(false);
		zc.getConstruction().clearSwitches();
		if (PM!=null) PM.move(x,y);
		else if (O instanceof ExpressionObject)
		{	((ExpressionObject)O).setSliderPosition(x);
		}
		zc.dovalidate();
	}
	
	/**
	 * Complicated procedure to recompute the automatic track.
	 * In principle, a moving point moves on some object, the
	 * coordinates of the tracked points or the intersections
	 * of the tracked lines are stored, as well as the positions
	 * of the moved point.
	 * But if the tracked point gets invalid, the movement reverses
	 * and the interesections change.
	 * Moreover, there is a list of secondary tracked objects.
	 * @param zc
	 */
	public synchronized void docompute (ZirkelCanvas zc)
	{	V=new Vector();
		for (int i=0; i<PN; i++) VO[i]=new Vector();
		VPM=new Vector();
		// Running on a circle:
		if (O instanceof PrimitiveCircleObject)
		{	zc.getConstruction().shouldSwitch(false);
			PrimitiveCircleObject c=(PrimitiveCircleObject)O;
			double x=c.getX(),y=c.getY(),r=c.getR();
			PM.project(c);
			double amin=0,amax=0,astart=0,anull=0;
			double dmax=0.5;
			boolean range=false;
			if (c.hasRange())
			{	range=true;
				double a1=c.getA1();
				double a2=c.getA2();
				double d=a2-a1;
				while (d<0) d+=2*Math.PI;
				while (d>=2*Math.PI) d-=2*Math.PI;
				amin=astart=-d/2;
				amax=d/2;
				anull=(a1+a2)/2;
			}
			else
			{	amin=astart=-Math.PI*0.9999;
				amax=Math.PI*0.9999;
			}
			double a=astart;
			PM.move(x+r*Math.cos(anull+a),y+r*Math.sin(anull+a));
			PM.project(c);
			zc.getConstruction().validate(P,PM);
			zc.resetSum();
			double x1=0,y1=0;
			boolean started=false;
			if (P.valid())
			{	zc.getConstruction().shouldSwitch(true);
				if (P instanceof PointObject)
				{	PointObject p=(PointObject)P;
					x1=p.getX(); y1=p.getY();
					V.addElement(new Coordinates(x1,y1));
					VPM.addElement(new Coordinates(PM.getX(),PM.getY()));
					started=true;
				}
				else if (P instanceof PrimitiveLineObject)
				{	PrimitiveLineObject L=(PrimitiveLineObject)P;
					X=L.getX(); Y=L.getY(); DX=L.getDX(); DY=L.getDY();
					started=true;	
				}
			}
			boolean startedO[]=new boolean[PMax];
			for (int i=0; i<PN; i++) startedO[i]=false;
			long time=System.currentTimeMillis();
			addSecondary(startedO,zc);
			double dmin=0.001;
			if (da<1e-10 || da>zc.dx(1)) da=zc.dx(1)/10;
			double aold=a;
			double x2=0,y2=0;
			while (true)
			{	a=a+da;
				boolean Break=false;
				if ((!started || range) && a>=amax)
				{	a=amax; 
					Break=true;
				}
				else if ((!started || range) && a<=amin)
				{	a=amin;
					Break=true;
				}
				else if (started && da>0)
				{	if ((mod(aold-astart)<0 && mod(a-astart)>=0) &&
						!zc.getConstruction().haveSwitched())
					{	Break=true;
						a=astart;
					}
				}
				aold=a;
				PM.move(x+r*Math.cos(anull+a),y+r*Math.sin(anull+a));
				PM.project(c);
				zc.getConstruction().validate(P,PM);
				if (P.valid())
				{	if (!started)
					{	zc.getConstruction().shouldSwitch(true);
						astart=a;
					}
					boolean valid=false;
					if (P instanceof PointObject)
					{	PointObject p=(PointObject)P;
						x2=p.getX(); y2=p.getY();
						valid=true;
					}
					else if (P instanceof PrimitiveLineObject)
					{	PrimitiveLineObject L=(PrimitiveLineObject)P;
						if (!started)
						{	X=L.getX(); Y=L.getY(); DX=L.getDX(); DY=L.getDY();
						}
						else
						{	double xx,yy,dx,dy;
							xx=L.getX(); yy=L.getY(); dx=L.getDX(); dy=L.getDY();
							double det=dx*DY-dy*DX;
							if (Math.sqrt(Math.abs(det))>1e-9)
							{	double h=(-(X-xx)*DY+DX*(Y-yy))/(-det);
								x2=xx+h*dx; y2=yy+h*dy;
								valid=true;
							}
							X=xx; Y=yy; DX=dx; DY=dy;
						}					
					}
					double dist=zc.dCenter(x2,y2);
					boolean different=((int)zc.col(x1)!=(int)zc.col(x2) || 
						(int)zc.row(y1)!=(int)zc.row(y2));
					if (valid && different)
					{	V.addElement(new Coordinates(x2,y2));
						VPM.addElement(new Coordinates(PM.getX(),PM.getY()));
					}
					double dp=Math.abs(x2-x1)+Math.abs(y2-y1);
					da=updateDA(da,valid,dist,dp,dmin,dmax,zc);
					x1=x2; y1=y2;
					started=true;
				}
				else if (started)
				{	V.addElement(new Coordinates(x2,y2));
					VPM.addElement(new Coordinates(PM.getX(),PM.getY()));
					da=-da;
				}
				addSecondary(startedO,zc);
				if (Break || System.currentTimeMillis()-time>5000) break;
			}
		}
		// Running on a line:
		else if (O instanceof PrimitiveLineObject)
		{	zc.getConstruction().shouldSwitch(false);
			PrimitiveLineObject l=(PrimitiveLineObject)O;
			PM.project(l);
			double lx=l.getX(),ly=l.getY(),ldx=l.getDX(),ldy=l.getDY();
			double amin=0,amax=0,astart=0;
			double dmax=0.5;
			boolean range=false;
			if (l instanceof RayObject)
			{	amin=astart=0;
				amax=Math.PI*0.9999;
				range=true;
			}
			else if (l instanceof SegmentObject)
			{	amin=astart=0; 
				double r=((SegmentObject)l).getLength();
				dmax=r/20;
				amax=Math.atan(r)*2;
				range=true;
			}
			else
			{	amin=astart=-Math.PI*0.99999;
				amax=Math.PI*0.9999;
			}				
			double a=astart;
			double hd=Math.tan(mod(a)/2);
			PM.move(lx+hd*ldx,ly+hd*ldy);
			PM.project(l);
			zc.getConstruction().validate(P,PM);
			zc.resetSum();
			double x1=0,y1=0;
			boolean started=false;
			if (P.valid())
			{	zc.getConstruction().shouldSwitch(true);
				if (P instanceof PointObject)
				{	PointObject p=(PointObject)P;
					x1=p.getX(); y1=p.getY();
					V.addElement(new Coordinates(x1,y1));
					VPM.addElement(new Coordinates(PM.getX(),PM.getY()));
					started=true;
				}
				else if (P instanceof PrimitiveLineObject)
				{	PrimitiveLineObject L=(PrimitiveLineObject)P;
					X=L.getX(); Y=L.getY(); DX=L.getDX(); DY=L.getDY();
					started=true;	
				}
			}
			boolean startedO[]=new boolean[PMax];
			for (int i=0; i<PN; i++) startedO[i]=false;
			long time=System.currentTimeMillis();
			addSecondary(startedO,zc);
			double dmin=0.001;
			if (da<1e-10 || da>zc.dx(1)) da=zc.dx(1)/10;
			double aold=a;
			double x2=0,y2=0;
			while (true)
			{	a=a+da;
				boolean Break=false;
				if ((!started || range) && a>=amax)
				{	a=amax; 
					Break=true;
				}
				else if ((!started || range) && a<=amin)
				{	a=amin;
					Break=true;
				}
				else if (started && da>0)
				{	if ((mod(aold-astart)<0 && mod(a-astart)>=0) &&
						!zc.getConstruction().haveSwitched())
					{	Break=true;
						a=astart;
					}
				}
				aold=a;
				hd=Math.tan(mod(a)/2);
				PM.move(lx+hd*ldx,ly+hd*ldy);
				PM.project(l);
				zc.getConstruction().validate(P,PM);
				if (P.valid())
				{	if (!started)
					{	zc.getConstruction().shouldSwitch(true);
						astart=a;
					}
					boolean valid=false;
					if (P instanceof PointObject)
					{	PointObject p=(PointObject)P;
						x2=p.getX(); y2=p.getY();
						valid=true;
					}
					else if (P instanceof PrimitiveLineObject)
					{	PrimitiveLineObject L=(PrimitiveLineObject)P;
						if (!started)
						{	X=L.getX(); Y=L.getY(); DX=L.getDX(); DY=L.getDY();
						}
						else
						{	double xx,yy,dx,dy;
							xx=L.getX(); yy=L.getY(); dx=L.getDX(); dy=L.getDY();
							double det=dx*DY-dy*DX;
							if (Math.sqrt(Math.abs(det))>1e-9)
							{	double h=(-(X-xx)*DY+DX*(Y-yy))/(-det);
								x2=xx+h*dx; y2=yy+h*dy;
								valid=true;
							}
							X=xx; Y=yy; DX=dx; DY=dy;
						}					
					}
					double dist=zc.dCenter(x2,y2);
					boolean different=((int)zc.col(x1)!=(int)zc.col(x2) || 
						(int)zc.row(y1)!=(int)zc.row(y2));
					if (valid && different)
					{	V.addElement(new Coordinates(x2,y2));
						VPM.addElement(new Coordinates(PM.getX(),PM.getY()));
					}
					double dp=Math.abs(x2-x1)+Math.abs(y2-y1);
					da=updateDA(da,valid,dist,dp,dmin,dmax,zc);
					x1=x2; y1=y2;
					started=true;
				}
				else if (started)
				{	V.addElement(new Coordinates(x2,y2));
					VPM.addElement(new Coordinates(PM.getX(),PM.getY()));
					da=-da;
				}
				addSecondary(startedO,zc);
				if (Break || System.currentTimeMillis()-time>5000) break;
			}
		}
		// Running an expression slider:
		else if (O instanceof ExpressionObject)
		{	zc.getConstruction().shouldSwitch(false);
			ExpressionObject eo=(ExpressionObject)O;
			if (!eo.isSlider()) return;
			double amin=0,amax=0,astart=0;
			double dmax=0.5;
			boolean range=false;
			amin=astart=0; 
			double r=1;
			dmax=r/20;
			amax=Math.atan(r)*2;
			range=true;
			double a=astart;
			double hd=Math.tan(mod(a)/2);
			eo.setSliderPosition(0);
			zc.getConstruction().validate(P,null);
			zc.resetSum();
			double x1=0,y1=0;
			boolean started=false;
			if (P.valid())
			{	zc.getConstruction().shouldSwitch(true);
				if (P instanceof PointObject)
				{	PointObject p=(PointObject)P;
					x1=p.getX(); y1=p.getY();
					V.addElement(new Coordinates(x1,y1));
					VPM.addElement(new Coordinates(eo.getSliderPosition(),0));
					started=true;
				}
				else if (P instanceof PrimitiveLineObject)
				{	PrimitiveLineObject L=(PrimitiveLineObject)P;
					X=L.getX(); Y=L.getY(); DX=L.getDX(); DY=L.getDY();
					started=true;	
				}
			}
			boolean startedO[]=new boolean[PMax];
			for (int i=0; i<PN; i++) startedO[i]=false;
			long time=System.currentTimeMillis();
			addSecondary(startedO,zc);
			double dmin=0.001;
			if (da<1e-10 || da>zc.dx(1)) da=zc.dx(1)/10;
			double aold=a;
			double x2=0,y2=0;
			while (true)
			{	a=a+da;
				boolean Break=false;
				if ((!started || range) && a>=amax)
				{	a=amax; 
					Break=true;
				}
				else if ((!started || range) && a<=amin)
				{	a=amin;
					Break=true;
				}
				else if (started && da>0)
				{	if ((mod(aold-astart)<0 && mod(a-astart)>=0) &&
						!zc.getConstruction().haveSwitched())
					{	Break=true;
						a=astart;
					}
				}
				aold=a;
				hd=Math.tan(mod(a)/2);
				eo.setSliderPosition(hd);
				zc.getConstruction().validate(P,null);
				if (P.valid())
				{	if (!started)
					{	zc.getConstruction().shouldSwitch(true);
						astart=a;
					}
					boolean valid=false;
					if (P instanceof PointObject)
					{	PointObject p=(PointObject)P;
						x2=p.getX(); y2=p.getY();
						valid=true;
					}
					else if (P instanceof PrimitiveLineObject)
					{	PrimitiveLineObject L=(PrimitiveLineObject)P;
						if (!started)
						{	X=L.getX(); Y=L.getY(); DX=L.getDX(); DY=L.getDY();
						}
						else
						{	double xx,yy,dx,dy;
							xx=L.getX(); yy=L.getY(); dx=L.getDX(); dy=L.getDY();
							double det=dx*DY-dy*DX;
							if (Math.sqrt(Math.abs(det))>1e-9)
							{	double h=(-(X-xx)*DY+DX*(Y-yy))/(-det);
								x2=xx+h*dx; y2=yy+h*dy;
								valid=true;
							}
							X=xx; Y=yy; DX=dx; DY=dy;
						}					
					}
					double dist=zc.dCenter(x2,y2);
					boolean different=((int)zc.col(x1)!=(int)zc.col(x2) || 
						(int)zc.row(y1)!=(int)zc.row(y2));
					if (valid && different)
					{	V.addElement(new Coordinates(x2,y2));
						VPM.addElement(new Coordinates(eo.getSliderPosition(),0));
					}
					double dp=Math.abs(x2-x1)+Math.abs(y2-y1);
					da=updateDA(da,valid,dist,dp,dmin,dmax,zc);
					x1=x2; y1=y2;
					started=true;
				}
				else if (started)
				{	V.addElement(new Coordinates(x2,y2));
					VPM.addElement(new Coordinates(eo.getSliderPosition(),0));
					da=-da;
				}
				addSecondary(startedO,zc);
				if (Break || System.currentTimeMillis()-time>5000) break;
			}
		}
	}
	
	public void addSecondary (boolean startedO[], ZirkelCanvas zc)
	{	for (int i=0; i<PN; i++)
		{	if (PM!=null) zc.getConstruction().validate(PO[i],PM);
			else zc.getConstruction().validate(PO[i],O);
			if (!PO[i].valid()) continue;
			if (PO[i] instanceof PointObject)
			{	PointObject p=(PointObject)PO[i];
				VO[i].addElement(new Coordinates(p.getX(),p.getY()));
			}
			else if (PO[i] instanceof PrimitiveLineObject)
			{	PrimitiveLineObject L=(PrimitiveLineObject)PO[i];
				if (!startedO[i])
				{	XO[i]=L.getX(); YO[i]=L.getY(); 
					DXO[i]=L.getDX(); DYO[i]=L.getDY();
					startedO[i]=true;
				}
				else
				{	double xx,yy,dx,dy;
					xx=L.getX(); yy=L.getY();
					dx=L.getDX(); dy=L.getDY();
					double det=dx*DYO[i]-dy*DXO[i];
					if (Math.sqrt(Math.abs(det))>1e-9)
					{	double h=(-(XO[i]-xx)*DYO[i]+
							DXO[i]*(YO[i]-yy))/(-det);
						XO[i]=xx; YO[i]=yy; DXO[i]=dx; DYO[i]=dy;
						VO[i].addElement(new Coordinates(xx+h*dx,yy+h*dy));							
					}
				}
			}
		}
	}
	
	public double StepSize=5;
	
	public double updateDA (double da, boolean valid, double dist, double dp, 
			double dmin, double dmax,
			ZirkelCanvas zc)
	{	if (V.size()>0 && valid)
		{	if (dist<1.2)
			{	if (dp>zc.dx(StepSize)) da/=2;
				else if (dp<zc.dx(StepSize/2)) da*=2;
				if (da>0 && da<dmin) da=dmin;
				else if (da<0 && da>-dmin) da=-dmin;
				if (da>dmax) da=dmax;
				else if (da<-dmax) da=-dmax;
			}
			else
			{	if (dp>zc.dx(StepSize*10)) da/=2;
				else if (dp<zc.dx(StepSize*5)) da*=2;
				if (da>0 && da<dmin) da=dmin;
				else if (da<0 && da>-dmin) da=-dmin;
				if (da>dmax) da=dmax;
				else if (da<-dmax) da=-dmax;
			}
		}
		return da;
	}
	
	public void increaseOmit ()
	{	Omit++;
	}
	
	public void decreaseOmit ()
	{	if (Omit>1) Omit--;
	}
	
	public void setOmit (int f)
	{	Omit=f+1;
	}
	
	public boolean isComplete ()
	{	return !(P==null || O==null || 
			(PM==null && !(O instanceof ExpressionObject && ((ExpressionObject)O).isSlider())));
	}
	
	public void keep (ZirkelCanvas zc)
	{	if (!isComplete()) return;
		TrackObject t=new TrackObject(zc.getConstruction(),
			P,PO,PN,O,PM);
		zc.addObject(t);
		t.setDefaults();
		reset(zc);
		t.compute(zc);
		zc.validate();
	}
	
	public String getTag () { return "Track"; }
	
	public boolean construct (XmlTree tree, Construction c)
		throws ConstructionException
	{	if (!testTree(tree,"Track")) return false;
		XmlTag tag=tree.getTag();
		if (!tag.hasParam("on") || !tag.hasParam("track"))
			throw new ConstructionException("Track parameters missing!");
		else try
		{	PointObject pm=null;
			try
			{	pm=(PointObject)c.find(tag.getValue("point"));
			}
			catch (Exception ex) {}
			ConstructionObject o=c.find(tag.getValue("on"));
			if (pm==null && !(o instanceof ExpressionObject)) throw new ConstructionException("");
			ConstructionObject p=c.find(tag.getValue("track"));
			ConstructionObject po[]=new ConstructionObject[PMax];
			int pn=0;
			for (pn=0; pn<PMax; pn++)
			{	if (!tag.hasParam("track"+pn)) break;
				po[pn]=c.find(tag.getValue("track"+pn));
				if (!(po[pn] instanceof PointObject || 
						po[pn] instanceof PrimitiveLineObject))
					throw new ConstructionException("Track parameters wrong!");
			}
			if (p==null || o==null)
				throw new ConstructionException("Track parameters wrong!");
			if (!(p instanceof PointObject || p instanceof PrimitiveLineObject))
				throw new ConstructionException("Track parameters wrong!");
			if (!(o instanceof PrimitiveCircleObject || 
					o instanceof PrimitiveLineObject || o instanceof ExpressionObject))
				throw new ConstructionException("Track parameters wrong!");
			TrackObject tr=new TrackObject(c,p,po,pn,o,pm);
			if (tag.hasParam("filled")) tr.setFilled(true);
			if (tag.hasParam("fixed")) tr.setFixed(true);
			if (tag.hasParam("dmin"))
			{	try
				{	tr.setDMin(new Double(tag.getValue("dmin")).doubleValue());
				}
				catch (Exception e) 
				{	throw new ConstructionException("Track parameters wrong!");
				}
			}
			if (tag.hasTrueParam("discrete")) tr.setDiscrete(true);
			setType(tag,tr);
			setName(tag,tr);
			set(tree,tr);
			c.add(tr);
			setConditionals(tree,c,tr);
			return true;
		}
		catch (ConstructionException e)
		{	// e.printStackTrace();
			throw e;
		}
		catch (Exception e)
		{	// e.printStackTrace();
			throw new ConstructionException("Track Parameters wrong!");
		}
	}
	
	public void setType (XmlTag tag, TrackObject p)
	{	if (tag.hasParam("shape"))
		{	String s=tag.getValue("shape");
			if (s.equals("square")) p.setType(0);
			if (s.equals("diamond")) p.setType(1);
			if (s.equals("circle")) p.setType(2);
			if (s.equals("dot")) p.setType(3);
			if (s.equals("cross")) p.setType(4);
			if (s.equals("dcross")) p.setType(5);
		}
	}
	
}
