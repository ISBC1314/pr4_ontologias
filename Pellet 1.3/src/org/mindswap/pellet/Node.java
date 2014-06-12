//The MIT License
//
//Copyright (c) 2003 Ron Alford, Mike Grove, Bijan Parsia, Evren Sirin
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to
//deal in the Software without restriction, including without limitation the
//rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
//sell copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in
//all copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
//FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
//IN THE SOFTWARE.

package org.mindswap.pellet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.mindswap.pellet.exceptions.InternalReasonerException;
import org.mindswap.pellet.utils.ATermUtils;

import aterm.ATerm;
import aterm.ATermAppl;
import aterm.ATermList;

/*
 * Created on Aug 27, 2003
 *
 */

/**
 * @author Evren Sirin
 *
 */
public abstract class Node {
	public static boolean DEBUG = false;
	
	public static int BLOCKABLE = Integer.MAX_VALUE;
	public static int NOMINAL   = 0;
	
	public static int CHANGED   = 0x7F;
	public static int UNCHANGED = 0x00;
	public static int ATOM = 0;
	public static int OR   = 1;
	public static int SOME = 2;
	public static int ALL  = 3;
	public static int MIN  = 4;
	public static int MAX  = 5;
	public static int NOM  = 6;
	public static int TYPES = 7;
	
	//Added for Econnections
	private String ontology;
	
	private int status;
	
	protected ABox abox;
	protected ATermAppl name;
	protected Map depends;
	protected int depth = 1;
	private boolean isRoot;
	private boolean isConceptRoot;		
	
	/**
	 * If this node is merged to another one, points to that node otherwise
	 * points to itself. This is a linked list implementation of disjoint-union
	 * data structure.
	 */
	protected Node mergedTo = this;
    
    protected EdgeList inEdges;
	
	/**
	 * Dependency information about why merged happened (if at all)
	 */
	protected DependencySet mergeDepends = null;
	
	protected DependencySet pruned = null;
	
	/**
	 * Set of other nodes that have been merged to this node. Note that this 
	 * is only the set of nodes directly merged to this one. A recursive traversal
	 * is required to get all the merged nodes.
	 */
	protected Set merged;
	
	protected Map differents;
	
	int branch;
	
	protected Node(ATermAppl name, ABox abox) {
		this.name = name;
		this.abox = abox;		
		
		if(abox != null)
		    ontology = abox.getKB().getOntology();
		
		isRoot = !ATermUtils.isAnon( name );
		isConceptRoot = false;
		
		mergeDepends = DependencySet.INDEPENDENT; 
		differents = new HashMap();
		depends = new HashMap();

        inEdges = new EdgeList();
        
		branch = 0;
		
		status = CHANGED;
	}

	protected Node(Node node, ABox abox) {
		this.name = node.getName();
		this.abox = abox;
		this.ontology = node.ontology;

		isRoot = node.isRoot;
		isConceptRoot = node.isConceptRoot;
		
		mergeDepends = node.mergeDepends;
		mergedTo = node.mergedTo;
		merged = node.merged;
		pruned = node.pruned;

		// do not copy differents right now because we need to
		// update node references later anyway
		differents = node.differents;
		depends = new HashMap(node.depends);
		
        
        if(abox == null) {
            mergedTo = null;
            inEdges = new EdgeList(node.inEdges.size());
            for(int i = 0; i < node.inEdges.size(); i++) {
                Edge edge = node.inEdges.edgeAt(i);
                Node to = this;
                Individual from = new Individual(edge.getFrom().getName());
                Edge newEdge = new Edge(edge.getRole(), from, to, edge.getDepends());
                inEdges.addEdge(newEdge);
            }
        }
        else {
            inEdges = node.inEdges;
        }
        
		branch = node.branch;
		status = CHANGED;
	}
	
	protected void updateNodeReferences() {
        mergedTo = abox.getNode( mergedTo.getName() );

        Map diffs = new HashMap( differents.size() );
        for(Iterator i = differents.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Entry) i.next();
            Node node = (Node) entry.getKey();

            diffs.put( abox.getNode( node.getName() ), entry.getValue() );
        }
        differents = diffs;

        if( merged != null ) {
            Set sames = new HashSet( merged.size() );
            for(Iterator i = merged.iterator(); i.hasNext();) {
                Node node = (Node) i.next();

                sames.add( abox.getNode( node.getName() ) );
            }
            merged = sames;
        }
        
        // FIXME should we use isPruned 
        boolean addOutEdges = !isMerged();
        
        EdgeList oldEdges = inEdges;
        inEdges = new EdgeList(oldEdges.size());
        for(int i = 0; i < oldEdges.size(); i++) {
            Edge edge = oldEdges.edgeAt(i);
            Individual from = abox.getIndividual(edge.getFrom().getName());
            Edge newEdge = new Edge(edge.getRole(), from, this, edge.getDepends());
            inEdges.addEdge(newEdge);
            if( addOutEdges )
                from.addOutEdge(newEdge);
        }
    }
	
	final public Node copy() {
		return copyTo(null);
	}
	
	final public boolean isChanged() {
		return status != UNCHANGED;
	}

	final public boolean isChanged(int type) {
		return (status & (1 << type)) != 0;
	}
	
	final public void setChanged(boolean changed) {
		status = changed ? CHANGED : UNCHANGED;
	}	
	
	//Added for Econnections
	public void setOntology(String ont)	{
		ontology = ont;
    }
	
	public String getOntology(){
		return ontology;
	}
	
	//*********************************************
	public void setChanged(int type, boolean changed) {
		if(changed)
			status = (status | (1 << type));
		else
			status = (status & ~(1 << type));
	}	

	
	/**
	 * Returns true if this is the node created for the concept satisfiability check.
	 *  
	 * @return
	 */
	public boolean isConceptRoot() {
	    return isConceptRoot;
	}
	
	public void setConceptRoot( boolean isConceptRoot ) {
	    this.isConceptRoot = isConceptRoot;
	}
	
	final public boolean isBnode() {
		return name.getName().startsWith(PelletOptions.BNODE);
	}

	final public boolean isNamedIndividual() {
		return isRoot && !isConceptRoot && !isBnode();
	}
	
	final public boolean isRoot() {
		return isRoot || isNominal();
	}	
	
	public abstract boolean isLeaf();
		
	public boolean isRootNominal() {
		return isRoot && isNominal();
	}
	
	public abstract Node copyTo(ABox abox);
	
	protected void addInEdge(Edge edge) {
        inEdges.addEdge( edge );   
    }

    public EdgeList getInEdges() {
	    return inEdges;
    }	
    
    public boolean removeInEdge(Edge edge) {
        boolean removed = inEdges.removeEdge(edge);
        
        if( !removed )
            throw new InternalReasonerException(
                "Trying to remove a non-existing edge " + edge);
        
        return true;
    }
    
    public void removeInEdges() {
        inEdges = new EdgeList();
    }

	public boolean restore(int branch) {
		//if(DEBUG) System.out.println("Node " + name  + (isMerged() ? "(" + mergedTo + ")" : ""));
		
	    if( pruned != null ) {
			if( pruned.branch > branch ) {			
				if(DEBUG) System.out.println("RESTORE: " + this + " merged node " + mergedTo + " " + mergeDepends);
				
				if( mergeDepends.branch > branch )
				    undoSetSame();
				
				unprune( branch );
			}
			else {
				if(DEBUG) System.out.println("DO NOT RESTORE: pruned node " + this + " = " + mergedTo + " " + mergeDepends);		    
			    return false;
			}
	    }
		
		List conjunctions = new ArrayList();
		
		status = CHANGED;
		
		Iterator i = getTypes().iterator();
		while(i.hasNext()) {									
			ATermAppl c = (ATermAppl) i.next();	
			DependencySet d = getDepends(c);
			
			boolean removeType = PelletOptions.USE_SMART_RESTORE
//                ? ( !d.contains( branch ) )
                ? ( d.max() >= branch )
				: ( d.branch > branch );  
			
			if( removeType ) {				
				if(DEBUG) System.out.println("RESTORE: " + this + " remove type " + c + " " + d + " " + branch);
				i.remove();
				removeType(c);
			}
			else if( PelletOptions.USE_SMART_RESTORE && ATermUtils.isAnd( c ) ) {
			    conjunctions.add( c );
			}			    
		}						
		
		// with smart restore there is a possibility that we remove a conjunct 
		// but not the conjunction. this is the case if conjunct was added before 
		// the conjunction but depended on an earlier branch. so we need to make
		// sure all conjunctions are actually applied
		if( PelletOptions.USE_SMART_RESTORE ) {
			i = conjunctions.iterator();
			while(i.hasNext()) {
	            ATermAppl c = (ATermAppl) i.next();
	            DependencySet d = getDepends(c);
				for(ATermList cs = (ATermList) c.getArgument(0); !cs.isEmpty(); cs = cs.getNext()) {
					ATermAppl conj = (ATermAppl) cs.getFirst();
					
					addType(conj, d);
				}            
	        }
		}        
        
        ATermAppl c =  (ATermAppl) abox.typeAssertions.get( name );
        if( c != null )
            addType( c, DependencySet.INDEPENDENT );
		
		i = differents.entrySet().iterator();
		while(i.hasNext()) {
			Map.Entry entry = (Entry) i.next();
			Node node = (Node) entry.getKey();
			DependencySet d = (DependencySet) entry.getValue();

			if( d.branch > branch ) {			
				if(DEBUG) System.out.println("RESTORE: " + name + " delete difference " + node);
				i.remove();
			}			
		}
        
        i = inEdges.iterator();
        while( i.hasNext() ) {            
            Edge e = (Edge) i .next();
            DependencySet d = e.getDepends();
            
            if( d.branch > branch ) {           
                if(DEBUG) System.out.println("RESTORE: " + name + " delete reverse edge " + e);
                i.remove();
            }           
        }
		
		return true;
	}
	
//	public void restore(int branch) {
////		if(DEBUG) System.out.println("Node " + name  + (isMerged() ? "(" + mergedTo + ")" : ""));
//
//		status = CHANGED;
//		
//		Iterator i = null;
//		
////		if( (PelletOptions.USE_SMART_RESTORE && mergeDepends.max() >= branch) || 
////		    (!PelletOptions.USE_SMART_RESTORE && mergeDepends.branch > branch) ) {			
//		if( mergeDepends.branch > branch ) {			
//			if(DEBUG) System.out.println("RESTORE: " + this + " merged node " + mergedTo + " " + mergeDepends);
//			
//			undoSetSame();
//			reattach(branch);
//		}
//		else if( mergedTo != this ) {
//			if(DEBUG) System.out.println("DO NOT RESTORE: " + this + " merged node " + mergedTo + " " + mergeDepends);		    
//		    return;
//		}
//		
//		List conjunctions = new ArrayList();
//		
//		i = getTypes().iterator();
//		while(i.hasNext()) {									
//			ATermAppl c = (ATermAppl) i.next();	
//			DependencySet d = getDepends(c);
//			
//			boolean removeType = PelletOptions.USE_SMART_RESTORE
//				? ( d.max() >= branch )
//				: ( d.branch > branch );    
//			
//			if( removeType ) {				
//				if(DEBUG) System.out.println("RESTORE: " + name + " remove type " + c + " " + d + " " + branch);
//				i.remove();
//				removeType(c);
//			}
//			else if( PelletOptions.USE_SMART_RESTORE && ATermUtils.isAnd( c ) ) {
//			    conjunctions.add( c );
//			}			    
//		}						
//		
//		// with smart restore there is a possibility that we remove a conjunct 
//		// but not the conjunction. this is the case if conjunct was added before 
//		// the conjunction but depended on an earlier branch. so we need to make
//		// sure all conjunctions are actually applied
//		if( PelletOptions.USE_SMART_RESTORE ) {
//			i = conjunctions.iterator();
//			while(i.hasNext()) {
//	            ATermAppl c = (ATermAppl) i.next();
//	            DependencySet d = getDepends(c);
//				for(ATermList cs = (ATermList) c.getArgument(0); !cs.isEmpty(); cs = cs.getNext()) {
//					ATermAppl conj = (ATermAppl) cs.getFirst();
//					
//					addType(conj, d);
//				}            
//	        }
//		}
//		
//		i = getInEdges().iterator();
//		while(i.hasNext()) {
//			Edge e = (Edge) i .next();
////			Node n = e.getFrom();
//			DependencySet d = e.getDepends();
//			
////			if( (PelletOptions.USE_SMART_RESTORE && (d.max() >= branch || n.branch > branch)) || 
////			    (!PelletOptions.USE_SMART_RESTORE && d.branch > branch) ) {			
//			if( d.branch > branch ) {			
//				if(DEBUG) System.out.println("RESTORE: " + name + " delete reverse edge " + e);
//				i.remove();
//			}			
//		}
//		
//		i = differents.entrySet().iterator();
//		while(i.hasNext()) {
//			Map.Entry entry = (Entry) i.next();
//			Node node = (Node) entry.getKey();
//			DependencySet d = (DependencySet) entry.getValue();
//			
////			if( (PelletOptions.USE_SMART_RESTORE && (d.max() >= branch || node.branch > branch)) || 
////			    (!PelletOptions.USE_SMART_RESTORE && d.branch > branch) ) {			
//			if( d.branch > branch ) {			
//				if(DEBUG) System.out.println("RESTORE: " + name + " delete difference " + node);
//				i.remove();
//			}			
//		}
//	}
//
//	/**
//	 * Detaches this node from all the other nodes. All the incoming edges to
//	 * this node are removed from other nodes' outEdges but this node still 
//	 * keeps the inEdges list so reaataching this node to the graph is possible
//	 * during a restore. Thus, after a node is detached calling getInEdges()
//	 * will return inconsistent info
//	 */
//	public void detach() {
//		EdgeList edges = getInEdges();
//		for(int i = 0; i < edges.size(); i++) {
//			Edge edge = edges.edgeAt(i);
//			Individual pred = edge.getFrom();
//			pred.removeEdge(edge);
//		}
//	}
//	
//	/**
//	 * Reattach this node to the graph. Simply traverses the inEdges that was
//	 * kept inside this node and restores the outEdges on the other nodes. 
//	 */
//	public void reattach(int branch) {
//		EdgeList edges = getInEdges();
//		for(int i = 0; i < edges.size(); i++) {
//			Edge edge = edges.edgeAt(i);
//			DependencySet d = edge.getDepends();
//			
//			if(d.branch <= branch) {
//				Individual pred = edge.getFrom();
//				
//				// if both pred and *this* were merged to other nodes (in that order)
//				// there is a chance we might duplicate the edge so first check for
//				// the existence of the edge
//				if(!pred.hasEdge(edge.getRole(), this)) {
//					pred.addOutEdge(edge);
//					
//					if(DEBUG) System.out.println("RESTORE: " + name + " ADD reverse edge " + edge);
//				}
//			}
//		}
//	}
	
	public void addType(ATermAppl c, DependencySet ds) {
	    if( isPruned() )
	        throw new InternalReasonerException( "Adding type to a pruned node " + this + " " + c );
	    else if( isMerged() )
	        return;
	    
		ds = ds.copy();
		
		ds.branch = abox.getBranch();
		
		int max = ds.max();
		if(ds.branch == -1 && max != 0)
		    ds.branch = max + 1;
		
		depends.put(c, ds);
		
		abox.changed = true;
	}

	public void removeType(ATermAppl c) {
		depends.remove(c);

		status = CHANGED;
	}

	public boolean hasType(ATerm c) {
		return depends.containsKey(c);
	}
	
	public boolean hasObviousType( ATermAppl c ) {
		DependencySet ds = getDepends( c );
		
		if( ds != null && ds.isIndependent() )
		    return true;
		
		if( isIndividual() && ATermUtils.isSomeValues( c ) ) {
		    Individual ind = (Individual) this;
            ATermAppl r = (ATermAppl) c.getArgument(0);
            ATermAppl d = (ATermAppl) c.getArgument(1);

            Role role = abox.getRole( r );
            EdgeList edges = ind.getRNeighborEdges( role );
            for(int e = 0; e < edges.size(); e++) {
                Edge edge = edges.edgeAt(e);
                
                if( !edge.getDepends().isIndependent() )
                    continue;
                
                Node y = edge.getNeighbor( ind );

                if( y.hasObviousType( d ) ) {
                    return true;
                }
            }    
		}
		
		return false;
	}

	public boolean hasObviousType( Collection coll ) {
		for(Iterator i = coll.iterator(); i.hasNext();) {
            ATermAppl c = (ATermAppl) i.next();
            
    		DependencySet ds = getDepends( c );
    		
    		if( ds != null && ds.isIndependent() )
    			return true;
        }
		
		return false;
	}	
	
    public Individual getParent() {
        if( isBlockable() ) {
            if( inEdges.size() == 0 )
                return null;
            else    
                return inEdges.edgeAt( 0 ).getFrom();                       
        }
        
        return null;
    }
	
	final public Set getPredecessors() {
		return getInEdges().getPredecessors();
	}				

	final boolean hasPredecessor( Individual x ) {
		return x.hasSuccessor( this );
	}
	
	public abstract boolean hasSuccessor( Node x );
	
	final public DependencySet getDepends(ATerm c) {
		return (DependencySet) depends.get(c);
	}
	
	final public Map getDepends() {
		return depends;
	}
	
	final public Set getTypes() {
		return depends.keySet();
	}	

	public void removeTypes() {
		depends.clear();
		status = CHANGED;
	}

	final public int prunedAt() {	    
		return pruned.branch;
	}
	
	final public boolean isPruned() {
		return pruned != null;
	}
	
	final public DependencySet getPruned() {
		return pruned;
	}
		
	public abstract void prune(DependencySet ds);

	public void unprune( int branch ) {
        pruned = null;

        for(int i = 0; i < inEdges.size(); i++) {
            Edge edge = inEdges.edgeAt( i );
            DependencySet d = edge.getDepends();

            if( d.branch <= branch ) {
                Individual pred = edge.getFrom();
                Role role = edge.getRole();

                // if both pred and *this* were merged to other nodes (in that order)
                // there is a chance we might duplicate the edge so first check for
                // the existence of the edge
                if( !pred.hasEdge( role, this ) ) {
                    pred.addOutEdge( edge );

                    if( DEBUG )
                        System.out.println( "RESTORE: " + name + " ADD reverse edge " + edge );
                }
            }
        }
    }

	public abstract int getNominalLevel();
	
	public abstract boolean isNominal();
	
	public abstract boolean isBlockable();
	
	public abstract boolean isLiteral();
	
	public abstract boolean isIndividual();
	
	final public int mergedAt() {	    
		return mergeDepends.branch;
	}
	
	final public boolean isMerged() {
		return mergedTo != this;
	}

	final public Node getMergedTo() {
		return mergedTo;
	}
	
//	final public DependencySet getMergeDependency() {
//		return mergeDepends;
//	}
	
    /**
     * Get the dependency if this node is merged to another node. This
     * node may be merged to another node which is later merged to another 
     * node and so on. This function may return the dependency for the 
     * first step or the union of all steps.
     *
     */
    public DependencySet getMergeDependency( boolean all ) {
        if( !isMerged() || !all )
            return mergeDepends;

        DependencySet ds = mergeDepends; 
        Node node = mergedTo;
        while( node.isMerged() ) {
            ds = ds.union( node.mergeDepends );
            node = node.mergedTo;            
        }
        
        return ds;
    }
    
	public Node getSame() {
		if(mergedTo == this)
			return this;
		
		return mergedTo.getSame();
	}
	
	public void undoSetSame() {
		mergedTo.removeMerged( this );
		mergeDepends = DependencySet.INDEPENDENT;
		mergedTo = this;	    
	}
	
	private void addMerged( Node node ) {
	    if( merged == null )
	        merged = new HashSet( 3 );
	    merged.add( node );
	}
	
	private void removeMerged( Node node ) {
	    merged.remove( node );
	    if( merged.isEmpty() )
	        merged = null; // free space
	}
	
	final public void setSame(Node node, DependencySet ds) {
		if( isSame( node ) ) 
		    return;
        if( isDifferent( node ) ) {
		    abox.setClash( Clash.nominal( this, ds, node.getName() ) );
		    return;
		}
		
		mergedTo = node;
		mergeDepends = ds.copy();
		mergeDepends.branch = abox.getBranch();
		node.addMerged( this );
	}
	
	public boolean isSame(Node node) {
		return getSame().equals( node.getSame() );
	}
		
	public boolean isDifferent( Node node ) {
		return differents.containsKey(node);
	}
		
	public Set getDifferents() {
		return differents.keySet();
	}

	public DependencySet getDifferenceDependency(Node node) {
		return (DependencySet) differents.get(node);
	}	

	public void setDifferent(Node node, DependencySet ds) {
		if( isDifferent( node ) )
			return;
		if( isSame( node ) ) {
		    abox.setClash( Clash.nominal( this, ds, node.getName() ));
		    return;
		}
		
		ds = ds.copy();
		ds.branch = abox.getBranch();
		differents.put(node, ds);
		node.setDifferent(this, ds);
	}
	
	public void inheritDifferents( Node y, DependencySet ds ) {
	    Iterator yDiffs = y.differents.entrySet().iterator();
		while( yDiffs.hasNext() ) {
		    Map.Entry entry = (Map.Entry) yDiffs.next();
			Node yDiff = (Node) entry.getKey();
			DependencySet finalDS = ds.union( (DependencySet) entry.getValue() );
			
			setDifferent( yDiff, finalDS );
		}
	}
	
	public boolean equals(Object obj) {
		return this == obj;
	}

	final public ATermAppl getName() {
		return name;
	}
	
	public abstract ATermAppl getTerm();
	
	final public String getNameStr() {
		return name.getName();
	}
	
	public String toString() {
		return name.getName();
	}
	
	/**
	 * A string that identifies this node either using its name or the path
	 * of individuals that comes to this node. For example, a node that has
	 * been generated by the completion rules needs to be identified with
	 * respect to a named individual. Ultimately, we need the shortest path
	 * or something like that but right now we just use the first inEdge 
	 * 
	 * @return
	 */
	final public List getPath() {	
	    LinkedList path = new LinkedList();

        if(isNamedIndividual()) 
            path.add(name);
	    else {
		    Node node = this;
		    while(!node.getInEdges().isEmpty()) {
		        Edge inEdge = node.getInEdges().edgeAt(0);
		        node = inEdge.getFrom();
	            path.addFirst(inEdge.getRole().getName());
		        if(node.isNamedIndividual()) {
		            path.addFirst(node.getName());
		            break;
		        }
		    }
	    }
	    
		
		return path;
	}
	

	/**
	 * getABox
	 * 
	 * @return
	 */
	public ABox getABox() {
		return abox;
	}
}

