
/*	
 Licence
 Copyright J.P. Quelen
 This is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; 
 It is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.
 You should have received a copy of the GNU General Public License
 along with this program. If not, see <http://www.gnu.org/licenses/>.
*/ 

// geo/Figure.java - 31/01/99

package geo ;

import java.awt.* ;
import java.util.Vector ;

/**
 * Figure géométrique formée d'une liste d'"Element"s. Cette liste contient en première position
 * le repère.
 */

public class Figure extends Vector
{

/**
 * Crée une figure vide.
 */

  public Figure ()
  { super () ; }

// marque en remplaçant le "id" de tous les éléments construits à partir de index par - id

  private void marquer (int index)
  { Element el ;
    el = (Element) elementAt (index) ;
    el.id = - el.id ;
    for (int i = index + 1 ; i < size () ; i ++)
      { el = (Element) elementAt (i) ;
        for (int j = 0 ; j < 3 ; j ++)
          { int k = el.args [j] ;
            if ((k > 0) && (((Element) elementAt (k)).id < 0) && (el.id > 0))
              el.id = - el.id ;
          }
      }
  }

/**
 * Effacement (graphique) de l'élément en position index et de tous les éléments chaînés
 * à lui sauf l'élément en position index1.
 */

  public void efface (int index, int index1, Graphics g)
  { if (index > 0)
      { marquer (index) ;
        for (int i = 1 ; i < size () ; i ++)
          { Element el = (Element) elementAt (i) ;
            if (el.id < 0)
              { el.id = - el.id ;
                if (i != index1) el.efface (g) ;

// 31/01/99
                else if ((el.id >= Element.PT_X_Y) && (el.id <= Element.POINT_TRANSLATION_PT_VECT))
                  ((pt) el.obj).traceNom (g) ;
//
                el.id = - el.id ;
              }
          }
      }
    else for (int i = 1 ; i < size () ; i ++)
           if (i != index1) ((Element) elementAt (i)).efface (g) ;
  }

/**
 * Tracé de la figure. (Modifié le 18/05/99) 
 */

  public void trace (Graphics g)
  { for (int i = 1 ; i < size () ; i ++)
    { Element el = (Element) elementAt (i) ;
      if (el.id > Element.POINT_TRANSLATION_PT_VECT) el.trace (g) ;
    }
    for (int i = 1 ; i < size () ; i ++)
    { Element el = (Element) elementAt (i) ;
      if (el.id <= Element.POINT_TRANSLATION_PT_VECT) el.trace (g) ;
    }
  }

  private Element tel [] = { null, null, null } ;

/**
 * Ajoute un élément à la figure.
 */

  public void ajout (Element el, String Nom, int X, int Y, double d)
  { Repere R = (Repere) firstElement () ;
    for (int i = 0 ; i < 3 ; i ++)
      { tel [i] = null ;
        if (el.args [i] > 0) tel [i] = ((Element) elementAt (el.args [i])) ;
      }
    ObjetRepere or ;
    if ((el.id >= Element.PT_X_Y) && (el.id <= Element.PT_SYMETRIQUE_PT_PT))
      or = new pt (Nom, R) ;
    else if ((el.id >= Element.POINT_CENTRE) && (el.id <= Element.POINT_TRANSLATION_PT_VECT))
      or = new point (Nom, R) ;
    else if ((el.id >= Element.VECTEUR_PT_PT) && (el.id <= Element.VECTEUR_PT_PT_PT))
      or = new Vecteur (Nom, R) ;
    else if ((el.id >= Element.DROITE_PT_PT) && (el.id <= Element.DROITE_BISS_PT_PT_PT))
      or = new Droite (Nom, R) ;
    else if ((el.id >= Element.NOMBRE) && (el.id <= Element.NOMBRE_PUISSANCE))
      or = new Scalaire (Nom, R) ;
    else switch (el.id)
      { case Element.POINTLIBRE : or = new pointlibre (X, Y, Nom, R) ; break ;
        case Element.POINTSURDROITE_DT : or = new PointSurDroite (X, Y, (Droite) tel [0].obj, Nom, R) ; break ;
        case Element.POINTSURCERCLE_CRCL : or = new PointSurCercle (X, Y, (Cercle) tel [0].obj, Nom, R) ; break ;
        case Element.ARCDECERCLE_PT_PT_PT : or = new ArcDeCercle (Nom, R) ; break ;
        case Element.CERCLE_PT_R :
        case Element.CERCLE_PT_PT :
        case Element.CERCLE_PT_PT_PT : or = new Cercle (Nom, R) ; break ;
        case Element.SEGMENT_PT_PT : or = new Segment (Nom, R) ; break ;
        case Element.DEMIDROITE_PT_PT :
        case Element.DEMIDROITE_PT_VECT : or = new DemiDroite (Nom, R) ; break ;

        default : throw new IllegalArgumentException () ;
      }
    calcule (el, or, d) ;
    el.obj = or ;
    addElement (el) ;
  }


/**
 * Calcule l'objet géométrique (ObjetRepere) lié à l'élément.
 */

  private void calcule (Element el, ObjetRepere or, double d)
  { switch (el.id)
      { case Element.PT_X_Y : ((pt) or).point (((Scalaire) tel [0].obj).d, ((Scalaire) tel [1].obj).d) ; break ;
        case Element.PT_MILIEU_PT_PT : ((pt) or).Milieu ((pt) tel [0].obj, (pt) tel [1].obj) ; break ;
        case Element.PT_ROTATION_PT_A : ((pt) or).Rotation ((pt) tel [0].obj, (pt) tel [1].obj, ((Scalaire) tel [2].obj).d) ; break ;
        case Element.PT_HOMOTHETIQUE_PT_PT_K : ((pt) or).Homothetique ((pt) tel [0].obj, (pt) tel [1].obj, ((Scalaire) tel [2].obj).d) ; break ;
        case Element.PT_SYMETRIQUE_PT_PT : ((pt) or).Symetrique ((pt) tel [0].obj, (pt) tel [1].obj) ; break ;
        case Element.POINTLIBRE : ((pointlibre) or).point () ; break ;
        case Element.POINTSURDROITE_DT : ((PointSurDroite) or).point () ; break ;
        case Element.POINTSURCERCLE_CRCL : ((PointSurCercle) or).point () ; break ;
        case Element.POINT_CENTRE : ((point) or).Centre ((Cercle) tel [0].obj) ; break ;
        case Element.POINT_INTER_DT_DT : ((point) or).Intersection ((Droite) tel [0].obj, (Droite) tel [1].obj) ; break ;
        case Element.POINT_INTER1_DT_CRCL : ((point) or).Intersection1 ((Droite) tel [0].obj, (Cercle) tel [1].obj) ; break ;
        case Element.POINT_INTER2_DT_CRCL : ((point) or).Intersection2 ((Droite) tel [0].obj, (Cercle) tel [1].obj) ; break ;
        case Element.POINT_INTER1_CRCL_CRCL : ((point) or).Intersection1 ((Cercle) tel [0].obj, (Cercle) tel [1].obj) ; break ;
        case Element.POINT_INTER2_CRCL_CRCL : ((point) or).Intersection2 ((Cercle) tel [0].obj, (Cercle) tel [1].obj) ; break ;
        case Element.POINT_PROJECTION_PT_DT : ((point) or).Projection ((pt) tel [0].obj, (Droite) tel [1].obj) ; break ;
        case Element.POINT_PROJECTION_PT_SEG : ((point) or).Projection ((pt) tel [0].obj, (Segment) tel [1].obj) ; break ;
        case Element.POINT_SYMETRIQUE_PT_DT : ((point) or).Symetrique ((pt) tel [0].obj, (Droite) tel [1].obj) ; break ;
        case Element.POINT_ROTATION_PT_PT_ARCDECERCLE : ((point) or).Rotation1 ((pt) tel [0].obj, (pt) tel [1].obj, (ArcDeCercle) tel [2].obj) ; break ;
        case Element.POINT_SYMETRIQUE_PT_SEG : ((point) or).Symetrique ((pt) tel [0].obj, (Segment) tel [1].obj) ; break ;
        case Element.POINT_TRANSLATION_PT_VECT : ((point) or).Translation ((pt) tel [0].obj, (Vecteur) tel [1].obj) ; break ;
        case Element.ARCDECERCLE_PT_PT_PT :  ((ArcDeCercle) or).ArcDeCercle ((pt) tel [0].obj, (pt) tel [1].obj, (pt) tel [2].obj) ; break ;
        case Element.CERCLE_PT_R : ((Cercle) or).Cercle ((pt) tel [0].obj, ((Scalaire) tel [1].obj).d) ; break ;
        case Element.CERCLE_PT_PT : ((Cercle) or).Cercle ((pt) tel [0].obj, (pt) tel [1].obj) ; break ;
        case Element.CERCLE_PT_PT_PT : ((Cercle) or).Cercle ((pt) tel [0].obj, (pt) tel [1].obj, (pt) tel [2].obj) ; break ;
        case Element.SEGMENT_PT_PT : ((Segment) or).Segment ((pt) tel [0].obj, (pt) tel [1].obj) ; break ;
        case Element.VECTEUR_PT_PT : ((Vecteur) or).Vecteur ((pt) tel [0].obj, (pt) tel [1].obj) ; break ;
        case Element.VECTEUR_SOMME_V_W_PT : ((Vecteur) or).Somme ((Vecteur) tel [0].obj, (Vecteur) tel [1].obj) ;
                                            ((Vecteur) or).fixeVecteur ((pt) tel [2].obj) ; break ;
        case Element.VECTEUR_PRODUIT_V_K_PT : ((Vecteur) or).Produit ((Vecteur) tel [0].obj, ((Scalaire) tel [2].obj).d) ;
                                              ((Vecteur) or).fixeVecteur ((pt) tel [1].obj) ; break ;
        case Element.VECTEUR_PT_PT_PT : ((Vecteur) or).Vecteur ((pt) tel [0].obj, (pt) tel [1].obj, (pt) tel [2].obj) ; break ;
        case Element.DROITE_PT_PT : ((Droite) or).Droite ((pt) tel [0].obj, (pt) tel [1].obj) ; break ;
        case Element.DROITE_PT_VECT : ((Droite) or).Droite ((pt) tel [0].obj, (Vecteur) tel [1].obj) ; break ;
        case Element.DROITE_A_B_C : ((Droite) or).Droite (((Scalaire) tel [0].obj).d, ((Scalaire) tel [1].obj).d, ((Scalaire) tel [2].obj).d) ; break ;
        case Element.DROITE_PARA_DT_PT : ((Droite) or).Parallele ((Droite) tel [0].obj, (pt) tel [1].obj) ; break ;
        case Element.DROITE_PARA_SEG_PT : ((Droite) or).Parallele ((Segment) tel [0].obj, (pt) tel [1].obj) ; break ;
        case Element.DROITE_PERP_DT_PT : ((Droite) or).Perpendiculaire ((Droite) tel [0].obj, (pt) tel [1].obj) ; break ;
        case Element.DROITE_PERP_SEG_PT : ((Droite) or).Perpendiculaire ((Segment) tel [0].obj, (pt) tel [1].obj) ; break ;
        case Element.DROITE_MEDI_PT_PT : ((Droite) or).Mediatrice ((pt) tel [0].obj, (pt) tel [1].obj) ; break ;
        case Element.DROITE_BISS_PT_PT_PT : ((Droite) or).Bissectrice ((pt) tel [0].obj, (pt) tel [1].obj, (pt) tel [2].obj) ; break ;
        case Element.DEMIDROITE_PT_PT : ((DemiDroite) or).DemiDroite ((pt) tel [0].obj, (pt) tel [1].obj) ; break ;
        case Element.DEMIDROITE_PT_VECT : ((DemiDroite) or).DemiDroite ((pt) tel [0].obj, (Vecteur) tel [1].obj) ; break ;
        case Element.NOMBRE : ((Scalaire) or).Scalaire (d) ;  break ;
        case Element.NOMBRE_PT_ABS : ((Scalaire) or).Scalaire (((pt) tel [0].obj).x) ; break ;
        case Element.NOMBRE_PT_ORD : ((Scalaire) or).Scalaire (((pt) tel [0].obj).y) ; break ;
        case Element.NOMBRE_DISTANCE_PT_DR : ((Scalaire) or).Scalaire (((point) tel [0].obj).Distance ((Droite) tel [1].obj)) ; break ;
        case Element.NOMBRE_DISTANCE_PT_PT : ((Scalaire) or).Scalaire (((pt) tel [0].obj).Distance ((pt) tel [1].obj)) ; break ;
        case Element.NOMBRE_NORME_VECT : ((Scalaire) or).Scalaire (((Vecteur) tel [0].obj).Norme ()) ; break ;
        case Element.NOMBRE_LONGUEUR_SEG : ((Scalaire) or).Scalaire (((Segment) tel [0].obj).Longueur ()) ; break ;
        case Element.NOMBRE_RAYON_CRCL : ((Scalaire) or).Scalaire (((Cercle) tel [0].obj).r) ; break ;
        case Element.NOMBRE_SOMME : ((Scalaire) or).Scalaire (((Scalaire) tel [0].obj).d + ((Scalaire) tel [1].obj).d) ; break ;
        case Element.NOMBRE_DIFFERENCE : ((Scalaire) or).Scalaire (((Scalaire) tel [0].obj).d - ((Scalaire) tel [1].obj).d) ; break ;
        case Element.NOMBRE_PRODUIT : ((Scalaire) or).Scalaire (((Scalaire) tel [0].obj).d * ((Scalaire) tel [1].obj).d) ; break ;
        case Element.NOMBRE_QUOTIENT : ((Scalaire) or).Scalaire (((Scalaire) tel [0].obj).d / ((Scalaire) tel [1].obj).d) ; break ;
        case Element.NOMBRE_OPPOSE : ((Scalaire) or).Scalaire (-((Scalaire) tel [0].obj).d) ; break ;
        case Element.NOMBRE_VABS : ((Scalaire) or).Scalaire (Math.abs (((Scalaire) tel [0].obj).d)) ; break ;
        case Element.NOMBRE_RACINE :
          double d1 = ((Scalaire) tel [0].obj).d ;
          if (d1 >= 0) ((Scalaire) or).Scalaire (Math.sqrt(d1)) ;
          else ((Scalaire) or).defini = false ;
          break ;
        case Element.NOMBRE_COS : ((Scalaire) or).Scalaire (Math.cos (((Scalaire) tel [0].obj).d)) ; break ;
        case Element.NOMBRE_SIN : ((Scalaire) or).Scalaire (Math.sin (((Scalaire) tel [0].obj).d)) ; break ;
        case Element.NOMBRE_ARCCOS : 
          d1 = ((Scalaire) tel [0].obj).d ;
          if (Math.abs(d1) <= 1.0) ((Scalaire) or).Scalaire (Math.acos (d1)) ;
          else ((Scalaire) or).defini = false ;
          break ;
        case Element.NOMBRE_ARCSIN :
          d1 = ((Scalaire) tel [0].obj).d ;
          if (Math.abs(d1) <= 1.0) ((Scalaire) or).Scalaire (Math.asin (d1)) ;
          else ((Scalaire) or).defini = false ;
          break ;
        case Element.NOMBRE_LN :
          d1 = ((Scalaire) tel [0].obj).d ;
          if (d1 > 0.0) ((Scalaire) or).Scalaire (Math.log (d1)) ;
          else ((Scalaire) or).defini = false ;
          break ;
        case Element.NOMBRE_PUISSANCE :
          d1 = ((Scalaire) tel [0].obj).d ;
          double d2 = ((Scalaire) tel [1].obj).d ;
          if ((d1 == 0.0) && (d2 < 0.0) || (d1 <= 0.0) && (d2 != Math.floor(d2))) ((Scalaire) or).defini = false ;
          else ((Scalaire) or).Scalaire (Math.pow (d1, d2)) ;
          break ;

        default : throw new IllegalArgumentException () ;
      }
  }


/**
 * Recalcule les éléments de la figure.
 */

  public void recalcule (int index)
  { Repere R = (Repere) firstElement () ;
    Element el ;
    for (int i = 1 ; i < size () ; i ++)
      { el = ((Element) elementAt (i)) ;
        if ((el.id < 0) || (index <= 0))
          { if (el.id < 0) el.id = - el.id ;
            for (int j = 0 ; j < 3 ; j ++)
              { tel [j] = null ;
                if (el.args [j] > 0) tel [j] = ((Element) elementAt (el.args [j])) ;
              }
            ObjetRepere or = el.obj ;
           if (el.id != el.NOMBRE) calcule (el, or, 0.0) ;
          }
      }
  }

/**
 * Cherche le premier élément à l'emplacement (X, Y) parmi tous les élément dont l'identificateur est
 * compris entre id1 et id2.
 */

  public int zone (int id1, int id2, int X, int Y)
  { Element el ;
      int i ;
      for (i = size () - 1 ; i >= 1 ; i --)
        { el = ((Element) elementAt (i)) ;
          if ((el.id >= id1) && (el.id <= id2) && (el.obj.zone (X, Y))) break ;
        }
      if (i == size ()) return 0 ; else return i ;
  }

/**
 * Supprime un élément et les éléments de figure fabriqués à partir de lui puis recalcule les chaînages.
 */

  public void supprime (int i)
  { if ((i > 0) && (i < size ()))
      { setElementAt (null, i) ;
        for (int k = i + 1 ; k < size () ; k ++)
          { Element el = (Element) elementAt (k) ;
            for (int l = 0 ; l < 3 ; l ++)
              { int m = el.args [l] ;
                if (m >0)
                  { if (elementAt (m) == null)
                      { setElementAt (null, k) ;
                        break ;
                      }
                  }
                else break ;
              }
          }
        int j = i ;
        while (j < size ())
          { if (elementAt (j) == null)
              { removeElementAt (j) ;
                for (int k = j + 1 ; k < size () ; k ++)
                  { Element el = (Element) elementAt (k) ;
                    if (el != null)
                      for (int l = 0 ; l < 3 ; l ++)
                        { int m = el.args [l] ;
                          if (m >0) { if (m > j) el.args [l] -- ; }
                          else break ;
                        }
                  }
              }
            else j ++ ;
          }
      }
  }

}
