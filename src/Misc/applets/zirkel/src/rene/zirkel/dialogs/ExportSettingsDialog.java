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
import rene.gui.MyPanel;
import rene.gui.Panel3D;
import rene.zirkel.Zirkel;

public class ExportSettingsDialog extends HelpCloseDialog
{
	Checkbox BitmapBackground,MinPointSize,MinFontSize; 
	
	public ExportSettingsDialog (Frame frame)
	{	super(frame,Zirkel.name("menu.options.export"),true); 
		setLayout(new BorderLayout()); 
		
		Panel north=new MyPanel(); 
		north.setLayout(new GridLayout(0,1)); 
		
		MinPointSize=addcheck(north,"menu.settings.minpointsize"); 
		MinPointSize.setState(Global.getParameter("options.minpointsize",false)); 
		
		MinFontSize=addcheck(north,"menu.settings.minfontsize"); 
		MinFontSize.setState(Global.getParameter("options.minfontsize",false)); 
		
		BitmapBackground=addcheck(north,"menu.settings.bitmapbackground"); 
		BitmapBackground.setState(Global.getParameter("options.bitmapbackground",false)); 
		
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
		{	Global.setParameter("options.minpointsize",MinPointSize.getState()); 
			Global.setParameter("options.minfontsize",MinFontSize.getState()); 
			Global.setParameter("options.bitmapbackground",BitmapBackground.getState()); 
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


