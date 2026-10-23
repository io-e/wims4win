/*
$Id: RequestVariable.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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

import fr.ove.openmath.jome.model.VariableOrNumber;

/**
* Variable elements in the formula.
*
* @author © 1999 DIRAT Laurent
* @version 2.0 29/09/1999
*/
public class RequestVariable extends VariableOrNumber {
    /**
    * The default constructor.<BR>
    * Constructs a variable with no name. A default constuctor is needed for dynamic instantiation.
    * The name of the variable must be set after instantiation
    */
    public RequestVariable() {
        setIsRequestVariable(true);
    }
    
    /**
    *
    */
    public void setValue(String value) {
        // surcharge, parce que tant que l'instance est un template, on veut que la valeur soit
        // [?] (la manière dont sont représentés les templates), plutôt que ? (la manière dont est
        // saisie une request variable.
        if (!isTemplate())
            super.setValue(value);
    }
}
