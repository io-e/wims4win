/*
$Id: StringLayout.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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


package fr.ove.openmath.jome.ctrlview.bidim;

import java.awt.*;
import fr.ove.openmath.jome.ctrlview.bidim.DisplayLayout;
import fr.ove.openmath.jome.ctrlview.bidim.Display;
import fr.ove.openmath.jome.ctrlview.bidim.StringDisplay;
import fr.ove.openmath.jome.ctrlview.bidim.selection.events.SelectionEvent;

/**
* A layout manager that lays strings.
*
* @author © 1998 DIRAT Laurent
* @version 1.0  23/06/98
*/
public class StringLayout extends AbstractStringLayout {
    /**
    * Returns <CODE>true</CODE> if during the selection, the display which is located
    * to the left of the display that lays the instance, must be selected too.
    * <CODE>false</CODE> otherwise.
    */
    public boolean selectLeftDisplay() {
        if (displayToLay.isSelected() && displayToLay.isSymbolOperatorDisplay())
            return true;
        else
            return false;
    }
    
    /**
    * Returns <CODE>true</CODE> if during the selection, the display which is located
    * to the right of the display that lays the instance, must be selected too.
    * <CODE>false</CODE> otherwise.
    */
    public boolean selectRightDisplay() {
        if (displayToLay.isSelected() && displayToLay.isSymbolOperatorDisplay())
            return true;
        else
            return false;
    }

    /**
    * Checks the validity of the selection.
    */
    public void validateSelection() {
    }
    
    /**
    * Checks the validity of the deselection.
    * @param display the display to deselect.
    */
    public void validateDeselection(Display display) {
    }
}
