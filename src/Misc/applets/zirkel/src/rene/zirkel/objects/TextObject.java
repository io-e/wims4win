package rene.zirkel.objects;

// file: PointObject.java

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

import rene.gui.*;

import rene.util.xml.*;
import rene.zirkel.*;
import rene.zirkel.construction.*;
import rene.zirkel.dialogs.*;
import rene.zirkel.expression.*;
import rene.zirkel.graphics.*;

class TextObjectDialog extends HelpCloseDialog
{	TextArea A;
	TextObject TO;
	boolean Settings=false;
	boolean More;
	
	public TextObjectDialog (Frame f, TextObject o)
	{	super(f,Zirkel.name("edit.text.title"),true);
		TO=o;
		setLayout(new BorderLayout());
		A=new TextArea(25,60);
		A.setBackground(Global.Background);
		add("Center",new Panel3D(A));
		A.setText(o.getLines());
		Panel south=new MyPanel();
		south.add(new ButtonAction(this,Global.name("edit.more"),"More"));
		south.add(new ButtonAction(this,Global.name("ok"),"OK"));
		south.add(new ButtonAction(this,Global.name("close"),"Close"));
		south.add(new ButtonAction(this,Global.name("edit.text.settings"),"Settings"));
		addHelp(south,"text");
		add("South",new Panel3D(south));
		pack();
		center(f);
	}

	public void doAction (String o)
	{	if (o.equals("OK") || o.equals("More"))
		{	TO.setLines(A.getText());
			More=o.equals("More");
			doclose();
		}
		if (o.equals("Settings"))
		{	Settings=true;
			doAction("OK");
		}
		else super.doAction(o);
	}
	
	public boolean wantSettings ()
	{	return Settings;
	}	

	public boolean wantsMore ()
	{	return More;
	}
}


class TextEditDialog extends ObjectEditDialog
{	TextField X,Y;
	Checkbox Fixed;
	IconBar TypeIB;
	
	public TextEditDialog (Frame f, TextObject o)
	{	super(f,Zirkel.name("edit.text.title"),o);
	}
	
	public void addFirst (Panel P)
	{	TextObject t=(TextObject)O;
	
		X=new TextFieldAction(this,"X",""+t.getX(),30);
		P.add(new MyLabel(Zirkel.name("edit.point.x"))); P.add(X);
		Y=new TextFieldAction(this,"Y",""+t.getY(),30);
		P.add(new MyLabel(Zirkel.name("edit.point.y"))); P.add(Y);

		Fixed=new Checkbox("");
		Fixed.setState(t.fixed());
		P.add(new MyLabel(Zirkel.name("edit.fixed")));
		P.add(Fixed);
		if (t.fixed())
		{	X.setText(t.getEX());
			Y.setText(t.getEY());
		}
	}
	
	public void doAction (String o)
	{	if ((o.equals("Y") || o.equals("X")) && Fixed!=null)
		{	Fixed.setState(true);
			super.doAction("OK");
		}
		else super.doAction(o);
	}

	public void setAction ()
	{	TextObject t=(TextObject)O;
		
		if (Fixed!=null && Fixed.getState()==true)
		{	t.setFixed(X.getText(),Y.getText());
		}
		else
		{	try
			{	double x=new Expression(X.getText(),
					t.getConstruction(),t).getValue();
				double y=new Expression(Y.getText(),
					t.getConstruction(),t).getValue();
				t.move(x,y);
			}
			catch (Exception e) {}
		}
		if (Fixed!=null && Fixed.getState()==false)
		{	t.setFixed(false);
		}
		
	}

	public void focusGained (FocusEvent e)
	{	if (Fixed!=null && Fixed.getState()) X.requestFocus();
		else super.focusGained(e);
	}

	public boolean showsValue ()
	{	return false;
	}
}

public class TextObject extends ConstructionObject
	implements MoveableObject
{	protected double X,Y;
	Vector T; // Vector of expression strings
	static Count N=new Count();
	double C,R,W,H;
	protected Expression EX,EY;
	protected boolean Fixed;
	protected boolean DoShow; // for replays
	
	public TextObject (Construction c, double x, double y)
	{	super(c);
		X=x; Y=y;
		T=new Vector();
		setColor(ColorIndex);
		Valid=true;
	}

	public boolean nearto (int x, int y, ZirkelCanvas zc)
	{	if (!displays(zc)) return false;
		return C<=x && R<=y && x<=C+W && y<=R+H;
	}
	
	public String getTag () { return "Text"; }
	public int getN () { return N.next(); }
	
	public void updateText ()
	{	setText(getLines(),true);			
	}
	
	public void move (double x, double y)
	{	X=x; Y=y;
	}
	
	public void paint (MyGraphics g, ZirkelCanvas zc)
	{	C=zc.col(X); R=zc.row(Y);
		if (mustHide(zc)) return;
		boolean hidden=Hidden;
		if (DoShow) Hidden=false;
		g.setColor(this);
		setFont(g);
		if (DoShow) Hidden=hidden;
		FontMetrics fm=g.getFontMetrics();
		W=H=fm.getHeight();
		Enumeration e=T.elements();
		double r=R;
		if (!e.hasMoreElements())
		{	String s="-";
			g.drawString(s,C,r);
			r+=H;
		}		
		while (e.hasMoreElements())
		{	String s=((ExpressionString)e.nextElement()).evaluate();
			boolean tex=false,bold=false,large=false;
			if (s.startsWith("***"))
			{	s=s.substring(3); bold=large=true;
			}
			else if (s.startsWith("**"))
			{	s=s.substring(2); large=true;
			}
			else if (s.startsWith("*"))
			{	s=s.substring(1); bold=true;
			}
			if (g instanceof MyGraphics13)
			{	if (isStrongSelected())
				{	((MyGraphics13)g).drawMarkerRect(C,r,10,10);
					g.setColor(this);
				}
				g.setFont(large,bold);
				r+=((MyGraphics13)g).drawStringExtended(s,C,r);
			}
			else
			{	g.setFont(large,bold);
				fm=g.getFontMetrics();
				int h=fm.getHeight();
				g.drawString(AngleObject.translateToUnicode(s),C,r);
				r+=H;
			}
		}
		H=r-R;
	}
	
	public boolean mustHide (ZirkelCanvas zc)
	{	return super.mustHide(zc) && !(Valid && DoShow);
	}
	
	public void validate ()
	{	Valid=true;
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
	
	public String getLines ()
	{	String S="";
		Enumeration e=T.elements();
		while (e.hasMoreElements())
		{	String s=((ExpressionString)e.nextElement()).toString();
			S=S+s;
			if (e.hasMoreElements()) S=S+"\n";
		}
		return S;
	}
	
	public void setLines (String o)
	{	Vector w=new Vector();
		try
		{	BufferedReader in=new BufferedReader(
				new StringReader(o));
			while (true)
			{	String s=in.readLine();
				if (s==null) break;
				w.addElement(new ExpressionString(s,this));
			}
			in.close();
		}
		catch (Exception e) {}
		T=w;
		updateText();
	}
	
	public String getDisplayValue ()
	{	return "("+roundDisplay(X)+","+roundDisplay(Y)+")";
	}
	
	public double getX () { return X; }
	public double getY () { return Y; }

	public void edit (ZirkelCanvas zc)
	{	TextObjectDialog d=new TextObjectDialog(zc.getFrame(),this);
		d.setVisible(true);
		zc.repaint();
		if (d.wantSettings())
		{	new TextEditDialog(zc.getFrame(),this).setVisible(true);
			zc.validate();
			zc.repaint();
		}
		if (d.wantsMore())
		{	new EditConditionals(zc.getFrame(),this);
			validate();
		}
	}

	public void printArgs (XmlWriter xml)
	{	if (Fixed && EX!=null && EX.isValid()) xml.printArg("x",EX.toString());
		else xml.printArg("x",""+X);
		if (Fixed && EY!=null && EY.isValid()) xml.printArg("y",EY.toString());
		else xml.printArg("y",""+Y);
		if (Fixed) xml.printArg("fixed","true");
	}

	public boolean moveable ()
	{	return !Fixed;
	}	

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

	public String getEX ()
	{	if (EX!=null) return EX.toString();
		else return ""+round(X);
	}

	public String getEY ()
	{	if (EY!=null) return EY.toString();
		else return ""+round(Y);
	}

	public void setDoShow (boolean flag)
	{	DoShow=flag;
	}

	public void snap (ZirkelCanvas zc)
	{	double d=zc.getGridSize()/2;
		X=Math.round(X/d)*d;
		Y=Math.round(Y/d)*d;
	}

	public void translate ()
	{	if (Fixed)
		{	try
			{	setFixed(EX.toString(),EY.toString());
				EX.translate(); EY.translate();
			}
			catch (Exception e) {}
		}
		setLines(getLines());
		Enumeration e=T.elements();
		while (e.hasMoreElements())
		{	((ExpressionString)e.nextElement()).translate();
		}
	}
	
	public Enumeration depending ()
	{	super.depending();
		Enumeration e=T.elements();
		while (e.hasMoreElements())
		{	((ExpressionString)e.nextElement()).addDep(this);
		}
		if (Fixed)
		{	if (EX!=null) EX.addDep(this);
			if (EY!=null) EY.addDep(this);
		}
		return DL.elements();
	}
	
	public boolean canDisplayName ()
	{	return false;
	}

	double oldx,oldy,startx,starty;
	
	public void startDrag (double x, double y) 
	{	oldx=X; oldy=Y;
		startx=x; starty=y;
	}

	public void dragTo (double x, double y) 
	{	move(oldx+(x-startx),oldy+(y-starty));
	}
}
