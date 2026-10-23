
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

// geo/point.java - 02/12/98

package geo ;

/**
 * Super classe contenant tous les points calculés tels les points d'intersections de Droites et Cercles.
 */

public class point extends pt
{

/**
 * Construit un point non défini.
 */

  public point (String Nom, Repere R)
  { super (Nom, R) ; }

/**
 * Construit une copie du point A.
 */

  public point (pt A, String Nom, Repere R)
  { super (A, Nom, R) ; }

/**
 * Construit le point placé en (X, Y) sur la fenêtre d'affichage.
 */

  public point (int X, int Y, String Nom, Repere R)
  { super (X, Y, Nom, R) ; }

/**
 * Construit le point de coordonnées (x, y).
 */

  public point (double x, double y, String Nom, Repere R)
  { super (x, y, Nom, R) ; }

/**
 * Donne le milieu du Segment s.
 */

  public void Milieu (Segment s)
  { defini = s.defini ;
    if (defini) { x = (s.Ax + s.Bx) / 2.0 ; y = (s.Ay + s.By) / 2.0 ; majXY () ; }
  }

/**
 * Donne l'intersection des droites d1 et d2.
 */

  public void Intersection (Droite d1, Droite d2)
  { double det = d1.a * d2.b - d2.a * d1.b ;
    defini = (d1.defini) && (d2.defini) && (det != 0.0) ;
    if (defini) { x = (d2.c * d1.b - d1.c * d2.b) / det ;
                  y = (d2.a * d1.c - d1.a * d2.c) / det ;
                  majXY () ; }
  }
/**
 * Donne l'intersection des segments s1 et s2.
 */

  public void Intersection (Segment s1, Segment s2)
  { double a1 = s1.By - s1.Ay ; double b1 = s1.Ax - s1.Bx ; double c1 = s1.Ay * s1.Bx - s1.Ax * s1.By ;
    double a2 = s2.By - s2.Ay ; double b2 = s2.Ax - s2.Bx ; double c2 = s2.Ay * s2.Bx - s2.Ax * s2.By ;
    double det = a1 * b2 - a2 * b1 ;
    defini = (s1.defini) && (s2.defini) && (det != 0.0) ;
    if (defini) { x = (c2 * b1 - c1 * b2) / det ;
                  y = (a2 * c1 - a1 * c2) / det ;
                  majXY () ; }
  }

/**
 * Donne le premier point d'intersection, s'il existe, de la droite d et du cercle c.
 */

  public void Intersection1 (Droite d, Cercle c)
  { i12 (d, c, 1.0) ; }


/**
 * Donne le second point d'intersection, s'il existe, de la Droite d et du Cercle c.
 */

  public void Intersection2 (Droite d, Cercle c)
  { i12 (d, c, -1.0) ; }

  private void i12 (Droite d, Cercle c, double signe)
  { defini = (d.defini) && (c.defini) ;
    if (defini) { if (d.b != 0.0) { double u = d.a * (d.c + d.b * c.yo) - d.b * d.b * c.xo ;
                                    double v = d.a * d.a + d.b * d.b ;
                                    double w = d.c + d.b * c.yo ;
                                    double deltap = u * u - v * (d.b * d.b * (c.xo * c.xo - c.r * c.r) + w * w) ;
                                    if (deltap >= 0.0) { x = (- u + signe * Math.sqrt (deltap)) / v ;
                                                         y = - (d.a * x + d.c) / d.b ; }
                                    else               defini = false ;
                                  }
                  else            { x = - d.c / d.a ;
                                    double u = x - c.xo ;
                                    double v = c.r * c.r - u * u ;
                                    if (v >= 0.0) y = c.yo + signe * Math.sqrt (v) ;
                                    else          defini = false ;
                                  }
                  majXY () ;
                }
  }

/**
 * Donne le premier point d'intersection de deux cercles quand celui-ci existe.
 */

  public void Intersection1 (Cercle c1, Cercle c2)
  { icc (c1, c2, 1.0) ; }


/**
 * Donne le second point d'intersection de 2 Cercles quand celui-ci existe.
 */

  public void Intersection2 (Cercle c1, Cercle c2)
  { icc (c1, c2, -1.0) ; }

  private void icc (Cercle c1, Cercle c2, double signe)
  { if (defini = c1.defini && c2.defini)
  	            { double a1 = -2.0 * c1.xo ; double b1 = -2.0 * c1.yo ;
                  double a2 = -2.0 * c2.xo ; double b2 = -2.0 * c2.yo ;
                  double a12 = a1 - a2 ; double b12 = b1 - b2 ;
                  double a = a12 * a12 + b12 * b12 ;
                  if (defini = (a != 0.0))
                              { double C1 = c1.xo * c1.xo + c1.yo * c1.yo - c1.r * c1.r ;
                                double C2 = c2.xo * c2.xo + c2.yo * c2.yo - c2.r * c2.r ;
                                double C12 = C1 - C2 ;
                                double b, c;
                                boolean bpga = Math.abs(b12) >= Math.abs(a12) ;
                                if (bpga)
                                   { b = 2.0 * a12 * C12 + b12 * (b1 * a2 - a1 * b2) ;
                                     c = C12 * C12 + b12 * (b1 * C2 - b2 * C1) ;
                                   }
                                else
                                   { b = 2.0 * b12 * C12 + a12 * (a1 * b2 - b1 * a2) ;
                                     c = C12 * C12 + a12 * (a1 * C2 - a2 * C1) ;
                                   }
                                double delta = b * b - 4.0 * a * c ;
                                if (defini = (delta >= 0.0))
                                            { x = -0.5 * (b + signe * Math.sqrt (delta)) / a ;
                                              if (bpga) y = - (a12 * x + C12) / b12 ;
                                              else
                                              { y = x ; x = - (b12 * x + C12) / a12 ;
                                              }
                                              majXY () ;
                                            }
                              }
                }
  }

/**
 * Donne le centre d'un cercle.
 */

  public void Centre (Cercle c)
  { defini = c.defini ;
    if (defini) { x = c.xo ; y = c.yo ; majXY () ; }
  }

/**
 * Donne le projeté d'un point sur une Droite.
 */

  public void Projection (pt A, Droite d)
  { defini = A.defini && d.defini ;
    if (defini) { double u = (d.a * A.x + d.b * A.y + d.c) / (d.a * d.a + d.b * d.b) ;
                  x = A.x - u * d.a ; y = A.y - u * d.b ;
                  majXY () ;
                }
  }

/**
 * Donne le projeté d'un point sur un segment (le point peut être à l'extérieur du segment).
 */

  public void Projection (pt A, Segment s)
  { double a, b ;
    a = s.Ay - s.By ; b = s.Bx - s.Ax ;
    defini = A.defini && ((a != 0.0) || (b != 0.0)) ;
    if (defini) { double c = - (a * s.Ax + b * s.Ay) ;
                  double u = (a * A.x + b * A.y + c) / (a * a + b * b) ;
                  x = A.x - u * a ; y = A.y - u * b ;
                  majXY () ;
                }
  }

/**
 * Donne le symétrique de A par rapport à la droite d.
 */

  public void Symetrique (pt A, Droite d)
// modifié le 27/09/99 pour autoriser C.Symetrique (C, d) ;
  { double Ax = A.x ; double Ay = A.y ; Projection (A, d) ;
    if (defini) { x = 2.0 * x - Ax ; y = 2.0 * y - Ay ; majXY () ; }
  }

/**
 * Donne le symétrique de A par rapport au segment s.
 */

  public void Symetrique (pt A, Segment s)
  { double Ax = A.x ; double Ay = A.y ; Projection (A, s) ;
    if (defini) { x = 2.0 * x - Ax ; y = 2.0 * y - Ay ; majXY () ; }
  }

/**
 * Donne l'image de A par la rotation de centre O et d'angle correspondant à l'arc de cercle ARC.
 */

  public void Rotation1 (pt A, pt O, ArcDeCercle ARC)
  { if (defini = ARC.defini) Rotation (A, O, ARC.b) ;
  }

/**
 * Retourne la distance du point à une droite.
 */

   public double Distance (Droite d)
   { if (defini && d.defini) return Math.abs (d.a * x + d.b * y + d.c) / Math.sqrt (d.a * d.a + d.b * d.b) ;
     else                    return -1.0 ;
   }


/**
 * Retourne la distance du point à un segment.
 */

   public double Distance (Segment s)
   { double resultat = -1.0 ;
     if (defini && s.defini) { double a = s.By - s.Ay ; double b = s.Ax - s.Bx ;
                               if ((a != 0.0) || (b != 0.0))
                resultat = Math.abs (a * x + b * y + s.Ay * s.Bx - s.Ax * s.By) / Math.sqrt (a * a + b * b) ;
                             }
   return resultat ;
   }

/**
 * Donne l'image du point A par la translation de vecteur v.
 */

   public void Translation (pt A, Vecteur v)
   { defini = A.defini && v.defini ;
     if (defini)
       { x = A.x + v.x ; y = A.y + v.y ;
         majXY () ;
       }
   }

}
