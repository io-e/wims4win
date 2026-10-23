package atp;

import java.util.Vector;

// Referenced classes of package atp:
//            e

class b
{

    private String _fldelse;
    private int _fldcase;
    private int _flddo;
    private e _fldtry;
    private boolean _fldbyte;
    private Vector _fldfor;
    private boolean _fldchar;
    private boolean a;
    private int _fldnew;
    private int _fldint;
    private StringBuffer _fldif;
    
	public static char TranslationC[]=
	{	'\u03B1','\u0391',
		'\u03B2','\u0392',
		'\u03B3','\u0393',
		'\u03B4','\u0394',
		'\u03B5','\u0395',
		'\u03D5','\u03A6','\u03C6',
		'\u03B3','\u0393',
		'\u03B7','\u0397',
		'\u03B9','\u0399',
		'\u03BA','\u039A',
		'\u03BB','\u039B',
		'\u03BC','\u039C',
		'\u03BD','\u039D',
		'\u03BF','\u03A9',
		'\u03C0','\u03A0',
		'\u03C7','\u03A7',
		'\u03C1','\u03A1',
		'\u03C3','\u03A3',
		'\u03C4','\u03A4',
		'\u03C5','\u03A5',
		'\u03C8','\u03A8',
		'\u03C9','\u03A9',
		'\u03BE','\u039E',
		'\u03C7','\u03A7',
		'\u03B6','\u0396',
		'\u03B8','\u0398',
		'$','%','&','§',
		'\u00A9','\u00AE',
		'\u2030','\u00BD','\u2153','\u00BC','\u00BE','\u2154',
		'\u00B1','\u2202','\u2206','\u221E',
	};

	public String TranslationS[]=
	{	"alpha","Alpha",
		"beta","Beta",
		"gamma","Gamma",
		"delta","Delta",
		"epsilon","Epsilon",
		"phi","Phi","varphi",
		"gamma","Gamma",
		"eta","Eta",
		"iota","I",
		"kappa","Kappa",
		"lambda","Lambda",
		"mu","Mu",
		"nu","Nu",
		"omikron","Omikron",
		"pi","Pi",
		"chi","Chi",
		"rho","Rho",
		"sigma","Sigma",
		"tau","Tau",
		"uu","Uu",
		"psi","Psi",
		"omega","Omega",
		"chi","Chi",
		"ypsilon","Ypsilon",
		"zeta","Zeta",
		"theta","Theta",
		"dollar","percent","ampersand","paragraph",
		"copyright","trademark",
		"promille","half","third","quarter","threequarters","twothirds",
		"pm","partial","deltaop","infty",
	};
    public b(String s)
    {
        _fldbyte = false;
        _fldfor = new Vector(50, 50);
        _fldchar = false;
        a = false;
        _fldnew = 0;
        _fldint = 0;
        _fldif = new StringBuffer("");
        _fldtry = new e(99);
        a(s);
    }

    public String a(int i, int j)
    {
        _fldnew = Math.min(i, j);
        _fldint = Math.max(i, j);
        _fldchar = true;
        _fldif = new StringBuffer("");
        a(_fldelse);
        _fldchar = false;
        return _fldif.toString();
    }

    public void a(String s)
    {
        _fldelse = s;
        int i = 0;
        boolean flag = false;
        boolean flag1 = false;
        _fldbyte = false;
        _flddo = -1;
        _fldcase = -1;
        _fldfor.removeAllElements();
        a();
        while(!_fldbyte) 
        {
            _flddo++;
            if(_fldchar && _flddo == _fldnew)
            {
                a = true;
            }
            _fldfor.addElement(_mthtry());
            if(_fldchar && _flddo == _fldint)
            {
                a = false;
            }
        }
        _flddo = -1;
        for(; i < _fldfor.size(); i++)
        {
            if(((e)_fldfor.elementAt(i)).y == 114)
            {
                int k = i - 1;
                int j1 = 0;
                for(; k > 0; k--)
                {
                    if(((e)_fldfor.elementAt(k)).y == 4)
                    {
                        j1--;
                    } else
                    if(((e)_fldfor.elementAt(k)).y == 3)
                    {
                        j1++;
                    }
                    if(j1 == 1)
                    {
                        break;
                    }
                }

                int i2 = i + 1;
                j1 = 0;
                for(; i2 < _fldfor.size(); i2++)
                {
                    if(((e)_fldfor.elementAt(i2)).y == 4)
                    {
                        j1++;
                    } else
                    if(((e)_fldfor.elementAt(i2)).y == 3)
                    {
                        j1--;
                    }
                    if(j1 == 1)
                    {
                        break;
                    }
                }

                if(j1 == 1 && k >= 0)
                {
                    _fldfor.insertElementAt(new e(109, ")"), i2 + 1);
                    _fldfor.insertElementAt(new e(17), i2 + 1);
                    _fldfor.setElementAt(new e(4), i);
                    _fldfor.insertElementAt(new e(3), i + 1);
                    _fldfor.insertElementAt(new e(115), k);
                    _fldfor.insertElementAt(new e(109, "("), k);
                    _fldfor.insertElementAt(new e(16), k);
                    i += 4;
                }
            } else
            if(((e)_fldfor.elementAt(i)).y == 115)
            {
                int l = i - 1;
                int k1 = 0;
                for(; l > 0; l--)
                {
                    if(((e)_fldfor.elementAt(l)).y == 4)
                    {
                        k1--;
                    } else
                    if(((e)_fldfor.elementAt(l)).y == 3)
                    {
                        k1++;
                    }
                    if(k1 == 1)
                    {
                        break;
                    }
                }

                if(l >= 0)
                {
                    _fldfor.setElementAt(new e(4), i);
                    _fldfor.insertElementAt(new e(3), i + 1);
                    _fldfor.insertElementAt(new e(115), l);
                    i += 2;
                }
            }
        }

        for(int j = 0; j < _fldfor.size() - 2; j++)
        {
            if(((e)_fldfor.elementAt(j)).y == 13 && ((e)_fldfor.elementAt(j + 1)).y == 109)
            {
                int i1 = j + 2;
                int l1 = 0;
                int j2 = 1;
                for(; i1 < _fldfor.size(); i1++)
                {
                    if(((e)_fldfor.elementAt(i1)).y == 4)
                    {
                        l1--;
                    } else
                    if(((e)_fldfor.elementAt(i1)).y == 3)
                    {
                        l1++;
                    }
                    if(l1 != 0)
                    {
                        continue;
                    }
                    if(((e)_fldfor.elementAt(i1)).w.equals("["))
                    {
                        j2++;
                    } else
                    if(((e)_fldfor.elementAt(i1)).w.equals("]"))
                    {
                        j2--;
                    }
                    if(j2 != 0)
                    {
                        continue;
                    }
                    _fldfor.setElementAt(new e(4), i1);
                    break;
                }

                j++;
            }
        }

    }

    public void _mthdo()
    {
        _flddo = -1;
    }

    public int _mthif()
    {
        return _flddo;
    }

    public void a(int i)
    {
        _flddo = i;
    }

    public e _mthint()
    {
        _flddo++;
        if(_flddo >= _fldfor.size())
        {
            _flddo = _fldfor.size() - 1;
            return new e(99);
        } else
        {
            return (e)_fldfor.elementAt(_flddo);
        }
    }

    public boolean _mthnew()
    {
        return _flddo == _fldfor.size() - 1;
    }

    private char _mthfor()
    {
        return _fldelse.charAt(_fldcase);
    }

    private void a()
    {
        if(a)
        {
            _fldif.append(_fldelse.charAt(_fldcase));
        }
        if(_fldcase < _fldelse.length() - 1)
        {
            _fldcase++;
            _fldbyte = false;
        } else
        {
            _fldcase = _fldelse.length();
            _fldbyte = true;
        }
    }

    private e _mthtry()
    {
        StringBuffer stringbuffer = new StringBuffer("");
        String s = new String("");
        e e1 = new e();
        boolean flag = false;
        while(!_fldbyte) 
        {
            char c = _mthfor();
            switch(c)
            {
            case 9: // '\t'
            case 10: // '\n'
            case 13: // '\r'
                a();
                break;

            case 32: // ' '
                a();
                return new e(25, new String(" "));

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
                a();
                return new e(108, String.valueOf(c));

            case 123: // '{'
                a();
                return new e(3);

            case 125: // '}'
                a();
                return new e(4);

            case 40: // '('
            case 41: // ')'
            case 91: // '['
            case 93: // ']'
            case 124: // '|'
                a();
                return new e(109, String.valueOf(c));

            case 38: // '&'
                a();
                return new e(7);

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
            case 37: // '%'
                        stringbuffer.append(c);
                a();
                for(boolean flag1 = false; !_fldbyte && !flag1;)
                {
                    c = _mthfor();
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
                        a();
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
                }

                return new e(1, stringbuffer.toString());

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
                a();
                for(boolean flag2 = false; !_fldbyte && !flag2;)
                {
                    c = _mthfor();
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
                        a();
                        break;

                    case 47: // '/'
                    default:
                        flag2 = true;
                        break;
                    }
                }

                return new e(2, stringbuffer.toString());

            case 92: // '\\'
                a();
                boolean flag3 = false;
                if(_fldbyte)
                {
                    break;
                }
                c = _mthfor();
                switch(c)
                {
                case 92: // '\\'
                    a();
                    return new e(8);

                case 123: // '{'
                    a();
                    return new e(109, String.valueOf(c));

                case 124: // '|'
                    a();
                    return new e(109, "||");

                case 125: // '}'
                    a();
                    return new e(109, String.valueOf(c));

                case 44: // ','
                    a();
                    return new e(113, "3");

                case 58: // ':'
                    a();
                    return new e(113, "4");

                case 59: // ';'
                    a();
                    return new e(113, "5");

                case 33: // '!'
                    a();
                    return new e(113, "-3");

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
                    a();
                    for(boolean flag4 = false; !_fldbyte && !flag4;)
                    {
                        c = _mthfor();
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
                            a();
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
                    }

                    String s1 = stringbuffer.toString();
                    if(s1.equals("acute"))
                    {
                        return new e(22, "\264");
                    }
                    if(s1.equals("array"))
                    {
                        return new e(15);
                    }
                    if(s1.equals("bar"))
                    {
                        return new e(14, "bar");
                    }
                    if(s1.equals("ddot"))
                    {
                        return new e(22, "..");
                    }
                    if(s1.equals("dot"))
                    {
                        return new e(22, ".");
                    }
                    if(s1.equals("frac"))
                    {
                        return new e(12);
                    }
                    if(s1.equals("dfrac"))
                    {
                        return new e(1001);
                    }
                    if(s1.equals("grave"))
                    {
                        return new e(22, "`");
                    }
                    if(s1.equals("hat"))
                    {
                        return new e(22, "^");
                    }
                    if(s1.equals("int"))
                    {
                        return new e(20, "int");
                    }
                    if(s1.equals("oint"))
                    {
                        return new e(20, "oint");
                    }
                    if(s1.equals("left"))
                    {
                        return new e(16);
                    }
                    if(s1.equals("limsup"))
                    {
                        return new e(24, "lim sup");
                    }
                    if(s1.equals("liminf"))
                    {
                        return new e(24, "lim inf");
                    }
                    if(s1.equals("prod"))
                    {
                        return new e(20, "prod");
                    }
                    if(s1.equals("right"))
                    {
                        return new e(17);
                    }
                    if(s1.equals("sqrt"))
                    {
                        return new e(13);
                    }
                    if(s1.equals("sum"))
                    {
                        return new e(20, "sum");
                    }
                    if(s1.equals("tilde"))
                    {
                        return new e(22, "~");
                    }
                    if(s1.equals("vec"))
                    {
                        return new e(14);
                    }
                    if(s1.equals("widehat"))
                    {
                        return new e(14, "widehat");
                    }
                    if(s1.equals("widetilde"))
                    {
                        return new e(14, "widetilde");
                    }
                    if(s1.equals("quad"))
                    {
                        return new e(113, "18");
                    }
                    if(s1.equals("qquad"))
                    {
                        return new e(113, "36");
                    }
                    if(s1.equals("backslash"))
                    {
                        return new e(2, "\\");
                    }
                    if(s1.equals("langle"))
                    {
                        return new e(5, "<");
                    }
                    if(s1.equals("rangle"))
                    {
                        return new e(5, ">");
                    }
                    if(s1.equals("not"))
                    {
                        return new e(110);
                    }
                    if(s1.equals("atop"))
                    {
                        return new e(115);
                    }
                    if(s1.equals("choose"))
                    {
                        return new e(114);
                    }
                    if(s1.equals("overline"))
                    {
                        return new e(116);
                    }
                    if(s1.equals("underline"))
                    {
                        return new e(117);
                    }
                    if(s1.equals("overbrace"))
                    {
                        return new e(118);
                    }
                    if(s1.equals("underbrace"))
                    {
                        return new e(119);
                    }
                    if(s1.equals("stackrel"))
                    {
                        return new e(120);
                    }
                    if(s1.equals("begin"))
                    {
                        return new e(50);
                    }
                    if(s1.equals("end"))
                    {
                        return new e(51);
                    }
                    if(s1.equals("fgcolor"))
                    {
                        return new e(121);
                    }
                    if(s1.equals("bgcolor"))
                    {
                        return new e(122);
                    }
                    if(s1.equals("fbox"))
                    {
                        return new e(123);
                    }
                    if(s1.equals("mbox"))
                    {
                        return new e(124);
                    }
                    if((" arccos arcsin arctan arg cos cosh cot coth csc def deg dim exp hom ker lg ln lo" +
                    		"g sec sin sinh tan tanh ")
                    		.indexOf(" " + s1 + " ") >= 0)
                    {
                        return new e(9, s1);
                    }
                    if(" det gcd inf lim max min Pr sup ".indexOf(" " + s1 + " ") >= 0)
                    {
                        return new e(24, s1);
                    }
                    {	boolean found=false;
                    	for (int i=0; i<TranslationS.length; i++)
                    	{	if (s1.equals(TranslationS[i]))
                    		{	found=true; c=TranslationC[i]; break;
                    		}
                    	}
                    	if (found)
                    	{	stringbuffer.append(c);
                			return new e(2,""+c);
                    	}
                    }
                    if((" alpha delta epsilon iota kappa lambda nu omega pi sigma theta tau upsilon varep" +
						"silon varpi vartheta pm mp times div cdot cdots ldots ast star amalg cap cup upl" +
						"us sqcap sqcup vee wedge wr circ bullet diamond lhd rhd oslash odot Box bigtrian" +
						"gleup triangleleft triangleright oplus ominus otimes ll subset sqsubset in vdash" +
						" models gg supset sqsupset ni dashv perp neq doteq approx cong equiv propto prec" +
						" sim simeq asymp smile frown bowtie succ aleph forall hbar exists imath neg flat" +
						" ell Re angle Im backslash mho Box prime emptyset triangle nabla partial top bot" +
						" Join infty vdash dashv Fourier Laplace leftarrow gets hookrightarrow leftharpoo" +
						"ndown rightarrow to rightharpoondown leadsto leftrightarrow mapsto hookleftarrow" +
						" leftharpoonup rightharpoonup rightleftharpoons longleftarrow longrightarrow lon" +
						"gleftrightarrow longmapsto ")
							.indexOf(" " + s1 + " ") >= 0)
                    {
                        return new e(18, s1);
                    }
                    if((" beta chi eta gamma mu psi phi rho varrho varsigma varphi xi zeta le leq ge geq " +
							"vdots ddots natural jmath bigtriangledown sharp uparrow downarrow updownarrow ne" +
							"arrow searrow swarrow nwarrow succeq mid preceq parallel subseteq sqsubseteq sup" +
							"seteq sqsupseteq clubsuit diamondsuit heartsuit spadesuit wp dagger ddagger setm" +
							"inus unlhd unrhd bigcirc ")
							.indexOf(" " + s1 + " ") >= 0)
                    {
                        return new e(19, s1);
                    }
                    if((" Delta Gamma Lambda Omega Pi Phi Psi Sigma Theta Upsilon Xi Leftarrow Rightarrow" +
						" Leftrightarrow Longleftarrow Longrightarrow Longleftrightarrow Diamond ")
						.indexOf(" " + s1 + " ") >= 0)
                    {
                        return new e(18, s1 + "Big");
                    }
                    if(" Uparrow Downarrow Updownarrow ".indexOf(" " + s1 + " ") >= 0)
                    {
                        return new e(19, s1 + "Big");
                    }
                    // fall through

                case 34: // '"'
                case 35: // '#'
                case 36: // '$'
                // case 37: // '%'
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
                    a();
                    //System.out.println("Scanner invalid tag: \\" + stringbuffer.toString());
                    return new e(100);
                }

            case 94: // '^'
                a();
                return new e(10);

            case 95: // '_'
                a();
                return new e(11);

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
            // case 37: // '%'
            case 63: // '?'
            case 96: // '`'
            default:
                a();
                //System.out.println("Scanner invalid character: " + c);
            	return new e(100);
            }
        }
        return new e(99);
    }
}

