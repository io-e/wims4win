/*
$Id: InfixOperatorLayout.java,v 1.3 2003/02/18 11:48:46 sander Exp $
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
import fr.ove.openmath.jome.model.*;

/**
* A layout manager that lays components horizontally and inserts between
* them, the display of the operator that the layout manager lays.
*
* @author © 1999 DIRAT Laurent
* @version 2.0  13/12/1999
*/
public abstract class InfixOperatorLayout extends HorizontalLayout implements OperatorDisplayCreator {    
    /**
    * Do we need to insert an operator display ?
    */
    protected boolean insertOperatorDisplay = true;
    
    /**
    * The number of the components already present before an other insertion
    * or removal. This is necessary to keep in mind this, in order to know if 
    * we must parse the list of components to check if we have to insert symbol
    * display or not.
    */
    protected int prevNbComponent = 0;
    
    /**
    * Inserts a display of the operator symbol that the display
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
    * Computes the size of the display according to its children size (if any),
    * and its different attributes.
    * @return the size of the display.
    */
    public Dimension computeAttributes() {
        // On regarde si le nombre de component dans le display a changé depuis
        // la dernière fois. Si oui, il faut contrôler s'il ne faut pas ajouter
        // des display d'opérateur.
        if (insertOperatorDisplay || (prevNbComponent != displayToLay.getComponentCount()))
            insertOperatorDisplay();
            
        return super.computeAttributes();
    }
        
    /**
    * The display needs to be rebuilt. We do this.
    */
    public void rebuildDisplay() {
        Display tmp;
        int nbDisplay = ((FormulaTreeStructure) displayToLay.getListener()).getNbChildren();
        Display listDisplay[] = new Display[nbDisplay];
        
        int count = displayToLay.getComponentCount();
        for (int i = 0; i < count; i++) {
            tmp = (Display) displayToLay.getComponent(i);
            if (!tmp.isSymbolOperatorDisplay()) {
                // A voir !!!!!
                // Mais il semblerai que bon, sinon à la (re)construction du display il se base
                // sur des anciennes valeurs, et donc pas terrible niveau affichage.
                tmp.setLocation(0, 0);
                listDisplay[((FormulaTreeStructure) tmp.getListener()).getRank()] = tmp;
            }
        }
        
        // ATTENTION : ici, on enlève les displays fils de display, mais on ne les enlève pas de la liste
        // des listeners de la fts qu'il sont en train d'écouter. Exception faite pour les displays d'opérateur.
        displayToLay.removeAllDisplays();
                
        for (int i = 0; i < nbDisplay; i++)
            displayToLay.add(listDisplay[i]);
            
        // On a reconstruit le display, il faut maintenant insérer les displays d'opérateur.
        insertOperatorDisplay = true;
        
        // La taille des displays est probablement différente de ceux qui étaient
        // précédemment. On demande alors le recalcul des display ancêtres.
        displayToLay.computeAncestorsAttributes();
    }
}
