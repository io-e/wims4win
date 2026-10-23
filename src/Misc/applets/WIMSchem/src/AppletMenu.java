/*
    Sketch Elements: Chemistry molecular diagram drawing tool.
    
    (c) 2005 Dr. Alex M. Clark
    
    Released as GNUware, under the Gnu Public License (GPL)
    
    See www.gnu.org for details.
*/

package WIMSchem;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import java.io.*;
import java.util.*;

// Subclassed version of JMenu which draws itself a bit differently

public class AppletMenu extends JMenu
{
    public AppletMenu(String Txt)
    {
    	super(Txt);
	setBorder(new SoftBevelBorder(BevelBorder.RAISED));
    }
    
    public Dimension getPreferredSize()
    {
    	Dimension sz=super.getPreferredSize();
	sz.width += 8;
	sz.height += 8;
	return sz;
    }
}
