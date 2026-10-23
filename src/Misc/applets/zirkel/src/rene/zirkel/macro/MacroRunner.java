package rene.zirkel.macro;

/**
This is an ObjectConstructor, which can run a macro.
*/

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.StringTokenizer;

import rene.gui.ButtonAction;
import rene.gui.CloseDialog;
import rene.gui.MyLabel;
import rene.gui.MyPanel;
import rene.gui.Panel3D;
import rene.gui.TextFieldAction;
import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelCanvas;
import rene.zirkel.construction.Construction;
import rene.zirkel.construction.ConstructionException;
import rene.zirkel.construction.DepList;
import rene.zirkel.construction.Selector;
import rene.zirkel.constructors.*;
import rene.zirkel.objects.*;

class MacroPrompt extends CloseDialog
{	boolean Valid;
	TextField T;
	String S="";

	public MacroPrompt (Frame f, String o)
	{	super(f,Zirkel.name("macro.prompt.title"),true);
		setLayout(new BorderLayout());
		
		Panel p=new MyPanel();
		p.setLayout(new BorderLayout());
		p.add("North",new MyLabel(Zirkel.name("macro.prompt.prompt")));
		Panel h=new MyPanel();
		h.setLayout(new GridLayout(1,0));
		h.add(new MyLabel(o));
		h.add(T=new TextFieldAction(this,"OK","",20));
		p.add("South",h);
		add("Center",new Panel3D(p));
		
		Panel s=new MyPanel();
		s.add(new ButtonAction(this,Zirkel.name("ok"),"OK"));
		s.add(new ButtonAction(this,Zirkel.name("abort"),"Close"));
		add("South",new Panel3D(s));
		
		Valid=false;
		
		pack();
		center(f);
		pack();
	}
	
	public void doAction (String o)
	{	if (o.equals("OK"))
		{	S=T.getText();
			Valid=true;
			doclose();
		}
		else super.doAction(o);
	}
	
	public boolean isValid () { return Valid; }
	public String getValue () { return S; }
}

public class MacroRunner extends ObjectConstructor
	implements Selector
{	String S[];
	int Param;
	Macro M;
	ConstructionObject Params[]; // array of parameters.
	boolean NewPoint[]; // are the parameters new points?
	boolean Fixed[];

	/**
	Must be called, when this constructor is started.
	@param m The macro to be run
	*/

	public void setMacro (Macro m, ZirkelCanvas zc)
	{	S=m.getPrompts(); Param=0; M=m;
		Params=new ConstructionObject[S.length];
		Fixed=new boolean[S.length];
		NewPoint=new boolean[S.length];
		for (int i=0; i<S.length; i++) Fixed[i]=false;
	}

	/**
	At each mouse press, one parameter is chosen. The valid objects
	are determined by the type of the parameter stored in the macro
	and retrieved by getParams(). Once all parameters are entered, the
	marco is run.
	*/
	public void mousePressed (MouseEvent e, ZirkelCanvas zc)
	{	if (!zc.Visual) return;
		ConstructionObject o=null;
		ConstructionObject p[]=M.getParams();
		if (p[Param] instanceof PointObject)
			o=zc.selectCreatePoint(e.getX(),e.getY());
		else
			o=zc.selectWithSelector(e.getX(),e.getY(),this);
		if (o==null) return;
		int ip=Param;
		if (!setNextParameter(o,zc,e.isShiftDown())) return;
		NewPoint[ip]=(o instanceof PointObject && zc.isNewPoint());
		if (Param>=S.length)
		{	doMacro(zc);
			reset(zc);
		}
		else getFixed(zc);
	}
	
	public boolean isAdmissible (ZirkelCanvas zc, ConstructionObject o)
	{	ConstructionObject p[]=M.getParams();
		if (p[Param] instanceof PointObject)
			return (o instanceof PointObject);
		else if (p[Param] instanceof FixedAngleObject)
			return (o instanceof FixedAngleObject);
		else if (p[Param] instanceof SegmentObject)
			return (o instanceof SegmentObject);
		else if (p[Param] instanceof RayObject)
			return (o instanceof RayObject);
		else if (p[Param] instanceof TwoPointLineObject)
			return (o instanceof TwoPointLineObject);
		else if (p[Param] instanceof PrimitiveLineObject)
			return (o instanceof PrimitiveLineObject);
		else if (p[Param] instanceof PrimitiveCircleObject)
			return (o instanceof PrimitiveCircleObject);
		else if (p[Param] instanceof FunctionObject)
			return (o instanceof FunctionObject);
		else if (p[Param] instanceof UserFunctionObject)
			return (o instanceof UserFunctionObject);
		else if (p[Param] instanceof AngleObject)
			return (o instanceof AngleObject);
		else if (p[Param] instanceof ExpressionObject)
			return (o instanceof ExpressionObject || 
					o instanceof AngleObject ||
					o instanceof FixedAngleObject ||
					o instanceof AreaObject);
		else if (p[Param] instanceof AreaObject)
			return (o instanceof AreaObject);		
		else return false;
	}
	
	public void mouseMoved (MouseEvent e, ZirkelCanvas zc, boolean simple)
	{	ConstructionObject p[]=M.getParams();
		if (!(p[Param] instanceof PointObject) && Param==p.length-1)
		{	zc.clearPreview();
			zc.repaint();
			ConstructionObject o=zc.selectWithSelector(e.getX(),e.getY(),this,false);
			if (o!=null)
			{	zc.prepareForPreview(e);
				Params[Param]=o;
				runMacroPreview(zc);
				zc.indicateWithSelector(e.getX(),e.getY(),this);
				return;
			}
		}
		if (!simple && waitForLastPoint())
		{	if (zc.isPreview())
			{	zc.movePreview(e);
			}
			else
			{	zc.prepareForPreview(e);
				finishConstruction(e,zc);
				return;
			}
		}
		if (p[Param] instanceof PointObject)
			zc.indicateCreatePoint(e.getX(),e.getY(),true);
		else
			zc.indicateWithSelector(e.getX(),e.getY(),this);
	}
	
	public boolean waitForLastPoint ()
	{	if (M.countPrompts()>0) return false;
		ConstructionObject p[]=M.getParams();
		return (p[Param] instanceof PointObject) && Param==p.length-1;
	}
	
	public void finishConstruction (MouseEvent e, ZirkelCanvas zc)
	{	ConstructionObject p[]=M.getParams();
		ConstructionObject o;
		if (p[Param] instanceof PointObject)
			o=zc.selectCreatePoint(e.getX(),e.getY());
		else return;
		NewPoint[Param]=true;
		Params[Param]=o;
		runMacroPreview(zc);
	}

	public void reset (ZirkelCanvas zc)
	{	if (zc.Visual)
		{	super.reset(zc);
			Param=0;
			if (M!=null && M.hasFixed()) getFixed(zc);
			showStatus(zc);
		}
		else if (M!=null) // show the input pattern
		{	StringBuffer b=new StringBuffer();
			b.append('=');
			String name=M.getName();
			if (name.indexOf("(")>0) b.append("\""+M.getName()+"\""); 
			else b.append(M.getName());
			b.append('(');
			for (int i=0; i<M.getParams().length-1; i++)
			{	b.append(',');
			}
			b.append(')');
			zc.setPrompt(b.toString());
		}
	}
	
	public void getFixed (ZirkelCanvas zc)
	{	if (M==null || !zc.Visual) return;
		boolean start=(Param==0);
		while ((M.isFixed(Param) || M.getPrompts()[Param].startsWith("="))
			&& Param<(start?S.length-1:S.length))
		{	String name;
			if (M.isFixed(Param)) name=M.getLast(Param);
			else name=M.getPrompts()[Param].substring(1);
			if (name.equals(""))
			{	M.setFixed(Param,false);
				break;
			}
			ConstructionObject o=zc.getConstruction().find(name);
			if (o==null)
			{	M.setFixed(Param,false);
				break;
			}
			if (!setNextParameter(o,zc,false)) return;
			if (Param>=S.length)
			{	doMacro(zc);
				reset(zc);
				break;
			}
		}
		showStatus(zc);
	}

	public void returnPressed (ZirkelCanvas zc)
	{	if (M==null || !zc.Visual) return;
		String name=M.getLast(Param);
		if (name.equals("")) return;
		ConstructionObject o=zc.getConstruction().find(name);
		if (!setNextParameter(o,zc,false)) return;
		if (Param>=S.length)
		{	doMacro(zc);
			reset(zc);
		}
		else getFixed(zc);
	}

	public boolean setNextParameter (ConstructionObject o,
		ZirkelCanvas zc, boolean fix)
	{	if (!isAdmissible(zc,o)) return false;
		Params[Param]=o;
		o.setSelected(true);
		if (fix) Fixed[Param]=true;
		zc.getConstruction().addParameter(o);
		zc.repaint();
		Param++;
		return true;
	}
	
	public void doMacro (ZirkelCanvas zc)
	{	String value[]=new String[0];
		runMacro(zc,value);
	}

	static DepList DL=new DepList();
	
	public void showStatus (ZirkelCanvas zc)
	{	if (M!=null)
		{	ConstructionObject p[]=M.getParams();
			String type="???";
			// Determine the expected type and display in status line
			if (p[Param] instanceof PointObject)
				type=Zirkel.name("name.Point");
			else if (p[Param] instanceof FixedAngleObject)
				type=Zirkel.name("name.FixedAngle");
			else if (p[Param] instanceof SegmentObject)
				type=Zirkel.name("name.Segment");
			else if (p[Param] instanceof LineObject)
				type=Zirkel.name("name.TwoPointLine");
			else if (p[Param] instanceof RayObject)
				type=Zirkel.name("name.Ray");
			else if (p[Param] instanceof PrimitiveLineObject)
				type=Zirkel.name("name.Line");
			else if (p[Param] instanceof PrimitiveCircleObject)
				type=Zirkel.name("name.Circle");
			else if (p[Param] instanceof ExpressionObject)
				type=Zirkel.name("name.Expression");
			else if (p[Param] instanceof AreaObject)
				type=Zirkel.name("name.Polygon");
			else if (p[Param] instanceof AngleObject)
				type=Zirkel.name("name.Angle");
			String s=M.getLast(Param);
			String prompt;
			if (s.equals(""))
				prompt=ConstructionObject.text4(
					Zirkel.name("message.runmacro"),
					M.getName(),""+(Param+1),type,S[Param]);
			else
				prompt=ConstructionObject.text4(
					Zirkel.name("message.runmacro"),
					M.getName(),""+(Param+1),type,S[Param])+" "+
					ConstructionObject.text1(
					Zirkel.name("message.runmacro.return"),M.getLast(Param));
			zc.showStatus(prompt);
		}
	}

	/**
	* Run a macro. The macro parameters have already been determined.
	* This is a rather complicated function.
	*/
	public void runMacro (ZirkelCanvas zc, Construction c, String value[])
	{	M.setTranslation(c);
		ConstructionObject LastBefore=c.last();
		int N=Params.length;
		// First clear all parameter flags. This makes it possible to
		// check for proper translation of secondary parameters later.
		// Secondary parameters without a translation will be
		// constructed.
		Enumeration e=M.elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			o.clearParameter(); o.setTranslation(null);
		}
		M.clearTranslations();
		c.clearTranslators();
		ConstructionObject p[]=M.getParams();
		// For all macro paramters, determine the translation to the
		// real construction, and do the same for the secondary
		// parameters, which belong to the parameter. The secondary
		// parameters are stored in the macro at its definition, as
		// the primary ones. Also the parameters in the macros are marked
		// as such to make sure and prevent construction.
		M.initLast(); // Macros remember the parameters for next call
		for (int i=0; i<N; i++)
		{	M.setLast(Params[i].getName(),i);
			p[i].setTranslation(Params[i]);
			p[i].setMainParameter();
			if (NewPoint[i] && p[i].isHidden())
				Params[i].setHidden(true);
			if (Params[i] instanceof PointObject 
					&& p[i] instanceof PointObject
					&& NewPoint[i])
			{	PointObject pa=(PointObject)Params[i],pp=(PointObject)p[i];
				pa.setIncrement(pp.getIncrement());
				if (pp.getBound()!=null)
				{	pa.setBound(pp.getBound());
					pa.setInside(pp.isInside());
					pa.translate();
				}
			}
			// translate parameters that depend on themself only
			if (Params[i] instanceof PointObject 
					&& p[i] instanceof PointObject
					&& ((PointObject)p[i]).dependsOnItselfOnly())
			{	PointObject P=(PointObject)Params[i];
				// Do not transfer self reference to objects that depend on something!
				// This might crash the construction.
				if (!P.depending().hasMoreElements())
				{	P.setConstruction(M);
					P.setFixed(((PointObject)p[i]).getEX(),((PointObject)p[i]).getEY());
					P.translateConditionals();
					P.translate();
					P.setConstruction(c);
				}
			}
			if (p[i].isMainParameter())
			{	// System.out.println("Main Parameter "+p[i].getName());
				e=p[i].secondaryParams();
				// Copy the list of secondary parameters in the macro,
				// which depend from p[i] to DL.
				DL.reset();
				while (e.hasMoreElements())
				{	ConstructionObject o=(ConstructionObject)e.nextElement();
					// System.out.println("Secondary Parameter "+o.getName()+" of "+p[i].getName());
					DL.add(o);
					o.setParameter();
				}
				e=DL.elements();
				// Get a list of actual secondary params in the
				// construction. Then translate the scecondary params
				// in the macro definition to the true construction objects.
				Enumeration ep=Params[i].secondaryParams();
				while (ep.hasMoreElements() && e.hasMoreElements())
				{	ConstructionObject o=
						(ConstructionObject)e.nextElement();
					ConstructionObject op=
						(ConstructionObject)ep.nextElement();
					if (o.getTranslation()!=op && o.getTranslation()!=null)
					{	zc.warning(Zirkel.name("macro.usage"));
						return;
					}
					o.setTranslation(op);
				}
			}
		}
		// Now we generate the objects.
		e=M.elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			// System.out.println(o.getName()+" "+o.isParameter());
			if (!o.isParameter())
				// else do not construct!
			{	// Copy the object and add to construction. Then
				// translate the dependencies properly
				ConstructionObject oc=(ConstructionObject)o.copy();
				oc.setKeep(false); // necessary for jobs
				oc.setTarget(false); // necessary for descriptive constructions
				c.addNoCheck(oc);
				o.setTranslation(oc);
				oc.validate();
				c.added(oc);
				// For the target objects, use default values instead
				// of values stored in the macro (point style etc.)
				if (o.isTarget()) oc.setTargetDefaults();
				//else if (o.isHidden()) oc.setHidden(true);
				// For black objects, use the default color.
				if (oc.getColorIndex()==0)
					oc.setColor(c.DefaultColor);
				// Handle objects to prompt for:
				if ((oc instanceof FixedCircleObject ||
					oc instanceof FixedAngleObject ||
					oc instanceof ExpressionObject) &&
					M.promptFor(o.getName()))
				{	c.updateCircleDep();
					c.dovalidate();
					zc.repaint();
					int index=M.getPromptFor(o.getName());
					String v="";
					if (index>=value.length || value[index].equals(""))
					{	MacroPrompt pr=new MacroPrompt(zc.getFrame(),
							M.getPromptName(o.getName()));
						pr.setVisible(true);
						if (pr.isValid()) v=pr.getValue();
					}
					else v=value[index];
					if (!v.equals(""))
					{	oc.setFixed(v);
						zc.check();
					}
					else
					{	zc.warning(Zirkel.name("macro.prompt.illegal"));
					}
				}
			}
		}
		// Now fix the objects, which depend on later objects
		e=M.elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (!o.isParameter()) o.laterTranslate(M);
		}
		c.updateCircleDep();
		c.runTranslators(M);
		c.dovalidate();
		zc.repaint();
		int fixed=0;
		for (int i=0; i<Fixed.length; i++)
			if (Fixed[i]) fixed++;
		if (fixed>0 && fixed<Fixed.length && !M.hasFixed())
		{	String name=M.getName()+" -";
			for (int i=0; i<Fixed.length; i++)
			{	if (Fixed[i])
					name=name+" "+M.LastParams[i];
			}
			M=zc.copyMacro(M,name,Fixed);
			for (int i=0; i<Fixed.length; i++) Fixed[i]=false;
			reset(zc);
		}
		if (LastBefore!=null && M.hideDuplicates())
			zc.hideDuplicates(LastBefore);
	}
	
	public void runMacro (ZirkelCanvas zc, String value[])
	{	runMacro(zc,zc.getConstruction(),value);
	}

	public void runMacroPreview (ZirkelCanvas zc)
	{	Construction c=zc.getConstruction();
		M.setTranslation(c);
		int N=Params.length;
		// First clear all parameter flags. This makes it possible to
		// check for proper translation of secondary parameters later.
		// Secondary parameters without a translation will be
		// constructed.
		Enumeration e=M.elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			o.clearParameter(); o.setTranslation(null);
		}
		M.clearTranslations();
		c.clearTranslators();
		ConstructionObject p[]=M.getParams();
		// For all macro paramters, determine the translation to the
		// real construction, and do the same for the secondary
		// parameters, which belong to the parameter. The secondary
		// parameters are stored in the macro at its definition, as
		// the primary ones. Also the parameters in the macros are marked
		// as such to make sure and prevent construction.
		for (int i=0; i<N; i++)
		{	M.setLast(Params[i].getName(),i);
			p[i].setTranslation(Params[i]);
			p[i].setMainParameter();
			if (NewPoint[i] && p[i].isHidden())
				Params[i].setHidden(true);
			if (p[i].isMainParameter())
			{	e=p[i].secondaryParams();
				// Copy the list of secondary parameters in the macro,
				// which depend from p[i] to DL.
				DL.reset();
				while (e.hasMoreElements())
				{	ConstructionObject o=(ConstructionObject)e.nextElement();
					DL.add(o);
					o.setParameter();
				}
				e=DL.elements();
				// Get a list of actual secondary params in the
				// construction. Then translate the scecondary params
				// in the macro definition to the true construction objects.
				Enumeration ep=Params[i].secondaryParams();
				while (ep.hasMoreElements() && e.hasMoreElements())
				{	ConstructionObject o=
						(ConstructionObject)e.nextElement();
					ConstructionObject op=
						(ConstructionObject)ep.nextElement();
					if (o.getTranslation()!=op && o.getTranslation()!=null)
					{	//zc.warning(Zirkel.name("macro.usage"));
						return;
					}
					if (o!=null) o.setTranslation(op);
				}
			}
		}
		// Now we generate the objects.
		e=M.elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (!o.isParameter())
				// else do not construct!
			{	// Copy the object and add to construction. Then
				// translate the dependencies properly
				ConstructionObject oc=(ConstructionObject)o.copy();
				oc.setKeep(false); // necessary for jobs
				oc.setTarget(false); // necessary for descriptive constructions
				oc.setSelectable(false);
				oc.setIndicated(true);
				c.addNoCheck(oc);
				o.setTranslation(oc);
				oc.validate();
				c.added(oc);
				// For the target objects, use default values instead
				// of values stored in the macro (point style etc.)
				if (o.isTarget()) oc.setTargetDefaults();
				if (o.isHidden()) oc.setHidden(true);
				// For black objects, use the default color.
				if (oc.getColorIndex()==0)
					oc.setColor(c.DefaultColor);
			}
		}
		// All objects have the chance to translate anything
		// (used by start and end points of arcs)
		e=M.elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (!o.isParameter()) o.laterTranslate(M);
		}
		c.updateCircleDep();
		// Run the translations of forward references of type @...
		c.runTranslators(M);
		c.dovalidate();
		zc.repaint();
	}
	
	/**
	Run a macro in nonvisual mode. This needs a previous setMacro()
	call!
	*/

	public void run (ZirkelCanvas zc, Construction c,
		String name, String params[], int nparams)
		throws ConstructionException
	{	ConstructionObject p[]=M.getParams();
		if (nparams!=p.length+M.countPrompts())
			throw new ConstructionException(Zirkel.name("exception.nparams"));
		String value[]=new String[M.countPrompts()];
		for (int i=0; i<M.countPrompts(); i++)
			value[i]=params[p.length+i];
		for (int i=0; i<p.length; i++)
		{	ConstructionObject o=c.find(params[i]);
			if (o==null)
				throw new ConstructionException(
					Zirkel.name("exception.notfound"));
			if (p[Param] instanceof PointObject)
				if (!(o instanceof PointObject))
					throw new ConstructionException(
						Zirkel.name("exception.type"));
			else if (p[Param] instanceof SegmentObject)
				if (!(o instanceof SegmentObject))
					throw new ConstructionException(
						Zirkel.name("exception.type"));
			else if (p[Param] instanceof LineObject)
				if (!(o instanceof LineObject))
					throw new ConstructionException(
						Zirkel.name("exception.type"));
			else if (p[Param] instanceof RayObject)
				if (!(o instanceof RayObject))
					throw new ConstructionException(
						Zirkel.name("exception.type"));
			else if (p[Param] instanceof PrimitiveLineObject)
				if (!(o instanceof PrimitiveLineObject))
					throw new ConstructionException(
						Zirkel.name("exception.type"));
			else if (p[Param] instanceof PrimitiveCircleObject)
				if (!(o instanceof PrimitiveCircleObject))
					throw new ConstructionException(
						Zirkel.name("exception.type"));
			else
				throw new ConstructionException(
					Zirkel.name("exception.type"));
			Params[i]=o;
		}
		runMacro(zc,c,value);
		StringTokenizer t=new StringTokenizer(name,",");
		Enumeration e=M.getTargets().elements();
		while (e.hasMoreElements() && t.hasMoreTokens())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			o.getTranslation().setName(t.nextToken().trim());
		}
		zc.repaint();
	}

}
