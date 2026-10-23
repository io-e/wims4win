/*
 * Created on 26.10.2005
 *
 */
package rene.zirkel.dialogs;

import java.awt.*;
import java.awt.event.*;

import rene.gui.*;
import rene.util.*;
import rene.lister.*;
import rene.zirkel.Zirkel;
import rene.zirkel.objects.*;

/**
 * @author Rene
 * Dialog to select objects, when the selection with the mouse
 * was not unique.
 */
public class SelectDialog extends HelpCloseDialog implements ActionListener
{	MyVector V;
	ConstructionObject O=null;
	Lister L;
	
	/**
	@param v A vector of ConstructionObjects to select from.
	*/
	public SelectDialog (Frame f, MyVector v)
	{	super(f,Zirkel.name("select.title","Select Object"),true);
		V=v;
		add("Center",L=new Lister());
		L.setMode(false,false,false,false);
		for (int i=0; i<v.size(); i++)
		{	ConstructionObject o=(ConstructionObject)v.elementAt(i);
			if (o instanceof PointObject)
				L.addElement(o.getName(),Color.black);
			else if (o instanceof PrimitiveLineObject)
				L.addElement(o.getName(),Color.red.darker());
			else if (o instanceof PrimitiveCircleObject)
				L.addElement(o.getName(),Color.blue.darker());			
			else L.addElement(o.getName(),Color.green.darker());
		}
		L.select(0);
		L.addActionListener(this);
		L.updateDisplay();
		Panel p=new MyPanel();
		p.add(new ButtonAction(this,Zirkel.name("select.ok"),"OK"));
		p.add(new ButtonAction(this,Zirkel.name("select.cancel"),"Close"));
		addHelp(p,"mouse");
		add("South",new Panel3D(p));
		pack();
		center(f);
		setVisible(true);
	}
	
	public void actionPerformed (ActionEvent e)
	{	if (e.getSource()==L) // List double clicked!
		{	doAction("OK");
		}
		else super.actionPerformed(e);
	}
	
	public void doAction (String o)
	{	Aborted=true;
		if (o.equals("OK"))
		{	int i=L.getSelectedIndex();
			if (i>=0) O=(ConstructionObject)V.elementAt(i);
			Aborted=false;
			doclose();
		}
		else super.doAction(o);
	}
	
	public ConstructionObject getObject ()
	{	return O;
	}
}


