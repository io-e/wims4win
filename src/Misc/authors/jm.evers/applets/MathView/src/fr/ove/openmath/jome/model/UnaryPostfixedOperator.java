/*
$Id: UnaryPostfixedOperator.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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
* @version 2.1  24/02/2000
*/
public class UnaryPostfixedOperator extends Operator {
    /**
    * Inserts the operator instance in the formula tree, from the current insertion position.
    * (checks the priorities and goes up in the tree if necessary).
    *
    * @param ope the current insertion position.
    * @return the new insertion position.
    */
    public FormulaTreeStructure insert(FormulaTreeStructure current) {
        VariableOrNumber template;
        
        // On cherche la position d'insertion de notre operateur
        current = findLocation(current);
        
        // On ajoute l'opérateur comme fils à l'opérateur courant
        current.addChild(this);

        if ((current.getFather() == null) && (current.getNbChildren() == 1))
            addChild(new VariableOrNumber());
        else {
            FormulaTreeStructure fts = (FormulaTreeStructure) current.getChild(getRank() - 1);
            // à cause des priorités sur les opérateurs, on ajoute comme fils l'opérateur
            // dont le rang est juste inférieur à notre instance
            addChild(fts);
            
            // on enlève l'opérateur qu'on a fait "descendre", de la liste
            // de son précédent père (son grand père maintenant)
            current.removeChild(fts);
        }
        
        FormulaTreeStructure child = (FormulaTreeStructure) getChild(0);
        if (child.getResourceIdentifier().equals("OPEN_PAREN"))
            ((Bracket) child).setIsVisible(true); // Dans le cas où la ! est fille d'une division, les parenthèses
            // auront été rendues invisibles d'abord.
            

        // On retourne la reférence de l'instance.
        return this;
    }
    
    /**
    * The Creation of the corresponding linear expression of the formula.
    */
    public String createLinear(String linear) {
        linear = ((FormulaTreeStructure) getChild(0)).createLinear(linear);
        linear += getTheOperator();
        return linear;
    }

    /**
    * Evaluates the instance.
    */
    public String evaluate() {
        return ((FormulaTreeStructure) getChild(0)).evaluate() + getTheOperator();
    }
}
        
