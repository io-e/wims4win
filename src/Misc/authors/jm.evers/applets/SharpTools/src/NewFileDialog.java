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
 * @(#)NewFileDialog.java
 *
 * $Id: NewFileDialog.java,v 1.6 2001/05/27 22:28:12 huaz Exp $
 *
 * Created on November 16, 2000, 12:00 AM
 */
package SharpTools;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
/** 
 * This class provides a new-file dialog.
 * User is prompted to choose rows/columns when they are creating a
 * new document.
 * 
 * @author Hua Zhong
 * @version $Revision: 1.6 $
 */

public class NewFileDialog extends SharpDialog {
    private int rows;
    private int cols;
    final ImageIcon newFileIcon=new ImageIcon(getClass().getResource("/images/new32.gif"));
    //final private static ImageIcon newFileIcon = SharpTools.getImageIcon("new32.gif");
    final NumberField rowField;
    final NumberField colField;
    final JCheckBox saveDefault;
    
    public NewFileDialog(JFrame aFrame, int initrows, int initcols) {
        
	super(aFrame, "New", true);

	//various properties of the dialog labels and text fields 
	final JLabel msgString1 = new JLabel(SharpTools.translation.getProperty("Rows")+": ");
	final JLabel msgString2 = new JLabel(SharpTools.translation.getProperty("Columns")+": ");
	rowField = new NumberField(5, true, true);
	colField = new NumberField(5, true, true);
	msgString1.setLabelFor(rowField);
	msgString2.setLabelFor(colField);
	msgString1.setDisplayedMnemonic(KeyEvent.VK_R);
	msgString2.setDisplayedMnemonic(KeyEvent.VK_C);

	saveDefault = new JCheckBox(SharpTools.translation.getProperty("Save_as_default"), false);
	saveDefault.setMnemonic(KeyEvent.VK_S);

	rowField.setText(String.valueOf(initrows));
	colField.setText(String.valueOf(initcols));
	
	Object[] input = {msgString1, rowField, msgString2, colField,
			  saveDefault};

        setOptionPane(input, 
		      JOptionPane.QUESTION_MESSAGE,
		      JOptionPane.OK_CANCEL_OPTION,
		      newFileIcon);
	
	//	pack();
    }

    /**
     * Get the input row number
     *
     * @return the row number
     */
    public int getRows() { return rows; }
    
    /**
     * Get the input column number
     *
     * @return the column number
     */
    public int getColumns() { return cols; }

    /**
     * a function to display error messages
     *
     * @param error the error message
     * @param field the component causing this error
     */
    private void newFileError(String error, JTextField com) {    
        SharpOptionPane.showMessageDialog(this, error, SharpTools.translation.getProperty("Error"),
					  JOptionPane.ERROR_MESSAGE);
	com.selectAll();
	com.requestFocus();
    }

    protected boolean onOK() {
		    
	try {
	    rows = rowField.getInteger().intValue();
	}
	catch (Exception e1) {
	    rows = 0;
	}
	
	if (rows <= 0) {
	    newFileError(SharpTools.translation.getProperty("Invalid_row_value"), rowField);
	    return false;
	}
	
	try {
	    cols = colField.getInteger().intValue();
	}
	catch (Exception e2) {
	    cols = 0;
	}
	
	if (cols <= 0) {
	    newFileError(SharpTools.translation.getProperty("Invalid_column_value"), colField);
	    return false;
	}   
	
	if (saveDefault.isSelected()) {
	    // save stuff
	    Config config = SharpTools.getConfig();
	    config.setInt("ROWS", rows);
	    config.setInt("COLUMNS", cols);
	}			
	
	return true;
    }

    protected boolean onCancel() {
	rows = cols = 0;
	return true;
    }
	
    protected void onOpen() {
	// set the initial focus to rowField
	rowField.selectAll();
	rowField.requestFocus();
    }
		
    protected boolean onClose() {
	// cancelled
	rows = cols = 0;
	return true;
    }
}
