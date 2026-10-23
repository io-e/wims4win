
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

// vecteur.java - 09/12/98

package geo ;

import java.awt.Graphics ;

/**
 * Vecteur.
 */

public class Vecteur extends ObjetRepere
{ 

/**
 * Abscisse du vecteur.
 */

public double x ;

/**
 * Ordonnée du vecteur.
 */

public double y ;

/**
 * Abscisse de l'origine du représentant du vecteur.
 */

public double Ax ;

/**
 * Ordonnée de l'origine du représentant du vecteur.
 */

public double Ay ;

/**
 * Construit le vecteur de coordonnées (x, y).
 */

  public Vecteur (double x, double y, String Nom, Repere R)
  { super (Nom, R) ; Vecteur (x, y) ; }

/**
 * Construit un vecteur non défini.
 */

  public Vecteur (String Nom, Repere R)
  { super (Nom, R) ; }

/**
 * Construit le vecteur AB.
 */

  public Vecteur (pt A, pt B, String Nom, Repere R)
  { super (Nom, R) ; Vecteur (A, B) ; }

/**
 * Construit le vecteur AB et positionne l'origine du représentant en C.
 */

  public Vecteur (pt A, pt B, pt C, String Nom, Repere R)
  { super (Nom, R) ; Vecteur (A, B, C) ; }

/**
 * Retourne la somme des vecteurs v et w.
 */

  public void Somme (Vecteur v, Vecteur w)
  { defini = v.defini && w.defini ;
    x = v.x + w.x ;
    y = v.y + w.y ;
  }

/**
 * Retourne le produit du vecteur v par le réel k.
 */

  public void Produit (Vecteur v, double k)
  { defini = v.defini ;
    x = k * v.x ;
    y = k * v.y ;
  }

/**
 * Retourne la norme du vecteur ou -1 si le vecteur n'est pas défini.
 */

  public double Norme ()
  { if (defini) return Math.sqrt (x * x + y * y) ; else return -1.0 ; }

/**
 * Teste si la souris est proche du vecteur.
 */

   public boolean zone (int X, int Y)
   { if (defini)
       { double xs = R.Abs (X) ; double ys = R.Ord (Y) ;
         double d = Math.abs (y * xs - x * ys + Ay * x - Ax * y) / Math.sqrt (x * x + y * y) ;
         if ((d <= 4.0) && ((xs - Ax) * (xs - Ax - x) <= 0) && ((ys - Ay) * (ys - Ay - y) <= 0))
           return true ;
         else return false ;
       }
     return false ;
   }

/**
 * Traçage d'un représentant d'un vecteur.
 */

  public void trace (Graphics g)
  { if (defini)
      { int AX = R.Iabs (Ax) ; int AY = R.Iord (Ay) ;
        double l = Math.sqrt (x * x + y * y) ;
        if (l != 0.0)
          { int BX = AX + (int) (x * R.unitex) ;
            int BY = AY - (int) (y * R.unitey) ;
            g.drawLine (AX, AY, BX, BY) ;
            int X = (int) (x * 4.0 / l) ;
            int Y = (int) (y * 4.0 / l) ;
            g.drawLine (BX, BY, BX - X - Y, BY + Y - X) ;
            g.drawLine (BX, BY, BX - X + Y, BY + Y + X) ;
          }
        else g.fillRect (AX - 2, AY - 2, 5, 5) ;
        if (Nom.length () != 0)
        g.drawString (Nom, R.Iabs (x / 2.0 + Ax) + 3, R.Iord (y / 2.0 + Ay) - 3);
      }
  }

/**
 * Fixe l'origine du vecteur en A.
 */

  public void fixeVecteur (pt A)
  { defini = defini && A.defini ;
    Ax = A.x ; Ay = A.y ;
  }

/**
 * Redéfinit le vecteur de coordonnées (x, y).
 */

  public void Vecteur (double x, double y)
  { defini = true ; this.R = R ; this.x = x ; this.y = y ; Ax = Ay = 0.0 ; }

/**
 * Redéfinit le vecteur AB.
 */

  public void Vecteur (pt A, pt B)
  { defini = A.defini && B.defini ;
    if (defini) { x = B.x - A.x ; y = B.y - A.y ; Ax = A.x ; Ay = A.y ; }
  }

/**
 * Redéfinit le vecteur AB et repositionne l'origine du représentant en C.
 */

  public void Vecteur (pt A, pt B, pt C)
  { defini = A.defini && B.defini && C.defini ;
    if (defini) { x = B.x - A.x ; y = B.y - A.y ; Ax = C.x ; Ay = C.y ; }
  }
 
}


