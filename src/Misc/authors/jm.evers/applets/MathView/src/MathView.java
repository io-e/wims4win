/*                                                                                                                                          
*  This program is free software; you can redistribute it and/or modify                                       
*  it under the terms of the GNU General Public License as published by                                       
*  the Free Software Foundation; either version 2 of the License, or                                          
*  (at your option) any later version.                                                                        
*                                                                                                             
*  This program is distributed in the hope that it will be useful,                                            
*  but WITHOUT ANY WARRANTY; without even the implied warranty of                                             
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                                              
*  GNU General Public License for more details.                                                               
*                                                                                                             
*  You should have received a copy of the GNU General Public License                                          
*  along with this program; if not, write to the Free Software                                                
*  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.                                                  
*/     


/*
* This is a slight modification of a JomeInput.java program
* The applet now accepts a few "parms" like:
*
* <param name="bgcolor" value="0xffff96">
* <param name="largefont" value="24">
* <param name="smallfont" value="16">
* <param name="wimsvars" value="x,y,z,n,d">
* <param name="oldreply" value="$wims_reply">
* <param name="my_panel" value="module_title:log[3]():sqrt[5]():and:or:isin:notin:R:Z:N:Q">
* <param name="helptext" value="f(x)=sin(3x)/cos(x^2)->f'(x)="> 
* 
* --> my_panel: a custom button panel ,tunable for the exercise at hand.
* --> wimsvars: a rudimentary form of "rawmath"...
* --> helptext: the exercise description ,some text may be allowed ?
* 
*
* version 0.1 
* 5/2007 
* first rudimentary adaption for wims usage: filtering with javascript
* 
* version 0.2 
* 11/2007 
* --> Introduction of log[g](a) and sqrt[g](a)
* --> Filtering now in poorly written java :)
* --> There still are some unresolved issues with "spaces" being turned into "+" signs by the included jome-code of applet.
*     This is due to the fact that the applet -the way I use it- is not suitable for multiple-answers.(single line)
* --> The function "filter(String s1 , int k)" will change the "helptext" appletparam from ascii-math into "unicode" and reverse.
*     Handling most common cases of false "+" signs. 
* --> More reserved keywords can be included, if needed. [added div,times,notin,isin...etc etc]
* --> The helptext now gets stripped away in this applet, when the function getLinear() is called by javascript.
*     (and will thus not be send as part of the answer)
*
*
* version 0.3 
* 1/2008
* --> Deleted all useless references to MathML,OML...reducing size > 50% !
* --> Introduced <param name="oldreply" value="$wim_reply"> echoing the wims-rejected answer in the textareafield
* --> All (?) flaws of the applet are -hopefully- corrected...it should now be a one-line matheditor.
*     In "Multi-aswers" statements/parts can be "glued" with arrows,and,or 
* --> Domainsyntax allowed: [0:+inf> --> javascript --> lbracket 0 : +inf rangle
* --> Input sqrt[6](123) is also allowed...for "compatibility" with JS checkfiles 
* --> Input log[8](123) is  also allowed...for "compatibility" with JS checkfiles
* --> added reserved word array(1,2) used for a twoline "statement"
* --> multilines: array(first line , array(line two,line three)) etc etc 
* --> restored the "," as special symbol -> n-th root : root(p,n) == sqrt[n](p)
* --> both n-th root syntaxes are read by javascript as: sqrt[n](p) --> send to wims as p^(1/n)
*
* version 0.4
* 3/2008
* --> compatible with java 1.4 ??? new replace function .... String.replace(String1,String2)
* --> vlist(line1,line2,line3....) 
* version 0.5
* 3/2011 correct carretposition if using buttons...updateJomeView() , via filer2() ,does correction anyway
* 2/2012 added ~ as approx symbol \u2248
*/

import fr.ove.openmath.jome.Jome;
import java.applet.Applet;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.PrintStream;

public class MathView extends Applet
    implements ActionListener, ItemListener, TextListener, WindowListener
{
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

    public void setWindow(JFrame frame)
    {
        standaloneWindow = frame;
        inAnApplet = false;
    }

    public void toggleFont()
    {
        Jome jome = j;
        if(jome.getFont() == bigFont)
        {
            jome.setFont(defaultFont);
            linearInput.setFont(defaultFont);
        } else
        {
            jome.setFont(bigFont);
            linearInput.setFont(bigFont);
        }
        invalidate();
        jome.invalidate();
        linearInput.invalidate();
        linearInput.requestFocus();
        repaint();
        jome.repaint();
        linearInput.repaint();
    }

    public static void main(String args[])
    {
        MathView mathview = new MathView();
        JFrame frame = new JFrame();
        mathview.setWindow(frame);
        frame.add(mathview);
        frame.setTitle("WIMS JAVA INPUT");
        frame.addWindowListener(mathview);
        mathview.init();
        frame.pack();
        frame.setSize(640,480);
        frame.setVisible(true);
        frame.validate();
        mathview.start();
        mathview.repaint();
    }

    private String removespecial(String t){
	/// vlist(1,2,3,4) and restanswer -> 1 and 2 and 3 and 4 and restanswer
	int begin=t.indexOf("vlist(");
	if(begin==-1){return t;}
	String org=t;
	int t1;int t2;int tot;int Got;int Wait;int End;char chr;int s;
	String value;String exp;String val;String L;String R;int cnt=0;
	while (begin >= 0){
	    cnt++;if(cnt>1000){return org+" vlist(a,b) syntax error" ;}
	    tot=t.length();Wait=0;Got=0;End=0;
	    for(s=begin+5;s<tot;s++){
		if(End==0){
		    chr=t.charAt(s);
		    if(chr=='('){Wait=Wait+1;}
		    if(chr==')'){Got=Got+1;}
		    if(Got!=0){if(Wait==Got){End=s;}}
		}
	    }
	    value=t.substring(begin+6,End);
	    t1=value.indexOf(",");if(t1<0){return org+" syntax error: comma missing\nvlist(a,b,c,d)";}
	    t2=value.length();
	    val=value.substring(0,t2);
	    if(val.indexOf("vlist(")!=-1){return org + "error :vlist within vlist";}
	    L="vlist("+val+")";R=val;
	    t=replace(t,L,R);
	    //System.out.println("val= " + val +" L= "+L);
	    begin=t.indexOf("vlist(");
	}
	t=replace(t,","," and ");
	return t;
    }
    
    
    private String root2sqrtn(String t){
	int begin=t.indexOf("root(");
	if(begin==-1){return t;}
	String org=t;
	int t1;int t2;int tot;int Got;int Wait;int End;char chr;int s;
	String value;String exp;String val;String L;String R;int cnt=0;
	while (begin >= 0){
	    cnt++;if(cnt>1000){return org+" root(a,b) syntax error" ;}
	    tot=t.length();Wait=0;Got=0;End=0;
	    for(s=begin+4;s<tot;s++){
		if(End==0){
		    chr=t.charAt(s);
		    if(chr=='('){Wait=Wait+1;}
		    if(chr==')'){Got=Got+1;}
		    if(Got!=0){if(Wait==Got){End=s;}}
		}
	    }
	    value=t.substring(begin+5,End);
	    t1=value.indexOf(",");if(t1<0){return org+" syntax error: comma missing\nroot(a,b)";}
	    t2=value.length();
	    val=value.substring(0,t1);exp=value.substring(t1+1,t2);
	    if(val.indexOf("root(")!=-1){return org + "error : absurd root-syntax";}
	    if(exp.indexOf("root(")!=-1){return org + "error : absurd root-syntax";}
	    L="root("+val+","+exp+")";R="sqrt["+exp+"]#"+val+"@";
	    t=replace(t,L,R);
	    //System.out.println("val= " + val + "  exp=  "+ exp+ "  L= "+L+"  R= "+R);
	    begin=t.indexOf("root(");
	}
	t=t.replace('#','(');
	t=t.replace('@',')');
	return t;
    }

    private String sqrtn2root(String t){
	int begin=t.indexOf("sqrt[");
	if(begin==-1){return t;}
	String org=t;
	int tot;int Got;int Wait;int End;char chr;int s;int restart;
	String value;String exp;String val;String L;String R;int cnt=0;
	while (begin >= 0){
	    cnt++;if(cnt>1000){return org+"sqrt[]() syntax error" ;}
	    tot=t.length();Wait=0;Got=0;End=0;
	    for(s=begin+4;s<tot;s++){
		if(End==0){
		    chr=t.charAt(s);
		    if(chr=='['){Wait=Wait+1;}
		    if(chr==']'){Got=Got+1;}
		    if(Got!=0){if(Wait==Got){End=s;}}
		}
	    }
	    restart=End;    
	    exp=t.substring(begin+5,End);
	    Wait=0;Got=0;End=0;
	    for(s=restart;s<tot;s++){
		if(End==0){
		    chr=t.charAt(s);
		    if(chr=='('){Wait=Wait+1;}
		    if(chr==')'){Got=Got+1;}
		    if(Got!=0){if(Wait==Got){End=s;}}
		}
	    }
	    val=t.substring(restart+2,End);
	    R="root#"+val+","+exp+"@";L="sqrt["+exp+"]("+val+")";
	    t=replace(t,L,R);
	    //System.out.println("val= " + val + "  exp=  "+ exp+ "  L= "+L+"  R= "+R);
	    begin=t.indexOf("sqrt[");
	}
	t=t.replace('#','(');
	t=t.replace('@',')');
	return t;
    }

    
    private String filter1(String s1 , int k ){
        int p;String r1;String r2;
	String d1[] = new String[]{"of","et","en","*","cdot","times","div","N","Z","Q","R","\\",
	"f^-1(x)","isin","and","or","notin","-->","=>","inf","->","~",
	"log[2]","log[3]","log[4]","log[5]","log[6]","log[7]","log[8]","log[9]"};
	
	String d2[] = new String[]{"\u22C1","\u22C0","\u22C0","\u2027","\u2027","X","\u00F7","\u2115","\u2124","\u211A","\u211D","\u2216",
	"f\u00AF\u00B9(x)","\u2208","\u22C0","\u22C1","\u2209","rightarrow","\u21D4","\u221E","\u2192","\u2248",
	"\u00B2log","\u00B3log","\u2074log","\u2075log","\u2076log","\u2077log","\u2078log","\u2079log"};
	
	String d3[] = new String[]{" or "," and "," and ","*","*","*","/","N","Z","Q","R","\\",
	"f^-1(x)"," isin "," and "," or "," notin "," -> "," -> "," inf "," -> "," ~ ",
	"log[2]","log[3]","log[4]","log[5]","log[6]","log[7]","log[8]","log[9]"};
	
	int d=d1.length;
	//int e=d1.length;int f=d1.length;if( d!=e || d!=f){System.out.println("ERROR array lengte verschil d= " + d +"\n e= "+ e +"\n f =" +f) ;}
	// d1=applettext from wims
	// d2=unchangable text in math-area from applet
	// d3=the syntax we want to read by javascript: basically spaces/words for readablitity 
	if(k==1){
	    // from ascii applettext-param to unicode applet and visa versa
    	    for ( p=0;p<d;p++){                                                                                                                
        	r1=d1[p];                                                                                                                 
        	r2=d2[p];                                                                                                                 
        	s1=replace(s1,r1,r2);                                                                                                          
    	    }
	    s1=sqrtn2root(s1);
	}
	else
	{
    	    for ( p=0;p<d;p++){                                                                                                                
        	r1=d3[p];                                                                                                                 
        	r2=d2[p];                                                                                                                 
        	s1=replace(s1,r2,r1);                                                                                                          
    	    }
	}
        return s1;                                                                                                                         
    }
    
    private String filter2(String s){
	if(Special_Fraction){s=replace(s," ","+");}// param "special_fraction" inserts a "+" sign -> 1 1/2 == 1+1/2
	String vars=readParams("wimsvars");
	if(vars.length()==0){vars="x";}
	vars=vars.toString();vars=replace(vars,",","");
	StringBuffer S=new StringBuffer(s);int p;int r;char x;char getal;char P1;char P2;char P3;String P4="";
	String nummers="0123456789"+vars;nummers=nummers.toString();int t=vars.length();
	int q=nummers.length();int m;int max=S.length();
	String[] mathopts={"sq","si","co","ta","ab","lo","ln"};int opts=7;String mop="";int a;
	int current = linearInput.getCaretPosition();
	if(max>1){
	    for(r=0;r<t;r++){ 
		x=vars.charAt(r);
		for(p=0;p<q;p++){
		    getal=nummers.charAt(p);
		    for(m=0;m<max-1;m++){
			P1=S.charAt(m);P2=S.charAt(m+1);
			if(P1==getal && P2==x){
			    S=S.insert(m+1,"*");max++;linearInput.setText(S.toString());
			    current++;
			}
		    }
		    max=S.length();
		    if(max>3){
			for(m=0;m<max-2;m++){
			    P1=S.charAt(m);P2=S.charAt(m+1);P3=S.charAt(m+2);
			    if( (P1==' ' && P2==' ' && P3==' ') || (P1==' ' && P2=='e' && P3=='n') || (P1=='e' && P2=='n' && P3==' ') || (P1=='e' && P2=='n' && P3==x) ){
				S=S.delete(m,m+3);S=S.append(" and ");m=m+3;linearInput.setText(S.toString());
			    }
			    else
			    {
				if( (P1==' ' && P2=='o' && P3=='f') || (P1=='o' && P2=='f' && P3==' ') || (P1=='o' && P2=='f' && P3==x) ){
				    S=S.delete(m,m+3);S=S.append(" or ");m=m+2;current++;linearInput.setText(S.toString());
				}
				else
				{
				    // poormans rawmath for "common function"
				    if(P1==getal || P1==x ){
					P4="" + P2 + P3;
					for(a=0;a<opts;a++){
					    mop=mathopts[a];if(P4.equals(mop)){S=S.insert(m+1,"*");a=opts;m=m+4;current++;linearInput.setText(S.toString());}
					}
				    }
				}
			    }
			    max=S.length();
			}
		    }
		}
	    }    
	}
	linearInput.setCaretPosition(current);
	linearInput.requestFocus();
	s=S.toString();
	s=sqrtn2root(s);
	return s;
    }
    
    private String readParams(String s){
	String c=getParameter(s);
	return c;
    }

    private void reportError(String s)
    {
        if(!inAnApplet)
            System.err.println(s);
    }

    private void addPaletteCard(Choice choice, Container container, String s, Container container1)
    {
        choice.add(s);
        container.add(container1, s);
    }

    private JButton addButton(Container container, String s, String s1)
    {
        JButton button = new JButton(s);
        button.setActionCommand(s1);
        button.addActionListener(this);
        container.add(button);
        return button;
    }

    private JButton addButton(Container container, String s, String s1, String s2)
    {
        JButton button = new JButton(s);
        button.setActionCommand(s1);
        button.addActionListener(this);
        container.add(button, s2);
        return button;
    }

    public void start()
    {
        linearInput.setFont(defaultFont);
        linearInput.invalidate();
        j.invalidate();
        repaint();
        linearInput.requestFocus();
	String oldreply=readParams("oldreply");
	if(oldreply.length()!=0){
	    oldreply=replace(oldreply," ","");
	    oldreply=filter1(oldreply,1);
	    linearInput.setText(oldreply.toString());
	}
    }
    
    public void init(){
	try{String helen = readParams("special_fraction");
	    if(helen.equalsIgnoreCase("yes") || helen.equals("1")){Special_Fraction = true;}
	}catch(Exception e){} 
	String special=readParams("my_panel");
	String smallfont=readParams("smallfont");
	String largefont=readParams("largefont");
	int font1;int font2;
	/* check op getallen */
	if(smallfont==null){font1=16;}else{font1 = Integer.parseInt(smallfont);}
	if(largefont==null){font2=24;}else{font2 = Integer.parseInt(largefont);}
        bigFont = new Font("Helvetica", 0, font2);defaultFont = new Font("Helvetica", 0, font1);

	String kleur=readParams("bgcolor");
	Color bgcolor = new Color(0xffffff);// default white
	if(kleur!=null){
	    try{ bgcolor=Color.decode(kleur);}catch(Exception e){System.out.println("do\'t know color "+kleur);}
	}
        setBackground(bgcolor);
        j = new Jome();
        j.setFont(defaultFont);
        j.setDrawBounds(true);
        //j.setBackground(new Color(0xffffdd));
        j.setBackground(bgcolor);	
        BorderLayout borderlayout = new BorderLayout();
        setLayout(new BorderLayout());
	

        controlPane = new JPanel();
	controlPane.setFont(defaultFont);
        controlPane.setLayout(new BorderLayout());
        palette = new JPanel();
        palette.setLayout(new BorderLayout());
	palette.setFont(defaultFont);
        paletteCards = new JPanel();
        paletteCardsLayout = new CardLayout(5, 4);
        paletteCards.setLayout(paletteCardsLayout);
        paletteChoice = new Choice();
        paletteChoice.addItemListener(this);

	// param name="my_panel" value="title : function1 : function1 : function1 : function1 "
	if(special!=null){
	    int maxfun=7;
	    String[] results = special.split(":");
	    JPanel panel01 = new JPanel();
	    panel01.setLayout(new GridLayout());
	    int p;
	    int pos=results.length;
	    String P;
	    String PP="";
	    String titel=results[0];
	    for (p=1;p<pos;p++){
	    P=results[p];
	    // converting wims-module syntax to panel buttons
	    if(P.equals("~")){PP="\u2248";}
	    else
	    if(P.equals("sqrt()")){PP="\u221A";}
	    else
	    if(P.equals("sqrt[2]()")){P="sqrt()";PP="<html><sup>2</sup>\u221A</html>";}
	    else
	    if(P.equals("sqrt[3]()")){PP="<html><sup>3</sup>\u221A</font></html>";}
	    else
	    if(P.equals("sqrt[4]()")){PP="<html><sup>4</sup>\u221A</html>";}
	    else
	    if(P.equals("sqrt[5]()")){PP="<html><sup>5</sup>\u221A</html>";}
	    else
	    if(P.equals("sqrt[6]()")){PP="<html><sup>6</sup>\u221A</html>";}
	    else
	    if(P.equals("sqrt[7]()")){PP="<html><sup>7</sup>\u221A</html>";}
	    else
	    if(P.equals("sqrt[8]()")){PP="<html><sup>8</sup>\u221A</html>";}
	    else
	    if(P.equals("sqrt[9]()")){PP="<html><sup>9</sup>\u221A</html>";}
	    else
	    if(P.equals("sqrt[n]()")){PP="<html><sup>n</sup>\u221A</html>";}
	    else
	    if(P.equals("log[2]()")){P="\u00B2log()";PP="<html><sup>2</sup>log</html>";}
	    else
	    if(P.equals("log[3]()")){P="\u00B3log()";PP="<html><sup>3</sup>log</html>";}
	    else
	    if(P.equals("log[4]()")){P="\u2074log()";PP="<html><sup>4</sup>log</html>";}
	    else
	    if(P.equals("log[5]()")){P="\u2075log()";PP="<html><sup>5</sup>log</html>";}
	    else
	    if(P.equals("log[6]()")){P="\u2076log()";PP="<html><sup>6</sup>log</html>";}
	    else
	    if(P.equals("log[7]()")){P="\u2077log()";PP="<html><sup>7</sup>log</html>";}
	    else
	    if(P.equals("log[8]()")){P="\u2078log()";PP="<html><sup>8</sup>log</html>";}
	    else
	    if(P.equals("log[9]()")){P="\u2079log()";PP="<html><sup>9</sup>log</html>";}
	    else
	    if(P.equals("isin")){P="\u0020in\u0020" ;PP="\u2208 [isin]";}
	    else
	    if(P.equals("and")){P="\u0020and\u0020";PP="\u22C0 [and]";}
	    else
	    if(P.equals("or")){P="\u0020or\u0020";PP="\u22C1 [or]";}
	    else
	    if(P.equals("pi")){P="\u0020pi\u0020";PP="\u03C0";}    
	    else
	    if(P.equals("notin")){P="\u0020notin\u0020";PP="\u2209 [notin]";}    
	    else
	    if(P.equals("->")){P="\u2192";PP="\u2192";}    
	    else
	    if(P.equals("=>")){P="\u2192";PP=P;}    
	    else
	    if(P.equals("<=>")){P="\u21E4";PP=P;}    
	    else
	    if(P.equals("<->")){P="\u21D4";PP=P;}    
	    else
	    if(P.equals("<->")){P="\u21D4";PP=P;}    
	    else
	    if(P.equals("langle")){P="\u0020langle\u0020";PP="<";}    
	    else
	    if(P.equals("rangle")){P="\u0020rangle\u0020";PP=">";}    
	    else
	    if(P.equals("lbracket")){P="\u0020lbracket\u0020";PP="[";}    
	    else
	    if(P.equals("rbracket")){P="\u0020rbracket\u0020";PP="]";}    
	    else
	    if(P.equals("inf")){P="infty";PP="\u221E";}
	    else{PP=P;}
	    addButton(panel01,PP,"insert "+P);
	    }
	    addPaletteCard(paletteChoice, paletteCards , titel , panel01);
	    palette.add(paletteChoice, "North");
	    palette.add(paletteCards, "Center");
	    controlPane.add(palette, "Center");
	    j.setVisible(true);
	    add(j, "Center");
	}

        JPanel panel04 = new JPanel();
        panel04.setLayout(new GridLayout());
        addButton(panel04, "\u22C0 [and]", "insert \u0020and ");
        addButton(panel04, "\u22C1 [or]", "insert \u0020or ");
        addButton(panel04, "\u2208 [in]", "insert \u0020in");
        addButton(panel04, "\u2209 [notin]", "insert \u0020notin");
        addButton(panel04, "\u2192", "insert \u0020\u2192\u0020");	
        addButton(panel04, "\u21D4", "insert \u21D4");
        addButton(panel04, "\u221E [inf]", "insert \u221E");
        addPaletteCard(paletteChoice, paletteCards, "Misc. Symbols", panel04);
        palette.add(paletteChoice, "North");
        palette.add(paletteCards, "Center");
        controlPane.add(palette, "Center");
        j.setVisible(true);
        add(j, "Center");

        JPanel panel05 = new JPanel();
        panel05.setLayout(new GridLayout());
        addButton(panel05, "multiple answers", "insert vlist(answer1,answer2,answer3,answer4)");
        addButton(panel05, "ln", "append ln()");
        addButton(panel05, "|x|", "append |x|");
        addButton(panel05, "f \u00AF\u00B9(x)= [inverse]", "append f\u00AF\u00B9(x)=");
        addPaletteCard(paletteChoice, paletteCards, "Misc.", panel05);
        palette.add(paletteChoice, "North");
        palette.add(paletteCards, "Center");
        controlPane.add(palette, "Center");
        j.setVisible(true);
        add(j, "Center");

        JPanel panel07 = new JPanel();
        panel07.setLayout(new GridLayout());
        addButton(panel07, "log", "append log()");
        addButton(panel07, "<html><sup>2</sup>log</html>", "append \u00B2log()");
        addButton(panel07, "<html><sup>3</sup>log</html>", "append \u00B3log()");
        addButton(panel07, "<html><sup>4</sup>log</html>", "append \u2074log()");
        addButton(panel07, "<html><sup>5</sup>log</html>", "append \u2075log()");
        addButton(panel07, "<html><sup>6</sup>log</html>", "append \u2076log()");
        addButton(panel07, "<html><sup>7</sup>log</html>", "append \u2077log()");
        addButton(panel07, "<html><sup>8</sup>log</html>", "append \u2078log()");
        addButton(panel07, "<html><sup>9</sup>log</html>", "append \u2079log()");
        addPaletteCard(paletteChoice, paletteCards, "Log", panel07);
        palette.add(paletteChoice, "North");
        palette.add(paletteCards, "Center");
        controlPane.add(palette, "Center");
        j.setVisible(true);
        add(j, "Center");

        JPanel panel08 = new JPanel();
        panel08.setLayout(new GridLayout());
        addButton(panel08, "\u221A", "append sqrt()");
        addButton(panel08, "<html><sup>3</sup>\u221A</html>", "append sqrt[3]()");
        addButton(panel08, "<html><sup>4</sup>\u221A</html>", "append sqrt[4]()");
        addButton(panel08, "<html><sup>5</sup>\u221A</html>", "append sqrt[5]()");
        addButton(panel08, "<html><sup>6</sup>\u221A</html>", "append sqrt[6]()");
        addButton(panel08, "<html><sup>7</sup>\u221A</html>", "append sqrt[7]()");
        addButton(panel08, "<html><sup>8</sup>\u221A</html>", "append sqrt[8]()");
        addButton(panel08, "<html><sup>n</sup>\u221A</html>", "append sqrt[n](p)");
        addPaletteCard(paletteChoice, paletteCards, "\u221A", panel08);
        palette.add(paletteChoice, "North");
        palette.add(paletteCards, "Center");
        controlPane.add(palette, "Center");
        j.setVisible(true);
        add(j, "Center");

        JPanel panel09 = new JPanel();
        panel09.setLayout(new GridLayout());
        addButton(panel09, "\u03C0 [pi]", "insert \u0020pi ");
        addButton(panel09, "sin()", "append sin()");
        addButton(panel09, "sin\u00B2()", "append sin()^2");
        addButton(panel09, "cos()", "append cos()");
        addButton(panel09, "cos\u00B2()", "append cos()^2");
        addButton(panel09, "tan()", "append tan()");
        addButton(panel09, "tan\u00B2()", "append tan()^2");
        addPaletteCard(paletteChoice, paletteCards, "Trigonometry", panel09);
        palette.add(paletteChoice, "North");
        palette.add(paletteCards, "Center");
        controlPane.add(palette, "Center");
        j.setVisible(true);
        add(j, "Center");

        JPanel panel10 = new JPanel();
        panel10.setLayout(new GridLayout());
        addButton(panel10, " < : > ", "append \u0020langle\u0020:\u0020rangle");
        addButton(panel10, " [ : > ", "append \u0020lbracket\u0020:\u0020rangle");
        addButton(panel10, " < : ] ", "append \u0020langle\u0020:\u0020rbracket");
        addButton(panel10, " [ : ] ", "append \u0020lbracket\u0020:\u0020rbracket");
        addButton(panel10, "-\u221E [-inf]", "insert -\u221E");
        addButton(panel10, "+\u221E [+inf]", "insert +\u221E");
        addPaletteCard(paletteChoice, paletteCards, "Domains I", panel10);
        palette.add(paletteChoice, "North");
        palette.add(paletteCards, "Center");
        controlPane.add(palette, "Center");
        j.setVisible(true);
        add(j, "Center");

        JPanel panel11 = new JPanel();
        panel11.setLayout(new GridLayout());
        addButton(panel11, "x \u2208 \u2115 \u2216 \u007B a \u003B b \u007D", "insert x\u2208\u2115\u0020\u2216\u0020\u007Ba:b:c\u007D");
        addButton(panel11, "x \u2208 \u2124 \u2216 \u007B a \u003B b \u007D", "insert x\u2208\u2124\u0020\u2216\u0020\u007Ba:b:c\u007D");
        addButton(panel11, "x \u2208 \u211A \u2216 \u007B a \u003B b \u007D", "insert x\u2208\u211A\u0020\u2216\u0020\u007Ba:b:c\u007D");
        addButton(panel11, "x \u2208 \u211D \u2216 \u007B a \u003B b \u007D", "insert x\u2208\u211D\u0020\u2216\u0020\u007Ba:b:c\u007D");
        addPaletteCard(paletteChoice, paletteCards, "Domains II", panel11);
        palette.add(paletteChoice, "North");
        palette.add(paletteCards, "Center");
        controlPane.add(palette, "Center");
        j.setVisible(true);
        add(j, "Center");
        
	JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout());

	JPanel panel4 = new JPanel();
        panel2.add(panel4, "South");
        reportError("OK");
        JPanel panel3 = new JPanel();
        panel2.add(panel3, "West");
	undoButton = addButton(panel3, "\u21A9", "undo");
        redoButton = null;
        addButton(panel3, "\261", "toggle_font", "Center");
        controlPane.add(panel2, "South");
        linearInput = new TextArea("",1,1,TextArea.SCROLLBARS_NONE);
        linearInput.addTextListener(this);
        linearInput.setFont(bigFont);
	linearInput.setBackground(Color.white);
        controlPane.add(linearInput, "North");
        add(controlPane, "South");
        setVisible(true);
        setLinear("");
        linearInput.requestFocus();
    }

    private void setLinear(String s){
        markUndo();		
	linearInput.setText(s);
        updateJomeView();
    }

    private void markUndo()
    {
        if(redoButton != null)
        {
            undoBuffer[undoBufferCursor] = linearInput.getText();
            undoBufferCursor++;
            if(undoBufferCursor >= undoBuffer.length)
                undoBufferCursor = 0;
            if(undoBufferValidSize < undoBuffer.length)
            {
                undoBufferValidSize++;
                undoBufferValidSizeMax = undoBufferValidSize;
            }
            updateUndoRedoButtons();
        } else
        {
            previousLinear = linearInput.getText();
        }
    }

    private void redo()
    {
        if(undoBufferValidSize < undoBufferValidSizeMax)
        {
            undoBufferCursor++;
            undoBufferValidSize++;
            if(undoBufferCursor >= undoBuffer.length)
                undoBufferCursor = 0;
            linearInput.setText(undoBuffer[undoBufferCursor]);
        }
        updateUndoRedoButtons();
        updateJomeView();
    }
    
    private void undo()
    {
        if(redoButton != null)
        {
            if(undoBufferValidSize > 0)
            {
                undoBufferValidSize--;
                undoBufferCursor--;
                if(undoBufferCursor < 0)
                    undoBufferCursor = undoBuffer.length - 1;
                if(undoBuffer[undoBufferCursor] != null)
                    linearInput.setText(undoBuffer[undoBufferCursor]);
            }
            updateUndoRedoButtons();
        } else
        if(previousLinear != null)
        {
            String s = linearInput.getText();
            linearInput.setText(previousLinear);
            previousLinear = s;
        }
        updateJomeView();
    }

    private void updateUndoRedoButtons()
    {
        undoButton.setEnabled(undoBufferValidSize > 0);
        redoButton.setEnabled(undoBufferValidSize < undoBufferValidSizeMax);
    }

    private void updateJomeView()
    {
	String s=linearInput.getText().toString();
	s=filter2(s);
	String applettext=readParams("helptext");
	if(applettext!=null || applettext.length()>1){
	    applettext=filter1(applettext,1);
	    s=applettext+s;
	    s=s.toString();
	}
        j.setLinear(s);
    }
    
    public MathView()
    {
        standaloneWindow = null;
        inAnApplet = true;
        previousLinear = null;
        undoBuffer = new String[50];
        undoBufferCursor = 0;
        undoBufferValidSize = 0;
        undoBufferValidSizeMax = 0;
        bigFont = new Font("Helvetica", 0, 32);
        defaultFont = new Font("Helvetica", 0, 18);
    }

    public void actionPerformed(ActionEvent actionevent)
    {
	boolean pos=false;
        String s = actionevent.getActionCommand();
        if(s.startsWith("append "))
        {
            markUndo();
            linearInput.setText(linearInput.getText() + s.substring("append ".length()));
	    pos=true;
        } else

        if(s.startsWith("insert "))
        {
            markUndo();
            String s1 = linearInput.getText();
            int i = linearInput.getCaretPosition();
            linearInput.setText(s1.substring(0, i) + s.substring("insert ".length()) + s1.substring(i));
	    pos=true;
        } else
        if(s.startsWith("enclose "))
        {
            markUndo();
            String s2 = linearInput.getText();
            String s3 = s.substring("append ".length(), s.length() - 1);
            String s4 = s.substring(s.length() - 1);
            int k = linearInput.getSelectionStart();
            int l = linearInput.getSelectionEnd();
            linearInput.setText(s2.substring(0, k) + s3 + linearInput.getSelectedText() + s4 + s2.substring(l));
	    pos=true;
        } else
        if(s.equals("undo"))
            undo();
        else
        if(s.equals("redo"))
            redo();
        else
        if(s.equals("toggle_font"))
        {
            toggleFont();
        } else
        {
            updateJomeView();
        }
	// 3/2011 correct carretposition if using buttons...updateJomeView() , via filer2() ,does correction anyway
	if(pos){
	    linearInput.setCaretPosition((linearInput.getText()).length());
	    linearInput.requestFocus();
	}
    }

    public void itemStateChanged(ItemEvent itemevent)
    {
        paletteCardsLayout.show(paletteCards, (String)itemevent.getItem());
        linearInput.requestFocus();
    }

    public void textValueChanged(TextEvent textevent)
    {
        updateJomeView();
    }

    public void windowOpened(WindowEvent windowevent)
    {
    }

    public void windowIconified(WindowEvent windowevent)
    {
    }

    public void windowDeiconified(WindowEvent windowevent)
    {
    }

    public void windowActivated(WindowEvent windowevent)
    {
    }

    public void windowDeactivated(WindowEvent windowevent)
    {
    }

    public void windowResized(WindowEvent windowevent)
    {
    }

    public void windowClosing(WindowEvent windowevent)
    {
        stop();
	destroy();
        if(!inAnApplet)
        {
            standaloneWindow.setVisible(false);
            standaloneWindow.dispose();
        }
    }


    public void windowClosed(WindowEvent windowevent)
    {
        System.exit(0);
    }

    public String getLinear(){
	String S=j.getLinear();	int lu;
    	String applettext=readParams("helptext");
	if(applettext!=null){
	    applettext=filter1(applettext , 1);
	    // cut helptext/applettext from the reply string
	    int li=applettext.length();
	    if(li > 0){
		lu=S.length();
		S=S.substring(li,lu);
	    }
	}
	// back to ascii wims/javascript processable syntax
	S=filter1(S, 0);
	S=root2sqrtn(S);
	S=removespecial(S);
	return S;
    }
	
    Jome j;
    JFrame standaloneWindow;
    JPanel controlPane;
    JPanel palette;
    JPanel paletteCards;
    CardLayout paletteCardsLayout;
    Choice paletteChoice;
    TextArea linearInput;
    JButton undoButton;
    JButton redoButton;
    boolean inAnApplet;
    String previousLinear;
    String undoBuffer[];
    int undoBufferCursor;
    int undoBufferValidSize;
    int undoBufferValidSizeMax;
    Font bigFont;
    Font defaultFont;
    boolean Special_Fraction=false; // param Special_Fraction inserts a "+" sign -> 1 1/2 == 1+1/2
}
