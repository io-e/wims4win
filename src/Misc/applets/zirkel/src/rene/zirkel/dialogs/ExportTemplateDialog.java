package rene.zirkel.dialogs;

import java.awt.*;

import rene.gui.*;
import rene.util.*;
import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelFrame;
import rene.zirkel.construction.Construction;

public class ExportTemplateDialog extends HelpCloseDialog
{	ZirkelFrame ZF;
	Construction C;
	TextField PageTitle,CFile,Codebase;
	Checkbox Job;
	Panel CP;
	Choice Style;
	boolean OK=false;
	IconBar IBa,IBb;
	boolean SaveDimensions=true;
	boolean Restrict;

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
	
	public ExportTemplateDialog (ZirkelFrame zf, Construction c, boolean restrict)
	{	super(zf,Zirkel.name("export.title"),true);
		ZF=zf; C=c; Restrict=restrict;
		setLayout(new BorderLayout());
		
		Panel p=new MyPanel();
		p.setLayout(new GridLayout(0,2));

		p.add(new MyLabel(Zirkel.name("export.pagetitle")));
		p.add(PageTitle=new TextFieldAction(this,"export.pagetitle",32));
		PageTitle.setText(FileName.purefilename(zf.Filename));

		p.add(new MyLabel(Zirkel.name("export.codebase")));
		p.add(Codebase=new TextFieldAction(this,"export.codebase",32));
		Codebase.setText(Global.getParameter("export.codebase","."));

		p.add(new MyLabel(Zirkel.name("export.file")));
		p.add(CFile=new TextFieldAction(this,"export.file",32));
		CFile.setText(Global.getParameter("export.path","")+
			FileName.filename(zf.Filename));

		if (zf.ZC.isJob())
		{	p.add(new MyLabel(""));
			p.add(Job=
				new CheckboxAction(this,Zirkel.name("export.job")));
			Job.setState(true);
		}
		
		Style=new MyChoice();
		for (int i=0; i<ST.length; i++)
			Style.add(Global.name("export."+ST[i]));
		int style=Global.getParameter("export.style",0);
		if (style<=1 && ZF.ZC.isJob()) style=3;
		Style.select(style);
		p.add(new MyLabel(Zirkel.name("export.style")));
		p.add(Style);
		
		add("North",new Panel3D(p));
		
		Panel icons=new MyPanel();
		icons.setLayout(new GridLayout(0,1));
		IBa=new IconBar(ZF);
		String ica;
		if (restrict)
			ica=Global.getParameter("restrictedicons",
				"point line segment ray circle circle3 fixedcircle fixedangle intersection");		
		else
			ica=Global.getParameter("export.icons",
				"point line segment ray circle circle3 fixedcircle fixedangle intersection");
		for (int i=0; i<ZirkelFrame.IconNumber; i++)
		{	String s=ZirkelFrame.ObjectStrings[i];
			IBa.addToggleLeft(s);	
			if (ica.equals("full") || ica.indexOf(s)>=0)
				IBa.setState(s,true);
		}
		icons.add(new Panel3D(IBa));
		IBb=new IconBar(ZF);
		String icb;
		if (restrict)
			icb=Global.getParameter("restrictedicons",
				"back hidden color twolines ");
		else
			icb=Global.getParameter("export.tools",
				"back hidden color twolines ");
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
		buttons.add(new ButtonAction(this,Zirkel.name("ok"),"OK"));
		buttons.add(new ButtonAction(this,Zirkel.name("abort"),"Close"));
		addHelp(buttons,"htmlexporttemplate");
		add("South",buttons);
		
		pack();
		if (getSize().width>750) setSize(750,getSize().height);
	}

	public void doAction (String o)
	{	OK=false;
		if (o.equals("OK"))
		{	OK=true;
			int i=Style.getSelectedIndex();
			if (i>=0) Global.setParameter("export.style",i);
			getIcons();
			getTools();
			doclose();
		}
		else super.doAction(o);
	}
	
	public boolean isAborted ()
	{	return !OK;
	}
	
	public String getPageTitle () { return PageTitle.getText(); }
	
	public String getIcons ()
	{	StringBuffer b=new StringBuffer();
		boolean first=true;
		for (int i=0; i<ZirkelFrame.IconNumber; i++)
		{	String s=ZirkelFrame.ObjectStrings[i];
			if (IBa.getState(s))
			{	if (!first) b.append(' ');
				b.append(s);
				first=false;
			}
		}
		if (!Restrict)
			Global.setParameter("export.icons",b.toString());
		return b.toString();
	}

	public String getTools ()
	{	StringBuffer b=new StringBuffer();
		boolean first=true;
		for (int i=0; i<ISb.length; i++)
		{	String s=ISb[i];
			if (s.equals("indicate") && !IBb.getState(s))
			{	if (!first) b.append(' ');
				b.append("noindicate");
				first=false;
			}
			else if (IBb.getState(s))
			{	if (!first) b.append(' ');
				b.append(ISb0[i]);
				first=false;
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
	
	public String getFile ()
	{	String path=FileName.pathAndSeparator(CFile.getText());
		Global.setParameter("export.path",path);
		return CFile.getText(); 
	}
	
	public String getCodebase ()
	{	Global.setParameter("export.codebase",Codebase.getText());
		return Codebase.getText();
	}

	public boolean isJob()
	{	if (Job==null) return false;
		return Job.getState(); 
	}
	
}
