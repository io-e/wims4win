package atp;

import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;

// Referenced classes of package atp:
//            f, b, a, c, 
//            e

public class sHotEqn
{

	Component PC; // container panel
	
    private int width;
    private int height;
    private String h;
    private String _fldcase;
    private String _fldint;
    ActionListener G;
    private b S;
    private e D;
    private Font d;
    private Font b;
    private Font _fldnull;
    private Font _fldlong;
    private static final float k = 2F;
    private static final int _fldvoid[] = {
        8, 10, 12, 14, 18
    };
    private static final int O[] = {
        2, 3, 4, 5, 6
    };
    private int j[] = {
        14, 12, 10, 8
    };
    private int _fldfor[] = {
        3, 3, 3, 3
    };
    private static final int _fldgoto[] = {
        9, 11, 14, 16, 22
    };
    private Image s;
    private boolean Q;
    private int f;
    private int H;
    private Color _fldbyte;
    private Color N;
    private Color _fldtry;
    private Color C;
    private boolean K;
    private boolean u;
    private int g;
    private String z;
    private String E;
    private int M;
    private int i;
    private boolean e;
    private f _flddo;
    private MediaTracker B;
    private Hashtable w;
    public boolean r;
    public boolean L;
    public boolean q;
    private boolean A;
    private boolean t;
    private int _fldelse;
    private int _fldchar;
    private int J;
    private int I;
    private int v;
    private int P;
    private int a;
    private int m;
    private int R;
    private int l;
    private int F;
    private boolean c;
    private int p;
    private int o;
    private Image _fldnew;

     public sHotEqn (Component PC)
    {
        width = 0;
        height = 0;
        h = null;
        _fldcase = null;
        _fldint = "Helvetica";
        Q = false;
        f = 0;
        H = 0;
        _fldbyte = Color.white;
        N = Color.white;
        _fldtry = Color.black;
        C = Color.red;
        K = false;
        u = false;
        g = 0;
        z = "left";
        E = "top";
        M = 0;
        i = 0;
        e = false;
        w = new Hashtable(13);
        r = false;
        L = false;
        q = true;
        A = false;
        t = true;
        _fldelse = 0;
        _fldchar = 0;
        J = 0;
        I = 0;
        v = 0;
        P = 0;
        a = 0;
        m = 0;
        R = 0;
        l = 0;
        F = 5;
        c = false;
        p = 0;
        o = 0;
        _flddo = new f();
        B = new MediaTracker(PC);
        S = new b("");
    }
     
     public void setEquation(String s1)
    {
        _fldcase = s1;
        S.a(s1);
        e = false;
        Q = false;
    }

    public String getEquation()
    {
        return _fldcase;
    }
    
    public String Status;

    public void printStatus(String s1)
    {
    	Status =s1;	
    }

    private void a(String s1)
    {
    	Status = s1;
    	
    }

    public Image getImage()
    {
        if(Q)
        {
            return s;
        } else
        {
            return null;
        }
    }

    public void setDebug(boolean flag)
    {
        q = flag;
    }

    public boolean isDebug()
    {
        return q;
    }

    public void setFontname(String s1)
    {
        _fldint = s1;
    }

    public String getFontname()
    {
        return _fldint;
    }

    int i10,j10,k10,l10;
    boolean bold0,fonts=false;
    
    public void setHAlign(String s1)
    {
        z = s1;
        e = false;
        Q = false;
    }

    public String getHAlign()
    {
        return z;
    }

    public void setVAlign(String s1)
    {
        E = s1;
        e = false;
        Q = false;
    }

    public String getVAlign()
    {
        return E;
    }

    public void setEditable(boolean flag)
    {
        t = flag;
    }

    public boolean isEditable()
    {
        return t;
    }

    public String getSelectedArea()
    {
        return S.a(p, o);
    }

    public Dimension getPreferredSize()
    {
        if((width == 0) & (height == 0))
        {
            Graphics g1 = PC.getGraphics();
            if(g1 != null)
            {
                g1.setFont(d);
                S._mthdo();
                c c1 = _mthnew(0, 150, false, g1, 1);
                if(K)
                {
                    g = 5;
                } else
                {
                    g = 0;
                }
                f = 1 + c1._flddo + 2 * g;
                H = 1 + c1._fldif + c1.a + 2 * g;
            }
        }
        width = f;
        height = H;
        if(f <= 1)
        {
            return new Dimension(100, 100);
        } else
        {
            return new Dimension(f, H);
        }
    }

    final int ff1=8,ff2=6;
    
    public Dimension getSizeof (String s1, Graphics g1)
    {
    	if (d!=g1.getFont())
    	{	d=g1.getFont();
			int fs=d.getSize();
    		b=new Font(d.getFamily(),d.getStyle(),fs*ff1/10);;
    		_fldnull=_fldlong=new Font(d.getFamily(),d.getStyle(),fs*ff2/10);
    	}
    	
        S.a(s1);
        c c1 = _mthnew(0, 150, false, g1, 1);
        byte byte0;
        if(K)
        {
            byte0 = 5;
        } else
        {
            byte0 = 0;
        }
        return new Dimension(1 + c1._flddo + 2 * byte0, 1 + c1._fldif + c1.a + 2 * byte0);
    }

    public int getAscent (String s1, Graphics g1)
    {
    	if (d!=g1.getFont())
    	{	d=g1.getFont();
			int fs=d.getSize();
    		b=new Font(d.getFamily(),d.getStyle(),fs*ff1/10);;
    		_fldnull=_fldlong=new Font(d.getFamily(),d.getStyle(),fs*ff2/10);
    	}
    	
        S.a(s1);
        c c1 = _mthnew(0, 150, false, g1, 1);
        byte byte0;
        if(K)
        {
            byte0 = 5;
        } else
        {
            byte0 = 0;
        }
        return c1._fldif+byte0;
    }

    public Dimension getMinimumSize()
    {
        return getPreferredSize();
    }

    private void a (Graphics g1)
    {
        int i1 = height / 2 - 10;
        int j1 = i1 + 20;
        int k1 = width / 2 - 5;
        int l1 = k1 + 10;
        Polygon polygon = new Polygon();
        polygon.addPoint(k1, i1);
        polygon.addPoint(l1, j1);
        polygon.addPoint(k1, j1);
        polygon.addPoint(l1, i1);
        g1.fillPolygon(polygon);
    }

    public synchronized int paint (int cc, int rr, Graphics g2)
    {
    	if (d!=g2.getFont())
    	{	d=g2.getFont();
			int fs=d.getSize();
    		b=new Font(d.getFamily(),d.getStyle(),fs*ff1/10);;
    		_fldnull=_fldlong=new Font(d.getFamily(),d.getStyle(),fs*ff2/10);
    	}
    	//rr-=g2.getFontMetrics().getAscent();
    	
        c c1 = new c();
        g2.setFont(d);
        g = 0;
        S._mthdo();
        c1 = _mthnew(cc, rr, false, g2, 1);
        int hh=c1._fldif;
        c1 = new c();
        S._mthdo();
        c1 = _mthnew(cc, rr+hh, true, g2, 1);
        a(" ");
        M = 0;
        if(z.equals("center"))
        {
            M = 1;
        } else
        if(z.equals("right"))
        {
            M = 2;
        }
        i = 0;
        if(E.equals("middle"))
        {
            i = 1;
        } else
        if(E.equals("bottom"))
        {
            i = 2;
        }
        f = 1 + c1._flddo + 2 * g;
        H = 1 + c1._fldif + c1.a + 2 * g;
        boolean flag = false;
        if(f > width)
        {
            flag = true;
            M = 0;
        }
        if(H > height)
        {
            flag = true;
            i = 1;
        }
        int i1 = g;
        int j1 = g;
        switch(M)
        {
        case 1: // '\001'
            i1 = (width - c1._flddo) / 2;
            break;

        case 2: // '\002'
            i1 = width - g - c1._flddo - 1;
            break;
        }
        switch(i)
        {
        case 1: // '\001'
            j1 = g - (H - height) / 2;
            break;

        case 2: // '\002'
            j1 = height - g - c1.a - c1._fldif;
            break;
        }
        Q = true;
        e = true;
        v = i1;
        P = j1 + c1._fldif;
        notify();
        return c1._fldif + c1.a;
    }

    private c _mthnew(int i1, int j1, boolean flag, Graphics g1, int k1)
    {
        return _mthtry(i1, j1, flag, g1, k1, true);
    }

    private c _mthtry(int i1, int j1, boolean flag, Graphics g1, int k1, boolean flag1)
    {
        c c1 = new c();
        c c2 = new c();
        boolean flag2 = true;
        boolean flag3 = false;
        boolean flag5 = false;
        int l1 = 0;
        boolean flag6 = false;
        while(!S._mthnew() && flag2) 
        {
            D = S._mthint();
            if(A && flag)
            {
                l1 = S._mthif();
            }
            boolean flag4 = false;
            int i2 = l1;
            int j2 = D.y;
            switch(D.y)
            {
            case 4: // '\004'
            case 7: // '\007'
            case 8: // '\b'
            case 17: // '\021'
            case 51: // '3'
                if(c && flag)
                {
                    if(l1 > o)
                    {
                        o = l1;
                    }
                    if(l1 < p)
                    {
                        p = l1;
                    }
                }
                return c2;

            case 22: // '\026'
                c1 = _mthbyte(i1 + c2._flddo, j1, flag, g1, k1);
                break;

            case 5: // '\005'
                c1 = a(i1 + c2._flddo, j1, flag, g1);
                break;

            case 15: // '\017'
                if(c && flag)
                {
                    flag5 = true;
                }
                c1 = _mthif(i1 + c2._flddo, j1, flag, g1, k1);
                break;

            case 50: // '2'
                if(c && flag)
                {
                    flag5 = true;
                }
                c1 = _mthgoto(i1 + c2._flddo, j1, flag, g1, k1);
                break;

            case 3: // '\003'
                c1 = _mthtry(i1 + c2._flddo, j1, flag, g1, k1, true);
                break;

             case 123: // '{'
                if(c && flag)
                {
                    flag5 = true;
                }
                c1 = _mthelse(i1 + c2._flddo, j1, flag, g1, k1);
                break;

            case 1: // '\001'
                c1 = _mthnew(i1 + c2._flddo, j1, flag, g1);
                break;

            case 110: // 'n'
                c1 = _mthtry(i1 + c2._flddo, j1, flag, g1, k1);
                break;

            case 108: // 'l'
                c1 = _mthint(i1 + c2._flddo, j1, flag, g1);
                break;

            case 12: // '\f'
                c1 = _mthfor(i1 + c2._flddo, j1, flag, g1, k1, true);
                break;
            
            case 1001: // '\f'
                c1 = _mthfor(i1 + c2._flddo, j1, flag, g1, k1-1, true);
                break;
            
            case 115: // 's'
                c1 = _mthfor(i1 + c2._flddo, j1, flag, g1, k1, false);
                break;

            case 2: // '\002'
            case 9: // '\t'
                c1 = _mthif(i1 + c2._flddo, j1, flag, g1);
                break;

            case 18: // '\022'
                c1 = _mthif(i1 + c2._flddo, j1, flag, g1, k1, false);
                break;

            case 19: // '\023'
                c1 = _mthif(i1 + c2._flddo, j1, flag, g1, k1, true);
                break;

            case 16: // '\020'
                if(c && flag)
                {
                    flag5 = true;
                }
                c1 = _mthnull(i1 + c2._flddo, j1, flag, g1, k1);
                break;

            case 24: // '\030'
                c1 = _mthfor(i1 + c2._flddo, j1, flag, g1, k1);
                break;

            case 124: // '|'
                c1 = _mthdo(i1 + c2._flddo, j1, flag, g1);
                break;

            case 118: // 'v'
                c1 = a(i1 + c2._flddo, j1, flag, g1, k1);
                break;

            case 119: // 'w'
                c1 = _mthlong(i1 + c2._flddo, j1, flag, g1, k1);
                break;

            case 116: // 't'
                c1 = a(i1 + c2._flddo, j1, flag, g1, k1, true);
                break;

            case 117: // 'u'
                c1 = a(i1 + c2._flddo, j1, flag, g1, k1, false);
                break;

            case 109: // 'm'
                c1 = _mthtry(i1 + c2._flddo, j1, flag, g1);
                break;

            case 113: // 'q'
                c1 = _mthfor(i1 + c2._flddo, j1, flag, g1);
                break;

            case 13: // '\r'
                if(c && flag)
                {
                    flag5 = true;
                }
                c1 = _mthcase(i1 + c2._flddo, j1, flag, g1, k1);
                break;

            case 120: // 'x'
                c1 = _mthchar(i1 + c2._flddo, j1, flag, g1, k1);
                break;

            case 10: // '\n'
                c1 = _mthint(i1 + c2._flddo, j1, flag, g1, k1, true);
                break;

            case 11: // '\013'
                c1 = _mthdo(i1 + c2._flddo, j1, flag, g1, k1, true);
                break;

            case 20: // '\024'
                c1 = _mthdo(i1 + c2._flddo, j1, flag, g1, k1);
                break;

            case 14: // '\016'
                c1 = _mthint(i1 + c2._flddo, j1, flag, g1, k1);
                break;

            case 25: // '\031'
                c1 = new c(0, 0, 0);
                flag4 = true;
                break;

            case 99: // 'c'
            case 100: // 'd'
                c1 = new c(0, 0, 0);
                break;

            case 6: // '\006'
            case 21: // '\025'
            case 23: // '\027'
            case 26: // '\032'
            case 27: // '\033'
            case 28: // '\034'
            case 29: // '\035'
            case 30: // '\036'
            case 31: // '\037'
            case 32: // ' '
            case 33: // '!'
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
            case 44: // ','
            case 45: // '-'
            case 46: // '.'
            case 47: // '/'
            case 48: // '0'
            case 49: // '1'
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
            case 91: // '['
            case 92: // '\\'
            case 93: // ']'
            case 94: // '^'
            case 95: // '_'
            case 96: // '`'
            case 97: // 'a'
            case 98: // 'b'
            case 101: // 'e'
            case 102: // 'f'
            case 103: // 'g'
            case 104: // 'h'
            case 105: // 'i'
            case 106: // 'j'
            case 107: // 'k'
            case 111: // 'o'
            case 112: // 'p'
            case 114: // 'r'
            default:
                printStatus("Parser: unknown token: " + D.y + " " + D.w);
                break;
            }
            if(flag)
            {
                if(A)
                {
                    if(!c && i1 + c2._flddo <= _fldelse && _fldelse <= i1 + c2._flddo + c1._flddo && j1 - c1._fldif <= _fldchar && _fldchar <= j1 + c1.a)
                    {
                        m = l = _fldelse;
                        a = R = _fldchar;
                        c = true;
                        p = l1;
                        o = l1;
                    }
                    if(!c && i1 + c2._flddo <= J && J <= i1 + c2._flddo + c1._flddo && j1 - c1._fldif <= I && I <= j1 + c1.a)
                    {
                        m = l = J;
                        a = R = I;
                        c = true;
                        p = l1;
                        o = l1;
                        int k2 = J;
                        int l2 = I;
                        J = _fldelse;
                        I = _fldchar;
                        _fldelse = k2;
                        _fldchar = l2;
                    }
                    if(c)
                    {
                        m = Math.min(m, i1 + c2._flddo);
                        l = Math.max(l, i1 + c2._flddo + c1._flddo);
                        a = Math.min(a, j1 - c1._fldif);
                        R = Math.max(R, j1 + c1.a);
                        if(F > k1)
                        {
                            F = k1;
                        }
                        switch(j2)
                        {
                        case 13: // '\r'
                        case 15: // '\017'
                        case 16: // '\020'
                        case 50: // '2'
                        case 123: // '{'
                        case 124: // '|'
                            flag5 = true;
                            if(i2 > o)
                            {
                                o = i2;
                            }
                            if(i2 < p)
                            {
                                p = i2;
                            }
                            l1 = S._mthif();
                            break;
                        }
                        if(l1 > o)
                        {
                            o = l1;
                        }
                        if(l1 < p)
                        {
                            p = l1;
                        }
                        if(i1 + c2._flddo <= J && J <= i1 + c2._flddo + c1._flddo && j1 - c1._fldif <= I && I <= j1 + c1.a && F == k1)
                        {
                            A = false;
                            c = false;
                        }
                    }
                }
                if(flag5)
                {
                    m = Math.min(m, i1 + c2._flddo);
                    l = Math.max(l, i1 + c2._flddo + c1._flddo);
                    a = Math.min(a, j1 - c1._fldif);
                    R = Math.max(R, j1 + c1.a);
                    switch(j2)
                    {
                    case 13: // '\r'
                    case 15: // '\017'
                    case 16: // '\020'
                    case 50: // '2'
                    case 123: // '{'
                    case 124: // '|'
                        if(i2 > o)
                        {
                            o = i2;
                        }
                        if(i2 < p)
                        {
                            p = i2;
                        }
                        l1 = S._mthif();
                        break;
                    }
                    if(l1 > o)
                    {
                        o = l1;
                    }
                    if(l1 < p)
                    {
                        p = l1;
                    }
                    flag5 = false;
                }
            }
            c2._flddo += c1._flddo;
            c2._fldif = Math.max(c2._fldif, c1._fldif);
            c2.a = Math.max(c2.a, c1.a);
            if(!flag1 && !flag4)
            {
                flag2 = false;
            }
        }
        return c2;
    }

    private c _mthbyte(int i1, int j1, boolean flag, Graphics g1, int k1)
    {
        c c1 = new c();
        int l1 = 0;
        FontMetrics fontmetrics = g1.getFontMetrics();
        String s1 = D.w;
        if(flag)
        {
            l1 = S._mthif();
        }
        c1 = _mthtry(i1, j1, false, g1, k1, false);
        int i2 = Math.max(c1._flddo, fontmetrics.stringWidth(s1));
        int j2 = c1._fldif + fontmetrics.getAscent() / 2;
        int k2 = c1.a;
        if(flag)
        {
            S.a(l1);
            c c2 = _mthtry(i1, j1, true, g1, k1, false);
            int l2 = 3 * ((i2 - fontmetrics.stringWidth(s1)) / 4);
            if(s1.equals(".") | s1.equals(".."))
            {
                g1.drawString(s1, i1 + l2, j1 - fontmetrics.getAscent());
            } else
            if(s1.equals("\264") | s1.equals("`"))
            {
                g1.drawString(s1, i1 + l2, j1 - fontmetrics.getAscent() / 3);
            } else
            {
                g1.drawString(s1, i1 + l2, j1 - (fontmetrics.getAscent() * 2) / 3);
            }
        }
        return new c(i2, j2, k2);
    }

    private c a(int i1, int j1, boolean flag, Graphics g1)
    {
        c c1 = new c();
        FontMetrics fontmetrics = g1.getFontMetrics();
        int k1 = g1.getFont().getSize() / 2;
        int l1 = fontmetrics.getHeight() - fontmetrics.getDescent();
        int i2 = fontmetrics.getDescent();
        if(flag)
        {
            int j2 = (j1 - l1) + 1;
            int k2 = (j1 + i2) - 1;
            int l2 = (j2 + k2) / 2;
            if(D.w.equals("<"))
            {
                g1.drawLine(i1 + k1, j2, i1, l2);
                g1.drawLine(i1, l2, i1 + k1, k2);
            } else
            {
                g1.drawLine(i1, j2, i1 + k1, l2);
                g1.drawLine(i1 + k1, l2, i1, k2);
            }
        }
        return new c(k1, l1, i2);
    }

    private c _mthif(int i1, int j1, boolean flag, Graphics g1, int k1)
    {
        boolean flag1 = false;
        boolean flag2 = false;
        boolean flag3 = false;
        int l2 = 0;
        int ai[] = new int[100];
        int ai1[] = new int[100];
        int ai2[] = new int[100];
        c c1 = new c();
        int i3 = 0;
        FontMetrics fontmetrics = g1.getFontMetrics();
        int j3 = g1.getFont().getSize();
        if(flag)
        {
            i3 = S._mthif();
        }
        if(!a(3, "ARRAY: BeginSym"))
        {
            return new c(0, 0, 0);
        }
        for(int k3 = 0; k3 < 99; k3++)
        {
            int i2 = 0;
            int k2 = 0;
            for(int l3 = 0; l3 < 99; l3++)
            {
                c c2 = _mthnew(i1, j1, false, g1, k1);
                i2 = Math.max(i2, c2._fldif);
                k2 = Math.max(k2, c2.a);
                ai[l3] = Math.max(ai[l3], c2._flddo + j3);
                if(D.y == 8 || D.y == 4)
                {
                    break;
                }
            }

            ai1[k3] = Math.max(ai1[k3], i2);
            ai2[k3] = Math.max(ai2[k3], k2);
            l2 += i2 + k2;
            if(D.y == 4)
            {
                break;
            }
        }

        int i4 = 0;
        for(int j4 = 0; j4 < 99; j4++)
        {
            i4 += ai[j4];
        }

        if(flag)
        {
            S.a(i3);
            a(3, "ARRAY: Begin");
            int j2 = 0;
            for(int k4 = 0; k4 < 99; k4++)
            {
                int l1 = 0;
                if(k4 == 0)
                {
                    j2 = ai1[k4];
                } else
                {
                    j2 += ai2[k4 - 1] + ai1[k4];
                }
                for(int l4 = 0; l4 < 99; l4++)
                {
                    c c3 = _mthnew(i1 + l1, (j1 - l2 / 2 - fontmetrics.getDescent()) + j2, true, g1, k1);
                    l1 += ai[l4];
                    if(D.y == 8 || D.y == 4)
                    {
                        break;
                    }
                }

                if(D.y == 4)
                {
                    break;
                }
            }

        }
        return new c(i4 - j3, l2 / 2 + fontmetrics.getDescent(), l2 / 2 - fontmetrics.getDescent());
    }

    private c _mthgoto(int i1, int j1, boolean flag, Graphics g1, int k1)
    {
        int i2 = 0;
        int j3 = 0;
        int ai[] = new int[100];
        int ai1[] = new int[100];
        int ai2[] = new int[100];
        int ai3[] = new int[100];
        int ai4[] = new int[100];
        int k3 = 0;
        int l3 = 0;
        int i4 = 0;
        c c1 = new c();
        int j4 = 0;
        FontMetrics fontmetrics = g1.getFontMetrics();
        int k5 = g1.getFont().getSize();
        int l5 = 0;
        boolean flag1 = false;
        boolean flag3 = false;
        boolean flag4 = true;
        boolean flag5 = true;
        boolean flag6 = false;
        boolean flag7 = false;
        if(!a(3))
        {
            return new c(0, 0, 0);
        }
        if(S._mthint().w.equals("eqnarray"))
        {
            flag5 = false;
        }
        if(!a(4, "BEGIN: EndSym"))
        {
            return new c(0, 0, 0);
        }
        if(flag5)
        {
            j4 = S._mthif();
            if(!a(3))
            {
                flag4 = false;
                S.a(j4);
            }
        }
        if(flag5 && flag4)
        {
            e e1 = new e();
            for(e e2 = S._mthint(); e2.y != 4; e2 = S._mthint())
            {
                StringBuffer stringbuffer = new StringBuffer(e2.w);
                for(int i7 = 0; i7 < stringbuffer.length(); i7++)
                {
                    switch(stringbuffer.charAt(i7))
                    {
                    case 108: // 'l'
                        ai3[l5] = 1;
                        if(l5 < 99)
                        {
                            l5++;
                        }
                        break;

                    case 99: // 'c'
                        ai3[l5] = 2;
                        if(l5 < 99)
                        {
                            l5++;
                        }
                        break;

                    case 114: // 'r'
                        ai3[l5] = 3;
                        if(l5 < 99)
                        {
                            l5++;
                        }
                        break;

                    case 64: // '@'
                        ai3[l5] = 4;
                        ai4[l5] = S._mthif();
                        c c2 = _mthtry(i1, j1, false, g1, k1, false);
                        k3 += c2._flddo;
                        l3 = Math.max(l3, c2._fldif);
                        i4 = Math.max(i4, c2.a);
                        if(l5 < 99)
                        {
                            l5++;
                        }
                        break;

                    case 42: // '*'
                        a(3, "Begin *{");
                        int j6;
                        try
                        {
                            j6 = Integer.parseInt(S._mthint().w);
                        }
                        catch(NumberFormatException numberformatexception)
                        {
                            j6 = 0;
                        }
                        a(4, 3, "Begin }{");
                        int l7 = S._mthif();
                        for(int j8 = 0; j8 < j6; j8++)
                        {
                            S.a(l7);
                            for(e2 = S._mthint(); e2.y != 4; e2 = S._mthint())
                            {
                                StringBuffer stringbuffer1 = new StringBuffer(e2.w);
                                for(int l8 = 0; l8 < stringbuffer1.length(); l8++)
                                {
                                    switch(stringbuffer1.charAt(l8))
                                    {
                                    case 108: // 'l'
                                        ai3[l5] = 1;
                                        if(l5 < 99)
                                        {
                                            l5++;
                                        }
                                        break;

                                    case 99: // 'c'
                                        ai3[l5] = 2;
                                        if(l5 < 99)
                                        {
                                            l5++;
                                        }
                                        break;

                                    case 114: // 'r'
                                        ai3[l5] = 3;
                                        if(l5 < 99)
                                        {
                                            l5++;
                                        }
                                        break;

                                    case 64: // '@'
                                        ai3[l5] = 4;
                                        ai4[l5] = S._mthif();
                                        c c3 = _mthtry(i1, j1, false, g1, k1, false);
                                        k3 += c3._flddo;
                                        l3 = Math.max(l3, c3._fldif);
                                        i4 = Math.max(i4, c3.a);
                                        if(l5 < 99)
                                        {
                                            l5++;
                                        }
                                        break;

                                    default:
                                        printStatus("P: begin: illegal format 2");
                                        break;
                                    }
                                }

                            }

                        }

                        break;

                    default:
                        printStatus("P: begin: illegal format 1");
                        break;
                    }
                }

            }

        }
        if(!flag5)
        {
            ai3[0] = 3;
            ai3[1] = 2;
            ai3[2] = 1;
            l5 = 3;
        }
        for(int k6 = 0; k6 < l5 - 1; k6++)
        {
            if(ai3[k6] != 4 && ai3[k6 + 1] != 4)
            {
                i2 += k5 / 2;
            }
        }

        if(flag)
        {
            j4 = S._mthif();
        }
        for(int l6 = 0; l6 < 99; l6++)
        {
            int j2 = 0;
            int l2 = 0;
            for(int j7 = 0; j7 < 99; j7++)
            {
                c c4 = _mthnew(i1, j1, false, g1, k1);
                j2 = Math.max(j2, c4._fldif);
                l2 = Math.max(l2, c4.a);
                ai[j7] = Math.max(ai[j7], c4._flddo);
                if(D.y == 8 || D.y == 51)
                {
                    break;
                }
            }

            j2 = Math.max(j2, l3);
            l2 = Math.max(l2, i4);
            ai1[l6] = j2;
            ai2[l6] = l2;
            j3 += j2 + l2;
            if(D.y == 51)
            {
                break;
            }
        }

        for(int i6 = 0; i6 < 99; i6++)
        {
            i2 += ai[i6];
        }

        i2 += (2 * k5) / 2;
        if(flag)
        {
            S.a(j4);
            int k2 = 0;
            int i3 = j3 / 2 + fontmetrics.getDescent();
            for(int k7 = 0; k7 < 99; k7++)
            {
                int l1 = k5 / 2;
                if(k7 == 0)
                {
                    k2 = ai1[k7];
                } else
                {
                    k2 += ai2[k7 - 1] + ai1[k7];
                }
                int i8 = 0;
                for(int k8 = 0; k8 < 99; k8++)
                {
                    while(ai3[i8] == 4) 
                    {
                        int k4 = S._mthif();
                        S.a(ai4[i8]);
                        c c5 = _mthtry(i1 + l1, (j1 - i3) + k2, true, g1, k1, false);
                        l1 += c5._flddo;
                        S.a(k4);
                        i8++;
                    }
                    switch(ai3[i8])
                    {
                    case 0: // '\0'
                    case 1: // '\001'
                        c c6 = _mthnew(i1 + l1, (j1 - i3) + k2, true, g1, k1);
                        i8++;
                        break;

                    case 2: // '\002'
                        int l4 = S._mthif();
                        c c7 = _mthnew(i1, j1, false, g1, k1);
                        S.a(l4);
                        c7 = _mthnew(i1 + l1 + (ai[k8] - c7._flddo) / 2, (j1 - i3) + k2, true, g1, k1);
                        i8++;
                        break;

                    case 3: // '\003'
                        int i5 = S._mthif();
                        c c8 = _mthnew(i1, j1, false, g1, k1);
                        S.a(i5);
                        c8 = _mthnew((i1 + l1 + ai[k8]) - c8._flddo, (j1 - i3) + k2, true, g1, k1);
                        i8++;
                        break;
                    }
                    if(ai3[i8] != 4)
                    {
                        l1 += k5 / 2;
                    }
                    l1 += ai[k8];
                    boolean flag2 = false;
                    flag3 = false;
                    if(D.y == 8)
                    {
                        flag2 = true;
                    } else
                    if(D.y == 51)
                    {
                        flag2 = true;
                        flag3 = true;
                    }
                    for(; ai3[i8] == 4; i8++)
                    {
                        int j5 = S._mthif();
                        S.a(ai4[i8]);
                        c c9 = _mthtry(i1 + l1, (j1 - i3) + k2, true, g1, k1, false);
                        l1 += c9._flddo;
                        S.a(j5);
                    }

                    if(flag2)
                    {
                        break;
                    }
                }

                if(flag3)
                {
                    break;
                }
            }

        }
        if(!a(3, "BEGIN 2: begin"))
        {
            return new c(0, 0, 0);
        }
        S._mthint();
        if(!a(4, "BEGIN 2: end"))
        {
            return new c(0, 0, 0);
        } else
        {
            return new c(i2 + k3, j3 / 2 + fontmetrics.getDescent(), j3 / 2 - fontmetrics.getDescent());
        }
    }

    private c _mthelse(int i1, int j1, boolean flag, Graphics g1, int k1)
    {
        c c1 = new c();
        int l1 = g1.getFont().getSize() / 2;
        c1 = _mthtry(i1 + l1, j1, flag, g1, k1, false);
        if(flag)
        {
            g1.drawRect(i1 + l1 / 2, j1 - c1._fldif - l1 / 2, c1._flddo + l1, c1._fldif + c1.a + l1);
        }
        return new c(c1._flddo + l1 + l1, c1._fldif + l1, c1.a + l1);
    }

    private c _mthfor(int i1, int j1, boolean flag, Graphics g1, int k1, boolean flag1)
    {
        int l1 = 0;
        c c1 = new c();
        c c3 = new c();
        c c4 = new c();
        int i2 = 0;
        Font font = g1.getFont();
        int j2 = font.getSize();
        a(g1, k1 + 1);
        FontMetrics fontmetrics = g1.getFontMetrics();
        if(flag)
        {
            i2 = S._mthif();
        }
        c3 = _mthtry(i1, j1, false, g1, k1 + 1, false);
        int k2 = c3._fldif + c3.a;
        c4 = _mthtry(i1, j1, false, g1, k1 + 1, false);
        int l2 = Math.max(c3._flddo, c4._flddo);
        int i3 = c4._fldif + c4.a;
        Font font1 = g1.getFont();
        int j3 = (3 * font1.getSize()) / 18;
        l2 += 2 * j3;
        if(fontmetrics.getAscent() < i3)
        {
            l1 = fontmetrics.getAscent() / 2;
        }
        k2 += 2 + l1;
        i3 += 1 - l1;
        if(flag)
        {
            S.a(i2);
            if(flag1)
            {
                g1.drawLine(i1 + j3, j1 - l1, (i1 + l2) - j3, j1 - l1);
            }
            c c2 = _mthtry(i1 + (l2 - c3._flddo) / 2, j1 - 2 - c3.a - l1, true, g1, k1 + 1, false);
            if(c && k1 < F)
            {
                F = k1;
            }
            c2 = _mthtry(i1 + (l2 - c4._flddo) / 2, (j1 + 1 + c4._fldif) - l1, true, g1, k1 + 1, false);
        }
        a(g1, k1);
        return new c(l2, k2, i3);
    }

    private c _mthnew(int i1, int j1, boolean flag, Graphics g1)
    {
        FontMetrics fontmetrics = g1.getFontMetrics();
        if(flag)
        {
            g1.drawString(D.w, i1, j1);
            //System.out.println(g1.getFont());
            //System.out.println("5: "+D.w);            
        }
        int k1 = fontmetrics.stringWidth(D.w);
        int l1 = fontmetrics.getHeight() - fontmetrics.getDescent();
        int i2 = fontmetrics.getDescent();
        return new c(k1, l1, i2);
    }

    private void a(Graphics g1, int i1, int j1, int k1, int l1, int i2)
    {
        g1.drawArc(i1 - k1, j1 - k1, 2 * k1, 2 * k1, l1, i2);
    }

    private void a(Graphics g1, String s1, int i1, int j1, int k1, int l1, int i2, 
            int j2)
    {
        int k2 = j1 / 2;
        int l2 = i1 + k2;
        int i3 = i1 + j1;
        int j3 = i1 + k2 / 2;
        int k3 = l2 + k2 / 2;
        int l3 = (k1 + l1) / 2;
        int i4 = (int)((double)k2 * 0.86602540378444004D);
        int j4 = k1 + i4;
        int k4 = l1 - i4;
        if(s1.equals("["))
        {
            g1.drawLine(j3, k1, j3, l1);
            g1.drawLine(j3, l1, k3, l1);
            g1.drawLine(j3, k1, k3, k1);
        } else
        if(s1.equals("]"))
        {
            g1.drawLine(k3, k1, k3, l1);
            g1.drawLine(j3, l1, k3, l1);
            g1.drawLine(j3, k1, k3, k1);
        } else
        if(s1.equals("|"))
        {
            g1.drawLine(l2, k1, l2, l1);
        } else
        if(s1.equals("||"))
        {
            int l4 = l2 + i2 / 4;
            g1.drawLine(l2, k1, l2, l1);
            g1.drawLine(l4, k1, l4, l1);
        } else
        if(s1.equals("("))
        {
            for(int i5 = j2; i5 < 2 + j2; i5++)
            {
                int i6 = j3 + i5;
                a(g1, k3 + i5, j4, k2, 180, -60);
                g1.drawLine(i6, j4, i6, k4);
                a(g1, k3 + i5, k4, k2, 180, 60);
            }

        } else
        if(s1.equals(")"))
        {
            for(int j5 = j2; j5 < 2 + j2; j5++)
            {
                int j6 = k3 + j5;
                a(g1, j3 + j5, j4, k2, 0, 60);
                g1.drawLine(j6, j4, j6, k4);
                a(g1, j3 + j5, k4, k2, 0, -60);
            }

        } else
        if(s1.equals("<"))
        {
            g1.drawLine(j3, l3, k3, k1);
            g1.drawLine(j3, l3, k3, l1);
        } else
        if(s1.equals(">"))
        {
            g1.drawLine(k3, l3, j3, k1);
            g1.drawLine(k3, l3, j3, l1);
        } else
        if(s1.equals("{"))
        {
            for(int k5 = j2; k5 < 2 + j2; k5++)
            {
                int k6 = l2 + k5;
                a(g1, i3 + k5, j4, k2, 180, -60);
                g1.drawLine(k6, j4, k6, l3 - k2);
                a(g1, i1 + k5, l3 - k2, k2, 0, -90);
                a(g1, i1 + k5, l3 + k2, k2, 0, 90);
                g1.drawLine(k6, l3 + k2, k6, k4);
                a(g1, i3 + k5, k4, k2, 180, 60);
            }

        } else
        if(s1.equals("}"))
        {
            for(int l5 = j2; l5 < 2 + j2; l5++)
            {
                int l6 = l2 + l5;
                a(g1, i1 + l5, j4, k2, 0, 60);
                g1.drawLine(l6, j4, l6, l3 - k2);
                a(g1, i3 + l5, l3 - k2, k2, -180, 90);
                a(g1, i3 + l5, l3 + k2, k2, 180, -90);
                g1.drawLine(l6, l3 + k2, l6, k4);
                a(g1, i1 + l5, k4, k2, 0, -60);
            }

        }
    }

    private c _mthnull(int i1, int j1, boolean flag, Graphics g1, int k1)
    {
        int l1 = 0;
        int i2 = 0;
        c c1 = new c();
        int j2 = 0;
        Font font = g1.getFont();
        int k2 = font.getSize();
        int l2 = (int)(2.0F * (float)k2);
        int i3 = k2 / 9;
        if(flag)
        {
            j2 = S._mthif();
        }
        String s1 = S._mthint().w;
        c1 = _mthnew(i1, j1, false, g1, k1);
        int j3 = c1._flddo;
        int k3 = c1._fldif;
        int l3 = c1.a;
        int i4 = (j1 - k3) + 1;
        int j4 = (j1 + l3) - 1;
        String s3 = S._mthint().w;
        int k4 = (k3 + l3) - 2;
        Font font1=new Font(font.getFamily(), font.getStyle(), k4);
        g1.setFont(font1);
        FontMetrics fontmetrics = g1.getFontMetrics();
        if(s1.equals("<") || s1.equals(">"))
        {
            l1 = k2;
        } else
        if(k4 < l2)
        {
            l1 = fontmetrics.stringWidth(s1);
            if("([{)]}".indexOf(s1) >= 0)
            {
                l1 += i3;
            }
        } else
        {
            l1 = k2;
        }
        if(s3.equals("<") || s3.equals(">"))
        {
            i2 = k2;
        } else
        if(k4 < l2)
        {
            i2 = fontmetrics.stringWidth(s3);
            if("([{)]}".indexOf(s3) >= 0)
            {
                i2 += i3;
            }
        } else
        {
            i2 = k2;
        }
        g1.setFont(font);
        int l4 = S._mthif();
        int j5 = 0;
        int k5 = 0;
        if(S._mthint().y == 11)
        {
            c c2 = _mthdo(i1, j1, false, g1, k1, false);
            j5 = c2._flddo;
            k5 = (j4 + c2._fldif) - (c2._fldif + c2.a) / 2;
            l3 += (c2._fldif + c2.a) / 2;
        } else
        {
            S.a(l4);
        }
        int l5 = S._mthif();
        int j6 = 0;
        int k6 = 0;
        if(S._mthint().y == 10)
        {
            c c3 = _mthint(i1, j1, false, g1, k1, false);
            j6 = c3._flddo;
            k6 = (i4 + c3._fldif) - (c3._fldif + c3.a) / 2;
            k3 += (c3._fldif + c3.a) / 2;
        } else
        {
            S.a(l5);
        }
        j5 = Math.max(j5, j6);
        if(flag)
        {
            S.a(j2);
            String s2 = S._mthint().w;
            if(!s2.equals("."))
            {
                if(k4 < l2 && !s2.equals("<") && !s2.equals(">"))
                {
                    g1.setFont(font1);
                    g1.drawString(s2, i1, j4 - fontmetrics.getDescent() - fontmetrics.getLeading() / 2);
                    g1.setFont(font);
                } else
                {
                    a(g1, s2, i1, l1, i4, j4, k2, 0);
                }
            }
            c c4 = _mthnew(i1 + l1, j1, true, g1, k1);
            String s4 = S._mthint().w;
            if(!s4.equals("."))
            {
                if(k4 < l2 && !s4.equals("<") && !s4.equals(">"))
                {
                    g1.setFont(font1);
                    if("([{)]}".indexOf(s4) < 0)
                    {
                        i3 = 0;
                    }
                    g1.drawString(s4, i1 + j3 + l1 + i3, j4 - fontmetrics.getDescent() - fontmetrics.getLeading() / 2);
                    //System.out.println("2: "+s4);            
                    g1.setFont(font);
                } else
                {
                    a(g1, s4, i1 + j3 + l1, i2, i4, j4, -k2, -1);
                }
            }
            int i5 = S._mthif();
            if(a(11))
            {
                c4 = _mthdo(i1 + j3 + l1 + i2, k5, true, g1, k1, false);
            } else
            {
                S.a(i5);
            }
            int i6 = S._mthif();
            if(a(10))
            {
                c4 = _mthint(i1 + j3 + l1 + i2, k6, true, g1, k1, false);
            } else
            {
                S.a(i6);
            }
        }
        return new c(j3 + l1 + i2 + j5, k3 + 2, l3 + 2);
    }

    private c _mthfor(int i1, int j1, boolean flag, Graphics g1, int k1)
    {
        int l1 = 0;
        c c1 = new c();
        int i2 = 0;
        int j2 = 0;
        FontMetrics fontmetrics = g1.getFontMetrics();
        String s1 = D.w;
        int k2 = S._mthif();
        int l2 = l1 = fontmetrics.stringWidth(s1);
        int i3 = fontmetrics.getHeight() - fontmetrics.getDescent();
        int j3 = fontmetrics.getDescent();
        if(a(11))
        {
            c c2 = _mthdo(i1, j1, false, g1, k1, false);
            i2 = c2._flddo;
            l1 = Math.max(l1, c2._flddo);
            j2 = c2._fldif;
            j3 = c2._fldif + c2.a;
        } else
        {
            S.a(k2);
        }
        if(flag)
        {
            S.a(k2);
            g1.drawString(s1, i1 + (l1 - l2) / 2, j1);
            //System.out.println("4: "+s1);            
            c c3;
            if(a(11))
            {
                c3 = _mthdo(i1 + (l1 - i2) / 2, j1 + j2, true, g1, k1, false);
            } else
            {
                S.a(k2);
            }
        }
        return new c(l1, i3, j3);
    }

    private c _mthdo(int i1, int j1, boolean flag, Graphics g1)
    {
        int k1 = 0;
        int l1 = 0;
        int i2 = 0;
        c c1 = new c();
        if(!a(3))
        {
            return new c(0, 0, 0);
        }
        while(!S._mthnew()) 
        {
            D = S._mthint();
            if(D.y == 4)
            {
                break;
            }
            c c2 = _mthif(i1 + k1, j1, flag, g1);
            k1 += c2._flddo;
            l1 = Math.max(l1, c2._fldif);
            i2 = Math.max(i2, c2.a);
        }
        return new c(k1, l1, i2);
    }

    private c _mthtry(int i1, int j1, boolean flag, Graphics g1, int k1)
    {
        c c1 = new c();
        c1 = _mthtry(i1, j1, flag, g1, k1, false);
        if(flag)
        {
            g1.drawLine(i1 + c1._flddo / 4, j1 + c1.a, i1 + (c1._flddo * 3) / 4, j1 - c1._fldif);
        }
        return c1;
    }

    private c _mthint(int i1, int j1, boolean flag, Graphics g1)
    {
        FontMetrics fontmetrics = g1.getFontMetrics();
        if(flag)
        {
            g1.drawString(D.w, i1 + 1, j1);
            //System.out.println("3: "+D.w);            
        }
        return new c(fontmetrics.stringWidth(D.w) + 2, fontmetrics.getHeight() - fontmetrics.getDescent(), fontmetrics.getDescent());
    }

    private c a(int i1, int j1, boolean flag, Graphics g1, int k1)
    {
        int l1 = 0;
        c c1 = new c();
        int i2 = g1.getFont().getSize() / 4;
        int j2 = i2 / 2;
        int k2 = 0;
        int l2 = 0;
        int i3 = 0;
        if(flag)
        {
            l1 = S._mthif();
        }
        c1 = _mthtry(i1, j1, false, g1, k1, false);
        int j3 = c1._flddo;
        int k3 = j3 / 2;
        int l3 = k3;
        int i4 = c1._fldif;
        int j4 = c1.a;
        int k4 = S._mthif();
        if(a(10))
        {
            c c2 = _mthint(i1, j1, false, g1, k1, false);
            k2 = c2._flddo;
            l3 = Math.max(l3, k2 / 2);
            l2 = i4 + c2.a;
            i3 = c2._fldif + c2.a;
        } else
        {
            S.a(k4);
        }
        if(flag)
        {
            S.a(l1);
            int i5 = (i1 + l3) - k3;
            c c3 = _mthtry(i5, j1, true, g1, k1, false);
            int j5 = (int)((double)i2 * 0.86602540378444004D);
            for(int k5 = 0; k5 < 2; k5++)
            {
                int l5 = (j1 - i4 - j2) + k5;
                a(g1, i5 + j5, l5 + i2, i2, 90, 60);
                g1.drawLine(i5 + j5, l5, (i5 + k3) - i2, l5);
                a(g1, (i5 + k3) - i2, l5 - i2, i2, 0, -90);
                a(g1, i5 + k3 + i2, l5 - i2, i2, -90, -90);
                g1.drawLine(i5 + k3 + i2, l5, (i5 + j3) - j5, l5);
                a(g1, (i5 + j3) - j5, l5 + i2, i2, 90, -60);
            }

            int l4 = S._mthif();
            if(a(10))
            {
                c3 = _mthint((i1 + l3) - k2 / 2, j1 - l2 - i2 - j2, true, g1, k1, false);
            } else
            {
                S.a(l4);
            }
        }
        i4 += i3 + i2 + j2;
        j3 = Math.max(j3, k2);
        return new c(j3, i4, j4);
    }

    private c _mthlong(int i1, int j1, boolean flag, Graphics g1, int k1)
    {
        int l1 = 0;
        c c1 = new c();
        int i2 = g1.getFont().getSize() / 4;
        int j2 = i2 / 2;
        int k2 = 0;
        int l2 = 0;
        int i3 = 0;
        if(flag)
        {
            l1 = S._mthif();
        }
        c1 = _mthtry(i1, j1, false, g1, k1, false);
        int j3 = c1._flddo;
        int k3 = j3 / 2;
        int l3 = k3;
        int i4 = c1._fldif;
        int j4 = c1.a;
        int k4 = S._mthif();
        if(a(11))
        {
            c c2 = _mthdo(i1, j1, false, g1, k1, false);
            k2 = c2._flddo;
            l3 = Math.max(l3, k2 / 2);
            l2 = j4 + c2._fldif;
            i3 = c2._fldif + c2.a;
        } else
        {
            S.a(k4);
        }
        if(flag)
        {
            S.a(l1);
            int i5 = (i1 + l3) - k3;
            c c3 = _mthtry(i5, j1, true, g1, k1, false);
            int j5 = (int)((double)i2 * 0.86602540378444004D);
            for(int k5 = 0; k5 < 2; k5++)
            {
                int l5 = (j1 + j4 + j2) - k5;
                a(g1, i5 + j5, l5 - i2, i2, -90, -60);
                g1.drawLine(i5 + j5, l5, (i5 + k3) - i2, l5);
                a(g1, (i5 + k3) - i2, l5 + i2, i2, 90, -90);
                a(g1, i5 + k3 + i2, l5 + i2, i2, 90, 90);
                g1.drawLine(i5 + k3 + i2, l5, (i5 + j3) - j5, l5);
                a(g1, (i5 + j3) - j5, l5 - i2, i2, -90, 60);
            }

            int l4 = S._mthif();
            if(S._mthint().y == 11)
            {
                c3 = _mthdo((i1 + l3) - k2 / 2, j1 + l2 + i2 + j2, true, g1, k1, false);
            } else
            {
                S.a(l4);
            }
        }
        j4 += i3 + i2 + j2;
        j3 = Math.max(j3, k2);
        return new c(j3, i4, j4);
    }

    private c a(int i1, int j1, boolean flag, Graphics g1, int k1, boolean flag1)
    {
        int l1 = 0;
        c c1 = new c();
        if(flag)
        {
            l1 = S._mthif();
        }
        c1 = _mthtry(i1, j1, false, g1, k1, false);
        if(flag1)
        {
            c1._fldif += 2;
        } else
        {
            c1.a += 2;
        }
        int i2 = c1._fldif;
        int j2 = c1.a;
        if(flag)
        {
            S.a(l1);
            if(flag1)
            {
                g1.drawLine(i1 + 1, (j1 - i2) + 2, (i1 + c1._flddo) - 1, (j1 - i2) + 2);
            } else
            {
                g1.drawLine(i1, (j1 + j2) - 2, i1 + c1._flddo, (j1 + j2) - 2);
            }
            c1 = _mthtry(i1, j1, true, g1, k1, false);
        }
        return new c(c1._flddo, i2, j2);
    }

    private c _mthtry(int i1, int j1, boolean flag, Graphics g1)
    {
        FontMetrics fontmetrics = g1.getFontMetrics();
        int k1 = g1.getFont().getSize() / 9;
        int l1 = fontmetrics.stringWidth(D.w);
        int i2 = "([{)]}".indexOf(D.w);
        if(i2 >= 0)
        {
            l1 += k1;
            if(i2 > 2)
            {
                i1 += k1;
            }
        }
        if(flag)
        {
            g1.drawString(D.w, i1, j1);
            //System.out.println("2: "+D.w);            
        }
        return new c(l1, fontmetrics.getHeight() - fontmetrics.getDescent(), fontmetrics.getDescent());
    }

    private c _mthif(int i1, int j1, boolean flag, Graphics g1)
    {
        FontMetrics fontmetrics = g1.getFontMetrics();
        if(flag)
        {
            g1.drawString(D.w, i1, j1);
            //System.out.println(g1.getFont());
            //System.out.println("1: "+D.w);            
        }
        return new c(fontmetrics.stringWidth(D.w), fontmetrics.getHeight() - fontmetrics.getDescent(), fontmetrics.getDescent());
    }

    private c _mthfor(int i1, int j1, boolean flag, Graphics g1)
    {
        int k1 = 0;
        Font font = g1.getFont();
        try
        {
            k1 = Integer.parseInt(D.w);
        }
        catch(NumberFormatException numberformatexception)
        {
            k1 = 0;
        }
        k1 = (k1 * font.getSize()) / 18;
        return new c(k1, 0, 0);
    }

    private c _mthcase(int i1, int j1, boolean flag, Graphics g1, int k1)
    {
        c c1 = new c();
        int l1 = 0;
        FontMetrics fontmetrics = g1.getFontMetrics();
        int i2 = 0;
        boolean flag1 = false;
        int k2 = 0;
        int l2 = 0;
        boolean flag2 = false;
        if(flag)
        {
            l1 = S._mthif();
        }
        int i3 = fontmetrics.stringWidth("A");
        int j3 = i3 / 2;
        int k3 = S._mthif();
        e e1 = new e();
        e1 = S._mthint();
        if(e1.w.equals("["))
        {
            a(g1, k1 + 1);
            c1 = _mthtry(i1, j1, false, g1, k1 + 1, true);
            a(g1, k1);
            i2 = c1._flddo;
            int j2 = c1._fldif;
            k2 = c1.a;
            l2 = k2 + j2;
            flag2 = true;
        } else
        {
            S.a(k3);
        }
        c1 = _mthtry(i1, j1, false, g1, k1, false);
        int l3 = c1._flddo + i3;
        int i4 = c1._fldif + 2;
        int j4 = c1.a;
        if(flag2 & (i2 > j3))
        {
            l3 += i2 - j3;
        }
        if(flag)
        {
            S.a(l1);
            int k4 = 0;
            if(flag2 & (i2 > j3))
            {
                k4 = i2 - j3;
            }
            g1.drawLine(i1 + k4 + 1, j1 - i4 / 2, i1 + k4 + j3, (j1 + j4) - 1);
            g1.drawLine(i1 + k4 + j3, (j1 + j4) - 1, (i1 + k4 + i3) - 2, (j1 - i4) + 2);
            g1.drawLine((i1 + k4 + i3) - 2, (j1 - i4) + 2, i1 + l3, (j1 - i4) + 2);
            if(flag2)
            {
                e e2 = S._mthint();
                a(g1, k1 + 1);
                c c3;
                if(i2 >= j3)
                {
                    g1.drawLine(i1 + 1, j1 - i4 / 2, i1 + k4 + 1, j1 - i4 / 2);
                    c c2 = _mthtry(i1 + 1, j1 - i4 / 2 - k2 - 1, true, g1, k1 + 1, true);
                } else
                {
                    c3 = _mthtry(i1 + 1 + (j3 - i2), j1 - i4 / 2 - k2 - 1, true, g1, k1 + 1, true);
                }
                a(g1, k1);
            }
            c c4 = _mthtry(i1 + k4 + i3, j1, true, g1, k1, false);
        }
        if(flag2 & (i4 / 2 < l2))
        {
            i4 = i4 / 2 + l2;
        }
        return new c(l3, i4, j4);
    }

    private c _mthchar(int i1, int j1, boolean flag, Graphics g1, int k1)
    {
        c c1 = new c();
        int l1 = 0;
        int i2 = g1.getFontMetrics().getLeading();
        if(flag)
        {
            l1 = S._mthif();
        }
        c1 = _mthint(i1, j1, false, g1, k1, true);
        int j2 = c1._flddo;
        int k2 = c1._flddo;
        int l2 = (c1._fldif + c1.a) - i2;
        int i3 = c1.a - i2;
        c1 = _mthtry(i1, j1, false, g1, k1, false);
        j2 = Math.max(j2, c1._flddo);
        int j3 = j2 / 2;
        int k3 = c1._flddo;
        l2 += c1._fldif;
        int l3 = c1.a;
        i3 += c1._fldif;
        if(flag)
        {
            S.a(l1);
            c c2 = _mthint((i1 + j3) - k2 / 2, j1 - i3, true, g1, k1, false);
            c2 = _mthtry((i1 + j3) - k3 / 2, j1, true, g1, k1, false);
        }
        return new c(j2, l2, l3);
    }

    private c _mthdo(int i1, int j1, boolean flag, Graphics g1, int k1, boolean flag1)
    {
        int l1 = 0;
        int i2 = 0;
        c c1 = new c();
        int j2 = 0;
        int l2 = g1.getFontMetrics().getAscent() / 2;
        if(flag)
        {
            j2 = S._mthif();
        }
        a(g1, k1 + 1);
        c1 = _mthtry(i1, j1, false, g1, k1 + 1, false);
        int i3 = c1._flddo;
        if(flag1)
        {
            l1 = l2 - 1;
            i2 = (c1._fldif + c1.a) - l1;
        } else
        {
            i2 = c1._fldif + c1.a;
        }
        if(flag)
        {
            S.a(j2);
            if(flag1)
            {
                c1 = _mthtry(i1, (j1 + c1._fldif) - l1, true, g1, k1 + 1, false);
            } else
            {
                c1 = _mthtry(i1, j1 + c1._fldif, true, g1, k1 + 1, false);
            }
        }
        a(g1, k1);
        if(flag1)
        {
            int k2 = S._mthif();
            if(a(10))
            {
                c c2 = _mthint(i1, j1, flag, g1, k1, true);
                i3 = Math.max(i3, c2._flddo);
                l1 = Math.max(l1, c2._fldif);
            } else
            {
                S.a(k2);
            }
        }
        return new c(i3, l1, i2);
    }

    private c _mthint(int i1, int j1, boolean flag, Graphics g1, int k1, boolean flag1)
    {
        int l1 = 0;
        int i2 = 0;
        c c1 = new c();
        int j2 = 0;
        int l2 = g1.getFontMetrics().getAscent() / 2;
        if(flag)
        {
            j2 = S._mthif();
        }
        a(g1, k1 + 1);
        c1 = _mthtry(i1, j1, false, g1, k1 + 1, false);
        int i3 = c1._flddo;
        if(flag1)
        {
            i2 = -l2 - 1;
            l1 = (c1._fldif + c1.a) - i2;
        } else
        {
            l1 = c1._fldif + c1.a;
        }
        if(flag)
        {
            S.a(j2);
            if(flag1)
            {
                c1 = _mthtry(i1, (j1 - c1.a) + i2, true, g1, k1 + 1, false);
            } else
            {
                c1 = _mthtry(i1, j1 - c1.a, true, g1, k1 + 1, false);
            }
        }
        a(g1, k1);
        if(flag1)
        {
            int k2 = S._mthif();
            if(a(11))
            {
                c c2 = _mthdo(i1, j1, flag, g1, k1, true);
                i3 = Math.max(i3, c2._flddo);
                i2 = Math.max(i2, c2.a);
            } else
            {
                S.a(k2);
            }
        }
        return new c(i3, l1, i2);
    }

    private Image _mthif(Graphics g1, int i1)
    {
        String s1 = D.w + j[i1 - 1] + g1.getColor().getRGB();
        if(!w.containsKey(s1))
        {
            String s2 = "Fonts/Greek" + j[i1 - 1] + "/" + D.w + ".gif";
            Image image = _flddo.a(s2, g1);
            int j1 = S._mthif();
            B.addImage(image, j1);
            a("Loading " + D.w);
            try
            {
                B.waitForID(j1, 10000L);
            }
            catch(InterruptedException interruptedexception) { }
            if(B.isErrorID(j1))
            {
                a("Error loading " + D.w);
            } else
            {
                w.put(s1, image);
            }
            return image;
        } else
        {
            return (Image)w.get(s1);
        }
    }

    private c _mthif(int i1, int j1, boolean flag, Graphics g1, int k1, boolean flag1)
    {
        FontMetrics fontmetrics = g1.getFontMetrics();
        k1 = Math.min(k1, j.length);
        Image image = _mthif(g1, k1);
        int l1 = image.getWidth(PC);
        if(l1 < 0)
        {
            l1 = fontmetrics.getMaxAdvance();
        }
        if(flag)
        {
            int i2 = 0;
            if(flag1)
            {
                i2 = _fldfor[k1 - 1];
            }
            g1.drawImage(image, i1, (j1 - image.getHeight(PC)) + i2, PC);
        }
        return new c(l1, fontmetrics.getHeight() - fontmetrics.getDescent(), fontmetrics.getDescent());
    }

    private c _mthdo(int i1, int j1, boolean flag, Graphics g1, int k1)
    {
        int l1 = 0;
        c c1 = new c();
        int i2 = 0;
        int j2 = 0;
        int k2 = 0;
        int l2 = 0;
        int i3 = 0;
        int j3 = g1.getFontMetrics().getAscent();
        k1 = Math.min(k1, j.length);
        Image image = _mthif(g1, k1);
        int k3 = l1 = image.getWidth(PC);
        int l3 = image.getHeight(PC);
        if(l3 < 0)
        {
            l3 = 2 * j3;
            k3 = l1 = j3;
        }
        int i4 = (int)((double)(l3 / 2) - 0.40000000000000002D * (double)j3);
        int j4 = i3 = l3 - i4;
        int k4 = S._mthif();
        if(a(11))
        {
            c c2 = _mthdo(i1, j1, false, g1, k1, false);
            k2 = c2._flddo;
            l1 = Math.max(l1, c2._flddo);
            i2 = i4 + c2._fldif;
            i4 += c2._fldif + c2.a;
        } else
        {
            S.a(k4);
        }
        int l4 = S._mthif();
        if(a(10))
        {
            c c3 = _mthint(i1, j1, false, g1, k1, false);
            l2 = c3._flddo;
            l1 = Math.max(l1, c3._flddo);
            j2 = j4 + c3.a;
            j4 += c3._fldif + c3.a;
        } else
        {
            S.a(l4);
        }
        if(flag)
        {
            S.a(k4);
            g1.drawImage(image, i1 + (l1 - k3) / 2, j1 - i3, PC);
            c c4;
            if(a(11))
            {
                c4 = _mthdo(i1 + (l1 - k2) / 2, j1 + i2, true, g1, k1, false);
            } else
            {
                S.a(k4);
            }
            int i5 = S._mthif();
            if(a(10))
            {
                c4 = _mthint(i1 + (l1 - l2) / 2, j1 - j2, true, g1, k1, false);
            } else
            {
                S.a(i5);
            }
        }
        return new c(l1, j4, i4);
    }

    private c _mthint(int i1, int j1, boolean flag, Graphics g1, int k1)
    {
        c c1 = new c();
        int l1 = g1.getFont().getSize();
        String s1 = D.w;
        c1 = _mthtry(i1, j1, flag, g1, k1, false);
        int i2 = c1._flddo;
        int j2 = i2 / 2;
        int k2 = l1 / 4;
        int l2 = c1._fldif + k2;
        int i3 = c1.a;
        if(flag)
        {
            int j3 = (j1 - l2) + k2;
            int k3 = l1 / 8;
            int l3 = i1 + i2;
            int i4 = i1 + j2;
            if(s1.equals(""))
            {
                g1.drawLine(i1, j3, l3, j3);
                g1.drawLine(i1 + (int)((double)i2 * 0.80000000000000004D), j3 - k3, l3, j3);
                g1.drawLine(i1 + (int)((double)i2 * 0.80000000000000004D), j3 + k3, l3, j3);
            } else
            if(s1.equals("bar"))
            {
                g1.drawLine(i1, j3, l3, j3);
            } else
            if(s1.equals("widehat"))
            {
                g1.drawLine(i1, j3, i4, j3 - k2);
                g1.drawLine(i4, j3 - k2, l3, j3);
            } else
            if(s1.equals("widetilde"))
            {
                boolean flag1 = false;
                int k4 = 0;
                for(int l4 = 1; l4 < j2; l4++)
                {
                    int j4 = k4;
                    k4 = (int)((double)k3 * Math.sin((4.0840704496667311D * (double)l4) / (double)j2));
                    g1.drawLine((i4 + l4) - 1, j3 + j4, i4 + l4, j3 + k4);
                    g1.drawLine((i4 - l4) + 1, j3 - j4, i4 - l4, j3 - k4);
                }

            }
        }
        return new c(i2, l2 + 2, i3);
    }

    private boolean a(int i1)
    {
        return a(i1, "");
    }

    private boolean a(int i1, String s1)
    {
        int j1;
        while((j1 = S._mthint().y) == 25) ;
        if(j1 == i1)
        {
            return true;
        }
        if(!s1.equals(""))
        {
            printStatus("Parser: " + s1 + " not found");
        }
        return false;
    }

    private boolean a(int i1, int j1)
    {
        return a(i1, j1, "");
    }

    private boolean a(int i1, int j1, String s1)
    {
        int k1;
        while((k1 = S._mthint().y) == 25) ;
        boolean flag = k1 == i1;
        while((k1 = S._mthint().y) == 25) ;
        flag = k1 == j1;
        if(!flag && !s1.equals(""))
        {
            printStatus("Parser: " + s1 + " not found");
        }
        return flag;
    }

    private void a(Graphics g1, int i1)
    {
        if(i1 <= 1)
        {
            g1.setFont(d);
        } else
        if(i1 == 2)
        {
            g1.setFont(b);
        } else
        if(i1 == 3)
        {
            g1.setFont(_fldnull);
        } else
        {
            g1.setFont(_fldlong);
        }
    }

}
