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

import java.util.Arrays;
import java.util.Comparator;

import org.mindswap.pellet.exceptions.InternalReasonerException;

import aterm.ATermAppl;

/**
 * @author Evren Sirin
 */
public class DisjunctionSorting {
	public static ATermAppl[] sort(final Individual node, ATermAppl[] disjunctions) {
	    Comparator comparator = new Comparator() {
            public int compare(Object o1, Object o2) {
                ATermAppl d1 = (ATermAppl) o1;
                ATermAppl d2 = (ATermAppl) o2;
                return node.getDepends(d1).max() - node.getDepends(d2).max();
            }	           
        };
        
	    if( PelletOptions.USE_DISJUNCTION_SORTING == PelletOptions.OLDEST_FIRST ) {
	        Arrays.sort( disjunctions, comparator );
	        
	        return disjunctions;
	    }
	    else 
	        throw new InternalReasonerException( "Unknown disjunction sorting option " + PelletOptions.USE_DISJUNCTION_SORTING );
	}
}
