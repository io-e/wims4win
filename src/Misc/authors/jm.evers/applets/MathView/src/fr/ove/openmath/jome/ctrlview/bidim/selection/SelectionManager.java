/*
$Id: SelectionManager.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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


package fr.ove.openmath.jome.ctrlview.bidim.selection;

import java.util.Vector;
import java.util.Enumeration;
import fr.ove.openmath.jome.ctrlview.bidim.Display;
import fr.ove.openmath.jome.ctrlview.bidim.selection.events.SelectionEvent;
import fr.ove.openmath.jome.ctrlview.bidim.selection.events.SelectionEventListener;


/**
* This class manages a list of selected elements.
*
* @author © 1998 DIRAT Laurent
* @version 1.0  30/06/98
*/
public class SelectionManager implements SelectionEventListener {
    /**
    * The list of selected elements.
    */
    private Vector selected;

    /**
    * The constructor.
    */
    public SelectionManager() {
        selected = new Vector();
    }

    /**
    * Consumes (i.e. treats) the event received.
    * @param selectionEvent the event to consume.
    */
    public void consumeSelectionEvent(SelectionEvent selectionEvent) {
        switch (selectionEvent.getAction()) {
            case SelectionEvent.PURGE :
                selected.setSize(0);
                selected.trimToSize();
                break;
            case SelectionEvent.REMOVE :
                selected.removeElement(selectionEvent.getArgument());
                break;
            case SelectionEvent.ADD :
                selected.addElement(selectionEvent.getArgument());
                break;
            case SelectionEvent.GET_SELECTION_SIZE :
                selectionEvent.setArgument(new Integer(selected.size()));
                break;
            case SelectionEvent.GET_SELECTION :
                selectionEvent.setArgument(selected.clone());
                break;
            case SelectionEvent.LIST :
            // For debugg only !!!
                System.out.println(" I've got "+ selected.size() + " selected elements");
                for (Enumeration e = selected.elements(); e.hasMoreElements(); )
                    ((Display) e.nextElement()).whoAmI();
                break;
        }
    }
}
