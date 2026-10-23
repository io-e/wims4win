/*
$Id: AnInteger.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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
* Integer elements in the formula.
*
* @author © 1999 DIRAT Laurent
* @version 2.0 29/09/1999
*/
public class AnInteger extends VariableOrNumber {
    /**
    * The default constructor.<BR>
    * Constructs a integer with no value. A default constuctor is needed for dynamic instantiation.
    * The value must be set after instantiation.
    */
    public AnInteger() {
        this("");
    }
    
    /**
    * The constructor.<BR>
    * Contstructs an integer with the specified string value.
    * @param value the integer value.
    */
    public AnInteger(String value) {
        super("INTEGER", value, true, true);
    }
}
