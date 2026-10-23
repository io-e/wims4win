/*
$Id: ImageLoader.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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


package fr.ove.openmath.jome.ctrlview.bidim.images;

import java.applet.*;
import java.awt.*;
import java.util.*;
import java.io.*;
import fr.ove.openmath.jome.ctrlview.bidim.images.*;
import fr.ove.utils.ByteVector;

/**
* This class is a little tool to load images.
*
* @author © 1998 DIRAT Laurent
* @version 1.0  17/02/99
*/
public class ImageLoader implements Serializable {
    /**
    * The hashtable which contains all the images loaded.
    */
    private static Hashtable allImages;
    // On utilise un mediatracker pour pouvoir attendre que toutes les
    // images soient chargées avant de rendre la main.
	private static MediaTracker tracker;
    // On récupère la toolkit par défaut pour créer les images.
    private static Toolkit toolkit;
    private static Class theClass;
    
    /**
    * The image resources manager.
    */
    private static ImagesResourcesManager imagesResourcesManager = new ImagesResourcesManager("fr.ove.openmath.jome.ctrlview.bidim.images.ImagesResources");
    
    /**
    * The Constructor.<BR>
    * This constructor is used in the case of an application, and not an applet such
    * as the precedent does.
    * @param component the component which needs the images to load.
    */
    public ImageLoader(Component component) {
		theClass = getClass();

        // On utilise un mediatracker pour pouvoir attendre que toutes les
        // images soient chargées avant de rendre la main.
		tracker = new MediaTracker(component);
        allImages = new Hashtable(10);  // On crée la table de Hashcode
        
        // On récupère la toolkit par défaut pour créer les images.
        toolkit = Toolkit.getDefaultToolkit();
        
        // On charge les parenthèses pour que ce soit un peu plus rapide (c'est a priori
        // les images les plus suceptibles d'être chargées)
        getImage("LeftTopPar");
        getImage("LeftMiddlePar");
        getImage("LeftBottomPar");
        getImage("RightTopPar");
        getImage("RightMiddlePar");
        getImage("RightBottomPar");
    }

    /**
    * Returns the image with the specified name.
    * @param name the name of the desired image.
    * @return the desired image.
    */
    public static Image getImage(String name) {
        Image image = (Image) allImages.get(name);

        if (image == null)
            image = setImage(name);

        return image;
    }
    
    
    /**
    * Returns the height factor to apply to the orignal size of the wanted image.
    * @param idImage the identifier of the wanted image
    */
    public static float getHeightFactor(String idImage) {
        float heightFactor = 1.0f;
        
        try {
            String height = imagesResourcesManager.getHeightFactor(idImage);
            if (height != null)
                heightFactor = Float.valueOf(height).floatValue();
        }
        catch (NumberFormatException fne) {
            System.out.println("height factor is not a valid number.");
        }
        catch (Exception e) {
            System.out.println("height factor is not a valid number."); // erreur d'un autre type
        }
        
        return heightFactor;
    }    
    
    /**
    * Returns the baseline of the wanted image. If the returned value equals -1n the baseline corresponds
    * the one of the current font.
    * @param idImage the identifier of the wanted image
    */
    public static float getBaseline(String idImage) {
        float baseline = -1.0f; // Si -1, alors la baseline est celle de la police courante.
        
        try {
            String base = imagesResourcesManager.getBaseline(idImage);
            if (base != null)
                baseline = Float.valueOf(base).floatValue();
        }
        catch (NumberFormatException fne) {
            System.out.println("baseline has not a valid number.");
        }
        catch (Exception e) {
            System.out.println("baseline has not a valid number."); // erreur d'un autre type
        }
        
        return baseline;
    }
    
    /**
    * @param idImage the identifier of the wanted image
    */
    public static float getTopInset(String idImage) {
        float inset = 0.0f;
        
        try {
            String srtInset = imagesResourcesManager.getTopInset(idImage);
            if (srtInset != null)
                inset = Float.valueOf(srtInset).floatValue();
        }
        catch (NumberFormatException fne) {
            System.out.println("top inset has not a valid number.");
        }
        catch (Exception e) {
            System.out.println("top inset has not a valid number."); // erreur d'un autre type
        }
        
        return inset;
    }
    
    /**
    * @param idImage the identifier of the wanted image
    */
    public static float getBottomInset(String idImage) {
        float inset = 0.0f;
        
        try {
            String srtInset = imagesResourcesManager.getBottomInset(idImage);
            if (srtInset != null)
                inset = Float.valueOf(srtInset).floatValue();
        }
        catch (NumberFormatException fne) {
            System.out.println("bottom inset has not a valid number.");
        }
        catch (Exception e) {
            System.out.println("bottom inset has not a valid number."); // erreur d'un autre type
        }
        
        return inset;
    }
    
    /**
    * @param idImage the identifier of the wanted image
    */
    public static float getLeftInset(String idImage) {
        float inset = 0.0f;
        
        try {
            String srtInset = imagesResourcesManager.getLeftInset(idImage);
            if (srtInset != null)
                inset = Float.valueOf(srtInset).floatValue();
        }
        catch (NumberFormatException fne) {
            System.out.println("left inset has not a valid number.");
        }
        catch (Exception e) {
            System.out.println("left inset has not a valid number."); // erreur d'un autre type
        }
        
        return inset;
    }
    
    /**
    * @param idImage the identifier of the wanted image
    */
    public static float getRightInset(String idImage) {
        float inset = 0.0f;
        
        try {
            String srtInset = imagesResourcesManager.getRightInset(idImage);
            if (srtInset != null)
                inset = Float.valueOf(srtInset).floatValue();
        }
        catch (NumberFormatException fne) {
            System.out.println("right inset has not a valid number.");
        }
        catch (Exception e) {
            System.out.println("right inset has not a valid number."); // erreur d'un autre type
        }
        
        return inset;
    }
        
    /**
    * Checks if there exist an image with the specified identifier.
    * @param idImage the identifier of the wanted image
    */
    public static boolean exists(String idImage) {
        return (imagesResourcesManager.getImageName(idImage) == null);
    }
    
    
    
    /**
    * Loads the image with the specified name and put it in the hashtable with the specified key.
    * @param name the name of theimage to load
    * @param key the key of the image in the hastable
    * @return the desired image
    */
    private static Image loadImage(String name, String key) throws Exception {
        Image image = null;
		java.io.InputStream iStream = theClass.getResourceAsStream(name);
        try {
            int read;
            ByteVector byteImage = new ByteVector();
		    while ((read = iStream.read()) != -1) {
		        byteImage.addElement((byte) read);
		        if (iStream.available() == 0)
		            break;
		    }
            
            image = toolkit.createImage(byteImage.getBytes());
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
            allImages.put(key, image);
            tracker.addImage(image, 0);
        
            // On attend que toutes les images soient chargées.
    		try { 
    		    tracker.waitForAll();
    		}
    		catch (InterruptedException e) {
    		    System.out.print(e.toString());
    		}
    		
    		if (tracker.isErrorAny())
    		    throw new Exception("Impossible to load image");
        }
        return image;
    }
    
    private static Image setImage(String name) {
        Image image = null;
        try {
            String imgName = imagesResourcesManager.getImageName(name);
            
            if (imgName == null)
                image = getImage("undef");
            else
                image = loadImage(imgName, name);
                        
            return image;
        }
        catch (Exception e) {
            e.printStackTrace();
            return image;
        }
    }
}
