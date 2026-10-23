/*
$Id: ModelEvent.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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

import java.util.EventObject;

/**
* The event to send to all objects that implements the interface ModelListener
* and which want to be alerted of changes of the model they are listening to.
*
* @author © 1998 DIRAT Laurent
* @version 1.0  09/07/98
*/
public class ModelEvent extends EventObject {
    /**
    * The action to execute with the event.
    */
    private int action;

    /**
    * The argument of the action to execute.
    */
    private Object argument;

    /**
    * The constructor.
    * @param src the object which produces this event.
    */
    public ModelEvent(Object src) {
        super(src);
    }

    /**
    * Sets the action and its argument, if necessary, to execute to the event.
    * @param action the action to execute.
    * @param argument the action argument. If the action don't need
    * an argument, it must be sets to <CODE>null</CODE>.
    */
    public void setAction(int action, Object argument) {
        this.action = action;
        this.argument = argument;
    }

    /**
    * Returns the action associated with the event.
    */
    public int getAction() {
        return action;
    }

    /**
    * Returns the argument of the action.
    */
    public Object getArgument() {
        return argument;
    }

    /**
    * The different actions associated with the event.<BR>
    */

    /**
    * Add the element which produces the event.
    */
    public static final int ADD                 = 0;

    /**
    * Remove the element which produces the event.
    */
    public static final int REMOVE              = 1;

    /**
    * Remove the element which produces the event.
    */
    public static final int MOVE                = 2;

    /**
    * Create the display for the instance which produces the event.
    */
    public static final int CREATE              = 10;
    
    /*
    * Clear the display
    */
    public static final int CLEAR               = 6;
    
    // Revoir les événements, update devrait devenir un truc du style
    // refresh ou repaint, puisque on l'utilise pour le réaffichage, 
    // et rebuild, plutôt update, puisque c'est pour reconstruire le
    // display, et donc en fait une mise à jour.

    /**
    * Update the element which produces the event.
    */
    public static final int UPDATE              = 3;

    /**
    * Rebuild the element which produces the event.
    */
    public static final int REBUILD             = 7;

    /**
    * Give the OpenMath object of the formula.
    */
    public static final int HERE_IS_OPENMATH    = 4;

    /*
    * Give the maple-like expression of the formula.
    */
    public static final int HERE_IS_MAPLE       = 5;


}
