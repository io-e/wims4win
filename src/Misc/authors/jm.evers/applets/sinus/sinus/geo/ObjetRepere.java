
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
 * Objet géométrique lié à un repère.
 */

public class ObjetRepere
{

/**
 * Repere associé.
 */

  public Repere R ;

/**
 * Existence de l'objet.
 */

  public boolean defini ;

/**
 * Nom de l'objet.
 */

  public String Nom ;

/**
 * Construit un objet non défini.
 */

  public ObjetRepere (String Nom, Repere R )
  { this.Nom = Nom ; this.R = R ; defini = false ; }

/**
 * Test la proximité de la souris.
 */

  public boolean zone (int X, int Y)
  { return false ; }

/**
 * Trace l'objet.
 */

  public void trace (Graphics g)
  {  }

}
