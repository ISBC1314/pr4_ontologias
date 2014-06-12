/*
 * Created on Jan 8, 2005
 */
package org.mindswap.pellet.query;

import java.util.List;

import org.mindswap.pellet.output.TableData;

/**
 * @author Evren Sirin
 *
 */
public interface QueryResults {
    public void add(QueryResultBinding binding);
    
    public QueryResultBinding get(int index);
    
    public boolean contains(QueryResultBinding binding);
    
    public int size();
    
    public boolean isEmpty();
    
    public Query getQuery();
    
    public List getResultVars();
    
    public TableData toTable();
    
    public TableData toTable( boolean formatHTML );
}
