package org.mindswap.pellet.rete;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Utils {
	
	public static List getSharedVars(Node node1, Node node2) {
		List result = new ArrayList();		
		
		for (int i=0; i < node1.vars.size(); i++)
			if (node2.vars.contains(node1.vars.get(i))) 
				result.add(node1.vars.get(i));
				
		return Utils.removeDups(result);		
	}

	public static List removeDups (List l) {		
		 List noDups = new ArrayList();
		 for (int i=0; i< l.size(); i++)
			 if (!noDups.contains(l.get(i)))
				 noDups.add(l.get(i));		  
		  return noDups;
	}
	/* following are non-destructive list operations */ 
	public static List append(List l, Object obj) {
		List tmp = new ArrayList();
		tmp.addAll(l);
		tmp.add(obj);
		return tmp;
	}
	
	public static List append(List l, List m) {
		List tmp = new ArrayList();
		tmp.addAll(l);
		tmp.addAll(m);
		return tmp;
	}
	
	public static List keysToList(Map ind) {		
		  
	 List result = new ArrayList();
	  
	 if (ind.isEmpty())
	    return result;
	 
	 keysToListHelper(ind, new ArrayList(), result);
	 
	 return result;
	}

	
	private static void keysToListHelper(Object ind, List prefix, List result) {
		if (ind instanceof ArrayList) {
						
			List r = new ArrayList();
			r.addAll(prefix);
			result.add(r);
			return;
		}
		if (ind instanceof Map) {
			HashMap dict = (HashMap) ind;
			if (dict.isEmpty()) {
				List r = new ArrayList();
				r.addAll(prefix);
				result.add(r);
				return;
			} else {
				Iterator itKeys = dict.keySet().iterator();
				while (itKeys.hasNext()) {
					Object t = itKeys.next();
					List r = Utils.append(prefix, t); 
					
					// prefix.add(t);
					keysToListHelper(dict.get(t), r, result);
				}
			}
		}				
	}


}
