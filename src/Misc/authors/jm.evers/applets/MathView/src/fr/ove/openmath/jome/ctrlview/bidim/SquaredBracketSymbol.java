/*
$Id: SquaredBracketSymbol.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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
import fr.ove.utils.FontInfo;

/**
* The bracket symbol for rendering interval. (i.e. symbol [ or ])
*
* @author © 1999 DIRAT Laurent
* @version 2.0 06/09/1999
*/
public class SquaredBracketSymbol extends DisplayableImpl {
    /**
    * The character (i.e. '[' or ']') represented by the instance
    * By default, the instance represents '['.
    */
    private char character;
    
    /**
    * The thickness of the line to draw the symbol.
    */
    private int thickness;
    
    /**
    *
    */
    private Component component;
    
    /**
    * The constructor.
    * @param character the associated character (i.e. '[' or ']') to the instance
    **/
    public SquaredBracketSymbol(char character, Component component) {
        this.character = character;
        this.component = component;
    }
    
    /**
    * The constructor.
    * @param character the string representation of the associated character (i.e. '[' or ']') to the instance
    **/
    public SquaredBracketSymbol(String character, Component component) {
        this.character = character.charAt(0);
        this.component = component;
    }
    
    /**
    * Sets the thickness of the line to draw the symbol
    * @param thickness the thickness to draw the symbol 
    */
    public void setThickness(int thickness) {
        this.thickness = thickness;
    }
    
    /**
    * Returns the preferred size of the display.
    */
    public Dimension getPreferredSize() {
        return new Dimension(FontInfo.getStringWidth(component, getGraphicContext().getFont(), "[") + 6, getHeight());
    }
    
    public Dimension getSize() {
        return getPreferredSize();
    }
    
    
    /**
    * The paint method of the object to display.
    * @param g the drawing area of the symbol.
    */
    public void paint(Graphics g) {
        Dimension size = getSize();
        // La barre horizontale du haut
        g.fillRect(0+3, 0, size.width-6, thickness);
        // La barre horizontale du bas
        g.fillRect(0+3, size.height - thickness, size.width-6, thickness);
        
        // La barre verticale
        if (character == '[')
            g.fillRect(0+3, 0, thickness, size.height);
        else
            g.fillRect(size.width - thickness - 3, 0, thickness, size.height);
    }
}
