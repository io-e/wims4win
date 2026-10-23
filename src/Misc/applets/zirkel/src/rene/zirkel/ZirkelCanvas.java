package rene.zirkel;

// file: ZirkelCanvas.java

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.*;
import java.util.zip.*;
import java.io.*;

import rene.util.xml.*;
import rene.gui.*;
import rene.dialogs.*;
import rene.zirkel.construction.*;
import rene.zirkel.constructors.*;
import rene.zirkel.dialogs.*;
import rene.zirkel.expression.*;
import rene.zirkel.graphics.*;
import rene.zirkel.listener.*;
import rene.zirkel.macro.*;
import rene.zirkel.objects.*;
import rene.zirkel.structures.*;
import rene.zirkel.tools.*;
import rene.util.*;
import rene.util.sort.*;

import atp.sHotEqn;

/**
 * @author Rene
 * Main class, doing most of the work in the C.a.R. application.
 * This canvas handles mouse and keyboard input, displays the
 * construction and has tools to save and load constructions.
 */
public class ZirkelCanvas extends Panel
	implements MouseListener,MouseMotionListener,ItemListener,
		AddEventListener,ActionListener,ChangedListener,MouseWheelListener
// the canvas to do the construction
{	ObjectConstructor OC=new MoverTool();
		// the current object constructor
	Construction C=new Construction(),COriginal;
		// the construction (collection of Constructor-Objects)
	StatusListener SL=null;
		// for displaying a status message
	public Image I=null;
	MyGraphics IG;
	Image Background=null;
	FontMetrics FM;
	double PointSize=4.0; // Size of a point
	double MinPointSize=3.0; // Default minimal point size
	double FontSize=12.0; // Size of font
	double MinFontSize=12.0; // Default minimal font size
	public int IW=0,IH=0; // Image and its dimensions
	public double Xmin,DX,Ymin,DY;
	boolean ShowHidden=false; // show hidden objects
	Frame F=new Frame();
	boolean ReadOnly;
	boolean AllowRightMouse=true;
	PopupMenu PM;
	CheckboxMenuItem CheckboxHidden;
	MenuItem Replay,Empty;
	boolean Job=false; // this is a job (save as such)
	String Last=""; // Last displayed job object
	Vector Targets=new Vector(); // Target names
	ConstructionObject TargetO[],TargetS[]; // Target objects and solutions
	int ShowColor=0; // 0=all, 1=green etc. (black displays always)
	static public double // factors for rounding
		EditFactor=1000000.0,
		LengthsFactor=100.0,
		AnglesFactor=1.0;
	public boolean 
		Visual=Global.getParameter("options.visual",true);
	boolean All;
	boolean Interactive=true;
	public int GridColor=0,GridThickness=ConstructionObject.THIN;
	public boolean ShowGrid=false,AxesOnly=false;
	public boolean GridLabels=true,GridBold=false,GridLarge=false;

	/**
	 * Initiate an empty construction. The display may have a popup
	 * menu (only for readonly displays).
	 * 
	 * @param readonly User cannot change the construction.
	 * @param replay Replay option in popup menu.
	 * @param hidden Show hidden option in popup menu.
	 */
	public ZirkelCanvas (boolean readonly, boolean replay, boolean hidden)
	{	ReadOnly=readonly;
		AllowRightMouse=!readonly;
		if (Global.getParameter("options.nopopupmenu",false))
		{	PM=null;
		}
		else if (ReadOnly)
		{	PM=new PopupMenu();
			CheckboxHidden=new MyCheckboxMenuItem(
				Zirkel.name("popup.hidden"));
			CheckboxHidden.addItemListener(this);
			if (hidden) PM.add(CheckboxHidden);
			Replay=new MyMenuItem(
				Zirkel.name("popup.replay"));
			Replay.addActionListener(this);
			if (replay) PM.add(Replay);
			if (hidden || replay) add(PM);
			else PM=null;
		}
		else
		{	PM=new PopupMenu();
			Empty=new MyMenuItem(
				Zirkel.name("popup.empty"));
			add(PM);
		}
		C.CL=this;
		clear();
		updateDigits();
		C.addAddEventListener(this);
		addMouseWheelListener(this);
	}
	public ZirkelCanvas (boolean readonly)
	{	this(readonly,true,true);
	}
	public ZirkelCanvas ()
	{	this(false,true,true);
	}
	
	public Dimension UseSize=null;
	
	public Dimension getMinimumSize ()
	{	if (Background==null || !Global.getParameter("background.usesize",false))
		{	if (UseSize!=null) return UseSize;
			else return new Dimension(600,600);
		}
		else
		{	int iw=Background.getWidth(this);
			if (iw<10) return new Dimension(600,600);
			int ih=Background.getHeight(this);
			if (Global.getParameter("background.usewidth",false))
			{	int w=getSize().width,h=(int)((double)ih/iw*w+0.5);
				return new Dimension(w,h);
			}
			else
				return new Dimension(iw,ih);
		}
	}
	
	public Dimension getPreferredSize ()
	{	return getMinimumSize();
	}

	public void updateDigits ()
	{	EditFactor=Math.pow(10,
			Global.getParameter("digits.edit",5));
		LengthsFactor=Math.pow(10,
			Global.getParameter("digits.lengths",5));
		AnglesFactor=Math.pow(10,
			Global.getParameter("digits.angles",0));
	}

	public void itemStateChanged (ItemEvent e)
	{	if (e.getSource()==CheckboxHidden)
		{	ShowHidden=CheckboxHidden.getState();
			repaint();
		}
	}

	ZirkelCanvasInterface ZCI;
	
	public void setZirkelCanvasListener (ZirkelCanvasInterface zci)
	{	ZCI=zci;
	}
	
	public String loadImage ()
	{	return ZCI.loadImage();
	}
	
	public Image doLoadImage (String filename)
	{	return ZCI.doLoadImage(filename);
	}
	
	public void actionPerformed (ActionEvent e)
	{	if (!Interactive) return;
		if (e.getSource()==Replay)
		{	if (ZCI!=null) ZCI.replayChosen();
		}
		else
		{	Enumeration en=Macros.elements();
			while (en.hasMoreElements())
			{	MacroItem m=(MacroItem)en.nextElement();
				if (m.I==e.getSource())
				{	if (ZCI!=null) ZCI.runMacro(m.M);
					break;
				}
			}
		}
	}

	// Some transfer functions to compute screen coordinates etc.
	
	public double col (double x)
	{	return (x-Xmin)/DX*IW;
	}
	public double row (double y)
	{	return IH-(y-Ymin)/DY*IH;
	}
	public int width ()
	{	return IW;
	}
	public int height ()
	{	return IH;
	}
	public double x (int c)
	{	return Xmin+DX*c/IW;
	}
	public double y (int r)
	{	return Ymin+DY*(IH-r)/IH;
	}
	public double dx (int c)
	{	return DX*c/IW;
	}
	public double dy (int r)
	{	return DY*r/IH;
	}
	public double dx (double c)
	{	return DX*c/IW;
	}
	public double dy (double r)
	{	return DY*r/IH;
	}
	public double maxX ()
	{	return Xmin+DX;
	}
	public double minX ()
	{	return Xmin;
	}
	public double maxY ()
	{	return Ymin+DY;
	}
	public double minY ()
	{	return Ymin;
	}
	public boolean isInside (double x, double y)
	{	return x>=Xmin && x<Xmin+DX && y>=Ymin && y<Ymin+DY;
	}
	public double dCenter (double x, double y)
	{	double dx=x-(Xmin+DX/2),dy=y-(Ymin+DY/2);
		return Math.sqrt(dx*dx+dy*dy)/
			Math.max(DX/2,DY/2);
	}
	public void recompute ()
	{	if (IH<IW)
		{	Xmin=C.getX()-C.getW();
			DX=C.getW()*2;
			DY=DX/IW*IH;
			Ymin=C.getY()-DY/2;
		}
		else
		{	Ymin=C.getY()-C.getW();
			DY=C.getW()*2;
			DX=DY/IH*IW;
			Xmin=C.getX()-DY/2;
		}
		C.setH(DY);
		if (DX>0) C.setPixel(getSize().width/DX);
	}

	DoneListener DL;
	public void setDoneListener (DoneListener dl) { DL=dl; }

	/**
	Add an item to the construction and re-paint the construction.
	*/
	public void addObject (ConstructionObject o)
	// called by the ObjectConstructor
	{	C.add(o);
		if (Preview)
		{	o.setIndicated(true);
			o.setSelectable(false);
		}
		C.updateCircleDep();
	}
	
	/**
	* A construction added an item. 
	* Check, if it solves a problem and notify the DoneListener, if so.
	*/
	public void added (Construction c, ConstructionObject o)
	{	if (displayJob() && TargetO.length>0)
		{	boolean olddone=true;
			for (int i=0; i<TargetO.length; i++)
			{	if (TargetS[i]!=null && !TargetS[i].isInConstruction())
					TargetS[i]=null;
				if (TargetS[i]!=null) continue;
				else olddone=false;
				if (TargetO[i].equals(o))
				{	TargetS[i]=o;
				}
			}
			boolean done=true;
			for (int i=0; i<TargetO.length; i++)
			{	if (TargetS[i]==null)
				{	done=false; break;
				}
			}
			if (done && DL!=null && !olddone)
			{	repaint();
				DL.notifyDone();
				freeJob();
			}
		}
		repaint();
	}

	public boolean check (Construction c)
	{	int n=TargetO.length;
		for (int i=0; i<n; i++)
		{	TargetS[i]=null;
		}
		int found=0;
		Enumeration e=c.elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o.isKeep()) continue;
			for (int i=0; i<n; i++)
			{	if (TargetS[i]!=null) continue;
				if (o.equals(TargetO[i]))
				{	TargetS[i]=o;
					found++; break;
				}
			}
		}
		return found==n;
	}
	
	/**
	 * Check if the solution to the current problem has been found.
	 */
	public void check ()
	{	if (displayJob() && TargetO.length>0)
		{	boolean olddone=true;
			for (int i=0; i<TargetO.length; i++)
			{	if (TargetS[i]!=null && !TargetS[i].isInConstruction())
					TargetS[i]=null;
				if (TargetS[i]!=null) continue;
				else olddone=false;
			}
			boolean done=check(C);
			if (done && DL!=null && !olddone)
			{	repaint();
				DL.notifyDone();
				freeJob();
			}
		}
	}

	int Moved=0;
	
	boolean Dragging=false,RightClicked=false;
	
	boolean Control=false; // Control-Taste bei letztem Mausdruck aktiv?
	
	public boolean SmartBoardPreview=false;
	
	// mouse events:
	public synchronized void mousePressed (MouseEvent e)
	{	if (!Interactive) return;
		SmartBoardPreview=false;
		clearIndicated();
		clearPreview();
		repaint();
		requestFocus();
		Dragging=false;
		RightClicked=false;
		Moved=0;
		if (e.isMetaDown() && AllowRightMouse) // right mouse button!
		{	if (!ReadOnly)
			{	ConstructionObject o=selectLabel(e.getX(),e.getY());
				if (o!=null)
				{	Dragging=true;
					setTool(new LabelMover(OC,this,e.getX(),e.getY(),
						o,e.isShiftDown()));
					return;
				}
				if (e.isShiftDown() && e.isControlDown()) // hiding shortcut
				{	o=selectObject(e.getX(),e.getY());
					if (o==null) return;
					o.toggleHidden();
					repaint();
					return;
				}
				if (e.isControlDown()) // edit conditionals
				{	o=selectObject(e.getX(),e.getY());
					if (o==null) return;
					new EditConditionals(getFrame(),o);
					validate();
					repaint();
					return;
				}
			}
			ConstructionObject p=selectImmediateMoveableObject(
				e.getX(),e.getY());
			RightClicked=true;
			if (p!=null)
			{	Dragging=true;
				setTool(new MetaMover(OC,this,p,e));
				return;
			}
			else if (!Global.getParameter("options.nomousezoom",false)) // Drag mit rechter Maustaste
			{	if (selectObjects(e.getX(),e.getY()).size()==0)
				{	setTool(new ZoomerTool(OC,e,this));
				}
				return;
			}
		}
		else // left mouse button!
		{	if (!SmartBoardPreview && Global.getParameter("smartboard",false) 
				&& OC.useSmartBoard())
			{	OC.mouseMoved(e,this,
					Global.getParameter("options.indicate.simple",false));
				SmartBoardPreview=true;
				return;
			}
			else
			{	Control=e.isControlDown();
				OC.mousePressed(e,this); // pass to ObjectConstructor
				Control=false;
			}
		}
	}

	public synchronized void mouseReleased (MouseEvent e)
	{	if (!Interactive) return;
		if (DT!=null) DT.waitReady();
		if (RightClicked)
		{	RightClicked=false;
			OC.mouseReleased(e,this);
			if (Moved<=2 && AllowRightMouse && !ReadOnly)
			{	MyVector v=selectObjects(e.getX(),e.getY());
				if (v.size()>0)
				{	ConstructionObject o=select(v,e.getX(),e.getY());
					if (o!=null)
					{	new EditTool().mousePressed(e,o,this);
						check();
						return;
					}
					else
					repaintCD();
					return;
				}
			}
			if (Moved<=2 && PM!=null && !Global.getParameter("restricted",false))
			{	int n=2;
				if (ReadOnly || !Global.getParameter("options.doubleclick",false))
					n=1;
				if (e.getClickCount()>=n && 
						(ReadOnly || Macros.size()!=0))
					PM.show(e.getComponent(),e.getX(),e.getY());
			}
			repaintCD();
			return;
		}
		if (SmartBoardPreview && Global.getParameter("smartboard",false))
		{	Control=e.isControlDown();
			clearIndicated();
			clearPreview();
			repaint();
			Dragging=false;
			OC.mousePressed(e,this);
			SmartBoardPreview=false;
			mouseReleased(e);
			return;
		}
		if (!Dragging)
		{	OC.mouseReleased(e,this);
			Dragging=false;
			repaintCD();
			return;
		}
		if (Moved<=1)
		{	if (OC instanceof LabelMover)
			{	((LabelMover)OC).resetPoint();
				OC.mouseReleased(e,this);
			}
			else if (OC instanceof MetaMover)
			{	OC.mouseReleased(e,this);
				if (!ReadOnly) new EditTool().mousePressed(e,this);
			}
		}
		else OC.mouseReleased(e,this);
		repaintCD();
		check();
		Dragging=false;
	}

	public synchronized void mouseClicked (MouseEvent e) {}
	
	public synchronized void mouseExited (MouseEvent e)
	{	if (!Interactive) return;
		clearIndicated();
		clearPreview();
		repaint();
		SmartBoardPreview=false;
		RightClicked=false;
		repaintCD();
	}
	
	public void mouseEntered (MouseEvent e) {}
	
	public synchronized void mouseMoved (MouseEvent e)
	{	if (!Interactive ||
			!Global.getParameter("options.indicate",true)) return;
		if (Global.getParameter("smartboard",false)) return;
		Count.setAllAlternate(true);
		OC.mouseMoved(e,this,Global.getParameter("options.indicate.simple",false));
		Count.setAllAlternate(false);
		repaintCD();
	}
	
	DragThread DT=null;

	public synchronized void mouseDragged (MouseEvent e)
	{	if (!Interactive) return;
		if (DT==null) DT=new DragThread(this);
		if (SmartBoardPreview 
			&& Global.getParameter("smartboard",false)) 
				OC.mouseMoved(e,this,
					Global.getParameter("options.indicate.simple",false));
		else 
		{	DT.mouseDragged(e);
			Moved++;
		}
		repaintCD();
	}
	
	public synchronized void doMouseDragged (MouseEvent e)
	{	OC.mouseDragged(e,this);
	}
	
	ConstructionObject LastPaint=null;

	public void newImage ()
	{	I=null; repaint();
	}

	boolean Frozen=false;
	public void setFrozen (boolean f)
	{	Frozen=f;
	}
	
	MessageWindow MW;

	final double PointSizeFactor=240.0;
	
	int CC=0;
	
	// repaint events
	public synchronized void paint (Graphics g)
	{	if (Frozen) return;
		int w=getSize().width,h=getSize().height;
		if (I==null || IW!=w || IH!=h)
		{	if (w==0 || h==0) return;
			IW=w; IH=h;
			I=createImage(IW,IH);
			if (!Global.getParameter("printscalepreview",false)
				|| !Global.getParameter("printscale.sizes",false))
			{	if (!Global.getParameter("simplegraphics",false))
				{	IG=new MyGraphics13(I.getGraphics(),this);
					IG.setSize(IW,IH);
				}
				else
				{	IG=new MyGraphics11(I.getGraphics());
					IG.setSize(IW,IH);
				}
				PointSize=IH/PointSizeFactor;
				FontSize=PointSize*4;
				MinPointSize=Global.getParameter("minpointsize",3);
				if (PointSize<MinPointSize) PointSize=MinPointSize;
				MinFontSize=Global.getParameter("minfontsize",12);
				if (FontSize<MinFontSize) FontSize=MinFontSize;
				IG.setDefaultFont((int)FontSize,
					Global.getParameter("font.large",false),
					Global.getParameter("font.bold",false));
			}
			else
			{	double dpi=IW/
					Global.getParameter("printscale.width",5.0);
				if (!Global.getParameter("simplegraphics",false))
				{	IG=new MyGraphics13(I.getGraphics(),
						Global.getParameter("printscale.linewidth",0.02)*dpi,
						this,null);
					IG.setSize(IW,IH);
				}
				else
				{	IG=new MyGraphics11(I.getGraphics());
					IG.setSize(IW,IH);
				}
				PointSize=Global.getParameter("printscale.pointsize",0.07)*dpi;
				FontSize=Global.getParameter("printscale.textsize",0.3)*dpi;
				IG.setDefaultFont((int)FontSize,
					Global.getParameter("font.large",false),
					Global.getParameter("font.bold",false));
			}
			recompute();
			C.dovalidate();
		}
		IG.clearRect(0,0,IW,IH,getBackground());			
		if (Background!=null)
		{	int bw=Background.getWidth(this),
				bh=Background.getHeight(this);
			if (bw==IW && bh==IH)
			{	IG.drawImage(Background,0,0,this);
			}
			else if (Global.getParameter("background.tile",true)
				&& bw<IW && bh<IH)
			{	for (int i=(IW%bw)/2-bw; i<IW; i+=bw)
					for (int j=(IH%bh)/2-bh; j<IH; j+=bh)
						IG.drawImage(Background,i,j,this);
			}
			else if (Global.getParameter("background.center",true))
			{	IG.drawImage(Background,(IW-bw)/2,(IH-bh)/2,this);
			}
			else
			{	IG.drawImage(Background,0,0,IW,IH,this);
			}
		}
		if (MW==null) 
		{	C.computeTracks(this);
			dopaint(IG);
		}
		if (Global.getParameter("printscale.middle",false) &&
			Global.getParameter("printscalepreview",false))
		{	
			//IG.setColor(Color.red);
			int PW=Global.getParameter("printscale.w",IW);
			int PH=Global.getParameter("printscale.h",IH);
			IG.drawRect(IW/2-PW/2-3,IH/2-PH/2-3,PW+7,PH+7);
		}
		g.drawImage(I,0,0,this);
	}

	public synchronized void resetGraphics ()
	{	I=null; repaint();
	}
	
	MyVector Breaks=new MyVector();
	
	public void updateBreakHide ()
	{	Breaks.removeAllElements();
		Enumeration e=C.elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o==LastPaint || o==C.last()) break;
			if (o.isBreak()) Breaks.addElement(o);
		}
		e=C.elements();
		ConstructionObject NextBreak=null;
		Enumeration eb=Breaks.elements();
		if (eb.hasMoreElements()) NextBreak=(ConstructionObject)eb.nextElement();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (NextBreak!=null && NextBreak.isHideBreak()) 
				o.setBreakHide(true);
			else 
				o.setBreakHide(false);
			if (o==NextBreak)
			{	if (eb.hasMoreElements()) 
					NextBreak=(ConstructionObject)eb.nextElement();
				else NextBreak=null;
			}
			if (o==LastPaint) break;
		}
	}

	void dopaint (MyGraphics IG)
	{	
		if (ShowGrid) 
		{	if (AxesOnly) paintAxes(IG);
			else paintGrid(IG);
		}
		long time=System.currentTimeMillis();
		updateBreakHide();
		// count z-buffer elements and mark all as non-painted
		Enumeration e=C.elements();
		int n=0;
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			o.HasZ=false;
			o.IsDrawn=false;
			try
			{	if (!o.selected())
				{	o.Value=-o.getZ(); o.HasZ=true; n++;
				}
			}
			catch (Exception ex) {}
			if (o==LastPaint) break;
		}
		// paint background objects
		e=C.elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o.isBack() && !o.HasZ)
			{	o.paint(IG,this); o.IsDrawn=true;
			}
			if (o==LastPaint) break;
		}
		// paint objects with z-buffer value in their order
		if (n>0)
		{	ConstructionObject os[]=new ConstructionObject[n];
			e=C.elements();
			n=0;
			while (e.hasMoreElements())
			{	ConstructionObject o=(ConstructionObject)e.nextElement();
				if (o.HasZ) os[n++]=o;
			}
			Sorter.sort(os);
			for (int i=0; i<n; i++)
			{	os[i].paint(IG,this);
				os[i].IsDrawn=true;
			}
		}
		// paint non-selected objects
		e=C.elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (!o.selected() && !o.IsDrawn)
			{	o.paint(IG,this);
				o.IsDrawn=true;
			}
			if (o==LastPaint) break;
		}
		// paint selected objects
		e=C.elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (!o.IsDrawn) 
			{	o.paint(IG,this);
				o.IsDrawn=true;
			}
			if (o==LastPaint) break;
		}
		if (LastPaint==null) paintTrack(IG);
		if (Interactive && IndicatePoint!=null) IndicatePoint.paint(IG,this);
		time=System.currentTimeMillis()-time;
		if (time>250 && PolygonDrawer.step<50) 
			PolygonDrawer.step=PolygonDrawer.step*2;
		if (time<100 && PolygonDrawer.step>4) 
			PolygonDrawer.step=PolygonDrawer.step/2;
		paintDrawings(IG);
	}
	
	void dopaintFig (MyGraphicsFig IG)
	{	if (ShowGrid) paintGrid(IG);
		IG.pushLayer(true);
		int layer=0;
		while (true)
		{	IG.pushLayer();
			int l=0;
			Enumeration e=C.elements();
			while (e.hasMoreElements())
			{	ConstructionObject o=(ConstructionObject)e.nextElement();
				if (o==LastPaint) break;
				if (l==layer && o.isBack()) o.paint(IG,this);
				if (o.isBreak())
				{	l++;
					if (l>layer) break;
				}
			}
			if (l<layer) break;
			l=0;
			e=C.elements();
			while (e.hasMoreElements())
			{	ConstructionObject o=(ConstructionObject)e.nextElement();
				if (o==LastPaint) break;
				if (l==layer && !o.isBack() && !o.selected()) o.paint(IG,this);
				if (o.isBreak())
				{	l++;
					if (l>layer) break;
				}
			}
			l=0;
			e=C.elements();
			while (e.hasMoreElements())
			{	ConstructionObject o=(ConstructionObject)e.nextElement();
				if (o==LastPaint) break;
				if (l==layer && !o.isBack() && o.selected()) o.paint(IG,this);
				if (o.isBreak())
				{	l++;
					if (l>layer) break;
				}
			}
			layer++;
		}
		IG.pushLayer(false);
		if (LastPaint==null) paintTrack(IG);
	}
	
	int xg[]=new int[64],yg[]=new int[64];
	
	public void setGrid ()
	{	ShowGrid=Global.getParameter("showgrid",false);
		AxesOnly=Global.getParameter("axesonly",false);
		GridThickness=Global.getParameter("grid.thickness",ConstructionObject.THIN);
		GridColor=Global.getParameter("grid.colorindex",0);
		GridBold=Global.getParameter("grid.bold",false);
		GridLarge=Global.getParameter("grid.large",false);
		GridLabels=Global.getParameter("grid.labels",true);
	}
	
	/**
	 * Paint axes and grid.
	 * @param IG
	 */
	void paintGrid (MyGraphics IG)
	{
		if (GridThickness==ConstructionObject.NORMAL)
			IG.setColor(ZirkelFrame.Colors[Global.getParameter("grid.colorindex",0)]);
		else
			IG.setColor(ZirkelFrame.LightColors[GridColor]);			
		IG.setFont(GridLarge,GridBold);
		double gridsize=getGridSize();
		//int dd=IH/200;
		// evers 14/03/2008
		int dd=IH;
		//if (dd<1) dd=1;
		double x1=Math.floor(
			(C.getX()-C.getW())/gridsize-1)*gridsize;
		int xi=0;
		while (x1<(C.getX()+C.getW()) && xi<64)
		{	int c=(int)col(x1);
			xg[xi++]=c;
			x1+=gridsize;
		}
		double y1=Math.floor(
			(C.getY()-C.getW())/gridsize-1)*gridsize;
		int yi=0;
		while (y1<(C.getY()+C.getW()) && yi<64)
		{	int r=(int)row(y1);
			yg[yi++]=r;
			y1+=gridsize;
		}
		for (int i=0; i<xi; i++)
		{	for (int j=0; j<yi; j++)
			{	IG.drawLine(xg[i],yg[j]-dd,xg[i],yg[j]+dd);
				IG.drawLine(xg[i]-dd,yg[j],xg[i]+dd,yg[j]);
			}
		}
		if (GridThickness!=ConstructionObject.INVISIBLE)
		{	if (0<C.getX()+C.getW() && 0>C.getX()-C.getW())
			{	int c=(int)col(0);
				if (GridThickness!=ConstructionObject.THICK)
					IG.drawLine(c,0,c,IH);
				else
					IG.drawThickLine(c,0,c,IH);
			}
			if (0<C.getY()+C.getW() && 0>C.getY()-C.getW())
			{	int r=(int)row(0);
				if (GridThickness!=ConstructionObject.THICK)
					IG.drawLine(0,r,IW,r);
				else
					IG.drawThickLine(0,r,IW,r);
			}
		}

		dd=dd*2;
		double labelsize=Math.pow(10,Math.floor(
			Math.log(C.getW()*2)/Math.log(10)))/10;
		while (C.getW()*2/labelsize>=10) labelsize*=10;
		if (C.getW()*2/labelsize<5) labelsize/=2;
		FontMetrics fm=IG.getFontMetrics();
		int FH=fm.getHeight();
		x1=Math.floor(
			(C.getX()-C.getW())/labelsize-1)*labelsize;
		int lh=(int)row(0);
		if (lh<0 || lh>IH-FH){	
		    lh=IH-FH;
		}

		while (x1<(C.getX()+C.getW()))
		{	int c=(int)col(x1);
			String s=format(x1);
			if (s.length()>0) 
			{	if (GridLabels) IG.drawString(s,c+3,lh+FH);
				IG.drawLine(c,lh-dd,c,lh+dd);
			}
			x1+=labelsize;
		}
		int lw=(int)col(0);
		if (lw<0 || lw>IW-20)
		{	lw=0;
		}
		y1=Math.floor((C.getY()-C.getW())/labelsize-1)*labelsize;
		while (y1<(C.getY()+C.getW()))
		{	int r=(int)row(y1);
			String s=format(y1);
			if (s.length()>0) 
			{	if (GridLabels) IG.drawString(s,lw+3,r+FH);
				IG.drawLine(lw-dd,r,lw+dd,r);
			}
			y1+=labelsize;
		}
		//axes jm.evers
		if(!Zirkel.IsApplet){
		    IG.drawThickLine((int)col(0),0,(int)col(0),IH);
		    IG.drawThickLine(0,(int)row(0),IW,(int)row(0));
		    IG.setColor(Color.red);
		    FontMetrics tm=IG.getFontMetrics();
		    int fh=tm.getHeight();
		    String yas=ZirkelApplet.yaxis_text+" ";
		    String xas=ZirkelApplet.xaxis_text;
		    IG.drawString(xas,(double)(col(0) + 3*fh),(double)(row(0) + fh));
		    int sy=yas.length();
		    if(ZirkelApplet.yaxis_text_vertical){
			String char_y="";int p=0;int begin=0;int end=1;int r=0;
			while( p < sy ){
			    char_y=yas.substring(begin,end);
			    if(char_y.indexOf(" ") == -1){
				end++;
			    }
			    else
			    {
				IG.drawString(char_y,(double)(col(0) - 2*fh),(double)(row(0) - (8*fh) + r*fh));
				begin=end;
				end++;
				r++;
			    }
			    p++;
			}
		    }
		    else
		    {
			IG.drawString(yas,(double)(col(0) - (sy*fh)/2),(double)(row(0) - 10*fh));
		    }
		}
		// end jm.evers
	}
	
	/**
	 * Paint only the coordinate axes (no grid)
	 * @param IG
	 */
	void paintAxes (MyGraphics IG)
	{	if (GridThickness==ConstructionObject.NORMAL)
			IG.setColor(ZirkelFrame.Colors[GridColor]);
		else
			IG.setColor(ZirkelFrame.LightColors[GridColor]);			
		IG.setFont(GridLarge,GridBold);
		double gridsize=getGridSize();
		double x1=Math.floor(
			(C.getX()-C.getW())/gridsize-1)*gridsize;
		int r0=(int)row(0);
		//int dd=IH/200;
		// evers 14/03/2008
		int dd=IH;
		//if (dd<1) dd=1;
		while (x1<(C.getX()+C.getW()))
		{	int c=(int)col(x1);
			IG.drawLine(c,r0-dd,c,r0+dd);
			x1+=gridsize;
		}
		double y1=Math.floor(
			(C.getY()-C.getW())/gridsize-1)*gridsize;
		int c0=(int)col(0);
		while (y1<(C.getY()+C.getW()))
		{	int r=(int)row(y1);
			IG.drawLine(c0-dd,r,c0+dd,r);
			y1+=gridsize;
		}

		if (GridThickness!=ConstructionObject.INVISIBLE)
		{	if (0<C.getX()+C.getW() && 0>C.getX()-C.getW())
			{	int c=(int)col(0);
				if (GridThickness!=ConstructionObject.THICK)
					IG.drawLine(c,0,c,IH);
				else
					IG.drawThickLine(c,0,c,IH);
			}
			if (0<C.getY()+C.getW() && 0>C.getY()-C.getW())
			{	int r=(int)row(0);
				if (GridThickness!=ConstructionObject.THICK)
					IG.drawLine(0,r,IW,r);
				else
					IG.drawThickLine(0,r,IW,r);
			}
		}
		// evers 14/03/2008
		//dd=dd*2;
		double labelsize=Math.pow(10,Math.floor(
			Math.log(C.getW()*2)/Math.log(10)))/10;
		while (C.getW()*2/labelsize>=10) labelsize*=10;
		if (C.getW()*2/labelsize<5) labelsize/=2;
		FontMetrics fm=IG.getFontMetrics();
		int FH=fm.getHeight();
		x1=Math.floor(
			(C.getX()-C.getW())/labelsize-1)*labelsize;
		int lh=(int)row(0);
		if (lh<0 || lh>IH-FH){	lh=IH-FH; }
		while (x1<(C.getX()+C.getW()))
		{	int c=(int)col(x1);
			String s=format(x1);
			if (GridLabels)	IG.drawString(s,c+4,lh+FH);
			if (s.length()>0) IG.drawLine(c,lh-dd,c,lh+dd);
			x1+=labelsize;
		}
		int lw=(int)col(0);
		if (lw<0 || lw>IW-20)
		{	lw=0;
		}
		y1=Math.floor(
			(C.getY()-C.getW())/labelsize-1)*labelsize;
		while (y1<(C.getY()+C.getW()))
		{	int r=(int)row(y1);
			String s=format(y1);
			if (GridLabels) IG.drawString(s,lw+3,r+FH);
			if (s.length()>0) IG.drawLine(lw-dd,r,lw+dd,r);
			y1+=labelsize;
		}

	}
	
	
	public double pointSize ()
	{	return PointSize;
	}
	
	public double SelectionPointFactor=Global.getParameter("selectionsize",1.5);
	
	public double selectionSize ()
	{	return SelectionPointFactor*PointSize;
	}
	
	public double Scale=1.0;
	
	public int scale (int x)
	{	return (int)(Scale*x);
	}
	
	static char c[]=new char[20];
	int nc;
	public String format (double x)
	{	// double xx=x;
		nc=0;
		boolean minus=false;
		if (x<-1e-12)
		{	minus=true; x=-x;
		}
		x+=1e-12;
		double h=x-Math.floor(x);
		if (rekformat(h,8)) c[nc++]='.';
		while (x>=1)
		{	double s=Math.floor(x/10);
			c[nc++]=(char)('0'+(int)(x-s*10));
			x=s;
		}
		if (nc>0 && minus) c[nc++]='-';
		// revert c:
		for (int i=0; i<nc/2; i++)
		{	char hc=c[nc-1-i];
			c[nc-1-i]=c[i];
			c[i]=hc;
		}
		// System.out.println(xx+" -> "+new String(c,0,nc));
		return new String(c,0,nc);
	}
	
	boolean rekformat (double h, int k)
	{	h=h*10;
		double x=Math.floor(h);
		if (k==0)
		{	int i=(int)x;
			if (i>0)
			{	c[nc++]=(char)('0'+i);
				return true;
			}
			else return false;
		}
		else
		{	int i=(int)x;
			if (rekformat(h-x,k-1) || i>0)
			{	c[nc++]=(char)('0'+i);
				return true;
			}
			else return false;
		}
	}
	
	public void update (Graphics g)
	{	paint(g);
	}
	
	public void paintUntil (ConstructionObject o)
	{	if (LastPaint==o) return;
		LastPaint=o;
		repaint();
	}
	
	// validate all elements
	public void validate ()
	{	dovalidate();
		if (OC instanceof TrackPainter)
			((TrackPainter)OC).validate(this);
	}
	
	/**
	 * Synchronized update of the construction to avoid a repaint
	 * during update.
	 */
	synchronized public void dovalidate ()
	{	C.dovalidate();
	}

	// selection routines:

	// This vector is used by ALL selection and indication routines for
	// performance reasons.
	MyVector V=new MyVector();

	public void sort (MyVector V)
	{	if (V.size()<2) return;
		Sorter.QuickSort(V.getArray(),0,V.size()-1);
	}
	
	/**
	Slow function to resort a vector by the number of each element in
	the construction.
	*/
	public void sortRow (MyVector V)
	{	ConstructionObject o[]=new ConstructionObject[V.size()];
		V.copyInto(o);
		V.removeAllElements();
		Enumeration e=C.elements();
		while (e.hasMoreElements())
		{	ConstructionObject oo=(ConstructionObject)e.nextElement();
			for (int i=0; i<o.length; i++)
			{	if (o[i]==oo)
				{	V.addElement(oo);
					break;
				}
			}
		}
	}

	/**
	select a circle or a line.
	**/
	public ConstructionObject selectCircleLine (int x, int y, boolean multiple)
	// select a circle or a line
	{	MyVector v=selectCircleLineObjects(x,y,multiple,false);
		return select(v);
	}
	public ConstructionObject selectCircleLine (int x, int y)
	// select a circle or a line
	{	return selectCircleLine(x,y,true);
	}
	
	/**
	select a circle or a line.
	**/
	public ConstructionObject selectPointonObject (int x, int y, boolean multiple)
	// select a circle or a line
	{	MyVector v=selectPointonObjects(x,y,multiple,false);
		return select(v);
	}
	public ConstructionObject selectPointonObject (int x, int y)
	// select a circle or a line
	{	return selectPointonObject(x,y,true);
	}
	
	/**
	Select all possible circles or lines at x,y. If a matching
	non-filled object is found above a filled object, the latter is
	never selected.
	@param multiple determines, if it is possible to select selected
	objects.
	@param testlocal determines, if objects that look locally like
	already selected objects should be omitted.
	*/
	public MyVector selectCircleLineObjects (int x, int y, boolean multiple, 
		boolean testlocal)
	// select a circle or a line
	{	Enumeration e=C.elements();
		V.removeAllElements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o.isSelectable() && (o instanceof PrimitiveLineObject ||
				o instanceof PrimitiveCircleObject)
				&& o.nearto(x,y,true,this) &&
				(multiple || !o.selected()))
			{	if (testlocal)
				{	Enumeration ev=V.elements();
					while (ev.hasMoreElements())
					{	ConstructionObject ov=(ConstructionObject)ev.nextElement();
						if (o.locallyLike(ov))
						{	o=null;
							break;
						}
					}
				}
				if (o!=null)
				{	V.addElement(o);
				}
			}
		}
		return V;
	}
	public MyVector selectCircleLineObjects (int x, int y, boolean multiple)
	{	return selectCircleLineObjects(x,y,multiple,false);
	}
	public MyVector selectCircleLineObjects (int x, int y)
	{	return selectCircleLineObjects(x,y,true,false);
	}

	/**
	Select all possible object at x,y that can carry a point.
	@param multiple determines, if it is possible to select selected
	objects.
	@param testlocal determines, if objects that look locally like
	already selected objects should be omitted.
	*/
	public MyVector selectPointonObjects (int x, int y, boolean multiple, 
		boolean testlocal)
	// select a circle or a line
	{	Enumeration e=C.elements();
		V.removeAllElements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o.isSelectable() && (o instanceof PointonObject)
				&& o.nearto(x,y,true,this) &&
				(multiple || !o.selected()))
			{	if (testlocal)
				{	Enumeration ev=V.elements();
					while (ev.hasMoreElements())
					{	ConstructionObject ov=(ConstructionObject)ev.nextElement();
						if (o.locallyLike(ov))
						{	o=null;
							break;
						}
					}
				}
				if (o!=null)
				{	V.addElement(o);
				}
			}
		}
		return V;
	}
	public MyVector selectPointonObjects (int x, int y, boolean multiple)
	{	return selectPointonObjects(x,y,multiple,false);
	}
	public MyVector selectPointonObjects (int x, int y)
	{	return selectPointonObjects(x,y,true,false);
	}

	/**
	Select all selectable objects near to x,y.
	@param multiple allows multiple selections.
	**/	
	public MyVector selectObjects (int x, int y, boolean multiple)
	{	Enumeration e=C.elements();
		V.removeAllElements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o.isSelectable() && o.nearto(x,y,this) && (multiple || !o.selected()))
				V.addElement(o);
		}
		return V;
	}

	public MyVector selectObjects (int x, int y)
	{	return selectObjects(x,y,true);
	}

	/**
	Select a single object near x,y.
	**/
	public ConstructionObject selectObject (int x, int y, boolean multiple)
	{	MyVector v=selectObjects(x,y,multiple);
		return select(v,x,y);
	}

	public ConstructionObject selectObject (int x, int y)
	{	return selectObject(x,y,true);
	}

	/**
	select all constructable objects near x,y.
	**/
	public MyVector selectConstructableObjects (int x, int y, boolean multiple)
	// select an enumeration of objects
	{	Enumeration e=C.elements();
		V.removeAllElements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o.valid() && o.isFlag() && o.nearto(x,y,this) &&
				(multiple || !o.selected()))
				V.addElement(o);
		}
		return V;
	}

	public MyVector selectConstructableObjects (int x, int y)
	{	return selectConstructableObjects(x,y,true);
	}

	/**
	select a single constructable object near x,y.
	**/
	public ConstructionObject selectConstructableObject (int x, int y)
	{	MyVector v=selectConstructableObjects(x,y,true);
		return select(v,x,y); // user determines
	}


	/**
	Select a single line, segment, ray, perp., paral. or fixed angle
	near x,y.
	**/
	public PrimitiveLineObject selectLine (int x, int y, boolean multiple)
	{	selectLineObjects(x,y,multiple);
		return (PrimitiveLineObject)select(V);
	}
	public void selectLineObjects (int x, int y, boolean multiple)
	{	Enumeration e=C.elements();
		V.removeAllElements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o.isSelectable() && (o instanceof PrimitiveLineObject)
				&& o.nearto(x,y,this)
				&& (multiple || !o.selected()))
				V.addElement(o);
		}
	}
	public PrimitiveLineObject selectLine (int x, int y)
	{	return selectLine(x,y,true);
	}

	/**
	Select a point or a line (for the object tracker).
	*/
	public void selectPointsOrLines (int x, int y, boolean multiple)
	{	Enumeration e=C.elements();
		V.removeAllElements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o.isSelectable() && (o instanceof PrimitiveLineObject
				|| o instanceof PointObject)
				&& o.nearto(x,y,this)
				&& (multiple || !o.selected()))
				V.addElement(o);
		}
	}

	/**
	Select a single line, segment, or ray near x,y
	**/
	public TwoPointLineObject selectTwoPointLine (int x, int y, boolean multiple)
	{	selectTwoPointLines(x,y,multiple);
		return (TwoPointLineObject)select(V);
	}
	public void selectTwoPointLines (int x, int y, boolean multiple)
	{	Enumeration e=C.elements();
		V.removeAllElements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o.isSelectable() && (o instanceof TwoPointLineObject)
				&& o.nearto(x,y,this)
				&& (multiple || !o.selected()))
				V.addElement(o);
		}
	}
	public TwoPointLineObject selectTwoPointLine (int x, int y)
	{	return selectTwoPointLine(x,y,true);
	}
	
	/**
	Select a single segment near x,y
	**/
	public SegmentObject selectSegment (int x, int y, boolean multiple)
	{	selectSegments(x,y,multiple);
		return (SegmentObject)select(V);
	}
	public void selectSegments (int x, int y, boolean multiple)
	{	Enumeration e=C.elements();
		V.removeAllElements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o.isSelectable() && (o instanceof SegmentObject)
				&& o.nearto(x,y,this)
				&& (multiple || !o.selected()))
				V.addElement(o);
		}
	}
	public SegmentObject selectSegment (int x, int y)
	{	return selectSegment(x,y,true);
	}
	
	/**
	Select a single ray near x,y
	**/
	public RayObject selectRay (int x, int y, boolean multiple)
	{	selectRays(x,y,multiple);
		return (RayObject)select(V);
	}
	public void selectRays (int x, int y, boolean multiple)
	{	Enumeration e=C.elements();
		V.removeAllElements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o.isSelectable() && (o instanceof RayObject)
				&& o.nearto(x,y,this)
				&& (multiple || !o.selected()))
				V.addElement(o);
		}
	}
	public RayObject selectRay (int x, int y)
	{	return selectRay(x,y,true);
	}
	
	/**
	Select a circle near x,y. A non-filled object is preferred before
	a filled object.
	**/
	public PrimitiveCircleObject selectCircle (int x, int y, boolean multiple)
	{	selectCircles(x,y,multiple);
		return (PrimitiveCircleObject)select(V);
	}
	public void selectCircles (int x, int y, boolean multiple)
	{	Enumeration e=C.elements();
		V.removeAllElements();
		boolean haveNotFilled=false;
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o.isSelectable() && (o instanceof PrimitiveCircleObject)
				&& o.nearto(x,y,this)
				&& (multiple || !o.selected()))
				{	V.addElement(o);
					if (!o.isFilledForSelect()) haveNotFilled=true;
				}
		}
		if (haveNotFilled)
		{	e=V.elements();
			while (e.hasMoreElements())
			{	ConstructionObject o=(ConstructionObject)e.nextElement();
				if (o.isFilledForSelect()) V.removeElement(o);
			}
		}
	}
	public PrimitiveCircleObject selectCircle (int x, int y)
	{	return selectCircle(x,y,true);
	}

	/**
	Select a point near x,y.
	**/
	public PointObject selectPoint (int x, int y, boolean multiple)
	{	selectPointObjects(x,y,multiple);
		return (PointObject)select(V);
	}
	public void selectPointObjects (int x, int y, boolean multiple)
	{	Enumeration e=C.elements();
		V.removeAllElements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o.isSelectable() && o instanceof PointObject && o.nearto(x,y,this)
				&& (multiple || !o.selected()))
				V.addElement(o);
		}
	}
	public PointObject selectPoint (int x, int y)
	{	return selectPoint(x,y,true);
	}
	public PointObject selectPoint (int x, int y, boolean multiple, 
		ConstructionObject until)
	{	selectPointObjects(x,y,multiple,until);
		return (PointObject)select(V);
	}
	public void selectPointObjects (int x, int y, boolean multiple, 
		ConstructionObject until)
	{	Enumeration e=C.elements();
		V.removeAllElements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o==until) break;
			if (o.isSelectable() && o instanceof PointObject && o.nearto(x,y,this)
				&& (multiple || !o.selected()))
				V.addElement(o);
		}
	}
	public PointObject selectPoint (int x, int y, ConstructionObject until)
	{	return selectPoint(x,y,true,until);
	}

	boolean NewPoint=false;
	public boolean isNewPoint () { return NewPoint; }

	/**
	* Select a point, and create a new point, if necessary. Even creates
	* an intersection, or a point bound to an object, if possible. If
	* enabled, the user is asked for confirmation in these cases. The
	* variable NewPoint is set to true, if the point was indeed created.
	* @param multiple determines, if multiple selections are possible.
	* @param any determines, if the first point should be used.
	**/
	public PointObject selectCreatePoint (int x, int y, boolean multiple,
		boolean any)
	{	NewPoint=false;
		if (Preview)
		{	PointObject p=new PointObject(C,x(x),y(y));
			addObject(p);
			p.setSuperHidden(true);
			PreviewObject=p;
			return p;
		}
		
		// User selects a known point:
		Enumeration e=C.elements();
		V.removeAllElements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o.isSelectable() && o instanceof PointObject && o.nearto(x,y,this)
				&& (multiple || !o.selected()))
					V.addElement(o);
			sort(V);
		}
		if (V.size()>0)
		{	if (any) return (PointObject)(V.elementAt(0));
			ConstructionObject o=select(V,Control 
				|| !Global.getParameter("options.indicate",true));
			if (o!=null) return (PointObject)o;
			return null;
		}
		
		// User creates a new point:
		ConstructionObject oc=tryCreateIntersection(x,y,true);
		if (oc!=null) return (PointObject)oc;
		if (!IntersectionYes) return null;
		MyVector w=selectPointonObjects(x,y,true,false);
		filter(w,x,y,true);
		if (w.size()>0)
		{	oc=select(w,!Control); 
			if (oc==null) return null;
			if (oc!=null)
			{	boolean yes=true;
				if (Global.getParameter("options.pointon",false))
				{	AgainQuestion q=new AgainQuestion(F,
						Zirkel.name("question.pointon"),
						Zirkel.name("question.title"));
					q.center(F);
					q.setVisible(true);
					yes=q.yes();
					Global.setParameter("options.pointon",q.again());
				}
				if (yes)
				{	PointObject o=new PointObject(C,x(x),y(y),oc);
					if (ShowGrid  && Global.getParameter("grid.leftsnap",true)){o.snap(this);}
					o.setUseAlpha(true);
					addObject(o);
					validate();
					o.setDefaults();
					NewPoint=true;
					return o;
				}
				else return null;
			}
		}
		PointObject p=new PointObject(C,x(x),y(y));
		if (ShowGrid && Global.getParameter("grid.leftsnap",true)){p.snap(this);}
		addObject(p);
		p.setDefaults();
		NewPoint=true;
		return p;
	}
	
	boolean IntersectionYes=false;

	public ConstructionObject tryCreateIntersection (int x, int y, boolean ask)
	{	MyVector w=selectPointonObjects(x,y,true,true);
		sort(w);
		IntersectionYes=true;
		if (w.size()<2) return null;
		ConstructionObject P1=(ConstructionObject)w.elementAt(0);
		ConstructionObject P2=(ConstructionObject)w.elementAt(1);
		if (!(P1 instanceof PointonObject && P2 instanceof PointonObject)) return null;
		if (!((PointonObject)P1).canInteresectWith(P2) 
			|| !((PointonObject)P2).canInteresectWith(P1)) return null;
		IntersectionObject o[]=IntersectionConstructor.construct(P1,P2,C);
		if (o.length==1 && !o[0].valid()) return null;
		if (o.length==2 && !o[0].valid() && !o[1].valid()) return null;
		if (ask && Global.getParameter("options.intersection",false))
		{	AgainQuestion q=new AgainQuestion(F,
				Zirkel.name("question.intersection"),
				Zirkel.name("question.title"));
			q.center(F);
			q.setVisible(true);
			IntersectionYes=q.yes();
			Global.setParameter("options.intersection",q.again());
		}
		if (IntersectionYes)
		{	if (o.length==1 || !o[1].valid())
			{	addObject(o[0]);
				o[0].autoAway();
				o[0].validate(x(x),y(y));
				validate();
				o[0].setDefaults();
				o[0].setRestricted(getRestricted());
				return o[0];
			}
			if (!o[0].valid())
			{	addObject(o[1]);
				o[1].autoAway();
				validate();
				o[1].setDefaults();
				o[1].setRestricted(getRestricted());
				return o[1];
			}			
			IntersectionObject oc=o[0];
			double d0=o[0].distanceTo(x,y,this),
				d1=o[1].distanceTo(x,y,this);
			if (d1<d0) oc=o[1];
			addObject(oc);
			oc.autoAway();
			oc.validate(x(x),y(y));
			validate();
			oc.setDefaults();
			oc.setRestricted(getRestricted());
			NewPoint=true;
			return oc;
		}
		else return null;
	}

	public PointObject selectCreatePoint (int x, int y)
	{	return selectCreatePoint(x,y,true,false);
	}

	public PointObject selectCreatePoint (int x, int y, boolean multiple)
	{	return selectCreatePoint(x,y,multiple,false);
	}

	/**
	Select a moveable point at x,y. Ask user, if necessary.
	**/	
	public ConstructionObject selectMoveablePoint (int x, int y)
	{	ConstructionObject s=findSelectedObject();
		if (s instanceof PointObject && 
				((MoveableObject)s).moveable() && s.nearto(x,y,this))
			return s;
		V.removeAllElements();
		Enumeration e=C.elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o.isSelectable() && (o instanceof PointObject) &&
				((MoveableObject)o).moveable() &&
				o.nearto(x,y,this))
			{	V.addElement(o);
			}
		}
		if (V.size()==1) return (ConstructionObject)V.elementAt(0);
		ConstructionObject o=select(V);
		if (o!=null) o.setSelected(true);
		return null;
	}

	/**
	Select a point with a selector, provided by the calling constructor.
	**/	
	public ConstructionObject selectWithSelector (int x, int y, 
		Selector sel, ConstructionObject until, boolean choice)
	{	V.removeAllElements();
		Enumeration e=C.elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o==until) break;
			if (o.isSelectable() && sel.isAdmissible(this,o) &&
				o.nearto(x,y,this))
			{	V.addElement(o);
			}
		}
		if (V.size()==1) return (ConstructionObject)V.elementAt(0);
		if (!choice)
		{	if (V.size()>0) return (ConstructionObject)V.elementAt(0);
			else return null;
		}
		ConstructionObject o=select(V,x,y);
		return o;
	}
	public ConstructionObject selectWithSelector (int x, int y, 
		Selector sel, ConstructionObject until)
	{	return selectWithSelector(x,y,sel,until,true);
	}
	public ConstructionObject selectWithSelector (int x, int y, 
		Selector sel, boolean choice)
	{	return selectWithSelector(x,y,sel,null,choice);
	}
	public ConstructionObject selectWithSelector (int x, int y, Selector sel)
	{	return selectWithSelector(x,y,sel,null,true);
	}


	/**
	Select a moveable point at x,y. Don't ask user, take first.
	**/	
	public ConstructionObject selectImmediateMoveablePoint (int x, int y)
	{	Enumeration e=C.elements();
		V.removeAllElements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o.isSelectable() &&
				o instanceof PointObject && ((MoveableObject)o).moveable() &&
				o.nearto(x,y,this))
					V.addElement(o);
		}
		if (V.size()==0) return null;
		return (ConstructionObject)V.elementAt(0);
	}

	/**
	Select a moveable object at x,y.
	**/	
	public ConstructionObject selectMoveableObject (int x, int y)
	{	ConstructionObject s=findSelectedObject();
		if (s instanceof MoveableObject && 
				((MoveableObject)s).moveable() && s.nearto(x,y,this))
			return s;
		selectMoveableObjects(x,y);
		if (V.size()==1) return (ConstructionObject)V.elementAt(0);
		ConstructionObject o=select(V);
		if (o!=null)
		{	if (!Global.getParameter("options.choice",true)) return o;
			o.setSelected(true);
		}
		return null;
	}
	public void selectMoveableObjects (int x, int y, boolean control)
	{	V.removeAllElements();
		ConstructionObject s=findSelectedObject();
		if (s instanceof MoveableObject && 
				((MoveableObject)s).moveable() && s.nearto(x,y,this))
		{	V.addElement(s);
			return;
		}
		else if (control && s instanceof FixedCircleObject && s.nearto(x,y,this) &&
			((FixedCircleObject)s).fixedByNumber())
		{	V.addElement(s);
			return;
		}
		else if (control && s instanceof FixedAngleObject && s.nearto(x,y,this) &&
			((FixedAngleObject)s).fixedByNumber())
		{	V.addElement(s);
			return;
		}
		Enumeration e=C.elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (!control && o.isSelectable() && 
				o instanceof MoveableObject && ((MoveableObject)o).moveable() &&
				o.nearto(x,y,this))
					V.addElement(o);
			else if (control && o instanceof FixedCircleObject && o.nearto(x,y,this) &&
				((FixedCircleObject)o).fixedByNumber())
					V.addElement(o);
			else if (control && o instanceof FixedAngleObject && o.nearto(x,y,this) &&
				((FixedAngleObject)o).fixedByNumber())
					V.addElement(o);
		}
		filter(V,x,y);
	}
	public void selectMoveableObjects (int x, int y)
	{	selectMoveableObjects(x,y,false);
	}

	/**
	Select a moveable object at x,y. Don't ask user, take first.
	**/	
	public ConstructionObject selectImmediateMoveableObject (int x, int y)
	{	Enumeration e=C.elements();
		V.removeAllElements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o.isSelectable() &&
				o instanceof MoveableObject && ((MoveableObject)o).moveable() &&
				o.nearto(x,y,this))
					V.addElement(o);
		}
		filter(V,x,y);
		if (V.size()==0) return null;
		return (ConstructionObject)V.elementAt(0);
	}

	/**
	try to determine the unique objects that are near coordinates x,y
	and delete all others from the vector v.
	**/
	public void filter (MyVector v, int x, int y, boolean choice)
	{	boolean HasPoints=false,HasNotFilled=false;
		Enumeration e=v.elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o instanceof PointObject)
			{	HasPoints=true;
			}
			if (!o.isFilledForSelect())
			{	HasNotFilled=true;
			}
		}
		if (HasPoints)
		{	e=v.elements();
			while (e.hasMoreElements())
			{	ConstructionObject o=(ConstructionObject)e.nextElement();
				if (!o.onlynearto(x,y,this)) v.removeElement(o);
			}
		}
		else if (HasNotFilled)
		{	e=v.elements();
			while (e.hasMoreElements())
			{	ConstructionObject o=(ConstructionObject)e.nextElement();
				if (o.isFilledForSelect()) v.removeElement(o);
			}
		}
		sort(v);
		if (!choice)
		{	v.truncate(1);
		}
	}
	public void filter (MyVector v, int x, int y)
	{	filter(v,x,y,Global.getParameter("options.choice",true) || Control);
	}

	/**
	user must select an object in the selection dialog, unless the
	selection is unique anyway, or the filter determines that the
	selection is unique.
	**/
	public ConstructionObject select (MyVector v, int x, int y, boolean choice)
	{	if (v.size()==0) return null;
		if (v.size()==1) return (ConstructionObject)v.elementAt(0);
		filter(v,x,y);
		if (v.size()==1) return (ConstructionObject)v.elementAt(0);
		if (!choice) return (ConstructionObject)v.elementAt(0);
		sortRow(V);
		RightClicked=false;
		SelectDialog d=new SelectDialog(F,v);
		if (d.isAborted()) return null;
		return d.getObject();
	}
	public ConstructionObject select (MyVector v, int x, int y)
	{	return select(v,x,y,Global.getParameter("options.choice",true) || Control);
	}

	/**
	user must select an object in the selection dialog, unless the
	selection is unique anyway.
	**/
	public ConstructionObject select (MyVector v, boolean choice)
	{	if (v.size()==0) return null;
		if (v.size()==1) return (ConstructionObject)v.elementAt(0);
		if (!choice) return (ConstructionObject)v.elementAt(0);
		sortRow(V);
		RightClicked=false;
		SelectDialog d=new SelectDialog(F,v);
		if (d.isAborted()) return null;
		return d.getObject();
	}
	public ConstructionObject select (MyVector v)
	{	return select(v,Global.getParameter("options.choice",true) || Control);
	}

	/**
	select the label of a point, i.e. a point, which is set by the
 	user 
 	@return the first label found
	**/
	public ConstructionObject selectLabel (int x, int y)
	{	Enumeration e=C.elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o.isSelectable() && o.textcontains(x,y,this)) return o;
		}
		return null;
	}

	public ConstructionObject findSelectedObject ()
	{	Enumeration e=C.elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o.selected()) return o;
		}
		return null;
	}
	
	// Indication functions

	MyVector Indicated=new MyVector();
	PointObject IndicatePoint=null;
	
	public void indicate (MyVector v, boolean showname)
	{	if (v.size()==1)
		{	if (showname)
				setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
			else if (v.elementAt(0) instanceof PointObject)
				setCursor(new Cursor(Cursor.HAND_CURSOR));				
		}
		else
			setCursor(Cursor.getDefaultCursor());
		if (Indicated.equalsIdentical(v)) return;
		Enumeration e=Indicated.elements();
		while (e.hasMoreElements())
			((ConstructionObject)e.nextElement()).setIndicated(false);
		Indicated.removeAllElements();
		e=v.elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			Indicated.addElement(o);
			o.setIndicated(true,showname);
		}
		repaint();
	}
	
	public void indicate (MyVector v)
	{	indicate(v,false);
	}
	
	public void clearIndicated ()
	{	IndicatePoint=null;
		if (Indicated.size()==0) return;
		Enumeration e=Indicated.elements();
		while (e.hasMoreElements())
			((ConstructionObject)e.nextElement()).setIndicated(false);
		Indicated.removeAllElements();
		setCursor(Cursor.getDefaultCursor());
		repaint();
	}

	boolean Preview=false;
	ConstructionObject LastNonPreview=null;
	MoveableObject PreviewObject=null;
	
	public void movePreview (MouseEvent e)
	{	if (PreviewObject!=null)
		{	PreviewObject.move(x(e.getX()),y(e.getY()));
			validate();
			repaint();
		}
	}

	public void prepareForPreview (MouseEvent e)
	{	LastNonPreview=C.last();
		Preview=true;
	}
	
	public boolean isPreview ()
	{	return Preview;
	}
	
	public void clearPreview ()
	{	if (!Preview) return;
		C.clearAfter(LastNonPreview);
		LastNonPreview=null;
		PreviewObject=null;
		Preview=false;
		Count.fixAll(false);
	}

	public void setPreviewObject (MoveableObject o)
	{	PreviewObject=o;
	}

	public ConstructionObject indicateTryCreateIntersection (int x, int y, boolean ask)
	{	MyVector w=selectPointonObjects(x,y,true,true);
		sort(w);
		IntersectionYes=true;
		if (w.size()<2) return null;
		IntersectionObject o[]=IntersectionConstructor.construct(
			(ConstructionObject)w.elementAt(0),
			(ConstructionObject)w.elementAt(1),C);
		if (o.length==1 && !o[0].valid()) return null;
		if (o.length==2)
		{	if (!o[1].valid())
			{	if (!o[0].valid()) return null;
			}
			else
			{	IntersectionObject h=o[0];
				o[0]=o[1]; o[1]=h;
			}
		}
		IntersectionObject oc=o[0];
		if (o.length==2 && o[1].valid())
		{	double d0=o[0].distanceTo(x,y,this),
				d1=o[1].distanceTo(x,y,this);
			if (d1<d0) oc=o[1];
		}
		oc.setDefaults();
		oc.setIndicated(true);
		oc.setType(PointObject.CIRCLE);
		oc.setColorType(ConstructionObject.THICK);
		IndicatePoint=oc;
		indicate(w);
		oc.validate(x(x),y(y));
		return oc;
	}

	public void indicateCreatePoint (int x, int y, boolean multiple)
	{	Enumeration e=C.elements();
		V.removeAllElements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o.isSelectable() && o instanceof PointObject && o.nearto(x,y,this)
				&& (multiple || !o.selected()))
			{	V.addElement(o);
			}
			sort(V);
		}
		if (V.size()>0)
		{	IndicatePoint=null;
			filter(V,x,y,false);
			indicate(V);
			return;
		}
		PointObject oc=(PointObject)indicateTryCreateIntersection(x,y,true);
		if (oc!=null) return;
		MyVector w=selectPointonObjects(x,y,true,false);
		filter(w,x,y,true);
		if (w.size()>=1)
		{	if (!w.equalsIdentical(Indicated))
			{	oc=new PointObject(C,x(x),y(y),
					(ConstructionObject)w.elementAt(0));
				if (ShowGrid && !Global.getParameter("grid.leftsnap",false)) 
					oc.snap(this);
				oc.setUseAlpha(true);
				oc.validate();
				oc.setIndicated(true);
				oc.setType(PointObject.CIRCLE);
				oc.setColorType(ConstructionObject.THICK);
				IndicatePoint=oc;
				indicate(w);
			}
			else if (IndicatePoint!=null)
			{	IndicatePoint.setType(PointObject.CIRCLE);
				IndicatePoint.setColorType(ConstructionObject.THICK);
				IndicatePoint.move(x(x),y(y));
				IndicatePoint.project((ConstructionObject)w.elementAt(0));
				repaint();
			}
		}
		else
		{	clearIndicated();
		}
	}

	public void indicateCircleLineObjects (int x, int y)
	{	MyVector w=selectCircleLineObjects(x,y);
		filter(V,x,y);
		indicate(w);
	}

	public void indicatePointonObjects (int x, int y)
	{	MyVector w=selectPointonObjects(x,y);
		filter(V,x,y);
		indicate(w);
	}

	public void indicateIntersectedObjects (int x, int y)
	{	MyVector w=selectPointonObjects(x,y);
		if (!w.equalsIdentical(Indicated) && w.size()>=2)
		{	IntersectionObject o[]=IntersectionConstructor.construct(
				(ConstructionObject)w.elementAt(0),
				(ConstructionObject)w.elementAt(1),C);
			IntersectionObject oc=o[0];
			if (o.length==2)
			{	double d0=o[0].distanceTo(x,y,this),
					d1=o[1].distanceTo(x,y,this);
				if (d1<d0) oc=o[1];
				oc.autoAway();
			}
			oc.validate();
			oc.setDefaults();
			oc.setIndicated(true);
			oc.setColorType(ConstructionObject.THICK);
			oc.setType(PointObject.CIRCLE);
			IndicatePoint=oc;
		}
		else
		{	IndicatePoint=null;
		}
		indicate(w);
	}

	public void indicateLineObjects (int x, int y)
	{	selectLineObjects(x,y,true);
		filter(V,x,y);
		indicate(V);
	}
	
	public void indicatePointObjects (int x, int y)
	{	selectPointObjects(x,y,true);
		filter(V,x,y);
		indicate(V);
	}

	public void indicatePointObjects (int x, int y, ConstructionObject until)
	{	selectPointObjects(x,y,true,until);
		filter(V,x,y);
		indicate(V);
	}

	public void indicatePointsOrLines (int x, int y)
	{	selectPointsOrLines(x,y,true);
		filter(V,x,y);
		indicate(V);
	}
	
	public void indicateSegments (int x, int y)
	{	selectSegments(x,y,true);
		filter(V,x,y);
		indicate(V);
	}

	public void indicateRays (int x, int y)
	{	selectRays(x,y,true);
		filter(V,x,y);
		indicate(V);
	}

	public void indicateTwoPointLines (int x, int y)
	{	selectTwoPointLines(x,y,true);
		filter(V,x,y);
		indicate(V);
	}

	public void indicateCircles (int x, int y)
	{	selectCircles(x,y,true);
		filter(V,x,y);
		indicate(V);
	}

	public void indicateMoveableObjects (int x, int y, boolean control)
	{	selectMoveableObjects(x,y,control);
		filter(V,x,y);
		indicate(V);
	}
	public void indicateMoveableObjects (int x, int y)
	{	indicateMoveableObjects(x,y,false);
	}
	
	public void indicateWithSelector (int x, int y, Selector sel)
	{	selectWithSelector(x,y,sel,false);
		filter(V,x,y);
		indicate(V);
	}

	public void indicateWithSelector (int x, int y, 
		Selector sel, ConstructionObject until)
	{	selectWithSelector(x,y,sel,until,false);
		filter(V,x,y);
		indicate(V);
	}

	public void indicateConstructableObjects (int x, int y)
	{	selectConstructableObjects(x,y);
		filter(V,x,y);
		indicate(V);
	}

	public void indicateObjects (int x, int y, boolean showname)
	{	selectObjects(x,y);
		filter(V,x,y);
		indicate(V,showname);
	}

	public void indicateObjects (int x, int y)
	{	indicateObjects(x,y,false);
	}
	
	public void setTool (ObjectConstructor oc)
	{	if (OC!=null) OC.invalidate(this);
		OC=oc;
		OC.showStatus(this);
		clearIndicated();
		clearPreview();
	}
	
	public void setSuddenTool (ObjectConstructor oc)
	// called from ZirkelFrame
	{	OC=oc;
	}

	public void reset ()
	{	clearPreview();
		clearIndicated();
		OC.reset(this);
	}
	
	public void clearSelected ()
	// called from ObjectConstructor
	{	Enumeration e=C.elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			o.setSelected(false);
		}
		repaint();
	}

	public synchronized void clear ()
	// delete complete construction
	{	displayJob(false);
		if (OC!=null) OC.invalidate(this);
		C.clear(); recompute();
		setDefaultColor(0);
		setDefaultColorType(0);
		setDefaultType(2);
		setGrid();
		reloadCD();
	}

	/**
	 * Delete last construction step done by user (highest number) 
	 * and all non-visible steps before it.
	 */
	public synchronized void back ()
	{	reset();
		ConstructionObject O=C.lastByNumber();
		if (O==null) return;
		if (O.isKeep()) return;
		delete(O);
		while (true)
		{	O=C.lastByNumber();
			if (O==null) break;
			else if (!O.mustHide(this) || O.isHideBreak() || O.isKeep()) break;
			delete(O);
		}
		validate();
	}
	
	public synchronized void undo ()
	{	reset();
		C.undo();
	}
	
	public void delete (ConstructionObject o)
	{	if (C.last()==null) return;
		if (o.isKeep()) return;
		C.clearConstructables();
		o.setFlag(true);
		C.determineChildren();
		C.delete(false);
	}
	
	/**
	 * Delete a vector of construction objects.
	 * @param v
	 */
	public void delete (Vector v)
	{	if (C.last()==null) return;
		C.clearConstructables();
		Enumeration e=v.elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o.isKeep()) return;
			o.setFlag(true);
		}
		C.determineChildren();
		C.delete(false);
	}
	
	public boolean depends (ConstructionObject o, ConstructionObject on)
	{	C.clearConstructables();
		on.setFlag(true);
		C.determineChildren();
		return o.isFlag();
	}

	public void addStatusListener (StatusListener sl)
	{	SL=sl;
	}

	public void showStatus (String s)
	{	if (SL!=null) SL.showStatus(s);
	}

	public void showStatus ()
	{	if (OC!=null) OC.showStatus(this);
	}

	public boolean showHidden ()
	{	return ShowHidden;
	}

	public void setDefaultColor (int c)
	{	C.DefaultColor=c;
	}
	public int getDefaultColor ()
	{	return C.DefaultColor;
	}

	public void setDefaultType (int c)
	{	C.DefaultType=c;
	}
	public int getDefaultType ()
	{	return C.DefaultType;
	}
	public void setPartial (boolean flag)
	{	C.Partial=flag;
	}
	public boolean getPartial ()
	{	return C.Partial;
	}
	public void setRestricted (boolean flag)
	{	C.Restricted=flag;
	}
	public boolean getRestricted ()
	{	return C.Restricted;
	}
	public void setPartialLines (boolean flag)
	{	C.PartialLines=flag;
	}
	public boolean getPartialLines ()
	{	return C.PartialLines;
	}
	public void setVectors (boolean flag)
	{	C.Vectors=flag;
	}
	public boolean getVectors ()
	{	return C.Vectors;
	}

	public void setLongNames (boolean flag)
	{	C.LongNames=flag;
	}
	public boolean getLongNames ()
	{	return C.LongNames;
	}

	public void setLargeFont (boolean flag)
	{	C.LargeFont=flag;
	}
	public boolean getLargeFont ()
	{	return C.LargeFont;
	}

	public void setBoldFont (boolean flag)
	{	C.BoldFont=flag;
	}
	public boolean getBoldFont ()
	{	return C.BoldFont;
	}

	public void setObtuse (boolean flag)
	{	C.Obtuse=flag;
	}
	public boolean getObtuse ()
	{	return C.Obtuse;
	}

	public void setSolid (boolean flag)
	{	C.Solid=flag;
	}
	public boolean getSolid ()
	{	return C.Solid;
	}

	public void setShowNames (boolean flag)
	{	C.ShowNames=flag;
	}
	public boolean getShowNames ()
	{	return C.ShowNames;
	}

	public void setShowValues (boolean flag)
	{	C.ShowValues=flag;
	}
	public boolean getShowValues ()
	{	return C.ShowValues;
	}

	public void setDefaultColorType (int c)
	{	C.DefaultColorType=c;
	}
	public int getDefaultColorType ()
	{	return C.DefaultColorType;
	}

	public void setShowHidden (boolean flag)
	{	ShowHidden=flag; repaint();
	}
	public boolean getShowHidden ()
	{	return ShowHidden;
	}

	public void setHidden (boolean flag)
	{	C.Hidden=flag;
	}

	/**
	With this it is possible to hide all non-constructable items.
	This function is called from any object to see if it has
	to hide.
	*/
	public boolean hides (ConstructionObject o)
	{	if (OC instanceof SetTargetsTool) return !o.isFlag();
		else return false;
	}
	/**
	Save the construction in this canvas in XML form to the
	specified output stream. This function will create the
	complete XML file, including headers.
	*/
	public void save (OutputStream o, boolean construction, boolean macros, boolean protectedmacros, Vector Macros, String Restrict)
		throws IOException
	{	boolean utf=Global.getParameter("options.utf",true);
		XmlWriter xml;
		if (utf)
		{	xml=new XmlWriter(new PrintWriter(
				new OutputStreamWriter(o,"UTF8"),true));
			xml.printEncoding(utf?"utf-8":"iso-8859-1");
		}
		else
		{	xml=new XmlWriter(new PrintWriter(
				new OutputStreamWriter(o),true));
			xml.printXml();
		}
		//xml.printXls("zirkel.xsl");
		//xml.printDoctype("CaR","zirkel.dtd");
		xml.startTagNewLine("CaR");
		if (macros)
		{	Sorter.sort(Macros);
			Enumeration e=Macros.elements();
			while (e.hasMoreElements())
			{	Macro m=((MacroItem)e.nextElement()).M;
				if (protectedmacros || !m.isProtected())
					m.saveMacro(xml);
			}
		}
		if (construction)
		{	xml.startTagStart("Construction");
			if (isJob())
			{	xml.printArg("job","true");
				xml.printArg("last",Last);
				int i=1;
				Enumeration e=Targets.elements();
				while (e.hasMoreElements())
				{	xml.printArg("target"+i,(String)e.nextElement());
					i++;
				}
			}
			xml.startTagEndNewLine();
			xml.startTagStart("Window");
			xml.printArg("x",""+C.getX());
			xml.printArg("y",""+C.getY());
			xml.printArg("w",""+C.getW());
			if (ShowGrid) xml.printArg("showgrid","true");
			xml.finishTagNewLine();
			if (ShowGrid)
			{	xml.startTagStart("Grid");
				if (AxesOnly) xml.printArg("axesonly","true");
				xml.printArg("color",""+GridColor);
				xml.printArg("thickness",""+GridThickness);
				if (!GridLabels) xml.printArg("nolabels","true");
				else
				{	if (GridLarge) xml.printArg("large","true");
					if (GridBold) xml.printArg("bold","true");
				}
				xml.finishTagNewLine();
			}
			if (getConstruction().BackgroundFile!=null &&
					!getConstruction().BackgroundFile.equals(""))
			{	xml.startTagStart("Background");
				xml.printArg("file",getConstruction().BackgroundFile);
				if (getConstruction().ResizeBackground)
					xml.printArg("resize","true");
				xml.finishTagNewLine();
			}
			if (!C.getComment().equals(""))
			{	xml.startTagNewLine("Comment");
				xml.printParagraphs(C.getComment(),60);
				xml.endTagNewLine("Comment");
			}
			if (!C.getJobComment().equals(""))
			{	xml.startTagNewLine("Assignment");
				xml.printParagraphs(C.getJobComment(),60);
				xml.endTagNewLine("Assignment");
			}
			if (!Restrict.equals(""))
			{	xml.finishTag("Restrict","icons",Restrict);
			}
			if (OC instanceof ObjectTracker)
			{	((ObjectTracker)OC).save(xml);
			}
			else if (OC instanceof Tracker)
			{	((Tracker)OC).save(xml);
			}
			else if (OC instanceof AnimatorTool)
			{	((AnimatorTool)OC).save(xml);
			}
			else if (OC instanceof BreakpointAnimator)
			{	((BreakpointAnimator)OC).save(xml);
			}
			saveDrawings(xml);
			xml.startTagNewLine("Objects");
			C.save(xml);
			xml.endTagNewLine("Objects");
			xml.endTagNewLine("Construction");
		}
		xml.endTagNewLine("CaR");
	}

	public void load (InputStream in, boolean construction, boolean macros)
		throws Exception
	{	// System.out.println("read file");
		try
		{	if (construction)
			{	C.clear(); All=false;
				paint(getGraphics());
			}
			else All=true;
			XmlReader xml=new XmlReader();
			xml.init(in);
			XmlTree tree=xml.scan();
			if (tree==null)
				throw new ConstructionException("XML file not recognized");
			Enumeration e=tree.getContent();
			while (e.hasMoreElements())
			{	tree=(XmlTree)e.nextElement();
				if (tree.getTag() instanceof XmlTagPI) continue;
				if (!tree.getTag().name().equals("CaR"))
					throw new ConstructionException("CaR tag not found");
				else break;
			}
			e=tree.getContent();
			boolean all=false;
			while (e.hasMoreElements())
			{	tree=(XmlTree)e.nextElement();
				XmlTag tag=tree.getTag();
				if (tag.name().equals("Macro"))
				{	if (macros)
					{	try
						{	Count.setAllAlternate(true);
							Macro m=new Macro(this,tree);
							int i=0;
							for (i=0; i<Macros.size(); i++)
							{	if (((MacroItem)Macros.elementAt(i)).M.getName()
									.equals(m.getName()))
								{	all=replaceMacro(m,i,all);
									break;
								}
							}
							if (i>=Macros.size()) appendMacro(m);
						}
						catch (ConstructionException ex)
						{	Count.setAllAlternate(false);
							throw ex;	
						}
						Count.setAllAlternate(false);
					}
				}
				else if (tag.name().equals("Construction"))
				{	if (construction)
					{	boolean job=false;
						if (tag.hasParam("job"))
						{	job=true;
							Last=tag.getValue("last");
							if (Last==null)
								throw new ConstructionException(
									Zirkel.name("exception.job"));
							String Target=tag.getValue("target");
							if (Target==null)
							{	Targets=new Vector();
								int i=1;
								while (true)
								{	String s=tag.getValue("target"+i);
									i++;
									if (s==null) break;
									Targets.addElement(s);
								}
							}
							else
							{	Targets=new Vector();
								Targets.addElement(Target);
							}
							if (Targets.size()==0)
								throw new ConstructionException(
									Zirkel.name("exception.job"));
						}
						C.load(tree,this);
						if (job)
						{	if (C.find(Last)==null)
								throw new ConstructionException(
									Zirkel.name("exception.job"));
							Enumeration et=Targets.elements();
							while (et.hasMoreElements())
							{	String s=(String)et.nextElement();
								if (C.find(s)==null
									&& 
									(!s.startsWith("~") ||
									C.find(s.substring(1))==null))
									throw new ConstructionException(
										Zirkel.name("exception.job"));					
							}
							Job=true;
						}
						break;
					}
				}			
				else
					throw new ConstructionException(
						"Construction not found");
			}
			recompute();
			C.translateOffsets(this);
			resetSum();
			validate();
			repaint();
		}
		catch (Exception e)
		{	throw e;
		}
		reloadCD();
		repaint();
		// System.out.println("finished reading file");
	}
	
	public void resetSum ()
	{	Enumeration e=C.elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o instanceof ExpressionObject)
				((ExpressionObject)o).reset();
		}
	}
	
	public void warning (String s, String help)
	{	Warning w=new Warning(F,s,
				Zirkel.name("warning"),true,help);
		w.center(F);
		w.setVisible(true);
	}
	
	public void warning (String s)
	{	warning(s,"");
	}
	
	public void load (InputStream in)
		throws Exception
	{	try
		{	C.Loading=true;
			load(in,true,true);
			C.Loading=false;
		}
		catch (Exception e)
		{	C.Loading=false;
			throw e;
		}
	}

	public String getComment ()
	{	return C.getComment();
	}
	public void setComment (String s)
	{	C.setComment(s);
	}

	public String getJobComment ()
	{	return C.getJobComment();
	}
	public void setJobComment (String s)
	{	C.setJobComment(s);
	}

	/**
	This can be used to set a frame window for the error dialogs that
	the canvas my display.
	*/
	public void setFrame (Frame f)
	{	F=f;
	}
	public Frame getFrame ()
	{	return F;
	}

	/**
	Maginify the view by the specified factor.
	*/
	public void magnify (double f)
	{	C.setXYW(C.getX(),C.getY(),C.getW()*f);
		recompute(); validate(); repaint();
	}

	/**
	Shift the view with these deltas.
	*/
	public void shift (double dx, double dy)
	{	C.setXYW(C.getX()+dx*C.getW(),C.getY()+dy*C.getW(),C.getW());
		recompute(); validate(); repaint();
	}

	/**
	Tracker routines:
	Call the OC (must be a TrackPainter) to paint the object track.
	*/
	public void paintTrack (MyGraphics g)
	{	if (!(OC instanceof TrackPainter)) return;
		((TrackPainter)OC).paint(g,this);
	}

	/**
	Run through the construction to update all object texts. This
	should be called, whenever the name of an item was changed. It
	will recreate only those texts, which contain the old name.
	*/
	public void updateTexts (ConstructionObject o, String oldname)
	{	C.updateTexts(o,oldname);
	}

	public Construction getConstruction ()
	{	return C;
	}
	
	public void setShowGrid (boolean flag)
	{	ShowGrid=flag;
		repaint(); 
	}
	public void toggleShowGrid ()
	{	if (ShowGrid && !AxesOnly)
		{	AxesOnly=true;
		}
		else if (ShowGrid)
		{	ShowGrid=false;
		}
		else
		{	AxesOnly=false;
			ShowGrid=true;
		}
		Global.setParameter("showgrid",ShowGrid);
		Global.setParameter("axesonly",AxesOnly);
		repaint(); 
	}
	public boolean showGrid () { return ShowGrid; }

	/**
	Sets the job parameters for the current construction (the last
	item to display and the target item).
	*/
	public void setJob (ConstructionObject last)
	{	Job=true;
		Last=last.getName(); Targets=new Vector();
	}
	public void addJobTarget (ConstructionObject target, boolean visible)
	{	if (visible) Targets.addElement("~"+target.getName());
		else Targets.addElement(target.getName());
	}
	public void clearTargets ()
	{	Targets=new Vector();
	}
	/**
	Cancel the job state o fthe current construction.
	*/
	public void clearJob ()
	{	Job=false;
	}
	/**
	Test, if the construction is a valid job.
	*/
	public boolean isJob ()
	{	Enumeration e=Targets.elements();
		while (e.hasMoreElements())
		{	String s=(String)e.nextElement();
			if (C.find(s)==null &&
				(!s.startsWith("~") || C.find(s.substring(1))==null)) return false;
		}
		return Job && C.find(Last)!=null && Targets.size()>0;
	}
	
	public boolean inTargets (String s)
	{	Enumeration e=Targets.elements();
		while (e.hasMoreElements())
		{	if (((String)e.nextElement()).equals(s))
				return true;
		}
		return false;
	}
	
	/**
	* 
	* This will display the construction as a job (if it is a job). 
	* The construction is copied up to the last element. The copies are 
	* marked as keep objects, so that they cannot be changed. The last 
	* object is marked as the target object, so that it displays in a 
	* special color.
	* 
	*  The function can also flip back to the original construction (flag=false). 
	*/
	public void displayJob (boolean flag)
	{	clearSelected();
		if (!(OC instanceof SaveJob)) OC.reset(this);
		if (flag) // diplay the job
		{	C.setOriginalOrder(true);
			if (displayJob() || !isJob()) return;
			Construction Cnew=new Construction();
			Cnew.setXYW(C.getX(),C.getY(),C.getW());
			Enumeration e=C.elements();
			while (e.hasMoreElements())
			{	ConstructionObject o=(ConstructionObject)e.nextElement();
				o.setJobTarget(true);
				o.setJobTarget(false);
			}
			e=C.elements();
			while (e.hasMoreElements())
			{	ConstructionObject o=(ConstructionObject)e.nextElement();
				Cnew.add(o); o.setKeep(true);
				if (inTargets(o.getName()) || inTargets("~"+o.getName())) 
					o.setJobTarget(true);
				if (o.getName().equals(Last)) break;
			}
			while (e.hasMoreElements())
			{	ConstructionObject o=(ConstructionObject)e.nextElement();
				if (inTargets(o.getName()) || inTargets("~"+o.getName()))
				{	Cnew.add(o); o.setKeep(true);
					o.setJobTarget(true);
				}
			}
			int n=0;
			e=Targets.elements();
			while (e.hasMoreElements())
			{	String s=(String)e.nextElement();
				if (!s.startsWith("~")) n++;
			}
			TargetO=new ConstructionObject[n];
			TargetS=new ConstructionObject[n];
			n=0;
			e=Targets.elements();
			while (e.hasMoreElements())
			{	String s=(String)e.nextElement();
				if (!s.startsWith("~"))
				{	TargetO[n]=Cnew.find(s);
					TargetS[n]=null;
					n++;
				}
			}
			COriginal=C;
			C=Cnew;
			C.addAddEventListener(this);
			C.setComment(COriginal.getJobComment());
			recompute();
			reloadCD();
			repaint();
		}
		else // display the orginal construction again
		{	if (!displayJob()) return;
			C.removeAddEventListener(this);
			C=COriginal;
			COriginal=null;
			Enumeration e=C.elements();
			while (e.hasMoreElements())
			{	ConstructionObject o=
					(ConstructionObject)e.nextElement();
				o.setKeep(false);
				o.setJobTarget(false);
			}
			C.setOriginalOrder(false);
			recompute();
			reloadCD();
			repaint();
		}
	}
	
	/**
	 * If the job is solved, this function removes all restrictions.
	 */
	public void freeJob ()
	{	Enumeration e=C.elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			o.setKeep(false);
			o.setJobTarget(false);
			repaint();
		}
	}

	/**
	Test if the canvas displays a job right now.
	*/
	public boolean displayJob ()
	{	return COriginal!=null;
	}
	
	// The following functions change the default values of some objects.
	
	public void setShowColor (int i)
	{	ShowColor=i; 
		repaint();
	}
	public int getShowColor () { return ShowColor; }

	public ObjectConstructor getOC () { return OC; }	
	
	// Macros:

	Vector Macros=new Vector();
	
	public Vector getMacros ()
	{	return Macros;
	}
	
	public boolean haveMacros ()
	{	return Macros.size()>0;
	}

	public boolean haveNonprotectedMacros ()
	{	Enumeration e=Macros.elements();
		while (e.hasMoreElements())
		{	MacroItem m=(MacroItem)e.nextElement();
			if (!m.M.isProtected()) return true;
		}
		return false;
	}

	/**
	Define a macro. There must be parameters (but not necessarily
	targets).
	The function will display the macro dialog. It will create a new
	macro, when the dialog was not aborted. The macro is kept in the
	Macros vector by name.
	*/
	boolean defineMacro ()
	{	clearSelected();
		// Read the list of Parameters and Targets from the
		// construction. This list is filled by the SetParameter and
		// SetTargets constructors.
		Vector P=C.getParameters();
		if (P.size()==0) // no parameters
		{	warning(Zirkel.name("definemacro.noparams"));
			return false;
		}
		// Display the macro creation dialog to the user.
		DefineMacro d=new DefineMacro(F,this);
		d.center(getFrame());
		d.setVisible(true);
		// Aborted?
		if (d.isAborted() || d.getName().equals("")) return false;
		// Define a marco with the name, the comment and the paramters.
		Macro m=new Macro(this,d.getName(),d.getComment(),d.getParams());
		// If there are no targets, determine everything as
		// constructed by the paramaters, else only the things needed
		// to get the targets.
		try
		{	defineMacro(C,m,d.targetsOnly(),d.invisible(),d.getPromptFor(),
				d.hideduplicates());
		}
		catch (ConstructionException e)
		{	warning(e.getDescription());
			OC.reset(this);
			return false;
		}
		storeMacro(m,false);
		OC.reset(this);
		return true;
	}
	
	
	/**
	Copy a macro with fixed parameters from another macro.
	*/
	public Macro copyMacro (Macro m, String name, boolean fixed[])
	{	try
		{	Macro macro=(Macro)(m.clone());
			macro.Name=name;
			boolean f[]=new boolean[fixed.length];
			for (int i=0; i<f.length; i++) f[i]=fixed[i];
			macro.Fixed=f;
			storeMacro(macro,true);
			return macro;
		}
		catch (Exception e) { return m; }
	}

	/**
	Define a macro based on a construction in c and the targets and
	parameters in this construction. Store the macro in m.
	@param 
	*/
	public void defineMacro (Construction c, Macro m, 
		boolean targetsonly, boolean superhide, String prompt[],
				boolean hideduplicates)
			throws ConstructionException
	{	Vector P=c.getParameters(),T=c.getTargets();
		c.setTranslation(m); // for construction expressions only (windoww etc.)
		c.clearTranslations();
		if (T.size()==0) // got no targets 
			c.determineConstructables();
		else // got targets
		{	c.clearConstructables();
			c.setParameterAsConstructables();
			for (int i=0; i<T.size(); i++)
			{	c.determineConstructables((ConstructionObject)T.elementAt(i));
			}
		}
		// Make sure the counter for the macro object names starts
		// fresh (P1, P2, ...)
		Count.setAllAlternate(true);
		// Walk through the construction and copy all marked objects
		// to the macro definition.
		m.clearTranslators();
		Enumeration e=c.elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o instanceof TwoPointLineObject && 
				canConvert(c,(TwoPointLineObject)o) && o.isMainParameter())
			{	((TwoPointLineObject)o).getP1().setFlag(false);
				((TwoPointLineObject)o).getP2().setFlag(false);
			}					
		}
		e=c.elements();
		while (e.hasMoreElements())
		{	ConstructionObject oc,o=(ConstructionObject)e.nextElement();
			if (o.isFlag() &&
				!(o.isParameter() && !o.isMainParameter() && !needed(c,o,null)))
			{	// System.out.println(o.getName()+", parameter: "+o.isParameter()+
				//	", main: "+o.isMainParameter()+", needed "+needed(c,o,null));
				// Now copy to the macro, but make sure that parameters
				// are simplified to their object type. E.g., an
				// objectpoint becomes a point, if it is a parameter.
				if (o instanceof PointObject && o.isParameter())
				{	PointObject p=(PointObject)o;
					if (p.isSpecialParameter() && p.dependsOnParametersOnly())
						oc=(ConstructionObject)p.copy();
					else
						oc=new PointObject(c,p.getX(),p.getY());
				}
				else if (o instanceof FunctionObject && o.isParameter())
				{	FunctionObject fo=new FunctionObject(c);
					fo.setExpressions("x","0","0");
					fo.setRange("-10","10","0.1");
					oc=fo;
				}
				else if (o instanceof UserFunctionObject && o.isParameter())
				{	UserFunctionObject fo=new UserFunctionObject(c);
					fo.setExpressions("x","0");
					oc=fo;
				}
				else if (o instanceof ExpressionObject && o.isParameter())
				{	oc=new ExpressionObject(c,0,0);
					((ExpressionObject)oc).setExpression(o.getValue()+"",
							o.getConstruction());
					((ExpressionObject)oc).setCurrentValue(o.getValue());
				}
				else if (o instanceof TwoPointLineObject && 
					canConvert(c,(TwoPointLineObject)o) && o.isParameter())
				{	oc=new PrimitiveLineObject(c);
				}					
				else if (o instanceof PrimitiveLineObject && 
					!(o instanceof TwoPointLineObject) &&
					!(o instanceof FixedAngleObject) &&
					o.isParameter())
				{	oc=new PrimitiveLineObject(c);
				}
				else if (o instanceof PrimitiveCircleObject && o.isParameter())
				{	oc=new PrimitiveCircleObject(c,
						((PrimitiveCircleObject)o).getP1());
					oc.translateConditionals();
					oc.translate();
				}
				else if (o instanceof AreaObject && o.isParameter())
				{	oc=new AreaObject(c,new Vector());
					oc.translateConditionals();
					oc.translate();
				}
				else
				{	oc=(ConstructionObject)o.copy();
				}
				if (oc!=null)
				{	m.add(oc);
					if (o.isMainParameter()) oc.setName(o.getName());
					if (targetsonly && !o.isTarget() && !o.isParameter())
					{	if (superhide) oc.setSuperHidden(true);
						else oc.setHidden(true);
					}
					if (o.isParameter() && o.isHidden())
					{	oc.setHidden(true);
					}
					// All parameters in the constructions translate to
					// the paramters in the macro definition.
					o.setTranslation(oc);
				}
			}
			else o.setTranslation(null);
		}
		e=c.elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o.isFlag() && !o.isParameter())
				o.laterTranslate(c);
		}
		Count.setAllAlternate(false);
		// translate the @... forward declarations in FindObjectExpression.
		c.clearErrors();
		m.runTranslators(c);
		// see if any errors occured (@.. to nonexisting object, generated
		// by the FindObjectExpression on translation.
		e=c.getErrors();
		if (e.hasMoreElements())
		{	warning((String)e.nextElement(),"macroerror");
		}
		// Now set the paramter array and make sure it is translated
		// to the objects in the macro definition.
		ConstructionObject Params[]=new ConstructionObject[P.size()];
		for (int i=0; i<P.size(); i++)
		{	Params[i]=((ConstructionObject)P.elementAt(i)).getTranslation();
		}
		m.setParams(Params);
		String p[]=new String[prompt.length];
		for (int j=0; j<prompt.length; j++)
		{	ConstructionObject o=c.find(prompt[j]);
			if (o==null || o.getTranslation()==null ||
				!(o instanceof FixedCircleObject || 
					o instanceof FixedAngleObject ||
					o instanceof ExpressionObject))
				throw new ConstructionException(
					Zirkel.name("exception.prompt"));
			for (int i=0; i<P.size(); i++)
			{	ConstructionObject op=(ConstructionObject)P.elementAt(i);
				if (op==o)
					throw new ConstructionException(
						Zirkel.name("exception.prompt.parameter"));
			}
			p[j]=o.getTranslation().getName();
		}
		m.setPromptFor(p);
		for (int i=0; i<prompt.length; i++)
			m.setPromptName(i,prompt[i]);
		m.hideDuplicates(hideduplicates);
	}

	/**
	 * See, if this secondary parameter "o" is needed in the construction "c"
	 * by either a constructable object, or a parameter different from "besides".
	*/
	public boolean needed (Construction c, ConstructionObject o, 
			ConstructionObject besides)
	{	Enumeration e=c.elements();
		while (e.hasMoreElements())
		{	ConstructionObject u=(ConstructionObject) e.nextElement();
			if (!u.isFlag() || u==besides) continue;
			if (c.dependsDirectlyOn(u,o)) return true;
		}
		return false;
	}
	
	/**
	See, if this two point line can be converted to a primitive line.
	*/
	public boolean canConvert (Construction c, TwoPointLineObject o)
	{	PointObject p1=o.getP1(),p2=o.getP2();
		if (p1.isMainParameter() || p2.isMainParameter()) return false;
		return !(needed(c,p1,o) || needed(c,p2,o));
	}
	
	/**
	Define a macro from the information stored in the construction c,
	and store it to the macros in this ZirkelCanvas object.
	*/
	public void defineMacro (String name, String comment, Construction c)
		throws ConstructionException
	{	Vector T=c.getTargets();
		String Prompts[]=new String[c.Prompts.size()];
		for (int i=0; i<Prompts.length; i++)
			Prompts[i]=(String)c.Prompts.elementAt(i);
		Macro m=new Macro(this,name,comment,Prompts);
		defineMacro(c,m,T.size()>0 && !c.ShowAll,c.SuperHide,c.getPromptFor(),true);
		storeMacro(m,true);
	}
	
	/*
	Store the macro in the macro list (or replace the old macro
	with the same name
	@param all Replace the macro without asking.
	*/
	public void storeMacro (Macro m, boolean all)
	{	int i=0;
		for (i=0; i<Macros.size(); i++)
		{	if (((MacroItem)Macros.elementAt(i)).M.getName().equals(m.getName()))
			{	All=replaceMacro(m,i,all); // ask user if All=false
				break;
			}
		}
		if (i>=Macros.size()) appendMacro(m);
	}

	public boolean ProtectMacros=false;

	public MacroMenu MM=null;
	
	public void appendMacro (Macro m)	
	{	if (!ReadOnly)
		{	if (ProtectMacros) m.setProtected(true);
			if (MM==null) MM=new MacroMenu(PM,"",null);
			MacroItem mi=MM.add(m,m.getName());
			mi.I.addActionListener(this);
			Macros.addElement(mi);
		}
		else
		{	if (MM==null) MM=new MacroMenu(null,"",null);
			MacroItem mi=MM.add(m,m.getName());
			if (mi.I!=null) mi.I.addActionListener(this);
			Macros.addElement(mi);
		}
	}
	
	/**
	 * Replace the macro item number i with m.
	 * @return User wants to replace all subsequent macros.
	*/
	public boolean replaceMacro (Macro m, int i, boolean all)
	{	MacroItem e=(MacroItem)Macros.elementAt(i);
		if (all) // don't ask
		{	Macros.setElementAt(new MacroItem(m,e.I),i);
			return true;
		}
		else // ask
		{	ReplaceMacroQuestion d=new ReplaceMacroQuestion(getFrame(),m);
			d.center(getFrame());
			d.setVisible(true);
			if (d.isNo()) return false;
			MacroItem newitem=new MacroItem(m,e.I);
			MM.replace((MacroItem)Macros.elementAt(i),newitem);
			Macros.setElementAt(newitem,i);
			return d.isAll();
		}
	}

	public String MacroCurrentComment;
	
	/**
	The user has to choose from a list of macros (for running).
	*/
	public Macro chooseMacro ()
	{	if (Macros.size()==0)
		{	warning(Zirkel.name("definemacro.nomacro"));
			return null;
		}
		MacroSelectDialog d=new MacroSelectDialog(getFrame(),MM,false);
		d.setVisible(true);
		return d.getMacro();
	}

	/**
	* The user can choose from a list of macros (for saving).
	* @return A vector of selected Macros.
	*/
	public Vector chooseMacros ()
	{	if (Macros.size()==0)
		{	warning(Zirkel.name("definemacro.nomacro"));
			return null;
		}
		MacroSelectDialog d=
			new MacroSelectDialog(getFrame(),MM,true);
		d.setVisible(true);
		return d.getMacros();
	}

	/**
	* Run a macro by name.
	*/
	public Macro chooseMacro (String name)
	{	Enumeration e=Macros.elements();
		while (e.hasMoreElements())
		{	Macro m=((MacroItem)e.nextElement()).M;
			if (m.getName().equals(name))
				return m;
		}
		return null;
	}

	public void deleteMacros (Vector v)
	{	Enumeration e=v.elements();
		while (e.hasMoreElements())
		{	MacroItem m=(MacroItem)e.nextElement();
			deleteMacro(m);
		}
	}
	
	public void deleteMacro (MacroItem m)
	{	Macros.removeElement(m);
		if (m.I!=null)
		{	m.I.removeActionListener(this);
			MM.remove(m);
		}
	}

	public void clearMacros ()
	{	Enumeration e=Macros.elements();
		while (e.hasMoreElements())
		{	MacroItem m=(MacroItem)e.nextElement();
			if (m.I!=null)
			{	m.I.removeActionListener(this);
				MM.remove(m);
			}
		}
		Macros.removeAllElements();
	}

	public void clearNonprotectedMacros ()	
	{	Vector V=new Vector();	
		Enumeration e=Macros.elements();
		while (e.hasMoreElements())
		{	MacroItem m=(MacroItem)e.nextElement();
			if (!m.M.isProtected()) V.addElement(m);
		}
		deleteMacros(V);
	}

	public void clearProtectedMacros ()	
	{	Vector V=new Vector();	
		Enumeration e=Macros.elements();
		while (e.hasMoreElements())
		{	MacroItem m=(MacroItem)e.nextElement();
			if (m.M.isProtected()) V.addElement(m);
		}
		deleteMacros(V);
	}

	public void protectMacros ()
	{	Enumeration e=Macros.elements();
		while (e.hasMoreElements())
		{	MacroItem m=(MacroItem)e.nextElement();
			m.M.setProtected(true);
		}
	}
	
	public void renameMacro (Macro macro, String name)
	{	Enumeration e=Macros.elements();
		while (e.hasMoreElements())
		{	MacroItem m=(MacroItem)e.nextElement();
			if (m.I!=null && m.M==macro)
			{	deleteMacro(m);
				break;
			}
		}
		macro.setName(name);
		appendMacro(macro);
	}
	
	MacroBar MBar;
	
	public void setMacroBar (MacroBar m)
	{	MBar=m;
	}
	
	public void updateMacroBar ()
	{	if (MBar!=null)
		{	MBar.update(Macros);
		}
	}
	
	// For the prompt in the status line:
	
	TextField TF;
	public void setTextField (TextField t)
	{	TF=t;
	}
	public void setPrompt (String s)
	{	if (TF!=null) TF.setText(s);
	}

	// Loading:
	
	public void loadRun (InputStream is)
	{	BufferedReader in=new BufferedReader(
			new InputStreamReader(is));
		String s="",comment="";
		while (true)
		{	try
			{	s=in.readLine();
				if (s==null) break;
				int n;
				if ((n=s.indexOf("//"))>=0)
				{	comment=s.substring(n+2).trim();
					s=s.substring(0,n);
				}
				else comment="";
				s=s.trim();
				int k=0;
				if ((k=Interpreter.startTest("macro",s))>=0)
				{	loadMacro(in,s.substring(k).trim());
				}
				else if (!s.equals("")) C.interpret(this,s,comment);
			}
			catch (ConstructionException e)
			{	warning(e.getDescription()+" --- "+s);
				break;
			}
			catch (Exception e)
			{	warning(e.toString()+" --- "+s);
				e.printStackTrace();
				break;
			}
		}
		C.updateCircleDep();
	}
	
	public void loadMacro (BufferedReader in, String name)
		throws ConstructionException
	{	Construction c=new Construction();
		c.clear();
		String s="",comment="",macrocomment="";
		boolean inComment=true,newLine=true;
		while (true)
		{	try
			{	s=in.readLine();
				if (s==null)
					throw new ConstructionException(
						Zirkel.name("exception.macroend"));
				s=s.trim();
				int n=s.indexOf("//");
				if (inComment && n==0)
				{	String h=s.substring(n+2).trim();
					if (newLine)
					{	macrocomment=macrocomment+h;
						newLine=false;
					}
					else
					{	if (h.equals(""))
						{	macrocomment=macrocomment+"\n";
							newLine=true;
						}
						else
						{	macrocomment=macrocomment+" "+h;
							newLine=false;
						}
					}
					continue;
				}
				inComment=false;
				if (n>=0)
				{	comment=s.substring(n+2).trim();
					s=s.substring(0,n);
				}
				else comment="";
				s=s.trim();
				if (s.equals(Zirkel.name("end"))) break;
				if (s.toLowerCase().equals("end")) break;
				if (!s.equals("")) c.interpret(this,s,comment);
			}
			catch (InvalidException e)
			{
			}
			catch (ConstructionException e)
			{	throw new ConstructionException(e.getDescription()+" --- "+s);
			}
			catch (IOException e)
			{	warning(e.toString());
				return;
			}
		}
		defineMacro(name,macrocomment,c);
	}
	
	public double getGridSize ()
	{	double gridsize=Math.pow(10,Math.floor(
			Math.log(C.getW()*2)/Math.log(10)))/10;
		if (C.getW()*2/gridsize>=30) gridsize*=5;
		if (C.getW()*2/gridsize<10) gridsize/=2;
		return gridsize;
	}
	
	synchronized public void print (Graphics g, int W, int H)
	{	if (IW==0 || I==null) return;
		startWaiting();
		int w=IW,h=IH;
		Scale=(double)W/w;
		PointSize=PointSize*Scale;
		double fontsize=FontSize*Scale;
		MyGraphics mg=new MyGraphics11(g);
		mg.setSize(w,h);
		mg.setDefaultFont((int)(fontsize),
				Global.getParameter("font.large",false),
				Global.getParameter("font.bold",false));
		IW=W; IH=H;
		recompute();
		if (Background!=null)
		{	int bw=Background.getWidth(this),
				bh=Background.getHeight(this);
			if (bw==IW && bh==IH)
			{	mg.drawImage(Background,0,0,this);
			}
			else if (Global.getParameter("background.tile",true)
				&& bw<IW && bh<IH)
			{	for (int i=(IW%bw)/2-bw; i<IW; i+=bw)
					for (int j=(IH%bh)/2-bh; j<IH; j+=bh)
						mg.drawImage(Background,i,j,this);
			}
			else if (Global.getParameter("background.center",true))
			{	mg.drawImage(Background,(IW-bw)/2,(IH-bh)/2,this);
			}
			else
			{	mg.drawImage(Background,0,0,IW,IH,this);
			}
		}
		dopaint(mg);
		Interactive=true;
		Scale=1.0;
		I=null;
		endWaiting();
		repaint();
	}
	
	public LatexOutput createBB (String filename, 
			int w, int h, double dpi)
	{	try
		{	String path="";
			if (Global.getParameter("options.fullpath",true))
				path=FileName.pathAndSeparator(filename);
			PrintWriter out=
				new PrintWriter(new FileOutputStream(
						path+FileName.purefilename(filename)+".bb"));
			out.println("%%BoundingBox: 0 0 "+w+" "+h);
			out.close();
			out=new PrintWriter(new FileOutputStream(
					path+FileName.purefilename(filename)+".ztx"));
			LatexOutput lout=new LatexOutput(out);
			lout.open(w,h,dpi,path+FileName.filename(filename));
			return lout;
		}
		catch (Exception e)
		{	warning(e.toString());
		}
		return null;
	}
	
	/**
	 * Save picture as PNG on file or copy to clipboard
	 * @param filename ("" for clipboard)
	 * @return not failed
	 */
	synchronized public boolean savePNG (String filename, boolean latex)
	{	LatexOutput lout=null;
		if (IW==0 || I==null) return false;
		if (Global.getParameter("printscale.middle",true))
		{	savePreviewAsPNG(filename);
			return true;
		}
		double dpi=Global.getParameter("printscale.dpi",300);
		if (!Global.getParameter("printscale.sizes",false))
		{	int w=Global.getParameter("printscale.w",IW);
			int h=Global.getParameter("printscale.h",IH);
			if (latex) lout=createBB(filename,w,h,dpi);
			int IWold=IW;
			IW=w; IH=h;
			Scale=((double)IW)/IWold;
			I=createImage(IW,IH);
			if (!Global.getParameter("simplegraphics",false))
			{	IG=new MyGraphics13(I.getGraphics(),Scale,this,lout);
				IG.setSize(w,h);
			}
			else
			{	IG=new MyGraphics11(I.getGraphics());
				IG.setSize(w,h);
			}
			double pointsize=PointSize*Scale;
			double fontsize=FontSize*Scale;
			if (Scale<1)
			{	if (Global.getParameter("options.minpointsize",false) &&
					pointsize<MinPointSize) pointsize=MinPointSize;
				if (Global.getParameter("options.minfontsize",false) &&
					fontsize<MinFontSize) fontsize=MinFontSize;
			}
			IG.setDefaultFont((int)(fontsize),
					Global.getParameter("font.large",false),
					Global.getParameter("font.bold",false));
			PointSize=pointsize;
		}
		else
		{	int IWOld=IW;
			dpi=Global.getParameter("printscale.dpi",300.0);
			IW=(int)(Global.getParameter("printscale.width",5.0)/2.54*dpi);
			IH=(int)(Global.getParameter("printscale.height",5.0)/2.54*dpi);
			if (latex) lout=createBB(filename,IW,IH,dpi);
			I=createImage(IW,IH);
			if (!Global.getParameter("simplegraphics",false))
			{	IG=new MyGraphics13(I.getGraphics(),
					Global.getParameter("printscale.linewidth",0.02)/2.54*dpi,this,lout);
				IG.setSize(IW,IH);
			}
			else
			{	IG=new MyGraphics11(I.getGraphics());
				IG.setSize(IW,IH);
			}
			PointSize=Global.getParameter("printscale.pointsize",0.07)
				/2.54*dpi;
			IG.setDefaultFont((int)(Global.getParameter("printscale.textsize",0.3)
							/2.54*dpi),
					Global.getParameter("font.large",false),
					Global.getParameter("font.bold",false));
			Scale=(double)IW/IWOld;
		}
		recompute();
		if (Global.getParameter("options.bitmapbackground",false))
			IG.clearRect(0,0,IW,IH,getBackground());
		else
			IG.clearRect(0,0,IW,IH,Color.white);
		if (Background!=null)
		{	int bw=Background.getWidth(this),
				bh=Background.getHeight(this);
			if (bw==IW && bh==IH)
			{	IG.drawImage(Background,0,0,this);
			}
			else if (Global.getParameter("background.tile",true)
				&& bw<IW && bh<IH)
			{	for (int i=(IW%bw)/2-bw; i<IW; i+=bw)
					for (int j=(IH%bh)/2-bh; j<IH; j+=bh)
						IG.drawImage(Background,i,j,this);
			}
			else if (Global.getParameter("background.center",true))
			{	IG.drawImage(Background,(IW-bw)/2,(IH-bh)/2,this);
			}
			else
			{	IG.drawImage(Background,0,0,IW,IH,this);
			}
		}
		dopaint(IG);
		if (lout!=null) lout.close();
		Interactive=true;
		if (!filename.equals(""))
		{	try
			{	BufferedOutputStream 
					out=new BufferedOutputStream(new FileOutputStream(filename));
				PngEncoder png=new PngEncoder(I,PngEncoder.NO_ALPHA,0,9);
				png.setDPI(dpi);
				out.write(png.pngEncode());
				out.close();
			}
			catch (Exception e)
			{	warning(e.toString());
			}
		}
		else
		{	try
			{	Clipboard clipboard = getToolkit().getSystemClipboard();
				ImageSelection is=new ImageSelection(I);
				clipboard.setContents(is,null);
			}
			catch (Exception e)
			{	Scale=1;
				I=null;
				repaint();
				return false;
			}
		}
		Scale=1;
		I=null;
		repaint();
		return true;
	}
	
	public void savePreviewAsPNG (String filename)
	{	int PW=Global.getParameter("printscale.w",IW);
		int PH=Global.getParameter("printscale.h",IH);
		Image i=createImage(PW,PH);
		Graphics g=i.getGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0,0,PW,PH);
		g.drawImage(I,-(IW-PW)/2,-(IH-PH)/2,this);
		if (!filename.equals(""))
		{	try
			{	FileOutputStream out=new FileOutputStream(filename);
				PngEncoder png=new PngEncoder(i,PngEncoder.NO_ALPHA,0,9);
				out.write(png.pngEncode());
				out.close();
			}
			catch (Exception e)
			{	warning(e.toString());
			}
		}		
		else
		{	try
			{	Clipboard clipboard = getToolkit().getSystemClipboard();
				ImageSelection is=new ImageSelection(I);
				clipboard.setContents(is,null);
			}
			catch (Exception e)
			{}
		}
	}
	
	public void saveFIG (String filename)
	{	try
		{	PrintWriter out=new PrintWriter(
				new FileOutputStream(filename));
			MyGraphicsFig fig=new MyGraphicsFig(out,IW,IH);
			fig.setSize(IW,IH);
			dopaintFig(fig);
			fig.close();
			out.close();
		}
		catch (Exception e)
		{	warning(e.toString());
			e.printStackTrace();
		}
	}
	
	public void saveSVG (String filename)
	{	try
		{	OutputStream o=new FileOutputStream(filename);
			if (ZirkelFrame.isCompressed(filename))
				o=new GZIPOutputStream(o); 
			PrintWriter out=new PrintWriter(new OutputStreamWriter(o,"UTF8"));
			MyGraphicsSvg svg=new MyGraphicsSvg(out,IW,IH);
			svg.setSize(IW,IH);
			dopaint(svg);
			svg.close();
			out.close();
		}
		catch (Exception e)
		{	warning(e.toString());
			e.printStackTrace();
		}
	}

	public void savePDF (String filename)
	{	PrintWriter out=null;
		try
		{	OutputStream o=new FileOutputStream(filename);
			if (ZirkelFrame.isCompressed(filename))
				o=new GZIPOutputStream(o); 
			out=new PrintWriter(new OutputStreamWriter(o,"CP1252"));
		}
		catch (Exception e)
		{	warning(e.toString());
			e.printStackTrace();
		}	
		int iw=IW,ih=IH;
		IW=(int)(Global.getParameter("printscale.width",IW*2)/2.54*72+0.5);
		IH=(int)(Global.getParameter("printscale.height",IH*2)/2.54*72+0.5);
		double ps=PointSize; 
		Scale=(double)IW/iw;
		double pointsize=PointSize*Scale;
		double fontsize=FontSize*Scale+1;
		double linewidth=Scale;
		if (Global.getParameter("printscale.sizes",false))
		{	pointsize=Global.getParameter("printscale.pointsize",0.07)/2.54*72;
			fontsize=Global.getParameter("printscale.fontsize",0.3)/2.54*72;
			linewidth=Global.getParameter("printscale.linewidth",0.02)/2.54*72;
		}
		PointSize=pointsize;
		recompute();
		MyGraphicsPDF pdf=new MyGraphicsPDF(out,IW,IH,linewidth);
		pdf.setSize(IW,IH);
		pdf.setDefaultFont((int)(fontsize),
				Global.getParameter("font.large",false),
				Global.getParameter("font.bold",false));
		try
		{	dopaint(pdf);
			pdf.close();
			out.close();
		}
		catch (Exception e)
		{	warning(e.toString());
			e.printStackTrace();
		}
		IW=iw; IH=ih; 
		Scale=1.0;
		PointSize=ps;
		I=null;
		repaint();
	}

	public void saveEPS (String filename)
	{	OutputStream o=null;
		try
		{	o=new FileOutputStream(filename);
			if (ZirkelFrame.isCompressed(filename))
				o=new GZIPOutputStream(o);
		}
		catch (Exception e)
		{	warning(e.toString());
			e.printStackTrace();
		}
		int iw=IW,ih=IH;
		IW=(int)(Global.getParameter("printscale.width",IW*2)/2.54*72+0.5);
		IH=(int)(Global.getParameter("printscale.height",IH*2)/2.54*72+0.5);
		double ps=PointSize; 
		Scale=(double)IW/iw;
		double pointsize=PointSize*Scale;
		double fontsize=FontSize*Scale+1;
		double linewidth=Scale;
		if (Global.getParameter("printscale.sizes",false))
		{	pointsize=Global.getParameter("printscale.pointsize",0.07)/2.54*72;
			fontsize=Global.getParameter("printscale.fontsize",0.3)/2.54*72;
			linewidth=Global.getParameter("printscale.linewidth",0.02)/2.54*72;
		}
		PointSize=pointsize;
		recompute();
		try
		{	MyGraphicsEPS eps=new MyGraphicsEPS(o,IW,IH);
			eps.setSize(IW,IH);
			eps.setLineWidth(linewidth);
			eps.setDefaultFont((int)(fontsize),
				Global.getParameter("font.large",false),
				Global.getParameter("font.bold",false));
			dopaint(eps);
			eps.close();
			o.close();
		}
		catch (Exception e)
		{	warning(e.toString());
			e.printStackTrace();
		}
		IW=iw; IH=ih; 
		Scale=1.0;
		PointSize=ps;
		I=null;
		repaint();
	}
	
	/**
	Return pressed.
	*/
	public void returnPressed ()
	{	if (OC instanceof MacroRunner)
		{	((MacroRunner)OC).returnPressed(this);
		}
	}
	
	public void bind (PointObject p)
	{	setTool(new BinderTool(this,p,OC));
	}
	
	public void setAway (IntersectionObject p, boolean away)
	{	setTool(new SetAwayTool(this,p,away,OC));
	}
	
	public void setCurveCenter (FunctionObject p)
	{	setTool(new SetCurveCenterTool(this,p,OC));
	}
	
	public void range (PrimitiveCircleObject c)
	{	setTool(new SetRangeTool(this,c,OC));
	}
	
	public void set (FixedAngleObject a)
	{	setTool(new SetFixedAngle(this,a,OC));
	}
	
	public void set (FixedCircleObject c)
	{	setTool(new SetFixedCircle(this,c,OC));
	}
	
	public boolean enabled (String function)
	{	if (ZCI!=null) return ZCI.enabled(function);
		else return true;
	}
	
	public void pause (boolean flag)
	{	OC.pause(flag);
	}
	
	public void setReadOnly (boolean flag)
	{	ReadOnly=flag;
	}

	public void allowRightMouse (boolean flag)
	{	AllowRightMouse=flag;
	}

	public boolean changed ()
	{	return C.changed();
	}
	
	Image OldBackground=null;
	
	/**
	 * Create a background image for the Movertool, consisting of
	 * the current construction.
	 * This is called when moving with the control key is
	 * called.
	 * @param flag
	 */
	public void grab (boolean flag)
	{	if (flag)
		{	OldBackground=Background;
			Background=createImage(IW,IH);
			Graphics G=Background.getGraphics();
			G.drawImage(I,0,0,this);
		}
		else
		{	Background=OldBackground;
			OldBackground=null;
		}
		repaint();
	}
	
	public void setBackground (Image i)
	{	Background=i;
		repaint();
	}

	public void setInteractive (boolean flag)
	{	Interactive=flag;
	}
	
	public ObjectConstructor getCurrentTool ()
	{	return OC;
	}

	MyVector Drawings=new MyVector();
	
	public synchronized void addDrawing (Drawing d)
	{	Drawings.addElement(d);
	}
	
	public synchronized void clearDrawings ()
	{	Drawings.removeAllElements();
		repaint();
	}	

	public synchronized void paintDrawings (MyGraphics g)
	{	Enumeration e=Drawings.elements();
		while (e.hasMoreElements())
		{	Drawing d=(Drawing)e.nextElement();
			Enumeration ec=d.elements();
			if (ec.hasMoreElements())
			{	g.setColor(ZirkelFrame.Colors[d.getColor()]);
				CoordinatesXY xy=(CoordinatesXY)ec.nextElement();
				int c=(int)col(xy.X),r=(int)row(xy.Y);
				while (ec.hasMoreElements())
				{	xy=(CoordinatesXY)ec.nextElement();
					int c1=(int)col(xy.X),r1=(int)row(xy.Y);
					g.drawLine(c,r,c1,r1);
					c=c1; r=r1;
				}
			}
		}
	}

	public void saveDrawings (XmlWriter xml)
	{	Enumeration e=Drawings.elements();
		while (e.hasMoreElements())
		{	Drawing d=(Drawing)e.nextElement();
			Enumeration ec=d.elements();
			if (ec.hasMoreElements())
			{	xml.startTagNewLine("Draw","color",""+d.getColor());
				while (ec.hasMoreElements())
				{	CoordinatesXY xy=(CoordinatesXY)ec.nextElement();
					xml.startTagStart("Point");
					xml.printArg("x",""+xy.X);
					xml.printArg("y",""+xy.Y);
					xml.finishTagNewLine();
				}
				xml.endTagNewLine("Draw");
			}	
		}
	}	

	public void loadDrawings (XmlTree tree)
		throws ConstructionException
	{	XmlTag tag=tree.getTag();
		if (!tag.name().equals("Draw")) return;
		Drawing d=new Drawing();
		try
		{	if (tag.hasParam("color"))
			{	d.setColor(
					Integer.parseInt(tag.getValue("color")));
			}
		}
		catch (Exception e)
		{	throw new ConstructionException("Illegal Draw Parameter");
		}
		Enumeration e=tree.getContent();
		while (e.hasMoreElements())
		{	XmlTree t=(XmlTree)e.nextElement();
			tag=t.getTag();
			if (tag.name().equals("Point"))
			{	try
				{	double x=new Double(tag.getValue("x")).doubleValue();
					double y=new Double(tag.getValue("y")).doubleValue();
					d.addXY(x,y);
				}
				catch (Exception ex)
				{	throw new ConstructionException("Illegal Draw Parameter");
				}
			}
		}
		Drawings.addElement(d);
	}

	int PointLast,LineLast,AngleLast;

	public void renameABC (ConstructionObject o, boolean enforce, boolean reset)
	{	if (!enforce)
		{	if (o instanceof PointObject)
			{	for (int i='A'; i<='Z'; i++)
				{	ConstructionObject h=C.find(""+(char)i);
					if (h==null)
					{	o.setName(""+(char)i);
						o.setShowName(true);
						repaint();
						break;
					}
				}
			}
			else if (o instanceof AngleObject || o instanceof FixedAngleObject)
			{	for (int i='a'; i<='z'; i++)
				{	ConstructionObject h=C.find("\\"+(char)i);
					if (h==null)
					{	o.setName("\\"+(char)i);
						o.setShowName(true);
						repaint();
						break;
					}
				}
			}
			else if (o instanceof PrimitiveLineObject)
			{	for (int i='a'; i<='z'; i++)
				{	ConstructionObject h=C.find(""+(char)i);
					if (h==null)
					{	o.setName(""+(char)i);
						o.setShowName(true);
						repaint();
						break;
					}
				}
			}
		}
		else
		{	if (reset)
			{	PointLast=0; LineLast=0; AngleLast=0;
			}
			if (o instanceof PointObject)
			{	String name=""+(char)('A'+PointLast);
				ConstructionObject h=C.find(name);
				if (h!=null && h!=o) 
				{	h.setName("***temp***");
					String s=o.getName();
					o.setName(name);
					h.setName(s);
				}
				else
				{	o.setName(name);
				}
				PointLast++;
			}
			else if (o instanceof AngleObject || o instanceof FixedAngleObject)
			{	String name="\\"+(char)('a'+AngleLast);
				ConstructionObject h=C.find(name);
				if (h!=null && h!=o) 
				{	h.setName("***temp***");
					String s=o.getName();
					o.setName(name);
					h.setName(s);
				}
				else
				{	o.setName(name);
				}
				AngleLast++;
			}
			else if (o instanceof PrimitiveLineObject)
			{	String name=""+(char)('a'+LineLast);
				ConstructionObject h=C.find(name);
				if (h!=null && h!=o) 
				{	h.setName("***temp***");
					String s=o.getName();
					o.setName(name);
					h.setName(s);
				}
				else
				{	o.setName(name);
				}
				LineLast++;
			}
		}
	}
	
	public void selectAllMoveableVisibleObjects ()
	{	Enumeration e=C.elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o instanceof MoveableObject && ((MoveableObject)o).moveable()
				&& !o.mustHide(this))
			{	o.setStrongSelected(true);
			}
		}
	}
	
	public void hideDuplicates (ConstructionObject from)
	{	Enumeration e=C.elements();
		if (from!=null)
		{	while (e.hasMoreElements())
			{	ConstructionObject o=(ConstructionObject)e.nextElement();
				if (o==from) break;
			}
		}
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o.mustHide(this) || o.isKeep()) continue;
			Enumeration ex=C.elements();
			while (ex.hasMoreElements())
			{	ConstructionObject o1=(ConstructionObject)ex.nextElement();
				if (o1==o) break;
				if (o1.mustHide(this) || o1.isKeep()) continue;
				if (o.equals(o1))
				{	o.setHidden(true);
					break;
				}
			}
		}
	}
	
	public void hideDuplicates ()
	{	hideDuplicates(null);
	}
	
	public void createCurve ()
	{	FunctionObject f=new FunctionObject(C);
		f.setDefaults();
		C.add(f);
		f.setExpressions("x","x","0");
		f.edit(this);
		if (f.EditAborted) delete(f);
		repaint();
		reloadCD();
	}
	
	public void createFunction ()
	{	UserFunctionObject f=new UserFunctionObject(C);
		f.setDefaults();
		f.setShowName(true);
		f.setShowValue(true);
		C.add(f);
		f.setExpressions("x","0");
		f.edit(this);
		if (f.EditAborted) delete(f);
		repaint();
		reloadCD();
	}
	
	public void editLast ()
	{	if (C.last()==null) return;
		C.last().edit(this);
	}
	
	public void breakpointLast (boolean flag, boolean hiding)
	{	ConstructionObject o=C.last();
		if (o==null) return;
		if (hiding) o.setHideBreak(flag);
		else o.setBreak(flag);
	}
	
	public void notifyChanged ()
	{	if (!C.Loading) reloadCD();
	}

	public void startWaiting ()
	{	Interactive=false;
		showMessage(Zirkel.name("message.saving"));
	}

	public void endWaiting ()
	{	Interactive=true;
		hideMessage();
	}
	
	public void showMessage (String s)
	{	hideMessage();
		MW=new MessageWindow(F,s);
		MW.setVisible(true);
	} 
	
	public void hideMessage ()
	{	if (MW!=null)
		{	MW.setVisible(false); MW.dispose();
			MW=null;
		}
	}
	
	// HotEqn stuff, requires the HotEqn classes:
	
	sHotEqn HE=null;
	
	public void setHotEqn (String s)
	{	if (HE==null)
		{	HE=new sHotEqn(this);
		}
		HE.setEquation(s);
	}
	
	public int paintHotEqn (int c, int r, Graphics g)
	{	if (HE==null) return 0;
		return HE.paint(c,r,g);
	}
	
	// Stuff for the permanent construction display
	
	public ConstructionDisplayPanel CDP=null;
	
	public void reloadCD ()
	{	if (CDP!=null && C!=null)
		{	CDP.reload();
		}
	}
	
	public void repaintCD ()
	{	if (CDP!=null && C!=null) CDP.updateDisplay();	
	}
	
	public void callCDAction (String action)
	{	if (CDP!=null && C!=null) CDP.doAction(action);
	}
	
	public void callCDItem (String action, boolean flag)
	{	if (CDP!=null && C!=null) CDP.itemAction(action,flag);
	}
	
	public void callCDToggleItem (String action)
	{	if (CDP!=null && C!=null) CDP.itemToggleAction(action);
	}
	
	public void mouseWheelMoved (MouseWheelEvent e) 
	{	if (Global.getParameter("options.nomousezoom",false)) return;
		if (e.getScrollType()==MouseWheelEvent.WHEEL_BLOCK_SCROLL)
		{	if (e.getWheelRotation()<0)
			{	magnify(1/Math.sqrt(Math.sqrt(2)));
			}
			else
			{	magnify(Math.sqrt(Math.sqrt(2)));
			}
		}
		else
		{	int n=e.getScrollAmount();
			if (e.getWheelRotation()<0)
			{	magnify(1/Math.pow(2,n/12.));
			}
			else
			{	magnify(Math.pow(2,n/12.));
			}
		}
	}
}
