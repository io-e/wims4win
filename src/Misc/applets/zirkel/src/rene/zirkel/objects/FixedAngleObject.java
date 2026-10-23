package rene.zirkel.objects;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import rene.gui.*;
import rene.dialogs.*;
import rene.util.xml.*;
import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelCanvas;
import rene.zirkel.construction.Construction;
import rene.zirkel.construction.ConstructionException;
import rene.zirkel.construction.Count;
import rene.zirkel.dialogs.EditConditionals;
import rene.zirkel.dialogs.ObjectEditDialog;
import rene.zirkel.expression.Expression;
import rene.zirkel.expression.InvalidException;
import rene.zirkel.graphics.MyGraphics;
import rene.zirkel.graphics.MyGraphics13;

class FixedAngleEditDialog extends ObjectEditDialog
{	TextField Length;
	Checkbox Fixed;
	IconBar SizeIB;
	ZirkelCanvas ZC;
	Button SetButton;
	
	public FixedAngleEditDialog (Frame f, FixedAngleObject o,
		ZirkelCanvas zc)
	{	super(f,Zirkel.name("edit.angle.title"),o,"fixedangle");
		ZC=zc;
	}
	
	public void addFirst (Panel P)
	{	FixedAngleObject o=(FixedAngleObject)O;
		Length=new TextFieldAction(this,"Length",o.getE());
		P.add(new MyLabel(Zirkel.name("edit.angle.length")));
		P.add(Length);
		Fixed=new Checkbox("");	
		Fixed.setState(!o.isDragable());
		P.add(new MyLabel(Zirkel.name("edit.fixed"))); P.add(Fixed);
	}

	public void doAction (String o)
	{	if (o.equals("Length") && Fixed!=null)
		{	Fixed.setState(true);
			super.doAction("OK");
		}
		else if (o.equals("Set"))
		{	ZC.set((FixedAngleObject)O);
			super.doAction("OK");
		}
		else super.doAction(o);
	}

	public void addSecond (Panel P)
	{	FixedAngleObject p=(FixedAngleObject)O;	
		SizeIB=new IconBar(F);
		SizeIB.addToggleGroupLeft("angle",5);
		SizeIB.toggle("angle",p.getDisplaySize());
		SizeIB.addOnOffLeft("filled");
		SizeIB.setState("filled",p.isFilled());
		SizeIB.addOnOffLeft("obtuse");
		SizeIB.setState("obtuse",p.getObtuse());
		SizeIB.addOnOffLeft("inverse");
		SizeIB.setState("inverse",p.getInverse());
		SizeIB.addOnOffLeft("anglereduce");
		SizeIB.setState("anglereduce",p.isReduced());
		P.add(new MyLabel("")); P.add(SizeIB);
		SizeIB.setIconBarListener(this);
	}
	
	public void addButton (Panel P)
	{	SetButton=new ButtonAction(this,
			Zirkel.name("edit.fixedangle.set"),"Set");
		P.add(SetButton);
	}
	
	public void iconPressed (String o)
	{	if (o.equals("filled"))
		{	if (SizeIB.getState("filled"))
			{	IB.setState("isback",true);
			}
		}
		super.iconPressed(o);
	}
	
	public void setAction ()
	{	FixedAngleObject o=(FixedAngleObject)O;
		o.setObtuse(SizeIB.getState("obtuse"));
		o.setInverse(SizeIB.getState("inverse"));
		o.setReduced(SizeIB.getState("anglereduce"));
		o.setEditFixed(Length.getText());
		o.setDisplaySize(SizeIB.getToggleState("angle"));
		o.setFilled(SizeIB.getState("filled"));
		o.setDragable(!Fixed.getState());
	}

	public void focusGained (FocusEvent e)
	{	if (Fixed!=null && Fixed.getState()) Length.requestFocus();
		else super.focusGained(e);
	}
}

public class FixedAngleObject extends PrimitiveLineObject
	implements MoveableObject, SimulationObject, InsideObject
{	protected PointObject P2;
	static Count N=new Count();
	double A,A1,A2,AA;
	Expression E;
	boolean Filled=false;
	boolean Inverse=false;
	boolean EditAborted=false;
	boolean Dragable=false;
	boolean Reduced=false;
	
	public static final int NORMALSIZE=1,SMALL=0,LARGER=2,LARGE=3,RECT=4;
	protected int DisplaySize=NORMALSIZE;
	
	public FixedAngleObject (Construction c, 
		PointObject p1, PointObject p2, double x, double y)
	{	super(c);
		P1=p1; P2=p2;
		init(c,x,y);
		Unit=Global.getParameter("unit.angle","°");
	}
	
	public void init (Construction c, double x, double y, boolean invert)
	{	double dx=P1.getX()-P2.getX(),dy=P1.getY()-P2.getY();
		A1=Math.atan2(dy,dx);
		dx=x-P2.getX(); dy=y-P2.getY();
		A2=Math.atan2(dy,dx);
		A=A2-A1;
		if (A<0) A+=2*Math.PI;
		else if (A>2*Math.PI) A-=2*Math.PI;
		if (Inverse && Obtuse)
		{	A=2*Math.PI-A;
		}
		if (invert && !Obtuse)
		{	if (A>Math.PI)
			{	A=2*Math.PI-A; Inverse=true;
			}
			else Inverse=false;
		}
		E=new Expression(""+round(A/Math.PI*180,ZirkelCanvas.EditFactor),
			c,this);
		validate();
		setColor(ColorIndex);
		updateText();
	}
	
	public void init (Construction c, double x, double y)
	{	init(c,x,y,true);
	}

	public void paint (MyGraphics g, ZirkelCanvas zc)
	{	if (!Valid || mustHide(zc)) return;
		//compute middle of the screen:
		double xm=(zc.minX()+zc.maxX())/2,ym=(zc.minY()+zc.maxY())/2;
		// compute distance from middle to line:
		double d=(xm-X1)*DY-(ym-Y1)*DX;
		// compute point with minimal distance
		double x=xm-d*DY,y=ym+d*DX;
		// compute size of the screen
		double a=Math.max(zc.maxX()-zc.minX(),zc.maxY()-zc.minY());
		if (Math.abs(d)>a) return;
		// compute distance from closest point to source
		double b=(x-X1)*DX+(y-Y1)*DY;
		// compute the two visible endpoints
		double k1=b-a,k2=b+a;
		if (k1<0) k1=0;
		if (k1>=k2) return;
		double c1=zc.col(X1+k1*DX),c2=zc.col(X1+k2*DX),
			r1=zc.row(Y1+k1*DY),r2=zc.row(Y1+k2*DY);
		// paint:
		g.setColor(this);
		if (!Reduced) g.drawLine(c1,r1,c2,r2,this);
		double R=zc.col(getDisplaySize(zc))-zc.col(0);
		c1=zc.col(X1)-R; r1=zc.row(Y1)-R;
		String s=AngleObject.translateToUnicode(getDisplayText());
		double DA=(int)(A/Math.PI*180);
		if (DA<0) DA+=360;
		else if (DA>=360) DA-=360;
		if (isStrongSelected() && g instanceof MyGraphics13)
		{	((MyGraphics13)g).drawMarkerArc(c1+R,r1+R,R,A1/Math.PI*180,DA);
		}
		g.setColor(this);
		if (Filled)
		{	g.fillArc(c1,r1,2*R+1,2*R+1,A1/Math.PI*180,DA,
				Selected||getColorType()==NORMAL,getColorType()!=THICK,true,this);
		}
		else 
		{	if (DisplaySize==RECT)
			{	double dx1=Math.cos(A1),dy1=Math.sin(A1),
				dx2=Math.cos(A1+DA/180*Math.PI),dy2=Math.sin(A1+DA/180*Math.PI);
				g.drawLine(c1+R+R*dx1,r1+R-R*dy1,c1+R+R*(dx1+dx2),r1+R-R*(dy1+dy2));
				g.drawLine(c1+R+R*(dx1+dx2),r1+R-R*(dy1+dy2),c1+R+R*dx2,r1+R-R*dy2);
			}
			else 
				g.drawCircleArc(c1+R,r1+R,R,A1/Math.PI*180,DA,this);
		}
		if (!s.equals(""))
		{	g.setLabelColor(this);
			setFont(g);
			DisplaysText=true;
			double dx=Math.cos(A1+A/2),dy=Math.sin(A1+A/2);
			if (s.equals("90"+getUnit()) || Name.startsWith("."))
			{	if (KeepClose)
				{	double dof=Math.sqrt(XcOffset*XcOffset+YcOffset*YcOffset);
					TX1=zc.col(X1+dof*dx)-3; 
					TY1=zc.row(Y1+dof*dy)-3;
					TX2=TX1+9;
					TY2=TY1+9;
					g.drawRect(zc.col(X1+dof*dx)-1,
						zc.row(Y1+dof*dy)-1,3,3);
				}
				else
				{	TX1=zc.col(X1+zc.dx(R*AngleObject.LabelScale)*dx+XcOffset)-3; 
					TY1=zc.row(Y1+zc.dy(R*AngleObject.LabelScale)*dy+YcOffset)-3;
					TX2=TX1+9;
					TY2=TY1+9;
					g.drawRect(zc.col(X1+zc.dx(R*AngleObject.LabelScale)*dx+XcOffset)-1,
						zc.row(Y1+zc.dy(R*AngleObject.LabelScale)*dy+YcOffset)-1,3,3);
				}
			}
			else
			{	if (KeepClose)
				{	double dof=Math.sqrt(XcOffset*XcOffset+YcOffset*YcOffset);
					drawCenteredLabel(g,s,zc,
						X1+dof*dx,Y1+dof*dy,0,0);
				}
				else
					drawCenteredLabel(g,s,zc,
						X1+zc.dx(R*AngleObject.LabelScale)*dx,
							Y1+zc.dy(R*AngleObject.LabelScale)*dy,
						XcOffset,YcOffset);
			}
		}
	}

	public boolean canKeepClose ()
	{	return true;
	}
	
	public void setKeepClose (double x, double y)
	{	KeepClose=true;
		XcOffset=x-X1;
		YcOffset=y-Y1;
	}

	double getDisplaySize (ZirkelCanvas zc)
	{	double R=zc.dx((int)(12*zc.pointSize()));
		if (DisplaySize==SMALL || DisplaySize==RECT) R/=2;
		else if (DisplaySize==LARGER) R*=2;
		else if (DisplaySize==LARGE)
		{	double dx=P1.getX()-X1,dy=P1.getY()-Y1;
			R=Math.sqrt(dx*dx+dy*dy);
		}
		return R;
	}

	public String getTag () { return "Angle"; }
	public int getN () { return N.next(); }
	
	public void updateText ()
	{	if (E==null || !E.isValid()) return;
		setText(text3(Zirkel.name("text.fixedangle"),
			P1.getName(),P2.getName(),E.toString()));
	}
	
	public String getDisplayValue ()
	{	if (ZirkelCanvas.AnglesFactor<=2) return ""+(int)(A/Math.PI*180+0.5);
		else return ""+round(A/Math.PI*180,ZirkelCanvas.AnglesFactor);
	}
	
	public void validate ()
	{	if (!P1.valid() || !P2.valid())
		{ 	Valid=false; 
			return; 
		}
		else
		{	X1=P2.getX(); Y1=P2.getY();
			double dx=P1.getX()-X1,dy=P1.getY()-Y1;
			if (Math.sqrt(dx*dx+dy*dy)<1e-9)
			{	Valid=false; return;
			}
			A1=Math.atan2(dy,dx);
			boolean Negative=false;
			try
			{	if (E!=null && E.isValid()) A=E.getValue()/180*Math.PI;
				if (Obtuse) 
				{	if (Inverse) A=-A;
				}
				else
				{	if (Inverse && Math.sin(A)>0) A=-A;
					if (!Inverse && Math.sin(A)<0) A=-A;
				}
				if (A<0) Negative=true;
				while (A<0) A=A+2*Math.PI;
				while (A>=2*Math.PI) A=A-2*Math.PI;
				A2=A1+A;
				DX=Math.cos(A2); DY=Math.sin(A2);
				AA=A;
			}
			catch (Exception e)
			{	Valid=false; return;
			}
			if (!Obtuse && A>Math.PI)
			{	A1=A2;
				A=2*Math.PI-A;
				A2=A1+A;
			}
			else if (Obtuse && Negative)
			{	A1=A2;
				A=2*Math.PI-A;
				A2=A1+A;
			}
			Valid=true;
		}
	}
	
	public double getLength () { return A; }
	public void setFixed (String s)
	{	E=new Expression(s,getConstruction(),this);
		updateText();
	}
	
	public void round ()
	{	try
		{	setFixed(round(E.getValue(),ZirkelCanvas.AnglesFactor)+"");
			validate();
		}
		catch (Exception e) {}
	}

	public void setEditFixed (String s)
	{	E=new Expression(s,getConstruction(),this);
		updateText();
	}

	public boolean canFix ()
	{	return true;
	}

	public void printArgs (XmlWriter xml)
	{	xml.printArg("first",P1.getName());
		xml.printArg("root",P2.getName());
		if (DisplaySize==SMALL) xml.printArg("display","small");
		if (DisplaySize==LARGE) xml.printArg("display","large");
		if (DisplaySize==LARGER) xml.printArg("display","larger");
		if (DisplaySize==RECT) xml.printArg("display","rectangle");
		if (Filled) xml.printArg("filled","true");
		if (E.isValid()) xml.printArg("fixed",E.toString());
		else xml.printArg("fixed",""+A/Math.PI*180);
		if (!Obtuse) xml.printArg("acute","true");
		if (Inverse) xml.printArg("inverse","true");
		if (Reduced) xml.printArg("reduced","true");
		if (Dragable) xml.printArg("dragable","true");
		super.printArgs(xml);
	}	

	public void edit (ZirkelCanvas zc)
	{	ObjectEditDialog d;
		while (true)
		{	d=new FixedAngleEditDialog(zc.getFrame(),this,zc);
			d.setVisible(true);
			zc.repaint();
			EditAborted=false;
			if (d.isAborted())
			{	EditAborted=true;
				break;
			}
			else if (E!=null && !E.isValid())
			{	Frame F=zc.getFrame();
				Warning w=new Warning(F,E.getErrorText(),
					Zirkel.name("warning"),true);
				w.center(F);
				w.setVisible(true);
			}
			else break;
		}
		if (d.wantsMore())
		{	new EditConditionals(zc.getFrame(),this);
			validate();
		}
	}

	public boolean nearto (int c, int r, ZirkelCanvas zc)
	{	if (!Valid && P2.valid()) return P2.nearto(c,r,zc);
		if (!displays(zc)) return false;
		//compute point at c,r
		double x=zc.x(c),y=zc.y(r);
		// compute distance from x,y
		double d=(x-X1)*DY-(y-Y1)*DX;
		double e=(x-X1)*DX+(y-Y1)*DY;
		// scale in screen coordinates
		Value=Math.abs(zc.col(zc.minX()+d)-zc.col(zc.minX()));
		if (!Reduced && Value<zc.selectionSize() && e>0) return true;
		double dx=zc.x(c)-X1,dy=zc.y(r)-Y1;
		double size=zc.dx((int)zc.selectionSize());
		double rd=getDisplaySize(zc);
		double rr=Math.sqrt(dx*dx+dy*dy);
		boolean near;
		if (Filled || DisplaySize==RECT) near=(rr<rd+size);
		else near=(Math.abs(rr-rd)<size);
		if (!near) return false;
		if (rd<size) return near;
		double a=Math.atan2(dy,dx);
		if (a<0) a+=2*Math.PI;
		double cc=0.05;
		if (A1-cc<a && A2+cc>a) return true;
		a=a-2*Math.PI;
		if (A1-cc<a && A2+cc>a) return true;
		a=a+4*Math.PI;
		if (A1-cc<a && A2+cc>a) return true;
		return false;
	}
	
	public void setDisplaySize (int i) { DisplaySize=i; }
	public int getDisplaySize () { return DisplaySize; }

	public Enumeration depending ()
	{	super.depending();
		depset(P1,P2);
		Enumeration e=E.getDepList().elements();
		while (e.hasMoreElements())
		{	DL.add((ConstructionObject)e.nextElement());
		}
		return DL.elements();
	}

	public void translate ()
	{	P1=(PointObject)P1.getTranslation();
		P2=(PointObject)P2.getTranslation();
		try
		{	setFixed(E.toString());
			E.translate();
		}
		catch (Exception e) {}
	}

	public String getE ()
	{	return E.toString();
	}
	
	public double getValue ()
		throws ConstructionException
	{	if (!Valid) throw new InvalidException("exception.invalid");
		else return A/Math.PI*180;
	}

	public void setFilled (boolean flag)
	{	Filled=flag;
	}	
	public boolean isFilled ()
	{	return Filled;
	}
	public boolean isFilledForSelect ()
	{	return false;
	}

	public boolean contains (double x, double y)
	{	double a=(x-X1)*DX+(y-Y1)*DY;
		if (a<1e-9) return false;
		return true;
	}
	public double project (double x, double y)
	{	double h=super.project(x,y);
		if (h<0) return 0;
		return h;
	}

	public Enumeration points ()
	{	super.depending();
		return depset(P2);
	}
	
	public void move (double x, double y)
	{	init(getConstruction(),x,y,true);
	}
	
	public boolean moveable ()
	{	return Dragable; // E.isNumber();
	}
	
	public boolean isFixed ()
	{	return true;
	}
	
	public boolean getInverse ()
	{	return Inverse;
	}
	
	public void setInverse (boolean inverse)
	{	Inverse=inverse;
	}
	
	public boolean isEditAborted ()
	{	return EditAborted;
	}

	public boolean isDragable ()
	{	return Dragable;
	}
	
	public void setDragable (boolean f)
	{	Dragable=f; 
	}

	public boolean isReduced ()
	{	return Reduced;
	}
	
	public void setReduced (boolean f)
	{	Reduced=f; 
	}

	public boolean fixedByNumber ()
	{	return (E!=null && E.isNumber());
	}

	// For the simulate function:
	
	/**
	 * Set the simulation value, remember the old value.
	 */
	public void setSimulationValue (double x) 
	{	A=x/180*Math.PI;
		Expression OldE=E; 
		E=null;
		validate();
		E=OldE;
	}
	
	/**
	 * Reset the old value.
	 */
	public void resetSimulationValue () 
	{	validate();
	}

	public void startDrag(double x, double y) 
	{}

	public void dragTo (double x, double y) 
	{	move(x,y);
	}

	public double containsInside (PointObject P) 
	{	double dx=P.getX()-X1,dy=P.getY()-Y1;
		double a=Math.atan2(dy,dx);
		if (a<A1) a+=2*Math.PI;
		double c=1e-5;
		if (a>A1 && a<A1+A) return 1;
		else if (a>A1-c && a<A1+A+c) return 0.5;
		return 0;
	}

	public boolean keepInside (PointObject P) 
	{	if (containsInside(P)>0) return true;
		double x=P.getX(),y=P.getY();
		double x1=X1,y1=Y1;
		double xmin=x1,ymin=y1,dmin=1e20;
		double x2=X1+Math.cos(A1),y2=Y1+Math.sin(A1);
		double dx=x2-x1,dy=y2-y1;
		double r=dx*dx+dy*dy;
		double h=dx*(x-x1)/r+dy*(y-y1)/r;
		if (h<0) h=0;
		double xh=x1+h*dx,yh=y1+h*dy;
		double dist=Math.sqrt((x-xh)*(x-xh)+(y-yh)*(y-yh));
		if (dist<dmin)
		{	dmin=dist;
			xmin=xh; ymin=yh;
		}
		x2=X1+Math.cos(A2); y2=Y1+Math.sin(A2);
		dx=x2-x1; dy=y2-y1;
		r=dx*dx+dy*dy;
		h=dx*(x-x1)/r+dy*(y-y1)/r;
		if (h<0) h=0;
		xh=x1+h*dx; yh=y1+h*dy;
		dist=Math.sqrt((x-xh)*(x-xh)+(y-yh)*(y-yh));
		if (dist<dmin)
		{	dmin=dist;
			xmin=xh; ymin=yh;
		}
		P.move(xmin,ymin);
		return false;
	}	
}
