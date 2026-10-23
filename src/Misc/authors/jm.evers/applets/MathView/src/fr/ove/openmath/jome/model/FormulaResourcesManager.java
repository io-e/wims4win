/*
$Id: FormulaResourcesManager.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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

import fr.ove.utils.CachingResourcesManager;

/**
* The resources manager for the formula.<BR>
*
* @author © 2000 DIRAT Laurent
* @version 2.1  10/01/2000
*/
public class FormulaResourcesManager extends CachingResourcesManager {
    /**
    * Constructor
    * @param resourcesName the name of the resources file. <BR> he name is specified without the ".properties"
    * extension. If the property file is located in a package, the fully qualified name (e.g. my.package.filename)
    * must be used.
    */
    public FormulaResourcesManager(String resourcesName) {
        super(resourcesName);
    }
    
    /**
    * Returns the class name corresponding to the specified property.
    * @param property the specified property
    */
    public String getClassName(String property) {
        return accessResource(property, 0);
    }
        
    /**
    * Returns the arity resource corresponding to the specified property.
    * @param property the specified property
    */
    public Integer getArity(String property) {
        String arity = accessResource(property, 2);
        Integer value = null; // On choisi un Integer putôt qu'un int pour des retours possibles de null
        
        if (arity != null) {
            try {
                value = Integer.valueOf(arity);
            }
            catch (NumberFormatException fne) {
                System.out.println("Wrong operator arity : " + property);
            }
        }
        
        return value;
    }
    
    public String getPrioritiesIdentifier(String property) {
        return accessResource(property, 1);
    }
    
    /**
    * Returns the as operator resource corresponding to the specified property.
    * @param property the specified property
    */
    public int getAsOperatorPriority(String property) {
        String priority = accessResource(property, 0);
        int value = 0;
        
        try {
            value = Integer.valueOf(priority).intValue();
        }
        catch (NumberFormatException fne) {
            System.out.println("Wrong specification of as operator priority : " + priority);
        }
        
        return value;
    }
    
    /**
    * Returns the as operand resource corresponding to the specified property.
    * @param property the specified property
    */
    public int getAsOperandPriority(String property) {
        String priority = accessResource(property, 1);
        int value = 0;
        
        try {
            value = Integer.valueOf(priority).intValue();
        }
        catch (NumberFormatException fne) {
            System.out.println("Wrong specification of as operand priority : " + priority);
        }
        
        return value;
    }
}
