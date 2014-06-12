/*
 * Created on Oct 16, 2004
 */
package org.mindswap.pellet.jena;

import org.mindswap.pellet.exceptions.UnsupportedFeatureException;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdql.Query;
import com.hp.hpl.jena.rdql.QueryEngine;
import com.hp.hpl.jena.rdql.QueryExecution;
import com.hp.hpl.jena.rdql.QueryResults;

/**
 * @author Evren Sirin
 */
public class PelletQueryEngine implements QueryExecution {
    private Query query;
    private OWLReasoner reasoner;
    private Model source;
    
    private boolean initialized = false;

    public PelletQueryEngine( String query, Model source ) {        
        this.query = new Query( query );
        this.source = source;
    }

    public PelletQueryEngine( Query query ) {
        this.query = query;
        this.source = query.getSource();
    }
    
    public void init() {
        if( !initialized ) {
            Graph graph = source.getGraph();
            if(graph instanceof PelletInfGraph) {
	            PelletInfGraph pelletInfGraph = (PelletInfGraph) source.getGraph();
	            if( !pelletInfGraph.isPrepared() )
	                pelletInfGraph.prepare();
                reasoner = pelletInfGraph.getOWLReasoner();
            }
            
	        if( reasoner == null ) {
	            reasoner = new OWLReasoner();
	            reasoner.load( source );
	        }        
    	}
    	
    	initialized = true;             
    }

    public QueryResults exec() {        
        init();
        
        QueryResults results = null;
        try {
            // first try Pellet engine for ABox queries
            if( query.getConstraints().isEmpty() )
                results = reasoner.runQuery( query );
            else
                results = new QueryEngine( query ).exec();
        } catch(UnsupportedFeatureException e) {
            System.out.println( "INFO: This is not an ABox query: " + e );
            
            // fall back to Jena engine for TBox queries
            results = new QueryEngine( query ).exec();
        }
        
        return results;
    }

    public void abort() {
        // nothing to do here                
    }

    public void close() {
        // nothing to do here
    }

    public static QueryResults exec( Query query ) {
        return new PelletQueryEngine( query ).exec();
    }

}
