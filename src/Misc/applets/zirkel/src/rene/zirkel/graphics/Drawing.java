package rene.zirkel.graphics;

import rene.util.*;
import rene.zirkel.structures.CoordinatesXY;

import java.util.*;

// file: Drawing.java

public class Drawing
{	MyVector P;
	int Col;

	public Drawing ()
	{	P=new MyVector();
	}

	public void addXY (double x, double y)
	{	P.addElement(new CoordinatesXY(x,y));
	}
	
	public Enumeration elements ()
	{	return P.elements();
	}
	
	public void setColor (int col)
	{	Col=col;
	}
	
	public int getColor ()
	{	return Col;
	}
}
