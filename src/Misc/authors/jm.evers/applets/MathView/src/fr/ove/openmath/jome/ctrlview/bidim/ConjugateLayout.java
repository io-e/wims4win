/*
$Id: ConjugateLayout.java,v 1.3 2003/02/18 11:48:46 sander Exp $
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
import fr.ove.openmath.jome.ctrlview.bidim.Bar;
import fr.ove.openmath.jome.ctrlview.bidim.selection.events.SelectionEvent;
import fr.ove.openmath.jome.model.*;
import fr.ove.utils.FontInfo;

/**
* Layout manager that lays the display of the conjugate operator
*
* @author © 1999 DIRAT Laurent
* @version 2.0 15/12/1999
*/
public class ConjugateLayout extends OverOperatorLayout {
    /**
    * Computes the size of the display according to its children size (if any),
    * and its different attributes.
    * @return the size of the display.
    */
    public Dimension computeAttributes() {
        SymbolDisplay displayBar = null;
        Bar bar = null;
        
        updateLevel(displayToLay.getLevel());
            
        // On est dans le cas particulier d'un symbol qui ne connais pas sa taille à l'avance.
        // Pour que cela puisse fonctionner correctement avec les autres displays, il faut qu'on
        // lui precise sa preferred size.
        // On met sa longueur à 0 pour éviter les élargissements infinis lors des différents retaillages
        // dus aux désiconifications.
        displayBar = (SymbolDisplay) displayToLay.getComponent(0);
        bar = (Bar) displayBar.getSymbol();
        bar.setWidth(0);
        bar.setHeight(FontInfo.getLineThickness(displayToLay, displayToLay.getFont())); // la hauteur fixe aussi ascent et descent        
        
        displayBar.setComputeAttributes(true);
        displayBar.setShiftY(2);
    
        // On calcule les attributs des display enfants comme si 
        // on avait affaire à un VerticalCenteredLayout
        Dimension dim = super.computeAttributes(); 
        
        // La différence réside en le calcul supplémentaire de la taille de la 
        // barre que l'on ne peut fixer après avoir calculé la largeur de l'argument
        bar.setWidth(dim.width + 2);
        displayBar.setSize(bar.getSize());
                                                
        displayToLay.setAscent(((Display) displayToLay.getComponent(0)).getShiftY() +
                  ((Display) displayToLay.getComponent(0)).getHeight() + 
                  ((Display) displayToLay.getComponent(1)).getShiftY() +
                  ((Display) displayToLay.getComponent(1)).getAscent());
                  
        displayToLay.setDescent(displayToLay.getHeight() - displayToLay.getAscent());
        
        displayToLay.setSize(dim);

        displayToLay.setComputeAttributes(false);
        
        return dim;
    }
    
    /**
    * Returns the display of the operator
    */
    public Display createOperatorDisplay() {
        SymbolDisplay bar = new SymbolDisplay(displayToLay.getGraphicContext(), new Bar());
        // Le display de la barre est le display d'un opérateur (on peut le considérer comme tel)
        bar.setIsSymbolOperatorDisplay(true);
        
        return bar;
    }
}
