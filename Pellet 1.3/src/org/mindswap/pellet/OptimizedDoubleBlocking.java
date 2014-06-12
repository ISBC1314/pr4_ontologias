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

import java.util.Iterator;
import java.util.List;

import org.mindswap.pellet.utils.ATermUtils;

import aterm.ATermAppl;
import aterm.ATermInt;

/**
 * @author Evren Sirin
 */
public class OptimizedDoubleBlocking extends Blocking {
	public boolean isDirectlyBlocked( Individual w, List ancestors ) {
		Iterator i = ancestors.iterator();			
		Iterator predecessors = w.getPredecessors().iterator();
		while( predecessors.hasNext() ) {
			Individual v = (Individual) predecessors.next();
			while(i.hasNext()) {
				Individual w1 = (Individual) i.next();
				
				if(v.equals(w1)) continue;
				
				boolean b1and2 = block1(w, w1) && block2(w, v, w1);
				boolean aBlock = b1and2	&& block3(w, v, w1) && block4(w, v, w1);

				if(aBlock) return true;				

				boolean cBlock = b1and2	&& block5(w, v, w1) && block6(w, v);

				if(cBlock) return true;
			}
		}
		
		return false;
	}

	private boolean block1(Individual w, Individual w1) {
		return subset(w, w1);
	}
	
	private boolean block2(Individual w, Individual v, Individual w1) {
		Iterator i = w1.getTypes(Node.ALL).iterator();
		while(i.hasNext()) {									
			ATermAppl av = (ATermAppl) i.next();	
			
			Role s = w.getABox().getRole(av.getArgument(0));
			ATermAppl c = (ATermAppl) av.getArgument(1);	
			
			if(s.isDatatypeRole())
				continue;
			
			Role invS = s.getInverse();
			
			if(v.hasRSuccessor(invS, w)) {
				if(!v.hasType(c))
					return false;
				
				Iterator j = s.getSubRoles().iterator();
				while(j.hasNext()) {									
					Role r = (Role) j.next();
					
					if(!r.isTransitive())
						continue;
					
					Role invR = r.getInverse();
					if(v.hasRSuccessor(invR, w)) {
						ATermAppl allRC = ATermUtils.makeAllValues(r.getName(), c);
						if(!v.hasType(allRC))
							return false;
					}
				}
			}
		}
		
		return true;
	}
	
	
	private boolean block3(Individual w, Individual v, Individual w1) {
		Iterator i = w1.getTypes(Node.MAX).iterator();
		while(i.hasNext()) {									
			ATermAppl mc = (ATermAppl) i.next();
			
			ATermAppl a = (ATermAppl) mc.getArgument(0);								
			Role s = w.getABox().getRole(a.getArgument(0));
			int n = ((ATermInt) a.getArgument(1)).getInt() - 1;		

			if(s.isDatatypeRole())
				continue;
			
			Role invS = s.getInverse();
			
			if(v.hasRSuccessor(invS, w)) {
				if(w1.getRSuccessors(s).size() >= n)
					return false;
			}
		}
		
		return true;
	}

	private boolean block4(Individual w, Individual v, Individual w1) {
		Iterator i = w1.getTypes(Node.MIN).iterator();
		while(i.hasNext()) {									
			ATermAppl mc = (ATermAppl) i.next();
			
			Role t = w.getABox().getRole(mc.getArgument(0));
			int m = ((ATermInt) mc.getArgument(1)).getInt();
			
			if(t.isDatatypeRole())
				continue;
			
			Role invT = t.getInverse();			
			if(!v.hasRSuccessor(invT, w) && w1.getRSuccessors(t).size() < m)
				return false;			
		}

		i = w1.getTypes(Node.SOME).iterator();
		while(i.hasNext()) {									
			ATermAppl sv = (ATermAppl) i.next();
			
			ATermAppl a = (ATermAppl) sv.getArgument(0);
			Role t = w.getABox().getRole(a.getArgument(0));
			
			if(t.isDatatypeRole())
				continue;
						
			Role invT = t.getInverse();
			if(!v.hasRSuccessor(invT, w) && !w1.hasRSuccessor(t))
				return false;			
		}
		
		return true;
	}
	
	private boolean block5(Individual w, Individual v, Individual w1) {
		Iterator i = w1.getTypes(Node.MAX).iterator();
		while(i.hasNext()) {									
			ATermAppl mc = (ATermAppl) i.next();
			
			ATermAppl a = (ATermAppl) mc.getArgument(0);								
			Role t = w.getABox().getRole(a.getArgument(0));

			if(t.isDatatypeRole())
				continue;
			
			Role invT = t.getInverse();
			
			if(v.hasRSuccessor(invT, w))
				return false;
		}
		
		return true;
	}	
	
	private boolean block6(Individual w, Individual v) {
		Iterator i = v.getTypes(Node.MIN).iterator();
		while(i.hasNext()) {									
			ATermAppl mc = (ATermAppl) i.next();
			
			Role u = w.getABox().getRole(mc.getArgument(0));

			if(u.isDatatypeRole())
				continue;
			
			if(v.hasRSuccessor(u, w))
				return false;			
		}
		
		return true;
	}	
}
