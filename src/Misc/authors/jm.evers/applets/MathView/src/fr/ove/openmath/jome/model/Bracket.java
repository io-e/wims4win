/*
$Id: Bracket.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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
import fr.ove.openmath.jome.model.evaluation.*;

/**
* The operator bracket.
*
* @author © 1999 DIRAT Laurent
* @version 2.0  25/06/1999
*/
public class Bracket extends UnaryOperator {
    private boolean isVisible = true;
    
    /**
    * The Constructor.
    */
    public Bracket() {
        setResourceIdentifier("OPEN_PAREN");
        setValue("(");
        setAsOperatorPriority(resourcesManager.getAsOperatorPriority("bracketPriorities"));
        setAsOperandPriority(resourcesManager.getAsOperandPriority("bracketPriorities"));
    }
    
    /**
    * Returns the father of the node which, from the current instance, have the specified
    * priority.
    *
    * @param priority the specified priority.
    * @return the desired father.
    */
    public FormulaTreeStructure goTo(int priority) {
        FormulaTreeStructure current = this;

        // Si le test est vrai, c'est qu'on est déjà sur un tel noeud.
        // Cela veut dire que l'on est en train de fermer des parenthèses successives
        // Ex: ((2+3))
        // Ce qui c'est passé: on a eu un goto avec la première ). On se retrouve alors
        // sur le noeud représentant les secondes parenthèses, c'est à dire l'instance courante.
        // Donc l'appel de ce goto a pour but de fermer les parenthèses représentées par l'instance
        // courante. On retourne donc simplement le père. Sinon, on remonte.
        if (current.getAsOperatorPriority() == priority)
            return (FormulaTreeStructure) current.getFather();
        else
            current = (FormulaTreeStructure) current.getFather();

        while ((current.getAsOperatorPriority() != priority) &&
                (((FormulaTreeStructure)current.getFather()).getAsOperatorPriority() != 0))
            current = (FormulaTreeStructure) current.getFather();

        return current;
    }

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

                // On regarde quel est le type de father. Si c'est une Fraction, les parenthèses
                // ne sont pas visibles.
                if (father.getAsOperatorPriority() ==  resourcesManager.getAsOperatorPriority("dividePriorities"))
                    isVisible = false;
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

        // On ajoute un template à nos parenthèses.
        VariableOrNumber template = new VariableOrNumber();
        addChild(template);

        // On retourne le dernier point d'insertion.
        return template;

    }
    
    /**
    * The Creation of the corresponding linear expression of the formula.
    */
    public String createLinear(String linear) {
        linear += "(" + ((FormulaTreeStructure) getChild(0)).createLinear("") + ")";

        return linear;
    }

    /**
    * Evaluates the instance.
    */
    public String evaluate() {
        return "(" + ((FormulaTreeStructure) getChild(0)).evaluate() + ")";
    }

    /**
    * Returns <CODE>true</CODE> if the brackets have to be displayed.
    * <CODE>false</CODE> otherwise.
    */
    public boolean isVisible() {
        return isVisible;
    }

    /**
    * Sets if the brackets have to be displayed or not.
    * @param isVisible <CODE>true</CODE> if we want it. <CODE>false</CODE> otherwise.
    */
    public void setIsVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }
    
    /**
    * Returns the name of the icon associated to the instance.<BR>
    * The icon name is the name of the ressource identifier where "_Ico" added to the end.
    * @returns The name of the icon, or <CODE>null</CODE> if there is no name
    * associated.
    */
    public String getIconName() {
        // On surcharge cette méthode pour que l'icône de la parenthèse soit en fait l'icône
        // de ce qu'elle contient.
        return ((FormulaTreeStructure) getChild(0)).getIconName();
    }
}
