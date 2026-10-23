/*
*********************************************************************************
* J.M. Evers 19/11/2009								*
* This is all amateur scriblings... So no copyrights.				*
* This source code file, and compiled classes derived from it,			*
* can be used and distributed without restriction, including for commercial use	*
* No warrenty whatoever								*
*********************************************************************************
*/
import java.applet.*;
import javax.swing.*;
import java.awt.*;
import org.javia.arity.Symbols;                                                                                                            
import org.javia.arity.SyntaxException;


public class GraphWindow extends javax.swing.JFrame {  

    public void plotfunction(String fun) {
	Symbols symbols=new Symbols();
	int c=0;
	xmin=-5;
	xmax=5;
	xpix=getSize().width;
	ypix=getSize().height;
	dx=xmax-xmin;
	steps=(int)(10*(dx));
	xdata=new double[steps+10];
	ydata=new double[steps+10];
	ymax=1.0;ymin=-1.0;String Y;double y;
        for (double x = xmin ; x < xmax ; x=x+0.1) {
	    Y=fun.replaceAll("x","("+x+")");
	    try { y=symbols.eval( Y );} catch (SyntaxException e ) {y=0.0;System.out.println("error "+e); }
	    if(y>ymax){ymax=y;}
	    if(y<ymin){ymin=y;}
	    xdata[c]=(double) (x*(xpix/(dx)) -  xmin*xpix/(dx));
	    ydata[c]=y;
	    c++;
        }
	ymax=1.2*ymax;
	ymin=1.2*ymin;
	dy=(ymax - ymin);
	if(dy==0){dy=100.0;}
	for(c=0;c<steps;c++){
	    ydata[c]=(double) (ydata[c]*(-0.6)*(ypix)/(dy) + (0.6*ypix)*ymax/(dy)) + 0.3*ypix;
	}
	repaint();
    } 
                                                                                                                                           
    public void paint(Graphics g){
	g.setColor(bgcolor3);
	g.fillRect(0,0,xpix,ypix);
	g.setColor(Color.red);
	for(int c=0;c<steps-1;c++){
            g.drawLine((int)xdata[c], (int)ydata[c], (int)xdata[c+1], (int)ydata[c+1]);
	}	
	g.setColor(Color.blue);
        g.drawLine(0,(int)((0.6*ypix)*ymax/(dy) + 0.3*ypix),xpix,(int)((0.6*ypix)*ymax/(dy) + 0.3*ypix));
        g.drawLine((int)(-1*xmin*xpix/dx),0,(int)(-1*xmin*xpix/(dx)),ypix );
     } 
    
    
    double[] xdata;
    double[] ydata;
    int xpix;
    int ypix;
    double xmax;
    double xmin;
    double ymax;
    double ymin;
    double dy;
    double dx;
    int steps;
    Color bgcolor3=new Color(164,235,60);                                                                                              

}