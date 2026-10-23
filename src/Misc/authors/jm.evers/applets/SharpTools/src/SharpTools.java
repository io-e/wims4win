/*                                                                                                             
License.                                                                                                       
This is free software; you can redistribute it and/or modify                                                   
it under the terms of the GNU General Public License as published by                                           
the Free Software Foundation;                                                                                  
HotEqn is distributed in the hope that it will be useful,                                                      
but WITHOUT ANY WARRANTY; without even the implied warranty of                                                 
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                                                  
GNU General Public License for more details.                                                                   
You should have received a copy of the GNU General Public License                                              
along with this program.  If not, see <http://www.gnu.org/licenses/>.                                          
*/ 
/*
 * @(#)SharpTools.java
 * 
 * $Id: SharpTools.java,v 1.178 2002/08/08 05:15:08 huaz Exp $
 * 
 * Created on October 10, 2000, 1:15 AM
 */
package SharpTools;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.net.*;

/**
 * This is the graphical user interface class for the spreadsheet.
 * <p>
 * The initial size of the window can be controlled by editing the
 * sharptools.ini file.  This class also contains button and menu
 * initialization and editing methods.
 *
 * @author  Daniel Goldberg (initial UI)
 * @author  Andrei Scudder, Daniel Medina (more work)
 * @author  Hua Zhong (complete for v1.0)
 * @version $Revision: 1.178 $
 */
final public class SharpTools extends JFrame implements ListSelectionListener {
    /* jm.evers */
    public URL backurl; 
    public String reply_url;
    SharpTools spreadsheet;
    static boolean inapplet=false; 
    static String language="en";
    public static Properties translation=new Properties(); 
    protected int maxNumPage = 1;

    // the rest of the components
    private Container container;
    private JTable table;
    private JScrollPane scrollPane;
    private JToolBar toolBar;
    private JMenuBar menuBar;
    private JPanel barPanel;
    private History history;
    private Histogram histogram;
    public SharpOptionPane sharpoptionpane;
    static private Config config;
    private ListSelectionModel rowSelectionModel;
    private ListSelectionModel columnSelectionModel;
    private URL url;
    // these are used to access our global objects
    static public Config getConfig() { return config; }    
    public History getHistory() { return history; }
    public JTable getTable() { return table; }
    public SharpTableModel getTableModel() { return tableModel; }
    public JToolBar getToolBar() { return toolBar; }
    //public JMenuBar getMenuBar() { return menuBar; }
    private FileOp fileOp;
    private EditOp editOp;
    private TableOp tableOp;
    private SharpTableModel tableModel;
    //    private HelpOp helpOp;

    // initialize JMenu fields
    // menu headings
    private JMenu fileMenu;
    private JMenu openMenu;
    private JMenu recentMenu;
    private JMenu editMenu;
    private JMenu tableMenu;
    private JMenu chartMenu;
    private JMenu helpMenu;
	// submenu headings
    private JMenu insertMenu;
    private JMenu deleteMenu;
    private JMenu sortMenu;
    //    private JMenuItem menuItem;
    private JMenuItem saveMenuItem;
    private JCheckBoxMenuItem saveOnExitMenuItem;
    private JMenuItem undoMenuItem;
    private JMenuItem redoMenuItem;
    private JMenuItem findNextMenuItem;
    private JCheckBoxMenuItem showHistogramMenuItem;
    private JCheckBoxMenuItem showFunctionsMenuItem;
    
    private JButton exitButton;
    private JButton saveButton;
    private JButton passwordButton;
    private JButton undoButton;
    private JButton redoButton;
    private JComponent funcList;
    //    private JCheckBox showButton;
    ImageIcon newIcon=new ImageIcon(getClass().getResource("/images/new.gif"));
    ImageIcon openIcon=new ImageIcon(getClass().getResource("/images/open.gif"));
    ImageIcon saveIcon=new ImageIcon(getClass().getResource("/images/save.gif"));
    ImageIcon unlockedIcon=new ImageIcon(getClass().getResource("/images/unlocked.gif"));
    ImageIcon lockedIcon=new ImageIcon(getClass().getResource("/images/locked.gif"));
    ImageIcon printIcon=new ImageIcon(getClass().getResource("/images/print.gif"));
    ImageIcon undoIcon=new ImageIcon(getClass().getResource("/images/undo.gif"));
    ImageIcon redoIcon=new ImageIcon(getClass().getResource("/images/redo.gif"));
    ImageIcon cutIcon=new ImageIcon(getClass().getResource("/images/cut.gif"));
    ImageIcon copyIcon=new ImageIcon(getClass().getResource("/images/copy.gif"));
    ImageIcon pasteIcon=new ImageIcon(getClass().getResource("/images/paste.gif"));
    ImageIcon findIcon=new ImageIcon(getClass().getResource("/images/find.gif"));
    ImageIcon insertRowIcon=new ImageIcon(getClass().getResource("/images/insertrow.gif"));
    ImageIcon insertColumnIcon=new ImageIcon(getClass().getResource("/images/insertcolumn.gif"));
    ImageIcon deleteRowIcon=new ImageIcon(getClass().getResource("/images/deleterow.gif"));
    ImageIcon deleteColumnIcon=new ImageIcon(getClass().getResource("/images/deletecolumn.gif"));
    ImageIcon sortIcon=new ImageIcon(getClass().getResource("/images/sort.gif"));
    ImageIcon chartIcon=new ImageIcon(getClass().getResource("/images/chart.gif"));
    ImageIcon helpIcon=new ImageIcon(getClass().getResource("/images/help.gif"));
    ImageIcon exitIcon=new ImageIcon(getClass().getResource("/images/no.gif"));
    ImageIcon WIMSIcon=new ImageIcon(getClass().getResource("/images/send.gif"));
    
    public static int baseRow = 0;
    public static int baseCol = 1;
    public int firstColWidth;
    private CellRange cellrange;
    /** Creates new SharpTools */
    public SharpTools(boolean is_this_the_applet,String lang) {	
        super("Sharp_Tools_Spreadsheet");
	//	setIconImage(Toolkit.getDefaultToolkit().getImage("tools.jpg")); 
	int x;
	int y;
	language=lang;
	System.out.println("in Sharptools.java the language="+language);
	if(is_this_the_applet){
	    inapplet = true;
	    x = MainApplet.Xlocation;                                                                                                                
            y = MainApplet.Ylocation;
	    backurl = MainApplet.backurl;
	    reply_url = MainApplet.reply_url;
	}
	else
	{
	    inapplet = false;
	    x = config.getInt("X");
	    y = config.getInt("Y");
	}
	setLocation(x, y);
	translation=loadProperties(language);
	setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        // get content pane
	container = this.getContentPane();
	container.setLayout(new BorderLayout());

	// create the table
	table = new JTable();

	// create the Functions combobox in the table
	funcList = HelpOp.createFunctionListComponent(this, table);
	
	// set up fileMenu
        fileMenu = new JMenu("File", true);
	fileMenu.setMnemonic(KeyEvent.VK_F); //used constructor instead
	// add fileMenu actions
	JMenuItem menuItem = new JMenuItem(translation.getProperty("New"));
	menuItem.setMnemonic(KeyEvent.VK_N);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_N, ActionEvent.CTRL_MASK));
	menuItem.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    if(!inapplet){fileOp.newFile();}else{ReStart();}
		}
	    });
	if(!inapplet){
	    // SJK Added
	    fileMenu.add(menuItem);
	    openMenu = new JMenu (translation.getProperty("Open"));
	    openMenu.setMnemonic (KeyEvent.VK_O);
	    menuItem = new JMenuItem (translation.getProperty("File"));
	    menuItem.setMnemonic (KeyEvent.VK_F);
	    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_O, ActionEvent.CTRL_MASK));
	    menuItem.addActionListener (new ActionListener () {
		public void actionPerformed (ActionEvent evt) {
		    fileOp.openFile ();
		}
	    } );
	    openMenu.add (menuItem);
	    menuItem = new JMenuItem (translation.getProperty("Database"));
	    menuItem.setMnemonic (KeyEvent.VK_D);
	    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_D, ActionEvent.CTRL_MASK));
	    menuItem.addActionListener (new ActionListener () {
		public void actionPerformed (ActionEvent evt) {
		    fileOp.openDb ();
		}
	    } );
	    openMenu.add (menuItem);
	    fileMenu.add (openMenu);
	    // use provate member for further reference
	    saveMenuItem = new JMenuItem(translation.getProperty("Save"));
	    saveMenuItem.setMnemonic(KeyEvent.VK_S);
    	    saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
	    saveMenuItem.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    fileOp.saveFile();
		}
	    });
    	    fileMenu.add(saveMenuItem);
	
	    menuItem = new JMenuItem(translation.getProperty("Save_As"));
	    menuItem.setMnemonic(KeyEvent.VK_A);
	    //        menuItem.setAccelerator(KeyStroke.getKeyStroke(
	    //				KeyEvent.VK_A, ActionEvent.CTRL_MASK));
	    menuItem.addActionListener (new ActionListener () {
        	public void actionPerformed (ActionEvent evt) {
		    fileOp.saveAsFile();
		}
	    });
	    fileMenu.add(menuItem);
	    
	    menuItem = new JMenuItem(translation.getProperty("Set_Password"));
	    menuItem.setMnemonic(KeyEvent.VK_E);
	    menuItem.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    fileOp.setPassword();
		    checkPasswordState();
		}
	    });
	    fileMenu.add(menuItem);
	    fileMenu.addSeparator();
	    menuItem = new JMenuItem(translation.getProperty("Print"));
	    menuItem.setMnemonic(KeyEvent.VK_P); //used constructor instead
    	    menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.CTRL_MASK));
	    menuItem.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    //notYetImplemented();		
		    Thread runner = new Thread() {
			    public void run() {
				fileOp.printData();
			    }
			};
		    runner.start(); 
		}
	    });
	    fileMenu.add(menuItem);
	    fileMenu.addSeparator();

	    recentMenu = new JMenu(translation.getProperty("Recent_Files"));
	    recentMenu.setMnemonic(KeyEvent.VK_R); //used constructor instead
	    recentMenu.addMenuListener(new MenuListener() {
		public void menuCanceled(MenuEvent e) {}
		public void menuDeselected(MenuEvent e) {}
		public void menuSelected(MenuEvent e) {
		    fileOp.createRecentFilesMenu(recentMenu);
		}
	    });
	
	    fileMenu.add(recentMenu);
	
	    fileMenu.addSeparator();

	    //set up Save Window on Exit
	    saveOnExitMenuItem = new JCheckBoxMenuItem(translation.getProperty("Save_Window_on_Exit"));
	    saveOnExitMenuItem.setMnemonic(KeyEvent.VK_W);
	    saveOnExitMenuItem.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    config.setBoolean("SAVEWINDOW", saveOnExitMenuItem.isSelected());
		}
	    });
	    // initialize
	    saveOnExitMenuItem.setSelected(config.getBoolean("SAVEWINDOW"));
	    fileMenu.add(saveOnExitMenuItem);
	    fileMenu.addSeparator();
	    menuItem = new JMenuItem(translation.getProperty("Exit"));
    	    menuItem.setMnemonic(KeyEvent.VK_X);
    	    menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.CTRL_MASK));
	    menuItem.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    //exitMenuActionPerformed(evt);
		    exit();
		}
	    });
    	    fileMenu.add(menuItem);
	} // einde if(!inapplet){}
	
	//Set up Edit menu
	editMenu = new JMenu(translation.getProperty("Edit"), true);
	editMenu.setMnemonic(KeyEvent.VK_E);

        // Undo
        undoMenuItem = new JMenuItem(translation.getProperty("Undo"));
        undoMenuItem.setMnemonic(KeyEvent.VK_U);
        undoMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK));
	undoMenuItem.addActionListener (new ActionListener () {
            public void actionPerformed (ActionEvent evt) {
		// save current selection
		history.undo(tableModel);
	    }
	});
        editMenu.add(undoMenuItem);

	// Redo
	redoMenuItem = new JMenuItem(translation.getProperty("Redo"));
        redoMenuItem.setMnemonic(KeyEvent.VK_R);
        redoMenuItem.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_Y, ActionEvent.CTRL_MASK));
	redoMenuItem.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    history.redo(tableModel);
		}
	    });
        editMenu.add(redoMenuItem);
	editMenu.addSeparator();

	// cut
	menuItem = new JMenuItem(translation.getProperty("Cut"));
	menuItem.setMnemonic(KeyEvent.VK_T); 
	menuItem.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_X, ActionEvent.CTRL_MASK));
	menuItem.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    editOp.cut();
                }
            });
	editMenu.add(menuItem);

	// copy
	menuItem = new JMenuItem(translation.getProperty("Copy"));
        menuItem.setMnemonic(KeyEvent.VK_C); 
	menuItem.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_C, ActionEvent.CTRL_MASK));
	menuItem.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    editOp.copy();
		}
	    });
	editMenu.add(menuItem);

	// paste
	menuItem = new JMenuItem(translation.getProperty("Paste"));
	menuItem.setMnemonic(KeyEvent.VK_P); 
	menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_V, ActionEvent.CTRL_MASK));
	menuItem.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    editOp.paste();
		}
	    });
	editMenu.add(menuItem);
	editMenu.addSeparator();

	// Fill
	menuItem = new JMenuItem(translation.getProperty("Fill"));
	menuItem.setMnemonic(KeyEvent.VK_F);
        menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_L, ActionEvent.CTRL_MASK));
	menuItem.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		  editOp.fill();
		}
	    }); 
	editMenu.add(menuItem);

	// Clear
	menuItem = new JMenuItem(translation.getProperty("Clear"));
	menuItem.setMnemonic(KeyEvent.VK_L);
	menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_DELETE, 0, true));
	menuItem.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    editOp.clear();
		}
	    });
	editMenu.add(menuItem);
	editMenu.addSeparator();

	// Find
	menuItem = new JMenuItem(translation.getProperty("Find"));
	menuItem.setMnemonic(KeyEvent.VK_F);
        menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_F, ActionEvent.CTRL_MASK));
	menuItem.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    editOp.find(true);
		}
	    });
	editMenu.add(menuItem);
	
	findNextMenuItem = new JMenuItem(translation.getProperty("Find_Next"));
	findNextMenuItem.setMnemonic(KeyEvent.VK_N);
        findNextMenuItem.setAccelerator(KeyStroke.getKeyStroke
				    (KeyEvent.VK_F3, 0));
	findNextMenuItem.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    editOp.find(false);
		}
	    });
	editMenu.add(findNextMenuItem);
	
	//set up tablemenu
	tableMenu = new JMenu(translation.getProperty("Table"), true);
	tableMenu.setMnemonic(KeyEvent.VK_T);
	//set up tableMenu actions
	insertMenu = new JMenu(translation.getProperty("Insert"));
	insertMenu.setMnemonic(KeyEvent.VK_I);
	
	menuItem = new JMenuItem(translation.getProperty("Row"));
	menuItem.setMnemonic(KeyEvent.VK_R);
        menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_INSERT, ActionEvent.CTRL_MASK));
	menuItem.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    tableOp.insert(true);
		}
		});
	insertMenu.add(menuItem);
	
	menuItem = new JMenuItem(translation.getProperty("Column"));
	menuItem.setMnemonic(KeyEvent.VK_C);
	menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_INSERT,
				 ActionEvent.CTRL_MASK|
				 ActionEvent.SHIFT_MASK));	
	menuItem.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    tableOp.insert(false);
		}
	    });
	insertMenu.add(menuItem);
	tableMenu.add(insertMenu);

	//set up Delete actions
        deleteMenu = new JMenu(translation.getProperty("Delete"));
	deleteMenu.setMnemonic(KeyEvent.VK_D);
	menuItem = new JMenuItem(translation.getProperty("Row"));
	menuItem.setMnemonic(KeyEvent.VK_R);
	menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_DELETE, ActionEvent.CTRL_MASK));
	menuItem.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    //notYetImplemented();
                    tableOp.remove(true);
		}
	    });
	deleteMenu.add(menuItem);
	menuItem = new JMenuItem(translation.getProperty("Column"));
	menuItem.setMnemonic(KeyEvent.VK_C);
	menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_DELETE,
				 ActionEvent.CTRL_MASK|
				 ActionEvent.SHIFT_MASK));
	menuItem.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    //notYetImplemented();
                    tableOp.remove(false);
		}
	    });
	deleteMenu.add(menuItem);
	tableMenu.add(deleteMenu);
	
	tableMenu.addSeparator();
	//set up Sort actions
	sortMenu = new JMenu(translation.getProperty("Sort"));
	sortMenu.setMnemonic(KeyEvent.VK_S);
	menuItem = new JMenuItem(translation.getProperty("Row"));
	menuItem.setMnemonic(KeyEvent.VK_R);
	menuItem.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
                    tableOp.sort(true);
		}
	    });
	sortMenu.add(menuItem);
	menuItem = new JMenuItem(translation.getProperty("Column"));
	menuItem.setMnemonic(KeyEvent.VK_C);
	menuItem.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    tableOp.sort(false);
		}
	    });
	sortMenu.add(menuItem);
	tableMenu.add(sortMenu);
	tableMenu.addSeparator();
	
	menuItem = new JMenuItem(translation.getProperty("Set_Column_Width"));
	menuItem.setMnemonic(KeyEvent.VK_W);
	menuItem.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    tableOp.setColumnWidth();
		}
	    });
	tableMenu.add(menuItem);
	menuItem = new JMenuItem(translation.getProperty("Set_First_Column_Width"));
	menuItem.setMnemonic(KeyEvent.VK_W);
	menuItem.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    tableOp.setFirstColumnWidth();
		}
	    });
	tableMenu.add(menuItem);
	
	//set up Histogram menu
	chartMenu = new JMenu(translation.getProperty("Chart"), true);
	chartMenu.setMnemonic(KeyEvent.VK_C);

	chartMenu.addMenuListener (new MenuListener () {
                public void menuSelected (MenuEvent e) {
		    checkShowHistogramState();
		}
		public void menuDeselected (MenuEvent e) { }
		public void menuCanceled (MenuEvent e) { }		
	    });


	//set up Show Histogram actions
	showHistogramMenuItem = new JCheckBoxMenuItem(translation.getProperty("Show"));
	showHistogramMenuItem.setMnemonic(KeyEvent.VK_S);
        showHistogramMenuItem.setAccelerator(KeyStroke.getKeyStroke
					 (KeyEvent.VK_F8, 0));
	showHistogramMenuItem.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    histogram.toggle();
		}
	    }); 
	chartMenu.add(showHistogramMenuItem);	

	//set up Add Histogram actions
	menuItem = new JMenuItem(translation.getProperty("Histogram"));
	menuItem.setMnemonic(KeyEvent.VK_H);
        menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_H, ActionEvent.CTRL_MASK));
	menuItem.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    histogram.addHistogram();
		}
	    }); 

	chartMenu.add(menuItem);
	tableMenu.add(chartMenu);


	//set up Help menu
	helpMenu = new JMenu(translation.getProperty("Help"), true);
	helpMenu.setMnemonic(KeyEvent.VK_H);

	//set up Help actions        
	menuItem = new JMenuItem(translation.getProperty("Help_Topics"));
	menuItem.setMnemonic(KeyEvent.VK_H);
	menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));

	menuItem.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    // testcode
		    try {
			URL url = new URL("http://localhost/sharptools/");
			new HelpOp(url);
		    } catch(MalformedURLException e) {
			e.printStackTrace();
			return;
		    }
		}
	    });
	helpMenu.add(menuItem);

	showFunctionsMenuItem = new JCheckBoxMenuItem(translation.getProperty("Show_Functions"));
	showFunctionsMenuItem.setMnemonic(KeyEvent.VK_S);
	if(inapplet){
	    showFunctionsMenuItem.setSelected(MainApplet.TOOLBAR_FUNCTIONS);
	}
	else
	{
	    showFunctionsMenuItem.setSelected(config.getBoolean("TOOLBAR_FUNCTIONS"));
	}
	showFunctionsMenuItem.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    boolean showFunc = showFunctionsMenuItem.isSelected();
		    if(!inapplet){
			config.setBoolean("TOOLBAR_FUNCTIONS", showFunc);
		    }
		    funcList.setVisible(showFunc);		    
		    // repaint
		}
            });
	
	helpMenu.add(showFunctionsMenuItem);

	helpMenu.addSeparator();
	menuItem = new JMenuItem(translation.getProperty("About_SharpTools"));
	menuItem.setMnemonic(KeyEvent.VK_A); 
	menuItem.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    HelpOp.showAboutBox(SharpTools.this);
		    table.requestFocus();
		}
            });
	helpMenu.add(menuItem); 
	
        // set up menu bar and menus
        menuBar = new JMenuBar();

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(tableMenu);
	menuBar.add(chartMenu);
        menuBar.add(helpMenu);
        
        // button initializations
	JButton WIMSButton = new JButton(WIMSIcon);
	JButton newButton = new JButton(newIcon);
	JButton exitButton = new JButton(exitIcon);
    	JButton openButton = new JButton(openIcon);
	JButton printButton = new JButton(printIcon);
	passwordButton = new JButton(unlockedIcon);
    	saveButton = new JButton(saveIcon);

	if(!inapplet){
	    newButton.setToolTipText(translation.getProperty("New"));
	    newButton.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    fileOp.newFile();
		    table.requestFocus();
		}
	    });
	    openButton.setToolTipText(translation.getProperty("Open"));
	    openButton.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    fileOp.openFile();
		    table.requestFocus();
		}
	    });
	
	    // we save this button as private member for further reference
	    saveButton.setToolTipText(translation.getProperty("Save"));
	    saveButton.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    fileOp.saveFile();
		    table.requestFocus();
		}
	    });
	
	    // we save this button as private member for further reference	
	    passwordButton.setToolTipText(translation.getProperty("Set_Password"));
	    passwordButton.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    fileOp.setPassword();
		    table.requestFocus();
		}
	    });

	    printButton.setToolTipText(translation.getProperty("Print"));
	    printButton.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    Thread runner = new Thread() {
			    public void run() {
				fileOp.printData();
			    }
			};
		    runner.start();
		    table.requestFocus();
		}
	    });
	}

	WIMSButton.setToolTipText(translation.getProperty("WIMS"));
	WIMSButton.addActionListener (new ActionListener () {
            public void actionPerformed (ActionEvent evt) {
		if(HelpOp.are_you_sure(SharpTools.this) == 0 && inapplet){
		    send_to_wims();
		}
	    }
	});

	undoButton = new JButton(undoIcon);
	undoButton.setToolTipText(translation.getProperty("Undo"));
	undoButton.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    history.undo(tableModel);
		    table.requestFocus();
		}
	    });

	redoButton = new JButton(redoIcon);
	redoButton.setToolTipText(translation.getProperty("Redo"));
	redoButton.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    history.redo(tableModel);
		    table.requestFocus();
		}
	    });
	
        JButton cutButton = new JButton(cutIcon);
	cutButton.setToolTipText(translation.getProperty("Cut"));
	cutButton.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    editOp.cut();
		    table.requestFocus();
		}
	    });
        JButton copyButton = new JButton(copyIcon);
	copyButton.setToolTipText(translation.getProperty("Copy"));
	copyButton.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    editOp.copy();
		    table.requestFocus();
		}
	    });
	
        JButton pasteButton = new JButton(pasteIcon);
	pasteButton.setToolTipText(translation.getProperty("Paste"));
        pasteButton.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    editOp.paste();
		    table.requestFocus();
		}
	    });

        JButton findButton = new JButton(findIcon);
	findButton.setToolTipText(translation.getProperty("Find"));
        findButton.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    editOp.find(true);
		    table.requestFocus();
		}
	    });

	JButton sortButton = new JButton(sortIcon);
	sortButton.setToolTipText(translation.getProperty("Sort_by_Column"));
        sortButton.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    tableOp.sort(false);
		    table.requestFocus();
		}
	    });

	JButton insertRowButton = new JButton(insertRowIcon);
	insertRowButton.setToolTipText(translation.getProperty("Insert_Row"));
        insertRowButton.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    tableOp.insert(true);
		    table.requestFocus();
		}
	    });

	JButton insertColumnButton = new JButton(insertColumnIcon);
	insertColumnButton.setToolTipText(translation.getProperty("Insert_Column"));
        insertColumnButton.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    tableOp.insert(false);
		    table.requestFocus();
		}
	    });

	JButton deleteRowButton = new JButton(deleteRowIcon);
	deleteRowButton.setToolTipText(translation.getProperty("Delete_Row"));
        deleteRowButton.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    tableOp.remove(true);
		    table.requestFocus();
		}
	    });

	JButton deleteColumnButton = new JButton(deleteColumnIcon);
	deleteColumnButton.setToolTipText(translation.getProperty("Delete_Column"));
        deleteColumnButton.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    tableOp.remove(false);
		    table.requestFocus();
		}
	    });

        JButton chartButton = new JButton(chartIcon);
	chartButton.setToolTipText(translation.getProperty("Histogram"));
        chartButton.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
		    histogram.addHistogram();
		    table.requestFocus();
		}
	    });
	
	JButton helpButton = new JButton(helpIcon);
	helpButton.setToolTipText(translation.getProperty("Help"));
	helpButton.addActionListener (new ActionListener () {
                public void actionPerformed (ActionEvent evt) {
			try {
			    if(!inapplet){
				String basePath = System.getProperty("user.dir");
			    }
			    url = new URL("http://localhost/sharptools/");
			    new HelpOp(url);

			} catch(MalformedURLException e) {
			    e.printStackTrace();
			    return;
			}
		    table.requestFocus();
		}
	    });

        /*
	 * Set up toolbar
	 *
	 * Toolbar can be customized in the configuration file.
	 */
        toolBar = new JToolBar();
	toolBar.setFloatable(false);
	int w;int h;
	if(!inapplet){
	    //if (config.getBoolean("TOOLBAR_NEW")){}
	    toolBar.add(newButton);
	    toolBar.add(openButton);
	    toolBar.add(saveButton);
	    toolBar.addSeparator();
	    toolBar.add(passwordButton);
	    toolBar.add(printButton);
    	    toolBar.addSeparator();
	    toolBar.add(undoButton);
	    toolBar.add(redoButton);
	    toolBar.add(cutButton);
	    toolBar.add(copyButton);
	    toolBar.add(pasteButton);
	    toolBar.add(findButton);
	    toolBar.addSeparator();
	    toolBar.add(insertRowButton);
	    toolBar.add(insertColumnButton);
	    toolBar.add(deleteRowButton);
	    toolBar.add(deleteColumnButton);
	    toolBar.add(sortButton);
    	    toolBar.addSeparator();
	    toolBar.add(chartButton);toolBar.addSeparator();
	    toolBar.add(helpButton);
    	    toolBar.addSeparator();
	    toolBar.add(funcList);
	    funcList.setVisible(true);
	    newTableModel(config.getInt("ROWS"), config.getInt("COLUMNS"));
	    // set window pos and size
	    w = config.getInt("WIDTH");
	    h = config.getInt("HEIGHT");	
	}
	else // inapplet ; we can't configure the sharptools.ini file...so we depend on params.
	{
	    if (MainApplet.TOOLBAR_UNDO){toolBar.add(undoButton);}
	    if (MainApplet.TOOLBAR_REDO){toolBar.add(redoButton);}
	    if (MainApplet.TOOLBAR_CUT){toolBar.add(cutButton);}
	    if (MainApplet.TOOLBAR_COPY){toolBar.add(copyButton);}
	    if (MainApplet.TOOLBAR_PASTE){toolBar.add(pasteButton);}
	    if (MainApplet.TOOLBAR_FIND){toolBar.add(findButton);}
	    toolBar.addSeparator();
	    if (MainApplet.TOOLBAR_INSERTROW){toolBar.add(insertRowButton);}
	    if (MainApplet.TOOLBAR_INSERTCOLUMN){toolBar.add(insertColumnButton);}
	    if (MainApplet.TOOLBAR_DELETEROW){toolBar.add(deleteRowButton);}
	    if (MainApplet.TOOLBAR_DELETECOLUMN){toolBar.add(deleteColumnButton);}
	    if (MainApplet.TOOLBAR_SORTCOLUMN){toolBar.add(sortButton);}
    	    toolBar.addSeparator();
	    if (MainApplet.TOOLBAR_HISTOGRAM) {toolBar.add(chartButton);toolBar.addSeparator();}
	    if (MainApplet.TOOLBAR_HELP){toolBar.add(helpButton);}
    	    toolBar.addSeparator();
	    toolBar.add(funcList);
	    funcList.setVisible(MainApplet.TOOLBAR_FUNCTIONS);
	    	 // evers send button
	    toolBar.addSeparator();
	    toolBar.add(WIMSButton);
	
	    newTableModel(MainApplet.ROWS, MainApplet.COLUMNS);
	    // set window pos and size
	    w = MainApplet.window_width;
	    h = MainApplet.window_height;
	}
	if (w >= 0 && h >= 0)
	    table.setPreferredScrollableViewportSize(new Dimension(w, h));

	// init fileOp objects
	if(!inapplet){fileOp = new FileOp(this);}

	// clobber resizing of all columns
	table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        
        // set table editor and renderer to custom ones
	table.setDefaultRenderer(Cell.class, new SharpCellRenderer());
        table.setDefaultEditor(Cell.class, new SharpCellEditor(
						  new JTextField()));

	// set selection mode for contiguous  intervals
        table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        table.setCellSelectionEnabled(true);

	// we don't allow reordering
        table.getTableHeader().setReorderingAllowed(false);
	table.getTableHeader().addMouseListener(new HeaderMouseAdapter());
	
	// create selection models
	rowSelectionModel = table.getSelectionModel();
        columnSelectionModel = table.getColumnModel().getSelectionModel();

	// add selection listeners to the selection models
	rowSelectionModel.addListSelectionListener(this);
	columnSelectionModel.addListSelectionListener(this);
	
	// set menu bar
	setJMenuBar(menuBar);
	container.add(toolBar, BorderLayout.NORTH);	
	
	scrollPane = new JScrollPane(table,
				     JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				     JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
	
        container.add(scrollPane, BorderLayout.CENTER);
        
        // add window exit listener
        addWindowListener (new WindowAdapter () {
		public void windowOpened (WindowEvent evt) {		    
		    table.requestFocus();
		}
		
		public void windowClosing (WindowEvent evt) {
		    exit();
		}
	    });

	// initial selection
	resetSelection();
	table.setRequestFocusEnabled(true);
	menuBar.setRequestFocusEnabled(false);
	toolBar.setRequestFocusEnabled(false);
	table.requestFocus();
	pack();
	show();
    }
    
    public void setColumnWidth(int colWidth) {
	if (colWidth > 0){
	    for (int i = baseCol; i < tableModel.getColumnCount(); i ++) {
		TableColumn column = table.getColumnModel().getColumn(i);
		column.setMinWidth(colWidth);
		column.setPreferredWidth(colWidth);

	    }
	}
    }
    
    /** 
     * Creates new blank SharpTableModel object with specified number of
     * rows and columns.  table is set to this table model to update screen.
     *
     * @param rows number of rows in new table model
     * @param cols number of columns in new table model
     */
    public void newTableModel(int rows, int cols) {
	tableModel = new SharpTableModel(this, rows, cols);
	table.setModel(tableModel);

	setBaseColumnWidth(0);
	if(!inapplet){setColumnWidth(config.getInt("COLUMNWIDTH"));} else {setColumnWidth(MainApplet.COLUMNWIDTH);}
	// update history with new one
	history = new History(this);
	tableModel.setHistory(history);	

	// inform tableModel that it's unmodified now
	tableModel.setPasswordModified(false);
	tableModel.setModified(false);

	// init op objects
	// we shouldn't init fileOp!
	
	if (editOp == null)
	    editOp = new EditOp(this);
	else
	    /* if we already have an object, we don't construct
	       a new EditOp to keep the clipboard and findValue
	       still valid.  This makes us be able to exchange
	       data across files */
	    editOp.init(this);

	tableOp = new TableOp(this);
	if(!inapplet){
	    histogram = new Histogram(this, "Histograms",config.getInt("HISTOGRAMWIDTH"),config.getInt("HISTOGRAMHEIGHT"));
	}
	else
	{
	    histogram = new Histogram(this, "Histograms",MainApplet.HISTOGRAMWIDTH,MainApplet.HISTOGRAMHEIGHT);
	}
	tableModel.setModified(false);
	
	resetSelection();
	
	// menubar/toolbar initial status
	checkUndoRedoState();
	
	table.requestFocus();
    }
    
    /**
     * a function to display warning messages
     *
     * @param s the operation that caused this error
     */
    public void noCellsSelected(String s) {
        sharpoptionpane.showMessageDialog(this, s + ": No cells selected","Error", JOptionPane.ERROR_MESSAGE, null);
    }

    /**
     * The error message for clicks on functions we haven't done yet.
     */
    private void notYetImplemented(){
	sharpoptionpane.showMessageDialog(this,
					  "Sorry, this function \n"+
					  "is not yet implemented!\n"+
					  "sharp@cs.columbia.edu",
					  "Sharp Tools Spreadsheet",
					  JOptionPane.WARNING_MESSAGE,
					  null);
    }

    /** Exit the Application */
    public void exit() {
	if(!inapplet){
	    if (fileOp.closeFile()) {
		// save window position and size
		if (config.getBoolean("SAVEWINDOW")) {
		    config.setInt("X", getX());
		    config.setInt("Y", getY());
		    Dimension dim = scrollPane.getViewport().getExtentSize();
		    config.setInt("WIDTH", (int)dim.getWidth());
		    config.setInt("HEIGHT", (int)dim.getHeight());
		}
		config.save();	    
	        System.exit(0);
	    }
	}
	else
	{
	    setVisible(false);
	}
    }
    
    /**
     * Directly open a file with specified name - used only in main
     *
     * @param filename the String of filename
     */
    private void openInitFile(String filename) {
	if(!inapplet){
	    fileOp.openTableModel(new File(filename));
	}
    }
    /**
     * Check menu items and toolbar buttons
     * Set to appropriate status (disable/enable)         
     */

    /**
     * Check the save menu/button state
     * Enable only when the file has been modified
     */
    public void checkSaveState() {
	boolean modified = tableModel.isModified();
	if(!inapplet){
	    saveMenuItem.setEnabled(modified);
	    saveButton.setEnabled(modified);
	}
    }

    /**
     * Check the set password toolbar button icon
     * Change the icon based on whether password is set
     */
    public void checkPasswordState() {
	if(!inapplet){
	    if (fileOp.hasPassword())
		passwordButton.setIcon(lockedIcon);
	    else
		passwordButton.setIcon(unlockedIcon);
	}
    }

    /**
     * Check the undo/redo menu/button state
     * Enable only when it's undoable/redoable
     */
    public void checkUndoRedoState() {
	boolean enable = history.isUndoable();
	undoMenuItem.setEnabled(enable);
	undoButton.setEnabled(enable);
	enable = history.isRedoable();
	redoMenuItem.setEnabled(enable);
	redoButton.setEnabled(enable);
    }

    /**
     * Check the find next menu/button state
     * Enable only when the user has searched once
     */
    public void checkFindNextState() {
	findNextMenuItem.setEnabled(editOp.hasFindValue());
    }

    /**
     * Check the show histogram menu
     * Enable only when there is at least one histogram defined
     */
    public void checkShowHistogramState() {	
	showHistogramMenuItem.setState(histogram.isVisible());
	showHistogramMenuItem.setEnabled(histogram.hasChart());	
    }

    public void setBaseColumnWidth(int width) {
	if(width == 0){
	    // resize first column
	    if (baseCol > 0) {
		TableColumn firstColumn = table.getColumnModel().getColumn(baseCol-1);
	        int firstColWidth;
		if(!inapplet){firstColWidth = config.getInt("FIRSTCOLUMNWIDTH");}else{firstColWidth=MainApplet.FIRSTCOLUMNWIDTH;}
		if (firstColWidth>0) {
		    firstColumn.setMinWidth(firstColWidth);
		    firstColumn.setPreferredWidth(firstColWidth);
		}
	    }
	}
	else
	{
	    if(width>0){
		TableColumn firstColumn = table.getColumnModel().getColumn(baseCol-1);
		firstColumn.setMinWidth(width);
	        firstColumn.setPreferredWidth(width);
	    }
	
	}
    }
    
    /** This is the main method that gets the ball rolling */
    public static void main(String args[]){
	if(!inapplet){
	    // read configuration file
	    config = new Config("sharptools.ini");
	    // set default value
	    config.setInt("ROWS", 20);
	    config.setInt("COLUMNS", 10);
	    //	config.set("AUTORESIZE", "TRUE");
	    config.setInt("HISTOGRAMWIDTH", 600);
	    config.setInt("HISTOGRAMHEIGHT", 400);
	    // read file
	    config.load();
	
	    // only change it when DEBUG is uncommented in the config file
	    if (config.get("DEBUG") != null)
		Debug.setDebug(config.getBoolean("DEBUG"));
	}
	// initialize the function handler table object
	Formula.registerFunctions();
        SharpTools spreadsheet = new SharpTools(inapplet,language);
        spreadsheet.show();
	if (args.length>0){spreadsheet.openInitFile(args[0]);}
    }

    // this is a static function to help loading images
    public static ImageIcon getImageIcon(String name) {
	//URL imgurl = SharpTools.class.getResource(name);
	//URL imgurl = getClass().getResource(name);
	//URL imgurl = ClassLoader.getResourceAsStream(name);
	URL imgurl = ClassLoader.getSystemResource(name);
	//URL imgurl = ClassLoader.getResource(name);
	if (imgurl == null) {
	    System.out.println("image "+name+" not found");
	    return null;
	}
	return new ImageIcon(imgurl);
    }

    class HeaderMouseAdapter extends MouseAdapter {

	public void mouseClicked(MouseEvent e) {
	    TableColumnModel colModel = 
		table.getColumnModel();
	    int col = 
		colModel.getColumn(colModel.getColumnIndexAtX(e.getX())).getModelIndex();
	    
	    int rowCount = table.getRowCount();
	    table.setRowSelectionInterval(baseRow, rowCount - 1);

	    if (col < baseCol)
		table.setColumnSelectionInterval(baseCol, table.getColumnCount()-1);
	    else
		table.setColumnSelectionInterval(col, col);
	}
    }

    // the ListSelectionListener interface
    public void valueChanged(ListSelectionEvent e) {
	table.requestFocus();
	    
	// Ignore extra messages
	if (e.getValueIsAdjusting()) return;
	
	// Get event source
	ListSelectionModel lsm = (ListSelectionModel)e.getSource();
	
	if (lsm.isSelectionEmpty() && table.getColumnCount() > baseCol &&
	    table.getRowCount() > baseRow) {
	    // always set selection
	    table.setRowSelectionInterval(baseRow, baseRow);
	    table.setColumnSelectionInterval(baseCol, baseCol);
	}/* else {
	    if (table.getSelectedColumn() < baseCol) {
		int columnCount = table.getColumnCount();
                if (columnCount > baseCol) {
                    Debug.println("setColumnSelectionInterval "
                                  + String.valueOf(baseCol)
                                  + " " + String.valueOf(columnCount - 1));
                              
                    table.setColumnSelectionInterval(baseCol, columnCount - 1);
                    table.removeColumnSelectionInterval(baseRow,baseRow);
                }
                }
		}*/
    }

    public void resetSelection() {
    	table.setRowSelectionInterval(baseRow, baseRow);
	table.setColumnSelectionInterval(baseCol, baseCol);
    }

    /* jm.evers */
    public void openFileString(String sp){
	StringBuffer textBuf = new StringBuffer();
	String line;
	try {
	    BufferedReader in =  new BufferedReader(new StringReader(sp));
	    line = in.readLine();
	    textBuf.append(line);
	    textBuf.append("\n");
		
	    while ((line = in.readLine()) != null) {
		textBuf.append(line);
		textBuf.append("\n");
	    }

	    String text = textBuf.toString();
	    // create new table model
	    CellPoint size = SharpTableModel.getSize(text);
	    newTableModel(size.getRow(), size.getCol());
	    //System.out.println(size.getRow()+","+size.getCol());
	    tableModel = getTableModel();
	    tableModel.fromString(text, 0,0,
				  new CellRange(baseRow,
						size.getRow(),
						baseCol,
						size.getCol()));
	    
	} catch (Exception e) { e.printStackTrace();System.out.println("in FileOp.java string = "+sp);}
    }

    public void ReStart(){
        setVisible(false);
        SharpTools spreadsheet = new SharpTools(inapplet,language);
        spreadsheet.show();
    }
    
    public String ReadRawSpreadSheet(){
	// included raw functions like =SUM(1,2,3,4)
	String reply=tableModel.toString();
	return reply;
    }

    public String ReadSpreadSheet(int type){
	String reply=tableModel.toWIMS(type);
	return reply;
    }
    
    public void send_to_wims() {                                                                                                           
        String reply = ReadSpreadSheet(MainApplet.type); 
	if(MainApplet.convert_initial_tsv_to_matrix){reply = reply +"\n"+ MainApplet.initial_tsv_matrix;}
	reply = URLEncoder.encode(reply);
        try {backurl=new URL(reply_url+reply);
	    (MainApplet.applet_context).showDocument(backurl,"_self");
	    this.dispose();
	}                                                                                        
        catch (MalformedURLException e) {backurl=null;System.out.println("could not send spreadsheet to wims "+reply_url);}
    } 

    public Properties loadProperties (String lang){
	Properties P=new Properties();      
	// jm.evers : trying global properties file with translations        
	try{
	    InputStream in = getClass().getResourceAsStream("/lang/SharpToolsProperties_"+lang+".properties");
	    P.load(in);in.close();
	    return P;
	}                
	catch (Exception e){ System.out.println("error reading lang/SharpToolsProperties_"+lang+".properties\n"+e);}    
        return null;             
    }                
  
}

