/*
 * Created on 04.04.2007
 * Translates the help files of Z.u.L. into HTML code.
 */

package rene.zirkel.help;

import java.io.*;
import java.util.*;

import rene.gui.Global;
import rene.util.*;
import rene.util.sort.*;

/**
 * Class to hold a keyword and a vector of topics,
 * containing the keyword. 
 */
class Keyword implements SortObject
{	public String K;
	public MyVector V;
	public Keyword (String key)
	{	K=key;
		V=new MyVector();
	}
	/**
	 * For the sorting. Ignore case!
	 */
	public int compare (SortObject o) 
	{	Keyword k=(Keyword)o;
		return K.toLowerCase().compareTo(k.K.toLowerCase());
	}
	/**
	 * Add a new topic.
	 * @param s
	 */
	public void addTopic (String s)
	{	if (V.indexOf(s)>=0) return;
		V.addElement(s);
	}
}

/**
 * A class to hold a topic and a long description for it.
 */
class Topic implements SortObject
{	public String T;
	public String D;
	public Topic (String topic, String descr)
	{	T=topic; D=descr;
	}
	public int compare(SortObject o) 
	{	Topic k=(Topic)o;
		return D.toLowerCase().compareTo(k.D.toLowerCase());
	}
}

/**
 * Translator for help texts in Z.u.L. help format to a
 * sequence of HTML pages, a keyword list, and a topic list.
 * You need files header.dat and footer.dat in the current
 * directory (which is the target directory).
 */
public class Translator 
{	BufferedReader in;
	PrintWriter out;
	String line;
	Hashtable Headers; // topics and long descriptions
	Hashtable Keywords; // keywords and occurances in topics
	Hashtable Aliases; // topics equal to other topics
	Hashtable NextTopic; // Next topic of each topic
	
	/**
	 * Translator class, which does the work.
	 * @param source Name of source help file
	 * @param codepage Codepage to read the source file
	 */
	public Translator (String source, String language)
	{	if (language!=null) Locale.setDefault(new Locale(language,""));
		Global.initBundle("rene/zirkel/docs/ZirkelProperties");
		String codepage=Global.name("codepage.help","default");
	
		try
		{	
			// collect topics:
			Headers=new Hashtable();
			Aliases=new Hashtable();
			NextTopic=new Hashtable();
			// open source:
			if (codepage.equals("default")) 
				in=new BufferedReader(new InputStreamReader(new FileInputStream(source)));
			else
				in=new BufferedReader(new InputStreamReader(new FileInputStream(source),codepage));
			// read all topics
			line=in.readLine();
			String topic="";
			while (line!=null)
			{	if (line.startsWith("."))
				{	String thistopic=addHeader(); // add the topic
					if (!topic.equals("") && !thistopic.equals("")) // add as next topic
					{	NextTopic.put(topic,thistopic);
					}
					if (!thistopic.equals(""))
						topic=thistopic;
					continue;
				}
				line=in.readLine();
			}
			in.close();
			
			// print and collect keywords:
			Keywords=new Hashtable();
			// open source:
			if (codepage.equals("default")) 
				in=new BufferedReader(new InputStreamReader(new FileInputStream(source)));
			else
				in=new BufferedReader(new InputStreamReader(new FileInputStream(source),codepage));
			line=in.readLine();
			// write output file for each topic:
			while (line!=null)
			{	if (line.startsWith("."))
				{	writeFile();
					continue;
				}
				line=in.readLine();
			}
			in.close();
			
			// write keyword file:
			writeKeywords();
			
			// write topics file:
			writeTopics();
		}
		catch (Exception e) { e.printStackTrace(); }
	}
	
    // function that implements a java 1.5 style: String0.replace(String1,String2) --> replace(String0,String1,String2)                    
    // thanks to Bernadette Perrin and her java 1.4 ;)                                                                                     
    public static String replace(String source, String pattern, String replace)                                                            
    {                                                                                                                                      
            if (source!=null)                                                                                                              
            {                                                                                                                              
                final int len = pattern.length();                                                                                          
                StringBuffer sb = new StringBuffer();                                                                                      
                int found = -1;                                                                                                            
                int start = 0;                                                                                                             
                while( (found = source.indexOf(pattern, start) ) != -1) {                                                                  
                    sb.append(source.substring(start, found));                                                                             
                    sb.append(replace);                                                                                                    
                    start = found + len;                                                                                                   
                }                                                                                                                          
            sb.append(source.substring(start));                                                                                            
            return sb.toString();                                                                                                          
            }                                                                                                                              
        else return "";                                                                                                                    
    } 	
	
	

	/**
	 * Assuming, we found a topic line ".topic ...", add it to the
	 * topic list
	 * @throws IOException
	 * @return topic name
	 */
	public String addHeader ()
		throws IOException
	{	// extract topic name:
		String filename=line.substring(1);
		int n=filename.indexOf(" ");
		
		// filename (topic) has some aliases
		if (n>0)
		{	StringTokenizer t=new StringTokenizer(filename.substring(n+1));
			filename=filename.substring(0,n);
			while (t.hasMoreTokens())
			{	String alias=t.nextToken();
				Aliases.put(alias,filename);
			}
		}
		
		// skip .related lines:
		line=in.readLine();
		while (line!=null)
		{	if (!line.startsWith(".related")) break;
			line=in.readLine();
		}
		if (line==null) return "";
		
		// denote long description of topic (header)
		// using 1.1 style replace
		String header=replace(line,"__","");
		Headers.put(filename,header);
		while (line!=null)
		{	if (line.startsWith(".")) break;
			line=in.readLine();
		}
		
		return filename;
	}
	
	/**
	 * Write a file for the topic, collecting all the keywords in it
	 * along the way.
	 * @throws IOException
	 */
	public void writeFile ()
		throws IOException
	{	// find the topic name
		String filename=line.substring(1);
		int n=filename.indexOf(" ");
		if (n>0) filename=filename.substring(0,n);
		
		// open the topic HTML file
		out=new PrintWriter(new OutputStreamWriter(
			new FileOutputStream(filename+".html"),"utf-8"));
		
		// write the header for this file
		writeHeader(out);
		
		// skip through the related lines and remember them
		line=in.readLine();
		String related="";
		while (line!=null)
		{	if (!line.startsWith(".related")) break;
			related=related+line.substring(".related".length());
			line=in.readLine();
		}
		if (line==null) return;
		
		// write out the header for the topic in h2 style
		out.println("<h2>"+extractKeywords(line,filename)+"</h2>");
		line=in.readLine();
		
		// go through the paragraphs of the topic
		boolean paragraph=false; // started a paragraph?
		boolean start=true; // first line of paragraph?
		while (line!=null)
		{	if (line.startsWith(".")) break; // end of topic!
			if (line.trim().equals("")) // new paragraph!
			{	if (paragraph) out.println("</p>"); // end paragraph
				start=true; // first line of paragraph!
				while (line!=null && // skip empty lines
					(line.trim().equals("") || line.startsWith("->") 
							|| line.startsWith("//image ")))
				{	if (line.startsWith("//image ")) // paragraph image
					{	String image=line.substring("//image ".length());
						out.println("<p align=\"center\"><img src=\""+image+"\"/></p>");
					}
					line=in.readLine();
				}
				out.println("<p>"); // start new paragraph
				paragraph=true;
				continue; // continue with the paragraph
			}
			else
			{	// if indented text is found, break for a new line
				if (line.startsWith(" ") && !start) out.println("<br />");
				else start=false; // no longer the first line
				// extract the keywords of the line and print the result
				if (line.startsWith("//image+ ")) // paragraph image
				{	String image=line.substring("//image+ ".length());
					out.println("<img align=\"right\" src=\""+image+"\"/>");
				}
				else if (line.startsWith("//image- ")) // paragraph image
				{	String image=line.substring("//image+ ".length());
					out.println("<img align=\"left\" src=\""+image+"\"/>");
				}
				else 
				{	out.println(extractKeywords(line.trim(),filename));
				}
			}
			line=in.readLine();
		}
		if (paragraph) out.println("</p>");
		
		// write out the related links
		out.println("<p>");
		out.print(Global.name("help.related","Related Topics")+": ");
		StringTokenizer st=new StringTokenizer(related," ");
		boolean first=true;
		while (st.hasMoreElements())
		{	String s=st.nextToken();
			String t="";
			try
			{	if (Headers.get(s)!=null)
					t=(String)Headers.get(s);
				else if (Headers.get((String)Aliases.get(s))!=null)
					t=(String)Headers.get((String)Aliases.get(s));
			}
			catch (Exception e)
			{	System.out.println(s+" missing in "+filename);
			}
			if (!first) out.println(", ");
			out.print("<a href=\""+s+".html\">"+t+"</a>");
			first=false;
		}
		out.println();
		out.println("</p>");
		
		// write out next topic
		if (NextTopic.get(filename)!=null)
		{	out.println("<p>");
			out.print(Global.name("help.nexttopic","NextTopic")+": ");
			String nexttopic=(String)(NextTopic.get(filename));
			out.println("<a href=\""+nexttopic+".html\">"+
					(String)(Headers.get(nexttopic))+"</a>");
			out.println("</p>");
		}
		
		// write footer and close
		writeFooter(out);
		out.close();
	}
	
	/**
	 * Extract the keywords from a line of text.
	 * Keywords are marked like __this§keyword__, with § replacing
	 * the blanks (to keep keywords in one line).
	 * @param s Line
	 * @param topic Store keywords in this topic
	 * @return Modified line
	 */
	public String extractKeywords (String s, String topic)
	{	// Do some necessary replacements
		s=s.replace('§',' '); // § is used for blanks in keywords
		s=s.replaceAll(">","&gt;"); // convert > to HTML
		s=s.replaceAll("<","&lt;"); // convert < to HTML
		
		// Go through the keywords
		String t="";
		int n=s.indexOf("__"); // position of keyword
		if (n<0) return s; // none found!
		while (n>=0)
		{	t=t+s.substring(0,n)+"<b>"; // start modified line
			s=s.substring(n+2); // rest of string
			n=s.indexOf("__"); // end of keyword
			if (n<0) return t+s+"</b>"; // keyword does not end!
			String keyword=s.substring(0,n); // get keyword
			addKeyword(keyword,topic); // add it
			t=t+keyword+"</b>"; // add it to output line
			s=s.substring(n+2); // find next keyword!
			n=s.indexOf("__");
		}
		return t+s;
	}
	
	/**
	 * Add a keyword in this topic to the Keywords list
	 * @param keyword
	 * @param topic
	 */
	public void addKeyword (String keyword, String topic)
	{	if (Keywords.get(keyword.toLowerCase())==null)
		{	// create a new keyword
			Keyword k=new Keyword(keyword);
			k.addTopic(topic);
			// put it into the keyword list
			Keywords.put(keyword.toLowerCase(),k);
		}
		else // have it already
		{	Keyword k=(Keyword)Keywords.get(keyword.toLowerCase());
			k.addTopic(topic);
		}
	}
	
	/**
	 * Write out all keyworkds to the keyword HTMl file
	 * @throws IOException
	 */
	public void writeKeywords ()
		throws IOException
	{	// open the output file and write header
		PrintWriter out=new PrintWriter(
			new OutputStreamWriter(new FileOutputStream("keywords.html"),"utf-8"));
		writeHeader(out);
		
		// sort the keywords
		Enumeration e=Keywords.elements(); // enumeration of all keywords
		MyVector All=new MyVector(); // vector to store the keywords
		while (e.hasMoreElements()) // store them
		{	Keyword key=(Keyword)e.nextElement();
			All.addElement(key);
		}
		// copy to an array
		Keyword k[]= new Keyword[All.size()];
		All.copyInto(k);
		// sort
		Sorter.sort(k,All.size());
		
		// write out with starting letter as header
		out.println("<p>");
		char preface=0; // no letter yet
		for (int i=0; i<k.length; i++)
		{	char c=Character.toUpperCase(k[i].K.charAt(0)); // get first letter
			if (c!=preface) // new first letter?
			{	preface=c;
				out.println("</p>");
				out.println("<p><b>"+preface+"</b></p>");
				out.println("<p>");
			}
			String keyword=k[i].K;
			keyword=Character.toUpperCase(keyword.charAt(0))+keyword.substring(1);
			out.print(keyword); // print keyword
			for (int j=0; j<k[i].V.size(); j++) // print topics containing the keyword
			{	String topic=(String)k[i].V.elementAt(j);
				if (j==0) out.print(": ");
				else out.print(", ");
				out.print("<a href=\""+topic+".html\">"+((String)Headers.get(topic))+"</a>");
			}
			out.print("<br />"); // new line for next keyword
		}
		
		// finish and close
		out.println("</p>");
		writeFooter(out);
		out.close();
	}
	
	/**
	 * Write the topics file.
	 * @throws IOException
	 */
	public void writeTopics ()
		throws IOException
	{	// open and write header
		PrintWriter out=new PrintWriter(new OutputStreamWriter(new FileOutputStream(
			"topics.html"),"utf-8"));
		writeHeader(out);
		out.println("<p>");
		
		// walk through all topics and store in vector
		Enumeration e=Headers.keys();
		MyVector All=new MyVector();
		while (e.hasMoreElements())
		{	String key=(String)e.nextElement();
			if (Headers.get(key)!=null)
				All.addElement(new Topic(key,(String)Headers.get(key)));
		}
		
		// stoore in vector and sort
		Topic k[]= new Topic[All.size()];
		All.copyInto(k);
		Sorter.sort(k,All.size());
		
		// write out all topics
		for (int i=0; i<k.length; i++)
		{	out.println("<a href=\""+k[i].T+".html\">"+k[i].D+"</a><br />");
		}
		
		// finish and close
		out.println("</p>");
		writeFooter(out);
		out.close();
	}
	
	/**
	 * Write the header for each file, copied from header.dat
	 * @param out
	 * @throws IOException
	 */
	public void writeHeader (PrintWriter out)
		throws IOException
	{	BufferedReader in=new BufferedReader(
			new InputStreamReader(new FileInputStream("header.dat"),"UTF-8"));
		copy(in,out);
		in.close();
	}
	
	/**
	 * Write the footer for each file, copied from footer.dat
	 * @param out
	 * @throws IOException
	 */
	public void writeFooter (PrintWriter out)
		throws IOException
	{	BufferedReader in=new BufferedReader(
			new InputStreamReader(new FileInputStream("footer.dat"),"UTF-8"));
		copy(in,out);
		in.close();
	}

	/**
	 * Copy in to out till end
	 * @param in
	 * @param out
	 * @throws IOException
	 */
	public void copy (BufferedReader in, PrintWriter out)
		throws IOException
	{	while (true)
		{	String line=in.readLine();
			if (line==null) return;
			out.println(line);
		}
	}
	
	/**
	 * @param args
	 */
	public static void main (String[] args) 
	{	if (args.length!=2)
			System.out.println("usage: java -cp zirkel.jar rene.helptohtmt.Translator source language");
		else
			new Translator(args[0],args[1]);
	}

}