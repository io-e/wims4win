/*
 * Created on 07.10.2004
 *
 */
package rene.zirkel.dialogs;

import java.awt.*;
import java.awt.event.*;

import rene.gui.*;
import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelFrame;

/**
 * @author Rene
 * This is a dialog to scale export to PNG.
 */
public class ExportScaler
	extends HelpCloseDialog implements ItemListener
{	TextFieldAction W,H,Scale,Width,Height,DPI,LineWidth,TextSize,PointSize;
	Checkbox Middle,Sizes,Aspect,Latex;
	ZirkelFrame F;
	boolean AbsCoord;
	MyChoice Presets;
	
	String Pres[]={
		"printscaler.preset",
		"printscaler.preset.300dpi",
		"printscaler.preset.300dpi12",
		"printscaler.preset.window",
		"printscaler.preset.clip50",
		"printscaler.preset.latex10"
	};
	
	String PresVect[]={
		"printscaler.preset",
		"printscaler.preset.300dpi",
		"printscaler.preset.300dpi12",
	};
	
	public ExportScaler (ZirkelFrame f, boolean abscoord)
	{	super(f,Global.name("printscaler.title"),true);
		F=f;
		AbsCoord=abscoord;
		setLayout(new BorderLayout());
		
		Panel panel0=new MyPanel();
		panel0.setLayout(new GridLayout(1,2));
		
		panel0.add(new MyLabel(Zirkel.name("printscaler.presets")));
		panel0.add(Presets=new MyChoice());
		if (abscoord)
			for (int i=0; i<Pres.length; i++)
				Presets.add(Zirkel.name(Pres[i]));
		else
			for (int i=0; i<PresVect.length; i++)
				Presets.add(Zirkel.name(PresVect[i]));
		Presets.addItemListener(this);
		
		add("North",new Panel3D(panel0));
		
		Panel panel1=new MyPanel();
		panel1.setLayout(new BorderLayout());
		
		Panel north1=new MyPanel();
		north1.setLayout(new BorderLayout());
		
		Panel north=new MyPanel();
		north.setLayout(new GridLayout(0,2));
		
		double scale=Global.getParameter("printscale.scale",1.0);
		int w=(int)(F.ZC.IW*scale);
		int h=(int)(F.ZC.IH*scale);
		north.add(new MyLabel(Zirkel.name("printscaler.w")));
		north.add(W=new TextFieldAction(this,"w"));
		W.setText(""+Global.getParameter("printscale.w",w));
		north.add(new MyLabel(Zirkel.name("printscaler.h")));
		north.add(H=new TextFieldAction(this,"h"));
		H.setText(""+Global.getParameter("printscale.h",h));
		north.add(new MyLabel(Zirkel.name("printscaler.scale")));
		north.add(Scale=new TextFieldAction(this,"scale"));
		Scale.setText(""+Global.getParameter("printscale.scale",1.0));
		north.add(new MyLabel(Zirkel.name("printscaler.aspect")));
		north.add(Aspect=new CheckboxAction(this,"","aspect"));
		Aspect.setState(Global.getParameter("printscale.aspect",false));
		north.add(new MyLabel(Zirkel.name("printscaler.middle")));
		north.add(Middle=new CheckboxAction(this,"","middle"));
		Middle.setState(Global.getParameter("printscale.middle",false));
		if (abscoord) panel1.add("North",new Panel3D(north));
		
		north1.add("Center",new Panel3D(north));
		
		Panel north2=new MyPanel();
		north2.setLayout(new GridLayout(0,2));
		
		north2.add(new MyLabel(Zirkel.name("printscaler.latex")));
		north2.add(Latex=new CheckboxAction(this,"","latex"));
		Latex.setState(Global.getParameter("printscale.latex",false));
		
		north1.add("South",new Panel3D(north2));
		
		if (abscoord) panel1.add("North",north1);
		
		Panel middle=new MyPanel();
		middle.setLayout(new GridLayout(0,2));
		middle.add(new MyLabel(Zirkel.name("printscaler.width")));
		middle.add(Width=new TextFieldAction(this,"width"));
		Width.setText(""+Global.getParameter("printscale.width",5.0));
		middle.add(new MyLabel(Zirkel.name("printscaler.height")));
		middle.add(Height=new TextFieldAction(this,"height"));
		Height.setText(""+Global.getParameter("printscale.height",5.0));
		middle.add(new MyLabel(Zirkel.name("printscaler.dpi")));
		middle.add(DPI=new TextFieldAction(this,"dpi"));
		DPI.setText(""+Global.getParameter("printscale.dpi",300));
		panel1.add("Center",new Panel3D(middle));
		
		Panel south=new MyPanel();
		south.setLayout(new GridLayout(0,2));
		
		south.add(new MyLabel(Zirkel.name("printscaler.sizes")));
		south.add(Sizes=new CheckboxAction(this,"","sizes"));
		Sizes.setState(Global.getParameter("printscale.sizes",true));
		south.add(new MyLabel(Zirkel.name("printscaler.linewidth")));
		south.add(LineWidth=new TextFieldAction(this,"linewidth"));
		LineWidth.setText(""+Global.getParameter("printscale.linewidth",0.02));
		south.add(new MyLabel(Zirkel.name("printscaler.pointsize")));
		south.add(PointSize=new TextFieldAction(this,"pointsize"));
		PointSize.setText(""+Global.getParameter("printscale.pointsize",0.07));
		south.add(new MyLabel(Zirkel.name("printscaler.textsize")));
		south.add(TextSize=new TextFieldAction(this,"textsize"));
		TextSize.setText(""+Global.getParameter("printscale.textsize",0.3));
		panel1.add("South",new Panel3D(south));
				
		add("Center",panel1);
		
		Panel panel2=new MyPanel();
		panel2.add(new ButtonAction(this,Zirkel.name("ok"),"OK"));
		panel2.add(new ButtonAction(this,Zirkel.name("cancel"),"Close"));
		addHelp(panel2,"print");
		
		add("South",new Panel3D(panel2));
		
		String last=Global.getParameter("printscaler.changed","");
		if (last.equals("scale")) doAction("scale");
		else if (last.equals("widthheight")) doAction("width");
		else doAction("w");
		
		if (Middle.getState()) Sizes.setState(false);
		
		pack();
		center(f);
		setVisible(true);
	}
	
	public double round (double x)
	{	return Math.round(x*100)/100.0;
	}
	
	public void doAction (String o)
	{	if (o.equals("width") || o.equals("height"))
		{	double w=getValue(Width,5,1,100);
			double h=getValue(Height,5,1,100);
			Width.setText(""+round(w));
			Height.setText(""+round(h));
			if (Aspect.getState())
			{	if (o.equals("width"))
					Height.setText(""+round(w*F.ZC.IH/F.ZC.IW));
				else if (o.equals("height"))
					Width.setText(""+round(h*F.ZC.IW/F.ZC.IH));
			}
			setWHS();
			Global.setParameter("printscaler.changed","widthheight");
		}
		else if (o.equals("dpi"))
		{	String last=Global.getParameter("printscaler.changed","");
			if (last.equals("wh") || last.equals("scale"))
				doAction("w");
			else
				doAction("width");
			return;
		}
		else if (o.equals("w") || o.equals("h"))
		{	double w=getValue(W,F.ZC.IW,50,5000);
			double h=getValue(H,F.ZC.IW,50,5000);
			W.setText(""+(int)w);
			H.setText(""+(int)h);
			if (Aspect.getState())
			{	if (o.equals("w"))
					H.setText(""+(int)(w*F.ZC.IH/F.ZC.IW));
				else if (o.equals("h"))
					W.setText(""+(int)(h*F.ZC.IW/F.ZC.IH));
			}
			Scale.setText(""+(w/F.ZC.IW));
			Global.setParameter("printscaler.changed","wh");
			setWidthHeight();
		}
		else if (o.equals("scale"))
		{	double scale=getValue(Scale,1.0,0.1,100);
			W.setText(""+(int)(F.ZC.IW*scale));
			H.setText(""+(int)(F.ZC.IH*scale));
			if (Global.getParameter("printscaler.changed","").equals("scaled"))
			{	double dpi=getValue(W,F.ZC.IW,50,5000)/
					(getValue(Width,5,1,100)/2.54);
				DPI.setText(""+round(dpi));
				System.out.println(dpi);
			}
			Global.setParameter("printscaler.changed","scale");
			setWidthHeight();
		}
		else if (o.equals("OK"))
		{	if (Width.isChanged() || Height.isChanged() || DPI.isChanged())
			{	setWHS();
				Global.setParameter("printscaler.changed","widthheight");
			}
			else if (W.isChanged() || H.isChanged())
			{	setWidthHeight();
				Global.setParameter("printscaler.changed","wh");
			}
			else if (Scale.isChanged())
			{	double scale=getValue(Scale,1.0,0.1,100);
				W.setText(""+(int)(F.ZC.IW*scale));
				H.setText(""+(int)(F.ZC.IH*scale));
				Global.setParameter("printscaler.changed","scale");
				setWidthHeight();	
			}
			Global.setParameter("printscale.w",
				getValue(W,F.ZC.IW,50,5000));
			Global.setParameter("printscale.h",
				getValue(H,F.ZC.IW,50,5000));
			Global.setParameter("printscale.scale",
				getValue(Scale,1,0.1,100));
			Global.setParameter("printscale.width",
				getValue(Width,5,1,100));
			Global.setParameter("printscale.height",
				getValue(Height,5,1,100));
			Global.setParameter("printscale.dpi",
				getValue(DPI,300,50,1200));
			Global.setParameter("printscale.linewidth",
				getValue(LineWidth,0.02,0.005,0.5));
			Global.setParameter("printscale.pointsize",
				getValue(PointSize,0.07,0.01,2));
			Global.setParameter("printscale.textsize",
				getValue(TextSize,0.3,0.05,2));
			Global.setParameter("printscale.middle",Middle.getState());
			Global.setParameter("printscale.sizes",Sizes.getState());
			Global.setParameter("printscale.aspect",Aspect.getState());
			Global.setParameter("printscale.latex",Latex.getState());
			doclose();			
		}
		else super.doAction(o);
	}
	
	public void itemAction (String o, boolean flag)
	{	if (o.equals("middle"))
		{	Sizes.setState(false);
		}
		else if (o.equals("sizes"))
		{	Middle.setState(false);
		}
	}
	
	public void setWHS ()
	{	double width=getValue(Width,5,1,100);
		double height=getValue(Height,5,1,100);
		double dpi=getValue(DPI,300,50,4800);
		W.setText(""+(int)(width/2.54*dpi));
		H.setText(""+(int)(height/2.54*dpi));
		Scale.setText(""+(width/2.54*dpi/F.ZC.IW));
	}
	
	public void setWidthHeight ()
	{	int w=(int)getValue(W,F.ZC.IW,50,5000);
		int h=(int)getValue(H,F.ZC.IW,50,5000);
		double dpi=getValue(DPI,300,50,4800);
		Width.setText(""+round(w/dpi*2.54));
		Height.setText(""+round(h/dpi*2.54));
	}
	
	double getValue (TextField f, double def, double min, double max)
	{	try
		{	String s=f.getText();
			double factor=1;
			if (s.endsWith("pt"))
			{	factor=0.035; s=s.substring(0,s.length()-2);
			}
			else if (s.endsWith("cm"))
			{	factor=1; s=s.substring(0,s.length()-2);
			}
			else if (s.endsWith("mm"))
			{	factor=0.1; s=s.substring(0,s.length()-2);
			}
			else if (s.endsWith("cm"))
			{	factor=1; s=s.substring(0,s.length()-2);
			}
			else if (s.endsWith("''"))
			{	factor=1/2.54; s=s.substring(0,s.length()-2);
			}
			else if (s.endsWith("cm"))
			{	factor=1; s=s.substring(0,s.length()-2);
			}
			else if (s.endsWith("in"))
			{	factor=1/2.54; s=s.substring(0,s.length()-2);
			}
			double x=new Double(s).doubleValue()*factor;
			if (x>max) x=max;
			if (x<min) x=min;
			return x;
		}
		catch (Exception e) { return def; }
	}

	public void itemStateChanged (ItemEvent e)
	{	switch (Presets.getSelectedIndex())
		{	case 1 :
				DPI.setText("300");
				Width.setText(""+round(F.ZC.DX));
				Middle.setState(false);
				Aspect.setState(true);
				Sizes.setState(true);
				LineWidth.setText(""+0.02);
				PointSize.setText(""+0.07);
				TextSize.setText(""+0.3);
				Latex.setState(false);
				doAction("width");
				Global.setParameter("printscaler.changed","scaled");
				break;
			case 2 :
				DPI.setText("300");
				Width.setText("12.0");
				Middle.setState(false);
				Aspect.setState(true);
				Sizes.setState(true);
				LineWidth.setText(""+0.02);
				PointSize.setText(""+0.07);
				TextSize.setText(""+0.3);
				Latex.setState(false);
				doAction("width");
				break;
			case 3 :
				Scale.setText("1.0");
				DPI.setText("100");
				Middle.setState(false);
				Aspect.setState(true);
				Sizes.setState(false);
				Latex.setState(false);
				doAction("scale");
				break;
			case 4 :
				Scale.setText("0.5");
				DPI.setText("100");
				Middle.setState(true);
				Aspect.setState(false);
				Sizes.setState(false);
				Latex.setState(false);
				doAction("scale");
				break;
			case 5 :
				DPI.setText("300");
				Width.setText("12.0");
				Middle.setState(false);
				Aspect.setState(true);
				Sizes.setState(true);
				LineWidth.setText(""+0.02);
				PointSize.setText(""+0.07);
				TextSize.setText(""+0.35);
				Latex.setState(true);
				doAction("width");
				break;
		}
	}
	
}
