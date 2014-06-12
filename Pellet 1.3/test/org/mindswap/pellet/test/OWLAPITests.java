/*
 * Created on Oct 26, 2005
 */
package org.mindswap.pellet.test;

import java.net.URI;

import junit.framework.TestSuite;

import org.mindswap.pellet.jena.PelletReasonerFactory;
import org.mindswap.pellet.owlapi.Reasoner;
import org.mindswap.pellet.utils.Namespaces;
import org.mindswap.pellet.utils.SetUtils;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLDataProperty;
import org.semanticweb.owl.model.OWLDataValue;
import org.semanticweb.owl.model.OWLException;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.change.AddDataPropertyInstance;
import org.semanticweb.owl.model.change.ChangeVisitor;
import org.semanticweb.owl.model.change.OntologyChange;
import org.semanticweb.owl.model.change.RemoveDataPropertyInstance;
import org.semanticweb.owl.model.helper.OntologyHelper;


/**
 * @author Evren Sirin
 *
 */
public class OWLAPITests  extends PelletTestCase {
    public static String base = "file:" + PelletTestSuite.base + "misc/";
    
    public static TestSuite suite() {
        TestSuite s = new TestSuite( "OWLAPITests" );
        s.addTestSuite( OWLAPITests.class );

        return s;
    }
    
    public void testInfiniteChain() throws Exception {
        URI ontURI = URI.create( base + "infiniteChain.owl" );
        OWLOntology ont = OntologyHelper.getOntology( ontURI );
        
        Reasoner reasoner = new Reasoner();
        reasoner.setOntology( ont );
        
        assertTrue( !reasoner.isConsistent() );
    }
    
//    public void testInfiniteChainDP() throws Exception {
//        URI ontURI = URI.create( base + "infiniteChainDP.owl" );
//        OWLOntology ont = OntologyHelper.getOntology( ontURI );
//        
//        Reasoner reasoner = new Reasoner();
//        reasoner.setOntology( ont );
//        
//        assertTrue( !reasoner.isConsistent() );
//    }
    
//    public void testTeams() throws Exception {
//        String ns = "http://owl.man.ac.uk/2005/sssw/teams#";
//        
//        URI ontURI = URI.create( base + "teams.owl" );
//        OWLOntology ont = OntologyHelper.getOntology( ontURI );
//        
//        Reasoner reasoner = new Reasoner();
//        reasoner.setOntology( ont );
//        
//        OWLIndividual t1 = ont.getIndividual( URI.create(ns + "OntologyFC") );
//        
//        OWLClass owlThing = ont.getClass( URI.create(Namespaces.OWL+"Thing" ));
//        OWLClass owlNothing = ont.getClass( URI.create(Namespaces.OWL+"Nothing" ));
//        
//        OWLClass Male = ont.getClass( URI.create(ns + "Male" ));
//        OWLClass Female = ont.getClass( URI.create(ns + "Female" ));
//        OWLIndividual Sam = ont.getIndividual( URI.create(ns + "Sam" ));
//        OWLIndividual Chris = ont.getIndividual( URI.create(ns + "Chris" ));
//
//        OWLClass Team = ont.getClass( URI.create(ns + "Team" ));
//        OWLClass MixedTeam = ont.getClass( URI.create(ns + "MixedTeam" ));
//        OWLClass NonSingletonTeam = ont.getClass( URI.create(ns + "NonSingletonTeam" ));
//        
//        assertTrue( reasoner.isConsistent() );
//        
//        assertTrue( reasoner.isDifferentFrom( Sam, Chris ) );
//        assertTrue( reasoner.isDifferentFrom( Chris, Sam ) );
//
//        assertTrue( reasoner.isSubClassOf( MixedTeam, Team ) );        
//        assertCollectionValues( this, reasoner.ancestorClassesOf(MixedTeam), 
//            new OWLClass[] { Team, NonSingletonTeam, owlThing } );
//        
//        assertTrue( reasoner.isSubClassOf( MixedTeam, NonSingletonTeam ) );
//        assertCollectionValues( this, reasoner.descendantClassesOf(NonSingletonTeam), 
//            new OWLClass[] { MixedTeam, owlNothing } );
//        
//        assertTrue( reasoner.isInstanceOf( t1, MixedTeam ) );
//        assertTrue( reasoner.instancesOf( MixedTeam ).contains( t1 ) );
//        assertCollectionValues( this, reasoner.typesOf( t1 ), 
//            new OWLClass[] { Team, NonSingletonTeam, MixedTeam, owlThing } );
//
////        Male.removeDisjointWith( Female );
////        Female.removeDisjointWith( Male );
////        Sam.removeDifferentFrom( Chris );
////        Chris.removeDifferentFrom( Sam );
////
////        assertTrue( !Sam.isDifferentFrom( Chris ) );
////        assertTrue( !Chris.isDifferentFrom( Sam ) );               
////        
////        assertTrue( MixedTeam.hasSuperClass( Team ) );        
////        assertIteratorValues( this, MixedTeam.listSuperClasses(), 
////            new Resource[] { Team, OWL.Thing } );
////        
////        assertTrue( !NonSingletonTeam.hasSuperClass( MixedTeam ) );
////        assertIteratorValues( this, NonSingletonTeam.listSuperClasses(), 
////            new Resource[] { Team, OWL.Thing } );
////        
////        assertTrue( t1.hasRDFType( MixedTeam ) );
////        assertTrue( t1.hasRDFType( MixedTeam, true ) );
////        assertIteratorValues( this, t1.listRDFTypes( false ), 
////            new Resource[] { Team, MixedTeam, OWL.Thing } );
//    }
    
    public void testRemoveLiteral() throws Exception {
        String ns = "http://www.example.org/test#";
        URI xsdDouble = URI.create("http://www.w3.org/2001/XMLSchema#double");
        
        URI ontURI = URI.create( base + "RemoveLiteral.owl" );
        OWLOntology ont = OntologyHelper.getOntology( ontURI );

        OWLDataProperty pInt = ont.getDataProperty( URI.create( ns + "pInt") );
        OWLDataProperty pDouble = ont.getDataProperty( URI.create( ns + "pDouble") );
        OWLDataProperty pBoolean = ont.getDataProperty( URI.create( ns + "pBoolean") );
        
        OWLIndividual ind = ont.getIndividual( URI.create( ns + "ind" ) );
        
        Reasoner reasoner = new Reasoner();
        reasoner.setOntology( ont );

        assertTrue( reasoner.isConsistent() );

        OWLDataValue valDouble = (OWLDataValue) reasoner.getPropertyValues(ind, pDouble).iterator().next();
        OntologyChange change = new RemoveDataPropertyInstance(ont, ind, pDouble, valDouble, null);
        change.accept((ChangeVisitor) ont);
        reasoner.refreshOntology();
        assertTrue( reasoner.getPropertyValues(ind, pDouble).isEmpty());
        
        OWLDataValue valInt = (OWLDataValue) reasoner.getPropertyValues(ind, pInt).iterator().next();
        change = new RemoveDataPropertyInstance(ont, ind, pInt, valInt, null);
        change.accept((ChangeVisitor) ont);
        reasoner.refreshOntology();
        assertTrue( reasoner.getPropertyValues(ind, pInt).isEmpty());

        OWLDataValue valBoolean = (OWLDataValue) reasoner.getPropertyValues(ind, pBoolean).iterator().next();
        change = new RemoveDataPropertyInstance(ont, ind, pBoolean, valBoolean, null);
        change.accept((ChangeVisitor) ont);
        reasoner.refreshOntology();
        assertTrue( reasoner.getPropertyValues(ind, pBoolean).isEmpty());

        assertTrue( reasoner.getDataPropertyValues(ind).isEmpty());

        OWLDataValue newVal = ont.getOWLDataFactory().getOWLConcreteData( 
            xsdDouble, null, new Double( 0.0 ) );
        change = new AddDataPropertyInstance(ont, ind, pDouble, newVal, null);
        change.accept((ChangeVisitor) ont);
        
        assertTrue( reasoner.isConsistent() );
    }
    
    public void testEconn1() throws Exception {
        String ns = "http://www.mindswap.org/2004/multipleOnt/FactoredOntologies/EasyTests/Easy2/people.owl#";
        
		OWLOntology ont = OntologyHelper.getOntology( URI.create( ns ) );

		Reasoner reasoner = new Reasoner();
		reasoner.setOntology(ont);
		
		assertTrue( reasoner.isConsistent() );
        
		assertTrue( !reasoner.isConsistent(
		    reasoner.getClass(URI.create(ns + "Unsat1"))) );
		assertTrue( !reasoner.isConsistent(
		    reasoner.getClass(URI.create(ns + "Unsat2"))) );
		assertTrue( !reasoner.isConsistent(
		    reasoner.getClass(URI.create(ns + "Unsat3"))) );
		assertTrue( !reasoner.isConsistent(
		    reasoner.getClass(URI.create(ns + "Unsat4"))) );
    }
    
    
    public void testUserDefinedDatatypes() throws Exception {
        String ns = "http://www.mindswap.org/ontologies/family.owl#";
        
        OWLOntology model = OntologyHelper.getOntology( URI.create( "http://www.mindswap.org/ontologies/family-ages.owl" ) );

        Reasoner reasoner = new Reasoner();
        reasoner.setOntology( model );
        
        OWLClass Child = model.getClass( URI.create( ns + "Child" ) );
        OWLClass Teenage = model.getClass( URI.create( ns + "Teenage" ) );
        OWLClass Adult = model.getClass( URI.create( ns + "Adult" ) );
        OWLClass Senior = model.getClass( URI.create( ns + "Senior" ) );

        OWLIndividual Daughter = model.getIndividual( URI.create( ns + "Daughter" ) );
        OWLIndividual Son = model.getIndividual( URI.create( ns + "Son" ) );
        OWLIndividual Dad = model.getIndividual( URI.create( ns + "Dad" ) );
        OWLIndividual Grandpa = model.getIndividual( URI.create( ns + "Grandpa" ) );

        assertTrue( reasoner.isSubClassOf( Senior, Adult ) );
        
        assertIteratorValues( this, reasoner.subClassesOf( Adult ).iterator(), 
            new Object[] { SetUtils.singleton(Senior) } );
 
        assertTrue( reasoner.isInstanceOf( Daughter, Child ) );

        assertTrue( reasoner.isInstanceOf( Son, Teenage ) );

        assertTrue( reasoner.isInstanceOf( Dad, Adult ) );
        
        assertTrue( reasoner.isInstanceOf( Grandpa, Senior ) );
    }
}
