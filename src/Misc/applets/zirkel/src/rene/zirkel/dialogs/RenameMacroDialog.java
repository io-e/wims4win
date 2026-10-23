/*
 * Created on 22.10.2005
 *
 */
package rene.zirkel.dialogs;

import java.awt.*;

import rene.gui.*;
import rene.zirkel.*;
import rene.zirkel.macro.*;

public class RenameMacroDialog extends CloseDialog
{	boolean Aborted=true; 
	TextField Name; 
	TextArea Comment;
	
	public RenameMacroDialog (Frame frame, Macro m)
	{	super(frame,Zirkel.name("renamemacro.title","Rename Macro"),true); 
		setLayout(new BorderLayout()); 
		
		Panel north=new MyPanel(); 
		north.setLayout(new GridLayout(0,1)); 
		north.add(new MyLabel(Zirkel.name("renamemacro.name"))); 
		north.add(Name=new TextFieldAction(this,"OK",m.getName(),64)); 
		add("North",new Panel3D(north)); 
		
		add("Center",new Panel3D(Comment=new TextArea("",5,40,TextArea.SCROLLBARS_VERTICAL_ONLY)));
		Comment.setText(m.getComment());
		
		Panel south=new MyPanel(); 
		south.add(new ButtonAction(this,Zirkel.name("ok"),"OK")); 
		south.add(new ButtonAction(this,Zirkel.name("cancel"),"Close")); 
		add("South",new Panel3D(south)); 
		
		pack(); 
	}
	
	public void doAction (String o)
	{	Aborted=true; 
		if (o.equals("OK"))
		{	Aborted=false; 
			doclose(); 
		}
		else super.doAction(o); 
	}
	
	public String getName ()
	{	return Name.getText(); 
	}
	
	public String getComment ()
	{	return Comment.getText();
	}
	
	public boolean isAborted ()
	{	return Aborted; 
	}
}

