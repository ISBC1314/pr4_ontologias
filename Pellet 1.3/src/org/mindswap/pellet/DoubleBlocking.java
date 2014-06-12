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
import java.util.Set;

import org.mindswap.pellet.utils.SetUtils;

/**
 * @author Evren Sirin
 */
public class DoubleBlocking extends Blocking {
	public boolean isDirectlyBlocked( Individual x, List ancestors ) {
		// 1) x has ancestors x1, y and y1
		// 2) x is a successor of x1 and y is a successor of y1
	    // 3) y, x and all nodes in between are blockable
		// 4) types(x) == types(y) && types(x1) == types(y1)
		// 5) edges(x1, x) == edges(y1, y)
		
		// FIXME can y1 be a nominal? (assumption: yes)
	    // FIXME can y and x1 be same? (assumption: no)

	    // we need at least two ancestors (y1 can be a nominal so it
	    // does no need to be included in the ancestors list)
	    if( ancestors.size() < 2 )
	        return false;
	    
		Iterator i = ancestors.iterator();			
		
		// first element is guaranteed to be x's predecessor 
		Individual x1 = (Individual) i.next();

		while( i.hasNext() ) {
			Individual y = (Individual) i.next();

			// if this is concept satisfiability then y might be a root node but
			// not necessarily a nominal and included in the ancestors list
			if( y.isRoot() )
			    return false;
			
			// y1 is not necessarily in the ancestors list (it might be a nominal)
			Individual y1 = y.getParent();
			
			Set xEdges = x.getInEdges().getRoles(); // all the incoming edges should be coming from x1
			Set yEdges = y.getInEdges().getRoles(); // all the incoming edges should be coming from y1

			if( equals( x, y ) && equals( y1, x1 ) && SetUtils.equals( xEdges, yEdges ) )
				return true;					
		}
		
		return false;
	}
}
