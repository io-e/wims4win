package rene.zirkel.graphics;

import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;

import atp.sHotEqn;

import rene.gui.*;
import rene.zirkel.ZirkelFrame;
import rene.zirkel.objects.*;

public class MyGraphics13 extends MyGraphics
{	Graphics2D G;
	BasicStroke Thin,Normal,Thick,SuperThick;
	AlphaComposite C,CO;
	Component ZC=null;
	LatexOutput LOut;
	
	public MyGraphics13 (Graphics g, double factor, Component zc, LatexOutput lout)
	{	LOut=lout;
		G=(Graphics2D)g;
		if (Global.getParameter("quality",true))
		{	G.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
			G.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			G.setRenderingHint(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);
			G.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		}
		float dash[]={(float)(factor*15.0),(float)(factor*15.0)};
		if (factor<Global.getParameter("minlinesize",1.0)) 
			factor=Global.getParameter("minlinesize",1.0);
		Thin=new BasicStroke((float)(factor*2.0),BasicStroke.CAP_BUTT,
			BasicStroke.JOIN_MITER,10.0f,dash,0.0f);
		Normal=new BasicStroke((float)(factor));
		Thick=new BasicStroke((float)(factor*4.0),BasicStroke.CAP_ROUND,
				BasicStroke.JOIN_MITER);
		SuperThick=new BasicStroke((float)(20),BasicStroke.CAP_ROUND,
				BasicStroke.JOIN_ROUND);
		C=AlphaComposite.getInstance(AlphaComposite.SRC_OVER,(float)0.5);
		CO=AlphaComposite.getInstance(AlphaComposite.SRC_OVER,(float)1.0);
		G.setComposite(CO);
		ZC=zc;
	}
	
	public MyGraphics13 (Graphics g)
	{	this(g,0.5,null,null);
	}

	public MyGraphics13 (Graphics g, Component zc)
	{	this(g,0.5,zc,null);
	}

	public MyGraphics13 (Graphics g, double factor)
	{	this(g,factor,null,null);
	}

	public void setColor (Color c)
	{	G.setColor(c);
	}

	public void setColor (ConstructionObject o)
	{	G.setStroke(Normal);
		if (o.isJobTarget()) setColor(ZirkelFrame.TargetColor);
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
		{	G.setStroke(Thin);
		}
		else if (o.getColorType()==ConstructionObject.THICK)
		{	G.setStroke(Thick);
		}
		else
		{	G.setStroke(Normal);
		}
	}

	public void clearRect (int x, int y, int w, int h, Color c)
	{	G.setColor(c);
		G.fillRect(x,y,w,h);
	}

	public void setFillColor (ConstructionObject o)
	{	G.setStroke(Normal);
		if (o.isJobTarget()) setColor(ZirkelFrame.TargetColor);
		else if ((o instanceof PointObject) && o.indicated()) setColor(ZirkelFrame.IndicateColor);
		else
		{	if (o.getColorType()!=ConstructionObject.THICK)
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
			{	G.setStroke(Thin);
			}
			else if (o.getColorType()==ConstructionObject.THICK)
			{	G.setStroke(Thick);
			}
			else
			{	G.setStroke(Normal);
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

	public void drawRect (double x, double y, double w, double h)
	{	if (test(x) || test(y) || test(x+w) || test(y+h)) return;
		G.setStroke(Normal);
		G.draw(new Rectangle2D.Double(x,y,w,h));
	}

	public void drawMarkerRect (double x, double y, double w, double h)
	{	if (test(x) || test(y) || test(x+w) || test(y+h)) return;
		G.setColor(ZirkelFrame.IndicateColor);
		G.setStroke(SuperThick);
		G.draw(new Rectangle2D.Double(x,y,w,h));
		G.setStroke(Normal);
	}

	public void drawLine (double x, double y, double x1, double y1)
	{	if (test(x) || test(y) || test(x1) || test(y1)) return;
		G.setStroke(Normal);
		G.draw(new Line2D.Double(x,y,x1,y1));
	}

	public void drawThickLine (double x, double y, double x1, double y1)
	{	if (test(x) || test(y) || test(x1) || test(y1)) return;
		G.setStroke(Thick);
		G.draw(new Line2D.Double(x,y,x1,y1));
		G.setStroke(Normal);
	}

	public void drawMarkerLine (double x, double y, double x1, double y1)
	{	if (test(x) || test(y) || test(x1) || test(y1)) return;
		G.setColor(ZirkelFrame.IndicateColor);
		G.setStroke(SuperThick);
		G.draw(new Line2D.Double(x,y,x1,y1));
		G.setStroke(Normal);
	}

	public void drawLine (double x, double y, double x1, double y1, ConstructionObject o)
	{	if (test(x) || test(y) || test(x1) || test(y1)) return;
		G.draw(new Line2D.Double(x,y,x1,y1));
	}
	
	public boolean test (double x)
	{	return Math.abs(x)>1e5;
	}

	public void drawArc (double x, double y, double w, double h, double a, double b)
	{	if (test(x) || test(y) || test(w) || test(h)) return;
		G.setStroke(Normal);
		G.draw(new Arc2D.Double(x,y,w,h,a,b,Arc2D.OPEN));
	}

	public void drawArc (double x, double y, double w, double h, double a, double b,
		ConstructionObject o)
	{	if (test(x) || test(y) || test(w) || test(h))
			return;
		else
			G.draw(new Arc2D.Double(x,y,w,h,a,b,Arc2D.OPEN));
	}

	public FontMetrics getFontMetrics ()
	{	return G.getFontMetrics();
	}

	public void drawString (String s, double x, double y)
	{	if (test(x) || test(y)) return;
		G.drawString(s,(float)x,(float)y);
	}

	public void drawOval (double x, double y, double w, double h)
	{	if (test(x) || test(y) || test(w) || test(h)) return;
		G.setStroke(Normal);
		G.draw(new Ellipse2D.Double(x,y,w,h));		
	}

	public void drawOval (double x, double y, double w, double h,
		ConstructionObject o)
	{	if (test(x) || test(y) || test(w) || test(h))
			return;
		else
			G.draw(new Ellipse2D.Double(x,y,w,h));		
	}
	
	public void drawCircle (double x, double y, double r,
		ConstructionObject o)
	{	if (r>10*(W+H))
			drawLargeCircleArc(x,y,r,0,360);
		else
			G.draw(new Ellipse2D.Double(x-r,y-r,2*r,2*r));		
	}
	
	public void drawMarkerArc (double x, double y, double r, double a, double b)
	{	if (test(x) || test(y) || test(r)) return;
		G.setColor(ZirkelFrame.IndicateColor);
		G.setStroke(SuperThick);
		G.draw(new Arc2D.Double(x-r,y-r,2*r,2*r,a,b,Arc2D.OPEN));
	}

	public void drawCircleArc (double x, double y, double r, double a, double b,
		ConstructionObject o)
	{	if (r>10*(W+H))
			drawLargeCircleArc(x,y,r,a,b);
		else
			G.draw(new Arc2D.Double(x-r,y-r,2*r,2*r,a,b,Arc2D.OPEN));
	}

	void drawLargeCircleArc (double x, double y, double r, double a, double b)
	{	double dw=Math.sqrt((W+H)/r/10);
		double w=a;
		double x0=x+r*Math.cos(w/180*Math.PI);
		double y0=y-r*Math.sin(w/180*Math.PI);
		w=w+dw;		
		while (w<a+b+dw)
		{	if (w>a+b) w=a+b;
			double x1=x+r*Math.cos(w/180*Math.PI);
			double y1=y-r*Math.sin(w/180*Math.PI);
			double dx=(x0+x1)/2,dy=(y0+y1)/2;
			if (Math.sqrt(dx*dx+dy*dy)<=10*(W+H)) 
				G.draw(new Line2D.Double(x0,y0,x1,y1));
			x0=x1; y0=y1;
			w+=dw;
		}
	}

	public void fillRect (double x, double y, double w, double h,
		boolean outline, boolean transparent, ConstructionObject o)
	{	if (test(x) || test(y) || test(w) || test(h)) return;
		setFillColor(o);
		if (transparent && !o.isSolid()) G.setComposite(C);
		G.fill(new Rectangle2D.Double(x,y,w,h));
		if (transparent && !o.isSolid()) G.setComposite(CO);
		if (outline)
		{	setColor(o);
			G.draw(new Rectangle2D.Double(x,y,w,h));
		}
	}

	public void fillOval (double x, double y, double w, double h,
		boolean outline, boolean transparent, ConstructionObject o)
	{	if (test(x) || test(y) || test(w) || test(h)) return;
		if (o.getColorType()!=ConstructionObject.INVISIBLE)
		{	setFillColor(o);
			if (transparent && !o.isSolid()) G.setComposite(C);
			try
			{	G.fill(new Ellipse2D.Double(x,y,w,h));		
			}
			catch (Exception e) {}
			if (transparent && !o.isSolid()) G.setComposite(CO);
		}
		if (outline)
		{	setColor(o);
			drawOval(x,y,w,h);
		}
	}

	public void fillArc (double x, double y, double w, double h, double a, double b,
		boolean outline, boolean transparent, boolean arcb, ConstructionObject o)
	{	if (test(x) || test(y) || test(w) || test(h)) return;
		setFillColor(o);
		if (transparent && !o.isSolid()) G.setComposite(C);
		Arc2D arc=new Arc2D.Double(x,y,w,h,a,b,arcb?Arc2D.PIE:Arc2D.CHORD);
		G.fill(arc);
		if (transparent && !o.isSolid()) G.setComposite(CO);
		if (outline)
		{	setColor(o);
			arc.setArcType(Arc2D.OPEN);
			G.setStroke(Normal);
			G.draw(arc);
		}
	}

	int xx[]=new int[64],yy[]=new int[64];

	public void fillPolygon (double x[], double y[], int n,
		boolean outline, boolean transparent, ConstructionObject o)
	{	if (o.getColorType()!=ConstructionObject.INVISIBLE)
		{	setFillColor(o);
			if (transparent && !o.isSolid()) G.setComposite(C);
		}
		if (n>xx.length)
		{	xx=new int[n]; yy=new int[n];
		}
		for (int i=0; i<n; i++) 
		{	xx[i]=(int)(x[i]);
			if (test(x[i])) return;
			yy[i]=(int)(y[i]);
			if (test(y[i])) return;
		}
		if (o.getColorType()!=ConstructionObject.INVISIBLE)
		{	G.fillPolygon(xx,yy,n);
			if (transparent && !o.isSolid()) G.setComposite(CO);
		}
		if (outline)
		{	setColor(o);
			G.setStroke(Normal);
			for (int i=0; i<n-1; i++)
				drawLine(xx[i],yy[i],xx[i+1],yy[i+1]);
			drawLine(xx[n-1],yy[n-1],xx[0],yy[0]);			
		}
	}

	public void drawImage (Image i, int x, int y, ImageObserver o)
	{	G.drawImage(i,x,y,o);
	}	
	
	public void drawImage (Image i, int x, int y, int w, int h,
		ImageObserver o)
	{	G.drawImage(i,x,y,w,h,o);
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
	{	ffactor=Global.getParameter("ffactor",130);
		fsize=h; flarge=large; fbold=bold;
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
	{	try
		{	int w=i.getWidth(o),h=i.getHeight(o);
			AffineTransform AT=new AffineTransform((x1-x)/w,(y1-y)/w,(x2-x)/h,(y2-y)/h,x,y);
			G.drawImage(i,AT,o);
		}
		catch (Exception e) {}
	}

	public Graphics getGraphics() 
	{	return G;
	}

	int StrH=0,StrW=0,StrAsc=0;
	boolean StrTex=false;
	String Str=null;
	
	public void computeString (String s)
	{	if (s.equals(Str)) return;
		StrH=StrW=StrAsc=0;
		StrTex=false;
		Str=s;
		while (s!="") 
		{	int n=s.indexOf('$');
			while (n>0 && s.charAt(n-1)=='\\') n=s.indexOf('$',n+1);
			if (n<0)
			{	StrH=Math.max(StrH,getFontMetrics().getHeight());
				StrW+=getFontMetrics().stringWidth(s);
				StrAsc=Math.max(StrAsc,getFontMetrics().getAscent());
				break;
			}
			else
			{	StrTex=true;
				String ss=s.substring(0,n);
				s=s.substring(n+1);
				StrH=Math.max(StrH,getFontMetrics().getHeight());
				StrW+=getFontMetrics().stringWidth(ss);
				StrAsc=Math.max(StrAsc,getFontMetrics().getAscent());
				n=s.indexOf('$');
				while (n>0 && s.charAt(n-1)=='\\') n=s.indexOf('$',n+1);
				ss=s;
				if (n>=0)
				{	ss=s.substring(0,n); s=s.substring(n+1);
				}
				else
				{	s="";
				}
				if (HE==null || !HE.getEquation().equals(ss))
					setHotEqn(ss);
				StrH=Math.max(StrH,heightHotEqn(G));
				StrW+=widthHotEqn(G);
				StrAsc=Math.max(StrAsc,ascentHotEqn(G));
			}
		}		
	}
	
	public int stringWidth (String s) 
	{	computeString(s);
		return StrW;
	}

	public int stringHeight (String s) 
	{	computeString(s);
		return StrH;
	}
	
	public boolean isTex (String s)
	{	computeString(s);
		return StrTex;
	}
	
	public int drawStringExtended (String s, double x, double y) 
	{	if (s.startsWith("$$"))
		{	s=s.substring(2);
			if (s.endsWith("$$")) s=s.substring(0,s.length()-2);
			if (HE==null || !HE.getEquation().equals(s))
				setHotEqn(s);
			if (LOut==null || 
					!LOut.println("$$"+s+"$$",x,y+ascentHotEqn(G)))
				return paintHotEqn((int)x,(int)y,G);
			else
				return heightHotEqn(G);
		}
		computeString(s);
		if (LOut!=null)
		{	if (StrTex && LOut.printDollar())
			{	LOut.println(s,x,y+StrAsc,true);
				return StrH;
			}
			else if (!StrTex)
			{	if (LOut.println(s,x,y+StrAsc))
					return StrH;
			}
		}
		int w=0;
		while (s!="") 
		{	int n=s.indexOf('$');
			while (n>0 && s.charAt(n-1)=='\\') n=s.indexOf('$',n+1);
			if (n<0)
			{	drawString(translateDollar(s),x+w,y+StrAsc);
				w+=getFontMetrics().stringWidth(s);
				break;
			}
			else
			{	String ss=s.substring(0,n);
				s=s.substring(n+1);
				drawString(translateDollar(ss),x+w,y+StrAsc);
				w+=getFontMetrics().stringWidth(ss);
				n=s.indexOf('$');
				while (n>0 && s.charAt(n-1)=='\\') n=s.indexOf('$',n+1);
				ss=s;
				if (n>=0)
				{	ss=s.substring(0,n); s=s.substring(n+1);
				}
				else
				{	s="";
				}
				if (HE==null || !HE.getEquation().equals(ss))
					setHotEqn(ss);
				paintHotEqn((int)x+w,(int)y+StrAsc-ascentHotEqn(G),G);
				w+=widthHotEqn(G);
			}
		}
		return StrH;
	}
	
	public String translateDollar (String s)
	{	int n;
		while ((n=s.indexOf("\\$"))>=0)
		{	s=s.substring(0,n)+"$"+s.substring(n+2);
		}
		return s;
	}

	public int stringAscent (String s) 
	{	return getFontMetrics().getAscent();
	}

	sHotEqn HE=null;
	
	public void setHotEqn (String s)
	{	if (ZC==null) return;
		if (HE==null)
		{	HE=new sHotEqn(ZC);
		}
		HE.setEquation(s);
	}
	
	public int paintHotEqn (int c, int r, Graphics g)
	{	if (HE==null) return 0;
		return HE.paint(c,r,g);
	}
	
	public int heightHotEqn (Graphics g)
	{	if (HE==null) return 0;
		return HE.getSizeof(HE.getEquation(),g).height;
	}
		
	public int ascentHotEqn (Graphics g)
	{	if (HE==null) return 0;
		return HE.getAscent(HE.getEquation(),g);
	}
	
	public int widthHotEqn (Graphics g)
	{	if (HE==null) return 0;
		return HE.getSizeof(HE.getEquation(),g).width;
	}
		
}
