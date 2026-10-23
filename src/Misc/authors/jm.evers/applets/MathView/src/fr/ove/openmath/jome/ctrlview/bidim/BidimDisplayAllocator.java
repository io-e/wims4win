/*
$Id: BidimDisplayAllocator.java,v 1.3 2003/02/18 11:48:46 sander Exp $
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
import fr.ove.openmath.jome.ctrlview.bidim.*;
import fr.ove.openmath.jome.model.*;

/**
* This class instanciates the right display according to the specified formula
* tree sturcture.
* This is an abstract class. It must be inherited to be specialized.
*
* @author Â© 1999 DIRAT Laurent
* @version 2.0  01/07/1999
*/

public class BidimDisplayAllocator extends DisplayAllocator {
    /**
    * The display layout allocator.
    */
    private static BidimLayoutAllocator layoutAllocator = new BidimLayoutAllocator();
    
    /**
    * The resources manager for the displays
    */
    private static BidimResourcesManager resourcesManager = new BidimResourcesManager("fr.ove.openmath.jome.ctrlview.bidim.resources");
    
    /**
    * Instanciates and creates the right display according to the specified
    * formula tree structure.
    * @param fts the formula tree structure which need a display.
    */
    public Display allocateDisplay(GraphicContext graphicContext, FormulaTreeStructure fts) {
        Display display = null;
        
        String ftsId = fts.getResourceIdentifier();
        String idDisplay = resourcesManager.getIdDisplay(ftsId);
        
        // Si il y a un problÃ¨me de display, alors on met le display par dÃ©faut
        idDisplay = (idDisplay == null) ? "default" : idDisplay;
        
        //System.out.println("ftsId = " + ftsId + "\t\t idDisplay = " + idDisplay);
        
        if (idDisplay.equals("constant")) {
            // Allocation d'un StringDisplay
            display = new StringDisplay(graphicContext, ((Constant) fts).getValue(), false);
            
            // Le display crÃ©Ã© Ã©coute la fts qui lui correspond
            display.addControlListener(fts);
            // Et vice et versa.
            fts.addModelListener(display);
        }
        else if (idDisplay.equals("symbol")) {
            // Allocation d'un SymbolDisplay
            display = new SymbolDisplay(graphicContext);
            // Allocation dudit symbole
            ImageSymbol symbol = new ImageSymbol(/*fts.getSymbolName*/fts.getValue(), display);
            // Affection du symbole
            ((SymbolDisplay) display).setSymbol(symbol);
            
            // Le display crÃ©Ã© Ã©coute la fts qui lui correspond
            display.addControlListener(fts);
            // Et vice et versa.
            fts.addModelListener(display);
        }
        else if (idDisplay.equals("icon")) {
            if (((Icon) fts).isSubstitution()) {
                // Allocation d'un SubstitutionDisplay
                display = new SubstitutionDisplay(graphicContext, ((Icon) fts).getIconName());
            }
            else {
                // Allocation d'un SymbolDisplay
                display = new IconDisplay(graphicContext);
                // Allocation de l'icone
                ImageSymbol icon = new ImageSymbol(fts.getIconName(), display);
                // Affection du symbole (i.e. de l'icone)
                ((SymbolDisplay) display).setSymbol(icon);
            }
            
            // Le display crÃ©Ã© Ã©coute la fts qui lui correspond
            display.addControlListener(fts);
            // Et vice et versa.
            fts.addModelListener(display);
        }
        else {
            // Dans tous les autres cas, allocation d'un BidimDisplay
            display = new BidimDisplay(graphicContext);
            
            // Le display crÃ©Ã© Ã©coute la fts qui lui correspond
            display.addControlListener(fts);
            // Et vice et versa.
            fts.addModelListener(display);
            
            // Allocation du layout manager qui va bien
            String layoutName = resourcesManager.getLayoutManager(ftsId);
            layoutName = (layoutName == null) ? resourcesManager.getLayoutManager("defaultLM") : layoutName;
            
            //System.out.println("layoutName = " + layoutName);
            
            DisplayLayout layout = layoutAllocator.allocateLayout(layoutName);
            display.setLayout(layout);
            layout.initDisplay(display);
        }
        
        return display;
    }
}
