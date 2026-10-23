/*                                                                                                             
Copyright 2004-2006 David P. Little                                                                            
license:                                                                                                       
Unless otherwise stated, the above applets were written by David Little.                                       
They may be used without permission from the author for home and/or educational (non-profit) purposes only.    
Any other use must be approved by the author.                                                                  
                                                                                                               
Modified for wims interactive usage with permission of the author.                                             
                                                                                                               
J.M. Evers                                                                                                     
*/
import java.applet.AudioClip;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.*;

public class PlinkoBoard extends JPanel implements Runnable, KeyListener, MouseListener, MouseMotionListener
{
	double COUNT;
	double TOTAL;
	double TOTAL_SQUARES;
	long HIST[];
	float MAX;
	double PINS[][][];
	double DIST;
	double BALL_RAD;
	int PIN_RAD;
	int BINS;
	int R;
	int W;
	int H;
	int BALL_COUNT;
	int BOTTOM_MARGIN;
	double SIDE_MARGIN;
	PlinkoBall FIRST_BALL;
	int RIGHT;
	double PERCENT;
	int CURRENT_BIN;
	static double DYS[][];
	static Color COLORS[];
	Image IMAGES[];
	Thread thread;
	boolean active;
	Image background;
	Image image;
	Graphics2D graphics;
	PlinkoNegativeBinomial plinko;

	static 
	{
		COLORS = (new Color[] {
			Color.red, Color.magenta, Color.orange, Color.yellow, Color.green, Color.blue, Color.cyan
		});
	}
	
	// jm.evers: defining a few things                                                                                                 
        int maximum_balls;int start_number=0;int showstats=0;

	public PlinkoBoard(PlinkoNegativeBinomial plinkonegativebinomial)
	{
		BOTTOM_MARGIN = 5;
		CURRENT_BIN = 0;
		plinko = plinkonegativebinomial;
		setup();
		newHist();
		FIRST_BALL = null;
		BALL_COUNT = 0;
		active = false;
		DYS = new double[12][];
		for(double d = 0.0D; d < 12D; d++)
		{
			DYS[(int)d] = new double[(int)d];
			for(double d1 = 0.0D; d1 < d; d1++)
			{
				DYS[(int)d][(int)d1] = (PlinkoBall.A * d1 * d1) / (d * d) + (PlinkoBall.B * d1) / d;
			}

		}

		addKeyListener(this);
		addMouseListener(this);
		// jm.evers: if the applet is in an wims exercise...read appletparam and start buckets_number with 1 instead of 0 
		maximum_balls=(int)plinko.total_balls - 1;
		if(plinko.wims_exercise == false){ showstats = 1;}else{ start_number=1; }
	}

	public void setup()
	{
		W = getWidth();
		H = getHeight();
		PlinkoNegativeBinomial _tmp = plinko;
		BINS = ((Integer)PlinkoNegativeBinomial.bins.getValue()).intValue();
		PlinkoNegativeBinomial _tmp1 = plinko;
		R = ((Integer)PlinkoNegativeBinomial.strk.getValue()).intValue();
		CURRENT_BIN = Math.min(CURRENT_BIN, BINS - 1);
		RIGHT = 0;
		PERCENT = 0.0D;
		DIST = Math.min((double)(H - 100 - BOTTOM_MARGIN) / (double)(BINS + R), (double)(W - 10) / (double)BINS);
		SIDE_MARGIN = ((double)W - (double)BINS * DIST) / 2D;
		PIN_RAD = (int)DIST / 9 + 1;
		BALL_RAD = Math.max((2D * DIST) / 7D, 2D) + 1.0D;
		IMAGES = new Image[COLORS.length];
		for(int i = 0; i < COLORS.length; i++)
		{
			IMAGES[i] = getBall(BALL_RAD, COLORS[i]);
		}

		PINS = new double[R + 1][][];
		for(int j = 0; j < R; j++)
		{
			PINS[j] = new double[200][2];
			for(int l = 0; l < 150; l++)
			{
				PINS[j][l][0] = DIST * (double)((l - j) + R) + SIDE_MARGIN;
				PINS[j][l][1] = DIST * (double)(l + j + 1);
			}

		}

		PINS[R] = new double[200][2];
		for(int k = 0; k < 200; k++)
		{
			PINS[R][k][0] = DIST * ((double)k + 0.5D) + SIDE_MARGIN;
			PINS[R][k][1] = (double)(H - 30) - BALL_RAD;
		}

	}

	public static Image getBall(double d, Color color)
	{
		BufferedImage bufferedimage = new BufferedImage((int)(2D * d + 2D), (int)(2D * d + 2D), 2);
		double ad[] = new double[3];
		double ad1[] = new double[3];
		double ad2[] = {
			-5D, 5D, 10D
		};
		double d1 = Math.sqrt(150D);
		for(int k1 = 0; k1 < (int)(2D * d + 2D); k1++)
		{
			for(int l1 = 0; l1 < (int)(2D * d + 2D); l1++)
			{
				if((d - (double)k1) * (d - (double)k1) + (d - (double)l1) * (d - (double)l1) <= (d - 1.0D) * (d - 1.0D))
				{
					ad[0] = ((double)k1 - d) / d;
					ad[1] = (d - (double)l1) / d;
					ad[2] = Math.sqrt(1.0D - ad[0] * ad[0] - ad[1] * ad[1]);
					double d2 = (ad[0] * ad2[0] + ad[1] * ad2[1] + ad[2] * ad2[2]) / Math.sqrt(ad2[0] * ad2[0] + ad2[1] * ad2[1] + ad2[2] * ad2[2]);
					d2 = (1.0D + d2) / 2D;
					double d4 = Math.pow(d2, 20D);
					int i = (int)((double)color.getRed() * (d2 - d4) + d4 * 255D);
					int k = (int)((double)color.getGreen() * (d2 - d4) + d4 * 255D);
					int i1 = (int)((double)color.getBlue() * (d2 - d4) + d4 * 255D);
					bufferedimage.setRGB(k1, l1, 0xff000000 + (i << 16) + (k << 8) + i1);
					continue;
				}
				if((d - (double)k1) * (d - (double)k1) + (d - (double)l1) * (d - (double)l1) > (d + 1.0D) * (d + 1.0D))
				{
					continue;
				}
				int j = 0;
				int l = 0;
				int j1 = 0;
				for(double d6 = -0.33000000000000002D; d6 < 0.5D; d6 += 0.33000000000000002D)
				{
					for(double d7 = -0.33000000000000002D; d7 < 0.5D; d7 += 0.33000000000000002D)
					{
						double d3;
						double d5;
						if((d - ((double)k1 + d6)) * (d - ((double)k1 + d6)) + (d - ((double)l1 + d7)) * (d - ((double)l1 + d7)) <= d * d)
						{
							ad[0] = (((double)k1 + d7) - d) / d;
							ad[1] = (d - ((double)l1 + d7)) / d;
							ad[2] = Math.sqrt(1.0D - ad[0] * ad[0] - ad[1] * ad[1]);
							d3 = (ad[0] * ad2[0] + ad[1] * ad2[1] + ad[2] * ad2[2]) / d1;
							d3 = (1.0D + d3) / 2D;
							d5 = Math.pow(d3, 20D);
						} else
						{
							d3 = 1.0D;
							d5 = 1.0D;
						}
						j += (int)((double)color.getRed() * (d3 - d5) + d5 * 255D);
						l += (int)((double)color.getGreen() * (d3 - d5) + d5 * 255D);
						j1 += (int)((double)color.getBlue() * (d3 - d5) + d5 * 255D);
					}

				}

				j /= 9;
				l /= 9;
				j1 /= 9;
				bufferedimage.setRGB(k1, l1, (255 - (j + l + j1) / 3 << 24) + (j << 16) + (l << 8) + j1);
			}

		}

		return bufferedimage;
	}

	public void newHist()
	{
		COUNT = 0.0D;
		TOTAL = 0.0D;
		TOTAL_SQUARES = 0.0D;
		MAX = 0.0F;
		PERCENT = 0.0D;
		HIST = new long[101];
	}
	    
	// @begin jm.evers :preparing a data string for javascript: must be of type string: arrays always give trouble on IE... 
	public String ReadData(){
	    String reply="";
	    // Rest is the number of balls that falls to "the right" of the last [max] bin
	    double total=0.0D;
	    double sum=0.0D;
	    for(int i=0;i<HIST.length;i++){
		total=total + HIST[i];
		if(i < plinko.bins_num){                                                                                                    
        	    if(i != 0){
			reply=reply+","+HIST[i];
		    }
		    else
		    {
			reply=""+HIST[0];    
		    }
		    sum=sum+HIST[i];
		}
    	    }
	    int Rest=(int) ( total - sum );
	    reply=reply+","+Rest;
    	    return reply;                                                                                                                      
	}                                                                                                                                      
        // @end
	

	public void run()
	{
		while(BALL_COUNT > 0) 
		{
			repaint();
			try
			{
				Thread.sleep(50L);
			}
			catch(InterruptedException interruptedexception) { }
		}
		repaint();
		active = false;
		plinko.active = false;
		PlinkoNegativeBinomial _tmp = plinko;
		PlinkoNegativeBinomial.strk.setEnabled(true);
	}

	public void kill()
	{
		FIRST_BALL = null;
		if(plinko.start.getText().equals("Start"))
		{
			active = false;
			plinko.active = false;
			PlinkoNegativeBinomial _tmp = plinko;
			PlinkoNegativeBinomial.strk.setEnabled(true);
		}
	}
	
	// jm.evers
	public void LimitReached(){                                                                                                            
            plinko.active = true;                                                                                                          
            FIRST_BALL=null;                                                                                                               
            plinko.toggleStart();
	}                                                                                                                                      
       

	public void dropBall(boolean flag)
	{
		BALL_COUNT++;
		//@begin jm.evers : I don't know of another/better way to get the system to stop at a given [param] number of balls... 
		//System.out.println("COUNT ="+COUNT+ "....maximum_balls ="+maximum_balls);
		if(COUNT >= maximum_balls ){LimitReached();}  
		//@end
		if(FIRST_BALL == null)
		{
			FIRST_BALL = new PlinkoBall();
			FIRST_BALL.sound = flag;
		} else
		{
			FIRST_BALL.previousBall = new PlinkoBall();
			FIRST_BALL.previousBall.sound = flag;
			FIRST_BALL.previousBall.nextBall = FIRST_BALL;
			FIRST_BALL = FIRST_BALL.previousBall;
		}
		try
		{
			Thread.sleep(0L);
		}
		catch(InterruptedException interruptedexception) { }
		if(!active)
		{
			active = true;
			thread = new Thread(this);
			thread.start();
		}
	}

	public void paintComponent(Graphics g)
	{
		W = getWidth();
		H = getHeight();
		if(background == null || background.getWidth(this) != W || background.getHeight(this) != H || image == null || image.getWidth(this) != W || image.getHeight(this) != H)
		{
			setup();
			background = createImage(W, H);
			drawBackground(W, H);
			image = createImage(W, H);
			graphics = (Graphics2D)image.getGraphics();
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		}
		graphics.drawImage(background, 0, 0, this);
		for(PlinkoBall plinkoball = FIRST_BALL; plinkoball != null; plinkoball = plinkoball.nextBall)
		{
			graphics.drawImage(IMAGES[plinkoball.spaz], (int)(plinkoball.X - BALL_RAD), (int)((plinkoball.Y - 2D * BALL_RAD - (double)PIN_RAD) + 1.0D), this);
			increment(plinkoball);
		}

		drawHist(graphics);
		drawStats(graphics);
		g.drawImage(image, 0, 0, this);
	}

	public void increment(PlinkoBall plinkoball)
	{
		if(plinkoball.t == plinkoball.C && plinkoball.ROW < R)
		{
			plinkoball.t = 0;
			plinkoball.DIR = 0;
			PlinkoNegativeBinomial _tmp = plinko;
			// @begin jm.evers: reading controls or applet param...plinko.chance is parameter                                         
                        if(plinko.wims_exercise == false){                                                                                 
                            if ( Math.random() < ((Double)PlinkoNegativeBinomial.prob.getValue()).doubleValue() ) plinkoball.DIR = 1;                            
                        }                                                                                                                  
                        else                                                                                                               
                        {                                                                                                                  
                            if ( Math.random() <  plinko.chance ){ plinkoball.DIR = 1;}                                                          
                        } 
			// @end
			
			plinkoball.ROW += 1 - plinkoball.DIR;
			plinkoball.COL += plinkoball.DIR;
			PlinkoNegativeBinomial _tmp1 = plinko;
			plinkoball.C = 11 - ((Integer)PlinkoNegativeBinomial.rate.getValue()).intValue();
			if(plinkoball.sound)
			{
				PlinkoNegativeBinomial.ping.play();
			}
		}
		double d = (DIST * (double)plinkoball.t * (double)(2 * plinkoball.DIR - 1)) / (double)plinkoball.C;
		if(plinkoball.ROW == 0 && plinkoball.COL == 0)
		{
			plinkoball.X = PINS[0][0][0];
			plinkoball.Y = (PINS[0][0][1] - DIST) + (DIST * (double)plinkoball.t * (double)plinkoball.t) / (double)(plinkoball.C * plinkoball.C);
		} else
		if(plinkoball.ROW < R)
		{
			plinkoball.X = DIST * (double)((plinkoball.COL - plinkoball.ROW - 2 * plinkoball.DIR) + 1 + R) + d + SIDE_MARGIN;
			plinkoball.Y = DIST * ((double)(plinkoball.COL + plinkoball.ROW) - DYS[plinkoball.C][plinkoball.t]);
		} else
		{
			plinkoball.X = PINS[R - 1][plinkoball.COL][0] + d;
			plinkoball.X = Math.max(plinkoball.X, (PINS[R][plinkoball.COL][0] - DIST / 2D) + BALL_RAD + 1.0D);
			d = Math.abs(d);
			PlinkoBall _tmp2 = plinkoball;
			PlinkoBall _tmp3 = plinkoball;
			plinkoball.Y = PINS[plinkoball.ROW - 1][plinkoball.COL][1] - d * ((PlinkoBall.A * d) / DIST + PlinkoBall.B);
			if(plinkoball.Y > (double)(H - BOTTOM_MARGIN - PIN_RAD))
			{
				if(plinkoball.previousBall != null && plinkoball.nextBall != null)
				{
					plinkoball.previousBall.nextBall = plinkoball.nextBall;
					plinkoball.nextBall.previousBall = plinkoball.previousBall;
				} else
				if(plinkoball.previousBall != null && plinkoball.nextBall == null)
				{
					plinkoball.previousBall.nextBall = null;
				} else
				if(plinkoball.previousBall == null && plinkoball.nextBall != null)
				{
					plinkoball.nextBall.previousBall = null;
					FIRST_BALL = plinkoball.nextBall;
				} else
				{
					FIRST_BALL = null;
					PlinkoNegativeBinomial _tmp4 = plinko;
					PlinkoNegativeBinomial.strk.setEnabled(true);
				}
				BALL_COUNT--;
				updateHist(plinkoball.COL);
				if(plinkoball.sound)
				{
					PlinkoNegativeBinomial.click.play();
				}
			}
		}
		plinkoball.t++;
	}

	public void updateHist(int i)
	{
		HIST[Math.min(i, 100)]++;
		COUNT++;
		TOTAL += i;
		TOTAL_SQUARES += i * i;
		if((float)HIST[Math.min(i, 100)] > MAX)
		{
			MAX = HIST[Math.min(i, 100)];
		}
		if(i <= RIGHT)
		{
			PERCENT++;
		}
	}

	public void drawStats(Graphics g)
	{
	    if(showstats == 1){ // jm.evers : we do not show calculated results in an exercise...just the balls per Bin.
		plinko.count.setText(plinko.label_count + (int)COUNT);
		plinko.current_bin.setText(plinko.label_bin+ CURRENT_BIN);
		plinko.current_bin_count.setText(plinko.label_bin_count + HIST[CURRENT_BIN]);
		if(COUNT > 0.0D)
		{
			double d = TOTAL / COUNT;
			plinko.mean.setText(plinko.label_mean + (float)d);
			plinko.variance.setText(plinko.label_variance + (float)(TOTAL_SQUARES / COUNT - d * d));
			plinko.current_bin_prob.setText(plinko.label_bin_probability + (float)((double)(int)((double)(0x186a0L * HIST[CURRENT_BIN]) / COUNT) / 1000D) + "%");
			if(RIGHT == 0)
			{
				plinko.confidence.setText(plinko.label_confidence + (float)((double)(int)((100000D * PERCENT) / COUNT) / 1000D) + "% of the balls landed in bin " + RIGHT + ".");
			} else
			{
				plinko.confidence.setText(plinko.label_confidence + (float)((double)(int)((100000D * PERCENT) / COUNT) / 1000D) + "% of the balls landed in bins 0 through " + RIGHT + ".");
			}
		} else
		{
			plinko.mean.setText(plinko.label_mean);
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

	public void drawHist(Graphics2D graphics2d)
	{
		W = getWidth();
		H = getHeight();
		for(int i = 0; i < BINS; i++)
		{
			double d;
			double d1;
			if(i == BINS - 1)
			{
				d = PINS[R][i][0] - DIST / 2D;
				d1 = PINS[R][i][0] + DIST / 2D;
			} else
			{
				d = PINS[R][i][0] - DIST / 2D;
				d1 = PINS[R][i + 1][0] - DIST / 2D;
			}
			long l = HIST[i];
			if(MAX > 100F)
			{
				l = (int)((100D * (double)l) / (double)MAX);
			}
			graphics2d.setColor(new Color(255, 0, 0, 175));
			if(i <= RIGHT)
			{
				graphics2d.setColor(new Color(0, 255, 0, 175));
			}
			java.awt.geom.Rectangle2D.Double double1 = new java.awt.geom.Rectangle2D.Double(d, (long)H - l - (long)BOTTOM_MARGIN, d1 - d, l);
			graphics2d.fill(double1);
			graphics2d.setColor(Color.black);
			graphics2d.setStroke(new BasicStroke(1.0F));
			graphics2d.draw(double1);
		}

		graphics2d.setColor(Color.black);
		graphics2d.setStroke(new BasicStroke(3F));
		graphics2d.draw(new java.awt.geom.Line2D.Double(PINS[R][CURRENT_BIN][0] - DIST / 2D, (H - BOTTOM_MARGIN) + 3, PINS[R][CURRENT_BIN][0] + DIST / 2D, (H - BOTTOM_MARGIN) + 3));
	}

	public void drawBackground()
	{
		drawBackground(getWidth(), getHeight());
	}

	public void drawBackground(int i, int j)
	{
		Graphics2D graphics2d = (Graphics2D)background.getGraphics();
		graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		graphics2d.setColor(Color.white);
		graphics2d.fillRect(0, 0, i, j);
		graphics2d.setColor(Color.black);
		Image image1 = getBall(PIN_RAD, Color.black);
		for(int l = 0; l < R; l++)
		{
			double ad[] = PINS[l][0];
			for(int k = 0; k < 1000 && (ad[1] + (double)k * DIST) - (double)PIN_RAD < (double)j; k++)
			{
				graphics2d.drawImage(image1, (int)((ad[0] - (double)PIN_RAD) + (double)k * DIST), (int)((ad[1] - (double)PIN_RAD) + (double)k * DIST), this);
			}

		}

		graphics2d.setFont(new Font("Helvetica", 1, Math.min((int)(2D * DIST) / 3, 50)));
		FontMetrics fontmetrics = graphics2d.getFontMetrics();
		graphics2d.draw(new java.awt.geom.Line2D.Double(0.0D, j - BOTTOM_MARGIN, i, j - BOTTOM_MARGIN));
		//jm.evers Bins start with number 1 for highschools...
		String s="";
		for(int i1 = 0; i1 < BINS; i1++)
		{
			double ad1[] = PINS[R][i1];
			s = ""+(i1+start_number); 
			graphics2d.setColor(Color.darkGray);
			graphics2d.drawString(s, (int)(ad1[0] - (double)(fontmetrics.stringWidth(s) / 2)), (j - 100 - BOTTOM_MARGIN) + Math.min((int)(2D * DIST) / 3, 50));
			graphics2d.setColor(Color.black);
			graphics2d.draw(new java.awt.geom.Line2D.Double(ad1[0] - DIST / 2D, j - 100 - BOTTOM_MARGIN, ad1[0] - DIST / 2D, j - BOTTOM_MARGIN));
		}

		double ad2[] = PINS[R][BINS - 1];
		graphics2d.draw(new java.awt.geom.Line2D.Double(ad2[0] + DIST / 2D, j - 100 - BOTTOM_MARGIN, ad2[0] + DIST / 2D, j - BOTTOM_MARGIN));
		repaint();
	}

	public void updatePercent()
	{
		RIGHT = CURRENT_BIN;
		PERCENT = 0.0D;
		for(int i = 0; i <= RIGHT; i++)
		{
			PERCENT += HIST[i];
		}

	}

	public void keyTyped(KeyEvent keyevent)
	{
	}

	public void keyPressed(KeyEvent keyevent)
	{
		int i = keyevent.getKeyCode();
		char c = keyevent.getKeyChar();
		if(c >= '0' && c <= '4')
		{
			int j = c - 48;
			int k = 1;
			for(int l = 0; l < j; l++)
			{
				k *= 10;
			}

			for(int i1 = 0; i1 < k; i1++)
			{
				dropBall(k == 1);
			}

		} else
		if(i == 37)
		{
			CURRENT_BIN--;
			if(CURRENT_BIN < 0)
			{
				CURRENT_BIN = BINS - 1;
			}
			updatePercent();
			repaint();
		} else
		if(i == 39)
		{
			CURRENT_BIN++;
			if(CURRENT_BIN > BINS - 1)
			{
				CURRENT_BIN = 0;
			}
			updatePercent();
			repaint();
		} else
		if(i != 10)
		{
			if(c == ' ')
			{
				plinko.toggleStart();
			} else
			if(c != '\024' && c != '\003')
			{
				if(c != '\030');
			}
		}
	}

	public void keyReleased(KeyEvent keyevent)
	{
	}

	public void mouseReleased(MouseEvent mouseevent)
	{
	}

	public void mouseEntered(MouseEvent mouseevent)
	{
	}

	public void mouseExited(MouseEvent mouseevent)
	{
	}

	public void mouseClicked(MouseEvent mouseevent)
	{
		requestFocus();
		Point point = mouseevent.getPoint();
		if((double)point.x > PINS[R][0][0] - DIST / 2D && (double)point.x < PINS[R][BINS - 1][0] + DIST / 2D && point.y > H - 100 - BOTTOM_MARGIN && point.y < H - BOTTOM_MARGIN)
		{
			CURRENT_BIN = (int)(((double)point.x - PINS[R][0][0]) / DIST + 0.5D);
			updatePercent();
			repaint();
		}
	}

	public void mousePressed(MouseEvent mouseevent)
	{
	}

	public void mouseDragged(MouseEvent mouseevent)
	{
	}

	public void mouseMoved(MouseEvent mouseevent)
	{
	}

}
