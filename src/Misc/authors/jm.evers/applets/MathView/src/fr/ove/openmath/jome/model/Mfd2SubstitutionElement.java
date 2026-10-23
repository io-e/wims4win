/*
$Id: Mfd2SubstitutionElement.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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
* The elements of an Mfd2substitution object.<BR>
* It is an operator of arity 2.
* <P>
* In fact, this object _MUST_ be instanciate by the Mfd2Substitution object, and only by it.
* Independently of the Mfd2Substitution object, the instance doesn't make any sense.
*
* @author © 1999 DIRAT Laurent
* @version 2.0 27/10/1999
*/
public class Mfd2SubstitutionElement extends KaryOperator {
    private boolean gotoVariable = true;
    private boolean addNewInstance = false;
    
    /**
    * The Constructor.
    * 
    */
    public Mfd2SubstitutionElement() {
        // le nom du Cd et le nom du symbol sont mis a "" car l'instance ne correspond à aucun symbol dans
        // un cd. L'instance est juste un constituant d'un opérateur correspondant à un symbol dans un cd
        // Même remarque en ce qui concerne la forme linéaire (nom opérateur et ending).
        //super("substElem", "", 2, "", "");
        setResourceIdentifier("substElem");
        setTheOperator("");
        setOperatorArity(2);
        setEnding("");
    }
    
    /** 
    * Inserts the instance in the formula tree structure.<BR>
    * @param current the position in the formula tree where the operator is to be insert.
    * @return the new current position int hte formula tree.
    */
    public FormulaTreeStructure insert(FormulaTreeStructure current) {
        // Surcharge car on insère un template supplémentaire pour coller à l'arité du symbol
        current = super.insert(current);
        super.addElement();
        return current;
    }
    
    /**
    * Adds a new element (template) to the end of the list.
    * Returns the new element.
    */
    public FormulaTreeStructure addElement() {
        // surchage car :
        // * si gotoVariable, le addElement correspond à se positionner sur le second template
        // * si addNewInstance, alors ça veut dire que l'on veut ajouter un autre élément (i.e. instance)
        //   C'est dû au fait que l'instance gère l'aspet 2n-aire d'un opérateur (dont l'instance est un
        // opérande) dont la saisie est la même que pour une opérateur n-aire.
        if (gotoVariable) {
            gotoVariable = false;
            addNewInstance = true;
            return (FormulaTreeStructure) getChild(1).getChild(0);
        }
        else if (addNewInstance) {
            // On veut ajouter un autre élement, donc il faut remonter sur le grand-père (car en fait
            // le père est un Slot) et ajouter un élément au père.
            addNewInstance = false;
            return ((Mfd2Substitution) getFather().getFather()).addElement();
        }
        else
            return super.addElement(); // normalement ne devrait pas être appelé, sinon non respect arité.
    }

    /**
    * The Creation of the corresponding OpenMath object.
    *
    * @param OM_Object the OpenMath object.
    * @param start start point for writing the OpenMath syntax for good presentation/indentation.
    * @param indent the size of the indentation.
    * @return the corresponding OpenMath object.
    *
    public String createOpenMath(String OM_Object, String start, String indent) {
        // En fait, l'instance ne sera instancié que par Mfd2Substitution qui à vraiment un correspondance
        // avec un objet OM.
        // Donc surcharge, puisque l'instance n'a pas de CD, et on retourne seulement les objets contenus
        // succéssivement.
        OM_Object = ((FormulaTreeStructure) getChild(0)).createOpenMath(OM_Object, start+indent, indent);
        OM_Object = ((FormulaTreeStructure) getChild(1)).createOpenMath(OM_Object, start+indent, indent);
        
        return OM_Object;
    }
    */
}
