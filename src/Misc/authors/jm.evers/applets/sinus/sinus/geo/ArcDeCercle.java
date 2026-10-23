// ArcDeCercle - 02/12/98 - 07/09/99 - 10/01/2001

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
 * Définit un arc de cercle de centre de coordonnées (xo, yo) de rayon r relativement au repère R
 * partant de A (xo + r cos a, yo + r sin a) allant vers B (xo + r cos (a+b), yo + r sin (a+b))
 * dans le sens trigonométrique si b > 0 dans le sens contraire si b < 0.
 */

public class ArcDeCercle extends ObjetRepere
{

/**
 * Coordonnée du centre de l'arc de cercle.
 */

public double xo, yo ;

/**
 * Rayon de l'arc de cercle.
 */

  public double r ;

/**
 * Angle correspondant à une extrémité de l'arc de cercle.
 */

  public double a, b ;

/**
 * Construit un arc de cercle non défini.
 */

  public ArcDeCercle (String Nom, Repere R)
  { super (Nom, R) ; }

/**
 * Construit l'arc de cercle de centre O de rayon r relativement au repère R.
 */

  public ArcDeCercle (pt O, double r, double a, double b, String Nom, Repere R)
  { super (Nom, R) ; ArcDeCercle (O, r, a, b) ; }

/**
 * Construit l'arc de cercle de centre de coordonnées (xo, yo) de rayon r relativement au repère R.
 */

  public ArcDeCercle (double xo, double yo, double r, double a, double b, String Nom, Repere R)
  { super (Nom, R) ; ArcDeCercle (xo, yo, r, a, b) ; }

/**
 * Construit l'arc de cercle de centre O passant par A.
 */

  public ArcDeCercle (pt O, pt A, double a, double b, String Nom, Repere R)
  { super (Nom, R) ; ArcDeCercle (O, A, a, b) ; }

/**
 * Construit l'arc de cercle correspondant à l'angle AOB.
 */

  public ArcDeCercle (pt A, pt O, pt B, String Nom, Repere R)
  { super (Nom, R) ; ArcDeCercle (A, O, B) ; }

/**
 * Trace ou efface l'arc de cercle.
 */

  public void trace (Graphics g)
  { if (defini) { int B = (int)(b * 180.0 / Math.PI) ;
                  int B1 = B % 360 ;
                  if (B1 != 0)
                    { int Xo = R.Iabs (xo) ; int Yo = R.Iord (yo);
                      int RX = (int) (R.unitex * r) ; int RY = (int) (R.unitey * r) ;
                      g.drawArc (Xo - RX, Yo - RY, 2 * RX, 2 * RY,
                                 (int)(a * 180.0 / Math.PI), B) ;
                      pt p = new pt (xo + r * Math.cos (a), yo + r * Math.sin (a), "", R) ;
                      p.Rotation (p, new pt (xo, yo, "", R), b) ;
                      double x = p.x - xo ; double y = p.y - yo ;
                      double l = Math.sqrt (x * x + y * y) ;
                      B = B % 360 ;
                      if (( B > 0) && (B <= 180) || (B < -180)) y = - y ; else x = - x ;
                      int X = (int) (y * 4.0 / l) ;
                      int Y = (int) (x * 4.0 / l) ;
                      g.drawLine (p.X, p.Y, p.X - X - Y, p.Y + Y - X) ;
                      g.drawLine (p.X, p.Y, p.X - X + Y, p.Y + Y + X) ;

                    }
                  if (Nom.length () != 0 )
                    { int X = R.Iabs (xo + r * Math.cos (a + b / 2)) + 3 ;
                      int Y = R.Iord (yo + r * Math.sin (a + b / 2)) - 3 ;
                      g.drawString (Nom, X, Y) ;
                    }
                }
  }

/**
 * Teste si la souris est proche de l'arc de cercle.
 */

   public boolean zone (int X, int Y)
   { if (defini)
       { double dx = R.Abs (X) - xo ; double dy = R.Ord (Y) - yo ;
         if (Math.abs (Math.sqrt (dx * dx + dy * dy) - r) * (R.unitex + R.unitey) > 3.0)
           return false ;
         else
           { Scalaire s = new Scalaire ("", R) ; s.Angle (new Vecteur (1.0, 0.0, "", R), new Vecteur (dx, dy, "", R)) ;
//10/01/2001
             double angle = na (s.d - a) ;
             double bt = na (b) ;
//
             return (((bt >= 0.0) && (angle >= 0.0) && (angle <= bt)) || ((bt < 0.0) && (angle <= 0.0) && (angle >= bt))) ;
           }
       }
     return false ;
   }

//10/01/2001
   private double na (double a)
   { double pi2 = 2.0 * Math.PI ;
     while (a > Math.PI) a -= pi2 ;
     while (a <= - Math.PI) a += pi2 ;
     return a ;
   }
//

/**
 * Redéfinit l'arc de cercle de centre O de rayon r.
 */

  public void ArcDeCercle (pt O, double r, double a, double b)
  { xo = O.x ; yo = O.y ; this.r = r ; this.a = a ; this.b = b ; defini = (r >= 0.0); }

/**
 * Redéfinit l'arc de cercle de centre de coordonnées (xo, yo) de rayon r relativement au repère R.
 */

  public void ArcDeCercle (double xo, double yo, double r, double a, double b)
  { this.xo = xo; this.yo = yo; this.r = r ; this.a = a ; this.b = b ; defini = (r >= 0.0); }

/**
 * Redéfinit l'arc de cercle de centre O passant par A.
 */

  public void ArcDeCercle (pt O, pt A, double a, double b)
  { defini = O.defini && A.defini ;
    this.xo = O.x ; this.yo = O.y ;
    r = A.x - O.x ; double d = A.y - O.y ; r = Math.sqrt (r * r + d * d) ;
    this.a = a ; this.b = b ;
  }

/**
 * Redéfinit l'arc de cercle correspondant à l'angle AOB.
 */

  public void ArcDeCercle (pt A, pt O, pt B)
  { defini = O.defini && A.defini && B.defini ;
    if (defini)
      { xo = O.x ; yo = O.y ;
        Vecteur u = new Vecteur (1.0, 0.0, "", R) ;
        Vecteur v = new Vecteur (A.x - O.x, A.y - O.y, "", R) ;
        r = v.Norme () ;
        Scalaire S = new Scalaire ("", R) ;
        S.Angle (u, v) ;
        if (defini = S.defini)
          { a = S.d ;
            u.Vecteur (B.x - O.x, B.y - O.y) ;
            S.Angle (v, u) ;
            if (defini = S.defini) b = S.d ;
          }
      }
  }
}
