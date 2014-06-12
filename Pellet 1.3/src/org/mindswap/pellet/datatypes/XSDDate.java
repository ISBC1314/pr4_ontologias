/*
 * Created on May 29, 2004
 */
package org.mindswap.pellet.datatypes;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Comparators;
import org.mindswap.pellet.utils.GenericIntervalList;
import org.mindswap.pellet.utils.Namespaces;
import org.relaxng.datatype.DatatypeException;

import aterm.ATermAppl;

import com.sun.msv.datatype.xsd.DatatypeFactory;
import com.sun.msv.datatype.xsd.XSDatatype;


/**
 * @author Evren Sirin
 */
public class XSDDate extends BaseXSDAtomicType implements AtomicDatatype, XSDAtomicType {
    private static XSDatatype dt = null;
    private static Calendar min = null;
    private static Calendar max = null;

    static {
        try {
            dt = DatatypeFactory.getTypeByName( "date" );
            min = (Calendar) dt.createJavaObject( "-9999-01-01", null );
            max = (Calendar) dt.createJavaObject( "9999-12-31", null );
        }
        catch( DatatypeException e ) {
            e.printStackTrace();
        }
    }

    private static final ValueSpace DATE_VALUE_SPACE = new DateValueSpace();

    private static class DateValueSpace extends AbstractValueSpace implements ValueSpace {
        public DateValueSpace() {
            super( min, null, max, false );
        }

        public int compare( Object a, Object b ) {
            return Comparators.calendarComparator.compare( a, b );
        }

        //return the difference in Dates
        public int count( Object start, Object end ) {
            double milliElapsed = ((Calendar) end).getTimeInMillis() - ((Calendar) start).getTimeInMillis();
            double daysElapsed = (milliElapsed / 24F / 3600F / 1000F);
            
            return Math.round(Math.round(daysElapsed * 100F) / 100F);
        }
        
        public boolean isValid( Object value ) {
            return (value instanceof Calendar);
        }
        
        public Object getValue( String value ) {
            return (Calendar) dt.createJavaObject( value, null );
        }

        public Object succ( Object value, int n ) {
            Calendar calendar = new GregorianCalendar();
            calendar.setTime( ((Calendar) value).getTime() );
            calendar.add( Calendar.MILLISECOND, n );

            return calendar;
        }
    }

    public static XSDDate instance = new XSDDate( ATermUtils.makeTermAppl( Namespaces.XSD + "date" ) );

    protected XSDDate( ATermAppl name ) {
        super( name, DATE_VALUE_SPACE );
    }

    public BaseXSDAtomicType create( GenericIntervalList intervals ) {
        XSDDate type = new XSDDate( null );
        type.values = intervals;

        return type;
    }

    public AtomicDatatype getPrimitiveType() {
        return instance;
    }
}
