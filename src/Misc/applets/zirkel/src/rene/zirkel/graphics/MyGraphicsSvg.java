package rene.zirkel.graphics;

import java.awt.*;
import java.awt.image.*;
import java.io.*;

import rene.util.xml.*;
import rene.zirkel.ZirkelFrame;
import rene.zirkel.objects.*;
import rene.gui.Global;

class SvgFontMetrics extends FontMetrics
{	public SvgFontMetrics ()
	{	super(new Font("Courier",10,Font.PLAIN)); // a dummy font.
	}
	
	public int stringWidth (String s)
	{	return s.length()*10; 
	}
	
	public int getHeight()
	{	return 12; 
	}
	
	public int getAscent()
	{	return 2; 
	}
	
}

/**
This is the Graphics class, you need to implement.
*/

public class MyGraphicsSvg extends MyGraphics
{	int W,H;

	PrintWriter Out; 
	final static int STROKE_NORMAL=0,STROKE_THIN=1,STROKE_THICK=2; 
	final static int STYLE_NORMAL=0,STYLE_THIN=1,STYLE_THICK=0; 
	int Stroke=STROKE_NORMAL; 
	Color col,fillcol; 
	SvgFontMetrics MFM=new SvgFontMetrics(); 
	int yoffset=6000;
	int FontH=12; 
	
	SVGWriter svg;
	
	public MyGraphicsSvg (PrintWriter out, int w, int h)
	{	svg=new SVGWriter(out);
		svg.startSVG(w,h);
		W=w; H=h; 
	}
	
	public void close ()
	{	svg.endTagNewLine("svg");
	}
	
	public void setColor (Color c)
	{	col=c;
	}
	
	public void setFillcolor (Color c)
	{	fillcol=c;
	}

	public void setColor (ConstructionObject o)
	{
		if (o.isJobTarget()) setColor(ZirkelFrame.TargetColor); 
		else if (o.selected()) setColor(ZirkelFrame.SelectColor); 
		else
		{	if (o.getColorType()==ConstructionObject.THIN)
			{	int i=o.getColorIndex(); 
				if (o.isHidden()) setColor(ZirkelFrame.BrighterLightColors[i]); 
				else setColor(ZirkelFrame.LightColors[i]); 
			}
			else
			{	int i=o.getColorIndex(); 
				if (o.isHidden()) setColor(ZirkelFrame.BrighterColors[i]); 
				else setColor(ZirkelFrame.Colors[i]); 
			}
			if (o.getColorType()==ConstructionObject.THIN)
			{
				Stroke=STROKE_THIN; 
			}
			else if (o.getColorType()==ConstructionObject.THICK)
			{
				Stroke=STROKE_THICK; 
			}
			else
			{
				Stroke=STROKE_NORMAL; 
			}
		}
	}
	
	public void setFillColor (ConstructionObject o)
	{	if (o.isJobTarget()) setFillcolor(ZirkelFrame.TargetColor); 
		else
		{	if (o.getColorType()!=ConstructionObject.THICK)
			{	int i=o.getColorIndex(); 
				if (o.isHidden()) setFillcolor(ZirkelFrame.BrighterLightColors[i]); 
				else setFillcolor(ZirkelFrame.LightColors[i]); 
			}
			else
			{	int i=o.getColorIndex(); 
				if (o.isHidden()) setFillcolor(ZirkelFrame.BrighterColors[i]); 
				else setFillcolor(ZirkelFrame.Colors[i]); 
			}
		}
	}
	
	public void setLabelColor (ConstructionObject o)
	{	if (o.labelSelected()) setColor(ZirkelFrame.SelectColor);
		else if (o.isFilled())
		{	int type=o.getColorType();
			o.setColorType(ConstructionObject.NORMAL);
			setColor(o);
			o.setColorType(type);
		}
		else setColor(o);
	}

	public void clearRect (int x, int y, int w, int h, Color c) {}
	
	public void drawRect (double x, double y, double w, double h)
	{	svg.startTagStart("path");
		w--; h--;
		svg.printArg("d","M "+x+" "+y+" H "+(x+w)+
			" V "+(y+h)+" H "+x+" Z");
		svg.printArg("style","fill:none;stroke:"+rgb()+";stroke-width:1");
		svg.finishTagNewLine();	
	}
	
	public String rgb ()
	{	return "rgb("+col.getRed()+","+col.getGreen()+","+col.getBlue()+")";
	}
	
	public String frgb ()
	{	return "rgb("+fillcol.getRed()+","+fillcol.getGreen()+","+fillcol.getBlue()+")";
	}
	
	public void drawLine (double x, double y, double x1, double y1)
	{	svg.startTagStart("line");
		svg.printArg("x1",""+x);
		svg.printArg("y1",""+y);
		svg.printArg("x2",""+x1);
		svg.printArg("y2",""+y1);
		switch (Stroke)		
		{	case STROKE_NORMAL :
				svg.printArg("style","stroke:"+rgb()+";stroke-width:1");
				break;
			case STROKE_THICK :
				svg.printArg("style","stroke:"+rgb()+";stroke-width:3");
				break;
			case STROKE_THIN :
				svg.printArg("style","stroke:"+rgb()+";stroke-width:1;fill:none;stroke-dasharray:5,5");
				break;
		}
		svg.finishTagNewLine();
	}
	
	public void drawThickLine (double x, double y, double x1, double y1)
	{	svg.startTagStart("line");
		svg.printArg("x1",""+x);
		svg.printArg("y1",""+y);
		svg.printArg("x2",""+x1);
		svg.printArg("y2",""+y1);
		svg.printArg("style","stroke:"+rgb()+";stroke-width:3");
		svg.finishTagNewLine();
	}
	
	public void drawLine (double x, double y, double x1, double y1, ConstructionObject o)
	{	svg.startTagStart("line");
		svg.printArg("x1",""+x);
		svg.printArg("y1",""+y);
		svg.printArg("x2",""+x1);
		svg.printArg("y2",""+y1);
		svg.printArg("style","stroke:"+rgb()+";stroke-width:1");
		svg.finishTagNewLine();
	}
	
	public void drawArc (double x, double y, double w, double h, double a, double b)
	{	double x0=Math.round(x+w/2.0+w*Math.cos(a/180*Math.PI)/2);
		double y0=Math.round(y+h/2.0-h*Math.sin(a/180*Math.PI)/2);
		double x1=Math.round(x+w/2.0+w*Math.cos((a+b)/180*Math.PI)/2);
		double y1=Math.round(y+h/2.0-h*Math.sin((a+b)/180*Math.PI)/2);
		int f=0;
		if (b>180) f=1;
		svg.startTagStart("path");
		svg.printArg("d","M "+x0+" "+y0+" A "+(w/2)+" "+(h/2)+" 0 "+f+" 0 "+x1+" "+y1);
		svg.printArg("style","stroke:"+rgb()+";stroke-width:1;fill:none");
		svg.finishTagNewLine();
	}
	
	public void drawArc (double x, double y, double w, double h, double a, double b,
		ConstructionObject o)
	{	w=w/2; h=h/2; x+=w; y+=h;
		double x0=Math.round(x+w*Math.cos(a/180*Math.PI));
		double y0=Math.round(y-h*Math.sin(a/180*Math.PI));
		double x1=Math.round(x+w*Math.cos((a+b)/180*Math.PI));
		double y1=Math.round(y-h*Math.sin((a+b)/180*Math.PI));
		int f=0;
		if (b>180) f=1;
		svg.startTagStart("path");
		svg.printArg("d","M "+x0+" "+y0+" A "+w+" "+h+" 0 "+f+" 0 "+x1+" "+y1);
		switch (Stroke)		
		{	case STROKE_NORMAL :
				svg.printArg("style","stroke:"+rgb()+";stroke-width:1;fill:none");
				break;
			case STROKE_THICK :
				svg.printArg("style","stroke:"+rgb()+";stroke-width:3;fill:none");
				break;
			case STROKE_THIN :
				svg.printArg("style","stroke:"+rgb()+";stroke-width:1;fill:none;stroke-dasharray:5,5");
				break;
		}
		svg.finishTagNewLine();
	}
	
	public FontMetrics getFontMetrics ()
	{	return MFM; 
	}
	
	public void drawString (String s, double x, double y)
	{	svg.startTagStart("text");
		svg.printArg("x",""+x);
		svg.printArg("y",""+(y+10));
		svg.printArg("style","font-size:"+FontH+";fill:"+rgb()+";font-weight:"+
			(Global.getParameter("font.bold",false)?"gold":"normal"));
		svg.startTagEnd();
		svg.print(s);
		svg.endTag("text");
	}
	
	public void drawOval (double x, double y, double w, double h)
	{	svg.startTagStart("ellipse");
		w=w/2; h=h/2; x+=w; y+=h;
		svg.printArg("cx",""+x);
		svg.printArg("cy",""+y);
		svg.printArg("rx",""+w);
		svg.printArg("ry",""+h);
 		svg.printArg("style","stroke:"+rgb()+";stroke-width:1;fill:none");
		svg.finishTagNewLine();
	}
	
	public void drawOval (double x, double y, double w, double h,
		ConstructionObject o)
	{	svg.startTagStart("ellipse");
		w=w/2; h=h/2; x+=w; y+=h;
		svg.printArg("cx",""+x);
		svg.printArg("cy",""+y);
		svg.printArg("rx",""+w);
		svg.printArg("ry",""+h);
 		switch (Stroke)		
		{	case STROKE_NORMAL :
				svg.printArg("style","stroke:"+rgb()+";fill:none;stroke-width:1");
				break;
			case STROKE_THICK :
				svg.printArg("style","stroke:"+rgb()+";fill:none;stroke-width:3");
				break;
			case STROKE_THIN :
				svg.printArg("style","stroke:"+rgb()+";stroke-width:1;fill:none;stroke-dasharray:5,5");
				break;
		}
		svg.finishTagNewLine();
	}
		
	public void fillRect (double x, double y, double w, double h,
		boolean outline, boolean transparent, ConstructionObject o)
	{	svg.startTagStart("path");
		w--; h--;
		svg.printArg("d","M "+x+" "+y+" H "+(x+w)+
			" V "+(y+h)+" H "+x+" Z");
		svg.printArg("style","fill:"+rgb()+";stroke:"+rgb()+";stroke-width:1");
		svg.finishTagNewLine();	
	}
	
	public void fillArc (double x, double y, double w, double h, double a, double b,
		boolean outline, boolean transparent, boolean arc, ConstructionObject o)
	{	setFillColor(o);
		int x0=(int)(Math.round(x+w/2.0+w*Math.cos(a/180*Math.PI)/2));
		int y0=(int)(Math.round(y+h/2.0-h*Math.sin(a/180*Math.PI)/2));
		int x1=(int)(Math.round(x+w/2.0+w*Math.cos((a+b)/180*Math.PI)/2));
		int y1=(int)(Math.round(y+h/2.0-h*Math.sin((a+b)/180*Math.PI)/2));
		int f=0;
		if (b>180) f=1;
		svg.startTagStart("path");
		if (arc)
			svg.printArg("d","M "+(x+w/2)+" "+(y+h/2)+
				" L "+x0+" "+y0+
				" A "+(w/2)+" "+(h/2)+" 0 "+f+" 0 "+x1+" "+y1+
				" L "+(x+w/2)+" "+(y+h/2));
		else
			svg.printArg("d",
				"M "+x0+" "+y0+
				" A "+(w/2)+" "+(h/2)+" 0 "+f+" 0 "+x1+" "+y1+
				" L "+x0+" "+y0);			
 		switch (Stroke)		
		{	case STROKE_NORMAL :
				svg.printArg("style","fill:"+frgb()+
					(transparent && !o.isSolid()?";fill-opacity:0.5":""));
				break;
			case STROKE_THICK :
				svg.printArg("style","fill:"+frgb());
				break;
			case STROKE_THIN :
				svg.printArg("style","fill:"+frgb()+
					(transparent && !o.isSolid()?";fill-opacity:0.5":""));
				break;
		}
		svg.finishTagNewLine();
		if (outline)
		{	setColor(o);
			drawArc(x,y,w,h,a,b);
		}
	}
 
	public void fillOval (double x, double y, double w, double h,
		boolean outline, boolean transparent, ConstructionObject o)
	{	setFillColor(o);
		svg.startTagStart("ellipse");
		svg.printArg("cx",""+(x+w/2));
		svg.printArg("cy",""+(y+h/2));
		svg.printArg("rx",""+(w/2));
		svg.printArg("ry",""+(h/2));
 		switch (Stroke)		
		{	case STROKE_NORMAL :
				svg.printArg("style","fill:"+frgb()+
					(transparent && !o.isSolid()?";fill-opacity:0.5":""));
				break;
			case STROKE_THICK :
				svg.printArg("style","fill:"+frgb());
				break;
			case STROKE_THIN :
				svg.printArg("style","fill:"+frgb()+
					(transparent && !o.isSolid()?";fill-opacity:0.5":""));
				break;
		}
		svg.finishTagNewLine();
		if (outline)
		{	setColor(o);
			drawOval(x,y,w,h);
		}
	}
	
	public void fillPolygon (double x[], double y[], int n,
		boolean outline, boolean transparent, ConstructionObject o)
	{	setFillColor(o);
		svg.startTagStart("path");
		String s="M "+x[0]+" "+y[0];
		for (int i=1; i<n; i++)
			s=s+" L "+x[i]+" "+y[i];
		s=s+" L "+x[0]+" "+y[0];
		svg.printArg("d",s);
 		switch (Stroke)		
		{	case STROKE_NORMAL :
				svg.printArg("style","fill:"+frgb()+
					(transparent && !o.isSolid()?";fill-opacity:0.5":""));
				break;
			case STROKE_THICK :
				svg.printArg("style","fill:"+frgb());
				break;
			case STROKE_THIN :
				svg.printArg("style","fill:"+frgb()+
					(transparent && !o.isSolid()?";fill-opacity:0.5":""));
				break;
		}
		svg.finishTagNewLine();
		if (outline)
		{	svg.startTagStart("path");
			svg.printArg("d",s);
			setColor(o);
			svg.printArg("style","fill:none;stroke:"+rgb()+";stroke-width:1");
			svg.finishTagNewLine();
		}
	}
	
	public void drawImage (Image i, int x, int y, ImageObserver o)
	{
	}
	public void drawImage (Image i, int x, int y, int w, int h, ImageObserver o)
	{
	}

	public void setFont (int h, boolean bold)
	{	FontH=h;
	}

	int fsize;
	boolean flarge,fbold;
	int ffactor=Global.getParameter("ffactor",130);
	
	public void setDefaultFont (int h, boolean large, boolean bold)
	{	fsize=h; flarge=large; fbold=bold;
		ffactor=Global.getParameter("ffactor",130);
		setFont(large,bold);
	}
	
	public void setFont (boolean large, boolean bold)
	{	int size=fsize;
		if (large) size=size*ffactor/100;
		if (flarge) size=size*ffactor/100;
		setFont(size,bold || fbold);
	}

	public void drawImage (Image i, double x, double y, double x1, double y1, 
			double x2, double y2, ImageObserver o)
	{
	}

	public Graphics getGraphics() {
		return null;
	}

	public int stringWidth(String s) 
	{	return getFontMetrics().stringWidth(s);
	}

	public int stringHeight(String s) 
	{	return getFontMetrics().getHeight();
	}

	public int drawStringExtended(String s, double x, double y) 
	{	drawString(s,x,y+getFontMetrics().getAscent());
		return getFontMetrics().getHeight(); 
	}

	public int stringAscent(String s) 
	{	return getFontMetrics().getAscent();
	}

}
