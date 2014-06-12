/*
 * Created on Oct 26, 2004
 */
package org.mindswap.pellet.examples;

import java.net.URI;
import java.util.Iterator;
import java.util.Set;

import org.mindswap.pellet.jena.ModelReader;
import org.mindswap.pellet.jena.OWLReasoner;
import org.mindswap.pellet.owlapi.Reasoner;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLDataProperty;
import org.semanticweb.owl.model.OWLException;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLObjectProperty;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.helper.OntologyHelper;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Example to demonstrate how to use the reasoner for queries related to individuals. Exact 
 * same functionality is shown for both Jena and OWL-API interfaces. 
 * 
 * @author Evren Sirin
 */
public class IndividualsExample {
    public static void main(String[] args) throws Exception {
        System.out.println("Results using Jena interface");
        System.out.println("----------------------------");
        runWithJena();
        
        System.out.println("Results using OWL-API interface");
        System.out.println("-------------------------------");
        runWithOWLAPI();
    }
    
    public static void runWithJena() {
        // ontology that will be used
        String ont = "http://www.mindswap.org/2004/owl/mindswappers#";

        // read the ontology using model reader so we will get a plain
        // RDF model but owl:imports will also be processed
		ModelReader reader = new ModelReader();
		Model model = reader.read(ont);

		// load the model to the reasoner
		OWLReasoner reasoner = new OWLReasoner();
		reasoner.load(model);
		
		// create property and resources to query the reasoner
		Resource Person = model.getProperty("http://xmlns.com/foaf/0.1/Person");
		Property workHomepage = model.getProperty("http://xmlns.com/foaf/0.1/workInfoHomepage");
		Property foafName = model.getProperty("http://xmlns.com/foaf/0.1/name");
		
		// get all instances of Person class
		Set individuals = reasoner.getInstances(Person);
		for(Iterator i = individuals.iterator(); i.hasNext(); ) {
		    Resource ind = (Resource) i.next();
		    
		    // get the info about this specific individual
		    String name = ((Literal) reasoner.getPropertyValue(foafName, ind)).getString();
		    Resource type = reasoner.getType(ind, true);
		    Resource homepage = (Resource) reasoner.getPropertyValue(workHomepage, ind);
		    
		    // print the results
		    System.out.println("Name: " + name);
		    System.out.println("Type: " + type.getLocalName());
		    if(homepage == null)
		        System.out.println("Homepage: Unknown");
		    else
		        System.out.println("Homepage: " + homepage);
		    System.out.println();
		}
    }
    
	public static void runWithOWLAPI() throws OWLException {
		String ont = "http://www.mindswap.org/2004/owl/mindswappers#";
		
		// read the ontology
		OWLOntology ontology = OntologyHelper.getOntology( URI.create(ont) );
		
		// load the ontology to the reasoner
		Reasoner reasoner = new Reasoner();
		reasoner.setOntology(ontology);
		
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
