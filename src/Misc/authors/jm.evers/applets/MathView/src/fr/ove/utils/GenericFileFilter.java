/*
$Id: GenericFileFilter.java,v 1.2 2003/02/18 11:48:48 sander Exp $
*/


package fr.ove.utils;

import javax.swing.filechooser.FileFilter;
import java.io.File;

/**
* A file filter for the specified extension.
*/
public class GenericFileFilter extends FileFilter {
    /**
    * The type of files to filter.
    */
    private String type;
    
    /**
    * The extension of files to filter.
    */
    private String extension;
    
    /**
    * The default constructor. Builds a text files (with a txt extension) filter.
    */
    public GenericFileFilter() {
        this("Text", "txt");
    }
    
    /**
    * The constructor.
    * @param type the file type (e.g. Text, JPEG, etc).
    * @param extension the file extension (e.g. txt, jpeg, etc).
    */
    public GenericFileFilter(String type, String extension) {
        this.type = type;
        this.extension = extension;
    }
    
    /**
    * Sets the file type.
    * @param type the file type (e.g. Text, JPEG, etc).
    */
    public void setType(String type) {
        this.type = type;
    }
    
    /**
    * Gets the file type.
    */
    public String getType() {
        return type;
    }
    
    /**
    * Sets the file extension.
    * @param extension the file extension (e.g. txt, jpeg, etc);
    */
    public void setExtension(String extension) {
        this.extension = extension;
    }
    
    /**
    * Gets the file extension.
    */
    public String getExtension() {
        return extension;
    }
    
    /**
    * Whether the given file is accepted by this filter.
    * @param file the given file.
    */
    public boolean accept(File file) {
        boolean accept = file.isDirectory();
        
        if (!accept) {
            String suffix = getSuffix(file);
            if (suffix != null)
                accept = suffix.equals(extension);
        }
        
        return accept;
    }
    
    /**
    * The description of this filter.
    */
    public String getDescription() {
        return type + " Files (*." + extension + ")";
    }
    
    /**
    * Gets the suffix of the specified file.
    * @param file the file.
    */
    private String getSuffix(File file) {
        String path = file.getPath();
        String suffix = null;
        int index = path.lastIndexOf('.');
        if ((index > 0) && (index < (path.length() - 1)))
            suffix = path.substring(index + 1).toLowerCase();
            
        return suffix;
    }
}