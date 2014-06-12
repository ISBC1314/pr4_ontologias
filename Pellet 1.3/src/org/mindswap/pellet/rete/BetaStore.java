package org.mindswap.pellet.rete;

import java.util.ArrayList;
import java.util.List;

public class BetaStore {
	
	
	List nodes;
	public BetaStore() {
		nodes = new ArrayList();
	}
	
	
	public void addNode(Node node) {
		if (!nodes.contains(node))
			nodes.add(node);
	}
	
}
