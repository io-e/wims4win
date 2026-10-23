
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
 * Segment.
 */

public class Segment extends ObjetRepere
{

/**
 * Position sur l'écran d'une extrémité du segment.
 */

  public int AX, AY, BX, BY;

/**
 * Coordonnée d'une extrémité du segment.
 */

  public double Ax, Ay, Bx, By ;

/**
 * Construit un segment non défini.
 */

  public Segment (String Nom, Repere R)
  { super (Nom, R) ; }

/**
 * Construit le segment [AB].
 */

  public Segment (pt A, pt B, String Nom, Repere R)
  { super (Nom, R) ; Segment (A, B) ; }

/**
 * Construit le segment d'extrémités les points de coordonnées (Ax, Ay) et (Bx, By).
 */

  public Segment (double Ax, double Ay, double Bx, double By, String Nom, Repere R)
  { super (Nom, R) ; Segment (Ax, Ay, Bx, By) ; }

/**
 * Retourne la longueur du segment.
 */

  public double Longueur ()
  { if (defini) { double u = Bx - Ax ; double v = By - Ay ;
                  return Math.sqrt (u * u + v * v) ;
                }
    else        return -1.0 ;
  }

/**
 * Trace ou efface le segment.
 */

  public void trace (Graphics g)
  { if (defini)
      { g.drawLine (AX, AY, BX, BY) ;
        if (Nom.length () != 0) g.drawString (Nom, (AX + BX) / 2 + 3, (AY + BY) / 2 - 3) ;
      }
  }

/**
 * Teste si la souris est proche du segment.
 */

   public boolean zone (int X, int Y)
   { if (defini)
       { int A = BY - AY ; int B = AX - BX ;
         double d = ((double) Math.abs (A * X + B * Y + AY * BX - AX * BY)) / Math.sqrt ((double) (A * A + B * B)) ;
         if ((d <= 4.0) && ((X - AX) * (X - BX) <= 0) && ((Y - AY) * (Y - BY) <= 0))
           return true ;
         else return false ;
       }
     return false ;
   }

/**
 * Redéfinit le Segment [AB].
 */

  public void Segment (pt A, pt B)
  { defini = A.defini && B.defini ;
    if (defini) { AX = A.X ; AY = A.Y ; BX = B.X ; BY = B.Y ;
                  Ax = A.x ; Ay = A.y ; Bx = B.x ; By = B.y ;
                }
  }

/**
 * Redéfinit le Segment d'extrémités les points de coordonnées (Ax, Ay) et (Bx, By).
 */

  public void Segment (double Ax, double Ay, double Bx, double By)
  { defini = true ;
    if (defini) { this.Ax = Ax ; this.Ay = Ay ; this.Bx = Bx ; this.By = By ;
                  AX = R.Iabs (Ax) ; AY = R.Iord (Ay) ; BX = R.Iabs (Bx) ; BY = R.Iord (By) ;
                }
  }
}
