/*
$Id: DepthProcessor.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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


package fr.ove.openmath.jome.model.processor;

import fr.ove.openmath.jome.model.*;
import fr.ove.openmath.jome.model.events.*;
import java.util.*;
import java.awt.*;

/**
* A processor for the depth of the formula.
*
* @author © 2000 DIRAT Laurent
* @version 1.0  21/08/2000
*/
public class DepthProcessor extends ProcessorImpl implements DepthProcessorInterface {
    /**
    * The maximal depth of the formula.
    */
    private int maxDepthValue = 0;
    
    /**
    * The constructor.
    * @param formula the formula to process.
    */
    public DepthProcessor(Formula formula) {
        super(formula);
    }
    
    /**
    * Sets the depth level form which the processing will be considering.<BR>
    * For convenience only, in most cases this method is equivalent to the
    * @see setLevel of the @see Processor interface.
    * @param depthLevel the depth level.
    */
    public void setDepthLevel(int depthLevel) {
        setLevel(depthLevel);
    }
    
    /**
    * Returns the depth level form which the processing will be considering.<BR>
    * For convenience only, in most cases this method is equivalent to the
    * @see getLevel of the @see Processor interface.
    */
    public int getDepthLevel() {
        return getLevel();
    }
    
    /**
    * Returns the max depth value of the formula
    */
    public int getMaxDepthValue() {
        return maxDepthValue;
    }
        
    /**
    * Does processing.
    */
    public void doProcess() {
        Formula formula = getFormula();
        
        if (formula != null) {
            switch (getProcessingId()) {
                case COMPUTE :
                    // En fait le calcul de la profondeur est fait à la construction de la formule.
                    // donc rien de particulier à faire ici, si ce n'est affecter la valeur de la 
                    // profondeur max de l'arbre de la formule.
                    maxDepthValue = 0; // initialisation
                    computeMaxDepthValue(formula); // calcul.
                    break;
                case SIMPLE_ICONIFICATION :
                    // Iconifie les parties de la formule qui ont une profondeur supérieure
                    // à celle spécifiée.
                    setUpdateDisplay(true);
                    iconifyTheFTS(formula, getLevel());
                    setUpdateDisplay(false);
                    break;
                case RECURSIVE_ICONIFICATION :
                    // Iconifie les parties de la formule qui ont une profondeur supérieure
                    // à celle spécifiée. Iconification récursive.
                    setUpdateDisplay(true);
                    iconifyAllTheFTS(formula, getLevel());
                    setUpdateDisplay(false);
                    break;
                case COMPUTE_AND_ICONIFY :
                    // En fait le calcul de la profondeur est fait à la construction de la formule.
                    // donc rien de particulier à faire ici, si ce n'est affecter la valeur de la 
                    // profondeur max de l'arbre de la formule.
                    maxDepthValue = 0; // initialisation
                    computeMaxDepthValue(formula); // calcul.
                    // Iconifie les parties de la formule qui ont une profondeur supérieure
                    // à celle spécifié. Iconification récursive.
                    // Le defaut est que l'on icionifie à une valeur correspondant à peu près à la
                    // moitié de la profondeur de l'arbre.
                    setLevel(maxDepthValue / 2);
                    iconifyAllTheFTS(formula, getLevel());
            }
        }
    }
    
    /**
    * Iconifies the descendance of the instance which has a depth greater
    * than the specified.
    * @param depth the specified depth value.
    */
    private void iconifyTheFTS(FormulaTreeStructure fts, int depth) {
        // On parcourt tous les enfants de l'instance et on descend dans toute l'arboresnce
        // jusqu'au feuilles. (DFS) Arrivée à la feuille, on teste la valeur de la profondeur et si
        // elle est spérieure à celle spécifiée, on iconifie.
        // On réitère le process sur tous les enfants.
        FormulaTreeStructure child = null;
        
        /*
        if (fts.getNbChildren() > 0) {
            for (Enumeration e = fts.getChildren().elements(); e.hasMoreElements(); ) {
                child = (FormulaTreeStructure) e.nextElement();
                if (child.getDepth() < depth)
                    iconifyTheFTS(child, depth);
                else
                    if (child.getNbChildren() > 0) // Si child n'est pas une feuille (i.e. constante), on iconifie
                        iconify(child, depth);
            }
        }
        */
        
        if (fts.isIcon()) {
            FormulaTreeStructure father = (FormulaTreeStructure) fts.getFather();
            fts.uniconify();
            
            // On envoie maintenant un événement comme quoi il faut reconstruire
            // l'affichage.
            ModelEvent modelEvent = new ModelEvent(father);
            modelEvent.setAction(ModelEvent.REBUILD, null);
            father.fireModelEvent(modelEvent);
            
            iconifyTheFTS(father, depth);
        }
        
        if (fts.getDepth() < depth) {
            if (fts.getNbChildren() > 0) {
                for (Enumeration e = fts.getChildren().elements(); e.hasMoreElements(); )
                        iconifyTheFTS((FormulaTreeStructure) e.nextElement(), depth);
            }
        }
        else {
            if (fts.getNbChildren() > 0)
                iconify(fts, depth);
        }
        
    }
    
    /**
    * Iconifies all the descendance of the instance which has a Strahler number lower
    * than the specified.
    * @param depth the specified Strahler number.
    */
    private void iconifyAllTheFTS(FormulaTreeStructure fts, int depth) {
        // On parcourt tous les enfants de l'instance et on descend dans toute l'arboresnce
        // jusqu'au feuilles. (DFS) Arrivée à la feuille, on teste la valeur du strahler et si
        // elle est inférieure à celle spécifiée, on iconifie.
        // On réitère le process sur tous les enfants.
        if (fts.getNbChildren() > 0) {
            for (Enumeration e = fts.getChildren().elements(); e.hasMoreElements(); ) 
                iconifyAllTheFTS((FormulaTreeStructure) e.nextElement(), depth);

            if (fts.getDepth() >= depth)
                iconify(fts, depth);
        }
    }
    
    /**
    * Iconifie the instance if it has a Strahler number lower than the specified.
    * @param depth the specified Strahler number
    */
    private void iconify(FormulaTreeStructure fts, int depth) {
        if (fts.getFather() != null) {
            Icon icon = new Icon(fts);
            // On ajoute notre instance à iconifier dans Icon (<=> on iconfie l'instance)
            icon.addIconified(fts);
            // On insère maintenant notre icon à la place de fts.
            // On prend le père de l'instance.
            FormulaTreeStructure father = (FormulaTreeStructure) fts.getFather();
            // On insère notre icone à la place de l'instance
            father.addChild(icon, fts.getRank());
            // On enlève l'instance de la liste des enfants de father.
            father.removeChild(fts);
            
            if (getUpdateDisplay()) {
                // On ajoute un display pour notre icon.
                ModelEvent modelEvent = new ModelEvent(father);
                modelEvent.setAction(ModelEvent.ADD, icon);
                father.fireModelEvent(modelEvent);
            }
        }
    }
    
    /**
    * Computes the max depth value of the formula
    */
    private void computeMaxDepthValue(FormulaTreeStructure fts) {
        if (fts.getNbChildren() > 0) {
            for (Enumeration e = fts.getChildren().elements(); e.hasMoreElements(); ) 
                computeMaxDepthValue((FormulaTreeStructure) e.nextElement());
        }
        
        maxDepthValue = (int) Math.max(fts.getDepth(), maxDepthValue);
    }
        
    public static void main(String args[]) {
        String exp = "1+2/3*(4+3/5)^2+2/((3+5^9)*(1+2+3))";
        Formula formula = new Formula();
        formula.setDoProcessing(true);
        
        DepthProcessor p = new DepthProcessor(formula);
        //p.setLevel(3);
        formula.setProcessor(p);
        
        fr.ove.openmath.jome.ctrlview.bidim.FormulaDisplay display = new fr.ove.openmath.jome.ctrlview.bidim.FormulaDisplay();
        formula.addModelListener(display);
        display.addControlListener(formula);
        
        java.awt.Frame f = new java.awt.Frame();
        f.setLayout(new java.awt.BorderLayout());
        f.setBounds(50, 50, 250, 80);
        f.add("Center", display);
        f.setVisible(true);
        
        fr.ove.openmath.jome.ctrl.linear.LinearParser linearParser = new fr.ove.openmath.jome.ctrl.linear.LinearParser();
        linearParser.addLinearParserListener(formula);
        linearParser.parse(exp);
        System.out.println("la formule saisie est : \t\t" + exp);
        System.out.println("la formule construite est : \t\t" + formula.getLinear());
    }    
}
