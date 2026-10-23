/*
$Id: VariableOrNumber.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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

import fr.ove.openmath.jome.model.*;
import fr.ove.openmath.jome.model.events.*;
import fr.ove.openmath.jome.model.evaluation.*;

/**
* Elements in the formula such as variables (letters) and numbers.<BR>
* <CODE>VariableOrNumber</CODE> represents a node in the formula tree.
* Terminal node, so leaves.
*
* @author © 1999 DIRAT Laurent
* @version 2.0 24/06/1999
*/
public class VariableOrNumber extends Constant {
    /**
    * To check if the Constant is a number or not.
    */
    private boolean isNumber; 
    
    /**
    * To check if the number represented by the Constant is an integer or a float.
    * So if the value is <CODE>true</CODE>, it's an integer; Otherwise, it's a float.
    */
    private boolean isInteger;
    
    /**
    * To indicate that we have a constant with a specific signification.<BR>
    * In this particular case, it's a request variable (from the mfd2 CD). <BR>
    * By default, it is not a request variable.
    */
    private boolean isRequestVariable = false;  // à voir plus tard mais probablement à virer.
    

    /**
    * The default constructor.<BR>
    * Constructs a template.
    */
    public VariableOrNumber() {
        this("VARIABLE", "[?]", false, false);
        setIsTemplate(true);
    }
    
    /**
    * The Constructor.
    *
    * @param value the value of the constant.
    * @param isNumber to set if the constant is a number or not.
    * @param isInteger to set, if the constant is a numbern if it is an integer or a float.
    */
    public VariableOrNumber(String resourceIdentifier, String value, boolean isNumber, boolean isInteger) {
        super();
        setResourceIdentifier(resourceIdentifier);
        setValue(value);
        this.isNumber = isNumber;
        this.isInteger = isInteger;
    }
    
    /**
    * Sets the constant as a number.
    * @param isNumber <CODE>true</CODE> if it is a number.<CODE>false</CODE>
    * otherwise.
    */
    public void setIsNumber(boolean isNumber) {
        this.isNumber = isNumber;
    }
    
    /**
    * Returns <CODE>true</CODE> if it is a number.<BR>
    * <CODE>false</CODE> otherwise.
    */
    public boolean isNumber() {
        return isNumber;
    }
    
    /**
    * Sets the constant, which is a number, as an integer or as a float.
    * @param isInteger <CODE>true</CODE> if it is an integer.<CODE>false</CODE>
    * if it is a float.
    */
    public void setIsInteger(boolean isInteger) {
        this.isInteger = isInteger;
    }
    
    /**
    * Returns <CODE>true</CODE> if it is an integer.<CODE>false</CODE>
    * if it is a float.
    */
    public boolean isInteger() {
        return isInteger;
    }

    /**
    * Sets the constant as a request variable.
    */
    public void setIsRequestVariable(boolean isRequestVariable) {
        this.isRequestVariable = isRequestVariable;
    }
    
    /**
    * Returns <CODE>true</CODE> if it is a request variable.<BR>
    * <CODE>false</CODE> otherwise.
    */
    public boolean isRequestVariable() {
        return isRequestVariable;
    }

    /**
    * Inserts the operator instance in the formula tree, from the current insertion position.
    * (checks the priorities and goes up in the tree if necessary).
    *
    * @param ope the current insertion position.
    * @return the new insertion position.
    */
    public FormulaTreeStructure insert(FormulaTreeStructure current) {
        if (isTemplate()) {
            if (isRequestVariable)
                return super.insert(current);
                
            // Si l'instance que l'on doit insérer est un template, alors on l'insère
            // directement à la position courante (heuu... on sait ce qu'on fait ;o) )
            current.addChild(this);
            return this;
        }
        else {  // L'instance n'est pas un template.
            if (current.getAsOperatorPriority() == resourcesManager.getAsOperatorPriority("constantPriorities")) {
                // A ce stade 2 solutions. Soit :
                //  * current est un template, alors encore 2 possibilités. Soit :
                //      - current est une request variable, alors si l'instance est une variable, c'est qu'on
                //        est en train de saisir sa valeur. Sinon, c'est une erreur syntaxique, on remplace
                //        la request variable, par l'instance.
                //      - current n'est pas une request variable, on remplace current par l'instance
                //  * current n'est pas un template, il faut donc créer une multiplication implicite.
                if (current.isTemplate()) {
                    if (((VariableOrNumber) current).isRequestVariable && !isNumber) {
                        current.setIsTemplate(false);
                        ((VariableOrNumber) current).setValue(getValue());
                        return current;
                    }
                    else {
                        FormulaTreeStructure father = (FormulaTreeStructure) current.getFather();
                        father.addChild(this, current.getRank());
                        father.removeChild(current);
                        return this;
                    }                    
                }
                else {// on créé donc une multiplication implicite.
                    current = (new Multiplication()).insert(current);
                    current = insert(current);
                    return current;
                }
            }
            else { // On est dans le cas de l'insertion courante d'une constante dans la FTS
                if ((current.getFather() == null) && (current.getNbChildren() == 0)) {
                    // On est dans ce cas là, seulement au tout début de la saisie de la 
                    // formule. On est obligé de faire ce test à cause de la multiplication
                    // implicite avec les parenthèses (Ex: (a+b)c ==> (a+b)*c, dans ce cas là,
                    // current.getNbChildren() est != de 0)
                    //On insère directement à la position courante
                    current.addChild(this);
                    
                    return this;
                }
                else { // On est dans aucun des cas précédents, on créé donc une
                    // multiplication implicite.
                    current = (new Multiplication()).insert(current);
                    current = insert(current);
                    return current;
                }
            }
        }
    }
    
    /**
    * The Creation of the corresponding linear expression of the formula.
    */
    public String createLinear(String linear) {
        if (isRequestVariable)
            linear += "?";
            
        if (!isTemplate())
            linear += getValue();
            
        return linear;
    }
}
