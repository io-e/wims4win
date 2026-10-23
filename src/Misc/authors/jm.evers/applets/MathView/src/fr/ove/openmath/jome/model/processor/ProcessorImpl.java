/*
$Id: ProcessorImpl.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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
* An implementation of a @see Processor.<BR>
* The method <CODE>doProcess</CODE> does nothing. Subclasses have to, 
* at least, override it to provide the desired processing.
*
* @author © 2000 DIRAT Laurent
* @version 1.0  17/08/2000
*/
public class ProcessorImpl implements Processor {
    /**
    * The formula to process.
    */
    private Formula formula;
    
    /**
    * To check if we have to update the display of the formula.<BR>
    * The default is false.
    */
    private boolean updateDisplay = false;
    
    /**
    * The processing to execute.<BR>
    * The default corresponds to <CODE>ProcessorImpl.COMPUTE_AND_ICONIFY</CODE>.
    */
    private int processingId = COMPUTE_AND_ICONIFY;
    
    /**
    * The level form which the processing starts.
    */
    private int level = 0;
    
    /**
    * The constructor.
    * @param formula the formula to process.
    */
    public ProcessorImpl(Formula formula) {
        this.formula = formula;
    }
    
    /**
    * Does processing.<BR>
    * !! Method to override by subclasses !!
    */
    public void doProcess() {}
    
    /**
    * Sets the formula to processs.
    */
    public void setFormula(Formula formula) {
        this.formula = formula;
    }
    
    /**
    * Returns the formula to process.
    */
    public Formula getFormula() {
        return formula;
    }
    
    /**
    * Specifies if, after processing the formula, the display has to be
    * updated.
    * @param updateDisplay <CODE>true</CODE> if the display needs update.
    * <CODE>false</CODE> otherwise.
    */
    public void setUpdateDisplay(boolean updateDisplay) {
        this.updateDisplay = updateDisplay;
    }
    
    /**
    * Checks if, after processing the formula, the display has to be
    * updated.
    * @returns <CODE>true</CODE> if the display needs update.
    * <CODE>false</CODE> otherwise.
    */
    public boolean getUpdateDisplay() {
        return updateDisplay;
    }
    
    /**
    * Initialisation of the processor.
    * !! Method to override by subclasses !!
    * The default is just setting the processing Id to be COMPUTE_AND_ICONIFY;
    */
    public void init() {
        setProcessingId(COMPUTE_AND_ICONIFY);
    }
    
    /**
    * Sets the processing to execute.
    * @param processingId the processing Id.
    */
    public void setProcessingId(int processingId) {
        this.processingId = processingId;
    }
    
    /**
    * Returns the current processing to execute.
    */
    public int getProcessingId() {
        return processingId;
    }
    
    /**
    * Sets the level from which the processing starts.
    * @param level the level.
    */
    public void setLevel(int level) {
        this.level = level;
    }
    
    /**
    * Returns the level from which the processing starts.
    */
    public int getLevel() {
        return level;
    }
    
    
    
    /**
    * The different processing to apply to the formula
    */
    
    /**
    * Compute the the necessaru stuff for the iconifcation of the formula.
    */
    public static final int COMPUTE                    = 1;
    
    /**
    * Iconify the formula according to the iconification level fixed.<BR>
    * We assume the necessary computation for the iconification of the formula was previously done.<BR>
    * This is a simple iconification process. Subtrees of iconfied ones are not iconified.
    */
    public static final int SIMPLE_ICONIFICATION        = 2;
    
    /**
    * Iconify the formula according to the iconification level fixed.<BR>
    * We assume the necessary computation for the iconification of the formula was previously done.<BR>
    * This is a recursive iconification process. Subtrees of iconfied ones are also iconified.
    */
    public static final int RECURSIVE_ICONIFICATION     = 3;
    
    /**
    * Compute and iconify (recursivly) the formula according to the iconification level fixed.<BR>
    * This is the default behaviour.
    */
    public static final int COMPUTE_AND_ICONIFY         = 4;
}