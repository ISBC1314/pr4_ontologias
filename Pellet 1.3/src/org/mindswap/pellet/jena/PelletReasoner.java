//
// Copyright (c) 2003 Ron Alford, Mike Grove, Bijan Parsia, Evren Sirin
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to
// deal in the Software without restriction, including without limitation the
// rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
// sell copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
// IN THE SOFTWARE.

/*
 * Created on Sep 19, 2004
 */
package org.mindswap.pellet.jena;

import com.hp.hpl.jena.graph.Capabilities;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.impl.InfModelImpl;
import com.hp.hpl.jena.reasoner.InfGraph;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ReasonerException;
import com.hp.hpl.jena.reasoner.BaseInfGraph.InfCapabilities;
import com.hp.hpl.jena.vocabulary.ReasonerVocabulary;

/**
 * @author Evren Sirin
 */
public class PelletReasoner implements Reasoner {
    private PelletReasonerFactory factory;

    private Capabilities graphCapabilities;
    
    private Graph schema;

    public PelletReasoner( ) {
        this( null, PelletReasonerFactory.theInstance() );
    }
    
    public PelletReasoner( PelletReasonerFactory factory ) {
        this( null, factory );
    }
    
    public PelletReasoner( Graph schema, PelletReasonerFactory factory ) {
        this.schema = schema;
        this.factory = factory;
        
        graphCapabilities = new InfCapabilities();
     }
    
    public Graph getSchema() {
        return schema;
    }

    public Reasoner bindSchema( Graph graph ) throws ReasonerException {
        return new PelletReasoner( graph, factory );
    }

    public Reasoner bindSchema( Model model ) throws ReasonerException {
        return bindSchema( model.getGraph() );
    }

    public InfGraph bind( Graph graph ) throws ReasonerException {
        return new PelletInfGraph( graph, this );
    }
    
    public InfModel bind( Model model ) throws ReasonerException {
    	return new InfModelImpl( bind( model.getGraph() ) );
    }

    public void setDerivationLogging( boolean enable ) {
    }

    public void setParameter(Property arg0, Object arg1) {
    }

    public Model getReasonerCapabilities() {
        return (factory == null) ? null : factory.getCapabilities();
    }
    
    public Capabilities getGraphCapabilities() {
        return graphCapabilities;
    }

    public void addDescription(Model arg0, Resource arg1) {
    }

    public boolean supportsProperty(Property property) {
        if( factory == null ) return false;
        Model caps = factory.getCapabilities();
        Resource root = caps.getResource(factory.getURI());
        return caps.contains(root, ReasonerVocabulary.supportsP, property);
    }

}
