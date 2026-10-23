
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
 * définit un point dont on peut modifier l'emplacement mais qui appartient à un cercle
 * c est le cercle contenant le point
 * a est l'angle (en radians) correspondant à la position du point sur le cercle c
 */

public class PointSurCercle extends pointlibre
{ public double a;
  public Cercle c;

/**
 * Construit un point sur le cercle c.
 */

  public PointSurCercle (double a, Cercle c, String Nom, Repere R)
  { super (c.xo + c.r * Math.cos (a), c.yo + c.r * Math.sin (a), Nom, R);
    this.c = c ; defini = c.defini ;
    this.a = a % (Math.PI * 2) ; if (this.a > Math.PI) this.a = this.a - 2.0 * Math.PI ;
  }

/**
 * Construit un point sur le cercle c.
 */

  public PointSurCercle (int X, int Y, Cercle c, String Nom, Repere R)
  { super (R.Abs (X), R.Ord (Y), Nom, R) ;
    defini = c.defini ; this.c = c ;
    if (defini) a = calcule (X, Y, c) ; else a = 0.0 ;
  }


  private double calcule (int X, int Y, Cercle c)
  { double dx, dy ;
    dx = X - R.Iabs (c.xo) ;
    dy = R.Iord (c.yo) - Y ;
    if (dx != 0.0) a = Math.atan (dy / dx) ;
      else { a = Math.PI / 2 ; if (dy < 0) a = - a ; }
    if (dx < 0) a = a + Math.PI ;
   if (a > Math.PI) a = a - 2.0 * Math.PI ;
   return a ;
  }

/**
 * Pour un point en cours de déplacement, met à jour (X, Y) et met dans (Xp, Yp) les anciennes
 * coordonnées "écran".
 */

  public void bouge (int X, int Y)
  { if (deplace && defini && R.affiche (X, Y)) { Xp = this.X ; Yp = this.Y ;
                                                 a = calcule (X, Y, c) ;
                                                 x = c.xo + c.r * Math.cos (a) ;
                                                 y = c.yo + c.r * Math.sin (a) ;
                                                 this.X = R.Iabs (x) ; this.Y = R.Iord (y) ;
                                               }
  }

/**
 * Redéfinit le point en cas de modification du cercle c.
 */

  public void point ()
  { x = c.xo + c.r * Math.cos (a) ; y = c.yo + c.r * Math.sin (a) ;
    Xp = X = R.Iabs (x) ; Yp = Y = R.Iord (y) ; 
    defini = c.defini; }                   

}
