/*
 * Created on Oct 12, 2004
 */
package org.mindswap.pellet.examples;
import java.util.ArrayList;
import java.util.List;

import org.mindswap.pellet.exceptions.UnsupportedFeatureException;
import org.mindswap.pellet.jena.NodeFormatter;
import org.mindswap.pellet.jena.PelletQueryEngine;
import org.mindswap.pellet.jena.PelletReasonerFactory;
import org.mindswap.pellet.output.TableData;
import org.mindswap.pellet.utils.QNameProvider;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdql.Query;
import com.hp.hpl.jena.rdql.QueryResults;
import com.hp.hpl.jena.rdql.ResultBinding;

/**
 * This program executes an RDQL and then pretty-prints the results using OWL Abstract Syntax. 
 * 
 * @author Evren Sirin
 */
public class PelletQueryExample {
    public static void main(String[] args) throws Exception {
        if(args.length != 1) {
            System.out.println("usage: java PelletQueryExample <rdqlQuery>");
            System.out.println();
            return;
        }
        
        PelletQueryExample base = new PelletQueryExample();
        base.run(args[0], false);
    }
    
    public void run(String queryStr, boolean formatHTML) throws Exception {            
        Query query = new Query( queryStr );
         
        // create an empty ontology model using Pellet spec
        OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );        
        model.setStrictMode( false );
       
        if( query.getSourceURL() == null )
            throw new UnsupportedFeatureException("RDQL query must have a FROM clause");
        
        model.read( query.getSourceURL() );
        
        query.setSource( model );
	    
        QueryResults results = PelletQueryEngine.exec( query );
        
        // create a node formatter
        NodeFormatter formatter = new NodeFormatter(model, formatHTML); 
        
        addDefaultQNames(formatter.getQNames());
                
        // variables used in select
        List resultVars = query.getResultVars();
        
        // store the formatted results an a table 
        TableData table = new TableData( resultVars );
        while( results.hasNext() ) {
            ResultBinding binding = (ResultBinding) results.next();
            List formattedBinding = new ArrayList();
            for(int i = 0; i < resultVars.size(); i++) {
                String var = (String) resultVars.get(i);
                RDFNode result = (RDFNode) binding.get(var);
                                
                formattedBinding.add(formatter.format(result));                
            }
            
            table.add(formattedBinding);
        }
        
        table.print(System.out, formatHTML);
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
}
