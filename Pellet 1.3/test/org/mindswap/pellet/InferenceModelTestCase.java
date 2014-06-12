/**
 * @(#) InferenceModelTestCase.java.
 */

package org.mindswap.pellet;

import java.io.BufferedInputStream;

import java.net.URL;

import junit.framework.TestCase;

import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.InfModel;

import com.hp.hpl.jena.reasoner.Reasoner;

import com.hp.hpl.jena.ontology.OntModel;

import org.mindswap.pellet.jena.PelletReasonerFactory;

/**
 * This test case creates an inference model binding an ontology model to pellet using the Jena Api, then asserts not null on load and serialization of the inference model.
 * @author <a href="richard.murphy@gsa.gov">Rick Murphy</a>
 * @version %I%,%G%
 */
public class InferenceModelTestCase extends TestCase{

  private OntModel model;  
  private InfModel inferenceModel;
  
  /**
   * Reads from the specified url into the ontology model using a buffered input stream
   * Binds the ontology model to pellet creating an inference model 
   */
  protected void setUp(){
    
    // creates ontology model
    model = ModelFactory.createOntologyModel();
    
    try{
      model.read(new BufferedInputStream(new URL("http://www.mindswap.org/2005/services-policies/LanguageEncodingExample.owl").openConnection().getInputStream()),"");
    }catch(Exception e){
      System.out.println("caught exception: " + e.getMessage());
    }
    
    // creates a reasoner and writes inference model to console
    Reasoner reasoner = PelletReasonerFactory.theInstance().create();
    inferenceModel = ModelFactory.createInfModel(reasoner,model);
    
  }//setup
  
  /**
   * Test not null assertion on model load for the inference model
   */
  public void testModelLoad(){
    assertNotNull("not null on load:", inferenceModel);
  }//testModelLoad

  /**
   * Test not mull assertion on the default writer for the inference model
   */
  public void testWriter(){
    assertNotNull("not null on default writer:",inferenceModel.getWriter());
  }//testModelLoad

}//InferenceModelTestCase
