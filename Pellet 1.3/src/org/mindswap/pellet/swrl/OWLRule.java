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

package org.mindswap.pellet.swrl;

import java.util.HashSet;
import java.util.Vector;

import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.utils.ATermUtils;

import aterm.ATerm;
import aterm.ATermAppl;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

public class OWLRule {
	// list of variable URIs
	HashSet variables  = new HashSet();
	// antecedent of rule stored as an abox  
	KnowledgeBase ruleBody = null;
	// list of Statement's representing the atoms in the consequent
	// they are stored as RDF triples
	Vector ruleHead = new Vector();
	
	Model model = null;
	
	OWLRule(Resource rule) {
		model = rule.getModel();
		
		parseAntecedent(rule.getProperty(OWLRuleVocabulary.antecedent).getResource());
		parseConsequent(rule.getProperty(OWLRuleVocabulary.consequent).getResource());				
	}
	
	public String toString() {
		return ruleHead + " :- " + ruleBody;
	}
	
	ATermAppl node2term(Resource r) {
		if(isVariable(r))		
			return ATermUtils.makeTermAppl(PelletOptions.BNODE+ r.getLocalName());
		else	
			return ATermUtils.makeTermAppl(r.getURI());
	}
	
	boolean isVariable(Resource x) {
		return variables.contains(x);
	}

	ATermAppl defineNode(Resource x) {
		ATermAppl a = null;
		if(x.hasProperty(RDF.type, OWLRuleVocabulary.Variable) && !isVariable(x)) {
			variables.add(x);
			
			a = node2term(x);
			ruleBody.addIndividual(a);
		}
		else { 
			a = node2term(x);
		
			if(!ruleBody.isIndividual(a))
				ruleBody.addIndividual(a);
		}
		
		return a;	
	}

	boolean unsafeVariable(Resource x) {
		return x.hasProperty(RDF.type, OWLRuleVocabulary.Variable) && !isVariable(x);
	}

	void parseAntecedent(Resource antecedent) {
		ruleBody = new KnowledgeBase();
		
		Resource atom = antecedent.getProperty(RDF.first).getResource();
		while(!antecedent.equals(RDF.nil)) {
			atom = antecedent.getProperty(RDF.first).getResource();
			
			Resource atomType = atom.getProperty(RDF.type).getResource();
			if(atomType.equals(OWLRuleVocabulary.classAtom)) {
				Resource c = atom.getProperty(OWLRuleVocabulary.classPredicate).getResource();
				Resource x = atom.getProperty(OWLRuleVocabulary.argument1).getResource();
				
				ATermAppl a = defineNode(x);
				ruleBody.addType(a, node2term(c));
			}
			else if(atomType.equals(OWLRuleVocabulary.individualPropertyAtom)) {
				Resource p  = atom.getProperty(OWLRuleVocabulary.propertyPredicate).getResource();
				Resource r1 = atom.getProperty(OWLRuleVocabulary.argument1).getResource();
				Resource r2 = atom.getProperty(OWLRuleVocabulary.argument2).getResource();
				
				ATermAppl r  = node2term(p);
				ATermAppl x1 = defineNode(r1);
				ATermAppl x2 = defineNode(r2);
				ruleBody.getRBox().addObjectRole(r);
				ruleBody.addPropertyValue(r, x1, x2);				
			}
			else if(atomType.equals(OWLRuleVocabulary.datavaluedPropertyAtom)) {
				throw new RuntimeException("datavaluedPropertyAtom in antecedent is not supported yet!");
			}
			else if(atomType.equals(OWLRuleVocabulary.sameIndividualAtom)) {
				throw new RuntimeException("sameIndividualAtom in antecedent is not supported yet!");				
			}
			else if(atomType.equals(OWLRuleVocabulary.differentIndividualAtom)) {
				throw new RuntimeException("differentIndividualAtom in antecedent is not supported yet!");				
			}
			
			antecedent = antecedent.getProperty(RDF.rest).getResource();
		}
	}

	void parseConsequent(Resource consequent) throws UnsafeVariableException {
		ruleHead = new Vector();
		
		Resource atom = consequent.getProperty(RDF.first).getResource();
		while(!consequent.equals(RDF.nil)) {
			atom = consequent.getProperty(RDF.first).getResource();
			
			Resource atomType = atom.getProperty(RDF.type).getResource();
			if(atomType.equals(OWLRuleVocabulary.classAtom)) {
				Resource c = atom.getProperty(OWLRuleVocabulary.classPredicate).getResource();
				Resource x = atom.getProperty(OWLRuleVocabulary.argument1).getResource();
				
				if(unsafeVariable(x)) throw new UnsafeVariableException(x + " is unsafe");
				
				//ruleHead.add(ResourceFactory.createStatement(x, RDF.type, c));
				ruleHead.add(ATermUtils.makeTermAppl(ATermUtils.TYPEFUN, new ATerm[] {node2term(x), node2term(c)}));		
			}
			else if(atomType.equals(OWLRuleVocabulary.individualPropertyAtom)) {
				Resource p1 = atom.getProperty(OWLRuleVocabulary.propertyPredicate).getResource();
				Resource r1 = atom.getProperty(OWLRuleVocabulary.argument1).getResource();
				Resource r2 = atom.getProperty(OWLRuleVocabulary.argument2).getResource();
				
				if(unsafeVariable(r1)) throw new UnsafeVariableException(r1 + " is unsafe");
				if(unsafeVariable(r2)) throw new UnsafeVariableException(r2 + " is unsafe");
				
				//ruleHead.add(ResourceFactory.createStatement(r1, p, r2));
				ruleHead.add(ATermUtils.makeTermAppl(ATermUtils.IPFUN, new ATerm[] {node2term(r1), node2term(p1), node2term(r2)}));					
			}
			else if(atomType.equals(OWLRuleVocabulary.datavaluedPropertyAtom)) {
				throw new RuntimeException("datavaluedPropertyAtom in consequent is not supported yet!");
			}
			else if(atomType.equals(OWLRuleVocabulary.sameIndividualAtom)) {
				throw new RuntimeException("sameIndividualAtom in consequent is not supported yet!");				
			}
			else if(atomType.equals(OWLRuleVocabulary.differentIndividualAtom)) {
				throw new RuntimeException("differentIndividualAtom in consequent is not supported yet!");				
			}
			
			consequent = consequent.getProperty(RDF.rest).getResource();
		}	
	}
	
	public class UnsafeVariableException extends RuntimeException {
		public UnsafeVariableException() {super();
		}
		public UnsafeVariableException(String e) {super(e);
		}
	}	
} 

