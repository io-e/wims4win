/*
 * Created on 26.10.2005
 *
 */
package rene.zirkel;

import java.awt.event.MouseEvent;

/**
 * @author Rene
 * A thread for reporting drags to the main program, avoiding
 * to many drag messages, so that the program seems to hang.
 */
class DragThread extends Thread
{	MouseEvent EWaiting=null;
	ZirkelCanvas ZC;
	Object Ready=new Object();
	boolean Working=false;
	
	public DragThread (ZirkelCanvas zc)
	{	ZC=zc;
		start();
	}
	
	public void run ()
	{	MouseEvent e;
		while (true)
		{	if (EWaiting==null)
			{	try
				{	synchronized (this)
					{	wait();
					}
				}
				catch (Exception ex) {}
			}
			if (EWaiting!=null)
			{	synchronized (this)
				{	e=EWaiting;
					EWaiting=null;
				}
				Working=true;
				ZC.doMouseDragged(e);
				try
				{	sleep(0);
				}
				catch (Exception ex) {}
				Working=false;
				synchronized (Ready)
				{	Ready.notify();
				}
			}
		}
	}
	
	public synchronized void mouseDragged (MouseEvent e)
	{	EWaiting=e;
		notify();
	}
	
	public void waitReady ()
	{	if (!Working) return;
		synchronized (Ready)
		{	try
			{	Thread.currentThread().wait(1000);
			}
			catch (Exception ex) {}
		}
	}
}


