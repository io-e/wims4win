/*
$Id: FormaterRepository.java,v 1.2 2003/02/18 11:48:48 sander Exp $
*/


package fr.ove.utils;

import fr.ove.utils.Formater;

/**
* A formater repository.<BR>
* The aim of this class is to format, as a string, an object by the means of @see Formater the instance manages.<BR>
* To each objet (or part of object) corresponds a @see Formater.
* <P>
* The different subclasses have to implement the @see format method in the @see Formater interface the instance
* pretends to implement.
*
* @author © 2000 DIRAT Laurent
* @version 1.0 07/01/2000
*/
public abstract class FormaterRepository implements Formater {
    private String indent = "  ";
    
    /**
    * Returns the current indentation
    */
    public String getIndent() {
        return indent;
    }
    
    /**
    * Increments the indentation.
    */
    public void incIndent() {
        indent += "  ";
    }
    
    /**
    * Decrements the indentation.
    */
    public void decIndent() {
        indent = indent.substring(0, indent.length() - 2);
    }
    
    /**
    * Init indentation
    */
    public void initIndent() {
        indent = "  ";
    }
    
    /**
    * Returns the formater with the specified identifier.
    * @param objectId the object (to format) identifier.
    */
    public abstract Formater getFormater(String objectId);
}