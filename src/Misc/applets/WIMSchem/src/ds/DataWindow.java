/*
    Sketch Elements: Chemistry molecular diagram drawing tool.

    (c) 2008 Dr. Alex M. Clark

    Released as GNUware, under the Gnu Public License (GPL)

    See www.gnu.org for details.
*/

package WIMSchem.ds;

import WIMSchem.*;

import java.io.*;
import java.awt.*;
import java.util.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

/*
    Main window for datasheet viewing & editing.
*/

public class DataWindow extends JFrame implements ActionListener, WindowListener, KeyListener, TitleListener
{
    public static final boolean ALLOW=true; // temporary: disable DataSheet use while development is in-progress

    String filename=null;
    JMenuBar menubar=null;
    JSplitPane splitter=null;
    DataSheet ds=null;
    DataSheetCache cache=null;
    DataTableModel model=null;
    JTable table=null;

    ImageIcon mainIcon=null,mainLogo=null;

    int unitRowHeight,curRowMag;

    public DataWindow(String LoadFN)
    {
    	super("WIMSchem DataSheet");

	JFrame.setDefaultLookAndFeelDecorated(false);
	setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

	mainIcon=new ImageIcon(getClass().getResource("/images/MainIcon.png"));
	mainLogo=new ImageIcon(getClass().getResource("/images/MainLogo.png"));

    	setIconImage(mainIcon.getImage());

    	filename=LoadFN;
	if (filename==null)
	{
	    ds=new DataSheet();
	    cache=new DataSheetCache();
	    ds.appendColumn("Molecule",DataSheet.COLTYPE_MOLECULE,"Molecular structure");
	}
	else loadDataSheet(filename);

	setLayout(new BorderLayout());

	menubar=new JMenuBar();
	createMenuBar();

    	model=new DataTableModel(ds,cache,this);
	table=new JTable(model)
	{
      	    protected boolean processKeyBinding(KeyStroke ks,KeyEvent e,int condition,boolean pressed)
	    {
	    	if (ks.getKeyCode()==KeyEvent.VK_ESCAPE) return false;
            	if ((e.getModifiersEx() & e.ALT_DOWN_MASK) != 0) return false;
            	if ((e.getModifiersEx() & e.CTRL_DOWN_MASK) != 0) return false;
            	return super.processKeyBinding(ks,e,condition,pressed);
      	    }
	};
    	table.setPreferredScrollableViewportSize(new Dimension(720,530));
	table.setDefaultRenderer(Molecule.class,new TableMoleculeRenderer());
	table.setDefaultEditor(Molecule.class,new TableMoleculeEditor());
	table.setRowSelectionAllowed(true);
	table.setColumnSelectionAllowed(true);
	table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	table.addKeyListener(this);
    	table.setTransferHandler(new TransferTableData(table,model));

	unitRowHeight=table.getRowHeight();
	updateRowHeight(2);

    	add(menubar,BorderLayout.NORTH);
	add(new JScrollPane(table),BorderLayout.CENTER);

	pack();

	addWindowListener(this);
    }

    // ------------------ utility functions --------------------

    // loads a new datasheet and replaces the old one, without question
    private void loadDataSheet(String FN)
    {
    	DataSheet newDS=null;
	FileInputStream istr=null;
    	try
	{
	    istr=new FileInputStream(FN);
	    if (DataSheetStream.examineIsXMLDS(istr))
	    {
	    	newDS=DataSheetStream.readXML(istr);
	    }
	    else if (DataSheetStream.examineIsMDLSDF(istr))
	    {
	    	newDS=DataSheetStream.readSDF(istr);
		FN=null;
	    }
	    else
	    {
	    	String msg="["+FN+"]\n"+
		    	   "The file does not appear to be of the XML\n"+
			   "WIMSchem DataSheet format, an MDL SD file.";
	    	JOptionPane.showMessageDialog(null,msg,"Open Failed",JOptionPane.ERROR_MESSAGE);
	    }
	    cache=new DataSheetCache();
	}
	catch (IOException e)
	{
	    e.printStackTrace();
	    JOptionPane.showMessageDialog(null,e.toString(),"Open Failed",JOptionPane.ERROR_MESSAGE);
	}
	finally
	{
	    try {if (istr!=null) istr.close();} catch (IOException e) {}
	}

	if (newDS==null)
	{
	    ds=new DataSheet(); // blank
	    replaceTitle();
	    return;
	}

	ds=newDS;
	setFilename(FN);

	if (model!=null)
	{
	    model.setDataSheet(ds,cache);
	    model.fireTableStructureChanged();
	}
    }

    // assembles the menu items, with appropriate context
    private void createMenuBar()
    {
    	boolean allKeys=true;

    	JMenu menufile=new JMenu("File");
	menufile.setMnemonic(KeyEvent.VK_F);
	menufile.add(menuItem("New",KeyEvent.VK_N,null,key('N',InputEvent.CTRL_MASK,allKeys)));
	menufile.add(menuItem("New Molecule",KeyEvent.VK_M));
	menufile.add(menuItem("Open",KeyEvent.VK_O,null,key('O',InputEvent.CTRL_MASK,allKeys)));
	menufile.add(menuItem("Save",KeyEvent.VK_S,null,key('S',InputEvent.CTRL_MASK,allKeys)));
	menufile.add(menuItem("Save As",KeyEvent.VK_A));
	JMenu menuexport=new JMenu("Export");
	menuexport.setMnemonic(KeyEvent.VK_X);
	menuexport.add(menuItem("as MDL SDF",KeyEvent.VK_S,null,key('S',InputEvent.CTRL_MASK+InputEvent.SHIFT_MASK,allKeys)));
	menufile.add(menuexport);
	menufile.addSeparator();
	menufile.add(menuItem("Quit",KeyEvent.VK_Q,null,key('Q',InputEvent.CTRL_MASK,allKeys)));

    	JMenu menuedit=new JMenu("Edit");
	menuedit.setMnemonic(KeyEvent.VK_E);
	menuedit.add(menuItem("Undo",KeyEvent.VK_U,null,key('Z',InputEvent.CTRL_MASK,allKeys)));
	menuedit.add(menuItem("Redo",KeyEvent.VK_R,null,key('Z',InputEvent.CTRL_MASK+InputEvent.SHIFT_MASK,allKeys)));
	menuedit.addSeparator();
	menuedit.add(menuItem("Add Row",KeyEvent.VK_A,null,key(KeyEvent.VK_INSERT,0,allKeys)));
	menuedit.add(menuItem("Delete Rows",KeyEvent.VK_D));
	menuedit.add(menuItem("Move Rows Up",KeyEvent.VK_U,null,key(KeyEvent.VK_UP,InputEvent.ALT_MASK,allKeys)));
	menuedit.add(menuItem("Move Rows Down",KeyEvent.VK_D,null,key(KeyEvent.VK_DOWN,InputEvent.ALT_MASK,allKeys)));
	menuedit.addSeparator();
	menuedit.add(menuItem("Copy Rows",KeyEvent.VK_C,null,key('C',InputEvent.CTRL_MASK+InputEvent.SHIFT_MASK,allKeys)));
	menuedit.add(menuItem("Cut Rows",KeyEvent.VK_T,null,key('X',InputEvent.CTRL_MASK+InputEvent.SHIFT_MASK,allKeys)));
	menuedit.add(menuItem("Paste Rows",KeyEvent.VK_P,null,key('V',InputEvent.CTRL_MASK+InputEvent.SHIFT_MASK,allKeys)));
	menuedit.addSeparator();
	menuedit.add(menuItem("Sheet Summary",KeyEvent.VK_S));
	menuedit.add(menuItem("Edit Columns",KeyEvent.VK_C));

	JMenu menuview=new JMenu("View");
	menuview.setMnemonic(KeyEvent.VK_V);
	menuview.add(menuItem("Single Line",KeyEvent.VK_L,null,key('1',InputEvent.CTRL_MASK,allKeys)));
	menuview.add(menuItem("Small Height",KeyEvent.VK_S,null,key('2',InputEvent.CTRL_MASK,allKeys)));
	menuview.add(menuItem("Medium Height",KeyEvent.VK_M,null,key('3',InputEvent.CTRL_MASK,allKeys)));
	menuview.add(menuItem("Large Height",KeyEvent.VK_L,null,key('4',InputEvent.CTRL_MASK,allKeys)));

    	JMenu menuhelp=new JMenu("Help");
	menuhelp.setMnemonic(KeyEvent.VK_H);
	menuhelp.add(menuItem("About",KeyEvent.VK_A));

	menubar.removeAll();
	menubar.add(menufile);
	menubar.add(menuedit);
	menubar.add(menuview);
	menubar.add(Box.createHorizontalGlue());
	menubar.add(menuhelp);
    }

    private KeyStroke key(char key, int mods, boolean really) {return really ? KeyStroke.getKeyStroke(key,mods) : null;}
    private KeyStroke key(int key, int mods, boolean really) {return really ? KeyStroke.getKeyStroke(key,mods) : null;}

    private JMenuItem menuItem(String txt,int key) {return menuItem(txt,key,null,null,true);}
    private JMenuItem menuItem(String txt,int key,Icon icon) {return menuItem(txt,key,icon,null,true);}
    private JMenuItem menuItem(String txt,int key,Icon icon,KeyStroke accel) {return menuItem(txt,key,icon,accel,true);}
    private JMenuItem menuItem(String txt,int key,Icon icon,KeyStroke accel,boolean enabled)
    {
    	JMenuItem mi=new JMenuItem(txt,key);
	mi.addActionListener(this);
	mi.setEnabled(enabled);
	if (icon!=null) mi.setIcon(icon);
	if (accel!=null) mi.setAccelerator(accel);
	return mi;
    }
    private JRadioButtonMenuItem radioMenuItem(String txt,int key,boolean sel,ButtonGroup bg)
    {
    	JRadioButtonMenuItem mi=new JRadioButtonMenuItem(txt,sel);
	mi.addActionListener(this);
	mi.setMnemonic(key);
	bg.add(mi);
	return mi;
    }

    private void updateRowHeight(int Mag)
    {
    	curRowMag=Mag;
	table.setRowHeight(unitRowHeight*(curRowMag==2 ? 3 : curRowMag==3 ? 6 : curRowMag==4 ? 12 : 1));
    }

    private void setFilename(String FN)
    {
    	filename=FN;
	replaceTitle();
    }

    public void replaceTitle()
    {
	String title="WIMSchem DataSheet";
	if (ds!=null && ds.isDirty()) title="*"+title;
	if (filename!=null) title+=" - "+new File(filename).getName();
	if (ds!=null && ds.getTitle().length()>0) title+=":"+ds.getTitle();
	setTitle(title);
    }

    private void saveCurrent()
    {
    	if (filename==null) return;
	try
	{
	    FileOutputStream ostr=new FileOutputStream(filename);
	    DataSheetStream.writeXML(ostr,ds);
	    ostr.close();
	    ds.clearDirty();
	    replaceTitle();
	}
	catch (IOException e)
	{
	    Util.errmsg("Save Failed",e.toString());
	}
    }

    // returns true if two datasheets have the same columns
    private boolean sameColumns(DataSheet DS1,DataSheet DS2)
    {
    	if (DS1.numCols()!=DS2.numCols()) return false;
	for (int n=0;n<DS1.numCols();n++)
	{
	    if (DS1.colName(n).compareTo(DS2.colName(n))!=0) return false;
	    if (DS1.colType(n)!=DS2.colType(n)) return false;
	    if (DS1.colDescr(n).compareTo(DS2.colDescr(n))!=0) return false;
	}
	return true;
    }

    // ------------------ user responses --------------------

    private void fileQuit()
    {
    	if (ds.isDirty())
	{
    	    if (JOptionPane.showConfirmDialog(null,
		"Current datasheet has been modified. Exit without saving?","Quit",
		JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
	}
    	dispose();
    }

    private void fileNew()
    {
	new DataWindow(null).setVisible(true);
    }

    private void fileNewMolecule()
    {
    	new MainWindow(null,false).setVisible(true);
    }

    private void fileOpen()
    {
	JFileChooser chooser=new JFileChooser(System.getenv().get("PWD"));
	chooser.setDragEnabled(false);
	chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	chooser.setFileFilter(new FileExtFilter("DataSheet Formats",".xml;.sdf"));
	// !! chooser.setAccessory(new FileMolPreview(chooser));
	if (chooser.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION) return;
	String newfn=chooser.getSelectedFile().getPath();

	if (ds.numRows()==0 && !ds.isDirty())
	    loadDataSheet(newfn); // replace blank with new thing
	else
    	    new DataWindow(newfn).setVisible(true); // pop up a new window
    }

    private void fileSave()
    {
    	if (filename==null) {fileSaveAs(); return;}
	saveCurrent();
    }

    private void fileSaveAs()
    {
	JFileChooser chooser=new JFileChooser(System.getenv().get("PWD"));
	chooser.setDragEnabled(false);
	chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	chooser.setFileFilter(new FileExtFilter("WIMSchem DataSheets",".xml"));
	if (chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION) return;

    	String fn=chooser.getSelectedFile().getPath();
	if (chooser.getSelectedFile().getName().indexOf('.')<0) fn=fn+".xml";

    	File newf=new File(fn);
    	if (newf.exists())
	{
    	    if (JOptionPane.showConfirmDialog(null,
		"Overwrite existing file ["+newf.getName()+"]?","Save As",
		JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
	}

    	setFilename(fn);
	saveCurrent();
    }

    private void fileExportSDF()
    {
	JFileChooser chooser=new JFileChooser(System.getenv().get("PWD"));
	chooser.setDragEnabled(false);
	chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	chooser.setFileFilter(new FileExtFilter("MDL SD files",".sdf"));
	if (chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION) return;

    	String fn=chooser.getSelectedFile().getPath();
	if (chooser.getSelectedFile().getName().indexOf('.')<0) fn=fn+".sdf";

    	File newf=new File(fn);
    	if (newf.exists())
	{
    	    if (JOptionPane.showConfirmDialog(null,
		"Overwrite existing file ["+newf.getName()+"]?","Save As",
		JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
	}

	// !! perhaps a warning if there are multiple molecule fields?

	try
	{
	    FileOutputStream ostr=new FileOutputStream(fn);
	    DataSheetStream.writeSDF(ostr,ds);
	    ostr.close();
	}
	catch (IOException e)
	{
	    Util.errmsg("Export Failed",e.toString());
	}
    }

    // !! TODO: undo/redo should un-dirtify the datasheet in some cases

    private void editUndo()
    {
    	if (!cache.canUndo()) return;

	DataSheet newds=cache.performUndo(ds);
	boolean colmod=!sameColumns(ds,newds);
	ds=newds;

	ds.setDirty();
	replaceTitle();
	model.setDataSheet(ds,cache);
	if (colmod) model.fireTableStructureChanged(); else model.fireTableDataChanged();
    }

    private void editRedo()
    {
    	if (!cache.canRedo()) return;

	DataSheet newds=cache.performRedo(ds);
	boolean colmod=!sameColumns(ds,newds);
	ds=newds;

	ds.setDirty();
	replaceTitle();
	model.setDataSheet(ds,cache);
	if (colmod) model.fireTableStructureChanged(); else model.fireTableDataChanged();
    }

    private void editSummary()
    {
    	DialogEditSummary edsumm=new DialogEditSummary(this,ds);
	if (!edsumm.execute()) return;

	if (edsumm.resultTitle().equals(ds.getTitle()) && edsumm.resultDescr().equals(ds.getDescription())) return;

    	ds.setTitle(edsumm.resultTitle());
	ds.setDescription(edsumm.resultDescr());
	ds.setDirty();
	replaceTitle();
    }

    private void editColumns()
    {
    	DialogEditColumns edcols=new DialogEditColumns(this,ds);
	if (!edcols.execute()) return;

	modifyColumns(edcols.resultOldPos(),edcols.resultNewPos(),edcols.resultName(),edcols.resultType(),edcols.resultDescr());

	model.fireTableStructureChanged();
	ds.setDirty();
	replaceTitle();
    }

    private void addRow()
    {
    	cache.cacheUndo(ds);
    	ds.appendRow();
	int selcol=table.getSelectedColumn();
	model.fireTableChanged(new TableModelEvent(model));

    	// !! currently, the INSERT key initiates an edit event...
	table.changeSelection(ds.numRows()-1,selcol<0 ? 0 : selcol,false,false);

    	ds.setDirty();
	replaceTitle();
    }

    private void deleteRows()
    {
    	if (table.getSelectedRowCount()==0) return;
    	cache.cacheUndo(ds);
	int[] todel=table.getSelectedRows(); // (sorted)
	for (int i=todel.length-1;i>=0;i--)
	{
	    ds.deleteRow(i);
	    for (int j=i+1;j<todel.length;j++) if (todel[j]>todel[i]) todel[j]--;
	}
	model.fireTableChanged(new TableModelEvent(model));
    	ds.setDirty();
	replaceTitle();
    }

    private void moveRowsUp()
    {
    	if (table.getSelectedRowCount()==0 || ds.numCols()==0) return;
	int[] rows=table.getSelectedRows(); // (is sorted)
	if (rows[0]==0) return; // if selected the top one, nop

    	cache.cacheUndo(ds);

    	for (int n=0;n<rows.length;n++) ds.moveRowUp(rows[n]);

	model.fireTableChanged(new TableModelEvent(model));
	for (int n=0;n<rows.length;n++) table.changeSelection(rows[n]-1,0,n!=0,false);
	table.setColumnSelectionInterval(0,ds.numCols()-1);

    	ds.setDirty();
    	replaceTitle();
    }

    private void moveRowsDown()
    {
    	if (table.getSelectedRowCount()==0 || ds.numCols()==0) return;
	int[] rows=table.getSelectedRows(); // (is sorted)
	if (rows[rows.length-1]==ds.numRows()-1) return; // if selected the bottom one, nop

    	cache.cacheUndo(ds);

    	for (int n=rows.length-1;n>=0;n--) ds.moveRowDown(rows[n]);

	model.fireTableChanged(new TableModelEvent(model));
	for (int n=0;n<rows.length;n++) table.changeSelection(rows[n]+1,0,n!=0,false);
	table.setColumnSelectionInterval(0,ds.numCols()-1);

    	ds.setDirty();
    	replaceTitle();
    }

    private void copyRows()
    {
    	if (table.getSelectedRowCount()==0) return;
    	int[] rn=table.getSelectedRows();

    	DataSheet copy=new DataSheet();
	for (int n=0;n<ds.numCols();n++)
	{
	    copy.appendColumn(ds.colName(n),ds.colType(n),ds.colDescr(n));
	}
	for (int i=0;i<rn.length;i++)
	{
	    copy.appendRow();
	    for (int j=0;j<ds.numCols();j++) copy.setObject(i,j,ds.getObject(rn[i],j));
	}

	StringWriter sw=new StringWriter();
	try
	{
	    DataSheetStream.writeXML(new BufferedWriter(sw),copy);
            Clipboard clip=Toolkit.getDefaultToolkit().getSystemClipboard();
    	    clip.setContents(new StringSelection(sw.toString()),null);
	}
	catch (IOException e)
	{
	    e.printStackTrace();
	    JOptionPane.showMessageDialog(null,e.toString(),"Copy Failed",JOptionPane.ERROR_MESSAGE);
	}
    }

    private void cutRows()
    {
    	copyRows();
	deleteRows();
    }

    private void pasteRows()
    {
    	String cliptext="";

    	try
	{
    	    Clipboard clip=Toolkit.getDefaultToolkit().getSystemClipboard();
	    Transferable contents=clip.getContents(null);
	    if (contents!=null && contents.isDataFlavorSupported(DataFlavor.stringFlavor))
		cliptext=(String)contents.getTransferData(DataFlavor.stringFlavor);
	}
	catch (UnsupportedFlavorException e)
	{
    	    JOptionPane.showMessageDialog(null,e.toString(),"Clipboard Read Failed",JOptionPane.ERROR_MESSAGE);
	}
	catch (IOException e)
	{
    	    JOptionPane.showMessageDialog(null,e.toString(),"Paste Failed",JOptionPane.ERROR_MESSAGE);
	}

    	DataSheet paste=null;
	try
	{
	    if (DataSheetStream.examineIsXMLDS(new BufferedReader(new StringReader(cliptext))))
		paste=DataSheetStream.readXML(new BufferedReader(new StringReader(cliptext)));
	    else if (DataSheetStream.examineIsMDLSDF(new BufferedReader(new StringReader(cliptext))))
		paste=DataSheetStream.readSDF(new BufferedReader(new StringReader(cliptext)));
	}
	catch (IOException e) {e.printStackTrace(); return;}

    	if (paste==null)
	{
	    JOptionPane.showMessageDialog(null,
	    	"Unknown format: must be DataSheet XML or MDL SDF","Paste Failed",JOptionPane.ERROR_MESSAGE);
	    return;
	}

    	cache.cacheUndo(ds);

	// handle columns first: find mapping index for each, based on name
	int[] newcolpos=new int[paste.numCols()];
	for (int i=0;i<paste.numCols();i++)
	{
	    newcolpos[i]=-1;
	    for (int j=0;j<ds.numCols();j++) if (ds.colName(j).compareTo(paste.colName(i))==0) {newcolpos[i]=j; break;}
	    if (newcolpos[i]<0) newcolpos[i]=ds.appendColumn(paste.colName(i),paste.colType(i),paste.colDescr(i));
	}

	// now paste the new rows
	for (int i=0;i<paste.numRows();i++)
	{
	    int rn=ds.appendRow();
	    for (int j=0;j<paste.numCols();j++)
	    {
	    	int cn=newcolpos[j];
		int ptype=paste.colType(j),dtype=ds.colType(j);
		String strval="";

		if (ptype==DataSheet.COLTYPE_MOLECULE && dtype==DataSheet.COLTYPE_MOLECULE)
		{
		    ds.setMolecule(rn,cn,paste.getMolecule(i,j));
		}
		else if (ptype==DataSheet.COLTYPE_MOLECULE || dtype==DataSheet.COLTYPE_MOLECULE) {} // not possible
		else
	    	{
		    String val="";
		    if (ptype==DataSheet.COLTYPE_STRING) val=paste.getString(i,j);
		    else if (ptype==DataSheet.COLTYPE_INTEGER) val=String.valueOf(paste.getInteger(i,j));
		    else if (ptype==DataSheet.COLTYPE_REAL) val=String.valueOf(paste.getReal(i,j));
		    else if (ptype==DataSheet.COLTYPE_BOOLEAN) val=paste.getBoolean(i,j) ? "true" : "false";

		    try
		    {
	    		if (dtype==DataSheet.COLTYPE_STRING) ds.setString(rn,cn,val);
			else if (dtype==DataSheet.COLTYPE_INTEGER) ds.setInteger(rn,cn,new Integer(val).intValue());
			else if (dtype==DataSheet.COLTYPE_REAL) ds.setReal(rn,cn,new Double(val).doubleValue());
			else if (dtype==DataSheet.COLTYPE_BOOLEAN)
			    ds.setBoolean(rn,cn,val.toLowerCase().compareTo("true")==0 ? true : false);
		    }
		    catch (NumberFormatException e) {} // stays null
		}
	    }
	}

	model.fireTableChanged(new TableModelEvent(model));
    }

    private void helpAbout()
    {
    	String msg="WIMSchem v"+MainPanel.VERSION+"\n"+
	    	   "Molecule drawing tool\n"+
		   "© 2005-2008 Dr. Alex M. Clark\n"+
		   "Released under the Gnu Public\n"+
		   "License (GPL), see www.gnu.org\n"+
		   "Home page and documentation:\n"+
		   "http://sketchel.sf.net\n";
    	JOptionPane.showMessageDialog(null,msg,"About WIMSchem",JOptionPane.INFORMATION_MESSAGE,mainLogo);
    }

    private void modifyColumns(int[] OldPos,int[] NewPos,String[] Name,int[] Type,String[] Descr)
    {
    	int sz=OldPos.length;

	// delete those which need to be chopped out
    	for (int n=0;n<sz;n++) if (NewPos[n]<0)
	{
	    ds.deleteColumn(OldPos[n]);
	    for (int i=0;i<sz;i++) if (OldPos[i]>OldPos[n]) OldPos[i]--;
	    for (int i=n;i<sz-1;i++)
	    {
	    	OldPos[i]=OldPos[i+1];
		NewPos[i]=NewPos[i+1];
		Name[i]=Name[i+1];
		Type[i]=Type[i+1];
		Descr[i]=Descr[i+1];
	    }
	    n--;
	    sz--;
	}

	// add the new ones
	for (int n=0;n<sz;n++) if (OldPos[n]<0)
	{
	    OldPos[n]=ds.appendColumn(Name[n],Type[n],Descr[n]);
	}

	// modify any existing content
	for (int n=0;n<sz;n++)
	{
	    ds.changeColumnName(OldPos[n],Name[n],Descr[n]);
	    ds.changeColumnType(OldPos[n],Type[n],true);
	}

	// now redefine the column order
	int[] reord=new int[sz];
	for (int n=0;n<sz;n++) reord[NewPos[n]]=OldPos[n];
    	cache.cacheUndo(ds);
	ds.reorderColumns(reord);
    }

    // ------------------ event functions --------------------

    public void actionPerformed(ActionEvent e)
    {
    	String cmd=e.getActionCommand();

    	//System.out.println("CMD:["+cmd+"]");

    	if (e.getSource()==table)
	{
	    /* !!
	    String[] rc=cmd.split(",");
	    editMolecule(Integer.parseInt(rc[0]),Integer.parseInt(rc[1]));
	    */
	}
	else if (cmd=="Quit") fileQuit();
	else if (cmd=="New") fileNew();
	else if (cmd=="New Molecule") fileNewMolecule();
    	else if (cmd=="Open") fileOpen();
	else if (cmd=="Save") fileSave();
	else if (cmd=="Save As") fileSaveAs();
	else if (cmd=="as MDL SDF") fileExportSDF();
	else if (cmd=="Undo") editUndo();
	else if (cmd=="Redo") editRedo();
    	else if (cmd=="Add Row") addRow();
    	else if (cmd=="Delete Rows") deleteRows();
	else if (cmd=="Move Rows Up") moveRowsUp();
	else if (cmd=="Move Rows Down") moveRowsDown();
    	else if (cmd=="Copy Rows") copyRows();
    	else if (cmd=="Cut Rows") cutRows();
    	else if (cmd=="Paste Rows") pasteRows();
    	else if (cmd=="Sheet Summary") editSummary();
    	else if (cmd=="Edit Columns") editColumns();
	else if (cmd=="Single Line") updateRowHeight(1);
	else if (cmd=="Small Height") updateRowHeight(2);
	else if (cmd=="Medium Height") updateRowHeight(3);
	else if (cmd=="Large Height") updateRowHeight(4);
	else if (cmd=="About") helpAbout();
    }

    public void windowActivated(WindowEvent e) {}
    public void windowClosed(WindowEvent e) {}
    public void windowClosing(WindowEvent e)
    {
    	fileQuit();
    }
    public void windowDeactivated(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowOpened(WindowEvent e) {}

    public void keyPressed(KeyEvent e) {}
    public void keyReleased(KeyEvent e) {}
    public void keyTyped(KeyEvent e) {}

    // Class for allowing molecule, or other table style, data to be dragged into parts of the table.

    class TransferTableData extends TransferHandler
    {
	JTable table;
	DataTableModel model;

	public TransferTableData(JTable table,DataTableModel model)
	{
	    this.table=table;
	    this.model=model;
	}
	public boolean canImport(TransferHandler.TransferSupport info)
	{
	    if (!info.isDataFlavorSupported(DataFlavor.stringFlavor)) return false;
	    String data=null;
	    try
	    {
		data=(String)info.getTransferable().getTransferData(DataFlavor.stringFlavor);
	    }
	    catch (InvalidDnDOperationException e)
	    {
	    	// this is thrown when dragging between different processes; it means we can't actually check the
		// data here, which is suboptimal, but not the end of the world
	    	return true;
	    }
	    catch (UnsupportedFlavorException e) {return false;}
	    catch (IOException e) {}

    	    // now we have string data: see if it is a molecule...
    	    try
	    {
	    	Molecule mol=MoleculeStream.readUnknown(new BufferedReader(new StringReader(data)));
		if (mol!=null)
		{
		    Point pos=info.getDropLocation().getDropPoint();
		    int col=table.columnAtPoint(pos);
		    int row=table.rowAtPoint(pos);
		    return col>=0 && row>=0 && model.getDataSheet().colType(col)==DataSheet.COLTYPE_MOLECULE;
		}
	    }
	    catch (IOException e) {}

	    // see if it is a datasheet fragment
	    // !!

	    return false;
	}
	public boolean importData(TransferHandler.TransferSupport info)
	{
	    if (!info.isDataFlavorSupported(DataFlavor.stringFlavor)) return false;
	    String data=null;
	    try
	    {
		data=(String)info.getTransferable().getTransferData(DataFlavor.stringFlavor);
	    }
	    catch (InvalidDnDOperationException e) {return false;}
	    catch (UnsupportedFlavorException e) {return false;}
	    catch (IOException e) {}

    	    // now we have string data: see if it is a molecule...
    	    try
	    {
	    	Molecule mol=MoleculeStream.readUnknown(new BufferedReader(new StringReader(data)));
		if (mol!=null)
		{
		    Point pos=info.getDropLocation().getDropPoint();
		    int row=table.rowAtPoint(pos);
		    int col=table.columnAtPoint(pos);
		    DataSheet ds=model.getDataSheet();
		    if (row>=0 && col>=0 && ds.colType(col)==DataSheet.COLTYPE_MOLECULE)
		    {
		    	model.setValueAt(mol,row,col);
			model.fireTableDataChanged();
			return true;
		    }
		    else return false;
		}
	    }
	    catch (IOException e) {}

	    // see if it is a datasheet fragment
	    // !!

	    return false;
	}
    }

}

// Used to allow other classes to notify that the title may need to change.

interface TitleListener
{
    public void replaceTitle();
}
