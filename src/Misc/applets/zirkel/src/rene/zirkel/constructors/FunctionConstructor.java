package rene.zirkel.constructors;

// file: QuadricConstructor.java

import rene.util.xml.*;
import rene.zirkel.*;
import rene.zirkel.construction.*;
import rene.zirkel.expression.Expression;
import rene.zirkel.objects.*;

public class FunctionConstructor extends ObjectConstructor
{	public boolean construct (XmlTree tree, Construction c)
		throws ConstructionException
	{	if (!testTree(tree,"Function")) return false;
		XmlTag tag=tree.getTag();
		if (tag.hasParam("f")) // function
		{	if (!tag.hasParam("var"))
					throw new ConstructionException("Function invalid!");
			try
			{	String y=tag.getValue("f");
				String var=tag.getValue("var");
				UserFunctionObject f=new UserFunctionObject(c);
				f.setDefaults();
				setName(tag,f);
				set(tree,f);
				c.add(f);
				double xpos,ypos;
				try
				{	if (tag.hasParam("fixed"))
					{	f.setFixed(tag.getValue("x"),tag.getValue("y"));
					}
					else
					{	xpos=new Expression(tag.getValue("x"),c,f).getValue();
						ypos=new Expression(tag.getValue("y"),c,f).getValue();
						f.move(xpos,ypos);
					}
				}
				catch (Exception e) {}
				if (tag.hasParam("filled")) f.setFilled(true);
				f.setExpressions(var,y);
				setConditionals(tree,c,f);
				f.updateText();
				return true;
			}
			catch (Exception e)
			{	throw new ConstructionException("Function invalid!");
			}
		}
		else // curve
		{	if (!tag.hasParam("min") &&
				!tag.hasParam("max") &&
				!tag.hasParam("d") &&
				!tag.hasParam("var") &&
				!tag.hasParam("x") &&
				!tag.hasParam("y"))
					throw new ConstructionException("Function invalid!");
			try
			{	String x=tag.getValue("x");
				String y=tag.getValue("y");
				String var=tag.getValue("var");
				String d=tag.getValue("d");
				String min=tag.getValue("min");
				String max=tag.getValue("max");
				FunctionObject f=new FunctionObject(c);
				f.setDefaults();
				setType(tag,f);
				setName(tag,f);
				set(tree,f);
				c.add(f);
				if (tag.hasParam("filled")) f.setFilled(true);
				f.setExpressions(var,x,y);
				f.setRange(min,max,d);
				f.setSpecial(tag.hasTrueParam("special"));
				setConditionals(tree,c,f);
				if (tag.hasParam("center")) f.setCenter(tag.getValue("center"));
				f.updateText();
				return true;
			}
			catch (Exception e)
			{	throw new ConstructionException("Function invalid!");
			}
		}
	}

	static public void setType (XmlTag tag, FunctionObject p)
	{	if (tag.hasParam("shape"))
		{	String s=tag.getValue("shape");
			if (s.equals("square")) p.setType(0);
			if (s.equals("diamond")) p.setType(1);
			if (s.equals("circle")) p.setType(2);
			if (s.equals("dot")) p.setType(3);
			if (s.equals("cross")) p.setType(4);
			if (s.equals("dcross")) p.setType(5);
		}
	}

	public String getTag () { return "Function"; }

	public void construct (Construction c, 
		String name, String params[], int nparams)
		throws ConstructionException
	{	if (nparams!=6)
			throw new ConstructionException(Zirkel.name("exception.nparams"));
		boolean added=false;
		try
		{	FunctionObject F=new FunctionObject(c);
			c.add(F);
			added=true;
			if (!name.equals("")) F.setNameCheck(name);
			F.setRange(params[0],params[1],params[2]);
			F.setExpressions(params[3],params[4],params[5]);
			F.setDefaults();
		}
		catch (ConstructionException e)
		{	if (added) c.back();
			throw e;
		}
		catch (Exception e)
		{	if (added) c.back();
			throw new ConstructionException("Function Invalid!");
		}
	}
}
