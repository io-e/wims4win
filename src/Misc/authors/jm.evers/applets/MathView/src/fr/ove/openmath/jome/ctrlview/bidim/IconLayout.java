/*
$Id: IconLayout.java,v 1.3 2003/02/18 11:48:46 sander Exp $
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
import java.util.Vector;
import fr.ove.openmath.jome.ctrlview.bidim.SymbolDisplay;
import fr.ove.openmath.jome.ctrlview.bidim.Display;
import fr.ove.openmath.jome.ctrlview.bidim.SubstitutedDisplayManager;

/**
* A layout manager for the display of of an icon
*
* @author © 1999 DIRAT Laurent
* @version 2.0 19/07/1999
*/
public class IconLayout extends SymbolLayout implements SubstitutedDisplayManager {
    /**
    * The list of the displays that have been substituted.
    */
    private Vector substitutedDisplay = new Vector();
    
    /*
    * Adds an substituted display to the list.
    */
    public void addSubstitutedDisplay(Display display) {
        substitutedDisplay.addElement(display);
    }
    
    /*
    * Removes a substituted display to the list.
    * @param display the display to remove.
    */
    public void removeSubstitutedDisplay(Display display) {
        substitutedDisplay.removeElement(display);
    }
    
    /*
    * Removes a substituted display at the specified rank to the list.
    * @param rank the specified rank.
    */
    public void removeSubstitutedDisplay(int rank) {
        substitutedDisplay.removeElementAt(rank);
    }
    
    /**
    * Returns the substituted display at the specified rank.
    * @param rank the specified rank.
    */
    public Display getSubstitutedDisplay(int rank) {
        return (Display) substitutedDisplay.elementAt(rank);
    }
    
    /**
    * Returns the number of substituted displays.
    */
    public int getNbSubstitutedDisplay() {
        return substitutedDisplay.size();
    }
}
