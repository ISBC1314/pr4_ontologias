package com.lre.graph;

/**
 * <p>Title: Graph</p>
 * <p>Description: A graph</p>
 * <p>Company: Lightning Round Entertainment, LLC</p>
 * @author Michael Grove
 * @version 1.0
 */

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;

public class Graph {
  private Hashtable vertices;
  // used for path finding
  private static Vector mVisited;

  public Graph() {
    vertices = new Hashtable();
  } // cons

  public void connect(Vertex start, Vertex end, String edgeLabel)
  {
    start.addEdge(new Edge(edgeLabel,end));
  } // connect

  public boolean hasPath(Vertex start, Vertex end)
  {
    mVisited = new Vector();
    return pathExists(start,end);
  } // hasPath

  private boolean pathExists(Vertex start, Vertex end)
  {
    mVisited.addElement(start);
    if (start.hasNeighbor(end))
      return true;
    else
    {
      Vector v = start.listNeighbors();
      for (int i = 0; i < v.size(); i++)
      {
        Vertex vert = (Vertex)v.elementAt(i);
        if (!mVisited.contains(vert))
          if (pathExists(vert,end))
            return true;
      } // for
    } // else
    return false;
  } // isPath

  public Vertex getVertex(String name)
  {
    if (vertices.containsKey(name))
      return (Vertex)vertices.get(name);
    else return null;
  } // getVertex

  public Enumeration getVertexKeys() { return vertices.keys(); }

  public void addVertex(Vertex v)
  {
    vertices.put(v.getName(),v);
  } // addVertex

  public int numVertices()
  {
    return vertices.size();
  } // numVertices

  public String toString()
  {
    StringBuffer sb = new StringBuffer();
    Enumeration e = vertices.keys();
    while (e.hasMoreElements())
    {
      String key = e.nextElement().toString();
      Vertex v = (Vertex)vertices.get(key);
      sb.append(v.toString()+"\n");
    } // while
    return sb.toString();
  } // toString

  protected Vector classify()
  {
    Vector classes = new Vector();
    Hashtable temp = new Hashtable();
    Enumeration e = vertices.keys();
    while (e.hasMoreElements())
    {
      Vertex curr = (Vertex)vertices.get(e.nextElement().toString());
      Integer key = new Integer(curr.countEdges());
      VertexClass vc;
      if (temp.containsKey(key))
        vc = (VertexClass)temp.get(key);
      else
      {
        vc = new VertexClass(curr.countEdges());
        classes.addElement(vc);
      } // else
      vc.addVertexToClass(curr);
      temp.put(key,vc);
    } // while
    return classes;
  } // classify

} // Graph