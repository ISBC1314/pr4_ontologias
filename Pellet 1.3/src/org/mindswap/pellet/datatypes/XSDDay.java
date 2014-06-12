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
public class XSDDay extends BaseXSDAtomicType implements AtomicDatatype, XSDAtomicType {	
    private static XSDatatype dt = null;
    private static Calendar min = null;
    private static Calendar max = null;

    static {
        try {
            dt = DatatypeFactory.getTypeByName( "gDay" );
            min = (Calendar) dt.createJavaObject( "---01", null );
            max = (Calendar) dt.createJavaObject( "---31", null );
        }
        catch( DatatypeException e ) {
            e.printStackTrace();
        }
    }
    
    private static final ValueSpace DAY_VALUE_SPACE = new DayValueSpace();
    
    private static class DayValueSpace extends AbstractValueSpace implements ValueSpace {	       
        public DayValueSpace() {
            super( min, null, max, false );
        }
        
		public int compare( Object a, Object b ) {
            return Comparators.calendarComparator.compare( a, b );
        }
		
		//return the difference in Days
		public int count(Object start, Object end) {			
			Calendar calendarStart = (Calendar) start;			
			Calendar calendarEnd = (Calendar) end; 		
			
			return calendarEnd.get(Calendar.DAY_OF_MONTH) - calendarStart.get(Calendar.DAY_OF_MONTH) + 1;			
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
            calendar.add( Calendar.DAY_OF_MONTH, n );
            
            return calendar;
        }
	}
    
	public static XSDDay instance = new XSDDay(ATermUtils.makeTermAppl(Namespaces.XSD + "gDay"));

	protected XSDDay(ATermAppl name) {
        super( name, DAY_VALUE_SPACE );
	}

    public BaseXSDAtomicType create( GenericIntervalList intervals ) {
        XSDDay type = new XSDDay( null );
        type.values = intervals;

        return type;
    }

    public AtomicDatatype getPrimitiveType() {
        return instance;
    }	
}