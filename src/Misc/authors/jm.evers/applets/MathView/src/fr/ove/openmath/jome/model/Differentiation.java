/*
$Id: Differentiation.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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
* The differentiation operator.<BR>
* The first child corresponds to the function. The next child(ren) to the bound variable(s).
*
* @author © 2000 DIRAT Laurent
* @version 2.1 10/01/2000
*/
public class Differentiation extends Function {
    private boolean gotoVariable = true;
    
    /**
    * <CODE>true</CODE> if the instance represents a partial differentiation.
    * <CODE>false</CODE> otherwise.
    */
    private boolean isPartial = false;
    
    /**
    * The order of the differentiation
    */
    private int order = 1;
    
    /**
    * Returns <CODE>true</CODE> if the instance represents a partial differentiation.
    * <CODE>false</CODE> otherwise.
    */
    public boolean isPartial() {
        return isPartial;
    }
    
    /**
    * Sets if the instance repressents a partial differentiation
    * @param isPartial <CODE>true</CODE> if the instance represents a partial differentiation.
    * <CODE>false</CODE> otherwise.
    */
    public void setIsPartial(boolean isPartial) {
        this.isPartial = isPartial;
    }

    /** 
    * Inserts the instance in the formula tree structure.<BR>
    * @param current the position in the formula tree where the operator is to be insert.
    * @return the new current position int hte formula tree.
    */
    public FormulaTreeStructure insert(FormulaTreeStructure current) {
        current = super.insert(current);
        
        if (isPartial) {
            Constant delta = new Constant();
            delta.setResourceIdentifier("delta");
            delta.setValue("delta");
            current = delta.insert(current);        
        }
        else
            current = (new Variable("d")).insert(current);
        
        current = super.addElement();
        super.addElement();
        
        return current;
    }
    
    /**
    * Computes the order of the differentiation.
    */
    public void computeOrder() {
        Hashtable ht = new Hashtable();
        FormulaTreeStructure fts, degree;
        String varName;
        
        for (int i = 2; i < getNbChildren(); i++) {
            fts = (FormulaTreeStructure) getChild(i).getChild(0);
            if (!fts.isOperator()) {
                // donc on dit que c'est une variable
                varName = ((VariableOrNumber) fts).getValue();
                degree = (FormulaTreeStructure) ht.get(varName);
                if (degree == null) {
                    degree = new Addition();
                    degree.addChild(new AnInteger("1"));
                    ht.put(varName, degree);
                }
                else
                    degree.addChild(new AnInteger("1"));
            }
            else {
                // donc on dit que c'est une ^
                varName = ((VariableOrNumber) fts.getChild(0)).getValue();
                degree = (FormulaTreeStructure) ht.get(varName);
                if (degree == null) {
                    degree = new Addition();
                    degree.addChild((FormulaTreeStructure) fts.getChild(1).clone());
                    ht.put(varName, degree);
                }
                else 
                    degree.addChild((FormulaTreeStructure) fts.getChild(1).clone());
            }
        }
        
        
        if (ht.size() > 1) {
            isPartial = true;
            fts = (FormulaTreeStructure) getChild(0);
            fts.removeChild(0);
            
            Constant delta = new Constant();
            delta.setResourceIdentifier("delta");
            delta.setValue("delta");
            fts.addChild(delta);
        }
        
        fts = new Addition();
        for (Enumeration e = ht.elements(); e.hasMoreElements(); )
            fts.addChild((FormulaTreeStructure) e.nextElement());
            
        String order_s = fts.evaluate();
        
        try {
            order = Integer.parseInt(order_s);
        }
        catch(NumberFormatException nfe) {
            order = 0;
        }
        
        if (!order_s.equals("1")) {
            fts = (FormulaTreeStructure) getChild(0).getChild(0);
            fts = (new Superscript()).insert(fts);
            fts = (new Variable(order_s)).insert(fts);
        }
    }
    
    /**
    * Returns the order of the differentiation.
    * @return an positive number if the order is a numeric evaluation, 0 otherwise.
    */
    public int getOrder() {
        return order;
    }
    
    /**
    * Adds a new element (template) to the end of the list.
    * Returns the new element.
    */
    public FormulaTreeStructure addElement() {
        if (gotoVariable) {
            gotoVariable = false;
            return (FormulaTreeStructure) getChild(2).getChild(0);
        }
        else
            return super.addElement();
    }
    
    /**
    * The Creation of the corresponding linear expression of the formula.
    */
    public String createLinear(String linear) {
        FormulaTreeStructure child;
        // même chose que super.createLinear(String linear), sauf que l'indice de boucle part de 1,
        // au lieu de 0. (le premier fils correspondant à l'ordre de la dérivée, qui n'intervient pas dans la
        // syntaxe)
        linear += getTheOperator();
        for (int i = 1; i < getNbChildren(); i++) {
            child = (FormulaTreeStructure) getChild(i);
            if (i == 1)
                linear += child.createLinear(linear);
            else {
                linear += "," + child.createLinear(linear);
            }
        }
        return linear + getEnding();
    }
}
