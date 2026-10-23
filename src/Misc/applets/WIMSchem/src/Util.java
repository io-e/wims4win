/*
    Sketch Elements: Chemistry molecular diagram drawing tool.
    
    (c) 2005 Dr. Alex M. Clark
    
    Released as GNUware, under the Gnu Public License (GPL)
    
    See www.gnu.org for details.
*/

package WIMSchem;

import java.util.*;
import java.text.*;
import java.lang.*;
import javax.swing.*;

// A very generic class containing static utility functions, which are painfully absent from the Java libraries.

public class Util
{
    // pure convenience
    public static void write(String S) {System.out.print(S);}
    public static void writeln(String S) {System.out.println(S);}

    public static void errmsg(String Title,String Text)
    {
    	JOptionPane.showMessageDialog(null,Text,Title,JOptionPane.ERROR_MESSAGE);
    }
    
    public static String arrayStr(int[] A) {String str=""; for (int n=0;n<A.length;n++) str+=(n>0 ? " " : "")+A[n]; return str;}
    public static String arrayStr(double[] A) {String str=""; for (int n=0;n<A.length;n++) str+=(n>0 ? " " : "")+A[n]; return str;}
    public static String arrayStr(String[] A) {String str=""; for (int n=0;n<A.length;n++) str+=(n>0 ? " " : "")+A[n]; return str;}
    public static String arrayStr(boolean[] A) {String str=""; for (int n=0;n<A.length;n++) str+=(n>0 ? " " : "")+A[n]; return str;}
    
    // parse number-from-string functions which return a default value if it's badly formatted, instead of throwing an exception
    public static int safeInt(String S,int Def) 
    {
    	if (S==null) return Def;
	try {return new Integer(S).intValue();} 
	catch (NumberFormatException e) {return Def;}
    }
    public static int safeInt(String S) {return safeInt(S,0);}
    public static double safeDouble(String S,double Def) 
    {
    	if (S==null) return Def;
    	try {return new Double(S).doubleValue();} 
	catch (NumberFormatException e) {return Def;}
    }
    public static double safeDouble(String S) {return safeDouble(S,0);}
    
    public static int iround(double V) {return (int)Math.round(V);}
    
    public static double sqr(double V) {return V*V;}
    public static double norm2(double x,double y) {return x*x+y*y;}
    public static double norm2(double x,double y,double z) {return x*x+y*y+z*z;}
    public static double norm(double x,double y) {return Math.sqrt(x*x+y*y);}
    public static double norm(double x,double y,double z) {return Math.sqrt(x*x+y*y+z*z);}
    
    // returns {Theta1}-{Theta2}, where both are in radians; the result is corrected to be between -PI and +PI
    public static double angleDiff(double Th1,double Th2)
    {
    	double theta=Th1-Th2;
	return theta-(theta>Math.PI ? 2*Math.PI : 0)+(theta<-Math.PI ? 2*Math.PI : 0);
    }
    
    // equality of real numbers, which are not significantly less than 1
    public static boolean dblEqual(double d1,double d2) {return Math.abs(d1-d2)<1E-10;}

    // integer colour to HTML-style hex colour
    public static String colourHTML(int col)
    {
    	String str=Integer.toHexString(col);
	while (str.length()<6) str="0"+str;
    	return "#"+str;
    }
}
