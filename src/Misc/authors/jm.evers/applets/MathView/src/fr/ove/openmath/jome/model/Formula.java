/*
$Id: Formula.java,v 1.4 2003/02/18 11:48:47 sander Exp $
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


package fr.ove.openmath.jome.model;

import java.util.*;
import java.awt.*;

import fr.ove.openmath.jome.model.*;
import fr.ove.openmath.jome.model.processor.*;
import fr.ove.openmath.jome.model.events.ModelEvent;
import fr.ove.openmath.jome.model.evaluation.*;
import fr.ove.openmath.jome.behaviour.Modifiable;
import fr.ove.openmath.jome.ctrl.linear.events.*;
import fr.ove.utils.Factory;

/**
* The formula.<BR>
* The root of the formula tree structure.
*
* @author © 2000 DIRAT Laurent
* @version 2.0  28/06/99
*/
public class Formula extends FormulaTreeStructure implements LinearParserListener {
    /**
    * The current insertion position in the formula (tree structure).
    */
    public FormulaTreeStructure current;
    
    /**
    * The OpenMath formater for exportation of the formula as an OpenMath object
    */
    
    /**
    * The MathML formater for exportation of the formula as a MathML object
    */
    
    // On maintient une pile qui va contenir les instances qui ont besoin d'être "fermées".
    // Par exemple les parenthèses, les listes, ...
    // Nécessaire de faire ça à cause des inclusions mutuelles d'opérateurs du même type.
    private Stack openingStack = new Stack();
    
    /**
    * Flag to deal with a possible processing of the model of the formula before rendering.<BR>
    * The default is <CODE>false</CODE>, i.e. we want the formula to be displayed
    * normally (no processing).
    */
    private boolean doProcessing = false;
    
    /**
    * The processor which will process the model of the formula before rendering if wanted.<BR>
    * By default, no processor is set. One processor has to added when desired.
    */
    private Processor processor = null;

    /**
    * The Constructor.
    */
    public Formula () {
        setResourceIdentifier("FORMULA");
        setAsOperatorPriority(resourcesManager.getAsOperatorPriority("formulaPriorities"));
        setAsOperandPriority(resourcesManager.getAsOperandPriority("formulaPriorities"));
        current = this;
    }
    
    /**
    * To check is the instance is an operator.
    * @return <CODE>true</CODE> if it is an operator. <CODE>false</CODE> otherwise.
    */
    public boolean isOperator() {
        return false;
    }
    
    /**
    * Inserts the operator instance in the formula tree, from the current insertion position.
    * (checks the priorities and goes up in the tree if necessary).<BR>
    * At the moment, we can't insert a formula in an another formula.
    * @param current the current insertion position.
    */
    public FormulaTreeStructure insert(FormulaTreeStructure current) {
        // On ne devrait pas insérer de formule !!
        // A voir, peut être utile pour le drag'n drop d'une nouvelle formule;
        return null;
    }

    /**
    * The Creation of the corresponding linear expression of the formula.
    */
    public String createLinear(String linear) {
        if (getNbChildren() > 0)
            linear = ((FormulaTreeStructure) getChild(0)).createLinear(linear);
            
        return linear;
    }
    
    /**
    * clears the formula.
    */
    public void clear() {
        // On enlève tous les enfants de la FTS.
        removeAll();
        // On remet la position courante d'insertion sur la racine de la FTS
        current = this;
    }
    
    /**
    * Returns the linear expression of the formula.
    */
    public String getLinear() {
        return createLinear("");
    }

    /**
    * Returns the OpenMath representation of the formula.
    */
    
    /**
    * Returns the MathML representation of the formula.
    */
    
    /**
    * Evaluates the instance.
    */
    public String evaluate() {
        if (getNbChildren() > 0)
            return ((FormulaTreeStructure) getChild(0)).evaluate();
        else
            return "";
    }
    
    /**
    * Treats the event received.
    * @param linearParserEvent the event received.
    */
    public void consumeLinearParserEvent(LinearParserEvent linearParserEvent) {
        int identifier = linearParserEvent.getIdentifier();
        String value = linearParserEvent.getValue();
        ModelEvent modelEvent;
        
        switch (identifier) {
            case LinearParserEvent.START_EXPRESSION :
                clear();
                openingStack.setSize(0);
                modelEvent = new ModelEvent(this);
                modelEvent.setAction(ModelEvent.CLEAR, null);
                fireModelEvent(modelEvent);
                break;
                
            case LinearParserEvent.END_EXPRESSION :
                // Le modèle de la formule est entièrement construit.
                // On regarde d'abord les éventuels traitements à effectuer sur le modèle
                // avant la construction des displays.
                if (doProcessing && (processor != null)) {
        		    // Initialisation du processeur si besoin est.
        		    processor.init();
                    processor.doProcess();
                }
                
                modelEvent = new ModelEvent(this);
                modelEvent.setAction(ModelEvent.CREATE, null);
                fireModelEvent(modelEvent);
                break;
                
            default :
                String strIdentifier = linearParserEvent.getIdentifierAsString();
                String className = resourcesManager.getClassName(strIdentifier);
                FormulaTreeStructure newFts = null;

                if (className.equals("null")) {
                    if (identifier == linearParserEvent.SEPARATOR) {
                        //current = current.goTo(FormulaTreeStructure.SLOT_AS_OPERATOR_PRIORITY);
                        current = current.goTo(resourcesManager.getAsOperatorPriority("slotPriorities"));
                        
                        // current est maintenant une instance de Slot, on récupère son père pour lui
                        // ajouter un nouvel élément.
                        NaryOperator2 currentFather = (NaryOperator2) current.getFather();
                        current = currentFather.addElement();
                    }
                    else {
                        // Alors c'est forcément un linearParserEvent.CLOSE_*
                        if (!openingStack.isEmpty()) {
                            current = (FormulaTreeStructure) openingStack.pop();
                            // Et remonte encore d'un cran à cause des priorités des opérateurs
                            // (surtout en ce qui concerne les parenthèses)
                            current = (FormulaTreeStructure) current.getFather();
                        }
                    }
                }
                else {
                    if (className.equals("refine")) {
                        String[] properties = resourcesManager.getResourceStrings(value);
                        if (properties.length != 0)
                            strIdentifier = value; // On a trouvé qque chose correspondant à value
                        else {
                            if (identifier == linearParserEvent.RESERVED) 
                                // Il n'existe pas de mot réservé défini dans le modèle correspondant à value.
                                // On considère donc value comme une variable.
                                strIdentifier = "VARIABLE";
                            else
                                // Si ce n'est pas un mot réservé, c'est une fonction.
                                // Il n'existe pas de fonction spéciale définie correspondant à value.
                                // On considère donc value comme une fonction par défaut ayant pour nom value.
                                strIdentifier = "defaultFunction";
                        }
                    }
                    
                    newFts = (FormulaTreeStructure) Factory.getClassInstance(resourcesManager.getClassName(strIdentifier));
                    newFts.setResourceIdentifier(strIdentifier);
                    String prioritiesId = resourcesManager.getPrioritiesIdentifier(strIdentifier);
                    newFts.setAsOperatorPriority(resourcesManager.getAsOperatorPriority(prioritiesId));
                    newFts.setAsOperandPriority(resourcesManager.getAsOperandPriority(prioritiesId));
                    
                    if (value != null)
                        newFts.setValue(value);
                        
                    Integer arity = resourcesManager.getArity(strIdentifier);
                    if (arity != null)
                        ((KaryOperator) newFts).setOperatorArity(arity.intValue());
                    
                    switch (identifier) {
                        case LinearParserEvent.FUNCTION :
                        case LinearParserEvent.OPEN_PAREN :
                        case LinearParserEvent.OPEN_BRACKET :
                        case LinearParserEvent.OPEN_CURLY :
                            openingStack.push(newFts);
                    }
                    
                    current = newFts.insert(current);
                }
        }
    }
    
    /**
    * Sets if we had to process the instance before rendering.
    * @param doProcessing <CODE>true</CODE> if the instance needs a processing.
    * <CODE>false</CODE> otherwise.
    */
    public void setDoProcessing(boolean doProcessing) {
        this.doProcessing = doProcessing;
    }
    
    /**
    * Returns <CODE>true</CODE> if the instance needs a processing.
    * <CODE>false</CODE> otherwise.
    */
    public boolean getDoProcessing() {
        return doProcessing;
    }
    
    /**
    * Sets the processor of the instance.
    * @param processor the processor.
    */
    public void setProcessor(Processor processor) {
        this.processor = processor;
    }
    
    /**
    * Returns the processor of the instance.
    */
    public Processor getProcessor() {
        return processor;
    }
    
    /**
    * Processes the instance
    */
    public void doProcess() {
        if (doProcessing && (processor != null))
            processor.doProcess();
    }
    
    public static void main(String args[]) {
        String exp = "sum(i^n,0,sum(i^n,0,5))t";
        Formula formula = new Formula();
        fr.ove.openmath.jome.ctrl.linear.LinearParser linearParser = new fr.ove.openmath.jome.ctrl.linear.LinearParser();
        linearParser.addLinearParserListener(formula);
        linearParser.parse(exp);
        System.out.println("la formule saisie est : \t\t" + exp);
        System.out.println("la formule construite est : \t\t" + formula.getLinear());
        
        try {
            Thread.sleep(5000);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
