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

package org.mindswap.pellet.owlapi; // Generated package name

import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.mindswap.pellet.EconnectedKB;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.Role;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.URIUtils;
import org.semanticweb.owl.impl.model.OWLConnectionImpl;
import org.semanticweb.owl.io.owl_rdf.OWLRDFParser;
import org.semanticweb.owl.io.vocabulary.OWLVocabularyAdapter;
import org.semanticweb.owl.io.vocabulary.RDFSVocabularyAdapter;
import org.semanticweb.owl.io.vocabulary.RDFVocabularyAdapter;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLClassAxiom;
import org.semanticweb.owl.model.OWLDataProperty;
import org.semanticweb.owl.model.OWLDataRange;
import org.semanticweb.owl.model.OWLDataType;
import org.semanticweb.owl.model.OWLDataValue;
import org.semanticweb.owl.model.OWLDescription;
import org.semanticweb.owl.model.OWLEnumeration;
import org.semanticweb.owl.model.OWLException;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLIndividualAxiom;
import org.semanticweb.owl.model.OWLObject;
import org.semanticweb.owl.model.OWLObjectProperty;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLPropertyAxiom;
import org.semanticweb.owl.model.helper.OntologyHelper;

import aterm.ATermAppl;

public class PelletLoader {
	public static boolean DEBUG = false;
	
	private KnowledgeBase kb;
	
	private Set loadedFiles; 
	private boolean loadImports = true;
	
	private Set ontologies; 

	private PelletVisitor visitor = new PelletVisitor(this);

	public PelletLoader(KnowledgeBase kb) {
	    this.kb = kb;
	    
		reset();
	}	
	
	/**
	 * @return Returns the useImports.
	 */
	public boolean loadImports() {
		return loadImports;
	}

	/**
	 * @param useImports The useImports to set.
	 */
	public void setLoadImports(boolean loadImports) {
		this.loadImports = loadImports;
	}	
	
	public void reset() {
		kb.clear();
		ontologies = new HashSet();
		loadedFiles = new HashSet();
		loadedFiles.add(URIUtils.getNameSpace(OWLVocabularyAdapter.OWL));
		loadedFiles.add(URIUtils.getNameSpace(RDFVocabularyAdapter.RDF));
		loadedFiles.add(URIUtils.getNameSpace(RDFSVocabularyAdapter.RDFS));
	}
	
	public void load(OWLOntology ontology) throws OWLException {	
		if(loadImports) {		
			Iterator i = OntologyHelper.importClosure(ontology).iterator();
			while(i.hasNext())
				loadOntology((OWLOntology) i.next());
		}
		else
			loadOntology(ontology);

	}
	
	public KnowledgeBase getKB() {
		return kb;
	}
		
    public void setKB(KnowledgeBase kb) {
        this.kb = kb;
    }
    
	public ATermAppl term(OWLObject d) throws OWLException {
		visitor.reset();
		d.accept(visitor);
		
		ATermAppl a = visitor.result();
		
		if(a == null) throw new OWLException("Cannot create ATerm from description " + d);
		
		return a;		
	}

	void loadOntology(OWLOntology ontology) throws OWLException {		
		Iterator it = null;

		String uri = URIUtils.getNameSpace(ontology.getURI());
		if(loadedFiles.contains(uri))
			return;
		loadedFiles.add(uri);
		ontologies.add(ontology);
		
		if(kb instanceof EconnectedKB) {
		    EconnectedKB econn = (EconnectedKB) kb;
		    String ontURI = ontology.getURI().toString();
    		if(!econn.getTBoxes().keySet().contains(ontURI))
    			econn.addOntology(ontURI);
    		((EconnectedKB) kb).setOntology(ontURI);
		}
		
		visitor = new PelletVisitor(this);

		defineEntities( ontology );
		
		it = ontology.getClasses().iterator(); 
		while(it.hasNext()) {
			loadClass(ontology, (OWLClass) it.next());
		}
		
		it = ontology.getObjectProperties().iterator();
		while(it.hasNext()) {
			loadObjectProperty(ontology, (OWLObjectProperty) it.next());
		}
				
		it = ontology.getDataProperties().iterator();
		while(it.hasNext()) {
			loadDataProperty(ontology, (OWLDataProperty) it.next());
		}
		
		it = ontology.getIndividuals().iterator();
		while(it.hasNext()) {
			loadIndividual(ontology, (OWLIndividual) it.next());
		}
		
		it = ontology.getDatatypes().iterator(); 
		while(it.hasNext()) {
			loadDataType(ontology, (OWLDataType) it.next());
		}
		
		it = ontology.getClassAxioms().iterator();
		while(it.hasNext()) {
			loadClassAxiom((OWLClassAxiom) it.next());
		}
		
		it = ontology.getPropertyAxioms().iterator();
		while(it.hasNext()) {
			loadPropertyAxiom((OWLPropertyAxiom) it.next());
		}
		
		it = ontology.getIndividualAxioms().iterator();
		while(it.hasNext()) {
			loadIndividualAxiom((OWLIndividualAxiom) it.next());
		}
		
		
		if(kb instanceof EconnectedKB) {
	    	//We go through the list of linked ontologies
	    	for(Iterator i = ontology.getForeignOntologies().iterator(); i.hasNext(); ) {
	    		URI foreignOntURI = (URI) i.next();	 
	    		
	    		OWLRDFParser parser = new OWLRDFParser();
	    		parser.setConnection(new OWLConnectionImpl());
	    		OWLOntology foreignOnt = parser.parseOntology(foreignOntURI);
	    		loadOntology(foreignOnt);
	    		
	    	} 
		}
	}

	private void defineEntities(OWLOntology ontology) throws OWLException {		
		Iterator it = null;

		it = ontology.getClasses().iterator(); 
		while(it.hasNext()) 
		    kb.addClass(term((OWLClass) it.next()));
				
		it = ontology.getObjectProperties().iterator();
		while(it.hasNext()) {
		    OWLObjectProperty prop = (OWLObjectProperty) it.next();
		    ATermAppl p = term( prop );
		
			if(prop.isLink()) {
			    ((EconnectedKB) kb).addLinkProperty(p);
			    
			    String target = prop.getLinkTarget().toString();
			    Role role = kb.getProperty(p);
			    role.setForeignOntology(target);
			}
			else
			    kb.addObjectProperty(p);
		}
		
		it = ontology.getDataProperties().iterator();
		while(it.hasNext()) 
			kb.addDatatypeProperty(term((OWLDataProperty) it.next()));

		it = ontology.getIndividuals().iterator();
		while(it.hasNext()) 
			kb.addIndividual( term( (OWLIndividual) it.next() ) );
	}

	private void loadClass(OWLOntology ontology, OWLClass clazz) throws OWLException {
		Iterator it = null;
		
		ATermAppl c = term(clazz);
		
		kb.addClass(c);

		it = clazz.getEquivalentClasses(ontology).iterator(); 
		while(it.hasNext()) {
			OWLDescription eq = (OWLDescription) it.next();
			
			kb.addEquivalentClass(c, term(eq));
		}

		it = clazz.getSuperClasses(ontology).iterator();
		while(it.hasNext()) {
			OWLDescription sup = (OWLDescription) it.next();
			
			kb.addSubClass(c, term(sup));
		}
		
		it = clazz.getEnumerations(ontology).iterator();
		while(it.hasNext()) {
			OWLEnumeration en = (OWLEnumeration) it.next();
			
			kb.addEquivalentClass(c, term(en));
		}		
	}

	private void loadIndividual(OWLOntology ontology, OWLIndividual ind) throws OWLException {
		Iterator it = null;		

		ATermAppl indReference = term(ind);		
				
		it = ind.getTypes(ontology).iterator();
		while(it.hasNext()) {		
			OWLDescription desc = (OWLDescription) it.next();
			visitor.reset();
			desc.accept(visitor);
			
//			kb.getExplanationTable().setSourceAxiom(new OWLIndividualTypeAssertionImpl(factory, ind, desc));
			kb.addType(indReference, visitor.result());
		}
		
		Map propertyValues = ind.getObjectPropertyValues(ontology);
		it = propertyValues.keySet().iterator(); 
		while(it.hasNext()) {
			OWLObjectProperty op = (OWLObjectProperty) it.next();
			Set vals = (Set) propertyValues.get(op);
			if(vals == null) continue;
			for (Iterator valIt = vals.iterator(); valIt.hasNext();) {
			    OWLIndividual oi = (OWLIndividual) valIt.next();
			    ATermAppl obj = term(oi);
			    
//				kb.getExplanationTable().setSourceAxiom(new OWLObjectPropertyInstanceImpl(factory, ind, op, oi));
				kb.addPropertyValue(term(op), indReference, obj);
			}
		}

		/* Don't do these for now! */
	 	Map dataValues = ind.getDataPropertyValues(ontology);
	 	it = dataValues.keySet().iterator();
	 	while(it.hasNext()) {	 		
	 		OWLDataProperty dp = (OWLDataProperty) it.next();
	 		Set vals = (Set) dataValues.get(dp);
	 		for (Iterator valIt = vals.iterator(); valIt.hasNext();) {
	 			OWLDataValue dv = (OWLDataValue) valIt.next();
	 			
	 			String lexicalValue = dv.getValue().toString();
	 			URI datatypeURI = dv.getURI();
	 			ATermAppl literalValue = null;
	 			
	 			if(datatypeURI != null)
	 			   literalValue = ATermUtils.makeTypedLiteral(lexicalValue, datatypeURI.toString());
	 			else if(dv.getLang() != null)
	 			   literalValue = ATermUtils.makePlainLiteral(lexicalValue, dv.getLang());
	 			else
	 			   literalValue = ATermUtils.makePlainLiteral(lexicalValue);

//				kb.getExplanationTable().setSourceAxiom(new OWLDataPropertyInstanceImpl(factory, ind, dp, dv));
	 			kb.addPropertyValue(term(dp), indReference, literalValue);
	 		}
	 	}
	}

	private void loadObjectProperty(OWLOntology ontology, OWLObjectProperty prop) throws OWLException {
		ATermAppl p = term(prop);
		Iterator it = null;
				
		if (prop.isTransitive(ontology)) {
//		    kb.getExplanationTable().setSourceAxiom(new OWLTransitivePropertyAxiomImpl(factory, prop));
			kb.addTransitiveProperty(p);
		}
		if (prop.isFunctional(ontology)) {
//		    kb.getExplanationTable().setSourceAxiom(new OWLFunctionalPropertyAxiomImpl(factory, prop));
			kb.addFunctionalProperty(p);
		}
		if (prop.isInverseFunctional(ontology)) {
//		    kb.getExplanationTable().setSourceAxiom(new OWLInverseFunctionalPropertyAxiomImpl(factory, prop));
			kb.addInverseFunctionalProperty(p);
		}
		if (prop.isSymmetric(ontology)) {
//		    kb.getExplanationTable().setSourceAxiom(new OWLSymmetricPropertyAxiomImpl(factory, prop));
			kb.addSymmetricProperty(p);
		}

		it = prop.getInverses(ontology).iterator();
		while(it.hasNext()) {
		    OWLObjectProperty inv = (OWLObjectProperty) it.next();
			
//		    kb.getExplanationTable().setSourceAxiom(new OWLInversePropertyAxiomImpl(factory, prop, inv));
			//****************************************************
		    //Changed for Econnections
		    //*****************************************************
		    if(!prop.isLink())
				kb.addInverseProperty(p, term(inv));
		    else{
		    	  Role role = kb.getProperty(p);
		    	  String ontURI = prop.getLinkTarget().toString();
		    	  if(!((EconnectedKB)kb).getTBoxes().keySet().contains(ontURI))
	    			((EconnectedKB)kb).addOntology(ontURI);    
	       		   
		    	  ((EconnectedKB) kb).setOntology(ontURI);
		    	  if(!(((EconnectedKB)kb).getRBox().isRole(term(inv))))
		       		((EconnectedKB)kb).addLinkProperty(term(inv));
		       		
				  Role roleInv = kb.getProperty(term(inv));
				  if (roleInv!=null)
       				 roleInv.setForeignOntology(ontology.getURI().toString());
       		
				 ((EconnectedKB) kb).setOntology(ontology.getURI().toString());
				 
				 ((EconnectedKB)kb).addInverseLink(role,roleInv);
		    }
		}

		it = prop.getSuperProperties(ontology).iterator();
		while(it.hasNext()) {
		    OWLObjectProperty sup = (OWLObjectProperty) it.next();
			
			kb.addSubProperty(p, term(sup));
		}

		it = prop.getDomains(ontology).iterator();
		while(it.hasNext()) {
			OWLDescription dom = (OWLDescription) it.next();
			visitor.reset();
			dom.accept(visitor);
			
//			kb.getExplanationTable().setSourceAxiom(new OWLPropertyDomainAxiomImpl(factory, prop, dom));
			kb.addDomain(p, visitor.result());
		}

		it = prop.getRanges(ontology).iterator();
		while(it.hasNext()) {
			OWLDescription ran = (OWLDescription) it.next();
			visitor.reset();
			ran.accept(visitor);
			
//			kb.getExplanationTable().setSourceAxiom(new OWLObjectPropertyRangeAxiomImpl(factory, prop, ran));
			kb.addRange(p, visitor.result());
		}
	}

	private void loadDataProperty(OWLOntology ontology, OWLDataProperty prop) throws OWLException {
		ATermAppl p = term(prop);
		Iterator it = null;
		
		kb.addDatatypeProperty(p);
		
		if (prop.isFunctional(ontology)) {
//		    kb.getExplanationTable().setSourceAxiom(new OWLFunctionalPropertyAxiomImpl(factory, prop));
			kb.addFunctionalProperty(p);
		}

		it = prop.getSuperProperties(ontology).iterator();
		while(it.hasNext()) {
		    OWLDataProperty sup = (OWLDataProperty) it.next();
			
			kb.addSubProperty(p, term(sup));
		}
		
		it = prop.getDomains(ontology).iterator();
		while(it.hasNext()) {
			OWLDescription dom = (OWLDescription) it.next();
			visitor.reset();
			dom.accept(visitor);
		
//			kb.getExplanationTable().setSourceAxiom(new OWLPropertyDomainAxiomImpl(factory, prop, dom));
			kb.addDomain(p, visitor.result());
		}

		it = prop.getRanges(ontology).iterator();
		while(it.hasNext()) {
			OWLDataRange ran = (OWLDataRange) it.next();
			visitor.reset();
			ran.accept(visitor);
			
//			kb.getExplanationTable().setSourceAxiom(new OWLDataPropertyRangeAxiomImpl(factory, prop, ran));
			kb.addRange(p, visitor.result());
		}		
	}

	private void loadDataType(OWLOntology ontology, OWLDataType datatype) throws OWLException {
		if(!kb.getDatatypeReasoner().isDefined(datatype.getURI().toString()))
			throw new OWLException("Unsupported datatype " + datatype.getURI() + " in ontology " + ontology.getURI()); 
	}

	private void loadClassAxiom(OWLClassAxiom axiom) throws OWLException {
		visitor.reset();
		axiom.accept(visitor);
	}

	private void loadPropertyAxiom(OWLPropertyAxiom axiom) throws OWLException {
		visitor.reset();
		axiom.accept(visitor);
	}

	private void loadIndividualAxiom(OWLIndividualAxiom axiom) throws OWLException {
		visitor.reset();
		axiom.accept(visitor);
	}
	/**
	 * @return Returns the ontologies.
	 */
	public Set getOntologies() {
		return ontologies;
	}

}
