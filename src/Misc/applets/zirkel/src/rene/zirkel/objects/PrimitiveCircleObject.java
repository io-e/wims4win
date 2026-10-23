package rene.zirkel.objects;

// file: PrimitiveCircleObject.java

import java.util.Enumeration;

import rene.gui.Global;
import rene.util.xml.XmlWriter;
import rene.zirkel.*;
import rene.zirkel.construction.*;
import rene.zirkel.dialogs.CircleEditDialog;
import rene.zirkel.dialogs.EditConditionals;
import rene.zirkel.dialogs.ObjectEditDialog;
import rene.zirkel.graphics.*;
import rene.zirkel.structures.Coordinates;
import rene.zirkel.expression.*;

public class PrimitiveCircleObject extends ConstructionObject
	implements PointonObject, InsideObject
{	protected double X,Y,R;
	static Count N=new Count();
	boolean Partial=false;
	PointObject Dep[]; // array for points, depending on the circle for partial display
	int NDep; // number of points in Dep
	PointObject M; // The midpoint
	boolean Filled=false;
	Expression Start=null,End=null; // drawing range
	double A1,A2,A;
	boolean Arc=true;

	public PrimitiveCircleObject (Construction c, PointObject p)
	{	super(c);
		setColor(ColorIndex);
		M=p;
		Unit=Global.getParameter("unit.length","");
	}

	public String getTag () { return "Circle"; }
	public int getN () { return N.next(); }
	
	public void paint (MyGraphics g, ZirkelCanvas zc)
	{	if (!Valid  || mustHide(zc)) return;
		double c1=zc.col(X-R),c2=zc.col(X+R),
			r1=zc.row(Y+R),r2=zc.row(Y-R),
			r=(r2-r1)/2;
		double ssa=1/Math.sqrt(2),ssb=-ssa;
		// paint:
		if (!zc.showHidden() && Dep!=null && NDep>0 && Partial && 
				!hasRange()) // partial display
		{	for (int i=0; i<NDep; i++)
			{	if (!Dep[i].valid() || Dep[i].mustHide(zc)) continue;
				double A=Math.atan2(Dep[i].getY()-Y,Dep[i].getX()-X);
				if (A<0) A+=2*Math.PI;
				double a=A/Math.PI*180;
				if (visible(zc))
				{	if (isStrongSelected() && g instanceof MyGraphics13)
					{	((MyGraphics13)g).drawMarkerArc((c1+c2)/2.0,(r1+r2)/2.0,r,a-10,20);
					}
					g.setColor(this);
					g.drawCircleArc(c1+r,r1+r,r,a-10,20,this);
				}
				if (i==0)
				{	String s=getDisplayText();
					if (!s.equals(""))
					{	g.setLabelColor(this);
						DisplaysText=true;
						double sx=Math.cos(A-0.1);
						double sy=Math.sin(A-0.1);
						drawLabel(g,s,zc,X+sx*R,Y+sy*R,sy,-sx,XcOffset,YcOffset);
					}
				}
			}
		}
		else
		{	if (hasRange()) // arc display
			{	computeA1A2();
				if (visible(zc))
				{	if (isStrongSelected() && g instanceof MyGraphics13)
					{	((MyGraphics13)g).drawMarkerArc((c1+c2)/2.0,(r1+r2)/2.0,
							r,A1/Math.PI*180,A/Math.PI*180);
					}
					g.setColor(this);
					if (Filled)
					{	g.fillArc(c1,r1,c2-c1,r2-r1,
							A1/Math.PI*180,A/Math.PI*180,
							Selected||(getColorType()!=THIN),
							getColorType()!=THICK,Arc,this);
					}
					else if (visible(zc)) 
						g.drawCircleArc(c1+r,r1+r,r,
							A1/Math.PI*180,A/Math.PI*180,this);
				}
				ssa=Math.cos(A1+A/2);
				ssb=Math.sin(A1+A/2);
			}
			else if (Filled)
			{	if (visible(zc))
				{	if (isStrongSelected() && g instanceof MyGraphics13)
					{	((MyGraphics13)g).drawMarkerArc((c1+c2)/2.0,(r1+r2)/2.0,r,0,360);
					}
					g.setColor(this);
					g.fillOval(c1,r1,c2-c1,r2-r1,
						Indicated||Selected||(getColorType()==NORMAL),getColorType()!=THICK,
							this);
				}
			}
			else // full unfilled display
			{	if (visible(zc)) 
				{	if (isStrongSelected() && g instanceof MyGraphics13)
					{	((MyGraphics13)g).drawMarkerArc((c1+c2)/2.0,(r1+r2)/2.0,r,0,360);
					}
					g.setColor(this);
					g.drawCircle(c1+r,r1+r,r,this);
				}
			}
			String s=getDisplayText();
			if (!s.equals(""))
			{	g.setLabelColor(this);
				DisplaysText=true;
				drawLabel(g,s,zc,X+ssa*R,Y+ssb*R,-ssa,ssb,XcOffset,YcOffset);
			}		
		}
	}
	
	public String getDisplayValue ()
	{	return ""+round(R,ZirkelCanvas.LengthsFactor);
	}
	
	public String getEquation ()
	{	return "(x"+helpDisplayNumber(false,-X)+")^2+"
		+"(y"+helpDisplayNumber(false,-Y)+")^2="
		+helpDisplayNumber(true,R*R);
	}

	public boolean nearto (int c, int r, ZirkelCanvas zc)
	{	return nearto(c,r,false,zc);
	}
	
	public boolean nearto (int c, int r, boolean ignorefill, ZirkelCanvas zc)
	{	if (!displays(zc)) return false;
		double x=zc.x(c)-X,y=zc.y(r)-Y;
		if (!ignorefill && Filled)
		{	double d=Math.sqrt(x*x+y*y);
			if (d<R) Value=0;
			return d<R;
		}
		else if (hasRange())
		{	computeA1A2();
			double a=Math.atan2(y,x);
			if (a<0) a+=2*Math.PI;
			a-=A1;
			if (a<0) a+=2*Math.PI;
			double d=Math.abs(Math.sqrt(x*x+y*y)-R);
			if (a<=A+0.01)
				Value=Math.abs(zc.col(zc.minX()+d)-zc.col(zc.minX()));
			return Value<zc.selectionSize() && a<=A+0.01;
		}
		// partial display:
		else if (!zc.showHidden() && NDep>0 && Partial)
		{	double d=Math.abs(Math.sqrt(x*x+y*y)-R);
			Value=Math.abs(zc.col(zc.minX()+d)-zc.col(zc.minX()));
			if (Math.abs(zc.col(zc.minX()+d)-zc.col(zc.minX()))>=
				zc.selectionSize()) return false;
			d=Math.PI/18;
			double a=Math.atan2(y,x);
			if (a<0) a+=2*Math.PI;
			for (int i=0; i<NDep; i++)
			{	if (!Dep[i].valid() || Dep[i].mustHide(zc)) continue;
				double A=Math.atan2(Dep[i].getY()-Y,Dep[i].getX()-X);
				if (A<0) A+=2*Math.PI;
				double h=a-A;
				if (h>2*Math.PI) h-=2*Math.PI;
				if (h<-2*Math.PI) h+=2*Math.PI;
				if (Math.abs(h)<d) return true;
			}
			return false;
		}
		else	
		{	double d=Math.abs(Math.sqrt(x*x+y*y)-R);
			Value=Math.abs(zc.col(zc.minX()+d)-zc.col(zc.minX()));
			return Math.abs(zc.col(zc.minX()+d)-zc.col(zc.minX()))<
				zc.selectionSize();
		}
	}
	
	public boolean onlynearto (int c, int r, ZirkelCanvas zc)
	{	if (R<zc.dx(3*(int)zc.pointSize())) return true;
		if (hasRange())
		{	double A1=Math.atan2(getStart().getY()-Y,getStart().getX()-X);
			if (A1<0) A1+=2*Math.PI;
			double A2=Math.atan2(getEnd().getY()-Y,getEnd().getX()-X);
			if (A2<0) A2+=2*Math.PI;
			double A=A2-A1;
			if (A>=2*Math.PI) A-=2*Math.PI;
			if (A<0) A+=2*Math.PI;
			if (!Obtuse && A>Math.PI)
			{	A1=A2;
				A=2*Math.PI-A;
				A2=A1+A;
			}
			if (A*R<zc.dx(6*(int)zc.pointSize())) return true;
		}
		return false;
	}

	public double getX () { return X; }
	public double getY () { return Y; }
	public double getR () { return R; }

	public static Coordinates intersect (PrimitiveCircleObject c1, 
		PrimitiveCircleObject c2)
	{	double dx=c2.X-c1.X,dy=c2.Y-c1.Y,r=Math.sqrt(dx*dx+dy*dy);
		if (r>c1.R+c2.R+1e-10) return null;
		if (r<=1e-10) return new Coordinates(c1.X,c1.Y,c1.X,c1.Y);
		double l=(r*r+c1.R*c1.R-c2.R*c2.R)/(2*r);
		dx/=r; dy/=r;
		double x=c1.X+l*dx,y=c1.Y+l*dy,h=c1.R*c1.R-l*l;
		if (h<-1e-10) return null;
		if (h<0) h=0;
		else h=Math.sqrt(h);
		return new Coordinates(x+h*dy,y-h*dx,x-h*dy,y+h*dx);
	}

	public void edit (ZirkelCanvas zc)
	{	ObjectEditDialog d=new CircleEditDialog(zc.getFrame(),this,zc);
		d.setVisible(true);
		zc.getConstruction().updateCircleDep(); 
		zc.validate(); zc.repaint();
		if (d.wantsMore())
		{	new EditConditionals(zc.getFrame(),this);
			validate();
		}
	}

	public boolean equals (ConstructionObject o)
	{	if (!(o instanceof PrimitiveCircleObject) || !o.valid())
			return false;
		PrimitiveCircleObject l=(PrimitiveCircleObject)o;
		return equals(X,l.X) && equals(Y,l.Y) && equals(R,l.R);
	}

	public void setPartial (boolean flag)
	{	if (flag==Partial) return;
		Partial=flag;
		if (flag) // depending objects no longer needed
		{	Dep=new PointObject[16];
			NDep=0;
		}
		else Dep=null;
	}
	
	/**
	 * Add a point that depends on the circle.
	 * Dep is used for partial display.
	 * @param p
	 */
	public void addDep (PointObject p)
	{	if (!Partial || hasRange() || 
			Dep==null || NDep>=Dep.length) return;
		Dep[NDep++]=p;
	} 
	
	public void clearCircleDep ()
	{	NDep=0;
	}
	
	public boolean isPartial ()
	{	return Partial;
	}
	
	public void printArgs (XmlWriter xml)
	{	xml.printArg("midpoint",M.getName());
		if (Partial) xml.printArg("partial","true");
		if (Filled) xml.printArg("filled","true");
		if (getStart()!=null) xml.printArg("start",getStart().getName());
		if (getEnd()!=null) xml.printArg("end",getEnd().getName());
		if (!Obtuse) xml.printArg("acute","true");
		if (!Arc) xml.printArg("chord","true");
		super.printArgs(xml);
	}
	
	/**
	Need to setup the Dep array.
	*/
	public ConstructionObject copy ()
	{	PrimitiveCircleObject o=
			(PrimitiveCircleObject)super.copy();
		if (o.Partial)
		{	o.Dep=new PointObject[16];
			o.NDep=0;
		}
		else o.Dep=null;
		return o;
	}

	public void setDefaults ()
	{	super.setDefaults();
		setPartial(Cn.Partial);
	}

	/**
	 * A circle depends on its midpoint at least.
	 * Other circles depen on more points!
	 * No circle depends on Start and End.
	 */
	public Enumeration depending ()
	{	super.depending();
		DL.add(M);
		return DL.elements();
	}

	/**
	* A circle will mark the midpoint as secondary parameter.
	*/	
	public Enumeration secondaryParams ()
	{	DL.reset();
		return depset(M);
	}

	public void toggleHidden ()
	{	if (Hidden)
		{	Hidden=false;
		}
		else
		{	if (Partial)
			{	setPartial(false);
				Hidden=true;
			}
			else setPartial(true);
		}
	}

	public PointObject getP1 ()
	{	return M;
	}
	
	public void setFilled (boolean flag)
	{	Filled=flag;
	}
	public boolean isFilled ()
	{	return Filled;
	}
	public boolean isFilledForSelect ()
	{	return Filled;
	}

	public void translate ()
	{	M=(PointObject)M.getTranslation();
		if (hasRange())
		{	setRange(getStart().getName(),getEnd().getName());
			Start.translate();
			End.translate();
		}
	}	

	public boolean setRange (String s1, String s2)
	{	try
		{	Start=new Expression("@\""+s1+"\"",Cn,this);
			End=new Expression("@\""+s2+"\"",Cn,this);
			return hasRange();
		}
		catch (Exception e) { Start=End=null; }
		return false;
	}

	public PointObject getStart ()
	{	return getPointObject(Start);
	}
	public PointObject getEnd ()
	{	return getPointObject(End);
	}
	
	public String getStartString ()
	{	String s="";
		if (Start!=null) s=Start.toString();
		if (s.startsWith("@")) s=s.substring(1);
		return s;
	}
	public String getEndString ()
	{	String s="";
		if (End!=null) s=End.toString();
		if (s.startsWith("@")) s=s.substring(1);
		return s;
	}
	
	public double getA1 () { return A1; }
	public double getA2 () { return A2; }
	
	public boolean hasRange ()
	{	return getStart()!=null && getEnd()!=null;
	}
	
	public void clearRange ()
	{	Start=End=null;
	}
	
	public boolean maybeTransparent ()
	{	return true;
	}
	
	public boolean locallyLike (ConstructionObject o)
	{	if (!(o instanceof PrimitiveCircleObject)) return false;
		return (equals(X,((PrimitiveCircleObject)o).X) &&
			equals(Y,((PrimitiveCircleObject)o).Y) &&
			equals(R,((PrimitiveCircleObject)o).R));
	}

	public boolean getArc ()
	{	return Arc;
	}
	public void setArc (boolean flag)
	{	Arc=flag;
	}

	public void computeA1A2 ()
	{	A1=Math.atan2(getStart().getY()-Y,getStart().getX()-X);
		if (A1<0) A1+=2*Math.PI;
		A2=Math.atan2(getEnd().getY()-Y,getEnd().getX()-X);
		if (A2<0) A2+=2*Math.PI;
		if (A2<A1) A2+=2*Math.PI;
		A=A2-A1;
		if (!Obtuse && A>Math.PI+1e-10)
		{	A1=A2;
			if (A1>=2*Math.PI) A1-=2*Math.PI;
			A=2*Math.PI-A;
			A2=A1+A;
		}
		if (Partial)
		{	A1-=10/180.0*Math.PI; A+=20/180.0*Math.PI;
		}
	}

	/**
	Test, if the projection of (x,y) to the arc contains
	that point.
	*/
	public boolean contains (double x, double y)
	{	if (!hasRange()) return true;
		computeA1A2();
		double a=Math.atan2(y-Y,x-X);
		if (a<0) a+=2*Math.PI;
		double d=a-A1;
		if (d<0) d+=2*Math.PI;
		return d<A+0.0001;
	}

	public void project (PointObject P) 
	{	double dx=P.getX()-getX(),dy=P.getY()-getY();
		double r=Math.sqrt(dx*dx+dy*dy);
		double X=0,Y=0;
		if (r<1e-10)
		{	X=getX()+getR(); Y=getY(); 
		}
		else
		{	X=getX()+dx/r*getR(); Y=getY()+dy/r*getR();
		}
		double Alpha=Math.atan2(P.getY()-getY(),P.getX()-getX());
		if (hasRange() && getStart()!=P && getEnd()!=P)
		{	if (Alpha<0) Alpha+=2*Math.PI;
			computeA1A2();
			double a1=getA1(),a2=getA2();
			if (Alpha<a1) Alpha+=2*Math.PI;
			if (Alpha>a2)
			{	if (2*Math.PI-(Alpha-a1) < Alpha-a2) Alpha=a1;
				else Alpha=a2;
			}
			X=getX()+getR()*Math.cos(Alpha);
			Y=getY()+getR()*Math.sin(Alpha);	
		}
		P.setXY(X,Y);
		P.setA(Alpha);
	}
	
	public void project (PointObject P, double alpha) 
	{	double dx=P.getX()-getX(),dy=P.getY()-getY();
		double r=Math.sqrt(dx*dx+dy*dy);
		double X=0,Y=0;
		if (r<1e-10)
		{	X=getX()+getR(); Y=getY(); 
		}
		else
		{	X=getX()+dx/r*getR(); Y=getY()+dy/r*getR();
		}
		if (hasRange() && getStart()!=P && getEnd()!=P)
		{	double Alpha=P.getAlpha();
			if (Alpha<0) Alpha+=2*Math.PI;
			if (Alpha>=2*Math.PI) Alpha-=2*Math.PI;
			computeA1A2();
			double a1=getA1(),a2=getA2();
			if (Alpha<a1) Alpha+=2*Math.PI;
			if (Alpha>a2)
			{	if (2*Math.PI-(Alpha-a1) < Alpha-a2) Alpha=a1;
				else Alpha=a2;
			}
			P.setA(Alpha);
			X=getX()+getR()*Math.cos(Alpha);
			Y=getY()+getR()*Math.sin(Alpha);
		}
		else
		{	X=getX()+getR()*Math.cos(alpha);
			Y=getY()+getR()*Math.sin(alpha);
		}
		P.setXY(X,Y);
	}

	public double containsInside (PointObject P) 
	{	double dx=P.getX()-X,dy=P.getY()-Y;
		double r=Math.sqrt(dx*dx+dy*dy);
		if (r<R*(1-1e-10)) return 1;
		if (r<R*(1+1e-10)) return 0.5;
		return 0;
	}

	public boolean keepInside(PointObject P) 
	{	double dx=P.getX()-X,dy=P.getY()-Y;
		double r=Math.sqrt(dx*dx+dy*dy);
		double f=1;
		if (Filled && ColorType==THIN) f=0.9999;
		if (r<R*f || R<1e-10) return true;
		P.setXY(X+dx/r*(R*f),Y+dy/r*(R*f));
		return false;
	}

	public boolean canInteresectWith(ConstructionObject o) 
	{
		return true;
	}

}
