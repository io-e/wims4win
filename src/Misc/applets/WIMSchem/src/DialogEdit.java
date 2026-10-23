/*
    Sketch Elements: Chemistry molecular diagram drawing tool.
    
    (c) 2005 Dr. Alex M. Clark
    
    Released as GNUware, under the Gnu Public License (GPL)
    
    See www.gnu.org for details.
*/

package WIMSchem;

import java.util.*;
import java.text.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

/*
    A dialog box which allows tabulated editing of molecular data, as an alternative to doing all editing graphically.
*/

public class DialogEdit extends JDialog implements ActionListener
{
    Molecule mol,retMol=null;
    ArrayList<Integer> aselidx,bselidx;
    
    JTabbedPane tabs;
    JButton accept,reject;
    JTable atoms,bonds;

    static final String BOND_TYPES[]={"Normal","Inclined","Declined","Unknown"};

    public DialogEdit(Frame Parent,Molecule Mol,ArrayList<Integer> SelIdx)
    {
    	super(Parent,"Edit Molecule",true);
    	mol=Mol.clone();
	aselidx=SelIdx;
	bselidx=new ArrayList<Integer>();
	for (int n=1;n<=mol.numBonds();n++) if (aselidx.indexOf(mol.bondFrom(n))>=0 && aselidx.indexOf(mol.bondTo(n))>=0) bselidx.add(n);
	
	setLayout(new BorderLayout());
	
	atoms=new JTable(compileAtomData(),new String[]{"#","El","X","Y","Charge","Unpaired","HExplicit","MapNum"})
	    {public boolean isCellEditable(int row,int column) {return column>0;}};
	bonds=new JTable(compileBondData(),new String[]{"#","From","To","Order","Type"})
	    {public boolean isCellEditable(int row,int column) {return column>2;}};

    	atoms.getColumnModel().getColumn(0).setCellEditor(null);
	JComboBox bondTypes=new JComboBox();
	for (int n=0;n<BOND_TYPES.length;n++) bondTypes.addItem(BOND_TYPES[n]);
	bonds.getColumnModel().getColumn(4).setCellEditor(new DefaultCellEditor(bondTypes));

	JPanel tabAtoms=new JPanel(),tabBonds=new JPanel();
	tabAtoms.setLayout(new BorderLayout());
	tabBonds.setLayout(new BorderLayout());

    	atoms.setPreferredScrollableViewportSize(new Dimension(350,200));
    	bonds.setPreferredScrollableViewportSize(new Dimension(350,200));

	tabAtoms.add(new JScrollPane(atoms));
	tabBonds.add(new JScrollPane(bonds));

	tabs=new JTabbedPane();
	tabs.addTab("Atoms",tabAtoms);
	tabs.addTab("Bonds",tabBonds);
	add(tabs,BorderLayout.CENTER);
	
	JPanel buttons=new JPanel();
	buttons.setLayout(new FlowLayout(FlowLayout.RIGHT));
	accept=new JButton("Accept"); accept.addActionListener(this); accept.setMnemonic(KeyEvent.VK_A);
	reject=new JButton("Reject"); reject.addActionListener(this); reject.setMnemonic(KeyEvent.VK_R);
	buttons.add(accept);
	buttons.add(reject);
	add(buttons,BorderLayout.SOUTH);
	
	pack();
    }
    
    public Molecule exec()
    {
    	setVisible(true);
	return retMol;
    }
    
    public void actionPerformed(ActionEvent e)
    {
    	if (e.getSource()==accept) 
	{
	    if (!ReadData()) return;
	    retMol=mol; 
	    setVisible(false);
	}
	if (e.getSource()==reject) setVisible(false);
    }
    
    private Object[][] compileAtomData()
    {
    	Object[][] data=new Object[aselidx.size()][];
	
	DecimalFormat fmt = new DecimalFormat("0.0000",new DecimalFormatSymbols(Locale.US));

	for (int n=0;n<aselidx.size();n++)
	{
	    int i=aselidx.get(n).intValue();
	    Object[] da=new Object[8];
	    da[0]=new Integer(i);
	    da[1]=new String(mol.atomElement(i));
	    da[2]=fmt.format(mol.atomX(i));
	    da[3]=fmt.format(mol.atomY(i));
	    da[4]=String.valueOf(mol.atomCharge(i));
	    da[5]=String.valueOf(mol.atomUnpaired(i));
	    da[6]=mol.atomHExplicit(i)==Molecule.HEXPLICIT_UNKNOWN ? "?" : String.valueOf(mol.atomHExplicit(i));
	    da[7]=String.valueOf(mol.atomMapNum(i));
	    data[n]=da;
	}
	
	return data;
    }

    private Object[][] compileBondData()
    {
    	Object[][] data=new Object[bselidx.size()][];
	
	for (int n=0;n<bselidx.size();n++)
	{
	    int i=bselidx.get(n).intValue();
	    Object[] db=new Object[5];
	    db[0]=new Integer(i);
	    db[1]=new Integer(mol.bondFrom(i));
	    db[2]=new Integer(mol.bondTo(i));
	    db[3]=String.valueOf(mol.bondOrder(i));
	    db[4]=new String(BOND_TYPES[mol.bondType(i)]);
	    data[n]=db;
	}
	
	return data;
    }
    
    private boolean ReadData()
    {
    	for (int n=0;n<atoms.getRowCount();n++)
	{
	    int i=(Integer)atoms.getValueAt(n,0);
	    mol.setAtomElement(i,(String)atoms.getValueAt(n,1));
	    mol.setAtomPos(i,Util.safeDouble((String)atoms.getValueAt(n,2)),Util.safeDouble((String)atoms.getValueAt(n,3)));
	    
	    mol.setAtomCharge(i,Util.safeInt((String)atoms.getValueAt(n,4)));
	    mol.setAtomUnpaired(i,Util.safeInt((String)atoms.getValueAt(n,5)));
	    String hyStr=(String)atoms.getValueAt(n,6);
	    int hy=Util.safeInt(hyStr);
	    mol.setAtomHExplicit(i,hyStr.compareTo("0")==0 ? 0 : hy>0 ? hy : Molecule.HEXPLICIT_UNKNOWN);
	    mol.setAtomMapNum(i,Util.safeInt((String)atoms.getValueAt(n,7)));
	}
    	for (int n=0;n<bonds.getRowCount();n++)
	{
	    int i=(Integer)bonds.getValueAt(n,0);
	    mol.setBondOrder(i,new Integer((String)bonds.getValueAt(n,3)).intValue());
	    int type;
	    for (int j=BOND_TYPES.length-1;j>=0;j--) if (BOND_TYPES[j].compareTo((String)bonds.getValueAt(n,4))==0) 
	    	{mol.setBondType(i,j); break;}
	}
    	return true;
    }
}
