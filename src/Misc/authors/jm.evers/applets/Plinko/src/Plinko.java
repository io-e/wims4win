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
import java.awt.event.*;
import java.applet.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;


public class Plinko extends JApplet implements Runnable, ActionListener, ChangeListener, WindowListener{
    static JSpinner bins;
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
    boolean active = false;
    int countdown = 0;
    JFrame frame;
    SplashPanel splash;
    static AudioClip click;
    static AudioClip ping;
// jm.evers: declaring a few things
    boolean wims_exercise = false;
    int bins_num=11;
    int speed=1;
    int total_balls=100000;
    double chance=0.5;
    int frame_x=600;
    int frame_y=600;
    int font_size=14;
    int bin_height=100;
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
    String some_text2=" % of the balls landed in bins ";
    String through=" through ";
    String label_controls=" Controls ";
    String label_statistics="Statistics";
     
    public void init(){
	// jm.evers: introducing a few Applet params , used for tailoring the exercise.
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
		s=getParameter("binheight");
		if(s!=null && s.length()>0){bin_height = Integer.parseInt(s, 10);}

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
		some_text2=" % van de ballen in bakje: ";
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
		some_text2=" % de balles dans les paniers ";
		through=" \u00E0 ";
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
		some_text2=" % der Kugeln fällt in Behälter ";
		through=" bis ";
		String label_controls=" Menu ";
		String label_statistics="Statistik";
	    }
	}
	setBackground(Color.white);
	frame = new JFrame();
	frame.setVisible(false);
	frame.setSize(frame_x, frame_y);
	frame.setLocation(0,0);
	frame.setTitle( title );
	Font font = new Font( "Helvetica", Font.BOLD, font_size );
    	GridBagLayout gridbaglayout = new GridBagLayout();
	GridBagConstraints constraints = new GridBagConstraints();
	constraints.ipadx = 5;
	constraints.ipady = 5;
	constraints.insets = new Insets( 1,1,1,1 );
	constraints.anchor = GridBagConstraints.CENTER;
	constraints.fill = GridBagConstraints.NONE;
	constraints.weightx = 1.0;
	JPanel controls_west = new JPanel(  );
	JLabel label = new JLabel( label_bins,JLabel.LEFT );
	controls_west.add( label );
	bins = new JSpinner( new SpinnerNumberModel(bins_num,2,101,1) );
	controls_west.add( bins );
	label = new JLabel( label_probability,JLabel.LEFT );
	controls_west.add( label );
	prob = new JSpinner( new SpinnerNumberModel(0.50,0.00,100,0.01) );
	SpinnerNumberModel model = (SpinnerNumberModel)prob.getModel();
	model.setMaximum( new Double(1.00) );
	controls_west.add( prob );
	label = new JLabel( label_speed,JLabel.LEFT );
	controls_west.add( label );
	rate = new JSpinner( new SpinnerNumberModel(1,1,10,1) );
	controls_west.add( rate );
	JPanel controls_east = new JPanel();
	controls_east.add( start = new JButton( start_text ) );
	start.setDefaultCapable( true );
	frame.getRootPane().setDefaultButton( start );
	controls_east.add( clear = new JButton( clear_text ) );
	controls_east.add( close = new JButton( close_text ) );
	close.setDefaultCapable( false );	
	JPanel controls = new JPanel( new BorderLayout() );
	controls.add( "East", controls_east );
	JPanel statistics_center = new JPanel( new GridLayout(2,3) );
	statistics_center.add( count = new JLabel( label_count ) );
	statistics_center.add( mean = new JLabel( label_mean ) );
	statistics_center.add( variance = new JLabel( label_variance ) );
	statistics_center.add( current_bin = new JLabel( label_bin ) );
	statistics_center.add( current_bin_count = new JLabel(label_bin_count ) );
	statistics_center.add( current_bin_prob = new JLabel( label_bin_probability ) );
	JPanel statistics_south = new JPanel( new GridLayout(1,1) );
	statistics_south.add( confidence = new JLabel( label_confidence ) );
	JPanel statistics = new JPanel( new BorderLayout() );
	if(wims_exercise == false){
	    // hmmm...these controls are handed over to applet params...if applet is part of an interactive exercise.
	    controls.add( "West", controls_west );
	    controls.setBorder( BorderFactory.createTitledBorder( controls.getBorder(), label_controls, TitledBorder.LEFT, TitledBorder.TOP, new Font( "Helvetica", Font.BOLD, font_size) ) );
	}
	statistics.add( "Center", statistics_center );
	statistics.add( "South", statistics_south );
	statistics.setBorder( BorderFactory.createTitledBorder( statistics.getBorder(), label_statistics, TitledBorder.LEFT, TitledBorder.TOP, new Font( "Helvetica", Font.BOLD, font_size) ) );

	JPanel south = new JPanel( new BorderLayout() );
	south.add( "Center", statistics );
	JLabel statusbar = new JLabel("");
	statusbar.setPreferredSize( new Dimension(15,15) );
	south.add( "South", statusbar );
	Container contentpane = frame.getContentPane();
	contentpane.setLayout(new BorderLayout(10,10));
	contentpane.add( "Center", plinkoboard = new PlinkoBoard(this) ); 
	contentpane.add( "North", controls );
	contentpane.add( "South", south );
	start.addActionListener( this );
	clear.addActionListener( this );
	close.addActionListener( this );
	bins.addChangeListener( this );
	rate.addChangeListener( this );
	prob.addChangeListener( this );

	plinkoboard.requestFocus();
	frame.addWindowListener( this );
	getContentPane().add( splash = new SplashPanel( this ) );
	click = getAudioClip( getCodeBase(), "sounds/click.au" );
	ping = getAudioClip( getCodeBase(), "sounds/ping.au" );
    }

	// jm.evers: returns a string to a javascript function ReadApplet(); [must be a string, and not an array ... IE trouble]
	// the string will be send -along with the "student reply" to the questions at hand- to the WIMS server; 
	public String ReadApplet(){
	    String s=plinkoboard.ReadData();
	    return s;                                                                                                                      
        }                                                                                                                                  

	public void showFrame(){
		frame.setVisible( true );
	}

	public void stop(){
		frame.setVisible( false );
		splash.start();
	}

	public void run(){		
	    //while ( active  || countdown > 0 ){
	    while ( active ){
		plinkoboard.dropBall( false );
		//if ( countdown > 0 ) countdown--;
		try {
		//if ( countdown < 1)
		    if(wims_exercise){
			Thread.sleep(105 - 10*( (int) speed ));
		    }
		    else
		    {
			Thread.sleep(105 - 10*((Integer)rate.getValue()).intValue());
		    }
		} catch (InterruptedException e){}
	    }
	}
	
	public void toggleStart(){
	    if ( active ){
		active = false;
		start.setText(start_text);
	    } 
	    else 
	    {
		active = true;
		bins.setEnabled( false );
		thread = new Thread(this);
		thread.start();
		start.setText(stop_text);
	    }
	}

	public void actionPerformed( ActionEvent ae ){
	    Object obj = ae.getSource();
	    if (obj == start){toggleStart();plinkoboard.requestFocus();}
	    if( obj == clear ){
		if ( plinkoboard.FIRST_BALL == null ){plinkoboard.newHist();} 
		else 
		{
		    plinkoboard.FIRST_BALL = null;
		    //plinkoboard.BALL_COUNT = 0;
		    if ( !active ){bins.setEnabled( true );}
		}
		    plinkoboard.repaint();plinkoboard.requestFocus();
	    }
	    if(obj == close){stop();}
	}
		
	public void stateChanged( ChangeEvent ce ){
	    Object obj = ce.getSource();
	    if ( obj == bins ){
		count.setText( "0" );
		plinkoboard.setup( );
		plinkoboard.newHist( );
		plinkoboard.drawBackground();
	    } else if ( obj == rate ){
	    } else if ( obj == prob ){
		plinkoboard.updatePercent();
		plinkoboard.repaint();
	    }
	}

	public void windowActivated( WindowEvent we ){
	    //splash.start();
	}
	

	public void windowClosed( WindowEvent we ){
		//splash.start();
	}

	public void windowClosing( WindowEvent we ){
		//splash.start();
	}

	public void windowDeactivated( WindowEvent we ){
		//splash.start();
	}

	public void windowDeiconified( WindowEvent we ){}

	public void windowIconified( WindowEvent we ){}

	public void windowOpened( WindowEvent we ){}
	
	public String getAppletInfo(){ 
	    return "Written by Dr. P. Little \n http://www.math.psu.edu/dlittle/java/probability/plinko/index.html" ; 
	}  

/*		
	public void keyTyped( KeyEvent ke ){
	    int code = ke.getKeyCode();
	    char key = ke.getKeyChar();

	    if ( key >= '0' && key <= '4' ){
		int x = key - '0';
		int n = 1;
		for (int i=0; i<x; i++ ){
		    n *= 10;
		}
		for (int i=0; i<n; i++){
		    plinkoboard.dropBall();
		}
	    } else if ( key == ' ' ){
			toggleStart();
	    } else if (key==20){ //control t - toggle erasing
			plinkoboard.toggleErase();
	    } else if (key==3){ //control c - clear screen
			plinkoboard.newHist();
	    } else if (key==24){ //control x - kill all threads and clear screen
			plinkoboard.kill();
	    } else if (key==16){ //control p - toggle displaying probabilities
			//plinkoboard.toggleProbabilities();
	    }
	}

	public void keyPressed( KeyEvent ke ){
	}
	
	public void keyReleased( KeyEvent ke ){
	}*/
          
}
 
