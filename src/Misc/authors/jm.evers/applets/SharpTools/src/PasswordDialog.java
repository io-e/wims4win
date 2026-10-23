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
 * @(#)PasswordDialog.java
 *
 * $Id: PasswordDialog.java,v 1.4 2001/05/27 22:28:58 huaz Exp $
 *
 * Created on November 16, 2000, 12:00 AM
 */
package SharpTools;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
/** 
 * This class provides a password validating dialog.
 * User is prompted to enter and confirm a password
 * If passwords do not much an error message is displayed
 * and user is prompted to re-enter the passwords
 * <P>
 * 
 * @author Andrei Scudder
 * @author Hua Zhong (rewrote using SharpDialog)
 * @version $Revision: 1.4 $
 */
public class PasswordDialog extends SharpDialog {
    
    private JFrame frame;
    private String typedText = null;
    private String password;
    final private JPasswordField textField;
    final private JPasswordField confirmField;	
    final ImageIcon passwordIcon=new ImageIcon(getClass().getResource("/images/password32.gif"));
    //final private static ImageIcon passwordIcon = SharpTools.getImageIcon("password32.gif");
    
    // password is null if this is Set Password...
    // it's a password if we need to verify
    public PasswordDialog(JFrame aFrame, String pass) {
        
	super(aFrame, "Password", true);

	frame = aFrame;
	password = pass;
        
	//various properties of the dialog labels and text fields 
        final String msgString1 = SharpTools.translation.getProperty("Enter_password")+" : ";
        final String msgString2 = SharpTools.translation.getProperty("Confirm_Password")+" : ";
        textField = new JPasswordField(10);
	confirmField = new JPasswordField(10);	
	
	Object[] input = {msgString1, textField, msgString2, confirmField};
	Object[] verify = {msgString1, textField};
	Object[] array = password == null ? input : verify;

        setOptionPane(array, 
		      JOptionPane.QUESTION_MESSAGE,
		      JOptionPane.OK_CANCEL_OPTION,
		      passwordIcon);	
    }
    
    /*
     * @return password string
     */
    public String getValidatedText() {
        return typedText;
    }
    
    //compares to arrays of characters
    private static boolean fieldsAreTheSame(char[] input1, char[] input2) {
        
        if (input1.length != input2.length)
            return false;
        for (int i = 0;  i < input1.length; i ++)
            if (input1[i] != input2[i])
                return false;
        return true;
    }

    protected boolean onOK() {
	typedText = new String(textField.getPassword());
	
	if (password != null) {
	    // verify password
	    if (!typedText.equals(password)) {
		SharpOptionPane.showMessageDialog
		    (PasswordDialog.this,
		     SharpTools.translation.getProperty("Password_Incorrect")+" !" , SharpTools.translation.getProperty("Password"),
		     JOptionPane.ERROR_MESSAGE);
		textField.setText(null);
		textField.requestFocus();
		return false;
	    }
	    else
		return true;
	}
	
	//validate password
	if (fieldsAreTheSame(textField.getPassword(),
			     confirmField.getPassword())) {
	    //compare the dialogs
	    return true;
	} else { 
	    // passwords dont match
	    
	    textField.selectAll();
 	    SharpOptionPane.showMessageDialog
		(this,
		 SharpTools.translation.getProperty("Passwords_do_not_match_Please_try_again"),
		 SharpTools.translation.getProperty("Password"),
		 JOptionPane.ERROR_MESSAGE,
		 null);
	    
	    typedText = null;
	    	    
	    textField.setText(null);
	    confirmField.setText(null);
	    textField.requestFocus();
	    return false;
	}
    }
    
    protected boolean onCancel() {
	typedText = null;
	return true;
    }
    
    protected void onOpen() {
	// set the initial focus to textField
	textField.requestFocus();
    }
}
