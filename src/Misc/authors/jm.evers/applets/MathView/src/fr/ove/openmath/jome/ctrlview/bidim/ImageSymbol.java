/*
$Id: ImageSymbol.java,v 1.3 2003/02/18 11:48:46 sander Exp $
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


package fr.ove.openmath.jome.ctrlview.bidim;

import java.awt.*;
import java.awt.image.ImageObserver;
import fr.ove.openmath.jome.ctrlview.bidim.*;
import fr.ove.openmath.jome.ctrlview.bidim.images.ImageLoader;

/**
* The Displayable implementation of an image symbol.
*
* @author © 2000 DIRAT Laurent
* @version 2.0  05/01/2000
*/
public class ImageSymbol extends DisplayableImpl {
    /**
    * The image identifier
    */
    private String imgId;
    
    /**
    * The initial image.
    * The reason of this field is we need to create an instance of the image
    * which will have a size proportional with the characters of the formula. Because
    * all the characters don't have the same size (exponents for example), we
    * need to keep a reference of the initial size of the icon, to compute
    * correctly its real size for displaying.
    */
    private Image symbolInit;
    private Image symbol;
        
    /**
    * Object to be notified as more of the image is converted. 
    */
    private ImageObserver observer;
    
    private Insets insets = new Insets(0, 0, 0, 0);
        
    /**
    * The constructor.
    * @param imgId the image identifier of the symbol.
    * @param observer the object to be notified as more of the image is converted.
    */
    public ImageSymbol(String imgId, ImageObserver observer) {
        symbolInit = ImageLoader.getImage(imgId);
        this.observer = observer;
        this.imgId = imgId;
    }
    
    /**
    * Sets the graphic context in which the object is.
    * @param graphicContext the object graphic context.
    * Actually, this methods gives information to the object, notably when its preferred
    * size is required. If the context has changed, the displayable object must be informed.
    * However, in the most part of the time, the whole graphic context won't be necessary
    * stored, but only specific and usefull data.
    */
    public void setGraphicContext(GraphicContext graphicContext) {
        super.setGraphicContext(graphicContext);
        FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(graphicContext.getFont());
        
        float heightFactor = ImageLoader.getHeightFactor(imgId);
        int fontHeight = fm.getHeight();
        
        insets.top = Math.round(ImageLoader.getTopInset(imgId)*fontHeight);
        insets.bottom = Math.round(ImageLoader.getBottomInset(imgId)*fontHeight);
        insets.left = Math.round(ImageLoader.getLeftInset(imgId)*fontHeight);
        insets.right = Math.round(ImageLoader.getRightInset(imgId)*fontHeight);
        
        setHeight((int) Math.round(fontHeight*heightFactor + insets.top + insets.bottom));
        
        float baseline = ImageLoader.getBaseline(imgId);
        int ascent;
        if (baseline == -1) {
            // C'est la baseline de la police courante.
            // Par contre, dans le cas d'un éventuel facteur de taille, l'ascent s'en trouve aussi
            // modifié
            ascent = (int) Math.round(fm.getAscent()*heightFactor);
        }
        else
            ascent = (int) Math.round(fontHeight*heightFactor*baseline);
            
        setAscent(ascent + insets.top);
        setDescent(getHeight() - ascent);
    }

    /**
    * The paint method of the object to display.
    * @param g the drawing area of the symbol.
    */
    public void paint(Graphics g) {
        g.drawImage(symbol, insets.left, insets.top, observer);
    }
    
    /**
    * Returns the preferred size of the display.
    */
    public Dimension getPreferredSize() {
		MediaTracker tracker = new MediaTracker((Component) observer);
        // Le -1 signifie que la largeur sera proportionnelle à la hauteur.
        symbol = symbolInit.getScaledInstance(-1, getHeight()- insets.top - insets.bottom, Image.SCALE_SMOOTH);
        tracker.addImage(symbol, 0);
        // On attend que toutes les images soient chargées.
		try {
		    tracker.waitForAll();
		}
		catch (InterruptedException e) {
		    System.out.print(e.toString());
		}
		        
        tracker.removeImage(symbol);
        return new Dimension(symbol.getWidth(observer) + insets.left + insets.right, getHeight());
    }
    
    /**
    * Returns the image of the symbol.
    */
    public Image getSymbol() {
        return symbol;
    }
}
