/*                                                                                                             
Copyright 2004-2006 David P. Little                                                                            
license:                                                                                                       
Unless otherwise stated, the above applets were written by David Little.                                       
They may be used without permission from the author for home and/or educational (non-profit) purposes only.    
Any other use must be approved by the author.                                                                  
                                                                                                               
Modified for wims interactive usage with permission of the author.                                             
                                                                                                               
J.M. Evers                                                                                                     
*/
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.geom.*;
import javax.swing.*;

public class PlinkoBoard extends JPanel implements Runnable, KeyListener, MouseListener, MouseMotionListener{

	double COUNT;					// number of balls that have been dropped
	double TOTAL;					// total of bins
	double TOTAL_SQUARES;			// total of bins
	
	long[] HIST;					// number of balls in each bin;
	float MAX;						// maximum number of balls in a single bin
	double[][][] PINS;					// coordinates of pins (including one at the base of each bin)
	double DIST;					// vertical distance between pins
	double BALL_RAD;				// radius of ball
	int PIN_RAD;					// radius of pin
	int BINS;						// total number of bins
	int W;							// width of board
	int H;							// height of board
	PlinkoBall FIRST_BALL;			// represents beginning of doubly linked list of plinko balls
	int BALL_COUNT;
	int BOTTOM_MARGIN = 5;
	
	// used in calculating confidence intervals
	int LEFT;
	int RIGHT;
	double PERCENT;
	int CURRENT_BIN = 0;
	int showstats=0;
	static double[][] DYS;
	// jm.evers: defining a few things
	int maximum_balls;String start_number="0";int incr=1;
	int BIN_HEIGHT;				// height of bins ****
	
	static Color[] COLORS = { Color.red, Color.magenta, Color.orange, Color.yellow, Color.green, Color.blue, Color.cyan };
	/*  new Color(.45f,.40f,.86f), new Color(.36f,.53f,.95f),
	    new Color(.38f,.84f,.67f), new Color(.37f,.74f,.44f), new Color(.49f,.51f,.36f), new Color(.90f,.90f,.35f),
	    new Color(.99f,.75f,.34f), new Color(.85f,.27f,.42f), new Color(.73f,.34f,.76f), new Color(.51f,.33f,.82f),
	    new Color(.41f,.46f,.91f), new Color(.36f,.73f,.79f), new Color(.38f,.79f,.56f), new Color(.44f,.60f,.37f),
	    new Color(.70f,.71f,.35f), new Color(.99f,.91f,.34f), new Color(.99f,.58f,.34f), new Color(.94f,.38f,.34f), 
	    new Color(.86f,.33f,.68f), new Color(.35f,.09f,.73f), new Color(.09f,.13f,.80f), new Color(.09f,.38f,.35f), 
	    new Color(.18f,.55f,.13f), new Color(.55f,.74f,.11f), new Color(.99f,.92f,.09f), new Color(.99f,.69f,.09f), 
	    new Color(.99f,.46f,.09f), new Color(.96f,.25f,.11f), new Color(.93f,.09f,.12f), new Color(.42f,.09f,.69f), 
	    new Color(.27f,.09f,.78f), new Color(.09f,.29f,.51f), new Color(.09f,.46f,.20f), new Color(.36f,.64f,.12f), 
	    new Color(.73f,.83f,.10f), new Color(.99f,.80f,.09f), new Color(.99f,.57f,.09f), new Color(.98f,.31f,.10f), 
	    new Color(.94f,.12f,.11f), new Color(.16f,.10f,.06f), new Color(.36f,.24f,.14f), new Color(.55f,.38f,.21f), 
	    new Color(.75f,.52f,.29f), new Color(.80f,.78f,.76f), new Color(.55f,.50f,.45f),  new Color(.24f,.22f,.20f), 
	    new Color(.76f,.76f,.80f), new Color(.45f,.45f,.55f), new Color(.20f,.20f,.24f)};*/
	
	//BufferedImage[] IMAGES;
    Image[] IMAGES;
    Thread thread;
    boolean active;	
    Image background;
    Image image;
    Graphics2D graphics;
    Plinko plinko;
    
    public PlinkoBoard( Plinko plinko ){
	super();
	this.plinko = plinko;
	setup();
	newHist();

	FIRST_BALL = null;
	BALL_COUNT = 0;
	active = false;

	DYS = new double[12][];

	for ( double i=0.0; i<12.0; i++ ){
	    DYS[(int)i] = new double[ (int)i ];
	    for ( double j=0.0; j<i; j++ ){
		DYS[(int)i][(int)j] = PlinkoBall.A*j*j/(i*i) + PlinkoBall.B*j/i;
	    }
	}

	addKeyListener( this );
	addMouseListener( this );
	// jm.evers: if the applet is in an wims exercise...read appletparam and start buckets_number with 1 instead of 0
	maximum_balls=(int)plinko.total_balls - 1;
	BIN_HEIGHT=(int)plinko.bin_height ; 
	if(plinko.wims_exercise == false){ showstats = 1;}else{ start_number="1"; incr=2; }
    }

    public void setup(){
		W = getWidth();
		H = getHeight();

		BINS = ((Integer)plinko.bins.getValue()).intValue();
		CURRENT_BIN = BINS/2;
		LEFT = BINS+1;
		RIGHT = BINS;
		PERCENT = 0;

		if ( H-BIN_HEIGHT-BOTTOM_MARGIN<W/2 ){
			DIST = (double)(H-BIN_HEIGHT-BOTTOM_MARGIN)/BINS;
		} else {
			DIST = (double)(W-10)/(2*BINS);
		}

		PIN_RAD = (int)DIST/9 + 1;
		BALL_RAD = Math.max(2*DIST/7,2.0)+1;
		
		// create images of colored balls
		IMAGES = new Image[ COLORS.length ];
		Graphics2D g;
		int red;
		int green;
		int blue;
		for ( int i=0; i<COLORS.length; i++ ){
			IMAGES[i] = getBall( BALL_RAD, COLORS[i] );
			/*
			IMAGES[i] = new BufferedImage( (int)(2*BALL_RAD+2), (int)(2*BALL_RAD+2), BufferedImage.TYPE_INT_ARGB);
			g = (Graphics2D)(IMAGES[i].getGraphics());
			g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
			g.setBackground( new Color(0,0,0,255) );

			red = COLORS[i].getRed();
			green = COLORS[i].getGreen();
			blue = COLORS[i].getBlue();
			
			for ( float k=0; k<1; k=k+(float)(1/BALL_RAD) ){
				g.setColor( new Color(red + (int)(k*k*(255-red)),green + (int)(k*k*(255-green)),blue + (int)(k*k*(255-blue))) );
				g.fill( new Ellipse2D.Double( k*k*BALL_RAD/2, k*k*BALL_RAD/2, 2*(BALL_RAD-k*k*BALL_RAD), 2*(BALL_RAD-k*k*BALL_RAD) ) );
			}
			*/
		}
		

		PINS = new double[BINS][][];
		for (int i=0; i<BINS; i++){
			PINS[i] = new double[i+1][2];
			for (int j=0; j<=i; j++){
				//PINS[i][j] = new Point((int)(DIST*(2*j-i)+W/2),(int)(DIST*(i+1)));
				PINS[i][j][0] = DIST*(2*j-i)+W/2;
				PINS[i][j][1] = DIST*(i+1);
			}
		}		
		
		// pins at the base of each bin
		for (int i=0; i<BINS; i++){
			//PINS[BINS-1][i] = new Point((int)(DIST*(2*i-BINS+1)+W/2),(int)(H-30-BALL_RAD));
			PINS[BINS-1][i][0] = DIST*(2*i-BINS+1)+W/2;
			PINS[BINS-1][i][1] = H-30-BALL_RAD;
		}
	}
	
	
	public static Image getBall( double R, Color color ){
		BufferedImage image = new BufferedImage( (int)(2*R+2), (int)(2*R+2), BufferedImage.TYPE_INT_ARGB);
						
		int red;
		int grn;
		int blu;
		double[] n = new double[3];
		double[] e = new double[3];
		double[] l = {-5,5,10};
		double ll = Math.sqrt(150);
		double F;
		double G;

		for ( int i=0; i<(int)(2*R+2); i++ ){
			for ( int j=0; j<(int)(2*R+2); j++ ){
				if ( (R-i)*(R-i) + (R-j)*(R-j) <= (R-1)*(R-1) ){		
					// vector normal to sphere
					n[0] = (i - R)/R;
					n[1] = (R - j)/R;
					n[2] = Math.sqrt(1 - n[0]*n[0] - n[1]*n[1]);
					
					F = (n[0]*l[0] + n[1]*l[1] + n[2]*l[2])/Math.sqrt(l[0]*l[0] + l[1]*l[1] + l[2]*l[2]);
					F = (1+F)/2;
					G = Math.pow(F,20.0);

					red = (int)(color.getRed()*(F-G) + G*255);
					grn = (int)(color.getGreen()*(F-G) + G*255);
					blu = (int)(color.getBlue()*(F-G) + G*255);

					image.setRGB(i,j, (255<<24) + (red<<16) + (grn<<8) + blu );
				} else if ( (R-i)*(R-i) + (R-j)*(R-j) <= (R+1)*(R+1) ){
					red = 0;
					grn = 0;
					blu = 0;
					
					for ( double di = -0.33; di<0.5; di+=0.33 ){
						for ( double dj = -0.33; dj<0.5; dj+=0.33 ){
		
							if ( (R-(i+di))*(R-(i+di)) + (R-(j+dj))*(R-(j+dj)) <= R*R ){
								n[0] = (i + dj - R)/R;
								n[1] = (R - (j+dj))/R;
								n[2] = Math.sqrt(1 - n[0]*n[0] - n[1]*n[1]);

								F = (n[0]*l[0] + n[1]*l[1] + n[2]*l[2])/ll;
								F = (1+F)/2;
								G = Math.pow(F,20.0);
							} else {
								F = 1;
								G = 1;
							}
							red += (int)(color.getRed()*(F-G) + G*255);
							grn += (int)(color.getGreen()*(F-G) + G*255);
							blu += (int)(color.getBlue()*(F-G) + G*255);

						}
					}
	
					red /= 9;
					grn /= 9;
					blu /= 9;
					
					image.setRGB(i,j, ((255-(red+grn+blu)/3)<<24) + (red<<16) + (grn<<8) + blu );
				}
			}
		}
		return image;
	}
	
	
    public void newHist(){
		COUNT = 0;
		TOTAL = 0;
		TOTAL_SQUARES = 0;
		MAX = 0;
		PERCENT = 0;

		HIST = new long[ BINS ];
		for (int i=0; i<BINS; i++){
			HIST[i] = 0;
		}
    }

    // jm.evers :preparing a data string for javascript: must be of type string: arrays always give trouble on IE...
    public String ReadData(){
	String reply="";
	for(int i=0;i<HIST.length;i++){
	    if(i != 0){reply=reply+","+HIST[i];}else{reply=""+HIST[0];}
	}
	return reply;
    }
    
    public void LimitReached(){
	    plinko.active = true;
	    FIRST_BALL=null;
	    plinko.toggleStart();
    }

    public void run(){
	while ( BALL_COUNT>0 ){
	    repaint();
	    try {Thread.sleep(50);} catch (InterruptedException e){}
	}
	repaint();
	active = false;
	plinko.active = false;
	plinko.bins.setEnabled( true );
    }


    // under construction
    public void kill(){
		FIRST_BALL = null;
		if ( plinko.start.getText().equals(plinko.start_text) ){
			active = false; 
			plinko.active = false;
			plinko.bins.setEnabled( true );
		}
    }


    public void dropBall( boolean sound ){
	BALL_COUNT++;
	// jm.evers : I don't know of another/better way to get the system to stop at a given [param] number of balls...
	if(COUNT >= maximum_balls ){LimitReached();}
	if ( FIRST_BALL == null ){
	    FIRST_BALL = new PlinkoBall();
	    FIRST_BALL.sound = sound;
	} else {
	    FIRST_BALL.previousBall = new PlinkoBall();
	    FIRST_BALL.previousBall.sound = sound;
	    FIRST_BALL.previousBall.nextBall = FIRST_BALL;
	    FIRST_BALL = FIRST_BALL.previousBall;
	}

	try { Thread.sleep(0);} catch (InterruptedException e){}
	    
    	if ( !active ){
	    active = true;
	    thread = new Thread(this);
	    thread.start();
	}
    }

    public void paintComponent( Graphics g ){
		// have a copy of the background on which to draw
		W = getWidth();
		H = getHeight();

		if ( background == null || background.getWidth(this) != W || background.getHeight(this) != H || image == null || image.getWidth(this) != W || image.getHeight(this) != H ){
			setup();
			background = createImage( W, H );
			drawBackground(W,H);
			image = createImage( W, H );
			graphics = (Graphics2D) image.getGraphics();
			graphics.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
			graphics.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON );
		}

		graphics.drawImage( background, 0, 0, this );

		// run through all active balls and draw them on the image
		PlinkoBall ball = FIRST_BALL;
		while ( ball != null ){
			graphics.drawImage( IMAGES[ball.spaz], (int)(ball.X - BALL_RAD),  (int)(ball.Y - 2*BALL_RAD - PIN_RAD + 1), this );
			//if ( ball.t == ball.C && ball.ROW < BINS-2 ) Plinko.click.play();
			//try {
				increment( ball );
			//} catch ( NullPointerException npe ) {
			//}
			ball = ball.nextBall;
		}
		drawHist( graphics );
	//	if(showstats==1){
		    drawStats( graphics );
	//	}
		g.drawImage(image,0,0,this);
    }
	
    // under construction
    public void drawNormal(){
    }
	
	
    public void increment( PlinkoBall ball ){
		// if ball has landed on pin, reset t to 0 and pick a direction
		if ( ball.t == ball.C && ball.ROW < BINS-2 ){
			ball.ROW++;
			ball.COL += ball.DIR;
			ball.t = 0;
			ball.DIR = 0;
			
			// pick a new direction
			
			// jm.evers: reading controls or applet param...plinko.chance is parameter
			if(plinko.wims_exercise == false){
			    if ( Math.random() < ((Double)plinko.prob.getValue()).doubleValue() ) ball.DIR = 1;
			}
			else
			{
			    if ( Math.random() <  plinko.chance ){ ball.DIR = 1;}
			}

			// update speed
			ball.C = 11-((Integer)plinko.rate.getValue()).intValue();
			
			if ( ball.sound ) Plinko.ping.play();
		}

		double dx = (double)(DIST*ball.t*(2*ball.DIR - 1))/(double)(ball.C);

		if ( ball.ROW < 0 ){ 
			// ball falling onto top pin
			ball.X = PINS[0][0][0];
			dx = Math.abs(dx);
			ball.Y = PINS[0][0][1] - DIST + DIST*ball.t*ball.t/(ball.C*ball.C);
		} else if ( ball.ROW < BINS-2 ) {
			ball.X = PINS[ball.ROW][ball.COL][0] + dx;
			dx = Math.abs(dx);
			ball.Y = PINS[ball.ROW][ball.COL][1] - DIST*DYS[ball.C][ball.t];
		} else { 
			// ball falling into bin
			// dx = DIST*ball.t*(2*ball.DIR - 1)/10.0;
			ball.X = PINS[ball.ROW][ball.COL][0] + dx;
			if ( dx>0 ){ 
				ball.X = Math.min(ball.X,PINS[ball.ROW][ball.COL][0] + 2*DIST - BALL_RAD );
			} else {
				ball.X = Math.max(ball.X,PINS[ball.ROW][ball.COL][0] - 2*DIST + BALL_RAD + 1);
			}
			dx = Math.abs(dx);
			ball.Y = PINS[ball.ROW][ball.COL][1]  - dx*(ball.A*dx/DIST+ball.B);
		}

		ball.t++;

		if ( ball.Y > H - BOTTOM_MARGIN - PIN_RAD ){
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
				plinko.bins.setEnabled( true );
			}

			BALL_COUNT--;
			updateHist(ball.COL+ball.DIR);
			if ( ball.sound ) Plinko.click.play();
		}
    }
	
		
    public void updateHist( int i ){
		HIST[i]++;
		COUNT++;
		TOTAL += i;
		TOTAL_SQUARES += i*i;
		if ( HIST[i] > MAX) MAX = HIST[i];
		if ( i>= LEFT && i <= RIGHT ){
			PERCENT++;
		}
    }
	

    public void drawStats(Graphics g){
    //jm.evers : if in a wims_exercise we show a limited amount of statistical data...
	if(showstats == 1){
    	    plinko.count.setText(plinko.label_count+ (int)COUNT);
	    plinko.current_bin.setText(plinko.label_bin + CURRENT_BIN);
	    plinko.current_bin_count.setText(plinko.label_bin_count + HIST[CURRENT_BIN]);
	    if(COUNT > 0.0D){
		double d = TOTAL / COUNT;
	        plinko.mean.setText(plinko.label_mean + (float)d);
	        plinko.variance.setText(plinko.label_variance + (float)(TOTAL_SQUARES / COUNT - d * d));
		plinko.current_bin_prob.setText(plinko.label_bin_probability + (float)((double)(int)((double)(0x186a0L * HIST[CURRENT_BIN]) / COUNT) / 1000D) + "%");
		if(LEFT < BINS){
		    if(LEFT == RIGHT){
			plinko.confidence.setText(plinko.label_confidence + (float)((double)(int)((100000D * PERCENT) / COUNT) / 1000D) + plinko.some_text + LEFT + ".");
		    } 
		    else 
		    {
			plinko.confidence.setText(plinko.label_confidence + (float)((double)(int)((100000D * PERCENT) / COUNT) / 1000D) + plinko.some_text2 + LEFT + plinko.through + RIGHT + ".");
		    }
		} 
		else
		{
		    plinko.confidence.setText(plinko.label_confidence );
		}
	    } 
	    else
	    {
		plinko.mean.setText(plinko.label_mean );
	        plinko.variance.setText(plinko.label_variance);
	        plinko.current_bin_prob.setText(plinko.label_bin_probability);
	        plinko.confidence.setText(plinko.label_confidence);
	    }
	}
	else
	{
	// just the total COUNT and "balls per active bin" 
    	    plinko.count.setText(plinko.label_count+ (int)COUNT);
	    plinko.current_bin.setText(plinko.label_bin + (CURRENT_BIN+1));
	    plinko.current_bin_count.setText(plinko.label_bin_count + HIST[CURRENT_BIN]);
	}
    }
	

    public void drawHist( Graphics2D g ){
		long h;
		double x0;
		double x1;
		String str;
		W = getWidth();
		H = getHeight();
		Rectangle2D rect;

		for (int i=0; i<BINS; i++){
			if ( i==0 ){
				x0 = PINS[BINS-1][0][0]-DIST;
				x1 = PINS[BINS-2][0][0];
			} else if ( i==BINS-1 ){
				x0 = PINS[BINS-2][BINS-2][0];
				x1 = PINS[BINS-1][BINS-1][0]+DIST;
			} else {
				x0 = PINS[BINS-2][i-1][0];
				x1 = PINS[BINS-2][i][0];
			}

			//Draw bar
			h = HIST[i];
			if (MAX>BIN_HEIGHT) h=(int)(BIN_HEIGHT*1.0*h/MAX);
			g.setColor( new Color(255,0,0,175) );
			if ( i >= LEFT && i <= RIGHT ) g.setColor( new Color(0,255,0,175) );
			rect = new Rectangle2D.Double(x0,H-h-BOTTOM_MARGIN,x1-x0,h);
			g.fill( rect );
			g.setColor(Color.black);
			g.setStroke( new BasicStroke(1.0f) );
			g.draw( rect );
		}
	
		// draw line under current bin
		g.setColor( Color.black );
		g.setStroke( new BasicStroke(3.0f) );
		g.draw( new Line2D.Double(PINS[BINS-1][CURRENT_BIN][0]-DIST,H-BOTTOM_MARGIN+3,PINS[BINS-1][CURRENT_BIN][0]+DIST,H-BOTTOM_MARGIN+3) );
    }
	
	
    public void drawBackground(){
		drawBackground( getWidth(), getHeight() );
    }


    public void drawBackground( int W, int H ){
		double[] p;

		Graphics2D backgroundgraphics = (Graphics2D) background.getGraphics();
		backgroundgraphics.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
		backgroundgraphics.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON );
		backgroundgraphics.setColor(Color.white);
		backgroundgraphics.fillRect( 0, 0, W, H );
		backgroundgraphics.setColor(Color.black);

		Image img = getBall( PIN_RAD, Color.black );

		// draw pins
		for (int i=0; i<BINS-1; i++){
			for (int j=0;j<=i;j++){
			p = PINS[i][j];
			//backgroundgraphics.fill( new Ellipse2D.Double(p[0]-PIN_RAD,p[1]-PIN_RAD,2*PIN_RAD,2*PIN_RAD) );
			backgroundgraphics.drawImage(img, (int)(p[0]-PIN_RAD),(int)(p[1]-PIN_RAD),this );
			}
		}

		String s;
		backgroundgraphics.setFont( new Font("Helvetica",Font.BOLD,Math.min((int)(4*DIST)/3,BIN_HEIGHT/2)) );
		FontMetrics fm = backgroundgraphics.getFontMetrics();
		// draw lines and numbers
		p = PINS[BINS-1][0];
		backgroundgraphics.draw( new Line2D.Double( p[0]-DIST,H-BIN_HEIGHT-BOTTOM_MARGIN,p[0]-DIST,H-BOTTOM_MARGIN) );
		backgroundgraphics.setColor( Color.darkGray );
		backgroundgraphics.drawString(start_number,(int)(p[0] - fm.stringWidth("0")/2), H-BIN_HEIGHT-BOTTOM_MARGIN+Math.min((int)(4*DIST)/3, BIN_HEIGHT/2) );
		for (int i=0; i<BINS-1; i++){
			p = PINS[BINS-2][i];
			s = ""+(i+incr);
			backgroundgraphics.setColor( Color.darkGray );
			backgroundgraphics.drawString(s,(int)(p[0] + DIST - fm.stringWidth(s)/2),H-BIN_HEIGHT-BOTTOM_MARGIN+Math.min((int)(4*DIST)/3,BIN_HEIGHT/2));
			backgroundgraphics.setColor( Color.black );
			backgroundgraphics.draw( new Line2D.Double( p[0],H-BIN_HEIGHT-BOTTOM_MARGIN,p[0],H-BOTTOM_MARGIN) );
		}
		p = PINS[BINS-1][BINS-1];
		backgroundgraphics.draw( new Line2D.Double(p[0]+DIST,H-BIN_HEIGHT-BOTTOM_MARGIN,p[0]+DIST,H-BOTTOM_MARGIN) );
		backgroundgraphics.draw( new Line2D.Double(0,H-BOTTOM_MARGIN,W,H-BOTTOM_MARGIN) );
		repaint();
    }


    public void keyTyped( KeyEvent ke ){
    }
	
	
    public void keyPressed( KeyEvent ke ){
		int code = ke.getKeyCode();
		char key = ke.getKeyChar();

		if ( key >= '0' && key <= '4' ){
			int x = key - '0';
			int n = 1;
			for (int i=0; i<x; i++ ){
				n *= 10;
			}/*
			plinko.countdown = n;
			if ( !plinko.active ){
				plinko.start();
				plinko.bins.setEnabled( false );
				plinko.thread = new Thread(plinko);
				plinko.thread.start();
				//start.setText("Stop");
			}*/
			for (int i=0; i<n; i++){
				dropBall( n == 1 );
			}
		} else if ( code == 37 ){  // left arrow
			CURRENT_BIN--;
			if ( CURRENT_BIN<0 ) CURRENT_BIN = BINS - 1;
			updatePercent();
			repaint();
		} else if ( code == 39 ){  // right arrow
			CURRENT_BIN++;
			if ( CURRENT_BIN>BINS-1 ) CURRENT_BIN = 0;
			updatePercent();
			repaint();
		} else if ( code == KeyEvent.VK_ENTER ){ // return or enter
		} else if ( key == ' ' ){
			plinko.toggleStart();
		} else if (key==20){ //control t - toggle erasing
		} else if (key==3){ //control c - clear screen
		} else if (key==24){ //control x - kill all threads
		}
    }


    public void keyReleased( KeyEvent ke ){}

    public void mouseReleased( MouseEvent me ){}
    
    public void mouseEntered( MouseEvent me ){}

    public void mouseExited( MouseEvent me ){}
		
    public void updatePercent(){
	double mean = (BINS-1)*((Double)plinko.prob.getValue()).doubleValue();
	if ( CURRENT_BIN <= mean ){
	    LEFT = CURRENT_BIN;
	    RIGHT = (int)(2*mean) - LEFT;
	} else {
	    RIGHT = CURRENT_BIN;
	    LEFT = (int)(2*mean) - RIGHT;
	}
	LEFT = Math.max( LEFT, 0 );
	RIGHT = Math.min( RIGHT, BINS-1 );
	PERCENT = 0;
	for ( int i=LEFT; i<=RIGHT; i++ ){
	    PERCENT += HIST[i];
	}
    }
	
	
    public void mouseClicked( MouseEvent me ){
	requestFocus();
	Point p = me.getPoint();
	int bin;
	if ( p.x + DIST> PINS[BINS-1][0][0] && p.x-DIST < PINS[BINS-1][BINS-1][0] && p.y> H-BIN_HEIGHT-BOTTOM_MARGIN && p.y<H-BOTTOM_MARGIN){
	    bin = 0; 
	    while ( bin<BINS-1 ){
		if ( p.x + DIST < PINS[BINS-1][bin+1][0] ) break;
	        bin++;
	    }
	    CURRENT_BIN = bin;
	    updatePercent();
	    repaint();
	}			
    }


    public void mousePressed( MouseEvent me ){}

    public void mouseDragged( MouseEvent me ){}
    
    public void mouseMoved( MouseEvent me ){}
}

