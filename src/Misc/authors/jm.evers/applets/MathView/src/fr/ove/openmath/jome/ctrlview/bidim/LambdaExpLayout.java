/*
$Id: LambdaExpLayout.java,v 1.3 2003/02/18 11:48:46 sander Exp $
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
* A layout manager that lays components of a function.
*
* @author © 1999 DIRAT Laurent
* @version 2.0 20/07/1999
*/
public class LambdaExpLayout extends HorizontalLayout {
    private boolean arrangeDisplay = true; // Au premier calcul, il faudra arranger les displays correctement
    private Display enumeration = null;
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
        
        // La fts associée au display
        LambdaExpression fts = (LambdaExpression) this.displayToLay.getListener();
        GraphicContext gc = this.displayToLay.getGraphicContext();
        // Display temporaire pour manipulations
        Display displayElems = null;
        if (!fts.isJustFunction()) {
            if (fts.getNbChildren() > 2) {
                // Alors il faut qu'on ajoute un display supplémentaire, qui contiendra les variables liées
                // du bind. Ce display aura un EnumerationLayout.
                enumeration = new BidimDisplay(gc);
                enumeration.addControlListener(fts);
                EnumerationLayout layout = new EnumerationLayout();
                layout.initDisplay(enumeration);
                enumeration.setLayout(layout);
                this.displayToLay.add(enumeration);
            }
            
            // Le symbole de la flèche
            SymbolDisplay arrow = new SymbolDisplay(gc, new ImageSymbol("RightArrow", this.displayToLay));
            // Le display de la somme est le display d'un opérateur (on peut le considérer comme tel)
            arrow.setIsSymbolOperatorDisplay(true);
            this.displayToLay.add(arrow);
            arrow.addControlListener(fts);
        }
        
        // De part la méthode d'allocation des displays, si fts.isJustFunction() est vrai, alors display n'a
        // pour le moment pas de fils.
        // Si fts.getNbChildren() > 2, alors display contient, dans cet ordre [enumeration, arrow].
        // Sinon, display contient seulement [arrow]
        // Donc selon les cas, après allocation complète, display sera de la forme :
        // [ftc, var1, var2,....] : il faut donc virer les var* (puisqu'en fait <=> fts.isJustFunction() == true)
        // [arrow, fct, var1] : juste faire un shiftX pour que affichage soit de la forme [var1, arrow, fct]
        // [enumeration, arrow, fct, var1, var2, ...] : il faut mettre les var* comme fils de enumeration
        
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
        updateLevel(displayToLay.getLevel());
        LambdaExpression fts = (LambdaExpression) displayToLay.getListener();
        
        if (displayToLay.getComponentCount() > 3) {
        //if (arrangeDisplay) {
            if (!fts.isJustFunction()) {
                if (fts.getNbChildren() > 2) {
                    // On enlève tous les displays correspondant en fait aux variables, de displayToLay,
                    // pour le mettre comme fils à enumeration
                    //Display enumeration = (Display) displayToLay.getComponent(0);
                    Display d;
                    for (int i = 3; i < displayToLay.getComponentCount(); ) {
                        d = (Display) displayToLay.getComponent(i);
                        // enumeration.add(d) appelle un displayToLay.remove(d).
                        // remove(d), enlève également d de la liste des listeners de l'objet (fts) qu'il écoutait.
                        // Ce qu'on ne veut pas, puisqu'il s'agit d'un simple déplacement de display. 
                        d.setDoRemoveFromListListeners(false);
                        enumeration.add(d);
                        // On remet le comportement de suppression par défault.
                        d.setDoRemoveFromListListeners(true);
                    }
                }
            }
            else {
                // On enlève tous les displays correspondant en fait aux variables.
                for (int i = 1; i < displayToLay.getComponentCount(); )
                    displayToLay.remove(i);
            }
            arrangeDisplay = false;
        }        
        
        Dimension dim = super.computeAttributes();
        
        if ((!fts.isJustFunction()) && (fts.getNbChildren() == 2)) {
            // en fait un truc du style lambda(sin(x), x), displayToLay est de a forme [arrow, sin(x), x]
            // faut faire en sorte qsue ce soit affiché [x, arrow, sin(x)]
            ((Display) displayToLay.getComponent(0)).setShiftX(((Display) displayToLay.getComponent(2)).getWidth());
            ((Display) displayToLay.getComponent(2)).setShiftX(-dim.width);
        }
                
        return dim;
    }
    
    /**
    * The display needs to be rebuilt. We do this.
    */
    public void rebuildDisplay() {
        // Il y a besoin de reconstruire le display, seulement lorsque nous avons un lambda expression avec
        // une variable. Au départ, display est de la forme [arrow, fct, var]. L'iconification modifie
        // display de telle sorte qu'il devient [arrow, var, icon].
        // Pour que cela colle avec le computeAttributes() il faut remettre dans l'ordre [arrox, icon, var]
        LambdaExpression fts = (LambdaExpression) displayToLay.getListener();
        if (!fts.isJustFunction() && (fts.getNbChildren() == 2)) {
            Display variable = (Display) displayToLay.getComponent(1);
            // On l'enlève mais on ne veut pas l'enlever de la liste des listeners dont elle fait partie
            variable.setDoRemoveFromListListeners(false);
            displayToLay.remove(variable);
            // On remet le comportement par défaut
            variable.setDoRemoveFromListListeners(true);
            // On rajoute la variable à la fin de display
            displayToLay.add(variable);
            
            int count = displayToLay.getComponentCount();
            for (int i = 0; i < count; i++)
                ((Display) displayToLay.getComponent(i)).setShiftX(0);
        }
            
    
        // La taille des displays est probablement différente de ceux qui étaient
        // précédemment. On demande alors le recalcul des display ancêtres.
        displayToLay.computeAncestorsAttributes();
    }

}
