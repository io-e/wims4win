/*
 * Created on 07.01.2006
 *
 */
package rene.zirkel.macro;

import java.awt.*;
import java.util.*;
import rene.gui.*;

public class MacroMenu 
{	Vector V; // Vector of MacroMenu or MacroItem
	Menu FatherMenu; // awt.Menu containing this MacroMenu
	String Name; // Name of this MacroMenu (as displayed in the Menu)
	MacroMenu Father;

	public MacroMenu (Menu o, String name, MacroMenu father)
	{	V=new Vector();
		FatherMenu=o;
		Name=name;
		Father=father;
	}
	
	/**
	 * Add a macro this menu. This is a recursive function!
	 * Macros may have a macro path with them.
	 * @param m Macro to add
	 * @param name Name for the macro (inclusive path)
	 * @return MyMenuItem for the macro
	 */
	public MacroItem add (Macro m, String name)
	{	String s=name;
		int n=s.indexOf("/");
		if (n<0)
		{	MyMenuItem item;
			if (FatherMenu==null)
				item=null;
			else 
			{	if (m.isProtected())
					item=new MyMenuItem("- "+name+" -");
				else 
					item=new MyMenuItem(name);
				FatherMenu.add(item);
			}
			MacroItem mi=new MacroItem(m,item);
			mi.Name=name;
			V.addElement(mi);
			return mi;
		}
		String menu=s.substring(0,n);
		s=s.substring(n+1);
		Enumeration e=V.elements();
		while (e.hasMoreElements())
		{	Object o=e.nextElement();
			if (o instanceof MacroMenu 
				&& ((MacroMenu)o).getName().equals(menu))
			{	return ((MacroMenu)o).add(m,s);
			}
		}
		MyMenu mm;
		if (FatherMenu==null)
			mm=null;
		else
		{	mm=new MyMenu(menu);
			FatherMenu.add(mm);
		}
		MacroMenu macm=new MacroMenu(mm,menu,this);
		V.addElement(macm);
		return macm.add(m,s);
	}
	
	public boolean remove (MacroItem item)
	{	Enumeration e=V.elements();
		while (e.hasMoreElements())
		{	Object o=e.nextElement();
			if (o instanceof MacroMenu)
			{	boolean res=((MacroMenu)o).remove(item);
				if (res)
				{	if (((MacroMenu)o).isEmpty())
					{	V.removeElement(o);
						if (FatherMenu!=null)
							FatherMenu.remove(((MacroMenu)o).FatherMenu);
					}
					return true;
				}
			}
			else
			{	if (((MacroItem)o).M==item.M)
				{	V.removeElement(o);
					if (FatherMenu!=null)
						FatherMenu.remove(item.I);
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean replace (MacroItem item, MacroItem newitem)
	{	Enumeration e=V.elements();
		while (e.hasMoreElements())
		{	Object o=e.nextElement();
			if (o instanceof MacroMenu)
			{	boolean res=((MacroMenu)o).replace(item,newitem);
				if (res) break;
			}
			else
			{	if (((MacroItem)o).M==item.M)
				{	((MacroItem)o).M=newitem.M;
					return true;
				}
			}
		}
		return false;
	}
	
	
	public String getName ()
	{	return Name;
	}
	
	public boolean isEmpty ()
	{	return V.size()==0;
	}

	public Vector getV ()
	{	return V;
	}
	
	public boolean hasSubmenus ()
	{	Enumeration e=V.elements();
		while (e.hasMoreElements())
		{	Object o=e.nextElement();
			if (o instanceof MacroMenu) return true;
		}
		return false;
	}

	public String getFullName ()
	{	String s=Name;
		MacroMenu m=this;
		while (m.Father!=null)
		{	m=m.Father;
			s=m.Name+"/"+s;
		}
		return s;
	}
	
	public MacroMenu findWithFullName (String name)
	{	int n=name.indexOf("/");
		if (n<0)
		{	if (name.equals(Name)) return this;
			else return null;
		}
		if(!name.substring(0,n).equals(Name)) return null;	
		name=name.substring(n+1);
		Enumeration e=V.elements();
		while (e.hasMoreElements())
		{	Object o=e.nextElement();
			if (o instanceof MacroMenu)
			{	MacroMenu m=(MacroMenu)o;
				MacroMenu res=m.findWithFullName(name);
				if (res!=null) return res;
			}
		}
		return null;
	}
}
