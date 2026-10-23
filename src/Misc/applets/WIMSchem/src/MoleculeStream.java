/*
    Sketch Elements: Chemistry molecular diagram drawing tool.
    
    (c) 2005 Dr. Alex M. Clark
    
    Released as GNUware, under the Gnu Public License (GPL)
    
    See www.gnu.org for details.
*/

package WIMSchem;

import java.util.*;
import java.io.*;
import java.text.*;
import java.lang.*;
import java.nio.channels.*;

/*
    Handles reading and writing of molecules to and from streams. Two formats are supported: native, which is a direct translation of
    the underlying data content; and a subset of MDL MOL, using only the fields that are relevant to WIMSchem.
*/

public class MoleculeStream
{
    // special implementation of the reader for when the format is not known a-priori, or might be a combination-of-two formats
    // as used by the clipboard; do some extra work to try to pull out the WIMSchem file preferentially
    public static Molecule readUnknown(InputStream istr) throws IOException 
    {
    	return readUnknown(new BufferedReader(new InputStreamReader(istr)));
    }
    
    public static Molecule readUnknown(BufferedReader in) throws IOException
    {
    	Molecule mdlmol=null,elmol=null;
	final int BUFFMAX=100000;
	in.mark(BUFFMAX);
	try
	{
	    mdlmol=readMDLMOL(in);
	    if (mdlmol!=null) in.mark(BUFFMAX); // so the WIMSchem version could follow
	}
	catch (IOException e) 
	{
	    mdlmol=null;
	    in.reset();
	}
	
	try
	{
	    elmol=readNative(in);
	}
	catch (IOException e) {elmol=null;}
	
	if (elmol!=null) return elmol;
	if (mdlmol!=null) return mdlmol;
	
	throw new IOException("Unknown or invalid format.");
    }

    public static Molecule readNative(InputStream istr) throws IOException 
    {
    	return readNative(new BufferedReader(new InputStreamReader(istr)));
    }
    public static Molecule readNative(BufferedReader in) throws IOException
    {
    	Molecule mol=new Molecule();
	final String GENERIC_ERROR="Invalid WIMSchem file.";
	
	try
	{
	    String line=in.readLine();
	    if (!line.startsWith("SketchEl!") && !line.startsWith("WIMSchem!")) throw new IOException("Not a WIMSchem file...could not find start tag \"SketchEl!\"");
	    int p1=line.indexOf('('),p2=line.indexOf(','),p3=line.indexOf(')');
	    if (p1==0 || p2==0 || p3==0) throw new IOException(GENERIC_ERROR);
	
	    int numAtoms=Integer.parseInt(line.substring(p1+1,p2).trim());
	    int numBonds=Integer.parseInt(line.substring(p2+1,p3).trim());
	    for (int n=0;n<numAtoms;n++) 
	    {
	    	line=in.readLine();
		String[] bits=line.split("[\\=\\,\\;]");
		if (bits.length<5) throw new IOException("WIMSchem format error: to few arguments in atomsection  line"+n);
		int num=mol.addAtom(bits[0],Double.parseDouble(bits[1].trim()),Double.parseDouble(bits[2].trim()),
		    	    	    Integer.parseInt(bits[3].trim()),Integer.parseInt(bits[4].trim()));
    	    	for (int i=5;i<bits.length;i++) if (bits[i].length()>0)
		{
		    if (bits[i].charAt(0)=='e') mol.setAtomHExplicit(num,Integer.parseInt(bits[i].substring(1)));
		    else if (bits[i].charAt(0)=='n') mol.setAtomMapNum(num,Integer.parseInt(bits[i].substring(1)));
		}
	    }
	    for (int n=0;n<numBonds;n++)
	    {
	    	line=in.readLine();
		String[] bits=line.split("[\\-\\=\\,]");
		if (bits.length<4) throw new IOException("WIMSchem fromat error : to few aguments in bondsection line "+n);
		mol.addBond(Integer.parseInt(bits[0].trim()),Integer.parseInt(bits[1].trim()),
		    	    Integer.parseInt(bits[2].trim()),Integer.parseInt(bits[3].trim()));
	    }
	    line=in.readLine();
	    if (line.compareTo("!End")!=0 && line.compareTo("!FIN")!=0) throw new IOException("could not find the end tag \"!End\"");
	}
	catch (Exception e)
	{
	    throw new IOException(GENERIC_ERROR);
	}

    	return mol;
    }
    
    public static void writeNative(OutputStream ostr,Molecule mol) throws IOException
    {
	writeNative(new BufferedWriter(new OutputStreamWriter(ostr)),mol);
    }
    public static void writeNative(BufferedWriter out,Molecule mol) throws IOException
    {
	DecimalFormat fmt = new DecimalFormat("0.0000",new DecimalFormatSymbols(Locale.US));
	
    	out.write("SketchEl!("+mol.numAtoms()+","+mol.numBonds()+")\n");
	for (int n=1;n<=mol.numAtoms();n++)
	{
	    String hy=mol.atomHExplicit(n)!=Molecule.HEXPLICIT_UNKNOWN ? ("e"+mol.atomHExplicit(n)) : ("i"+mol.atomHydrogens(n));
	    out.write(mol.atomElement(n)+"="+fmt.format(mol.atomX(n))+","+fmt.format(mol.atomY(n))+";"+
	    	    	    	    	    	    	    	    	mol.atomCharge(n)+","+mol.atomUnpaired(n)+","+hy);
    	    if (mol.atomMapNum(n)>0) out.write(",n"+mol.atomMapNum(n));
    	    out.write("\n");
	}
	for (int n=1;n<=mol.numBonds();n++)
	{
	    out.write(mol.bondFrom(n)+"-"+mol.bondTo(n)+"="+mol.bondOrder(n)+","+mol.bondType(n)+"\n");
	}
	out.write("!End\n");
	
	out.flush();
    }

    public static Molecule readMDLMOL(BufferedReader in) throws IOException
    {
    	Molecule mol=new Molecule();
    	final String GENERIC_ERROR="Invalid MDL MOL file.";

	try
	{
	    String line=null;
	    for (int n=0;n<4;n++) line=in.readLine();
	    if (!line.substring(34,39).equals("V2000")) throw new IOException(GENERIC_ERROR);
	    int numAtoms=Integer.parseInt(line.substring(0,3).trim());
	    int numBonds=Integer.parseInt(line.substring(3,6).trim());
	    for (int n=0;n<numAtoms;n++)
	    {
	    	line=in.readLine();
		double x=Double.parseDouble(line.substring(0,10).trim());
		double y=Double.parseDouble(line.substring(10,20).trim());
		String el=line.substring(31,34).trim();
		int chg=Integer.parseInt(line.substring(36,39).trim()),rad=0;
		int mapnum=Integer.parseInt(line.substring(60,63).trim());
		if (chg<=3) {}
		else if (chg==4) {chg=0; rad=2;}
		else chg=4-chg;
		mol.addAtom(el,x,y,chg,rad);
		mol.setAtomMapNum(mol.numAtoms(),mapnum);
	    }
	    for (int n=0;n<numBonds;n++)
	    {
	    	line=in.readLine();
		int from=Integer.parseInt(line.substring(0,3).trim()),to=Integer.parseInt(line.substring(3,6).trim());
		int type=Integer.parseInt(line.substring(6,9).trim()),stereo=Integer.parseInt(line.substring(9,12).trim());
		if (from==to || from<1 || from>numAtoms || to<1 || to>numAtoms) throw new IOException(GENERIC_ERROR);
		int order=type>=1 && type<=3 ? type : 1;
		int style=Molecule.BONDTYPE_NORMAL;
		if (stereo==1) style=Molecule.BONDTYPE_INCLINED;
		else if (stereo==6) style=Molecule.BONDTYPE_DECLINED;
		// !! supposed to be for double bonds... else if (stereo==3 || stereo==4) style=Molecule.BONDTYPE_UNKNOWN;
    	        mol.addBond(from,to,order,style);
	    }
	    while (true)
	    {
	    	line=in.readLine();
		if (line.startsWith("M  END")) break;
		int type=0;
		if (line.startsWith("M  CHG")) type=1;
		else if (line.startsWith("M  RAD")) type=2;
		else if (line.startsWith("M  RGP")) type=3;
		
		if (type>0)
		{
		    int len=Integer.parseInt(line.substring(6,9).trim());
		    for (int n=0;n<len;n++)
		    {
		    	int apos=Integer.parseInt(line.substring(9+8*n,13+8*n).trim());
			int aval=Integer.parseInt(line.substring(13+8*n,17+8*n).trim());
			if (apos<1 || apos>mol.numAtoms()) continue;
			
			if (type==1) mol.setAtomCharge(apos,aval); 
			else if (type==2) mol.setAtomUnpaired(apos,aval);
			else if (type==3) mol.setAtomElement(apos,"R"+aval);
		    }
		}
	    }
	}
	catch (Exception e) {throw new IOException(GENERIC_ERROR,e);}

    	return mol;
    }

    public static void writeMDLMOL(OutputStream ostr,Molecule mol) throws IOException
    {
	writeMDLMOL(new BufferedWriter(new OutputStreamWriter(ostr)),mol);
    }
    public static void writeMDLMOL(BufferedWriter out,Molecule mol) throws IOException
    {
    	DecimalFormat fmt=new DecimalFormat("0.0000",new DecimalFormatSymbols(Locale.US));

    	out.write("\nWIMSchem molfile\n\n");
	out.write(intrpad(mol.numAtoms(),3)+intrpad(mol.numBonds(),3)+"  0  0  0  0  0  0  0  0999 V2000\n");
    	
	String line;
	
	int numRGroups=0,rgAtom[]=new int[mol.numAtoms()],rgNumber[]=new int[mol.numAtoms()];
	
	// export atoms, and make a few notes along the way
	
	for (int n=1;n<=mol.numAtoms();n++)
	{
	    String str=fmt.format(mol.atomX(n));
	    line=rep(" ",10-str.length())+str;
	    str=fmt.format(mol.atomY(n)); 
	    line+=rep(" ",10-str.length())+str;
	    line+="    0.0000 ";

	    str=mol.atomElement(n);
	    if (str.length()>1 && str.charAt(0)=='R' && str.charAt(1)>='0' && str.charAt(1)<='9')
	    {
		rgAtom[numRGroups]=n;
		rgNumber[numRGroups]=Util.safeInt(str.substring(1));
		numRGroups++;
	    	str="R#";
	    }
	    line+=str+rep(" ",4-str.length())+"0";
	    
	    int chg=mol.atomCharge(n),spin=mol.atomUnpaired(n),mapnum=mol.atomMapNum(n);
	    if (chg>=-3 && chg<=-1) chg=4-chg;
    	    else if (chg==0 && spin==2) chg=4;
	    else if (chg<1 || chg>3) chg=0;
	    line+=intrpad(chg,3)+"  0  0  0  0  0  0  0"+intrpad(mapnum,3)+"  0  0";
	    
	    out.write(line+"\n");
	}
	
	// export bonds
	
	for (int n=1;n<=mol.numBonds();n++)
	{
	    int type=mol.bondOrder(n);
	    if (type<1 || type>3) type=1;
	    int stereo=mol.bondType(n);
	    if (stereo==Molecule.BONDTYPE_NORMAL) {}
	    else if (stereo==Molecule.BONDTYPE_INCLINED) {stereo=1; type=1;}
	    else if (stereo==Molecule.BONDTYPE_DECLINED) {stereo=6; type=1;}
	    else if (stereo==Molecule.BONDTYPE_UNKNOWN) {stereo=4; type=1;}
	    else stereo=0;

    	    out.write(intrpad(mol.bondFrom(n),3)+intrpad(mol.bondTo(n),3)+intrpad(type,3)+intrpad(stereo,3)+"  0  0  0\n");
	}
	
	// export charges
	
	int count=0;
	line="";
	for (int n=1;n<=mol.numAtoms();n++) if (mol.atomCharge(n)!=0)
	{
	    line+=intrpad(n,4)+intrpad(mol.atomCharge(n),4);
	    count++;
	    if (count==8)
	    {
		out.write("M  CHG"+intrpad(count,3)+line+"\n");
	    	count=0; line="";
	    }
	}
	if (count>0) out.write("M  CHG"+intrpad(count,3)+line+"\n");
	
	// export "unpaired" atom counts (aka radicals, sort of)
	
	count=0;
	line="";
	for (int n=1;n<=mol.numAtoms();n++) if (mol.atomUnpaired(n)!=0)
	{
	    line+=intrpad(n,4)+intrpad(mol.atomUnpaired(n),4);
	    count++;
	    if (count==8)
	    {
		out.write("M  RAD"+intrpad(count,3)+line+"\n");
	    	count=0; line="";
	    }
	}
	if (count>0) out.write("M  RAD"+intrpad(count,3)+line+"\n");
	
	// export R-group identifiers
	
    	count=0;
	line="";
	for (int n=0;n<numRGroups;n++)
	{
	    line+=intrpad(rgAtom[n],4)+intrpad(rgNumber[n],4);
	    count++;
	    if (count==8)
	    {
	    	out.write("M  RGP"+intrpad(count,3)+line+"\n");
		count=0; line="";
	    }
	}
	if (count>0) out.write("M  RGP"+intrpad(count,3)+line+"\n");
	
	out.write("M  END\n");
	out.flush();
    }
    
    public static void writeCMLXML(OutputStream ostr,Molecule mol) throws IOException
    {
	writeCMLXML(new BufferedWriter(new OutputStreamWriter(ostr)),mol);
    }
    public static void writeCMLXML(BufferedWriter out,Molecule mol) throws IOException
    {
    	out.write("<cml>\n");
	out.write("  <molecule>\n");

	out.write("    <atomArray>\n");
	for (int n=1;n<=mol.numAtoms();n++)
	{
	    out.write("      <atom id=\"a"+n+"\" elementType=\""+mol.atomElement(n)+"\""+
	    	    	    	 " x2=\""+mol.atomX(n)+"\" y2=\""+mol.atomY(n)+"\" hydrogenCount=\""+mol.atomHydrogens(n)+"\"/>\n");
	}
	out.write("    </atomArray>\n");
	
	out.write("    <bondArray>\n");
	for (int n=1;n<=mol.numBonds();n++)
	{
	    out.write("      <bond id=\"b"+n+"\" atomRefs2=\"a"+mol.bondFrom(n)+" a"+mol.bondTo(n)+"\" order=\""+mol.bondOrder(n)+"\"/>\n");
	}
	out.write("    </bondArray>\n");
	
	out.write("  </molecule>\n");
	out.write("</cml>\n");
	out.flush();
    }
    
    // examines the beginning of a file and decides whether it can be considered a database of structures which this class is capable
    // of reading...
    // (NB: currently this includes MDL SD-files, and nothing else)
    static boolean examineIsDatabase(FileInputStream istr) throws IOException 
    {
    	long lastpos=istr.getChannel().position();
	boolean isdb=findNextPosition(istr,0)>=0;
    	istr.getChannel().position(lastpos);
	return isdb;
    }
    
    static long findNextPosition(FileInputStream istr,long startpos) throws IOException
    {
    	FileChannel fch=istr.getChannel();
	fch.position(startpos);
	long pos=startpos,size=fch.size(),nextpos=-1;
	
	String rec="";
	while (nextpos<size)
	{
	    int inp=istr.read();
	    pos++;
	    if (inp<0) break;
	    char ch=(char)inp;
	    if (ch=='\r') continue;
	    rec=rec.concat(String.valueOf(ch));
	    if (rec.endsWith("$$$$\n")) {nextpos=pos; break;}
	}
	if (nextpos<0) return -1;

	try
	{
	    BufferedReader in=new BufferedReader(new StringReader(rec));
	    Molecule mol=readMDLMOL(in);
	    if (mol==null) nextpos=-1;
	}
	catch (IOException e) {nextpos=-1;}
	
	return nextpos;
    }
    
    static Molecule fetchFromPosition(FileInputStream istr,long pos) throws IOException
    {
    	istr.getChannel().position(pos);
	return readMDLMOL(new BufferedReader(new InputStreamReader(istr)));
    }
    
    // miscellaneous help
    
    static String intrpad(int Val,int Len)
    {
    	String str=Integer.toString(Val);
	str=rep(" ",Len-str.length())+str;
	if (str.length()>Len) str=str.substring(0,Len);
	return str;
    }
    static String rep(String Ch,int Len)
    {
    	if (Len<=0) return "";
    	String str=Ch;
	while (str.length()<Len) str=str+Ch;
	return str;
    }
}
