package org.mindswap.pellet.rete;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


import org.mindswap.pellet.ABox;
import org.mindswap.pellet.Edge;
import org.mindswap.pellet.EdgeList;
import org.mindswap.pellet.Individual;
import org.mindswap.pellet.utils.ATermUtils;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLException;
import org.semanticweb.owl.model.OWLObjectProperty;
import org.semanticweb.owl.rules.OWLRule;
import org.semanticweb.owl.rules.OWLRuleAtom;
import org.semanticweb.owl.rules.OWLRuleClassAtom;
import org.semanticweb.owl.rules.OWLRuleIObject;
import org.semanticweb.owl.rules.OWLRuleIndividual;
import org.semanticweb.owl.rules.OWLRuleObjectPropertyAtom;
import org.semanticweb.owl.rules.OWLRuleVariable;

import aterm.ATermAppl;


public class Compiler {
	
	AlphaStore alphaNodeStore;
	BetaStore betaNodeStore;
	AlphaIndex alphaIndex;
	// do we need a  betaindex?
	public Compiler() {
		alphaNodeStore = new AlphaStore();
		betaNodeStore = new BetaStore();
		alphaIndex = new AlphaIndex();
		
	}
	
	public Compiler(Set facts) {
		
	}
	
	public Compiler compile(List rules) {
		Iterator it = rules.iterator();
		while (it.hasNext()) {
			Rule rule = (Rule) it.next();
			AlphaStore alphaNodesOfRule = new AlphaStore();
			Iterator patternIt = rule.lhs.iterator();
			while (patternIt.hasNext()) {
				Triple anodePattern = (Triple) patternIt.next();
				AlphaNode anode = makeAlphaNode(anodePattern);
				alphaNodesOfRule.addNode(anode);				
			}
			
			alphaNodesOfRule.sort();
			alphaNodeStore.sort();
			int l = alphaNodesOfRule.nodes.size();
			if (l==0) {
				System.err.println("Malformed Input");
			} else if (l==1) {
				BetaNode beta1 = makeBetaNode((Node)alphaNodesOfRule.nodes.get(0), (Node)alphaNodesOfRule.nodes.get(0), false);
				AlphaNode a = (AlphaNode) alphaNodesOfRule.nodes.get(0);
				a.betaNodes = new ArrayList();
				a.betaNodes.add(beta1);
				beta1.rule = new RuleNode(rule);
				beta1.rule.betaNode = beta1;
				
				
			} else  if (l==2){
				BetaNode beta1 = makeBetaNode((Node)alphaNodesOfRule.nodes.get(0), (Node)alphaNodesOfRule.nodes.get(1), false);
				AlphaNode a = (AlphaNode) alphaNodesOfRule.nodes.get(0);
				a.betaNodes = new ArrayList();
				a.betaNodes.add(beta1);
				
				AlphaNode b = (AlphaNode) alphaNodesOfRule.nodes.get(1);
				b.betaNodes = new ArrayList();
				b.betaNodes.add(beta1);
				
				beta1.rule = new RuleNode(rule);
				beta1.rule.betaNode = beta1;
				betaNodeStore.addNode(beta1);				
			} else {
				
				BetaNode beta1 = makeBetaNode((Node)alphaNodesOfRule.nodes.get(0), (Node)alphaNodesOfRule.nodes.get(1), true);
//				System.out.println("adding betanode:");
//				System.out.println(beta1);
				AlphaNode a = (AlphaNode) alphaNodesOfRule.nodes.get(0);
				a.betaNodes = new ArrayList();
				a.betaNodes.add(beta1);
				
				AlphaNode b = (AlphaNode) alphaNodesOfRule.nodes.get(1);
				b.betaNodes = new ArrayList();
				b.betaNodes.add(beta1);
				
				betaNodeStore.addNode(beta1);
				makeBetaNetwork(rule, beta1, alphaNodesOfRule.nodes.subList(2, alphaNodesOfRule.nodes.size()));
				
			}
		}
		return this;
	}
	
	
	public void makeBetaNetwork(Rule rule, BetaNode betaNode, List alphaNodeList) {
		if (alphaNodeList.size()==0) {
			betaNode.rule = new RuleNode(rule);
			betaNode.rule.betaNode = betaNode;
			
		} else {
			AlphaNode alpha = (AlphaNode) alphaNodeList.get(0);
			BetaNode betaChild = makeBetaNode(betaNode, alpha, true);
			

			
			betaChild.parents = new ArrayList();
			betaChild.parents.add(betaNode);
			
			betaNode.children = new ArrayList();
			betaNode.children.add(betaChild);
			ArrayList sharedJoinVars = (ArrayList) Utils.getSharedVars(betaNode, alpha);
			Collections.sort(sharedJoinVars);
			betaNode.svars = sharedJoinVars;
			
			List tmp = Utils.append(sharedJoinVars, betaNode.vars);
			
			
			betaNode.vars = Utils.removeDups(tmp);
			alpha.betaNodes = new ArrayList();
			alpha.betaNodes.add(betaChild);
			
			betaNodeStore.addNode(betaNode);
			betaNodeStore.addNode(betaChild);
			makeBetaNetwork(rule, betaChild, alphaNodeList.subList(1, alphaNodeList.size()));
			
		}
	}		
	
	public AlphaNode makeAlphaNode(Triple pattern) {
		AlphaNode a = new AlphaNode(pattern);
		alphaIndex.add(a);
		alphaNodeStore.addNode(a);
		return a;	        
	}
	
	public BetaNode makeBetaNode(Node node1, Node node2, boolean futureJoins) {
		List sharedVars = Utils.getSharedVars(node1, node2);
		Collections.sort(sharedVars);
		if (node1 instanceof AlphaNode) {
			node1.svars = sharedVars;
			
		}
		node2.svars = sharedVars;
		
		BetaNode b = new BetaNode(node1, node2);
		b.svars = sharedVars;
		return b;
		
	}
	
	public Triple getTripleFromPropertyAtom(OWLRuleObjectPropertyAtom atom) throws OWLException {
		Term subj = null, pred = null, obj = null;
		
		//subject
		if (atom.getFirstArgument() instanceof OWLRuleVariable) {
			OWLRuleVariable v = ((OWLRuleVariable)atom.getFirstArgument());			
			subj = new Variable(v.getURI().toString());
			
		} else if (atom.getFirstArgument() instanceof OWLRuleIndividual) {
			OWLRuleIndividual ind = ((OWLRuleIndividual)atom.getFirstArgument());			
			subj = new Constant(ind.getIndividual().getURI().toString());
		}
		
		//predicate
		OWLObjectProperty prop = atom.getProperty();
		pred = new Constant(prop.getURI().toString());
		
		// object		
		if (atom.getSecondArgument() instanceof OWLRuleVariable) {
			OWLRuleVariable v = ((OWLRuleVariable)atom.getSecondArgument());			
			obj = new Variable(v.getURI().toString());
			
		} else if (atom.getSecondArgument() instanceof OWLRuleIndividual) {
			OWLRuleIndividual ind = ((OWLRuleIndividual)atom.getSecondArgument());			
			obj = new Constant(ind.getIndividual().getURI().toString());
		}
		
		return new Triple(subj, pred, obj);
	}
	
	
	public Triple getTripleFromClassAtom(OWLRuleClassAtom atom) throws OWLException {
		Term subj = null, obj = null;
		
		Constant predType = new Constant("type");
		
		OWLClass clazz =  (OWLClass) atom.getDescription();    			 		
		obj = new Constant(clazz.getURI().toString());
		
		OWLRuleIObject v = atom.getArgument();   
		if (v instanceof OWLRuleVariable) {			
			subj = new Variable(((OWLRuleVariable) v).getURI().toString());    				
			
		} else if (v instanceof OWLRuleIndividual) {			
			subj = new Constant(((OWLRuleIndividual)v).getIndividual().getURI().toString());
		}
		return new Triple(subj, predType, obj);
	}
	
	
	public List convertSWRLRules(Set rules) throws OWLException {
		List result = new ArrayList();		
		OWLRule rule;
		
		Iterator rulesIterator = rules.iterator();
		while (rulesIterator.hasNext()) {    		
			rule = (OWLRule) rulesIterator.next();
			Rule reteRule = new Rule();    		    		
			
			OWLRuleAtom head = (OWLRuleAtom) rule.getConsequents().iterator().next();
			if (head instanceof OWLRuleClassAtom) {
				
				Triple t = getTripleFromClassAtom((OWLRuleClassAtom)head);
				reteRule.rhs.add(t);    			 
			} else if (head instanceof OWLRuleObjectPropertyAtom) {
				
				Triple t = getTripleFromPropertyAtom((OWLRuleObjectPropertyAtom) head);
				reteRule.rhs.add(t);   						    		
			}		 
			
			Iterator ants = rule.getAntecedents().iterator();
			while (ants.hasNext()) {
				
				OWLRuleAtom atom = (OWLRuleAtom) ants.next();
				if (atom instanceof OWLRuleClassAtom) {
					
					Triple t = getTripleFromClassAtom((OWLRuleClassAtom)atom);
					reteRule.lhs.add(t);    			 
				} else if (atom instanceof OWLRuleObjectPropertyAtom) {
					
					Triple t = getTripleFromPropertyAtom((OWLRuleObjectPropertyAtom) atom);
					reteRule.lhs.add(t);   						    		
				}
			}
			
			result.add(reteRule);
		}
		
		return result;
	}
	
	public Set compileFacts(ABox abox) {
		Set result = new HashSet();
		//compile facts
		Iterator i = abox.getIndIterator();
		while (i.hasNext()) {
			Individual ind = (Individual) i.next();			
			if (!ind.isNamedIndividual())
				continue;
			List atomic = ind.getTypes( org.mindswap.pellet.Node.ATOM );
			for(Iterator it = atomic.iterator(); it.hasNext();) {
				ATermAppl c = (ATermAppl) it.next();
				
				if( ind.getDepends( c ).isIndependent() ) {
					if( ATermUtils.isPrimitive( c ) ) {
						result.add(createFact(ind, c));
					}					
				}
			}
			
			//get the out edges
			EdgeList edges = ind.getOutEdges();
			for (Iterator it = edges.iterator(); it.hasNext();) {
				Edge edge = (Edge) it.next();
				
				Individual from = edge.getFrom();
				// 
				
				if (edge.getTo() instanceof Individual) {
					
					Individual to= (Individual) edge.getTo();
					
					if (!from.isNamedIndividual() || !to.isNamedIndividual() )
						continue;
					
					result.add(createFact(from, to, edge));
				}
			}

		}
		return result;
	}
	
	private Fact createFact(Individual from, Individual to, Edge edge) {
		Constant subj = null, pred = null, obj = null;
		
		subj = new Constant(from.getNameStr());		
		pred = new Constant(edge.getRole().getName().toString()); 
		obj = new Constant(to.getNameStr());
		
		return new Fact(subj, pred, obj);
	}

	private Fact createFact(Individual ind, ATermAppl c) {
		Constant subj = null, obj = null;
		// System.out.println("str:" + ind);
		subj = new Constant(ind.getNameStr());		
		Constant predType = new Constant("type"); 
		obj = new Constant(c.getName());
		
		return new Fact(subj, predType, obj);
	}
	
	
}
