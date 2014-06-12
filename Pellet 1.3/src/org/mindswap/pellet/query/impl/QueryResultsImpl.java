/*
 * Created on Jan 8, 2005
 */
package org.mindswap.pellet.query.impl;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.mindswap.pellet.output.ATermAbstractSyntaxRenderer;
import org.mindswap.pellet.output.ATermRenderer;
import org.mindswap.pellet.output.OutputFormatter;
import org.mindswap.pellet.output.TableData;
import org.mindswap.pellet.query.Query;
import org.mindswap.pellet.query.QueryResultBinding;
import org.mindswap.pellet.query.QueryResults;
import org.mindswap.pellet.query.QueryUtils;

import aterm.ATermAppl;

/**
 * @author Evren Sirin
 *
 */
public class QueryResultsImpl implements QueryResults {
    private Query query;
    private List results;
    
    public QueryResultsImpl(Query query) {
        this.query = query;
        results = new ArrayList();        
    }
    
    public void add(QueryResultBinding binding) {
        results.add(binding);
    }
    
    public boolean contains(QueryResultBinding binding) {
        return results.contains(binding);
    }
    

    public QueryResultBinding get(int index) {
        return (QueryResultBinding) results.get(index);
    }

    public int size() {
        return results.size();
    }

    public boolean isEmpty() {
        return results.isEmpty();
    }
    
    public Query getQuery() {
        return query;
    }

    /* (non-Javadoc)
     * @see org.mindswap.pellet.newquery.QueryResults#getResultVars()
     */
    public List getResultVars() {
        return query.getResultVars();
    }

    public TableData toTable() {
        return toTable( false );
    }
    
    public TableData toTable( boolean formatHTML ) {
        List resultVars = query.getResultVars();
        List colNames = new ArrayList(resultVars.size());
        for( int i = 0; i < resultVars.size(); i++ ) {
            ATermAppl var = (ATermAppl) resultVars.get(i);
            
            colNames.add(QueryUtils.getVarName(var));
        }
        
        StringWriter sw = new StringWriter();
        OutputFormatter formatter = new OutputFormatter( sw, formatHTML );

        ATermRenderer renderer = new ATermAbstractSyntaxRenderer();
        renderer.setWriter(formatter);        
        
        TableData table = new TableData(colNames);
        for( int i = 0; i < size(); i++ ) {
            QueryResultBinding binding = get(i);
            
            List list = new ArrayList();
            for(int j = 0; j < resultVars.size(); j++) {
                sw.getBuffer().setLength(0);
                ATermAppl var = (ATermAppl) resultVars.get(j);
                ATermAppl val = binding.getValue(var);
                
                if( val != null ) { 
                    renderer.visit(val);
                    list.add(sw.toString());
                }
                else
                    list.add("<<unbound>>");
            }
            
            table.add(list);
        }
        
        return table;
    }
    
    public String toString() {
        return toTable().toString();
    }
}
