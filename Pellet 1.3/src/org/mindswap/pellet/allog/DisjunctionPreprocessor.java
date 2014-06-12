/*
 * Created on Nov 18, 2004
 */
package org.mindswap.pellet.allog;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.RDFWriter;

/**
 * @author Evren Sirin
 */
public class DisjunctionPreprocessor {
    public static void usage() {
		System.out.println("DisjunctionPreprocessor");
		System.out.println("");
		System.out.println("Create a new ontology that imports the given input ontology and");
		System.out.println("additional class descriptions, nemaly disjunction (union) classes");
		System.out.println("that are defined in the disjuncitonFile. The output is saved at");
		System.out.println("the location specified by the outputFile paramater.");
		System.out.println("");
		System.out.println("Usage: java DisjunctionPreprocessor <inputOntologyURI> <outputFile> <disjunctionFile>");
    }
    
    public static void main(String[] args) throws FileNotFoundException, IOException {
        if(args.length != 3) {
            usage();
            System.exit(0);
        }
        
        String inputOntologyURI = args[0];
        String outputFile = args[1];
        String disjunctionFile = args[2];
        
        BufferedReader in = new BufferedReader(new FileReader(disjunctionFile));
        
        OntModel model = ModelFactory.createOntologyModel();
        Ontology ont = model.createOntology("");
        ont.addImport(model.getResource(inputOntologyURI));
        
        String line = in.readLine();
        while(line != null) {
            line = line.replace('[', ' ').replace(']', ' ');
            
            String[] input = line.split(",");
            RDFNode[] nodes = new RDFNode[input.length - 1];
            for(int i = 1; i < input.length; i++) {
                nodes[i - 1] = model.createResource(input[i].trim());
            }
                               
            model.createUnionClass(input[0], model.createList(nodes));
            
            line = in.readLine();
        }
        
        Model baseModel = model.getBaseModel();
        
        RDFWriter writer = baseModel.getWriter("RDF/XML-ABBREV");
        writer.setProperty("allowBadURIs", "true");
        
        writer.write(baseModel, new FileOutputStream(outputFile), "");
    }
}
