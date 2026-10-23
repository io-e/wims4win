/*
$Id: NaryOperator.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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
import fr.ove.openmath.jome.model.events.*;
import fr.ove.openmath.jome.model.evaluation.*;

/**
*
* @author © 2000 DIRAT Laurent
* @version 2.1 10/01/2000
*/
public class NaryOperator extends Operator {
    /**
    * Inserts the operator instance in the formula tree, from the current insertion position.
    * (checks the priorities and goes up in the tree if necessary).
    *
    * @param ope the current insertion position.
    * @return the new insertion position.
    */
    public FormulaTreeStructure insert(FormulaTreeStructure current) {
        VariableOrNumber template;
        // On cherche la position d'insertion de notre opérateur
        current = findLocation(current);

        if (current.getAsOperatorPriority() == getAsOperandPriority()) {
            // On a déjà tapé dans la formule un opérateur de ce genre, on va donc seulement insérer
            // un template.
            template = new VariableOrNumber();
            current.addChild(template);
            
            // On retourne la reférence de notre dernier point d'insertion.
            return template;
        }
        else { 
            // On est dans le cas où on commence à saisir un tel opérateur.
            
            // On ajoute l'opérateur comme fils à l'opérateur courant
            current.addChild(this);
            
            // On teste s'il on est dans le cas où l'on a déjà tapé qque chose dans
            // la formule. Si non, current est forcément de type Formula, par conséquent
            // on est dans le cas particulier où cela doit produire [?]*[?] puisque
            // normalement on ne devrait, dans ce cas là, pas pouvoir taper de "*"
            if ((current.getFather() == null) && (current.getNbChildren() == 1)) {
                template = new VariableOrNumber();
                addChild(template); // On ajoute le premier template.
            }
            else {
                FormulaTreeStructure fts = (FormulaTreeStructure) current.getChild(getRank() - 1);
                // à cause des priorités sur les opérateurs, on ajoute comme fils l'opérateur
                // dont le rang est juste inférieur à notre "*"
                addChild(fts);
                
                // on enlève l'opérateur qu'on a fait "descendre", de la liste
                // de son précédent père (son grand père maintenant)
                current.removeChild(fts);
            }
            
            // On ajoute un template à la multiplication
            template = new VariableOrNumber();
            addChild(template);
            
            // On retourne la reférence de notre dernier point d'insertion.
            return template;
        }
    }
    
    /**
    * The Creation of the corresponding linear expression of the formula.
    */
    public String createLinear(String linear) {
        FormulaTreeStructure child;
        
        for (int i = 0; i < getNbChildren(); i++) {
            child = (FormulaTreeStructure) getChild(i);
            if (i == 0)
                linear = child.createLinear(linear);
            else {
                linear += getTheOperator();
                linear = child.createLinear(linear);
            }
        }
        return linear;
    }
    
    /**
    * Evaluates the instance.
    */
    public String evaluate() {
        Vector evaluations = new Vector();
        
        // On commence par évaluer tous les fils de l'instance
        for (Enumeration e = getChildren().elements(); e.hasMoreElements(); )
            evaluations.addElement(((FormulaTreeStructure) e.nextElement()).evaluate());
    
        int nbEvaluations = evaluations.size();
        String anEvaluation, currEvaluation;
        // Maintenant, on parcourre toutes les évaluations calculées et on essaye de leur appliquer
        // l'opérateur courant
        for (int i = 1; i < nbEvaluations; i++) {
            currEvaluation = (String) evaluations.elementAt(i);
            
            for (int j = 0; j < i; j++) {
                anEvaluation = (String) evaluations.elementAt(j);
                
                if (Evaluator.type(currEvaluation) == Evaluator.type(anEvaluation)) {
                    evaluations.insertElementAt(Evaluator.evaluate(anEvaluation, currEvaluation, getTheOperator()), j);
                    evaluations.removeElement(anEvaluation);
                    evaluations.removeElement(currEvaluation);
                    i--;
                    nbEvaluations--;
                    break;
                }
            }
        }
        
        // On a appliqué l'opérateur courant, on retourne maintenant le résultat, en prenant soin
        // d'intercaler des opérateur entre les évaluations si nécessaire
        String result = (String) evaluations.firstElement();
        for (int i = 1; i < nbEvaluations; i++)
            result += getTheOperator() + (String) evaluations.elementAt(i);

        return result;
    }
}
        
