/*
$Id: UnderOperatorLayout.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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
import fr.ove.openmath.jome.ctrlview.bidim.*;
import fr.ove.openmath.jome.ctrlview.bidim.selection.events.SelectionEvent;
import fr.ove.openmath.jome.model.*;

/**
* Layout manager that lays the display of the operator whose symbol is located under its operand
*
* @author © 1999 DIRAT Laurent
* @version 2.0 15/12/1999
*/
public abstract class UnderOperatorLayout extends SuperposedOperatorLayout {
    /**
    * Computes the size of the display according to its children size (if any),
    * and its different attributes.
    * @return the size of the display.
    */
    public Dimension computeAttributes() {
        // La différence réside dans le fait qu'il faut faire en sorte que le display de l'opérateur soit affiché
        // en dessous de l'opérande
        Dimension dim = super.computeAttributes(); 
        
        Display displayOperator = (Display) displayToLay.getComponent(0);
        Display operand = (Display) displayToLay.getComponent(1);
        
        int ascent = operand.getAscent();
        
        displayToLay.setAscent(ascent);
        displayToLay.setDescent(dim.height - ascent);

        // Cette taille là correspond au décalage qu'il faut appliquer à la barre de fraction
        displayOperator.setShiftY(operand.getHeight());
        // Mais il faut l'enlever au display du numérateur
        operand.setShiftY(-displayOperator.getShiftY() - displayOperator.getHeight());
        
        displayToLay.setComputeAttributes(false);
        
        return dim;

    
    }
}
