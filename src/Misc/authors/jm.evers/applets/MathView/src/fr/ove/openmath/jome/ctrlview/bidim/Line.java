/*
$Id: Line.java,v 1.3 2003/02/18 11:48:46 sander Exp $
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
import java.io.Serializable;

/**
* A line.
*
* @author © 1999 DIRAT Laurent
* @version 2.0  22/07/1999
*/
public class Line implements Serializable {
    /**
    * The starting point of the line.
    */
    private Point p1;
    
    /**
    * The ending point of the line.
    */
    private Point p2;
    
    /**
    * The thickness of the line.
    */
    private int thickness;
    
    /**
    * The alignment of the drawing of the line.
    * <UL>
    * <LI> LEFT : the thickness of the line is distributed to the left of the line</LI>
    * <LI> RIGHT : the thickness of the line is distributed to the right of the line</LI>
    * <LI> CENTER : the thickness of the line is distributed in equitably on both sides of the line </LI>
    * </UL>
    * Be carefull of the position of the points. The line goes from P1 to P2.
    */
    private byte alignment;
    
    private Polygon line;
    
    /**
    * The constructor.<BR>
    * Constructs the line [(0,0),(0,0)], with a thickness of 1.
    */
    public Line() {
        this(new Point(), new Point(), 1, CENTER);
    }
    
    /**
    * The constructor.<BR>
    * @param p1 the starting point of the line
    * @param p2 the ending point of the line
    * @param thickness the thickness of the line
    * @param alignment the alignment of the line
    */
    public Line(Point p1, Point p2, int thickness, byte alignment) {
        this.p1 = p1;
        this.p2 = p2;
        this.thickness = thickness;
        this.alignment = alignment;
        computeLine();
    }
    
    /**
    * Sets a new starting point.
    * @param p1 the starting point of the line
    */
    public void setP1(Point p1) {
        this.p1 = p1;
        computeLine();
    }
    
    /**
    * Returns the starting point of the line.
    */
    public Point getP1() {
        return p1;
    }
    
    /**
    * Sets a new ending point.
    * @param p2 the ending point of the line
    */
    public void setP2(Point p2) {
        this.p2 = p2;
        computeLine();
    }
    
    /**
    * Returns the ending point of the line.
    */
    public Point getP2() {
        return p2;
    }
    
    /**
    * Sets a new alingment.
    * @param alignment the alignment of the line
    */
    public void setAlignment(byte alignment) {
        this.alignment = alignment;
    }
    
    /**
    * Returns the alignement of the line.
    */
    public int getAlignment() {
        return alignment;
    }
    
    /**
    * Sets the thickness of the line.
    * @param thickness the thickness of the line
    */
    public void setThickness(int thickness) {
        this.thickness = thickness;
        computeLine();
    }
    
    /**
    * Returns the thickness of the line.
    */
    public int getThickness() {
        return thickness;
    }
    
    /**
    * Draws the line.
    * @param g the graphics in which the line has to be drawn.
    */
    public void draw(Graphics g) {
        g.fillPolygon(line);
    }
    
    /**
    * Returns the <CODE>Polygon</CODE> which represents the line.
    */
    public Polygon getLine() {
        return line;
    }
    
    private void computeLine() {
        line = new Polygon();
        Point vectDir = new Point(p2.x - p1.x, p2.y - p1.y);
        float ortho_x = -vectDir.y;
        float ortho_y = vectDir.x;
        // On normalise ortho
        float norme = (float) Math.sqrt(ortho_x*ortho_x + ortho_y*ortho_y);
        ortho_x = (ortho_x/norme)*((float) thickness);
        ortho_y = (ortho_y/norme)*((float) thickness);
        
        int x, y;
        
        switch(alignment) {
            case RIGHT :
                line.addPoint(p1.x, p1.y);
                line.addPoint(p2.x, p2.y);
                line.addPoint((int) Math.round(((int) p2.x) - ortho_x), (int) Math.round(((int) p2.y) - ortho_y));
                line.addPoint((int) Math.round(((int) p1.x) - ortho_x), (int) Math.round(((int) p1.y) - ortho_y));
                break;
            case CENTER :
                float half_x = ortho_x/2.0f;
                float half_y = ortho_y/2.0f;
                line.addPoint((int) Math.round(((int) p1.x) - half_x), (int) Math.round(((int) p1.y) - half_y));
                line.addPoint((int) Math.round(((int) p2.x) - half_x), (int) Math.round(((int) p2.y) - half_y));
                line.addPoint((int) Math.round(((int) p2.x) + half_x), (int) Math.round(((int) p2.y) + half_y));
                line.addPoint((int) Math.round(((int) p1.x) + half_x), (int) Math.round(((int) p1.y) + half_y));
                break;
            case LEFT :
                line.addPoint(p1.x, p1.y);
                line.addPoint(p2.x, p2.y);
                line.addPoint((int) Math.round(((int) p2.x) + ortho_x), (int) Math.round(((int) p2.y) + ortho_y));
                line.addPoint((int) Math.round(((int) p1.x) + ortho_x), (int) Math.round(((int) p1.y) + ortho_y));
                break;
        }
    }
    
    /**
    * The different alignements of the line.
    */
    
    /**
    * The thickness of the line is distributed to the left of the line
    */
    public static final byte LEFT      = 0;
    
    /**
    * The thickness of the line is distributed in equitably on both sides of the line
    */
    public static final byte CENTER    = 1;
    
    /**
    * The thickness of the line is distributed to the right of the line
    */
    public static final byte RIGHT     = 2;
}
