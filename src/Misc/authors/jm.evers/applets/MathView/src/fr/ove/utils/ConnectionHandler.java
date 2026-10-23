/*
$Id: ConnectionHandler.java,v 1.2 2003/02/18 11:48:48 sander Exp $
*/


package fr.ove.utils;

import fr.ove.utils.Connection;

/**
* A connection handler.<BR>
*/
public interface ConnectionHandler {
    /**
    * Handles the specified connection.
    * @param connection the connection to handle.
    */
    public void handleConnection(Connection connection);
}