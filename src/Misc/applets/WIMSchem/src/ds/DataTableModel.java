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
import javax.swing.border.*;

/*
    Data model to reconcile a DataSheet instance with the editing performed by a JTable component.
*/

public class DataTableModel extends AbstractTableModel 
{
    DataSheet ds=null;
    DataSheetCache cache=null;
    TitleListener tlist=null;
    
    public DataTableModel(DataSheet DS,DataSheetCache Cache,TitleListener TList) {ds=DS; cache=Cache; tlist=TList;}
    public DataSheet getDataSheet() {return ds;}
    public void setDataSheet(DataSheet DS,DataSheetCache Cache) {ds=DS; cache=Cache;}
    
    public int getColumnCount() {return ds.numCols();}
    public int getRowCount() {return ds.numRows();}
    public String getColumnName(int col) {return ds.colName(col);}
    public Object getValueAt(int row,int col) 
    {
    	int t=ds.colType(col);
	if (ds.isNull(row,col))
	{
	    if (t==DataSheet.COLTYPE_MOLECULE) return null;
	    if (t==DataSheet.COLTYPE_BOOLEAN) return new Boolean(false); // (no UI concept of null)
	    return ""; // null
	}
	else
	{
	    if (t==DataSheet.COLTYPE_MOLECULE) return ds.getMolecule(row,col);
	    if (t==DataSheet.COLTYPE_STRING) return ds.getString(row,col);
	    if (t==DataSheet.COLTYPE_INTEGER) return new Integer(ds.getInteger(row,col));
	    if (t==DataSheet.COLTYPE_REAL) return new Double(ds.getReal(row,col));
	    if (t==DataSheet.COLTYPE_BOOLEAN) return new Boolean(ds.getBoolean(row,col));
	}
	return "";
    }
    public Class getColumnClass(int col) 
    {
    	int t=ds.colType(col);
	if (t==DataSheet.COLTYPE_MOLECULE) return new Molecule().getClass();
	if (t==DataSheet.COLTYPE_STRING) return new String("").getClass();
	if (t==DataSheet.COLTYPE_INTEGER) return new String("").getClass();
	if (t==DataSheet.COLTYPE_REAL) return new String("").getClass();
	if (t==DataSheet.COLTYPE_BOOLEAN) return new Boolean(false).getClass();
	return new String("").getClass();
    }
    public boolean isCellEditable(int row,int col) {return true;}
    
    // updates the data in the underlying datasheet, with care to cache undo states, and do nothing if the value is the same
    public void setValueAt(Object val,int row,int col) 
    {
    	int t=ds.colType(col);
	try
	{
	    if (t==DataSheet.COLTYPE_MOLECULE) 
	    {
	    	if (!ds.isEqualMolecule(row,col,(Molecule)val))
		{
	    	    cache.cacheUndo(ds);
	    	    ds.setMolecule(row,col,(Molecule)val);
		    ds.setDirty();
		    tlist.replaceTitle();
		}
	    } 
	    else if (t==DataSheet.COLTYPE_STRING) 
	    {
	    	if (!ds.isEqualString(row,col,(String)val))
		{
	    	    cache.cacheUndo(ds);
	    	    ds.setString(row,col,(String)val);
		    ds.setDirty();
		    tlist.replaceTitle();
		}
	    }
	    else if (t==DataSheet.COLTYPE_INTEGER) 
	    {
	    	String str=(String)val;
		if (str.length()==0) 
		{
		    if (!ds.isNull(row,col))
		    {
		    	cache.cacheUndo(ds);
		    	ds.setToNull(row,col);
			ds.setDirty();
			tlist.replaceTitle();
		    }
		}
	    	else 
		{
		    int i=Integer.parseInt(str);
		    if (!ds.isEqualInteger(row,col,i))
		    {
		    	cache.cacheUndo(ds);
		    	ds.setInteger(row,col,i);
			ds.setDirty();
			tlist.replaceTitle();
		    }
		}
	    }
	    else if (t==DataSheet.COLTYPE_REAL) 
	    {
	    	String str=(String)val;
		if (str.length()==0) 
		{
		    if (!ds.isNull(row,col))
		    {
		    	cache.cacheUndo(ds);
		    	ds.setToNull(row,col);
			ds.setDirty();
			tlist.replaceTitle();
		    }
		}
		else 
		{
		    double d=Double.parseDouble(str);
		    if (!ds.isEqualReal(row,col,d))
		    {
		    	cache.cacheUndo(ds);
		    	ds.setReal(row,col,d);
			ds.setDirty();
			tlist.replaceTitle();
		    }
		}
	    }
	    else if (t==DataSheet.COLTYPE_BOOLEAN) 
	    {
	    	boolean b=((Boolean)val).booleanValue();
		if (!ds.isEqualBoolean(row,col,b))
		{
		    cache.cacheUndo(ds);
	    	    ds.setBoolean(row,col,b);
		    ds.setDirty();
		    tlist.replaceTitle();
	    	}
	    }
	}
	catch (NumberFormatException ex) {} // (do nothing --> change gets rejected)
    }
}


// Class implemented specially to draw molecules on a table.

// !! TODO: don't use EditorPane for rendering; instead use DrawMolecule to render onto an offscreen image, and cache this

class TableMoleculeRenderer extends EditorPane implements TableCellRenderer
{
    Border focusBorder=null;

    public TableMoleculeRenderer() 
    {
        setOpaque(true);

	setEditable(false);
	setAutoScale(true);
    }

    public Component getTableCellRendererComponent(JTable table,Object mol,boolean isSelected,boolean hasFocus,int row,int col)
    {
    	if (mol!=null) this.replace((Molecule)mol); else this.clear();

    	setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());

	if (hasFocus) 
	{
    	    if (focusBorder==null) 
		focusBorder=BorderFactory.createMatteBorder(1,1,1,1,table.getSelectionBackground().darker());
            setBorder(focusBorder);
	}
	else setBorder(null);

	return this;
    }
}

// Class implemented for editing of molecules: just sends an event notice to the parent, rather than actually doing the work.

class TableMoleculeEditor extends AbstractCellEditor implements TableCellEditor
{
    EditorPane edview=null;

    class PopMeUp implements Runnable
    {
    	JFrame win;
    	PopMeUp(JFrame win) {this.win=win;}
	public void run() {win.setVisible(true); win.requestFocus();}
    }

    public TableMoleculeEditor()
    {
    }

    // custom behaviour: the number of ways to make molecule cells popup the editor has to be somewhat constrained
    public boolean isCellEditable(EventObject e) 
    {
    	if (e instanceof KeyEvent)
	{
	    KeyEvent ev=(KeyEvent)e;
	    return ev.getKeyChar()==' ';
	}
	else if (e instanceof MouseEvent)
	{
	    MouseEvent ev=(MouseEvent)e;
	    return ev.getClickCount()==2 && ev.getButton()==MouseEvent.BUTTON1;
	}
	// (anything else??)
    	return false;
    }
    
    public void setMolecule(Molecule mol) {edview.replace(mol);}

    public Component getTableCellEditorComponent(JTable table,Object value,boolean isSelected,int row,int col) 
    {
    	Molecule mol=(Molecule)value;
	DataTableModel model=(DataTableModel)table.getModel();

    	if (mol==null) mol=new Molecule();
    
    	edview=new EditorPane();
	edview.setOpaque(true);
	edview.setEditable(false);
	edview.setAutoScale(true);
	Color sel=table.getSelectionBackground(),dark=sel.darker();
	edview.setBackground(dark);
	edview.replace(mol);

    	EditWindow edwin=new EditWindow(mol.clone(),model,this);
	addCellEditorListener(edwin);

        javax.swing.SwingUtilities.invokeLater(new PopMeUp(edwin));

	return edview;
    }
    public Object getCellEditorValue() {return edview==null ? null : edview.molData();}
}

