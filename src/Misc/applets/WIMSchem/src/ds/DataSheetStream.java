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
    Readers and writers of the DataSheet format.

    The native format is XML (or at least, the subset of XML used by the TrivialDOM class), and is structured as follows:

    	<?xml version="1.0" encoding="UTF-8"?>
    	<DataSheet>
	    <Summary>
	    	<Title>{title}</Title>
		<Description><![CDATA[{description}]]></Description>
	    </Summary>
	    <Header ncols=~ nrows=~>
	    	<Column id="1" name=~ type=~>{description}</Column>
	    	<Column id="2" name=~ type=~>{description}</Column>
		...
	    	<Column id="{ncols}" name=~ type=~>{description}</Column>
	    </Header>
	    <Content>
	    	<Row id="1">
		    <Cell id="1">{data}</Cell>
		    <Cell id="2">{data}</Cell>
		    ...
		    <Cell id="{ncols}">{data}</Cell>
		</Row>
		...
		<Row id="{nrows}">
		    ...
		</Row>
	    </Content>
	</DataSheet>

    All indices are 1-based.

*/

public class DataSheetStream
{
    // read-ahead code to try to figure out a filetype

    // returns true if stream is the native datasheet format; preserves file position
    public static boolean examineIsXMLDS(FileInputStream istr)
    {
    	boolean ret=false;
    	try
	{
    	    long lastpos=istr.getChannel().position();
    	    BufferedReader rdr=new BufferedReader(new InputStreamReader(istr));
	    ret=examineIsXMLDS(rdr);
    	    istr.getChannel().position(lastpos);
	}
	catch (IOException e) {}
    	return ret;
    }

    // as above, except this version loses the stream position
    public static boolean examineIsXMLDS(BufferedReader rdr)
    {
    	try
	{
	    for (int n=0;n<2;n++)
	    {
		String str=rdr.readLine();
		if (str==null) break;
		if (str.startsWith("<DataSheet>")) return true;
	    }
	}
	catch (IOException e) {}
	return false;
    }

    // returns true if stream appears to be an MDL SDfile; preserves file position
    public static boolean examineIsMDLSDF(FileInputStream istr)
    {
    	boolean ret=false;
    	try
	{
    	    long lastpos=istr.getChannel().position();
    	    BufferedReader rdr=new BufferedReader(new InputStreamReader(istr));
	    ret=examineIsMDLSDF(rdr);
    	    istr.getChannel().position(lastpos);
	}
	catch (IOException e) {}
    	return ret;
    }

    // as above, except this version loses the stream position
    public static boolean examineIsMDLSDF(BufferedReader rdr)
    {
    	try
	{
	    for (int n=0;n<3000;n++)
	    {
		String str=rdr.readLine();
		if (str==null) break;
		if (str.compareTo("$$$$")==0 /* || str.compareTo("M  END")==0*/) return true;
	    }
	}
	catch (IOException e) {}
	return false;
     }

   // reading of datasheets from the WIMSchem XML format

    public static DataSheet readXML(InputStream istr) throws IOException
    {
    	return readXML(new BufferedReader(new InputStreamReader(istr)));
    }
    public static DataSheet readXML(BufferedReader in) throws IOException
    {
	TrivialDOM xml=TrivialDOM.readXML(in);

	if (xml.document().nodeName().compareTo("DataSheet")!=0)
	    throw new IOException("Input stream is XML, but the root node is not <DataSheet>.");

    	DataSheet ds=new DataSheet();

	// do a precursory check
	TrivialDOM.Node doc=xml.document(),header=null,content=null,summary=null;
	for (int n=0;n<doc.numChildren();n++) if (doc.childType(n)==TrivialDOM.TYPE_NODE)
	{
	    TrivialDOM.Node node=doc.getChildNode(n);
	    if (node.nodeName().equals("Summary")) summary=node;
	    if (node.nodeName().equals("Header")) header=node;
	    if (node.nodeName().equals("Content")) content=node;
	}
	if (header==null) throw new IOException("XML document lacks a <Header> element.");
	if (content==null) throw new IOException("XML document lacks a <Content> element.");

	int ncols=Util.safeInt(header.attribute("ncols"),-1),nrows=Util.safeInt(header.attribute("nrows"),-1);
	if (ncols<0 || ncols>5000) throw new IOException("Header@ncols attribute absent or improperly specified.");
	if (nrows<0) throw new IOException("Header@nrows attribute absent or improperly specified.");

    	// read out the summary info
	if (summary!=null) for (int n=0;n<summary.numChildren();n++)
	    if (summary.childType(n)==TrivialDOM.TYPE_NODE)
	{
    	    TrivialDOM.Node node=summary.getChildNode(n);
    	    if (node.nodeName().equals("Title")) ds.setTitle(node.getText());
	    else if (node.nodeName().equals("Description")) ds.setDescription(node.getText());
	}

	// put the columns into an array, then create in datasheet
	String[] colName=new String[ncols],colDescr=new String[ncols];
	int[] colType=new int[ncols];
	for (int n=0;n<ncols;n++) colName[n]=null; // means unspecified in source
	for (int n=0;n<header.numChildren();n++) if (header.childType(n)==TrivialDOM.TYPE_NODE)
	{
	    TrivialDOM.Node node=header.getChildNode(n);
	    if (node.nodeName().compareTo("Column")!=0) continue;
	    int id=Util.safeInt(node.attribute("id"),0);
	    if (id<1 || id>ncols) throw new IOException("Column@id out of range.");
	    String strName=node.attribute("name"),strType=node.attribute("type");
	    if (strName==null) throw new IOException("Column name not specified.");
	    if (strType==null) throw new IOException("Column type not specified.");
	    int type=0;
	    if (strType.compareTo("molecule")==0) type=DataSheet.COLTYPE_MOLECULE;
	    else if (strType.compareTo("string")==0) type=DataSheet.COLTYPE_STRING;
	    else if (strType.compareTo("integer")==0) type=DataSheet.COLTYPE_INTEGER;
	    else if (strType.compareTo("real")==0) type=DataSheet.COLTYPE_REAL;
	    else if (strType.compareTo("boolean")==0) type=DataSheet.COLTYPE_BOOLEAN;
	    else throw new IOException("Coltype type ["+strType+"] not recognised.");

	    colName[id-1]=strName;
	    colType[id-1]=type;
	    colDescr[id-1]=node.getText();
	}
	for (int n=0;n<ncols;n++) if (colName[n]==null) throw new IOException("Column id#"+(n+1)+" is not defined.");
	for (int n=0;n<ncols;n++) ds.appendColumn(colName[n],colType[n],colDescr[n]);

	// append a row for each claimed case, then fill in the data as it is encountered
	for (int n=0;n<nrows;n++) ds.appendRow();
	for (int i=0;i<content.numChildren();i++) if (content.childType(i)==TrivialDOM.TYPE_NODE)
	{
	    TrivialDOM.Node row=content.getChildNode(i);
	    if (row.nodeName().compareTo("Row")!=0) continue;
	    int rid=Util.safeInt(row.attribute("id"),0);
	    if (rid<1 || rid>nrows) throw new IOException("Row@id out of range.");

	    for (int j=0;j<row.numChildren();j++) if (row.childType(j)==TrivialDOM.TYPE_NODE)
	    {
	    	TrivialDOM.Node cell=row.getChildNode(j);
    	    	if (cell.nodeName().compareTo("Cell")!=0) continue;
		int cid=Util.safeInt(cell.attribute("id"),0);
		if (cid<1 || cid>ncols) throw new IOException("Cell@id out of range.");

		String data=cell.getText();
    	    	int type=colType[cid-1];

		if (type==DataSheet.COLTYPE_MOLECULE)
		{
		    Molecule mol=null;
		    try {mol=MoleculeStream.readUnknown(new BufferedReader(new StringReader(data)));}
		    catch (IOException e) {} // leave it null
		    ds.setMolecule(rid-1,cid-1,mol);
		}
		else if (type==DataSheet.COLTYPE_STRING)
		{
		    ds.setString(rid-1,cid-1,data);
		}
		else if (type==DataSheet.COLTYPE_INTEGER)
		{
		    try
		    {
		    	int v=new Integer(data).intValue();
			ds.setInteger(rid-1,cid-1,v);
		    }
	    	    catch (NumberFormatException e) {ds.setToNull(rid-1,cid-1);}
		}
		else if (type==DataSheet.COLTYPE_REAL)
		{
		    try
		    {
		    	double v=new Double(data).doubleValue();
			ds.setReal(rid-1,cid-1,v);
		    }
	    	    catch (NumberFormatException e) {ds.setToNull(rid-1,cid-1);}
		}
		else if (type==DataSheet.COLTYPE_BOOLEAN)
		{
		    ds.setBoolean(rid-1,cid-1,data.toLowerCase().compareTo("true")==0) ;
		}
	    }
	}

	return ds;
    }

    // reading of datasheets from the MDL SD file format

    public static DataSheet readSDF(InputStream istr) throws IOException
    {
    	return readSDF(new BufferedReader(new InputStreamReader(istr)));
    }
    public static DataSheet readSDF(BufferedReader in) throws IOException
    {
    	DataSheet ds=new DataSheet();

	ds.appendColumn("mol",DataSheet.COLTYPE_MOLECULE,"Molecule");

	ArrayList<String> entry=new ArrayList<String>();

    	while (true)
	{
	    String line=in.readLine();
	    if (line==null) break;
	    if (!line.startsWith("$$$$")) {entry.add(line); continue;}

	    int rn=ds.appendRow();

	    StringBuffer sb=new StringBuffer();
	    int pos=0;
	    while (pos<entry.size())
	    {
	    	line=entry.get(pos);
		if (line.startsWith("> ")) break;
		sb.append(line+"\n");
		pos++;
	    	if (line.startsWith("M  END")) {break;}
	    }

	    Molecule mol=null;
	    try {mol=MoleculeStream.readMDLMOL(new BufferedReader(new StringReader(sb.toString())));}
	    catch (IOException e) {} // leave it null
	    if (mol!=null) ds.setMolecule(rn,0,mol);

	    for (;pos+2<entry.size();pos+=3)
	    {
	    	String key=entry.get(pos),val=entry.get(pos+1);
		if (!key.startsWith(">")) continue;
		int z=key.indexOf("<"); if (z<0) continue;
		key=key.substring(z+1);
		z=key.indexOf(">"); if (z<0) continue;
		key=key.substring(0,z);
		if (key.length()==0) continue;

		int type=val.length() > 0 ? DataSheet.COLTYPE_STRING : DataSheet.COLTYPE_INTEGER;
		double dval=0;
		int ival=0;
		try
		{
		    dval=Double.parseDouble(val);
		    ival=(int)Math.round(dval);
		    type=dval==ival ? DataSheet.COLTYPE_INTEGER : DataSheet.COLTYPE_REAL;
		}
	    	catch (NumberFormatException e) {} // stays string

		int cn=-1;
		for (int n=0;n<ds.numCols();n++) if (ds.colName(n).compareTo(key)==0) {cn=n; break;}
		if (cn<0) cn=ds.appendColumn(key,type,"");

		int curType=ds.colType(cn);
		if (val.length()==0) ds.setToNull(rn,cn);
		else if (curType==DataSheet.COLTYPE_STRING) ds.setString(rn,cn,val);
		else if (curType==DataSheet.COLTYPE_REAL)
		{
		    if (type==DataSheet.COLTYPE_STRING)
		    {
		    	if (ds.changeColumnType(cn,DataSheet.COLTYPE_STRING,false)) ds.setString(rn,cn,val);
		    }
		    else ds.setReal(rn,cn,dval);
		}
		else if (curType==DataSheet.COLTYPE_INTEGER)
		{
		    if (type==DataSheet.COLTYPE_STRING)
		    {
		    	if (ds.changeColumnType(cn,DataSheet.COLTYPE_STRING,false)) ds.setString(rn,cn,val);
		    }
		    else if (type==DataSheet.COLTYPE_REAL)
		    {
		    	if (ds.changeColumnType(cn,DataSheet.COLTYPE_REAL,false)) ds.setReal(rn,cn,dval);
		    }
		    else ds.setInteger(rn,cn,ival);
		}
	    }

	    entry.clear();
	}

	return ds;
    }

    // writing of datasheets to the WIMSchem XML format

    public static void writeXML(OutputStream ostr,DataSheet ds) throws IOException
    {
	writeXML(new BufferedWriter(new OutputStreamWriter(ostr)),ds);
    }
    public static void writeXML(BufferedWriter out,DataSheet ds) throws IOException
    {
    	TrivialDOM xml=new TrivialDOM("DataSheet");

    	int ncols=ds.numCols(),nrows=ds.numRows();

    	TrivialDOM.Node summary=xml.document().appendNode("Summary");
	summary.appendNode("Title").setText(ds.getTitle(),false);
	summary.appendNode("Description").setText(ds.getDescription(),true);

    	TrivialDOM.Node header=xml.document().appendNode("Header");
	header.setAttribute("ncols",ds.numCols()+"");
	header.setAttribute("nrows",ds.numRows()+"");
	for (int n=0;n<ncols;n++)
	{
	    TrivialDOM.Node col=header.appendNode("Column");
	    col.setAttribute("id",String.valueOf(n+1));
	    col.setAttribute("name",ds.colName(n));
	    int type=ds.colType(n);
	    if (type==DataSheet.COLTYPE_MOLECULE) col.setAttribute("type","molecule");
	    else if (type==DataSheet.COLTYPE_STRING) col.setAttribute("type","string");
	    else if (type==DataSheet.COLTYPE_INTEGER) col.setAttribute("type","integer");
	    else if (type==DataSheet.COLTYPE_REAL) col.setAttribute("type","real");
	    else if (type==DataSheet.COLTYPE_BOOLEAN) col.setAttribute("type","boolean");

	    col.setText(ds.colDescr(n),false);
	}

    	TrivialDOM.Node content=xml.document().appendNode("Content");
	for (int i=0;i<nrows;i++)
	{
	    TrivialDOM.Node row=content.appendNode("Row");
	    row.setAttribute("id",String.valueOf(i+1));
	    for (int j=0;j<ncols;j++)
	    {
	    	TrivialDOM.Node col=row.appendNode("Cell");
		col.setAttribute("id",String.valueOf(j+1));
		int type=ds.colType(j);
		if (ds.isNull(i,j)) {} // do nothing (stays blank)
		else if (type==DataSheet.COLTYPE_MOLECULE)
		{
		    try
		    {
			StringWriter sw=new StringWriter();
			BufferedWriter bw=new BufferedWriter(sw);
			MoleculeStream.writeNative(bw,ds.getMolecule(i,j));
			col.setText(sw.toString(),true);
		    }
		    catch (IOException e) {} // entry stays blank
		}
		else if (type==DataSheet.COLTYPE_STRING) col.setText(ds.getString(i,j),true);
		else if (type==DataSheet.COLTYPE_INTEGER) col.setText(String.valueOf(ds.getInteger(i,j)),false);
		else if (type==DataSheet.COLTYPE_REAL) col.setText(String.valueOf(ds.getReal(i,j)),false);
		else if (type==DataSheet.COLTYPE_BOOLEAN) col.setText(ds.getBoolean(i,j) ? "true" : "false",false);
	    }
	}

	TrivialDOM.writeXML(out,xml);
    }

    // writing of datasheets to the MDL SD file format

    public static void writeSDF(OutputStream ostr,DataSheet ds) throws IOException
    {
	writeSDF(new BufferedWriter(new OutputStreamWriter(ostr)),ds);
    }
    public static void writeSDF(BufferedWriter out,DataSheet ds) throws IOException
    {
    	int molfld=-1;
	for (int n=0;n<ds.numCols();n++) if (ds.colType(n)==DataSheet.COLTYPE_MOLECULE) {molfld=n; break;}

	for (int i=0;i<ds.numRows();i++)
	{
	    if (molfld>=0) if (!ds.isNull(i,molfld))
	    {
	    	MoleculeStream.writeMDLMOL(out,ds.getMolecule(i,molfld));
	    }
	    for (int j=0;j<ds.numCols();j++) if (ds.colType(j)!=DataSheet.COLTYPE_MOLECULE && !ds.isNull(i,j))
	    {
	    	String line="";

		if (ds.colType(j)==DataSheet.COLTYPE_STRING) line=ds.getString(i,j);
		else if (ds.colType(j)==DataSheet.COLTYPE_INTEGER) line=String.valueOf(ds.getInteger(i,j));
		else if (ds.colType(j)==DataSheet.COLTYPE_REAL) line=String.valueOf(ds.getReal(i,j));
		else if (ds.colType(j)==DataSheet.COLTYPE_BOOLEAN) line=ds.getBoolean(i,j) ? "true" : "false";

    	    	if (line.length()==0) continue;
		String[] bits=line.split("\n");
		boolean anything=false;
		for (int n=0;n<bits.length;n++) if (bits[n].length()>0) anything=true;
		if (!anything) continue;

		out.write("> <"+ds.colName(j)+">\n");
		for (int n=0;n<bits.length;n++) if (bits[n].length()>0)
		{
		    if (bits[n].length()>78) bits[n]=bits[n].substring(0,78); // tuff
		    out.write(bits[n]+"\n");
		}
		out.write("\n");
	    }

	    out.write("$$$$\n");
	}

	out.flush();
    }
}
