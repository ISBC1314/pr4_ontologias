/*
 * Created on Jul 1, 2005
 */
package org.mindswap.pellet.jena;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.hp.hpl.jena.util.iterator.NiceIterator;


class CollectionsIterator extends NiceIterator {
    private Iterator top;
    private Iterator curr;
    
    public CollectionsIterator(Collection coll) {
        this.top = coll.iterator();
        this.curr = null;
    }

    public boolean hasNext() {
		while( curr == null || !curr.hasNext() ) {
			if( !top.hasNext() )
				return false;
			
			curr = ((Collection) top.next()).iterator();
		}
		return true;
    }

    public Object next() {
		if( !hasNext() )
			throw new NoSuchElementException();
		
		return curr.next();
    }
}