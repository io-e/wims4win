/*
$Id: Factory.java,v 1.2 2003/02/18 11:48:48 sander Exp $
*/


package fr.ove.utils;

/**
* A little factory to dynamically instanciate classes.
*
* @author © 1999 DIRAT Laurent
* @version 1.0  29/10/1999
*/
public class Factory implements java.io.Serializable {
    /**
    * Returns an instance of the class with the specified name.
    * @param className the name of the class we want an instance.
    */
    public static Object getClassInstance(String className) {
        Object newClass = null;
        
    	try {
        	Class c = Class.forName(className);
        	
        	try {
        		newClass = c.newInstance();
        	}
        	catch (InstantiationException e ) {
        		System.out.println(className + " cannot be instantiated");
        		return null;
        	}
        	catch (IllegalAccessException e ) {
        		System.out.println(className + " impossible to access default constructor");
        		return null;
        	}
        	catch (NoClassDefFoundError e ) {
    			System.out.println("No definition of " + className + " can be found");
        	}

        }
        catch (ClassNotFoundException e) {
        	System.out.println("Class " + className + " not found");
        		return null;
        }

        return newClass;
    }
}
