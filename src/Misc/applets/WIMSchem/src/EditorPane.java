/*
    Sketch Elements: Chemistry molecular diagram drawing tool.
    
    (c) 2005 Dr. Alex M. Clark
    
    Released as GNUware, under the Gnu Public License (GPL)
    
    See www.gnu.org for details.
*/

package WIMSchem;

import java.io.*;
import java.util.*;
import java.text.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import javax.swing.*;
import javax.swing.event.*;

/*
    Custom widget for viewing and editing a molecular structure. The widget is a rather heavy one, and features a lot of the work-horse
    functions for various tools, including the specific details of user interaction, and drawing of the structure itself, with various
    annotations for editing and selection. Can optionally be used as a lightweight widget with just view and selection.
*/

public class EditorPane extends JComponent 
    implements MouseListener, MouseMotionListener, FocusListener, KeyListener, MouseWheelListener, ComponentListener
{
    private Molecule mol;
    private boolean editable=true,hasBorder=false,autoScale=false;
    private static final double IDEALBOND=1.5; // stylised bond distance (Angstroms)
    // note: px=(atomX+offsetX)*scale; ax=px/scale-offsetX; offsetX=px/scale-ax (and same for Y)
    private double offsetX=0,offsetY=0; // in Angstrom units
    private double scale=DrawMolecule.DEFSCALE; // pixels per Angstrom
    public boolean[] selected=null,dragged=null;
    private double[] px=null,py=null,rw=null,rh=null;
    // !! private double[] bfx=null,bfy=null,btx=null,bty=null;
    private int highlightAtom=0,highlightBond=0;
    boolean showHydr=false;
    /*** jm.evers 27/12/2008 **************/
    public static boolean[] bondselection = null;
    public static boolean[] atomselection = null;
    /***************************************/   
    
    private int showMode=ArrangeMolecule.SHOW_ELEMENTS;
    private boolean showSter = false;

    private static final int TOOL_CURSOR=1;
    private static final int TOOL_ROTATOR=2;
    private static final int TOOL_ERASOR=3;
    private static final int TOOL_ATOM=4;
    private static final int TOOL_BOND=5;
    private static final int TOOL_CHARGE=6;
    private static final int TOOL_TEMPLATE=7;
    /***** jm.evers 4/3/2010  ************/
    private static final int TOOL_SELECT=8;
    /************************ ************/
    
    private static final int DRAG_SELECT=1;
    private static final int DRAG_MOVE=2;
    private static final int DRAG_COPY=3;
    private static final int DRAG_SCALE=4;
    private static final int DRAG_ROTATE=5;
    
    private int trackX=-1,trackY=-1; // last seen position of mouse
    
    private boolean isSelectionPane=false; // false=is for editing; true=is for viewing and selecting only
    private int selBoxW=0,selBoxH=0; // size to this, for selection panes
    
    private MolSelectListener selectListen=null;
        
    private int tool=0;
    private int toolDragReason=0;
    private double toolDragX1,toolDragY1,toolDragX2,toolDragY2;
    private String toolAtomType=null;
    private boolean toolAtomDrag,toolAtomSnap;
    private int toolAtomEditSel=0,toolAtomEditX,toolAtomEditY;
    private JTextField toolAtomEditBox=null;

    private int toolBondOrder=0,toolBondType=0,toolBondFrom=0,toolBondHit=0;
    private double toolBondFromX=0,toolBondFromY=0,toolBondToX=0,toolBondToY=0;
    private boolean toolSnap,toolBondDrag=false;
    
    private int toolCharge=0;
    
    private static final int UNDO_LEVELS=10;
    private class EditState
    {
    	Molecule Molecule;
	boolean Selected[];
    };
    private EditState[] undo=null,redo=null;
    
    private Molecule template=null,templDraw=null;
    private int templateIdx=0;
    
    private Molecule lastCleanMol=null;
    private boolean lastDirty=false;

    // ------------------ public functions --------------------

    // Constructor for fully-fledged interactive editing panes.
    public EditorPane() {init();}

    // Constructor for "selection only" editor panes.
    public EditorPane(int width,int height) 
    {
    	isSelectionPane=true; 
	selBoxW=width;
	selBoxH=height;
	init();
    }
    
    private void init()
    {
    	mol=new Molecule();
	/**** jm.evers ********/
	if(MainApplet.USER_SELECTION){
	    atomselection=new boolean[mol.numAtoms()+32];
	    bondselection=new boolean[mol.numBonds()+32];
	    for(int p=0; p<mol.numAtoms()+32 ; p++){
		atomselection[p] = false;
		bondselection[p] = false;
	    }
	}
	/**********************/

    	determineSize();
	addMouseListener(this);
	addMouseMotionListener(this);
	addMouseWheelListener(this);
	addComponentListener(this);
    }
    
    // call this function to cause the editor to become a receptacle for dragged molecules
    public void enableDnD() 
    {
    	setTransferHandler(new TransferMoleculeDest(this));
    }
    
    // obtain underlying molecule; not a copy, careful about modifying
    public Molecule molData() {return mol;}
    
    public boolean isEmpty() {return mol.numAtoms()==0;}
    
    // unit operation equivalent to deleting all atoms
    public void clear()
    {
    	cacheUndo();
    	if(MainApplet.USER_SELECTION){
    	    atomselection = new boolean[256];
    	    bondselection = new boolean[256];
    	}
    	mol=new Molecule();
	clearTemporary();
	determineSize();
	repaint();
	
	checkDirtiness();
    }

    // override the underlying molecule
    public void replace(Molecule Mol) {replace(Mol,false,true);}
    public void replace(Molecule Mol,boolean ClearSelection) {replace(Mol,ClearSelection,true);}
    public void replace(Molecule Mol,boolean ClearSelection,boolean Repaint) 
    {
    	if (mol.numAtoms()!=Mol.numAtoms()) ClearSelection=true;
    	mol=Mol;
	clearTemporary(ClearSelection);
	if (Repaint) repaint();
    }
    
    // set which object, if any, gets a response when an atom is "selected" with a mouse click
    public void setMolSelectListener(MolSelectListener listen) {selectListen=listen;}
    
    // by default the editor pane captures lots of events and allows much editor; this function can be used to turn it off
    public void setEditable(boolean Editable) {editable=Editable;}
    
    // if true, will draw a border around the edge
    public void setBorder(boolean HasBorder) {hasBorder=HasBorder;}

    // if true, every time the size changes, the molecule will scale-to-fit    
    public void setAutoScale(boolean AutoScale) {autoScale=AutoScale;}

    // informs the editor that the current state has been synchronised with what is in a disk file, or something equivalent
    public void notifySaved()
    {
    	lastCleanMol=mol.clone();
	lastDirty=false;
	if (selectListen!=null) selectListen.dirtyChanged(false);
    }
    
    // dirty==true when there have been some changes since the last modification
    public boolean isDirty() {return lastDirty;}
    
    // checks to see whether the current molecule is the same as the last saved state; notifies if different; note that this is done by
    // an actual molecule comparison, which makes tracking changes a lot simpler, and also a {do something/restore it somehow} sequence
    // is functionally equivalent to undo, which is nice
    private void checkDirtiness()
    {
    	boolean nowDirty=mol.compareTo(lastCleanMol)!=0;
	
    	if (nowDirty!=lastDirty)
	{
	    if (selectListen!=null) selectListen.dirtyChanged(nowDirty);
	    lastDirty=nowDirty;
	}
    }
        
    // affect the way the molecule is rendered
    public void setShowMode(int ShowMode)
    {
    	if (showMode==ShowMode) return;
	showMode=ShowMode;
	repaint();
    }
    public void setShowHydrogens(boolean ShowHydr)
    {
    	if (showHydr==ShowHydr) return;
	showHydr=ShowHydr;
	repaint();
    }
    public void setShowStereolabels(boolean ShowSter)
    {
    	if (showSter==ShowSter) return;
	showSter=ShowSter;
	repaint();
    }
    
    // notify selection of various tools
    public void setToolCursor() 
    {
    	tool=TOOL_CURSOR; 
	repaint();
    }
    public void setToolRotator()
    {
    	tool=TOOL_ROTATOR;
	repaint();
    }
    public void setToolErasor() 
    {
    	tool=TOOL_ERASOR; 
	repaint();
    }
    public void setToolAtom(String Atom) 
    {
    	tool=TOOL_ATOM;
	toolAtomType=Atom;
    	toolAtomDrag=false;
	toolAtomSnap=false;
    	toolBondFrom=0;
    	toolBondToX=0;
	toolBondToY=0;
	repaint();
    }
    public void setToolBond(int Order)
    {
    	tool=TOOL_BOND;
	toolBondFrom=0;
	if (Order>=0) {toolBondOrder=Order; toolBondType=Molecule.BONDTYPE_NORMAL;}
	else 
	{
	    toolBondOrder=1;
	    if (Order==-1) toolBondType=Molecule.BONDTYPE_INCLINED;
	    else if (Order==-2) toolBondType=Molecule.BONDTYPE_DECLINED;
	    else if (Order==-3) toolBondType=Molecule.BONDTYPE_UNKNOWN;
	}
	repaint();
    }
    public void setToolCharge(int DChg)
    {
    	tool=TOOL_CHARGE;
	toolCharge=DChg;
    }
    public void setToolTemplate(Molecule Templ,int Idx)
    {
    	tool=TOOL_TEMPLATE;
    	template=Templ;
    	templateIdx=Idx;
	repaint();
    }
    
    // whether or not there is anything in the undo/redo stacks
    public boolean canUndo() {return undo!=null && undo[0]!=null;}
    public boolean canRedo() {return redo!=null && redo[0]!=null;}
    
    // cause the actual undo/redo to happen
    public void undo()
    {
    	if (!canUndo()) return;
	
	if (redo==null) redo=new EditState[UNDO_LEVELS];
	for (int n=UNDO_LEVELS-1;n>0;n--) redo[n]=redo[n-1];
    	redo[0]=new EditState();
	redo[0].Molecule=mol;
	redo[0].Selected=selected;
	
	mol=undo[0].Molecule;
	selected=undo[0].Selected;
	for (int n=0;n<UNDO_LEVELS-1;n++) undo[n]=undo[n+1];
	clearTemporary(false);
	determineSize();
	repaint();
	
	checkDirtiness();
    }
    public void redo()
    {
    	if (!canRedo()) return;
	
	if (undo==null) undo=new EditState[UNDO_LEVELS];
	for (int n=UNDO_LEVELS-1;n>0;n--) undo[n]=undo[n-1];
	undo[0]=new EditState();
	undo[0].Molecule=mol;
	undo[0].Selected=selected;
	
	mol=redo[0].Molecule;
	selected=redo[0].Selected;
	for (int n=0;n<UNDO_LEVELS-1;n++) redo[n]=redo[n+1];
	
	clearTemporary(false);
	determineSize();
	repaint();
	
	checkDirtiness();
    }
    
    // fits the molecule on the screen and centres everything; very pleasant thing to have at certain junctures, but not too often
    public void scaleToFit() {scaleToFit(20);}
    public void scaleToFit(double MaxScale)
    {
	clearTemporary();

    	double mw=2+mol.rangeX(),mh=2+mol.rangeY();
	Rectangle vis=getVisibleRect();
	if (vis.getWidth()==0 || vis.getHeight()==0) vis=new Rectangle(0,0,getWidth(),getHeight());
	double sw=selBoxW>vis.getWidth() ? selBoxW : vis.getWidth();
	double sh=selBoxH>vis.getHeight() ? selBoxH : vis.getHeight();
    	scale=Math.min(Math.min(sw/mw,sh/mh),MaxScale);
	offsetX=0.5*(sw/scale-mol.rangeX())-mol.minX();
	offsetY=0.5*(sh/scale-mol.rangeY())+mol.maxY();
    }

    // change the magnification, and adjust scrollbars etc accordingly
    public void zoomFull() 
    {
    	scaleToFit();
    	determineSize();
    	repaint();
    }
    public void zoomIn(double Mag)
    {
    	scale*=Mag;
    	determineSize();
    	repaint();
    }
    public void zoomOut(double Mag)
    {
    	scale/=Mag;
    	determineSize();
    	repaint();
    }
    /****** jm.evers ****************/
     public void deSelectAll(){
       if(MainPanel.appletMode){
        int i = atomselection.length;
        for(int p = 0 ; p < i ; p++ ){
         atomselection[p] = false;
        }
        i = bondselection.length;
        for(int p = 0 ; p < i ; p++ ){
         bondselection[p] = false;
        }
        System.out.println("EditorPane deSelectAll()");
        repaint();
       }    
    }
    public void Select(){
        System.out.println("EditorPane Select()");
	tool = TOOL_SELECT;
    }
    /*********************************/
                              
    
    // select all atoms
    public void selectAll()
    {
    	selected=new boolean[mol.numAtoms()];
	for (int n=0;n<mol.numAtoms();n++) selected[n]=true;
    	repaint();
    }
    
    // finds a nice place to put the new fragment which does not overlap existing content, then appends the atoms & bonds; note that
    // ownership of Frag is assumed
    public void addArbitraryFragment(Molecule Frag)
    {
    	if (Frag.numAtoms()==0) return;
    
    	cacheUndo();
    	if (mol.numAtoms()==0) 
	{
	    mol=Frag;
	    clearTemporary();
	    scaleToFit();
	    determineSize();
	    repaint();
	    checkDirtiness();
	    return;
	}
    
    	final double dirX[]={1,0,-1,1,-1,1,0,-1},dirY[]={1,1,1,0,0,-1,-1,-1};
    	double dx[]=new double[8],dy[]=new double[8],score[]=new double[8];
	
	for (int n=0;n<8;n++)
	{
	    double vx=dirX[n],vy=dirY[n];

	    if (n==0 || n==3 || n==5) {dx[n]=mol.minX()-Frag.maxX();}
	    else if (n==2 || n==4 || n==7) {dx[n]=mol.maxX()-Frag.minX();}
	    else dx[n]=0.5*(mol.minX()+mol.maxX()-Frag.minX()-Frag.maxX());
	    
	    if (n==5 || n==6 || n==7) {dy[n]=mol.minY()-Frag.maxY();}
	    else if (n==0 || n==1 || n==2) {dy[n]=mol.maxY()-Frag.minY();}
	    else dy[n]=0.5*(mol.minY()+mol.maxY()-Frag.minY()-Frag.maxY());
	    
	    dx[n]-=vx;
	    dy[n]-=vy;
	    score[n]=fragPosScore(Frag,dx[n],dy[n]);
	    
	    vx*=0.25;
	    vy*=0.25;
	    for (int iter=100;iter>0;iter--)
	    {
	    	double iscore=fragPosScore(Frag,dx[n]+vx,dy[n]+vy);
		if (iscore<=score[n]) break;
		score[n]=iscore;
		dx[n]+=vx;
		dy[n]+=vy;
	    }
	    for (int iter=100;iter>0;iter--) for (int d=0;d<8;d++)
	    {
	    	vx=dirX[d]*0.1;
		vy=dirY[d]*0.1;
	    	double iscore=fragPosScore(Frag,dx[n]+vx,dy[n]+vy);
		if (iscore<=score[n]) break;
		score[n]=iscore;
		dx[n]+=vx;
		dy[n]+=vy;
	    }
	}
	
	int best=0;
	for (int n=1;n<8;n++) if (score[n]>score[best]) best=n;
	
	for (int n=1;n<=Frag.numAtoms();n++) Frag.setAtomPos(n,Frag.atomX(n)+dx[best],Frag.atomY(n)+dy[best]);
	int base=mol.numAtoms();
	mol.append(Frag);
	
    	clearTemporary();
	scaleToFit();
	determineSize();
	
	selected=new boolean[mol.numAtoms()];
	for (int n=0;n<mol.numAtoms();n++) selected[n]=n>=base;
	
	repaint();
	checkDirtiness();
    }

    // like above, except that the destination centre of gravity for positioning the new fragment is provided, in 
    // component coordinates; note that ownership of Frag is claimed
    private void addFragmentPosition(Molecule Frag,int X,int Y)
    {
    	if (Frag.numAtoms()==0) return;
	
    	double cx=xToAng(X),cy=yToAng(Y);
	double loX=Frag.atomX(1),hiX=loX,loY=Frag.atomY(1),hiY=loY;
	for (int n=2;n<=Frag.numAtoms();n++)
	{
	    loX=Math.min(loX,Frag.atomX(n));
	    hiX=Math.max(hiX,Frag.atomX(n));
	    loY=Math.min(loY,Frag.atomY(n));
	    hiY=Math.max(hiY,Frag.atomY(n));
	}
	cx-=0.5*(loX+hiX);
	cy-=0.5*(loY+hiY);
	for (int n=1;n<=Frag.numAtoms();n++) Frag.setAtomPos(n,Frag.atomX(n)+cx,Frag.atomY(n)+cy);
	
    	cacheUndo();
	int base=mol.numAtoms();
	mol.append(Frag);
	
	clearTemporary();
	scaleToFit();
	determineSize();
	
	selected=new boolean[mol.numAtoms()];
	for (int n=0;n<mol.numAtoms();n++) selected[n]=n>=base;
	
	repaint();
	checkDirtiness();
    }
    
    // scoring function for above: more congested is better, but any two atoms < 1A = zero; post-biased to favour square aspect ratio
    private double fragPosScore(Molecule Frag,double DX,double DY)
    {
    	double score=0;
    	for (int i=1;i<=mol.numAtoms();i++)
	for (int j=1;j<=Frag.numAtoms();j++)
	{
	    double dx=Frag.atomX(j)+DX-mol.atomX(i),dy=Frag.atomY(j)+DY-mol.atomY(i);
	    double dist2=dx*dx+dy*dy;
	    if (dist2<1) return 0;
	    score+=1/dist2;
	}
	double minX=Math.min(Frag.minX()+DX,mol.minX()),maxX=Math.max(Frag.maxX()+DX,mol.maxX());
	double minY=Math.min(Frag.minY()+DY,mol.minY()),maxY=Math.max(Frag.maxY()+DY,mol.maxY());
	double rangeX=Math.max(1,maxX-minX),rangeY=Math.max(1,maxY-minY);
	double ratio=Math.max(rangeX/rangeY,rangeY/rangeX);
	return score/ratio;
    }
    
    // returns the # of atoms in selection set
    public int countSelected()
    {
    	if (selected==null) return 0;
	int num=0;
	for (int n=0;n<mol.numAtoms();n++) if (selected[n]) num++;
	return num;
    }
    
    // returns array of atoms which are presently selected, or everything if none
    public ArrayList<Integer> selectedIndices()
    {
    	ArrayList<Integer> selidx=new ArrayList<Integer>();
    	if (selected!=null) for (int n=0;n<mol.numAtoms();n++) if (selected[n]) selidx.add(n+1);
	if (selidx.size()==0) for (int n=1;n<=mol.numAtoms();n++) selidx.add(n);
	return selidx;
    }
    
    // returns a subgraph of the molecule corresponding to selected atoms - or if none, the whole thing
    public Molecule selectedSubgraph()
    {
    	if (selected==null) return mol.clone();
	int sum=0;
	for (int n=0;n<mol.numAtoms();n++) if (selected[n]) sum++;
	if (sum==0) return mol.clone();

	return mol.subgraph(selected);
    }
    
    // deletes selected atoms, or all atoms if none selected
    public void deleteSelected()
    {
    	cacheUndo();

    	boolean anySelected=false;
	if (selected!=null) for (int n=0;n<mol.numAtoms();n++) if (selected[n]) {anySelected=true; break;}
	if (!anySelected) return;

	for (int n=mol.numAtoms()-1;n>=0;n--) if (selected[n]) mol.deleteAtomAndBonds(n+1);

    	clearTemporary();
	determineSize();
	repaint();
	checkDirtiness();
    }
    
    // switch between explicit/implicit modes; going to explicit marks current calculated value as absolute
    public void hydrogenSetExplicit(boolean IsExpl) {hydrogenSetExplicit(IsExpl,Molecule.HEXPLICIT_UNKNOWN);}
    public void hydrogenSetExplicit(boolean IsExpl,int NumExpl)
    {
	cacheUndo();

    	ArrayList<Integer> sel=selectedIndices();
	for (int n=0;n<sel.size();n++) 
	{
	    int i=sel.get(n).intValue();
	    if (IsExpl) mol.setAtomHExplicit(i,mol.atomHydrogens(i)); else mol.setAtomHExplicit(i,NumExpl);
	}
	repaint();

	checkDirtiness();
    }
    
    // any hydrogens which are implicit or explicit are actually created as nodes in the molecular graph; the explicit value of each
    // parent is set to unknown
    public void hydrogenCreateActual()
    {
	cacheUndo();

    	ArrayList<Integer> sel=selectedIndices();
	int score[]=new int[360];
	for (int n=0;n<sel.size();n++)
	{
	    int i=sel.get(n).intValue();
	    int hy=mol.atomHydrogens(i);
	    if (hy==0) continue;
	    
	    for (int j=0;j<360;j++) score[j]=0;
	    int adj[]=mol.atomAdjList(i);
	    for (int j=0;j<adj.length;j++) 
	    {
	    	int iang=(int)(Math.atan2(mol.atomY(adj[j])-mol.atomY(i),mol.atomX(adj[j])-mol.atomX(i))*180/Math.PI);
		if (iang<0) iang+=360;
		score[iang]=-1; score[(iang+1)%360]=-1; score[(iang+359)%360]=-1;
		int i0=(iang+180)%360,i1=(iang+120)%360,i2=(iang+240)%360;
		if (score[i0]>=0) score[i0]+=2;
		if (score[i1]>=0) score[i1]+=4;
		if (score[i2]>=0) score[i2]+=4;
	    }
	    
	    while (hy>0)
	    {
	    	int iang=0;
		for (int j=1;j<360;j++) if (score[j]>score[iang]) iang=j;
		int num=mol.addAtom("H",mol.atomX(i)+Math.cos(iang*Math.PI/180.0),mol.atomY(i)+Math.sin(iang*Math.PI/180.0));
		mol.addBond(i,num,1);
		score[iang]=-1; score[(iang+1)%360]=-1; score[(iang+359)%360]=-1;
		int i0=(iang+180)%360,i1=(iang+120)%360,i2=(iang+240)%360;
		if (score[i0]>=0) score[i0]++;
		if (score[i1]>=0) score[i1]+=2;
		if (score[i2]>=0) score[i2]+=2;
	    	hy--;
	    }
	    
	    mol.setAtomHExplicit(i,Molecule.HEXPLICIT_UNKNOWN);
	}
    
    	clearTemporary();
	determineSize();
	repaint();

	checkDirtiness();
    }
    
    // of all the selected atoms and their neighbours, removes any which are element H
    public void hydrogenDeleteActual()
    {
    	ArrayList<Integer> sel=selectedIndices(),chop=new ArrayList<Integer>();
	for (int n=0;n<sel.size();n++)
	{
	    int i=sel.get(n).intValue();
	    if (mol.atomElement(i).compareTo("H")==0) chop.add(new Integer(i));
	    int adj[]=mol.atomAdjList(i);
	    for (int j=0;j<adj.length;j++) if (mol.atomElement(adj[j]).compareTo("H")==0) chop.add(adj[j]);
	}
	
	if (chop.size()==0) return;
	cacheUndo();
	Collections.sort(chop);
	
	for (int n=0;n<chop.size();n++)
	{
	    int adj[]=mol.atomAdjList(chop.get(n).intValue());
	    for (int i=0;i<adj.length;i++) mol.setAtomHExplicit(adj[i],Molecule.HEXPLICIT_UNKNOWN);
	}

	int decr=0,lastVal=-1;
	for (int n=0;n<chop.size();n++)
	{
	    int i=chop.get(n).intValue();
	    if (i==lastVal) continue;
	    mol.deleteAtomAndBonds(i-decr);
	    decr++;
	    lastVal=i;
	}
    
    	clearTemporary();
	determineSize();
	repaint();

	checkDirtiness();
    }
    
    // scale bond lengths to reasonable values (note: affects all atoms, selected or not)
    public void normaliseBondLengths()
    {
    	double numer=0,denom=0;
	for (int n=1;n<=mol.numBonds();n++)
	{
	    double dx=mol.atomX(mol.bondFrom(n))-mol.atomX(mol.bondTo(n)),dy=mol.atomY(mol.bondFrom(n))-mol.atomY(mol.bondTo(n));
	    double weight=mol.bondInRing(n) ? 1 : 2;
	    numer+=Math.sqrt(dx*dx+dy*dy)*weight;
	    denom+=weight;
	}
	if (denom==0) return;
	
	cacheUndo();
    	
	double stretch=IDEALBOND*denom/numer;
    	for (int n=1;n<=mol.numAtoms();n++)
	{
	    mol.setAtomPos(n,mol.atomX(n)*stretch,mol.atomY(n)*stretch);
	}

	clearTemporary();
	determineSize();
	repaint();
    }
    
    // select next/prev atoms or connected components
    public void cycleSelection(boolean Forward,boolean Group)
    {
    	if (mol.numAtoms()<=1) return;
	
    	int high=0;
	if (selected!=null) for (int n=1;n<=mol.numAtoms();n++) if (selected[n-1])
	{
	    if (Group) {if (mol.atomConnComp(n)>high) high=mol.atomConnComp(n);}
	    else {high=n;}
	}
	int max=Group ? 0 : mol.numAtoms();
	if (Group) for (int n=1;n<=mol.numAtoms();n++) if (mol.atomConnComp(n)>max) max=mol.atomConnComp(n);
	
	int pos=Forward ? high+1 : high-1;
	if (pos<1) pos=max;
	if (pos>max) pos=1;
	
	selected=new boolean[mol.numAtoms()];
	for (int n=1;n<=mol.numAtoms();n++)
	{
	    if (Group) {selected[n-1]=mol.atomConnComp(n)==pos;}
	    else {selected[n-1]=n==pos;}
	}
	
	clearTemporary(false);
	repaint();
    }
    
    // move selected atoms by a small translation
    public void nudgeSelectedAtoms(double DX,double DY)
    {
    	if (selected==null) return;
    	cacheUndo();	
	for (int n=1;n<=mol.numAtoms();n++) if (selected[n-1]) mol.setAtomPos(n,mol.atomX(n)+DX,mol.atomY(n)+DY);

	clearTemporary(false);
	determineSize();
	repaint();
    }
    
    // selected atoms are inverted about a mirror plane coincident with their centre of gravity
    public void flipSelectedAtoms(boolean Vertical)
    {
    	if (selected==null) return;
	
	int count=0;
	double cx=0,cy=0;
	for (int n=1;n<=mol.numAtoms();n++) if (selected[n-1]) {cx+=mol.atomX(n); cy+=mol.atomY(n); count++;}
	if (count==0) return;
	
    	cacheUndo();	

	cx/=count;
	cy/=count;
	for (int n=1;n<=mol.numAtoms();n++) if (selected[n-1]) 
	{
	    if (Vertical) mol.setAtomPos(n,mol.atomX(n),2*cy-mol.atomY(n));
	    else mol.setAtomPos(n,2*cx-mol.atomX(n),mol.atomY(n));
	}
	
	clearTemporary(false);
	determineSize();
	repaint();
    }
    
    // selected atoms are rotated about their centre of gravity
    public void rotateSelectedAtoms(double Degrees)
    {
    	if (selected==null) return;
	
	int count=0;
	double cx=0,cy=0;
	for (int n=1;n<=mol.numAtoms();n++) if (selected[n-1]) {cx+=mol.atomX(n); cy+=mol.atomY(n); count++;}
	if (count==0) return;
	
    	cacheUndo();	

	cx/=count;
	cy/=count;
    	double radians=Degrees*Math.PI/180;
	for (int n=1;n<=mol.numAtoms();n++) if (selected[n-1]) 
	{
	    double dx=mol.atomX(n)-cx,dy=mol.atomY(n)-cy;
	    double dist=Math.sqrt(dx*dx+dy*dy),theta=Math.atan2(dy,dx);
	    mol.setAtomPos(n,cx+dist*Math.cos(theta+radians),cy+dist*Math.sin(theta+radians));
	}

	clearTemporary(false);
	determineSize();
	repaint();
    }
    
    
    // changes stereochemistry; STEREO_UNKNOWN=invert, POS/NEG=set to this
    public void setStereo(int Operation)
    {
    	ArrayList<Integer> selidx=selectedIndices();

    	int[][] graph=new int[mol.numAtoms()][];
	for (int n=0;n<mol.numAtoms();n++) graph[n]=mol.atomAdjList(n+1);

	cacheUndo();

    	// chiral centres
	for (int n=0;n<selidx.size();n++) 
	{
	    int a=selidx.get(n);
	    int ster=mol.atomChirality(a);
	    if (Operation==Molecule.STEREO_UNKNOWN)
	    	{if (ster!=Molecule.STEREO_POS && ster!=Molecule.STEREO_NEG) continue;}
	    else 
	    	{if (ster==Operation) continue;}

    	    // first the easy option: the atom already has chirality, can just flip all the wedges...
	    if (ster==Molecule.STEREO_POS || ster==Molecule.STEREO_NEG)
	    {
	    	for (int i=1;i<=mol.numBonds();i++) 
		    if (mol.bondFrom(i)==a)
		{
		    if (mol.bondType(i)==Molecule.BONDTYPE_INCLINED) mol.setBondType(i,Molecule.BONDTYPE_DECLINED);
		    else if (mol.bondType(i)==Molecule.BONDTYPE_DECLINED) mol.setBondType(i,Molecule.BONDTYPE_INCLINED);
    	    	}
	    	continue;
	    }
	    
	    // not quite so easy: centre has no current chirality, and a specific enantiomer has been requested
	    ArrayList<int[]> perm=wedgeFormations(a,Operation);
	    if (perm!=null && perm.size()>0) // if anything available, use best...
	    {
	    	int[] adj=mol.atomAdjList(a);
    	    	for (int i=0;i<adj.length;i++)
		{
		    int j=mol.findBond(a,adj[i]); if (j==0) continue;
		    mol.setBondType(j,perm.get(0)[i]<0 ? Molecule.BONDTYPE_DECLINED 
		                    : perm.get(0)[i]>0 ? Molecule.BONDTYPE_INCLINED : Molecule.BONDTYPE_NORMAL);
    	    	    if (mol.bondFrom(j)!=a) mol.setBondFromTo(j,mol.bondTo(j),mol.bondFrom(j));
		}
	    }
	}
		
	// cis/trans 
	for (int n=1;n<=mol.numBonds();n++) 
	{
	    int bf=mol.bondFrom(n),bt=mol.bondTo(n);
	    if (mol.bondOrder(n)==2 && selidx.indexOf(bf)<0 && selidx.indexOf(bt)<0) continue;
	    int ster=mol.bondStereo(n);
	    if ((ster!=Molecule.STEREO_POS && ster!=Molecule.STEREO_NEG) || ster==Operation) continue;
	    if (mol.atomRingBlock(bf)!=0 && mol.atomRingBlock(bf)!=mol.atomRingBlock(bt)) continue; // refuse to work with ring alkene
	    
	    // classify the sides of the X=Y bond by partitioning the component
	    int sc1=1,sc2=1,side[]=new int[mol.numAtoms()];
	    for (int i=0;i<mol.numAtoms();i++) side[i]=0;
	    side[bf-1]=1; side[bt-1]=2;
	    while (true)
	    {
	    	boolean changed=false;
		for (int i=0;i<mol.numAtoms();i++) if (side[i]==0)
		    for (int j=0;j<graph[i].length;j++) if (side[graph[i][j]-1]!=0) 
		{
		    side[i]=side[graph[i][j]-1];
		    if (side[i]==1) sc1++; else sc2++;
		    changed=true;
		}
		if (!changed) break;
	    }
	    int which=sc1<=sc2 ? 1 : 2;
	    double cx=mol.atomX(which==1 ? bf : bt),cy=mol.atomY(which==1 ? bf : bt);
	    double axis=Math.atan2(cy-mol.atomY(which==1 ? bt : bf),cx-mol.atomX(which==1 ? bt : bf));
	    for (int i=0;i<mol.numAtoms();i++) if (side[i]==which)
	    {
	    	double dx=mol.atomX(i+1)-cx,dy=mol.atomY(i+1)-cy;
	    	double r=Math.sqrt(dx*dx+dy*dy),th=Math.atan2(dy,dx);
		th=2*axis-th;
		mol.setAtomPos(i+1,cx+r*Math.cos(th),cy+r*Math.sin(th));
	    }
	    for (int i=1;i<=mol.numBonds();i++) 
	    	if (mol.bondType(i)==Molecule.BONDTYPE_INCLINED || mol.bondType(i)==Molecule.BONDTYPE_DECLINED)
	    	    if (side[mol.bondFrom(i)-1]==which && side[mol.bondTo(i)-1]==which)
	    	    	mol.setBondType(i,mol.bondType(i)==Molecule.BONDTYPE_INCLINED ? Molecule.BONDTYPE_DECLINED 
			    	    	    	    	    	    	    	      : Molecule.BONDTYPE_INCLINED);
	}

   	clearTemporary(false);
    	determineSize();
    	repaint();
    }
    
    // selected chiral centres lose their wedge bonds
    public void removeChiralWedges()
    {
    	cacheUndo();
    
    	ArrayList<Integer> selidx=selectedIndices();
	for (int n=0;n<selidx.size();n++) if (mol.atomChirality(selidx.get(n))!=Molecule.STEREO_NONE)
	{
	    for (int i=1;i<=mol.numBonds();i++) 
		if ((mol.bondFrom(i)==selidx.get(n) || mol.bondTo(i)==selidx.get(n)) && 
		    (mol.bondType(i)==Molecule.BONDTYPE_INCLINED || mol.bondType(i)==Molecule.BONDTYPE_DECLINED))
		mol.setBondType(i,Molecule.BONDTYPE_NORMAL);
	}
	repaint();
    }

    // for any chiral centres, pick the next set of valid wedge bonds
    public void cycleChiralWedges()
    {
    	cacheUndo();
    
    	ArrayList<Integer> selidx=selectedIndices();
	for (int n=0;n<selidx.size();n++)
	{
	    int a=selidx.get(n),chi=mol.atomChirality(a);
	    if (chi!=Molecule.STEREO_POS && chi!=Molecule.STEREO_NEG) continue;
	    ArrayList<int[]> perm=wedgeFormations(a,chi);
    	    if (perm.size()<=1) continue; // invalid or no point
	    
	    int[] adj=mol.atomAdjList(a),curperm=new int[adj.length];
	    for (int i=0;i<adj.length;i++)
	    {
	    	int j=mol.findBond(a,adj[i]);
		curperm[i]=mol.bondType(j)==Molecule.BONDTYPE_INCLINED ? 1 : mol.bondType(j)==Molecule.BONDTYPE_DECLINED ? -1 : 0;
	    }
	    int match=-1;
	    for (int i=0;i<perm.size();i++)
	    {
	    	int[] thisperm=perm.get(i);
		boolean same=true;
		for (int j=0;j<curperm.length;j++) if (thisperm[j]!=curperm[j]) {same=false; break;}
		if (same) {match=i; break;}
	    }
	    match=(match+1)%perm.size();
	    curperm=perm.get(match);

	    for (int i=0;i<adj.length;i++)
	    {
	    	int j=mol.findBond(a,adj[i]);
		if (mol.bondFrom(j)!=a) mol.setBondFromTo(j,a,adj[i]);
		mol.setBondType(j,curperm[i]<0 ? Molecule.BONDTYPE_DECLINED 
		    	        : curperm[i]>0 ? Molecule.BONDTYPE_INCLINED : Molecule.BONDTYPE_NORMAL);
	    }
	}
	repaint();
    }
    
    // ------------------ private functions --------------------

    // translation of screen & molecule coordinates    
    double angToX(double AX) {return (offsetX+AX)*scale;}
    double angToY(double AY) {return (offsetY-AY)*scale;}
    double xToAng(double PX) {return (PX/scale)-offsetX;}
    double yToAng(double PY) {return (-PY/scale)+offsetY;}

    // resizes the widget, which is assumed scrollable, to fit the current magnification of the whole molecule
    void determineSize()
    {
    	int w,h;
    	if (!isSelectionPane)
	{
	    w=Math.max((int)angToX(mol.maxX()+1),500);
	    h=Math.max((int)angToY(mol.minY()-1),500);
	}
	else
	{
    	    w=selBoxW;
	    h=selBoxH;
	}
    	setPreferredSize(new Dimension(w,h));
	setSize(w,h);
    }
    
    // erases some of the datastructures used for caching the drawing elements
    void clearTemporary() {clearTemporary(true);}
    void clearTemporary(boolean AndSelected)
    {
    	px=py=rw=rh=null;
	highlightAtom=highlightBond=0;
	if (AndSelected) selected=null; 
	else if (selected!=null && selected.length!=mol.numAtoms())
	{
	    boolean newSelected[]=new boolean[mol.numAtoms()];
	    for (int n=0;n<selected.length;n++) newSelected[n]=selected[n];
	    selected=newSelected;
	}
    }
    
    void resetSelected(boolean Clear)
    {
    	if (selected==null) selected=new boolean[mol.numAtoms()];
	if (Clear) for (int n=0;n<mol.numAtoms();n++) selected[n]=false;
    }
    
    // return the atom underneath the given position, in screen coordinates; assumes that the appropriate arrays of size and position
    // have been filled out
    private int pickAtom(int X,int Y)
    {
    	if (px==null || py==null) return 0; //DefinePositions()...?;
	
    	for (int n=1;n<=mol.numAtoms();n++) 
	{
	    double dx=X-px[n-1],dy=Y-py[n-1];
	    if (Math.abs(dx)<=rw[n-1] && Math.abs(dy)<=rh[n-1])
	    	if (dx*dx/(rw[n-1]*rw[n-1])+dy*dy/(rh[n-1]*rh[n-1])<=1) {return n;}
	}
	return 0;
    }
    
    // returns the bond underneath the screen position
    private int pickBond(int X,int Y)
    {
    	if (px==null || py==null) return 0;
    
    	for (int n=1;n<=mol.numBonds();n++)
	{
	    double x1=px[mol.bondFrom(n)-1],y1=py[mol.bondFrom(n)-1],x2=px[mol.bondTo(n)-1],y2=py[mol.bondTo(n)-1];

	    double nx1=x1,ny1=y1,nx2=x2,ny2=y2;
	    int delta=Math.max(2,(int)(scale/20));
	    if (nx1>nx2) {nx1=x2; nx2=x1;}
	    if (ny1>ny2) {ny1=y2; ny2=y1;}
	    if (X<nx1-2*delta || X>nx2+2*delta || Y<ny1-2*delta || Y>ny2+2*delta) continue;

	    double dx=x2-x1,dy=y2-y1,d;
	    if (Math.abs(dx)>Math.abs(dy)) d=Y-y1-(X-x1)*dy/dx; else d=X-x1-(Y-y1)*dx/dy;
	    if (Math.abs(d)>(2+mol.bondOrder(n))*delta) continue;
	    return n;
	}
	return 0;
    }
    
    // snaps the draw-to-position to multiples of 30 degrees
    private void snapToolBond()
    {
    	double cx=toolBondFrom>0 ? mol.atomX(toolBondFrom) : toolBondFromX;
	double cy=toolBondFrom>0 ? mol.atomY(toolBondFrom) : toolBondFromY;
	double dx=toolBondToX-cx,dy=toolBondToY-cy;
    	double th=Math.atan2(dy,dx)*180/Math.PI,ext=Math.sqrt(dx*dx+dy*dy);
	th=(Math.round(th/30)*30)*Math.PI/180;
	ext=Math.round(ext/IDEALBOND)*IDEALBOND;
	toolBondToX=cx+ext*Math.cos(th);
	toolBondToY=cy+ext*Math.sin(th);
    }
        
    // should be called before any unit operation is conducted; the current molecule state is stored in the undo buffer
    public void cacheUndo()
    {
    	// !! check to see if the last molecule in the cache is literally identical, and if so, do nothing
    	if (undo==null) undo=new EditState[UNDO_LEVELS];
	redo=null;
	for (int n=UNDO_LEVELS-1;n>0;n--) undo[n]=undo[n-1];
	undo[0]=new EditState();
	undo[0].Molecule=mol==null ? null : mol.clone();
	undo[0].Selected=selected==null ? null : selected.clone();
    }
    
    // called when the element editing widget has ended its lifecycle, and the change is to be applied
    private void completeAtomEdit()
    {
    	if (toolAtomEditBox==null) return;
	String el=toolAtomEditBox.getText();
	if (el.length()>0)
	{
	    cacheUndo();

	    if (el.charAt(0)>='a' && el.charAt(0)<='z') el=el.substring(0,1).toUpperCase()+el.substring(1);

	    if (toolAtomEditSel==0)
	    {
		mol.addAtom(el,xToAng(toolAtomEditX),yToAng(toolAtomEditY));
		clearTemporary();
		determineSize();
	    }
	    else mol.setAtomElement(toolAtomEditSel,el);
	}
	
	toolAtomEditBox.setVisible(false);
    	remove(toolAtomEditBox);
	toolAtomEditBox=null;
	
	repaint();
	checkDirtiness();
    }
    
    // the currently active template is rotated according to a mapping between atoms
    private void adjustTemplateByAtom(int Atom)
    {
    	templDraw=template.clone();
	
	ArrayList<Integer> bonded=new ArrayList<Integer>();
	for (int n=1;n<=mol.numBonds();n++)
	{
	    if (mol.bondFrom(n)==Atom) bonded.add(new Integer(mol.bondTo(n)));
	    if (mol.bondTo(n)==Atom) bonded.add(new Integer(mol.bondFrom(n)));
	}
	
	final int INCR=1;
	double[] rotScores=new double[360/INCR];
	for (int n=1;n<=templDraw.numAtoms();n++) if (n!=templateIdx)
	{
	    double x=template.atomX(n)-template.atomX(templateIdx),y=template.atomY(n)-template.atomY(templateIdx);
	    double th=Math.atan2(y,x),ext=Math.sqrt(x*x+y*y);
	    for (int i=0;i<(360/INCR);i++)
	    {
	    	double rx=mol.atomX(Atom)+ext*Math.cos(th+i*INCR*Math.PI/180),ry=mol.atomY(Atom)+ext*Math.sin(th+i*INCR*Math.PI/180);
		for (int j=0;j<bonded.size();j++)
		{
		    int k=bonded.get(j).intValue();
		    double dx=mol.atomX(k)-rx,dy=mol.atomY(k)-ry;
		    double ext2=dx*dx+dy*dy;
		    if (ext2<0.01) ext2=0.01;
		    rotScores[i]+=1/ext2;
		}
	    }
	}
	
	int bestRot=0;
	for (int n=1;n<(360/INCR);n++) if (rotScores[n]<rotScores[bestRot]) bestRot=n;
	
	for (int n=1;n<=templDraw.numAtoms();n++)
	{
	    double x=template.atomX(n)-template.atomX(templateIdx),y=template.atomY(n)-template.atomY(templateIdx);
	    double th=Math.atan2(y,x),ext=Math.sqrt(x*x+y*y);
	    templDraw.setAtomPos(n,mol.atomX(Atom)+ext*Math.cos(th+bestRot*INCR*Math.PI/180),
	    	    	    	   mol.atomY(Atom)+ext*Math.sin(th+bestRot*INCR*Math.PI/180));
	}
    }
    
    // the currently active template is rotated according to a mapping between bonds
    private boolean adjustTemplateByBond(int Bond)
    {
    	Molecule[] rotMol=new Molecule[2];
	double[] rotScores=new double[2];
	
	for (int r=0;r<2;r++)
	{
	    rotMol[r]=template.clone();
	    int imol1=r==0 ? mol.bondFrom(Bond) : mol.bondTo(Bond),imol2=r==0 ? mol.bondTo(Bond) : mol.bondFrom(Bond);
	    int irot1=template.bondFrom(-templateIdx),irot2=template.bondTo(-templateIdx);
	    double dtheta=Math.atan2(mol.atomY(imol2)-mol.atomY(imol1),mol.atomX(imol2)-mol.atomX(imol1))
	    	    	 -Math.atan2(template.atomY(irot2)-template.atomY(irot1),template.atomX(irot2)-template.atomX(irot1));
	    
	    for (int n=1;n<=template.numAtoms();n++)
	    {
	    	double rx=template.atomX(n)-template.atomX(irot1),ry=template.atomY(n)-template.atomY(irot1);
		double th=Math.atan2(ry,rx),ext=Math.sqrt(rx*rx+ry*ry);
		rx=mol.atomX(imol1)+ext*Math.cos(th+dtheta);
		ry=mol.atomY(imol1)+ext*Math.sin(th+dtheta);
		rotMol[r].setAtomPos(n,rx,ry);
		
		for (int i=1;i<=mol.numAtoms();i++)
		{
		    double dx=mol.atomX(i)-rx,dy=mol.atomY(i)-ry;
		    double ext2=dx*dx+dy*dy;
		    if (ext2<0.01) ext2=0.01;
		    rotScores[r]+=1/ext2;
		}
	    }
	}
	
	boolean swap=rotScores[0]<rotScores[1];
	templDraw=rotMol[swap ? 0 : 1];
	return swap;
    }
    
    // the currently active template is merely translated, as there is no current atom or bond mapping
    private void adjustTemplateByCoord(double X,double Y)
    {
    	templDraw=template.clone();

    	double dx=0,dy=0;
	if (templateIdx>0) {dx=template.atomX(templateIdx); dy=template.atomY(templateIdx);}
	else if (templateIdx<0)
	{
	    int from=template.bondFrom(-templateIdx),to=template.bondTo(-templateIdx);
	    dx=0.5*(template.atomX(from)+template.atomX(to));
	    dy=0.5*(template.atomY(from)+template.atomY(to));
	}
    	for (int n=1;n<=template.numAtoms();n++) templDraw.setAtomPos(n,template.atomX(n)-dx+X,template.atomY(n)-dy+Y);
    }
    
    // places a template, where atoms are mapped
    private void templateSetByAtom(int JoinAtom)
    {
    	int[] map=new int[templDraw.numAtoms()];
	int oldNum=mol.numAtoms();
    	for (int n=1;n<=templDraw.numAtoms();n++) if (JoinAtom==0 || n!=templateIdx)
	{
	    mol.addAtom(templDraw.atomElement(n),templDraw.atomX(n),templDraw.atomY(n),
		    	templDraw.atomCharge(n),templDraw.atomUnpaired(n));
	}
	for (int n=1;n<=templDraw.numBonds();n++)
	{
	    int from=templDraw.bondFrom(n);
	    int to=templDraw.bondTo(n);
	    if (JoinAtom>0)
	    {
		if (from==templateIdx) from=JoinAtom; 
		else 
		{
		    if (from>templateIdx) from--;
		    from+=oldNum;
		}
		if (to==templateIdx) to=JoinAtom; 
		else 
		{
		    if (to>templateIdx) to--;
		    to+=oldNum;
		}
	    }
	    else {from+=oldNum; to+=oldNum;}
	    mol.addBond(from,to,templDraw.bondOrder(n),templDraw.bondType(n));
	}

    	mergeNewAtoms(oldNum);

	clearTemporary();
	determineSize();
	repaint();
    }
    
    // places a template, where bonds are mapped
    private void templateSetByBond(int JoinBond,boolean Swap)
    {
    	int[] map=new int[templDraw.numAtoms()];
	int oldNum=mol.numAtoms();
	int joinFrom=JoinBond>0 ? mol.bondFrom(JoinBond) : 0,joinTo=JoinBond>0 ? mol.bondTo(JoinBond) : 0;
	int newFrom=Swap ? templDraw.bondFrom(-templateIdx) : templDraw.bondTo(-templateIdx);
	int newTo=Swap ? templDraw.bondTo(-templateIdx) : templDraw.bondFrom(-templateIdx);	
	for (int n=1;n<=templDraw.numAtoms();n++)
	{
	    if (n==newFrom && JoinBond>0) map[n-1]=joinFrom;
	    else if (n==newTo && JoinBond>0) map[n-1]=joinTo;
	    else 
	    {
	    	map[n-1]=mol.addAtom(templDraw.atomElement(n),templDraw.atomX(n),templDraw.atomY(n),
		    	    	     templDraw.atomCharge(n),templDraw.atomUnpaired(n));
	    }
	}
	for (int n=1;n<=template.numBonds();n++) if (n!=-templateIdx || JoinBond==0)
	{
	    mol.addBond(map[templDraw.bondFrom(n)-1],map[templDraw.bondTo(n)-1],templDraw.bondOrder(n),templDraw.bondType(n));
	}
	
	mergeNewAtoms(oldNum);
	
	clearTemporary();
	determineSize();
	repaint();
    }
    
    // any atoms of index greater than the watermark are merged with previously defined atoms if they are close
    private void mergeNewAtoms(int Watermark)
    {
    	int pos=Watermark+1;
	while (pos<=mol.numAtoms())
	{
	    int close=0;
	    for (int n=1;n<=Watermark;n++) 
	    {
	    	double dx=mol.atomX(n)-mol.atomX(pos),dy=mol.atomY(n)-mol.atomY(pos);
		if (dx*dx+dy*dy<0.2*0.2) {close=n; break;}
	    }
    	    if (close>0)
	    {
	    	int[] adj=mol.atomAdjList(pos);
	    	for (int i=0;i<adj.length;i++) if (mol.findBond(close,adj[i])==0)
		{
		    int j=mol.findBond(pos,adj[i]);
		    mol.addBond(close,adj[i],mol.bondOrder(j));
		}
		mol.deleteAtomAndBonds(pos);
	    }
	    else pos++;
	}
    }

    // returns true if there are any selected atoms
    private boolean anySelected()
    {
    	if (selected==null) return false;
	for (int n=0;n<mol.numAtoms();n++) if (selected[n]) return true;
	return false;
    }

    private double dragExtendBy(double px,double py)
    {
	double diff=0.2*Math.sqrt(px*px+py*py)/scale;
	if (px<0 && py<0) diff=-diff;
	
	if (diff>=0) return 1+diff;
	else return Math.exp(diff);
    }
    
    // calculate all the wedge bond formations for a given atom for a given chirality (+/-), ranked in order, null if none
    private ArrayList<int[]> wedgeFormations(int N,int Chi)
    {
    	if (mol.atomAdjCount(N)!=3 && mol.atomAdjCount(N)!=4) return null;
    	int[] adj=mol.atomAdjList(N);
	for (int i=0;i<adj.length-1;i++) for (int j=i+1;j<adj.length;j++) 
	    if (mol.atomPriority(adj[i])==mol.atomPriority(adj[j])) return null;

	int[] badj=new int[adj.length];
	for (int n=0;n<adj.length;n++) badj[n]=mol.findBond(N,adj[n]);
	
	ArrayList<int[]> perm=new ArrayList<int[]>();

    	// generate all possible sensible wedge combinations
    	if (adj.length==3)
	{
	    for (int i=0;i<3;i++) for (int iz=-1;iz<=1;iz+=2)
	    {
	    	int[] wedges=new int[3];
		for (int n=0;n<3;n++) wedges[n]=0;
		wedges[i]=iz;
		perm.add(wedges);
	    }
	}
	else
	{
	    for (int i=0;i<4;i++) for (int iz=-1;iz<=1;iz+=2)
	    {
	    	int[] wedges=new int[4];
		for (int n=0;n<4;n++) wedges[n]=0;
		wedges[i]=iz;
		perm.add(wedges);
		
		for (int j=i+1;j<4;j++) for (int jz=-1;jz<=1;jz+=2)
		{
	    	    if (/*i==j || */jz==iz) continue;
	    	    wedges=new int[4];
		    for (int n=0;n<4;n++) wedges[n]=0;
		    wedges[i]=iz;
		    wedges[j]=jz;
		    perm.add(wedges);
		}
	    }
	}

	// keep only the ones which indicate the desired enantiomer
	int pos=0;
	while (pos<perm.size())
	{
	    int[] wedges=perm.get(pos);
	    Molecule mchi=mol.clone();
	    for (int n=0;n<adj.length;n++) 
	    {
	    	mchi.setBondType(badj[n],wedges[n]<0 ? Molecule.BONDTYPE_DECLINED 
		    	    	       : wedges[n]>0 ? Molecule.BONDTYPE_INCLINED : Molecule.BONDTYPE_NORMAL);
    	    	if (mchi.bondFrom(badj[n])!=N) mol.setBondFromTo(badj[n],mol.bondTo(badj[n]),mol.bondFrom(badj[n]));

	    }
	    if (mchi.atomChirality(N)!=Chi) perm.remove(pos); else pos++;
	}
	
	// score each one based on crude aesthetic criteria
	double[] score=new double[perm.size()];
	for (int n=0;n<perm.size();n++)
	{
	    score[n]=0;
	    int[] wedges=perm.get(n);
	    int wcount=0;
	    for (int i=0;i<adj.length;i++) if (wedges[i]!=0)
	    {
	    	wcount++;
		score[n]-=0.5*mol.atomPriority(adj[i])/mol.numAtoms();
	    	if (mol.atomAdjCount(adj[i])==1) score[n]++;
		if (mol.atomRingBlock(adj[i])>0) 
		{
		    score[n]--;
		    if (mol.atomRingBlock(N)==mol.atomRingBlock(adj[i])) score[n]--;
		}
	    }
	    if (adj.length==4 && wcount==2) score[n]++;
	}
	
	// sort best-first
	pos=0;
	while (pos<perm.size()-1)
	{
	    if (score[pos]<score[pos+1])
	    {
	    	int[] w1=perm.get(pos),w2=perm.get(pos+1);
		perm.set(pos+1,w1);
		perm.set(pos,w2);
		double s=score[pos]; score[pos]=score[pos+1]; score[pos+1]=s;
		if (pos>0) pos--;
	    } else pos++;
	}
	
	
	/*System.out.println("PERMSIZE:"+perm.size());
	for (int n=0;n<perm.size();n++)
	{
	    int[] w=perm.get(n);
	    System.out.print("n="+n);
    	    for (int i=0;i<w.length;i++) System.out.print(" "+w[i]);
	    System.out.println(" score="+score[n]);
	}*/

	return perm;
    }
    
    // ------------------ event functions --------------------
    
    // the paint function sets up the basic graphics drawing context, then performs some preliminary transformations of any tools effects
    // that are in progress, hands off all the work to DrawMolecule, then fetches back some of data determined in situ
    
    protected void paintComponent(Graphics gr) 
    {
    	if (autoScale) scaleToFit();
    
    	gr.setColor(Color.WHITE);
	gr.fillRect(0,0,getWidth(),getHeight());
    	if (hasBorder)
	{
	    gr.setColor(Color.BLACK);
	    gr.drawRect(0,0,getWidth()-1,getHeight()-1);
	}

    	Graphics2D g=(Graphics2D)gr;
    	g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
    	DrawMolecule draw=new DrawMolecule(mol,g,scale);
	
	// provide a miscellany of editing data to the drawing class, so that it can represent everything that is going on
	
	draw.setBackground(getBackground());
	draw.setShowHydr(showHydr);
	draw.setShowMode(showMode);
	draw.setShowStereo(showSter);
	draw.setOffset(offsetX,offsetY);
	draw.setHighlight(highlightAtom,highlightBond);
	
    	resetSelected(false);
	draw.setSelected(selected,dragged);

	if ((tool==TOOL_ATOM && toolAtomDrag) || (tool==TOOL_BOND && toolBondFrom>0))
	    draw.bondInProgress(toolBondFrom,toolBondToX,toolBondToY,toolBondOrder,toolBondType);
    	if (tool==TOOL_ATOM && toolAtomDrag && toolAtomType!=null && toolAtomType.compareTo("C")!=0) 
	    draw.atomInProgress(toolAtomType,toolBondToX,toolBondToY);
	if (tool==TOOL_BOND && toolBondFrom==0 && toolBondDrag)
	{
	    int i=pickAtom((int)angToX(toolBondToX),(int)angToY(toolBondToY));
	    if (i==0 && toolSnap) snapToolBond();
	    double x1=toolBondFromX,y1=toolBondFromY,x2=toolBondToX,y2=toolBondToY;
	    if (i>0) {x2=mol.atomX(i); y2=mol.atomY(i);} 
	    draw.newBondLine(x1,y1,x2,y2);
	}
	if (toolDragReason==DRAG_SELECT)
	{
	    draw.dragSelect((int)toolDragX1,(int)toolDragY1,(int)toolDragX2,(int)toolDragY2);
	}
    	if ((toolDragReason==DRAG_MOVE || toolDragReason==DRAG_COPY || toolDragReason==DRAG_SCALE) 
    	    && (toolDragX1!=toolDragX2 || toolDragY1!=toolDragY2))
	{
	    if (toolDragReason==DRAG_SCALE)
	    {
	    	double extmul=dragExtendBy(toolDragX2-toolDragX1,toolDragY2-toolDragY1),cx=0,cy=0;
		int count=0;
    	    	for (int n=1;n<=mol.numAtoms();n++) if (selected[n-1]) {cx+=mol.atomX(n); cy+=mol.atomY(n); count++;}
		cx/=count; cy/=count;
		draw.dragScale(cx,cy,extmul);
	    }
	    else
	    {
	    	int dx=(int)(toolDragX2-toolDragX1),dy=(int)(toolDragY2-toolDragY1);
	    	draw.dragMove(dx,dy,toolDragReason==DRAG_COPY);
	    }
	}
	if (toolDragReason==DRAG_ROTATE && (Math.abs(toolDragX2-toolDragX1)>5 || Math.abs(toolDragY2-toolDragY1)>5))
	{
	    double dx=toolDragX2-toolDragX1,dy=toolDragY2-toolDragY1;
	    double th=-Math.atan2(dy,dx)*180/Math.PI;
	    if (toolSnap) th=Math.round(th/15)*15;
	    
	    draw.dragRotate(th,(int)toolDragX1,(int)toolDragY1);
	}
	if (tool==TOOL_TEMPLATE && trackX>=0 && trackY>=0)
	{
	    if (highlightAtom!=0 && templateIdx>0) adjustTemplateByAtom(highlightAtom);
	    else if (highlightBond!=0 && templateIdx<0) adjustTemplateByBond(highlightBond);
	    else adjustTemplateByCoord(xToAng(trackX),yToAng(trackY));

	    draw.outlineTemplate(templDraw);
	}

	draw.draw();

    	// fetch some of the calculated properties, which are used for the editing progress
	ArrangeMolecule arrmol=draw.arrangement();
	px=new double[mol.numAtoms()];
	py=new double[mol.numAtoms()];
	rw=new double[mol.numAtoms()];
	rh=new double[mol.numAtoms()];
	for (int n=0;n<mol.numAtoms();n++)
	{
	    px[n]=arrmol.pointCX(n);
	    py[n]=arrmol.pointCY(n);
	    rw[n]=Math.max(arrmol.pointRW(n),5);
	    rh[n]=Math.max(arrmol.pointRH(n),5);
	}
    }
    
    // Mouse events: the callbacks for Clicked, Pressed, Released, Dragged and Moved form a slightly complicated interplay of the various
    // tool events. The 'tool' variable, and its various permitted values, should make most of the behaviour reasonably self-explanatory;
    // note that many of the tools have multiple functions which may be sprinkled around the various event callbacks.
    
    public void mouseClicked(MouseEvent e)
    {
    //jm.evers 17/1/2010
	if( e.getButton() == MouseEvent.BUTTON1 && MainApplet.USER_SELECTION  && tool == TOOL_SELECT  ){
	//	System.out.println("highlightBond = "+highlightBond);
    	     if( highlightAtom > 0){
		if(highlightAtom > atomselection.length - 1){
    		    atomselection = GrowArray(atomselection , highlightAtom+2 );
		}
		/* flip flop...select unselect */
		if(atomselection[highlightAtom]){
    		    atomselection[highlightAtom] = false; // de_select
		}else{
    		    atomselection[highlightAtom] = true; // select
		}
    	     }
	     if(highlightBond > 0){
		if(highlightBond > bondselection.length - 1){
    		    bondselection = GrowArray(bondselection , highlightBond+2 );
		}
		/* flip flop...select unselect */
		if(bondselection[highlightBond]){
    		    bondselection[highlightBond] = false; // de_select
		}else{
    		    bondselection[highlightBond] = true; // select
		}
    	     }
    	    repaint();
	}
	else
    	if (tool==TOOL_CURSOR && selectListen!=null)
	{
	    int i=pickAtom(e.getX(),e.getY());
	    if ((e.getModifiers()&MouseEvent.CTRL_MASK)>0 && i>0 && editable) // select connected component
	    {
	    	if ((e.getModifiers()&MouseEvent.SHIFT_MASK)==0 && selected!=null) for (int n=0;n<mol.numAtoms();n++) selected[n]=false;
	    	if (selected==null) selected=new boolean[mol.numAtoms()];
		int cc=mol.atomConnComp(i);
		for (int n=1;n<=mol.numAtoms();n++) if (mol.atomConnComp(n)==cc) selected[n-1]=true;
		repaint();
	    }
	    else if (i>0) selectListen.molSelected(this,i,e.getClickCount()>1); // notify of atom selection
	    else // notify of bond (or nothing) selection
	    {
	    	i=pickBond(e.getX(),e.getY());
		/*if (i>0)*/ selectListen.molSelected(this,-i,e.getClickCount()>1); 
		// (0==clicked in general area)
	    }
	}
	else if (tool==TOOL_ROTATOR) // deselect
	{
	    selected=null;
	    clearTemporary();
	    repaint();
	}
    	else if (tool==TOOL_ERASOR) // delete something, be it atom or bond
	{
	    if(MainApplet.USER_SELECTION){ deSelectAll(); } /* remove markings otherwiser trouble */
	    int i=pickAtom(e.getX(),e.getY());
	    if (i>0) 
	    {
	    	cacheUndo();
	    	mol.deleteAtomAndBonds(i);
	    }
	    else
	    {
	    	cacheUndo();
	    	i=pickBond(e.getX(),e.getY());
		if (i>0) mol.deleteBond(i);
	    }
	    if (i>0)
	    {
	    	clearTemporary();
		determineSize();
		repaint();
	    }
	}
    	else if (tool==TOOL_ATOM && e.getButton()==MouseEvent.BUTTON1 && !toolAtomDrag)
	{
	    if (toolAtomEditBox!=null) 
	    {
		completeAtomEdit();
		return;
    	    }
	
	    if (toolAtomType!=null)  // add new atom, or change element label
	    {
	    	int i=pickAtom(e.getX(),e.getY());
    	    	cacheUndo();
		if (i==0) 
		{
		    i=mol.addAtom(toolAtomType,xToAng(e.getX()),yToAng(e.getY()));
    	    	    offsetX=e.getX()/scale-mol.atomX(i);
	    	    offsetY=e.getY()/scale+mol.atomY(i);
		}
		else mol.setAtomElement(i,toolAtomType);
		clearTemporary();
    	    	determineSize();
		repaint();
	    } 
	    else // setup new editing box for element
	    {
	    	toolAtomEditX=e.getX(); toolAtomEditY=e.getY();
	    	toolAtomEditSel=pickAtom(toolAtomEditX,toolAtomEditY);
	    	if (toolAtomEditSel==0 && pickBond(e.getX(),e.getY())>0) return;
		
		toolAtomEditBox=new JTextField(toolAtomEditSel>0 ? mol.atomElement(toolAtomEditSel) : "");
		add(toolAtomEditBox);
		toolAtomEditBox.addFocusListener(this);
		toolAtomEditBox.addKeyListener(this);
		toolAtomEditBox.setLocation(toolAtomEditX-10,toolAtomEditY-10);
		toolAtomEditBox.setSize(20,20);
		toolAtomEditBox.setVisible(true);
		toolAtomEditBox.setSelectionStart(0);
		toolAtomEditBox.setSelectionEnd(toolAtomEditBox.getText().length());
		toolAtomEditBox.grabFocus();
	    }
	}
	else if (tool==TOOL_TEMPLATE && e.getButton()==MouseEvent.BUTTON2) // flip the template, horizontal or vertical
	{
	    boolean vertical=e.isShiftDown();
	    for (int n=1;n<=template.numAtoms();n++) 
	    	template.setAtomPos(n,template.atomX(n)*(vertical?1:-1),template.atomY(n)*(vertical?-1:1));
    	    templDraw=template.clone();
    	    repaint();
	}	

	checkDirtiness();
    }
    
    public void mouseEntered(MouseEvent e) 
    {
    	boolean redraw=false;
	if (tool==TOOL_TEMPLATE && (trackX!=e.getX() || trackY!=e.getY())) redraw=true;
    	trackX=e.getX(); trackY=e.getY();
	if (redraw) repaint();
    }
    
    public void mouseExited(MouseEvent e) 
    {
    	boolean redraw=false;
	if (tool==TOOL_TEMPLATE && (trackX!=e.getX() || trackY!=e.getY())) redraw=true;
    	trackX=-1; trackY=-1;
	if (redraw) repaint();
    }
    
    public void mousePressed(MouseEvent e)
    {
	grabFocus();

    	if ((tool==TOOL_CURSOR || (tool==TOOL_ROTATOR && !anySelected())) && e.getButton()==MouseEvent.BUTTON1 && editable)
	{ // consider initiating a drag of the select, or translate position variety
    	    highlightAtom=highlightBond=0;
	    boolean shift=(e.getModifiers()&MouseEvent.SHIFT_MASK)>0;
	    boolean ctrl=(e.getModifiers()&MouseEvent.CTRL_MASK)>0;
	    boolean alt=(e.getModifiers()&MouseEvent.ALT_MASK)>0;
	    boolean anySelected=countSelected()>0;
	    if (tool==TOOL_ROTATOR) {shift=false; ctrl=false; alt=false;} // can only select with rotator
	    if (!ctrl && !alt) 
	    {
	    	resetSelected(!shift);
	    	int atom=pickAtom(e.getX(),e.getY());
		if (atom>0) selected[atom-1]=true;
		else toolDragReason=DRAG_SELECT;
	    }
	    else if (!shift && ctrl && !alt && anySelected) toolDragReason=DRAG_COPY;
	    else if (!shift && !ctrl && alt && anySelected) toolDragReason=DRAG_MOVE;
	    else if (shift && !ctrl && alt && anySelected) toolDragReason=DRAG_SCALE;

    	    toolDragX1=toolDragX2=e.getX();
	    toolDragY1=toolDragY2=e.getY();
	    repaint();
	}
	else if (tool==TOOL_ERASOR && e.getButton()==MouseEvent.BUTTON1) // initiate a drag-rect-delete sequence
	{
	    if(MainApplet.USER_SELECTION){ deSelectAll(); } /* first remove marked stuff */
	    highlightAtom=highlightBond=0;
	    resetSelected(true);
	    toolDragReason=DRAG_SELECT;
    	    toolDragX1=toolDragX2=e.getX();
	    toolDragY1=toolDragY2=e.getY();
	    repaint();
	}
	else if (tool==TOOL_ATOM) // note drag or change atom
	{
	    toolBondFrom=pickAtom(e.getX(),e.getY()); 	    	// in case it gets...
    	    toolAtomSnap=e.getButton()==MouseEvent.BUTTON1; 	// ... dragged later
	}
    	else if (tool==TOOL_BOND && (e.getButton()==MouseEvent.BUTTON1 || e.getButton()==MouseEvent.BUTTON3)) // initiate bond drag
	{
    	    highlightAtom=highlightBond=0;
	    toolBondDrag=false;
	    toolBondFrom=pickAtom(e.getX(),e.getY());
	    toolSnap=e.getButton()==MouseEvent.BUTTON1;
	    if (toolBondFrom>0)
	    {
	    	toolBondToX=mol.atomX(toolBondFrom);
		toolBondToY=mol.atomY(toolBondFrom);
		repaint();
	    }
	    toolBondFromX=xToAng(e.getX()); 
	    toolBondFromY=yToAng(e.getY());
	    toolBondHit=pickBond(e.getX(),e.getY());
	}
	else if (tool==TOOL_TEMPLATE && e.getButton()==MouseEvent.BUTTON1) // slap a template right down
	{
	    boolean swap=false;
	    if (highlightAtom!=0 && templateIdx>0) adjustTemplateByAtom(highlightAtom);
    	    else if (highlightBond!=0 && templateIdx<0) swap=adjustTemplateByBond(highlightBond);
	    else adjustTemplateByCoord(xToAng(trackX),yToAng(trackY));
		
	    cacheUndo();
		
	    if (templateIdx>=0) 
	    	templateSetByAtom(highlightAtom); 
    	    	else 
		templateSetByBond(highlightBond,swap);
	}
	else if (tool==TOOL_ROTATOR && (e.getButton()==MouseEvent.BUTTON1 || e.getButton()==MouseEvent.BUTTON3) && anySelected())
	{ // initiate a rotation-drag
	    toolDragReason=DRAG_ROTATE;
	    toolSnap=e.getButton()==MouseEvent.BUTTON1;
	    if (highlightAtom>0) {toolDragX1=angToX(mol.atomX(highlightAtom)); toolDragY1=angToY(mol.atomY(highlightAtom));}
	    else if (highlightBond>0) 
	    {
	    	toolDragX1=angToX(0.5*(mol.atomX(mol.bondFrom(highlightBond))+mol.atomX(mol.bondTo(highlightBond))));
	    	toolDragY1=angToY(0.5*(mol.atomY(mol.bondFrom(highlightBond))+mol.atomY(mol.bondTo(highlightBond))));
	    }
	    else {toolDragX1=e.getX(); toolDragY1=e.getY();}
	    highlightAtom=highlightBond=0;
	    
	    toolDragX2=toolDragX1;
	    toolDragY2=toolDragY1;
	    repaint();
	}
	else if (tool==TOOL_CHARGE && highlightAtom>0) // offset charge
	{
	    int chg=mol.atomCharge(highlightAtom);
	    if (e.getButton()==MouseEvent.BUTTON1) chg+=toolCharge;
	    else if (e.getButton()==MouseEvent.BUTTON3) chg-=toolCharge;
	    else chg=0;
	    cacheUndo();
	    mol.setAtomCharge(highlightAtom,chg);
	    repaint();
	}
	
	checkDirtiness();
    }
    
    public void mouseReleased(MouseEvent e)
    {
    	if ((tool==TOOL_CURSOR && toolDragReason!=0) || (tool==TOOL_ROTATOR && toolDragReason==DRAG_SELECT) && editable)
	{ // solidify a translate or select
	    toolDragX2=e.getX();
	    toolDragY2=e.getY();
	    double mx=toolDragX2-toolDragX1,my=toolDragY2-toolDragY1;
	    
	    if (toolDragReason==DRAG_SELECT && dragged!=null)
	    {
	    	for (int n=0;n<mol.numAtoms();n++) selected[n]=selected[n] || dragged[n];
	    }
    	    if (toolDragReason==DRAG_MOVE && selected!=null && mx*mx+my*my>5*5)
	    {
	    	double dx=mx/scale,dy=-my/scale;
		cacheUndo();
		for (int n=1;n<=mol.numAtoms();n++) if (selected[n-1]) 
		{
		    mol.setAtomPos(n,mol.atomX(n)+dx,mol.atomY(n)+dy);
		}
		clearTemporary(false);
		determineSize();
	    }
            if (toolDragReason==DRAG_COPY && selected!=null && mx*mx+my*my>5*5)
	    {
	    	double dx=(toolDragX2-toolDragX1)/scale,dy=-(toolDragY2-toolDragY1)/scale;
		int oldnumAtoms=mol.numAtoms(),oldnumBonds=mol.numBonds();
		int[] newPos=new int[mol.numAtoms()];
		cacheUndo();
		for (int n=1;n<=oldnumAtoms;n++) if (selected[n-1]) 
		{
		    newPos[n-1]=mol.addAtom(mol.atomElement(n),mol.atomX(n)+dx,mol.atomY(n)+dy,mol.atomCharge(n),mol.atomUnpaired(n));
		}
		for (int n=1;n<=oldnumBonds;n++) if (selected[mol.bondFrom(n)-1] && selected[mol.bondTo(n)-1])
		{
		    mol.addBond(newPos[mol.bondFrom(n)-1],newPos[mol.bondTo(n)-1],mol.bondOrder(n),mol.bondType(n));
		}

		clearTemporary();
		selected=new boolean[mol.numAtoms()];
		for (int n=1;n<=mol.numAtoms();n++) selected[n-1]=n>oldnumAtoms;
		determineSize();
	    }
	    if (toolDragReason==DRAG_SCALE && selected!=null && mx*mx+my*my>5*5)
	    {
		double extmul=dragExtendBy(mx,my);
		double cx=0,cy=0;
		int count=0;
    	    	for (int n=1;n<=mol.numAtoms();n++) if (selected[n-1]) {cx+=mol.atomX(n); cy+=mol.atomY(n); count++;}
		cx/=count; cy/=count;
		cacheUndo();
		for (int n=1;n<=mol.numAtoms();n++) if (selected[n-1]) 
		{
    	    	    mol.setAtomPos(n,(mol.atomX(n)-cx)*extmul+cx,(mol.atomY(n)-cy)*extmul+cy);
		}
		
		clearTemporary(false);
		determineSize();
	    }

	    toolDragReason=0;
	    dragged=null;
    	    repaint();
	}
	if (tool==TOOL_ERASOR && toolDragReason!=0) // erase selection
	{
	    toolDragX2=e.getX();
	    toolDragY2=e.getY();
	    if (toolDragReason==DRAG_SELECT && dragged!=null)
	    {
	    	for (int n=0;n<mol.numAtoms();n++) selected[n]=selected[n] || dragged[n];
		deleteSelected();
		clearTemporary();
	    }
	    toolDragReason=0;
	    dragged=null;
    	    repaint();
	}
    	else if (tool==TOOL_ROTATOR && toolDragReason==DRAG_ROTATE) // solidify a rotation
	{
	    double dx=toolDragX2-toolDragX1,dy=toolDragY2-toolDragY1;
	    double th=-Math.atan2(dy,dx)*180/Math.PI;
	    if (toolSnap) th=Math.round(th/15)*15;
	    if (Math.abs(th)>1) 
	    {
	    	cacheUndo();
		th=th*Math.PI/180;
		double ax=xToAng(toolDragX1),ay=yToAng(toolDragY1);
		for (int n=1;n<=mol.numAtoms();n++) if (selected[n-1])
		{
	    	    double rx=mol.atomX(n)-ax,ry=mol.atomY(n)-ay;
		    double rth=Math.atan2(ry,rx),ext=Math.sqrt(rx*rx+ry*ry);
		    mol.setAtomPos(n,ax+ext*Math.cos(rth+th),ay+ext*Math.sin(rth+th));
		}
		clearTemporary(false);
		determineSize();
	    }

	    toolDragReason=0;
	    dragged=null;
    	    repaint();
	}
    	else if (tool==TOOL_ATOM && toolAtomDrag && toolBondFrom>0) // place a new atom-from
	{
	    cacheUndo();
	    mol.addAtom(toolAtomType,toolBondToX,toolBondToY);
	    mol.addBond(toolBondFrom,mol.numAtoms(),1);
	    clearTemporary();
    	    determineSize();
	    toolAtomDrag=false;
	    toolBondFrom=0;
    	    repaint();
	}
    	else if (tool==TOOL_BOND) // bond addition, possibly by adding new atoms, too
	{
	    toolBondToX=xToAng(e.getX());
	    toolBondToY=yToAng(e.getY());

    	    int joinTo=pickAtom(e.getX(),e.getY());
	    if (toolBondFrom>0 && joinTo==0 && toolSnap)
	    {
	    	snapToolBond();
		joinTo=pickAtom((int)angToX(toolBondToX),(int)angToY(toolBondToY));
	    }
	    
	    if (e.getButton()==MouseEvent.BUTTON1 && toolBondFrom==0 && toolBondHit>0) // change hit bond order
	    {
		int i=pickBond(e.getX(),e.getY());
		if (i==toolBondHit)
		{
	    	    cacheUndo();
	    	    if (toolBondOrder==mol.bondOrder(i) && toolBondType==mol.bondType(i)) 
			mol.setBondFromTo(i,mol.bondTo(i),mol.bondFrom(i));
	    	    mol.setBondOrder(i,toolBondOrder); 
		    mol.setBondType(i,toolBondType);
		    clearTemporary();
		}
	    }
	    else if (toolBondFrom==0) // create a new bond from/in the middle of nowhere, possibly connected to something
	    {
	    	int a1=0,a2=0;
		double x1=0,x2=0,y1=0,y2=0;
	    	if (toolBondDrag)
		{
		    if (toolSnap) snapToolBond();
		    x1=toolBondFromX;
		    y1=toolBondFromY;
		    a2=pickAtom(e.getX(),e.getY());
		    if (a2>0) {x2=mol.atomX(a2); y2=mol.atomY(a2);} else {x2=toolBondToX; y2=toolBondToY;}
		}
		else
		{
		    x1=x2=xToAng(e.getX()); if ((e.getModifiers()&MouseEvent.SHIFT_MASK)>0) {x1-=0.5*IDEALBOND; x2+=0.5*IDEALBOND;}
		    y1=y2=yToAng(e.getY()); if ((e.getModifiers()&MouseEvent.SHIFT_MASK)==0) {y1-=0.5*IDEALBOND; y2+=0.5*IDEALBOND;}
		}
		double dx=x2-x1,dy=y2-y1;
		if (dx*dx+dy*dy>0.5*0.5)
		{
		    cacheUndo();
	    	    a1=mol.addAtom("C",x1,y1,0,0);
		    if (a2==0) a2=mol.addAtom("C",x2,y2,0,0);
		    mol.addBond(a1,a2,toolBondOrder);
		    clearTemporary();
		}
		repaint();
	    }
	    else if (joinTo>0 && joinTo!=toolBondFrom) // link two atoms together
	    {
	    	cacheUndo();
	    	mol.addBond(toolBondFrom,joinTo,toolBondOrder);
		mol.setBondType(mol.numBonds(),toolBondType);
    	    	clearTemporary();
	    }
	    else if (toolBondFrom>0) // draw a new bond out to some place not specified by the user, i.e. a healthy guess
	    {
		double dx=toolBondToX-mol.atomX(toolBondFrom),dy=toolBondToY-mol.atomY(toolBondFrom);
		if (toolBondFrom==joinTo) 
		{
		    int adj[]=mol.atomAdjList(toolBondFrom);
		    ArrayList<Double> poss=new ArrayList<Double>();
		    double ax=mol.atomX(toolBondFrom),ay=mol.atomY(toolBondFrom);
		    if (adj.length==0) poss.add(0.0);
		    else if (adj.length==1)
		    {
		    	double ang=Math.atan2(mol.atomY(adj[0])-ay,mol.atomX(adj[0])-ax)*180/Math.PI;
			if (toolBondOrder!=3)
			{
		    	    poss.add(ang+120);
			    poss.add(ang-120);
			}
			else poss.add(ang+180);
		    }
		    else if (adj.length==2)
		    {
		    	double ang1=Math.atan2(mol.atomY(adj[0])-ay,mol.atomX(adj[0])-ax)*180/Math.PI;
		    	double ang2=Math.atan2(mol.atomY(adj[1])-ay,mol.atomX(adj[1])-ax)*180/Math.PI;
		    	if (ang2<ang1) ang2+=360;
			if (ang2-ang1<180) poss.add(0.5*(ang1+ang2)+180); else poss.add(0.5*(ang1+ang2));
		    }
		    else for (int n=0;n<adj.length;n++)
		    {
		    	double ang=Math.atan2(mol.atomY(adj[n])-ay,mol.atomX(adj[n])-ax)*180/Math.PI;
		    	poss.add(ang+180);
		    }
		    double ang=poss.get(0);
		    if (poss.size()>1)
		    {
			int best=-1;
			double bestScore=0;
			for (int n=0;n<poss.size();n++)
			{
			    double nx=ax+IDEALBOND*Math.cos(poss.get(n)*Math.PI/180);
			    double ny=ay+IDEALBOND*Math.sin(poss.get(n)*Math.PI/180);
			    double score=0;
			    for (int i=1;i<=mol.numAtoms();i++)
			    {
			    	dx=mol.atomX(i)-nx;
				dy=mol.atomY(i)-ny;
			    	score+=1/Math.min(1000,dx*dx+dy*dy);
			    }
			    if (best<0 || score<bestScore) {best=n; bestScore=score;}
			}
			ang=poss.get(best);
		    }
		    
		    dx=IDEALBOND*Math.cos(ang*Math.PI/180);
		    dy=IDEALBOND*Math.sin(ang*Math.PI/180);
		    toolBondToX=ax+dx;
		    toolBondToY=ay+dy;
		}
		if (dx*dx+dy*dy>0.5)
		{
		    cacheUndo();
	    	    mol.addAtom("C",toolBondToX,toolBondToY);
		    mol.addBond(toolBondFrom,mol.numAtoms(),toolBondOrder);
		    mol.setBondType(mol.numBonds(),toolBondType);
		    clearTemporary();
    	    	    determineSize();
    		}
	    }

	    toolBondDrag=false;
	    toolBondFrom=0;
	    toolBondHit=0;
    	    repaint();
	}

	checkDirtiness();
    }
    
    public void mouseMoved(MouseEvent e)
    {
    	boolean redraw=false;
	
	if (trackX!=e.getX() || trackY!=e.getY())
	{
	    if (tool==TOOL_TEMPLATE) redraw=true;
	}
	
	trackX=e.getX();
	trackY=e.getY();

    	if (e.getButton()==0)
	{
    	    int mx=e.getX(),my=e.getY();
    	    int newAtom=0,newBond=0;

    	    newAtom=pickAtom(mx,my);
	    if (newAtom==0) newBond=pickBond(mx,my);
	    
	    if (tool==TOOL_TEMPLATE && templateIdx>0) newBond=0;
	    if (tool==TOOL_TEMPLATE && templateIdx<0) newAtom=0;
	    
	    if (newAtom!=highlightAtom || newBond!=highlightBond)
	    {
    		highlightAtom=newAtom;
		highlightBond=newBond;
		redraw=true;
	    }
	}
	
    	if (redraw) repaint();
    }
    
    public void mouseDragged(MouseEvent e)
    {
    	boolean redraw=false;
	if (tool==TOOL_TEMPLATE && (trackX!=e.getX() || trackY!=e.getY())) redraw=true;
    	trackX=e.getX(); trackY=e.getY();
    
    	if ((tool==TOOL_CURSOR && toolDragReason!=0) || (tool==TOOL_ERASOR && toolDragReason!=0) ||
    	    (tool==TOOL_ROTATOR && toolDragReason==DRAG_SELECT))
	{
	    toolDragX2=e.getX();
	    toolDragY2=e.getY();
	    if (toolDragReason==DRAG_SELECT)
	    {
		int x=(int)toolDragX1,y=(int)toolDragY1,w=(int)toolDragX2-x,h=(int)toolDragY2-y;
		if (w<0) {w=-w; x-=w;}
		if (h<0) {h=-h; y-=h;}
		dragged=new boolean[mol.numAtoms()];
		for (int n=0;n<mol.numAtoms();n++) dragged[n]=px[n]>=x && px[n]<=x+w && py[n]>=y && py[n]<=y+h;
	    }
    	    redraw=true;
	}
	else if (tool==TOOL_ROTATOR && toolDragReason==DRAG_ROTATE)
	{
	    toolDragX2=e.getX();
	    toolDragY2=e.getY();
	    redraw=true;
	}
	else if (tool==TOOL_ATOM && toolBondFrom!=0)
	{
	    if (!toolAtomDrag)
	    {
	    	double dx=xToAng(e.getX())-mol.atomX(toolBondFrom),dy=yToAng(e.getY())-mol.atomY(toolBondFrom);
		if (dx*dx+dy*dy>0.8*0.8) 
		{
		    toolAtomDrag=true;
    	    	    toolBondOrder=1;
		    toolBondType=Molecule.BONDTYPE_NORMAL;
		}
	    }
	    if (toolAtomDrag)
	    {
	    	toolBondToX=xToAng(e.getX());
	    	toolBondToY=yToAng(e.getY());
		if (toolAtomSnap) snapToolBond();
	    	redraw=true;
	    }
	}
	else if (tool==TOOL_BOND /*&& toolBondFrom!=0*/)
	{
	    toolBondToX=xToAng(e.getX());
	    toolBondToY=yToAng(e.getY());
    	    int joinTo=pickAtom(e.getX(),e.getY());
    	    if (!toolBondDrag)
	    	if (Math.abs(toolBondToX-toolBondFromX)>2/scale || Math.abs(toolBondToY-toolBondFromY)>2/scale) toolBondDrag=true;
	    if (joinTo>0) {toolBondToX=mol.atomX(joinTo); toolBondToY=mol.atomY(joinTo);}
	    else if (toolSnap) snapToolBond();
	    redraw=true;
	}

    	if (redraw) repaint();
	checkDirtiness();
    }
    
    public void mouseWheelMoved(MouseWheelEvent e)
    {
	if (tool==TOOL_TEMPLATE)
	{
	    double cx=0,cy=0;
	    for (int n=1;n<=template.numAtoms();n++) {cx+=template.atomX(n); cy+=template.atomY(n);}
	    cx/=template.numAtoms();
	    cy/=template.numAtoms();
	
	    double accel=e.isShiftDown() ? 3 : 1;
	
	    if (e.isControlDown()) // scale
	    {
	    	double factor=1-0.1*accel*e.getWheelRotation();
	    	for (int n=1;n<=template.numAtoms();n++) 
		    template.setAtomPos(n,cx+(template.atomX(n)-cx)*factor,cy+(template.atomY(n)-cy)*factor);
	    }
	    else // rotate
    	    {
	    	double radians=5*accel*Math.PI/180*e.getWheelRotation();
		for (int n=1;n<=template.numAtoms();n++) 
		{
		    double dx=template.atomX(n)-cx,dy=template.atomY(n)-cy;
		    double dist=Math.sqrt(dx*dx+dy*dy),theta=Math.atan2(dy,dx);
		    template.setAtomPos(n,cx+dist*Math.cos(theta+radians),cy+dist*Math.sin(theta+radians));
		}
	    }
    	    templDraw=template.clone();
    	    repaint();
	}
    }
    
    // Other callbacks...
    
    public void focusGained(FocusEvent e) {}
    public void focusLost(FocusEvent e)
    {
    	if (e.getSource()==toolAtomEditBox) completeAtomEdit();
    }
    public void keyPressed(KeyEvent e) {}
    public void keyReleased(KeyEvent e) {}
    public void keyTyped(KeyEvent e) 
    {
    	if (e.getSource()==toolAtomEditBox) 
	{
	    if (e.getKeyChar()=='\n') completeAtomEdit();
	}
    }

    public void componentHidden(ComponentEvent e) {}
    public void componentMoved(ComponentEvent e) {}
    public void componentResized(ComponentEvent e)
    {
    	if (autoScale) {scaleToFit(); repaint();}
    }
    public void componentShown(ComponentEvent e) 
    {
    	if (autoScale) {scaleToFit(); repaint();}
    }
    
    // Class for deciding when molecule data can be dragged into this control.
    
    class TransferMoleculeDest extends TransferHandler
    {
	EditorPane dest;

	public TransferMoleculeDest(EditorPane dest) {this.dest=dest;}
	public boolean canImport(TransferHandler.TransferSupport info)
	{
	    if (!info.isDataFlavorSupported(DataFlavor.stringFlavor)) return false;
	    try
	    {
		String data=(String)info.getTransferable().getTransferData(DataFlavor.stringFlavor);
	    	Molecule mol=MoleculeStream.readUnknown(new BufferedReader(new StringReader(data)));
		return mol!=null;
	    }
	    catch (InvalidDnDOperationException e) 
	    {
	    	// this is thrown when dragging between different processes; it means we can't actually check the
		// data here, which is suboptimal, but not the end of the world
	    	return true;
	    }
	    catch (UnsupportedFlavorException e) {return false;}
	    catch (IOException e) {return false;}
	}
	public boolean importData(TransferHandler.TransferSupport info)
	{
	    if (!info.isDataFlavorSupported(DataFlavor.stringFlavor)) return false;
	    try
	    {
		String data=(String)info.getTransferable().getTransferData(DataFlavor.stringFlavor);
	    	Molecule mol=MoleculeStream.readUnknown(new BufferedReader(new StringReader(data)));
		if (mol==null || mol.numAtoms()==0) return false;
		Point pos=info.getDropLocation().getDropPoint();
		dest.addFragmentPosition(mol,(int)pos.getX(),(int)pos.getY());
		return true;
	    }
	    catch (UnsupportedFlavorException e) {return false;}
	    catch (IOException e) {return false;}
	}
    }
    
// jm.evers 13/1/2010 rotating according to applet params ; called by MainPanel.java
    public void RotateMolecule(){
     int degrees = MainApplet.rotation;
      if(degrees != 0){
        double dist;
        double dx;
        double dy;
        double theta;
        double radians=(double)(degrees*Math.PI/180);
        for(int n=1;n<=mol.numAtoms();n++){
    	 dx=mol.atomX(n);
    	 dy=mol.atomY(n);
    	 dist=Math.sqrt(dx*dx+dy*dy);
    	 theta=Math.atan2(dy,dx);
    	 try{
    	  mol.setAtomPos(n,dist*Math.cos(theta+radians),dist*Math.sin(theta+radians));
         }catch(Exception e){System.out.println("problem with rotation : "+e.toString());}
        }
        repaint();
       }
     }
                        
    public static boolean[] GrowArray(boolean array[], int newlength){
	boolean[] grow;
	int oldlength = array.length;
	grow = new boolean[ newlength ];
	for(int i=0; i < oldlength; i++){
    	    grow[i] = array[i];
	}
	return grow;
    }
}





