/*
 * Created on Jan 8, 2005
 */
package org.mindswap.pellet.query.impl;

import org.mindswap.pellet.query.Query;
import org.mindswap.pellet.query.QueryResultBinding;
import org.mindswap.pellet.query.QueryResults;

/**
 * @author Evren Sirin
 *
 */
public class MultiQueryResults extends QueryResultsImpl implements QueryResults {
    private QueryResults[] queryResults;

    private int size;

    public MultiQueryResults(Query query, QueryResults[] queryResults) {
        super( query );

        this.queryResults = queryResults;

        for(int i = 0; i < queryResults.length; i++) {
            size += queryResults[i].size();
        }
    }

    public void add( QueryResultBinding binding ) {
        throw new UnsupportedOperationException( "MultiQueryResults do not support addition!" );
    }

    public boolean contains( QueryResultBinding binding ) {
        for(int i = 0; i < queryResults.length; i++) {
            if( queryResults[i].contains( binding ) ) return true;
        }

        return false;
    }

    public QueryResultBinding get( int index ) {
        if( index > size ) throw new IndexOutOfBoundsException();

        QueryResultBinding result = new QueryResultBindingImpl();

        for(int i = queryResults.length - 1; i >= 0; i--) {
            int k = index % queryResults[i].size();
            index = (index - k) / queryResults[i].size();

            result.setValues( queryResults[i].get( k ) );
        }

        return result;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }
}
