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

import java.io.PrintStream;
import java.util.Vector;

// Referenced classes of package atp:
//            EqToken

class EqScanner
{

    public EqScanner(String s)
    {
        EOF = false;
        TokenV = new Vector(50, 50);
        selectB = false;
        collectB = false;
        selectCount1 = 0;
        selectCount2 = 0;
        selectSB = new StringBuffer("");
        token = new EqToken(99);
        setEquation(s);
    }

    public String getSelectedArea(int i, int j)
    {
        selectCount1 = Math.min(i, j);
        selectCount2 = Math.max(i, j);
        selectB = true;
        selectSB = new StringBuffer("");
        setEquation(equation);
        selectB = false;
        return selectSB.toString();
    }

    public void setEquation(String s)
    {
        equation = s;
        int i = 0;
        boolean flag = false;
        boolean flag1 = false;
        EOF = false;
        countT = -1;
        count = -1;
        TokenV.removeAllElements();
        advance();
        do
        {
            if(EOF)
                break;
            countT++;
            if(selectB && countT == selectCount1)
                collectB = true;
            TokenV.addElement(ScanNextToken());
            if(selectB && countT == selectCount2)
                collectB = false;
        } while(true);
        countT = -1;
        for(; i < TokenV.size(); i++)
        {
            if(((EqToken)TokenV.elementAt(i)).typ == 114)
            {
                int k = i - 1;
                int j1 = 0;
                do
                {
                    if(k <= 0)
                        break;
                    if(((EqToken)TokenV.elementAt(k)).typ == 4)
                        j1--;
                    else
                    if(((EqToken)TokenV.elementAt(k)).typ == 3)
                        j1++;
                    if(j1 == 1)
                        break;
                    k--;
                } while(true);
                int i2 = i + 1;
                j1 = 0;
                do
                {
                    if(i2 >= TokenV.size())
                        break;
                    if(((EqToken)TokenV.elementAt(i2)).typ == 4)
                        j1++;
                    else
                    if(((EqToken)TokenV.elementAt(i2)).typ == 3)
                        j1--;
                    if(j1 == 1)
                        break;
                    i2++;
                } while(true);
                if(j1 == 1 && k >= 0)
                {
                    TokenV.insertElementAt(new EqToken(109, ")"), i2 + 1);
                    TokenV.insertElementAt(new EqToken(17), i2 + 1);
                    TokenV.setElementAt(new EqToken(4), i);
                    TokenV.insertElementAt(new EqToken(3), i + 1);
                    TokenV.insertElementAt(new EqToken(115), k);
                    TokenV.insertElementAt(new EqToken(109, "("), k);
                    TokenV.insertElementAt(new EqToken(16), k);
                    i += 4;
                }
                continue;
            }
            if(((EqToken)TokenV.elementAt(i)).typ != 115)
                continue;
            int l = i - 1;
            int k1 = 0;
            do
            {
                if(l <= 0)
                    break;
                if(((EqToken)TokenV.elementAt(l)).typ == 4)
                    k1--;
                else
                if(((EqToken)TokenV.elementAt(l)).typ == 3)
                    k1++;
                if(k1 == 1)
                    break;
                l--;
            } while(true);
            if(l >= 0)
            {
                TokenV.setElementAt(new EqToken(4), i);
                TokenV.insertElementAt(new EqToken(3), i + 1);
                TokenV.insertElementAt(new EqToken(115), l);
                i += 2;
            }
        }

        for(int j = 0; j < TokenV.size() - 2; j++)
        {
            if(((EqToken)TokenV.elementAt(j)).typ != 13 || ((EqToken)TokenV.elementAt(j + 1)).typ != 109)
                continue;
            int i1 = j + 2;
            int l1 = 0;
            int j2 = 1;
            for(; i1 < TokenV.size(); i1++)
            {
                if(((EqToken)TokenV.elementAt(i1)).typ == 4)
                    l1--;
                else
                if(((EqToken)TokenV.elementAt(i1)).typ == 3)
                    l1++;
                if(l1 != 0)
                    continue;
                if(((EqToken)TokenV.elementAt(i1)).stringS.equals("["))
                    j2++;
                else
                if(((EqToken)TokenV.elementAt(i1)).stringS.equals("]"))
                    j2--;
                if(j2 != 0)
                    continue;
                TokenV.setElementAt(new EqToken(4), i1);
                break;
            }

            j++;
        }

    }

    public void start()
    {
        countT = -1;
    }

    public int get_count()
    {
        return countT;
    }

    public void set_count(int i)
    {
        countT = i;
    }

    public EqToken nextToken()
    {
        countT++;
        if(countT >= TokenV.size())
        {
            countT = TokenV.size() - 1;
            return new EqToken(99);
        } else
        {
            return (EqToken)TokenV.elementAt(countT);
        }
    }

    public boolean EoT()
    {
        return countT == TokenV.size() - 1;
    }

    private char getChar()
    {
        return equation.charAt(count);
    }

    private void advance()
    {
        if(collectB)
            selectSB.append(equation.charAt(count));
        if(count < equation.length() - 1)
        {
            count++;
            EOF = false;
        } else
        {
            count = equation.length();
            EOF = true;
        }
    }

    private EqToken ScanNextToken()
    {
        StringBuffer stringbuffer = new StringBuffer("");
        String s = new String("");
        EqToken eqtoken = new EqToken();
        boolean flag = false;
        do
        {
            if(EOF)
                break;
            char c = getChar();
            switch(c)
            {
            case 9: // '\t'
            case 10: // '\n'
            case 13: // '\r'
                advance();
                break;

            case 32: // ' '
                advance();
                return new EqToken(25, new String(" "));

            case 33: // '!'
            case 35: // '#'
            case 42: // '*'
            case 43: // '+'
            case 44: // ','
            case 45: // '-'
            case 47: // '/'
            case 58: // ':'
            case 59: // ';'
            case 60: // '<'
            case 61: // '='
            case 62: // '>'
            case 126: // '~'
                advance();
                return new EqToken(108, String.valueOf(c));

            case 123: // '{'
                advance();
                return new EqToken(3);

            case 125: // '}'
                advance();
                return new EqToken(4);

            case 40: // '('
            case 41: // ')'
            case 91: // '['
            case 93: // ']'
            case 124: // '|'
                advance();
                return new EqToken(109, String.valueOf(c));

            case 38: // '&'
                advance();
                return new EqToken(7);

            case 39: // '\''
            case 64: // '@'
            case 65: // 'A'
            case 66: // 'B'
            case 67: // 'C'
            case 68: // 'D'
            case 69: // 'E'
            case 70: // 'F'
            case 71: // 'G'
            case 72: // 'H'
            case 73: // 'I'
            case 74: // 'J'
            case 75: // 'K'
            case 76: // 'L'
            case 77: // 'M'
            case 78: // 'N'
            case 79: // 'O'
            case 80: // 'P'
            case 81: // 'Q'
            case 82: // 'R'
            case 83: // 'S'
            case 84: // 'T'
            case 85: // 'U'
            case 86: // 'V'
            case 87: // 'W'
            case 88: // 'X'
            case 89: // 'Y'
            case 90: // 'Z'
            case 97: // 'a'
            case 98: // 'b'
            case 99: // 'c'
            case 100: // 'd'
            case 101: // 'e'
            case 102: // 'f'
            case 103: // 'g'
            case 104: // 'h'
            case 105: // 'i'
            case 106: // 'j'
            case 107: // 'k'
            case 108: // 'l'
            case 109: // 'm'
            case 110: // 'n'
            case 111: // 'o'
            case 112: // 'p'
            case 113: // 'q'
            case 114: // 'r'
            case 115: // 's'
            case 116: // 't'
            case 117: // 'u'
            case 118: // 'v'
            case 119: // 'w'
            case 120: // 'x'
            case 121: // 'y'
            case 122: // 'z'
                stringbuffer.append(c);
                advance();
                boolean flag1 = false;
                do
                {
                    if(EOF || flag1)
                        break;
                    c = getChar();
                    switch(c)
                    {
                    case 39: // '\''
                    case 64: // '@'
                    case 65: // 'A'
                    case 66: // 'B'
                    case 67: // 'C'
                    case 68: // 'D'
                    case 69: // 'E'
                    case 70: // 'F'
                    case 71: // 'G'
                    case 72: // 'H'
                    case 73: // 'I'
                    case 74: // 'J'
                    case 75: // 'K'
                    case 76: // 'L'
                    case 77: // 'M'
                    case 78: // 'N'
                    case 79: // 'O'
                    case 80: // 'P'
                    case 81: // 'Q'
                    case 82: // 'R'
                    case 83: // 'S'
                    case 84: // 'T'
                    case 85: // 'U'
                    case 86: // 'V'
                    case 87: // 'W'
                    case 88: // 'X'
                    case 89: // 'Y'
                    case 90: // 'Z'
                    case 97: // 'a'
                    case 98: // 'b'
                    case 99: // 'c'
                    case 100: // 'd'
                    case 101: // 'e'
                    case 102: // 'f'
                    case 103: // 'g'
                    case 104: // 'h'
                    case 105: // 'i'
                    case 106: // 'j'
                    case 107: // 'k'
                    case 108: // 'l'
                    case 109: // 'm'
                    case 110: // 'n'
                    case 111: // 'o'
                    case 112: // 'p'
                    case 113: // 'q'
                    case 114: // 'r'
                    case 115: // 's'
                    case 116: // 't'
                    case 117: // 'u'
                    case 118: // 'v'
                    case 119: // 'w'
                    case 120: // 'x'
                    case 121: // 'y'
                    case 122: // 'z'
                        stringbuffer.append(c);
                        advance();
                        break;

                    case 40: // '('
                    case 41: // ')'
                    case 42: // '*'
                    case 43: // '+'
                    case 44: // ','
                    case 45: // '-'
                    case 46: // '.'
                    case 47: // '/'
                    case 48: // '0'
                    case 49: // '1'
                    case 50: // '2'
                    case 51: // '3'
                    case 52: // '4'
                    case 53: // '5'
                    case 54: // '6'
                    case 55: // '7'
                    case 56: // '8'
                    case 57: // '9'
                    case 58: // ':'
                    case 59: // ';'
                    case 60: // '<'
                    case 61: // '='
                    case 62: // '>'
                    case 63: // '?'
                    case 91: // '['
                    case 92: // '\\'
                    case 93: // ']'
                    case 94: // '^'
                    case 95: // '_'
                    case 96: // '`'
                    default:
                        flag1 = true;
                        break;
                    }
                } while(true);
                return new EqToken(1, stringbuffer.toString());

            case 46: // '.'
            case 48: // '0'
            case 49: // '1'
            case 50: // '2'
            case 51: // '3'
            case 52: // '4'
            case 53: // '5'
            case 54: // '6'
            case 55: // '7'
            case 56: // '8'
            case 57: // '9'
                stringbuffer.append(c);
                advance();
                boolean flag2 = false;
                do
                {
                    if(EOF || flag2)
                        break;
                    c = getChar();
                    switch(c)
                    {
                    case 46: // '.'
                    case 48: // '0'
                    case 49: // '1'
                    case 50: // '2'
                    case 51: // '3'
                    case 52: // '4'
                    case 53: // '5'
                    case 54: // '6'
                    case 55: // '7'
                    case 56: // '8'
                    case 57: // '9'
                        stringbuffer.append(c);
                        advance();
                        break;

                    case 47: // '/'
                    default:
                        flag2 = true;
                        break;
                    }
                } while(true);
                return new EqToken(2, stringbuffer.toString());

            case 92: // '\\'
                advance();
                boolean flag3 = false;
                if(!EOF)
                {
                    c = getChar();
                    switch(c)
                    {
                    case 92: // '\\'
                        advance();
                        return new EqToken(8);

                    case 123: // '{'
                        advance();
                        return new EqToken(109, String.valueOf(c));

                    case 124: // '|'
                        advance();
                        return new EqToken(109, "||");

                    case 125: // '}'
                        advance();
                        return new EqToken(109, String.valueOf(c));

                    case 44: // ','
                        advance();
                        return new EqToken(113, "3");

                    case 58: // ':'
                        advance();
                        return new EqToken(113, "4");

                    case 59: // ';'
                        advance();
                        return new EqToken(113, "5");

                    case 33: // '!'
                        advance();
                        return new EqToken(113, "-3");

                    case 65: // 'A'
                    case 66: // 'B'
                    case 67: // 'C'
                    case 68: // 'D'
                    case 69: // 'E'
                    case 70: // 'F'
                    case 71: // 'G'
                    case 72: // 'H'
                    case 73: // 'I'
                    case 74: // 'J'
                    case 75: // 'K'
                    case 76: // 'L'
                    case 77: // 'M'
                    case 78: // 'N'
                    case 79: // 'O'
                    case 80: // 'P'
                    case 81: // 'Q'
                    case 82: // 'R'
                    case 83: // 'S'
                    case 84: // 'T'
                    case 85: // 'U'
                    case 86: // 'V'
                    case 87: // 'W'
                    case 88: // 'X'
                    case 89: // 'Y'
                    case 90: // 'Z'
                    case 97: // 'a'
                    case 98: // 'b'
                    case 99: // 'c'
                    case 100: // 'd'
                    case 101: // 'e'
                    case 102: // 'f'
                    case 103: // 'g'
                    case 104: // 'h'
                    case 105: // 'i'
                    case 106: // 'j'
                    case 107: // 'k'
                    case 108: // 'l'
                    case 109: // 'm'
                    case 110: // 'n'
                    case 111: // 'o'
                    case 112: // 'p'
                    case 113: // 'q'
                    case 114: // 'r'
                    case 115: // 's'
                    case 116: // 't'
                    case 117: // 'u'
                    case 118: // 'v'
                    case 119: // 'w'
                    case 120: // 'x'
                    case 121: // 'y'
                    case 122: // 'z'
                        stringbuffer.append(c);
                        advance();
                        boolean flag4 = false;
                        do
                        {
                            if(EOF || flag4)
                                break;
                            c = getChar();
                            switch(c)
                            {
                            case 65: // 'A'
                            case 66: // 'B'
                            case 67: // 'C'
                            case 68: // 'D'
                            case 69: // 'E'
                            case 70: // 'F'
                            case 71: // 'G'
                            case 72: // 'H'
                            case 73: // 'I'
                            case 74: // 'J'
                            case 75: // 'K'
                            case 76: // 'L'
                            case 77: // 'M'
                            case 78: // 'N'
                            case 79: // 'O'
                            case 80: // 'P'
                            case 81: // 'Q'
                            case 82: // 'R'
                            case 83: // 'S'
                            case 84: // 'T'
                            case 85: // 'U'
                            case 86: // 'V'
                            case 87: // 'W'
                            case 88: // 'X'
                            case 89: // 'Y'
                            case 90: // 'Z'
                            case 97: // 'a'
                            case 98: // 'b'
                            case 99: // 'c'
                            case 100: // 'd'
                            case 101: // 'e'
                            case 102: // 'f'
                            case 103: // 'g'
                            case 104: // 'h'
                            case 105: // 'i'
                            case 106: // 'j'
                            case 107: // 'k'
                            case 108: // 'l'
                            case 109: // 'm'
                            case 110: // 'n'
                            case 111: // 'o'
                            case 112: // 'p'
                            case 113: // 'q'
                            case 114: // 'r'
                            case 115: // 's'
                            case 116: // 't'
                            case 117: // 'u'
                            case 118: // 'v'
                            case 119: // 'w'
                            case 120: // 'x'
                            case 121: // 'y'
                            case 122: // 'z'
                                stringbuffer.append(c);
                                advance();
                                break;

                            case 91: // '['
                            case 92: // '\\'
                            case 93: // ']'
                            case 94: // '^'
                            case 95: // '_'
                            case 96: // '`'
                            default:
                                flag4 = true;
                                break;
                            }
                        } while(true);
                        String s1 = stringbuffer.toString();                    
                        if(s1.equals("euro"))
			    return new EqToken(2, "\u20AC");
                        if(s1.equals("acute"))
                            return new EqToken(22, "\uFFFD");
                        if(s1.equals("array"))
                            return new EqToken(15);
                        if(s1.equals("bar"))
                            return new EqToken(14, "bar");
                        if(s1.equals("ddot"))
                            return new EqToken(22, "..");
                        if(s1.equals("dot"))
                            return new EqToken(22, ".");
                        if(s1.equals("frac"))
                            return new EqToken(12);
                        if(s1.equals("grave"))
                            return new EqToken(22, "`");
                        if(s1.equals("hat"))
                            return new EqToken(22, "^");
                        if(s1.equals("int"))
                            return new EqToken(20, "int");
                        if(s1.equals("oint"))
                            return new EqToken(20, "oint");
                        if(s1.equals("left"))
                            return new EqToken(16);
                        if(s1.equals("limsup"))
                            return new EqToken(24, "lim sup");
                        if(s1.equals("liminf"))
                            return new EqToken(24, "lim inf");
                        if(s1.equals("prod"))
                            return new EqToken(20, "prod");
                        if(s1.equals("right"))
                            return new EqToken(17);
                        if(s1.equals("sqrt"))
                            return new EqToken(13);
                        if(s1.equals("sum"))
                            return new EqToken(20, "sum");
                        if(s1.equals("tilde"))
                            return new EqToken(22, "~");
                        if(s1.equals("vec"))
                            return new EqToken(14);
                        if(s1.equals("widehat"))
                            return new EqToken(14, "widehat");
                        if(s1.equals("widetilde"))
                            return new EqToken(14, "widetilde");
                        if(s1.equals("quad"))
                            return new EqToken(113, "18");
                        if(s1.equals("qquad"))
                            return new EqToken(113, "36");
                        if(s1.equals("backslash"))
                            return new EqToken(2, "\\");
                        if(s1.equals("langle"))
                            return new EqToken(5, "<");
                        if(s1.equals("rangle"))
                            return new EqToken(5, ">");
                        if(s1.equals("not"))
                            return new EqToken(110);
                        if(s1.equals("atop"))
                            return new EqToken(115);
                        if(s1.equals("choose"))
                            return new EqToken(114);
                        if(s1.equals("overline"))
                            return new EqToken(116);
                        if(s1.equals("underline"))
                            return new EqToken(117);
                        if(s1.equals("overbrace"))
                            return new EqToken(118);
                        if(s1.equals("underbrace"))
                            return new EqToken(119);
                        if(s1.equals("stackrel"))
                            return new EqToken(120);
                        if(s1.equals("begin"))
                            return new EqToken(50);
                        if(s1.equals("end"))
                            return new EqToken(51);
                        if(s1.equals("fgcolor"))
                            return new EqToken(121);
                        if(s1.equals("bgcolor"))
                            return new EqToken(122);
                        if(s1.equals("fbox"))
                            return new EqToken(123);
                        if(s1.equals("mbox"))
                            return new EqToken(124);
                        if(" arccos arcsin arctan arg cos cosh cot coth csc csch def deg dim exp hom ker lg ln log sec sech sin sinh tan tanh ".indexOf((new StringBuilder()).append(" ").append(s1).append(" ").toString()) >= 0)
                            return new EqToken(9, s1);
                        if(" det gcd inf lim max min Pr sup ".indexOf((new StringBuilder()).append(" ").append(s1).append(" ").toString()) >= 0)
                            return new EqToken(24, s1);
                        if(" alpha delta epsilon iota kappa lambda nu omega pi sigma theta tau upsilon varepsilon varpi vartheta pm mp times div cdot cdots ldots ast star amalg cap cup uplus sqcap sqcup vee wedge wr circ bullet diamond lhd rhd oslash odot Box bigtriangleup triangleleft triangleright oplus ominus otimes ll subset sqsubset in vdash models gg supset sqsupset ni dashv perp neq doteq approx cong equiv propto prec sim simeq asymp smile frown bowtie succ aleph forall hbar exists imath neg flat ell Re angle Im backslash mho Box prime emptyset triangle nabla partial top bot Join infty vdash dashv Fourier Laplace leftarrow gets hookrightarrow leftharpoondown rightarrow to rightharpoondown leadsto leftrightarrow mapsto hookleftarrow leftharpoonup rightharpoonup rightleftharpoons longleftarrow longrightarrow longleftrightarrow longmapsto ".indexOf((new StringBuilder()).append(" ").append(s1).append(" ").toString()) >= 0)
                            return new EqToken(18, s1);
                        if(" beta chi eta gamma mu psi phi rho varrho varsigma varphi xi zeta le leq ge geq vdots ddots natural jmath bigtriangledown sharp uparrow downarrow updownarrow nearrow searrow swarrow nwarrow succeq mid preceq parallel subseteq sqsubseteq supseteq sqsupseteq clubsuit diamondsuit heartsuit spadesuit wp dagger ddagger setminus unlhd unrhd bigcirc ".indexOf((new StringBuilder()).append(" ").append(s1).append(" ").toString()) >= 0)
                            return new EqToken(19, s1);
                        if(" Delta Gamma Lambda Omega Pi Phi Psi Sigma Theta Upsilon Xi Leftarrow Rightarrow Leftrightarrow Longleftarrow Longrightarrow Longleftrightarrow Diamond ".indexOf((new StringBuilder()).append(" ").append(s1).append(" ").toString()) >= 0)
                            return new EqToken(18, (new StringBuilder()).append(s1).append("Big").toString());
                        if(" Uparrow Downarrow Updownarrow ".indexOf((new StringBuilder()).append(" ").append(s1).append(" ").toString()) >= 0)
                            return new EqToken(19, (new StringBuilder()).append(s1).append("Big").toString());
                        // fall through

                    case 34: // '"'
                    case 35: // '#'
                    case 36: // '$'
                    case 37: // '%'
                    case 38: // '&'
                    case 39: // '\''
                    case 40: // '('
                    case 41: // ')'
                    case 42: // '*'
                    case 43: // '+'
                    case 45: // '-'
                    case 46: // '.'
                    case 47: // '/'
                    case 48: // '0'
                    case 49: // '1'
                    case 50: // '2'
                    case 51: // '3'
                    case 52: // '4'
                    case 53: // '5'
                    case 54: // '6'
                    case 55: // '7'
                    case 56: // '8'
                    case 57: // '9'
                    case 60: // '<'
                    case 61: // '='
                    case 62: // '>'
                    case 63: // '?'
                    case 64: // '@'
                    case 91: // '['
                    case 93: // ']'
                    case 94: // '^'
                    case 95: // '_'
                    case 96: // '`'
                    default:
                        boolean flag5 = true;
                        advance();
                        System.out.println((new StringBuilder()).append("Scanner invalid tag: \\").append(stringbuffer.toString()).toString());
                        return new EqToken(100);
                    }
                }
                break;

            case 94: // '^'
                advance();
                return new EqToken(10);

            case 95: // '_'
                advance();
                return new EqToken(11);

            case 11: // '\013'
            case 12: // '\f'
            case 14: // '\016'
            case 15: // '\017'
            case 16: // '\020'
            case 17: // '\021'
            case 18: // '\022'
            case 19: // '\023'
            case 20: // '\024'
            case 21: // '\025'
            case 22: // '\026'
            case 23: // '\027'
            case 24: // '\030'
            case 25: // '\031'
            case 26: // '\032'
            case 27: // '\033'
            case 28: // '\034'
            case 29: // '\035'
            case 30: // '\036'
            case 31: // '\037'
            case 34: // '"'
            case 36: // '$'
            case 37: // '%'
            case 63: // '?'
            case 96: // '`'
            default:
                advance();
                System.out.println((new StringBuilder()).append("Scanner invalid character: ").append(c).toString());
                return new EqToken(100);
            }
        } while(true);
        return new EqToken(99);
    }

    private String equation;
    private int count;
    private int countT;
    private EqToken token;
    private boolean EOF;
    private Vector TokenV;
    private boolean selectB;
    private boolean collectB;
    private int selectCount1;
    private int selectCount2;
    private StringBuffer selectSB;
}
