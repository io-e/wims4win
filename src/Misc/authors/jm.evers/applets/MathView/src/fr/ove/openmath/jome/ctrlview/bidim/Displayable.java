/*
$Id: Displayable.java,v 1.3 2003/02/18 11:48:46 sander Exp $
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
import fr.ove.openmath.jome.ctrlview.bidim.GraphicContext;
import fr.ove.openmath.jome.ctrlview.bidim.Localisable;
import fr.ove.openmath.jome.ctrlview.bidim.Sizable;

/**
* An implementation of the <CODE>Displayable</CODE> interface.<BR>
*
* @author © 1999 DIRAT Laurent
* @version 2.0 29/06/1999
*/
public interface Displayable extends Localisable, Sizable {
    /**
    * Sets the ascent.
    * @param ascent the new ascent value.
    */
    public void setAscent(int ascent);
    
    /**
    * Returns the ascent.
    * @return the ascent.
    */
    public int getAscent();
    
    /**
    * Sets the descent.
    * @param descent the new descent value.
    */
    public void setDescent(int descent);
    
    /**
    * Returns the descent.
    * @return the descent.
    */
    public int getDescent();
    
    /**
    * Sets the horizontal shift.
    * @param shift the new horizontal shift value.
    */
    public void setShiftX(int shiftX);
    
    /**
    * Returns the horizontal shift.
    * @return the horizontal shift.
    */
    public int getShiftX();
    
    /**
    * Sets the vertical shift.
    * @param shiftY the new vertical shift value.
    */
    public void setShiftY(int shiftY);
    
    /**
    * Returns the vertical shift.
    * @return the vertical shift.
    */
    public int getShiftY();
    
    /**
    * Sets all the attributes.
    * @param ascent the new ascent value.
    * @param descent the new descent value.
    * @param shiftX the new horizontal shift value.
    * @param shiftY the new vertical shift value.
    */
    public void setAttributes(int ascent, int descent, int shiftX, int shiftY);
        
    /**
    * Sets the graphic context of the instance.
    * @param graphicContext the graphic context.
    */
    public void setGraphicContext(GraphicContext graphicContext);
    
    /**
    * Returns the graphic context of the instance.
    */
    public GraphicContext getGraphicContext();
    
    /**
    * The paint method of the object to display.
    * @param g the drawing area of the symbol.
    */
    public void paint(Graphics g);
}
