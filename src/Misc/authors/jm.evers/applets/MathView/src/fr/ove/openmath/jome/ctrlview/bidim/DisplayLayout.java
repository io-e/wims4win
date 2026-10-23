/*
$Id: DisplayLayout.java,v 1.3 2003/02/18 11:48:46 sander Exp $
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
import java.io.Serializable;
import java.lang.IllegalArgumentException;
import fr.ove.openmath.jome.ctrlview.bidim.Display;
import fr.ove.openmath.jome.ctrlview.bidim.selection.events.SelectionEvent;

/**
* A layout manager.
*
* @author © 1999 DIRAT Laurent
* @version 2.0 13/12/1999
*/
public abstract class DisplayLayout implements LayoutManager2, Serializable , Cloneable {
    /**
    * The display laid out by the instance.
    */
    protected Display displayToLay = null;
    
    
    /**
    * According to the operator, the layout manager has to add some components (e.g. brackets, ...)
    * or has to perform some "re-oganisation" before rendering.<BR>
    * As soon as the layout manager is set to the display, this mehtod MUST be called with the display laid out
    * as parameter. This method serves as well as a registering method. So all sub-classes of the instance MUST
    * call super.initDisplay(displayToLay).
    * @param displayToLay the display laid by the instance
    */
    public void initDisplay(Display displayToLay) {
        this.displayToLay = displayToLay;
    }
    
    
    /**
    * Returns <CODE>true</CODE> if during the selection, the display which is located
    * to the left of the display that the instance lays, must be selected too.
    * <CODE>false</CODE> otherwise.
    */
    public boolean selectLeftDisplay() {
        return false;
    }
    
    /**
    * Returns <CODE>true</CODE> if during the selection, the display which is located
    * to the right of the display that the instance lays, must be selected too.
    * <CODE>false</CODE> otherwise.
    */
    public boolean selectRightDisplay() {
        return false;
    }
    
    /**
    * Selects the display (and its children if any)
    * @param the display to select.
    */
    public void selectDisplay() {
        displayToLay.select();
        
        SelectionEvent selEvt = new SelectionEvent(displayToLay);
        // On purge la liste des éléments sélectionnés.
        selEvt.setAction(SelectionEvent.PURGE, null);
        displayToLay.fireSelectionEvent(selEvt);
        selEvt.setAction(SelectionEvent.ADD, displayToLay);
        displayToLay.fireSelectionEvent(selEvt);
        
        Display display = displayToLay;
        if (displayToLay.getParent() instanceof Display) {
            display = (Display) displayToLay.getParent();
            ((DisplayLayout) display.getLayout()).validateSelection();
        }
    }


    /**
    * Deselects the display.
    * @param the display to deselect.
    */
    public void deselectDisplay() {
        displayToLay.deselect();

        SelectionEvent selEvt = new SelectionEvent(displayToLay);
        selEvt.setAction(SelectionEvent.REMOVE, displayToLay);
        displayToLay.fireSelectionEvent(selEvt);
        
        if (displayToLay.getParent() instanceof Display) {
            Display display = (Display) displayToLay.getParent();
            ((DisplayLayout) display.getLayout()).validateDeselection(displayToLay);
        }
    }
    
    
    /**
    * Updates the level of the display laid out.
    * @param display the display laid out
    * @param level the level put to the display.
    */
    public void updateLevel(int level) {
        Display tmp;
        
        if (displayToLay.getUpdateLevel()) {
            // On met le niveau adéquat
            displayToLay.setLevel(level);
            // On scale le display en fonction du niveau
            displayToLay.scaleDisplay();
            // On n'a plus besoin de mettre à jour le niveau de l'instance
            displayToLay.setUpdateLevel(false);
            // Le comportement par défaut est que les displays enfant de l'instance courante
            // ont le même niveau que leur père.
            int count = displayToLay.getComponentCount();
            for (int i = 0; i < count; i++) {
                tmp = (Display) displayToLay.getComponent(i);
                ((DisplayLayout) tmp.getLayout()).updateLevel(level);
            }
        }
    }
    
    
    // ############################################
    // ### Les différentes méthodes abstraites  ###
    // ############################################
    
    
    /**
    * Checks the validity of the selection.
    */
    public abstract void validateSelection();
    
    /**
    * Checks the validity of the deselection.<BR>
    * @param display the display which just has been deselected.
    */
    public abstract void validateDeselection(Display display);
    
    /**
    * The display needs to be rebuilt. We do this.
    */
    public abstract void rebuildDisplay();
    
    /**
    * Computes the size of the display according to its children size (if any),
    * and its different attributes.
    * @param display the display laid by the instance
    * @return the size of the display.
    */
    public abstract Dimension computeAttributes();
    
    // #################################################
    // ### Implémentation des différentes interfaces ###
    // #################################################
    
    // ***************************************************
    // *** Implémentation de l'interface LayoutManager ***
    
    /**
     * Adds the specified component with the specified name to
     * the layout.
     * @param name the component name
     * @param comp the component to be added
     */
    public void addLayoutComponent(String name, Component comp) {
    }

    /**
     * Removes the specified component from the layout.
     * @param comp the component ot be removed
     */
    public void removeLayoutComponent(Component comp) {
    }

    /** 
     * Calculates the minimum size dimensions for the specified 
     * panel given the components in the specified parent container.
     * @param parent the component to be laid out
     * @see #preferredLayoutSize
     */
    public Dimension minimumLayoutSize(Container parent) {
        return preferredLayoutSize(parent);
    }
    
    /**
     * Calculates the preferred size dimensions for the specified 
     * panel given the components in the specified parent container.
     * @param parent the component to be laid out
     *  
     * @see #minimumLayoutSize
     */
    public Dimension preferredLayoutSize(Container parent) {
        if (((Display) parent).getComputeAttributes())
            return computeAttributes();
        else
            return parent.getSize();
    }
    
    // *** Fin de l'interface LayoutManager ***
    // ****************************************
    
    
    // ****************************************************
    // *** Implémentation de l'interface LayoutManager2 ***

    /**
     * Adds the specified component to the layout, using the specified
     * constraint object.
     * @param comp the component to be added
     * @param constraints  where/how the component is added to the layout.
     */
    public void addLayoutComponent(Component comp, Object constraints) {
	    if (!(constraints instanceof Display))
    		throw new IllegalArgumentException("The component to add must be a Display instance !!!");
    }

    /** 
     * Returns the maximum size of this component.
     * @see java.awt.Component#getMinimumSize()
     * @see java.awt.Component#getPreferredSize()
     * @see LayoutManager
     */
    public Dimension maximumLayoutSize(Container target) {
        return preferredLayoutSize(target);
    }

    /**
     * Returns the alignment along the x axis.  This specifies how
     * the component would like to be aligned relative to other 
     * components.  The value should be a number between 0 and 1
     * where 0 represents alignment along the origin, 1 is aligned
     * the furthest away from the origin, 0.5 is centered, etc.
     */
    public float getLayoutAlignmentX(Container target) {
        return 0.0f;
    }

    /**
     * Returns the alignment along the y axis.  This specifies how
     * the component would like to be aligned relative to other 
     * components.  The value should be a number between 0 and 1
     * where 0 represents alignment along the origin, 1 is aligned
     * the furthest away from the origin, 0.5 is centered, etc.
     */
    public float getLayoutAlignmentY(Container target) {
        return 0.0f;
    }

    /**
     * Invalidates the layout, indicating that if the layout manager
     * has cached information it should be discarded.
     */
    public void invalidateLayout(Container target) {
    }
    
    // *** Fin de l'interface LayoutManager ***
    // ****************************************
}

