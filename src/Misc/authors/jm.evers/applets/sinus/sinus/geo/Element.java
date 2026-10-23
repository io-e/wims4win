
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

// geo/Element.java - 09/12/98

package geo ;

import java.awt.* ;

/**
 * Définit un élement d'une figure géométrique.
 */

public class Element
{

/**
 * Valeur de l'identificateur de l'élement de la figure.
 */

  public final static int PT_X_Y =                            1 ;
  public final static int PT_MILIEU_PT_PT =                   2 ;
  public final static int PT_ROTATION_PT_A =                  3 ;
  public final static int PT_HOMOTHETIQUE_PT_PT_K =           4 ;
  public final static int PT_SYMETRIQUE_PT_PT =               5 ;
  public final static int POINTLIBRE =                        6 ;
  public final static int POINTSURDROITE_DT =                 7 ;
  public final static int POINTSURCERCLE_CRCL =               8 ;
  public final static int POINT_CENTRE =                      9 ;
  public final static int POINT_INTER_DT_DT =                10 ;
  public final static int POINT_INTER1_DT_CRCL =             11 ;
  public final static int POINT_INTER2_DT_CRCL =             12 ;
  public final static int POINT_INTER1_CRCL_CRCL =           13 ;
  public final static int POINT_INTER2_CRCL_CRCL =           14 ;
  public final static int POINT_PROJECTION_PT_DT =           15 ;
  public final static int POINT_PROJECTION_PT_SEG =          16 ;
  public final static int POINT_SYMETRIQUE_PT_DT =           17 ;
  public final static int POINT_SYMETRIQUE_PT_SEG =          18 ;
  public final static int POINT_ROTATION_PT_PT_ARCDECERCLE = 19 ;
  public final static int POINT_TRANSLATION_PT_VECT =        20 ;
  public final static int ARCDECERCLE_PT_PT_PT =             21 ;
  public final static int CERCLE_PT_R =                      22 ;
  public final static int CERCLE_PT_PT =                     23 ;
  public final static int CERCLE_PT_PT_PT =                  24 ;
  public final static int SEGMENT_PT_PT =                    25 ;
  public final static int VECTEUR_PT_PT =                    26 ;
  public final static int VECTEUR_SOMME_V_W_PT =             27 ;
  public final static int VECTEUR_PRODUIT_V_K_PT =           28 ;
  public final static int VECTEUR_PT_PT_PT =                 29 ;
  public final static int DROITE_PT_PT =                     30 ;
  public final static int DROITE_PT_VECT =                   31 ;
  public final static int DROITE_A_B_C =                     32 ;
  public final static int DROITE_PARA_DT_PT =                33 ;
  public final static int DROITE_PARA_SEG_PT =               34 ;
  public final static int DROITE_PERP_DT_PT =                35 ;
  public final static int DROITE_PERP_SEG_PT =               36 ;
  public final static int DROITE_MEDI_PT_PT =                37 ;
  public final static int DROITE_BISS_PT_PT_PT =             38 ;
  public final static int DEMIDROITE_PT_PT =                 39 ;
  public final static int DEMIDROITE_PT_VECT =               40 ;
  public final static int NOMBRE =                           41 ;
  public final static int NOMBRE_PT_ABS =                    42 ;
  public final static int NOMBRE_PT_ORD =                    43 ;
  public final static int NOMBRE_DISTANCE_PT_DR =            44 ;
  public final static int NOMBRE_DISTANCE_PT_PT =            45 ;
  public final static int NOMBRE_NORME_VECT =                46 ;
  public final static int NOMBRE_LONGUEUR_SEG =              47 ;
  public final static int NOMBRE_RAYON_CRCL =                48 ;
  public final static int NOMBRE_SOMME =                     49 ;
  public final static int NOMBRE_DIFFERENCE =                50 ;
  public final static int NOMBRE_PRODUIT =                   51 ;
  public final static int NOMBRE_QUOTIENT =                  52 ;
  public final static int NOMBRE_OPPOSE =                    53 ;
  public final static int NOMBRE_VABS =                      54 ;
  public final static int NOMBRE_RACINE =                    55 ;
  public final static int NOMBRE_COS =                       56 ;
  public final static int NOMBRE_SIN =                       57 ;
  public final static int NOMBRE_ARCCOS =                    58 ;
  public final static int NOMBRE_ARCSIN =                    59 ;
  public final static int NOMBRE_LN =                        60 ;
  public final static int NOMBRE_PUISSANCE =                 61 ;

/**
 * Identificateur de l'élement de la figure.
 */

  public int id ;

/**
 * Couleur de l'objet.
 */

  public Color couleur ;

/**
 * Objet géométrique.
 */

  public ObjetRepere obj ;

/**
 * Indique l'adresse des éléments servant à construire l'élément courant.
 */

  public int args [] = {0, 0, 0} ;

/**
 * Construction de l'élément.
 */

 public Element (int id, Color couleur, ObjetRepere obj, int arg0, int arg1, int arg2)
 { this.id = id ; this.couleur = couleur ; this.obj = obj ;
   args [0] = arg0 ; args [1] = arg1 ; args [2] = arg2 ; }

/**
 * Construction d'un élément vide.
 */

 public Element (int id)
 { this.id = id ; }

/**
 * Effacement (graphique) de l'objet.
 */

  public void efface (Graphics g)
  { if ((id >= PT_X_Y) && (id < NOMBRE))
      switch (id) 
        { case POINTLIBRE :
          case POINTSURDROITE_DT :
          case POINTSURCERCLE_CRCL : 
            if (((pointlibre) obj).deplace)
              { ((pointlibre) obj).efface (g) ;
                break ;
              }
          default : obj.trace (g) ;
        }
  }

/**
 * Tracé de l'objet.
 */

  public void trace (Graphics g)
  { if ((id >= PT_X_Y) && (id < NOMBRE) && (couleur != Color.white))
      { g.setColor (couleur) ;
        obj.trace (g) ;
      }
  }

}

