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

package org.mindswap.pellet.taxonomy;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.exceptions.InternalReasonerException;
import org.mindswap.pellet.output.OutputFormatter;
import org.mindswap.pellet.output.TaxonomyPrinter;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Bool;
import org.mindswap.pellet.utils.SetUtils;

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
public class Taxonomy {
    /**
     * @deprecated Edit log4j.properties instead to turn on debugging
     */
	public static boolean DEBUG = false;
    /**
     * @deprecated Edit log4j.properties instead to turn on debugging
     */
    public static boolean DETAILED_DEBUG = false;
    
    protected static Log log = LogFactory.getLog( Taxonomy.class );
    
    public static boolean SUB   = true;
    public static boolean SUPER = false;
	
    public static boolean TOP_DOWN  = true;
    public static boolean BOTTOM_UP = false;
	
	protected Map nodes;
	
	protected TaxonomyNode TOP_NODE;
	protected TaxonomyNode BOTTOM_NODE;
	
	protected TaxonomyPrinter printer;

	public Taxonomy() {
	    this( false );
	}
	
	public Taxonomy( boolean hideTopBottom ) {
		printer = new TaxonomyPrinter();
		nodes = new HashMap();
		
		TOP_NODE = addNode( ATermUtils.TOP, hideTopBottom );
		BOTTOM_NODE = addNode( ATermUtils.BOTTOM, hideTopBottom );
		
		TOP_NODE.addSub( BOTTOM_NODE );
	}
	
    public TaxonomyNode getBottom() {
        return BOTTOM_NODE;
    }
    
    public TaxonomyNode getTop() {
        return TOP_NODE;
    }
    
    public Set getClasses() {
        return nodes.keySet();
    }    
    
    public boolean contains( ATermAppl c ) {
        return nodes.containsKey( c );
    }
    
	public TaxonomyNode addNode( ATermAppl c ) {
	    return addNode( c, false );
	}

	public TaxonomyNode addNode( ATermAppl c, boolean hide ) {
	    TaxonomyNode node = new TaxonomyNode( c, hide );
	    nodes.put( c, node );
	    
	    return node;
	}

	public void addEquivalentNode( ATermAppl c, TaxonomyNode node ) {
	    boolean hide = !ATermUtils.isPrimitive( c );

	    if( !hide ) 
		    node.addEquivalent( c );
		    
	    nodes.put( c, node );
	}

	
	public TaxonomyNode getNode( ATermAppl c ) {
	    return (TaxonomyNode) nodes.get( c );
	}

	public void removeNode( TaxonomyNode node ) {
        node.disconnect();

        nodes.remove( node.getName() );
	}

	/** 
	 * Returns all the instances of concept c. If TOP concept is used every individual in the
	 * knowledge base will be returned
	 * 
	 * @param c Class whose instances are returned 
	 * @return A set of ATerm objects
	 */
	public Set getInstances(ATermAppl c) {
		return getInstances(c, false);
	}

	/**
	 * Returns the instances of class c. Depending on the second parameter the resulting
	 * list will include all or only the direct instances, i.e. if the individual
	 * is not type of any other class that is a subclass of c.
	 * 
	 * @param c Class whose instances are found
	 * @param direct If true return only the direct instances, otherwise return all the instances 
	 * @return A set of ATerm objects
	 */
	public Set getInstances(ATermAppl c, boolean direct) {
	    TaxonomyNode node = (TaxonomyNode) nodes.get( c );
	    
	    if(node == null) throw new RuntimeException(c + " is an unknown class!");	 	    
	    
		Set result = new HashSet( node.getInstances() );

		if(!direct) { 
			Iterator subs = getSubs( c ).iterator();
			while(subs.hasNext()) {
				Set sub = (Set) subs.next();
				ATermAppl a = (ATermAppl) sub.iterator().next();
				result.addAll( getInstances( a ) );
			}
		}
		
		return result;	
	}

	/**
	 * Checks if x is equivalent to y
	 * 
	 * @param x Name of the first class
	 * @param y Name of the second class
	 * @return true if x is equivalent to y
	 */
	public boolean isEquivalent(ATermAppl x, ATermAppl y) {
	    TaxonomyNode node1 = (TaxonomyNode) nodes.get( x );
	    TaxonomyNode node2 = (TaxonomyNode) nodes.get( y );

		return node1.equals(node2);	
	}
	
	/**
	 * Checks if x has an ancestor y.
	 * 
	 * @param x Name of the node
	 * @param y Name of the ancestor ode
	 * @return true if x has an ancestor y
	 */
	public Bool isSubNodeOf( ATermAppl x, ATermAppl y ) {   
	    TaxonomyNode nodeX = (TaxonomyNode) nodes.get( x );
        TaxonomyNode nodeY = (TaxonomyNode) nodes.get( y );
        
        if( nodeX == null || nodeY == null )
            return Bool.UNKNOWN;
        
        if( nodeX.isHidden() ) {
            if( nodeY.isHidden() )
                return Bool.UNKNOWN;
            else
                return getSupers( x, false, true ).contains( y ) ? Bool.TRUE : Bool.FALSE;
        }
        else 
            return getSubs( y, false, true ).contains( x ) ? Bool.TRUE : Bool.FALSE;
	}
		
	/** 
	 * Returns all the (named) subclasses of class c. The
	 * class c itself is not included in the list but all the other classes that
	 * are equivalent to c are put into the list. Also note that the returned
	 * list will always have at least one element, that is the BOTTOM concept. By 
	 * definition BOTTOM concept is subclass of every concept.
	 * This function is equivalent to calling getSubClasses(c, true).
	 * 
	 * @param c class whose subclasses are returned 
	 * @return A set of sets, where each set in the collection represents an equivalence 
	 * class. The elements of the inner class are ATermAppl objects. 
	 */
	public Set getSubs(ATermAppl c) {
		return getSubs(c, false);
	}

	/**
	 * Returns the (named) subclasses of class c. Depending onthe second parameter the resulting
	 * list will include either all subclasses or only the direct subclasses.
	 * 
	 * A class d is a direct subclass of c iff
	 * <ol>
	 *   <li>d is subclass of c</li> 
	 *   <li>there is no other class x different from c and d such that x is subclass 
	 *   of c and d is subclass of x</li>
	 * </ol> 
	 * The class c itself is not included in the list but all the other classes that
	 * are sameAs c are put into the list. Also note that the returned
	 * list will always have at least one element. The list will either include one other
	 * concept from the hierarchy or the BOTTOM concept if no other class is subsumed by c. 
	 * By definition BOTTOM concept is subclass of every concept. 
	 * 
	 * @param c Class whose subclasses are found
	 * @param direct If true return only direct subclasses elese return all the subclasses 
	 * @return A set of sets, where each set in the collection represents an equivalence 
	 * class. The elements of the inner class are ATermAppl objects. 
	 */
	public Set getSubs(ATermAppl c, boolean direct) {
	    return getSubSupers( c, direct, SUB, false );	
	}
		
	/** 
	 * Returns all the superclasses (implicitly or explicitly defined) of class c. The
	 * class c itself is not included in the list. but all the other classes that
	 * are sameAs c are put into the list. Also note that the returned
	 * list will always have at least one element, that is TOP concept. By definition
	 * TOP concept is superclass of every concept.
	 * This function is equivalent to calling getSuperClasses(c, true).
	 * 
	 * @param c class whose superclasses are returned 
	 * @return A set of sets, where each set in the collection represents an equivalence 
	 * class. The elements of the inner class are ATermAppl objects. 
	 */
	public Set getSupers(ATermAppl c) {
		return getSupers(c, false);
	}

	public Set getSupers(ATermAppl c, boolean direct, boolean flat) {
	    return getSubSupers( c, direct, SUPER, flat );	
	}
	
	public Set getSubs(ATermAppl c, boolean direct, boolean flat) {
	    return getSubSupers( c, direct, SUB, flat );	
	}
	
	/**
	 * Returns the (named) superclasses of class c. Depending on the second parameter the resulting
	 * list will include either all or only the direct superclasses.
	 * 
	 * A class d is a direct superclass of c iff
	 * <ol>
	 *   <li> d is superclass of c </li> 
	 *   <li> there is no other class x such that x is superclass of c and d is superclass of x </li>
	 * </ol>
	 * The class c itself is not included in the list but all the other classes that
	 * are sameAs c are put into the list. Also note that the returned
	 * list will always have at least one element. The list will either include one other
	 * concept from the hierarchy or the TOP concept if no other class subsumes c. By definition
	 * TOP concept is superclass of every concept. 
	 * 
	 * @param c Class whose subclasses are found
	 * @param direct If true return all the superclasses else return only direct superclasses
	 * @return A set of sets, where each set in the collection represents an equivalence 
	 * class. The elements of the inner class are ATermAppl objects. 
	 */
	public Set getSupers(ATermAppl c, boolean direct) {
	    return getSubSupers( c, direct, SUPER, false );		
	}
	
	public Set getSubSupers(ATermAppl c, boolean direct, boolean subOrSuper, boolean flat ) {
	    TaxonomyNode node = (TaxonomyNode) nodes.get( c );
        
        if( node == null )
            return SetUtils.EMPTY_SET;
	    
	    Set result = new HashSet();
	    
	    List visit = new ArrayList();
	    visit.addAll( ( subOrSuper == SUB ) ? node.getSubs() : node.getSupers() );
	    
	    for( int i = 0; i < visit.size(); i++ ) {
	        node = (TaxonomyNode) visit.get( i );
	        
	        if( node.isHidden() )
	            continue;
	        
	        Set add = node.getEquivalents();
	        if( flat )
	            result.addAll( add );
	        else if( !add.isEmpty() )
	            result.add( add );
	        
	        if( !direct )
	            visit.addAll( ( subOrSuper == SUB ) ? node.getSubs() : node.getSupers() );
	    }
		
		return result;	    
	}
		
	public Set getFlattenedSubSupers(ATermAppl c, boolean direct, boolean subOrSuper ) {
	    TaxonomyNode node = (TaxonomyNode) nodes.get( c );
	    
	    Set result = new HashSet();
	    
	    List visit = new ArrayList();
	    visit.addAll( ( subOrSuper == SUB ) ? node.getSubs() : node.getSupers() );
	    
	    for( int i = 0; i < visit.size(); i++ ) {
	        node = (TaxonomyNode) visit.get( i );
	        
	        if( node.isHidden() )
	            continue;
	        
	        Set add = node.getEquivalents();
            result.addAll( add );
	        
	        if( !direct )
	            visit.addAll( ( subOrSuper == SUB ) ? node.getSubs() : node.getSupers() );
	    }
		
		return result;	    
	}	

	/**
	 * Returns all the classes that are equivalent to class c. Class c itself is NOT
	 * included in the result.
	 * 
	 * @param c class whose equivalent classes are found
	 * @return A set of ATerm objects
	 */
	public Set getEquivalents(ATermAppl c) {
	    TaxonomyNode node = (TaxonomyNode) nodes.get( c );
	    
	    if( node == null ) throw new RuntimeException( c + " is an unknown class!" );
	    
	    if( node.isHidden() ) return SetUtils.EMPTY_SET;
	    
		Set result = new HashSet( node.getEquivalents() );
		result.remove( c );
			
		return result;	
	}	
		
	/**
	 * Returns all the classes that are equivalent to class c. Class c itself is
	 * included in the result.
	 * 
	 * @param c class whose equivalent classes are found
	 * @return A set of ATerm objects
	 */
	public Set getAllEquivalents(ATermAppl c) {
	    TaxonomyNode node = (TaxonomyNode) nodes.get( c );
	    
	    if(node == null) throw new RuntimeException(c + " is an unknown class!");
	    
	    if( node.isHidden() ) return SetUtils.EMPTY_SET;
	    
		Set result = new HashSet( node.getEquivalents() );
			
		return result;	
	}
	
	/**
	 * Get all the direct classes individual belongs to.
	 * 
	 * @param ind An individual name
	 * @return A set of sets, where each set in the collection represents an equivalence 
	 * class. The elements of the inner class are ATermAppl objects. 
	 */
	public Set getDirectTypes(ATermAppl ind) {
		Set result = new HashSet();
				
		for(Iterator i = nodes.values().iterator(); i.hasNext();) {
            TaxonomyNode node = (TaxonomyNode) i.next();
            
            if( node.getInstances().contains( ind ) )
                result.add( node.getEquivalents() );
        }
		
		return result;
	}

	/**
	 * Get all the named classes individual belongs to. The result is returned as a set
	 * of sets where each 
	 * 
	 * @param ind An individual name
	 * @return A set of sets, where each set in the collection represents an equivalence 
	 * class. The elements of the inner class are ATermAppl objects. 
	 */
	public Set getTypes(ATermAppl ind) {
		Set result = new HashSet();
					
		for( Iterator i = nodes.values().iterator(); i.hasNext(); ) {
		    TaxonomyNode node = (TaxonomyNode) i.next();
	        
	        if( node.getInstances().contains( ind ) ) {
	            result.add( node.getEquivalents() );
		        
                Set supers = getSupers( node.getName() );
                result.addAll( supers );
	        }

	    }
		
		return result;	
	}
	
	/**
	 * Returns the classes individual belongs to. Depending on the second parameter the resulting
	 * list will include either all types or only the direct types.
	 * 
	 * @param ind An individual name
	 * @param direct If true return only the direct types, otherwise return all types
	 * @return A set of sets, where each set in the collection represents an equivalence 
	 * class. The elements of the inner class are ATermAppl objects. 
	 */
	public Set getTypes(ATermAppl ind, boolean direct) {
	    if(direct)
	        return getDirectTypes(ind);
	    else
	        return getTypes(ind);
	}
    
	public List topologocialSort( ) {
	    Integer ZERO = new Integer( 0 );
	    
	    Map degrees = new HashMap();
	    Set nodesPending = new LinkedHashSet();
	    Set nodesLeft = new HashSet();
	    List nodesSorted = new ArrayList();	    

	    log.debug( "Topological sort..." );	    

	    for(Iterator i = nodes.values().iterator(); i.hasNext();) {
            TaxonomyNode node = (TaxonomyNode) i.next();
            nodesLeft.add( node );
            int degree = node.getSupers().size();
            if( degree == 0 ) {
                nodesPending.add( node );
                degrees.put( node,  ZERO );
            }
            else
                degrees.put( node,  new Integer( degree ) );
        }
	    
        if( nodesPending.size() != 1 )
            throw new InternalReasonerException( "More than one node with no incoming edges "  + nodesPending );

	    for(int i = 0, size = nodesLeft.size(); i < size; i++) {
	        if( nodesPending.isEmpty() )
                throw new InternalReasonerException( "Cycle detected in the taxonomy!");

	        TaxonomyNode node = (TaxonomyNode) nodesPending.iterator().next();
            
            Integer deg = (Integer) degrees.get( node );
            if( deg == null )
                throw new InternalReasonerException( "No degree for node " + node);
            if( deg != ZERO ) 
                throw new InternalReasonerException( "Cycle detected in the taxonomy " + node + " " + deg + " " + nodesSorted.size() + " " + nodes.size());
            
            nodesPending.remove( node );
            nodesLeft.remove( node );
            nodesSorted.add( node.getName() );
            
            for(Iterator j = node.getSubs().iterator(); j.hasNext();) {
                TaxonomyNode sub = (TaxonomyNode) j.next();
                int degree = ((Integer) degrees.get( sub )).intValue();    
                if( degree == 1 ) {
                    nodesPending.add( sub );
                    degrees.put( sub, ZERO );
                }
                else
                    degrees.put( sub, new Integer( degree - 1 ) );
            }
        }
	    
        if( !nodesLeft.isEmpty() )
            throw new InternalReasonerException( "Failed to sort elements: " + nodesLeft);

	    log.debug( "done" );	    
        
	    return nodesSorted;
	}
	
	/**
	 * Walk through the super nodes of the given node and when a cycle is detected merge all 
	 * the nodes in that path 
	 * 
	 * @param node
	 */
    public void removeCycles( TaxonomyNode node ) {
        if( !nodes.get( node.getName() ).equals( node ) )
            throw new InternalReasonerException( "This node does not exist in the taxonomy: " + node.getName() );
        removeCycles( node, new ArrayList() );
    }

    /**
     * Given a node and (a possibly empty) path of sub nodes, remove cycles by
     * merging all the nodes in the path.
     * 
     * @param node
     * @param path
     * @return
     */
    private boolean removeCycles( TaxonomyNode node, List path ) {
        ATermUtils.assertTrue( this.nodes.containsValue( node ) );

        ATermUtils.assertTrue( this.nodes.containsKey( node.getName() ) );
	    
        // cycle detected
		if( path.contains( node ) ) {
		    mergeNodes( path );
		    return true;
		}
		else {
		    // no cycle yet, add this node to the path and continue
		    path.add( node );
		    
		    List supers = node.getSupers();
		    for(int i = 0; i < supers.size(); ) {
		        TaxonomyNode sup = (TaxonomyNode) supers.get( i );
		        // is there a cycle involving super node?
		        boolean cycle = removeCycles( sup, path );
		        // remove the super from the path
		        path.remove( sup );
//		        // if we merged this with the super
//		        if( isEquivalent( node.getName(), sup.getName() ) )
//		            System.err.println( "Merged with sup " + cycle );
		        if( !cycle ) i++;
		    }
		    return false;
		}
    }

    public void merge( TaxonomyNode node1, TaxonomyNode node2 ) {
        List mergeList = new ArrayList();
        mergeList.add( node1 );
        mergeList.add( node2 );
        
        mergeNodes( mergeList );
    }

    private void mergeNodes( List mergeList ) {
        if( log.isTraceEnabled() ) 
            log.trace( "Merge " + mergeList );
         
        if( mergeList.size() == 1 )
            log.warn( "Merge one node?" );
        
        TaxonomyNode node = null;
        if( mergeList.contains( TOP_NODE ) ) {
            node = TOP_NODE;
        }
        else if( mergeList.contains( BOTTOM_NODE ) ) {
            node = BOTTOM_NODE;
        }
        else
            node = (TaxonomyNode) mergeList.get( 0 );
        
        Set merged = new HashSet();
        merged.add( node );
        
        for(Iterator i = mergeList.iterator(); i.hasNext();) {
            TaxonomyNode other = (TaxonomyNode) i.next();
            
            if( merged.contains( other ) )
                continue;
            else
                merged.add( other );

            for(Iterator j = other.getSubs().iterator(); j.hasNext();) {
                TaxonomyNode sub = (TaxonomyNode) j.next();
                if( !mergeList.contains( sub ) )
                    node.addSub( sub );
            }
            
            for(Iterator j = other.getSupers().iterator(); j.hasNext();) {
                TaxonomyNode sup = (TaxonomyNode) j.next();
                if( !mergeList.contains( sup ) )
                    sup.addSub( node );
            }
            
            removeNode( other );
                        
            for(Iterator j = other.getEquivalents().iterator(); j.hasNext();) {
                ATermAppl c = (ATermAppl) j.next();
                addEquivalentNode( c, node );                
            }
        }
    }
    
    /**
     * Given a list of concepts, find all the Least Common Ancestors (LCA). Note that a taxonomy
     * is DAG not a tree so we do not have a unique LCA but a set of LCA. The function might
     * return a singleton list that contains TOP if there are no lower level nodes that
     * satisfy the LCA condition.
     * 
     * @param names
     * @return
     */
    public List computeLCA( ATermList list ) {
        // TODO this function can probably be improved 

        // get the first concept
        ATermAppl c = (ATermAppl) list.getFirst();
        
        // add all its ancestor as possible LCA candidates
        List ancestors = new ArrayList( getSupers( c, true, true ) );
        
        for( ; !list.isEmpty(); list = list.getNext() ) {
            c = (ATermAppl) list.getFirst();
            
            // take the intersection of possible candidates to get rid of uncommon ancestors
            ancestors.retainAll( getSupers( c, true, true ) );
            
            // we hit TOP so no need to continue
            if( ancestors.size() == 1 ) {
                ATermUtils.assertTrue( ancestors.contains( ATermUtils.TOP ) );
                return ancestors;
            }
        }
        
        // we have all common ancestors now remove the ones that have descendants in the list
        for(int j = 0; j < ancestors.size(); j++) {
            c = (ATermAppl) ancestors.get( j );
            ancestors.removeAll( getSupers( c, true, true ) );
        }        
        
        return ancestors;
    }

	public void print() {
		printer.print( this );		
	}
	
	public void print(OutputFormatter out) {
		printer.print( this, out );		
	}
}
