
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
 * Courbe d'une fonction.
 */

public class CourbeDeFonction extends ObjetRepere
{
  Fonction f ;

/**
 * Définit la courbe à partir d'une fonction.
 */

  public CourbeDeFonction (Fonction f, String Nom, Repere R)
  { super (Nom, R) ; this.f = f ; }


/**
 * Trace ou efface la courbe.
 */

  public void trace (Graphics g)
  { int X, Y ;
    double x ;
    for (X = 0 ; X <= R.XMAX ; X++)
      { x = R.Abs (X) ;
        if (f.defini (x))
          { Y = R.Iord (f.Image (x)) ;
            if ((Y >=0) && (Y <= R.YMAX)) g.drawLine (X, Y, X, Y) ;
          }
      }
    if (Nom.length () != 0)
      { int U = R.XMAX / 2 ;
        for (X = R.XMAX / 2 ; X >= 0 ; X --)
          { x = R.Abs (X) ;
            if (f.defini (x))
              { Y = R.Iord (f.Image (x)) ;
                if ((Y >=0) && (Y <= R.YMAX))
                  { g.drawString (Nom, X + 3, Y - 3) ;
                    break ;
                  }
              }
            x = R.Abs (U) ;
            if (f.defini (x))
              { Y = R.Iord (f.Image (x)) ;
                if ((Y >=0) && (Y <= R.YMAX))
                  { g.drawString (Nom, X + 3, Y - 3) ;
                    break ;
                  }
              }
            U ++ ;
          }
      }
  }
}
