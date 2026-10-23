package rene.zirkel.macro;

import java.util.*;

import rene.util.xml.*;
import rene.zirkel.ZirkelCanvas;
import rene.zirkel.construction.Construction;
import rene.zirkel.construction.ConstructionException;
import rene.zirkel.objects.*;

/**
Macros are stored as constructions. Some of the construction objects
are parameters, which divide into two groups: primary parapemers and
secondary parameters (e.g. endpoints of a segment). Moreover, there is
a separate list of the primary parameters and the prompts to display
to the user at each primary parameter. Some objects are marked as
targets.
*/


public class Macro extends Construction
	implements Cloneable
{	public String Name;
	public String Prompts[];
	public ConstructionObject Params[];
	public String PromptFor[]=new String[0];
	public String PromptName[]=new String[0];
	public String LastParams[];
	public boolean Fixed[];
	boolean Protected=false;
	boolean HideDuplicates=true;

	/**
	@param zc The ZirkelCanvas.
	@param name The name of this macro
	@param comment The macro comment.
	@param e The parameter prompt strings.
	*/
	public Macro (ZirkelCanvas zc, String name, String comment, String s[])
	{	Name=name; Comment=comment;
		Prompts=s;
	}
	
	public String getName () { return Name; }
	public void setName (String name)
	{	Name=name;
	}
	public String getComment () { return Comment; }
	public String[] getPrompts () { return Prompts; }

	/**
	Set the list of parameters. The setup of the macro is done in a
	function in ZirkelCanvas.defineMacro().
	*/	
	public void setParams (ConstructionObject p[]) { Params=p; }
	public ConstructionObject[] getParams () { return Params; }

	/**
	Denote and recall previously set parameters.
	*/
	public void initLast ()
	{	LastParams=new String[Params.length];
	}
	public void setLast (String name, int i)
	{	try
		{	LastParams[i]=name;
		}
		catch (Exception e) {}
	}
	public String getLast (int i)
	{	if (LastParams!=null && LastParams[i]!=null) return LastParams[i];
		else return "";
	}

	public void setPromptFor (String s[])
	{	PromptFor=s;
		PromptName=new String[PromptFor.length];
		for (int i=0; i<PromptFor.length; i++) PromptName[i]=PromptFor[i];
	}
	public void setPromptName (int i, String s)
	{	PromptName[i]=s;
	}
	public boolean promptFor (String s)
	{	return getPromptFor(s)>=0;	
	}
	public int getPromptFor (String s)
	{	for (int i=0; i<PromptFor.length; i++)
			if (PromptFor[i].equals(s)) return i;
		return -1;
	}
	public String getPromptName (String s)
	{	for (int i=0; i<PromptFor.length; i++)
			if (PromptFor[i].equals(s)) return PromptName[i];
		return "";
	}
	public int countPrompts ()
	{	return PromptFor.length;	
	}

	/**
	Save a macro.
	*/
	public void saveMacro (XmlWriter xml)
	{	// Start the macro with its name as parameter
		xml.startTagStart("Macro");
		xml.printArg("Name",Name);
		if (!HideDuplicates) xml.printArg("showduplicates","true");
		xml.startTagEndNewLine();
		// Write the parameters and their prompts
		for (int i=0; i<Params.length; i++)
		{	xml.startTagStart("Parameter");
			xml.printArg("name",Params[i].getName());
			if (Fixed!=null && Fixed[i] && 
					LastParams!=null && LastParams[i]!=null)
				xml.printArg("fixed",LastParams[i]);
			xml.startTagEnd();
			xml.print(Prompts[i]);
			xml.endTagNewLine("Parameter");
		}
		// Write a comment
		if (!getComment().equals(""))
		{	xml.startTagNewLine("Comment");
			xml.printParagraphs(getComment(),60);
			xml.endTagNewLine("Comment");
		}
		// Write the objects. I.e., secondary parameters, primary
		// parameters and constructed things, including targets as in
		// any other construction.
		xml.startTagNewLine("Objects");
		save(xml);
		xml.endTagNewLine("Objects");
		// Save the objects prompted for
		if (PromptFor.length>0)
		{	xml.startTagStart("PromptFor");
			for (int i=0; i<PromptFor.length; i++)
			{	xml.printArg("object"+i,PromptFor[i]);
				xml.printArg("prompt"+i,PromptName[i]);
			}
			xml.finishTagNewLine();
		}
		// End the macro
		xml.endTagNewLine("Macro");
	}

	/**
	Read a macro (implemented as a constructor).
	*/
	public Macro (ZirkelCanvas zc, XmlTree tree)
		throws ConstructionException
	{	// See, if this is a macro tree, and has a name.
		XmlTag tag=tree.getTag();
		if (!tag.name().equals("Macro"))
			throw new ConstructionException("No macro!");
		if (!tag.hasParam("Name"))
			throw new ConstructionException("Name missing!");
		Name=tag.getValue("Name");
		if (tag.hasParam("showduplicates"))
			HideDuplicates=false;
		// Walk through content
		Enumeration e=tree.getContent();
		while (e.hasMoreElements())
		{	XmlTree t=(XmlTree)e.nextElement();
			tag=t.getTag();
			// Read the objects, the comment
			if (tag.name().equals("Objects")) // Objects
			{	readConstruction(t);
				break;
			}
			else if (tag.name().equals("Comment")) // Comment
			{	try
				{	setComment(t.parseComment());
				}
				catch (Exception ex)
				{	throw new ConstructionException("Illegal Comment");
				}
			}
		}
		// Read the parameters.
		int ParamCount=0;
		// First count the paramters.
		e=tree.getContent();
		while (e.hasMoreElements())
		{	XmlTree t=(XmlTree)e.nextElement();
			tag=t.getTag();
			if (tag.name().equals("Parameter"))
			{	if (!tag.hasParam("name"))
					throw new ConstructionException(
						"Parameter name missing!");
				ParamCount++;
			}
			else if (tag.name().equals("PromptFor"))
			{	if (tag.hasParam("object"))
				{	String s[]=new String[1];
					s[0]=tag.getValue("object");
					setPromptFor(s);
					if (tag.hasParam("prompt"))
					setPromptName(0,tag.getValue("prompt"));
				}
				else
				{	int n=0;
					while (tag.hasParam("object"+n)) n++;
					String s[]=new String[n];
					for (int i=0; i<n; i++)
					{	s[i]=tag.getValue("object"+i);
					}
					setPromptFor(s);
					for (int i=0; i<n; i++)
					{	if (tag.hasParam("prompt"+i))
							setPromptName(i,tag.getValue("prompt"+i));
					}					
				}
			}
		}
		// Then read the parameters.
		Params=new ConstructionObject[ParamCount];
		initLast();
		Prompts=new String[ParamCount];
		for (int pr=0; pr<ParamCount; pr++) Prompts[pr]="";
		int i=0;
		e=tree.getContent();
		while (e.hasMoreElements())
		{	XmlTree t=(XmlTree)e.nextElement();
			tag=t.getTag();
			if (tag.name().equals("Parameter"))
			{	// Search a parameter by this name
				Params[i]=find(tag.getValue("name"));
				if (Params[i]==null)
					throw new ConstructionException(
						"Illegal parameter "+tag.getValue("name")+"!");
				if (tag.hasParam("fixed"))
				{	if (Fixed==null)
					{	Fixed=new boolean[ParamCount];
						for (int j=0; j<ParamCount; j++) Fixed[j]=false;
					}
					Fixed[i]=true;
					LastParams[i]=tag.getValue("fixed");
				}
				Enumeration en=t.getContent();
				while (en.hasMoreElements())
				{	tree=(XmlTree)en.nextElement();
					if (tree.getTag() instanceof XmlTagText)
					{	Prompts[i]=((XmlTagText)tree.getTag()).getContent();
					}
				}
				i++;
			}
		}
	}
	
	/**
	@return A list of targets.
	*/
	public Vector getTargets ()
	{	Vector v=new Vector();
		Enumeration e=V.elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o.isTarget())
				v.addElement(o);
		}
		return v;
	}

	public boolean hasFixed ()
	{	for (int i=0; i<Prompts.length; i++)
			if (Prompts[i].startsWith("=")) return true;
		if (Fixed==null) return false;
		for (int i=0; i<Fixed.length; i++)
			if (Fixed[i]) return true;
		return false;
	}
	public boolean isFixed (int i)
	{	if (Fixed==null) return false;
		return Fixed[i];
	}
	public void setFixed (int i, boolean f)
	{	if (Fixed==null) return;
		Fixed[i]=f;
	}
	
	public Object clone ()
	{	try
		{	return super.clone();
		}
		catch (Exception e) {}
		return null;
	}
	
	public boolean isProtected ()
	{	return Protected;
	}
	public void setProtected (boolean flag)
	{	Protected=flag;
	}
	
	public boolean hideDuplicates ()
	{	return HideDuplicates;
	}
	public void hideDuplicates (boolean flag)
	{	HideDuplicates=flag;
	}
}
