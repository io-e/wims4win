/*
 * Created on 26.10.2005
 *
 */
package rene.zirkel.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Window;

import rene.gui.Global;
import rene.gui.MyLabel;
import rene.gui.Panel3D;

public class MessageWindow extends Window
{	public MessageWindow (Frame f, String s)
	{	super(f);
		setBackground(Global.ControlBackground);
		MyLabel label=new MyLabel(s);
		label.setFont(new Font("Courier",Font.BOLD,16));
		setLayout(new BorderLayout());
		add("Center",new Panel3D(label));
		pack();
		Dimension d=f.getSize();
		Point p=f.getLocation();
		setLocation(p.x+d.width/2-getSize().width/2,
			p.y+d.height/2-getSize().height/2);
	}
}

