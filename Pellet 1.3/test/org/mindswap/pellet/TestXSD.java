package org.mindswap.pellet;

import org.relaxng.datatype.DatatypeException;

import com.sun.msv.datatype.xsd.DatatypeFactory;
import com.sun.msv.datatype.xsd.XSDatatype;

public class TestXSD {
	public static void main( String args[] ) throws DatatypeException {
		XSDatatype dt = DatatypeFactory.getTypeByName("byte");
		String value = "127.0";
		        
	    // if the name is not recognized, null is returned.
		if(dt==null) {
			System.out.println("no such type");
			System.exit(0);
		}

		// check validity.
		boolean valid = dt.isValid( value, null );

		System.out.println( "Value : " + value );
		System.out.println( "Type  : " + dt.getName() );
		System.out.println( "Valid : " + valid );
		

		if( valid ) {
			System.out.println( "Java type : " + dt.getJavaObjectType() );
			
			Object obj = dt.createJavaObject( value, null );
			System.out.println( "Java value: " + dt.createJavaObject( value, null ) );
			System.out.println( "Serialized: " + dt.serializeJavaObject( obj, null ) );			
		}
		else {
			// call diagnose method to see what is wrong.
			try {
				dt.checkValid( value, null );
				System.out.println( "confused?" );
			}
			catch( DatatypeException diag ) {
				if( diag.getMessage()==null ) {
					// datatype object may not support diagnosis.
					// in that case, UnsupportedOperationException is thrown.
					System.out.println("Reason : no diagnosys available");
				} 
				else {
					System.out.println("Reason: " + diag.getMessage() );
				}
			}
		}
	}
}
