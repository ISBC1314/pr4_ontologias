/*
 * Created on Aug 9, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
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
 * @author kolovski
 */
public class XSDMonthDay extends BaseXSDAtomicType implements AtomicDatatype, XSDAtomicType {
    private static XSDatatype dt = null;

    private static Calendar min = null;

    private static Calendar max = null;

    static {
        try {
            dt = DatatypeFactory.getTypeByName( "gMonthDay" );
            min = (Calendar) dt.createJavaObject( "--01-01", null );
            max = (Calendar) dt.createJavaObject( "--12-31", null );
        }
        catch( DatatypeException e ) {
            e.printStackTrace();
        }
    }

    private static final ValueSpace MONTH_DAY_VALUE_SPACE = new MonthDayValueSpace();

    private static class MonthDayValueSpace extends AbstractValueSpace implements ValueSpace {
        public MonthDayValueSpace() {
            super( min, null, max, false );
        }

        public int compare( Object a, Object b ) {
            return Comparators.calendarComparator.compare( a, b );
        }

        //return the difference in MonthDays
        public int count( Object start, Object end ) {
            long calendarStart = ((Calendar) start).getTimeInMillis();
            long calendarEnd = ((Calendar) end).getTimeInMillis();
            long diff = calendarStart - calendarEnd;
            int numberOfDays = (int) (diff / (100 * 60 * 60 * 24));

            return numberOfDays;
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
            calendar.add( Calendar.DAY_OF_MONTH, n );

            return calendar;
        }
    }

    public static XSDMonthDay instance = new XSDMonthDay( ATermUtils.makeTermAppl( Namespaces.XSD
        + "gMonthDay" ) );

    protected XSDMonthDay( ATermAppl name ) {
        super( name, MONTH_DAY_VALUE_SPACE );
    }

    public BaseXSDAtomicType create( GenericIntervalList intervals ) {
        XSDMonthDay type = new XSDMonthDay( null );
        type.values = intervals;

        return type;
    }

    public AtomicDatatype getPrimitiveType() {
        return instance;
    }
}