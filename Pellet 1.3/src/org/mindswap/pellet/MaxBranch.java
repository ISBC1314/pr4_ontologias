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

import java.util.List;

import org.mindswap.pellet.exceptions.InternalReasonerException;



class MaxBranch extends Branch {
	List mergePairs;
	Role r;
	int n;

	MaxBranch(ABox abox, CompletionStrategy strategy, Individual x, Role r, int n, List mergePairs, DependencySet ds) {
		super(abox, strategy, x, ds, mergePairs.size());
		
		this.r = r;
		this.n = n;
		this.mergePairs = mergePairs;
	}		
		
	protected Branch copyTo(ABox abox) {
	    Individual x = abox.getIndividual(ind.getName());
	    Branch b = new MaxBranch(abox, null, x, r, n, mergePairs, termDepends);
	    b.anonCount = anonCount;
	    b.nodeCount = nodeCount;
	    b.branch = branch;
	    b.nodeName = ind.getName();
	    b.strategy = strategy;
	    
	    return b;
	}
	
	protected void tryBranch() {		
		abox.incrementBranch();
		
		DependencySet ds = termDepends;			
		for(; tryNext < tryCount; tryNext++) {	
			NodeMerge nm = (NodeMerge) mergePairs.get(tryNext);			
			Node y = abox.getNode(nm.y);
			Node z = abox.getNode(nm.z);
			
			if(ABox.DEBUG)						
				System.out.println( 
				    "MAX : (" + (tryNext+1) + "/" + mergePairs.size() + 
				    ") at branch (" + branch + ") to  " + ind + 
				    " for prop " + r + " merge " + y + " -> " + z + " " + ds);						
			
			ds = ds.union(new DependencySet(branch));
			
			EdgeList rNeighbors = ind.getRNeighborEdges(r);
			EdgeList yEdges = rNeighbors.getEdgesContaining(y);
			EdgeList zEdges = rNeighbors.getEdgesContaining(z);
			if(yEdges.isEmpty() || zEdges.isEmpty())
			    throw new InternalReasonerException( 
			        "An error occured related to the max cardinality restriction about "  + r);
			ds = ds.union(yEdges.edgeAt(0).getDepends());
			ds = ds.union(zEdges.edgeAt(0).getDepends());
			
			strategy.mergeTo(y, z, ds);

//			abox.validate();
			
			boolean earlyClash = abox.isClosed();
			if(earlyClash) {
				if(ABox.DEBUG)
					System.out.println("CLASH: Branch " + branch + " " + abox.getClash() + "!");

				DependencySet clashDepends = abox.getClash().depends;
				
				if(clashDepends.contains(branch)) {
					// we need a global restore here because the merge operation modified three
					// different nodes and possibly other global variables
					strategy.restore(this);
					
					// global restore sets the branch number to previous value so we need to
					// increment it again
					abox.incrementBranch();
									
                    setLastClash( clashDepends );
				}
				else
					return;
			} 
			else 
				return;	
		}
		
        ds = getCombinedClash();
        ds.remove( branch );
		if(abox.doExplanation())
		    abox.setClash(Clash.maxCardinality(ind, ds, r.getName(), n));
		else
		    abox.setClash(Clash.maxCardinality(ind, ds));
	
		return;
	}

	
	public String toString() {
		if(tryNext < mergePairs.size())
			return "Branch " + branch + " max rule on " + ind + " merged  " + mergePairs.get(tryNext);
		
		return "Branch " + branch + " max rule on " + ind + " exhausted merge possibilities";
	}
}