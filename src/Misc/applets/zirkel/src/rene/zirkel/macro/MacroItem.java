package rene.zirkel.macro;

// file: ZirkelCanvas.java

import java.awt.*;

import rene.util.sort.SortObject;

public class MacroItem
	implements SortObject
{	public MenuItem I;
	public Macro M;
	public String Name;
	public MacroItem (Macro m, MenuItem i)
	{	M=m; I=i;
	}
	public int compare (SortObject o) 
	{	MacroItem mio=(MacroItem)o;
		return -mio.M.Name.compareTo(M.Name);
	}
}
