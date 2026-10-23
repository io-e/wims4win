/*
$Id: FontInfo.java,v 1.2 2003/02/18 11:48:48 sander Exp $
*/


package fr.ove.utils;

import java.awt.*;
import java.io.Serializable;
import java.awt.image.*;

/**
* A class that gives information about the font used.
*
* @author © 1999 DIRAT Laurent
* @version 2.0  01/09/1999
*/
public class FontInfo implements Serializable {
    /**
    * Returns the thickness of the line elements of the characters in the specified font.<BR>
    * Typically this corresponds to the width of the character '|'.
    * @param component
    * @param font the specified font
    */
    public static int getLineThickness(Component component, Font font) {
        // On créé une petite image contenant le caractère '-'.
        // Le fond de cette image est blanc, tandis que le charactère est écrit en noir.
        // Une fois l'image générée, on va en créer un tableau d'entier, que l'on va parcourir jusqu'à
        // trouver un entier dont la valeur est différente de la valeur du blanc. (cela correspond à tomber
        // sur le début du '-', l'image étant parcourrue de haut en bas). Tant que l'on trouve cette valeur,
        // on est sur le '-'. Dès que l'on retombe sur la valeur du blanc, c'est qu'on est sorti du '-'.
        // On a récupéré l'épaisseur du charactère.
        // L'alignement sur le '-' correspond à la distance du haut de l'image sur le début du '-' plus la moitié
        // de sa taille.

        Graphics g = component.getGraphics();
        // On récupère les caractéristiques de la police utilisée
        FontMetrics fontMetrics = g.getFontMetrics(font);
        int height = fontMetrics.getHeight();
        int width = fontMetrics.stringWidth("-");
        
        // On créé l'image où l'on va écrire les '|'
        Image image = component.createImage(1, height);
        g = image.getGraphics();
        // Le fond est blanc et...
        g.setColor(Color.white);
        g.fillRect(0, 0, 1, height);
        // ...on écrit en noir
        g.setColor(Color.black);
        g.setFont(font);
        g.drawString("-", -width/2, fontMetrics.getAscent());

        // On récupère un tableau d'entier de l'image
	    int img[] = new int[height];
        PixelGrabber grabber = new PixelGrabber(image, 0, 0, 1, height, img, 0, 1);
        try {
    	    grabber.grabPixels();
	    }
	    catch (InterruptedException e) {
    	    System.err.println("interrupted waiting for pixels!");
	        return -1;
    	}

    	if ((grabber.getStatus() & ImageObserver.ABORT) != 0) {
	        System.err.println("image fetch aborted or errored");
	        return -1;
    	}

    	int blanc = img[0];
    	int thickness = 0;
    	for (int i = 0; i < height; i++) {
    	    if (img[i] == blanc) {
    	        if (thickness == 0)
        	        continue;  // depuis le début on est sur du blanc, on continue le parcours de l'image
        	    else
        	        break; // on a trouvé du noir, et on retrouve du blanc, on a l'épaisseur, on arrête.
        	}

        	thickness++;
        }

        return (thickness > 0) ? thickness : 1;
    }
    
    /**
    * Returns the width of the specified string, with the specified font.
    * @param component
    * @param font the specified font
    * @param str the specified string.
    */
    public static int getStringWidth(Component component, Font font, String str) {
        Graphics g = component.getGraphics();
        FontMetrics fontMetrics = g.getFontMetrics(font);
        return fontMetrics.stringWidth(str);
    }
    
    /**
    * Returns the ascent corresponding to the middle of the width of the minus character
    * @param component
    * @param font the specified font
    */
    public static int getMinusAlignment(Component component, Font font) {
        // On créé une petite image contenant le caractère '-'.
        // Le fond de cette image est blanc, tandis que le charactère est écrit en noir.
        // Une fois l'image générée, on va en créer un tableau d'entier, que l'on va parcourir jusqu'à
        // trouver un entier dont la valeur est différente de la valeur du blanc. (cela correspond à tomber
        // sur le début du '-', l'image étant parcourrue de haut en bas). Tant que l'on trouve cette valeur,
        // on est sur le '-'. Dès que l'on retombe sur la valeur du blanc, c'est qu'on est sorti du '-'.
        // On a récupéré l'épaisseur du charactère.
        // L'alignement sur le '-' correspond à la distance du haut de l'image sur le début du '-' plus la moitié
        // de sa taille.

        Graphics g = component.getGraphics();
        // On récupère les caractéristiques de la police utilisée
        FontMetrics fontMetrics = g.getFontMetrics(font);
        int height = fontMetrics.getHeight();
        int width = fontMetrics.stringWidth("-");
        
        // On créé l'image où l'on va écrire les '|'
        Image image = component.createImage(1, height);
        g = image.getGraphics();
        // Le fond est blanc et...
        g.setColor(Color.white);
        g.fillRect(0, 0, 1, height);
        // ...on écrit en noir
        g.setColor(Color.black);
        g.setFont(font);
        g.drawString("-", -width/2, fontMetrics.getAscent());

        // On récupère un tableau d'entier de l'image
	    int img[] = new int[height];
        PixelGrabber grabber = new PixelGrabber(image, 0, 0, 1, height, img, 0, 1);
        try {
    	    grabber.grabPixels();
	    }
	    catch (InterruptedException e) {
    	    System.err.println("interrupted waiting for pixels!");
	        return -1;
    	}

    	if ((grabber.getStatus() & ImageObserver.ABORT) != 0) {
	        System.err.println("image fetch aborted or errored");
	        return -1;
    	}

    	int blanc = img[0];
    	int thickness = 0;
    	int startMinus = 0;
    	for (int i = 0; i < height; i++) {
    	    if (img[i] == blanc) {
    	        if (thickness == 0) {
    	            startMinus++;
        	        continue;  // depuis le début on est sur du blanc, on continue le parcours de l'image
                }
        	    else
        	        break; // on a trouvé du noir, et on retrouve du blanc, on a l'épaisseur, on arrête.
        	}

        	thickness++;
        }
        
        thickness /= 2;
        thickness = (thickness > 0) ? thickness : 1;
        
        return startMinus + thickness;
    }
    public static int getPlusAlignment(Component component, Font font) {
        // On créé une petite image contenant le caractère '-'.
        // Le fond de cette image est blanc, tandis que le charactère est écrit en noir.
        // Une fois l'image générée, on va en créer un tableau d'entier, que l'on va parcourir jusqu'à
        // trouver un entier dont la valeur est différente de la valeur du blanc. (cela correspond à tomber
        // sur le début du '-', l'image étant parcourrue de haut en bas). Tant que l'on trouve cette valeur,
        // on est sur le '-'. Dès que l'on retombe sur la valeur du blanc, c'est qu'on est sorti du '-'.
        // On a récupéré l'épaisseur du charactère.
        // L'alignement sur le '-' correspond à la distance du haut de l'image sur le début du '-' plus la moitié
        // de sa taille.

        Graphics g = component.getGraphics();
        // On récupère les caractéristiques de la police utilisée
        FontMetrics fontMetrics = g.getFontMetrics(font);
        int height = fontMetrics.getHeight();
        int width = fontMetrics.stringWidth("+");
        
        // On créé l'image où l'on va écrire les '|'
        Image image = component.createImage(1, height);
        g = image.getGraphics();
        // Le fond est blanc et...
        g.setColor(Color.white);
        g.fillRect(0, 0, 1, height);
        // ...on écrit en noir
        g.setColor(Color.black);
        g.setFont(font);
        g.drawString("+", -width/2, fontMetrics.getAscent());

        // On récupère un tableau d'entier de l'image
	    int img[] = new int[height];
        PixelGrabber grabber = new PixelGrabber(image, 0, 0, 1, height, img, 0, 1);
        try {
    	    grabber.grabPixels();
	    }
	    catch (InterruptedException e) {
    	    System.err.println("interrupted waiting for pixels!");
	        return -1;
    	}

    	if ((grabber.getStatus() & ImageObserver.ABORT) != 0) {
	        System.err.println("image fetch aborted or errored");
	        return -1;
    	}

    	int blanc = img[0];
    	int thickness = 0;
    	int startMinus = 0;
    	for (int i = 0; i < height; i++) {
    	    if (img[i] == blanc) {
    	        if (thickness == 0) {
    	            startMinus++;
        	        continue;  // depuis le début on est sur du blanc, on continue le parcours de l'image
                }
        	    else
        	        break; // on a trouvé du noir, et on retrouve du blanc, on a l'épaisseur, on arrête.
        	}

        	thickness++;
        }
        
        thickness /= 2;
        thickness = (thickness > 0) ? thickness : 1;
        
        return startMinus + thickness;
    }
}