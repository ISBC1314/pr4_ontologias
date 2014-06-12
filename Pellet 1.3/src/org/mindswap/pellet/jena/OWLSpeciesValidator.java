//The MIT License
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

package org.mindswap.pellet.jena;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.Role;
import org.mindswap.pellet.exceptions.InternalReasonerException;
import org.mindswap.pellet.exceptions.UnsupportedFeatureException;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Namespaces;

import aterm.AFun;
import aterm.ATerm;
import aterm.ATermAppl;
import aterm.ATermList;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.lre.graph.Graph;
import com.lre.graph.UndirectedGraph;
import com.lre.graph.Vertex;

/**
 * Validate the species of an ontology given as a Jena model.
 * 
 * @author Evren Sirin
 */
public class OWLSpeciesValidator {
    public static final String[] LEVELS = {"Lite", "DL", "Full" }; 
    public static final int LITE = 0;
    public static final int DL   = 1;
    public static final int FULL = 2; 	
	    
    public static final int WARNING   = 3;

	/*
	 * predicates related to restrictions (owl:onProperty, owl:allValuesFrom, etc.)
	 * are preprocessed before all the triples are processed. these predicates
	 * are stored in the following list so processTriples function can ignore
	 * the triples with these predicates
	 */
	final static List RESTRICTION_PROPS =
		Arrays.asList(
			new Property[] {
				OWL.onProperty,
				OWL.hasValue,
				OWL.allValuesFrom,
				OWL.someValuesFrom,
				OWL.minCardinality,
				OWL.maxCardinality,
				OWL.cardinality,
				});

	// These are the properpties that belong to RDFS namespace but sometimes used
	// mistakenly in OWL namespace. When we see one of these props we automatically
	// correct it and print a warning
	final static List RDFS_PROPS =
		Arrays.asList(
			new String[] {
				"subClassOf",
				"subPropertyOf",
				"domain",
				"range",
				"label",
				"comment",
				"isDefinedBy",
				"seeAlso" });

	private Map lists = new HashMap();
	private Map restrictions = new HashMap();
	private Set classes = new HashSet();
	private Set mDataranges = new HashSet();    
	private Map ontologies = new HashMap();

	private KnowledgeBase kb = null;

	public Model model = null;

	public boolean canOutputDL = false;
	public Model missingTriples = ModelFactory.createDefaultModel();

	private OWLSpeciesReport report;

	// structure sharing and clique detection
	private boolean mCheckStructureSharing = false;
	private Set mVisitedNodes = new HashSet();
	private Set mUsedBnodes = new HashSet();
	private Graph mDisjointWithGraph = new Graph();
	private Graph mEquivalentClassGraph = new Graph();

	public OWLSpeciesValidator() {
	    
	}
	
	public OWLSpecies validate(Model model) {
	    this.model = model;
	    
		mUsedBnodes = new HashSet();
		mDisjointWithGraph = new Graph();
		mCheckStructureSharing = false;
		mEquivalentClassGraph = new Graph();
		mVisitedNodes = new HashSet();
		
		canOutputDL = true;

		kb = new KnowledgeBase();
        kb.addAnnotationProperty(node2term(RDFS.label));
        kb.addAnnotationProperty(node2term(RDFS.comment));
        kb.addAnnotationProperty(node2term(RDFS.seeAlso));
        kb.addAnnotationProperty(node2term(RDFS.isDefinedBy));
        kb.addAnnotationProperty(node2term(OWL.versionInfo));
        kb.addOntologyProperty(node2term(OWL.backwardCompatibleWith));
        kb.addOntologyProperty(node2term(OWL.priorVersion));
        kb.addOntologyProperty(node2term(OWL.incompatibleWith));
        
		lists = new HashMap();
		restrictions = new HashMap();

		missingTriples = ModelFactory.createDefaultModel();
		report = new OWLSpeciesReport();

		processTypes();
		processTriples();
		    
		processRestrictions();
		
		// if there isn't any unsupported feature, the file is Full and we have found the missing triples
		// then it is possible to output the DL version of the input file
		canOutputDL &= (missingTriples.size() > 0);

		return new OWLSpecies(report, canOutputDL ? missingTriples : null);
	}

	private ATermList createList(Resource r) {
		if (r.equals(RDF.nil))
			return ATermUtils.EMPTY_LIST;

		if(!r.hasProperty(RDF.first)) {
		    report.addMessage(WARNING, "Invalid List", "The list " + r + " does not have a rdf:first property");
		    return ATermUtils.EMPTY_LIST;
		}
		if(!r.hasProperty(RDF.rest)) {
		    report.addMessage(WARNING, "Invalid List", "The list " + r + " does not have a rdf:rest property");
		    return ATermUtils.EMPTY_LIST;
		}
		
		RDFNode first = r.getProperty(RDF.first).getObject();
		Resource rest = r.getProperty(RDF.rest).getResource();

		if ((first instanceof Resource) && ((Resource) first).isAnon() && mCheckStructureSharing) {
			if (mUsedBnodes.contains(first.toString())) {
				report.addMessage(FULL, "Structure Sharing", "The bNode " + first.toString() + " is used in multiple structures");
				canOutputDL = false;
				return ATermUtils.EMPTY_LIST;
			}
			else
				mUsedBnodes.add(first.toString());
		} // if

//		ATerm first = node2term(theFirst);
//		if (restrictions.containsKey(first))
//			first = (ATerm) restrictions.get(first);
		
		return ATermUtils.makeList(node2term(first), createList(rest));
	} // createList

	private boolean check(Resource orig, Resource curr) {
		if (curr == null || curr.isAnon()) {
			if (curr != null && orig.equals(curr))
				return false;
			StmtIterator sIter = (curr != null ? curr : orig).listProperties();
			while (sIter.hasNext()) {
				Statement stmt = sIter.nextStatement();
				Resource pred = stmt.getPredicate();
				RDFNode obj = stmt.getObject();
				if (pred.equals(OWL.intersectionOf) || pred.equals(OWL.unionOf)) {
					if (!checkList(orig, (Resource) obj)) {
					    report.addMessage(FULL, "Cycle in Class Description", "Definition for " + stmt.getSubject().toString() + " is cyclic");
						return false;
					} // if
				} // if
				else if (orig.equals(obj)) {
					if (pred.equals(OWL.equivalentClass))
					    report.addMessage(FULL, "Cycle in Class Description", "Definition for " + stmt.getSubject().toString() + " is cyclic");
					else
					    report.addMessage(FULL, "Cycle in Class Description", "Definition for " + stmt.getSubject().toString() + " is cyclic");
					return false;
				} // if
			} // while
			return true;
		} // if
		else
			return true;
	} // check

	private boolean checkList(Resource orig, Resource theList) {
		List v = new ArrayList();
		Resource theFirst = theList.getProperty(RDF.first).getResource();
		Resource theRest = theList.getProperty(RDF.rest).getResource();
		v.add(theFirst);
		while (!theRest.equals(RDF.nil)) {
			theFirst = theRest.getProperty(RDF.first).getResource();
			theRest = theRest.getProperty(RDF.rest).getResource();
			v.add(theFirst);
		} // while

		for (int i = 0; i < v.size(); i++) {
			Resource elem = (Resource) v.get(i);
			if (!elem.isAnon())
				continue;

			if (orig.equals(elem))
				return false;
			else if (elem.hasProperty(OWL.intersectionOf)) {
				if (elem.getProperty(OWL.intersectionOf).getResource().equals(theList))
					return false;
				else
					return checkList(orig, elem.getProperty(OWL.intersectionOf).getResource());
			} // else if
			else if (elem.hasProperty(OWL.unionOf)) {
				if (elem.getProperty(OWL.unionOf).getResource().equals(theList))
					return false;
				else
					return checkList(orig, elem.getProperty(OWL.unionOf).getResource());
			} // else if
		} // for
		return true;
	} // checkList

	private void processTypes() throws UnsupportedFeatureException {
		StmtIterator i = model.listStatements(null, RDF.first, (Resource) null);

		// list pre-processing
		while (i.hasNext()) {
			Statement stmt = i.nextStatement();
			StmtIterator si = model.listStatements(null, null, stmt.getSubject());
			while (si.hasNext()) {
				Statement aStmt = si.nextStatement();
				if (!aStmt.getPredicate().equals(RDF.first) && !aStmt.getPredicate().equals(RDF.rest)) {
					Resource s = stmt.getSubject();
					//ATerm st = node2term(s, true);
					if (aStmt.getPredicate().equals(OWL.intersectionOf) || aStmt.getPredicate().equals(OWL.unionOf))
						mCheckStructureSharing = true;
					lists.put(s, createList(s));
					mCheckStructureSharing = false;
					break;
				} // if
			} // while
		} // while

		i = model.listStatements(null, RDF.type, (Resource) null);
		List processLater = new ArrayList();
		while (i.hasNext()) {
			Statement stmt = i.nextStatement();
			Resource s = stmt.getSubject();
			Resource o = stmt.getResource();

			ATermAppl st = node2term(s);
			ATermAppl ot = node2term(o);

			if (s.isAnon() && !check(s, null)) {
				//check(s,null);
				canOutputDL = false;
				//report.addMessage(FULL, "Cycle in class description detected for: "+s.toString());
			}

			if (o.equals(OWL.Class)) {
				if (!isClass(st)) {
					String previousDefinition = isIndividual(st) ? "n individual" : " property";
					String problem = " is defined both as a class and a" + previousDefinition;
					report.addMessage(FULL, "Multiple Types", "Resource %1% " + problem, st); 
				} // if

				addClass(st);
			} // else if
			else if (o.equals(RDFS.Class)) {
				processLater.add(stmt);
			}
			else if (o.equals(RDFS.Datatype)) {
				kb.addDatatype(st);
				printDebug("datatype(" + s + ")");
			} // else if
			else if (o.equals(OWL.Thing)) {
				if (!isIndividual(st)) {
					String previousDefinition = isClass(st) ? "class" : "property";
					String problem = " is defined both as an individual and a " + previousDefinition;
					report.addMessage(FULL, "Multiple Types", "Resource %1% " + problem, st); 
				} // if
				if (!kb.getABox().isNode(st))
					kb.addIndividual(st);
				kb.addType(st, ATermUtils.TOP);
			} // else if
			else if (o.equals(RDF.List)) {
				// Jena 2 does not create explicit x rdf:type rdf:List triples
				// so we should handle this by looking at rdf:first elements and
				// skip this triple in case it exists
				//lists.put(st, createList(s));
			}
			else if (o.equals(OWL.Restriction)) {
				restrictions.put(s, st);

			} // else if
			else if (o.equals(OWL.AllDifferent)) {
			} // else if
			else if (o.equals(OWL.ObjectProperty)) {
				if (!isProperty(st)) {
					String previousDefinition = isIndividual(st) ? "n individual" : " class";
					String problem = " is defined both as an onbject property and a" + previousDefinition;
					report.addMessage(FULL, "Multiple Types", "Resource %1% " + problem, st); 
				} // if
				else {
					kb.getABox().getRBox().addObjectRole(st);
					printDebug("object-property(" + st + ")");
				}
			} // else if
			else if (o.equals(OWL.DatatypeProperty)) {
				if (!isProperty(st)) {
					String previousDefinition = isIndividual(st) ? "n individual" : " class";
					String problem = " is defined both as a datatype property and a" + previousDefinition;
					report.addMessage(FULL, "Multiple Types", "Resource %1% " + problem, st); 
				} // if
				kb.getABox().getRBox().addDatatypeRole(st);
				printDebug("datatype-property(" + st + ")");
			} // else if
			else if (o.equals(OWL.AnnotationProperty)) {
				if (!isProperty(st)) {
					String previousDefinition = isIndividual(st) ? "n individual" : " class";
					String problem = " is defined both as aa annotation property and a" + previousDefinition;
					report.addMessage(FULL, "Multiple Types", "Resource %1% " + problem, st); 
				} // if
				kb.getABox().getRBox().addAnnotationRole(st);
				printDebug("annotation-property(" + st + ")");
			} // else if
			else if (o.equals(RDF.Property)) {
				processLater.add(stmt);
			} // else if
			else if (o.equals(OWL.TransitiveProperty)) {
				processLater.add(stmt);
			} // else if
			else if (o.equals(OWL.SymmetricProperty)) {
				processLater.add(stmt);
			} // else if
			else if (o.equals(OWL.FunctionalProperty)) {
				processLater.add(stmt);
			} // else if
			else if (o.equals(OWL.InverseFunctionalProperty)) {
				processLater.add(stmt);
			} // else if
			else if (o.equals(OWL.Ontology)) {
				if (!isOntology(s)) {
					addOntology(s);
					printDebug("(ontology " + s + ")");
				}
			} // else if
			else if (o.equals(OWL.DataRange)) {
				//unsupportedFeatures.add("owl:DataRange is not supported yet!");
				//mDataranges.put(s.toString(),s);
				mDataranges.add(st);

				kb.addDatatype(st);

				printDebug("(datarange " + s + ")");
			} // else if
			else if (isInvalidOWLTerm(o)) {
			    report.addMessage(FULL, "Invalid OWL Term", "URI %1% does not belong to OWL namespace", ot); 
			} // else if
			else {
				addType(st, ot);
				// to check if ot is a class 
				processLater.add(stmt);
			} // else
		} // while

		for (int j = 0; j < processLater.size(); j++) {
			Statement stmt = (Statement) processLater.get(j);
			Resource s = stmt.getSubject();
			Resource o = stmt.getResource();
			ATermAppl st = node2term(s);
			ATermAppl obj = node2term(o);

			if (o.equals(RDFS.Class)) {
				if (!s.hasProperty(RDF.type, OWL.Restriction) && !s.hasProperty(RDF.type, OWL.Class)) {
					report.addMessage(FULL, "RDFS Vocabulary", "Using  rdfs:Class instead of owl:Class"); 
					classes.add(st);
				}
			} // if
			else if (o.equals(OWL.FunctionalProperty)) {
				if (!isProperty(st)) {
					String previousDefinition = isIndividual(st) ? "n individual" : " class";
					String problem = st + " is defined both as a property and a" + previousDefinition;
					report.addMessage(FULL, "Multiple Types", "Resource %1% " + problem, st); 
				} // if
				else {
					if (!kb.getABox().getRBox().isRole(st)) {
						kb.getABox().getRBox().addObjectRole(st);
						report.addMessage(FULL, "Untyped Property", "%1% needs to be defined as an Object or Data property", st); 						
						addMissingTriple(s, RDF.type, OWL.ObjectProperty);
					} // if
					kb.addFunctionalProperty(st);
					printDebug("functional(" + st + ")");
				}
			} // if
			else if (o.equals(OWL.InverseFunctionalProperty)) {
				if (kb.isDatatypeProperty(st) || kb.isAnnotationProperty(st)) {
					String previousDefinition = kb.isDatatypeProperty(st) ? " datatype" : "n annotation";
					String problem = " is defined both as a" + previousDefinition + " property and as InverseFunctionalProperty";
					report.addMessage(FULL, "Multiple Types", "Resource %1%" + problem, st);
				}
				else {
					if (!kb.getABox().getRBox().isRole(st)) {
						// InverseFunctional properties are by definition object properties
						kb.getABox().getRBox().addObjectRole(st);
					} // if
					kb.addInverseFunctionalProperty(st);
					printDebug("inv-functional(" + st + ")");
				}
			} // else
			else if (o.equals(OWL.TransitiveProperty)) {
				if (kb.isDatatypeProperty(st) || kb.isAnnotationProperty(st)) {
					String previousDefinition = kb.isDatatypeProperty(st) ? " datatype" : "n annotation";
					String problem = " is defined both as a" + previousDefinition + " property and as a TransitiveProperty";
					report.addMessage(FULL, "Multiple Types", "Resource %1% " + problem, st);
				}
				else {
					if (!kb.getABox().getRBox().isRole(st)) {
						// transitive properties are by definition object properties
						kb.getABox().getRBox().addObjectRole(st);
					} // if
					kb.addTransitiveProperty(st);
					printDebug("transitive-role(" + st + ")");
				}
			} // else if
			else if (o.equals(OWL.SymmetricProperty)) {
				if (kb.isDatatypeProperty(st) || kb.isAnnotationProperty(st)) {
					String previousDefinition = kb.isDatatypeProperty(st) ? " datatype" : "n annotation";
					String problem = " is defined both as a" + previousDefinition + " property and as a SymmetricProperty";
					report.addMessage(FULL, "Multiple Types", "Resource %1% " + problem, st);
				}
				else {
					if (!kb.getABox().getRBox().isRole(st)) {
						// symmetric properties are by definition object properties
						kb.getABox().getRBox().addObjectRole(st);
					} // if
					kb.addInverseProperty(st, st);
					printDebug("inverse-role(" + st + " " + st + ")");
				}
			} // else if
			else if (o.equals(RDF.Property)) {

				if (!isProperty(st)) {
					String previousDefinition = isIndividual(st) ? "n individual" : " class";
					String problem = " is defined both as a property and a" + previousDefinition;
					report.addMessage(FULL, "Multiple Types", "Resource %1% " + problem, st);

				} // if
				else if (!kb.isProperty(st)) {
					report.addMessage(FULL, "RDF Vocabulary", "Using rdf:Property instead of owl:[Object|Data]Property for %1% ", st); 
					addMissingTriple(s, RDF.type, OWL.ObjectProperty);
					kb.getABox().getRBox().addObjectRole(st);
					printDebug("plain-role(" + st + ")");
				}
			} // else if
			else {
				isClass(obj, true);
			} // else
		} // for
	} // processTypes

	private void processRestrictions() {
		Iterator e = restrictions.keySet().iterator();
		while (e.hasNext()) {
			Object key = e.next();
			Resource res = (Resource) key;
			
			if (res.hasProperty(OWL.onProperty)) {
				Resource prop = res.getProperty(OWL.onProperty).getResource();
				ATermAppl target = node2term(prop);
				if (!kb.isProperty(target)) {
					report.addMessage(FULL, "Untyped Property", "%1% needs to be defined as owl:[Object|Data]Property", target); 
					isProperty(target, true, "object");
					//kb.getABox().getRBox().addObjectRole(target);
					addMissingTriple(prop, RDF.type, OWL.ObjectProperty);
					printDebug("(object-prop " + target +")");
				}
			} // if

			if (res.hasProperty(OWL.hasValue) && res.getProperty(OWL.hasValue).getObject() instanceof Resource) {
				Resource val = res.getProperty(OWL.hasValue).getResource();
				ATermAppl target = node2term(val);
				if (!kb.getABox().isNode(target)) {
					report.addMessage(FULL, "Untyped Individual", "%1% needs to be defined as an individual", target); 
					kb.addIndividual(target);
					//kb.getABox().getRBox().addObjectRole(target);
					addMissingTriple(val, RDF.type, OWL.Thing);
					printDebug("(individual " + target +")");
				}
			} // if
		} // while	    
	}
	
	private ATermAppl createRestriction(Resource s) throws UnsupportedFeatureException {
		ATermAppl aTerm = ATermUtils.BOTTOM;
		Resource p = null;

		StmtIterator si = s.listProperties(OWL.onProperty);

		if (si.hasNext()) {
			p = si.nextStatement().getResource();

			if (si.hasNext()) {
				canOutputDL = false;
				report.addMessage(FULL, "Invalid Restriction", "A restriction has multiple owl:onProperty values"); 
			}
		}
		else {
			report.addMessage(FULL, "Invalid Restriction", "A restriction has no owl:onProperty values"); 
			return aTerm;
		}

		ATermAppl pt = node2term(p);
		if (!s.isAnon()) {
			report.addMessage(FULL, "Invalid Restriction", "A restriction has a URI " + ATermUtils.makeTermAppl(s.getURI())); 
		} // if
		else if (s.hasProperty(OWL.hasValue)) {
			RDFNode o = s.getProperty(OWL.hasValue).getObject();

			ATermAppl ot = node2term(o);
			ATermAppl nominal = ATermUtils.makeValue(ot);
			aTerm = ATermUtils.makeSomeValues(pt, nominal);
			
			report.addMessage(DL, "Value Restriction", "owl:hasValue construct is used %1%", aTerm); 

		} // else if
		else if (s.hasProperty(OWL.allValuesFrom)) {
			Resource o = (Resource) s.getProperty(OWL.allValuesFrom).getObject();
			ATerm ot = node2term(o);
			aTerm = ATermUtils.makeAllValues(pt, ot);
		} // else if
		else if (s.hasProperty(OWL.someValuesFrom)) {
			Resource o = (Resource) s.getProperty(OWL.someValuesFrom).getObject();
			ATerm ot = node2term(o);
			aTerm = ATermUtils.makeSomeValues(pt, ot);
		} // else if
		else if (s.hasProperty(OWL.minCardinality)) {
		    int cardinality = 0;
			try {
				cardinality = s.getProperty(OWL.minCardinality).getInt();
			} // try
			catch (Exception ex) {
			    RDFNode value = s.getProperty(OWL.minCardinality).getObject();
				try {
				    com.hp.hpl.jena.rdf.model.Literal num = (com.hp.hpl.jena.rdf.model.Literal) value;
				    cardinality = Integer.parseInt(num.getLexicalForm());
				    if(!kb.getDatatypeReasoner().isDefined(num.getDatatypeURI()))
						report.addMessage(FULL, "Invalid XSD Vocabulary", "Using wrong URI " + num.getDatatypeURI() + " for XMLSchema (" + Namespaces.XSD + ")"); 
				} // try
				catch (Exception ex2) {
				    report.addMessage(FULL, "Invalid Cardinality Restriction", "minCardinality restriction on %1% is not an integer: " + value, pt);
				} // catch
			} // catch
			
			if (cardinality < 0)
			    report.addMessage(FULL, "Invalid Cardinality Restriction", "minCardinality restriction on %1% is a negative integer: " + cardinality, pt); 

			else {
			    aTerm =  ATermUtils.makeMin(pt, cardinality);
			    
			    if (cardinality > 1)
			        report.addMessage(DL, "Cardinality Restriction", "minCardinality value is greater than 1 %1% ", aTerm); 			 
			}
		} // else if
		else if (s.hasProperty(OWL.maxCardinality)) {
		    int cardinality = 0;
			try {
				cardinality = s.getProperty(OWL.maxCardinality).getInt();
			} // try
			catch (Exception ex) {
			    RDFNode value = s.getProperty(OWL.maxCardinality).getObject();
				try {
				    com.hp.hpl.jena.rdf.model.Literal num = (com.hp.hpl.jena.rdf.model.Literal) value;
				    cardinality = Integer.parseInt(num.getLexicalForm());
				    if(!kb.getDatatypeReasoner().isDefined(num.getDatatypeURI()))
				        report.addMessage(FULL, "Invalid XSD Vocabulary", "Using wrong URI " + num.getDatatypeURI() + " for XMLSchema (" + Namespaces.XSD + ")"); 
				} // try
				catch (Exception ex2) {
				    report.addMessage(FULL, "Invalid Cardinality Restriction", "maxCardinality restriction on %1% is not an integer: " + value, pt);
				} // catch
			} // catch
			
			if (cardinality < 0)
				report.addMessage(FULL, "Invalid Cardinality Restriction", "maxCardinality restriction on %1% is a negative integer: " + cardinality, pt); 

			else { 
			    aTerm = ATermUtils.makeMax(pt, cardinality);
			    
			    if (cardinality > 1)
			        report.addMessage(DL, "Cardinality Restriction", "maxCardinality value is greater than 1 %1% ", aTerm);
			}
		} // else if
		else if (s.hasProperty(OWL.cardinality)) {
		    int cardinality = 0;
			try {
				cardinality = s.getProperty(OWL.cardinality).getInt();
			} // try
			catch (Exception ex) {
			    RDFNode value = s.getProperty(OWL.cardinality).getObject();
				try {
				    com.hp.hpl.jena.rdf.model.Literal num = (com.hp.hpl.jena.rdf.model.Literal) value;
				    cardinality = Integer.parseInt(num.getLexicalForm());
				    if(!kb.getDatatypeReasoner().isDefined(num.getDatatypeURI()))
						report.addMessage(FULL, "Invalid XSD Vocabulary", "Using wrong URI " + num.getDatatypeURI() + " for XMLSchema (" + Namespaces.XSD + ")"); 		    
				} // try
				catch (Exception ex2) {
				    report.addMessage(FULL, "Invalid Cardinality Restriction", "cardinality restriction on %1% is not an integer: " + value, pt);
				} // catch
			} // catch
			
			if (cardinality < 0)
			    report.addMessage(FULL, "Invalid Cardinality Restriction", "cardinality restriction on %1% is a negative integer: " + cardinality, pt); 
			else { 
			    aTerm = ATermUtils.makeCard(pt, cardinality);
			    
			    if (cardinality > 1)
			        report.addMessage(DL, "Cardinality Restriction", "cardinality value is greater than 1 restriction(%1% cardinality(" + cardinality + "))", pt); 			 			    
			}
		} // else if
		else {
		    report.addMessage(FULL, "Invalid Cardinality Restriction", "Restriction does not have any of owl:allValuesFrom, owl:someValuesFrom, owl:hasValue or cardinality restrictions");
		} // else if
		
		
		return aTerm;
	} // createRestriction

	/* each resource node is converted to an ATerm. Some
	 * resources such as owl:Thing is converted to special
	 * terms. If the node is a restriction than the whole
	 * restriction construct (e.g. max(p, 1)) is returned
	 * if the resource is in the subject place then don't return
	 * the complex restriction because having a complex type
	 * on the left hand side of a tbox axiom causes a problem
	 */
	protected ATermAppl node2term(RDFNode node) {
		ATermAppl aTerm = null;

		if (node.equals(OWL.Thing))
			return ATermUtils.TOP;
		else if (node.equals(OWL.Nothing))
			return ATermUtils.BOTTOM;
		else if (node instanceof com.hp.hpl.jena.rdf.model.Literal) {
			com.hp.hpl.jena.rdf.model.Literal l = (com.hp.hpl.jena.rdf.model.Literal) node;
			String datatypeURI = l.getDatatypeURI();
			if (datatypeURI != null)
			    aTerm = ATermUtils.makeTypedLiteral(l.getString(), datatypeURI);
			else
			    aTerm = ATermUtils.makePlainLiteral(l.getString(), l.getLanguage());
		}
		else if (node instanceof Resource) {
			Resource r = (Resource) node;

			if (r.getModel() != null && r.hasProperty(OWL.onProperty))
				aTerm = createRestriction(r);
			else if (r.isAnon()) {
				if (r.hasProperty(OWL.intersectionOf)) {
					ATermList list = (ATermList) lists.get(r.getProperty(OWL.intersectionOf).getResource());
					aTerm = ATermUtils.makeAnd(list);
				}
				else if (r.hasProperty(OWL.unionOf)) {
					ATermList list = (ATermList) lists.get(r.getProperty(OWL.unionOf).getResource());
					aTerm = ATermUtils.makeOr(list);
				}
				else if (r.hasProperty(OWL.oneOf)) {
					ATermList list = (ATermList) lists.get(r.getProperty(OWL.oneOf).getResource());
					ATermList result = ATermUtils.EMPTY_LIST;
					if(list != null) {
						for (ATermList l = list; !l.isEmpty(); l = l.getNext()) {
							ATermAppl c = (ATermAppl) l.getFirst();
							ATermAppl nominal = ATermUtils.makeValue(c);
							result = result.insert(nominal);
						}

						aTerm = ATermUtils.makeOr(result);
					}
					else
						aTerm = ATermUtils.BOTTOM;
				}
				else if (r.hasProperty(OWL.complementOf)) {
					ATerm complement = node2term(r.getProperty(OWL.complementOf).getResource());
					aTerm = ATermUtils.makeNot(complement);
				}
				else
					aTerm = ATermUtils.makeTermAppl(PelletOptions.BNODE + r.toString());
			}
			else {
				aTerm = ATermUtils.makeTermAppl(r.getURI());
			}
		}

		return aTerm;
	}

	private void addType(ATermAppl ind, ATermAppl c) {
		if (!isIndividual(ind)) {
			String previousDefinition = isClass(ind) ? "class" : "property";
			String problem = " is defined both as an individual and a " + previousDefinition;
			report.addMessage(FULL, "Multiple Types", "Resource %1% " + problem, ind); 
		} // if
		if (!kb.isIndividual(ind))
			kb.addIndividual(ind);
		
		kb.getABox().addType(ind, c);
		printDebug("type(" + ind + " " + c + ")");
	}
	
	private void addClass(ATermAppl c) {
		if (ATermUtils.isPrimitive(c)) {
			classes.add(c);
			printDebug("class(" + c + ")");
		}
	}

	private void addSubClass(ATerm c1, ATerm c2) {
		if(!c1.equals(c2)) {
			printDebug("sub(" + c1 + ", " + c2 + ")");
		}
	}

	private void addSameClass(ATermAppl c1, ATermAppl c2) {
		if (!c1.equals(c2))
			printDebug("same(" + c1 + ", " + c2 + ")");
	}

	private boolean isInvalidOWLTerm(Resource r) {
		if (!r.isAnon()
			&& r.getURI().startsWith(OWL.getURI())
			&& !r.equals(OWL.AllDifferent)
			&& !r.equals(OWL.allValuesFrom)
			&& !r.equals(OWL.AnnotationProperty)
			&& !r.equals(OWL.backwardCompatibleWith)
			&& !r.equals(OWL.cardinality)
			&& !r.equals(OWL.Class)
			//&& !r.equals(OWL.comment)
			&& !r.equals(OWL.complementOf)
			&& !r.equals(RDFS.Datatype)
			&& !r.equals(OWL.DatatypeProperty)
			&& !r.equals(OWL.DeprecatedClass)
			&& !r.equals(OWL.DeprecatedProperty)
			&& !r.equals(OWL.differentFrom)
			&& !r.equals(OWL.disjointWith)
			&& !r.equals(OWL.distinctMembers)
			//&& !r.equals(OWL.domain)
			&& !r.equals(OWL.equivalentClass)
			&& !r.equals(OWL.equivalentProperty)
			&& !r.equals(OWL.FunctionalProperty)
			&& !r.equals(OWL.hasValue)
			&& !r.equals(OWL.imports)
			&& !r.equals(OWL.incompatibleWith)
			&& !r.equals(OWL.intersectionOf)
			&& !r.equals(OWL.InverseFunctionalProperty)
			&& !r.equals(OWL.inverseOf)
			//&& !r.equals(OWL.label)
			//&& !r.equals(OWL.List)
			//&& !r.equals(OWL.Literal)
			&& !r.equals(OWL.maxCardinality)
			&& !r.equals(OWL.minCardinality)
			//&& !r.equals(OWL.nil)
			&& !r.equals(OWL.Nothing)
			&& !r.equals(OWL.ObjectProperty)
			&& !r.equals(OWL.oneOf)
			&& !r.equals(OWL.onProperty)
			&& !r.equals(OWL.Ontology)
			&& !r.equals(OWL.priorVersion)
			//&& !r.equals(OWL.range)
			&& !r.equals(OWL.Restriction)
			&& !r.equals(OWL.sameAs)
			&& !r.equals(OWL.someValuesFrom)
			//&& !r.equals(RDFS.subClassOf)
			//&& !r.equals(OWL.subPropertyOf)
			&& !r.equals(OWL.SymmetricProperty)
			&& !r.equals(OWL.Thing)
			&& !r.equals(OWL.TransitiveProperty)
			//&& !r.equals(OWL.type)
			&& !r.equals(OWL.unionOf)
			&& !r.equals(OWL.versionInfo))
			return true;
		else
			return false;
	} // isInvalidOWLTerm

	private void processTriples() throws UnsupportedFeatureException {
		mDisjointWithGraph = new Graph();
		for (StmtIterator i = model.listStatements(); i.hasNext();) {
			Statement stmt = (Statement) i.next();
			Resource s = stmt.getSubject();
			Resource p = stmt.getPredicate();
			RDFNode o = stmt.getObject();

			// structure sharing detection
			if (o instanceof Resource && ((Resource) o).isAnon()) {
				if (p.equals(OWL.complementOf)
					|| p.equals(RDF.type)
					|| p.equals(OWL.someValuesFrom)
					|| p.equals(OWL.allValuesFrom)) {
					if (mUsedBnodes.contains(o.toString())) {
						canOutputDL = false;
						report.addMessage(FULL, "Structure Sharing", "The bNode " + o.toString() + " is used in multiple structures");
					}
					else
						mUsedBnodes.add(o.toString());
				} // if
			} // if

			
			if (isInvalidOWLTerm(p)) {
			    report.addMessage(FULL, "Invalid OWL Term", "Property %1% does not belong to OWL namespace", ATermUtils.makeTermAppl(p.toString())); 

				String localName = p.getLocalName();
				if (RDFS_PROPS.contains(localName)) {
				    report.addMessage(WARNING, "Invalid OWL Term", "Using rdfs:" + localName + " instead owl:" + localName); 
					p = ResourceFactory.createProperty(RDFS.getURI(), localName);
					canOutputDL = false;
				}
				else {
					continue;
				}

			} // if

			ATermAppl st = node2term(s);
			ATermAppl pt = node2term(p);
			ATermAppl ot = node2term(o);
			//print("Process triple(" + st + ", " + pt + ", " + ot + ")");

			if (p.equals(RDF.type)) {
				// these triples have been processed before so don't do anything
			}
			else if (RESTRICTION_PROPS.contains(p)) {
				if (!s.hasProperty(RDF.type, OWL.Restriction)) {
					canOutputDL = false;
					report.addMessage(FULL, "Invalid Restriction", "The property " + p + " is used with a subject which is not a restriction");
				}
			}
			else if (p.equals(OWL.intersectionOf)) {
				if (s.isAnon())
					report.addMessage(DL, "Anonymous Intersection Class", "owl:intersectionOf triples cannot have an anonymous subject in OWL Lite");

				ATermList list = (ATermList) lists.get(o);
				if (list == null) {
				    report.addMessage(FULL, "Invalid Intersection Class", "owl:intersectionOf should point to a rdf:List structure");
				    continue;
				}
				for (ATermList l = list; !l.isEmpty(); l = l.getNext()) {
					ATermAppl c = (ATermAppl) l.getFirst();
					if (!isClass(c, true)) {
						String previousDefinition = isIndividual(st) ? "n individual" : " property";
						report.addMessage(FULL, "Invalid Intersection Class", "owl:intersectionOf includes %1% which is a" + previousDefinition, c); 
					} // if
				} // for
				
				ATermAppl conjunction = ATermUtils.makeAnd(list);
				addSameClass(st, conjunction);
			} // else if
			else if (p.equals(OWL.unionOf)) {
				ATermList list = (ATermList) lists.get(o);
				if (list == null) {
				    report.addMessage(FULL, "Invalid Union Class", "owl:unionOf should point to a rdf:List structure");
				    continue;
				}
				for (ATermList l = list; !l.isEmpty(); l = l.getNext()) {
					ATermAppl c = (ATermAppl) l.getFirst();
					if (!isClass(c, true)) {
						String previousDefinition = isIndividual(st) ? "n individual" : " property";
						report.addMessage(FULL, "Invalid Union Class", "owl:unionOf includes %1% which is a" + previousDefinition, c); 
					} // if
				} // for
				ATermAppl disjunction = ATermUtils.makeOr(list);
				report.addMessage(DL, "Union Class", "owl:unionOf construct is used %1%", disjunction);
				addSameClass(st, disjunction);
			} // else if
			else if (p.equals(OWL.complementOf)) {
				if (!isClass(st, true)) {
				    String previousDefinition = isIndividual(st) ? "n individual" : " property";
					report.addMessage(FULL, "Invalid Complement Class", "owl:complementOf is used with %1% which is a" + previousDefinition, st);
				}
				if (!isClass(ot, true)) {
				    String previousDefinition = isIndividual(ot) ? "n individual" : " property";
					report.addMessage(FULL, "Invalid Complement Class", "owl:complementOf is used with %1% which is a" + previousDefinition, ot);
				}
				ATermAppl complement = ATermUtils.makeNot(ot);
				report.addMessage(DL, "Complement Class", "owl:complementOf construct is used %1%", complement);
				addSameClass(st, complement);
			} // else if
			else if (p.equals(RDFS.subClassOf)) {
				if (!isClass(st, true)) {
				    String previousDefinition = isIndividual(st) ? "n individual" : " property";
					report.addMessage(FULL, "Invalid SubClass Axiom", "rdfs:subClassOf is used with %1% which is a" + previousDefinition, st);
				}
				if (!isClass(ot, true)) {
				    String previousDefinition = isIndividual(ot) ? "n individual" : " property";
					report.addMessage(FULL, "Invalid SubClass Axiom", "rdfs:subClassOf is used with %1% which is a" + previousDefinition, ot);
				}
				
				addSubClass(st, ot);
			} // else if
			else if (p.equals(OWL.equivalentClass)) {
				if (!isClass(st, true)) {
				    String previousDefinition = isIndividual(st) ? "n individual" : " property";
					report.addMessage(FULL, "Invalid EquivalentClass Axiom", "owl:equivalentClass is used with %1% which is a" + previousDefinition, st);
				}
				if (!isClass(ot, true)) {
				    String previousDefinition = isIndividual(ot) ? "n individual" : " property";
					report.addMessage(FULL, "Invalid EquivalentClass Axiom", "owl:equivalentClass is used with %1% which is a" + previousDefinition, ot);
				}
				
				addSameClass(st, ot);				
				addToGraph(mEquivalentClassGraph, stmt);
			} // else if
			else if (p.equals(OWL.disjointWith)) {
				if (!isClass(st, true)) {
				    String previousDefinition = isIndividual(st) ? "n individual" : " property";
					report.addMessage(FULL, "Invalid DisjointClasses Axiom", "owl:disjointWith is used with %1% which is a" + previousDefinition, st);
				}
				if (!isClass(ot, true)) {
				    String previousDefinition = isIndividual(ot) ? "n individual" : " property";
					report.addMessage(FULL, "Invalid DisjointClasses Axiom", "owl:disjointWith is used with %1% which is a" + previousDefinition, ot);
				}
				
				ATerm complement = ATermUtils.makeNot(ot);
				addSubClass(st, complement);
				report.addMessage(DL, "Disjoint Classes", "owl:disjointWith construct is used DisjointClasses(%1% %2%)", st, ot);
				addToGraph(mDisjointWithGraph, stmt);
			} // else if
			else if (p.equals(OWL.equivalentProperty)) {
				if (!isProperty(st, true)) {
				    String previousDefinition = isIndividual(st) ? "n individual" : " class";
					report.addMessage(FULL, "Invalid EquivalentProperty Axiom", "owl:equivalentProperty is used with %1% which is a" + previousDefinition, st);
				}
				if (!isProperty(ot, true)) {
				    String previousDefinition = isIndividual(ot) ? "n individual" : " class";
					report.addMessage(FULL, "Invalid EquivalentProperty Axiom", "owl:equivalentProperty is used with %1% which is a" + previousDefinition, ot);
				}
				
				if ((kb.isObjectProperty(st) && !kb.isObjectProperty(ot))
				|| (kb.isDatatypeProperty(st) && !kb.isDatatypeProperty(ot))) {
					report.addMessage(FULL, 
					    "Invalid EquivalentProperty Axiom", "owl:equivalentProperty is used with %1% (a" + getDefinition(st) + ") and %2% (a" + getDefinition(ot) +	")", st, ot);
				}
				else {
					kb.getABox().getRBox().addSubRole(st, ot);
					printDebug("sub-role(" + st + " " + ot + ")");
					kb.getABox().getRBox().addSubRole(ot, st);
					printDebug("sub-role(" + ot + " " + st + ")");
				}
			} // else if
			else if (p.equals(RDFS.subPropertyOf)) {
				if (!isProperty(st, true)) {
				    String previousDefinition = isIndividual(st) ? "n individual" : " class";
					report.addMessage(FULL, "Invalid SubProperty Axiom", "rdfs:subPropertyOf is used with %1% which is a" + previousDefinition, st);
				}
				if (!isProperty(ot, true)) {
				    String previousDefinition = isIndividual(ot) ? "n individual" : " class";
					report.addMessage(FULL, "Invalid SubProperty Axiom", "rdfs:subPropertyOf is used with %1% which is a" + previousDefinition, ot);
				}
				
				if ((kb.isObjectProperty(st) && !kb.isObjectProperty(ot)) ||
				    (kb.isDatatypeProperty(st) && !kb.isDatatypeProperty(ot))) {
					report.addMessage(FULL, 
					    "Invalid SubProperty Axiom", "rdfs:subPropertyOf is used with %1% (" + getDefinition(st) + ") and %2% (" + getDefinition(ot) +	")", st, ot);

				}
				else {
					kb.getABox().getRBox().addSubRole(st, ot);
					printDebug("sub-role(" + st + " " + ot + ")");
				}
			} // else if
			else if (p.equals(OWL.inverseOf)) {
				if (!isProperty(st, true)) {
				    String previousDefinition = isIndividual(st) ? "n individual" : " class";
					report.addMessage(FULL, "Invalid InverseProperty Axiom", "owl:inverseOf is used with %1% which is a" + previousDefinition, st);
				}
				if (!isProperty(ot, true)) {
				    String previousDefinition = isIndividual(ot) ? "n individual" : " class";
					report.addMessage(FULL, "Invalid InverseProperty Axiom", "owl:inverseOf is used with %1% which is a" + previousDefinition, ot);
				}
				
				kb.addInverseProperty(st, ot);
				printDebug("inverse-role(" + st + " " + ot + ")");
			} // else if
			else if (p.equals(OWL.sameAs)) {
				if (!isIndividual(st, true)) {
				    String previousDefinition = isClass(st) ? "class" : "property";
					report.addMessage(FULL, "Invalid SameAs Axiom", "owl:sameAs is used with %1% which is a" + previousDefinition, st);
				}
				if (!isIndividual(ot, true)) {
				    String previousDefinition = isClass(st) ? "class" : "property";
					report.addMessage(FULL, "Invalid SameAs Axiom", "owl:sameAs is used with %1% which is a" + previousDefinition, ot);
				}

				printDebug("same(" + st + " " + ot + ")");
			} // else if
			else if (p.equals(OWL.differentFrom)) {
				if (!isIndividual(st, true)) {
				    String previousDefinition = isClass(st) ? "class" : "property";
					report.addMessage(FULL, "Invalid DifferentFrom Axiom", "owl:differentFrom is used with %1% which is a" + previousDefinition, st);
				}
				if (!isIndividual(ot, true)) {
				    String previousDefinition = isClass(st) ? "class" : "property";
					report.addMessage(FULL, "Invalid DifferentFrom Axiom", "owl:differentFrom is used with %1% which is a" + previousDefinition, ot);
				}
				
				printDebug("different(" + st + " " + ot + ")");				
			} // else if
			else if (p.equals(RDFS.domain)) {
				if (s.getNameSpace().equals(RDF.getURI()) || s.getNameSpace().equals(OWL.getURI())) {
				    report.addMessage(FULL, "Invalid Domain Restriction", "rdfs:domain is used on built-in property %1%", st);
					continue;
				}
				
				if (!isProperty(st, true)) {
				    String previousDefinition = isIndividual(st) ? "n individual" : " class";
					report.addMessage(FULL, "Invalid Domain Restriction", "rdfs:domain statement has a subject %1% which is a" + previousDefinition, st);
				}

				if (!isClass(ot, true)) {
				    String previousDefinition = isIndividual(st) ? "n individual" : " property";
					report.addMessage(FULL, "Invalid Domain Restriction", "rdfs:domain statement has an object %1% which is a" + previousDefinition, st);
				}
				
				printDebug("domain(" + st + " " + ot + ")");				
			} // else if
			else if (p.equals(RDFS.range)) {
				if(s.isAnon()) {
				    report.addMessage(FULL, "Invalid Range Restriction", "rdfs:range is used on an anonymous property");
					continue;
				}
				if (s.getNameSpace().equals(RDF.getURI()) || s.getNameSpace().equals(OWL.getURI())) {
				    report.addMessage(FULL, "Invalid Range Restriction", "rdfs:range is used on built-in property %1%", st);
					continue;
				}

				// we have s rdfs:range o
				// there are couple of different possibilities
				// s is DP & o is undefined -> o is Datatype
				// s is OP & o is undefined -> o is class
				// s is undefined & o is Class -> s is OP
				// s is undefined & o is Datatype -> s is DP
				// s is undefined & o is undefined -> s is OP, o is class
				// any other case error!
				String sd = getDefinition(st);
				String od = getDefinition(ot);

				if (sd.equals("Untyped Resource")) {
					if (od.equals("Datatype"))
						isProperty(st, true, "datatype");
					else if (od.equals("Class"))
						isProperty(st, true, "object");
					else if (od.equals("Untyped Resource")) {
						isProperty(st, true, "object");
						isClass(ot, true);
					}
					else
					    report.addMessage(FULL, "Untyped Resource", "%1% is used in an rdfs:range restriction", st, ot);
				}
				else if (od.equals("Untyped Resource")) {
					if (sd.equals("ObjectProperty"))
						isClass(ot, true);
					else if (sd.equals("DatatypeProperty")) {
						Resource r = (Resource) o;
						kb.addDatatype(ot);
						ot = node2term(r);

						report.addMessage(FULL, "Unknown Datatype", r + " is an unknown datatype");
						addMissingTriple(r, RDF.type, RDFS.Datatype);
					}
					else
					    report.addMessage(FULL, "Untyped Property", "%1% has an rdfs:range restriction", st);
				}

				sd = getDefinition(st);
				od = getDefinition(ot);

				if ((sd.equals("DatatypeProperty") && od.equals("Datatype"))
				 || (sd.equals("ObjectProperty") && od.equals("Class"))) {
					kb.addRange(st, ot);
					printDebug("range(" + st + " " + ot + ")");
				}
				else {
					report.addMessage(FULL, "Invalid Range Restriction", "%1% (" + sd + ") has rdfs:range %2% (" + od + ")", st, ot);
				}
			} // else if
			else if (p.equals(OWL.distinctMembers)) {
				List result = new ArrayList();
				ATermList list = (ATermList) lists.get(o);
				if (list == null) {
				    report.addMessage(FULL, "Invalid List", "owl:distinctMembers must point to a list");
				    continue;
				}
				for (ATermList l = list; !l.isEmpty(); l = l.getNext()) {
					ATermAppl c = (ATermAppl) l.getFirst();
					if (!isIndividual(c, true)) {
					    String previousDefinition = isClass(st) ? "class" : "property";
						report.addMessage(FULL, "Invalid AllDifferent Axiom", "owl:AllDifferent includes %1% which is a" + previousDefinition, ot);
					} // if
					else
						result.add(c);
				} // for
				for (int k = 0; k < result.size(); k++) {
					for (int j = k + 1; j < result.size(); j++) {
						kb.getABox().addDifferent((ATermAppl) result.get(k), (ATermAppl) result.get(j));
						printDebug("different(" + result.get(k) + " " + result.get(j) + ")");
					} // for
				} // for
			} // else if
			else if (p.equals(OWL.oneOf)) {
				ATermList result = ATermUtils.EMPTY_LIST;
				ATermAppl disjunction = null;
				
				if (isDatatype(st)) {
					if (!o.equals(RDF.nil)) {
						ATermList list = (ATermList) lists.get(o);
						if (list == null) {
						    report.addMessage(FULL, "Invalid List", "owl:oneOf must point to a list");
						    continue;
						}
						for (ATermList l = list; !l.isEmpty(); l = l.getNext()) {
							ATermAppl c = (ATermAppl) l.getFirst();
							// added clause for literal because dataranges can use literals in a oneOf
							// and this fixed the error in species validation
							// later we'll have to actually add support/detection for this
							if (!isLiteral(c)) {
							    String definition = getDefinition(c);
								report.addMessage(FULL, "Invalid DataRange", "owl:oneOf list in a DataRange includes a " + definition + " %1%", c);
							} // if
							result = result.insert(ATermUtils.makeValue(c));
						} // for
					} // if		
					
					disjunction = ATermUtils.makeOr(result);
					report.addMessage(DL, "Enumerated DataRange", "DataRange is defined %1%", disjunction);
				}
				else {
					// assert the subject is a class
					isClass(st, true);
	
					if (!o.equals(RDF.nil)) {
						ATermList list = (ATermList) lists.get(o);
						if (list == null) {
						    report.addMessage(FULL, "Invalid List", "owl:oneOf must point to a list");
						    continue;
						}
						for (ATermList l = list; !l.isEmpty(); l = l.getNext()) {
							ATermAppl c = (ATermAppl) l.getFirst();
							// added clause for literal because dataranges can use literals in a oneOf
							// and this fixed the error in species validation
							// later we'll have to actually add support/detection for this
							if (!isIndividual(c, true)) {
							    String previousDefinition = isClass(c) ? "class" : "property";
								report.addMessage(FULL, "Invalid Enumerated Class", "owl:oneOf list includes %1% which is a" + previousDefinition, c);
							} // if
							result = result.insert(ATermUtils.makeValue(c));
						} // for
					} // if
					
					disjunction = ATermUtils.makeOr(result);
					report.addMessage(DL, "Enumerated Class", "owl:oneOf construct is used %1%", disjunction);
				}
				

				addSameClass(st, disjunction);
			} // else if
			else if (p.equals(OWL.imports)) {
				Resource r = (Resource) o;
				if (!isOntology(r)) {
					report.addMessage(FULL, "Untyped Ontology", s + " is importing " + o + " which is not specified as an owl:Ontology");
					addMissingTriple(r, RDF.type, OWL.Ontology);
				}

				if (!isOntology(s)) {
					report.addMessage(FULL, "Untyped Ontology", s + " has a value for owl:imports property but is not defined as an owl:Ontology");
					addMissingTriple(s, RDF.type, OWL.Ontology);
				}
			} // else if
			else if (p.equals(RDF.first)) {
				if (s.equals(RDF.nil)) {
					canOutputDL = false;
					report.addMessage(FULL, "Invalid List Structure", "rdf:nil cannot have a rdf:first element.");
				}
			} // else if
			else if (p.equals(RDF.rest)) {
				if (s.equals(RDF.nil)) {
					canOutputDL = false;
					report.addMessage(FULL, "Invalid List Structure", "rdf:nil cannot have a rdf:rest element.");
				}
			} // else if
			else if (kb.isOntologyProperty(pt)) {
				Resource r = (Resource) o;
				if (!isOntology(s)) {
					addOntology(s);
					report.addMessage(FULL, "Untyped Ontology", "An OntologyProperty " + p + " is used with a subject " + s + " that is not an Ontology");
					addMissingTriple(s, RDF.type, OWL.Ontology);
				}
				if (!isOntology(r)) {
					addOntology(r);
					report.addMessage(FULL, "Untyped Ontology", "An OntologyProperty " + p + " is used with an object " + o + " that is not an Ontology");
					addMissingTriple(r, RDF.type, OWL.Ontology);
				}

				Map props = getOntologyDefinition(s);
				List propList = (List) props.get(p);
				if (propList == null)
					propList = new ArrayList();
				propList.add(o);
				props.put(p, propList);
			} // else if
			else {
				if (kb.isAnnotationProperty(pt)) {
					continue;
				} // if
				else {
					String propType = "object";
					if (isLiteral(ot))
						propType = "datatype";					
					if (!isIndividual(st))
						propType = "annotation";


					if (!isProperty(pt, true, propType)) {
						String previousDefinition = isIndividual(st) ? "n individual" : " class";
						String problem = " is used in predicate position but defined as a" + previousDefinition;
						report.addMessage(FULL, "Multiple Types", "Resource %1% " + problem, pt); 
						continue;
					}
				} // else if

				if (kb.isAnnotationProperty(pt))
					continue;
					
				if (kb.isDatatypeProperty(pt)) {
					if (!isIndividual(st, true)) {
						String previousDefinition = isClass(st) ? "class" : "property";
						String problem = " is used as an individual but defined as a" + previousDefinition;
						report.addMessage(FULL, "Multiple Types", "Resource %1% " + problem, pt);
					}
					else if (!isLiteral(ot)) {
						report.addMessage(FULL, "Multiple Types", "Resource %1% is defined as a DatatypeProperty but used with values that are OWL individuals", pt);
					}
					else {
	                    com.hp.hpl.jena.rdf.model.Literal jenaLiteral = (com.hp.hpl.jena.rdf.model.Literal) o;
	                    String datatypeURI = jenaLiteral.getDatatypeURI();

						if (datatypeURI != null && !datatypeURI.equals("")) {
						    ATermAppl term = ATermUtils.makeTermAppl( datatypeURI );
							if (!kb.isDatatype(term)) {
								kb.addDatatype(term);

								report.addMessage(FULL, "Unknown Datatype", datatypeURI + " is an unknown datatype");
								addMissingTriple(ResourceFactory.createResource(datatypeURI), RDF.type, RDFS.Datatype);
							}
						}
						
	                    kb.addPropertyValue(pt, st, ot);
						printDebug("edge(" + pt + " " + st + " " + ot + ")");
					}
				} // if
				else {
					if (!isIndividual(st, true)) {
						String previousDefinition = isClass(st) ? "class" : "property";
						String problem = " is used as an individual but defined as a" + previousDefinition;
						report.addMessage(FULL, "Multiple Types", "Resource %1% " + problem, pt);
					}
					else if (isLiteral(ot))
						report.addMessage(FULL, "Multiple Types", "Resource %1% is defined as an ObjectProperty but used with a literal value", pt);
					else if (!isIndividual(ot, true)) {
						String previousDefinition = isClass(st) ? "class" : "property";
						String problem = " is used as an individual but defined as a" + previousDefinition;
						report.addMessage(FULL, "Multiple Types", "Resource %1% " + problem, pt);						
					} // else if
					else {
						kb.addPropertyValue(pt, st, ot);
						printDebug("edge(" + pt + " " + st + " " + ot + ")");
					}
				} // else
			} // else
		} // for

		// Equivalent class structure sharing check
		Enumeration enum_ = mEquivalentClassGraph.getVertexKeys();
		while (enum_.hasMoreElements()) {
			String key = enum_.nextElement().toString();

			if (mDisjointWithGraph.getVertex(key) != null && key.startsWith("anon")) {
				report.addMessage(FULL, "Structure Sharing", "owl:equivalentClass and owl:disjointWith descriptions share bNodes");
				canOutputDL = false;
			}
			else if (key.startsWith("anon") && mUsedBnodes.contains(key.substring(key.indexOf("anon") + 4))) {
				canOutputDL = false;
				report.addMessage(FULL, "Structure Sharing", 
					"Structure sharing not permitted for "
						+ key.substring(key.indexOf("anon") + 4)
						+ " via owl:equivalentClass.");
			}
		} // while

		doDisjointCheck();
	} // processTriples

	// add a node into the specified graph, connect with specified sibling (from Statement)
	private void addToGraph(Graph theGraph, Statement stmt) {
		Resource s = stmt.getSubject();
		Resource p = stmt.getPredicate();
		RDFNode o = stmt.getObject();

		Vertex start = null;
		Vertex end = null;
		if (theGraph.getVertex((s.isAnon() ? "anon" : "") + s.toString()) == null) {
			start = new Vertex((s.isAnon() ? "anon" : "") + s.toString());
			theGraph.addVertex(start);
		} // if
		else
			start = theGraph.getVertex((s.isAnon() ? "anon" : "") + s.toString());

		if (theGraph.getVertex((o instanceof Resource && ((Resource) o).isAnon() ? "anon" : "") + o.toString())
			== null) {
			end =
				new com.lre.graph.Vertex(
					(o instanceof Resource && ((Resource) o).isAnon() ? "anon" : "") + o.toString());
			theGraph.addVertex(end);
		} // if
		else
			end = theGraph.getVertex((o instanceof Resource && ((Resource) o).isAnon() ? "anon" : "") + o.toString());
		theGraph.connect(start, end, p.toString());
	} // addToGraph

	// if a graph is composed of several non-connected subgraph
	// this will break the main graph into it's constituent smaller sub-graphs
	private List splitGraph(Graph g, String edge) {
		List graphs = new ArrayList();
		Enumeration e = g.getVertexKeys();
		HashSet seen = new HashSet();
		while (e.hasMoreElements()) {
			String key = e.nextElement().toString();
			if (seen.contains(key))
				continue;
			Vertex aVert = g.getVertex(key);
			boolean inserted = false;

			for (int i = 0; i < graphs.size(); i++) {
				Graph gg = (Graph) graphs.get(i);
				Enumeration enum_ = gg.getVertexKeys();
				while (enum_.hasMoreElements()) {
					String s = enum_.nextElement().toString();

					if (aVert.hasNeighbor(gg.getVertex(s), edge)) {
						inserted = true;
						gg.addVertex(aVert);
						break;
					} // if
				} // while
				if (inserted)
					break;
			} // for
			if (!inserted) {
				List neighbors = collectNodes(aVert, true);
				Graph aGraph = new Graph();
				aGraph.addVertex(aVert);
				for (int i = 0; i < neighbors.size(); i++) {
					Vertex v = (Vertex) neighbors.get(i);
					seen.add(v.getName());
					aGraph.addVertex(v);
				} // for
				graphs.add(aGraph);
			} // if
			if (seen.size() == g.numVertices())
				break;
		} // while
		return graphs;
	} // splitGraph

	private void doDisjointCheck() {
		// divide main graph into sub graphs
		if (mDisjointWithGraph.numVertices() == 0)
			return;

		List allGraphs = splitGraph(mDisjointWithGraph, OWL.disjointWith.toString());
		for (int i = 0; i < allGraphs.size(); i++)
			if (!checkGraph((Graph) allGraphs.get(i)))
				return;
	} // doDisjointCheck

	// check to see if the disjointWith graph is legal in OWL DL
	private boolean checkGraph(Graph theGraph) {
		Enumeration enum_ = theGraph.getVertexKeys();
		boolean invalid = false;
		while (enum_.hasMoreElements()) {
			String key = enum_.nextElement().toString();
			Vertex vert = theGraph.getVertex(key);
			// TODO Evren: following bit gave wrong warnings and gas been commented
//			if (!vert.getName().startsWith("anon")) {
//				// named nodes need edges to all other named nodes in graph...i think
//				Enumeration e = theGraph.getVertexKeys();
//				while (e.hasMoreElements()) {
//					String s = e.nextElement().toString();
//					Vertex vt = theGraph.getVertex(s);
//					if (vt.equals(vert))
//						continue;
//					if (!vt.getName().startsWith("anon")
//						&& !(vert.hasNeighbor(vt, OWL.disjointWith.toString())
//							|| vt.hasNeighbor(vert, OWL.disjointWith.toString()))) {
//						report.addMessage(FULL, 
//							"Badly connected subgraph", "No connection exists between "
//								+ vt.getName()
//								+ " and "
//								+ vert.getName());
//						canOutputDL = false;
//						return false;
//					} // if
//				} // while
//				continue;
//			} // if

			// check the connectivity of the graph involving bnodes
			List allNodes = collectNodes(vert, false);
			allNodes.add(vert);
			for (int i = 0; i < allNodes.size(); i++) {
				Vertex firstVert = (Vertex) allNodes.get(i);
				for (int j = 0; j < allNodes.size(); j++) {
					Vertex secondVert = (Vertex) allNodes.get(j);
					if (firstVert.equals(secondVert))
						continue;
					if (!firstVert.hasNeighbor(secondVert, OWL.disjointWith.toString()))
						invalid = true;
				} // for
				if (invalid)
					break;
			} // for
			if (invalid)
				break;
		} // while

		if (invalid) {
			// not connected?
			// try seeing if the undirected graph is connected
			UndirectedGraph g = new UndirectedGraph();
			Enumeration anEnum = theGraph.getVertexKeys();
			while (anEnum.hasMoreElements()) {
				Vertex theVertex = theGraph.getVertex(anEnum.nextElement().toString());
				Vertex copy = null;
				if (g.getVertex(theVertex.getName()) != null)
					copy = g.getVertex(theVertex.getName());
				else {
					copy = new Vertex(theVertex.getName());
					g.addVertex(copy);
				} // else
				List v = theVertex.listNeighbors();
				for (int j = 0; j < v.size(); j++) {
					Vertex neighbor = (Vertex) v.get(j);
					Vertex target = null;
					if (g.getVertex(neighbor.getName()) != null)
						target = g.getVertex(neighbor.getName());
					else {
						target = new Vertex(neighbor.getName());
						g.addVertex(target);
					} // else
					g.connect(copy, target, OWL.disjointWith.toString());
				} // for
			} // for

			if (isConnected(g, OWL.disjointWith.toString()))
				invalid = false;
			else {
				report.addMessage(FULL, "Structure Sharing", 
					"owl:disjointWith edges in the graph form undirected complete subgraphs which share bNodes");
				canOutputDL = false;
			}

		} // if

		// check see if nodes used in disjointwith were already used to detect structure sharing
		Enumeration e = mDisjointWithGraph.getVertexKeys();
		while (e.hasMoreElements()) {
			String aKey = e.nextElement().toString();
			if (aKey.startsWith("anon")) {
				aKey = aKey.substring(aKey.indexOf("anon") + 4);
				if (mUsedBnodes.contains(aKey)) {
					report.addMessage(FULL, "Structure sharing violation", "DisjointWith case!");
					canOutputDL = false;

					return false;
				} // if
				else
					mUsedBnodes.add(aKey);
			} // if
		} // while

		return invalid;
	} // doDisjointCheck

	private boolean isConnected(Graph g, String theEdge) {
		Enumeration e = g.getVertexKeys();
		while (e.hasMoreElements()) {
			Vertex firstVert = g.getVertex(e.nextElement().toString());
			Enumeration enum_ = g.getVertexKeys();
			while (enum_.hasMoreElements()) {
				Vertex secondVert = g.getVertex(enum_.nextElement().toString());
				if (firstVert.equals(secondVert))
					continue;
				if (!firstVert.hasNeighbor(secondVert, theEdge))
					return false;
			} // while
		} // while
		return true;
	} // isConnected

	private List collectNodes(Vertex vert, boolean all) {
		mVisitedNodes = new HashSet();
		return collectNodesR(vert, all);
	} // collectNodes

	// recursive helper function
	private List collectNodesR(Vertex vert, boolean all) {
		if (mVisitedNodes.contains(vert.getName()))
			return new ArrayList();
		else
			mVisitedNodes.add(vert.getName());

		List nodes = new ArrayList();
		List neighbors = vert.listNeighbors();
		for (int i = 0; i < neighbors.size(); i++) {
			Vertex v = (Vertex) neighbors.get(i);
			if (!mVisitedNodes.contains(v.getName()) && (all || v.getName().startsWith("anon"))) {
				List moreNodes = collectNodesR(v, all);
				if (!moreNodes.contains(v))
					moreNodes.add(v);
				moreNodes.removeAll(nodes);
				nodes.addAll(moreNodes);
			} // if
			else
				return nodes;
		} // for
		return nodes;
	} // collectNodes

	private void addMissingTriple(Resource s, Property p, Resource o) {
		if (s.isAnon() || p.isAnon() || o.isAnon())
			canOutputDL = false;
		else
			missingTriples.add( s, p, o );
	}

	private void addMissingTriple(ATerm s, Property p, Resource o) {
		String st = s.toString();

		if (st.startsWith("_anon"))
			canOutputDL = false;
		else
			addMissingTriple(ResourceFactory.createResource(st), p, o);
	}

	boolean isDefinedClass(ATerm c) {
		return c.equals(ATermUtils.TOP)
			|| c.equals(ATermUtils.BOTTOM)
			|| classes.contains(c)
			|| isRestriction(c)
			|| isComplexClass(c);
	}

	boolean isDefinedIndividual(ATerm i) {
		return kb.getABox().isNode(i);
	}

	String getDefinition(ATerm x) {
	    if (isDatatype(x))
			return "Datatype";
		else if (isDefinedClass(x))
			return "Class";
		else if (isDefinedIndividual(x))
			return "Individual";
		else if(kb.isProperty(x)) {
		    Role r = kb.getProperty(x);
		    if (r.isDatatypeRole())		
				return "DatatypeProperty";
			else if (r.isObjectRole())
				return "ObjectProperty";
			else if (r.isAnnotationRole())
				return "AnnotationProperty";
			else if (r.isOntologyRole())
				return "OntologyProperty";
			else
			    return "Property";
		}
		else
			return "Untyped Resource";
	}

	boolean isDatatype(ATerm c) {
		return ATermUtils.isDataRange((ATermAppl) c) || kb.getDatatypeReasoner().isDefined(c.toString());
	}
	boolean isRestriction(ATerm c) {
		return restrictions.containsKey(c);
	}
	boolean isClass(ATermAppl c) {
		return isClass(c, false);
	}
	boolean isClass(ATermAppl c, boolean assertTrue) {
		if (c.equals(ATermUtils.TOP) || c.equals(ATermUtils.BOTTOM))
			return true;
		else if (isDatatype(c))
			return false;
		else if (classes.contains(c) || isRestriction(c))
			return true;
		else if (isComplexClass(c))
			return true;
		else if (kb.getABox().isNode(c) || kb.isProperty(c))
			return false;
        else if (isLiteral(c))
            report.addMessage(FULL, "Invalid Class", "Literal %1% is used as a class", c);
		else if (assertTrue) {
			report.addMessage(FULL, "Untyped Class", "Assuming %1% is a class", c);
			classes.add(c);
			addMissingTriple(c, RDF.type, OWL.Class);

		}
		return true;
	}
	
	boolean isComplexClass(ATerm c) {
		if (c instanceof ATermAppl) {
			ATermAppl a = (ATermAppl) c;
			AFun f = a.getAFun();
			
			if(ATermUtils.isDataRange(a))
			    return false;
			
			return f.equals(ATermUtils.ALLFUN)
				|| f.equals(ATermUtils.SOMEFUN)
				|| f.equals(ATermUtils.MAXFUN)
				|| f.equals(ATermUtils.MINFUN)
				|| f.equals(ATermUtils.ANDFUN)
				|| f.equals(ATermUtils.ORFUN)
				|| f.equals(ATermUtils.NOTFUN)
				|| f.equals(ATermUtils.VALUEFUN);
		}
		return false;
	}
	boolean isLiteral(ATerm l) {
		return (l instanceof ATermAppl) && ((ATermAppl) l).getAFun().equals(ATermUtils.LITFUN);
	}
	boolean isIndividual(ATermAppl i) {
		return isIndividual(i, false);
	}
	boolean isIndividual(ATermAppl i, boolean assertTrue) {
		if (kb.getABox().isNode(i))
			return true;
		else if (classes.contains(i) || kb.isProperty(i) || isLiteral(i))
			return false;
		else if (assertTrue) {
			report.addMessage(FULL, "Untyped Individual", "Assuming %1% is an individual", i);
			kb.addIndividual(i);
			addMissingTriple(i, RDF.type, OWL.Thing);

		}
		return true;
	}
	boolean isProperty(ATermAppl p) {
		return isProperty(p, false, null);
	}

	boolean isProperty(ATermAppl p, boolean assertTrue) {
		return isProperty(p, assertTrue, "object");
	}

	boolean isProperty(ATermAppl p, boolean assertTrue, String type) {
		if (kb.isProperty(p)) 
		    return true;
		else if (classes.contains(p) || kb.getABox().isNode(p))
			return false;
		else if (assertTrue) {
			if (type.equals("object")) {
				report.addMessage(FULL, "Untyped Property", "Assuming %1% is an object property", p);
				kb.addObjectProperty(p);
				addMissingTriple(p, RDF.type, OWL.ObjectProperty);

			}
			else if (type.equals("datatype")) {
				report.addMessage(FULL, "Untyped Property", "Assuming %1% is an datatype property", p);
				kb.addDatatypeProperty(p);
				addMissingTriple(p, RDF.type, OWL.DatatypeProperty);
			}
			else if (type.equals("annotation")) {
				report.addMessage(FULL, "Untyped Property", "Assuming %1% is an annotation property", p);
				kb.addAnnotationProperty(p);
				addMissingTriple(p, RDF.type, OWL.AnnotationProperty);
			}
			else
				throw new InternalReasonerException("Invalid parameter for isProperty function");
		}
		return true;
	}

	private Map getOntologyDefinition(Resource o) {
		if (!o.toString().endsWith("#"))
			o = ResourceFactory.createResource(o + "#");

		return (Map) ontologies.get(o);
	}

	private void addOntology(Resource o) {
		if (!o.toString().endsWith("#"))
			o = ResourceFactory.createResource(o + "#");

		ontologies.put(o, new HashMap());
	}

	boolean isOntology(Resource o) {
		if (!o.toString().endsWith("#"))
			o = ResourceFactory.createResource(o + "#");

		return ontologies.containsKey(o);
	}	

	private void printDebug(String s) {
		if (OWLSpecies.DEBUG) System.out.println(s);
	}
} 
