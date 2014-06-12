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

import org.mindswap.pellet.utils.ATermUtils;



class GuessBranch extends Branch {
	List mergePairs;
	Role r;
	
	int minGuess;

	GuessBranch(ABox abox, CompletionStrategy strategy, Individual x, Role r, int minGuess, int maxGuess, DependencySet ds) {
		super(abox, strategy, x, ds, maxGuess - minGuess + 1);
		
		this.r = r;
		this.minGuess = minGuess;
	}		
		
	protected Branch copyTo(ABox abox) {
	    Individual x = abox.getIndividual(ind.getName());
	    Branch b = new GuessBranch(abox, null, x, r, minGuess, minGuess + tryCount - 1, termDepends);
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
		     // start with max possibility and decrement at each try  
		    int n = minGuess + tryCount - tryNext - 1;
			
			if(ABox.DEBUG)						
				System.out.println( 
				    "GUES: (" + (tryNext+1) + "/" + tryCount + 
				    ") at branch (" + branch + ") to  " + ind + 
                    " -> " + r + " -> anon" + (n == 1 ? "" : 
                    (abox.anonCount + 1) + " - anon") +
                    (abox.anonCount + n) + " " + ds);						

			ds = ds.union( new DependencySet( branch ) );
			
			// add the max cardinality for guess
			strategy.addType( ind, ATermUtils.makeNormalizedMax(r.getName(), n), ds);
			
			// create n distinct nominal successors
            Individual[] y = new Individual[n];
            for(int c1 = 0; c1 < n; c1++) {
                y[c1] = abox.addFreshIndividual();
                y[c1].setNominalLevel( 1 );
                y[c1].depth = 1;

                strategy.addEdge( ind, r, y[c1], ds );
                for(int c2 = 0; c2 < c1; c2++)
                    y[c1].setDifferent( y[c2], ds );
            }

            // add the min cardinality restriction just to make early clash detection easier
			strategy.addType( ind, ATermUtils.makeMin(r.getName(), n), ds);

			
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
		abox.setClash(Clash.unexplained(ind, ds));
	
		return;
	}

	
	public String toString() {
		if(tryNext < mergePairs.size())
			return "Branch " + branch + " max rule on " + ind + " merged  " + mergePairs.get(tryNext);
		
		return "Branch " + branch + " max rule on " + ind + " exhausted merge possibilities";
	}
}