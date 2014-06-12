/*
 * Created on May 29, 2004
 */
package org.mindswap.pellet.datatypes;

import org.mindswap.pellet.utils.ATermUtils;


/**
 * @author Evren Sirin
 */
public class UnknownDatatype extends BaseAtomicDatatype implements AtomicDatatype {
	public static final UnknownDatatype instance = new UnknownDatatype();

	public UnknownDatatype() {
		super(ATermUtils.makeTermAppl("UnknownDatatype"));
	}

	public AtomicDatatype getPrimitiveType() {
		return instance;
	}

	public Object getValue(String value, String datatypeURI) {
		return value;
	}
}
