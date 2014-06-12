/*
 * Created on May 29, 2004
 */
package org.mindswap.pellet.datatypes;

import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Namespaces;


/**
 * @author Evren Sirin
 */
public class XSDDuration extends BaseAtomicDatatype implements AtomicDatatype {
	public static XSDDuration instance = new XSDDuration();
	

	class DurationValue {
		// TODO find the correct type for this
		Object value;
		
		DurationValue(Object value) {
			this.value = value;
		}
	}

	XSDDuration() {
		super(ATermUtils.makeTermAppl(Namespaces.XSD + "duration"));		
	}

	/* (non-Javadoc)
	 * @see org.mindswap.pellet.datatypes.AtomicDatatype#getPrimitiveType()
	 */
	public AtomicDatatype getPrimitiveType() {
		return instance;
	}

	/* (non-Javadoc)
	 * @see org.mindswap.pellet.datatypes.Datatype#getValue(java.lang.String)
	 */
	public Object getValue(String value) {
		return new DurationValue(value);
	}


	
	/* (non-Javadoc)
	 * @see org.mindswap.pellet.datatypes.Datatype#contains(java.lang.Object)
	 */
	public boolean contains(Object value) {
		return (value instanceof DurationValue) && super.contains(value);
	}

    /* (non-Javadoc)
     * @see org.mindswap.pellet.datatypes.Datatype#getValue(java.lang.String, java.lang.String)
     */
    public Object getValue(String value, String datatypeURI) {
		return new DurationValue(value);
    }
	
}
