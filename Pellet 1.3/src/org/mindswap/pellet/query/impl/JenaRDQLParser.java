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
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.exceptions.UnsupportedFeatureException;
import org.mindswap.pellet.jena.JenaUtils;
import org.mindswap.pellet.jena.OWLLoader;
import org.mindswap.pellet.query.Query;
import org.mindswap.pellet.query.QueryEngine;
import org.mindswap.pellet.query.QueryParser;
import org.mindswap.pellet.query.QueryPattern;
import org.mindswap.pellet.utils.ATermUtils;

import aterm.ATermAppl;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdql.Constraint;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * @author Evren Sirin
 */
public class JenaRDQLParser implements QueryParser {
    public static Log log = LogFactory.getLog( QueryEngine.class );
    
    public JenaRDQLParser() {        
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
	    com.hp.hpl.jena.rdql.Query rdql = new com.hp.hpl.jena.rdql.Query( queryStr );
	    
	    return parse( rdql, kb );
	}
	
	public Query parse( com.hp.hpl.jena.rdql.Query rdql, KnowledgeBase kb ) {
	    QueryImpl query = new QueryImpl( kb );

		for ( Iterator i = rdql.getResultVars().iterator(); i.hasNext(); ) {
			String var = (String) i.next();
	
			query.addResultVar( ATermUtils.makeVar( var ) );
		}
			
		for ( Iterator i = rdql.getTriplePatterns().iterator(); i.hasNext(); ) {
			Triple t = (Triple) i.next();
	
			if ( t.getPredicate().equals(RDF.Nodes.type) ) {
			    if( t.getObject().isVariable() )
			        throw new UnsupportedFeatureException("Variables cannot be used as objects of rdf:type triples in ABoxQuery");
			    
				ATermAppl subj = makeTerm( t.getSubject() );
				ATermAppl obj = makeTerm( t.getObject() );
				
				query.addTypePattern(subj, obj);		
				
			} else if ( t.getPredicate().isVariable() ) {
				throw new UnsupportedFeatureException("Variables cannot be used in predicate position in AboxQuery");
			} else if (	t.getPredicate().toString().startsWith(RDF.getURI()) ||
						t.getPredicate().toString().startsWith(OWL.getURI()) ||
						t.getPredicate().toString().startsWith(RDFS.getURI()) ) {
		
				// this is to make sure no TBox, RBox queries are encoded in RDQL
				throw new UnsupportedFeatureException("Predicates that belong to [RDF, RDFS, OWL] namespaces cannot be used in ABoxQuery" );	
			} else {
				
				ATermAppl subj = makeTerm( t.getSubject() );
				ATermAppl pred = makeTerm( t.getPredicate() );
				ATermAppl obj = makeTerm( t.getObject());							
				
				if ( !kb.isProperty( pred ) ) 
					throw new UnsupportedFeatureException( "Property " + t.getPredicate().toString() + " is not present in KB." );
				else if ( kb.isDatatypeProperty( pred ) && query.getDistVars().contains( obj ) && t.getSubject().isVariable() ) { //&& vars.containsKey( sub ) ) {
					log.warn( "Warning: Forcing variable " + t.getSubject() + " to be distinguished (Subject of datatype triple)" );
					
					query.addDistVar( subj );
				}
				
				query.addEdgePattern(subj, pred, obj);
			}
		}
		
		for ( Iterator i = rdql.getConstraints().iterator(); i.hasNext(); ) {
			Constraint c = (Constraint) i.next();
			
			String[] tokens = c.toString().split( " " );
			
			String var = tokens[1];
			String op = tokens[2];
			String val = tokens[3];
			
			if( var.charAt( 0 ) != '?' )
			    throw new UnsupportedFeatureException( "Expecting '?' to be the first char in the expression (" + c + ")");
			
			ATermAppl curr = ATermUtils.makeVar( var.substring( 1 ) );

			if ( query.getLitVars().contains( curr ) ) {
				DConstraint dc = new DConstraint( curr, op, val );
				query.addConstraint( curr, dc.getDerivedDatatype() );
				kb.getDatatypeReasoner().defineDatatype( dc.getDerivedDatatype() );
				
				if( dc.isForced() ) {
				    List patterns = query.findPatterns( null, null, curr );
					for( Iterator it = patterns.iterator(); it.hasNext(); ) {
						QueryPattern triple = (QueryPattern) it.next();
								
						ATermAppl subj = triple.getSubject();
						if ( ATermUtils.isVar( subj ) && !query.getDistVars().contains( subj ) )
							query.addDistVar( subj );							
					}
				}
			} else {
				throw new UnsupportedFeatureException( "Literal variable in the expression (" + c + ") does not exist in the query.");
			}
		}
		
		return query;
	}	

	private ATermAppl makeTerm( Node node ) {
	    if( node.equals( OWL.Thing.asNode() ))
	        return ATermUtils.TOP;
		else if( node.equals( OWL.Nothing.asNode() ))
		    return ATermUtils.BOTTOM;	        
		else if( node.isLiteral() ) 
		    return JenaUtils.makeLiteral( node.getLiteral() );
		else if( node.isVariable() )
			return ATermUtils.makeVar( node.getName() );
		else if( PelletOptions.USE_LOCAL_NAME )
			return ATermUtils.makeTermAppl( node.getLocalName() );		
		else if( PelletOptions.USE_QNAME )
			return ATermUtils.makeTermAppl( OWLLoader.qnames.shortForm( node.toString() ) );		
		else
			return ATermUtils.makeTermAppl( node.toString() );		
	}
}
