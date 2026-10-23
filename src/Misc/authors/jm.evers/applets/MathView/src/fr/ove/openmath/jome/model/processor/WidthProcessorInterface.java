/*
$Id: WidthProcessorInterface.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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


package fr.ove.openmath.jome.model.processor;

import java.util.Vector;

/**
* The interface to implement to be a width processor.<BR>
* A width processor manages to display a certain amount of children (the view)
* of an element whose number of children (the width) is greater than a specified level.
*
* @author © 2000 DIRAT Laurent
* @version 1.0  21/08/2000
*/
public interface WidthProcessorInterface extends Processor {
    /**
    * Sets the width level form which the processing will be considering.<BR>
    * For convenience only, in most cases this method is equivalent to the
    * @see setLevel of the @see Processor interface.
    * @param widthLevel the width level.
    */
    public void setWidthLevel(int widthLevel);
    
    /**
    * Returns the width level form which the processing will be considering.<BR>
    * For convenience only, in most cases this method is equivalent to the
    * @see getLevel of the @see Processor interface.
    */
    public int getWidthLevel();
    
    /**
    * Sets the number of children viewed.
    * @param the number of children viewed.
    */
    public void setView(int view);
    
    /**
    * Returns the number of children viewed.
    */
    public int getView();
    
    /**
    * Returns the list of widths calculated.
    */
    public Vector getWidthList();
    
    /**
    * Returns the biggest width calculated.
    */
    public int getBiggestWidth();
}
