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

import org.mindswap.pellet.datatypes.Datatype;
import org.mindswap.pellet.datatypes.ValueSpace;

import aterm.ATermAppl;



class LiteralValueBranch extends Branch {
    int shuffle;
    Datatype datatype;
    
	LiteralValueBranch(ABox abox, CompletionStrategy strategy, Literal lit, Datatype datatype ) {
		super(abox, strategy, lit, DependencySet.INDEPENDENT, 
            datatype.size() == ValueSpace.INFINITE ? Integer.MAX_VALUE : datatype.size());
		
        this.shuffle = abox.getBranch();
        this.datatype = datatype;
	}		
		
	protected Branch copyTo(ABox abox) {
	    Literal x = abox.getLiteral(node.getName());
	    Branch b = new LiteralValueBranch(abox, null, x, datatype);
	    b.anonCount = anonCount;
	    b.nodeCount = nodeCount;
	    b.branch = branch;
	    b.nodeName = node.getName();
	    b.strategy = strategy;
	    
	    return b;
	}
	
	protected void tryBranch() {		
		abox.incrementBranch();
		
		DependencySet ds = termDepends;			
		for(; tryNext < tryCount; tryNext++) {	
            int tryIndex = (tryNext + shuffle) % tryCount;
            ATermAppl value = datatype.getValue( tryIndex );
//            System.out.println(tryNext + " + " + shuffle + " % " + tryCount + " == " + tryIndex + " " + value);

            if(ABox.DEBUG)						
				System.out.println( 
				    "LIT : (" + (tryNext+1) + "/" + tryCount + ") at branch (" + branch + ") " + 
				    " for literal " + node + " merge " + value + " " + ds);						
			
//            if(branch > 100)
//                System.out.println("DEBUG");
			ds = ds.union(new DependencySet(branch));
			
            Node y = node;
			Node z = abox.getNode( value );
            if( z == null ) {
                z = abox.addLiteral( value );
            }            
            
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
	    abox.setClash(Clash.unexplained(node, ds));
	
		return;
	}

	
	public String toString() {
		if(tryNext < tryCount)
			return "Branch " + branch + " max rule on " + node + " datatype  " + datatype;
		
		return "Branch " + branch + " max rule on " + node + " exhausted merge possibilities";
	}
}