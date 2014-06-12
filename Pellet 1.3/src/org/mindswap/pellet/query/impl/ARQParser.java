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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.exceptions.UnsupportedQueryException;
import org.mindswap.pellet.jena.JenaUtils;
import org.mindswap.pellet.jena.OWLLoader;
import org.mindswap.pellet.query.Query;
import org.mindswap.pellet.query.QueryEngine;
import org.mindswap.pellet.query.QueryParser;
import org.mindswap.pellet.utils.ATermUtils;

import aterm.ATermAppl;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Constants;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.query.core.Element;
import com.hp.hpl.jena.query.core.ElementBlock;
import com.hp.hpl.jena.query.core.ElementGroup;
import com.hp.hpl.jena.query.core.ElementTriplePattern;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * @author Evren Sirin
 */
public class ARQParser implements QueryParser {
    public static Log log = LogFactory.getLog( QueryEngine.class );
    
    private Syntax syntax = QueryEngine.DEFAULT_SYNTAX;
    
    public ARQParser() {        
    }
    
    public Syntax getSyntax() {
        return syntax;
    }
    
    public void setSyntax( Syntax syntax ) {
        this.syntax = syntax;
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
	    com.hp.hpl.jena.query.Query sparql = QueryFactory.create( queryStr, syntax );
	    
	    return parse( sparql, kb );
	}
	
	public Query parse( com.hp.hpl.jena.query.Query sparql, KnowledgeBase kb ) {
//        sparql.getPrefixMapping().setNsPrefix( "rdf", Namespaces.RDF );
//        sparql.getPrefixMapping().setNsPrefix( "rdfS", Namespaces.RDFS );
//        sparql.getPrefixMapping().setNsPrefix( "owl", Namespaces.OWL );
        
//	    if( !sparql.isSelectType() )
//	        throw new UnsupportedQueryException("Only SELECT queries are supported");
        if( sparql.isDescribeType() )
	        throw new UnsupportedQueryException("DESCRIBE queries cannot be answered with PelletQueryEngine");
	    
        
	    ElementBlock block = sparql.getQueryBlock();

	    if( !(block.getPatternElement() instanceof ElementGroup) )
	        throw new UnsupportedQueryException("ElementGroup was expected");
	    
	    ElementGroup elementGroup = (ElementGroup) block.getPatternElement();
	    
//	    if( !element.getConstraints().isEmpty() )
//	        throw new UnsupportedQueryException("Variables cannot be used as objects of rdf:type triples in ABoxQuery");
	    
	    QueryImpl query = new QueryImpl( kb );
        
        // very important to call this function so that getResultVars() will work 
        // fine for SELECT * queries 
        sparql.setResultVars();
        for ( Iterator i = sparql.getResultVars().iterator(); i.hasNext(); ) {
            String var = (String) i.next();
    
            query.addResultVar( ATermUtils.makeVar( var ) );
        }

		for ( Iterator i = elementGroup.getElements().iterator(); i.hasNext(); ) {
			Element element = (Element) i.next();
	
			if( !(element instanceof ElementTriplePattern) ) {
			    throw new UnsupportedQueryException("Nested query patterns are not supported");
			}
						
			Triple t = ((ElementTriplePattern) element).getTriple();
            Node subj = t.getSubject();
            Node pred = t.getPredicate();
            Node obj = t.getObject();
            
            ATermAppl s = makeTerm( subj );
            ATermAppl p = makeTerm( pred );
            ATermAppl o = makeTerm( obj ) ; 
            
            if( subj.isVariable() && !subj.getName().startsWith(Constants.anonVarMarker) )
                query.addDistVar( s );
            if( obj.isVariable() && !obj.getName().startsWith(Constants.anonVarMarker) )
                query.addDistVar( o );
            
			if ( pred.equals(RDF.Nodes.type) ) {
			    if( obj.isVariable() )
			        throw new UnsupportedQueryException("Variables cannot be used as objects of rdf:type triples in ABoxQuery");

				query.addTypePattern( s, o );						
			} else if ( pred.isVariable() ) {
				throw new UnsupportedQueryException("Variables cannot be used in predicate position in AboxQuery");
			} else if (	pred.toString().startsWith(RDF.getURI()) ||
						pred.toString().startsWith(OWL.getURI()) ||
						pred.toString().startsWith(RDFS.getURI()) ) {
		
				// this is to make sure no TBox, RBox queries are encoded in RDQL
				throw new UnsupportedQueryException("Predicates that belong to [RDF, RDFS, OWL] namespaces cannot be used in ABoxQuery" );	
			} else {				
				if ( !kb.isProperty( p ) ) 
					throw new UnsupportedQueryException( "Property " + pred + " is not present in KB." );
				else if ( kb.isDatatypeProperty( p ) && query.getDistVars().contains( o ) && subj.isVariable() && !query.getDistVars().contains( s ) ) { 
					log.warn( "Warning: Forcing variable " + subj + " to be distinguished (Subject of datatype triple)" );
					
					query.addDistVar( s );
				}
				
				query.addEdgePattern( s, p, o );
			}
		}

        
		return query;
	}	

	private ATermAppl makeTerm( Node node ) {
	    if( node.equals( OWL.Thing.asNode() ))
	        return ATermUtils.TOP;
		else if( node.equals( OWL.Nothing.asNode() ))
		    return ATermUtils.BOTTOM;	        
        else if( node.equals( RDF.type.asNode() ))
            return null;           
		else if( node.isLiteral() ) 
		    return JenaUtils.makeLiteral( node.getLiteral() );
		else if( node.isVariable() )
			return ATermUtils.makeVar( node.getName() );
        else if( node.isBlank() )
            return ATermUtils.makeVar( node.getBlankNodeId().toString() );
		else if( PelletOptions.USE_LOCAL_NAME )
			return ATermUtils.makeTermAppl( node.getLocalName() );		
		else if( PelletOptions.USE_QNAME )
			return ATermUtils.makeTermAppl( OWLLoader.qnames.shortForm( node.toString() ) );		
		else
			return ATermUtils.makeTermAppl( node.toString() );		
	}
}
