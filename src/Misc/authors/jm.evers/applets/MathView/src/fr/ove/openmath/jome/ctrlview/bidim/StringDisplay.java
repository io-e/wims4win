/*
$Id: StringDisplay.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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


package fr.ove.openmath.jome.ctrlview.bidim;

import java.awt.*;
import java.awt.event.*;
import java.text.*;
import fr.ove.utils.NumberUtils;
import fr.ove.openmath.jome.ctrlview.bidim.StringLayout;
import fr.ove.openmath.jome.ctrlview.bidim.Display;
import fr.ove.openmath.jome.ctrlview.bidim.GraphicContext;
import fr.ove.openmath.jome.model.*;
import fr.ove.openmath.jome.model.events.ModelEvent;

/**
* The display for variables and numbers.
*
* @author © 1999 DIRAT Laurent
* @version 2.0  04/08/1999
*/
public class StringDisplay extends AbstractStringDisplay {
    /**
    * The constructor.
    * @param graphicContext the graphic context of the display.
    * @param string the string to display
    * @param isSymbolOperatorDisplay <CODE>true</CODE> if the instance is the display of an operator.
    * <CODE>false</CODE> otherwise.
    */
    public StringDisplay(GraphicContext graphicContext, String string, boolean isSymbolOperatorDisplay ) {
        super(graphicContext, string);
        setIsSymbolOperatorDisplay(isSymbolOperatorDisplay);
        StringLayout layout = new StringLayout();
        layout.initDisplay(this);
        setLayout(layout);
        
        addMouseListener(
            new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    if (!isSymbolOperatorDisplay()) {
                        if (getListener() instanceof VariableOrNumber) {
                            VariableOrNumber listener = (VariableOrNumber) getListener();
                            if (listener.isNumber() && !listener.isInteger()) {
                                // On a affaire à un float
                                String value = listener.getValue();
                                
                                //if (!value.equals(StringDisplay.this.getString())) {
                                if (!value.equals(getString())) {
                                    setString(value);
                                    invalidate();
                                    computeAncestorsAttributes();
                                    Container container = StringDisplay.this;
                                    while (container.getParent() != null)
                                        container = container.getParent();
                                        
                                    container.validate();
                                    container.repaint();
                                }
                            }
                        }
                    }
                }
                
                public void mouseExited(MouseEvent e) {
                    if (!isSymbolOperatorDisplay()) {
                        if (getListener() instanceof VariableOrNumber) {
                            VariableOrNumber listener = (VariableOrNumber) getListener();
                            if (listener.isNumber() && !listener.isInteger()) {
                                // On a affaire à un float
                                String value = listener.getValue();
                                String newString = NumberUtils.formatDouble(value);
                                
                                if (!newString.equals(value)) {
                                    setString(newString);
                                    StringDisplay.this.invalidate();
                                    StringDisplay.this.computeAncestorsAttributes();
                                    Container container = StringDisplay.this;
                                    while (container.getParent() != null)
                                        container = container.getParent();
                                        
                                    container.validate();
                                    container.repaint();
                                }
                            }
                        }
                    }
                }
            }
        );
    }
    
    /**
    * Consumes (i.e. treats) the event received from the model.
    * @param modelEvent the event to consume.
    */
    public void consumeModelEvent(ModelEvent modelEvent) {
        // En principe, c'est le seul événement que doit recevoir ce type de display
        // De plus, forcément, le display représenté n'est pas le display d'un opérateur.
        switch (modelEvent.getAction()) {
            case ModelEvent.UPDATE :
                //System.out.println("ModelEvent.UPDATE : on update le StringDisplay");
                VariableOrNumber src = (VariableOrNumber) modelEvent.getSource();
                
                // On met à jour le display.
                // Ca c'est pour la troncature de l'affichage d'une nombre flottant dépassant
                // 2 chiffres après la virgule
                if (!isSymbolOperatorDisplay()) {
                    if (src.isNumber() && !src.isInteger())
                        // On a affaire à un float
                        setString(NumberUtils.formatDouble(src.getValue()));
                    else
                        setString(src.getValue());
                }
                else
                    setString(src.getValue());

                computeAncestorsAttributes();
                invalidate();
        }
    }
}