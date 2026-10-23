/*
 * Created on 28.09.2004
 *
 */
package rene.zirkel.graphics;

import java.awt.*;

/**
 * @author Rene
 * An internal class to hold a font sructure (size, face
 * and font object)
 */
public class FontStruct
{	final int max=4;
	int Size[]=new int[max];
	boolean Bold[]=new boolean[max];
	Font F[]=new Font[4];
	int Next=0;

	public void storeFont (int size, boolean bold, Font f)
	{	if (Next>=max) Next=0;
		Size[Next]=size;
		Bold[Next]=bold;
		F[Next]=f;
		Next++;
	}
	
	public Font getFont (int size, boolean bold)
	{	for (int i=0; i<max; i++)
		{	if (F[i]==null) break;
			if (Size[i]==size && Bold[i]==bold)
				return F[i];
		}
		return null;
	}

	public static void main(String[] args)
	{}
}
