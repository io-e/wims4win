/*
    Sketch Elements: Chemistry molecular diagram drawing tool.
    
    (c) 2007 Dr. Alex M. Clark
    
    Released as GNUware, under the Gnu Public License (GPL)
    
    See www.gnu.org for details.
*/

package WIMSchem;

/*
    Should be implemented by classes that wish to "listen in" on save events.
*/

public interface SaveListener
{
    public void saveMolecule(Molecule mol);
}
