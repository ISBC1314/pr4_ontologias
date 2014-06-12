/*
 * Created on Aug 29, 2004
 */
package org.mindswap.pellet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mindswap.pellet.datatypes.ValueSpace;
import org.mindswap.pellet.exceptions.InternalReasonerException;
import org.mindswap.pellet.tbox.TBox;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.SetUtils;
import org.mindswap.pellet.utils.Timer;
import org.mindswap.pellet.utils.Timers;

import aterm.ATerm;
import aterm.ATermAppl;
import aterm.ATermInt;
import aterm.ATermList;

/**
 * A completion strategy specifies how the tableau rules will be applied to an ABox. Depending
 * on the expressivity of the KB, e.g. SHIN, SHON, etc., different (more efficient) strategies 
 * may be used. This class is the base for all different implementations and contains strategy
 * independent functions. 
 * 
 * @author Evren Sirin
 */
public abstract class CompletionStrategy {
    /**
     * ABox being completed
     */
    protected ABox abox;

    /**
     * Blocking method specific to this completion strategy
     */
    protected Blocking blocking;

    /**
     * Timers of the associated KB
     */
    protected Timers timers;
    
    /**
     * Mpa concepts to lazy nfolding results (already normalized)
     */
    protected Map unfoldingMap;
    
    /**
     * Timer to be used by the complete function. KB's consistency timer depends on this one
     * and this dependency is set in the constructor. Any concrete class that extends
     * CompletionStrategy should check this timer to respect the timeouts definde in the KB.
     */
    protected Timer completionTimer;     
    
    private boolean merging;
    
    protected List mergeList;
    
    /**
     *  
     */
    public CompletionStrategy(ABox abox, Blocking blocking) {
        this.abox = abox;
        this.blocking = blocking;
        this.timers = abox.getKB().timers;
        
        completionTimer = timers.createTimer("complete");
     }
    
    public void initialize() {     
        TBox tbox = abox.getKB().getTBox();

        unfoldingMap = tbox.getUnfoldingMap();
        
        mergeList = new ArrayList();
        
        for(Iterator i = abox.getBranches().iterator(); i.hasNext();) {
            Branch branch = (Branch) i.next();
            branch.setStrategy(this);            
        }

        if( abox.isInitialized() ) {
    		Iterator i = abox.getIndIterator();
    		while(i.hasNext()) {
    			Individual n = (Individual) i.next();

    			if( !n.isChanged( Node.ALL ) )
    			    continue;
    			
                applyAllValues(n);
                applyNominalRule(n);
    		}	
            
            return;
        }
            
        
        if(ABox.DEBUG) System.out.println("Initialize started");
        
		abox.setBranch( 0 );
        
		mergeList.addAll( abox.toBeMerged );
		
		if( !mergeList.isEmpty() ) mergeFirst();
		
		Iterator i = abox.getIndIterator();
        while( i.hasNext() ) {
            Individual n = (Individual) i.next();

            if( n.isMerged() ) continue;

            n.setChanged( true );

            abox.applyUC( n );
			
			EdgeList allEdges = n.getOutEdges();
			for(int e = 0; e < allEdges.size(); e++) {
				Edge edge = allEdges.edgeAt( e );
				
				if( edge.getTo().isPruned() )
				    continue;
				
                applyDomainRange( edge );
                applyAllValues( edge );
                applyFunctionalRole( edge );
                
                if( n.isMerged() ) break;
			}
		}	
		
        if( ABox.DEBUG ) System.out.println( "Merging: " + mergeList );

        if( !mergeList.isEmpty() ) mergeFirst();

        if( ABox.DEBUG ) System.out.println( "Initialize finished" );

        abox.setBranch( abox.getBranches().size() + 1 );
        abox.treeDepth = 1;
        abox.changed = true;
        abox.setComplete( false );
        abox.setInitialized( true );
    }
    
    private void mergeLater( Node y, Node z, DependencySet ds ) {
        mergeList.add( new NodeMerge( y, z, ds ) );
    }
    
    /**
     * return a complete ABox by applying all the tableau rules
     *  
     */
    abstract ABox complete();
    
    abstract boolean supportsPseudoModelCompletion();
	
    public void addType( Node node, ATermAppl c, DependencySet ds ) {
		node.addType( c, ds );
		
		if( c.getAFun().equals( ATermUtils.ANDFUN ) ){
			for(ATermList cs = (ATermList) c.getArgument(0); !cs.isEmpty(); cs = cs.getNext()) {
				ATermAppl conj = (ATermAppl) cs.getFirst();

			    addType( node, conj, ds );
			    
			    node = node.getSame();
			}
		}
		else if( c.getAFun().equals( ATermUtils.ALLFUN ) ) {
			applyAllValues( (Individual) node, c, ds);
		}	
//		else if( c.getAFun().equals( ATermUtils.VALUE ) ) {
//			applyNominalRule( (Individual) node, c, ds);
//		}	    
	}
        
	public void addEdge( Individual subj, Role pred, Node obj, DependencySet ds ) {
	    Edge edge = subj.addEdge( pred, obj, ds );
		if( edge != null ) {
			applyDomainRange( edge );
			applyAllValues( edge );
			applyFunctionalRole( edge );			
		}
	}


    /**
     * Apply the unfolding rule to every concept in every node.
     *  
     */
    protected void applyUnfoldingRule( IndividualIterator i ) {
        i.reset();
        while( i.hasNext() ) {
            Individual node = (Individual) i.next();

            applyUnfoldingRule( node );

            if( abox.isClosed() ) return;
        }
    }
    
    protected final void applyUnfoldingRule( Individual node ) {
        if( !node.canApply( Node.ATOM ) || blocking.isBlocked( node ) ) return;

        List types = node.getTypes( Node.ATOM );
        int size = types.size();
        for(int j = node.applyNext[Node.ATOM]; j < size; j++) {
            ATermAppl c = (ATermAppl) types.get( j );
            ATermAppl unfolded = (ATermAppl) unfoldingMap.get( c );

            if( unfolded != null ) {
                DependencySet ds = node.getDepends( c );

                if( ABox.DEBUG && !node.hasType( unfolded ) ) {
                    System.out.println( 
                        "UNF : " + node + ", " + c + " -> " + unfolded + " - " + ds );
                }

                addType( node, unfolded, ds );

                size = types.size();

                if( abox.isClosed() ) return;
            }
        }
        node.applyNext[Node.ATOM] = size;
    }
	
	void applyFunctionalRole( Edge edge ) {
	    Individual subj = edge.getFrom();
		Role r = edge.getRole();
		
        DependencySet maxCardDS = r.isFunctional() 
	        ? DependencySet.INDEPENDENT 
	        : subj.hasMax1( r );

		if( maxCardDS != null ) {
	        applyFunctionalMaxRule( subj, r, maxCardDS );    
		}
		
        if( r.isDatatypeRole() && r.isInverseFunctional() ) {
            Literal obj = (Literal) edge.getTo();

            applyFunctionalMaxRule( obj, r, DependencySet.INDEPENDENT );            
        }
        else if( r.isObjectRole() ) {
    	    Individual obj = (Individual) edge.getTo();
    		Role invR = r.getInverse();
    		
            maxCardDS = invR.isFunctional() 
    	        ? DependencySet.INDEPENDENT 
    	        : obj.hasMax1( invR );
            
            if( maxCardDS != null ) {
                applyFunctionalMaxRule( obj, invR, maxCardDS );
                
            // TODO try to fix the following commented part 
            // guessing rule for a functional property will create one
            // successor and then merge the node. the following code just 
            // turns the subj into a nominal but leaves it a predecessor 
//	            // if subj is merged to another node, get it
//	            subj = (Individual) subj.getSame();
//	            
//	            // if this is an edge from a blocakble node to a nominal
//	            // and the inverse property is functional then we need to
//	            // apply the guessing rule. for the functional case it is
//	            // just equivalent to make the blockable node nominal 
//	            if( subj.isBlockable() && obj.isNominal() ) {
//	                if(ABox.DEBUG)
//	                    System.out.println("GUEF: " + subj + " -> " + r + " -> " + obj );
//	                
//	                subj.setNominalLevel( 1 );
//	            }
            }
        }

	}

    protected void applyFunctionalMaxRule( Individual x, Role s, DependencySet ds ) {
	    Set functionalSupers = s.getFunctionalSupers();
	    if( functionalSupers.isEmpty() )
	        functionalSupers = SetUtils.singleton( s ); 
	    for(Iterator it = functionalSupers.iterator(); it.hasNext(); ) {
            Role r = (Role) it.next();
            
	        EdgeList edges = x.getRNeighborEdges( r );
	
	        // if there is not more than one edge then func max rule won't be triggered
	        if( edges.size() <= 1 ) continue;
	
	        // find all distinct R-neighbors of x
	        Set neighbors = edges.getFilteredNeighbors( x );
	        
	        // if there is not more than one neighbor then func max rule won't be triggered
	        if( neighbors.size() <= 1 ) continue;

	        Edge first = edges.edgeAt( 0 );
	        Node head = first.getNeighbor( x );
	        ds = ds.union( first.getDepends() );
            
	        for(int i = 1; i < edges.size(); i++) {
                Edge edge = edges.edgeAt( i );
                Node next = edge.getNeighbor( x );
                
                if( next.isPruned() ) continue;
                
                // it is possible that there are multiple edges to the same 
                // node, e.g. property p and its super property, so check if
                // we already merged this one
                if( head.isSame( next ) )
                    continue;
	            
                ds = ds.union( edge.getDepends() );
                
                if( next.isDifferent( head ) ) {
	                ds = ds.union( next.getDifferenceDependency( head ) );
	                if( r.isFunctional() )
	                    abox.setClash(Clash.functionalCardinality(x, ds, r.getName()));
	                else
	                    abox.setClash(Clash.maxCardinality(x, ds, r.getName(), 1));
	                
	                break;
	            }	           
                
                // always merge to a nominal (of lowest level) or an ancestor
                if( ( next.getNominalLevel() < head.getNominalLevel() ) ||                    
                    ( !head.isNominal() && next.hasSuccessor( x ) ) ) {
//              if( !head.isNominal() && ( next.isNominal() || next.hasSuccessor( x ) ) ) {
                	Node temp = head;
                    head = next;
                    next = temp;
                }                                
                
                if(ABox.DEBUG)
                    System.out.println("FUNC: " + x + " for prop "
                        + r + " merge " + next + " -> " + head + " " + ds);

                mergeTo( next, head, ds );

	            if(abox.isClosed()) return;

                if( head.isPruned() )
                    head = head.getSame();            	
	        }     
	    }
    }
	
    protected void applyFunctionalMaxRule( Literal x, Role r, DependencySet ds ) {
//        Set functionalSupers = s.getFunctionalSupers();
//        if( functionalSupers.isEmpty() )
//            functionalSupers = SetUtils.singleton( s ); 
//        for(Iterator it = functionalSupers.iterator(); it.hasNext(); ) {
//            Role r = (Role) it.next();
            
            EdgeList edges = x.getInEdges().getEdges( r );
    
            // if there is not more than one edge then func max rule won't be triggered
            if( edges.size() <= 1 ) return;//continue;
    
            // find all distinct R-neighbors of x
            Set neighbors = edges.getNeighbors( x );
            
            // if there is not more than one neighbor then func max rule won't be triggered
            if( neighbors.size() <= 1 ) return;//continue;

            Edge first = edges.edgeAt( 0 );
            Node head = first.getNeighbor( x );
            ds = ds.union( first.getDepends() );
            
            for(int i = 1; i < edges.size(); i++) {
                Edge edge = edges.edgeAt( i );
                Node next = edge.getNeighbor( x );
                
                if( next.isPruned() ) continue;
                
                // it is possible that there are multiple edges to the same 
                // node, e.g. property p and its super property, so check if
                // we already merged this one
                if( head.isSame( next ) )
                    continue;
                
                ds = ds.union( edge.getDepends() );
                
                if( next.isDifferent( head ) ) {
                    ds = ds.union( next.getDifferenceDependency( head ) );
                    if( r.isFunctional() )
                        abox.setClash(Clash.functionalCardinality(x, ds, r.getName()));
                    else
                        abox.setClash(Clash.maxCardinality(x, ds, r.getName(), 1));
                    
                    break;
                }              
                
                // always merge to a nominal (of lowest level) or an ancestor
                if( ( next.getNominalLevel() < head.getNominalLevel() ) ||                    
                    ( !head.isNominal() && next.hasSuccessor( x ) ) ) {
//              if( !head.isNominal() && ( next.isNominal() || next.hasSuccessor( x ) ) ) {
                    Node temp = head;
                    head = next;
                    next = temp;
                }                                
                
                if(ABox.DEBUG)
                    System.out.println("FUNC: " + x + " for prop "
                        + r + " merge " + next + " -> " + head + " " + ds);

                mergeTo( next, head, ds );

                if(abox.isClosed()) return;

                if( head.isPruned() )
                    head = head.getSame();              
            }     
//        }
    }

    
	void applyDomainRange(Edge edge) {
//        Timer timer = kb.timers.startTimer("applyDomainRange");
		Role r = edge.getRole();
		ATermAppl domain = r.getDomain();
		ATermAppl range = r.getRange();
			
		if(domain != null) {
		    Node from = edge.getFrom();
			if(ABox.DEBUG && !from.hasType(domain))
				System.out.println("DOM : " + edge.getTo() + " <- " + edge.getRole() + " <- " + 
				    edge.getFrom() + " : " + domain);
			addType(from, domain, edge.getDepends());
		}
		if(range != null) {
		    Node to = edge.getTo();
			if(ABox.DEBUG && !to.hasType(range))
				System.out.println("RAN : " + edge.getFrom() + " -> " + edge.getRole() + " -> " + 
				    edge.getTo() + " : " + range);
			addType(to, range, edge.getDepends());
		}
//        timer.stop();
	}

//	/**
//	 * Iterate through all the conjunctions and add the conjuncts (should only be
//	 * called after a restore because notmally)
//	 * 
//	 * @param x
//	 */
//	void applyConjunctions(Individual x) {
//		List types = x.getTypes(Node.AND);
//		Iterator i = types.iterator();
//		while(i.hasNext()) {
//			ATermAppl type = (ATermAppl) i.next();
//			DependencySet typeDepends = x.getDepends(type);
//			
//			for(ATermList cs = (ATermList) type.getArgument(0); !cs.isEmpty(); cs = cs.getNext()) {
//				ATermAppl conj = (ATermAppl) cs.getFirst();
//				
//			    x.addType( conj, typeDepends );
//			}
//		}
//	}
	
	/**
	 * Iterate through all the allValuesFrom restrictions on this individual and
	 * apply the restriction.
	 * 
	 * @param x
	 */
	void applyAllValues(Individual x) {
		List allValues = x.getTypes(Node.ALL);
		x.setChanged(Node.ALL, false);
		Iterator i = allValues.iterator();
		while(i.hasNext()) {
			ATermAppl av = (ATermAppl) i.next();	
			DependencySet avDepends = x.getDepends(av);
			
			applyAllValues(x, av, avDepends);		
			
			if(x.isMerged()) return;
			
			// if there are self links through transitive properties restart
			if(x.isChanged(Node.ALL)) {
			    i = allValues.iterator();
			    x.setChanged(Node.ALL, false);
			}
		}
	}
	
	/**
	 * Apply the allValues rule for the given type with the given dependency. The concept
	 * is in the form all(r,C) and this funciton adds C to all r-neighbors of x
	 * 
	 * @param x
	 * @param av
	 * @param ds
	 */
	void applyAllValues(Individual x, ATermAppl av, DependencySet ds) {
//	    Timer timer = kb.timers.startTimer("applyAllValues"); 
		
		Role s = abox.getRole( av.getArgument( 0 ) );
        ATermAppl c = (ATermAppl) av.getArgument( 1 );

        EdgeList edges = x.getRNeighborEdges( s );
        for(int e = 0; e < edges.size(); e++) {
            Edge edgeToY = edges.edgeAt( e );
            applyAllValues( x, s, c, edgeToY, ds );

            if( x.isMerged() ) return;
        }				
		
		if( !s.isSimple() ) {
            Set transitiveSubRoles = s.getTransitiveSubRoles();
            for(Iterator it = transitiveSubRoles.iterator(); it.hasNext();) {
                Role r = (Role) it.next();
                ATermAppl allRC = ATermUtils.makeAllValues( r.getName(), c );

                edges = x.getRNeighborEdges( r );
                for(int e = 0; e < edges.size(); e++) {
                    Edge edgeToY = edges.edgeAt( e );
                    applyAllValues( x, r, allRC, edgeToY, ds );

                    if( x.isMerged() ) return;
                }	
            }
        }		
		
//	    timer.stop();
	}
	
	void applyAllValues(Individual x, Role s, ATermAppl c, Edge edgeToY, DependencySet avDepends) {
		DependencySet ds = null;
        Node y = edgeToY.getNeighbor( x );

        if( !y.hasType( c ) ) {
            ds = avDepends.union( edgeToY.getDepends() );
            if( ABox.DEBUG )
                System.out.println( "ALL : " + x + " -> " + s + " -> " + y + " : " + c + " - " + ds );

            addType( y, c, ds );
        }	
	}	
	
	void applyAllValues( Edge edge ) {
//        Timer timer = kb.timers.startTimer("applyAllValuesEdge"); 

		Individual x = edge.getFrom();
		Role r = edge.getRole();

		List allValues = x.getTypes( Node.ALL );
        Iterator i = allValues.iterator();
        while( i.hasNext() ) {
            ATermAppl av = (ATermAppl) i.next();

            Role s = abox.getRole( av.getArgument( 0 ) );
            if( r.isSubRoleOf( s ) ) {
                ATermAppl c = (ATermAppl) av.getArgument( 1 );
                DependencySet ds = x.getDepends( av );

                applyAllValues( x, s, c, edge, ds );
                
        		if( s.isTransitive() ) {
                    ATermAppl allRC = ATermUtils.makeAllValues( s.getName(), c );

                    applyAllValues( x, s, allRC, edge, ds );
                }
            }
            
			// if there are self links through transitive properties restart
			if( x.isChanged( Node.ALL ) ) {
                i = allValues.iterator();
                x.setChanged( Node.ALL, false );
            }
        }

        if( r.isObjectRole() ) {
            Individual y = (Individual) edge.getTo();
            r = r.getInverse();
            allValues = y.getTypes( Node.ALL );
            i = allValues.iterator();
            while( i.hasNext() ) {                
                // if there are self links through transitive properties restart
                if( x.isChanged( Node.ALL ) ) {
                    i = allValues.iterator();
                    x.setChanged( Node.ALL, false );
                }
                
                ATermAppl av = (ATermAppl) i.next();

                Role s = abox.getRole( av.getArgument( 0 ) );
                if( r.isSubRoleOf( s ) ) {
                    ATermAppl c = (ATermAppl) av.getArgument( 1 );
                    DependencySet ds = y.getDepends( av );

                    applyAllValues( y, s, c, edge, ds );
                    
            		if( r.isTransitive() ) {
                        ATermAppl allRC = ATermUtils.makeAllValues( r.getName(), c );

                        applyAllValues( y, r, allRC, edge, ds );
                    }
                }
            }
        }
		
//        timer.stop();
	}
	
    /**
     * apply some values rule to the ABox
     *  
     */
    protected void applySomeValuesRule(IndividualIterator i) {
        i.reset();
        while( i.hasNext() ) {
            Individual x = (Individual) i.next();

//           x.setChanged(Node.SOME, false);

            if(!x.canApply(Individual.SOME) || blocking.isBlocked(x)) continue;

            List types = x.getTypes(Node.SOME);
            int size = types.size();
            loop: for(int j = x.applyNext[Node.SOME]; j < size; j++) {
                ATermAppl sv = (ATermAppl) types.get(j);

                // someValuesFrom is now in the form not(all(p. not(c)))
                ATermAppl a = (ATermAppl) sv.getArgument(0);
                ATermAppl s = (ATermAppl) a.getArgument(0);
                ATermAppl c = (ATermAppl) a.getArgument(1);

                Role role = abox.getRole(s);

                c = ATermUtils.negate(c);

                // Is there a r-neighbor that satisfies the someValuesFrom restriction
                boolean neighborFound = false;
                // Safety condition as defined in the SHOIQ algorithm. 
                // An R-neighbour y of a node x is safe if 
                //    (i) x is blockable or if 
                //   (ii) x is a nominal node and y is not blocked.
                boolean neighborSafe = x.isBlockable();
                // y is going to be the node we create, and edge its connection to the
                // current node
                Node y = null;
                Edge edge = null;
                
                
                // edges contains all the edges going into of coming out from the node
                // And labelled with the role R
                EdgeList edges = x.getRNeighborEdges( role );
                // We examine all those edges one by one and check if the neighbor has 
                // type C, in which case we set neighborFound to true
                for(int e = 0; e < edges.size(); e++) {
                    edge = edges.edgeAt(e);

                    y = edge.getNeighbor(x);

                    if( y.hasType( c ) ) {
                        neighborSafe |= y.isLiteral() || !blocking.isBlocked( (Individual) y );
                        if( neighborSafe ) {
                            neighborFound = true;
                            break;
                        }
                    }
                }

                // If we have found a R-neighbor with type C, continue, do nothing
                if( neighborFound ) continue;
                
                // If not, we have to create it 
                DependencySet ds = x.getDepends(sv).copy();
                
                // If the role is a datatype property...
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
                        abox.copyOnWrite();
                        
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
                        
                        if( y.isMerged() ) {
                            ds = ds.union( y.getMergeDependency( true ) );
                            
                            y = (Individual) y.getSame();
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
                            
                            // if there is an r-neighbor and we can have at most one r then
                            // we should reuse that node and edge. there is no way that neighbor
                            // is not safe (a node is unsafe only if it is blockable and has 
                            // a nominal successor which is not possible if there is a cardinality
                            // restriction on the poroperty)
                            if( edge != null )
                                useExistingRole = useExistingNode = true;
                            else {
                                // this is the tricky part. we need some merges to happen
                                // under following conditions:
                                //   1) if r is functional and there is a p-neighbor where
                                //      p is superproperty of r then we need to reuse that
                                //      p neighbor for the some values restriction (no
                                //      need to check subproperties because functionality of r
                                //      precents having two or more successors for subproperties)
                                //   2) if r is not functional, i.e. max(r, 1) is in the types,
                                //      then having a p neighbor (where p is subproperty of r) 
                                //      means we need to reuse that p-neighbor  
                                // In either case if there are more than one such value we also
                                // need to merge them together
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

                        if( useExistingNode ) {
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

                        addType( y, c, ds );
                        
                        if( !useExistingRole ) 
                            addEdge( x, role, y, ds );
                    }
                }

                if(abox.isClosed() || x.isMerged()) return;
            }
            x.applyNext[Individual.SOME] = size;
        }
    }
	

    /**
     * apply disjunction rule to the ABox
     *  
     */
    protected void applyDisjunctionRule(IndividualIterator i) {
        i.reset();
        while(i.hasNext()) {
            Individual node = (Individual) i.next();

            node.setChanged(Node.OR, false);

            if(!node.canApply(Node.OR) || blocking.isIndirectlyBlocked(node))
                continue;

            List types = node.getTypes(Node.OR);

            int size = types.size();
            ATermAppl[] disjunctions = new ATermAppl[size - node.applyNext[Node.OR]];
            types.subList(node.applyNext[Node.OR], size).toArray( disjunctions );
    	    if( PelletOptions.USE_DISJUNCTION_SORTING != PelletOptions.NO_SORTING ) 
    	        DisjunctionSorting.sort( node, disjunctions );
            
    	    LOOP:
            for(int j = 0, n = disjunctions.length; j < n; j++) {
                ATermAppl disjunction = disjunctions[j];
            
                // disjunction is now in the form not(and([not(d1), not(d2), ...]))
                ATermAppl a = (ATermAppl) disjunction.getArgument(0);
                ATermList disjuncts = (ATermList) a.getArgument(0);
                ATermAppl[] disj = new ATermAppl[disjuncts.getLength()];
 
                for(int index = 0; !disjuncts.isEmpty(); disjuncts = disjuncts.getNext(), index++) {
                    disj[index] = ATermUtils.negate((ATermAppl) disjuncts.getFirst());
                    if(node.hasType(disj[index])) continue LOOP;
                }

                DisjunctionBranch newBranch = new DisjunctionBranch(
                    abox, this, node, disjunction, node.getDepends(disjunction), disj);
                addBranch(newBranch);

                newBranch.tryNext();
                
                if(abox.isClosed() || node.isMerged() ) return;
            }
            node.applyNext[Node.OR] = size;
        }
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
    	
        EdgeList edges = x.getRNeighborEdges(r);
        // find all distinct R-neighbors of x
        Set neighbors = edges.getFilteredNeighbors(x);

        // if restriction was maxCardinality 0 then having any R-neighbor
        // violates the restriction. no merge can fix this. compute the 
        // dependency and return
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
        List mergePairs = findMergeNodes( neighbors, x);

        // if no pairs were found, i.e. all were defined to be different from
        // each other, to be merged then it means this max cardinality restriction is
        // violated. dependency of this clash is on all the neihbors plus the
        // dependency of the restriction type
        if( mergePairs.size() == 0 ) {
            DependencySet dsEdges = x.hasDistinctRNeighborsForMax(r, k + 1);
            if(dsEdges == null) {
                System.err.println("DEBUG: Cannot determine the exact clash dependency for " + x);
                abox.setClash(Clash.maxCardinality(x, ds));
                return false;
            }
            else {
                if(ABox.DEBUG)
                    System.out.println("Early clash detection for max rule worked "
                            + x + " has more than " + k + " " + r + " edges "
                            + ds.union(dsEdges) + " "
                            + x.getRNeighborEdges(r).getNeighbors(x));
                
                if(abox.doExplanation())
                    abox.setClash(Clash.maxCardinality(x, ds.union(dsEdges), r.getName(), k));
                else
                    abox.setClash(Clash.maxCardinality(x, ds.union(dsEdges)));

                return false;
            }
        }

        // add the list of possible pairs to be merged in the branch list
        MaxBranch newBranch = new MaxBranch(abox, this, x, r, k, mergePairs, ds);
        addBranch(newBranch);

        // try a merge that does not trivially fail
        if(newBranch.tryNext() == false) return false;

        // if there were exactly n + 1 neighbors the previous step would
        // eliminate one node and only n neighbors would be left. This means
        // restriction is satisfied. If there were more than n + 1 neighbors
        // merging one pair would not be enough and more merges are required,
        // thus false is returned
        return neighbors.size() > k + 1;
    }
  

    /**
     * apply max rule to the ABox
     *  
     */
    protected void applyMaxRule(IndividualIterator i) {
    	
        i.reset();
        while(i.hasNext()) {
            Individual x = (Individual) i.next();            
            if(!x.canApply(Individual.MAX) || blocking.isIndirectlyBlocked(x)) continue;
            
            List maxCardinality = x.getTypes(Node.MAX);            
            Iterator j = maxCardinality.iterator();
            while(j.hasNext()) {
                ATermAppl mc = (ATermAppl) j.next();

                // max(r, n) is in normalized form not(min(p, n + 1))
                ATermAppl max = (ATermAppl) mc.getArgument(0);
                
                Role r = abox.getRole(max.getArgument(0));
                int n = ((ATermInt) max.getArgument(1)).getInt() - 1;

                DependencySet ds = x.getDepends(mc);

                if(n == 1) {
                    applyFunctionalMaxRule(x, r, ds);
                    if(abox.isClosed()) return;
                }
                else {
                    boolean hasMore = true;
                    do {
                        hasMore = applyMaxRule(x, r, n, ds);


                        if(abox.isClosed() || x.isMerged()) return;

                        if(hasMore) {
                            // subsequent merges depend on the previous merge
                            ds = ds.union( new DependencySet( abox.getBranches().size() ) );
                        }
                        
                    } while(hasMore);
                }
            }
            
            x.setChanged(Node.MAX, false);
        }
    }

    /**
     * apply min rule to the ABox
     *  
     */
    protected void applyMinRule(IndividualIterator i) {
        i.reset();
        while(i.hasNext()) {
            Individual x = (Individual) i.next();

            if(!x.canApply(Node.MIN) || blocking.isBlocked(x)) continue;
            
            // We get all the minCard restrictions in the node and store
            // them in the list ''types''
            List types = x.getTypes(Node.MIN);
            int size = types.size();
            for(int j = x.applyNext[Node.MIN]; j < size; j++) {
                //mc stores the current type (the current minCard restriction)
            	ATermAppl mc = (ATermAppl) types.get(j);

                // We retrieve the role associated to the current
            	// min restriction
             	Role r = abox.getRole(mc.getArgument(0));
                int n = ((ATermInt) mc.getArgument(1)).getInt();

                // FIXME make sure all neighbors are safe
                if( x.hasDistinctRNeighborsForMin(r, n) ) continue;
              
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

                        if(x.depth >= abox.treeDepth)
                            abox.treeDepth = x.depth + 1;
                    }
                    addEdge(x, r, y[c1], ds);
                    for(int c2 = 0; c2 < c1; c2++)
                        y[c1].setDifferent(y[c2], ds);
                }
             
                if(abox.isClosed()) return;
            }
            x.applyNext[Node.MIN] = size;
        }
    }
    
    protected void applyLiteralRule() {
        Iterator i = new LiteralIterator(abox);
        while(i.hasNext()) {
            Literal lit = (Literal) i.next();

            if( lit.getValue() != null )
                continue;

            LiteralValueBranch newBranch = new LiteralValueBranch(abox, this, lit, lit.getDatatype());
            addBranch(newBranch);

            newBranch.tryNext();
            
            if(abox.isClosed()) return;
        }
    }
    
    protected void applyNominalRule(IndividualIterator i) {
//        boolean ruleApplied = true;
//        while(ruleApplied) {
//            ruleApplied = false;

            i.reset();
            while(i.hasNext()) {
                Individual y = (Individual) i.next();

                if(!y.canApply(Individual.NOM) || blocking.isBlocked(y))
                    continue;

                applyNominalRule(y);
 
                y.setChanged(Node.NOM, false);
                
                if(abox.isClosed()) return;
                
                if( y.isMerged() ) {
//                    ruleApplied = true;
                    applyNominalRule( (Individual) y.getSame() );
//                    break;
                }
            }
//        }
    }

    void applyNominalRule(Individual y) {
        if(PelletOptions.USE_PSEUDO_NOMINALS) return;
        
        List types = y.getTypes(Node.NOM);
        int size = types.size();
        for(int j = 0; j < size; j++) {
            ATermAppl nc = (ATermAppl) types.get(j);
            DependencySet ds = y.getDepends(nc);

            applyNominalRule( y, nc, ds);

            if( abox.isClosed() || y.isMerged() ) return;
        }
    }
	
    void applyNominalRule(Individual y, ATermAppl nc, DependencySet ds) {
        abox.copyOnWrite();
        
        ATerm nominal = nc.getArgument(0);
        // first find the individual for the given nominal
        Individual z = abox.getIndividual(nominal);
        if(z == null)
            throw new InternalReasonerException("Nominal " + nominal + " not found in KB!");
        // Get the value of mergedTo because of the following possibility:
        // Suppose there are three individuals like this
        // [x,{}],[y,{value(x)}],[z,{value(y)}]
        // After we merge x to y, the individual x is now represented by
        // the node y. It is too hard to update all the references of
        // value(x) so here we find the actual representative node
        // by calling getSame()
        if( z.isMerged() ) {
            // FIXME the following if was causing problem (why did we need it at all)
            //if( z.mergedAt() < abox.getBranch() ) 
            ds = ds.union( z.getMergeDependency( true ) );
            
            z = (Individual) z.getSame();
        }

        if( y.isSame( z ) ) 
            return;

        if( y.isDifferent( z ) ) {
            ds = ds.union( y.getDifferenceDependency( z ) );
            if( abox.doExplanation() )
                abox.setClash( Clash.nominal( y, ds, z.getName() ) );
            else
                abox.setClash( Clash.nominal( y, ds ) );
            return;
        }

        if( ABox.DEBUG ) 
            System.out.println( "NOM:  " + y + " -> " + z );

        mergeTo( y, z, ds );
    }
    
	private void mergeFirst() {
	    NodeMerge merge = (NodeMerge) mergeList.remove( 0 );
        
        Node y = abox.getNode( merge.y ).getSame();
        Node z = abox.getNode( merge.z ).getSame();

        if( y.isPruned() || z.isPruned() )
            return;
        
	    mergeTo( y, z, merge.ds );
	}
    
    public void mergeTo(Node y, Node z, DependencySet ds) {
	    if( y.isSame(z) )
	        return;
		else if( y.isDifferent( z ) ) {
			abox.setClash( Clash.nominal(y, y.getDifferenceDependency(z).union(ds)) );
			return;
		}
		
	    if( merging ) {
	        mergeLater( y, z, ds);
	        return;
	    }
	    else
	        merging = true;
	    
        if(ABox.DEBUG) System.out.println("MERG: " + y + " -> " + z + " " + ds);
        
        ds = ds.copy();
        ds.branch = abox.getBranch();
        
        if(y instanceof Literal && z instanceof Literal)
            mergeLiterals( (Literal) y, (Literal) z, ds );
        else if(y instanceof Individual && z instanceof Individual)
            mergeIndividuals( (Individual) y, (Individual) z, ds );
        else
            throw new InternalReasonerException( "Invalid merge operation!" );
        
        merging = false;
        
        if( !mergeList.isEmpty() ) {
            if( abox.isClosed() ) return;

            mergeFirst();
        }
    }
    

    private void mergeIndividuals(Individual y, Individual x, DependencySet ds) {       
		y.setSame(x, ds);

		// if both x and y are blockable x still remains blockable (nominal level
		// is still set to BLOCKABLE), if one or both are nominals then x becomes
		// a nominal with the minimum level
		x.setNominalLevel( Math.min( x.getNominalLevel(), y.getNominalLevel() ) );
		
		// copy the types
	    Map types = y.getDepends();
		for(Iterator yTypes = types.entrySet().iterator(); yTypes.hasNext(); ) {
		    Map.Entry entry = (Map.Entry) yTypes.next();
			ATermAppl yType = (ATermAppl) entry.getKey();
			DependencySet finalDS = ds.union( (DependencySet) entry.getValue() );
			
			addType( x, yType, finalDS );
		}	
		
		// for all edges (z, r, y) add an edge (z, r, x) 
		EdgeList inEdges = y.getInEdges();
		for(int e = 0; e < inEdges.size(); e++) {
			Edge edge = inEdges.edgeAt(e);
			
			Individual z = edge.getFrom();  
			Role r = edge.getRole();
			DependencySet finalDS = ds.union( edge.getDepends() );
			
			// if y has a self edge then x should have the same self edge
			if( y.equals( z ) ) {
			    addEdge( x, r, x, finalDS );
			}
			// if z is already a successor of x add the reverse edge
			else if( x.hasSuccessor( z ) ) {
			    // FIXME what if there were no inverses in this expressitivity
			    addEdge( x, r.getInverse(), z, finalDS );
			}
			else {
			    addEdge( z, r, x, finalDS );			    
			}
			
			// only remove the edge from z and keep a copy in y for a 
			// possible restore operation in the future 
			z.removeEdge( edge ); 
		}

		// we want to prune y early due to an implementation issue about literals
		// if y has an outoging edge to a literal with concrete value
		y.prune( ds );		

		// for all edges (y, r, z) where z is a nominal add an edge (x, r, z) 		
		EdgeList outEdges = y.getOutEdges();
		for(int e = 0; e < outEdges.size(); e++) {
			Edge edge = outEdges.edgeAt(e);			
			Node z = edge.getTo();  
			
			if( z.isNominal() && !y.equals( z ) ) {
				Role r = edge.getRole();
				DependencySet finalDS = ds.union( edge.getDepends() );
				
			    addEdge( x, r, z, finalDS );
			    
			    // do not remove edge here because prune will take care of that
			}
		}
		
		// for all z such that y != z set x != z  
		x.inheritDifferents( y, ds );
	}

    private void mergeLiterals(Literal y, Literal x, DependencySet ds) {
        y.setSame(x, ds);
        
        x.addAllTypes( y.getDepends(), ds );
        
        // for all edges (z, r, y) add an edge (z, r, x) 
        EdgeList inEdges = y.getInEdges();
        for(int e = 0; e < inEdges.size(); e++) {
            Edge edge = inEdges.edgeAt(e);
            
            Individual z = edge.getFrom();  
            Role r = edge.getRole();
            DependencySet finalDS = ds.union( edge.getDepends() );
            
            addEdge( z, r, x, finalDS );                            
            
            // only remove the edge from z and keep a copy in y for a 
            // possible restore operation in the future 
            z.removeEdge( edge ); 
        }
        
//		Edge edge = y.getInEdge();
//		Individual z = edge.getFrom();  
//
//		// only remove the edge from z and keep a copy in y for a 
//		// possible restore operation in the future 
//		z.removeEdge( edge ); 
//
//		// no need to add the edge (z, r, x) because only way these two literals
//		// can merge is if there is an edge (z, r, x)
//		if( !z.hasEdge( edge.getRole(), x ) )
//            throw new InternalReasonerException( "Cannot find expected edge " + edge);
		
        y.prune( ds );

        x.inheritDifferents( y, ds );
    }

    List findMergeNodes( Set neighbors, Individual node ) {
        Timer t = timers.startTimer("findMergeNodes"); 
        List pairs = new ArrayList();

        List nodes = new ArrayList(neighbors);
        for(int i = 0; i < nodes.size(); i++) {
            Node y = (Node) nodes.get(i);
            for(int j = i + 1; j < nodes.size(); j++) {
                Node x = (Node) nodes.get(j);

                if( y.isDifferent( x ) )
                    continue;

                // 1. if x is a nominal node (of lower level), then Merge(y, x) 
                if( x.getNominalLevel() <= y.getNominalLevel() )
                    pairs.add( new NodeMerge( y, x ) );
                // 2. if y is a nominal node or an ancestor of x, then Merge(x, y)
                else if( y.isNominal() )
                    pairs.add( new NodeMerge( x, y ) );
                // 3. if y is an ancestor of x, then Merge(x, y)
                else if( y.hasSuccessor( node ) )
                    pairs.add( new NodeMerge( x, y ) );
                // 4. else Merge(y, x)
                else
                    pairs.add( new NodeMerge( y, x ) );
                
//                if(y.isLeaf() && z.isLeaf())
//                    pairs.add(new NodeMerge(y, z));
//                else if(y.isRoot() && z.isRoot()) {
//                    if(z.isBnode())
//                        pairs.add(new NodeMerge(z, y));
//                    else
//                        pairs.add(new NodeMerge(y, z));
//                }
//                else if(!y.isRoot() && /*!y.isLeaf() &&*/ !z.hasAncestor((Individual) y))
//                    pairs.add(new NodeMerge(y, z));
//                else if(!z.isRoot() && /*!z.isLeaf() &&*/ !y.hasAncestor((Individual) z))
//                    pairs.add(new NodeMerge(z, y));
//                else
//                    System.err.println("DEBUG: Cannot determine how to merge nodes " + y + " " + z);
            }
        }
        t.stop();
        return pairs;
    }

	public void restore(Branch br) {
//	    Timers timers = abox.getKB().timers;
//		Timer timer = timers.startTimer("restore");
        
		abox.setBranch(br.branch);
		abox.setClash(null);
		abox.anonCount = br.anonCount;
		abox.rulesNotApplied = true;
		
		mergeList.clear();
		
		List nodeList = abox.getNodeNames();
		Map nodes = abox.getNodeMap();
		
		if(ABox.DEBUG) System.out.println("RESTORE: Branch " + br.branch);
		if(ABox.DEBUG && br.nodeCount < nodeList.size())
		    System.out.println("Remove nodes " + nodeList.subList(br.nodeCount, nodeList.size()));
		for(int i = 0; i < nodeList.size(); i++) {
			ATerm x = (ATerm) nodeList.get(i);
			
			Node node = abox.getNode(x);
			if(i >= br.nodeCount) 
				nodes.remove(x);
//			if(node.branch > br.branch) {
//				if(ABox.DEBUG) System.out.println("Remove node " + x);	
//				nodes.remove(x);
//				int lastIndex = nodeList.size() - 1;
//				nodeList.set(i, nodeList.get(lastIndex));
//				nodeList.remove(lastIndex);
//				i--;
//			}
			else
				node.restore(br.branch);
		}		
		nodeList.subList(br.nodeCount, nodeList.size()).clear();

		for(Iterator i = abox.getIndIterator(); i.hasNext(); ) {
			Individual ind = (Individual) i.next();
//			applyConjunctions(ind);			
			applyAllValues(ind);
//			applyNominalRule(ind);
		}
		
		if(ABox.DEBUG) abox.printTree();
		
		if(!abox.isClosed()) abox.validate();
			
//		timer.stop();
	}
	

//    /**
//     * check if there is a clash in any of the nodes in the tree
//     */
//    protected boolean hasClash() {
//        abox.setClash(null);
//
//        boolean hasClash = false;
//        Iterator i = abox.getIndIterator();
//        while(!hasClash && i.hasNext()) {
//            Individual node = (Individual) i.next();
//
//            if(node.isChanged() && !blocking.isBlocked(node)) hasClash = hasClash(node);
//
//            if(/*ABox.DEBUG && */hasClash)
//                System.out.println("found a clash in " + node.getName());
//        }
//
//        return hasClash;
//    }
//
//    /**
//     * check if there is a max clash
//     */
//    boolean hasClash(Individual node) {
//        ATerm x = node.getName();
//
//        if(node.isChanged(Node.MAX)) {
//            // max rule
//            for(Iterator i = node.getTypes(Node.MAX).iterator(); i.hasNext();) {
//                ATermAppl mc = (ATermAppl) i.next();
//
//                // max(r, n) is in normalized form not(min(p, n + 1))
//                ATermAppl a = (ATermAppl) mc.getArgument(0);
//                ATermAppl r = (ATermAppl) a.getArgument(0);
//                int n = ((ATermInt) a.getArgument(1)).getInt() - 1;
//
//                DependencySet ds = node.getDepends(mc);
//                //*****************************************
//                Role role = abox.getRole(r);
//                //******************************************
//                DependencySet dsEdges = node.hasDistinctRNeighborsForMax(role, n + 1);
//
//                if(dsEdges != null) {
//                    abox.setClash(Clash.maxCardinality(node, ds.union(dsEdges), r, n));
//
//                    if(ABox.DEBUG)
//                        System.out.println("3) Clash dependency all " + x
//                            + " has " + mc + " and more neighbors -> "
//                            + abox.getClash());
//
//                    return true;
//                }
//            }
//        }
//
//        return false;
//    }
    
    void addBranch(Branch newBranch) {
        abox.getBranches().add(newBranch);
//        System.out.println( "Add branch " + newBranch);

        if(newBranch.branch != abox.getBranches().size())
            throw new RuntimeException(
                "Invalid branch created!");

        completionTimer.check();
    }	

    void printBlocked() {
        int blockedCount = 0;
        String blockedNodes = "";
        Iterator n = abox.getIndIterator();
        while(n.hasNext()) {
            Individual node = (Individual) n.next();
            ATermAppl x = node.getName();

            if(blocking.isBlocked(node)) {
                blockedCount++;
                blockedNodes += x + " ";
            }
        }

        System.out.println("Blocked nodes " + blockedCount + " ["
                + blockedNodes + "]");
    }

    void checkDatatypeCount( IndividualIterator it ) {
        timers.startTimer( "clashDatatype" );

        it.reset();
        LOOP: while( it.hasNext() ) {
            Individual x = (Individual) it.next();

            if( !x.isChanged( Node.ALL ) && !x.isChanged( Node.MIN ) )
                continue;

            // for DatatypeProperties we have to compute the maximum number of 
            // successors it can have on the fly. This is because as we go on 
            // with completion there can be more concepts in the form all(dp, X) 
            // added to node label. so first for each datatype property collect
            // all the different allValuesFrom axioms together
            Map allValues = new HashMap();
            Map depends = new HashMap();
            for( Iterator i = x.getTypes( Node.ALL ).iterator(); i.hasNext(); ) {
                ATermAppl av = (ATermAppl) i.next();
                ATermAppl r = (ATermAppl) av.getArgument( 0 );
                ATermAppl c = (ATermAppl) av.getArgument( 1 );

                Role role = abox.getRole( r );
                if( !role.isDatatypeRole()) continue;

                DependencySet ds = (DependencySet) depends.get( r );

                List ranges = (List) allValues.get( r );
                if( ranges == null ) {
                    ranges = new ArrayList();
                    ds = DependencySet.EMPTY;
                }

                if( ATermUtils.isAnd( c ) ) {
                    ATermList types = (ATermList) c.getArgument( 0 );
                    for( ; types.isEmpty(); types = types.getNext() )
                        ranges.add( types.getFirst() );
                }
                else
                    ranges.add( c );

                ds = ds.union( x.getDepends( av ) );

                allValues.put( r, ranges );
                depends.put( r, ds );                    
            }
            
            for( Iterator i = x.getTypes( Node.MIN ).iterator(); i.hasNext(); ) {
                //mc stores the current type (the current minCard restriction)
                ATermAppl mc = (ATermAppl) i.next();
                ATermAppl r = (ATermAppl) mc.getArgument(0);
                Role role = abox.getRole( r );
                ATermAppl range = role.getRange();
                
                if( !role.isDatatypeRole() || range == null ) continue;
                
                List ranges = (List) allValues.get( r );
                if( ranges == null ) {
                    ranges = new ArrayList();
                    allValues.put( r, ranges );
                    depends.put( r, DependencySet.INDEPENDENT );
                }

                ranges.add( range );
            }

            for( Iterator i = allValues.keySet().iterator(); i.hasNext(); ) {
                ATerm r = (ATerm) i.next();
                Role role = abox.getRole( r );
                List ranges = (List) allValues.get( r );

                ATermAppl[] dt = new ATermAppl[ranges.size()];
                ranges.toArray( dt );

                timers.startTimer( "getMaxCount" );
                int n = abox.getDatatypeReasoner().intersection( dt ).size();
                timers.stopTimer( "getMaxCount" );

                if( n == ValueSpace.INFINITE || n == Integer.MAX_VALUE )
                    continue;
                
                boolean clash = x.checkMaxClash( ATermUtils.makeNormalizedMax( r, n ), DependencySet.INDEPENDENT );
                if( clash )
                    break LOOP;
                
                DependencySet dsEdges = x.hasDistinctRNeighborsForMax( role, n + 1 );

                if( dsEdges != null ) {
                    DependencySet ds = (DependencySet) depends.get( r );
                    ds = ds.union( dsEdges );

                    if( ABox.DEBUG )
                        System.out.println( "CLASH: literal restriction " + x + " has "
                            + ranges + " and more neighbors -> " + ds );

                    abox.setClash( Clash.unexplained( x, ds ) );

                    break LOOP;
                }
            }
        }
        timers.stopTimer( "clashDatatype" );
    }

    
    public String toString() {
	    String name = getClass().getName();
	    int lastIndex = name.lastIndexOf('.');
	    return name.substring(lastIndex + 1);
    }
}