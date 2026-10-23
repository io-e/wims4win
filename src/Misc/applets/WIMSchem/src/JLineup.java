/*
    Sketch Elements: Chemistry molecular diagram drawing tool.
    
    (c) 2008 Dr. Alex M. Clark
    
    Released as GNUware, under the Gnu Public License (GPL)
    
    See www.gnu.org for details.
*/

package WIMSchem;

import java.io.*;
import java.awt.*;
import java.util.*;
import java.awt.image.*;
import java.awt.event.*;
import javax.swing.*;

/*
    JLineup: a medium-weight layout-support component for Swing, which encapsulates some of the fundamental aspects of making
    everything in a panel line up nicely. Somewhat of a subset of the functionality offered by GroupLayout (v1.6 and later).
    
    Two modes:
    	VERTICAL - the child components are lined up from top to bottom; titles+padding line up
	HORIZONTAL - the child components are lined up from left to right; titles only line up on the baseline
	
    Both modes have optional titles, though the vertical invocation goes further than just the basic convenience of not
    needing to do the widget creation. Both have stretch weights for X & Y, though the meanings are inverted. For vertical
    lineups, the Y weights affect the overall size of the widget, while the X weights just affect how existing space is
    filled. The converse is true for horizontal lineups.
*/


public class JLineup extends JPanel
{
    class Unit
    {
    	Component c;
	JLabel label;
	int stretchX,stretchY;
    }
    private ArrayList<Unit> content=new ArrayList<Unit>();

    public static final int VERTICAL=1;
    public static final int HORIZONTAL=2;
    private int dir,padding;

    // Constructor:
    // dir: must be VERTICAL or HORIZONTAL
    // padding: number of pixels between each component, and around the edges

    public JLineup(int dir)
    {
    	super();
	this.dir=dir;
	this.padding=0;
    }
    public JLineup(int dir,int padding)
    {
    	super();
	this.dir=dir;
	this.padding=padding;
    }
    
    // for cleaning house
    
    public void clear()
    {
    	for (int n=0;n<content.size();n++)
	{
	    Unit u=content.get(n);
	    super.remove(u.c);
	    if (u.label!=null) super.remove(u.label);
	}
	content.clear();
    }
    public void remove(Component c)
    {
    	for (int n=0;n<content.size();n++)
	{
	    Unit u=content.get(n);
	    if (u.c==c)
	    {
	    	super.remove(c);
		if (u.label!=null) super.remove(u.label);
	    }
	}
    }
    
    // convenience shortcuts
    
    public Component add(Component c) {return add(c,null,0,0);}
    public Component add(Component c,String title) {return add(c,title,0,0);}
    
    public Component add(Component c,String title,int stretchX,int stretchY)
    {
    	super.add(c);

    	Unit u=new Unit();
	u.c=c;
	u.label=null;
	u.stretchX=stretchX;
	u.stretchY=stretchY;
    	
	if (title!=null)
	{
	    u.label=new JLabel(title);
	    super.add(u.label);
	}
	
	content.add(u);
	invalidate();
	
	return c;
    }
    
    public Dimension getMinimumSize()
    {
    	Dimension sz=new Dimension(0,0);
	
	if (dir==VERTICAL)
	{
	    int mw=0;
    	    for (int n=0;n<content.size();n++)
	    {
		Unit u=content.get(n);
		mw=Math.max(mw,u.c.getPreferredSize().width);
		sz.height+=Math.max(u.label==null ? 0 : u.label.getPreferredSize().height,u.c.getPreferredSize().height);
	    }
	    sz.width=calculateTitleWidth()+mw+2*padding;
	    sz.height+=padding*(content.size()+1);
	}
	else // HORIZONTAL
	{
	    int mh=0;
    	    for (int n=0;n<content.size();n++)
	    {
		Unit u=content.get(n);
		if (u.label!=null) 
		{
		    sz.width+=u.label.getPreferredSize().width+padding;
		    sz.height=Math.max(sz.height,u.label.getPreferredSize().height);
		}
		sz.width+=u.c.getPreferredSize().width;
		sz.height=Math.max(sz.height,u.c.getPreferredSize().height);
	    }
	    sz.width+=padding*(content.size()+1);
	    sz.height+=padding*2;
	}
	
	return sz;
    }
    public Dimension getPreferredSize() {return getMinimumSize();}
    public Dimension getMaximumSize()
    {
    	Dimension sz=new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE);
	boolean anyX=false,anyY=false;
    	for (int n=0,y=0;n<content.size();n++)
	{
	    Unit u=content.get(n);
	    anyX=anyX || u.stretchX>0;
	    anyY=anyY || u.stretchY>0;
	}
    	if (!anyX || !anyY)
	{
	    Dimension psz=getPreferredSize();
	    if (!anyX) sz.width=psz.width;
	    if (!anyY) sz.height=psz.height;
	}

	return sz;
    }
    
    public void doLayout()
    {
    	if (dir==VERTICAL) layoutVertical();
	else layoutHorizontal();
    }
    
    private void layoutVertical()
    {
    	Dimension prefsz=getPreferredSize(),cursz=getSize();
	int totalStretch=0,stretchPoints=cursz.height-prefsz.height;
	if (stretchPoints>0) for (int n=0;n<content.size();n++) totalStretch+=content.get(n).stretchY;
	
    	int tw=calculateTitleWidth(),residual=stretchPoints,cw=cursz.width-tw-2*padding;
    	for (int n=0,y=padding;n<content.size();n++)
	{
	    Unit u=content.get(n);
	    int h=Math.max(u.label==null ? 0 : u.label.getPreferredSize().height,u.c.getPreferredSize().height);
	    if (u.stretchY>0 && stretchPoints>0)
	    {
	    	int extra=(int)Math.min(Math.ceil(stretchPoints*u.stretchY/(double)totalStretch),residual);
		h+=extra;
		residual-=extra;
	    }
	    if (u.label!=null) 
	    {
	    	int ly=y;
		if (u.c instanceof JComponent)
		{
		    int delta=((JComponent)u.c).getBaseline(tw,h)-u.label.getBaseline(cw,u.label.getPreferredSize().height);
		    ly+=delta>0 ? delta : 0;
		}
	    	u.label.setBounds(padding,ly,tw,u.label.getPreferredSize().height);
	    }
	    u.c.setBounds(padding+tw,y,u.stretchX==0 ? u.c.getPreferredSize().width : cw,h);
	    
	    y+=h+padding;
	}
    }
    
    private void layoutHorizontal()
    {
    	Dimension prefsz=getPreferredSize(),cursz=getSize();
	int totalStretch=0,stretchPoints=cursz.height-prefsz.height-2*padding;
	if (stretchPoints>0) for (int n=0;n<content.size();n++) totalStretch+=content.get(n).stretchX;
	
	int residual=stretchPoints;
	for (int n=0,x=padding;n<content.size();n++)
	{
	    Unit u=content.get(n);
	    int w=u.c.getPreferredSize().width,h=cursz.height-2*padding;
	    int ch=u.stretchY==0 ? u.c.getPreferredSize().height : h;
	    if (u.stretchX>0 && stretchPoints>0)
	    {
	    	int extra=(int)Math.min(Math.ceil(stretchPoints*u.stretchX/(double)totalStretch),residual);
		w+=extra;
		residual-=extra;
	    }
	    if (u.label!=null)
	    {
		int ly=padding;
		Dimension lsz=u.label.getPreferredSize();
		if (u.c instanceof JComponent)
		{
		    // (NOTE: this matches the baseline for pairs; probably should do for all components...)
	    	    int delta=((JComponent)u.c).getBaseline(w,ch)-u.label.getBaseline(lsz.width,lsz.height);
		    ly+=delta>0 ? delta : 0;
		}
		u.label.setBounds(x,ly,lsz.width,lsz.height);
		x+=lsz.width+padding;
	    }
	    u.c.setBounds(x,padding,w,ch);
	    x+=w+padding;
	}
    }
    
    private int calculateTitleWidth()
    {
    	int w=0;
    	for (int n=0;n<content.size();n++)
	{
	    Unit u=content.get(n);
	    if (u.label==null) continue;
	    w=Math.max(w,u.label.getPreferredSize().width);
	}
	if (w>0) w+=padding;
	return w;
    }
}
