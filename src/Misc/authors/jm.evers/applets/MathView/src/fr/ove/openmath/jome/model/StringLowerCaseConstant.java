/*
$Id: StringLowerCaseConstant.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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

import fr.ove.openmath.jome.model.FormulaTreeStructure;
import fr.ove.openmath.jome.model.events.ModelEvent;
import fr.ove.openmath.jome.model.evaluation.*;

/**
* @author © 2000 DIRAT Laurent
* @version 2.1 10/01/2000
*/
public class StringLowerCaseConstant extends Constant {
    // Juste pour des cas spéciaux qui font que la syntaxe linéaire de la constante est faite en majucules,
    // par exemple e (exp(1)) et i (i^2 = -1) alors que l'affichage de la constante est fait en minuscules.
    /**
    * Returns the value.
    */
    public String getValue() {
        return (super.getValue()).toLowerCase();
    }
}
