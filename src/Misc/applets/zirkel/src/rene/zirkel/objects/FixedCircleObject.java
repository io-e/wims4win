package rene.zirkel.objects;

// file: CircleObject.java

import java.awt.*;
import java.util.*;

import rene.util.xml.*;
import rene.zirkel.*;
import rene.zirkel.construction.*;
import rene.zirkel.dialogs.*;
import rene.zirkel.expression.*;
import rene.dialogs.*;

public class FixedCircleObject extends PrimitiveCircleObject
	implements MoveableObject, SimulationObject
{	Expression E;
	boolean EditAborted=false;
	boolean Dragable=false;

	public FixedCircleObject (Construction c, PointObject p1, double x, double y)
	{	super(c,p1);
		init(c,x,y);
	}
	
	public void init (Construction c, double x, double y)
	{	E=new Expression(""+
			Math.sqrt((x-M.getX())*(x-M.getX())+(y-M.getY())*(y-M.getY())),
			c,this);
		validate();
		updateText();
	}
	
	public String getTag () { return "Circle"; }
	
	public void updateText ()
	{	if (E==null || !E.isValid()) return;
		setText(text2(Zirkel.name("text.fixedcircle"),
				M.getName(),""+E.toString()));
	}

	public void validate ()
	{	super.validate();
		if (!M.valid()) { Valid=false; return; }
		Valid=true;
		X=M.getX(); Y=M.getY();
		if (E!=null && !E.isValid())
		{	return;
		}
		try
		{	if (E!=null) R=E.getValue();
		}
		catch (Exception e)
		{	R=0; Valid=false;
		}
		if (R<-1e-10) { Valid=false; return; }
	}

	public void printArgs (XmlWriter xml)
	{	if (E.isValid()) xml.printArg("fixed",E.toString());
		else xml.printArg("fixed",""+R);
		if (Dragable) xml.printArg("dragable","true");
		super.printArgs(xml);
	}
	
	public boolean canFix ()
	{	return true;
	}
	
	public boolean fixed () { return true; }
	public void setFixed (String s)
	{	E=new Expression(s,getConstruction(),this);
	}
	
	public void round ()
	{	try
		{	setFixed(round(E.getValue(),ZirkelCanvas.LengthsFactor)+"");
			validate();
		}
		catch (Exception e) {}
	}

	public void edit (ZirkelCanvas zc)
	{	ObjectEditDialog d;
		while (true)
		{	d=new CircleEditDialog(zc.getFrame(),this,zc);
			d.setVisible(true);
			EditAborted=false;
			if (d.isAborted())
			{	//E=new Expression(""+R,getConstruction(),this);
				EditAborted=true;
				break;
			}
			else if (!E.isValid())
			{	Frame F=zc.getFrame();
				Warning w=new Warning(F,E.getErrorText(),
					Zirkel.name("warning"),true);
				w.center(F);
				w.setVisible(true);
			}
			else break;
		}
		validate();
		updateText();
		zc.getConstruction().updateCircleDep();
		zc.repaint();
		if (d.wantsMore())
		{	new EditConditionals(zc.getFrame(),this);
			validate();
		}
	}
	
	public boolean nearto (int c, int r, ZirkelCanvas zc)
	{	if (!Valid && M.valid()) return M.nearto(c,r,zc);
		return super.nearto(c,r,zc);
	}
	
	public boolean isValidFix ()
	{	return E.isValid();
	}
	
	public void translate ()
	{	super.translate();
		try
		{	setFixed(E.toString());
			E.translate();
		}
		catch (Exception e) {}
	}

	public String getStringLength ()
	{	return E.toString();
	}

	public double getValue ()
		throws ConstructionException
	{	if (!Valid) throw new InvalidException("exception.invalid");
		else return R;
	}

	public Enumeration depending ()
	{	super.depending();
		Enumeration e=E.getDepList().elements();
		while (e.hasMoreElements())
		{	DL.add((ConstructionObject)e.nextElement());
		}
		return DL.elements();
	}

	public void move (double x, double y)
	{	init(getConstruction(),x,y);
	}
	
	public boolean moveable ()
	{	return Dragable || M.moveable();
	}
	
	public boolean isFixed ()
	{	return true;
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
	
	public boolean fixedByNumber ()
	{	return (E!=null && E.isNumber());
	}

	// For the simulate function:
	
	/**
	 * Set the simulation value, remember the old value.
	 */
	public void setSimulationValue (double x) 
	{	R=x;
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

	double x1,y1,x2,y2;
	
	public void startDrag (double x, double y) 
	{	x1=M.getX(); y1=M.getY();
		x2=x; y2=y;
	}

	public void dragTo (double x, double y) 
	{	if (Dragable) move(x,y);
		else M.move(x1+(x-x2),y1+(y-y2));
	}

	public void snap (ZirkelCanvas zc)
	{	if (moveable() && !Dragable)
		{	M.snap(zc);
		}
	}
}
