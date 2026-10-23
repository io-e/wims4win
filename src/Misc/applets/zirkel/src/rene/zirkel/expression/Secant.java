/*
 * Created on 05.11.2005
 *
 */
package rene.zirkel.expression;

import rene.zirkel.construction.ConstructionException;
import rene.zirkel.objects.FunctionObject;

public class Secant
{	public static double compute (FunctionObject F,
		double a, double b, double eps)
	throws ConstructionException
	{	double ay=F.evaluateF(a),by=F.evaluateF(b);
		double c=(a+b)/2;
		if (ay*by>eps)
			throw new ConstructionException("");
		while (Math.abs(b-a)>eps)
		{	double cy=F.evaluateF(c);
			if (Math.abs(cy)<eps) return c;
			if (cy*ay>0)
			{	if (Math.abs(ay-cy)<eps)
				{	a=c; ay=cy; c=(a+b)/2;
				}
				else
				{	double d=a-ay*(a-c)/(ay-cy);
					if (d>b || d<a)
					{	a=c; ay=cy; c=(a+b)/2;
					}
					else
					{	a=c; ay=cy; c=d;
					}
				}
			}
			else
			{	double d=a-ay*(a-c)/(ay-cy);
				b=c; by=cy; c=d;
			}
		}
		return c;
	}
}

