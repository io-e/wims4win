/*

*********************************************************************************
* J.M. Evers 19/11/2009*
* This is all amateur scriblings... So no copyrights.*
* This source code file, and compiled classes derived from it,*
* can be used and distributed without restriction, including for commercial use*
* No warrenty whatoever*
*********************************************************************************

*/
import java.awt.*;
import java.awt.event.*;

public class ChemInputFrame extends Frame implements ActionListener{

    public static void main(String args[]){
        new ChemInputFrame();
    }
    
                                                                                                        
    public ChemInputFrame(){
        super(ChemInputApplet.Title);
        setBackground(ChemInputApplet.bgcolor3);
        canvas = new ChemInputCanvas();
	add("Center", canvas);
	setTitle(ChemInputApplet.Title);
	getBounds();

        Menu textMenu = new Menu(ChemInputApplet.Text, true);
//        textMenu.add("HELP");
        textMenu.add(ChemInputApplet.Clear);
	textMenu.addSeparator();
        textMenu.add(ChemInputApplet.Quit);
	textMenu.addSeparator();
        textMenu.add(ChemInputApplet.More);
        textMenu.add(ChemInputApplet.Less);
        textMenu.addActionListener(this);
        
	Menu colorMenu = new Menu(ChemInputApplet.Color, true);
        colorMenu.add("Red");
        colorMenu.add("Blue");
        colorMenu.add("Black");
        colorMenu.addActionListener(this);

	Menu manipMenu = new Menu("+/-", true);
        manipMenu.add(ChemInputApplet.Insert);
        manipMenu.add(ChemInputApplet.Up);
        manipMenu.add(ChemInputApplet.Down);
        manipMenu.add(ChemInputApplet.Left);
        manipMenu.add(ChemInputApplet.Right);
        manipMenu.addActionListener(this);

        Menu chemMenu = new Menu(ChemInputApplet.Chemarrows, true);
        chemMenu.add(ChemInputApplet.Arrow1);
        chemMenu.add(ChemInputApplet.Arrow2);
	chemMenu.addSeparator();
        chemMenu.add(ChemInputApplet.Precip);
        chemMenu.add(ChemInputApplet.Solid);
        chemMenu.add(ChemInputApplet.Liquid);
        chemMenu.add(ChemInputApplet.Gas);
        chemMenu.add(ChemInputApplet.Precip);
        chemMenu.addActionListener(this);

	MenuBar mbar = new MenuBar();
	if(ChemInputApplet.chembuttons && ChemInputApplet.navbuttons){
    	    mbar.add(textMenu);
    	    mbar.add(colorMenu);
    	    mbar.add(manipMenu);
    	    mbar.add(chemMenu);
	}
	else
	{
	    if(ChemInputApplet.navbuttons){
    		mbar.add(textMenu);
    		mbar.add(colorMenu);
    	    mbar.add(manipMenu);
	    }
	    else
	    {
		if(ChemInputApplet.chembuttons){
    		    mbar.add(chemMenu);
    		    mbar.add(manipMenu);
		}
	    }
	}
        setMenuBar(mbar);
        setBounds(0, 0,ChemInputApplet.frame_x,ChemInputApplet.frame_y);
        setResizable(true);
        addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent evt){dispose();}});
	//pack();
        setVisible(true);
    }
    
    public void actionPerformed(ActionEvent evt){
        String command = evt.getActionCommand();
        if(command.equals("HELP [niet klaar]")){canvas.ShowHelp();}
	else
        if(command.equals(ChemInputApplet.Insert))
            canvas.InsertKey("?", canvas.marked,true);
        else
        if(command.equals(ChemInputApplet.Up))
	    canvas.UpDownMarked(1,true);
        else
        if(command.equals(ChemInputApplet.Down))
            canvas.UpDownMarked(-1,true);
        else
        if(command.equals(ChemInputApplet.Left))
            canvas.LeftRightMarked(-1,true);
        else
        if(command.equals(ChemInputApplet.Right))
            canvas.LeftRightMarked(1,true);
        else
        if(command.equals("Red")){canvas.pencolor = Color.red;canvas.update();}
        else
        if(command.equals("Blue")){canvas.pencolor = Color.blue;canvas.update();}
        else
        if(command.equals("Black")){canvas.pencolor = Color.black;canvas.update();}
        else
	if(command.equals(ChemInputApplet.Quit))
            dispose();
        else
        if(command.equals(ChemInputApplet.Arrow1))
	    canvas.AppendSymbol(ChemInputApplet.Arrow1, 1);
	else
        if(command.equals(ChemInputApplet.Arrow2))
	    canvas.AppendSymbol(ChemInputApplet.Arrow2, 1);
	else
        if(command.equals(ChemInputApplet.Gas))
	    canvas.AppendSymbol("(g)",0);
	else
        if(command.equals(ChemInputApplet.Precip))
	    canvas.AppendSymbol("\u2193",0);
	else
        if(command.equals(ChemInputApplet.Solid))
	    canvas.AppendSymbol("(s)",0);
	else
        if(command.equals(ChemInputApplet.Liquid))
	    canvas.AppendSymbol("(aq)",0);
	else
        if(command.equals(ChemInputApplet.Clear))
            canvas.clear();
	else
        if(command.equals(ChemInputApplet.More))
            canvas.SwitchFont(6);
	else
        if(command.equals(ChemInputApplet.Less))
            canvas.SwitchFont(-6);

    }
    ChemInputCanvas canvas;
}

