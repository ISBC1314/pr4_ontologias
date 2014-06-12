package org.mindswap.pellet;
import java.util.Iterator;
import java.util.Set;

import org.mindswap.pellet.utils.Timer;
import org.mindswap.pellet.utils.VersionInfo;

import aterm.ATermAppl;

/*
 * Created on Nov 2, 2005
 */

public class PelletKRSS {
    static String inFile = null;
    static String verifyFile = null;
    
    static boolean classify  = false;
    static boolean realize   = false;
    static boolean findUnsat = false;
    static boolean quiet     = false;    
    static boolean timing    = false;
    
    static int timeout = -1;
    
    static Set unsatClasses;
    
    private static void usage() {
        VersionInfo vinfo = Pellet.getVersionInfo();
        
        System.out.println("PelletKRSS (Version:" + vinfo.getVersionString() + ")");
        System.out.println("");
        System.out.println("Usage: java PelletKRSS OPTIONS");
        System.out.println(" {-if,-inputFile} <file>      Input ontology URI");
        System.out.println(" {-vf,-verifyFile} <file>     Verify the classification results using the <file>");
        System.out.println("                              that contains classification tree. The file should");
        System.out.println("                              be in the format as in the DL benchmark suite");
        System.out.println(" {-c,-classify}               Classify the ontology and display the hierarchy");
        System.out.println("                              as an indented tree");
        System.out.println(" {-r,-realize}                Compute and display the most specific instances");
        System.out.println("                              for each class. When this option is enabled, ");
        System.out.println("                              classification will be automatically done");
        System.out.println(" -unsat                       Find the unsatisfiable classes in the ontology.");
        System.out.println("                              This option is unnecessary if classification is");
        System.out.println("                              selected");
        System.out.println(" -timeout <time>              Timeout after <time> seconds");
        System.out.println(" -timing                      Print detailed timing information");        
        System.out.println(" -quiet                       Don't print classification hierarchy");
        System.out.println(" -version                     Print the version information and exit");
        System.out.println(" -help                        Print this message");
    }

    public static void parseArgs( String[] args ) {
        for( int i = 0; i < args.length; i++ ) {
            String arg = args[i];

            if( arg.equalsIgnoreCase( "-help" ) ) {
                usage();
                System.exit( 0 );
            }
            else if( arg.equalsIgnoreCase( "-version" ) ) {
                VersionInfo vinfo = Pellet.getVersionInfo();
                System.out.println("Version : " + vinfo.getVersionString() );
                System.out.println("Released: " + vinfo.getReleaseDate() );
                System.exit( 0 );
            }
            else if( arg.equalsIgnoreCase( "-classify" ) || arg.equalsIgnoreCase( "-c" ) ) 
                classify = true;
            else if( arg.equalsIgnoreCase( "-realize" ) || arg.equalsIgnoreCase( "-r" ) )
                realize = true;
            else if( arg.equalsIgnoreCase( "-inputFile" ) || arg.equalsIgnoreCase( "-if" ) )
                inFile = args[++i];
            else if( arg.equalsIgnoreCase( "-verifyFile" ) || arg.equalsIgnoreCase( "-vf" ) )
                verifyFile = args[++i];
            else if( arg.equalsIgnoreCase( "-timeout" ) )
                timeout = Integer.parseInt( args[++i] );
            else if( arg.equalsIgnoreCase( "-unsat" ) )
                findUnsat = true;
            else if( arg.equalsIgnoreCase( "-quiet" ) )
                quiet = true;
            else if( arg.equalsIgnoreCase( "-timing" ) )
                timing = true;            
            else {
                System.err.println( "Unrecognized option: " + arg );
                usage();
                System.exit( 1 );
            }
        }        
        
        if( inFile == null ) {
            System.err.println("No input file is given!");
            usage();
            System.exit( 1 );
        }
    }
    
    public static void main( String[] args ) throws Exception {
        parseArgs( args );
        
        String time = "";
        Timer timer = new Timer( "" );
        
        KRSSLoader loader = new KRSSLoader();
        
        KnowledgeBase kb = new KnowledgeBase();
        
        System.out.println( "Input File  : " + inFile );
        
        timer.start();
        loader.load( inFile, kb );
        timer.stop();
        
        time += "Loading: " + timer.getLast();
        
        timer.start();
        kb.prepare();
        timer.stop();
        
        time += " Preprocessing: " + timer.getLast();        
        
        System.out.println( "Expressivity: " + kb.getExpressivity() );
        
        timer.start();
        boolean consistent = kb.isConsistent();
        timer.stop();
        
        time += " Consistency: " + timer.getLast(); 
        
        System.out.println( "Consistent  : " + (consistent?"Yes":"No") );
        
        if( classify || realize ) {
            timer.start();
            kb.classify();
            timer.stop();
            
            time += " Classification: " + timer.getLast(); 
        }
        
        if( realize ) {
            timer.start();
            kb.realize();
            timer.stop();
            
            time += " Realization: " + timer.getLast(); 
        }
        
        if( !kb.isClassified() && findUnsat ) {
            timer.start();
            Iterator i = kb.getClasses().iterator();
            while( i.hasNext() ) {
                ATermAppl c = (ATermAppl) i.next();
                if( !kb.isSatisfiable( c ) )
                    unsatClasses.add( c );
            }
            timer.stop();
            
            time += " Unsatisfiability: " + timer.getLast(); 
            
            System.out.println("Unsatisfiable Concepts: " + unsatClasses );
        }            

        if( verifyFile != null ) {
            timer.start();
            loader.verifyTBox( verifyFile, kb );
            timer.stop();
            
            time += " Verification: " + timer.getLast(); 
            
            System.out.println( "Verified    : Yes (" + verifyFile + ")" );
        }
        
        time = timer.getTotal() + "ms (" + time + ")";
        
        System.out.println( "Time        : " + time );
        System.out.println( );
        
        if( kb.isClassified() && !quiet )
            kb.printClassTree();

        if( timing ) 
            kb.timers.print();        
    }

}
