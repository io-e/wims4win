/*
$Id: UnaryPlus.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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
import fr.ove.openmath.jome.model.FormulaTreeStructure;
import fr.ove.openmath.jome.model.events.ModelEvent;

/**
* The operator "+", which is the unary plus.<BR>
*
* <CODE>UnaryPlus</CODE> represents a node in the formula tree.<BR>
* Its children are the operands of the operation.
*
* @author © 2000 DIRAT Laurent
* @version 2.1 10/01/2000
*/
public class UnaryPlus extends UnaryOperator {
    /**
    * The Constructor.
    */
    public UnaryPlus() {
        setResourceIdentifier("UNARYPLUS");
        setValue("+");
        setAsOperatorPriority(resourcesManager.getAsOperatorPriority("unaryPlusPriorities"));
        setAsOperandPriority(resourcesManager.getAsOperandPriority("unaryPlusPriorities"));
    }
    
    /**
    * Inserts the operator instance in the formula tree, from the current insertion position.
    * (checks the priorities and goes up in the tree if necessary).
    *
    * @param ope the current insertion position.
    * @return the new insertion position.
    */
    public FormulaTreeStructure insert(FormulaTreeStructure current) {
        // Le seul moment où l'on fait appel à cet opérateur, c'est pour la saisie d'un
        // plus unaire, et donc où l'on a une instance de Addition qui est en fait notre
        // point d'insertion courant, i.e. current.
        // On peut ajouter l'instance courante telle qu'elle.
        current.addChild(this);
        
        // On insère notre template dans le FTS
        VariableOrNumber template = new VariableOrNumber();
        addChild(template);

        // On retourne la reférence de notre dernier point d'insertion.
        return template;
    }
}
        
