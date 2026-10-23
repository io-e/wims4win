/*
 * Created on 22.10.2005
 *
 */
package rene.zirkel.dialogs;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Panel;

import rene.gui.ButtonAction;
import rene.gui.CheckboxAction;
import rene.gui.Global;
import rene.gui.IconBar;
import rene.gui.MyPanel;
import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelFrame;

public class EditIconBar extends HelpCloseDialog
{	IconBar IA,IAA,IB; 
	Checkbox C,CS; 
	boolean Abort=true; 
	boolean Restrict; 
	
	public EditIconBar (Frame f, boolean restrict)
	{	super(f,Zirkel.name("iconedit.title"),true); 
		Restrict=restrict; 
		setLayout(new BorderLayout()); 
		
		Panel p=new MyPanel(); 
		p.setLayout(new GridLayout(0,1)); 
		IA=new IconBar(f,false); 
		add(IA,"new"); 
		add(IA,"load"); 
		add(IA,"save"); 
		IA.addSeparatorLeft(); 
		add(IA,"back"); 
		add(IA,"delete"); 
		add(IA,"undo"); 
		IA.addSeparatorLeft(); 
		add(IA,"color","color"); 
		add(IA,"type","type0"); 
		add(IA,"thickness","thickness0"); 
		add(IA,"partial"); 
		add(IA,"plines"); 
		add(IA,"arrow"); 
		IA.addSeparatorLeft(); 
		add(IA,"showname"); 
		add(IA,"longnames"); 
		add(IA,"large"); 
		add(IA,"bold"); 
		add(IA,"showvalue"); 
		add(IA,"obtuse"); 
		add(IA,"solid"); 
		IA.addSeparatorLeft(); 
		add(IA,"edit"); 
		p.add(IA); 
		IAA=new IconBar(f,false); 
		add(IAA,"hidden"); 
		add(IAA,"showcolor","showcolor0"); 
		IAA.addSeparatorLeft(); 
		add(IAA,"macro","macro0"); 
		IAA.addSeparatorLeft(); 
		add(IAA,"zoom"); 
		add(IAA,"grid"); 
		add(IAA,"comment"); 
		add(IAA,"grab"); 
		add(IAA,"draw"); 
		add(IAA,"rename"); 
		add(IAA,"function"); 
		IAA.addSeparatorLeft(); 
		add(IAA,"visual"); 
		add(IAA,"replay"); 
		add(IAA,"animatebreak"); 
		add(IAA,"info"); 
		p.add(IAA); 
		IB=new IconBar(f,false); 
		for (int i=0; i<ZirkelFrame.IconNumber; i++)
			add(IB,ZirkelFrame.ObjectStrings[i]); 
		p.add(IB); 
		p.add(C=new CheckboxAction(this,Zirkel.name("iconedit.twobars"))); 
		if (Restrict)
			C.setState(Global.getParameter("restrictedicons","").indexOf("twolines")
			>=0); 
		else
			C.setState(Global.getParameter("options.fullicons",true));
		p.add(CS=new CheckboxAction(this,Zirkel.name("iconedit.showseparators"))); 
		CS.setState(Global.getParameter("iconbar.showseparators",false)); 
		add("Center",p); 
		
		p=new MyPanel(); 
		p.add(new ButtonAction(this,Zirkel.name("ok"),"OK")); 
		p.add(new ButtonAction(this,Zirkel.name("abort"),"Close"));
		addHelp(p,"iconbar"); 
		add("South",p); 
		
		pack(); 
		if (getSize().width>750) setSize(getSize().width,getSize().height);
		center(f); 
		setVisible(true); 
	}
	
	public void doAction (String o)
	{	if (o.equals("OK"))
		{	StringBuffer s=new StringBuffer(" "); 
			if (Restrict)
			{	if (C.getState()) s.append("twolines "); 
			}
			else
			{	Global.setParameter("options.fullicons",C.getState()); 
			}
			add(s,IA,"new"); 
			add(s,IA,"load"); 
			add(s,IA,"save"); 
			add(s,IA,"back"); 
			add(s,IA,"delete"); 
			add(s,IA,"undo"); 
			add(s,IA,"color","color"); 
			add(s,IA,"type","type0"); 
			add(s,IA,"thickness","thickness0"); 
			add(s,IA,"partial"); 
			add(s,IA,"plines"); 
			add(s,IA,"arrow"); 
			add(s,IA,"showname"); 
			add(s,IA,"longnames"); 
			add(s,IA,"large"); 
			add(s,IA,"bold"); 
			add(s,IA,"showvalue"); 
			add(s,IA,"edit"); 
			add(s,IA,"obtuse"); 
			add(s,IA,"solid"); 
			add(s,IAA,"hidden"); 
			add(s,IAA,"showcolor","showcolor0"); 
			add(s,IAA,"macro","macro0"); 
			add(s,IAA,"zoom"); 
			add(s,IAA,"grid"); 
			add(s,IAA,"comment"); 
			add(s,IAA,"grab"); 
			add(s,IAA,"draw"); 
			add(s,IAA,"rename"); 
			add(s,IAA,"function"); 
			add(s,IAA,"visual"); 
			add(s,IAA,"replay"); 
			add(s,IAA,"animatebreak"); 
			add(s,IAA,"info"); 
			for (int i=0; i<ZirkelFrame.IconNumber; i++)
				add(s,IB,ZirkelFrame.ObjectStrings[i]); 
			Global.setParameter(
				Restrict?"restrictedicons":"icons",s.toString());
			Global.setParameter("iconbar.showseparators",CS.getState()); 
			doclose(); 
			Abort=false; 
		}
		else super.doAction(o); 
	}
	
	public void add (StringBuffer b, IconBar ib, String s, String o)
	{	if (ib.getState(o))
		{	b.append(s); 
			b.append(" "); 
		}
	}
	
	public void add (StringBuffer b, IconBar ib, String s)
	{	add(b,ib,s,s); 
	}
	
	public void add (IconBar ib, String o, String i)
	{	ib.addToggleLeft(i); 
		if (Global.getParameter(
			Restrict?"restrictedicons":"icons","none").indexOf(" "+o)>=0)
			ib.setState(i,true); 
	}
	public void add (IconBar ib, String o)
	{	add(ib,o,o); 
	}
	
	public boolean isAbort ()
	{	return Abort; 
	}
}

