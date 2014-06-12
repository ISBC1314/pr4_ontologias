/*
 * Created on May 29, 2004
 */
package org.mindswap.pellet.datatypes;

import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.GenericIntervalList;
import org.mindswap.pellet.utils.Namespaces;
import org.mindswap.pellet.utils.NumberUtils;

import aterm.ATermAppl;

/**
 * @author Evren Sirin
 */
public class XSDDecimal extends BaseXSDAtomicType implements AtomicDatatype, XSDAtomicType {
    private static final Object min = new String( "-Inf" );
    private static final Object zero = new Byte( (byte) 0 );
    private static final Object max = new String( "+Inf" );

    private static final ValueSpace DECIMAL_VALUE_SPACE = new DecimalValueSpace();

    private static class DecimalValueSpace extends AbstractValueSpace implements ValueSpace {
        public DecimalValueSpace() {
            super( min, zero, max, true );
        }
                
        public boolean isValid( Object value ) {
            return (value instanceof Number);
        }
        
        public Object getValue( String literal ) {
            return NumberUtils.parseDecimal( literal );
        }

        public int compare( Object a, Object b ) {
            Integer cmp = compareInternal( a, b );
            if( cmp != null )
                return cmp.intValue();

            return NumberUtils.compare( (Number) a, (Number) b );
        }

        public int count( Object start, Object end ) {
            Integer cmp = countInternal( start, end );
            if( cmp != null )
                return cmp.intValue();

            // FIXME
            long count = 1;
            count += ((Number) end).longValue();
            count -= ((Number) start).longValue();

            return count > Integer.MAX_VALUE ? INFINITE : (int) count;
        }

        public Object succ( Object start, int n ) {
            if( isInfinite( start ) )
                throw new IllegalArgumentException( "Cannot handle infinite values" );

            return NumberUtils.add( (Number) start, n );
        }
    }

    public static XSDDecimal instance = new XSDDecimal( ATermUtils.makeTermAppl( Namespaces.XSD + "decimal" ) );

    protected XSDDecimal( ATermAppl name ) {
        super( name, DECIMAL_VALUE_SPACE );
    }

    public BaseXSDAtomicType create( GenericIntervalList intervals ) {
        XSDDecimal type = new XSDDecimal( null );
        type.values = intervals;

        return type;
    }

    public AtomicDatatype getPrimitiveType() {
        return instance;
    }
}
