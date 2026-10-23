/*    Sketch Elements: Chemistry molecular diagram drawing tool.
    
    (c) 2007 Dr. Alex M. Clark
    
    Released as GNUware, under the Gnu Public License (GPL)
    
    See www.gnu.org for details.
*/

package WIMSchem;

import WIMSchem.ds.*;

import java.io.*;
import java.awt.*;
import java.util.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import javax.swing.*;

/*
    Encapsulates the editing panel, provides menus and toolbars, and responds to various types
    of events.
    
    few other miscellaneous useful tasks. Is generally intended for using in conjunction with various computational or presentation
    packages which do not themselves necessarily have a good editor for molecular connection graphs.
*/


public class MainPanel extends JPanel implements ActionListener, MouseListener, WindowListener, KeyListener, ClipboardOwner, 
    	    	    	    	    	    	  TemplSelectListener, MolSelectListener
{
    public static final String LICENSE= // (encoded in a string so that it appears in the final .jar file)
	"This program is free software; you can redistribute it and/or modify\n"+
	"it under the terms of the GNU General Public License as published by\n"+
	"the Free Software Foundation; either version 2 of the License, or\n"+
	"(at your option) any later version.\n\n"+
	"This program is distributed in the hope that it will be useful,\n"+
	"but WITHOUT ANY WARRANTY; without even the implied warranty of\n"+
	"MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n"+
	"GNU General Public License for more details.\n\n"+
	"You should have received a copy of the GNU General Public License\n"+
	"along with this program; if not, write to the Free Software\n"+
	"Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA\n\n"+
	"or see http://www.gnu.org for details.";

    public static final String VERSION="SketchEl 1.21 WIMSversion 0.2 SVG export";
    private JFrame frameParent;
    public ImageIcon mainIcon=null,mainLogo=null;
    private static final int TOOL_CURSOR=0;
    private static final int TOOL_ROTATOR=1;
    private static final int TOOL_ERASOR=2;
    private static final int TOOL_DIALOG=3;
    private static final int TOOL_EDIT=4;
    private static final int TOOL_SETATOM=5;
    private static final int TOOL_SINGLE=6;
    private static final int TOOL_DOUBLE=7;
    private static final int TOOL_TRIPLE=8;
    private static final int TOOL_ZERO=9;
    private static final int TOOL_INCLINED=10;
    private static final int TOOL_DECLINED=11;
    private static final int TOOL_UNKNOWN=12;
    private static final int TOOL_CHARGE=13;
    private static final int TOOL_UNDO=14;
    private static final int TOOL_REDO=15;
    private static final int TOOL_TEMPLATE=16;
    private static final int TOOL_CUT=17;
    private static final int TOOL_COPY=18;
    private static final int TOOL_PASTE=19;
    private static final int TOOL_SELECT=20;
    private static final int TOOL_UNSELECT=21;
    public static int 	     TOOL_COUNT=22;
    public String myAtoms[];
    public boolean ATOM_BUTTONS = false;
    private Properties translation;
    public int MENU_COUNT=8;
    private static final String[] IMAGE_TOOL={"Cursor","Rotator","Erasor","EDialog","AEdit",
    "ASelect","BSingle","BDouble","BTriple","BZero",
    "BInclined","BDeclined","BUnknown","ACharge","Undo",
    "Redo","Template","ECut","ECopy","EPaste","Select","UnSelect","BUnknown","BUnknown"};
    public static boolean[] TOOL_SELECTION={true,true,true,false,true,true,true,true,true,true,
    	    	    	    	    	true,true,true,true,false,false,true,false,false,false,false,false};
    public static boolean[] MENU_SELECTION={true,true,true,true,true,true,true,true};
    private AbstractButton[] atomlistButtons;
    private ButtonGroup atomlistGroup;
    private AbstractButton[] toolButtons;
    private ButtonGroup toolGroup;
    private ImageIcon[] toolIcons;
    public EditorPane editor;
    private Templates templ;
    private JCheckBoxMenuItem chkShowHydr,chkShowSter;
    private DraggableMolecule dragMol=null;
    private boolean firstResize=true;
    private String filename=null;
    private String lastElement=null,typedElement="";
    private Molecule lastTemplate=null;
    private int templateIdx=-1;
    public static boolean appletMode=false;
    private boolean streamMode=false,slaveMode=false;
    private boolean useLocalClipboard=false;
    private String appletClipboard="";
    private SaveListener saver=null;
    public final static int MODE_NORMAL=0; // usual invocation, with a frame, and a current file
    public final static int MODE_STREAM=1; // molecule flow-through editing, from stdin to stdout
    public final static int MODE_APPLET=2; // embedded applet version, with no frame
    public final static int MODE_SLAVE=3; // non-file version, editing a transient datastructure

    public MainPanel(String LoadFN,int Mode,JFrame FrameParent) 
    {
	streamMode=Mode==MODE_STREAM;
	appletMode=Mode==MODE_APPLET;
	slaveMode=Mode==MODE_SLAVE;
	frameParent=FrameParent;
	if(appletMode){
	 translation = loadProperties((MainApplet.language).toString());
	 TOOL_SELECTION = MainApplet.TOOL_SELECTION;
	 TOOL_COUNT = TOOL_SELECTION.length;
	 MENU_SELECTION = MainApplet.MENU_SELECTION;
	 MENU_COUNT = MENU_SELECTION.length;
	 myAtoms = MainApplet.myAtoms;
	 ATOM_BUTTONS = MainApplet.ATOM_BUTTONS;
	 mainIcon=new ImageIcon(getClass().getResource("/images/wims_icon.gif"));
	 mainLogo=new ImageIcon(getClass().getResource("/images/wims_logo.gif"));
	}
	else
	{
	    for(int i = 0 ;i < TOOL_COUNT ; i++){ TOOL_SELECTION[i] = true; }
	    mainIcon=new ImageIcon(getClass().getResource("/images/MainIcon.png"));
	    mainLogo=new ImageIcon(getClass().getResource("/images/MainLogo.png"));
	    translation = loadProperties("en");
	}
	if(myAtoms == null ){ myAtoms = new String[] {"C","N","O","H","F","Cl","Br","I","S","P","Na","K","Mg","Ca","Fe","Si"}; }
	useLocalClipboard=appletMode; // applet mode always uses "local clipboard"; if the applet is signed, this will need to switch
	    	    	    	      // to false if permission has been granted to use the system clipboard
	



	templ=new Templates(getClass());

	/* list of atoms in row , like ketcher */
	int num_atoms = myAtoms.length;
	JToolBar atomlist = new JToolBar(JToolBar.HORIZONTAL);
	atomlist.setFloatable(true);
	atomlistButtons = new AbstractButton[num_atoms];
	atomlistGroup = new ButtonGroup();
	Font f = new Font("Dialog", Font.PLAIN, 24);
	atomlist.setFont(f);
	
	/* show a button bar with atoms if the main tool was activated via params*/
	if( ATOM_BUTTONS && appletMode && TOOL_SELECTION[TOOL_SETATOM] ){
	    for(int n=0 ; n < num_atoms ;n++ ){
		atomlistButtons[n]=new JButton(myAtoms[n]); 
	        atomlistGroup.add(atomlistButtons[n]);
		atomlist.add(atomlistButtons[n]);
	        atomlistButtons[n].addActionListener(this);
	        atomlistButtons[n].setToolTipText("element"+myAtoms[n]);    
		atomlistButtons[n].setBorderPainted(false);
	    }
	}
	

	// toolbar
	
	JToolBar tools = new JToolBar(JToolBar.VERTICAL);
	tools.setFloatable(true);
	toolButtons=new AbstractButton[TOOL_COUNT];
	toolIcons=new ImageIcon[TOOL_COUNT];
	toolGroup=new ButtonGroup();
	for (int n=0;n<TOOL_COUNT;n++){
	  if(TOOL_SELECTION[n]){
	   toolIcons[n]=new ImageIcon(getClass().getResource("/images/"+IMAGE_TOOL[n]+".png"));
	   toolButtons[n]=new ToolButton(toolIcons[n]);
	   toolGroup.add(toolButtons[n]);
	   tools.add(toolButtons[n]);
	   toolButtons[n].addActionListener(this);
	   toolButtons[n].setToolTipText(getToolTips(n));
	 }
	}
	if( TOOL_SELECTION[TOOL_CURSOR] ){ toolGroup.setSelected(toolButtons[TOOL_CURSOR].getModel(),true);}
	if( TOOL_SELECTION[TOOL_SETATOM] ){ toolButtons[TOOL_SETATOM].addMouseListener(this);toolButtons[TOOL_SETATOM].addKeyListener(this);selectElement("C");}
	if( TOOL_SELECTION[TOOL_TEMPLATE] ){ toolButtons[TOOL_TEMPLATE].addMouseListener(this);}
	    
	// menu
	
	JMenuBar menubar=appletMode ? menuBarApplet() : menuBarApplication();

	// molecule
	
    	editor=new EditorPane();
	editor.setMolSelectListener(this);
	editor.enableDnD();
	JScrollPane scroll=new JScrollPane(editor);

    	// overall layout

    	if (!appletMode)
	{
	    setLayout(new BorderLayout());
	    add(scroll,BorderLayout.CENTER);
    	    add(menubar,BorderLayout.NORTH);
    	    add(tools,BorderLayout.WEST);
	}
	else
	{
    	    // (!! add a little icon somewhere which does the About box...)

	    JPanel p=new JPanel();
	    p.setLayout(new BorderLayout());
	    p.add(menubar,BorderLayout.NORTH);
	    
	    setLayout(new BorderLayout());
	    add(tools,BorderLayout.WEST);
	    if( ATOM_BUTTONS ){ add(atomlist,BorderLayout.SOUTH); }
	    add(scroll,BorderLayout.EAST);
	    add(p,BorderLayout.CENTER);
	    p.add(scroll,BorderLayout.CENTER);
	}
	
	editor.grabFocus();
	
	editor.setToolCursor();
	
	if (LoadFN!=null)
	{
	    try
	    {
		FileInputStream istr=new FileInputStream(LoadFN);
		Molecule frag=MoleculeStream.readUnknown(istr);
		editor.addArbitraryFragment(frag);
		istr.close();
	    }
	    catch (IOException e) 
	    {
		JOptionPane.showMessageDialog(null,e.toString(),"Open Failed",JOptionPane.ERROR_MESSAGE);
		return;
	    }

	    setFilename(LoadFN);
	    editor.notifySaved();
	}
	if (streamMode) readStream();
	
	addKeyListener(this);
	editor.addKeyListener(this);
	if (frameParent!=null) frameParent.addWindowListener(this);

    	if (!appletMode)
	{	
	    dragMol=new DraggableMolecule(editor);
	    menubar.add(dragMol);
	}
    }
    
    // if specified, this interface will hijack all user efforts to "Save" to the source file
    public void setSaveListener(SaveListener saver) {this.saver=saver;}
    
    // builds and returns a menu bar suitable for the application-style invocation
    private JMenuBar menuBarApplication()
    {
	JMenuBar menubar=new JMenuBar();
		
    	JMenu menufile=new JMenu(translation.getProperty("File"));
	menufile.setMnemonic(KeyEvent.VK_F);
	menufile.add(menuItem(translation.getProperty("New"),KeyEvent.VK_N,null,KeyStroke.getKeyStroke('N',InputEvent.CTRL_MASK)));
	menufile.add(menuItem(translation.getProperty("New_Window"),KeyEvent.VK_W,null,KeyStroke.getKeyStroke('N',InputEvent.CTRL_MASK+InputEvent.SHIFT_MASK)));
	if (DataWindow.ALLOW) // (temporary)
	    menufile.add(menuItem(translation.getProperty("New_DataSheet"),KeyEvent.VK_D,null));
	menufile.add(menuItem(translation.getProperty("Open"),KeyEvent.VK_O,null,KeyStroke.getKeyStroke('O',InputEvent.CTRL_MASK)));
	if (!streamMode) menufile.add(menuItem(translation.getProperty("Save"),KeyEvent.VK_S,null,KeyStroke.getKeyStroke('S',InputEvent.CTRL_MASK)));
	menufile.add(menuItem(translation.getProperty("Save_As"),KeyEvent.VK_A));
	JMenu menuexport=new JMenu(translation.getProperty("Export"));
	menuexport.setMnemonic(KeyEvent.VK_X);
	menuexport.add(menuItem(translation.getProperty("as_MDL_MOL"),KeyEvent.VK_M,null,KeyStroke.getKeyStroke('M',InputEvent.CTRL_MASK+InputEvent.SHIFT_MASK)));
	menuexport.add(menuItem(translation.getProperty("as_CML_XML"),KeyEvent.VK_X,null,KeyStroke.getKeyStroke('X',InputEvent.CTRL_MASK+InputEvent.SHIFT_MASK)));
	menuexport.add(menuItem(translation.getProperty("as_SVG"),KeyEvent.VK_S,null,KeyStroke.getKeyStroke('S',InputEvent.CTRL_MASK+InputEvent.SHIFT_MASK)));
	menufile.add(menuexport);

	menufile.addSeparator();
	if (streamMode) menufile.add(menuItem(translation.getProperty("Save_and_Quit"),KeyEvent.VK_Q,null,KeyStroke.getKeyStroke('Q',InputEvent.CTRL_MASK)));
	else if (slaveMode) menufile.add(menuItem(translation.getProperty("Close"),KeyEvent.VK_C,null,KeyStroke.getKeyStroke('W',InputEvent.CTRL_MASK)));
	else menufile.add(menuItem(translation.getProperty("Quit"),KeyEvent.VK_Q,null,KeyStroke.getKeyStroke('Q',InputEvent.CTRL_MASK)));
	
	JMenu menuedit=new JMenu(translation.getProperty("Edit"));
	menuedit.setMnemonic(KeyEvent.VK_E);
	menuedit.add(menuItem(translation.getProperty("Editttt"),KeyEvent.VK_E,toolIcons[TOOL_DIALOG],KeyStroke.getKeyStroke(' ',InputEvent.CTRL_MASK)));
	menuedit.add(menuItem(translation.getProperty("Undo"),KeyEvent.VK_U,toolIcons[TOOL_UNDO],KeyStroke.getKeyStroke('Z',InputEvent.CTRL_MASK)));
	menuedit.add(menuItem(translation.getProperty("Redo"),KeyEvent.VK_R,toolIcons[TOOL_REDO],
	    	    	    	    	    	    	KeyStroke.getKeyStroke('Z',InputEvent.CTRL_MASK+InputEvent.SHIFT_MASK)));
    
    
    	menuedit.add(menuItem(translation.getProperty("Cut"),KeyEvent.VK_X,toolIcons[TOOL_CUT],KeyStroke.getKeyStroke('X',InputEvent.CTRL_MASK)));
    	menuedit.add(menuItem(translation.getProperty("Copy"),KeyEvent.VK_C,toolIcons[TOOL_COPY],KeyStroke.getKeyStroke('C',InputEvent.CTRL_MASK)));
	menuedit.add(menuItem(translation.getProperty("Copy_SVG"),KeyEvent.VK_V,null,KeyStroke.getKeyStroke('C',InputEvent.CTRL_MASK+InputEvent.SHIFT_MASK)));
    	menuedit.add(menuItem(translation.getProperty("Paste"),KeyEvent.VK_V,toolIcons[TOOL_PASTE],KeyStroke.getKeyStroke('V',InputEvent.CTRL_MASK)));
	menuedit.addSeparator();
	menuedit.add(menuItem(translation.getProperty("Select_All"),KeyEvent.VK_S,null,KeyStroke.getKeyStroke('A',InputEvent.CTRL_MASK)));
	menuedit.add(menuItem(translation.getProperty("Next_Atom"),KeyEvent.VK_N,null,KeyStroke.getKeyStroke('E',InputEvent.CTRL_MASK)));
	menuedit.add(menuItem(translation.getProperty("Previous_Atom"),KeyEvent.VK_P,null,KeyStroke.getKeyStroke('E',InputEvent.CTRL_MASK+InputEvent.SHIFT_MASK)));
	menuedit.add(menuItem(translation.getProperty("Next_Group"),KeyEvent.VK_G,null,KeyStroke.getKeyStroke('G',InputEvent.CTRL_MASK)));
	menuedit.add(menuItem(translation.getProperty("Previous_Group"),KeyEvent.VK_R,null,KeyStroke.getKeyStroke('G',InputEvent.CTRL_MASK+InputEvent.SHIFT_MASK)));
	menuedit.addSeparator();
	menuedit.add(menuItem(translation.getProperty("Flip_Horizontal"),KeyEvent.VK_H,null,null));
	menuedit.add(menuItem(translation.getProperty("Flip_Vertical"),KeyEvent.VK_V,null,null));
	menuedit.add(menuItem(translation.getProperty("Rotate_P45"),KeyEvent.VK_4,null,null));
	menuedit.add(menuItem(translation.getProperty("Rotate_M45"),KeyEvent.VK_5,null,null));
	menuedit.add(menuItem(translation.getProperty("Rotate_P90"),KeyEvent.VK_9,null,null));
	menuedit.add(menuItem(translation.getProperty("Rotate_M90"),KeyEvent.VK_0,null,null));
	menuedit.addSeparator();
    	menuedit.add(menuItem(translation.getProperty("Add_Temporary_Template"),KeyEvent.VK_T,null,null));
	menuedit.add(menuItem(translation.getProperty("Normalise_Bond_Lengths"),KeyEvent.VK_N,null,null));
    	
	JMenu menuview=new JMenu(translation.getProperty("View"));
	menuview.setMnemonic(KeyEvent.VK_V);
	menuview.add(menuItem(translation.getProperty("Zoom_Full"),KeyEvent.VK_F,null,KeyStroke.getKeyStroke('0',InputEvent.CTRL_MASK)));
	menuview.add(menuItem(translation.getProperty("Zoom_In"),KeyEvent.VK_I,null,KeyStroke.getKeyStroke('=',InputEvent.CTRL_MASK)));
	menuview.add(menuItem(translation.getProperty("Zoom_Out"),KeyEvent.VK_O,null,KeyStroke.getKeyStroke('-',InputEvent.CTRL_MASK)));
	menuview.addSeparator();
	ButtonGroup showBG=new ButtonGroup();
	menuview.add(radioMenuItem(translation.getProperty("Show_Elements"),KeyEvent.VK_E,true,showBG));
	menuview.add(radioMenuItem(translation.getProperty("Show_All_Elements"),KeyEvent.VK_A,false,showBG));
	menuview.add(radioMenuItem(translation.getProperty("Show_Indices"),KeyEvent.VK_I,false,showBG));
	menuview.add(radioMenuItem(translation.getProperty("Show_Ring_ID"),KeyEvent.VK_R,false,showBG));
    	menuview.add(radioMenuItem(translation.getProperty("Show_CIP_Priority"),KeyEvent.VK_C,false,showBG));
	menuview.add(radioMenuItem(translation.getProperty("Show_Mapping_Number"),KeyEvent.VK_M,false,showBG));
	
	JMenu menutool=new JMenu(translation.getProperty("Tool"));
	menutool.setMnemonic(KeyEvent.VK_T);
	menutool.add(menuItem(translation.getProperty("Cursor"),KeyEvent.VK_C,toolIcons[TOOL_CURSOR],KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0)));
	menutool.add(menuItem(translation.getProperty("Rotator"),KeyEvent.VK_R,toolIcons[TOOL_ROTATOR],KeyStroke.getKeyStroke('R',InputEvent.CTRL_MASK)));
	menutool.add(menuItem(translation.getProperty("Erasor"),KeyEvent.VK_E,toolIcons[TOOL_ERASOR],KeyStroke.getKeyStroke('D',InputEvent.CTRL_MASK)));
    	menutool.add(menuItem(translation.getProperty("Edit_Atom"),KeyEvent.VK_A,toolIcons[TOOL_EDIT],KeyStroke.getKeyStroke(',',InputEvent.CTRL_MASK)));
    	menutool.add(menuItem(translation.getProperty("Set_Atom"),KeyEvent.VK_S,new ImageIcon(getClass().getResource("/images/ASelMenu.png")),
	    	    	    	    	    	    	    	    	    	KeyStroke.getKeyStroke('.',InputEvent.CTRL_MASK)));
	menutool.add(menuItem(translation.getProperty("Single_Bond"),KeyEvent.VK_1,toolIcons[TOOL_SINGLE],KeyStroke.getKeyStroke('1',InputEvent.CTRL_MASK)));
	menutool.add(menuItem(translation.getProperty("Double_Bond"),KeyEvent.VK_2,toolIcons[TOOL_DOUBLE],KeyStroke.getKeyStroke('2',InputEvent.CTRL_MASK)));
	menutool.add(menuItem(translation.getProperty("Triple_Bond"),KeyEvent.VK_3,toolIcons[TOOL_TRIPLE],KeyStroke.getKeyStroke('3',InputEvent.CTRL_MASK)));
	menutool.add(menuItem(translation.getProperty("Zero_Bond"),KeyEvent.VK_0,toolIcons[TOOL_ZERO],KeyStroke.getKeyStroke('0',InputEvent.CTRL_MASK)));
	menutool.add(menuItem(translation.getProperty("Inclined_Bond"),KeyEvent.VK_I,toolIcons[TOOL_INCLINED]));
	menutool.add(menuItem(translation.getProperty("Declined_Bond"),KeyEvent.VK_D,toolIcons[TOOL_DECLINED]));
	menutool.add(menuItem(translation.getProperty("Unknown_Bond"),KeyEvent.VK_U,toolIcons[TOOL_UNKNOWN]));
	menutool.add(menuItem(translation.getProperty("Charge"),KeyEvent.VK_H,toolIcons[TOOL_CHARGE],KeyStroke.getKeyStroke('H',InputEvent.CTRL_MASK)));
	menutool.add(menuItem(translation.getProperty("Template_Tool"),KeyEvent.VK_T,toolIcons[TOOL_TEMPLATE],KeyStroke.getKeyStroke('T',InputEvent.CTRL_MASK)));
	menutool.add(menuItem(translation.getProperty("Select_Template"),KeyEvent.VK_T,toolIcons[TOOL_TEMPLATE],
	    	    	    	    	    	    	KeyStroke.getKeyStroke('T',InputEvent.CTRL_MASK+InputEvent.SHIFT_MASK)));
	JMenu menuhydr=new JMenu(translation.getProperty("Hydrogen"));
	menuhydr.setMnemonic(KeyEvent.VK_Y);
	chkShowHydr=new JCheckBoxMenuItem(translation.getProperty("Show_Hydrogen"));
	chkShowHydr.setMnemonic(KeyEvent.VK_Y);
	chkShowHydr.setSelected(true);
	chkShowHydr.addActionListener(this);
	menuhydr.add(chkShowHydr);
	menuhydr.add(menuItem(translation.getProperty("Set_Explicit"),KeyEvent.VK_E));
	menuhydr.add(menuItem(translation.getProperty("Clear_Explicit"),KeyEvent.VK_X));
	menuhydr.add(menuItem(translation.getProperty("Zero_Explicit"),KeyEvent.VK_Z));
	menuhydr.add(menuItem(translation.getProperty("Create_Actual"),KeyEvent.VK_C));
	menuhydr.add(menuItem(translation.getProperty("Delete_Actual"),KeyEvent.VK_D));

	JMenu menuster=new JMenu(translation.getProperty("Stereochemistry"));
	menuster.setMnemonic(KeyEvent.VK_S);
	chkShowSter=new JCheckBoxMenuItem(translation.getProperty("Show_Stereolabels"));
	chkShowSter.setMnemonic(KeyEvent.VK_L);
	chkShowSter.setSelected(false);
	chkShowSter.addActionListener(this);
	menuster.add(chkShowSter);
	menuster.add(menuItem(translation.getProperty("Invert_Stereochemistry"),KeyEvent.VK_I));
	menuster.add(menuItem(translation.getProperty("Set_R/Z"),KeyEvent.VK_R));
	menuster.add(menuItem(translation.getProperty("Set_S/E"),KeyEvent.VK_S));
	menuster.add(menuItem(translation.getProperty("Cycle_Wedges"),KeyEvent.VK_C));
	menuster.add(menuItem(translation.getProperty("Remove_Wedges"),KeyEvent.VK_W));

    	JMenu menuhelp=new JMenu(translation.getProperty("Help"));
	menuhelp.setMnemonic(KeyEvent.VK_H);
	menuhelp.add(menuItem(translation.getProperty("About"),KeyEvent.VK_A));

	menubar.add(menufile);
	menubar.add(menuedit);
	menubar.add(menuview);
	menubar.add(menutool);
	menubar.add(menuhydr);
	menubar.add(menuster);
	menubar.add(Box.createHorizontalGlue());
	menubar.add(menuhelp);

	return menubar;
    }    
    
    // builds and returns a menu bar suitable for the applet-style invocation
    private JMenuBar menuBarApplet()
    {
	JMenuBar menubar=new JMenuBar();
	AppletMenu menublock=new AppletMenu(translation.getProperty("Block"));
	AppletMenu menuselect=new AppletMenu(translation.getProperty("Select"));
	AppletMenu menutransform=new AppletMenu(translation.getProperty("Transform"));
	AppletMenu menuzoom=new AppletMenu(translation.getProperty("Zoom"));
	AppletMenu menushow=new AppletMenu(translation.getProperty("Show"));
	AppletMenu menuhydrogen=new AppletMenu(translation.getProperty("Hydrogen"));
    	AppletMenu menuhelp=new AppletMenu(translation.getProperty("Help"));
	if( MENU_SELECTION[0]){
	 menublock.setMnemonic(KeyEvent.VK_B);
	 menublock.add(menuItem(translation.getProperty("New"),KeyEvent.VK_N,null,KeyStroke.getKeyStroke('N',InputEvent.CTRL_MASK)));
	 menublock.add(menuItem(translation.getProperty("Editttt"),KeyEvent.VK_E,null,KeyStroke.getKeyStroke(' ',InputEvent.CTRL_MASK)));
	 menublock.add(menuItem(translation.getProperty("Undo"),KeyEvent.VK_U,null,KeyStroke.getKeyStroke('Z',InputEvent.CTRL_MASK)));
	 menublock.add(menuItem(translation.getProperty("Redo"),KeyEvent.VK_R,null,KeyStroke.getKeyStroke('Z',InputEvent.CTRL_MASK+InputEvent.SHIFT_MASK)));
    	 menublock.add(menuItem(translation.getProperty("Cut"),KeyEvent.VK_X,null,KeyStroke.getKeyStroke('X',InputEvent.CTRL_MASK)));
    	 menublock.add(menuItem(translation.getProperty("Copy"),KeyEvent.VK_C,null,KeyStroke.getKeyStroke('C',InputEvent.CTRL_MASK)));
    	 menublock.add(menuItem(translation.getProperty("Paste"),KeyEvent.VK_V,null,KeyStroke.getKeyStroke('V',InputEvent.CTRL_MASK)));
	menubar.add(menublock);
	}
	if( MENU_SELECTION[1]){
	menuselect.setMnemonic(KeyEvent.VK_S);
	menuselect.add(menuItem(translation.getProperty("Select_All"),KeyEvent.VK_S,null,KeyStroke.getKeyStroke('A',InputEvent.CTRL_MASK)));
	menuselect.add(menuItem(translation.getProperty("Next_Atom"),KeyEvent.VK_N,null,KeyStroke.getKeyStroke('E',InputEvent.CTRL_MASK)));
	menuselect.add(menuItem(translation.getProperty("Previous_Atom"),KeyEvent.VK_P,null,KeyStroke.getKeyStroke('E',InputEvent.CTRL_MASK+InputEvent.SHIFT_MASK)));
	menuselect.add(menuItem(translation.getProperty("Next_Group"),KeyEvent.VK_G,null,KeyStroke.getKeyStroke('G',InputEvent.CTRL_MASK)));
	menuselect.add(menuItem(translation.getProperty("Previous_Group"),KeyEvent.VK_R,null,KeyStroke.getKeyStroke('G',InputEvent.CTRL_MASK+InputEvent.SHIFT_MASK)));
	menubar.add(menuselect);	
	}

	if( MENU_SELECTION[2]){
	menutransform.setMnemonic(KeyEvent.VK_T);
	menutransform.add(menuItem(translation.getProperty("Flip_Horizontal"),KeyEvent.VK_H,null,null));
	menutransform.add(menuItem(translation.getProperty("Flip_Vertical"),KeyEvent.VK_V,null,null));
	menutransform.add(menuItem(translation.getProperty("Rotate_P45"),KeyEvent.VK_4,null,null));
	menutransform.add(menuItem(translation.getProperty("Rotate_M45"),KeyEvent.VK_5,null,null));
	menutransform.add(menuItem(translation.getProperty("Rotate_P90"),KeyEvent.VK_9,null,null));
	menutransform.add(menuItem(translation.getProperty("Rotate_M90"),KeyEvent.VK_0,null,null));
	menutransform.addSeparator();
    	menutransform.add(menuItem(translation.getProperty("Add_Temporary_Template"),KeyEvent.VK_T,null,null));
	menutransform.add(menuItem(translation.getProperty("Normalise_Bond_Lengths"),KeyEvent.VK_N,null,null));
	menubar.add(menutransform);
	}

	if( MENU_SELECTION[3]){
	menuzoom.setMnemonic(KeyEvent.VK_Z);
	menuzoom.add(menuItem(translation.getProperty("Zoom_Full"),KeyEvent.VK_F,null,KeyStroke.getKeyStroke('0',InputEvent.CTRL_MASK)));
	menuzoom.add(menuItem(translation.getProperty("Zoom_In"),KeyEvent.VK_I,null,KeyStroke.getKeyStroke('=',InputEvent.CTRL_MASK)));
	menuzoom.add(menuItem(translation.getProperty("Zoom_Out"),KeyEvent.VK_O,null,KeyStroke.getKeyStroke('-',InputEvent.CTRL_MASK)));
	menubar.add(menuzoom);
	}

	if( MENU_SELECTION[4]){
	menushow.setMnemonic(KeyEvent.VK_O);
	ButtonGroup showBG=new ButtonGroup();
	menushow.add(radioMenuItem(translation.getProperty("Show_Elements"),KeyEvent.VK_E,true,showBG));
	menushow.add(radioMenuItem(translation.getProperty("Show_All_Elements"),KeyEvent.VK_A,false,showBG));
	menushow.add(radioMenuItem(translation.getProperty("Show_Indices"),KeyEvent.VK_I,false,showBG));
	menushow.add(radioMenuItem(translation.getProperty("Show_Ring_ID"),KeyEvent.VK_R,false,showBG));
    	menushow.add(radioMenuItem(translation.getProperty("Show_CIP_Priority"),KeyEvent.VK_C,false,showBG));
	menushow.add(radioMenuItem(translation.getProperty("Show_Mapping_Number"),KeyEvent.VK_M,false,showBG));
	menubar.add(menushow);
	}

	if( MENU_SELECTION[5]){
	menuhydrogen.setMnemonic(KeyEvent.VK_H);
	chkShowHydr=new JCheckBoxMenuItem(translation.getProperty("Show_Hydrogen"));
	chkShowHydr.setMnemonic(KeyEvent.VK_Y);
	chkShowHydr.setSelected(true);
	chkShowHydr.addActionListener(this);
	menuhydrogen.add(chkShowHydr);
	menuhydrogen.add(menuItem(translation.getProperty("Set_Explicit"),KeyEvent.VK_E));
	menuhydrogen.add(menuItem(translation.getProperty("Clear_Explicit"),KeyEvent.VK_X));
	menuhydrogen.add(menuItem(translation.getProperty("Zero_Explicit"),KeyEvent.VK_Z));
	menuhydrogen.add(menuItem(translation.getProperty("Create_Actual"),KeyEvent.VK_C));
	menuhydrogen.add(menuItem(translation.getProperty("Delete_Actual"),KeyEvent.VK_D));
	menubar.add(menuhydrogen);
	}

	if( MENU_SELECTION[6]){
	AppletMenu menustereo=new AppletMenu(translation.getProperty("Stereo"));
	menustereo.setMnemonic(KeyEvent.VK_E);
	chkShowSter=new JCheckBoxMenuItem(translation.getProperty("Show_Stereolabels"));
	chkShowSter.setMnemonic(KeyEvent.VK_L);
	chkShowSter.setSelected(false);
	chkShowSter.addActionListener(this);
	menustereo.add(chkShowSter);
	menustereo.add(menuItem(translation.getProperty("Invert_Stereochemistry"),KeyEvent.VK_I));
	menustereo.add(menuItem(translation.getProperty("Set R/Z"),KeyEvent.VK_R));
	menustereo.add(menuItem(translation.getProperty("Set_S/E"),KeyEvent.VK_S));
	menustereo.add(menuItem(translation.getProperty("Cycle_Wedges"),KeyEvent.VK_C));
	menustereo.add(menuItem(translation.getProperty("Remove_Wedges"),KeyEvent.VK_W));
	menubar.add(menustereo);
	}

	if( MENU_SELECTION[7]){
	menuhelp.setMnemonic(KeyEvent.VK_P);
	menuhelp.add(menuItem(translation.getProperty("About"),KeyEvent.VK_A));
	menubar.add(menuhelp);	
	}
	
	return menubar;
    }
    
    public Molecule molData() {return editor.molData();} // shallow copy, use with care
    public void setMolecule(Molecule Mol) 
    {
    	editor.replace(Mol);
    	editor.RotateMolecule();
    	editor.scaleToFit();
	editor.notifySaved();
    }
    public void addMolecule(Molecule Mol) 
    {
    	editor.addArbitraryFragment(Mol);
    	editor.scaleToFit();
	editor.notifySaved();
    }
    public void scaleToFit() {editor.scaleToFit();}
    
    JMenuItem menuItem(String txt,int key) {return menuItem(txt,key,null,null);}
    JMenuItem menuItem(String txt,int key,Icon icon) {return menuItem(txt,key,icon,null);}
    JMenuItem menuItem(String txt,int key,Icon icon,KeyStroke accel)
    {
    	JMenuItem mi=new JMenuItem(txt,key);
	mi.addActionListener(this);
	if (icon!=null) mi.setIcon(icon);
	if (accel!=null) mi.setAccelerator(accel);
	return mi;
    }
    JRadioButtonMenuItem radioMenuItem(String txt,int key,boolean sel,ButtonGroup bg)
    {
    	JRadioButtonMenuItem mi=new JRadioButtonMenuItem(txt,sel);
	mi.addActionListener(this);
	mi.setMnemonic(key);
	bg.add(mi);
	return mi;
    }
    
    void fileQuit()
    {
    	if (!streamMode)
	{
    	    if (editor.isDirty())
	    {
    	    	if (JOptionPane.showConfirmDialog(null,
		    "Current structure has been modified. Exit without saving?","Quit",
		    JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
	    }
	}
	else
	{
	    writeStream();
	}

	if (frameParent!=null) frameParent.dispose(); 

	return;
    }
    void fileNew()
    {
    	if (editor.molData().numAtoms()>0)
	{
    	    if (JOptionPane.showConfirmDialog(null,
		"Clear current structure and start anew?","New",
		JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
	}
    	editor.clear();
	filename=null;
	if (frameParent!=null && saver==null) frameParent.setTitle("WIMSchem");
	editor.notifySaved();
    }
    void fileNewWindow()
    {
    	MainWindow mw=new MainWindow(null,false);
	mw.setVisible(true);
    }
    void fileNewDataSheet()
    {
    	DataWindow dw=new DataWindow(null);
	dw.setVisible(true);
    }
    void fileOpen()
    {
	JFileChooser chooser=new JFileChooser(System.getenv().get("PWD"));
	chooser.setDragEnabled(false);
	chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	chooser.setFileFilter(new FileExtFilter("Molecular Structures",".el;.mol;.sdf;.xml"));
	chooser.setAccessory(new FileMolPreview(chooser));
	if (chooser.showOpenDialog(frameParent)!=JFileChooser.APPROVE_OPTION) return;
	
	boolean fresh=editor.isEmpty();
	String newfn=chooser.getSelectedFile().getPath();
	boolean anything=editor.molData().numAtoms()>0;
	try
	{
	    FileInputStream istr=new FileInputStream(newfn);
	    if (!DataWindow.ALLOW && MoleculeStream.examineIsDatabase(istr)) 
	    {	// !! this section is to be deprecated when DataSheet is fully functional
    	    	istr.close();
	    	CatalogWindow cw=new CatalogWindow(newfn);
		cw.setVisible(true);
	    }
	    else if (DataSheetStream.examineIsXMLDS(istr) || DataSheetStream.examineIsMDLSDF(istr))
	    {
	    	istr.close();
		DataWindow dw=new DataWindow(newfn);
		dw.setVisible(true);
	    }
	    else
	    {
	    	Molecule frag=MoleculeStream.readUnknown(istr);
	    	editor.addArbitraryFragment(frag);
    	    	istr.close();
	    	if (fresh) setFilename(newfn);
    	    	if (!anything) editor.notifySaved();
	    }
	}
	catch (IOException e) 
	{
	    JOptionPane.showMessageDialog(null,e.toString(),"Open Failed",JOptionPane.ERROR_MESSAGE);
	    //e.printStackTrace();
	    return;
	}
    }
    
    void fileSave()
    {
    	if (filename==null && saver==null) {fileSaveAs(); return;}
	saveCurrent();
    }
    
    void fileSaveAs()
    {
	JFileChooser chooser=new JFileChooser(System.getenv().get("PWD"));
	chooser.setDragEnabled(false);
	chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	chooser.setFileFilter(new FileExtFilter("WIMSchem Files",".el"));
	chooser.setAccessory(new FileMolPreview(chooser));
	if (chooser.showSaveDialog(frameParent)!=JFileChooser.APPROVE_OPTION) return;
    
    	String fn=chooser.getSelectedFile().getPath();
	if (chooser.getSelectedFile().getName().indexOf('.')<0) fn=fn+".el";
    
    	File newf=new File(fn);
    	if (newf.exists())
	{
    	    if (JOptionPane.showConfirmDialog(null,
		"Overwrite existing file ["+newf.getName()+"]?","Save As",
		JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
	}    	
    
    	setFilename(fn);
	saveCurrent(true);
    }
    
    void fileExportMDLMOL()
    {
	JFileChooser chooser=new JFileChooser(System.getenv().get("PWD"));
	chooser.setDragEnabled(false);
	chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	chooser.setFileFilter(new FileExtFilter("MDL MOL Files",".mol"));
	chooser.setAccessory(new FileMolPreview(chooser));
	if (chooser.showSaveDialog(frameParent)!=JFileChooser.APPROVE_OPTION) return;

    	String fn=chooser.getSelectedFile().getPath();
	if (chooser.getSelectedFile().getName().indexOf('.')<0) fn=fn+".mol";

    	File newf=new File(fn);
    	if (newf.exists())
	{
    	    if (JOptionPane.showConfirmDialog(null,
		"Overwrite existing file ["+newf.getName()+"]?","Export MDL MOL",
		JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
	}    	

	try
	{
	    FileOutputStream ostr=new FileOutputStream(fn);
	    MoleculeStream.writeMDLMOL(ostr,editor.molData());
	    ostr.close();
	}
	catch (IOException e)
	{
	    JOptionPane.showMessageDialog(null,e.toString(),"Export Failed",JOptionPane.ERROR_MESSAGE);
	}
    }
    
    void fileExportSVG()
    {
	JFileChooser chooser=new JFileChooser(System.getenv().get("PWD"));
	chooser.setDragEnabled(false);
	chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	chooser.setFileFilter(new FileExtFilter("SVG Files",".svg"));
	chooser.setAccessory(new FileMolPreview(chooser));
	if (chooser.showSaveDialog(frameParent)!=JFileChooser.APPROVE_OPTION) return;

    	String fn=chooser.getSelectedFile().getPath();
	if (chooser.getSelectedFile().getName().indexOf('.')<0) fn=fn+".svg";

    	File newf=new File(fn);
    	if (newf.exists())
	{
    	    if (JOptionPane.showConfirmDialog(null,
		"Overwrite existing file ["+newf.getName()+"]?","Export SVG",
		JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
	}    	

	try
	{
	    FileOutputStream ostr=new FileOutputStream(fn);
    	    SVGMolecule svgmol=new SVGMolecule(editor.molData());
	    svgmol.draw();
	    svgmol.build(new PrintWriter(ostr));
	    ostr.close();
	}
	catch (IOException e)
	{
	    JOptionPane.showMessageDialog(null,e.toString(),"Export Failed",JOptionPane.ERROR_MESSAGE);
	}
    }
    
    void fileExportCMLXML()
    {
	JFileChooser chooser=new JFileChooser(System.getenv().get("PWD"));
	chooser.setDragEnabled(false);
	chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	chooser.setFileFilter(new FileExtFilter("XML Files",".xml"));
	chooser.setAccessory(new FileMolPreview(chooser));
	if (chooser.showSaveDialog(frameParent)!=JFileChooser.APPROVE_OPTION) return;

    	String fn=chooser.getSelectedFile().getPath();
	if (chooser.getSelectedFile().getName().indexOf('.')<0) fn=fn+".xml";

    	File newf=new File(fn);
    	if (newf.exists())
	{
    	    if (JOptionPane.showConfirmDialog(null,
		"Overwrite existing file ["+newf.getName()+"]?","Export CML XML",
		JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
	}    	

	try
	{
	    FileOutputStream ostr=new FileOutputStream(fn);
	    MoleculeStream.writeCMLXML(ostr,editor.molData());
	    ostr.close();
	}
	catch (IOException e)
	{
	    JOptionPane.showMessageDialog(null,e.toString(),"Export Failed",JOptionPane.ERROR_MESSAGE);
	}
    }

    void setFilename(String fn)
    {
    	if (fn.length()==0) {filename=null; return;}
    	filename=fn;
	
	if (!streamMode)
	{
	   String chopfn=fn;
	   int i=chopfn.lastIndexOf("/");
	   if (i>=0) chopfn=chopfn.substring(i+1);
           if (frameParent!=null && saver==null) frameParent.setTitle(chopfn+" - WIMSchem");
	} 
	else {if (frameParent!=null && saver==null) frameParent.setTitle("WIMSchem");}
    }
    
    void saveCurrent() {saveCurrent(false);}
    void saveCurrent(boolean force)
    {
    	if (saver!=null && !force)
	{
	    saver.saveMolecule(editor.molData().clone());
	    editor.notifySaved();
	    return;
	}
    
	try
	{
	    boolean fmtNative=true;
	
	    if (filename.toLowerCase().endsWith(".mol")) 
	    {
		String msg="The filename to save ends with '.mol', which is the\n"+
		    	   "conventional suffix for MDL MOL-files. Exporting to\n"+
		    	   "this format will cause some information loss. Do you wish\n"+
		    	   "to save in MDL MOL-file format?";
    	    	if (JOptionPane.showConfirmDialog(null,msg,"Format",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)
		    fmtNative=false;
	    }
	
	    FileOutputStream ostr=new FileOutputStream(filename);
	    if (fmtNative) MoleculeStream.writeNative(ostr,editor.molData());
	    else MoleculeStream.writeMDLMOL(ostr,editor.molData());
	    ostr.close();
	    editor.notifySaved();
	}
	catch (IOException e)
	{
	    JOptionPane.showMessageDialog(null,e.toString(),"Save Failed",JOptionPane.ERROR_MESSAGE);
	}
    }
    
    void readStream()
    {
	try
	{
	    Molecule frag=MoleculeStream.readUnknown(System.in);
	    editor.addArbitraryFragment(frag);
	}
	catch (IOException e) 
	{
	    JOptionPane.showMessageDialog(null,e.toString(),"<stdin> Read Failed",JOptionPane.ERROR_MESSAGE);
	    return;
	}
    }
    void writeStream()
    {
    	Molecule mol=editor.molData();
	try
	{
	    MoleculeStream.writeMDLMOL(System.out,mol);
	    MoleculeStream.writeNative(System.out,mol);
	}
	catch (IOException e) 
	{
	    JOptionPane.showMessageDialog(null,e.toString(),"<stdout> Write Failed",JOptionPane.ERROR_MESSAGE);
	}
    }
    
    void testMol()
    {
    	Molecule mol=new Molecule();
	
	mol.addAtom("N",0,0);
	mol.addAtom("C",1.2,0);
	mol.addAtom("O",2,0.8);
	mol.addAtom("H",3,-0.8);
	mol.addAtom("H",4,0);
	mol.addBond(1,2,1);
	mol.addBond(2,3,2);
	mol.addBond(3,4,1);
	mol.addBond(4,5,0);
	
	editor.replace(mol);
    } 
    
    void editCut()
    {
    	Molecule frag=editor.selectedSubgraph();
	try
	{
	    StringWriter sw=new StringWriter();
	    BufferedWriter bw=new BufferedWriter(sw);
	    MoleculeStream.writeMDLMOL(bw,frag);
	    MoleculeStream.writeNative(bw,frag);
	    if (useLocalClipboard)
	    {
	    	appletClipboard=sw.toString();
	    }
	    else
	    {
    		Clipboard clip=Toolkit.getDefaultToolkit().getSystemClipboard();
    		clip.setContents(new StringSelection(sw.toString()),this);
	    }
	    editor.deleteSelected(); // (keep this within the exception trap)
	}
	catch (IOException e) 
	{
	    JOptionPane.showMessageDialog(null,e.toString(),"Cut Failed",JOptionPane.ERROR_MESSAGE);
	}
    }
    
    void editCopy()
    {
    	Molecule frag=editor.selectedSubgraph();
	try
	{
	    StringWriter sw=new StringWriter();
	    BufferedWriter bw=new BufferedWriter(sw);
	    MoleculeStream.writeMDLMOL(bw,frag);
	    MoleculeStream.writeNative(bw,frag);
	    if (useLocalClipboard)
	    {
	    	appletClipboard=sw.toString();
	    }
	    else
	    {
    	    	Clipboard clip=Toolkit.getDefaultToolkit().getSystemClipboard();
    	    	clip.setContents(new StringSelection(sw.toString()),this);
	    }
	}
	catch (IOException e) 
	{
	    JOptionPane.showMessageDialog(null,e.toString(),"Copy Failed",JOptionPane.ERROR_MESSAGE);
	}
    }

    void editCopySVG()
    {
	StringWriter sw=new StringWriter();
    	SVGMolecule svgmol=new SVGMolecule(editor.molData());
	svgmol.draw();
	svgmol.build(new PrintWriter(sw));
	if (useLocalClipboard)
	{
	    appletClipboard=sw.toString();
	}
	else
	{
    	    Clipboard clip=Toolkit.getDefaultToolkit().getSystemClipboard();
    	    clip.setContents(new StringSelection(sw.toString()),this);
	}
    }
    
    void editPaste()
    {
    	try 
	{
	    String cliptext=null;
	    if (useLocalClipboard) cliptext=appletClipboard;
	    else
	    {
    		Clipboard clip=Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable contents=clip.getContents(null);
		if (contents!=null && contents.isDataFlavorSupported(DataFlavor.stringFlavor))
		{
		    cliptext=(String)contents.getTransferData(DataFlavor.stringFlavor);
		}
	    }
	    
	    if (cliptext!=null)
	    {
	    	Molecule frag=MoleculeStream.readUnknown(new BufferedReader(new StringReader(cliptext)));
		if (frag!=null) editor.addArbitraryFragment(frag);
	    }
	}
	catch (UnsupportedFlavorException e) 
	{
    	    JOptionPane.showMessageDialog(null,e.toString(),"Clipboard Read Failed",JOptionPane.ERROR_MESSAGE);
	}
	catch (IOException e) 
	{
    	    JOptionPane.showMessageDialog(null,e.toString(),"Paste Failed",JOptionPane.ERROR_MESSAGE);
	}
    }

    void selectElement(String El)
    {
    	    if (lastElement!=null){
		if (lastElement.compareTo(El)==0) return;
		toolIcons[TOOL_SETATOM]=new ImageIcon(getClass().getResource("/images/"+IMAGE_TOOL[TOOL_SETATOM]+".png"));
	    }
	    int w=toolIcons[TOOL_SETATOM].getImage().getWidth(null),h=toolIcons[TOOL_SETATOM].getImage().getHeight(null);
	    BufferedImage img=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
	    Graphics2D g=(Graphics2D)img.getGraphics();
	    g.setColor(new Color(0x00000000,true));
	    g.fillRect(0,0,w,h);
	    g.drawImage(toolIcons[TOOL_SETATOM].getImage(),0,0,null);
    	    Font font=new Font("SansSerif",Font.PLAIN,El.length()==1 ? 20 : 14);
	    g.setFont(font);
	    g.setColor(new Color(0,192,0));
	    FontMetrics metrics=g.getFontMetrics();
	    g.drawString(El,(w-metrics.stringWidth(El))/2-3,(h+metrics.getAscent())/2-2);
	    toolButtons[TOOL_SETATOM].setIcon(new ImageIcon(img));
    	    lastElement=El;
    }
    
    void templateTool()
    {
    	if (lastTemplate==null) {templateSelect(); return;}
    	editor.setToolTemplate(lastTemplate,templateIdx);
    }
    
    void templateSelect()
    {
    	int heightFudge=appletMode ? 30 : 0; // !! unpleasant hack which stands in for being a non-signed applet; correct at some point
    	TemplateSelector sel=new TemplateSelector(templ,this,heightFudge);
	Point pos=toolButtons[TOOL_TEMPLATE].getLocationOnScreen();

	Dimension ssz=Toolkit.getDefaultToolkit().getScreenSize();
    	GraphicsEnvironment ge=GraphicsEnvironment.getLocalGraphicsEnvironment();
	GraphicsConfiguration gc=ge.getScreenDevices()[0].getConfigurations()[0];
	ssz.width-=Toolkit.getDefaultToolkit().getScreenInsets(gc).right;
	ssz.height-=Toolkit.getDefaultToolkit().getScreenInsets(gc).bottom;
	      	
	if (pos.x+sel.getWidth()>ssz.width) pos.x=ssz.width-sel.getWidth();
	if (pos.y+sel.getHeight()>ssz.height) pos.y=ssz.height-sel.getHeight();
	sel.setLocation(pos);
	sel.setVisible(true);
    }
    
    void templateAddTo()
    {
    	templ.addTemplate(editor.selectedSubgraph());
    }
    
    void editDialog()
    {
    	Molecule newMol=(new DialogEdit(frameParent,editor.molData(),editor.selectedIndices())).exec();
	if (newMol!=null) 
	{
	    editor.cacheUndo();
	    editor.replace(newMol,false);
	}
    }
    
    void helpAbout()
    {
    	String msg="WIMSchem v"+VERSION+"\n"+
	    	   "Molecule drawing tool\n"+
		   "© 2005-2008 Dr. Alex M. Clark\n"+
		   "Released under the Gnu Public\n"+
		   "License (GPL), see www.gnu.org\n"+
		   "Home page and documentation:\n"+
		   "http://sketchel.sf.net\n"+
		   "Modified for WIMS\n"+
		   "jm.evers 2009";
    	JOptionPane.showMessageDialog(null,msg,"About WIMSchem",JOptionPane.INFORMATION_MESSAGE,mainLogo);
    }
    
    // ------------------ event functions --------------------

    public void actionPerformed(ActionEvent e) 
    {
    	String cmd=e.getActionCommand();
	
	int setsel=-1;
	
	if (cmd.equals(translation.getProperty("Quit")) || cmd.equals(translation.getProperty("Close")) || cmd.equals(translation.getProperty("Save_and_Quit"))) fileQuit();
	else if (cmd.equals(translation.getProperty("New"))) fileNew();
	else if (cmd.equals(translation.getProperty("New_Window"))) fileNewWindow();
	else if (cmd.equals(translation.getProperty("New_DataSheet"))) fileNewDataSheet();
	else if (cmd.equals(translation.getProperty("Open"))) fileOpen();
	else if (cmd.equals(translation.getProperty("Save"))) fileSave();
	else if (cmd.equals(translation.getProperty("Save_As"))) fileSaveAs();
    	else if (cmd.equals(translation.getProperty("as_MDL_MOL"))) fileExportMDLMOL();
    	else if (cmd.equals(translation.getProperty("as_CML_XML"))) fileExportCMLXML();
	else if (cmd.equals(translation.getProperty("as_SVG"))) fileExportSVG();
	else if (cmd.equals(translation.getProperty("Cursor")) || e.getSource()==toolButtons[TOOL_CURSOR]) {editor.setToolCursor(); setsel=TOOL_CURSOR;}
	else if (cmd.equals(translation.getProperty("Rotator")) || e.getSource()==toolButtons[TOOL_ROTATOR]) {editor.setToolRotator(); setsel=TOOL_ROTATOR;}
	else if (cmd.equals(translation.getProperty("Erasor")) || e.getSource()==toolButtons[TOOL_ERASOR]) {editor.setToolErasor(); setsel=TOOL_ERASOR;}
	else if (cmd.equals(translation.getProperty("Editttt")) || e.getSource()==toolButtons[TOOL_DIALOG]) editDialog();
    	else if (cmd.equals(translation.getProperty("Select_All"))) editor.selectAll();
	else if (cmd.equals(translation.getProperty("Next_Atom"))) editor.cycleSelection(true,false);
	else if (cmd.equals(translation.getProperty("Previous_Atom"))) editor.cycleSelection(false,false);
	else if (cmd.equals(translation.getProperty("Next_Group"))) editor.cycleSelection(true,true);
	else if (cmd.equals(translation.getProperty("Previous_Group"))) editor.cycleSelection(false,true);
	else if (cmd.equals(translation.getProperty("Edit_Atom")) || e.getSource()==toolButtons[TOOL_EDIT]) {editor.setToolAtom(null); setsel=TOOL_EDIT;}
	else if (cmd.equals(translation.getProperty("Set_Atom")) || e.getSource()==toolButtons[TOOL_SETATOM]) {editor.setToolAtom(lastElement); setsel=TOOL_SETATOM;}
	else if (cmd.equals(translation.getProperty("Single_Bond")) || e.getSource()==toolButtons[TOOL_SINGLE]) {editor.setToolBond(1); setsel=TOOL_SINGLE;}
	else if (cmd.equals(translation.getProperty("Double_Bond")) || e.getSource()==toolButtons[TOOL_DOUBLE]) {editor.setToolBond(2); setsel=TOOL_DOUBLE;}
	else if (cmd.equals(translation.getProperty("Triple_Bond")) || e.getSource()==toolButtons[TOOL_TRIPLE]) {editor.setToolBond(3); setsel=TOOL_TRIPLE;}
	else if (cmd.equals(translation.getProperty("Zero_Bond")) || e.getSource()==toolButtons[TOOL_ZERO]) {editor.setToolBond(0); setsel=TOOL_ZERO;}
	else if (cmd.equals(translation.getProperty("Inclined_Bond")) || e.getSource()==toolButtons[TOOL_INCLINED]) {editor.setToolBond(-1); setsel=TOOL_INCLINED;}
	else if (cmd.equals(translation.getProperty("Declined_Bond")) || e.getSource()==toolButtons[TOOL_DECLINED]) {editor.setToolBond(-2); setsel=TOOL_DECLINED;}
	else if (cmd.equals(translation.getProperty("Unknown_Bond")) || e.getSource()==toolButtons[TOOL_UNKNOWN]) {editor.setToolBond(-3); setsel=TOOL_UNKNOWN;}
	else if (cmd.equals(translation.getProperty("Charge")) || e.getSource()==toolButtons[TOOL_CHARGE]) {editor.setToolCharge(1); setsel=TOOL_CHARGE;}
	else if (cmd.equals(translation.getProperty("Undo")) || e.getSource()==toolButtons[TOOL_UNDO]) editor.undo();
	else if (cmd.equals(translation.getProperty("Redo")) || e.getSource()==toolButtons[TOOL_REDO]) editor.redo();
	else if (cmd.equals(translation.getProperty("Un_select_all")) || e.getSource()==toolButtons[TOOL_UNSELECT]) editor.deSelectAll();
        else if (cmd.equals(translation.getProperty("Select")) || e.getSource()==toolButtons[TOOL_SELECT]) editor.Select();
	else if (cmd.equals(translation.getProperty("Cut"))) editCut();
	else if (cmd.equals(translation.getProperty("Copy"))) editCopy();
	else if (cmd.equals(translation.getProperty("Copy_SVG"))) editCopySVG();
	else if (cmd.equals(translation.getProperty("Paste"))) editPaste();
	else if (cmd.equals(translation.getProperty("Flip_Horizontal"))) editor.flipSelectedAtoms(false);
	else if (cmd.equals(translation.getProperty("Flip_Vertical"))) editor.flipSelectedAtoms(true);
	else if (cmd.equals(translation.getProperty("Rotate_P45"))) editor.rotateSelectedAtoms(45);
	else if (cmd.equals(translation.getProperty("Rotate_M45"))) editor.rotateSelectedAtoms(-45);
	else if (cmd.equals(translation.getProperty("Rotate_P90"))) editor.rotateSelectedAtoms(90);
	else if (cmd.equals(translation.getProperty("Rotate_M90"))) editor.rotateSelectedAtoms(-90);
    	else if (cmd.equals(translation.getProperty("Add_Temporary_Template"))) templateAddTo();
    	else if (cmd.equals(translation.getProperty("Normalise_Bond_Lengths"))) editor.normaliseBondLengths();
	else if (cmd.equals(translation.getProperty("Template_Tool")) || e.getSource()==toolButtons[TOOL_TEMPLATE]) {templateTool(); setsel=TOOL_TEMPLATE;}
	else if (cmd.equals(translation.getProperty("Select_Template"))) {templateSelect(); setsel=TOOL_TEMPLATE;}
	else if (cmd.equals(translation.getProperty("Zoom_Full"))) editor.zoomFull();
	else if (cmd.equals(translation.getProperty("Zoom_In"))) editor.zoomIn(1.5);
	else if (cmd.equals(translation.getProperty("Zoom_Out"))) editor.zoomOut(1.5);
	else if (cmd.equals(translation.getProperty("Show_Elements"))) editor.setShowMode(ArrangeMolecule.SHOW_ELEMENTS);
	else if (cmd.equals(translation.getProperty("Show_All_Elements"))) editor.setShowMode(ArrangeMolecule.SHOW_ALL_ELEMENTS);
	else if (cmd.equals(translation.getProperty("Show_Indices"))) editor.setShowMode(ArrangeMolecule.SHOW_INDEXES);
	else if (cmd.equals(translation.getProperty("Show_Ring_ID"))) editor.setShowMode(ArrangeMolecule.SHOW_RINGID);
	else if (cmd.equals(translation.getProperty("Show_CIP_Priority"))) editor.setShowMode(ArrangeMolecule.SHOW_PRIORITY);
	else if (cmd.equals(translation.getProperty("Show_Mapping_Number"))) editor.setShowMode(ArrangeMolecule.SHOW_MAPNUM);
	else if (cmd.equals(translation.getProperty("Show_Hydrogen"))) editor.setShowHydrogens(chkShowHydr.isSelected());
	else if (cmd.equals(translation.getProperty("Set_Explicit")))  editor.hydrogenSetExplicit(true);
	else if (cmd.equals(translation.getProperty("Clear_Explicit"))) editor.hydrogenSetExplicit(false);
    	else if (cmd.equals(translation.getProperty("Zero_Explicit"))) editor.hydrogenSetExplicit(false,0);
	else if (cmd.equals(translation.getProperty("Create_Actual"))) editor.hydrogenCreateActual();
	else if (cmd.equals(translation.getProperty("Delete_Actual"))) editor.hydrogenDeleteActual();
	else if (cmd.equals(translation.getProperty("Show_Stereolabels"))) editor.setShowStereolabels(chkShowSter.isSelected());
	else if (cmd.equals(translation.getProperty("Invert_Stereochemistry"))) editor.setStereo(Molecule.STEREO_UNKNOWN);
	else if (cmd.equals(translation.getProperty("Set_R/Z"))) editor.setStereo(Molecule.STEREO_POS);
	else if (cmd.equals(translation.getProperty("Set_S/E"))) editor.setStereo(Molecule.STEREO_NEG);
	else if (cmd.equals(translation.getProperty("Cycle_Wedges"))) editor.cycleChiralWedges();
	else if (cmd.equals(translation.getProperty("Remove_Wedges"))) editor.removeChiralWedges();
	else if (cmd.equals(translation.getProperty("About"))) helpAbout();
	else if (TOOL_SELECTION[TOOL_SETATOM] && cmd.length()<=2) {selectElement(cmd); editor.setToolAtom(lastElement);}
    	if (setsel!=-1) toolGroup.setSelected(toolButtons[setsel].getModel(),true);
    } 
    
    public void mouseClicked(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mousePressed(MouseEvent e) 
    {
    	if (e.getSource()==toolButtons[TOOL_SETATOM] && e.getButton()==MouseEvent.BUTTON3)
	{
	    toolButtons[TOOL_SETATOM].setSelected(true);
	    JPopupMenu popup=new JPopupMenu();
	    int max = myAtoms.length;
	    for(int i=0 ; i < max ;i++ ){
	     popup.add(menuItem( myAtoms[i] ,0));
	    }
	    popup.show(toolButtons[TOOL_SETATOM],0,0);
	}
	if (e.getSource()==toolButtons[TOOL_TEMPLATE] && e.getButton()==MouseEvent.BUTTON3) 
	{
	    toolGroup.setSelected(toolButtons[TOOL_TEMPLATE].getModel(),true);
	    templateSelect();
	}
    }
    public void mouseReleased(MouseEvent e) {}   
    public void keyPressed(KeyEvent e) 
    {
    	// keyboard arrow-nudges
    	if (!e.isAltDown() && !e.isShiftDown() && !e.isControlDown() && !e.isMetaDown())
	{
	    if (e.getKeyCode()==KeyEvent.VK_UP) {editor.nudgeSelectedAtoms(0,0.05); return;}
	    if (e.getKeyCode()==KeyEvent.VK_DOWN) {editor.nudgeSelectedAtoms(0,-0.05); return;}
	    if (e.getKeyCode()==KeyEvent.VK_LEFT) {editor.nudgeSelectedAtoms(-0.05,0); return;}
	    if (e.getKeyCode()==KeyEvent.VK_RIGHT) {editor.nudgeSelectedAtoms(0.05,0); return;}
	}
    }
    public void keyReleased(KeyEvent e) {}
    public void keyTyped(KeyEvent e)
    {    
    	// user typing in an element...
    	char ch=e.getKeyChar();
	if (ch>='A' && ch<='Z') typedElement=""+ch;
	else if (typedElement.length()==1 && ch>='a' && ch<='z') typedElement=typedElement+ch;
	else if (typedElement.compareTo("R")==0 && ch>='0' && ch<='9') typedElement=typedElement+ch;
	else {typedElement=""; return;}

	String elset=null;
	if (typedElement.length()>=2 && typedElement.charAt(0)=='R' && typedElement.charAt(1)>='0' && typedElement.charAt(1)<='9') 
	{
	    elset=typedElement;
	}
	else
	{
	    for (int n=1;n<Molecule.ELEMENTS.length;n++) if (typedElement.compareTo(Molecule.ELEMENTS[n])==0) {elset=typedElement;}
	}
	if (elset!=null)
	{
	    selectElement(elset); 
	    toolGroup.setSelected(toolButtons[TOOL_SETATOM].getModel(),true);
	    editor.setToolAtom(elset);
	}
    }
    
    public void templSelected(Molecule mol,int idx)
    {
    	lastTemplate=mol;
	templateIdx=idx;
	editor.setToolTemplate(mol,idx);
    }
    public void molSelected(EditorPane source,int idx,boolean dblclick)
    {
    	if (dblclick && idx!=0)
	{
	    ArrayList<Integer> selidx=new ArrayList<Integer>();
	    if (idx>0) selidx.add(idx);
	    else {selidx.add(editor.molData().bondFrom(-idx)); selidx.add(editor.molData().bondTo(-idx));}
	    Molecule newMol=(new DialogEdit(frameParent,editor.molData(),selidx)).exec();
	    if (newMol!=null) editor.replace(newMol);
	}
    }
    public void dirtyChanged(boolean isdirty)
    {
    	String str=frameParent==null ? "WIMSchem" : frameParent.getTitle();
	if (str.charAt(0)=='*') str=str.substring(1);
	if (isdirty) str="*"+str;
	if (frameParent!=null && saver==null) frameParent.setTitle(str);
    }
    
    public void lostOwnership(Clipboard clipboard,Transferable contents) {} // don't care
    
    public void windowActivated(WindowEvent e) {}
    public void windowClosed(WindowEvent e) {}
    public void windowClosing(WindowEvent e) 
    {
    	fileQuit();
    }
    public void windowDeactivated(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowOpened(WindowEvent e) 
    {
    	if (firstResize)
	{
	    editor.scaleToFit();
	    editor.repaint();
	    firstResize=false;
	}
	editor.requestFocusInWindow();
    }
    
    public  String getToolTips( int p ){
	switch(p){
 	 case 0: return translation.getProperty("CURSOR_TIP");
 	 case 1: return translation.getProperty("ROTATOR_TIP");
 	 case 2: return translation.getProperty("ERASOR_TIP");
 	 case 3: return translation.getProperty("EDIT_DIALOG_TIP");
 	 case 4: return translation.getProperty("EDIT_ELEMENT_TIP");
 	 case 5: return translation.getProperty("PLACE_ELEMENT_TIP");
 	 case 6: return translation.getProperty("SINGLE_BOND_TIP");
 	 case 7: return translation.getProperty("DOUBLE_BOND_TIP");
 	 case 8: return translation.getProperty("TRIPLE_BOND_TIP");
 	 case 9: return translation.getProperty("ZERO_BOND_TIP");
 	 case 10: return translation.getProperty("INCLINED_BOND_TIP");
 	 case 11: return translation.getProperty("DECLINE_BOND_TIP");
 	 case 12: return translation.getProperty("SQUIGGLY_BOND_TIP");
 	 case 13: return translation.getProperty("CHARGE_TIP");
 	 case 14: return translation.getProperty("UNDO_TIP");
 	 case 15: return translation.getProperty("REDO_TIP");
 	 case 16: return translation.getProperty("TEMPLATE_TIP");
 	 case 17: return translation.getProperty("CUT_TIP");
 	 case 18: return translation.getProperty("COPY_TIP");
 	 case 19: return translation.getProperty("PASTE");
 	 case 20: return translation.getProperty("SELECT");
	 case 21: return translation.getProperty("UNSELECT");
 	 default : return "NO TIP?";
	}
    }
    private Properties loadProperties(String lang){
     Properties P=new Properties();
     try{
      InputStream in = getClass().getResourceAsStream("/lang/WIMSchemProperties_"+lang+".properties");
      P.load(in);in.close();
      return P;
     }catch (Exception e){ System.out.println("error reading /lang/WIMSchemProperties_"+lang+".properties\n"+e);}
     return null;
    }
	

}
