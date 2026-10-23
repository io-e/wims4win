/*
$Id: SelectionEvent.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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


package fr.ove.openmath.jome.ctrlview.bidim.selection.events;

import java.util.EventObject;

/**
* The event to send to all objects that implements the interface SlectionEventListener
* and which want to maintain a list of selected elements.
*
* @author © 1998 DIRAT Laurent
* @version 1.0  30/06/98
*/
public class SelectionEvent extends EventObject {
    /**
    * The action to execute by sending the event.
    */
    private int action;

    /**
    * The object which has been selected.
    */
    private Object argument;

    /**
    * The constructor.
    * @param src the object which produces this event.
    */
    public SelectionEvent(Object src) {
        super(src);
    }

    /**
    * Sets the action to execute and its argument.
    * @param action the action.
    * @param argument the argument.<BR>
    * In the case of the actions GET_SELECTION and GET_SELECTION_SIZE,
    * arguments will serve as well as the return value
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
    * Returns the action argument.
    */
    public Object getArgument() {
        return argument;
    }
    
    /**
    * Sets the action argument.
    * @param argument the action argument.
    */
    public void setArgument(Object argument) {
        this.argument = argument;
    }

    /**
    * The different actions associated with the event.<BR>
    */

    /**
    * This action tells the selections manager to remove all the elements it has.
    */
    public static final int PURGE               = 0;

    /**
    * This action tells the selections manager to remove an element. (which will be
    * given to him)
    */
    public static final int REMOVE              = 1;

    /**
    * This action tells the selections manager to add an element. (which will be
    * given to him)
    */
    public static final int ADD                 = 2;

    /**
    * This action tells the selections manager to give the number of selected 
    * elements
    */
    public static final int GET_SELECTION_SIZE  = 3;

    /**
    * This action tells the selections manager to give all the selected elements
    */
    public static final int GET_SELECTION       = 4;

    /**
    * For debugg only. To list all the selected elements.
    */
    public static final int LIST                = 10;
}
