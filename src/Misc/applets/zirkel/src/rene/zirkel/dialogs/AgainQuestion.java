package rene.zirkel.dialogs;

// file: ZirkelFrame.java

import java.awt.*;
import java.awt.event.*;

import rene.gui.*;

public class AgainQuestion extends CloseDialog 
    implements ActionListener
{	public int Result;
	Frame F;
	public static int NO=0,YES=1;
	Checkbox Again;
	public AgainQuestion (Frame f, String c, String title)
	{	super(f,title,true);
		F=f;
		Panel main=new MyPanel();
		main.setLayout(new GridLayout(0,1));
		Panel pc=new MyPanel();
		FlowLayout fl=new FlowLayout();
		pc.setLayout(fl);
		fl.setAlignment(FlowLayout.CENTER);
		pc.add(new MyLabel(" "+c+" "));
		main.add(pc);
		Panel pd=new MyPanel();
		pd.add(Again=new CheckboxAction(this,Global.name("question.again")));
		Again.setState(true);
		main.add(pd);
		add("Center",main);
		Panel p=new MyPanel();
		p.setLayout(new FlowLayout(FlowLayout.RIGHT));
		p.add(new ButtonAction(this,Global.name("yes"),"Yes"));
		p.add(new ButtonAction(this,Global.name("no"),"No"));
		add("South",p);
		pack();
	}
	public void doAction (String o)
	{	if (o.equals("Yes"))
  		{	tell(YES);
  		}
  		else if (o.equals("No"))
  		{	tell(NO);
  		}
  	}
  	/**
  	Needs to be overriden for modal usage. Should dispose the dialog.
  	*/
	public void tell (int f)
	{	Result=f;
		doclose();
	}
	/**
	@return if the user pressed yes.
	*/
	public boolean yes ()
	{	return Result==YES;
	}
	public int getResult ()
	{	return Result;
	}
	public boolean again ()
	{	return Again.getState();
	}
}

