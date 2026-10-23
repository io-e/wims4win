/*                                                                                                             
Copyright 2004-2006 David P. Little                                                                            
license:                                                                                                       
Unless otherwise stated, the above applets were written by David Little.                                       
They may be used without permission from the author for home and/or educational (non-profit) purposes only.    
Any other use must be approved by the author.                                                                  
                                                                                                               
Modified for wims interactive usage with permission of the author.                                             
                                                                                                               
J.M. Evers                                                                                                     
*/ 
import java.awt.*;
import java.util.*;


public class PlinkoBall{
    final static double A = -2.0 - Math.sqrt(3);	// Ball follows the motion of
    final static double B = 1.0 + Math.sqrt(3);		// y = Ax^2 + Bx
    Color color;					// color of ball, choosen randomly from above list
    PlinkoBall nextBall;			// link to next ball in list
    PlinkoBall previousBall;		// link to previous ball in list
    double X;						// x-coordinate of location of ball
    double Y;						// y-coordinate of location of ball
    int ROW;
    int COL;
    int DIR;
    int C;										
    int t;							// ranges from 0 to (10-rate), parametrizes the motion of ball
    int spaz;						// 
    //double dx;
    boolean sound = false;

    public PlinkoBall( ){
		// pick a random color for the ball
		spaz = (int)( PlinkoBoard.COLORS.length*Math.random() );
		color = PlinkoBoard.COLORS[spaz];

		nextBall = null;
		previousBall = null;

		ROW = -1;
		COL = 0;
		DIR = 0;
		t = 0;
		C = 11-((Integer)Plinko.rate.getValue()).intValue();
    }
}