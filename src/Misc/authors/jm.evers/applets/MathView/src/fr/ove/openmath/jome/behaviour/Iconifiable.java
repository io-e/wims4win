/*
$Id: Iconifiable.java,v 1.3 2003/02/18 11:48:46 sander Exp $
*/


/*
Copyright (C) 2001-2002 Mainline Project (I3S - ESSI - CNRS -UNSA)

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

For further information on the GNU Lesser General Public License,
see: http://www.gnu.org/copyleft/lesser.html
For further information on this library, contact: mainline@essi.fr
*/


package fr.ove.openmath.jome.behaviour;

/**
* Interface that must implement all objects that want to be iconified and then uniconified.<BR>
*
* @author Â© 1999 DIRAT Laurent
* @version 2.0 29/06/1999
*/
public interface Iconifiable {
    /**
    * Associates an icon name to the instance.
    *
    * @param iconName the name of the icon
    */
    public void setIconName(String iconName);

    /**
    * Returns the name of the icon associated to the instance.
    *
    * @returns The name of the icon, or <CODE>null</CODE> if there is no name
    * associated.
    */
    public String getIconName();
    
    /**
    * @return <CODE>true</CODE> if the instance is an icon. <CODE>false</CODE> otherwise.
    */
    public boolean isIcon();
    
    /**
    * Iconifies the selection of the instance.<BR>
    * An icon, can't be inconified. But, we can inconify icons _plus_
    * non-icon parts. The process works as russian dolls.
    */
    public void iconify();
    
    /**
    * Uniconifies all the selected icons.<BR>
    * If the icons contains other icons, these are _not_ uniconified. There is no
    * recursion process.
    */
    public void uniconify();
    
    /**
    * Uniconifies all the selected icons.<BR>
    * If the icons contains other icons, these are uniconified. There is a
    * recursion process.
    */
    public void uniconifyAll();
    
    /**
    * Sets the istance as iconifiable.
    * @param isIconifiable <CODE>true</CODE> if the instance is iconifiable.
    * <CODE>false</CODE> otherwise.
    */
    public void setIsIconifiable(boolean isIconifiable);
    
    /**
    * Returns <CODE>true</CODE> if the instance is iconifiable.
    * <CODE>false</CODE> otherwise.
    */
    public boolean isIconifiable();
}
