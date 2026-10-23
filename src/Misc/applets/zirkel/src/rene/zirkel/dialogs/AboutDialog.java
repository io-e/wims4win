package rene.zirkel.dialogs;

// file: AboutDialog.java

import java.awt.*;
import java.awt.event.*;

import rene.gui.*;
import rene.zirkel.Zirkel;

public class AboutDialog extends CloseDialog
{	public AboutDialog (Frame f)
	{	super(f,Zirkel.name("about.title","About this Program"),true);
		setLayout(new BorderLayout());
		Panel text=new MyPanel();
		text.setLayout(new GridLayout(0,1));
		MyLabel l;
		text.add(l=new MyLabel(Zirkel.name("program.name")));
		l.setAlignment(Label.CENTER);
		text.add(l=new MyLabel(Zirkel.name("version")+" "+Zirkel.name("program.version")));
		l.setAlignment(Label.CENTER);
		text.add(l=new MyLabel(Zirkel.name("date")+" "+Zirkel.name("program.date")));
		l.setAlignment(Label.CENTER);
		String empty="                                         ";
		text.add(l=new MyLabel(empty+"***"+empty));
		l.setAlignment(Label.CENTER);
		text.add(l=new MyLabel(Zirkel.name("about.programmed","programmed by")));
		l.setAlignment(Label.CENTER);
		text.add(l=new MyLabel("R. Grothmann"));
		l.setAlignment(Label.CENTER);
		text.add(l=new MyLabel(empty+"***"+empty));
		l.setAlignment(Label.CENTER);
		text.add(l=new MyLabel(Zirkel.name("about.language")+": "
			+Global.name("language","")));
		l.setAlignment(Label.CENTER);
		add("Center",new Panel3D(text));
		Panel p=new MyPanel();
		Button b=new Button(Zirkel.name("close","Close"));
		b.addActionListener(this);
		p.add(b);
		add("South",new Panel3D(p));
		addWindowListener( // to close properly
			new WindowAdapter ()
			{	public void windowClosing (WindowEvent e)
				{	doclose();
				}
			}
		);
		pack();
		center(f);
		setVisible(true);
	}
	public void actionPerformed (ActionEvent e)
	{	dispose();
	}
}
