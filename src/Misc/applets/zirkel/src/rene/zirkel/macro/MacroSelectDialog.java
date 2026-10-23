/*
 * Created on 26.10.2005
 *
 */
package rene.zirkel.macro;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import rene.gui.*;
import rene.lister.*;
import rene.zirkel.*;
import rene.zirkel.dialogs.*;

/**
 * @author Rene
 * Select a macro or macros from the list of macros.
 */
public class MacroSelectDialog extends HelpCloseDialog 
	implements ActionListener
{	Vector Chosen;
	Macro O=null;
	Lister L;
	Frame F;
	MacroMenu MM; // MacroMenu
	boolean Multiple;
	TextArea Comment;
	
	/**
	* @param v A vector of MacroItems to select from.
	*/
	public MacroSelectDialog (Frame f, MacroMenu mm, boolean multiple)
	{	super(f,Zirkel.name("select.title","Select Object"),true);
		
		MM=mm;
		if (!multiple)
		{	MacroMenu mmf=MM.findWithFullName(Global.getParameter("select.olddir",""));
			if (mmf!=null) MM=mmf;
		}
		
		F=f;
		Multiple=multiple;
		
		L=new Lister();
		add("Center",L);
		list();
		
		L.setMode(multiple,multiple,true,false);
		L.addActionListener(this); 
		
		Panel south=new MyPanel();
		south.setLayout(new BorderLayout());
		
		south.add("Center",Comment=new TextArea("",5,40,TextArea.SCROLLBARS_VERTICAL_ONLY));
		Comment.setEditable(false);
		
		Panel p=new MyPanel();
		p.add(new ButtonAction(this,Zirkel.name("select.ok"),"OK")); 
		if (multiple)
			p.add(new ButtonAction(this,Zirkel.name("select.all"),"All"));
		p.add(new ButtonAction(this,Zirkel.name("select.cancel"),"Close")); 
		addHelp(p,"selectmacro");
		south.add("South",p);
		
		add("South",new Panel3D(south));
		
		pack();
		center(f);
		Chosen=new Vector();
		setSize("macroselect");
	}
	
	public void actionPerformed (ActionEvent e)
	{	int offset=(MM.Father!=null)?1:0;
		if (e.getSource()==L && ((ListerMouseEvent)e).clickCount()==2)
		{	int i=L.getSelectedIndex();
			if (i<0) return;
			if (i<offset)
			{	MM=MM.Father;
				list();
			}
			else
			{	Object o=MM.getV().elementAt(i-offset);
				if (o instanceof MacroMenu)
				{	MM=(MacroMenu)o;
					list();
				}
				else if (!Multiple)
					doAction("OK");
			}
		}
		if (e.getSource()==L && ((ListerMouseEvent)e).clickCount()==1)
		{	int i=L.getSelectedIndex();
			if (i<0) return;
			if (i>=offset)
			{	Object o=MM.getV().elementAt(i-offset);
				if (!(o instanceof MacroMenu))
				{	MacroItem mi=(MacroItem)o;	
					Comment.setText(mi.M.getComment());
				}
				else
				{	Comment.setText("");
				}
			}
		}
		else super.actionPerformed(e);
	}
	
	public void doAction (String o)
	{	noteSize("macroselect");
		int offset=(MM.Father!=null)?1:0;
		if (o.equals("OK"))
		{	Chosen=new Vector();
			int s[]=L.getSelectedIndices();
			for (int i=0; i<s.length; i++)
			{	if (s[i]<offset) continue;
				Object sel=MM.getV().elementAt(s[i]-offset);
				if (sel instanceof MacroItem)
					Chosen.addElement(sel);
				else
					add(Chosen,(MacroMenu)sel);
			}
			if (Chosen.size()>0)
			{	doclose();
				Global.setParameter("select.olddir",MM.getFullName());
			}
		}
		else if (o.equals("All"))
		{	for (int i=0; i<MM.getV().size(); i++)
			{	L.select(i+offset);
			}
		}
		else if (o.equals("Comment"))
		{	int i=L.getSelectedIndex();
			if (i<offset) return;
			Object sel=MM.getV().elementAt(i-offset);
			if (sel instanceof MacroItem)
				new CommentDialog(F,
						((MacroItem)sel).M.getComment(),
						Zirkel.name("select.comment.title"),
						true);
		}
		else super.doAction(o);
	}
	
	/**
	 * @return First selected macro.
	 */
	public Macro getMacro ()
	{	if (Chosen.size()>0) return ((MacroItem)Chosen.elementAt(0)).M;
		return null;
	}
	
	/**
	 * @return List of selected macros.
	 */
	public Vector getMacros ()
	{	return Chosen;
	}
	
	public void list ()
	{	L.clear();
		if (MM.Father!=null) L.addElement(new StringElement("..",Color.black));
		Enumeration e=MM.getV().elements();
		while (e.hasMoreElements())
		{	Object o=e.nextElement();
			if (o instanceof MacroMenu)
			{	MacroMenu mm=(MacroMenu)o;
				L.addElement(new StringElement("/"+mm.getName(),Color.blue.darker()));
			}
			else
			{	MacroItem mi=(MacroItem)o;
				if (mi.M.isProtected())
					L.addElement(new StringElement(mi.Name,Color.red.darker()));
				else
					L.addElement(new StringElement(mi.Name,Color.green.darker()));
			}
		}
		L.updateDisplay();
	}
	
	public void add (Vector v, MacroMenu mm)
	{	Enumeration e=mm.getV().elements();
		while (e.hasMoreElements())
		{	Object o=e.nextElement();
			if (o instanceof MacroMenu) add(v,(MacroMenu)o);
			else v.addElement((MacroItem)o);
		}
	}
	
}

