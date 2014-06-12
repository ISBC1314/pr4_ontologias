/*
 * Created on Sep 16, 2004
 */
package org.mindswap.pellet.datatypes;

/**
 * Represent a string literal value. Optionally a language identifier can be assigned to the literal.
 * 
 * @author Evren Sirin
 */
public class StringValue {
	private String value;
	private String lang;
	
	private String intern;
	
	StringValue(String value) {
		this.value = value;
		
		lang = "";
		intern = "\"" + value + "\"";
		intern = intern.intern();
	}

	StringValue(String value, String lang) {
		this.value = value;
		this.lang = (lang == null) ? "" : lang;
		
		intern = "\"" + value + "\"@" + lang;
		intern = intern.intern();
	}

	public boolean equals(Object obj) {
		if(obj instanceof StringValue) {
			StringValue strVal = (StringValue) obj;
		
			return lang.equals(strVal.lang) && value.equals(strVal.value);
		}
		return false;
	}
	
	public int hashCode() {
	    // TODO Fix this 
	    // there is one corner case where the following two literals
	    // are treated equal
	    // 		<prop xml:lang="en">str</prop>
	    // 		<prop>"str"@en</prop>
		return intern.hashCode();
	}
	
	public String toString() {    
		return value;
	}
	
    /**
     * @return Returns the lang.
     */
    public String getLang() {
        return lang;
    }
    
    /**
     * @return Returns the value.
     */
    public String getValue() {
        return value;
    }
}