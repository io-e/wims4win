/*
$Id: PanelImage.java,v 1.3 2003/02/18 11:48:46 sander Exp $
*/


/*
Copyright (C) 2001-2002 Mainline Project (I3S - ESSI - CNRS -UNSA)

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

For further information on the GNU Lesser General Public License,
see: http://www.gnu.org/copyleft/lesser.html
For further information on this library, contact: mainline@essi.fr
*/


package fr.ove.errordialog;

import java.awt.*;
import java.io.*;
import fr.ove.utils.ByteVector;

/**
* A simple panel that contains an image to display.<BR>
* The image to display must be in the same package of the instance.
* (possibly the subpackages with the correct path specified in the 
* constructo)
*
* @author Â© 1998 DIRAT Laurent
* @version 1.0  13/04/99
*/

/* The image is loaded fom the package of the instance fr.ove.utils.<BR>
* So when we create a new instance, the path of the image to load must
* must be relatif to fr.ove.utils.
* For example, in the package fr.ove.errordialog we create a PanelImage with
* the smiley.gif image (in the same directory/package), so the instanciation
* must be new <CODE>PanelImage("../errordialog/smiley.gif");</CODE> to respect
* the requirements aforementioned.
*/
public class PanelImage extends Panel {
    private Image image;
    
    /**
    * The constructor.
    * @param imageName the name of the image the instance displays.
    */
    public PanelImage(String imageName) throws Exception {
		java.io.InputStream iStream = getClass().getResourceAsStream(imageName);
		
        try {
            /*
            int avaliable = iStream.available();
            byte imageBytes[] = new byte[avaliable];
            int bytesRead = iStream.read(imageBytes);
            bytesRead = iStream.read(imageBytes);
            image = (Toolkit.getDefaultToolkit()).createImage(imageBytes);
            iStream.close();
            */
            
            int read;
            ByteVector byteImage = new ByteVector();
		    while ((read = iStream.read()) != -1) {
		        byteImage.addElement((byte) read);
		        if (iStream.available() == 0)
		            break;
		    }
            
            image = (Toolkit.getDefaultToolkit()).createImage(byteImage.getBytes());
            iStream.close();
        }
        catch (IOException e){
            System.out.println("Impossible to read image : IO problems");
            e.printStackTrace();
        }
        catch (Exception e){
            System.out.println("Impossible to create image");
            e.printStackTrace();
        }
        
        if (image != null) {
    		MediaTracker tracker = new MediaTracker(this);
            tracker.addImage(image, 0);
        
            // On attend que toutes les images soient chargÃ©es.
    		try { 
    		    tracker.waitForAll();
    		}
    		catch (InterruptedException e) {
    		    System.out.print(e.toString());
    		}
    		
    		if (tracker.isErrorAny())
    		    throw new Exception("Impossible to load image");
        }
    }
    
    /**
    * Returns the preferred size of the instance.
    */
    public Dimension getPreferredSize() {
        return new Dimension(image.getWidth(this), image.getHeight(this));
    }
    
    /**
    * Paints the instance.
    * @param g the graphics where to paint.
    */
    public void paint(Graphics g) {
        g.drawImage(image, 0, 0, this);
    }
}        