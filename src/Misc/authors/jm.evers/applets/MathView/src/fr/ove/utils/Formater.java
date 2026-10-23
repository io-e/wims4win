/*
$Id: Formater.java,v 1.2 2003/02/18 11:48:48 sander Exp $
*/


package fr.ove.utils;

import fr.ove.utils.FormaterRepository;

/**
* The interface to implement to format an object as a string.
*
* @author © 2000 DIRAT Laurent
* @version 1.0 07/01/2000
*/
public interface Formater {
    /**
    * Returns the specified object formatted as a string.
    * @param formatedObject the formatted object (for structured object, could represents the beginning).
    * @param formaterRepository where the different other formaters are.
    * @param obj the object to format.
    */
    public String format(String formatedObject, FormaterRepository formaterRepository, Object obj);
}