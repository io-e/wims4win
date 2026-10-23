package atp;

import java.applet.Applet;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.net.URL;
import java.util.Hashtable;
import java.util.StringTokenizer;

// Referenced classes of package atp:
//            a

class d
{

    private ImageProducer _flddo[] = {
        null, null, null, null, null
    };
    private String _fldif[] = {
        "8", "10", "12", "14", "18"
    };
    private Hashtable _fldfor;
    private static boolean a = true;

    public d()
    {
        _fldfor = new Hashtable(189);
    }

    public Image a(boolean flag, boolean flag1, String s, Graphics g, Applet applet)
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
            _flddo[i] = a(flag, flag1, "Fonts" + s1 + ".gif", applet);
            String s3 = "Des" + s1 + ".gif";
            BufferedInputStream bufferedinputstream = null;
            try
            {
                if(a)
                {
                    InputStream inputstream = getClass().getResourceAsStream(s3);
                    bufferedinputstream = new BufferedInputStream(getClass().getResourceAsStream(s3));
                } else
                if((!flag) & (!flag1))
                {
                    bufferedinputstream = new BufferedInputStream((new URL(s3)).openStream());
                } else
                if(flag)
                {
                    bufferedinputstream = new BufferedInputStream((new URL(applet.getCodeBase(), s3)).openStream());
                } else
                {
                    try
                    {
                        bufferedinputstream = new BufferedInputStream(getClass().getResource(s3).openStream());
                    }
                    catch(Exception exception) { }
                }
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

    public ImageProducer a(boolean flag, boolean flag1, String s, Applet applet)
    {
        ImageProducer imageproducer = null;
        if(a)
        {
            imageproducer = a(s);
        }
        if(imageproducer == null)
        {
            a = false;
            if((!flag) & (!flag1))
            {
                imageproducer = Toolkit.getDefaultToolkit().getImage(s).getSource();
            } else
            if(flag)
            {
                imageproducer = applet.getImage(applet.getCodeBase(), s).getSource();
            } else
            {
                try
                {
                    URL url = getClass().getResource(s);
                    imageproducer = (ImageProducer)url.getContent();
                }
                catch(Exception exception) { }
            }
        }
        return imageproducer;
    }

    ImageProducer a(String s)
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
