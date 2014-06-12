/*
 * Created on Aug 16, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.mindswap.pellet.query.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.Individual;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.query.Query;
import org.mindswap.pellet.query.QueryEngine;
import org.mindswap.pellet.query.QueryExec;
import org.mindswap.pellet.query.QueryPattern;
import org.mindswap.pellet.query.QueryResults;

import aterm.ATermAppl;

/**
 * @author Daniel
 *
 */
public class RelaxedDistVarsQueryEngine implements QueryExec {
    public static Log log = LogFactory.getLog( QueryEngine.class );
    
    public RelaxedDistVarsQueryEngine() {        
    }
    
	public boolean supports( Query q ) {
		for ( Iterator v = q.getVars().iterator(); v.hasNext(); )  {
			ATermAppl curr = (ATermAppl) v.next();
			
			if ( !q.getDistVars().contains( curr ) && !q.findPatterns( curr, null, null ).isEmpty() ) {
				return false;
			}
		}
		
		return true;
	}
	
	public QueryResults exec( Query q ) {
	    QueryResults results = new QueryResultsImpl( q );
	    KnowledgeBase kb = q.getKB();
	    
		List master = new ArrayList();
		HashMap classMap = new HashMap();
		
		for ( Iterator i = q.getDistObjVars().iterator(); i.hasNext(); ) {
			ATermAppl x = (ATermAppl) i.next();
			
			List out = q.findPatterns( x, null, null ); 
			
			for ( Iterator t = out.iterator(); t.hasNext(); ) {
				QueryPattern triple = (QueryPattern) t.next();
				
				if ( !q.getLitVars().contains( triple.getObject() ) ) {
					master.add( triple );
				}
			}
			
			classMap.put( x, q.getClasses( x ) );
		}
		
		for ( Iterator i = q.getConstants().iterator(); i.hasNext(); ) {
			ATermAppl x = ( (Individual) i.next() ).getName();
			classMap.put( x, q.getClasses( x ) );
		}
		
        if( log.isTraceEnabled() )
            log.trace( "Here is the class Map: " + classMap );
		
		ATermAppl starter = (ATermAppl) q.getDistObjVars().iterator().next();
		Collection initial = kb.getInstances( q.rollUpTo( starter ) );
		
		HashMap edgeMapping = new HashMap();
		HashMap varBindings = new HashMap();
		
		varBindings.put( starter, initial );
//		List varOrder = new ArrayList();
//		
//		for ( Iterator e = master.iterator(); e.hasNext(); ) {
//			QueryPattern curr = (QueryPattern) e.next();
//			List currList = new ArrayList();
//			
//			if ( !varOrder.contains( curr.getSubject() ) ) {
//				varOrder.add( curr.getSubject() );
//			}
//			
//			//System.out.println( starter + " " + curr.getSubject() + " " + curr.getSubject().equals( starter ) );
//			if ( curr.getSubject().equals( starter ) ) {
//				if ( q.getLitVars().contains( curr.getObject() ) ) {
//					//System.out.println( "A PROBLEM" );
//					// TODO no code yet to handle this
//				} else {
//					for ( Iterator i = initial.iterator(); i.hasNext(); ) {
//						ATermAppl x = (ATermAppl) i.next();
//						//System.out.println( "Checking " + x );
//					
//						if ( !varOrder.contains( curr.getObject() ) ) {
//							varOrder.add( curr.getObject() );
//						}
//					
//						ATermAppl r = curr.getPredicate();
//						List succ = kb.getPropertyValues( r, x );
//						if ( succ.isEmpty() ) {
//							//System.out.println( x + " has no " + curr.role + " successors." );
//							i.remove();
//						} else {
//							for ( Iterator s = succ.iterator(); s.hasNext(); ) {
//								ATermAppl y = (ATermAppl) s.next();
//								ATermList classes = (ATermList) classMap.get( curr.getObject() );
//							
//								//System.out.println( "Classes: " + classes );
//							
//								// TODO make the classMap hold constants and their class restrictions
//								boolean classCheck = true;
//								for ( ATermList list = classes; list != null && !list.isEmpty(); list = list.getNext()) {
//									ATermAppl c = (ATermAppl) list.getFirst();
//									//System.out.println( c );
//
//									if ( !kb.isType( y, c ) ) {
//										classCheck = false;
//										break;
//									}
//								}
//							
//								if ( classCheck ) {
//									// prevents accidently expanding the set to consider
//									if ( !varBindings.keySet().contains( curr.getObject() ) ) {
//										ArrayList temp = new ArrayList();
//										temp.add( y );
//										varBindings.put( curr.getObject(), temp );
//									} else {
//										((ArrayList) varBindings.get( curr.getObject() )).add( y );
//									}
//								
//									//System.out.println( "Adding edge mapping..." );
//									currList.addEdge( edge );
//								}
//							}
//						}
//					}	
//				}
//			} else {
//				//System.out.println( varBindings.get( curr.getSubject() ) );
//				List b = (List) varBindings.get( curr.getSubject() );
//				
//				if ( b == null ) {
//					System.err.println( "Query Failed: could not bind variable: " + curr.getSubject() );
//					// TODO this is wrong, we should instead use get subjects, etc. like everywhere else!?
//					return results;
//				} else {
//					for ( Iterator i = b.iterator(); i.hasNext(); ) {
//						ATermAppl x = (ATermAppl) i.next();
//						//System.out.println( "Checking " + x );
//
//						if ( !varOrder.contains( curr.getObject() ) ) {
//							varOrder.add( curr.getObject() );
//						}
//
//						ATermAppl r = curr.getPredicate();
//						List succ = kb.getPropertyValues( r, x );
//						if ( succ.isEmpty() ) {
//							i.remove();
//						} else {
//							for ( Iterator s = succ.iterator(); s.hasNext(); ) {
//								ATermAppl y = (ATermAppl) s.next();
//								
//								if ( kb.isIndividual( y ) ) {
//									//System.out.println( curr.getObject() + "\n" + classMap );
//									ATermList classes = (ATermList) classMap.get( curr.getObject() );
//							
//									//System.out.println( "Classes: " + classes );
//
//									boolean classCheck = true;
//									for ( ATermList list = classes; list != null && !list.isEmpty(); list = list.getNext()) {
//										ATermAppl c = (ATermAppl) list.getFirst();
//										//System.out.println( c );
//	
//										if ( !kb.getABox().getNode( y ).hasType( c ) ) {
//											classCheck = false;
//											break;
//										}
//									}
//
//									if ( classCheck ) {
//										// prevents accidently expanding the set to consider
//										if ( !varBindings.keySet().contains( curr.getObject() ) ) {
//											ArrayList temp = new ArrayList();
//											temp.add( y );
//											varBindings.put( curr.getObject(), temp );
//										} else {
//											((ArrayList) varBindings.get( curr.getObject() )).add( y );
//										}
//	
//										//System.out.println( "Adding edge mapping..." );
//										//edgeMapping.put( curr, new Edge( curr.name, x, y, null ) );
//										currList.addEdge( edge ); 
//										//master.addEdge( new Edge( x, curr.name, y, null ) );
//									}
//								}
//							}	
//						}
//					}
//				}
//				//System.err.println( varBindings.get( curr.from ).getClass() );
//			}
//			
//			edgeMapping.put( curr, currList );
//			
//			//System.out.println( "Edge finished: " + curr );
//			//System.out.println( currList );
//			//System.out.println( varOrder );
//		}
//		
//		//System.out.println( "Here are the edges: " + edgeMapping );
//		//System.out.println( "Bindings: " + varBindings );
//		//System.out.println( "Master: " + master );
//		
//		for ( Iterator i = varBindings.keySet().iterator(); i.hasNext(); ) {
//			Object o = i.next();
//			if(QueryEngine.DEBUG) System.out.println( o );
//			//System.out.println( varBindings.get( o ) );
//		}
//				
//		if ( q.getDistLitVars().isEmpty() ) {
//			for ( Iterator b = new BindingIterator( q, varBindings ); b.hasNext(); ) {
//				HashMap curr = (HashMap) b.next();
//	
//				//System.out.println( "WHY?" );
//	
//				boolean status = true;
//				for ( Iterator e = master.iterator(); e.hasNext(); ) {
//					Edge ce = (Edge) e.next();
//					if ( q.isLitVar( ce.getObject() ) ) {
//						// TODO should this really be here?
//					} else {
//						EdgeList edges = (EdgeList) edgeMapping.get( ce );
//			
//						ATermAppl x = (ATermAppl) curr.get( ce.getSubject() );
//						ATermAppl y = (ATermAppl) curr.get( ce.getObject() );
//			
//						if ( edges.getEdgesFromTo( kb.getABox().getIndividual( x ), kb.getABox().getNode( y ) ).isEmpty() ) {
//							// TODO do role check also, not just getedgesfromto
//							//System.out.println( curr );
//							//System.out.println( ce.from + " has no " + ce.role.getName() + " edge to " + ce.to );
//							//System.out.println( x + " has no " + ce.role.getName() + " edge to " + y );
//							status = false; 
//							break;
//						}
//					}
//				}
//	
//				if ( status ) {
//					results.add( curr );
//				}
//			}
//		} else {
//			for ( Iterator b = new BindingIterator( q, varBindings ); b.hasNext(); ) {
//				HashMap curr = (HashMap) b.next();
//
//				//System.out.println( curr );
//
//				boolean status = true;
//				for ( Iterator e = master.iterator(); e.hasNext(); ) {
//					Edge ce = (Edge) e.next();
//					if ( q.isLitVar( ce.getObject() ) ) {
//						// TODO should this really be here?
//					} else {
//						EdgeList edges = (EdgeList) edgeMapping.get( ce );
//
//						ATermAppl x = (ATermAppl) curr.get( ce.getSubject() );
//						ATermAppl y = (ATermAppl) curr.get( ce.getObject() );
//
//						if ( edges.getEdgesFromTo( kb.getABox().getIndividual( x ), kb.getABox().getNode( y ) ).isEmpty() ) {
//							//System.out.println( x + " has no " + ce.role + " edge to " + y );
//							status = false; 
//							break;
//						}
//					}
//					
//				}
//
//				if ( status ) {
//					Iterator l = new LiteralIterator( curr, kb, q );
//					
//					while ( l.hasNext() ) {
//						results.add( l.next() );
//					}		
//					
//				}
//			}
//		}
		
		return results;
	}
}
