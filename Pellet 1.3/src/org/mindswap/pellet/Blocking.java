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
public abstract class Blocking {
	/**
	 * Return true if first node contains all the types y has
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	protected boolean subset(Individual x, Individual y) {
		Set xTypes = x.getTypes();
		Set yTypes = y.getTypes();
		
		return SetUtils.subset(xTypes, yTypes);			
	}
	
	/**
	 * Return true if both individuals has exactly same set of types
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	protected boolean equals(Individual x, Individual y) {
		Set xTypes = x.getTypes();
		Set yTypes = y.getTypes();
		
		return SetUtils.equals(xTypes, yTypes);			
	}
	
	public boolean isBlocked( Individual x ) {
		if( x.isNominal() ) return false;
		
//		Timer t = x.getABox().getKB().timers.startTimer("blocking");

		List ancestors = x.getAncestors();
		boolean isBlocked = 
			isIndirectlyBlocked( ancestors ) || 
			isDirectlyBlocked( x, ancestors );
		
//		t.stop();
		
		return isBlocked;	 
	}
	
	public boolean isIndirectlyBlocked(Individual x) {
		if( x.isNominal() ) return false;

		return isIndirectlyBlocked( x.getAncestors() );
	}
	
	// FIXME this can be made much more efficient
	private boolean isIndirectlyBlocked( List ancestors ) {
		Iterator i = ancestors.iterator();			
		while(i.hasNext()) {
			Individual ancestor = (Individual) i.next();
			
			if( isDirectlyBlocked( ancestor, ancestor.getAncestors() ) )
				return true;			
		}	

		return false;
	}
	
	public abstract boolean isDirectlyBlocked( Individual x, List ancestors );
}
