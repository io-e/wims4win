package rene.zirkel.objects;

// file: SegmentObject.java

import java.awt.*;
import java.util.*;

import rene.util.xml.*;
import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelCanvas;
import rene.zirkel.construction.Construction;
import rene.zirkel.construction.ConstructionException;
import rene.zirkel.construction.Count;
import rene.zirkel.expression.Expression;
import rene.zirkel.expression.InvalidException;
import rene.zirkel.graphics.MyGraphics;
import rene.zirkel.graphics.MyGraphics13;
import rene.dialogs.*;
import rene.gui.*;

/**
 * @author Rene
 * Class for segments, derived from LineObject, TwoPointLineObject.
 * Segments override various methods from lines. They have a length.
 * Also the length can be fixed. 
 */

public class SegmentObject extends TwoPointLineObject
{	static Count N=new Count();
	protected boolean Fixed=false; // fixed length?
	Expression E; // expression to fix the length.
	boolean ExpressionFailed; // invalid expression?
	boolean Arrow=false; // draw as arrow.
	
	public SegmentObject (Construction c, PointObject p1, PointObject p2)
	{	super(c,p1,p2);
		validate();
		updateText();
		Unit=Global.getParameter("unit.length","");
	}
	public String getTag () { return "Segment"; }
	public int getN () { return N.next(); }
	
	public void setDefaults ()
	{	Arrow=Cn.Vectors;
		super.setDefaults();
	}
	
	public void updateText ()
	{	if (!Fixed)
			setText(text2(Zirkel.name("text.segment"),P1.getName(),P2.getName()));
		else
		{	if (E==null) 
				setText(text3(Zirkel.name("text.segment.fixed"),
					P1.getName(),P2.getName(),""+round(R)));
			else
				setText(text3(Zirkel.name("text.segment.fixed"),
					P1.getName(),P2.getName(),"\""+E.toString()+"\""));
		}
	}
	
	public void validate ()
	{	ExpressionFailed=false;
		if (!P1.valid() || !P2.valid()) { Valid=false; return; }
		else
		{	Valid=true;
			X1=P1.getX(); Y1=P1.getY();
			X2=P2.getX(); Y2=P2.getY();
			// compute normalized vector in the direction of the line:
			DX=X2-X1; DY=Y2-Y1; 
			R=Math.sqrt(DX*DX+DY*DY);
			// if fixed, move the moveable endpoint.
			if (Fixed && E!=null)
			{	try
				{	double FixedR=E.getValue();
					// System.out.println(R+" "+FixedR);
					if (FixedR<1e-8)
					{	R=0; ExpressionFailed=true; Valid=false; return;
					}
					boolean movefirst=P1.moveableBy(this),
						movesecond=P2.moveableBy(this);
					if (P2.getBound()!=null)
					{	ConstructionObject bound=P2.getBound();
						if (bound instanceof RayObject)
						{	if (((RayObject)bound).getP1()==P1) movesecond=true;
						}
					}
					else if (P1.getBound()!=null)
					{	ConstructionObject bound=P1.getBound();
						if (bound instanceof RayObject)
						{	if (((RayObject)bound).getP1()==P2) 
							{	movefirst=true; movesecond=false;
							}
						}
					}
					if (movesecond)
					{	if (R<1e-10) P2.move(X1+FixedR,Y1);
						else P2.move(X1+FixedR*DX/R,Y1+FixedR*DY/R);
						P1.setUseAlpha(false);
						// System.out.println("Move "+P2.getName());
					}
					else if (movefirst)
					{	if (R<1e-10) P1.move(X2-FixedR,Y2);
						else P1.move(X2-FixedR*DX/R,Y2-FixedR*DY/R);
						P2.setUseAlpha(false);
						// System.out.println("Move "+P1.getName());
					}
					else Fixed=false; // no moveable endpoint!
					if (Fixed)
					{	X1=P1.getX(); Y1=P1.getY();
						X2=P2.getX(); Y2=P2.getY();
						DX=X2-X1; DY=Y2-Y1; 
						R=Math.sqrt(DX*DX+DY*DY);
						P2.movedBy(this);
						P1.movedBy(this);
					}
				}
				catch (Exception e)
				{	ExpressionFailed=true; Valid=false; R=0; return;
				}
			}
			// See of the length is too small.
			if (R<1e-10) { R=0; DX=1; DY=0; }
			else { DX/=R; DY/=R; }
		}
	}

	public void paint (MyGraphics g, ZirkelCanvas zc)
	{	if (!Valid || mustHide(zc)) return;
		double c1=zc.col(X1),r1=zc.row(Y1),
			c2=zc.col(X2),r2=zc.row(Y2);
		if (visible(zc))
		{	if (isStrongSelected() && g instanceof MyGraphics13)
			{	((MyGraphics13)g).drawMarkerLine(c1,r1,c2,r2);
			}
			g.setColor(this);
			g.drawLine(c1,r1,c2,r2,this);
			if (Arrow) // draw as arrow!
			{	double a=Math.PI*0.9;
				double r=zc.dx(zc.scale(
					Global.getParameter("arrowsize",15))); // 10 pixel on the screen
				g.drawLine(c2,r2,
					zc.col(X2+(DX*Math.cos(a)+DY*Math.sin(a))*r),
					zc.row(Y2+(-DX*Math.sin(a)+DY*Math.cos(a))*r),this);
				a=-a;
				g.drawLine(c2,r2,
					zc.col(X2+(DX*Math.cos(a)+DY*Math.sin(a))*r),
					zc.row(Y2+(-DX*Math.sin(a)+DY*Math.cos(a))*r),this);				
			}
		}
		String s=getDisplayText();
		if (!s.equals(""))
		{	g.setLabelColor(this);
			setFont(g);
			DisplaysText=true;
			if (KeepClose)
			{	double side=(YcOffset<0)?1:-1;
				drawLabel(g,s,zc,X1+XcOffset*(X2-X1),Y1+XcOffset*(Y2-Y1),
					side*DX,side*DY,0,0);
			}
			else
				drawLabel(g,s,zc,(X1+X2)/2,(Y1+Y2)/2,
					DX,DY,XcOffset,YcOffset);
		}
	}
	
	public boolean canKeepClose ()
	{	return true;
	}
	
	public void setKeepClose (double x, double y)
	{	KeepClose=true;
		XcOffset=(x-X1)/R*DX+(y-Y1)/R*DY;
		YcOffset=(x-X1)/R*DY-(y-Y1)/R*DX;
	}

	public String getDisplayValue ()
	{	return ""+round(R,ZirkelCanvas.LengthsFactor);
	}
	
	/**
	 * see, if a point is on the segment or near to it.
	 */
	public boolean nearto (int c, int r, ZirkelCanvas zc)
	{	if (ExpressionFailed && P1.valid()) return P1.nearto(c,r,zc);
		if (ExpressionFailed && P2.valid()) return P2.nearto(c,r,zc);
		if (!displays(zc)) return false;
		//compute point at c,r
		double x=zc.x(c),y=zc.y(r);
		// compute distance from line
		double d=(x-X1)*DY-(y-Y1)*DX;
		// compute offset
		double o=(x-X1)*DX+(y-Y1)*DY,o1=(X2-X1)*DX+(Y2-Y1)*DY;
		if (o1>0)
		{	if (o>o1) d=Math.sqrt((x-X2)*(x-X2)+(y-Y2)*(y-Y2));
			else if (o<0) d=Math.sqrt((x-X1)*(x-X1)+(y-Y1)*(y-Y1));
		}
		else
		{	if (o<o1) d=Math.sqrt((x-X2)*(x-X2)+(y-Y2)*(y-Y2));
			else if (o>0) d=Math.sqrt((x-X1)*(x-X1)+(y-Y1)*(y-Y1));
		}
		// scale in screen coordinates
		Value=Math.abs(zc.col(zc.minX()+d)-zc.col(zc.minX()))*0.9;
		return Value<zc.selectionSize();
	}
	
	/**
	 * true, if the segment is too small.
	 */
	public boolean onlynearto (int c, int r, ZirkelCanvas zc)
	{	return R<zc.dx(3*(int)zc.pointSize());
	}

	public void printArgs (XmlWriter xml)
	{	xml.printArg("from",P1.getName());
		xml.printArg("to",P2.getName());
		if (Fixed && E!=null) xml.printArg("fixed",E.toString());
		if (Arrow) xml.printArg("arrow","true");
		super.printArgs(xml);
	}

	public double getLength () { return R; }
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
				throw new ConstructionException (E.getErrorText());
			Fixed=true;
		}
		updateText();
	}
	public void round ()
	{	try
		{	setFixed(true,getDisplayValue());
			validate();
		}
		catch (Exception e) {}
	}
	
	/**
	@return Segment can be fixed in length.
	*/
	public boolean canFix ()
	{	return P1.moveableBy(this) || P2.moveableBy(this);
	}
	
	public boolean contains (double x, double y)
	{	double a=(x-X1)*DX+(y-Y1)*DY;
		if (a<-1e-9 || a>R+1e-9) return false;
		return true;
	}
	
	public double project (double x, double y)
	{	double h=super.project(x,y);
		if (h<0) return 0;
		if (h>R) return R;
		return h;
	}
	
	/**
	 * @return true, if equal.
	 */
	public boolean equals (ConstructionObject o)
	{	if (!(o instanceof SegmentObject) || !o.valid()) return false;
		SegmentObject l=(SegmentObject)o;
		return
		(	equals(X1,l.X1) && equals(X2,l.X2) &&
			equals(Y1,l.Y1) && equals(Y2,l.Y2)
		)
		||
		(	equals(X1,l.X2) && equals(Y1,l.Y2) &&
			equals(X2,l.X1) && equals(Y2,l.Y1)
		);
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

	public String getStringLength ()
	{	if (E!=null) return E.toString();
		else return ""+round(R);
	}
	
	public double getValue ()
		throws ConstructionException
	{	if (!Valid) throw new InvalidException("exception.invalid");
		else return R;
	}

	public void translate ()
	{	super.translate();
		try
		{	setFixed(Fixed,E.toString());
			E.translate();
		}
		catch (Exception e) { Fixed=false; }
	}
	
	public Enumeration depending ()
	{	if (!Fixed || E==null) return super.depending();
		else
		{	super.depending();
			Enumeration e=E.getDepList().elements();
			while (e.hasMoreElements())
			{	DL.add((ConstructionObject)e.nextElement());
			}
			return DL.elements();
		}
	}

	public void setArrow (boolean arrow)
	{	Arrow=arrow;
	}
	public boolean isArrow ()
	{	return Arrow;
	}

	public void project (PointObject P) 
	{	double h=project(P.getX(),P.getY());
		P.setXY(getX()+h*getDX(),getY()+h*getDY());
		P.setA(h/getLength());
	}

	public void project(PointObject P, double alpha)
	{	double d=alpha*getLength();
		P.setXY(getX()+d*getDX(),getY()+d*getDY());
	}

	public boolean moveable () 
	{	if (!Fixed && P1.moveable() && P2.moveable()) return true;
		return false;
	}

}
