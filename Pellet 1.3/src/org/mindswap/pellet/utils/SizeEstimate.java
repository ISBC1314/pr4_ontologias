/*
 * Created on Mar 13, 2006
 */
package org.mindswap.pellet.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.Role;
import org.mindswap.pellet.exceptions.InternalReasonerException;

import aterm.ATermAppl;

public class SizeEstimate {
    protected static Log log = LogFactory.getLog( SizeEstimate.class );
    
    private KnowledgeBase kb;
    private Map sizes;
    private Map avgs;
    
    public SizeEstimate( KnowledgeBase kb ) {
        this.kb = kb;
        
        init();
    }
    
    private void init() {
        sizes = new HashMap();
        avgs = new HashMap();     
        
        sizes.put( ATermUtils.TOP, new Integer( kb.getIndividuals().size() ) );
        sizes.put( ATermUtils.BOTTOM, new Integer( 0 ) );        
    }
    
    public void computeAll() {
        compute( new HashSet( kb.getClasses() ), kb.getProperties() );
    }
    
    public void compute( Collection concepts, Collection properties ) {
        Timer timer = kb.timers.startTimer("sizeEstimate");
        
        concepts.removeAll( sizes.keySet() );
        properties.removeAll( sizes.keySet() );

        if( concepts.isEmpty() && properties.isEmpty() ) {
            timer.stop();
            return;
        }
        
        log.info( "Size estimation started" );
        
        Map pSubj = new HashMap();
        Map pObj = new HashMap();
        
        for( Iterator i = concepts.iterator(); i.hasNext(); ) {
            ATermAppl c = (ATermAppl) i.next();
            sizes.put( c, new Integer( 1 ) );
            if( log.isTraceEnabled() )
                log.trace( "Initialize " + c  + " = " + size( c ) );
        }
        
        for( Iterator i = properties.iterator(); i.hasNext(); ) {
            ATermAppl p = (ATermAppl) i.next();
            sizes.put( p, new Integer( 1 ) );
            pSubj.put( p, new Integer( 1 ) );
            pObj.put( p, new Integer( 1 ) );
        }                       
        
        for( Iterator i = kb.getIndividuals().iterator(); i.hasNext(); ) {
            ATermAppl ind = (ATermAppl) i.next();
            for( Iterator j = concepts.iterator(); j.hasNext(); ) {
                ATermAppl c = (ATermAppl) j.next();
                
                if( kb.isKnownType( ind, c ).isTrue() )
                    sizes.put( c, new Integer( size( c ) + 1 ) );  
            }
            
            for( Iterator j = properties.iterator(); j.hasNext(); ) {
                ATermAppl p = (ATermAppl) j.next();
                Role role = kb.getRBox().getRole(p);
                
                Collection knowns = new HashSet();
                Collection unknowns = new HashSet();           
                
                if( role.isObjectRole() )
                    kb.getABox().getObjectPropertyValues(ind, role, (Set) knowns, (Set) unknowns);
                else
                    knowns = kb.getABox().getObviousDataPropertyValues(ind, role, null);
                
                if( !knowns.isEmpty() ) {
                    if( log.isTraceEnabled() )
                        log.trace( "Update "  + p + " by " + knowns.size() );
                    sizes.put( p, new Integer( size( p ) + knowns.size() ) );
                    pSubj.put( p, new Integer( ((Integer)pSubj.get( p )).intValue() + 1 ) ); 
                }
                
                if( role.isObjectRole() ) {
                    role = role.getInverse();
                    
                    knowns = new HashSet();
                    unknowns = new HashSet();           
                    
                    kb.getABox().getObjectPropertyValues(ind, role, (Set)knowns, (Set)unknowns);
                    
                    if( !knowns.isEmpty() ) {
                        pObj.put( p, new Integer( ((Integer)pObj.get( p )).intValue() + 1 ) ); 
                    }
                }
            } 
        }       
              
        for( Iterator i = properties.iterator(); i.hasNext(); ) {
            ATermAppl p = (ATermAppl) i.next();
            Role role = kb.getRBox().getRole( p );
            ATermAppl invP = (role.getInverse() != null) ? role.getInverse().getName() : null;
            int size = size( p );
            int subjCount = ((Integer) pSubj.get( p )).intValue();
            int objCount = ((Integer) pObj.get( p )).intValue();

            avgs.put( p, new Double( (double) size / subjCount ) );
            if( invP != null )
                avgs.put( invP, new Double( (double) size / objCount ) );
        }

        timer.stop();

        if( log.isDebugEnabled() ) {
            log.debug( "Sizes:" );
            log.debug( sizes );
            log.debug( "Averages:" );
            log.debug( avgs );
        }
        
        if( log.isInfoEnabled() )
            log.info( "Size estimation finished in " + timer.getLast() + "ms");
    }

    
//    private void compute() {
//        sizes = new HashMap();
//        avgs = new HashMap();
//        Map pSubj = new HashMap();
//        Map pObj = new HashMap();
//        
//        for( Iterator i = kb.getAllClasses().iterator(); i.hasNext(); ) {
//            ATermAppl c = (ATermAppl) i.next();
//            sizes.put( c, new Integer( 1 ) );
//        }
//        
//        for( Iterator i = kb.getProperties().iterator(); i.hasNext(); ) {
//            ATermAppl p = (ATermAppl) i.next();
//            sizes.put( p, new Integer( 1 ) );
//            pSubj.put( p, new Integer( 1 ) );
//            pObj.put( p, new Integer( 1 ) );
//        }               
//        
//        ABox abox = kb.getABox().getPseudoModel();        
//        for( Iterator i = abox.getIndIterator(); i.hasNext(); ) {
//            Individual ind = (Individual) i.next();
//            
//            if( !ind.isIndividual() )
//                continue;
//                
//            List types = ind.getTypes( Node.ATOM );
//            for (int j = 0; j < types.size(); j++) {
//                ATermAppl type = (ATermAppl) types.get(j);
//                boolean nonDeterministic = !ind.getDepends( type ).isIndependent();
//                
//                if( nonDeterministic || ATermUtils.isNot( type ) )
//                    continue;
//                                
//                sizes.put( type, new Integer( size( type ) + 1 ) );                
//            }
//            
//            Set outRoles = new HashSet();
//            EdgeList edges = ind.getOutEdges();
//            for (int j = 0; j < edges.size(); j++) {
//                Edge edge = edges.edgeAt(j);
//                ATermAppl pred = edge.getRole().getName();
//                Node val = edge.getTo();
//                
//                if( edge.getDepends().isIndependent() && val.isRootNominal() ) {
//                    sizes.put( pred, new Integer( size( pred ) + 1 ) );  
//                    
//                    if( !outRoles.contains( pred ) ) {
//                        outRoles.add( pred );
//                        if(!pSubj.containsKey( pred ))
//                            System.out.println(pred);
//                        pSubj.put( pred, new Integer( ((Integer)pSubj.get( pred )).intValue() + 1 ) ); 
//                    }
//                }
//            }
//            
//            Set inRoles = new HashSet();
//            edges = ind.getOutEdges();
//            for (int j = 0; j < edges.size(); j++) {
//                Edge edge = edges.edgeAt(j);
//                ATermAppl pred = edge.getRole().getName();
//                Node val = edge.getFrom();
//                
//                if( edge.getDepends().isIndependent() && val.isRootNominal() ) {
//                    sizes.put( pred, new Integer( size( pred ) + 1 ) );  
//                    
//                    if( !inRoles.contains( pred ) ) {
//                        inRoles.add( pred );
//                        pObj.put( pred, new Integer( ((Integer)pObj.get( pred )).intValue() + 1 ) ); 
//                    }
//                }
//            }
//        }   
//        
//        for( Iterator i = kb.getProperties().iterator(); i.hasNext(); ) {
//            ATermAppl p = (ATermAppl) i.next();
//            Role role = kb.getRBox().getRole( p );
//            ATermAppl invP = ( role.getInverse() != null ) ? role.getInverse().getName() : null;
//            int size = size( p );
//            int subjCount = ((Integer) pSubj.get( p )).intValue();
//            int objCount = ((Integer) pObj.get( p )).intValue();
//            
//            sizes.put( p, new Integer( 1 ) );
//            avgs.put( p, new Double( (double)size/subjCount) );
//            if( invP != null )
//                avgs.put( invP, new Double( (double)size/objCount) );
//        } 
//        
//        System.out.println( sizes );
//        System.out.println();
//        System.out.println( avgs );
//    }
    
    public int size( ATermAppl c ) {
        if( !sizes.containsKey( c ) )
            throw new InternalReasonerException( "Size estimate for " + c + " is not found!");
        return ((Integer) sizes.get( c )).intValue();
    }
    
    public double avg( ATermAppl pred ) {
        if( !avgs.containsKey( pred ) )
            throw new InternalReasonerException( "Average estimate for " + pred + " is not found!");
        return ((Double) avgs.get( pred )).doubleValue();        
    }
}