package org.mindswap.pellet.rete;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


import org.mindswap.pellet.rete.Node;

public class BetaNode extends Node {
	
	public Node lnode;
	public Node rnode;
	
	/*public List lvars;
	 public List rvars;
	 public List svars;*/
	
	public List parents;
	public List children;
	
	public RuleNode rule = null;
	public List pattern;
	

	public BetaNode(Node lnode, Node rnode) {
		this.lnode = lnode;
		this.rnode = rnode;
				
		this.svars = new ArrayList();
		if (rnode instanceof AlphaNode) {
			for (int i=0; i< lnode.vars.size(); i++)
				if (rnode.vars.contains(lnode.vars.get(i)))
					svars.add(lnode.vars.get(i));			
		}
		Collections.sort(svars);
		svars = Utils.removeDups(svars);
		
		vars = Utils.append(svars, lnode.vars);
		vars = Utils.append(vars, rnode.vars);		
		vars = Utils.removeDups(vars);		
		
		parents = new ArrayList();
		children = new ArrayList();
								
		ind = new HashMap();
				
		
	}
	
	
	public HashMap getBindings(List row) {
		HashMap bindings = new HashMap();
		List key = getKey();				
		for (int i=0; i< key.size(); i++) {
			bindings.put(key.get(i), row.get(i));        	
		}
		return bindings;      
	}
	
	public List getKey() {
		List key = new ArrayList();
		
		key.addAll(lnode.svars);
		key.addAll(lnode.vars);
		key.addAll(rnode.vars);		
		key = Utils.removeDups(key);
		return key;
	}
	
	public boolean add(List row) {
		List key = new ArrayList();
		//check whether this is a deep copy
		List k  = getKey();
		for (int i = 0; i < getKey().size(); i++)
			key.add(k.get(i));
		
		Map bindings = getBindings(row);
		if (bindings != null) {
			return index(ind, row, bindings, key);
		}
		return false;
	}
	
	
	private boolean index(Map ind, List row, Map bindings, List key) {
		if (key.size() > 0) {
			Term t = (Term) bindings.get(key.remove(0)); 
			if (!ind.containsKey(t)) {
				// fact does not exist
				if (key.size() >0) {									
					ind.put(t, new HashMap());					
					return index((Map)ind.get(t), row, bindings, key);
				} else { 
					//TODO: pychinko adds tuple() here
					ind.put(t, new ArrayList());
					return true;			
				}
			} else {
				if (key.size() > 0) { //maybe the node exists in the index					
					return index((Map)ind.get(t), row, bindings, key);
				} else 
					return false;		
			}
		}
		return false;
	}
		
	
	public List join() {
		List key;
		if (lnode.equals(rnode)) {
			this.vars = lnode.vars;
			if (!lnode.ind.isEmpty()) {
				return Utils.keysToList(lnode.ind);
			}
			else 
				return null;
			
		} else {			
			key = this.svars;
			List joinResults = new ArrayList();
			if (key.isEmpty()) {
				List leftMemory =  Utils.keysToList(this.lnode.ind);
				List rightMemory = Utils.keysToList(this.rnode.ind);
				for (int i=0; i < leftMemory.size(); i++) 
					for (int k=0; k < rightMemory.size(); k++) {
						
						List row = Utils.append((List)leftMemory.get(i),(List)rightMemory.get(k));						
						joinResults.add(row);
						
						this.add(row);
					}
				
			} else { 				
				joinResults = joinHelper(lnode.ind, rnode.ind, key, new ArrayList(), new ArrayList());				
			}
			return joinResults;					
			
		}
		
	}
	
	//TODO: error here, list / set stuff
	public List joinHelper(Map leftMemory, Map rightMemory, List sharedVars, List matchedValues, List results) {
		
//		System.out.println("left:" + leftMemory);
//		System.out.println("right:" + rightMemory);
//		System.out.println("matched:" + matchedValues);
		if (sharedVars.size() > 0) {
			// HashMap binding = new HashMap();
			Object[] leftMemoryKeys = leftMemory.keySet().toArray();
			
			for (int i=0; i< leftMemoryKeys.length; i++) {
				// System.out.println("left memory key:" + leftMemoryKeys[i] + " " + leftMemoryKeys.length);	
				if (rightMemory.containsKey(leftMemoryKeys[i])) {	
					
					
					List tmp = Utils.append(matchedValues, leftMemoryKeys[i]);
					
					
					joinHelper((HashMap)leftMemory.get(leftMemoryKeys[i]), 
							(HashMap)rightMemory.get(leftMemoryKeys[i]), 
							sharedVars.subList(1,sharedVars.size()),
							tmp,
							results
					);
				}
			}	
		} else if (matchedValues.size() > 0) {
			
			if (!leftMemory.isEmpty()) {
				//list of lists								
				List lm =  Utils.keysToList(leftMemory);				
				for (int i=0; i < lm.size(); i++) {
					
					if (!rightMemory.isEmpty()) {
						List rm = Utils.keysToList(rightMemory);
						for (int k=0; k < rm.size(); k++) {
							List row =  Utils.append(matchedValues,(List)lm.get(i)) ;
							row = Utils.append(row,(List)rm.get(k));
							
							results.add(row);
							this.add(row);						
						}
					} else {					
						List row =  Utils.append(matchedValues,(List)lm.get(i)) ;
						results.add(row);
						this.add(row);						
					}
				}
			} else if (!rightMemory.isEmpty()) {
				List rm = Utils.keysToList(rightMemory);
				for (int k=0; k < rm.size(); k++) {
					List row =  Utils.append(matchedValues,(List)rm.get(k)) ;					
					
					results.add(row);
					this.add(row);
				}
			}
			
		}
		return results;
	}
	
	public Set matchingFacts(Triple rhs, List facts) {
		Set results = new HashSet();
		Iterator it =  facts.iterator();
		while (it.hasNext()) {
			//is this a fact or a list?
			List f = (ArrayList) it.next();
			
			Fact newFact = new Fact();
			if (rhs.getSubj() instanceof Variable) {
				newFact.subj = getVar((Variable)rhs.getSubj(), f); 
			} else {
				newFact.subj = rhs.getSubj();
			}
			if (rhs.getPred() instanceof Variable) {
				System.err.println("Variables not allowed in predicates!");
			} else {
				newFact.pred = rhs.getPred();
			}
			
			if (rhs.getObj() instanceof Variable) {
				newFact.obj = getVar((Variable)rhs.getObj(), f); 
			} else {
				newFact.obj = rhs.getObj();
			}
			
			results.add(newFact);			
		}
		return results;
	}

	private Term getVar(Variable var, List fact) {
		if (vars.contains(var))  {
			int index = vars.indexOf(var);
			return (Term) fact.get(index);
		}
		else {
			System.err.println("Unbound rule variable:" + var + this.vars);
			return null;
		}
	}
	
	public String toString() {

		return "BetaNode vars: " + vars.toString() + " size: " + Utils.keysToList(ind).size(); 	
		
	}
}
