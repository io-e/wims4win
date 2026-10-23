/*
$Id: NaryOperator2.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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
import fr.ove.openmath.jome.model.*;
import fr.ove.openmath.jome.model.events.ModelEvent;

/**
*
* @author © 2000 DIRAT Laurent
* @version 2.1 10/01/2000
*/
public class NaryOperator2 extends Operator {
    /**
    * The ending of the linear syntax of the operator represented by the instance.
    * The default is the closing bracket.
    */
    private String ending = ")";
        
    /**
    * Returns the ending of the operator.
    */
    public String getEnding() {
        return ending;
    }
    
    /**
    * Sets the ending of the operator.
    * @param the ending of the operator.
    */
    public void setEnding(String ending) {
        this.ending = ending;
    }
    
    /** 
    * Inserts the instance in the formula tree structure.<BR>
    * @param current the position in the formula tree where the operator is to be insert.
    * @return the new current position int hte formula tree.
    */
    public FormulaTreeStructure insert(FormulaTreeStructure current) {
        ModelEvent modelEvent;
        
        if ((current.getFather() == null) && (current.getNbChildren() == 0)) {
            // On est dans ce cas là, seulement au tout début de la saisie de la 
            // formule. On est obligé de faire ce test à cause de la multiplication
            // implicite avec les parenthèses (Ex: (a+b)c ==> (a+b)*c, dans ce cas là,
            // current.getNbChildren() est != de 0)
            //On insère directement à la position courante
            current.addChild(this);
        } 
        else if ((current.getAsOperatorPriority() == resourcesManager.getAsOperatorPriority("constantPriorities")) &&
            current.isTemplate()) {
            // Le cas classique (on va même dire normal !!!)
            // La position d'insertion est un template.
            // On doit remplacer le template par l'instance courante
            int rank = current.getRank();
            FormulaTreeStructure father = (FormulaTreeStructure) current.getFather();
            father.addChild(this, rank);
            father.removeChild(current);
        }
        else { 
	    // On est dans aucun des cas précédents, on créé donc une
            // multiplication implicite.
            current = (new Multiplication()).insert(current);
            current = insert(current);
            return current;
        }
        
        // Maintenant, on va rajouter un slot comme élément de l'opérateur
        Slot aSlot = new Slot();
        addChild(aSlot);
        aSlot.setNextSlot(this);
        
        // ...puis un template comme élément de ce slot
        VariableOrNumber template = new VariableOrNumber();
        aSlot.addChild(template);
        
        return template;
    }
    
    /**
    * Adds a new element (template) to the end of the list.
    * Returns the new element.
    */
    public FormulaTreeStructure addElement() {
        Slot aSlot = new Slot();
        addChild(aSlot);
        
        // ...puis un template comme élément de ce slot
        VariableOrNumber template = new VariableOrNumber();
        aSlot.addChild(template);
        
        aSlot.setNextSlot(this);
        ((Slot) getChild(getNbChildren()-2)).setNextSlot(template);
        
        return template;
    }
    
    /**
    * The Creation of the corresponding linear expression of the formula.
    */
    public String createLinear(String linear) {
        FormulaTreeStructure child;
        
        // Là encore, avec ce genre d'opérateur, la saisie se fait globalement comme la saisie
        // d'une fonction. Ex : sum(i=0,n,i^n).
        // Ou alors c'est qque chose qui est encadré par des symboles par exemple une liste {1, 2, 3}.
        // Donc, l'opérateur est ce qui "ouvre" la syntaxe et l'ending, ce qui "ferme" la syntaxe.        
        linear += getTheOperator();
        for (int i = 0; i < getNbChildren(); i++) {
            child = (FormulaTreeStructure) getChild(i);
            if (i == 0)
                linear += child.createLinear(linear);
            else {
                linear += "," + child.createLinear(linear);
            }
        }
        return linear + ending;
    }

    /**
    * Evaluates the instance.
    */
    public String evaluate() {
        return "";
    }
}
