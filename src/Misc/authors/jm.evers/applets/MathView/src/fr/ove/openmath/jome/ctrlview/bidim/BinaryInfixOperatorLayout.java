/*
$Id: BinaryInfixOperatorLayout.java,v 1.3 2003/02/18 11:48:46 sander Exp $
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

import fr.ove.openmath.jome.ctrlview.bidim.*;
import fr.ove.openmath.jome.ctrlview.bidim.selection.events.SelectionEvent;
import fr.ove.openmath.jome.model.*;

/**
* A layout manager for an binary infix operator.
*
* @author © 1999 DIRAT Laurent
* @version 2.0  14/12/1999
*/
public abstract class BinaryInfixOperatorLayout extends InfixOperatorLayout {    
    /**
    * Checks the validity of the selection.
    */
    public void validateSelection() {
        SelectionEvent selEvt = new SelectionEvent(displayToLay);

        // La validité de la sélection est triviale.
        // Si les opérandes sont sélectionnés ou ont un élément sélectionné, ou si l'opérateur
        // est sélectionnée, alors on sélectionne tout.
        Display left = (Display) displayToLay.getComponent(0);
        Display ope = (Display) displayToLay.getComponent(1);
        Display right = (Display) displayToLay.getComponent(2);
        if ((left.gotSelectedElements() && right.gotSelectedElements()) ||
            (ope.isSelected())) {
            // Sélectionne le display.
            displayToLay.select();
            // On purge la liste des éléments sélectionnés.
            selEvt.setAction(SelectionEvent.PURGE, null);
            displayToLay.fireSelectionEvent(selEvt);
            // On y ajoute l'opération
            selEvt.setAction(SelectionEvent.ADD, displayToLay);
            displayToLay.fireSelectionEvent(selEvt);
        }

        // On a vérifié la validité de la sélection de la puissance. On doit maitenant
        // la contrôler au niveau supérieur, au niveau du père.
        Display display = displayToLay;
        if (displayToLay.getParent() instanceof Display) {
            display = (Display) displayToLay.getParent();
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
        SelectionEvent selEvt = new SelectionEvent(father);
        
        // Si l'opération est sélectionnée, alors il faut la déselectionner.
        if (father.isSelected()) {
            father.setNotSelected();
            // On enlève le display père de la liste des display sélectionnés.
            selEvt.setAction(SelectionEvent.REMOVE, father);
            father.fireSelectionEvent(selEvt);
            
            Display left = (Display) father.getComponent(0);
            Display ope = (Display) father.getComponent(1);
            Display right = (Display) father.getComponent(2);
            
            if (display == left) {
                ope.setNotSelected();
                selEvt.setAction(SelectionEvent.ADD, right);
                father.fireSelectionEvent(selEvt);
            }
            else if (display == right) {
                ope.setNotSelected();
                selEvt.setAction(SelectionEvent.ADD, left);
                father.fireSelectionEvent(selEvt);
            }
            else {
                left.deselect();
                ope.setNotSelected();
                right.deselect();
            }

            // Comme pour la sélection, on contrôle la validité de la désélection.
            if (father.getParent() instanceof Display) {
                father = (Display) father.getParent();
                FormulaTreeStructure fts = (FormulaTreeStructure) father.getListener();
                if (fts.getFather() != null)
                    ((DisplayLayout) father.getLayout()).validateDeselection(displayToLay);
            }
            
            // Hé oui, on contrôle la validité de la sélection... dans une désélection.
            // Toujours le même pb, est-ce que le nouvel état de la sélection (après
            // désélection donc) est syntaxiquement cohérent ?
            validateSelection();
            
            // On met à jour l'affichage.
            father.repaint();
        }        
    }
}