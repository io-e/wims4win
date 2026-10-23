package rene.zirkel.objects;

// file: Functionbject.java

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import rene.gui.*;
import rene.util.xml.*;
import rene.zirkel.*;
import rene.zirkel.construction.*;
import rene.zirkel.dialogs.*;
import rene.zirkel.expression.*;
import rene.zirkel.graphics.*;
import rene.dialogs.*;

class CurveEditDialog extends ObjectEditDialog
{	TextField VarMin,VarMax,DVar,Var,EX,EY;
	IconBar IC,TypeIB;
	ZirkelCanvas ZC;
	Checkbox Special;
        // jm.evers                                                                                                                        
         public String xaxis_variable;                                                                                                      
         public String yaxis_variable;
	//jm.evers
	
	public CurveEditDialog (ZirkelCanvas zc, FunctionObject o)
	{	super(zc.getFrame(),Zirkel.name("edit.function.title"),o,"function");
		ZC=zc;
	}
	
	public void addFirst (Panel P)
	{	
		
		FunctionObject f=(FunctionObject)O;
		VarMin=new TextFieldAction(this,"varmin",""+f.VarMin,30);
		P.add(new MyLabel(Zirkel.name("function.varmin"))); P.add(VarMin);
		VarMax=new TextFieldAction(this,"varmax",""+f.VarMax,30);
		P.add(new MyLabel(Zirkel.name("function.varmax"))); P.add(VarMax);
		DVar=new TextFieldAction(this,"dvar",""+f.DVar,30);
		
		P.add(new MyLabel(Zirkel.name("function.dvar"))); P.add(DVar);
		Var=new TextFieldAction(this,"var",""+f.getVar(),30);
		P.add(new MyLabel(Zirkel.name("function.var"))); P.add(Var);
		String ex=f.getEX();
		if (ex.equals(f.Var)) ex="";
		EX=new TextFieldAction(this,"ex",""+ex,30);
	

		P.add(new MyLabel(Zirkel.name("function.x"))); P.add(EX);
		EY=new TextFieldAction(this,"ey",""+f.getEY(),30);
		P.add(new MyLabel(Zirkel.name("function.y"))); P.add(EY);
	}
	
	public void addSecond (Panel P)
	{	FunctionObject Func=(FunctionObject)O;
		
		IC=new IconBar(F);
		IC.setIconBarListener(this);
		IC.addOnOffLeft("filled");
		IC.setState("filled",Func.isFilled());
		IC.setIconBarListener(this);
		P.add(new MyLabel(""));
		P.add(IC);

		TypeIB=new IconBar(F);
		TypeIB.addToggleGroupLeft("type",6);
		TypeIB.toggle("type",Func.getType());
		P.add(new MyLabel("")); P.add(TypeIB);
		
		P.add(new MyLabel(Zirkel.name("edit.discrete"))); P.add(Special=new Checkbox());
		Special.setState(Func.isSpecial());
	}
	
	public void addButton (Panel P)
	{	FunctionObject f=(FunctionObject)O;
		if (!f.Var.equals(f.EX))
		{	if (f.Center==null)
				P.add(new ButtonAction(this,Zirkel.name("edit.function.center"),
					"SetCenter"));
			else
				P.add(new ButtonAction(this,Zirkel.name("edit.function.free"),
					"SetFree"));
			P.add(new MyLabel(" "));
		}
	}
	
	public void iconPressed (String o)
	{	if (o.equals("filled"))
		{	if (IC.getState("filled"))
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
	{	FunctionObject f=(FunctionObject)O;
		f.setExpressions(Var.getText(),EX.getText(),EY.getText());		
		f.setRange(VarMin.getText(),VarMax.getText(),DVar.getText());
		f.setFilled(IC.getState("filled"));
		f.setType(TypeIB.getToggleState("type"));
		f.setSpecial(Special.getState());
	}
	
	public void doAction (String o)
	{	if (o.equals("SetCenter"))
		{	ZC.setCurveCenter((FunctionObject)O);
			super.doAction("OK");
		}
		else if (o.equals("SetFree"))
		{	((FunctionObject)O).Center=null;
			super.doAction("OK");
		}
		else super.doAction(o);
	}

	public void focusGained (FocusEvent e)
	{	VarMin.requestFocus();
	}
	
}

/**
 * @author Rene
 * 
 * Function objects are parametric curves (or functions).
 * Depending on a parameter t (can have another name),
 * x(t) and y(t) are computed and drawn on the screen.
 * If x(t)=t the object is a function.
 *
 */
public class FunctionObject extends ConstructionObject
	implements PointonObject
{	Expression EX=null,EY=null;
	Expression VarMin=null,VarMax=null,DVar=null;
	double X[]={0};
	String Var[]={"x"};
	boolean Filled=false;
	Expression Center=null;
	protected int Type=0;
	public final static int SQUARE=0,DIAMOND=1,CIRCLE=2,DOT=3,CROSS=4,DCROSS=5;
	protected boolean Special=false;

	public FunctionObject (Construction c)
	{	super(c);
		VarMin=new Expression("-10",c,this);
		VarMax=new Expression("10",c,this);
		DVar=new Expression("0.1",c,this);
		validate();
		updateText();
	}
	
	public void setFilled (boolean flag)
	{	Filled=flag;
	}
	public boolean isFilled ()
	{	return Filled;
	}	

	public String getTag () { return "Function"; }
	public int getN () { return N.next(); }
	
	public void updateText ()
	{	if (EX!=null && EY!=null)
			setText(text2(Zirkel.name("text.function"),EX.toString(),EY.toString()));
		else
			setText(text2(Zirkel.name("text.function"),"",""));
	}
	
	public void validate ()
	{	if (EX!=null && EY!=null)
		{	Valid=EX.isValid() && EY.isValid() && VarMin.isValid()
			&& VarMax.isValid() && DVar.isValid();
		}
		else Valid=false;
	}

	public void setExpressions (String t, String ex, String ey)
	{	StringTokenizer tok=new StringTokenizer(t);
		Var=new String[tok.countTokens()];
		X=new double[tok.countTokens()];
		int i=0;
		while (tok.hasMoreTokens())
		{	Var[i++]=tok.nextToken();
		}
		if (ex.equals("")) ex=Var[0];
		EX=new Expression(ex,getConstruction(),this,Var);
		EY=new Expression(ey,getConstruction(),this,Var);
		validate();
	}

	public void setRange (String min, String max, String d)
	{	VarMin=new Expression(min,getConstruction(),this); 
		VarMax=new Expression(max,getConstruction(),this);
		DVar=new Expression(d,getConstruction(),this);
	}

	public String getEX ()
	{	if (EX!=null) return EX.toString();
		else return Var[0];
	}

	public String getEY ()
	{	if (EY!=null) return EY.toString();
		else return "0";
	}

	PolygonFiller PF=null;
	double C1,C2;
	int C,R,W,H;
	
	public void paint (MyGraphics g, ZirkelCanvas zc)
	{	if (!Valid || mustHide(zc)) return;
		double varmin,varmax,d;
		boolean special=Special,reverse=false;
		try
		{	varmin=VarMin.getValue();
			varmax=VarMax.getValue();
			d=DVar.getValue();
			if (varmin>varmax)
			{	double h=varmin; varmin=varmax; varmax=h;
				reverse=true;
			}
			if (d<0) d=-d;
			if (d<(varmax-varmin)/1000) d=(varmax-varmin)/1000;
		}
		catch (Exception e)
		{	Valid=false; return;
		}
		X[0]=varmin;
		if (isStrongSelected() && g instanceof MyGraphics13)
		{	PolygonDrawer pm=new PolygonDrawer(g,this);
			pm.useAsMarker();
			double x1=0,y1=0;
			while (true)
			{	try
				{	x1=EX.getValue();
					y1=EY.getValue();
					if (Math.abs(zc.col(x1))+Math.abs(zc.col(y1))>1e5)
					{	finish(g,zc);
						C1=X[0];
						if (pm!=null) pm.finishPolygon();
					}
					else
					{	if (!special || isFilled()) 
						{	if (special && isFilled())
							{	if (pm.hasStarted())
								{	if (reverse)
									{	pm.drawTo(pm.c(),zc.row(y1));
										pm.drawTo(zc.col(x1),zc.row(y1));
									}
									else
									{	pm.drawTo(zc.col(x1),pm.r());
										pm.drawTo(zc.col(x1),zc.row(y1));
									}
								}
							}
							pm.drawTo(zc.col(x1),zc.row(y1));
						}
						else 
						{	((MyGraphics13)g).drawMarkerLine(zc.col(x1),zc.row(y1),zc.col(x1),zc.row(y1));
						}
						C2=X[0];
					}
				}
				catch (Exception e) 
				{	finish(g,zc);
					C1=X[0];
					pm.finishPolygon();
				}
				if (X[0]>=varmax) break;
				X[0]=X[0]+d;
				if (X[0]>varmax) X[0]=varmax;
			}
			pm.finishPolygon();
		}
		X[0]=varmin;
		g.setColor(this);
		PolygonDrawer pd=null;
		if (!special || isFilled()) pd=new PolygonDrawer(g,this);
		if (isFilled())
		{	if (PF==null) PF=new PolygonFiller(g,this);
			PF.start();
			PF.setGraphics(g);
		}
		C1=X[0];
		double x1=0,y1=0;
		while (true)
		{	try
			{	x1=EX.getValue();
				y1=EY.getValue();
				if (Math.abs(zc.col(x1))+Math.abs(zc.col(y1))>1e5)
				{	finish(g,zc);
					C1=X[0];
					if (pd!=null) pd.finishPolygon();
				}
				else
				{	if (isFilled())
					{	if (PF.length()==0) C1=X[0];
						if (special)
						{	if (pd.hasStarted())
							{	if (reverse)
								{	PF.drawTo(pd.c(),zc.row(y1));
									PF.drawTo(zc.col(x1),zc.row(y1));
								}
								else
								{	PF.drawTo(zc.col(x1),pd.r());
									PF.drawTo(zc.col(x1),zc.row(y1));
								}
							}
							else
								PF.drawTo(zc.col(x1),zc.row(y1));
						}
						else
							PF.drawTo(zc.col(x1),zc.row(y1),X[0]<varmax);
					}
					if (pd!=null) 
					{	if (special && isFilled())
						{	if (pd.hasStarted())
							{	if (reverse)
								{	pd.drawTo(pd.c(),zc.row(y1));
									pd.drawTo(zc.col(x1),zc.row(y1));
								}
								else
								{	pd.drawTo(zc.col(x1),pd.r());
									pd.drawTo(zc.col(x1),zc.row(y1));
								}
							}
						}
						pd.drawTo(zc.col(x1),zc.row(y1));
					}
					else 
					{	PointObject.drawPoint(g,zc,this,x1,y1,Type);
					}
					C2=X[0];
				}
			}
			catch (Exception e) 
			{	finish(g,zc);
				C1=X[0];
				pd.finishPolygon();
			}
			if (X[0]>=varmax) break;
			X[0]=X[0]+d;
			if (X[0]>varmax) X[0]=varmax;
		}
		finish(g,zc);
		if (pd!=null) pd.finishPolygon();
	}
	
	static double x[]=new double[4],y[]=new double[4];
	
	public void finish (MyGraphics g, ZirkelCanvas zc)
	{	if (isFilled())
		{	if (getEX().equals(getVar()))
			{	System.out.println(C1+" "+C2);
				PF.drawTo(zc.col(C2),zc.row(0));
				PF.drawTo(zc.col(C1),zc.row(0));
			}
			else
			{	if (getCenter()!=null)
					PF.drawTo(zc.col(getCenter().getX()),
							zc.row(getCenter().getY()));
				else
					PF.drawTo(zc.col(0),zc.row(0));
			}
			PF.finishPolygon();
		}
	}
	
	public double getValue ()
		throws ConstructionException
	{	if (!Valid) throw new InvalidException("exception.invalid");
		return X[0];
	}
	
	public double getValue (String var)
		throws ConstructionException
	{	if (!Valid) throw new InvalidException("exception.invalid");
		for (int i=0; i<Var.length; i++)
			if (var.equals(Var[i])) return X[i];
		return X[0];
	}

	public double getIntegral ()
		throws ConstructionException
	{	return getSum();
	}
	
	public String getDisplayValue ()
	{	if (getEX().equals(getVar())) return EY.toString();
		else return "("+EX.toString()+","+EY.toString()+")"; 
	}
	
	public boolean nearto (int cc, int rr, ZirkelCanvas zc)
	{	if (!displays(zc)) return false;
		int size=(int)zc.selectionSize();
		double varmin,varmax,d;
		try
		{	varmin=VarMin.getValue();
			varmax=VarMax.getValue();
			d=DVar.getValue();
			if (varmin>varmax)
			{	double h=varmin; varmin=varmax; varmax=h;
			}	
			if (d<0) d=-d;
			if (d<(varmax-varmin)/1000) d=(varmax-varmin)/1000;
		}
		catch (Exception e)
		{	Valid=false; return false;
		}
		X[0]=varmin;
		double x=zc.x(cc),y=zc.y(rr);
		boolean last=false;
		double x0=0;
		double y0=0;
		while (X[0]<varmax)
		{	X[0]=X[0]+d;
			if (X[0]>varmax) X[0]=varmax;
			try
			{	double x1=EX.getValue();
				double y1=EY.getValue();
				if (Math.abs(zc.col(x1))+Math.abs(zc.col(y1))>1e5)
					throw new Exception("");
				if (last)
				{	Value=Math.abs(cc-zc.col(x1))+Math.abs(rr-zc.row(y1));
					if (Value<=size) return true;
					double r=Math.sqrt(Math.abs(x1-x0)*Math.abs(x1-x0)
							+Math.abs(y1-y0)*Math.abs(y1-y0));
					if (r>1e-5)
					{	double h=((x-x0)*(x1-x0)+(y-y0)*(y1-y0))/r;
						if (h>=0 && h<=r)
						{	Value=Math.abs(((x-x0)*(y1-y0)-(y-y0)*(x1-x0))/r)
								/zc.dx(1);
							if (Value<=size) return true;
						}
					}
				}
				x0=x1; y0=y1; last=true;
			}
			catch (Exception e) 
			{	last=false;
			}
		}
		return false;
	}
	
	public boolean EditAborted;
		
	public void edit (ZirkelCanvas zc)
	{	ObjectEditDialog d;
		while (true)
		{	d=new CurveEditDialog(zc,this);
			d.setVisible(true);
			EditAborted=false;
			if (d.isAborted())
			{	EditAborted=true;
				break;
			}
			else if (!EX.isValid())
			{	Frame F=zc.getFrame();
				Warning w=new Warning(F,EX.getErrorText(),
					Zirkel.name("warning"),true);
				w.center(F);
				w.setVisible(true);
			}
			else if (!EY.isValid())
			{	Frame F=zc.getFrame();
				Warning w=new Warning(F,EY.getErrorText(),
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

	public void printArgs (XmlWriter xml)
	{	xml.printArg("x",EX.toString());
		xml.printArg("y",EY.toString());
		xml.printArg("var",getVar());
		xml.printArg("min",""+VarMin);
		xml.printArg("max",""+VarMax);
		xml.printArg("d",""+DVar);
		if (Special) xml.printArg("special","true");
		printType(xml);
		if (Filled) xml.printArg("filled","true");
		if (getCenter()!=null) xml.printArg("center",getCenter().getName());
	}
	
	public void setType (int type)
	{	Type=type;
	}
	public int getType ()
	{	return Type;
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

	public void translate ()
	{	try
		{	EX=new Expression(EX.toString(),getConstruction(),this,Var);
			EY=new Expression(EY.toString(),getConstruction(),this,Var);
			VarMin=new Expression(VarMin.toString(),getConstruction(),this);
			VarMax=new Expression(VarMax.toString(),getConstruction(),this);
			DVar=new Expression(DVar.toString(),getConstruction(),this);
			ConstructionObject O=getTranslation();
			setTranslation(this);
			EX.translate(); 
			EY.translate();
			VarMin.translate(); 
			VarMax.translate(); 
			DVar.translate();
			if (getCenter()!=null)
			{	setCenter(getCenter().getName());
				Center.translate();
			}
			validate();
			setTranslation(O);
		}
		catch (Exception e)
		{ 	System.out.println();
			System.out.println(getName());
			System.out.println(e);
			e.printStackTrace();
		}
	}

	public boolean onlynearto (int x, int y, ZirkelCanvas zc)
	{	return false;
		//return nearto(x,y,zc);
	}

	public boolean equals (ConstructionObject o)
	{	return false;
	}

	public Enumeration depending ()
	{	DL.reset();
		addDepending(EX);
		addDepending(EY);
		addDepending(VarMin);
		addDepending(VarMax);
		addDepending(DVar);
		return DL.elements();
	}
	
	public void addDepending (Expression E)
	{	if (E!=null)
		{	Enumeration e=E.getDepList().elements();
			while (e.hasMoreElements())
			{	DL.add((ConstructionObject)e.nextElement());
			}
		}
	}

	public boolean hasUnit ()
	{	return false;
	}
	
	public double evaluateF (double x[])
		throws ConstructionException
	{	int n=x.length;
		if (n>X.length) n=X.length;
		for (int i=0; i<n; i++) X[i]=x[i];
		for (int i=n; i<X.length; i++) X[i]=0;
		try
		{	return EY.getValue();
		}
		catch (Exception e)
		{	throw new ConstructionException("");
		}
	}
	

	public double evaluateF (double x)
		throws ConstructionException
	{	X[0]=x;
		for (int i=1; i<X.length; i++) X[i]=0;
		try
		{	return EY.getValue();
		}
		catch (Exception e)
		{	throw new ConstructionException("");
		}
	}

	
	public void project (PointObject P) 
	{	double varmin,varmax,dvar;
		try
		{	varmin=VarMin.getValue();
			varmax=VarMax.getValue();
			dvar=DVar.getValue();
			if (varmin>varmax)
			{	double h=varmin; varmin=varmax; varmax=h;
			}	
			if (dvar<(varmax-varmin)/1000) 
				dvar=(varmax-varmin)/1000;
		}
		catch (Exception e)
		{	Valid=false; return;
		}
		X[0]=varmin;
		double x=0,y=0,x0=0,y0=0,dmin=0;
		boolean started=false;
		while (true)
		{	try
			{	double x1=EX.getValue();
				double y1=EY.getValue();
				if (!started)
				{	dmin=Math.sqrt((P.getX()-x1)*(P.getX()-x1)+
							(P.getY()-y1)*(P.getY()-y1));
					x0=x=x1; y0=y=y1;
					started=true;
				}
				else
				{	double h=(x1-x0)*(x1-x0)+(y1-y0)*(y1-y0);
					double g=(P.getX()-x0)*(x1-x0)+(P.getY()-y0)*(y1-y0);
					if (g<0) g=0;
					if (g>h) g=h;
					double x2=x0+g/h*(x1-x0),y2=y0+g/h*(y1-y0);
					double d=Math.sqrt((P.getX()-x2)*(P.getX()-x2)+
						(P.getY()-y2)*(P.getY()-y2));
					if (d<dmin)
					{	dmin=d;
						x=x2; y=y2;
					}
					x0=x1; y0=y1;
				}
			}
			catch (Exception e) 
			{}
			if (X[0]>=varmax) break;
			X[0]=X[0]+dvar;
			if (X[0]>varmax) X[0]=varmax;
		}
		if (started)
		{	P.setXY(x,y);
		}
	}

	public double getSum ()
	{	double varmin,varmax,dvar;
		boolean reverse=false;
		boolean parametric=!getEX().equals(getVar());
		try
		{	varmin=VarMin.getValue();
			varmax=VarMax.getValue();
			dvar=DVar.getValue();
			if (varmin>varmax)
			{	double h=varmin; varmin=varmax; varmax=h;
				reverse=true;
			}	
			if (dvar<0) dvar=-dvar;
			if (dvar<(varmax-varmin)/1000) 
				dvar=(varmax-varmin)/1000;
		}
		catch (Exception e)
		{	Valid=false; return 0;
		}
		X[0]=varmin;
		double x0=0,y0=0;
		boolean started=false;
		double sum=0;
		while (true)
		{	try
			{	double x1=EX.getValue();
				double y1=EY.getValue();
				if (parametric)
				{	double x=0,y=0;
					if (getCenter()!=null)
					{	x=getCenter().getX(); y=getCenter().getY();
					}
					if (started) 
						sum+=((x0-x)*(y1-y)-(y0-y)*(x1-x))/2;
				}
				else
				{	if (started)
					{	if (Special)
						{	if (reverse) sum+=(x1-x0)*y1;
							else sum+=(x1-x0)*y0;
						}
						else
						{	sum+=(x1-x0)*(y0+y1)/2;
						}
					}
				}
				x0=x1; y0=y1;
				started=true;
			}
			catch (Exception e) {}
			if (X[0]>=varmax) break;
			X[0]=X[0]+dvar;
			if (X[0]>varmax) X[0]=varmax;
		}
		return sum;
	}
	
	public double getLength ()
	{	double varmin,varmax,dvar;
		boolean reverse=false;
		boolean parametric=!getEX().equals(getVar());
		try
		{	varmin=VarMin.getValue();
			varmax=VarMax.getValue();
			dvar=DVar.getValue();
			if (varmin>varmax)
			{	double h=varmin; varmin=varmax; varmax=h;
				reverse=true;
			}	
			if (dvar<0) dvar=-dvar;
			if (dvar<(varmax-varmin)/1000) 
				dvar=(varmax-varmin)/1000;
		}
		catch (Exception e)
		{	Valid=false; return 0;
		}
		X[0]=varmin;
		double x0=0,y0=0;
		boolean started=false;
		double sum=0;
		while (true)
		{	try
			{	double x1=EX.getValue();
				double y1=EY.getValue();
				if (started)
					sum+=Math.sqrt((x1-x0)*(x1-x0)+(y1-y0)*(y1-y0));
				started=true;
				x0=x1; y0=y1;
			}
			catch (Exception e) {}
			if (X[0]>=varmax) break;
			X[0]=X[0]+dvar;
			if (X[0]>varmax) X[0]=varmax;
		}
		return sum;
	}
	
	public boolean isSpecial ()
	{	return Special;
	}
	public void setSpecial (boolean f)
	{	Special=f;	
	}
	
	public void project(PointObject P, double alpha) 
	{	project(P);
	}

	public boolean maybeTransparent ()
	{	return true;
	}

	public boolean canDisplayName ()
	{	return false;
	}
	
	public void setCenter (String s)
	{	if (Cn==null) return;
		Center=new Expression("@\""+s+"\"",Cn,this);
	}
	
	public boolean isFilledForSelect ()
	{	return false;
	}
	
	public PointObject getCenter ()
	{	try
		{	return (PointObject)Center.getObject();
		}
		catch (Exception e)
		{	return null;
		}
	}

	public String getVar ()
	{	String vars=Var[0];
		for (int i=1; i<Var.length; i++) vars=vars+" "+Var[i];
		return vars;
	}

	public boolean canInteresectWith(ConstructionObject o) 
	{	return true;
	}

}
