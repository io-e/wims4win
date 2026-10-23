/*
$Id: KaryOperator.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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

/**
*
* @author © 2000 DIRAT Laurent
* @version 2.1 10/01/2000
*/
public class KaryOperator extends NaryOperator2 {
    /**
    * The arity of the operator. (in fact the number max of operands the operator must have)
    */
    private int operatorArity;
    
    /**
    * Sets the operator arity
    */
    public void setOperatorArity(int operatorArity) {
        this.operatorArity = operatorArity;
    }
    
    /**
    * Returns the operator arity
    */
    public int getOperatorArity() {
        return operatorArity;
    }
    
    /**
    * Adds a new element (template) to the end of the list.
    * Returns the new element.
    */
    public FormulaTreeStructure addElement() {
        if (getNbChildren() < operatorArity)
            return super.addElement();
        else 
            return this;
        // faudra faire des exceptions pour dire que l'arité n'est pas respéctée
    }
    
    // *****************************************
    // Implémentation de l'interface Modifiable
    
    /**
    * Sets the value.
    */
    public void setValue(String value) {
        setTheOperator(value + "(");
    }
    
    /**
    * Returns the value.
    */
    public String getValue() {
        return getTheOperator();
    }
    
    // *** Fin de l'interface Modifiable ***
    // *************************************
}
