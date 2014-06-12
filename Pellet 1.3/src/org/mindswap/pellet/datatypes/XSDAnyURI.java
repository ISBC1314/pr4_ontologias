/*
 * Created on May 29, 2004
 */
package org.mindswap.pellet.datatypes;

import java.net.URI;
import java.net.URISyntaxException;

import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Namespaces;

import aterm.ATermAppl;



/**
 * @author Evren Sirin
 */
public class XSDAnyURI extends BaseAtomicDatatype implements AtomicDatatype {
	public static XSDAnyURI instance = new XSDAnyURI();

	XSDAnyURI() {
		super(ATermUtils.makeTermAppl(Namespaces.XSD + "anyURI"));
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
	public Object getValue(String value, String datatypeURI) {
		try {
			return new URI(value.trim());
		} catch (URISyntaxException e) {
		    if(datatypeURI.equals(instance.name.getName())) {
		        System.err.println("Invalid xsd:anyURI value: '" + value + "'");
		        System.err.println(e);
		    }
			
			return null;
		}		
	}
	
	public boolean contains(Object value) {
		return (value instanceof URI) && super.contains(value);
	}

    public boolean isFinite() {
        return true;
    }

    public ATermAppl getValue( int n ) {
//        System.out.println(instance.getURI() + " " + this);
        return ATermUtils.makeTypedLiteral( "http://test" + n, name.getName());
    }	
}
