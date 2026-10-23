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
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

/*
    A dialog box which allows the summary information about a database to be edited.
*/

public class DialogEditSummary extends JDialog implements ActionListener
{
    private JTextField txtTitle;
    private JTextArea txtDescr;
    
    private JButton baccept,breject;
    
    private String resultTitle=null,resultDescr=null;
   
    public DialogEditSummary(Frame Parent,DataSheet DS)
    {
    	super(Parent,"Edit DataSheet Summary",true);
	
	setLayout(new BorderLayout());
	
	JLineup edits=new JLineup(JLineup.VERTICAL,1);
	edits.add(txtTitle=new JTextField(DS.getTitle()),"Title:",1,0);
	txtDescr=new JTextArea(DS.getDescription());
	txtDescr.setMinimumSize(new Dimension(300,200));
	txtDescr.setPreferredSize(new Dimension(300,200));
	edits.add(new JScrollPane(txtDescr),"Description:",1,1);
	
/* !!! aaarrgh	
	JPanel edits=new JPanel();
	GridBagLayout gb=new GridBagLayout();
	edits.setLayout(gb);

	GridBagConstraints gc=new GridBagConstraints();
    	gc.fill=GridBagConstraints.BOTH;
	gc.weighty=0.0;
	gc.insets=new Insets(1,1,1,1);

    	gc.gridwidth=GridBagConstraints.RELATIVE;
        gc.weightx=0.0;
	JLabel label=new JLabel("Title:");
	gb.setConstraints(label,gc);
	edits.add(label);
	
	gc.gridwidth=GridBagConstraints.REMAINDER;
        gc.weightx=1.0;
	txtTitle=new JTextField(DS.getTitle());
	gb.setConstraints(txtTitle,gc);
	edits.add(txtTitle);
	
    	gc.gridwidth=GridBagConstraints.RELATIVE;
        gc.weightx=0.0;
    	label=new JLabel("Description:");
	label.setVerticalAlignment(JLabel.TOP);
	gb.setConstraints(label,gc);
	edits.add(label);
	
        gc.weightx=1.0;
	gc.weighty=1.0;
	gc.gridwidth=GridBagConstraints.REMAINDER;
	gc.gridheight=GridBagConstraints.REMAINDER;
	txtDescr=new JTextArea(DS.getDescription());
	txtDescr.setMinimumSize(new Dimension(300,200));
	txtDescr.setPreferredSize(new Dimension(300,200));
	JScrollPane scrDescr=new JScrollPane(txtDescr);
	gb.setConstraints(scrDescr,gc);
	edits.add(scrDescr);
*/
    
	JPanel buttons=new JPanel();
	buttons.setLayout(new FlowLayout(FlowLayout.RIGHT));
	baccept=new JButton("Accept"); baccept.addActionListener(this); baccept.setMnemonic(KeyEvent.VK_A);
	breject=new JButton("Reject"); breject.addActionListener(this); breject.setMnemonic(KeyEvent.VK_R);

	buttons.add(baccept);
	buttons.add(breject);

    	add(edits,BorderLayout.CENTER);
	add(buttons,BorderLayout.SOUTH);
	
	pack();
    }

    // returns true if the datasheet has changed
    public boolean execute()
    {
    	setVisible(true);
	return resultTitle!=null && resultDescr!=null;
    }

    public String resultTitle() {return resultTitle;}
    public String resultDescr() {return resultDescr;}

    public void actionPerformed(ActionEvent e)
    {
    	if (e.getSource()==baccept) 
	{
    	    resultTitle=txtTitle.getText();
	    resultDescr=txtDescr.getText();
	    setVisible(false);
	}
    	else if (e.getSource()==breject) setVisible(false);
    }
}
