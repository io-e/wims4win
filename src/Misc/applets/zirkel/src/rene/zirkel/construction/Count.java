package rene.zirkel.construction;

import java.util.*;

/**
This is used to generate unique numbers for construction objects.
Instances will be static in their classes. There is also an
alternative count, which can be used for macro generation.
*/

public class Count
{	static Vector Counts=new Vector();
	int N,BackupN;
	boolean Fixed=false;
	public Count ()
	{	reset();
		Counts.addElement(this);
	}
	public synchronized int next ()
	{	if (Fixed) return 0;
		N++; return N;
	}
	public synchronized void reset ()
	{	N=0;
	}
	public synchronized void setAlternate (boolean flag)
	{	if (flag) { BackupN=N; N=0; }
		else N=BackupN;
	}
	static synchronized public void resetAll ()
	{	Enumeration e=Counts.elements();
		while (e.hasMoreElements())
			((Count)e.nextElement()).reset();
	}
	static synchronized public void setAllAlternate (boolean flag)
	{	Enumeration e=Counts.elements();
		while (e.hasMoreElements())
			((Count)e.nextElement()).setAlternate(flag);
	}
	public synchronized void fix (boolean flag)
	{	Fixed=flag;
	}
	static synchronized public void fixAll (boolean flag)
	{	Enumeration e=Counts.elements();
		while (e.hasMoreElements())
			((Count)e.nextElement()).fix(flag);
	}
}
