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

package org.mindswap.pellet.owlapi;

import java.net.URI;
import java.util.Iterator;

import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.QNameProvider;
import org.semanticweb.owl.io.vocabulary.OWLVocabularyAdapter;
import org.semanticweb.owl.model.OWLAnd;
import org.semanticweb.owl.model.OWLAnnotationInstance;
import org.semanticweb.owl.model.OWLAnnotationProperty;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLDataAllRestriction;
import org.semanticweb.owl.model.OWLDataCardinalityRestriction;
import org.semanticweb.owl.model.OWLDataEnumeration;
import org.semanticweb.owl.model.OWLDataProperty;
import org.semanticweb.owl.model.OWLDataPropertyInstance;
import org.semanticweb.owl.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owl.model.OWLDataSomeRestriction;
import org.semanticweb.owl.model.OWLDataType;
import org.semanticweb.owl.model.OWLDataValue;
import org.semanticweb.owl.model.OWLDataValueRestriction;
import org.semanticweb.owl.model.OWLDescription;
import org.semanticweb.owl.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owl.model.OWLDisjointClassesAxiom;
import org.semanticweb.owl.model.OWLEnumeration;
import org.semanticweb.owl.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owl.model.OWLEquivalentPropertiesAxiom;
import org.semanticweb.owl.model.OWLException;
import org.semanticweb.owl.model.OWLFrame;
import org.semanticweb.owl.model.OWLFunctionalPropertyAxiom;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLIndividualTypeAssertion;
import org.semanticweb.owl.model.OWLInverseFunctionalPropertyAxiom;
import org.semanticweb.owl.model.OWLInversePropertyAxiom;
import org.semanticweb.owl.model.OWLNamedObject;
import org.semanticweb.owl.model.OWLNot;
import org.semanticweb.owl.model.OWLObjectAllRestriction;
import org.semanticweb.owl.model.OWLObjectCardinalityRestriction;
import org.semanticweb.owl.model.OWLObjectProperty;
import org.semanticweb.owl.model.OWLObjectPropertyInstance;
import org.semanticweb.owl.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owl.model.OWLObjectSomeRestriction;
import org.semanticweb.owl.model.OWLObjectValueRestriction;
import org.semanticweb.owl.model.OWLObjectVisitor;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOr;
import org.semanticweb.owl.model.OWLProperty;
import org.semanticweb.owl.model.OWLPropertyDomainAxiom;
import org.semanticweb.owl.model.OWLSameIndividualsAxiom;
import org.semanticweb.owl.model.OWLSubClassAxiom;
import org.semanticweb.owl.model.OWLSubPropertyAxiom;
import org.semanticweb.owl.model.OWLSymmetricPropertyAxiom;
import org.semanticweb.owl.model.OWLTransitivePropertyAxiom;

import aterm.ATerm;
import aterm.ATermAppl;
import aterm.ATermList;


/**
 * PelletVisitor
 *  
 */

public class PelletVisitor implements OWLObjectVisitor {
    /**
     * 
     */
    private static final long serialVersionUID = 8211773146996997500L;

    public static boolean DEBUG = false;
    
    public static QNameProvider qnames = new QNameProvider();
	private static OWLVocabularyAdapter  OWL  = OWLVocabularyAdapter.INSTANCE;
	
	PelletLoader loader;

	ATermAppl term;

	public PelletVisitor(PelletLoader loader) {
		this.loader = loader;
	}

	public ATermAppl result() {
		return term;
	}

	public void reset() {
		term = null;
	}	
	
	
	public ATermAppl term(OWLDataValue dv) throws OWLException {
		URI datatypeURI = dv.getURI();
		String lexicalValue = dv.getValue().toString();
		String lang = dv.getLang();
		
		if (datatypeURI != null)
		    return ATermUtils.makeTypedLiteral(lexicalValue, datatypeURI.toString());
		else if(lang != null)
		    return ATermUtils.makePlainLiteral(lexicalValue, lang);
		else
		    return ATermUtils.makePlainLiteral(lexicalValue);
	}
	
	public ATermAppl term(OWLNamedObject o) throws OWLException {
		URI uri = o.getURI();
		if(uri == null) {
			if(o instanceof OWLIndividual) {
			    uri = ((OWLIndividual) o).getAnonId();
				return term( uri );
			}
			throw new OWLException("No name can be created for " + o);
		}
		else
			return term(uri);
	}

	public ATermAppl term(String s) {
		return ATermUtils.makeTermAppl(s);
	}

	public ATermAppl term(URI u) {
		if(u.toString().equals(OWL.getThing()))
			return ATermUtils.TOP;
		if(u.toString().equals(OWL.getNothing()))
			return ATermUtils.BOTTOM;
		if(PelletOptions.USE_LOCAL_NAME)
			return term(u.getFragment());
        else if(PelletOptions.USE_QNAME)
            return term(qnames.shortForm(u));
            
		return term(u.toString());
	}	
	
	public void visit(OWLClass clazz) throws OWLException {
		term = term(clazz);
	}

	public void visit(OWLIndividual ind) throws OWLException {
		term = term(ind);
	}

	public void visit(OWLObjectProperty prop) throws OWLException {
		term = term(prop);		
	}
	public void visit(OWLDataProperty prop) throws OWLException {
		term = term(prop);
	}
	
	public void visit(OWLDataValue cd) throws OWLException {
		term = term(cd);
	}

	public void visit(OWLDataType ocdt) throws OWLException {
		term = term(ocdt.getURI());
		
		KnowledgeBase kb = loader.getKB();
		kb.loadDatatype( term );
	}
	
	public void visit(OWLAnd and) throws OWLException {
		ATermList ops = ATermUtils.EMPTY_LIST;
		for (Iterator it = and.getOperands().iterator(); it.hasNext();) {
			OWLDescription desc = (OWLDescription) it.next();
			desc.accept(this);
			ops = ops.insert(result());
		}
		term = ATermUtils.makeAnd(ops);
	}

	public void visit(OWLOr or) throws OWLException {
		ATermList ops = ATermUtils.EMPTY_LIST;
		for (Iterator it = or.getOperands().iterator(); it.hasNext();) {
			OWLDescription desc = (OWLDescription) it.next();
			desc.accept(this);
			ops = ops.insert(result());
		}
		term = ATermUtils.makeOr(ops);
	}

	public void visit(OWLNot not) throws OWLException {
		OWLDescription desc = not.getOperand();
		desc.accept(this);
		
		term = ATermUtils.makeNot(term);
	}

	public void visit(OWLEnumeration enumeration) throws OWLException {
		ATermList ops = ATermUtils.EMPTY_LIST;
		for (Iterator it = enumeration.getIndividuals().iterator(); it.hasNext(); ) {
			OWLIndividual desc = (OWLIndividual) it.next();
			desc.accept(this);
			ops = ops.insert(ATermUtils.makeValue(result()));
		}
		term = ATermUtils.makeOr(ops);
	}

	public void visit(OWLObjectSomeRestriction restriction) throws OWLException {		
		restriction.getObjectProperty().accept(this);
		ATerm p = term;
		restriction.getDescription().accept(this);
		ATerm c = term;
		
		term = ATermUtils.makeSomeValues(p ,c);
	}

	public void visit(OWLObjectAllRestriction restriction) throws OWLException {
		restriction.getObjectProperty().accept(this);
		ATerm p = term;
		restriction.getDescription().accept(this);
		ATerm c = term;
		
		term = ATermUtils.makeAllValues(p ,c);
	}

	public void visit(OWLObjectValueRestriction restriction) throws OWLException {
		restriction.getObjectProperty().accept(this);
		ATerm p = term;
		restriction.getIndividual().accept(this);
		ATermAppl ind = term;
		
		term = ATermUtils.makeHasValue( p, ind );
	}

	public void visit(OWLObjectCardinalityRestriction restriction) throws OWLException {
		if (restriction.isExactly()) {
			restriction.getObjectProperty().accept(this);
			ATerm p = term;
			int n = restriction.getAtLeast();
			
			term = ATermUtils.makeCard(p, n);
			
		} else if (restriction.isAtMost()) {
			restriction.getObjectProperty().accept(this);
			ATerm p = term;
			int n = restriction.getAtMost();
			
			term = ATermUtils.makeMax(p, n);
			
		} else if (restriction.isAtLeast()) {
			restriction.getObjectProperty().accept(this);
			ATerm p = term;
			int n = restriction.getAtLeast();
			
			term = ATermUtils.makeMin(p, n);
		}
	}

	public void visit(OWLDataCardinalityRestriction restriction) throws OWLException {
		if (restriction.isExactly()) {
			restriction.getDataProperty().accept(this);
			ATerm p = term;
			int n = restriction.getAtLeast();
			
			term = ATermUtils.makeCard(p, n);
			
		} else if (restriction.isAtMost()) {
			restriction.getDataProperty().accept(this);
			ATerm p = term;
			int n = restriction.getAtMost();
			
			term = ATermUtils.makeMax(p, n);
			
		} else if (restriction.isAtLeast()) {
			restriction.getDataProperty().accept(this);
			ATerm p = term;
			int n = restriction.getAtLeast();
			
			term = ATermUtils.makeMin(p, n);
		}
	}

	public void visit(OWLEquivalentClassesAxiom axiom) throws OWLException {
		KnowledgeBase kb = loader.getKB();
	    
		Iterator eqs = axiom.getEquivalentClasses().iterator();
		if(eqs.hasNext()) {
			OWLDescription desc1 = (OWLDescription) eqs.next();
			desc1.accept(this);
			ATermAppl c1 = term;
			
			while(eqs.hasNext()) {
				OWLDescription desc2 = (OWLDescription) eqs.next();
				desc2.accept(this);
				ATermAppl c2 = term;
				
				kb.addEquivalentClass(c1, c2);
			}
		}
	}

	public void visit(OWLDisjointClassesAxiom axiom) throws OWLException {
		KnowledgeBase kb = loader.getKB();

		Object[] disjs = axiom.getDisjointClasses().toArray();
		for (int i = 0; i < disjs.length; i++) {
			for (int j = i + 1; j < disjs.length; j++) {
				OWLDescription desc1 = (OWLDescription) disjs[i];
				OWLDescription desc2 = (OWLDescription) disjs[j];				
				desc1.accept(this);
				ATerm c1 = term;
				desc2.accept(this);
				ATerm c2 = term;
				
				kb.addDisjointClass(c1, c2);
			}
		}
	}

	public void visit(OWLSubClassAxiom axiom) throws OWLException {
		axiom.getSubClass().accept(this);
		ATermAppl c1 = term;
		axiom.getSuperClass().accept(this);
		ATermAppl c2 = term;
		
		KnowledgeBase kb = loader.getKB();
		kb.addSubClass(c1, c2);
	}

	public void visit(OWLEquivalentPropertiesAxiom axiom) throws OWLException {
		KnowledgeBase kb = loader.getKB();

		Object[] eqs = axiom.getProperties().toArray();
		for (int i = 0; i < eqs.length; i++) {
			for (int j = i + 1; j < eqs.length; j++) {
				OWLProperty prop1 = (OWLProperty) eqs[i];
				OWLProperty prop2 = (OWLProperty) eqs[j];
				prop1.accept(this);
				ATermAppl p1 = term;
				prop2.accept(this);
				ATermAppl p2 = term;
				
				kb.addEquivalentProperty(p1, p2);
			}
		}
	}

	public void visit(OWLSubPropertyAxiom axiom) throws OWLException {
		axiom.getSubProperty().accept(this);
		ATermAppl p1 = term; 
		axiom.getSuperProperty().accept(this);
		ATermAppl p2 = term;
		
		KnowledgeBase kb = loader.getKB();
		kb.addSubProperty(p1 , p2);
	}

	public void visit(OWLDifferentIndividualsAxiom axiom) throws OWLException {
	    KnowledgeBase kb = loader.getKB();
	    
		Object[] inds = axiom.getIndividuals().toArray();
		for (int i = 0; i < inds.length; i++) {
			for (int j = i + 1; j < inds.length; j++) {
				ATermAppl i1 = loader.term((OWLIndividual) inds[i]);
				ATermAppl i2 = loader.term((OWLIndividual) inds[j]);
				
				kb.addDifferent(i1, i2);
			}
		}		
	}

	public void visit(OWLSameIndividualsAxiom axiom) throws OWLException {
	    KnowledgeBase kb = loader.getKB();
	    
		Iterator eqs = axiom.getIndividuals().iterator();
		if(eqs.hasNext()) {
			ATermAppl i1 = loader.term((OWLIndividual) eqs.next());
			
			while(eqs.hasNext()) {
				ATermAppl i2 = loader.term((OWLIndividual) eqs.next());
				
				kb.addSame(i1, i2);
			}
		}
	}

	public void visit(OWLAnnotationProperty ap) {
		// skip annotation properties	
	    if(DEBUG) System.out.println("OWLAnnotationProperty " + ap);
	}

	public void visit(OWLAnnotationInstance ai) {
		// skip annotation instances		
	    if(DEBUG) System.out.println("OWLAnnotationInstance " + ai);
	}

	public void visit(OWLDataEnumeration enumeration) throws OWLException {
		ATermList ops = ATermUtils.EMPTY_LIST;
		for (Iterator it = enumeration.getValues().iterator(); it.hasNext(); ) {
			OWLDataValue value = (OWLDataValue) it.next();
			value.accept(this);
			ops = ops.insert(ATermUtils.makeValue(result()));
		}
		term = ATermUtils.makeOr(ops);		
	}

	public void visit(OWLDataAllRestriction restriction) throws OWLException {
		restriction.getDataProperty().accept(this);
		ATerm p = term;
		restriction.getDataType().accept(this);
		ATerm c = term;
		
		term = ATermUtils.makeAllValues(p ,c);
	}

	public void visit(OWLDataSomeRestriction restriction) throws OWLException {
		restriction.getDataProperty().accept(this);
		ATerm p = term;
		restriction.getDataType().accept(this);
		ATerm c = term;
		
		term = ATermUtils.makeSomeValues(p ,c);
	}

	public void visit(OWLDataValueRestriction restriction) throws OWLException {
		restriction.getDataProperty().accept(this);
		ATermAppl p = term;
		restriction.getValue().accept(this);
		ATermAppl dv = term;
		
		term = ATermUtils.makeHasValue( p, dv );	
	}

	public void visit(OWLFrame f) {
		// skip OWLFrame 
	    if(DEBUG) System.out.println("OWLFrame " + f);
	}

	public void visit(OWLOntology o) {
		// skip ontology	
	    if(DEBUG) System.out.println("OWLOntology " + o);
	}

	public void visit(OWLFunctionalPropertyAxiom node) throws OWLException {
		throw new OWLException( "Invalid axiom " + node );
	}

	public void visit(OWLInverseFunctionalPropertyAxiom node) throws OWLException {
		throw new OWLException( "Invalid axiom " + node );
	}

	public void visit(OWLTransitivePropertyAxiom node) throws OWLException {
		throw new OWLException( "Invalid axiom " + node );
	}

	public void visit(OWLSymmetricPropertyAxiom node) throws OWLException {
		throw new OWLException( "Invalid axiom " + node );
	}

	public void visit(OWLInversePropertyAxiom node) throws OWLException {
		throw new OWLException( "Invalid axiom " + node );
	}

	public void visit(OWLPropertyDomainAxiom node) throws OWLException {
		throw new OWLException( "Invalid axiom " + node );
	}

	public void visit(OWLObjectPropertyRangeAxiom node) throws OWLException {
		throw new OWLException( "Invalid axiom " + node );
	}

	public void visit(OWLDataPropertyRangeAxiom node) throws OWLException {
		throw new OWLException( "Invalid axiom " + node );
	}

	public void visit(OWLObjectPropertyInstance node) throws OWLException {
		throw new OWLException( "Invalid axiom " + node );
	}

	public void visit(OWLDataPropertyInstance node) throws OWLException {
		throw new OWLException( "Invalid axiom " + node );
	}

	public void visit(OWLIndividualTypeAssertion node) throws OWLException {
		throw new OWLException( "Invalid axiom " + node );
	}

}
