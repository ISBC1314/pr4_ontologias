package org.mindswap.pellet.rete;


public class Constant implements Term {
	/** The value of the constant. */
    protected String value;

    /**
     * Creates a null constant.
     */
    public Constant() {
        this(null);
    }
    /**
     * Creates a constant.
     *
     * @param value                             the value of the constant
     */
    public Constant(String value) {    	
        this.value=value;
    }
    
      
    
    public String getValue() {
        return value;
    }
   
    /**
     * Converts this constant to a string.
     *
     * @return                                  the string representation of the constant
     */
    public String toString() {
        return value.toString();
    }
    /**
     * Checks if this constant is equal to some other constant.
     *
     * @param other                             the other constant
     * @return                                  <code>true</code> if this constant is equal to some other constant
     */
    public boolean equals(Object other) {
        if (this==other)
            return true;
        if (!(other instanceof Constant))
            return false;
        Object otherValue=((Constant)other).value;
        return value==otherValue || (value!=null && value.equals(otherValue));
    } 
    
    public int hashCode() {
        return value.hashCode();
    }
}
