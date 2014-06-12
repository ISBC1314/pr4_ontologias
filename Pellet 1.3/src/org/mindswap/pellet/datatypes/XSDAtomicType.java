/*
 * Created on Jul 19, 2005
 */
package org.mindswap.pellet.datatypes;


/**
 * @author Evren Sirin
 *
 */
public interface XSDAtomicType extends AtomicDatatype {
   	public XSDAtomicType deriveByRestriction( int facet, Object value );
    
    public XSDAtomicType restrictMin( boolean inclusive, Object value ) ;

    public XSDAtomicType restrictMinInclusive( Object value );

    public XSDAtomicType restrictMinExclusive( Object value );

    public XSDAtomicType restrictMax( boolean inclusive, Object value ) ;

    public XSDAtomicType restrictMaxInclusive( Object value );

    public XSDAtomicType restrictMaxExclusive( Object value );
    
    public XSDAtomicType restrictTotalDigits( int digits );

    public XSDAtomicType restrictFractionDigits( int digits );

    public XSDAtomicType restrictPattern( String pattern);

}
