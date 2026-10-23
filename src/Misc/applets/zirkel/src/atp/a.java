package atp;

import java.awt.Color;
import java.awt.image.RGBImageFilter;

class a extends RGBImageFilter
{

    Color a;
    boolean _fldif;

    a(Color color)
    {
        _fldif = false;
        a = color;
        _fldif = false;
        super.canFilterIndexColorModel = true;
    }

    a(Color color, boolean flag)
    {
        _fldif = false;
        a = color;
        _fldif = flag;
        super.canFilterIndexColorModel = true;
    }

    public int filterRGB(int i, int j, int k)
    {
        if(_fldif)
        {
            return 0x1fff0000;
        }
        int l = k & 0xffffff;
        if(l == 0xffffff)
        {
            return l;
        } else
        {
            return 0xff000000 | a.getRGB();
        }
    }
}
