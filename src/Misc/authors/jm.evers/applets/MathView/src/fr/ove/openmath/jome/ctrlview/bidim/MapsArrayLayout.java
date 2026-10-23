/*
$Id: MapsArrayLayout.java,v 1.3 2003/02/18 11:48:46 sander Exp $
modified MapsToSigmaLayout.java
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
import fr.ove.openmath.jome.ctrlview.bidim.DisplayLayout;
import fr.ove.openmath.jome.ctrlview.bidim.HorizontalLayout;
import fr.ove.openmath.jome.ctrlview.bidim.selection.events.SelectionEvent;
import fr.ove.openmath.jome.model.*;

/**
* A layout manager that lays components to place them as the different elements like the operator
* sum does.<BR>
* The different elements are contained in the display in the following order :
* <UL>
* <LI>the symbol of the operator.a blank gif...</LI>
* <LI>the lower bound.</LI>
* <LI>the upper bound.</LI>
*</UL>
*
* Bounds are optional elements.
*
* @author © 1999 DIRAT Laurent
* @version 2.0 16/12/1999
*/
public abstract class MapsArrayLayout extends HorizontalLayout  implements OperatorDisplayCreator {
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
        
        Display array = createOperatorDisplay();
        array.addControlListener(this.displayToLay.getListener());
        this.displayToLay.add(array);
    }
    
    /**
    * Updates the level of the display that is layed out.<BR>
    * @param level the level put to the display
    */
    public void updateLevel(int level) {
        Display tmp;
        
        if (displayToLay.getUpdateLevel()) {            
            displayToLay.updateChildrenLevel();
            
            // On met à jour le niveau du display géré par le LM.
            displayToLay.setLevel(level);
            displayToLay.setUpdateLevel(false);
            
            // Le symbole a le même niveau que le display
            tmp = (Display) displayToLay.getComponent(0);
            ((DisplayLayout) tmp.getLayout()).updateLevel(level);
            tmp.setUpdateLevel(false);
            int count = displayToLay.getComponentCount();
            if (count > 1) {
                // La borne inférieure a le niveau du dispay +1
                tmp = (Display) displayToLay.getComponent(1);
                ((DisplayLayout) tmp.getLayout()).updateLevel(level+1);
                tmp.setUpdateLevel(false);
                if (count > 2) {
                    // La borne supérieure a le niveau du dispay +1
                    tmp = (Display) displayToLay.getComponent(2);
                    ((DisplayLayout) tmp.getLayout()).updateLevel(level+1);
                    tmp.setUpdateLevel(false);
                }
            }
        }
    }

    /**
    * Checks the validity of the selection.
    * @param display the display to select.
    */
    public void validateSelection() {
        Display symbol = (Display) displayToLay.getComponent(0);
        //Display expression = (Display) displayToLay.getComponent(1);
        //symbool=0 onder=1 boven=2
        int count = displayToLay.getComponentCount();
        
        // Si le symbole est sélectionné, on sélectionne tout le display
        if (symbol.isSelected())
            displayToLay.select();
        else if (count > 1) {
            Display lower = (Display) displayToLay.getComponent(1);
            Display upper;
            
            if (lower.gotSelectedElements()) {
                if (count > 2) {
                    upper = (Display) displayToLay.getComponent(2);
                    if (upper.gotSelectedElements())
                        displayToLay.select();
                }
            }
        }
        
        // Quand on est ici, si display est sélectionné, alors c'est qu'on a validé un des tests
        // ci-dessus. On doit alors mettre à jour le gestionnaire de sélections, et y ajouter display
        if (displayToLay.isSelected()) {
            SelectionEvent selEvt = new SelectionEvent(displayToLay);
            // On purge la liste des éléments sélectionnés.
            selEvt.setAction(SelectionEvent.PURGE, null);
            displayToLay.fireSelectionEvent(selEvt);
            // On y ajoute notre puissance.
            selEvt.setAction(SelectionEvent.ADD, displayToLay);
            displayToLay.fireSelectionEvent(selEvt);
        }

        // On a vérifié la validité de la sélection de la puissance. On doit maitenant
        // la contrôler au niveau supérieur, au niveau du père.
        Display display = displayToLay;
        if (display.getParent() instanceof Display) {
            display = (Display) display.getParent();
            //if (!(display.getListener() instanceof Formula))
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
        
        if (father.isSelected()) {
            father.setNotSelected();
            // On enlève le display père de la liste des display sélectionnés.
            selEvt.setAction(SelectionEvent.REMOVE, father);
            father.fireSelectionEvent(selEvt);
            int count = father.getComponentCount();
            ((Display) father.getComponent(0)).setNotSelected();// On désélectionne le symbole.
            if (count > 1) {
                ((Display) father.getComponent(1)).deselect();// On désélectionne la borne inf.
                if (count > 2) 
                    ((Display) father.getComponent(2)).deselect();// On désélectionne la borne sup.
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
        int ascent = 0;
        int descent = 0;

        updateLevel(displayToLay.getLevel());

        int width = 0;
        int height = 0;
        
        Display symbol, lower, upper;
        int gap = 0;
        
        // On calcule la taille du symbole
        symbol = (Display) displayToLay.getComponent(0);
        symbol.setSize(symbol.getPreferredSize());
        
        int width1 = symbol.getWidth();
        int count = displayToLay.getComponentCount();
        if (count > 1) {
            // On calcule la taille de la borne inférieure
            lower = (Display) displayToLay.getComponent(1);
            lower.setSize(lower.getPreferredSize());
            lower.setShiftY(symbol.getDescent() + lower.getAscent() + gap);
            
            width1 = Math.max(lower.getWidth(), width1);
            
            if (count > 2 ) {
                // On calcule la taille de la borne supérieure
                upper = (Display) displayToLay.getComponent(2);
                upper.setSize(upper.getPreferredSize());
                width1 = Math.max(upper.getWidth(), width1);
                upper.setShiftX(-(lower.getWidth() + upper.getWidth()) / 2);
                upper.setShiftY(-(symbol.getAscent() + upper.getDescent() + gap));
                ascent = symbol.getAscent() + upper.getHeight() + gap;
            }
            else {
                ascent = symbol.getAscent();
            }
            
            symbol.setShiftX((width1 - symbol.getWidth()) / 2);
            lower.setShiftX(-(symbol.getWidth() + lower.getWidth())/2 );
            descent = symbol.getDescent() + lower.getHeight() + gap;
        }
        else {
            ascent = symbol.getAscent();
            descent = symbol.getDescent();
        }
                
        displayToLay.setAscent(ascent);
        displayToLay.setDescent(descent);
        
        width += width1 + 4*gap; // 1 gap entre symbole et expression, et 3 après expression  
        
        height += ascent + descent;
        displayToLay.setSize(width , height);
        
        displayToLay.setComputeAttributes(false);
        return new Dimension(width, height);
    }
    
    /**
    * The display needs to be rebuilt. We do this.
    */
    public void rebuildDisplay() {
        
        //Faudra faire ce qui faut !!!!
        
        // La taille des displays est probablement différente de ceux qui étaient
        // précédemment. On demande alors le recalcul des display ancêtres.
        displayToLay.computeAncestorsAttributes();
    }
   
}
