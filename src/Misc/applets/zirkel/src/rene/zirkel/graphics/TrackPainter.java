package rene.zirkel.graphics;

import java.util.Enumeration;

import rene.zirkel.ZirkelCanvas;

public interface TrackPainter
{	public Enumeration elements ();
	public void paint (MyGraphics g, ZirkelCanvas zc);
	public void validate (ZirkelCanvas zc);
}
