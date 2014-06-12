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

package org.mindswap.pellet;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.mindswap.pellet.exceptions.TimeoutException;
import org.mindswap.pellet.exceptions.UnsupportedFeatureException;
import org.mindswap.pellet.jena.ModelReader;
import org.mindswap.pellet.jena.NodeFormatter;
import org.mindswap.pellet.jena.OWLReasoner;
import org.mindswap.pellet.jena.OWLSpecies;
import org.mindswap.pellet.jena.PelletInfGraph;
import org.mindswap.pellet.jena.PelletQueryExecution;
import org.mindswap.pellet.jena.PelletReasonerFactory;
import org.mindswap.pellet.output.OutputFormatter;
import org.mindswap.pellet.output.TableData;
import org.mindswap.pellet.utils.FileUtils;
import org.mindswap.pellet.utils.Timer;
import org.mindswap.pellet.utils.Timers;
import org.mindswap.pellet.utils.VersionInfo;
import org.semanticweb.owl.io.owl_rdf.SWRLParser;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.helper.OntologyHelper;

import aterm.ATermAppl;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.arp.ParseException;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFErrorHandler;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.shared.JenaException;

/**
 * This is the command-line version of Pellet. It is provided as a stand-alone program
 * and should not be directly used in applications. 
 * 
 * @author Evren Sirin
 *
 */
public class Pellet {
    final static public int SPECIES_ON    = 0;
    final static public int SPECIES_LEVEL = 1;
    final static public int SPECIES_OFF   = 2;
    
    final static public int CLASS_NONE    = 0;
    final static public int CLASS_TREE    = 1;
    final static public int CLASS_RDF     = 2;
    final static public int CLASS_RDF_ALL = 3;
    
    private static VersionInfo vinfo = null;

	String inFile   = null;
	String rulesFile   = null;
	String inString = null;
	String inFormat = null;
	String conclusionsFile   = null;
	String conclusionsString = null;
	String conclusionsFormat = null;
	String outFile   = null;
	String queryFile = null;
	String queryString = null;
    Syntax queryFormat = Syntax.syntaxSPARQL;
	int     classifyFormat = CLASS_NONE;
	boolean checkConsistency = true;
    int     species = SPECIES_ON;
	boolean realize    = false;
	boolean findUnsat  = false;
	boolean quiet      = false;
	boolean econnEnabled = false;
	boolean printTiming = false;
    
    
	Timers timers;

	ModelReader modelReader;	
	List parseErrors;

    OntModel model;
	OWLReasoner reasoner;
    KnowledgeBase kb;
    
	Set unsatClasses;
	Query query;
	ResultSet queryResults;
	
	OutputFormatter out;

	int timeout = -1;

	public Pellet() {		
		out = new OutputFormatter();
		
		unsatClasses = new HashSet();
		modelReader = new ModelReader();		
		parseErrors = new ArrayList();
		
		timers = new Timers();
		
		modelReader.setErrorHandler(new RDFErrorHandler() {
			public void warning(Exception e) {
			    parseErrors.add(ParseException.formatMessage(e));
			}

			public void error(Exception e) {
			    parseErrors.add(ParseException.formatMessage(e));			
			}

			public void fatalError(Exception e) {		
//			    out.printBold("Fatal error encountered parsing RDF:").println();
//			    out.println(ParseException.formatMessage(e));
			    throw new JenaException(e);
			}				
		});	    
	}

	private void loadInput() {
		Timer t = timers.startTimer("Loading");

		if (inFile != null) {
			if (inString != null)
				throw new RuntimeException("Both an input url and text is given.");
		} else if (inString == null) {
            if( query!= null && !query.getGraphURIs().isEmpty() )
                inFile = query.getGraphURIs().get( 0 ).toString();
		    if(inFile == null) {
		        out.println("No input file is given!");
		        if(!out.isFormatHTML())
		            usage();
		        out.flush();
				System.exit(1);
		    }
		} else if(econnEnabled) {
			System.err.println("The URI of the input should be given when E-connections are enabled!");
			System.exit(0);
		}
		


		if(econnEnabled) {
			out.printBold("Input file: ").printLink(inFile).println();
            reasoner = new OWLReasoner();
			reasoner.setEconnEnabled(true);
			reasoner.load(inFile);
		}
		else {
			Model plainModel = null;
			if (inFile != null) {
				out.printBold("Input file: ").printLink(inFile).println();
				plainModel = modelReader.read(inFile, inFormat);
			}
			else {
				out.printBold("Input file: ").println("Text area");
				plainModel = modelReader.read(new ByteArrayInputStream(inString.getBytes()), inFormat);				
			}
		    
			if (plainModel != null) {
                model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC, plainModel );
				reasoner = ((PelletInfGraph) model.getGraph()).getOWLReasoner();
			}
			
			printParseErrors();
		}
        
        ((PelletInfGraph) model.getGraph()).setLazyConsistency( true );
        
        model.prepare();
        
        kb = reasoner.getKB();

		if(timeout > 0)
		    kb.setTimeout(timeout * 1000);

		t.stop();		
	}
	
	private void loadRules() {
		if (rulesFile != null) {			
			Set rules = parseRules();			
			reasoner.getKB().setRules(rules);
		}		
	}

	public Set parseRules() {	

		HashSet importedOnts = new HashSet();
		Set rules = new HashSet();

		try {
			OWLOntology ontology = OntologyHelper.getOntology(URI.create(rulesFile));
			URI uri = ontology.getPhysicalURI();

			if (!(ontology.equals(null))) {
				SWRLParser parser = new SWRLParser();
				// For imported ontologies
				importedOnts = (HashSet) ontology.getIncludedOntologies();
				// If there is no imported ontology in the rules file it assumes
				// that rules and ontology are in the same file
				if (importedOnts.isEmpty()) {
					parser.setOntology(ontology);
					rules = parser.parseRules(uri);
					
				} else {
					Iterator it = importedOnts.iterator();
					ontology = (OWLOntology) it.next();
					parser.setOntology(ontology);
					rules = parser.parseRules(uri);
					
				}
			}
		} catch (Exception e) {
			out.println( "Error loading rules file: " + e );
			return null;
		}
		return rules;

	}
	
	private void printParseErrors() {
		if(!parseErrors.isEmpty()) {
		    out.printBold("Problems encountered parsing RDF:").println();
			for (Iterator iter = parseErrors.iterator(); iter.hasNext();)
				out.println(iter.next());
		}
		out.flush();	    
	}

	private void checkSpecies() {
        if( species == SPECIES_OFF ) return;
        
	    Timer t = timers.startTimer("Species Validation");
	    
	    OWLSpecies species = reasoner.getSpecies();
	    
		out.printBold("OWL Species: ").println(species);
		
		if(econnEnabled)
		    out.printBold("DL Expressivity: ").println(((EconnectedKB)kb).getEconnExpressivity());
		else
		    out.printBold("DL Expressivity: ").println(kb.getExpressivity());

	    t.stop();	    		
	}
	
	private void checkConsistency() {
	    if( !checkConsistency )
            return;
		
		Timer t = timers.startTimer("Consistency");
		boolean isConsistent = reasoner.isConsistent();
        t.stop();
		
		out.printBold("Consistent: ");
		if(isConsistent) 
		    out.println("Yes");
		else {
		    out.printHTML("<font color=Red><b>").print("No").printHTML("</b></font>").println();
			out.printBold("Reason: ").println(kb.getExplanation());
		}
	}

	private void checkConclusions() {
		if (conclusionsFile != null && conclusionsString != null)
			throw new RuntimeException("Both a conclusions url and text is given.");		

		if(conclusionsFile == null && conclusionsString == null)
		    return;
		
		if(!reasoner.isConsistent())
		    return;
		
		Model coModel = null;
		if(conclusionsFile != null)
			coModel = modelReader.read(conclusionsFile, conclusionsFormat);
		else
			coModel = modelReader.read(new ByteArrayInputStream(conclusionsString.getBytes()), conclusionsFormat);

		boolean isEntailed = reasoner.isEntailed(coModel);
		
		out.printBold("Entailed: ").print(isEntailed ? "Yes" : "No").println();
	}
	
	private void checkUnsat() {
	    if(!findUnsat && !kb.isClassified())
	        return;
	        		
		if(!reasoner.isConsistent())
		    return;

		Iterator i = kb.getClasses().iterator();
		while (i.hasNext()) {
			ATermAppl c = (ATermAppl) i.next();
			if(!kb.isSatisfiable(c))
			    unsatClasses.add(c);
		}
	}
	
	private boolean doClassify() {
		if(classifyFormat == CLASS_NONE || !reasoner.isConsistent()) 
		    return false;

		Timer t = timers.startTimer("Classification");
		reasoner.classify();
		t.stop();

		if (realize) {
			t = timers.startTimer("Realization");
			reasoner.realize();
			t.stop();
		}		
		
		return true;
	}

	private void printClassTree() {
	    if(!kb.isClassified())
	        return;
	    
	    out.println();
		out.printBold("Classification:").println();
		if(classifyFormat == CLASS_TREE) {					
			kb.printClassTree(out);
		}
		else {
			boolean verbose;
			if(classifyFormat == CLASS_RDF)	
				verbose = false;
			else if(classifyFormat == CLASS_RDF_ALL) 
				verbose = true;
			else
				throw new RuntimeException("Invalid format to display classification");
			
			out.printHTML("<xmp>");
			reasoner.extractModel(verbose).write(out.getWriter(), "RDF/XML-ABBREV");
			out.printHTML("</xmp>");
			out.flush();
		}		 
	}
	
	private void printUnsat() {
	    if(unsatClasses.isEmpty())
	        return;
	    
		out.printParagraph().printBold("Unsatisfiable Concepts").println();
		Iterator i = unsatClasses.iterator();
		while (i.hasNext()) {
		    String name = i.next().toString(); 
			out.printURI(name);
			if(i.hasNext())
			    out.print(", ");
		}
		out.println();
	}
	
	private void printTimeInfo() {
	    out.printBold("Time: ").println(getTimeInfo());	    
	}
	
	private void loadQuery() throws Exception {
		if( queryFile != null && queryString != null )
            throw new RuntimeException( "Both a query url and text is given." );

        if( queryFile == null && queryString == null )
            return;

        Timer t = timers.startTimer( "Loading" );

        if( queryString == null ) {
            queryString = FileUtils.readURL( new URL( queryFile ) );
        }

        query = QueryFactory.create( queryString, queryFormat );

        t.stop();
	}
	
	private void execQuery() {		
		if(query == null)
		    return;

		if(!reasoner.isConsistent())
		    return;
		
        PelletQueryExecution queryEngine = new PelletQueryExecution( query, model );        
        queryResults = queryEngine.execSelect();
	}
	
	private void printQueryResults() {
	    if(query == null)
	        return;
	    
	    out.println();
	    out.printBold("Query: ").println();
	    out.printHTML("<pre>");
	    if(out.isFormatHTML())
	        out.print(queryString.trim().replaceAll("<", "&lt;").replaceAll(">", "&gt;"));
	    else
	        out.println(queryString.trim());
	    out.printHTML("</pre>");
	    out.println("-----------------------------------------------------");	    
	    if( queryResults.hasNext() ) {            
            // number of distinct bindings
            int count = 0;
            
            NodeFormatter formatter = new NodeFormatter( model, out.isFormatHTML() ); 
                    
            // variables used in select
            List resultVars = query.getResultVars();
            
            // store the formatted results in a set to avoid duplicates but keep 
            // the same order by using LinkedHashSet
            Set data = new LinkedHashSet();
            while( queryResults.hasNext() ) {
                QuerySolution binding = queryResults.nextSolution();
                List formattedBinding = new ArrayList();
                for( int i = 0; i < resultVars.size(); i++ ) {
                    String var = (String) resultVars.get( i );
                    RDFNode result = binding.get( var );

                    // format the result
                    formattedBinding.add( formatter.format( result ) );
                }
                
                if( data.add( formattedBinding ) ) 
                    count++;                
            }                
            
            out.printBold("Query Results (" + count + " answers): ").println();
            
            TableData table = new TableData(data, resultVars);
            table.print( out );
	    }
	    else {
            out.printBold("Query Results (0 answers): ").println();
	        out.printBold("NO RESULTS").println();
        }
	}	

    public void run() {
		try {
		    Timer t = timers.startTimer("totalTime");

		    loadQuery();
			loadInput();
			loadRules();
            checkSpecies();
            checkConsistency();
			checkConclusions();		
			doClassify();
			checkUnsat();
			execQuery();
						
			t.stop();
			
			printTimeInfo();
	
		    printUnsat();
			printClassTree();		
		    printQueryResults();
			printSpeciesReport();
			if (printTiming)
				kb.timers.print();
        } catch (QueryParseException e) {
            out.printBold("Cannot parse query: " + e.getMessage()).println();
            out.println();
            throw(e);
		} catch (JenaException e) {
		    out.printBold("Unrecoverable error while parsing RDF:").println();
		    out.println(ParseException.formatMessage(e));	
		    out.println();
		    out.println("Cannot process file due to parse error!");
		    throw(e);
		} catch(TimeoutException e) {
			out.printParagraph().printBold("TIMEOUT:").println("Timeout after " + timeout + " seconds");
		} catch(UnsupportedFeatureException e) {
			out.printParagraph().printBold("Unsupported features in input:");
			out.println( e.getMessage() );
			throw(e);
        } catch(Exception e) {
            out.println( "Exception: " + e );	
            e.printStackTrace();
        } catch(Error e) {
            out.println( "Error: " + e );    
            e.printStackTrace();        
        } finally {			
			out.flush();
		}
	}

	private void printSpeciesReport() {
        if( species != SPECIES_ON ) return;
		
		out.printParagraph();
		
		OWLSpecies species = reasoner.getSpecies();
		species.getReport().print(out);

		if (species.getLevel() == OWLSpecies.FULL) {
			out
				.printParagraph()
				.printBold("Add the following statements to make this document OWL DL")
				.println();
			if (species.missingTriples != null) {
				out.printHTML("<xmp>");

				try {
					RDFWriter rdfWriter = species.missingTriples.getWriter();
					rdfWriter.setProperty("allowBadURIs", "true");
					rdfWriter.write(species.missingTriples, out.getWriter(), null);
				} catch (Exception e) {
					out.print("Following error occurred when trying to print the missing triples " + e);
					e.printStackTrace();

					Iterator si = species.missingTriples.listStatements();
					while (si.hasNext())
						System.err.println(si.next());
				}
				out.printHTML("</xmp>");
			} else {
				out.println("This ontology cannot be converted to OWL DL.");
			}
		}		
	}
	
	public String getTimeInfo() {
		String timeInfo = timers.getTimer("totalTime").getTotal() + " ms (";
		timeInfo += getTimerString("Loading");		
		timeInfo += getTimerString("Preprocessing");
		timeInfo += getTimerString("Species Validation");
		timeInfo += getTimerString("Consistency");
		timeInfo += getTimerString("Entailment");
		timeInfo += getTimerString("Unsatisfiability");
		timeInfo += getTimerString("Classification");
		timeInfo += getTimerString("Realization");
		timeInfo += ")";

		return timeInfo;
	}

	private String getTimerString(String name) {
		String info = "";
		Timer t = timers.getTimer(name);
		if (t != null)
			info = name + ": " + t.getTotal() + " ";
		return info;
	}
    
    public void setQueryFormat( String format ) {        
        queryFormat = Syntax.lookup( format.toUpperCase() );
        if( queryFormat == null )
            throw new RuntimeException("Unknown query format " + queryFormat);    
    }
    
	/**
	 * @param string
	 */
	public void setClassifyFormat(int format) {
		classifyFormat = format;
	}
    
    public void setClassifyFormat(String s) {
        if( s.equalsIgnoreCase( "none" ) )
            setClassifyFormat( CLASS_NONE );
        else if( s.equalsIgnoreCase( "tree" ) )
            setClassifyFormat( CLASS_TREE );
        else if( s.equalsIgnoreCase( "rdf" ) )
            setClassifyFormat( CLASS_RDF );
        else if( s.equalsIgnoreCase( "rdf-all" ) )
            setClassifyFormat( CLASS_RDF_ALL );
        else
            throw new RuntimeException("Unknown classification format " + s);
    }
    
	/**
	 * @param string
	 */
	public void setConclusionsFile(String string) {
		conclusionsFile = string;
	}

	/**
	 * @param string
	 */
	public void setConclusionsFormat(String string) {
		conclusionsFormat = string;
	}

	/**
	 * @param string
	 */
	public void setConclusionsString(String string) {
		conclusionsString = string;
	}

	/**
	 * @param b
	 */
	public void setFormatHTML(boolean b) {
		out.setFormatHTML(b);
	}

	/**
	 * @param string
	 */
	public void setInFile(String string) {
		inFile = string;
	}

	/**
	 * @param string
	 */
	public void setInFormat(String string) {
		inFormat = string;
	}

	/**
	 * @param string
	 */
	public void setInString(String string) {
		inString = string;
	}

//	public void setOutFile(String string) {
//		outFile = string;
//	}

	public void setPrintTiming(boolean value) {
		printTiming = value;
	}
	
	/**
	 * @param string
	 */
	public void setQueryFile(String string) {
		queryFile = string;
	}

	public void setQueryString(String string) {
		queryString = string;
	}
	
	/**
	 * @param i
	 */
	public void setTimeout(int i) {
		timeout = i;
	}

	/**
	 * @param b
	 */
	public void setSpecies(int i) {
        species = i;
	}
    
    public void setQuiet(boolean b) {
        quiet = b;
    }

	/**
	 * @param b
	 */
	public void setConsistency(boolean b) {
		checkConsistency = b;
	}

	/**
	 * @param b
	 */
	public void setRealize(boolean b) {
        if( b && classifyFormat == CLASS_NONE )
            classifyFormat = CLASS_TREE;
        
		realize = b;
	}
	/**
	 * @param b
	 */
	public void setUnsat(boolean b) {
		findUnsat = b;
	}
    
    /**
     * @param econnEnabled 
     */
    public void setEconnEnabled(boolean econnEnabled) {
        this.econnEnabled = econnEnabled;
    }
    
	private static void usage() {
        VersionInfo vinfo = getVersionInfo();
        
		System.out.println("Pellet - OWL DL Reasoner (Version:" + vinfo.getVersionString() + ")");
		System.out.println(
			"For the OWL ontologies Pellet provides options find the ontology level");
		System.out.println("(Lite, DL, FULL), check consistency, find unsatisfiable concepts, ");
		System.out.println("display class hierarchy, save OWL Full ontologies as OWL DL, ");
		System.out.println("check if triples in another ontology is entailed by the input");
		System.out.println("ontology");
		System.out.println("");
		System.out.println("Usage: java Pellet OPTIONS");
		System.out.println(" {-if,-inputFile} <file URI>  Input ontology URI");
		System.out.println(" {-is,-inputString} string    A string representation of the input file");
		System.out.println(" {-ifmt,-inputFormat} format  Format of the input file, ");
		System.out.println("                              one of [RDF/XML, N3, NTRIPLES]");
		System.out.println(" {-s,-species} {on,level,off} Turn on/off species validation (Default: on). The");
        System.out.println("                              option \"level\" means species validation will be");
        System.out.println("                              done but no detailed report is printed");        
        System.out.println(" -consistency {on,off}        Turn on/off consistency checking (Default: on)");
        System.out.println("                              Note that any reasoning service (classification");
        System.out.println("                              realization,query) will turn this option on");                
		System.out.println(" -unsat              	      Find the unsatisfiable classes in the ontology.");
        System.out.println("                              This option is unnecessary if classification is");
        System.out.println("                              selected");
		System.out.println(" {-c,-classify} [format]      Classify the ontology and display the hierarchy");
		System.out.println("                              in one of the formats [TREE, RDF, RDF-ALL]. If");
		System.out.println("                              realize option is selected, types for individuals");
		System.out.println("                              are also printed (Default format: TREE)");
		System.out.println("                              TREE: Display the hierarchy as an indented tree");
		System.out.println("                              RDF: Display only direct subclasses in RDF/XML");
		System.out.println("                              RDF-ALL: Display all subclasses in RDF/XML");
		System.out.println(" {-r,-realize}                Compute and display the most specific instances");
		System.out.println("                              for each class. When this option is enabled, ");
		System.out.println("                              classification will be automatically done");
		System.out.println(" {-cf,-conclusionsFile} <URI> Check if all the triples in this ontology is");
		System.out.println("                              entailed by the input ontology");
		System.out.println(" {-cs,-conclusionsString} str A string representation of the conclusions file");
		System.out.println(" -conclusionsFormat format    Format of the conclusions file,");
		System.out.println("                              one of [RDF/XML, N3, NTRIPLES]");
		System.out.println(" {-qf,-queryFile} file        Read the SPARQL (or RDQL) query from the given");
		System.out.println("                              file and run it on the input ontology");
		System.out.println(" {-qs,-queryString} string    Run the given query on the input ontology");
        System.out.println(" {-qfmt,-queryFormat} format  Specify the format of the query (Default: SPARQL)");
//		System.out.println(" -outputDL file               If the input file is OWL Full then save it in the");
//		System.out.println("                              file as OWL DL file by adding missing type triples");
        System.out.println(" -econn                       Enable processing DL-safe rules found in the input");
        System.out.println("                              file");
		System.out.println(" -econn                       Enable processing for E-Connected ontologies.");
        System.out.println("                              Should not be used if the input ontology is not");
        System.out.println("                              E-connected ");
		System.out.println(" -timeout time                Timeout after <time> seconds");
		System.out.println(" -timing                      Print detailed timing information");
//		System.out.println(" -quiet                       Don't print species report");
        System.out.println(" -version                     Print the version information and exit");
		System.out.println(" -help                        Print this message");
	}
    
    public static VersionInfo getVersionInfo() {
        if (vinfo == null) 
            vinfo = new VersionInfo();
        return vinfo;
    }

    private static void invalidOption( String option ) {
        System.err.println("Invalid " + option + " option");
        usage();
        System.exit(1);
    }
    
	public final static void main(String[] args) throws Exception {
		Pellet pellet = new Pellet();

        for( int i = 0; i < args.length; i++ ) {
            String arg = args[i];

            if( arg.equalsIgnoreCase( "-help" ) ) {
                usage();
                System.exit( 0 );
            }
            else if( arg.equalsIgnoreCase( "-version" ) ) {
                VersionInfo vinfo = getVersionInfo();
                System.out.println("Version : " + vinfo.getVersionString() );
                System.out.println("Released: " + vinfo.getReleaseDate() );
                System.exit( 0 );
            }
            else if( arg.equalsIgnoreCase( "-species" ) || arg.equalsIgnoreCase( "-s" ) ) {
                String s = args[++i];
                if( s.equalsIgnoreCase( "on" ) )
                    pellet.setSpecies( SPECIES_ON );
                else if( s.equalsIgnoreCase( "level" ) )
                    pellet.setSpecies( SPECIES_LEVEL );
                else if( s.equalsIgnoreCase( "off" ) )
                    pellet.setSpecies( SPECIES_OFF );
                else
                    invalidOption( "species" );
            }
            else if( arg.equalsIgnoreCase( "-consistency" ) ) {
                String s = args[++i];
                if( s.equalsIgnoreCase( "on" ) )
                    pellet.setConsistency( true );
                else if( s.equalsIgnoreCase( "off" ) )
                    pellet.setConsistency( false );
                else
                    invalidOption( "consistency" );
            }
            else if( arg.equalsIgnoreCase( "-classify" ) || arg.equalsIgnoreCase( "-c" ) ) {
                String s = (i == args.length -1) ? "tree" : args[++i];                
                if( s.equalsIgnoreCase( "tree" ) )
                    pellet.setClassifyFormat( CLASS_TREE );
                else if( s.equalsIgnoreCase( "rdf" ) )
                    pellet.setClassifyFormat( CLASS_RDF );
                else if( s.equalsIgnoreCase( "rdf-all" ) )
                    pellet.setClassifyFormat( CLASS_RDF_ALL );
                else
                    i--;
            }
            else if( arg.equalsIgnoreCase( "-inputFile" ) || arg.equalsIgnoreCase( "-if" ) )
                pellet.setInFile( args[++i] );
            else if( arg.equalsIgnoreCase( "-inputFormat" ) || arg.equalsIgnoreCase( "-ifmt" ) )
                pellet.setInFormat( args[++i] );
            else if( arg.equalsIgnoreCase( "-inputString" ) || arg.equalsIgnoreCase( "-is" ) )
                pellet.setInString( args[++i] );
            else if( arg.equalsIgnoreCase( "-conclusionsFile" ) || arg.equalsIgnoreCase( "-cf" ) )
                pellet.setConclusionsFile( FileUtils.toURI( args[++i] ) );
            else if( arg.equalsIgnoreCase( "-conclusionsFormat" ) || arg.equalsIgnoreCase( "-cfmt" ) )
                pellet.setConclusionsFormat( args[++i] );
            else if( arg.equalsIgnoreCase( "-conclusionsString" ) || arg.equalsIgnoreCase( "-cs" ) )
                pellet.setConclusionsString( args[++i] );
            else if( arg.equalsIgnoreCase( "-queryFile" ) || arg.equalsIgnoreCase( "-qf" ) )
                pellet.setQueryFile( FileUtils.toURI( args[++i] ) );
            else if( arg.equalsIgnoreCase( "-queryString" ) || arg.equalsIgnoreCase( "-qs" ) )
                pellet.setQueryString( args[++i] );
            else if( arg.equalsIgnoreCase( "-queryFormat" ) || arg.equalsIgnoreCase( "-qfmt" ) )
                pellet.setQueryFormat( args[++i] );
            // else if (arg.equalsIgnoreCase("-outputDL"))
            // pellet.setOutFile(args[++i]);
            else if( arg.equalsIgnoreCase( "-timeout" ) )
                pellet.setTimeout( Integer.parseInt( args[++i] ) );
            else if( arg.equalsIgnoreCase( "-econn" ) )
                pellet.setEconnEnabled( true );
            else if( arg.equalsIgnoreCase( "-html" ) )
                pellet.setFormatHTML( true );
            else if( arg.equalsIgnoreCase( "-text" ) )
                pellet.setFormatHTML( false );
            else if( arg.equalsIgnoreCase( "-unsat" ) )
                pellet.setUnsat( true );
            else if( arg.equalsIgnoreCase( "-realize" ) || arg.equalsIgnoreCase( "-r" ) )
                pellet.setRealize( true );
            else if( arg.equalsIgnoreCase( "-quick" ) )
                pellet.setConsistency( false );
            else if( arg.equalsIgnoreCase( "-quiet" ) )
                pellet.setSpecies( SPECIES_LEVEL );
            else if( arg.equalsIgnoreCase( "-timing" ) )
                pellet.setPrintTiming( true );            
            else if (arg.equalsIgnoreCase("-dlsafe")) {
				pellet.rulesFile = pellet.inFile;
				pellet.setInFile(pellet.rulesFile);
			}
            else {
                System.err.println( "Unrecognized option: " + arg );
                usage();
                System.exit( 1 );
            }
        }

        pellet.run();
    }
}
