/*    Copyright (C) 1998 XIAO, Gang of Universite de Nice - Sophia Antipolis
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
/*
    slightly modified [crippled] by jm.evers 
    applet does not send any data to wims....
*/

import java.awt.event.*;
import java.awt.*;
import java.applet.*;
import java.net.*;
import java.applet.*;
import java.util.Vector;

public class noninput extends Applet{
    inp_panel panel;
    inp_controls controls;
    Image bg;
    Color bgcolor;
/*    String replystring;
    public static String prompt="OK";
    public static String retry="Erase";*/
    URL backurl;

    public void init() {
	URL url;
	String parmstr;
	parmstr=getParameter("type");
	if(parmstr!=null && parmstr.length()>0) {
	    parmstr.toLowerCase(); parmstr.trim();
	    if(parmstr.compareTo("curve")==0) inp_panel.ctype=inp_panel.CURVE;
	    if(parmstr.compareTo("rectangle")==0) inp_panel.ctype=inp_panel.RECT;
	    if(parmstr.compareTo("rect")==0) inp_panel.ctype=inp_panel.RECT;
	    if(parmstr.compareTo("circle")==0) inp_panel.ctype=inp_panel.CIRCLE;
	    if(parmstr.compareTo("lines")==0) inp_panel.ctype=inp_panel.LINES;
	    if(parmstr.compareTo("segments")==0) inp_panel.ctype=inp_panel.LINES;
	    if(parmstr.compareTo("line")==0) inp_panel.ctype=inp_panel.LINE;
	    if(parmstr.compareTo("sline")==0) inp_panel.ctype=inp_panel.SLINE;
	    if(parmstr.compareTo("semiline")==0) inp_panel.ctype=inp_panel.SLINE;
	    if(parmstr.compareTo("seg")==0) inp_panel.ctype=inp_panel.SEG;
	    if(parmstr.compareTo("segment")==0) inp_panel.ctype=inp_panel.SEG;
	    if(parmstr.compareTo("poly")==0) inp_panel.ctype=inp_panel.POLY;
	    if(parmstr.compareTo("polygon")==0) inp_panel.ctype=inp_panel.POLY;
	    if(parmstr.compareTo("points")==0) inp_panel.ctype=inp_panel.POINTS;
	}
	
	parmstr=getParameter("background");
	if (parmstr!=null && parmstr.length()>0) {
	    try {url=new URL(parmstr);}
	    catch (MalformedURLException e) {url=null;}
	    if(url!=null) bg=getImage(url);
	    else bg=null;
	}
	else bg=null;
	
	parmstr=getParameter("bgcolor");
	if(parmstr!=null && parmstr.length()>0) {
	    bgcolor=Color.decode(parmstr);
	    if(bgcolor==null) bgcolor=Color.white;
	}
	else bgcolor=Color.white;
	
	
/*	parmstr=getParameter("retry");
	if(parmstr!=null && parmstr.length()>0) {
	    retry=parmstr;
	}*/
	
/*	parmstr=getParameter("prompt");
	if(parmstr!=null && parmstr.length()>0) {
	    prompt=parmstr;
	}*/
	
	setLayout(new BorderLayout());
	panel=new inp_panel(bgcolor,bg);
        controls=new inp_controls(panel,this);
	add("Center", panel);
	/*add("South",controls);*/
    }

    public void destroy() {
        remove(panel);
        /*remove(controls);*/
    }

    public static void main(String args[]) {
	Frame f=new Frame("input");
	noninput noninput=new noninput();
	noninput.init();
	noninput.start();

	f.add("Center", noninput);
	f.setVisible(true);
    }

    public String getAppletInfo() {
        return "Curve input program for WIMS.";
    }
}

class inp_panel extends Panel implements MouseListener, MouseMotionListener {
    public static final int CURVE=0, RECT=1, CIRCLE=2, LINES=3, LINE=4,
     SLINE=5,SEG=6,POLY=7,POINTS=8;
    public static int ctype;
    static int ll=4;
    Image bg;
    Vector lines=new Vector(16384);
    int x1,y1;
    int x2,y2;
    int radius;
    int drag;
    int st;

    public inp_panel(Color bgcolor,Image gotbg) {
	setBackground(bgcolor);
	setForeground(Color.black);
	bg=gotbg;
	addMouseMotionListener(this);
	addMouseListener(this);
	st=0;
    }
   
/*    public String points2string(String rep) {
	StringBuffer buf=new StringBuffer(16384);
	buf.append(rep);
	switch(ctype) {
	    case POLY:
	    case POINTS:
	    case LINES:
	    case CURVE: {
		int i, np;
		Point p=null;
		np=lines.size();
		for(i=0;i<np;i++) {
		    p=(Point)lines.elementAt(i);
		    buf.append(p.x).append(",").append(p.y).append(";");
		}
		break;
	    }
	    case SLINE:
	    case LINE:
	    case SEG:
	    case RECT: {
		if(st>0) {
		    buf.append(x1).append(",").append(y1).append(",").append(x2).append(",").append(y2);
		}
		break;
	    }
	    case CIRCLE: {
		if(st>0) {
		    buf.append(x1).append(",").append(y1).append(",").append(radius);
		}
		break;
	    }
	}
	return buf.toString();
    }
  */ 
    public void retry() {
	lines.removeAllElements();
	st=0; repaint();
    }

    public void mouseDragged(MouseEvent e) {
	int t=lines.size();
	int dr;
        e.consume();
	switch(ctype) {
	    case CURVE: {
		if(t<2000) {
		    x1=e.getX(); y1=e.getY();
		    lines.addElement(new Point(x1, y1));
		    repaint();
		}
		st=2; return;
	    }
	    default: {
		dr=drag; mouseMoved(e); drag=dr+1;
		return;
	    }
	}
    }

    public void mouseMoved(MouseEvent e) {
        e.consume(); drag=0;
	switch(ctype) {
	    case POLY:
	    case POINTS:
	    case LINES:
	    case CURVE: {
		if(st==0) return; else st=1;
		x2=e.getX(); y2=e.getY(); break;
	    }
	    case CIRCLE:
	    case SLINE:
	    case LINE:
	    case SEG:
	    case RECT: {
		if(st!=1) return;
		x2=e.getX(); y2=e.getY();
		radius=(int) Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1));
		break;
	    }
	    default: return;
	}
	if(ctype!=POINTS) repaint();
    }

    public void mousePressed(MouseEvent e) {
        e.consume();
	switch(ctype) {
	    case CURVE: {
		if(st>0) return;
		x1=e.getX(); y1=e.getY(); x2=x1; y2=y1;
		lines.removeAllElements();
		lines.addElement(new Point(x1,y1));
		repaint(); st=2;
		return;
	    }
	    case POLY:
	    case POINTS:
	    case LINES: {
		x2=e.getX(); y2=e.getY();
		if(st==0) lines.removeAllElements();
		lines.addElement(new Point(x2,y2));
		st=1; x1=x2; y1=y2; repaint(); return;
	    }
	    case CIRCLE:
	    case SLINE:
	    case LINE:
	    case SEG:
	    case RECT: {
		x2=e.getX(); y2=e.getY();
		switch(st) {
		    case 2:
		    case 0: x1=x2; y1=y2; radius=0; st=1; repaint(); return;
		    case 1: {
			radius=(int) Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1));
			st=2; return;
		    }
		}		
		break;
	    }
	    default: return;
	}
    }

    public void mouseReleased(MouseEvent e) {
	e.consume();
	switch(ctype) {
	    case CURVE: {
		if(st==2) return;
		x2=e.getX(); y2=e.getY();
		if(st==0) lines.removeAllElements();
		lines.addElement(new Point(x2,y2));
		st=1; x1=x2; y1=y2;
		return;
	    }
	    case POINTS: return;
	    default: {
		if(st>0 && drag>=8) mousePressed(e);
		return;
	    }
	}
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
	if(ctype!=CURVE && ctype!=LINES && ctype!=POLY) return;
	e.consume();
	x2=x1; y2=y1; if(st>0) repaint();
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void paint(Graphics g) {
	int np=lines.size();
	Point pp;

	if(bg!=null) g.drawImage(bg,0,0,this);
	switch(ctype) {
	    case POINTS: {
		if(np>0) for (int i=0; i < np; i++) 
		  pointPaint(g,(Point)lines.elementAt(i));
		break;
	    }
	    case POLY:
	    case CURVE:
	    case LINES: {
		if(np>0) {
		    pp=(Point)lines.elementAt(0);
		    for (int i=1; i < np; i++) {
			Point p=(Point)lines.elementAt(i);
			g.drawLine(pp.x, pp.y, p.x, p.y);
			pp=p;
		    }
		    if (st==1) g.drawLine(x1, y1, x2, y2);
		}
		if(ctype==POLY && np>1) {
		    Point p1=(Point)lines.elementAt(0);
		    Point p2=(Point)lines.elementAt(np-1);
		    if(st==0) g.drawLine(p1.x,p1.y,p2.x,p2.y);
		    else g.drawLine(p1.x,p1.y,x2,y2);
		}
		break;
	    }
	    case SEG:
	    case SLINE:
	    case LINE:
	    case RECT: {
		if(st>0) {
		    int xx1,yy1,xx2,yy2;
		    int X1,Y1,X2,Y2,max;
		    xx1=Math.min(x1,x2); yy1=Math.min(y1,y2);
		    xx2=Math.max(x1,x2); yy2=Math.max(y1,y2);
		    max=Math.max(Math.abs(x2-x1),Math.abs(y2-y1));
		    if(max<10) max=10;
		    max=500/max+1;
		    switch(ctype) {
			case RECT: g.drawRect(xx1,yy1,xx2-xx1,yy2-yy1); break;
			case SLINE: {
			    g.fillOval(x1-2,y1-2,4,4);
			    g.drawLine(x1,y1,max*x2-(max-1)*x1,max*y2-(max-1)*y1); break;
			}
			case LINE: {
			    g.fillOval(x1-2,y1-2,4,4);
			    g.fillOval(x2-2,y2-2,4,4);
			    g.drawLine(max*x1-(max-1)*x2,max*y1-(max-1)*y2,
				       max*x2-(max-1)*x1,max*y2-(max-1)*y1); break;
			}
			case SEG: g.drawLine(x1,y1,x2,y2); break;
		    }
		}
		break;
	    }
	    case CIRCLE: {
		if(st>0) {
		    pointPaint(g,new Point(x1,y1));
		    g.drawOval(x1-radius,y1-radius,radius*2,radius*2);
		}
		break;
	    }
	}
    }
   
    void pointPaint(Graphics g, Point p) {
	g.drawLine(p.x-ll,p.y+ll,p.x+ll,p.y-ll);
	g.drawLine(p.x+ll,p.y+ll,p.x-ll,p.y-ll);
    }
}


class inp_controls extends Panel implements ActionListener {
   noninput ci;
   inp_panel targ;
   Button retry, ok;
   
   public inp_controls(inp_panel pan, noninput cci) {
       this.ci=cci;
       targ=pan;
       setLayout(new GridLayout());
       setBackground(Color.white);
      /* retry=new Button(noninput.retry);*/
      /* retry.addActionListener(this);*/
       /*ok=new Button(noninput.prompt);
       ok.addActionListener(this);
       add(retry); add(ok);*/
   }
   public void actionPerformed(ActionEvent e) {
       Object src=e.getSource();
       if(src == retry) targ.retry();
   }
}
