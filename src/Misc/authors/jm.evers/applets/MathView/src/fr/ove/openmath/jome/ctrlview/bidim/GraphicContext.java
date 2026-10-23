/*
$Id: GraphicContext.java,v 1.3 2003/02/18 11:48:46 sander Exp $
*/


/*
Copyright (C) 2001-2002 Mainline Project (I3S - ESSI - CNRS -UNSA)

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

For further information on the GNU Lesser General Public License,
see: http://www.gnu.org/copyleft/lesser.html
For further information on this library, contact: mainline@essi.fr
*/


package fr.ove.openmath.jome.ctrlview.bidim;

import java.awt.*;

/**
* The graphic context of each part of the formula to display.
*
* @author © 1998 DIRAT Laurent
* @version 1.0  22/06/98
*/
public class GraphicContext implements java.io.Serializable {
    /**
    * The font used.
    */
    private Font font;
    
    /**
    * The size of the font used.
    */
    private int sizeRef; // Serves as the reference font size for font scales.
    
    /**
    * The color used to display the part of the formula.
    */
    private Color foregroundColor;
    
    /**
    * The color used to display the background of the formula.
    */
    private Color backgroundColor;
    
    /**
    * The color used to hilight the selected part.
    */
    private static Color selectionColor;
    
    /**
    * How many times the scale factor is apply to reduce the font size.
    */
    private int nbTime = 0;
    
    /**
    * The default constructor.<BR>
    * The default is :
    * <UL>
    * <LI> a Times New Roman font, regular, with a size of 14 </LI>
    * <LI> the foreground color set to black </LI>
    * <LI> the background color set to white </LI>
    * <LI> the selection color set to yellow </LI>
    * </UL>
    */
    public GraphicContext() {
        this(new Font("Times New Roman", Font.PLAIN, 14), Color.black, Color.white, Color.yellow);
    }
    
    /**
    * The Constructor.
    *
    * @param font the font used to display all the text elements.
    * @param color the color used to display all the text elements.
    * @param background the color used as background for the display.
    * @param selectionColor the color of all the selected elements.
    */
    public GraphicContext(Font font, Color foregroundColor, Color backgroundColor, Color selectionColor) {
        this.font = font;
        sizeRef = font.getSize();
        this.foregroundColor = foregroundColor;
        this.backgroundColor = backgroundColor;
        this.selectionColor = selectionColor;
    }
    
    /**
    * The constructor by copy.
    * @param gc the graphic context to copy.
    */
    public GraphicContext(GraphicContext gc) {
        font = gc.getFont();
        sizeRef = gc.getSizeRef();
        foregroundColor = gc.getForegroundColor();
        backgroundColor = gc.getBackgroundColor();
        selectionColor = gc.getSelectionColor();
    }
    
    // ### The getters ###
    
    /**
    * Returns the font used in the graphic context.
    */
    public Font getFont() {
        return font;
    }
    
    /**
    * Returns the reference size of the font.
    */
    public int getSizeRef() {
        return sizeRef;
    }
    
    /**
    * Returns the color used by the graphic context.
    */
    public Color getForegroundColor() {
        return foregroundColor;
    }
    
    /**
    * Returns the background for the display.
    */
    public Color getBackgroundColor() {
        return backgroundColor;
    }
    
    /**
    * Returns the color used in the graphic context to hilight the selection.
    * @return the color.
    */
    public Color getSelectionColor() {
        return selectionColor;
    }
    
    // ### The setters ###
    
    /**
    * Sets a new font to the context.
    * @param font the new font.
    */
    public void setFont(Font font) {
        this.font = font;
        sizeRef = font.getSize();
    }
    
    /**
    * Sets a new color to the context.
    * @param color the new color.
    */
    public void setForegroundColor(Color foregroundColor) {
        this.foregroundColor = foregroundColor;
    }
    
    /**
    * Sets a new background color for the display.
    * @param background the color used as background for the display.
    */
    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
    
    /**
    * Sets a new selection color to the context.
    * @param selectionColor the new color.
    */
    public void setSelectionColor(Color selectionColor) {
        this.selectionColor = selectionColor;
    }
    
    /**
    * Scales the current font.
    * @param nbTime how many the scale factor is applied.
    * @return the font scaled.
    */
    public Font scaleFont(int nbTime) {
        if (this.nbTime != nbTime) {
            String name = font.getName();
            int style = font.getStyle();
            int size = sizeRef;
            
            this.nbTime = nbTime;
            
            for (int i = 0; i < nbTime; i++) {
                size = (int) Math.round( ((float) size) * 0.7f );
                if (size < 10)
                    break;
            }
            
            size = (size < 10) ? 10 : size;
            
            font = new Font(name, style, size);
        }
        
        return font;
    }
    
}
