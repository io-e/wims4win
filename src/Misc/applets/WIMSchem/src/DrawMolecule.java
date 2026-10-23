/*
    Sketch Elements: Chemistry molecular diagram drawing tool.
    
    (c) 2008 Dr. Alex M. Clark
    
    Released as GNUware, under the Gnu Public License (GPL)
    
    See www.gnu.org for details.
*/

package WIMSchem;

import java.util.*;
import java.text.*;
import java.awt.*;
import java.awt.geom.*;

/*
    Wrapper class for molecule drawing, which renders the molecule to a Java Graphics2D context, under the assumption
    that it is going to be displayed on the screen. This class also has options to draw lots of extra stuff on top of the
    basic molecule construction.
*/

public class DrawMolecule implements ArrangeMeasurement
{
    // public constants

    public static final double DEFSCALE=20; // default # Angstroms per pixel

    // private data
    
    private Molecule mol;
    private Graphics2D g;
    private ArrangeMolecule arrmol=null;

    private final double ASCENT_FUDGE=0.8; // for some #@!& reason, the ascent reserves quite a lot of room at the top

    private Color backgr=Color.WHITE;
    private Color colHighlight=null,colSelected=null,colDragged=null;
    
    private double offsetX=0,offsetY=0; // in Angstrom units
    private double scale=DEFSCALE; // pixels per Angstrom

    private boolean[] selected=null,dragged=null;
    private int highlightAtom=0,highlightBond=0;

    private boolean bondInProgress=false;
    private int bipFrom,bipOrder,bipType;
    private double bipToX,bipToY;
    
    private boolean atomInProgress=false;
    private String aipLabel;
    private double aipToX,aipToY;
    
    private boolean newBondLine=false;
    private double nblX1,nblY1,nblX2,nblY2;

    private boolean dragSelect=false;
    private int dslX1,dslY1,dslX2,dslY2;
    
    private boolean dragScale=false;
    private double dscCX,dscCY,dscExtMul;

    private boolean dragMove=false;
    private double dmvDX,dmvDY;
    private boolean dmvCopy;
    
    private boolean dragRotate=false;
    private double droTheta;
    private int droX,droY;
    
    private boolean outlineTemplate=false;
    private Molecule oltMol;

    // constructor

    public DrawMolecule(Molecule Mol,Graphics2D Gr,double Scale)
    {
    	mol=Mol;
	g=Gr;
	scale=Scale;

    	arrmol=new ArrangeMolecule(mol,this);
    	arrmol.setDevRounding(true);
    }
    
    // methods to provide other drawing options...
    
    public void setOffset(double OX,double OY) {offsetX=OX; offsetY=OY;}
    public void setShowMode(int Mode) {arrmol.setElementMode(Mode);}
    public void setShowHydr(boolean ShowH) {arrmol.setShowHydrogens(ShowH);}
    public void setShowStereo(boolean ShowSter) {arrmol.setAnnotRS(ShowSter); arrmol.setAnnotEZ(ShowSter);}
    public void setBackground(Color Col) {backgr=Col;}
    public void setSelected(boolean[] Selected,boolean[] Dragged) {selected=Selected; dragged=Dragged;}
    public void setHighlight(int Atom,int Bond) {highlightAtom=Atom; highlightBond=Bond;}

    public void bondInProgress(int From,double X,double Y,int Order,int Type)
    {
    	bondInProgress=true;
	bipFrom=From;
	bipToX=X;
	bipToY=Y;
	bipOrder=Order;
	bipType=Type;
    }
    public void atomInProgress(String Label,double X,double Y)
    {
    	atomInProgress=true;
	aipLabel=Label;
	aipToX=X;
	aipToY=Y;
    }
    public void newBondLine(double X1,double Y1,double X2,double Y2)
    {
    	// !! bond order and type should be accounted for...
    	newBondLine=true;
	nblX1=X1;
	nblY1=Y1;
	nblX2=X2;
	nblY2=Y2;
    }
    public void dragSelect(int X1,int Y1,int X2,int Y2)
    {
    	dragSelect=true;
	dslX1=X1;
	dslY1=Y1;
	dslX2=X2;
	dslY2=Y2;
    }
    public void dragScale(double CX,double CY,double ExtMul)
    {
    	dragScale=true;
	dscCX=CX;
	dscCY=CY;
	dscExtMul=ExtMul;
    }
    public void dragMove(double DX,double DY,boolean Copy)
    {
    	dragMove=true;
	dmvDX=DX;
	dmvDY=DY;
	dmvCopy=Copy;
    }
    public void dragRotate(double Theta,int X,int Y)
    {
    	dragRotate=true;
    	droTheta=Theta;
	droX=X;
	droY=Y;
    }
    public void outlineTemplate(Molecule Templ)
    {
    	outlineTemplate=true;
    	oltMol=Templ;
    }
    
    // perform the actual drawing
    
    public void draw()
    {
	arrmol.arrange();	
	setupColours();
	drawBacklighting();
	drawBonds();
	drawAtoms();
	drawEffects();
	drawCorrections();
	if(MainApplet.USER_SELECTION){
	    drawAtomSelection();
	}
    }
    
    public ArrangeMolecule arrangement() {return arrmol;}

    // private implementations
    
    // decides on particular colors
    private void setupColours()
    {
	if (colHighlight==null) colHighlight=backgr.darker();
	if (colSelected==null) colSelected=new Color(colHighlight.getRed(),colHighlight.getGreen(),255);
	if (colDragged==null) colDragged=new Color(colHighlight.getRed(),192,255);
    }

    // draws various types of highlighting, if appropriate
    private void drawBacklighting()
    {
	for (int n=1;n<=mol.numAtoms();n++)
	{	    
    	    boolean drag=false;
	    if (dragged!=null) drag=dragged[n-1];
	    
	    if (selected[n-1] || n==highlightAtom || drag)
	    {
    	    	g.setColor(selected[n-1] ? colSelected : drag ? colDragged : colHighlight);
		double cx=arrmol.pointCX(n-1),cy=arrmol.pointCY(n-1),rw=arrmol.pointRW(n-1),rh=arrmol.pointRH(n-1);
		double ext=rw==0 && rh==0 ? 0.25*scale : Math.max(rw*1.5,rh*1.5);
		g.fill(new Ellipse2D.Double(cx-ext,cy-ext,2*ext,2*ext));
	    }
	}

    	if (highlightBond!=0) for (int n=0;n<arrmol.numLines();n++) if (arrmol.lineBNum(n)==highlightBond)
	{
	    double x1=arrmol.lineX1(n),y1=arrmol.lineY1(n),x2=arrmol.lineX2(n),y2=arrmol.lineY2(n);
	    double sz=arrmol.lineSize(n)+0.10*scale;
	    double dx=x2-x1,dy=y2-y1,norm=sz/Math.sqrt(dx*dx+dy*dy);
	    double ox=norm*dy,oy=-norm*dx;
	    Polygon pgn=new Polygon();
	    pgn.addPoint(Util.iround(x1+oy*0.5),Util.iround(y1-ox*0.5));
	    pgn.addPoint(Util.iround(x1-ox),Util.iround(y1-oy));
	    pgn.addPoint(Util.iround(x2-ox),Util.iround(y2-oy));
	    pgn.addPoint(Util.iround(x2-oy*0.5),Util.iround(y2+ox*0.5));
	    pgn.addPoint(Util.iround(x2+ox),Util.iround(y2+oy));
	    pgn.addPoint(Util.iround(x1+ox),Util.iround(y1+oy));
    	    g.setColor(colHighlight);
	    g.fill(pgn);
	}
    }

    // draws all of the bonds
    private void drawBonds()
    {
    	for (int n=0;n<arrmol.numLines();n++){
    	    int btype=arrmol.lineType(n);
	    double x1=arrmol.lineX1(n),y1=arrmol.lineY1(n),x2=arrmol.lineX2(n),y2=arrmol.lineY2(n);
	    double dx=x2-x1,dy=y2-y1;
	    int factor = 1;/* multiply linewidth when user selected */
	    g.setColor(new Color(arrmol.lineCol(n)));
	    if( MainApplet.USER_SELECTION ){
		if( EditorPane.bondselection.length > mol.numBonds()){ // userbased click selection
		    if( EditorPane.bondselection[arrmol.lineBNum(n)] ){
			g.setColor(MainApplet.BOND_SELECT_COLOR);
			factor = 2;
		    }
		    else
		    {
	    		g.setColor(new Color(arrmol.lineCol(n)));
	    		factor = 1;
		    }
		}
		else
		{
		    EditorPane.bondselection = EditorPane.GrowArray(EditorPane.bondselection,mol.numBonds()+32);
		}
	    }
	    else
	    {
	    	if( MainApplet.ExternalBondSelection != null ){ // for answer molecule ; set by params
    		    for(int i=0;i<MainApplet.ExternalBondSelection.length;i++){
			if( n == MainApplet.ExternalBondSelection[i]){
			    g.setColor(MainApplet.SelectedBondColorArray[i]);
			    factor = 2;
			}
		    }
		}
	    }
	    if (btype==ArrangeMolecule.BLINE_NORMAL){
    	    	g.setStroke(new BasicStroke( (float) (factor*(arrmol.lineSize(n))) ,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
		g.draw(new Line2D.Double(x1,y1,x2,y2));
	    }
	    else if (btype==ArrangeMolecule.BLINE_INCLINED)
	    {
	    	double norm=factor*0.15*scale/Math.sqrt(dx*dx+dy*dy);
		double ox=norm*dy,oy=-norm*dx;
		Path2D path=new Path2D.Double();
		path.moveTo(x1,y1);
		path.lineTo(x2-ox,y2-oy);
		path.lineTo(x2+ox,y2+oy);
		g.fill(path);
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
    	    	    g.setStroke(new BasicStroke((float)(factor*0.05*scale)));
		    g.draw(new Line2D.Double(cx-ix,cy-iy,cx+ix,cy+iy));
		}
	    }
	    else if (btype==ArrangeMolecule.BLINE_UNKNOWN)
	    {
	    	int nsteps=(int)Math.ceil(Math.sqrt(dx*dx+dy*dy)*0.2);
	    	double norm=0.2*scale/Math.sqrt(dx*dx+dy*dy);
		double ox=norm*dy,oy=-norm*dx;
		for (int i=0;i<=nsteps;i++)
		{
		    double ax=x1+i*dx/(nsteps+1),ay=y1+i*dy/(nsteps+1);
		    double cx=x1+(i+1)*dx/(nsteps+1),cy=y1+(i+1)*dy/(nsteps+1);
		    double bx=(ax+cx)/2,by=(ay+cy)/2;
		    int sign=i%2==0 ? 1 : -1;
    	    	    g.setStroke(new BasicStroke((float)(factor*0.05*scale)));
		    g.draw(new QuadCurve2D.Double(ax,ay,bx+sign*ox,by+sign*oy,cx,cy));
		}
	    }
	    else if (btype==ArrangeMolecule.BLINE_DOTTED)
	    {
	    	int nsteps=(int)Math.ceil(Math.sqrt(dx*dx+dy*dy)*0.10);
		float radius=(float)arrmol.lineSize(n);
		for (int i=0;i<=nsteps+1;i++)
		{
		    double cx=x1+i*dx/(nsteps+1),cy=y1+i*dy/(nsteps+1);
		    g.fill(new Ellipse2D.Double(cx-radius,cy-radius,2*radius,2*radius));
		}
	    }
	}

	if (bondInProgress)
	{
	    double x1=arrmol.pointCX(bipFrom-1),y1=arrmol.pointCY(bipFrom-1),x2=angToX(bipToX),y2=angToY(bipToY);
	    double dx=x2-x1,dy=y2-y1,norm=0.20*scale/Math.sqrt(dx*dx+dy*dy);
	    double ox=norm*dy,oy=-norm*dx;

    	    g.setColor(colHighlight);

	    if (bipType==Molecule.BONDTYPE_INCLINED || bipType==Molecule.BONDTYPE_DECLINED)
	    {
		Path2D path=new Path2D.Double();
		path.moveTo(x1,y1);
		path.lineTo(x2-ox,y2-oy);
		path.lineTo(x2+ox,y2+oy);
		path.closePath();
		if (bipType==Molecule.BONDTYPE_INCLINED) g.fill(path); else g.draw(path);
	    } 
	    else if (bipType==Molecule.BONDTYPE_UNKNOWN || bipOrder==0)
	    {
	    	int nsteps=(int)Math.ceil(Math.sqrt(dx*dx+dy*dy)*0.15);
		float radius=(float)(0.15*scale);
		for (int i=0;i<=nsteps+1;i++)
		{
		    double cx=x1+i*dx/(nsteps+1),cy=y1+i*dy/(nsteps+1);
		    g.fill(new Ellipse2D.Double(cx-0.05*scale,cy-0.05*scale,radius,radius));
		}
	    }
	    else
	    {
		double v=-0.5*(bipOrder-1);
    	    	g.setStroke(new BasicStroke((float)(0.1*scale),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
		
		for (int i=0;i<bipOrder;i++,v++)
		    g.draw(new Line2D.Double(x1+ox*v,y1+oy*v,x2+ox*v,y2+oy*v));
	    	
	    }
	}
    }
    
    // draw the atoms proper, as well as standard annotations such as hydrogen atoms and charges
    private void drawAtoms()
    {
    	for (int n=0;n<arrmol.numPoints();n++)
	{
	    String txt=arrmol.pointText(n);
	    if (txt==null) continue; // is a point, so do not draw anything
	    
    	    Font font=new Font("SansSerif",arrmol.pointBold(n) ? Font.BOLD : Font.PLAIN,Util.iround(arrmol.pointFontSize(n)));
	    g.setFont(font);
	    FontMetrics fm=g.getFontMetrics();
	    double ascent=ASCENT_FUDGE*fm.getAscent();
	    float cx=(float)(arrmol.pointCX(n)-0.5*fm.stringWidth(txt)),cy=(float)(arrmol.pointCY(n)+0.5*ascent);
    	    g.setColor(new Color(arrmol.pointCol(n)));
	    g.drawString(txt,cx,cy);

	}
	if(MainApplet.ExternalAtomSelection !=  null){
    	    for (int n=0;n<arrmol.numPoints();n++){
    		for(int i=0;i<MainApplet.ExternalAtomSelection.length;i++){
    		    if( n == MainApplet.ExternalAtomSelection[i]){
    			g.setColor(MainApplet.SelectedAtomColorArray[i]);
    			g.fill(circle(arrmol.pointCX(n-1),arrmol.pointCY(n-1),0.5*scale));
    		    }
    		}    
    	    }
	}
    }
/******** jm.ever 2013 **********/
    private void drawAtomSelection(){
	g.setColor(MainApplet.ATOM_SELECT_COLOR);
	int len1 = mol.numAtoms();
	int len2 = (EditorPane.atomselection).length;
	if( len2 < len1 - 2){EditorPane.atomselection = EditorPane.GrowArray(EditorPane.atomselection,len1+8);}
	for(int n = 1 ; n < len2 ; n++){
	    if (EditorPane.atomselection[n]){
   		g.fill(circle(arrmol.pointCX(n-1),arrmol.pointCY(n-1),0.5*scale));
		//System.out.println("selected n ="+n+" text = "+arrmol.pointText(n));
	    }
	}
    }


/*********************************/

    // draw the effects of various editing-in-progress actions
    private void drawEffects()
    {
    	if (atomInProgress)
	{
	    g.setColor(Color.BLUE);
    	    Font font=new Font("SansSerif",Font.PLAIN,Util.iround(arrmol.getFontSizeDev()));
	    g.setFont(font);
	    double tx=angToX(aipToX),ty=angToY(aipToY);
	    double[] wad=measureText(aipLabel,arrmol.getFontSizeDev());
	    g.drawString(aipLabel,(float)(tx-0.5*wad[0]),(float)(ty+0.4*wad[1]));
	}

    	if (newBondLine)
	{
	    g.setColor(colHighlight);
	    g.draw(new Line2D.Double(angToX(nblX1),angToY(nblY1),angToX(nblX2),angToY(nblY2)));
    	}
	
	if (dragSelect)
	{
	    g.setXORMode(Color.YELLOW);
	    int x=dslX1,y=dslY1,w=dslX2-dslX1,h=dslY2-dslY1;
	    if (w<0) {w=-w; x-=w;}
	    if (h<0) {h=-h; y-=h;}
	    g.drawRect(x,y,w,h);
	    g.setXORMode(Color.BLACK);
	}
	
	if (dragScale)
	{
	    g.setColor(Color.BLACK);
    	    g.setStroke(new BasicStroke(1.1F));
	    for (int n=1;n<=mol.numAtoms();n++) if (selected[n-1])
	    {
	    	double sx=angToX((mol.atomX(n)-dscCX)*dscExtMul+dscCX),sy=angToY((mol.atomY(n)-dscCY)*dscExtMul+dscCY);
    	    	g.draw(new Ellipse2D.Double(sx-scale*0.3,sy-scale*0.3,scale*0.6,scale*0.6));
	    }
    	}
	
	if (dragMove)
	{
	    g.setColor(Color.BLACK);
    	    g.setStroke(new BasicStroke(1.1F));
	    for (int n=1;n<=mol.numAtoms();n++) if (selected[n-1])
	    {
	    	double sx=arrmol.pointCX(n-1)+dmvDX,sy=arrmol.pointCY(n-1)+dmvDY;
    	    	g.draw(new Ellipse2D.Double(sx-scale*0.3,sy-scale*0.3,scale*0.6,scale*0.6));
		if (dmvCopy)
		{
		    g.draw(new Line2D.Double(sx-scale*0.15,sy,sx+scale*0.15,sy));
		    g.draw(new Line2D.Double(sx,sy-scale*0.15,sx,sy+scale*0.15));
		}
	    }
    	}
	
    	if (dragRotate)
	{
	    double thrad=droTheta*Math.PI/180;
	    g.setColor(Color.RED);
	    g.setStroke(new BasicStroke(0.5F,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,1F,new float[]{2,2},0));
	    g.draw(new Line2D.Double(droX,droY,droX+50,droY));
	    g.setStroke(new BasicStroke(1F));
	    g.draw(new Line2D.Double(droX,droY,droX+50*Math.cos(-thrad),droY+50*Math.sin(-thrad)));
	    g.draw(new Arc2D.Double(droX-20,droY-20,40,40,0,droTheta,Arc2D.OPEN));

    	    Font font=new Font("SansSerif",Font.PLAIN,12);
	    g.setFont(font);

    	    int ty=(int)((droTheta>25 || (droTheta<0 && droTheta>=-25)) ? droY-5 : droY+5+ASCENT_FUDGE*font.getSize());
	    DecimalFormat fmt=new DecimalFormat("0");
	    g.drawString((droTheta>0?"+":"")+fmt.format(Math.round(droTheta))+"\u00B0",(int)(droX+25),ty);
	    
	    double ax=xToAng(droX),ay=yToAng(droY);
    	    g.setStroke(new BasicStroke(1.1F));
	    for (int n=1;n<=mol.numAtoms();n++) if (selected[n-1])
	    {
	    	double rx=mol.atomX(n)-ax,ry=mol.atomY(n)-ay;
		double rth=Math.atan2(ry,rx),ext=Math.sqrt(rx*rx+ry*ry);
		rx=ax+ext*Math.cos(rth+thrad);
		ry=ay+ext*Math.sin(rth+thrad);
		g.draw(new Ellipse2D.Double(angToX(rx)-scale*0.3,angToY(ry)-scale*0.3,scale*0.6,scale*0.6));
	    }
	}
		    
    	if (outlineTemplate)
	{
	    g.setColor(new Color(128,128,128));
    	    g.setStroke(new BasicStroke(1));
	    for (int n=1;n<=oltMol.numBonds();n++)
	    {
	    	int from=oltMol.bondFrom(n),to=oltMol.bondTo(n);
		g.draw(new Line2D.Double(angToX(oltMol.atomX(from)),angToY(oltMol.atomY(from)),
		    	    	    	 angToX(oltMol.atomX(to)),angToY(oltMol.atomY(to))));
	    }
	}
    }
    
    // bring attention to structures which are malformed
    private void drawCorrections()
    {
	// see if any atoms severely overlap, and if so, draw a red circle around them
	for (int i=1;i<=mol.numAtoms()-1;i++)
	for (int j=i+1;j<=mol.numAtoms();j++)
	{
	    double dx=mol.atomX(i)-mol.atomX(j),dy=mol.atomY(i)-mol.atomY(j);
	    if (dx*dx+dy*dy<0.2*0.2)
	    {
	    	g.setColor(Color.RED);
	    	g.setStroke(new BasicStroke(0.5F));
	    	g.draw(new Ellipse2D.Double(arrmol.pointCX(i-1)-scale*0.25,arrmol.pointCY(i-1)-scale*0.25,scale*0.5,scale*0.5));
	    }
	}
    }

    // implementation of measurement metrics
    
    public double scale() {return scale;}

    public double angToX(double AX) {return (offsetX+AX)*scale;}
    public double angToY(double AY) {return (offsetY-AY)*scale;}
    public double xToAng(double PX) {return (PX/scale)-offsetX;}
    public double yToAng(double PY) {return (-PY/scale)+offsetY;}

    public double[] measureText(String str,double fontSize)
    {
    	Font font=new Font("SansSerif",Font.PLAIN,(int)Math.round(fontSize));
	FontMetrics fm=g.getFontMetrics(font);
	return new double[]{fm.stringWidth(str),fm.getAscent()*ASCENT_FUDGE,fm.getDescent()};
    }

    private Ellipse2D circle(double x, double y, double radius){
	double newX = x - radius;
        double newY = y - radius;
	double diam = 2*radius;
        Ellipse2D ellipse = new Ellipse2D.Double(newX, newY, diam, diam );
	return ellipse;
    }
}



