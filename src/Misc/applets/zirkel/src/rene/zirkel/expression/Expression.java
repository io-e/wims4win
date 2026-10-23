package rene.zirkel.expression;

import java.util.Enumeration;

import rene.util.MyVector;
import rene.zirkel.*;
import rene.zirkel.construction.*;
import rene.zirkel.objects.*;

/*
This file contains classes for computing arithmetic expression. Those
classes might be useful elsewhere. However, variables evaluate to sizes of
construction objects or values of expressions here.
*/

/**
 * This class holds information abut the current position
 * in the scanned text.
 */
class ExpressionText
{	char A[];
	int N;
	Construction C;
	ConstructionObject O;
	DepList DL;
	String Var[];
	boolean NoCircles;
	public ExpressionText (String S, Construction c, ConstructionObject o,
		DepList dl, boolean nocircles)
	{	A=S.toCharArray();
		N=0;
		C=c;
		O=o;
		DL=dl;
		NoCircles=nocircles;
	}
	public void setVar (String t[])
	{	Var=t;
	}
	public boolean isVar (String var)
	{	if (Var==null) return false;
		for (int i=0; i<Var.length; i++)
			if (Var[i].equals(var)) return true;
		return false;
	}
	public char next (boolean quotes)
	{	if (!quotes) skipblanks();
		if (N<A.length) return A[N];
		else return (char)0;
	}
	public char next ()
	{	return next(false);
	}
	public char nextnext ()
	{	skipblanks();
		if (N+1<A.length) return A[N+1];
		else return (char)0;
	}
	public void advance (boolean quotes)
	{	if (!quotes) skipblanks();
		N++;
	}
	public void advance ()
	{	advance(false);
	}
	public Construction getConstruction ()
	{	return C;
	}
	public ConstructionObject getObject ()
	{	return O;
	}
	public DepList getDepList ()
	{	return DL;
	}
	public void skipblanks ()
	{	while (N<A.length && A[N]==' ') N++;
	}
	public boolean nocircles ()
	{	return NoCircles;
	}
}

/**
basic = (sum)
basic = elementary
*/

class BasicExpression
{	BasicExpression E;

	static final int MINUS=-1,PLUS=1;

	public static BasicExpression scan (ExpressionText t)
		throws ConstructionException
	{	if (t.next()=='-')
		{	t.advance();
			return scan(t,MINUS);
		}
		if (t.next()=='+')
		{	t.advance();
			return scan(t,PLUS);
		}
		if (t.next()=='(') return new BracketExpression(scanBracket(t));
		return ElementaryExpression.scan(t);
	}
	
	public static BasicExpression scan (ExpressionText t,
		int sign)
		throws ConstructionException
	{	if (sign==MINUS)
		{	if (t.next()=='(') return new MinusExpression(scanBracket(t));
			return new MinusExpression(ElementaryExpression.scan(t));
		}
		if (t.next()=='(') return scanBracket(t);
		return ElementaryExpression.scan(t);
	}
	
	public static BasicExpression scanBracket (ExpressionText t)
		throws ConstructionException
	{	t.advance();
		BasicExpression E=TopExpression.scan(t);
		if (t.next()==')') t.advance();
		else
			throw new ConstructionException(Zirkel.name("exception.bracket"));
		return E;
	}

	public double getValue () throws ConstructionException
	{	throw new ConstructionException("");
	}
	public void translate ()
	{
	}
	
	public boolean isNumber ()
	{	return false;
	}
	
	public void reset ()
	{
	}

	public boolean isLogical ()
	{	return false;
	}
}

/**
double = 34.45879
double = +34.45879
double = -34.45879
*/

class DoubleExpression extends BasicExpression
{	double X;

	public DoubleExpression (double x)
	{	X=x;
	}

	public static BasicExpression scan (ExpressionText t)
		throws ConstructionException
	{	double result=0;
		double Faktor=1;
		boolean Punkt=false,Digit=false;
		boolean Negative=false;
		if (t.next()=='+') t.advance();
		else if (t.next()=='-')
		{	Negative=true;
			t.advance();
		}
		while (true) 
		{	char c=t.next();
			if (c>='0' && c<='9')
			{	result=result*10+(c-'0');
				if (Punkt) Faktor=Faktor/10;
				t.advance();
				Digit=true;
			}
			else if (c=='.')
			{	if (Punkt)
					throw new ConstructionException(Zirkel.name("exception.dot"));
				Punkt=true;
				t.advance();
			}
			else break;
		}
		if (!Digit)
			throw new ConstructionException(Zirkel.name("exception.nodigit"));
		if (Negative) result=-result;
		return new DoubleExpression(Faktor*result);
	}
	
	public double getValue () throws ConstructionException
	{	return X;
	}

	public String toString ()
	{	String s=""+X;
		if (s.endsWith(".0")) s=s.substring(0,s.length()-2);
		return s;
	}
	
	public void translate ()
	{}
	
	public boolean isNumber ()
	{	return true;
	}
}

/**
object = @name
object = @"name"
*/

class FindObjectExpression extends ObjectExpression
	implements Translator
{	String Name;
	String Var=null;
	Construction C;
	ConstructionObject O=null; // found object
	
	public FindObjectExpression (String name, Construction c)
	{	super(null);
		Name=name;
		C=c;
	}
	
	public ConstructionObject getObject ()
	{	if (C==null) return null;
		if (O!=null && O.getConstruction()==C && O.isInConstruction()
				&& O.getName().equals(Name)) return O;
		return O=C.find(Name);
	}

	public double getValue () throws ConstructionException
	{	if (C==null)
			throw new InvalidException("");
		if (O!=null && O.getConstruction()==C && O.isInConstruction()
				&& O.getName().equals(Name)) return O.getValue();
		O=C.find(Name);
		if (O==null)
			throw new InvalidException("");
		return O.getValue();
	}

	public static BasicExpression scan (ExpressionText t, boolean quotes)
		throws ConstructionException
	{	if (!quotes && t.next()=='\"')
		{	t.advance();
			BasicExpression E=scan(t,true);
			if (t.next()!='\"')
				throw new ConstructionException(Zirkel.name("exception.quotes"));
			t.advance();
			return E;
		}
		StringBuffer b=new StringBuffer();
		while (true)
		{	char c=t.next(quotes);
			if (!(Character.isLetterOrDigit(c) 
			|| c=='\\' 
			|| c=='\''
			|| (quotes && c!='\"' && c!=0)
			|| (quotes && c==' '))) 
				break;
			b.append(c);
			t.advance(quotes);
		}
		if (!quotes && t.next()=='(')
			return FunctionExpression.scan(t,b.toString());
		String s=b.toString();
		return new FindObjectExpression(s,t.getConstruction());
	}
	
	public static BasicExpression scan (ExpressionText t)
		throws ConstructionException
	{	return scan(t,false);
	}
	
	public void translate ()
	{	C=C.getTranslation();
		C.addTranslator(this);
	}
	
	public String toString ()
	{	if (Var!=null) return Var;
		return "@"+ObjectExpression.quote(Name);
	}
	
	/**
	 * Used to translate expressions with @P in macros
	 */
	public void laterTranslate (Construction from)
	{	if (C==null) return;
		ConstructionObject o=from.find(Name);
		if (o==null || o.getTranslation()==null) 
		{	from.addError(ConstructionObject.text1(
				Zirkel.name("warning.macrodefinition"),Name));
			return;
		}
		Name=o.getTranslation().getName();
		O=null;
	}
}

/**
object = name
object = "name"
*/

class ObjectExpression extends BasicExpression
{	ConstructionObject O;
	String Var=null;
	public ObjectExpression (ConstructionObject o)
	{	O=o;
	}

	public double getValue () throws ConstructionException
	{	if (!O.valid())
			throw new InvalidException("");
		if (Var!=null && 
				(O instanceof FunctionObject)) return ((FunctionObject)O).getValue(Var);
		if (Var!=null && 
				(O instanceof UserFunctionObject)) return ((UserFunctionObject)O).getValue(Var);
		return O.getValue();
	}

	public ConstructionObject getObject ()
	{	return O;
	}
	public static BasicExpression scan (ExpressionText t, boolean quotes)
		throws ConstructionException
	{	if (!quotes && t.next()=='\"')
		{	t.advance();
			BasicExpression E=scan(t,true);
			if (t.next()!='\"')
				throw new ConstructionException(Zirkel.name("exception.quotes"));
			t.advance();
			return E;
		}
		StringBuffer b=new StringBuffer();
		while (true)
		{	char c=t.next(quotes);
			if (!(Character.isLetterOrDigit(c) 
			|| c=='\\' 
			|| c=='\''
			|| (quotes && c!='\"' && c!=0)
			|| (quotes && c==' '))) 
				break;
			b.append(c);
			t.advance(quotes);
		}
		if (!quotes && t.next()=='(')
			return FunctionExpression.scan(t,b.toString());
		String s=b.toString();
		if (s.equals("pi")) return new ConstantExpression("pi",Math.PI);
		if (s.equals("valid")) return new ValidExpression(true);
		if (s.equals("invalid")) return new ValidExpression(false);
		if (s.equals("this"))
		{	t.getDepList().add(t.getObject());
			return new ObjectExpression(t.getObject());
		}
		if (s.equals("windoww")) 
			return new ConstructionExpression(t.getConstruction(),
					ConstructionExpression.WIDTH);
		if (s.equals("windowh")) 
			return new ConstructionExpression(t.getConstruction(),
					ConstructionExpression.HEIGHT);
		if (s.equals("windowcx")) 
			return new ConstructionExpression(t.getConstruction(),
					ConstructionExpression.CENTERX);
		if (s.equals("windowcy")) 
			return new ConstructionExpression(t.getConstruction(),
					ConstructionExpression.CENTERY);
		if (s.equals("pixel")) 
			return new ConstructionExpression(t.getConstruction(),
					ConstructionExpression.PIXEL);
		if (t.isVar(s))
		{	ObjectExpression oe=new ObjectExpression(t.getObject());
			oe.Var=s;
			return oe;
		}
		ConstructionObject o=
			t.getConstruction().findInclusive(s,t.getObject());
		if (o==null)
		{	o=t.getConstruction().find(s);
			if (o==null)
			{	if (t.getConstruction().loading())
					return new FindObjectExpression(s,t.getConstruction());
				else
					throw new ConstructionException(Zirkel.name("exception.notfound")
							+" ("+s+")");
			}
			//System.out.println("---");
			//System.out.println(o.getName()+" "+t.getObject().getName());
			//System.out.println(t.getConstruction().dependsOn(o,t.getObject()));
			if (t.getConstruction().dependsOn(o,t.getObject()))
			{	if (t.nocircles())
					throw new ConstructionException(
							ConstructionObject.text1(Zirkel.name("exception.depends"),s));
				return new FindObjectExpression(s,t.getConstruction());
			}
			t.getConstruction().needsOrdering();
		}
		t.getDepList().add(o);
		return new ObjectExpression(o);
	}
	public static BasicExpression scan (ExpressionText t)
		throws ConstructionException
	{	if (t.next()=='@')
		{	t.advance();
			return FindObjectExpression.scan(t);
		}
		return scan(t,false);
	}
	public void translate ()
	{	O=O.getTranslation();
	}
	public String toString ()
	{	if (Var!=null) return Var;
		return quote(O.getName());
	}
	static public String quote (String name)
	{	boolean quotes=false;
		for (int i=0; i<name.length(); i++)
		{	if (!Character.isLetterOrDigit(name.charAt(i)))
			{	quotes=true;
				break;
			}
		}
		if (quotes) return "\""+name+"\"";
		else return name;
	}
}

/**
 * @author Rene
 * 
 * Evaluates a user funktion (a FunctionObject).
 * The Y-expression of this function is used.
 *
 */
class UserFunctionExpression extends BasicExpression
{	ConstructionObject F;
	BasicExpression E[];
	double X[];
	
	public UserFunctionExpression (ConstructionObject f, BasicExpression e[])
	{	F=f; 
		E=e; 
		X=new double[E.length];
	}
	
	public double getValue () throws ConstructionException
	{	for (int i=0; i<E.length; i++) X[i]=E[i].getValue();
		if (F instanceof FunctionObject) return ((FunctionObject)F).evaluateF(X);
		return ((UserFunctionObject)F).evaluateF(X);
	}
	
	public void translate ()
	{	for (int i=0; i<E.length; i++) E[i].translate();
		if (F instanceof FunctionObject) F=(FunctionObject)F.getTranslation();
		else F=(UserFunctionObject)F.getTranslation();
	}
	
	public void reset ()
	{	for (int i=0; i<E.length; i++) E[i].reset();
	}

	public String toString ()
	{	String ex=E[0].toString();
		for (int i=1; i<E.length; i++) ex=ex+","+E[i].toString();
		return F.getName()+"("+ex+")";
	}
}

class SimulationExpression extends BasicExpression
{	ConstructionObject SO;
	ConstructionObject Container,F;
	BasicExpression E;
	Construction C;
	
	public SimulationExpression (ConstructionObject so, ConstructionObject c,
		BasicExpression e, ConstructionObject f)
	{	SO=so; Container=c; E=e; F=f;
		C=c.getConstruction();
	}
	
	public double getValue () throws ConstructionException
	{	if (C.BlockSimulation)
			throw new ConstructionException(
				Zirkel.name("exception.simmulation.block")); 
		try
		{	double aa=E.getValue();
			C.BlockSimulation=true;
			((SimulationObject)SO).setSimulationValue(aa);
			F.getConstruction().validate(F,SO);
			double res=F.getValue();
			((SimulationObject)SO).resetSimulationValue();
			F.getConstruction().validate(F,SO);
			C.BlockSimulation=false;
			return res;
		}
		catch (Exception e)
		{	C.BlockSimulation=false;
			throw new ConstructionException("");
		}
	}
	
	public void translate ()
	{	E.translate();
		SO=SO.getTranslation(); 
		Container=Container.getTranslation();
		F=F.getTranslation();
	}
	
	public void reset ()
	{	E.reset();
	}
	
	public String toString ()
	{	return "simulate("+SO.getName()+","+E.toString()+","+F.getName()+")";
	}
}

/**
function = sin(parameter)
etc.
function = x(point)
function = y(point)
*/

class FunctionExpression extends BasicExpression
{	int F;
	BasicExpression E[];
	int NParams;
	
	public FunctionExpression (int f, BasicExpression e)
	{	F=f; 
		E=new BasicExpression[1]; 
		E[0]=e; NParams=1;
	}
	
	public FunctionExpression (int f, BasicExpression e, BasicExpression ee)
	{	F=f; 
		E=new BasicExpression[2]; 
		E[0]=e; E[1]=ee; 
		NParams=2;
	}
	
	public FunctionExpression (int f, BasicExpression e, BasicExpression ee,
		BasicExpression eee)
	{	F=f; 
		E=new BasicExpression[3]; 
		E[0]=e; E[1]=ee; E[2]=eee; 
		NParams=3;
	}
	
	final static String Functions[]=
		{	"sin","cos","tan","arcsin","arccos","arctan",
			"sqrt","exp","log","round","x","y","floor","ceil",
			"d","a","angle180","angle360","abs","scale","sign","d","sum",
			"if","deg","rad","integrate","zero","diff","min","max","length",
			"rsin","rcos","rtan","rarcsin","rarccos","rarctan",
			"sinhyp","coshyp","z","simulate","inside"
		};
	final static int NX=10,NY=11,ND=14,NA=15,NS=19,
		NSUM=22,NIF=23,NINT=26,NZERO=27,NDIFF=28,NMIN=29,NMAX=30,NLENGTH=31,
		NZ=40,NSIM=41,NINSIDE=42;
		
	public static BasicExpression scan (ExpressionText t, String name)
		throws ConstructionException
	{	int f=-1;
		for (int i=0; i<Functions.length; i++)
		{	if (name.equals(Functions[i])) { f=i; break; }
		}
		if (f<0)
		{	boolean forward=false;
			ConstructionObject o=t.getConstruction().find(name,t.getObject());
			if (o==null)
			{	o=t.getConstruction().find(name);
				forward=true;
			}
			if (o!=null && (o instanceof FunctionObject || o instanceof UserFunctionObject) && !(o==t.getObject())
					&& !t.getConstruction().dependsOn(o,t.getObject()))
			{	if (t.next()!='(')
					throw new ConstructionException(Zirkel.name("exception.parameter"));
				t.advance();
				MyVector ex=new MyVector();
				while (true)
				{	BasicExpression e=TopExpression.scan(t);
					ex.addElement(e);
					if (t.next()==')') break;
					if (t.next()!=',')
						throw new ConstructionException(Zirkel.name("exception.parameter"));
					t.advance();
				}
				t.advance();
				t.getDepList().add(o);
				if (forward) t.getConstruction().needsOrdering();
				BasicExpression exp[]=new BasicExpression[ex.size()];
				ex.copyInto(exp);
				return new UserFunctionExpression(o,exp);
			}
			throw new ConstructionException(Zirkel.name("exception.function")
				+" ("+name+")");
		}
		if (t.next()!='(')
				throw new ConstructionException(Zirkel.name("exception.parameter"));
		t.advance();
		BasicExpression e=TopExpression.scan(t);
		if (f==NX || f==NY || f==NZ)
		{	if (e instanceof FindObjectExpression)
				e=new FunctionExpression(f,e);
			else if (e instanceof ObjectExpression &&
				((ObjectExpression)e).getObject() instanceof PointObject)
				e=new FunctionExpression(f,e);
			else
				throw new ConstructionException(Zirkel.name("exception.parameter")
					+" ("+Functions[f]+")");		
		}
		else if (f==ND)
		{	if (t.next()!=',')
			{	e=new DExpression(e);
			}
			else
			{	t.advance();
				BasicExpression ee=TopExpression.scan(t);
				if (
					((e instanceof ObjectExpression &&
						((ObjectExpression)e).getObject() instanceof PointObject)
					|| e instanceof FindObjectExpression)
					 && 
					 ((ee instanceof ObjectExpression &&
						((ObjectExpression)ee).getObject() instanceof PointObject)
					|| ee instanceof FindObjectExpression)
					)
					e=new FunctionExpression(f,e,ee);
				else
					throw new ConstructionException(Zirkel.name("exception.parameter")
						+" ("+Functions[f]+")");
			}
		}
		else if (f==NA)
		{	if (t.next()!=',')
				throw new ConstructionException(Zirkel.name("exception.parameter"));
			t.advance();
			BasicExpression ee=TopExpression.scan(t);
			if (t.next()!=',')
				throw new ConstructionException(Zirkel.name("exception.parameter"));
			t.advance();
			BasicExpression eee=TopExpression.scan(t);
			if (
				((e instanceof ObjectExpression &&
					((ObjectExpression)e).getObject() instanceof PointObject)
				|| e instanceof FindObjectExpression)
				 && 
				 ((ee instanceof ObjectExpression &&
					((ObjectExpression)ee).getObject() instanceof PointObject)
				|| ee instanceof FindObjectExpression)
				 && 
				((eee instanceof ObjectExpression &&
					((ObjectExpression)eee).getObject() instanceof PointObject)
				|| eee instanceof FindObjectExpression)
				)
					e=new FunctionExpression(f,e,ee,eee);
			else
				throw new ConstructionException(Zirkel.name("exception.parameter")
					+" ("+Functions[f]+")");		
			
		}
		else if (f==NS)
		{	if (t.next()!=',')
				throw new ConstructionException(Zirkel.name("exception.parameter"));
			t.advance();
			BasicExpression ee=TopExpression.scan(t);
			if (t.next()!=',')
				throw new ConstructionException(Zirkel.name("exception.parameter"));
			t.advance();
			BasicExpression eee=TopExpression.scan(t);
			e=new FunctionExpression(f,e,ee,eee);			
		}
		else if (f==NSUM)
		{	if (t.next()!=',')
			{	e=new CumSumExpression(e);
			}
			else
			{	t.advance();
				BasicExpression ee=TopExpression.scan(t);
				e=new CumSumExpression(e,ee);
			}
		}
		else if (f==NIF)
		{	if (t.next()!=',')
				throw new ConstructionException(Zirkel.name("exception.parameter"));
			t.advance();
			BasicExpression ee=TopExpression.scan(t);
			if (t.next()!=',')
				throw new ConstructionException(Zirkel.name("exception.parameter"));
			t.advance();
			BasicExpression eee=TopExpression.scan(t);
			e=new IfExpression(e,ee,eee);
		}
		else if (f==NINT || f==NZERO || f==NMIN || f==NMAX || f==NLENGTH)
		{	if (!(e instanceof ObjectExpression))
			{	if (f==NMAX || f==NMIN)
				{	if (t.next()!=',')
						throw new ConstructionException(Zirkel.name("exception.parameter"));
					t.advance();
					BasicExpression ee=TopExpression.scan(t);
					e=new FunctionExpression(f,e,ee);
				}
				else
					throw new ConstructionException(Zirkel.name("exception.parameter")
							+" ("+Functions[f]+")");				
			}
			else
			{	boolean function=((ObjectExpression)e).getObject() instanceof FunctionObject
					|| ((ObjectExpression)e).getObject() instanceof TrackObject;
				if ((f==NINT || f==NLENGTH) && t.next()==')')
				{	if (!function)
							throw new ConstructionException(Zirkel.name("exception.parameter")
									+" ("+Functions[f]+")");				
					e=new FunctionExpression(f,e);
				}
				else if (f==NLENGTH)
				{	throw new ConstructionException(Zirkel.name("exception.parameter"));
				}
				else
				{	if (t.next()!=',')
						throw new ConstructionException(Zirkel.name("exception.parameter"));
					t.advance();
					BasicExpression ee=TopExpression.scan(t);
					if (function)
					{	if (t.next()!=',')
							throw new ConstructionException(Zirkel.name("exception.parameter"));
						t.advance();
						BasicExpression eee=TopExpression.scan(t);
						e=new FunctionExpression(f,e,ee,eee);
					}
					else if (f==NMIN || f==NMAX)
					{	e=new FunctionExpression(f,e,ee);
					}
					else
						throw new ConstructionException(Zirkel.name("exception.parameter")
								+" ("+Functions[f]+")");					
				}
			}
		}
		else if (f==NDIFF)
		{	if (!(e instanceof ObjectExpression) ||
				!(((ObjectExpression)e).getObject() instanceof FunctionObject))
					throw new ConstructionException(Zirkel.name("exception.parameter")
							+" ("+Functions[f]+")");
			if (t.next()!=',')
				throw new ConstructionException(Zirkel.name("exception.parameter"));
			t.advance();
			BasicExpression ee=TopExpression.scan(t);
			e=new FunctionExpression(f,e,ee);
		}
		else if (f==NSIM)
		{	if (t.next()!=',')
				throw new ConstructionException(Zirkel.name("exception.parameter"));
			t.advance();
			BasicExpression ee=TopExpression.scan(t);
			if (t.next()!=',')
				throw new ConstructionException(Zirkel.name("exception.parameter"));
			t.advance();
			BasicExpression eee=TopExpression.scan(t);
			if (
				(e instanceof ObjectExpression &&
					((ObjectExpression)e).getObject() instanceof SimulationObject)
				||
				!(eee instanceof ObjectExpression)
				)
			{	ConstructionObject SO=((ObjectExpression)e).getObject();
				if (t.getConstruction().dependsOn(SO,t.getObject()))
					throw new ConstructionException(Zirkel.name("exception.parameter")
							+" ("+Functions[f]+")");		
				e=new SimulationExpression(
					((ObjectExpression)e).getObject(),t.getObject(),ee,
					((ObjectExpression)eee).getObject());
			}
			else 
				throw new ConstructionException(Zirkel.name("exception.parameter")
						+" ("+Functions[f]+")");		
			
		}
		else if (f==NINSIDE)
		{	if (t.next()!=',')
				throw new ConstructionException(Zirkel.name("exception.parameter"));
			t.advance();
			BasicExpression ee=TopExpression.scan(t);
			if (
				((e instanceof ObjectExpression &&
					((ObjectExpression)e).getObject() instanceof PointObject)
				|| e instanceof FindObjectExpression)
				 && 
				 ((ee instanceof ObjectExpression &&
					((ObjectExpression)ee).getObject() instanceof InsideObject)
				|| ee instanceof FindObjectExpression)
				)
				e=new FunctionExpression(f,e,ee);
			else
				throw new ConstructionException(Zirkel.name("exception.parameter")
					+" ("+Functions[f]+")");
		}
		else
		{	e=new FunctionExpression(f,e);
		}
		if (t.next()!=')')
			throw new ConstructionException(Zirkel.name("exception.parameter"));
		t.advance();
		return e;
	}

	
	public double getValue () throws ConstructionException
	{	PointObject P,PP,PPP;
		switch (F)
		{	case NX : 
				P=getPoint(0);
				if (!P.valid())
					throw new InvalidException(Zirkel.name("exception.invalid"));
				return ((PointObject)P).getX();
			case NY :
				P=getPoint(0);
				if (!P.valid())
					throw new InvalidException(Zirkel.name("exception.invalid"));
				return P.getY();
			case NZ :
				P=getPoint(0);
				if (!P.valid())
					throw new InvalidException(Zirkel.name("exception.invalid"));
				return P.getZ();
			case ND :
				P=getPoint(0); PP=getPoint(1);
				if (!P.valid() || !PP.valid())
					throw new InvalidException(Zirkel.name("exception.invalid"));
				double dx=P.getX()-PP.getX();
				double dy=P.getY()-PP.getY();
				return Math.sqrt(dx*dx+dy*dy);
			case NA :
				P=getPoint(0); PP=getPoint(1); PPP=getPoint(2);
				if (!P.valid() || !PP.valid() || !PPP.valid())
					throw new InvalidException(Zirkel.name("exception.invalid"));
				dx=P.getX()-PP.getX();
				dy=P.getY()-PP.getY();
				double dx1=PPP.getX()-PP.getX();
				double dy1=PPP.getY()-PP.getY();
				double a=Math.atan2(dx,dy)-Math.atan2(dx1,dy1);
				a=a/Math.PI*180;
				if (a<0) a+=360;
				if (a>360) a-=360;
				return a;
			case NS :
				double x=E[0].getValue(),xa=E[1].getValue(),xb=E[2].getValue();
				if (x<xa || x>=xb || xb<=xa)
					throw new InvalidException(Zirkel.name("exception.invalid"));
				return (x-xa)/(xb-xa);
			case NINT :
				if (((ObjectExpression)E[0]).getObject() instanceof FunctionObject)
				{	FunctionObject F=(FunctionObject)(((ObjectExpression)E[0]).getObject());
					if (E.length>1)
					{	double aa=E[1].getValue(),bb=E[2].getValue();
						return Romberg.compute(F,aa,bb,10,1e-10,10);
					}
					else
					{	return F.getIntegral();
					}
				}
				else
				{	TrackObject TO=(TrackObject)(((ObjectExpression)E[0]).getObject());
					if (E.length>1)
					{	double aa=E[1].getValue(),bb=E[2].getValue();
						return TO.getSum(aa,bb);
					}
					else return TO.getSum();
				}					
			case NLENGTH :
				if (((ObjectExpression)E[0]).getObject() instanceof FunctionObject)
				{	FunctionObject F=(FunctionObject)(((ObjectExpression)E[0]).getObject());
					return F.getLength();
				}
				else
				{	TrackObject TO=(TrackObject)(((ObjectExpression)E[0]).getObject());
					return TO.getLength();
				}	
			case NZERO :
				FunctionObject F=(FunctionObject)(((ObjectExpression)E[0]).getObject());
				double aa=E[1].getValue(),bb=E[2].getValue();
				return Secant.compute(F,aa,bb,1e-10);
			case NDIFF :
				F=(FunctionObject)(((ObjectExpression)E[0]).getObject());
				aa=E[1].getValue();
				return 
					(F.evaluateF(aa+1e-3)-F.evaluateF(aa-1e-3))/2e-3;	
			case NMIN :
				if (NParams==2)
				{	return Math.min(E[0].getValue(),E[1].getValue());
				}
				else
				{	F=(FunctionObject)(((ObjectExpression)E[0]).getObject());
					aa=E[1].getValue(); bb=E[2].getValue();
					return ConvexMin.computeMin(F,aa,bb,1e-10);
				}
			case NMAX :
				if (NParams==2)
				{	return Math.max(E[0].getValue(),E[1].getValue());
				}
				else
				{	F=(FunctionObject)(((ObjectExpression)E[0]).getObject());
					aa=E[1].getValue(); bb=E[2].getValue();
					return ConvexMin.computeMax(F,aa,bb,1e-10);
				}
			case NINSIDE :
				P=getPoint(0);
				InsideObject IO=(InsideObject)(((ObjectExpression)E[1]).getObject());
				return IO.containsInside(P);
		}
		double x=E[0].getValue();
		switch (F)
		{	case 0 : return Math.sin(x/180*Math.PI);
			case 1 : return Math.cos(x/180*Math.PI);
			case 2 : return Math.tan(x/180*Math.PI);
			case 3 : return Math.asin(x)/Math.PI*180;
			case 4 : return Math.acos(x)/Math.PI*180;
			case 5 : return Math.atan(x)/Math.PI*180;
			case 6 : return Math.sqrt(x);
			case 7 : return Math.exp(x);
			case 8 : return Math.log(x);
			case 9 : return Math.round(x);
			case 12 : return Math.floor(x);
			case 13 : return Math.ceil(x);
			case 16 :
				x=x/360;
				x-=Math.floor(x);
				x=x*360;
				if (x<180) return x;
				else return (x-360);
			case 17 :
				x/=360;
				x-=Math.floor(x);
				return x*360;
			case 18 :
				return Math.abs(x);
			case 20 :
				if (x>0) return 1;
				else if (x==0) return 0;
				else return -1;
			case 24 : return (x/Math.PI*180);
			case 25 : return (x/180*Math.PI);
			case 32 : return Math.sin(x);
			case 33 : return Math.cos(x);
			case 34 : return Math.tan(x);
			case 35 : return Math.asin(x);
			case 36 : return Math.acos(x);
			case 37 : return Math.atan(x);
			case 38 : return (Math.exp(x)-Math.exp(-x))/2;
			case 39 : return (Math.exp(x)+Math.exp(-x))/2;
		}
		throw new ConstructionException("");
	}
	
	public PointObject getPoint (int n)
		throws ConstructionException
	{	PointObject p;
		try
		{	p=(PointObject)((ObjectExpression)E[n]).getObject();
		}
		catch (Exception e)
		{	throw new ConstructionException("exception.notfound");
		}
		if (!p.valid()) throw new ConstructionException("exception.invalid");
		return p;
	}
	
	public void translate ()
	{	for (int i=0; i<NParams; i++)
			E[i].translate();
	}
	
	public void reset ()
	{	for (int i=0; i<NParams; i++)
			E[i].reset();
	}

	public String toString ()
	{	String s=Functions[F]+"(";
		for (int i=0; i<NParams; i++)
		{	if (i>0) s=s+",";
			s=s+E[i];
		}
		return s+")";
	}
}

class DExpression extends FunctionExpression
{	double Old=0;
	double Old1=0;
	boolean start=false;
	
	public DExpression (BasicExpression e)
	{	super(21,e);
	}
	
	public double getValue () throws ConstructionException
	{	if (E[0] instanceof ObjectExpression)
		{	ConstructionObject o=((ObjectExpression)E[0]).getObject();
			if (o instanceof PointObject)
			{	PointObject p=(PointObject)o;
				if (p.dontUpdate()) return 0;
				double x=p.getX(),y=p.getY();
				if (start)
				{	double res=(x-Old)*(x-Old)+(y-Old1)*(y-Old1);
					Old=x; Old1=y;
					return Math.sqrt(res);
				}
				Old=x; Old1=y;
				start=true;
				return 0;
			}
			else if (o instanceof AngleObject)
			{	AngleObject a=(AngleObject)o;
				double x=a.getValue();
				if (start)
				{	double res=x-Old;
					if (res<-180) res+=180;
					if (res>180) res-=180;
					Old=x;
					return res;
				}
				Old=x;
				start=true;
				return 0;
			}
		}
		double x=E[0].getValue();
		double res;
		if (start)
		{	res=x-Old;
		}
		else
		{	res=0;
			start=true;
		}
		Old=x;
		return res;
	}
	
	public void reset ()
	{	start=false;
		super.reset();
	}
}

class CumSumExpression extends FunctionExpression
{	double sum=0;

	public CumSumExpression (BasicExpression e, BasicExpression ee)
	{	super(22,e,ee);
	}

	public CumSumExpression (BasicExpression e)
	{	super(22,e);
	}

	public double getValue () throws ConstructionException
	{	if (NParams>1) 
			try
			{	double x=E[1].getValue();
				if (x<0)
				{	reset(); return sum;
				}
			}
			catch (Exception e)
			{	sum=0; return sum;
			}
		sum+=E[0].getValue();
		return sum;
	}
	
	public void reset ()
	{	sum=0;
		super.reset();
	}	
}

class IfExpression extends FunctionExpression
{	public IfExpression (BasicExpression e, BasicExpression ee,
		BasicExpression eee)
	{	super(23,e,ee,eee);
	}

	public double getValue ()
		throws ConstructionException
	{	double v=0;
		try
		{	v=E[0].getValue();
		}
		catch (NoValueException e)
		{	if (e.isValid())
				return E[1].getValue();
			else
				return E[2].getValue();
		}
		catch (ConstructionException e)
		{	return E[2].getValue();
		}
		if (E[0].isLogical())
		{	if (v==0) return E[2].getValue();
			else return E[1].getValue();	
		}
		return E[1].getValue();
	}
}

/**
constant = pi
*/

class ConstantExpression extends BasicExpression
{	double X;
	String S;
	public ConstantExpression (String s, double x)
	{	X=x; S=s;
	}
	public double getValue ()
		throws ConstructionException
	{	return X;
	}
	public void translate () {}
	public String toString ()
	{	return S;
	}

}

/**
constant depending on construction
*/

class ConstructionExpression extends BasicExpression
{	int Type;
	Construction C;
	final static int CENTERX=0,CENTERY=1,WIDTH=2,HEIGHT=3,PIXEL=4;
	public ConstructionExpression (Construction c, int type)
	{	Type=type; C=c;
	}
	public double getValue ()
		throws ConstructionException
	{	if (C==null) return 0;
		switch (Type)
		{	case WIDTH : return C.getW();
			case CENTERX : return C.getX();
			case CENTERY : return C.getY();
			case HEIGHT : return C.getH();
			case PIXEL : return C.getPixel();
		}
		return 0;
	}
	public void translate ()
	{	C=C.getTranslation();
	}
	public String toString ()
	{	switch (Type)
		{	case WIDTH: return "windoww";
			case CENTERX: return "windowcx";
			case CENTERY: return "windowcy";
			case HEIGHT : return "windowh";
			case PIXEL : return "pixel";
		}
		return "";
	}

}

/**
constant = valid,invalid
*/

class ValidExpression extends BasicExpression
{	boolean V;
	public ValidExpression (boolean valid)
	{	V=valid;
	}
	public double getValue ()
		throws ConstructionException
	{	if (V) return 0;
		else throw new ConstructionException("");
	}
	public void translate () {}
	public String toString ()
	{	if (V) return "valid";
		else return "invalid";
	}

}

/**
Bracket = (Basic)
*/

class BracketExpression extends ElementaryExpression
{	BasicExpression E;

	public BracketExpression (BasicExpression e)
	{	E=e;
	}
	
	public double getValue ()
		throws ConstructionException
	{	return E.getValue();
	}
	
	public void translate ()
	{	E.translate();
	}
	
	public String toString ()
	{	return "("+E.toString()+")";
	}
	
	public boolean isNumber ()
	{	return E.isNumber();
	}

	public void reset ()
	{	E.reset();
	}
}


/**
element = double
element = object (with value)
*/

abstract class ElementaryExpression extends BasicExpression
{	public static BasicExpression scan (ExpressionText t)
		throws ConstructionException
	{	try
		{	BasicExpression E=DoubleExpression.scan(t);
			return E;
		}
		catch (Exception e) {}
		try
		{	BasicExpression E=ObjectExpression.scan(t);
			return E;
		}
		catch (ConstructionException e) { throw e; }
		catch (Exception e) {}
		throw new ConstructionException(Zirkel.name("exception.elementary"));
	}
	abstract public boolean isNumber ();
	abstract public double getValue () throws ConstructionException;
}

/**
* E^E
*/

class PotenzExpression extends BasicExpression
{	BasicExpression E1,E2;

	public PotenzExpression (BasicExpression e1, BasicExpression e2)
	{	E1=e1; E2=e2;
	}

	public static BasicExpression scan (ExpressionText t)
		throws ConstructionException
	{	BasicExpression E1=BasicExpression.scan(t);
		if (t.next()=='^' || (t.next()=='*' && t.nextnext()=='*'))
		{	if (t.next()=='*') t.advance();
			t.advance();
			return scan(t,E1);
		}
		return E1;
	}
	
	public static BasicExpression scan (ExpressionText t, BasicExpression E)
		throws ConstructionException
	{	BasicExpression E1=BasicExpression.scan(t);
		if (t.next()=='^' || (t.next()=='*' && t.nextnext()=='*'))
		{	if (t.next()=='*') t.advance();
			t.advance();
			return scan(t,new PotenzExpression(E,E1));
		}
		return new PotenzExpression(E,E1);
	}
	
	public double getValue ()
		throws ConstructionException
	{	return Math.pow(E1.getValue(),E2.getValue());
	}
	
	public void translate ()
	{	E1.translate(); E2.translate();
	}
	public String toString ()
	{	return E1+"^"+E2;
	}

	public void reset ()
	{	E1.reset();
		E2.reset();
	}
}

/**
* -E
*/

class MinusExpression extends ElementaryExpression
{	BasicExpression E;

	public MinusExpression (BasicExpression e)
	{	E=e;
	}
	
	public double getValue ()
		throws ConstructionException
	{	return -E.getValue();
	}
	
	public static BasicExpression scan (ExpressionText t)
		throws ConstructionException
	{	if (t.next()!='-') return PotenzExpression.scan(t);
		t.advance();
		BasicExpression E1=PotenzExpression.scan(t);
		return new MinusExpression(E1);
	}
	
	public void translate ()
	{	E.translate();
	}
	
	public String toString ()
	{	return "-"+E.toString();
	}
	
	public boolean isNumber ()
	{	return E.isNumber();
	}

	public void reset ()
	{	E.reset();
	}
}


/**
* E/E
*/

class QuotientExpression extends BasicExpression
{	BasicExpression E1,E2;

	public QuotientExpression (BasicExpression e1, BasicExpression e2)
	{	E1=e1; E2=e2;
	}

	public static BasicExpression scan (ExpressionText t, BasicExpression E)
		throws ConstructionException
	{	BasicExpression E1=MinusExpression.scan(t);
		if (t.next()=='/')
		{	t.advance();
			return scan(t,new QuotientExpression(E,E1));
		}
		else if (t.next()=='*' && t.nextnext()!='*')
		{	t.advance();
			return ProductExpression.scan(t,new QuotientExpression(E,E1));
		}
		return new QuotientExpression(E,E1);
	}
	
	public double getValue ()
		throws ConstructionException
	{	return E1.getValue()/E2.getValue();
	}

	public void translate ()
	{	E1.translate(); E2.translate();
	}
	public String toString ()
	{	return E1+"/"+E2;
	}
	public void reset ()
	{	E1.reset();
		E2.reset();
	}
}

/**
* E*E
*/
class ProductExpression extends BasicExpression
{	BasicExpression E1,E2;

	public ProductExpression (BasicExpression e1, BasicExpression e2)
	{	E1=e1; E2=e2;
	}

	public static BasicExpression scan (ExpressionText t)
		throws ConstructionException
	{	BasicExpression E1=MinusExpression.scan(t);
		if (t.next()=='*')
		{	t.advance();
			return scan(t,E1);
		}
		else if (t.next()=='/')
		{	t.advance();
			return QuotientExpression.scan(t,E1);
		}
		return E1;
	}
	
	public static BasicExpression scan (ExpressionText t, BasicExpression E)
		throws ConstructionException
	{	BasicExpression E1=MinusExpression.scan(t);
		if (t.next()=='*' && t.nextnext()!='*')
		{	t.advance();
			return scan(t,new ProductExpression(E,E1));
		}
		else if (t.next()=='/')
		{	t.advance();
			return QuotientExpression.scan(t,new ProductExpression(E,E1));
		}
		return new ProductExpression(E,E1);
	}
	
	public double getValue ()
		throws ConstructionException
	{	return E1.getValue()*E2.getValue();
	}
	
	public void translate ()
	{	E1.translate(); E2.translate();
	}
	
	public String toString ()
	{	return E1+"*"+E2;
	}
	public void reset ()
	{	E1.reset();
		E2.reset();
	}
}

/**
* E-E
*/

class DifferenceExpression extends BasicExpression
{	BasicExpression E1,E2;

	public DifferenceExpression (BasicExpression e1, BasicExpression e2)
	{	E1=e1; E2=e2;
	}

	public static BasicExpression scan (ExpressionText t, BasicExpression E)
		throws ConstructionException
	{	BasicExpression E1=ProductExpression.scan(t);
		if (t.next()=='+')
		{	t.advance();
			return SumExpression.scan(t,new DifferenceExpression(E,E1));
		}
		else if (t.next()=='-')
		{	t.advance();
			return scan(t,new DifferenceExpression(E,E1));
		}
		return new DifferenceExpression(E,E1);
	}
	
	public double getValue ()
		throws ConstructionException
	{	return E1.getValue()-E2.getValue();
	}

	public void translate ()
	{	E1.translate(); E2.translate();
	}

	public String toString ()
	{	return E1+"-"+E2;
	}

	public void reset ()
	{	E1.reset();
		E2.reset();
	}
}

 /**
 * E+E 
 */

class SumExpression extends BasicExpression
{	BasicExpression E1,E2;

	public SumExpression (BasicExpression e1, BasicExpression e2)
	{	E1=e1; E2=e2;
	}

	public static BasicExpression scan (ExpressionText t)
		throws ConstructionException
	{	BasicExpression E1=ProductExpression.scan(t);
		if (t.next()=='+')
		{	t.advance();
			return scan(t,E1);
		}
		else if (t.next()=='-')
		{	t.advance();
			return DifferenceExpression.scan(t,E1);
		}
		return E1;
	}
	
	public static BasicExpression scan (ExpressionText t, BasicExpression E)
		throws ConstructionException
	{	BasicExpression E1=ProductExpression.scan(t);
		if (t.next()=='+')
		{	t.advance();
			return scan(t,new SumExpression(E,E1));
		}
		else if (t.next()=='-')
		{	t.advance();
			return DifferenceExpression.scan(t,new SumExpression(E,E1));
		}
		return new SumExpression(E,E1);
	}

	public double getValue ()
		throws ConstructionException
	{	return E1.getValue()+E2.getValue();
	}

	public void translate ()
	{	E1.translate(); E2.translate();
	}

	public String toString ()
	{	return E1+"+"+E2;
	}

	public void reset ()
	{	E1.reset();
		E2.reset();
	}
}
	
 /**
 * Diverse Vegleiche.
 */

class CompareExpression extends BasicExpression
{	BasicExpression E1,E2;
	int Operator;
	final static int LESS=1,LESSEQUAL=2,GREATER=3,GREATEREQUAL=4,
		EQUAL=5,ABOUTEQUAL=6;

	public CompareExpression (BasicExpression e1, BasicExpression e2, int op)
	{	E1=e1; E2=e2; Operator=op;
	}

	public static BasicExpression scan (ExpressionText t)
		throws ConstructionException
	{	BasicExpression E1=SumExpression.scan(t);
		char c=t.next();
		int op=0;
		if (c=='<')
		{	t.advance();
			if (t.next(true)=='=')
			{	op=LESSEQUAL;
				t.advance();
			}
			else op=LESS;
		}
		else if (c=='>')
		{	t.advance();
			if (t.next(true)=='=')
			{	op=GREATEREQUAL;
				t.advance();
			}
			else op=GREATER;
		}
		else if (c=='=')
		{	t.advance();
			if (t.next(true)=='=') t.advance();
			op=EQUAL;
		}
		else if (c=='~')
		{	t.advance();
			if (t.next(true)=='=') t.advance();
			op=ABOUTEQUAL;
		}
		else return E1;
		BasicExpression E2=SumExpression.scan(t);
		return new CompareExpression(E1,E2,op);
	}
	
	public double getValue ()
		throws ConstructionException
	{	switch (Operator)
		{	case LESS :
				return logical(E1.getValue()<E2.getValue());
			case LESSEQUAL :
				return logical(E1.getValue()<=E2.getValue());
			case GREATER :
				return logical(E1.getValue()>E2.getValue());
			case GREATEREQUAL :
				return logical(E1.getValue()>=E2.getValue());
			case EQUAL :
				return logical(E1.getValue()==E2.getValue());
			case ABOUTEQUAL :
				return logical(
					Math.abs(E1.getValue()-E2.getValue())<1e-10);
		}
		return 0.0;
	}
	
	public double logical (boolean flag)
	{	if (flag) return 1.0;
		else return 0.0;
	}

	public void translate ()
	{	E1.translate(); E2.translate();
	}

	public String toString ()
	{	switch (Operator)
		{	case LESS : return E1+"<"+E2;
			case LESSEQUAL : return E1+"<="+E2;
			case GREATER : return E1+">"+E2;
			case GREATEREQUAL : return E1+">="+E2;
			case EQUAL : return E1+"=="+E2;
			case ABOUTEQUAL : return E1+"~="+E2;
		}
		return "";
	}

	public void reset ()
	{	E1.reset(); E2.reset();
	}
	
	public boolean isLogical ()
	{	return true;
	}
}

/**
 * ! CE
 */

class NotExpression extends BasicExpression
{	BasicExpression E1;

	public NotExpression (BasicExpression e1)
	{	E1=e1;
	}

	public static BasicExpression scan (ExpressionText t)
		throws ConstructionException
	{	if (t.next()!='!') return CompareExpression.scan(t);
		t.advance();
		BasicExpression E1=CompareExpression.scan(t);
		return new NotExpression(E1);
	}
	
	public double getValue ()
		throws ConstructionException
	{	if (E1.getValue()==0.0)
			return 1.0;
		else
			return 0.0;
	}

	public void translate ()
	{	E1.translate();
	}

	public String toString ()
	{	return "!"+E1;
	}

	public void reset ()
	{	E1.reset();
	}

	public boolean isLogical ()
	{	return true;
	}
}

/**
/* E & E
*/

class AndExpression extends BasicExpression
{	BasicExpression E1,E2;

	public AndExpression (BasicExpression e1, BasicExpression e2)
	{	E1=e1; E2=e2;
	}

	public static BasicExpression scan (ExpressionText t)
		throws ConstructionException
	{	BasicExpression E1=NotExpression.scan(t);
		if (t.next()=='&')
		{	t.advance();
			if (t.next(true)=='&') t.advance();
			return scan(t,E1);
		}
		return E1;
	}
	
	public static BasicExpression scan (ExpressionText t, BasicExpression E)
		throws ConstructionException
	{	BasicExpression E1=NotExpression.scan(t);
		if (t.next()=='&')
		{	t.advance();
			return scan(t,new AndExpression(E,E1));
		}
		return new AndExpression(E,E1);
	}
	
	public double getValue ()
		throws ConstructionException
	{	if (E1.getValue()!=0.0 && E2.getValue()!=0.0)
			return 1.0;
		else
			return 0.0;
	}

	public void translate ()
	{	E1.translate(); E2.translate();
	}

	public String toString ()
	{	return E1+"&&"+E2;
	}

	public void reset ()
	{	E1.reset();
		E2.reset();
	}

	public boolean isLogical ()
	{	return true;
	}
}

/**
* E | E
*/

class OrExpression extends BasicExpression
{	BasicExpression E1,E2;

	public OrExpression (BasicExpression e1, BasicExpression e2)
	{	E1=e1; E2=e2;
	}

	public static BasicExpression scan (ExpressionText t)
		throws ConstructionException
	{	BasicExpression E1=AndExpression.scan(t);
		if (t.next()=='|')
		{	t.advance();
			if (t.next(true)=='|') t.advance();
			return scan(t,E1);
		}
		return E1;
	}
	
	public static BasicExpression scan (ExpressionText t, BasicExpression E)
		throws ConstructionException
	{	BasicExpression E1=AndExpression.scan(t);
		if (t.next()=='|')
		{	t.advance();
			if (t.next(true)=='|') t.advance();
			return scan(t,new OrExpression(E,E1));
		}
		return new OrExpression(E,E1);
	}
	
	public double getValue ()
		throws ConstructionException
	{	if (E1.getValue()!=0.0 || E2.getValue()!=0.0)
			return 1.0;
		else
			return 0.0;
	}

	public void translate ()
	{	E1.translate(); E2.translate();
	}

	public String toString ()
	{	return E1+"||"+E2;
	}

	public void reset ()
	{	E1.reset();
		E2.reset();
	}

	public boolean isLogical ()
	{	return true;
	}
}

class TopExpression extends BasicExpression
{	
	public static BasicExpression scan (ExpressionText t)
		throws ConstructionException
	{	return OrExpression.scan(t);
	}	
}

public class Expression
{	String S;
	BasicExpression E;
	boolean Valid;
	String ErrorText;
	DepList DL;
	double Value;
	boolean HasDoubleValue=false;
	boolean ForcePlus=false;
	
	/**
	 * Create a new expression
	 * @param s expression string
	 * @param c construction to search for objects
	 * @param o "this" object
	 * @param var variable names used in the referring object
	 * @param nocircles prevent circular exressions
	 */
	public Expression (String s, Construction c, ConstructionObject o, String var[], 
			boolean nocircles)
	{	if (s.startsWith("+"))
		{	ForcePlus=true;
			s=s.substring(1);
		}
		S=s;
		DL=new DepList();
		try
		{	ExpressionText t=new ExpressionText(s,c,o,DL,nocircles);
			t.setVar(var);
			E=TopExpression.scan(t);
			if (t.next()!=0)
				throw new ConstructionException(
					Zirkel.name("exception.superfluous"));
			Valid=true;
		}
		catch (Exception e)
		{	Valid=false; 
			ErrorText=(e.toString());
			//e.printStackTrace();
		}
	}
	
	public Expression (String s, Construction c, ConstructionObject o, String var[])
	{	this(s,c,o,var,false);
	}
	
	public Expression (String s, Construction c, ConstructionObject o)
	{	this(s,c,o,null,false);
	}
	
	public boolean isValid ()
	{	return Valid;
	}
	public String getErrorText ()
	{	return ErrorText;
	}
	public double getValue () throws ConstructionException
	{	if (HasDoubleValue) return Value;
		if (!Valid)
			throw new ConstructionException(Zirkel.name("exception.expression"));
		double x=E.getValue();
		if (Double.isNaN(x) || Double.isInfinite(x))
			throw new ConstructionException(Zirkel.name("exception.value"));
		return x;
	}

	public void setValue (double x)
	{	HasDoubleValue=true;
		ForcePlus=false;
		DL=new DepList();
		Value=x;
	}
	
	public String toString ()
	{	if (HasDoubleValue) return ""+Value;
		if (Valid) return E.toString();
		else return S;
	}
	
	public DepList getDepList ()
	{	return DL;
	}
	
	public void translate ()
	{	E.translate();
		DL.translate();
	}
	
	public boolean isNumber ()
	{	return HasDoubleValue || E.isNumber();
	}
	
	public void reset ()
	{	if (E!=null && Valid) E.reset();
	}

	public boolean isForcePlus () { return ForcePlus; }
	public void setForcePlus (boolean flag) { ForcePlus=flag; }

	public ConstructionObject getObject ()
	{	if (E instanceof ObjectExpression) return ((ObjectExpression)E).getObject();
		else return null;
	}

	/**
	 * Add the objects this expression depends on to the
	 * static dependency list of the current object.
	 */
	public void addDep (ConstructionObject o)
	{	Enumeration e=getDepList().elements();
		while (e.hasMoreElements())
		{	ConstructionObject.DL.add((ConstructionObject)e.nextElement());
		}
	}
}
