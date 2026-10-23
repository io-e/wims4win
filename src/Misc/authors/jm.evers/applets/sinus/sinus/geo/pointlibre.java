
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

// pointlibre.java - 28/11/98

package geo ;

import java.awt.Graphics ;

/**
 * définit un point dont l'emplacement peut être modifié à la souris
 *
 */

public class pointlibre extends pt
{ public int Xp, Yp ;
  public boolean deplace ;

/**
 * construit un point libre à l'emplacement (X, Y)
 *
 */

  public pointlibre (int X, int Y, String Nom, Repere R)
  { super (X, Y, Nom, R) ; Xp = X ; Yp = Y ; deplace = false ; }

/**
 * construit un point libre de coordonnées (x, y)
 *
 */

  public pointlibre (double x, double y, String Nom, Repere R)
  { super (x, y, Nom, R) ; Xp = X ; Yp = Y ; deplace = false; }

/**
 * teste si la souris est proche du point libre, retourne alors deplace = true
 *
 */

  public boolean zone (int X, int Y)
  { if (deplace = (X >=  this.X - 2) && (X <= this.X + 2) && (Y >= this.Y - 2) && (Y <= this.Y + 2))
    Xp = this.X ; Yp = this.Y ;
    return deplace ; }

/**
 * pour un point en cours de déplacement, met à jour (X, Y) et met dans (Xp, Yp) les anciennes
 * coordonnées "écran"
 *
 */

  public void bouge (int X, int Y)
  { if (deplace && R.affiche (X, Y)) { this.X = X ; x = R.Abs (X) ;
                                       this.Y = Y ; y = R.Ord (Y) ;
                                     }
  }

/**
 * efface le point (utilisé uniquement par les points libres)
 *
 */

  public void efface (Graphics g)
  { if (defini && deplace)
      { g.fillRect (Xp - 2, Yp - 2, 5, 5) ;
        if (Nom.length () != 0) g.drawString (Nom, Xp + 3, Yp - 3) ;
        Xp = X ; Yp = Y ;
      }
  }

/**
 * Mise à jour d'un point libre.
 */

 public void point ()
 { majXY () ; Xp = X ; Yp = Y ; }

}

