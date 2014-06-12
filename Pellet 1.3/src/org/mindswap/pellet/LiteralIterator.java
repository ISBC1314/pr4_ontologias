/*
 * Created on Aug 29, 2004
 */
package org.mindswap.pellet;

import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * An iterator to return nodes in the order they are added. Having a seperate 
 * iterator instead of using nodes.iterator() allows to change the nodes
 * table without resetting the iteration process. 
 * 
 * @author Evren Sirin
 */
class LiteralIterator implements Iterator {
    /**
     * Map of node names to node objects
     */
	protected Map nodes;
	/**
	 * List of node names
	 */
	protected List nodeList;
	/**
	 * Last returned index
	 */
	protected int index;
	/**
	 * Index where iterator starts (0 be default)
	 */
	protected int start;
	/**
	 * Index where iterator stops (size of list by default)
	 */
	protected int stop;

	/**
	 * Create an iterator over all the individuals in the ABox
	 */
	public LiteralIterator(ABox abox) {
		this(abox, true);
	}		

	/**
	 * Create an iterator over all the individuals in the ABox 
	 * but do not automatically find the first individual if
	 * findNext parameter is false 
	 * 
	 * @param abox
	 * @param findNext
	 */
	protected LiteralIterator(ABox abox, boolean findNext) {
		nodes = abox.getNodeMap();
		nodeList = abox.getNodeNames();
		start = 0;
		stop = nodeList.size();
		index = start;
		
		if(findNext)
		    findNext();
	}		

	/**
	 * Create a limited iterator over the individuals in the ABox
	 * that only covers the individuals whose index in nodeList
	 * is between start ans stop indices.
	 * 
	 * @param abox
	 * @param start
	 * @param stop
	 */
	public LiteralIterator(ABox abox, int start, int stop) {
		this.nodes = abox.getNodeMap();
		this.nodeList = abox.getNodeNames();
		this.start = start;
		this.stop = Math.max( stop, nodeList.size() );
		index = start;

		findNext();
	}
    
    public int getIndex() {
        return index;
    }
	
	protected void findNext() {
		for(; index < stop; index++) {
		    Node node = (Node) nodes.get( nodeList.get( index ) ) ;
			if( !node.isPruned() && node instanceof Literal )
				break;
		}
	}
	
	public boolean hasNext() {
		findNext();
		return index < stop;
	}
	
	public void reset() {
		index = start;
		findNext();
	}

	public void jump(int i) {
		index = i;
	}
	
	public Object next() {
		findNext();
		Literal lit = (Literal) nodes.get(nodeList.get(index++));
		
		return lit;
	}

	public void remove() {
		throw new RuntimeException("Remove is not supported");
	}
	
}