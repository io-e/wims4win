package rene.zirkel;

import java.awt.Image;

import rene.zirkel.macro.*;

public interface ZirkelCanvasInterface
{	public void replayChosen ();
	public void runMacro (Macro m);
	public boolean enabled (String tool);
	public String loadImage ();
	public Image doLoadImage (String filename);
}
