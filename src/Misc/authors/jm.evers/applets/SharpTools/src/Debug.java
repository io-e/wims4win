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
/*
 * This is a class used for print out debug information and can be
 * easily turned on/off.
 */
package SharpTools;
import java.io.*;

public class Debug {

    static private boolean debug = false;
    
    static void setDebug(boolean flag) {
	debug = flag;
    }
    
    static boolean isDebug() {
	return debug;
    }
    
    static void println(Object s) {
	if (debug)
	    System.out.println(s.toString());
    }        
}
