/*
$Id: AbstractStringLayout.java,v 1.3 2003/02/18 11:48:46 sander Exp $
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
public abstract class AbstractStringLayout extends DisplayLayout {
    /**
    * Updates the level of the display that is layed out.<BR>
    * @param level the level put to the display
    */
    public void updateLevel(int level) {
        if (displayToLay.getUpdateLevel()) {
            displayToLay.setLevel(level);
            displayToLay.scaleDisplay();
            displayToLay.setUpdateLevel(false);
            displayToLay.setSize(computeAttributes());
        }
    }

    /**
    * Computes the size of the display and its different attributes.
    * @return the size of the display.
    */
    public Dimension computeAttributes() {
        FontMetrics fm = displayToLay.getFontMetrics(displayToLay.getFont());
        
        displayToLay.setAscent(fm.getAscent());
        displayToLay.setDescent(fm.getDescent());
        
        int width = fm.stringWidth(((AbstractStringDisplay) displayToLay).getString()) + 2;
        int height = displayToLay.getAscent() + displayToLay.getDescent();
        
        displayToLay.setSize(width, height);
        displayToLay.setComputeAttributes(false);
        
        return new Dimension(width, height);
    }

    /*
     * Lays out the container in the specified panel.
     * @param parent the component which needs to be laid out.
     */
     public void layoutContainer(Container parent) {
        return;
     }


    /**
    * The display needs to be rebuilt. We do this.
    */
    public void rebuildDisplay() {
        // En fait, celui là ne sera jamais reconstruit.
    }
   
}
