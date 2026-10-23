package rene.zirkel.dialogs;

import java.awt.*;
import rene.gui.*;
import rene.dialogs.*;

public class Sure extends Question
{	public Sure (Frame f, String question)
	{	super(f,question,Global.name("sure.title"),false);
	}
	public static boolean ask (Frame f, String prompt)
	{	Sure s=new Sure(f,prompt);
		s.center(f);
		s.setVisible(true);
		return s.yes();
	}
	public static void main (String args[])
	{	Frame f=new CloseFrame()
			{	public boolean close ()
				{	return Sure.ask(this,"Close?");
				}
			};
		f.setSize(400,400);
		f.setLocation(100,100);
		f.setVisible(true);
	}
}
