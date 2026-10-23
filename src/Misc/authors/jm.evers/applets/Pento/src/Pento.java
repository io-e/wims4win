
/*
*********************************************************************************
* J.M. Evers 19/11/2009								*
* This is all amateur scriblings... So no copyrights.				*
* This source code file, and compiled classes derived from it,			*
* can be used and distributed without restriction, including for commercial use	*
* No warrenty whatoever								*
*********************************************************************************
added a public function ReadApplet() and some applet-params
Applet returns String S=number_of_squares+","+used_squares+","+used_objects+","+total_perimeter_of_all_objects;
<html>
    <body>
    <script language="javascript" type="text/javascript">
	    function lees(){
		var input=document.applets[0].ReadApplet();
		alert(input);
	    }
	</script>
	<applet id="MathView" code="Pento.class" codebase="." archive="Pento.jar" width="640" height="480">
    	    <param name="objects" value="0,1:1,0,0,0,1:0,0,0,0,0:1,1,1,1,1@1,1,1,1,1:0,1,1,1,0@1,1,1:0,1,1:0,0,1@1,1,1,1@1:1:1">                                                                                                                                                                              
	    <param name="helptext" value="what ever">
	</applet>
	<p>
	<input type="button" name=".....TEST......" value=".....TEST....." onclick="javascript:lees();">
    </body>
</html>
*/

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class Pento extends Applet implements Runnable{
    private static final long serialVersionUID = -4039349014715451389L;
    Font small_font = new Font("Serif",Font.BOLD,16);FontMetrics small_fm = getFontMetrics(small_font);int small_height = small_fm.getHeight()+20;
    int collision=0;int gebruikte_vierkantjes=0;int aantal_vierkantjes=60;int used_objects=0;int teller=0;
    int figuur_nummer;String objects="";String helptext="";public Image plaatje;
    int mtd[][] = new int[100][140];
    int ttd[][] = new int[5][5],ttds[][] = new int[5][5];
    int ttn,tts = 0;
    int std[] = new int[12];
    int pd[][][] = {
                    {{0,0,0,0,0},{0,0,0,0,0},{1,1,1,1,1},{0,0,0,0,0},{0,0,0,0,0}},
                    {{0,0,0,0,0},{0,0,0,0,0},{0,1,1,1,1},{0,1,0,0,0},{0,0,0,0,0}},
                    {{0,0,0,0,0},{0,0,0,0,0},{0,1,1,1,1},{0,0,1,0,0},{0,0,0,0,0}},
                    {{0,0,0,0,0},{0,0,0,0,0},{0,0,1,1,1},{0,1,1,0,0},{0,0,0,0,0}},
                    {{0,0,0,0,0},{0,0,0,0,0},{0,1,1,1,0},{0,1,1,0,0},{0,0,0,0,0}},
                    {{0,0,0,0,0},{0,0,0,1,0},{0,0,1,1,0},{0,1,1,0,0},{0,0,0,0,0}},
                    {{0,0,0,0,0},{0,1,1,1,0},{0,0,1,0,0},{0,0,1,0,0},{0,0,0,0,0}},
                    {{0,0,0,0,0},{0,0,0,0,0},{0,1,1,1,0},{0,1,0,1,0},{0,0,0,0,0}},
                    {{0,0,0,0,0},{0,0,0,1,0},{0,1,1,1,0},{0,1,0,0,0},{0,0,0,0,0}},
                    {{0,0,0,0,0},{0,1,1,0,0},{0,0,1,1,0},{0,0,1,0,0},{0,0,0,0,0}},
                    {{0,0,0,0,0},{0,0,1,0,0},{0,1,1,1,0},{0,0,1,0,0},{0,0,0,0,0}},
                    {{0,0,0,0,0},{0,0,0,1,0},{0,0,0,1,0},{0,1,1,1,0},{0,0,0,0,0}},
                  };
    int pdl=pd.length;
    Color pc[] = new Color[12],bc;
    int cd[][] = {{250,0,0},{0,250,0},{0,0,250},{250,250,0},{250,0,250},{0,250,250},{150,100,0},{150,0,100},{100,150,0},{100,0,150},{0,100,150},{0,150,100}};
    int dx,dy,pn;
    int mx,my,mcx,mcy;
    Thread th = null;
    Image os;
    Graphics og;

   public void init(){
	plaatje = getImage(getCodeBase(), "draai.gif"); 
      setBackground(Color.white);
      os = createImage(680,420);
      og = os.getGraphics();
      for(int j1 = 0;j1 < 14;j1 ++){
         mtd[0][j1] = -1;mtd[1][j1] = -1;mtd[8][j1] = -1;mtd[9][j1] = -1;
      }
      for(int j2 = 0;j2 < 10;j2 ++){
         mtd[j2][0] = -1;mtd[j2][1] = -1;mtd[j2][12] = -1;mtd[j2][13] = -1;
      }
      for(int k = 0;k < 12;k ++){
         std[k] = 1;
         pc[k] = new Color(cd[k][0],cd[k][1],cd[k][2]);
      }
      bc = new Color(225,225,225);

      addMouseListener(
         new MouseAdapter(){
            public void mousePressed(MouseEvent e){
               dx = e.getX() ; dy = e.getY() ;

               if((dx > 40) && (dx < 640) && (dy > 260) && (dy < 420)){
                  pn = ((dx - 40) / 100) + ((dy - 260) / 80) * 6;

                  if((std[pn] == 1)){
                     if(tts == 0){
                        tts = 1 ; ttn = pn ; std[pn] = 0;
                        D_in(pn);
                     }else{
                        std[ttn] = 1 ; ttn = pn ; std[pn] = 0;
                        D_in(pn);
                     }
                  }else{
                     if(tts == 1){
                        std[ttn] = 1 ; tts = 0 ;
                        D_in(-1);
                     }
                  }
                  repaint();
               }

               if((dx > 440) && (dx < 640) && (dy > 20) && (dy < 220)){
                  if(tts == 1){
                     for(int f1 = 0;f1 < 5;f1 ++){
                        for(int f2 = 0;f2 < 5;f2 ++){
                           ttds[f1][4 - f2] = ttd[f2][f1];
                        }
                     }
                     for(int g1 = 0;g1 < 5;g1 ++){
                        for(int g2 = 0;g2 < 5;g2 ++){
                           ttd[g2][g1] = ttds[g2][g1];
                        }
                     }
                  repaint();
                  }
               }
		collision=0;
               if((dx > 20) && (dx < 420) && (dy > 20) && (dy < 260)){
                   dx = (e.getX() - 20) / 40 ; dy = (e.getY() -20) / 40;
                   if(tts == 1){
                      C_dp();
                      repaint();
                   }else{
                      if(mtd[dy + 2][dx + 2]  > 0){
                         ttn = mtd[dy + 2][dx + 2] - 1;
                         tts = 1;
                         B_dp();
                         repaint();
                      }
                   }
                }
            }         
         }
      );

      addMouseMotionListener(
         new MouseMotionAdapter(){
            public void mouseMoved(MouseEvent e){
               mx = (e.getX() - 20) / 40 ; my = (e.getY() -20) / 40;
               if((mx > -1) && (mx < 10) && (my > -1) && (my < 6)){
                  if((mx != mcx) || (my != mcy)){
                     mcx = mx;mcy = my;
                     repaint();
                  }
               }
            }
         }
      );

   }

   public void paint(Graphics g){
	objects=getParameter("objects");                                                                                                   
        if(objects!=null && objects.length()>0){                                                                                           
            pd = StringToIntArray(objects);                                                                                                
            pdl=pd.length;                                                                                                                 
        }   
      og.setColor(Color.white); 
      og.fillRect(0,0,680,420);
      gebruikte_vierkantjes=0;
      for(int lx = 0;lx < 10;lx ++){
         for(int ly = 0;ly < 6;ly ++){
            if(mtd[ly + 2][lx + 2] == 0){
               og.setColor(bc);       	     
            }else{
               og.setColor(pc[mtd[ly + 2][lx + 2] - 1]);
	       gebruikte_vierkantjes++;   
            }
            og.fillRect(lx * 40 + 20,ly * 40 + 20,40,40);
            og.setColor(Color.blue);
            og.draw3DRect(lx * 40 + 20,ly * 40 + 20,40,40,true);
         }
      }

      for(int tx = 0;tx < 5;tx ++){
         for(int ty = 0;ty < 5;ty ++){
            if(ttd[ty][tx] == 0){
               og.setColor(bc);
            }else{
               og.setColor(pc[ttn]);
            }
            og.fillRect(tx * 40 + 440,ty * 40 + 60,40,40);
            og.setColor(Color.blue);
            og.draw3DRect(tx * 40 + 440,ty * 40 + 60,40,40,true);
         }
      }

      for(int sn = 0;sn < pdl ;sn ++){
         if(std[sn] == 1){
            for(int d1 = 0;d1 < 5;d1 ++){
               for(int d2 = 0;d2 < 5;d2 ++){
                  if(pd[sn][d2][d1] == 1){
                     og.setColor(pc[sn]);
                     og.fillRect(d1 * 20 + (sn % 6) * 100 + 40,d2 * 20 + (sn / 6) * 80 + 240,20,20);
                     og.setColor(Color.black);
                     og.draw3DRect(d1 * 20 + (sn % 6) * 100 + 40,d2 * 20 + (sn / 6) * 80 + 240,20,20,true);
                  }
               }
            }
         }
      }

      if((tts == 1) && (mx > -1) && (mx < 10) && (my > -1) && (my < 6)){
         for(int q1 = 0;q1 < 5;q1 ++){
            for(int q2 = 0;q2 < 5;q2 ++){
               if(ttd[q2][q1] == 1){
                  if(((mx + q1 - 2) > -1) && ((mx + q1 - 2) < 10) && ((my + q2 - 2) > -1) && ((my + q2 - 2) < 6)){
		    if(collision==0){og.setColor(Color.lightGray);}else{og.setColor(Color.gray);}
                     og.drawRect(40 * (mx + q1 - 2) + 20,40 * (my + q2 - 2) + 20,40,40);
                     og.fillRect(40 * (mx + q1 - 2) + 20,40 * (my + q2 - 2) + 20,40,40);
                  }
               }
            }
         }   
      }
      g.drawImage(os,0,0,this );
      helptext=getParameter("helptext");
      if(helptext!=null && helptext.length()>0){og.setColor(Color.black);g.drawString(helptext,20,small_height);}
      g.drawImage(plaatje, 500, 0, this);
 
   }
//// einde graphics
///// converteer applet param String naar Array:  0,0,0,0,1:1,0,0,0,1:0,0,0,0,0:1,1,1,1,1 @ 1,1,1,1,1:0,1,1,1,0

    public int[][][] StringToIntArray(String tmp0){
	aantal_vierkantjes=0;
	int array[][][] = new int[12][5][5];String tmp1="0";String tmp2="0";String tmp3="0";
	int a = 0;int b = 0;int c = 0;
	StringTokenizer q = new StringTokenizer(tmp0, "@");
	int ma=q.countTokens();int mb=0;int mc=0;
	if( ma > 12 ){ ma = 12;}
	for( a = 0 ; a<ma ; a++){
	    tmp1=q.nextToken();
	    StringTokenizer r = new StringTokenizer(tmp1,":");
	    mb=r.countTokens();
	    for(b = 0 ; b<5 ; b++){
		if ( b < mb ){ tmp2=r.nextToken(); } else { tmp2="0,0,0,0,0";}
		    StringTokenizer t = new StringTokenizer(tmp2,",");
		    mc=t.countTokens();
		    for ( c = 0 ; c<5 ; c++){
			if( c < mc){ tmp3=t.nextToken(); if(tmp3.equals("1")){ array[a][b][c]=1;aantal_vierkantjes++; } else { array[a][b][c]=0;}}
			else { array[a][b][c]=0; }
		    }
		}
	    }
	return array;                                                                                                                      
    } 

//// einde "converteer applet param String naar Array"

///
   public void D_in(int nn){
      for(int k1 = 0;k1 < 5;k1 ++){
         for(int k2 = 0;k2 < 5;k2 ++){
            if(nn == -1){
               ttd[k2][k1] = 0;
            }else{
               ttd[k2][k1] = pd[nn][k2][k1];
            }
         }
      }
   }

   public void C_dp(){
      int fg = 1;collision=0;
      for(int c1 = 0;c1 < 5;c1 ++){
         for(int c2 = 0;c2 < 5;c2 ++){
            if((ttd[c2][c1] == 1) && (mtd[dy + c2][dx + c1] != 0)){
               fg = 0;collision=1;
            }
         }
      }
      if(fg == 1){
        used_objects++;
         for(int d1 = 0;d1 < 5;d1 ++){
            for(int d2 = 0;d2 < 5;d2 ++){
               if(ttd[d2][d1] == 1){
                  mtd[dy + d2][dx + d1] = ttn + 1;
               }
            }
         }
         D_in(-1);
         tts = 0;ttn = 0;
      }
   }

   public void B_dp(){
      for(int p1 = 0;p1 < 10;p1 ++){
         for(int p2 = 0;p2 < 6;p2 ++){
            if(mtd[p2 + 2][p1 + 2] == ttn + 1){
               mtd[p2 + 2][p1 + 2] = 0;
            }
         }
      }
      for(int p3 = 0;p3 < 5;p3 ++){
         for(int p4 = 0;p4 < 5;p4 ++){
            ttd[p4][p3] = pd[ttn][p4][p3];
         }
      }
      used_objects--;
   }
    public void start(){    
        if(th == null){
            th = new Thread(this);
            th.start();
        }
    }

    public void stop(){
        th = null;
    }

    public void run(){
        while (th != null){
            try {
                Thread.sleep(20);
            }
            catch (InterruptedException e){ }
        }
    }
    public void update( Graphics g )
    {
        paint( g ) ;
    }

    //  leest applet met javascript...bereken de omtrek...
    public String ReadApplet(){
	int border[][]=new int[20][20];
	int c1;int c2;int t=0;int r=0;
	// c1 vertikaal van boven->beneden   c2 horizontaal van links naar rechts
	for( c1 = 0; c1 < 20 ;c1++){
	    for( c2 = 0; c2 < 20 ;c2++){
		if(mtd[c2][c1] > 0){t=0;
		    if( (mtd[c2][c1-1] < 1) && (mtd[c2][c1+1] < 1) ){t=t+2;}else{if( (mtd[c2][c1-1] < 1) || (mtd[c2][c1+1] < 1) ){t=t+1;}}		
		    if( (mtd[c2-1][c1] < 1) && (mtd[c2+1][c1] < 1) ){t=t+2;}else{if( (mtd[c2-1][c1] < 1) || (mtd[c2+1][c1] < 1) ){t=t+1;}}	
		    border[c2][c1]=t; 
        	}
	    } 
	    for( c2 = 20; c2 < 0 ;c2--){
		if(mtd[c2][c1] > 0){r=0;
		    if( (mtd[c2][c1-1] < 1) && (mtd[c2][c1+1] < 1) ){r=r+2;}else{if( (mtd[c2][c1-1] < 1) || ( mtd[c2][c1+1] < 1) ){r=r+1;}}		
		    if( (mtd[c2-1][c1] < 1) && (mtd[c2+1][c1] < 1) ){r=r+2;}else{if( (mtd[c2-1][c1] < 1) || ( mtd[c2+1][c1] < 1) ){r=r+1;}}	
		    border[c2][c1]=r+border[c2][c1];    
        	}
	    }
	}
	int cnt=0;
	for( c1=0 ; c1<20 ; c1++ ){
	    for( c2=0 ; c2<20 ; c2++ ){
		cnt=cnt+border[c2][c1];
	    }
	}
	//String S="total_squares="+aantal_vierkantjes+",used_squares="+gebruikte_vierkantjes+"used_objects="+used_objects+",perimeter="+cnt;
	String S=aantal_vierkantjes+","+gebruikte_vierkantjes+","+used_objects+","+cnt;
        return S;
    }          
// einde  "public String ReadApplet()"
    public int readparm(String S){
	int s=0;String c="";
	if(S.equals("interactive")){
	    s=1;c=getParameter(S);if(c!=null && c.length()>0){ if (c.equals("0")){s = 0;}}
	}
        if(S.equals("cols")){
	    s=10;c=getParameter("cols");
	    if(c!=null && c.length()>0){
		s = Integer.parseInt(c, 10); 
		if(s>10){s=10;}
		if(s<3){s=3;}
	    }
	}
	if(S.equals("rows")){
	    s=6;c=getParameter("rows");
	    if(c!=null && c.length()>0){
		s = Integer.parseInt(c, 10); 
		if(s>6){s=6;}
		if(s<3){s=3;}
	    }
	}
	return s;
    }
}

