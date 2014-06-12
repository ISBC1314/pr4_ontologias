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


import java.util.BitSet;

/*
 * Created on Aug 27, 2003
 *
 */

/**
 * DependencySet for concepts and edges in the ABox for backjumping
 *
 * @author Evren Sirin
 *
 */
public class DependencySet {
    public static final int NO_BRANCH = -1;
    
    /**
     * An empty dependency set
     */
	public static final DependencySet EMPTY = new DependencySet();
	
	/**
	 * Used for assertions that are true by nature, i.e. an individual always
	 * has type owl:Thing
	 */
	public static final DependencySet INDEPENDENT = new DependencySet(0);
	
	/**
	 * A dummy dependency set that is used just to indicate there is a dependency
	 */
	public static final DependencySet DUMMY = new DependencySet(1);
	
	/**
	 * index of branches this assertion depends on 
	 */
	private BitSet depends;
	
	/**
	 * branch number when this assertion was added to ABox
	 */
	int branch = NO_BRANCH; 
	
	/**
	 * Create an empty set
	 */
	private DependencySet() {
		depends = new BitSet();
	}
	
	/**
	 * Create a dependecy set that depends on a single branch
	 * 
	 * @param branch Branch number
	 */
	public DependencySet(int branch) {
		this.depends = new BitSet();		
		
		depends.set(branch);
	}
	
	/**
	 * Creates a dependecy set with the given BitSet (no separate copy of BitSet is created 
	 * so if BitSet is modified this DependencySet will be affected).
	 */
	public DependencySet(BitSet depends) {
		this.depends = depends;		
	}
		
	/**
	 * Creates a new DependencySet object where the BitSet is shared (changing one will change
	 * the other).
	 * 
	 * @return
	 */
	public DependencySet copy() {
		return new DependencySet(this.depends);
	}

	/**
	 * Return true if <code>b</code> ic in this set.
	 * 
	 * @param b
	 * @return
	 */
	public boolean contains(int b) {
		return depends.get(b);
	}

	/**
	 * Add the integer value <code>b</code> to this DependencySet. 
	 * 
	 * @param b
	 */
	public void add(int b) {
		depends.set(b);
	}
	
	/**
	 * Remove the integer value <code>b</code> from this DependencySet. 
	 * 
	 * @param b
	 */
	public void remove(int b) {
		depends.clear(b);
	}	

	/**
	 * Return true if there is no dependancy on a non-deterministic branch
	 *  
	 * @return
	 */
	public boolean isIndependent() {
	    return max() <= 0;
	}
	
	/**
	 * Return the number of elements in this set.
	 * 
	 * @return
	 */
	public int size() {
		return depends.cardinality();
	}

	/**
	 * Return the maximum value in this set.
	 * 
	 * @return
	 */
	public int max() {
		return depends.length() - 1;
	}

	/**
	 * Create a new DependencySet and all the elements of <code>this</code> and
	 * <code>ds</code> (Neither set is affected when the return the set is modified).
	 * 
	 * @param ds
	 * @return
	 */
	public DependencySet union(DependencySet ds) {
		BitSet newDepends = (BitSet) depends.clone();
		newDepends.or(ds.depends);
		
		return new DependencySet(newDepends);
	}
	
	public String toString() {
		return "[" + branch + "-" + depends.toString() + "]";
	}
}
