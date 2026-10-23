package rene.zirkel.graphics;

import java.io.*;

import java.awt.*;
import java.awt.image.*;

import rene.gui.Global;
import rene.zirkel.ZirkelFrame;
import rene.zirkel.construction.Construction;
import rene.zirkel.objects.*;

class PdfFontMetrics extends FontMetrics
{	int Size;

	public PdfFontMetrics (int size)
	{	super(new Font("Courier",Font.PLAIN,size)); // a dummy font.
		Size=size;
	}

	public int stringWidth (String s)
	{	return s.length()*Size; 
	}
	
	public int getHeight()
	{	return Size; 
	}
	
	public int getAscent()
	{	return Size*4/5; 
	}
	
}

public class MyGraphicsPDF extends MyGraphics
{	PrintWriter Out;
	StringBuffer buf,xref,stream;
	int nxref=1;
	String prefix="";
	final int Normal=0,Thin=1,Thick=2;
	int Stroke=Normal;
	private PdfFontMetrics FM=new PdfFontMetrics(10);	
	boolean Bold;	
	double W,H;
	double LineWidth;

	public MyGraphicsPDF (PrintWriter out, double w, double h, double linewidth)
	{	Out=out;
		LineWidth=linewidth;
		W=w; H=h;
		buf=new StringBuffer();
		xref=new StringBuffer();
		appendxref(0,65535,false);
		appendln("%PDF-1.4");
		newObject();
		appendln("<< /Type /Catalog");
		appendln("   /Outlines 2 0 R");
		appendln("   /Pages 3 0 R");
		appendln(">>" );
		endObject(); 
		newObject();
		appendln("<< /Type /Outlines");
		appendln("   /Count 0");
		appendln(">>");
		endObject();
		newObject();
		appendln("<< /Type /Pages");
		appendln("   /Kids [4 0 R]");
		appendln("   /Count 1");
		appendln(">>");
		endObject();
		newObject();
		appendln("<< /Type /Page");
		appendln("   /Parent 3 0 R");
		appendln("   /MediaBox [0 0 "+w+" "+h+"]");
		appendln("   /Contents 5 0 R");
		appendln("   /Resources << /ProcSet 6 0 R");
		appendln("                 /Font << /F1 7 0 R");
		appendln("                          /F2 7 0 R " +
			"								/F3 7 0 R >>");
		appendln("              >>");
		appendln(">>");
		endObject();		
		newObject();
		stream=new StringBuffer();
	}

	public void close ()
	{	appendln("<< /Length "+stream.length()+" >>");
		prefix=""; appendln("stream");
		buf.append(stream.toString());
		appendln("endstream");
		endObject();
		newObject();
		appendln("[/PDF]");
		endObject();
		newObject();
		appendln("<< /Type /Font");
		appendln("   /Subtype /Type1");
		appendln("   /Name /F1");
		appendln("   /BaseFont /Helvetica");
		appendln("   /Encoding /WinAnsiEncoding");
		appendln(">>");
		endObject();
		newObject();
		appendln("<< /Type /Font");
		appendln("   /Subtype /Type1");
		appendln("   /Name /F2");
		appendln("   /BaseFont /Symbol");
		appendln("   /Encoding /WinAnsiEncoding");
		appendln(">>");
		endObject();
		newObject();
		appendln("<< /Type /Font");
		appendln("   /Subtype /Type1");
		appendln("   /Name /F3");
		appendln("   /BaseFont /Helvetica-Bold");
		appendln("   /Encoding /WinAnsiEncoding");
		appendln(">>");
		endObject();
		appendln("");
		appendln("xref");
		int xrefstart=buf.length();
		appendln(0+" "+nxref);
		buf.append(xref.toString());
		appendln("");
		appendln("trailer");
		appendln("<< /Size "+nxref);
		appendln("   /Root 1 0 R");
		appendln(">>");
		appendln("");
		appendln("startxref");
		appendln(""+xrefstart);
		appendln("");
		appendln("%%EOF");
		Out.write(buf.toString());	
	}
	
	public void appendln (String s)
	{	buf.append(prefix+s);
		buf.append((char)13); buf.append((char)10);
	}
	
	public void streamln (String s)
	{	stream.append(prefix+s);
		stream.append((char)13); stream.append((char)10);
	}
	
	public void appendxref (int offset, int number, boolean present)
	{	xref.append(format(offset,10)+" "+format(number,5)+(present?" n":" f"));
		xref.append((char)13); xref.append((char)10);
	}

	public String format (int n, int size)
	{	String s=""+n;
		while (s.length()<size) s="0"+s;
		return s;
	}
	
	public void newObject ()
	{	int n=buf.length();
		appendln("");
		appendln(nxref+" "+0+" obj");
		appendxref(n,0,true);
		nxref++;
		prefix="  ";
	}
	
	public void endObject ()
	{	prefix="";
		appendln("endobj");
	}

	public double r (double x)
	{	return Math.round(x*100.0)/100.0;
	}
	
	public double ry (double y)
	{	return Math.round((H-y)*100.0)/100.0;
	}
	
	public void setStroke (int stroke)
	{	if (Stroke==stroke) return;
		Stroke=stroke;
		switch (Stroke)
		{	case Normal :
				streamln(r(LineWidth)+" w"); 
				streamln("[] 0 d");
				break;
			case Thick :
				streamln(r(3*LineWidth)+" w"); 
				streamln("[] 0 d");
				break;
			case Thin :
				streamln(LineWidth+" w");
				streamln("["+r(3*LineWidth)+" "+r(3*LineWidth)+"] 0 d");
			break;
		}
	
	}
	
	public void clearRect (int x, int y, int w, int h, Color c){}

	Color OldColor=null;

	public void setColor (Color c)
	{	if (OldColor!=null && 
			c.getRed()==OldColor.getRed() &&
			c.getGreen()==OldColor.getGreen() &&
			c.getBlue()==OldColor.getBlue()) return;
		streamln(
			r(c.getRed()/255.0)+" "+
			r(c.getGreen()/255.0)+" "+
			r(c.getBlue()/255.0)+" rg");
		OldColor=c;
	}
	
	public void setColor (ConstructionObject o)
	{	if (o.isJobTarget()) setColor(ZirkelFrame.TargetColor);
		else if (o.indicated()) setColor(ZirkelFrame.IndicateColor);
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
		}
		if (o.getColorType()==ConstructionObject.THIN)
		{	setStroke(Thin);
		}
		else if (o.getColorType()==ConstructionObject.THICK)
		{	setStroke(Thick);
		}
		else
		{	setStroke(Normal);
		}
	}

	Color OldFillColor=null;

	public void setFillColor (Color c)
	{	if (OldFillColor!=null && 
			c.getRed()==OldFillColor.getRed() &&
			c.getGreen()==OldFillColor.getGreen() &&
			c.getBlue()==OldFillColor.getBlue()) return;
		streamln(
			r(c.getRed()/255.0)+" "+
			r(c.getGreen()/255.0)+" "+
			r(c.getBlue()/255.0)+" rg");
		OldFillColor=c;
	}
	
	public void setFillColor (ConstructionObject o)
	{	setStroke(Normal);
		if (o.isJobTarget()) setColor(ZirkelFrame.TargetColor);
		else if ((o instanceof PointObject) && o.indicated())
			setFillColor(ZirkelFrame.IndicateColor);
		else
		{	if (o.getColorType()!=ConstructionObject.THICK)
			{	int i=o.getColorIndex();
				if (o.isHidden()) setFillColor(ZirkelFrame.BrighterLightColors[i]);
				else setFillColor(ZirkelFrame.LightColors[i]);			
			}		
			else
			{	int i=o.getColorIndex();
				if (o.isHidden()) setFillColor(ZirkelFrame.BrighterColors[i]);
				else setFillColor(ZirkelFrame.Colors[i]);			
			}
		}
	}

	public void setLabelColor (ConstructionObject o)
	{	if (o.labelSelected()) setFillColor(ZirkelFrame.SelectColor);
		else if (o.isFilled())
		{	int type=o.getColorType();
			o.setColorType(ConstructionObject.NORMAL);
			int i=o.getColorIndex();
			setFillColor(ZirkelFrame.Colors[i]);			
			o.setColorType(type);
		}
		else
		{	int type=o.getColorType();
			o.setColorType(ConstructionObject.NORMAL);
			int i=o.getColorIndex();
			if (o.isHidden()) setFillColor(ZirkelFrame.BrighterColors[i]);
			else setFillColor(ZirkelFrame.Colors[i]);			
			o.setColorType(type);
		}
	}
	
	public void drawRect (double x, double y, double w, double h)
	{	streamln(r(x)+" "+ry(y+h)+" "+r(w)+" "+r(h)+" "+"re");
		streamln("S");
	}
	
	public void drawLine (double x, double y, double x1, double y1, 
		ConstructionObject o)
	{	streamln(r(x)+" "+ry(y)+" m");
		streamln(r(x1)+" "+ry(y1)+" l");
		streamln("S");
	}
	
	public void drawLine (double x, double y, double x1, double y1)
	{	setStroke(Normal);
		streamln(r(x)+" "+ry(y)+" m");
		streamln(r(x1)+" "+ry(y1)+" l");
		streamln("S");
	}
	
	public void drawThickLine (double x, double y, double x1, double y1)
	{	setStroke(Thick);
		streamln(r(x)+" "+ry(y)+" m");
		streamln(r(x1)+" "+ry(y1)+" l");
		streamln("S");
		setStroke(Normal);
	}

	public void drawArc (double x, double y, double r, double a, double b)
	{	double f=r/Math.cos(b/3);
		streamln(r(x+Math.cos(a+b/3)*f)+" "+ry(y-Math.sin(a+b/3)*f)+" "+
			r(x+Math.cos(a+2*b/3)*f)+" "+ry(y-Math.sin(a+2*b/3)*f)+" "+
			r(x+Math.cos(a+b)*r)+" "+ry(y-Math.sin(a+b)*r)+" c");			
	}

	public void drawArc (double x, double y, double w, double h, double a, double b)
	{	setStroke(Normal);
		double r=w/2;
		x+=r; y+=r;
		a=a/180*Math.PI; b=b/180*Math.PI;
		int n=(int)(r*b/10);
		if (n<4) n=4;
		streamln(r(x+Math.cos(a)*r)+" "+ry(y-Math.sin(a)*r)+" m");
		for (int i=0; i<n; i++)
		{	drawArc(x,y,r,a+i*b/n,b/n);
		}
		streamln("S");
	}

	public void drawArc (double x, double y, double w, double h, double a, double b,
		ConstructionObject o)
	{	double r=w/2;
		x+=r; y+=r;
		a=a/180*Math.PI; b=b/180*Math.PI;
		int n=(int)(r*b/10);
		if (n<4) n=4;
		streamln(r(x+Math.cos(a)*r)+" "+ry(y+-Math.sin(a)*r)+" m");
		for (int i=0; i<n; i++)
		{	drawArc(x,y,r,a+i*b/n,b/n);
		}
		streamln("S");
	}
	
	public void setFont (int size, boolean bold)
	{	Bold=bold;
		FM=new PdfFontMetrics(size); 
		streamln("/F1 "+size+" Tf");
	}

	public FontMetrics getFontMetrics ()
	{	if (FM==null) FM=new PdfFontMetrics(20); 
		return FM; 
	}

	public void drawString (String s, double x, double y)
	{	if (s.length()==1)
		{	char c=s.charAt(0);
			for (int i=0; i<Translation.length/2; i++)
			{	if (Translation[2*i+1]==c)
				{	streamln("BT");
					streamln("/F2 "+FM.Size+" Tf");
					streamln(r(x)+" "+ry(y)+" Td");
					streamln("("+Translation[2*i]+") Tj");
					streamln("ET");
					return;
				} 
			}
		}
		streamln("BT");
		if (Bold) streamln("/F3 "+FM.Size+" Tf");
		else streamln("/F1 "+FM.Size+" Tf");
		streamln(r(x)+" "+ry(y)+" Td");
		streamln("("+s+") Tj");
		streamln("ET");
	}

	public static char Translation[]=
	{	'a','\u03B1','A','\u0391',
		'b','\u03B2','B','\u0392',
		'c','\u03B3','C','\u0393',
		'd','\u03B4','D','\u0394',
		'e','\u03B5','E','\u0395',
		'f','\u03D5','F','\u03A6',
		'g','\u03B3','G','\u0393',
		'h','\u03B7','H','\u0397',
		'i','\u03B9','I','\u0399',
		'k','\u03BA','K','\u039A',
		'l','\u03BB','L','\u039B',
		'm','\u03BC','M','\u039C',
		'n','\u03BD','N','\u039D',
		'o','\u03BF','O','\u03A9',
		'p','\u03C0','P','\u03A0',
		'q','\u03C7','Q','\u03A7',
		'r','\u03C1','R','\u03A1',
		's','\u03C3','S','\u03A3',
		't','\u03C4','T','\u03A4',
		'u','\u03C5','U','\u03A5',
		'v','\u03C8','V','\u03A8',
		'w','\u03C9','W','\u03A9',
		'x','\u03BE','X','\u039E',
		'y','\u03C7','Y','\u03A7',
		'z','\u03B6','Z','\u0396',		
	};

	public void drawOval (double x, double y, double w, double h)
	{	drawArc(x,y,w,h,0,360);
	}

	public void drawOval (double x, double y, double w, double h,
		ConstructionObject o)
	{	drawArc(x,y,w,h,0,360,o);
	}

	public void fillRect (double x, double y, double w, double h, 
		boolean outline, boolean transparent, ConstructionObject o)
	{	setFillColor(o);
		if (outline) setColor(o);
		streamln(r(x)+" "+ry(y+h)+" "+r(w)+" "+r(h)+" "+"re");
		streamln(outline?"b":"f");
	}

	public void fillOval (double x, double y, double w, double h, 
		boolean outline, boolean transparent, ConstructionObject o)
	{	fillArc(x,y,w,h,0,360,outline,transparent,true,o);
	}
		
	public void fillPolygon (double x[], double y[], int n,
		boolean outline, boolean tranparent, ConstructionObject o)
	{	setFillColor(o);
		if (outline) setColor(o);
		streamln(r(x[0])+" "+ry(y[0])+" m");
		for (int i=1; i<n; i++)
		{	streamln(r(x[i])+" "+ry(y[i])+" l");
		}
		streamln(outline?"b*":"f*");
	}


	public void fillArc (double x, double y, double w, double h, double a, double b,
		boolean outline, boolean transparent, boolean arc, ConstructionObject o)
	{	setFillColor(o);
		if (outline) setColor(o);
		double r=w/2;
		x+=r; y+=r;
		a=a*Math.PI/180; b=b*Math.PI/180;
		int n=(int)(r*b/10);
		if (n<4) n=4;
		streamln(r(x+Math.cos(a)*r)+" "+ry(y-Math.sin(a)*r)+" m");
		for (int i=0; i<n; i++)
		{	drawArc(x,y,r,a+i*b/n,b/n);
		}
		if (arc)
		{	streamln(r(x)+" "+ry(y)+" l");
		}
		streamln(outline?"b":"f");
	}

	public void drawImage (Image i, int x, int y, ImageObserver o){}
	public void drawImage (Image i, int x, int y, int w, int h,
		ImageObserver o){}

	public static void main (String args[])
	{	try
		{	ConstructionObject o=new ConstructionObject(new Construction());
			PrintWriter out=new PrintWriter(
				new FileOutputStream("test.pdf"));
			MyGraphicsPDF pdf=new MyGraphicsPDF(out,1000,1000,1);
			pdf.streamln("1 0 0 -1 0 1000 cm");
			pdf.drawRect(300,300,400,400);
			for (int i=0; i<180; i++)
			{	double s=i/180.0*Math.PI,t=(i+1)/180.0*Math.PI;
				pdf.drawLine(500+Math.cos(s)*200,500+Math.sin(s)*200,
					500+Math.cos(s)*200,500+Math.sin(s)*200);
			}
			pdf.setColor(Color.red);
			pdf.drawArc(-300,-300,1000,1000,0,360);
			pdf.close();
			out.close();
		}
		catch (Exception e)
		{	System.out.println(e);
		}
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

