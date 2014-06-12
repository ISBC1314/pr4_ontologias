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


import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.WindowConstants;

import org.mindswap.pellet.ABox;
import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.exceptions.TimeoutException;
import org.mindswap.pellet.exceptions.UnsupportedFeatureException;
import org.mindswap.pellet.jena.ModelReader;
import org.mindswap.pellet.jena.OWLReasoner;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

public class WebOntTest {     
	public static boolean DEBUG = false;

	// each test case should be handled in timeout seconds
	// or otherwise is assumed to fail that test case
	// if timeout is set to 0 then test case will not be stopped
	// default value is 10 seconds
	public static int timeout = 10; 

	// At the end of tests stats about the results are displayed
	// There are couple of options here; short means only total
	// number of passes are shown, long means individual results
	// for each test is also displayed, all means a GUI window is
	// popped up to show these stats 
	public final static int NO_STATS    = 0;
	public final static int SHORT_STATS = 1;
	public final static int LONG_STATS  = 2;
	public final static int ALL_STATS   = 3;
	public int showStats = ALL_STATS;

	// if there is no (or very slow) connection to download 
	// test files and the test cases are present in the local
	// disk and by setting the useLocal variable to true these
	// local copies can be used. base variable should also be
	// set accordingly
	public boolean useCache;
	public String base = "http://www.w3.org/2002/03owlt/editors-draft/draft/";

	// if this flag is set for every succesful test case (consistency, entailment)
	// a new RDF file is saved to the directory DLized with the necessary triples
	// added to the original file
	public static boolean OUTPUT_DL = false;
			          
	final static List TYPES = Arrays.asList(new Resource[] {
		OWLTestVocabulary.NotOwlFeatureTest, 
		OWLTestVocabulary.PositiveEntailmentTest, 
		OWLTestVocabulary.NegativeEntailmentTest, 
		OWLTestVocabulary.TrueTest, 
		OWLTestVocabulary.OWLforOWLTest, 
		OWLTestVocabulary.ConsistencyTest, 
		OWLTestVocabulary.InconsistencyTest,
		OWLTestVocabulary.ImportEntailmentTest, 
		OWLTestVocabulary.ImportLevelTest
	});	

	final static List LEVELS = Arrays.asList(new Resource[] {
		OWLTestVocabulary.Lite, OWLTestVocabulary.DL,	OWLTestVocabulary.Full
	});	
	final static List STATUS = Arrays.asList(new RDFNode[] {
		OWLTestVocabulary.Approved, OWLTestVocabulary.Proposed, 
		OWLTestVocabulary.ExtraCredit, OWLTestVocabulary.Obsoleted
	});	
	
	public boolean avoidFailTests = false;
	/**
	 * Pellet is known to fail the following test cases either because they are not in OWL DL or
	 * they are exteremely hard, e.g. more than 100 GCI's, or a very large number restriction. 
	 * Such cases are not very realistic and it is not considered to be a problem to fail those 
	 * test cases. But if Pellet fails on one of the other test cases then it indicates a problem.  
	 */
	final static List AVOID = Arrays.asList(new Resource[] {
	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/AnnotationProperty/Manifest002#test"),
	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/DatatypeProperty/Manifest001#test"),
	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/description-logic/Manifest661#test"),	    
	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/description-logic/Manifest662#test"),	    
	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/description-logic/Manifest663#test"),	    
	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/description-logic/Manifest664#test"),	    	    
	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/description-logic/Manifest903#test"),
	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/description-logic/Manifest905#test"),	    
	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/description-logic/Manifest906#test"),	    
	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/description-logic/Manifest907#test"),
	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/description-logic/Manifest909#test"),
	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/description-logic/Manifest910#test"),
	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/disjointWith/Manifest010#test"),
	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/equivalentProperty/Manifest005#test"),
	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/extra-credit/Manifest002#test"),
	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/extra-credit/Manifest003#test"),
	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/extra-credit/Manifest004#test"),
	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/I4.6/Manifest003#test"),
	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/I5.1/Manifest001#test"),
	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/I5.26/Manifest006#test"),
	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/I5.26/Manifest007#test"),
	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/I5.3/Manifest014#test"),
	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/I5.5/Manifest003#test"),
	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/I5.5/Manifest004#test"),
	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/I5.5/Manifest007#test"),
//	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/I5.8/Manifest001#test"),
//	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/I5.8/Manifest003#test"),
//	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/I5.8/Manifest004#test"),
//	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/I5.8/Manifest013#test"),
//	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/I5.8/Manifest014#test"),
//	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/I5.8/Manifest015#test"),
//	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/I5.8/Manifest016#test"),
        ResourceFactory.createResource("http://www.w3.org/2002/03owlt/I5.8/Manifest012#test"),
	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/I5.8/Manifest017#test"),
	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/miscellaneous/Manifest205#test"),
	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/Ontology/Manifest003#test"),
	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/sameAs/Manifest001#test"),
	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/someValuesFrom/Manifest001#test"),
	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/someValuesFrom/Manifest003#test"),
	    ResourceFactory.createResource("http://www.w3.org/2002/03owlt/Thing/Manifest005#test"),
	});

	int syntacticTestCount = 0;
	int syntacticTestPass  = 0;
	
	final public static int TEST_PASS = 0;
	final public static int TEST_FAIL = 1;
	final public static int TEST_SKIP = 2;
	final public static List RESULTS = Arrays.asList(new String[] {
		"PASS", "FAIL", "SKIP" 
	});

	// There are three test levels Lite, DL, Full. There are three test 
	// status: Approved, Proposed, Obsolete. There are three different 
	// test result that can happen: Fail, Pass, Skip. We'll keep the 
	// statistics for each possibility. The following two dimensional
	// array is used for this purpose. Each line stores information 
	// about one level. First three columns are for Approved tests and
	// first column of these is count of passed tests, second column is
	// count of failed tests, third column is for skipped tests
			
	int[][][][] stats = new int[LEVELS.size()][STATUS.size()][TYPES.size()][RESULTS.size()];

	// a table showing the test results for each individual test case 
	Vector results = new Vector();
	
	// maximum number of test cases to process
	static int MAX_TEST_COUNT = Integer.MAX_VALUE;

	public final static void main(String[] args) {
	    WebOntTest owlTest = new WebOntTest();
	    owlTest.run(args);
	}
	
	public WebOntTest() {   
	}
	
    public String getBase() {
        return base;
    }
    
    public void setBase( String base ) {
		if( !base.startsWith("file:") )
			base = new File(base).toURI().toString();

		this.base = base;
        
        useCache = true;
    }	
	 
    public boolean isAvoidFailTests() {
        return avoidFailTests;
    }
    
    public void setAvoidFailTests( boolean avoidFailTests ) {
        this.avoidFailTests = avoidFailTests;
    }
    
    public int getShowStats() {
        return showStats;
    }
    
    public void setShowStats( int showStats ) {
        this.showStats = showStats;
    }
    
    public void run(String[] args) {
		String manifestFile = base + "Manifest.rdf";
		
		for (int i = 0 ; i < args.length ; i++ ) {
			String arg = args[i];
			
			if(arg.equals("-timeout")) 
				timeout = Integer.parseInt(args[++i]);
			else if(arg.equals("-avoidFail")) {
			    avoidFailTests = true;
			}
			else if(arg.equals("-manifest")) {
				manifestFile = args[++i];
				if(!manifestFile.startsWith("http://"))
					manifestFile = "http://www.w3.org/2002/03owlt/" + manifestFile;
			}
			else if(arg.equals("-base")) {
				setBase( args[++i] );
			}	
			else if(arg.equals("-validate"))
				PelletOptions.VALIDATE_ABOX = true;
			else if(arg.equals("-n")) {
				try {
					MAX_TEST_COUNT = Integer.parseInt(args[++i]);
				} catch(Exception e) {
					System.err.println(e);
				}
			}
			else if(arg.startsWith("-stats")) {
				String stats = args[++i].toLowerCase();
				if(stats.equals("no"))
					showStats = NO_STATS;
				else if(stats.equals("short"))
					showStats = SHORT_STATS;
				else if(stats.equals("long"))
					showStats = LONG_STATS;
				else if(stats.equals("all"))
					showStats = ALL_STATS;
			}
		}
	
	    System.out.println("Reading manifest file " + manifestFile);
		System.out.println();
	    
		long time = System.currentTimeMillis();						    
	    doTest(manifestFile);
		time = System.currentTimeMillis() - time;
		System.out.println("Total time: " + time);		    
	}
	
	public boolean doTest(String manifestFile) {
	    boolean allPassed = true;
	    int testCount = 0;
	    
		try {					
			Model model = ModelFactory.createDefaultModel();
			Model outputModel = ModelFactory.createDefaultModel();
			
			// Create system description
			Resource system = ResourceFactory.createResource();
			Property label = ResourceFactory.createProperty("http://www.w3.org/2000/01/rdf-schema#label");
			Property comment = ResourceFactory.createProperty("http://www.w3.org/2000/01/rdf-schema#comment");
			Literal name = outputModel.createLiteral("Pellet");
			Literal description = outputModel.createLiteral("This was generated by the Pellet reasoner which can be found at http://www.mindswap.org/2003/pellet/");
			outputModel.add(ResourceFactory.createStatement(system, label, name));
			outputModel.add(ResourceFactory.createStatement(system, comment, description));
						
			model.read(manifestFile, "");
			
			StmtIterator i = model.listStatements(null, RDF.type, (Resource)null); 
			while(i.hasNext() && testCount <= MAX_TEST_COUNT) {
				Statement stmt = i.nextStatement();
				if(!TYPES.contains(stmt.getObject()))
					continue;				
				
				Resource testCase = stmt.getSubject();

				if( avoidFailTests && AVOID.contains( testCase ) )
				    continue;				

				// Output statements
				Statement levelStmt = testCase.getProperty(OWLTestVocabulary.level);
				Statement statusStmt = testCase.getProperty(OWLTestVocabulary.status);

				Vector levels = new Vector();
				// Changes according to the editors-draft. For each test there can
				// be more than one level. Basically each Lite test is also a Full
				// test and now this information is given explicitly in Manifest
				// So loop through all the levels and find the tight bound
				int level = 3; // 0 - Lite, 1 - DL, 2 - Full
				StmtIterator si = testCase.listProperties(OWLTestVocabulary.level);
				while(si.hasNext()) {
					Statement s = si.nextStatement();
					String levelName = s.getResource().getLocalName();
					if(level > LEVELS.indexOf(levelName)) {
						levelStmt = s;
						levels.add(levelName);						
					}
				}
			
				final Resource testType = stmt.getResource();
				final Resource testLevel = levelStmt.getResource();
				final RDFNode  testStatus = statusStmt.getObject();

				// don't do or report anything about obsolete test cases
				if(testStatus.equals(OWLTestVocabulary.Obsoleted)) 		   
					continue;					
				else if(testStatus.toString().startsWith("OBSOLETE"))
					continue;
				else if(testType.equals(OWLTestVocabulary.OWLforOWLTest))
					continue;				
														
				testCount++;
				
				String fileName = testCase.toString();
				System.out.println("Test  : " + testCount);
				System.out.println("Name  : " + testCase);
				System.out.print("Info  : ");
				System.out.print(testType.getLocalName() + " ");				
				System.out.print(levels + " ");
				System.out.print(testStatus);					
				System.out.println();						
				
				Vector result = new Vector();			
				result.add(new Integer(testCount-1));
				result.add(fileName.substring(1+fileName.substring(1,fileName.lastIndexOf("/")-1).lastIndexOf("/")));
				result.add(testType.getLocalName());
				result.add(testLevel.getLocalName());
				result.add(testStatus);
				
				long time = System.currentTimeMillis();
				
//				int testResult = 
//					doTestCase(testCase, testType, testLevel, testStatus);

				int testResult = TEST_FAIL;
				
				try {
					testResult = doTestCase(testCase, testType);
				}
				catch (StackOverflowError e) {
					// reporting failure for the bugs in our code causes
					// problem for the approval of tests in webont wg
					testResult = TEST_FAIL;
					System.err.println("Fail  : Stack overflow");
					printStackTrace(e);								
				}
				catch (OutOfMemoryError e) {
					testResult = TEST_FAIL;
					System.err.println("Fail  : Out of Memory");		
				}
				
				time = System.currentTimeMillis() - time;					

				// Insert test results;
				Resource testRun = ResourceFactory.createResource();
				Property type = ResourceFactory.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
				Property tcProp = ResourceFactory.createProperty("http://www.w3.org/2002/03owlt/resultsOntology#test");
				Resource runtype = ResourceFactory.createResource("http://www.w3.org/2002/03owlt/resultsOntology#TestRun");
				Property systemProp = ResourceFactory.createProperty("http://www.w3.org/2002/03owlt/resultsOntology#system");
				Resource resultType = null;
				if (testResult == TEST_FAIL) {
				    if (time >= timeout * 1000) {
						resultType = ResourceFactory.createResource("http://www.w3.org/2002/03owlt/resultsOntology#IncompleteRun");
				    } else {
						resultType = ResourceFactory.createResource("http://www.w3.org/2002/03owlt/resultsOntology#FailingRun");
				    }
				} else if (testResult == TEST_PASS) {
				    resultType = ResourceFactory.createResource("http://www.w3.org/2002/03owlt/resultsOntology#PassingRun");
				} else if (testResult == TEST_SKIP) {
				    //resultType = ResourceFactory.createResource("http://www.w3.org/2002/03owlt/resultsOntology#FailingRun");
				} else {
				    //resultType = ResourceFactory.createResource("http://www.w3.org/2002/03owlt/resultsOntology#FailingRun");
				}
				
				outputModel.add(ResourceFactory.createStatement(testRun, type, runtype));
				outputModel.add(ResourceFactory.createStatement(testRun, tcProp, testCase));
				outputModel.add(ResourceFactory.createStatement(testRun, systemProp, system));
				if (resultType != null) 
				    outputModel.add(ResourceFactory.createStatement(testRun, type, resultType));
				// Insert test time;
				Property timeProp = ResourceFactory.createProperty("http://owl.mindswap.org/2003/ont/owlweb.rdf#testTime");
				Literal timeLiteral = model.createLiteral(""+time);
				outputModel.add(ResourceFactory.createStatement(testRun,timeProp,timeLiteral));

				result.add(new Long(time));
				result.add(RESULTS.get(testResult));
				
				results.add(result);

				int i1 = LEVELS.indexOf(testLevel);
				int i2 = STATUS.indexOf(testStatus);
				int i3 = TYPES.indexOf(testType);
				int i4 = testResult;
							
				stats[i1][i2][i3][i4]++;	

//				System.out.println("Time  : " + Pellet.getTimeInfo(true));//time + " ms (" + Pellet.getTimeInfo(true) + " ms)");
				System.out.println("Result: " + RESULTS.get(testResult)); 
				System.out.println("--------------------------------------------------------------------");
				
				allPassed &= (testResult == TEST_PASS);
			}

			
//			FileOutputStream output = new FileOutputStream("results.rdf");
//			outputModel.write(output);
//			output.close();

			printStatistics();
		} catch(Exception e) {
			System.out.println(e);
			e.printStackTrace();
			allPassed = false;
			
			if( testCount == 2 )
			    throw new RuntimeException( e );
		}		
		
		return allPassed;
	}	
	
	public int doSingleTest( String manifestFile ) {			
		Model model = ModelFactory.createDefaultModel();
					
		model.read( manifestFile, "" );
		
		StmtIterator i = model.listStatements(null, RDF.type, (Resource)null);
		Statement stmt;
		do {
		    stmt = i.nextStatement();
		}
		while( !TYPES.contains(stmt.getObject()) );
		
		Resource testCase = stmt.getSubject();
		Statement statusStmt = testCase.getProperty(OWLTestVocabulary.status);
	
		final Resource testType = stmt.getResource();
		final RDFNode  testStatus = statusStmt.getObject();
		

		if( avoidFailTests && AVOID.contains( testCase ) )
		    return TEST_SKIP;				
		else if(testStatus.equals(OWLTestVocabulary.Obsoleted)) 		   
			return TEST_SKIP;					
		else if(testStatus.toString().startsWith("OBSOLETE"))
		    return TEST_SKIP;
		else if(testType.equals(OWLTestVocabulary.OWLforOWLTest))
		    return TEST_SKIP;								
		
//		System.out.print("Name  : " + testCase);
		
		long time = System.currentTimeMillis();

		int testResult = TEST_FAIL;
		
//		try {
			testResult = doTestCase(testCase, testType);
//		}
//		catch (StackOverflowError e) {
//			// reporting failure for the bugs in our code causes
//			// problem for the approval of tests in webont wg
//			testResult = TEST_FAIL;
//			System.err.println("Fail  : Stack overflow");
//			printStackTrace(e);								
//		}
//		catch (OutOfMemoryError e) {
//			testResult = TEST_FAIL;
//			System.err.println("Fail  : Out of Memory");		
//		}
		
		time = System.currentTimeMillis() - time;					

//		System.out.print( RESULTS.get(testResult) ); 
//		System.out.print(ln " ("  + time + "ms)"); 
		
		return testResult;
	}	
	
	public void printStatistics() {		
		ArrayList dataArray = new ArrayList();
		for(int s = 0; s < STATUS.size() - 1; s++) { 
			Object[][] data = new Object[LEVELS.size()*(TYPES.size()+1)+1][RESULTS.size()];
			
			//level,status,type,result
			for(int r = 0; r < 3; r++) {
				int columnTotal = 0;	
				for(int l = 0; l < LEVELS.size(); l++) {
					int total = 0;	
					for(int t = 0; t < TYPES.size(); t++) {
						int count = stats[l][s][t][r];
						data[l*(TYPES.size()+1)+t+1][r] = new Integer(count);
						
						total += count;
					}
					data[l*(TYPES.size()+1)][r] = new Integer(total);
					columnTotal += total;					
				}
				data[LEVELS.size()*(TYPES.size()+1)][r] = new Integer(columnTotal);				
			}
			
			dataArray.add(data);
		}			
		
		if(showStats >= LONG_STATS) {
		    System.out.println();
			for(int i = 0; i < results.size(); i++) {
				Vector result = (Vector) results.get(i);  
				boolean fail = result.get(result.size()-1).equals("FAIL");
				if(fail) {
					for(int j = 0; j < result.size(); j++)  
						System.out.print(result.get(j) + " ");
					System.out.println();
				}
			}
 		}

		if(showStats >= SHORT_STATS) {
		    System.out.println();
			for(int s = 0; s < STATUS.size() - 1; s++) { 
				Object[][] data = (Object[][]) dataArray.get(s);
	
				System.out.println(STATUS.get(s).toString());
				for(int c = 0; c < 3; c++) 
					System.out.print(data[data.length-1][c] + " ");			
				System.out.println();
			}
		}

		if(showStats >= ALL_STATS)
			showStatistics(dataArray);
	}	
	
	private void showStatistics(ArrayList dataArray) { 	
		Object[][] types = new String[LEVELS.size()*(TYPES.size()+1)+1][1];
		for(int l = 0; l < LEVELS.size(); l++) {
			types[l*(TYPES.size()+1)][0] = ((Resource)LEVELS.get(l)).getLocalName();
			for(int t = 0; t < TYPES.size(); t++)
				types[l*(TYPES.size()+1)+t+1][0] = "     " + 
					((Resource)TYPES.get(t)).getLocalName();
			types[LEVELS.size()*(TYPES.size()+1)][0] = "TOTAL";		
		} 

		JFrame info = new JFrame("Result");
		info.getContentPane().setLayout(new BoxLayout(info.getContentPane(), BoxLayout.Y_AXIS));

		Box mainPanel = Box.createHorizontalBox();
		final JTable table = new JTable(types, new String[] {"TYPE"});		

		Box p1 = Box.createVerticalBox();
		p1.add(new JLabel(" "));
		p1.add(new JScrollPane(table));
		mainPanel.add(p1);				
		
		final JTable[] tables = new JTable[STATUS.size()];
		for(int s = 0; s < STATUS.size() - 1; s++) { 
			Box p = Box.createVerticalBox();

			Object[][] data = (Object[][]) dataArray.get(s);
			tables[s] = new JTable(data, RESULTS.toArray());
			
			JLabel label = new JLabel(STATUS.get(s).toString()); 
			label.setAlignmentX(Component.CENTER_ALIGNMENT);
			p.add(label);
			p.add(new JScrollPane(tables[s]));
			mainPanel.add(p);
		}
		
		Box optionsPanel = Box.createVerticalBox();
		final JCheckBox details = new JCheckBox("Show Details", true);
		details.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    if(DEBUG) System.out.println(e);
			    
				int rowHeight = details.isSelected() ?
					table.getRowHeight(0) : 1;
					
				for(int l = 0; l < LEVELS.size(); l++) {
					for(int t = 0; t < TYPES.size(); t++) {					
						table.setRowHeight(l*(TYPES.size()+1)+t+1, rowHeight);
						tables[0].setRowHeight(l*(TYPES.size()+1)+t+1, rowHeight);
						tables[1].setRowHeight(l*(TYPES.size()+1)+t+1, rowHeight);
						tables[2].setRowHeight(l*(TYPES.size()+1)+t+1, rowHeight);
					}
				} 
			}
		});
		details.doClick();
		optionsPanel.add(details);

		Vector columnNames = new Vector();
		columnNames.add("No");
		columnNames.add("Name");
		columnNames.add("Type");
		columnNames.add("Level");
		columnNames.add("Status");
		columnNames.add("Time (ms)");
		columnNames.add("Result");

//		String[] columnNames = new String[] {
//			"No", "Name", "Type", "Level", "Status", "Time (ms)", "Result"};

		JTable resultsTable = new JTable(results, columnNames);

		resultsTable.getColumnModel().getColumn(0).setPreferredWidth(15);
		resultsTable.getColumnModel().getColumn(1).setPreferredWidth(250);
		resultsTable.getColumnModel().getColumn(2).setPreferredWidth(150);
		resultsTable.getColumnModel().getColumn(3).setPreferredWidth(30);
		resultsTable.getColumnModel().getColumn(5).setPreferredWidth(30);
		resultsTable.getColumnModel().getColumn(6).setPreferredWidth(30);

		info.getContentPane().add(mainPanel);				
		info.getContentPane().add(optionsPanel);				
		info.getContentPane().add(new JScrollPane(resultsTable));				
		info.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		info.setSize(800, 600);
		info.setVisible( true );	
	}

	/**
	 * Given a filename converts it to file path on local machine if use_cache
	 * option is set. This is used when testing is done without network connection
	 */
	String getFileName(String fileName) {
		if( useCache )
			fileName = base + 
				fileName.substring(1+
				fileName.substring(1,fileName.lastIndexOf("/")-1).lastIndexOf("/")) +
				".rdf";
		
		return fileName;
	} 
	
	int doConsistencyTest(Resource testCase, boolean isConsistent) {
		String inputFile = null;
		try {
			inputFile = 
				testCase.hasProperty(OWLTestVocabulary.inputDocument) ?
				testCase.getProperty(OWLTestVocabulary.inputDocument).getObject().toString() :
				null;
					
			if(inputFile != null) {		
				inputFile = getFileName(inputFile);

//				System.out.println("Input : " + inputFile);
			}
			
			ModelReader reader = new ModelReader();
			Model model = reader.read(inputFile);
			
			OWLReasoner reasoner = new OWLReasoner();
			reasoner.getKB().setTimeout(timeout * 1000);
			reasoner.load(model);
						
			if(reasoner.isConsistent() != isConsistent) {
				System.err.println("Fail  : Consistency error");
				return TEST_FAIL;
			}
			
		} catch(UnsupportedFeatureException e) {
			System.err.println("Skip  : " + e.getMessage());
			return TEST_FAIL;
		} catch (TimeoutException e1) {
			System.err.println("Fail  : Timeout - Couldn't find answer after " + timeout + " seconds");
			return TEST_FAIL;
		} catch(Exception e) {			
			System.err.println("Fail  : " + inputFile);
			printStackTrace(e);
			return TEST_FAIL;
		}	

		return TEST_PASS;
	}

	int doEntailmentTest(Resource testCase, boolean isEntailed) {
		String inputFile = null;
		try {
			inputFile = 
				testCase.hasProperty(OWLTestVocabulary.premiseDocument) ?
				testCase.getProperty(OWLTestVocabulary.premiseDocument).getObject().toString() :
				null;
			String conclusionsFile =	
				testCase.getProperty(OWLTestVocabulary.conclusionDocument).
				getObject().toString();
			
					
			if(inputFile != null) {		
				inputFile = getFileName(inputFile);

//				System.out.println("Input : " + inputFile);	
			}
			conclusionsFile = getFileName(conclusionsFile);
//			System.out.println("Conc. : " + conclusionsFile);
				
			ModelReader reader = new ModelReader();
			Model model = reader.read(inputFile);
			
			OWLReasoner reasoner = new OWLReasoner();
			reasoner.getKB().setTimeout(timeout * 1000);
			reasoner.load(model);

			if(!reasoner.isConsistent()) {
				System.err.println("Fail  : Premises file is not consistent!");
				return TEST_FAIL;				
			}
				
			Model conclusions = reader.read(conclusionsFile);
			if(reasoner.isEntailed(conclusions) != isEntailed) {
				System.err.println("Fail  : Entailment error");
				return TEST_FAIL;
			}
			
		} catch(UnsupportedFeatureException e) {
			System.err.println("Fail  : " + e.getMessage());
			return TEST_FAIL;
		} catch (TimeoutException e1) {
			System.err.println("Fail  : Timeout - Couldn't find answer after " + timeout + " seconds");
			return TEST_FAIL;
		} catch(Exception e) {			
			System.err.println("Fail  : " + inputFile);
			printStackTrace(e);
			return TEST_FAIL;
		}	
		
		return WebOntTest.TEST_PASS;
	}

	int doNotOWLFeatureTest(Resource testCase) {
		String inputFile = null;
		try {
			inputFile = testCase.getProperty(OWLTestVocabulary.inputDocument).getObject().toString();					
			inputFile = getFileName(inputFile);

			
			ModelReader reader = new ModelReader();
			Model model = reader.read(inputFile);

			OWLReasoner reasoner = new OWLReasoner();
			reasoner.load(model);			
						
			// TODO fix this test
			String speciesInfo = reasoner.getSpecies().getReport().toString();
			if(speciesInfo.indexOf("Invalid OWL Term") != -1)
				return TEST_PASS;						
		} catch(UnsupportedFeatureException e) {
			return TEST_PASS;
		} catch(Exception e) {			
			System.err.println("Fail  : " + inputFile);
			printStackTrace(e);
			return TEST_FAIL;
		}	

		return TEST_FAIL;
	}

	int doImportLevelTest(Resource testCase) {
		String inputFile = null;
		
		try {
			inputFile = 
				testCase.getProperty(OWLTestVocabulary.inputDocument).getObject().toString();
					
			inputFile = getFileName(inputFile);

//			System.out.println("Input : " + inputFile);	
			
			String testLevel = 
				testCase.getProperty(OWLTestVocabulary.level).getResource().getLocalName();

			ModelReader reader = new ModelReader();
			Model model = reader.read(inputFile);
			
			OWLReasoner reasoner = new OWLReasoner();
			reasoner.load(model);			
			
			if(reasoner.getLevel().equals(testLevel))
				return TEST_PASS;
		} catch(UnsupportedFeatureException e) {
			return TEST_PASS;
		} catch(Exception e) {			
			System.err.println("Fail  : " + inputFile);
			printStackTrace(e);
			return TEST_FAIL;
		}	

		return TEST_FAIL;
	}
	
	int doTestCase(Resource testCase, Resource testType) {
		if(testType.equals(OWLTestVocabulary.PositiveEntailmentTest))
			return doEntailmentTest(testCase, true);
		else if(testType.equals(OWLTestVocabulary.NegativeEntailmentTest))
			return doEntailmentTest(testCase, false);
		else if(testType.equals(OWLTestVocabulary.ConsistencyTest ))
			return doConsistencyTest(testCase, true);
		else if(testType.equals(OWLTestVocabulary.InconsistencyTest ))
			return doConsistencyTest(testCase, false);
		else if(testType.equals(OWLTestVocabulary.ImportEntailmentTest))
			return doEntailmentTest(testCase, true);
		else if(testType.equals(OWLTestVocabulary.OWLforOWLTest))
//			return doEntailmentTest(testCase, true);
		    return TEST_SKIP;
		else if(testType.equals(OWLTestVocabulary.NotOwlFeatureTest))
			return doNotOWLFeatureTest(testCase);
		else if(testType.equals(OWLTestVocabulary.ImportLevelTest))
			return doImportLevelTest(testCase);
		
		throw new RuntimeException( 
		    "Unknown test type " + testType.getLocalName() + " for " + testCase );
	}

	public static void printStackTrace(Throwable e) {
		StackTraceElement[] ste = e.getStackTrace();
		
		System.err.println(e);
		if(ste.length > 25) {
			for(int i = 0; i < 15 && i < ste.length; i++)
				System.err.println("   " + ste[i]);
			System.err.println("   ...");
			for(int i = ste.length - 10; i < ste.length; i++)
				System.err.println("   " + ste[i]);
		}
		else {
			for(int i = 0; i < ste.length; i++)
				System.err.println("   " + ste[i]);			
		}
	}
}
