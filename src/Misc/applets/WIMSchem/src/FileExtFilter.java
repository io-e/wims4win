/*
    Sketch Elements: Chemistry molecular diagram drawing tool.
    
    (c) 2005 Dr. Alex M. Clark
    
    Released as GNUware, under the Gnu Public License (GPL)
    
    See www.gnu.org for details.
*/

package WIMSchem;

import java.util.*;
import java.text.*;
import java.lang.*;
import java.io.*;
import javax.swing.filechooser.*;

// Selecting files by extension (strangely absent from Java).

public class FileExtFilter extends javax.swing.filechooser.FileFilter
{
    String descr;
    ArrayList<String> exts;
    
    public FileExtFilter(String descroot,String suffixes)
    {
	exts=new ArrayList<String>();
    	StringTokenizer tok=new StringTokenizer(suffixes,";");
	while (tok.hasMoreTokens()) exts.add(tok.nextToken());
	
    	descr=descroot+" (";
	for (int n=0;n<exts.size();n++) descr=descr+(n>0 ? " " : "")+"*"+exts.get(n);
	descr=descr+")";
    }
    
    public String getDescription() {return descr;}
    
    public boolean accept(File f)
    {
    	if (f.isDirectory()) return true;
    	for (int n=0;n<exts.size();n++) if (f.getName().endsWith(exts.get(n))) return true;
    	return false;
    }
}
