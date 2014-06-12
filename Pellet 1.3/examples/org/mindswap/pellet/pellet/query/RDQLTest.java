/*
 * Created on Oct 12, 2004
 */
package org.mindswap.pellet.query;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.jena.DisjointMultiUnion;
import org.mindswap.pellet.jena.OWLReasoner;
import org.mindswap.pellet.output.TableData;
import org.mindswap.pellet.query.impl.DistVarsQueryExec;
import org.mindswap.pellet.query.impl.OptimizedQueryExec;
import org.mindswap.pellet.query.impl.SimpleQueryExec;
import org.mindswap.pellet.utils.FileUtils;

import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdql.Query;

/**
 * Run an RDQL query using all the different query engines and print the satistics, # of results,
 * time, # of consistency checks, etc.
 * 
 * @author Evren Sirin
 */
public class RDQLTest {
    public static void main(String[] args) throws Exception {
        String queryStr = args[0].trim();
                
        if(!queryStr.substring(0,6).equalsIgnoreCase("SELECT"))
            queryStr = FileUtils.readFile(new File(args[0]));
        
        Query query = new Query( queryStr );
        
        int limit = 5;
        
        Model[] models = null;
        String in = query.getSourceURL();
        URL url = new URL( in );
        if(url.getProtocol().equals("file")) {
            File file = new File( url.getPath() );
            if(file.exists()) {
                if(file.isDirectory()) {
		    		File[] files = file.listFiles();
		    		models = new Model[files.length > limit ? limit : models.length];
		    		for (int j = 0; j < models.length; j++) {
		                models[j] = ModelFactory.createDefaultModel();
		    			String fileURI = files[j].toURI().toString();
		    			System.out.println("Reading file " + fileURI);		    			
		    			models[j].read(fileURI);
		    		}
                }
                else {
                    models = new Model[1];
                    models[0] = ModelFactory.createDefaultModel();
                    models[0].read( file.toURI().toString() );
                }
            }
        }
        else  {
            models = new Model[1];
            models[0] = ModelFactory.createDefaultModel();
            models[0].read( in );            
        }

        

         
        OWLReasoner reasoner = new OWLReasoner();

        DisjointMultiUnion union = new DisjointMultiUnion();
		for (int j = 0; j < models.length; j++) {    			
			union.addGraph(models[j].getGraph());
		}
		
        reasoner.load(ModelFactory.createModelForGraph(union));
        
//        reasoner.realize();
        KnowledgeBase kb = reasoner.getKB();
        
        List cols = Arrays.asList(new String[] {"Name", "Supports", "Size", "Consistency", "Time"});
        TableData table = new TableData(cols); 
        
//        table.addRow(testOldEngine(new DistVarsQueryEngine(), queryStr, kb));
//        table.addRow(testOldEngine(new OptimizedQueryEngine(), queryStr, kb));
//        table.addRow(testOldEngine(new SimpleQueryEngine(), queryStr, kb));

        org.mindswap.pellet.query.Query newQuery = QueryEngine.parse( queryStr, kb, Syntax.syntaxRDQL );
        table.add(testNewEngine(new DistVarsQueryExec(), newQuery));
        table.add(testNewEngine(new OptimizedQueryExec(), newQuery));
        table.add(testNewEngine(new SimpleQueryExec(), newQuery));

        System.out.println(queryStr);
        System.out.println();
        System.out.println(table);
    }

    
    public static List testNewEngine(QueryExec queryExec, org.mindswap.pellet.query.Query q) {
        List list = new ArrayList();
        list.add(getClassName(queryExec));
        
        System.out.println("Running query with NEW " + queryExec.getClass().getName());
        
        boolean supports = queryExec.supports(q);
        System.out.println("Supports " + supports);
		
        QueryResults results = null;
        
		long time = -1;
		int size = -1;
		long satCount = -1;
		try {
		    satCount = q.getKB().getABox().consistencyCount;
		    long start = System.currentTimeMillis();
		    results = queryExec.exec( q );
            time = System.currentTimeMillis() - start;
            satCount = q.getKB().getABox().consistencyCount - satCount;
            
            System.out.println("Size " + results.size());
    		System.out.println(results);
    	        
    		size = results.size();            
        } catch(RuntimeException e) {
            e.printStackTrace();
        }
        System.out.println("Time " + time);
        System.out.println();
        list.add(new Boolean(supports));
        list.add(new Integer(size));
        list.add(new Long(satCount));
        list.add(new Long(time));
        
        return list;
    }
    
//    public static List testOldEngine(org.mindswap.pellet.oldquery.QueryEngine engine, String str, KnowledgeBase kb) throws IOException {
//        List list = new ArrayList();
//        list.add(getClassName(engine));
//        System.out.println("Running query with " + engine.getClass().getName());
//		QueryParser parser = new RDQLQueryParser();
//		//Query q = parser.loadQuery( new URI( args[1] ).toURL().openStream(), kb );
//		org.mindswap.pellet.oldquery.Query q = parser.loadQuery( new StringReader( str ), kb );
//
//		List results = null;
//		long time = -1;
//		int size = -1;
//		long satCount = -1;
//		try {
//		    satCount = kb.getABox().consistencyCount;
//		    long start = System.currentTimeMillis();
//            results = engine.runQuery( q, kb );
//            time = System.currentTimeMillis() - start;
//            satCount = kb.getABox().consistencyCount - satCount;
//            
//            System.out.println("Size " + results.size());
//    		try {
//    			TableQueryResultsFormatter printer = new TableQueryResultsFormatter( results, q.getDistVars(), parser.getVarNames() );
//    			printer.print( System.out );
//    		} catch ( NullPointerException e ) {
//    			TableQueryResultsFormatter printer = new TableQueryResultsFormatter( results, q.getDistVars(), null );
//    			printer.print( System.out );
//    		}
//    		size = results.size();
//        } catch(RuntimeException e) {
//            e.printStackTrace();
//        }
//        System.out.println("Time " + time);
//        System.out.println();
//        list.add(new Boolean(true));
//        list.add(new Integer(size));
//        list.add(new Long(satCount));
//        list.add(new Long(time));
//        
//        return list;
//    }
//
    public static String getClassName( Object obj ) {
        String name = obj.getClass().getName();
        int index = name.lastIndexOf(".");
        
        return name.substring(index + 1);
    }
}
