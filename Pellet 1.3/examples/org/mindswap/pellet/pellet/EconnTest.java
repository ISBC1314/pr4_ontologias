/*
 * Created on Oct 27, 2004
 */
package org.mindswap.pellet;

import org.mindswap.pellet.jena.OWLReasoner;
import org.mindswap.pellet.taxonomy.Taxonomy;

/**
 * @author Evren Sirin
 */
public class EconnTest {
    public static void main(String[] args) throws Exception {
    	OWLReasoner reasoner = new OWLReasoner();
    	reasoner.setEconnEnabled( true );
    	
    	reasoner.load( "http://www.mindswap.org/2004/multipleOnt/FactoredOntologies/EasyTests/Easy1/people.owl" );

		EconnectedKB kb = (EconnectedKB) reasoner.getKB();

		kb.printClassTree();
		
//		kb.timers.print();
    }
}
