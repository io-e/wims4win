/*
$Id: LinearParser.java,v 1.3 2003/02/18 11:48:46 sander Exp $
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


package fr.ove.openmath.jome.ctrl.linear;

import java.util.*;
import fr.ove.openmath.jome.ctrl.linear.events.*;
import fr.ove.utils.*;

/**
* A bean which parses, more precisely tokenizes, what we can call the linear form of an expression.<BR>
* For each specific token encountered, an event (@see LinearParserEvent) is fired to all the listeners
* (@see LinearParserListener) of the instance.<BR>
* The event fired is made of 2 parts :
* <UL>
* <LI> the token identifier (for example, for + (addition) the associated identifier is
* <CODE>LinearParserEvent.ADDITION</CODE>)</LI>
* <LI> the value of the token, if needed. (for example, when the digit 2 is encountered, the identifier
* has the <CODE>LinearParserEvent.INTEGER</CODE> value, and the token value is represented by the @see String "2")</LI>
* </UL>
* When the parsing starts, an event whith the identifier set to <CODE>LinearParserEvent.START_EXPRESSION</CODE> is
* fired.
* Similarly, when the parsing ends (the end of the linear form has been reached), an event whith the identifier
* set to <CODE>LinearParserEvent.END_EXPRESSION</CODE> is fired.<BR>
* In order to make distinctions between what should be variables and special keywords, a resources file
* (LinearParserResources.properties) is attached to the parser. This file contains the user defined keywords.<BR>
* To add your own keywords, edit this properties file with respect to the used format (see this file for further
* details).
*
* @author Â© 1999 DIRAT Laurent
* @version 1.0 27/09/1999
*/
public class LinearParser implements java.io.Serializable {
    /**
    * The parser resources manager.<BR>
    * Reads into the properties file (LinearParserResources.properties) to get the user defined specific
    * keywords.
    */
    private ResourcesManager resourcesManager;
    
    /**
    * The list of listeners of the instance
    */
    private Vector listeners = new Vector();

    /**
    * The event sent to all the listeners of the instance during the parsing.
    */
    private LinearParserEvent linearParserEvent = new LinearParserEvent(this);
    
    /**
    * The constructor.
    */
    public LinearParser() {
        resourcesManager = new ResourcesManager("fr.ove.openmath.jome.ctrl.linear.LinearParserResources");
    }
    
    /**
    * Registers another listener of the instance.
    * @param linearParserListener the listener to add.
    */
    public synchronized void addLinearParserListener(LinearParserListener linearParserListener) {
        listeners.addElement(linearParserListener);
    }

    /**
    * Removes a listener.
    * @param linearParserListener the listener to remove.
    */
    public synchronized void removeLinearParserListener(LinearParserListener linearParserListener) {
        listeners.removeElement(linearParserListener);
    }

    /**
    * Fires a LinearParserEvent event to registered listeners.
    * @param linearParserEvent event encapsulating relevant information.
    */
    public void fireLinearParserEvent(LinearParserEvent linearParserEvent) {
        for (int i = 0; i < listeners.size(); i++)
          ((LinearParserListener) listeners.elementAt(i)).consumeLinearParserEvent(linearParserEvent);
    }

    /**
    * Parses the specified linear form of the formula.
    * @param expression the linear form of the formula.
    */
    public void parse(String expression) {
        // Le nombre de tokens utilisÃ©s .
        int nbTokenUsed = 0;
        // On spÃ©cifie ici quels sont les tokens qui vont permettre de dÃ©couper
        // l'expression passÃ©e en argument.
        StringTokenizer theTokens = new StringTokenizer(expression, "+-/*^_()=<>;, {}\\?!\u2192\u21D4\u211E\u2027", true);
        // Des tokens temporaires pour travailler sur le token suivant le token courant.
        StringTokenizer tmpTokens = null;
        // Le token courant
        String token = null;
        // Le token suivant le token courant
        String nextToken = null;
        
        // On commence le parsing
        linearParserEvent.setToken(LinearParserEvent.START_EXPRESSION, null);
        fireLinearParserEvent(linearParserEvent);
        
        while (theTokens.hasMoreTokens()) {
            token = theTokens.nextToken();
            nbTokenUsed++;
	    
            //if (token.equals("[")){System.out.println(linearParserEvent.toString());}

            if (token.equals(" "))
                continue;
		//linearParserEvent.setToken(LinearParserEvent.RESERVED,token);
            else if (token.equals("\u2192"))
		linearParserEvent.setToken(LinearParserEvent.RESERVED, "\u2192");
            else if (token.equals("\u21D4"))
		linearParserEvent.setToken(LinearParserEvent.RESERVED, "\u21D4");
            else if (token.equals("\u211E"))
		linearParserEvent.setToken(LinearParserEvent.RESERVED, "\u211E");
	    else if (token.equals("+"))
        	linearParserEvent.setToken(LinearParserEvent.ADDITION, token);
            else if (token.equals("-"))
        	linearParserEvent.setToken(LinearParserEvent.SUBSTRACTION, token);
            else if (token.equals("*"))
        	linearParserEvent.setToken(LinearParserEvent.MULTIPLICATION, "\u2027");
            else if (token.equals("\u2027"))
        	linearParserEvent.setToken(LinearParserEvent.RESERVED, "\u2027");
            else if (token.equals("/"))
        	linearParserEvent.setToken(LinearParserEvent.DIVISION, token);
            else if (token.equals("^"))
        	linearParserEvent.setToken(LinearParserEvent.POWER, token);
            else if (token.equals("("))
        	linearParserEvent.setToken(LinearParserEvent.OPEN_PAREN, token);
            else if (token.equals(")"))
        	linearParserEvent.setToken(LinearParserEvent.CLOSE_PAREN, token);
            else if (token.equals("{"))
    		linearParserEvent.setToken(LinearParserEvent.OPEN_CURLY, token);
            else if (token.equals("}"))
                linearParserEvent.setToken(LinearParserEvent.CLOSE_CURLY, token);
	    else if (token.equals(","))                                                                               
               linearParserEvent.setToken(LinearParserEvent.SEPARATOR, null); 
// no check on [] because  n-th root syntax: sqrt[3](123) --> sqrt*[*3*]*(123)
//            else if (token.equals("["))
//                linearParserEvent.setToken(LinearParserEvent.OPEN_BRACKET, token);
//                //linearParserEvent.setToken(LinearParserEvent.RESERVED, token);
//            else if (token.equals("]"))
//        	linearParserEvent.setToken(LinearParserEvent.CLOSE_BRACKET, token);
//                //linearParserEvent.setToken(LinearParserEvent.RESERVED, token);
            else if (token.equals("_"))
               linearParserEvent.setToken(LinearParserEvent.UNDERSCRIPT, token);
	    else if (token.equals("<") || token.equals(">")) {
                // On re-tokenize l'expression pour rÃ©cupÃ©rer l'Ã©lement suivant le token
                // (Ã  dÃ©faut d'autre chose.... :o( )
                tmpTokens = new StringTokenizer(expression, "+-/*^_()=<>;, {}\\?!\u2192\u21D4\u211E\u2027", true );
                for (int i = 0; i < nbTokenUsed + 1; i++) {
                    try {
                        nextToken = tmpTokens.nextToken();
                    }
                    catch (NoSuchElementException  e) {
                        nextToken = null;
                    }
                }

                if (token.equals("<")) {
                    if (nextToken != null ) {
                        if (nextToken.equals("=")) {
                            theTokens.nextToken();
                            nbTokenUsed++;
                            linearParserEvent.setToken(LinearParserEvent.LESSEQUAL, "<=");
                        }
                        else if (nextToken.equals(">")) {
                            theTokens.nextToken();
                            nbTokenUsed++;
                            linearParserEvent.setToken(LinearParserEvent.UNEQUAL, "<>");
                        }
                        else
                            linearParserEvent.setToken(LinearParserEvent.LESS, token);
                    }
                    else
                        linearParserEvent.setToken(LinearParserEvent.LESS, token);
                }
                else {
                    if (nextToken != null ) {
                        if (nextToken.equals("=")) {
                            theTokens.nextToken();
                            nbTokenUsed++;
                            linearParserEvent.setToken(LinearParserEvent.GREATEREQUAL, ">=");
                        }
                        else
                            linearParserEvent.setToken(LinearParserEvent.GREATER, token);
                    }
                    else
                        linearParserEvent.setToken(LinearParserEvent.GREATER, token);
                }
            }
            else if (token.equals("="))
                linearParserEvent.setToken(LinearParserEvent.EQUAL, token);
            else if (token.equals("\\"))
                linearParserEvent.setToken(LinearParserEvent.SPECIALIZED, token);
            else if (token.equals("?"))
                linearParserEvent.setToken(LinearParserEvent.SPECIALIZED, token);
            else if (token.equals("!"))
                linearParserEvent.setToken(LinearParserEvent.SPECIALIZED, "fact");
            else {
                if (NumberUtils.isNumber(token)) {
                    if (NumberUtils.isFloat(token))
                        linearParserEvent.setToken(LinearParserEvent.FLOAT, token);
                    else
                        linearParserEvent.setToken(LinearParserEvent.INTEGER, token);
                }
                else {
                    // On a saisi des lettres. Il faut qu'on regarde si Ã  la suite de ces
                    // lettres, il n'y a pas comme token suivant une parenthÃ¨se. Si tel est le cas,
                    // c'est que l'on a voulu saisir une fonction.

                    // On re-tokenize l'expression pour rÃ©cupÃ©rer l'Ã©lement suivant le token
                    // (Ã  dÃ©faut d'autre chose.... :o( )
                    tmpTokens = new StringTokenizer(expression, "+-/*^_()=<>;, {}\\?!\u2192\u21D4\u211E\u2027", true );
                    for (int i = 0; i < nbTokenUsed + 1; i++) {
                        try {
                            nextToken = tmpTokens.nextToken();
                        }
                        catch (NoSuchElementException  e) {
                            nextToken = null;
                        }
                    }

                    if (nextToken != null) {
                        if (nextToken.equals("(")) {
                            // C'est une fonction
                            linearParserEvent.setToken(LinearParserEvent.FUNCTION , token);
                            // On passe outre le token suivant, i.e. la parenthÃ¨se ouvrante.
                            theTokens.nextToken();
                            nbTokenUsed++;
                        }
                        else {
                            String str1 = resourcesManager.getResourceString(token);
                            if (str1 == null)
                                // C'est une variable.
                                linearParserEvent.setToken(LinearParserEvent.VARIABLE, token);
                            else
                                linearParserEvent.setToken(LinearParserEvent.RESERVED, token);
                        }
                    }
                    else {
                            String str2 = resourcesManager.getResourceString(token);
                            if (str2 == null)
                                // C'est une variable.
                                linearParserEvent.setToken(LinearParserEvent.VARIABLE, token);
                            else
                                linearParserEvent.setToken(LinearParserEvent.RESERVED,token);
                    
		    }
                }
            }
            
            fireLinearParserEvent(linearParserEvent);
        }
        
        // On a fini le parsing
        linearParserEvent.setToken(LinearParserEvent.END_EXPRESSION, null);
        fireLinearParserEvent(linearParserEvent);
    }
    
    public static void main(String args[]) {
        LinearParserListener l = new LinearParserListener() {
            public void consumeLinearParserEvent(LinearParserEvent linearParserEvent) {
                System.out.println(linearParserEvent.toString());
            }
        };
        
        LinearParser lp = new LinearParser();
        lp.addLinearParserListener(l);
        lp.parse("1+2+23+C+in+pi+x_8");
    }
}