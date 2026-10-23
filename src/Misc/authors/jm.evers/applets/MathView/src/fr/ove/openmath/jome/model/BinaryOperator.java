/*
$Id: BinaryOperator.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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
import fr.ove.openmath.jome.model.evaluation.*;

/**
* @author © 1999 DIRAT Laurent
* @version 2.0 25/06/1999
*/
public class BinaryOperator extends Operator {
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
        
        // Pas de problème de priorité pour ce type d'opérateur.
        // On doit juste savoir si current est une instance de Formula pour gérer le cas
        // particulier où, la formule vide, on veut saisir un tel opérateur et donc insertion
        // des templates adequat ==> [?] ope [?].
        
        // On ajoute l'opérateur comme fils à l'opérateur courant
        current.addChild(this);

        if ((current.getFather() == null) && (current.getNbChildren() == 1)) {
            template = new VariableOrNumber();
            addChild(template); // On ajoute le premier template.
        }
        else {
            FormulaTreeStructure fts = (FormulaTreeStructure) current.getChild(getRank() - 1);
            // à cause des priorités sur les opérateurs, on ajoute comme fils l'opérateur
            // dont le rang est juste inférieur à notre instance
            addChild(fts);
            
            // On doit faire attention à quel type d'opérateur on a affaire.
            // Si fts est les parenthèse, elles ne sont pas visibles si l'instance courante est une fraction.
            // Dans les autres cas, elles le sont.
            int prio = fts.getAsOperatorPriority();
            if (prio ==  resourcesManager.getAsOperatorPriority("bracketPriorities")) {
                prio = getAsOperatorPriority();
                if (prio ==  resourcesManager.getAsOperatorPriority("dividePriorities"))
                    ((Bracket) fts).setIsVisible(false);
                else
                    ((Bracket) fts).setIsVisible(true);
            }
            
            
            // on enlève l'opérateur qu'on a fait "descendre", de la liste
            // de son précédent père (son grand père maintenant)
            current.removeChild(fts);
        }

        // On ajoute un template à la division
        template = new VariableOrNumber();
        addChild(template);

        // On retourne la reférence de notre dernier point d'insertion.
        return template;
    }
    
    /**
    * The Creation of the corresponding linear expression of the formula.
    */
    public String createLinear(String linear) {
        linear = ((FormulaTreeStructure) getChild(0)).createLinear(linear);
        linear += getTheOperator();
        linear = ((FormulaTreeStructure) getChild(1)).createLinear(linear);
        return linear;
    }

    /**
    * Evaluates the instance.
    */
    public String evaluate() {
        return Evaluator.evaluate(((FormulaTreeStructure) getChild(0)).evaluate(),
                        ((FormulaTreeStructure) getChild(1)).evaluate(),
                        getTheOperator());
    }
            
}
