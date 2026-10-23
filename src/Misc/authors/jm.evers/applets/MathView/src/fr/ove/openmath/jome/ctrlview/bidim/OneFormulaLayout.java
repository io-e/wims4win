/*
$Id: OneFormulaLayout.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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
import fr.ove.openmath.jome.ctrlview.bidim.selection.events.SelectionEvent;
import fr.ove.openmath.jome.model.*;

/**
* A layout manager that lays the display of a formula.
*
* @author © 1999 DIRAT Laurent
* @version 2.0 22/07/1999
*/
public class OneFormulaLayout extends DisplayLayout {
    /**
    * Computes the size of the display according to its children size (if any),
    * and its different attributes.
    * @return the size of the display.
    */
    public Dimension computeAttributes() {
        int width = 10;  // Ca c'est parce que c'est la taille préferée du display de la formule
        int height = 10; // faudra voir de modifier ça de manière plus propre.
        
        if (displayToLay.getComponentCount() != 0) {
            updateLevel(displayToLay.getLevel());
            Display display = (Display) displayToLay.getComponent(0);
            display.setSize(display.getPreferredSize());
            width = 2*display.getShiftX() + display.getWidth();
            height = 2*display.getShiftY() + display.getHeight();

            displayToLay.setAscent(display.getShiftY() + display.getAscent());
            displayToLay.setDescent(height - display.getAscent());
            
            displayToLay.setSize(width , height);
            displayToLay.setComputeAttributes(false);
        }
        
        return new Dimension(width , height);
    }
    
    /**
    * Selects the display (and its children if any)
    * @param the display to select.
    */
    public void selectDisplay() {
        Display display = (Display) displayToLay.getComponent(0);
        display.select();
        
        SelectionEvent selEvt = new SelectionEvent(display);
        // On purge la liste des éléments sélectionnés.
        selEvt.setAction(SelectionEvent.PURGE, null);
        display.fireSelectionEvent(selEvt);
        selEvt.setAction(SelectionEvent.ADD, display);
        display.fireSelectionEvent(selEvt);
    }


    /**
    * Deselects the display.
    * @param the display to deselect.
    */
    public void deselectDisplay() {
        Display display = (Display) displayToLay.getComponent(0);
        display.deselect();
        
        SelectionEvent selEvt = new SelectionEvent(display);
        selEvt.setAction(SelectionEvent.PURGE, null);
        display.fireSelectionEvent(selEvt);
    }
    
    /**
    * Checks the validity of the selection.
    */
    public void validateSelection() {
    }
    
    /**
    * Checks the validity of the deselection.
    * @param display the display to deselect.
    */
    public void validateDeselection(Display display) {
    }

    /**
    * The display needs to be rebuilt. We do this.
    */
    public void rebuildDisplay() {
        // La taille des displays est probablement différente de ceux qui étaient
        // précédemment. On demande alors le recalcul des display ancêtres.
        displayToLay.computeAncestorsAttributes();
    }
   
    // #################################################
    // ### Implémentation des différentes interfaces ###
    // #################################################

    // ***************************************************
    // *** Implémentation de l'interface LayoutManager ***


    /*
     * Lays out the container in the specified panel.
     * @param parent the component which needs to be laid out
     */
     public void layoutContainer(Container parent) {
        // Ca c'est parce que c'est le layout manager de la formule. Et comme personne ne dit au container
        // contenant la formule, que la taille de la formule a besoin d'être recalculée, il faut bien le
        // faire qque part. Chaque display informe son père qu'il doit être recalculé, mais la formule ne
        // le fait pas pour le sien.
        parent.setSize(parent.getPreferredSize());
        
        if (parent.getComponentCount() != 0) {
            // On est obligé de faire ce test car comme lorsque la formule est "vide", on ne met pas template,
            // parent ne contient aucun display fils.
            
            Display display = (Display) parent.getComponent(0);
            display.setSize(display.getPreferredSize());
            display.setLocation(display.getShiftX(), display.getShiftY());

            // On place maintenant les éventuels display enfants du display courant
            display.doLayout();
        }
    }

    // *** Fin de l'interface LayoutManager ***
    // ****************************************
}
