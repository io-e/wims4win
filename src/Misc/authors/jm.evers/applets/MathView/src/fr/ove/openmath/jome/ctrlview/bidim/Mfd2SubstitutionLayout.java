/*
$Id: Mfd2SubstitutionLayout.java,v 1.3 2003/02/18 11:48:46 sander Exp $
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
import fr.ove.openmath.jome.ctrlview.bidim.Display;
import fr.ove.openmath.jome.ctrlview.bidim.DisplayLayout;
import fr.ove.openmath.jome.ctrlview.bidim.selection.events.SelectionEvent;
import fr.ove.openmath.jome.model.*;

/**
* A layout manager that lays components horizontally and inserts between
* them, the display of the operator that the layout manager lays.<BR>
* For example, if this layout manager lays an equality, it will insert
* between the components (i.e. operands) the display of the symbol "=",
* and so on, for all the relational operators.<BR>
*
* @author © 1999 DIRAT Laurent
* @version 2.0 17/12/1999
*/
public class Mfd2SubstitutionLayout extends HorizontalLayout {
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
        
        // On ajoute un display d'opérateur.
        SymbolDisplay arrow = new SymbolDisplay(this.displayToLay.getGraphicContext(), new ImageSymbol("LeftArrow", this.displayToLay));
        
        // On met un listener à l'opérateur.
        // En fait, il n'y en a pas besoin, dans le sens où il n'y a pas spécifiquement de fts qui
        // écoute le comportement de ce display. Néanmoins, il s'avère nécessaire qu'il en ait
        // un, par exemple lors de l'iconification, car c'est le display qui reçoit la demande
        // d'iconification qui envoie l'événement correspondant à la FTS. Or si ce display n'a pas
        // d'écouteur, alors pb. Par cohérence, l'écouteur du display d'opérateur, est le fts qui
        // représente cette opération. Par contre, la fts en question, n'écoute pas le display
        // d'opérateur.
        arrow.addControlListener(this.displayToLay.getListener());
        arrow.setIsSymbolOperatorDisplay(true);
        this.displayToLay.add(arrow);
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
    * Computes the size of the display according to its children size (if any),
    * and its different attributes.
    * @return the size of the display.
    */
    public Dimension computeAttributes() {
        Dimension dim = super.computeAttributes();
        
        Display arrow = (Display) displayToLay.getComponent(0);
        Display left = (Display) displayToLay.getComponent(1);
        Display right = (Display) displayToLay.getComponent(2);
        arrow.setShiftX(left.getShiftX() + left.getWidth());
        left.setShiftX(-(arrow.getShiftX() + arrow.getWidth()));
        right.setShiftX(arrow.getWidth());
        
        return dim;
    }
    
    /**
    * The display needs to be rebuilt. We do this.
    */
    public void rebuildDisplay() {
        Display tmp;
        Display listDisplay[] = new Display[2];  // y en a 2 (en ne comptant pas celui de la flèche)
        
        for (int i = 0; i < 3; i++) {
            tmp = (Display) displayToLay.getComponent(i);
            if (!tmp.isSymbolOperatorDisplay()) {
                // A voir !!!!!
                // Mais il semblerai que bon, sinon à la (re)construction du display il se base
                // sur des anciennes valeurs, et donc pas tarrible niveau affichage.
                tmp.setLocation(0, 0);
                listDisplay[((FormulaTreeStructure) tmp.getListener()).getRank()] = tmp;
            }
        }
        
        // Pour que la flèche soient gardée, on la met temporairement comme un display "classique".
        // Autrement dit, ce n'est plus un display d'opérateur.
        ((Display) displayToLay.getComponent(0)).setIsSymbolOperatorDisplay(false);
        
        // ATTENTION : ici, on enlève les displays fils de display, mais on ne les enlève pas de la liste
        // des listeners de la fts qu'il sont en train d'écouter. Exception faite pour les displays d'opérateur.
        displayToLay.removeAllDisplays();
                
        // On remet la flèche dans un état qui est le sien, i.e. display d'opérateur.
        ((Display) displayToLay.getComponent(0)).setIsSymbolOperatorDisplay(true);
        
        for (int i = 0; i < 2; i++)
            displayToLay.add(listDisplay[i]);
            
        // La taille des displays est probablement différente de ceux qui étaient
        // précédemment. On demande alors le recalcul des display ancêtres.
        displayToLay.computeAncestorsAttributes();
    }
   
}
