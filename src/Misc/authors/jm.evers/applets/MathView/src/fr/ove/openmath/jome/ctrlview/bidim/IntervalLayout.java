/*
$Id: IntervalLayout.java,v 1.3 2003/02/18 11:48:46 sander Exp $
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
import fr.ove.utils.FontInfo;

/**
* Temporaire
*
* @author © 1999 DIRAT Laurent
* @version 2.0 06/09/1999
*/
public class IntervalLayout extends EnclosingLayout {
    /**
    * According to the operator, the layout manager has to add some components (e.g. brackets, ...)
    * or has to perform some "re-oganisation" before rendering.<BR>
    * As soon as the layout manager is set to the display, this mehtod MUST be called with the display laid out
    * as parameter. This method serves as well as a registering method. So all sub-classes of the instance MUST
    * call super.initDisplay(displayToLay).
    * @param displayToLay the display laid by the instance
    */
    public void initDisplay(Display displayToLay) {
        super.initDisplay(displayToLay);  // Par cet appel, on ajoute dans displayToLay les crochets
        // On va maintenant y rajouter un display, dans lequel on va mettre les displays correspondants
        // aux bornes de l'intervalle, display à qui on va affecter un SeparatorOperatorLayout, qui va se charger
        // d'afficher entre chacun des éléments une virgule (un séparateur)
        Display boundsDisplay = new BidimDisplay(displayToLay.getGraphicContext());
        // On met un listener à boundsDisplay
        // En fait, il n'y en a pas besoin, dans le sens où il n'y a pas spécifiquement de fts qui
        // écoute le comportement de ce display. Néanmoins, il s'avère nécessaire qu'il en ait
        // un, par exemple lors de l'iconification, car c'est le display qui reçoit la demande
        // d'iconification qui envoie l'événement correspondant à la FTS. Or si ce display n'a pas
        // d'écouteur, alors pb. Par cohérence, l'écouteur du display d'opérateur, est le fts qui
        // représente cette opération. Par contre, la fts en question, n'écoute pas le display
        // d'opérateur.
        boundsDisplay.addControlListener((FormulaTreeStructure) displayToLay.getListener());
        
        SeparatorOperatorLayout layout = new SeparatorOperatorLayout();
        layout.initDisplay(boundsDisplay);
        boundsDisplay.setLayout(layout);
        
        // On l'ajoute comme fils à display
        this.displayToLay.add(boundsDisplay);
    }
    
    /**
    * Returns the opening
    */
    public SymbolDisplay createOpening() {
        // On récupère le fts associé (listener) au display.
        KaryOperator fts = (KaryOperator) displayToLay.getListener();
        
        SymbolDisplay bracket = new SymbolDisplay(displayToLay.getGraphicContext(), 
                                                  new SquaredBracketSymbol(fts.getTheOperator(), displayToLay));            
        // Le display de l'accolade est le display d'un opérateur (on peut le considérer comme tel)
        bracket.setIsSymbolOperatorDisplay(true);
        
        return bracket;
    }
    
    /**
    * Returns the closing
    */
    public SymbolDisplay createClosing() {
        // On récupère le fts associé (listener) au display.
        KaryOperator fts = (KaryOperator) displayToLay.getListener();
        
        SymbolDisplay bracket = new SymbolDisplay(displayToLay.getGraphicContext(), 
                                                  new SquaredBracketSymbol(fts.getEnding(), displayToLay));            
        // Le display de l'accolade est le display d'un opérateur (on peut le considérer comme tel)
        bracket.setIsSymbolOperatorDisplay(true);
        
        return bracket;
    }    
    
    /**
    * Computes the size of the display according to its children size (if any),
    * and its different attributes.
    * @return the size of the display.
    */
    public Dimension computeAttributes() {
        Display display = null;
        
        if (displayToLay.getComponentCount() > 3) {
            Display boundsDisplay = (Display) displayToLay.getComponent(2);
            Display d;
            for (int i = 3; i < displayToLay.getComponentCount(); ) {
                d = (Display) displayToLay.getComponent(i);
                // elementsDisplay.add(d) appelle un displayToLay.remove(d).
                // remove(d), enlève également d de la liste des listeners de l'objet (fts) qu'il écoutait.
                // Ce qu'on ne veut pas, puisqu'il s'agit d'un simple déplacement de display. 
                d.setDoRemoveFromListListeners(false);
                boundsDisplay.add(d);
                // On remet le comportement de suppression par défault.
                d.setDoRemoveFromListListeners(true);
            }
        }
        
        SymbolDisplay leftBracket = (SymbolDisplay) displayToLay.getComponent(0);
        SquaredBracketSymbol leftSymbol = (SquaredBracketSymbol) leftBracket.getSymbol();
        SymbolDisplay rightBracket = (SymbolDisplay) displayToLay.getComponent(1);
        SquaredBracketSymbol rightSymbol = (SquaredBracketSymbol) rightBracket.getSymbol();
        
        // On met la hauteur à 0, pour que dans tous les cas, le super.computeAttributes() ne soit pas
        // biaisée par une éventuelle nouvelle taille des éléments qui composent la liste.
        // Comme c'est plus loin qu'on leur fixe la taille....
        leftSymbol.setHeight(0);
        rightSymbol.setHeight(0);
        
        Dimension dim = super.computeAttributes();
        
        // Par contre, par rapport à comment est calculée la taille des accolades, dim comprend
        // déjà la taille des accolades
        int ascent = displayToLay.getAscent();
        int descent = displayToLay.getDescent();
        int height =  displayToLay.getHeight();
        int thickness = FontInfo.getLineThickness(displayToLay, displayToLay.getFont());
        
        leftSymbol.setAscent(ascent);
        leftSymbol.setDescent(descent);
        leftSymbol.setHeight(height);
        leftSymbol.setThickness(thickness);
        leftBracket.setComputeAttributes(true);
        leftBracket.invalidate();
        leftBracket.setSize(leftBracket.getPreferredSize());

        rightSymbol.setAscent(ascent);
        rightSymbol.setDescent(descent);
        rightSymbol.setHeight(height);
        rightSymbol.setThickness(thickness);
        rightBracket.setComputeAttributes(true);
        rightBracket.invalidate();
        rightBracket.setSize(rightBracket.getPreferredSize());
        
        // Cette taille là correspond au décalage qu'il faut appliquer à la dernière accolade
        rightBracket.setShiftX(dim.width - 2*rightBracket.getWidth());
        // Mais il faut l'enlever au premier terme de la liste qui doit être contre la première
        // accolade (because HonrizontalLayout)
        ((Display) displayToLay.getComponent(2)).setShiftX(-rightBracket.getShiftX() - rightBracket.getWidth());
        
        return dim;
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
