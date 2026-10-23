/*
$Id: ConnectionFactory.java,v 1.2 2003/02/18 11:48:48 sander Exp $
*/


package fr.ove.utils;

import fr.ove.utils.Connection;

/**
* A connection factory.
*/
public class ConnectionFactory implements ConnectionConfigurable {
    /**
    * The host name for the connection to create.
    * The default host is localhost.
    */
    private String hostName = "localhost";
    
    /**
    * The port number for the connection to create.
    * The default port number is 6666;
    */
    private int portNumber = 6666;
    
    /**
    * Sets the host name for the connection to create.
    * @param name the host name.
    */
    public void setHostName(String name) {
        hostName = name;
    }
    
    /**
    * Returns the host name for the connection to create.
    */
    public String getHostName() {
        return hostName;
    }
    
    /**
    * Sets the port number for the connexion to create.
    * @param number the port number.
    */
    public void setPortNumber(int number) {
        portNumber = number;
    }
    
    /**
    * Returns the port number for the connexion to create.
    */
    public int getPortNumber() {
        return portNumber;
    }
    
    /**
    * Returns a connection with the set parameters.
    */
    public Connection createConnection() {
        return new Connection(hostName, portNumber);
    }
    
    /**
    * Returns a connection with the specified parameters.
    * @param hostName the host name.
    * @param portNumber the port number.
    */
    public static Connection createConnection(String hostName, int portNumber) {
        return new Connection(hostName, portNumber);
    }
}