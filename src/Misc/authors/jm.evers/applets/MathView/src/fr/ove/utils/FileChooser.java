/*
$Id: FileChooser.java,v 1.2 2003/02/18 11:48:48 sander Exp $
*/


package fr.ove.utils;

import javax.swing.JFileChooser;
import java.awt.Component;
import java.io.File;

/**
* A file chooser.
*/
public class FileChooser extends JFileChooser {
    /**
    *
    */
    private Component compReference = null;
    
    /**
    * The Constructor.
    */
    public FileChooser() {
        this(null);
    }
    
    /**
    * The Constructor.
    */
    public FileChooser(Component compReference) {
        super();
        this.compReference = compReference;
    }
    
    /**
    * Pops the instance for opening a file.
    */
    public int showOpen() {
        return showOpenDialog(compReference);
    }
    
    /**
    * Pops the instance for saving a file.
    */
    public int showSave() {
        return showSaveDialog(compReference);
    }
    
    /**
    *
    */
    public void setCompReference(Component compReference) {
        this.compReference = compReference;
    }
    
    /**
    * 
    */
    public Component getCompReference() {
        return compReference;
    }
}