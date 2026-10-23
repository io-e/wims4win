package rene.zirkel.expression;

import rene.zirkel.construction.ConstructionException;

public class NoValueException extends ConstructionException
{	boolean Valid;
	public NoValueException (boolean valid)
	{	super("NoValueException"); Valid=valid;
	}
	public boolean isValid ()
	{	return Valid;
	}
}
