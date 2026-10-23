
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
 * Nombre lié à un repère.
 */

public class Scalaire extends ObjetRepere
{

/**
 * Scalaire.
 */

  public double d ;

/**
 * Construit un nombre non défini.
 */

  public Scalaire (String Nom, Repere R )
  { super (Nom, R ) ; }

/**
 * Construit un nombre.
 */

  public Scalaire (double d, String Nom, Repere R)
  { super (Nom, R ) ; this.d = d ; defini = true ; }

/**
 * Donne l'angle de deux vecteurs.
 */

  public void Angle (Vecteur u, Vecteur v)
  { defini = u.defini && v.defini &&
             (u.x != 0.0 || u.y != 0.0) && (v.x != 0.0 || v.y != 0.0) ;
    if (defini)
      { d = Math.acos ((u.x * v.x + u.y * v.y) / (u.Norme () * v.Norme ())) ;
        if ((u.x * v.y - u.y * v.x) < 0.0) d = - d ;
      }
  }

/**
 *Redéfinit le scalaire.
 */

  public void Scalaire (double d)
  { this.d = d ; defini = true ; }

}
