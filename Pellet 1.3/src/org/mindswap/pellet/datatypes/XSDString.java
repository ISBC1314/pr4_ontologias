/*
 * Created on May 29, 2004
 */
package org.mindswap.pellet.datatypes;

import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Namespaces;



/**
 * @author Evren Sirin
 */
public class XSDString extends BaseAtomicDatatype implements AtomicDatatype {
	public static XSDString instance = new XSDString();
	
	XSDString() {
		super(ATermUtils.makeTermAppl(Namespaces.XSD + "string"));
	}

	public AtomicDatatype getPrimitiveType() {
		return instance;
	}
	
	public Object getValue(String value, String datatypeURI) {
		return new StringValue( value );
	}
	
	public boolean contains(Object value) {
		return (value instanceof StringValue) && super.contains(value);
	}
	
	public Datatype deriveByRestriction(String facet, String value) throws UnsupportedOperationException {
	    throw new UnsupportedOperationException("xsd:string does not support facet " + facet);			
	}
}
