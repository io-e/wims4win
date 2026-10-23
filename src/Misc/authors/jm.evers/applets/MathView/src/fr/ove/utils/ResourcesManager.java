/*
$Id: ResourcesManager.java,v 1.2 2003/02/18 11:48:48 sander Exp $
*/


package fr.ove.utils;

import java.net.*;
import java.util.*;

/**
* A resources manager.<BR>
* All the resources are specified in a properties file, each property defined in a key = val1:val2:....:valn.<BR>
* The key represents the name of the property. The different vali the parameters identifying the property.<BR>
* Each parameter is separated by the ':' character.
*
* @author © 1999 DIRAT Laurent
* @version 2.0  17/12/1999
*/
public class ResourcesManager implements java.io.Serializable {
    private ResourceBundle resources;
    
    /**
    * Constructor
    * @param resourcesName the name of the resources file. <BR> he name is specified without the ".properties"
    * extension. If the property file is located in a package, the fully qualified name (e.g. my.package.filename)
    * must be used.
    */
    public ResourcesManager(String resourcesName) {
        try {
            //resources = ResourceBundle.getBundle(resourcesName, Locale.getDefault());
            resources = ResourceBundle.getBundle(resourcesName);
        } 
            catch (MissingResourceException mre) {
                System.err.println(resourcesName+".properties not found");
                System.exit(1);
        }
    }

    /**
    * Get the raw string for the given property
    */
    public String getResourceString(String prop) {
        String str;
        try {
            str = resources.getString(prop);
        } 
        catch (MissingResourceException mre) {
            str = null;
        }

        return str;
    }

    /**
    * Get the resource strings for the given property
    */
    public String [] getResourceStrings(String prop) {
        return tokenize(getResourceString (prop));
    }

    /**
    * Take the given string and chop it up into a series
    * of strings on whitespace boundries.  This is useful
    * for trying to get an array of strings out of the
    * resource file.
    */
    private String[] tokenize(String input) {
        if (input != null) {
            Vector v = new Vector();
            StringTokenizer t = new StringTokenizer (input, ":");
            String cmd[];

            while (t.hasMoreTokens())
                v.addElement(t.nextToken());
                
            cmd = new String[v.size()];
            for (int i = 0; i < cmd.length; i++)
                cmd[i] = (String) v.elementAt(i);

            return cmd;
        }
        else
            return new String [0];
    }
}
