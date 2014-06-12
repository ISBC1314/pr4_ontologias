/*
 * Created on Nov 4, 2004
 */
package org.mindswap.pellet.allog;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Pattern;

import org.mindswap.pellet.jena.OWLReasoner;
import org.mindswap.pellet.utils.FileUtils;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdql.QueryResults;
import com.hp.hpl.jena.rdql.ResultBinding;

/**
 * Reads a set of RDQL queries from an input file and run them on the given
 * input ontology. The result is printed as a set of variable bindings.  
 * 
 * @author Evren Sirin
 */
public class MultiQuery {
    public static void usage() {
		System.out.println("MultiQuery");
		System.out.println("");
		System.out.println("Reads a set of RDQL queries from an input file and run them on the given");
		System.out.println("input ontology. The result is printed as a set of variable bindings.");
		System.out.println("If there are multiple variables in a query, bindings for each variable");
		System.out.println("is printed on the same line separated with ',' character. After all the");
		System.out.println("result sets for a query is printed, an empty line is printed and then");
		System.out.println("the result sof the next query. If there are no answers for a query only");
		System.out.println("the string 'FALSE' is printed on one line. If the query has no variables");
		System.out.println("and it succeeds then the string 'TRUE' is printed in one line.");
		System.out.println("if output file is given results are saved to the file, otherwise printed");
		System.out.println("on console.");
		System.out.println("");
		System.out.println("Usage: java MultiQuery <inputOntologyURI> <queryFile> [<outputFile>]");
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {        
        PrintWriter out = new PrintWriter(System.out);
      
        if(args.length < 2 || args.length > 3) {
            usage();
            System.exit(0);
        }
        
        String inputOntologyURI = FileUtils.toURI(args[0]);
        String queryFile = args[1];        

        if(args.length == 3)
            out = new PrintWriter(new FileWriter(args[2]));
        
        OWLReasoner reasoner = new OWLReasoner();
        reasoner.load(inputOntologyURI);
        
        String input = FileUtils.readFile(queryFile).trim();
        String[] queries = Pattern.compile("SELECT", Pattern.CASE_INSENSITIVE).split(input);
        for(int i = 1; i < queries.length; i++) {
            String query = "SELECT " + queries[i];
            
            QueryResults results = reasoner.runQuery(query);
            int varCount = results.getResultVars().size();

            if(results.hasNext()) {
                if(results.getResultVars().isEmpty()) {
                    out.println("TRUE");
                    out.println();
                    continue;
                }
                
	            while(results.hasNext()) {
	                ResultBinding binding = (ResultBinding) results.next();
	                
		            for(int col = 0 ; col < varCount; col++ ) {
		                String var = (String) results.getResultVars().get(col); 
		                Object value = binding.get(var);
		                
		                if(col > 0)
		                    out.print(",");
		                
		                out.print(var + "=");
		                if(value instanceof Literal) {
		                    Literal l = (Literal) value;
		                    out.print("\"" + l.getLexicalForm() + "\"");
		                }
		                else {
		                    Resource r = (Resource) value;
		                    out.print("<" + r.getURI() + ">");
		                }
		            }
		            out.println();
	            }
            }
            else
                out.println("FALSE");
            out.println("-");
        }
        
        out.flush();
        out.close();
     }
}
