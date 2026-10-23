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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JPanel;

public class SplashPanel extends JPanel
	implements MouseListener, Runnable
{

	public SplashPanel(PlinkoNegativeBinomial plinkonegativebinomial)
	{
		BINS = 20;
		BOTTOM_MARGIN = 5;
		newBackground = true;
		active = true;
		FUDGE = 200;
		setBackground(Color.white);
		addMouseListener(this);
		plinko = plinkonegativebinomial;
		for(int i = 0; i < 25; i++)
		{
			dropBall();
			FIRST_BALL.ROW = 10 + (int)(10D * Math.random());
			FIRST_BALL.COL = (int)((double)FIRST_BALL.ROW * Math.random());
			FIRST_BALL.C = 10;
			FIRST_BALL.t = i % 11;
		}

		start();
	}

	public void start()
	{
		active = true;
		thread = new Thread(this);
		thread.start();
	}

	public void run()
	{
		while(active) 
		{
			if(Math.random() < 0.40000000000000002D)
			{
				dropBall();
				FIRST_BALL.ROW = 11;
				FIRST_BALL.COL = (int)((double)FIRST_BALL.ROW * Math.random());
				FIRST_BALL.C = 10;
			}
			repaint();
			try
			{
				Thread.sleep(100L);
			}
			catch(InterruptedException interruptedexception) { }
		}
	}

	public void dropBall()
	{
		if(FIRST_BALL == null)
		{
			FIRST_BALL = new PlinkoBall();
		} else
		{
			FIRST_BALL.previousBall = new PlinkoBall();
			FIRST_BALL.previousBall.nextBall = FIRST_BALL;
			FIRST_BALL = FIRST_BALL.previousBall;
		}
	}

	public void setup()
	{
		if(h - (double)BOTTOM_MARGIN < h / 2D)
		{
			DIST = (h - (double)BOTTOM_MARGIN) / (double)BINS;
		} else
		{
			DIST = (w - 10D) / (double)(2 * BINS);
		}
		PIN_RAD = (int)DIST / 9 + 1;
		BALL_RAD = Math.max((2D * DIST) / 7D, 2D) + 1.0D;
		IMAGES = new Image[PlinkoBoard.COLORS.length];
		for(int i = 0; i < PlinkoBoard.COLORS.length; i++)
		{
			IMAGES[i] = PlinkoBoard.getBall(BALL_RAD, PlinkoBoard.COLORS[i]);
		}

		PINS = new Point[BINS][];
		for(int j = 0; j < BINS; j++)
		{
			PINS[j] = new Point[j + 1];
			for(int l = 0; l <= j; l++)
			{
				PINS[j][l] = new Point((int)(DIST * (double)(2 * l - j) + w / 2D) - FUDGE / 2, (int)(DIST * (double)(j + 1)) - (FUDGE - 15));
			}

		}

		for(int k = 0; k < BINS; k++)
		{
			PINS[BINS - 1][k] = new Point((int)(DIST * (double)((2 * k - BINS) + 1) + w / 2D), (int)(h - 30D - BALL_RAD));
		}

	}

	public void paintComponent(Graphics g)
	{
		w = getWidth();
		h = getHeight();
		w = Math.max(getWidth(), getHeight()) + FUDGE;
		h = w;
		Graphics2D graphics2d = (Graphics2D)g;
		graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		if(background == null || (double)background.getWidth(this) != h || (double)background.getHeight(this) != w)
		{
			setup();
			background = createImage((int)w, (int)h);
			drawBackground((int)w, (int)h);
		}
		graphics2d.drawImage(background, 0, 0, this);
		for(PlinkoBall plinkoball = FIRST_BALL; plinkoball != null; plinkoball = plinkoball.nextBall)
		{
			g.drawImage(IMAGES[plinkoball.spaz], (int)(plinkoball.X - BALL_RAD), (int)((plinkoball.Y - 2D * BALL_RAD - (double)PIN_RAD) + 1.0D), this);
			if(active)
			{
				increment(plinkoball);
			}
		}

		w = getWidth();
		h = getHeight();
		graphics2d.setColor(new Color(128, 128, 128, 220));
		graphics2d.setFont(new Font("Helvetica", 1, plinko.font_size));
		String s =plinko.click_text;
		// "CLICK ANYWHERE TO BEGIN";
		graphics2d.drawString(s, (int)(w - (double)graphics2d.getFontMetrics().stringWidth(s) - 5D), (int)(h - 5D));
	}

	public void increment(PlinkoBall plinkoball)
	{
		if(plinkoball.t == plinkoball.C && plinkoball.ROW < BINS - 2)
		{
			plinkoball.ROW++;
			plinkoball.COL += plinkoball.DIR;
			plinkoball.t = 0;
			plinkoball.DIR = 0;
			if(Math.random() < 0.5D)
			{
				plinkoball.DIR = 1;
			}
			plinkoball.C = 10;
		}
		double d = (DIST * (double)plinkoball.t * (double)(2 * plinkoball.DIR - 1)) / (double)plinkoball.C;
		if(plinkoball.ROW < 0)
		{
			plinkoball.X = PINS[0][0].x;
			d = Math.abs(d);
			plinkoball.Y = ((double)PINS[0][0].y - DIST) + (DIST * (double)plinkoball.t * (double)plinkoball.t) / (double)(plinkoball.C * plinkoball.C);
		} else
		if(plinkoball.ROW < BINS - 2)
		{
			plinkoball.X = (double)PINS[plinkoball.ROW][plinkoball.COL].x + d;
			d = Math.abs(d);
			plinkoball.Y = (double)PINS[plinkoball.ROW][plinkoball.COL].y - DIST * PlinkoBoard.DYS[plinkoball.C][plinkoball.t];
		} else
		{
			plinkoball.X = (double)PINS[plinkoball.ROW][plinkoball.COL].x + d;
			if(d > 0.0D)
			{
				plinkoball.X = Math.min(plinkoball.X, ((double)PINS[plinkoball.ROW][plinkoball.COL].x + 2D * DIST) - BALL_RAD);
			} else
			{
				plinkoball.X = Math.max(plinkoball.X, ((double)PINS[plinkoball.ROW][plinkoball.COL].x - 2D * DIST) + BALL_RAD + 1.0D);
			}
			d = Math.abs(d);
			PlinkoBall _tmp = plinkoball;
			PlinkoBall _tmp1 = plinkoball;
			plinkoball.Y = (double)PINS[plinkoball.ROW][plinkoball.COL].y - d * ((PlinkoBall.A * d) / DIST + PlinkoBall.B);
		}
		plinkoball.t++;
		if(plinkoball.Y > h - (double)BOTTOM_MARGIN - (double)PIN_RAD)
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
			}
			BALL_COUNT--;
		}
	}

	public void drawBackground(int i, int j)
	{
		Graphics2D graphics2d = (Graphics2D)background.getGraphics();
		graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		graphics2d.setColor(Color.white);
		graphics2d.fillRect(0, 0, i, j);
		graphics2d.setColor(Color.black);
		Image image = PlinkoBoard.getBall(PIN_RAD, Color.black);
		for(int k = 0; k < BINS - 1; k++)
		{
			for(int l = 0; l <= k; l++)
			{
				Point point = PINS[k][l];
				graphics2d.drawImage(image, point.x - PIN_RAD, point.y - PIN_RAD, this);
			}

		}

		repaint();
	}

	public void mouseClicked(MouseEvent mouseevent)
	{
		active = false;
		plinko.showFrame();
	}

	public void mouseEntered(MouseEvent mouseevent)
	{
	}

	public void mouseExited(MouseEvent mouseevent)
	{
	}

	public void mousePressed(MouseEvent mouseevent)
	{
	}

	public void mouseReleased(MouseEvent mouseevent)
	{
	}

	PlinkoNegativeBinomial plinko;
	double w;
	double h;
	int TOTAL;
	int HIST[];
	double MAX;
	Point PINS[][];
	double DIST;
	double BALL_RAD;
	int PIN_RAD;
	int BINS;
	PlinkoBall FIRST_BALL;
	int BALL_COUNT;
	int BOTTOM_MARGIN;
	private static final int hx = 15;
	private static final int hy = 15;
	private static final int bgGrey = 192;
	Image IMAGES[];
	boolean newBackground;
	boolean active;
	Thread thread;
	Image background;
	Graphics2D backGraphics;
	int FUDGE;
}
