/*
$Id: LinkedElement.java,v 1.2 2003/02/18 11:48:48 sander Exp $
*/


package fr.ove.utils;

import java.io.Serializable;

/**
* A element of linked structure (queue, lists, etc..
* <P>
* Contains the element added in the structure and the reference to the previous
* and the next element in the structure.
*
* @author © 2000 DIRAT Laurent
* @version 1.0 28/06/2000
*/
public class LinkedElement implements Serializable {
    /**
    * The element in the structure.
    */
    private Object element;
    
    /**
    * The previous element in the structure.
    */
    private LinkedElement previous;
    
    /**
    * The next element in the structure.
    */
    private LinkedElement next;
    
    /**
    * The default contstructor
    */
    public LinkedElement() {
        this(null);
    }
    
    /**
    * The contstructor
    * @param element the element in the structure
    */
    public LinkedElement(Object element) {
        this.element = element;
        previous = null;
        next = null;
    }
    
    /**
    * Sets the element
    */
    public void setElement(Object element) {
        this.element = element;
    }
    
    /**
    * Returns the element
    */
    public Object getElement() {
        return element;
    }
    
    /**
    * Sets the previous element of the instance
    */
    public void setPrevious(LinkedElement previous) {
        this.previous = previous;
    }
    
    /**
    * Returns the previous element of the instance
    */
    public LinkedElement getPrevious() {
        return previous;
    }
    
    /**
    * Sets the next element of the instance
    */
    public void setNext(LinkedElement next) {
        this.next = next;
    }
    
    /**
    * Returns the next element of the instance
    */
    public LinkedElement getNext() {
        return next;
    }
    
    /**
    * Removes the instance from the structure
    */
    public void remove() {
        element = null;
        
        if (previous != null)
            previous.next = next;
            
        if (next != null)
            next.previous = previous;
    }
    
    /**
    * Returns a string representation of the element
    */
    public String toString() {
        return element.toString();
    }
}