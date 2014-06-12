/*
 * Created on Oct 2, 2005
 */
package org.mindswap.pellet.utils;

/**
 * @author Evren Sirin
 *
 */
public class Bool {
    public final static Bool FALSE   = new Bool();
    public final static Bool TRUE    = new Bool();
    public final static Bool UNKNOWN = new Bool();
    
    private Bool() {
    }
    
    public static Bool create( boolean value ) {
        return value ? TRUE : FALSE;
    }
    
    public Bool not() {
        if( this == TRUE )
            return FALSE;
        
        if( this == FALSE )
            return TRUE;
        
        return UNKNOWN;       
    }

    public Bool or( Bool other ) {
        if( this == TRUE && other != UNKNOWN )
            return TRUE;
        
        if( this != UNKNOWN && other == TRUE )
            return TRUE;
        
        return UNKNOWN;       
    }
    
    public Bool and( Bool other ) {
        if( this == TRUE && other == TRUE )
            return TRUE;
        
        if( this == UNKNOWN || other == UNKNOWN )
            return FALSE;
        
        return UNKNOWN;       
    }
    
    public boolean isTrue() {
        return this == TRUE;
    }
    
    public boolean isFalse() {
        return this == FALSE;        
    }

    public boolean isUnknown() {
        return this == UNKNOWN;        
    }

    public boolean isKnown() {
        return this != UNKNOWN;        
    }
    
    public boolean equals( Object obj ) {
        return this == obj;
    }
    
    public String toString() {
        return isTrue() ? "true" : isFalse() ? "false" : "unknown";
    }
}
