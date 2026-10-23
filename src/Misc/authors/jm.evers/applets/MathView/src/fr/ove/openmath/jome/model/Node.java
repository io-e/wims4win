/*
$Id: Node.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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
import java.io.Serializable;


/**
* Definition of a N-ary tree structure.
*
* @author © 1999 DIRAT Laurent
* @version 2.0  23/06/1999
*/
public abstract class Node implements Serializable, Cloneable {
    /**
    * The father of the instance in the tree structure
    */
    private Node father;

    /**
    * The children of the instance in the tree structure
    */
    private Vector children;

    /**
    * The rank of the instance in the set of its father's children
    */
    private int rank;
    
    /**
    * The strahler number of the node
    */
    private int nbStrahler;
    
    /**
    * The depth of the node in the tree structure.
    * It corresponds in fact at the number of its ancestors. (so root.depth == 0)
    */
    private int depth;
    

    /**
    * The Constructor.
    */
    public Node() {
        father = null;
        rank = 0;
        nbStrahler = 1;
        depth = 0;
    }

    // ### The Accessors ###

    /**
    * Returns the father of the instance.
    *
    * @return the father of the node.
    */
    public Node getFather() {
        return father;
    }

    /**
    * Returns the rank of the instance in the set of its father's children
    *
    * @return the rank of the node.
    */
    public int getRank() {
        return rank;
    }

    /**
    * Returns the child of the instance at the specified rank.
    *
    * @param rank the specified rank.
    * @return the child at the specified rank.
    */
    public Node getChild(int rank) {
        Node child = null;
        
        if (children != null) {
            try {
                child = (Node) children.elementAt(rank);
            }
            catch (ArrayIndexOutOfBoundsException e) {
                return null;
            }
        }
        
        return child;
    }

    /**
    * Returns the number of children of the instance.
    */
    public int getNbChildren() {
        int size = 0;
        
        if (children != null)
            size = children.size();
            
        return size;
    }

    /**
    * Returns the list of children of the instance.
    *
    * @return the list of children
    */
    public Vector getChildren() {
        return children;
    }

    // ### The Modifiers ###

    /**
    * Sets a father to instance.
    *
    * @param father the father to set
    */
    public void setFather(Node father) {
        this.father = father;
        depth = father.depth + 1;
        
        // On met à jour maintenant la profondeur des fils éventuels.
        if (children != null)
            adjustChildrenDepth();
    }

    /**
    * Adds a child to the instance.<BR>
    * The child added is the last of the list.
    *
    * @param child the child to add.
    */
    public void addChild(Node child) {
        if (children == null)
            children = new Vector(0, 1);
            
        children.addElement(child);
        child.setFather(this);
        adjustRank();
    }

    /**
    * Adds a child to the instance.<BR>
    * The child is added at the specified rank.
    *
    * @param child the child to add.
    * @param rank the specified rank.
    */
    public void addChild(Node child, int rank) {
        if (children == null)
            children = new Vector(0, 1);
            
        children.insertElementAt(child, rank);
        child.setFather(this);
        adjustRank();
    }
    
    /**
    * Removes the child of the current node at the specified rank.
    *
    * @param rank the rank of the child to remove.
    */
    public void removeChild(int rank) {
        if (children != null) {
            children.removeElementAt(rank);
            adjustRank();
        }
    }

    /**
    * Removes the specified child of the instance.
    *
    * @param node the child to remove.
    */
    public void removeChild(Node node) {
        if (children != null) {
            children.removeElement(node);
            adjustRank();
        }
    }
    
    /**
    * Remove all the children of the instance.
    */
    public void removeAll() {
        if (children != null) {
            int count = children.size();
            for (int i = 0; i < count; i++)
                ((Node) children.elementAt(rank)).father = null;
                
            children.setSize(0);
            children.trimToSize();
        }
    }
    
    /**
    * Sets the rank of the children of the instance.
    */
    private void adjustRank() {
        int i = 0;

        for (Enumeration e = children.elements(); e.hasMoreElements(); i++)
            ((Node) e.nextElement()).rank  = i;
    }
    
    /**
    * Changes the rank of the instance.
    *
    * @param rank the new rank.
    */
    private void changeRank(int rank) {
        int oldRank = this.rank;
        //Node father = this.father;

        if (rank > father.getNbChildren()) {
            rank = father.getNbChildren() + 1;
            father.addChild(this);  // On ajoute en dernier
        }
        else {
            if (rank < 0)
                rank = 0;
            father.addChild(this, rank);
        }

        // On élimine le noeud courant situé à l'ancienne position
        if (rank < oldRank)
            father.removeChild(oldRank + 1);  // l'insertion s'est faite avant, donc le fils courant est décalé de 1
        else
            father.removeChild(oldRank);
    }
    
    /**
    * Moves the specified list of the instance children to the specified
    * rank. The first child in the list has its rank setted to the specified
    * one, the second to the first+1, ...and so on.
    * @param list the list of the instance children.
    * @param rank the specified rank.
    */
    public void moveChildren(Vector list, int rank) {
        // Puisque list contient des fils de l'instance, on peut partir du prérequis que
        // children est non null.
        int NbChildren = children.size();
        Node child = null;
        Node first = null;
        
        for (int i = 0; i < list.size(); i++) {
            child = (Node) list.elementAt(i);
            if (i == 0)
                first = child;
            if (rank > NbChildren)
                child.changeRank(rank + (i+1));
            else {
                child.changeRank(rank);
                rank = first.getRank() + (i+1);
            }
        }
    }
    
    /**
    * Checks if the specified node is a child of the current instance.
    * @param node the specified node.
    * @return <CODE>true</CODE> if the specified node is a child of the current
    * instance. <CODE>false</CODE> otherwise.
    */
    public boolean hasChild(Node node) {
        boolean hasChild = false;
        
        if (children != null) 
            hasChild = children.contains(node);
            
        return hasChild;
    }
    
    /**
    * Clones (copies) the instance.<BR>
    * Make a deep clone, i.e. the children of the instance are also cloned.<BR>
    * Nevertheless, the father of the clone is set to <CODE>null</CODE>.
    */
    public synchronized Object clone(){
        Node theClone = null;
        try {
            //return super.clone();
            
            // On clone l'instance.
            theClone = (Node) super.clone();
            // Le père du clone est null
            theClone.father = null;
            if (children != null) {
                // On clone les enfants
                Vector theChildren = new Vector();
                int count = children.size();
                for (int i = 0; i < count; i++)
                    theChildren.addElement(((Node) children.elementAt(i)).clone());
                theClone.children = theChildren;
            }
            
        } catch (CloneNotSupportedException e) {
            System.out.println("clone failed !!");
        }
        
        return theClone;
    }
    
    /**
    * Duplicates (copies) the instance.<BR>
    * The difference with clone, is a deep copy is not performed.<BR>
    * The father of the duplicate is set to <CODE>null</CODE>.
    */
    public synchronized Object duplicate(){
        Node theClone = null;
        try {
            // On clone l'instance.
            theClone = (Node) super.clone();
            // Le père du clone est null
            theClone.father = null;
            // Pas de fils
            theClone.children = new Vector();
        } catch (CloneNotSupportedException e) {
            System.out.println("duplicate failed !!");
        }
        
        return theClone;
    }
    
    
    /*
    * ############################################
    * ### Les différents paramètres de l'arbre ###
    * ############################################
    */
    
    /*
    * La profondeur de l'arbre
    */
    
    /**
    * Returns the depth of the node.
    */
    public int getDepth() {
        return depth;
    }
    
    /**
    * Compute the depth of the node.
    */
    public void computeDepth() {
        int theDepth = 0;
        Node theFather = father;
        
        while (theFather != null) {
            theDepth++;
            theFather = theFather.father;
        }
        
        depth = theDepth;
        
        // On met à jour maintenant la profondeur des fils éventuels.
        if (children != null)
            adjustChildrenDepth();
    }
    
    /**
    * Adjusts the depth of the children of the node.
    */
    private void adjustChildrenDepth() {
        Node child = null;
        int count = children.size();
        for (int i = 0; i < count; i++) {
            child = (Node) children.elementAt(i);
            //child.depth++;
            child.depth = depth + 1;
            
            // On met à jour maintenant la profondeur des fils éventuels.
            if (child.children != null)
                child.adjustChildrenDepth();
        }
    }
    
    /*
    * Le nombre de Strahler
    */
    
    /**
    * Returns the strahler number of the node.
    */
    public int getNbStrahler() {
        return nbStrahler;
    }
    
    /**
    * Sets the Strahler number of the instance.<BR>
    * !!! ATTENTION !!! : should not be called at any time.
    * Just specific cases only, above all, when you know what you are doing.
    * @param nbStrahler the Strahler number value to set.
    */
    public void setNbStrahler(int nbStrahler) {
        this.nbStrahler = nbStrahler;
    }
    
    /**
    * Computes the strahler number of the node.
    * Prerequisite : the strahler number of the children of the instance are computed.
    */
    private void computeNodeNbStrahler() {
        // Si children == null, alors ça veut dire que l'instance est une feuille, et donc le
        // Strahler est déjà initialisé à 1.
        if (children != null) {
            int count = children.size();
            if (count == 0)
                // ça c'est au cas où des manipulations sur l'arbre, mais normalement, pas de soucis.
                nbStrahler = 1;
            else if (count == 1)
                nbStrahler = ((Node) children.elementAt(0)).nbStrahler;
            else {
                int max = 0;
                int min = Integer.MAX_VALUE;
                
                int tmpStrahler;
                
                for (int i = 0; i < count; i++) {
                    tmpStrahler = ((Node) children.elementAt(i)).nbStrahler;
                    min = (min < tmpStrahler) ? min : tmpStrahler;
                    max = (max > tmpStrahler) ? max : tmpStrahler;
                }
                
                int tabLength = max - min + 1;
                int tabValue[] = new int[tabLength];
                
                for (int i = 0; i < tabLength; i++) 
                    tabValue[i] = 0;
                    
                for (int i = 0; i < count; i++)
                    tabValue[((Node) children.elementAt(i)).nbStrahler - min] += 1;
                    
                int free = max - 1;
                tmpStrahler = max + tabValue[tabLength - 1] - 1;
                
                int requiered;
                for (int i = tabLength - 2; i >= 0; i--) {
                    if (tabValue[i] == 0)
                        continue;
                        
                    requiered = i + min +  tabValue[i] - 1;
                    
                    if (free > requiered)
                        free -= tabValue[i];
                    else {
                        tmpStrahler += requiered - free;
                        free = i + min - 1;
                    }
                }
                
                nbStrahler = tmpStrahler;
            }
        }
    }
    
    /**
    * Computes the strahler number of the tree whose the instance is the root
    */
    public void computeNbStrahler() {
        if (children != null) {
            int count = children.size();
            for (int i = 0; i < count; i++)
                ((Node) children.elementAt(i)).computeNbStrahler();
                
            computeNodeNbStrahler();
        }
    }
}
