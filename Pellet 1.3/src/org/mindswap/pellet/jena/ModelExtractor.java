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
 * Created on Aug 24, 2004
 */
package org.mindswap.pellet.jena;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.Role;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.SetUtils;

import aterm.ATermAppl;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.ReasonerVocabulary;

/**
 * Extract a Jena model that contains the information Pellet inferred. Models can be generated
 * about classes, properties or individuals. Note that individual models do not contain any
 * information about property assertions, it just contains type assertions about individuals. 
 * 
 * @author Evren Sirin
 */
public class ModelExtractor {
    /**
     * Reasoner being used to extract the information
     */
	private OWLReasoner reasoner;
	
	/**
	 * Associated KB
	 */
	private KnowledgeBase kb;
	
	/**
	 * The parameter that controls whether all or only direct subclass, subproperty
	 * and type assertions will be included in the result
	 */
	private boolean verbose;
	
	/**
	 * When this parameter is set Jena specific triple related to directSubclassOf
	 * is included in the result. 
	 */
	private boolean includeDirects;
	
	/**
	 * Initialize an empty extractor
	 *
	 */
	public ModelExtractor() {
	}
	
	/**
	 * Initialize the extractor with a reasoner
	 */
	public ModelExtractor(OWLReasoner reasoner) {
	    setReasoner(reasoner);
	}
	
	public Model extractModel() {
		Model model = ModelFactory.createDefaultModel();
		
		model = extractClassModel(model);
		model = extractPropertyModel(model);
		if(kb.isRealized())
			model = extractIndividualModel(model);
		
		return model;
	}

	public Model extractClassModel() {
		return extractClassModel(ModelFactory.createDefaultModel());		
	}

	public Model extractPropertyModel() {
		return extractPropertyModel(ModelFactory.createDefaultModel());		
	}
	
	public Model extractIndividualModel() {
		kb.realize();
		
		return extractIndividualModel(ModelFactory.createDefaultModel());
	}

	public Model extractClassModel(Model model) {
		kb.classify();
		
		Set classes = new HashSet( kb.getClasses() );
		classes.add( ATermUtils.TOP );
        classes.add( ATermUtils.BOTTOM );
		for( Iterator i = classes.iterator(); i.hasNext(); ) {
			ATermAppl c = (ATermAppl) i.next();
			Resource r = reasoner.toJenaResource(c);			
			model.add(r, RDF.type, OWL.Class);
			
			model.add( OWL.Nothing, RDFS.subClassOf, r );
			
			Set eqs = kb.getEquivalentClasses(c);
			eqs.add(c);
			for(Iterator j = eqs.iterator(); j.hasNext(); ) {
				Resource eq = reasoner.toJenaResource((ATermAppl) j.next());
				model.add(r, OWL.equivalentClass, eq);
				model.add(r, RDFS.subClassOf, eq);
				model.add(eq, RDFS.subClassOf, r);
			}
			
			Set supers = verbose ? kb.getSuperClasses(c) : kb.getSuperClasses(c, true);
			supers = SetUtils.union(supers);
			for(Iterator j = supers.iterator(); j.hasNext(); ) {
				Resource sup = reasoner.toJenaResource((ATermAppl) j.next());
				model.add(r, RDFS.subClassOf, sup);										
			}
			
			if(includeDirects) {
				Set direct = verbose ? SetUtils.union(kb.getSuperClasses(c, true)) : supers;
				for(Iterator j = direct.iterator(); j.hasNext(); ) {
					Resource sup = reasoner.toJenaResource((ATermAppl) j.next());
					model.add(r, ReasonerVocabulary.directSubClassOf, sup);										
				}			    
			}
		}
		
		return model;
	}

	public Model extractIndividualModel(Model model) {
		kb.realize();

		Set individuals = kb.getIndividuals();
		for(Iterator i = individuals.iterator(); i.hasNext(); ) {
			ATermAppl ind = (ATermAppl) i.next();
			Resource r = reasoner.toJenaResource(ind);
			
			Set types = verbose ? kb.getTypes(ind) : kb.getTypes(ind, true);
			types = SetUtils.union(types);
			for(Iterator j = types.iterator(); j.hasNext(); ) {
				ATermAppl sub = (ATermAppl) j.next();
				model.add(r, RDF.type, reasoner.toJenaResource(sub));													
			}
			
			if(includeDirects) {
				Set direct = verbose ? SetUtils.union(kb.getTypes(ind, true)) : types;
				for(Iterator j = direct.iterator(); j.hasNext(); ) {
					Resource sup = reasoner.toJenaResource((ATermAppl) j.next());
					model.add(r, ReasonerVocabulary.directRDFType, sup);										
				}			    
			}
		}
		
		return model;
	}	
	
	public Model extractPropertyModel(Model model) {
		kb.prepare();
		
		Collection props = kb.getRBox().getRoles();
		for(Iterator i = props.iterator(); i.hasNext(); ) {
			Role role = (Role) i.next();
			ATermAppl name = role.getName();
			
			if(role.isAnon())
			    continue;
			
			Resource r = reasoner.toJenaResource(name);			
			if(role.isDatatypeRole())
			    model.add(r, RDF.type, OWL.DatatypeProperty);
			else if(role.isObjectRole())
			    model.add(r, RDF.type, OWL.ObjectProperty);
			else
			    continue;
			
            model.add(r, RDF.type, RDF.Property);
			if(role.isFunctional())
			    model.add(r, RDF.type, OWL.FunctionalProperty);
			if(role.isInverseFunctional())
			    model.add(r, RDF.type, OWL.InverseFunctionalProperty);
			if(role.isTransitive())
			    model.add(r, RDF.type, OWL.TransitiveProperty);
			if(role.isSymmetric())
			    model.add(r, RDF.type, OWL.SymmetricProperty);		
			
			Set eqs = kb.getAllEquivalentProperties(name);
			for(Iterator j = eqs.iterator(); j.hasNext(); ) {
				ATermAppl eq = (ATermAppl) j.next();
				Resource eqR = reasoner.toJenaResource( eq );
				model.add(r, OWL.equivalentProperty, eqR);
				model.add(r, RDFS.subPropertyOf, eqR);			
			}
			
			// TODO add inverse properties
			
			Set supers = verbose ? kb.getSuperProperties(name) : kb.getSuperProperties(name, true);
			supers = SetUtils.union(supers);
			for(Iterator j = supers.iterator(); j.hasNext(); ) {
				ATermAppl sup = (ATermAppl) j.next();
				model.add( r, RDFS.subPropertyOf, reasoner.toJenaResource( sup ) );													
			}
			
			if(includeDirects) {
				Set direct = verbose ? SetUtils.union(kb.getSuperProperties(name, true)) : supers;
				for(Iterator j = direct.iterator(); j.hasNext(); ) {
				    ATermAppl sup = (ATermAppl) j.next();
					model.add( r, ReasonerVocabulary.directSubPropertyOf,  reasoner.toJenaResource( sup ) );										
				}			    
			}
			
//			Set domains = kb.getDomains(name);
//			for(Iterator j = domains.iterator(); j.hasNext(); ) {
//				ATermAppl domain = (ATermAppl) j.next();
//				if(domain.getArity() == 0)
//				    model.add(r, RDFS.domain, reasoner.toJenaResource(domain));													
//			}
//			
//			Set ranges = kb.getRanges(name);
//			for(Iterator j = ranges.iterator(); j.hasNext(); ) {
//				ATermAppl range = (ATermAppl) j.next();
//				if(range.getArity() == 0)
//				    model.add(r, RDFS.range, reasoner.toJenaResource(range));													
//			}
		}
		
		return model;
	}

    /**
     * Returns true if direct subclass/subproperty relations will be
     * included in extractiont
     */
    public boolean isIncludeDirects() {
        return includeDirects;
    }
    /**
     * 
     */
    public void setIncludeDirects(boolean includeDirects) {
        this.includeDirects = includeDirects;
    }
    /**
     * @return Returns the verbose.
     */
    public boolean isVerbose() {
        return verbose;
    }
    /**
     * @param verbose The verbose to set.
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    /**
     * @return Returns the reasoner.
     */
    public OWLReasoner getReasoner() {
        return reasoner;
    }
    /**
     * @param reasoner The reasoner to set.
     */
    public void setReasoner(OWLReasoner reasoner) {
        this.reasoner = reasoner;
		this.kb = reasoner.getKB();
    }
}
