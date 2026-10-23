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

import java.applet.Applet;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.net.URL;
import java.util.Hashtable;
import java.util.StringTokenizer;

// Referenced classes of package atp:
//            ColorMaskFilter

class SymbolLoader
{

    public SymbolLoader()
    {
        fontdesH = new Hashtable(189);
    }

    public Image getImage(boolean flag, boolean flag1, String s, Graphics g, Applet applet)
    {
        StringTokenizer stringtokenizer = new StringTokenizer(s, "/");
        String s1 = stringtokenizer.nextToken();
        s1 = stringtokenizer.nextToken().substring(5);
        String s2 = stringtokenizer.nextToken();
        int i = -1;
        boolean flag2 = true;
        do
        {
            if(!flag2)
                break;
            if(fontsizes[++i].equals(s1))
                flag2 = false;
            if(i == 4)
                flag2 = false;
        } while(true);
        if(imageSources[i] == null)
        {
            imageSources[i] = getBigImage(flag, flag1, (new StringBuilder()).append("Fonts").append(s1).append(".gif").toString(), applet);
            String s3 = (new StringBuilder()).append("Des").append(s1).append(".gif").toString();
            BufferedInputStream bufferedinputstream = null;
            try
            {
                if(kLocalFonts)
                {
                    InputStream inputstream = getClass().getResourceAsStream(s3);
                    bufferedinputstream = new BufferedInputStream(getClass().getResourceAsStream(s3));
                } else
                if((!flag) & (!flag1))
                    bufferedinputstream = new BufferedInputStream((new URL(s3)).openStream());
                else
                if(flag)
                    bufferedinputstream = new BufferedInputStream((new URL(applet.getCodeBase(), s3)).openStream());
                else
                    try
                    {
                        bufferedinputstream = new BufferedInputStream(getClass().getResource(s3).openStream());
                    }
                    catch(Exception exception) { }
                ObjectInputStream objectinputstream = new ObjectInputStream(bufferedinputstream);
                int j = objectinputstream.readInt();
                for(int k = 0; k < j; k++)
                {
                    String s4 = (String)objectinputstream.readObject();
                    fontdesH.put((new StringBuilder()).append(s1).append(s4).toString(), new Rectangle((Rectangle)objectinputstream.readObject()));
                }

                bufferedinputstream.close();
            }
            catch(Exception exception1)
            {
                System.out.println(exception1.toString());
                imageSources[i] = null;
            }
        }
        Image image = null;
        if(imageSources[i] != null)
        {
            Rectangle rectangle = (Rectangle)(Rectangle)fontdesH.get((new StringBuilder()).append(s1).append(s2).toString());
            image = Toolkit.getDefaultToolkit().createImage(new FilteredImageSource(new FilteredImageSource(imageSources[i], new CropImageFilter(rectangle.x, rectangle.y, rectangle.width, rectangle.height)), new ColorMaskFilter(g.getColor())));
        }
        return image;
    }

    public ImageProducer getBigImage(boolean flag, boolean flag1, String s, Applet applet)
    {
        ImageProducer imageproducer = null;
        if(kLocalFonts)
            imageproducer = getLocalImageSource(s);
        if(imageproducer == null)
        {
            kLocalFonts = false;
            if((!flag) & (!flag1))
                imageproducer = Toolkit.getDefaultToolkit().getImage(s).getSource();
            else
            if(flag)
                imageproducer = applet.getImage(applet.getCodeBase(), s).getSource();
            else
                try
                {
                    URL url = getClass().getResource(s);
                    imageproducer = (ImageProducer)url.getContent();
                }
                catch(Exception exception) { }
        }
        return imageproducer;
    }

    ImageProducer getLocalImageSource(String s)
    {
        ImageProducer imageproducer = null;
        try
        {
            InputStream inputstream = getClass().getResourceAsStream(s);
            int i = inputstream.available();
            byte abyte0[] = new byte[i];
            int j = 0;
            int k = 0;
            do
            {
                if(k == -1)
                    break;
                k = inputstream.read(abyte0, j, i);
                if(k != -1)
                {
                    j += k;
                    i = inputstream.available();
                    int l = j + i;
                    if(l > abyte0.length)
                    {
                        byte abyte1[] = (byte[])(byte[])abyte0.clone();
                        abyte0 = new byte[l];
                        System.arraycopy(abyte1, 0, abyte0, 0, j);
                    }
                }
            } while(i != 0);
            imageproducer = Toolkit.getDefaultToolkit().createImage(abyte0).getSource();
        }
        catch(Exception exception) { }
        return imageproducer;
    }

    private ImageProducer imageSources[] = {
        null, null, null, null, null
    };
    private String fontsizes[] = {
        "8", "10", "12", "14", "18"
    };
    private Hashtable fontdesH;
    private static boolean kLocalFonts = true;

}
