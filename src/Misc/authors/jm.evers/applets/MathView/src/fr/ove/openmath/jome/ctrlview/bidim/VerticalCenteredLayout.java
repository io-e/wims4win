/*
$Id: VerticalCenteredLayout.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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

/**
* A layout manager that lays components vertically.
*
* @author © 1999 DIRAT Laurent
* @version 2.0  15/12/1999
*/
public abstract class VerticalCenteredLayout extends DisplayLayout {
    /**
    * Computes the size of the display according to its children size (if any),
    * and its different attributes.
    * @return the size of the display.
    */
    public Dimension computeAttributes() {
        int height = 0;
        int width1 = 0;
        int width2 = 0;
        int width = 0;
        int ascent;
        int descent;
        Display tmp;
        
        updateLevel(displayToLay.getLevel());
        
        int count = displayToLay.getComponentCount();
        for (int i = 0; i < count; i++) {
            tmp = (Display) displayToLay.getComponent(i);
            tmp.setSize(tmp.getPreferredSize());
        }
                    
        for (int i = 0; i < count; i++) {
            tmp = (Display) displayToLay.getComponent(i);
            
            width = (tmp.getWidth() / 2);
            
            width1 = Math.max( width - tmp.getShiftX(),
                               width1 );
            
            width = ((tmp.getWidth() % 2) == 0 ) ? width : (width + 1);
            width2 = Math.max( width + tmp.getShiftX(),
                               width2 );
            
            height += tmp.getAscent() + tmp.getDescent() + tmp.getShiftY();
        }
        
        // Possibles changements à effectuer pour ne pas mettre la
        // la ligne de référence au milieu de la BBox
        ascent = ( (height%2) == 0 ) ? (height / 2) : (height / 2) + 1 ;
        descent = ( (height%2) == 0 ) ? ascent : ascent - 1 ;
        
        displayToLay.setAscent(ascent);
        displayToLay.setDescent(descent);
        displayToLay.setSize(width1 + width2 , height);
        displayToLay.setComputeAttributes(false);
        
        return new Dimension(width1 + width2 , height);
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
        for ( int i = 0; i < count; i++ ) {
            display = (Display) parent.getComponent(i);
            
            display.setSize(display.getPreferredSize());
            
            x = (((Display) parent).getWidth() / 2) -
                (display.getWidth() / 2) +
                display.getShiftX();
            
            if ( i == 0 )
                y = display.getShiftY();
            else {
                previous = (Display) parent.getComponent(i-1);
                y = previous.getY() + previous.getHeight() + display.getShiftY();
            }
            
            display.setLocation(x, y);
            
            // On place maintenant les éventuels display enfants du display courant
            display.doLayout();
        }
    }

    // *** Fin de l'interface LayoutManager ***
    // ****************************************
}
