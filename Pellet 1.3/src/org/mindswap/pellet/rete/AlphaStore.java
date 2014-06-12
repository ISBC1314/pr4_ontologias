package org.mindswap.pellet.rete;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class AlphaStore {

	List nodes;
	Map sharedIndex;
	
	public AlphaStore() {
		nodes = new ArrayList();
		sharedIndex = new HashMap();
	}
		

	public void addNode(Node node) {
		if (!nodes.contains(node)) {
			nodes.add(node);
			for (int i=0; i< node.vars.size(); i++)
				if (!sharedIndex.containsKey(node.vars.get(i))) {
					List l = new ArrayList();
					l.add(node);
					sharedIndex.put(node.vars.get(i), l);
				}
				else {					
					List l = (List) sharedIndex.get(node.vars.get(i)); 
					l.add(node);
					sharedIndex.put(node.vars.get(i), l);
				}
					
		}
	}

	public void sort() {
		       
       List sortedNonBuiltins = new ArrayList();
       for (int nb=0; nb < nodes.size(); nb++) 
    	   for (int v=0; v < ((Node)nodes.get(nb)).vars.size(); v++) {
    		   List nodesThatShare = (List) sharedIndex.get( ((Node)nodes.get(nb)).vars.get(v));
    		   if (nodesThatShare.size() > 0) {    			   
    			   sortedNonBuiltins.addAll((nodesThatShare));
    			   sortedNonBuiltins.add(((Node)nodes.get(nb)));
    		   }
    	   }
      
        sortedNonBuiltins = Utils.removeDups(sortedNonBuiltins);
               
        //System.out.println(sortedNonBuiltins);
    	// List tmp = Utils.removeDups(sortedNonBuiltins));
    	this.nodes.addAll(0, sortedNonBuiltins);
    	this.nodes = Utils.removeDups(this.nodes);
    	// System.out.println(this);
	}
	
	public String toString() {
		String tmp  = "";
		Iterator it = this.nodes.iterator();
		while (it.hasNext()) {
			tmp += ((Node) it.next()).toString() + "\n";
		}
		return tmp;
	}
}
