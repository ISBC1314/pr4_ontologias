package com.lre.utils;

/**
 * <p>Title: Util</p>
 * <p>Description: A utility class that offers many static methods for performing routine operations</p>
 * <p>Copyright:    Copyright (c) 2003</p>
 * <p>Company: Lightning Round Entertainment</p>
 * @author Michael Grove
 * @version 1.0
 */

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Random;
import java.util.StringTokenizer;

public class Util {

  /**
   * Number of milliseconds in one minute.<br><br>
   */
 public static final long ONE_MINUTE = 1000*60;
 /**
  * Number of milliseconds in one hour<br><br>
  */
 public static final long ONE_HOUR = ONE_MINUTE*60;
 /**
  * Number of milliseconds in one day<br><br>
  */
 public static final long ONE_DAY = ONE_HOUR*24;
 /**
  * Number of milliseconds in one week<br><br>
  */
 public static final long ONE_WEEK = ONE_DAY*7;
 /**
  * Number of milliseconds in one year.<br><br>
  */
 public static final long ONE_YEAR = ONE_WEEK*52;

 /**
  * Returns a randomly generated string of up to 15 characters.<br><br>
  *
  * @return A randomly generated string
  */
 public static String getRandomString() { return getRandomString(15); }

 /**
  * Returns a randomly generated string of up to X characters.<br><br>
  *
  * @param numChars the number of characters long the random string should be
  *
  * @return A randomly generated string
  */
 public static String getRandomString(int numChars)
 {
   Random r = new Random(System.currentTimeMillis());
   int chars = r.nextInt(numChars);
   while (chars == 0)
     chars = r.nextInt(numChars);
   StringBuffer sb = new StringBuffer();
   for (int i = 0; i < chars; i++)
   {
     int index = 97+r.nextInt(26);
     char c = (char)index;
     sb.append(c);
   } // for
   return sb.toString();
 } // getRandomString

 /**
  * Merges the contents of two Hashtables.<br><br>
  *
  * @param one     Hashtable to merge
  * @param two     Hashtable to merge
  * @return A new hashtable that is the result of the merging of the two parameter hashtables
  */
 public static Hashtable mergeHashtables(Hashtable one, Hashtable two)
 {
   Hashtable newHash = (Hashtable)one.clone();
   Enumeration e = two.keys();
   while (e.hasMoreElements())
   {
     Object key = e.nextElement();
     if (!newHash.containsKey(key))
       newHash.put(key,two.get(key));
     else
     {
        // DUPs TAKEN CARE OF HERE
     } // else
   } // while
   return newHash;
 } // mergeHashtables

 /**
  * Returns true or false depending on whether or not the specified string is in the array.<br><br>
  *
  * @param elem     String to search for
  * @param array    String array to search in
  * @return true if elem is in the array, false otherwise
  */
 public static boolean isInArray(String elem, String[] array)
 {
   if (elem == null || array == null)
     return false;
   for (int i = 0; i < array.length; i++)
     if (array[i].equals(elem))
       return true;
   return false;
 } // isInArray

 /**
  * Returns true or false depending on whether or not the specified string is in the array.<br><br>
  *
  * @param elem     int to search for
  * @param array    int array to search through
  * @return true if elem is in array, false otherwise
  */
 public static boolean isInArray(int elem, int[] array)
 {
   if (array == null)
     return false;
   for (int i = 0; i < array.length; i++)
     if (array[i] == elem)
       return true;
   return false;
 } // isInArray

 /**
  * Adds an int to an int array.<br><br>
  *
  * @param elem     int to be added
  * @param intList array to which elem will be added
  * @return The array with the parameter added to the end.
  */
  public static int[] addToIntArray(int elem, int[] intList)
  {
    int[] temp = null;
    if (intList == null)
    {
      temp = new int[1];
      temp[0] = elem;
    } // if
    else
    {
      temp = new int[intList.length+1];
      System.arraycopy(intList,0,temp,0,intList.length);
      temp[intList.length] = elem;
    } // else
    return temp;
  } // addToIntArray

  /**
   * Adds an Object to an Object array.<br><br>
   *
   * @param elem     Object to be added
   * @param arr array to which elem will be added
   * @return The array with the parameter added to the end.
   */
   public static Object[] addToObjectArray(Object elem, Object[] arr)
   {
     Object[] temp = null;
     if (arr == null)
     {
       temp = new Object[1];
       temp[0] = elem;
     } // if
     else
     {
       temp = new Object[arr.length+1];
       System.arraycopy(arr,0,temp,0,arr.length);
       temp[arr.length] = elem;
     } // else
     return temp;
  } // addToObjectArray

  /**
   * Adds an String to an String array.<br><br>
   *
   * @param elem     String to be added
   * @param arr array to which elem will be added
   * @return The array with the parameter added to the end.
   */
   public static String[] addToStringArray(String elem, String[] arr)
   {
     String[] temp = null;
     if (arr == null)
     {
       temp = new String[1];
       temp[0] = elem;
     } // if
     else
     {
       temp = new String[arr.length+1];
       System.arraycopy(arr,0,temp,0,arr.length);
       temp[arr.length] = elem;
     } // else
     return temp;
  } // addToStringArray

  /**
   * Adds a Class to a Class array.<br><br>
   *
   * @param elem     Class to be added
   * @param arr array to which elem will be added
   * @return The array with the parameter added to the end.
   */
   public static Class[] addToClassArray(Class elem, Class[] arr)
   {
     Class[] temp = null;
     if (arr == null)
     {
       temp = new Class[1];
       temp[0] = elem;
     } // if
     else
     {
       temp = new Class[arr.length+1];
       System.arraycopy(arr,0,temp,0,arr.length);
       temp[arr.length] = elem;
     } // else
     return temp;
  } // addToClassArray

  /**
   * Given a string of delimited integers, return an int array.<br><br>
   *
   * @param str     String of delimited integers
   * @param delims String of delimiters used in the list, for example: ",;:"
   * @return int array whose contents reflect the list of integers in the parameter str
   * @throws java.lang.NumberFormatException if the string contents elements other than integers in the list
   */
  public static int[] readDelimitedIntegers(String str, String delims) throws NumberFormatException
  {
    int[] intList = null;
    if (str == null || str.equals(""))
      return new int[0];
    StringTokenizer toke = new StringTokenizer(str,delims);
    while (toke.hasMoreTokens())
    {
      String num = toke.nextToken();
      int col = -1;
      try {
        if (num.indexOf("-") != -1)
        {
          StringTokenizer st = new StringTokenizer(num,"-");
          int startInt = Integer.parseInt(st.nextToken());
          int endInt = Integer.parseInt(st.nextToken());
          for (int i = startInt; i <= endInt; i++)
            intList = addToIntArray(i,intList);
        } // if
        else
        {
          col = Integer.parseInt(num);
          intList = addToIntArray(col,intList);
        } // else
      } // try
      catch (NumberFormatException nfe) {
        System.out.println("Invalid format in exclude param.  Type \"int,int ... \" expected.");
        throw nfe;
      } // catch
    } // while
    return intList;
  } // readDelimitedIntegers

  /**
   * Inverts a hashtable so that its keys become values and the values become keys.
   * No validation is done on duplicates.<br><br>
   *
   * @param h     Hashtable to invert
   * @return the inverted hashtable
   */
  public static Hashtable invertHashtable(Hashtable h)
  {
    Enumeration e = h.keys();
    Hashtable hash = new Hashtable();
    while(e.hasMoreElements())
    {
      String key = e.nextElement().toString();
      hash.put(h.get(key),key);
    } // while
    return hash;
 } // invertHashtable

 /**
  * Given a string, replace all occurances of one string with another.<br><br>
  *
  * @param host     parent string
  * @param oldchar  String to remove
  * @param newchar  String to insert in the place of oldchar in the String host
  * @return copy of host with all occurances of param oldchar replaced with param newchar
  */
  public static String replace(String host, String oldchar, String newchar)
  {
    int index = 0;
    while (host.indexOf(oldchar,index) != -1) // diff, no index
    {
      index = host.indexOf(oldchar,index);
      host = host.substring(0,index)+newchar+host.substring(index+oldchar.length(),host.length());
      index += newchar.length(); // diff absent
    } // while
    return host;
  } // replace

  /**
   * Prints Hashtable to System.out.<br><br>
   * @see #printHashtable(Hashtable, java.io.PrintStream)
   */
  public static void printHashtable(Hashtable h) { printHashtable(h,System.out); }

  /**
   * Prints a hashtable in pairs: "key ==> value" to the specified output stream<br><br>
   *
   * @param h     Hashtable to print
   * @param out     Stream to output the string
   */
  public static void printHashtable(Hashtable h, java.io.PrintStream out)
  {
    Enumeration e = h.keys();
    while (e.hasMoreElements())
    {
      String key = e.nextElement().toString();
      String value = h.get(key).toString();
      out.println(key+" ==> "+value);
    } // while
  } // printHashtable

  /**
   * Prints Vector to System.out<br><br>
   * @see #printVector(Vector, java.io.PrintStream)
   */
  public static void printVector(Vector v) { printVector(v,System.out); }

  /**
   * Prints a vector via each elements toString() method and sends output to the specified stream<br><br>
   *
   * @param v     Vector to print
   * @param out     Stream to output the string
   * @see #printVector(Vector)
   */
  public static void printVector(Vector v, java.io.PrintStream out)
  {
    for (int i = 0; i < v.size(); i++)
      out.println(v.elementAt(i));
  } // printVector

  /**
   * Given a path to a file on the local disk, return the contents of that file as a String.<br><br>
   *
   * @param fn     Fully qualified file name to a file on the local disk
   * @return Contents of the file as a String
   * @throws java.io.IOException if there are problems opening or reading from the file
   * @throws java.io.FileNotFoundException if the file cannot be found
   */
  public static String getFileAsString(String fn) throws java.io.IOException, java.io.FileNotFoundException
  {
    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(fn)));
    StringBuffer theFile = new StringBuffer();
    String line = reader.readLine();
    while (line != null)
    {
      theFile.append(line+"\n");
      line = reader.readLine();
    } // while
    reader.close();
    return theFile.toString();
  } // getFileAsString

  /**
   * Saves a String to the specified file on the local disk.<br><br>
   *
   * @param toSave     String to save to the file
   * @param fn         file name of the file to which the String will be saved
   * @throws java.io.IOException if there are problems opening or reading the file
   * @throws java.io.FileNotFoundException if the file cannot be found or created
   */
  public static void saveStringToFile(String toSave, String fn) throws java.io.IOException, java.io.FileNotFoundException
  {
    java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(fn)));
    java.util.StringTokenizer st = new java.util.StringTokenizer(toSave,"\n",true);
    String s;
    while (st.hasMoreTokens())
    {
      s = st.nextToken();
      if (s.equals("\n"))
        writer.newLine();
      else writer.write(s);
    } // while
    writer.flush();
    writer.close();
  } // saveStringToFile

  /**
   * Given a URL, return the contents of that URL as a String.<br><br>
   *
   * @param theURL     URL to read from
   * @return Contents of the URL as a String
   * @throws java.io.IOException if there are problems opening or reading from the URL
   */
  public static String getURLAsString(java.net.URL theURL) throws java.io.IOException
  {
    StringBuffer content = new StringBuffer();
    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(theURL.openStream()));
    String line = reader.readLine();
    while (line != null)
    {
      content.append(line+"\n");
      line = reader.readLine();
    } // while
    reader.close();
    return content.toString();
  } // getURLAsString

  /**
   * Dynamically creates an object given its class name and the constructor params.<br><br>
   *
   * @param className      Fully qualified name of class to create
   * @param paramTypes     Constructor parameter types
   * @param params         Values for constructor (should be same size array as paramTypes)
   * @return a new instances of the specified class
   * @throws Exception propagates any exception thrown during the creation process
   */
  public static Object createObject(String className, Class[] paramTypes, Object[] params) throws Exception
  {
    Class theClass = Class.forName(className);
    java.lang.reflect.Constructor cons = theClass.getConstructor(paramTypes);
    return cons.newInstance(params);
  } // createObject

  /**
   * Clones a vector of strings.<br><br>
   *
   * @param stringVector      Vector of strings to clone
   * @return the cloned vector of strings
   */
  public static Vector cloneStringVector(Vector stringVector)
  {
    Vector v = new Vector();
    for (int i = 0; i < stringVector.size(); i++)
      v.addElement(new String(stringVector.elementAt(i).toString()));
    return v;
  } // cloneStringVector

  /**
   * Adds the item to the list model in "alphabetical" order via the compareTo function.<br><br>
   *
   * @param lm      ListModel to add the item to
   * @param li     Item to be added
   */
//  public static void addToListAlpha(DefaultListModel lm, ListItem li)
//  {
//    boolean in = false;
//    for (int i = 0; i < lm.size(); i++)
//    {
//      if (((ListItem)lm.elementAt(i)).getValue().toString().compareTo(li.getValue()) > 0)
//      {
//        lm.insertElementAt(li,i);
//        in = true;
//        break;
//      } // if
//    } // for
//    if (!in)
//      lm.addElement(li);
//  } // addToListAlpha

  /**
   * Reduces an images alpha values by 50%.  Works on Windows platforms only.<br><br>
   *
   * @param img     Image to fade
   * @return copy of the image with reduced alpha values
   */
  public static java.awt.Image getTranslucentVersionOf(java.awt.Image img)
  {
    int w = img.getWidth(null);
    int h = img.getHeight(null);
    int[] pix = new int[w*h];

    // get the pixels:
    java.awt.image.PixelGrabber pg = new java.awt.image.PixelGrabber(img, 0, 0, w, h, pix, 0, w);
    try {
      pg.grabPixels();
    } // try
    catch (Exception e) { return img; }

    // loop through the array, shift the alpha values
    for (int i=pix.length-1; i>=0; i--)
      pix[i] = 0x99000000 | (pix[i]&0x00ffffff);

    // make and return an Image of the array
    java.awt.image.MemoryImageSource mis = new java.awt.image.MemoryImageSource(w, h, pix, 0, w);
    return java.awt.Toolkit.getDefaultToolkit().createImage(mis);
  } // getTranslucentVersionOf

  public static String getLocalName(String uri) {
    return uri.substring(uri.indexOf("#") + 1);
  }

  public static String getFileUri(String uri) {
    return uri.substring(0, uri.indexOf("#"));
  }


  public static String[] split(String s, String token) {
    Vector result = new Vector();

    int length = token.length();
    int pos = s.indexOf(token);
    int lastPos = 0;
    while (pos >= 0) {
      result.addElement(s.substring(lastPos, pos));
      lastPos = pos + length;
      pos = s.indexOf(token, lastPos);
    } // while
    if(lastPos < s.length())
      result.addElement(s.substring(lastPos));
    String[] r = new String[result.size()];
    result.copyInto(r);
    return r;
  } // split

  public static String convertToUnicodeString(char ch)
  {
    if (Character.getNumericValue(ch) != -1)
      return String.valueOf(ch);
    String val = "\\u";
    int intVal = ch;
    int bit4 = (int)(intVal/Math.pow(16,3));
    intVal -= bit4*Math.pow(16,3);
    int bit3 = (int)(intVal/Math.pow(16,2));
    intVal -= bit3*Math.pow(16,2);
    int bit2 = (int)(intVal/Math.pow(16,1));
    intVal -= bit2*Math.pow(16,1);
    int bit1 = (int)(intVal/Math.pow(16,0));
    intVal -= bit1*Math.pow(16,0);
    if (intVal != 0)
      System.err.println("SHIT");
    return val+Character.toUpperCase(Integer.toHexString(bit4).charAt(0))+Character.toUpperCase(Integer.toHexString(bit3).charAt(0))+Character.toUpperCase(Integer.toHexString(bit2).charAt(0))+Character.toUpperCase(Integer.toHexString(bit1).charAt(0));
  } // convertToUnicodeString

  public static void main(String[] args)
  {
    System.err.println("LRE Utility Classes Test Package...");
  } // main

} // Util