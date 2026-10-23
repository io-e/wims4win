package rene.zirkel.tools;

// file: Tracker.java

import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.Vector;

import rene.util.xml.XmlWriter;
import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelCanvas;
import rene.zirkel.constructors.*;
import rene.zirkel.graphics.*;
import rene.zirkel.objects.*;
import rene.zirkel.structures.Coordinates;

public class Tracker extends ObjectConstructor
	implements TrackPainter
{	ConstructionObject PM;
	int PMax=8,PN;
	ConstructionObject P;
	ConstructionObject PO[]=new ConstructionObject[PMax];
	Vector V=new Vector();
	Vector VO[]=new Vector[PMax];
	double X,Y,DX,DY;
	double XO[]=new double[PMax],YO[]=new double[PMax],
		DXO[]=new double[PMax],DYO[]=new double[PMax];
	boolean Started;
	boolean StartedO[]=new boolean[PMax];
	boolean Other;

	public Tracker (ConstructionObject p, ConstructionObject po[])
	{	super();
		P=p;
		PN=0;
		for (int i=0; i<po.length; i++)
		{	if (i>=PMax || po[i]==null) break;
			PO[PN]=po[i];
			VO[i]=new Vector();
			PN++;
		}
	}
	
	public Tracker ()
	{	super();
	}

	public void mousePressed (MouseEvent e, ZirkelCanvas zc)
	{	double x=zc.x(e.getX()),y=zc.y(e.getY());
		if (P==null)
		{	P=zc.selectPoint(e.getX(),e.getY());
			if (P==null) P=zc.selectLine(e.getX(),e.getY());
			if (P==null) return;
			P.setSelected(true);
			zc.repaint();
			if (e.isShiftDown()) Other=true;
			else Other=false;
			showStatus(zc);
		}
		else if (Other)
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
		else
		{	ConstructionObject pm=zc.selectMoveableObject(e.getX(),e.getY());
			PM=pm;
			if (PM!=null)
			{	zc.clearSelected();
				PM.setSelected(true);
				((MoveableObject)PM).startDrag(x,y);
				zc.repaint();
				showStatus(zc);
			}
			Started=false;
		}
	}

	public void mouseMoved (MouseEvent e, ZirkelCanvas zc, boolean simple)
	{	if (P==null || Other)
			zc.indicatePointsOrLines(e.getX(),e.getY());
		else if (PM==null)
			zc.indicateMoveableObjects(e.getX(),e.getY());
		else
			zc.clearIndicated();
	}

	public void mouseDragged (MouseEvent e, ZirkelCanvas zc)
	{	if (PM==null) return;
		((MoveableObject)PM).dragTo(zc.x(e.getX()),zc.y(e.getY()));
		zc.validate(); track(zc); zc.repaint();
	}

	public void mouseReleased (MouseEvent e, ZirkelCanvas zc)
	{	if (PM==null) return;
		PM.setSelected(false);
		PM=null;
		zc.repaint();
		showStatus(zc);
	}

	public void reset (ZirkelCanvas zc)
	{	zc.clearSelected();
		P=PM=null; PN=0;
		V=new Vector(); 
		showStatus(zc); zc.repaint();
	}

	public void showStatus (ZirkelCanvas zc)
	{	if (P==null || Other) zc.showStatus(
			Zirkel.name("message.tracker.select"));
		else if (PM==null) zc.showStatus(
			Zirkel.name("message.tracker.selectpoint"));
		else zc.showStatus(
			Zirkel.name("message.tracker.move"));
	}

	public void track (ZirkelCanvas zc)
	{	if (P==null) return;
		if (P instanceof PointObject)
		{	PointObject p=(PointObject)P;
			if (p.valid())
				V.addElement(new Coordinates(p.getX(),p.getY()));
		}
		else if (P instanceof PrimitiveLineObject)
		{	PrimitiveLineObject L=(PrimitiveLineObject)P;
			if (L.valid())
			{	if (!Started)
				{	X=L.getX(); Y=L.getY(); DX=L.getDX(); DY=L.getDY();
					Started=true;
				}
				else
				{	double x,y,dx,dy;
					x=L.getX(); y=L.getY(); dx=L.getDX(); dy=L.getDY();
					double det=dx*DY-dy*DX;
					if (Math.sqrt(Math.abs(det))>1e-9)
					{	double a=(-(X-x)*DY+DX*(Y-y))/(-det);
						V.addElement(new Coordinates(x+a*dx,y+a*dy));
					}
					X=x; Y=y; DX=dx; DY=dy;
				}
			}
		}
		for (int i=0; i<PN; i++)
		{	if (PO[i]==null || !PO[i].valid()) continue;
			if (PO[i] instanceof PointObject)
			{	PointObject p=(PointObject)PO[i];
				VO[i].addElement(new Coordinates(p.getX(),p.getY()));
			}
			else if (PO[i] instanceof PrimitiveLineObject)
			{	PrimitiveLineObject L=(PrimitiveLineObject)PO[i];
				if (!StartedO[i])
				{	XO[i]=L.getX(); YO[i]=L.getY();
					DXO[i]=L.getDX(); DYO[i]=L.getDY();
					StartedO[i]=true;
				}
				else
				{	double x,y,dx,dy;
					x=L.getX(); y=L.getY(); dx=L.getDX(); dy=L.getDY();
					double det=dx*DYO[i]-dy*DXO[i];
					if (Math.sqrt(Math.abs(det))>1e-9)
					{	double a=(-(XO[i]-x)*DYO[i]+
							DXO[i]*(YO[i]-y))/(-det);
						VO[i].addElement(new Coordinates(x+a*dx,y+a*dy));
					}
					XO[i]=x; YO[i]=y; DXO[i]=dx; DYO[i]=dy;
				}
			}
		}
	}

	public Enumeration elements ()
	{	return V.elements();
	}

	public void paint (MyGraphics g, ZirkelCanvas zc)
	{	if (P==null) return;
		Coordinates C;
		Enumeration e=V.elements();
		g.setColor(P);
		double c0,r0,c,r;
		int maxd=zc.width()/20;
		if (e.hasMoreElements())
		{	C=(Coordinates)e.nextElement();
			c0=zc.col(C.X); r0=zc.row(C.Y);
			while (e.hasMoreElements())
			{	C=(Coordinates)e.nextElement();
				c=zc.col(C.X); r=zc.row(C.Y);
				if (Math.abs(c0-c)<maxd && Math.abs(r0-r)<maxd)
					g.drawLine(c0,r0,c,r,P);
				else
					g.drawLine(c0,r0,c0,r0,P);
				c0=c; r0=r;
			}
		}
		for (int i=0; i<PN; i++)
		{	if (PO[i]==null) continue;
			g.setColor(PO[i]);
			e=VO[i].elements();
			if (e.hasMoreElements())
			{	C=(Coordinates)e.nextElement();
				c0=zc.col(C.X); r0=zc.row(C.Y);
				while (e.hasMoreElements())
				{	C=(Coordinates)e.nextElement();
					c=zc.col(C.X); r=zc.row(C.Y);
					if (Math.abs(c0-c)<maxd && Math.abs(r0-r)<maxd)
						g.drawLine(c0,r0,c,r,PO[i]);
					else
						g.drawLine(c0,r0,c0,r0,PO[i]);
					c0=c; r0=r;
				}
			}
		}
	}
	public void validate (ZirkelCanvas zc)
	{
	}

	public void save (XmlWriter xml)
	{	if (P==null) return;
		xml.startTagStart("Track");
		xml.printArg("track",P.getName());
		for (int i=0; i<PN; i++)
		{	xml.printArg("track"+i,PO[i].getName());
		}
		if (PM!=null) xml.printArg("move",PM.getName());
		xml.finishTagNewLine();
	}

	public boolean useSmartBoard ()
	{	return false;
	}
}
