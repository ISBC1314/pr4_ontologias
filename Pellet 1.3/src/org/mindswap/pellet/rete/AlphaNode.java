package org.mindswap.pellet.rete;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlphaNode extends Node {
	
	protected Triple pattern;
	protected List betaNodes;
	protected List dependents;
	protected List dependsOn;
	
	public AlphaNode(Triple t) {
		super();
		this.pattern = t;
		this.ind = new HashMap();
		this.betaNodes = new ArrayList();
		this.vars = pattern.getVars();
		
		Collections.sort(vars); // Sort variables
		this.svars = new ArrayList();
		this.dependents = new ArrayList();
		this.dependsOn = new ArrayList();		       		       
	}
	
	
	public void clear() {
		ind.clear();
	}
	
	public Map getBindings(List row) {
		
		Map bindings = new HashMap();
		
		List key = Utils.append(this.svars, this.vars);
		key = Utils.removeDups(key);
		
		for (int i=0; i< key.size(); i++) {
			bindings.put(key.get(i), row.get(i));        	
		}
		return bindings;        
	}
	
	
	public boolean index(Map ind, Map bindings, List key, Fact factAdded) {
		
		if (key.size() > 0) {
			Term t = (Term) bindings.get(key.remove(0)); 
			if (!ind.containsKey(t)) {
				// fact doesnot exist
				if (key.size() >0) {									
					ind.put(t, new HashMap());					
					return index((Map)ind.get(t), bindings, key, factAdded);
				} else { 
					ind.put(t, new HashMap());
					return true;			
				}
			} else {
				if (key.size() > 0) { //maybe the node exists in the index					
					return index((Map)ind.get(t),bindings, key, factAdded);
				} else 
					return false;		
			}
		}
		return false;
	}
	
	//determine whether the fact matches the node's pattern
	public Map match(Fact fact) {
		Map bindings = new HashMap();
		List pList = this.pattern.getList();
		List fList = fact.getList();
		Term p=null, f=null;
		for (int i=0; i<3; i++) {
			p = (Term) pList.get(i);
			f = (Term) fList.get(i);
			
			if (!(p instanceof Variable)) { 
				if (!(p.equals(f)))
					return null;
			} else if (!bindings.containsKey(p)) {
				bindings.put(p, f);
			} else if (!((Term)bindings.get(p)).equals(f))
				return null;			
		}
		return bindings;
	}
	
	
	public boolean add(Fact fact) {
		Map bindings = match(fact);
		
		if (bindings != null) {
			List key = Utils.append(this.svars, this.vars);
			key = Utils.removeDups(key);
			
			return index(ind, bindings, key, fact);
		} else 
			return false;
	}
		
	public String toString() {
		return "AlphaNode(" + pattern.toString() + ")";
	}
}
