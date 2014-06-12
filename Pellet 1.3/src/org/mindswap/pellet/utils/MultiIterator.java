/*
 * Created on Jul 1, 2005
 */
package org.mindswap.pellet.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class MultiIterator implements Iterator {
    private List list = new ArrayList(2);

    private int index = 0;

    private Iterator curr;

    public MultiIterator( Iterator first ) {
        curr = first;
    }

    public MultiIterator(Iterator first, Iterator second) {
        curr = first;
        list.add( second );
    }
    
    public boolean hasNext() {
        while( !curr.hasNext() && index < list.size() )
            curr = (Iterator) list.get( index++ );
        
        return curr.hasNext();
    }

    public Object next() {
        if( !hasNext() )
            throw new NoSuchElementException( "multi iterator" );
        
        return curr.next(); 
    }


    public void append( Iterator other ) {
        if( other instanceof MultiIterator )
            list.addAll( ((MultiIterator) other).list );
        else
            list.add( other );
    }

    public void remove() {
        curr.remove();
    }
}