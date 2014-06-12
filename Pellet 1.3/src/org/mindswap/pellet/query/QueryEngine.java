// The MIT License
//
// Copyright (c) 2003 Ron Alford, Mike Grove, Bijan Parsia, Evren Sirin
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to
// deal in the Software without restriction, including without limitation the
// rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
// sell copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
// IN THE SOFTWARE.

/*
 * Created on Jul 26, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.mindswap.pellet.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.exceptions.InternalReasonerException;
import org.mindswap.pellet.query.impl.ARQParser;
import org.mindswap.pellet.query.impl.DistVarsQueryExec;
import org.mindswap.pellet.query.impl.ImprovedARQParser;
import org.mindswap.pellet.query.impl.MultiQueryResults;
import org.mindswap.pellet.query.impl.NoDistVarsQueryExec;
import org.mindswap.pellet.query.impl.OptimizedQueryExec;
import org.mindswap.pellet.query.impl.QueryImpl;
import org.mindswap.pellet.query.impl.QueryResultBindingImpl;
import org.mindswap.pellet.query.impl.QueryResultsImpl;
import org.mindswap.pellet.query.impl.SimpleQueryExec;
import org.mindswap.pellet.utils.PermutationGenerator;
import org.mindswap.pellet.utils.Timer;

import aterm.ATermAppl;

import com.hp.hpl.jena.query.Syntax;


/**
 * @author Evren Sirin
 */
public class QueryEngine {
    public static Log log = LogFactory.getLog( QueryEngine.class );
    
    public static Syntax DEFAULT_SYNTAX = Syntax.syntaxSPARQL;
    
    private static DistVarsQueryExec distVars = new DistVarsQueryExec();
    private static OptimizedQueryExec optimized = new OptimizedQueryExec();
    private static SimpleQueryExec simple = new SimpleQueryExec();
    private static NoDistVarsQueryExec noVars = new NoDistVarsQueryExec();
    
    private static QueryExec[] queryExecs = 
    	{noVars, distVars, optimized, simple};
    
    private static QuerySplitter splitter = new QuerySplitter();

    public static QueryParser createParser() {
        return createParser( DEFAULT_SYNTAX );
    }
    
    public static QueryParser createParser( Syntax syntax ) {
        ARQParser parser = PelletOptions.USE_NEW_QUERY_PARSER
            ? new ImprovedARQParser()
            : new ARQParser();   
        parser.setSyntax( syntax );

        return parser;
    }

    public static QueryResults exec( String queryStr, KnowledgeBase kb ) {    
	    return exec( queryStr, kb, DEFAULT_SYNTAX );	    
	}
    
    public static QueryResults execRDQL( String queryStr, KnowledgeBase kb ) {
	    return exec( queryStr, kb, Syntax.syntaxRDQL );	    
	}
    
    public static QueryResults execSPARQL( String queryStr, KnowledgeBase kb ) {
	    return exec( queryStr, kb, Syntax.syntaxSPARQL );	    
	}

    public static Query parse( String queryStr, KnowledgeBase kb ) {
        return parse( queryStr, kb, DEFAULT_SYNTAX );       
    }
    
    public static Query parse( String queryStr, KnowledgeBase kb, Syntax syntax ) {    
        QueryParser parser = createParser( syntax );
        Query query = parser.parse( queryStr, kb );
        
        return query;
    }
    
    public static QueryResults exec( String queryStr, KnowledgeBase kb, Syntax syntax ) {    
	    Query query = parse( queryStr, kb, syntax );
	    
	    return exec( query );
	}

    public static QueryResults exec( Query query ) {
        query.prepare();
        
        if( query.getQueryPatterns().isEmpty() ) {
    		QueryResultsImpl results = new QueryResultsImpl( query );	
   		    results.add(new QueryResultBindingImpl());
                
            return results;             
        }
        else if( query.isGround() ) {
            return noVars.exec( query );
        }
            
        List queries = PelletOptions.SPLIT_QUERY              
            ? splitter.split( query )
            : Collections.singletonList( query );
        
        if( queries.isEmpty() ) {
    	    throw new InternalReasonerException( "Splitting query returned no results!" );
        }
        else if( queries.size() == 1 ) {
            return execSingleQuery( (Query) queries.get( 0 ) );
        }
        else {    
            QueryResults[] results = new QueryResults[ queries.size() ];
            for( int i = 0; i < queries.size(); i++ ) {                
                Query qry = (Query) queries.get( i );
                results[i] = execSingleQuery( qry );
            }
            
            return new MultiQueryResults( query, results );
        }
	}
    
    private static QueryResults execSingleQuery( Query query ) {
        query.prepare();
        
        if( PelletOptions.REORDER_QUERY ) {
//            System.out.println( "Before reorder:\n" + query );
            query = reorder( query );
        }
        
//        System.out.println( "Query:\n" + query );
        
        for( int i = 0; i < queryExecs.length; i++ ) {
            if( queryExecs[ i ].supports( query ) ) {               
                Timer timer = query.getKB().timers.startTimer( "Query" );
                QueryExec queryExec = queryExecs[ i ];
                QueryResults results = queryExec.exec( query );
                timer.stop();
                
                return results;
            }
        }
        
        // this should never happen
	    throw new InternalReasonerException( "Cannot determine which query engine to use" );
    }
    
    private static Query reorder( Query query ) {        
        double minCost = Double.POSITIVE_INFINITY;
        
        prepare( query );
        
        QueryCost queryCost = new QueryCost( query.getKB() );
        
        List patterns = query.getQueryPatterns();
        List bestOrder = null;
        int n = patterns.size();
        
        PermutationGenerator gen = new PermutationGenerator( n );
        for(int i = 0; gen.hasMore(); i++) {
            int[] perm = gen.getNext();
            List newPatterns = new ArrayList();
            for(int j = 0; j < n; j++) 
                newPatterns.add( patterns.get( perm[j] ) );

            double cost = queryCost.estimateCost( newPatterns );
            if( cost < minCost ) {
                minCost = cost;
                bestOrder = newPatterns;
            }
        }
        
        if( bestOrder == patterns )
            return query;
        
        Query newQuery = new QueryImpl( query.getKB() );
        for(int j = 0; j < n; j++) {
            newQuery.addPattern( (QueryPattern) bestOrder.get( j ) );
        }
        for( Iterator j = query.getResultVars().iterator(); j.hasNext(); ) {
            ATermAppl var = (ATermAppl) j.next();
            newQuery.addResultVar( var );
        }
        return newQuery;
    }
    
    public static void prepare( Query query ) {
        prepare( new Query[] { query } );
    }
    
    public static void prepare( Query[] queries ) {
        if( !PelletOptions.REORDER_QUERY )
            return;
        
        KnowledgeBase kb = queries[0].getKB();

        Set concepts = new HashSet();
        Set properties = new HashSet();        
        for(int j = 0; j < queries.length; j++) {
            Query query = queries[j];
            List patterns = query.getQueryPatterns();
            for( int i = 0; i < patterns.size(); i++ ) {
                QueryPattern pattern = (QueryPattern) patterns.get( i );

                if( pattern.isTypePattern() )
                    concepts.add( pattern.getObject() );
                else
                    properties.add( pattern.getPredicate() );
            }
        }
        
        kb.getSizeEstimate().compute( concepts, properties );
    }
    
    public static boolean execBoolean( Query query ) {
	    return noVars.execBoolean( query );
	}
}
