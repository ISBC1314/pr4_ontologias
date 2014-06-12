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

/*
 * Created on Oct 12, 2004
 */
package org.mindswap.pellet;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.mindswap.pellet.exceptions.UnsupportedFeatureException;
import org.mindswap.pellet.jena.NodeFormatter;
import org.mindswap.pellet.jena.PelletInfGraph;
import org.mindswap.pellet.jena.PelletQueryEngine;
import org.mindswap.pellet.jena.PelletReasonerFactory;
import org.mindswap.pellet.output.TableData;
import org.mindswap.pellet.utils.FileUtils;
import org.mindswap.pellet.utils.QNameProvider;
import org.mindswap.pellet.utils.Timers;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdql.Query;
import com.hp.hpl.jena.rdql.QueryResults;
import com.hp.hpl.jena.rdql.ResultBinding;


/**
 * @author Evren Sirin
 */
public class PelletQuery {
    private boolean formatHTML;
    private boolean printTime;
    private boolean detailedTime;
    private boolean printOnlyNumbers;
    private boolean classify;
    private String data;

    public PelletQuery() {
        printTime = false;
        printOnlyNumbers = false;
        formatHTML = false;
    }

    public void setData(String data) {
        this.data = data;
    }    

    public boolean isPrintTime() {
        return printTime;
    }
    
    public void setPrintTime(boolean printTime) {
        this.printTime = printTime;
    }
    
    public boolean isClassify() {
        return classify;
    }

    public void setClassify(boolean classify) {
        this.classify = classify;
    }

    public boolean isQuiet() {
        return printOnlyNumbers;
    }
    
    public void setQuiet(boolean quiet) {
        this.printOnlyNumbers = quiet;
    }

    public boolean isFormatHTML() {
        return formatHTML;
    }
    
    public void setFormatHTML(boolean formatHTML) {
        this.formatHTML = formatHTML;
    }
    
    public boolean isDetailedTime() {
        return detailedTime;
    }
    
    public void setDetailedTime(boolean detailedTime) {
        this.detailedTime = detailedTime;
    }

    /**
     * @deprecated Use setFormatHTML(boolean) and run(String) instead
     */
    public void run(String queryStr, boolean formatHTML) throws Exception {
        setFormatHTML( formatHTML );
        run( queryStr );        
    }

    public void run( String input ) throws Exception {
        String loadedOnt = "";
        OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );        
        
        String[] queries = Pattern.compile("SELECT", Pattern.CASE_INSENSITIVE).split( input );
        if( queries.length <= 1 ) {
            System.err.println( "Invalid RDQL query: " + input );
            usage();
            System.exit( 1 );
        }
        for(int j = 1; j < queries.length; j++) {
            String queryStr = "SELECT " + queries[j];
            
            if( queries.length > 1)
                System.out.println( "Query (" + j + ")" );
//            System.out.println( queryStr );
            
	        Timers timers = new Timers();               
	
	        timers.startTimer( "total" );
	        	        
	        Query query = new Query( queryStr );
	       
	        String sourceURL = (data != null) ? data : query.getSourceURL();
	        if( sourceURL == null )
	            throw new UnsupportedFeatureException("RDQL query must have a FROM clause");
	        else if( sourceURL.equals( loadedOnt ) ) { 
		        query.setSource( model );
	        }		       
	        else {
		        timers.startTimer( "parse" );
		                
		        model.read( sourceURL );
		
		        timers.stopTimer( "parse" );
		        
		        query.setSource( model );
		        
		        timers.startTimer( "prepare" );
		
		        model.prepare();
		        
		        timers.stopTimer( "prepare" );
		        
		        if( classify ) {
			        timers.startTimer( "classify" );
					
		            ((PelletInfGraph)model.getGraph()).classify();
		        
		            timers.stopTimer( "classify" );
		        }
	        }
	        
//	        ABox.DEBUG = true;

	        loadedOnt = sourceURL;
	       
	        timers.startTimer( "query" );
	
	        QueryResults results = new PelletQueryEngine( query ).exec();
	        
	        timers.stopTimer( "query" );
	        
	        // rest is formatting results for output so stop the main timer now
	        timers.stopTimer( "total" );
	        
	        // number of distinct bindings
	        int count = 0;
	        
	        // if query is in the form SELECT * and we are not printing the results 
	        // we can simply iterate through results and count. otherwise since Jena 
	        // bindings may contain duplicates (binding where only undistinguished
	        // variables differ) we need to extract actual bindings and do the count 
	        if( printOnlyNumbers && query.getResultVars().containsAll( query.getBoundVars()  ) ) {
	            while( results.hasNext() ) {
	                results.next();
	                count++;
	            }
	        }
	        else {
		        NodeFormatter formatter = null;
		        
		        if( !printOnlyNumbers) {
		            formatter = new NodeFormatter(model, formatHTML); 
			        addDefaultQNames(formatter.getQNames());
		        }
		                
		        // variables used in select
		        List resultVars = query.getResultVars();
		        
		        // store the formatted results in a set to avoid duplicates but keep 
		        // the same order by using LinkedHashSet
		        Set data = new LinkedHashSet();
		        while( results.hasNext() ) {
		            ResultBinding binding = (ResultBinding) results.next();
		            List formattedBinding = new ArrayList();
		            for(int i = 0; i < resultVars.size(); i++) {
		                String var = (String) resultVars.get(i);
		                RDFNode result = (RDFNode) binding.get(var);
		                                
		                // format only if we are printing
		                if(printOnlyNumbers)
		                    formattedBinding.add(result);
		                else
		                    formattedBinding.add(formatter.format(result));
		            }
		            
		            if( data.add(formattedBinding) ) 
		                count++;	            
		        }
		        
		        if( !printOnlyNumbers ) {
			        TableData table = new TableData(data, resultVars);
			        table.print(System.out, formatHTML);
			        System.out.println();
		        }        
		    }
		        
	        System.out.println( "Number of answers: " + count );
	        
	        if( printTime ) {
	            System.out.println();            
	            System.out.println( "Total time       : " + timers.getTimer( "total" ).getTotal() );
	            if( timers.getTimer( "parse" ) != null ) {
		            System.out.println( "Parsing RDF file : " + timers.getTimer( "parse" ).getTotal() );
		            System.out.println( "Preprocessing    : " + timers.getTimer( "prepare" ).getTotal() );
		            if( classify )
		                System.out.println( "Classify         : " + timers.getTimer( "classify" ).getTotal() );
		            System.out.println( "Query time       : " + timers.getTimer( "query" ).getTotal() );
	            }
	            
	            if( detailedTime ) {
	                System.out.println(); 
	                System.out.println("Detailed timing about reasoner internals:"); 
	                ((PelletInfGraph) model.getGraph()).getKB().timers.print();
	            }
	        }
	        
	        System.out.println();
        }
    }
    
    private static void addDefaultQNames(QNameProvider qnames) {
        qnames.setMapping("tce-service", "http://www.flacp.fujitsulabs.com/tce/ontologies/2004/03/service.owl#");
        qnames.setMapping("tce-object", "http://www.flacp.fujitsulabs.com/tce/ontologies/2004/03/object.owl#");

        String owls = "http://www.daml.org/services/owl-s/";
        String[] versions = {"0.9", "1.0", "1.1"};
        String[] add = {"-0.9", "-1.0", ""};
        String[] files = {"Service", "Profile", "Process", "Grounding"};
        for(int version = 0; version < versions.length; version++) {
            for(int file = 0; file < files.length; file++) {
                String prefix = files[file].toLowerCase() + add[version];
                String uri = owls + versions[version] + "/" + files[file] + ".owl#";
                qnames.setMapping(prefix, uri);
            }
        }

    }
    
    public static void usage() {
		System.out.println("PelletQuery");
		System.out.println("");
		System.out.println("Reads one (or more) RDQL queries from a string or an input file and");
		System.out.println("runs each one separately. Multiple RDQL queries can be separated by");
		System.out.println("one or more whitespace characters. If an ontology is queried more than");
		System.out.println("once it will be loaded only once and reused for subsequent queries.");
		System.out.println("");		
        System.out.println("usage: java PelletQuery OPTIONS [<rdqlQuery> | -f <rdqlFile]");
        System.out.println("  -t            Print time about how long query answering takes. 'd'");
        System.out.println("                option causes a more detailed time information to be");
        System.out.println("                printed");
        System.out.println("  -c            Classify the KB before answering queries");
        System.out.println("  -q            Print only the number of answers not the bindings");
        System.out.println("  -d <dataURI>  Use the given URI as the data source (the FROM clause");
        System.out.println("                in the queries will be ignored)");
        System.out.println("  -h            Print this information");        
    }
    
    public static void main(String[] args) throws Exception {
        PelletQuery pelletQuery = new PelletQuery();

        String query = "";
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];

			if (arg.equals("-h")) {
				usage();
				System.exit(0);
			} 
			else if (arg.equals("-dt")) {
				pelletQuery.setPrintTime( true );
				pelletQuery.setDetailedTime( true );
			}
			else if (arg.equals("-t"))
				pelletQuery.setPrintTime( true );
			else if (arg.equals("-q"))
				pelletQuery.setQuiet(true);
			else if (arg.equals("-d"))
				pelletQuery.setData( args[++i] );
			else if (arg.equals("-c"))
				pelletQuery.setClassify( true );
			else if(arg.startsWith("-f")) {
				query = FileUtils.readFile( args[++i] );
			}
			else if(i == args.length - 1) {
				query = args[i];
			}
			else {
			    System.err.println( "Unknown option; " + arg );
				usage();
				System.exit(1);			    
			}
		}
		
		if( query == null ) {
		    System.err.println( "No query is given" );
		    usage();
		    System.exit(1);
		}

        pelletQuery.run( query );
    }    
}
