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

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.PrintStream;
import java.util.Hashtable;
import javax.swing.JApplet;
import javax.swing.JComponent;

// Referenced classes of package atp:
//            sSymbolLoader, EqScanner, ColorMaskFilter, BoxC, 
//            EqToken

public class sHotEqn extends JComponent
    implements MouseListener, MouseMotionListener
{

    public sHotEqn()
    {
        this("sHotEqn", null, "sHotEqn");
    }

    public sHotEqn(String s)
    {
        this(s, null, "sHotEqn");
    }

    public sHotEqn(String s, JApplet japplet, String s1)
    {
        width = 0;
        height = 0;
        nameS = null;
        equation = null;
//       Fontname = "Helvetica";
        Fontname = "Utopia";
        f1 = new Font(Fontname, 0, 18);
        f2 = new Font(Fontname, 0, 14);
        f3 = new Font(Fontname, 0, 12);
        f4 = new Font(Fontname, 0, 10);
        imageOK = false;
        localWidth = 0;
        localHeight = 0;
        BGColor = Color.white;
        EnvColor = Color.white;
        FGColor = Color.black;
        BorderColor = Color.red;
        borderB = false;
        roundRectBorderB = false;
        border = 0;
        halign = "left";
        valign = "top";
        xpos = 0;
        ypos = 0;
        drawn = false;
        imageH = new Hashtable(13);
        appletB = false;
        beanB = false;
        debug = false;
        editMode = false;
        editableB = true;
        mouse1X = 0;
        mouse1Y = 0;
        mouse2X = 0;
        mouse2Y = 0;
        xOFF = 0;
        yOFF = 0;
        y0 = 0;
        x0 = 0;
        y1 = 0;
        x1 = 0;
        editModeRec = 5;
        editModeFind = false;
        editModeCount1 = 0;
        editModeCount2 = 0;
        app = japplet;
        equation = s;
        nameS = s1;
        addMouseListener(this);
        addMouseMotionListener(this);
        if(japplet != null)
            appletB = true;
        symbolLoader = new SymbolLoader();
        tracker = new MediaTracker(this);
        eqScan = new EqScanner(s);
        //System.out.println((new StringBuilder()).append("sHotEqn V 4.00 ").append(s1).toString());
    }

    public void setEquation(String s)
    {
        equation = s;
        eqScan.setEquation(s);
        drawn = false;
        imageOK = false;
        repaint();
    }

    public String getEquation()
    {
        return equation;
    }

    public void printStatus(String s)
    {
        if(debug)
            System.out.println((new StringBuilder()).append(nameS).append(" ").append(s).toString());
    }

    private void displayStatus(String s)
    {
        if(debug)
            if(appletB)
                app.showStatus((new StringBuilder()).append(nameS).append(" ").append(s).toString());
            else
                printStatus(s);
    }

    public Image getImage()
    {
        if(imageOK)
            return bufferImage;
        else
            return null;
    }

    public BufferedImage GetMyImage()
    {
        return finalimage;
    }

    public void setDebug(boolean flag)
    {
        debug = flag;
    }

    public boolean isDebug()
    {
        return debug;
    }

    public void setFontname(String s)
    {
        Fontname = s;
    }

    public String getFontname()
    {
        return Fontname;
    }

    public void setFontsizes(int i, int j, int k, int l)
    {
        int i1 = 18;
        int j1 = 14;
        int k1 = 12;
        int l1 = 10;
        GreekSize[0] = 0;
        GreekSize[1] = 0;
        GreekSize[2] = 0;
        GreekSize[3] = 0;
        for(int i2 = 0; i2 < GreekFontSizes.length; i2++)
        {
            if(i == GreekFontSizes[i2])
            {
                GreekSize[0] = i;
                GreekDescent[0] = GreekFontDescents[i2];
                i1 = EmbedFontSizes[i2];
            }
            if(j == GreekFontSizes[i2])
            {
                GreekSize[1] = j;
                GreekDescent[1] = GreekFontDescents[i2];
                j1 = EmbedFontSizes[i2];
            }
            if(k == GreekFontSizes[i2])
            {
                GreekSize[2] = k;
                GreekDescent[2] = GreekFontDescents[i2];
                k1 = EmbedFontSizes[i2];
            }
            if(l == GreekFontSizes[i2])
            {
                GreekSize[3] = l;
                GreekDescent[3] = GreekFontDescents[i2];
                l1 = EmbedFontSizes[i2];
            }
        }

        if(GreekSize[0] == 0)
        {
            GreekSize[0] = GreekFontSizes[GreekFontSizes.length - 1];
            GreekDescent[0] = GreekFontDescents[GreekFontDescents.length - 1];
            i1 = EmbedFontSizes[EmbedFontSizes.length - 1];
        }
        if(GreekSize[1] == 0)
        {
            GreekSize[1] = GreekSize[0];
            GreekDescent[1] = GreekDescent[0];
            j1 = i1;
        }
        if(GreekSize[2] == 0)
        {
            GreekSize[2] = GreekSize[1];
            GreekDescent[2] = GreekDescent[1];
            k1 = j1;
        }
        if(GreekSize[3] == 0)
        {
            GreekSize[3] = GreekSize[2];
            GreekDescent[3] = GreekDescent[2];
            l1 = k1;
        }
        f1 = new Font(Fontname, 0, i1);
        f2 = new Font(Fontname, 0, j1);
        f3 = new Font(Fontname, 0, k1);
        f4 = new Font(Fontname, 0, l1);
    }

    public void setBackground(Color color)
    {
        BGColor = EnvColor = color;
        drawn = false;
        imageOK = false;
        repaint();
    }

    public Color getBackground()
    {
        return BGColor;
    }

    public void setForeground(Color color)
    {
        FGColor = color;
        drawn = false;
        imageOK = false;
        repaint();
    }

    public Color getForeground()
    {
        return FGColor;
    }

    public void setBorderColor(Color color)
    {
        BorderColor = color;
        drawn = false;
        imageOK = false;
        repaint();
    }

    public Color getBorderColor()
    {
        return BorderColor;
    }

    public void setBorder(boolean flag)
    {
        borderB = flag;
        drawn = false;
        imageOK = false;
        repaint();
    }

    public boolean isBorder()
    {
        return borderB;
    }

    public void setRoundRectBorder(boolean flag)
    {
        roundRectBorderB = flag;
        drawn = false;
        imageOK = false;
        repaint();
    }

    public boolean isRoundRectBorder()
    {
        return roundRectBorderB;
    }

    public void setHAlign(String s)
    {
        halign = s;
        drawn = false;
        imageOK = false;
    }

    public void setEnvColor(Color color)
    {
        EnvColor = color;
        drawn = false;
        imageOK = false;
        repaint();
    }

    public Color getEnvColor()
    {
        return EnvColor;
    }

    public String getHAlign()
    {
        return halign;
    }

    public void setVAlign(String s)
    {
        valign = s;
        drawn = false;
        imageOK = false;
    }

    public String getVAlign()
    {
        return valign;
    }

    public void setEditable(boolean flag)
    {
        editableB = flag;
    }

    public boolean isEditable()
    {
        return editableB;
    }

    public String getSelectedArea()
    {
        return eqScan.getSelectedArea(editModeCount1, editModeCount2);
    }

    public void mousePressed(MouseEvent mouseevent)
    {
    }

    public void mouseReleased(MouseEvent mouseevent)
    {
    }

    public void mouseEntered(MouseEvent mouseevent)
    {
    }

    public void mouseExited(MouseEvent mouseevent)
    {
    }

    public void mouseClicked(MouseEvent mouseevent)
    {
    }

    public void mouseMoved(MouseEvent mouseevent)
    {
    }

    public void mouseDragged(MouseEvent mouseevent)
    {
    }

    public void processMouseEvent(MouseEvent mouseevent)
    {
        if(mouseevent.isControlDown())
        {
            if(mouseevent.getID() == 501 && !mouseevent.isMetaDown())
                System.out.println((new StringBuilder()).append(nameS).append(" (width,height) given=(").append(getSize().width).append(",").append(getSize().height).append(") used=(").append(getPreferredSize().width).append(",").append(getPreferredSize().height).append(")").toString());
        } else
        if(editableB)
            if(mouseevent.getID() == 501)
            {
                mouse1X = mouseevent.getX();
                mouse1Y = mouseevent.getY();
                mouse2X = 0;
                mouse2Y = 0;
                editModeRec = 5;
                selectImage = null;
                repaint();
            } else
            if(mouseevent.getID() == 502 && editMode)
            {
                Graphics g = getGraphics();
                g.setFont(f1);
                g.setColor(FGColor);
                eqScan.start();
                BoxC boxc = eqn(xOFF, yOFF, true, g, 1);
                if(debug)
                    printStatus((new StringBuilder()).append("selectedArea = ").append(eqScan.getSelectedArea(editModeCount1, editModeCount2)).toString());
                FilteredImageSource filteredimagesource = new FilteredImageSource(bufferImage.getSource(), new CropImageFilter(x0, y0, x1 - x0, y1 - y0));
                FilteredImageSource filteredimagesource1 = new FilteredImageSource(filteredimagesource, new ColorMaskFilter(Color.red, true));
                selectImage = Toolkit.getDefaultToolkit().createImage(filteredimagesource1);
                g.drawImage(selectImage, x0, y0, this);
                editMode = false;
            }
        super.processMouseEvent(mouseevent);
    }

    public void processMouseMotionEvent(MouseEvent mouseevent)
    {
        if(mouseevent.getID() == 506 && mouse1X != 0 && editableB)
        {
            editMode = true;
            mouse2X = mouseevent.getX();
            mouse2Y = mouseevent.getY();
        }
    }

    public Dimension getPreferredSize()
    {
        if((width == 0) & (height == 0))
        {
            Graphics g = getGraphics();
            if(g != null)
            {
                g.setFont(f1);
                eqScan.start();
                BoxC boxc = eqn(0, 150, false, g, 1);
                if(borderB)
                    border = 5;
                else
                    border = 0;
                localWidth = 1 + boxc.dx + 2 * border;
                localHeight = 1 + boxc.dy_pos + boxc.dy_neg + 2 * border;
            }
        }
        width = localWidth;
        height = localHeight;
        if(localWidth <= 1)
            return new Dimension(100, 100);
        else
            return new Dimension(localWidth, localHeight);
    }

    public Dimension getSizeof(String s)
    {
        Image image = createImage(200, 200);
        Graphics g = image.getGraphics();
        g.setFont(f1);
        eqScan.setEquation(s);
        BoxC boxc = eqn(0, 150, false, g, 1);
        g.dispose();
        byte byte0;
        if(borderB)
            byte0 = 5;
        else
            byte0 = 0;
        return new Dimension(1 + boxc.dx + 2 * byte0, 1 + boxc.dy_pos + boxc.dy_neg + 2 * byte0);
    }

    public Dimension getMinimumSize()
    {
        return getPreferredSize();
    }

    public void addActionListener(ActionListener actionlistener)
    {
        actionListener = AWTEventMulticaster.add(actionListener, actionlistener);
        enableEvents(16L);
    }

    public void removeActionListener(ActionListener actionlistener)
    {
        actionListener = AWTEventMulticaster.remove(actionListener, actionlistener);
    }

    private void draw_hourglass(Graphics g)
    {
        g.setColor(Color.red);
        int i = height / 2 - 10;
        int j = i + 20;
        int k = width / 2 - 5;
        int l = k + 10;
        Polygon polygon = new Polygon();
        polygon.addPoint(k, i);
        polygon.addPoint(l, j);
        polygon.addPoint(k, j);
        polygon.addPoint(l, i);
        g.fillPolygon(polygon);
    }

    public synchronized void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        if(width != getSize().width || height != getSize().height)
        {
            imageOK = false;
            bufferImage = null;
            width = getSize().width;
            height = getSize().height;
        }
        if(!imageOK)
        {
            draw_hourglass(g);
            selectImage = null;
            if(bufferImage == null)
                bufferImage = createImage(width, height);
            Graphics g1 = bufferImage.getGraphics();
            generateImage(g1);
            g1.dispose();
        }
        g.drawImage(bufferImage, 0, 0, this);
        if(selectImage != null)
            g.drawImage(selectImage, x0, y0, this);
    }

    private synchronized void generateImage(Graphics g)
    {
        BoxC boxc = new BoxC();
        Image image = createImage(width, height + height);
        Graphics g1 = image.getGraphics();
        g1.setFont(f1);
        g.setColor(BGColor);
        g.fillRect(0, 0, width, height);
        g1.setColor(BGColor);
        g1.fillRect(0, 0, width, height + height);
        border = 0;
        if(borderB && roundRectBorderB)
        {
            g.setColor(EnvColor);
            g.fillRect(0, 0, width, height);
            g.setColor(BGColor);
            g.fillRoundRect(0, 0, width - 1, height - 1, 20, 20);
            g.setColor(BorderColor);
            g.drawRoundRect(0, 0, width - 1, height - 1, 20, 20);
            border = 5;
        } else
        if(borderB && !roundRectBorderB)
        {
            g.setColor(BorderColor);
            g.drawRect(0, 0, width - 1, height - 1);
            border = 5;
        }
        g1.setColor(FGColor);
        eqScan.start();
        boxc = eqn(0, height, true, g1, 1);
        displayStatus(" ");
        xpos = 0;
        if(halign.equals("center"))
            xpos = 1;
        else
        if(halign.equals("right"))
            xpos = 2;
        ypos = 0;
        if(valign.equals("middle"))
            ypos = 1;
        else
        if(valign.equals("bottom"))
            ypos = 2;
        localWidth = 1 + boxc.dx + 2 * border;
        localHeight = 1 + boxc.dy_pos + boxc.dy_neg + 2 * border;
        boolean flag = false;
        if(localWidth > width)
        {
            flag = true;
            xpos = 0;
        }
        if(localHeight > height)
        {
            flag = true;
            ypos = 1;
        }
        int i = border;
        int j = border;
        switch(xpos)
        {
        case 1: // '\001'
            i = (width - boxc.dx) / 2;
            break;

        case 2: // '\002'
            i = width - border - boxc.dx - 1;
            break;
        }
        switch(ypos)
        {
        case 1: // '\001'
            j = border - (localHeight - height) / 2;
            break;

        case 2: // '\002'
            j = height - border - boxc.dy_neg - boxc.dy_pos;
            break;
        }
        g.drawImage(image, i, j, i + boxc.dx, j + boxc.dy_pos + boxc.dy_neg + 1, 0, height - boxc.dy_pos, boxc.dx, height + boxc.dy_neg + 1, this);
        g1.dispose();
        if(flag)
            printStatus((new StringBuilder()).append("(width,height) given=(").append(width).append(",").append(height).append(") used=(").append(localWidth).append(",").append(localHeight).append(")").toString());
        imageOK = true;
        drawn = true;
        xOFF = i;
        yOFF = j + boxc.dy_pos;
        notify();
    }
// jm.evers
    public int[] getActualSize()
    {
        int ai[] = new int[2];
        ai[0] = localWidth;
        ai[1] = localHeight;
        return ai;
    }
// jm.evers
    public synchronized BufferedImage mkImage(Color color, Color color1, int i, int j ,boolean outline , int xp , int yp,boolean disk)
    {
        width = i;
        height = j;
        xpos = xp;
	ypos = yp;
        finalimage = new BufferedImage(width, height, 2);
        Graphics g = finalimage.getGraphics();
        BoxC boxc = new BoxC();
        BoxC boxc1 = new BoxC();
        g.setFont(f1);
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        border = 0;
        if(borderB && roundRectBorderB)
        {
            g.setColor(EnvColor);
            g.fillRect(0, 0, width, height);
            g.setColor(color1);
            g.fillRoundRect(0, 0, width - 1, height - 1, 20, 20);
            g.setColor(BorderColor);
            g.drawRoundRect(0, 0, width - 1, height - 1, 20, 20);
            border = 5;
        } else
        if(borderB && !roundRectBorderB)
        {
            g.setColor(BorderColor);
            g.drawRect(0, 0, width - 1, height - 1);
            border = 5;
        }
        g.setColor(color1);
        eqScan.start();
        boxc1 = eqn(0, 150, false, g, 1);
        displayStatus(" ");
        localWidth = 1 + boxc1.dx + 2 * border;
        localHeight = 1 + boxc1.dy_pos + boxc1.dy_neg + 2 * border;
        boolean flag = false;
        if(localWidth > width)
        {
            flag = true;
            xpos = 0;
        }
        if(localHeight > height)
        {
            flag = true;
            ypos = 1;
        }
        int k = border;
        int l = boxc1.dy_pos + border;
        switch(xpos)
        {
        case 1: // '\001'
            k = (width - boxc1.dx) / 2;
            break;

        case 2: // '\002'
            k = width - border - boxc1.dx - 1;
            break;
        }
        switch(ypos)
        {
        case 1: // '\001'
            l = (border + boxc1.dy_pos) - (localHeight - height) / 2;
            break;

        case 2: // '\002'
            l = height - border - boxc1.dy_neg - 1;
            break;
        }
        eqScan.start();
        boxc = eqn(k, l, true, g, 1);
        xOFF = k;
        yOFF = l;
        g.dispose();
	if(localWidth > width ){ width = localWidth;}
	if(localHeight > height ){height = localHeight;}
	
	// ok now paint the image as big as it should be
        finalimage = new BufferedImage(width,height, 2);
        g = finalimage.getGraphics();
        boxc = new BoxC();
        boxc1 = new BoxC();
        g.setFont(f1);
        g.setColor(color);
	if(disk){
    	    g.fillOval(0, 0, width, height);
	}
	else
	{
    	    g.fillRect(0, 0, width, height);
        }
	g.setColor(color1);
	if(outline){g.drawRect(border,border,width - border,height - border);}
        eqScan.start();
        boxc = eqn(k, l, true, g, 1);
        imageOK = true;
        drawn = true;
        xOFF = k;
        yOFF = l;
        notify();
        g.dispose();
        return finalimage;
    }

    private BoxC eqn(int i, int j, boolean flag, Graphics g, int k)
    {
        return eqn(i, j, flag, g, k, true);
    }

    private BoxC eqn(int i, int j, boolean flag, Graphics g, int k, boolean flag1)
    {
        BoxC boxc = new BoxC();
        BoxC boxc1 = new BoxC();
        boolean flag2 = true;
        boolean flag3 = false;
        boolean flag5 = false;
        int l = 0;
        boolean flag6 = false;
        do
        {
            if(eqScan.EoT() || !flag2)
                break;
            eqTok = eqScan.nextToken();
            if(editMode && flag)
                l = eqScan.get_count();
            boolean flag4 = false;
            int i1 = l;
            int j1 = eqTok.typ;
            switch(eqTok.typ)
            {
            case 4: // '\004'
            case 7: // '\007'
            case 8: // '\b'
            case 17: // '\021'
            case 51: // '3'
                if(editModeFind && flag)
                {
                    if(l > editModeCount2)
                        editModeCount2 = l;
                    if(l < editModeCount1)
                        editModeCount1 = l;
                }
                return boxc1;

            case 22: // '\026'
                boxc = ACCENT(i + boxc1.dx, j, flag, g, k);
                break;

            case 5: // '\005'
                boxc = ANGLE(i + boxc1.dx, j, flag, g);
                break;

            case 15: // '\017'
                if(editModeFind && flag)
                    flag5 = true;
                boxc = ARRAY(i + boxc1.dx, j, flag, g, k);
                break;

            case 50: // '2'
                if(editModeFind && flag)
                    flag5 = true;
                boxc = BEGIN(i + boxc1.dx, j, flag, g, k);
                break;

            case 3: // '\003'
                boxc = eqn(i + boxc1.dx, j, flag, g, k, true);
                break;

            case 121: // 'y'
                boxc = FG_BGColor(i + boxc1.dx, j, flag, g, k, true);
                break;

            case 122: // 'z'
                boxc = FG_BGColor(i + boxc1.dx, j, flag, g, k, false);
                break;

            case 123: // '{'
                if(editModeFind && flag)
                    flag5 = true;
                boxc = FBOX(i + boxc1.dx, j, flag, g, k);
                break;

            case 1: // '\001'
                boxc = Id(i + boxc1.dx, j, flag, g);
                break;

            case 110: // 'n'
                boxc = NOT(i + boxc1.dx, j, flag, g, k);
                break;

            case 108: // 'l'
                boxc = Op(i + boxc1.dx, j, flag, g);
                break;

            case 12: // '\f'
                boxc = FRAC(i + boxc1.dx, j, flag, g, k, true);
                break;

            case 115: // 's'
                boxc = FRAC(i + boxc1.dx, j, flag, g, k, false);
                break;

            case 2: // '\002'
            case 9: // '\t'
                boxc = Plain(i + boxc1.dx, j, flag, g);
                break;

            case 18: // '\022'
                boxc = SYMBOP(i + boxc1.dx, j, flag, g, k, false);
                break;

            case 19: // '\023'
                boxc = SYMBOP(i + boxc1.dx, j, flag, g, k, true);
                break;

            case 16: // '\020'
                if(editModeFind && flag)
                    flag5 = true;
                boxc = LEFT(i + boxc1.dx, j, flag, g, k);
                break;

            case 24: // '\030'
                boxc = LIM(i + boxc1.dx, j, flag, g, k);
                break;

            case 124: // '|'
                boxc = MBOX(i + boxc1.dx, j, flag, g);
                break;

            case 118: // 'v'
                boxc = OverBRACE(i + boxc1.dx, j, flag, g, k);
                break;

            case 119: // 'w'
                boxc = UnderBRACE(i + boxc1.dx, j, flag, g, k);
                break;

            case 116: // 't'
                boxc = OverUnderLINE(i + boxc1.dx, j, flag, g, k, true);
                break;

            case 117: // 'u'
                boxc = OverUnderLINE(i + boxc1.dx, j, flag, g, k, false);
                break;

            case 109: // 'm'
                boxc = Paren(i + boxc1.dx, j, flag, g);
                break;

            case 113: // 'q'
                boxc = SPACE(i + boxc1.dx, j, flag, g);
                break;

            case 13: // '\r'
                if(editModeFind && flag)
                    flag5 = true;
                boxc = SQRT(i + boxc1.dx, j, flag, g, k);
                break;

            case 120: // 'x'
                boxc = STACKREL(i + boxc1.dx, j, flag, g, k);
                break;

            case 10: // '\n'
                boxc = SUP(i + boxc1.dx, j, flag, g, k, true);
                break;

            case 11: // '\013'
                boxc = SUB(i + boxc1.dx, j, flag, g, k, true);
                break;

            case 20: // '\024'
                boxc = SYMBOLBIG(i + boxc1.dx, j, flag, g, k);
                break;

            case 14: // '\016'
                boxc = VEC(i + boxc1.dx, j, flag, g, k);
                break;

            case 25: // '\031'
                boxc = new BoxC(0, 0, 0);
                flag4 = true;
                break;

            case 99: // 'c'
            case 100: // 'd'
                boxc = new BoxC(0, 0, 0);
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
                printStatus((new StringBuilder()).append("Parser: unknown token: ").append(eqTok.typ).append(" ").append(eqTok.stringS).toString());
                break;
            }
            if(flag)
            {
                if(editMode)
                {
                    if(!editModeFind && i + boxc1.dx <= mouse1X && mouse1X <= i + boxc1.dx + boxc.dx && j - boxc.dy_pos <= mouse1Y && mouse1Y <= j + boxc.dy_neg)
                    {
                        x0 = x1 = mouse1X;
                        y0 = y1 = mouse1Y;
                        editModeFind = true;
                        editModeCount1 = l;
                        editModeCount2 = l;
                    }
                    if(!editModeFind && i + boxc1.dx <= mouse2X && mouse2X <= i + boxc1.dx + boxc.dx && j - boxc.dy_pos <= mouse2Y && mouse2Y <= j + boxc.dy_neg)
                    {
                        x0 = x1 = mouse2X;
                        y0 = y1 = mouse2Y;
                        editModeFind = true;
                        editModeCount1 = l;
                        editModeCount2 = l;
                        int k1 = mouse2X;
                        int l1 = mouse2Y;
                        mouse2X = mouse1X;
                        mouse2Y = mouse1Y;
                        mouse1X = k1;
                        mouse1Y = l1;
                    }
                    if(editModeFind)
                    {
                        x0 = Math.min(x0, i + boxc1.dx);
                        x1 = Math.max(x1, i + boxc1.dx + boxc.dx);
                        y0 = Math.min(y0, j - boxc.dy_pos);
                        y1 = Math.max(y1, j + boxc.dy_neg);
                        if(editModeRec > k)
                            editModeRec = k;
                        switch(j1)
                        {
                        case 13: // '\r'
                        case 15: // '\017'
                        case 16: // '\020'
                        case 50: // '2'
                        case 123: // '{'
                        case 124: // '|'
                            flag5 = true;
                            if(i1 > editModeCount2)
                                editModeCount2 = i1;
                            if(i1 < editModeCount1)
                                editModeCount1 = i1;
                            l = eqScan.get_count();
                            break;
                        }
                        if(l > editModeCount2)
                            editModeCount2 = l;
                        if(l < editModeCount1)
                            editModeCount1 = l;
                        if(i + boxc1.dx <= mouse2X && mouse2X <= i + boxc1.dx + boxc.dx && j - boxc.dy_pos <= mouse2Y && mouse2Y <= j + boxc.dy_neg && editModeRec == k)
                        {
                            editMode = false;
                            editModeFind = false;
                        }
                    }
                }
                if(flag5)
                {
                    x0 = Math.min(x0, i + boxc1.dx);
                    x1 = Math.max(x1, i + boxc1.dx + boxc.dx);
                    y0 = Math.min(y0, j - boxc.dy_pos);
                    y1 = Math.max(y1, j + boxc.dy_neg);
                    switch(j1)
                    {
                    case 13: // '\r'
                    case 15: // '\017'
                    case 16: // '\020'
                    case 50: // '2'
                    case 123: // '{'
                    case 124: // '|'
                        if(i1 > editModeCount2)
                            editModeCount2 = i1;
                        if(i1 < editModeCount1)
                            editModeCount1 = i1;
                        l = eqScan.get_count();
                        break;
                    }
                    if(l > editModeCount2)
                        editModeCount2 = l;
                    if(l < editModeCount1)
                        editModeCount1 = l;
                    flag5 = false;
                }
            }
            boxc1.dx += boxc.dx;
            boxc1.dy_pos = Math.max(boxc1.dy_pos, boxc.dy_pos);
            boxc1.dy_neg = Math.max(boxc1.dy_neg, boxc.dy_neg);
            if(!flag1 && !flag4)
                flag2 = false;
        } while(true);
        return boxc1;
    }

    private BoxC ACCENT(int i, int j, boolean flag, Graphics g, int k)
    {
        BoxC boxc = new BoxC();
        int l = 0;
        FontMetrics fontmetrics = g.getFontMetrics();
        String s = eqTok.stringS;
        if(flag)
            l = eqScan.get_count();
        boxc = eqn(i, j, false, g, k, false);
        int i1 = Math.max(boxc.dx, fontmetrics.stringWidth(s));
        int j1 = boxc.dy_pos + fontmetrics.getAscent() / 2;
        int k1 = boxc.dy_neg;
        if(flag)
        {
            eqScan.set_count(l);
            BoxC boxc1 = eqn(i, j, true, g, k, false);
            int l1 = 3 * ((i1 - fontmetrics.stringWidth(s)) / 4);
            if(s.equals(".") | s.equals(".."))
                g.drawString(s, i + l1, j - fontmetrics.getAscent());
            else
            if(s.equals("\uFFFD") | s.equals("`"))
                g.drawString(s, i + l1, j - fontmetrics.getAscent() / 3);
            else
                g.drawString(s, i + l1, j - (fontmetrics.getAscent() * 2) / 3);
        }
        return new BoxC(i1, j1, k1);
    }

    private BoxC ANGLE(int i, int j, boolean flag, Graphics g)
    {
        BoxC boxc = new BoxC();
        FontMetrics fontmetrics = g.getFontMetrics();
        int k = g.getFont().getSize() / 2;
        int l = fontmetrics.getHeight() - fontmetrics.getDescent();
        int i1 = fontmetrics.getDescent();
        if(flag)
        {
            int j1 = (j - l) + 1;
            int k1 = (j + i1) - 1;
            int l1 = (j1 + k1) / 2;
            if(eqTok.stringS.equals("<"))
            {
                g.drawLine(i + k, j1, i, l1);
                g.drawLine(i, l1, i + k, k1);
            } else
            {
                g.drawLine(i, j1, i + k, l1);
                g.drawLine(i + k, l1, i, k1);
            }
        }
        return new BoxC(k, l, i1);
    }

    private BoxC ARRAY(int i, int j, boolean flag, Graphics g, int k)
    {
        boolean flag1 = false;
        boolean flag2 = false;
        boolean flag3 = false;
        int l1 = 0;
        int ai[] = new int[100];
        int ai1[] = new int[100];
        int ai2[] = new int[100];
        BoxC boxc = new BoxC();
        int i2 = 0;
        FontMetrics fontmetrics = g.getFontMetrics();
        int j2 = g.getFont().getSize();
        if(flag)
            i2 = eqScan.get_count();
        if(!expect(3, "ARRAY: BeginSym"))
            return new BoxC(0, 0, 0);
        int k2 = 0;
        do
        {
            if(k2 >= 99)
                break;
            int i1 = 0;
            int k1 = 0;
            int l2 = 0;
            do
            {
                if(l2 >= 99)
                    break;
                BoxC boxc1 = eqn(i, j, false, g, k);
                i1 = Math.max(i1, boxc1.dy_pos);
                k1 = Math.max(k1, boxc1.dy_neg);
                ai[l2] = Math.max(ai[l2], boxc1.dx + j2);
                if(eqTok.typ == 8 || eqTok.typ == 4)
                    break;
                l2++;
            } while(true);
            ai1[k2] = Math.max(ai1[k2], i1);
            ai2[k2] = Math.max(ai2[k2], k1);
            l1 += i1 + k1;
            if(eqTok.typ == 4)
                break;
            k2++;
        } while(true);
        k2 = 0;
        for(int i3 = 0; i3 < 99; i3++)
            k2 += ai[i3];

        if(flag)
        {
            eqScan.set_count(i2);
            expect(3, "ARRAY: Begin");
            int j1 = 0;
            int j3 = 0;
            do
            {
                if(j3 >= 99)
                    break;
                int l = 0;
                if(j3 == 0)
                    j1 = ai1[j3];
                else
                    j1 += ai2[j3 - 1] + ai1[j3];
                int k3 = 0;
                do
                {
                    if(k3 >= 99)
                        break;
                    BoxC boxc2 = eqn(i + l, (j - l1 / 2 - fontmetrics.getDescent()) + j1, true, g, k);
                    l += ai[k3];
                    if(eqTok.typ == 8 || eqTok.typ == 4)
                        break;
                    k3++;
                } while(true);
                if(eqTok.typ == 4)
                    break;
                j3++;
            } while(true);
        }
        return new BoxC(k2 - j2, l1 / 2 + fontmetrics.getDescent(), l1 / 2 - fontmetrics.getDescent());
    }

    private BoxC BEGIN(int i, int j, boolean flag, Graphics g, int k)
    {
        int i1 = 0;
        int j2 = 0;
        int ai[] = new int[100];
        int ai1[] = new int[100];
        int ai2[] = new int[100];
        int ai3[] = new int[100];
        int ai4[] = new int[100];
        int k2 = 0;
        int l2 = 0;
        int i3 = 0;
        BoxC boxc = new BoxC();
        int j3 = 0;
        FontMetrics fontmetrics = g.getFontMetrics();
        int k4 = g.getFont().getSize();
        int l4 = 0;
        boolean flag1 = false;
        boolean flag3 = false;
        boolean flag4 = true;
        boolean flag5 = true;
        boolean flag6 = false;
        boolean flag7 = false;
        if(!expect(3))
            return new BoxC(0, 0, 0);
        if(eqScan.nextToken().stringS.equals("eqnarray"))
            flag5 = false;
        if(!expect(4, "BEGIN: EndSym"))
            return new BoxC(0, 0, 0);
        if(flag5)
        {
            j3 = eqScan.get_count();
            if(!expect(3))
            {
                flag4 = false;
                eqScan.set_count(j3);
            }
        }
        if(flag5 && flag4)
        {
            EqToken eqtoken = new EqToken();
            for(EqToken eqtoken1 = eqScan.nextToken(); eqtoken1.typ != 4; eqtoken1 = eqScan.nextToken())
            {
                StringBuffer stringbuffer = new StringBuffer(eqtoken1.stringS);
                for(int l6 = 0; l6 < stringbuffer.length(); l6++)
label0:
                    switch(stringbuffer.charAt(l6))
                    {
                    case 108: // 'l'
                        ai3[l4] = 1;
                        if(l4 < 99)
                            l4++;
                        break;

                    case 99: // 'c'
                        ai3[l4] = 2;
                        if(l4 < 99)
                            l4++;
                        break;

                    case 114: // 'r'
                        ai3[l4] = 3;
                        if(l4 < 99)
                            l4++;
                        break;

                    case 64: // '@'
                        ai3[l4] = 4;
                        ai4[l4] = eqScan.get_count();
                        BoxC boxc1 = eqn(i, j, false, g, k, false);
                        k2 += boxc1.dx;
                        l2 = Math.max(l2, boxc1.dy_pos);
                        i3 = Math.max(i3, boxc1.dy_neg);
                        if(l4 < 99)
                            l4++;
                        break;

                    case 42: // '*'
                        expect(3, "Begin *{");
                        int j5;
                        try
                        {
                            j5 = Integer.parseInt(eqScan.nextToken().stringS);
                        }
                        catch(NumberFormatException numberformatexception)
                        {
                            j5 = 0;
                        }
                        expect(4, 3, "Begin }{");
                        int j7 = eqScan.get_count();
                        int k7 = 0;
                        do
                        {
                            if(k7 >= j5)
                                break label0;
                            eqScan.set_count(j7);
                            for(eqtoken1 = eqScan.nextToken(); eqtoken1.typ != 4; eqtoken1 = eqScan.nextToken())
                            {
                                StringBuffer stringbuffer1 = new StringBuffer(eqtoken1.stringS);
                                for(int l7 = 0; l7 < stringbuffer1.length(); l7++)
                                    switch(stringbuffer1.charAt(l7))
                                    {
                                    case 108: // 'l'
                                        ai3[l4] = 1;
                                        if(l4 < 99)
                                            l4++;
                                        break;

                                    case 99: // 'c'
                                        ai3[l4] = 2;
                                        if(l4 < 99)
                                            l4++;
                                        break;

                                    case 114: // 'r'
                                        ai3[l4] = 3;
                                        if(l4 < 99)
                                            l4++;
                                        break;

                                    case 64: // '@'
                                        ai3[l4] = 4;
                                        ai4[l4] = eqScan.get_count();
                                        BoxC boxc2 = eqn(i, j, false, g, k, false);
                                        k2 += boxc2.dx;
                                        l2 = Math.max(l2, boxc2.dy_pos);
                                        i3 = Math.max(i3, boxc2.dy_neg);
                                        if(l4 < 99)
                                            l4++;
                                        break;

                                    default:
                                        printStatus("P: begin: illegal format 2");
                                        break;
                                    }

                            }

                            k7++;
                        } while(true);

                    default:
                        printStatus("P: begin: illegal format 1");
                        break;
                    }

            }

        }
        if(!flag5)
        {
            ai3[0] = 3;
            ai3[1] = 2;
            ai3[2] = 1;
            l4 = 3;
        }
        for(int k5 = 0; k5 < l4 - 1; k5++)
            if(ai3[k5] != 4 && ai3[k5 + 1] != 4)
                i1 += k4 / 2;

        if(flag)
            j3 = eqScan.get_count();
        int l5 = 0;
        do
        {
            if(l5 >= 99)
                break;
            int j1 = 0;
            int l1 = 0;
            int j6 = 0;
            do
            {
                if(j6 >= 99)
                    break;
                BoxC boxc3 = eqn(i, j, false, g, k);
                j1 = Math.max(j1, boxc3.dy_pos);
                l1 = Math.max(l1, boxc3.dy_neg);
                ai[j6] = Math.max(ai[j6], boxc3.dx);
                if(eqTok.typ == 8 || eqTok.typ == 51)
                    break;
                j6++;
            } while(true);
            j1 = Math.max(j1, l2);
            l1 = Math.max(l1, i3);
            ai1[l5] = j1;
            ai2[l5] = l1;
            j2 += j1 + l1;
            if(eqTok.typ == 51)
                break;
            l5++;
        } while(true);
        for(int i5 = 0; i5 < 99; i5++)
            i1 += ai[i5];

        i1 += (2 * k4) / 2;
        if(flag)
        {
            eqScan.set_count(j3);
            int k1 = 0;
            int i2 = j2 / 2 + fontmetrics.getDescent();
            int i6 = 0;
            do
            {
                if(i6 >= 99)
                    break;
                int l = k4 / 2;
                if(i6 == 0)
                    k1 = ai1[i6];
                else
                    k1 += ai2[i6 - 1] + ai1[i6];
                int k6 = 0;
                int i7 = 0;
                do
                {
                    if(i7 >= 99)
                        break;
                    for(; ai3[k6] == 4; k6++)
                    {
                        int k3 = eqScan.get_count();
                        eqScan.set_count(ai4[k6]);
                        BoxC boxc4 = eqn(i + l, (j - i2) + k1, true, g, k, false);
                        l += boxc4.dx;
                        eqScan.set_count(k3);
                    }

                    switch(ai3[k6])
                    {
                    case 0: // '\0'
                    case 1: // '\001'
                        BoxC boxc5 = eqn(i + l, (j - i2) + k1, true, g, k);
                        k6++;
                        break;

                    case 2: // '\002'
                        int l3 = eqScan.get_count();
                        BoxC boxc6 = eqn(i, j, false, g, k);
                        eqScan.set_count(l3);
                        boxc6 = eqn(i + l + (ai[i7] - boxc6.dx) / 2, (j - i2) + k1, true, g, k);
                        k6++;
                        break;

                    case 3: // '\003'
                        int i4 = eqScan.get_count();
                        BoxC boxc7 = eqn(i, j, false, g, k);
                        eqScan.set_count(i4);
                        boxc7 = eqn((i + l + ai[i7]) - boxc7.dx, (j - i2) + k1, true, g, k);
                        k6++;
                        break;
                    }
                    if(ai3[k6] != 4)
                        l += k4 / 2;
                    l += ai[i7];
                    boolean flag2 = false;
                    flag3 = false;
                    if(eqTok.typ == 8)
                        flag2 = true;
                    else
                    if(eqTok.typ == 51)
                    {
                        flag2 = true;
                        flag3 = true;
                    }
                    for(; ai3[k6] == 4; k6++)
                    {
                        int j4 = eqScan.get_count();
                        eqScan.set_count(ai4[k6]);
                        BoxC boxc8 = eqn(i + l, (j - i2) + k1, true, g, k, false);
                        l += boxc8.dx;
                        eqScan.set_count(j4);
                    }

                    if(flag2)
                        break;
                    i7++;
                } while(true);
                if(flag3)
                    break;
                i6++;
            } while(true);
        }
        if(!expect(3, "BEGIN 2: begin"))
            return new BoxC(0, 0, 0);
        eqScan.nextToken();
        if(!expect(4, "BEGIN 2: end"))
            return new BoxC(0, 0, 0);
        else
            return new BoxC(i1 + k2, j2 / 2 + fontmetrics.getDescent(), j2 / 2 - fontmetrics.getDescent());
    }

    private BoxC FBOX(int i, int j, boolean flag, Graphics g, int k)
    {
        BoxC boxc = new BoxC();
        int l = g.getFont().getSize() / 2;
        boxc = eqn(i + l, j, flag, g, k, false);
        if(flag)
            g.drawRect(i + l / 2, j - boxc.dy_pos - l / 2, boxc.dx + l, boxc.dy_pos + boxc.dy_neg + l);
        return new BoxC(boxc.dx + l + l, boxc.dy_pos + l, boxc.dy_neg + l);
    }

    private BoxC FG_BGColor(int i, int j, boolean flag, Graphics g, int k, boolean flag1)
    {
        BoxC boxc = new BoxC();
        int l = 0;
        Color color = Color.white;
        if(!expect(3, "Color: BeginSym"))
            return new BoxC(0, 0, 0);
        StringBuffer stringbuffer = new StringBuffer("");
        int i1 = 1;
        do
        {
            if(i1 >= 7)
                break;
            stringbuffer.append(eqScan.nextToken().stringS);
            if(stringbuffer.length() == 6)
                break;
            i1++;
        } while(true);
        try
        {
            color = new Color(Integer.parseInt(stringbuffer.toString(), 16));
        }
        catch(NumberFormatException numberformatexception)
        {
            BGColor = Color.white;
        }
        if(!expect(4, "Color: EndSym"))
            return new BoxC(0, 0, 0);
        if(flag)
            l = eqScan.get_count();
        Color color1 = g.getColor();
        if(flag1)
            g.setColor(color);
        boxc = eqn(i, j, false, g, k, false);
        g.setColor(color1);
        if(flag)
        {
            eqScan.set_count(l);
            g.setColor(color);
            if(!flag1)
            {
                g.fillRect(i, j - boxc.dy_pos, boxc.dx, boxc.dy_pos + boxc.dy_neg);
                g.setColor(color1);
            }
            boxc = eqn(i, j, true, g, k, false);
            g.setColor(color1);
        }
        return boxc;
    }

    private BoxC FRAC(int i, int j, boolean flag, Graphics g, int k, boolean flag1)
    {
        int l = 0;
        BoxC boxc = new BoxC();
        BoxC boxc2 = new BoxC();
        BoxC boxc3 = new BoxC();
        int i1 = 0;
        Font font = g.getFont();
        int j1 = font.getSize();
        rec_Font(g, k + 1);
        FontMetrics fontmetrics = g.getFontMetrics();
        if(flag)
            i1 = eqScan.get_count();
        boxc2 = eqn(i, j, false, g, k + 1, false);
        int k1 = boxc2.dy_pos + boxc2.dy_neg;
        boxc3 = eqn(i, j, false, g, k + 1, false);
        int l1 = Math.max(boxc2.dx, boxc3.dx);
        int i2 = boxc3.dy_pos + boxc3.dy_neg;
        Font font1 = g.getFont();
        int j2 = (3 * font1.getSize()) / 18;
        l1 += 2 * j2;
        if(fontmetrics.getAscent() < i2)
            l = fontmetrics.getAscent() / 2;
        k1 += 2 + l;
        i2 += 1 - l;
        if(flag)
        {
            eqScan.set_count(i1);
            if(flag1)
                g.drawLine(i + j2, j - l, (i + l1) - j2, j - l);
            BoxC boxc1 = eqn(i + (l1 - boxc2.dx) / 2, j - 2 - boxc2.dy_neg - l, true, g, k + 1, false);
            if(editModeFind && k < editModeRec)
                editModeRec = k;
            boxc1 = eqn(i + (l1 - boxc3.dx) / 2, (j + 1 + boxc3.dy_pos) - l, true, g, k + 1, false);
        }
        rec_Font(g, k);
        return new BoxC(l1, k1, i2);
    }

    private BoxC Id(int i, int j, boolean flag, Graphics g)
    {
        Font font = g.getFont();
        g.setFont(new Font(Fontname, 2, font.getSize()));
        FontMetrics fontmetrics = g.getFontMetrics();
        if(flag)
            g.drawString(eqTok.stringS, i, j);
        int k = fontmetrics.stringWidth(eqTok.stringS);
        int l = fontmetrics.getHeight() - fontmetrics.getDescent();
        int i1 = fontmetrics.getDescent();
        g.setFont(font);
        return new BoxC(k, l, i1);
    }

    private void arc(Graphics g, int i, int j, int k, int l, int i1)
    {
        g.drawArc(i - k, j - k, 2 * k, 2 * k, l, i1);
    }

    private void drawBracket(Graphics g, String s, int i, int j, int k, int l, int i1, 
            int j1)
    {
        int k1 = j / 2;
        int l1 = i + k1;
        int i2 = i + j;
        int j2 = i + k1 / 2;
        int k2 = l1 + k1 / 2;
        int l2 = (k + l) / 2;
        int i3 = (int)((double)k1 * 0.86602540378444004D);
        int j3 = k + i3;
        int k3 = l - i3;
        if(s.equals("["))
        {
            g.drawLine(j2, k, j2, l);
            g.drawLine(j2, l, k2, l);
            g.drawLine(j2, k, k2, k);
        } else
        if(s.equals("]"))
        {
            g.drawLine(k2, k, k2, l);
            g.drawLine(j2, l, k2, l);
            g.drawLine(j2, k, k2, k);
        } else
        if(s.equals("|"))
            g.drawLine(l1, k, l1, l);
        else
        if(s.equals("||"))
        {
            int l3 = l1 + i1 / 4;
            g.drawLine(l1, k, l1, l);
            g.drawLine(l3, k, l3, l);
        } else
        if(s.equals("("))
        {
            for(int i4 = j1; i4 < 2 + j1; i4++)
            {
                int i5 = j2 + i4;
                arc(g, k2 + i4, j3, k1, 180, -60);
                g.drawLine(i5, j3, i5, k3);
                arc(g, k2 + i4, k3, k1, 180, 60);
            }

        } else
        if(s.equals(")"))
        {
            for(int j4 = j1; j4 < 2 + j1; j4++)
            {
                int j5 = k2 + j4;
                arc(g, j2 + j4, j3, k1, 0, 60);
                g.drawLine(j5, j3, j5, k3);
                arc(g, j2 + j4, k3, k1, 0, -60);
            }

        } else
        if(s.equals("<"))
        {
            g.drawLine(j2, l2, k2, k);
            g.drawLine(j2, l2, k2, l);
        } else
        if(s.equals(">"))
        {
            g.drawLine(k2, l2, j2, k);
            g.drawLine(k2, l2, j2, l);
        } else
        if(s.equals("{"))
        {
            for(int k4 = j1; k4 < 2 + j1; k4++)
            {
                int k5 = l1 + k4;
                arc(g, i2 + k4, j3, k1, 180, -60);
                g.drawLine(k5, j3, k5, l2 - k1);
                arc(g, i + k4, l2 - k1, k1, 0, -90);
                arc(g, i + k4, l2 + k1, k1, 0, 90);
                g.drawLine(k5, l2 + k1, k5, k3);
                arc(g, i2 + k4, k3, k1, 180, 60);
            }

        } else
        if(s.equals("}"))
        {
            for(int l4 = j1; l4 < 2 + j1; l4++)
            {
                int l5 = l1 + l4;
                arc(g, i + l4, j3, k1, 0, 60);
                g.drawLine(l5, j3, l5, l2 - k1);
                arc(g, i2 + l4, l2 - k1, k1, -180, 90);
                arc(g, i2 + l4, l2 + k1, k1, 180, -90);
                g.drawLine(l5, l2 + k1, l5, k3);
                arc(g, i + l4, k3, k1, 0, -60);
            }

        }
    }

    private BoxC LEFT(int i, int j, boolean flag, Graphics g, int k)
    {
        int l = 0;
        int i1 = 0;
        BoxC boxc = new BoxC();
        int j1 = 0;
        Font font = g.getFont();
        int k1 = font.getSize();
        int l1 = (int)(2.0F * (float)k1);
        int i2 = k1 / 9;
        if(flag)
            j1 = eqScan.get_count();
        String s = eqScan.nextToken().stringS;
        boxc = eqn(i, j, false, g, k);
        int j2 = boxc.dx;
        int k2 = boxc.dy_pos;
        int l2 = boxc.dy_neg;
        int i3 = (j - k2) + 1;
        int j3 = (j + l2) - 1;
        String s2 = eqScan.nextToken().stringS;
        int k3 = (k2 + l2) - 2;
//        Font font1 = new Font("Helvetica", 0, k3);
        Font font1 = new Font("Utopia", 0, k3);
        g.setFont(font1);
        FontMetrics fontmetrics = g.getFontMetrics();
        if(s.equals("<") || s.equals(">"))
            l = k1;
        else
        if(k3 < l1)
        {
            l = fontmetrics.stringWidth(s);
            if("([{)]}".indexOf(s) >= 0)
                l += i2;
        } else
        {
            l = k1;
        }
        if(s2.equals("<") || s2.equals(">"))
            i1 = k1;
        else
        if(k3 < l1)
        {
            i1 = fontmetrics.stringWidth(s2);
            if("([{)]}".indexOf(s2) >= 0)
                i1 += i2;
        } else
        {
            i1 = k1;
        }
        g.setFont(font);
        int l3 = eqScan.get_count();
        int j4 = 0;
        int k4 = 0;
        if(eqScan.nextToken().typ == 11)
        {
            BoxC boxc1 = SUB(i, j, false, g, k, false);
            j4 = boxc1.dx;
            k4 = (j3 + boxc1.dy_pos) - (boxc1.dy_pos + boxc1.dy_neg) / 2;
            l2 += (boxc1.dy_pos + boxc1.dy_neg) / 2;
        } else
        {
            eqScan.set_count(l3);
        }
        int l4 = eqScan.get_count();
        int j5 = 0;
        int k5 = 0;
        if(eqScan.nextToken().typ == 10)
        {
            BoxC boxc2 = SUP(i, j, false, g, k, false);
            j5 = boxc2.dx;
            k5 = (i3 + boxc2.dy_pos) - (boxc2.dy_pos + boxc2.dy_neg) / 2;
            k2 += (boxc2.dy_pos + boxc2.dy_neg) / 2;
        } else
        {
            eqScan.set_count(l4);
        }
        j4 = Math.max(j4, j5);
        if(flag)
        {
            eqScan.set_count(j1);
            String s1 = eqScan.nextToken().stringS;
            if(!s1.equals("."))
                if(k3 < l1 && !s1.equals("<") && !s1.equals(">"))
                {
                    g.setFont(font1);
                    g.drawString(s1, i, j3 - fontmetrics.getDescent() - fontmetrics.getLeading() / 2);
                    g.setFont(font);
                } else
                {
                    drawBracket(g, s1, i, l, i3, j3, k1, 0);
                }
            BoxC boxc3 = eqn(i + l, j, true, g, k);
            String s3 = eqScan.nextToken().stringS;
            if(!s3.equals("."))
                if(k3 < l1 && !s3.equals("<") && !s3.equals(">"))
                {
                    g.setFont(font1);
                    if("([{)]}".indexOf(s3) < 0)
                        i2 = 0;
                    g.drawString(s3, i + j2 + l + i2, j3 - fontmetrics.getDescent() - fontmetrics.getLeading() / 2);
                    g.setFont(font);
                } else
                {
                    drawBracket(g, s3, i + j2 + l, i1, i3, j3, -k1, -1);
                }
            int i4 = eqScan.get_count();
            if(expect(11))
                boxc3 = SUB(i + j2 + l + i1, k4, true, g, k, false);
            else
                eqScan.set_count(i4);
            int i5 = eqScan.get_count();
            if(expect(10))
                boxc3 = SUP(i + j2 + l + i1, k5, true, g, k, false);
            else
                eqScan.set_count(i5);
        }
        return new BoxC(j2 + l + i1 + j4, k2 + 2, l2 + 2);
    }

    private BoxC LIM(int i, int j, boolean flag, Graphics g, int k)
    {
        int l = 0;
        BoxC boxc = new BoxC();
        int i1 = 0;
        int j1 = 0;
        FontMetrics fontmetrics = g.getFontMetrics();
        String s = eqTok.stringS;
        int k1 = eqScan.get_count();
        int l1 = l = fontmetrics.stringWidth(s);
        int i2 = fontmetrics.getHeight() - fontmetrics.getDescent();
        int j2 = fontmetrics.getDescent();
        if(expect(11))
        {
            BoxC boxc1 = SUB(i, j, false, g, k, false);
            i1 = boxc1.dx;
            l = Math.max(l, boxc1.dx);
            j1 = boxc1.dy_pos;
            j2 = boxc1.dy_pos + boxc1.dy_neg;
        } else
        {
            eqScan.set_count(k1);
        }
        if(flag)
        {
            eqScan.set_count(k1);
            g.drawString(s, i + (l - l1) / 2, j);
            BoxC boxc2;
            if(expect(11))
                boxc2 = SUB(i + (l - i1) / 2, j + j1, true, g, k, false);
            else
                eqScan.set_count(k1);
        }
        return new BoxC(l, i2, j2);
    }

    private BoxC MBOX(int i, int j, boolean flag, Graphics g)
    {
        int k = 0;
        int l = 0;
        int i1 = 0;
        BoxC boxc = new BoxC();
        if(!expect(3))
            return new BoxC(0, 0, 0);
        do
        {
            if(eqScan.EoT())
                break;
            eqTok = eqScan.nextToken();
            if(eqTok.typ == 4)
                break;
            BoxC boxc1 = Plain(i + k, j, flag, g);
            k += boxc1.dx;
            l = Math.max(l, boxc1.dy_pos);
            i1 = Math.max(i1, boxc1.dy_neg);
        } while(true);
        return new BoxC(k, l, i1);
    }

    private BoxC NOT(int i, int j, boolean flag, Graphics g, int k)
    {
        BoxC boxc = new BoxC();
        boxc = eqn(i, j, flag, g, k, false);
        if(flag)
            g.drawLine(i + boxc.dx / 4, j + boxc.dy_neg, i + (boxc.dx * 3) / 4, j - boxc.dy_pos);
        return boxc;
    }

    private BoxC Op(int i, int j, boolean flag, Graphics g)
    {
        FontMetrics fontmetrics = g.getFontMetrics();
        if(flag)
            g.drawString(eqTok.stringS, i + 1, j);
        return new BoxC(fontmetrics.stringWidth(eqTok.stringS) + 2, fontmetrics.getHeight() - fontmetrics.getDescent(), fontmetrics.getDescent());
    }

    private BoxC OverBRACE(int i, int j, boolean flag, Graphics g, int k)
    {
        int l = 0;
        BoxC boxc = new BoxC();
        int i1 = g.getFont().getSize() / 4;
        int j1 = i1 / 2;
        int k1 = 0;
        int l1 = 0;
        int i2 = 0;
        if(flag)
            l = eqScan.get_count();
        boxc = eqn(i, j, false, g, k, false);
        int j2 = boxc.dx;
        int k2 = j2 / 2;
        int l2 = k2;
        int i3 = boxc.dy_pos;
        int j3 = boxc.dy_neg;
        int k3 = eqScan.get_count();
        if(expect(10))
        {
            BoxC boxc1 = SUP(i, j, false, g, k, false);
            k1 = boxc1.dx;
            l2 = Math.max(l2, k1 / 2);
            l1 = i3 + boxc1.dy_neg;
            i2 = boxc1.dy_pos + boxc1.dy_neg;
        } else
        {
            eqScan.set_count(k3);
        }
        if(flag)
        {
            eqScan.set_count(l);
            int i4 = (i + l2) - k2;
            BoxC boxc2 = eqn(i4, j, true, g, k, false);
            int j4 = (int)((double)i1 * 0.86602540378444004D);
            for(int k4 = 0; k4 < 2; k4++)
            {
                int l4 = (j - i3 - j1) + k4;
                arc(g, i4 + j4, l4 + i1, i1, 90, 60);
                g.drawLine(i4 + j4, l4, (i4 + k2) - i1, l4);
                arc(g, (i4 + k2) - i1, l4 - i1, i1, 0, -90);
                arc(g, i4 + k2 + i1, l4 - i1, i1, -90, -90);
                g.drawLine(i4 + k2 + i1, l4, (i4 + j2) - j4, l4);
                arc(g, (i4 + j2) - j4, l4 + i1, i1, 90, -60);
            }

            int l3 = eqScan.get_count();
            if(expect(10))
                boxc2 = SUP((i + l2) - k1 / 2, j - l1 - i1 - j1, true, g, k, false);
            else
                eqScan.set_count(l3);
        }
        i3 += i2 + i1 + j1;
        j2 = Math.max(j2, k1);
        return new BoxC(j2, i3, j3);
    }

    private BoxC UnderBRACE(int i, int j, boolean flag, Graphics g, int k)
    {
        int l = 0;
        BoxC boxc = new BoxC();
        int i1 = g.getFont().getSize() / 4;
        int j1 = i1 / 2;
        int k1 = 0;
        int l1 = 0;
        int i2 = 0;
        if(flag)
            l = eqScan.get_count();
        boxc = eqn(i, j, false, g, k, false);
        int j2 = boxc.dx;
        int k2 = j2 / 2;
        int l2 = k2;
        int i3 = boxc.dy_pos;
        int j3 = boxc.dy_neg;
        int k3 = eqScan.get_count();
        if(expect(11))
        {
            BoxC boxc1 = SUB(i, j, false, g, k, false);
            k1 = boxc1.dx;
            l2 = Math.max(l2, k1 / 2);
            l1 = j3 + boxc1.dy_pos;
            i2 = boxc1.dy_pos + boxc1.dy_neg;
        } else
        {
            eqScan.set_count(k3);
        }
        if(flag)
        {
            eqScan.set_count(l);
            int i4 = (i + l2) - k2;
            BoxC boxc2 = eqn(i4, j, true, g, k, false);
            int j4 = (int)((double)i1 * 0.86602540378444004D);
            for(int k4 = 0; k4 < 2; k4++)
            {
                int l4 = (j + j3 + j1) - k4;
                arc(g, i4 + j4, l4 - i1, i1, -90, -60);
                g.drawLine(i4 + j4, l4, (i4 + k2) - i1, l4);
                arc(g, (i4 + k2) - i1, l4 + i1, i1, 90, -90);
                arc(g, i4 + k2 + i1, l4 + i1, i1, 90, 90);
                g.drawLine(i4 + k2 + i1, l4, (i4 + j2) - j4, l4);
                arc(g, (i4 + j2) - j4, l4 - i1, i1, -90, 60);
            }

            int l3 = eqScan.get_count();
            if(eqScan.nextToken().typ == 11)
                boxc2 = SUB((i + l2) - k1 / 2, j + l1 + i1 + j1, true, g, k, false);
            else
                eqScan.set_count(l3);
        }
        j3 += i2 + i1 + j1;
        j2 = Math.max(j2, k1);
        return new BoxC(j2, i3, j3);
    }

    private BoxC OverUnderLINE(int i, int j, boolean flag, Graphics g, int k, boolean flag1)
    {
        int l = 0;
        BoxC boxc = new BoxC();
        if(flag)
            l = eqScan.get_count();
        boxc = eqn(i, j, false, g, k, false);
        if(flag1)
            boxc.dy_pos += 2;
        else
            boxc.dy_neg += 2;
        int i1 = boxc.dy_pos;
        int j1 = boxc.dy_neg;
        if(flag)
        {
            eqScan.set_count(l);
            if(flag1)
                g.drawLine(i + 1, (j - i1) + 2, (i + boxc.dx) - 1, (j - i1) + 2);
            else
                g.drawLine(i, (j + j1) - 2, i + boxc.dx, (j + j1) - 2);
            boxc = eqn(i, j, true, g, k, false);
        }
        return new BoxC(boxc.dx, i1, j1);
    }

    private BoxC Paren(int i, int j, boolean flag, Graphics g)
    {
        FontMetrics fontmetrics = g.getFontMetrics();
        int k = g.getFont().getSize() / 9;
        int l = fontmetrics.stringWidth(eqTok.stringS);
        int i1 = "([{)]}".indexOf(eqTok.stringS);
        if(i1 >= 0)
        {
            l += k;
            if(i1 > 2)
                i += k;
        }
        if(flag)
            g.drawString(eqTok.stringS, i, j);
        return new BoxC(l, fontmetrics.getHeight() - fontmetrics.getDescent(), fontmetrics.getDescent());
    }

    private BoxC Plain(int i, int j, boolean flag, Graphics g)
    {
        FontMetrics fontmetrics = g.getFontMetrics();
        if(flag)
            g.drawString(eqTok.stringS, i, j);
        return new BoxC(fontmetrics.stringWidth(eqTok.stringS), fontmetrics.getHeight() - fontmetrics.getDescent(), fontmetrics.getDescent());
    }

    private BoxC SPACE(int i, int j, boolean flag, Graphics g)
    {
        int k = 0;
        Font font = g.getFont();
        try
        {
            k = Integer.parseInt(eqTok.stringS);
        }
        catch(NumberFormatException numberformatexception)
        {
            k = 0;
        }
        k = (k * font.getSize()) / 18;
        return new BoxC(k, 0, 0);
    }

    private BoxC SQRT(int i, int j, boolean flag, Graphics g, int k)
    {
        BoxC boxc = new BoxC();
        int l = 0;
        FontMetrics fontmetrics = g.getFontMetrics();
        int i1 = 0;
        boolean flag1 = false;
        int k1 = 0;
        int l1 = 0;
        boolean flag2 = false;
        if(flag)
            l = eqScan.get_count();
        int i2 = fontmetrics.stringWidth("A");
        int j2 = i2 / 2;
        int k2 = eqScan.get_count();
        EqToken eqtoken = new EqToken();
        eqtoken = eqScan.nextToken();
        if(eqtoken.stringS.equals("["))
        {
            rec_Font(g, k + 1);
            boxc = eqn(i, j, false, g, k + 1, true);
            rec_Font(g, k);
            i1 = boxc.dx;
            int j1 = boxc.dy_pos;
            k1 = boxc.dy_neg;
            l1 = k1 + j1;
            flag2 = true;
        } else
        {
            eqScan.set_count(k2);
        }
        boxc = eqn(i, j, false, g, k, false);
        int l2 = boxc.dx + i2;
        int i3 = boxc.dy_pos + 2;
        int j3 = boxc.dy_neg;
        if(flag2 & (i1 > j2))
            l2 += i1 - j2;
        if(flag)
        {
            eqScan.set_count(l);
            int k3 = 0;
            if(flag2 & (i1 > j2))
                k3 = i1 - j2;
            g.drawLine(i + k3 + 1, j - i3 / 2, i + k3 + j2, (j + j3) - 1);
            g.drawLine(i + k3 + j2, (j + j3) - 1, (i + k3 + i2) - 2, (j - i3) + 2);
            g.drawLine((i + k3 + i2) - 2, (j - i3) + 2, i + l2, (j - i3) + 2);
            if(flag2)
            {
                EqToken eqtoken1 = eqScan.nextToken();
                rec_Font(g, k + 1);
                BoxC boxc2;
                if(i1 >= j2)
                {
                    g.drawLine(i + 1, j - i3 / 2, i + k3 + 1, j - i3 / 2);
                    BoxC boxc1 = eqn(i + 1, j - i3 / 2 - k1 - 1, true, g, k + 1, true);
                } else
                {
                    boxc2 = eqn(i + 1 + (j2 - i1), j - i3 / 2 - k1 - 1, true, g, k + 1, true);
                }
                rec_Font(g, k);
            }
            BoxC boxc3 = eqn(i + k3 + i2, j, true, g, k, false);
        }
        if(flag2 & (i3 / 2 < l1))
            i3 = i3 / 2 + l1;
        return new BoxC(l2, i3, j3);
    }

    private BoxC STACKREL(int i, int j, boolean flag, Graphics g, int k)
    {
        BoxC boxc = new BoxC();
        int l = 0;
        int i1 = g.getFontMetrics().getLeading();
        if(flag)
            l = eqScan.get_count();
        boxc = SUP(i, j, false, g, k, true);
        int j1 = boxc.dx;
        int k1 = boxc.dx;
        int l1 = (boxc.dy_pos + boxc.dy_neg) - i1;
        int i2 = boxc.dy_neg - i1;
        boxc = eqn(i, j, false, g, k, false);
        j1 = Math.max(j1, boxc.dx);
        int j2 = j1 / 2;
        int k2 = boxc.dx;
        l1 += boxc.dy_pos;
        int l2 = boxc.dy_neg;
        i2 += boxc.dy_pos;
        if(flag)
        {
            eqScan.set_count(l);
            BoxC boxc1 = SUP((i + j2) - k1 / 2, j - i2, true, g, k, false);
            boxc1 = eqn((i + j2) - k2 / 2, j, true, g, k, false);
        }
        return new BoxC(j1, l1, l2);
    }

    private BoxC SUB(int i, int j, boolean flag, Graphics g, int k, boolean flag1)
    {
        int l = 0;
        int i1 = 0;
        BoxC boxc = new BoxC();
        int j1 = 0;
        int l1 = g.getFontMetrics().getAscent() / 2;
        if(flag)
            j1 = eqScan.get_count();
        rec_Font(g, k + 1);
        boxc = eqn(i, j, false, g, k + 1, false);
        int i2 = boxc.dx;
        if(flag1)
        {
            l = l1 - 1;
            i1 = (boxc.dy_pos + boxc.dy_neg) - l;
        } else
        {
            i1 = boxc.dy_pos + boxc.dy_neg;
        }
        if(flag)
        {
            eqScan.set_count(j1);
            if(flag1)
                boxc = eqn(i, (j + boxc.dy_pos) - l, true, g, k + 1, false);
            else
                boxc = eqn(i, j + boxc.dy_pos, true, g, k + 1, false);
        }
        rec_Font(g, k);
        if(flag1)
        {
            int k1 = eqScan.get_count();
            if(expect(10))
            {
                BoxC boxc1 = SUP(i, j, flag, g, k, true);
                i2 = Math.max(i2, boxc1.dx);
                l = Math.max(l, boxc1.dy_pos);
            } else
            {
                eqScan.set_count(k1);
            }
        }
        return new BoxC(i2, l, i1);
    }

    private BoxC SUP(int i, int j, boolean flag, Graphics g, int k, boolean flag1)
    {
        int l = 0;
        int i1 = 0;
        BoxC boxc = new BoxC();
        int j1 = 0;
        int l1 = g.getFontMetrics().getAscent() / 2;
        if(flag)
            j1 = eqScan.get_count();
        rec_Font(g, k + 1);
        boxc = eqn(i, j, false, g, k + 1, false);
        int i2 = boxc.dx;
        if(flag1)
        {
            i1 = -l1 - 1;
            l = (boxc.dy_pos + boxc.dy_neg) - i1;
        } else
        {
            l = boxc.dy_pos + boxc.dy_neg;
        }
        if(flag)
        {
            eqScan.set_count(j1);
            if(flag1)
                boxc = eqn(i, (j - boxc.dy_neg) + i1, true, g, k + 1, false);
            else
                boxc = eqn(i, j - boxc.dy_neg, true, g, k + 1, false);
        }
        rec_Font(g, k);
        if(flag1)
        {
            int k1 = eqScan.get_count();
            if(expect(11))
            {
                BoxC boxc1 = SUB(i, j, flag, g, k, true);
                i2 = Math.max(i2, boxc1.dx);
                i1 = Math.max(i1, boxc1.dy_neg);
            } else
            {
                eqScan.set_count(k1);
            }
        }
        return new BoxC(i2, l, i1);
    }

    private Image getSymbol(Graphics g, int i)
    {
        String s = (new StringBuilder()).append(eqTok.stringS).append(GreekSize[i - 1]).append(g.getColor().getRGB()).toString();
        if(!imageH.containsKey(s))
        {
            String s1 = (new StringBuilder()).append("Fonts/Greek").append(GreekSize[i - 1]).append("/").append(eqTok.stringS).append(".gif").toString();
            Image image = symbolLoader.getImage(appletB, beanB, s1, g, app);
            int j = eqScan.get_count();
            tracker.addImage(image, j);
            displayStatus((new StringBuilder()).append("Loading ").append(eqTok.stringS).toString());
            try
            {
                tracker.waitForID(j, 10000L);
            }
            catch(InterruptedException interruptedexception) { }
            if(tracker.isErrorID(j))
                displayStatus((new StringBuilder()).append("Error loading ").append(eqTok.stringS).toString());
            else
                imageH.put(s, image);
            return image;
        } else
        {
            return (Image)(Image)imageH.get(s);
        }
    }

    private BoxC SYMBOP(int i, int j, boolean flag, Graphics g, int k, boolean flag1)
    {
        FontMetrics fontmetrics = g.getFontMetrics();
        k = Math.min(k, GreekSize.length);
        Image image = getSymbol(g, k);
        int l = image.getWidth(this);
        if(l < 0)
            l = fontmetrics.getMaxAdvance();
        if(flag)
        {
            int i1 = 0;
            if(flag1)
                i1 = GreekDescent[k - 1];
            g.drawImage(image, i, (j - image.getHeight(this)) + i1, this);
        }
        return new BoxC(l, fontmetrics.getHeight() - fontmetrics.getDescent(), fontmetrics.getDescent());
    }

    private BoxC SYMBOLBIG(int i, int j, boolean flag, Graphics g, int k)
    {
        int l = 0;
        BoxC boxc = new BoxC();
        int i1 = 0;
        int j1 = 0;
        int k1 = 0;
        int l1 = 0;
        int i2 = 0;
        int j2 = g.getFontMetrics().getAscent();
        k = Math.min(k, GreekSize.length);
        Image image = getSymbol(g, k);
        int k2 = l = image.getWidth(this);
        int l2 = image.getHeight(this);
        if(l2 < 0)
        {
            l2 = 2 * j2;
            k2 = l = j2;
        }
        int i3 = (int)((double)(l2 / 2) - 0.40000000000000002D * (double)j2);
        int j3 = i2 = l2 - i3;
        int k3 = eqScan.get_count();
        if(expect(11))
        {
            BoxC boxc1 = SUB(i, j, false, g, k, false);
            k1 = boxc1.dx;
            l = Math.max(l, boxc1.dx);
            i1 = i3 + boxc1.dy_pos;
            i3 += boxc1.dy_pos + boxc1.dy_neg;
        } else
        {
            eqScan.set_count(k3);
        }
        int l3 = eqScan.get_count();
        if(expect(10))
        {
            BoxC boxc2 = SUP(i, j, false, g, k, false);
            l1 = boxc2.dx;
            l = Math.max(l, boxc2.dx);
            j1 = j3 + boxc2.dy_neg;
            j3 += boxc2.dy_pos + boxc2.dy_neg;
        } else
        {
            eqScan.set_count(l3);
        }
        if(flag)
        {
            eqScan.set_count(k3);
            g.drawImage(image, i + (l - k2) / 2, j - i2, this);
            BoxC boxc3;
            if(expect(11))
                boxc3 = SUB(i + (l - k1) / 2, j + i1, true, g, k, false);
            else
                eqScan.set_count(k3);
            int i4 = eqScan.get_count();
            if(expect(10))
                boxc3 = SUP(i + (l - l1) / 2, j - j1, true, g, k, false);
            else
                eqScan.set_count(i4);
        }
        return new BoxC(l, j3, i3);
    }

    private BoxC VEC(int i, int j, boolean flag, Graphics g, int k)
    {
        BoxC boxc = new BoxC();
        int l = g.getFont().getSize();
        String s = eqTok.stringS;
        boxc = eqn(i, j, flag, g, k, false);
        int i1 = boxc.dx;
        int j1 = i1 / 2;
        int k1 = l / 4;
        int l1 = boxc.dy_pos + k1;
        int i2 = boxc.dy_neg;
        if(flag)
        {
            int j2 = (j - l1) + k1;
            int k2 = l / 8;
            int l2 = i + i1;
            int i3 = i + j1;
            if(s.equals(""))
            {
                g.drawLine(i, j2, l2, j2);
                g.drawLine(i + (int)((double)i1 * 0.80000000000000004D), j2 - k2, l2, j2);
                g.drawLine(i + (int)((double)i1 * 0.80000000000000004D), j2 + k2, l2, j2);
            } else
            if(s.equals("bar"))
                g.drawLine(i, j2, l2, j2);
            else
            if(s.equals("widehat"))
            {
                g.drawLine(i, j2, i3, j2 - k1);
                g.drawLine(i3, j2 - k1, l2, j2);
            } else
            if(s.equals("widetilde"))
            {
                boolean flag1 = false;
                int k3 = 0;
                for(int l3 = 1; l3 < j1; l3++)
                {
                    int j3 = k3;
                    k3 = (int)((double)k2 * Math.sin((4.0840704496667311D * (double)l3) / (double)j1));
                    g.drawLine((i3 + l3) - 1, j2 + j3, i3 + l3, j2 + k3);
                    g.drawLine((i3 - l3) + 1, j2 - j3, i3 - l3, j2 - k3);
                }

            }
        }
        return new BoxC(i1, l1 + 2, i2);
    }

    private boolean expect(int i)
    {
        return expect(i, "");
    }

    private boolean expect(int i, String s)
    {
        int j;
        while((j = eqScan.nextToken().typ) == 25) ;
        if(j == i)
            return true;
        if(!s.equals(""))
            printStatus((new StringBuilder()).append("Parser: ").append(s).append(" not found").toString());
        return false;
    }

    private boolean expect(int i, int j)
    {
        return expect(i, j, "");
    }

    private boolean expect(int i, int j, String s)
    {
        int k;
        while((k = eqScan.nextToken().typ) == 25) ;
        boolean flag = k == i;
        while((k = eqScan.nextToken().typ) == 25) ;
        flag = k == j;
        if(!flag && !s.equals(""))
            printStatus((new StringBuilder()).append("Parser: ").append(s).append(" not found").toString());
        return flag;
    }

    private void rec_Font(Graphics g, int i)
    {
        if(i <= 1)
            g.setFont(f1);
        else
        if(i == 2)
            g.setFont(f2);
        else
        if(i == 3)
            g.setFont(f3);
        else
            g.setFont(f4);
    }

    private static final String VERSION = "sHotEqn V 4.00 ";
    private int width;
    private int height;
    private String nameS;
    private String equation;
    private String Fontname;
    ActionListener actionListener;
    private EqScanner eqScan;
    private EqToken eqTok;
    private Font f1;
    private Font f2;
    private Font f3;
    private Font f4;
    private static final float mk = 2F;
    private static final int GreekFontSizes[] = {
        8, 10, 12, 14, 18
    };
    private static final int GreekFontDescents[] = {
        2, 3, 4, 5, 6
    };
    private int GreekSize[] = {
        14, 12, 10, 8
    };
    private int GreekDescent[] = {
        3, 3, 3, 3
    };
    private static final int EmbedFontSizes[] = {
        9, 11, 14, 16, 22
    };
    private Image bufferImage;
    private boolean imageOK;
    private int localWidth;
    private int localHeight;
    private Color BGColor;
    private Color EnvColor;
    private Color FGColor;
    private Color BorderColor;
    private boolean borderB;
    private boolean roundRectBorderB;
    private int border;
    private String halign;
    private String valign;
    private int xpos;
    private int ypos;
    private boolean drawn;
    private SymbolLoader symbolLoader;
    private MediaTracker tracker;
    private Hashtable imageH;
    private JApplet app;
    public boolean appletB;
    public boolean beanB;
    public boolean debug;
    private boolean editMode;
    private boolean editableB;
    private int mouse1X;
    private int mouse1Y;
    private int mouse2X;
    private int mouse2Y;
    private int xOFF;
    private int yOFF;
    private int y0;
    private int x0;
    private int y1;
    private int x1;
    private int editModeRec;
    private boolean editModeFind;
    private int editModeCount1;
    private int editModeCount2;
    private Image selectImage;
    public BufferedImage finalimage;

}
