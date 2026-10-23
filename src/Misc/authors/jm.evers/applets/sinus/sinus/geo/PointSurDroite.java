
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

/**
 * Définit un point d'une droitedont on peut modifier l'emplacement.
 */

public class PointSurDroite extends pointlibre
{ public Droite d ;

/**
 * Construit un point de position modifiable, projeté de M sur d.
 */

  public PointSurDroite (int X, int Y, Droite d, String Nom, Repere R)
  { super (R.Abs (X), R.Ord (Y), Nom, R) ; this.d = d ; point () ; }

/**
 * Construit un point de position modifiable, projeté de M (x, y) sur d.
 */

  public PointSurDroite (double x, double y, Droite d, String Nom, Repere R)
  { super (x, y, Nom, R) ; this.d = d ; point () ; }

/**
 * Pour un point en cours de déplacement, met à jour (X, Y) et met dans (Xp, Yp) les anciennes
 * coordonnées "écran".
 */

  public void bouge (int X, int Y)
  { if (deplace && defini && R.affiche (X, Y))
      { Xp = this.X ; Yp = this.Y ;
        x = R.Abs (X) ; y = R.Ord (Y) ;
        double k = (d.a * x + d.b * y + d.c) / (d.a * d.a + d.b * d.b) ;
        x = x - k * d.a; y = y - k * d.b ;
        this.X = R.Iabs (x); this.Y = R.Iord (y) ;
      }
  }

/**
 * Redéfinit le point en cas de modification de la Droite d.
 */

  public void point ()
  { if (d.defini) { double k = (d.a * x + d.b * y + d.c) / (d.a * d.a + d.b * d.b) ;
                    x = x - k * d.a ; y = y - k * d.b ;
                    Xp = X = R.Iabs (x) ; Yp = Y = R.Iord (y) ; }
    defini = d.defini;
  }                   

}
