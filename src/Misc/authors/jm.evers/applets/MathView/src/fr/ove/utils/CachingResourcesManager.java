/*
$Id: CachingResourcesManager.java,v 1.2 2003/02/18 11:48:48 sander Exp $
*/


package fr.ove.utils;

import fr.ove.utils.ResourcesManager;

/**
* The resources manager caching the last resource required.
*
* @author © 1999 DIRAT Laurent
* @version 2.0  17/12/1999
*/
public class CachingResourcesManager extends ResourcesManager {
    /**
    * The last resource name accessed by the instance.
    */
    private String cachedResource = "";
    
    /**
    * The list of properties associated to the last resource accessed by the instance.
    */
    private String[] cachedProperties = null;
    
    /**
    * The Constructor.
    * @param resourcesName the name of the resources file.
    */
    public CachingResourcesManager(String resourcesName) {
        super(resourcesName);
    }
    
    /**
    * Returns the index-th resource of the specified property.
    * @param propertyName the name of the property
    * @param index the index of the resource
    */
    public String accessResource(String resource, int index) {
        String property = null;
        
        if (!cachedResource.equals(resource)) {
            String[] properties = getResourceStrings(resource);
            if ((index >= 0) && (index < properties.length)) {
                property = properties[index];
                cachedResource = resource;
                cachedProperties = properties;
            }
        }
        else if ((index >= 0) && (index < cachedProperties.length))
            property = cachedProperties[index];
        
        return property;
        
    }
}
