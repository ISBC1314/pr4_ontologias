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

/*
 * Created on Aug 29, 2004
 */
package org.mindswap.pellet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mindswap.pellet.exceptions.InternalReasonerException;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Bool;
import org.mindswap.pellet.utils.Timer;

import aterm.ATerm;
import aterm.ATermAppl;
import aterm.ATermInt;
import aterm.ATermList;

/**
 * Completion strategy for a SHN KB that does not have individuals in the ABox.
 * When ABox is empty completion always starts with a single root individual
 * that represents the concept whose satisfiability is being searched. Since
 * there are no inverse roles in SHN completion starts with root node and moves
 * towards the leaves. Once a node's satisfiability has been established there
 * is no need to reevaluate that value.
 * 
 * @author Evren Sirin
 */
public class EmptySHNStrategy extends CompletionStrategy {       
    private LinkedList mayNeedExpanding;

    private Individual root;
//    private boolean useCaching;
    
    private Map cachedNodes;

    public static final int NONE = 0x00;
    public static final int HIT  = 0x01;
    public static final int MISS = 0x02;
    public static final int FAIL = 0x04;
    public static final int ADD  = 0x08;
    public static final int ALL  = 0x0F;

    public static int SHOW_CACHE_INFO = NONE;

    public EmptySHNStrategy(ABox abox) {
        super(abox, new SubsetBlocking());
    }
    
    boolean supportsPseudoModelCompletion() {
        return false;
    }
    
    public void initialize() {     
//        useCaching = !abox.getKB().getExpressivity().hasDomain;
        mergeList = new ArrayList();
        
        unfoldingMap = abox.getKB().getTBox().getUnfoldingMap();
        
        cachedNodes = new HashMap();
		
        root = (Individual) abox.getNodes().iterator().next();
		root.setChanged(true);		
		abox.applyUC( root );
		
		abox.setBranch( 1 );
		abox.treeDepth = 1;
		abox.changed = true;
		abox.setComplete( false );		
        abox.setInitialized( true );
    }


    ABox complete() {
        if(ABox.DEBUG)
            System.out.println("************  EmptySHNStrategy  ************");
        
        if(abox.getNodes().isEmpty()) {
            abox.setComplete(true);
            return abox;
        }
        else if(abox.getNodes().size() > 1)
            throw new RuntimeException(
                "EmptySHNStrategy can only be used with an ABox that has a single individual.");

        initialize();
        
        mayNeedExpanding = new LinkedList();
        mayNeedExpanding.add( root );

        while( !abox.isComplete() && !abox.isClosed() ) {
            Individual x = getNextIndividual();

            if( x == null ) {
                abox.setComplete( true );
                break;
            }
            
            if(ABox.DEBUG) System.out.println("Starting with node " + x);
            if(ABox.DEBUG) abox.printTree();
                        
            abox.validate();
            
            expand( x );
            
            if(abox.isClosed()) {
                if(ABox.DEBUG)
                    System.out.println("Clash at Branch (" + abox.getBranch() + ") " + abox.getClash());

                if( backtrack() )
                    abox.setClash( null );
                else
                    abox.setComplete( true );
            }
        }
        
        if(ABox.DEBUG) abox.printTree();
        
        if( PelletOptions.USE_ADVANCED_CACHING ) {
            // if completion tree is clash free cache all sat concepts
	        if( !abox.isClosed() ) {
	            for(Iterator i = abox.getIndIterator(); i.hasNext();) {
	                Individual ind = (Individual) i.next();
	                ATermAppl c = (ATermAppl) cachedNodes.get( ind );
	                if( c != null ) {
					    if( abox.cacheSatConcept( c ) ) {
					        if((EmptySHNStrategy.SHOW_CACHE_INFO & EmptySHNStrategy.ADD) != 0) 
					            System.out.println( "+++ Cache sat concept " + c );
					        if(ATermUtils.isAnd( c )) {
					            ATermList list = (ATermList) c.getArgument(0);
					            while(  !list.isEmpty() ) {
					                ATermAppl d = (ATermAppl) list.getFirst();
								    if( abox.cacheSatConcept( d ) ) 
								        if((EmptySHNStrategy.SHOW_CACHE_INFO & EmptySHNStrategy.ADD) != 0) 
								            System.out.println( "+++ Cache sat concept " + d );
								    list = list.getNext();
					            }					            
					        }					        
					    }
	                }
	            }
	        }
        }
        
        return abox;
    }
    
    private Individual getNextIndividual() {
        Node next = null;
        while( !mayNeedExpanding.isEmpty() ) {
            next = (Node) mayNeedExpanding.getFirst();
            if( next instanceof Literal ) {
                next = null;
                mayNeedExpanding.removeFirst();
            }
            else
                break;
        }
        
        return (Individual) next;
    }
    
    private void expand(Individual x) {
        if( blocking.isBlocked(x) ) {
            mayNeedExpanding.removeFirst();
            return;
        }
        
        if( /*useCaching &&*/ PelletOptions.USE_ADVANCED_CACHING ) {
            Timer t = abox.getKB().timers.startTimer( "cache" );
	        Bool cachedSat = cachedSat( x );
	        t.stop();
	        if( cachedSat.isKnown() ) {
	            if( cachedSat.isTrue() )
	                mayNeedExpanding.removeFirst();
	            else {
	                // set the clash information to be the union of all types
	                DependencySet ds = DependencySet.EMPTY;
	                for(Iterator i = x.getTypes().iterator(); i.hasNext();) {
                        ATermAppl c = (ATermAppl) i.next();
                        ds = ds.union( x.getDepends( c ) );
                    }
	                abox.setClash( Clash.atomic( x, ds ) );
	            }
	            return;
	        }
        }
        
        do {	        
	        applyUnfoldingRule(x);
	        if(abox.isClosed()) return;
		    
	        applyDisjunctionRule(x);
	        if(abox.isClosed()) return;
	        
	        if(x.canApply(Node.ATOM) || x.canApply(Node.OR)) continue;
	        
	        // TODO: do we want to check blocking here again?

		    applySomeValuesRule(x);
		    if(abox.isClosed()) return;
		
		    applyMinRule(x);
		    if(abox.isClosed()) return;
		    
		    // we don't have any inverse properties but we could have 
		    // domain restrictions which means we might have to re-apply
		    // unfolding and disjunction rules
		    if(x.canApply(Node.ATOM) || x.canApply(Node.OR)) continue;
		    
	        applyMaxRule(x);
	        if(abox.isClosed()) return;		    
            
//            applyLiteralRule(x);
//            if(abox.isClosed()) return;         
        }    
	    while( x.canApply(Node.ATOM) || x.canApply(Node.OR) || x.canApply(Node.SOME) || x.canApply(Node.MIN) );
        
        mayNeedExpanding.removeFirst();
        
        int insert = ( PelletOptions.SEARCH_TYPE == PelletOptions.DEPTH_FIRST )
        	? 0
        	: mayNeedExpanding.size();
        
        mayNeedExpanding.addAll( insert, x.getSortedSuccessors() );                        
    }
    
    private ATermAppl createConcept( Individual x ) {
        Set types = new HashSet( x.getTypes() );
        types.remove( ATermUtils.TOP );
        types.remove( ATermUtils.makeValue( x.getName() ) );
        
        if( types.isEmpty() ) {
        	return ATermUtils.TOP;
        }
        
        int count = 0;
        ATerm[] terms = new ATerm[ types.size() ];        
        for(Iterator i = types.iterator(); i.hasNext(); ) {
            ATermAppl c = (ATermAppl) i.next();
            if( !ATermUtils.isAnd( c ) )
                terms[ count++ ] = c ;
        }
        
        return ATermUtils.makeAnd( ATermUtils.toSet( terms, count ) );   
    }
    
//  public static int earlyCache = 0;
//  public static int doubleCache = 0;
//  public static int multipleCache = 0;
    
    private Bool cachedSat(Individual x) {
        if( x.equals( root ) )
            return Bool.UNKNOWN;
        
        ATermAppl c = createConcept( x );
        
        if( cachedNodes.containsValue( c ) ) {
//            earlyCache++;
            if((EmptySHNStrategy.SHOW_CACHE_INFO & EmptySHNStrategy.HIT) != 0)
                System.out.println("already searching for " + c);
            return Bool.TRUE;
        }
        
        Bool sat = abox.getCachedSat( c );
        
        if( sat.isUnknown() && ATermUtils.isAnd( c ) ) {
	        ATermList concepts = (ATermList) c.getArgument( 0 );
	        if( concepts.getLength() == 2 ) {
	            ATermAppl c1 = (ATermAppl) concepts.getFirst();
	            ATermAppl c2 = (ATermAppl) concepts.getLast();
	            CachedNode cached1 = abox.getCached( c1 );
	            CachedNode cached2 = abox.getCached( c2 );
	            if( cached1 != null && cached1.isComplete() && cached2 != null && cached2.isComplete() ) {
	                sat = abox.mergable( cached1.node, cached2.node,
	                    cached1.depends.isIndependent() && cached2.depends.isIndependent());
	                if( sat.isKnown() ) {
//	                    doubleCache++;
	                    if( sat.isTrue() )
	                        abox.cacheSatConcept( c );
	                    else
	                        abox.cacheUnsatConcept( c );	                        
	                }
	            }
	        }
//	        else {
//	            boolean allCached = true;
//	            for( ATermList list = concepts; !list.isEmpty(); list = list.getNext() ) {
//	                ATermAppl t = (ATermAppl) list.getFirst();
//		            CachedNode cached = abox.getCached( t );
//		            if( cached == null || !cached.isComplete() ) {
//	                    allCached = false;
//	                    break;
//	                }	                    
//	            }
//	            if(allCached) {
////	                multipleCache++;
//	                System.out.println( "Cache possibility " + concepts);
//	            }
//	        }
	    }
        
        if( sat.isUnknown() ) {
            if((EmptySHNStrategy.SHOW_CACHE_INFO & EmptySHNStrategy.MISS) != 0) 
                System.out.println( "??? Cache miss for " + c );
            cachedNodes.put( x, c );            
        }
        else
            if((EmptySHNStrategy.SHOW_CACHE_INFO & EmptySHNStrategy.HIT) != 0) 
                System.out.println( "*** Cache hit for " + c + " sat = " + sat);            
        
        return sat;
    }

	public void restore(Branch br) {
//		Timer timer = timers.startTimer("restore");
	    	    
	    Node clashNode = abox.getClash().node;
	    List clashPath = clashNode.getPath();
	    clashPath.add( clashNode.getName() );
		
		abox.setBranch(br.branch);
		abox.setClash(null);
		abox.anonCount = br.anonCount;
		
		mergeList.clear();
		
		List nodeList = abox.getNodeNames();
		Map nodes = abox.getNodeMap();
		
		if(ABox.DEBUG) System.out.println("RESTORE: Branch " + br.branch);
		if(ABox.DEBUG && br.nodeCount < nodeList.size())
		    System.out.println("Remove nodes " + nodeList.subList(br.nodeCount, nodeList.size()));
		for(int i = 0; i < nodeList.size(); i++) {
			ATerm x = (ATerm) nodeList.get(i);
			
			Node node = abox.getNode(x);
			if(i >= br.nodeCount) {
				nodes.remove(x);
				ATermAppl c = (ATermAppl) cachedNodes.remove( node );
				if(c != null) {
				    if(clashPath.contains(x)) {
					    if((EmptySHNStrategy.SHOW_CACHE_INFO & EmptySHNStrategy.ADD) != 0) 
					        System.out.println( "+++ Cache unsat concept " + c );
				        abox.cacheUnsatConcept( c );
				    }
				    else
					    if((EmptySHNStrategy.SHOW_CACHE_INFO & EmptySHNStrategy.ADD) != 0) 
					        System.out.println( "--- Do not cache concept " + c + " " + x + " " + clashNode + " " + clashPath);				        
				}
			}
			else {
				node.restore(br.branch);
				
				// FIXME should we look at the clash path or clash node
				if( node.equals( clashNode ) )
				    cachedNodes.remove( node );
			}
		}		
		nodeList.subList(br.nodeCount, nodeList.size()).clear();

		for(Iterator i = abox.getIndIterator(); i.hasNext(); ) {
			Individual ind = (Individual) i.next();
//			applyConjunctions(ind);
			applyAllValues(ind);
//			applyNominalRule(ind);
		}		
		
		if(ABox.DEBUG) abox.printTree();
			
//		timer.stop();
	}
	
    protected boolean backtrack() {
        boolean branchFound = false;

        while(!branchFound) {
            int lastBranch = abox.getClash().depends.max();

            if(lastBranch <= 0) return false;

            List branches = abox.getBranches();
            Branch newBranch = null;
            if( lastBranch <= branches.size() ) {
                branches.subList(lastBranch, branches.size()).clear();
                newBranch = (Branch) branches.get(lastBranch - 1);
    
                if(ABox.DEBUG)
                    System.out.println("JUMP: " + lastBranch);
                if(newBranch == null || lastBranch != newBranch.branch)
                    throw new RuntimeException(
                        "Internal error in reasoner: Trying to backtrack branch "
                            + lastBranch + " but got " + newBranch);
    
                if(newBranch.tryNext < newBranch.tryCount)
                    newBranch.setLastClash( abox.getClash().depends );
    
                newBranch.tryNext++;
                
                if(newBranch.tryNext < newBranch.tryCount) {
                    restore(newBranch);
    
                    branchFound = newBranch.tryNext();
                }                    
            }

            if(!branchFound) {
                abox.getClash().depends.remove(lastBranch);
                if(ABox.DEBUG)
                    System.out.println("FAIL: " + lastBranch);
            }
            else {
                mayNeedExpanding = new LinkedList((LinkedList) newBranch.get("mnx"));
                if(ABox.DEBUG)
                    System.out.println("MNX : " + mayNeedExpanding);
            }

        }
        
        abox.validate();

        return branchFound;
    }

    void addBranch(Branch newBranch) {
        super.addBranch( newBranch );

        newBranch.put("mnx", new LinkedList(mayNeedExpanding));
    }

    /**
     * apply disjunction rule to the ABox
     *  
     */
    protected void applyDisjunctionRule(Individual node) {
        if(!node.canApply(Node.OR)) return;

        List types = node.getTypes(Node.OR);
        int size = types.size();
        ATermAppl[] disjunctions = new ATermAppl[size - node.applyNext[Node.OR]];
        types.subList(node.applyNext[Node.OR], size).toArray( disjunctions );
	    if( PelletOptions.USE_DISJUNCTION_SORTING != PelletOptions.NO_SORTING ) 
	        DisjunctionSorting.sort( node, disjunctions );
        
        for(int j = 0, n = disjunctions.length; j < n; j++) {
            ATermAppl disjunction = disjunctions[j];

            // disjunction is now in the form not(and([not(d1), not(d2), ...]))
            ATermAppl a = (ATermAppl) disjunction.getArgument(0);
            ATermList disjuncts = (ATermList) a.getArgument(0);
            ATermAppl[] disj = new ATermAppl[disjuncts.getLength()];

            for(int index = 0; !disjuncts.isEmpty(); disjuncts = disjuncts.getNext(), index++) {
                disj[index] = ATermUtils.negate( (ATermAppl) disjuncts.getFirst() );
                if(node.hasType(disj[index])) break;
            }

            if(!disjuncts.isEmpty()) continue;

            DisjunctionBranch newBranch = new DisjunctionBranch(abox, this, node,
                disjunction, node.getDepends(disjunction), disj);

            addBranch(newBranch);

            if(newBranch.tryNext() == false) return;
        }
        node.applyNext[Node.OR] = size;

    }

    /**
     * apply min rule to the ABox
     *  
     */
    protected void applyMinRule(Individual x) {
        if(!x.canApply(Individual.MIN)) return;

        List types = x.getTypes(Node.MIN);
        int size = types.size();
        for(int j = x.applyNext[Node.MIN]; j < size; j++) {
            ATermAppl mc = (ATermAppl) types.get(j);

            Role r = abox.getRole(mc.getArgument(0));
            int n = ((ATermInt) mc.getArgument(1)).getInt();

            if(x.hasDistinctRNeighborsForMin(r, n)) continue;

            DependencySet ds = x.getDepends(mc);

            if(ABox.DEBUG)
                System.out.println("MIN : " + x + " -> " + r
                    + " -> anon" + (n == 1 ? "" : 
                    (abox.anonCount + 1) + " - anon")
                    + (abox.anonCount + n) + " " + ds);

            Node[] y = new Node[n];
            for(int c1 = 0; c1 < n; c1++) {
                if(r.isDatatypeRole())
                    y[c1] = abox.addLiteral();
                else {
                    y[c1] = abox.addFreshIndividual();
                    y[c1].depth = x.depth + 1;

                    if(x.depth >= abox.treeDepth) abox.treeDepth = x.depth + 1;
                }
                addEdge(x, r, y[c1], ds);
            }

            for(int c1 = 0; c1 < n; c1++)
                for(int c2 = c1 + 1; c2 < n; c2++)
                    y[c1].setDifferent(y[c2], ds);
        }
        x.applyNext[Node.MIN] = size;
    }

    /**
     * apply some values rule to the ABox
     *  
     */
    protected void applySomeValuesRule(Individual x) {
        if(!x.canApply(Individual.SOME)) return;

        List types = x.getTypes(Node.SOME);
        int size = types.size();
        for(int j = x.applyNext[Node.SOME]; j < size; j++) {
            ATermAppl sv = (ATermAppl) types.get(j);

            // someValuesFrom is now in the form not(all(p. not(c)))
            ATermAppl a = (ATermAppl) sv.getArgument(0);
            ATermAppl s = (ATermAppl) a.getArgument(0);
            ATermAppl c = (ATermAppl) a.getArgument(1);

            Role role = abox.getRole(s);

            c = ATermUtils.negate(c);

            boolean neighborFound = false;
            // ''y'' is going to be the node we create, and ''edge'' its connection to the
            //current node
            Node y = null;
            Edge edge = null;
            
            //edges contains all the edges going into of coming out from the node
            //And labelled with the role R
            EdgeList edges = x.getRNeighborEdges(role);
            //We examine all those edges one by one and check if
            //the neighbor has type C, in which case we set neighborFound to
            //true
            for(int e = 0; !neighborFound && e < edges.size(); e++) {
                edge = edges.edgeAt(e);

                y = edge.getNeighbor(x);

                neighborFound = y.hasType(c);
            }

            //If we have found a R-neighbor with type C, continue, do nothing
            if(neighborFound) continue;
            //If not, we have to create it 
            DependencySet ds = x.getDepends(sv).copy();
            //If the role is a datatype property...
            if(role.isDatatypeRole()) {
                if(ABox.DEBUG)
                    System.out.println("SOME: " + x
                        + " -> " + s + " -> " + y + " : " + c + " - " + ds);

                Literal literal = (Literal) y;
                if( ATermUtils.isNominal(c) ) {
                    literal = abox.addLiteral((ATermAppl) c.getArgument(0));
                }
                else {
                    if( !role.isFunctional() || literal == null )
                        literal = abox.addLiteral();
                    literal.addType(c, ds);
                }
                addEdge(x, role, literal, ds);
            }
            //If it is an object property
            else {
                if(ATermUtils.isNominal(c) && !PelletOptions.USE_PSEUDO_NOMINALS) {
                    ATermAppl value = (ATermAppl) c.getArgument(0);
                    y = abox.getIndividual( value );
                    if(ABox.DEBUG)
                        System.out.println("VAL : " + x + " -> " + s
                            + " -> " + y + " - " + ds);
                    if( y == null ) {
                        if( ATermUtils.isLiteral( value ) )
                            throw new InternalReasonerException( 
                                "Object Property " + role + " is used with a hasValue restriction " +
                                "where the value is a literal: " + ATermUtils.toString( value ) );
                        else    
                            throw new InternalReasonerException( "Nominal " + c + " is not found in the KB!");
                    }
                    addEdge(x, role, y, ds);
                }
                else {
                    boolean useExistingNode = false;
                    boolean useExistingRole = false;
                    DependencySet maxCardDS = role.isFunctional() 
                        ? DependencySet.INDEPENDENT 
                        : x.hasMax1( role );
                    if( maxCardDS != null ) {
                        ds = ds.union( maxCardDS );
                        
                        if( !edges.isEmpty() )
                            useExistingRole = useExistingNode = true;
                        else {
                            Set fs = role.isFunctional() ? 
                                role.getFunctionalSupers() : 
                                role.getSubRoles();
                    	    for(Iterator it = fs.iterator(); it.hasNext(); ) {
                                Role f = (Role) it.next();
                                edges = x.getRNeighborEdges(f);
                                if( !edges.isEmpty() ) {
                                    if( useExistingNode ) {	                          
                                        Edge otherEdge = edges.edgeAt(0); 
                                        Node otherNode = otherEdge.getNeighbor(x);
                                        DependencySet d = 
                                            ds.union( edge.getDepends() )
                                              .union( otherEdge.getDepends() );
                                        mergeTo( y, otherNode, d );
                                    }
                                    else {
                                        useExistingNode = true;
                                        edge = edges.edgeAt(0);
                                        y = edge.getNeighbor(x);
                                    }
                                }
                            }
                            if( y != null )
                                y = y.getSame();
                        }
                    }

                    if(useExistingNode) {
                         ds = ds.union( edge.getDepends() );
                    }
                    else {
                        y = abox.addFreshIndividual();
                        y.depth = x.depth + 1;

                        if(x.depth >= abox.treeDepth)
                            abox.treeDepth = x.depth + 1;
                    }

                    if(ABox.DEBUG)
                        System.out.println("SOME: "
                            + x + " -> " + s + " -> " + y + " : " + c + 
                            (useExistingNode ? "" : " (*)")  + " - " + ds);

                    if( !useExistingRole ) addEdge(x, role, y, ds);

                    addType(y.getSame(), c, ds);
                }
            }

            if(abox.isClosed()) return;
        }
        x.applyNext[Individual.SOME] = size;
    }

    /**
     * apply max rule to the ABox
     *  
     */
    protected void applyMaxRule(Individual x) {
        if(!x.isChanged(Node.MAX)) return;

        List types = x.getTypes(Node.MAX);
        int size = types.size();
        for(int i = 0; i < size; i++) {
            ATermAppl mc = (ATermAppl) types.get( i );

            // max(r, n) is in normalized form not(min(p, n + 1))
            ATermAppl max = (ATermAppl) mc.getArgument(0);
            Role r = abox.getRole(max.getArgument(0));
            int n = ((ATermInt) max.getArgument(1)).getInt() - 1;

            DependencySet ds = x.getDepends(mc);

            if(n == 1)
                applyFunctionalMaxRule(x, r, ds);
            else {
                boolean hasMore = true;
                while(hasMore)
                    hasMore = applyMaxRule(x, r, n, ds);
            }

            if(abox.isClosed()) return;
        }

        x.setChanged(Node.MAX, false);
    }

    /**
     * 
     * applyMaxRule
     * 
     * @param x
     * @param r
     * @param k
     * @param ds
     * 
     * @return true if more merges are required for this maxCardinality
     */
    protected boolean applyMaxRule(Individual x, Role r, int k, DependencySet ds) {
        EdgeList edges = x.getRSuccessorEdges(r);
        // find all distinct R-neighbors of x
        Set neighbors = edges.getNeighbors(x);

        // if restriction was maxCardinality 0 then having any R-neighbor
        // violates
        // the restriction. no merge can fix this. compute the dependency and
        // return
        if(k == 0 && neighbors.size() > 0) {
            for(int e = 0; e < edges.size(); e++) {
                Edge edge = edges.edgeAt(e);
                ds = ds.union(edge.getDepends());
            }

            abox.setClash( Clash.maxCardinality( x, ds, r.getName(), 0 ) );

            return false;
        }

        // if there are less than n neighbors than max rule won't be triggered
        // return false beceuse no more merge required for this role
        if(neighbors.size() <= k) return false;

        // create the pairs to be merged
        List mergePairs = findMergeNodes( neighbors, x );

        // if no pairs were found, i.e. all were defined to be different from
        // each
        // other, to be merged then it means this max cardinality restriction is
        // violated. dependency of this clash is on all the neihbors plus the
        // dependency of the restriction type
        if(mergePairs.size() == 0) {
            DependencySet dsEdges = x.hasDistinctRNeighborsForMax(r, k + 1);
            if(dsEdges == null)
                return false;
            else {
                if(ABox.DEBUG)
                    System.out
                        .println("Early clash detection for max rule worked "
                            + x + " has more than " + k + " " + r + " edges "
                            + ds.union(dsEdges) + " " + neighbors);
                
                abox.setClash( Clash.maxCardinality( x, ds.union(dsEdges), r.getName(), 0 ) );

                return false;
            }
        }

        // add the list of possible pairs to be merged in the branch list
        MaxBranch newBranch = new MaxBranch(abox, this, x, r, k, mergePairs, ds);
        addBranch(newBranch);

        // try a merge that does not trivially fail
        if(newBranch.tryNext() == false) return false;

        if(ABox.DEBUG) abox.printTree();

        // if there were exactly n + 1 neighbors the previous step would
        // eliminate one node and only n neighbors would be left. This means
        // restriction is satisfied. If there were more than n + 1 neighbors
        // merging one pair would not be enough and more merges are required,
        // thus false is returned
        return neighbors.size() > k + 1;
    }
    
    protected void applyLiteralRule(Individual x) {
        Iterator succ = x.getSuccessors().iterator();
        while( succ.hasNext() ) {
            Node node = (Node) succ.next();
            
            if( node instanceof Individual )
                continue;
            
            Literal lit = (Literal) node;
            if( lit.getValue() != null )
                continue;

            LiteralValueBranch newBranch = new LiteralValueBranch(abox, this, lit, lit.getDatatype());
            addBranch(newBranch);

            newBranch.tryNext();            

            if(abox.isClosed()) return;
        }
    }
}