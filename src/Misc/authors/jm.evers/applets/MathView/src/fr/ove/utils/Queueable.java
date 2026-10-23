/*
$Id: Queueable.java,v 1.2 2003/02/18 11:48:48 sander Exp $
*/


package fr.ove.utils;

import java.util.*;

/**
* Interface to respect to have a queue behaviour
*
* @author © 2000 DIRAT Laurent
* @version 1.0 28/06/2000
*/

public interface Queueable {
    /**
    * Adds the specified element to the queue
    */
    public void enqueue(Object element); 

    /**
    * Removes the first element added into the queue.
    * Returns the element removed. <CODE>null</CODE> if the queue is empty
    */
    public Object dequeue();

    /**
    * Returns the first element added into the queue.
    * <CODE>null</CODE> if the queue is empty
    */
    public Object peek();

    /**
    * Checks if the queue is empty.
    * Returns <CODE>true</CODE> if the queue is empty. <CODE>false</CODE> otherwise.
    */
    public boolean isEmpty();
    
    /**
    * Clears the queue
    */
    public void clear();
    
    /**
    * Returns an @see Enumeration of the elements in the queue.
    */
    public Enumeration elements();
}