/*
 * Created on Jul 1, 2005
 */
package org.mindswap.pellet.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import aterm.ATerm;
import aterm.ATermList;

public class MultiListIterator implements Iterator {
    private List list = new ArrayList( 2 );

    private int index = 0;

    private ATermList curr;

    public MultiListIterator( ATermList first ) {
        curr = first;
    }

    public boolean hasNext() {
        while( curr.isEmpty() && index < list.size() )
            curr = (ATermList) list.get( index++ );
        
        return !curr.isEmpty();
    }

    public Object next() {
        if( !hasNext() )
            throw new NoSuchElementException();
                
        ATerm next = curr.getFirst();
        
        curr = curr.getNext();
        
        return next; 
    }

    public void append( ATermList other ) {
        list.add( other );
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}