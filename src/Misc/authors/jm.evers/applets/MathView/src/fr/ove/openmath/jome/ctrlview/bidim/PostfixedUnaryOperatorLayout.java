/*
$Id: PostfixedUnaryOperatorLayout.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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

/**
* This layout manager lays postfixed unary operators.
*
* @author © 1999 DIRAT Laurent
* @version 2.0  13/12/1999
*/
public abstract class PostfixedUnaryOperatorLayout extends UnaryOperatorLayout {
    /**
    * Computes the size of the display according to its children size (if any),
    * and its different attributes.
    * @return the size of the display.
    */
    public Dimension computeAttributes() {
        Display dOperator = (Display) displayToLay.getComponent(0);
        Display dOperand = (Display) displayToLay.getComponent(1);
        
        // Initialisation des shift* pour ne pas introduire de faux paramètres.
        dOperator.setAttributes(dOperator.getAscent(), dOperator.getDescent(), 0, 0);
        dOperand.setAttributes(dOperand.getAscent(), dOperand.getDescent(), 0, 0);
        
        // Calcul des attributs de manière classique...
        Dimension dim = super.computeAttributes();
        
        // ...sauf que l'opérateur, il faut le faire passer derrière
        dOperator.setShiftX(dim.width - dOperator.getWidth());
        dOperand.setShiftX(-dOperator.getShiftX() - dOperator.getWidth());
        
        return dim;
    }
}
