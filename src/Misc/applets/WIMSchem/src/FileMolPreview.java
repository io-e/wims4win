/*
    Sketch Elements: Chemistry molecular diagram drawing tool.
    
    (c) 2005 Dr. Alex M. Clark
    
    Released as GNUware, under the Gnu Public License (GPL)
    
    See www.gnu.org for details.
*/

package WIMSchem;

import java.util.*;
import java.text.*;
import java.lang.*;
import java.io.*;
import javax.swing.*;
import java.beans.*;
import java.awt.*;

// Previewing molecule-type files within the file choose mechanism.

public class FileMolPreview extends EditorPane implements PropertyChangeListener
{
    ImageIcon thumbnail=null;
    File file=null;

    public FileMolPreview(JFileChooser fc)
    {
    	super(200,200);
        fc.addPropertyChangeListener(this);
	setBackground(Color.WHITE);
	setBorder(true);
	setToolCursor();
	setEditable(false);
    }

    public void propertyChange(PropertyChangeEvent ev) 
    {
        boolean update=false;
        String prop=ev.getPropertyName();

        if (JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(prop)) // changed directory, do nothing much
    	{
            file=null;
            update=true;
    	}
        else if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(prop)) // file just got selected
    	{
            file=(File)ev.getNewValue();
            update=true;
        }

        if (update) 
	{
            thumbnail=null;
	    Molecule mol=null;
	    if (file!=null && file.isFile())
	    {
		try
		{
		    FileInputStream istr=new FileInputStream(file);
		    mol=MoleculeStream.readUnknown(istr);
		    istr.close();
		}
		catch (IOException e) 
		{
		    mol=null;
		}
	    }
	    if (mol==null) mol=new Molecule();
	    replace(mol);
	    scaleToFit();
            if (isShowing()) repaint();
        }
    }
    
    protected void paintComponent(Graphics g) 
    {
	Dimension sz=getSize();
	int width=(int)sz.getWidth(),height=(int)sz.getHeight();
	g.setColor(Color.WHITE);
	g.fillRect(0,0,width,height);
    	g.setColor(Color.BLACK);
	g.drawRect(0,0,width,height);
	
    	super.paintComponent(g);
    }
}
