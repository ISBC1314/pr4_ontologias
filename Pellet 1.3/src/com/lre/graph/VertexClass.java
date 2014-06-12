package com.lre.graph;

/**
 * <p>Title: VertexClass</p>
 * <p>Description: A collection of similar vertices used when determing if two graphs are isomorphic</p>
 * <p>Company: Lightning Round Entertainment, LLC</p>
 * @author Michael Grove
 * @version 1.0
 */

import java.util.Vector;

public class VertexClass {
  private Vector vertices;
  private int num;

  public VertexClass(int n)
  {
    num = n;
    vertices = new Vector();
  } // cons

  public VertexClass(int n, Vector v)
  {
    num = n;
    vertices = v;
  } // cons

  public void addVertexToClass(Vertex v) { vertices.addElement(v); }

  public boolean isInClass(Vertex v)
  {
    for (int i = 0; i < vertices.size(); i++)
    {
      Vertex curr = (Vertex)vertices.elementAt(i);
      if (curr.matches(v))
        return true;
    } // for
    return false;
  } // isInClass

  public boolean matches(VertexClass vc)
  {
    if (vc.getNum() != getNum())
      return false;

    if (vc.getVertices().size() != getVertices().size())
      return false;

    boolean match = true;
    Vector oneUnmatched = new Vector();
    Vector twoUnmatched = (Vector)vc.getVertices().clone();
    for (int i = 0; i < getVertices().size(); i++)
    {
      boolean matchCurr = false;
      Vertex local = (Vertex)getVertices().elementAt(i);
      for (int j = 0; j < vc.getVertices().size(); j++)
      {
        Vertex other = (Vertex)vc.getVertices().elementAt(j);
        if (local.matches(other))
        {
          twoUnmatched.remove(other);
          matchCurr = true;
          break;
        } // if
      } // for
      if (!matchCurr)
      {
        //match = false;
        //break;
        oneUnmatched.addElement(local);
      } // if
    } // for
    if (oneUnmatched.size() == 0 && twoUnmatched.size() == 0)
      return true;
    else
    {
      // didnt match them all, wanna get mappings between leftover vars to see if they are equal
      for (int i = 0; i < oneUnmatched.size(); i++)
      {
        Vertex one = (Vertex)oneUnmatched.elementAt(i);
        boolean matchCurr = false;
        for (int j = 0; j < twoUnmatched.size(); j++)
        {
          Vertex two = (Vertex)twoUnmatched.elementAt(j);
          //if (!one.isLeaf() && !two.isLeaf() && one.similar(two) && one.matches(two,one.getName()) && two.matches(one,two.getName()))

          // only want to structure check anon nodes...otherwise non identical literal nodes could get passed off as identical
//          if (!one.getName().startsWith("_:") && !two.getName().startsWith("_:"))
//            continue;
          if (one.similar(two))// && one.matches(two,one.getName()) && two.matches(one,two.getName()))
          {
            matchCurr = true;
            break;
          } // if
        } // for
        if (!matchCurr)
          return false;
      } // for
    } // else
    return match;
  } // matches

/*
  public boolean matches(VertexClass vc)
  {
    if (vc.getNum() != getNum())
      return false;
    if (vc.getVertices().size() != getVertices().size())
      return false;

    boolean match = true;
    Vector oneUnmatched = new Vector();
    Vector twoUnmatched = org.mindswap.utils.Util.cloneStringVector(vc.getVertices());
    for (int i = 0; i < getVertices().size(); i++)
    {
      boolean matchCurr = false;
      Vertex local = (Vertex)getVertices().elementAt(i);
      for (int j = 0; j < vc.getVertices().size(); j++)
      {
        Vertex other = (Vertex)vc.getVertices().elementAt(j);
        if (local.matches(other))
        {
          twoUnmatched.remove(other);
          matchCurr = true;
          break;
        } // if
      } // for
      if (!matchCurr)
      {
        //match = false;
        //break;
        oneUnmatched.addElement(local);
      } // if
    } // for
    if (oneUnmatched.size() == 0 && twoUnmatched.size() == 0)
      return true;
    else
    {
    } // else
    return match;
  } // matches
  */
  public int getNum() { return num; }
  public Vector getVertices() { return vertices; }
} // VertexClass
