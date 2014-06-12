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

package org.mindswap.pellet.query.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mindswap.pellet.query.Query;
import org.mindswap.pellet.query.QueryResultBinding;

import aterm.ATermAppl;

public class BindingIterator implements Iterator {
	int[] indices;
	List[] varB;
	boolean more = true;
	Map binds;
	List vars;
	
	public BindingIterator( Query q, Map b ) {
		binds = b;
			
		vars = new ArrayList();
		
		Set distVars = q.getDistVars();
		for ( Iterator v = b.keySet().iterator(); v.hasNext(); ) {
			Object o = v.next();
			
			if ( distVars.contains( o ) ) {
				vars.add( o );
			}
		}
			
		varB = new ArrayList[vars.size()];
			
		for ( int i = 0; i < vars.size(); i++ ) {
			//System.out.println( binds.get( vars.get( i ) ).getClass() );
			varB[i] = new ArrayList( (Collection) binds.get( vars.get( i ) ) );	
			if ( varB[i].isEmpty() ) {
				more = false;
			}
		}
			
		indices = new int[ vars.size() ];
		//System.out.println( varB[0] );
			
	}
	
	private boolean incIndex( int index ) {		
		if ( indices[index] + 1 < varB[index].size() ) {
			indices[index]++;
		} else {
			if ( index == indices.length - 1 ) {
				return false;
			} else {
				indices[index] = 0;
				return incIndex( index + 1 );
			}
		}
		
		return true;
	}
		/* (non-Javadoc)
	 * @see java.util.Iterator#hasNext()
	 */
	public boolean hasNext() {
		return more;
	}

	/* (non-Javadoc)
	 * @see java.util.Iterator#next()
	 */
	public Object next() {
		if ( !more )
			return null;
		
		QueryResultBinding next = new QueryResultBindingImpl();
		
		for ( int i = 0; i < indices.length; i++ ) {
			next.setValue( (ATermAppl) vars.get( i ), (ATermAppl) varB[i].get( indices[i] ) );
		}	
		
		more = incIndex( 0 );
		
		return next;
	}

	/* (non-Javadoc)
	 * @see java.util.Iterator#remove()
	 */
	public void remove() {
		// TODO Auto-generated method stub
	}
}