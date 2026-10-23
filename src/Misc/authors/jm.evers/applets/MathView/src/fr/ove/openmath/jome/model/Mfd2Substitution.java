/*
$Id: Mfd2Substitution.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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
* The substitution model of the symbol, with the same name, in the Mfd2 cd.
*
* @author © 1999 DIRAT Laurent
* @version 2.0 28/10/1999
*/
public class Mfd2Substitution extends Function {
    /** 
    * Inserts the instance in the formula tree structure.<BR>
    * @param current the position in the formula tree where the operator is to be insert.
    * @return the new current position int hte formula tree.
    */
    public FormulaTreeStructure insert(FormulaTreeStructure current) {
        // Surchage le la méthode insert(..) car on ne veut pas qu'un template soit inséré,
        // mais une instance de SubstitutionElement.
        // C'est en qques sorte SubstitutionElement qui se charge de l'aspet 2n-aire de la chose.
        
        // Donc insertion classique...
        current = super.insert(current);
        // ...mais à la place du template, on met un SubstitutionElement
        return (new Mfd2SubstitutionElement()).insert(current);
    }
    
    /**
    * Adds a new SubstitutionElement to the end of the list.
    * Returns the new element.
    */
    public FormulaTreeStructure addElement() {
        // Pareil que pour insert, on veut un SubstitutionElement à la place d'un template
        FormulaTreeStructure current = super.addElement();
        return (new Mfd2SubstitutionElement()).insert(current);
    }
}
