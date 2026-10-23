/*
$Id: BasicStringInfixOperatorLayout.java,v 1.3 2003/02/18 11:48:46 sander Exp $
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
import fr.ove.openmath.jome.ctrlview.bidim.*;
import fr.ove.openmath.jome.ctrlview.bidim.selection.events.*;
import fr.ove.openmath.jome.model.*;

/**
* A layout manager for an infix operator which symbol can be rendered as a string (e.g. +, *)
*
* @author © 1999 DIRAT Laurent
* @version 2.0  14/12/1999
*/
public class BasicStringInfixOperatorLayout extends StringInfixOperatorLayout {    
    /**
    * Inserts, if necessary, a display of the operator symbol that the display
    * laid by the instance represents.
    */
    protected void insertOperatorDisplay() {
        Display current, next, displayOperator;
        FormulaTreeStructure fatherFts = (FormulaTreeStructure) displayToLay.getListener();
        FormulaTreeStructure fts;
        int nbComponent = displayToLay.getComponentCount();
        
        // On parcourre tous les components présents dans le display.
        for (int i = 1; i < nbComponent; i++) {
            // la référence sur le component courant
            current = (Display) displayToLay.getComponent(i - 1);
            // la référence sur le suivant
            next = (Display) displayToLay.getComponent(i);
            
            // Si le courant est un display d'opérateur alors on continue
            if (current.isSymbolOperatorDisplay())
                continue;
                
            // Si le suivant n'est pas un display d'opérateur alors 2 cas
            if (!next.isSymbolOperatorDisplay()) {
                // La seule distinction qu'il faut faire à ce stade là, c'est le cas où next
                // est le diplay d'un - unaire. Si c'est le cas, on n'insère pas de display.
                fts = (FormulaTreeStructure) next.getListener();
                if (fts.isOperator()) {
                    String ope = ((Operator) fts).getTheOperator();
                    if (ope.equals("-"))
                        continue;
                }
                    
                // On ajoute un display d'opérateur.
                displayOperator = createOperatorDisplay();
                displayToLay.add(displayOperator, displayOperator, i);
                
                // On met un listener à l'opérateur.
                // En fait, il n'y en a pas besoin, dans le sens où il n'y a pas spécifiquement de fts qui
                // écoute le comportement de ce display. Néanmoins, il s'avère nécessaire qu'il en ait
                // un, par exemple lors de l'iconification, car c'est le display qui reçoit la demande
                // d'iconification qui envoie l'événement correspondant à la FTS. Or si ce display n'a pas
                // d'écouteur, alors pb. Par cohérence, l'écouteur du display d'opérateur, est le fts qui
                // représente cette opération. Par contre, la fts en question, n'écoute pas le display
                // d'opérateur.
                displayOperator.addControlListener(fatherFts);
                i++;
                nbComponent++;
            }
        }
        
        // Mise à jour des rangs des displays
        displayToLay.adjustRank();
        // Mise à jour du nombre de components présents dans le display.
        prevNbComponent = displayToLay.getComponentCount();
        // On n'a plus besoin d'insérer des displays d'opérateur.
        insertOperatorDisplay = false;
        // On fait en sorte que la mise à niveau des displays (opérateurs) soit faite.
        displayToLay.setUpdateLevel(true);
        // On a rajouté un display, on demande le recalcul de tous les ancêtres
        // de l'instance.
        displayToLay.computeAncestorsAttributes();
    }
    
    /**
    * Checks the validity of the selection.
    */
    public void validateSelection() {
        Display childDisplay;
        DisplayLayout childLayout;
        SelectionEvent selEvt = new SelectionEvent(displayToLay);
        
        int nbChild = displayToLay.getComponentCount();
        
        // On parcourre la liste des displays, et on regarde sur chacun des dispays s'il faut sélectionner
        // un de ses voisins.
        for (int i = 0; i < nbChild; i++) {
            childDisplay = (Display) displayToLay.getComponent(i);
            childLayout = (DisplayLayout) childDisplay.getLayout();
            
            // On regarde ici s'il faut sélectionner le display à gauche de childLayout
            // Si ledit display n'est pas sélectionner, on le sélectionne.
            if (childLayout.selectLeftDisplay() && (i > 0)) {
                childDisplay = (Display) displayToLay.getComponent(i-1);
                if (!childDisplay.isSelected())
                    childDisplay.select();
            }

            // On regarde ici s'il faut sélectionner le display à droite de childLayout
            // Si ledit display n'est pas sélectionner, on le sélectionne.
            if (childLayout.selectRightDisplay() && (i < (nbChild - 1))) {
                childDisplay = (Display) displayToLay.getComponent(i+1);
                if (!childDisplay.isSelected())
                    childDisplay.select();
                // On a sélectionné le display à la position i+1, donc on incrémente ici i,
                // pour que dans le for, le i soit encore incrémenté et que l'on se positionne
                // ainsi sur un display suivant, non sélectionné
                i++;
            }
        }
        
        // On parcourre la liste des displays et on regarde si les displays à gauche et à droite du display
        // courant ont des éléments sélectionnés. Si c'est le cas et que le display courant est un display
        // d'opérateur et qu'il n'est pas sélectionné, alors on sélectionne le display à gauche, le displa
        // d'opérateur et le display à droite.
        // Ex: (...) + (...), dans chacune des parenthèses, un élément est sélectionné, on sélectionne tout
        Display prev;
        Display next;
        for (int i = 1; i < (nbChild - 1); i++) {
            prev = (Display) displayToLay.getComponent(i-1);
            childDisplay = (Display) displayToLay.getComponent(i);
            next = (Display) displayToLay.getComponent(i+1);
            if (prev.gotSelectedElements() && next.gotSelectedElements()) {
                //if ((childDisplay instanceof StringDisplay) && !childDisplay.isSelected()) {
                if (childDisplay.isSymbolOperatorDisplay() && !childDisplay.isSelected()) {
                    if (!prev.isSelected())
                        prev.select();
                    childDisplay.select();
                    if (!next.isSelected())
                        next.select();
                }
            }
        }
        
        // On créé maintenant une liste contenant tous les displays qui ont été sélectionnés au cours des
        // opérations précédentes.
        Vector list = new Vector();
        for (int i = 0; i < nbChild; i++) {
            childDisplay = (Display) displayToLay.getComponent(i);
            if (childDisplay.gotSelectedElements())
                list.addElement(childDisplay);
        }

        if (list.size() > 0) {
            if ((list.size() == nbChild) && !(displayToLay.getListener() instanceof Formula) && !(displayToLay.getListener() instanceof Slot)) {
                selEvt.setAction(SelectionEvent.PURGE, null);
                displayToLay.fireSelectionEvent(selEvt);

                displayToLay.setSelected();
                selEvt.setAction(SelectionEvent.ADD, displayToLay);
                displayToLay.fireSelectionEvent(selEvt);
            }
            else if (list.size() != 1) {
                selEvt.setAction(SelectionEvent.PURGE, null);
                displayToLay.fireSelectionEvent(selEvt);

                for (Enumeration e = list.elements(); e.hasMoreElements(); ) {
                    childDisplay = (Display) e.nextElement();
                    if (!childDisplay.isSelected())
                        childDisplay.select();
                    selEvt.setAction(SelectionEvent.ADD, childDisplay);
                    displayToLay.fireSelectionEvent(selEvt);
                }
            }
        }
        
        // On a une sélection valide dans le display layed.
        // On demande la validation au niveau de son père.
        // En principe, à cause du test sur le fait d'avoir une Formula, pas besoin
        // d'avoir le test juste suivant. A surveiller.
        Display display = displayToLay;
        if (display.getParent() instanceof Display) {
            display = (Display) display.getParent();
            //if (!(display.getListener() instanceof Formula))
            FormulaTreeStructure fts = (FormulaTreeStructure) display.getListener();
            if (fts.getFather() != null)
                ((DisplayLayout) display.getLayout()).validateSelection();
        }
        
        display.repaint(); // faut voir, parce que autant de repaint que de display ?????????
    }


    /**
    * Checks the validity of the deselection.
    * @param display the display to deselect.
    */
    public void validateDeselection(Display display) {
        Display father = displayToLay;
        Display tmp;
        SelectionEvent selEvt = new SelectionEvent(father);
        int rank = display.getRank();

        // Premier cas: father (i.e. le display géré par l'instance) est sélectionné.
        if (father.isSelected()) {
            // On l'enlève de la liste des displays sélectionnés.
            father.setNotSelected();
            selEvt.setAction(SelectionEvent.REMOVE, father);
            father.fireSelectionEvent(selEvt);

            // Comme on arrive dans cette méthode par display, display a été déjà désélectionné.
            // De plus, ce LM gère principalement (voire que) des displays N-aires dont il s'agit
            // la plupart du temps d'une succession de displays opérandes entre lesquels se trouvent
            // des displays opérateur (ex: + ou *).
            // Donc, s'ils existent, on doit alors désélectionner le display précédent et le display
            // suivant... display. (d'où récupération du rang de display au début)

            // On s'occupe du display précédant... display.
            if (rank > 0) {
                if (!(display.getListener() instanceof UnaryMinus)) {
                    // Il existe un display avant... display, donc on prend garde de le désélectionner,
                    // s'il était sélectionné.
                    tmp = ((Display) father.getComponent(rank-1));
                    if (tmp.isSelected()) {
                        // On désélectionne donc ledit display.
                        tmp.deselect();

                        // Maintenant, on regarde s'il existe un display précédent celui que l'on vient
                        // de désélectionner, qui est un display d'opérateur et qui est sélectionné.
                        // Si c'est le cas, on le désélectionne.
                        if (rank > 1) {
                            tmp = ((Display) father.getComponent(rank-2));
                            if (tmp.isSymbolOperatorDisplay() && tmp.isSelected())
                                tmp.deselect();
                        }
                    }
                }
            }

            // On s'occupe du display suivant... display.
            if (rank < (father.getComponentCount()-1)) {
                // Il existe un display après... display, donc on prend garde de le désélectionner,
                // s'il était sélectionné.
                tmp = ((Display) father.getComponent(rank+1));
                // Il faut faire attention dans le cas d'une addition où il y aurait un opérateur unaire.
                // (Normalement seulement un UnaryMinus). Dans le cas le plus simple, une soustraction: a-b
                // Si a-b est sélectionné et que l'on arrive ici parce on a désélectionné a, alors -b doit rester
                // sélectionné. D'où ce cas particulier supplémentaire.
                if (!(tmp.getListener() instanceof UnaryMinus)) {
                    if (tmp.isSelected()) {
                        tmp.deselect();

                        // Maintenant, on regarde s'il existe un display suivant celui que l'on vient
                        // de désélectionner, qui est un display d'opérateur et qui est sélectionné.
                        // Si c'est le cas, on le désélectionne.
                        if (rank < (father.getComponentCount()-2)) {
                            tmp = ((Display) father.getComponent(rank+2));
                            if (tmp.isSymbolOperatorDisplay() && tmp.isSelected())
                                tmp.deselect();
                        }
                    }
                }
                else
                    ((LeftAssocPrefixedUnaryOperatorLayout) tmp.getLayout()).setSelectLeftDisplay(false);
            }
            
            // On parcourre la liste des display de father et on ajoute dans la liste des sélectionnés,
            // ... les sélectionnés.
            for (int i = 0; i < father.getComponentCount(); i++) {
                tmp = (Display) father.getComponent(i);
                if (tmp.isSelected()) {
                    selEvt.setAction(SelectionEvent.ADD, tmp);
                    father.fireSelectionEvent(selEvt);
                }
            }
            
            // On regarde maintenant au niveau supérieur, si la désélection de father génère
            // une désélection qui est valide.
            if (father.getParent() instanceof Display) {
                father = (Display) father.getParent();
                FormulaTreeStructure fts = (FormulaTreeStructure) father.getListener();
                if (fts.getFather() != null)
                    ((DisplayLayout) father.getLayout()).validateDeselection(displayToLay);
            }
        }
        // Deuxième cas: father (i.e. le display géré par l'instance) n'est pas sélectionné.
        // Mais, il en a des sélectionnés, puisque display fait partie de sa descendance.
        else {
            // Et bien, on fait exactement la même chose que précédemment, à la différence importante
            // que là, les displays sélectionnés font partie de la liste des displays sélectionnés.
            // Il faut donc les y enlever.
            if (rank > 0) {
                if (!(display.getListener() instanceof UnaryMinus)) {
                    tmp = ((Display) father.getComponent(rank-1));
                    if (tmp.isSelected()) {
                        tmp.deselect();
                        selEvt.setAction(SelectionEvent.REMOVE, tmp);
                        father.fireSelectionEvent(selEvt);

                        if (rank > 1) {
                            tmp = ((Display) father.getComponent(rank-2));
                            if (tmp.isSymbolOperatorDisplay() && tmp.isSelected()) {
                                tmp.deselect();
                                selEvt.setAction(SelectionEvent.REMOVE, tmp);
                                father.fireSelectionEvent(selEvt);
                            }
                        }
                    }
                }
            }

            if (rank < (father.getComponentCount()-1)) {
                tmp = ((Display) father.getComponent(rank+1));
                // Il faut faire attention dans le cas d'une addition où il y aurait un opérateur unaire.
                // (Normalement seulement un UnaryMinus). Dans le cas le plus simple, une soustraction: a-b
                // Si a-b est sélectionné et que l'on arrive ici parce on a désélectionné a, alors -b doit rester
                // sélectionné. D'où ce cas particulier supplémentaire.
                if (!(tmp.getListener() instanceof UnaryMinus)) {
                    if (tmp.isSelected()) {
                        tmp.deselect();

                        // Maintenant, on regarde s'il existe un display suivant celui que l'on vient
                        // de désélectionner, qui est un display d'opérateur et qui est sélectionné.
                        // Si c'est le cas, on le désélectionne.
                        if (rank < (father.getComponentCount()-2)) {
                            tmp = ((Display) father.getComponent(rank+2));
                            if (tmp.isSymbolOperatorDisplay() && tmp.isSelected())
                                tmp.deselect();
                        }
                    }
                }
                else
                    ((LeftAssocPrefixedUnaryOperatorLayout) tmp.getLayout()).setSelectLeftDisplay(false);
            }
        }

        // Comme toujours, on contrôle maintenant, avec les éventuels displays sélectionnés restant,
        // si la sélection est valide.
        validateSelection();
        // On met à jour le display.
        father.repaint();
    }
}