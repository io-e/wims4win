/*
$Id: UnaryOperator.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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

/**
* @author © 2000 DIRAT Laurent
* @version 2.1  10/01/2000
*/
public class UnaryOperator extends Operator {
    /**
    * Inserts the operator instance in the formula tree, from the current insertion position.
    * (checks the priorities and goes up in the tree if necessary).
    *
    * @param ope the current insertion position.
    * @return the new insertion position.
    */
    public FormulaTreeStructure insert(FormulaTreeStructure current) {
        // On recherche la position d'insertion de notre instance.
        current = findLocation(current);

        if (current.getFather() == null) {
            // On commence la saisie de la formule, on ajoute l'instance courante telle
            // qu'elle dansla formule.
            current.addChild(this);

        }
        else { // 2 possibilités s'offrent à nous !!!
            if ((current.getAsOperatorPriority() == resourcesManager.getAsOperatorPriority("constantPriorities")) &&
                current.isTemplate()) {
                // Le cas classique (on va même dire normal !!!)
                // La position d'insertion est un template.
                // On doit remplacer le template par l'instance courante (nos parenthèses)
                int rank = current.getRank();
                FormulaTreeStructure father = (FormulaTreeStructure) current.getFather();
                father.addChild(this, rank);
                father.removeChild(current);
            }
            else {
                // Le cas pas classique
                // La position d'insertion n'est pas un template.
                // On créé une multiplication implicite.
                current = (new Multiplication()).insert(current);
                // On insère nos parenthèses
                current = insert(current);
                // On retourne le dernier point d'insertion.
                return current;
            }
        }

        // On ajoute un template à notre opérateur.
        VariableOrNumber template = new VariableOrNumber();
        addChild(template);

        // On retourne le dernier point d'insertion.
        return template;

    }
    
    /**
    * The Creation of the corresponding linear expression of the formula.
    */
    public String createLinear(String linear) {
        linear += getTheOperator();
        linear = ((FormulaTreeStructure) getChild(0)).createLinear(linear);
        return linear;
    }

    /**
    * Evaluates the instance.
    */
    public String evaluate() {
        return getTheOperator() + ((FormulaTreeStructure) getChild(0)).evaluate();
    }
}
        
