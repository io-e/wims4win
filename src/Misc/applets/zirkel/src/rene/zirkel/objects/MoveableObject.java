package rene.zirkel.objects;

public interface MoveableObject
{	public void move (double x, double y);
	public void startDrag (double x, double y);
	public void dragTo (double x, double y);
	public boolean moveable ();
}
