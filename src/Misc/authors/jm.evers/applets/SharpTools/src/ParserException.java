/*                                                                                                             
License.                                                                                                       
This is free software; you can redistribute it and/or modify                                                   
it under the terms of the GNU General Public License as published by                                           
the Free Software Foundation;                                                                                  
HotEqn is distributed in the hope that it will be useful,                                                      
but WITHOUT ANY WARRANTY; without even the implied warranty of                                                 
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                                                  
GNU General Public License for more details.                                                                   
You should have received a copy of the GNU General Public License                                              
along with this program.  If not, see <http://www.gnu.org/licenses/>.                                          
*/ 
package SharpTools;
/*
 * @(#)ParserException.java
 * 
 * $Id: ParserException.java,v 1.6 2000/11/18 07:12:17 huaz Exp $
 * 
 * Created on October 28, 2000, 6:26 PM
 */

/**
 * This Exception is raised when Formula fails in tokenizing or parsing the
 * formula.
 *
 * @author Hua Zhong <huaz@cs.columbia.edu>
 * @version $Revision: 1.6 $
 */
public class ParserException extends Exception {
    private boolean quiet;
    private String msg;

    /**
     * Contructor for ParserException.  By default, sets quiet to true.
     */
    public ParserException() { quiet = true; };

    /**
     * @param msg the error message string 
     */
    public ParserException(String msg) { super(msg); this.msg = msg; };

    /**
     * @param msg the error object
     */
    public ParserException(Object msg) {
	super(msg.toString());
	this.msg = msg.toString();
    };

    /**
     * This returns the value of quiet.
     *
     * @return true if quiet is true, false otherwise
     */
    public boolean isQuiet() { return quiet; }

    /**
     * toString method for ParserException.
     *
     * @return the error message string
     */
    public String toString() { return msg; }
}










