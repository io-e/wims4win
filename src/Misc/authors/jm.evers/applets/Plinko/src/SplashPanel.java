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
import java.awt.geom.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;

public class SplashPanel extends JPanel implements MouseListener,Runnable{
	Plinko plinko;
	double w;
	double h;	
	int TOTAL;					// total number of balls that have been dropped
	int[] HIST;					// number of balls in each bin;
	double MAX;					// maximum number of balls in a single bin
	Point[][] PINS;				// coordinates of pins (including one at the base of each bin)
	double DIST;				// vertical distance between pins
	double BALL_RAD;			// radius of ball
	int PIN_RAD;				// radius of pin
	int BINS = 20;				// total number of bins
	PlinkoBall FIRST_BALL;	    // represents beginning of doubly linked list of plinko balls
	int BALL_COUNT;
	int BOTTOM_MARGIN = 5;
//	private static byte[] data;
	private final static int hx = 15;
	private final static int hy = 15;
	private final static int bgGrey = 192;
	Image[] IMAGES;
	boolean newBackground = true;
	boolean active = true;
	Thread thread;
	public SplashPanel( Plinko plinko ){
		setBackground( Color.white );
		addMouseListener( this );	
		this.plinko = plinko;
		for ( int i = 0; i<25; i++ ){
			dropBall();
			FIRST_BALL.ROW = 10+(int)(10*Math.random());
			FIRST_BALL.COL = (int)(FIRST_BALL.ROW*Math.random());
			FIRST_BALL.C = 10;
			FIRST_BALL.t = i%11;
		}
		start();
	}

	public void start(){
		active = true;
		thread = new Thread(this);
		thread.start();
	}


	public void run(){
	    while ( active ){
		if ( Math.random() < 0.4 ){
		    dropBall();
		    FIRST_BALL.ROW = 11;
		    FIRST_BALL.COL = (int)(FIRST_BALL.ROW*Math.random());
		    FIRST_BALL.C = 10;
		}
		repaint();
		try {Thread.sleep(100);} catch (InterruptedException e){}
	    }
	}


	public void dropBall(){
	    if ( FIRST_BALL == null ){
			FIRST_BALL = new PlinkoBall();
	    } else {
			FIRST_BALL.previousBall = new PlinkoBall();
			FIRST_BALL.previousBall.nextBall = FIRST_BALL;
			FIRST_BALL = FIRST_BALL.previousBall;
	    }
    }


    //Image backImage;
	Image background;
	Graphics2D backGraphics;
	int FUDGE = 200;

    public void setup(){
		if(plinko.wims_exercise == true){ FUDGE = 100;}
		if ( h-BOTTOM_MARGIN<h/2 ){
			DIST = (double)(h-BOTTOM_MARGIN)/BINS;
		} else {
			DIST = (double)(w-10)/(2*BINS);
		}

		PIN_RAD = (int)DIST/9 + 1;
		BALL_RAD = Math.max(2*DIST/7,2.0) + 1;

		// create images of colored balls
		IMAGES = new Image[ PlinkoBoard.COLORS.length ];
		Graphics2D g;
		int red;
		int green;
		int blue;
		for ( int i=0; i<PlinkoBoard.COLORS.length; i++ ){
			IMAGES[i] = PlinkoBoard.getBall( BALL_RAD, PlinkoBoard.COLORS[i] );
		}

		PINS = new Point[BINS][];
		for (int i=0; i<BINS; i++){
			PINS[i] = new Point[i+1];
			for (int j=0; j<=i; j++){
				PINS[i][j]= new Point((int)(DIST*(2*j-i)+w/2)-FUDGE/2,(int)(DIST*(i+1))-(FUDGE-15));
			}
		}		
		
		// pins at the base of each bin
		for (int i=0; i<BINS; i++){
			PINS[BINS-1][i]= new Point((int)(DIST*(2*i-BINS+1)+w/2),(int)(h-30-BALL_RAD));
		}
    }


	public void paintComponent( Graphics graphics ){
		// have a copy of the background on which to draw
		w = getWidth();
		h = getHeight();
		
		w = Math.max( getWidth(),getHeight() ) + FUDGE;
		h = w;

		Graphics2D g = (Graphics2D)graphics;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

		if ( background == null || background.getWidth(this) != h || background.getHeight(this) != w ){
			setup();
			background = createImage( (int)w, (int)h );
			drawBackground((int)w,(int)h);
		}

		g.drawImage( background, 0, 0, this );

		// run through all active balls and draw them on the image
		PlinkoBall ball = FIRST_BALL;

		int red;
		int green;
		int blue;
		while ( ball != null ){
			graphics.drawImage( IMAGES[ball.spaz], (int)(ball.X - BALL_RAD),  (int)(ball.Y - 2*BALL_RAD - PIN_RAD + 1), this );
			if ( active ) increment( ball );
			ball = ball.nextBall;
		}
		//g.drawImage(image,0,0,this);

		w = getWidth();
		h = getHeight();
		g.setColor( new Color( 128,128,128,220 ) );
		g.setFont( new Font("Helvetica",Font.BOLD,plinko.font_size) );
		//String str = "CLICK ANYWHERE TO BEGIN";
		String str = plinko.click_text;
		
		g.drawString( str, (int)(w-g.getFontMetrics().stringWidth(str)-5), (int)(h-5) );
	}


    public void increment( PlinkoBall ball ){
		// if ball has landed on pin, reset t to 0 and pick a direction
		if ( ball.t == ball.C && ball.ROW < BINS-2 ){
			ball.ROW++;
			ball.COL += ball.DIR;
			ball.t = 0;
			ball.DIR = 0;
			if ( Math.random() < 0.5 ) ball.DIR = 1;

			ball.C = 10;
			
			//if ( C>1 ) C += spaz;
		}

		double dx = DIST*ball.t*(2*ball.DIR - 1)/ball.C;

		if ( ball.ROW < 0 ){ // ball falling onto top pin
			ball.X = PINS[0][0].x;
			dx = Math.abs(dx);
			ball.Y = PINS[0][0].y - DIST + DIST*ball.t*ball.t/(ball.C*ball.C);
		} else if ( ball.ROW < BINS-2 ) {
			ball.X = PINS[ball.ROW][ball.COL].x + dx;
			dx = Math.abs(dx);
			//Y = PlinkoBoard.PINS[ROW][COL].y - (int)(dx*(A*dx/PlinkoBoard.DIST+B));
			ball.Y = PINS[ball.ROW][ball.COL].y - DIST*PlinkoBoard.DYS[ball.C][ball.t];
		} else { // ball falling into bin
			ball.X = PINS[ball.ROW][ball.COL].x + dx;
			if ( dx>0 ){ 
				ball.X = Math.min(ball.X,PINS[ball.ROW][ball.COL].x + 2*DIST - BALL_RAD );
			} else {
				ball.X = Math.max(ball.X,PINS[ball.ROW][ball.COL].x - 2*DIST + BALL_RAD + 1);
			}
			dx = Math.abs(dx);
			ball.Y = PINS[ball.ROW][ball.COL].y  - dx*(ball.A*dx/DIST+ball.B);
		}

		ball.t++;

		if ( ball.Y > h - BOTTOM_MARGIN - PIN_RAD ){
			if ( ball.previousBall != null && ball.nextBall != null){
				ball.previousBall.nextBall = ball.nextBall;
				ball.nextBall.previousBall = ball.previousBall;
			} else if ( ball.previousBall != null && ball.nextBall == null ) {
				ball.previousBall.nextBall = null;
			} else if ( ball.previousBall == null && ball.nextBall != null ) {
				ball.nextBall.previousBall = null;
				FIRST_BALL = ball.nextBall;
			} else  {
				FIRST_BALL = null;
				//plinko.bins.setEnabled( true );
			}

			BALL_COUNT--;
		}
    }
	
	
    public void drawBackground( int W, int H ){
		Point p;

		Graphics2D backgroundgraphics = (Graphics2D) background.getGraphics();
		backgroundgraphics.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
		backgroundgraphics.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON );
		backgroundgraphics.setColor(Color.white);
		backgroundgraphics.fillRect( 0, 0, W, H );
		backgroundgraphics.setColor(Color.black);
		Image img = PlinkoBoard.getBall( PIN_RAD, Color.black );

		// draw pins
		for (int i=0; i<BINS-1; i++){
			for (int j=0;j<=i;j++){
				p = PINS[i][j];
				//backgroundgraphics.fillOval(p.x-PIN_RAD,p.y-PIN_RAD,2*PIN_RAD,2*PIN_RAD);
			backgroundgraphics.drawImage(img, (int)(p.x-PIN_RAD),(int)(p.y-PIN_RAD),this );
			}
		}
		repaint();
    }


	public void mouseClicked( MouseEvent me ){
		active = false;
		plinko.showFrame();
	}
	
	
	public void mouseEntered( MouseEvent me ){
	}


	public void mouseExited( MouseEvent me ){
	}


	public void mousePressed( MouseEvent me ){
	}


	public void mouseReleased(MouseEvent me){
	}

	
}