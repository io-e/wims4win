/*
    Sketch Elements: Chemistry molecular diagram drawing tool.
    
    (c) 2005 Dr. Alex M. Clark
    
    Released as GNUware, under the Gnu Public License (GPL)
    
    See www.gnu.org for details.
*/

package WIMSchem;

public interface MolSelectListener
{
    public void molSelected(EditorPane source,int idx,boolean dblclick);
    public void dirtyChanged(boolean isdirty);
}
