/*
 * LatticeViewer.java	
 * Date :       18th Feb 1998	
 * Adapted by : Simon P.A.Gill
 * WWW :	http://www.le.ac.uk/engineering/spg3/lattice/
 * 
 * The following java code is an adaptation of the Molecule Viewer XYZApp.java
 * freely distributed by Sun Microsystems. The conditions stated below apply
 * to this code. S.P.A. Gill takes no responsibility for this code. 
 * Requires class Matrix3D.java (also from Sun at
 * http://www.javasoft.com:80/applets/js-applets.html)
 *
 *****************************************************************************
 *
 * @(#)XYZApp.java	1.3 96/12/06
 *
 * Copyright (c) 1994-1996 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Sun grants you ("Licensee") a non-exclusive, royalty free, license to use,
 * modify and redistribute this software in source and binary code form,
 * provided that i) this copyright notice and license appear on all copies of
 * the software; and ii) Licensee does not utilize the software in a manner
 * which is disparaging to Sun.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS
 * LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
 * INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF
 * OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line control of
 * aircraft, air traffic, aircraft navigation or aircraft communications; or in
 * the design, construction, operation or maintenance of any nuclear
 * facility. Licensee represents and warrants that it will not use or
 * redistribute the Software for such purposes.
 */

/*
 * Initially a set of classes to parse, represent and display Chemical compounds
 * in .xyz format (see http://chem.leeds.ac.uk/Project/MIME.html). Now adapted
 * to also display interatomic bonds, allow the user to define the atomic radii
 * and colour of the atoms and define other lines in the display (such as the
 * unit cell). This code will still display .xyz files but all the atoms will
 * look the same (the default atom size and colour) unless they are defined
 * in the .xyz file. A .xyz file in which the bond locations and atom colours and
 * sizes have been defined have been distinguished by the .lat extension
 *
 * FILE FORMAT :
 *
 * The file format is based around the standard .xyz file which defines the 
 * position in space of the atoms in the lattice
 * (see http://cst-www.nrl.navy.mil/lattice/ for details and many examples).
 * Any comments must be removed. Each atom type must be defined. 
 * The following line defines an atom by its name (C), its RGB colour (red=255,
 * green=0, blue=0) and relative size (default=1.0, minimum=0.0 maximum=2.0)
 *
 *	ATOM  C  255  0  0  1.0 
 *
 * The position and bonds of the atom are defined as follows :
 * 
 *  	C  0.0  1.0  0.0  3  6  8
 *
 * This places a carbon (C) atom at the co-ordinates (0.0,1.0,0.0) and says that
 * bonds exist between it and the 3rd, 6th and 8th atoms in the list of atoms.
 * An atom can have an unlimited number of bonds, the only restriction is that
 * the total is less than 300 (unless the array size of bonds[] is increased.
 * NOTE - any atom defined with a size of 0.0 (eg ATOM X 0 0 0 0.0) can be used
 * to define the location of a position in space without an atom being placed
 * there. Any bonds between such points are known as the BOX (as these have
 * been used here to define the unit cell of a lattice) and are only displayed
 * if parameter box has value=true (see below).
 *
 * APPLET OPTIONS :
 *
 * PARAM = model	VALUE = filename
 * PARAM = label	VALUE = true/false	- labels atoms with their
 *						  numerical position in the list.
 *						  Very useful when defining bond
 *						  positions (default = false)
 * PARAM = scale	VALUE = float		- size of lattice in applet (default=1.0)
 * PARAM = box		VALUE = true/false	- the box concept is defined above.
 *						  Only displayed if TRUE (default=false)
 * PARAM = bonds	VALUE = true/false	- display interatomic bonds if true (default=false).
 * PARAM = bgcolor	VALUE = RGB colour	- background colour (default=lightGrey)
 * PARAM = bondcolor	VALUE = RGB colour	- bond colour (default=black)
 * PARAM = boxcolor	VALUE = RGB colour 	- colour of box bonds (default=red)
 * 
 * NOTE - it has been found that this applet runs significantly better under Sun's 
 * appletviewer than on Netscape (especially when viewing complex structures)
 */

import java.applet.Applet;
import java.awt.Image;
import java.awt.Event;
import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.Color;
import java.io.StreamTokenizer;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Hashtable;
import java.awt.image.IndexColorModel;
import java.awt.image.ColorModel;
import java.awt.image.MemoryImageSource;


/** The representation of a Chemical .xyz or Atomic Lattice (.lat) model */
class XYZLatModel {
    float vert[];
    ScaleableAtom atoms[];
    int tvert[];
    int ZsortMap[];
    int nvert, maxvert;
    int bond[][] = new int[800][2];
    int nbond = -1;
    boolean box;
    boolean label;
    boolean bonds;
    int br, bg, bb;	// RGB of bonds
    int gr, gg, gb;	// RGB of background
    int xr, xg, xb;	// RGB of box

    static Hashtable atomTable;
    static ScaleableAtom defaultAtom;

    boolean transformed;
    Matrix3D mat;

    float xmin, xmax, ymin, ymax, zmin, zmax;


    XYZLatModel () {
	mat = new Matrix3D();
	mat.xrot(20);
	mat.yrot(30);
    }


    /** Create a Chemical model by parsing an input stream */
    XYZLatModel (InputStream is, boolean labl, boolean bx, boolean bnds, Color bgcolor, Color bndcolor, Color bxcolor) throws Exception
    {
       this();
       StreamTokenizer st;
       st = new StreamTokenizer(new BufferedInputStream(is, 4000));
       st.eolIsSignificant(true);
       st.commentChar('#');
       int slot = 0;
      
       this.label=labl;
       this.box=bx;
       this.bonds=bnds;
       this.br=bndcolor.getRed();
       this.bg=bndcolor.getGreen();
       this.bb=bndcolor.getBlue();
       this.gr=bgcolor.getRed();
       this.gg=bgcolor.getGreen();
       this.gb=bgcolor.getBlue();
       this.xr=bxcolor.getRed();
       this.xg=bxcolor.getGreen();
       this.xb=bxcolor.getBlue();
       atomTable = new Hashtable();
       defaultAtom = new ScaleableAtom(255, 100, 200, 1.0);
       try
       {
scan:
          while (true)
          {
             switch ( st.nextToken() ) 
             {
                case StreamTokenizer.TT_EOF:
                   break scan;
                default:
                   break;
                case StreamTokenizer.TT_WORD:
                   // get atom type
                   String name = st.sval;
                   if (name.equals("ATOM")) {
                       // get name, RGB colour and size
                       int r=255, g=255, b=255;
                       double s=1.0;
                       if (st.nextToken() == StreamTokenizer.TT_WORD) 
                       {
                           name=st.sval;
                           if (st.nextToken() == StreamTokenizer.TT_NUMBER) 
                           {
                              r = (int)st.nval;
                              if (st.nextToken() == StreamTokenizer.TT_NUMBER) 
                              {
                                 g = (int)st.nval;
                                 if (st.nextToken() == StreamTokenizer.TT_NUMBER)
                                 {
                                     b =(int)st.nval;
                                     if (st.nextToken() == StreamTokenizer.TT_NUMBER)
                                         s = (double)st.nval;
                                 }
                              }
                           }
                       }
                       if (s>=2.0) s=2.0;
                       if (s<0.0) s=1.0;
	               atomTable.put(name.toLowerCase(), new ScaleableAtom(r, g, b, s));
                   } else {
                       // get position
                       double x = 0, y = 0, z = 0;
                       if (st.nextToken() == StreamTokenizer.TT_NUMBER) 
                       {
                          x = st.nval;
                          if (st.nextToken() == StreamTokenizer.TT_NUMBER) 
                          {
                             y = st.nval;
                             if (st.nextToken() == StreamTokenizer.TT_NUMBER)
                                z = st.nval;
                          }
                       }
                       addVert(name, (float) x, (float) y, (float) z);
                       // get connectivities
                       while (st.nextToken() == StreamTokenizer.TT_NUMBER)
                       {
                         nbond++;
                         bond[nbond][0]=nvert;
                         bond[nbond][1]=(int)st.nval;
                       }
                   }
                   while( st.ttype != StreamTokenizer.TT_EOL &&
                          st.ttype != StreamTokenizer.TT_EOF )
                   st.nextToken();

             }   // end Switch

          }  // end while

          is.close();

       }  // end Try
       catch( IOException e) {}

       if (st.ttype != StreamTokenizer.TT_EOF)
          throw new Exception(st.toString());

    }  // end XYZLatModel()


    /** Add a vertex to this model */
    int addVert(String name, float x, float y, float z) {
	int i = nvert;
	if (i >= maxvert)
	    if (vert == null) {
		maxvert = 100;
		vert = new float[maxvert * 3];
		atoms = new ScaleableAtom[maxvert];
	    } else {
		maxvert *= 2;
		float nv[] = new float[maxvert * 3];
		System.arraycopy(vert, 0, nv, 0, vert.length);
		vert = nv;
		ScaleableAtom na[] = new ScaleableAtom[maxvert];
		System.arraycopy(atoms, 0, na, 0, atoms.length);
		atoms = na;
	    }
	ScaleableAtom a = (ScaleableAtom) atomTable.get(name.toLowerCase());
	if (a == null) a = defaultAtom;
	atoms[i] = a;
	i *= 3;
	vert[i] = x;
	vert[i + 1] = y;
	vert[i + 2] = z;
	return nvert++;
    }

    /** Transform all the points in this model */
    void transform() {
	if (transformed || nvert <= 0)
	    return;
	if (tvert == null || tvert.length < nvert * 3)
	    tvert = new int[nvert * 3];
	mat.transform(vert, tvert, nvert);
	transformed = true;
    }


    /** Paint this model to a graphics context.  It uses the matrix associated
	with this model to map from model space to screen space.
	The next version of the browser should have double buffering,
	which will make this *much* nicer */
    void paint(Graphics g) {
	if (vert == null || nvert <= 0)
	    return;
	transform();
	int v[] = tvert;
	int zs[] = ZsortMap;
	if (zs == null) {
	    ZsortMap = zs = new int[nvert];
	    for (int i = nvert; --i >= 0;)
		zs[i] = i * 3;
	}

	/*
	 * I use a bubble sort since from one iteration to the next, the sort
	 * order is pretty stable, so I just use what I had last time as a
	 * "guess" of the sorted order.  With luck, this reduces O(N log N)
	 * to O(N)
	 */

	for (int i = nvert - 1; --i >= 0;) {
	    boolean flipped = false;
	    for (int j = 0; j <= i; j++) {
		int a = zs[j];
		int b = zs[j + 1];
		if (v[a + 2] > v[b + 2]) {
		    zs[j + 1] = a;
		    zs[j] = b;
		    flipped = true;
		}
	    }
	    if (!flipped)
		break;
	}

	int lg = 0;
	int lim = nvert;
	ScaleableAtom ls[] = atoms;
        int drawn[] = new int[nbond+1];
	if (lim <= 0 || nvert <= 0)
	    return;
	for (int i = 0; i < lim; i++) {
	    int j = zs[i];
	    int grey = v[j + 2];
	    if (grey < 0)
		grey = 0;
	    if (grey > 15)
		grey = 15;

            // draw bonds
            int v1, v2;
            boolean e = atoms[j/3].Exist();
            v1=j/3+1;
            if (((e)&&(bonds))||((!e)&&(box)))
            {
                for (int k=0; k<=nbond; k++)
                {
                    v2=-1;
                    if (bond[k][0]==v1)
                        v2=bond[k][1]-1;
                    if (bond[k][1]==v1)
                        v2=bond[k][0]-1;
                    if ((v2!=-1)&&(drawn[k]==0))
                    {
                        drawn[k]=1;
                        double rr=(v[j+2]+v[v2*3+2]+6)/36.0;
                        int r1,b1,g1;
                        if (e) {
                            r1=(int)(rr*(br-gr)+gr);
                            b1=(int)(rr*(bb-gb)+gb);
                            g1=(int)(rr*(bg-gg)+gg);
                        } else {
                            r1=(int)(rr*(xr-gr)+gr);
                            b1=(int)(rr*(xb-gb)+gb);
                            g1=(int)(rr*(xg-gg)+gg);
                        }   
                        Color line = new Color(r1,g1,b1);
                        g.setColor(line);
                        g.drawLine(v[j],v[j+1],v[v2*3],v[v2*3+1]);
                    }
                }
            }
            if (e)
	        atoms[j/3].paint(g, v[j], v[j + 1], grey);
            if (label) {
                g.setColor(Color.black);
	        g.drawString(String.valueOf(v1), v[j], v[j+1]); 
            }
	}
    }

    /** Find the bounding box of this model */
    void findBB() {
	if (nvert <= 0)
	    return;
	float v[] = vert;
	float xmin = v[0], xmax = xmin;
	float ymin = v[1], ymax = ymin;
	float zmin = v[2], zmax = zmin;
	for (int i = nvert * 3; (i -= 3) > 0;) {
	    float x = v[i];
	    if (x < xmin)
		xmin = x;
	    if (x > xmax)
		xmax = x;
	    float y = v[i + 1];
	    if (y < ymin)
		ymin = y;
	    if (y > ymax)
		ymax = y;
	    float z = v[i + 2];
	    if (z < zmin)
		zmin = z;
	    if (z > zmax)
		zmax = z;
	}
	this.xmax = xmax;
	this.xmin = xmin;
	this.ymax = ymax;
	this.ymin = ymin;
	this.zmax = zmax;
	this.zmin = zmin;
    }
}

/** An applet to put a Chemical/Atomic Lattice model into a page */
public class LatticeViewer extends Applet implements Runnable {
    XYZLatModel md;
    boolean painted = true;
    float xfac;
    int prevx, prevy;
    float xtheta, ytheta;
    float scalefudge = 1;
    Matrix3D amat = new Matrix3D(), tmat = new Matrix3D();
    String mdname = null;
    String message = null;
    boolean label = false;
    boolean box = false;
    boolean bonds = false;
    Image backBuffer;
    Graphics backGC;
    Dimension backSize;
    String param;
    Color bgcolor, bondcolor, boxcolor;


    private synchronized void newBackBuffer() {
	backBuffer = createImage(size().width, size().height);
	backGC = backBuffer.getGraphics();
	backSize = size();
    }

    public void init() {
	mdname = getParameter("model");
	try {
	    scalefudge = Float.valueOf(getParameter("scale")).floatValue();
	} catch(Exception e) {
	};
	try {
	    label = Boolean.valueOf(getParameter("label")).booleanValue();
	} catch(Exception e) {
	};
	try {
	    box = Boolean.valueOf(getParameter("box")).booleanValue();
	} catch(Exception e) {
	};
	try {
	    bonds = Boolean.valueOf(getParameter("bonds")).booleanValue();
	} catch(Exception e) {
	};
	try {
	    param = getParameter("bgcolor");
	} catch(Exception e) {
	};
        try {
	   Integer i = Integer.valueOf(param, 16);
	   bgcolor = new Color(i.intValue());
        } catch (NumberFormatException e) {
	   bgcolor = Color.lightGray;
        }
	try {
	    param = getParameter("bondcolor");
	} catch(Exception e) {
	};
        try {
	   Integer i = Integer.valueOf(param, 16);
	   bondcolor = new Color(i.intValue());
        } catch (NumberFormatException e) {
	   bondcolor = Color.black;
        };
	try {
	    param = getParameter("boxcolor");
	} catch(Exception e) {
	};
        try {
	   Integer i = Integer.valueOf(param, 16);
	   boxcolor = new Color(i.intValue());
        } catch (NumberFormatException e) {
	   boxcolor = Color.red;
        };
	amat.yrot(20);
	amat.xrot(20);
	if (mdname == null)
	    mdname = "model.obj";
	resize(size().width <= 20 ? 400 : size().width,
	       size().height <= 20 ? 400 : size().height);
	newBackBuffer();
    }
    public void run() {
	InputStream is = null;
	try {
	    Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
	    is = new URL(getDocumentBase(), mdname).openStream();
	    XYZLatModel m = new XYZLatModel (is, label, box, bonds, bgcolor, bondcolor, boxcolor);
	    ScaleableAtom.setApplet(this);
	    md = m;
	    m.findBB();
	    float xw = m.xmax - m.xmin;
	    float yw = m.ymax - m.ymin;
	    float zw = m.zmax - m.zmin;
	    if (yw > xw)
		xw = yw;
	    if (zw > xw)
		xw = zw;
	    float f1 = size().width / xw;
	    float f2 = size().height / xw;
	    xfac = 0.7f * (f1 < f2 ? f1 : f2) * scalefudge;
	} catch(Exception e) {
	    e.printStackTrace();
	    md = null;
	    message = e.toString();
	}
	try {
	    if (is != null)
		is.close();
	} catch(Exception e) {
	}
	repaint();
    }
    public void start() {
	if (md == null && message == null)
	    new Thread(this).start();
    }
    public void stop() {
    }
    public boolean mouseDown(Event e, int x, int y) {
	prevx = x;
	prevy = y;
	return true;
    }
    public boolean mouseDrag(Event e, int x, int y) {
	tmat.unit();
	float xtheta = (prevy - y) * (360.0f / size().width);
	float ytheta = (x - prevx) * (360.0f / size().height);
	tmat.xrot(xtheta);
	tmat.yrot(ytheta);
	amat.mult(tmat);
	if (painted) {
	    painted = false;
	    repaint();
	}
	prevx = x;
	prevy = y;
	return true;
    }
    public void update(Graphics g) {
	if (backBuffer == null)
	    g.clearRect(0, 0, size().width, size().height);
	paint(g);
    }

    public void paint(Graphics g) {
	if (md != null) {
	    md.mat.unit();
	    md.mat.translate(-(md.xmin + md.xmax) / 2,
			     -(md.ymin + md.ymax) / 2,
			     -(md.zmin + md.zmax) / 2);
	    md.mat.mult(amat);
	    // md.mat.scale(xfac, -xfac, 8 * xfac / size().width);
	    md.mat.scale(xfac, -xfac, 16 * xfac / size().width);
	    md.mat.translate(size().width / 2, size().height / 2, 8);
	    md.transformed = false;
	    if (backBuffer != null) {
		if (!backSize.equals(size()))
		    newBackBuffer();
                setBackground(bgcolor);
		backGC.setColor(bgcolor);
		backGC.fillRect(0,0,size().width,size().height);
		md.paint(backGC);
		g.drawImage(backBuffer, 0, 0, this);
	    }
	    else
		md.paint(g);
	    setPainted();
	} else if (message != null) {
	    g.drawString("Error in model:", 3, 20);
	    g.drawString(message, 10, 40);
	}
    }
    private synchronized void setPainted() {
	painted = true;
	notifyAll();
    }

    private synchronized void waitPainted() 
    {
       while (!painted)
       {
          try
          {
             wait();
          }
          catch (InterruptedException e) {}
       }
       painted = false;
    }
}   // end class LatticeViewer

class ScaleableAtom {
    private static Applet applet;
    private static byte[] data;
    private final static int R = 40;
    private final static int hx = 15;
    private final static int hy = 15;
    private final static int bgGrey = 192;
    private static int maxr;

    private int Rl;
    private int Gl;
    private int Bl;
    private double Sf;

    private Image balls[];

    static {
	data = new byte[R * 2 * R * 2];
	int mr = 0;
	for (int Y = 2 * R; --Y >= 0;) {
	    int x0 = (int) (Math.sqrt(R * R - (Y - R) * (Y - R)) + 0.5);
	    int p = Y * (R * 2) + R - x0;
	    for (int X = -x0; X < x0; X++) {
		int x = X + hx;
		int y = Y - R + hy;
		int r = (int) (Math.sqrt(x * x + y * y) + 0.5);
		if (r > mr)
		    mr = r;
		data[p++] = r <= 0 ? 1 : (byte) r;
	    }
	}
	maxr = mr;
    }
    static void setApplet(Applet app) {
	applet = app;
    }
    ScaleableAtom(int Rl, int Gl, int Bl, double Sf) {
	this.Rl = Rl;
	this.Gl = Gl;
	this.Bl = Bl;
        this.Sf = Sf;
    }
    boolean Exist() {
	if (Sf==0.0)
            return false;
        else
            return true;
    }
    private final int blend(int fg, int bg, float fgfactor) {
	return (int) (bg + (fg - bg) * fgfactor);
    }
    private void Setup(int nBalls) {
	balls = new Image[nBalls];
	byte red[] = new byte[256];
	red[0] = (byte) bgGrey;
	byte green[] = new byte[256];
	green[0] = (byte) bgGrey;
	byte blue[] = new byte[256];
	blue[0] = (byte) bgGrey;
	for (int r = 0; r < nBalls; r++) {
	    float b = (float) (r+1) / nBalls;
	    for (int i = maxr; i >= 1; --i) {
		float d = (float) i / maxr;
		red[i] = (byte) blend(blend(Rl, 255, d), bgGrey, b);
		green[i] = (byte) blend(blend(Gl, 255, d), bgGrey, b);
		blue[i] = (byte) blend(blend(Bl, 255, d), bgGrey, b);
	    }
	    IndexColorModel model = new IndexColorModel(8, maxr + 1,
							red, green, blue, 0);
	    balls[r] = applet.createImage(
		new MemoryImageSource(R*2, R*2, model, data, 0, R*2));
	}
    }
    void paint(Graphics gc, int x, int y, int r) {
	Image ba[] = balls;
	if (ba == null) {
	    Setup((int)(16*Sf));
	    ba = balls;
	}
        r=(int)(r*Sf);
	Image i = ba[r];
	int size = 10 + r;
	gc.drawImage(i, x - (size >> 1), y - (size >> 1), size, size, applet);
    }
}
