/*
$Id: ByteVector.java,v 1.2 2003/02/18 11:48:48 sander Exp $
*/


package fr.ove.utils;

public class ByteVector implements java.io.Serializable {
    protected byte elementData[];
    protected int elementCount;
    protected int capacityIncrement;

    public ByteVector(int initialCapacity, int capacityIncrement) {
    	super();
    	this.elementData = new byte[initialCapacity];
    	this.capacityIncrement = capacityIncrement;
    }

    public ByteVector(int initialCapacity) {
    	this(initialCapacity, 5);
    }

    public ByteVector() {
    	this(10);
    }

    public final synchronized void trimToSize() {
    	int oldCapacity = elementData.length;
    	if (elementCount < oldCapacity) {
    	    byte oldData[] = elementData;
    	    elementData = new byte[elementCount];
    	    System.arraycopy(oldData, 0, elementData, 0, elementCount);
    	}
    }

    public final int size() {
    	return elementCount;
    }

    public final boolean isEmpty() {
    	return elementCount == 0;
    }

    public final boolean contains(byte elem) {
    	return indexOf(elem, 0) >= 0;
    }

    public final int indexOf(byte elem) {
    	return indexOf(elem, 0);
    }

    public final synchronized int indexOf(byte elem, int index) {
    	for (int i = index; i < elementCount; i++) {
    	    if (elem == elementData[i])
        		return i;
    	}
    	return -1;
    }

    public final synchronized byte elementAt(int index) {
    	if (index >= elementCount)
    	    throw new ArrayIndexOutOfBoundsException(index + " >= " + elementCount);

    	try {
    	    return elementData[index];
    	} catch (ArrayIndexOutOfBoundsException e) {
    	    throw new ArrayIndexOutOfBoundsException(index + " < 0");
    	}
    }

    public final synchronized void addElement(byte b) {
	    ensureCapacity(elementCount + 1);
    	elementData[elementCount++] = b;
    }

    public final synchronized void addElements(byte[] b) {
        if (b != null) {
    	    ensureCapacity(elementCount + b.length);
            System.arraycopy(b, 0, elementData, elementCount, b.length);
            elementCount += b.length;
        }
        else
    	    throw new NullPointerException("Try to add null elements");
    }

    public final synchronized void addElements(byte[] b, int length) {
        if (b != null) {
            if ((length > 0) && (length <= b.length)) {
        	    ensureCapacity(elementCount + b.length);
                System.arraycopy(b, 0, elementData, elementCount, length);
                elementCount += length;
            }
            else
        	    throw new IllegalArgumentException("Invalid length argument : " + length);
        }
        else
    	    throw new NullPointerException("Try to add null elements");
    }

    public final synchronized boolean removeElement(byte b) {
    	int i = indexOf(b);
    	if (i >= 0) {
    	    removeElementAt(i);
    	    return true;
    	}
    	return false;
    }

    public final synchronized void removeElementAt(int index) {
    	if (index >= elementCount) {
    	    throw new ArrayIndexOutOfBoundsException(index + " >= " + elementCount);
    	}
    	else if (index < 0) {
    	    throw new ArrayIndexOutOfBoundsException(index);
    	}
    	int j = elementCount - index - 1;
    	if (j > 0) {
    	    System.arraycopy(elementData, index + 1, elementData, index, j);
    	}
    	elementCount--;
    }

    public final synchronized void removeAllElements() {
    	elementCount = 0;
    	elementData = new byte[capacityIncrement];
    }

    public final synchronized byte[] getBytes() {
        trimToSize();
	    return elementData;
    }

    public final synchronized void ensureCapacity(int minCapacity) {
    	int oldCapacity = elementData.length;
    	if (minCapacity > oldCapacity) {
    	    byte oldData[] = elementData;
    	    int newCapacity = (capacityIncrement > 0) ? (oldCapacity + capacityIncrement) : (oldCapacity * 2);
        	if (newCapacity < minCapacity)
        		newCapacity = minCapacity;
    	    elementData = new byte[newCapacity];
    	    System.arraycopy(oldData, 0, elementData, 0, elementCount);
    	}
    }

    public final synchronized String toString() {
        String s = " [ ";
    	for (int i = 0 ; i <elementCount ; i++)
    	    s += elementData[i] + " ";
    	s += "]";
    	return s;
    }
}
