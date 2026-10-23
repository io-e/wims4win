/*    Copyright (C) 1998 XIAO, Gang of Universite de Nice - Sophia Antipolis
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/* Testing file for wims.
 * It contains a routine exec_test() for testing
 * purposes during development. Not documented, and don't
 * touch this in usual modules. */

/* No automatic substitution is done when this
 * routine is called. */

#include "wims.h"
void exec_test(char *p)
{
  if(!trusted_module()) module_error("not_trusted");
}

