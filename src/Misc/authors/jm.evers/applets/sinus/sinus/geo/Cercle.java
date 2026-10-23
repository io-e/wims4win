
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

// Cercle.java _ 26/11/98

package geo ;

import java.awt.Graphics ;

/**
 * Cercle de centre de coordonnées (xo, yo) de rayon r relativement au repère R.
 */

public class Cercle extends ObjetRepere
{ 

/**
 * Abscisse du centre.
 */

  public double xo ;

/**
 * Ordonnée du centre.
 */

  public double yo ;

/**
 * Rayon du cercle.
 */

  public double r;

/**
 * Construit le cercle de centre O de rayon r relativement au repère R.
 */

  public Cercle (pt O, double r, String Nom, Repere R)
  { super (Nom, R) ; Cercle (O, r) ; }

/**
 * Construit un cercle non défini.
 */

  public Cercle (String Nom, Repere R)
  { super (Nom, R) ; }

/**
 * Construit le cercle de centre de coordonnées (xo, yo) de rayon r relativement au repère R.
 */

  public Cercle (double xo, double yo, double r, String Nom, Repere R)
  { super (Nom, R) ; Cercle (xo, yo, r) ; }

/**
 * Construit le cercle de centre O passant par A.
 */

  public Cercle (pt O, pt A, String Nom, Repere R)
  { super (Nom, R) ; Cercle (O, A) ; }

/**
 * Construit le cercle passant par les points A, B et C.
 */

  public Cercle (pt A, pt B, pt C, String Nom, Repere R)
  { super (Nom, R) ; Cercle (A, B, C) ; }


/**
 * Teste si la souris est proche du cercle.
 */

   public boolean zone (int X, int Y)
   { if (defini)
       { double d = R.Abs (X) - xo ; double d1 = R.Ord (Y) - yo ;
         d = Math.abs (Math.sqrt (d * d + d1 * d1) - r) * (R.unitex + R.unitey) ;
         if (d <= 2.0) return true ; else return false ;
       }
     return false ;
   }

/**
 * Trace ou efface le Cercle.
 */

  public void trace (Graphics g)
  { if (defini) { int Xo, Yo, RAYONX, RAYONY ;
                  Xo = R.Iabs (xo); Yo = R.Iord (yo);
                  RAYONX = (int) (R.unitex * r) ;
                  RAYONY = (int) (R.unitey * r) ;
                  g.drawOval (Xo - RAYONX, Yo - RAYONY, 2 * RAYONX, 2 * RAYONY);
                  if (Nom.length () != 0 )
                    { int X = R.Iabs (xo + r * 0.7) + 3 ;
                      int Y = R.Iord (yo + r * 0.7) - 3 ;
                      g.drawString (Nom, X, Y) ;
                    }
                 }
  }

/**
 * Redéfinit le Cercle de centre O de rayon r.
 */

  public void Cercle (pt O, double r)
  { xo = O.x; yo = O.y; this.r = r; defini = (r >= 0.0); }

/**
 * Redéfinit le Cercle de centre de coordonnées (xo, yo) de rayon r.
 */

  public void Cercle (double xo, double yo, double r)
  { this.xo = xo; this.yo = yo; this.r = r; defini = (r >= 0.0); }

/**
 * Redéfinit le Cercle de centre O passant par A.
 */

  public void Cercle (pt O, pt A)
  { defini = O.defini && A.defini ;
    this.xo = O.x ; this.yo = O.y ;
    r = A.x - O.x ; double d = A.y - O.y ; r = Math.sqrt (r * r + d * d) ; 
  }

/**
 * Redéfinit le Cercle passant par les points A, B et C.
 */

  public void Cercle (pt A, pt B, pt C)
  { defini = A.defini && B.defini && C.defini ;
    if (defini) { double det = A.x * (B.y - C.y) + B.x * (C.y - A.y) + C.x * (A.y - B.y) ;
                  defini = (det != 0.0) ;
                  if (defini) { double a = A.x * A.x + A.y * A.y ;
                                double b = B.x * B.x + B.y * B.y ;
                                double c = C.x * C.x + C.y * C.y ;
                                xo = (a * (B.y - C.y) + b * (C.y - A.y) + c * (A.y - B.y)) / (det + det) ;
                                yo = (a * (C.x - B.x) + b * (A.x - C.x) + c * (B.x - A.x)) / (det + det) ;
       r = (a * (B.y * C.x - C.y * B.x) + b * (C.y * A.x - A.y * C.x) + c * (A.y * B.x - B.y * A.x)) / det ;
                                r = xo * xo + yo * yo - r ;
                                defini = (r >= 0.0) ;
                                if (defini) r = Math.sqrt (r) ;
                              }
                  }
  }

}
