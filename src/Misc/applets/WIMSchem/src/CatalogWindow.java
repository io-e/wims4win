/*
    Sketch Elements: Chemistry molecular diagram drawing tool.
    
    (c) 2005 Dr. Alex M. Clark
    
    Released as GNUware, under the Gnu Public License (GPL)
    
    See www.gnu.org for details.
*/

package WIMSchem;

import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;

/*
    A window class for displaying a catalog of structures, ripped out of a text-style database such as an SD-file.
*/

public class CatalogWindow extends JFrame 
    implements MolSelectListener, ActionListener, ComponentListener, AdjustmentListener, WindowListener
{
    String catFN;
    FileInputStream istr=null;

    final int COL_SIZE=5;
    int basepos=0,selnum=-1,highestUpdated=0;

    Color bckgr,highl;

    JScrollBar scroll;
    JButton bopen,bclose;
    EditorPane[] entries;
    
    CatalogLoader loader=null;

    public CatalogWindow(String CatFN)
    {
    	catFN=CatFN;
	
	setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	JFrame.setDefaultLookAndFeelDecorated(false); 
	
	setTitle("WIMSchem Catalog - "+catFN);

	bckgr=getBackground();
	highl=new Color(Math.max(bckgr.getRed()-32,0),Math.max(bckgr.getGreen()-32,0),bckgr.getBlue());

    	ImageIcon mainIcon=new ImageIcon(getClass().getResource("/images/MainIcon.png"));
    	setIconImage(mainIcon.getImage());

    	GridBagLayout gb=new GridBagLayout();
    	GridBagConstraints gc=new GridBagConstraints();
    	gc.insets=new Insets(2,2,2,2);
	
	// add the molecules
	JPanel content=new JPanel();
	content.setLayout(new BoxLayout(content,BoxLayout.X_AXIS));
	entries=new EditorPane[COL_SIZE];
	for (int n=0;n<COL_SIZE;n++)
	{
	    entries[n]=new EditorPane(100,100);
	    entries[n].setEditable(false);
	    entries[n].setMolSelectListener(this);
	    entries[n].setToolCursor();
	    entries[n].setBorder(true);
	    entries[n].setMaximumSize(new Dimension(Short.MAX_VALUE,Short.MAX_VALUE));
	    content.add(entries[n]);
	}
	gc.fill=GridBagConstraints.BOTH;
	gc.gridwidth=GridBagConstraints.REMAINDER;
	gc.weightx=1;
	gc.weighty=1;
	gb.setConstraints(content,gc);
	add(content);
	
	// add the scroller and buttons
    	gc.fill=GridBagConstraints.HORIZONTAL;
	gc.weighty=0;

	scroll=new JScrollBar(JScrollBar.HORIZONTAL,0,0,0,0);
	scroll.addAdjustmentListener(this);
	gc.gridwidth=5;
	gc.weightx=1;
	gb.setConstraints(scroll,gc);
	add(scroll);
	
	bopen=new JButton("Open");
    	bopen.addActionListener(this);
    	bopen.setMnemonic(KeyEvent.VK_O);
	gc.gridwidth=1;
	gc.weightx=0;
	gb.setConstraints(bopen,gc);
	add(bopen);
	
	bclose=new JButton("Close");
    	bclose.setMnemonic(KeyEvent.VK_C);
    	bclose.addActionListener(this);
	gc.gridwidth=1;
	gc.weightx=0;
	gb.setConstraints(bclose,gc);
	add(bclose);

    	setLayout(gb);
	pack();
    	addComponentListener(this);
	addWindowListener(this);

    	try
	{	
    	    istr=new FileInputStream(catFN);
	}
	catch (IOException e)
	{
	    JOptionPane.showMessageDialog(null,e.toString(),"Catalog Read Failed",JOptionPane.ERROR_MESSAGE);
	    dispose();
	    return;
	}

	loader=new CatalogLoader(this,istr);
	loader.start();
    }
    
    
    public void synch(int Sz)
    {
	scroll.setMaximum(Sz);
	scroll.setVisibleAmount(COL_SIZE);
	scroll.setUnitIncrement(1);
	scroll.setBlockIncrement(COL_SIZE);
	
	if (basepos+5>=Sz || highestUpdated<=COL_SIZE) updatePosition(basepos);
	highestUpdated=Sz;
    }

    private void updatePosition(int NewPos)
    {
    	// NB: should speed this up sometime by re-using molecules that are already loaded
    
    	basepos=NewPos;
    	try
	{
    	    for (int n=0;n<COL_SIZE;n++)
	    {
	    	if (basepos+n>=loader.count()) 
		{
		    entries[n].setBackground(bckgr);
		    entries[n].clear();
		}
		else
		{
		    Molecule mol=loader.get(basepos+n);
		    entries[n].setBackground(basepos+n==selnum ? highl : bckgr);
		    entries[n].replace(mol,true,false);
		    entries[n].scaleToFit();
		    entries[n].repaint();
		}
	    }
	}
	catch (Exception e) {e.printStackTrace();}
    }

    private synchronized void openMolecule(int N)
    {
    	try
	{
	    Molecule mol=loader.get(N);
	    MainWindow mw=new MainWindow(null,false);
	    mw.mainPanel().setMolecule(mol);
	    mw.setVisible(true);
	}
	catch (Exception e) {e.printStackTrace();}
    }

    // event handling

    public void molSelected(EditorPane source,int idx,boolean dblclick)
    {
    	int entnum=-1;
	for (int n=0;n<COL_SIZE;n++) if (source==entries[n]) {entnum=n; break;}
	if (entnum>=0)
	{
	    selnum=basepos+entnum;
	    for (int n=0;n<COL_SIZE;n++) 
	    {
		entries[n].setBackground(n==entnum ? highl : bckgr);
		entries[n].repaint();
	    }
	    
	    if (idx!=0) openMolecule(selnum);
	}
	
    }
    public void dirtyChanged(boolean isdirty) {}
    
    public void actionPerformed(ActionEvent e)
    {
    	if (e.getSource()==bopen) {if (selnum>=0) openMolecule(selnum);}
	if (e.getSource()==bclose) 
	{
	    if (loader!=null) loader.zap();
	    dispose(); 
	    return;
	}
    }

    public void componentHidden(ComponentEvent e) {}
    public void componentMoved(ComponentEvent e) {}
    public void componentResized(ComponentEvent e) 
    {
    	for (int n=0;n<COL_SIZE;n++) entries[n].scaleToFit();
    }
    public void componentShown(ComponentEvent e) {}

    public void adjustmentValueChanged(AdjustmentEvent e)
    {
    	if (e.getSource()==scroll)
    	{
	    int pos=e.getValue();
	    if (pos!=basepos) updatePosition(pos);
	}
    }
    
    public void windowActivated(WindowEvent e) {}
    public void windowClosed(WindowEvent e) {loader.zap();}
    public void windowClosing(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowOpened(WindowEvent e) {}
}

// background thread which loads up the entries of the database, and lets the window know when new ones have arrived

class CatalogLoader extends Thread
{
    CatalogWindow wnd;
    FileInputStream istr;
    boolean zap=false;
    ArrayList<Long> filepos=new ArrayList<Long>();
    Object mutex=new Object();

    public CatalogLoader(CatalogWindow Wnd,FileInputStream IStr)
    {
    	wnd=Wnd;
    	istr=IStr;
    }
    
    public void run() 
    {
    	try
	{	    
	    long pos=0,nextpos;
	    while (!zap)
	    {
	    	synchronized (mutex) {nextpos=MoleculeStream.findNextPosition(istr,pos);}
		if (nextpos<0) break;

    	    	filepos.add(pos);
		pos=nextpos;
		
		// inform the main window that is has work to do!
    	    	EventQueue.invokeLater(new Runnable()
		{
    	    	    public void run()
    	    	    {
     	    	    	wnd.synch(filepos.size());
		    }
		});
	    }
	}
	catch (IOException e) 
	{
	    JOptionPane.showMessageDialog(null,e.toString(),"Catalog Load Failed",JOptionPane.ERROR_MESSAGE);
	    e.printStackTrace();
    	}
    }
    
    public void zap() {zap=true;} 
    public int count() {synchronized(mutex) {return filepos.size();}}
    public Molecule get(int N) 
    {
    	try
	{
    	    synchronized(mutex) {return MoleculeStream.fetchFromPosition(istr,filepos.get(N).longValue());}
	}
	catch (IOException e) {return null;}
    }
}
