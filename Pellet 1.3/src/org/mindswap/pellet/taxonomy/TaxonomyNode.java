/*
 * Created on Mar 14, 2005
 */
package org.mindswap.pellet.taxonomy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.SetUtils;

import aterm.ATermAppl;

/**
 * @author Evren Sirin
 *
 */
public class TaxonomyNode {
    private ATermAppl name;
    
    private boolean hidden;
    
    private Set equivalents;
    private List supers;
    private List subs;
    
    private Set instances;
    
    public TaxonomyNode( ATermAppl name, boolean hidden ) {        
        this.name = name;
        this.hidden = hidden;
                
        equivalents = Collections.singleton( name );
        
        if( name.equals( ATermUtils.TOP ) ) {
            supers = Collections.EMPTY_LIST;
            subs = new ArrayList();
        }
        else if( name.equals( ATermUtils.BOTTOM ) ) {
            supers = new ArrayList();
            subs = Collections.EMPTY_LIST;
        } 
        else {
            supers = new ArrayList( 2 );
            subs = new ArrayList();
        }
    }
    
    public boolean isHidden() {
        return hidden;
    }
    
    public boolean contains( ATermAppl c ) {
        return equivalents.contains( c );
    }
    
    public void addEquivalent(ATermAppl c) {
        if( equivalents.size() == 1 )
            equivalents = new HashSet( equivalents );
        
        equivalents.add( c );
    }
    
    public void addSub( TaxonomyNode other ) {
        if( this.equals( other ) || subs.contains( other ) )
            return;
        
        subs.add( other );
        if( !hidden )
            other.supers.add( this );
    }

    public void addSubs( Collection others ) {
        subs.addAll( others );
        if( !hidden ) {
	        for(Iterator i = others.iterator(); i.hasNext();) {
	            TaxonomyNode other = (TaxonomyNode) i.next();
	            other.supers.add( this );
	        }        
        }
    }
      
    public void addSupers( Collection others ) {
        supers.addAll( others );
        if( !hidden ) {
	        for(Iterator i = others.iterator(); i.hasNext();) {
	            TaxonomyNode other = (TaxonomyNode) i.next();
	            other.subs.add( this );
	        }        
        }
    }
    
    public void removeSub(TaxonomyNode other) {
        subs.remove( other );
        other.supers.remove( this );
    }
    
	public void disconnect() {
        for(Iterator j = subs.iterator(); j.hasNext();) {
            TaxonomyNode sub = (TaxonomyNode) j.next();
            j.remove();
            sub.supers.remove( this );
        }
        
        for(Iterator j = supers.iterator(); j.hasNext();) {
            TaxonomyNode sup = (TaxonomyNode) j.next();
            j.remove();
            sup.subs.remove( this );
        } 
	}
    
    public void addInstance(ATermAppl ind) {
        if(instances == null)
            instances = new HashSet();
        instances.add(ind);
    }
	
    public ATermAppl getName() {
        return name;
    }
    
    public Set getEquivalents() {
        return equivalents;
    }
    
    public Set getInstances() {
        return (instances == null) ? SetUtils.EMPTY_SET : instances;
    }
    
    public List getSubs() {
        return subs;
    }
    
    public List getSupers() {
        return supers;
    }
    
    public void removeMultiplePaths() {
        if( !hidden ) {
			for(Iterator i1 = supers.iterator();  i1.hasNext(); ) {
				TaxonomyNode sup = (TaxonomyNode) i1.next();
				 
				for(Iterator i2 = subs.iterator(); i2.hasNext(); ) {
				    TaxonomyNode sub = (TaxonomyNode) i2.next();
				    
					sup.removeSub( sub );
				}
			}
        }
    }
    
    public void print() {
        print( "" );
    }
    
    public void print( String indent ) {
        if( subs.isEmpty() ) return;
        
        System.out.print( indent );
        System.out.println( equivalents + "(" + hashCode() + ")");
        
        indent += "  ";
        for(Iterator j = subs.iterator(); j.hasNext();) {
            TaxonomyNode sub = (TaxonomyNode) j.next();
            sub.print( indent );
        }
    }

    public String toString() {
        return name + " = " + equivalents;
    }
}
