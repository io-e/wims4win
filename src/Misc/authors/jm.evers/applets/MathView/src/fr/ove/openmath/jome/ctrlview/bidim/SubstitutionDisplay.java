/*
$Id: SubstitutionDisplay.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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
import java.awt.event.*;
import java.text.*;
import fr.ove.utils.NumberUtils;
import fr.ove.openmath.jome.ctrlview.bidim.StringLayout;
import fr.ove.openmath.jome.ctrlview.bidim.Display;
import fr.ove.openmath.jome.ctrlview.bidim.GraphicContext;
import fr.ove.openmath.jome.model.*;
import fr.ove.openmath.jome.model.events.ModelEvent;

/**
* The display for subsitution of parts of a expression with frienfly name.
*
* @author © 1999 DIRAT Laurent
* @version 2.0  04/08/1999
*/
public class SubstitutionDisplay extends AbstractStringDisplay {
    /**
    * The constructor.
    * @param graphicContext the graphic context of the display.
    * @param string the string to display
    */
    public SubstitutionDisplay(GraphicContext graphicContext, String string) {
        super(graphicContext, string);
        Font font = getFont();
        font = new Font(font.getName(), Font.BOLD, font.getSize());
        setFont(font);
        
        SubstitutionLayout layout = new SubstitutionLayout();
        layout.initDisplay(this);
        setLayout(layout);
    }
    
    /**
    * Consumes (i.e. treats) the event received from the model.
    * @param modelEvent the event to consume.
    */
    public void consumeModelEvent(ModelEvent modelEvent) {
        // En principe, c'est le seul événement que doit recevoir ce type de display
        // De plus, forcément, le display représenté n'est pas le display d'un opérateur.
        switch (modelEvent.getAction()) {
            case ModelEvent.UPDATE :
                //System.out.println("ModelEvent.UPDATE : on update le StringDisplay");
                Icon src = (Icon) modelEvent.getSource();
                
                // On met à jour le display.
                setString(src.getIconName());

                computeAncestorsAttributes();
                invalidate();
        }
    }
}