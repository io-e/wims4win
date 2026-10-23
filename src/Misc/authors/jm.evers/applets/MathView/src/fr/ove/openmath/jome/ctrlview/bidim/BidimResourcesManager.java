/*
$Id: BidimResourcesManager.java,v 1.3 2003/02/18 11:48:46 sander Exp $
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


package fr.ove.openmath.jome.ctrlview.bidim;

import fr.ove.utils.CachingResourcesManager;

/**
* The resources manager for the rendering formula.<BR>
* The different resources take the following syntax : (see the resources files for more details)
*       modelId = idDisplay:layoutManager
*
* @author © 2000 DIRAT Laurent
* @version 2.0  03/01/2000
*/
public class BidimResourcesManager extends CachingResourcesManager {
    /**
    * The Constructor.
    * @param resourcesName the name of the resources file.
    */
    public BidimResourcesManager(String resourcesName) {
        super(resourcesName);
    }
    
    /**
    * Returns the display identificator of the specified property.
    * @param property the specified property
    */
    public String getIdDisplay(String property) {
        return accessResource(property, 0);
    }
    
    /**
    * Returns the class name of the layout manager corresponding to the specified property.
    * @param property the specified property
    */
    public String getLayoutManager(String property) {
        return accessResource(property, 1);
    }    
}
