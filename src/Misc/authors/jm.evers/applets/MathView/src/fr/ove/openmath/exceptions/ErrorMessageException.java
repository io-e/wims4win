/*
$Id: ErrorMessageException.java,v 1.3 2003/02/18 11:48:46 sander Exp $
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

/**
* An exception to subclass to alerts application of errors or to specialize
* the treatment of the symbol that causes the thrown of that kind of instance.<BR>
* This class must be subclassed (it is an abstract class) to set the adequat error
* message and the possible details according to the symbol.
*
* @author Â© 1998 DIRAT Laurent
* @version 1.0  13/04/99
*/
public abstract class ErrorMessageException extends RuntimeException {
    /**
    * The error symbol which cause this exception
    */
    private String symbol;
    
    /**
    * The error message
    */
    private String errorMessage;
    
    /** 
    * The details to send with the message
    */
    private Object details;
    
    /**
    * Do we have to throw the exception again ?
    */
    private boolean throwAgain = false;

    /**
    * The constructor.
    * @param symbol The symbol which throws the instance.
    */
    public ErrorMessageException(String symbol) {
        this(symbol, null);
    }

    /**
    * The constructor.
    * @param symbol The symbol which throws the instance.
    * @param details the details of the exception
    */
    public ErrorMessageException(String symbol, Object details) {
        this.symbol = symbol;
        this.details = details;
    }
    
    /**
    * Returns the symbols which throws the instance
    */
    public String getSymbol() {
        return symbol;
    }
    
    /**
    * Sets the symbol which throws the instance
    */
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    /**
    * Returns the details of the exception
    */
    public Object getDetails() {
        return details;
    }
    
    /**
    * Sets the details of the exception
    */ 
    public void setDetails(Object details) {
        this.details = details;
    }
    
    /**
    * Sets <CODE>true</CODE> if we've got to throw the exception.<BR>
    * <CODE>false</CODE> otherwise.
    */
    public void setThrowAgain(boolean throwAgain) {
        this.throwAgain = throwAgain;
    }
    
    /**
    * Returns if we've got to throw the exception.
    */
    public boolean getThrowAgain() {
        return throwAgain;
    }
    
    /**
    * Sets the error message
    */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    /**
    * Returns the error message
    */
    public String getErrorMessage() {
        return errorMessage;
    }
}