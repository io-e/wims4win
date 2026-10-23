
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
 * Fonction abstraite à étendre.
 */

public abstract class Fonction
{ 

/**
 * Retourne "true" si l'image de x existe.
 */

  public abstract boolean defini (double x) ;

/**
 * Retourne l'image de x.
 */

  public abstract double Image (double x) ;

}