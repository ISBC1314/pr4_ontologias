/*
 * Created on Aug 6, 2005
 */
package org.mindswap.pellet.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.datatypes.Datatype;
import org.mindswap.pellet.query.impl.QueryImpl;
import org.mindswap.pellet.taxonomy.Taxonomy;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.SetUtils;

import aterm.ATermAppl;
import aterm.ATermList;

/**
 * Reorganizes the order of variables to improve the performance of the query answering.
 * Designed for queries with one connected component and no undistinguished variables.
 * 
 * @author Evren Sirin
 */
public class QuerySplitter {
    public static Log log = LogFactory.getLog( QueryEngine.class );
    
    private KnowledgeBase kb;
    private Query oldQuery;
    private Query newQuery;
    private Set addedVars; 
    private Set addedPatterns; 
    
    public List split( Query query ) {
        try {
            List queries = new ArrayList();
            
            this.kb = query.getKB();
            this.oldQuery = query;
            this.addedVars = new HashSet();
            this.addedPatterns = new HashSet();
            
            log.debug( oldQuery );

            Set constants = query.getConstants();
            for(Iterator i = constants.iterator(); i.hasNext();) {
                ATermAppl val = (ATermAppl) i.next();
                
                if( !addedVars.contains( val ) ) {
                    newQuery = new QueryImpl( kb );
                    addVar( val );
                    
                    queries.add( newQuery );
                    
                    Set vars = newQuery.getVars();
                    for( Iterator j = oldQuery.getResultVars().iterator(); j.hasNext(); ) {
                        ATermAppl var = (ATermAppl) j.next();
                        if( vars.contains( var ) )
                            newQuery.addResultVar( var );
                    }
                    
                    for( Iterator j = oldQuery.getDistVars().iterator(); j.hasNext(); ) {
                        ATermAppl var = (ATermAppl) j.next();
                        if( vars.contains( var ) )
                            newQuery.addDistVar( var );
                    }
                    
                    log.debug( newQuery );
                }                
            }
            
            Set vars = query.getObjVars();
            for(Iterator i = vars.iterator(); i.hasNext();) {
                ATermAppl var = (ATermAppl) i.next();
                
                if( !addedVars.contains( var ) ) {
                    newQuery = new QueryImpl( kb );
                    addVar( var );
                    
                    queries.add( newQuery );                    
                    
                    Set allVars = newQuery.getVars();
                    for( Iterator j = oldQuery.getResultVars().iterator(); j.hasNext(); ) {
                        var = (ATermAppl) j.next();
                        if( allVars.contains( var ) )
                            newQuery.addResultVar( var );
                    }
                    
                    for( Iterator j = oldQuery.getDistVars().iterator(); j.hasNext(); ) {
                        var = (ATermAppl) j.next();
                        if( vars.contains( var ) )
                            newQuery.addDistVar( var );
                    }
                    
                    log.debug( newQuery );
                }                
            }
            
//            ATermUtils.assertTrue( 
//                oldQuery.getVars().containsAll(newQuery.getVars()) &&
//                newQuery.getVars().containsAll(oldQuery.getVars()) &&
//                oldQuery.getQueryPatterns().containsAll(newQuery.getQueryPatterns()) &&
//                newQuery.getQueryPatterns().containsAll(oldQuery.getQueryPatterns())); 

            return queries;
        } catch(RuntimeException e) {
            System.err.println( "WARNNING: Query split failed, continuing with query execution.");
            System.err.println( oldQuery );
            System.err.println( newQuery );
            return Collections.singletonList( query );
        }
    }
    
    private void addVar( ATermAppl var ) {
//        if( !ATermUtils.isVar( var ) )
//            return;
        if( addedVars.contains( var ) )
            return;
        else
            addedVars.add( var );
        
//        if( oldQuery.getResultVars().contains( var ) )
//            newQuery.addResultVar( var );
        
        List inferredTypes = new ArrayList();
        List patterns = new ArrayList();
        Set neighbors = new LinkedHashSet();
        List outList = oldQuery.findPatterns( var, null, null );
        for(Iterator i = outList.iterator(); i.hasNext(); ) {
            QueryPattern pattern = (QueryPattern) i.next();
            ATermAppl pred = pattern.getPredicate();
            ATermAppl obj = pattern.getObject();
            
            inferredTypes.addAll( kb.getDomains( pred ) );

            if( addedPatterns.contains( pattern ) )
                continue;
            else
                addedPatterns.add( pattern );
                        
            patterns.add( new ATermAppl[] { var, pred, obj } );
            neighbors.add( obj );
        }
        
        List inList = oldQuery.findPatterns( null, null, var );
        for(Iterator i = inList.iterator(); i.hasNext(); ) {
            QueryPattern pattern = (QueryPattern) i.next();
            ATermAppl subj = pattern.getSubject();
            ATermAppl pred = pattern.getPredicate();

            inferredTypes.addAll( kb.getRanges( pred ) );
            
            if( addedPatterns.contains( pattern ) )
                continue;
            else
                addedPatterns.add( pattern );
            
            patterns.add( new ATermAppl[] { subj, pred, var } );
            neighbors.add( subj );
        }                
        
        boolean isConstant = !ATermUtils.isVar( var );
        boolean isLiteral = oldQuery.getLitVars().contains( var ) || ATermUtils.isLiteral( var );
        
        if( isLiteral ) {            
            Datatype datatype = oldQuery.getDatatype( var );
            newQuery.addConstraint( var, datatype );    
        }
        else {
            ATermAppl nominal = isConstant ? ATermUtils.makeValue( var ) : null;
            ATermList classes = oldQuery.getClasses( var );
            for( ; !classes.isEmpty(); classes = classes.getNext() ) {
                ATermAppl c = (ATermAppl) classes.getFirst();
                if( c == nominal )
                    continue;
                
                if( PelletOptions.SIMPLIFY_QUERY ) {
                    // TODO even if kb is not classified we should use told subsumers
                    if( kb.isClassified() ) {
                        Set subs = kb.getTaxonomy().getSubSupers( c, false, Taxonomy.SUB, true );
                        Set eqs = kb.getAllEquivalentClasses( c );
                        if( SetUtils.intersects( inferredTypes, subs ) || 
                            SetUtils.intersects( inferredTypes, eqs ) )
                            continue;
                    }
                    else {
                        if( inferredTypes.contains( c ) )
                            continue;
                    }
                }
                    
                newQuery.addTypePattern( var, c );
            }
        }
        
        for(Iterator i = patterns.iterator(); i.hasNext();) {
            ATermAppl[] pattern = (ATermAppl[]) i.next();
            newQuery.addEdgePattern( pattern[0], pattern[1], pattern[2] );
        }                    
        
        for(Iterator i = neighbors.iterator(); i.hasNext();) {
            ATermAppl neighbor = (ATermAppl) i.next();
            addVar( neighbor );
        }
    }

}
