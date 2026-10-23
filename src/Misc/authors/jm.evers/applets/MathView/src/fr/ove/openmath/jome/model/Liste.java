/*
$Id: Liste.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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


package fr.ove.openmath.jome.model;

import java.util.*;
import fr.ove.openmath.jome.model.*;
import fr.ove.openmath.jome.model.events.ModelEvent;

/**
*
* @author © 1999 DIRAT Laurent
* @version 2.0  28/06/1999
*/
public class Liste extends NaryOperator2 {
    /**
    * Is the "enclosure" of the list visible ? 
    */
    private boolean isVisible = true;
    
    /**
    * Is the list displayed vertically or horizontally ?
    */
    private boolean isVertical = false;
    
    /**
    * The Constructor.
    * 
    */
    public Liste() {
        setTheOperator("{");
        setEnding("}");
    }
    
    /**
    * Returns <CODE>true</CODE> if the curly brackets around the elements have to be displayed.
    * <CODE>false</CODE> otherwise.
    */
    public boolean isVisible() {
        return isVisible;
    }
    
    /**
    * Sets if the curly brackets around the elements have to be displayed.
    * @param isVisible <CODE>true</CODE> if we want it. <CODE>false</CODE> otherwise.
    */
    public void setIsVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }

    /**
    * Returns <CODE>true</CODE> if the list is displayed vertically.
    * <CODE>false</CODE> otherwise.
    */
    public boolean isVertical() {
        return isVertical;
    }
    
    /**
    * Sets if the list is displayed vertically.
    * @param isVertical <CODE>true</CODE> if we want it. <CODE>false</CODE> otherwise.
    */
    public void setIsVertical(boolean isVertical) {
        this.isVertical = isVertical;
    }
}
