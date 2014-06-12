/*
 * Created on Feb 3, 2006
 */
package org.mindswap.pellet.query;

import org.mindswap.pellet.PelletOptions;

public class LUBMTest {
    public static void main( String[] args ) throws Exception {
        PelletOptions.REORDER_QUERY = true;
        
        // 1 univ = 16 files
        // 3 univ = 51 files
        // 5 univ = 94 files        
        String dir = "file:/mindswap/pellet/files/lubm/";
//        dir = "file:/tools/kaon2/ontologies/semintec_2/semintec_2.owl";
//        dir = "file:/tools/kaon2/ontologies/vicodi_2/vicodi_2.owl";
                
        PelletQueryTest test = new PelletQueryTest();
        test.setLimit( Integer.parseInt(args[0]) );
        test.setQuiet( true );
        test.setPrintQuery( false );
        test.setDetailedTime( false );
        test.setClassify( false );
        
//        test.setQueryIndex( 9 );
//        test.run( dir, dir + "semintec_1.owl" );
        test.run( dir + "query", dir + "data" );
        
//        String ns;
//      ns = "http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#";
//        ns = "http://www.owl-ontologies.com/unnamed.owl#";
//      ns = "http://vicodi.org/ontology#";
      
//      String query = 
//          "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \r\n" + 
//          "PREFIX a: <" + ns + ">\r\n" + 
//          "SELECT * WHERE { \r\n"+ 
//            "?X1 rdf:type a:Man . " +
//            "?Key a:isCreditCardOf ?X1 . " +
//            "?Key rdf:type a:Gold . " +
//            "?X1 a:livesIn ?X2 . " +
//            "?X2 rdf:type a:Region ." +
//            "}";
      
//      String query =
//        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \r\n" + 
//        "PREFIX a: <" + ns + ">\r\n" + 
//        "SELECT * WHERE { \r\n"+ 
//        "?r rdf:type a:Military-Person . "+
//        "?i a:hasRole ?r . "+
//        "?r a:related ?x ." +
//        "}";
      
//      test.run( query, dir );
    }
}
