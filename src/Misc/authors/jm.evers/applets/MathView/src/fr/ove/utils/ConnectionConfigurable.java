/*
$Id: ConnectionConfigurable.java,v 1.2 2003/02/18 11:48:48 sander Exp $
*/


package fr.ove.utils;

/**
* The interface to implement for all objects that are intended to configure
* a connection.
*/
public interface ConnectionConfigurable {
    /**
    * Sets the host name for the connection to configure.
    * @param name the host name.
    */
    public void setHostName(String name);
    
    /**
    * Returns the host name for the connection to configure.
    */
    public String getHostName();
    
    /**
    * Sets the port number for the connexion to configure.
    * @param number the port number.
    */
    public void setPortNumber(int number);
    
    /**
    * Returns the port number for the connexion to configure.
    */
    public int getPortNumber() ;
}