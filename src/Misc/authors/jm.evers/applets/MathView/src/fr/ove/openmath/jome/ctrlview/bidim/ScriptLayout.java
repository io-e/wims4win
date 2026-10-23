/*
$Id: ScriptLayout.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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
* A layout manager that lays components horyzontally, but the last one in the
* list, is treated as a superscript or an underscript in the inherited classes.
* (There is a shift calculated)
*
* @author © 1999 DIRAT Laurent
* @version 2.0 19/07/1999
*/
public abstract class ScriptLayout extends HorizontalLayout {
    /**
    * Updates the level of the display that is layed out.<BR>
    * @param level the level put to the display
    */
    public void updateLevel(int level) {
        Display tmp;
        
        // Pour éviter de refaire le calcul plusieurs fois....
        if (displayToLay.getUpdateLevel()) {
            // On met le niveau adéquat
            displayToLay.setLevel(level);
            // On scale le display en fonction du niveau
            displayToLay.scaleDisplay();
            // On n'a plus besoin de mettre à jour le niveau de l'instance
            displayToLay.setUpdateLevel(false);
            
            // Le premier display fils possède le même niveau que son père.
            tmp = (Display) displayToLay.getComponent(0);
            ((DisplayLayout) tmp.getLayout()).updateLevel(level);
            
            // Le deuxième display fils possède le même niveau que la base + 1.
            // (on applique une réduction de taille)
            tmp = (Display) displayToLay.getComponent(1);
            ((DisplayLayout) tmp.getLayout()).updateLevel(level+1);
        }
    }
    
    /**
    * Checks the validity of the selection.
    */
    public void validateSelection() {
        SelectionEvent selEvt = new SelectionEvent(displayToLay);

        // La validité de la sélection est triviale.
        // Si un des éléments de la puissance est sélectionné, alors la sélection
        // de l'autre élément entraîne la sélection de toute la puissance.
        Display base = (Display) displayToLay.getComponent(0);
        Display script = (Display) displayToLay.getComponent(1);
        if (base.gotSelectedElements() && script.gotSelectedElements()) {
            displayToLay.select();
            // On purge la liste des éléments sélectionnés.
            selEvt.setAction(SelectionEvent.PURGE, null);
            displayToLay.fireSelectionEvent(selEvt);
            // On y ajoute notre puissance.
            selEvt.setAction(SelectionEvent.ADD, displayToLay);
            displayToLay.fireSelectionEvent(selEvt);
        }
        
        // On a vérifié la validité de la sélection de la puissance. On doit maitenant
        // la contrôler au niveau supérieur, au niveau du père.
        // Ex : on a l'expression A+B^C et on a A déjà sélectionné. On sélectionne C.
        // D'un pt de vue sémantique (oui, on va dire ça comme ça) A et C ne peuvent
        // être sélectionnés séparément. Tout doit être sélectionné. Et ça, c'est la 
        // validation de la sélection qui va le déterminer. oufff....
        Display display = displayToLay;
        if (displayToLay.getParent() instanceof Display) {
            display = (Display) displayToLay.getParent();
            FormulaTreeStructure fts = (FormulaTreeStructure) display.getListener();
            if (fts.getFather() != null) // Si fts n'est pas la racine de la formule, on valide la sélection
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
        Display tmp;
        SelectionEvent selEvt = new SelectionEvent(displayToLay);
        
        if (displayToLay.isSelected()) {
            // On désélectionne la puissance
            displayToLay.setNotSelected();
            // On enlève le display père de la liste des display sélectionnés.
            selEvt.setAction(SelectionEvent.REMOVE, displayToLay);
            displayToLay.fireSelectionEvent(selEvt);
            
            // Si la puissance toute entière était sélectionnée, donc présente dans la liste
            // des éléments sélectionné. Ce n'est pas le cas de la base et de l'exposant.
            // Donc en fonction de cela, il faut y ajouter l'élément adéquat
            if (((Display) displayToLay.getComponent(0)).isSelected())
                // On a demandé la désélection en cliquant sur l'exposant, ce qui revient
                // à sélectionner uniquement la base.
                // On l'ajoute donc dans la liste des display sélectionnés.
                selEvt.setAction(SelectionEvent.ADD, (Display) displayToLay.getComponent(0));
            else
                // On a demandé la désélection en cliquant sur la base, ce qui revient
                // à sélectionner uniquement l'exposant.
                // On l'ajoute donc dans la liste des display sélectionnés.
                selEvt.setAction(SelectionEvent.ADD, (Display) displayToLay.getComponent(1));

            displayToLay.fireSelectionEvent(selEvt);

            // Comme pour la sélection, on contrôle la validité de la désélection.
            Display father = displayToLay;
            if (displayToLay.getParent() instanceof Display) {
                father = (Display) displayToLay.getParent();
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
    * The display needs to be rebuilt. We do this.
    */
    public void rebuildDisplay() {
        Display tmp;
        Display base = null;
        Display script = null;
        // On parcourre la liste des displays pour trouver qui est qui.
        for (int i = 0; i < 2; i++) {
            tmp = (Display) displayToLay.getComponent(i);
            // On fait les initialisations qui s'imposent pour le bon repositionnement de chacun.
            tmp.setLocation(0,0);
            if (((FormulaTreeStructure) tmp.getListener()).getRank() == 0)
                base = tmp;
            else 
                script = tmp;
        }
        // On a retrouvé qui est qui, on reconstruit le display pour tout mettre
        // à la bonne position.
        displayToLay.removeAllDisplays();
        displayToLay.add(base);
        displayToLay.add(script);
        // La taille des displays est probablement différente de ceux qui étaient
        // précédemment. On demande alors le recalcul des display ancêtres.
        displayToLay.computeAncestorsAttributes();
    }
}
