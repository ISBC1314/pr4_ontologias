/*
 * Created on Jan 9, 2005
 */
package org.mindswap.pellet.query.impl;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.exceptions.UnsupportedFeatureException;
import org.mindswap.pellet.query.Query;
import org.mindswap.pellet.query.QueryEngine;
import org.mindswap.pellet.query.QueryExec;
import org.mindswap.pellet.query.QueryPattern;
import org.mindswap.pellet.query.QueryResults;
import org.mindswap.pellet.utils.Bool;

import aterm.ATermAppl;

/**
 * @author Evren Sirin
 *
 */
public class NoDistVarsQueryExec implements QueryExec {
    public static Log log = LogFactory.getLog( QueryEngine.class );
    
    private KnowledgeBase kb;
    
    public NoDistVarsQueryExec() {        
    }
    
    public boolean supports( Query q ) {
        return q.getDistVars().isEmpty();
    }
    
    public QueryResults exec( Query query ) {
		QueryResultsImpl results = new QueryResultsImpl( query );	

		boolean success = execBoolean( query );

		if( success )
		    results.add(new QueryResultBindingImpl());
            
        return results;    
    }
    
    public boolean execBoolean( Query query ) {				
		boolean querySatisfied; 
		
		kb = query.getKB();
	    
		if( query.getConstants().isEmpty() ) {
		    throw new UnsupportedFeatureException( 
		        "Executing queries with no constants is not implemented yet!" );
		}
		
		//if(Query.DEBUG) System.out.println( "Boolean query: " + query);
		
		// unless proven otherwise all (ground) triples are satisfied
		Bool allTriplesSatisfied = Bool.TRUE;

	    List patterns = query.getQueryPatterns();
		for(int i = 0; i < patterns.size(); i++) {
			QueryPattern triple = (QueryPattern) patterns.get(i);

			// by default we don't know if triple is satisfied
			Bool tripleSatisfied = Bool.UNKNOWN;
			// we can only check ground triples
			if( triple.isGround() ) {
				tripleSatisfied = triple.isTypePattern()
					? kb.isKnownType( triple.getSubject(), triple.getObject() )		
				    : kb.hasKnownPropertyValue( triple.getSubject(), triple.getPredicate(), triple.getObject());
			}
			
			// if we cannot decide the truth value of this triple (without a consistency
			// check) then over all truth value cannot be true. However, we will continue 
		    // to see if there is a triple that is obviously false  	
			if( tripleSatisfied.isUnknown() )
			    allTriplesSatisfied = Bool.UNKNOWN;
			else if( tripleSatisfied.isFalse() ) {
			    // if one triple is false then the whole query, which is the conjunction of
			    // all triples, is false. We can stop now.
			    allTriplesSatisfied = Bool.FALSE;
			    
                if( log.isTraceEnabled() )
                    log.trace( "Failed triple: " + triple );
			    
			    break;
			}					
		}
		
		// if we reached a verdict, return it
		if( allTriplesSatisfied.isKnown() ) 
		    querySatisfied = allTriplesSatisfied.isTrue();
		else {
		    // do the unavoidable consistency check
		    ATermAppl testInd = (ATermAppl) query.getConstants().iterator().next();
			ATermAppl testClass = query.rollUpTo( testInd );
		
            if( log.isTraceEnabled() )
                log.trace( "Boolean query: " + testInd + " -> " + testClass );
			    
			querySatisfied = kb.isType( testInd, testClass );  		    
		}       

		return querySatisfied;		    
    }
}
