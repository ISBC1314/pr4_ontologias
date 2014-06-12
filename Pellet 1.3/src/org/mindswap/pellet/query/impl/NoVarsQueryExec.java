/*
 * Created on Jan 9, 2005
 */
package org.mindswap.pellet.query.impl;

import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.exceptions.UnsupportedFeatureException;
import org.mindswap.pellet.query.Query;
import org.mindswap.pellet.query.QueryExec;
import org.mindswap.pellet.query.QueryResults;

import aterm.ATermAppl;

/**
 * @author Evren Sirin
 *
 */
public class NoVarsQueryExec implements QueryExec {
    public QueryResults exec( Query query ) {
		QueryResultsImpl results = new QueryResultsImpl( query );	

		boolean success = execBoolean( query );

		if( success )
		    results.add(new QueryResultBindingImpl());
            
        return results;    
    }
    
    public boolean execBoolean( Query query ) {
		KnowledgeBase kb = query.getKB();
	    
		if( query.getConstants().isEmpty() ) {
		    throw new UnsupportedFeatureException( 
		        "Execution queries with no constants and no results variables is not implemented yet!" );
		}
		
	    ATermAppl testInd = (ATermAppl) query.getConstants().iterator().next();
		ATermAppl testClass = query.rollUpTo( testInd );
	
		return kb.isType( testInd, testClass );        
    }

    public boolean supports( Query q ) {
        return q.getVars().isEmpty();
    }
}
