package org.mindswap.pellet.servlet.functions;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.expr.NodeValue;
import com.hp.hpl.jena.query.function.FunctionBase1;
import com.hp.hpl.jena.query.util.Context;
import com.hp.hpl.jena.query.util.NodeUtils;
import com.hp.hpl.jena.query.util.Symbol;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.OWL;

public class IsCanonical extends FunctionBase1 {
	static Symbol CACHE = new Symbol("org.mindswap.pellet.servlet.function.IsCanonical.Cache");
	
	public static int cacheMisses = 0;
	public static int cacheHits = 0;
	private class NodeComparator implements Comparator {

		public int compare(Object arg0, Object arg1) {
			Node node0 = (Node) arg0;
			Node node1 = (Node) arg1;
			
			if (node0.isBlank() && node1.isURI()) {
				return 1;
			}
			if (node0.isURI() && node1.isBlank()) {
				return -1;
			}
			return NodeUtils.compareNodesByValue(node0, node1);
		}
		
	}
	
	private class NodeID {
		private Graph graph;
		private Node node;
		public NodeID(Graph graph, Node node) {
			this.graph = graph;
			this.node = node;
		}
		
		public boolean equals(Object object) {
			boolean result = false;
			if (object instanceof NodeID) {
				NodeID nodeId = (NodeID) object;
				result = graph.equals(nodeId.graph) && node.equals(nodeId.node);
			}
			return result;
		}
		public int hashCode() {
			return graph.hashCode() + node.hashCode();
		}
		
	}
	public IsCanonical() {
	}
	
	public NodeValue exec(NodeValue nodeValue) {
		boolean result;
		
		Node node = nodeValue.asNode();
		if (node.equals(getCanonical(node))) {
			result = true;
		} else {
			result = false;
		}
		return NodeValue.makeBoolean(result);
	}
	
	private Node getCanonical(Node node) {
		Node canonical;
		Context context = this.getContext().getContext();
		Graph graph = this.getContext().getActiveGraph();
		NodeID nodeID = new NodeID(graph, node);
		
		Map cache = (Map) context.get(CACHE);
		if (cache == null) {
			cache = new HashMap();
			context.put(CACHE, cache);
		}
		
		canonical = (Node) cache.get(nodeID);
		if (canonical == null) {
			cacheMisses += 1;
			Iterator equivalents = findSortedEquivalents(node);
			if (equivalents.hasNext()) {
				canonical = (Node) equivalents.next();
			} else {
				canonical = node;
			}
			cache.put(nodeID, canonical);
			
			while (equivalents.hasNext()) {
				Node equiv = (Node) equivalents.next();
				NodeID equivID = new NodeID(graph, equiv);
				cache.put(equivID, canonical);
			}
		} else {
			cacheHits += 1;
		}
		
		return canonical;
	}

	private Iterator findSortedEquivalents(Node node) {
		Graph graph = this.getContext().getActiveGraph();
		Set equivalents;
		
		if (!node.isLiteral()) {
			equivalents = new TreeSet(new NodeComparator());
			ExtendedIterator iter = graph.find(node, OWL.sameAs.asNode(), null);	
			iter = iter.andThen(graph.find(node, OWL.equivalentClass.asNode(), null));
			iter = iter.andThen(graph.find(node, OWL.equivalentProperty.asNode(), null));
			
			for (; iter.hasNext();) {
				Triple triple = (Triple) iter.next();
				equivalents.add(triple.getObject());
				equivalents.add(triple.getSubject());
			}
			
			
		} else {
			equivalents = Collections.EMPTY_SET;
		}
		
		return equivalents.iterator();
	}
}
