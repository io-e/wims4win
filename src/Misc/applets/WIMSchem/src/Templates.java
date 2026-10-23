/*
    Sketch Elements: Chemistry molecular diagram drawing tool.
    
    (c) 2005 Dr. Alex M. Clark
    
    Released as GNUware, under the Gnu Public License (GPL)
    
    See www.gnu.org for details.
*/

package WIMSchem;

import java.io.*;
import java.net.*;
import java.util.*;

// For obtaining the template list.

public class Templates
{
    ArrayList<Molecule> templ=new ArrayList<Molecule>();

    // jm.evers: copies... of functions in applet. Should be reorganised !!
    public static byte [] loadURL(URL url) throws IOException {
        int bufSize = 1024 * 2;
	byte [] buf = new byte[bufSize];
	ByteArrayOutputStream bout = new ByteArrayOutputStream();
	BufferedInputStream   in   = new BufferedInputStream(url.openStream());
	int n;
	while ((n = in.read(buf)) > 0){
	    bout.write(buf, 0, n);
	}
	try 
	{ in.close(); } catch (Exception ignored) { }
	return bout.toByteArray();
    }

    public static String loadFile(String fname) throws IOException {
        byte[] bytes = loadURL(new URL("file:" + fname));
	return new String(bytes);
    }

    public static String load(String fileOrURL) throws IOException {
	try {
	    URL url = new URL(fileOrURL);
	    return new String(loadURL(url));
	} catch (Exception e) { return loadFile(fileOrURL);}
    }
        

    public Templates(Class cls)
    {
    	ArrayList<String> list=new ArrayList<String>();
	// jm.evers : if an param template is defined, load them in the ArrayList.
	if( MainApplet.templateURL != null ){
	    for(int p=0; p<MainApplet.templateURL.length ;p++ ){
		try {
		    //Molecule mol = MoleculeStream.readMDLMOL(new BufferedReader(new StringReader((load(MainApplet.templateURL[p])).toString())));    
		    Molecule mol = MoleculeStream.readUnknown(new BufferedReader(new StringReader((load(MainApplet.templateURL[p])).toString())));    
		    templ.add(mol);
	    	    System.out.println("loading template no"+p);
		} catch (IOException e) {System.out.println("FAILED loading template"+p+"\n Are you using correct MDLMol or Native files??");}
    	    }
	}
	else	
    	{    // read the list of molecules from the directory file, then create each one of them
    	    try
	    {
    		InputStream istr=cls.getResourceAsStream("/templ/list");
    	        BufferedReader in=new BufferedReader(new InputStreamReader(istr));
		String line;
	        while ((line=in.readLine())!=null) {list.add(line);}
		istr.close();
	    }
	    catch (IOException e) 
	    {
		System.out.println("Failed to obtain list of jar included default templates:\n"+e.toString());
		return;
	    }
    	    try
	    {
		for (int n=0;n<list.size();n++)
		{
    	    	    InputStream istr=cls.getResourceAsStream("/templ/"+list.get(n));
	    	    Molecule mol=MoleculeStream.readNative(istr);
		    templ.add(mol);
		    istr.close();
		}
    	    }
	    catch (IOException e) 
	    {
		System.out.println("Failed to obtain particular template:\n"+e.toString());
		return;
	    }
	}	
	// sort the molecules by an index of "complexity" (smaller molecules first, carbon-only favoured)
	
	int[] complex=new int[templ.size()];
	for (int n=0;n<templ.size();n++)
	{
	    Molecule mol=templ.get(n);
	    complex[n]=mol.numAtoms()*100;
	    boolean nonCH=false;
	    for (int i=1;i<=mol.numAtoms();i++) 
	    	if (mol.atomElement(i).compareTo("C")!=0 && mol.atomElement(i).compareTo("H")!=0) nonCH=true;
	    if (!nonCH) complex[n]-=1000;
	    for (int i=1;i<=mol.numBonds();i++) complex[n]=complex[n]+mol.bondOrder(i);
	}
	
	int p=0;
	while (p<templ.size()-1)
	{
	    if (complex[p]>complex[p+1])
	    {
	    	int i=complex[p]; complex[p]=complex[p+1]; complex[p+1]=i;
		Molecule mol=templ.get(p); templ.set(p,templ.get(p+1)); templ.set(p+1,mol);
	    	if (p>0) p--;
	    }
	    else p++;
	}
    }
    
    public int numTemplates() {return templ.size();}
    public Molecule getTemplate(int N) {return templ.get(N);}
    public void addTemplate(Molecule Mol) {templ.add(Mol);}
}




