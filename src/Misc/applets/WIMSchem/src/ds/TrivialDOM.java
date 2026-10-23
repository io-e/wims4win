/*
    Sketch Elements: Chemistry molecular diagram drawing tool.
    
    (c) 2008 Dr. Alex M. Clark
    
    Released as GNUware, under the Gnu Public License (GPL)
    
    See www.gnu.org for details.
*/

package WIMSchem.ds;

import WIMSchem.*;

import java.io.*;
import java.util.*;

/*
    An incredibly lightweight implementation of DOM-style access to XML content. Only a subset of XML files are supported, that 
    being simple combinations of elements, attributes and text. Overly sophisticated input files may break the reader. Also, some
    of the pedantic XML treatment of whitespace is simplified (which suits the rest of this application nicely). Malformed XML
    should generate vaguely helpful explanations, by and large.
*/

public class TrivialDOM
{
    public static final int TYPE_NODE=1;
    public static final int TYPE_TEXT=2;

    class Node
    {
    	private Node parentNode=null;
    	private String nodeName;
	private Hashtable<String,String> nodeAttr;
	private ArrayList<Object> children;
	
	public Node(String NodeName)
	{
	    nodeName=NodeName;
	    nodeAttr=new Hashtable<String,String>();
	    children=new ArrayList<Object>();
	}
	
	public Node parent() {return parentNode;}
	public void setParent(Node Parent) {parentNode=Parent;}
	
	public String nodeName() {return nodeName;}
	public void setNodeName(String Name) {nodeName=Name;}
	public String attribute(String Attr) {return nodeAttr.containsKey(Attr) ? nodeAttr.get(Attr) : null;}
	public void setAttribute(String Attr,String Value) {nodeAttr.put(Attr,Value);}
	public String[] getAttributeNames() 
	{
	    Set<String> attr=nodeAttr.keySet();
	    String[] names=new String[attr.size()];
	    return attr.toArray(names);
	}
	
	public int numChildren() {return children.size();}
	public int childType(int N) 
	{
	    Object child=children.get(N);
	    if (child instanceof Node) return TYPE_NODE;
	    if (child instanceof Text) return TYPE_TEXT;
	    return 0;
	}
	public Node getChildNode(int N) {return (Node)children.get(N);}
	public Text getChildText(int N) {return (Text)children.get(N);}
	
	public void clear() {children.clear();}
	public void deleteChild(int N) {children.remove(N);}
	
	public void setText(String Txt,boolean Preserve)
	{
	    clear();
	    Text txt=new Text(Txt,Preserve);
	    txt.setParent(this);
	    children.add(txt);
	}
	
	public String getText()
	{
	    String txt="";
	    for (int n=0;n<numChildren();n++)
	    {
	    	if (childType(n)==TYPE_TEXT) txt+=getChildText(n).get();
		else txt+=getChildNode(n).getText();
	    }
	    return txt;
	}
	
	public void appendChild(Node Nod) {Nod.setParent(this); children.add(Nod);}
	public void appendChild(Text Txt) {Txt.setParent(this); children.add(Txt);}
	public void insertChild(int N,Node Nod) {Nod.setParent(this); children.add(N,Nod);}
	public void insertChild(int N,Text Txt) {Txt.setParent(this); children.add(N,Txt);}

    	public Node appendNode(String Name) 
	{
	    Node nod=new Node(Name);
	    nod.setParent(this);
	    children.add(nod); 
	    return nod;
	}
	public Text appendText(String Txt,boolean Preserve) 
	{
	    Text txt=new Text(Txt,Preserve); 
	    txt.setParent(this);
	    children.add(txt); 
	    return txt;
	}
    }
    
    class Text
    {
    	private Node parentNode=null;
    	private String text;
	private boolean preserve; // if true, is CDATA type; otherwise may be freely trimmed for whitespace
    
    	public Text(String Text,boolean Preserve) {text=Text; preserve=Preserve;}

	public Node parent() {return parentNode;}
	public void setParent(Node Parent) {parentNode=Parent;}

	public String get() {return text;}
    	public void set(String Txt) {text=Txt;}
	public boolean preserve() {return preserve;}
    }
    
    public Node createNode(String Name) {return new Node(Name);}
    public Text createText(String Text,boolean Preserve) {return new Text(Text,Preserve);}
    
    Node doc=null;

    // constructors
    
    public TrivialDOM() {}

    public TrivialDOM(String DocName)
    {
    	doc=new Node(DocName);
    }
    public TrivialDOM(Node DocNode)
    {
    	doc=DocNode;
    }
    
    public Node document() {return doc;}
    public String toString()
    {
    	StringWriter out=new StringWriter();
	try {writeXML(out,this);}
	catch (IOException e) {return e.getMessage();}
	return out.toString();
    }

    // parsing input files

    public static TrivialDOM readXML(BufferedReader in) throws IOException
    {
    	final String EOF="ReadXML: unexpected end of file during parsing";

    	// PART 1: read the input file one character at a time, and carve it up into chunks, which are preserved as strings; these
	// include tag start & end, text, CDATA, and comments.
    	
	ArrayList<String> chunks=new ArrayList<String>();
	String str="";
	while (true)
	{
	    int ich;
	    if (str.length()==0)
	    {
	    	ich=in.read();
	    	if (ich<0) break;
	    	str=String.valueOf((char)ich);
	    }
	    
	    if (str.charAt(0)=='<') // either a tag or a CDATA
	    {
	    	for (int n=0;n<2;n++)
		{
	    	    ich=in.read();
		    if (ich<0) throw new IOException(EOF);
		    str=str+(char)ich;
		}
		
		if (str.startsWith("<![")) // it's a CDATA
		{
		    while (true)
		    {
		    	ich=in.read();
			if (ich<0) throw new IOException(EOF);
		    	str=str+(char)ich;
			if (str.endsWith("]]>")) 
			{
			    chunks.add(str);
			    str="";
			    break;
			}
		    }
		}
		else if (str.startsWith("<!-")) // it's a comment
		{
		    while (true)
		    {
		    	ich=in.read();
			if (ich<0) throw new IOException(EOF);
		    	str=str+(char)ich;
			if (str.endsWith("-->")) 
			{
			    chunks.add(str);
			    str="";
			    break;
			}
		    }
		}
		else // it's an opening tag, which will get closed later
		{
		    boolean inquot=false;
		    while (true)
		    {
		    	ich=in.read();
			if (ich<0) throw new IOException(EOF);
		    	str=str+(char)ich;
			if ((char)ich=='"') inquot=!inquot;
			else if ((char)ich=='>') 
			{
			    chunks.add(str);
			    str="";
			    break;
			}
		    }
		}
	    }
	    else // must be plain text
	    {
	    	boolean eof=false;
	    	while (true)
		{
		    ich=in.read();
		    if (ich<0) {eof=true; break;}
		    if ((char)ich=='<')
		    {
		    	chunks.add(str);
			str=String.valueOf((char)ich);
		    	break;
		    }
		    str=str+(char)ich;
		}
		if (eof)
		{
		    if (str.trim().length()==0) break; else throw new IOException(EOF);
		}
	    }
	}

    	// PART 2: analyze the resulting pieces, and build up the node tree

    	TrivialDOM xml=new TrivialDOM("unknown");
    	Node node=null;
    	for (int n=0;n<chunks.size();n++)
	{
	    str=chunks.get(n);
	    if (str.trim().length()==0) continue; // ignore chunks which are pure whitespace

	    if (str.charAt(0)=='<' && str.length()>=2 && ((str.charAt(1)>='A' && str.charAt(1)<='Z') ||
	    	    	    	    	    	          (str.charAt(1)>='a' && str.charAt(1)<='z')) && str.endsWith(">"))
    	    {
	    	str=str.substring(1,str.length()-1);
		boolean isclosed=str.endsWith("/");
		if (isclosed) str=str.substring(0,str.length()-1);
		
		String[] bits=str.split(" ");
		Node newNode=null;
		if (node==null)
		{
		    newNode=xml.document();
		    newNode.setNodeName(bits[0]);
		}
		else newNode=node.appendNode(bits[0]);
		
    	    	for (int i=1;i<bits.length;i++)
		{
		    int spc=bits[i].indexOf("=");
		    if (spc<=0) throw new IOException("Malformatted attribute: ["+snip(bits[i])+"].");
		    String key=bits[i].substring(0,spc),val=bits[i].substring(spc+1);
		    if (!val.startsWith("\"") || !val.endsWith("\""))
		    	throw new IOException("Malformed attribute value: ["+snip(bits[i])+"].");
		    val=val.substring(1,val.length()-1);
		    newNode.setAttribute(key,val);
		}
		
		if (!isclosed) node=newNode;
	    }
	    else if (str.startsWith("</"))
	    {
	    	if (node==null) throw new IOException("Unexpected end tag: ["+snip(str)+"].");
	    	str=str.substring(2,str.length()-1);
		if (str.compareTo(node.nodeName())!=0)
		    throw new IOException("Closing tag does not match opening tag: ["+snip(str)+"].");
		node=node.parent();
	    }
	    else if (str.startsWith("<![CDATA["))
	    {
	    	if (node==null) throw new IOException("Unexpected CDATA node: ["+snip(str)+"].");
	    	if (!str.endsWith("]]>")) throw new IOException("CDATA node not ended: ["+snip(str)+"].");
		str=str.substring(9,str.length()-3);
    	    	node.appendText(str,true);
	    }
	    else if (str.startsWith("<!--"))
	    {
	    	if (!str.endsWith("-->")) throw new IOException("Unterminated comment: ["+snip(str)+"].");
	    }
	    else if (str.startsWith("<?")) {} // ignore
	    else if (str.startsWith("<")) throw new IOException("Unexpected angle bracket, near: ["+snip(str)+"].");
	    else
	    {
	    	if (node==null) throw new IOException("Misplaced text-like block: ["+snip(str)+"].");
		node.appendText(unescapeText(str).trim(),false);
	    }
	}	

    	return xml;
    }
    
    // chop a string off if it's too big to go in an exception
    private static String snip(String str)
    {
    	if (str.length()<60) return str;
	return str.substring(0,60)+"...";
    }
    
    // writing output files

    public static void writeXML(Writer out,TrivialDOM dom) throws IOException
    {
    	out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    	recursiveWriteNode(out,dom.document(),0);
	out.flush();
    }
    
    private static void recursiveWriteNode(Writer out,Node nod,int Level) throws IOException
    {
    	// emit the node tag & attributes
	
    	for (int n=0;n<Level;n++) out.write(" ");
	out.write("<"+nod.nodeName());
	String[] attr=nod.getAttributeNames();
	for (int n=0;n<attr.length;n++) out.write(" "+attr[n]+"=\""+escapeAttr(nod.attribute(attr[n]))+"\"");

	// special case for empty nodes
	if (nod.numChildren()==0) {out.write("/>\n"); return;}

	out.write(">");
	
	boolean doIndent=true;
	if (nod.numChildren()==1 && nod.childType(0)==TYPE_TEXT) doIndent=false;
	else if (nod.numChildren()>0 && nod.childType(0)==TYPE_TEXT && nod.getChildText(0).preserve()) doIndent=false;
	
	if (doIndent) out.write("\n");
	
	// emit the child nodes
	
	for (int n=0;n<nod.numChildren();n++)
	{
	    if (nod.childType(n)==TYPE_TEXT)
	    {
	    	Text txt=nod.getChildText(n);
	    	if (doIndent) for (int i=0;i<Level+1;i++) out.write(" ");
		
		if (txt.preserve())
		    out.write("<![CDATA["+txt.get()+"]]>");
		else
		    out.write(escapeText(txt.get()));
		
		if (doIndent) out.write("\n");
	    }
	    else recursiveWriteNode(out,nod.getChildNode(n),Level+1);
	}
	
	// emit the closing tag
	
	if (doIndent) for (int n=0;n<Level;n++) out.write(" ");
	out.write("</"+nod.nodeName()+">\n");
    }
    
    // miscellaneous
    
    // make sure a string is suitable to encode in an attribute value (quoted)
    public static String escapeAttr(String S)
    {
    	int i;
	while ((i=S.indexOf('"'))>=0) {S=S.substring(0,i)+"&quot;"+S.substring(i+1);}
	while ((i=S.indexOf('\''))>=0) {S=S.substring(0,i)+"&apos;"+S.substring(i+1);}
    	return S;
    }
    // make sure a string is suitable for general XML text
    public static String escapeText(String S)
    {
    	String str="";
    	int i;
	while ((i=S.indexOf('&'))>=0) {str=str+S.substring(0,i)+"&amp;"; S=S.substring(i+1);}
	S=str+S;
	while ((i=S.indexOf('<'))>=0) {S=S.substring(0,i)+"&lt;"+S.substring(i+1);}
	while ((i=S.indexOf('>'))>=0) {S=S.substring(0,i)+"&gt;"+S.substring(i+1);}
	return S;
    }
    // convert any escaped entities back into regular text
    public static String unescapeText(String S)
    {
    	String str="";
	int i=0;
	while (i<S.length())
	{
	    if (i+5<=S.length() && S.substring(i,i+5).equals("&amp;")) {str+="&"; i+=5;}
	    else if (i+4<=S.length() && S.substring(i,i+4).equals("&lt;")) {str+="<"; i+=4;}
	    else if (i+4<=S.length() && S.substring(i,i+4).equals("&gt;")) {str+=">"; i+=4;}
	    else {str+=S.charAt(i); i++;}
	}
	return str;
    }
}
