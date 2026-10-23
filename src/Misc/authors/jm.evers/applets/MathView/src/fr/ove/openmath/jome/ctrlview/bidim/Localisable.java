/*
$Id: Localisable.java,v 1.3 2003/02/18 11:48:46 sander Exp $
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

import java.awt.Point;

/**
* Interface that must implement all objects that want to be located.
*
* @author © 1999 DIRAT Laurent
* @version 2.0 29/06/1999
*/
public interface Localisable {
    /**
    * Sets the x-location of the instance.
    * @param x the x-location of the instance.
    */
    public void setX(int x);
    
    /**
    * Returns the x-location of the instance.
    */
    public int getX();
    
    /**
    * Sets the y-location of the instance.
    * @param y the y-location of the instance.
    */
    public void setY(int y);
    
    /**
    * Returns the y-location of the instance.
    */
    public int getY();
    
    /**
    * Sets the location of the instance.
    * @param x the x-location of the instance.
    * @param y the y-location of the instance.
    */
    public void setLocation(int x, int y);
    
    /**
    * Sets the location of the instance.
    * @param location the location of the instance.
    */
    public void setLocation(Point location);
    
    /**
    * Returns the location of the instance.
    * @return A <CODE>Point</CODE> representing the location of the instance.
    */
    public Point getLocation();
}
