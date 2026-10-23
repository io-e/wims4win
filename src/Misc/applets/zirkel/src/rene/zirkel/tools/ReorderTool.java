package rene.zirkel.tools;

// file: Hider.java

import java.awt.*;
import java.awt.event.*;

import rene.gui.*;
import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelCanvas;
import rene.zirkel.constructors.*;
import rene.zirkel.objects.ConstructionObject;

class ReorderDialog extends CloseDialog
{	String Name="";
	boolean Abort=true;
	TextField Input;
	
	public ReorderDialog (ZirkelCanvas zc, ConstructionObject o)
	{	super(zc.getFrame(),Zirkel.name("reorder.title"),true);
		setLayout(new BorderLayout());
		Panel north=new MyPanel();
		north.setLayout(new GridLayout(1,0));
		north.add(new MyLabel(o.getName()+" : "+Zirkel.name("reorder.message")));
		ConstructionObject ol=zc.getConstruction().lastDep(o);
		String s="";
		if (ol!=null) s=ol.getName();
		north.add(Input=new TextFieldAction(this,"Reorder",s));
		add("North",new Panel3D(north));
		Panel south=new MyPanel();
		south.add(new ButtonAction(this,Zirkel.name("ok"),"OK"));
		south.add(new ButtonAction(this,Zirkel.name("abort"),"Close"));
		add("South",south);
		pack();
		center(zc.getFrame());
		setVisible(true);
	}
	public void doAction (String o)
	{	if (o.equals("OK"))
		{	Abort=false;
			Name=Input.getText();
			doclose();
		}
		else super.doAction(o);
	}
	public String getResult ()
	{	return Name;
	}
	public boolean isAborted ()
	{	return Abort;
	}
}

public class ReorderTool extends ObjectConstructor
{	public void mousePressed (MouseEvent e, ZirkelCanvas zc)
	{	double x=zc.x(e.getX()),y=zc.y(e.getY());
		ConstructionObject o=zc.selectObject(e.getX(),e.getY());
		if (o==null) return;
		ReorderDialog d=new ReorderDialog(zc,o);
		if (!d.isAborted())
		{	String name=d.getResult();
			if (!name.equals(""))
			{	ConstructionObject u=zc.getConstruction().find(name);
				if (u==null)
				{	zc.warning(Zirkel.name("reorder.notfound"));
					return;
				}
				if (!zc.getConstruction().reorder(o,u))
					zc.warning(Zirkel.name("reorder.warning"));
			}
			else
				if (!zc.getConstruction().reorder(o,null))
					zc.warning(Zirkel.name("reorder.warning"));
		}
		zc.repaint();
	}
	public void mouseMoved (MouseEvent e, ZirkelCanvas zc, boolean simple)
	{	zc.indicateObjects(e.getX(),e.getY());
	}

	public void showStatus (ZirkelCanvas zc)
	{	zc.showStatus(
			Zirkel.name("message.reorder","Reorder: Select an object!"));
	}
	public boolean useSmartBoard ()
	{	return false;
	}
}
