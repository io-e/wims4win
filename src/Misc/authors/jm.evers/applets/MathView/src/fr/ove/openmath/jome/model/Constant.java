/*
$Id: Constant.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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

import fr.ove.openmath.jome.model.FormulaTreeStructure;
import fr.ove.openmath.jome.model.events.ModelEvent;
import fr.ove.openmath.jome.model.evaluation.*;

/**
* @author © 2000 DIRAT Laurent
* @version 2.1 10/01/2000
*/
public class Constant extends FormulaTreeStructure {
    /**
    * The value.
    */
    private String value = "";

    /**
    * The Constructor.
    * 
    */
    public Constant() {
        setAsOperatorPriority(resourcesManager.getAsOperatorPriority("constantPriorities"));
        setAsOperandPriority(resourcesManager.getAsOperandPriority("constantPriorities"));
    }
    
    /**
    * Inserts the operator instance in the formula tree, from the current insertion position.
    * (checks the priorities and goes up in the tree if necessary).
    *
    * @param ope the current insertion position.
    * @return the new insertion position.
    */
    public FormulaTreeStructure insert(FormulaTreeStructure current) {
        ModelEvent modelEvent;
        FormulaTreeStructure father = (FormulaTreeStructure) current.getFather();

        if ((current.getAsOperatorPriority() == resourcesManager.getAsOperatorPriority("constantPriorities")) &&
            current.isTemplate()) {
            // Normalement dans ce cas, current est un template, on le remplace sans état d'âme.
            father.addChild(this);
            father.removeChild(current);

            return this;
        }
        else { // On est dans le cas de l'insertion courante d'une constante dans la FTS
            if ((current.getFather() == null) && (current.getNbChildren() == 0)) {
                // On est dans ce cas là, seulement au tout début de la saisie de la 
                // formule. On est obligé de faire ce test à cause de la multiplication
                // implicite avec les parenthèses (Ex: (a+b)c ==> (a+b)*c, dans ce cas là,
                // current.getNbChildren() est != de 0)
                //On insère directement à la position courante
                current.addChild(this);
                
                return this;
            }
            else { // On est dans aucun des cas précédents, on créé donc une
                // multiplication implicite.
                current = (new Multiplication()).insert(current);
                current = insert(current);
                return current;
            }
        }
    }
    
    /**
    * The Creation of the corresponding linear expression of the formula.
    */
    public String createLinear(String linear) {
        return linear + value;
    }

    /**
    * Evaluates the instance.
    */
    public String evaluate() {
        return value;
    }
    
    /**
    * To check is the instance is an operator.
    * @return <CODE>true</CODE> if it is an operator. <CODE>false</CODE> otherwise.
    */
    public boolean isOperator() {
        return false;
    }
    
    
    // *****************************************
    // Implémentation de l'interface Modifiable
    
    /**
    * Sets the value.
    */
    public void setValue(String value) {
        this.value = value;
    }
    
    /**
    * Returns the value.
    */
    public String getValue() {
        return value;
    }
    
    // *** Fin de l'interface Modifiable ***
    // *************************************
    
    // *****************************************
    // Implémentation de l'interface Iconifiable
    
    /**
    * Returns <CODE>true</CODE> if the instance is iconifiable.
    * <CODE>false</CODE> otherwise.
    */
    public boolean isIconifiable() {
        return false;  // L'instance n'est pas iconifiable
    }
    
    // *** Fin de l'interface Iconifiable ***
    // **************************************
}
