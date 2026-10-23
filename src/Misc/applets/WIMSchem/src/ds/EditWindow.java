/*
    Sketch Elements: Chemistry molecular diagram drawing tool.
    
    (c) 2008 Dr. Alex M. Clark
    
    Released as GNUware, under the Gnu Public License (GPL)
    
    See www.gnu.org for details.
*/

package WIMSchem.ds;

import WIMSchem.*;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

/*
    Editor window for the molecule contained within a datasheet cell
*/

public class EditWindow extends JFrame implements SaveListener, WindowListener, CellEditorListener
{
    private MainPanel mainPanel;
    private DataTableModel model;
    private TableMoleculeEditor edcell;
    
    private boolean hardkill=false;

    public EditWindow(Molecule mol,DataTableModel model,TableMoleculeEditor edcell) 
    {
    	super("WIMSchem - Cell Edit");
	this.model=model;
	this.edcell=edcell;

    	setFocusableWindowState(true);

	// application
	
	JFrame.setDefaultLookAndFeelDecorated(false); 
	setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

    	// main panel

	mainPanel=new MainPanel(null,MainPanel.MODE_SLAVE,this);
	mainPanel.setMolecule(mol);
	mainPanel.setSaveListener(this);

	getContentPane().setLayout(new BorderLayout());
	getContentPane().add(mainPanel,BorderLayout.CENTER);
	pack();

    	setIconImage(mainPanel.mainIcon.getImage());
	
	addWindowListener(this);
    }
    
    public void saveMolecule(Molecule mol)
    {
	edcell.setMolecule(mainPanel.molData().clone());
    }    

    public void editingCanceled(ChangeEvent e)
    {
    	hardkill=true;
    	dispose();
    }
    public void editingStopped(ChangeEvent e)
    {
    	hardkill=true;
    	dispose();
    }

    public void windowActivated(WindowEvent e) {}
    public void windowClosed(WindowEvent e)
    {
    	if (!hardkill)
	{
    	    edcell.stopCellEditing();
	}
    }
    public void windowClosing(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowOpened(WindowEvent e) {}
}
