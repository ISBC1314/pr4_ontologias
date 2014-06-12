package org.mindswap.pellet.rete;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import java.util.Set;

import org.mindswap.pellet.utils.SetUtils;

public class Interpreter {
	
	public Compiler compiler, rete;
	Set totalFacts, joinedBetaNodes;
	public Set inferredFacts, fta;
	Set initialFacts;
	
	
	public Interpreter() {
		super();

		compiler = new Compiler();
		
		this.rete = compiler;
		this.totalFacts = new HashSet();
		this.joinedBetaNodes = new HashSet();
		this.inferredFacts = new HashSet();
		this.fta = new HashSet();
		this.initialFacts = new HashSet();
						
	}
	
	public boolean addFacts(Set facts, boolean initialSet) {
		boolean status = false;
		
		if (initialSet)
			initialFacts = facts;
		
		Iterator it = facts.iterator();	
		
		while (it.hasNext()) {
			Fact f = (Fact) it.next();
			totalFacts.add(f);
			List alphaMatches = rete.alphaIndex.match(f);
			for (int i=0; i < alphaMatches.size(); i++) {
				AlphaNode a = (AlphaNode) alphaMatches.get(i);
				if (a.add(f))
					status = true;
			}	
		}
		return status;
	}
	
	public void processBetaNode(BetaNode betaNode) {
		List inferences = betaNode.join();
		if (inferences.size() > 0) {
			joinedBetaNodes.add(betaNode);
			if (betaNode.rule != null) {
				for (int i = 0; i < betaNode.rule.rhs.size(); i++) {
					Set results = betaNode.matchingFacts( (Triple) betaNode.rule.rhs.get(i), inferences);
					fta.addAll(results);
				}				
				
			} else {
				for (int i=0; i< betaNode.children.size(); i++) {
					processBetaNode((BetaNode) betaNode.children.get(i));
				}
				
			}
		}
	}
	
	public void run() {
		Iterator it = rete.alphaNodeStore.nodes.iterator();
		while (it.hasNext()) {
			AlphaNode alphaNode = (AlphaNode) it.next();
			Iterator betaIt = alphaNode.betaNodes.iterator();
			while (betaIt.hasNext()) {
				BetaNode betaNode  = (BetaNode) betaIt.next();
				if (joinedBetaNodes.contains(betaNode))
					continue;
				
				fta = new HashSet(); // resetes the list of inferred facts from joins
				processBetaNode(betaNode);				
			}
			if (!fta.isEmpty()) {
				fta.removeAll(initialFacts);
				Set newInferredFacts = SetUtils.difference(fta, inferredFacts);					
				if (newInferredFacts.size()>0) {
					inferredFacts.addAll(newInferredFacts);
					if (addFacts(newInferredFacts, false)) {
						joinedBetaNodes.clear();
						run();
					}
				}
			}
			
		}
	}	

}
