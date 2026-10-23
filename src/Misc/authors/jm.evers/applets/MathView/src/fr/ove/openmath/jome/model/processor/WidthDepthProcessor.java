/*
$Id: WidthDepthProcessor.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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

import fr.ove.openmath.jome.model.*;
import fr.ove.openmath.jome.model.events.*;
import java.util.*;

/**
* A Width-Depth processor.<BR>
* It is a mix bettween a Width processor (@see WidthProcessor) and a Depth processor
* (@see DepthProcessor).<BR>
* The iconification of the formula operates in 2 phases :
* <OL>
*   <LI>iconification of a certain amount of operands of the operators whose number
*   of operands is greater than a specified value</LI>
*   <LI>iconification of the subtree of the formula tree structure whose depth is greater than
*   than the specified value<LI>
* </OL>
*
* @author © 2000 DIRAT Laurent
* @version 1.0  22/08/2000
*/
public class WidthDepthProcessor extends ProcessorImpl implements WidthProcessorInterface, DepthProcessorInterface {
    /**
    * The depth processor.
    */
    private DepthProcessor depth;
    
    /**
    * The width processor.
    */
    private WidthProcessor width;
    
    
    /**
    * The constructor.
    * @param formula the formula to process.
    */
    public WidthDepthProcessor(Formula formula) {
        super(formula);
        depth = new DepthProcessor(formula);
        width = new WidthProcessor(formula);
    }
    
    /**
    * Does processing.
    */
    public void doProcess() {
        Formula formula = getFormula();
        
        if (formula != null) {
            width.setProcessingId(getProcessingId());
            width.doProcess();
            depth.setProcessingId(getProcessingId());
            depth.doProcess();
            
            /*
            switch (getProcessingId()) {
                case COMPUTE :
                    width.setProcessingId(COMPUTE);
                    width.doProcess();
                    depth.setProcessingId(COMPUTE);
                    depth.doProcess();
                    break;
                case SIMPLE_ICONIFICATION :
                    width.setProcessingId(SIMPLE_ICONIFICATION);
                    width.doProcess();
                    depth.setProcessingId(SIMPLE_ICONIFICATION);
                    depth.doProcess();
                    break;
                case RECURSIVE_ICONIFICATION :
                    width.setProcessingId(RECURSIVE_ICONIFICATION);
                    width.doProcess();
                    depth.setProcessingId(RECURSIVE_ICONIFICATION);
                    depth.doProcess();
                    break;
                case COMPUTE_AND_ICONIFY :
                    width.setProcessingId(COMPUTE_AND_ICONIFY);
                    width.doProcess();
                    depth.setProcessingId(COMPUTE_AND_ICONIFY);
                    depth.doProcess();
            }
            */
        }
    }
    
    /**
    * Sets the level from which the processing starts.<BR>
    * The level correspond for both width and depth processor.
    * @param level the level.
    */
    public void setLevel(int level) {
        width.setLevel(level);
        depth.setLevel(level);
    }
    
    // #########################################
    // ### Interface DepthProcessorInterface ###
    // #########################################
    
    /**
    * Sets the depth level form which the processing will be considering.<BR>
    * For convenience only, in most cases this method is equivalent to the
    * @see setLevel of the @see Processor interface.
    * @param depthLevel the depth level.
    */
    public void setDepthLevel(int depthLevel) {
        depth.setLevel(depthLevel);
    }
    
    /**
    * Returns the depth level form which the processing will be considering.<BR>
    * For convenience only, in most cases this method is equivalent to the
    * @see getLevel of the @see Processor interface.
    */
    public int getDepthLevel() {
        return depth.getLevel();
    }
    
    /**
    * Returns the max depth value calculated.
    */
    public int getMaxDepthValue() {
        return depth.getMaxDepthValue();
    }
    
    // #########################################
    // ### Interface WidthProcessorInterface ###
    // #########################################
    
    /**
    * Sets the width level form which the processing will be considering.<BR>
    * For convenience only, in most cases this method is equivalent to the
    * @see setLevel of the @see Processor interface.
    * @param widthLevel the width level.
    */
    public void setWidthLevel(int widthLevel) {
        width.setLevel(widthLevel);
    }
    
    /**
    * Returns the width level form which the processing will be considering.<BR>
    * For convenience only, in most cases this method is equivalent to the
    * @see getLevel of the @see Processor interface.
    */
    public int getWidthLevel() {
        return width.getLevel();
    }
    
    /**
    * Sets the number of children viewed.
    * @param the number of children viewed.
    */
    public void setView(int view) {
        width.setView(view);
    }
    
    /**
    * Returns the number of children viewed.
    */
    public int getView() {
        return width.getView();
    }
    
    /**
    * Returns the list of widths calculated.
    */
    public Vector getWidthList() {
        return width.getWidthList();
    }
    
    /**
    * Returns the biggest width calculated.
    */
    public int getBiggestWidth() {
        return width.getBiggestWidth();
    }
}
