package rene.zirkel;

// File: Zirkel.java

import java.awt.*;
import java.applet.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

import rene.dialogs.Warning;
import rene.gui.*;
import rene.zirkel.help.*;

public class Zirkel extends Applet
	implements ActionListener
{	public static final long Version=110;
	public static boolean IsApplet=false;
	public static String name (String tag, String def)
	{	return Global.name(tag,def);
	}
	public static String name (String tag)
	{	return Global.name(tag);
	}
	public void init ()
	// zirkel is called as applet
	{	String s=getParameter("Language");
		if (s!=null) Locale.setDefault(new Locale(s,""));
		Global.initBundle("rene/zirkel/docs/ZirkelProperties");
		setLayout(new BorderLayout());
		Button StartButton=new Button("Start");
		add("Center",StartButton);
		StartButton.addActionListener(this);
		IsApplet=true;
		if (getParameter("oldicons")!=null)
		{	Global.setParameter("iconpath","/rene/zirkel/icons/");
			Global.setParameter("icontype","gif");
			Global.setParameter("iconsize",20);
		}	
		else
		{	Global.setParameter("iconpath","/rene/zirkel/newicons/");
			Global.setParameter("icontype","png");
			if (getParameter("smallicons")!=null)
				Global.setParameter("iconsize",24);
			else Global.setParameter("iconsize",32);
		}
	}

	public static void main (String args[])
	// zirkel is calles as application
	{	int i=0;
		String filename="";
		boolean simple=false,restricted=false;
		String Home=null;
		if (Global.getJavaVersion()<1.4)
		{	Frame f=new Frame();
			Warning w=new Warning(f,"Please update to Java 1.4!","Warning");
			w.center();
			w.setVisible(true);
			System.exit(0);
		}
		while (i<args.length)
		{	if (args[i].startsWith("-l") && i<args.length-1)
			{	Locale.setDefault(new Locale(args[i+1],""));
				i+=2;
			}
			else if (args[i].startsWith("-h") && i<args.length-1)
			{	Home=args[i+1];
				i+=2;
			}
			else if (args[i].startsWith("-s"))
			{	simple=true;
				i++;
			}
			else if (args[i].startsWith("-r"))
			{	restricted=true;
				i++;
			}
			else if (args[i].startsWith("-d"))
			{	Properties p=System.getProperties();
				try
				{	PrintStream out=new PrintStream(
						new FileOutputStream(p.getProperty("user.home")+
							p.getProperty("file.separator")+"zirkel.log"));
					System.setErr(out);
					System.setOut(out);
				}
				catch (Exception e)
				{	System.out.println("Could not open log file!");
				}
				i++;
			}
			else
			{	filename=args[i];
				i++;
			}
		}
		if (new File(".zir.cfg").exists()) Global.loadProperties(".zir.cfg");
		else if (Home!=null) Global.loadProperties(Home,".zir.cfg");
		else Global.loadPropertiesInHome(".zir.cfg");
		Global.initBundle("rene/zirkel/docs/ZirkelProperties",true);
		if (simple) Global.setParameter("simplegraphics",simple); 
		if (restricted) Global.setParameter("restricted",true);
		if (!Global.getParameter("program.version","").equals(
			Zirkel.name("program.version")))
		{	Global.setParameter("program.newversion",true);
			Global.setParameter("program.version",Zirkel.name("program.version"));
			Global.setParameter("icons",ZirkelFrame.DefaultIcons);
		}
		if (simple || Global.getParameter("options.oldicons",false))
		{	Global.setParameter("iconpath","/rene/zirkel/icons/");
			Global.setParameter("icontype","gif");
			Global.setParameter("iconsize",20);
		}	
		else
		{	Global.setParameter("iconpath","/rene/zirkel/newicons/");
			Global.setParameter("icontype","png");
			if (Global.getParameter("options.smallicons",false))
				Global.setParameter("iconsize",24);
			else Global.setParameter("iconsize",32);
		}
		Help.CodePage=Global.name("codepage.help","");
		ZirkelFrame f=new ZirkelFrame(false);
		if (!filename.equals(""))
		{	f.load(filename);
		}
		if (!Global.haveParameter("options.germanpoints") && 
				Locale.getDefault().getLanguage().equals("de"))
			Global.setParameter("options.germanpoints",true);
			
	}
	
	public void actionPerformed (ActionEvent e)
	// the user pressed the start button of the applet
	{	ZirkelFrame F=new ZirkelFrame(true);
		F.setVisible(true);
	}
} 

