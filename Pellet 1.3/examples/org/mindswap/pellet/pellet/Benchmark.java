package org.mindswap.pellet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mindswap.pellet.jena.ModelReader;
import org.mindswap.pellet.jena.OWLReasoner;
import org.mindswap.pellet.output.TableData;
import org.mindswap.pellet.utils.Timer;
import org.mindswap.pellet.utils.Timers;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * @author Evren Sirin
 */
public class Benchmark {

	static String[] DEFAULT_FILES = {
	    "Mindswappers", "http://www.mindswap.org/2004/owl/mindswappers",
		"AKT Portal", "http://www.aktors.org/ontology/portal",
		"OWL-S CongoService", "http://www.daml.org/services/owl-s/1.1/CongoService.owl",
	    "Food", "http://www.w3.org/2001/sw/WebOnt/guide-src/food.owl",		
		"SUMO", "http://reliant.teknowledge.com/DAML/SUMO.owl",
	    "Tambis", "http://www.cs.man.ac.uk/~horrocks/OWL/Ontologies/tambis-full.owl",
		"Financial", "http://www.cs.put.poznan.pl/alawrynowicz/financial.owl",
		"SWEET-JPL", "http://sweet.jpl.nasa.gov/ontology/earthrealm.owl",
    	"Wine", "http://www.w3.org/2001/sw/WebOnt/guide-src/wine.owl",
		"Galen", "http://www.cs.man.ac.uk/~horrocks/OWL/Ontologies/galen.owl",
	};
	
	public static void main(String[] args) throws Exception {
		String[] files = DEFAULT_FILES;
		
		boolean formatHTML = false;
		for( int i = 0; i < args.length; i++ ) {
		    if( args[i].equals( "-html" ) )
		        formatHTML = true;
		    else
		        System.err.println( "Unknown option " + args[i] );
        }

		for( int k = 0; k < 1; k++ ) {
		    System.out.print( "Run " + (k + 1 )  + " ");
	        List cols = Arrays.asList(new String[] {
	            "Name", "OWL Species", "DL Expressivity", "Triples", "Classes", "Properties", "Individuals",
	            "Loading", "Consistency", "Classify", "Realize", "Total"});
	        TableData table = new TableData(cols); 
	        
			for(int i = 0; i < files.length; i += 2) {	
				try {
				    String name = files[ i ];
					String file = files[ i+1 ];
					
					System.out.print( "." );
					
//				    System.out.print( (i+1) + " " + name );

				    List list = new ArrayList();
			        if( formatHTML )
			            list.add( "<a href=\"" + file + "\">" + name + "</a>" );
			        else
			            list.add( name );
					
					ModelReader reader = new ModelReader();
					Model model = reader.read( file );
					
//					System.out.println(" Triples: " + model.size());
					
					Timer t = new Timer( "timer" );
					
					OWLReasoner reasoner = new OWLReasoner();
					// measure loading time and consistency checking
					t.start();
					reasoner.load( model );
					reasoner.isConsistent();
					t.stop();
					// do not measure species validation time
					list.add(reasoner.getSpecies().toString());
					list.add(reasoner.getKB().getExpressivity());
					list.add(new Long(model.size()));
					
					KnowledgeBase kb = reasoner.getKB();
					list.add(new Long(kb.getClasses().size()));
					list.add(new Long(kb.getProperties().size()));
					list.add(new Long(kb.getIndividuals().size()));
					
					// measure classification and realization time
					t.start();
					reasoner.realize();
					t.stop();
					
					Timers timers = reasoner.getKB().timers;
			        list.add(new Double((timers.getTimer("Loading").getTotal()+timers.getTimer("preprocessing").getTotal())/1000.0));
			        list.add(new Double(timers.getTimer("consistency").getTotal()/1000.0));
			        list.add(new Double(timers.getTimer("classify").getTotal()/1000.0));
			        list.add(new Double(timers.getTimer("realize").getTotal()/1000.0));
			        list.add(new Double(t.getTotal()/1000.0));
			        
//			        System.err.println( list );
			        
			        table.add(list);
			        
//			        kb.timers.print();
				} catch (Throwable e) {
					e.printStackTrace();
//					System.exit(0);
				}
			}
			
			System.out.println();
			table.print(System.out, formatHTML);
		}
	}
}
