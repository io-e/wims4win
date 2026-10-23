/*
    Sketch Elements: Chemistry molecular diagram drawing tool.
    
    (c) 2008 Dr. Alex M. Clark
    
    Released as GNUware, under the Gnu Public License (GPL)
    
    See www.gnu.org for details.
*/

package WIMSchem;

import java.util.*;
import java.text.*;
import java.lang.*;
import java.io.*;

/*
    Controlling class for drawing a molecule in SVG format. This class performs a similar function to that of DrawMolecule, but outputs to
    a text stream instead of a graphics context. Note that in this implementation of rendering, only the molecule is drawn, without
    interactive effects.
*/

public class SVGMolecule implements ArrangeMeasurement
{
    private SVGBuilder svg=new SVGBuilder();
    
    private Molecule mol;
    private double scale=20;
    
    private ArrangeMolecule arrmol;
    
    public SVGMolecule(Molecule mol)
    {
    	this.mol=mol;
	
    	arrmol=new ArrangeMolecule(mol,this);
    	arrmol.setDevRounding(false);
    }
    
    public void draw()
    {
	arrmol.arrange();

    	for (int n=0;n<arrmol.numLines();n++)
	{
    	    int btype=arrmol.lineType(n);
	    double x1=arrmol.lineX1(n),y1=arrmol.lineY1(n),x2=arrmol.lineX2(n),y2=arrmol.lineY2(n);
	    double dx=x2-x1,dy=y2-y1;
	    
	    if (btype==ArrangeMolecule.BLINE_NORMAL)
	    {
    	    	svg.drawLine(x1,y1,x2,y2,arrmol.lineCol(n),arrmol.lineSize(n));
	    }
	    else if (btype==ArrangeMolecule.BLINE_INCLINED)
	    {
	    	double norm=0.15*scale/Math.sqrt(dx*dx+dy*dy);
		double ox=norm*dy,oy=-norm*dx;
		svg.drawPoly(new double[]{x1,x2-ox,x2+ox},new double[]{y1,y2-oy,y2+oy},SVGBuilder.NOCOLOUR,0,arrmol.lineCol(n),true);
	    }
	    else if (btype==ArrangeMolecule.BLINE_DECLINED)
	    {
	    	int nsteps=(int)Math.ceil(Math.sqrt(dx*dx+dy*dy)*0.15);
	    	double norm=0.15*scale/Math.sqrt(dx*dx+dy*dy);
		double ox=norm*dy,oy=-norm*dx;
		for (int i=0;i<=nsteps+1;i++)
		{
		    double cx=x1+i*dx/(nsteps+1),cy=y1+i*dy/(nsteps+1);
		    double ix=ox*i/(nsteps+1),iy=oy*i/(nsteps+1);
		    svg.drawLine(cx-ix,cy-iy,cx+ix,cy+iy,arrmol.lineCol(n),arrmol.lineSize(n));
		}
	    }
	    else if (btype==ArrangeMolecule.BLINE_UNKNOWN)
	    {
	    	int nsteps=(int)Math.ceil(Math.sqrt(dx*dx+dy*dy)*0.2);
	    	double norm=0.2*scale/Math.sqrt(dx*dx+dy*dy);
		double ox=norm*dy,oy=-norm*dx;
		int sz=1+3*(nsteps+1);
		double[] x=new double[sz],y=new double[sz];
		boolean[] ctrl=new boolean[sz];
		x[0]=x1; y[0]=y1; ctrl[0]=false;
		for (int i=0,j=1;i<=nsteps;i++,j+=3)
		{
		    double ax=x1+i*dx/(nsteps+1),ay=y1+i*dy/(nsteps+1);
		    double cx=x1+(i+1)*dx/(nsteps+1),cy=y1+(i+1)*dy/(nsteps+1);
		    double bx=(ax+cx)/2,by=(ay+cy)/2;
		    int sign=i%2==0 ? 1 : -1;
		    
		    x[j]=ax; x[j+1]=bx+sign*ox; x[j+2]=cx;
		    y[j]=ay; y[j+1]=by+sign*oy; y[j+2]=cy;
		    ctrl[j]=true; ctrl[j+1]=true; ctrl[j+2]=false;
		}
		svg.drawCurve(x,y,ctrl,arrmol.lineCol(n),arrmol.lineSize(n),SVGBuilder.NOCOLOUR,false);
	    }
	    else if (btype==ArrangeMolecule.BLINE_DOTTED)
	    {
	    	int nsteps=(int)Math.ceil(Math.sqrt(dx*dx+dy*dy)*0.10);
		float radius=(float)arrmol.lineSize(n);
		for (int i=0;i<=nsteps+1;i++)
		{
		    double cx=x1+i*dx/(nsteps+1),cy=y1+i*dy/(nsteps+1);
		    svg.drawOval(cx,cy,radius,radius,SVGBuilder.NOCOLOUR,0,arrmol.lineCol(n));
		}
	    }
	}
    	for (int n=0;n<arrmol.numPoints();n++)
	{
	    String txt=arrmol.pointText(n);
	    if (txt==null) continue; // is a point, so do not draw anything
	    
	    int fstyle=arrmol.pointBold(n) ? SVGBuilder.TXTSTYLE_BOLD : SVGBuilder.TXTSTYLE_NORMAL;
    	    svg.drawText(arrmol.pointCX(n),arrmol.pointCY(n)+arrmol.pointRH(n),txt,arrmol.pointFontSize(n),
	    	    	 arrmol.pointCol(n),fstyle,SVGBuilder.TXTALIGN_CENTRE);
	}
    }

    // construct the SVG document, and output it to the given writer    
    public void build(PrintWriter out)
    {
    	int w=(int)Math.ceil(svg.highX()-svg.lowX()),h=(int)Math.ceil(svg.highY()-svg.lowY());
    	svg.build(out,w,h,-svg.lowX(),-svg.lowY(),1,1);
    }

    // convenient function for obtaining the SVG output as a string
    public String toString()
    {
    	StringWriter sw=new StringWriter();
	build(new PrintWriter(sw));
	return sw.toString();
    }
    
    // implementation of measurement metrics
    
    public double scale() {return scale;}
    public double angToX(double AX) {return AX*scale;}
    public double angToY(double AY) {return -AY*scale;}
    public double xToAng(double PX) {return PX*scale;}
    public double yToAng(double PY) {return -PY*scale;}
    public double[] measureText(String str,double fontSize) {return svg.measureText(str,fontSize,SVGBuilder.TXTSTYLE_NORMAL);}
}
