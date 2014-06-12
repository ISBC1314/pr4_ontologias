/*
 * Created on Nov 18, 2005
 */
package org.mindswap.pellet.utils;

import java.util.Calendar;
import java.util.Comparator;

public class Comparators {
    public static final Comparator comparator = new Comparator() {
        public int compare( Object o1, Object o2 ) {
            return ((Comparable) o1).compareTo( o2 );
        }
    };

    public static final Comparator hashCodeComparator = new Comparator() {
        public int compare( Object o1, Object o2 ) {
            return o1.hashCode() - o2.hashCode();
        }
    };

    public static final Comparator numberComparator = new Comparator() {
        public int compare( Object o1, Object o2 ) {
            Number n1 = (Number) o1;
            Number n2 = (Number) o2;

            return NumberUtils.compare( n1, n2 );
        }
    };

    public static final Comparator stringComparator = new Comparator() {
        public int compare( Object o1, Object o2 ) {
            return o1.toString().compareTo( o2.toString() );
        }
    };


    public static final Comparator calendarComparator = new Comparator() {
        public int compare( Object o1, Object o2 ) {
            long t1 = ((Calendar) o1).getTimeInMillis();
            long t2 = ((Calendar) o2).getTimeInMillis();

            if( t1 == t2 )
                return 0;
            else if( t1 < t2 )
                return -1;
            else
                return 1;
        }
    };
}
