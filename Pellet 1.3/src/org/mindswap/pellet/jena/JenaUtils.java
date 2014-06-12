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
 * Created on Sep 20, 2004
 */
package org.mindswap.pellet.jena;

import java.util.Iterator;
import java.util.Map;

import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.datatypes.RDFXMLLiteral;
import org.mindswap.pellet.exceptions.InternalReasonerException;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.QNameProvider;

import aterm.ATermAppl;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.impl.LiteralLabel;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.vocabulary.OWL;

/**
 * Utility functions related to Jena structures. The functions here may have similar functinoality to the ones
 * in ATermUtils but they are aprovided here because ATermUtils is supposed to be library-independent (it should 
 * NOT import Jena packagesotherwise applications based on OWL-API would require Jena packages)
 * 
 * @author Evren Sirin
 */
public class JenaUtils {
    static public ATermAppl makeLiteral(LiteralLabel jenaLiteral) {
        String lexicalValue = jenaLiteral.getLexicalForm();
        String datatypeURI = jenaLiteral.getDatatypeURI();
        ATermAppl literalValue = null;
    
        if(datatypeURI != null)
            literalValue = ATermUtils.makeTypedLiteral(lexicalValue, datatypeURI);
        else if(jenaLiteral.language() == null)
            literalValue = ATermUtils.makePlainLiteral(lexicalValue, jenaLiteral.language());
        else
            literalValue = ATermUtils.makePlainLiteral(lexicalValue);
        
        return literalValue;
    }

    static public Node makeGraphLiteral( ATermAppl literal ) {
        Node node;
        
	    String lexicalValue = ((ATermAppl) literal.getArgument( 0 )).getName();
        String lang = ((ATermAppl) literal.getArgument( 1 )).getName();
        String datatypeURI = ((ATermAppl) literal.getArgument( 2 )).getName();
        
        if( !lang.equals( "" ) )
            node = Node.createLiteral( lexicalValue, lang, false );
        else if( datatypeURI.equals( "" ) )
            node = Node.createLiteral( lexicalValue );
        else if( datatypeURI.equals( RDFXMLLiteral.instance.getURI() ) )
            node = Node.createLiteral( lexicalValue, "", true );
        else {
            RDFDatatype datatype = TypeMapper.getInstance().getTypeByName( datatypeURI );
            node = Node.createLiteral( lexicalValue, "", datatype );
        }

        return node;
    }
    
    static public Node makeGraphResource( ATermAppl term ) {
        String name = term.getName();
            
        if (name.startsWith( PelletOptions.BNODE )) {
            String anonID = name.substring( PelletOptions.BNODE.length() );
            return Node.createAnon( new AnonId( anonID ) );
        }
        else if( term.equals( ATermUtils.TOP ) ) {
            return OWL.Thing.asNode();
        }
        else if( term.equals( ATermUtils.BOTTOM ) ) {
            return OWL.Nothing.asNode();
        }
        else if( term.getArity() == 0 ){
            return Node.createURI( name );
        }
        else {
            throw new InternalReasonerException( "Invalid term found " + term );
        }
    }
    
    static public Node makeGraphNode( ATermAppl value ) {
		if ( ATermUtils.isLiteral( value ) ) 
		    return makeGraphLiteral( value );
		else 
		    return makeGraphResource( value );
    }    

    static public Literal makeLiteral(ATermAppl literal, Model model) {        
		Literal node = null;

        String lexicalValue = ((ATermAppl) literal.getArgument( 0 )).getName();
        String lang = ((ATermAppl) literal.getArgument( 1 )).getName();
        String datatypeURI = ((ATermAppl) literal.getArgument( 2 )).getName();

        if( !lang.equals( "" ) )
            node = model.createLiteral( lexicalValue, lang );
        else if( datatypeURI.equals( "" ) )
            node = model.createLiteral( lexicalValue );
        else if( datatypeURI.equals( RDFXMLLiteral.instance.getName() ) )
            node = model.createLiteral( lexicalValue, true );
        else
            node = model.createTypedLiteral( lexicalValue, datatypeURI );

        return node;
    }

    static public Resource makeResource(ATermAppl term, Model model) {        
		if(term.equals(ATermUtils.TOP))
			return OWL.Thing;
		else if(term.equals(ATermUtils.BOTTOM))
			return OWL.Nothing;
		else if(term.getArity() == 0) {
	        String name = term.getName();
            
	        if (name.startsWith( PelletOptions.BNODE )) {
	            String anonID = name.substring( PelletOptions.BNODE.length() );
	            return model.createResource( new AnonId( anonID ) );
	        }
	        else {
	            return model.getResource( name );
	        }
		}
		
		return null;
    }

    static public RDFNode makeRDFNode(ATermAppl term, Model model) {        
	    if( ATermUtils.isLiteral( term) )
	        return makeLiteral( term, model );
	    else
	        return makeResource( term, model );
    }
    
    static public QNameProvider makeQNameProvider(PrefixMapping mapping) {
        QNameProvider qnames = new QNameProvider();
        
        Iterator entries = mapping.getNsPrefixMap().entrySet().iterator();
        while(entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();
            String prefix = (String) entry.getKey();
            String uri = (String) entry.getValue();
            
            qnames.setMapping(prefix, uri);
        }
                
	    return qnames;
	}
}
