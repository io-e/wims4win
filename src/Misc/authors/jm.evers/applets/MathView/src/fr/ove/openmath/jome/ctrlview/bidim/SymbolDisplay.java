/*
$Id: SymbolDisplay.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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
import fr.ove.openmath.jome.ctrlview.bidim.Displayable;
import fr.ove.openmath.jome.ctrlview.bidim.SymbolLayout;

/**
* The display of a symbol.
*
* @author © 1999 DIRAT Laurent
* @version 2.0  09/07/1999
*/
public class SymbolDisplay extends BidimDisplay {
    /**
    * the symbol to display.
    */
    private Displayable symbol;
    
    /**
    * The constructor.
    * @param graphicContext the graphic context of the display.
    * @param symbol The displayable symbol of the display.
    */
    public SymbolDisplay(GraphicContext graphicContext, Displayable symbol) {
        this(graphicContext);
        this.symbol = symbol;
        symbol.setGraphicContext(graphicContext);
    }
    
    /**
    * The constructor.<BR>
    * Instanciate a new display, with no displayable symbol associated. The symbol has to be set
    * by hand.
    * @param graphicContext the graphic context of the display.
    */
    public SymbolDisplay(GraphicContext graphicContext) {
        super(graphicContext);
        SymbolLayout layout = new SymbolLayout();
        layout.initDisplay(this);
        setLayout(layout);
    }
    
    /**
    * Sets a new symbol to the display.
    * @param symbol the symbol to set.
    */
    public void setSymbol(Displayable symbol) {
        this.symbol = symbol;
        symbol.setGraphicContext(getGraphicContext());
    }
    
    /**
    * Returns the symbol which is in the dispay.
    */
    public Displayable getSymbol() {
        return symbol;
    }

    /**
    * Paints the display.
    * @param g where we paint the display.
    */
    public void paint(Graphics g) {
        super.paint(g);
        Color old = g.getColor();
        Rectangle bounds = getBounds();
        
        if (isSelected()) {
            g.setColor(getSelectionColor());
            g.fillRect(0, 0, bounds.width - 1, bounds.height - 1);
        }
        
        g.setColor(getForegroundColor());
        
        if (weDrawBounds())
            g.drawRect(0, 0, bounds.width - 1, bounds.height - 1);
            
        symbol.paint(g);
        
        g.setColor(old);
    }
    
    /**
    * Scales the display.
    */
    public void scaleDisplay() {
        setTheFont(getGraphicContext().scaleFont(getLevel()));
        // Il faut que le layout manager recalcule les attributs du display.
        setComputeAttributes(true);
        // Ceci est nécessaire pour que ce soit le layout manager qui retourne
        // la preferredSize du display, et donc qu'il y ait recalcul des attributs.
        // Sinon, le container considère qu'il a une taille valide, et donc retourne
        // comme preferredSize, la précédente, sans qu'il demande le recalcul au LM.
        invalidate();
        
        // On affecte au symbol le nouveau contexte graphique du display, en fait la réduction de font
        // effectuée.
        // On fait ce test à cause de la construction. Quand on construit le display, on fait un scaleDisplay.
        // (au niveau de Display) Or, scaleDisplay (de l'instance donc), est applelé avant que le symbole ne soit
        // affecté dans leconstructeur.
        if (symbol != null)
            symbol.setGraphicContext(getGraphicContext());
    }
    
    /**
    * Sets all the attributes.
    * @param ascent the new ascent value.
    * @param descent the new descent value.
    * @param shiftX the new horizontal shift value.
    * @param shiftY the new vertical shift value.
    */
    public void setAttributes(int ascent, int descent, int shiftX, int shiftY) {
        super.setAttributes(ascent, descent, shiftX, shiftY);
        symbol.setAscent(ascent);
        symbol.setDescent(descent);
        symbol.setShiftX(shiftX);
        symbol.setShiftY(shiftY);
    }
    
    /**
    * Sets the graphic context of the instance.
    * @param graphicContext the graphic context.
    */
    public void setGraphicContext(GraphicContext graphicContext) {
        super.setGraphicContext(graphicContext);
        symbol.setGraphicContext(graphicContext);
    }
}

