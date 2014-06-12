/*
 * Created on Jan 7, 2005
 */
package org.mindswap.pellet.jena;

import java.io.StringWriter;

import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.output.ATermAbstractSyntaxRenderer;
import org.mindswap.pellet.output.ATermRenderer;
import org.mindswap.pellet.output.OutputFormatter;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.QNameProvider;

import aterm.ATerm;
import aterm.ATermAppl;
import aterm.ATermList;

import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.reasoner.InfGraph;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * @author Evren Sirin
 *
 */
public class NodeFormatter {
    private QNameProvider qnames;
    private StringWriter sw;
    private ATermRenderer renderer;
    private OutputFormatter formatter;
    
    private Model rawModel;
    
    private final static Resource NULL = null;
    
//    private Set usedStatements;
    
    public NodeFormatter(boolean formatHTML) {
        this(null, formatHTML);
    }
    
    public NodeFormatter(Model model) {
        this(model, false);
    }
    
    public NodeFormatter(Model model, boolean formatHTML) {
        if(model == null)
            throw new NullPointerException("No model given!");
        
        renderer = new ATermAbstractSyntaxRenderer();
        sw = new StringWriter();

        formatter = new OutputFormatter(sw, formatHTML);
        renderer.setWriter(formatter);        
                
        qnames = JenaUtils.makeQNameProvider(model);
        formatter.setQNames(qnames);
        
        if(model instanceof InfModel) {
            InfGraph graph = (InfGraph) model.getGraph();
            rawModel = ModelFactory.createModelForGraph( graph.getRawGraph() );
        }
        else
            rawModel = model;
    }
    
    public void setFormatHTML(boolean formatHTML) {
        formatter.setFormatHTML(formatHTML);
    }
    
    public QNameProvider getQNames() {
        return qnames;
    }
    
    public String format(RDFNode node) {
        if(node == null)
            return "<<null>>";
//        usedStatements = new HashSet();
        sw.getBuffer().setLength(0);
        
        ATerm term = node2term(node);
        if(term instanceof ATermAppl)
            renderer.visit((ATermAppl) term);
        else {
            sw.write("{");
            renderer.visitList((ATermList) term);
            sw.write("}");
        }
        
        return sw.toString();
    }    
    
    public ATerm node2term(RDFNode node) {
        ATerm aTerm = null;

        if(node.equals(OWL.Thing))
            return ATermUtils.TOP;
        else if(node.equals(OWL.Nothing))
            return ATermUtils.BOTTOM;
        else if(node instanceof com.hp.hpl.jena.rdf.model.Literal) {
            com.hp.hpl.jena.rdf.model.Literal l = (com.hp.hpl.jena.rdf.model.Literal) node;
            String datatypeURI = l.getDatatypeURI();
            if(datatypeURI != null)
                aTerm = ATermUtils.makeTypedLiteral(l.getString(), datatypeURI);
            else
                aTerm = ATermUtils.makePlainLiteral(l.getString(), l.getLanguage());
        }
        else if(node instanceof Resource) {
            Resource r = (Resource) node;

            if(rawModel.contains(r, OWL.onProperty, NULL)) {
                aTerm = createRestriction(r);
            }
            else if(r.isAnon()) {
                if(rawModel.contains(r, RDF.first, NULL)) {
                    aTerm = createList(r);
                }
                else if(rawModel.contains(r, OWL.intersectionOf)) {
                    ATermList list = createList(rawModel.getProperty(r, OWL.intersectionOf).getResource());
                    aTerm = ATermUtils.makeAnd(list);
                }
                else if(rawModel.contains(r, OWL.unionOf)) {
                    ATermList list = createList(rawModel.getProperty(r, OWL.unionOf).getResource());
                    aTerm = ATermUtils.makeOr(list);
                }
                else if(rawModel.contains(r, OWL.oneOf)) {
                    ATermList list = createList(rawModel.getProperty(r, OWL.oneOf).getResource());
                    ATermList result = ATermUtils.EMPTY_LIST;
                    for(ATermList l = list; !l.isEmpty(); l = l.getNext()) {
                        ATermAppl c = (ATermAppl) l.getFirst();
                        ATermAppl nominal = ATermUtils.makeValue(c);
                        result = result.insert(nominal);
                    }

                    aTerm = ATermUtils.makeOr(result);
                }
                else if(rawModel.contains(r, OWL.complementOf)) {
                    ATerm complement = node2term(rawModel.getProperty(r, OWL.complementOf).getResource());
                    aTerm = ATermUtils.makeNot(complement);
                }
                else
                    aTerm = ATermUtils.makeTermAppl(PelletOptions.BNODE + r.toString());
            }
            else {
                aTerm = ATermUtils.makeTermAppl(r.getURI());
            }
        }

        return aTerm;
    }

    private ATermAppl createRestriction(Resource s) {        
	    ATermAppl aTerm = ATermUtils.BOTTOM;

	    Statement stmt = rawModel.getProperty(s, OWL.onProperty);
//        usedStatements.add(stmt);

	    Resource p = stmt.getResource();
	    ATerm pt = node2term(p);	    
	
	    if(s.hasProperty(OWL.hasValue)) {
	        stmt = rawModel.getProperty(s, OWL.hasValue);
	        RDFNode o = stmt.getObject();
//	        usedStatements.add(stmt);
	        
	        ATerm ot = node2term(o);        
	        aTerm = ATermUtils.makeHasValue(pt, ot);
	    }
	    else if(s.hasProperty(OWL.allValuesFrom)) {
	        stmt = rawModel.getProperty(s, OWL.allValuesFrom);
//	        usedStatements.add(stmt);
	        
	        Resource o = stmt.getResource();
	        ATerm ot = node2term(o);
	        aTerm = ATermUtils.makeAllValues(pt, ot);
	    }
	    else if(s.hasProperty(OWL.someValuesFrom)) {
	        stmt = rawModel.getProperty(s, OWL.someValuesFrom);
//	        usedStatements.add(stmt);
	        
	        Resource o = stmt.getResource();
	        ATerm ot = node2term(o);    	        
	        aTerm = ATermUtils.makeSomeValues(pt, ot);
	    }
	    else if(s.hasProperty(OWL.minCardinality)) {
	        stmt = rawModel.getProperty(s, OWL.minCardinality);
//	        usedStatements.add(stmt);
	        
	        int cardinality = 0;
	        try {
	            cardinality = stmt.getInt();
	        } // try
	        catch(Exception ex) {
	            cardinality = Integer.parseInt(stmt.getLiteral().getLexicalForm());
	        } // catch
	        aTerm = ATermUtils.makeDisplayMin(pt, cardinality);
	    }
	    else if(s.hasProperty(OWL.maxCardinality)) {
	        stmt = rawModel.getProperty(s, OWL.maxCardinality);
//	        usedStatements.add(stmt);
	        
	        int cardinality = 0;
	        try {
	            cardinality = stmt.getInt();
	        } // try
	        catch(Exception ex) {
	            cardinality = Integer.parseInt(stmt.getLiteral().getLexicalForm());
	        } // catch
	        aTerm = ATermUtils.makeDisplayMax(pt, cardinality);
	    }
	    else if(s.hasProperty(OWL.cardinality)) {
	        stmt = rawModel.getProperty(s, OWL.cardinality);
//	        usedStatements.add(stmt);
	        
	        int cardinality = 0;
	        try {
	            cardinality = stmt.getInt();
	        } // try
	        catch(Exception ex) {
	            cardinality = Integer.parseInt(stmt.getLiteral().getLexicalForm());
	        } // catch
	        aTerm = ATermUtils.makeDisplayCard(pt, cardinality);
	    }
	    else {
	    }
	
	    return aTerm;
	} // createRestriction

    private ATermList createList(Resource r) {
        if(r.equals(RDF.nil))
            return ATermUtils.EMPTY_LIST;
        else if(!rawModel.contains(r, RDF.first)) {
            System.err.println("Invalid list structure: List " + r
                    + " does not have a rdf:first property. Ignoring rest of the list.");
            return ATermUtils.EMPTY_LIST;
        }

        ATerm first = node2term(rawModel.getProperty(r, RDF.first).getObject());
        Resource rest = rawModel.getProperty(r, RDF.rest).getResource();
        return ATermUtils.makeList(first, createList(rest));
    } // createList

}
