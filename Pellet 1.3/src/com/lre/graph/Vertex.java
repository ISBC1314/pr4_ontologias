package com.lre.graph;

/**
 * <p>Title: Vertex</p>
 * <p>Description: A vertex in a graph</p>
 * <p>Company: Lightning Round Entertainment, LLC</p>
 * @author Michael Grove
 * @version 1.0
 */

import java.util.Vector;

import com.lre.utils.Util;

public class Vertex implements Cloneable {
  private String name;
  private Vector edgeList;

  public Vertex(String s) {
    name = s;
    edgeList = new Vector();
  } // cons

  public Object clone()
  {
    Vertex v = new Vertex(new String(name));
    Vector edges = new Vector();
    for (int i = 0; i < edgeList.size(); i++)
      edges.addElement(((Edge)edgeList.elementAt(i)).clone());
    v.edgeList = edges;
    return v;
  } // clone

  public String getName() { return name; }
  public void addEdge(Edge e) { edgeList.addElement(e); }
  public int countEdges() { return edgeList.size(); }
  public Edge getEdgeAt(int i) { return (Edge)edgeList.elementAt(i); }

  public boolean matches(Vertex v)
  {
    if (name.equals(v.getName()))
    {
//      for (int i = 0; i < edgeList.size(); i++)
//        if (getEdgeAt(i).getLabel().equals(v.getEdgeAt(i))
//System.err.println("similar? "+similar(v));
      //return true;
      return edgesMatch(v);
    } // if
    else return false;
  } // matches

  public boolean equals(Object obj)
  {
    if (obj instanceof Vertex)
      return ((Vertex)obj).matches(this);
    else return false;
  } // equals

  public boolean hasNeighbor(Vertex v)
  {
    for (int i = 0; i < countEdges(); i++)
    {
      Edge e = getEdgeAt(i);
      if (e.getTarget().equals(v))
        return true;
    } // for
    return false;
  } // hasNeighbor

  public boolean hasNeighbor(Vertex v, String edgeName)
  {
    for (int i = 0; i < countEdges(); i++)
    {
      Edge e = getEdgeAt(i);
      if (e.getTarget().equals(v) && e.getLabel().equals(edgeName))
        return true;
    } // for
    return false;
  } // hasNeighbor

  public Vector listNeighbors()
  {
    Vector v = new Vector();
    for (int i = 0; i < countEdges(); i++)
    {
      Edge e = getEdgeAt(i);
      if (!v.contains(e.getTarget()))
        v.addElement(e.getTarget());
    } // for
    return v;
  } // listNeighbors

  public boolean similar(Vertex v) { return edgesMatch(v); }

  private boolean edgesMatch(Vertex v)
  {
    if (v.countEdges() != countEdges())
      return false;

    int count = 0;
    int[] matched = new int[0];
    for (int i = 0; i < v.countEdges(); i++)
      for (int j = 0; j < countEdges(); j++)
        if (v.getEdgeAt(i).equals(getEdgeAt(j)) && !Util.isInArray(j,matched))
        {
          count++;
          Util.addToIntArray(j,matched);
          break;
        } // if
    if (count == countEdges())
      return true;
    else return false;
  } // matches

  public boolean isLeaf() { return edgeList.size() == 0; }

  public String toString()
  {
    StringBuffer sb = new StringBuffer();
    sb.append(name);
    for (int i = 0; i < countEdges(); i++)
      sb.append("\n"+"\t"+getEdgeAt(i).getLabel()+" --> "+getEdgeAt(i).getTarget().getName());
    return sb.toString();
  } // toString

} // Vertex