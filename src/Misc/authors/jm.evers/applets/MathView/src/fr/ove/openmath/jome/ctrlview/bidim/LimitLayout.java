/*
$Id: LimitLayout.java,v 1.3 2003/02/18 11:48:46 sander Exp $
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
* A layout manager that lays components for the limit operator.
*
* @author © 2000 DIRAT Laurent
* @version 2.0 25/02/2000
*/
public class LimitLayout extends HorizontalLayout {
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
        
        KaryOperator limit = (KaryOperator) this.displayToLay.getListener();
        
        // Allocation d'un display, qui va correspondre à la partie "lim" de la représentation
        Display display = new BidimDisplay(displayToLay.getGraphicContext());
        // On met un listener à display
        // En fait, il n'y en a pas besoin, dans le sens où il n'y a pas spécifiquement de fts qui
        // écoute le comportement de ce display. Néanmoins, il s'avère nécessaire qu'il en ait
        // un, par exemple lors de l'iconification, car c'est le display qui reçoit la demande
        // d'iconification qui envoie l'événement correspondant à la FTS. Or si ce display n'a pas
        // d'écouteur, alors pb. Par cohérence, l'écouteur du display d'opérateur, est le fts qui
        // représente cette opération. Par contre, la fts en question, n'écoute pas le display
        // d'opérateur.
        display.addControlListener(limit);
        // On lui met dont un SymbolLimLayout
        DisplayLayout layout = new SymbolLimLayout();
        layout.initDisplay(display);
        display.setLayout(layout);
        
        // On l'ajoute comme fils à display
        this.displayToLay.add(display);
        
        // On met un display pour l'affichage de lim
        StringDisplay lim = new StringDisplay(this.displayToLay.getGraphicContext(), "lim", true);
        // idem
        lim.addControlListener(limit);
        display.add(lim);
        
        // On met un display pour l'affichage de qui tend vers quoi
        Display towards = new BidimDisplay(displayToLay.getGraphicContext());
        towards.addControlListener(limit);
        // On lui met un 
        layout = new ImageBinaryInfixOperatorLayout();
        layout.initDisplay(towards);
        towards.setLayout(layout);
        display.add(towards);
    }
    
    /**
    * Updates the level of the display that is layed out.<BR>
    * @param level the level put to the display
    */
    public void updateLevel(int level) {
        // Pour éviter de refaire le calcul plusieurs fois....
        if (displayToLay.getUpdateLevel()) {
            super.updateLevel(level);
            
            // sauf que l'affichage de quoi tend vers quoi est a un niveu supérieur
            Display towards = (Display) displayToLay.getComponent(0);
            towards = (Display) towards.getComponent(1);
            towards.setUpdateLevel(true);
            ((DisplayLayout) towards.getLayout()).updateLevel(level+1);
            towards = null;
        }
    }
    
    /**
    * Checks the validity of the selection.
    */
    public void validateSelection() {
        /*
        SelectionEvent selEvt = new SelectionEvent(displayToLay);

        Display derivator = (Display) displayToLay.getComponent(0);
        Display func = (Display) displayToLay.getComponent(1);
        // Si le display de ce qui est la partie fraction de la dérivée (derivator) est sélectionnée,
        // alors on sélectionne tout.
        // Si la function et dérivator ont qque chose de sélectionné, alors on sélectionne tout.
        if (derivator.isSelected() || (func.gotSelectedElements() && derivator.gotSelectedElements())) {
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
            //if (!(display.getListener() instanceof Formula))
            FormulaTreeStructure fts = (FormulaTreeStructure) display.getListener();
            if (fts.getFather() != null)
                ((DisplayLayout) display.getLayout()).validateSelection();
        }

        // On met à jour l'affichage.
        display.repaint();
        */
    }
    
    /**
    * Checks the validity of the deselection.
    * @param display the display to deselect.
    */
    public void validateDeselection(Display display) {
        /*
        // father est le display de l'opérateur unaire.
        Display father = displayToLay;
        Display tmp;
        SelectionEvent selEvt = new SelectionEvent(father);
        
        // Si l'opérateur en entier est sélectionné, il faut le désélectionner
        if (father.isSelected()) {
            father.deselect();
            // On enlève le display père de la liste des display sélectionnés.
            selEvt.setAction(SelectionEvent.REMOVE, father);
            father.fireSelectionEvent(selEvt);
            
            // Si display est l'opérateur (on vient de déselectionner l'opérateur), alors
            // on ajoute l'opérande dans la liste des sélectionnés.
            if (display == displayToLay.getComponent(0)) {
                ((Display) displayToLay.getComponent(1)).select();
                selEvt.setAction(SelectionEvent.ADD, (Display) displayToLay.getComponent(1));
                father.fireSelectionEvent(selEvt);
            }                
            
            // Comme pour la sélection, on contrôle la validité de la désélection.
            if (father.getParent() instanceof Display) {
                father = (Display) father.getParent();
                //((DisplayLayout) father.getLayout()).validateDeselection((Display) display.getParent());
                if (!(father.getListener() instanceof Formula))
                    ((DisplayLayout) father.getLayout()).validateDeselection(displayToLay);
            }
            
            // Hé oui, on contrôle la validité de la sélection... dans une désélection.
            // Toujours le même pb, est-ce que le nouvel état de la sélection (après
            // désélection donc) est syntaxiquement cohérent ?
            validateSelection();
            
            // On met à jour l'affichage.
            father.repaint();
        } 
        */
    }
        
    
    /**
    * Computes the size of the display according to its children size (if any),
    * and its different attributes.
    * @return the size of the display.
    */
    public Dimension computeAttributes() {
        Display towards = (Display) displayToLay.getComponent(0);
        towards = (Display) towards.getComponent(1);
        
        if (displayToLay.getComponentCount() > 2) {
            // Dans l'ordre, displayToLay contient [[lim, []], fct, var, borne]
            // Il faut mettre var et borne, comme fils au display frère de lim.
            // Ce display a un LM qui rajoutera une --> entre var et borne.
            // On aura donc [[lim, [var, rightArrow, borne]], fct]
            
            Display d;
            d = (Display) displayToLay.getComponent(2);
            // towards.add(d) appelle un displayToLay.remove(d).
            // remove(d), enlève également d de la liste des listeners de l'objet (fts) qu'il écoutait.
            // Ce qu'on ne veut pas, puisqu'il s'agit d'un simple déplacement de display. 
            d.setDoRemoveFromListListeners(false);
            towards.add(d);
            d.setDoRemoveFromListListeners(true);
            //displayToLay contient [[lim [var]], fct, borne]
            
            d = (Display) displayToLay.getComponent(2);
            // towards.add(d) appelle un displayToLay.remove(d).
            // remove(d), enlève également d de la liste des listeners de l'objet (fts) qu'il écoutait.
            // Ce qu'on ne veut pas, puisqu'il s'agit d'un simple déplacement de display. 
            d.setDoRemoveFromListListeners(false);
            towards.add(d);
            d.setDoRemoveFromListListeners(true);
            //displayToLay contient [[lim [var, borne]], fct]            
        }        
        
        ((Display) displayToLay.getComponent(1)).setShiftX(5);
        updateLevel(displayToLay.getLevel());
        Dimension dim = super.computeAttributes();
        
        return new Dimension(dim.width + 5, dim.height);
    }
    
    /**
    * The display needs to be rebuilt. We do this.
    */
    public void rebuildDisplay() {
        // La taille des displays est probablement différente de ceux qui étaient
        // précédemment. On demande alors le recalcul des display ancêtres.
        displayToLay.computeAncestorsAttributes();
    }
}


class SymbolLimLayout extends VerticalCenteredLayout {
    /**
    * Computes the size of the display according to its children size (if any),
    * and its different attributes.
    * @return the size of the display.
    */
    public Dimension computeAttributes() {
        Dimension dim = super.computeAttributes();
        
        displayToLay.setAscent(((Display) displayToLay.getComponent(0)).getAscent());
        displayToLay.setDescent(displayToLay.getHeight() - displayToLay.getAscent());
        
        return dim;
    }
    
    /**
    * Checks the validity of the selection.
    */
    public void validateSelection() {
        // To Do !!!!
    }
     /**
    * Checks the validity of the deselection.
    * @param display the display to deselect.
    */
    public void validateDeselection(Display display) {
        // To Do !!!!
    }
    
   /**
    * The display needs to be rebuilt. We do this.
    */
    public void rebuildDisplay() {
        // La taille des displays est probablement différente de ceux qui étaient
        // précédemment. On demande alors le recalcul des display ancêtres.
        displayToLay.computeAncestorsAttributes();
    }
}