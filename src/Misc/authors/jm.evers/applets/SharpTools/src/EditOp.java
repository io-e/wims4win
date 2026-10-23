/*                                                                                                             
License.                                                                                                       
This is free software; you can redistribute it and/or modify                                                   
it under the terms of the GNU General Public License as published by                                           
the Free Software Foundation;                                                                                  
HotEqn is distributed in the hope that it will be useful,                                                      
but WITHOUT ANY WARRANTY; without even the implied warranty of                                                 
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                                                  
GNU General Public License for more details.                                                                   
You should have received a copy of the GNU General Public License                                              
along with this program.  If not, see <http://www.gnu.org/licenses/>.                                          
*/ 
/*
 * @(#)EditOp.java
 *
 * $Id: EditOp.java,v 1.37 2001/06/03 22:20:47 huaz Exp $
 *
 * Created on November 19, 2000, 02:16:16 AM
 */
package SharpTools;
import javax.swing.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
/**
 * This contain certain higher level edit operations on the spreadsheet table
 * 
 * @author Hua Zhong
 * @version $Revision: 1.37 $
 */



public class EditOp implements ActionListener {

    // these variables correspond to the variables in SharpTools
    private SharpTools sharp;
    private JTable table;
    private SharpTableModel tableModel;
    private History history;
    private JToolBar toolBar;
    private CellPoint copyPoint = new CellPoint(SharpTools.baseRow, SharpTools.baseCol);
    // jm.evers
    private Clipboard my_local_clipboard;
    // used for Find and Find Next
    //    private String fillValue;
    private String findValue;
    private boolean matchCase = false;
    private boolean matchCell = false;
    final ImageIcon fillIcon=new ImageIcon(getClass().getResource("/images/fill32.gif"));                                                  
    final ImageIcon findIcon=new ImageIcon(getClass().getResource("/images/find32.gif"));
    //final private static ImageIcon fillIcon = SharpTools.getImageIcon("fill32.gif");
    //final private static ImageIcon findIcon = SharpTools.getImageIcon("find32.gif");
    
    /** constructor
     *
     * @param sharp the GUI object
     */
    EditOp(SharpTools sharp) {
	this.sharp = sharp;
	table = sharp.getTable();

	// Identifying the undo KeyStroke user can modify this
	// to undo on some other Key combination.
	KeyStroke undo = KeyStroke.getKeyStroke
	    (KeyEvent.VK_Z,ActionEvent.CTRL_MASK,false);	

	// Identifying the undo KeyStroke user can modify this
	// to undo on some other Key combination.
	KeyStroke redo = KeyStroke.getKeyStroke
	    (KeyEvent.VK_Y,ActionEvent.CTRL_MASK,false);
	
	// Identifying the cut KeyStroke user can modify this
	// to cut on some other Key combination.
	KeyStroke cut = KeyStroke.getKeyStroke
	    (KeyEvent.VK_X,ActionEvent.CTRL_MASK,false);
	
	// Identifying the copy KeyStroke user can modify this
	// to copy on some other Key combination.
	KeyStroke copy = KeyStroke.getKeyStroke
	    (KeyEvent.VK_C,ActionEvent.CTRL_MASK,false);	
	
	// Identifying the Paste KeyStroke user can modify this
	// to copy on some other Key combination.
	KeyStroke paste = KeyStroke.getKeyStroke
	    (KeyEvent.VK_V,ActionEvent.CTRL_MASK,false);

	KeyStroke fill = KeyStroke.getKeyStroke
	    (KeyEvent.VK_L, ActionEvent.CTRL_MASK, false);
	
	KeyStroke clear = KeyStroke.getKeyStroke
	    (KeyEvent.VK_DELETE, 0, false);

	// Identifying the find/findnext KeyStroke user can modify this
	// to find on some other Key combination.
	KeyStroke find = KeyStroke.getKeyStroke
	    (KeyEvent.VK_F,ActionEvent.CTRL_MASK,false);
	
	KeyStroke findnext = KeyStroke.getKeyStroke
	    (KeyEvent.VK_F3,0,false);
	
	table.registerKeyboardAction(this,SharpTools.translation.getProperty("Undo"), undo,
				     JComponent.WHEN_FOCUSED);

	table.registerKeyboardAction(this,SharpTools.translation.getProperty("Redo"), redo,
				     JComponent.WHEN_FOCUSED);	
	
	table.registerKeyboardAction(this,SharpTools.translation.getProperty("Cut"), cut,
				     JComponent.WHEN_FOCUSED);

	table.registerKeyboardAction(this,SharpTools.translation.getProperty("Copy"), copy,
				     JComponent.WHEN_FOCUSED);

	table.registerKeyboardAction(this,SharpTools.translation.getProperty("Paste"),paste,
				     JComponent.WHEN_FOCUSED);

	table.registerKeyboardAction(this,SharpTools.translation.getProperty("Fill"),fill,
				     JComponent.WHEN_FOCUSED);
	
	table.registerKeyboardAction(this,SharpTools.translation.getProperty("Clear"),clear,
				     JComponent.WHEN_FOCUSED);

	table.registerKeyboardAction(this,SharpTools.translation.getProperty("Find"),find,
				     JComponent.WHEN_FOCUSED);
	
	table.registerKeyboardAction(this,SharpTools.translation.getProperty("Find_Next"),findnext,
				     JComponent.WHEN_FOCUSED);


	init(sharp);
    }

    /**
     * init method.
     * The reason to have this is to reset the EditOp but keep
     * the findValue and clipboard.
     *
     * @param sharp the GUI object
     */
    public void init(SharpTools sharp) {
	tableModel = sharp.getTableModel();
	history = sharp.getHistory();
    }
    
    /**
     * Perfroms a clipboard function of cutting COPY + CLEAR
     *
     * @param clipboard the SharpClipboard object
     */
    public void cut(){
	doCopy(true); //sets isCut to true
    }

    /**
     * Perfroms a clipboard function of cutting COPY + CLEAR
     */
    public void copy() {
	doCopy(false); //sets isCut to false
    }
    
    /**
     * Performs a clipboard function of cut/copy
     *
     * @param isCut true for cut, false for copy
     */
    private void doCopy(boolean isCut) {
	if (table.getSelectedRowCount() != 0) { 
	    CellRange range = new CellRange(
                         table.getSelectedRows(), table.getSelectedColumns());

	    if (isCut) {
		history.add(tableModel, range);
	    }	    

	    //	    toolBar.getComponent(8).setEnabled(true);
	    // now do the copy operation
	    StringBuffer sbf=new StringBuffer();

	    int startRow = table.getSelectedRow();
	    int startCol = table.getSelectedColumn();
	    
	    int numrows=table.getSelectedRowCount();
	    int numcols=table.getSelectedColumnCount();
	    
	    copyPoint = new CellPoint(table.getSelectedRow(),
				      table.getSelectedColumn());

	    String str = tableModel.toString(range, false);
	    StringSelection stsel = new StringSelection(str);
	    //jm.evers using a local clipboard to avoid jar-signing
	    //Clipboard clipboard = new Clipboard ("name").
	    my_local_clipboard = new Clipboard("my_local_clipboard");
	    my_local_clipboard.setContents(stsel,stsel);

	    if (isCut)
		tableModel.clearRange(range);
	    
	} else {
            if (isCut) {
	        sharp.noCellsSelected(SharpTools.translation.getProperty("Cut"));
            }else {
                sharp.noCellsSelected(SharpTools.translation.getProperty("Copy"));
	    }
        }
    }

    /**
     * Performs a clipboard function of pasting
     */
    public void paste(){
        //checks if anything is selected	
        if (table.getSelectedRowCount() != 0) {
	    int startRow=table.getSelectedRow();
	    int startCol=table.getSelectedColumn(); 
	    
	    int rowOff = startRow-copyPoint.getRow();
	    int colOff = startCol-copyPoint.getCol();
	    
	    try {
		
		String trstring=
		    (String)(my_local_clipboard.getContents(this).
			     getTransferData(DataFlavor.stringFlavor));

		CellPoint size = SharpTableModel.getSize(trstring);
		int endRow = Math.min(table.getRowCount()-1,
				      startRow+size.getRow()-1);
		int endCol = Math.min(table.getColumnCount()-1,
				      startCol+size.getCol()-1);

		CellRange affectedRange = new CellRange(startRow, endRow,
							startCol, endCol);
		// add to history
		history.add(tableModel, affectedRange);
		//		System.out.println(affectedRange);
		
		tableModel.fromString(trstring, rowOff, colOff, affectedRange);
	    }
	    catch(Exception e){
			System.out.println(e.toString());
	    }
	} else {
	    sharp.noCellsSelected(SharpTools.translation.getProperty("Paste"));
	}
    }    
    /** 
     * Wrapper function to clear cell and range of cells....
     */
    public void clear() {
	//checks if anything is selected
	if (table.getSelectedRowCount() != 0) {
	    CellRange range = new CellRange(
			 table.getSelectedRows(), table.getSelectedColumns());
	    history.add(tableModel, range);
	    Debug.println("Clear");
	    tableModel.clearRange(range);
	} else { 
	    sharp.noCellsSelected(SharpTools.translation.getProperty("Clear"));
	}
    }


    /** 
     * Wrapper function to fill a range of cell with a user defined value
     */
    public void fill() {
	//checks if anything is selected
	if (table.getSelectedRowCount() != 0) {
	    CellRange range = new CellRange(
                         table.getSelectedRows(), table.getSelectedColumns());

	    Cell first = tableModel.getCellAt(range.getStartRow(),
					      range.getStartCol());
	    String fillValue = null;
	    if (first.isFormula())
		fillValue = "="+first.getFormula().toString();
	    else
		fillValue = first.getValue().toString();
	    
	    Object inputValue =
		SharpOptionPane.showInputDialog
		(sharp, SharpTools.translation.getProperty("Please_enter_a_value_to_fill_the_range"),
		 SharpTools.translation.getProperty("Fill"), JOptionPane.INFORMATION_MESSAGE,
		 fillIcon, null, fillValue);

	    //if input is cancelled or nothing is entered 
	    //then don't change anything
	    if (inputValue != null){
		if (((String)inputValue).length() != 0) {
		    history.add(tableModel, range);
		    tableModel.fillRange(range, (String)inputValue);
		    //		    fillValue = inputValue;
		}
	    }
	}else { 
	    sharp.noCellsSelected(SharpTools.translation.getProperty("Fill"));
	}
    }

    /** 
     * Wrapper function to fill a range of cell with a user defined value
     *
     * @param newValue is true if it should require a new value (Find...)
     *                 is false if it already has a value (Find Next)
     */
    public void find(boolean newValue) {
        CellPoint start;
        
	//checks if anything is selected

	if (table.getSelectedRowCount() != 0) {
	    int x = table.getSelectedRow();
	    int y = table.getSelectedColumn();
	    // start from the next cell
	    if (!newValue)
		if (y < table.getColumnCount()-1)
		    y++;
		else {
		    y = 1;
		    x++;
		}
	    
	    start = new CellPoint(x, y);
	    
        }
	else {
	    // or start from the beginning
            start = new CellPoint(SharpTools.baseRow, SharpTools.baseCol);
        }

	if (newValue) {
	    // ask for new value
	    //String inputValue = SharpOptionPane.showInputDialog
		//(sharp, "Find: ", "Find", findIcon, findValue, 10);
	    FindDialog findDialog = new FindDialog(sharp, findValue, matchCase, matchCell);
	    findDialog.pack();
	    findDialog.setLocationRelativeTo(sharp);
	    findDialog.setVisible(true);

	    String inputValue = findDialog.getString();
	    /*	    
	    matchCase = findDialog.isCaseSensitive();
	    matchCell = findDialog.isCellMatching();

	    Debug.println("case sens : " + findDialog.isCaseSensitive());
	    Debug.println("match cell : " + findDialog.isCellMatching());
	    */
	    //if input is cancelled or nothing is entered 
	    //then don't change anything
	    if (inputValue == null || inputValue.length() == 0)
		return;
	    else {
		setFindValue(inputValue);
		matchCase = findDialog.isCaseSensitive();
		matchCell = findDialog.isCellMatching();

		Debug.println("case sens : " + findDialog.isCaseSensitive());
		Debug.println("match cell : " + findDialog.isCellMatching());
	    }
		
	}
	else if (!hasFindValue())
	    return;

	CellPoint found = tableModel.look(start, SharpTableModel.fieldParser(findValue), matchCase, matchCell);
	if (found != null) {
	    //System.out.println(found);
	    table.setColumnSelectionInterval(found.getCol(), 
					     found.getCol());
	    table.setRowSelectionInterval(found.getRow(),
					  found.getRow());
	    // set it visible
	    table.scrollRectToVisible
		(new Rectangle
		    (table.getCellRect(found.getRow(), found.getCol(), true)));
	}	
	else {
	    SharpOptionPane.showMessageDialog
		(sharp, 
		SharpTools.translation.getProperty("SharpTools_has_finished_the_search") + "  : "+ findValue ,
		 SharpTools.translation.getProperty("Find_Completed"),
		 JOptionPane.INFORMATION_MESSAGE, findIcon); 
	}
    }

    /**
     * Whether a value has been searched by the user
     *
     * @return true if the findValue has been set
     */
    public boolean hasFindValue() {
	return findValue != null && findValue.length() != 0;
    }

    private void setFindValue(String s) {
	findValue = s;
	sharp.checkFindNextState();
    }
    
    /**
     * Whether the clipboard has data
     *
     * @return true if clipboard has been set
     */
    /*
      public boolean hasClipboard() {
      return 
      }
    */
    
   /**
    * This method is activated on the Keystrokes we are listening to
    * in this implementation. Here it listens for keystroke
    * ActionCommands.
    *
    * Without this listener, when we press certain keys the individual
    * cell will be activated into editing mode in addition to the
    * effect of the key accelerators we defined with menu items.
    * With this key listener, we avoid this side effect.
    */
    public void actionPerformed(ActionEvent e) {

	if (e.getActionCommand().compareTo(SharpTools.translation.getProperty("Undo"))==0) {
	    history.undo(tableModel);
	}
	else if (e.getActionCommand().compareTo(SharpTools.translation.getProperty("Redo"))==0) {
	    history.redo(tableModel);
	}	
	else if (e.getActionCommand().compareTo(SharpTools.translation.getProperty("Cut"))==0) {
	    cut();
	}	
	else if (e.getActionCommand().compareTo(SharpTools.translation.getProperty("Copy"))==0) {
	    copy();
	}		
	else if (e.getActionCommand().compareTo(SharpTools.translation.getProperty("Paste"))==0) {
	    paste();
	}
	else if (e.getActionCommand().compareTo(SharpTools.translation.getProperty("Fill"))==0) {
	    fill();
	}
	else if (e.getActionCommand().compareTo(SharpTools.translation.getProperty("Find"))==0) {
	    find(true);
	}
	else if (e.getActionCommand().compareTo(SharpTools.translation.getProperty("Find_Next"))==0) {
	    find(false);
	}
    }
}

