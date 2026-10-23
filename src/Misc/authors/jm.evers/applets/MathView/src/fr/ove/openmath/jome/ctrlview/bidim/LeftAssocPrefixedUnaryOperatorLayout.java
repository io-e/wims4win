/*
$Id: LeftAssocPrefixedUnaryOperatorLayout.java,v 1.3 2003/02/18 11:48:46 sander Exp $
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
* This layout manager lays prefixed unary operators.<BR>
* Basically, unary + and unary - because these operators have the particularity to select the operand located
* to its left when selected.
*
* @author © 1999 DIRAT Laurent
* @version 2.0  13/12/1999
*/
public abstract class LeftAssocPrefixedUnaryOperatorLayout extends UnaryOperatorLayout {
    /**
    * Do we have to select the display to the left of the one the instance lays ?
    */
    private boolean selectLeftDisplay;
    
    /**
    * Returns <CODE>true</CODE> if during the selection, the display which is located
    * to the left of the display that the instance lays, must be selected too.
    * <CODE>false</CODE> otherwise.
    */
    public boolean selectLeftDisplay() {
        // On est dans les opérateurs unaires, donc si le display tout entier
        // est sélectionné, alors cela revient à avoir sélectionné l'opérateur,
        // et donc il faut sélectionner le display de gauche dans la liste de 
        // display de son père.
        if (displayToLay.isSelected())
            return selectLeftDisplay;
        else
            return false;
    }    
    
    /**
    * Sets if the display to the left of that laid by the instance have to be selected
    * or not.
    * @param selectLeftDisplay <CODE>true</CODE> if the left display have to be selected.
    * <CODE>false</CODE> otherwise.
    */
    public void setSelectLeftDisplay(boolean selectLeftDisplay) {
        this.selectLeftDisplay = selectLeftDisplay;
    }
    
    /**
    * Checks the validity of the selection.
    */
    public void validateSelection() {
        SelectionEvent selEvt = new SelectionEvent(displayToLay);

        // La validité de la sélection est triviale.
        // Si le display de l'opérateur ou si le display de l'opérande est
        // sélectionné, on sélectionne tout le display.
        Display op = (Display) displayToLay.getComponent(0);
        Display opRand = (Display) displayToLay.getComponent(1);
        if (op.isSelected() || opRand.isSelected()) {
            
            selectLeftDisplay = op.isSelected() ? true : false;
            
            displayToLay.select();
            // On purge la liste des éléments sélectionnés.
            selEvt.setAction(SelectionEvent.PURGE, null);
            displayToLay.fireSelectionEvent(selEvt);
            // On y ajoute l'instance dans la liste des sélectionnés
            selEvt.setAction(SelectionEvent.ADD, displayToLay);
            displayToLay.fireSelectionEvent(selEvt);
        }
        
        // On a vérifié la validité de la sélection de l'opérateur. On doit maitenant
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
        // father est le display de l'opérateur unaire.
        Display father = displayToLay;
        Display tmp;
        SelectionEvent selEvt = new SelectionEvent(father);
        
        // Si l'opérateur en entier est sélectionné, il faut le désélectionner
        if (father.isSelected()) {
            selectLeftDisplay = false;
            father.deselect();
            // On enlève le display père de la liste des display sélectionnés.
            selEvt.setAction(SelectionEvent.REMOVE, father);
            father.fireSelectionEvent(selEvt);
            
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
