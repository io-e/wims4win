/*
$Id: AbsoluteLayout.java,v 1.3 2003/02/18 11:48:46 sander Exp $
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
import fr.ove.utils.FontInfo;

/**
* Layout manager that lays the display of the absolute value operator
*
* @author Â© 1999 DIRAT Laurent
* @version 2.0  15/12/1999
*/
public class AbsoluteLayout extends EnclosingLayout {
    /**
    * Returns the opening
    */
    public SymbolDisplay createOpening() {
        SymbolDisplay bar = new SymbolDisplay(displayToLay.getGraphicContext(), new Bar());
        // Le display de la barre est le display d'un opÃ©rateur (on peut le considÃ©rer comme tel)
        bar.setIsSymbolOperatorDisplay(true);
        return bar;
    }
    
    /**
    * Returns the closing
    */
    public SymbolDisplay createClosing() {
        return createOpening();
    }

    /**
    * Computes the size of the display according to its children size (if any),
    * and its different attributes.
    * @return the size of the display.
    */
    public Dimension computeAttributes() {
        Display display = null;

        // On calcule la taille de l'Ã©lement en valeur absolue
        display = (Display) displayToLay.getComponent(2);
        display.setSize(display.getPreferredSize());
        // On met un petit dÃ©calage horizontal pour que la barre de gauche ne soit pas collÃ©e au terme
        display.setShiftX(2);
        
        int width = display.getShiftX() + display.getWidth();
        int height = display.getShiftY() + display.getHeight();
        int ascent = display.getAscent();
        int descent = display.getDescent();
        int thickness = FontInfo.getLineThickness(displayToLay, displayToLay.getFont());
        
        // Maintenant, on peut fixer la hauteur des barres, qui est la hauteur du composant
        // qu'elles contiennent, et l'ascent, qui est celui du composant qu'elles contiennent.
        SymbolDisplay opening = getOpening();
        Bar bar = (Bar) opening.getSymbol();
        bar.setHeight(display.getHeight());
        bar.setWidth(thickness);
        bar.setAscent(ascent);
        bar.setDescent(descent);
        opening.setComputeAttributes(true);
        opening.invalidate();
        opening.setSize(opening.getPreferredSize());

        width += opening.getWidth();
        
        SymbolDisplay closing = getClosing();
        bar = (Bar) closing.getSymbol();
        bar.setHeight(display.getHeight());
        bar.setWidth(thickness);
        bar.setAscent(ascent);
        bar.setDescent(descent);
        closing.setComputeAttributes(true);
        closing.invalidate();
        closing.setSize(closing.getPreferredSize());
        // On met un petit dÃ©calage horizontal pour que la barre de droite ne soit pas collÃ©e au terme
        closing.setShiftX(2);

        width += closing.getShiftX() + closing.getWidth();
            
        displayToLay.setAscent(ascent);
        displayToLay.setDescent(descent);
        displayToLay.setSize(width, height);
        
        displayToLay.setComputeAttributes(false);
            
        // Cette taille lÃ  correspond au dÃ©calage qu'il faut appliquer Ã  closing
        closing.setShiftX(width - 2*closing.getWidth());
        // Mais il faut l'enlever au display du milieu qui doit Ãªtre contre l'opening (because HonrizontalLayout)
        display.setShiftX(display.getShiftX() - closing.getShiftX() - closing.getWidth());
            
        return new Dimension(width, height);
    }
}
