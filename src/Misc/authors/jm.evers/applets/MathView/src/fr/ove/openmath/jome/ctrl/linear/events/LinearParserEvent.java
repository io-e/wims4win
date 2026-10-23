/*
$Id: LinearParserEvent.java,v 1.3 2003/02/18 11:48:46 sander Exp $
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


package fr.ove.openmath.jome.ctrl.linear.events;

import java.util.EventObject;

/**
* The event sent during the parsing of a linear form of an expression.<BR>
* Each event sent corresponds to token in the expression parsed. The tokens are the following :
* <UL>
* <LI> VARIABLE when a variable is encountered in the expression </LI>
* <LI> INTEGER  when an integer is encountered in the expression </LI>
* <LI> FLOAT    when a real number is encountered in the expression </LI>
* <LI> ADDITION when the + operator is encountered in the expression </LI>
* <LI> SUBSTRACTION when the - operator is encountered in the expression </LI>
* <LI> MULTIPLICATION when the * operator is encountered in the expression </LI>
* <LI> DIVISION when the / operator is encountered in the expression </LI>
* <LI> POWER    when the ^ operator is encountered in the expression </LI>
* <LI> EQUAL    when the = operator is encountered in the expression </LI>
* <LI> UNEQUAL  when the <> operator is encountered in the expression </LI>
* <LI> LESS     when the < operator is encountered in the expression </LI>
* <LI> LESSEQUAL  when the <= operator is encountered in the expression </LI>
* <LI> GREATER  when the > operator is encountered in the expression </LI>
* <LI> GREATEREQUAL when the >= operator is encountered in the expression </LI>
* <LI> FUNCTION when a name of a function (an function is expected to be an identificator immediately followed
* by an opening parenthesis) is encountered in the expression </LI>
* <LI> SEPARATOR when a separator (, or ;) is encountered in the expression </LI>
* <LI> OPEN_PAREN when an opening paranthesis is encountered in the expression </LI>
* <LI> CLOSE_PAREN when a closing paranthesis is encountered in the expression </LI>
* <LI> OPEN_BRACKET when an opening bracket is encountered in the expression </LI>
* <LI> CLOSE_BRACKET when a closing bracket is encountered in the expression </LI>
* <LI> OPEN_CURLY when an opening curly brace is encountered in the expression </LI>
* <LI> CLOSE_CURLY when a closing curly brace is encountered in the expression </LI>
* <LI> UNDERSCRIPT when the underscript (_) operator is encountered in the expression </LI>
* </UL>
*
* There exists other token identifiers :
* <UL>
* <LI> RESERVED when a specific keyword (define in the property file of the parser) is encountered
* in the expression </LI>
* <LI> SPECIALIZED when a specialized operator is encountered in the expression.<BR>
* (not really clearly defined :o( )</LI>
* </UL>
*
* Finally, 2 other token identifiers dealing with the parsing itself.
* <UL>
* <LI> START_EXPRESSION when the parsing of the expression starts </LI>
* <LI> END_EXPRESSION when the parsing of the expression ends </LI>
* </UL>
*
* @author Â© 1999 DIRAT Laurent
* @version 1.0  27/09/1999
*/
public class LinearParserEvent extends EventObject {
    /**
    * The token identifier.
    */
    private int identifier;

    /**
    * The token value.
    */
    private String value;

    /**
    * The constructor.
    * @param src the object which produces this event.
    */
    public LinearParserEvent(Object src) {
        super(src);
    }

    /**
    * Sets the identifier of the token and its value, if needed.
    * @param identifier the token identifier.
    * @param value the token value.
    */
    public void setToken(int identifier, String value) {
        this.identifier = identifier;
        this.value = value;
    }

    /**
    * Returns the identifier of the token wrapped into the event.
    */
    public int getIdentifier() {
        return identifier;
    }

    /**
    * Returns the identifier of the token wrapped into the event, as a string
    */
    public String getIdentifierAsString() {
        String str = "";

        switch (identifier) {
            case SPACE : str += "SPACE"; break;
            case VARIABLE : str += "VARIABLE"; break;
            case INTEGER  : str += "INTEGER"; break;
            case FLOAT    : str += "FLOAT"; break;
            case ADDITION : str += "ADDITION"; break;
            case SUBSTRACTION : str += "SUBSTRACTION"; break;
            case MULTIPLICATION: str += "MULTIPLICATION"; break;
            case DIVISION : str += "DIVISION"; break;
            case POWER    : str += "POWER"; break;
            case EQUAL    : str += "EQUAL"; break;
            case UNEQUAL  : str += "UNEQUAL"; break;
            case LESS     : str += "LESS"; break;
            case LESSEQUAL  : str += "LESSEQUAL"; break;
            case GREATER  : str += "GREATER"; break;
            case GREATEREQUAL : str += "GREATEREQUAL"; break;
            case FUNCTION : str += "FUNCTION"; break;
            case SEPARATOR : str += "SEPARATOR"; break;
            case OPEN_PAREN : str += "OPEN_PAREN"; break;
            case CLOSE_PAREN : str += "CLOSE_PAREN"; break;
            case OPEN_BRACKET : str += "OPEN_BRACKET"; break;
            case CLOSE_BRACKET : str += "CLOSE_BRACKET"; break;
            case OPEN_CURLY : str += "OPEN_CURLY"; break;
            case CLOSE_CURLY : str += "CLOSE_CURLY"; break;
            case UNDERSCRIPT : str += "UNDERSCRIPT"; break;
            case RESERVED : str += "RESERVED"; break;
            case SPECIALIZED : str += "SPECIALIZED"; break;
            case START_EXPRESSION : str += "START_EXPRESSION"; break;
            case END_EXPRESSION : str += "END_EXPRESSION"; break;
        }

        return str;
    }
    
    /**
    * Returns the int value of the specified identifier string representation .
    */
    public static int getStringIdentifierAsInt(String identifier) {
        int value = 0;

        if (identifier.equals("SPACE")) 
            value = 0;
        if (identifier.equals("VARIABLE")) 
            value = 1;
        else if (identifier.equals("INTEGER"))
            value = 2;
        else if (identifier.equals("FLOAT"))
            value = 3;
        else if (identifier.equals("ADDITION"))
            value = 4;
        else if (identifier.equals("SUBSTRACTION"))
            value = 5;
        else if (identifier.equals("MULTIPLICATION"))
            value = 6;
        else if (identifier.equals("DIVISION"))
            value = 7;
        else if (identifier.equals("POWER"))
            value = 8;
        else if (identifier.equals("EQUAL"))
            value = 9;
        else if (identifier.equals("UNEQUAL"))
            value = 10;
        else if (identifier.equals("LESS"))
            value = 11;
        else if (identifier.equals("LESSEQUAL"))
            value = 12;
        else if (identifier.equals("GREATER"))
            value = 13;
        else if (identifier.equals("GREATEREQUAL"))
            value = 14;
        else if (identifier.equals("FUNCTION"))
            value = 15;
        else if (identifier.equals("SEPARATOR"))
            value = 16;
        else if (identifier.equals("OPEN_PAREN"))
            value = 17;
        else if (identifier.equals("CLOSE_PAREN"))
            value = 18;
        else if (identifier.equals("OPEN_BRACKET"))
            value = 19;
        else if (identifier.equals("CLOSE_BRACKET"))
            value = 20;
        else if (identifier.equals("OPEN_CURLY"))
            value = 21;
        else if (identifier.equals("CLOSE_CURLY"))
            value = 22;
        else if (identifier.equals("RESERVED"))
            value = 23;
        else if (identifier.equals("UNDERSCRIPT"))
            value = 24;
        else if (identifier.equals("SPECIALIZED"))
            value = 25;
        else if (identifier.equals("START_EXPRESSION"))
            value = 26;
        else if (identifier.equals("END_EXPRESSION"))
            value = 27;

        return value;
    }
    
    /**
    * Returns the value of the identifier.
    */
    public String getValue() {
        return value;
    }
    
    /**
    * Returns a @see String representation of the instance.<BR>
    * Mainly for debugg.
    */
    public String toString() {
        return getIdentifierAsString() + "\t" + value;
    }

    /**
    * The different command tokens.
    */

    // The basic elements of a formula
    public static final int SPACE               = 0;
    public static final int VARIABLE            = 1;
    public static final int INTEGER             = 2;
    public static final int FLOAT               = 3;

    // The basic operators
    public static final int ADDITION            = 4;
    public static final int SUBSTRACTION        = 5;
    public static final int MULTIPLICATION      = 6;
    public static final int DIVISION            = 7;
    public static final int POWER               = 8;

    // The relational operators
    public static final int EQUAL               = 9;
    public static final int UNEQUAL             = 10;
    public static final int LESS                = 11;
    public static final int LESSEQUAL           = 12;
    public static final int GREATER             = 13;
    public static final int GREATEREQUAL        = 14;

    // Special operators
    public static final int FUNCTION            = 15;
    public static final int SEPARATOR           = 16;

    // The different kinds of bracket
    public static final int OPEN_PAREN          = 17;
    public static final int CLOSE_PAREN         = 18;
    public static final int OPEN_BRACKET        = 19;
    public static final int CLOSE_BRACKET       = 20;
    public static final int OPEN_CURLY          = 21;
    public static final int CLOSE_CURLY         = 22;

    // Special terminals
    public static final int RESERVED            = 23;

    // Special operators
    public static final int UNDERSCRIPT         = 24;
    public static final int SPECIALIZED         = 25;

    // Parsing event
    public static final int START_EXPRESSION    = 26;
    public static final int END_EXPRESSION      = 27;
}
