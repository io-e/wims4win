
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
 * Droite d'équation a x + b y + c = 0 (a et b non tous nuls).
 */

public class Droite extends ObjetRepere
{

/**
 * Coefficient de l'équation de la droite.
 */

  public double a, b, c;

/**
 * Construit une droite non définie.
 */

  public Droite (String Nom, Repere R)
  { super (Nom, R); }

/**
 * Construit la copie de la droite d.
 */

  public Droite (Droite d, String Nom, Repere R)
  { super (Nom, R); Droite (d) ; }

/**
 * Construit la droite AB.
 */

  public Droite (pt A, pt B, String Nom, Repere R)
  { super (Nom, R); Droite (A, B) ; }

/**
 * Construit la droite passant par A et de vecteur directeur u.
 */

  public Droite (pt A, Vecteur u, String Nom, Repere R)
  { super (Nom, R); Droite (A, u) ; }

/**
 * Construit la droite passant par les points de coordonnées (xA, yA) et (xB, yB).
 */

  public Droite (double xA, double yA, double xB, double yB, String Nom, Repere R)
  { super (Nom, R); Droite (xA, yA, xB, yB) ; }

/**
 * Construit la droite d'équation a x + b y + c = 0.
 */

  public Droite (double a, double b, double c, String Nom, Repere R)
  { super (Nom, R); Droite (a, b, c) ; }

/**
 * Construit la droite à partir du segment s.
 */

  public Droite (Segment s, String Nom, Repere R)
  { super (Nom, R); Droite (s) ; }

/**
 * Définit la droite comme parallèle à la droite d et passant par A.
 */

  public void Parallele (Droite d, pt A)
  { a = d.a; b = d.b; c = - (d.a * A.x + d.b * A.y);
    defini = d.defini; }

/**
 * retourne la parallèle au segment s passant par A
 * 
 */

  public void Parallele (Segment s, pt A)
  { a = s.By - s.Ay; b = s.Ax - s.Bx;
    c = - (a * A.x + b * A.y);
    defini = ((a != 0.0) || (b != 0.0)) && (A.defini) ;
  }

/**
 * Définit la droite comme perpendiculaire à la droite d et passant par A.
 */

  public void Perpendiculaire (Droite d, pt A)
  { a = - d.b; b = d.a; c = d.b * A.x - d.a * A.y;
    defini = d.defini && A.defini ; }

/**
 * retourne la perpendiculaire du segment s passant par A
 * 
 */

  public void Perpendiculaire (Segment s, pt A)
  { b = s.By - s.Ay; a = s.Bx - s.Ax;
    c = - (a * A.x + b * A.y);
    defini = ((a != 0.0) || (b != 0.0)) && A.defini ;
  }

/**
 * Définit la droite comme médiatrice de [AB].
 */

  public void Mediatrice (pt A, pt B)
  { defini = A.defini && B.defini && ((A.x != B.x) || (A.y != B.y)) ;
    if (defini) { a = B.x - A.x ; b = B.y - A.y ;
                  c = (A.x * A.x - B.x * B.x + A.y * A.y - B.y * B.y) * 0.5 ;
                }
  }

/**
 * Définit la droite comme bissectrice de l'angle ABC.
 */

  public void Bissectrice (pt A, pt B, pt C)
  { defini = A.defini && B.defini && C.defini ;
    if (defini) { double bcx = C.x - B.x ; double bcy = C.y - B.y ;
                  double bc = Math.sqrt (bcx * bcx + bcy * bcy) ;
                  double bax = A.x - B.x ; double bay = A.y - B.y ;
                  double ba = Math.sqrt (bax * bax + bay * bay) ;
                  bax = bc * bax ; bay = bc * bay ; bcx = ba * bcx ; bcy = ba * bcy ;
                  Droite (B.x, B.y, B.x + bax + bcx, B.y + bay + bcy) ;
                }
  }

/**
 * Teste si la souris est proche de la droite.
 */

   public boolean zone (int X, int Y)
   { if (defini)
       { double A = a / R.unitex ; double B = b / R.unitey ;
         double d = Math.abs (a * R.Abs (X) + b * R.Ord (Y) + c) / Math.sqrt (A * A + B * B) ;
         if (d <= 4.0) return true ; else return false ;
       }
     return false ;
   }

/**
 * Trace ou efface la droite.
 */

  public void trace (Graphics g)
  { int N, X1, Y1, X2, Y2;
    X1 = Y1 = X2 = Y2 = 0;
    if (defini)
      { if (Math.abs (b) >= Math.abs (a))
          { X1 = 0;
            Y1 = R.Iord (-(a * R.Abs (0) + c) / b);
            X2 = R.XMAX; 
            Y2 = R.Iord (-(a * R.Abs (X2) + c) / b); }
        else
          { Y1 = 0;
            X1 = R.Iabs (-(b * R.Ord (0) + c) / a);
            Y2 = R.YMAX;
            X2 = R.Iabs (-(b * R.Ord (Y2) + c) / a); }
            g.drawLine (X1, Y1, X2, Y2) ;
        if (Nom.length () != 0)
          { int i ;
            if (Math.abs (b) >= Math.abs (a))
              { i = R.XMAX / 2 ;
                g.drawString (Nom, i + 3, R.Iord (- (a * R.Abs (i) + c) / b) - 3) ;
              }
            else
              { i = R.YMAX / 2 ;
                g.drawString (Nom, R.Iabs (- (b * R.Ord (i) + c) / a) + 3, i - 3) ;
              }
          }
      }
  }

/**
 * Redéfinit la droite passant par A et B.
 */

  public void Droite (pt A, pt B)
  { a = B.y - A.y; b = A.x - B.x ;
    c = A.y * B.x - A.x * B.y ;
    defini = ((a != 0.0) || (b != 0.0)) && A.defini && B.defini ; }

/**
 * Redéfinit la droite passant par A et de vecteur directeur u.
 */

  public void Droite (pt A, Vecteur u)
  { defini = A.defini && u.defini && (u.x != 0.0 || u.y != 0.0) ;
    a = u.y ; b = - u.x ;
    c = A.y * u.x - A.x * u.y ; }

/**
 * Redéfinit la droite passant par les points de coOrdonnées (xA, yA) et (xB, yB).
 */

  public void Droite (double xA, double yA, double xB, double yB)
  { a = yB - yA ; b = xA - xB ;
    c = yA * xB - xA * yB ; defini = (a != 0.0) || (b != 0.0) ;
  }

/**
 * Redéfinit la droite d'équation a x + b y + c = 0.
 */

  public void Droite (double a, double b, double c)
  { this.a = a ; this.b = b ; this.c = c ; defini = (a != 0.0) | (b != 0.0) ; }

/**
 * Redéfinit la droite à partir de d.
 */

  public void Droite (Droite d)
  { a = d.a; b = d.b; c = d.c; defini = d.defini ; }

/**
 * Redéfinit la droite à partir du segment s.
 */

  public void Droite (Segment s)
  { defini = s.defini ;
    if (defini) Droite (s.Ax, s.Ay, s.Bx, s.By) ;
  }


}
