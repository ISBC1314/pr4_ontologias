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
package org.mindswap.pellet.query;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.jena.ModelReader;
import org.mindswap.pellet.jena.OWLReasoner;
import org.mindswap.pellet.output.TableData;
import org.mindswap.pellet.query.impl.ARQParser;
import org.mindswap.pellet.utils.AlphaNumericComparator;
import org.mindswap.pellet.utils.FileUtils;
import org.mindswap.pellet.utils.Timer;
import org.mindswap.pellet.utils.Timers;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * @author Evren Sirin
 */
public class PelletQueryTest {
    private boolean formatHTML;
    private boolean detailedTime;
    private boolean printOnlyNumbers;
    private boolean classify;
    private boolean realize;
    private boolean printQuery;
    private int limit;
    private int queryIndex;
    private Timers timers;
    private QueryParser parser;

    public PelletQueryTest() {
        printOnlyNumbers = false;
        formatHTML = false;
        limit = 100;
        queryIndex = -1;
        timers = new Timers();
        parser = QueryEngine.createParser();
    }  
    
    public boolean isPrintQuery() {
        return printQuery;
    }
    
    public void setPrintQuery(boolean printQuery) {
        this.printQuery = printQuery;
    }
    
    public boolean isClassify() {
        return classify;
    }

    public void setClassify(boolean classify) {
        this.classify = classify;
    }

    public boolean isRealize() {
        return realize;
    }

    public void setRealize(boolean realize) {
        this.realize = realize;
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
    
    public int getLimit() {
        return limit;
    }
    
    public void setLimit( int limit ) {
        this.limit = limit;
    }
    
    public boolean isDetailedTime() {
        return detailedTime;
    }
    
    public void setDetailedTime(boolean detailedTime) {
        this.detailedTime = detailedTime;
    }

    public int getQueryIndex() {
        return queryIndex;
    }

    public void setQueryIndex( int queryIndex ) {
        this.queryIndex = queryIndex - 1;
    }

    public void run( String queryLoc, String data ) throws Exception {
        Timer t = null;

        KnowledgeBase kb = null;        
        kb = loadData( data );

        Query[] queries = readQueries( kb, queryLoc );
        
        t = timers.startTimer( "queryPrepare" );
//        kb.getSizeEstimate().computeAll();
        QueryEngine.prepare( queries );
        t.stop();
        long queryPrepareTime = t.getLast();

        long parseTime = timers.getTimerTotal( "parse" );
        long consTime = timers.getTimerTotal( "consistency" );   
        long classifyTime = timers.getTimerTotal( "classify" );
        long totalSetupTime = consTime + classifyTime + queryPrepareTime;

        System.out.println( "Parsing/Loading  : " + parseTime );
        System.out.println( "Consistency      : " + consTime );
        System.out.println( "Classify         : " + classifyTime );
        System.out.println( "Query Prepare    : " + queryPrepareTime );
        System.out.println( "Total Setup      : " + totalSetupTime );
   
        TableData table = new TableData( Arrays.asList( new String[] { "No", "Answers", "Time" } ) );
        table.setAlignment( new boolean[] { false, true, true } );
        table.add( Arrays.asList(new String[] { "Consistency", "", "" + consTime }) );
        table.add( Arrays.asList(new String[] { "Classify", "", "" + classifyTime }) );       
        table.add( Arrays.asList(new String[] { "Query Prepare", "", "" +queryPrepareTime }) );
        table.add( Arrays.asList(new String[] { "Total Setup", "", "" + totalSetupTime }) );
        
        for(int j = 0; j < queries.length; j++) {
            if( queryIndex != -1 && j != queryIndex )
                continue;
            
            List row = new ArrayList();
            
            Query query = queries[j];
            if( printQuery )
                System.out.println( query );

	        row.add( "Q" + (j+1) );
            if( queries.length > 1)
                System.out.println( "Query (" + (j+1) + ")" );
            
            t = timers.startTimer( "queryPrepare" );
            
            QueryEngine.prepare( query );
            
            t.stop();
            
	        t = timers.startTimer( "query" );
	
	        QueryResults results = QueryEngine.exec( query );
	        
            t.stop();
            
	        long queryTime = t.getLast();
            
	        if( !printOnlyNumbers ) 
	            System.out.println( results );
		    		        
	        // number of distinct bindings
	        int count = results.size();
	        System.out.println( "Number of answers: " + count );
	        row.add( String.valueOf( count ) );

	        row.add( String.valueOf( queryTime ) );
	        table.add( row );
	        
	        System.out.println();
        }
        
        System.out.println( table ); 

        if( detailedTime ) {
            System.out.println(); 
            System.out.println("Detailed timing about reasoner internals:"); 
            kb.timers.print();
        }
    }

    public KnowledgeBase loadData( String sourceURL ) throws Exception {
        KnowledgeBase kb = readData( sourceURL );
        
        Timer t;

        t = timers.startTimer( "consistency" );
        kb.isConsistent();
        t.stop();

        if( classify ) {
	        t = timers.startTimer( "classify" );
			
            kb.classify();
        
            t.stop();
        }
        
        if( realize ) {
	        t = timers.startTimer( "realize" );
			
            kb.realize();
        
            t.stop();
        }
        
        return kb;
    }
    
    public KnowledgeBase readData( String sourceURL ) throws Exception {
        Timer t = timers.startTimer( "parse" );

        OWLReasoner reasoner = new OWLReasoner();
        reasoner.setDiscardJenaGraph( true );
        
        ModelReader reader = new ModelReader();
        long triples = 0;
        URL url = new URL( sourceURL );
        if( url.getProtocol().equals( "file" ) ) {
            File file = new File( url.getPath() );
            if(file.exists()) {
                if(file.isDirectory()) {
            		File[] files = file.listFiles( new FilenameFilter() {
            			public boolean accept(File dir, String name) {
            				return dir != null && name.endsWith(".owl");
            			}			
            		});		            
            		Arrays.sort( files, AlphaNumericComparator.CASE_INSENSITIVE );
            		int size = (files.length > limit) ? limit : files.length;
		    		for (int j = 0; j < size; j++) {
		    			String fileURI = files[j].toURI().toString();
		    			System.out.println("Reading file " + fileURI);	
		    	        Model model = reader.read( fileURI );
		    			triples += model.size();
		    			reasoner.load( model );
		    		}
                }
                else  {
                    Model model = reader.read( sourceURL );
                    triples += model.size();
                    reasoner.load( model );
                }
            }            
        }
        else  {
            Model model = reader.read( sourceURL );
            triples += model.size();
            reasoner.load( model );
        }
        
//        t.stop();
        
        System.out.println( triples + " triples" );
        
//        OWLReasoner reasoner = new OWLReasoner();
//        t = timers.startTimer( "load" );
//        reasoner.load( model );
        t.stop();
        
        return reasoner.getKB();
    }
    
    private String ensureFilePath( String str ) {
        try {
            URL url = new URL( str );
            return url.getPath();
        }
        catch( MalformedURLException e ) {
            return str;
        }
    }
    
    
    public Query[] readQueries( KnowledgeBase kb, String loc ) throws Exception {
        List queries = new ArrayList();

        System.out.print( "Reading queries..." );
        File file = new File( ensureFilePath( loc ) );
        if(file.exists()) {
            if(file.isDirectory()) {
        		File[] files = file.listFiles( new FilenameFilter() {
        			public boolean accept(File dir, String name) {
        				return dir != null && name.endsWith(".sparql");
        			}			
        		});		            
        		Arrays.sort( files, AlphaNumericComparator.CASE_INSENSITIVE );
        		int size = files.length;
	    		for (int j = 0; j < size; j++) {
	    		    //System.out.println( files[j] );
	    			String queryStr = FileUtils.readFile( files[j] );                    
                    Query query = parser.parse( queryStr, kb );                    
	    		    //System.out.println( query );
	    			queries.add( query );
	    		}
            }
            else  {
    			String query = FileUtils.readFile( file );
    			queries.add( query );
            }
        }            
        System.out.println( "(" + queries.size() + ") done. " );

        return (Query[]) queries.toArray( new Query[queries.size()] );
    }
    
    public static void usage() {
		System.out.println("PelletQuery");
		System.out.println("");
		System.out.println("Reads one (or more) SPARQL queries from an input file (or directory)");
		System.out.println("and runs each one. Data is loaded once and then all queries are");
		System.out.println("executed consecutively.");
		System.out.println("");		
        System.out.println("usage: java PelletQuery OPTIONS -d <data> -q <query>");
        System.out.println("  -d <data>     URI for an ontology or a directory that contains multiple");
        System.out.println("                ontologies");
        System.out.println("  -l <limit>    If a directory is given for data do not load more than");
        System.out.println("                <limit> ontologies");
        System.out.println("  -q <query>    Either a file containing a SPARQL query or a directory");
        System.out.println("                that contains multiple such files)");
        System.out.println("  -p            Print the query before printing answers");
        System.out.println("  -n            Print only the number of answers not the actual answers");
        System.out.println("  -t            Print detailed time information");
        System.out.println("  -c[r]         Classify the KB before answering queries. 'r' option");
        System.out.println("                can be used to realize all the instances");
        System.out.println("  {-h,-help}    Print this information");        
    }
    
    public static void main(String[] args) throws Exception {
        PelletQueryTest pelletQuery = new PelletQueryTest();

        String query = null;
        String data = null;
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];

			if (arg.equals("-h") || arg.equals("-help")) {
				usage();
				System.exit(0);
			} 
			else if (arg.equals("-p"))
				pelletQuery.setPrintQuery( true );
			else if (arg.equals("-t"))
				pelletQuery.setDetailedTime( true );
			else if (arg.equals("-n"))
				pelletQuery.setQuiet(true);
			else if (arg.equals("-l"))
				pelletQuery.setLimit( Integer.parseInt( args[++i] ) );
			else if (arg.equals("-d"))
				data = args[++i];
			else if (arg.equals("-c"))
				pelletQuery.setClassify( true );
			else if (arg.equals("-cr")) {
				pelletQuery.setClassify( true );
				pelletQuery.setRealize( true );
			}
			else if(arg.startsWith("-q")) {
				query =args[++i];
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
		
		if( data == null ) {
		    System.err.println( "No data is given" );
		    usage();
		    System.exit(1);		    
		}

        pelletQuery.run( query, data );
    }    
}
