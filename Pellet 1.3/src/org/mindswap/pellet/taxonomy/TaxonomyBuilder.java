//The MIT License
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

package org.mindswap.pellet.taxonomy;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.Individual;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.SetUtils;
import org.mindswap.pellet.utils.Timer;
import org.mindswap.pellet.utils.URIUtils;

import aterm.ATermAppl;
import aterm.ATermList;

/*
 * Created on Aug 13, 2003
 *
 */

/**
 * @author Evren Sirin
 *
 */
public class TaxonomyBuilder {
    protected static Log log = LogFactory.getLog( Taxonomy.class );
    
	private byte PROPOGATE_UP = 1;
    private byte NO_PROPOGATE = 0;
    private byte PROPOGATE_DOWN = -1;
    
	protected Collection classes;
//	protected Set primitives;
	
	private Map toldDisjoints;
	
	private Map unionClasses;
	
	protected Taxonomy definitionOrder; 
	
	protected Taxonomy taxonomy; 
	protected KnowledgeBase kb;
	
	private int count;
	
	private ClassifyProgress listener = new SilentClassifyProgress();

	public TaxonomyBuilder( KnowledgeBase kb ) {
		this.kb = kb;
	}
	
	public void setListener(ClassifyProgress listener) {
	    if( listener == null )
	        this.listener = new SilentClassifyProgress();
	    else
	        this.listener = listener;
	}
	
	/**
	 * Classify the KB.
	 */
	public Taxonomy classify() {	
        classes = kb.getClasses();
        
		if( log.isInfoEnabled() ) {
		    kb.timers.createTimer("classifySub");
		    log.info("Classes: " + (classes.size()  + 2) + " Individuals: " + kb.getIndividuals().size());				
		}
		
		if( PelletOptions.SHOW_CLASSIFICATION_PROGRESS && (listener instanceof SilentClassifyProgress))
		    listener = new DefaultClassifyProgress();
		listener.classificationStarted( classes.size() );
		
		init();
		
		if( log.isInfoEnabled() ) {
		    log.info("Starting classification...");
		    //printStats(subclass);
		    //printArray(subclass, true);
		}				
		
		Iterator i = classes.iterator();		
		while( i.hasNext() ) {
			if( listener.isCanceled() ) {
			    listener.taskFinished();
			    return null;
			}
			
            ATermAppl c = (ATermAppl) i.next();		
            
            listener.startClass( getName( c ) );

			// early detection for TOP and BOTTOM
			if( !taxonomy.contains(c) ) 
			    classify( c );
		}				
		
		listener.taskFinished();

		if( log.isInfoEnabled() ) {
//			printArray(subclass);
//			printStats(subclass);
//			printStats(directSubclass);
			System.out.println("Sub Count: " + kb.timers.getTimer("classifySub").getCount());
			System.out.println("Sat Count: " + (kb.getABox().satisfiabilityCount - (2*kb.getClasses().size())));
		}
		
		return taxonomy;
	}

	private void init() {
//	    Timer timer = kb.timers.startTimer( "classify.init" );
		
	    count = 0;
        
        
        
        taxonomy = new Taxonomy();  
        
//      toldSubsumers = new HashMap();
//      toldEquivalents = new HashMap();
        toldDisjoints = new HashMap();        
	    
	    unionClasses = new HashMap();


//		System.out.println( 
//		    "GCI: " + (kb.getTBox().getUC()!=null) +
//		    " TOP: " + !kb.getTBox().getAxioms(ATermUtils.TOP).isEmpty() +
//		    " Domain: " + kb.getExpressivity().hasDomain() + 
//		    " Range: " + kb.getExpressivity().hasRange() +
//		    " Functional: " + kb.getExpressivity().hasFunctionality()
//		    );
	    
//	    boolean skipBottomSearch = (kb.getTBox().getUC() == null);	    
//	    primitives = skipBottomSearch ? new HashSet( classes ) : SetUtils.EMPTY_SET;
	    
	    // initialize the definitionOrder
	    definitionOrder = new Taxonomy();	    
	    TaxonomyNode top = definitionOrder.getTop();
	    TaxonomyNode bottom = definitionOrder.getBottom();
	    for(Iterator i = classes.iterator(); i.hasNext();) {
            ATermAppl c = (ATermAppl) i.next();	
	        TaxonomyNode node = definitionOrder.addNode( c );
	        top.addSub( node );
	        node.addSub( bottom );
	    }
	        
		// compute told subsumers for each concept
        List axioms = kb.getTBox().getAxioms();
        for(Iterator j = axioms.iterator(); j.hasNext();) {
            ATermAppl axiom = (ATermAppl) j.next();
            ATermAppl term1 = (ATermAppl) axiom.getArgument(0);
			ATermAppl term2 = (ATermAppl) axiom.getArgument(1);

            boolean equivalent = axiom.getName().equals( ATermUtils.SAME );
            
			preclassify( term1, term2, equivalent );
		}

        // additional step for union classes. for example, if we have
        //    C = or(A, B)
        // and both A nd B subclass of D then we can conclude C is also 
        // subclass of D
		for( Iterator i = unionClasses.keySet().iterator(); i.hasNext(); ) {
            ATermAppl c = (ATermAppl) i.next();
            ATermList disj = (ATermList) unionClasses.get( c );
            
            List lca = definitionOrder.computeLCA( disj );
            
            for( Iterator j = lca.iterator(); j.hasNext(); ) {
                ATermAppl d = (ATermAppl) j.next();
                
				if( log.isDebugEnabled() ) 
				    log.debug("Union subsumption " + getName(c) + " " + getName(d));
				
                addToldSubsumer( c, d );
            }
        }
		
		// we don't need this any more
		unionClasses = null;
		
		// classify the concepts in definition order
		classes = definitionOrder.topologocialSort();
		
//		if( Taxonomy.DETAILED_DEBUG ) definitionOrder.print();
		
//		timer.stop();
	}
	

	private void preclassify(ATermAppl c, ATermAppl d, boolean isSame) {
//        if( isSame ) 
//            primitives.remove( c );
//	    primitives.remove( d );
		if( !ATermUtils.isPrimitive( c ) ) {
			if(c.getAFun().equals(ATermUtils.ORFUN)) {
				ATermList list = (ATermList) c.getArgument(0);
				for( ATermList disj = list; !disj.isEmpty(); disj = disj.getNext()) {
				    ATermAppl e = (ATermAppl) disj.getFirst();
				    preclassify( e, d, false );
				}
			}
			else if(c.getAFun().equals(ATermUtils.NOTFUN)) {
			    if( ATermUtils.isPrimitive( d ) ) {
			        ATermAppl negation = (ATermAppl) c.getArgument(0);
								
					addToldDisjoint(d, negation);
					addToldDisjoint(negation, d);
				}
			}			    
		}
		else if( ATermUtils.isPrimitive( d ) ) {
		    if(d.getName().startsWith(PelletOptions.BNODE))
		        return;
		    
			if(!isSame) {			
				if( log.isDebugEnabled() ) 
				    log.debug("Preclassify (1) " + 
				        getName(c) + " " + 
				        getName(d));
	
				addToldSubsumer( c, d );
			}
			else {			
				if( log.isDebugEnabled() ) 
				    log.debug("Preclassify (2) " + 
				        getName(c) + " " + 
				        getName(d));
				
				addToldEquivalent(c, d);
			}
		}
		else if(d.getAFun().equals(ATermUtils.ANDFUN)) {
			for(ATermList conj = (ATermList) d.getArgument(0); !conj.isEmpty(); conj = conj.getNext()) {
			    ATermAppl e = (ATermAppl) conj.getFirst();
				preclassify( c, e, false );
			}
		}
//		else if(d.getAFun().equals(ATermUtils.SOMEFUN) || d.getAFun().equals(ATermUtils.MINFUN)) {
//		    ATermAppl p = (ATermAppl) d.getArgument( 0 );
//		    Set domains = kb.getDomains( p );
//		    for(Iterator i = domains.iterator(); i.hasNext();) {
//                ATermAppl e = (ATermAppl) i.next();
//                preclassify( c, e, false );
//            }
//		}		
		else if(d.getAFun().equals(ATermUtils.ORFUN)) {
		    boolean allPrimitive = true;
		    
			ATermList list = (ATermList) d.getArgument(0);
			for( ATermList disj = list; !disj.isEmpty(); disj = disj.getNext()) {
			    ATermAppl e = (ATermAppl) disj.getFirst();
				if( ATermUtils.isPrimitive( e ) ) {
					if( isSame ) {
						if( log.isDebugEnabled() ) 
						    log.debug("Preclassify (3) " + 
						        getName(c) + " " + 
						        getName(e));
						
					    addToldSubsumer( e, c );
					}
				}		
				else
				    allPrimitive = false;
			}
			
			if( allPrimitive )
			    unionClasses.put( c, list );
		}
        else if(d.equals(ATermUtils.BOTTOM)) {
            if( log.isDebugEnabled() ) 
                log.debug("Preclassify (4) " + 
                    getName(c) + " BOTTOM");
            addToldEquivalent( c, ATermUtils.BOTTOM );
        }
        else if(d.getAFun().equals(ATermUtils.NOTFUN)) {
			// handle case sub(a, not(b)) which implies sub[a][b] is false
			ATermAppl negation = (ATermAppl) d.getArgument(0);
			if(ATermUtils.isPrimitive(negation)) {
				if( log.isDebugEnabled() ) 
                    log.debug("Preclassify (5) " + 
				        getName(c) + " not " + 
				        getName(negation));
								
				addToldDisjoint(c, negation);
				addToldDisjoint(negation, c);
			}
		}		
	}

	private void addToldEquivalent(ATermAppl c, ATermAppl d) {
	    if( c.equals( d ) )
	        return;

	    TaxonomyNode cNode = definitionOrder.getNode( c );
		TaxonomyNode dNode = definitionOrder.getNode( d );

		definitionOrder.merge( cNode, dNode );
	}
	
	private void addToldSubsumer(ATermAppl c, ATermAppl d) {
	    if( c.equals( d ) )
	        return;

		TaxonomyNode cNode = definitionOrder.getNode( c );
		TaxonomyNode dNode = definitionOrder.getNode( d );
        if( cNode.equals( definitionOrder.getTop() ) )
            definitionOrder.merge( cNode, dNode );
        else {
    		dNode.addSub( cNode );
    		definitionOrder.removeCycles( cNode );
        }
	}
	
	private void addToldDisjoint(ATermAppl c, ATermAppl d) {
	    Set disjoints = (Set) toldDisjoints.get( c );
	    if( disjoints == null ) {
	        disjoints = new HashSet();
	        toldDisjoints.put( c, disjoints );
	    }
	    disjoints.add( d );
	}
	
	private void markToldSubsumers( ATermAppl c, Map marked ) {
	    TaxonomyNode node = taxonomy.getNode( c );
	    if( node != null ) {
	        boolean newMark = mark( node, marked, Boolean.TRUE, PROPOGATE_UP );
	        if( !newMark )
	            return;
	    }
	    else if( log.isInfoEnabled() && marked.size() > 2 )
	        log.warn( "Told subsumer " + c + " is not classified yet");
	    
	    if( definitionOrder.contains( c ) ) {
	        // TODO just getting direct supers and letting recursion handle rest 
	        // might be more efficient 
		    Set supers = definitionOrder.getSupers( c, true, true );
		    for(Iterator i = supers.iterator(); i.hasNext();) {
	            ATermAppl sup = (ATermAppl) i.next();
	            markToldSubsumers( sup, marked );
	        }
	    }	    	    
	}
	
	private void markToldSubsumeds( ATermAppl c, Map marked ) {
	    TaxonomyNode node = taxonomy.getNode( c );
	    if( node != null ) {
	        boolean newMark = mark( node, marked, Boolean.TRUE, PROPOGATE_DOWN );
	        if( !newMark )
	            return;
	        
		    Set subs = taxonomy.getSubs( c, true, true );
		    for(Iterator i = subs.iterator(); i.hasNext();) {
	            ATermAppl sub = (ATermAppl) i.next();
	            markToldSubsumeds( sub, marked );
	        }
	    }
	    
	    if( definitionOrder.contains( c ) ) {
		    Set subs = definitionOrder.getSubs( c, true, true );
		    for(Iterator i = subs.iterator(); i.hasNext();) {
	            ATermAppl sub = (ATermAppl) i.next();
	            markToldSubsumeds( sub, marked );
	        }
	    }	    	    
	}

	private void markToldDisjoints( ATermAppl c, Map marked ) {
		Set disjoints = (Set) toldDisjoints.get( c );
		if( disjoints != null ) {
			for(Iterator i = disjoints.iterator(); i.hasNext(); ) {
	            ATermAppl sup = (ATermAppl) i.next();
	    	    TaxonomyNode node = taxonomy.getNode( sup );
	    	    if( node != null && node.getName().equals( ATermUtils.BOTTOM ) )
	    	        mark( node, marked, Boolean.FALSE, PROPOGATE_DOWN );
	        }
		}	    
		
		Set supers = SetUtils.EMPTY_SET;
	    if( taxonomy.contains( c ) )
		    supers = taxonomy.getSupers( c, true, true );
	    else if( definitionOrder.contains( c ) ) 
		    supers = definitionOrder.getSupers( c, true, true );		    
		    
	    for(Iterator i = supers.iterator(); i.hasNext();) {
            ATermAppl sup = (ATermAppl) i.next();
            markToldDisjoints( sup, marked );
        }
	}
	
	private TaxonomyNode checkSatisfiability(ATermAppl c) {
        if( log.isDebugEnabled() ) 
            log.debug("Satisfiable ");	
	    
		Timer t = kb.timers.startTimer("classifySat");
		boolean isSatisfiable = kb.isSatisfiable(c);
		t.stop();

		if( log.isDebugEnabled() ) 
            log.debug((isSatisfiable?"true":"*****FALSE*****") + " (" + t.getLast() + "ms)");	
									
		if(!isSatisfiable) 
		    taxonomy.addEquivalentNode( c, taxonomy.getBottom() );		    
		
		if(PelletOptions.USE_CACHING) {
            if( log.isDebugEnabled() ) 
                log.debug("...negation ");	
			    
			t.start();
		    isSatisfiable = kb.isSatisfiable( ATermUtils.makeNot( c ) );
			t.stop();

			if(!isSatisfiable) 
			    taxonomy.addEquivalentNode( c, taxonomy.getTop() );
			
			if( log.isDebugEnabled() ) 
                log.debug(isSatisfiable + " (" + t.getLast() + "ms)");				
		}	
		
		return taxonomy.getNode( c );
	}

	public void classify(ATermAppl c) {
	    TaxonomyNode node = classify( c, !ATermUtils.isPrimitive( c ) );
	    
	    // if c is a complex class expression not a named class then it is not in definitionOrder
	    if( definitionOrder.contains( c ) ) {
		    TaxonomyNode defOrder = definitionOrder.getNode( c );
		    for(Iterator i = defOrder.getEquivalents().iterator(); i.hasNext();) {
	            ATermAppl eq = (ATermAppl) i.next();
	            taxonomy.addEquivalentNode( eq, node );
	        }
	    }
	}
	
	private TaxonomyNode classify( ATermAppl c, boolean hide ) {
		if( log.isInfoEnabled() ) 
            log.info("Classify (" + (++count) + ") " + getName(c) + "...");	
		
	    TaxonomyNode node = checkSatisfiability( c );	
	    
	    if( node != null ) return node;

		Map marked = new HashMap();
		mark( taxonomy.getTop(), marked, Boolean.TRUE, NO_PROPOGATE );
		mark( taxonomy.getBottom(), marked, Boolean.FALSE, NO_PROPOGATE );
		markToldSubsumers( c, marked );
		markToldDisjoints( c, marked );
		
		log.debug("Top search...");
	    
		Collection supers = search(true, c, taxonomy.getTop(), new HashSet(), new ArrayList(), marked);
				
		marked = new HashMap();		
		mark( taxonomy.getTop(), marked, Boolean.FALSE, NO_PROPOGATE );
		mark( taxonomy.getBottom(), marked, Boolean.TRUE, NO_PROPOGATE );
		markToldSubsumeds( c, marked );
		markToldDisjoints( c, marked );
		
		if(supers.size() == 1) {
		    TaxonomyNode sup = (TaxonomyNode) supers.iterator().next();
			
			// if i has only one super class j and j is a subclass 
			// of i then it means i = j. There is no need to classify
			// i since we already know everything about j
			if( subsumed( sup, c, marked ) ) {
				if( log.isInfoEnabled() ) 
                    log.info(
				        getName(c) + " = " + 
				        getName(sup.getName()));	

				taxonomy.addEquivalentNode( c, sup );
				return sup;
			}
		}
		
		log.debug("Bottom search...");

		Collection subs = null;
		
//		if( primitives.contains( c ) ) 
//		    subs = Collections.singleton( taxonomy.getBottom() );
//		else
		    subs = search(false, c, taxonomy.getBottom(), new HashSet(), new ArrayList(), marked);
		
//		Set copy = new HashSet( subs );
//		copy.remove( taxonomy.getBottom() );
//		if( !copy.isEmpty() ) {
//		    boolean domain = false;
//		    for(Iterator i = kb.getProperties().iterator(); i.hasNext();) {
//                ATermAppl p = (ATermAppl) i.next();
//                Set dom = kb.getDomains(p);
//                domain |= dom.contains( c );
//                domain |= kb.getRanges(p).contains(c);
//            }
//		    System.out.println( c + " primitives:" + primitives.contains(c) + " isPrimitive:" + kb.getTBox().isPrimitive(c) + " domain:" + domain + " subs " + copy );
//		}
		
		node = taxonomy.addNode( c, hide );
		node.addSupers( new ArrayList( supers ) ) ;
		node.addSubs( new ArrayList( subs ) );
		
		node.removeMultiplePaths();

		if( log.isDebugEnabled() ) 
            log.debug("Subsumption Count: " + kb.getABox().satisfiabilityCount);
		//if(DEBUG) printArray(subclass, true);
		
		return node;
	}	
	
	private Collection search(boolean topSearch, ATermAppl c, TaxonomyNode x, Set visited, List result, Map marked) {
		List posSucc = new ArrayList();
		visited.add(x);
		
		List list = topSearch ? x.getSubs() : x.getSupers();
		for(int i = 0; i < list.size(); i++) {
		    TaxonomyNode next = (TaxonomyNode) list.get(i);

		    if( topSearch ) {
				if( subsumes( next, c, marked ) )
					posSucc.add( next );
		    }
		    else {
				if( subsumed( next, c, marked ) )
					posSucc.add( next );
		    }
		}

		if( posSucc.isEmpty() ) {
            result.add( x );
        }
        else {
            for(Iterator i = posSucc.iterator(); i.hasNext();) {
                TaxonomyNode y = (TaxonomyNode) i.next();
                if( !visited.contains( y ) ) 
                    search( topSearch, c, y, visited, result, marked );
            }
        }
				
		return result;
	}


	private boolean subsumes(TaxonomyNode node, ATermAppl c, Map marked) {	
	    Boolean cached = (Boolean) marked.get( node ); 
	    if( cached != null )
	        return cached.booleanValue();
		
		// check subsumption
		boolean subsumes = subsumes( node.getName(), c );
		// create an object based on result
		Boolean value = subsumes ? Boolean.TRUE : Boolean.FALSE;
		// during top search only negative information is propogated down
		byte propogate = subsumes ? NO_PROPOGATE : PROPOGATE_DOWN;
		// mark the node appropriately
		mark( node, marked, value, propogate);
		
		return subsumes;
	}

	private boolean subsumed(TaxonomyNode node, ATermAppl c, Map marked) {		
	    Boolean cached = (Boolean) marked.get( node ); 
	    if( cached != null )
	        return cached.booleanValue();

	    // check subsumption
		boolean subsumed = subsumes( c, node.getName() );
		// create an object based on result
		Boolean value = subsumed ? Boolean.TRUE : Boolean.FALSE;
		// during bottom search only negative information is propogated down
		byte propogate = subsumed ? NO_PROPOGATE : PROPOGATE_UP;
		// mark the node appropriately
		mark( node, marked, value, propogate);
		
		return subsumed;
	}
	
	private boolean mark(TaxonomyNode node, Map marked, Boolean value, byte propogate) {
	    if( node.getEquivalents().contains( ATermUtils.BOTTOM ) )
	        return true;
	    
	    Boolean exists = (Boolean) marked.get( node );
	    if( exists != null ) {
	        if( exists != value )
	            throw new RuntimeException("Inconsistent classification result " + 
	                node.getName() + " " + exists + " " + value);
	        else 
	            return false;
	    }
	    marked.put( node, value );
	    
	    if( propogate != NO_PROPOGATE ) {
		    List others = (propogate == PROPOGATE_UP) ? node.getSupers() : node.getSubs();
		    for(Iterator i = others.iterator(); i.hasNext();) {
	            TaxonomyNode next = (TaxonomyNode) i.next();
	            mark( next, marked, value, propogate );
	        }
	    }
	    
	    return true;
	}
	
	private boolean subsumes(ATermAppl sup, ATermAppl sub) {
		long time = 0, count = 0;
	    if( log.isDebugEnabled() ) {
			time = System.currentTimeMillis();
	        count = kb.getABox().satisfiabilityCount;
	        log.debug("Subsumption testing for [" + 
	            getName(sub) + "," + 
	            getName(sup) + "]...");
	    }
	    
	    Timer t = kb.timers.startTimer("classifySub");
	    boolean result = kb.getABox().isSubClassOf(sub, sup);
		t.stop();

		if( log.isDebugEnabled() ) {
		    String sign = (kb.getABox().satisfiabilityCount > count)  ? "+" : "-";
			time = System.currentTimeMillis() - time;
		    log.debug(" done (" + (result ? "+" : "-") + ") (" + sign + time + "ms)");	
		    
//		    if( sign.equals("+") && !result) {
//		        System.out.println("DEBUG");
//		        kb.getABox().isSubClassOf(sub, sup);
//		    }
		}		
		
		return result;
	} 
			
	private void mark( Set set, Map marked, Boolean value ) {
        for(Iterator i = set.iterator(); i.hasNext(); ) {
            ATermAppl c = (ATermAppl) i.next();
            
            marked.put( c, value );
        }
	}

	
	/**
	 * Realize the KB by finding the instances of each class.
	 */
	public Taxonomy realize() {		
//		if( PelletOptions.SHOW_CLASSIFICATION_PROGRESS && (listener instanceof SilentClassifyProgress))
//		    listener = new DefaultClassifyProgress();
	    listener.realizationStarted( kb.getIndividuals().size() );
	    
		Iterator i = kb.getABox().getIndIterator();
		for(int count = 0; i.hasNext(); count++) {
			if( listener.isCanceled() ) {
			    listener.taskFinished();
			    return null;
			}
			
			Individual x = (Individual) i.next();
			            
            listener.startIndividual( getName( x.getName() ) );
            
			if( log.isInfoEnabled() ) 
                log.info(count + ") Realizing " + getName(x.getName()) + " ");

			Map marked = new HashMap();
			
			List obviousTypes = new ArrayList();
			List obviousNonTypes = new ArrayList();
			
			kb.getABox().getObviousTypes( x.getName(), obviousTypes, obviousNonTypes );
						
			for(Iterator j = obviousTypes.iterator(); j.hasNext();) {
                ATermAppl c = (ATermAppl) j.next();
                
                // since nominals can be returned by getObviousTypes 
                // we need the following check
                if( !taxonomy.contains( c ) ) continue;
                
                mark( taxonomy.getAllEquivalents( c ), marked, Boolean.TRUE );
                mark( taxonomy.getSupers( c, true, true ), marked, Boolean.TRUE );
                
                markToldDisjoints( c, marked );
            }
			
			for(Iterator j = obviousNonTypes.iterator(); j.hasNext();) {
                ATermAppl c = (ATermAppl) j.next();
                               
                mark( taxonomy.getAllEquivalents( c ), marked, Boolean.FALSE );
                mark( taxonomy.getSubs( c, true, true ), marked, Boolean.FALSE );
            }			
			
			realize( x.getName(), ATermUtils.TOP, marked );
		}		
		
		listener.taskFinished();
		
		return taxonomy;
	}
	
	private boolean realize( ATermAppl n, ATermAppl c, Map marked ) {
		boolean realized = false;
		
		if(c.equals(ATermUtils.BOTTOM)) return false;
    
	    boolean isType;
	    if( marked.containsKey( c ) ) {
	        isType = ((Boolean) marked.get( c )).booleanValue(); 
	    }
	    else {			
			long time = 0, count = 0;
		    if( log.isDebugEnabled() ) {
				time = System.currentTimeMillis();
		        count = kb.getABox().consistencyCount;
		        log.debug("Type checking for [" + 
		            getName(n) + ", " + 
		            getName(c) + "]...");	
		    }
		    
		    Timer t= kb.timers.startTimer("classifyType");
		    isType = kb.isType(n, c);
			t.stop();
			marked.put( c,  isType ? Boolean.TRUE : Boolean.FALSE );			
		    
			if( log.isDebugEnabled() ) {
			    String sign = (kb.getABox().consistencyCount > count)  ? "+" : "-";
				time = System.currentTimeMillis() - time;
			    log.debug(" done (" + (isType ? "+" : "-") + ") (" + sign + time + "ms)");	
			}		    
		}

		
		if( isType ) {
		    TaxonomyNode node = taxonomy.getNode( c );
			
			Iterator subs = node.getSubs().iterator();
			while(subs.hasNext()) {
			    TaxonomyNode sub = (TaxonomyNode) subs.next();
				ATermAppl d = sub.getName();
					
				realized = realize(n, d, marked) || realized; 
			}
			
			// this concept is the most specific concept x belongs to
			// so add it here and return true 
			if( !realized ) {	
				taxonomy.getNode( c ).addInstance( n );
				realized = true;
			}
		}
		
		return realized;
	}


	public void printStats() {
//		printStats(subclass);
	    int numClasses = classes.size();
		System.out.println("Num of Classes: " + numClasses + " Pairs: " + (numClasses*numClasses) + " Subsumption Count: " + kb.getABox().satisfiabilityCount);		
	}


	private String getName(ATermAppl c) {
		if(c.equals(ATermUtils.TOP))		    
			return "owl:Thing";
		else if(c.equals(ATermUtils.BOTTOM))
			return "owl:Nothing";
		else if(ATermUtils.isPrimitive(c))
			return URIUtils.getLocalName(c.getName());
        else
            return c.toString();
	}
	
//	/**
//	 * Print the subclass table with ?, + , - characters. For debugging
//	 * purposes
//	 */
//	private void printArray(byte[][] subclass, boolean all) {
//		int known = 0;
//		System.out.println();
//		for(int i = 0; i < numClasses; i++) {
//			if(!all && !classified[i]) continue;
//			StringBuffer name = new StringBuffer(i + " " + getClassName(i));
//			name.append("              ");
//			name.setLength(15); 
//			name.append(" ");
//			System.out.print(name.toString());
//			for(int j = 0; j < numClasses; j++) { 
//				if(!all && !classified[j]) continue;
//				System.out.print(chars[subclass[i][j]] + " ");
//				if(subclass[i][j] != UNKNOWN)
//					known++;
//			}
//			System.out.println("");		
//		}		
//		System.out.println("Known: " + known);		
//	}

//	private void printStats(byte[][] subclass) {
//		int size = numClasses*numClasses;
//		int known = 0;
//		int positive = 0;
//		
//		for(int i = 0; i < numClasses; i++) {
//			for(int j = 0; j < numClasses; j++) { 
//				if(subclass[i][j] != UNKNOWN)
//					known++;
//				if(subclass[i][j] == YES)
//					positive++;					
//			}
//		}		
//		System.out.println("Size: " + size + " Known: " + known + " Positive: " + positive);		
//	}	

}
