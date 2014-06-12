/**
 * @(#) RawModelTestCase.java.
 */

package org.mindswap.pellet;

import java.io.BufferedInputStream;

import java.net.URL;

import junit.framework.TestCase;

import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.ontology.OntModel;

/**
 * This test case is provided for completeness against the validity, graph, deductions, and inference model test cases using the Jena Api. The test case asserts not null on load and serialization of the raw model.
 * @author <a href="richard.murphy@gsa.gov">Rick Murphy</a>
 * @version %I%,%G%
 */
public class RawModelTestCase extends TestCase{

  private OntModel model;  
  
  /**
   * Reads from the specified url into the ontology model using a buffered input stream
   */
  protected void setUp(){
    
    // creates ontology model
    model = ModelFactory.createOntologyModel();
    
    try{
      model.read(new BufferedInputStream(new URL("http://www.mindswap.org/2005/services-policies/LanguageEncodingExample.owl").openConnection().getInputStream()),"");
    }catch(Exception e){
      System.out.println("caught exception: " + e.getMessage());
    }
    
  }//setup
  
  /**
   * Test not null assertion on model load for the raw model
   */
  public void testModelLoad(){
    assertNotNull("not null on load:", model.getRawModel());
  }//testModelLoad

  /**
   * Test not mull assertion on the default writer for the raw model
   */
  public void testWriter(){
    assertNotNull("not null on default writer:",model.getRawModel().getWriter());
  }//testModelLoad

}//RawModelTestCase
