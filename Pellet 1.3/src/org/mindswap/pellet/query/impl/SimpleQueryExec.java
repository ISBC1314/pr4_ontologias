/*
 * Created on Jul 27, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.mindswap.pellet.query.impl;

import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.query.Query;
import org.mindswap.pellet.query.QueryEngine;
import org.mindswap.pellet.query.QueryExec;
import org.mindswap.pellet.query.QueryResultBinding;
import org.mindswap.pellet.query.QueryResults;

import aterm.ATermAppl;


/**
 * @author Daniel
 */
public class SimpleQueryExec implements QueryExec {
    public static Log log = LogFactory.getLog( QueryEngine.class );
    
    public SimpleQueryExec() {        
    }
    
    public boolean supports( Query q ) {
        return !q.getDistObjVars().isEmpty();
    }
    
	public QueryResults exec( Query q ) {
	    QueryResults results = new QueryResultsImpl( q );
	    KnowledgeBase kb = q.getKB();
	    
	    long satCount = kb.getABox().satisfiabilityCount;
	    
		HashMap varBindings = new HashMap();
		
		for ( Iterator v = q.getDistObjVars().iterator(); v.hasNext(); ) {
			ATermAppl currVar = (ATermAppl) v.next();
			ATermAppl rolledUpClass = q.rollUpTo( currVar );
			
            if( log.isTraceEnabled() )
                log.trace( "Rolled up class " + rolledUpClass );
			varBindings.put( currVar, kb.getInstances( rolledUpClass ) );
		}
		
        if( log.isTraceEnabled() )
            log.trace( "Var bindings: " + varBindings );
		
		Iterator i = new BindingIterator( q, varBindings );
		
		boolean hasLiterals = !q.getDistLitVars().isEmpty();

		if ( hasLiterals ) {
			while ( i.hasNext() ) {
			    QueryResultBinding b = (QueryResultBinding) i.next();

				Iterator l = new LiteralIterator( q, b );
				while ( l.hasNext() ) {
				    QueryResultBinding mappy = (QueryResultBinding) l.next();
					boolean queryTrue = QueryEngine.execBoolean( q.apply( mappy ) ); 
					if ( queryTrue ) 
						results.add( mappy );				
				}
			}		
		} else {
			while ( i.hasNext() ) {
			    QueryResultBinding b = (QueryResultBinding) i.next();
	
				boolean queryTrue = 
				    (q.getDistObjVars().size() == 1) || // if there is only a single var no need to verify
					QueryEngine.execBoolean( q.apply( b ) ); 
				if ( queryTrue ) 
					results.add( b );					
			}
		}
		
        if( log.isTraceEnabled() )
            log.trace( "Total satisfiability operations: " + (kb.getABox().satisfiabilityCount-satCount) );
		
		return results;
	}
	
}
