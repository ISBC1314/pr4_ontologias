package com.lre.graph;

/**
 * <p>Title: Edge</p>
 * <p>Description: An edge in a graph</p>
 * <p>Company: Lightning Round Entertainment, LLC</p>
 * @author Michael Grove
 * @version 1.0
 */

public class Edge implements Cloneable {
  private String label;
  private Vertex target;

  public Edge(String s, Vertex v) {
    target = v;
    label = s;
  } // cons

  public Object clone() { return new Edge(new String(label),(Vertex)target.clone()); }

  public boolean equals(Object obj)
  {
    if (obj instanceof Edge)
      if (((Edge)obj).getLabel().equals(getLabel()))// && target.equals(((Edge)obj).target))
        return true;
      else return false;
    else return false;
  } // equals

  public String getLabel() { return label; }
  public Vertex getTarget() { return target; }

  public String toString() { return label+" to "+target.getName(); }
} // Edge