/*
$Id: Bar.java,v 1.3 2003/02/18 11:48:46 sander Exp $
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
import fr.ove.openmath.jome.ctrlview.bidim.DisplayableImpl;

/**
* The fraction bar to display with a fraction.
*
* @author © 1999 DIRAT Laurent
* @version 2.0 08/07/1999
*/
public class Bar extends DisplayableImpl {
    // La barre de fraction correspond en fait à la taille de DisplayableImpl.
    // La longueur correpond à la largeur.
    // L'épaisseur correspond à la hauteur
    
    /**
    * Sets the height of the instance.
    * @param height the height of the instance
    */
    public void setHeight(int height) {
        super.setHeight(height);
        // Mais là, en plus, on calcule l'ascent et le descent
        // car en fait, c'est la hauteur (l'épaisseur) de la barre de fraction
        // Qui détermine leur valeur
        //setAscent(height / 2);
        setAscent(((height % 2) == 0) ? (height / 2) - 1 : height / 2);
        setDescent(height - getAscent());
    }
    
    /**
    * The paint method of the object to display.
    * @param g the drawing area of the symbol.
    */
    public void paint(Graphics g) {
        Dimension size = getSize();
        g.fillRect(0, 0, size.width, size.height);
    }
}
