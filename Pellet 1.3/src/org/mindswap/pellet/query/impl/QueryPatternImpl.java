/*
 * Created on Jan 8, 2005
 */
package org.mindswap.pellet.query.impl;

import org.mindswap.pellet.exceptions.UnsupportedFeatureException;
import org.mindswap.pellet.query.QueryPattern;
import org.mindswap.pellet.query.QueryResultBinding;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.HashCodeUtil;

import aterm.ATermAppl;

/**
 * @author Evren Sirin
 *
 */
public class QueryPatternImpl implements QueryPattern {
    private ATermAppl s;
    private ATermAppl p;
    private ATermAppl o;
    
    private int hashCode;
    
    public QueryPatternImpl(ATermAppl ind, ATermAppl c) {
        if(ATermUtils.isVar( c ))
            throw new UnsupportedFeatureException();

        this.s = ind;
        this.o = c;        
        
        computeHashCode();
    }

    public QueryPatternImpl(ATermAppl s, ATermAppl p, ATermAppl o) {
        if(ATermUtils.isVar( p ))
            throw new UnsupportedFeatureException();

        this.s = s;
        this.p = p;
        this.o = o;
        
        computeHashCode();
    }

    public boolean isTypePattern() {
        return (p == null);
    }

    public boolean isEdgePattern() {
        return (p != null);
    }

    public boolean isGround() {
        return !ATermUtils.isVar( s ) && !ATermUtils.isVar( o ) ;
    }

    public ATermAppl getSubject() {
        return s;
    }

    public ATermAppl getPredicate() {
        return p;
    }
    
    public ATermAppl getObject() {
        return o;
    }
    
    public QueryPattern apply( QueryResultBinding binding ) {
	    ATermAppl subj = binding.hasValue( s ) ? binding.getValue( s ) : s;
	    ATermAppl obj = binding.hasValue( o ) ? binding.getValue( o ) : o;
	    
	    if( p == null )
	        return new QueryPatternImpl( subj, obj );
	    else
	        return new QueryPatternImpl( subj, p, obj );
    }
    
	
    public boolean equals(Object other) {
        if(this == other) return true;
        if(!(other instanceof QueryPatternImpl)) return false;
        QueryPatternImpl that = (QueryPatternImpl) other;
        return s.equals(that.s) && 
               o.equals(that.o) &&
               ((p == null && that.p == null) ||
                (p != null && that.p != null && p.equals(that.p)));
    }

    private void computeHashCode() {
        hashCode = HashCodeUtil.SEED;
        hashCode = HashCodeUtil.hash(hashCode, s);
        hashCode = HashCodeUtil.hash(hashCode, p);
        hashCode = HashCodeUtil.hash(hashCode, o);            
    }
    
    public int hashCode() {
        return hashCode;
    }
    
    public String toString() {
        if(p== null)
            return "(" + s + " rdf:type " + o + ")"; 
        else
            return "(" + s + " " + p +  " " + o + ")";
    }
}
