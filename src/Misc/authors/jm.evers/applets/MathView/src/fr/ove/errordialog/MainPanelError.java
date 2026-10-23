/*
$Id: MainPanelError.java,v 1.3 2003/02/18 11:48:46 sander Exp $
*/


/*
Copyright (C) 2001-2002 Mainline Project (I3S - ESSI - CNRS -UNSA)

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

For further information on the GNU Lesser General Public License,
see: http://www.gnu.org/copyleft/lesser.html
For further information on this library, contact: mainline@essi.fr
*/


package fr.ove.errordialog;

import java.awt.*;
import java.awt.event.*;
import fr.ove.errordialog.*;
//import fr.ove.utils.PanelImage;

/**
* A panel to insert in the <CODE>fr.ove.errordialog.ErrorDialod</CODE> dialog
* box to display an error.
* The instance can display specific error message and give more details about
* the error.
*
* @author Â© 1998 DIRAT Laurent
* @version 1.0  13/04/99
*/
public class MainPanelError extends Panel {
	Panel panelError;
	
	Panel panelErrorName;
	PanelImage smileyL, smileyR;
	Label labelErrorName;
	
	Panel panelErrorMesgs;
	Label labelMesg1;
	Label labelMesg2;
	Label labelMesg3;
	
	Panel panelButton;
	Button buttonCloseWindow;
	Button buttonDetails;
	
	TextArea details;
	
	Window window;
    
    /**
    * The constructor
    */
	public MainPanelError (Window theWindow, String errorMessage)	{
	    window = theWindow;
	    
	    Font commonFont = new Font("Dialog", Font.PLAIN, 14);
	    
	    // Le panel qui contient tout ce qui est information de l'erreur (le nom, les messages, ...)
		setLayout(new BorderLayout(0,0));
		panelError = new Panel();
		panelError.setLayout(new BorderLayout(0,0));
		add("North", panelError);
		
		// Le panel qui contient le nom de l'erreur (avec le smiley)
		panelErrorName = new Panel();
		panelErrorName.setLayout(new BorderLayout(0,0));
		panelErrorName.setBackground(Color.white);
		panelError.add("North", panelErrorName);
		
		try {
		    // Comme PanelImage est dans un autre package que le courant il faut que l'on donne
		    // le chemin de l'image Ã  charger relativement au package de PanelImage.
		    // Donc :
		    //      PanelImage est dans le package fr.ove.utils
		    //      L'instance dans le package fr.ove.errordialog, rÃ©pertoire ou se trouve smiley.gif
		    //      D'oÃ¹ le chemin ../errordialog/smiley.gif
    		//smileyL = new PanelImage("../errordialog/smiley.gif");
    		
    		// Bon on verra cette histoire si on refait passer PanelImage dans fr.ove.utils et l'image
    		// ailleurs
    		
    		smileyL = new PanelImage("smiley.gif");
    		panelErrorName.add("West", smileyL);
    	}
    	catch(Exception e) {
    	    e.printStackTrace();
    	}
		
		labelErrorName = new Label(errorMessage, Label.CENTER);
		labelErrorName.setFont(new Font("Dialog", Font.PLAIN, 20));
		panelErrorName.add("Center", labelErrorName);
		
		try {
    		//smileyR = new PanelImage("../errordialog/smiley.gif");
    		smileyR = new PanelImage("smiley.gif");
    		panelErrorName.add("East", smileyR);
    	}
    	catch(Exception e) {
    	    e.printStackTrace();
    	}

		// Le panel qui contient tous les messages et boutons de commande
		panelErrorMesgs = new Panel();
		panelErrorMesgs.setLayout(new GridLayout(4,1,0,0));
		panelError.add("Center", panelErrorMesgs);
		
		// Un message d'erreur
		labelMesg1 = new Label("",Label.CENTER);
		labelMesg1.setFont(commonFont);
		panelErrorMesgs.add(labelMesg1);
		
		// Un autre message d'erreur
		labelMesg2 = new Label("",Label.CENTER);
		labelMesg2.setFont(commonFont);
		panelErrorMesgs.add(labelMesg2);
		
		// Et le dernier
		labelMesg3 = new Label("",Label.CENTER);
		labelMesg3.setFont(commonFont);
		panelErrorMesgs.add(labelMesg3);
		
		// Le panel qui contient les boutons de commande
		panelButton = new Panel();
		panelButton.setLayout(new FlowLayout(FlowLayout.CENTER,80,5));
		panelErrorMesgs.add(panelButton);

		// On ferme la la fenÃªtre qui contient notre instance
		buttonCloseWindow = new Button();
		buttonCloseWindow.setLabel("Close Window");
		buttonCloseWindow.setFont(commonFont);
		panelButton.add(buttonCloseWindow);
		
		// On montre ou cache les dÃ©tails
		buttonDetails = new Button();
		buttonDetails.setLabel("Show Details >>");
		buttonDetails.setFont(commonFont);
		panelButton.add(buttonDetails);
		
		// Les Ã©ventuels dÃ©tails de l'erreur
		details = new TextArea();
		//add("Center", details);
        
        // La gestion des actions
        
        // On affiche ou on cache les dÃ©tails de l'erreur
        buttonDetails.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    Button button = (Button) event.getSource();
                    String label = button.getLabel();
                    Dimension size = window.getSize();
                    
                    if (label.equals("Show Details >>")) {
                        button.setLabel("Hide Details <<");
                        MainPanelError.this.add("Center", details);
                        size.height += 250;
                    }
                    else {
                        button.setLabel("Show Details >>");
                        MainPanelError.this.remove(details);
                        size.height -= 250;
                    }
                    
                    window.setSize(size);
                    window.validate();
                }
            }
        );
        
        // On ferme le fenÃªtre d'erreur
        buttonCloseWindow.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    window.setVisible(false);
                }
            }
        );
    }
    
    /**
    * Sets the first of the three messages that can be displayed
    * @param mesg the error message
    */
    public void setMesg1(String mesg) {
        labelMesg1.setText(mesg);
    }
    
    /**
    * Sets the second of the three messages that can be displayed
    * @param mesg the error message
    */
    public void setMesg2(String mesg) {
        labelMesg2.setText(mesg);
    }
    
    /**
    * Sets the third of the three messages that can be displayed
    * @param mesg the error message
    */
    public void setMesg3(String mesg) {
        labelMesg3.setText(mesg);
    }
    
    /**
    * Sets the details of the error
    * @param theDetails the details of the error
    */
    public void setDetails(String theDetails) {
        details.setText(theDetails);
    }
}

