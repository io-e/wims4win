/*
$Id: HorizontalLayout.java,v 1.3 2003/02/18 11:48:46 sander Exp $
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
import java.util.*;
import fr.ove.openmath.jome.ctrlview.bidim.Display;
import fr.ove.openmath.jome.ctrlview.bidim.DisplayLayout;
import fr.ove.openmath.jome.ctrlview.bidim.selection.events.SelectionEvent;
import fr.ove.openmath.jome.model.*;

/**
* A layout manager that lays components horyzontally.
*
* @author © 1999 DIRAT Laurent
* @version 2.0 13/12/1999
*/
public abstract class HorizontalLayout extends DisplayLayout {
    /**
    * Computes the size of the display according to its children size (if any),
    * and its different attributes.
    * @return the size of the display.
    */
    public Dimension computeAttributes() {
        int ascent = 0;
        int descent = 0;
        Display tmp;
        int width = 0;
        int height = 0;
        
        updateLevel(displayToLay.getLevel());

        int count = displayToLay.getComponentCount();
        for (int i = 0; i < count; i++) {
            tmp = (Display) displayToLay.getComponent(i);
            tmp.setSize(tmp.getPreferredSize());
            
            width += tmp.getWidth() + tmp.getShiftX();
            
            ascent = Math.max(tmp.getAscent() - tmp.getShiftY(),
                              ascent);
            
            descent = Math.max(tmp.getDescent() + tmp.getShiftY(),
                               descent);
        }

        displayToLay.setAscent(Math.max( 0, ascent ));
        displayToLay.setDescent(Math.max( 0, descent));
        height += displayToLay.getAscent() + displayToLay.getDescent();
        displayToLay.setSize(width , height);
        displayToLay.setComputeAttributes(false);

        return new Dimension(width, height);
    }


    // #################################################
    // ### Implémentation des différentes interfaces ###
    // #################################################

    // ***************************************************
    // *** Implémentation de l'interface LayoutManager ***


    /*
     * Lays out the container in the specified panel.
     * @param parent the component which needs to be laid out
     */
     public void layoutContainer(Container parent) {
        int x, y;
        Display display, previous;
        
        int count = parent.getComponentCount();
        for (int i = 0; i < count; i++) {
            display = (Display) parent.getComponent(i);
            display.setSize(display.getPreferredSize());

            if (i == 0)
                x = display.getShiftX();
            else {
                previous = (Display) parent.getComponent(i-1);
                x = previous.getX() + previous.getWidth() + display.getShiftX();
            }

            y = ((Display) parent).getAscent() - display.getAscent() + display.getShiftY();

            display.setLocation(x, y);

            // On place maintenant les éventuels display enfants du display courant
            display.doLayout();
        }
    }

    // *** Fin de l'interface LayoutManager ***
    // ****************************************
}
