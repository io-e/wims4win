/*
 * Created on 13.11.2005
 *
 */
package rene.zirkel.expression;

import rene.zirkel.construction.ConstructionException;
import rene.zirkel.objects.FunctionObject;

public class ConvexMin
{	
	public static double computeMin (
			FunctionObject F, double a, double b,
			double eps)
		throws ConstructionException
	{	double lambda=(Math.sqrt(5)-1)/2;
		double x2=lambda*a+(1-lambda)*b;
		double y2=F.evaluateF(x2);
		double x3=(1-lambda)*a+lambda*b;
		double y3=F.evaluateF(x3);
		while (b-a>eps)
		{	if (y2<y3)
			{	b=x3; x3=x2; y3=y2;
				x2=lambda*a+(1-lambda)*b; y2=F.evaluateF(x2);
			}
			else
			{	a=x2; x2=x3; y2=y3;
				x3=(1-lambda)*a+lambda*b; y3=F.evaluateF(x3);
			}
		}
		return (a+b)/2;
	}
		
	public static double computeMax (
			FunctionObject F, double a, double b,
			double eps)
		throws ConstructionException
	{	double lambda=(Math.sqrt(5)-1)/2;
		double x2=lambda*a+(1-lambda)*b;
		double y2=F.evaluateF(x2);
		double x3=(1-lambda)*a+lambda*b;
		double y3=F.evaluateF(x3);
		while (b-a>eps)
		{	if (y2>y3)
			{	b=x3; x3=x2; y3=y2;
				x2=lambda*a+(1-lambda)*b; y2=F.evaluateF(x2);
			}
			else
			{	a=x2; x2=x3; y2=y3;
				x3=(1-lambda)*a+lambda*b; y3=F.evaluateF(x3);
			}
		}
		return (a+b)/2;
	}
}



