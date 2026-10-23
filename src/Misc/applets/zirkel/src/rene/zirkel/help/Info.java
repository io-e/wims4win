package rene.zirkel.help;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import rene.gui.*;
import rene.dialogs.*;
import rene.viewer.*;
import rene.zirkel.Zirkel;
import rene.zirkel.ZirkelFrame;

/**
Info class. Reads a file "info.txt" or "de_info.txt" etc. that has
the structure

.subject1 substitute1 substitute2 ...
.related subject subject ...
Header
...

.subject2 ...

and displays the text, starting from header, searching for a subject
or any of its substitutes. The headers of the related subjects are
presented in a choice list. The user can switch to any of it.

There is a history and a back button.

Moroever, there is a search button, that displays the first subject
containing a string and presents all other subjects containing the
string in the choice list.
*/

public class Info extends CloseDialog
	implements ItemListener
{	ExtendedViewer V;
	Button Close,Back;
	public static String Subject="start";
	String Search=null;

	MyChoice L;
	Vector Other=null;
	Vector History=new Vector();
	
	Frame F;

	public Info (Frame f)
	{	this(f,false);
	}
	
	public Info (Frame f, boolean modal)
	{	super(f,Zirkel.name("info.title"),modal);
		F=f;
		V=new ExtendedViewer();
		if (Global.Background!=null) V.setBackground(Global.Background);
		V.setFont(Global.NormalFont);
		setLayout(new BorderLayout());
		Panel north=new MyPanel();
		north.setLayout(new GridLayout(0,2));
		north.add(new MyLabel(Zirkel.name("info.related")));
		L=new MyChoice();
		fill();
		north.add(L);
		add("North",north);
		add("Center",V);
		Panel p=new MyPanel();
		p.add(new ButtonAction(this,Zirkel.name("info.start"),"Start"));
		p.add(new ButtonAction(this,Zirkel.name("info.search"),"Search"));
		p.add(new ButtonAction(this,Zirkel.name("info.back"),"Back"));
		p.add(new MyLabel(""));
		p.add(new ButtonAction(this,Zirkel.name("close","Close"),"Close"));
		add("South",new Panel3D(p));
		// seticon("rene/zirkel/icon.png");
		L.addItemListener(this);
		pack();
		// setLocation(0,70);
		centerOut(f);
		setPosition("info");
		setVisible(true);
	}
	
	public void fill ()
	{	L.removeAll();
		V.setText("");
		V.setVisible(false);
		boolean Found=false,Appending=false;
		Vector Related=null;
		Other=new Vector();
		String pair[]=null,lastpair[]=null;
		String lang=Global.name("language","");
		Vector SearchResults=new Vector();
		String SearchResult="";
		String FoundTopic=null;
		boolean FirstRun=true,FoundHeader=false;

		String Search1=Search;
		if (Search!=null && Search.length()>0)
		{	Search1=Search.substring(0,1).toUpperCase()+Search.substring(1);
		}

		read: while (true)
		{	try
			{	String cp=Global.name("codepage.help","");
				BufferedReader in=null;
				if (cp.equals(""))
					in=new BufferedReader(new InputStreamReader(
						getClass().getResourceAsStream("/rene/zirkel/docs/"+lang+"info.txt")));
				else
				{	try
					{	in=new BufferedReader(new InputStreamReader(
							getClass().getResourceAsStream("/rene/zirkel/docs/"+lang+"info.txt"),cp));
					}
					catch (Exception ex)
					{	in=new BufferedReader(new InputStreamReader(
							getClass().getResourceAsStream("/rene/zirkel/docs/"+lang+"info.txt")));
					}
				}
				boolean newline=false;
				newline: while (true)
				{	String s=in.readLine();
					if (s==null) break newline;
					if (s.startsWith("//")) continue;
					s=clear(s);
					if (!s.startsWith(".") && Search!=null && 
						(s.indexOf(Search)>=0 || s.indexOf(Search1)>=0))
					{	if (lastpair!=null && pair==null &&
							!SearchResult.equals(lastpair[0]))
						{	SearchResults.addElement(lastpair);
							SearchResult=lastpair[0];
							if (FoundTopic==null) FoundTopic=lastpair[0];
						}
					}
					interpret: while (true)
					{	if (!Appending && s.startsWith(".") && 
							!s.startsWith(".related"))
						{	if (!Found)
							{	if (s.startsWith("."+Subject))
								{	Found=true; Appending=true;
									continue newline;
								}
								StringTokenizer t=new StringTokenizer(s);
								while (t.hasMoreElements())
								{	String name=t.nextToken();
									if (name.equals(Subject))
									{	Found=true; Appending=true;
										continue newline;
									}
								}
							}
							pair=new String[2];
							s=s.substring(1);
							int n=s.indexOf(' ');
							if (n>0) s=s.substring(0,n);
							pair[0]=s;
							continue newline;
						}
						if (Appending)
						{	if (s.startsWith(".related"))
							{	s=s.substring(".related".length());
								Related=new Vector();
								StringTokenizer t=new StringTokenizer(s);
								while (t.hasMoreElements())
								{	Related.addElement(t.nextToken());
								}
								continue newline;
							}
							if (s.startsWith("."))
							{	Appending=false; 
								continue interpret;
							}
							if (s.trim().equals(""))
							{	if (!newline)
								{	V.newLine();
									V.appendLine("");
								}
								newline=true;
							}
							else 
							{	newline=false;
								if (s.startsWith(" "))
									V.newLine();
								V.append(s+" ");
							}
						}
						else if (pair!=null && !s.startsWith("."))
						{	pair[1]=s;
							Other.addElement(pair);
							lastpair=pair;
							pair=null;
							if (Search!=null && 
								(s.indexOf(Search)>=0 || s.indexOf(Search1)>=0))
							{	if (!SearchResult.equals(lastpair[0]))
								{	SearchResults.addElement(lastpair);
									SearchResult=lastpair[0];
									if (!FoundHeader) FoundTopic=lastpair[0];
									FoundHeader=true;
								}
							}
						}
						continue newline;
					}
				}
				V.newLine();
				in.close();
			}
			catch (Exception e)
			{	if (!lang.equals(""))
				{	lang=""; continue read;
				}
				else
				{	V.appendLine(
						Zirkel.name("help.error","Could not find the help file!"));
				}
			}
			if (FoundTopic!=null && FirstRun)
			{	Subject=FoundTopic;
				SearchResults=new Vector();
				SearchResult="";
				pair=null; lastpair=null;
				Found=false;
				V.setText("");
				FirstRun=false;
				continue read;
			}
			else break read;
		}

		if (!Found) V.appendLine(Zirkel.name("info.notfound","Topic not found!"));

		if (Search!=null)
		{	if (SearchResults.size()>0) L.add(Zirkel.name("info.searchresults"));
			else L.add(Zirkel.name("info.noresults"));
		}
		else L.add(Zirkel.name("info.select"));

		if (Search==null && Related!=null)
		{	Enumeration e=Related.elements();
			while (e.hasMoreElements())
			{	String topic=(String)e.nextElement();
				Enumeration ev=Other.elements();
				while (ev.hasMoreElements())
				{	String s[]=(String[])ev.nextElement();
					if (s[0].equals(topic)) 
					{	L.add(s[1]); break;
					}
				}
			}
		}
		
		if (Search!=null)
		{	Enumeration e=SearchResults.elements();
			while (e.hasMoreElements())
			{	String s[]=(String[])e.nextElement();
				L.add(s[1]);
			}
		}
		
		History.addElement(Subject);
		V.update();
		V.setVisible(true);
		V.showFirst();
	}
	
	public String clear (String s)
	{	s=s.replace('§',' ');
		s=s.replaceAll("__","");
		return s;
	}

	public void doAction (String o)
	{	if (o.equals("Close"))
		{	notePosition("info");
			super.doAction("Close");
		}
		else if (o.equals("Back"))
		{	int n=History.size();
			if (n<2) return;
			History.removeElementAt(n-1);
			Subject=(String)History.elementAt(n-2);
			History.removeElementAt(n-2);
			fill();
		}
		else if (o.equals("Start"))
		{	Subject="start";
			fill();
		}
		else if (o.equals("Search"))
		{	GetParameter.InputLength=50;
			GetParameter g=new GetParameter(F,
				Zirkel.name("info.title"),
				Zirkel.name("info.search"),
				Zirkel.name("info.search","ok"));
			// g.center(this);
			g.setVisible(true);
			if (!g.aborted())
				Search=g.getResult();
			fill();
			Search=null;
		}
		else super.doAction(o);
	}
	
	public void itemStateChanged (ItemEvent e)
	{	String s=L.getSelectedItem();
		Enumeration ev=Other.elements();
		while (ev.hasMoreElements())
		{	String p[]=(String[])ev.nextElement();
			if (p[1].equals(s))
			{	Subject=p[0];
				fill();
				break;
			}
		}
	}
	
	public void doclose ()
	{	if (F instanceof ZirkelFrame) ((ZirkelFrame)F).removeInfo();
		notePosition("info");
		super.doclose();
	}
}
