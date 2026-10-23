/*                                                                                                             
Copyright 2004-2006 David P. Little                                                                            
license:                                                                                                       
Unless otherwise stated, the above applets were written by David Little.                                       
They may be used without permission from the author for home and/or educational (non-profit) purposes only.    
Any other use must be approved by the author.                                                                  
                                                                                                               
Modified for wims interactive usage with permission of the author.                                             
                                                                                                               
J.M. Evers                                                                                                     
*/
import java.awt.Color;
import javax.swing.JSpinner;

public class PlinkoBall
{

	public PlinkoBall()
	{
		sound = false;
		spaz = (int)((double)PlinkoBoard.COLORS.length * Math.random());
		color = PlinkoBoard.COLORS[spaz];
		nextBall = null;
		previousBall = null;
		ROW = 0;
		COL = 0;
		DIR = 0;
		t = 0;
		C = 11 - ((Integer)PlinkoNegativeBinomial.rate.getValue()).intValue();
	}

	static final double A = -2D - Math.sqrt(3D);
	static final double B = 1.0D + Math.sqrt(3D);
	Color color;
	PlinkoBall nextBall;
	PlinkoBall previousBall;
	double X;
	double Y;
	int ROW;
	int COL;
	int DIR;
	int C;
	int t;
	int spaz;
	boolean sound;

}
