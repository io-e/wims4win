/*
$Id: ErrorDialog.java,v 1.3 2003/02/18 11:48:46 sander Exp $
*/


/*
Copyright (C) 2001-2002 Mainline Project (I3S - ESSI - CNRS -UNSA)

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

For further information on the GNU Lesser General Public License,
see: http://www.gnu.org/copyleft/lesser.html
For further information on this library, contact: mainline@essi.fr
*/


package fr.ove.errordialog;

import java.awt.*;
import java.awt.event.*;

/**
* A dialog box for error messages.<BR>
* The error messages must take the form of a <CODE>java.awt.Component</CODE>
* (and subclasses), that way sophisticated managment errors can be achieved.
*
* @author Â© 1998 DIRAT Laurent
* @version 1.0  13/04/99
*/
public class ErrorDialog extends Dialog {
	public ErrorDialog(Frame parent, boolean modal)	{
		super(parent, modal);
		
		setLayout(new BorderLayout(0, 0));
		setTitle("Oups, we've got a problem...");
		
		addWindowListener(
		    new WindowAdapter()	{
        		public void windowClosing(WindowEvent event) {
        			dispose();
            	}
            }
        );
    }
    
    public void addMesg(Component mesg) {
        add("Center", mesg);
        validate();
    }

}
