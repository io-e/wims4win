package rene.zirkel.objects;

import rene.zirkel.construction.Construction;
import rene.zirkel.structures.Coordinates;

// file: IntersectionObject.java


public class LineIntersectionObject extends IntersectionObject
{	public LineIntersectionObject (Construction c, 
		PrimitiveLineObject P1, PrimitiveLineObject P2)
	{	super(c,P1,P2);
		validate();
	}
	
	public void updateCircleDep ()
	{	((PrimitiveLineObject)P1).addDep(this);
		((PrimitiveLineObject)P2).addDep(this);
	}

	public void validate ()
	{	if (!P1.valid() || !P2.valid()) Valid=false;
		else Valid=true; 
		if (!Valid) return;
		Coordinates c=
			PrimitiveLineObject.intersect((PrimitiveLineObject)P1,
					(PrimitiveLineObject)P2);
		if (c==null) { Valid=false; return; }
		X=c.X; Y=c.Y;
		if (Restricted)
		{	if (!((PrimitiveLineObject)P1).contains(X,Y)) Valid=false;
			else if (!((PrimitiveLineObject)P2).contains(X,Y)) Valid=false;
		}
	}
}
