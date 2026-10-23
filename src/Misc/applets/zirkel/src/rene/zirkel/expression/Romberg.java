/*
 * Created on 05.11.2005
 *
 */
package rene.zirkel.expression;

import rene.zirkel.construction.ConstructionException;
import rene.zirkel.objects.FunctionObject;

public class Romberg
{	/**
	Summiere f(x),f(x+h),...,f(x+n*h)
	*/
	private static double sumUp (FunctionObject F,
		double x, double h, int n)
	throws ConstructionException
	{   double sum=F.evaluateF(x);
	  
	    for (int i=1; i<=n; i++)
	    {   sum=sum+F.evaluateF(x+i*h);
	    }
	    return sum;
	}
	
	/** 
	Romberg-Verfahren.
	@param a,b = Intervall-Grenzen
	@param F = Funktion, die integriert wird
	@param nstart = Anzahl der ersten Unterteilungen
	@param eps = Relative Genauigkeit zum Abbruch
	@param maxiter = Maximale Iterationen
	@return Integral oder RuntimeException
	*/
	static public double compute (FunctionObject F,
		double a, double b,
		int nstart, double eps, int maxiter)
	throws ConstructionException
	{			
		// Ergebnisse der Trapezregel mit Schrittweite h/2^i
		double t[]=new double[maxiter];
		int n=nstart;
		
		// Anfangsschrittweite
		double h=(b-a)/n;
		// Berechne Trapezregel dazu und d[0]
		double tlast=t[0]=(F.evaluateF(a)+F.evaluateF(b)+
					2*sumUp(F,a+h,h,n-2))*h/2;
		double tlastbutone=tlast;
		// Bisheriges Ergebnis festhalten
		double old=t[0];
	
		// Halbiere Schrittweite bis Genauigkeit erreicht		
		for (int i=1; i<maxiter; i++)
		{	// Halbiere Schrittweite:
			h=h/2;
			n=n*2;
			
			// Berechne Trapezregel (unter Verwendung des
			// letzten Ergebnisses:
			t[i]=tlast/2+sumUp(F,a+h,2*h,n/2-1)*h;
			tlastbutone=tlast;
			tlast=t[i];
			
			// Update der t[i]:
			double q=4;
			for (int j=i-1; j>=0; j--)
			{	t[j]=t[j+1]+(t[j+1]-t[j])/(q-1);
				q=q*4;
			}
				
			// Abbruch-Kriterium
			double res=t[0];
			if (Math.abs((res-old)/res)<eps) return res;
			old=res;
		}
		
		// Bei Überschreiten der Interationsgrenze:
		return tlast;
	}
	
}


