//The MIT License
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

package org.mindswap.pellet;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.mindswap.pellet.exceptions.UnsupportedFeatureException;
import org.mindswap.pellet.jena.ModelReader;
import org.mindswap.pellet.jena.OWLReasoner;
import org.mindswap.pellet.test.OWLTestVocabulary;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * A simple program to test the species validator on OWL test cases. 
 * 
 * @author Evren Sirin
 */
public class OWLSpeciesTest {
	public static boolean DEBUG = false;

	// if there is no (or very slow) connection to download
	// test files and the test cases are present in the local
	// disk and by setting the useLocal variable to true these
	// local copies can be used. base variable should also be
	// set accordingly
	static boolean useLocal = false;
	static String base =
		useLocal
			? "file://C/Mindswap/owl-test/"
			: "http://www.w3.org/2002/03owlt/editors-draft/draft/";

	final static List TYPES =
		Arrays.asList(
			new Resource[] {
				OWLTestVocabulary.NotOwlFeatureTest,
				OWLTestVocabulary.PositiveEntailmentTest,
				OWLTestVocabulary.NegativeEntailmentTest,
				OWLTestVocabulary.TrueTest,
				OWLTestVocabulary.OWLforOWLTest,
				OWLTestVocabulary.ConsistencyTest,
				OWLTestVocabulary.InconsistencyTest,
				OWLTestVocabulary.ImportEntailmentTest,
				OWLTestVocabulary.ImportLevelTest });

	final static List LEVELS =
		Arrays.asList(new Resource[] { OWLTestVocabulary.Lite, OWLTestVocabulary.DL, OWLTestVocabulary.Full });

	static String allTests = base + "Manifest.rdf";

	public final static void main(String[] args) {
		String manifestFile = allTests;
		
		OWLSpeciesTest owlTest = new OWLSpeciesTest();
		
		if(args.length == 0 || args[0].equals("-all"))
		    owlTest.testAll(manifestFile);
		else if(args[0].equals("-list"))
			owlTest.testList(AT_RISK);
	} // main

	private static final String[] AT_RISK = new String[] {
//		"http://www.w3.org/2002/03owlt/I5.26/Manifest006",
	    "http://www.w3.org/2002/03owlt/disjointWith/Manifest006",
        "http://www.w3.org/2002/03owlt/disjointWith/Manifest009"	    
	};

	private void testList(String[] uris) {
		DEBUG = true;

		Vector incorrectCases = new Vector();
		int numIncorrect = 0;
		int numTests = 0;
		int testCount = 1;
		long start = System.currentTimeMillis();
		for (int i = 0; i < uris.length; i++) {
			Model m = ModelFactory.createDefaultModel();
			m.read(uris[i], "");

			StmtIterator sIter = m.listStatements(null, RDF.type, (Resource) null);
			Resource testCase = null;
			while (sIter.hasNext()) {
				Statement stmt = sIter.nextStatement();
				if (!TYPES.contains(stmt.getObject()))
					continue;

				testCase = stmt.getSubject();
				break;
			} // while

			final Statement statusStmt = testCase.getProperty(OWLTestVocabulary.status);
			final RDFNode testStatus = statusStmt.getObject();
			//testCase.getProperty(OWLTestVocabulary.status).getObject();

			// don't do or report anything about obsolete test cases
			if (testStatus.equals(OWLTestVocabulary.Obsoleted))
				continue;

			System.out.println("Test Case: " + testCount++);
			System.out.println("Name: " + testCase);
			System.out.println();

			Property[] docList =
				new Property[] {
					OWLTestVocabulary.premiseDocument,
					OWLTestVocabulary.inputDocument,
					OWLTestVocabulary.conclusionDocument };
			for (int j = 0; j < docList.length; j++)
				if (testCase.hasProperty(docList[j])) {
					String inputFile = testCase.getProperty(docList[j]).getObject().toString();
					String inputLevel =
						testCase
							.getProperty(docList[j])
							.getProperty(OWLTestVocabulary.level)
							.getObject()
							.toString();

					System.out.println("Document: " + inputFile);
					System.out.println(
						"Document Level: " + inputLevel.substring(inputLevel.lastIndexOf("#") + 1));

					numTests++;
					try {
						String aLevel = getFileLevel(inputFile);

						System.out.print("Level : " + aLevel);

						if (aLevel == null || !inputLevel.endsWith(aLevel)) {
							System.out.print(" (WRONG)");
							numIncorrect++;
							incorrectCases.addElement(testCount - 1 + ": " + inputFile);
						} // if
						System.out.println("\n");
					} // try
					catch (Exception ex) {
						numIncorrect++;
						incorrectCases.addElement(testCount - 1 + ": " + inputFile);
						System.out.println(" (WRONG)\n");
						ex.printStackTrace(System.out);
					} // catch
				} // if
		} // for
		long total = System.currentTimeMillis() - start;

		System.out.println("Final Statistics");
		System.out.println("Total Test Cases: " + (testCount - 1));
		System.out.println("Total Tests Conducted: " + numTests);
		System.out.println("Total Passed: " + (numTests - numIncorrect));
		System.out.println("Total Failed: " + numIncorrect);
		System.out.println(
			"Percent: " + (((float) (numTests - numIncorrect) / (float) numTests) * 100));
		System.out.println("Total Time (in seconds): " + (total / 1000));
		System.out.println();

		System.out.println("List of failed cases: ");
		for (int i = 0; i < incorrectCases.size(); i++)
			System.out.println(incorrectCases.elementAt(i).toString());
	} // testList

	private String getFileLevel(String inputFile) {
		inputFile = getFileName(inputFile);

		try {
		    ModelReader reader = new ModelReader();		    
		    OWLReasoner reasoner = new OWLReasoner();
		    try {
                reasoner.load(reader.read(inputFile));
            } catch(Throwable e1) {
            }
			String aLevel = reasoner.getLevel();
			if (DEBUG)
				reasoner.getSpecies().getReport().print();
			return aLevel;
		} // try
		catch (UnsupportedFeatureException ufe) {
			// should never happen, the parser should be set to not throw
			// unsupported exceptions
			System.err.println("File has unsupported features");
		} // catch
		catch (StackOverflowError ufe) {
			System.err.println(ufe);
		} // catch
		catch (Exception e) {
			System.err.println("Exception " + e);
			e.printStackTrace();
		} // catch
		return "Unknown";
	} // getFileLevel

	private String getRDFHead() {
		StringBuffer sb = new StringBuffer();
		sb.append("<rdf:RDF \n");
		sb.append("  xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" \n");
		sb.append("  xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" \n");
		sb.append("  xmlns:results=\"http://www.w3.org/2002/03owlt/resultsOntology#\" \n");
		sb.append("  xmlns=\"http://www.w3.org/1999/xhtml\" \n");
		sb.append("  xml:base=\"http://www.mindswap.org/2003/pellet\">\n\n");
		sb.append("<rdf:Description rdf:about=\"#pellet\">\n");
		sb.append("\t<rdfs:label>Pellet</rdfs:label>\n");
		sb.append("</rdf:Description>\n");
		return sb.toString();
	} //

	public void testAll(String manifestFile) {
		Vector incorrectCases = new Vector();
		int numIncorrect = 0;
		int numTests = 0;
		System.out.println("Reading manifest file " + manifestFile);
		System.out.println();
		Model manifestModel = ModelFactory.createDefaultModel();
		manifestModel.read(manifestFile, "");
		int testCount = 1;
		StmtIterator sIter = manifestModel.listStatements(null, RDF.type, (Resource) null);
		long start = System.currentTimeMillis();
		StringBuffer rdf = new StringBuffer(getRDFHead());
		while (sIter.hasNext()) {
			Statement stmt = sIter.nextStatement();
			if (!TYPES.contains(stmt.getObject()))
				continue;

			final Resource testCase = stmt.getSubject();
			final Statement statusStmt = testCase.getProperty(OWLTestVocabulary.status);
			final RDFNode testStatus = statusStmt.getObject();
			//testCase.getProperty(OWLTestVocabulary.status).getObject();

			// don't do or report anything about obsolete test cases
			if (testStatus.equals(OWLTestVocabulary.Obsoleted))
				continue;

			// these test cases are obsolete, but the model doesn't think they are, must skip over
			// them
			if (useLocal == true && (testCount == 81 || testCount == 140)) {
				testCount++;
				continue;
			} // if

			//            if (testCount != 21)
			//            {
			//              testCount++;
			//              continue;
			//            } // if
			//              if (testCount > 100)
			//                break;

			System.out.println("Test Case: " + testCount++);
			System.out.println("Name: " + testCase);
			System.out.println();

			boolean ranATest = false;
			boolean passed = true;
			if (testCase.hasProperty(OWLTestVocabulary.premiseDocument)) {
				String inputFile =
					testCase.getProperty(OWLTestVocabulary.premiseDocument).getObject().toString();
				try {
					ranATest = true;
					String premiseLevel =
						testCase
							.getProperty(OWLTestVocabulary.premiseDocument)
							.getProperty(OWLTestVocabulary.level)
							.getObject()
							.toString();

					System.out.println("Premise Document: " + inputFile);
					System.out.println(
						"Premise Level: "
							+ premiseLevel.substring(premiseLevel.lastIndexOf("#") + 1));

					numTests++;

					String aLevel = getFileLevel(inputFile);

					System.out.print("Level : " + aLevel);

					if (aLevel == null || !premiseLevel.endsWith(aLevel)) {
						System.out.print(" (WRONG)");
						numIncorrect++;
						incorrectCases.addElement(testCount - 1 + ": " + inputFile);
						passed = false;
					} // if
					System.out.println("\n");
				} // try
				catch (Exception ex) {
					numIncorrect++;
					incorrectCases.addElement(testCount - 1 + ": " + inputFile);
					System.out.println(" (WRONG)\n");
					ex.printStackTrace(System.out);
					passed = false;
				} // catch
			} // if

			if (testCase.hasProperty(OWLTestVocabulary.inputDocument)) {
				ranATest = true;
				String inputFile =
					testCase.getProperty(OWLTestVocabulary.inputDocument).getObject().toString();
				String inputLevel =
					testCase
						.getProperty(OWLTestVocabulary.inputDocument)
						.getProperty(OWLTestVocabulary.level)
						.getObject()
						.toString();

				System.out.println("Input Document: " + inputFile);
				System.out.println(
					"Input Level: " + inputLevel.substring(inputLevel.lastIndexOf("#") + 1));

				numTests++;
				try {
					String aLevel = getFileLevel(inputFile);

					System.out.print("Level : " + aLevel);

					if (aLevel == null || !inputLevel.endsWith(aLevel)) {
						System.out.print(" (WRONG)");
						//OWLParser.printInfo();
						numIncorrect++;
						passed = false;
						incorrectCases.addElement(testCount - 1 + ": " + inputFile);
					} // if
					System.out.println("\n");
				} // try
				catch (Exception ex) {
					numIncorrect++;
					incorrectCases.addElement(testCount - 1 + ": " + inputFile);
					System.out.println(" (WRONG)\n");
					ex.printStackTrace(System.out);
					passed = false;
				} // catch
			} // if

			if (testCase.hasProperty(OWLTestVocabulary.conclusionDocument)) {
				ranATest = true;
				String inputFile =
					testCase.getProperty(OWLTestVocabulary.conclusionDocument).getObject().toString();
				String conclusionLevel =
					testCase
						.getProperty(OWLTestVocabulary.conclusionDocument)
						.getProperty(OWLTestVocabulary.level)
						.getObject()
						.toString();

				System.out.println("Conclusion Document: " + inputFile);
				System.out.println(
					"Conclusion Level: "
						+ conclusionLevel.substring(conclusionLevel.lastIndexOf("#") + 1));

				numTests++;
				try {
					String aLevel = getFileLevel(inputFile);

					System.out.print("Level : " + aLevel);

					if (aLevel == null || !conclusionLevel.endsWith(aLevel)) {
						System.out.print(" (WRONG)");
						//OWLParser.printInfo();
						numIncorrect++;
						incorrectCases.addElement(testCount - 1 + ": " + inputFile);
						passed = false;
					} // if
					System.out.println("\n");
				} // try
				catch (Exception ex) {
					numIncorrect++;
					passed = false;
					incorrectCases.addElement(testCount - 1 + ": " + inputFile);
					System.out.println(" (WRONG)\n");
					ex.printStackTrace(System.out);
				} // catch
			} // else if

			if (!ranATest)
				System.out.println("No tests run for this case!");

			System.out.println("-----------------------------");

			/*
			 * <results:TestRun><results:system rdf:resource="#wonderweb"/>
			 * <results:output rdf:resource="http://wonderweb.man.ac.uk/owl/ww-validation-results.html#http%3A%2F%2Fwww.w3.org%2F2002%2F03owlt%2FAllDifferent%2FManifest001%23test"/>
			 * <rdf:type rdf:resource="http://www.w3.org/2002/03owlt/resultsOntology#PassingRun"/>
			 * <results:test rdf:parseType="Resource">
			 * <results:syntacticLevelTestFrom rdf:resource="http://www.w3.org/2002/03owlt/AllDifferent/Manifest001#test"/>
			 * </results:test></results:TestRun>
			 *  
			 */
			if (!passed) {
				rdf.append("<results:FailingRun>\n");
				rdf.append("\t<results:system rdf:resource=\"#pellet\"/>\n");
				rdf.append(
					"\t<results:output rdf:resource=\"http://www.mindswap.org/2003/pellet/test.shtml\"/>\n");
				rdf.append("\t<results:test rdf:parseType=\"Resource\">\n");
				rdf.append(
					"\t\t<results:syntacticLevelTestFrom rdf:resource=\""
						+ testCase.toString()
						+ "\"/>\n");
				rdf.append("\t</results:test>\n");
				rdf.append("</results:FailingRun>\n\n");
			} // if
			else {
				rdf.append("<results:PassingRun>\n");
				rdf.append("\t<results:system rdf:resource=\"#pellet\"/>\n");
				rdf.append(
					"\t<results:output rdf:resource=\"http://www.mindswap.org/2003/pellet/test.shtml\"/>\n");
				rdf.append("\t<results:test rdf:parseType=\"Resource\">\n");
				rdf.append(
					"\t\t<results:syntacticLevelTestFrom rdf:resource=\""
						+ testCase.toString()
						+ "\"/>\n");
				rdf.append("\t</results:test>\n");
				rdf.append("</results:PassingRun>\n\n");
			} // else
		} // while

		long total = System.currentTimeMillis() - start;

		rdf.append("</rdf:RDF>");
//		try {
//			com.lre.utils.Util.saveStringToFile(rdf.toString(), "syntax_results.rdf");
//		} // try
//		catch (Exception ex) {
//		}
		System.out.println("Final Statistics");
		System.out.println("Total Test Cases: " + (testCount - 1));
		System.out.println("Total Tests Conducted: " + numTests);
		System.out.println("Total Passed: " + (numTests - numIncorrect));
		System.out.println("Total Failed: " + numIncorrect);
		System.out.println(
			"Percent: " + (((float) (numTests - numIncorrect) / (float) numTests) * 100));
		System.out.println("Total Time (in seconds): " + (total / 1000));
		System.out.println();

		System.out.println("List of failed cases: ");
		for (int i = 0; i < incorrectCases.size(); i++)
			System.out.println(incorrectCases.elementAt(i).toString());
	} // doTest2

	/**
	 * Given a filename converts it to file path on local machine if useLocal option is set. This
	 * is used when testing is done without network connection
	 */
	String getFileName(String fileName) {
		if (useLocal)
			fileName =
				base
					+ fileName.substring(
						1 + fileName.substring(1, fileName.lastIndexOf("/") - 1).lastIndexOf("/"))
					+ ".rdf";

		return fileName;
	}

}
