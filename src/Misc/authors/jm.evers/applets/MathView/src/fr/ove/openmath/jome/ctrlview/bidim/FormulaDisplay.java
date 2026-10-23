/*
$Id: FormulaDisplay.java,v 1.3 2003/02/18 11:48:46 sander Exp $
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
import java.util.Vector;
import fr.ove.openmath.jome.ctrlview.events.*;
import fr.ove.openmath.jome.ctrlview.bidim.*;
import fr.ove.openmath.jome.ctrlview.bidim.selection.events.*;
import fr.ove.openmath.jome.ctrlview.bidim.selection.*;
import fr.ove.openmath.jome.ctrlview.bidim.images.ImageLoader;
import fr.ove.openmath.jome.model.*;

/**
* The bidimensional display of the elements of the formula.
*
* @author © 1999 DIRAT Laurent
* @version 2.0 17/12/1999
*/

public class FormulaDisplay extends BidimDisplay {
    /**
    * The image loader.
    */
    private static ImageLoader loader;
    
    /**
    * The default constructor.
    */
    public FormulaDisplay() {
        super(new GraphicContext());
        
        // On affecte le layout manager d'une formule.
        OneFormulaLayout layout = new OneFormulaLayout();
        layout.initDisplay(this);
        super.setLayout(layout);
        
        // Création du gestionnaire de la sélection
        SelectionManager selectionManager = new SelectionManager();
        addSelectionEventListener(selectionManager);
        
        // On charge les icones
        loader = new ImageLoader(this);
    }
    
    /**
    * Registers another listener to be informed of changes of the display.
    * @param controlListener a listener to add.
    */
    public void addControlListener(ControlListener controlListener) {
        
        // ben ça, ça marche pas encore comme voulu !!!
        // au moins dans café.
        
        super.addControlListener(controlListener);
        buildDisplay();
        invalidate();
        setComputeAttributes(true);
        validate();
        repaint();
    }
    
    /**
    * Returns the preferred size of the display.
    */
    public Dimension getPreferredSize() {
        Dimension prefSize;
        
        if (getComponentCount() == 0)
            // si y a rien à afficher, on fixe une taille quelconque pour voir qque chose
            prefSize = new Dimension(/*100, 50*/10, 10);
        else
            prefSize = super.getPreferredSize();
            
        return prefSize;
    }
    
    /**
    * Sets the layout manager for the display.<BR>
    * In fact does nothing because the instance has its own display and must only have the one it has.
    * Overrinden just to prevent unwanted manipulation.
    * @param mgr the specified layout manager
    */
    public void setLayout(LayoutManager lm) {
    }
    
    
    /***************************/
    /* Gestion de la sélection */
    /***************************/

    
    /**
    * Returns the linear expression of the selection.
    */
    public String getLinearSelection() {
        FormulaTreeStructure ftsSelection = getFtsSelection();
        
        if (ftsSelection == null)
            return "";
        else 
            return ftsSelection.createLinear("");
    }

    /**
    * Returns the selected displays.<BR>
    * Display of symbol operator are not in this list.
    */
    private Vector getSelection() {
        SelectionEvent selectionEvent = new SelectionEvent(this);

        // On récupère la taille de la sélection.
        Integer selectionSize = null;
        selectionEvent.setAction(SelectionEvent.GET_SELECTION_SIZE, selectionSize);
        fireSelectionEvent(selectionEvent);
        selectionSize = (Integer) selectionEvent.getArgument();
        // Si différente de 0, il y a qque chose à récupérer.
        if (selectionSize.intValue() != 0) {
            // On récupère la sélection entière.
            Vector selection = null;
            selectionEvent.setAction(SelectionEvent.GET_SELECTION, selection);
            fireSelectionEvent(selectionEvent);
            selection = (Vector) selectionEvent.getArgument();
            // On créé la liste des éléments à récupérer. (on enlève les displays
            // d'opérateurs parasites, et aux displays qui restent, on récupère
            // la fts dont ils sont le display).
            Display selectionDisplay;
            FormulaTreeStructure fts;
            Vector toGet = new Vector();
            for (int i = 0; i < selectionSize.intValue(); i++) {
                selectionDisplay = (Display) selection.elementAt(i);
                if (!selectionDisplay.isSymbolOperatorDisplay()) {
                    // On récupère le fts associé au display.
                    fts = (FormulaTreeStructure) selectionDisplay.getListener();
                    // On l'ajoute à la liste des fts à iconifier
                    toGet.addElement(fts);
                }
            }

            return toGet;
        }
        
        return null;
    }
    
    /**
    * Returns the formula tree structure associated with the selected displays.<BR>
    * Returns <CODE>null</CODE> if there is no selected displays.
    * For example, let us assume that the formula is a+b+c, and a and c are selected.
    * The fts returned is the fts of a+c.
    * @param selection the list of selected displays.
    */
    private FormulaTreeStructure getFtsSelection() {
        FormulaTreeStructure ftsSelection = null;
        Vector selection = getSelection();
        
        if (selection != null) {
            ftsSelection = (FormulaTreeStructure) selection.elementAt(0);
            if ((selection.size() == 1) && 
                (!ftsSelection.isIcon() || (ftsSelection.isIcon() && (((Icon) ftsSelection).getNbIconified() == 1)))) {
                ftsSelection = (FormulaTreeStructure) ftsSelection.clone();
            }
            else {
                FormulaTreeStructure childFts = null;
                
                // On récupère le père dans la FTS du premier élément sélectionné
                // pour savoir à quel type d'opération on a affaire.
                ftsSelection = (FormulaTreeStructure) ftsSelection.getFather();
                ftsSelection = (FormulaTreeStructure) ftsSelection.duplicate();
                int count = selection.size();
                for (int i = 0; i < count; i++) {
                    childFts = (FormulaTreeStructure) selection.elementAt(i);
                    childFts = (FormulaTreeStructure) childFts.clone();
                    ftsSelection.addChild(childFts);
                }
            }
        }
        
        return ftsSelection;
    }
}
    
    
