/*
$Id: DepthProcessorInterface.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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

/**
* The interface to implement to be depth processor.
*
* @author © 2000 DIRAT Laurent
* @version 1.0  21/08/2000
*/
public interface DepthProcessorInterface extends Processor {
    /**
    * Sets the depth level form which the processing will be considering.<BR>
    * For convenience only, in most cases this method is equivalent to the
    * @see setLevel of the @see Processor interface.
    * @param depthLevel the depth level.
    */
    public void setDepthLevel(int depthLevel);
    
    /**
    * Returns the depth level form which the processing will be considering.<BR>
    * For convenience only, in most cases this method is equivalent to the
    * @see getLevel of the @see Processor interface.
    */
    public int getDepthLevel();
    
    /**
    * Returns the max depth value calculated.
    */
    public int getMaxDepthValue();
}
