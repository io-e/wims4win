/*
$Id: ModelListenerController.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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


package fr.ove.openmath.jome.model.events;

import java.util.EventListener;
import fr.ove.openmath.jome.model.events.ModelEvent;
import fr.ove.openmath.jome.ctrlview.events.*;

/**
* The interface that an object which want to be alerted of the changes of the model
* and want to control at the same time, must implement.
*
* @author © 1998 DIRAT Laurent
* @version 1.0  29/07/98
*/
public interface ModelListenerController extends ModelListener {
    /**
    * Registers another listener to be informed of some changes.
    * @param controlListener a listener to add.
    */
    public void addControlListener(ControlListener controlListener);
    
    /**
    * Removes a listener.
    * @param controlListener a listener to remove.
    */
    public void removeControlListener(ControlListener controlListener);
    
    /**
    * Fires a Control event to registered listeners.
    * @param controlEvent event encapsulating relevant information.
    */
    public void fireControlEvent(ControlEvent controlEvent);
}
