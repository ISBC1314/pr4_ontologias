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
 * @author kolovski
 */
public class XSDYearMonth extends BaseXSDAtomicType implements AtomicDatatype, XSDAtomicType {    
    private static XSDatatype dt = null;
    private static Calendar min = null;
    private static Calendar max = null;

    static {
        try {
            dt = DatatypeFactory.getTypeByName( "gYearMonth" );
            min = (Calendar) dt.createJavaObject( "0001-01", null );
            max = (Calendar) dt.createJavaObject( "9999-12", null );
        }
        catch( DatatypeException e ) {
            e.printStackTrace();
        }
    }
    
    private static final ValueSpace YEAR_MONTH_VALUE_SPACE = new YearMonthValueSpace();
    
    private static class YearMonthValueSpace extends AbstractValueSpace implements ValueSpace {          
        public YearMonthValueSpace() {
            super( min, null, max, false );
        }
        
        public int compare( Object a, Object b ) {
            return Comparators.calendarComparator.compare( a, b );
        }
        
        //return the difference in YearMonths
        public int count(Object start, Object end) {   
            Calendar calendarStart = (Calendar) start;          
            Calendar calendarEnd = (Calendar) end;      
            
            int numYears = (calendarEnd.get(Calendar.YEAR) - calendarStart.get(Calendar.YEAR) + 1);
            int numMonths = (calendarEnd.get(Calendar.MONTH) - calendarStart.get(Calendar.MONTH) + 1);
            
            return 12 * numYears + numMonths;            
        }       
        
        public boolean isValid( Object value ) {
            return (value instanceof Calendar);
        }
                        
        public Object getValue( String value ) {
            return (Calendar) dt.createJavaObject( value, null );       
        }

        public Object succ( Object value, int n ) {
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(((Calendar) value).getTime());         
            calendar.add( Calendar.MONTH, n );
            
            return calendar;
        }
    }
    
    public static XSDYearMonth instance = new XSDYearMonth(ATermUtils.makeTermAppl(Namespaces.XSD + "gYearMonth"));

    protected XSDYearMonth(ATermAppl name) {
        super( name, YEAR_MONTH_VALUE_SPACE );
    }

    public BaseXSDAtomicType create( GenericIntervalList intervals ) {
        XSDYearMonth type = new XSDYearMonth( null );
        type.values = intervals;

        return type;
    }

    public AtomicDatatype getPrimitiveType() {
        return instance;
    }   
}   
		