/*
    Sketch Elements: Chemistry molecular diagram drawing tool.
    
    (c) 2008 Dr. Alex M. Clark
    
    Released as GNUware, under the Gnu Public License (GPL)
    
    See www.gnu.org for details.
*/

package WIMSchem;

import java.io.*;
import java.awt.*;
import java.util.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import javax.swing.*;

public class DraggableMolecule extends JLabel implements MouseListener,MouseMotionListener
{
    EditorPane src;
    TransferMoleculeSource trans;

    public DraggableMolecule(EditorPane src)
    {
    	this.src=src;
    	setIcon(new ImageIcon(getClass().getResource("/images/SmallIcon.png")));
	
        addMouseListener(this);
        addMouseMotionListener(this);
	
	trans=new TransferMoleculeSource(src);
	setTransferHandler(trans);
    }

    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mouseClicked(MouseEvent e) {}
    public void mousePressed(MouseEvent e)
    {
    	if (src.molData().numAtoms()>0) trans.exportAsDrag(this,e,TransferHandler.COPY);
    }
    public void mouseReleased(MouseEvent e) {}
    public void mouseMoved(MouseEvent e) {}
    public void mouseDragged(MouseEvent e) {}
  
    // Class for describing the content of that which is dragged from this control.
    
    class TransferMoleculeSource extends TransferHandler
    {
	EditorPane src;

	public TransferMoleculeSource(EditorPane src) {this.src=src;}
	public boolean canImport(TransferHandler.TransferSupport info) {return false;}
	public int getSourceActions(JComponent c) {return COPY;}
	public void exportAsDrag(JComponent source,InputEvent e,int action)
	{
	    super.exportAsDrag(source,e,action);
	}
    	protected void exportDone(JComponent source,Transferable data,int action) 
	{
	    super.exportDone(source,data,action);
	}
    	protected Transferable createTransferable(JComponent c)
	{
	    Molecule mol=/*src.molData()*/ src.selectedSubgraph();
	    try
	    {
		StringWriter sw=new StringWriter();
		BufferedWriter bw=new BufferedWriter(sw);
		MoleculeStream.writeMDLMOL(bw,mol);
		MoleculeStream.writeNative(bw,mol);
    		return new StringSelection(sw.toString());
	    }
	    catch (IOException ex) {return null;}
	}
    }
}
