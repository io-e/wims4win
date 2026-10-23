/*
$Id: Factorial.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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

/**
* The factorial operator
*
* @author © 2000 DIRAT Laurent
* @version 2.1  24/02/2000
*/
public class Factorial extends UnaryPostfixedOperator {
    /**
    * The constructor.
    */
    public Factorial() {
        super();
        setTheOperator("!");
    }
        
    /**
    * Set the specified value
    * @param the value to set.
    */
    public void setValue(String value) {
        // Surcharge et on ne fait rien car le nom de l'opérateur est spécifé dans le constructeur.
        // C'est nécessaire parce que danslefchier de ressources, il semble qu'il y ait des pb avec "!"
        // comme identificateur. D'où l'identificateur "fact".
        // Si pas surcharge, le nom de lopérateur sera "fact" et non "!" comme attendu.
    }
    
}
        
