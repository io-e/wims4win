package rene.zirkel.help;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import rene.gui.*;
import rene.viewer.*;
import rene.zirkel.Zirkel;

public class Help extends CloseFrame implements ActionListener
{	Viewer V;
	Button Close;
	static public String CodePage="";
	// display the help from subject.txt
	public Help (String subject)
	{	super(Zirkel.name("help.title","Help"));
		V=new Viewer();
		if (Global.Background!=null) V.setBackground(Global.Background);
		V.setFont(Global.FixedFont);
		V.setTabWidth(4);
		String lang=Global.name("language","");
		while (true)
		{	try
			{	BufferedReader in;
				try
				{	if (CodePage.equals(""))
						in=new BufferedReader(new InputStreamReader(
						getClass().getResourceAsStream("/rene/zirkel/docs/"+lang+subject),CodePage));
					else 
						in=new BufferedReader(new InputStreamReader(
						getClass().getResourceAsStream("/rene/zirkel/docs/"+lang+subject),CodePage));
				}
				catch (Exception e)
				{	in=new BufferedReader(new InputStreamReader(
						getClass().getResourceAsStream("/rene/zirkel/docs/"+lang+subject)));
				}
				while (true)
				{	String s=in.readLine();
					if (s==null) break;
					V.appendLine(s);
				}
				in.close();
			}
			catch (Exception e)
			{	if (!lang.equals(""))
				{	lang=""; continue;
				}
				else
				{	V.setText(
						Zirkel.name("help.error","Could not find the help file!"));
				}
			}
			break;
		}
		V.doUpdate(false);
		
		setLayout(new BorderLayout());
		setSize(600,600);
		setLocation(120,70);
		add("Center",V);
		Panel p=new MyPanel();
		p.add(Close=new Button(Zirkel.name("close","Close")));
		Close.addActionListener(this);
		add("South",p);
		setPosition("help");
		seticon("rene/zirkel/icon.png");
		setVisible(true);
	}
	public void actionPerformed (ActionEvent e)
	{	if (e.getSource()==Close)
		{	notePosition("help");
			setVisible(false); dispose();
		}
	}
}
