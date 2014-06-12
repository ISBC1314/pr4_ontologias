/*
 * Created on Oct 12, 2004
 */

package org.mindswap.pellet.test;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.mindswap.pellet.utils.AlphaNumericComparator;
import org.mindswap.pellet.utils.Comparators;

public class WebOntTestSuite extends TestSuite {
    public static String base = PelletTestSuite.base + "owl-test";

    private WebOntTest test;

    class OWLTestCase extends TestCase {
        File manifest;
        
        OWLTestCase( File manifest, String name ) {
            super( "OWLTestCase-" + name );
            this.manifest = manifest;
        }

        public void runTest() throws IOException {
            assertTrue( test.doSingleTest( manifest.toURL().toString() ) != WebOntTest.TEST_FAIL );
        }
    }
    
    public WebOntTestSuite() {
        super( "OWLTests" );
        
        test = new WebOntTest();
        test.setAvoidFailTests( true );
        test.setBase( base );
        test.setShowStats( WebOntTest.NO_STATS );
        
		File testDir = new File( base );
		File[] dirs = testDir.listFiles();
		
		Arrays.sort( dirs, Comparators.stringComparator );
		
		for (int i = 0; i < dirs.length; i++) {
			if(dirs[i].isFile()) continue;

			File[] files = dirs[i].listFiles(  new FileFilter() {
	            public boolean accept( File file ) {
	                return file.getName().indexOf( "Manifest" ) != -1;
	            }		    
			});
			
			Arrays.sort(files, AlphaNumericComparator.CASE_INSENSITIVE);
			
			for (int j = 0; j < files.length; j++)
				addTest( new OWLTestCase( files[j], dirs[i].getName() + "-" + files[j].getName() ) );
			
		}        
    }
    
    public static TestSuite suite() {
        return new WebOntTestSuite();
    }
    
    public static void main(String args[]) { 
        junit.textui.TestRunner.run(suite());
    }

}
