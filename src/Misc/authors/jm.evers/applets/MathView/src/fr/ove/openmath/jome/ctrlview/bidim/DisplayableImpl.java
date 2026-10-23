/*
$Id: DisplayableImpl.java,v 1.3 2003/02/18 11:48:46 sander Exp $
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
import java.io.Serializable;
import fr.ove.openmath.jome.ctrlview.bidim.GraphicContext;
import fr.ove.openmath.jome.ctrlview.bidim.Sizable;
import fr.ove.openmath.jome.ctrlview.bidim.Localisable;

/**
* An implementation of the <CODE>Displayable</CODE> interface.<BR>
* Nevertheless, it is an abstract class, because we don't know a priori
* how to paint it. This will be one of the tasks of the inherited classes.
*
* @author © 1999 DIRAT Laurent
* @version 2.0 29/06/1999
*/
public abstract class DisplayableImpl implements Displayable, Serializable {
    /**
    * The bounds of the instance. We keep location and size information.
    */
    private Rectangle bounds;
    
    /**
    * Distance between the top of the bounding box of the element to display
    * and its baseline.
    */
    private int ascent;

    /**
    * Distance between the bottom of the bounding box of the element to display
    * and its baseline.
    */
    private int descent;

    /**
    * An horizontal shift.
    */
    private int shiftX;

    /**
    * A vertical shift.
    */
    private int shiftY;
    
    /**
    * The graphic context of the displayable object.
    */
    private GraphicContext graphicContext;
    
    /**
    * The default constructor.<BR>
    * By default, all the parameters equal to 0.
    */
    public DisplayableImpl() {
        this(new Rectangle(), 0, 0, 0, 0);
    }
    
    /**
    * The constructor.
    * @param bounds the bounds of the instance.
    * @param ascent the ascent of the instance.
    * @param descent the descent of the instance.
    * @param shiftX the horizontal shift of the instance.
    * @param shiftY the vertical shift of the instance.
    */
    public DisplayableImpl(Rectangle bounds, int ascent, int descent, int shiftX, int shiftY) {
        this.bounds = bounds;
        this.ascent = ascent;
        this.descent = descent;
        this.shiftX = shiftX;
        this.shiftY = shiftY;
    }
    
    
    // #################################################
    // ### Implémentation des différentes interfaces ###
    // #################################################
    
    // *************************************************
    // *** Implémentation de l'interface Displayable ***
    
    /**
    * Sets the ascent.
    * @param ascent the new ascent value.
    */
    public void setAscent(int ascent) {
        this.ascent = ascent;
    }
    
    /**
    * Returns the ascent.
    * @return the ascent.
    */
    public int getAscent() {
        return ascent;
    }
    
    /**
    * Sets the descent.
    * @param descent the new descent value.
    */
    public void setDescent(int descent) {
        this.descent = descent;
    }
    
    /**
    * Returns the descent.
    * @return the descent.
    */
    public int getDescent() {
        return descent;
    }
    
    /**
    * Sets the horizontal shift.
    * @param shiftX the new horizontal shift value.
    */
    public void setShiftX(int shiftX) {
        this.shiftX = shiftX;
    }
    
    /**
    * Returns the horizontal shift.
    * @return the horizontal shift.
    */
    public int getShiftX() {
        return shiftX;
    }
    
    /**
    * Sets the vertical shift.
    * @param shiftY the new vertical shift value.
    */
    public void setShiftY(int shiftY) {
        this.shiftY = shiftY;
    }
    
    /**
    * Returns the vertical shift.
    * @return the vertical shift.
    */
    public int getShiftY() {
        return shiftY;
    }
    
    /**
    * Sets all the attributes.
    * @param ascent the new ascent value.
    * @param descent the new descent value.
    * @param shiftX the new horizontal shift value.
    * @param shiftY the new vertical shift value.
    */
    public void setAttributes(int ascent, int descent, int shiftX, int shiftY) {
        this.ascent = ascent;
        this.descent = descent;
        this.shiftX = shiftX;
        this.shiftY = shiftY;
    }
    
    /**
    * Sets the graphic context of the instance.
    * @param graphicContext the graphic context.
    */
    public void setGraphicContext(GraphicContext graphicContext) {
        // contrairement à la création d'un display, il n'y a pas allocation mais maintient d'une référence
        // sur graphicContext, qui est en fait le contexte graphique du display (dans notre cas précis) qui
        // va afficher l'objet.
        this.graphicContext = graphicContext;
    }
    
    /**
    * Returns the graphic context of the instance.
    */
    public GraphicContext getGraphicContext() {
        return graphicContext;
    }
    
    // *** Fin de l'interface Displayable ***
    // **************************************
    
    // *************************************************
    // *** Implémentation de l'interface Localisable ***
    
    /**
    * Sets the x-location of the instance.
    * @param x the x-location of the instance.
    */
    public void setX(int x) {
        bounds.x = x;
    }
    
    /**
    * Returns the x-location of the instance.
    */
    public int getX() {
        return bounds.x;
    }
    
    /**
    * Sets the y-location of the instance.
    * @param y the y-location of the instance.
    */
    public void setY(int y) {
        bounds.y = y;
    }
    
    /**
    * Returns the y-location of the instance.
    */
    public int getY() {
        return bounds.y;
    }
    
    /**
    * Sets the location of the instance.
    * @param x the x-location of the instance.
    * @param y the y-location of the instance.
    */
    public void setLocation(int x, int y) {
        bounds.setLocation(x, y);
    }
    
    /**
    * Sets the location of the instance.
    * @param location the location of the instance.
    */
    public void setLocation(Point location) {
        bounds.setLocation(location);
    }
    
    /**
    * Returns the location of the instance.
    * @return A <CODE>Point</CODE> representing the location of the instance.
    */
    public Point getLocation() {
        return bounds.getLocation();
    }
    
    // *** Fin de l'interface Localisable ***
    // **************************************
    
    // *********************************************
    // *** Implémentation de l'interface Sizable ***
    
    /**
    * Returns the width of the instance.
    */
    public int getWidth() {
        return bounds.width;
    }
    
    /**
    * Sets the width of the instance.
    * @param width the width of the instance
    */
    public void setWidth(int width) {
        bounds.width = width;
    }
    
    /**
    * Returns the height of the instance.
    */
    public int getHeight() {
        return bounds.height;
    }
    
    /**
    * Sets the height of the instance.
    * @param height the height of the instance
    */
    public void setHeight(int height) {
        bounds.height = height;
    }
    
    /**
    * Returns the size of the instance.
    */
    public Dimension getSize() {
        return bounds.getSize();
    }
    
    /**
    * Sets the size of the instance.
    * @param width the width of the instance.
    * @param height the height of the instance
    */
    public void setSize(int width, int height) {
        bounds.setSize(width, height);
    }
    
    /**
    * Sets the size of the instance.
    * @param size the size of the instance.
    */
    public void setSize(Dimension size) {
        bounds.setSize(size);
    }
    
    // Le comportement par défaut est que la taille de bounds est la taille préférée.
    /**
    * Returns the preferred size of the instance.
    */
    public Dimension getPreferredSize() {
        return bounds.getSize();
    }
    
    // *** Fin de l'interface sizable ***
    // **********************************
}
