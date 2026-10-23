/*
$Id: Queue.java,v 1.2 2003/02/18 11:48:48 sander Exp $
*/


package fr.ove.utils;

import java.io.Serializable;
import java.util.*;
import fr.ove.utils.Queueable;
import fr.ove.utils.LinkedElement;

/**
* A queue.
*
* @author © 2000 DIRAT Laurent
* @version 1.0 28/06/2000
*/
public class Queue implements Queueable, Serializable {
    /**
    * The head of the queue.
    */
    private LinkedElement head;
    
    /**
    * The tail of the queue.
    */
    private LinkedElement tail;
    
    /**
    * The number of elements.
    */
    private int nbElements;
    
    /**
    * The Constructor
    */
    public Queue() {
        head = tail = null;
        nbElements = 0;
    }
    
    /**
    * Adds the specified element to the queue
    */
    public void enqueue(Object element) {
        LinkedElement newElement = new LinkedElement(element);
        if (nbElements == 0)
            head = tail = newElement;
        else {
            newElement.setNext(tail);
            tail.setPrevious(newElement);
            tail = newElement;
        }
        
        nbElements++;
    }

    /**
    * Removes the first element added into the queue.
    * Returns the element removed. <CODE>null</CODE> if the queue is empty.
    */
    public Object dequeue() {
        Object element = null;
        
        if (nbElements != 0) {
            LinkedElement dequeued = head;
            element = dequeued.getElement();
            head = dequeued.getPrevious();
            dequeued.remove();
            
            if (nbElements == 1)
                head = tail = null;
                
            nbElements--;
        }
        
        return element;
    }

    /**
    * Returns the first element added into the queue.
    * <CODE>null</CODE> if the queue is empty
    */
    public Object peek() {
        if (nbElements != 0)
            return head.getElement();
        else 
            return null;
    }

    /**
    * Checks if the queue is empty.
    * Returns <CODE>true</CODE> if the queue is empty. <CODE>false</CODE> otherwise.
    */
    public boolean isEmpty() {
        return (nbElements == 0);
    }
    
    /**
    * Clears the queue
    */
    public void clear() {
        head = tail = null;
        nbElements = 0;
    }
        
    /**
    * Returns an @see Enumeration of the elements in the queue.
    */
    public Enumeration elements() {
        synchronized (this) {
            return (Enumeration) new QueueEnumerator(this);
        }
    }
    
    
    /**
    * Returns an array of the elements contained in the queue.<BR>
    * If the queue is empty, returns <CODE>null</CODE>.
    */
    public Object[] getElements() {
        Object elements[] = null;
        
        if (nbElements != 0) {
            elements = new Object[nbElements];
            
            LinkedElement ptr = tail;
            int index = 0;
            while (ptr != null) {
                elements[index++] = ptr.getElement();
                ptr = ptr.getNext();
            }
        }
        
        return elements;
    }
    
    /**
    * Returns a string representation of the queue
    */
    public String toString() {
        String string = "";
        LinkedElement ptr = tail;
        
        while (ptr != null) {
            string += " " + ptr.toString();
            ptr = ptr.getNext();
        }
        
        return string + " ";
    }



    /*
    * An Enumeration representation of a queue.
    */
    private class QueueEnumerator implements Enumeration, Serializable {
        /**
        * The queue to enumerate
        */
        Queue queue;
        
        /**
        * The next element in the queue;
        */
        LinkedElement next;
        
        /**
        * The constructor
        * @param queue the queue to enumerate
        */
        public QueueEnumerator(Queue queue) {
            this.queue = queue;
            next = queue.head;
        }
        
        /**
         * Tests if this enumeration contains more elements.
         *
         * @return  <code>true</code> if this enumeration contains more elements;
         *          <code>false</code> otherwise.
         */
        public boolean hasMoreElements() {
            return (next != null);
        }

        /**
         * Returns the next element of this enumeration.
         *
         * @return     the next element of this enumeration. 
         * @exception  NoSuchElementException  if no more elements exist.
         */
        public Object nextElement() {
        	synchronized (queue) {
        	    if (next != null) {
        	        Object element = next.getElement();
        	        next = next.getPrevious();
            		return element;
            	}
        	}
        	
        	throw new NoSuchElementException("QueueEnumerator");
        }
    }

}