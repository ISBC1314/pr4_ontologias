/*
 * Created on Jul 23, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.mindswap.pellet;

import java.util.List;

import org.mindswap.pellet.exceptions.InternalReasonerException;
import org.mindswap.pellet.utils.Timer;

import aterm.ATermAppl;
import aterm.ATermInt;

/**
 * @author Evren Sirin
 */
public class SHOINStrategy extends CompletionStrategy {
	 /**
     * @param abox
     * @param blocking
     */
    public SHOINStrategy(ABox abox) {
        super(abox, PelletOptions.FORCE_OPTIMIZED_BLOCKING 
            ? (Blocking) new OptimizedDoubleBlocking() 
            : (Blocking) new DoubleBlocking() );
    }
        
    boolean supportsPseudoModelCompletion() {
        return true;
    }        

    public void addEdge( Individual subj, Role pred, Node obj, DependencySet ds ) {
        Edge edge = subj.addEdge( pred, obj, ds );
        if( subj.isBlockable() && obj.isNominal() ) {
            if( obj.isLiteral() ) {
                if( pred.isInverseFunctional() ) {
                    subj.setNominalLevel( 1 );
                }                
            }
            else {
                Individual o = (Individual) obj;
                int max = o.getMaxCard( pred.getInverse() );
                if( max != Integer.MAX_VALUE ) {
                    int guessMin = o.getMinCard( pred.getInverse() );                
                    if( guessMin == 0 )
                        guessMin = 1;
                    
                    GuessBranch newBranch = new GuessBranch(abox, this, o, pred.getInverse(), guessMin, max, ds);
                    addBranch(newBranch);

                    // try a merge that does not trivially fail
                    if(newBranch.tryNext() == false) return;         
                    
                    if( abox.isClosed() ) return;                    
                }
            }            
        }
        
        if( edge != null ) {
            applyDomainRange( edge );
            applyAllValues( edge );
            applyFunctionalRole( edge );            
        }
    }
    
    protected void applyGuessingRule(IndividualIterator i) {    
    	i.reset();
        loop: while(i.hasNext()) {
            Individual x = (Individual) i.next();

            if ( x.isBlockable() ) continue;
            
            List types = x.getTypes( Node.MAX );
            int size = types.size();
            for(int j = 0; j < size; j++) {
                ATermAppl mc = (ATermAppl) types.get( j );

                // max(r, n) is in normalized form not(min(p, n + 1))
                ATermAppl max = (ATermAppl) mc.getArgument(0);
                
                Role r = abox.getRole(max.getArgument(0));                
                int n = ((ATermInt) max.getArgument(1)).getInt() - 1;
                
                // obviously if r is a datatype role then there can be no r-predecessor
                // and we cannot apply the rule
                if( r.isDatatypeRole() )
                    continue;
 
                // FIXME instead of doing the following check set a flag when the edge is added 
                // check that x has to have atleast one r neighbor y
                // which is blocakble and has successor x 
                // (so y is an inv(r) predecessor of x)
                boolean apply = false;
                EdgeList edges = x.getRPredecessorEdges( r.getInverse() );
                for( int e = 0; e < edges.size(); e++ ) {
                    Edge edge = edges.edgeAt( e );
                    Individual pred = edge.getFrom();
                	if( pred.isBlockable() ) {
                		apply = true;
                		break;
                	}
                }
                if( !apply ) continue;
                
                if( x.getMaxCard( r ) < n )
                    continue;

                if( x.hasDistinctRNeighborsForMin( r, n, true ) )
                    continue;
                
//                if( n == 1 ) {
//                    throw new InternalReasonerException( 
//                        "Functional rule should have been applied " +  
//                        x + " " + x.isNominal() + " " + edges);
//                }
                
                int guessMin = x.getMinCard( r );                
                if( guessMin == 0 )
                    guessMin = 1;
                
                // TODO not clear what the correct ds is so be pessimistic and include everything
                DependencySet ds = x.getDepends(mc);
                edges = x.getRNeighborEdges( r );
                for( int e = 0; e < edges.size(); e++ ) {
                    Edge edge = edges.edgeAt( e );
                    ds = ds.union( edge.getDepends() );
                }
                
                GuessBranch newBranch = new GuessBranch(abox, this, x, r, guessMin, n, ds);
                addBranch(newBranch);

                // try a merge that does not trivially fail
                if(newBranch.tryNext() == false) return;         
                
                if( abox.isClosed() ) return;
                
                if( x.isPruned() ) break loop;
            }
        }
    }
    
    protected void applyFunctionalGuessingRule(Individual x, Role r, DependencySet ds) {                  
        GuessBranch newBranch = new GuessBranch(abox, this, x, r, 1, 1, ds);
        addBranch(newBranch);

        // try a merge that does not trivially fail
        if(newBranch.tryNext() == false) return;         
        
        if( abox.isClosed() ) return;
    }
    
    protected boolean backtrack() {
        boolean branchFound = false;

        while(!branchFound) {
            completionTimer.check();

            int lastBranch = abox.getClash().depends.max();

            if(lastBranch <= 0)
                return false;
            else if(lastBranch > abox.getBranches().size())
                throw new InternalReasonerException(
                    "Backtrack: Trying to backtrack to branch " + lastBranch
                        + " but has only " + abox.getBranches().size()
                        + " branches");

            List branches = abox.getBranches();
            branches.subList(lastBranch, branches.size()).clear();
            Branch newBranch = (Branch) branches.get(lastBranch - 1);


            if(ABox.DEBUG)
                System.out.println("JUMP: Branch " + lastBranch);
            
            if(lastBranch != newBranch.branch)
                throw new InternalReasonerException(
                    "Backtrack: Trying to backtrack to branch " + lastBranch
                        + " but got " + newBranch.branch);

            if(newBranch.tryNext < newBranch.tryCount)
                newBranch.setLastClash( abox.getClash().depends );

            newBranch.tryNext++;

            if(newBranch.tryNext < newBranch.tryCount) {
                restore(newBranch);

                branchFound = newBranch.tryNext();
            }
            else
                abox.getClash().depends.remove(lastBranch);
            if(!branchFound) {
                if(ABox.DEBUG)
                    System.out.println("FAIL: Branch " + lastBranch);
            }
        }

        return branchFound;
    }

    ABox complete() {
        Timer t;
        
        completionTimer.start();

        Expressivity expressivity = abox.getKB().getExpressivity();        
        boolean fullDatatypeReasoning =
            PelletOptions.USE_FULL_DATATYPE_REASONING &&
            (expressivity.hasCardinalityD() || expressivity.hasKeys());
        
        initialize();
        
        while(!abox.isComplete()) {
            while(abox.changed && !abox.isClosed()) {                
                completionTimer.check();

                abox.changed = false;
               
                if(ABox.DEBUG) {
                    System.out.println("Branch: " + abox.getBranch() +
                        ", Depth: " + abox.treeDepth + ", Size: " + abox.getNodes().size() + 
                        ", Mem: " + (Runtime.getRuntime().freeMemory()/1000) + "kb");
                    abox.validate();
                    printBlocked();
                    abox.printTree();
                }

                IndividualIterator i = abox.getIndIterator();

                if( !PelletOptions.USE_PSEUDO_NOMINALS ) {
                    t = timers.startTimer( "rule-nominal");
	                applyNominalRule(i);
                    t.stop();
	                if(abox.isClosed()) break;
                }
                
                t = timers.startTimer("rule-guess");
                applyGuessingRule(i);
                t.stop();
                if(abox.isClosed()) break;
                
                t = timers.startTimer("rule-max");
                applyMaxRule(i);
                t.stop();
                if(abox.isClosed()) break;
                                
                if( fullDatatypeReasoning ) {
                    t = timers.startTimer("check-dt-count");
                    checkDatatypeCount(i);
                    t.stop();
                    if(abox.isClosed()) break;
    
                    t = timers.startTimer("rule-lit");
                    applyLiteralRule();
                    t.stop();
                    if(abox.isClosed()) break;
                }
                
                t = timers.startTimer("rule-unfold");
                applyUnfoldingRule(i);
                t.stop();
                if(abox.isClosed()) break;

                t = timers.startTimer("rule-disj");
                applyDisjunctionRule(i);
                t.stop();
                if(abox.isClosed()) break;
                
                t = timers.startTimer("rule-some");
                applySomeValuesRule(i);
                t.stop();
                if(abox.isClosed()) break;

                t = timers.startTimer("rule-min");
                applyMinRule(i);
                t.stop();
                if(abox.isClosed()) break;
                                
//                t = timers.startTimer("rule-max");
//                applyMaxRule(i);
//                t.stop();
//                if(abox.isClosed()) break;
//                
//                t = timers.startTimer("rule-lit");
//                applyLiteralRule();
//                t.stop();
//                if(abox.isClosed()) break;
            }

            if( abox.isClosed() ) {
                if(ABox.DEBUG)
                    System.out.println(
                        "Clash at Branch (" + abox.getBranch() + ") " + abox.getClash());

                if(backtrack())
                    abox.setClash( null );
                else
                    abox.setComplete( true );
            }
            else {
            	if (PelletOptions.SATURATE_TABLEAU) {
            		Branch unexploredBranch = null;
                	for (int i=abox.getBranches().size()-1; i>=0; i--) {
                		unexploredBranch = (Branch) abox.getBranches().get(i);
                        unexploredBranch.tryNext++;
                		if (unexploredBranch.tryNext < unexploredBranch.tryCount) {
                			restore(unexploredBranch);
                            System.out.println("restoring branch "+ unexploredBranch.branch + " tryNext = "+unexploredBranch.tryNext + " tryCount = "+unexploredBranch.tryCount);
                			unexploredBranch.tryNext();
                			break;
                		}
                		else { 
                            System.out.println("removing branch "+ unexploredBranch.branch);
                            abox.getBranches().remove(i);
                            unexploredBranch = null;
                        }
                	}
                	if(unexploredBranch == null) {
                		abox.setComplete( true );	
                	}
            	}
                else abox.setComplete( true );
            }
        }
        
        completionTimer.stop();

        return abox;
    }
//
//	public void restore(Branch br) {
////	    Timers timers = abox.getKB().timers;
////		Timer timer = timers.startTimer("restore");
//		
//		abox.setBranch(br.branch);
//		abox.setClash(null);
//		abox.anonCount = br.anonCount;
//		
//		mergeList.clear();
//		
//		List nodeList = abox.getNodeNames();
//		Map nodes = abox.getNodeMap();
//		
//		if(ABox.DEBUG) System.out.println("RESTORE: Branch " + br.branch);
//		if(ABox.DEBUG && br.nodeCount < nodeList.size())
//		    System.out.println("Remove nodes " + nodeList.subList(br.nodeCount, nodeList.size()));
//		for(int i = 0; i < nodeList.size(); i++) {
//			ATerm x = (ATerm) nodeList.get(i);
//			
//			Node node = abox.getNode(x);
//			if(i >= br.nodeCount) 
//				nodes.remove(x);
////			if(node.branch > br.branch) {
////				if(ABox.DEBUG) System.out.println("Remove node " + x);	
////				nodes.remove(x);
////				int lastIndex = nodeList.size() - 1;
////				nodeList.set(i, nodeList.get(lastIndex));
////				nodeList.remove(lastIndex);
////				i--;
////			}
//			else
//				node.restore(br.branch);
//		}		
//		nodeList.subList(br.nodeCount, nodeList.size()).clear();
//
//		for(Iterator i = abox.getIndIterator(); i.hasNext(); ) {
//			Individual ind = (Individual) i.next();
////			applyConjunctions(ind);			
//			applyAllValues(ind);
////			applyNominalRule(ind);
//		}
//		
//		if(ABox.DEBUG) abox.printTree();
//		
//		if(!abox.isClosed()) abox.validate();
//			
////		timer.stop();
//	}

}
