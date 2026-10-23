/*
$Id: SymbolLayout.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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
import fr.ove.openmath.jome.ctrlview.bidim.StringDisplay;
import fr.ove.openmath.jome.ctrlview.bidim.selection.events.SelectionEvent;
import fr.ove.openmath.jome.model.*;

/**
* A layout manager that lays symbols.
*
* @author © 1999 DIRAT Laurent
* @version 2.0  09/07/1999
*/
public class SymbolLayout extends DisplayLayout {
    /**
    * Checks the validity of the selection.
    */
    public void validateSelection() {
        Display display = displayToLay;
        if (display.getParent() instanceof Display) {
            display = (Display) display.getParent();
            FormulaTreeStructure fts = (FormulaTreeStructure) display.getListener();
            if (fts.getFather() != null)
                ((DisplayLayout) display.getLayout()).validateSelection();
        }

        // On met à jour l'affichage.
        display.repaint();
    }
    
    /**
    * Checks the validity of the deselection.
    * @param display the display to deselect.
    */
    public void validateDeselection(Display display) {
        Display father = displayToLay;
        
        if (father.isSelected()) {
            father.setNotSelected();
            // On enlève le display père de la liste des display sélectionnés.
            SelectionEvent selEvt = new SelectionEvent(father);
            selEvt.setAction(SelectionEvent.REMOVE, father);
            father.fireSelectionEvent(selEvt);
            
            if (father.getParent() instanceof Display) {
                father = (Display) father.getParent();
                FormulaTreeStructure fts = (FormulaTreeStructure) father.getListener();
                if (fts.getFather() != null)
                    ((DisplayLayout) father.getLayout()).validateDeselection(displayToLay);
            }
        }
    }

    /**
    * Computes the size of the display and its different attributes.
    * @return the size of the display.
    */
    public Dimension computeAttributes() {
        Displayable symbol = ((SymbolDisplay) displayToLay).getSymbol();
        symbol.setGraphicContext(displayToLay.getGraphicContext());
        Dimension dim = symbol.getPreferredSize();

        displayToLay.setSize(dim);
        displayToLay.setAscent(symbol.getAscent());
        displayToLay.setDescent(symbol.getDescent());
        displayToLay.setComputeAttributes(false);
        
        return dim;
    }
  
    /**
    * The display needs to be rebuilt. We do this.
    */
    public void rebuildDisplay() {
        // En fait, celui là ne sera jamais reconstruit.
    }
   
    // #################################################
    // ### Implémentation des différentes interfaces ###
    // #################################################

    // ***************************************************
    // *** Implémentation de l'interface LayoutManager ***

    /*
     * Lays out the container in the specified panel.
     * @param parent the component which needs to be laid out.
     */
     public void layoutContainer(Container parent) {
     }

    // *** Fin de l'interface LayoutManager ***
    // ****************************************

}
