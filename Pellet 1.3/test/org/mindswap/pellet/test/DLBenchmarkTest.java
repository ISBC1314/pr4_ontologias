// The MIT License
//
// Copyright (c) 2003 Ron Alford, Mike Grove, Bijan Parsia, Evren Sirin
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to
// deal in the Software without restriction, including without limitation the
// rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
// sell copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
// IN THE SOFTWARE.

package org.mindswap.pellet.test;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mindswap.pellet.KRSSLoader;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.exceptions.TimeoutException;
import org.mindswap.pellet.output.TableData;
import org.mindswap.pellet.utils.AlphaNumericComparator;
import org.mindswap.pellet.utils.Timer;

/**
 * Parse and test the cases from DL benchmark suite. This class provides parsing
 * for KRSS files.
 * 
 * @author Evren Sirin
 */
public class DLBenchmarkTest {
	public static boolean DEBUG = false;
	public static boolean PRINT_TIME = false;
	public static boolean PRINT_TREE = false;
	
	// time limits for different kind of tests
	public static int SAT_LIMIT  = 10;
	public static int TBOX_LIMIT = 100;
	public static int ABOX_LIMIT = 200;
	
	public static boolean FAST = false;
	public static boolean FORCE_UPPERCASE = true;
	
	private KRSSLoader loader;
	private KnowledgeBase kb;

	static class ExtensionFilter implements FilenameFilter {
	    private String extension;
	    
	    public ExtensionFilter( String pattern ) {
	        this.extension = pattern;
	    }
	    
		public boolean accept( File dir, String name ) {
			return dir != null && name.endsWith( extension ) 
			&& name.indexOf("-roles")==-1
			&& name.indexOf("-cd")==-1
			&& name.indexOf("pdwq")==-1
			&& name.indexOf("people")==-1
			&& name.indexOf("veda-all")==-1
			;
		}			
	};
	
	public DLBenchmarkTest() {
	    loader = new KRSSLoader();
	    loader.setForceUppercase( FORCE_UPPERCASE );
	}

	public KnowledgeBase initKB(long timeout) {
	    KnowledgeBase kb = new KnowledgeBase();
		kb.setTimeout(timeout * 1000);
        
		return kb;
	}
	
	public void doAllTBoxTests(String dirName) throws Exception {
		File dir = new File(dirName);
		String[] files = dir.list(new ExtensionFilter( ".tkb"));
		Arrays.sort(files, AlphaNumericComparator.CASE_INSENSITIVE);
		
		TableData table = new TableData(Arrays.asList(new String[] { "Name", "Size", "Time"} ));
		for(int i = 0; i < files.length; i++) {
			System.out.print((i+1) + ") ");

			List data = new ArrayList();	
			data.add( files[i] );
			try {
				doTBoxTest(dirName + files[i]);
				data.add(new Integer(kb.getClasses().size()));
				data.add(kb.timers.getTimer("test").getTotal() + "");
			} catch(TimeoutException e) {
				System.out.println(" ** Timeout: " + e.getMessage() + " ** ");
			} catch(Exception e) {
				e.printStackTrace(System.err);				
				System.out.println();
			} catch(OutOfMemoryError e) {
				System.out.println(" ** Out of Memory ** ");
			} catch(StackOverflowError e) {
				System.out.println(" ** Stack Overflow ** ");			
			} catch(Error e) {
				e.printStackTrace(System.err);				
			}
			table.add(data);
		}		
		
		System.out.print( table );
	}
	
	public boolean doTBoxTest( String file ) throws Exception {
	    String ext = ".tkb";
		int index = file.lastIndexOf('.');
		if(index != -1) {
		    ext = file.substring( index );
		    file = file.substring( 0, index );
		}
		index = file.lastIndexOf(File.separator);
		String displayName = (index == -1) ? file : file.substring(index + 1);
		System.out.print(displayName + " ");
		
		kb = initKB(TBOX_LIMIT);		
		loader.load( file + ext, kb  );
		
		Timer t = kb.timers.startTimer( "test" );
		
		System.out.print("preparing...");
		
		kb.prepare();
		
		System.out.print("classifying...");
		
		kb.classify();
				
		t.stop();
		
		if(PRINT_TREE) kb.printClassTree();
		
		System.out.print("verifying...");
//		loader.verifyTBox( file + ".tree", kb );
		
		System.out.print("done");
		
		System.out.print( " Prepare " + kb.timers.getTimer("preprocessing").getTotal() );
		System.out.print( " Classify " + kb.timers.getTimer("classify").getTotal() );
		
		System.out.println( " " + t.getTotal() );
		
		if(PRINT_TIME) kb.timers.print();
		
		return true;
	}
	

	public void doAllSatTests(String dirName) throws Exception {
		File dir = new File(dirName);
		String[] files = dir.list();
		
		for(int i = 0; i < files.length; i++) {
			System.out.print((i+1) + ") " + files[i] + " ");

			try {
				int count = doSatTest(dirName + files[i]);
				System.out.println(count);
			} catch(TimeoutException e) {
				System.out.println(" ** Timeout ** ");
				System.out.println();
			} catch(Exception e) {
				e.printStackTrace(System.err);				
				System.out.println();
			} catch(OutOfMemoryError e) {
				System.out.println(" ** Out of Memory ** ");
				System.out.println();
			} catch(Error e) {
				e.printStackTrace(System.err);				
			}
		}		
	}

	public int doSatTest(String file) throws Exception {
	    int count = 0;
	    
	    System.err.println( "Sat test currently disabled!" );
	    
//		final StreamTokenizer in = initTokenizer(file);
//		
//		final boolean result = file.endsWith("_n.alc");
//
//		for(; count < 21; count ++) {			
//			kb = initKB(SAT_LIMIT);
//			
//
//			ATermAppl c = parseExpr(in);
//
//			long time = System.currentTimeMillis();
//			boolean sat = kb.isSatisfiable(c);	
//			time = System.currentTimeMillis() - time;
//			
//			if(sat != result)
//			    throw new RuntimeException("Consistency error");
//			else
//			    System.out.print( "(" + (count+1) + ":" + time + ")" );
//		}	
		
		return count;
	}
	
	
	public boolean doABoxTest(String file) throws Exception {
	    String ext = ".tkb";
		int index = file.lastIndexOf('.');
		if(index != -1) {
		    ext = file.substring( index );
		    file = file.substring( 0, index );
		}
		index = file.lastIndexOf(File.separator);
		String displayName = (index == -1) ? file : file.substring(index + 1);
		System.out.print(displayName + " ");
		
		kb = initKB(TBOX_LIMIT);		
		loader.load( file + ext, kb  );

		Timer t = kb.timers.startTimer( "test" );
		
		System.out.print("preparing...");
		
		kb.prepare();
		
        if( !FAST ) {
    		System.out.print("classifying...");    		
        	kb.realize();
        	
        	if(DEBUG) kb.printClassTree();
        }
				
		t.stop();
            
		
		System.out.print("verifying...");
		loader.verifyABox( file + ".query", kb );
		
		System.out.print("done");
		
		System.out.print( " Prepare " + kb.timers.getTimer("preprocessing").getTotal() );
		System.out.print( " Classify " + kb.timers.getTimer("classify").getTotal() );
		
		System.out.println( " " + t.getTotal() );
		
		if(PRINT_TIME) kb.timers.print();
		
		return true;
	}
	
	public void doAllABoxTests(String dirName) throws Exception {
		File dir = new File(dirName);
		File[] files = dir.listFiles(new ExtensionFilter( ".akb"));
		Arrays.sort(files, AlphaNumericComparator.CASE_INSENSITIVE);
		
		for(int i = 0; i < files.length; i++) {
			System.out.print((i+1) + ") ");
			try {
				doABoxTest(files[i].getAbsolutePath());
			} catch(TimeoutException e) {
				System.out.println(" ***** Timeout ***** ");
				System.out.println();
			} catch(Exception e) {
				e.printStackTrace(System.err);				
				System.out.println();
			} catch(OutOfMemoryError e) {
				System.out.println(" ***** Out of Memory ***** ");
				System.out.println();
			} catch(Error e) {
				e.printStackTrace(System.err);				
			}
		}		
	}
	
	public static void usage() {
		System.out.println("DLTest - Run the tests in DL-benchmark suite");
		System.out.println("");
		System.out.println("Usage: java DLTest [-timing] <input> <type>");
		System.out.println("   input    A single file or a directory that contains");
		System.out.println("            a set of test files");
		System.out.println("   type     Type of the test, one of [sat, tbox, abox]");
	}
	
	public final static void main(String[] args)throws Exception  {
	    if(args.length == 0 ) {
	        usage();
	        return;
	    }
	    
	    int base = 0;
	    if( args[0].equals( "-timing" ) ) {
	        DLBenchmarkTest.PRINT_TIME = true;
	        base = 1;
	    }
	    else if( args.length != 2 ) {
	        System.out.println( "Invalid arguments" );
	        usage();
	        return;
	    }
	    	    
		String in   = args[base + 0];
		String type = args[base + 1];
		
	    File file = new File(in);
	    
	    if( !file.exists() )
	        throw new FileNotFoundException( file + " does not exist!" );
	    
		boolean singleTest = file.isFile();

		DLBenchmarkTest test = new DLBenchmarkTest();
		if(type.equals("sat")) {
		    if(singleTest)
				test.doSatTest(in);
		    else
				test.doAllSatTests(in);
		}
		else if(type.equals("tbox")) {
		    if(singleTest)
				test.doTBoxTest(in);
		    else
				test.doAllTBoxTests(in);		    
		}
		else if(type.equals("abox")) {
		    if(singleTest)
				test.doABoxTest(in);
		    else
				test.doAllABoxTests(in);		    		    
		}
		else
		    usage();
	}	
}
