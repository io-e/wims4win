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
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class PlinkoNegativeBinomial extends JApplet implements Runnable, ActionListener, ChangeListener, WindowListener
{
	static JSpinner bins;
	static JSpinner strk;
	static JSpinner rate;
	static JSpinner prob;
	JLabel count;
	JLabel mean;
	JLabel variance;
	JLabel current_bin;
	JLabel current_bin_count;
	JLabel current_bin_prob;
	JLabel confidence;
	JButton start;
	JButton clear;
	JButton close;
	PlinkoBoard plinkoboard;
	Thread thread;
	boolean active;
	JFrame frame;
	SplashPanel splash;
	static AudioClip click;
	static AudioClip ping;
	
    // @begin jm.evers: declaring a few things
	boolean wims_exercise = false;
	int bins_num=11;
	int speed=1;
	int total_balls=100000;
	double chance=0.5;
	int frame_x=650;
	int frame_y=600;
	int font_size=14;
	int R=1;
	String title="Binomial Distribution";
	String click_text="CLICK ANYWHERE TO BEGIN";
	String start_text="START";
	String stop_text="STOP";
	String clear_text="CLEAR";
	String close_text="CLOSE";
	String label_count="Count: ";
	String label_mean="Mean: ";
	String label_variance="Variance: ";
	String label_bin="Bin: ";
	String label_bins="Bins: ";
	String label_speed="Speed: ";
	String label_probability="Probability: ";    
	String label_bin_count="Bin Count: ";
	String label_bin_probability="Bin Probability: ";
	String label_confidence="Confidence Interval: ";
	String some_text=" % of the balls landed in bin ";
	String through=" through ";
	String label_controls=" Controls ";
	String label_statistics="Statistics";
    // @end jm.evers: declaring a few things



	public PlinkoNegativeBinomial()
	{
		active = false;
	}

	public void init()
	{
    //@begin jm.evers: introducing a few Applet params , used for tailoring the exercise.
	String s=getParameter("wims_exercise");
	if(s!=null){
	    if(s.equalsIgnoreCase("yes") || s.equals("1")){
		wims_exercise = true;
		s=getParameter("balls");
		if(s!=null && s.length()>0){total_balls = Integer.parseInt(s, 10);}
		s=getParameter("chance");
		if(s!=null && s.length()>0){chance = Double.parseDouble(s);}
		s=getParameter("bins");
		if(s!=null && s.length()>0){bins_num = Integer.parseInt(s, 10);}
		s=getParameter("speed");
		if(s!=null && s.length()>0){speed = Integer.parseInt(s, 10);}
		s=getParameter("xsize");
		if(s!=null && s.length()>0){frame_x = Integer.parseInt(s, 10);}
		s=getParameter("ysize");
		if(s!=null && s.length()>0){frame_y = Integer.parseInt(s, 10);}
		s=getParameter("title");
		if(s!=null && s.length()>0){title=s;}
		s=getParameter("font_size");
		if(s!=null && s.length()>0){font_size = Integer.parseInt(s, 10);}
		s=getParameter("R");
		if(s!=null && s.length()>0){R = Integer.parseInt(s, 10);}

	    }
	}
	s=getParameter("language");
	if(s!=null && s.length()>0){
	    if(s.equalsIgnoreCase("nl")){
		click_text="KLIK HIER OM TE BEGINNEN";
		start_text="START";
		stop_text="STOP";
		clear_text="WISSEN";
		close_text="SLUITEN";
		label_count="Aantal: ";
		label_mean="Gemiddeld: ";
		label_variance="Variantie: ";
		label_bin="Bakje: ";
		label_bins="Bakjes: ";
		label_speed="Snelheid: ";
		label_bin_count="Ballen/bakje: ";
		label_probability="Kans: ";    
		label_bin_probability="Kans per bakje: ";
		label_confidence="Betrouwbaarheids interval: ";
		some_text=" % van de ballen in bakje: ";
		through=" t/m ";
		String label_controls=" Menu ";
		String label_statistics="Statistiek";
	    }
	    if(s.equalsIgnoreCase("fr")){
		click_text="CLIQUEZ ICI";
		start_text="COMMENCER";
		stop_text="PAUSE";
		clear_text="EFFACER";
		close_text="FERMER";
		label_count="Quantit\u00E9 : ";
		label_mean="Moyenne : ";
		label_variance="Variance: ";
		label_bin="Panier : ";
		label_bins="Paniers : ";
		label_speed="Vitesse : ";
		label_bin_count="Balles dans ce panier : ";
		label_bin_probability="% de balles dans ce panier : ";
		label_probability="Probabilit\u00E9 : ";    
		label_confidence="Intervalle de confiance : ";
		some_text=" % de balles dans le panier ";
		through=" through ";
		label_controls="Menu";
		label_statistics="Statistiques";
	    }
	    if(s.equalsIgnoreCase("de")){
		click_text="Klicken Sie hier um zu starten";
		start_text="START";
		stop_text="STOP";
		clear_text="CLEAR";
		close_text="CLOSE";
		label_count="Zahl: ";
		label_mean="Mean: ";
		label_variance="Variance: ";
		label_bin="Behälter: ";
		label_bins="Behälter: ";
		label_speed="Geschwindigkeit : ";
		label_bin_count="Zahl/Behälter: ";
		label_bin_probability="Wahrscheinlichkeit in Behälter: ";
		label_probability="Wahrscheinlichkeit: ";    
		label_confidence="Wahrscheinlichkeits Intervall: ";
		some_text=" % der Kugeln fällt in Behälter ";
		through=" bis ";
		String label_controls=" Menu ";
		String label_statistics="Statistik";
	    }
	}
    //@end jm.evers: introducing a few Applet params , used for tailoring the exercise.

	    setBackground(Color.white);
	    frame = new JFrame();
	    frame.setVisible(false);
	    frame.setSize(frame_x, frame_y);
	    frame.setLocation(0,0);
	    frame.setTitle( title );
	    Font font = new Font( "Helvetica", Font.BOLD, font_size );
		
	    JPanel jpanel = new JPanel();
	    JLabel jlabel = new JLabel("R:", 2);
	    if(wims_exercise == false){
		jpanel.add(jlabel);
	    }
	    strk = new JSpinner(new SpinnerNumberModel( R , 1, 50, 1));
	    if(wims_exercise == false){
		jpanel.add(strk);
	    }
	    jlabel = new JLabel(label_bins, 2);
	    if(wims_exercise == false){
		jpanel.add(jlabel);
	    }
	    bins = new JSpinner(new SpinnerNumberModel(bins_num, 2, 100, 1));
	    if(wims_exercise == false){	    
		jpanel.add(bins);
	    }
	    jlabel = new JLabel(label_probability, 2);
	    if(wims_exercise == false){
		jpanel.add(jlabel);
	    }
	    prob = new JSpinner(new SpinnerNumberModel(chance, 0.0D, 100D, 0.01D));
	    SpinnerNumberModel spinnernumbermodel = (SpinnerNumberModel)prob.getModel();
	    spinnernumbermodel.setMaximum(new Double(1.0D));
	    if(wims_exercise == false){
		jpanel.add(prob);
	    }
	    jlabel = new JLabel(label_speed, 2);
	    if(wims_exercise == false){
		jpanel.add(jlabel);
	    }
	    rate = new JSpinner(new SpinnerNumberModel(speed, 1, 10, 1));
	    
	    if(wims_exercise == false){jpanel.add(rate);
	    }
	    JPanel jpanel1 = new JPanel();
	    jpanel1.add(start = new JButton(start_text));
	    start.setDefaultCapable(true);
	    frame.getRootPane().setDefaultButton(start);
	    jpanel1.add(clear = new JButton(clear_text));
	    jpanel1.add(close = new JButton(close_text));
	    close.setDefaultCapable(false);
	    JPanel jpanel2 = new JPanel(new BorderLayout());
	    jpanel2.add("West", jpanel);
	    jpanel2.add("South", jpanel1);
	    jpanel2.add("South", jpanel1);
	    jpanel2.setBorder(BorderFactory.createTitledBorder(jpanel2.getBorder(), label_controls, 1, 2, new Font("Helvetica", 1, font_size)));
	    JPanel jpanel3 = new JPanel(new GridLayout(2, 3));
	    jpanel3.add(count = new JLabel(label_count));
	    jpanel3.add(current_bin = new JLabel(label_bin));
	    jpanel3.add(current_bin_count = new JLabel(label_bin_count));
	    JPanel jpanel4 = new JPanel(new GridLayout(1, 1));
	    JPanel jpanel5 = new JPanel(new BorderLayout());
	    jpanel5.add("Center", jpanel3);
	    jpanel5.add("South", jpanel4);
	    jpanel5.setBorder(BorderFactory.createTitledBorder(jpanel5.getBorder(),label_statistics, 1, 2, new Font("Helvetica", 1, font_size)));
	    JPanel jpanel6 = new JPanel(new BorderLayout());
	    jpanel6.add("Center", jpanel5);
	    JLabel jlabel1 = new JLabel("");
	    jlabel1.setPreferredSize(new Dimension(15, 15));
	    jpanel6.add("South", jlabel1);
	    if(wims_exercise == false){
		jpanel3.add(mean = new JLabel(label_mean));
	        jpanel3.add(variance = new JLabel(label_variance));
		jpanel3.add(current_bin_prob = new JLabel(label_bin_probability));
		jpanel4.add(confidence = new JLabel(label_confidence));
	    }

	    Container container = frame.getContentPane();
	    container.setLayout(new BorderLayout(10, 10));
	    container.add("Center", plinkoboard = new PlinkoBoard(this));
	    container.add("North", jpanel2);
	    container.add("South", jpanel6);
	    start.addActionListener(this);
	    clear.addActionListener(this);
	    close.addActionListener(this);
	    strk.addChangeListener(this);
	    bins.addChangeListener(this);
	    rate.addChangeListener(this);
	    prob.addChangeListener(this);
	    plinkoboard.requestFocus();
	    frame.addWindowListener(this);
	    getContentPane().add(splash = new SplashPanel(this));
	    click = getAudioClip(getCodeBase(), "sounds/click.au");
	    ping = getAudioClip(getCodeBase(), "sounds/ping.au");
	}

	public void showFrame()
	{
		frame.setVisible(true);
	}

	public void start()
	{
		if(!frame.isVisible())
		{
			splash.start();
		}
	}

	public void stop()
	{
		splash.active = false;
                frame.setVisible( false );                                                                                                 
                splash.start();                                                                                                            
	}

	public void run()
	{
		while(active) 
		{
			plinkoboard.dropBall(false);
			try
			{
				Thread.sleep(105 - 10 * ((Integer)rate.getValue()).intValue());
			}
			catch(InterruptedException interruptedexception) { }
		}
	}

	public void toggleStart()
	{
		if(active)
		{
			active = false;
			start.setText(start_text);
		} else
		{
			active = true;
			strk.setEnabled(false);
			thread = new Thread(this);
			thread.start();
			start.setText(stop_text);
		}
	}

	public void actionPerformed(ActionEvent actionevent){
		Object obj = actionevent.getSource();
		if(obj == close){ stop();}
		if(obj == start){ toggleStart();} 
		if(obj == clear){
		    if(plinkoboard.FIRST_BALL == null){
			plinkoboard.newHist();
		    } 
		    else
		    {
			plinkoboard.FIRST_BALL = null;
			if(!active){
			    strk.setEnabled(true);
			}
		    }
		    plinkoboard.repaint();
		}
		plinkoboard.requestFocus();
	}

	public void stateChanged(ChangeEvent changeevent)
	{
		Object obj = changeevent.getSource();
		if(obj == strk)
		{
			count.setText("0");
			plinkoboard.setup();
			plinkoboard.newHist();
			plinkoboard.drawBackground();
		} else
		if(obj == bins)
		{
			plinkoboard.setup();
			plinkoboard.drawBackground();
		} else
		if(obj != rate && obj == prob)
		{
			plinkoboard.updatePercent();
			plinkoboard.repaint();
		}
	}

	public void windowActivated(WindowEvent windowevent)
	{
	}

	public void windowClosed(WindowEvent windowevent)
	{
	}

	public void windowClosing(WindowEvent windowevent)
	{
		splash.start();
		active = false;
		start.setText(start_text);
	}

	public void windowDeactivated(WindowEvent windowevent)
	{
	}

	public void windowDeiconified(WindowEvent windowevent)
	{
	}

	public void windowIconified(WindowEvent windowevent)
	{
	}

	public void windowOpened(WindowEvent windowevent)
	{
	}

        public String getAppletInfo(){                                                                                                     
            return "Written by Dr. P. Little \n http://www.math.psu.edu/dlittle/java/probability/plinko/index.html" ;
	}                                                                                                                                  
             
        // jm.evers: returns a string to a javascript function ReadApplet(); [must be a string, and not an array ... IE trouble]           
        // the string will be send -along with the "student reply" to the questions at hand- to the WIMS server;                           
        public String ReadApplet(){                                                                                                        
            String s=plinkoboard.ReadData();                                                                                               
            return s;                                                                                                                      
        }                                                                                                                                  
              
}
