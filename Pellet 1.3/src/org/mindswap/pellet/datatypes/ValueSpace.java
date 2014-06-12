/*
 * Created on Jan 23, 2006
 */
package org.mindswap.pellet.datatypes;

import java.util.Comparator;

public interface ValueSpace extends Comparator {
    public static final int INFINITE = -1;

    public Object getMidValue();
    
    public Object getMinValue();
    
    public Object getMaxValue();
    
    public boolean isInfinite();
    
    public boolean isInfinite( Object value );
    
    public boolean isValid( Object value );
    
    public Object getValue( String literal );
    
    public int compare(Object o1, Object o2);
    
    public int count( Object start, Object end );
    
    public Object succ( Object start, int n );
}