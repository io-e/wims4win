/*   
 *  Configuration: config.html
 *  Status: Free
 *  Java Boutique:Upload Date October 11, 1999
 *  Author: Saket Kumar
 *  download from: http:*javaboutique.internet.com/PieChart/index.html
 *  PieChart.java  for Drawing Bar Chart.
 *  slightly altered: J.M. Evers 
 *  extra params: degree,radian,rounding etc etc                                                                                                           
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
 
4/2008
 adapted for WIMS by jm.evers.
 modifications:
 - public JS functions: ReadApplet()
 - public JS functions: SetApplet()
 
10/2011
 added:
 - generic alpha channel for all standard colors : 

 <param name="alpha" value="140">
 <param name="Pcolor1" value="red">
 <param name="Pcolor2" value="blue">
 <param name="Pcolor3" value="green">

 - RGB colors: <param name="Pcolor3" value="255,0,120">
 - RGBalpha colors: <param name="Pcolor4" value="255,0,120,140"> 

 - fontsizes:
 <param name="largefont" value="22">
 <param name="smallfont" value="16">
 
 <html>
    <body>
	<applet id="cirkeldiagram1" name="cirkeldiagram1" codebase="dist" archive="2D_pie.jar" code="PieChart.class" width="300" height="300" >
	    <param name="title" value="Year : 1972">
	    <param name="smallfont" value="16">
	    <param name="largefont" value="20">
	    <param name="unit" value="%">
	    <param name="showinput" value="yes">
	    <param name="showdegrees" value="no">
	    <param name="showradians" value="no">
	    <param name="rounding" value="100000">
	    <param name="showlabel"   value="yes">
	    <param name="showpercent" value="no">
	    <param name="bgcolor" value="">
	    <param name="alpha" value="150">
	    <param name="columns" value="4">
	    <param name="Plabel1" value="">
	    <param name="Pvalue1" value="6.6">
	    <param name="Pcolor1" value="cyan">
	    <param name="Plabel2" value="">
	    <param name="Pvalue2" value="13.8">
	    <param name="Pcolor2" value="green">
	    <param name="Plabel3" value="">
	    <param name="Pvalue3" value="14.2">
	    <param name="Pcolor3" value="red">
	    <param name="Plabel4" value="">
	    <param name="Pvalue4" value="65.4">
	    <param name="Pcolor4" value="orange">
	</applet>
    </body>
 </html>
*/

import java.applet.Applet;
import java.awt.*;
import java.io.PrintStream;
import java.util.StringTokenizer;

public class PieChart extends Applet
{

    public PieChart()
    {
        titleHeight = 15;
        round = 1000;
        maxLabelWidth = 0;
        maxValueWidth = 0;
        max = 0;
        strWidth = 0;
        showLabel = true;
        showPercent = false;
        showRadians = false;
        showDegrees = false;
        showInput = true;
        lx = 0;
        ly = 0;
        cx = 0;
        cy = 0;
	smallfont = 16;
	largefont = 18;
	alpha = 255;
    }

    public synchronized void init()
    {
        String s2 = getParameter("smallfont");
	if(s2 != null && s2.length()>0 ){ smallfont = Integer.parseInt(s2); }
	s2 = getParameter("largefont");
	if(s2 != null && s2.length()>0 ){ largefont = Integer.parseInt(s2); }
        font = new Font("Sanserif", 1,smallfont);
        fontMetrics = getFontMetrics(font);
	s2 = getParameter("alpha");
	if(s2 != null && s2.length()>0 ){ alpha = Integer.parseInt(s2);}
        s2 = getParameter("bgcolor");
        if(s2 == null)
            setBackground(Color.white);
        else
        if(s2.equals("red"))
            setBackground(Color.red);
        else
        if(s2.equals("green"))
            setBackground(Color.green);
        else
        if(s2.equals("blue"))
            setBackground(Color.blue);
        else
        if(s2.equals("pink"))
            setBackground(Color.pink);
        else
        if(s2.equals("orange"))
            setBackground(Color.orange);
        else
        if(s2.equals("magenta"))
            setBackground(Color.magenta);
        else
        if(s2.equals("cyan"))
            setBackground(Color.cyan);
        else
        if(s2.equals("white"))
            setBackground(Color.white);
        else
        if(s2.equals("yellow"))
            setBackground(Color.yellow);
        else
        if(s2.equals("gray"))
            setBackground(Color.gray);
        else
        if(s2.equals("darkGray"))
            setBackground(Color.darkGray);
        else
            setBackground(Color.white);
        title = getParameter("title");
        if(title == null)
            title = "WIMS Pie Chart";
        unit = getParameter("unit");
        if(unit == null)
            unit = "";
        String s = getParameter("columns");
        if(s == null)
            columns = 5;
        else
            columns = Integer.parseInt(s);
        s = getParameter("rounding");
        if(s != null)
        {
            round = Integer.parseInt(s);
            if(round < 1)
                round = 100;
        }
        s = getParameter("showdegrees");
        if(s == null)
        {
            showDegrees = false;
        } else
        {
            if(s.equalsIgnoreCase("YES"))
                showDegrees = true;
            if(s.equalsIgnoreCase("NO"))
                showDegrees = false;
        }
        s = getParameter("showradians");
        if(s == null)
        {
            showRadians = false;
        } else
        {
            if(s.equalsIgnoreCase("YES"))
                showRadians = true;
            if(s.equalsIgnoreCase("NO"))
                showRadians = false;
        }
        s = getParameter("showinput");
        if(s == null)
        {
            showInput = true;
        } else
        {
            if(s.equalsIgnoreCase("YES"))
                showInput = true;
            if(s.equalsIgnoreCase("NO"))
                showInput = false;
        }
        s = getParameter("showlabel");
        if(s == null)
        {
            showLabel = false;
        } else
        {
            if(s.equalsIgnoreCase("YES"))
                showLabel = true;
            if(s.equalsIgnoreCase("NO"))
                showLabel = false;
        }
        s = getParameter("showpercent");
        if(s == null)
        {
            showPercent = false;
        } else
        {
            if(s.equalsIgnoreCase("YES"))
                showPercent = true;
            if(s.equalsIgnoreCase("NO"))
                showPercent = false;
        }
        radian = new float[columns];
        degree = new float[columns];
        rawinput = new float[columns];
        values = new float[columns];
        colors = new Color[columns];
        labels = new String[columns];
        percent = new float[columns];
        angle = new float[columns];
        Object obj = null;
        float f = 0.0F;
        for(int i = 0; i < columns; i++)
        {
            String s3 = getParameter("Pvalue" + (i + 1));
            float f2 = Float.valueOf(s3.trim()).floatValue();
            values[i] = f2;
            float f3 = Float.valueOf(s3).floatValue();
            rawinput[i] = f3;
            f += values[i];
            String s1 = getParameter("Plabel" + (i + 1));
            labels[i] = s1 != null ? s1 : "";
            maxLabelWidth = Math.max(fontMetrics.stringWidth(labels[i]), maxLabelWidth);
            s1 = getParameter("Pcolor" + (i + 1));
            if(s1 != null)
            {
	        if (s1.indexOf(",") != -1){
		    StringTokenizer cs = new StringTokenizer(s1, ",");
		    int m = cs.countTokens();
		    if(m == 3){// no alpha
			int[] k = new int[3];
			for(int p=0;p<3;p++){
			    k[p] = Integer.parseInt(cs.nextToken());
			}
			colors[i]=new Color(k[0],k[1],k[2]);
			System.out.println("color "+k[0]+","+k[1]+","+k[2]);
		    }
		    else
		    {
			if(m ==4){//alpha
			    int[] k = new int[4];
			    for(int p=0;p<4;p++){
				k[p] = Integer.parseInt(cs.nextToken());
			    }
			    colors[i]=new Color(k[0],k[1],k[2],k[3]);
			    System.out.println("color "+k[0]+","+k[1]+","+k[2]+","+k[3]);
			}
			else
			{
			    colors[i] = Color.gray;System.out.println("error syntax is value=\"R:G:B\" or value=\"R:G:B:alpha\"");
			}
		    }    
		}
		else
            	if(s1.equals("red")) colors[i] = new Color(255,0,0,alpha);
		else
		if(s1.equals("green")) colors[i] = new Color(0,255,0,alpha);
            	else    
		if(s1.equals("blue")) colors[i] = new Color(0,0,255,alpha);
		else
            	if(s1.equals("pink")) colors[i] = new Color(255,0,255,alpha);
                else
                if(s1.equals("orange")) colors[i] = new Color(255,210,0,alpha);
                else
		if(s1.equals("magenta")) colors[i] = new Color(255,0,255,alpha);
		else
                if(s1.equals("cyan")) colors[i] = new Color(0,255,255,alpha);
		else
                if(s1.equals("white")) colors[i] = new Color(255,255,255,alpha);
                else
		if(s1.equals("yellow")) colors[i] = new Color(255,255,0,alpha);
		else
                if(s1.equals("gray")) colors[i] = new Color(139,137,137,alpha);
		else
                if(s1.equals("darkGray")) colors[i] = new  Color(49,79,79,alpha);
                else colors[i] = new Color(139,137,137,alpha);
            } 
	    else
            {
                colors[i] = Color.gray;
            }
        }

        float f1 = 100F / f;
        for(int j = 0; j < columns; j++)
        {
            percent[j] = (float)Math.round((float)round * values[j] * f1) / (float)round;
            angle[j] = (float)Math.round((double)round * 3.6000000000000001D * (double)values[j] * (double)f1) / (float)round;
            degree[j] = (float)Math.round((double)((float)round * percent[j]) * 3.6000000000000001D) / (float)round;
            radian[j] = (float)Math.round(((float)round * angle[j]) / 180F) / (float)round;
        }

    }

    public synchronized void paint(Graphics g)
    {
        int i = 0;
        byte byte0 = 0;
        int j = 0;
        int k = 0;
        boolean flag = false;
        boolean flag1 = false;
        boolean flag2 = false;
        boolean flag3 = false;
        int j1 = 0;
	int marge=100;
        j = k = Math.min(getSize().width - marge, getSize().height - marge);
        i = byte0 = 50;
        if(getSize().width > j)
            i = (getSize().width - j) / 2;
        cx = i + j / 2;
        cy = byte0 + k / 2;
        j1 = j / 2;
        strWidth = fontMetrics.stringWidth(title);
        Font font1 = new Font("Sanserif", 1, largefont);
        g.setFont(font1);
        g.setColor(Color.red);
        g.drawString(title, (getSize().width - strWidth) / 2, 15);
        g.setFont(font);
        int k1 = 90;
        boolean flag4 = false;
        int i2 = 0;
        int j2 = (int)(angle[0] / 2.0F);
        for(int k2 = 0; k2 < columns; k2++)
        {
            int l1 = Math.round(angle[k2]);
            g.setColor(colors[k2]);
            if(k2 == columns - 1)
            {
                l1 = 360 - i2;
                g.fillArc(i, byte0, j, k, k1, -l1);
                g.setColor(Color.black);
                g.drawArc(i, byte0, j, k, k1, -l1);
                lx = (int)((double)cx + (double)j1 * Math.cos(((float)j2 * 3.14F) / 180F - 1.57F));
                ly = (int)((double)cy + (double)j1 * Math.sin(((float)j2 * 3.14F) / 180F - 1.57F));
                adjustLabel(k2);
                int l2 = ly + 15;
                if(showLabel)
                {
                    g.drawString(labels[k2], lx, ly);
                    if(showDegrees)
                        g.drawString(String.valueOf(degree[k2]) + "\260", lx, l2);
                    else
                    if(showRadians)
                        g.drawString(String.valueOf(radian[k2]) + "\u03C0 rad", lx, l2);
                    else
                    if(showPercent)
                        g.drawString(String.valueOf(percent[k2]) + " %", lx, l2);
                    else
                    if(showInput)
                        g.drawString(String.valueOf(rawinput[k2]) + "  " + unit, lx, l2);
                } else
                if(showDegrees)
                    g.drawString(String.valueOf(degree[k2]) + "\260", lx, l2);
                else
                if(showRadians)
                    g.drawString(String.valueOf(radian[k2]) + "\u03C0 rad", lx, l2);
                else
                if(showPercent)
                    g.drawString(String.valueOf(Math.round((float)round * percent[k2]) / round) + " %", lx, l2);
                else
                if(showInput)
                    g.drawString(String.valueOf(rawinput[k2]) + "  " + unit, lx, l2);
                break;
            }
            g.fillArc(i, byte0, j, k, k1, -l1);
            g.setColor(Color.black);
            g.drawArc(i, byte0, j, k, k1, -l1);
            i2 += l1;
            int l = (int)((double)cx + (double)j1 * Math.cos(((float)i2 * 3.14F) / 180F - 1.57F));
            int i1 = (int)((double)cy + (double)j1 * Math.sin(((float)i2 * 3.14F) / 180F - 1.57F));
            g.drawLine(cx, cy, l, i1);
            lx = (int)((double)cx + (double)j1 * Math.cos(((float)j2 * 3.14F) / 180F - 1.57F));
            ly = (int)((double)cy + (double)j1 * Math.sin(((float)j2 * 3.14F) / 180F - 1.57F));
            adjustLabel(k2);
            int i3 = ly + 15;
            if(showLabel)
            {
                g.drawString(labels[k2], lx, ly);
                if(showDegrees)
                    g.drawString(String.valueOf(degree[k2]) + "\260", lx, i3);
                else
                if(showRadians)
                    g.drawString(String.valueOf(radian[k2]) + "\u03C0 rad", lx, i3);
                else
                if(showPercent)
                    g.drawString(String.valueOf(percent[k2]) + " %", lx, i3);
                else
                if(showInput)
                    g.drawString(String.valueOf(rawinput[k2]) + "  " + unit, lx, i3);
            } else
            if(showDegrees)
                g.drawString(String.valueOf(degree[k2]) + "\260", lx, i3);
            else
            if(showRadians)
                g.drawString(String.valueOf(radian[k2]) + "\u03C0 rad", lx, i3);
            else
            if(showPercent)
                g.drawString(String.valueOf(Math.round((float)round * percent[k2]) / round) + " %", lx, i3);
            else
            if(showInput)
                g.drawString(String.valueOf(rawinput[k2]) + "  " + unit, lx, i3);
            j2 += (int)(angle[k2] / 2.0F + angle[k2 + 1] / 2.0F);
            k1 += -l1;
        }

        g.setColor(Color.black);
        g.drawLine(cx, cy, cx, cy - j1);
    }

    private void adjustLabel(int i)
    {
        if(lx > cx && ly < cy)
        {
            lx += 5;
            ly -= 5;
        }
        if(lx > cx && ly > cy)
        {
            lx += 5;
            ly += 10;
        }
        if(lx < cx && ly > cy)
        {
            strWidth = fontMetrics.stringWidth(labels[i]);
            lx -= strWidth + 5;
            if(lx < 0)
                lx = 0;
        }
        if(lx < cx && ly < cy)
        {
            strWidth = fontMetrics.stringWidth(labels[i]);
            lx -= strWidth + 5;
            if(lx < 0)
                lx = 0;
        }
    }

    public String ReadApplet()
    {
        int i = values.length;
        String s = "";
        if(i == 0)
        {
            s = "error empty answer";
        } else
        {
            String s1 = "";
            for(int j = 0; j < i; j++)
            {
                String s2 = labels[j];
                float f = rawinput[j];
                s = s + "segment " + j + " [ " + s2 + " ] = " + f + "\n";
            }

        }
        return s;
    }

    public void SetApplet(String s)
    {
        String s1 = "";
        StringTokenizer stringtokenizer = new StringTokenizer(s, ",");
        int i = stringtokenizer.countTokens();
        float af[] = new float[i];
        float f2 = 0.0F;
        for(int j = 0; j < i; j++)
        {
            String s2 = stringtokenizer.nextToken();
            if(s2.length() == 0)
                s2 = "0";
            float f = Float.valueOf(s2).floatValue();
            System.out.println("input = " + f + "\n");
            rawinput[j] = f;
            af[j] = f;
            f2 += f;
        }

        for(int k = 0; k < i; k++)
        {
            float f1 = af[k];
            angle[k] = (360F * f1) / f2;
            percent[k] = (100F * f1) / f2;
            radian[k] = f1 / f2;
            degree[k] = (360F * f1) / f2;
        }

        repaint();
    }

    String title;
    Font font;
    FontMetrics fontMetrics;
    int titleHeight;
    int columns;
    public int round;
    public float values[];
    Color colors[];
    public String labels[];
    public float percent[];
    public float angle[];
    public float degree[];
    public float radian[];
    public float rawinput[];
    int maxLabelWidth;
    int maxValueWidth;
    int max;
    int strWidth;
    public boolean showLabel;
    public boolean showPercent;
    public boolean showRadians;
    public boolean showDegrees;
    public boolean showInput;
    String unit;
    int lx;
    int ly;
    int cx;
    int cy;
    int smallfont,largefont;
    int alpha;
}
    

