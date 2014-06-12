/*
 * Created on Oct 12, 2004
 */

package org.mindswap.pellet.test;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.mindswap.pellet.utils.AlphaNumericComparator;

public class DLTestSuite extends TestSuite {
    public static String base = PelletTestSuite.base + "dl-benchmark/";

    private DLBenchmarkTest test = new DLBenchmarkTest();
       
    class DLTestCase extends TestCase {
        File name;
        boolean tbox;
        
        DLTestCase( File name, boolean tbox ) {
            super( "DLTestCase-" + name.getName() );
            this.name = name;
            this.tbox = tbox;
        }

        public void runTest() throws Exception {
            boolean pass = false;
            
            if( tbox )
                pass = test.doTBoxTest( name.getAbsolutePath() );
            else
                pass = test.doABoxTest( name.getAbsolutePath() );
            
            assertTrue( pass );
        }
    } 
    
    public DLTestSuite() {
        super( "DLTests" );
        
		File dir = new File( base + "tbox" );
		File[] files = dir.listFiles( new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return dir != null && name.endsWith(".tkb");
			}			
		});

		Arrays.sort(files, AlphaNumericComparator.CASE_INSENSITIVE);
		
		for (int i = 0; i < files.length; i++) {
			addTest( new DLTestCase( files[i], true ) );			
		}        
    }
    
    public static TestSuite suite() {
        return new DLTestSuite();
    }
}
