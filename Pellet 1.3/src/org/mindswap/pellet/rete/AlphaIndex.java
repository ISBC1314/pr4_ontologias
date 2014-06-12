package org.mindswap.pellet.rete;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlphaIndex {

	protected Map ind;
	public AlphaIndex() {
		ind = new HashMap();		
	}
	
	public boolean index(Map ind, AlphaNode node, List vars) {
		if (vars.size() > 0) {
			Term t = (Term) vars.remove(0); // pop the first item off
			if (!ind.containsKey(t)) {
				// node doesn't exist in index
				if (vars.size() >0) {
					if (t instanceof Variable) {
						// index Variables as null
						t = null;					
					}
					if (!ind.containsKey(t)) {
						ind.put(t, new HashMap());
					}
					return index((Map)ind.get(t), node, vars);
				} else { // no vars left
					if (t instanceof Variable) {
						t = null;
					}
					if (!ind.containsKey(t)) {
						List x = new ArrayList();
						x.add(node);
						ind.put(t, x);
					} else {
						if (ind.get(t) instanceof Map && ((Map)ind.get(t)).isEmpty()) {
							List x = new ArrayList();
							x.add(node);
							ind.put(t, x);
						} else if (ind.get(t) instanceof List) {
							List x = (ArrayList) ind.get(t);
							if (!x.contains(node)) {
								x.add(node);
							} else 
								return false;  							
						}
					}
					return true;			
				}
			} else {
				if (vars.size() > 0) { //maybe the node exists in the index
					if (t instanceof Variable) 
						t = null;
					return index((Map)ind.get(t),node, vars);
				} else 
					return false;		
			}
		}
		return false;
	}
	
	// return a list of matching alpha nodes for a given pattern
	public List match(Triple pattern) {
		List nodesMatched = new ArrayList();
		List key = new ArrayList();
		key.add(pattern.getPred());
		key.add(pattern.getSubj());
		key.add(pattern.getObj());
		
		nodesMatched = matchHelper(key, ind, nodesMatched);
		return nodesMatched;
	}
	
    
    public List matchHelper(List pattern, Object ind, List nodesMatched) {
        if (pattern.isEmpty()) {
        	
        	nodesMatched.addAll(((List)ind));
        	return nodesMatched;
        }        
        Term p = (Term) pattern.remove(0);
        if (((Map)ind).containsKey(null)) { //variables are stored as null
        	matchHelper(pattern, ((Map) ind).get(null), nodesMatched);
        }
        if (((Map)ind).containsKey(p)) {
        	matchHelper(pattern, ((Map)ind).get(p), nodesMatched);
        }
        return nodesMatched;
    }
      
    public boolean add(AlphaNode anode) {
    	
    	List key = new ArrayList();
		key.add(anode.pattern.getPred());		
		key.add(anode.pattern.getSubj());
		key.add(anode.pattern.getObj());
		return index(ind, anode, key);
    }
                   
}
