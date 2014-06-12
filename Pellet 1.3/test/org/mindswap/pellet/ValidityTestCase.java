/**
 * @(#) ValidityTestCase.java.
 */

package org.mindswap.pellet;

import java.io.BufferedInputStream;

import java.net.URL;

import junit.framework.TestCase;

import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.InfModel;

import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ValidityReport;

import com.hp.hpl.jena.ontology.OntModel;

import org.mindswap.pellet.jena.PelletReasonerFactory;

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.dom.DOMResult;

import com.hp.hpl.jena.rdf.arp.DOM2Model;

/**
 * This test case asserts the validity of the inference model and also tests whether the model is clean using the Jena Api. The test case uses the Mindswap service-policies example which is inconsistent by definition. The Jena Api defines clean as follows: A particular case of this arises in the case of OWL. In the Description Logic community a class which cannot have an instance is regarded as "inconsistent". That term is used because it generally arises from an error in the ontology. However, it is not a logical inconsistency - i.e. something giving rise to a contradiction. Having an instance of such a class is, clearly a logical error. In the Jena 2.2 release we clarified the semantics of isValid(). An ontology which is logically consistent but contains empty classes is regarded as valid (that is isValid() is false only if there is a logical inconsistency). Class expressions which cannot be instantiated are treated as warnings rather than errors. To make it easier to test for this case there is an additional method Report.isClean() which returns true if the ontology is both valid (logically consistent) and generated no warnings (such as inconsistent classes).    
 * @author <a href="richard.murphy@gsa.gov">Rick Murphy</a>
 * @version %I%,%G%
 */
public class ValidityTestCase extends TestCase{

  private OntModel model;  
  InfModel inferenceModel;  

  /**
   * Reads from the specified url into the ontology model using a buffered input stream
   * Binds the ontology model to pellet creating an inference model 
   */
  protected void setUp(){
    
    try{
      //transform normal form service policy to ontology model
      model = transformServicePolicy(new URL("http://www.mindswap.org/2005/services-policies/wsp2owl.xsl"),new URL("http://www.mindswap.org/2005/services-policies/normal-form.xml"));
      model.setDynamicImports(true);
      
      // creates a reasoner and writes inference model to console
      Reasoner reasoner = PelletReasonerFactory.theInstance().create();
      inferenceModel = ModelFactory.createInfModel(reasoner,model);
      
    }catch(Exception e){
      System.out.println("caught exception: " + e.getMessage());
    }
    
  }//setup
  
  /**
   * Asserts true on the validity of the inference model
   */
  public void testValidity(){
    
    // check the validity of the model
    ValidityReport report = inferenceModel.validate();
    assertTrue("assert true on validity:", report.isValid());

  }//testValidity

  /**
   * Asserts true on defined clean of the inference model
   */
  public void testClean(){
    
    // check the validity of the model
    ValidityReport report = inferenceModel.validate();
    assertTrue("assert true on clean criteria:", report.isClean());

  }//testValidity

  /**
   * Transforms the xml file into an ontology model based on the xsl stylesheet 
   * @param xsl The url of the xsl transformer stylesheet
   * @param xml The url of the xml file to be transformed
   * @return OntModel The constructed ontology model
   * @throws Exception Generic exception handler
   */
  private OntModel transformServicePolicy(URL xsl, URL xml) throws Exception{

    // transforms the xml file
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    Transformer transformer = TransformerFactory.newInstance().newTransformer(new StreamSource(new BufferedInputStream(xsl.openConnection().getInputStream())));
    transformer.transform(new StreamSource(new BufferedInputStream(xml.openConnection().getInputStream())),new DOMResult(doc));

    // creates the ontology model
    OntModel model = ModelFactory.createOntologyModel();

    // reads the transformed result into the model
    DOM2Model.createD2M("http://www.mindswap.org/2005/services-policies/",model).load(doc);
    
    return model;
    
  }//transform
  
}//ValidityTestCase
