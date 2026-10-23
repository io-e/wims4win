/*
 * Created on 14.01.2006
 *
 */
package rene.zirkel.construction;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.*;

import rene.gui.*;
import rene.lister.*;
import rene.zirkel.*;
import rene.zirkel.dialogs.EditConditionals;
import rene.zirkel.dialogs.ObjectsEditDialog;
import rene.zirkel.objects.*;

public class ConstructionDisplayPanel 
	extends MyPanel 
		implements DoActionListener, ActionListener, ClipboardOwner
{	Lister V;
	Vector W;
	Construction C;
	ZirkelCanvas ZC;

	Choice Ch;
	CheckboxMenuItemAction Visible;
	boolean ShowVisible=true;
	CheckboxMenuItemAction Sort,Description,Size,Formula;
	public static String Choices[]=
		{"all","points","lines","circles","angles","expressions","other"};
	int State=0;
	PopupMenu PM;
	Button Menu;
	
	public ConstructionDisplayPanel (ZirkelCanvas zc)
	{	ZC=zc;
		C=ZC.getConstruction();
		V=new Lister();
		V.setMode(true,false,true,true);
		V.addActionListener(this);

		if (Global.Background!=null) V.setBackground(Global.Background);
		V.setFont(Global.FixedFont);
		State=Global.getParameter("constructiondisplay.state",0);
		
		setLayout(new BorderLayout());
		
		Panel north=new MyPanel();
		north.setLayout(new BorderLayout());
		Ch=new ChoiceAction(this,"choices");
		for (int i=0; i<Choices.length; i++)
		{	Ch.add(Zirkel.name("constructiondisplay."+Choices[i]));
		}
		north.add("Center",Ch);
		Ch.select(State);
		Menu=new ButtonAction(this,"?","Menu");
		north.add("East",Menu);
		
		add("North",new Panel3D(north));
		
		setListerState();
		
		makePopup();
		
		add("Center",V);
	}
	
	public void reload ()
	{	V.clear();
		C=ZC.getConstruction();
		Enumeration e=null;
		if (Global.getParameter("constructiondisplay.sort",true)) e=C.getSortedElements();
		else e=C.elements();
		W=new Vector();
		outer: while (e.hasMoreElements())
		{	ConstructionObject o=(ConstructionObject)e.nextElement();
			if (ShowVisible && o.mustHide(ZC)) continue outer;
			if (Global.getParameter("restricted",false) &&
					o.isSuperHidden()) continue outer;
			switch (State)
			{	case 0 :
					break;
				case 1 :
					if (!(o instanceof PointObject))
									continue outer;
					break;
				case 2 :
					if (!(o instanceof PrimitiveLineObject) ||
							(o instanceof FixedAngleObject))
									continue outer;
					break;
				case 3 :
					if (!(o instanceof PrimitiveCircleObject))
									continue outer;
					break;
				case 4 :
					if (!(o instanceof AngleObject || 
							o instanceof FixedAngleObject))
									continue outer;
					break;
				case 5 :
					if (!(o instanceof ExpressionObject || 
							o instanceof FunctionObject))
									continue outer;
					break;
				case 6 :
					if (o instanceof PointObject || 
							o instanceof PrimitiveLineObject ||
							o instanceof PrimitiveCircleObject ||
							o instanceof AngleObject ||
							o instanceof ExpressionObject ||
							o instanceof FunctionObject)
									continue outer;
					break;	
			}
			V.addElement(o);
			W.addElement(o);
		}
		V.showLast();
		updateDisplay();
	}
	
	public void updateDisplay ()
	{	V.updateDisplay();
	}

	public void doAction (String o) 
	{	if (o.equals("Edit"))
		{	int selected[]=V.getSelectedIndices();
			if (selected.length==0) return;
			if (selected.length==1)
			{	((ConstructionObject)W.elementAt(selected[0])).edit(ZC);
			}	
			else
			{	Vector v=new Vector();
				for (int i=0; i<selected.length; i++)
					v.addElement(W.elementAt(selected[i]));
				ObjectsEditDialog d=new ObjectsEditDialog(ZC.getFrame(),v);
				d.setVisible(true);
				ZC.validate();
			}
			ZC.repaint();
			reload();
		}
		else if (o.equals("EditConditions"))
		{	int selected[]=V.getSelectedIndices();
			if (selected.length==0) return;
			if (selected.length==1)
			{	new EditConditionals(ZC.getFrame(),
						(ConstructionObject)W.elementAt(selected[0]));
			}	
			else
			{	Vector v=new Vector();
				for (int i=0; i<selected.length; i++)
					v.addElement(W.elementAt(selected[i]));
				new EditConditionals(ZC.getFrame(),v);
			}
			ZC.repaint();
			updateDisplay();
		}
		else if (o.equals("Copy"))
		{	try
			{	ByteArrayOutputStream ba=new ByteArrayOutputStream(50000);
				PrintWriter po=new PrintWriter(
					new OutputStreamWriter(ba),true);
				V.save(po);
				po.close();
				String S=ba.toString();
				Clipboard clip=getToolkit().getSystemClipboard();
				StringSelection sel=new StringSelection(S);
				clip.setContents(sel,this);
			}
			catch (Exception e) {	}
		}
		else if (o.equals("Delete"))
		{	int selected[]=V.getSelectedIndices();
			if (selected.length==0) return;
			Vector v=new Vector();
			for (int i=0; i<selected.length; i++)
				v.addElement(W.elementAt(selected[i]));
			ZC.delete(v);
			ZC.repaint();
			ZC.reset();
			reload();
		}
		else if (o.equals("Hide"))
		{	int selected[]=V.getSelectedIndices();
			if (selected.length==0) return;
			for (int i=0; i<selected.length; i++)
			{	ConstructionObject oc=(ConstructionObject)W.elementAt(selected[i]);
				oc.setHidden(!oc.isHidden());
			}
			ZC.repaint();
			updateDisplay();
		}
		else if (o.equals("SuperHide"))
		{	int selected[]=V.getSelectedIndices();
			if (selected.length==0) return;
			for (int i=0; i<selected.length; i++)
			{	ConstructionObject oc=(ConstructionObject)W.elementAt(selected[i]);
				oc.setSuperHidden(true);
			}
			ZC.repaint();
			updateDisplay();
		}
		else if (o.equals("HighLight"))
		{	int selected[]=V.getSelectedIndices();
			if (selected.length==0) return;
			for (int i=0; i<selected.length; i++)
			{	ConstructionObject oc=(ConstructionObject)W.elementAt(selected[i]);
				oc.setStrongSelected(true);
			}
			Graphics g=ZC.getGraphics();
			if (g!=null)
			{	ZC.paint(g);
				g.dispose();
				try
				{	Thread.sleep(400);
				}
				catch (Exception e) {}
			}
			for (int i=0; i<selected.length; i++)
			{	ConstructionObject oc=(ConstructionObject)W.elementAt(selected[i]);
				oc.setStrongSelected(false);
			}
			ZC.repaint();
		}
		else if (o.equals("Menu"))
		{	displayPopup(V.L,10,10);
		}
		ZC.requestFocus();
	}

	public void itemToggleAction (String o)
	{	if (o.equals("Sort"))
		{	Sort.setState(!Sort.getState());
			itemAction("Sort",Sort.getState());
		}
		else if (o.equals("Visible"))
		{	Visible.setState(!Visible.getState());
			itemAction("Visible",Visible.getState());
		}
	}
	
	public void itemAction (String o, boolean flag)
	{	if (o.equals("Sort"))
		{	Global.setParameter("constructiondisplay.sort",Sort.getState());
			reload();
		}
		else if (o.equals("Visible"))
		{	ShowVisible=Visible.getState();
			reload();
		}
		else if (o.equals("Description"))
		{	Global.setParameter("constructiondisplay.listerstate",
				ConstructionObject.DescriptionState);
			setListerState();
			updateDisplay();
		}
		else if (o.equals("Size"))
		{	Global.setParameter("constructiondisplay.listerstate",
				ConstructionObject.SizeState);
			setListerState();
			updateDisplay();
		}
		else if (o.equals("Formula"))
		{	Global.setParameter("constructiondisplay.listerstate",
				ConstructionObject.FormulaState);
			setListerState();
			updateDisplay();
		}
		else if (flag)
		{	State=Ch.getSelectedIndex();
			Global.setParameter("constructiondisplay.state",State);
			reload();
		}
	}
	
	public void setListerState ()
	{	int state=Global.getParameter("constructiondisplay.listerstate",ConstructionObject.SizeState);
		V.setState(state);
		if (PM!=null)
		{	Description.setState(state==ConstructionObject.DescriptionState);
			Size.setState(state==ConstructionObject.SizeState);
			Formula.setState(state==ConstructionObject.FormulaState);
		}
	}

	public Dimension getPreferredSize ()
	{	return new Dimension(Global.getParameter("options.constructiondisplay.width",200),
			400);
	}

	/**
	 * React on click events for the construction list
	 */
	public void actionPerformed (ActionEvent e) 
	{	if (e.getSource()==V && (e instanceof ListerMouseEvent))
		{	ListerMouseEvent em=(ListerMouseEvent)e;
			if (em.rightMouse())
				displayPopup(em.getEvent().getComponent(),
						em.getEvent().getX(),em.getEvent().getY());
			else
			{	if (em.clickCount()>=2)
					doAction("Edit");
				else
					doAction("HighLight");
			}
		}
	}
	
	/**
	 * Display the popup menu. Create it, if necessary.
	 * @param e mouse event
	 */
	public void displayPopup (Component c, int x, int y)
	{	PM.show(c,x,y);
	}
	
	public void makePopup ()
	{	PM=new PopupMenu();
		PM.add(new MenuItemAction(this,Zirkel.name("constructiondisplay.edit"),"Edit"));
		PM.add(new MenuItemAction(this,Zirkel.name("constructiondisplay.editconditions"),"EditConditions"));
		PM.addSeparator();
		PM.add(new MenuItemAction(this,Zirkel.name("constructiondisplay.hide"),"Hide"));
		PM.add(new MenuItemAction(this,Zirkel.name("constructiondisplay.superhide"),
				"SuperHide"));
		PM.addSeparator();
		PM.add(new MenuItemAction(this,Zirkel.name("constructiondisplay.delete"),
				"Delete"));
		PM.addSeparator();
		Description=new CheckboxMenuItemAction(this,
				Zirkel.name("constructiondisplay.description"),"Description");
		Description.setState(Global.getParameter("constructiondisplay.description",false));
		PM.add(Description);
		Size=new CheckboxMenuItemAction(this,
				Zirkel.name("constructiondisplay.size"),"Size");
		Description.setState(Global.getParameter("constructiondisplay.size",true));
		PM.add(Size);
		Formula=new CheckboxMenuItemAction(this,
				Zirkel.name("constructiondisplay.formula"),"Formula");
		Description.setState(Global.getParameter("constructiondisplay.formula",false));
		PM.add(Formula);
		PM.addSeparator();
		PM.add(new MenuItemAction(this,Zirkel.name("constructiondisplay.copy"),
				"Copy"));
		V.L.add(PM);
		PM.addSeparator();
		Visible=new CheckboxMenuItemAction(this,
			Zirkel.name("constructiondisplay.visible"),"Visible");
		Visible.setState(true);
		PM.add(Visible);
		Sort=new CheckboxMenuItemAction(this,
			Zirkel.name("constructiondisplay.sorted"),"Sort");
		Sort.setState(Global.getParameter("constructiondisplay.sort",true));
		PM.add(Sort);
		setListerState();
	}
	
	/**
	 * Make sure, the last object displays.
	 */
	public void showLast ()
	{	V.showLast();
	}

	public void lostOwnership(Clipboard clipboard, Transferable contents) {
	}

	public void setListingBackground (Color c)
	{	V.setListingBackground(c);
	}
}
