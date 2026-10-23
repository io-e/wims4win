
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
import geo.ObjetRepere ;
import geo.pt ;
import geo.Vecteur ;

/**
 * Demi-droite [AB).
 */

public class DemiDroite extends ObjetRepere
{

/**
 * Coordonnée de A.
 */

  public double Ax, Ay ;

/**
 * Coordonnée de B.
 */

  public double Bx, By ;

/**
 * Construit la demi-droite [AB).
 */

  public DemiDroite (pt A, pt B, String Nom, Repere R)
  { super (Nom, R) ; DemiDroite (A, B) ; }

/**
 * Construit une demi-droite non définie.
 */

  public DemiDroite (String Nom, Repere R)
  { super (Nom, R) ; }

/**
 * Construit la demi-droite [AB) à partir des coordonnées respectives (Ax, Ay) et (Bx, By) de A et B.
 */

  public DemiDroite (double Ax, double Ay, double Bx, double By, String Nom, Repere R)
  { super (Nom, R) ; DemiDroite (Ax, Ay, Bx, By) ; }

/**
 * Construit la demi-droite [Au où u est un vecteur.
 */

  public DemiDroite (pt A, Vecteur u, String Nom, Repere R)
  { super (Nom, R) ; DemiDroite (A, u) ; }

/**
 * Teste si la souris est proche de la demi-droite.
 */

   public boolean zone (int X, int Y)
   { if (defini)
       { double x = R.Abs (X) ; double y = R.Ord (Y) ;
         double a = By - Ay ; double b = Ax - Bx ;
         double A = a / R.unitex ; double B = b / R.unitey ;
         double d = Math.abs (a * x + b * y + Ay * Bx - Ax * By) / Math.sqrt (A * A + B * B) ;
         double xax = x - Ax ; double xbx = x - Bx ; double yay = y - Ay ; double yby = y - By ;
         if ((d <= 4.0) && ((xax * xbx <= 0) && (yay * yby <= 0) || (xax * xax + yay * yay >= xbx * xbx + yby * yby)))
           return true ;
         else return false ;
       }
     return false ;
   }

/**
 * Trace ou efface le DemiDroite.
 */

  public void trace (Graphics g)
  { int N, X1, Y1, X2, Y2 ;
    X1 = Y1 = X2 = Y2 = 0 ;
    double a = By - Ay ; double b = Ax - Bx ; double c = Ay * Bx - Ax * By ;
    if (defini) { if (Math.abs (b) >= Math.abs (a)) if (b < 0.0) { X1 = R.Iabs (Ax) ; Y1 = R.Iord (Ay) ;
                                                                   X2 = R.XMAX ;
                                                                   Y2 = R.Iord (-(a * R.Abs (X2) + c) / b) ;
                                                                 }
                                                    else         { X1 = 0 ;
                                                                   Y1 = R.Iord (-(a * R.Abs (X1) + c) / b) ;
                                                                   X2 = R.Iabs (Ax) ; Y2 = R.Iord (Ay) ;
                                                               }
                                               else if (a > 0.0) { Y1 = 0 ;
                                                                   X1 = R.Iabs (-(b * R.Ord (0) + c) / a);
                                                                   X2 = R.Iabs (Ax) ; Y2 = R.Iord (Ay) ;
                                                                 }
                                                    else         { X1 = R.Iabs (Ax) ; Y1 = R.Iord (Ay) ;
                                                                   Y2 = R.YMAX;
                                                                   X2 = R.Iabs (-(b * R.Ord (Y2) + c) / a) ;
                                                                 }
                  g.drawLine (X1, Y1, X2, Y2);
                  if (Nom.length () != 0) g.drawString (Nom, (X1 + X2) / 2 + 3, (Y1 + Y2) / 2 - 3) ;
                }
  }




/**
 * Redéfinit la demi-droite [AB].
 */

  public void DemiDroite (pt A, pt B)
  { defini = A.defini && B.defini && ((A.x != B.x) || (A.y != B.y)) ;
    if (defini) { Ax = A.x ; Ay = A.y ; Bx = B.x ; By = B.y ; }
  }

/**
 * Redéfinit la demi-droite à partir des points de coordonnées (Ax, Ay) et (Bx, By).
 */

  public void DemiDroite (double Ax, double Ay, double Bx, double By)
  { defini = ((Ax != Bx) || (Ay != By)) ;
    if (defini) { this.Ax = Ax ; this.Ay = Ay ; this.Bx = Bx ; this.By = By ; }
  }

/**
 * Redéfinit la demi-droite [Au où u est un vecteur.
 */

  public void DemiDroite (pt A, Vecteur u)
  { defini = ((u.x != 0.0) || (u.y != 0.0)) ;
    if (defini) { Ax = A.x ; Ay = A.y ; Bx = A.x + u.x ; By = A.y + u.y ; }
  }

}
