package rene.zirkel.graphics;

import java.awt.*;
import java.awt.image.*;

import rene.gui.*;
import rene.zirkel.ZirkelFrame;
import rene.zirkel.objects.*;

public class MyGraphics11 extends MyGraphics
{	Graphics G;
	static public int MaxR=1000000;

	public MyGraphics11 (Graphics g)
	{	G=g;
	}

	public void clearRect (int x, int y, int w, int h, Color c)
	{	G.setColor(c);
		G.fillRect(x,y,w,h);
	}

	public void setColor (Color c)
	{	G.setColor(c);
	}

	public void setColor (ConstructionObject o)
	{	if (o.isJobTarget()) setColor(ZirkelFrame.TargetColor);
		else if (o.indicated()) setColor(ZirkelFrame.IndicateColor);
		else if (o.selected()) setColor(ZirkelFrame.SelectColor);
		else if (o.getColorType()==ConstructionObject.THIN)
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

	public void setFillColor (ConstructionObject o)
	{	if (o.isJobTarget()) setColor(ZirkelFrame.TargetColor);
		else if ((o instanceof PointObject) && o.indicated()) setColor(ZirkelFrame.IndicateColor);
		else if (o.getColorType()!=ConstructionObject.THICK)
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

	public void drawRect (double x, double y, double w, double h)
	{	G.drawRect((int)x,(int)y,(int)w,(int)h);
	}
	
	public void drawLine (double x, double y, double x1, double y1, ConstructionObject o)
	{	if (o.getColorType()==ConstructionObject.THICK)
			drawThickLine(x,y,x1,y1);
		else
			drawLine(x,y,x1,y1);
	}

	public void drawLine (double x, double y, double x1, double y1)
	{	G.drawLine((int)x,(int)y,(int)x1,(int)y1);
	}

	public void drawThickLine (double c1, double r1, double c2, double r2)
	{	drawLine(c1+1,r1,c2+1,r2);
		drawLine(c1-1,r1,c2-1,r2);
		drawLine(c1,r1+1,c2,r2+1);
		drawLine(c1,r1-1,c2,r2-1);
		drawLine(c1,r1,c2,r2);
	}

	public void drawArc (double x, double y, double w, double h, double a, double b)
	{	int aa=(int)(a),bb=(int)(a+b+1);
		if (w<MaxR) G.drawArc((int)x,(int)y,(int)w,(int)h,aa,bb-aa);
		else if (b<1)
		{	double x1=x+w/2,y1=y+h/2,r=w/2,r2=h/2;
			int ia=(int)(x1+Math.cos(a/180*Math.PI)*r),
				ja=(int)(y1-Math.sin(a/180*Math.PI)*r2);
			int ib=(int)(x1+Math.cos((a+b)/180*Math.PI)*r),
				jb=(int)(y1-Math.sin((a+b)/180*Math.PI)*r2);
			drawLine(ia,ja,ib,jb);
		}
	}

	public void drawThickArc (double x, double y, double w, double h, double a, double b)
	{	drawArc(x-1,y-1,w+2,w+2,a,b);
		drawArc(x+1,y+1,w-2,w-2,a,b);
		drawArc(x,y,w,h,a,b);
	}

	public void drawArc (double x, double y, double w, double h, double a, double b,
		ConstructionObject o)
	{	if (o.getColorType()==ConstructionObject.THICK)
			drawThickArc(x,y,w,h,a,b);
		else
			drawArc(x,y,w,h,a,b);
	}

	public FontMetrics getFontMetrics ()
	{	return G.getFontMetrics();
	}

	public void drawString (String s, double x, double y)
	{	G.drawString(s,(int)x,(int)y);
	}

	public void drawOval (double x, double y, double w, double h)
	{	if (w<MaxR) G.drawOval((int)x,(int)y,(int)w,(int)h);
	}

	public void drawThickOval (double x, double y, double w, double h)
	{	drawOval(x,y,w,h);
		drawOval(x-1,y-1,w+2,h+2);
		drawOval(x+1,y+1,w-2,h-2);
	}

	public void drawOval (double x, double y, double w, double h,
		ConstructionObject o)
	{	setColor(o);
		if (o.getColorType()==ConstructionObject.THICK)
			drawThickOval(x,y,w,h);
		else
			drawOval(x,y,w,h);
	}

	public void fillRect (double x, double y, double w, double h,
		boolean outline, boolean transparent, ConstructionObject o)
	{	setFillColor(o);
		G.fillRect((int)x,(int)y,(int)w,(int)h);
		if (outline)
		{	setColor(o);
			G.drawRect((int)x,(int)y,(int)w,(int)h);
		}
	}

	static int px[]=new int[500],py[]=new int[500];

	public void fillArc (double x, double y, double w, double h, double a, double b,
		boolean outline, boolean transparent, boolean arc, ConstructionObject o)
	{	int aa=(int)(a),bb=(int)(a+b+1);
		if (w>=MaxR) return;
		setFillColor(o);
		if (arc)
		{	G.fillArc((int)x,(int)y,(int)(w+1),(int)(h+1),aa,bb-aa);
		}
		else
		{	double t=a/180*Math.PI;
			double a1=(a+b)/180*Math.PI;
			x+=w/2; y+=w/2;
			px[0]=(int)(x+w/2*Math.cos(t)); py[0]=(int)(y-h/2*Math.sin(t));
			int i=1;
			while (t<a1)
			{	t+=2*Math.PI/490;
				px[i]=(int)(x+w/2*Math.cos(t)); py[i]=(int)(y-h/2*Math.sin(t));
				i++;
				if (i>=499) break;
			}
			px[i]=(int)(x+w/2*Math.cos((a+b)/180*Math.PI));
			py[i]=(int)(y-h/2*Math.sin((a+b)/180*Math.PI));
			i++;
			G.fillPolygon(px,py,i);
			x-=w/2; y-=w/2;
		}
		if (outline)
		{	setColor(o);
			G.drawArc((int)x,(int)y,(int)w,(int)h,aa,bb-aa);
		}
	}

	public void fillOval (double x, double y, double w, double h,
		boolean outline, boolean transparent, ConstructionObject o)
	{	if (w>=MaxR) return;
		if (o.getColorType()!=ConstructionObject.INVISIBLE)
		{	setFillColor(o);
			G.fillOval((int)x,(int)y,(int)(w+1),(int)(h+1));
		}
		if (outline)
		{	setColor(o);
			G.drawOval((int)x,(int)y,(int)w,(int)h);
		}
	}

	int xx[]=new int[64],yy[]=new int[64];

	public void fillPolygon (double x[], double y[], int n,
		boolean outline, boolean transparent, ConstructionObject o)
	{	setFillColor(o);
		for (int i=0; i<n; i++) xx[i]=(int)(x[i]);
		for (int i=0; i<n; i++) yy[i]=(int)(y[i]);
		if (o.getColorType()!=ConstructionObject.INVISIBLE)
		{	G.fillPolygon(xx,yy,n);
		}
		if (outline)
		{	setColor(o);
			G.drawPolygon(xx,yy,n);
		}
	}

	public void drawImage (Image i, int x, int y, ImageObserver o)
	{	G.drawImage(i,x,y,o);
	}	

	public void drawImage (Image i, int x, int y, int w, int h,
		ImageObserver o)
	{	G.drawImage(i,x,y,w,h,o);
	}	

	public void drawImage (Image i, double x, double y, double x1, double y1, 
			double x2, double y2, ImageObserver o)
	{	
	}

	FontStruct FS=new FontStruct();
	
	public void setFont (int h, boolean bold)
	{	Font f=FS.getFont(h,bold);
		if (f!=null) G.setFont(f);
		else
		{	f=new Font(
				Global.getParameter("font.name","dialog"),
				bold?Font.BOLD:Font.PLAIN,
				h);
			FS.storeFont(h,bold,f);
			G.setFont(f);
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

	public Graphics getGraphics() 
	{	return G;
	}

	public int stringWidth(String s) {
		return getFontMetrics().stringWidth(s);
	}

	public int stringHeight(String s) 
	{	return getFontMetrics().getHeight();
	}

	public int stringAscent(String s) {
		return 0;
	}

	public int drawStringExtended(String s, double x, double y) 
	{	drawString(s,x,y+getFontMetrics().getAscent());
		return getFontMetrics().getHeight(); 
	}

}
