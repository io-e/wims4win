/*
 * Created on 27.10.2005
 * An InsideObject is an object that can test, if a point is inside it,
 * or on its boundary.
 */
package rene.zirkel.objects;

public interface InsideObject 
{	/**
	* Returns 0.5, if the point is on the boundary, and 1, if it is inside.
	*/
	public double containsInside (PointObject P);
	public boolean keepInside (PointObject P);
}
