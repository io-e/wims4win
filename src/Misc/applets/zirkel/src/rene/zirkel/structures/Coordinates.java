package rene.zirkel.structures;

// file: Coordinates.java

public class Coordinates
{	public double X,Y,X1,Y1;
	public boolean flag=true;
	public int Color=-1,Thickness=-1;
	public Coordinates (double x, double y)
	{	X=x; Y=y;
	}
	public Coordinates (double x, double y, double x1, double y1)
	{	X=x; Y=y; X1=x1; Y1=y1;
	}
}
