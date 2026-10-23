/*
$Id: UnderscriptLayout.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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
import fr.ove.openmath.jome.ctrlview.bidim.Display;
import fr.ove.openmath.jome.ctrlview.bidim.DisplayLayout;
import fr.ove.openmath.jome.ctrlview.bidim.HorizontalLayout;
import fr.ove.openmath.jome.ctrlview.bidim.selection.events.SelectionEvent;
import fr.ove.openmath.jome.model.*;

/**
* A layout manager that lays components horyzontally, but the last one in the
* list, is treated as a underscript (there is an downward shitf).
*
* @author © 1999 DIRAT Laurent
* @version 2.0  13/12/1999
*/
public class UnderscriptLayout extends ScriptLayout {
    /**
    * Computes the size of the display according to its children size (if any),
    * and its different attributes.
    * @return the size of the display.
    */
    public Dimension computeAttributes() {
        int ascent = 0;
        int descent = 0;
        int width = 0;
        int height = 0;
        
        Display base, indice;
        int decalage = 0;
        
        base = (Display) displayToLay.getComponent(0);
        base.setSize(base.getPreferredSize());
        width += base.getWidth() + base.getShiftX();
        
        indice = (Display) displayToLay.getComponent(1);

        if (base.getLayout() instanceof UnderscriptLayout)
            ((DisplayLayout) indice.getLayout()).updateLevel(((Display) base.getComponent(1)).getLevel()+ 1);
        else
            ((DisplayLayout) indice.getLayout()).updateLevel(base.getLevel()+ 1);
        
        indice.setSize(indice.getPreferredSize());
        
        if (base.getLayout() instanceof UnderscriptLayout) {
            Display ind_base = (Display) base.getComponent(1);
            decalage = ind_base.getDescent() + ind_base.getShiftY();
        }
        else
            decalage = base.getDescent();

        /* ############################# */
        base.setShiftY(0); // ATTENTION : rajout !!!!!!
        /* ############################# */
        
        indice.setShiftY(decalage);
        width += indice.getWidth() + indice.getShiftX();
        
        displayToLay.setAscent(base.getAscent()); 
        displayToLay.setDescent(decalage + indice.getDescent());
        
        height += displayToLay.getAscent() + displayToLay.getDescent();
        displayToLay.setSize(width , height);
        
        displayToLay.setComputeAttributes(false);
        return new Dimension(width, height);
    }
}
