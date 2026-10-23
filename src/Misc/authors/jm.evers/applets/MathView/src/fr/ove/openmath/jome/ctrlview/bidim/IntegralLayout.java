/*
$Id: IntegralLayout.java,v 1.3 2003/02/18 11:48:46 sander Exp $
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
* A layout manager that lays components to place them as an integral.<BR>
* The different elements (when complete) are contained in the display in the following order :
* <UL>
* <LI>the lower bound.</LI>
* <LI>the symbol integral.</LI>
* <LI>the upper bound.</LI>
* <LI>the expression.</LI>
* <LI>the integrand.</LI>
*</UL>
*
* @author © 1999 DIRAT Laurent
* @version 2.0 16/12/1999
*/
public class IntegralLayout extends HorizontalLayout {
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

        SymbolDisplay integral = new SymbolDisplay(this.displayToLay.getGraphicContext(), 
                                                new ImageSymbol("Integrale", this.displayToLay));
        // Le display de l'intégrale est le display d'un opérateur (on peut le considérer comme tel)
        integral.setIsSymbolOperatorDisplay(true);
        this.displayToLay.add(integral);
        integral.addControlListener((FormulaTreeStructure) this.displayToLay.getListener());
    }
    
    /**
    * Updates the level of the display that is layed out.<BR>
    * @param level the level put to the display
    */
    public void updateLevel(int level) {
        Display tmp;
        StringDisplay varIntDisplay;
        VariableOrNumber varInt;
        
        // Pour éviter de refaire le calcul plusieurs fois....
        if (displayToLay.getUpdateLevel()) {
            // On met à jour le niveau du display géré par le LM.
            displayToLay.setLevel(level);
            displayToLay.setUpdateLevel(false);
            
            int i = 0;
            int count = displayToLay.getComponentCount();
            // Si le nombre de composant est au moins 4, alors on a affaire à une intégrale où au moins une
            // borne est spécifiée (la borne inférieure)
            // On fait en sorte qu'elle soit en première position dans display,
            // sinon, à l'affichage, elle va être en partie masquée par le display de l'intégrale.
            if (count > 3) {
                // On regarde si le premier display est celui d'un opérateur (celui de l'intégrale en fait)
                // Si c'est le cas, il faut déplacer le display de la borne inf.
                if (((Display) displayToLay.getComponent(0)).isSymbolOperatorDisplay()) {
                    tmp = (Display) displayToLay.getComponent(3);
                    
                    // De part l'implémentation du add(tmp, tmp, 0), tmp va être enlevé de la liste
                    // des listeners qu'il écoute. Chose qu'on ne veut pas.
                    displayToLay.removeFromListListeners(false);
                    
                    displayToLay.add(tmp, tmp, 0);
                    
                    // On se remet dans la cas classique où la suppression de display entraînera sa suppression
                    // dans la liste des listeners qu'il écoute.
                    displayToLay.removeFromListListeners(true);
                    
                    displayToLay.adjustRank();
                }
                
                // On réduit la taille de la borne inf. de 1
                tmp = (Display) displayToLay.getComponent(0);
                ((DisplayLayout) tmp.getLayout()).updateLevel(level+1);
                tmp.setUpdateLevel(false);
                
                // On incrémente l'indice i, pour que dans la boucle qui suit, on parte bien du display
                // du tick
                i++;
                
                // Le display de la variable d'intégration est en 4-ième position
                tmp = (Display) displayToLay.getComponent(3);
                varIntDisplay = (StringDisplay) tmp.getComponent(0);
                
                if (count > 4) {
                    // C'est la borne supérieure. On réduit sa taille de 1
                    tmp = (Display) displayToLay.getComponent(4);
                    ((DisplayLayout) tmp.getLayout()).updateLevel(level+1);
                    tmp.setUpdateLevel(false);
                    
                    count--;
                }
            }
            else {
                // Le display de la variable d'intégration est en 3-ième position
                tmp = (Display) displayToLay.getComponent(2);
                varIntDisplay = (StringDisplay) tmp.getComponent(0);
            }
            
            // On récupère la fts de la variable d'intégration.
            // Si c'est un template, alors pas besoin de rajouter le d. Sinon, on le rajoute
            varInt = (VariableOrNumber) varIntDisplay.getListener();
            if (!varInt.isTemplate())
                varIntDisplay.setString("d" + varInt.getValue());
            
            // le symbole de l'intégrale, l'intégrande et la variable d'intégration (en fait le d"x")
            // ont le niveau du display qui les contient
            for (; i < count; i++) {
                tmp = (Display) displayToLay.getComponent(i);
                ((DisplayLayout) tmp.getLayout()).updateLevel(level);
                tmp.setUpdateLevel(false);
            }
        }
    }

    /**
    * Checks the validity of the selection.
    * @param display the display to select.
    */
    public void validateSelection() {
        int count = displayToLay.getComponentCount();
        Display symbol, expression, varInt;
        
        if (count > 3) {
            symbol = (Display) displayToLay.getComponent(1);
            expression = (Display) displayToLay.getComponent(2);
            varInt = (Display) displayToLay.getComponent(3);
        }
        else {
            symbol = (Display) displayToLay.getComponent(0);
            expression = (Display) displayToLay.getComponent(1);
            varInt = (Display) displayToLay.getComponent(2);
        }
        
        if ((symbol.isSelected()) || (varInt.isSelected()))
            displayToLay.select();
        else if (count > 3) {
            Display lower = (Display) displayToLay.getComponent(0);
            Display upper;
            
            if (lower.gotSelectedElements()) {
                if (expression.gotSelectedElements())
                    displayToLay.select();
                else if (count > 4) {
                    upper = (Display) displayToLay.getComponent(4);
                    if (upper.gotSelectedElements())
                        displayToLay.select();
                }
            }
            else if (count > 4) {
                upper = (Display) displayToLay.getComponent(4);
                if (upper.gotSelectedElements()) {
                    if (expression.gotSelectedElements())
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
            // On y ajoute notre intégrale.
            selEvt.setAction(SelectionEvent.ADD, displayToLay);
            displayToLay.fireSelectionEvent(selEvt);
        }

        // On a vérifié la validité de la sélection de l'intégrale. On doit maitenant
        // la contrôler au niveau supérieur, au niveau du père.
        Display display = displayToLay;
        if (display.getParent() instanceof Display) {
            display = (Display) display.getParent();
            FormulaTreeStructure fts = (FormulaTreeStructure) display.getListener();
            if (fts.getFather() != null) // Si fts est la racine de la formule alors la sélection à déjà été validée
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
        Display tmp;
        SelectionEvent selEvt = new SelectionEvent(father);
        
        if (father.isSelected()) {
            father.setNotSelected();
            // On enlève le display père de la liste des display sélectionnés.
            selEvt.setAction(SelectionEvent.REMOVE, father);
            father.fireSelectionEvent(selEvt);
            
            int count = father.getComponentCount();
            Display expression;
            
            if (count > 3)
                expression = (Display) father.getComponent(2);
            else
                expression = (Display) father.getComponent(1);
            
            if (display != expression) {
                // quand on désélectionne un élément de l'intégrale, autre que l'expression, l'expression
                // reste sélectionnée.
                selEvt.setAction(SelectionEvent.ADD, expression);
                father.fireSelectionEvent(selEvt);
            }
            
            if (count > 3) {
                ((Display) father.getComponent(0)).deselect();
                ((Display) father.getComponent(1)).setNotSelected();
                ((Display) father.getComponent(3)).deselect();
                if (count > 4) 
                    ((Display) father.getComponent(4)).deselect();
            }
            else {
                ((Display) father.getComponent(0)).setNotSelected();
                ((Display) father.getComponent(2)).deselect();
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
        
        SymbolDisplay symbol;
        Display expression, integrand;
        int gap = 4;
        
        int count = displayToLay.getComponentCount();
        if (count > 3) {
            symbol = (SymbolDisplay) displayToLay.getComponent(1);
            expression = (Display) displayToLay.getComponent(2);
            integrand = (Display) displayToLay.getComponent(3);
        }
        else {
            symbol = (SymbolDisplay) displayToLay.getComponent(0);
            expression = (Display) displayToLay.getComponent(1);
            integrand = (Display) displayToLay.getComponent(2);
        }
        
        symbol.setSize(symbol.getPreferredSize());
        expression.setSize(expression.getPreferredSize());
        integrand.setSize(integrand.getPreferredSize());
        
        int width1 = symbol.getWidth();
        width += expression.getWidth() + integrand.getWidth();
        
        ascent = Math.max(expression.getAscent(), symbol.getAscent());
        descent = Math.max(expression.getDescent(), symbol.getDescent());
        
        if (count > 3) {
            Display lower = (Display) displayToLay.getComponent(0);
            lower.setSize(lower.getPreferredSize());
            lower.setShiftY((int) Math.round(symbol.getDescent()*0.2f) + lower.getAscent());
            lower.setShiftX((int) Math.round(symbol.getWidth()*0.55f));
            
            //width1 += lower.getWidth();
            width1 = Math.max(symbol.getWidth(), lower.getShiftX() + lower.getWidth());
            
            descent = Math.max(lower.getShiftY() + lower.getDescent() + gap, descent);
            
            symbol.setShiftX(-lower.getWidth() - lower.getShiftX());
            
            if (count > 4) {
                Display upper = (Display) displayToLay.getComponent(4);
                upper.setSize(upper.getPreferredSize());
                
                width1 = Math.max(width1, symbol.getWidth() + upper.getWidth());
                
                int decalage = width1 - symbol.getWidth();
                upper.setShiftX(-(decalage + width/* - gap*/));
                upper.setShiftY((int) Math.round(-symbol.getAscent()*0.6f) - upper.getDescent());
            
                ascent = Math.max(-upper.getShiftY() + upper.getAscent() + gap, ascent);
            }
        }
        
        expression.setShiftX(width1 - symbol.getWidth() + gap);
                    
        displayToLay.setAscent(ascent);
        displayToLay.setDescent(descent);
        
        width += width1/* + expression.getWidth()*/ + 2*gap;
        
        height += ascent + descent;
        displayToLay.setSize(width , height);
        
        displayToLay.setComputeAttributes(false);
        return new Dimension(width, height);
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
