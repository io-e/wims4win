/*
$Id: EnclosingLayout.java,v 1.3 2003/02/18 11:48:46 sander Exp $
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
import fr.ove.openmath.jome.behaviour.*;

/**
* A layout manager that lays components to be displayed between something.<BR>
* Obviously, what is called something will be parenthesis, braces, ....
*
* @author © 1999 DIRAT Laurent
* @version 2.0  13/12/1999
*/
public abstract class EnclosingLayout extends HorizontalLayout {
    /**
    * The opening of the enclosure
    */
    private SymbolDisplay opening;
    
    /**
    * The closing of the enclosure
    */
    private SymbolDisplay closing;
    
    /**
    * Returns the opening
    */
    public SymbolDisplay getOpening() {
        return opening;
    }
    
    /**
    * Returns the closing
    */
    public SymbolDisplay getClosing() {
        return closing;
    }
    
    /**
    * According to the operator, the layout manager has to add some components (e.g. brackets, ...)
    * or has to perform some "re-oganisation" before rendering.<BR>
    * As soon as the layout manager is set to the display, this mehtod MUST be called with the display laid out
    * as parameter. This method serves as well as a registering method. So all sub-classes of the instance MUST
    * call super.initDisplay(displayToLay).
    * @param displayToLay the display laid by the instance
    */
    public void initDisplay(Display displayToLay) {
        super.initDisplay(displayToLay);
        
        FormulaTreeStructure fts = (FormulaTreeStructure) displayToLay.getListener();
        if (((Maskable) fts).isVisible()) {
            opening = createOpening();

            // On met un listener à l'opening
            // En fait, il n'y en a pas besoin, dans le sens où il n'y a pas spécifiquement de fts qui
            // écoute le comportement de ce display. Néanmoins, il s'avère nécessaire qu'il en ait
            // un, par exemple lors de l'iconification, car c'est le display qui reçoit la demande
            // d'iconification qui envoie l'événement correspondant à la FTS. Or si ce display n'a pas
            // d'écouteur, alors pb. Par cohérence, l'écouteur du display d'opérateur, est le fts qui
            // représente cette opération. Par contre, la fts en question, n'écoute pas le display
            // d'opérateur.
            opening.addControlListener(fts);
            this.displayToLay.add(opening);
            
            closing = createClosing();
            closing.addControlListener(fts);
            this.displayToLay.add(closing);
        }
    }
    
    /**
    * Checks the validity of the selection.
    */
    public void validateSelection() {
        SelectionEvent selEvt = new SelectionEvent(displayToLay);

        if (((Maskable) displayToLay.getListener()).isVisible()) {
            // La validité de la sélection est triviale.
            // Si l'une ou l'autre des parenthèse est sélectionnée, alors on sélectionne
            // tout.
            if (opening.isSelected() || closing.isSelected()) {
                // Sélectionne le display.
                displayToLay.select();
                // On purge la liste des éléments sélectionnés.
                selEvt.setAction(SelectionEvent.PURGE, null);
                displayToLay.fireSelectionEvent(selEvt);
                // On y ajoute nos parenthèses
                selEvt.setAction(SelectionEvent.ADD, displayToLay);
                displayToLay.fireSelectionEvent(selEvt);
            }
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
        Display tmp;
        SelectionEvent selEvt = new SelectionEvent(displayToLay);

        // Si les parenthèses sont sélectionnées, alors il faut les déselectionner.
        if (father.isSelected()) {
            father.setNotSelected();
            // On enlève le display père de la liste des display sélectionnés.
            selEvt.setAction(SelectionEvent.REMOVE, father);
            father.fireSelectionEvent(selEvt);

            if (((Maskable) displayToLay.getListener()).isVisible()) {
                if (display == opening)
                    closing.setNotSelected();
                else
                    opening.setNotSelected();
                    
                // L'opérande est mis dans la liste des sélectionnés
                selEvt.setAction(SelectionEvent.ADD, (Display) father.getComponent(2));
                father.fireSelectionEvent(selEvt);
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

    /**
    * Computes the size of the display according to its children size (if any),
    * and its different attributes.
    * @return the size of the display.
    */
    public Dimension computeAttributes() {
        Display display = null;

        if (((Maskable) displayToLay.getListener()).isVisible()) {
            Displayable openingSymbol = opening.getSymbol();
            Displayable closingSymbol = closing.getSymbol();
            
            // On met la hauteur à 0, pour que dans tous les cas, le super.computeAttributes() ne soit pas
            // biaisée par une éventuelle nouvelle taille des éléments qui composent la liste.
            // Comme c'est plus loin qu'on leur fixe la taille....
            openingSymbol.setHeight(0);
            opening.setHeight(0);
            closingSymbol.setHeight(0);
            closing.setHeight(0);
            
            // même remarque que ci-dessus
            opening.setAttributes(0, 0, 0, 0);
            closing.setAttributes(0, 0, 0, 0);
            ((Display) displayToLay.getComponent(2)).setShiftX(0);
            
            Dimension dim = super.computeAttributes();
            
            // Par contre, par rapport à comment est calculée la taille display, dim comprend
            // déjà la largeur de opening et closing
            
            openingSymbol.setAscent(displayToLay.getAscent());
            openingSymbol.setDescent(displayToLay.getDescent());
            openingSymbol.setHeight(displayToLay.getHeight());
            opening.setComputeAttributes(true);
            opening.invalidate();
            opening.setSize(opening.getPreferredSize());

            closingSymbol.setAscent(displayToLay.getAscent());
            closingSymbol.setDescent(displayToLay.getDescent());
            closingSymbol.setHeight(displayToLay.getHeight());
            closing.setComputeAttributes(true);
            closing.invalidate();
            closing.setSize(closing.getPreferredSize());
            
            // Cette taille là correspond au décalage qu'il faut appliquer à closing
            closing.setShiftX(dim.width - 2*closing.getWidth());
            // Mais il faut l'enlever au display du milieu qui doit être contre l'opening (because HonrizontalLayout)
            ((Display) displayToLay.getComponent(2)).setShiftX(-closing.getShiftX() - closing.getWidth());
            
            return dim;
        }
        else
            return super.computeAttributes();
    }

    /**
    * The display needs to be rebuilt. We do this.
    */
    public void rebuildDisplay() {
        // La taille des displays est probablement différente de ceux qui étaient
        // précédemment. On demande alors le recalcul des display ancêtres.
        displayToLay.computeAncestorsAttributes();
    }
        
    // ############################################
    // ### Les différentes méthodes abstraites  ###
    // ############################################
    
    /**
    * Returns the opening
    */
    public abstract SymbolDisplay createOpening();
    
    /**
    * Returns the closing
    */
    public abstract SymbolDisplay createClosing();
}
