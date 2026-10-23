/*
*********************************************************************************
* J.M. Evers 19/11/2009								*
* This is all amateur scriblings... So no copyrights.				*
* This source code file, and compiled classes derived from it,			*
* can be used and distributed without restriction, including for commercial use	*
* No warrenty whatoever								*
*********************************************************************************
*/
import java.applet.*;
import javax.swing.*;

public class InputWindow extends javax.swing.JFrame {  
    
    public InputWindow() {
        panel1 = new javax.swing.JPanel();
	textarea=new JTextArea();
	panel1.add(textarea);
	getContentPane().add(panel1);
        pack();
    }

    public javax.swing.JPanel panel1;
    public JTextArea textarea;
    
}