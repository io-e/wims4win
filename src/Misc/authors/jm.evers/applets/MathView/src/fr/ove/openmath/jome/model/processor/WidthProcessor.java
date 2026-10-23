/*
$Id: WidthProcessor.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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
* A processor for the width of the formula.<BR>
* What is called width corresponds to the number of operand of an operator. (i.e. the number
* of children of a node in the formula tree stucture.<BR>
* The processor only displays a specified amount of operand of an operator whose number of operands
* is greater than a specified value. The other operands are iconified.
*
* @author © 2000 DIRAT Laurent
* @version 1.0  21/08/2000
*/
public class WidthProcessor extends ProcessorImpl implements WidthProcessorInterface {
    /**
    * The list of widths numbers in the formula.
    */
    private Vector widthList = new Vector();
    
    /**
    * Check if the current list of Strahler numbers in the formula is valid or not.
    */
    private boolean isListValid = false;
    
    /**
    * The number of children viewed.<BR>
    * Should be a positive value. If equals to -1, then all children have to be displayed.
    * This is the default value.
    */
    private int view = -1;
            
    /**
    * The constructor.
    */
    public WidthProcessor(Formula formula) {
        super(formula);
    }
    
    /**
    * Sets the width level form which the processing will be considering.<BR>
    * For convenience only, in most cases this method is equivalent to the
    * @see setLevel of the @see Processor interface.
    * @param widthLevel the width level.
    */
    public void setWidthLevel(int widthLevel) {
        setLevel(widthLevel);
    }
    
    /**
    * Returns the width level form which the processing will be considering.<BR>
    * For convenience only, in most cases this method is equivalent to the
    * @see getLevel of the @see Processor interface.
    */
    public int getWidthLevel() {
        return getLevel();
    }
    
    /**
    * Sets the number of children viewed.
    * @param the number of children viewed.
    */
    public void setView(int view) {
        this.view = view;
    }
    
    /**
    * Returns the number of children viewed.
    */
    public int getView() {
        return view;
    }
    
    
    /**
    * Returns the list of widths of the formula.
    */
    public Vector getWidthList() {
        if (!isListValid) {
            buildWidthList(getFormula());
            isListValid = true;
        }
        return widthList;
    }
    
    /**
    * Returns the biggest width of the formula.
    */
    public int getBiggestWidth() {
        return ((Integer) widthList.lastElement()).intValue();
    }
    
    /**
    * Does processing.
    */
    public void doProcess() {
        Formula formula = getFormula();
        
        if (formula != null) {
            switch (getProcessingId()) {
                case COMPUTE :
                    // La liste des Strahler n'est (a priori) plus à jour.
                    buildWidthList(formula);
                    break;
                case SIMPLE_ICONIFICATION :
                    // Iconifie les parties de la formule qui ont un nombre de Strahler inférieur
                    // à celui spécifié.
                    setUpdateDisplay(true);
                    iconifyTheFTS(formula, getLevel());
                    setUpdateDisplay(false);
                    break;
                case RECURSIVE_ICONIFICATION :
                    // Iconifie les parties de la formule qui ont un nombre de Strahler inférieur
                    // à celui spécifié. Iconification récursive.
                    setUpdateDisplay(true);
                    iconifyAllTheFTS(formula, getLevel());
                    setUpdateDisplay(false);
                    break;
                case COMPUTE_AND_ICONIFY :
                    // La liste des Strahler n'est (a priori) plus à jour.
                    buildWidthList(formula);
                    // Iconifie les parties de la formule qui ont un nombre de Strahler inférieur
                    // à celui spécifié. Iconification récursive.
                    // Le defaut est que l'on icionifie à une valeur correspondant à peu près à la
                    // valeur moyenne du Strahler max calculé.
                    int width = getBiggestWidth() / 2;
                    // Maintenant on regarde dans la liste des width, le width valide immédiatement
                    // supérieur à la valeur correpsondant à la moitié du plus grand.
                    int count = widthList.size();
                    int tmp;
                    for (int i = 0; i < count; i++ ) {
                        tmp = ((Integer) widthList.elementAt(i)).intValue();
                        if (tmp > width) {
                            width = tmp;
                            break;
                        }
                        
                    }
                    setLevel(width);
                    view = width / 2; // On ne visualise que la moitié de fils de l'opérateur
                    iconifyAllTheFTS(formula, width);
            }
        }
    }
    
    /**
    * Iconifies the descendance of the instance which has a widht number greater
    * than the specified.
    * @param width the specified width.
    */
    private void iconifyTheFTS(FormulaTreeStructure fts, int width) {
        // On parcourt tous les enfants de l'instance et on descend dans toute l'arboresnce
        // jusqu'au feuilles. (DFS) Arrivée à la feuille, on teste la valeur du strahler et si
        // elle est inférieure à celle spécifiée, on iconifie.
        // On réitère le process sur tous les enfants.
        FormulaTreeStructure child = null;
        int ftsWidth = fts.getNbChildren();
        if ((ftsWidth > 0) && (ftsWidth >= width))
            iconify(fts, width);
                        
        int count = fts.getNbChildren();
        for (int i = 0; i < count; i++) 
            iconifyTheFTS((FormulaTreeStructure) fts.getChild(i), width);
    }
    
    /**
    * Iconifies all the descendance of the instance which has a width greater
    * than the specified.
    * @param width the specified width.
    */
    private void iconifyAllTheFTS(FormulaTreeStructure fts, int width) {
        // On parcourt tous les enfants de l'instance et on descend dans toute l'arboresnce
        // jusqu'au feuilles. (DFS) Arrivée à la feuille, on teste la valeur du strahler et si
        // elle est inférieure à celle spécifiée, on iconifie.
        // On réitère le process sur tous les enfants.
        int ftsWidth = fts.getNbChildren();
        if (ftsWidth > 0) {
            for (Enumeration e = fts.getChildren().elements(); e.hasMoreElements(); ) 
                iconifyAllTheFTS((FormulaTreeStructure) e.nextElement(), width);
                
            if (ftsWidth >= width)
                iconify(fts, width);
        }
    }
    
    /**
    * Iconifie the instance if it has a width greater than the specified.
    * @param width the specified width.
    */
    private void iconify(FormulaTreeStructure fts, int width) {
        int count = fts.getNbChildren();
        Icon icon = new Icon(fts);
        // On ajoute notre instance à iconifier dans Icon (<=> on iconfie l'instance)
        FormulaTreeStructure child;
        // On ajout notre icone
        fts.addChild(icon);
        /**
        for (int i = view; i < count; ) {
            child = (FormulaTreeStructure) fts.getChild(i);
            icon.addIconified(child);
            //fts.removeChild(child);
            count--;
        }*/
        
        for (int i = view; i < count; i++)
            icon.addIconified((FormulaTreeStructure) fts.getChild(i));
            
        for (int i = view; i < count; ) {
            fts.removeChild((FormulaTreeStructure) fts.getChild(i));
            count--;
        }
        
        if (getUpdateDisplay()) {
            // On ajoute un display pour notre icon.
            ModelEvent modelEvent = new ModelEvent(fts);
            modelEvent.setAction(ModelEvent.ADD, icon);
            fts.fireModelEvent(modelEvent);
        }
    }
    
    /**
    * Builds the list of widths of the formula.
    */
    private void buildWidthList(FormulaTreeStructure fts) {
        int width = fts.getNbChildren();
        
        if (width > 1)
            addNewWidth(width);
            
        for (int i = 0; i < width; i++)
            buildWidthList((FormulaTreeStructure) fts.getChild(i));
    }
    
    /**
    * Adds the specified width value in the list if necessary
    * @param width the width value to add
    */
    private void addNewWidth(int width) {
        int currentWidth;
        
        if (widthList.size() == 0)
            widthList.addElement(new Integer(width));
        else {
            for (int i = 0; i < widthList.size(); i++) {
                currentWidth = ((Integer) widthList.elementAt(i)).intValue();
                if (width == currentWidth)
                    return;
                else if (width < currentWidth) {
                    widthList.insertElementAt(new Integer(width), i);
                    return;
                }
            }
            widthList.addElement(new Integer(width));
        }
    }
    
    public static void main(String args[]) {
        String exp = "a*z*e*r*r*t*(t+2+2+2+2+2+2+2+6+6+6+6+6+6)*1*6*6*6*6*6*6+2+3+4+5+6+7+8+9+10+11+12+13+14";
        Formula formula = new Formula();
        formula.setDoProcessing(true);
        
        WidthProcessor p = new WidthProcessor(formula);
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
