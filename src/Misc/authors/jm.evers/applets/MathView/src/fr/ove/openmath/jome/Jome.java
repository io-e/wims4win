/*
$Id: Jome.java,v 1.3 2003/02/18 11:48:46 sander Exp $
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


package fr.ove.openmath.jome;

import java.awt.*;
import java.util.Vector;
import java.io.*;

import fr.ove.openmath.jome.model.*;
import fr.ove.openmath.jome.model.events.*;
import fr.ove.openmath.jome.ctrlview.events.*;
import fr.ove.openmath.jome.ctrlview.bidim.*;
import fr.ove.openmath.jome.ctrlview.bidim.selection.events.*;
import fr.ove.openmath.jome.ctrlview.bidim.selection.*;
import fr.ove.openmath.jome.ctrlview.bidim.images.ImageLoader;
import fr.ove.openmath.jome.ctrl.linear.*;

public class Jome extends Container {
    /**
    * The formula.
    */
    private Formula formula;

    /**
    * fr.ove.openmath.jome display.
    */
    Display jomeDisplay;

    /**
    * The default display of the formula.
    */
    FormulaDisplay formulaDisplay;

    /**
    * The graphic context.
    */
    private GraphicContext graphicContext = new GraphicContext(
            new Font("Times New Roman", Font.PLAIN, 14),
            Color.black,
            Color.white,
            Color.yellow);

    LinearParser linearParser = new LinearParser();

    /*********************/
    /* Les constructeurs */
    /*********************/

    /**
    * The default constructor.
    */
    public Jome() {
        super();
        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        super.setFont(graphicContext.getFont());

        // CrÃ©ation de la formule (modÃ¨le)
        formula = new Formula();

        linearParser.addLinearParserListener(formula);

        // CrÃ©ation du gestionnaire de la sÃ©lection
        SelectionManager selectionManager = new SelectionManager();

        // CrÃ©ation du display par dÃ©faut de la formule (vue + contrÃ´le)
        formulaDisplay = new FormulaDisplay();
        formulaDisplay.setGraphicContext(graphicContext);
        formulaDisplay.addControlListener(formula);
        formulaDisplay.setShiftX(10);
        formulaDisplay.setShiftY(10);
        formula.addModelListener(formulaDisplay);

        // CrÃ©ation du display de jome.
        // C'est Ã©galement un display, qui va contenir le display (par dÃ©faut)
        // de la formule.
        jomeDisplay = new BidimDisplay(graphicContext);
        jomeDisplay.drawBounds();
        OneFormulaLayout layout = new OneFormulaLayout();
        layout.initDisplay(jomeDisplay);
        jomeDisplay.setLayout(layout);
        jomeDisplay.addSelectionEventListener(selectionManager);
        // On ajoute le display (par dÃ©faut) de la formule dans le display de fr.ove.openmath.jome
        jomeDisplay.add(formulaDisplay);

        // On ajoute de display de jome dans l'instance (i.e. en JOME)
        add(jomeDisplay);

        // On crÃ©Ã© la formule.
        setLinear("");
    }

    /**********************************************/
    /* MÃ©thodes nÃ©cessaires Ã  cause de l'hÃ©ritage */
    /**********************************************/

    /**
    * Returns the preferred size of the fr.ove.openmath.jome.
    */
    public Dimension getPreferredSize() {
        return jomeDisplay.getPreferredSize();
    }

    /**
    * The paint method.
    * @param g where the formula is painted.
    */
    public void paint(Graphics g) {
        Color oldColor = g.getColor();
        Color color = getBackground();
        if (color != null) {
            g.setColor(color);
            Dimension size = getSize();
            g.fillRect(0, 0, size.width, size.height);
            g.setColor(oldColor);
        }

        super.paint(g);
    }

    /*************************/
    /* Gestion de la formule */
    /*************************/

    /**
    * Creates a formula from the specified maple-like expression.
    * @param exp the specified maple-like expression.
    */
    public void setLinear(String exp) {
        linearParser.parse(exp);
        jomeDisplay.invalidate();
        jomeDisplay.setComputeAttributes(true);
        validate();
        repaint();
    }

    /**
    * Returns the linear expression of the formula.
    */
    public String getLinear() {
        return formula.createLinear("");
    }

    /***************************/
    /* Gestion de la sÃ©lection */
    /***************************/

    /**
    * Returns the linear expression of the selection.
    */
    public String getLinearSelection() {
        return formulaDisplay.getLinearSelection();
    }


    /*********************************************************************/
    /* DÃ©calage de l'affichage de la formule Ã  l'intÃ©rieur de la fenÃªtre */
    /*********************************************************************/

    /**
    * Sets a horizontal shift to the display of the formula.
    * @param shiftY the vertical shift.
    */
    public void setShiftX(int shiftX) {
        shiftX = (shiftX < 1) ? 1 : shiftX;
        formulaDisplay.setShiftX(shiftX);
        jomeDisplay.invalidate();
        jomeDisplay.setComputeAttributes(true);
        validate();
        repaint();
    }

    /**
    * Returns the horizontal shift.
    * @return the horizontal shift.
    */
    public int getShiftX() {
        return formulaDisplay.getShiftX();
    }

    /**
    * Sets a vertical shift to the display of the formula.
    * @param shiftY the vertical shift.
    */
    public void setShiftY(int shiftY) {
        shiftY = (shiftY < 1) ? 1 : shiftY;
        formulaDisplay.setShiftY(shiftY);
        jomeDisplay.invalidate();
        jomeDisplay.setComputeAttributes(true);
        validate();
        repaint();
    }

    /**
    * Returns the vertical shift.
    * @return the vertical shift.
    */
    public int getShiftY() {
        return formulaDisplay.getShiftY();
    }


    /**************************************/
    /* Manipulation du contexte graphique */
    /**************************************/

    /**
    * Sets a new font to the context.
    * @param font the new font.
    */
    public void setFont(Font font) {
        graphicContext.setFont(font);
        formulaDisplay.setFont(font);
        jomeDisplay.invalidate();
        jomeDisplay.setComputeAttributes(true);
        validate();

        super.setFont(font);
    }

    /**
    * Sets a new color to the context.
    * @param color the new color.
    */
    public void setForegroundColor(Color foregroundColor) {
        graphicContext.setForegroundColor(foregroundColor);
        repaint();
    }

    /**
    * Returns the foreground color used.
    */
    public Color getForegroundColor() {
        return graphicContext.getForegroundColor();
    }

    /**
    * Sets a new background color for the display.
    * @param background the color used as background for the display.
    */
    public void setBackgroundColor(Color backgroundColor) {
        graphicContext.setBackgroundColor(backgroundColor);
        repaint();
    }

    /**
    * Returns the background color used.
    */
    public Color getBackgroundColor() {
        return graphicContext.getBackgroundColor();
    }

    /**
    * Sets a new selection color to the context.
    * @param selectionColor the new color.
    */
    public void setSelectionColor(Color selectionColor) {
        graphicContext.setSelectionColor(selectionColor);
        repaint();
    }

    /**
    * Returns the selection color used.
    */
    public Color getSelectionColor() {
        return graphicContext.getSelectionColor();
    }

    /**
    * Sets if we draw the bounds of fr.ove.openmath.jome.
    * @param drawBounds <CODE>true</CODE> if we want the bounds to be drawn.
    * <CODE>false</CODE> otherwise.
    */
    public void setDrawBounds(boolean drawBounds) {
        if (drawBounds)
            jomeDisplay.drawBounds();
        else
            jomeDisplay.dontDrawBounds();
    }

    /**
    * Retuns <CODE>true</CODE> if we want the bounds to be drawn.
    * <CODE>false</CODE> otherwise.
    */
    public boolean getDrawBounds() {
        return jomeDisplay.weDrawBounds();
    }

    /**************************************************/
    /* Iconification/DÃ©siconification de la sÃ©lection */
    /**************************************************/

    /**
    * Iconifies the selection.
    */
    public void iconifySelection() {
        formulaDisplay.iconify();
    }

    /**
    * Uniconifies the selected icons.
    */
    public void uniconifySelectedIcons() {
        formulaDisplay.uniconify();
    }

    /**
    * Uniconifies all icons in the expression.
    */
    public void uniconifyAllIcons() {
        formulaDisplay.uniconifyAll();
    }

    /**
    * Substitutes the selection by the specified friendly name.
    * @param the name for the substitution
    */
    public void substitute(String name) {
        if ((name != null) && !name.equals(""))
            formulaDisplay.substitute(name);
    }
}
