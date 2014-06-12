/*
 * Created on Oct 12, 2004
 */
package org.mindswap.pellet.examples;

import java.util.Iterator;

import org.mindswap.pellet.jena.PelletReasoner;

import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ReasonerRegistry;
import com.hp.hpl.jena.reasoner.ValidityReport;
import com.hp.hpl.jena.reasoner.dig.DIGReasoner;
import com.hp.hpl.jena.reasoner.dig.DIGReasonerFactory;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.ReasonerVocabulary;

/**
 * An example to show some differences between Pellet and Racer. Racer is a higly optimized
 * DL reasoner that handles the DL SHIQ(D)- (which is also known as ALCHQIr+(D)-). Racer is
 * more optmized and robust than Pellet but has these shortcomings:
 * <ul>
 * 	<li>Racer does not support complete reasoning with respect to nominals, i.e. constructs 
 *  owl:oneOf and owl:hasValue. Racer uses the technique known as <i>pseudo nominal 
 *  transformation</i> which transforms the individuals in the enumareted classes to atomic 
 *  classes. This technique is not complete whereas Pellet provides sound and complete resoning
 *  for nominals by using the algorithms developed for SHOQ(D).</li>
 *  <li>Racer has the Unique Name Assumption whereas OWL semantics does not have this assumption.
 *  With Unique Name Assumption all individuals with different URI's are assumed to be different
 *  even though equality can be asserted with the owl:sameAs property. Pellet does not have the
 *  UNA.</li>
 *  <li>Racer assumes that all datatype properties are functional. It is not possible to have
 *  multiple values for a datatype property even if it is not functional.</li>
 * </ul>
 * This program shows some simple examples to demonstrate these differences. More explanations
 * about the examples are inside the code as comments.   
 *  
 * <p>For this program to run, Racer 
 * server should be running. The default HTTP port Jena uses is 8081 (for Jena 2.2) so Racer
 * should be startes with the command <code>racer.exe -http 8081</code>. See 
 * <a href="http://jena.sourceforge.net/how-to/dig-reasoner.html>Jena DIG interface</a> for
 * more details. 
 * 
 * @author Evren Sirin
 */
public class CompareReasoners {
    public static String ns = "urn:test:";
    
    public static void main(String[] args) {       
  	    // create reasoners (more reasoners can be added for comparison)
        Reasoner pellet = new PelletReasoner();
        
        Model cModel = ModelFactory.createDefaultModel();
        Resource conf = cModel.createResource();
        conf.addProperty( ReasonerVocabulary.EXT_REASONER_URL, cModel.createResource( "http://localhost:8080" ) );

        // create the reasoner factory and the reasoner
        DIGReasonerFactory drf = (DIGReasonerFactory) 
            ReasonerRegistry.theRegistry().getFactory( DIGReasonerFactory.URI );
        DIGReasoner racer = (DIGReasoner) drf.create( conf );
        
        Reasoner[] reasoners = new Reasoner[] { pellet, racer };
        String[] names = new String[] { "Pellet", "Racer" };
        
        testNominalReasoning( reasoners, names );
        
        testUniqueNameAssumption( reasoners, names );
        
        testDatatypeProperties( reasoners, names );
        
        testPropertyAssertions( reasoners, names );
        
        System.out.println();
    }
 
    public static void testNominalReasoning( Reasoner[] reasoners, String[] names ) {
        System.out.println("Comparing Nominal Reasoning");
        System.out.println("===========================");
    
        // create an empty model without any reasoner. This will be the common model
        // that will be used by all the reasoners
        OntModel model = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM );
        
        /*
         * Color is defined as a class and red, blue, yellow are instances of 
         * the Color class. There may be more Color instances for all we know. 
         */
        OntClass Color = model.createClass(ns + "Color");
        Individual red = model.createIndividual(ns + "red", Color);
        Individual blue = model.createIndividual(ns + "blue", Color);
        Individual yellow = model.createIndividual(ns + "yellow", Color);

        /*
         * hasColor is a property whose range is Color class
         */
        OntProperty hasColor = model.createObjectProperty(ns + "hasColor");
        hasColor.addRange( Color );
        
        /* 
         * PrimaryColors is defined as an enumerated class whose elements are the
         * individuals red, blue, yellow. This definition implies that PrimaryColors
         * is a subClassOf Color because any PrimaryColor is also a Color. 
         */
        OntClass PrimaryColors = model.createEnumeratedClass(ns + "PrimaryColors", 
            model.createList(new RDFNode[] {red, blue, yellow} ) );

        /*
         * MyFavoriteColors is another enumerated class with two elements. This 
         * definition also implies MyFavoriteColors is subClassOf Color.
         */
        OntClass MyFavoriteColors = model.createEnumeratedClass(ns + "MyFavoriteColors", 
            model.createList(new RDFNode[] {red, yellow} ) );

        /*
         * HasFourColors is a class that has four values for hasColor property.
         */
        OntClass HasFourColors = model.createClass( ns + "HasFourColors" );
        HasFourColors.addEquivalentClass(            
            model.createCardinalityRestriction(null, hasColor, 4 ));
        
        /*
         * OnlyHasPrimaryColors is a class whose all hasColor values are from
         * PrimaryColor class. 
         */
        OntClass OnlyHasPrimaryColors = model.createClass( ns + "OnlyHasPrimaryColors" );
        OnlyHasPrimaryColors.addEquivalentClass( 
            model.createAllValuesFromRestriction(null, hasColor, PrimaryColors ));
        
        /*
         * HasFourPrimaryColors is intersection of HasFourColors and  OnlyHasPrimaryColors.
         * This definition is not consistent because there are only three PrimaryColors
         * and it is not possible to have four values for the hasColor that are also
         * PrimaryColor.
         */
        OntClass HasFourPrimaryColors = model.createIntersectionClass( 
            ns + "HasFourPrimaryColors", model.createList(new RDFNode[] {HasFourColors, OnlyHasPrimaryColors} ) );
        
        /*
         * Create inferencing models based on the existing model using the given reasoners 
         */
        OntModel[] infModels = new OntModel[reasoners.length]; 
        for( int i = 0; i < reasoners.length; i++ ) {
            OntModelSpec spec = new OntModelSpec( OntModelSpec.OWL_MEM );
	        spec.setReasoner( reasoners[i] );
	        
            infModels[i] = ModelFactory.createOntologyModel( spec, model.getBaseModel() );
        }

        /*
         * Check if individual red belongs to class PrimaryColors. Both reasoners should answers
         * yes to this question. 
         */
	    System.out.println( "Is red a PrimaryColor?" );
        for( int i = 0; i < reasoners.length; i++ ) {
            System.out.println(names[i] + ": " +  
                (infModels[i].contains( red, RDF.type, MyFavoriteColors ) ? "Yes" : "No") + " ");
        }
        System.out.println();
	
        /*
         * Check if the HasFourPrimaryColors class is consistent. Pellet answers Yes (correct),
         * Racer answers No (wrong).
         */
	    System.out.println( "Is it possible to have 4 PrimaryColors?");
        for( int i = 0; i < reasoners.length; i++ ) {
            System.out.println(names[i] + ": " +  
                (infModels[i].contains( HasFourPrimaryColors, RDFS.subClassOf, OWL.Nothing )? "No" : "Yes" )  + " ");
        }
        System.out.println();
        
        /*
         * Find all the subclasses Color class. Pellet answers {PrimaryColors, MyFavoriteColors, 
         * HasFourPrimaryColors} (correct), Racer does not return any answers (wrong). Note that
         * HasFourPrimaryColors is subclass of Color because it is an inconcistent class, or in
         * other words denotes the empty set. Thus it will be subclass of every other class.
         */
        System.out.println( "What are the sub classes of Color?");
        for( int i = 0; i < names.length; i++ ) {
            printIterator( infModels[i].getOntClass( Color.getURI() ).listSubClasses(), names[i]);
        }
        System.out.println();
    }

    public static void testDatatypeProperties( Reasoner[] reasoners, String[] names ) {
        System.out.println("Comparing Datatype Properties");
        System.out.println("=============================");
    
        // create an empty model without any reasoner. This will be the common model
        // that will be used by all the reasoners
        OntModel model = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM );
        
        /*
         * Color is defined as a class and red, blue, yellow are instances of 
         * the Color class. There may be more Color instances for all we know. 
         */
        OntClass Person = model.createClass(ns + "Person");
        Individual john = model.createIndividual(ns + "JohnDoe", Person);
        /*
         * create a datatype property whihc is NOT functional
         */  
        DatatypeProperty email = model.createDatatypeProperty(ns + "email", false);
        
        john.addProperty( email, " john.doe@unknown.org ");
        john.addProperty( email, " jdoe@unknown.org ");

        /*
         * Create inferencing models based on the existing model using the given reasoners 
         */
        OntModel[] infModels = new OntModel[reasoners.length]; 
        for( int i = 0; i < reasoners.length; i++ ) {
            OntModelSpec spec = new OntModelSpec( OntModelSpec.OWL_MEM );
	        spec.setReasoner( reasoners[i] );
	        
            infModels[i] = ModelFactory.createOntologyModel( spec, model.getBaseModel() );
        }

	    System.out.println( "Is this ontology consistent?" );
        for( int i = 0; i < reasoners.length; i++ ) {
            ValidityReport report = infModels[i].validate();
            System.out.println(names[i] + ": " + report.isValid() );
        }       
        System.out.println();
    }
    
    public static void testUniqueNameAssumption( Reasoner[] reasoners, String[] names ) {
        System.out.println("Comparing Unique Name Assumption");
        System.out.println("================================");
        
        // create an empty model without any reasoner
        OntModel model = ModelFactory.createOntologyModel( OntModelSpec.OWL_DL_MEM );
        
        /*
         * Define the class Country and two instances USA and UnitedStates (without 
         * asserting owl:sameAs)
         */
        OntClass Country = model.createClass(ns + "Country");
        Individual USA = model.createIndividual(ns + "USA", Country);
        Individual UnitedStates = model.createIndividual(ns + "UnitedStates", Country);
 
        /*
         * Define functional property livesIn 
         */
        OntProperty livesIn = model.createObjectProperty(ns + "livesIn");
        livesIn.convertToFunctionalProperty();
        
        /*
         * JohnDoe is a Person instance who is asserted to live in both USA and UnitedStates.
         * OWL semantics dictate that the individuals USA and UnitedStates should be inferred
         * to be same because a functional property can have at most one value. However, under 
         * Unique Name Assumption this definition is inconsistent.
         */
        OntClass Person = model.createClass(ns + "Person");
        Individual JohnDoe = model.createIndividual(ns + "JohnDoe", Person);
        JohnDoe.addProperty( livesIn, USA );
        JohnDoe.addProperty( livesIn, UnitedStates );

        /*
         * Create inferencing models based on the existing model using the given reasoners 
         */
        OntModel[] infModels = new OntModel[reasoners.length]; 
        for( int i = 0; i < reasoners.length; i++ ) {
            OntModelSpec spec = new OntModelSpec( OntModelSpec.OWL_MEM );
	        spec.setReasoner( reasoners[i] );
	        
            infModels[i] = ModelFactory.createOntologyModel( spec, model.getBaseModel() );
        }
        
        /*
         * Check if JohnDoe belogns to Person class. This is an obvious question but due to
         * UNA Racer would infer that the KB is incoherent.
         */
        System.out.println( "Is JohnDoe a Person?");
        for( int i = 0; i < reasoners.length; i++ ) {
            try {
                System.out.print(names[i] + ": ");  
                System.out.println((infModels[i].contains( JohnDoe, RDF.type, Person ) ? "Yes" : "No") + " ");
            } catch(RuntimeException e) {
                System.out.println(e);
            }
        }
        System.out.println();

        /*
         * Check if USA and UnitedStates denotes the same indvidual. Pellet correctly answers yes
         * whereas Racer fails.
         */
        System.out.println( "Is USA same as UnitedStates?");
        for( int i = 0; i < reasoners.length; i++ ) {
            try {
                System.out.print(names[i] + ": ");  
                System.out.println((infModels[i].contains( USA, OWL.sameAs, UnitedStates ) ? "Yes" : "No") + " ");
            } catch(RuntimeException e) {
                System.out.println(e);
            }
        }
        System.out.println();
        
        /*
         * Check if USA and UnitedStates denotes the same indvidual. Pellet correctly answers yes
         * whereas Racer fails.
         */
        System.out.println( "Where does JohnDoe live?");
        for( int i = 0; i < reasoners.length; i++ ) {
            try {
                System.out.print(names[i] + ": ");  
                printIterator( infModels[i].listObjectsOfProperty( JohnDoe, livesIn ), names[i] );
            } catch(RuntimeException e) {
                System.out.println(e);
            }
        }
        System.out.println();
    }

    public static void testPropertyAssertions( Reasoner[] reasoners, String[] names ) {
        System.out.println("Comparing Reasoning about Property Assertions");
        System.out.println("=============================================");
        
        /*
         * create an empty model without any reasoner
         */ 
        OntModel model = ModelFactory.createOntologyModel( OntModelSpec.OWL_DL_MEM );

        /*
         * Create Person class
         */
        OntClass Person = model.createClass(ns + "Person");
        
        /*
         * hasSon property is subproperty of hasChild 
         */
        OntProperty hasChild = model.createObjectProperty(ns + "hasChild");
        OntProperty hasSon = model.createObjectProperty(ns + "hasSon");
        hasSon.addSuperProperty(hasChild);       
        
        /*
         * PersonWithSingleSon has only one son and no other children 
         */
        OntClass PersonWithSingleSon = model.createClass(ns + "PersonWithSingleSon");
        PersonWithSingleSon.addSuperClass( 
            model.createCardinalityRestriction(null,hasChild,1));
        PersonWithSingleSon.addSuperClass( 
            model.createCardinalityRestriction(null,hasSon,1));

        /*
         * Bob has a single child John and PersonWithSingleSon class also tells me
         * that John should be Bob's son. 
         */
        Individual Bob = model.createIndividual(ns + "Bob", PersonWithSingleSon);
        Individual John = model.createIndividual(ns + "John", Person);
        Bob.addProperty( hasChild, John );

        /*
         * Create inferencing models based on the existing model using the given reasoners 
         */
        OntModel[] infModels = new OntModel[reasoners.length]; 
        for( int i = 0; i < reasoners.length; i++ ) {
            OntModelSpec spec = new OntModelSpec( OntModelSpec.OWL_MEM );
	        spec.setReasoner( reasoners[i] );
	        
            infModels[i] = ModelFactory.createOntologyModel( spec, model.getBaseModel() );
        }
        
        /*
         * Print Bob's son
         */
        System.out.println("Bob's son:");
        for( int i = 0; i < reasoners.length; i++ ) {
            try {
                printIterator(infModels[i].listObjectsOfProperty( Bob, hasSon ),  names[i]);
            } catch(RuntimeException e) {
                System.out.println(e);
            }
        }
        System.out.println();
    }

    public static void printIterator(Iterator i, String header) {
        System.out.print(header + ": ");
        
        System.out.print("{");
        while (i.hasNext()) {
            Object node = i.next();
            if( node instanceof Resource ) {
                Resource resource = (Resource) node;
                if( resource.isAnon() )
                    System.out.print( resource );
                else
                    System.out.print( resource.getLocalName() );
            }
            else 
                System.out.print( node );
                
            if( i.hasNext() )
                System.out.print(", ");
        }
        System.out.println("} ");
    }
}
