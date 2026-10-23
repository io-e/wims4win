/*
$Id: Sizable.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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

import java.awt.Dimension;

/**
* Interface that must implement all objects that want to be sized.
*
* @author © 1999 DIRAT Laurent
* @version 2.0 29/06/1999
*/
public interface Sizable {
    /**
    * Returns the width of the instance.
    */
    public int getWidth();
    
    /**
    * Sets the width of the instance.
    * @param width the width of the instance
    */
    public void setWidth(int width);
    
    /**
    * Returns the height of the instance.
    */
    public int getHeight();
    
    /**
    * Sets the height of the instance.
    * @param height the height of the instance
    */
    public void setHeight(int height);
    
    /**
    * Returns the size of the instance.
    */
    public Dimension getSize();
    
    /**
    * Sets the size of the instance.
    * @param width the width of the instance.
    * @param height the height of the instance
    */
    public void setSize(int width, int height);
    
    /**
    * Sets the size of the instance.
    * @param size the size of the instance.
    */
    public void setSize(Dimension size);
    
    /**
    * Returns the preferred size of the instance.
    */
    public Dimension getPreferredSize();
}