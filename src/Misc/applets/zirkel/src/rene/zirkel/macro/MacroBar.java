/*
 * Created on 02.07.2007
 *
 */
package rene.zirkel.macro;

import java.awt.*;
import java.util.*;

import rene.gui.*;

class MacroBarElement
{	public MacroItem M;
	public String Name;
	public MacroBarElement (MacroItem m, String name)
	{	M=m; Name=name;
	}
}

public class MacroBar extends IconBar 
{	Vector V=new Vector();
	
	public MacroBar (Frame f)
	{	super(f,false);
		Resource="/icons/";
	}
	
	public void update (Vector macros)
	{	removeAll();
		V.removeAllElements();
		Enumeration e=macros.elements();
		while (e.hasMoreElements())
		{	MacroItem m=(MacroItem)e.nextElement();
			String name=m.Name;
			if (name!=null && name.endsWith(")") &&
				(!m.M.isProtected() || Global.getParameter("defaultmacrosinbar",true)))
			{	addToggleLeft(name);
				V.addElement(new MacroBarElement(m,name));
			}
		}
		doLayout();
		forceRepaint();
	}
	
	public void deselectAll ()
	{	Enumeration e=V.elements();
		while (e.hasMoreElements())
		{	MacroBarElement me=(MacroBarElement)e.nextElement();
			setState(me.Name,false);
		}
	}

	public Macro find (String o)
	{	Enumeration e=V.elements();
		while (e.hasMoreElements())
		{	MacroBarElement me=(MacroBarElement)e.nextElement();
			if (me.Name.equals(o))
			{	return me.M.M;
			}
		}
		return null;
	}
	
	public void select (Macro m)
	{	Enumeration e=V.elements();
		while (e.hasMoreElements())
		{	MacroBarElement me=(MacroBarElement)e.nextElement();
			if (me.M.M==m)
			{	setState(me.Name,true);
			}
		}
	}

	public String getHelp (String name)
	{	return name;
	}
}
