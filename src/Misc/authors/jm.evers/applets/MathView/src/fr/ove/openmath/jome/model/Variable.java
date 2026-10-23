/*
$Id: Variable.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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
* @author © 2000 DIRAT Laurent
* @version 2.1 10/01/2000
*/
public class Variable extends VariableOrNumber {
    /**
    * The default constructor.<BR>
    * Constructs a variable with no name. A default constuctor is needed for dynamic instantiation.
    * The name of hte variable must be set after instantiation
    */
    public Variable() {
        this("");
    }
    
    /**
    * The constructor.<BR>
    * Contstructs a variable with the specified name.
    * @param varName the name of the variable.
    */
    public Variable(String varname) {
        super("VARIABLE", varname, false, false);
    }
}
