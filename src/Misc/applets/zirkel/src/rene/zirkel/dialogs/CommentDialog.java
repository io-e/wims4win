package rene.zirkel.dialogs;

// file: ZirkelFrame.java

import java.awt.*;
import java.awt.event.*;

import rene.gui.*;
import rene.zirkel.Zirkel;

public class CommentDialog extends HelpCloseDialog implements DoActionListener
{	TextArea V;
	String S;
	Button Close;
	public CommentDialog (Frame f, String s, String title, boolean readonly)
	{	super(f,title,true);
		setLayout(new BorderLayout());
		S=s;
		V=new TextArea(s,30,60,TextArea.SCROLLBARS_VERTICAL_ONLY);
		if (Global.Background!=null) V.setBackground(Global.Background);
		add("Center",V);
		Panel p=new MyPanel();
		if (readonly)
		{	p.add(Close=new ButtonAction(this,Zirkel.name("close"),"Close"));
		}
		else
		{	p.add(new ButtonAction(this,Zirkel.name("ok"),"OK"));
			p.add(Close=new ButtonAction(this,Zirkel.name("cancel"),"Close"));
		}
		addHelp(p,"comment");
		add("South",new Panel3D(p));
		pack();
		center(f);
		V.addKeyListener(this);
		V.setEditable(!readonly);
		setVisible(true);
	}
	public void doAction (String s)
	{	if (s.equals("OK"))
		{	S=V.getText();
			doclose();
		}
		else super.doAction(s);
	}
	public String getText ()
	{	return S;
	}
	public boolean escape ()
	{	return true;
	}
	public void focusGained (FocusEvent e)
	{	Close.requestFocus();
	}
}
