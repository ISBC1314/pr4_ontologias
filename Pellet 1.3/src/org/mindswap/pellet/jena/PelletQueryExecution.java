/*
 * Created on Oct 16, 2004
 */
package org.mindswap.pellet.jena;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.exceptions.UnsupportedQueryException;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryException;
import com.hp.hpl.jena.query.QueryExecException;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.core.ResultBinding;
import com.hp.hpl.jena.query.core.Template;
import com.hp.hpl.jena.query.engine1.QueryEngineUtils;
import com.hp.hpl.jena.query.util.Context;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.util.FileManager;


/**
 * @author Evren Sirin
 */
public class PelletQueryExecution implements QueryExecution {
    public static Log log = LogFactory.getLog( PelletQueryExecution.class );
    
    private Query query;
    private Model source;

    public PelletQueryExecution( String query, Model source ) {        
        this( QueryFactory.create( query ), source );
    }

    public PelletQueryExecution( Query query, Model source ) {
        this.query = query;
        this.source = source;
        
        Graph graph = source.getGraph();
        if( !( graph instanceof PelletInfGraph ) )             
            throw new QueryException( "PelletQueryExecution can only be used with Pellet-backed models" );
    }

    public Model execDescribe() {
        throw new UnsupportedOperationException( "Not supported yet!" );
    }
    
    public Model execConstruct() {
        if ( ! query.isConstructType() )
            throw new QueryExecException("Attempt to get a CONSTRUCT model from a "+labelForQuery(query)+" query") ; 
            
        Model model = ModelFactory.createDefaultModel();

        model.setNsPrefixes( source );
        model.setNsPrefixes( query.getPrefixMapping() );

        ResultSet results = exec();
        Set set = new HashSet();

        Template template = query.getConstructTemplate();

        while( results.hasNext() ) {
            Map bNodeMap = new HashMap();
            QuerySolution qs = results.nextSolution();
            ResultBinding rb = (ResultBinding) qs;
            template.subst( set, bNodeMap, rb.getBinding() );
        }

        for( Iterator iter = set.iterator(); iter.hasNext(); ) {
            Triple t = (Triple) iter.next();
            Statement stmt = QueryEngineUtils.tripleToStatement( model, t );
            if( stmt != null ) model.add( stmt );
        }

        close();
        
        return model;
    }

    public boolean execAsk() {
        if ( ! query.isAskType() )
            throw new QueryExecException("Attempt to have boolean from a "+labelForQuery(query)+" query") ; 

        return exec().hasNext();
    }
    
    public ResultSet execSelect() {
        if ( ! query.isSelectType() )
            throw new QueryExecException("Attempt to have ResultSet from a "+labelForQuery(query)+" query") ; 

        return exec();
    }
    
    private ResultSet exec() {
        PelletInfGraph pelletInfGraph = (PelletInfGraph) source.getGraph();
        if( !pelletInfGraph.isPrepared() )
            pelletInfGraph.prepare();
        OWLReasoner reasoner = pelletInfGraph.getOWLReasoner();
        
        ResultSet results = null;
        try {
            // first try Pellet engine for ABox queries
            results = reasoner.execQuery( query );
        } catch( UnsupportedQueryException e ) {
            log.info( "This is not an ABox query: " + e.getMessage() );
            
            // fall back to Jena engine for mixed queries
            results = QueryExecutionFactory.create( query, source ).execSelect();
        }
        
        return results;
    }

    public void abort() {
        // nothing to do here                
    }

    public void close() {
        // nothing to do here
    }

    public void setFileManager( FileManager manager ) {
        // TODO Auto-generated method stub        
    }

    public void setInitialBinding( QuerySolution arg0 ) {
        throw new UnsupportedOperationException( "Not supported yet!" );
    }

    public Context getContext() {
        throw new UnsupportedOperationException( "Not supported yet!" );
    }

    static private String labelForQuery(Query q)
    {
        if ( q.isSelectType() )     return "SELECT" ; 
        if ( q.isConstructType() )  return "CONSTRUCT" ; 
        if ( q.isDescribeType() )   return "DESCRIBE" ; 
        if ( q.isAskType() )        return "ASK" ;
        return "<<unknown>>" ;
    }
}
