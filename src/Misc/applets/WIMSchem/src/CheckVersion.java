/*
    J.M. Evers 2009
    A check for browserversion.
    A very small applet to be started before the WIMSchem applet.
    Example:
    
    <applet id="CheckApplet" name="CheckApplet" data="WIMSchem/CheckVersion.class" archive="dist/WIMSchem.jar" width="1" height="1">
    </applet>
    <script language="javascript" type="text/javascript">
	var VERSION_REQUIRED = 150;
	var javaVersion;
	try { javaVersion = document.getElementById("CheckApplet").getVersion(); alert(javaVersion);} catch(e){ javaVersion = 0;}
	if( javaVersion < VERSION_REQUIRED ){
	    if(javaVersion==0){
		alert(" It seems your browser does not support Java - Javascript communication.\n Continue at own risk...");
	    }
	    else
	    {            
		var t=confirm("You must go to the java download page to get the latest JavaPlugin...\nMinimal Version :"+VERSION_REQUIRED);
		if(t){window.open("http://java.sun.com/javase/downloads/index.jsp","","status=1,toolbar=1,width=600,height=480");}
	    }
	}	
    </script>
*/

package WIMSchem;
import java.applet.*;

public class CheckVersion extends Applet{
    public void init(){}
    public void stop(){}
    public void destroy(){}
    public String Version = System.getProperty("java.version");

    public String getVersion(){
	String[] v=Version.split("\\.");
	String version="";// 1.5.0_test123
	for(int p=0;p<v.length;p++){
	    if(v[p].length() == 1 || v[p]!=null){
    		version=version+""+v[p];
	    }
	}
	if(version.length()!=3){
	    if(version.length()==1){version=version+"00";}
	    if(version.length()==2){version=version+"0";}
	}
	return version;
    }
}

