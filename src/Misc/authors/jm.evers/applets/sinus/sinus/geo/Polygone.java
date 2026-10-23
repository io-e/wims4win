
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

// Polygone.java - 12/12/98

package geo ;

import java.awt.Graphics ;
import java.awt.Polygon ;

/**
 * Polygone
 */

public class Polygone extends ObjetRepere
{

  public double xi [] ;
  public double yi [] ;

/**
 * uitilisé pour le traçage : "true" pour un remplissage de la figure, "false" pour le dessin du bord uniquement
 */

  public boolean plein ;

  public Polygon p ;

/**
 * Construit un polygone non défini.
 */

  public Polygone (String Nom, Repere R)
  { super (Nom, R) ; plein = false ; }

/**
 * Construit le polygone A1, A2, ..., An, A1.
 */

  public Polygone (pt Ai [], int n, boolean type, String Nom, Repere R)
  { super (Nom, R) ; Polygone (Ai, n, type) ; }

/**
 * Construit le polygone avec des coordonnees de points.
 */

  public Polygone (double xi [], double yi [], int n, boolean type, String Nom, Repere R)
  { super (Nom, R) ; Polygone (xi, yi, n, type) ; }

/**
 * Trace ou efface le polygone.
 */

  public void trace (Graphics g)
  { if (defini) if (plein) g.fillPolygon (p) ; else g.drawPolygon (p) ;
        if ((defini) && (Nom.length () != 0) && (p.npoints > 0))
          g.drawString (Nom, p.xpoints [0] + 2 , p.ypoints [0] + 2) ;
  }

/**
 * Teste si le curseur de la souris est proche du polygone.
 */

   public boolean zone (int X, int Y)
   { boolean resultat = false ;
     if (defini) for (int i = 0 ; i < p.npoints ; i ++)
       { int AX = p.xpoints [i] ;
         int AY = p.xpoints [i] ;
         int BX = (i == p.npoints - 1) ? p.xpoints [0] : p.xpoints [i + 1] ;
         int BY = (i == p.npoints - 1) ? p.ypoints [0] : p.ypoints [i + 1] ;
         int A = BY - AY ; int B = AX - BX ;
         double d = ((double) Math.abs (A * X + B * Y + AY * BX - AX * BY)) / Math.sqrt ((double) (A * A + B * B)) ;
         if (resultat = ((d <= 4.0) && ((X - AX) * (X - BX) <= 0) && ((Y - AY) * (Y - BY) <= 0))) break ;
       }
     return resultat ;
   }

/**
 * Redéfinit le polygone.
 */

  public void Polygone (pt Ai [], int n, boolean type)
  { defini = true ;
    for (int i = 0 ; (i < n) && defini ; i ++) defini = defini && Ai [i].defini ;
    if (defini)
      { if ((xi == null) || (n != xi.length))
          { xi = new double [n] ;
            yi = new double [n] ;
          }
        for (int i = 0 ; i < n ; i ++)
          { xi [i] = Ai [i].x ;
            yi [i] = Ai [i].y ;
          }
        maj (n) ;
        plein = type ;
      }
  }

  private void maj (int n)
  { if ((p == null) || (p.npoints != n + 1))
      { p = new Polygon () ;
        for (int i = 0 ; i < n ; i ++) p.addPoint (R.Iabs (xi [i]), R.Iord (yi [i])) ;
        p.addPoint (R.Iabs (xi [0]), R.Iord (yi [0])) ;
      }
    else 
      { for (int i = 0 ; i < n ; i ++)
          { p.xpoints [i] = R.Iabs (xi [i]) ;
            p.ypoints [i] = R.Iord (yi [i]) ;
          }
        p.xpoints [n] = R.Iabs (xi [0]) ;
        p.ypoints [n] = R.Iord (yi [0]) ;
      }
  }

/**
 * Redéfinit le polygone.
 */

  public void Polygone (double xi [], double yi [], int n, boolean type)
  { defini = true ;
    if ((this.xi == null) || (n != xi.length))
      { this.xi = new double [n] ;
        this.yi = new double [n] ;
      }
    for (int i = 0 ; i < n ; i ++)
      { this.xi [i] = xi [i] ;
        this.yi [i] = yi [i] ;
      }
    maj (n) ;
    plein = type ;
  }

}
