/*
 * Created on 24.05.2007
 *
 */
package rene.zirkel.dialogs;

import java.awt.*;

import rene.gui.CloseDialog;
import rene.zirkel.help.Info;

public class HelpCloseDialog extends CloseDialog
{	Frame F;
	public HelpCloseDialog (Frame f, String s, boolean modal)
	{	super(f,s,modal);
		F=f;
	}
	public void showHelp ()
	{	Info.Subject=Subject;
		Info id=new Info(F,true);
	}
}
