// The MIT License
//
// Copyright (c) 2003 Ron Alford, Mike Grove, Bijan Parsia, Evren Sirin
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to
// deal in the Software without restriction, including without limitation the
// rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
// sell copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
// IN THE SOFTWARE.

/*
 * Created on May 4, 2004
 */
package org.mindswap.pellet;

import java.util.HashMap;
import java.util.Map;

import org.mindswap.pellet.utils.ATermUtils;

import aterm.ATermAppl;


abstract class Branch {
	ABox abox;
	CompletionStrategy strategy;
	int branch;
	int tryCount;
	int tryNext;
	ATermAppl nodeName;
	Node node;
    Individual ind;
	DependencySet termDepends;
    private DependencySet prevDS;
	
	// store things that can be changed after this branch
	int anonCount;
	int nodeCount;
	
	Map info;
	
	Branch(ABox abox, CompletionStrategy strategy, Node x, DependencySet ds, int n) {
		this.abox = abox;
		this.strategy = strategy;
		node = x;
        if( node instanceof Individual )
            ind = (Individual) node;
		nodeName = node.getName();
		termDepends = ds;
		tryCount = n;
		prevDS = DependencySet.EMPTY;
		tryNext = 0;			
		
		branch 	  = abox.getBranch();
		anonCount = abox.anonCount;
		nodeCount = abox.nodes.size();
        
        ATermUtils.assertTrue( tryCount > 0 );
	}
    
    public void setLastClash( DependencySet ds ) {
        prevDS = prevDS.union( ds );
    }
	
    public DependencySet getCombinedClash() {
        return prevDS;
    }
    
	public void setStrategy(CompletionStrategy strategy) {
	    this.strategy = strategy;
	}
	
	public boolean tryNext() {	
        if(abox.isClosed())
            return false;
       
		tryBranch();
			
		if( abox.isClosed() ) 
			abox.getClash().depends.remove(branch);
		
		return !abox.isClosed();
	}
	
	protected abstract Branch copyTo(ABox abox);
	
	protected abstract void tryBranch();
	
	void put(String key, Object obj) {
	    if(info == null)
	        info = new HashMap();
	    info.put(key, obj);
	}

	Object get(String key) {
	    return info.get(key);
	}
	
	public String toString() {
		return "Branch " + branch + " (" + tryCount + ")";
	}
}