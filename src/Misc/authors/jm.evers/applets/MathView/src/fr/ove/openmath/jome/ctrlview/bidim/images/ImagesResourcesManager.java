/*
$Id: ImagesResourcesManager.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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


package fr.ove.openmath.jome.ctrlview.bidim.images;

import fr.ove.utils.CachingResourcesManager;

/**
* The resources manager for the images.<BR>
* The syntax for the resources is : imgId = imgName:heightFactor:baseline
* (Look at the resources file to get full signification of the different fields)
*
* @author © 2000 DIRAT Laurent
* @version 2.0  04/01/2000
*/
public class ImagesResourcesManager extends CachingResourcesManager {
    /**
    * The Constructor.
    * @param resourcesName the name of the resources file.
    */
    public ImagesResourcesManager(String resourcesName) {
        super(resourcesName);
    }
    
    /**
    * Returns the name of the image corresponding to the property
    * @param property the specified property
    */
    public String getImageName(String property) {
        return accessResource(property, 0);
    }    
    
    /**
    * Returns the height factor to apply to the orignal size of the image corresponding to the property
    * @param property the specified property
    */
    public String getHeightFactor(String property) {
        String heightFactor = accessResource(property, 1);
        if ((heightFactor != null) && heightFactor.equals("null"))
            heightFactor = null;
            
        return heightFactor;
    }    
    
    /**
    * Returns the baseline of the image corresponding to the property
    * @param property the specified property
    */
    public String getBaseline(String property) {
        String baseline = accessResource(property, 2);
        if ((baseline != null) && baseline.equals("null"))
            baseline = null;
            
        return baseline;
    }
    
    /**
    * Returns the top inset of the image corresponding to the property
    * @param property the specified property
    */
    public String getTopInset(String property) {
        String inset = accessResource(property, 3);
        if ((inset != null) && inset.equals("null"))
            inset = null;
            
        return inset;
    }
    
    /**
    * Returns the bottom inset of the image corresponding to the property
    * @param property the specified property
    */
    public String getBottomInset(String property) {
        String inset = accessResource(property, 4);
        if ((inset != null) && inset.equals("null"))
            inset = null;
            
        return inset;
    }
    
    /**
    * Returns the left inset of the image corresponding to the property
    * @param property the specified property
    */
    public String getLeftInset(String property) {
        String inset = accessResource(property, 5);
        if ((inset != null) && inset.equals("null"))
            inset = null;
            
        return inset;
    }
    
    /**
    * Returns the right inset of the image corresponding to the property
    * @param property the specified property
    */
    public String getRightInset(String property) {
        String inset = accessResource(property, 6);
        if ((inset != null) && inset.equals("null"))
            inset = null;
            
        return inset;
    }
}
