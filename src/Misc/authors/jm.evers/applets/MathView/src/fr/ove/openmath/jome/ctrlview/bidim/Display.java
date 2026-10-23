/*
$Id: Display.java,v 1.3 2003/02/18 11:48:46 sander Exp $
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
import java.util.*;
import fr.ove.openmath.jome.model.*;
import fr.ove.openmath.jome.model.events.*;
import fr.ove.openmath.jome.behaviour.*;
import fr.ove.openmath.jome.ctrlview.events.*;
import fr.ove.openmath.jome.ctrlview.bidim.*;
import fr.ove.openmath.jome.ctrlview.bidim.selection.*;
import fr.ove.openmath.jome.ctrlview.bidim.selection.events.*;

/**
* The display elements of the formula.
*
* @author © 1999 DIRAT Laurent
* @version 2.0  27/06/1999
*/
public abstract class Display extends Container implements Displayable, Colorizable, Selectable, Iconifiable, ModelListenerController {
    /**
    * Distance between the top of the bounding box of the element to display
    * and its baseline.
    */
    private int ascent;

    /**
    * Distance between the bottom of the bounding box of the element to display
    * and its baseline.
    */
    private int descent;

    /**
    * An horizontal shift.
    */
    private int shiftX = 0;

    /**
    * A vertical shift.
    */
    private int shiftY = 0;

    /**
    * Indicates whether the symbol is selected or not.
    */
    protected boolean isSelected = false;
    
    /**
    * Indicates the level of reduction for the font.
    * (Usefull for superscripts, underscripts, ....)
    */
    private int level = 0;
    
    /**
    * Indicates if the level needs to be updated.
    */
    private boolean updateLevel = true;
    
    /**
    * Indicates if we draw the bounds of the display or not.
    */
    private boolean weDrawBounds = false;
    
    /**
    * The graphic context of the display.
    */
    private GraphicContext graphicContext;
    
    /**
    * Indicates if the display need to be computed.
    */
    private boolean computeAttributes = true;
    
    /**
    * The rank of the container in the set of children of its parent.
    */
    private int rank = 0;
    
    /**
    * The selection manager of the display.
    */
    private static SelectionEventListener selectionManager = null;
    
    /**
    * The list of listeners of the instance
    */
    private ControlListener listener;
    
    /**
    * Are we in drag'n drop mode ??
    */
    protected boolean dragNDrop;
    
    /**
    * To indicate if the instance represents the display of the symbol of an operator.<BR>
    * For example, the display of the operator "+", display that is automatically inserted
    * during the rendering of the formula.
    */
    private boolean isSymbolOperatorDisplay = false;
    
    /**
    * To indicate if, when the instance is removed from its father, it has to be removed from the
    * list of listeners it is listening to.<BR>
    * The default is <CODE>true<CODE>. In fact, it is in special cases the value equals to <CODE>false<CODE>.
    * These special cases are mainly moves of the instance in its father (e.g. add(comp, comp, index) calls
    * a remove(...) method, which automatically removes the instance from the list of listeners it is listening
    * to. And we don't want that)
    */
    private boolean doRemoveFromListListeners = true;
    
    
    /**
    * The display allocator for the building of the visualisation of the formula.<BR>
    * REMINDER : Don't forget to set the right allocator in the constructor of the inherited class.
    */
    private static DisplayAllocator displayAllocator = null;
    
    /**
    * The constructor.
    * @param graphicContext the graphic context of the display.
    */
    public Display(GraphicContext graphicContext) {
        super();
        
        this.graphicContext = new GraphicContext(graphicContext);
        setFont(graphicContext.getFont());
        setForeground(graphicContext.getForegroundColor());
        setBackground(graphicContext.getBackgroundColor());
        
        addMouseListener( 
            new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    SelectionEvent selEvt = new SelectionEvent(Display.this);
                    if (e.isControlDown()) {
                        if (!isSelected) {
                            DisplayLayout displayLayout = (DisplayLayout) getLayout();
                            
                            if (displayLayout instanceof OneFormulaLayout) {
                                Display formulaDisplay = (Display) getComponent(0);
                                if (formulaDisplay.isSelected())
                                    displayLayout.deselectDisplay();
                                else {
                                    displayLayout.selectDisplay();
                                    setCursor(new Cursor(Cursor.HAND_CURSOR));
                                }
                            }
                            else {
                                displayLayout.selectDisplay();
                                setCursor(new Cursor(Cursor.HAND_CURSOR));
                            }

                            repaint();
                        }
                        else {
                            ((DisplayLayout) getLayout()).deselectDisplay();
                            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

                            repaint();
                        }
                    }
                    else if (e.isShiftDown())
                        iconify();
                    else if (e.isAltDown())
                        uniconify();
                }
                
                public void mouseEntered(MouseEvent e) {
                    if (isSelected) 
                        setCursor(new Cursor(Cursor.HAND_CURSOR));
                    else
                        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
            
                public void mouseReleased(MouseEvent e) {
                    int mouseX = e.getX();
                    
                    if (dragNDrop) {
                        moveSelectedDisplays(mouseX);
                        dragNDrop = false;
                        repaint();
                    }
                }

            }
        );
        
        addMouseMotionListener(
            new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    if (isSelected) {
                        dragNDrop = true;
                        setCursor(new Cursor(Cursor.MOVE_CURSOR));
                    }
                }
                
                public void mouseMoved(MouseEvent e) {
                    if (isSelected) 
                        setCursor(new Cursor(Cursor.HAND_CURSOR));
                    else
                        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
            }
        );
	}
	
	
	
    // *******************
    // Display information
    // *******************
    
    
    /**
    * Returns <CODE>true</CODE> if the instance is a display of the specified
    * formula tree structure. <CODE>false</CODE> otherwise.
    */
    public boolean isDisplay(FormulaTreeStructure fts) {
        // On recupère la liste de tous les listeners de fts
        Vector listeners = fts.getListeners();
        
        return listeners.contains(this);
    }
    
	
    // ***************************************
    // About the display as one of an operator
    // ***************************************
    
    /**
    * Sets if the instance represents the display of the symbol of an operator.
    * @param isSymbolOperatordisplay <CODE>true</CODE> if the instance represents 
    * the display of the symbol of an operator. <CODE>false</CODE> otherwise.
    */
    public void setIsSymbolOperatorDisplay(boolean isSymbolOperatorDisplay) {
        this.isSymbolOperatorDisplay = isSymbolOperatorDisplay;
    }
    
    /**
    * @return <CODE>true</CODE> if the instance represents the display of the symbol
    * of an operator. <CODE>false</CODE> otherwise.
    */
    public boolean isSymbolOperatorDisplay() {
        return isSymbolOperatorDisplay;
    }
    
    
    // *************************
    // The bounds of the display
    // *************************
    
    /**
    * The bounds of the display have to be drawn.
    */
    public void drawBounds() {
        weDrawBounds = true;
    }
    
    /**
    * The bounds of the display don't have to be drawn.
    */
    public void dontDrawBounds() {
        weDrawBounds = false;
    }
    
    /**
    * Checks if the bound must be drawn or not.
    * @return <CODE>true</CODE> if we have to draw the bounds of the 
    * display. <CODE>false</CODE> otherwise.
    */
    public boolean weDrawBounds() {
        return weDrawBounds;
    }
    
    // ************************
    // The level of the display
    // ************************
    
    /**
    * Sets the level of the display
    * @param level the level of the display.
    */
    public void setLevel(int level) {
        this.level = level;
    }
    
    /**
    * Returns the level of the display
    */
    public int getLevel() {
        return level;
    }

    /**
    * Increments the level value.
    */
    public void incLevel() {
        level++;
    }
    
    /**
    * Decrements the level value.
    */
    public void decLevel() {
        level--;
    }
    
    /**
    * Sets the level to be updated.
    * @param updateLevel <CODE>true</CODE> if the display level need to be
    * updated. <CODE>false</CODE> otherwise.
    */
    public void setUpdateLevel(boolean updateLevel) {
        this.updateLevel = updateLevel;
        if (updateLevel) {
            int count = getComponentCount();
            for (int i = 0; i < count; i++)
                ((Display) getComponent(i)).setUpdateLevel(updateLevel);
        }
    }
    
    /**
    * Checks if we need to update the level of the display.
    * @return <CODE>true</CODE> if needed. <CODE>false</CODE> otherwise.
    */
    public boolean getUpdateLevel() {
        return updateLevel;
    }
    
    /**
    * Recurses into the tree of the display to inform the ancestors of the instance
    * that they need to update their level
    */
    public void updateAncestorsLevel() {
        Container parent = getParent();
        while (parent instanceof Display) {
            ((Display) parent).setUpdateLevel(true);
            parent = parent.getParent();
        }
    }
    
    /**
    * Recurses into the tree of the display to inform the children of the instance
    * that they need to update their level
    */
    public void updateChildrenLevel() {
        Display childDisplay;
        int count = getComponentCount();
        for (int i = 0; i < count; i++) {
            childDisplay = (Display) getComponent(i);
            childDisplay.setUpdateLevel(true);
            childDisplay.updateChildrenLevel();
        }
    }
    
    
    // ************************************
    // The management of display attributes 
    // ************************************
    
    
    /**
    * Sets the attributes to be computed.
    * @param computeAttributes <CODE>true</CODE> if the display attributes need to be
    * computed. <CODE>false</CODE> otherwise.
    */
    public void setComputeAttributes(boolean computeAttributes) {
        this.computeAttributes = computeAttributes;
    }

    /**
    * Checks if we need to compute the display.
    * @return <CODE>true</CODE> if needed. <CODE>false</CODE> otherwise.
    */
    public boolean getComputeAttributes() {
        return computeAttributes;
    }
    
    /**
    * Recurses into the tree of the display to inform the ancestors of the instance
    * that they need to compute their attributes.
    */
    public void computeAncestorsAttributes() {
        computeAttributes = true;
        Container parent = getParent();
        while (parent instanceof Display) {
            ((Display) parent).setComputeAttributes(true);
            parent = parent.getParent();
        }
    }
    
    /**
    * Recurses into the tree of the display to inform the children of the instance
    * that they need to compute their attributes.
    */
    public void computeChildrenAttributes() {
        computeAttributes = true;
        int count = getComponentCount();
        for (int i = 0; i < count; i++)
            ((Display) getComponent(i)).computeChildrenAttributes();
    }
    
    
    // ***********************
    // The rank of the display
    // ***********************
    
    
    /**
    * Sets the rank of the display.
    * @param rank the rank.
    */
    public void setRank(int rank) {
        this.rank = rank;
    }
    
    /**
    * Returns the rank of the display.
    */
    public int getRank() {
        return rank;
    }
    
    /**
    * Adjusts the rank of its children.
    */
    public void adjustRank() {
        int count = getComponentCount();
        for (int i = 0; i < count; i++)
            ((Display) getComponent(i)).setRank(i);
    }
    
    // *********************
    // Display manipulations
    // *********************
    
    /**
    * Removes all the displays of the instance. <BR>
    * The fundamental difference with removeAll is in that case, the displays removed are not
    * removed form the list of listeners of the formula tree structure they are listening to.
    */
    public void removeAllDisplays() {
        Display display;
        int count = getComponentCount();
        for (int i = 0; i < count; ) {
            display = (Display) getComponent(i);
            // Si c'est le display d'un opérateur, alors on l'enlève de la liste et en plus, on l'enlève
            // de la liste des listeners de la fts qu'il écoutait.
            // Le remove surchargé.
            // si ce n'est pas le cas, on ne fait que le supprimer des fils de l'instance.
            if (display.isSymbolOperatorDisplay)
                //remove(display);
                remove(i);
            else
                //super.remove(display);
                super.remove(i);
                
            count--;
        }
    }
    
    /**
    * Scales the display.
    */
    public void scaleDisplay() {
        setTheFont(graphicContext.scaleFont(level));
        setComputeAttributes(true);
        
        int count = getComponentCount();
        for (int i = 0; i < count; i++)
            ((Display) getComponent(i)).scaleDisplay();
        
        // On regarde si le display de l'instance est le display d'une icone.
        // Si tel est le cas, il faut que l'on scale les displays iconifiés.
        FormulaTreeStructure fts = (FormulaTreeStructure) getListener();
        if ((fts != null) && fts.isIcon()) {
            SubstitutedDisplayManager layout = (SubstitutedDisplayManager) getLayout();
            count = layout.getNbSubstitutedDisplay();
            for (int i = 0; i < count; i++)
                layout.getSubstitutedDisplay(i).scaleDisplay();
        }
    }
    
    
    /**
    * Moves the selected displays to the mouse position.
    * @param mouseX the mouse position.
    */
    public void moveSelectedDisplays(int mouseX) {
        SelectionEvent selectionEvent = new SelectionEvent(this);

        // On récupère la taille de la sélection.
        Integer selectionSize = null;
        selectionEvent.setAction(SelectionEvent.GET_SELECTION_SIZE, selectionSize);
        fireSelectionEvent(selectionEvent);
        selectionSize = (Integer) selectionEvent.getArgument();
        // Si différente de 0, on peut faire du drag'n drop de qque chose.
        if (selectionSize.intValue() != 0) {
            // On récupère la sélection entière.
            Vector selection = null;
            selectionEvent.setAction(SelectionEvent.GET_SELECTION, selection);
            fireSelectionEvent(selectionEvent);
            selection = (Vector) selectionEvent.getArgument();
            Display display = (Display) selection.elementAt(0);
            // On récupère le fts associé au display.
            FormulaTreeStructure fts = (FormulaTreeStructure) display.getListener();
            // On récupère l'opérateur dont les display sont les opérandes.
            fts = (FormulaTreeStructure) fts.getFather();
            // On fonction du type de cet opérateur, on permet ou pas le drag'n
            // drop.
            if (!fts.getAreOperandsMovable())
                return;  // Pour ces opérateur là, on ne permet pas le drag'n drop.

            // Il faut maintenant savoir où on va dropper la sélection.
            // On récupère le display de l'opérateur, et on cherche cette position
            display = (Display) display.getParent();
            
            // On ajuste la position de la souris pour qu'on soit en coordonnée relative
            // dans le repère de display.
            // ATTENTION : l'instance courante est celle qui a reçu le mouseDragged. Elle
            // a reçu également le mouseReleased. Ce qui fait que le mouseX est une coordonnée
            // relative à l'instance. Donc, pour ramener notre coordonnée relativement à display
            // il faut rajouter la coordonnée de l'instance.
            Display tmp = this;
            while (tmp != display) {
                mouseX += tmp.getX();
                tmp = (Display) tmp.getParent();
            }
            
            // On regarde si la position de la souris ainsi calculée est toujours contenue
            // dans le display. Si ce n'est pas le cas, on se trouve dans un cas particulier.
            int insertionPosition;
            if (mouseX <= 0) {
                // On a droppé à gauche de display, on insère donc au début de celui-ci
                // Cela revient à dropper sur le premier display de la liste.
                tmp = (Display) display.getComponentAt(1, display.getAscent());
                insertionPosition = tmp.computeInsertionPosition(1);
            }
            else if (mouseX >= display.getWidth()) {
                // On a droppé à droite de display, on insère donc à la fin de celui-ci
                // Cela revient dropper sur le dernier display de la liste.
                tmp = (Display) display.getComponentAt(display.getWidth() - 1, display.getAscent());
                insertionPosition = tmp.computeInsertionPosition(display.getWidth() - 1 - tmp.getX());
            }
            else {
                // On a droppé qque part dans display. On récupère ce display.
                tmp = (Display) display.getComponentAt(mouseX, display.getAscent());
                insertionPosition = tmp.computeInsertionPosition(mouseX - tmp.getX());
            }
            
            // On créé la liste des opérandes à permuter
            Vector operands = new Vector();
            for (Enumeration e = selection.elements(); e.hasMoreElements(); ) {
                tmp = (Display) e.nextElement();
                // Si l'on n'a pas affaire à un display d'operateur, alors on ajoute l'operande
                // dans la liste.
                if (!tmp.isSymbolOperatorDisplay()) {
                    tmp.deselect();
                    operands.addElement(tmp.getListener());
                }
            }
            
            // On fait la permutation
            // Faut-il rajouter l'événement pour rester dans la philosophie ???
            fts.moveOperands(operands, insertionPosition);
            
            // On purge la liste de sélection
            selectionEvent.setAction(SelectionEvent.PURGE, null);
            fireSelectionEvent(selectionEvent);
        }
    }

    /**
    * Returns the insertion position for the moving of displays.
    * @param mouseX the mouse position.
    */
    public int computeInsertionPosition(int mouseX) {
        int insertionPosition;
        
        // On regarde la position de la souris.
        // Si elle est inférieure à la moitié de la largeur de l'instance, alors
        // la position d'insertion est la position de l'opérande dont l'instance est
        // le display. Sinon, la position d'insertion est la position de l'opérande 
        // dont l'instance est le display + 1.
        if (mouseX <= (getWidth() / 2))
            insertionPosition = ((FormulaTreeStructure) listener).getRank();
        else
            insertionPosition = ((FormulaTreeStructure) listener).getRank() + 1;
            // Pas de pb de test pour savoir si on n'est pas sur le dernier opérande
            // puisqu'on est obligé dans ce cas de retourner le nbre réel + 1 pour dire
            // effectivement que l'on insère à la dernière position.

        // On retourne la position d'insertion calculée.
        return insertionPosition;
    }
    
    // ******************
    // Display allocation
    // ******************
    
    /**
    * Sets the display allocator.
    * @param displayAllocator the display allocator
    */
    public void setDisplayAllocator(DisplayAllocator displayAllocator) {
        this.displayAllocator = displayAllocator;
    }
    
    /**
    * Returns the display allocator.
    * @return the display allocator, or <CODE>null</CODE> if none has been specified.
    */
    public DisplayAllocator getDisplayAllocator() {
        return displayAllocator;
    }
    
    /**
    * Builds the display of the formula tree structure.<BR>
    * This method has to be called when a formula tree structure has been created and the 
    * displays have not been associated yet.<BR>
    * As a prerequesit, the instance which call this method MUST have a corresponding instance
    * (a listener) in the formula tree structure (i.e. the formula tree structure we want to display).<BR>
    */
    public void buildDisplay() {
        Display childDisplay;
        FormulaTreeStructure fts, ftsChild;
        int count;
        
        // On récupère l'élément de la FTS donc l'instance est le display.
        fts = (FormulaTreeStructure) getListener();
        if (fts != null) { // Bon malgrè le prérequis, on fait le test quand même
            count = fts.getNbChildren();
            
            // On parcourre la liste des fils de la fts, pour leur associer le display qui va bien
            for (int i = 0; i < count; i++) {
                ftsChild = (FormulaTreeStructure) fts.getChild(i);
                // Allocation du display
                childDisplay = displayAllocator.allocateDisplay(graphicContext, ftsChild);
                // Chacun écoute l'autre
                //childDisplay.addControlListener(ftsChild);
                //ftsChild.addModelListener(childDisplay);
                // On ajoute le nouveau display comme fils à l'instance
                add(childDisplay);
                // On descend dans la fts pour poursuivre la construction des display
                childDisplay.buildDisplay();
            }
        }
    }
    
    /**
    * Builds the display of the part of the formula tree structure at the specified index.<BR>
    * This method has to be called when a new part of the formula tree structure has been created
    * and the displays have not been associated yet. (modification of the formula expression, and then
    * we only ask to build displays of the new part, and only them)<BR>
    * As a prerequesit, the instance which calls this method MUST have a corresponding instance
    * (a listener) in the formula tree structure (i.e. the formula tree structure we want to display)
    * @param indexFts the index of the fts which needs displays to be associated, in the list of "sub-fts"
    * of the corresponding fts of the instance (ouf !!!).<BR>
    * <CODE>indexFts</CODE> must be less than the number of elements of the correponding fts, and
    * greater or equal than 0.
    * @return  the new display created.
    */
    public Display buildDisplay(int indexFts) {
        Display childDisplay = null;
        FormulaTreeStructure fts, ftsChild;
        int count;
        
        // On récupère l'élément de la FTS donc l'instance est le display.
        fts = (FormulaTreeStructure) getListener();
        if (fts != null) {
            count = fts.getNbChildren();
            if ((indexFts >= 0) && (indexFts < count)) {
                ftsChild = (FormulaTreeStructure) fts.getChild(indexFts);
                // Allocation du display
                childDisplay = displayAllocator.allocateDisplay(graphicContext, ftsChild);
                // Le display que l'on ajoute est au même niveau que celui du père
                // (rappel: niveau = nb de fois que la réduction de taille doit être appliquée,
                // par exemple pour les exposants.) C'est le layout manager qui s'occupe de
                // faire la réduction si nécessaire.
                childDisplay.setLevel(getLevel());
                // Chacun écoute l'autre
                childDisplay.addControlListener(ftsChild);
                ftsChild.addModelListener(childDisplay);
                // On ajoute le nouveau display comme fils à l'instance
                add(childDisplay);
                // On descend dans la fts pour poursuivre la construction des display
                childDisplay.buildDisplay();
            }
        }
        
        return childDisplay;
    }
    
    // ############################
    // ### Méthodes surchargées ###
    // ############################
    
    /**
    * Adds a child display to the instance. The child is added as the last child of the
    * set of children of the instance.
    * @param display the display to add.
    */
    public void add(Display display) {
        super.add(display, display);
        display.setRank(getComponentCount() - 1);
    }
    
    /**
    * Removes from the instance the display at the specified index.
    * @param index the specified index.
    */
    public void remove(int index) {
        try {
            Display display = (Display) getComponent(index);
            
            if (display.doRemoveFromListListeners())
                // On enlève maintenant le display supprimé de la liste des listeners de la fts
                // qu'il écoutait
                display.removeFromListListeners();
            
            // On enlève le display de la liste
            super.remove(index);
            
            // On réajuste maintenant le rank des fils de l'instance
            adjustRank();
        }
        catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Trying to remove display with a wrong index");
            e.printStackTrace();
        }
    }
    
    /**
    * Removes the specified display to the instance.
    * @param display the display to remove.
    *
    public void remove(Display display) {
        super.remove(display);
        // On enlève maintenant le display supprimé de la liste des listeners de la fts
        // qu'il écoutait
        display.removeFromListListeners();
        
        // On réajuste maintenant le rank des fils de l'instance
        adjustRank();
    }
    */
    
    /**
    * Removes all the displays of the instance.
    */
    public void removeAll() {
        /*
        for (int i = 0; i < getComponentCount(); i++)
            remove(i);
        */
        for (int i = 0; i < getComponentCount(); )
            remove(i);
            
        computeAncestorsAttributes();
    }
    
    /**
    * Sets if during the removal of the instance from the its father, we remove it from the list of
    * listeners it is listening to.
    * @param doRemoveFromListListeners <CODE>true<CODE> if we remove the instance from the list of listeners.
    * <CODE>false</CODE> otherwise.
    */
    protected void setDoRemoveFromListListeners(boolean doRemoveFromListListeners) {
        this.doRemoveFromListListeners = doRemoveFromListListeners;
    }
    
    /**
    * Returns <CODE>true<CODE> if we remove the instance from the list of listeners.
    * <CODE>false</CODE> otherwise.
    */
    protected boolean doRemoveFromListListeners() {
        return doRemoveFromListListeners;
    }
    
    /**
    * Sets if during the manipulation of children of the instance, the children manipulated have to be
    * removed from the list od listeners.
    * @param doRemoveFromListListeners <CODE>true<CODE> if we remove the children manipulated from the list
    * of listeners. <CODE>false</CODE> otherwise.
    */
    protected void removeFromListListeners(boolean doRemoveFromListListeners) {
        int count = getComponentCount();
        for (int i = 0; i < count; i++)
            ((Display) getComponent(i)).doRemoveFromListListeners = doRemoveFromListListeners;
    }
    
    /**
    * Removes the instance from the list of listeners of the formula tree structure it
    * is listening to.
    */
    private void removeFromListListeners() {
        // On récupère la fts associée au display enlevé et on lui enlève display
        // de la liste de ses listeners. 
        FormulaTreeStructure fts = (FormulaTreeStructure) getListener();
        fts.removeModelListener(this);
        removeControlListener(fts);
        
        // Il faut maintenant que l'on fasse la même chose avec les displays fils
        int count = getComponentCount();
        for (int i = 0; i < count; i++)
            ((Display) getComponent(i)).removeFromListListeners();
    }
    
        
    
    /**
    * Sets a new font to the context.
    * @param font the new font.
    */
    public void setFont(Font font) {
        setTheFont(font);
        
        graphicContext.setFont(font);
        
        int count = getComponentCount();
        // On parcourre tous les displays fils et ont leur affecte la nouvelle font.
        for (int i = 0; i < count; i++)
            ((Display) getComponent(i)).setFont(font);
            
        // On calcule la taille de la nouvelle font utilisée en fonction du niveau
        // du display.
        scaleDisplay();
            
        // On indique au layout manager que la taille du display (container) n'est plus
        // valide.
        invalidate();
        
        // On regarde si le display de l'instance est le display d'une icone.
        // Si tel est le cas, il faut que l'on mette la nouvelle font aux displays iconifiés.
        FormulaTreeStructure fts = (FormulaTreeStructure) getListener();
        if ((fts != null) && fts.isIcon()) {
            SubstitutedDisplayManager layout = (SubstitutedDisplayManager) getLayout();
            count = layout.getNbSubstitutedDisplay();
            for (int i = 0; i < count; i++)
                layout.getSubstitutedDisplay(i).setFont(font);
        }
    }
    
    protected void setTheFont(Font font) {
        super.setFont(font);
    }
    
    /**
    * Substitutes the selection by the specified friendly name.
    * @param the name for the substitution
    */
    public void substitute(String name) {
        SelectionEvent selectionEvent = new SelectionEvent(this);

        // On récupère la taille de la sélection.
        Integer selectionSize = null;
        selectionEvent.setAction(SelectionEvent.GET_SELECTION_SIZE, selectionSize);
        fireSelectionEvent(selectionEvent);
        selectionSize = (Integer) selectionEvent.getArgument();
        // Si différent de 0, il y a qque chose à substituer.
        if (selectionSize.intValue() != 0) {
            // On récupère la sélection entière.
            Vector selection = null;
            selectionEvent.setAction(SelectionEvent.GET_SELECTION, selection);
            fireSelectionEvent(selectionEvent);
            selection = (Vector) selectionEvent.getArgument();
            // On créé la liste des éléments à iconifier. (on enlève les displays
            // d'opérateurs parasites, et aux displays qui restent, on récupère
            // la fts dont ils sont le display).
            Display display;
            FormulaTreeStructure fts;
            Vector toIconify = new Vector();
            for (int i = 0; i < selectionSize.intValue(); i++) {
                display = (Display) selection.elementAt(i);
                if (!display.isSymbolOperatorDisplay()) {
                    // On désélectionne le display
                    display.deselect();
                    // On récupère le fts associé au display.
                    fts = (FormulaTreeStructure) display.getListener();
                    // On l'ajoute à la liste des fts à iconifier
                    toIconify.addElement(fts);
                }
            }
            // On purge la sélection
            selectionEvent.setAction(SelectionEvent.PURGE, null);
            fireSelectionEvent(selectionEvent);
            
            // On rajoute comme premier élément à toIconify le nom de la substitution
            toIconify.insertElementAt(name, 0);
            
            // On envoie l'événement au modèle pour lui dire qu'il faut faire la substitution
            ControlEvent controlEvent = new ControlEvent(this);
            controlEvent.setAction(ControlEvent.SUBSTITUTE, toIconify);
            fireControlEvent(controlEvent);
        }
    }
    
    
    // #################################################
    // ### Implémentation des différentes interfaces ###
    // #################################################
    
    // *************************************************
    // *** Implémentation de l'interface Displayable ***
    
    /**
    * Sets the ascent.
    * @param ascent the new ascent value.
    */
    public void setAscent(int ascent) {
        this.ascent = ascent;
    }

    /**
    * Returns the ascent.
    * @return the ascent.
    */
    public int getAscent() {
        return ascent;
    }

    /**
    * Sets the descent.
    * @param descent the new descent value.
    */
    public void setDescent(int descent) {
        this.descent = descent;
    }

    /**
    * Returns the descent.
    * @return the descent.
    */
    public int getDescent() {
        return descent;
    }
    
    /**
    * Sets the horizontal shift.
    * @param shift the new horizontal shift value.
    */
    public void setShiftX(int shiftX) {
        this.shiftX = shiftX;
    }

    /**
    * Returns the horizontal shift.
    * @return the horizontal shift.
    */
    public int getShiftX() {
        return shiftX;
    }

    /**
    * Sets the vertical shift.
    * @param shiftY the new vertical shift value.
    */
    public void setShiftY(int shiftY) {
        this.shiftY = shiftY;
    }

    /**
    * Returns the vertical shift.
    * @return the vertical shift.
    */
    public int getShiftY() {
        return shiftY;
    }
    
    /**
    * Sets all the attributes.
    * @param ascent the new ascent value.
    * @param descent the new descent value.
    * @param shiftX the new horizontal shift value.
    * @param shiftY the new vertical shift value.
    */
    public void setAttributes(int ascent, int descent, int shiftX, int shiftY) {
        this.ascent = ascent;
        this.descent = descent;
        this.shiftX = shiftX;
        this.shiftY = shiftY;
    }
        
    /**
    * Sets the graphic context to the display.
    * @param contextGraphic the graphic context.
    */
    public void setGraphicContext(GraphicContext graphicContext) {
        this.graphicContext = graphicContext;
    }
    
    /**
    * Returns the graphic context of the display.
    */
    public GraphicContext getGraphicContext() {
        return graphicContext;
    }
    
    // *** Fin de l'interface Displayable ***
    // **************************************
    
    // *************************************************
    // *** Implémentation de l'interface Localisable ***
    
    /**
    * Sets the x-location of the instance.
    * @param x the x-location of the instance.
    */
    public void setX(int x) {
        setLocation(x, getLocation().y);
    }
    
    /**
    * Returns the x-location of the display.
    */
    public int getX() {
        return getLocation().x;
    }
    
    /**
    * Sets the y-location of the instance.
    * @param y the y-location of the instance.
    */
    public void setY(int y) {
        setLocation(getLocation().x, y);
    }
    
    /**
    * Returns the y-location of the display.
    */
    public int getY() {
        return getLocation().y;
    }
    
    // *** Fin de l'interface Localisable ***
    // **************************************
    
    // *********************************************
    // *** Implémentation de l'interface Sizable ***
    
    /**
    * Sets the width of the instance.
    * @param width the width of the instance
    */
    public void setWidth(int width) {
        setSize(width, getSize().height);
    }
    
    /**
    * Returns the width of the display.
    */
    public int getWidth() {
        return getSize().width;
    }
    
    /**
    * Sets the height of the instance.
    * @param height the height of the instance
    */
    public void setHeight(int height) {
        setSize(getSize().width, height);
    }
    
    /**
    * Returns the height of the display.
    */
    public int getHeight() {
        return getSize().height;
    }
    
    // *** Fin de l'interface sizable ***
    // **********************************
    
    
    // ****************************************
    // Implémentation de l'interface Selectable
    
    /**
    * Selects the display. Selects the instance and its children if any.
    */
    public void select() {
        if (!isSelected) {
            isSelected = true;
            int count = getComponentCount();
            for (int i = 0; i < count; i++)
                ((Display) getComponent(i)).select();
        }
    }
    
    /**
    * Deselects the display. Deselects the instance and its children if any.
    */
    public void deselect() {
        if (isSelected) {
            isSelected = false;
            int count = getComponentCount();
            for (int i = 0; i < count; i++)
                ((Display) getComponent(i)).deselect();
        }
    }
    
    /**
    * Sets the display as selected.
    */
    public void setSelected() {
        isSelected = true;
    }

    /**
    * Sets the display as not selected.
    */
    public void setNotSelected() {
        isSelected = false;
    }
    
    /**
    * Checks if the display is selected or not.
    * @return <CODE>true</CODE> if the display is selected. 
    * <CODE>false</CODE> otherwise.
    */
    public boolean isSelected() {
        return isSelected;
    }
    
    /**
    * Returns <CODE>true</CODE> if the display have components selected or is selected.<BR>
    * <CODE>false</CODE> otherwise.
    */
    public boolean gotSelectedElements() {
        if (isSelected)
            return true;
            
        int count = getComponentCount();
        for (int i = 0; i < count; i++)
            if (((Display) getComponent(i)).gotSelectedElements())
                return true;
        
        return false;
    }
    
    // *** Fin de l'interface Selectable ***
    // *************************************
    
    // *****************************************
    // Implémentation de l'interface Colorisable
    
    /**
    * Sets a new color to the context.
    * @param foregroundColor the new color.
    */
    public void setForegroundColor(Color foregroundColor) {
        graphicContext.setForegroundColor(foregroundColor);
    }
 
    /**
    * Returns the color used by the graphic context.
    */
    public Color getForegroundColor() {
        return graphicContext.getForegroundColor();
    }
    
    /**
    * Sets a new color to the context.
    * @param backgroundColor the new color.
    */
    public void setBackgroundColor(Color backgroundColor) {
        graphicContext.setBackgroundColor(backgroundColor);
    }
    
    /**
    * Returns the color used by the graphic context.
    */
    public Color getBackgroundColor() {
        return graphicContext.getBackgroundColor();
    }
    
    /**
    * Sets a new selection color to the context.
    * @param selectionColor the new color.
    */
    public void setSelectionColor(Color selectionColor) {
        graphicContext.setSelectionColor(selectionColor);
    }

    /**
    * Returns the color used in the graphic context to hilight the selection.
    */
    public Color getSelectionColor() {
        return graphicContext.getSelectionColor();
    }
    
    // *** Fin de l'interface Colorisable ***
    // **************************************
    
    // *****************************************
    // Implémentation de l'interface Iconifiable
    
    // Même si en fait ce n'est pas un display que l'on va iconifier, on implémente l'interface
    // iconifiable, parce que c'est à partir de là que le processus d'iconification va être lancé,
    // notamment en parcourant la liste de tous les display sélectionnés, potentiellement iconifiables.
    
    /**
    * Associates an icon name to the instance.
    *
    * @param iconName the name of the icon
    */
    public void setIconName(String iconName) {
        // on ne fait rien, ce n'est pas un display que l'on va iconifier.
    }

    /**
    * Returns the name of the icon associated to the instance.
    *
    * @returns The name of the icon, or <CODE>null</CODE> if there is no name
    * associated.
    */
    public String getIconName() {
        return null; // ce n'est pas un display que l'on va iconifier.
    }
    
    /**
    * @return <CODE>true</CODE> if the instance is an icon. <CODE>false</CODE> otherwise.
    */
    public boolean isIcon() {
        return false; // ce n'est pas un display que l'on va iconifier.
    }
    
    /**
    * Iconifies all the selected elements of the formula.
    */
    public void iconify() {
        SelectionEvent selectionEvent = new SelectionEvent(this);

        // On récupère la taille de la sélection.
        Integer selectionSize = null;
        selectionEvent.setAction(SelectionEvent.GET_SELECTION_SIZE, selectionSize);
        fireSelectionEvent(selectionEvent);
        selectionSize = (Integer) selectionEvent.getArgument();
        // Si différente de 0, il y a qque chose à iconifier.
        if (selectionSize.intValue() != 0) {
            // On récupère la sélection entière.
            Vector selection = null;
            selectionEvent.setAction(SelectionEvent.GET_SELECTION, selection);
            fireSelectionEvent(selectionEvent);
            selection = (Vector) selectionEvent.getArgument();
            // On créé la liste des éléments à iconifier. (on enlève les displays
            // d'opérateurs parasites, et aux displays qui restent, on récupère
            // la fts dont ils sont le display).
            Display display;
            FormulaTreeStructure fts;
            Vector toIconify = new Vector();
            for (int i = 0; i < selectionSize.intValue(); i++) {
                display = (Display) selection.elementAt(i);
                if (!display.isSymbolOperatorDisplay()) {
                    // On désélectionne le display
                    display.deselect();
                    // On récupère le fts associé au display.
                    fts = (FormulaTreeStructure) display.getListener();
                    // On l'ajoute à la liste des fts à iconifier
                    toIconify.addElement(fts);
                }
            }
            // On purge la sélection
            selectionEvent.setAction(SelectionEvent.PURGE, null);
            fireSelectionEvent(selectionEvent);
            // On envoie l'événement au modèle pour lui dire qu'il faut iconifier
            ControlEvent controlEvent = new ControlEvent(this);
            controlEvent.setAction(ControlEvent.ICONIFY, toIconify);
            fireControlEvent(controlEvent);
        }
    }

    /**
    * Uniconifies all the selected icons which are in the formula.
    */
    public void uniconify() {
        SelectionEvent selectionEvent = new SelectionEvent(this);
        // On récupère la taille de la sélection.
        Integer selectionSize = null;
        selectionEvent.setAction(SelectionEvent.GET_SELECTION_SIZE, selectionSize);
        fireSelectionEvent(selectionEvent);
        selectionSize = (Integer) selectionEvent.getArgument();
        // Si différente de 0, il y a éventuellement qque chose à désiconifier.
        if (selectionSize.intValue() != 0) {
            // On récupère la sélection entière.
            Vector selection = null;
            selectionEvent.setAction(SelectionEvent.GET_SELECTION, selection);
            fireSelectionEvent(selectionEvent);
            selection = (Vector) selectionEvent.getArgument();
            selectionEvent.setAction(SelectionEvent.PURGE, null);
            fireSelectionEvent(selectionEvent);
            // On parcourt la liste de sélection, et si l'on a affaire à une icone
            // on la désiconifie.
            Display display = null;
            FormulaTreeStructure fts = null;
            for (int i = 0; i < selectionSize.intValue(); i++) {
                display = (Display) selection.elementAt(i);
                // On désélectionne le display.
                display.deselect();
                // On récupère le fts associé au display.
                fts = (FormulaTreeStructure) display.getListener();
                if (fts.isIcon()) {
                    display.computeAncestorsAttributes();
                    // On envoie un événement pour dire au modèle qu'il doit
                    // déisconifier fts.
                    ControlEvent controlEvent = new ControlEvent(this);
                    controlEvent.setAction(ControlEvent.UNICONIFY, fts);
                    fireControlEvent(controlEvent);
                }
            }
        }
    }
    
    /**
    * Uniconifies all the selected icons.<BR>
    * If the icons contains other icons, these are uniconified. There is a
    * recursion process.
    */
    public void uniconifyAll() {
        ControlEvent controlEvent = new ControlEvent(this);
        controlEvent.setAction(ControlEvent.UNICONIFY_ALL, null);
        fireControlEvent(controlEvent);
    }
    
    /**
    * Sets the istance as iconifiable.
    * @param isIconifiable <CODE>true</CODE> if the instance is iconifiable.
    * <CODE>false</CODE> otherwise.
    */
    public void setIsIconifiable(boolean isIconifiable) {
        // Ce n'est pas un display que l'on va iconifier
    }
    
    /**
    * Returns <CODE>true</CODE> if the instance is iconifiable.
    * <CODE>false</CODE> otherwise.
    */
    public boolean isIconifiable() {
        // Ce n'est pas un display que l'on va iconifier
        return false;
    }
    
    // *** Fin de l'interface Iconifiable ***
    // **************************************
    
    
    // *****************************************************
    // Implémentation de l'interface ModelListenerController
    
    /**
    * Registers another listener to be informed of changes of the display.
    * @param controlListener a listener to add.
    */
    public void addControlListener(ControlListener controlListener) {
        listener = controlListener;
    }

    /**
    * Removes all the listeners.
    */
    public void removeAllControlListener() {
        listener = null;
    }
    
    /**
    * Removes a listener.
    * @param controlListener a listener to remove.
    */
    public void removeControlListener(ControlListener controlListener) {
        listener  = null;
    }
    
    /**
    * Fires a Control event to registered listeners.
    * @param controlEvent event encapsulating relevant information.
    */
    public void fireControlEvent(ControlEvent controlEvent) {
        listener.consumeControlEvent(controlEvent);
    }
    
    /**
    * Returns the listener of the dispay. (i.e. a fts)
    */
    public ControlListener getListener() {
        return listener;
    }

    // *** Fin de l'interface ModelListenerController ***
    // **************************************************
    
    
    // Selection management.
    
    /**
    * Registers another listener to be informed of changes of the display.
    * @param selectionEventListener a listener to add.
    */
    public void addSelectionEventListener(SelectionEventListener selectionEventListener) {
        selectionManager = selectionEventListener;
    }
    
    /**
    * Removes a listener.
    * @param selectionEventListener listener to remove.
    */
    public void removeSelectionEventListener(SelectionEventListener selectionEventListener) {
        if (selectionEventListener == selectionManager)
            selectionManager = null;
    }
    
    /**
    * Fires a SelectionEvent event to registered listeners.
    * @param selectionEvent event encapsulating relevant information.
    */
    public void fireSelectionEvent(SelectionEvent selectionEvent) {
        selectionManager.consumeSelectionEvent(selectionEvent);
    }
    
    
    
    /**
    * For debugg only !!!!
    */

    public void ToString() {
        System.out.println(super.toString());
        System.out.println("\t x = " + getX() + " y = " + getY());
        System.out.println("\t ascent = " + ascent + " descent = " + descent);
        System.out.println("\t width = " + getWidth() + " height = " + getHeight());
    }
    
    public void whoAmI() {
        System.out.println("I am a " + getClass().getName() + " with the " + getLayout().getClass().getName()  + " manager");
        System.out.println("My level is " + level);
        System.out.println("My children are :");
        for (int i = 0; i < getComponentCount(); i++)
            ((Display) getComponent(i)).whoAmI();
    }
}
