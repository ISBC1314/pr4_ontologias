package org.mindswap.pellet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.mindswap.pellet.jena.ModelReader;
import org.mindswap.pellet.jena.OWLReasoner;
import org.mindswap.pellet.output.TableData;
import org.mindswap.pellet.taxonomy.Taxonomy;
import org.mindswap.pellet.test.PelletTestCase;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Timer;

import aterm.ATermAppl;
import aterm.ATermList;

import com.hp.hpl.jena.rdf.model.Model;

/*
 * Created on Aug 14, 2004
 */

/**
 * @author Evren Sirin
 */
public class TestOptimizations extends PelletTestCase {
    public static int repeat = 1;
    
	public final static void main(String[] args) throws Exception {
	    String file = null;
	
//        file = "http://www.mindswap.org/2004/multipleOnt/FactoredOntologies/AKT/AKTfull.owl";
//        file = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl";
//        file = "file:files/combined_wine.owl";
//      file = "file:files/sweet";
//        file = "file:files/3Sat_20_50.owl";
//		file = "http://sweet.jpl.nasa.gov/ontology/earthrealm.owl";
//		file = "http://www.daml.org/services/owl-s/1.1/CongoService.owl";
//		file = "file:/fla/tce";	    
		file = "http://www.w3.org/2001/sw/WebOnt/guide-src/wine.owl" ;
//      file = "http://www.w3.org/2002/03owlt/description-logic/consistent501.rdf";
	    
	    if( args.length > 0 )
	        file = args[0];
	    if( args.length > 1 )
	        repeat = Integer.parseInt( args[1] );
	    
		TestOptimizations test = new TestOptimizations();
		test.run( file, repeat );
	}
	
	public void run( String file, int repeat ) {	    
//		PelletOptions.USE_PSEUDO_NOMINALS = true;
//		PelletOptions.COPY_ON_WRITE = true;		
//		PelletOptions.USE_UNIQUE_NAME_ASSUMPTION = false;
//		PelletOptions.USE_DISJUNCTION_SORTING = PelletOptions.OLDEST_FIRST;		
//		PelletOptions.DEFAULT_COMPLETION_STRATEGY = SHNStrategy.class;

		// Debugging options
//		PelletOptions.PRINT_SIZE = true;
//		Node.DEBUG = true;
//		ABox.DEBUG = true;
//        KnowledgeBase.DEBUG = true;
//		PelletOptions.USE_QNAME = true;
	    
	    System.out.println( file );
 
	    Model model = ModelReader.read( file, ".*owl", 500 );
//        Model model = new ModelReader().read(file);
        
        OWLReasoner reasoner = new OWLReasoner();           
        final KnowledgeBase kb = reasoner.getKB();
        
        reasoner.load( model );
        
        System.out.println(kb.getExpressivity().getNominals());
        
        System.out.println( kb.getInfo() );

	    boolean[][] options = {
//            {true,  true,  true,  true,  true, true, true},
//            {true,  true,  true,  true,  true, true, true},
//	        {true,  true,  false, true,  true, true, true},
//            {true,  true,  true,  false, true, true, true},
//	        {true,  true,  true,  true,  false, true, true},
//            {false, true,  true,  true,  true, true, true},
//            {true,  false, true,  true,  true, true, true},
//            {false, false, true,  true,  true, true, true},
//            {true,  true,  true,  true,  true, false, true},
            {false,  true,  true,  true,  false, true, true},            
	    };
	    
        TableData table = new TableData( new String[] {
            "Options", "Consistency", " Classify", "Realize", "# Cons", "# Subs", "# Inst"} );
	    for( int i = 0; i < options.length; i++ ) {
            try {
			PelletOptions.USE_NOMINAL_ABSORPTION  = options[i][0];
            PelletOptions.USE_HASVALUE_ABSORPTION = options[i][1];
		    PelletOptions.USE_DISJUNCT_SORTING    = options[i][2];
			PelletOptions.CHECK_NOMINAL_EDGES     = options[i][3];
			PelletOptions.USE_SMART_RESTORE       = options[i][4];
            PelletOptions.COPY_ON_WRITE           = options[i][5];
            PelletOptions.USE_PSEUDO_MODEL        = options[i][6];
			
            String name = "";
            if( options[i][0] ) name += "O"; else name += "_";
            if( options[i][1] ) name += "H"; else name += "_";
            if( options[i][2] ) name += "D"; else name += "_";
            if( options[i][3] ) name += "M"; else name += "_";
            if( options[i][4] ) name += "B"; else name += "_";
            if( options[i][5] ) name += "C"; else name += "_";
            if( options[i][6] ) name += "P"; else name += "_";
            
			System.out.println( "OPTIONS = " + name);
			System.out.println( "   ONEOF ABSORPTION    = " + options[i][0]);
            System.out.println( "   HASVALUE ABSORPTION = " + options[i][1]);
			System.out.println( "   LEARNING DISJUNCT   = " + options[i][2]);
			System.out.println( "   NOMINAL MODEL MERGE = " + options[i][3]);
			System.out.println( "   PARTIAL BACKJUMPING = " + options[i][4]);
            System.out.println( "   COPY ON WRITE       = " + options[i][5]);
            System.out.println( "   PSEUDO MODEL COMPLE = " + options[i][6]);

            kb.timers.resetAll();
            
			for( int k = 0; k < repeat; k++ ) {
			    System.out.print( (k+1) + ") " );
	
                model = ModelReader.read( file, ".*owl", 500 );
				reasoner.clear();
				reasoner.load( model );
                System.gc();
                System.gc();
				
                Thread thread = new Thread() {
                    public void run() {               
        				Timer t = kb.timers.startTimer( "total" );
        	               
        				kb.ensureConsistency();				
        				System.out.print( "Consistency: " + kb.timers.getTimer( "consistency" ).getLast());
        	
    					kb.classify();
    					System.out.print( " Classify: " + kb.timers.getTimer( "classify" ).getLast());
                        
    					kb.realize();		
    					System.out.print( " Realize: " + kb.timers.getTimer( "realize" ).getLast());
        				t.stop();
        	
        				System.out.println(" Total: " + t.getLast());
                    }
                };
                thread.start();
                thread.join( 500 * 1000 );
                
                if( thread.isAlive() ) {
                    System.out.println( "****** TIMEOUT ******" );
                    thread.stop();
                }
			}
            
            List row = new ArrayList();
            row.add(name);
            row.add(new Double(getAvg(kb.timers.getTimer("consistency"))));
            row.add(new Double(getAvg(kb.timers.getTimer("classify"))));
            row.add(new Double(getAvg(kb.timers.getTimer("realize"))));
            row.add(new Double(getCount(kb.timers.getTimer("isConsistent"))/ repeat));
            row.add(new Double(getCount(kb.timers.getTimer("subClassSat"))/ repeat));
            row.add(new Double(getCount(kb.timers.getTimer("isType"))/ repeat));
            table.add( row );
            }
            catch(Exception e) {
                e.printStackTrace(System.out);
            }
            
			System.out.println();
			kb.timers.print();
	    }
        
        System.out.println( table );
//				printStats( kb.getABox().disjBranchStats );		
	}

    public static double getAvg( Timer timer ) {
        if( timer == null )
            return 0;
        else
            return timer.getAverage();
    }
    
    public static double getCount( Timer timer ) {
        if( timer == null )
            return 0;
        else
            return timer.getCount();
    }
	
	public static void printStats(Map stats) {
	    for(Iterator i = stats.keySet().iterator(); i.hasNext();) {
            ATermAppl disjunction = (ATermAppl) i.next();
            int[] stat = (int[]) stats.get( disjunction );
            
            ATermAppl term = (ATermAppl) disjunction.getArgument(0);            
            ATermList disjuncts = (ATermList) term.getArgument(0);
            System.out.println( disjunction );
            for(int index = 0; !disjuncts.isEmpty(); disjuncts = disjuncts.getNext(), index++) {
                System.out.println( stat[index] + " - " + ATermUtils.negate((ATermAppl) disjuncts.getFirst()));
            }
            
        }
	    
	}

}
