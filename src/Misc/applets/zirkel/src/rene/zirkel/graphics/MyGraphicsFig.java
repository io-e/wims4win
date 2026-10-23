package rene.zirkel.graphics;

import java.awt.*;
import java.awt.image.*;
import java.io.*;

import rene.gui.Global;
import rene.zirkel.ZirkelFrame;
import rene.zirkel.objects.*;

/**
You need to provide a font measurement system (in x,y coordinates)
*/

class MyFontMetrics extends FontMetrics
{	public MyFontMetrics ()
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

public class MyGraphicsFig extends MyGraphics
{
	PrintWriter Out; 
	final static int STROKE_NORMAL=1,STROKE_THIN=1,STROKE_THICK=2; 
	final static int STYLE_NORMAL=0,STYLE_THIN=1,STYLE_THICK=0; 
	int Stroke=STROKE_NORMAL; 
	int Red,Green,Blue; 
	MyFontMetrics MFM; 
	int yoffset=6000; 
	String[] FigColorsArray; 
	int FigLastcolor=31; 
	int FigColor=0; 
	int FigFillcolor=0; 
	int FigLinestyle=STYLE_NORMAL; 
	int FigLayer=10000; 
	boolean PushLayer=true;
	int xfactor=15; 
	int yfactor=15; 
	StringBuffer FigFirstpart; 
	StringBuffer FigLastpart; 
	
	public MyGraphicsFig (PrintWriter out, int w, int h)
	{
		FigFirstpart=new StringBuffer(); 
		FigLastpart=new StringBuffer(); 
		//	FigFirstpart.append("#primera linea\n");
		//	FigFirstpart.append("#segona linea\n");
		FigFirstpart.append("#FIG 3.2\n"); 
		FigFirstpart.append("Landscape\n"); 
		FigFirstpart.append("Center\n"); 
		FigFirstpart.append("Metric\n"); 
		FigFirstpart.append("A4\n"); 
		FigFirstpart.append("100.00\n"); 
		FigFirstpart.append("Single\n"); 
		FigFirstpart.append("-2\n"); 
		FigFirstpart.append("1200 2\n"); 
		MFM=new MyFontMetrics(); 
		Out=out; 
		FigColorsArray=new String[543]; 
	}
	
	public void close ()
	{
		append("#End\n"); 
		Out.print(FigFirstpart); 
		Out.print(FigLastpart); 
	}
	
	String ColorString (Color c)
	{
		int r,g,b; 
		String str,stg,stb,coded="#"; 
		str=Integer.toHexString(c.getRed()); 
		if (str.length()==1) {
			str="0"+str; 
		}
		stg=Integer.toHexString(c.getGreen()); 
		if (stg.length()==1) {
			stg="0"+stg; 
		}
		stb=Integer.toHexString(c.getBlue()); 
		if (stb.length()==1) {
			stb="0"+stb; 
		}
		//	coded.concat(str);//.concat(stg).concat(stb);
		coded+=str; 
		coded+=stg; 
		coded+=stb; 
		//	append(coded);
		return coded; 
	}
	
	
	public void setColor (Color c)
	{
		String codedcolor=ColorString(c); 
		boolean newcolor=true; 
		for (int i=32; i<= FigLastcolor; i++) {
			if (codedcolor.equals(FigColorsArray[i])) {
				FigColor=i; 
				newcolor=false; 
				break; 
			}
		}
		if (newcolor) {
			FigLastcolor+=1; 
			FigColor=FigLastcolor; 
			FigColorsArray[FigLastcolor]=codedcolor; 
			FigFirstpart.append("0 "); 
			//	    Out.print("0 ");
			FigFirstpart.append(FigLastcolor); 
			FigFirstpart.append(" "); 
			//	    Out.print(FigLastcolor);Out.print(" ");
			FigFirstpart.append(codedcolor); 
			FigFirstpart.append("\n"); 
			//	    Out.println(codedcolor);
		}
	}
	
	public void setFillcolor (Color c)
	{
		String codedcolor=ColorString(c); 
		boolean newcolor=true; 
		for (int i=32; i<= FigLastcolor; i++) {
			if (codedcolor.equals(FigColorsArray[i])) {
				FigFillcolor=i; 
				newcolor=false; 
				break; 
			}
		}
		if (newcolor) {
			FigLastcolor+=1; 
			FigFillcolor=FigLastcolor; 
			FigColorsArray[FigLastcolor]=codedcolor; 
			FigFirstpart.append("0 "); 
			//	    Out.print("0 ");
			FigFirstpart.append(FigLastcolor); 
			FigFirstpart.append(" "); 
			//	    Out.print(FigLastcolor);Out.print(" ");
			FigFirstpart.append(codedcolor); 
			FigFirstpart.append("\n"); 
			//	    Out.println(codedcolor);
		}
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
				FigLinestyle=STYLE_THIN; 
			}
			else if (o.getColorType()==ConstructionObject.THICK)
			{
				Stroke=STROKE_THICK; 
				FigLinestyle=STYLE_THICK; 
			}
			else
			{
				Stroke=STROKE_NORMAL; 
				FigLinestyle=STYLE_NORMAL; 
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
	{
		append("# drawRect"); 
		append("\n"); 
		append("2 2 "); 
		//	append(FigLinestyle);append(" ");
		append(0); append(" "); 
		append(Stroke); append(" "); 
		append(FigColor); append(" "); 
		append(FigFillcolor); append(" "); 
		append(pushLayer()); 
		append(" 0 -1 0.000 0 0 -1 0 0 5"); 
		append("\n"); 
		append(xfactor*x); append(" "); 
		append(yfactor*y); append(" "); 
		append(xfactor*(x+w)-1); append(" "); 
		append(yfactor*y); append(" "); 
		append(xfactor*(x+w)-1); append(" "); 
		append((yfactor*(y+h)-1)); append(" "); 
		append(xfactor*x); append(" "); 
		append((yfactor*(y+h)-1)); append(" "); 
		append(xfactor*x); append(" "); 
		append(yfactor*y); 
		append("\n"); 
	}
	
	public void drawLine (double x, double y, double x1, double y1)
	{
		append("# drawLine"); 
		append("\n"); 
		append("2 1 "); 
		//	append(FigLinestyle);append(" ");
		append(0); append(" "); 
		append(Stroke); append(" "); 
		append(FigColor); append(" "); 
		append(FigFillcolor); append(" "); 
		append(pushLayer()); 
		append(" 0 -1 0.000 0 0 -1 0 0 2"); 
		append("\n"); 
		//	append("2 2 0 1 0 1 50 0 20 0.000 0 0 -1 0 0 2");
		append(xfactor*x); append(" "); 
		append(yfactor*y); append(" "); 
		append(xfactor*x1); append(" "); 
		append(yfactor*y1); 
		append("\n"); 
	}
	
	public void drawThickLine (double x, double y, double x1, double y1)
	{
		append("# drawThickLine"); 
		append("\n"); 
		append("2 1 "); 
		//	append(FigLinestyle);append(" ");
		append(0); append(" "); 
		append(STROKE_THICK); append(" "); 
		append(FigColor); append(" "); 
		append(FigFillcolor); append(" "); 
		append(pushLayer()); 
		append(" 0 -1 0.000 0 0 -1 0 0 2"); 
		append("\n"); 
		//	append("2 2 0 1 0 1 50 0 20 0.000 0 0 -1 0 0 2");
		append(xfactor*x); append(" "); 
		append(yfactor*y); append(" "); 
		append(xfactor*x1); append(" "); 
		append(yfactor*y1); 
		append("\n"); 
	}
	
	public void drawLine (double x, double y, double x1, double y1, ConstructionObject o)
	{
		append("# drawLine ConstructionObject"); 
		append("\n"); 
		append("2 1 "); 
		append(FigLinestyle); append(" "); 
		append(Stroke); append(" "); 
		append(FigColor); append(" "); 
		append(FigFillcolor); append(" "); 
		append(pushLayer()); 
		append(" 0 -1 0.000 0 0 -1 0 0 2"); 
		append("\n"); 
		//	append("2 1 0 1 0 1 50 0 -1 0.000 0 0 -1 0 0 2");
		append(xfactor*x); append(" "); 
		append(yfactor*y); append(" "); 
		append(xfactor*x1); append(" "); 
		append(yfactor*y1); 
		append("\n"); 
	}
	
	
	public void drawArc (double x, double y, double w, double h, double a, double b)
	{
		append("# drawArc"); 
		append("\n"); 
		int x1,x2,x3,y1,y2,y3; 
		double cx,cy,rx,ry,arad,brad; 
		rx=(double)(w)/2; 
		ry=(double)(h)/2; 
		cx=x+rx; 
		cy=y+ry; 
		arad=a*Math.PI/180.0; 
		brad=b*Math.PI/180.0; 
		x1=(int)(cx+rx*Math.cos(arad)); 
		y1=(int)(cy+ry*Math.sin(arad)); 
		x2=(int)(cx+rx*Math.cos(arad+brad/2)); 
		y2=(int)(cy+ry*Math.sin(arad+brad/2)); 
		x3=(int)(cx+rx*Math.cos(arad+brad)); 
		y3=(int)(cy+ry*Math.sin(arad+brad)); 
		append("5 1 "); 
		append(FigLinestyle); append(" "); 
		append(Stroke); append(" "); 
		append(FigColor); append(" "); 
		append(FigFillcolor); append(" "); 
		append(pushLayer()); 
		append(" 0 -1 0.000 0 1 0 0 "); 
		append(xfactor*cx); append(" "); 
		append(yfactor*cy); append(" "); 
		append(xfactor*x1); append(" "); 
		append(yfactor*y1); append(" "); 
		append(xfactor*x2); append(" "); 
		append(yfactor*y2); append(" "); 
		append(xfactor*x3); append(" "); 
		append(yfactor*y3); 
		append("\n"); 
	}
	
	public void drawArc (double x, double y, double w, double h, double a, double b,
		ConstructionObject o)
	{
		append("# drawArc ConstructionObject"); 
		append("\n"); 
		int x1,x2,x3,y1,y2,y3; 
		double cx,cy,rx,ry,arad,brad; 
		rx=(double)(w)/2; 
		ry=(double)(h)/2; 
		cx=x+rx; 
		cy=y+ry; 
		arad=a*Math.PI/180.0; 
		brad=b*Math.PI/180.0; 
		x1=(int)(cx+rx*Math.cos(arad)); 
		y1=(int)(cy-ry*Math.sin(arad)); 
		x2=(int)(cx+rx*Math.cos(arad+brad/2)); 
		y2=(int)(cy-ry*Math.sin(arad+brad/2)); 
		x3=(int)(cx+rx*Math.cos(arad+brad)); 
		y3=(int)(cy-ry*Math.sin(arad+brad)); 
		append("5 1 "); 
		append(FigLinestyle); append(" "); 
		append(Stroke); append(" "); 
		append(FigColor); append(" "); 
		append(FigColor); append(" "); 
		append(pushLayer()); 
		append(" 0 -1 0.000 0 1 0 0 "); 
		//	append("5 1 0 1 0 7 50 0 -1 0.000 0 1 0 0 ");
		append(xfactor*cx); append(" "); 
		append(yfactor*cy); append(" "); 
		append(xfactor*x1); append(" "); 
		append(yfactor*y1); append(" "); 
		append(xfactor*x2); append(" "); 
		append(yfactor*y2); append(" "); 
		append(xfactor*x3); append(" "); 
		append(yfactor*y3); 
		append("\n"); 
	}
	
	public FontMetrics getFontMetrics ()
	{	return MFM; 
	}
	
	public void drawString (String s, double x, double y)
	{
		append("#drawString"); 
		append("\n"); 
		append("4 0 "); 
		append(FigColor); append(" "); 
		append(pushLayer()); 
		append(" 0 4 12 0.000 2 0 0 "); 
		append(xfactor*x); append(" "); 
		append(yfactor*(y+10)); append(" "); 
		append(s); 
		append("\\001"); 
		append("\n"); 
	}
	
	public void drawOval (double x, double y, double w, double h)
	{
		double cx,cy,rx,ry; 
		rx=(double)(w)/2; 
		ry=(double)(h)/2; 
		cx=x+rx; 
		cy=y+ry; 
		append("#drawOval"); 
		append("\n"); 
		append("1 1 "); 
		append(0); append(" "); 
		append(Stroke); append(" "); 
		append(FigColor); append(" "); 
		append(FigFillcolor); append(" "); 
		append(pushLayer()); 
		append(" 0 -1 0.000 1 0.000 "); 
		append((int)(xfactor*cx)); append(" "); 
		append((int)(yfactor*cy)); append(" "); 
		append((int)(xfactor*rx)); append(" "); 
		append((int)(yfactor*ry)); append(" "); 
		append("0 0 0 0"); 
		append("\n"); 
	}
	
	public void drawOval (double x, double y, double w, double h,
		ConstructionObject o)
	{
		double cx,cy,rx,ry; 
		rx=(double)(w)/2; 
		ry=(double)(h)/2; 
		cx=x+rx; 
		cy=y+ry; 
		append("#drawOval ConstructionObject"); 
		append("\n"); 
		append("1 1 "); 
		append(FigLinestyle); append(" "); 
		append(Stroke); append(" "); 
		append(FigColor); append(" "); 
		append(FigFillcolor); append(" "); 
		append(pushLayer()); 
		append(" 0 -1 0.000 1 0.000 "); 
		append((int)(xfactor*cx)); append(" "); 
		append((int)(yfactor*cy)); append(" "); 
		append((int)(xfactor*rx)); append(" "); 
		append((int)(yfactor*ry)); append(" "); 
		append("0 0 0 0"); 
		append("\n"); 
	}
	
	
	public void fillRect (double x, double y, double w, double h,
		boolean outline, boolean transparent, ConstructionObject o)
	{	
		setColor(o);
		setFillColor(o);
		append("# fillRect"); 
		append("\n"); 
		append("2 2 "); 
		append(outline?FigLinestyle:STYLE_NORMAL); append(" "); 
		append(Stroke); append(" "); 
		append(FigColor); append(" "); 
		append(FigFillcolor); append(" "); 
		append(pushLayer()); 
		if (transparent) append(" 0 20 0.000 0 0 -1 0 0 5"); 
		else append(" 0 30 0.000 0 0 -1 0 0 5"); 
		append("\n"); 
		append(xfactor*x); append(" "); 
		append(yfactor*y); append(" "); 
		append(xfactor*(x+w)-1); append(" "); 
		append(yfactor*y); append(" "); 
		append(xfactor*(x+w)-1); append(" "); 
		append(yfactor*(y+h)-1); append(" "); 
		append(xfactor*x); append(" "); 
		append(yfactor*(y+h)-1); append(" "); 
		append(xfactor*x); append(" "); 
		append(yfactor*y); 
		append("\n"); 
	}
	
	public void fillArc (double x, double y, double w, double h, double a, double b,
		boolean outline, boolean transparent, boolean arc, ConstructionObject o)
	{
		setColor(o);
		setFillColor(o);
		append("#fillArc2"); 
		append("\n"); 
		int x1,x2,x3,y1,y2,y3; 
		double cx,cy,rx,ry,arad,brad; 
		rx=(double)(w-1)/2; 
		ry=(double)(h-1)/2; 
		cx=x+rx; 
		cy=y+ry; 
		arad=a*Math.PI/180.0; 
		brad=b*Math.PI/180.0; 
		x1=(int)(cx+rx*Math.cos(arad)); 
		y1=(int)(cy-ry*Math.sin(arad)); 
		x2=(int)(cx+rx*Math.cos(arad+brad/2)); 
		y2=(int)(cy-ry*Math.sin(arad+brad/2)); 
		x3=(int)(cx+rx*Math.cos(arad+brad)); 
		y3=(int)(cy-ry*Math.sin(arad+brad)); 
		append("5 2 "); 
		append(outline?FigLinestyle:STYLE_NORMAL); append(" "); 
		append(Stroke); append(" "); 
		append(FigColor); append(" "); 
		append(FigFillcolor); append(" "); 
		append(pushLayer()); 
		if (transparent) append(" 0 20 0.000 0 1 0 0 "); 
		else append(" 0 30 0.000 0 1 0 0 "); 
		append(xfactor*cx); append(" "); 
		append(yfactor*cy); append(" "); 
		append(xfactor*x1); append(" "); 
		append(yfactor*y1); append(" "); 
		append(xfactor*x2); append(" "); 
		append(yfactor*y2); append(" "); 
		append(xfactor*x3); append(" "); 
		append(yfactor*y3); 
		append("\n"); 
	}
	
	public void fillOval (double x, double y, double w, double h,
		boolean outline, boolean transparent, ConstructionObject o)
	{
		setColor(o);
		setFillColor(o);
		append("#fillOval"); 
		append("\n"); 
		double cx,cy,rx,ry; 
		rx=(double)(w-1)/2; 
		ry=(double)(h-1)/2; 
		cx=x+rx; 
		cy=y+ry; 
		append("1 1 "); 
		append(outline?FigLinestyle:STYLE_NORMAL); append(" "); 
		append(Stroke); append(" "); 
		append(FigColor); append(" "); 
		append(FigFillcolor); append(" "); 
		append(pushLayer()); 
		if (transparent) append(" 0 20 0.000 1 0.000 "); 
		else append(" 0 30 0.000 1 0.000 "); 
		append((int)(xfactor*cx)); append(" "); 
		append((int)(yfactor*cy)); append(" "); 
		append((int)(xfactor*rx)); append(" "); 
		append((int)(yfactor*ry)); append(" "); 
		append("0 0 0 0"); 
		append("\n"); 
	}
	
	public void fillPolygon (double x[], double y[], int n,
		boolean outline, boolean transparent, ConstructionObject o)
	{
		setColor(o);
		setFillColor(o);
		append("#fillPolygon"); 
		append("\n"); 
		append("2 1 "); 
		append(outline?FigLinestyle:STYLE_NORMAL);
			append(" "); 
		append(Stroke); append(" "); 
		append(FigColor); append(" "); 
		append(FigFillcolor); append(" "); 
		append(pushLayer()); 
		if (transparent) append(" 0 20 0.000 0 0 -1 0 0 "); 
		else append(" 0 30 0.000 0 0 -1 0 0 "); 
		append(n+1); 
		append("\n"); 
		for (int i=0; i < n; i++) {
			append(xfactor*x[i]); append(" "); 
			append(yfactor*y[i]); append(" "); 
		}
		append(xfactor*x[0]); append(" "); 
		append(yfactor*y[0]); append(" "); 
		append("\n"); 
	}
	
	public void setLayer (int n)
	{	FigLayer=n; 
	}

	void append (String s)
	{	FigLastpart.append(s);
	}
	void append (int n)
	{	FigLastpart.append(""+n);
	}
	void append (double x)
	{	FigLastpart.append(""+((int)x));
	}
	
	public int pushLayer ()
	{	if (PushLayer) FigLayer--;
		return FigLayer;
	}

	public void pushLayer (boolean flag)
	{	PushLayer=flag;
	}

	public void drawImage (Image i, int x, int y, ImageObserver o)
	{
	}
	public void drawImage (Image i, int x, int y, int w, int h, ImageObserver o)
	{
	}

	public void setFont (int h, boolean bold)
	{
	}

	int fsize;
	boolean flarge,fbold;
	int ffactor=Global.getParameter("ffactor",130);
	
	public void setDefaultFont (int h, boolean large, boolean bold)
	{	fsize=h; flarge=large; fbold=bold;
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
