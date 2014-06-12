/*
 * Created on Aug 6, 2005
 */
package org.mindswap.pellet.query;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.SizeEstimate;

import aterm.ATermAppl;

/**
 * Reorganizes the order of variables to improve the performance of the query answering.
 * Designed for queries with one connected component and no undistinguished variables.
 * 
 * @author Evren Sirin
 */
public class QueryCost {
    private KnowledgeBase kb;
    private SizeEstimate estimate;
    private List patterns;
    
    public static double costIR = 5;
    public static double costIC = 1;
    public static double costRR = 2;
    public static double costRC = 1;

    public QueryCost( KnowledgeBase kb ) {
        this.kb = kb; 
        this.estimate = kb.getSizeEstimate();
    }    

    public double estimateCost( Query query ) {
        return estimateCost( query.getQueryPatterns() );
    }
    
    public double estimateCost( List patterns ) {
        this.patterns = patterns;
        
        if( notOptimal() )
            return Double.MAX_VALUE;
        
        return estimateCost( 0, new HashSet() );
    }
    
    protected boolean notOptimal() {
        boolean allSemiGround = true;
        
//        Map firstPos = new HashMap();
        Set bound = new HashSet();       
        for( int i = 0; i < patterns.size(); i++ ) {
            QueryPattern pattern = (QueryPattern) patterns.get( i );
            
            ATermAppl subj = pattern.getSubject();
            ATermAppl obj = pattern.getObject();
            
//            if( ATermUtils.isVar( subj ) ) {
//                if( !bound.contains( subj ) )
//                    firstPos.put( subj, new Integer( i ) );
//                else {
//                    int pos = ((Integer)firstPos.get( subj )).intValue();
//                    if( pos != i - 1 && )
//                }
//            }
                
            if( pattern.isTypePattern() ) {
                allSemiGround = false;
                if( ATermUtils.isVar( subj ) && !bound.contains( subj ) )
                    if( i > 0 )
                        return true;
            }
            else {
                if( ATermUtils.isVar( subj ) &&  ATermUtils.isVar( obj ) ) {
                    allSemiGround = false;
                    if( !bound.contains( subj ) && !bound.contains( obj ) && i > 0 )
                        return true;
                }
                else if( ATermUtils.isVar( subj ) ) {
                    if( !bound.contains( subj ) && i > 0 )
                        return true;     
                    if( !allSemiGround )
                        return true;
                }
                else if( ATermUtils.isVar( obj ) ) {
                    if( !bound.contains( obj ) && i > 0 )
                        return true;     
                    if( !allSemiGround )
                        return true;
                }
            }
            
            bound.add( pattern.getSubject() );
            bound.add( pattern.getObject() );
        }
        
        return false;        
    }
    
    protected ATermAppl inv( ATermAppl pred ) {
        return kb.getRBox().getRole( pred ).getInverse().getName();
    }
    
    protected double estimateCost( int index, Set bound ) {
        if( patterns.size() <= index ) 
            return 1.0; 
                
        QueryPattern pattern = (QueryPattern) patterns.get(index);        
                      
        ATermAppl subj = pattern.getSubject();
        ATermAppl pred = pattern.getPredicate();
        ATermAppl obj = pattern.getObject();
        
        double cost = -1.0;
        if( pattern.isTypePattern() ) { 
            if( bound.contains( subj ) )
                cost = costIC + estimateCost( index + 1, bound );
            else {
                bound.add( subj );
                cost = costIR + estimate.size( obj ) * estimateCost( index + 1, bound );
            }
        }
        else {
            if( bound.contains( subj ) && bound.contains( obj ) ) {
                cost = costRC + estimateCost( index + 1, bound );
            }
            else if( bound.contains( subj ) ) {
                bound.add( obj );
                cost = costRR + estimate.avg( pred ) * estimateCost( index + 1, bound );
            }
            else if( bound.contains( obj ) ) {
                bound.add( subj );
                cost = costRR + estimate.avg( inv( pred ) ) * estimateCost( index + 1, bound );
            }
            else {
                bound.add( subj );
                bound.add( obj );
                cost = costRR + estimate.size( pred ) * estimateCost( index + 1, bound );
            }
        }
        
        return cost;
    }
}
