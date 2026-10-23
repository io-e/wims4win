/*
$Id: ControlException.java,v 1.3 2003/02/18 11:48:46 sander Exp $
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


package fr.ove.openmath.exceptions;

import fr.ove.openmath.exceptions.*;

/**
* Specific Control exception to specialize treatment of errors according
* to the symbol that thrown it.
*
* @author Â© 1999 DIRAT Laurent
* @version 1.0  28/05/99
*/
public class ControlException extends ErrorMessageException {
    /**
    * The constructor.
    * @param symbol The symbol which thrown the instance.
    */
    public ControlException(String symbol) {
        this(symbol, null);
    }

    /**
    * The constructor.
    * @param symbol the symbol which thrown the instance.
    * @param details the details of the exception
    */
    public ControlException(String symbol, Object details) {
        super(symbol, details);

        if (symbol.equals("unhandledSymbol"))
            setErrorMessage("Symbol Unhandled by the Application");
        else if (symbol.equals("unknownSymbol"))
            setErrorMessage("The Symbol does not Exist in the Specified CD");
        else if (symbol.equals("unknownCD"))
            setErrorMessage("The Specified CD is not Handled by the Application");
        else if (symbol.equals("algorithm"))
            setErrorMessage("Operation not or Partially Implemented");
        else if (symbol.equals("limitation"))
            setErrorMessage("Implementation Limitation");
        else if (symbol.equals("unexpected"))
            setErrorMessage("Unexpected Problem");
        else if (symbol.equals("system"))
            setErrorMessage("System Error");
        else if (symbol.equals("terminated"))
            setErrorMessage("Communication Terminted. Server Shutted Down");
        else if (symbol.equals("commandOk"))
            setErrorMessage("Command Successfully Executed");
        else if (symbol.equals("encodingError"))
            setErrorMessage("Lexical and/or Syntactic Error Detected");
        else if (symbol.equals("semanticError"))
            setErrorMessage("Semantic Error Detected");
    }
}
