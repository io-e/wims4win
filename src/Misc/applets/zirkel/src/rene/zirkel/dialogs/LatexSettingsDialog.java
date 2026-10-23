/*
 * Created on 30.3.2006
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
import rene.gui.MyPanel;
import rene.gui.Panel3D;
import rene.zirkel.Zirkel;

public class LatexSettingsDialog extends HelpCloseDialog
{
	Checkbox BoundingBox,LatexInput,DoubleDollar,Dollar,NoDollar,FullPath; 
	
	public LatexSettingsDialog (Frame frame)
	{	super(frame,Zirkel.name("latexsettings.title"),true); 
		setLayout(new BorderLayout()); 
		
		Panel north=new MyPanel(); 
		north.setLayout(new GridLayout(0,1)); 
		
		BoundingBox=addcheck(north,"latexsettings.boundingbox"); 
		BoundingBox.setState(Global.getParameter("options.boundingbox",true)); 
		
		LatexInput=addcheck(north,"latexsettings.latexinput"); 
		LatexInput.setState(Global.getParameter("options.latexinput",true)); 
		
		DoubleDollar=addcheck(north,"latexsettings.doubledollar"); 
		DoubleDollar.setState(Global.getParameter("options.doubledollar",true)); 
		
		Dollar=addcheck(north,"latexsettings.dollar"); 
		Dollar.setState(Global.getParameter("options.dollar",true)); 

		NoDollar=addcheck(north,"latexsettings.nodollar"); 
		NoDollar.setState(Global.getParameter("options.nodollar",false)); 
		
		FullPath=addcheck(north,"latexsettings.fullpath"); 
		FullPath.setState(Global.getParameter("options.fullpath",true)); 
		
		add("North",new Panel3D(north)); 
		
		Panel south=new MyPanel(); 
		
		south.add(new ButtonAction(this,Zirkel.name("ok"),"OK")); 
		south.add(new ButtonAction(this,Zirkel.name("abort"),"Close")); 
		addHelp(south,"exportsettings");
		
		add("South",new Panel3D(south)); 
		
		pack(); 
		center(frame); 
		setVisible(true); 
	}
	
	public void doAction (String s)
	{	if (s.equals("OK"))
		{	Global.setParameter("options.boundingbox",BoundingBox.getState()); 
			Global.setParameter("options.doubledollar",DoubleDollar.getState()); 
			Global.setParameter("options.dollar",Dollar.getState()); 
			Global.setParameter("options.nodollar",NoDollar.getState()); 
			Global.setParameter("options.fullpath",FullPath.getState()); 
			doclose(); 
		}
		else super.doAction(s); 
	}
	
	public Checkbox addcheck (Panel p, String name)
	{	Checkbox c=new CheckboxAction(this,Zirkel.name(name),name); 
		p.add(c); 
		return c; 
	}
}



