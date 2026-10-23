package rene.zirkel.dialogs;

import java.awt.*;
import java.awt.event.ActionEvent;

import rene.gui.*;
import rene.util.*;
import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelFrame;
import rene.zirkel.construction.Construction;
import rene.dialogs.*;

public class ExportDialog extends HelpCloseDialog
{	ZirkelFrame ZF;
	Construction C;
	TextField PageTitle,CFile,Jar,Width,Height,Solution,StyleSheet;
	Checkbox BackgroundColor,AppletColor,Job,Comment,SaveSolution,LinkSolution,
		JumpSolution,Digits,Colors,Background,Popup,Zoom,RestrictedMove;
	Panel CP;
	Choice Style;
	boolean OK=false;
	IconBar IBa,IBb;
	boolean SaveDimensions=true;
	boolean Restrict;
	double Persp=1; // Fenster Höhe / Breite

	static String ISb[]={"back","delete","undo","showcolor","showname","showvalue",
		"color","hidden","macro0","replay","arrow","obtuse","solid","grid",
		"thickness0","type0","partial","plines",
		"twolines","macrobar","qintersection","qpointon","qchoice","indicate",
		"draw","rename","function"};
	static String ISb0[]={"back","delete","undo","showcolor","showname","showvalue",
		"color","hidden","macro","replay","arrow","obtuse","solid","grid",
		"thickness","type","partial","plines",
		"twolines","macrobar","qintersection","qpointon","qchoice","indicate",
		"draw","rename","function"};
	
	static String ST[]={"plain","3D","icons","full","nonvisual","breaks"};
	
	public ExportDialog (ZirkelFrame zf, Construction c, boolean restrict)
	{	super(zf,Zirkel.name("export.title"),true);
		ZF=zf; C=c; Restrict=restrict;
		setLayout(new BorderLayout());
		
		Panel p=new MyPanel();
		p.setLayout(new GridLayout(0,2));

		p.add(new MyLabel(Zirkel.name("export.pagetitle")));
		p.add(PageTitle=new TextFieldAction(this,"export.pagetitle",32));
		PageTitle.setText(FileName.purefilename(zf.Filename));

		p.add(new MyLabel(Zirkel.name("export.stylesheet")));
		p.add(StyleSheet=new TextFieldAction(this,"export.stylesheet",32));
		StyleSheet.setText(Global.getParameter("export.stylesheet",""));

		p.add(new MyLabel(Zirkel.name("export.width")));
		p.add(Width=new TextFieldAction(this,"export.width",32));
		Width.setText(""+Global.getParameter("export.width",600));
		Width.addActionListener(this);

		p.add(new MyLabel(Zirkel.name("export.height")));
		p.add(Height=new TextFieldAction(this,"export.height",32));
		Height.setText(""+Global.getParameter("export.height",600));
		Height.addActionListener(this);

		p.add(new MyLabel(Zirkel.name("export.file")));
		p.add(CFile=new TextFieldAction(this,"export.file",32));
		CFile.setText(FileName.filename(zf.Filename));

		p.add(new MyLabel(Zirkel.name("export.jar")));
		p.add(Jar=new TextFieldAction(this,"export.jar",32));
		Jar.setText(Global.getParameter("export.jar","zirkel.jar"));

		if (ZF.ZC.isJob())
		{	p.add(new MyLabel(""));
			p.add(Job=
				new CheckboxAction(this,Zirkel.name("export.job")));
			Job.setState(ZF.ZC.isJob());
		}

		if (ZF.ZC.isJob())
		{	p.add(new MyLabel(Zirkel.name("export.solution")));
			p.add(Solution=new TextFieldAction(this,"export.solution",32));
			Solution.setText(FileName.purefilename(zf.Filename)+"-sol.html");
	
			p.add(JumpSolution=
				new CheckboxAction(this,Zirkel.name("export.jumpsol")));
			JumpSolution.setState(Global.getParameter("export.jumpsol",false));
	
			p.add(SaveSolution=
				new CheckboxAction(this,Zirkel.name("export.savesol")));
			SaveSolution.setState(Global.getParameter("export.savesol",false));
	
			p.add(LinkSolution=
				new CheckboxAction(this,Zirkel.name("export.linksol")));
			LinkSolution.setState(Global.getParameter("export.linksol",false));
			
			p.add(new MyLabel(""));
		}

		p.add(new MyLabel(Zirkel.name("export.color")));
		p.add(CP=new MyPanel());
		CP.setBackground(Global.getParameter("export.color",
			ZF.ZC.getBackground()));

		p.add(BackgroundColor=
			new CheckboxAction(this,Zirkel.name("export.backgroundcolor")));
		BackgroundColor.setState(Global.getParameter("export.backgroundcolor",true));

		p.add(AppletColor=
			new CheckboxAction(this,Zirkel.name("export.appletcolor")));
		AppletColor.setState(Global.getParameter("export.appletcolor",true));
		
		Style=new MyChoice();
		for (int i=0; i<ST.length; i++)
			Style.add(Global.name("export."+ST[i]));
		int style=Global.getParameter("export.style",0);
		if (style<=1 && ZF.ZC.isJob()) style=3;
		Style.select(style);
		p.add(new MyLabel(Zirkel.name("export.style")));
		p.add(Style);
		
		p.add(Digits=
			new CheckboxAction(this,Zirkel.name("export.digits")));
		Digits.setState(Global.getParameter("export.digits",true));
		p.add(Comment=
			new CheckboxAction(this,Zirkel.name("export.comment")));
		Comment.setState(Global.getParameter("export.comment",true));

		p.add(Colors=
			new CheckboxAction(this,Zirkel.name("export.colors")));
		Colors.setState(Global.getParameter("export.colors",true));
		p.add(Background=
			new CheckboxAction(this,Zirkel.name("export.background")));
		Background.setState(Global.getParameter("export.background",false));

		p.add(Popup=
			new CheckboxAction(this,Zirkel.name("export.popup")));
		Popup.setState(Global.getParameter("export.popup",true));
		p.add(Zoom=
			new CheckboxAction(this,Zirkel.name("export.zoom")));
		Zoom.setState(Global.getParameter("export.zoom",false));

		p.add(RestrictedMove=
			new CheckboxAction(this,Zirkel.name("export.restrictedmove")));
		RestrictedMove.setState(Global.getParameter("export.restrictedmove",false));
		p.add(new MyLabel(""));

		add("North",new Panel3D(p));
		
		Panel icons=new MyPanel();
		icons.setLayout(new GridLayout(0,1));
		IBa=new IconBar(ZF);
		String ica;
		if (restrict)
			ica=Global.getParameter("restrictedicons",
				" point line segment ray circle circle3 fixedcircle fixedangle intersection ");		
		else
			ica=Global.getParameter("export.icons",
				" point line segment ray circle circle3 fixedcircle fixedangle intersection ");
		for (int i=0; i<ZirkelFrame.IconNumber; i++)
		{	String s=ZirkelFrame.ObjectStrings[i];
			IBa.addToggleLeft(s);	
			if (ica.equals("full") || ica.indexOf(" "+s+" ")>=0)
				IBa.setState(s,true);
		}
		icons.add(new Panel3D(IBa));
		IBb=new IconBar(ZF);
		String icb;
		if (restrict)
			icb=Global.getParameter("restrictedicons",
				"back hidden color indicate twolines ");
		else
			icb=Global.getParameter("export.tools",
				"back hidden color indicate twolines ");
		for (int i=0; i<ISb.length; i++)
		{	String s=ISb[i];
			if (s.equals("qintersection")) IBb.addSeparatorLeft();
			if (s.equals("twolines")) IBb.addSeparatorLeft();
			if (s.equals("draw")) IBb.addSeparatorLeft();
			IBb.addToggleLeft(s);
			if (icb.equals("full") || icb.indexOf(ISb0[i])>=0)
				IBb.setState(s,true);
		}
		icons.add(new Panel3D(IBb));
		IBb.setState("qchoice",Global.getParameter("options.choice",false));
		IBb.setState("qintersection",Global.getParameter("options.intersection",false));
		IBb.setState("qpointon",Global.getParameter("options.pointon",false));
		IBb.setState("indicate",!Global.getParameter("options.indicate.simple",false));
		add("Center",icons);
		
		Panel buttons=new MyPanel();
		buttons.add(new ButtonAction(this,Zirkel.name("export.setsize"),"setsize"));
		buttons.add(new ButtonAction(this,Zirkel.name("export.setcolor"),"setcolor"));
		buttons.add(new MyLabel(" "));
		buttons.add(new ButtonAction(this,Zirkel.name("ok"),"OK"));
		buttons.add(new ButtonAction(this,Zirkel.name("abort"),"Close"));
		addHelp(buttons,"htmlexport");
		add("South",buttons);
		
		pack();
		if (getSize().width>750) setSize(750,getSize().height);
		
		Dimension d=ZF.ZC.getSize();
		if (d.width>0) Persp=((double)d.height)/d.width;
	}

	public void actionPerformed (ActionEvent e)
	{	if (e.getSource()==Width)
		{	int w=getWidth();
			Width.setText(""+w);
			Height.setText(""+(int)(w*Persp));	
		}
		else if (e.getSource()==Height && Persp>0)
		{	int h=getHeight();
			Height.setText(""+h);
			Width.setText(""+(int)(h/Persp));
		}		
	}
	

	public void doAction (String o)
	{	OK=false;
		if (o.equals("OK"))
		{	OK=true;
			Global.setParameter("export.stylesheet",StyleSheet.getText());
			Global.setParameter("export.jar",Jar.getText());
			if (SaveDimensions) try
			{	Global.setParameter("export.height",
					Integer.parseInt(Height.getText()));
				Global.setParameter("export.width",
					Integer.parseInt(Width.getText()));
			}
			catch (Exception e) {}
			Global.setParameter("export.color",CP.getBackground());
			Global.setParameter("export.backgroundcolor",BackgroundColor.getState());
			Global.setParameter("export.appletcolor",AppletColor.getState());
			if (JumpSolution!=null)
				Global.setParameter("export.jumpsol",JumpSolution.getState());
			if (SaveSolution!=null)
				Global.setParameter("export.savesol",SaveSolution.getState());
			if (LinkSolution!=null)
				Global.setParameter("export.linksol",LinkSolution.getState());
			Global.setParameter("export.comment",Comment.getState());
			Global.setParameter("export.colors",Colors.getState());
			Global.setParameter("export.background",Background.getState());
			Global.setParameter("export.zoom",Zoom.getState());
			Global.setParameter("export.restrictedmove",RestrictedMove.getState());
			Global.setParameter("export.popup",Popup.getState());
			int i=Style.getSelectedIndex();
			if (i>=0) Global.setParameter("export.style",i);
			Global.setParameter("export.digits",Digits.getState());
			getIcons();
			getTools();
			doclose();
		}
		else if (o.equals("setcolor"))
		{	ColorEditor d=new ColorEditor(ZF,"export.color",CP.getBackground());
			d.center(ZF);
			d.setVisible(true);
			CP.setBackground(
				Global.getParameter("export.color",CP.getBackground()));
		}
		else if (o.equals("setsize"))
		{	Dimension d=ZF.ZC.getSize();
			Height.setText(""+d.height);
			Width.setText(""+d.width);
		}
		else super.doAction(o);
	}
	
	public boolean isAborted ()
	{	return !OK;
	}
	
	public String getPageTitle () { return PageTitle.getText(); }
	public String getStyleSheet () { return StyleSheet.getText(); }
	public String getFile () { return CFile.getText(); }
	public String getDezimalColor ()
	{	Color c=CP.getBackground();
		return c.getRed()+","+c.getGreen()+","+c.getBlue(); 
	}
	public String getHexColor ()
	{	Color c=CP.getBackground();
		return "#"+hex(c.getRed())+hex(c.getGreen())+hex(c.getBlue()); 
	}
	static char A[]={'0','1','2','3','4','5','6','7','8','9',
		'A','B','C','D','E','F'};
	String hex (int n)
	{	return ""+A[n/16]+A[n%16];
	}

	public boolean useForBackground()
	{	return BackgroundColor.getState(); 
	}
	public boolean useForApplet()
	{	return AppletColor.getState(); 
	}

	public boolean isJob()
	{	if (Job==null) return false;
		return Job.getState(); 
	}
	
	public String getIcons ()
	{	StringBuffer b=new StringBuffer();
		b.append(" ");
		for (int i=0; i<ZirkelFrame.IconNumber; i++)
		{	String s=ZirkelFrame.ObjectStrings[i];
			if (IBa.getState(s))
			{	b.append(s);
				b.append(" ");
			}
		}
		if (!Restrict)
			Global.setParameter("export.icons",b.toString());
		return b.toString();
	}

	public String getTools ()
	{	StringBuffer b=new StringBuffer();
		b.append(" ");
		for (int i=0; i<ISb.length; i++)
		{	String s=ISb[i];
			if (s.equals("indicate") && !IBb.getState(s))
			{	b.append("noindicate ");
			}
			else if (IBb.getState(s))
			{	b.append(ISb0[i]);
				b.append(" ");
			}
		}
		if (!Restrict)
			Global.setParameter("export.tools",b.toString());
		return b.toString();
	}
	
	public String getStyle ()
	{	int n=Style.getSelectedIndex();
		if (n>=0) Global.setParameter("export.style",n);
		if (n<0) return ST[0];
		else return ST[n];
	}
	
	public String getJar ()
	{	return Jar.getText();
	}
	
	public String getSolution ()
	{	if (Solution==null) return "";
		return Solution.getText();
	}
	
	public boolean saveComment ()
	{	Global.setParameter("export.comment",Comment.getState());
		return Comment.getState();
	}
	
	public int getWidth ()
	{	try
		{	int w=Integer.parseInt(Width.getText());
			return w;
		}
		catch (Exception e)
		{	return Global.getParameter("export.width",550);
		}
	}

	public int getHeight ()
	{	try
		{	int w=Integer.parseInt(Height.getText());
			return w;
		}
		catch (Exception e)
		{	return Global.getParameter("export.height",550);
		}
	}
	
	public boolean jumpSolution ()
	{	if (JumpSolution==null) return false;
		return JumpSolution.getState();
	}

	public boolean saveSolution ()
	{	if (SaveSolution==null) return false;
		return SaveSolution.getState();
	}

	public boolean linkSolution ()
	{	if (LinkSolution==null) return false;
		return LinkSolution.getState();
	}
	
	public boolean saveDigits ()
	{	return Digits.getState();
	}

	public boolean saveColors ()
	{	return Colors.getState();
	}

	public boolean saveBackground ()
	{	return Background.getState();
	}
	
	public boolean allowZoom ()
	{	return Zoom.getState();
	}
	
	public boolean allowPopup ()
	{	return Popup.getState();
	}

	public boolean restrictedMove ()
	{	return RestrictedMove.getState();
	}
	
	public void setDimensions (int w, int h)
	{	Height.setText(""+h); Height.setEnabled(false);
		Width.setText(""+w); Width.setEnabled(false);
		SaveDimensions=false;
	}

}
