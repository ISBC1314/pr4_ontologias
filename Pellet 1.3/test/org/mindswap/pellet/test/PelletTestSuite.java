/*
 * Created on Oct 12, 2004
 */

package org.mindswap.pellet.test;

import junit.framework.TestSuite;

public class PelletTestSuite extends TestSuite {
    public static String base = "test_data/";
    
    public static TestSuite suite() {
        TestSuite suite = new TestSuite( "PelletTestSuite" );

        suite.addTest( WebOntTestSuite.suite() );
        suite.addTest( DLTestSuite.suite() );
        suite.addTest( MiscTests.suite() );
        suite.addTest( OWLAPITests.suite() );
        
        return suite;
    }

    
    public static void main(String args[]) { 
        junit.textui.TestRunner.run(suite());
    }
}
