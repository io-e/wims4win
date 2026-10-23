/*
$Id: RequestLayout.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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
import java.util.*;
import fr.ove.openmath.jome.ctrlview.bidim.Display;
import fr.ove.openmath.jome.ctrlview.bidim.DisplayLayout;
import fr.ove.openmath.jome.ctrlview.bidim.selection.events.SelectionEvent;
import fr.ove.openmath.jome.model.*;

/**
*
* @author © 1999 DIRAT Laurent
* @version 2.0 17/12/1999
*/
public class RequestLayout extends HorizontalLayout {
    /**
    * Do we need to insert separator display ?
    */
    private boolean insertSeparatorDisplay = true;
    
    /**
    * The number of the components al ready present before an other insertion
    * or removal. This is necessary to keep in mind this, in order to know if 
    * we must parse the list of components to check if we have to insert symbol
    * display or not.
    */
    private int prevNbComponent = 0;
    
    private String opeSymbol;
    
    private Display displaySlot = null;
    
    /**
    * Inserts, if necessary, a display of the operator symbol that the display
    * laid by the instance represents.
    */
    public void insertOperatorDisplay() {
        Display current, next;
        FormulaTreeStructure fatherFts = (FormulaTreeStructure) displayToLay.getListener();
        FormulaTreeStructure fts;
        StringDisplay stringDisplay;
        int nbComponent = displayToLay.getComponentCount();
        
        if (nbComponent > 1) {
            if (fatherFts.getResourceIdentifier().equals("request"))
                opeSymbol = new String("  with  ");
            else
                opeSymbol = new String("  if  ");
                
            stringDisplay = new StringDisplay(displayToLay.getGraphicContext(), opeSymbol, true);
            stringDisplay.addControlListener(fatherFts);
            displayToLay.add(stringDisplay, stringDisplay, 1);
            nbComponent++;
        } 
        
        // Mise à jour du nombre de components présents dans le display.
        prevNbComponent = displayToLay.getComponentCount();
        // On n'a plus besoin d'insérer des displays d'opérateur.
        insertSeparatorDisplay = false;
        // On a rajouté un display, on demande le recalcul de tous les ancêtres
        // de l'instance.
        displayToLay.computeAncestorsAttributes();
    }
    
    /**
    * Checks the validity of the selection.
    */
    public void validateSelection() {
    }
    
    /**
    * Checks the validity of the deselection.
    * @param display the display to deselect.
    */
    public void validateDeselection(Display display) {
    }
    
    /**
    * Computes the size of the display according to its children size (if any),
    * and its different attributes.
    * @return the size of the display.
    */
    public Dimension computeAttributes() {
        int ascent = 0;
        int descent = 0;
        
        //Always add the container's insets!
        Insets insets = displayToLay.getParent().insets();
        int width = insets.left + insets.right;
        int height = insets.top + insets.bottom;
        
        Display tmp;
        
        int count = displayToLay.getComponentCount();
        Slot slot;
        Constant cte;
        int stop = 2;
        
        if (count == 1) {
            if (displaySlot != null) {
                slot = (Slot) displaySlot.getListener();
                if (slot.getChild(0) instanceof Constant) {
                    cte = (Constant) slot.getChild(0);
                    if (!cte.isTemplate()) {
                        displayToLay.add(displaySlot);
                        stop = 2;
                    }
                }
                else
                    stop = 1;
            }
        }
        else if (count == 2) {
            displaySlot = (Display) displayToLay.getComponent(1);
            slot = (Slot) displaySlot.getListener();
            if (slot.getChild(0) instanceof Constant) {
                cte = (Constant) slot.getChild(0);
                if (cte.isTemplate()) {
                    displayToLay.remove(displaySlot);
                    stop = 1;
                }
            }
            else 
                stop = 2;
        }
        
        
        // On regarde si le nombre de component dans le display a chnagé depuis
        // la dernière fois. Si oui, il faut contrôler s'il ne faut pas ajouter
        // des display d'opérateur.
        if (insertSeparatorDisplay || (prevNbComponent != ((Display) displayToLay).getComponentCount()))
            insertOperatorDisplay();
        
        updateLevel(displayToLay.getLevel());
        
        count = displayToLay.getComponentCount();
        stop = (count > 1) ? 2 : 1;
        
        // Ca va de la condition au with ou if selon le type
        for (int i = 0; i < stop; i++) {
            tmp = (Display) displayToLay.getComponent(i);
            tmp.setSize(tmp.getPreferredSize());
                    
            width += tmp.getWidth() + tmp.getShiftX();
                    
            ascent = Math.max(tmp.getAscent() - tmp.getShiftY(),
                              ascent);
                      
            descent = Math.max(tmp.getDescent() + tmp.getShiftY(),
                               descent);
                                
        }
        
        
        if (count == 3) {
            tmp = (Display) displayToLay.getComponent(2);
            tmp.setSize(tmp.getPreferredSize());
            height = (int) Math.max(((Display) displayToLay.getComponent(0)).getHeight(), 
                                    ((Display) displayToLay.getComponent(2)).getHeight());
            width += tmp.getWidth() + tmp.getShiftX();
            displayToLay.setAscent(Math.max( ((Display) displayToLay.getComponent(0)).getAscent(), 
                                             ((Display) displayToLay.getComponent(2)).getAscent() ));
        }
        else {
            height = ((Display) displayToLay.getComponent(0)).getHeight();
            int shiftY = ((Display) displayToLay.getComponent(0)).getDescent();
            //count = displayToLay.getComponentCount();
            for (int i = 2; i < count; i++) {
                tmp = (Display) displayToLay.getComponent(i);
                tmp.setSize(tmp.getPreferredSize());
                
                if (i == 2) {
                    if (count > 3) {
                        // On fait en sorte que cet élément commence à 20 pixels du bord gauche du display le contenant
                        tmp.setShiftX(-width + 20 );
                        shiftY += tmp.getAscent()+ 10;
                        tmp.setShiftY(shiftY);
                    }
                    else
                        width += tmp.getWidth();
                }
                else {
                    tmp.setShiftX(-((Display) displayToLay.getComponent(i-1)).getWidth());
                    shiftY += ((Display) displayToLay.getComponent(i-1)).getDescent() + tmp.getAscent()+ 5;
                    tmp.setShiftY(shiftY);
                }
                
                width = Math.max(width, 20 + tmp.getWidth());
                
                if (i == (count - 1))
                    height = shiftY + tmp.getHeight();
            }
            
            displayToLay.setAscent(Math.max( 0, ((Display) displayToLay.getComponent(0)).getAscent() ));
        }
        
        displayToLay.setSize(width , height);
        displayToLay.setDescent(height - displayToLay.getAscent());
        
        displayToLay.setComputeAttributes(false);
        
        return new Dimension(width, height);
    }

    /*
     * Lays out the container in the specified panel.
     * @param parent the component which needs to be laid out 
     */
     public void layoutContainer(Container parent) {
        // On regarde si le nombre de component dans le display a chnagé depuis
        // la dernière fois. Si oui, il faut contrôler s'il ne faut pas ajouter
        // des display d'opérateur.
        if (insertSeparatorDisplay || (prevNbComponent != ((Display) parent).getComponentCount()))
            insertOperatorDisplay();
        
        super.layoutContainer(parent);
    }
    

    /**
    * The display needs to be rebuilt. We do this.
    */
    public void rebuildDisplay() {
        Display tmp;
        int nbDisplay = ((FormulaTreeStructure) displayToLay.getListener()).getNbChildren();
        Display listDisplay[] = new Display[nbDisplay];
        
        for (int i = 0; i < displayToLay.getComponentCount(); i++) {
            tmp = (Display) displayToLay.getComponent(i);
            if (!tmp.isSymbolOperatorDisplay()) {
                // A voir !!!!!
                // Mais il semblerai que bon, sinon à la (re)construction du display il se base
                // sur des anciennes valeurs, et donc pas tarrible niveau affichage.
                tmp.setLocation(0, 0);
                listDisplay[((FormulaTreeStructure) tmp.getListener()).getRank()] = tmp;
            }
        }
        
        // ATTENTION : ici, on enlève les displays fils de display, mais on ne les enlève pas de la liste
        // des listeners de la fts qu'il sont en train d'écouter. Exception faite pour les displays d'opérateur.
        displayToLay.removeAllDisplays();
        
        for (int i = 0; i < nbDisplay; i++)
            displayToLay.add(listDisplay[i]);
            
        // On a reconstruit le display, il faut maintenant insérer les displays d'opérateur.
        insertSeparatorDisplay = true;
    }
   
}
