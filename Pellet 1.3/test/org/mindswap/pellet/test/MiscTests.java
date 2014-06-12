/*
 * Created on Oct 12, 2004
 */

package org.mindswap.pellet.test;

import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.jena.PelletInfGraph;
import org.mindswap.pellet.jena.PelletQueryExecution;
import org.mindswap.pellet.jena.PelletReasoner;
import org.mindswap.pellet.jena.PelletReasonerFactory;
import org.mindswap.pellet.rete.Interpreter;
import org.mindswap.pellet.utils.ATermUtils;
import org.semanticweb.owl.io.owl_rdf.SWRLParser;
import org.semanticweb.owl.model.OWLException;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.helper.OntologyHelper;

import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.IntersectionClass;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.UnionClass;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

public class MiscTests extends PelletTestCase {
    public static String base = "file:" + PelletTestSuite.base + "misc/";
    
    public static TestSuite suite() {
        TestSuite s = new TestSuite( "MiscTests" );
        s.addTestSuite( MiscTests.class );

        return s;
    }
    
    public void testReflexive() {
        String ns = "http://www.example.org/test#";
        String foaf = "http://xmlns.com/foaf/0.1/";

        OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );
        model.read( base + "reflexive.owl" );

        ObjectProperty knows = model.getObjectProperty( foaf + "knows" );
        
        Individual[] people = new Individual[5];
        for( int i = 0; i < people.length; i++ ) {
            people[i] = model.getIndividual( ns + "P" + (i + 1) );
            
            assertTrue( people[i].hasProperty( knows, people[i] ) );
            
            assertIteratorValues( 
                this, 
                people[i].listPropertyValues(knows), 
                new Resource[] { people[i] } );
        }
        
        Query query = QueryFactory.create(
            "PREFIX foaf: <http://xmlns.com/foaf/0.1/>" +
            "SELECT * \n" +
            "WHERE { ?p foaf:knows ?q } \n"
        );
        
        List answers = createBindings( new String[] { "p", "q"},
        	new Resource[][] { 
            	{people[0], people[0]}, 
            	{people[1], people[1]}, 
            	{people[2], people[2]}, 
            	{people[3], people[3]}, 
            	{people[4], people[4]}, 
            }) ;
        
        ResultSet results1 = new PelletQueryExecution( query, model ).execSelect();
        testResultSet( results1, answers );
        
        ResultSet results2 = QueryExecutionFactory.create( query, model ).execSelect();
        testResultSet( results2, answers );
    }

    public void testFoodQuery() {
        String ns = "http://www.w3.org/2001/sw/WebOnt/guide-src/food";
        
        // create an empty ontology model using Pellet spec
        OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );        
            
        // read the file
        model.read( ns + ".owl" );    

        // getOntClass is not used because of the same reason mentioned above
        // (i.e. avoid unnecessary classifications)
        Resource RedMeatCourse = model.getResource( ns + "#RedMeatCourse" ); 
        Resource PastaWithLightCreamCourse = model.getResource( ns + "#PastaWithLightCreamCourse" ); 
        
        // create two individuals Lunch and dinner that are instances of  
        // PastaWithLightCreamCourse and RedMeatCourse, respectively
        Individual MyLunch = model.createIndividual( ns + "#MyLunch", PastaWithLightCreamCourse);
        Individual MyDinner = model.createIndividual( ns + "#MyDinner", RedMeatCourse);
                
        Individual White = model.getIndividual( ns + "#White" );
        Individual Red = model.getIndividual( ns + "#Red" );
        
        String queryBegin = 
            "PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\r\n" + 
            "PREFIX food: <http://www.w3.org/2001/sw/WebOnt/guide-src/food#>\r\n" + 
            "PREFIX wine: <http://www.w3.org/2001/sw/WebOnt/guide-src/wine#>\r\n" + 
            "\r\n" + 
            "SELECT ?Meal ?WineColor\r\n" + 
            "WHERE {\r\n";
        String queryEnd = "}";
        
        // create a query that asks for the color of the wine that
        // would go with each meal course
        String queryStr1 =
            queryBegin + 
            "   ?Meal rdf:type food:MealCourse .\r\n" + 
            "   ?Meal food:hasDrink ?Wine .\r\n" + 
            "   ?Wine wine:hasColor ?WineColor" +
            queryEnd;

        // same query as above but uses a bnode instead of a variable        
        String queryStr2 =
            queryBegin + 
            "   ?Meal rdf:type food:MealCourse .\r\n" + 
            "   ?Meal food:hasDrink _:Wine .\r\n" + 
            "   _:Wine wine:hasColor ?WineColor" +
            queryEnd;

        Query query1 = QueryFactory.create( queryStr1 );
        Query query2 = QueryFactory.create( queryStr2 );
        
        ResultSet results1 = new PelletQueryExecution( query1, model ).execSelect();
        assertTrue( !results1.hasNext() );
                
        ResultSet results2 = new PelletQueryExecution( query2, model ).execSelect();
        testResultSet( results2, 
            createBindings( new String[] { "Meal", "WineColor"},
                            new Resource[][] { {MyLunch, White}, {MyDinner, Red} }) );
        
    }
    
    
    public void testDatatypeCardinality() {
        String ns = "http://www.example.org/test#";

        OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );
        
        OntClass C1 = model.createClass( ns + "C1" );
        OntClass C2 = model.createClass( ns + "C2" );
        
        DatatypeProperty p = model.createDatatypeProperty( ns + "p" );
        p.addRange( XSD.xboolean );    
        
        C1.addSuperClass( model.createMinCardinalityRestriction( null, p, 2 ) );
        C2.addSuperClass( model.createMinCardinalityRestriction( null, p, 3 ) );
        
        model.prepare();
            
        ATermUtils.assertTrue( ((PelletInfGraph)model.getGraph()).getKB().isConsistent() );
    
        ATermUtils.assertTrue( !model.contains( C1, RDFS.subClassOf, OWL.Nothing ) );
        ATermUtils.assertTrue( model.contains( C2, RDFS.subClassOf, OWL.Nothing ) );
    }
    
    public void testIFDP1() {
        String ns = "http://www.example.org/test#";

        OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );

        Individual a = model.createIndividual( ns + "a", OWL.Thing );
        Individual b = model.createIndividual( ns + "b", OWL.Thing );
        Individual c = model.createIndividual( ns + "c", OWL.Thing );

        ObjectProperty op = model.createObjectProperty( ns + "op" );
        DatatypeProperty dp = model.createDatatypeProperty( ns + "dp" );
        dp.convertToInverseFunctionalProperty();
        
        a.addProperty( op, c );

        Literal one = model.createTypedLiteral(new Integer(1));
        a.addProperty( dp, one );
        b.addProperty( dp, one );

        model.prepare();

        assertTrue( a.isSameAs( b ) );        
        assertIteratorValues( this, a.listSameAs(), new Resource[] { a, b } );

        assertTrue( b.hasProperty( op, c ) );
        assertIteratorValues( this, b.listPropertyValues( op ), new Resource[] { c } );
    }
    
    public void testIFDP2() {
        String ns = "http://www.example.org/test#";

        OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );

        DatatypeProperty p = model.createDatatypeProperty( ns + "p" );
        p.convertToInverseFunctionalProperty();
        p.addRange( XSD.xboolean );    
        
        OntClass C = model.createClass( ns + "C" );
        C.addSuperClass( model.createCardinalityRestriction( null, p, 1 ) );

        OntClass D = model.createClass( ns + "D" );
        OntClass E = model.createClass( ns + "E" );
        D.addDisjointWith( E );
        
        Individual i1 = model.createIndividual( ns + "i1", C );
        i1.addRDFType( D );
        Individual i2 = model.createIndividual( ns + "i2", C );
        i2.addRDFType( D );
        Individual i3 = model.createIndividual( ns + "i3", C );
        i3.addRDFType( E );

        model.prepare();

        assertTrue( i1.isSameAs( i2 ) );        
        assertIteratorValues( this, i1.listSameAs(), new Resource[] { i1, i2 } );

        assertTrue( !i1.isSameAs( i3 ) );        

        assertTrue( !i1.listProperties( p ).hasNext() );
        assertTrue( !i2.listProperties( p ).hasNext() );
        assertTrue( !i3.listProperties( p ).hasNext() );
    }
    
    public void testIFDP3() {
        String ns = "http://www.example.org/test#";

        OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );

        DatatypeProperty dp = model.createDatatypeProperty( ns + "dp" );
        dp.addRange(XSD.nonNegativeInteger);
        dp.convertToInverseFunctionalProperty();

        OntClass C = model.createClass( ns + "C" );
        C.addSuperClass( model.createMinCardinalityRestriction( null, dp, 1 ) );

        Individual a = model.createIndividual( ns + "a", C );
        Individual b = model.createIndividual( ns + "b", C );
        Individual c = model.createIndividual( ns + "c", C );
        
        Literal zero = model.createTypedLiteral(new Integer(0));
        a.addProperty( dp, zero );
        
        b.addRDFType( model.createAllValuesFromRestriction(null, dp, XSD.nonPositiveInteger) );

        Literal one = model.createTypedLiteral(new Integer(1));
        c.addProperty( dp, one );

        model.prepare();

        assertTrue( a.isSameAs( b ) );
        assertTrue( b.isSameAs( a ) );        
        assertIteratorValues( this, a.listSameAs(), new Resource[] { a, b } );
        assertIteratorValues( this, b.listSameAs(), new Resource[] { a, b } );

        assertTrue( !c.isSameAs( a ) );        
        assertTrue( !c.isSameAs( b ) );        
    }
    
    public void testDuplicateLiterals() {
        String ns = "http://www.example.org/test#";

        OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );

        DatatypeProperty dp = model.createDatatypeProperty( ns + "dp" );

        OntClass C = model.createClass( ns + "C" );
        Individual a = model.createIndividual( ns + "a", C );
        
        Literal one = model.createTypedLiteral("1", TypeMapper.getInstance().getTypeByName(XSD.positiveInteger.getURI()));
        a.addProperty( dp, one );

        model.prepare();

        assertIteratorValues( this, a.listPropertyValues(dp), new Literal[] { one } );
    }
    
    public void test3Sat() {
        String ns = "http://www.example.org/test#";

        OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );

        model.read( base + "3Sat.owl" );
        
        String solution = "101";
        int n = solution.length();
        
        Individual T = model.getIndividual( ns + "T" );
        Individual F = model.getIndividual( ns + "F" );
            
        model.prepare();
            
        ATermUtils.assertTrue( ((PelletInfGraph)model.getGraph()).getKB().isConsistent() );
    
        Individual[] positives = new Individual[ n + 1 ]; 
        Individual[] negatives = new Individual[ n + 1];
        
        positives[ 0 ] = T;
        negatives[ 0 ] = F;
        
        for( int i = 1; i <= n; i++ ) {
            boolean t = solution.charAt( i - 1 ) == '1' ;

            if( t ) {
                positives[i] = model.getIndividual( ns + "plus" + i );
                negatives[i] = model.getIndividual( ns + "minus" + i );
            }
            else {
                positives[i] = model.getIndividual( ns + "minus" + i );
                negatives[i] = model.getIndividual( ns + "plus" + i );
            }
            
            TestCase.assertTrue( T +" = " + positives[i], T.isSameAs( positives[i] ) );
            TestCase.assertTrue( F +" = " + negatives[i], F.isSameAs( negatives[i] ) );
        }
        
//        System.out.println(
//            ((org.mindswap.pellet.Individual)((PelletInfGraph)model.getGraph()).getKB().getABox().pseudoModel.
//            getIndividual(ATermUtils.makeTermAppl(ns+"T")).getSame()).getTypes(Node.NOM));
//        
//        System.out.println(
//            ((org.mindswap.pellet.Individual)((PelletInfGraph)model.getGraph()).getKB().getABox().pseudoModel.
//            getIndividual(ATermUtils.makeTermAppl(ns+"F")).getSame()).getTypes(Node.NOM));

        assertIteratorValues( new MiscTests(), T.listSameAs(), positives );
        assertIteratorValues( new MiscTests(), F.listSameAs(), negatives );                        
    }

    public void testPropertyRestrictionsInSuperclasses() {
        String ns = "urn:test:";
        OntModelSpec spec = new OntModelSpec( OntModelSpec.OWL_DL_MEM );
        spec.setReasoner( new PelletReasoner() );

        OntModel model = ModelFactory.createOntologyModel( spec, null );

        OntClass X = model.createClass( ns + "X" );
        ObjectProperty hasX = model.createObjectProperty( ns + "hasX" );
        OntClass AllX = model.createAllValuesFromRestriction( null, hasX, X );
        OntClass Y = model.createIntersectionClass( ns + "Y", model.createList( new RDFNode[] { X,
                AllX } ) );

        assertTrue( "AllX is not a superclass of Y", Y.hasSuperClass( AllX ) );
    }

    public void testMaxCardinality() {
        KnowledgeBase kb = new KnowledgeBase();

        kb.addObjectProperty( term( "p" ) );
        kb.addObjectProperty( term( "q" ) );
        kb.addFunctionalProperty( term( "q" ) );

        kb.addClass( term( "C" ) );
        kb.addSubClass( term( "C" ), ATermUtils.makeMax( term( "p" ), 2 ) );

        kb.addClass( term( "D1" ) );
        kb.addClass( term( "D2" ) );
        kb.addClass( term( "D3" ) );
        kb.addClass( term( "D4" ) );
        kb.addClass( term( "E1" ) );
        kb.addClass( term( "E2" ) );
        kb.addClass( term( "E3" ) );
        kb.addClass( term( "E4" ) );
        kb.addSubClass( term( "D1" ), ATermUtils.makeSomeValues( term( "q" ), term( "E1" ) ) );
        kb.addSubClass( term( "D2" ), ATermUtils.makeSomeValues( term( "q" ), term( "E2" ) ) );
        kb.addSubClass( term( "D3" ), ATermUtils.makeSomeValues( term( "q" ), term( "E3" ) ) );
        kb.addSubClass( term( "D4" ), ATermUtils.makeSomeValues( term( "q" ), term( "E4" ) ) );

        kb.addIndividual( term( "x" ) );
        kb.addType( term( "x" ), term( "C" ) );
        kb.addIndividual( term( "x1" ) );
        kb.addType( term( "x1" ), term( "D1" ) );
        kb.addIndividual( term( "x2" ) );
        kb.addType( term( "x2" ), term( "D2" ) );
        kb.addIndividual( term( "x3" ) );
        kb.addType( term( "x3" ), term( "D3" ) );
        kb.addIndividual( term( "x4" ) );
        kb.addType( term( "x4" ), term( "D4" ) );

        kb.addPropertyValue( term( "p" ), term( "x" ), term( "x1" ) );
        kb.addPropertyValue( term( "p" ), term( "x" ), term( "x2" ) );
        kb.addPropertyValue( term( "p" ), term( "x" ), term( "x3" ) );
        kb.addPropertyValue( term( "p" ), term( "x" ), term( "x4" ) );

        kb.addDisjointClass( term( "E1" ), term( "E2" ) );
        kb.addDisjointClass( term( "E1" ), term( "E4" ) );
        kb.addDisjointClass( term( "E2" ), term( "E3" ) );

        assertTrue( kb.isConsistent() );

        assertTrue( kb.isSameAs( term( "x1" ), term( "x3" ) ) );
        assertTrue( kb.isSameAs( term( "x3" ), term( "x1" ) ) );
        assertTrue( kb.isSameAs( term( "x2" ), term( "x4" ) ) );

        assertTrue( kb.getSames( term( "x1" ) ).contains( term( "x3" ) ) );
        assertTrue( kb.getSames( term( "x2" ) ).contains( term( "x4" ) ) );
    }

    public void testAnonTypes() {
        String ns = "urn:test:";
        OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );
        OntClass c = model.createClass( ns + "C" );

        Individual anon = model.createIndividual( c );
        Individual x = model.createIndividual( ns + "x", c );

        assertIteratorValues( this, model.listObjectsOfProperty( x, null ),
            new Resource[] { OWL.Thing, c } );

        assertIteratorValues( this, model.listObjectsOfProperty( anon, null ),
            new Resource[] { OWL.Thing, c } );
    }

    public void testAnonClasses() {
        OntModel ontmodel = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );

        String nc = "urn:test:";

        OntClass class1 = ontmodel.createClass( nc + "C1" );
        OntClass class2 = ontmodel.createClass( nc + "C2" );

        Individual[] inds = new Individual[6];
        for(int j = 0; j < 6; j++) {
            inds[j] = ontmodel.createIndividual( nc + "Ind" + j, OWL.Thing );
        }

        inds[0].addRDFType( class1 );
        inds[1].addRDFType( class1 );
        inds[2].addRDFType( class1 );
        inds[3].addRDFType( class1 );

        inds[2].addRDFType( class2 );
        inds[3].addRDFType( class2 );
        inds[4].addRDFType( class2 );
        inds[5].addRDFType( class2 );

        assertIteratorValues( this, class1.listInstances(), new Resource[] { inds[0],
                inds[1], inds[2], inds[3] } );

        assertIteratorValues( this, class2.listInstances(), new Resource[] { inds[2],
                inds[3], inds[4], inds[5] } );

        RDFList list = ontmodel.createList( new RDFNode[] { class1, class2 } );

        IntersectionClass class3 = ontmodel.createIntersectionClass( null, list );

        UnionClass class4 = ontmodel.createUnionClass( null, list );

        assertIteratorValues( this, class3.listInstances(), new Resource[] { inds[2],
                inds[3] } );

        assertIteratorValues( this, class4.listInstances(), new Resource[] { inds[0],
                inds[1], inds[2], inds[3], inds[4], inds[5] } );

    }

    public void testDelete() {
        String ns = "urn:test:";

        OntModel model = ModelFactory.createOntologyModel();

        OntClass A = model.createClass( ns + "A" );
        ObjectProperty P = model.createObjectProperty( ns + "P" );
        P.addDomain( A );
        Individual x = model.createIndividual( ns + "x", OWL.Thing );
        Individual y = model.createIndividual( ns + "y", OWL.Thing );
        x.addProperty( P, y );

        assertTrue( x.hasRDFType( A ) );

        x.removeRDFType( A );

        assertTrue( x.hasRDFType( A ) );
    }

    public void testDeclaredProperties() {
        String ns = "urn:test:";

        Reasoner r = PelletReasonerFactory.theInstance().create();
        //ReasonerRegistry.getOWLMicroReasoner();

        OntModelSpec spec = new OntModelSpec( OntModelSpec.OWL_DL_MEM );
        spec.setReasoner( r );
        OntModel model = ModelFactory.createOntologyModel( spec, null );

        OntClass A = model.createClass( ns + "A" );
        OntClass B = model.createClass( ns + "B" );
        ObjectProperty P = model.createObjectProperty( ns + "P" );
        P.addDomain( model.createUnionClass( null, model.createList( new RDFNode[] { A, B } ) ) );

        OntClass oc = model.getOntClass( ns + "B" );

        assertIteratorValues( this, oc.listDeclaredProperties(), new Resource[] { P } );
    }

    public void testSameAs1() {
        String ns = "urn:test:";

        OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );

        Individual a = model.createIndividual( ns + "a", OWL.Thing );
        Individual b = model.createIndividual( ns + "b", OWL.Thing );
        Individual c = model.createIndividual( ns + "c", OWL.Thing );

        ObjectProperty p = model.createObjectProperty( ns + "p" );
        ObjectProperty q = model.createObjectProperty( ns + "q" );

        a.addProperty( p, c );

        b.addProperty( p, b );
        c.addProperty( q, a );

        c.addSameAs( b );

        a.addProperty( q, c );

        model.prepare();

        assertIteratorValues( this, a.listPropertyValues( p ), new Resource[] { b, c } );

        assertIteratorValues( this, a.listPropertyValues( q ), new Resource[] { b, c } );

        assertIteratorValues( this, b.listPropertyValues( p ), new Resource[] { b, c } );

        assertIteratorValues( this, b.listPropertyValues( q ), new Resource[] { a } );

        assertIteratorValues( this, c.listPropertyValues( p ), new Resource[] { b, c } );

        assertIteratorValues( this, c.listPropertyValues( q ), new Resource[] { a } );

    }
    
    public void testSameAs2() {
        OntModelSpec ontModelSpec = new OntModelSpec(OntModelSpec.OWL_DL_MEM_RULE_INF);
        ontModelSpec.setReasoner(new PelletReasoner());
        OntModel model = ModelFactory.createOntologyModel(ontModelSpec);
        Individual i1 = model.createIndividual("http://test#i1", OWL.Thing);
        Individual i2 = model.createIndividual("http://test#i2", OWL.Thing);
        Property prop = model.createProperty("http://test#prop");
		i1.addProperty(prop, "test");
        i1.addSameAs(i2);
         
        // confirm that sameAs was created
        assertTrue(i1.isSameAs(i2));
        
        // confirm that symmetric sameAs inferred
        assertTrue(i2.isSameAs(i1));
        
        // confirm that the property is there    
        assertTrue( i1.hasProperty(prop, "test") );

        // confirm that the property is there when querying with a predicate
        assertIteratorContains( this, i1.listProperties(), model.createStatement(i1, prop, "test"));

        // confirm that the property is copied over when querying with a predicate
        assertTrue(i2.hasProperty(prop, "test"));
        
        // confirm that the property is copied over when querying with a predicate
        assertIteratorContains( this, i2.listProperties(), model.createStatement(i2, prop, "test"));        
	}    

    public void testSameAs3() {
        OntModelSpec ontModelSpec = new OntModelSpec(OntModelSpec.OWL_DL_MEM_RULE_INF);
        ontModelSpec.setReasoner(new PelletReasoner());
        OntModel model = ModelFactory.createOntologyModel(ontModelSpec);
        Individual i1 = model.createIndividual("http://test#i1", OWL.Thing);
        Individual i2 = model.createIndividual("http://test#i2", OWL.Thing);
        OntClass c = model.createEnumeratedClass("http://test#C",model.createList(new RDFNode[]{i1,i2}));
//        ABox.DEBUG = true;
        Individual i3 = model.createIndividual("http://test#i3", c);
        
        assertTrue(!i1.isSameAs(i2));
        assertTrue(!i1.isSameAs(i3));
        assertIteratorValues( this, i1.listSameAs(), new Resource[] { i1 } );
        
        assertTrue(!i2.isSameAs(i1));
        assertTrue(!i2.isSameAs(i3));
        assertIteratorValues( this, i2.listSameAs(), new Resource[] { i2 } );
                
        assertTrue(!i3.isSameAs(i1));
        
        assertTrue(!i3.isSameAs(i2));
        assertIteratorValues( this, i3.listSameAs(), new Resource[] { i3 } ); 
    } 
    
    public void testSudaku() {
        String ns = "http://sudoku.owl#";

        OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );
        model.read( base + "sudaku.owl" );
         
        OntClass[][] C = new OntClass[4][4];
        Individual[][] V = new Individual[4][4];
        Individual[] N = new Individual[4];
        for( int i = 1; i < 4; i++ ) {
            N[i] = model.getIndividual( ns + i );
            for( int j = 1; j < 4; j++ ) {
                V[i][j] = model.getIndividual( ns + "V" + i + j );
                C[i][j] = model.getOntClass( ns + "C" + i + j );
            }            
        }
        
        V[2][1].setSameAs( N[2] );
        V[1][2].setSameAs( N[3] );
        
//      | ?1 |  3 | ?2 |
//      |  2 | ?1 | ?3 |       
//      | ?3 | ?2 | ?1 |       

        Individual[][] eq = new Individual[][] {
            { V[1][1], V[2][2], V[3][3], N[1] },
            { V[1][3], V[2][1], V[3][2], N[2] },
            { V[1][2], V[2][3], V[3][1], N[3] }
        };
        for( int k = 0; k < 3; k++ ) {
            for( int i = 0; i < 4; i++ ) {
                Individual ind = eq[k][i];
                for( int j = 0; j < 4; j++ ) {
//                    System.out.println( ind + " = " + eq[k][j] );
                    assertTrue( ind.isSameAs( eq[k][j] ) );
                }
                assertIteratorValues( this, ind.listSameAs(), eq[k] );            
            }
        }
    } 
    
    public void testFuncProp() {
        String ns = "urn:test:";

        OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );

        Individual a = model.createIndividual( ns + "a", OWL.Thing );
        Individual b = model.createIndividual( ns + "b", OWL.Thing );
        Individual c = model.createIndividual( ns + "c", OWL.Thing );
        Individual d = model.createIndividual( ns + "d", OWL.Thing );

        ObjectProperty p = model.createObjectProperty( ns + "p" );
        a.addProperty( p, b );

        ObjectProperty q = model.createObjectProperty( ns + "q", true );
        a.addProperty( q, b );
        a.addProperty( q, d );

        c.addSameAs( b );

        assertIteratorValues( this, a.listPropertyValues( p ), new Resource[] { b, c, d } );
        assertIteratorValues( this, b.listSameAs(), new Resource[] { b, c, d } );
        assertIteratorValues( this, c.listSameAs(), new Resource[] { b, c, d } );
        assertIteratorValues( this, d.listSameAs(), new Resource[] { b, c, d } );

        assertTrue( b.isSameAs( c ) );
        assertTrue( c.isSameAs( b ) );
        assertTrue( b.isSameAs( d ) );
        assertTrue( d.isSameAs( b ) );
        assertTrue( d.isSameAs( c ) );
        assertTrue( c.isSameAs( d ) );
    }

    public void testHasValueReasoning() {
        String ns = "urn:test:";
        OntModelSpec spec = new OntModelSpec( OntModelSpec.OWL_DL_MEM );
        spec.setReasoner( new PelletReasoner() );
        OntModel model = ModelFactory.createOntologyModel( spec, null );
        OntClass HomeOwner = model.createClass( ns + "HomeOwner" );
        Individual bob = model.createIndividual( ns + "bob", HomeOwner );
        ObjectProperty hasNeighbor = model.createObjectProperty( ns + "hasNeighbor" );
        OntClass NeighborOfBob = model.createClass( ns + "NeighborOfBob" );
        NeighborOfBob
            .addEquivalentClass( model.createHasValueRestriction( null, hasNeighbor, bob ) );
        Individual susan = model.createIndividual( ns + "susan", HomeOwner );
        susan.setPropertyValue( hasNeighbor, bob );
        //        model.write(System.out, "RDF/XML-ABBREV");

        assertTrue( "susan is not a NeighborOfBob", susan.hasRDFType( NeighborOfBob ) );
    }

    public void testInfiniteChain() {
        OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );
        model.read( base + "infiniteChain.owl" );
        
        assertTrue( !model.validate().isValid() );
        
        String ns = "http://www.example.org/test#";
        OntProperty prop = model.getOntProperty( ns + "ssn" );
        prop.removeRange(prop.getRange());
        
        assertTrue( model.validate().isValid() );
    }

    public void testInfiniteChainDP() {
        OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );
        model.read( base + "infiniteChainDP.owl" );
        
        assertTrue( !model.validate().isValid() );
        
//        String ns = "http://www.example.org/test#";
//        DatatypeProperty prop = model.getDatatypeProperty( ns + "ssn" );
//        prop.removeRange(prop.getRange());
//        
//        assertTrue( model.validate().isValid() );        
    }
    
    public void testFamily() {
        String ns = "http://www.example.org/test#";

        OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );
        model.read( base + "family.owl" );

        Individual Bob = model.getIndividual( ns + "Bob" );
        Individual Mom = model.getIndividual( ns + "Mom" );
        Individual Dad = model.getIndividual( ns + "Dad" );
        
        OntProperty hasParent = model.getObjectProperty( ns + "hasParent" );
        OntProperty hasFather = model.getObjectProperty( ns + "hasFather" );
        OntProperty hasMother = model.getObjectProperty( ns + "hasMother" );

//        assertTrue( model.validate().isValid() );
        
        assertIteratorValues( this, model.listObjectsOfProperty( Bob, hasParent ),
            new Resource[] { Mom, Dad } );
        
        assertIteratorValues( this, model.listObjectsOfProperty( hasFather ),
            new Object[] { Dad } );

        assertIteratorValues( this, model.listObjectsOfProperty( hasMother ),
            new Object[] { Mom } );

        assertIteratorValues( this, model.listStatements( null, hasParent, (Resource) null ),
            new Statement[] { 
                ResourceFactory.createStatement(Bob, hasParent, Mom), 
                ResourceFactory.createStatement(Bob, hasParent, Dad)} );

        assertIteratorValues( this, model.listStatements( Bob, null, Dad ),
            new Statement[] { 
                ResourceFactory.createStatement(Bob, hasParent, Dad), 
                ResourceFactory.createStatement(Bob, hasFather, Dad)} );

        assertIteratorValues( this, model.listObjectsOfProperty( Bob, hasFather ),
            new Resource[] { Dad } );

        assertIteratorValues( this, model.listObjectsOfProperty( Bob, hasMother ),
            new Resource[] { Mom } );
    }
    
    public void testTeams() {
        String ns = "http://owl.man.ac.uk/2005/sssw/teams#";
        
        OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );
        

        model.read( base + "teams.owl" );

        Individual t1 = model.getIndividual( ns + "OntologyFC" );
        
        OntClass Male = model.getOntClass( ns + "Male" );
        OntClass Female = model.getOntClass( ns + "Female" );
        Individual Sam = model.getIndividual( ns + "Sam" );
        Individual Chris = model.getIndividual( ns + "Chris" );

        OntClass Team = model.getOntClass( ns + "Team" );
        OntClass MixedTeam = model.getOntClass( ns + "MixedTeam" );
        OntClass NonSingletonTeam = model.getOntClass( ns + "NonSingletonTeam" );
        
        model.prepare();
        
        assertTrue( Sam.isDifferentFrom( Chris ) );
        assertTrue( Chris.isDifferentFrom( Sam ) );

        assertTrue( MixedTeam.hasSuperClass( Team ) );        
        assertIteratorValues( this, MixedTeam.listSuperClasses(), 
            new Resource[] { Team, NonSingletonTeam, OWL.Thing } );
        
        assertTrue( NonSingletonTeam.hasSubClass( MixedTeam ) );
        assertIteratorValues( this, NonSingletonTeam.listSubClasses(), 
            new Resource[] { MixedTeam, OWL.Nothing } );
        
        assertTrue( t1.hasRDFType( MixedTeam ) );
        assertTrue( t1.hasRDFType( MixedTeam, true ) );
        assertIteratorValues( this, t1.listRDFTypes( false ), 
            new Resource[] { Team, NonSingletonTeam, MixedTeam, OWL.Thing } );

        Male.removeDisjointWith( Female );
        Female.removeDisjointWith( Male );
        Sam.removeDifferentFrom( Chris );
        Chris.removeDifferentFrom( Sam );

        assertTrue( !Sam.isDifferentFrom( Chris ) );
        assertTrue( !Chris.isDifferentFrom( Sam ) );               
        
        assertTrue( MixedTeam.hasSuperClass( Team ) );        
        assertIteratorValues( this, MixedTeam.listSuperClasses(), 
            new Resource[] { Team, OWL.Thing } );
        
        assertTrue( !NonSingletonTeam.hasSuperClass( MixedTeam ) );
        assertIteratorValues( this, NonSingletonTeam.listSuperClasses(), 
            new Resource[] { Team, OWL.Thing } );
        
        assertTrue( t1.hasRDFType( MixedTeam ) );
        assertTrue( t1.hasRDFType( MixedTeam, true ) );
        assertIteratorValues( this, t1.listRDFTypes( false ), 
            new Resource[] { Team, MixedTeam, OWL.Thing } );
    }
    
    public void testPropertyAssertions2() {
        String ns = "urn:test:";

        OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );
        OntClass Person = model.createClass( ns + "Person" );
        OntProperty hasFather = model.createObjectProperty( ns + "hasFather" );
        OntProperty hasBioFather = model.createObjectProperty( ns + "hasBioFather", true );
        hasBioFather.addSuperProperty( hasFather );
        Person.addSuperClass( model.createMinCardinalityRestriction( null, hasBioFather, 1 ) );

        Individual Bob = model.createIndividual( ns + "Bob", Person );
        Individual Dad = model.createIndividual( ns + "Dad", Person );
        Bob.addProperty( hasBioFather, Dad );
        Bob.addRDFType( model.createCardinalityRestriction( null, hasFather, 1 ) );

        model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC, model );

        assertIteratorValues( this, model.listObjectsOfProperty( Bob, hasFather ),
            new Resource[] { Dad } );

        assertIteratorValues( this, model.listObjectsOfProperty( Bob, hasBioFather ),
            new Resource[] { Dad } );
    }

    public void testTransitive1() {
        OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );
        model.read( base + "agencies.owl" );

        model.prepare();

        String ns = "http://www.owl-ontologies.com/unnamed.owl#";
        Individual Forest_Service = model.getIndividual( ns + "Forest_Service" );
        ObjectProperty comprises = model.getObjectProperty( ns + "comprises" );
        Individual Executive = model.getIndividual( ns + "Executive" );
        Individual USDA = model.getIndividual( ns + "USDA" );

        assertTrue( "Forest_Service, comprises, Executive", model.contains( Forest_Service,
            comprises, Executive ) );

        assertIteratorValues( this, model
            .listObjectsOfProperty( Forest_Service, comprises ),
            new Resource[] { USDA, Executive } );

        assertIteratorValues( this,
            model.listSubjectsWithProperty( comprises, Executive ), new Resource[] {
                    model.getIndividual( ns + "USDA" ),
                    model.getIndividual( ns + "DOE" ),
                    model.getIndividual( ns + "DHS" ),
                    model.getIndividual( ns + "HHS" ),
                    model.getIndividual( ns + "HUD" ),
                    model.getIndividual( ns + "DOC" ),
                    model.getIndividual( ns + "DOD" ),
                    model.getIndividual( ns + "DOI" ),
                    model.getIndividual( ns + "Research__Economics___Education" ),
                    model.getIndividual( ns + "Forest_Service" ),
                    model.getIndividual( ns + "Rural_Development" ),
                    model.getIndividual( ns + "Natural_Resources_Conservation_Service" ),
                    model.getIndividual( ns + "Economic_Research_Service" ),
                    model.getIndividual( ns + "Farm_Service_Agency" ),
                    model.getIndividual( ns
                        + "Cooperative_State_Research__Education__and_Extension_Service" ),
                    model.getIndividual( ns + "Animal___Plant_Health_Inspection_Service" ),
                    model.getIndividual( ns + "Agricultural_Research_Service" ),
                    model.getIndividual( ns + "National_Agricultural_Library" ), } );
    }

    public void testTransitive2() {
        OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );
        model.read( base + "cyclic_transitive.owl" );

        model.prepare();

        String ns = "http://www.example.org/test#";

        OntClass Probe = model.getOntClass( ns + "Probe" );
        Individual Instance1 = model.getIndividual( ns + "Instance1" );
        Individual Instance2 = model.getIndividual( ns + "Instance2" );
        Individual Instance3 = model.getIndividual( ns + "Instance3" );

        assertIteratorValues( this, Probe.listInstances(),
            new Resource[] { Instance1, Instance2, Instance3 } );
    }
    
    public void testTransitiveSubProperty() {
        String ns = "urn:test:";

        OntModel model = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM );

        ObjectProperty knows = model.createObjectProperty( ns + "knows" );

        ObjectProperty hasRelative = model.createObjectProperty( ns + "hasRelative" );
        // a person knows all his/her relatives
        hasRelative.addSuperProperty( knows );
        // being a relative is transitive (but knowing someone is not
        // transitive)
        hasRelative.addRDFType( OWL.TransitiveProperty );

        ObjectProperty hasParent = model.createObjectProperty( ns + "hasParent" );
        // a parent is also a relative
        hasParent.addSuperProperty( hasRelative );

        OntClass cls = model.createClass( ns + "cls" );
        Individual a = cls.createIndividual( ns + "a" );
        Individual b = cls.createIndividual( ns + "b" );
        Individual c = cls.createIndividual( ns + "c" );
        Individual d = cls.createIndividual( ns + "d" );
        Individual e = cls.createIndividual( ns + "e" );
        Individual f = cls.createIndividual( ns + "f" );
        Individual g = cls.createIndividual( ns + "g" );

        OntModelSpec spec = new OntModelSpec( OntModelSpec.OWL_DL_MEM );
        //	    spec.setReasoner( ReasonerRegistry.getDIGReasoner() );
        spec.setReasoner( PelletReasonerFactory.theInstance().create() );
        model = ModelFactory.createOntologyModel( spec, model );

        model.add( a, hasParent, b ); // (1)
        model.add( b, hasParent, c ); // (2)

        model.add( a, knows, d ); // (3)
        model.add( d, knows, e ); // (4)

        model.add( b, knows, e ); // (5)

        model.add( c, hasRelative, f ); // (6)

        model.add( d, hasRelative, g ); // (6)

        // (1) implies a hasRelative b, a knows b
        assertTrue( model.contains( a, hasRelative, b ) );
        assertTrue( model.contains( a, knows, b ) );

        // (2) implies b hasRelative c, b knows c
        assertTrue( model.contains( b, hasRelative, c ) );
        assertTrue( model.contains( b, knows, c ) );

        // (1) and (2) implies implies a hasRelative c, a knows c
        assertTrue( model.contains( a, hasRelative, c ) );
        assertTrue( model.contains( a, knows, c ) );

        // (2) and (6) implies b hasRelative f, b knows f
        assertTrue( model.contains( b, hasRelative, f ) );
        assertTrue( model.contains( b, knows, f ) );

        // (1), (2) and (6) implies implies a hasRelative f, a knows f
        assertTrue( model.contains( a, hasRelative, f ) );
        assertTrue( model.contains( a, knows, f ) );

        // Neither (1) and (5) nor (3) and (4) implies a hasRelative e
        assertTrue( !model.contains( a, hasRelative, e ) );

        // Neither (1) and (5) nor (3) and (4) implies a knows e
        assertTrue( !model.contains( a, knows, e ) );

        assertTrue( !model.contains( a, knows, g ) );

        assertTrue( !model.contains( a, hasRelative, g ) );

        // checking get functions
        assertIteratorValues( this, model.listObjectsOfProperty( a, hasRelative ),
            new Resource[] { b, c, f } );

        assertIteratorValues( this, model.listObjectsOfProperty( a, knows ),
            new Resource[] { b, c, d, f } );

        assertIteratorValues( this, model.listObjectsOfProperty( b, knows ),
            new Resource[] { c, e, f } );

        assertIteratorValues( this, model.listSubjectsWithProperty( knows, e ),
            new Resource[] { b, d } );

        assertIteratorValues( this, model.listSubjectsWithProperty( hasRelative, f ),
            new Resource[] { a, b, c } );
    }

    public void testNominals() {
        String ns = "http://www.example.org/test#";

        OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );

        model.read( base + "nominals.owl" );
        
        OntClass Color = model.getOntClass( ns + "Color" );
        Individual red = model.getIndividual( ns + "red" );

        OntClass PrimaryColors = model.getOntClass( ns + "PrimaryColors" );

        OntClass MyFavoriteColors = model.getOntClass( ns + "MyFavoriteColors" );

        OntClass HasFourPrimaryColors = model.getOntClass( ns + "HasFourPrimaryColors" );
        
        model.prepare();

        assertTrue( model.contains( red, RDF.type, MyFavoriteColors ) );

        assertTrue( model.contains( HasFourPrimaryColors, RDFS.subClassOf, OWL.Nothing ) );

        assertIteratorValues( this, Color.listSubClasses(), new Resource[] {
                PrimaryColors, MyFavoriteColors, HasFourPrimaryColors, OWL.Nothing } );
    }

    public void testDatatypeProperties() {
        String ns = "urn:test:";

        OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );
        OntClass Person = model.createClass( ns + "Person" );
        Individual john = model.createIndividual( ns + "JohnDoe", Person );
        DatatypeProperty email = model.createDatatypeProperty( ns + "email", false );

        john.addProperty( email, "john.doe@unknown.org" );
        john.addProperty( email, "jdoe@unknown.org" );

        assertTrue( model.validate().isValid() );
        
        assertIteratorValues( this, model.listSubjectsWithProperty( email, "john.doe@unknown.org" ), 
            new Resource[] { john } );
        
        assertTrue( model.contains( null, email, "john.doe@unknown.org" ) );
        
        assertTrue( !model.contains( null, email, john ) );
 
        DatatypeProperty name1 = model.createDatatypeProperty( ns + "name1", true );

        john.addProperty( name1, "Name", "en" );
        john.addProperty( name1, "Nom", "fr" );

        assertTrue( model.validate().isValid() );
        
        DatatypeProperty name2 = model.createDatatypeProperty( ns + "name2", true );

        john.addProperty( name2, "Name" );
        john.addProperty( name2, "Nom" );

        assertTrue( !model.validate().isValid() );
    }
    
    public void testDataPropCard1() {
        String ns = "urn:test:";

        OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );
        DatatypeProperty prop = model.createDatatypeProperty( ns + "prop" );
        OntClass C = model.createClass( ns + "C" );
        C.addSuperClass( model.createCardinalityRestriction( null, prop, 2 ) );
        Individual x = model.createIndividual( ns + "x", C );
        x.addProperty( prop, "literal");

        model.prepare();
        
        assertTrue( ((PelletInfGraph)model.getGraph()).isConsistent() );
    }
    
    public void testDataPropCard2() {
        String ns = "urn:test:";

        OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );
        DatatypeProperty prop = model.createDatatypeProperty( ns + "prop" );
        OntClass C = model.createClass( ns + "C" );
        C.addSuperClass( model.createCardinalityRestriction( null, prop, 2 ) );
        Individual x = model.createIndividual( ns + "x", C );
        x.addProperty( prop, "literal1");
        x.addProperty( prop, "literal2");
        x.addProperty( prop, "literal3");
        
        assertTrue( !model.validate().isValid() );
    }

    
    public void testSubDataPropCard() {
        String ns = "urn:test:";

        OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );
        DatatypeProperty prop = model.createDatatypeProperty( ns + "prop" );
        DatatypeProperty sub = model.createDatatypeProperty( ns + "sub" );
        sub.addSuperProperty( prop );
        
//        prop.addRange( XSD.decimal );
        
        OntClass C = model.createClass( ns + "C" );
        C.addSuperClass( model.createCardinalityRestriction( null, prop, 2 ) );
        Individual x = model.createIndividual( ns + "x", C );
        
        
        Literal val1 = model.createLiteral( "val1" ); 
        x.addProperty( prop, val1 );
        Literal val2 = model.createLiteral( "val2" );
        x.addProperty( sub, val2 );
        
        assertTrue( model.validate().isValid() );

        assertTrue( model.contains( x, prop, val1 ) );
        assertTrue( model.contains( x, prop, val2 ) );
        assertTrue( model.contains( x, sub, val2 ) );
        
        assertTrue( x.hasProperty( prop, val1 ) );
        assertTrue( x.hasProperty( prop, val2 ) );
        assertTrue( x.hasProperty( sub, val2 ) );

        assertIteratorValues( this, model.listObjectsOfProperty( x, prop ),
            new RDFNode[] { val1, val2 } );        
        assertIteratorValues( this, model.listObjectsOfProperty( x, sub ),
            new RDFNode[] { val2 } );                
        
        String queryStr = "SELECT * WHERE { ?x <" + prop +"> ?y }";
        Query query = QueryFactory.create( queryStr );

        ResultSet results1 = new PelletQueryExecution( query, model ).execSelect();
        testResultSet( results1, 
            createBindings( new String[] { "x", "y"},
            				new RDFNode[][] { {x, val1}, {x, val2} }) );
    }
    
    public void testUniqueNameAssumption() {
        String ns = "urn:test:";

        OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );

        OntClass Country = model.createClass( ns + "Country" );
        Individual USA = model.createIndividual( ns + "USA", Country );
        Individual UnitedStates = model.createIndividual( ns + "UnitedStates", Country );

        OntProperty livesIn = model.createObjectProperty( ns + "livesIn" );
        livesIn.convertToFunctionalProperty();

        OntClass Person = model.createClass( ns + "Person" );
        Individual JohnDoe = model.createIndividual( ns + "JohnDoe", Person );
        JohnDoe.addProperty( livesIn, USA );
        JohnDoe.addProperty( livesIn, UnitedStates );

        assertTrue( model.contains( JohnDoe, RDF.type, Person ) );
        assertTrue( model.contains( USA, OWL.sameAs, UnitedStates ) );
        assertIteratorValues( this, model.listObjectsOfProperty( JohnDoe, livesIn ),
            new Resource[] { USA, UnitedStates } );
    }

    public void testESG() {
        String ns = "http://www.csm.ornl.gov/~7lp/onto-library/esg1.1#";

        OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );
        model.getDocumentManager().setProcessImports( false );

        model.read( base + "ESG1.1.owl" );

        model.prepare();

        assertTrue( ((PelletInfGraph) model.getGraph()).getKB().isConsistent() );

        Individual jdl62 = model.getIndividual( ns + "JDL_00062" );
        Individual jdl63 = model.getIndividual( ns + "JDL_00063" );

        assertTrue( jdl62.isSameAs( jdl63 ) );
        assertTrue( jdl63.isSameAs( jdl62 ) );

        assertIteratorValues( 
            this, jdl62.listSameAs(), new Resource[] { jdl62, jdl63 } );

        assertIteratorValues( 
            this, jdl63.listSameAs(), new Resource[] { jdl62, jdl63 } );
        
        model.getDocumentManager().setProcessImports( true );
//        ((PelletInfGraph) model.getGraph()).getKB().timers.print();
    }

    public void testSHOIN() {
        KnowledgeBase kb = new KnowledgeBase();

        kb.addObjectProperty( term( "R1" ) );
        kb.addObjectProperty( term( "invR1" ) );
        kb.addObjectProperty( term( "R2" ) );
        kb.addObjectProperty( term( "invR2" ) );
        kb.addObjectProperty( term( "S1" ) );
        kb.addObjectProperty( term( "invS1" ) );
        kb.addObjectProperty( term( "S2" ) );
        kb.addObjectProperty( term( "invS2" ) );

        kb.addInverseProperty( term( "R1" ), term( "invR1" ) );
        kb.addInverseProperty( term( "R2" ), term( "invR2" ) );
        kb.addInverseProperty( term( "S1" ), term( "invS1" ) );
        kb.addInverseProperty( term( "S2" ), term( "invS2" ) );

        kb.addIndividual( term( "o1" ) );
        kb.addIndividual( term( "o2" ) );

        kb.addSubClass( value( term( "o1" ) ), and( max( term( "invR1" ), 2 ), all(
            term( "invR1" ), some( term( "S1" ), some( term( "invS2" ), some( term( "R2" ),
                value( term( "o2" ) ) ) ) ) ) ) );

        kb.addSubClass( value( term( "o2" ) ), and( max( term( "invR2" ), 2 ), all(
            term( "invR2" ), some( term( "S2" ), some( term( "invS1" ), some( term( "R1" ),
                value( term( "o1" ) ) ) ) ) ) ) );

        assertTrue( kb.isConsistent() );
        

        assertTrue( kb.isSatisfiable( and( value( term( "o1" ) ), some( term( "invR1" ), TOP ) ) ) );
    }

//    public void testEconn1() throws Exception {
//        String ns = "http://www.mindswap.org/2004/multipleOnt/FactoredOntologies/EasyTests/Easy2/people.owl#";
//        
//        OWLReasoner reasoner = new OWLReasoner();
//        reasoner.setEconnEnabled( true );
//        
//        reasoner.load( ns );
//		
//		assertTrue( reasoner.isConsistent() );
//        
//		assertTrue( !reasoner.isSatisfiable( ResourceFactory.createResource( ns + "Unsat1" ) ) );
//		assertTrue( !reasoner.isSatisfiable( ResourceFactory.createResource( ns + "Unsat2" ) ) );
//		assertTrue( !reasoner.isSatisfiable( ResourceFactory.createResource( ns + "Unsat3" ) ) );
//		assertTrue( !reasoner.isSatisfiable( ResourceFactory.createResource( ns + "Unsat4" ) ) );
//    }

    public void testDatapropertyRange() {
        OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );
        model.read( base + "datataype_range.owl" );
        
        model.prepare();

        Iterator i = model.listDatatypeProperties();
        while( i.hasNext() ) {
            DatatypeProperty p = (DatatypeProperty) i.next();
            Iterator j = p.listRange();
            while( j.hasNext() ) {
                Resource range = (Resource) j.next();
                assertTrue( TypeMapper.getInstance().getTypeByName( range.getURI() ) != null ) ;
            }             
        }
    }
    
    public void testDLSafeRulesRete() throws OWLException {
    	
    	URI ontURI = URI.create("http://www.mindswap.org/~kolovski/rules.owl");
    	
    	OWLOntology ont = OntologyHelper.getOntology( ontURI );
    	
    	HashSet importedOnts = new HashSet();    	
    	Set rules = new HashSet();    	
    	org.mindswap.pellet.owlapi.Reasoner reasoner = new org.mindswap.pellet.owlapi.Reasoner();    	
    	reasoner.setOntology( ont );    	    	
    	
    	try {
    		OWLOntology ontology = OntologyHelper.getOntology(ontURI);
    		URI uri = ontology.getPhysicalURI();
    		
    		if (!(ontology.equals(null))) {
    			SWRLParser parser = new SWRLParser();
    			// For imported ontologies
    			importedOnts = (HashSet) ontology.getIncludedOntologies();
    			// If there is no imported ontology in the rules file it assumes
    			// that rules and ontology are in the same file
    			if (importedOnts.isEmpty()) {
    				parser.setOntology(ontology);
    				rules = parser.parseRules(uri);
    				
    			} else {
    				Iterator it = importedOnts.iterator();
    				ontology = (OWLOntology) it.next();
    				parser.setOntology(ontology);
    				rules = parser.parseRules(uri);    				
    			}
    		}
    	} catch (Exception e) {
    		System.out.println(e.getMessage());
    		rules = null;
    	}  		
    	reasoner.getKB().setRules(rules);
    	
    	assertFalse( reasoner.isConsistent() );    
    }

    public void testRete() throws OWLException {
    	
    	URI ontURI = URI.create("http://www.mindswap.org/~kolovski/rules.owl");
    	
    	OWLOntology ont = OntologyHelper.getOntology( ontURI );
    	
    	HashSet importedOnts = new HashSet();    	
    	Set rules = new HashSet();    	
    	org.mindswap.pellet.owlapi.Reasoner reasoner = new org.mindswap.pellet.owlapi.Reasoner();    	
    	reasoner.setOntology( ont );    	    	
    	
    	try {
    		OWLOntology ontology = OntologyHelper.getOntology(ontURI);
    		URI uri = ontology.getPhysicalURI();
    		
    		if (!(ontology.equals(null))) {
    			SWRLParser parser = new SWRLParser();
    			// For imported ontologies
    			importedOnts = (HashSet) ontology.getIncludedOntologies();
    			// If there is no imported ontology in the rules file it assumes
    			// that rules and ontology are in the same file
    			if (importedOnts.isEmpty()) {
    				parser.setOntology(ontology);
    				rules = parser.parseRules(uri);
    				
    			} else {
    				Iterator it = importedOnts.iterator();
    				ontology = (OWLOntology) it.next();
    				parser.setOntology(ontology);
    				rules = parser.parseRules(uri);    				
    			}
    		}
    	} catch (Exception e) {
    		System.out.println(e.getMessage());
    		rules = null;
    	}  		
    	reasoner.getKB().setRules(rules);
    	
    	
    	Interpreter interp = new Interpreter();
    	
    	List r = interp.rete.convertSWRLRules(reasoner.getKB().getRules());	        	        	
    	interp.rete.compile(r);
    	
    	
    	Set facts = interp.rete.compileFacts(reasoner.getKB().getABox());
    	
    	interp.addFacts(facts, true);
    	interp.run();
    	assertEquals(interp.inferredFacts.size(), 2);
//    	System.out.println();
//    	System.out.println( interp.inferredFacts.size() +  " inferred fact(s)");    	
    }
    
    public void testUserDefinedFloatDatatypes() {


        String ns = "http://www.lancs.ac.uk/ug/dobsong/owl/float_test.owl#";
        
        OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );
        model.read( base + "float_test.owl" );
        
        model.prepare();
        
        assertTrue( model.validate().isValid() );
        
        OntClass ThingWithFloatValue = model.getOntClass( ns + "ThingWithFloatValue" );
        OntClass ThingWithFloatProbability = model.getOntClass( ns + "ThingWithProbabilityValue" );
        
        Individual exampleThingWithFloatValue = model.getIndividual( ns + "exampleThingWithFloatValue" );
        Individual exampleThingWithFloatProbability = model.getIndividual( ns + "exampleThingWithProbabilityValue" );

        assertTrue( ThingWithFloatValue.hasSubClass( ThingWithFloatProbability ) );
        assertTrue( !ThingWithFloatProbability.hasSubClass( ThingWithFloatValue ) );
        
        assertTrue( exampleThingWithFloatValue.hasRDFType( ThingWithFloatValue ) );
        assertTrue( !exampleThingWithFloatValue.hasRDFType( ThingWithFloatProbability ) );
        
        assertTrue( exampleThingWithFloatProbability.hasRDFType( ThingWithFloatValue ) );
        assertTrue( exampleThingWithFloatProbability.hasRDFType( ThingWithFloatProbability ) );        
    }
    
    public void testUserDefinedDecimalDatatypes() {
        String ns = "http://www.lancs.ac.uk/ug/dobsong/owl/decimal_test.owl#";
        
        OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );
        model.read( base + "decimal_test.owl" );
        
        model.prepare();
        
        assertTrue( model.validate().isValid() );
        
        OntClass ThingWithDecimalValue = model.getOntClass( ns + "ThingWithDecimalValue" );
        OntClass ThingWithDecimalProbability = model.getOntClass( ns + "ThingWithDecimalProbability" );
        OntClass ThingWithIntegerValue = model.getOntClass( ns + "ThingWithIntegerValue" );
        

        Individual exampleThingWithDecimalValue = model.getIndividual( ns + "exampleThingWithDecimalValue" );
        Individual exampleThingWithDecimalProbability = model.getIndividual( ns + "exampleThingWithDecimalProbability" );

        assertTrue( ThingWithDecimalValue.hasSubClass( ThingWithIntegerValue ) );
        assertTrue( ThingWithDecimalValue.hasSubClass( ThingWithDecimalProbability ) );
        
        assertTrue( exampleThingWithDecimalValue.hasRDFType( ThingWithDecimalValue ) );
        
        assertTrue( exampleThingWithDecimalProbability.hasRDFType( ThingWithIntegerValue ) );
        assertTrue( exampleThingWithDecimalProbability.hasRDFType( ThingWithDecimalProbability ) );        
        assertTrue( exampleThingWithDecimalProbability.hasRDFType( ThingWithDecimalValue ) );
        
//        assertTrue( !ThingWithDecimalValue.hasSuperClass( ThingWithIntegerValue ) );
//        assertTrue( !ThingWithIntegerValue.hasSubClass( ThingWithDecimalProbability ) );
    }
    
    public void testUserDefinedDatatypes() {
 //       DatatypeReasoner.DEBUG = true;
        
        String ns = "http://www.mindswap.org/ontologies/family.owl#";
        
        OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );
        model.read( "http://www.mindswap.org/ontologies/family-ages.owl" );
        
//        ABox.DEBUG = true;
        model.prepare();
        
//        ((PelletInfGraph)model.getGraph()).getKB().realize();
//        ((PelletInfGraph)model.getGraph()).getKB().printClassTree();
       
        OntClass Child = model.getOntClass( ns + "Child" );
        OntClass Teenage = model.getOntClass( ns + "Teenage" );
        OntClass Adult = model.getOntClass( ns + "Adult" );
        OntClass Senior = model.getOntClass( ns + "Senior" );
        OntClass Female = model.getOntClass( ns + "Female" );
        OntClass Person = model.getOntClass( ns + "Person" );

        Individual Daughter = model.getIndividual( ns + "Daughter" );
        Individual Son = model.getIndividual( ns + "Son" );
        Individual Dad = model.getIndividual( ns + "Dad" );
        Individual Grandpa = model.getIndividual( ns + "Grandpa" );

        assertTrue( Adult.hasSubClass( Senior ) );
        
        assertIteratorValues( this, Adult.listSubClasses( true ), new Resource[] { Senior } );
 
        assertTrue( Daughter.hasRDFType( Child ) );
        assertIteratorValues( this, Daughter.listRDFTypes(false), new Resource[] { Female, Person, Child, OWL.Thing } );

        assertTrue( Son.hasRDFType( Teenage ) );

        assertTrue( Dad.hasRDFType( Adult ) );
        
        assertTrue( Grandpa.hasRDFType( Senior ) );
        
        assertIteratorValues( this, Adult.listInstances(), new Resource[] { Dad, Grandpa } );

    }
}
