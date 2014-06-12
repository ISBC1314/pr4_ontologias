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

import org.mindswap.pellet.utils.HashCodeUtil;



/*
 * Created on Aug 27, 2003
 *
 */

/**
 * @author Evren Sirin
 *
 */
public class Edge {
	private Individual from;
	private Node to;
	private Role role;
	
	private DependencySet depends;
	
	private int hashCode;
	
	public Edge(Role name, Individual from, Node to) {
		this.role = name;
		this.from = from;
		this.to = to;
		
		computeHashCode();
	}
	
	public Edge(Role name, Individual from, Node to, DependencySet d) {
		this.role = name;
		this.from = from;
		this.to = to;
		this.depends = d;
		
		computeHashCode();
	}
	
	public Node getNeighbor( Node node ) {
		if( from.equals( node ) )
            return to;
        else if( to.equals( node ) )
            return from;
        else
            return null;
	}
	
	public String toString() {
		return "[" + from + ", " + role + ", " + to + "] - " + depends; 
	}
	/**
	 * @return Returns the depends.
	 */
	public DependencySet getDepends() {
		return depends;
	}
	/**
	 * @return Returns the from.
	 */
	public Individual getFrom() {
		return from;
	}
	/**
	 * @return Returns the role.
	 */
	public Role getRole() {
		return role;
	}
	/**
	 * @return Returns the to.
	 */
	public Node getTo() {
		return to;
	}
	
    public boolean equals(Object other) {
        if(this == other) return true;
        if(!(other instanceof Edge)) return false;
        Edge that = (Edge) other;
        return from.equals(that.from) && role.equals(that.role) && to.equals(that.to);
    }

    private void computeHashCode() {
        hashCode = HashCodeUtil.SEED;
        hashCode = HashCodeUtil.hash(hashCode, from);
        hashCode = HashCodeUtil.hash(hashCode, role);
        hashCode = HashCodeUtil.hash(hashCode, to);            
    }
    
    public int hashCode() {
        return hashCode;
    }
}
