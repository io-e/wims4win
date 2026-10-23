/*
 * Created on 22.10.2005
 *
 */
package rene.zirkel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.InputStream;

import rene.gui.Global;

class LogoWindow extends Window
implements Runnable
{	ZirkelFrame ZF;

public LogoWindow (ZirkelFrame zf)
{	super(zf);
	setSize(400,300);
	Dimension d=getSize();
	String name="zirkelframe";
	int x=Global.getParameter(name+".x",100);
	int y=Global.getParameter(name+".y",100);
	int w=Global.getParameter(name+".w",600);
	int h=Global.getParameter(name+".h",600);
	setLocation(x+w/2-d.width/2,y+h/2-d.height/2);
	loadLogo();
	setVisible(true);
	new Thread(this).start();
}

Image I;

public void loadLogo ()
{	try
	{	InputStream in=getClass().getResourceAsStream("/rene/zirkel/logowindow.gif");
		int pos=0;
		int n=in.available();
		byte b[]=new byte[200000];
		while (n>0)
		{	int k=in.read(b,pos,n);
			if (k<0) break;
			pos+=k;
			n=in.available();
		}
		in.close();
		I=Toolkit.getDefaultToolkit().createImage(b,0,pos);
		MediaTracker T=new MediaTracker(this);
		T.addImage(I,0);
		T.waitForAll();
	}
	catch (Exception e)
	{	setVisible(false);
		I=null;
	}
}

public void paint (Graphics g)	
{	if (I==null) return; 
	g.drawImage(I,0,0,this);
	Graphics2D G=(Graphics2D)g;
	G.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
		RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	g.setFont(new Font("Dialog",Font.BOLD,14));
	String s=Zirkel.name("version")+" "+Zirkel.name("program.version");
	g.setColor(Color.black);
	g.drawString(s,45,270);
}

public void run ()
{	try
	{	Thread.sleep(5000);
	} catch(Exception e){}
	setVisible(false);
	dispose();
}
}


