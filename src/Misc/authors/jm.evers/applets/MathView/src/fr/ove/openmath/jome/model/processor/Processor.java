/*
$Id: Processor.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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

import fr.ove.openmath.jome.model.Formula;

/**
* The interface to implement if a class wants to be considered
* as a processor of the model of the formula (@see fr.ove.openmath.jome.model.Formula)
*
* @author © 2000 DIRAT Laurent
* @version 1.0  16/08/2000
*/
public interface Processor {
    /**
    * Does processing.
    */
    public void doProcess();
    
    /**
    * Sets the formula to processs
    */
    public void setFormula(Formula formula);
    
    /**
    * Returns the formula to process.
    */
    public Formula getFormula();
    
    /**
    * Specifies if, after processing the formula, the display has to be
    * updated.
    * @param updateDisplay <CODE>true</CODE> if the display needs update.
    * <CODE>false</CODE> otherwise.
    */
    public void setUpdateDisplay(boolean updateDisplay);
    
    /**
    * Checks if, after processing the formula, the display has to be
    * updated.
    * @returns <CODE>true</CODE> if the display needs update.
    * <CODE>false</CODE> otherwise.
    */
    public boolean getUpdateDisplay();
    
    /**
    * Initialisation of the processor.
    */
    public void init();
    
    /**
    * Sets the processing to execute.
    * @param processingId the processing Id.
    */
    public void setProcessingId(int processingId);
    
    /**
    * Returns the current processing to execute.
    */
    public int getProcessingId();
    
    /**
    * Sets the level from which the processing starts.
    * @param level the level.
    */
    public void setLevel(int level);
    
    /**
    * Returns the level from which the processing starts.
    */
    public int getLevel();
}