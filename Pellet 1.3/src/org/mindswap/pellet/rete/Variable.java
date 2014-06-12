package org.mindswap.pellet.rete;

import java.net.URI;

public class Variable implements Term, Comparable {
	
    /** The name of the variable. */
    protected String name;

    /**
     * Creates a variable.
     *
     * @param variableName                      the name of the variable
     */
    public Variable(String variableName) {      
    	name = variableName;
    }
    /**
     * Returns the name of this variable.
     *
     * @return                                  the name of this variable
     */
    public String getVariableName() {
        return name;
    }
   
    /**
     * Converts this variable to a string.
     *
     * @return                                  the string representation of the variable
     */
    public String toString() {
        return name.toString();
    }
    /**
     * Checks if this variable is equal to some other variable.
     *
     * @param other                             the other object
     * @return                                  <code>true</code> if this variable is equal to some other variable
     */
    public boolean equals(Object other) {
        if (this==other)
            return true;
        if (!(other instanceof Variable))
            return false;
        return name.equals(((Variable)other).name);
    }    
    
    public int hashCode() {
        return name.hashCode();
    }
	public int compareTo(Object arg0) {
		Variable t = (Variable) arg0;
		return this.name.compareTo(t.getVariableName());
	}
}
