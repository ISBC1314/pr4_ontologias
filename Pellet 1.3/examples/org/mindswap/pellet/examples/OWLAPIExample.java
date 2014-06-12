package org.mindswap.pellet.examples;
import java.net.URI;
import java.util.Iterator;
import java.util.Set;

import org.mindswap.pellet.owlapi.Reasoner;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLDataProperty;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLObjectProperty;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.helper.OntologyHelper;


/*
 * Created on Oct 10, 2004
 */

/**
 * @author Evren Sirin
 */
public class OWLAPIExample {
    public final static void main(String[] args) throws Exception  {
		String file = "http://www.mindswap.org/2004/owl/mindswappers#";
		
		System.out.print("Reading file " + file + "...");
		Reasoner reasoner = new Reasoner();

		OWLOntology ontology = OntologyHelper.getOntology(URI.create(file));
		System.out.println("done.");
		
		reasoner.setOntology(ontology);
		
		reasoner.getKB().realize();
		reasoner.getKB().printClassTree();
		
		// create property and resources to query the reasoner
		OWLClass Person = reasoner.getClass(URI.create("http://xmlns.com/foaf/0.1/Person"));
		OWLObjectProperty workHomepage = reasoner.getObjectProperty(URI.create("http://xmlns.com/foaf/0.1/workInfoHomepage"));
		OWLDataProperty foafName = reasoner.getDataProperty(URI.create("http://xmlns.com/foaf/0.1/name"));
		
		// get all instances of Person class
		Set individuals = reasoner.allInstancesOf(Person);
		for(Iterator i = individuals.iterator(); i.hasNext(); ) {
		    OWLIndividual ind = (OWLIndividual) i.next();
		    
		    // get the info about this specific individual
		    String name = (String) reasoner.getPropertyValue(ind, foafName).getValue();
		    OWLClass type = reasoner.typeOf(ind);
		    OWLIndividual homepage = reasoner.getPropertyValue(ind, workHomepage);
		    
		    // print the results
		    System.out.println("Name: " + name);
		    System.out.println("Type: " + type.getURI().getFragment());
		    if(homepage == null)
		        System.out.println("Homepage: Unknown");
		    else
		        System.out.println("Homepage: " + homepage.getURI());
		    System.out.println();
		}
	}
}
