package rene.zirkel;

// file: ZirkelFrame.java

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

import rene.dialogs.*;
import rene.gui.*;
import rene.util.*;
import rene.util.parser.StringParser;
import rene.util.xml.XmlWriter;
import rene.zirkel.construction.*;
import rene.zirkel.constructors.*;
import rene.zirkel.dialogs.*;
import rene.zirkel.help.*;
import rene.zirkel.listener.*;
import rene.zirkel.macro.*;
import rene.zirkel.objects.*;
import rene.zirkel.tools.*;


class ZirkelFrameLoadThread implements Runnable
{	String Name; 
	ZirkelFrame ZF; 
	public ZirkelFrameLoadThread (ZirkelFrame zf, String name)
	{	Name=name; ZF=zf; 
		new Thread(this).start(); 
	}
	public void run ()
	{	ZF.doload(Name); 
		ZF.setEnabled(true);
		ZF.ZC.requestFocus(); 
	}
}

class ShowWarning
	implements Runnable
{	ZirkelFrame ZF;
	String S;
	public ShowWarning (ZirkelFrame zf, String s)
	{	ZF=zf; S=s;
		new Thread(this).start();
	}
	public void run ()
	{	try 
		{	Thread.sleep(1000);
		} 
		catch (InterruptedException e) 
		{	e.printStackTrace();
		}
		ZF.warning(S);
	}
}

public class ZirkelFrame extends CloseFrame
	implements StatusListener, KeyListener, IconBarListener, DoneListener,
		ZirkelCanvasInterface
{	public ZirkelCanvas ZC; 
	Label Status; 
	public String Filename=""; 
	CheckboxMenuItem ShowHidden,Grid,TestJob,Partial,PartialLines,Vectors,
		IncludeMacros,LongNames,ShowNames,ShowValues,Visual,FullIcons,
		IsoScale,Obtuse,Solid,Compress,RestrictedMode,
		FontBold,FontLarge,RestrictIcons,Beginner,AlwaysClearMacros,
		LargeFont,BoldFont,PrintScalePreview,ConstructionDisplay,LeftSnap,Restricted,
		MacroBar,DefaultMacrosInBar; 
	IconBar IA,IB;
	MacroBar IM; 
	HistoryTextField Input; 
	String Background=""; 
	
	// color menu items:
	static Color DefaultColors[]={
		Color.black,
			Color.green.darker().darker(),
			Color.blue.darker(),
			new Color(150,100,0),
			Color.cyan.darker().darker(),
			new Color(180,0,0)}; 
	public static Color Colors[]=DefaultColors; 
	public static Color LightColors[]; 
	public static Color BrighterLightColors[]; 
	public static Color BrighterColors[]; 
	public static Color SelectColor=Global.getParameter("colorselect",
		new Color(255,0,0)); 
	public static Color IndicateColor=Global.getParameter("colorselect",
		new Color(250,180,50)); 
	public static Color TargetColor=Global.getParameter("colortarget",Color.pink); 
	
	public static String ColorStrings[]=
	{	"black","green","blue","brown","cyan","red"
	}; 
	public static String PointTypes[]=
	{	"square","diamond","circle","dot","cross","dcross"
	}; 
	public static String ColorTypes[]=
	{	"normal","thick","thin"
	}; 
	public static int ColorTypeKeys[]={	KeyEvent.VK_5,KeyEvent.VK_6,KeyEvent.VK_7}; 
	CheckboxMenuItem ColorTypeMenuItems[]=new CheckboxMenuItem[ColorTypes.length]; 
	public static int ColorKeys[]={	KeyEvent.VK_1,KeyEvent.VK_2,KeyEvent.VK_3,KeyEvent.VK_4}; 
	CheckboxMenuItem ColorMenuItems[]=new CheckboxMenuItem[ColorStrings.length]; 
	CheckboxMenuItem ShowColorMenuItems[]=new CheckboxMenuItem[ColorStrings.length]; 
	static int PointKeys[]={KeyEvent.VK_5,KeyEvent.VK_6,KeyEvent.VK_7,KeyEvent.VK_8,KeyEvent.VK_9,KeyEvent.VK_0}; 
	CheckboxMenuItem PointMenuItems[]=new CheckboxMenuItem[PointTypes.length]; 
	
	// arrays for the object tools
	final public static String ObjectStrings[]= // names
	{	"point","boundedpoint","intersection",
		"line","ray","segment","fixedsegment",
		"circle","circle3","fixedcircle",
		"parallel","plumb","midpoint",
		"angle","fixedangle",
		"move","tracker","objecttracker","animate",
		"expression","area","quadric","image","text",
		"hide","runmacro",
		"edit","parameter","targets","definejob","delete","reorder",
		"draw","rename","zoom","animatebreak"
	}; 
	final public static String Separators[]= // names
	{	"point","boundedpoint","intersection",
		"!line","ray","segment","fixedsegment",
		"!circle","circle3","fixedcircle",
		"!parallel","plumb","midpoint",
		"!angle","fixedangle",
		"!move","tracker","objecttracker","animate",
		"!expression","area","quadric","image","text",
		"!hide","runmacro",
		"edit","parameter","targets","definejob","delete","reorder",
		"draw","rename","zoom","animatebreak"
	}; 
	final public static String MenuTitles[]=
	{	"points","lines","circles","complex","angles","move","decorative"
	}; 
	final public static int IconNumber=26; 
	final public static int NEdit=IconNumber,NParameters=IconNumber+1,
		NTargets=IconNumber+2,NDefineJob=IconNumber+3,
		NDelete=IconNumber+4,NReorder=IconNumber+5,NDraw=IconNumber+6,
		NRename=IconNumber+7,NZoom=IconNumber+8,NAnimateBreak=IconNumber+9; 
	final public static int NAnimator=18,NObjectTracker=17,NTracker=16,NMover=15; 
	final public static int NMacro=IconNumber-1; 
	public static ObjectConstructor ObjectConstructors[]= // constructors
	{	new PointConstructor(),
		new BoundedPointConstructor(),
		new IntersectionConstructor(),
		new LineConstructor(),
		new RayConstructor(),
		new SegmentConstructor(),
		new SegmentConstructor(true),
		new CircleConstructor(),
		new Circle3Constructor(),
		new CircleConstructor(true),
		new ParallelConstructor(),
		new PlumbConstructor(),
		new MidpointConstructor(),
		new AngleConstructor(),
		new AngleConstructor(true),
		new MoverTool(),
		new Tracker(),
		new ObjectTracker(),
		new AnimatorTool(),
		new ExpressionConstructor(),
		new AreaConstructor(),
		new QuadricConstructor(),
		new ImageConstructor(),
		new TextConstructor(),
		new HiderTool(),
		new MacroRunner(),
		new EditTool(),
		new SetParameterTool(),
		new SetTargetsTool(),
		new SaveJob(),
		new DeleteTool(),
		new ReorderTool(),
		new DrawerTool(),
		new RenamerTool(),
		new ZoomerTool(),
		new BreakpointAnimator(),
	}; 
	CheckboxMenuItem ObjectMenuItems[]=
		new CheckboxMenuItem[ObjectConstructors.length]; // menu checkbos items
	static char ObjectKeys[]; 
	int CurrentTool=0; // current tool
	boolean IsApplet; 
	
	Panel North,Center,MainPanel; 
	Panel StatusPanel,InputPanel;
	Panel CenterPanel;
	boolean Init=false;
	boolean SawPreviewWarning=false; 
	
	MyFileDialog FileLoad,FileSave,PicSave,HTMLSave,BackgroundLoad,
		ImageLoad,TemplateLoad; 
	// The file dialogs
	
	LogoWindow Logo;
	
	public ZirkelFrame (boolean applet)
	{	super(Zirkel.name("program.name")); // set window title
		IsApplet=applet; 
		if (applet)
		{	addWindowListener( // to close properly
			new WindowAdapter ()
			{	public void windowClosing (WindowEvent e)
				{	close(); 
				}
			}
			); 
		}
		else
		{	addWindowListener( // to close properly
			new WindowAdapter ()
			{	public void windowClosed (WindowEvent e)
				{	Global.saveProperties("CaR Properties"); 
					Global.exit(0); 
				}
			}); 
		}
		
		if (!applet) Logo=new LogoWindow(this);
		
		// create menu bar
		MenuBar menu=new MenuBar(); 
		setMenuBar(menu); 
		
		// file menu:
		Menu file=new MyMenu(Zirkel.name("menu.file")); 
		menuadd(file,"menu.file.new"); 
		menuadd(file,"menu.file.clearmacros"); 
		file.addSeparator(); 
		menuadd(file,"menu.file.load"); 
		menuadd(file,"menu.file.save"); 
		menuadd(file,"menu.file.saveas"); 
		file.addSeparator(); 
		AlwaysClearMacros=menuaddcheck(file,"menu.file.alwaysclearmacros"); 
		AlwaysClearMacros.setState(Global.getParameter(
			"load.clearmacros",true)); 
		IncludeMacros=menuaddcheck(file,"menu.file.includemacros"); 
		IncludeMacros.setState(Global.getParameter(
			"save.includemacros",true)); 
		Compress=menuaddcheck(file,"menu.file.compress"); 
		Compress.setState(Global.getParameter("save.compress",false)); 
		file.addSeparator(); 
		menuadd(file,"menu.file.loadjob"); 
		file.addSeparator(); 
		menuadd(file,"menu.file.loadrun"); 
		menuadd(file,"menu.file.editrun"); 
		file.addSeparator(); 
		menuadd(file,"menu.file.print"); 
		if (!Global.getParameter("restricted",false))
		{	IsoScale=menuaddcheck(file,"menu.file.print.isoscale"); 
			IsoScale.setState(Global.getParameter("print.isoscale",false)); 
		}
		file.addSeparator();
		PrintScalePreview=menuaddcheck(file,"menu.options.printscalepreview");
		PrintScalePreview.setState(false); 
		Global.setParameter("printscalepreview",false);
		menuadd(file,"menu.file.savepng"); 
		menuadd(file,"menu.file.copypng"); 
		file.addSeparator(); 
		Menu sg=new MyMenu(Zirkel.name("menu.file.savegraphics")); 
		menuadd(sg,"menu.file.saveeps"); 
		menuadd(sg,"menu.file.savepdf"); 
		sg.addSeparator(); 
		menuadd(sg,"menu.file.savesvg"); 
		menuadd(sg,"menu.file.savefig"); 
		file.add(sg);
		file.addSeparator(); 
		menuadd(file,"menu.options.exportsettings"); 
		file.addSeparator(); 
		menuadd(file,"menu.file.exit"); 
		menu.add(file); 
		
		// objects menu:
		Menu objects=new MyMenu(Zirkel.name("menu.actions","Actions")); 
		int i; 
		ObjectKeys=new char[ObjectStrings.length]; 
		for (i=0; i<ObjectStrings.length; i++)
		{	String shortcut=Zirkel.name("shortcuts."+ObjectStrings[i]); 
			ObjectMenuItems[i]=new CheckboxMenuItemAction(this,
				Zirkel.name("objects."+ObjectStrings[i])
				+" ("+shortcut+")",
				ObjectStrings[i]); 
			if (shortcut.length()>0)
				ObjectKeys[i]=shortcut.charAt(0); 
		}
		int nsub=0; 
		Menu sub=new MyMenu(Zirkel.name("menu.actions."+MenuTitles[nsub])); 
		for (i=0; i<NEdit; i++)
		{	if (Separators[i].startsWith("!"))
			{	objects.add(sub); 
				nsub++; 
				if (nsub>=MenuTitles.length)
				{	while (i<NEdit)
					{	if (enabled(ObjectStrings[i]))
						{	objects.add(ObjectMenuItems[i]); 
						}
						i++; 
					}
					break; 
				}
				sub=new MyMenu(Zirkel.name("menu.actions."+MenuTitles[nsub])); 
			}
			if (enabled(ObjectStrings[i]))
			{	sub.add(ObjectMenuItems[i]); 
			}
		}
		ObjectMenuItems[CurrentTool].setState(true); 
		if (!Global.getParameter("restricted",false) || icon("function"))
			menuadd(objects,"menu.options.function"); 
		if (!Global.getParameter("restricted",false) || icon("function"))
			menuadd(objects,"menu.options.userfunction"); 
		objects.addSeparator(); 
		menuadd(objects,"menu.options.back"); 
		objects.add(ObjectMenuItems[NDelete]); 
		menuadd(objects,"menu.options.undo"); 
		objects.addSeparator(); 
		if (!Global.getParameter("restricted",false) || icon("rename"))
			objects.add(ObjectMenuItems[NRename]); 
		if (!Global.getParameter("restricted",false) || icon("reorder"))
			objects.add(ObjectMenuItems[NReorder]); 
		if (!Global.getParameter("restricted",false) || icon("objecttracker"))
			menuadd(objects,"menu.options.track"); 
		menuadd(objects,"menu.options.hideduplicates");
		menuadd(objects,"menu.options.editlast"); 
		objects.addSeparator(); 
		objects.add(ObjectMenuItems[NDraw]); 
		menuadd(objects,"menu.options.cleardraw"); 
		menu.add(objects); 
		
		// options menu:
		Menu options=new MyMenu(Zirkel.name("menu.options","Options")); 
		
		Visual=menuaddcheck(options,"menu.options.visual"); 
		Visual.setState(Global.getParameter("options.visual",true)); 
		if (!enabled("visual")) options.remove(Visual); 
		else options.addSeparator(); 
		
		ShowHidden=menuaddcheck(options,"menu.options.hidden"); 
		if (!enabled("hidden")) options.remove(ShowHidden); 
		
		Menu oshowcolor=new MyMenu(Zirkel.name("menu.options.showcolor")); 
		ShowColorMenuItems[0]=
			menuaddcheck(oshowcolor,Zirkel.name("menu.options.all")+" "+
			Zirkel.name("shortcuts.showcolor."+0),
			"scs-"+ColorStrings[0]); 
		for (i=1; i<ColorStrings.length; i++)
			ShowColorMenuItems[i]=
			menuaddcheck(oshowcolor,
			Zirkel.name("colors."+ColorStrings[i])+
			" "+Zirkel.name("shortcuts.showcolor."+i),
			"scs-"+ColorStrings[i]); 
		options.add(oshowcolor); 
		
		options.addSeparator();
		
		menuadd(options,"menu.options.comment"); 
		// menuadd(options,"menu.options.constructiondisplay"); 
		
		options.addSeparator();
		Grid=menuaddcheck(options,"menu.options.grid");
		LeftSnap=menuaddcheck(options,"menu.options.leftsnap");
		LeftSnap.setState(Global.getParameter("grid.leftsnap",false));
		menuadd(options,"menu.options.editgrid");
		options.addSeparator();
		
		Menu background=new MyMenu(Zirkel.name("menu.background")); 
		menuadd(background,"menu.background.grab"); 
		menuadd(background,"menu.background.clear"); 
		background.addSeparator();
		menuadd(background,"menu.background.load"); 
		background.addSeparator();
		menuaddcheck(background,"menu.background.usesize")
			.setState(Global.getParameter("background.usesize",false)); 
		menuaddcheck(background,"menu.background.usewidth")
			.setState(Global.getParameter("background.usewidth",false)); 
		menuaddcheck(background,"menu.background.tile")
			.setState(Global.getParameter("background.tile",true)); 
		menuaddcheck(background,"menu.background.center")
			.setState(Global.getParameter("background.center",true)); 
		options.add(background); 
		
		Menu zoom=new MyMenu(Zirkel.name("menu.zoom")); 
		zoom.add(ObjectMenuItems[NZoom]); 
		menuadd(zoom,"menu.zoom.in"); 
		menuadd(zoom,"menu.zoom.out"); 
		menuadd(zoom,"menu.zoom.left"); 
		menuadd(zoom,"menu.zoom.right"); 
		menuadd(zoom,"menu.zoom.up"); 
		menuadd(zoom,"menu.zoom.down"); 
		options.add(zoom); 
		
		options.addSeparator(); 
		
		Menu ocolor=new MyMenu(Zirkel.name("menu.options.defaultcolor")); 
		for (i=0; i<ColorStrings.length; i++)
		{	String shortcut=Zirkel.name("shortcuts.color."+i); 
			if (!shortcut.equals(""+i))
				ColorMenuItems[i]=
				menuaddcheck(ocolor,
				Zirkel.name("colors."+ColorStrings[i])+" "+shortcut,
				"cs-"+ColorStrings[i]); 
			else
				ColorMenuItems[i]=
				menuaddcheck(ocolor,
				Zirkel.name("colors."+ColorStrings[i]),
				"cs-"+ColorStrings[i]); 
		}
		options.add(ocolor); 
		Menu otype=new MyMenu(Zirkel.name("menu.options.defaulttype")); 
		for (i=0; i<PointTypes.length; i++)
			PointMenuItems[i]=
			menuaddcheck(otype,
			Zirkel.name("point.type."+PointTypes[i])+
			" "+Zirkel.name("shortcuts.pointtype."+i),
			"pt-"+PointTypes[i]); 
		options.add(otype); 
		Menu othickness=new MyMenu(Zirkel.name("menu.options.defaultthickness")); 
		for (i=0; i<ColorTypes.length; i++)
			ColorTypeMenuItems[i]=
			menuaddcheck(othickness,
			Zirkel.name("color.type."+ColorTypes[i])+
			" "+Zirkel.name("shortcuts.thickness."+i),
			"ct-"+ColorTypes[i]); 
		options.add(othickness); 
		
		Menu other=new MyMenu(Zirkel.name("menu.options.other")); 
		Restricted=menuaddcheck(other,"menu.options.restricted"); 
		Partial=menuaddcheck(other,"menu.options.partial"); 
		PartialLines=menuaddcheck(other,"menu.options.plines"); 
		Vectors=menuaddcheck(other,"menu.options.arrow"); 
		ShowNames=menuaddcheck(other,"menu.options.shownames"); 
		ShowValues=menuaddcheck(other,"menu.options.showvalues"); 
		LargeFont=menuaddcheck(other,"menu.options.largefont"); 
		BoldFont=menuaddcheck(other,"menu.options.boldfont"); 
		LongNames=menuaddcheck(other,"menu.options.longnames"); 
		Obtuse=menuaddcheck(other,"menu.options.obtuse"); 
		Solid=menuaddcheck(other,"menu.options.solid"); 
		options.add(other); 
		
		if (!Global.getParameter("restricted",false))
			menu.add(options); 
		
		// settings menu
		Menu settings=new MyMenu(Zirkel.name("menu.moresettings"));
		
		RestrictIcons=new CheckboxMenuItemAction(this,
				Zirkel.name("menu.special.restricticons"),
				"menu.special.restricticons"); 
		
		ConstructionDisplay=menuaddcheck(settings,"menu.settings.constructiondisplay");
		ConstructionDisplay.setState(Global.getParameter("options.showdisplay",true));
		settings.addSeparator();

		if (!Global.getParameter("restricted",false))
		{	settings.add(RestrictIcons);
			menuadd(settings,"menu.options.editicons"); 
			
			settings.addSeparator();

			menuadd(settings,"menu.options.sizes");
			
			settings.addSeparator();
			
			FontBold=menuaddcheck(settings,"menu.settings.font.bold"); 
			FontBold.setState(Global.getParameter("font.bold",false)); 
			FontLarge=menuaddcheck(settings,"menu.settings.font.large"); 
			FontLarge.setState(Global.getParameter("font.large",false)); 
			
			settings.addSeparator();
			 
			menuadd(settings,"menu.options.setdigits"); 
			
			settings.addSeparator();
			 
			Menu colors=new MyMenu(Zirkel.name("menu.colors")); 
			menuadd(colors,"colors.default"); 
			menuadd(colors,"colors.color0"); 
			menuadd(colors,"colors.color1"); 
			menuadd(colors,"colors.color2"); 
			menuadd(colors,"colors.color3"); 
			menuadd(colors,"colors.color4"); 
			menuadd(colors,"colors.color5"); 
			menuadd(colors,"colors.select"); 
			menuadd(colors,"colors.target"); 
			menuadd(colors,"colors.background");
			settings.add(colors); 
		
			settings.addSeparator();
			 
			menuadd(settings,"menu.settings");
			
		}

		if (!Locale.getDefault().toString().startsWith("en"))
			settings.add(new MenuItemAction(this,Zirkel.name("menu.settings.language")+" (Set Language)",
				"menu.settings.language"));
		else menuadd(settings,"menu.settings.language");
		settings.addSeparator(); 
		
		RestrictedMode=menuaddcheck(settings,"menu.settings.restricted"); 
		RestrictedMode.setState(Global.getParameter("restricted",false)); 
		Beginner=menuaddcheck(settings,"menu.settings.beginner"); 
		Beginner.setState(Global.getParameter("beginner",false)); 
		
		menu.add(settings);
		
		
		// macro menu
		Menu macros=new MyMenu(Zirkel.name("menu.macros")); 
		macros.add(ObjectMenuItems[NParameters]); 
		macros.add(ObjectMenuItems[NTargets]); 
		menuadd(macros,"menu.special.definemacro"); 
		macros.add(ObjectMenuItems[NMacro]); 
		macros.addSeparator(); 
		menuadd(macros,"menu.special.loadmacros"); 
		menuadd(macros,"menu.special.savemacros"); 
		macros.addSeparator(); 
		menuadd(macros,"menu.special.renamemacro"); 
		menuadd(macros,"menu.special.deletemacros"); 
		macros.addSeparator(); 
		MacroBar=menuaddcheck(macros,"menu.special.macrobar"); 
		MacroBar.setState(Global.getParameter("macrobar",true));
		DefaultMacrosInBar=menuaddcheck(macros,"menu.special.defaultmacrosinbar"); 
		DefaultMacrosInBar.setState(Global.getParameter("defaultmacrosinbar",true));
		if (!Global.getParameter("restricted",false)) menu.add(macros); 		
		
		// special menu:
		Menu special=new MyMenu(Zirkel.name("menu.special")); 
		special.add(ObjectMenuItems[NDefineJob]); 
		menuadd(special,"menu.special.jobcomment"); 
		TestJob=menuaddcheck(special,"menu.special.testjob"); 
		menuadd(special,"menu.file.savejob"); 
		special.addSeparator(); 
		menuadd(special,"menu.special.export"); 
		menuadd(special,"menu.special.exporttemplate"); 
		special.addSeparator(); 
		menuadd(special,"menu.special.replay"); 
		if (!Global.getParameter("restricted",false)) menu.add(special); 
		Menu bp=new MyMenu(Zirkel.name("menu.special.break"));
		menuadd(bp,"menu.bp.setbreak");
		menuadd(bp,"menu.bp.sethidingbreak");
		menuadd(bp,"menu.bp.clearbreak");
		bp.addSeparator();
		menuadd(bp,"menu.bp.animatebreak");
		special.add(bp);
		
		// help menu:
		Menu help=new MyMenu(Zirkel.name("menu.help")); 
		menuadd(help,"menu.help.about"); 
		menuadd(help,"menu.help.info"); 
		help.addSeparator(); 
		menuadd(help,"menu.help.browser"); 
		menuadd(help,"menu.help.configure"); 
		help.addSeparator(); 
		menuadd(help,"menu.file.loadexamples"); 
		help.addSeparator(); 
		if (haveHelp("gui.txt"))
		{	Menu texthelp=new MyMenu(Zirkel.name("menu.help.text")); 
			menuadd(texthelp,"menu.help.help"); 
			menuadd(texthelp,"menu.help.gui"); 
			menuadd(texthelp,"menu.help.tools"); 
			menuadd(texthelp,"menu.help.macros"); 
			menuadd(texthelp,"menu.help.interactive"); 
			menuadd(texthelp,"menu.help.tips"); 
			help.add(texthelp); 
		}
		menuadd(help,"menu.help.welcome"); 
		menu.add(help); 
		
		// the canvas
		ZC=new ZirkelCanvas(); 
		if (Global.Background!=null) ZC.setBackground(Global.Background); 
		ZC.setBackground(Global.getParameter("colorbackground",ZC.getBackground())); 
		ZC.addMouseListener(ZC); 
		ZC.addMouseMotionListener(ZC); 
		ZC.setZirkelCanvasListener(this); 
		setLayout(new BorderLayout()); 
		
		Center=makeCenterPanel(); 
		
		ZC.setTool(ObjectConstructors[CurrentTool]); 
		ZC.addKeyListener(this); 
		ZC.setFrame(this); 
		
		// the Status line
		Status=new MyLabel(""); 
		add("South",StatusPanel=new Panel3D(Status)); 
		ZC.addStatusListener(this); 
		ZC.showStatus(); 
		
		Input=new HistoryTextField(this,"Input"); 
		InputPanel=new Panel3D(Input); 
		ZC.setTextField(Input); 
		
		// Icon Bar at North
		makeIconBar(); 
		Panel north=new MyPanel(); 
		north.setLayout(new GridLayout(0,1)); 
		north.add(IA); 
		if (IB!=IA) north.add(IB);
		if (IM!=null) north.add(IM);
		North=new Panel3D(north); 
		
		// add the center panel, depending on the north or south icon bar
		makeMainPanel(); 
		add("Center",MainPanel); 
		
		// Icon
		seticon("rene/zirkel/icon.png"); 
		
		// init various things
		initLightColors(); 
		initFileDialogs(); 
		
		// initialize choices
		settool(0); 
		setcolor(Global.getParameter("options.color",0)); 
		settype(Global.getParameter("options.type",2)); 
		setcolortype(Global.getParameter("options.colortype",0)); 
		showcolor(0); 
		sethidden(false);
		
		setRestricted(Global.getParameter("options.restricted",true));
		setPartial(Global.getParameter("options.partial",false)); 
		setPartialLines(Global.getParameter("options.plines",false)); 
		setVectors(Global.getParameter("options.arrow",false)); 
		setShowNames(Global.getParameter("options.shownames",false)); 
		setShowValues(Global.getParameter("options.showvalues",false)); 
		setLongNames(Global.getParameter("options.longnames",false)); 
		setLargeFont(Global.getParameter("options.largefont",false)); 
		setBoldFont(Global.getParameter("options.boldfont",false)); 
		setObtuse(Global.getParameter("options.obtuse",false)); 
		setSolid(Global.getParameter("options.solid",false)); 
		
		if (Global.Background!=null) setBackground(Global.Background); 
		
		// show:
		pack();
		setLocation(100,40); 
		setPosition("zirkelframe"); 
		
		loadDefaultMacros();
		updateMacroBar();
		
		Init=true; 
		setVisible(true); 
		clearsettings();

	}
	
	final static public String DefaultIcons=
		" new load save back undo delete color type thickness"+
		" hidden showcolor macro grid comment replay"+
		" point line segment ray circle fixedcircle"+
		" parallel plumb circle3 midpoint angle fixedangle"+
		" move tracker objecttracker hide expression area text quadric"+
		" runmacro edit animate "+
		" info zoom draw function rename "; 
	final static public String DefaultRestrictedIcons=
		" back undo color"+
		" hidden showcolor macro grid comment"+
		" point line segment ray circle"+
		" parallel plumb circle3 midpoint angle fixedangle"+
		" move tracker objecttracker hide area text quadric"+
		" runmacro zoom info "+
		" "; 
	
	/**
	Generate an icon bar and insert some icons. The icon bar
	is inserrted into the frame at North.
	*/
	public void makeIconBar ()
	{	String icons=Global.getParameter("icons",DefaultIcons); 
		if (RestrictIcons.getState())
			Global.setParameter("icons",
				Global.getParameter("restrictedicons",DefaultRestrictedIcons));
		else
			Global.setParameter("icons",icons); 
		if (RestrictIcons.getState()?
			Global.getParameter("icons",DefaultIcons).indexOf("twolines")>=0:
			Global.getParameter("options.fullicons",true))
		{	IA=new IconBar(this,false); 
			IA.addKeyListener(this); 
			if (icon("new")) IA.addLeft("new"); 
			if (icon("load")) IA.addLeft("load"); 
			if (icon("save")) IA.addLeft("save"); 
			IA.addSeparatorLeft(); 
			if (icon("back")) IA.addLeft("back"); 
			if (icon("delete")) IA.addToggleLeft("delete"); 
			if (icon("undo")) IA.addLeft("undo"); 
			IA.addSeparatorLeft(); 
			if (icon("edit")) IA.addToggleLeft("edit"); 
			if (icon("draw")) IA.addToggleLeft("draw"); 
			if (icon("rename")) IA.addToggleLeft("rename");
			if (icon("macro")) IA.addMultipleToggleIconLeft("macro",3); 
			IA.addSeparatorLeft(); 
			if (icon("comment")) IA.addLeft("comment"); 
			if (icon("function")) IA.addLeft("function");
			if (icon("replay")) IA.addLeft("replay"); 
			if (icon("animatebreak")) IA.addToggleLeft("animatebreak"); 
			IA.addSeparatorLeft(); 
			if (icon("color")) IA.addMultipleIconLeft("color",6); 
			if (icon("type")) IA.addMultipleIconLeft("type",6); 
			if (icon("thickness")) IA.addMultipleIconLeft("thickness",3); 
			if (icon("partial")) IA.addOnOffLeft("partial"); 
			if (icon("plines")) IA.addOnOffLeft("plines"); 
			if (icon("arrow")) IA.addOnOffLeft("arrow"); 
			IA.addSeparatorLeft(); 
			if (icon("showname")) IA.addOnOffLeft("showname"); 
			if (icon("longnames")) IA.addOnOffLeft("longnames"); 
			if (icon("large")) IA.addOnOffLeft("large"); 
			if (icon("bold")) IA.addOnOffLeft("bold"); 
			if (icon("showvalue")) IA.addOnOffLeft("showvalue"); 
			if (icon("obtuse")) IA.addOnOffLeft("obtuse"); 
			if (icon("solid")) IA.addOnOffLeft("solid"); 
			IA.addSeparatorLeft(); 
			if (icon("zoom")) IA.addToggleLeft("zoom"); 
			if (icon("grid")) IA.addOnOffLeft("grid"); 
			if (icon("grab")) IA.addOnOffLeft("grab"); 
			IA.addSeparatorLeft(); 
			if (icon("hidden")) IA.addOnOffLeft("hidden"); 
			if (icon("showcolor")) IA.addMultipleIconLeft("showcolor",Colors.length); 
			if (icon("visual")) IA.addOnOffLeft("visual");
			IA.addSeparatorLeft(); 
			if (icon("info")) IA.addLeft("info"); 
			IA.setIconBarListener(this); 
			IB=new IconBar(this,false); 
			IB.addKeyListener(this); 
			IB.setIconBarListener(this); 
			int n=0; 
			for (int i=0; i<IconNumber; i++)
			{	if (Separators[i].startsWith("!")) n++; 
				if (icon(ObjectStrings[i])) n++; 
			}
			String a[]=new String[n]; 
			for (int i=0,k=0; i<IconNumber; i++)
			{	if (Separators[i].startsWith("!"))
				a[k++]=""; 
				if (icon(ObjectStrings[i]))
					a[k++]=ObjectStrings[i]; 
			}
			IB.addToggleGroupLeft(a); 
		}
		else
		{	IB=new IconBar(this,false); 
			IB.addKeyListener(this); 
			IB.setIconBarListener(this); 
			int n=0; 
			for (int i=0; i<IconNumber; i++)
			{	if (Separators[i].startsWith("!")) n++; 
				if (icon(ObjectStrings[i])) n++; 
			}
			String a[]=new String[n]; 
			for (int i=0,k=0; i<IconNumber; i++)
			{	if (Separators[i].startsWith("!"))
				a[k++]=""; 
				if (icon(ObjectStrings[i]))
					a[k++]=ObjectStrings[i]; 
			}
			IB.addToggleGroupLeft(a); 

			IA=IB; 
			if (icon("back")) IB.addRight("back"); 
			if (icon("delete")) IA.addToggleLeft("delete"); 
			if (icon("undo")) IA.addLeft("undo");
			IA.addSeparatorLeft(); 
			if (icon("macro")) IB.addMultipleToggleIconRight("macro",3); 
			if (icon("replay")) IB.addRight("replay"); 
			if (icon("edit")) IA.addToggleLeft("edit"); 
			if (icon("zoom")) IA.addToggleLeft("zoom"); 
			if (icon("draw")) IA.addToggleLeft("draw"); 
			if (icon("rename")) IA.addToggleLeft("rename");
			IA.addSeparatorLeft(); 
			if (icon("color")) IA.addMultipleIconLeft("color",6); 
			if (icon("type")) IA.addMultipleIconLeft("type",6); 
			if (icon("thickness")) IA.addMultipleIconLeft("thickness",3); 
			if (icon("partial")) IA.addOnOffLeft("partial"); 
			if (icon("plines")) IA.addOnOffLeft("plines"); 
			if (icon("arrow")) IA.addOnOffLeft("arrow"); 
			if (icon("showname")) IA.addOnOffLeft("showname"); 
			if (icon("showvalue")) IA.addOnOffLeft("showvalue"); 
			if (icon("longnames")) IA.addOnOffLeft("longnames"); 
			if (icon("large")) IA.addOnOffLeft("large"); 
			if (icon("bold")) IA.addOnOffLeft("bold"); 
			if (icon("obtuse")) IA.addOnOffLeft("obtuse"); 
			if (icon("solid")) IA.addOnOffLeft("solid"); 

			if (icon("showcolor")) IB.addMultipleIconLeft("showcolor",Colors.length); 
			if (icon("hidden")) IB.addOnOffRight("hidden"); 
			if (icon("grid")) IB.addOnOffRight("grid"); 
			if (icon("info")) IB.addRight("info"); 
		}
		makeMacroBar();
		Global.setParameter("icons",icons); 
	}
	
	public void makeMacroBar ()
	{	if (Global.getParameter("macrobar",true))
		{	IM=new MacroBar(this);
			IM.addKeyListener(this); 
			IM.setIconBarListener(this);
			ZC.setMacroBar(IM);
		}
		else
		{	if (IM!=null)
			{	IM.removeIconBarListener(this);
			}
			IM=null;
			ZC.setMacroBar(IM);
		}
	}
	
	public void updateMacroBar ()
	{	ZC.updateMacroBar();
	}
	
	public void remakeIconBar ()
	{	IA.removeIconBarListener(this); 
		IB.removeIconBarListener(this); 
		remove(MainPanel); 
		makeIconBar();
		makeMacroBar();
		updateMacroBar();
		Panel north=new MyPanel(); 
		north.setLayout(new GridLayout(0,1)); 
		north.add(IA); 
		if (IA!=IB) north.add(IB);
		if (IM!=null) north.add(IM);
		North=new Panel3D(north); 
		makeMainPanel(); 
		add("Center",MainPanel); 
		validate(); 
		doLayout(); 
		clearsettings(); 
		repaint(); 
	}
	
	public boolean icon (String s)
	{	return Global.getParameter("icons","none").indexOf(" "+s+" ")>=0; 
	}
	
	public boolean enabled (String s)
	{	return !Global.getParameter("restricted",false) || icon(s); 
	}
	
	public void initFileDialogs ()
	{	if (Global.getParameter("options.filedialog",true))
		{	FileLoad=new MyFileDialog(this,
				Zirkel.name("filedialog.open"),
				Zirkel.name("filedialog.open.action"),
				false,true); 
			FileLoad.setDispose(false); 
			FileLoad.setPattern("*.zir *.job *.zirz *.jobz"); 
			FileLoad.loadHistories("file","","");
			FileSave=new MyFileDialog(this,
				Zirkel.name("filedialog.saveas"),
				Zirkel.name("filedialog.saveas.action"),
				true,true); 
			FileSave.setPattern("*.zir *.job *.zirz *.jobz"); 
			FileSave.setDispose(false); 
			FileSave.loadHistories("file","","");
			PicSave=new MyFileDialog(this,
				Zirkel.name("filedialog.saveas"),
				Zirkel.name("filedialog.saveas.action"),
				true,true); 
			PicSave.setPattern("*.zir *.job *.zirz *.jobz"); 
			PicSave.setDispose(false); 
			PicSave.loadHistories("image","","jpg");
			HTMLSave=new MyFileDialog(this,
				Zirkel.name("filedialog.htmlsave"),
				Zirkel.name("filedialog.htmlsave.action"),
				true,true); 
			HTMLSave.setPattern("*.html *.htm"); 
			HTMLSave.setDispose(false); 
			HTMLSave.loadHistories("html","","html");
			BackgroundLoad=new MyFileDialog(this,
				Zirkel.name("filedialog.backgroundload"),
				Zirkel.name("filedialog.backgroundload.action"),
				false,true); 
			BackgroundLoad.setPattern("*.gif *.jpg *.png"); 
			BackgroundLoad.setDispose(false); 
			BackgroundLoad.loadHistories("image","","jgp");
			ImageLoad=new MyFileDialog(this,
					Zirkel.name("filedialog.imageload"),
					Zirkel.name("filedialog.imageload.action"),
					false,true); 
			ImageLoad.setPattern("*.gif *.jpg *.png"); 
			ImageLoad.setDispose(false); 
			ImageLoad.loadHistories("image","","jgp");
			TemplateLoad=new MyFileDialog(this,
				Zirkel.name("templateload.open"),
				Zirkel.name("templateload.open.action"),
				false,true); 
			TemplateLoad.setPattern("*.template"); 
			TemplateLoad.setDispose(false); 
			TemplateLoad.loadHistories("template","","template");
		}
		else
		{	FileLoad=new MyFileDialog(this,
				Zirkel.name("filedialog.open"),
				false); 
			FileLoad.setPattern("*.zir *.job *.zirz *.jobz"); 
			FileSave=new MyFileDialog(this,
				Zirkel.name("filedialog.saveas"),
				true); 
			FileSave.setPattern("*.zir *.job *.zirz *.jobz"); 
			PicSave=new MyFileDialog(this,
				Zirkel.name("filedialog.saveas"),
				true); 
			PicSave.setPattern("*"); 
			HTMLSave=new MyFileDialog(this,
				Zirkel.name("filedialog.htmlsave"),
				true); 
			HTMLSave.setPattern("*.html *.htm"); 
			BackgroundLoad=new MyFileDialog(this,
				Zirkel.name("filedialog.backgroundload"),
				false); 
			BackgroundLoad.setPattern("*.gif *.jpg"); 
			ImageLoad=new MyFileDialog(this,
					Zirkel.name("filedialog.imageload"),
					false); 
				ImageLoad.setPattern("*.gif *.jpg"); 
			TemplateLoad=new MyFileDialog(this,
				Zirkel.name("templateload.open"),
				false); 
			TemplateLoad.setPattern("*.template"); 
		}
	}
	
	public static void initLightColors (Color back)
	{	int n=DefaultColors.length; 
		Colors=new Color[DefaultColors.length]; 
		for (int i=0; i<n; i++)
		{	if (Global.haveParameter("color"+i))
			{	Colors[i]=Global.getParameter("color"+i,Color.black); 
			}
			else Colors[i]=DefaultColors[i]; 
		}
		LightColors=new Color[n]; 
		BrighterLightColors=new Color[n]; 
		BrighterColors=new Color[n]; 
		if (back==null) back=Color.gray.brighter(); 
		int red=back.getRed(),green=back.getGreen(),blue=back.getBlue(); 
		double lambda=0.4; 
		for (int i=0; i<n; i++)
		{	int r=(int)(red*(1-lambda)+Colors[i].getRed()*lambda); 
			int g=(int)(green*(1-lambda)+Colors[i].getGreen()*lambda); 
			int b=(int)(blue*(1-lambda)+Colors[i].getBlue()*lambda); 
			LightColors[i]=new Color(r,g,b); 
			if (i==0) BrighterColors[i]=Color.gray; 
			else BrighterColors[i]=Colors[i].brighter(); 
			BrighterLightColors[i]=LightColors[i].brighter(); 
		}
	}
	
	public void initLightColors ()
	{	initLightColors(Color.white); 
	}
	
	public CheckboxMenuItem menuaddcheck (Menu m, String o, String s)
		// adds a menu item to the Menu m
	{	CheckboxMenuItem item=new CheckboxMenuItemAction(this,o,s); 
		m.add(item); 
		return item; 
	}
	public CheckboxMenuItem menuaddcheck (Menu m, String o)
	{	return menuaddcheck(m,Zirkel.name(o),o); 
	}
	
	public MenuItem menuadd (Menu m, String o)
		// adds a menu item to the Menu m
	{	MenuItem item=new MenuItemAction(this,Zirkel.name(o),o); 
		m.add(item); 
		return item; 
	}
	
	public void doAction (String s)
		// interpret menu items
	{	if (!Init) return;
		ZC.pause(true); 
		if (s.equals("menu.file.exit"))
		{	setinfo("save"); 
			doclose(); 
		}
		else if (s.equals("menu.file.save"))
		{	setinfo("save"); 
			save(); 
		}
		else if (s.equals("menu.file.clearmacros"))
		{	setinfo("macro"); 
			clearNonprotectedMacros(); 
		}
		else if (s.equals("menu.file.saveas"))
		{	setinfo("save"); 
			saveas(); 
		}
		else if (s.equals("menu.file.load"))
		{	setinfo("save"); 
			load(); 
		}
		else if (s.equals("menu.file.loadexamples"))
		{	setinfo("save"); 
			loadExamples(); 
		}
		else if (s.equals("menu.file.loadrun"))
		{	setinfo("run"); 
			loadRun(); 
		}
		else if (s.equals("menu.file.editrun"))
		{	setinfo("run"); 
			editRun(OldRun); 
		}
		else if (s.equals("menu.file.print"))
		{	setinfo("print"); 
			print(); 
		}
		else if (s.equals("menu.file.savepng"))
		{	setinfo("print"); 
			savePNG(); 
		}
		else if (s.equals("menu.file.copypng"))
		{	setinfo("print"); 
			copyPNG(); 
		}
		else if (s.equals("menu.file.savefig"))
		{	setinfo("print"); 
			saveFIG(); 
		}
		else if (s.equals("menu.file.savesvg"))
		{	setinfo("print"); 
			saveSVG(); 
		}
		else if (s.equals("menu.file.savepdf"))
		{	setinfo("print"); 
			savePDF(); 
		}
		else if (s.equals("menu.file.saveeps"))
		{	setinfo("print"); 
			saveEPS(); 
		}
		else if (s.equals("menu.special.loadmacros"))
		{	setinfo("macro"); 
			loadMacros(); 
		}
		else if (s.equals("menu.special.savemacros"))
		{	setinfo("macro"); 
			saveMacros(); 
		}
		else if (s.equals("menu.special.deletemacros"))
		{	setinfo("macro"); 
			deleteMacros(); 
		}
		else if (s.equals("menu.special.renamemacro"))
		{	setinfo("macro"); 
			renameMacro(); 
		}
		else if (s.equals("menu.file.loadjob"))
		{	setinfo("assignment"); 
			loadJob(); 
		}
		else if (s.equals("menu.file.savejob"))
		{	setinfo("assignment"); 
			saveJob(); 
		}
		else if (s.equals("menu.file.new"))
		{	setinfo("start"); 
			newfile(false);
		}
		else if (s.equals("menu.options.back"))
		{	setinfo("delete"); 
			ZC.back(); 
			ZC.repaint(); 
		}
		else if (s.equals("menu.options.undo"))
		{	setinfo("undo"); 
			ZC.undo(); 
			ZC.repaint(); 
		}
		else if (s.equals("menu.options.track"))
		{	setinfo("tracker"); 
			track(); 
		}
		else if (s.equals("menu.options.hideduplicates"))
		{	setinfo("hide"); 
			ZC.hideDuplicates();
			ZC.repaint();
		}
		else if (s.equals("menu.options.comment"))
		{	setinfo("comment"); 
			showcomment(); 
		}
		else if (s.equals("menu.special.jobcomment"))
		{	setinfo("comment"); 
			showjobcomment(); 
		}
		else if (s.equals("menu.special.definemacro"))
		{	setinfo("macro"); 
			definemacro(); 
		}
		else if (s.equals("menu.special.replay"))
		{	setinfo("replay"); 
			replay(); 
		}
		else if (s.equals("menu.options.constructiondisplay"))
		{	setinfo("construction"); 
			showconstruction(); 
		}
		else if (s.equals("menu.options.setdigits"))
		{	setinfo("defaults"); 
			setDigits(); 
		}
		else if (s.equals("menu.settings.language"))
		{	setinfo("language"); 
			setLanguage(); 
		}
		else if (s.equals("menu.options.editicons"))
		{	setinfo("iconbar"); 
			editIcons(); 
		}
		else if (s.equals("menu.options.function"))
		{	setinfo("function");
			ZC.createCurve();
		}
		else if (s.equals("menu.options.userfunction"))
		{	setinfo("function");
			ZC.createFunction();
		}
		else if (s.equals("menu.options.editlast"))
		{	setinfo("edit");
			ZC.editLast();
		}
		else if (s.equals("menu.settings"))
		{	setinfo("settings"); 
			boolean iconbartop=Global.getParameter("options.iconbartop",true); 
			boolean filedialog=Global.getParameter("options.filedialog",true); 
			new SettingsDialog(this); 
			if (Global.getParameter("options.iconbartop",true)!=iconbartop)
				itemAction("menu.settings.iconbartop",
				Global.getParameter("options.iconbartop",true)); 
			if (Global.getParameter("options.filedialog",true)!=filedialog)
				initFileDialogs();
			ZC.newImage();
		}
		else if (s.equals("menu.options.exportsettings"))
		{	setinfo("exportsettings"); 
			new ExportSettingsDialog(this); 
			ZC.newImage();
		}
		else if (s.equals("menu.help.browser"))
		{	setinfo("browser"); 
			browser(); 
		}
		else if (s.equals("menu.help.info"))
		{	info();
			ZC.pause(false);
			return; 
		}
		else if (s.equals("menu.help.configure"))
		{	setinfo("browser"); 
			configure(); 
		}
		else if (s.equals("menu.help.help"))
		{	setinfo("start"); 
			new Help("schoolgeometry.txt"); 
			ZC.pause(false);
			return; 
		}
		else if (s.equals("menu.help.gui"))
		{	setinfo("start"); 
			new Help("gui.txt"); 
			ZC.pause(false);
			return; 
		}
		else if (s.equals("menu.help.macros"))
		{	setinfo("start"); 
			new Help("macros.txt"); 
			ZC.pause(false);
			return; 
		}
		else if (s.equals("menu.help.tools"))
		{	setinfo("start"); 
			new Help("tools.txt"); 
			ZC.pause(false);
			return; 
		}
		else if (s.equals("menu.help.tips"))
		{	setinfo("start"); 
			new Help("tips.txt"); 
			ZC.pause(false);
			return; 
		}
		else if (s.equals("menu.help.interactive"))
		{	setinfo("start"); 
			new Help("interactiv.txt"); 
			ZC.pause(false);
			return; 
		}
		else if (s.equals("menu.help.about"))
		{	setinfo("start"); 
			new AboutDialog(this); 
		}
		else if (s.equals("menu.help.welcome"))
		{	setinfo("start"); 
			new Help("version.txt"); 
			ZC.pause(false);
			return; 
		}
		else if (s.equals("menu.special.export"))
		{	setinfo("htmlexport"); 
			exportHTML(); 
		}
		else if (s.equals("menu.special.exporttemplate"))
		{	setinfo("htmlexporttemplate"); 
			exportTemplateHTML(); 
		}
		else if (s.equals("menu.zoom.in"))
		{	setinfo("zoomer"); 
			ZC.magnify(1/Math.sqrt(Math.sqrt(2))); 
		}
		else if (s.equals("menu.zoom.out"))
		{	setinfo("zoomer"); 
			ZC.magnify(Math.sqrt(Math.sqrt(2))); 
		}
		else if (s.equals("menu.zoom.left"))
		{	setinfo("zoomer"); 
			ZC.shift(-0.1,0); 
		}
		else if (s.equals("menu.zoom.right"))
		{	setinfo("zoomer"); 
			ZC.shift(0.1,0); 
		}
		else if (s.equals("menu.zoom.up"))
		{	setinfo("zoomer"); 
			ZC.shift(0,0.1); 
		}
		else if (s.equals("menu.zoom.down"))
		{	setinfo("zoomer"); 
			ZC.shift(0,-0.1); 
		}
		else if (s.equals("Input"))
		{	try
			{	ZC.getConstruction().interpret(ZC,Input.getText()); 
				ZC.validate(); 
				ZC.getConstruction().updateCircleDep(); 
				ZC.repaint(); 
				Input.remember(); 
				Input.setText(""); 
				loadsettings(); 
				Input.requestFocus();
				ZC.pause(false);
				return;
			}
			catch (ConstructionException e)
			{	warning(e.getDescription()); 
			}
		}
		else if (s.equals("colors.default"))
		{	setinfo("colors"); 
			for (int i=0; i<Colors.length; i++)
				Global.removeParameter("color"+i); 
			Global.removeParameter("colorbackground"); 
			Global.removeParameter("colorselect"); 
			Global.removeParameter("colortarget"); 
			ZC.setBackground(getBackground()); 
			Center.setBackground(getBackground());
			if (ZC.CDP!=null)
			{	ZC.CDP.setListingBackground(getBackground());
				if (CenterPanel!=null)
					CenterPanel.setBackground(getBackground());
			}
			initLightColors(); 
			SelectColor=Global.getParameter("colorselect",SelectColor); 
			TargetColor=Global.getParameter("colortarget",TargetColor); 
			if (ZC.CDP!=null)
			{	ZC.CDP.setListingBackground(Global.getParameter("colorbackground",
					Color.white));
			}
			ZC.repaint(); 
		}
		else if (s.startsWith("colors.color"))
		{	setinfo("colors"); 
			try
			{	int c=Integer.parseInt(s.substring("colors.color".length())); 
				ColorEditor ce=new ColorEditor(this,"color"+c,
					Colors[c]); 
				ce.center(this); 
				ce.setVisible(true); 
				initLightColors(); 
				ZC.repaint(); 
			}
			catch (Exception e) {	}
		}
		else if (s.equals("colors.background"))
		{	setinfo("colors"); 
			ColorEditor ce=new ColorEditor(this,"colorbackground",
				getBackground()); 
			ce.center(this); 
			ce.setVisible(true); 
			initLightColors();
			if (Global.haveParameter("colorbackground"))
			{	ZC.setBackground(Global.getParameter("colorbackground",
					Color.white));
				if (ZC.CDP!=null)
				{	ZC.CDP.setListingBackground(Global.getParameter("colorbackground",
						Color.white));
					if (CenterPanel!=null)
						CenterPanel.setBackground(Global.getParameter("colorbackground",
								Color.white));
				}
				Center.setBackground(Global.getParameter("colorbackground",
					Color.white)); 
			}
			ZC.repaint(); 
		}
		else if (s.equals("colors.select"))
		{	setinfo("colors"); 
			ColorEditor ce=new ColorEditor(this,"colorselect",
				SelectColor); 
			ce.center(this); 
			ce.setVisible(true); 
			SelectColor=Global.getParameter("colorselect",SelectColor); 
			ZC.repaint(); 
		}
		else if (s.equals("colors.target"))
		{	setinfo("colors"); 
			ColorEditor ce=new ColorEditor(this,"colortarget",
				SelectColor); 
			ce.center(this); 
			ce.setVisible(true); 
			TargetColor=Global.getParameter("colortarget",TargetColor); 
			ZC.repaint(); 
		}
		else if (s.equals("menu.background.grab"))
		{	setinfo("background"); 
			dograb(true); 
		}
		else if (s.equals("menu.background.clear"))
		{	setinfo("background"); 
			dograb(false); 
		}
		else if (s.equals("menu.background.load"))
		{	setinfo("background"); 
			loadBackground(); 
		}
		else if (s.equals("menu.options.sizes"))
		{	new SizesDialog(this);
			ZC.resetGraphics();
		}
		else if (s.equals("menu.options.cleardraw"))
		{	setinfo("draw"); 
			ZC.clearDrawings(); 
		}
		else if (s.equals("menu.bp.setbreak"))
		{	ZC.breakpointLast(true,false);
		}
		else if (s.equals("menu.bp.clearbreak"))
		{	ZC.breakpointLast(false,false);
		}
		else if (s.equals("menu.bp.sethidingbreak"))
		{	ZC.breakpointLast(true,true);
		}
		else if (s.equals("menu.bp.animatebreak"))
		{	settool(NAnimateBreak);
		}
		else if (s.equals("menu.options.editgrid"))
		{	editGrid(); 
			setinfo("grid"); 
		}
		ZC.pause(false);
		ZC.requestFocus();
	}
	
	public void clear (boolean defaults)
	{	ZC.clear(); 
		Count.resetAll(); 
		TestJob.setState(false); 
		clearsettings(defaults); 
		ZC.clearDrawings();
		ZC.repaint(); 
	}
	
	public void clearsettings (boolean defaults)
	{	if (defaults)
		{	settool(0); 
			setcolor(0);
			setcolortype(0); 
			settype(2); 
			showcolor(0); 
			setRestricted(true); 
			setPartial(false); 
			setPartialLines(false); 
			setVectors(false); 
			setShowNames(false); 
			setShowValues(false); 
			setLongNames(false); 
			setLargeFont(false); 
			setBoldFont(false); 
			setObtuse(false); 
			setSolid(false); 
			setVisual(true); 
			sethidden(false);
		}
		else
		{	settool(0); 
			setcolor(Global.getParameter("options.color",0));
			setcolortype(Global.getParameter("options.colortype",0)); 
			settype(Global.getParameter("options.type",2)); 
			showcolor(0); 
			setRestricted(Global.getParameter("options.restricted",true)); 
			setPartial(Global.getParameter("options.partial",false)); 
			setPartialLines(Global.getParameter("options.plines",false)); 
			setVectors(Global.getParameter("options.arrow",false)); 
			setShowNames(Global.getParameter("options.shownames",false)); 
			setShowValues(Global.getParameter("options.showvalues",false)); 
			setLongNames(Global.getParameter("options.longnames",false)); 
			setLargeFont(Global.getParameter("options.largefont",false)); 
			setBoldFont(Global.getParameter("options.boldfont",false)); 
			setObtuse(Global.getParameter("options.obtuse",false)); 
			setSolid(Global.getParameter("options.solid",false)); 
			setVisual(Global.getParameter("options.visual",true)); 
			sethidden(false);
		}
	}
	public void clearsettings ()
	{	clearsettings(false);
	}
	
	public void loadsettings ()
	{	setcolor(ZC.getDefaultColor()); 
		settype(ZC.getDefaultType()); 
		setcolortype(ZC.getDefaultColorType()); 
		setPartial(ZC.getPartial()); 
		setPartialLines(ZC.getPartialLines()); 
		setVectors(ZC.getVectors()); 
		setShowNames(ZC.getConstruction().ShowNames); 
		setShowValues(ZC.getConstruction().ShowValues); 
	}
	
	public void itemAction (String o, boolean flag)
		// interpret checkbox changes
	{	for (int i=0; i<ObjectMenuItems.length; i++)
		{	if (o.equals(ObjectStrings[i]))
			{	if (i==NMacro)
				{	setinfo("runmacro"); 
					runMacro(false); 
				}
				else
				{	setinfo(ObjectStrings[i]); 
					settool(i); 				
				}
				return; 
			}
		}
		for (int i=0; i<ColorMenuItems.length; i++)
		{	if (o.equals("cs-"+ColorStrings[i]))
			{	setcolor(i); 
				setinfo("defaults"); 
				return; 
			}
		}
		for (int i=0; i<ShowColorMenuItems.length; i++)
		{	if (o.equals("scs-"+ColorStrings[i]))
			{	showcolor(i); 
				setinfo("show"); 
				return; 
			}
		}
		for (int i=0; i<PointMenuItems.length; i++)
		{	if (o.equals("pt-"+PointTypes[i]))
			{	settype(i); 
				setinfo("defaults"); 
				return; 
			}
		}
		for (int i=0; i<ColorTypeMenuItems.length; i++)
		{	if (o.equals("ct-"+ColorTypes[i]))
			{	setcolortype(i); 
				setinfo("defaults"); 
				return; 
			}
		}
		if (o.equals("menu.options.hidden"))
		{	sethidden(flag);
			ZC.reloadCD();
			setinfo("hide"); 
		}
		else if (o.equals("menu.file.includemacros"))
		{	IncludeMacros.setState(flag); 
			Global.setParameter("save.includemacros",flag); 
			setinfo("save"); 
		}
		else if (o.equals("menu.file.alwaysclearmacros"))
		{	AlwaysClearMacros.setState(flag); 
			Global.setParameter("load.clearmacros",flag); 
			setinfo("save"); 
		}
		else if (o.equals("menu.options.visual"))
		{	setVisual(flag); 
			setShowNames(!flag);
			setinfo("visual"); 
		}
		else if (o.equals("menu.options.printscalepreview"))
		{	if (flag)
			{	ExportScaler d=new ExportScaler(this,true);
				if (d.isAborted()) flag=false;
			} 
			Global.setParameter("printscalepreview",flag);
			PrintScalePreview.setState(flag);
			ZC.newImage();
			setinfo("print"); 
		}
		else if (o.equals("menu.file.compress"))
		{	Global.setParameter("save.compress",flag); 
			setinfo("save"); 
		}
		else if (o.equals("menu.options.partial"))
		{	setPartial(flag); 
			setinfo("defaults"); 
		}
		else if (o.equals("menu.options.restricted"))
		{	setRestricted(flag); 
			setinfo("defaults"); 
		}
		else if (o.equals("menu.options.plines"))
		{	setPartialLines(flag); 
			setinfo("defaults"); 
		}
		else if (o.equals("menu.options.arrow"))
		{	setVectors(flag); 
			setinfo("defaults"); 
		}
		else if (o.equals("menu.options.longnames"))
		{	setLongNames(flag); 
			setinfo("defaults"); 
		}
		else if (o.equals("menu.options.largefont"))
		{	setLargeFont(flag); 
			setinfo("defaults"); 
		}
		else if (o.equals("menu.options.boldfont"))
		{	setBoldFont(flag); 
			setinfo("defaults"); 
		}
		else if (o.equals("menu.options.shownames"))
		{	setShowNames(flag); 
			setinfo("defaults"); 
		}
		else if (o.equals("menu.options.obtuse"))
		{	setObtuse(flag); 
			setinfo("defaults"); 
		}
		else if (o.equals("menu.options.solid"))
		{	setSolid(flag); 
			setinfo("defaults"); 
		}
		else if (o.equals("menu.options.showvalues"))
		{	setShowValues(flag); 
			setinfo("defaults"); 
		}
		else if (o.equals("menu.options.grid"))
		{	toggleGrid(); 
			setinfo("grid"); 
		}
		else if (o.equals("menu.options.leftsnap"))
		{	Global.setParameter("grid.leftsnap",flag); 
			setinfo("grid"); 
			ZC.repaint();
		}
		else if (o.equals("menu.background.tile"))
		{	Global.setParameter("background.tile",flag); 
			ZC.repaint(); 
			setinfo("background"); 
		}
		else if (o.equals("menu.background.usesize"))
		{	Global.setParameter("background.usesize",flag);
			resize();
			setinfo("background"); 
		}
		else if (o.equals("menu.background.usewidth"))
		{	Global.setParameter("background.usewidth",flag);
			resize();
			setinfo("background"); 
		}
		else if (o.equals("menu.background.center"))
		{	Global.setParameter("background.center",flag); 
			ZC.repaint(); 
			setinfo("background"); 
		}
		else if (o.equals("menu.background.usesize"))
		{	Global.setParameter("background.usesize",flag); 
			if (flag)
			{	resize(); 
			}
			setinfo("background"); 
		}
		else if (o.equals("menu.special.testjob"))
		{	testjob(flag); 
			if (flag && !ZC.getConstruction().getComment().equals(""))
				showcomment(); 
			setinfo("assignments"); 
		}
		else if (o.equals("menu.settings.constructiondisplay"))
		{	Global.setParameter("options.showdisplay",flag);
			showConstructionDisplay(flag);
			setinfo("constructiondisplay");
		}
		else if (o.equals("menu.settings.font.bold"))
		{	Global.setParameter("font.bold",flag); 
			ZC.resetGraphics(); 
			setinfo("fonts"); 
		}
		else if (o.equals("menu.settings.font.large"))
		{	Global.setParameter("font.large",flag); 
			ZC.resetGraphics(); 
			setinfo("fonts"); 
		}
		else if (o.equals("menu.settings.iconbartop"))
		{	Global.setParameter("options.iconbartop",flag); 
			remove(MainPanel); 
			makeMainPanel(); 
			add("Center",MainPanel); 
			validate(); 
			setinfo("iconbar"); 
		}
		else if (o.equals("menu.settings.restricted"))
		{	Global.setParameter("restricted",flag); 
			warning(Zirkel.name("warning.reset")); 
			setinfo("restricted"); 
		}
		else if (o.equals("menu.settings.beginner"))
		{	Global.setParameter("beginner",flag); 
			if (flag)
			{	Global.setParameter("options.indicate",true); 
				Global.setParameter("options.indicate.simple",true); 
				Global.setParameter("options.pointon",true); 
				Global.setParameter("options.intersection",true); 
				Global.setParameter("options.choice",true); 
				Global.setParameter("showtips",true); 
				Global.setParameter("restrictedicons",DefaultRestrictedIcons); 
				Global.setParameter("saveicons",
					Global.getParameter("icons",DefaultIcons)); 
				Global.setParameter("icons",DefaultRestrictedIcons); 
				remakeIconBar(); 
			}
			else
			{	Global.setParameter("options.indicate",true); 
				Global.setParameter("options.indicate.simple",false); 
				Global.setParameter("options.pointon",false); 
				Global.setParameter("options.intersection",false); 
				Global.setParameter("icons",
					Global.getParameter("saveicons",DefaultIcons)); 
				remakeIconBar(); 
			}
			setinfo("beginner"); 
		}
		else if (o.equals("menu.special.macrobar"))
		{	Global.setParameter("macrobar",flag);
			setinfo("macrobar");
			remakeIconBar();
		}
		else if (o.equals("menu.special.defaultmacrosinbar"))
		{	Global.setParameter("defaultmacrosinbar",flag);
			setinfo("macrobar");
			updateMacroBar();
		}
		else if (o.equals("menu.file.print.isoscale"))
		{	Global.setParameter("print.isoscale",flag); 
			setinfo("background"); 
		}
		else if (o.equals("menu.special.restricticons"))
		{	restrictIcons(flag); 
			setinfo("restricted"); 
		}
	}
	
	public void iconPressed (String o)
	{	ZC.pause(true); 
		ZC.requestFocus();
		if (o.equals("load"))
		{	setinfo("save"); 
			load(); 
		}
		else if (o.equals("save"))
		{	setinfo("save"); 
			save(); 
		}
		else if (o.equals("new"))
		{	setinfo("save"); 
			newfile(IA.isControlPressed()); 
			IA.clearShiftControl();
		}
		else if (o.equals("hidden"))
		{	itemAction("menu.options.hidden",IA.getState("hidden")); 
			setinfo("hide"); 
		}
		else if (o.equals("partial"))
		{	setPartial(IA.getState("partial")); 
			setinfo("defaults"); 
		}
		else if (o.equals("plines"))
		{	setPartialLines(IA.getState("plines")); 
			setinfo("defaults"); 
		}
		else if (o.equals("arrow"))
		{	setVectors(IA.getState("arrow")); 
			setinfo("defaults"); 
		}
		else if (o.equals("visual"))
		{	setVisual(IA.getState("visual")); 
			setShowNames(!IA.getState("visual"));
			setinfo("visual"); 
		}
		else if (o.equals("color"))
		{	int n=IA.getMultipleState("color"); 
			if (n>=0) setcolor(n); 
			setinfo("defaults"); 
		}
		else if (o.equals("showcolor"))
		{	int n=IA.getMultipleState("showcolor"); 
			if (n>=0) showcolor(n); 
			setinfo("show"); 
		}
		else if (o.equals("type"))
		{	int n=IA.getMultipleState("type"); 
			if (n>=0) settype(n); 
			setinfo("defaults"); 
		}
		else if (o.equals("thickness"))
		{	int n=IA.getMultipleState("thickness"); 
			if (n>=0) setcolortype(n); 
			setinfo("defaults"); 
		}
		else if (o.equals("showname"))
		{	setShowNames(IA.getState("showname")); 
			setinfo("defaults"); 
		}
		else if (o.equals("showvalue"))
		{	setShowValues(IA.getState("showvalue")); 
			setinfo("defaults"); 
		}
		else if (o.equals("longnames"))
		{	setLongNames(IA.getState("longnames")); 
			setinfo("defaults"); 
		}
		else if (o.equals("large"))
		{	setLargeFont(IA.getState("large")); 
			setinfo("defaults"); 
		}
		else if (o.equals("bold"))
		{	setBoldFont(IA.getState("bold")); 
			setinfo("defaults"); 
		}
		else if (o.equals("obtuse"))
		{	setObtuse(IA.getState("obtuse")); 
			setinfo("defaults"); 
		}
		else if (o.equals("solid"))
		{	setSolid(IA.getState("solid")); 
			setinfo("defaults"); 
		}
		else if (o.equals("grid"))
		{	toggleGrid(); 
			setinfo("grid"); 
		}
		else if (o.equals("back"))
		{	ZC.back(); 
			ZC.repaint(); 
			setinfo("back"); 
		}
		else if (o.equals("undo"))
		{	ZC.undo(); 
			ZC.repaint(); 
			setinfo("undo"); 
		}
		else if (o.equals("comment"))
		{	setinfo("comment"); 
			if (IA.isShiftPressed()) showjobcomment(); 
			else showcomment(); 
		}
		else if (o.equals("grab"))
		{	dograb(); 
			setinfo("background"); 
		}
		else if (o.equals("macro"))
		{	int n=IA.getMultipleState("macro"); 
			switch (n)
			{	case 1 :
					setinfo("parameter"); 
					settool(NParameters);
				break; 
				case 2 :
					setinfo("target"); 
					settool(NTargets); 
				break; 
				case 0 :
					setinfo("macro"); 
					definemacro(); 
				break; 
			}
			IA.setState("macro",true);
		}
		else if (o.equals("replay"))
		{	setinfo("replay"); 
			replay(); 
		}
		else if (o.equals("info"))
		{	info(); 
		}
		else if (o.equals("function"))
		{	setinfo("function"); 
			if (IA.isControlPressed()) ZC.createFunction();
			else ZC.createCurve();
		}
		else if (IB.isControlPressed())
		{	int i=CurrentTool;
			if (o.equals("hide"))
			{	ZC.hideDuplicates();
				ZC.repaint();
			}
			else if (o.equals("runmacro"))
			{	setinfo("runmacro");
				runMacro(true);
				return;
			}
			else if (o.equals("objecttracker"))
			{	track();
			}
			settool(i);
		}	
		else if (IA.isControlPressed())
		{	int i=CurrentTool;
			if (o.equals("edit"))
			{	if (CurrentTool!=NEdit) IA.setState("edit",false);
				ZC.editLast();
				ZC.repaint();
			}
			else if (o.equals("objecttracker"))
			{	track();
			}
			settool(i);
		}
		else 
		{	for (int i=0; i<ObjectMenuItems.length; i++)
			{	if (o.equals(ObjectStrings[i]))
				{	if (i==NMacro)
					{	setinfo("runmacro");
						runMacro(IB.isShiftPressed()); 
					}
					else
					{	settool(i); 
						setinfo(ObjectStrings[i]); 
					}
					ZC.requestFocus(); 
					return; 
				}
			}
			if (IM!=null) // Search in the macro line
			{	Macro m=IM.find(o);
				if (m!=null) runMacro(m);
			}
		}
		IA.clearShiftControl();
		IB.clearShiftControl();
		ZC.pause(false); 
		ZC.requestFocus(); 
	}

	/**
	Choose the tool i and set the icons and menu entries.
	*/
	public void settool (int i)
	{	if (IM!=null) IM.deselectAll();
		ObjectMenuItems[CurrentTool].setState(false); 
		CurrentTool=i; 
		ObjectMenuItems[i].setState(true); 
		ZC.setTool(ObjectConstructors[i]); 
		if (i<IconNumber && IB.have(ObjectStrings[i]))
			IB.toggle(ObjectStrings[i]); 
		else
			IB.unselect("point"); 
		ObjectConstructors[i].resetFirstTime(ZC); 
		if (i==NTargets) IA.setMultipleState("macro",2); 
		else if (i==NParameters) IA.setMultipleState("macro",1); 
		else if (i==NDefineJob) testjob(false); 
		else
		{	IA.setMultipleState("macro",0); 
			IA.setState("macro",false);
		}
		IA.setState("delete",i==NDelete); 
		IA.setState("edit",i==NEdit); 
		IA.setState("draw",i==NDraw); 
		IA.setState("rename",i==NRename); 
		IA.setState("zoom",i==NZoom); 
		IA.setState("animatebreak",i==NAnimateBreak); 
	}
	
	public void setcolor (int c)
	{	for (int i=0; i<ColorMenuItems.length; i++)
		ColorMenuItems[i].setState(false); 
		ColorMenuItems[c].setState(true); 
		IA.setMultipleState("color",c); 
		ZC.setDefaultColor(c); 
		Global.setParameter("options.color",c); 
	}
	
	public void settype (int c)
	{	for (int i=0; i<PointMenuItems.length; i++)
		PointMenuItems[i].setState(false); 
		PointMenuItems[c].setState(true); 
		IA.setMultipleState("type",c); 
		ZC.setDefaultType(c); 
		Global.setParameter("options.type",c); 
	}
	
	public void setcolortype (int c)
	{	if (c>=ColorTypeMenuItems.length || c<0) c=0;
		for (int i=0; i<ColorTypeMenuItems.length; i++)
		ColorTypeMenuItems[i].setState(false); 
		ColorTypeMenuItems[c].setState(true); 
		IA.setMultipleState("thickness",c); 
		ZC.setDefaultColorType(c); 
		Global.setParameter("options.colortype",c); 
	}
	
	public void showcolor (int c)
	{	ZC.setShowColor(c); 
		for (int i=0; i<ShowColorMenuItems.length; i++)
		{	ShowColorMenuItems[i].setState(c==i); 
		}
		IA.setMultipleState("showcolor",c); 
	}
	
	public void showStatus (String s)
	{	if (Status.getText().equals(s)) return; 
		Status.setText(s); 
	}
	
	public void keyReleased (KeyEvent e) {}
	
	public void keyPressed (KeyEvent e)
	{	int code=e.getKeyCode(); 
		int i; 
		boolean Shift=e.isShiftDown(),Control=e.isControlDown(),Alt=e.isAltDown(); 
		if (Control && Alt)
		{	switch (code)
			{	case '1' : ZC.callCDItem("Description",true); break;
				case '2' : ZC.callCDItem("Size",true); break;
				case '3' : ZC.callCDItem("Formula",true); break;
				case '4' : ZC.callCDAction("Hide"); break;
				case '5' : ZC.callCDAction("SuperHide"); break;
				case '7' : ZC.callCDAction("Copy"); break;
				case '9' : ZC.callCDToggleItem("Visible"); break;
				case '0' : ZC.callCDToggleItem("Sort"); break;
			}
		}
		else if (Control)
		{	switch (e.getKeyCode())
			{	case KeyEvent.VK_O : load(); return; 
				case KeyEvent.VK_R : loadRun(); return; 
				case KeyEvent.VK_X : editRun(OldRun); return; 
				case KeyEvent.VK_J : loadJob(); return; 
				case KeyEvent.VK_K : saveJob(); return; 
				case KeyEvent.VK_S : save(); return; 
				case KeyEvent.VK_A : saveas(); return; 
				case KeyEvent.VK_E : exportHTML(); return; 
				case KeyEvent.VK_Z : ZC.undo(); ZC.repaint(); return; 
				case KeyEvent.VK_N : doAction("menu.file.new"); return; 
				case KeyEvent.VK_I :
					itemAction("menu.special.restricticons",!RestrictIcons.getState()); 
				return; 
			}
			for (i=0; i<PointKeys.length; i++)
			{	if (PointKeys[i]==code)
				{	settype(i); 
					return; 
				}
			}
			for (i=0; i<ColorKeys.length; i++)
			{	if (ColorKeys[i]==code)
				{	setcolor(i); 
					return; 
				}
			}
		}
		else if (Alt)
		{	for (i=0; i<ColorKeys.length; i++)
			{	if (ColorKeys[i]==code)
				{	showcolor(i); 
					return; 
				}
			}
			for (i=0; i<ColorTypeKeys.length; i++)
			{	if (ColorTypeKeys[i]==code)
				{	setcolortype(i); 
					return; 
				}
			}
		}
		else
		{	switch (code)
			{	case KeyEvent.VK_DELETE :
				settool(NDelete); break; 
				case KeyEvent.VK_ESCAPE :
					if (ZC.getCurrentTool() instanceof DrawerTool) ZC.clearDrawings(); 
				else reset(); 
				break; 
				case KeyEvent.VK_SPACE :
					ZC.returnPressed(); break; 
				case KeyEvent.VK_ENTER :
					if (Shift) track(); 
				break; 
			}
		}
		if (!e.isActionKey()) return; 
		switch (code)
		{	case KeyEvent.VK_F1 :
				if (Shift || Control)
				{	if (!Global.getParameter("restricted",false))
					{	TestJob.setState(!TestJob.getState()); 
						itemAction("menu.special.testjob",TestJob.getState()); 
					}
				}
			else info(); 
			break; 
			case KeyEvent.VK_F3 :
				if (Shift || Control)
				{	itemAction("menu.options.printscalepreview",
						!PrintScalePreview.getState());
				} 
			break; 
			case KeyEvent.VK_F4 :
				if (Alt) doclose(); 
			break; 
			case KeyEvent.VK_F5 :
				if (Shift || Control)
				{	if (enabled("macro")) definemacro(); 
				}
				else
				{	if (enabled("runmacro")) runMacro(false); 
				}
			break; 
			case KeyEvent.VK_F6 :
				if (Shift || Control)
				{	setShowNames(!ShowNames.getState()); 
				}
			break; 
			case KeyEvent.VK_F7 :
				if (Shift || Control)
				{	setShowValues(!ShowValues.getState());
				}	
			break; 
			case KeyEvent.VK_F8 :
				if (Shift || Control)
				{	setLongNames(!LongNames.getState());
				}
				else showjobcomment(); 
			break; 
			case KeyEvent.VK_F9 :
				/*if (Shift && Control && Alt)
				{	ZC.getConstruction().dovalidateDebug();
				}
				else */ if (Shift || Control)
				{	setPartial(!Partial.getState()); 
				}
				else
				{	ShowHidden.setState(!ShowHidden.getState()); 
					IA.setState("hidden",ShowHidden.getState()); 
					ZC.setShowHidden(ShowHidden.getState()); 
				}
			break; 
			case KeyEvent.VK_F10 :
				if (Shift || Control)
				{	setPartialLines(!PartialLines.getState()); 
				}
			else showcomment(); 
			break; 
			case KeyEvent.VK_F11 :
				if (Shift || Control)
				{	setVectors(!Vectors.getState()); 
				}
			else 
				{	showConstructionDisplay(
						!Global.getParameter("options.showdisplay",true));
				}
			break; 
			case KeyEvent.VK_F12 :
				if (Shift || Control)
				{	setObtuse(!Obtuse.getState()); 
				}
			else toggleGrid(); 
			break; 
			case KeyEvent.VK_LEFT :
				if (Shift && ZC.getCurrentTool() instanceof ObjectTracker)
					((ObjectTracker)ZC.getCurrentTool()).decreaseOmit(); 
				else if (Shift && ZC.getCurrentTool() instanceof BreakpointAnimator)
					((BreakpointAnimator)ZC.getCurrentTool()).decreaseSpeed(); 
				else if (Shift && ZC.getCurrentTool() instanceof AnimatorTool)
					((AnimatorTool)ZC.getCurrentTool()).decreaseSpeed(); 
				else ZC.shift(-0.1,0); 
			break; 
			case KeyEvent.VK_RIGHT :
				if (Shift && ZC.getCurrentTool() instanceof ObjectTracker)
					((ObjectTracker)ZC.getCurrentTool()).increaseOmit(); 
				else if (Shift && ZC.getCurrentTool() instanceof BreakpointAnimator)
					((BreakpointAnimator)ZC.getCurrentTool()).increaseSpeed(); 
				else if (Shift && ZC.getCurrentTool() instanceof AnimatorTool)
					((AnimatorTool)ZC.getCurrentTool()).increaseSpeed(); 
				else ZC.shift(0.1,0); 
			break; 
			case KeyEvent.VK_UP :
				ZC.shift(0,0.1); break; 
			case KeyEvent.VK_DOWN :
				ZC.shift(0,-0.1); break; 
		}
	}
	public void keyTyped (KeyEvent e)
	{	char c=e.getKeyChar(); 
		int i; 
		if (e.isControlDown() || e.isAltDown()) return; 
		for (i=0; i<ObjectKeys.length; i++)
		{	if (c==ObjectKeys[i])
			{	if (enabled(ObjectStrings[i]))
				{	if (i==NMacro) runMacro(false); 
					else settool(i); 
				}
				return; 
			}
		}
		switch (c)
		{	case KeyEvent.VK_BACK_SPACE :
			ZC.back(); 
			ZC.repaint(); 
			return; 
			case '+' :
				if (e.isControlDown() &&
				ZC.getCurrentTool() instanceof ObjectTracker)
				((ObjectTracker)ZC.getCurrentTool()).increaseOmit(); 
			else ZC.magnify(1/Math.sqrt(Math.sqrt(2))); 
			break; 
			case '-' :
				ZC.magnify(Math.sqrt(Math.sqrt(2))); 
			break; 
		}
	}
	
	public void save ()
	{	testjob(false); 
		if (!haveFile()) saveas(); 
		else dosave(Filename,true,Global.getParameter("save.includemacros",false),false,
			ZC.getMacros()); 
	}
	
	public static boolean isCompressed (String filename)
	{	return FileName.extension(filename).endsWith("z"); 
	}
	
	public boolean dosave (String Filename,
		boolean construction, boolean macros, boolean protectedmacros, Vector v)
	{	if (Global.getParameter("options.backups",true) && exists(Filename))
		{	File F=new File(Filename);
			File Back=new File(Filename+".bak");
			try
			{	if (Back.exists())
				{	Back.delete();
				}
				F.renameTo(Back);
			}
			catch (Exception e)
			{	Warning w=new Warning(this,Zirkel.name("warning.save.backup"),
					FileName.chop(32,e.toString(),64),
				Zirkel.name("warning"),true); 
				w.center(this); 
				w.setVisible(true); 
				return false; 
			}
		}
		try
		{	OutputStream o=new FileOutputStream(Filename); 
			if (isCompressed(Filename))
				o=new GZIPOutputStream(o,10000); 
			ZC.getConstruction().BackgroundFile=Background;
			ZC.getConstruction().ResizeBackground=
				Global.getParameter("background.usesize",false);
			if (RestrictIcons.getState())
			{	String icons=Global.getParameter("restrictedicons",DefaultIcons); 
				ZC.save(o,construction,macros,protectedmacros,v,icons); 
			}
			else ZC.save(o,construction,macros,protectedmacros,v,""); 
			o.close(); 
			if (construction)
				setTitle(Zirkel.name("program.name")+" : "+FileName.chop(Filename)); 
		}
		catch (Exception e)
		{	Warning w=new Warning(this,Zirkel.name("warning.save"),
				FileName.chop(32,e.toString(),64),
			Zirkel.name("warning"),true); 
			w.center(this); 
			w.setVisible(true); 
			return false; 
		}
		return true; 
	}
	
	public boolean saveas (String pattern, String ext)
	{	testjob(false); 
		FileSave.center(this); 
		if (haveFile())
		{	FileSave.setDirectory(FileName.path(Filename)); 
			FileSave.setFilePath(FileName.filename(Filename)); 
		}
		FileSave.setPattern(
			Global.getParameter("pattern",pattern)); 
		FileSave.update(!haveFile()); 
		FileSave.setVisible(true); 
		if (FileSave.isAborted()) return false; 
		String filename=FileSave.getFilePath(); 
		if (FileName.extension(filename).equals(""))
			filename=filename+ext; 
		if (Global.getParameter("options.filedialog",true) && exists(filename))
		{	Question d=new Question(this,
			FileName.filename(filename)+" : "+
				Zirkel.name("file.exists.overwrite"),
				Zirkel.name("file.exists.title"),
				this,false,true); 
			d.center(this); 
			d.setVisible(true); 
			if (!d.yes()) return false; 
		}
		Filename=filename; 
		return dosave(Filename,true,Global.getParameter("save.includemacros",false),false,
			ZC.getMacros()); 
	}
	public boolean saveas ()
	{	return saveas("*.zir *.zirz *.job *.jobz",
		Global.getParameter("save.compress",false)?".zirz":".zir"); 
	}
	
	public boolean exists (String filename)
	{	File f=new File(filename); 
		return f.exists(); 
	}
	
	public boolean savefile ()
	{	testjob(false); 
		if (!haveFile()) return saveas(); 
		else return dosave(Filename,true,Global.getParameter("save.includemacros",false),false,
			ZC.getMacros()); 
	}
	
	public void saveMacros ()
	{	testjob(false); 
		Vector v=ZC.chooseMacros(); 
		if (v==null || v.size()==0) return; 
		FileSave.center(this); 
		FileSave.setPattern(Global.getParameter(
			"pattern.macro","*.mcr *mcrz")); 
		FileSave.update(); 
		FileSave.setVisible(true); 
		if (FileSave.isAborted()) return; 
		String Filename=FileSave.getFilePath(); 
		if (FileName.extension(Filename).equals(""))
			Filename=Filename+
			(Global.getParameter("save.compress",false)?".mcrz":".mcr"); 
		if (Global.getParameter("options.filedialog",true) && exists(Filename))
		{	Question d=new Question(this,
			FileName.filename(Filename)+" : "+
				Zirkel.name("file.exists.overwrite"),
				Zirkel.name("file.exists.title"),
				this,false,true); 
			d.center(this); 
			d.setVisible(true); 
			if (!d.yes()) return; 
		}
		dosave(Filename,false,true,true,v); 
	}
	
	public void deleteMacros ()
	{	Vector v=ZC.chooseMacros(); 
		if (v==null || v.size()==0) return; 
		ZC.deleteMacros(v); 
	}
	
	public void renameMacro ()
	{	Macro m=ZC.chooseMacro(); 
		if (m==null) return; 
		if (ZC.MacroCurrentComment!=null) m.setComment(ZC.MacroCurrentComment);
		RenameMacroDialog d=new RenameMacroDialog(this,m); 
		d.center(this); 
		d.setVisible(true); 
		if (d.isAborted()) return; 
		if (!d.getName().equals(""))
			ZC.renameMacro(m,d.getName());
		m.setComment(d.getComment());
		updateMacroBar();
	}
	
	public void clearMacros ()
	{	if (!ZC.haveMacros()) return; 
		if (!Global.getParameter("options.sure",true) ||
			Sure.ask(this,Zirkel.name("sure.macros"))) ZC.clearMacros(); 
		updateMacroBar();
	}
	
	public void clearNonprotectedMacros ()
	{	if (!ZC.haveNonprotectedMacros()) return; 
		if (!Global.getParameter("options.sure",true) ||
			Sure.ask(this,Zirkel.name("sure.macros"))) ZC.clearNonprotectedMacros(); 
		updateMacroBar();
	}
	
	public void loadExamples ()
	{	String dir=System.getProperty("user.dir");
		if (new File(dir+System.getProperty("file.separator")+"Data").exists())
			dir=dir+System.getProperty("file.separator")+"Data";
		loadInDir(dir);
	}
	
	public void loadInDir (String dir)
	{	testjob(false); 
		if (ZC.changed())
		{	Question q=new Question(this,Zirkel.name("savequestion.qsave"),
			Zirkel.name("savequestion.title"),true); 
			q.center(this); 
			q.setVisible(true); 
			if (q.yes() && !savefile()) return; 
			if (q.isAborted()) return; 
		}
		FileLoad.setPattern(Global.getParameter(
			"pattern","*.zir *.job *.zirz *.jobz")); 
		FileLoad.center(this); 
		if (dir!=null)
		{	FileLoad.setDirectory(dir);
			FileLoad.updateFiles();
			FileLoad.updateDir();
		}
		FileLoad.update(dir==null); 
		FileLoad.setVisible(true); 
		if (FileLoad.isAborted()) return; 
		Filename=FileLoad.getFilePath(); 
		if (Global.getParameter("load.clearmacros",true))
			clearNonprotectedMacros();
		if (!new File(Filename).exists())
		{	if (new File(Filename+".zir").exists())
				Filename=Filename+".zir";
			else if (new File(Filename+".zirz").exists())
				Filename=Filename+".zirz";
		}
		reset();
		load(Filename); 
	}
	public void load ()
	{	loadInDir(null);
	}
	
	public void loadRun ()
	{	testjob(false); 
		FileLoad.setPattern(Global.getParameter(
			"pattern.run","*.run")); 
		FileLoad.center(this); 
		FileLoad.update(); 
		FileLoad.setVisible(true); 
		if (FileLoad.isAborted()) return; 
		String filename=FileLoad.getFilePath(); 
		if (Global.getParameter("load.clearmacros",true))
			clearNonprotectedMacros();
		loadRun(filename); 
		Filename=""; 
	}
	
	public void loadRun (String name)
	{	try
		{	InputStream o=new FileInputStream(name); 
			if (isCompressed(name))
				o=new GZIPInputStream(o); 
			clear(false); 
			ZC.loadRun(o); 
			o.close(); 
			setTitle(Zirkel.name("program.name")+" : "+FileName.chop(name)); 
			if (!ZC.getConstruction().getComment().equals("")) showcomment(); 
			loadsettings(); 
		}
		catch (Exception e)
		{	Warning w=new Warning(this,Zirkel.name("warning.load"),
				FileName.chop(32,e.toString(),64),
			Zirkel.name("warning"),true); 
			w.center(this); 
			w.setVisible(true); 
			ZC.endWaiting(); 
			e.printStackTrace();
		}
		settool(NMover);
		updateMacroBar();
	}
	
	String OldRun=""; 
	
	public void editRun (String oldrun)
	{	testjob(false); 
		if (oldrun.equals(""))
		{	FileLoad.setPattern(Global.getParameter("pattern.run","*.run")); 
			FileLoad.center(this); 
			FileLoad.update(); 
			FileLoad.setVisible(true); 
			if (FileLoad.isAborted()) return; 
			OldRun=FileLoad.getFilePath(); 
		}
		else OldRun=oldrun; 
		EditRunDialog d=new EditRunDialog(this,OldRun); 
		d.setVisible(true); 
	}
	
	public void load (String name)
	{	setEnabled(false); 
		new ZirkelFrameLoadThread(this,name); 
	}
	
	public void doload (String name)
	{	// System.out.println("load "+name);
		try
		{	InputStream o=new FileInputStream(name); 
			if (isCompressed(name))
				o=new GZIPInputStream(o); 
			clear(false); 
			ZC.startWaiting();
			ZC.load(o);
			ZC.endWaiting(); 
			o.close(); 
			setTitle(Zirkel.name("program.name")+" : "+FileName.chop(name)); 
			setEnabled(true);
			if (!ZC.getConstruction().getComment().equals("")
				&& !ZC.isJob())
				showcomment(); 
			Filename=name; 
			Grid.setState(ZC.showGrid()); 
			IA.setState("grid",ZC.showGrid()); 
		}
		catch (Exception e)
		{	Warning w=new Warning(this,Zirkel.name("warning.load"),
				FileName.chop(32,e.toString(),64),
			Zirkel.name("warning"),true); 
			w.center(this); 
			w.setVisible(true);
			ZC.endWaiting(); 
			//e.printStackTrace();
			return;
		}
		// System.out.println("finished loading "+name);
		String icons=ZC.getConstruction().Icons; 
		if (!icons.equals(""))
		{	// System.out.println("setting restricted icons");
			Global.setParameter("restrictedicons",icons); 
			RestrictIcons.setState(true); 
			showDefaultIcons(false);
			remakeIconBar(); 
		}
		else if (RestrictIcons.getState())
		{	// System.out.println("no restricted icons");
			RestrictIcons.setState(false); 
			showDefaultIcons(true);
			remakeIconBar(); 
		}
		// System.out.println("finished setting icons");
		if (ZC.getConstruction().BackgroundFile!=null)
		{	// System.out.println("setting background");
			String backgroundfile=ZC.getConstruction().BackgroundFile;
			String file=backgroundfile;
			if (FileName.path(backgroundfile).equals(""))
				file=FileName.path(name)+
					File.separator+backgroundfile;
			Global.setParameter("background.usesize",ZC.getConstruction().ResizeBackground);
			doloadBackground(file);
		}
		Construction C=ZC.getConstruction(); 
		if (C.TrackP!=null)
		{	// System.out.println("setting track");
			try
			{	ConstructionObject P=C.find(C.TrackP); 
				if (P==null || !((P instanceof PointObject)
					|| (P instanceof PrimitiveLineObject)))
					throw new ConstructionException("");
				PointObject PM=null;
				if (C.find(C.TrackPM)!=null)
					PM=(PointObject)C.find(C.TrackPM); 
				ConstructionObject po[]=
					new ConstructionObject[C.TrackPO.size()]; 
				for (int i=0; i<po.length; i++)
				{	ConstructionObject o=C.find(
					(String)C.TrackPO.elementAt(i)); 
					if (o==null || !((o instanceof PointObject)
						|| (o instanceof PrimitiveLineObject)))
						throw new ConstructionException(""); 
					po[i]=o; 
				}
				if (C.TrackO!=null)
				{	ConstructionObject O=C.find(C.TrackO); 
					if (P==null || (PM==null && !(O instanceof ExpressionObject)) || O==null)
						throw new ConstructionException(""); 
					settool(NObjectTracker); 
					ObjectTracker TR=new ObjectTracker(P,PM,O,ZC,C.Animate,C.Paint,po); 
					if (C.Omit>0) TR.setOmit(C.Omit); 
					ZC.setTool(TR); 
					ZC.validate(); 
					ZC.repaint(); 
				}
				else
				{	if (P==null)
					throw new ConstructionException(""); 
					settool(NTracker); 
					ZC.setTool(new Tracker(P,po)); 
					if (PM!=null) PM.setSelected(true); 
					ZC.validate(); 
					ZC.repaint(); 
				}
			}
			catch (Exception e)
			{	warning(Zirkel.name("exception.track")); 
			}
		}
		else if (C.AnimateP!=null)
		{	// System.out.println("setting animation");
			try
			{	PointObject P=(PointObject)C.find(C.AnimateP); 
				if (P==null)
					throw new ConstructionException(""); 
				Enumeration e=C.AnimateV.elements(); 
				while (e.hasMoreElements())
				{	String s=(String)e.nextElement(); 
					ConstructionObject o=C.find(s); 
					if (o==null || !(o instanceof SegmentObject
						|| o instanceof PrimitiveCircleObject
						|| o instanceof PointObject))
						throw new ConstructionException(""); 
				}
				settool(NAnimator); 
				ZC.setTool(new AnimatorTool(P,C.AnimateV,ZC,C.AnimateNegative,
							C.AnimateOriginal,C.AnimateDelay)); 
			}
			catch (Exception e)
			{	warning(Zirkel.name("exception.animate")); 
			}
		}
		else if (C.AnimateBreakpoints)
		{	// System.out.println("setting animation with brakpoints");
			BreakpointAnimator bp=new BreakpointAnimator();
			bp.setLoop(C.AnimateLoop);
			bp.setSpeed(C.AnimateTime);
			ZC.setTool(bp);
			bp.reset(ZC);
		}
		else
		{	// System.out.println("setting mover");
			settool(NMover); 
		}
		updateMacroBar();
		// System.out.println("finished loading");
	}
	
	public void loadMacros ()
	{	FileLoad.setPattern("*.mcr *.mcrz"); 
		FileLoad.center(this); 
		FileLoad.update(); 
		FileLoad.setVisible(true); 
		if (FileLoad.isAborted()) return; 
		String Filename=FileLoad.getFilePath(); 
		try
		{	InputStream o=new FileInputStream(Filename); 
			if (isCompressed(Filename))
				o=new GZIPInputStream(o); 
			ZC.load(o,false,true); 
			o.close(); 
		}
		catch (Exception e)
		{	Warning w=new Warning(this,Zirkel.name("warning.loadmacros"),
				FileName.chop(32,e.toString(),64),
			Zirkel.name("warning"),true); 
			w.center(this); 
			w.setVisible(true); 
		}
		updateMacroBar();
	}
	
	public void loadJob ()
	{	testjob(false); 
		FileLoad.setPattern("*.job *.jobz"); 
		FileLoad.center(this); 
		FileLoad.update(); 
		FileLoad.setVisible(true); 
		if (FileLoad.isAborted()) return; 
		Filename=FileLoad.getFilePath(); 
		try
		{	InputStream o=new FileInputStream(Filename); 
			if (isCompressed(Filename))
				o=new GZIPInputStream(o); 
			clear(false); 
			ZC.load(o); 
			o.close(); 
			setTitle(Zirkel.name("program.name")+" : "+FileName.chop(Filename)); 
			String icons=ZC.getConstruction().Icons; 
			if (!icons.equals(""))
			{	Global.setParameter("restrictedicons",icons); 
				RestrictIcons.setState(true); 
				showDefaultIcons(false);
				remakeIconBar(); 
			}
			else if (RestrictIcons.getState())
			{	RestrictIcons.setState(false); 
				showDefaultIcons(true);
				remakeIconBar(); 
			}
			if (ZC.isJob()) testjob(true); 
			if (!ZC.getConstruction().getComment().equals("")) showcomment(); 
		}
		catch (Exception e)
		{	Warning w=new Warning(this,Zirkel.name("warning.load"),
				FileName.chop(32,e.toString(),64),
			Zirkel.name("warning"),true); 
			w.center(this); 
			w.setVisible(true); 
		}
		updateMacroBar();
	}
	
	public void saveJob ()
	{	testjob(false); 
		if (!ZC.isJob())
		{	warning(Zirkel.name("warning.nojob")); 
			TestJob.setState(false); 
			return; 
		}
		saveas("*.job *.jobz",
			Global.getParameter("save.compress",false)?".jobz":".job"); 
	}
	
	public void showcomment ()
	{	CommentDialog d=new CommentDialog(this,
		ZC.getComment(),Zirkel.name("comment.title"),ZC.displayJob()); 
		ZC.setComment(d.getText()); 
	}
	
	public void showjobcomment ()
	{	CommentDialog d=new CommentDialog(this,
		ZC.getJobComment(),Zirkel.name("jobcomment.title"),false); 
		ZC.setJobComment(d.getText()); 
	}
	
	public void showconstruction ()
	{	new ConstructionDisplay(this,ZC); 
	}
	
	public boolean close ()
	{	if (Zirkel.IsApplet) return true; 
		if (ZC.changed())
		{	Question q=new Question(this,Zirkel.name("savequestion.qsave"),
			Zirkel.name("savequestion.title"),true); 
			q.center(this); 
			q.setVisible(true); 
			if (q.yes()) return savefile(); 
			return q.getResult()!=Question.ABORT; 
		}
		return true; 
	}
	
	public void doclose ()
	{	notePosition("zirkelframe"); 
		super.doclose(); 
	}
	
	public void windowActivated (WindowEvent e)
	{	if (ZC!=null) ZC.requestFocus();
		// It seems Linux may call this before the constructor. Strange!
	}
	
	public void toggleGrid ()
	{	ZC.toggleShowGrid(); 
		Grid.setState(ZC.showGrid()); 
		IA.setState("grid",ZC.showGrid()); 
	}
	
	/* (non-Javadoc)
	 * @see rene.zirkel.DoneListener#notifyDone()
	 * Display a message for the user.
	 */
	public void notifyDone ()
	{	repaint(); 
		try
		{	Thread.sleep(500); 
		}
		catch (Exception e) {	}
		warning(Zirkel.name("done")); 
	}
	
	/**
	 * Test a job, or stop testing a job. 
	 * Register as listener to ZC, when the job is done.
	 * 
	 * @param flag
	 */
	public void testjob (boolean flag)
	{	if (flag==ZC.displayJob()) return; // nothing to do 
		if (flag && !ZC.isJob()) // user error!
		{	warning(Zirkel.name("warning.nojob")); 
			TestJob.setState(false); 
			ZC.reloadCD();
			return; 
		}
		// else:
		ZC.displayJob(flag); // main action is in ZC. 
		if (flag) ZC.setDoneListener(this); 
		else ZC.setDoneListener(null); 
		TestJob.setState(flag); 
		ZC.reloadCD();
	}
	
	public void exportHTML ()
	{	testjob(false); 
		if (!haveFile())
		{	warning(Zirkel.name("export.savefirst")); 
			return; 
		}
		ExportDialog d=new ExportDialog(this,ZC.getConstruction(),
			RestrictIcons.getState()); 
		d.center(this); 
		if (!Background.equals("") &&
			Global.getParameter("background.usesize",false) &&
			ZC.Background.getWidth(this)==ZC.IW &&
			ZC.Background.getHeight(this)==ZC.IH)
		{	d.setDimensions(ZC.IW,ZC.IH); 
		}
		d.setVisible(true); 
		if (d.isAborted()) return; 
		doexport(d,false); 
		if (!d.getSolution().equals("") && d.saveSolution())
			doexport(d,true); 
	}
	
	public void doexport (ExportDialog d, boolean solution)
	{	HTMLSave.center(this); 
		HTMLSave.setDirectory(FileName.path(Filename)); 
		if (solution)
		{	HTMLSave.setFilePath(d.getSolution()); 
		}
		else
		{	HTMLSave.setFilePath(FileName.purefilename(Filename)+".html"); 
		}
		HTMLSave.update(false); 
		HTMLSave.setVisible(true); 
		if (HTMLSave.isAborted()) return; 
		String filename=HTMLSave.getFilePath();
		if (FileName.extension(filename).equals(""))
			filename=filename+".html"; 
		if (Global.getParameter("options.filedialog",true) &&
			exists(filename))
		{	Question q=new Question(this,
			FileName.filename(filename)+" : "+
				Zirkel.name("file.exists.overwrite"),
				Zirkel.name("file.exists.title"),
				this,false,true); 
			q.center(this); 
			q.setVisible(true); 
			if (!q.yes()) return; 
		}
		boolean utf=Global.getParameter("options.utf",true); 
		try
		{	PrintWriter out; 
			if (utf)
			{	out=new PrintWriter(
				new OutputStreamWriter(
					new FileOutputStream(filename),
					"UTF8")); 
			}
			else
			{	out=new PrintWriter(
				new OutputStreamWriter(
					new FileOutputStream(filename))); 
			}
			out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2//EN\">");
				out.println("<HTML>"); 
			out.println("<HEAD>"); 
			if (utf)
			{	out.println("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; CHARSET=utf-8\">"); 
			}
			else
			{	out.println("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html\">"); 
			}
			out.println("<META NAME=\"GENERATOR\" Content=\"Z.u.L. (C.a.R.)\">"); 
			if (solution)
				out.println("<TITLE>"+d.getPageTitle()+
				", "+Zirkel.name("export.soltext","Solution")+"</TITLE>"); 
			else
				out.println("<TITLE>"+d.getPageTitle()+"</TITLE>"); 
			if (!d.getStyleSheet().equals(""))
				out.println("<LINK REL=\"stylesheet\" TYPE=\"text/css\""+
				" HREF=\""+d.getStyleSheet()+"\">"); 
			out.println("</HEAD>"); 
			if (d.useForBackground())
				out.println("<BODY BGCOLOR="+d.getHexColor()+">"); 
			else
				out.println("<BODY>"); 
			if (solution)
				out.println("<H1>"+d.getPageTitle()+
				", "+Zirkel.name("export.soltext","Solution")+"</H1>"); 
			else
				out.println("<H1>"+d.getPageTitle()+"</H1>"); 
			out.println("<CENTER>"); 
			out.println("<P>"); 
			String jar=d.getJar(); 
			String jarname=FileName.filename(jar); 
			String jardir=FileName.path(jar); 
			if (jardir.equals(""))
				out.println("<APPLET ARCHIVE=\""+jar+"\" "+
				"CODE=\"rene.zirkel.ZirkelApplet.class\" "+
				"WIDTH=\""+d.getWidth()+"\" HEIGHT=\""+
				d.getHeight()+"\" ALIGN=\"CENTER\">");
				else
				out.println("<APPLET "+
				"CODEBASE=\""+jardir+"\" "+
				"ARCHIVE=\""+jarname+"\" "+
				"CODE=\"rene.zirkel.ZirkelApplet.class\" "+
				"WIDTH=\""+d.getWidth()+"\" HEIGHT=\""+
				d.getHeight()+"\" ALIGN=\"CENTER\">");
			if (!d.getFile().equals(""))
			{	if (d.isJob() && !solution)
				out.println("<PARAM NAME=\"job\" VALUE=\""+d.getFile()+"\">"); 
				else
					out.println("<PARAM NAME=\"file\" VALUE=\""+d.getFile()+"\">"); 
			}
			if (d.useForApplet())
				out.println("<PARAM NAME=\"color\" VALUE=\""+
				d.getDezimalColor()+"\">");
			if (d.saveBackground() && Global.haveParameter("colorbackground"))
				out.println("<PARAM NAME=\"colorbackground\" VALUE=\""+
				Global.getParameter("colorbackground","")+"\">");
			if (d.saveColors())
			{	if (Global.haveParameter("colorselect"))
					out.println("<PARAM NAME=\"colorselect\" VALUE=\""+
					Global.getParameter("colorselect","")+"\">");
					if (Global.haveParameter("colortarget"))
					out.println("<PARAM NAME=\"colortarget\" VALUE=\""+
					Global.getParameter("colortarget","")+"\">");
					for (int i=0; i<Colors.length; i++)
				{	if (Global.haveParameter("color"+i))
					out.println("<PARAM NAME=\"color"+i+"\" VALUE=\""+
						Global.getParameter("color"+i,"")+"\">");
				}
			}
			/*// now saved in the construction file
			if (Global.getParameter("grid.fine",false))
			{	out.println("<PARAM NAME=\"grid\" VALUE=\"coordinates\">");
			}
			*/
			if (Global.getParameter("grid.leftsnap",false))
			{	out.println("<PARAM NAME=\"snap\" VALUE=\"left\">");
			}
			String style=d.getStyle(); 
			if (solution) style="3D"; 
			out.println("<PARAM NAME=\"style\" VALUE=\""+style+"\">"); 
			if (!style.equals("plain") && !style.equals("3D"))
			{	String s=d.getIcons(); 
				out.println("<PARAM NAME=\"tools\" VALUE=\""+s+"\">"); 
				s=d.getTools(); 
				out.println("<PARAM NAME=\"options\" VALUE=\""+s+"\">"); 
			}
			String sol=d.getSolution(); 
			if (!sol.equals("") && d.jumpSolution() && !solution)
				out.println("<PARAM NAME=\"solution\" VALUE=\""+sol+"\">"); 
			if (ZC.showHidden())
				out.println("<PARAM NAME=\"showhidden\" VALUE=\"true\">"); 
			if (Global.getParameter("options.movename",false))
				out.println("<PARAM NAME=\"movename\" VALUE=\"true\">"); 
			if (Global.getParameter("options.movefixname",true))
				out.println("<PARAM NAME=\"movefixname\" VALUE=\"true\">"); 
			if (Global.getParameter("options.smallicons",false))
				out.println("<PARAM NAME=\"smallicons\" VALUE=\"true\">"); 
			if (Global.getParameter("options.oldicons",false))
				out.println("<PARAM NAME=\"oldicons\" VALUE=\"true\">"); 
			if (!d.allowZoom())
				out.println("<PARAM NAME=\"nomousezoom\" VALUE=\"true\">"); 				
			if (!d.allowPopup())
				out.println("<PARAM NAME=\"nopopupmenu\" VALUE=\"true\">"); 				
			if (d.restrictedMove())
				out.println("<PARAM NAME=\"restrictedmove\" VALUE=\"true\">"); 				
			String s=""; 
			if (Global.getParameter("font.bold",false)) s=s+"bold"; 
			if (Global.getParameter("font.large",false)) s=s+" large"; 
			if (!s.equals("")) out.println("<PARAM NAME=\"font\" VALUE=\""+s+"\">"); 
			if (d.saveDigits())
			{	out.println("<PARAM NAME=\"editdigits\" VALUE=\""+
				getDigits(ZirkelCanvas.EditFactor)+"\">");
					out.println("<PARAM NAME=\"displaydigits\" VALUE=\""+
					getDigits(ZirkelCanvas.LengthsFactor)+"\">");
					out.println("<PARAM NAME=\"angledigits\" VALUE=\""+
					getDigits(ZirkelCanvas.AnglesFactor)+"\">");
			}
			if (Global.getParameter("minpointsize",3)!=3)
				out.println("<PARAM NAME=\"minpointsize\" VALUE=\""+
				Global.getParameter("minpointsize",3)+"\">");
			if (Global.getParameter("minlinesize",1.0)!=1.0)
				out.println("<PARAM NAME=\"minlinewidth\" VALUE=\""+
				Global.getParameter("minlinesize",1.0)+"\">");
			if (Global.getParameter("minfontsize",12)!=12)
				out.println("<PARAM NAME=\"minfontsize\" VALUE=\""+
				Global.getParameter("minfontsize",12)+"\">");
			if (Global.getParameter("options.germanpoints",true))
				out.println("<PARAM NAME=\"germanpoints\" VALUE=\"true\">");
			if (!Background.equals(""))
				out.println("<PARAM NAME=\"background\" VALUE=\""+Background+"\">"); 
			out.println("</APPLET>"); 
			out.println("</P>"); 
			out.println("</CENTER>"); 
			
			s=ZC.getComment(); 
			if (d.isJob() && !solution) s=ZC.getJobComment(); 
			if (!s.equals("") && d.saveComment())
			{	XmlWriter xml=new XmlWriter(out); 
				xml.printParagraphs(s,60); 
			}
			
			if (!sol.equals("") && !solution && d.linkSolution())
			{	out.println("<P>"); 
				out.println("<A href=\""+sol+"\">"+
					Zirkel.name("export.soltext","Solution")+"</A>"); 
				out.println("</P>"); 
			}
			
			out.println("<HR>"); 
			out.println("<P>"); 
			out.println(Zirkel.name("export.signature")); 
			out.println("</P>"); 
			
			out.println("</BODY>"); 
			out.println("</HTML>"); 
			
			out.close(); 
		}
		catch (Exception e)
		{	Warning w=new Warning(this,Zirkel.name("warning.save"),
				FileName.chop(32,e.toString(),64),
			Zirkel.name("warning"),true); 
			w.center(this); 
			w.setVisible(true); 
			//e.printStackTrace();
		}
	}
	
	public void exportTemplateHTML ()
	{	testjob(false); 
		if (!haveFile())
		{	warning(Zirkel.name("export.savefirst")); 
			return; 
		}		
		TemplateLoad.center(this); 
		TemplateLoad.update(); 
		TemplateLoad.setVisible(true); 
		if (TemplateLoad.isAborted()) return; 
		String Template=TemplateLoad.getFilePath();
		ExportTemplateDialog d=new ExportTemplateDialog(this,ZC.getConstruction(),
			RestrictIcons.getState()); 
		d.center(this); 
		d.setVisible(true); 
		if (d.isAborted()) return; 
		doexporttemplate(d,Template); 
	}
	
	public void doexporttemplate (ExportTemplateDialog d, String template)
	{	HTMLSave.center(this); 
		HTMLSave.setDirectory(FileName.path(Filename)); 
		HTMLSave.setFilePath(FileName.purefilename(Filename)+".html"); 
		HTMLSave.update(false); 
		HTMLSave.setVisible(true); 
		if (HTMLSave.isAborted()) return; 
		String filename=HTMLSave.getFilePath();
		if (FileName.extension(filename).equals(""))
			filename=filename+".html"; 
		if (Global.getParameter("options.filedialog",true) &&
			exists(filename))
		{	Question q=new Question(this,
			FileName.filename(filename)+" : "+
				Zirkel.name("file.exists.overwrite"),
				Zirkel.name("file.exists.title"),
				this,false,true); 
			q.center(this); 
			q.setVisible(true); 
			if (!q.yes()) return; 
		}
		boolean utf=Global.getParameter("options.utf",true); 
		try
		{	PrintWriter out; 			
			if (utf)
			{	out=new PrintWriter(
				new OutputStreamWriter(
					new FileOutputStream(filename),
					"UTF8")); 
			}
			else
			{	out=new PrintWriter(
				new OutputStreamWriter(
					new FileOutputStream(filename))); 
			}
			BufferedReader in;
			if (utf)
			{	in=
				new BufferedReader(
				new InputStreamReader(new FileInputStream(template),"UTF8"));
			}
			else
			{	in=
				new BufferedReader(
				new InputStreamReader(new FileInputStream(template)));
			}
			while (true)
			{	String s=readTemplateLine(in);
				if (s==null) break;
				if (s.startsWith("#"))
				{	if (s.equals("#title"))
					{	printCheck(out,d.getPageTitle());
					}
					else if (s.equals("#file"))
					{	out.println("<PARAM NAME=\"file\" VALUE=\""
							+d.getFile()+"\">"); 
					}
					else if (s.startsWith("#comment"))				
					{	String com=ZC.getComment(); 
						if (ZC.isJob()) com=ZC.getJobComment(); 
						if (!s.equals("#comment"))
						{	try
							{	int n=Integer.parseInt(s.substring(8));
								StringTokenizer t=new StringTokenizer(com,"~");
								int i=1;
								com="";
								while (t.hasMoreTokens())
								{	String h=t.nextToken();
									if (i==n)
									{	com=h; break;
									}
									i++;
								}
							}
							catch (Exception e) { System.out.println(e); }
						}
						if (!com.equals(""))
						{	printParagraphs(out,com,60); 
						}
					}
					else if (s.startsWith("#text"))				
					{	String com=ZC.getComment(); 
						if (ZC.isJob()) com=ZC.getJobComment(); 
						if (!s.equals("#text"))
						{	try
							{	int n=Integer.parseInt(s.substring(5));
								StringTokenizer t=new StringTokenizer(com,"~");
								int i=1;
								while (t.hasMoreTokens())
								{	String h=t.nextToken();
									if (i==n)
									{	com=h; break;
									}
									i++;
								}
							}
							catch (Exception e) { System.out.println(e); }
							if (com.startsWith("\n"))
								com=com.substring(1);
							if (com.endsWith("\n") && com.length()>1)
								com=com.substring(0,com.length()-1);
						}
						if (!com.equals(""))
						{	printCheck(out,com);
						}
					}
					else if (s.equals("#parameter"))
					{	if (!d.getFile().equals(""))
						{	if (ZC.isJob() && d.isJob())
								out.println("<PARAM NAME=\"job\" VALUE=\""
									+d.getFile()+"\">"); 
							else
								out.println("<PARAM NAME=\"file\" VALUE=\""
									+d.getFile()+"\">"); 
						}
						String style=d.getStyle(); 
						out.println("<PARAM NAME=\"style\" VALUE=\""+style+"\">"); 
						if (!style.equals("plain") && !style.equals("3D"))
						{	String ss=d.getIcons(); 
							out.println("<PARAM NAME=\"tools\" VALUE=\""+ss+"\">"); 
							ss=d.getTools(); 
							out.println("<PARAM NAME=\"options\" VALUE=\""+ss+"\">"); 
						}
						if (ZC.showHidden())
							out.println("<PARAM NAME=\"showhidden\" VALUE=\"true\">"); 
						if (Global.getParameter("options.movename",false))
							out.println("<PARAM NAME=\"movename\" VALUE=\"true\">"); 
						if (Global.getParameter("options.movefixname",true))
							out.println("<PARAM NAME=\"movefixname\" VALUE=\"true\">"); 
						if (Global.getParameter("options.smallicons",false))
							out.println("<PARAM NAME=\"smallicons\" VALUE=\"true\">"); 
						if (Global.getParameter("options.oldicons",false))
							out.println("<PARAM NAME=\"oldicons\" VALUE=\"true\">"); 
						if (Global.getParameter("options.germanpoints",true))
							out.println("<PARAM NAME=\"germanpoints\" VALUE=\"true\">");
						/*// now saved in the construction file
						if (Global.getParameter("grid.fine",false))
							out.println("<PARAM NAME=\"grid\" VALUE=\"coordinates\">");
						*/
						if (Global.getParameter("grid.leftsnap",false))
							out.println("<PARAM NAME=\"snap\" VALUE=\"left\">");
					}
					else if (s.equals("#color"))
					{	if (Global.haveParameter("colorselect"))
							out.println("<PARAM NAME=\"colorselect\" VALUE=\""+
								Global.getParameter("colorselect","")+"\">");
						if (Global.haveParameter("colortarget"))
							out.println("<PARAM NAME=\"colortarget\" VALUE=\""+
								Global.getParameter("colortarget","")+"\">");
						for (int i=0; i<Colors.length; i++)
						{	if (Global.haveParameter("color"+i))
								out.println("<PARAM NAME=\"color"+i+"\" VALUE=\""+
									Global.getParameter("color"+i,"")+"\">");
						}	
					}
					else if (s.equals("#font"))
					{	String ss=""; 
						if (Global.getParameter("font.bold",false)) ss=ss+"bold"; 
						if (Global.getParameter("font.large",false)) ss=ss+" large"; 
						if (!ss.equals("")) 
							out.println("<PARAM NAME=\"font\" VALUE=\""+ss+"\">"); 
						out.println("<PARAM NAME=\"editdigits\" VALUE=\""+
							getDigits(ZirkelCanvas.EditFactor)+"\">");
						out.println("<PARAM NAME=\"displaydigits\" VALUE=\""+
							getDigits(ZirkelCanvas.LengthsFactor)+"\">");
						out.println("<PARAM NAME=\"angledigits\" VALUE=\""+
							getDigits(ZirkelCanvas.AnglesFactor)+"\">");
						if (Global.getParameter("minpointsize",3)!=3)
							out.println("<PARAM NAME=\"minpointsize\" VALUE=\""+
								Global.getParameter("minpointsize",3)+"\">");
						if (Global.getParameter("minfontsize",12)!=12)
							out.println("<PARAM NAME=\"minfontsize\" VALUE=\""+
								Global.getParameter("minfontsize",12)+"\">");
						if (Global.getParameter("minlinesize",1.0)!=1.0)
							out.println("<PARAM NAME=\"minlinewidth\" VALUE=\""+
								Global.getParameter("minlinesize",1.0)+"\">");
						if (!Background.equals(""))
							out.println("<PARAM NAME=\"background\" VALUE=\""
								+Background+"\">"); 
					}
					else if (s.equals("#codebase"))
					{	String cb=d.getCodebase();
						if (!cb.equals(""))
							out.print(" CODEBASE=\""+cb+"\" ");
					}
					}
					else
					{	printCheck(out,s);
					}
			}
			in.close();
			out.close(); 
		}
		catch (Exception e)
		{	Warning w=new Warning(this,Zirkel.name("warning.template"),
				FileName.chop(32,e.toString(),64),
			Zirkel.name("warning"),true); 
			w.center(this); 
			w.setVisible(true); 
			e.printStackTrace();
		}
		
	}

	MyVector TB=new MyVector();
	int TBN=0;

	public void printCheck (PrintWriter out, String s)
	{	if (TBN==0) out.println(s);
		else out.print(s);
	}

	public String readTemplateLine (BufferedReader in)
		throws IOException
	{	if (TB.size()>0)
			// Still strings in the buffer
		{	String h=(String)TB.elementAt(TBN);
			TBN++;
			if (TBN>=TB.size())
			{	TBN=0; TB.removeAllElements();
			}
			return h;
		}
		String h=in.readLine();
		if (h==null) return h;	
		if (h.indexOf('#')<0) return h;	
		bufferTemplate(h);
		if (TB.size()>0) return readTemplateLine(in);
		else return h;
	}
	
	public void bufferTemplate (String s)
	{	int n=s.indexOf('#');
		if (n<0)
		{	TB.addElement(s);
			return;
		}
		String h=s.substring(n);
		if (h.startsWith("#title")) bufferTemplate(s,n,h,"#title");
		else if (h.startsWith("#parameter")) bufferTemplate(s,n,h,"#parameter");
		else if (h.startsWith("#color")) bufferTemplate(s,n,h,"#color");
		else if (h.startsWith("#font")) bufferTemplate(s,n,h,"#font");
		else if (h.startsWith("#codebase")) bufferTemplate(s,n,h,"#codebase");
		else if (h.startsWith("#comment"))
		{	for (int i=0; i<10; i++)
			{	String t="#comment"+i;
				if (h.startsWith(t))
				{	bufferTemplate(s,n,h,t); return;
				}
			}
			bufferTemplate(s,n,h,"#comment");
		}
		else if (h.startsWith("#text"))
		{	for (int i=0; i<10; i++)
			{	String t="#text"+i;
				if (h.startsWith(t))
				{	bufferTemplate(s,n,h,t); return;
				}
			}
			bufferTemplate(s,n,h,"#text");
		}
	}
	
	public void bufferTemplate (String s, int n, String h, String ph)
	{	if (n>0) TB.addElement(s.substring(0,n));
		TB.addElement(ph);
		h=h.substring(ph.length());
		if (!h.equals("")) bufferTemplate(h);
	}

	public void printParagraphs (PrintWriter out, String s, int linelength)
	{	StringParser p=new StringParser(s);
		Vector v=p.wrapwords(linelength);
		for (int i=0; i<v.size(); i++)
		{	out.println("<P>");
			s=(String)v.elementAt(i);
			StringParser q=new StringParser(s);
			Vector w=q.wraplines(linelength);
			for (int j=0; j<w.size(); j++)
			{	if (j>0) out.println();
				s=(String)w.elementAt(j);
				out.print(s);
			}
			out.println("</P>");
		}
	}
	
	public int getDigits (double x)
	{	return (int)(Math.log(x)/Math.log(10)+0.5); 
	}
	
	public void setPartial (boolean flag)
	{	IA.setState("partial",flag); 
		Partial.setState(flag); 
		Global.setParameter("options.partial",flag); 
		ZC.setPartial(flag); 
	}
	
	public void setRestricted (boolean flag)
	{	Restricted.setState(flag); 
		Global.setParameter("options.restricted",flag); 
		ZC.setRestricted(flag); 
	}
	
	public void setPartialLines (boolean flag)
	{	IA.setState("plines",flag); 
		PartialLines.setState(flag); 
		Global.setParameter("options.plines",flag); 
		ZC.setPartialLines(flag); 
	}
	
	public void setVectors (boolean flag)
	{	IA.setState("arrow",flag); 
		Vectors.setState(flag); 
		Global.setParameter("options.arrow",flag); 
		ZC.setVectors(flag); 
	}
	
	public void setLongNames (boolean flag)
	{	LongNames.setState(flag); 
		IA.setState("longnames",flag); 
		Global.setParameter("options.longnames",flag); 
		ZC.setLongNames(flag); 
	}
	
	public void setBoldFont (boolean flag)
	{	BoldFont.setState(flag); 
		IA.setState("bold",flag); 
		Global.setParameter("options.boldfont",flag); 
		ZC.setBoldFont(flag); 
	}
	
	public void setLargeFont (boolean flag)
	{	LargeFont.setState(flag); 
		IA.setState("large",flag); 
		Global.setParameter("options.largefont",flag); 
		ZC.setLargeFont(flag); 
	}
	
	public void setObtuse (boolean flag)
	{	Obtuse.setState(flag); 
		IA.setState("obtuse",flag); 
		Global.setParameter("options.obtuse",flag); 
		ZC.setObtuse(flag); 
	}
	
	public void setSolid (boolean flag)
	{	Solid.setState(flag); 
		IA.setState("solid",flag); 
		Global.setParameter("options.solid",flag); 
		ZC.setSolid(flag); 
	}
	
	public void setShowNames (boolean flag)
	{	ShowNames.setState(flag); 
		IA.setState("showname",flag); 
		Global.setParameter("options.shownames",flag);
		ZC.setShowNames(flag); 
	}
	
	
	public void setShowValues (boolean flag)
	{	ShowValues.setState(flag); 
		IA.setState("showvalue",flag); 
		Global.setParameter("options.showvalue",flag); 
		ZC.setShowValues(flag); 
	}
	
	void definemacro ()
	{	ZC.defineMacro(); 
		updateMacroBar();
		settool(NParameters); 
		ZC.getOC().reset(ZC); 
	}
	
	String OldMacro=null; 
	
	public void runMacro (boolean shift)
	{	Macro m=ZC.chooseMacro(OldMacro); 
		if (!shift || m==null) m=ZC.chooseMacro(); 
		if (m==null)
		{	settool(CurrentTool); return; 
		}
		runMacro(m); 
	}
	
	public void runMacro (Macro m)
	{	((MacroRunner)ObjectConstructors[NMacro]).setMacro(m,ZC); 
		settool(NMacro); 
		if (IM!=null) IM.select(m);
		IB.setMultipleState("macro",0); 
		OldMacro=m.getName(); 
	}
	
	public void replayChosen ()
	{
	}
	
	public void setDigits ()
	{	new DigitsDialog(this); 
		ZC.updateDigits(); 
		ZC.repaint(); 
	}
	
	public void setLanguage ()
	{	GetParameter.InputLength=20; 
		GetParameter g=new GetParameter(this,
			Zirkel.name("language.title"),
			Zirkel.name("language.prompt"),
			Zirkel.name("ok"),"language"); 
		g.set("default"); 
		g.center(this); 
		g.setVisible(true); 
		if (!g.aborted())
			Global.setParameter("language",g.getResult()); 
	}
	
	public void print ()
	{	Toolkit toolkit=getToolkit(); 
		Properties printrefs=new Properties(); 
		PrintJob job=toolkit.getPrintJob(this,
			Zirkel.name("program.name")+" "+FileName.filename(Filename),
			printrefs); 
		if (job!=null)
		{	Graphics g=job.getGraphics(); 
			Dimension dim=job.getPageDimension(); 
			double res=job.getPageResolution()/2.54; 
			int w=dim.width,h=dim.height; 
			int d=w/10; 
			w-=2*d; h-=2*d; 
			double format=(double)ZC.height()/ZC.width(); 
			int W=w; 
			int H=(int)(W*format); 
			if (H>h)
			{	W=(int)(h/format); 
				H=h; 
			}
			double xw=ZC.maxX()-ZC.minX(); 
			int dd=0; 
			if (Global.getParameter("print.isoscale",false) && res>0)
			{	if (W/res<=xw)
				{	warning(Zirkel.name("exception.printsize")); 
				}
				double scale=xw*res/W; 
				W=(int)(W*scale); 
				H=(int)(H*scale); 
				dd=(w-W)/2; 
			}
			g.setClip(d+dd,d,W,H); 
			g.translate(d+dd,d); 
			Font font=g.getFont(); 
			Font f; 
			if (font!=null)
				f=new Font(font.getName(),
				Global.getParameter("font.bold",false)?Font.BOLD:Font.PLAIN,
				font.getSize()*H/ZC.height()*3/
				(Global.getParameter("font.large",false)?2:3)); 
			else
				f=new Font("Courier",
				Global.getParameter("font.bold",false)?Font.BOLD:Font.PLAIN,
				10*H/ZC.height()*3/
				(Global.getParameter("font.large",false)?2:3)); 
			g.setFont(f); 
			ZC.print(g,W,H); 
			job.end(); 
			job.finalize(); 
		}
		else
		{	warning(Zirkel.name("warning.print")); 
		}
	}
	
	public void savePNG ()
	{	ExportScaler d=new ExportScaler(this,true);
		if (d.isAborted()) return;
		if (Global.getParameter("printscale.latex",false))
		{	LatexSettingsDialog lsd=new LatexSettingsDialog(this);
			lsd.setVisible(true);
			if (lsd.isAborted()) return;
		}
		PicSave.center(this); 
		if (haveFile())
		{	PicSave.setDirectory(FileName.path(Filename)); 
			PicSave.setFilePath(FileName.purefilename(Filename)+".png"); 
		}
		PicSave.setPattern(
			Global.getParameter("pattern.bitmap","*.png")); 
		PicSave.update(!haveFile()); 
		PicSave.setVisible(true); 
		if (PicSave.isAborted()) return; 
		String filename=PicSave.getFilePath(); 
		if (FileName.extension(filename).equals(""))
			filename=filename+".png"; 
		if (Global.getParameter("options.filedialog",true) &&
			exists(filename))
		{	Question qd=new Question(this,
			FileName.filename(filename)+" : "+
				Zirkel.name("file.exists.overwrite"),
				Zirkel.name("file.exists.title"),
				this,false,true); 
			qd.center(this); 
			qd.setVisible(true); 
			if (!qd.yes()) return; 
		}
		try
		{	ZC.startWaiting();
			ZC.savePNG(filename,Global.getParameter("printscale.latex",false));
		} 
		catch (Exception e) {}
		ZC.endWaiting();
	}
	
	/**
	 * Copy graphics to clipboard (in print preview format).
	 */
	public void copyPNG()
	{	ExportScaler d=new ExportScaler(this,true);
		if (d.isAborted()) return;
		try
		{	ZC.startWaiting();
			if (!ZC.savePNG("",false))
				warning(Global.name("exception.clipboardcopy"));
		} 
		catch (Exception e) {}
		ZC.endWaiting();
	}
	
	public void saveFIG ()
	{	PicSave.center(this); 
		if (haveFile())
		{	PicSave.setDirectory(FileName.path(Filename)); 
			PicSave.setFilePath(FileName.purefilename(Filename)+".fig"); 
		}
		PicSave.setPattern(
			Global.getParameter("pattern.fig","*.fig")); 
		PicSave.update(!haveFile()); 
		PicSave.setVisible(true); 
		if (PicSave.isAborted()) return; 
		String filename=PicSave.getFilePath(); 
		if (FileName.extension(filename).equals(""))
			filename=filename+".fig"; 
		if (Global.getParameter("options.filedialog",true) &&
			exists(filename))
		{	Question d=new Question(this,
			FileName.filename(filename)+" : "+
				Zirkel.name("file.exists.overwrite"),
				Zirkel.name("file.exists.title"),
				this,false,true); 
			d.center(this); 
			d.setVisible(true); 
			if (!d.yes()) return; 
		}
		ZC.saveFIG(filename); 
	}
	
	public void saveSVG ()
	{	PicSave.center(this); 
		if (haveFile())
		{	PicSave.setDirectory(FileName.path(Filename)); 
			PicSave.setFilePath(FileName.purefilename(Filename)+".svg"); 
		}
		PicSave.setPattern("*.svg *.svgz"); 
		PicSave.update(!haveFile()); 
		PicSave.setVisible(true); 
		if (PicSave.isAborted()) return; 
		String filename=PicSave.getFilePath(); 
		if (FileName.extension(filename).equals(""))
			filename=filename+
			(Global.getParameter("save.compress",false)?".svgz":".svg"); 
		if (Global.getParameter("options.filedialog",true) &&
			exists(filename))
		{	Question d=new Question(this,
			FileName.filename(filename)+" : "+
				Zirkel.name("file.exists.overwrite"),
				Zirkel.name("file.exists.title"),
				this,false,true); 
			d.center(this); 
			d.setVisible(true); 
			if (!d.yes()) return; 
		}
		try
		{	ZC.startWaiting();
			ZC.saveSVG(filename);
		} 
		catch (Exception e) {}
		ZC.endWaiting(); 
	}
	
	public void savePDF ()
	{	ExportScaler d=new ExportScaler(this,false);
		if (d.isAborted()) return;
		PicSave.center(this); 
		if (haveFile())
		{	PicSave.setDirectory(FileName.path(Filename)); 
			PicSave.setFilePath(FileName.purefilename(Filename)+".pdf"); 
		}
		PicSave.setPattern("*.pdf"); 
		PicSave.update(!haveFile()); 
		PicSave.setVisible(true); 
		if (PicSave.isAborted()) return; 
		String filename=PicSave.getFilePath(); 
		if (FileName.extension(filename).equals(""))
			filename=filename+".pdf"; 
		if (Global.getParameter("options.filedialog",true) &&
			exists(filename))
		{	Question qd=new Question(this,
			FileName.filename(filename)+" : "+
				Zirkel.name("file.exists.overwrite"),
				Zirkel.name("file.exists.title"),
				this,false,true); 
			qd.center(this); 
			qd.setVisible(true); 
			if (!qd.yes()) return; 
		}
		try
		{	ZC.startWaiting();
			ZC.savePDF(filename); 
		} 
		catch (Exception e) {}
		ZC.endWaiting(); 
	}
	
	public void saveEPS ()
	{	ExportScaler d=new ExportScaler(this,false);
		if (d.isAborted()) return;
		PicSave.center(this); 
		if (haveFile())
		{	PicSave.setDirectory(FileName.path(Filename)); 
			PicSave.setFilePath(FileName.purefilename(Filename)+".eps"); 
		}
		PicSave.setPattern("*.eps *.epsz"); 
		PicSave.update(!haveFile()); 
		PicSave.setVisible(true); 
		if (PicSave.isAborted()) return; 
		String filename=PicSave.getFilePath(); 
		if (FileName.extension(filename).equals(""))
			filename=filename+
			(Global.getParameter("save.compress",false)?".epsz":".eps"); 
		if (Global.getParameter("options.filedialog",true) &&
			exists(filename))
		{	Question qd=new Question(this,
			FileName.filename(filename)+" : "+
				Zirkel.name("file.exists.overwrite"),
				Zirkel.name("file.exists.title"),
				this,false,true); 
			qd.center(this); 
			qd.setVisible(true); 
			if (!qd.yes()) return; 
		}
		try
		{	ZC.startWaiting();
			ZC.saveEPS(filename); 
		} 
		catch (Exception e) {}
		ZC.endWaiting(); 
	}
	
	public void setVisual (boolean flag)
	{	Visual.setState(flag); 
		Global.setParameter("options.visual",flag); 
		ZC.Visual=flag; 
		IA.setState("visual",flag); 
		if (!flag)
		{	remove(StatusPanel); 
			add("South",InputPanel); 
			validate(); 
			Input.requestFocus(); 
		}
		else
		{	remove(InputPanel); 
			add("South",StatusPanel); 
			validate(); 
			String s=Status.getText(); 
			Status.setText(""); 
			Status.setText(s); 
			ZC.getConstruction().Hidden=false; 
		}
	}
	
	public void replay ()
	{	ZC.OC.invalidate(ZC);
		ZC.getConstruction().setOriginalOrder(true);
		Replay r=new Replay(this,ZC); 
		r.setVisible(true);
		ZC.getConstruction().setOriginalOrder(false);
		ZC.validate();
		ZC.repaint();
	}
	
	public void editIcons ()
	{	EditIconBar b=new EditIconBar(this,
		RestrictIcons.getState()); 
		if (!b.isAbort()) remakeIconBar(); 
	}
	
	public void makeMainPanel ()
	{	MainPanel=new MyPanel(); 
		MainPanel.setLayout(new BorderLayout()); 
		if (Global.getParameter("options.iconbartop",true))
		{	MainPanel.add("North",North); 
			MainPanel.add("Center",Center); 
		}
		else
		{	MainPanel.add("Center",Center); 
			MainPanel.add("South",North); 
		}
	}
	
	Panel CDP3D;
	MouseAdapter CDPMA;
	
	public Panel makeCenterPanel ()
	{	if (Global.getParameter("options.showdisplay",true))
		{	Panel center=new MyPanel();
			center.setLayout(new BorderLayout());
			ZC.CDP=new ConstructionDisplayPanel(ZC);
			ZC.CDP.setListingBackground(Global.getParameter("colorbackground",
				Color.white));
			ZC.reloadCD();
			CDP3D=new Panel3D(ZC.CDP);
			center.add("West",CDP3D);
			center.add("Center",CenterPanel=new Panel3D(ZC,ZC.getBackground()));
			CDP3D.addMouseListener(CDPMA=new MouseAdapter ()
					{	public void mousePressed (MouseEvent e) 
						{	centerResizeStart(e.getX());
						}
						public void mouseReleased (MouseEvent e)
						{	centerResizeEnd(e.getX());
						}
					}
				);
			return center;
		}
		else
		{	ZC.CDP=null;
			return new Panel3D(ZC,ZC.getBackground());
		}
	}
	
	public int resizeCol;
	boolean resizeFlag=false;
	
	public void centerResizeStart (int col)
	{	if (CDP3D.getSize().width-col>10) return;
		resizeCol=col; resizeFlag=true;
		setCursor(new Cursor(Cursor.W_RESIZE_CURSOR));
	}
	
	public void centerResizeEnd (int col)
	{	if (!resizeFlag) return;
		setCursor(Cursor.getDefaultCursor());
		int c=Global.getParameter("options.constructiondisplay.width",200);
		c=c+(col-resizeCol);
		if (c<10) c=10;
		if (c>600) c=600;
		Global.setParameter("options.constructiondisplay.width",c);
		CDP3D.removeMouseListener(CDPMA);
		Center=makeCenterPanel();
		remove(MainPanel); 
		makeMainPanel(); 
		add("Center",MainPanel); 
		validate(); 
		repaint();
		ZC.requestFocus();
	}
	
	public void reset ()
	{	ZC.reset(); 
		if (CurrentTool==NTargets) settool(NParameters); 
	}
	
	public void browser ()
	{	try
		{	Runtime R=Runtime.getRuntime(); 
			String user=Global.getParameter("browser.user",""); 
			if (user.equals(""))
			{	String file=Zirkel.name("homefile"); 
				if (!new File(file).exists())
					file=Zirkel.name("homepage")+file; 
				String os=System.getProperty("os.name"); 
				String browser="netscape"; 
				if (os.indexOf("Windows")>=0)
				{	browser="explorer"; 
				}
				exec(R,"\""+browser+"\" \""+file+"\""); 
			}
			else
				exec(R,user); 
		}
		catch (Exception e)
		{	e.printStackTrace(); configure(); 
		}
	}
	
	public void exec (Runtime r, String s)
		throws Exception
	{	Vector v=new Vector();
		int pos=0;
		s=s.trim();
		while (pos<s.length())
		{	if (s.charAt(pos)==' ')
			{	pos++;
			}
			else if (s.charAt(pos)=='\"')
			{	pos++;
				int n=s.indexOf('\"',pos);
				if (n>=0)
				{	v.addElement(s.substring(pos,n));
					pos=n+1;
				}
				else
				{	v.addElement(s.substring(pos));
					break;
				}
			}
			else
			{	int n=s.indexOf(' ',pos);
				if (n>=0)
				{	v.addElement(s.substring(pos,n));
					pos=n+1;
				}
				else
				{	v.addElement(s.substring(pos));
					break;
				}
			}
		}
		String c[]=new String[v.size()];
		for (int i=0; i<c.length; i++)
		{	c[i]=(String)v.elementAt(i);
		}
		r.exec(c);
	}
	
	public void configure ()
	{	GetParameter.InputLength=50; 
		GetParameter g=new GetParameter(this,
			Zirkel.name("configure.title"),
			Zirkel.name("configure.prompt"),
			Zirkel.name("ok"),"browser"); 
		String user=Global.getParameter("browser.user",""); 
		if (user.equals(""))
		{	String file=Zirkel.name("homefile"); 
			if (!new File(file).exists())
				file=Zirkel.name("homepage")+file; 
			String browser="netscape"; 
			String os=System.getProperty("os.name"); 
			if (os.indexOf("Windows")>=0) browser="explorer";
			user="\""+browser+"\" \""+file+"\"";
		}
		g.set(user); 
		g.center(this); 
		g.setVisible(true); 
		if (!g.aborted())
			Global.setParameter("browser.user",g.getResult()); 
	}
	
	public void dograb ()
	{	if (IA.getState("grab")) ZC.grab(true); 
		else ZC.grab(false); 
		Background=""; 
	}
	
	public void dograb (boolean flag)
	{	ZC.grab(flag); 
		IA.setState("grag",flag); 
		Background=""; 
	}
	
	public String loadImage ()
	{	ImageLoad.center(this); 
		ImageLoad.update(); 
		ImageLoad.setVisible(true); 
		if (ImageLoad.isAborted()) return ""; 
		return ImageLoad.getFilePath();
	}
	
	public Image doLoadImage (String filename)
	{	filename=FileName.path(Filename)+System.getProperty("file.separator")+filename;
		try
		{	Image i=getToolkit().getImage(filename); 
			MediaTracker mt=new MediaTracker(this); 
			mt.addImage(i,0); 
			mt.waitForID(0); 
			if (mt.checkID(0) && !mt.isErrorAny())
			{	return i; 
			}
			else throw new Exception(Zirkel.name("error.image")); 
		}
		catch (Exception e)
		{	new ShowWarning(this,e.toString()); 
			return null; 
		}
	}	
	
	/**
	 * Loada  background picture, and resize if wanted
	 */
	public void loadBackground ()
	{	BackgroundLoad.center(this); 
		BackgroundLoad.update(); 
		BackgroundLoad.setVisible(true); 
		if (BackgroundLoad.isAborted()) return; 
		String filename=BackgroundLoad.getFilePath();
		doloadBackground(filename);
	}
	
	public void doloadBackground (String filename)
	{	try
		{	Image i=getToolkit().getImage(filename); 
			MediaTracker mt=new MediaTracker(this); 
			mt.addImage(i,0); 
			mt.waitForID(0); 
			if (mt.checkID(0) && !mt.isErrorAny())
			{	ZC.setBackground(i); 
				IA.setState("grab",true); 
				if (Global.getParameter("background.usesize",false)) resize(); 
			}
			else throw new Exception(Zirkel.name("background.loaderror")); 
			Background=FileName.filename(filename); 
		}
		catch (Exception e)
		{	warning(e.toString()); 
			Background=""; 
		}
	}	
	
	public void resize	()
	{	pack(); 
	}
	
	public void track ()
	{	if (ZC.getCurrentTool() instanceof ObjectTracker &&
		((ObjectTracker)ZC.getCurrentTool()).isComplete())
		{	Question q=new Question(this,
			Zirkel.name("trackquestion.keep"),
				Zirkel.name("trackquestion.title"),true); 
			q.center(this); 
			q.setVisible(true); 
			if (q.yes()) ((ObjectTracker)ZC.getCurrentTool()).keep(ZC); 
		}
	}
	
	public void restrictIcons (boolean flag)
	{	if (flag)
		{	RestrictIcons.setState(true); 
			showDefaultIcons(false);
			remakeIconBar(); 
		}
		else
		{	RestrictIcons.setState(false); 
			showDefaultIcons(true);
			remakeIconBar(); 
		}
	}
	
	Info InfoWindow; 
	
	public synchronized void removeInfo ()
	{	InfoWindow=null; 
	}
	
	public synchronized void info ()
	{	if (InfoWindow!=null)
		{	InfoWindow.doclose(); 
		}
		else 
		{	InfoWindow=new Info(this); 
		}
	}
	
	public void setinfo (String s)
	{	Info.Subject=s; 
		if (InfoWindow!=null) InfoWindow.fill(); 
	}
	
	/**
	 * Load default macros from ".default.mcr", if that
	 * file exists. Else load from resources, if such
	 * a file exists in the resources. 
	 */
	public void loadDefaultMacros ()
	{	String Filename=".default.mcr";
		if (new File("."+Zirkel.name("language","")+"default.mcr").exists())
			Filename="."+Zirkel.name("language","")+"default.mcr";
		if (new File(Filename).exists())
		{	try
			{	InputStream o=new FileInputStream(Filename); 
				if (isCompressed(Filename))
					o=new GZIPInputStream(o); 
				ZC.ProtectMacros=true;
				ZC.load(o,false,true); 
				ZC.ProtectMacros=false;
				o.close();
			}
			catch (Exception e)
			{	warning(Zirkel.name("warning.load"));
			}
			return;
		}
		try
		{	InputStream o=
				getClass().getResourceAsStream("/"+
					Zirkel.name("language","")+"default.mcr"); 
			ZC.ProtectMacros=true;
			ZC.load(o,false,true); 
			ZC.ProtectMacros=false;
			o.close();		
			return;	
		}
		catch (Exception e) {}
		try
		{	InputStream o=
				getClass().getResourceAsStream("/default.mcr"); 
			ZC.ProtectMacros=true;
			ZC.load(o,false,true); 
			ZC.ProtectMacros=false;
			o.close();
			return;			
		}
		catch (Exception e) {}
	}

	public void showDefaultIcons (boolean flag)
	{	if (!flag) ZC.clearProtectedMacros();
		else loadDefaultMacros();
	}	

	/**
	 * Search for a help topic.
	 * This help should be replaced by the context help.
	 * 
	 * @param subject file name
	 * @return
	 */
	public boolean haveHelp (String subject)	
	{	String lang=Zirkel.name("language","");
		try
		{	BufferedReader in=new BufferedReader(new InputStreamReader(
				getClass().getResourceAsStream("/rene/zirkel/docs/"+lang+subject)));
			in.close();
		}
		catch (Exception e)
		{	return false;
		}
		return true;
	}
	
	/**
	 * Opened or saved a file (or new construction).
	 * @return state
	 */
	public boolean haveFile ()
	{	return !Filename.equals("");
	}
	
	public void newfile (boolean defaults)
	{	if (ZC.changed())
		{	Question q=new Question(this,Zirkel.name("savequestion.qsave"),
			Zirkel.name("savequestion.title"),true); 
			q.center(this); 
			q.setVisible(true); 
			if (q.yes() && !savefile()) return; 
			if (q.isAborted()) return; 
		}
		clear(defaults); 
		if (RestrictIcons.getState())
		{	RestrictIcons.setState(false); 
			showDefaultIcons(true);
			remakeIconBar(); 
		}
		Filename=""; 
		clearNonprotectedMacros();
		setTitle(Zirkel.name("program.name")); 
	}

	public void warning (String s)
	{	Warning w=new Warning(this,s,
				"",true);
		w.center(this);
		w.setVisible(true);
	}

	/**
	 * Display or hide the permanent construction display
	 * @param flag
	 */
	public void showConstructionDisplay (boolean flag)
	{	Global.setParameter("options.showdisplay",flag);
		notePosition("zirkelframe");
		ConstructionDisplay.setState(flag);
		Center=makeCenterPanel();
		ZC.UseSize=ZC.getSize();
		remove(MainPanel); 
		makeMainPanel(); 
		add("Center",MainPanel); 
		validate();
		// resize();
		ZC.UseSize=null;
		repaint();
		ZC.requestFocus();
	}

	/**
	 * Set the show hidden state
	 * @param flag
	 */
	public void sethidden (boolean flag)
	{	ZC.setShowHidden(flag); 
		IA.setState("hidden",flag); 
		ShowHidden.setState(flag);
	}

	public void editGrid ()
	{	EditGridDialog d=new EditGridDialog(this);
		ZC.setGrid();
		ZC.repaint();
	}
}
