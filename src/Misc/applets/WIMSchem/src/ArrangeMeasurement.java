/*
    Sketch Elements: Chemistry molecular diagram drawing tool.
    
    (c) 2008 Dr. Alex M. Clark
    
    Released as GNUware, under the Gnu Public License (GPL)
    
    See www.gnu.org for details.
*/

package WIMSchem;

/*
    Interface used to provide text metrics for some arbitrary font system
*/

public interface ArrangeMeasurement
{
    // conversion between angstrom units of molecules, and device units
    double scale();
    double angToX(double AX);
    double angToY(double AY);
    double xToAng(double PX);
    double yToAng(double PY);

    // returns an array of 3 numbers: ascent, descent, width
    double[] measureText(String str,double fontSize);
}
