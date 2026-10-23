package rene.zirkel.objects;

// file: CircleObject.java

import java.awt.*;
import java.util.*;

import rene.util.xml.*;
import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelCanvas;
import rene.zirkel.construction.Construction;
import rene.zirkel.construction.ConstructionException;
import rene.zirkel.expression.Expression;
import rene.zirkel.expression.InvalidException;
import rene.dialogs.*;

public class CircleObject extends PrimitiveCircleObject
	implements MoveableObject
{	protected PointObject P2;
	boolean Fixed=false;
	Expression E;
	boolean ExpressionFailed;
	boolean Ellipse=false;

	public CircleObject (Construction c, PointObject p1, PointObject p2)
	{	super(c,p1);
		P2=p2;
		validate();
		updateText();
	}
	
	public String getTag () { return "Circle"; }
	
	public void updateText ()
	{	if (Fixed)
		{	if (E==null)	
				setText(text3(Zirkel.name("text.circle.fixed"),
					M.getName(),P2.getName(),""+round(R)));
			else
				setText(text3(Zirkel.name("text.circle.fixed"),
					M.getName(),P2.getName(),"\""+E.toString()+"\""));
		}	
		else 
			setText(text2(Zirkel.name("text.circle"),M.getName(),P2.getName()));
	}

	public void validate ()
	{	super.validate();
		ExpressionFailed=false;
		if (!M.valid() || !P2.valid()) { Valid=false; return; }
		else
		{	Valid=true;
			X=M.getX(); Y=M.getY();
			double X2=P2.getX(),Y2=P2.getY();
			// compute normalized vector in the direction of the line:
			double DX=X2-X,DY=Y2-Y; 
			R=Math.sqrt(DX*DX+DY*DY);
			if (Fixed)
			{	if (!P2.moveableBy(this))
				{	Fixed=false;
				}
				else try
				{	double FixR=E.getValue();
					if (FixR<0) FixR=0;
					if (R<1e-10) P2.move(X+FixR,Y);
					else P2.move(X+DX*FixR/R,Y+DY*FixR/R);
					R=FixR;
					P2.movedBy(this);
				}
				catch (Exception e)
				{	R=0; P2.move(X,Y); ExpressionFailed=true; Valid=false;
				}
			}
		}
	}

	public void printArgs (XmlWriter xml)
	{	xml.printArg("through",P2.getName());
		if (Fixed && E!=null) xml.printArg("fixed",E.toString());
		super.printArgs(xml);
	}
	
	public boolean canFix ()
	{	return /* M.moveableBy(this) || */ P2.moveableBy(this);
	}
	
	public boolean fixed () { return Fixed; }
	public void setFixed (boolean flag, String s)
		throws ConstructionException
	{	if (!flag || s.equals(""))
		{	Fixed=false;
			E=null;
		}
		else
		{	E=new Expression(s,getConstruction(),this);
			if (!E.isValid())
				throw new ConstructionException(E.getErrorText());
			Fixed=true;
		}
		updateText();
	}

	public boolean nearto (int c, int r, ZirkelCanvas zc)
	{	if (ExpressionFailed && M.valid()) return M.nearto(c,r,zc);
		return super.nearto(c,r,zc);
	}
	
	public void edit (ZirkelCanvas zc)
	{	super.edit(zc);
		if (E!=null && !E.isValid())
		{	Frame F=zc.getFrame();
			Warning w=new Warning(F,E.getErrorText(),
				Zirkel.name("warning"),true);
			w.center(F);
			w.setVisible(true);
		}
	}
	
	public boolean isValidFix ()
	{	return E!=null && E.isValid();
	}
	
	public void updateCircleDep ()
	{	addDep(P2);
	}

	public void translate ()
	{	super.translate();
		P2=(PointObject)P2.getTranslation();
		try
		{	setFixed(Fixed,E.toString());
			E.translate();
		}
		catch (Exception e) { Fixed=false; }
	}

	public String getStringLength ()
	{	if (E!=null) return E.toString();
		else return ""+roundDisplay(R);
	}

	public double getValue ()
		throws ConstructionException
	{	if (!Valid) throw new InvalidException("exception.invalid");
		else return R;
	}

	public Enumeration depending ()
	{	super.depending();
		if (!Fixed || E==null) return depset(P2);
		else
		{	depset(P2);
			Enumeration e=E.getDepList().elements();
			while (e.hasMoreElements())
			{	DL.add((ConstructionObject)e.nextElement());
			}
			return DL.elements();
		}
	}

	public PointObject getP2 ()
	{	return P2;
	}
	
	public boolean contains (PointObject p)
	{	return p==P2;
	}

	public void dragTo (double x, double y) 
	{	M.move(x1+(x-x3),y1+(y-y3));
		P2.move(x2+(x-x3),y2+(y-y3));
	}

	public void move (double x, double y) 
	{
	}

	public boolean moveable () 
	{	if (!Fixed && M.moveable() && P2.moveable()) return true;
		return false;
	}

	double x1,y1,x2,y2,x3,y3;
	
	public void startDrag (double x, double y) 
	{	x1=M.getX(); y1=M.getY();
		x2=P2.getX(); y2=P2.getY();
		x3=x; y3=y;
	}
	
	public void snap (ZirkelCanvas zc)
	{	if (moveable())
		{	M.snap(zc); P2.snap(zc);
		}
	}

}
