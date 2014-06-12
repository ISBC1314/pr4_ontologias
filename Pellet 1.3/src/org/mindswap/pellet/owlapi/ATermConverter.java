/*
 * Created on May 25, 2005
 */
package org.mindswap.pellet.owlapi;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.mindswap.pellet.exceptions.InternalReasonerException;
import org.mindswap.pellet.output.ATermBaseVisitor;
import org.mindswap.pellet.output.ATermVisitor;
import org.semanticweb.owl.model.OWLDataFactory;
import org.semanticweb.owl.model.OWLDataProperty;
import org.semanticweb.owl.model.OWLDataType;
import org.semanticweb.owl.model.OWLDataValue;
import org.semanticweb.owl.model.OWLDescription;
import org.semanticweb.owl.model.OWLException;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLObject;
import org.semanticweb.owl.model.OWLObjectProperty;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLProperty;

import aterm.ATermAppl;
import aterm.ATermList;

/**
 * Converts ATerm concepts and concept axioms (i.e. subclass axiom) to OWL-API structures.
 * 
 * @author Evren Sirin
 *
 */
public class ATermConverter extends ATermBaseVisitor implements ATermVisitor {
    private OWLOntology ont;
    private OWLDataFactory factory;
    private OWLObject obj;
    private Set set;
    
    public ATermConverter( OWLOntology ont ) throws OWLException {
        this.ont = ont;
        this.factory = ont.getOWLDataFactory();
    }
    
    public OWLObject getResult() {
        return obj;
    }

    public void visitTerm(ATermAppl term) {
        try {
            URI uri = new URI( term.getName() );
            obj = ont.getClass( uri );
            if( obj == null )
                obj = ont.getObjectProperty( uri );
            if( obj == null )
                obj = ont.getDataProperty( uri );
            if( obj == null )
                obj = ont.getIndividual( uri );            
            if( obj == null )
                obj = ont.getDatatype( uri );            
        } catch(Exception e) {
            throw new InternalReasonerException( e );
        }
    }

    public void visitAnd(ATermAppl term) {
        visitList((ATermList) term.getArgument(0));
        
        try {
            obj = factory.getOWLAnd( set );
        } catch(OWLException e) {
            throw new InternalReasonerException( e );
        }
    }

    public void visitOr(ATermAppl term) {
        visitList((ATermList) term.getArgument(0));
        
        try {
            obj = factory.getOWLOr( set );
        } catch(OWLException e) {
            throw new InternalReasonerException( e );
        }
    }

    public void visitNot(ATermAppl term) {
        visit((ATermAppl) term.getArgument(0));
        
        try {
            obj = factory.getOWLNot( (OWLDescription) obj );
        } catch(OWLException e) {
            throw new InternalReasonerException( e );
        }
    }

    public void visitSome(ATermAppl term) {
        visitTerm((ATermAppl) term.getArgument(0));
        OWLProperty prop = (OWLProperty) obj;
        
        visit((ATermAppl) term.getArgument(1));
        
        try {
            if( prop instanceof OWLObjectProperty ) {
                OWLDescription desc = (OWLDescription) obj;
                
                obj = factory.getOWLObjectSomeRestriction( (OWLObjectProperty) prop, desc );
            }
            else {
                OWLDataType datatype = (OWLDataType) obj;

                obj = factory.getOWLDataSomeRestriction( (OWLDataProperty) prop, datatype );
            }
        } catch(OWLException e) {
            throw new InternalReasonerException( e );
        }        
    }

    /* (non-Javadoc)
     * @see org.mindswap.pellet.output.ATermVisitor#visitAll(aterm.ATermAppl)
     */
    public void visitAll(ATermAppl term) {
    	visitTerm((ATermAppl) term.getArgument(0));
        OWLProperty prop = (OWLProperty) obj;
        
        visit((ATermAppl) term.getArgument(1));
        
        try {
            if( prop instanceof OWLObjectProperty ) {
                OWLDescription desc = (OWLDescription) obj;
                
                obj = factory.getOWLObjectAllRestriction( (OWLObjectProperty) prop, desc );
            }
            else {
                OWLDataType datatype = (OWLDataType) obj;

                obj = factory.getOWLDataAllRestriction( (OWLDataProperty) prop, datatype );
            }
        } catch(OWLException e) {
            throw new InternalReasonerException( e );
        }

    }

    public void visitMin(ATermAppl term) {
    	visitTerm((ATermAppl) term.getArgument(0));
        OWLProperty prop = (OWLProperty) obj;
        
        int cardinality = Integer.parseInt(term.getArgument(1).toString());
        
        try {
            if( prop instanceof OWLObjectProperty ) {
                obj = factory.getOWLObjectCardinalityAtLeastRestriction((OWLObjectProperty) prop, cardinality);
            }
            else {
                obj = factory.getOWLDataCardinalityAtLeastRestriction( (OWLDataProperty) prop, cardinality );
            }
        } catch(OWLException e) {
            throw new InternalReasonerException( e );
        }
    }

    public void visitMax(ATermAppl term) {
    	visitTerm((ATermAppl) term.getArgument(0));
        OWLProperty prop = (OWLProperty) obj;
        
        int cardinality = Integer.parseInt(term.getArgument(1).toString());
        
        try {
            if( prop instanceof OWLObjectProperty ) {
                obj = factory.getOWLObjectCardinalityAtMostRestriction((OWLObjectProperty) prop, cardinality);
            }
            else {
                obj = factory.getOWLDataCardinalityAtMostRestriction( (OWLDataProperty) prop, cardinality );
            }
        } catch(OWLException e) {
            throw new InternalReasonerException( e );
        }
    }

    public void visitHasValue(ATermAppl term) {
    	visitTerm((ATermAppl) term.getArgument(0));
        OWLProperty prop = (OWLProperty) obj;
        
        visit((ATermAppl) term.getArgument(1));
        
        try {
            if( prop instanceof OWLObjectProperty ) {
                OWLIndividual ind = (OWLIndividual) obj;
                
                obj = factory.getOWLObjectValueRestriction( (OWLObjectProperty) prop, ind );
            }
            else {
                OWLDataValue dataVal = (OWLDataValue) obj;

                obj = factory.getOWLDataValueRestriction( (OWLDataProperty) prop, dataVal );
            }
        } catch(OWLException e) {
            throw new InternalReasonerException( e );
        }
    }

    /* (non-Javadoc)
     * @see org.mindswap.pellet.output.ATermVisitor#visitValue(aterm.ATermAppl)
     */
    public void visitValue(ATermAppl term) {
        // TODO Auto-generated method stub
    	
    }

    /* (non-Javadoc)
     * @see org.mindswap.pellet.output.ATermVisitor#visitOneOf(aterm.ATermAppl)
     */
    public void visitOneOf(ATermAppl term) {
    	visitList((ATermList) term.getArgument(0));
        
        try {
            obj = factory.getOWLEnumeration( set );
        } catch(OWLException e) {
            throw new InternalReasonerException( e );
        }

    }

    public void visitLiteral(ATermAppl term) {
    	// literal(lexicalValue, language, datatypeURI)
    	try {
    		String lexValue = ((ATermAppl) term.getArgument(0)).toString();
    		String lang = ((ATermAppl) term.getArgument(1)).toString();
    		URI dtypeURI = new URI(((ATermAppl) term.getArgument(2)).toString());
    		obj = ont.getOWLDataFactory().getOWLConcreteData(dtypeURI, lang, lexValue);
    	}
    	catch (Exception e) {
    		throw new InternalReasonerException (e);
    	}
    }

    public void visitList(ATermList list) {
        Set elements = new HashSet();
		while (!list.isEmpty()) {
			ATermAppl term = (ATermAppl) list.getFirst();
			visit(term);
			elements.add( obj );
			list = list.getNext();
		}
		this.set = elements;
    }
    
    public void visitSubClass(ATermAppl term) {
    	try {
		    ATermAppl sub = (ATermAppl) term.getArgument(0);
		    ATermAppl sup = (ATermAppl) term.getArgument(1);
		    visit(sub);
		    OWLDescription subDesc = (OWLDescription) obj;
		    visit(sup);
		    OWLDescription supDesc = (OWLDescription) obj;
		    obj = ont.getOWLDataFactory().getOWLSubClassAxiom(subDesc, supDesc);
    	} 
    	catch(OWLException e) {
            throw new InternalReasonerException( e );
        }
	}
}
