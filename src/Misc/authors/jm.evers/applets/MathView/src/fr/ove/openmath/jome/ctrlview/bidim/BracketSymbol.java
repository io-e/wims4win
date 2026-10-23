/*
$Id: BracketSymbol.java,v 1.3 2003/02/18 11:48:46 sander Exp $
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
* The bracket.
*
* @author © 1999 DIRAT Laurent
* @version 2.0 19/07/1999
*/
public class BracketSymbol extends DisplayableImpl {
    /**
    * The initial images which are parts of the representation of the bracket.<BR>
    * The bracket is made of 3 parts : top, middle and bottom.<BR>
    * The top and bottom are unique during the rendering, and according to the height of the
    * bracketted display, mutiple instance of the middle are displayed to give the right size
    * to the brackets.
    */
    private Image topInit;
    private Image middleInit;
    private Image bottomInit;
    
    /**
    * The scale images of the initials ones.
    */
    private Image top;
    private Image middle;
    private Image bottom;
    
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
    public BracketSymbol(boolean isLeftBracket, int heightExtremities, ImageObserver observer) {
        if (isLeftBracket) {
            topInit = ImageLoader.getImage("LeftTopPar");
            middleInit = ImageLoader.getImage("LeftMiddlePar");
            bottomInit = ImageLoader.getImage("LeftBottomPar");
        }
        else {
            topInit = ImageLoader.getImage("RightTopPar");
            middleInit = ImageLoader.getImage("RightMiddlePar");
            bottomInit = ImageLoader.getImage("RightBottomPar");
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
        
        // On dessine la partie supérieure de la parenthèse
        g.drawImage(top, 0, 0, observer);
        
        // On dessine la partie centrale de la parenthèse
        for (int i = heightExtremities; i < height - heightExtremities; i += heightExtremities)
            g.drawImage(middle, 0, i, observer);
        
        // On dessine la partie inférieure de la parenthèse
        g.drawImage(bottom, 0, height - heightExtremities, observer);
    }
    
    /**
    * Returns the preferred size of the display.
    */
    public Dimension getPreferredSize() {
		MediaTracker tracker = new MediaTracker((Component) observer);
        // Le -1 signifie que la largeur sera proportionnelle à la hauteur.
        top = topInit.getScaledInstance(-1, heightExtremities, Image.SCALE_SMOOTH);
        tracker.addImage(top, 0);
        bottom = bottomInit.getScaledInstance(-1, heightExtremities, Image.SCALE_SMOOTH);
        tracker.addImage(bottom, 0);
        middle = middleInit.getScaledInstance(-1, heightExtremities, Image.SCALE_SMOOTH);
        tracker.addImage(middle, 0);
        
        // On attend que toutes les images soient chargées.
		try { tracker.waitForAll(); }
		catch (InterruptedException e) {
		    System.out.print(e.toString());
		}
		
		// Une fois que toutes les images ont été chargées, on les enlèves du media tracker.
        tracker.removeImage(top);
        tracker.removeImage(middle);
        tracker.removeImage(bottom);

        return new Dimension(top.getWidth(observer), getHeight());
    }
    
    /**
    * Sets the graphic context of the instance.
    * @param graphicContext the graphic context.
    */
    public void setGraphicContext(GraphicContext graphicContext) {
        super.setGraphicContext(graphicContext);
        // Surcharge de cette méthode pour que l'on puisse récupérer en fonction du contexte graphique
        // du symbole, la hauteur des extrémité de la parenthèse.
        FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(graphicContext.getFont());
        heightExtremities = fm.getHeight() / 2;
    }
}
