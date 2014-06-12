package com.lre.graph;

/**
 * <p>Title: Semantic Web Parser</p>
 * <p>Description: A Parser for the RDF/XML semantic web languages</p>
 * <p>Company: Lightning Round Entertainment, LLC</p>
 * @author Michael Grove
 * @version 1.0
 */

public class UndirectedGraph extends Graph {
  public UndirectedGraph() {
    super();
  } // cons

  public void connect(Vertex start, Vertex end, String edgeLabel)
  {
    start.addEdge(new Edge(edgeLabel,end));
    end.addEdge(new Edge(edgeLabel,start));
  } // connect

} // UndirectedGraph