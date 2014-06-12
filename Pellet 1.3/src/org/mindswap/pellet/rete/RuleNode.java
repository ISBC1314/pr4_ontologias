package org.mindswap.pellet.rete;

import java.util.List;


public class RuleNode extends Node {

	public BetaNode betaNode;
	public List rhs, lhs;
	public RuleNode(Rule rule) {
		super();
		rhs = rule.rhs;
		lhs = rule.lhs;
		
	}

	
	
	/* #####Make it a method of RuleNode
	    def matchingFacts(self, rhs, facts):
	        """I generate a set of facts and justifications, of the form:
	        
	        [[fact1, fact, fact2], [just1, just2, just2]]
	        
	        according to the given rhs from 'facts'"""
	        results = list()
	        for fact, just in facts:
	            newFact = list()
	            for p in rhs:
	                if isinstance(p, Variable):
	                    newFact.append(self.getvar(p, fact))
	                else:
	                    newFact.append(p)
	#            print "The fact: ", Fact(*newFact), " is justified by: ", just
	            results.append(Fact(*newFact))
	        return results*/
}
