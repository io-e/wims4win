package atp;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.Hashtable;
import java.util.StringTokenizer;

// Referenced classes of package atp:
//            a

class f
{

    private ImageProducer _flddo[] = {
        null, null, null, null, null
    };
    private String _fldif[] = {
        "8", "10", "12", "14", "18"
    };
    private Hashtable _fldfor;
    private static boolean a = true;

    public f()
    {
        _fldfor = new Hashtable(189);
    }

    public Image a(String s, Graphics g)
    {
        StringTokenizer stringtokenizer = new StringTokenizer(s, "/");
        String s1 = stringtokenizer.nextToken();
        s1 = stringtokenizer.nextToken().substring(5);
        String s2 = stringtokenizer.nextToken();
        int i = -1;
        boolean flag2 = true;
        while(flag2) 
        {
            if(_fldif[++i].equals(s1))
            {
                flag2 = false;
            }
            if(i == 4)
            {
                flag2 = false;
            }
        }
        if(_flddo[i] == null)
        {
            _flddo[i] = a("Fonts" + s1 + ".gif");
            String s3 = "Des" + s1 + ".gif";
            BufferedInputStream bufferedinputstream = null;
            try
            {
                InputStream inputstream = getClass().getResourceAsStream(s3);
                   bufferedinputstream = new BufferedInputStream(getClass().getResourceAsStream(s3));
                ObjectInputStream objectinputstream = new ObjectInputStream(bufferedinputstream);
                int j = objectinputstream.readInt();
                for(int k = 0; k < j; k++)
                {
                    String s4 = (String)objectinputstream.readObject();
                    _fldfor.put(s1 + s4, new Rectangle((Rectangle)objectinputstream.readObject()));
                }

                bufferedinputstream.close();
            }
            catch(Exception exception1)
            {
                //System.out.println(exception1.toString());
                _flddo[i] = null;
            }
        }
        Image image = null;
        if(_flddo[i] != null)
        {
            Rectangle rectangle = (Rectangle)_fldfor.get(s1 + s2);
            image = Toolkit.getDefaultToolkit().createImage(new FilteredImageSource(new FilteredImageSource(_flddo[i], new CropImageFilter(rectangle.x, rectangle.y, rectangle.width, rectangle.height)), new a(g.getColor())));
        }
        return image;
    }

    ImageProducer a (String s)
    {
        ImageProducer imageproducer = null;
        try
        {
            InputStream inputstream = getClass().getResourceAsStream(s);
            int i = inputstream.available();
            byte abyte0[] = new byte[i];
            int j = 0;
            for(int k = 0; k != -1;)
            {
                k = inputstream.read(abyte0, j, i);
                if(k != -1)
                {
                    j += k;
                    i = inputstream.available();
                    int l = j + i;
                    if(l > abyte0.length)
                    {
                        byte abyte1[] = (byte[])abyte0.clone();
                        abyte0 = new byte[l];
                        System.arraycopy(abyte1, 0, abyte0, 0, j);
                    }
                }
                if(i == 0)
                {
                    break;
                }
            }

            imageproducer = Toolkit.getDefaultToolkit().createImage(abyte0).getSource();
        }
        catch(Exception exception) { }
        return imageproducer;
    }

}
