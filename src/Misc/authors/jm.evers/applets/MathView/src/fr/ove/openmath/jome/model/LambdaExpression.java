/*
$Id: LambdaExpression.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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


package fr.ove.openmath.jome.model;

import java.util.*;
import fr.ove.openmath.jome.model.*;
import fr.ove.openmath.jome.model.events.ModelEvent;

/**
* The lambda expression object.<BR>
* The first child corresponds to the function. The next child(ren) to the bound variable(s).
*
* @author © 1999 DIRAT Laurent
* @version 2.0  11/10/1999
*/
public class LambdaExpression extends Function /*NaryOperator2*/ {
    private boolean gotoVariable = true;
    
    /**
    * Indicator for the rendrering.<BR>
    * Is only function displayed ?<BR>
    * In other words, if we got the lambda expression coresponding to lambda(sin(x)+cos(y), x, y), if 
    * its values equals <CODE>true</CODE>, the rendering will be something like (x, y) -> sin(x)+cos(y).
    * Otherwise, the rendering will simply be sin(x)+cos(y). (we just function is rendered).<BR>
    */
    private boolean justFunction = false;
    
    /**
    * Returns <CODE>true</CODE> if only the is displayed.
    * <CODE>false</CODE> otherwise.
    */
    public boolean isJustFunction() {
        return justFunction;
    }
    
    /**
    * Sets if just the function is rendered or not.
    * @param justFunction <CODE>true</CODE> if we only want the function to be displayed. <CODE>false</CODE> otherwise.
    */
    public void setJustFunction(boolean justFunction) {
        this.justFunction = justFunction;
    }

    /** 
    * Inserts the instance in the formula tree structure.<BR>
    * @param current the position in the formula tree where the operator is to be insert.
    * @return the new current position int hte formula tree.
    */
    public FormulaTreeStructure insert(FormulaTreeStructure current) {
        current = super.insert(current);
        super.addElement();
        return current;
    }
    
    /**
    * Adds a new element (template) to the end of the list.
    * Returns the new element.
    */
    public FormulaTreeStructure addElement() {
        if (gotoVariable) {
            gotoVariable = false;
            return (FormulaTreeStructure) getChild(1).getChild(0);
        }
        else
            return super.addElement();
    }
}
