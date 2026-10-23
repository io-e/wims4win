/*
 * Created on 27.10.2005
 *
 */
package rene.zirkel.objects;

public interface PointonObject 
{	public void project (PointObject P);
	public void project (PointObject P, double alpha);
	public boolean canInteresectWith (ConstructionObject o);
}
