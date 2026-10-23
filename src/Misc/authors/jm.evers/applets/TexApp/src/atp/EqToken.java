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


class EqToken
{

    public EqToken(int i, String s)
    {
        typ = i;
        stringS = s;
    }

    public EqToken(int i)
    {
        typ = i;
        stringS = "";
    }

    public EqToken()
    {
        typ = 0;
        stringS = "";
    }

    public int typ;
    public String stringS;
    public static final int EOF = 0;
    public static final int Id = 1;
    public static final int Num = 2;
    public static final int BeginSym = 3;
    public static final int EndSym = 4;
    public static final int ANGLE = 5;
    public static final int AndSym = 7;
    public static final int DBackSlash = 8;
    public static final int FUNC = 9;
    public static final int SUP = 10;
    public static final int SUB = 11;
    public static final int FRAC = 12;
    public static final int SQRT = 13;
    public static final int VEC = 14;
    public static final int ARRAY = 15;
    public static final int LEFT = 16;
    public static final int RIGHT = 17;
    public static final int SYMBOP = 18;
    public static final int SYMBOPD = 19;
    public static final int SYMBOLBIG = 20;
    public static final int ACCENT = 22;
    public static final int LIM = 24;
    public static final int SpaceChar = 25;
    public static final int BEGIN = 50;
    public static final int END = 51;
    public static final int Null = 99;
    public static final int Invalid = 100;
    public static final int Op = 108;
    public static final int Paren = 109;
    public static final int NOT = 110;
    public static final int SPACE = 113;
    public static final int CHOOSE = 114;
    public static final int ATOP = 115;
    public static final int OverLINE = 116;
    public static final int UnderLINE = 117;
    public static final int OverBRACE = 118;
    public static final int UnderBRACE = 119;
    public static final int STACKREL = 120;
    public static final int FGColor = 121;
    public static final int BGColor = 122;
    public static final int FBOX = 123;
    public static final int MBOX = 124;
}
