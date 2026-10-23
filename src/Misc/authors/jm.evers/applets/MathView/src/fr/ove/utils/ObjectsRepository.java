/*
$Id: ObjectsRepository.java,v 1.2 2003/02/18 11:48:48 sander Exp $
*/


package fr.ove.utils;

import java.util.*;

/**
* A repository of the main objects used by the programm.
* <P>
* It is used as a repository of the different objects created that are
* supposed to be used (listener registering, ...) by other objects.
* Provide a convenient way to give the reference of objects to an another one. 
* Rather than explicitly pass the reference to objects needed, pass the
* instance. The needed references are then obtains via a name that identifies
* the different objects.
*/
public class ObjectsRepository {
    /**
    * The repository
    */
    private Hashtable repository;
    
    /**
    * The Constructor.
    */
    public ObjectsRepository() {
        repository = new Hashtable();
    }
    
    /**
    * Puts an object with the specified name idenfier in the repository.
    * @param object the object to add.
    * @param name the name that identifies the object.
    */
    public Object putObject(Object object, String name) {
        return repository.put(name, object);
    }
    
    /**
    * Gets the object with  the specified name indentifier in the repository.
    * @param name the name that identifies the object.
    */
    public Object getObject(String name) {
        return repository.get(name);
    }
    
    /**
    * Remove the object with the specified name identifier from  the repository.
    * @param name the name that identifies the object.
    */
    public Object removeObject(String name) {
        return repository.remove(name);
    }
    
    /**
    * Gets the repository.
    */
    public Hashtable getRepository() {
        return repository;
    }
}