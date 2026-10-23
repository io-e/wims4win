/*
 * Created on 22.10.2005
 *
 */
package rene.zirkel.dialogs;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Panel;

import rene.gui.ButtonAction;
import rene.gui.CloseDialog;
import rene.gui.Global;
import rene.gui.IntField;
import rene.gui.MyLabel;
import rene.gui.MyPanel;
import rene.gui.Panel3D;
import rene.zirkel.Zirkel;

public class DigitsDialog extends CloseDialog
{	IntField Edit,Lengths,Angles;
	
	public DigitsDialog (Frame f)
	{	super(f,Zirkel.name("digits.title"),true); 
		setLayout(new BorderLayout()); 
		
		Panel c=new MyPanel(); 
		c.setLayout(new GridLayout(0,2)); 
		
		c.add(new MyLabel(Zirkel.name("digits.edit"))); 
		c.add(Edit=new IntField(this,"Edit",
			Global.getParameter("digits.edit",5),20)); 
		
		c.add(new MyLabel(Zirkel.name("digits.lengths"))); 
		c.add(Lengths=new IntField(this,"Lengths",
			Global.getParameter("digits.lengths",5))); 
		
		c.add(new MyLabel(Zirkel.name("digits.angles"))); 
		c.add(Angles=new IntField(this,"Angles",
			Global.getParameter("digits.angles",0))); 
		
		add("Center",new Panel3D(c)); 
		
		Panel s=new MyPanel(); 
		s.add(new ButtonAction(this,Zirkel.name("ok"),"OK")); 
		s.add(new ButtonAction(this,Zirkel.name("abort"),"Close")); 
		add("South",new Panel3D(s)); 
		
		pack(); 
		center(f); 
		setVisible(true); 
	}
	
	public void doAction (String o)
	{	if (o.equals("OK"))
		{	Global.setParameter("digits.edit",Edit.value(2,20)); 
			Global.setParameter("digits.lengths",Lengths.value(0,10)); 
			Global.setParameter("digits.angles",Angles.value(0,10)); 
			doclose(); 
		}
		else super.doAction(o); 
	}
}

