/*
Copyright 2006 Stefan Muller and Christian Schmid
This file is part of the HotEqn package.

HotEqn is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; 
HotEqn is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package atp;


class BoxC
{

    public BoxC(int i, int j, int k)
    {
        dx = i;
        dy_pos = j;
        dy_neg = k;
    }

    public BoxC()
    {
        dx = 0;
        dy_pos = 0;
        dy_neg = 0;
    }

    public int dx;
    public int dy_pos;
    public int dy_neg;
}
