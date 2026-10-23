/*
 * Created on 18.07.2006
 *
 */
package rene.zirkel.expression;

public class Quartic 
{

	static public int solve (double c[], double sol[])
	{	double sum=0;
		for (int k=0; k<c.length; k++) sum+=Math.abs(c[k]);
		if (sum<1e-15)
		{	sol[0]=0; return 1;
		}
		for (int k=0; k<c.length; k++) c[k]/=sum;
		int n=4;
		while (Math.abs(c[n])<1e-15) n--;
		if (n==4)
		{	double soli[]=new double[4];
			return quartic(c,sol,soli);
		}
		else if (n==3)
		{	return cubic(c,sol);
		}
		else if (n==2)
		{	double a=-c[1]/(2*c[2]);
			double r=a*a-c[0]/c[2];
			if (r<-1e-10) return 0;
			else if (Math.abs(r)<1e-10)
			{	sol[0]=a; return 1;
			}
			else
			{	sol[0]=a+Math.sqrt(r);
				sol[1]=a-Math.sqrt(r);
				return 2;
			}
		}
		else if (n==1)
		{	sol[0]=-c[0]/c[1];
			return 1;
		}
		else
		{	if (Math.abs(c[0])<1e-10)
			{	sol[0]=0; return 1;
			}
		}
		return 0;
	}
	
	/*-------------------- Global Function Description Block ----------------------
	 *
	 *     ***QUARTIC************************************************25.03.98
	 *     Solution of a quartic equation
	 *     ref.: J. E. Hacke, Amer. Math. Monthly, Vol. 48, 327-328, (1941)
	 *     NO WARRANTY, ALWAYS TEST THIS SUBROUTINE AFTER DOWNLOADING
	 *     ******************************************************************
	 *     dd(0:4)     (i)  vector containing the polynomial coefficients
	 *     sol(1:4)    (o)  results, real part
	 *     soli(1:4)   (o)  results, imaginary part
	 *     Nsol        (o)  number of real solutions 
	 *     ==================================================================
	 *  	17-Oct-2004 / Raoul Rausch
	 *		Conversion from Fortran to C
	 *
	 *
	 *-----------------------------------------------------------------------------
	 */
	 static int quartic (double dd[], double sol[], double soli[])
	 {
		double AA[] = new double[4], z[] = new double[3];
		double a, b, c, d, f, p, q, r, zsol, xK2, xL, xK, sqp, sqm;
		int ncube, i;
		int Nsol = 0;

		if (dd[4] == 0.0)
		{	return 0;
		}

		a = dd[4];
		b = dd[3];
		c = dd[2];
		d = dd[1];
		f = dd[0];

		p = (-3.0*Math.pow(b,2) + 8.0 *a*c)/(8.0*Math.pow(a,2));
		q = (Math.pow(b,3) - 4.0*a*b*c + 8.0 *d*Math.pow(a,2)) / (8.0*Math.pow(a,3));
		r = (-3.0*Math.pow(b,4) + 16.0 *a*Math.pow(b,2)*c - 64.0 *Math.pow(a,2)*b*d + 256.0 *Math.pow(a,3)*f)
			/(256.0*Math.pow(a,4));
		
		// Solve cubic resolvent
		AA[3] = 8.0;
		AA[2] = -4.0*p;
		AA[1] = -8.0*r;
		AA[0] = 4.0*p*r - Math.pow(q,2);

		ncube = cubic(AA, z);
		
		zsol = - 1.e99;
		for (i=0;i<ncube;i++)	zsol = Math.max(zsol, z[i]);	//Not sure C has max fct
		z[0] =zsol;
		xK2 = 2.0*z[0] -p;
		xK = Math.sqrt(xK2);
		xL = q/(2.0*xK);
		sqp = xK2 - 4.0 * (z[0] + xL);
		sqm = xK2 - 4.0 * (z[0] - xL);

		for (i=0;i<4;i++)	soli[i] = 0.0;
		if ( (sqp >= 0.0) && (sqm >= 0.0))
		{
			sol[0] = 0.5 * (xK + Math.sqrt(sqp));
			sol[1] = 0.5 * (xK - Math.sqrt(sqp));
			sol[2] = 0.5 * (-xK + Math.sqrt(sqm));
			sol[3] = 0.5 * (-xK - Math.sqrt(sqm));
			Nsol = 4;
		}
		else if ( (sqp >= 0.0) && (sqm < 0.0))
		{
			sol[0] = 0.5 * (xK + Math.sqrt(sqp));
			sol[1] = 0.5 * (xK - Math.sqrt(sqp));
			sol[2] = -0.5 * xK;
			sol[3] = -0.5 * xK;
			soli[2] =  Math.sqrt(-.25 * sqm);
			soli[3] = -Math.sqrt(-.25 * sqm);
			Nsol = 2;
		}
		else if ( (sqp < 0.0) && (sqm >= 0.0))
		{
			sol[0] = 0.5 * (-xK + Math.sqrt(sqm));
			sol[1] = 0.5 * (-xK - Math.sqrt(sqm));
			sol[2] = 0.5 * xK;
			sol[3] = 0.5 * xK;
			soli[2] =  Math.sqrt(-0.25 * sqp);
			soli[3] = -Math.sqrt(-0.25 * sqp);
			Nsol = 2;
		}
		else if ( (sqp < 0.0) && (sqm < 0.0))
		{
			sol[0] = -0.5 * xK;
			sol[1] = -0.5 * xK;
			soli[0] =  Math.sqrt(-0.25 * sqm);
			soli[1] = -Math.sqrt(-0.25 * sqm);
			sol[2] = 0.5 * xK;
			sol[3] = 0.5 * xK;
			soli[2] =  Math.sqrt(-0.25 * sqp);
			soli[3] = -Math.sqrt(-0.25 * sqp);
			Nsol = 0;
		}
		
		for (i=0;i<4;i++)	sol[i] -= b/(4.0*a);
		return Nsol;

	 }
	 
	 /*-------------------- Global Function Description Block ----------------------
	  *
	  *     ***CUBIC************************************************08.11.1986
	  *     Solution of a cubic equation
	  *     Equations of lesser degree are solved by the appropriate formulas.
	  *     The solutions are arranged in ascending order.
	  *     NO WARRANTY, ALWAYS TEST THIS SUBROUTINE AFTER DOWNLOADING
	  *     ******************************************************************
	  *     A(0:3)      (i)  vector containing the polynomial coefficients
	  *     X(1:L)      (o)  results
	  *     L           (o)  number of valid solutions (beginning with X(1))
	  *     ==================================================================
	  *  	17-Oct-2004 / Raoul Rausch
	  *		Conversion from Fortran to C
	  *
	  *
	  *-----------------------------------------------------------------------------
	  */
	static public int cubic (double A[], double X[])
	{
		final double PI = 3.1415926535897932;
		final double THIRD = 1./3.;
		double U[]=new double[3], W, P, Q, DIS, PHI;
		int i;
		int L;

		if (A[3] != 0.0)
		{
			//cubic problem
			W = A[2]/A[3]*THIRD;
			P = Math.pow((A[1]/A[3]*THIRD - Math.pow(W,2)),3);
			Q = -.5*(2.0*Math.pow(W,3)-(A[1]*W-A[0])/A[3] );
			DIS = Math.pow(Q,2)+P;
			if ( DIS < 0.0 )
			{
				//three real solutions!
				//Confine the argument of ACOS to the interval [-1;1]!
				PHI = Math.acos(Math.min(1.0,Math.max(-1.0,Q/Math.sqrt(-P))));
				P=2.0*Math.pow((-P),(5.e-1*THIRD));
				for (i=0;i<3;i++)	U[i] = P*Math.cos((PHI+2*((double)i)*PI)*THIRD)-W;
				X[0] = Math.min(U[0], Math.min(U[1], U[2]));
				X[1] = Math.max(Math.min(U[0], U[1]),Math.max( Math.min(U[0], U[2]), Math.min(U[1], U[2])));
				X[2] = Math.max(U[0], Math.max(U[1], U[2]));
				L = 3;
			}
			else
			{
				// only one real solution!
				DIS = Math.sqrt(DIS);
				X[0] = CBRT(Q+DIS)+CBRT(Q-DIS)-W;
				L=1;
			}
		}
		else if (A[2] != 0.0)
		{
			// quadratic problem
			P = 0.5*A[1]/A[2];
			DIS = Math.pow(P,2)-A[0]/A[2];
			if (DIS > 0.0)
			{
				// 2 real solutions
				X[0] = -P - Math.sqrt(DIS);
				X[1] = -P + Math.sqrt(DIS);
				L=2;
			}
			else
			{
				// no real solution
				L=0;
			}
		}
		else if (A[1] != 0.0)
		{
			//linear equation
			X[0] =A[0]/A[1];
			L=1;
		}
		else
		{
			//no equation
			L=0;
		}
	 /*
	  *     ==== perform one step of a newton iteration in order to minimize
	  *          round-off errors ====
	  */
		for (i=0;i<L;i++)
		{
			X[i] = X[i] - (A[0]+X[i]*(A[1]+X[i]*(A[2]+X[i]*A[3])))/(A[1]+X[i]*(2.0*A[2]+X[i]*3.0*A[3]));
		}

		return L;
	}

	static double CBRT (double Z)
	{
		final double THIRD = 1./3.;
		if (Z>0) return Math.pow(Z,THIRD);
		else if (Z<0) return -Math.pow(Math.abs(Z),THIRD);
		else return 0;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{	double a[]={0,3,-3,-6,5},x[]=new double[5],y[]=new double[5];
		int n=quartic(a,x,y);
		System.out.println(n+" solutions!");
		for (int i=0; i<n; i++) System.out.println(x[i]);
	}

}
