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

package org.mindswap.pellet;


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
import org.mindswap.pellet.taxonomy.Taxonomy;
import org.mindswap.pellet.taxonomy.TaxonomyNode;
import org.mindswap.pellet.utils.URIUtils;

import aterm.ATermAppl;

/*
 * Created on Aug 13, 2003
 *
 */

/**
 * @author Evren Sirin
 *
 */
public class RoleTaxonomyBuilder {
    protected static Log log = LogFactory.getLog( Taxonomy.class );
        
	private byte PROPOGATE_UP = 1;
    private byte NO_PROPOGATE = 0;
    private byte PROPOGATE_DOWN = -1;
    
	protected Collection properties;
	
	protected Taxonomy taxonomy; 
	protected RBox rbox;
	
	public RoleTaxonomyBuilder( RBox rbox ) {
		this.rbox = rbox;
		
		properties =  rbox.getRoles();
	    taxonomy = new Taxonomy( true );	
	}

	public Taxonomy classify() {		
		if( log.isDebugEnabled() ) {
		    log.debug("Properties: " + properties.size());				
		}

		for(Iterator i = properties.iterator(); i.hasNext();) {
            Role c = (Role) i.next();		

            if(c.isAnnotationRole() || c.isOntologyRole() )
                continue;

			classify(c);
		}				
		
		return taxonomy;
	}

	int count= 0;
	private void classify(Role c) {
	    boolean hide = c.isAnon();
	    
	    if( taxonomy.contains( c.getName() ) ) return;
	    
		if( log.isTraceEnabled() ) 
		    log.trace("Property (" + (++count) + ") " + c + "...");	
		

		Map marked = new HashMap();
		mark( taxonomy.getTop(), marked, Boolean.TRUE, NO_PROPOGATE );
		mark( taxonomy.getBottom(), marked, Boolean.FALSE, NO_PROPOGATE );
		
		Collection supers = search(true, c, taxonomy.getTop(), new HashSet(), new ArrayList(), marked);
		
		marked = new HashMap();		
		mark( taxonomy.getTop(), marked, Boolean.FALSE, NO_PROPOGATE );
		mark( taxonomy.getBottom(), marked, Boolean.TRUE, NO_PROPOGATE );
		
		if(supers.size() == 1) {
		    TaxonomyNode sup = (TaxonomyNode) supers.iterator().next();
			
			// if i has only one super class j and j is a subclass 
			// of i then it means i = j. There is no need to classify
			// i since we already know everything about j
			if( subsumed( sup, c, marked ) ) {
				if( log.isTraceEnabled() ) 
				    log.trace(
				        getName(c.getName()) + " = " + 
				        getName(sup.getName()));	

				taxonomy.addEquivalentNode( c.getName(), sup );
				return;
			}
		}
		
		Collection subs = search(false, c, taxonomy.getBottom(), new HashSet(), new ArrayList(), marked);
		
		TaxonomyNode node = taxonomy.addNode( c.getName(), hide );
		node.addSupers( new ArrayList( supers ) ) ;
		node.addSubs( new ArrayList( subs ) );
		
		node.removeMultiplePaths();
	}	
	
	private Collection search(boolean topSearch, Role c, TaxonomyNode x, Set visited, List result, Map marked) {
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

		if(posSucc.isEmpty()) {
			result.add( x );
		} 
		else {
			for(Iterator i = posSucc.iterator(); i.hasNext(); ) {
			    TaxonomyNode y = (TaxonomyNode) i.next();
				if(!visited.contains(y))
				    search( topSearch, c, y, visited, result, marked);
			}
		}
				
		return result;
	}


	private boolean subsumes(TaxonomyNode node, Role c, Map marked) {	
	    Boolean cached = (Boolean) marked.get( node ); 
	    if( cached != null )
	        return cached.booleanValue();
		
		// check subsumption
		boolean subsumes = subsumes( rbox.getRole( node.getName() ), c );
		// create an object based on result
		Boolean value = subsumes ? Boolean.TRUE : Boolean.FALSE;
		// during top search only negative information is propogated down
		byte propogate = subsumes ? NO_PROPOGATE : PROPOGATE_DOWN;
		// mark the node appropriately
		mark( node, marked, value, propogate);
		
		return subsumes;
	}

	private boolean subsumed(TaxonomyNode node, Role c, Map marked) {		
	    Boolean cached = (Boolean) marked.get( node ); 
	    if( cached != null )
	        return cached.booleanValue();

	    // check subsumption
		boolean subsumed = subsumes( c, rbox.getRole( node.getName() ) );
		// create an object based on result
		Boolean value = subsumed ? Boolean.TRUE : Boolean.FALSE;
		// during bottom search only negative information is propogated down
		byte propogate = subsumed ? NO_PROPOGATE : PROPOGATE_UP;
		// mark the node appropriately
		mark( node, marked, value, propogate);
		
		return subsumed;
	}
	
	private void mark(TaxonomyNode node, Map marked, Boolean value, byte propogate) {
	    Boolean exists = (Boolean) marked.get( node );
	    if( exists != null ) {
	        if( exists != value )
	            throw new RuntimeException("Inconsistent classification result " + 
	                node.getName() + " " + exists + " " + value);
	        else 
	            return;
	    }
	    marked.put( node, value );
	    
	    if( propogate != NO_PROPOGATE ) {
		    List others = (propogate == PROPOGATE_UP) ? node.getSupers() : node.getSubs();
		    for(Iterator i = others.iterator(); i.hasNext();) {
	            TaxonomyNode next = (TaxonomyNode) i.next();
	            mark( next, marked, value, propogate );
	        }
	    }
	}
	
	private boolean subsumes(Role sup, Role sub) {
	    boolean result = sup.isSuperRoleOf( sub );
		
		return result;
	} 
		
	
	private String getName(ATermAppl c) {
	    return URIUtils.getLocalName(c.getName());
	}
	
}
