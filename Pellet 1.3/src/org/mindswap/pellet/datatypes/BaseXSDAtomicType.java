/*
 * Created on May 29, 2004
 */
package org.mindswap.pellet.datatypes;

import java.util.Iterator;
import java.util.Set;

import org.apache.xerces.impl.dv.XSSimpleType;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.GenericIntervalList;

import aterm.ATermAppl;

/**
 * @author Evren Sirin
 */
public abstract class BaseXSDAtomicType extends BaseDatatype implements XSDAtomicType {
    protected GenericIntervalList values;

    protected ValueSpace valueSpace;

    protected BaseXSDAtomicType( ATermAppl name, ValueSpace valueSpace ) {
        super( name );

        this.valueSpace = valueSpace;
        this.values = new GenericIntervalList( valueSpace.getMinValue(), valueSpace.getMaxValue(), valueSpace );
    }

    public abstract BaseXSDAtomicType create( GenericIntervalList intervals );
    
    public ValueSpace getValueSpace() {
        return valueSpace;
    }
    
    public Object getValue( String value, String datatypeURI ) {
        try {
            return valueSpace.getValue( value.trim() );
        }
        catch( NumberFormatException e ) {
            return null;
        }
    }

    public AtomicDatatype not() {
        return getPrimitiveType().difference( this );
    }

    public AtomicDatatype intersection( AtomicDatatype dt ) {
        if( this == dt )
            return this;

        GenericIntervalList result = new GenericIntervalList( valueSpace );
        if( dt instanceof BaseXSDAtomicType ) {
            BaseXSDAtomicType other = (BaseXSDAtomicType) dt;

            GenericIntervalList original = new GenericIntervalList( values );
            Iterator it = other.values.iterator();
            while( it.hasNext() ) {
                GenericIntervalList.Interval interval = (GenericIntervalList.Interval) it.next();
                GenericIntervalList o = new GenericIntervalList( original );
                o.restrictToInterval( interval );
                result.addIntervalList( o );
            }
        }

        return create( result );
    }

    public AtomicDatatype union( AtomicDatatype dt ) {
        if( this == dt )
            return this;

        GenericIntervalList result = new GenericIntervalList( valueSpace );
        if( dt instanceof BaseXSDAtomicType ) {
            BaseXSDAtomicType other = (BaseXSDAtomicType) dt;

            result.addIntervalList( values );
            result.addIntervalList( other.values );
        }

        return create( result );
    }

    public AtomicDatatype difference( AtomicDatatype dt ) {
        if( this == dt )
            return EmptyDatatype.instance;

        GenericIntervalList result = new GenericIntervalList( valueSpace );
        if( dt instanceof BaseXSDAtomicType ) {
            BaseXSDAtomicType other = (BaseXSDAtomicType) dt;

            result.addIntervalList( values );
            result.removeIntervalList( other.values );
        }

        return create( result );
    }

    public AtomicDatatype enumeration( Set enum_ ) {
        GenericIntervalList result = new GenericIntervalList( valueSpace );
        for( Iterator i = enum_.iterator(); i.hasNext(); ) {
            Number number = (Number) i.next();
            result.addInterval( number, number );
        }

        return create( result );
    }

    public Datatype singleton( Object value ) {
        GenericIntervalList result = new GenericIntervalList( valueSpace );
        if( value instanceof Number ) {
            Number number = (Number) value;
            result.addInterval( number, number );
        }

        return create( result );
    }

    public int size() {
        return values.count();
    }

    public ATermAppl getValue( int i ) {
        // System.out.println(instance.getURI() + " " + i);
        return ATermUtils.makeTypedLiteral( values.get( i ).toString(), getPrimitiveType().getURI() );
    }

    public boolean contains( Object value ) {
        if( valueSpace.isValid( value ) )
            return values.contains( value );

        return false;
    }
        
    public XSDAtomicType deriveByRestriction( int facet, Object value ) throws UnsupportedOperationException {
        try {
            Object start = valueSpace.getMinValue(), end = valueSpace.getMaxValue();
            boolean incStart = true, incEnd = true;
            if( facet == XSSimpleType.FACET_MININCLUSIVE ) {
                start = value;
            }
            else if( facet == XSSimpleType.FACET_MINEXCLUSIVE ) {
                start = value;
                incStart = false;           
            }
            else if( facet == XSSimpleType.FACET_MAXINCLUSIVE ) {
                end = value;
            }
            else if( facet == XSSimpleTypeDefinition.FACET_MAXEXCLUSIVE )  {
                end = value;
                incEnd = false;           
            }
//          else if( facet == XSSimpleType.FACET_TOTALDIGITS ) {
//              int digits = Integer.parseInt((String)value);
//              start = 10^digits;
//              end = 10^(digits+1)-1;
//          }
//          else if( facet == XSSimpleType.FACET_WHITESPACE ) {
//              return this;
//          }           
//          else
//              throw new UnsupportedOperationException("xsd:decimal does not support facet " + facet);
            else 
                return this;
            
            // create an interval from the min max values
            GenericIntervalList intervalList = new GenericIntervalList( values );
            // intersect the current interval list with this restriction
            intervalList.restrictToInterval( start, incStart, end, incEnd );
            // derive the new type 
            return create( intervalList );
        } catch (Exception e) {
            e.printStackTrace();
            throw new UnsupportedOperationException("Value " + value + " is not valid for the facet " + facet);
        }       
    }
    
    public XSDAtomicType restrictMin( boolean inclusive, Object value ) {      
        return inclusive
            ? deriveByRestriction( XSSimpleType.FACET_MININCLUSIVE, value )
            : deriveByRestriction( XSSimpleType.FACET_MINEXCLUSIVE, value );
    }

    public XSDAtomicType restrictMinInclusive( Object value ) {
        return deriveByRestriction( XSSimpleType.FACET_MININCLUSIVE, value );
    }

    public XSDAtomicType restrictMinExclusive( Object value ) {
        return deriveByRestriction( XSSimpleType.FACET_MINEXCLUSIVE, value );
    }

    public XSDAtomicType restrictMax( boolean inclusive, Object value ) {
        return inclusive
            ? deriveByRestriction( XSSimpleType.FACET_MAXINCLUSIVE, value )
            : deriveByRestriction( XSSimpleType.FACET_MAXEXCLUSIVE, value );        
    }

    public XSDAtomicType restrictMaxInclusive( Object value ) {
        return deriveByRestriction( XSSimpleType.FACET_MAXINCLUSIVE, value );
    }

    public XSDAtomicType restrictMaxExclusive( Object value ) {
        return deriveByRestriction( XSSimpleType.FACET_MAXEXCLUSIVE, value );
    }
    
    public XSDAtomicType restrictTotalDigits( int digits ) {
        return deriveByRestriction( XSSimpleType.FACET_TOTALDIGITS, new Integer( digits ) );
    }

    public XSDAtomicType restrictFractionDigits( int digits ) {
        return deriveByRestriction( XSSimpleType.FACET_FRACTIONDIGITS, new Integer( digits ) );        
    }

    public XSDAtomicType restrictPattern( String pattern ) {
        return deriveByRestriction( XSSimpleType.FACET_PATTERN, pattern );        
    }
}
