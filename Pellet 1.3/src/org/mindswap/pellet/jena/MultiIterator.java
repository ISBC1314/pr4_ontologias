/*
 * Created on Jul 1, 2005
 */
package org.mindswap.pellet.jena;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.hp.hpl.jena.util.iterator.ClosableIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.NiceIterator;

public class MultiIterator extends NiceIterator {
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
    
    public static MultiIterator create( Iterator it ) {
        return (it instanceof MultiIterator) ? (MultiIterator) it : new MultiIterator( it );
    }

    public boolean hasNext() {
        while( !curr.hasNext() && index < list.size() )
            curr = (Iterator) list.get( index++ );
        
        return curr.hasNext();
    }

    public Object next() {
        return hasNext() ? curr.next() : noElements( "multi iterator" );
    }

    public void close() {
        close(curr);
        for(int i = index; i < list.size(); i += 1)
            close((Iterator) list.get(i));
    }

    public ExtendedIterator andThen(ClosableIterator other) {
        if( other instanceof MultiIterator )
            list.addAll( ((MultiIterator) other).list );
        else
            list.add( other );
        return this;
    }
}