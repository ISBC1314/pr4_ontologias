//The MIT License
//
//Copyright (c) 2003 Ron Alford, Mike Grove, Bijan Parsia, Evren Sirin
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to
//deal in the Software without restriction, including without limitation the
//rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
//sell copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in
//all copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
//FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
//IN THE SOFTWARE.

package org.mindswap.pellet;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/*
 * Created on Aug 27, 2003
 *
 */

/**
 * @author Evren Sirin
 *
 */
public class EdgeList {
	private List list;
	
	public EdgeList() {
		list = new ArrayList();
	}

	public EdgeList(int size) {
		list = new ArrayList(size);
	}
	
	public EdgeList(EdgeList edges) {
		list = new ArrayList(edges.list);
	}
	
	/**
	 * Create an immutable singleton EdgeList;
	 * 
	 * @param edge
	 */
	public EdgeList(Edge edge) {
		list = Collections.singletonList( edge );
	}	

	public void addEdgeList(EdgeList edges) {
		// just a precaution
		if(edges == null)
			return;

		list.addAll(edges.list);		
	}
		
	public void addEdge(Edge e) {
		list.add(e);
	}
	
	public boolean removeEdge(Edge edge) {
	    return list.remove(edge);			
	}
	
	public Edge edgeAt(int i) {
		return (Edge) list.get(i);
	}
	
	public int size() {
		return list.size();
	}
	
	public EdgeList sort() {
	    EdgeList sorted = new EdgeList( this );
	    Collections.sort( sorted.list, new Comparator() {
            public int compare(Object o1, Object o2) {
                Edge e1 = (Edge) o1;
                Edge e2 = (Edge) o2;
                return e1.getDepends().max() - e2.getDepends().max();
            }});
	    return sorted;
	}
	
	public boolean isEmpty() {
		return list.isEmpty();
	}
	
	public Iterator iterator() {
		return list.iterator();
	}
	
	private EdgeList findEdges(Role role, Individual from, Node to) {
		EdgeList result = new EdgeList();
		
		for(int i = 0, n = list.size(); i < n; i++) {
			Edge e = (Edge) list.get(i);
            if( (from == null || from.equals( e.getFrom() )) &&
                (role == null || e.getRole().isSubRoleOf(role)) &&
                (to == null || to.equals( e.getTo() )) )
				result.addEdge(e);
		}		
		
		return result;
	}
	
	public EdgeList getEdgesFromTo(Individual from, Node to) {
		return findEdges(null, from, to);	
	}
	
	public EdgeList getEdgesFrom(Individual from) {
		return findEdges(null, from, null);	
	}

	public EdgeList getEdgesTo(Node to) {
		return findEdges(null, null, to);	
	}
		
	public EdgeList getEdges(Role role) {
		EdgeList result = new EdgeList();
		
		for(int i = 0, n = list.size(); i < n; i++) {
			Edge e = (Edge) list.get(i);
            if( e.getRole().isSubRoleOf(role))
				result.addEdge(e);
		}		
		
		return result;
		
//		return findEdges(role, null, null);	
	}

	public EdgeList getEdgesContaining(final Node node) {
		EdgeList result = new EdgeList();
		
		for(int i = 0, n = list.size(); i < n; i++) {
			Edge e = (Edge) list.get(i);
            if( e.getFrom().equals(node) || e.getTo().equals(node) )
				result.addEdge(e);
		}
		
		return result;
	}
	
	public Set getPredecessors() {
		Set result = new HashSet();
		
		for(int i = 0, n = list.size(); i < n; i++) {
			Edge edge = (Edge) list.get(i); 
			result.add(edge.getFrom());
		}
		
		return result;
	}
	
	public Set getSuccessors() {
		Set result = new HashSet();
		
		for(int i = 0, n = list.size(); i < n; i++) {
			Edge edge = (Edge) list.get(i); 
			result.add(edge.getTo());
		}
		
		return result;
	}

	public Set getRoles() {
		Set result = new HashSet();
		
		for(int i = 0, n = list.size(); i < n; i++) {
			Edge edge = (Edge) list.get(i); 
			result.add(edge.getRole());
		}
		
		return result;
	}

    public Set getNeighborNames(Individual node) {
        Set result = new HashSet();
        
        for(int i = 0, n = list.size(); i < n; i++) {
            Edge edge = (Edge) list.get(i); 
            result.add(edge.getNeighbor(node).getName());
        }
        
        return result;
    }
    
	public Set getNeighbors( Node node ) {
		Set result = new HashSet();
		
		for(int i = 0, n = list.size(); i < n; i++) {
			Edge edge = (Edge) list.get(i); 
			result.add(edge.getNeighbor(node));
		}
		
		return result;
	}
	
	public Set getFilteredNeighbors(Individual node) {
		Set result = new HashSet();
		
		String lang = null;
		for(int i = 0, n = list.size(); i < n; i++) {
			Edge edge = (Edge) list.get(i); 
			Node neighbor = edge.getNeighbor(node);
			
			if(neighbor instanceof Literal) {
			    Literal lit = (Literal) neighbor;
			    if(lang == null) {
			        lang = lit.getLang();
			        result.add(neighbor);
			    }
			    else if(lang.equals(lit.getLang())) {
			        result.add(neighbor);
			    }
			}
			else
			    result.add(neighbor);
		}
		
		return result;
	}
	
	public boolean hasEdgeFrom(Individual from) {
		return hasEdge(from, null, null);
	}
	
	public boolean hasEdgeFrom(Individual from, Role role) {
		return hasEdge(from, role, null);
	}
	
	public boolean hasEdgeTo(Node to) {
		return hasEdge(null, null, to);
	}

	public boolean hasEdgeTo(Role role, Node to) {
		return hasEdge(null, role, to);
	}
	
	public boolean hasEdge(Role role) {
		return hasEdge(null, role, null);
	}
	
	public boolean hasEdge(Individual from, Role role, Node to) {
		for(int i = 0, n = list.size(); i < n; i++) {
			Edge e = (Edge) list.get(i);
            if( (from == null || from.equals( e.getFrom() )) &&
                (role == null || e.getRole().isSubRoleOf(role)) &&
                (to == null || to.equals( e.getTo() )) )
				return true;
		}		
		
		return false;
	}
	
	public boolean hasEdge(Edge e) {
		return hasEdge(e.getFrom(), e.getRole(), e.getTo());
	}

	public String toString() {
		return list.toString(); 
	}
}
