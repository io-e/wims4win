package rene.zirkel.construction;

import rene.zirkel.ZirkelCanvas;
import rene.zirkel.objects.*;

public interface Selector
{	public boolean isAdmissible (ZirkelCanvas zc, ConstructionObject o);
}
