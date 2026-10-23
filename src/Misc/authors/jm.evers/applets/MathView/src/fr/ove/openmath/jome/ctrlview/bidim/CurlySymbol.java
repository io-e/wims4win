/*
$Id: CurlySymbol.java,v 1.3 2003/02/18 11:48:46 sander Exp $
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
* The curly bracket.
*
* @author © 1999 DIRAT Laurent
* @version 2.0 19/07/1999
*/
public class CurlySymbol extends DisplayableImpl {
    /**
    * The initial images which are parts of the representation of the curly bracket.<BR>
    * The curly bracket is made of 4 parts : top, middle, bottom and bar.<BR>
    * The top, middle and bottom are unique during the rendering, and according to the height of the
    * bracketted display, mutiple instance of the bar are displayed to give the right size
    * to the curly bracket.
    */
    private Image topInit;
    private Image middleInit;
    private Image bottomInit;
    private Image barInit;
    
    /**
    * The scale images of the initials ones.
    */
    private Image top;
    private Image middle;
    private Image bottom;
    private Image bar;
    
    /**
    * The height of the top and the bottom of the symbol.
    */
    private int heightExtremities;
    
    /**
    * Object to be notified as more of the image is converted. 
    */
    ImageObserver observer;
     
    /**
    * The constructor.
    * @param isLeftBracket <CODE>true<CODE> if the instance represents a left bracket. <CODE>false</CODE>
    * otherwise.
    * @param heightExtremities the height of the extremities of the bracket.
    * @param observer the object to be notified as more of the image is converted.
    */
    public CurlySymbol(boolean isLeftBracket, int heightExtremities, ImageObserver observer) {
        if (isLeftBracket) {
            topInit = ImageLoader.getImage("LeftTopCurl");
            middleInit = ImageLoader.getImage("LeftMiddleCurl");
            bottomInit = ImageLoader.getImage("LeftBottomCurl");
            barInit = ImageLoader.getImage("LeftBarCurl");
        }
        else {
            topInit = ImageLoader.getImage("RightTopCurl");
            middleInit = ImageLoader.getImage("RightMiddleCurl");
            bottomInit = ImageLoader.getImage("RightBottomCurl");
            barInit = ImageLoader.getImage("RightBarCurl");
        }

        this.heightExtremities = heightExtremities;
        this.observer = observer;
    }
    
    /**
    * The paint method of the object to display.
    * @param g the drawing area of the symbol.
    */
    public void paint(Graphics g) {
        int height = getHeight();
        
        // On dessine la partie supérieure de l'accolade
        g.drawImage(top, 3, 0, observer);
        
        // On dessine les "barres", i.e. la partie que l'on va faire croitre pour ajuster correctement
        // la taille de l'accolade
        for (int i = heightExtremities; i < height - heightExtremities; i += heightExtremities)
            g.drawImage(bar, 3, i, observer);
        
        // On dessine la partie inférieure de l'accolade
        g.drawImage(bottom, 3, height - heightExtremities, observer);
        
        // On dessine la partie centrale de l'accolade
        // On essaie de faire en sorte que le milieu de l'accolade se trouve un peu au dessus de la baseline
        // Par exemple, aligné avec le +. La barre horizontale du + se trouve grosso modo à la moitié de
        // son ascent. Il se trouve que c'est la hauteur de middle.
        g.drawImage(middle, 3, getAscent() - middle.getHeight(observer), observer);
    }
    
    /**
    * Returns the preferred size of the display.
    */
    public Dimension getPreferredSize() {
		MediaTracker tracker = new MediaTracker((Component) observer);
		
		// La hauteur de la partie du milieu est égale à environ 2 fois les extrémités.
		
        // Le -1 signifie que la largeur sera proportionnelle à la hauteur.
        top = topInit.getScaledInstance(-1, heightExtremities, Image.SCALE_SMOOTH);
        tracker.addImage(top, 0);
        middle = middleInit.getScaledInstance(-1, heightExtremities*2, Image.SCALE_SMOOTH);
        tracker.addImage(middle, 0);
        bottom = bottomInit.getScaledInstance(-1, heightExtremities, Image.SCALE_SMOOTH);
        tracker.addImage(bottom, 0);
        bar = barInit.getScaledInstance(-1, heightExtremities, Image.SCALE_SMOOTH);
        tracker.addImage(bar, 0);
        
        // On attend que toutes les images soient chargées.
		try { tracker.waitForAll(); }
		catch (InterruptedException e) {
		    System.out.println(e.toString());
		}
		
        tracker.removeImage(top);
        tracker.removeImage(middle);
        tracker.removeImage(bottom);
        tracker.removeImage(bar);

        return new Dimension(top.getWidth(observer) + 6, getHeight());
    }
    
    /**
    * Sets the graphic context in which the object is.
    * @param graphicContext the object graphic context.
    * Actually, this methods gives information to the object, notably when its preferred
    * size is required. If the context has changed, the displayable object must be informed.
    * However, in the most part of the time, the whole graphic context won't be necessary to
    * be stored, but only specific and usefull data.
    */
    public void setGraphicContext(GraphicContext graphicContext) {
        super.setGraphicContext(graphicContext);
        // Surcharge de cette méthode pour que l'on puisse récupérer en fonction du contexte graphique
        // du symbole, la hauteur des extrémité de l'accolade.
        FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(graphicContext.getFont());
        heightExtremities = fm.getHeight() / 4;
    }
}
