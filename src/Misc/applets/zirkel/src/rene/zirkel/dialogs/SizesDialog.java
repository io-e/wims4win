/*
 * Created on 05.04.2006
 *
 */
package rene.zirkel.dialogs;

import java.awt.*;

import rene.gui.*;
import rene.zirkel.*;

public class SizesDialog extends HelpCloseDialog 
{	ZirkelFrame ZF;
	TextField Pointsize,Linewidth,Arrowsize,Selectionsize,Fontsize;
	
	public SizesDialog (ZirkelFrame zf)
	{	super(zf,Zirkel.name("sizesdialog.title"),true);
		ZF=zf;
		setLayout(new BorderLayout());
		
		Panel center=new MyPanel();
		center.setLayout(new GridLayout(0,2));
		
		Pointsize=addfield(center,"minpointsize.prompt","minpointsize",3);
		Linewidth=addfield(center,"minlinesize.prompt","minlinesize",1);
		Fontsize=addfield(center,"minfontsize.prompt","minfontsize",12);
		Selectionsize=addfield(center,"selectionsize.prompt","selectionsize",1.5);
		Arrowsize=addfield(center,"arrowsize.prompt","arrowsize",15);
		
		add("Center",new Panel3D(center));
		
		Panel p=new MyPanel();
		Button b=new ButtonAction(this,Zirkel.name("edit.ok"),"OK");
		p.add(b);
		b=new ButtonAction(this,Zirkel.name("edit.cancel"),"Close");
		p.add(b);
		b.addActionListener(this);
		addHelp(p,"sizes");
		add("South",new Panel3D(p));
		
		center();
		pack();
		setVisible(true);
	}
	
	public void doAction (String o)
	{	if (o.equals("OK"))
		{	set(Pointsize,"minpointsize",0.5,10);
			set(Linewidth,"minlinesize",0.5,3);
			set(Fontsize,"minfontsize",2,30);
			set(Arrowsize,"arrowsize",5,50);
			set(Selectionsize,"selectionsize",0.5,5);
			doclose();
		}
		else super.doAction(o);
	}

	public TextField addfield (Panel p, String tag, String deftag, double def)
	{	p.add(new MyLabel(Global.name(tag)));
		TextField t=new MyTextField(""+Global.getParameter(deftag,def),20);
		p.add(t);
		return t;
	}
	
	public void set (TextField t, String tag, double min, double max)
	{	try
		{	double x=new Double(t.getText()).doubleValue();
			if (x<min) x=min;
			if (x>max) x=max;
			Global.setParameter(tag,x);
		}
		catch (Exception e)
		{}
	}
}
