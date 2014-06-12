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
public class XSDFloat extends BaseXSDAtomicType implements AtomicDatatype {
    private static final Object min  = new Float( Float.MIN_VALUE );
    private static final Object zero = new Float( 0.0 );
    private static final Object max  = new Float( Float.MAX_VALUE );

    private  static final ValueSpace FLOAT_VALUE_SPACE = new FloatValueSpace();

    private  static class FloatValueSpace extends AbstractValueSpace implements ValueSpace {
        public FloatValueSpace() {
            super( min, zero, max, true );
        }
        
        public boolean isValid( Object value ) {
            return (value instanceof Float);
        }
                
        public Object getValue( String literal ) {
            return NumberUtils.parseFloat( literal );
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

    public static XSDFloat instance = new XSDFloat( ATermUtils.makeTermAppl( Namespaces.XSD + "float" ) );

    protected XSDFloat( ATermAppl name ) {
        super( name, FLOAT_VALUE_SPACE );
    }

    public BaseXSDAtomicType create( GenericIntervalList intervals ) {
        XSDFloat type = new XSDFloat( null );
        type.values = intervals;

        return type;
    }

    public AtomicDatatype getPrimitiveType() {
        return instance;
    }
}
