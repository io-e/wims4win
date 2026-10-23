package rene.zirkel.dialogs;

import java.awt.*;
import java.io.*;

import rene.gui.*;
import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelFrame;
import rene.dialogs.*;

public class EditRunDialog extends HelpCloseDialog
{	String Filename;
	ZirkelFrame ZF;
	TextArea Text;

	public EditRunDialog (ZirkelFrame zf, String filename)
	{	super(zf,Zirkel.name("editrun.title"),true);
		Filename=filename;
		ZF=zf;
		setLayout(new BorderLayout());
		
		Panel north=new MyPanel();
		north.add(new MyLabel(filename));
		add("North",new Panel3D(north));
		
		add("Center",new Panel3D(Text=new TextArea(30,60)));
		if (Global.FixedFont!=null) Text.setFont(Global.FixedFont);
		
		load(filename);
		
		Panel south=new MyPanel();
		south.add(new ButtonAction(this,Zirkel.name("editrun.run"),"Run"));
		south.add(new ButtonAction(this,Zirkel.name("editrun.load"),"Load"));
		south.add(new ButtonAction(this,Zirkel.name("abort"),"Close"));
		addHelp(south,"visual");
		add("South",new Panel3D(south));
		
		setSize("editrun");
		center(zf);
	}
	
	public void doAction (String s)
	{	noteSize("editrun");
		if (s.equals("Run"))
		{	doclose();
			try
			{	save(Filename);
			}
			catch (Exception e)
			{	Warning w=new Warning(ZF,Zirkel.name("editrun.error"),
					Zirkel.name("warning"),true);
				w.center(ZF);
				w.setVisible(true);
				return; 
			}
			ZF.loadRun(Filename);
		}
		else if (s.equals("Load"))
		{	doclose();
			ZF.editRun("");
		}
		else super.doAction(s);
	}
	
	public void load (String filename)
	{	Text.setText("");
		Text.setEnabled(false);
		try
		{	BufferedReader in=new BufferedReader(
				new InputStreamReader(new FileInputStream(filename)));
			while (true)
			{	String line=in.readLine();
				if (line==null) break;
				Text.append(line+"\n");
			}
			in.close();
		}
		catch (Exception e) {}
		Text.setEnabled(true);
		Text.repaint();
	}
	
	public void save (String filename)
		throws IOException
	{	String text=Text.getText();
		PrintWriter out=new PrintWriter(
			new OutputStreamWriter(new FileOutputStream(filename)));
		out.print(text);
		out.close();
	}
	
}
