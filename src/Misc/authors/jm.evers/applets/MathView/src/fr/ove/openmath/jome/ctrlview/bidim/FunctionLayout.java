/*
$Id: FunctionLayout.java,v 1.3 2003/02/18 11:48:46 sander Exp $
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
* Layout manager for rendering functions.
*
* @author © 1999 DIRAT Laurent
* @version 2.0 17/12/1999
*/
public class FunctionLayout extends UnaryOperatorLayout {
    /**
    * According to the operator, the layout manager has to add some components (e.g. brackets, ...)
    * or has to perform some "re-oganisation" before rendering.<BR>
    * As soon as the layout manager is set to the display, this mehtod MUST be called with the display laid out
    * as parameter. This method serves as well as a registering method. So all sub-classes of the instance MUST
    * call super.initDisplay(displayToLay).
    * @param displayToLay the display laid by the instance
    */
    public void initDisplay(Display displayToLay) {
        super.initDisplay(displayToLay);  // Par cet appel, on ajoute dans displayToLay le display du nom de la fonction
        // On va maintenant y rajouter un display, dans lequel on va mettre tous les displays correspondants
        // aux arguments de la fonction, display à qui on va affecter un EnumerationOperatorLayout, qui va se charger
        // d'afficher les arguments de la function de manière horizontale avec chacun des arguments séparés par une
        // virgule (un séparateur)
        Display argumentsDisplay = new BidimDisplay(displayToLay.getGraphicContext());
        // On met un listener à argumentsDisplay
        // En fait, il n'y en a pas besoin, dans le sens où il n'y a pas spécifiquement de fts qui
        // écoute le comportement de ce display. Néanmoins, il s'avère nécessaire qu'il en ait
        // un, par exemple lors de l'iconification, car c'est le display qui reçoit la demande
        // d'iconification qui envoie l'événement correspondant à la FTS. Or si ce display n'a pas
        // d'écouteur, alors pb. Par cohérence, l'écouteur du display d'opérateur, est le fts qui
        // représente cette opération. Par contre, la fts en question, n'écoute pas le display
        // d'opérateur.
        argumentsDisplay.addControlListener((FormulaTreeStructure) this.displayToLay.getListener());
        
        EnumerationLayout layout = new EnumerationLayout();
        layout.initDisplay(argumentsDisplay);
        argumentsDisplay.setLayout(layout);
        
        // On l'ajoute comme fils à display
        this.displayToLay.add(argumentsDisplay);
    }
    
    /**
    * Computes the size of the display according to its children size (if any),
    * and its different attributes.
    * @return the size of the display.
    */
    public Dimension computeAttributes() {
        updateLevel(displayToLay.getLevel());
        
        if (displayToLay.getComponentCount() > 2) {
            // Dans l'ordre, displayToLay contient [displayOperator, argumentsDisplay, var1, var2, ...]
            // Il faut mettre tous les vari dans argumentsDisplay.
            
            Display argumentsDisplay = (Display) displayToLay.getComponent(1);
            Display d;
            for (int i = 2; i < displayToLay.getComponentCount(); ) {
                d = (Display) displayToLay.getComponent(i);
                // argumentsDisplay.add(d) appelle un displayToLay.remove(d).
                // remove(d), enlève également d de la liste des listeners de l'objet (fts) qu'il écoutait.
                // Ce qu'on ne veut pas, puisqu'il s'agit d'un simple déplacement de display. 
                d.setDoRemoveFromListListeners(false);
                argumentsDisplay.add(d);
                // On remet le comportement de suppression par défault.
                d.setDoRemoveFromListListeners(true);
            }
        }
        
        return super.computeAttributes();
    }
    
    /**
    * Returns the display of the operator
    */
    public Display createOperatorDisplay() {
        Operator fts = (Operator) displayToLay.getListener();
        // On créé un display pour l'opérateur.
        // Attention, fts.getOperatorName() retourne le nom de la fonction, mais "(" y est collée.
        // Il faut donc la supprimer.
        String funcName = fts.getTheOperator();
        funcName = funcName.substring(0, funcName.length()-1);
        
        StringDisplay stringDisplay = new StringDisplay(displayToLay.getGraphicContext(), funcName, true);
        stringDisplay.setIsSymbolOperatorDisplay(true);
        
        return stringDisplay;
        
        /*
        Display operatorDisplay;
        if (ImageLoader.exists(funcName)) {
            SpecialSymbolView symbolOperator = new SpecialSymbolView(funcName, displayToLay);
            operatorDisplay = new SymbolDisplay(displayToLay.getGraphicContext(), symbolOperator);
            operatorDisplay.setIsSymbolOperatorDisplay(true);
        }
        else
            operatorDisplay = new StringDisplay(displayToLay.getGraphicContext(), funcName, true);
            
        return operatorDisplay;
        
        // En fait, si au nom de la fonction est associée une image (e.g. alpha), alors le nom de la func
        // est représenté par l'image.
        */
    }
}
