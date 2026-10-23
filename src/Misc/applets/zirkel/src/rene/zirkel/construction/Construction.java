package rene.zirkel.construction;

// file: ZirkelCanvas.java

import java.util.*;
import rene.util.*;
import rene.util.sort.Sorter;
import rene.util.xml.*;
import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelCanvas;
import rene.zirkel.ZirkelFrame;
import rene.zirkel.constructors.*;
import rene.zirkel.listener.*;
import rene.zirkel.objects.*;
import rene.zirkel.tools.*;
import rene.zirkel.expression.*;
import rene.gui.Global;

/**
 * Construction holds all construction details, like objects, default values,
 * viewing window etc., but not macros, which are constructions themselves,
 * and stored in ZirkelCanvas. The class has methods to read and write constructions,
 * and to draw constructions to MyGraphics. 
 * @author Rene Grothmann
 */

public class Construction
{	static final long serialVersionUID=Zirkel.Version;
	public Vector V; // The vector of objects (ConstructionObject's)
	public Vector Parameters; // The vector of parameter object names (String's)
	public Vector Targets; // The vector of target object names (String's)
	public String Comment="",JobComment="";
	public Vector PromptFor=new Vector();
	public Vector Prompts;
	private double X=0,Y=0,W=8,H=8; // H is set by ZirkelCanvas
	public boolean 
		Partial=Global.getParameter("options.partial",false),
		Restricted=Global.getParameter("options.restricted",true),
		PartialLines=Global.getParameter("options.plines",false),
		Vectors=Global.getParameter("options.arrow",false),
		ShowNames=Global.getParameter("options.shownames",false),
		ShowValues=Global.getParameter("options.showvalues",false),
		LongNames=Global.getParameter("options.longnames",false),
		LargeFont=Global.getParameter("options.largefont",false),
		BoldFont=Global.getParameter("options.boldfont",false),
		Hidden=false,
		Obtuse=Global.getParameter("options.obtuse",false),
		Solid=Global.getParameter("options.solid",false);
	public boolean Animate=false,Paint=false; // for loading tracks
	public boolean ShowAll=false; // for macros
	public boolean SuperHide=false; // for macros
	public int
		DefaultColor=Global.getParameter("options.color",0),
		DefaultType=Global.getParameter("options.type",0),
		DefaultColorType=Global.getParameter("options.colortype",0);
	public boolean Changed=false;
	int Count=0; // Unique number for each object
	public Construction TranslateInto;
	public boolean BlockSimulation=false; // To block two simulations at once! 
	public boolean DontAlternateIntersections=false; // To block alternating invalid intersections
	
	ObjectConstructor ObjectConstructors[]=
	// Constructors for reading a construction from file	
		{	new PointConstructor(),
			new LineConstructor(),
			new SegmentConstructor(),
			new RayConstructor(),
			new CircleConstructor(),
			new IntersectionConstructor(),
			new ParallelConstructor(),
			new PlumbConstructor(),
			new Circle3Constructor(),
			new MidpointConstructor(),
			new AngleConstructor(),
			new BoundedPointConstructor(),
			new ExpressionConstructor(),
			new AreaConstructor(),
			new TextConstructor(),
			new QuadricConstructor(),
			new ImageConstructor(),
			new ObjectTracker(),
			new FunctionConstructor()
		};
	
	public Construction ()
	{	clear(); Changed=false;
	}
	
	private AddEventListener AEL=null;

	public void addAddEventListener (AddEventListener ael)
	{	AEL=ael;
	}

	public void removeAddEventListener (AddEventListener ael)
	{	AEL=null;
	}
	
	/**
	 * Add an object o to the construction.
	 * @param o
	 */
	public void add (ConstructionObject o)
	{	if (!o.isGotNCount()) o.setNCount(Count++); // give count
		else o.setGotNCount(false); 
		V.addElement(o);
		if (!Undo.isEmpty()) Undo.removeAllElements(); // clear undo
		o.setConstruction(this);
		if (AEL!=null) AEL.added(this,o); // note add listener
		haveChanged(); // set changed flag
		// System.out.println(o.getName()+" "+o.getNCount());
	}

	/**
	 * Add an object o, but do not note add listener.
	 * Add listener is used to check, if an assignment is finished.
	 * @param o
	 */
	public void addNoCheck (ConstructionObject o)
	// add a new object
	{	if (!o.isGotNCount()) o.setNCount(Count++); // give count
		else o.setGotNCount(false); 
		V.addElement(o);
		if (!Undo.isEmpty()) Undo.removeAllElements();
		o.setConstruction(this);
		haveChanged();
	}

	public void added (ConstructionObject o)
	{	if (AEL!=null) AEL.added(this,o);
	}

	/**
	 * Delete all objects.
	 */
	public synchronized void clear ()
	{	V=new Vector();
		if (!Undo.isEmpty()) Undo.removeAllElements();
		Comment="";
		JobComment="";
		clearParameters(); clearTargets();
		Prompts=new Vector();
		X=0; Y=0; W=8;
		Changed=false;
		Count=0;
	}
	
	Vector Undo=new Vector();
	
	/**
	 * Clear last element.
	 */
	public void back ()
	{	ConstructionObject o=last();
		if (o==null) return;
		o.setInConstruction(false);
		Undo.addElement(o);
		if (V.size()>0) V.removeElementAt(V.size()-1);
		updateCircleDep();
		clearParameters(); clearTargets();
		haveChanged();
	}
	
	/**
	* Delete the object and all children.
	* The constructable elements must have been marked before.
	*/
	public void delete (boolean clearUndo)
	{	if (clearUndo && !Undo.isEmpty()) Undo.removeAllElements();
		for (int i=V.size()-1; i>=0; i--)
		{	ConstructionObject o=(ConstructionObject)V.elementAt(i);
			if (o.isFlag() && !o.isJobTarget())
			{	o.setInConstruction(false);
				Undo.addElement(o);
				V.removeElementAt(i);
			}
		}
		updateCircleDep();
		clearParameters(); 
		clearTargets();
		haveChanged();
	}
	public void delete ()
	{	delete(true);
	}
	
	/**
	* Restore all elements from Undo
	*/
	public void undo ()
	{	if (Undo.isEmpty()) return;
		ConstructionObject o[]=new ConstructionObject[Undo.size()];
		Undo.copyInto(o);
		for (int i=o.length-1; i>=0; i--)
		{	V.addElement(o[i]);
			o[i].setConstruction(this);
		}
		Undo.removeAllElements();
		haveChanged();
	}
	
	public Enumeration elements ()
	// enumerate all objects
	{	return V.elements();
	}
	
	public Enumeration getSortedElements ()
	{	ConstructionObject o[]=new ConstructionObject[V.size()];
		V.copyInto(o);
		for (int i=0; i<o.length; i++) o[i].Value=(o[i].getNCount());
		Sorter.sort(o);
		Vector v=new Vector();
		for (int i=0; i<o.length; i++) v.addElement(o[i]);
		return v.elements();
	}

	public ConstructionObject last ()
	{	if (V.size()>0) return (ConstructionObject)V.elementAt(V.size()-1);
		else return null;
	}
	
	public ConstructionObject lastByNumber ()
	{	ConstructionObject o=null;
		int maxCount=-1;
		Enumeration e=elements();
		while (e.hasMoreElements())
		{	ConstructionObject co=(ConstructionObject)e.nextElement();
			if (co.getNCount()>maxCount)
			{	maxCount=co.getNCount();
				o=co;
			}
		}
		return o;
	}

	public ConstructionObject lastButOne ()
	{	if (V.size()>1) return (ConstructionObject)V.elementAt(V.size()-2);
		else return null;
	}
	
	public void clearAfter (ConstructionObject after)
	{	while (true)
		{	ConstructionObject o=last();
			if (o==null || o==after) break;
			o.setInConstruction(false);
			V.removeElementAt(V.size()-1);
			haveChanged();
		}
		updateCircleDep();
		clearParameters(); clearTargets();	
	}

	public String getComment ()
	{	return Comment;
	}
	
	public void setComment (String s)
	{	if (s.length()<=2) s="";
		Comment=s;
	}
	
	public double getX () { return X; }
	public double getY () { return Y; }
	public double getW () { return W; }
	public double getH () { return H; }
	public void setH (double h) { H=h; }
	public void setXYW (double x, double y, double w)
	{	X=x; Y=y; W=w;
	}
	
	public void save (XmlWriter xml)
	{	Enumeration e=elements();
		while (e.hasMoreElements())
		{	((ConstructionObject)e.nextElement()).save(xml);
		}
		Changed=false;
	}

	public String TrackP=null,TrackPM=null,TrackO=null,AnimateP=null;
	public Vector TrackPO;
	public Vector AnimateV=null;
	public int Omit=0;
	public boolean AnimateNegative=false;
	public boolean AnimateOriginal=false;
	public String AnimateDelay=null;
	public String Icons="";
	public boolean AnimateBreakpoints=false;
	public long AnimateTime=1000;
	public boolean AnimateLoop=false;
	public boolean ResizeBackground=false;
	public String BackgroundFile=null;
	
	public synchronized void load (XmlTree tree, ZirkelCanvas zc)
		throws ConstructionException
	{	Enumeration root=tree.getContent();
		TrackP=null;
		TrackPO=new Vector();
		AnimateP=null;
		AnimateNegative=false;
		AnimateOriginal=false;
		AnimateBreakpoints=false;
		AnimateLoop=false;
		AnimateTime=1000;
		Icons="";
		BackgroundFile=null;
		ResizeBackground=false;
		zc.clearDrawings();
		while (root.hasMoreElements())
		{	tree=(XmlTree)root.nextElement();	
			XmlTag tag=tree.getTag();
			if (tag.name().equals("Comment"))
			{	try
				{	setComment(tree.parseComment());
				}
				catch (Exception e)
				{	throw new ConstructionException("Illegal Comment");
				}
			}
			else if (tag.name().equals("Assignment"))
			{	try
				{	setJobComment(tree.parseComment());
				}
				catch (Exception e)
				{	throw new ConstructionException("Illegal Assignment");
				}
			}
			else if (tag.name().equals("Track"))
			{	if (!tag.hasParam("track"))
					throw new ConstructionException(
						Zirkel.name("exception.track"));
				TrackP=tag.getValue("track");
				TrackPO=new Vector();
				int i=0;
				while (tag.hasParam("track"+i))
				{	TrackPO.addElement(tag.getValue("track"+i));
					i++;
				}
				if (tag.hasParam("move")) TrackPM=tag.getValue("move");
				else TrackPM=null;
				if (tag.hasParam("on")) TrackO=tag.getValue("on");
				else TrackO=null;
				Animate=false; Paint=true;
				if (tag.hasParam("animate"))
				{	if (tag.getValue("animate").equals("nopaint")) Paint=false;
					Animate=true;
				}
				Omit=0;
				if (tag.hasParam("omit"))
				{	try
					{	Omit=Integer.parseInt(tag.getValue("omit"));
					}
					catch (Exception e) {}
				}
			}
			else if (tag.name().equals("Animate"))
			{	if (!tag.hasParam("animate") || !tag.hasParam("via0"))
					throw new ConstructionException(
						Zirkel.name("exception.animate"));
				AnimateP=tag.getValue("animate");
				int k=0;
				AnimateV=new Vector();
				while (tag.hasParam("via"+k))
				{	AnimateV.addElement(tag.getValue("via"+k));
					k++;
				}
				AnimateNegative=false;
				if (tag.hasParam("negative") && 
						tag.getValue("negative").equals("true"))
					AnimateNegative=true;
				if (tag.hasParam("original") && 
						tag.getValue("original").equals("true"))
					AnimateOriginal=true;
				AnimateDelay=null;
				if (tag.hasParam("delay"))
				{	AnimateDelay=tag.getValue("delay");
				}
			}
			else if (tag.name().equals("AnimateBreakpoints"))
			{	AnimateBreakpoints=true;
				try
				{	if (tag.hasParam("time"))
					{	AnimateTime=new Long(tag.getValue("time")).longValue();
					}
					if (tag.hasParam("loop"))
					{	AnimateLoop=true;
					}
				}
				catch (Exception e)
				{	throw new ConstructionException("exception.animate");
				}
			}
			else if (tag.name().equals("Window"))
			{	try
				{	if (tag.hasParam("x"))
						X=new Double(tag.getValue("x")).doubleValue();
					if (tag.hasParam("y"))
						Y=new Double(tag.getValue("y")).doubleValue();
					if (tag.hasParam("w"))
						W=new Double(tag.getValue("w")).doubleValue();
					zc.ShowGrid=tag.hasTrueParam("showgrid");
				}
				catch (Exception e)
				{	throw new ConstructionException("Illegal Window Parameters");
				}				
			}
			else if (tag.name().equals("Grid"))
			{	try
				{	if (tag.hasParam("color"))
					{	int n=new Integer(tag.getValue("color")).intValue();
						if (n>=0 && n<ZirkelFrame.Colors.length)
							zc.GridColor=n;
					}
					if (tag.hasParam("thickness"))
					{	int n=new Integer(tag.getValue("thickness")).intValue();
						if (n>=0 && n<4)
							zc.GridThickness=n;
					}
					zc.GridLabels=!(tag.hasTrueParam("nolables"));
					zc.GridLarge=tag.hasTrueParam("large");
					zc.GridBold=tag.hasTrueParam("bold");
					zc.AxesOnly=tag.hasTrueParam("axesonly");
				}
				catch (Exception e)
				{	throw new ConstructionException("Illegal Grid Parameters");
				}
			}
			else if (tag.name().equals("Background"))
			{	try
				{	if (tag.hasTrueParam("resize"))
						ResizeBackground=true;
					BackgroundFile=tag.getValue("file");
					if (BackgroundFile==null) 
						throw new ConstructionException("Illegal Background Parameters");
				}
				catch (Exception e)
				{	throw new ConstructionException("Illegal Background Parameters");
				}				
			}
			else if (tag.name().equals("Draw"))
			{	zc.loadDrawings(tree);
			}
			else if (tag.name().equals("Objects"))
			{	// System.out.println("reading objects");
				readConstruction(tree);
				// System.out.println("finished reading objects");
				updateCount();
				computeNeedsOrdering();
				//System.out.println("needs ordering: "+NeedsOrdering);
				doOrder();
				//System.out.println("finished reading objects");
				break;
			}
			else if (tag.name().equals("Restrict"))
			{	if (!tag.hasParam("icons"))
					throw new ConstructionException("Illegal Window Parameters");
				Icons=tag.getValue("icons");
			}
		}
	}
	
	public void translateOffsets (ZirkelCanvas zc)
	{	Enumeration e=V.elements();
		while (e.hasMoreElements())
		{	((ConstructionObject)e.nextElement()).translateOffset(zc);
		}
	}
	
	public synchronized void readConstruction (XmlTree tree)
		throws ConstructionException
	{	Enumeration e=tree.getContent();
		//System.out.println("start reading tree");
		while (e.hasMoreElements())
		{	tree=(XmlTree)e.nextElement();
			int i,n=ObjectConstructors.length;
			for (i=0; i<n; i++)
			{	try
				{	if (ObjectConstructors[i].construct(tree,this)) break;
				}
				catch (ConstructionException ex)
				{	if (tree.getTag().hasParam("name"))
					{	String name=tree.getTag().getValue("name");
						throw new ConstructionException(
							ex.getDescription()+" (in "+name+")");
					}
					else throw ex;
				}
			}
			if (i>=n) throw 
				new ConstructionException(tree.getTag().name()+" unknown!");
		}
		//System.out.println("end reading tree");
		e=V.elements();
		while (e.hasMoreElements())
		{	((ConstructionObject)e.nextElement()).laterBind(this);
		}
		//System.out.println("finished later bind");
		dovalidate();
		updateCircleDep();
		Changed=false;
		//System.out.println("finished circle dep");
	}

	/**
	 * 
	 * @param name
	 * @return Object with that name or null.
	 */
	public ConstructionObject find (String name)
	{	Enumeration e=V.elements();
		while (e.hasMoreElements())
		{	ConstructionObject c=(ConstructionObject)e.nextElement();
			if (c.getName().equals(name)) return c;
		}
		return null;
	}
	
	/**
	 * Find an object with a name, defined before object until.
	 * @param name
	 * @param until
	 * @return Object with that name or null
	 */
	public ConstructionObject find (String name, ConstructionObject until)
	{	Enumeration e=V.elements();
		while (e.hasMoreElements())
		{	ConstructionObject c=(ConstructionObject)e.nextElement();
			if (c==until) break;
			if (c.getName().equals(name)) return c;
		}
		return null;
	}
	
	/**
	 * Find an object with that name before and inclusive object until.
	 * @param name
	 * @param until
	 * @return Object with that name or null
	 */
	public ConstructionObject findInclusive (String name, ConstructionObject until)
	{	Enumeration e=V.elements();
		while (e.hasMoreElements())
		{	ConstructionObject c=(ConstructionObject)e.nextElement();
			if (c.getName().equals(name)) return c;
			if (c==until) break;
		}
		return null;
	}

	/**
	* See, if the first object is prior to the second.
	*/	
	public boolean before (ConstructionObject first,
		ConstructionObject second)
	{	Enumeration e=V.elements();
		while (e.hasMoreElements())
		{	ConstructionObject c=(ConstructionObject)e.nextElement();
			if (c==first) return true;
			if (c==second) break;
		}
		return false;
	}
	
	/**
	* See, if one Object on directly depends on another o.
	*/
	public boolean dependsDirectlyOn (ConstructionObject o, ConstructionObject on)
	{	Enumeration e=o.depending();
		while (e.hasMoreElements())
		{	if (on==e.nextElement()) return true;
		}
		return false;
	}
	
	/**
	 * Unmark recursion flag of all objects.
	 */
	public void clearRekFlags ()
	{	Enumeration e=elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			o.setRekFlag(false);
		}
	}	
	
	/**
	 * Check of object o depends on object on.
	 * @param o
	 * @param on
	 * @return true or false
	 */
	public boolean dependsOn (ConstructionObject o, ConstructionObject on)
	{	clearRekFlags(); // used as markers
		boolean res=dependsOnRek(o,on);
		return res;
	}
	
	/**
	 * Rekursive check, if o depends on object on. Clear constructables before,
	 * since they are marked as already checked!
	 * @param o
	 * @param on
	 * @return true or false
	 */
	public boolean dependsOnRek (ConstructionObject o, ConstructionObject on)
	{	//System.out.println(o.getName()+" depends on "+on.getName()+"?");
		o.setRekFlag(true);
		if (o==on) return true;
		ConstructionObject o1[]=o.getDepArray();
		for (int i=0; i<o1.length; i++)
		{	// System.out.println(o.getName()+" - check: "+o1[i].getName());
			if (o1[i]==o || o1[i].isRekFlag()) continue;
			// System.out.println("flag not set.");
			if (dependsOnRek(o1[i],on)) return true;
		}
		return false;
	}
	
	/**
	 * Reorder the construction completely, so that no forwared references
	 * occur. This is a simple algorithm using tail chain length in dircted
	 * graphs. The construction must not contain circles, however.
	 *
	 */
	public void reorderConstruction ()
	{	// Get the objects into a faster array
		ConstructionObject o[]=new ConstructionObject[V.size()];
		V.copyInto(o);
		int n=o.length;
		if (n==0) return;
		// Compute tail chain length for all objects recursively
		for (int i=0; i<n; i++) 
		{	o[i].Scratch=0; 
			o[i].Flag=false; 
		}
		for (int i=0; i<n; i++)
		{	countTail(((ConstructionObject)o[i]));
			/* 
			// Test print:
			System.out.println(((ConstructionObject)o[i]).getName()+" "+
					((ConstructionObject)o[i]).Scratch);
			Enumeration e=((ConstructionObject)o[i]).depending();
			while (e.hasMoreElements())
			{	System.out.println("- "+((ConstructionObject)e.nextElement()).getName());
			}
			*/
		}
		// Sort the array using bubble sort (least distroying sort)
		if (n<500)
		{	for (int i=1; i<n; i++)
			{	int k=o[i].Scratch;
				int j=i;
				while (j>0 && (o[j-1]).Scratch>k) j--;
				if (j<i)
				{	ConstructionObject oh=o[i];
					for (int h=i; h>j; h--) o[h]=o[h-1];
					o[j]=oh;
				}
			}
		}
		else // Use quick sort
		{	for (int i=0; i<o.length; i++) o[i].Value=o[i].Scratch;
			Sorter.sort(o);
		}
		// Copy back all objects into the construction
		V=new Vector();
		for (int i=0; i<n; i++) V.addElement(o[i]);
	}
	
	/**
	 * Recursive help routine for the reordering.
	 * Returns the maximal tail length of the object o.
	 * It is assumed that marked objects already know their correct
	 * tail length.
	 * @param o
	 * @return tail length
	 */
	public int countTail (ConstructionObject o)
	{	if (o.Flag) return o.Scratch;
		o.Flag=true;
		int max=0;
		ConstructionObject oc[]=o.getDepArray();
		if (oc.length==0)
		{	o.Scratch=0;
		}
		else
		{	for (int i=0; i<oc.length; i++)
			{	if (oc[i]==o) continue;
				int k=countTail(oc[i]);
				if (k>max) max=k;
			}
			o.Scratch=max+1;
		}
		return o.Scratch;
	}
	
	boolean NeedsOrdering=false;
	
	/**
	 * Set the reordering flag.
	 */
	public void needsOrdering ()
	{	NeedsOrdering=true;
	}
	
	/**
	 * If the construction needs reording, order it!
	 */
	public void doOrder ()
	{	if (!NeedsOrdering) return;
		reorderConstruction();
		NeedsOrdering=false;
	}
	
	/**
	 * Walk through the construction and see, if any element contains
	 * a forward dependency.
	 */
	public void computeNeedsOrdering ()
	{	// none checked so far
		Enumeration e=V.elements();
		while (e.hasMoreElements())
		{	((ConstructionObject)e.nextElement()).Flag=false;
		}
		// check all
		e=V.elements();
		while (e.hasMoreElements())
		{	Enumeration ec=((ConstructionObject)e.nextElement()).depending();
			while (ec.hasMoreElements())
			{	if (!((ConstructionObject)ec.nextElement()).Flag)
				{	NeedsOrdering=true;
					return;
				}
			}
		}	
		NeedsOrdering=false;
	}
	
	/**
	 * 
	 * @param o
	 * @return Index of the object o in the vector.
	 */
	public int indexOf (ConstructionObject o)
	{	return V.indexOf(o);
	}
	
	/**
	 * 
	 * @param o
	 * @return Last object, o depends on or nil
	 */
	public ConstructionObject lastDep (ConstructionObject o)
	{	int res=-1;
		ConstructionObject ores=null;
		Enumeration e=o.depending();
		while (e.hasMoreElements())
		{	ConstructionObject u=(ConstructionObject)e.nextElement();
			int i=indexOf(u);
			if (i>res)
			{	res=i; ores=u; 
			}
		}
		return ores;
	}
	
	/**
	 * Set the object o1 right after o2, if possible. Never put an element
	 * to a later position!
	*/
	public boolean reorder (ConstructionObject o1, ConstructionObject o2)
	{	/*// old routine changing internal order
		int k1=indexOf(o1),k2=-1;
		if (o2!=null) k2=indexOf(o2);
		if (k1<=k2+1) return false;
		ConstructionObject o=lastDep(o1);
		if (o!=null && indexOf(o)>k2) return false;
		V.removeElement(o1);
		V.insertElementAt(o1,k2+1);
		*/
		// new routine changing generation numbers
		int n=-1;
		if (o2!=null) n=o2.getNCount();
		Enumeration e=V.elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o.getNCount()>n)
			o.setNCount(o.getNCount()+1);
		}
		o1.setNCount(n+1);
		haveChanged();
		return true;
	}
	
	/**
	 * Update the texts of all objects that contain oldname, but not of the
	 * object o itself.
	 * @param o
	 * @param oldname
	 */
	public void updateTexts (ConstructionObject o, String oldname)
	{	Enumeration e=V.elements();
		while (e.hasMoreElements())
		{	ConstructionObject u=(ConstructionObject)e.nextElement();
			if (dependsDirectlyOn(u,o)) u.updateText();
		}
	}
	
	public String getJobComment ()
	{	return JobComment;
	}
	public void setJobComment (String s)
	{	JobComment=s;
	}

	public void updateCircleDep ()
	{	Enumeration e=V.elements();
		while (e.hasMoreElements())
		{	((ConstructionObject)e.nextElement()).clearCircleDep();
		}
		e=V.elements();
		while (e.hasMoreElements())
		{	((ConstructionObject)e.nextElement()).updateCircleDep();
		}
	}
	
	public Vector getParameters () { return Parameters; }
	public int countParameters ()
	{	if (Parameters==null) return 0;
		return Parameters.size();
	}
	public Vector getTargets () { return Targets; }
	public int countTargets ()
	{	if (Targets==null) return 0;
		return Targets.size();
	}
	
	public void addParameter (ConstructionObject o) { Parameters.addElement(o); }
	public void addTarget (ConstructionObject o) { Targets.addElement(o); }
	
	public void clearParameters ()
	{	Enumeration e=elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			o.clearParameter();
			o.setSpecialParameter(false);
		}
		Parameters=new Vector(); 
	}
	public void clearTargets ()
	{	Enumeration e=elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			o.setTarget(false);
		}
		Targets=new Vector();
	}

	public void testParameter (ConstructionObject o)
		throws ConstructionException
	{	throw new ConstructionException(Zirkel.name("exception.null"));
	}	

	Interpreter Int=new Interpreter(this);
	
	/**
	This is to interpret a single line of input in descriptive mode or
	a single line read from a construction description in a file.
	*/
	public void interpret (ZirkelCanvas zc, String s, String comment)
		throws ConstructionException
	{	Int.interpret(zc,s,comment);
	}
	
	public void interpret (ZirkelCanvas zc, String s)
	throws ConstructionException
	{	Int.interpret(zc,s,"");
	}
	
	boolean IntersectionBecameInvalid;
	
	public void dovalidate ()
	{	doOrder(); // will do something only if necessary
		while (true)
		{	Enumeration e=elements();
			boolean stop=true;
			IntersectionBecameInvalid=false;
			while (e.hasMoreElements())
			{	ConstructionObject o=(ConstructionObject)e.nextElement();
				boolean valid=o.valid();
				o.validate();
				if (o instanceof IntersectionObject && valid && !o.valid())
				{	IntersectionBecameInvalid=true;
				}
				if (o.changedBy()>1e-6) stop=false;
			}
			if (stop) break;
		}
	}
	
	/**
	 * @return During last dovalidate an intersection became invalid.
	 */
	public boolean intersectionBecameInvalid ()
	{	return IntersectionBecameInvalid;
	}
	
	public void dovalidateDebug ()
	{	doOrder(); // will do something only if necessary
		System.out.println("--- Time validate() ---");
		while (true)
		{	Enumeration e=elements();
			boolean stop=true;
			while (e.hasMoreElements())
			{	ConstructionObject o=(ConstructionObject)e.nextElement();
				long time=System.currentTimeMillis();
				for (int i=0; i<100; i++) o.validate();
				time=System.currentTimeMillis()-time;
				if (time>0)
					System.out.println(o.getName()+" - "+(time/100.0)+" msec");
				if (o.changedBy()>1e-6) stop=false;
			}
			if (stop) break;
		}
	}

	/**
	 * Validate only o and those objects it depends on. This is
	 * a recursive function. To avoid problems with cycles we set flags.
	 * @param o
	 */
	public void validate (ConstructionObject o, ConstructionObject avoid)
	{	if (o.RekValidating) return; // should not happen!
		o.RekValidating=true;
		if (o.VRek==null) o.VRek=new MyVector();
		else o.VRek.removeAllElements();
		Enumeration e=elements();
		while (e.hasMoreElements())
		{	((ConstructionObject)e.nextElement()).setRekFlag(false);
		}
		recursiveValidate(o,avoid);
		e=elements();
		while (e.hasMoreElements())
		{	ConstructionObject oc=(ConstructionObject)e.nextElement();
			if (oc.isRekFlag()) o.VRek.addElement(oc);
		}
		// System.out.println("---");
		e=o.VRek.elements();
		while (e.hasMoreElements())
		{	ConstructionObject oc=(ConstructionObject)e.nextElement();
			oc.validate();
			// System.out.println("Validating "+oc.getName());
		}		
		o.RekValidating=false;
	}
	
	public void recursiveValidate (ConstructionObject o, ConstructionObject avoid)
	{	if (o.isRekFlag() || o==avoid) return;
		o.setRekFlag(true);
		ConstructionObject d[]=o.getDepArray();
		for (int i=0; i<d.length; i++)
		{	recursiveValidate(d[i],avoid);
		}
	}
	
	public void computeTracks (ZirkelCanvas zc)
	{	Enumeration e=elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o instanceof TrackObject) ((TrackObject)o).compute(zc);
		}
	}
	
	public void clearTranslations ()
	{	Enumeration e=elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			o.setTranslation(null);
		}
	}

	/**
	* Walk through the construction and mark all items, which are
	* constructable from the set parameter items. This is used to
	* determine the possible targets.
	* 
	*/
	public void determineConstructables ()
	{	Enumeration e=elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o.isParameter()) o.setFlag(true);
			else
			{	Enumeration ee=o.depending();
				boolean constructable=
						(o instanceof ExpressionObject || o instanceof FunctionObject);
				while (ee.hasMoreElements())
				{	ConstructionObject oo=
						(ConstructionObject)ee.nextElement();
					if (o!=oo)
					{	if (oo.isFlag()) constructable=true;
						else
						{	constructable=false; break;
						}
					}
				}
				o.setFlag(constructable);
				// if (constructable) System.out.println(o.getName());
			}
		}
	}

	/**
	* Recursively go throught the objects, which o needs, and mark them
	* as constructable, until a parameter object has been reached.
	* The object flags are used and must be cleared before.
	* @see clearConstructables
	* @param o
	* @return Object is constructable.
	*/
	public boolean determineConstructables (ConstructionObject o)
	{	if (o.isFlag()) return true;
		ConstructionObject dep[]=o.getDepArray();
		boolean constructable=
			(o instanceof ExpressionObject || o instanceof FunctionObject);
		for (int i=0; i<dep.length; i++)
		{	if (dep[i]==null) return false;
			if (dep[i]==o) continue;
			if (!determineConstructables(dep[i])) return false;
			else constructable=true;
		}
		o.setFlag(constructable);
		return true;
	}
	
	/**
	 * Unmark constructable flag of all objects.
	 *
	 */
	public void clearConstructables ()
	{	Enumeration e=elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			o.setFlag(false);
		}
	}
	
	/**
	* Walk through the objects and determine all children of any
	* object, which is marked constructable. Mark those children as
	* constructable too.
	*/
	public void determineChildren ()
	{	Enumeration e=elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (!o.isFlag())
			{	Enumeration ee=o.depending();
				while (ee.hasMoreElements())
				{	ConstructionObject oo=
						(ConstructionObject)ee.nextElement();
					if (oo.isFlag())
					{	o.setFlag(true); break;
					}
				}
			}
		}

	}

	/**
	 * Mark all parameter objects as constructables.
	 *
	 */
	public void setParameterAsConstructables ()
	{	Enumeration e=elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o.isParameter() || o.isMainParameter())
				o.setFlag(true);
		}
	}

	public String[] getPromptFor ()
	{	String s[]=new String[PromptFor.size()];
		PromptFor.copyInto(s);
		return s;
	}
	
	boolean ShouldSwitch=false,NoteSwitch=false;
	
	public boolean shouldSwitch ()
	{	return ShouldSwitch;
	}
	
	public boolean noteSwitch ()
	{	return NoteSwitch;
	}
	
	public void shouldSwitch (boolean flag, boolean note)
	{	ShouldSwitch=flag; NoteSwitch=note;
	}
	
	public void shouldSwitch (boolean flag)
	{	ShouldSwitch=flag; NoteSwitch=true;
	}
	
	public void switchBack ()
	{	Enumeration e=elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o instanceof IntersectionObject)
				((IntersectionObject)o).switchBack();
		}
	}

	public void clearSwitches ()
	{	Enumeration e=elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o instanceof IntersectionObject)
				((IntersectionObject)o).switchBack();
		}
	}

	public boolean haveSwitched ()
	{	Enumeration e=elements();
		while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (o instanceof IntersectionObject)
				if (((IntersectionObject)o).isSwitched()) return true;
		}
		return false;
	}

	public boolean changed ()
	{	return Changed;
	}
	
	public ChangedListener CL=null;
	
	public void haveChanged ()
	{	changed(true);
		if (CL!=null) CL.notifyChanged();
	}
	
	public void changed (boolean flag)
	{	Changed=flag;
	}
	
	public MyVector TranslatorList=new MyVector();

	public void addTranslator (Translator t)
	{	TranslatorList.addElement(t);
	}
	
	public void runTranslators (Construction from)
	{	Enumeration e=TranslatorList.elements();
		while (e.hasMoreElements())
		{	Translator t=(Translator)e.nextElement();
			t.laterTranslate(from);
		}
	}
	
	public void clearTranslators ()
	{	TranslatorList.removeAllElements();
	}
	
	/**
	 * Set the count the maximal count number of all objects plus 1.
	 * (After loading e.g.)
	 */
	public void updateCount ()
	{	int max=0;
		Enumeration e=V.elements();
		while (e.hasMoreElements())
		{	int n=((ConstructionObject)e.nextElement()).getNCount();
			if (n>max) max=n;
		}
		Count=max+1;
	}

	Vector VOld=null;
	
	/**
	 * Reset the constuction to the original order, saving the current one,
	 * or use the current one again.
	 * @param original or current
	 */
	public void setOriginalOrder (boolean flag)
	{	if (V==null) return;
		if (flag)
		{	ConstructionObject o[]=new ConstructionObject[V.size()];
			V.copyInto(o);
			for (int i=0; i<o.length; i++) o[i].Value=(o[i].getNCount());
			Sorter.sort(o);
			Vector W=new Vector();
			for (int i=0; i<o.length; i++) W.addElement(o[i]);
			VOld=V;
			V=W;
		}
		else if (VOld!=null)
		{	V=VOld;
			VOld=null;
		}
	}

	public Construction getTranslation ()
	{	return TranslateInto;
	}
	
	public void setTranslation (Construction C)
	{	TranslateInto=C;
	}

	public boolean Loading=false;	
	
	public boolean loading () { return Loading; }

	Vector Errors=new Vector();
	
	public void addError (String s)
	{	Errors.addElement(s);
	}
	
	public Enumeration getErrors ()
	{	return Errors.elements();
	}
	
	public void clearErrors ()
	{	Errors.removeAllElements();
	}

	public void dontAlternate (boolean flag)
	{	DontAlternateIntersections=flag;
	}
	
	public boolean canAlternate ()
	{	return !DontAlternateIntersections;
	}

	double Pixel=100; // Pixel per unit
	
	/**
	 * Called by the ZirkelCanvas to set the pixel per units.
	 * This is used by the pixel() function in Expression.
	 * @param pixel
	 */
	public void setPixel (double pixel)
	{	Pixel=pixel;
	}
	
	public double getPixel ()
	{	return Pixel;
	}
}

