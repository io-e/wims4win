/*
$Id: SigmaLayout.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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
import fr.ove.openmath.jome.ctrlview.bidim.Display;
import fr.ove.openmath.jome.ctrlview.bidim.DisplayLayout;
import fr.ove.openmath.jome.ctrlview.bidim.HorizontalLayout;
import fr.ove.openmath.jome.ctrlview.bidim.selection.events.SelectionEvent;
import fr.ove.openmath.jome.model.*;

/**
* A layout manager that lays components to place them as the different elements of a
* sum.<BR>
* The different elements are contained in the display in the following order :
* <UL>
* <LI>the symbol sigma.</LI>
* <LI>the expression.</LI>
* <LI>the lower bound.</LI>
* <LI>the upper bound.</LI>
*</UL>
*
* Bounds are optional elements.
*
* @author © 1999 DIRAT Laurent
* @version 2.0 16/12/1999
*/
public class SigmaLayout extends MapsToSigmaLayout {
    /**
    * Returns the display of the operator
    */
    public Display createOperatorDisplay() {
        SymbolDisplay sigma = new SymbolDisplay(displayToLay.getGraphicContext(), 
                                                new ImageSymbol("Sigma", displayToLay));
        // Le display de la somme est le display d'un opérateur (on peut le considérer comme tel)
        sigma.setIsSymbolOperatorDisplay(true);
        
        return sigma;
    }
}
