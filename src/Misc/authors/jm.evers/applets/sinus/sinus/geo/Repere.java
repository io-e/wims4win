
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

package geo ;

import java.awt.Graphics ;

/**
 * Définit un repère orthogonal.
 * Le point de coordonnées réelles (x, y) a pour position dans la fenêtre d'affichage (X, Y)
 * où l'entier X varie de 0 à XMAX et Y de 0 à YMAX (vers le bas).
 * (XO, YO) donne la position de l'origine sur l'écran et unitex, unitey fixent les unités sur les
 * deux axes.
 */

public class Repere
{

/**
 * Abscisse de l'origine du repère dans la fenêtre d'affichage.
 */

  public int X0 ;

/**
 * Ordonnée de l'origine du repère dans la fenêtre d'affichage.
 */

  public int Y0 ;

/**
 * Abscisse maximale.
 */

   public int XMAX ;

/**
 *
 * Ordonnée maximale.
 */

   public int YMAX ;

/**
 * Valeur en points "écran" de l'unité suivant l'axe des x ; unitex = unitey pour un repère orthonormal.
 */

  public double unitex ;

/**
 * Valeur en points "écran" de l'unité suivant l'axe des y.
 */

  public double unitey ;

/**
 * Construit le repère orthogonal.
 */

  public Repere (int X0,int Y0,int XMAX,int YMAX,double unitex,double unitey)
  { this.X0 = X0; this.Y0 = Y0;
    this.XMAX = XMAX ; this.YMAX = YMAX ;
    this.unitex = unitex ; this.unitey = unitey ;
  }

/**
 * Trace le repère.
 */

  public void trace (Graphics g)
  { int UX = X0 + new Double (unitex) . intValue () ;
    int UY = Y0 - new Double (unitey) . intValue () ;
    if ((Y0 >= 0) && (Y0 <= YMAX)) { g.drawLine (0, Y0, XMAX, Y0) ;
                                        if ((UX >= 0) && (UX <= XMAX))
                                              g.drawLine (UX, Y0 - 2, UX, Y0 + 2) ;
                                      } 
    if ((X0 >= 0) && (X0 <= XMAX)) { g.drawLine (X0, 0, X0, YMAX) ;
                                        if ((UY >= 0) && (UY <= YMAX))
                                              g.drawLine (X0 - 2, UY, X0 + 2, UY);
                                      }
    }

/**
 * Trace un cadre.
 */

  public void cadre (Graphics g)
  { g.drawRect (0, 0, XMAX - 1, YMAX - 1) ; }

/**
 * Retourne la position X sur l'écran d'un point d'abscisse x.
 */

  public int Iabs (double x)
  { return (int) (x * unitex + X0); }

/**
 * Retourne la position Y sur l'écran d'un point d'abscisse y.
 */

  public int Iord (double y)
  { return (int) (- y * unitey + Y0); }

/**
 * Retourne l'abscisse x d'un point placé en position (X, Y) sur l'écran.
 */

  public double Abs (int X)
  { return (X - X0) / unitex ; }

/**
 * Retourne l'ordonnée y d'un point placé en position (X, Y) sur l'écran.
 */

  public double Ord (int Y)
  { return (Y0 - Y) / unitey ; }

/**
 * Retourne "true" si le point placé en position (X, Y) sur l'écran est affichable.
 */

  public boolean affiche (int X, int Y)
  { return ((X >= 0) && (X <= XMAX) && (Y >= 0) && (Y <= YMAX)); }

/**
 * Mise à jour du repère orthogonal.
 */

  public void Repere (int X0,int Y0,int XMAX,int YMAX,double unitex,double unitey)
  { this.X0 = X0; this.Y0 = Y0;
    this.XMAX = XMAX ; this.YMAX = YMAX ;
    this.unitex = unitex ; this.unitey = unitey ;
  }

}

