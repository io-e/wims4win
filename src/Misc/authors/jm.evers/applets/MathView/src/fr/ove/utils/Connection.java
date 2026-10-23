/*
$Id: Connection.java,v 1.2 2003/02/18 11:48:48 sander Exp $
*/


package fr.ove.utils;

import java.io.*;
import java.net.*;

/**
* A connection object.
* Opens a socket to the specified host on the specified port number.
*/
public class Connection implements ConnectionConfigurable {
    /**
    * The socket of the connection
    */
    private Socket socket;

    /**
    * The input stream to read from the socket.
    */
    private InputStream inputStream = null;

    /**
    * The output stream to write into the socket.
    */
    private OutputStream outputStream = null;

    /**
    * The host name to connect.
    */
    private String hostName;

    /**
    * The port number.
    */
    private int portNumber;

    /**
    * The default constructor.
    * The default host name is "localhost" and the default port number is 6666.
    */
    public Connection() {
        this("localhost", 6666);
    }

    /**
    * The Constructor.
    * @param hostName the host name.
    * @param portNumber the port number.
    */
    public Connection(String hostName, int portNumber) {
        this.hostName = hostName;
        this.portNumber = portNumber;
    }

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
    * Opens the connection.
    */
    public void open() throws IOException {
		socket = new Socket(InetAddress.getByName(hostName), portNumber);
		inputStream = socket.getInputStream();
		outputStream = socket.getOutputStream();
    }

    /**
    * Opens the connection through the specified socket.
    * @param socket the specified socket.
    */
    public void open(Socket socket) throws IOException {
		this.socket = socket;
		hostName = socket.getInetAddress().getHostName();
		portNumber = socket.getPort();
		inputStream = socket.getInputStream();
		outputStream = socket.getOutputStream();
    }

    /**
    * Checks wether the connection is opened.
    */
    public boolean isOpened() {
        return ((outputStream != null) && (inputStream != null));
    }

    /**
    * Closes the connection.
    */
    public void close() throws IOException {
        outputStream.close();
        inputStream.close();
        outputStream = null;
        inputStream = null;
    }

    /**
    * Gets the input stream to read from the socket.
    */
    public InputStream getInputStream() {
        return inputStream;
    }

    /**
    * Gets the output stream to write into the socket.
    */
    public OutputStream getOutputStream() {
        return outputStream;
    }
}