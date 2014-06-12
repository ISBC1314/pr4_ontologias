// The MIT License
//
// Copyright (c) 2003 Ron Alford, Mike Grove, Bijan Parsia, Evren Sirin
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to
// deal in the Software without restriction, including without limitation the
// rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
// sell copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
// IN THE SOFTWARE.

package org.mindswap.pellet;


import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mindswap.pellet.datatypes.AtomicDatatype;
import org.mindswap.pellet.datatypes.Datatype;
import org.mindswap.pellet.datatypes.DatatypeReasoner;
import org.mindswap.pellet.exceptions.InternalReasonerException;
import org.mindswap.pellet.tbox.TBox;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Bool;
import org.mindswap.pellet.utils.CandidateSet;
import org.mindswap.pellet.utils.MultiListIterator;
import org.mindswap.pellet.utils.Namespaces;
import org.mindswap.pellet.utils.SetUtils;
import org.mindswap.pellet.utils.Timer;

import aterm.ATerm;
import aterm.ATermAppl;
import aterm.ATermInt;
import aterm.ATermList;

/**
 * @author Evren Sirin
 */
public class ABox {
	public static boolean DEBUG = false;
	
	// nodes used for cached root for Top and Bottom concepts
	static final Individual TOP_IND    = new Individual(ATermUtils.TOP);
	static final Individual BOTTOM_IND = new Individual(ATermUtils.BOTTOM);
	static final Individual DUMMY_IND  = new Individual(ATermUtils.makeTermAppl("_DUMMY_"));
	
	// following two variables are used to generate names
	// for newly generated individuals. so during rules are
	// applied anon1, anon2, etc. will be generated. This prefix
	// will also make sure that any node whose name starts with 
	// this prefix is not a root node
	protected int anonCount = 0;
		
	/**
	 * Total number of satisfiability tests performed (for statistical purposes)
	 */ 
	public long satisfiabilityCount = 0;
	
	/**
	 * Total number of ABox consistency checks (for statistical purposes)
	 */ 
	public long consistencyCount = 0;
	
	/**
	 * size of the completion tree for statistical purposes 
	 */
	public int    treeDepth = 0;

	/**
	 * datatype reasoner used for checking the satisfiability of datatypes
	 */  
	protected DatatypeReasoner dtReasoner;
	
	 /**
	  * This is a list of nodes. Each node has a name expressed as an
	  * ATerm which is used as the key in the Hashtable. The value is
	  * the actual node object 
	  */
	protected Map nodes;
	
	/** 
	 * This is a list of node names. This list stores the individuals
	 * in the order they are created
	 */
	protected List nodeList;
	
	/**
	 * Indicates if any of the completion rules has been applied to 
	 * modify ABox
	 */
	boolean changed = false;
	
	private boolean doExplanation;
	
	// cached satisfiability results
	// the table maps every atomic concept A (and also its negation not(A))
	// to the root node of its completed tree. If a concept is mapped to 
	// null value it means it is not satisfiable
	protected Map cache;
	
	// pseudo model for this Abox. This is the ABox that results from
	// completing to the original Abox
	private ABox pseudoModel;

	// cache of the last completion. it may be different from the pseudo
	// model, e.g. type checking for individual adds one extra assertion
	// last completion is stored for caching the root nodes that was
	// the result of 
	private ABox lastCompletion;
	private boolean keepLastCompletion;
	private Clash lastClash;
	
	
	HashMap allBindings = new HashMap();
	
	// complete ABox means no more tableau rules are applicable
	private boolean isComplete = false;		
	
	// the last clash recorded
	private Clash clash;
	
	// the current branch number
	private int  branch;	
	private List branches;
	
	List toBeMerged;
	
    Map disjBranchStats;
    
    // if we are using copy on write, this is where to copy from
    ABox sourceABox;
    
    Map typeAssertions = new HashMap();
	
	// return true if init() function is called. This indicates parsing 
	// is completed and ABox is ready for completion
	private boolean initialized = false;
	
	// The KB to which this ABox belongs
	private KnowledgeBase kb;

	public boolean rulesNotApplied;

	public boolean ranRete = false;

	public ABox() {
		this(new KnowledgeBase());
	}

	public ABox(KnowledgeBase kb) {
		this.kb = kb;
		nodes = new HashMap();
		nodeList = new ArrayList();
		clash = null;
		doExplanation = false;
		dtReasoner = new DatatypeReasoner();
		keepLastCompletion = false;
		
		clearCaches(true);
		
		branch = 0;
		branches = new ArrayList();
		disjBranchStats = new HashMap();
		
		toBeMerged = new ArrayList();
		rulesNotApplied = true;
	}	

	public ABox(ABox abox) {
	    this(abox, null, true);
	}
	
	public ABox(ABox abox, ATermAppl extraIndividual, boolean copyIndividuals) {
		this.kb = abox.kb;
		Timer timer = kb.timers.startTimer("cloneABox");
	
		this.rulesNotApplied = true;
		initialized  = abox.initialized; 
		changed		 = abox.changed;
		anonCount	 = abox.anonCount;
		cache        = abox.cache;
		clash		 = abox.clash;	
		dtReasoner   = abox.dtReasoner;
		doExplanation = abox.doExplanation;
		disjBranchStats = abox.disjBranchStats;
		
		int extra = (extraIndividual == null) ? 0 : 1;
		int nodeCount = ( copyIndividuals ? abox.nodes.size() : 0 ) + extra;
		
		nodes = new HashMap( nodeCount );
        nodeList = new ArrayList( nodeCount );
		
		if(extraIndividual != null) {
			Individual n = new Individual(extraIndividual, this, Individual.BLOCKABLE);
			
			n.setConceptRoot( true );
			n.branch = DependencySet.NO_BRANCH;
			n.addType(ATermUtils.TOP, DependencySet.INDEPENDENT);
			nodes.put(extraIndividual, n);
			nodeList.add(extraIndividual);
			applyUC(n);
			
			if(PelletOptions.COPY_ON_WRITE) sourceABox = abox;
		}
				
		if( copyIndividuals ) {
			toBeMerged = abox.toBeMerged;
			if( sourceABox == null ) {
				for(int i = 0; i < nodeCount - extra; i++) {
					ATerm x = (ATerm) abox.nodeList.get(i);
					Node node = abox.getNode(x);					
					Node copy = node.copyTo(this);
					    				    
			        nodes.put(x, copy);
			        nodeList.add(x);
				}

				for(Iterator i = nodes.values().iterator(); i.hasNext(); ) {
					Node node = (Node) i.next();
	
					node.updateNodeReferences();
				}		
			}
		}
		else {
			toBeMerged = Collections.EMPTY_LIST;
		    sourceABox = null;
		}
		branch   = abox.branch;
		branches = new ArrayList(abox.branches.size());
		for(int i = 0, n = abox.branches.size(); i < n; i++) {
			Branch branch = (Branch) abox.branches.get(i);
			Branch copy;
			
			if( sourceABox == null ) {
				copy = branch.copyTo(this);
				copy.nodeCount = branch.nodeCount + extra; 			    
			}
			else {
				copy = branch;			    
			}
			branches.add(copy);
		}
		
		timer.stop();
	}	
		
	/**
	 * 
	 * Create a copy of this ABox with all the nodes and edges.
	 * 
	 * @return
	 */
	public ABox copy() {
		return new ABox(this);	
	}
	
	/**
	 * Create a copy of this ABox with one more additional individual. This
	 * is <b>NOT</b> equivalent to create a copy and then add the individual.
	 * The order of individuals in the ABox is important to figure out which
	 * individuals exist in the original ontology and which ones are created
	 * by the tableau algorithm. This function creates a new ABox such that
	 * the individual is supposed to exist in the original ontology. This is
	 * very important when satisfiability of a concept starts with a pesudo
	 * model rather than the initial ABox.
	 * 
	 * @param extraIndividual Extra individual to be added to the copy ABox 
	 * @return
	 */
	public ABox copy(ATermAppl extraIndividual, boolean copyIndividuals) {
		return new ABox(this, extraIndividual, copyIndividuals);	
	}
	
	public void copyOnWrite() {
	    if( sourceABox == null )
	        return;
	    
	    Timer t = kb.timers.startTimer( "copyOnWrite" );
	    
	    List currentNodeList = new ArrayList( nodeList );
	    int currentSize = currentNodeList.size();
		int nodeCount = sourceABox.nodes.size();	    
	    
	    nodeList = new ArrayList( nodeCount + 1 );
	    nodeList.add( currentNodeList.get( 0 ) );
	    
		for(int i = 0; i < nodeCount; i++) {
			ATerm x = (ATerm) sourceABox.nodeList.get(i);
			Node node = sourceABox.getNode(x);
			Node copyNode = node.copyTo(this);
			nodes.put(x, copyNode);
			nodeList.add(x);
		}
		
		if( currentSize > 1 )
		    nodeList.addAll( currentNodeList.subList( 1, currentSize ) );
		
		for(Iterator i = nodes.values().iterator(); i.hasNext(); ) {
			Node node = (Node) i.next();

			if( sourceABox.nodes.containsKey( node.getName() ) )
			    node.updateNodeReferences();
		}		

		for(int i = 0, n = branches.size(); i < n; i++) {
			Branch branch = (Branch) branches.get(i);			
			Branch copy = branch.copyTo(this);
			branches.set( i, copy );
						
			if( i >= sourceABox.getBranches().size() )
				copy.nodeCount += nodeCount;
            else
                copy.nodeCount += 1;
		}
		
		t.stop();
			    
	    sourceABox = null;		
	}
	
	/**
	 * Clear the psuedo model created for the ABox and concept satisfiability.
	 * 
	 * @param clearSatCache If true clear concept satisfiability cache, if false only clear
	 * pseudo model. 
	 */
	public void clearCaches(boolean clearSatCache) {
		pseudoModel = null;
		lastCompletion = null;
		
		if(clearSatCache) {
			cache = new HashMap();		
		}
	}
	
	Bool getCachedSat(ATermAppl c) {
	    CachedNode cached = (CachedNode) cache.get(c);
		return cached == null
			? Bool.UNKNOWN
		    : Bool.create( !cached.isBottom() );
	}

	Map getAllCached() {
	    return cache;
	}
	
	CachedNode getCached(ATermAppl c) {
		return (CachedNode) cache.get(c);
	}

	boolean cacheUnsatConcept(ATermAppl c) {
	    if( cache.containsKey( c ) ) {
	        CachedNode cached = getCached(c);
	        if( !cached.isBottom() )
	            throw new InternalReasonerException( "Caching inconsistent results for " + c);
	        return false;
	    }
	    else {
		    ATermAppl notC = ATermUtils.negate(c);
		    
		    cacheConcept(c, BOTTOM_IND, DependencySet.INDEPENDENT);
		    cacheConcept(notC, TOP_IND, DependencySet.INDEPENDENT);
		    
		    return true;
	    }
	}

	boolean cacheSatConcept(ATermAppl c) {
	    if( cache.containsKey( c ) ) {
	        CachedNode cached = getCached(c);
	        if( cached.isBottom() )
	            throw new InternalReasonerException( "Caching inconsistent results for " + c);
	        return false;
	    }
	    else {
	        cacheConcept(c, DUMMY_IND, DependencySet.INDEPENDENT);
	        return true;
	    }
	}
	
	void cacheConcept(ATermAppl c, Individual ind, DependencySet ds) {
	    // TODO should we check for cache override?
	    cache.put(c, new CachedNode( ind, ds ) );
	}	
	
	private void cache( ATermAppl x, ATermAppl c, boolean isConsistent ) {
		if( PelletOptions.USE_CACHING && x != null && c != null ) {
//			if( ATermUtils.isPrimitiveOrNegated(c) ) {
				if( isConsistent ) {
				    ABox lastABox = lastCompletion;
				    
				    // get the individual for this concept
					Individual rootNode = lastABox.getIndividual(x);
				    // if a merge occurred due to one or more non-deterministic 
				    // branches then what we are caching depends on this set
					DependencySet ds = rootNode.getMergeDependency( true );
					// if it is nerged get the representative node
					rootNode = (Individual) rootNode.getSame();

				    // collect all transitive property values
					if( kb.getExpressivity().hasNominal() )
					    lastABox.collectTransitivePropertyValues( rootNode );

					// create a copy of the individual (copying the edges 
					// but not the neighbors) 
				    rootNode = (Individual) rootNode.copy();
				    
				    if( DEBUG )
				        System.out.println( "Cache " + rootNode.debugString() );
				    
				    cacheConcept(c, rootNode, ds);
				}
				else {
					if(DEBUG) {
						System.out.println(c + " is not satisfiable");
						System.out.println(ATermUtils.negate(c) + " is TOP");
					}
					
					cacheUnsatConcept( c );
				}
//			}
		}		    
	}
	
	Bool mergable(Individual root1, Individual root2, boolean independent) {		
		Individual roots[] = new Individual[] { root1, root2 };
		
		// if a concept c has a cached node rootX == topNode then it means 
		// not(c) has a cached node rootY == bottomNode. Only nodes whose 
		// negation is unsatisfiable has topNode in their cache  
		
		if(roots[0] == BOTTOM_IND || roots[1] == BOTTOM_IND) {
			if(DEBUG) System.out.println("(1) true ");
			return Bool.FALSE;
		}
		else if(roots[0] == TOP_IND && roots[1] != BOTTOM_IND) {
			if(DEBUG) System.out.println("(2) false ");
			return Bool.TRUE;
		}
		else if(roots[1] == TOP_IND && roots[0] != BOTTOM_IND) {
			if(DEBUG) System.out.println("(3) false ");
			return Bool.TRUE;
		}
		else if( roots[0] == DUMMY_IND || roots[1] == DUMMY_IND )
		    return Bool.UNKNOWN;
					
		Bool result = Bool.TRUE;
		
		// first test if there is an atomic clash between the types of two roots
		// we pick the root with lower number of types an iterate through all the 
		// types in its label		
		int root = roots[0].getTypes().size() < roots[1].getTypes().size() ? 0 : 1;
		int otherRoot = 1 - root;		 
		for(Iterator i = roots[root].getTypes().iterator(); i.hasNext();) {
            ATermAppl c = (ATermAppl) i.next();
            ATermAppl notC = ATermUtils.negate( c );
            
			if(roots[otherRoot].hasType(notC)) {
			    DependencySet ds1 = roots[root].getDepends( c );
			    DependencySet ds2 = roots[otherRoot].getDepends( notC );
			    boolean allIndependent = independent && ds1.isIndependent() && ds2.isIndependent();
			    if( allIndependent )
			        return Bool.FALSE;
			    else {
				    if( DEBUG ) 
				        System.out.println( 
				            roots[root] + " has " + c + " " + 
				            roots[otherRoot] + " has negation " + 
				            ds1.max() + " " + ds2.max());
			        result = Bool.UNKNOWN;
			    }
			}            
        }

		// if there is a suspicion there is no way to fix it later so return now
		if( result.isUnknown() )
		    return result;
		
		for( root = 0; root < 2; root++ ) {
			otherRoot = 1 - root;		 
		
			for(Iterator i = roots[root].getTypes(Node.ALL).iterator(); i.hasNext(); ) {
				ATermAppl av = (ATermAppl) i.next();
				ATerm r = av.getArgument(0);
				Role role = getRole(r);
		
				if(roots[otherRoot].hasRNeighbor(role)) {
				    if( DEBUG ) 
				        System.out.println( 
				            roots[root] + " has " + av + " " + roots[otherRoot] + " has R-neighbor" );
				    
				    return Bool.UNKNOWN;
				}
			}								 

			for(Iterator i = roots[root].getTypes(Node.MAX).iterator(); i.hasNext(); ) {
				ATermAppl mc = (ATermAppl) i.next();
				ATermAppl maxCard = (ATermAppl) mc.getArgument(0);	
				
				Role maxR = getRole(maxCard.getArgument(0));
				int max = ((ATermInt) maxCard.getArgument(1)).getInt() - 1;

				int n1 = roots[root].getRNeighborEdges(maxR).getFilteredNeighbors(roots[root]).size();
				int n2 = roots[otherRoot].getRNeighborEdges(maxR).getFilteredNeighbors(roots[otherRoot]).size();
				
				if(n1 + n2 > max) {
				    if( DEBUG ) 
				        System.out.println( 
				            roots[root] + " has " + mc + " " + roots[otherRoot] + " has R-neighbor" );
				    return Bool.UNKNOWN;
				}
			}
		}	
		
		if( kb.getExpressivity().hasFunctionality() ) {
//			Timer t = kb.timers.startTimer( "func" );
			
			root = 
			    ( roots[0].getOutEdges().size() + roots[0].getInEdges().size() ) <
			    ( roots[1].getOutEdges().size() + roots[1].getInEdges().size() )
			    ? 0 : 1;
			otherRoot = 1 - root;		 

			Set checked = new HashSet();
			for(Iterator i = roots[root].getOutEdges().iterator(); i.hasNext();) {
			    Edge edge = (Edge) i.next();
			    Role role = edge.getRole();
			    
			    if( !role.isFunctional() || checked.contains( role ) )
			        continue;
			    
			    checked.add( role );
	
			    if( roots[otherRoot].hasRNeighbor( role ) ) {
				    if( DEBUG ) 
				        System.out.println( 
				            root1 + " and " + root2 + " has " + role);
				    return Bool.UNKNOWN;
			    }
	        }
	
			for(Iterator i = roots[root].getInEdges().iterator(); i.hasNext();) {
			    Edge edge = (Edge) i.next();
			    Role role = edge.getRole().getInverse();
			    
			    if( role == null || !role.isFunctional() || checked.contains( role ) )
			        continue;
			    
			    checked.add( role );
	
			    if( roots[otherRoot].hasRNeighbor( role ) ) {
				    if( DEBUG ) 
				        System.out.println( 
				            root1 + " and " + root2 + " has " + role);
			        return Bool.UNKNOWN;		    
			    }
	        }
			
//			t.stop();
		}
        
        if( kb.getExpressivity().hasNominal() ) {
            boolean nom1 = root1.isNamedIndividual();
            for(Iterator i = root1.getTypes(Node.NOM).iterator(); !nom1 && i.hasNext(); ) {
                ATermAppl nom = (ATermAppl) i.next();
                ATermAppl name = (ATermAppl) nom.getArgument(0);
    
                nom1 = !ATermUtils.isAnon( name );
            }
            
            boolean nom2 = root2.isNamedIndividual();
            for(Iterator i = root2.getTypes(Node.NOM).iterator(); !nom2 && i.hasNext(); ) {
                ATermAppl nom = (ATermAppl) i.next();
                ATermAppl name = (ATermAppl) nom.getArgument(0);
    
                nom2 = !ATermUtils.isAnon( name );
            }

            // FIXME it should be enough to check if named individuals are 
            // different or not 
            if( nom1 && nom2 )
                return Bool.UNKNOWN;
        }
		
		// if there is no obvious clash then c1 & not(c2) is satisfiable therefore
		// c1 is NOT a subclass of c2.
		return Bool.TRUE;
	}
		
	public boolean isSubClassOf(ATermAppl c1, ATermAppl c2) {	 
        CachedNode cached = getCached( c1 );
        if( cached != null ) {
            Bool type = isType( cached.node, c2, cached.depends.isIndependent() );
			if( type.isKnown() )
			    return type.isTrue();
		}
        
		if( DEBUG ) {
	        long count = kb.timers.getTimer("subClassSat") == null ? 0 : kb.timers.getTimer("subClassSat").getCount();
		    System.out.print( count + ") Checking subclass [" + c1.getName() + " " + c2.getName() + "]");
		}
        
//        System.err.println("DEBUGGING");
//        isType( cached.node, c2, cached.depends.isIndependent() );
        
		ATermAppl notC2 = ATermUtils.negate( c2 );
        Timer t = kb.timers.startTimer( "subClassSat" );
        boolean sub = !isSatisfiable( ATermUtils.makeAnd( c1, notC2 ) );
        t.stop();

		if( DEBUG ) 
        	System.out.println( " Result: " + sub + " (" + t.getLast() + "ms)" );

		return sub;
	}
			
	public boolean isSatisfiable(ATermAppl c) {
		c = ATermUtils.normalize(c);
		
		if(DEBUG) 
            System.out.print( "Satisfiability for " + c );  
		
		if(PelletOptions.USE_CACHING) {
		    CachedNode cached = getCached( c );
			if( cached != null ) {
				boolean satisfiable = !cached.isBottom();
				boolean needToCacheModel = 
				    ATermUtils.isPrimitiveOrNegated(c) && !cached.isComplete();
				if(DEBUG) System.out.println("Cached sat for " + c + " is " + satisfiable);
				// if explanation is enabled we should actually build the tableau 
				// again to generate the clash. we don't cache the explanation up
				// front because generating explanation is costly and we only want 
				// to do it when explicitly asked
				// note that when the concepts is satisfiable there is no explanation
				// to be generated so we return the result immediately
				if(!needToCacheModel && 
				    (satisfiable || !doExplanation))
					return satisfiable;
			}
		}

		satisfiabilityCount++;
	    
	    Timer t = kb.timers.startTimer( "satisfiability");
		boolean isSat = isConsistent( SetUtils.EMPTY_SET, c );
		t.stop();
		
		return isSat;
	}

	public CandidateSet getObviousInstances( ATermAppl c ) {
	    CandidateSet candidates = new CandidateSet( kb.getIndividuals() );
	    getObviousInstances( c, candidates );
	    
	    return candidates;
	}
	
	public void getObviousInstances( ATermAppl c, CandidateSet candidates ) {
        c = ATermUtils.normalize( c );
        Set subs = kb.isClassified() ? kb.taxonomy.getSubs( c, false, true ) : SetUtils.EMPTY_SET;
        subs.remove( ATermUtils.BOTTOM );
            
        Iterator i = candidates.iterator();
        while( i.hasNext() ) {                
            ATermAppl x = (ATermAppl) i.next();

            Bool isType = isKnownType( x, c, subs );
            if( isType.isFalse() )
                i.remove();
            else
                candidates.update( x, isType );
        }
    }

	public void getObviousTypes( ATermAppl x, List types, List nonTypes ) {
		Individual pNode = pseudoModel.getIndividual(x);
		if(pNode.mergedAt() > 0)
		    pNode = getIndividual(x);
		else        
		    pNode = (Individual) pNode.getSame();
		
		pNode.getObviousTypes( types, nonTypes );
	}

	public CandidateSet getObviousSubjects( ATermAppl p, ATermAppl o ) {
	    CandidateSet candidates = new CandidateSet( kb.getIndividuals() );
	    getObviousSubjects( p, o, candidates );
	    
	    return candidates;	    
	}

	public void getSubjects( ATermAppl p, ATermAppl o, CandidateSet candidates ) {
		Iterator i = candidates.iterator();
		while(i.hasNext()) {
		    ATermAppl s = (ATermAppl) i.next();
			
		    Bool hasObviousValue = hasObviousPropertyValue( s, p, o );
		    candidates.update( s, hasObviousValue );
		}
	}

	public void getObviousSubjects( ATermAppl p, ATermAppl o, CandidateSet candidates ) {
		Iterator i = candidates.iterator();
		while(i.hasNext()) {
		    ATermAppl s = (ATermAppl) i.next();
			
		    Bool hasObviousValue = hasObviousPropertyValue( s, p, o );
            if( hasObviousValue.isFalse() )
                i.remove();
            else
                candidates.update( s, hasObviousValue );
		}
	}

	public void getObviousObjects( ATermAppl p, CandidateSet candidates ) {
	    p = getRole( p ).getInverse().getName();
		Iterator i = candidates.iterator();
		while(i.hasNext()) {
			ATermAppl s = (ATermAppl) i.next();
						
		    Bool hasObviousValue = hasObviousObjectProperty( s, p );
		    candidates.update( s, hasObviousValue );
		}
	}

	public Bool isKnownType( ATermAppl x, ATermAppl c ) {
	    return isKnownType( x, c, SetUtils.EMPTY_SET );
	}
	
	public Bool isKnownType( ATermAppl x, ATermAppl c, Collection subs ) {
		Individual pNode = pseudoModel.getIndividual( x );
		if( pNode.mergedAt() > 0 )
		    pNode = getIndividual( x );
		else        
		    pNode = (Individual) pNode.getSame();
		
		return isKnownType( pNode, c, subs );
	}
		
	public Bool isKnownType( Individual pNode, ATermAppl concept, Collection subs ) {		
//	    Timer t = kb.timers.startTimer( "isKnownType" );
		Bool isType = isType( pNode, concept, true );    
		if( isType.isUnknown() ) {
            Set concepts = ATermUtils.isAnd( concept )
                ? ATermUtils.listToSet( (ATermList) concept.getArgument(0) )
                : SetUtils.singleton( concept );
            for( Iterator it = concepts.iterator(); it.hasNext(); ) {
                ATermAppl c = (ATermAppl) it.next();
                if( pNode.hasObviousType( c ) || pNode.hasObviousType( subs ) ) {
                    isType = Bool.TRUE;
                }
                else {          
                    isType = Bool.UNKNOWN;
                    
                    List axioms = kb.getTBox().getAxioms( c );
                    LOOP:
                    for( Iterator j = axioms.iterator(); j.hasNext(); ) {
                        ATermAppl axiom = (ATermAppl) j.next();
                        ATermAppl term = (ATermAppl) axiom.getArgument(1);
            
                        boolean equivalent = axiom.getName().equals( ATermUtils.SAME );
                        if( equivalent ) {
                            Iterator i = ATermUtils.isAnd( term )
                                ? new MultiListIterator( (ATermList) term.getArgument( 0 ) )
                                : Collections.singleton( term ).iterator();
                            Bool knownType = Bool.TRUE;
                            while( i.hasNext() && knownType.isTrue() ) {
                                term = (ATermAppl) i.next();
                                knownType = isKnownType( pNode, term, SetUtils.EMPTY_SET );
                            }
                            if( knownType.isTrue() ) {
                                isType = Bool.TRUE;
                                break LOOP;
                            }
                        }
                    }
                    if( isType.isUnknown() ) return Bool.UNKNOWN;
                }
            }    

		}
//		t.stop();
		
		return isType;
	}
	
	private Bool isType( Individual pNode, ATermAppl c, boolean isIndependent ) {
	    ATermAppl notC = ATermUtils.negate(c);
		
		CachedNode cached = getCached( notC );
		if( cached != null ) {
		    Timer t = kb.timers.startTimer("mergable");
		    isIndependent &= cached.depends.isIndependent();
			Bool mergable = mergable( pNode, cached.node, isIndependent );
			t.stop();
			if( mergable.isKnown() )
				return mergable.not();
		}
		
		if( PelletOptions.CHECK_NOMINAL_EDGES /*&& kb.getExpressivity().hasNominal()*/ ) {		 
		    cached = getCached( c );
			if( cached != null ) {
//			    Timer t = kb.timers.startTimer("checkNominalEdges");
				Individual cNode = cached.node;
				for(Iterator i = cNode.getOutEdges().iterator(); i.hasNext();) {
				    Edge edge = (Edge) i.next();
				    Role role = edge.getRole();
				    
				    if( edge.getDepends().isIndependent() ) {
				        boolean found = false;				        
				        Node val = edge.getTo();			    

				        if( !role.isObjectRole() ) {
				            found = pNode.hasRSuccessor( role );
				        }
				        else if( !val.isRootNominal() ) {
                            found = pNode.hasRNeighbor( role );
                        }				        
                        else {
					        Set neighbors = null;					        
					        
					        if( role.isSimple() || pNode.isConceptRoot() )
					            neighbors = pNode.getRNeighborNames( role );
					        else {
					            neighbors = new HashSet();
					            getObjectPropertyValues( pNode, role, neighbors, neighbors, new HashSet(), false );
					        }
                            
                            found = neighbors.contains( val.getName() );
                        }			
				            
				        if( !found ) {
				            return Bool.FALSE;
				        }
				    }
		        }
	
				for(Iterator i = cNode.getInEdges().iterator(); i.hasNext();) {
				    Edge edge = (Edge) i.next();
				    Role role = edge.getRole().getInverse();				    
				    Node val = edge.getFrom();
				    
				    if( edge.getDepends().isIndependent() ) {
				        boolean found = false;		
                        
                        if( !val.isRootNominal() ) {
                            found = pNode.hasRNeighbor( role );
                        }                       
                        else {
                            Set neighbors = null;                           
                            
                            if( role.isSimple() || pNode.isConceptRoot() )
                                neighbors = pNode.getRNeighborNames( role );
                            else {
                                neighbors = new HashSet();
                                getObjectPropertyValues( pNode, role, neighbors, neighbors, new HashSet(), false );
                            }
                            
                            found = neighbors.contains( val.getName() );
                        }       			            				        
				        
				        if( !found ) {
				            //System.out.println( "NOMINAL EDGE " + pNode + " " + c + " " + edge );
				            return Bool.FALSE;
				        }
				    }  
		        }					
//				t.stop();				
			}
		}	
		    
		return Bool.UNKNOWN;
	}
		
	public boolean isSameAs(ATermAppl ind1, ATermAppl ind2) {
		ATermAppl c = ATermUtils.makeValue( ind2 );

		return isType(ind1, c);	    
	}	
	
	/**
	 * Returns true if individual x belongs to type c. This is a logical 
	 * consequence of the KB if in all possible models x belongs to C.
	 * This is checked by trying to construct a model where x belongs
	 * to not(c).
	 * 
	 * @param x
	 * @param c
	 * @return
	 */
	public boolean isType( ATermAppl x, ATermAppl c ) {
		c = ATermUtils.normalize( c );
		
        Set subs = kb.isClassified() && kb.taxonomy.contains( c )
            ? kb.taxonomy.getSubs( c, false, true )  
            : SetUtils.EMPTY_SET;
        subs.remove( ATermUtils.BOTTOM );
            
        Bool type = isKnownType( x, c, subs );
		if( type.isKnown() )
		    return type.isTrue();		
        
        List list = (List) kb.instances.get( c );
        if( list != null )
            return list.contains( x );
        
		if( DEBUG ) 
		    System.out.println( "Checking type " + c + " for individual " + x );
		        
		ATermAppl notC = ATermUtils.negate(c);
		
        Timer t = kb.timers.startTimer( "isType" );
		boolean isType = !isConsistent( SetUtils.singleton( x ), notC );
        t.stop();
		
		if(DEBUG) 
		    System.out.println("Type "+ isType + " "  + c + " for individual " + x);
		
		return isType;
	}	
	
	/**
	 * Returns true if any of the individuals in the given list belongs
	 * to type c.
	 * 
	 * @param c
	 * @param inds
	 * @return
	 */
	public boolean isType(List inds, ATermAppl c) {
		c = ATermUtils.normalize(c);
		
		if(DEBUG) System.out.println("Checking type " + c + " for individuals " + inds);

		ATermAppl notC = ATermUtils.negate(c);

		boolean isType = !isConsistent( inds, notC );

		
		if(DEBUG) System.out.println("Type " + isType + " " + c + " for individuals " + inds);
		
		return isType;
	}	

	public Bool hasObviousPropertyValue(ATermAppl s, ATermAppl p, ATermAppl o) {
		Role prop = getRole( p );		

		if( prop.isDatatypeRole() ) {
		    Object value = (o == null) ? null : dtReasoner.getValue( o );
		    return hasObviousDataPropertyValue( s, p, value );
		}
		else if( o == null )
		    return hasObviousObjectProperty( s, p );
		else
		    return hasObviousObjectPropertyValue( s, p, o );		    
	}
	
	public Bool hasObviousDataPropertyValue(ATermAppl s, ATermAppl p, Object value) {
		Individual subj = pseudoModel.getIndividual( s );
		Role prop = getRole( p );		

		// if onlyPositive is set then the answer returned is sound but not
		// complete so we cannot return negative answers		
		boolean onlyPositive = false;
	
		if( subj.mergedAt() > 0 ) {
		    onlyPositive = true;		
		    subj = getIndividual( s );
		}
		else 
		    subj = (Individual) subj.getSame();
				
		Bool hasValue = subj.hasDataPropertyValue( prop, value );
		if( onlyPositive && hasValue.isFalse() )
		    return Bool.UNKNOWN;
		
		return hasValue; 
	}
	
	public Bool hasObviousObjectPropertyValue(ATermAppl s, ATermAppl p, ATermAppl o) {
	    Role prop = getRole( p );
	    
		Set knowns = new HashSet();
		Set unknowns = new HashSet();			
		
		getObjectPropertyValues( s, prop, knowns, unknowns );

		if( knowns.contains( o ) )
		    return Bool.TRUE;
		else if( unknowns.contains( o ) )
		    return Bool.UNKNOWN;
		else
		    return Bool.FALSE;
	}
	
	public Bool hasObviousObjectProperty(ATermAppl s, ATermAppl p) {
		Individual subj = pseudoModel.getIndividual( s );
		Role prop = getRole( p );		
		
//		if(subj == null)
//		    throw new InternalReasonerException(s + " is not a known individual!");
		
		if( subj.mergedAt() > 0 ) {
		    subj = getIndividual( s );
		}
		else {        
		    subj = (Individual) subj.getSame();
		}
		
		Bool hasValue = subj.hasObjectPropertyValue( prop, null );
		// FIXME we don't need the following check
//		if( hasValue.isFalse() )
//		    return Bool.UNKNOWN;
		
		return hasValue;   
	}
	
	public boolean hasProperty(ATermAppl s, ATermAppl p) {
	    Bool hasObviousValue = hasObviousPropertyValue( s, p, null );
		if( hasObviousValue.isKnown() )
			return hasObviousValue.isTrue();
		
		return false;		
	}
	
	public boolean hasPropertyValue(ATermAppl s, ATermAppl p, ATermAppl o) {
	    Timer t = kb.timers.startTimer("hasPropertyValue");
	    Bool hasObviousValue = hasObviousPropertyValue( s, p ,o );
		if( hasObviousValue.isKnown() )
			return hasObviousValue.isTrue();

		ATermAppl c = null;
		if( o == null ) {
		    c = ATermUtils.makeMin( p, 1 );
		}
		else {
		    c = ATermUtils.makeHasValue( p, o );
		}
		
		boolean isType = isType( s, c );
		
		t.stop();
		
		return isType;
	}

    public Set getPossibleProperties( ATermAppl s ) {
        Individual subj = pseudoModel.getIndividual( s );

        if( subj.mergedAt() > 0 )
            subj = getIndividual( s );
        else 
            subj = (Individual) subj.getSame();
                
        Set set = new HashSet();
        EdgeList edges = subj.getOutEdges();
        for(int i = 0; i < edges.size(); i++) {
            Edge edge = edges.edgeAt( i );
            Role role = edge.getRole();
            
            set.addAll(role.getSubRoles());
            set.addAll(role.getSuperRoles());
        }        

        edges = subj.getInEdges();
        for(int i = 0; i < edges.size(); i++) {
            Edge edge = edges.edgeAt( i );
            Role role = edge.getRole();
            
            if( !role.hasNamedInverse() )
                continue;
            
            role = role.getInverse();
            set.addAll(role.getSubRoles());
            set.addAll(role.getSuperRoles());
        } 
        return set;
    }
    
	public List getObviousDataPropertyValues(ATermAppl s, Role prop, Datatype datatype) {
		Individual subj = pseudoModel.getIndividual( s );

		if( subj.mergedAt() > 0 )
		    subj = getIndividual( s );
		else 
		    subj = (Individual) subj.getSame();
				
		List values = new ArrayList();
		EdgeList edges = subj.getRSuccessorEdges( prop );
		for(int i = 0; i < edges.size(); i++) {
		    Edge edge = edges.edgeAt( i );
		    DependencySet ds = edge.getDepends();
		    Literal literal = (Literal) edge.getTo();
		    ATermAppl literalValue = literal.getTerm();
		    if(literalValue != null && ds.isIndependent() ) {
		        if( datatype != null && !datatype.contains( literal.getValue() ) )
		            continue;
		        
		        values.add( literalValue );
		    }
		    else {
		        // TODO maybe we can get the value is literal.getDatatype().size() == 1 
		    }
		}		
		
		return values; 
	}
	
	public void getObjectPropertyValues( ATermAppl s, Role role, Set knowns, Set unknowns) {
	    ABox abox = pseudoModel != null ? pseudoModel : this;
	    
		Individual subj = abox.getIndividual( s );

		// FIXME check this
		// if we use the initial ABox rather than pseudoModel is it guaranteed 
		// that the unknowns returned will be complete?
		if( subj.mergedAt() > 0 )
		    subj = getIndividual( s );
		else 
		    subj = (Individual) subj.getSame();

		getObjectPropertyValues( subj, role, knowns, unknowns, new HashSet(), true );
	}

	void getObjectPropertyValues(Individual subj, Role prop, Set knowns, Set unknowns, Set visited, boolean getSames) {	    
        if( visited.contains( subj.getName() ) )
            return;
        else
            visited.add( subj.getName() );
        
		EdgeList edges = subj.getRNeighborEdges( prop );
		for(int i = 0; i < edges.size(); i++) {
		    Edge edge = edges.edgeAt( i );
		    DependencySet ds = edge.getDepends();
		    Individual value = (Individual) edge.getNeighbor( subj );
		    Role edgeRole = edge.getFrom().equals( subj ) 
		    	? edge.getRole() : edge.getRole().getInverse();		        
	        if( value.isRootNominal() ) {
	    	    if( ds.isIndependent() ) {
	                if( getSames )
	                    getSames( value, knowns, unknowns );
	                else
	                    knowns.add( value.getName() );
	            }
	            else {
	                if( getSames )
		                getSames( value, unknowns, unknowns );
	                else
	                    unknowns.add( value.getName() );	                
	            }
	        }
	        
			if( !prop.isSimple() ) {
			    // all the following roles might cause this property to propagate
			    Set transRoles = SetUtils.intersection( 
			        edgeRole.getSuperRoles(), 
			        prop.getTransitiveSubRoles() );
			    for(Iterator j = transRoles.iterator(); j.hasNext();) {
                    Role transRole = (Role) j.next();
    	            if( ds.isIndependent() ) 
    				    getObjectPropertyValues( value, transRole, knowns, unknowns, visited, getSames );
    	            else 
    				    getObjectPropertyValues( value, transRole, unknowns, unknowns, visited, getSames );                    
                }
            }
		}	
	}


	void collectTransitivePropertyValues( Individual subj ) {    
	    Set collected = new HashSet();	    
	    Set roles = subj.getOutEdges().getRoles();
	    for(Iterator i = roles.iterator(); i.hasNext();) {
            Role role = (Role) i.next();
            // only collect transitive roles, even if role is non-simple
            // we do not continue because only way a non-simple role
            // gets transitivity is if it there is successor for its
            // transitive sub role
            if( !role.isTransitive() || collected.contains( role ) )
                continue;
            
            collectTransitivePropertyValues( subj, role );
        }	    
	    roles = subj.getInEdges().getRoles();
	    for(Iterator i = roles.iterator(); i.hasNext();) {
            Role role = (Role) i.next();
            role = role.getInverse();
            if( !role.isTransitive() || collected.contains( role ) )
                continue;
            
            collectTransitivePropertyValues( subj, role );
        }	    
	}
	
	void collectTransitivePropertyValues( Individual subj, Role role ) {    
	    Set knowns = new HashSet();
	    Set unknowns = new HashSet();

	    getObjectPropertyValues( subj, role, knowns, unknowns, new HashSet(), false );
     
	    for(Iterator j = knowns.iterator(); j.hasNext();) {
            ATermAppl val = (ATermAppl) j.next();
            Individual ind = getIndividual( val );
            subj.addEdge( role, ind, DependencySet.INDEPENDENT );
        }
	    for(Iterator j = unknowns.iterator(); j.hasNext();) {
            ATermAppl val = (ATermAppl) j.next();
            Individual ind = getIndividual( val );
            subj.addEdge( role, ind, DependencySet.EMPTY );
        }    	    
	}
	
	public void getSames(Individual ind, Set knowns, Set unknowns) {
	    knowns.add( ind.getName() );
	    
		boolean thisMerged = ind.isMerged() && !ind.getMergeDependency(true).isIndependent();
		
		Iterator i = ind.getMerged().iterator();
		while( i.hasNext() ){
		    Individual other = (Individual) i.next();
		    
		    if( !other.isRootNominal() )
		        continue;

            boolean otherMerged = other.isMerged() && !other.getMergeDependency(true).isIndependent();
   	        if( thisMerged || otherMerged ) {
	            unknowns.add( other.getName() );
	            getSames( other, unknowns, unknowns );
   	        }
	        else {
	            knowns.add( other.getName() );
	            getSames( other, knowns, unknowns );
   	        }	            
        }
	}
	
	/**
	 * Return true if this ABox is consistent. Consistent ABox means 
	 * after applying all the tableau completion rules at least one 
	 * branch with no clashes was found  
	 * 
	 * @return
	 */
	public boolean isConsistent() {	   
	    // if ABox is empty then we need to actually check the satisfiability
	    // of the TOP class to make sure that universe is not empty
	    ATermAppl c = isEmpty() ? ATermUtils.TOP : null;
	    
		boolean isConsistent = isConsistent( SetUtils.EMPTY_SET, c );
		
		if( isConsistent ) {
			cacheConcept(ATermUtils.TOP, TOP_IND, DependencySet.INDEPENDENT);
			cacheConcept(ATermUtils.BOTTOM, BOTTOM_IND, DependencySet.INDEPENDENT);
		}
		
		return isConsistent;
	}
	
	/**
	 * Check the consistency of this ABox possibly after adding some type assertions. 
	 * If <code>c</code> is null then nothing is added to ABox (pure consistency test)
	 * and the indivdiuals should be an empty collection.
	 * If <code>c</code> is not null but <code>individuals</code> is empty, this is a
	 * satisfiability check for concept <code>c</code> so a new individual will be 
	 * added with type <code>c</code>. If individuals is not empty, this means we will
	 * add type <code>c</code> to each of the individuals in the collection and check
	 * the consistency.
	 * 
	 * <p>The consistency checks will be done either on a copy of the ABox or its pseudo
	 * model depending on the situation. In either case this ABox will not be modified
	 * at all. After the consistency check lastCompletion points to the modified ABox.
	 * 
	 * @param individuals
	 * @param c
	 * @return
	 */
	private boolean isConsistent( Collection individuals, ATermAppl c ) {
	    Timer t = kb.timers.startTimer( "isConsistent" );
	    
	    if( DEBUG ) 
	        System.out.println("Consistency " + c + " for " + individuals.size() + " individuals "/* + individuals*/);
	    
	    // throw away old information to let gc do its work
	    lastCompletion = null;
	    
	    // if c is null we are checking the consistency of this ABox as
	    // it is and we will not add anything extra
	    boolean buildPseudoModel = (c == null);

	    // if individuals is empty and we are not building the pseudo 
	    // model then this is concept satisfiability
	    boolean conceptSatisfiability =
	        !buildPseudoModel && individuals.isEmpty();
	    
	    // Check if there are any nominals in the KB or nominal
	    // reasoning is disabled
        boolean hasNominal = 
            kb.getExpressivity().hasNominal() &&
            !PelletOptions.USE_PSEUDO_NOMINALS;
       
        // Use empty model only if this is concept satisfiability for a KB
        // where there are no nominals (for Econn never use empty ABox)
	    boolean canUseEmptyModel = 
	        conceptSatisfiability && 
	        !hasNominal && 
	        !(kb instanceof EconnectedKB);
	    
	    // Use pseudo model only if we are not building the pseudo model, pseudo model
	    // option is enabled  and the strategy used to build the pseduo model actually
	    // supports pseduo model completion
	    boolean canUsePseudoModel =
	        !buildPseudoModel && 
	        pseudoModel != null &&
	        PelletOptions.USE_PSEUDO_MODEL &&
	        kb.chooseStrategy( this ).supportsPseudoModelCompletion();	 
	    	    
 
	    ATermAppl x = null;	    
	    if( conceptSatisfiability ) {            
			x = ATermUtils.makeTermAppl(ABox.DEBUG ? 
			    "x" + new UID() :
			    "Any member of " +  c.toString());
			
			individuals = SetUtils.singleton( x );	
	    }
	    	    
        ABox abox = canUseEmptyModel 
    		? copy( x, false )
    		: canUsePseudoModel 
    			? pseudoModel.copy( x, true ) 
    			: copy( x, true );

		for(Iterator i = individuals.iterator(); i.hasNext(); ) {
			ATermAppl ind = (ATermAppl) i.next();
			abox.addType( ind, c );
		}
	    
		if(DEBUG) System.out.println("Consistency check starts");

		if( abox.isEmpty() ) {
			lastCompletion = abox;		    
		}
		else {
			CompletionStrategy strategy = kb.chooseStrategy( abox );
			lastCompletion = strategy.complete();
		}

        
		boolean consistent = !lastCompletion.isClosed();
		
		if( buildPseudoModel )
		    pseudoModel = lastCompletion;
		
        if(DEBUG) 
            System.out.println(
                "Consistent: " + consistent + 
                " Tree depth: " + lastCompletion.treeDepth + 
                " Tree size: " + lastCompletion.getNodes().size());
        
        
		if( !consistent ) {
		    lastClash = lastCompletion.getClash();
			if(DEBUG) {
				System.out.println(lastCompletion.getClash().detailedString());
			}
		}
		
		cache( x, c, consistent );
		
		consistencyCount++;
		
//		kb.timers.print();
		
		if( !keepLastCompletion )
		    lastCompletion = null;
		
		t.stop();
		
		return consistent;
	}

	void applyUC(Individual node) {
		ATermList UC = kb.getTBox().getUC();
		if(UC != null) {
			// all the concepts inside the universal concept should 
			// be added to each node manually
			for(ATermList t = UC; !t.isEmpty(); t = t.getNext()) {
				ATermAppl c = (ATermAppl) t.getFirst();
				node.addType(c, DependencySet.INDEPENDENT);									
			}
		}
	}
	
	public EdgeList getInEdges(ATerm x) {
		return getNode(x).getInEdges();
	}
	
	public EdgeList getOutEdges(ATerm x) {
		Node node = getNode(x);
		if(node instanceof Literal)
			return new EdgeList();
		return ((Individual) node).getOutEdges();
	}	
	
	public Individual getIndividual(ATerm x) {
		return (Individual) nodes.get(x);
	}	

	public Literal getLiteral(ATerm x) {
		return (Literal) nodes.get(x);
	}	

	public Node getNode(ATerm x) {
		return (Node) nodes.get(x);
	}
	
	public void addType(ATermAppl x, ATermAppl c) {
		c = ATermUtils.normalize(c);

		// when a type is being added to a pseudo model, i.e.
		// an abox that has already been completed, the branch
		// of the dependency set will automatically be set to
		// the current branch. We need to set it to initial
		// branch number to make sure that this type assertion
		// is being added to the initial model
		int remember = branch;
		branch = DependencySet.NO_BRANCH;
		
		Node node = getNode(x);
        DependencySet ds = DependencySet.INDEPENDENT;
		if( node.isMerged() ) {
            ds = node.getMergeDependency( true );
		    node = (Individual) node.getSame();
            if( !ds.isIndependent() ) {
                typeAssertions.put( x, c );
            }
		}		

		node.addType(c, ds);

		branch = remember;
	}

	public void removeType(ATermAppl x, ATermAppl c) {
		c = ATermUtils.normalize(c);
				
		Node node = getNode(x);
		node.removeType(c);
	}

	/**
	 * 
	 * Add a new literal to the ABox. This function is used only when the literal value
	 * does not have a known value, e.g. applyMinRule would create such a literal.
	 * 
	 * @return
	 */
	public Literal addLiteral() {
	    return createLiteral(ATermUtils.makeLiteral(createUniqueName()));
	}
	
	/**
	 * Add a new literal to the ABox. Literal will be assigned a fresh unique name.
	 * 
	 * @param dataValue A literal ATerm which should be constructed with one of
	 * ATermUtils.makeXXXLiteral functions
	 * @return Literal object that has been created
	 */
	public Literal addLiteral(ATermAppl dataValue) {
		if(dataValue == null || !ATermUtils.isLiteral(dataValue))
		    throw new InternalReasonerException(
		        "Invalid value to create a literal. Value: " + dataValue);
				        
		return createLiteral(dataValue);
	}
	
	/**
	 * 
	 * Helper function to add a literal. 
	 * 
	 * @param value The java object that represents the value of this literal
	 * @return
	 */
	private Literal createLiteral(ATermAppl dataValue) {
        String lexicalValue = ((ATermAppl) dataValue.getArgument( 0 )).getName();
        String lang = ((ATermAppl) dataValue.getArgument( 1 )).getName();
        String datatypeURI = ((ATermAppl) dataValue.getArgument( 2 )).getName();
        
        ATermAppl name = null;
        if( !datatypeURI.equals("") ) {
            Datatype dt = kb.getDatatypeReasoner().getDatatype( datatypeURI );
            if( dt instanceof AtomicDatatype )
                datatypeURI = ((AtomicDatatype) dt).getPrimitiveType().getURI();
            name = ATermUtils.makeTypedLiteral(lexicalValue, datatypeURI);
        }
        else
            name = ATermUtils.makePlainLiteral(lexicalValue, lang);
        
        Node node = getNode( name );
        if( node != null ) {
            if( node instanceof Literal )
                return (Literal) node;
            else
                throw new InternalReasonerException(
                    "Same term refers to both a literal and an individual: " + name);                
        }
        
		Literal lit = new Literal(name, dataValue, this);	
		lit.addType(ATermUtils.makeTermAppl(Namespaces.RDFS + "Literal"), DependencySet.INDEPENDENT);
		
		nodes.put(name, lit);
		nodeList.add(name);
		
		return lit;	
	}

	public Individual addIndividual(ATermAppl x) {
		Individual ind = addIndividual( x, Individual.NOMINAL );
		
        if( !PelletOptions.USE_PSEUDO_NOMINALS ) {
    		// add value(x) for nominal node but do not apply UC yet
    		// because it might not be complete. it will be added
    		// by CompletionStrategy.initialize()
    		ATermAppl valueX = ATermUtils.makeValue(x);
    		ind.addType(valueX, DependencySet.INDEPENDENT);
        }
        
		return ind;
	}

	Individual addFreshIndividual() {
	    ATermAppl name = createUniqueName();
	    Individual ind = addIndividual( name, Individual.BLOCKABLE );
	    
	    // apply UC directly because we know tableau rules are being 
	    // applied thus UC should be ready
	    applyUC( ind );
	    
	    return ind;
	}
	
	private Individual addIndividual(ATermAppl x, int nominalLevel) {
		if( nodes.containsKey( x ) ) 
			throw new InternalReasonerException("adding a node twice " + x);					
		
		//System.out.println("adding node " + x);	
		changed = true;
		
		Individual n = new Individual(x, this, nominalLevel);
		n.branch = branch;
		n.addType(ATermUtils.TOP, DependencySet.INDEPENDENT);
		
		nodes.put(x, n);
		nodeList.add(x);
		
		return n;
	}
	
	public void addSame(ATermAppl x, ATermAppl y) {
		Individual ind1 = getIndividual(x);
		Individual ind2 = getIndividual(y);
		
//		ind1.setSame(ind2, new DependencySet(explanationTable.getCurrent()));
		
//		ind1.setSame(ind2, DependencySet.INDEPENDENT);
		
		toBeMerged.add( new NodeMerge( ind1, ind2, DependencySet.INDEPENDENT ) );
	}
	
	public void addDifferent(ATermAppl x, ATermAppl y) {
		Individual ind1 = getIndividual(x);
		Individual ind2 = getIndividual(y);
		
//		ind1.setDifferent(ind2, new DependencySet(explanationTable.getCurrent()));
		
		ind1.setDifferent(ind2, DependencySet.INDEPENDENT);
	}

	public boolean isNode(ATerm x) {
		return getNode(x) != null;			
	}	
		
	final private ATermAppl createUniqueName() {
		return ATermUtils.makeTermAppl( PelletOptions.ANON + (++anonCount) );
	}

	final public Collection getNodes() {
		return nodes.values();
	}
	
	final Map getNodeMap() {
		return nodes;
	}

	final public List getNodeNames() {
		return nodeList;
	}

	final public IndividualIterator getIndIterator() {	
		return new IndividualIterator(this);
	}
	
	final public IndividualIterator getSingletonIterator(Individual x) {
		int index = nodeList.indexOf(x.getName());
		return new IndividualIterator(this, index, index + 1);
	}

	public String toString() {
		return "[size: " + nodes.size() + " freeMemory: " + (Runtime.getRuntime().freeMemory()/1000000.0) + "mb]";
	}
	
	/**
	 * @return Returns the datatype reasoner.
	 */
	public DatatypeReasoner getDatatypeReasoner() {
		return dtReasoner;
	}

	/**
	 * @return Returns the isComplete.
	 */
	public boolean isComplete() {
		return isComplete;
	}

	/**
	 * @param isComplete The isComplete to set.
	 */
	void setComplete(boolean isComplete) {
		this.isComplete = isComplete;
	}
	
	/**
	 * Returns true if Abox has a clash.
	 * 
	 * @return
	 */
	public boolean isClosed() {
		return !PelletOptions.SATURATE_TABLEAU && clash != null;
	}
	
	public Clash getClash() {
		return clash;
	}

	public void setClash(Clash clash) {
		if(clash != null) {
		    if(this.clash != null) {
				if(ABox.DEBUG) 
				    System.out.println("Clash was already set \nExisting: " + this.clash + "\nNew     : " + clash);
				
				if(this.clash.depends.max() < clash.depends.max())
				    return;
//				else if(this.clash.isAtomic() && ATermUtils.isPrimitive((ATermAppl)this.clash.args[0]))
//				    return;
			}
		}
		this.clash = clash;
	}
	
	/**
	 * @return Returns the kb.
	 */
	public KnowledgeBase getKB() {
		return kb;
	}

	/**
	 * Convenience function to get the named role.
	 */
	public Role getRole(ATerm r) {
		return kb.getRole(r);
	}
	
	/**
	 * Return the RBox
	 */
	public RBox getRBox() {
		return kb.getRBox();
	}
	
	/**
	 * Return the TBox
	 */
	public TBox getTBox() {
		return kb.getTBox();
	}
	
	/**
	 * Return the current branch number. Branches are created when a non-deterministic rule,
	 * e.g. disjunction or max rule, is being applied.
	 * 
	 * @return Returns the branch.
	 */
	int getBranch() {
		return branch;
	}

	/**
	 * Set the branch number (should only be called when backjumping is in progress)
	 * 
	 * @param branch
	 */
	void setBranch(int branch) {
		this.branch = branch;
	}
	
	/**
	 * Incrment the branch number (should only be called when a non-deterministic rule,
	 * e.g. disjunction or max rule, is being applied)
	 * 
	 * @param branch
	 */
	void incrementBranch() {
		this.branch++;
	}
	
	/**
	 * Check if the ABox is ready to be completed.
	 * 
	 * @return Returns the initialized.
	 */
	public boolean isInitialized() {
		return initialized && !kb.isChanged();
	}

	public void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}

	/**
	 * Checks if the explanatino is turned on.
	 * 
	 * @return Returns the doExplanation.
	 */
	final public boolean doExplanation() {
		return doExplanation || DEBUG;
	}
	
	/**
	 * Enable/disable explanation generation
	 * 
	 * @param doExplanation The doExplanation to set.
	 */
	public void setDoExplanation(boolean doExplanation) {
		this.doExplanation = doExplanation;
	}
	
	public String getExplanation() {
//		Clash lastClash = (lastCompletion != null) ? lastCompletion.getClash() : null;
		if(lastClash == null)
			return "No inconsistency was found! There is no explanation generated.";
		else
			return lastClash.detailedString();
	}
	
	/**
	 * Returns the branches.
	 */
	public List getBranches() {
		return branches;
	}

	
	/**
	 * Validate all the edges in the ABox nodes. Used to find bugs in the copy and
	 * detach/attach functions.
	 */
    public void validate() {
        if(!PelletOptions.VALIDATE_ABOX) return;
        System.out.print("VALIDATING...");
        Iterator n = getIndIterator();
        while(n.hasNext()) {
            Individual node = (Individual) n.next();
            if( node.isPruned() )
                continue;
            validate(node);
        }
    }

    void validate(Individual node) {
		List[] negatedTypes = {
		    node.getTypes(Node.ATOM), 
		    node.getTypes(Node.SOME),
		    node.getTypes(Node.OR),
		    node.getTypes(Node.MAX)}; 
	
		for(int j = 0; j < negatedTypes.length; j++ ) {
			for(int i = 0, n=negatedTypes[j].size(); i < n; i++ ) {
				ATermAppl a = (ATermAppl) negatedTypes[j].get(i);
				if(a.getArity() == 0) continue;
				ATermAppl notA = (ATermAppl) a.getArgument(0);
		
				if(node.hasType(notA)) {
				    if(!node.hasType(a))
				        throw new InternalReasonerException("Invalid type found: " + node + " " + j + " " + a + " " + node.debugString() + " " + node.depends); 
			        throw new InternalReasonerException("Clash found: " + node + " " + a + " " + node.debugString() + " " + node.depends); 				}
			}			
		}
        
        if( !node.isRoot() ) {
            if( node.getPredecessors().size() != 1 ) 
                throw new InternalReasonerException("Invalid blockable node: " + node + " " + node.getInEdges() );                
            
        }
        else if( node.isNominal() ) {
            ATermAppl nominal = ATermUtils.makeValue(node.getName());
            if( !node.hasType(nominal) )
                throw new InternalReasonerException("Invalid nominal node: " + node + " " + node.getTypes() );                                
        }
        
        for(Iterator i = node.getDepends().keySet().iterator(); i.hasNext();) {
            ATermAppl c = (ATermAppl) i.next();
            DependencySet ds = node.getDepends(c);
            if(ds.max() > branch || (!PelletOptions.USE_SMART_RESTORE && ds.branch > branch))
                throw new InternalReasonerException("Invalid ds found: " + node + " " + c + " " + ds + " " + branch); 
//    		if( c.getAFun().equals( ATermUtils.VALUEFUN ) ) {
//    		    if( !PelletOptions.USE_PSEUDO_NOMINALS ) {
//    		        Individual z = getIndividual(c.getArgument(0));
//    		        if(z == null)
//    		            throw new InternalReasonerException("Nominal to non-existing node: " + node + " " + c + " " + ds + " " + branch);
//    		    }
//    		}        
    	}
        for(Iterator i = node.getDifferents().iterator(); i.hasNext();) {
            Node ind = (Node) i.next();
            DependencySet ds = node.getDifferenceDependency(ind);
            if(ds.max() > branch || ds.branch > branch)
                throw new InternalReasonerException("Invalid ds: " + node + " != " + ind + " " + ds); 
            if(ind.getDifferenceDependency(node) == null)
                throw new InternalReasonerException("Invalid difference: " + node + " != " + ind + " " + ds); 
        }
        EdgeList edges = node.getOutEdges();
        for(int e = 0; e < edges.size(); e++) {
            Edge edge = edges.edgeAt(e);
            Node succ = edge.getTo();
            if(nodes.get(succ.getName()) != succ)
                throw new InternalReasonerException("Invalid edge to a non-existing node: " + edge + " " + 
                    nodes.get(succ.getName()) + "(" + nodes.get(succ.getName()).hashCode() + ")" +
                    succ +  "(" + succ.hashCode() + ")");   
            if(!succ.getInEdges().hasEdge(edge))                
                throw new InternalReasonerException("Invalid edge: " + edge);            
            if(succ.isMerged())                
                throw new InternalReasonerException("Invalid edge to a removed node: " + edge + " " + succ.isMerged());   
            DependencySet ds = edge.getDepends();
            if(ds.max() > branch || ds.branch > branch)
                throw new InternalReasonerException("Invalid ds: " + edge + " " + ds); 
            EdgeList allEdges = node.getEdgesTo(succ);
            if(allEdges.getRoles().size() != allEdges.size())
                throw new InternalReasonerException("Duplicate edges: " + allEdges);
        }
        edges = node.getInEdges();
        for(int e = 0; e < edges.size(); e++) {
            Edge edge = edges.edgeAt(e);
            DependencySet ds = edge.getDepends();
            if(ds.max() > branch || ds.branch > branch)
                throw new InternalReasonerException("Invalid ds: " + edge + " " + ds); 
        }
    }
    
    /**
     * Print the ABox as a completion tree (child nodes are indented).
     *
     */
    public void printTree() {
        if(!PelletOptions.PRINT_ABOX) return;
        System.out.println("PRINTING...");
        Iterator n = getIndIterator();
        while(n.hasNext()) {
            Individual node = (Individual) n.next();
            if( !node.isNominal() )
                continue;
            printNode(node, new HashSet(), "   ");
        }
    }

    /**
     * Print the node in the completion tree. 
     * 
     * @param node
     * @param printed
     * @param indent
     */
    private void printNode(Individual node, Set printed, String indent) {
        boolean printOnlyName = ( node.isNominal() && !printed.isEmpty() );

        if(printed.contains(node)) {
            System.out.println(" " + node.getNameStr());
            return;
        } else
            printed.add(node);
        if( node.isMerged() ) {
            System.out.println(
                node.getNameStr() + " -> " + 
                node.getSame().getNameStr() + " " + 
                node.getMergeDependency( true ));
            return;
        }

        System.out.println(node.debugString());
//        System.out.println(" " + node.getDepends());
        
        if(printOnlyName)
            return;

        indent += "  ";
        Iterator i = node.getSuccessors().iterator();
        while(i.hasNext()) {
            Node succ = (Node) i.next();
            EdgeList edges = node.getEdgesTo(succ);

            System.out.print(indent + "[");
            for(int e = 0; e < edges.size(); e++) {
                if(e > 0) System.out.print(", ");
                System.out.print(edges.edgeAt(e).getRole());
            }
            System.out.print("] ");
            if(succ instanceof Individual) 
                printNode((Individual) succ, printed, indent);
            else
                System.out.println(" (Literal) " + succ.getName() + " "
                        + succ.getTypes());
        }
    }

    public Clash getLastClash() {
        return lastClash;
    }
    
    public ABox getLastCompletion() {
        return lastCompletion;
    }
    
    public boolean isKeepLastCompletion() {
        return keepLastCompletion;
    }
    
    public void setKeepLastCompletion( boolean keepLastCompletion ) {
        this.keepLastCompletion = keepLastCompletion;
    }
    
    public ABox getPseudoModel() {
        return pseudoModel;
    }    

    /**
     * Return the number of nodes in the ABox. This number includes both the individuals
     * and the literals. 
     * 
     * @return
     */
    public int size() {
        return nodes.size();
    }
    
    /**
     * Returns true if there are no individuals in the ABox.
     * 
     * @return
     */
    public boolean isEmpty() {
        return nodes.isEmpty();
    }
}
