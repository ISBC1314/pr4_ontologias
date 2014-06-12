/*
 * Created on Jul 27, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.mindswap.pellet.query.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.exceptions.UnsupportedFeatureException;
import org.mindswap.pellet.query.Query;
import org.mindswap.pellet.query.QueryEngine;
import org.mindswap.pellet.query.QueryParser;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Namespaces;
import org.mindswap.pellet.utils.QNameProvider;
import org.mindswap.pellet.utils.URIUtils;

import aterm.ATermAppl;


/**
 * @author Evren Sirin
 */
public class SimpleRDQLParser implements QueryParser {
    public static Log log = LogFactory.getLog( QueryEngine.class );
    
    private QNameProvider qnames;
    private Query query;
    private KnowledgeBase kb;
    
    public SimpleRDQLParser() {
        qnames = new QNameProvider();
        
        qnames.setMapping("rdf", Namespaces.RDF);
        qnames.setMapping("rdfs", Namespaces.RDFS);
        qnames.setMapping("owl", Namespaces.OWL);
    }
    
	public Query parse(InputStream in, KnowledgeBase kb) throws IOException {
		return parse( new InputStreamReader(in), kb );
	}
	
	public Query parse(Reader in, KnowledgeBase kb) throws IOException {
		StringBuffer queryString = new StringBuffer();
		BufferedReader r = new BufferedReader( in );
		
		String line = r.readLine();
		while ( line != null ) {
			queryString.append( line ).append("\n");
			line = r.readLine();
		}
		
		return parse( queryString.toString(), kb );
	}
	
	public Query parse( String queryStr, KnowledgeBase kb ) {
	    this.kb = kb;
	    query = new QueryImpl( kb );

	    queryStr = queryStr.trim();
	    
	    String[] clauses = 
		    Pattern.compile("WHERE", Pattern.CASE_INSENSITIVE).split( queryStr );
	    
	    String selectClause = clauses[0].substring( "SELECT".length() + 1 ).trim();
	    clauses =
	        Pattern.compile("USING", Pattern.CASE_INSENSITIVE).split( clauses[1] );
	    String whereClause = clauses[0].trim();
	    String usingClause = ( clauses.length > 1 ) ? clauses[1] : null;
	    
	    parseUsing( usingClause );
	    parseWhere( whereClause );
	    parseSelect( selectClause );

		return query;
	}
	
	private void parseUsing( String usingClause ) {
	    if( usingClause == null ) 
	        return;
	    
        String[] ns = usingClause.split( "," );
        for( int i = 0; i < ns.length; i++ ) {
            String[] str = Pattern.compile(" FOR ", Pattern.CASE_INSENSITIVE).split( ns[i], 2 );
            
            if( str.length != 2 )
                throw new UnsupportedFeatureException( "Invalid prefix declaration: " + ns[i] );
            
            String prefix = str[0].trim();
            String uri = str[1].trim();
            
    		if( !uri.startsWith("<") || !uri.endsWith( ">" ) )
    	    	throw new UnsupportedOperationException( "The URI should be written between '<' and '>': " + ns[i]);
    	    	
    	    uri = uri.substring( 1 , uri.length() - 1 );
    	        
            qnames.setMapping( prefix, uri );
        }	    
	}
	
	private void parseWhere( String whereClause ) {
	    String patterns[] = whereClause.split( "\\)" );	    
	    for( int i = 0; i < patterns.length; i++ ) {
	        String pattern = patterns[i].trim();
	        // there can be an optional , between triple patterns
	        if(pattern.startsWith( "," ))
	            pattern = pattern.substring(1).trim();
	        // get rid of '('
	        pattern = pattern.substring(1); 
	        
	        String[] nodes = pattern.split( "," );
	        ATermAppl subj = makeTerm( nodes[0] );
	        ATermAppl pred = makeTerm( nodes[1] );
	        ATermAppl obj = makeTerm( nodes[2] );
	        
	        if( pred.getName().equals( Namespaces.RDF + "type") ) {
			    if( ATermUtils.isVar( obj ) )
			        throw new UnsupportedFeatureException("Variables cannot be used as objects of rdf:type triples in ABoxQuery");
			    
				query.addTypePattern(subj, obj);		
				
			} else if ( ATermUtils.isVar( pred ) ) {
				throw new UnsupportedFeatureException("Variables cannot be used in predicate position in AboxQuery");
			} else if (	pred.getName().startsWith(Namespaces.RDF) ||
					    pred.getName().startsWith(Namespaces.OWL) ||
					    pred.getName().startsWith(Namespaces.RDFS) ) {
		
				// this is to make sure no TBox, RBox queries are encoded in RDQL
				throw new UnsupportedFeatureException("Predicates that belong to [RDF, RDFS, OWL] namespaces cannot be used in ABoxQuery" );	
			} else {
				if ( !kb.isProperty( pred ) ) 
					throw new UnsupportedFeatureException( "Property " + pred + " is not present in KB." );
				else if ( kb.isDatatypeProperty( pred ) && query.getDistVars().contains( obj ) && ATermUtils.isVar( subj ) ) { 
					log.warn( "Warning: Forcing variable " + subj + " to be distinguished (Subject of datatype triple)" );
					
					query.addDistVar( subj );
				}
				
				query.addEdgePattern(subj, pred, obj);
			}
		}
	}
	
	private void parseSelect( String selectClause ) {
	    if( selectClause.equals( "*" ) ) {
	        for( Iterator i = query.getVars().iterator(); i.hasNext(); ) {
	            ATermAppl var = (ATermAppl) i.next();
	            query.addResultVar( var );
	        }
	    }
	    else {
	        String[] vars = selectClause.split( "," );
	        for( int i = 0; i < vars.length; i++ ) {
	            ATermAppl var = makeTerm( vars[i] );
	            query.addResultVar( var );
	        }
	    }
	}

	private ATermAppl makeTerm( String name ) {
	    name = name.trim();
	    
		if( name.startsWith("?") ) 
			return ATermUtils.makeVar( name.substring(1) );
		
		if( name.startsWith("<") ) {
	    	if( !name.endsWith( ">" ) )
	    	    throw new UnsupportedOperationException( "URI missing closing '>': " + name);
	    	
	        name = name.substring( 1 , name.length() - 1 );
		}
		else {
	    	String[] str = name.split(":");
	    	
	    	if( str.length != 2 )
	    	    throw new UnsupportedOperationException( "Invalid qname: " + name);
	    	
	    	String ns = qnames.getURI( str[0] );
	    	
	    	if( ns == null )
	    	    throw new UnsupportedOperationException( "Prefix '" + name + "' not defined");
	    	
	    	name = ns + str[1];
		}

		if( URI.create( name ) == null )
		    throw new UnsupportedOperationException( "Invalid URI: " + name);
		    
	    if( name.equals( Namespaces.OWL + "Thing" ) )
	        return ATermUtils.TOP;
		else if( name.equals( Namespaces.OWL + "Nothing" ) )
		    return ATermUtils.BOTTOM;	        
		else if( PelletOptions.USE_LOCAL_NAME )
			return ATermUtils.makeTermAppl( URIUtils.getLocalName( name ) );		
		else 
			return ATermUtils.makeTermAppl( name ) ;		
	}
}
