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
public class XSDDateTime extends BaseXSDAtomicType implements AtomicDatatype, XSDAtomicType {	
    private static XSDatatype dt = null;
    private static Calendar min = null;
    private static Calendar max = null;

    static {
        try {
            dt = DatatypeFactory.getTypeByName( "dateTime" );
            min = (Calendar) dt.createJavaObject( "-9999-01-01T12:00:00", null );
            max = (Calendar) dt.createJavaObject( "9999-12-31T12:00:00", null );
        }
        catch( DatatypeException e ) {
            e.printStackTrace();
        }
    }

    private static final ValueSpace DATE_TIME_VALUE_SPACE = new DateTimeValueSpace();

    private static class DateTimeValueSpace extends AbstractValueSpace implements ValueSpace {
        public DateTimeValueSpace() {
            super( min, null, max, false );
        }

        public int compare( Object a, Object b ) {
            return Comparators.calendarComparator.compare( a, b );
        }

        //return the difference in Times
        public int count( Object start, Object end ) {
            long calendarStart = ((Calendar) start).getTimeInMillis();
            long calendarEnd = ((Calendar) end).getTimeInMillis();
            long diff = calendarStart - calendarEnd;

            if( diff > Integer.MAX_VALUE )
                return INFINITE;

            return (int) diff;
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

    public static XSDDateTime instance = new XSDDateTime( ATermUtils.makeTermAppl( Namespaces.XSD + "dateTime" ) );

    protected XSDDateTime( ATermAppl name ) {
        super( name, DATE_TIME_VALUE_SPACE );
    }

    public BaseXSDAtomicType create( GenericIntervalList intervals ) {
        XSDDateTime type = new XSDDateTime( null );
        type.values = intervals;

        return type;
    }

    public AtomicDatatype getPrimitiveType() {
        return instance;
    }
}
    