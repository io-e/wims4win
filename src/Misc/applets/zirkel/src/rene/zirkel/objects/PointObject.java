package rene.zirkel.objects;

// file: PointObject.java

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import rene.gui.*;
import rene.dialogs.*;

import rene.util.xml.*;
import rene.zirkel.*;
import rene.zirkel.construction.*;
import rene.zirkel.dialogs.*;
import rene.zirkel.expression.*;
import rene.zirkel.graphics.*;

class PointEditDialog extends ObjectEditDialog
{	
	// jm.evers
	public String xaxis_variable;
	public String yaxis_variable;
	// end jm.evers
	TextFieldAction X,Y;
	MyTextField Away,Bound,Increment;
	Checkbox Fixed,Close,Restricted,Alternate,Inside;
	IconBar TypeIB;
	ZirkelCanvas ZC;
	
	public PointEditDialog (Frame f, PointObject o)
	{	super(f,Zirkel.name("edit.point.title"),o);
	}
	public PointEditDialog (ZirkelCanvas zc, PointObject o)
	{	this(zc.getFrame(),o);
		ZC=zc;
	}
	
	public void addFirst (Panel P)
	{
	    // jm.evers
		xaxis_variable=ZirkelApplet.xaxis_variable;
		yaxis_variable=ZirkelApplet.yaxis_variable;
		PointObject p=(PointObject)O;
		X=new TextFieldAction(this,xaxis_variable,""+p.round(p.getX()),30);
		P.add(new MyLabel(""+xaxis_variable+"")); P.add(X);
		Y=new TextFieldAction(this,yaxis_variable,""+p.round(p.getY()),30);
		P.add(new MyLabel(""+yaxis_variable+"")); P.add(Y);
	    // end jm.evers
/*
		X=new TextFieldAction(this,"X",""+p.round(p.getX()),30);
		P.add(new MyLabel(Zirkel.name("edit.point.x"))); P.add(X);
		Y=new TextFieldAction(this,"Y",""+p.round(p.getY()),30);
		P.add(new MyLabel(Zirkel.name("edit.point.y"))); P.add(Y);
*/
		if (p.moveablePoint())
		{	Fixed=new Checkbox("");
			Fixed.setState(p.fixed());
			P.add(new MyLabel(Zirkel.name("edit.fixed")));
			P.add(Fixed);
			if (p.fixed())
			{	X.setText(p.getEX());
				Y.setText(p.getEY());
			}
			P.add(new MyLabel(Zirkel.name("edit.point.increment")));
			P.add(Increment=new MyTextField(""+p.round(p.getIncrement())));
		}
		else
		{	X.setEditable(false);
			Y.setEditable(false);
		}

		if ((p instanceof IntersectionObject &&
			((IntersectionObject)p).isSwitchable()))
		{	Away=new MyTextField("",5);
			Away.setText(((IntersectionObject)p).away());
			Close=new CheckboxAction(this,Zirkel.name("edit.point.close"));
			Close.setState(!((IntersectionObject)p).stayAway());
			P.add(new MyLabel(Zirkel.name("edit.point.intersection")));
			Panel ap=new MyPanel();
			ap.setLayout(new GridLayout(1,2));
			ap.add(Away);
			ap.add(Close);
			P.add(ap);
		}
		
		if (p instanceof IntersectionObject)
		{	P.add(new MyLabel(Zirkel.name("edit.plumb.restricted")));
			Restricted=new CheckboxAction(this,"","Restricted");
			Restricted.setState(((IntersectionObject)p).isRestricted());
			P.add(Restricted);
			if (((IntersectionObject)p).canAlternate())
			{	P.add(new MyLabel(Zirkel.name("edit.intersection.alternate")));
				Alternate=new CheckboxAction(this,"","Alternate");
				Alternate.setState(((IntersectionObject)p).isAlternate());
				P.add(Alternate);
			}
		}

		if (p.isPointOn())
		{	P.add(new MyLabel(Zirkel.name("edit.point.bound")));
			P.add(Bound=new MyTextField(p.getBound().getName()));
			Bound.setEditable(false);
			if (p.getBound() instanceof InsideObject)
			{	P.add(new MyLabel(Zirkel.name("edit.point.inside")));
				P.add(Inside=new CheckboxAction(this,"","Inside"));
				Inside.setState(p.isInside());
			}
			if (Fixed!=null) Fixed.setState(p.useAlpha());
		}
		
	}
	
	Button BoundButton;
	
	public void addButton (Panel P)
	{	PointObject p=(PointObject)O;
		if (p.moveablePoint())
		{	if (p.isPointOn())
				BoundButton=new ButtonAction(this,
					Zirkel.name("bound.release"),"Release");
			else
				BoundButton=new ButtonAction(this,
					Zirkel.name("bound.bind"),"Bind");
			P.add(BoundButton);
			P.add(new MyLabel(" "));
		}
		else if (p instanceof IntersectionObject  &&
				((IntersectionObject)p).isSwitchable())
		{	P.add(new ButtonAction(this,Zirkel.name("edit.point.away"),
				"SetAway"));
			P.add(new ButtonAction(this,Zirkel.name("edit.point.close"),
				"SetClose"));
			if (!((IntersectionObject)p).away().equals(""))
			P.add(new ButtonAction(this,Zirkel.name("edit.point.free"),
				"SetFree"));
			P.add(new MyLabel(" "));
		}
	}
	
	public void addSecond (Panel P)
	{	PointObject p=(PointObject)O;
	
		TypeIB=new IconBar(F);
		TypeIB.addToggleGroupLeft("type",6);
		TypeIB.toggle("type",p.getType());
		P.add(new MyLabel("")); P.add(TypeIB);
	}

	public void doAction (String o)
	{
		//jm.evers probably does nothing...
		if ((o.equals(xaxis_variable) || o.equals(yaxis_variable)) && Fixed!=null)
		{	Fixed.setState(true);
			super.doAction("OK");
		}
		//end jm.evers
		if ((o.equals("Y") || o.equals("X")) && Fixed!=null)
		{	Fixed.setState(true);
			super.doAction("OK");
		}
		else if (o.equals("Release"))
		{	((PointObject)O).setBound("");
			O.getConstruction().updateCircleDep();
			if (Fixed!=null) Fixed.setState(false);
			super.doAction("OK");
		}
		else if (o.equals("Bind"))
		{	ZC.bind((PointObject)O);
			super.doAction("OK");
		}
		else if (o.equals("SetAway"))
		{	ZC.setAway((IntersectionObject)O,true);
			super.doAction("OK");
		}
		else if (o.equals("SetClose"))
		{	ZC.setAway((IntersectionObject)O,false);
			super.doAction("OK");
		}
		else if (o.equals("SetFree"))
		{	((IntersectionObject)O).setAway("");
			Away=null;
			super.doAction("OK");
		}
		else if (o.equals("OK"))
		{	if (Fixed!=null && X.isChanged() || Y.isChanged())
				Fixed.setState(true);
			super.doAction("OK");
		}
		else super.doAction(o);
	}

	public void setAction ()
	{	PointObject p=(PointObject)O;
		
		if ((X.isChanged() || Y.isChanged()) && p.isPointOn())
		{	try
			{	double x=new Expression(X.getText(),
					p.getConstruction(),p).getValue();
				double y=new Expression(Y.getText(),
					p.getConstruction(),p).getValue();
				p.move(x,y);
				p.validate();
			}
			catch (Exception e) {}
		}
		
		if (Fixed!=null && Fixed.getState()==true)
		{	if (p.isPointOn()) p.setUseAlpha(true);
			else p.setFixed(X.getText(),Y.getText());
		}
		else
		{	try
			{	double x=new Expression(X.getText(),
					p.getConstruction(),p).getValue();
				double y=new Expression(Y.getText(),
					p.getConstruction(),p).getValue();
				if (p.moveable()) p.move(x,y);
			}
			catch (Exception e) {}
		}
		if (Fixed!=null && Fixed.getState()==false)
		{	if (p.isPointOn()) p.setUseAlpha(false);
			else p.setFixed(false);
		}
		
		if (Away!=null)
		{	if (!((IntersectionObject)p).setAway(Away.getText(),!Close.getState()))
			{	Warning w=new Warning(F,Zirkel.name("bound.error"),
					Zirkel.name("warning"));
				w.center(F);
				w.setVisible(true);
			}
		}
		if (Restricted!=null)
		{	((IntersectionObject)p).setRestricted(Restricted.getState());
		}
		if (Alternate!=null)
		{	((IntersectionObject)p).setAlternate(Alternate.getState());
		}
		if (Increment!=null)
		{	try
			{	p.setIncrement(new Expression(Increment.getText(),
					p.getConstruction(),p).getValue());
			}
			catch (Exception e) {}
		}
		p.setType(TypeIB.getToggleState("type"));
		if (Inside!=null)
		{	p.setInside(Inside.getState());
		}
	}

	public void focusGained (FocusEvent e)
	{	if (Fixed!=null && Fixed.getState()) X.requestFocus();
		else super.focusGained(e);
	}
}

public class PointObject extends ConstructionObject
	implements MoveableObject
{	protected double X,Y;
	protected double Alpha; // parameter relative zu object
	protected boolean AlphaValid=false; // Alpha is valid
	protected boolean UseAlpha=false; // Use Alpha at all
	protected boolean Moveable,Fixed;
	private static Count N=new Count();
	protected int Type=0;
	public final static int SQUARE=0,DIAMOND=1,CIRCLE=2,DOT=3,CROSS=4,DCROSS=5;
	public static int MaxType=3;
	protected Expression EX,EY;
	private ConstructionObject Bound=null; // point is on a line etc.
	private boolean Later; // bound is later in construction
	private String LaterBind="";
	private boolean KeepInside; // keep point inside bound
	private boolean DontUpdate=false;
	private double Increment=0; // increment to keep on grid 
		
	protected ConstructionObject MovedBy;
		// The object that may have moved this point
	
	public PointObject (Construction c, double x, double y)
	{	super(c);
		X=x; Y=y;
		Moveable=true; Fixed=false;
		setColor(ColorIndex);
		updateText();
		Type=0;
	}
	public PointObject (Construction c, double x, double y,
		ConstructionObject bound)
	{	this(c,x,y);
		Bound=bound;
	}
	public PointObject (Construction c, String name)
	{	super(c,name);
		X=0; Y=0;
		Moveable=true; Fixed=false;
		setColor(ColorIndex);
		updateText();
		Type=0;
	}
	
	public String getTag ()
	{	if (Bound==null) return "Point"; 
		else return "PointOn";
	}
	public int getN () { return N.next(); }
	
	public void setDefaults ()
	{	super.setDefaults();
		Type=Cn.DefaultType;
	}
	
	private double Delta;
	
	public double changedBy ()
	{	return Delta;
	}
	
	public void validate ()
	{	if (DontUpdate) return;
		MovedBy=null;
		Delta=0.0;
		Valid=true;
		if (Bound!=null && !Bound.isInConstruction()) Bound=null;
		if (Bound!=null && !Bound.valid())
		{	Valid=false;
			return;
		}
		if (Increment>1e-4)
		{	X=Math.floor(X/Increment+0.5)*Increment;
			Y=Math.floor(Y/Increment+0.5)*Increment;
		}
		if (Bound!=null)
		{	double x=X,y=Y;
			if (KeepInside && Bound instanceof InsideObject)
			{	((InsideObject)Bound).keepInside(this);
			}
			else if (!KeepInside && Bound instanceof PointonObject)
			{	if (!AlphaValid || !UseAlpha) project(Bound);
				else project(Bound,Alpha);
			}
			if (Later) Delta=Math.sqrt((x-X)*(x-X)+(y-Y)*(y-Y));
		}
		if (Fixed && EX!=null && EX.isValid())
		{	try
			{	X=EX.getValue();
			}
			catch (Exception e)
			{	Valid=false; return;
			}
		}
		if (Fixed && EY!=null && EY.isValid())
		{	try
			{	Y=EY.getValue();
			}
			catch (Exception e)
			{	Valid=false; return;
			}
		}
	}
	
	public void updateText ()
	{	if (Bound!=null)
			setText(text1(Zirkel.name("text.boundedpoint"),Bound.getName()));
		else if (EX!=null && EY!=null)
			setText(text2(Zirkel.name("text.point"),"\""+EX+"\"","\""+EY+"\""));
		else
			setText(text2(Zirkel.name("text.point"),""+round(X),""+round(Y)));			
	}
	
	static double x[]=new double[4],y[]=new double[4];
	
	public void paint (MyGraphics g, ZirkelCanvas zc)
	{	DisplaysText=false;
		if (!Valid || mustHide(zc)) return;
		double size=drawPoint(g,zc,this,X,Y,Type);
		String s=AngleObject.translateToUnicode(getDisplayText());
		if (!s.equals(""))
		{	g.setLabelColor(this);
			DisplaysText=true;
			setFont(g);
			double d=Math.sqrt(XcOffset*XcOffset+YcOffset*YcOffset);
			if (!KeepClose || d<1e-10)
			{	TX1=zc.col(X+XcOffset)+2*size;
				TY1=zc.row(Y+YcOffset)+2*size;
				drawLabel(g,s);
			}
			else
			{	drawPointLabel(g,s,zc,X,Y,YcOffset/d,-XcOffset/d,0,0);
			}
		}
	}
	
	static public double drawPoint (MyGraphics g, ZirkelCanvas zc, 
			ConstructionObject o, double X, double Y, int type)
	{	double size=zc.pointSize();
		double r=zc.col(X),c=zc.row(Y);
		if (size<1) size=1;
		if (o.visible(zc))
		{	if (o.isStrongSelected() && g instanceof MyGraphics13)
			{	((MyGraphics13)g).drawMarkerLine(r,c,r,c);
			}
			g.setColor(o);
			switch (type)
			{	case SQUARE :
					if (o.getColorType()==THICK)
					{	size-=1;
						g.fillRect(r-size,c-size,2*size,2*size,true,false,o);
					}
					else
						g.drawRect(r-size,c-size,2*size,2*size);
					break;
				case DIAMOND : 
					size+=1;
					if (o.getColorType()==THICK)
					{	size+=1;
						x[0]=r-size; x[1]=r; x[2]=r+size; x[3]=r;
						y[0]=c; y[1]=c-size; y[2]=c; y[3]=c+size;
						g.fillPolygon(x,y,4,false,false,o);
					}
					else
					{	g.drawLine(r-size,c,r,c+size);
						g.drawLine(r-size,c,r,c-size);
						g.drawLine(r+size,c,r,c+size);
						g.drawLine(r+size,c,r,c-size);
					}
					break;
				case CIRCLE :
					if (o.getColorType()==THICK)
						g.fillOval(r-size-1,c-size-1,2*size+2,2*size+2,true,false,o);
					else
						g.drawOval(r-size,c-size,2*size,2*size);
					break;
				case DOT :
					if (o.getColorType()==THICK) g.fillRect(r,c,1,1,true,false,o);
					else g.drawLine(r,c,r,c);
					break;
				case CROSS :
					if (o.getColorType()==THICK)
					{	g.drawThickLine(r-size,c,r+size,c);
						g.drawThickLine(r,c-size,r,c+size);
					}
					else
					{	g.drawLine(r-size,c,r+size,c);
						g.drawLine(r,c-size,r,c+size);
					}
					break;
				case DCROSS :
					if (o.getColorType()==THICK)
					{	g.drawThickLine(r-size,c-size,r+size,c+size);
						g.drawThickLine(r+size,c-size,r-size,c+size);
					}
					else
					{	g.drawLine(r-size,c-size,r+size,c+size);
						g.drawLine(r+size,c-size,r-size,c+size);
					}
					break;
			}
		}
		return size;
	}
	
	public String getDisplayValue ()
	{	return "("+roundDisplay(X)+
			(Global.getParameter("options.germanpoints",false)?"|":",")
			+roundDisplay(Y)+")";
	}
	
	public boolean nearto (int x, int y, ZirkelCanvas zc)
	{	if (!displays(zc)) return false;
		double c=zc.col(X),r=zc.row(Y);
		int size=(int)zc.selectionSize();
		Value=Math.abs(x-c)+Math.abs(y-r);
		return Value<=size*3/2;
	}
	
	public boolean nearto (PointObject p)
	{	if (!Valid) return false;
		double dx=p.X-X,dy=p.Y-Y;
		return Math.sqrt(dx*dx+dy*dy)<1e-9;
	}
	
	public double distanceTo (int x, int y, ZirkelCanvas zc)
	{	double dx=x-zc.col(X),dy=y-zc.row(Y);
		return Math.sqrt(dx*dx+dy*dy);
	}
	
	public double getX () { return X; }
	public double getY () { return Y; }
	
	public boolean moveable ()
	{	boolean fixed=Fixed;
		if (dependsOnItselfOnly()) fixed=false;
		return Moveable && !fixed && !Keep; 
	}
	
	/**
	 * Check if the point depends on itself and no other object.
	 * Such points are moveable, even if they are fixed.
	 * @return
	 */
	public boolean dependsOnItselfOnly ()
	{	boolean res=false;
		Enumeration e=depending();
		while (e.hasMoreElements())
		{	if ((ConstructionObject)e.nextElement()==this)
			{	res=true;
				break;
			}
		}
		e=depending();
		while (e.hasMoreElements())
		{	if ((ConstructionObject)e.nextElement()!=this)
			{	res=false;
				break;
			}
		}
		return res;
	}
	
	public boolean dependsOnParametersOnly ()
	{	Enumeration e=depending();
		while (e.hasMoreElements())
		{	if (!((ConstructionObject)e.nextElement()).isParameter())
				return false;
		}
		return true;
	}
	
	/**
	@return o can move this point (moveable and not moved by something else)
	*/
	public boolean moveableBy (ConstructionObject o)
	{	if (Bound!=null) return false;
		return moveable() && (MovedBy==null || MovedBy==o); 
	}
	
	public boolean moveablePoint ()
	{	if (Bound!=null) return true;
		return Moveable && !Keep; 
	}

	public boolean setBound (String name)
	{	if (name.equals(""))
		{	Bound=null;
			setFixed(false);
			Later=false;
			return true;	
		}
		try
		{	Bound=null;
			ConstructionObject o=Cn.find(name);
			if (o instanceof PointonObject)
			{	Bound=o;
				Moveable=true;
				Fixed=false;
				KeepInside=false;
			}
			else if (o instanceof InsideObject)
			{	Bound=o;
				Moveable=true;
				Fixed=false;
				KeepInside=true;
			}
			else { return false; }
		}
		catch (Exception e) { return false; }
		if (Cn.before(this,Bound))
		{	Cn.needsOrdering();
			Cn.dovalidate();
		}
		updateText();
		return true;
	}	
	
	public void setBound (ConstructionObject bound)
	{	Bound=bound;
	}
	
	public ConstructionObject getBound () { return Bound; }

	public void setMoveable (boolean flag) { Moveable=flag; }
	public boolean fixed () { return Fixed; }

	public void setFixed (boolean flag)
	{	Fixed=flag; 
		if (!Fixed) EX=EY=null;
		updateText();
	}
	
	public void setFixed (String x, String y)
	{	Fixed=true;
		EX=new Expression(x,getConstruction(),this);
		EY=new Expression(y,getConstruction(),this);
		updateText();
	}
	
	public void move (double x, double y)
	{	X=x; Y=y;
		AlphaValid=false;
	}
	
	public void setXY (double x, double y)
	{	X=x; Y=y;
	}
	
	public void setA (double alpha)
	{	Alpha=alpha;
	}
	
	public void project (ConstructionObject o)
	{	if (!(o instanceof PointonObject)) return;
		((PointonObject)o).project(this);
		if (UseAlpha) AlphaValid=true;
	}
	
	public void project (ConstructionObject o, double alpha)
	{	((PointonObject)o).project(this,alpha);
	}
	
	public void edit (ZirkelCanvas zc)
	{	ObjectEditDialog d=new PointEditDialog(zc,this);
		d.setVisible(true);
		zc.repaint();
		if ((EX!=null && !EX.isValid()))
		{	Frame F=zc.getFrame();
			Warning w=new Warning(F,EX.getErrorText(),
				Zirkel.name("warning"),true);
			w.center(F);
			w.setVisible(true);
		}
		else if ((EY!=null && !EY.isValid()))
		{	Frame F=zc.getFrame();
			Warning w=new Warning(F,EY.getErrorText(),
				Zirkel.name("warning"),true);
			w.center(F);
			w.setVisible(true);
		}
		validate();
		if (d.wantsMore())
		{	new EditConditionals(zc.getFrame(),this);
			validate();
		}
	}

	public void printArgs (XmlWriter xml)
	{	updateText();
		if (Bound!=null) 
		{	xml.printArg("on",Bound.getName());
			if (KeepInside) xml.printArg("inside","true");
		}
		if (Fixed && EX!=null) 
		{	xml.printArg("x",EX.toString());
			xml.printArg("actx",""+X);
		}
		else
		{	if (Bound!=null && AlphaValid && UseAlpha)
				xml.printArg("alpha",""+Alpha);
			xml.printArg("x",""+X);
		}
		if (Fixed && EY!=null)
		{	xml.printArg("y",EY.toString());
			xml.printArg("acty",""+Y);
		}
		else
			xml.printArg("y",""+Y);
		printType(xml);
		if (Fixed) xml.printArg("fixed","true");
		if (Increment>1e-4) xml.printArg("increment",""+getIncrement());
	}
	
	public void printType (XmlWriter xml)
	{	if (Type!=0)
		{	switch (Type)
			{	case DIAMOND : xml.printArg("shape","diamond"); break;
				case CIRCLE : xml.printArg("shape","circle"); break;
				case DOT : xml.printArg("shape","dot"); break;
				case CROSS : xml.printArg("shape","cross"); break;
				case DCROSS : xml.printArg("shape","dcross"); break;
			}
		}

	}
	
	public int getType () { return Type; }
	public void setType (int type) { Type=type; }
	
	public void movedBy (ConstructionObject o)
	{	MovedBy=o;
	}

	public boolean equals (ConstructionObject o)
	{	if (!(o instanceof PointObject) || !o.valid()) return false;
		PointObject p=(PointObject)o;
		return equals(X,p.X) && equals(Y,p.Y);
	}
	
	public String getEX ()
	{	if (EX!=null) return EX.toString();
		else return ""+round(X);
	}


	public String getEY ()
	{	if (EY!=null) return EY.toString();
		else return ""+round(Y);
	}
	
	public boolean isOn (ConstructionObject o)
	{	if (Bound!=null) return o==Bound;
		return o.contains(this);
	}

	public void translate ()
	{	if (Bound!=null) Bound=Bound.getTranslation();
		else if (Fixed)
		{	try
			{	setFixed(EX.toString(),EY.toString());
				EX.translate(); EY.translate();
			}
			catch (Exception e) {}
		}
	}
	
	public Enumeration depending ()
	{	super.depending();
		if (Bound!=null)
		{	DL.add(Bound);
		}
		if (Fixed)
		{	if (EX!=null) EX.addDep(this);
			if (EY!=null) EY.addDep(this);
		}
		return DL.elements();
		
	}
	
	public void snap (ZirkelCanvas zc)
	{	double d=zc.getGridSize()/2;
		X=Math.round(X/d)*d;
		Y=Math.round(Y/d)*d;
		updateText();
	}

	public void updateCircleDep ()
	{	if (Bound!=null && Bound instanceof PrimitiveCircleObject)
		{	((PrimitiveCircleObject)Bound).addDep(this);
		}
		if (Bound!=null && Bound instanceof PrimitiveLineObject)
		{	((PrimitiveLineObject)Bound).addDep(this);
		}
	}

	public boolean isPointOn ()
	{	return Bound!=null;
	}
	
	public void setLaterBind (String s)
	{	LaterBind=s;
	}
	
	public void setInside (boolean flag)
	{	KeepInside=flag;
	}
	
	public boolean isInside ()
	{	return KeepInside;
	}
	
	public void laterBind (Construction c)
	{	if (LaterBind.equals("")) return;
		ConstructionObject o=c.find(LaterBind);
		if (o!=null && ((o instanceof PointonObject) || (o instanceof InsideObject)))
		{	Bound=o;
			updateText();
			validate();
		}
		LaterBind="";
	}
	
	public void setAlpha (double alpha)
	{	Alpha=alpha; AlphaValid=true;
	}
	
	public void setUseAlpha (boolean flag)
	{	UseAlpha=flag;
	}
	
	public boolean useAlpha ()
	{	return UseAlpha;
	}
	
	public double getAlpha ()
	{	return Alpha;	
	}
	
	public void round ()
	{	move(round(X,ZirkelCanvas.LengthsFactor),
			round(Y,ZirkelCanvas.LengthsFactor));
	}
	
	/**
	 * For bounded points.
	 */
	public void setKeepClose (double x, double y)
	{	KeepClose=true;
		XcOffset=x-X; YcOffset=y-Y;
	}
	public boolean canKeepClose ()
	{	return true;
	}

	public boolean dontUpdate () { return DontUpdate; }
	public void dontUpdate (boolean flag) { DontUpdate=flag; }

	/**
	 * Overwrite setting of default things for macro targets.
	 */
	public void setTargetDefaults ()
	{	super.setTargetDefaults();
		Type=Cn.DefaultType;
	}
	
	public void startDrag(double x, double y) 
	{}
	
	public void dragTo (double x, double y) 
	{	move(x,y);
	}
	
	public void setIncrement (double inc)
	{	Increment=inc;
	}
	
	public double getIncrement ()
	{	return Increment;
	}
}
