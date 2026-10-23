package rene.zirkel.construction;

public class ConstructionException extends Exception
{	String S;
	public ConstructionException (String s)
	{	super(s); S=s;
	}
	public String getDescription ()
	{	return S;
	}
	public String toString ()
	{	return S;
	}
}
