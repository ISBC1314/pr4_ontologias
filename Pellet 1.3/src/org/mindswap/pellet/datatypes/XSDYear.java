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
public class XSDYear extends BaseXSDAtomicType implements AtomicDatatype, XSDAtomicType {    
    private static XSDatatype dt = null;
    private static Calendar min = null;
    private static Calendar max = null;

    static {
        try {
            dt = DatatypeFactory.getTypeByName( "gYear" );
            min = (Calendar) dt.createJavaObject( "-9999", null );
            max = (Calendar) dt.createJavaObject( "9999", null );
        }
        catch( DatatypeException e ) {
            e.printStackTrace();
        }
    }
    
    private static final ValueSpace YEAR_VALUE_SPACE = new YearValueSpace();
    
    private static class YearValueSpace extends AbstractValueSpace implements ValueSpace {          
        public YearValueSpace() {
            super( min, null, max, false );
        }
        
        public int compare( Object a, Object b ) {
            return Comparators.calendarComparator.compare( a, b );
        }

        //return the difference in Years
        public int count(Object start, Object end) {            
            Calendar calendarStart = (Calendar) start;          
            Calendar calendarEnd = (Calendar) end;      
            
            return calendarEnd.get(Calendar.YEAR) - calendarStart.get(Calendar.YEAR) + 1;           
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
            calendar.add( Calendar.YEAR, n );
            
            return calendar;
        }
    }
    
    public static XSDYear instance = new XSDYear(ATermUtils.makeTermAppl(Namespaces.XSD + "gYear"));

    protected XSDYear(ATermAppl name) {
        super( name, YEAR_VALUE_SPACE );
    }

    public BaseXSDAtomicType create( GenericIntervalList intervals ) {
        XSDYear type = new XSDYear( null );
        type.values = intervals;

        return type;
    }

    public AtomicDatatype getPrimitiveType() {
        return instance;
    }   
}   