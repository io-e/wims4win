/*
$Id: SqrtTick.java,v 1.3 2003/02/18 11:48:47 sander Exp $
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
import fr.ove.openmath.jome.ctrlview.bidim.DisplayableImpl;
import fr.ove.openmath.jome.ctrlview.bidim.Line;


/**
* The "tick" of the square root
*
* @author © 1999 DIRAT Laurent
* @version 2.0 22/07/1999
*/
public class SqrtTick extends DisplayableImpl {
    /**
    * The thickness of the tick
    */
    private int thickness;
    
    /**
    * The elements of the drawing of the tick
    */
    private Line line1;
    private Line line2;
    
    /**
    * Sets the thickness of the drawing.
    * @param thickness the thickness of the drawing.
    */
    public void setThickness(int thickness) {
        this.thickness = thickness;
    }
    
    /**
    * Returns the thickness of the drawing.
    */
    public int getThickness() {
        return thickness;
    }
    
    /**
    * The paint method of the object to display.
    * @param g the drawing area of the symbol.
    */
    public void paint(Graphics g) {
        int height = getHeight();
        int width = getWidth();
        
        g.fillRect(0, (int) Math.round(0.48f * (float) height), (int) Math.round(0.2f * (float) width), thickness);

        line1 = new Line(new Point((int) Math.round(0.1f * (float) width), (int) Math.round(0.48f * (float) height)),
                         new Point((int) Math.round(0.4f * (float) width), height),
                         thickness, Line.RIGHT);
        line1.draw(g);
        
        line2 = new Line(new Point((int) Math.round(0.4f * (float) width), height),
                         new Point(width - (int) Math.round(0.2f * (float) width), 0),
                         thickness, Line.CENTER);
        line2.draw(g);
        
        g.drawLine((int) Math.round(0.4f * (float) width), height, width - (int) Math.round(0.2f * (float) width), 0);
        
        g.fillRect(width - (int) Math.round(0.2f * (float) width), 0, (int) Math.round(0.2f * (float) width), thickness);
    }
}

