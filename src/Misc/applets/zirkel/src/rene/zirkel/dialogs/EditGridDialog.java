/*
 * Created on 26.10.2005
 *
 */
package rene.zirkel.dialogs;

import java.awt.*;

import rene.gui.*;
import rene.zirkel.Zirkel;
import rene.zirkel.objects.*;

/**
 * @author Rene
 * Dialog to select objects, when the selection with the mouse
 * was not unique.
 */
public class EditGridDialog extends HelpCloseDialog
{	IconBar ColorIB,ThicknessIB,StyleIB;	
	
	/**
	@param v A vector of ConstructionObjects to select from.
	*/
	public EditGridDialog (Frame f)
	{	super(f,Zirkel.name("ccordinates.title"),true);
	
		Panel center=new MyPanel();
		center.setLayout(new BorderLayout());
		
		add("Center",new Panel3D(center));
		
		Panel P=new Panel();
		P.setLayout(new GridLayout(0,2));
		
		ColorIB=new IconBar(f);
		ColorIB.addToggleGroupLeft("color",6);
		ColorIB.toggle("color",Global.getParameter("grid.colorindex",0));
		P.add(new MyLabel("")); P.add(ColorIB);

		ThicknessIB=new IconBar(f);
		ThicknessIB.addToggleGroupLeft("thickness",4);
		ThicknessIB.toggle("thickness",Global.getParameter("grid.thickness",ConstructionObject.THIN));
		P.add(new MyLabel("")); P.add(ThicknessIB);
		
		StyleIB=new IconBar(f);
		StyleIB.addOnOffLeft("showname");
		StyleIB.setState("showname",Global.getParameter("grid.labels",true));
		StyleIB.addOnOffLeft("bold");
		StyleIB.setState("bold",Global.getParameter("grid.bold",false));
		StyleIB.addOnOffLeft("large");
		StyleIB.setState("large",Global.getParameter("grid.large",false));
		P.add(new MyLabel("")); P.add(StyleIB);
		
		center.add("South",P);
	
		Panel p=new MyPanel();
		p.add(new ButtonAction(this,Zirkel.name("ok"),"OK"));
		p.add(new ButtonAction(this,Zirkel.name("cancel"),"Close"));
		addHelp(p,"grid");
		add("South",new Panel3D(p));
		
		pack();
		center(f);
		setVisible(true);
	}
	
	public void doAction (String o)
	{	Aborted=true;
		if (o.equals("OK"))
		{	Aborted=false;
			Global.setParameter("grid.colorindex",ColorIB.getToggleState("color"));
			Global.setParameter("grid.thickness",ThicknessIB.getToggleState("thickness"));
			Global.setParameter("grid.labels",StyleIB.getState("showname"));
			Global.setParameter("grid.bold",StyleIB.getState("bold"));
			Global.setParameter("grid.large",StyleIB.getState("large"));
			doclose();
		}
		else super.doAction(o);
	}
	
}


