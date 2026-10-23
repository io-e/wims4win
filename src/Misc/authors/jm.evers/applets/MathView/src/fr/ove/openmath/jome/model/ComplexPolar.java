/*
$Id: ComplexPolar.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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
* @author © 1999 DIRAT Laurent
* @version 2.0 28/06/1999
*/
public class ComplexPolar extends KaryOperator {
    /*
    * Pour savoir si on est dans le cas où il faut positionner current sur la partie imaginaire ou pas
    */
    private boolean gotoImaginary = true;
    
    /** 
    * Inserts the instance in the formula tree structure.<BR>
    * @param current the position in the formula tree where the operator is to be insert.
    * @return the new current position int hte formula tree.
    */
    public FormulaTreeStructure insert(FormulaTreeStructure current) {
        current = super.insert(current);
        FormulaTreeStructure realPart = current;
        current = (new Multiplication()).insert(current);
        // Construction de la partie imaginaire.
        current = (new E()).insert(current);
        current = (new Superscript()).insert(current);
        current = (new Bracket()).insert(current);
        current = (new I()).insert(current);
        current = (new Multiplication()).insert(current);
        current = (new VariableOrNumber()).insert(current);
        
        return realPart;
    }
    
    /**
    * Adds a new element (template) to the end of the list.
    * Returns the new element.
    */
    public FormulaTreeStructure addElement() {
        if (gotoImaginary) {
            gotoImaginary = false;
            FormulaTreeStructure imaginay = (FormulaTreeStructure) getChild(0); // le slot
            imaginay = (FormulaTreeStructure) imaginay.getChild(0); // la multiplication
            imaginay = (FormulaTreeStructure) imaginay.getChild(1); // la puissance
            imaginay = (FormulaTreeStructure) imaginay.getChild(1); // la parenthèse
            imaginay = (FormulaTreeStructure) imaginay.getChild(0); // la partie multiplication
            imaginay = (FormulaTreeStructure) imaginay.getChild(1); // la partie imaginaire
            
            return imaginay;
        }
        else
            return this;
    }
    
    /**
    * The Creation of the corresponding linear expression of the formula.
    */
    public String createLinear(String linear) {
        FormulaTreeStructure imaginay = (FormulaTreeStructure) getChild(0); // le slot
        imaginay = (FormulaTreeStructure) imaginay.getChild(0); // la multiplication
        FormulaTreeStructure realPart = (FormulaTreeStructure) imaginay.getChild(0);
        imaginay = (FormulaTreeStructure) imaginay.getChild(1); // la puissance
        imaginay = (FormulaTreeStructure) imaginay.getChild(1); // la parenthèse
        imaginay = (FormulaTreeStructure) imaginay.getChild(0); // la multiplication
        imaginay = (FormulaTreeStructure) imaginay.getChild(1); // la partie imaginaire
        
        linear = realPart.createLinear(linear + getTheOperator());
        linear = imaginay.createLinear(linear + ",");
        return linear + ")"; 
    }
}
