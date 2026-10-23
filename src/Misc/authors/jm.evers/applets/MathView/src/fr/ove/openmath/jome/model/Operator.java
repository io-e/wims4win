/*
$Id: Operator.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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
* @author © 1999 DIRAT Laurent
* @version 2.0  24/06/1999
*/
public abstract class Operator extends FormulaTreeStructure {
    /**
    * The operator name
    */
    private String value;

    /**
    * Returns the name of the operator.
    */
    public String getTheOperator() {
        return value;
    }
    
    /**
    * Sets the name of the operator.
    * @param theOperator the name of the operator.
    */
    public void setTheOperator(String theOperator) {
        value = theOperator;
    }
    
    /**
    * To check is the instance is an operator.
    * @return <CODE>true</CODE> if it is an operator. <CODE>false</CODE> otherwise.
    */
    public boolean isOperator() {
        return true;
    }
    
    // *****************************************
    // Implémentation de l'interface Modifiable
    
    /**
    * Sets the value.
    */
    public void setValue(String value) {
        this.value = value;
    }
    
    /**
    * Returns the value.
    */
    public String getValue() {
        return value;
    }
    
    // *** Fin de l'interface Modifiable ***
    // *************************************
}
