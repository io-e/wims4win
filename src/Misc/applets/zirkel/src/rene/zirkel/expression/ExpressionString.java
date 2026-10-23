/*
 * Created on 13.03.2006
 */
package rene.zirkel.expression;

import java.util.Enumeration;

import rene.util.MyVector;
import rene.zirkel.objects.ConstructionObject;

class EquationExpressionString
{	public Expression E;
	public EquationExpressionString (Expression o)
	{	E=o;
	}
	public String toString ()
	{	return E.toString(); 
	}
	public String getEquation ()
	{	ConstructionObject o=E.getObject();
		if (o==null) return "???";
		else return o.getEquation();
	}
	public void translate ()
	{	E.translate();
	}
	public void addDep (ConstructionObject o)
	{	E.addDep(o);
	}
}

class NameExpressionString
{	public Expression E;
	public NameExpressionString (Expression o)
	{	E=o;
	}
	public String toString ()
	{	return E.toString(); 
	}
	public String getName ()
	{	ConstructionObject o=E.getObject();
		if (o==null) return "???";
		else return o.getName();
	}
	public void translate ()
	{	E.translate();
	}
	public void addDep (ConstructionObject o)
	{	E.addDep(o);
	}
}

/**
 * Class containing a chain of strings and expressions
 * for alias and text lines.
 */
public class ExpressionString 
{	MyVector v=new MyVector(3); // Strings or Expressions
	ConstructionObject O;

	public ExpressionString (String s, ConstructionObject o)
	{	O=o;
		v.removeAllElements();
		int h1=0;
		int n;
		while ((n=s.indexOf("%",h1))>=0)
		{	int h2=s.indexOf("%",n+1);
			if (h2==n+1) // found %%
			{	v.addElement(s.substring(h1,n));
				v.addElement("%");
				h1=n+2;
			}
			else if (h2>=0) // found %...%
			{	v.addElement(s.substring(h1,n));
				if (s.charAt(n+1)=='~') // found %~...%
				{	String name=s.substring(n+2,h2);
					v.addElement(new EquationExpressionString(
							new Expression(s.substring(n+2,h2),O.getConstruction(),O)));
					h1=h2+1;
				}
				else if (s.charAt(n+1)=='=') // found %=...%
				{	String name=s.substring(n+2,h2);
					v.addElement(new NameExpressionString(
							new Expression(s.substring(n+2,h2),O.getConstruction(),O)));
					h1=h2+1;
				}
				else
				{	Expression ex=new Expression(s.substring(n+1,h2),
						O.getConstruction(),O);
					v.addElement(ex);
					h1=h2+1;
				}
			}
			else // only one %
			{	v.addElement(s.substring(h1,n));
				v.addElement("%");
				h1=n+1;
			}
		}
		if (!s.equals("")) v.addElement(s.substring(h1));
		// System.out.println(evaluate());
	}

	/**
	 * Get the original string back (but all objects with
	 * correct current names).
	 */
	public String toString ()
	{	StringBuffer sb=new StringBuffer();
		Enumeration e=v.elements();
		while (e.hasMoreElements())
		{	Object o=e.nextElement();
			if (o instanceof String)
			{	if (((String)o).equals("%")) sb.append("%%");
				else sb.append((String)o); 
			}
			else if (o instanceof Expression)
			{	sb.append("%");
				if (((Expression)o).isForcePlus()) sb.append("+");
				sb.append(o.toString()); 
				sb.append("%");
			}
			else if (o instanceof EquationExpressionString)
			{	sb.append("%~");
				sb.append(((EquationExpressionString)o).toString());
				sb.append("%");
			}
			else if (o instanceof NameExpressionString)
			{	sb.append("%=");
				sb.append(((NameExpressionString)o).toString());
				sb.append("%");
			}
		}
		return sb.toString();
	}
	
	/**
	 * Get the string with all expressions evaluated.
	 * @return
	 */
	public String evaluate ()
	{	StringBuffer sb=new StringBuffer();
		Enumeration e=v.elements();
		while (e.hasMoreElements())
		{	Object o=e.nextElement();
			if (o instanceof String)
			{	if (((String)o).equals("%")) sb.append("%");
				else sb.append((String)o); 
			}
			else if (o instanceof Expression)
			{	try
				{	double value=((Expression)o).getValue();
					value=O.round(value);
					if (((Expression)o).isForcePlus() && value>=0) sb.append("+");
					if (value==Math.floor(value+0.5))
						sb.append((int)(value));
					else
						sb.append(O.round(value)); 
				}
				catch (Exception exc)
				{	sb.append("???");
				}
			}
			else if (o instanceof EquationExpressionString)
			{	sb.append(((EquationExpressionString)o).getEquation());
			}
			else if (o instanceof NameExpressionString)
			{	sb.append(((NameExpressionString)o).getName());
			}
		}
		return sb.toString();
	}
	
	/**
	 * Translate the string for macros.
	 */
	public void translate ()
	{	Enumeration e=v.elements();
		while (e.hasMoreElements())
		{	Object o=e.nextElement();
			if (o instanceof Expression)
			{	((Expression)o).translate();
			}
			else if (o instanceof EquationExpressionString)
			{	((EquationExpressionString)o).translate();
			}
			else if (o instanceof NameExpressionString)
			{	((NameExpressionString)o).translate();
			}
		}
	}
	
	/**
	 * Add all objcts this string depends on to the DL of O.
	 * @param O
	 */
	public void addDep (ConstructionObject oc)
	{	Enumeration e=v.elements();
		while (e.hasMoreElements())
		{	Object o=e.nextElement();
			if (o instanceof Expression)
			{	((Expression)o).addDep(oc);
			}
			else if (o instanceof EquationExpressionString)
			{	((EquationExpressionString)o).addDep(oc);
			}
			else if (o instanceof NameExpressionString)
			{	((NameExpressionString)o).addDep(oc);
			}
		}
	}
}
