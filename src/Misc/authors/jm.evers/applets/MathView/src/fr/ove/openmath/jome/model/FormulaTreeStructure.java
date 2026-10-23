/*
$Id: FormulaTreeStructure.java,v 1.4 2003/02/18 11:48:47 sander Exp $
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


package fr.ove.openmath.jome.model;

import java.util.*;
import fr.ove.utils.*;
import fr.ove.openmath.jome.behaviour.*;
import fr.ove.openmath.jome.model.*;
import fr.ove.openmath.jome.model.events.*;
import fr.ove.openmath.jome.ctrlview.events.*;
import fr.ove.openmath.jome.model.evaluation.*;

/**
* @author © 2000 DIRAT Laurent
* @version 2.1  10/01/2000
*/
public abstract class FormulaTreeStructure extends Node implements ControlListener, Iconifiable, fr.ove.utils.Comparable, Maskable, Modifiable {
    /**
    * To check if we consider the instance as a template or not. <BR>
    * (usefull for the building of the formula tree structure).
    * By default, the new intance create is not a template.
    */
    private boolean isTemplate = false;
    
    /**
    * The priority of the instance considered as an operator.
    */
    private int asOperatorPriority;

    /**
    * The priority of the instance considered as an operand.
    */
    private int asOperandPriority;
    
    /**
    * The list of listeners of the instance
    */
    private Vector listeners = new Vector();
    
    /**
    * Flag to set if we allow the move of the children (so operands) of the instance.<BR>
    * In other word, says some kind of commutativity of the operator.<BR>
    * The default is <CODE>false</CODE> because only "few" operators have this property.
    */
    private boolean areOperandsMovable = false;
    
    /**
    * The resources manager
    */
    static FormulaResourcesManager resourcesManager = new FormulaResourcesManager("fr.ove.openmath.jome.model.resources2");
    
    /**
    * theresource indentifier of the instance
    */
    private String resourceIdentifier;
    
    // ***********************************
    // Gestion des listeners de l'instance
    
    /**
    * Registers another listener to be informed of changes of the FTS.
    * @param modelListener a listener to add.
    */
    public void addModelListener(ModelListener modelListener) {
        listeners.addElement(modelListener);
    }

    /**
    * Removes a listener.
    * @param modelListener a listener to remove.
    */
    public void removeModelListener(ModelListener modelListener) {
        listeners.removeElement(modelListener);
    }

    /**
    * Removes all the listeners.
    */
    public void removeAllModelListener() {
        listeners.setSize(0);
    }

    /**
    * Fires a ModelEvent event to registered listeners.
    * @param modelEvent event encapsulating relevant information.
    */
    public void fireModelEvent(ModelEvent modelEvent) {
        for (int i = 0; i < listeners.size(); i++)
          ((ModelListener)listeners.elementAt(i)).consumeModelEvent(modelEvent);
    }
    
    /**
    * Returns all the registered listener of the instance.
    */
    public Vector getListeners() {
        return listeners;
    }

    // **** fin de la gestion des listeners ****
    // *****************************************

    
    /**
    * Sets the resource identifier of the instance.
    * @param the resource identifier.
    */
    public void setResourceIdentifier(String resourceIdentifier) {
        this.resourceIdentifier = resourceIdentifier;
    }
    
    /**
    * Returns the resource identifier of the instance.
    */
    public String getResourceIdentifier() {
        return resourceIdentifier;
    }
    
    /**
    * Sets the as operator priority
    * @param asOperatorPriority the priority
    */
    public void setAsOperatorPriority(int asOperatorPriority) {
        this.asOperatorPriority = asOperatorPriority;
    }
    
    /**
    * Returns the priority of the instance viewed as an operator.
    * @return the priority of the instance viewed as an operator.
    */
    public int getAsOperatorPriority() {
        return asOperatorPriority;
    }

    /**
    * Sets the as operand priority
    * @param asOperandPriority the priority
    */
    public void setAsOperandPriority(int asOperandPriority) {
        this.asOperandPriority = asOperandPriority;
    }
    
    /**
    * Returns the priority of the instance viewed as an operand.
    * @return the priority of the instance viewed as an operand.
    */
    public int getAsOperandPriority() {
        return asOperandPriority;
    }
    
    /**
    * Returns <CODE>true</CODE> if it is a template.<BR>
    * <CODE>false</CODE> otherwise.
    */
    public boolean isTemplate() {
        return isTemplate;
    }

    /**
    * Sets the instance as a template.
    */
    public void setIsTemplate(boolean isTemplate) {
        this.isTemplate = isTemplate;
    }
    
    /**
    * Returns the node (operator), which is the position in the formula tree, where the
    * instance have to be inserted.
    *
    * @param current the current insert position in the formula tree.
    * @return the insert position in the formula tree for the instance.
    */
    public FormulaTreeStructure findLocation(FormulaTreeStructure current) {
        //while (this.asOperandPriority < current.asOperatorPriority)
        while (asOperandPriority < current.getAsOperatorPriority())
            current = (FormulaTreeStructure) current.getFather();

        return current;
    }

    /**
    * Returns the father of the node which, from the current instance, have the specified
    * priority.
    *
    * @param priority the specified priority.
    * @return the desired father.
    */
    public FormulaTreeStructure goTo(int priority) {
        FormulaTreeStructure current = this;

        // Si le test est vrai, c'est qu'on est déjà sur un tel noeud.
        // On retourne un noeud identique placé au-dessus dans l'arbre
        int prio = current.asOperatorPriority;
        if (current.asOperatorPriority == priority)
            current = (FormulaTreeStructure) current.getFather();

        while ((current.asOperatorPriority != priority) &&
                (((FormulaTreeStructure)current.getFather()).asOperatorPriority != 0))
            current = (FormulaTreeStructure) current.getFather();

        return (FormulaTreeStructure) current.getFather();
    }
    
    /*
    * Sets if we allow the move of the children (so operands) of the instance.
    * @param areOperandsMovable <CODE>true</CODE> if the operands are movable.
    * <CODE>false</CODE> otherwise.
    */
    public void setAreOperandsMovable(boolean areOperandsMovable) {
        this.areOperandsMovable = areOperandsMovable;
    }
    
    /*
    * Returns if we allow the move of the children (so operands) of the instance.
    * @return <CODE>true</CODE> if the operands are movable. <CODE>false</CODE> otherwise.
    */
    public boolean getAreOperandsMovable() {
        return areOperandsMovable;
    }
    
    /**
    * Moves the specified list of the instance children to the specified
    * rank. The first child in the list has its rank setted to the specified
    * one, the second to the first+1, ...and so on.
    * @param list the list of the instance operands to move.
    * @param rank the specified rank.
    */
    public void moveOperands(Vector list, int rank) {
        // On fait la permutation dans la FTS
        moveChildren(list, rank);
        
        ModelEvent modelEvent = new ModelEvent(this);
        // On a fait les déplacements nécéssaires, on reconstruit le display
        modelEvent.setAction(ModelEvent.REBUILD, null);
        // On envoie l'événement.
        fireModelEvent(modelEvent);
        // On envoie maintenant un événement comme quoi il faut mettre à
        // jour l'affichage.
        modelEvent.setAction(ModelEvent.UPDATE, null);
        // On envoie l'événement.
        fireModelEvent(modelEvent);
    }
    
    // #################################################
    // ### Implémentation des différentes interfaces ###
    // #################################################
    
    // ****************************************
    // Implémentation de l'interface Comparable
    
    /**
    * Tests if the instance is equal to the specified one.
    * @param toCompare the instance to compare with the current instance.
    */
    public boolean isEqual(fr.ove.utils.Comparable toCompare) {
        // On verra plus tard
        return false;
    }
    
    /**
    * Tests if the instance is greater than the specified one.
    * @param toCompare the instance to compare with the current instance.
    */
    public boolean isGreater(fr.ove.utils.Comparable toCompare) {
        // On s'en fout, seule l'égalité nous intéresse
        return false;
    }
    
    /**
    * Tests if the instance is greater or equal than the specified one.
    * @param toCompare the instance to compare with the current instance.
    */
    public boolean isGreaterOrEqual(fr.ove.utils.Comparable toCompare) {
        // On s'en fout, seule l'égalité nous intéresse
        return false;
    }
    
    /**
    * Tests if the instance is lesser than the specified one.
    * @param toCompare the instance to compare with the current instance.
    */
    public boolean isLesser(fr.ove.utils.Comparable toCompare) {
        // On s'en fout, seule l'égalité nous intéresse
        return false;
    }
    
    /**
    * Tests if the instance is lesser or equal than the specified one.
    * @param toCompare the instance to compare with the current instance.
    */
    public boolean isLesserOrEqual(fr.ove.utils.Comparable toCompare) {
        // On s'en fout, seule l'égalité nous intéresse
        return false;
    }
    
    // *** Fin de l'interface Comparable ***
    // *************************************
    
    
    // *****************************************
    // Implémentation de l'interface Iconifiable
    
    /**
    * Associates an icon name to the instance.
    * @param iconName the name of the icon
    */
    public void setIconName(String iconName) {
        // On ne fait rien, c'est dans les classes dérivées qu'on va implémenter getIconName().
        // Ca évite de stocker une String pour pas grand chose.
    }
    
    /**
    * Returns the name of the icon associated to the instance.<BR>
    * The icon name is the name of the ressource identifier where "_Ico" added to the end.
    * @returns The name of the icon, or <CODE>null</CODE> if there is no name
    * associated.
    */
    public String getIconName() {
        return resourceIdentifier + "_Ico"; //resourcesManager.getIconName(resourceIdentifier);
    }
    
    /**
    * @return <CODE>true</CODE> if the instance is an icon. <CODE>false</CODE> otherwise.
    */
    public boolean isIcon() {
        return false;
    }
    
    /**
    * Iconifies the instance.
    */
    public void iconify() {
        if (isIconifiable()) {
            Icon icon = new Icon(this);
            // On ajoute l'instance à iconifier dans Icon (<=> on iconfie l'instance)
            icon.addIconified(this);
            // On insère maintenant notre icon à la place de l'instance.
            icon.insert(this);
        }
    }
    
    /**
    * Uniconifies the instance.
    */
    public void uniconify() {
        // Par défaut on ne fait rien, on ne peut désiconifier qu'une icone
    }
    
    
    /**
    * Uniconifies all the iconified parts of the instance.
    */
    public void uniconifyAll() {
        if (getNbChildren() != 0) {
            boolean rebuildDisplay = false;
            // On parcourre tous les enfants de l'instance et si on trouve une icone, on la
            // désiconifie
            FormulaTreeStructure fts = null;
            
            /*
            for (Enumeration e = getChildren().elements(); e.hasMoreElements(); ) {
                fts = (FormulaTreeStructure) e.nextElement();
                if (fts.isIcon()) {
                    fts.uniconify();
                    rebuildDisplay = true;
                }
            }
            */
            for (int i = 0; i < getNbChildren(); i++) {
                fts = (FormulaTreeStructure) getChild(i);
                if (fts.isIcon()) {
                    fts.uniconify();
                    rebuildDisplay = true;
                    i = -1; // On repart à zéro car la désiconification a pu en refaire apparaître.
                }
            }
            
            // l'instance n'a plus de d'icone fille, on s'occupe de la descendance
            for (Enumeration e = getChildren().elements(); e.hasMoreElements(); )
                ((FormulaTreeStructure) e.nextElement()).uniconifyAll();
        
            // Reconstruction du display que si y a eu désiconification
            if (rebuildDisplay) {
                // On part du principe que si l'on a une icone, c'est que l'on a le display qui lui est
                // associé, donc reconstruction du display.
                // Faire gaffe à appeler le ModelEvent.UPDATE quand nécessaire
                
                // On envoie maintenant un événement comme quoi il faut reconstruire
                // l'affichage.
                ModelEvent modelEvent = new ModelEvent(this);
                modelEvent.setAction(ModelEvent.REBUILD, null);
                fireModelEvent(modelEvent);
            }
        }
    }
        
    /**
    * Sets the instance as iconifiable.
    * @param isIconifiable <CODE>true</CODE> if the instance is iconifiable.
    * <CODE>false</CODE> otherwise.
    */
    public void setIsIconifiable(boolean isIconifiable) {
        // On ne fait rien, les classes héritières retourneront la valeur qui leur correspond.
        // On évite de stocker un booléen pour pas grand chose puisque peu ne seront pas iconifiables
    }
    
    /**
    * Returns <CODE>true</CODE> if the instance is iconifiable.
    * <CODE>false</CODE> otherwise.
    */
    public boolean isIconifiable() {
        return true;  // Très peu ne sont pas iconifiables. Les concernés surchargeront cette méthode.
    }
    
    // *** Fin de l'interface Iconifiable ***
    // **************************************
    
    
    // *********************************************
    // Implémentation de l'interface ControlListener
    
    
    /**
    * Consumes (i.e. treats) the event received.
    * @param controlEvent the event to consume.
    */
    public void consumeControlEvent(ControlEvent controlEvent) {
        ModelEvent modelEvent;
        int action = controlEvent.getAction();
        FormulaTreeStructure fts = null;
        
        switch (action) {
            case ControlEvent.ADD :
                //System.out.println("ControlEvent.ADD : on ajoute un nouveau display");
                break;
            case ControlEvent.REMOVE :
                //System.out.println("ControlEvent.REMOVE : on enleve un display");
                break;
            case ControlEvent.ICONIFY :
                //System.out.println("ControlEvent.ICONIFY : on iconifie");
                Vector toIconify = (Vector) controlEvent.getArgument();
                Icon icon = null;
                if (toIconify.size() == 1) {
                    fts = (FormulaTreeStructure) toIconify.elementAt(0);
                    // une seule fts à iconifier.
                    if (fts.isIconifiable()) {
                        // Ce n'est pas déjà une icone et fts est iconifiable, on peut en créer une.
                        icon = new Icon(fts);
                        // On ajoute notre fts à iconifier dans Icon (<=> on iconfie fts)
                        icon.addIconified(fts);
                        // On insère maintenant notre icon à la place de fts.
                        icon.insert(fts);
                    }
                    // fts = icon.father pour que l'on puisse ajouter le display de notre icone dans icon.father
                    fts = (FormulaTreeStructure) icon.getFather();
                }
                else {
                    // Plusieurs fts sont à iconifier. Il faut donc récupérer la fts
                    // dont elles sont les filles, pour pouvoir créer l'icone adéquate.
                    fts = (FormulaTreeStructure) ((FormulaTreeStructure) toIconify.elementAt(0)).getFather();
                    // On crée une nouvelle icone
                    icon = new Icon(fts);
                    // On ajoute maintenant tous les fts à iconifier.
                    int countToIconify = toIconify.size();
                    for (int i = 0; i < countToIconify; i++)
                        icon.addIconified((FormulaTreeStructure) toIconify.elementAt(i));
                    // On insère maintenant notre icon à la place de la 1-ère fts.
                    icon.insert(icon.getIconified(0));
                }
                
                // On ajoute un display pour notre icon.
                modelEvent = new ModelEvent(fts);
                modelEvent.setAction(ModelEvent.ADD, icon);
                fts.fireModelEvent(modelEvent);
                
                // On met à jour l'affichage de la fts.
                modelEvent.setAction(ModelEvent.UPDATE, null);
                fts.fireModelEvent(modelEvent);
                
                break;
                
            case ControlEvent.UNICONIFY :
                //System.out.println("ControlEvent.UNICONIFY : on desiconifie");
                Icon anIcon = (Icon) controlEvent.getArgument();
                fts = (FormulaTreeStructure) anIcon.getFather();
                anIcon.uniconify();
                
                // On envoie maintenant un événement comme quoi il faut reconstruire
                // l'affichage.
                modelEvent = new ModelEvent(fts);
                modelEvent.setAction(ModelEvent.REBUILD, null);
                fts.fireModelEvent(modelEvent);
                // On envoie maintenant un événement comme quoi il faut mettre à
                // jour l'affichage.
                modelEvent.setAction(ModelEvent.UPDATE, null);
                // On envoie l'événement.
                fts.fireModelEvent(modelEvent);
                break;
                
            case ControlEvent.UNICONIFY_ALL :
                // On remonte dans la FTS jusqu'à tomber sur la racine de la formule.
                fts = this;
                while (fts.getFather() != null)
                    fts = (FormulaTreeStructure) getFather();
                    
                // On est sur la racine de la formule.
                // A partir de là, on demande de tout désiconifier
                fts.uniconifyAll();
                
                // On envoie maintenant un événement comme quoi il faut mettre à
                // jour l'affichage.
                modelEvent = new ModelEvent(fts);
                modelEvent.setAction(ModelEvent.UPDATE, null);
                // On envoie l'événement.
                fts.fireModelEvent(modelEvent);
                break;
                
            case ControlEvent.SUBSTITUTE :
                //System.out.println("ControlEvent.ICONIFY : on iconifie");
                // ### ATTENTION, le premier élément sera le nom pour la substitution
                Vector toSubstitute = (Vector) controlEvent.getArgument();
                fts = (FormulaTreeStructure) toSubstitute.elementAt(1);
                // On crée une nouvelle icone
                Icon substitution = new Icon((String) toSubstitute.elementAt(0));
                // On ajoute maintenant tous les fts à iconifier.
                int countToIconify = toSubstitute.size();
                
                if ((countToIconify == 2) && !fts.isIconifiable())
                    break;
                
                for (int i = 1; i < countToIconify; i++)
                    substitution.addIconified((FormulaTreeStructure) toSubstitute.elementAt(i));

                // On insère maintenant notre icon à la place de la 1-ère fts.
                substitution.insert(fts);

                // fts = substitution.father pour que l'on puisse ajouter le display de notre icone
                // dans substitution.father
                fts = (FormulaTreeStructure) substitution.getFather();

                // On ajoute un display pour notre icon.
                modelEvent = new ModelEvent(fts);
                modelEvent.setAction(ModelEvent.ADD, substitution);
                fts.fireModelEvent(modelEvent);
                // On met à jour l'affichage de la fts.
                modelEvent.setAction(ModelEvent.UPDATE, null);
                fts.fireModelEvent(modelEvent);
                break;
        }
    }

    // *** Fin de l'interface ControlListener ***
    // ******************************************
    
    // **************************************
    // Implémentation de l'interface Maskable
    
    /**
    * Sets the instance as vissible or not.
    * @param isVisible <CODE>true</CODE> if the instance is visible. <CODE>false</CODE> otherwise.
    */
    public void setIsVisble(boolean isVisible) {
        // Ne fait rien par défaut. Tout les opérateurs sont visibles à de rares exceptions près.
        // Ce sont ces exceptions qui maintiendront cette propriété correctement en la surchargeant
    }

    /**
    * Checks if the instance is visible.
    * @returns <CODE>true</CODE> if the instance is visible. <CODE>false</CODE> otherwise.
    */
    public boolean isVisible() {
        // Tout les opérateurs sont visibles à de rares exceptions près.
        // Ce sont ces exceptions qui maintiendront cette propriété correctement en la surchargeant
        return true;
    }
    
    // *** Fin de l'interface Maskable ***
    // ***********************************
    
    
    // *****************************************
    // Implémentation de l'interface Modifiable
    
    /**
    * Sets the value.
    */
    public void setValue(String value) {
    }
    
    /**
    * Returns the value.
    */
    public String getValue() {
        return null;
    }
    
    // *** Fin de l'interface Modifiable ***
    // *************************************
    
    // ############################################
    // ### Les différentes méthodes abstraites  ###
    // ############################################
    
    /**
    * To check is the instance is an operator.
    * @return <CODE>true</CODE> if it is an operator. <CODE>false</CODE> otherwise.
    */
    public abstract boolean isOperator();
    
    /**
    * Inserts the instance in the formula tree, from the current insertion position.
    * (checks the priorities and goes up in the tree if necessary).
    *
    * @param current the current insertion position.
    * @return the new insertion position.
    */
    public abstract FormulaTreeStructure insert(FormulaTreeStructure current);
    
    /**
    * The Creation of the corresponding linear expression of the formula.
    */
    public abstract String createLinear(String linear);

    /**
    * Evaluates the instance.
    */
    public abstract String evaluate();
}
