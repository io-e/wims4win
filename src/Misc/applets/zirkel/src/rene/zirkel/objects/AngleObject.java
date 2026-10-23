package rene.zirkel.objects;

// file: Circle3Object.java

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

class AngleEditDialog extends ObjectEditDialog
	implements IconBarListener
{	TextField Length;
	Checkbox Fixed;
	IconBar SizeIB;
	
	public AngleEditDialog (Frame f, AngleObject o)
	{	super(f,Zirkel.name("edit.angle.title"),o,"angle");
	}
	
	public void addFirst (Panel P)
	{	AngleObject o=(AngleObject)O;
		Length=new TextFieldAction(this,"Length",o.getE(),30);
		P.add(new MyLabel(Zirkel.name("edit.angle.length")));
		P.add(Length);
		if (o.canFix())
		{	Fixed=new Checkbox("");	
			Fixed.setState(o.fixed());
		}
		else Length.setEditable(false);
		if (Fixed!=null)
		{	P.add(new MyLabel(Zirkel.name("edit.fixed"))); P.add(Fixed);
		}
	}
	public void doAction (String o)
	{	if (o.equals("Length") && Fixed!=null)
		{	Fixed.setState(true);
			super.doAction("OK");
		}
		else super.doAction(o);
	}
	public void addSecond (Panel P)
	{	AngleObject p=(AngleObject)O;
	
		SizeIB=new IconBar(F);
		SizeIB.addToggleGroupLeft("angle",5);
		SizeIB.toggle("angle",p.getDisplaySize());
		SizeIB.addSeparatorLeft();
		SizeIB.addOnOffLeft("filled");
		SizeIB.setState("filled",p.isFilled());
		SizeIB.addOnOffLeft("obtuse");
		SizeIB.setState("obtuse",p.getObtuse());
		P.add(new MyLabel("")); P.add(SizeIB);
		SizeIB.setIconBarListener(this);
	}
	public void iconPressed (String o)
	{	if (o.equals("filled"))
		{	if (SizeIB.getState("filled"))
			{	IB.setState("isback",true);
				ThicknessIB.setEnabled("solid",true);
			}
			else
			{	IB.setState("isback",false);
				ThicknessIB.setState("solid",false);
				ThicknessIB.setEnabled("solid",false);
			}
		}
		super.iconPressed(o);
	}
	public void setAction ()
	{	AngleObject o=(AngleObject)O;
		if (Fixed!=null && Fixed.getState()==true)
		{	o.setFixed(Length.getText());
		}
		if (Fixed!=null && Fixed.getState()==false)
			o.setFixed(false);
		o.setDisplaySize(SizeIB.getToggleState("angle"));
		o.setFilled(SizeIB.getState("filled"));
		o.setObtuse(SizeIB.getState("obtuse"));
		Global.setParameter("unit.angle",Unit.getText());
	}
	public void focusGained (FocusEvent e)
	{	if (Fixed!=null && Fixed.getState()) Length.requestFocus();
		else super.focusGained(e);
	}
}

public class AngleObject extends ConstructionObject
	implements InsideObject
{	protected PointObject P1,P2,P3;
	static Count N=new Count();
	double A,A1,A2;
	double X,Y;
	boolean Fixed;
	Expression E;
	boolean Filled=false;
	final static double LabelScale=0.66;
	
	public static final int NORMALSIZE=1,SMALL=0,LARGER=2,LARGE=3,RECT=4;
	protected int DisplaySize=NORMALSIZE;
	
	public AngleObject (Construction c,
		PointObject p1, PointObject p2, PointObject p3)
	{	super(c);
		P1=p1; P2=p2; P3=p3;
		validate();
		setColor(ColorIndex);
		updateText();
		Unit=Global.getParameter("unit.angle","°");
	}
	
	public AngleObject (Construction c)
		{	super(c);
		}
		
	public String getTag () { return "Angle"; }
	public int getN () { return N.next(); }
	
	public void updateText ()
	{	if (!Fixed || E==null)
			setText(text3(Zirkel.name("text.angle"),
				P1.getName(),P2.getName(),P3.getName()));
		else
			setText(text4(Zirkel.name("text.angle.fixed"),
					P1.getName(),P2.getName(),P3.getName(),"\""+E.toString()+"\""));
	}
	
	public String getDisplayValue ()
	{	if (ZirkelCanvas.AnglesFactor<=2) return ""+(int)(A/Math.PI*180+0.5);
		else return ""+round(A/Math.PI*180,ZirkelCanvas.AnglesFactor);
	}
	
	public boolean nearto (int x, int y, ZirkelCanvas zc)
	{	if (!displays(zc)) return false;
		double dx=zc.x(x)-X,dy=zc.y(y)-Y;
		double size=zc.dx((int)zc.selectionSize());
		double rd=getDisplaySize(zc),r=Math.sqrt(dx*dx+dy*dy);
		boolean near;
		Value=Math.abs(r-rd);
		if (Filled || DisplaySize==RECT)
		{	near=(r<rd+size);
			if (near) Value=0;
		}
		else near=(Math.abs(r-rd)<size);
		if (!near) return false;
		if (rd<size) return near;
		double a=Math.atan2(dy,dx);
		if (a<0) a+=2*Math.PI;
		double c=0.05;
		if (a<A1) a+=2*Math.PI;
		return a>A1-c && a<A1+A+c;
	}
	
	public void validate ()
	{	if (P1==null) return;
		if (!P1.valid() || !P2.valid() || !P3.valid()) { Valid=false; return; }
		else
		{	X=P2.getX(); Y=P2.getY();
			double dx=P1.getX()-X,dy=P1.getY()-Y;
			if (Math.sqrt(dx*dx+dy*dy)<1e-9)
			{	Valid=false; return;
			}
			A1=Math.atan2(dy,dx);
			if (A1<0) A1+=2*Math.PI;
			dx=P3.getX()-X; dy=P3.getY()-Y;
			if (Math.sqrt(dx*dx+dy*dy)<1e-9)
			{	Valid=false; return;
			}
			A2=Math.atan2(dy,dx);
			if (A2<0) A2+=2*Math.PI;
			A=A2-A1;
			if (A<0) A=A+2*Math.PI;
			Valid=true;
			if (Fixed)
			{	double FixedAlpha=0;
				try
				{	FixedAlpha=E.getValue()/180*Math.PI;
				}
				catch (Exception e)
				{	return;
				}
				if (P3.moveableBy(this))
				{	dx=P3.getX()-X; dy=P3.getY()-Y;
					double r=Math.sqrt(dx*dx+dy*dy);
					if (r<1e-9) r=1e-9;
					P3.move(X+Math.cos(A1+FixedAlpha)*r,
						Y+Math.sin(A1+FixedAlpha)*r);
					A2=A1+FixedAlpha;
				}
				else Fixed=false;
				if (Fixed)
				{	A=FixedAlpha;
					P3.movedBy(this);
					P1.movedBy(this);
				}
			}
			else if (!Obtuse && A>Math.PI)
			{	A1=A2;
				A=2*Math.PI-A;
				A2=A1+A;
			}
		}
	}

	double x[]=new double[4],y[]=new double[4];
	
	public void paint (MyGraphics g, ZirkelCanvas zc)
	{	if (!Valid  || mustHide(zc)) return;
		double R=zc.col(getDisplaySize(zc))-zc.col(0);
		double c1=zc.col(X)-R,r1=zc.row(Y)-R;
		// paint:
		double DA=(A2-A1)/Math.PI*180;
		if (DA<0) DA+=360;
		else if (DA>=360) DA-=360;
		if (visible(zc))
		{	if (isStrongSelected() && g instanceof MyGraphics13)
			{	((MyGraphics13)g).drawMarkerArc(c1+R,r1+R,R,A1/Math.PI*180,DA);
			}
			if (Filled)
			{	if (DisplaySize==RECT)
				{	double dx1=Math.cos(A1),dy1=Math.sin(A1),
					dx2=Math.cos(A1+DA/180*Math.PI),dy2=Math.sin(A1+DA/180*Math.PI);
					double dx3=dx1+dx2,dy3=dy1+dy2;
					if (DA>180)
					{	dx3=-dx3; dy3=-dy3;
					}
					if (Selected||getColorType()!=THIN)
					{	g.setColor(this);
						g.drawLine(c1+R+R*dx1,r1+R-R*dy1,c1+R+R*dx3,r1+R-R*dy3);
						g.drawLine(c1+R+R*dx3,r1+R-R*dy3,c1+R+R*dx2,r1+R-R*dy2);
					}
					x[0]=c1+R; y[0]=r1+R;
					x[1]=c1+R+R*dx1; y[1]=r1+R-R*dy1;
					x[2]=c1+R+R*dx3; y[2]=r1+R-R*dy3;
					x[3]=c1+R+R*dx2; y[3]=r1+R-R*dy2;
					g.fillPolygon(x,y,4,false,getColorType()!=THICK,this);
				}
				else 
					g.fillArc(c1,r1,2*R,2*R,A1/Math.PI*180,DA,
							Selected||getColorType()!=THIN,getColorType()!=THICK,true,this);
			}
			else
			{	g.setColor(this);
				if (DisplaySize==RECT)
				{	double dx1=Math.cos(A1),dy1=Math.sin(A1),
						dx2=Math.cos(A1+DA/180*Math.PI),dy2=Math.sin(A1+DA/180*Math.PI);
					g.drawLine(c1+R+R*dx1,r1+R-R*dy1,c1+R+R*(dx1+dx2),r1+R-R*(dy1+dy2));
					g.drawLine(c1+R+R*(dx1+dx2),r1+R-R*(dy1+dy2),c1+R+R*dx2,r1+R-R*dy2);
				}
				else 
					g.drawCircleArc(c1+R,r1+R,R,A1/Math.PI*180,DA,this);
			}
		}
		String s=translateToUnicode(getDisplayText());
		if (!s.equals(""))
		{	g.setLabelColor(this);
			setFont(g);
			DisplaysText=true;
			double dx=Math.cos(A1+A/2),dy=Math.sin(A1+A/2);
			if (s.equals("90"+getUnit()) || Name.startsWith("."))
			{	if (KeepClose)
				{	double d=Math.sqrt(XcOffset*XcOffset+YcOffset*YcOffset);
					TX1=zc.col(X+d*dx)-3; 
					TY1=zc.row(Y+d*dy)-3;
					TX2=TX1+9;
					TY2=TY1+9;
					g.drawRect(zc.col(X+d*dx)-1,
						zc.row(Y+d*dy)-1,3,3);
				}
				else
				{	TX1=zc.col(X+zc.dx(R*LabelScale)*dx+XcOffset)-3; 
					TY1=zc.row(Y+zc.dy(R*LabelScale)*dy+YcOffset)-3;
					TX2=TX1+9;
					TY2=TY1+9;
					g.drawRect(zc.col(X+zc.dx(R*LabelScale)*dx+XcOffset)-1,
						zc.row(Y+zc.dy(R*LabelScale)*dy+YcOffset)-1,3,3);
				}
			}
			else
			{	if (KeepClose)
				{	double d=Math.sqrt(XcOffset*XcOffset+YcOffset*YcOffset);
					drawCenteredLabel(g,s,zc,
						X+d*dx,Y+d*dy,0,0);
				}
				else
					drawCenteredLabel(g,s,zc,
						X+zc.dx(R*LabelScale)*dx,Y+zc.dy(R*LabelScale)*dy,
						XcOffset,YcOffset);
			}
		}
	}

	public boolean canKeepClose ()
	{	return true;
	}
	
	public void setKeepClose (double x, double y)
	{	KeepClose=true;
		XcOffset=x-X;
		YcOffset=y-Y;
	}

	double getDisplaySize (ZirkelCanvas zc)
	{	double R=zc.dx((int)(12*zc.pointSize()));
		if (DisplaySize==SMALL || DisplaySize==RECT) R/=2;
		else if (DisplaySize==LARGER) R*=2;
		else if (DisplaySize==LARGE)
		{	double dx=P1.getX()-X,dy=P1.getY()-Y;
			R=Math.sqrt(dx*dx+dy*dy);
		}
		return R;
	}

	public double getLength () { return A; }
	public boolean fixed () { return Fixed; }
	public void setFixed (boolean flag)
	{	Fixed=flag;
		updateText();
	}
	public void setFixed (String s)
	{	Fixed=true;
		E=new Expression(s,getConstruction(),this);
		updateText();
	}

	public boolean canFix ()
	{	return P3.moveableBy(this);
	}

	public void printArgs (XmlWriter xml)
	{	if (P1!=null)
		{	xml.printArg("first",P1.getName());
			xml.printArg("root",P2.getName());
			xml.printArg("second",P3.getName());
		}
		if (DisplaySize==SMALL) xml.printArg("display","small");
		if (DisplaySize==LARGE) xml.printArg("display","large");
		if (DisplaySize==LARGER) xml.printArg("display","larger");
		if (DisplaySize==RECT) xml.printArg("display","rectangle");
		if (Filled) xml.printArg("filled","true");
		if (Fixed && E!=null) xml.printArg("fixed",E.toString());
		if (!Obtuse) xml.printArg("acute","true");
		super.printArgs(xml);
	}	

	public void edit (ZirkelCanvas zc)
	{	ObjectEditDialog d=new AngleEditDialog(zc.getFrame(),this);
		d.setVisible(true);
		zc.repaint();
		if ((E!=null && !E.isValid()))
		{	Frame F=zc.getFrame();
			Warning w=new Warning(F,E.getErrorText(),
				Zirkel.name("warning"),true);
			w.center(F);
			w.setVisible(true);
		}
		if (d.wantsMore())
		{	new EditConditionals(zc.getFrame(),this);
			validate();
		}
	}

	public void setDisplaySize (int i) { DisplaySize=i; }
	public int getDisplaySize () { return DisplaySize; }

	public Enumeration depending ()
	{	super.depending();
		if (P1==null) return DL.elements();
		if (!Fixed) return depset(P1,P2,P3);
		else
		{	depset(P1,P2,P3);
			Enumeration e=E.getDepList().elements();
			while (e.hasMoreElements())
			{	DL.add((ConstructionObject)e.nextElement());
			}
			return DL.elements();
		}
	}

	public boolean equals (ConstructionObject o)
	{	if (!(o instanceof AngleObject) || !o.valid()) return false;
		AngleObject l=(AngleObject)o;
		return equals(X,l.X) && equals(Y,l.Y) &&
			equals(A1,l.A1) && equals(A2,l.A2);
	}

	public static char Translation[]=
	{	'a','\u03B1','A','\u0391',
		'b','\u03B2','B','\u0392',
		'c','\u03B3','C','\u0393',
		'd','\u03B4','D','\u0394',
		'e','\u03B5','E','\u0395',
		'f','\u03D5','F','\u03A6',
		'g','\u03B3','G','\u0393',
		'h','\u03B7','H','\u0397',
		'i','\u03B9','I','\u0399',
		'k','\u03BA','K','\u039A',
		'l','\u03BB','L','\u039B',
		'm','\u03BC','M','\u039C',
		'n','\u03BD','N','\u039D',
		'o','\u03BF','O','\u03A9',
		'p','\u03C0','P','\u03A0',
		'q','\u03C7','Q','\u03A7',
		'r','\u03C1','R','\u03A1',
		's','\u03C3','S','\u03A3',
		't','\u03C4','T','\u03A4',
		'u','\u03C5','U','\u03A5',
		'v','\u03C8','V','\u03A8',
		'w','\u03C9','W','\u03A9',
		'x','\u03BE','X','\u039E',
		'y','\u03C7','Y','\u03A7',
		'z','\u03B6','Z','\u0396',		
	};

	public static String translateToUnicode (String s)
	{	if (s.startsWith("$")) return s;
		if (s.indexOf('\\')<0) return s;
		StringBuffer b=new StringBuffer();
		for (int i=0; i<s.length(); i++)
		{	char c=s.charAt(i);
			if (c!='\\') b.append(c);
			else
			{	i++;
				if (i<s.length())
				{	c=s.charAt(i);
					if (c=='0')
					{	int n=0;
						i++;
						while (i<s.length())
						{	char ch=s.charAt(i);
							if (ch>='0' && ch<='9')
							{	n=n*16+(int)(ch-'0');
							}
							else if (ch>='A' && ch<='F')
							{	n=n*16+(int)(ch-'A'+10);
							}
							else break;
							i++;
						}
						if (n>0)
						{	c=(char)n;
							b.append(c);
						}
						i--; continue;
					}
					int j=0;
					for (j=0; j<Translation.length; j+=2)
					{	if (Translation[j]==c)
						{	b.append(Translation[j+1]);
							break;
						}	
					}
					if (j>=Translation.length) b.append(c);
				}
			}
		}
		return b.toString();
	}

	public void translate ()
	{	P1=(PointObject)P1.getTranslation();
		P2=(PointObject)P2.getTranslation();
		P3=(PointObject)P3.getTranslation();
		if (Fixed)
		{	try
			{	setFixed(E.toString());
				E.translate();
			}
			catch (Exception e) { Fixed=false; }
		}
	}

	public String getE ()
	{	if (Fixed && E!=null) return E.toString();
		else return ""+round(A/Math.PI*180);
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

	public boolean maybeTransparent ()
	{	return true;
	}	

	public boolean isFilledForSelect ()
	{	return false;
	}

	public double containsInside (PointObject P) 
	{	double dx=P.getX()-X,dy=P.getY()-Y;
		double a=Math.atan2(dy,dx);
		if (a<0) a+=2*Math.PI;
		if (a<A1) a+=2*Math.PI;
		double c=1e-5;
		if (a>A1 && a<A1+A) return 1;
		else if (a>A1-c && a<A1+A+c) return 0.5;
		return 0;
	}

	public boolean keepInside(PointObject P) 
	{	if (containsInside(P)>0) return true;
		double x=P.getX(),y=P.getY();
		double x1=P2.getX(),y1=P2.getY();
		double xmin=x1,ymin=y1,dmin=1e20;
		double x2=P1.getX(),y2=P1.getY();
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
		x2=P3.getX(); y2=P3.getY();
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
