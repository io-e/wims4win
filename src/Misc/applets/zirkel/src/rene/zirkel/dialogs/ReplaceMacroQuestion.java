/*
 * Created on 26.10.2005
 *
 */
package rene.zirkel.dialogs;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Panel;

import rene.gui.ButtonAction;
import rene.gui.CloseDialog;
import rene.gui.MyLabel;
import rene.gui.MyPanel;
import rene.zirkel.Zirkel;
import rene.zirkel.macro.Macro;


/**
 * @author Rene
 * Dialog to ask the user if all macros should be replaced from
 * the loaded file.
 */

public class ReplaceMacroQuestion extends CloseDialog 
{	public int Result=NO;
	public static int NO=0,YES=1,ALL=-1;
	public ReplaceMacroQuestion (Frame f, Macro m)
	{	super(f,Zirkel.name("macro.replace.title"),true);
		Panel pc=new MyPanel();
		FlowLayout fl=new FlowLayout();
		pc.setLayout(fl);
		fl.setAlignment(FlowLayout.CENTER);
		pc.add(new MyLabel(Zirkel.name("macro.replace")
			+" "+m.getName()));
		add("Center",pc);
		Panel p=new MyPanel();
		p.setLayout(new FlowLayout(FlowLayout.RIGHT));
		p.add(new ButtonAction(this,Zirkel.name("yes"),"Yes"));
		p.add(new ButtonAction(this,Zirkel.name("no"),"No"));
		p.add(new ButtonAction(this,
			Zirkel.name("macro.replace.all"),"All"));
		add("South",p);
		pack();
	}
	
	public void doAction (String o)
	{	if (o.equals("Yes"))
  		{	tell(this,YES);
  		}
  		else if (o.equals("No"))
  		{	tell(this,NO);
  		}
  		else if (o.equals("All"))
  		{	tell(this,ALL);
  		}
  		else super.doAction(o);
  	}
  	
	public void tell (ReplaceMacroQuestion q, int f)
	{	Result=f;
		doclose();
	}
	
	public boolean isNo ()
	{	return Result==NO;
	}
	
	public boolean isAll ()
	{	return Result==ALL;
	}
}


