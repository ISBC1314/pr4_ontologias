// The MIT License
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

package org.mindswap.pellet.jena;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.EconnectedKB;
import org.mindswap.pellet.Individual;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.Role;
import org.mindswap.pellet.exceptions.UnsupportedFeatureException;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.FileUtils;
import org.mindswap.pellet.utils.Namespaces;
import org.mindswap.pellet.utils.QNameProvider;
import org.mindswap.pellet.utils.URIUtils;

import aterm.ATermAppl;
import aterm.ATermList;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.impl.LiteralLabel;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class OWLLoader {
    protected static Log log = LogFactory.getLog( OWLLoader.class );
    
    /*
     * predicates related to restrictions (owl:onProperty, owl:allValuesFrom,
     * etc.) are preprocessed before all the triples are processed. these
     * predicates are stored in the following list so processTriples function
     * can ignore the triples with these predicates
     */
    final public static List SKIP_PROPS = Arrays.asList(new Node[] {
    	RDF.type.asNode(),
    	RDF.first.asNode(), RDF.rest.asNode(),
    	OWL.imports.asNode(),   
    	OWL.onProperty.asNode(),
    	OWL.hasValue.asNode(), 
        OWL.allValuesFrom.asNode(), OWL.someValuesFrom.asNode(), 
        OWL.minCardinality.asNode(), OWL.maxCardinality.asNode(),
        OWL.cardinality.asNode() });
    
    final public static List SKIP_TYPES = Arrays.asList(new Node[] {
        RDF.List.asNode(), OWL.Restriction.asNode(), 
        OWL.AllDifferent.asNode(), OWL.Ontology.asNode()
    });
    
    final public static Node OWL_ForeignOntology
		= Node.createURI( OWL.NAMESPACE + "foreignOntology" );


    final static String[] TYPES = { "Class", "Individual", "Object Property",
            "Datatype Property", "Datatype" };

    final static int CLASS      = 0x00;
    final static int INDIVIDUAL = 0x01;
    final static int OBJ_PROP   = 0x02;
    final static int DT_PROP    = 0x04;
    final static int ANT_PROP   = 0x08;
    final static int ONT_PROP   = 0x0F;
    final static int DATATYPE   = 0x10;
    final static int LINK_PROP   = 0x12;    
    
    public static QNameProvider qnames = new QNameProvider();
    
    private KnowledgeBase kb;

    private Graph graph;

    private Map terms;

    private Map lists;

    private Map restrictions;

    //Added for Foreign Classes
    private Map resourceLinkTypes;

    // E-connected ontologies
    private List linkedOntologies;
    
    private List warnings;

    public OWLLoader() {
        clear();
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }
    
    public Graph getGraph() {
        return graph;
    }
    
    public List getWarnings() {
        return warnings;
    }
    
    private void addWarning( String msg ) {
        warnings.add( msg );
        log.warn( msg );
    }

    public void clear() {
        terms = new HashMap();
        terms.put(OWL.Thing.asNode(), ATermUtils.TOP);
        terms.put(OWL.Nothing.asNode(), ATermUtils.BOTTOM);

        lists = new HashMap();
        restrictions = new HashMap();
        
        resourceLinkTypes = new HashMap(); 
        
        warnings = new ArrayList();
    }

    private Node getObject(Node subj, Node pred) {
        Iterator all = graph.find(subj, pred, null);
        if( all.hasNext() ) {
	        Triple triple = (Triple) all.next();
	
	        return triple.getObject();
        }
        else
            return null;
    }

    private boolean hasObject(Node subj, Node pred) {
        return graph.find(subj, pred, null).hasNext();
    }

    public ATermList createList( Node node ) {
        if( node.equals( RDF.nil.asNode() ) )
            return ATermUtils.EMPTY_LIST;
        else if( lists.containsKey( node ) ) 
            return (ATermList) lists.get( node );

        Node first = getObject( node, RDF.first.asNode() );
        Node rest = getObject( node, RDF.rest.asNode() );

        if( first == null || rest == null ) {
            addWarning( "Invalid list structure: List " + node + " does not have a "
                + (first == null ? "rdf:first" : "rdf:rest")
                + " property. Ignoring rest of the list." );
            return ATermUtils.EMPTY_LIST;
        }

        ATermList list = ATermUtils.makeList( node2term( first ), createList( rest ) );

        lists.put( node, list );

        return list;
    }

    public  ATermAppl createRestriction( Node node ) throws UnsupportedFeatureException {
        ATermAppl aTerm = ATermUtils.TOP;

        Node p = getObject(node, OWL.onProperty.asNode());

        // TODO warning message: no owl:onProperty
        if(p == null) return aTerm;

        ATermAppl pt = node2term(p);
        //        defineProperty(pt);

        // TODO warning message: multiple owl:onProperty
        Node o = null;
        if((o = getObject(node, OWL.hasValue.asNode())) != null) {
            if( PelletOptions.USE_PSEUDO_NOMINALS ) {
                if( o.isLiteral() ) {                       
                    aTerm = ATermUtils.makeMin(pt, 1);
                }
                else {
                    ATermAppl ind = ATermUtils.makeTermAppl( o.getURI() );
                    ATermAppl nom = ATermUtils.makeTermAppl( o.getURI() + "_nom" );
                    
                    defineClass( nom );
                    kb.addType( ind, nom );
                        
                    aTerm = ATermUtils.makeSomeValues(pt, nom);
                }
            }
            else {
                ATermAppl ot = node2term(o);
                
                if( o.isLiteral() )
                    defineDatatypeProperty( pt );
                else
                    defineObjectProperty( pt );
                    
                aTerm = ATermUtils.makeHasValue(pt, ot);
            }
        }
        else if((o = getObject(node, OWL.allValuesFrom.asNode())) != null) {
            ATermAppl ot = node2term(o);
                            
            if( kb.isClass( ot ) )
                defineObjectProperty( pt );
            else if( kb.isDatatype( ot ) )
                defineDatatypeProperty( pt );

            aTerm = ATermUtils.makeAllValues(pt, ot);
        }
        else if((o = getObject(node, OWL.someValuesFrom.asNode())) != null) {
            ATermAppl ot = node2term(o);
            
            if( kb.isClass( ot ) )
                defineObjectProperty( pt );
            else if( kb.isDatatype( ot ) )
                defineDatatypeProperty( pt );

            aTerm = ATermUtils.makeSomeValues(pt, ot);
        }
        else if((o = getObject(node, OWL.minCardinality.asNode())) != null) {
            try {
                int cardinality = Integer.parseInt(o.getLiteral().getLexicalForm());
                aTerm = ATermUtils.makeMin(pt, cardinality);
            } catch(Exception ex) {
                // TODO print warning message (invalid number)
            }
        }
        else if((o = getObject(node, OWL.maxCardinality.asNode())) != null) {
            try {
                int cardinality = Integer.parseInt(o.getLiteral().getLexicalForm());
                aTerm = ATermUtils.makeMax(pt, cardinality);
            } catch(Exception ex) {
                // TODO print warning message (invalid number)
            }
        }
        else if((o = getObject(node, OWL.cardinality.asNode())) != null) {
            try {
                int cardinality = Integer.parseInt(o.getLiteral().getLexicalForm());
                aTerm = ATermUtils.makeCard(pt, cardinality);
            } catch(Exception ex) {
                // TODO print warning message (invalid number)
            }
        }
        else {
            // TODO print warning message (invalid restriction type)
        }

        return aTerm;
    }

    public ATermAppl node2term(Node node) {
        ATermAppl aTerm = (ATermAppl) terms.get(node);

        if(aTerm == null) {
	        if(node.isLiteral()) {
	            LiteralLabel label = node.getLiteral();
	
	            String value = label.getLexicalForm();
	            String datatypeURI = label.getDatatypeURI();
	            String lang = label.language();
	
	            if(datatypeURI != null) 
	                aTerm = ATermUtils.makeTypedLiteral(value, datatypeURI);                
	            else
	                aTerm = ATermUtils.makePlainLiteral(value, lang);
	        }
	        else if(hasObject(node, OWL.onProperty.asNode())) {
	            aTerm = createRestriction(node);
	            restrictions.put(node, aTerm);
	        }
	        else if(node.isBlank()) {
	            Node o = null;
	            if((o = getObject(node, OWL.intersectionOf.asNode())) != null ) {
	                ATermList list = createList( o );
	                aTerm = ATermUtils.makeAnd(list);
	            }
	            else if((o = getObject(node, OWL.unionOf.asNode())) != null ) {
	                ATermList list = createList( o );
	                aTerm = ATermUtils.makeOr(list);
	            }
	            else if((o = getObject(node, OWL.oneOf.asNode())) != null) {
	                ATermList list = createList( o );
	                ATermList result = ATermUtils.EMPTY_LIST;
	                for(ATermList l = list; !l.isEmpty(); l = l.getNext()) {
	                    ATermAppl c = (ATermAppl) l.getFirst();
                        if( PelletOptions.USE_PSEUDO_NOMINALS ) {
                            ATermAppl nominal = ATermUtils.makeTermAppl(c.getName()+"_nominal");
                            result = result.insert(nominal);
                            
                            defineClass( nominal );
                            defineIndividual( c );
                            kb.addType(c, nominal);
                        }
                        else {
    	                    ATermAppl nominal = ATermUtils.makeValue(c);
    	                    result = result.insert(nominal);
                        }
	                }
	
	                aTerm = ATermUtils.makeOr(result);
	            }
	            else if((o = getObject(node, OWL.complementOf.asNode())) != null) {
	                ATermAppl complement = node2term( o );
	                aTerm = ATermUtils.makeNot(complement);
	            }
	            else {
	                String bNode = PelletOptions.BNODE + node.getBlankNodeId();
	                aTerm = ATermUtils.makeTermAppl(bNode);
	            }
	        }
	        else {
	            String uri = node.getURI();

	            if(PelletOptions.USE_LOCAL_NAME) {	        
		            if( uri.startsWith( Namespaces.XSD ) )
		                aTerm = ATermUtils.makeTermAppl( uri);
		            else
		                aTerm = ATermUtils.makeTermAppl( URIUtils.getLocalName( uri ) );
		        }
		        else if(PelletOptions.USE_QNAME) {
		            if( uri.startsWith( Namespaces.XSD ) )
		                aTerm = ATermUtils.makeTermAppl( uri);
		            else
		                aTerm = ATermUtils.makeTermAppl( qnames.shortForm( uri ) );
		        }
		        else 
		            aTerm = ATermUtils.makeTermAppl( uri );
	        }
	
	        terms.put(node, aTerm);
    	}

        return aTerm;
    }
    
    private boolean defineClass(ATermAppl c) {
        if(ATermUtils.isPrimitive(c)) {
            kb.addClass(c);
            return true;
        }
        else 
            return kb.isClass(c);
    }
    //*********************************************************
    //Added for Econnections
    //*********************************************************
    private boolean defineForeignClass(ATermAppl c) {
        Integer type = (Integer) resourceLinkTypes.get(c);
        if(type == null)
            type = new Integer(CLASS);
        else
            type = new Integer(type.intValue() | CLASS);
        resourceLinkTypes.put(c, type);
        if(kb.isClass(c))
            return true;
        else if(ATermUtils.isPrimitive(c)) {
            return true;
        }
        return false;
    }
    //******************************************************
    //Added for EConnections
    //*******************************************************
    private boolean defineForeignObjectProperty(ATermAppl c) {
        Integer type = (Integer) resourceLinkTypes.get(c);
        if(type == null)
            type = new Integer(OBJ_PROP);
        else
            type = new Integer(type.intValue() | OBJ_PROP);
        resourceLinkTypes.put(c, type);
        if(((EconnectedKB)kb).isObjectProperty(c))
            return true;
        else if(ATermUtils.isPrimitive(c)) {
            return true;
        }
        return false;
    }
    
   //******************************************************   
   //Added for Econnections
   //*******************************************************
    private boolean defineForeignLinkProperty(ATermAppl c) {
        Integer type = (Integer) resourceLinkTypes.get(c);
        if(type == null)
            type = new Integer(OBJ_PROP);
        else
            type = new Integer(type.intValue() | LINK_PROP);
        resourceLinkTypes.put(c, type);
        if(((EconnectedKB)kb).isProperty(c))
            return true;
        else if(ATermUtils.isPrimitive(c)) {
            return true;
        }
        return false;
    }
   
//  ******************************************************   
    //Added for Econnections
    //*******************************************************
     private boolean defineForeignDatatypeProperty(ATermAppl c) {
         Integer type = (Integer) resourceLinkTypes.get(c);
         if(type == null)
             type = new Integer(DT_PROP);
         else
             type = new Integer(type.intValue() | DT_PROP);
         resourceLinkTypes.put(c, type);
         if(((EconnectedKB)kb).isDatatypeProperty(c))
             return true;
         else if(ATermUtils.isPrimitive(c)) {
             return true;
         }
         return false;
    }
    
    private boolean defineDatatype(ATermAppl datatypeURI) {
        kb.addDatatype( datatypeURI );
        
        return true;
    }

    private boolean loadDatatype(ATermAppl datatypeURI) {
        kb.loadDatatype( datatypeURI );
        
        return true;
    }
    
    /**
     * 
     * There are two properties that are used in a subPropertyOf or
     * equivalentProperty axiom. If one of them is defined as an Object (or
     * Data) Property the other should also be defined as an Object (or Data)
     * Property
     * 
     * @param p1
     * @param p2
     * @return
     */
    private boolean defineProperties(ATermAppl p1, ATermAppl p2) {
        int type1 = kb.getPropertyType(p1);
        int type2 = kb.getPropertyType(p2);
        if(type1 != type2) {
            if(type1 == Role.UNTYPED) {
                if(type2 == Role.OBJECT)
                    defineObjectProperty(p1);
                else if(type2 == Role.DATATYPE) 
                    defineDatatypeProperty(p1);
            }
            else if(type2 == Role.UNTYPED) {
                if(type1 == Role.OBJECT)
                    defineObjectProperty(p2);
                else if(type1 == Role.DATATYPE) 
                    defineDatatypeProperty(p2);
            }
            else {
//                addWarning("Properties " + p1 + ", " + p2
//                    + " are related but first is " + Role.TYPES[type1]
//                    + "Property and second is " + Role.TYPES[type2]);                 
                return false;
            }
        }
        else if(type1 == Role.UNTYPED) {
            defineProperty(p1);
            defineProperty(p2);
        }
        
        return true;
    }

    private boolean defineObjectProperty(ATermAppl c) {
        if(!ATermUtils.isPrimitive(c)) 
            return false;

        return kb.addObjectProperty(c);
    }

    
    private boolean defineDatatypeProperty(ATermAppl c) {
        if(!ATermUtils.isPrimitive(c)) 
            return false;

        return kb.addDatatypeProperty(c);
    }
    
    private boolean defineAnnotationProperty(ATermAppl c) {
        if(!ATermUtils.isPrimitive(c)) 
            return false;

        kb.addAnnotationProperty(c);
        return true;
    }
    
    private boolean defineOntologyProperty(ATermAppl c) {
        if(!ATermUtils.isPrimitive(c)) 
            return false;

        kb.addOntologyProperty(c);
        return true;
    }
    
    private boolean defineProperty(ATermAppl c) {
        if(!ATermUtils.isPrimitive(c)) 
            return false;

        kb.addProperty(c);
        return true;
    }


    private boolean defineIndividual(ATermAppl c) {
        if(!ATermUtils.isPrimitive(c)) 
            return false;
        
        kb.addIndividual(c);
        return true;
    }

//    private void processLists(Model model) {
//        StmtIterator i = model.listStatements(null, RDF.first, (Resource) null);
//
//        // list pre-processing
//        while(i.hasNext()) {
//            Statement stmt = i.nextStatement();
//            StmtIterator si = model.listStatements(null, null, stmt.getSubject());
//            while(si.hasNext()) {
//                Statement aStmt = si.nextStatement();
//                if(!aStmt.getPredicate().equals(RDF.first) && 
//                   !aStmt.getPredicate().equals(RDF.rest)) {
//                    Resource s = stmt.getSubject();
//                    lists.put(s, createList(s));
//                    break;
//                }
//            }
//        }
//    }
    
//    // TODO process nested class expressions
//    void processClassExpressions(Model model) {
//        ATerm aTerm;
//        StmtIterator i;
//        
//        i = model.listStatements(null, OWL.onProperty, (Resource) null);
//        while(i.hasNext()) {        
//            Statement stmt = i.nextStatement();
//            Resource subj = stmt.getSubject();
//            aTerm = createRestriction( subj );
//            terms.put( subj, aTerm );
//            restrictions.put( subj, aTerm );
//        }
//        
//        i = model.listStatements(null, OWL.intersectionOf, (Resource) null);
//        while(i.hasNext()) {        
//            Statement stmt = i.nextStatement();
//            Resource subj = stmt.getSubject();
//            if(!subj.isAnon()) continue;
//            ATermList list = createList( stmt.getResource() );
//            aTerm = ATermUtils.makeAnd( list );
//            terms.put( subj, aTerm );
//        }
//
//        i = model.listStatements(null, OWL.unionOf, (Resource) null);
//        while(i.hasNext()) {        
//            Statement stmt = i.nextStatement();
//            Resource subj = stmt.getSubject();
//            if(!subj.isAnon()) continue;
//            ATermList list = createList( stmt.getResource() );
//            aTerm = ATermUtils.makeOr( list );
//            terms.put( subj, aTerm );
//        }
//
//        i = model.listStatements(null, OWL.oneOf, (Resource) null);
//        while(i.hasNext()) {        
//            Statement stmt = i.nextStatement();
//            Resource subj = stmt.getSubject();
//            if(!subj.isAnon()) continue;
//            ATermList list = createList( stmt.getResource() );
//            ATermList result = ATermUtils.EMPTY_LIST;
//            for(ATermList l = list; !l.isEmpty(); l = l.getNext()) {
//                ATermAppl c = (ATermAppl) l.getFirst();
//                ATermAppl nominal = ATermUtils.makeValue(c);
//                result = result.insert(nominal);
//            }
//
//            aTerm = ATermUtils.makeOr(result);
//            terms.put( subj, aTerm );
//        }
//        
//        i = model.listStatements(null, OWL.complementOf, (Resource) null);
//        while(i.hasNext()) {        
//            Statement stmt = i.nextStatement();
//            Resource subj = stmt.getSubject();
//            if(!subj.isAnon()) continue;
//            ATermAppl complement = node2term( stmt.getResource() );
//            aTerm = ATermUtils.makeNot(complement);
//            terms.put( subj, aTerm );
//        }
//    }

    private void processTypes(Graph model) throws UnsupportedFeatureException {
        Iterator i = model.find(null, RDF.type.asNode(), null);
        while(i.hasNext()) {
            Triple stmt = (Triple) i.next();
            Node o = stmt.getObject();

            if( SKIP_TYPES.contains( o ) ) {
                continue;
            }
            
            Node s = stmt.getSubject();
            ATermAppl st = node2term( s );

            String nameSpace = "";
            String localName = null;
            
            if( o.isURI() ) {
                nameSpace = o.getNameSpace();
                if( nameSpace == null ) nameSpace = "";
                localName = o.getLocalName();
            }
            
            
            if( o.equals( RDF.Nodes.Property ) ) {
                defineProperty(st);
            }
            else if( nameSpace.equals( Namespaces.RDFS ) ) {
                if(localName.equals("Class"))  {
                    defineClass(st);
                }
                else if(localName.equals("Datatype")) {
                    loadDatatype(st);
                }
                else {
                    addWarning( "Warning skipping invalid RDF-S term " + o );
                }
            } // startsWith( Namespaces.RDFS )
            else if( nameSpace.equals( Namespaces.OWL ) ) {
                if(localName.equals("Class")) {
                    defineClass(st);
                }
                else if(localName.equals("Thing")) {
                    defineIndividual(st);
                }
                else if(localName.equals("Nothing")) {
                    defineIndividual(st);
                    kb.addType(st, ATermUtils.BOTTOM);
                }                
                else if(localName.equals("ObjectProperty")) {
                    defineObjectProperty(st);
                }
                else if(localName.equals("DatatypeProperty")) {
                    defineDatatypeProperty(st);
                }
                else if(localName.equals("FunctionalProperty")) {
                    defineProperty(st);
                    kb.addFunctionalProperty(st);
                }
                else if(localName.equals("InverseFunctionalProperty")) {
                    if( defineProperty(st) )
                        kb.addInverseFunctionalProperty(st);
                    else
                        addWarning("Ignoring InverseFunctionalProperty axiom for " + 
                            st + " (" + Role.TYPES[kb.getPropertyType(st)] + "Property)"); 
                        
                }
                else if(localName.equals("TransitiveProperty")) {
                    if( defineObjectProperty(st) ) {
	                    kb.addTransitiveProperty(st);
	                    if (kb instanceof EconnectedKB)
	                    	((EconnectedKB)kb).getEconnExpressivity().setHasTransitivity(kb.getOntology());
                    }
                    else
                        addWarning("Ignoring TransitiveProperty axiom for " + 
                            st + " (" + Role.TYPES[kb.getPropertyType(st)] + "Property)");                     
                }
                else if(localName.equals("SymmetricProperty")) {
                    if( defineObjectProperty(st) )
                        kb.addSymmetricProperty(st);
                    else
                        addWarning("Ignoring SymmetricProperty axiom for " + 
                            st + " (" + Role.TYPES[kb.getPropertyType(st)] + "Property)");                     
                }
                else if(localName.equals("AnnotationProperty")) {
                    kb.addAnnotationProperty(st);
                }
                else if(localName.equals("DataRange")) {
                    Node dataValuesR = getObject( s, OWL.oneOf.asNode() );
                    if( dataValuesR == null ) {
                        String name = s.isBlank() ? "Anonymous owl:DataRange" : "DataRange " + s.getURI();
                        addWarning( name + " is missing the range decription (no owl:oneOf property)" );
                    }
                    else { 
                        ATermList list = createList( dataValuesR );
                        if( s.isBlank() )
                            kb.addDataRange(PelletOptions.BNODE + s.toString(), list);
                        else
                            kb.addDataRange(s.toString(), list);
                    }
                }
                //**************************************
                //Added for Econnections
                //**************************************            
                else if(kb instanceof EconnectedKB) {
                    EconnectedKB econnKB = (EconnectedKB) kb;
                    if( localName.equals("LinkProperty") ) {                
                        econnKB.addLinkProperty(st);
                	}                	
	                else if( localName.equals("ForeignIndividual" )) {
	                    econnKB.addIndividual(st);
                	}
	                else if( localName.equals("ForeignClass")) {
	                	defineForeignClass(st);            	        		
	                }                
	                else if( localName.equals("ForeignObjectProperty")) {
	                	defineForeignObjectProperty(st);            	        		
	                }                
	                else if( localName.equals("ForeignDatatypeProperty" )) {
	                	defineForeignDatatypeProperty(st);            	        		
	                }                
	                else if( localName.equals("ForeignLinkProperty" )) {
	                	defineForeignLinkProperty(st);            	        		
	                }
                }
                else {
                    addWarning( "Warning skipping invalid OWL term " + o );
                }
            } // startsWith( Namespaces.OWL )
            else {
                ATermAppl ot = node2term(o);
                
                defineIndividual(st);
                defineClass(ot);
                kb.addType(st, ot);
            } // else
        } // while
    } // processTypes

    private void processLinkTriples( Graph graph ) throws UnsupportedFeatureException {
        for( Iterator i = graph.find( Triple.ANY ); i.hasNext(); ) {
            Triple stmt = (Triple) i.next();

            Node s = stmt.getSubject();
            Node p = stmt.getPredicate();
            Node o = stmt.getObject();

            ATermAppl st = node2term( s );

            if( p.equals( RDF.type.asNode() ) ) {
                // these triples have been processed before so don't do anything
            }
            else if( p.equals( OWL_ForeignOntology ) ) {
                String foreignOnt = o.getURI();

                String ont = ((EconnectedKB) kb).getOntology();
                if( kb instanceof EconnectedKB ) {
                    // Links, as opposed to foreign properties have already been added to the KB
                    Role r = kb.getProperty( st );
                    if( r != null ) {
                        r.setForeignOntology( foreignOnt );

                        if( !linkedOntologies.contains( foreignOnt ) )
                            linkedOntologies.add( foreignOnt );
                    }
                    else {
                        if( resourceLinkTypes.containsKey( st ) ) {
                            if( !((EconnectedKB) kb).getTBoxes().keySet().contains( foreignOnt ) ) {
                                ((EconnectedKB) kb).addOntology( foreignOnt );
                            }
                            ((EconnectedKB) kb).setOntology( foreignOnt );
                            if( !(((EconnectedKB) kb).isClass( st )) && ATermUtils.isPrimitive( st ) ) {
                                if( ((Integer) resourceLinkTypes.get( st )).intValue() == CLASS ) {
                                    kb.addClass( st );
                                    if( log.isDebugEnabled() ) {
                                        log.debug( "Added Foreign Class" + st
                                            + "To ontology" + kb.getOntology() );
                                    }
                                }
                            }
                            if( !(((EconnectedKB) kb).isProperty( st ))
                                && ATermUtils.isPrimitive( st ) ) {
                                if( ((Integer) resourceLinkTypes.get( st )).intValue() == OBJ_PROP ) {
                                    kb.addObjectProperty( st );
                                    if( log.isDebugEnabled() ) {
                                        log.debug( "Added Foreign Object Property" + st
                                            + "To ontology" + kb.getOntology() );
                                    }
                                }
                                if( ((Integer) resourceLinkTypes.get( st )).intValue() == DT_PROP ) {
                                    kb.addDatatypeProperty( st );
                                    if( log.isDebugEnabled() ) {
                                        log.debug( "Added Foreign Datatype Property" + st
                                            + "To ontology" + kb.getOntology() );
                                    }
                                }
                                if( ((Integer) resourceLinkTypes.get( st )).intValue() == LINK_PROP ) {
                                    ((EconnectedKB) kb).addLinkProperty( st );
                                    if( log.isDebugEnabled() ) {
                                        log.debug( "Added Foreign Link Property" + st
                                            + "To ontology" + kb.getOntology() );
                                    }
                                }

                            }

                            ((EconnectedKB) kb).setOntology( ont );
                        }

                        else {
                            Individual aux = kb.getABox().getIndividual( st );
                            aux.setOntology( foreignOnt );
                            //For expressivity.We know that the foreign ontology needs to have nominals;
                            ((EconnectedKB) kb).getEconnExpressivity().setHasNominal( foreignOnt );
                        }
                    }
                }
            }
        }
    }//End processLinkTriples

    private void processTriples( Graph graph ) throws UnsupportedFeatureException {
        for( Iterator i = graph.find( Triple.ANY ); i.hasNext();) {
            Triple triple = (Triple) i.next();
            Node p = triple.getPredicate();
            
            if( SKIP_PROPS.contains(p) ) {
                // these triples have been processed before so don't do anything
                continue;
            }
            
            Node s = triple.getSubject();
            Node o = triple.getObject();

            String nameSpace = p.getNameSpace();
            String localName = p.getLocalName();
            
            if( nameSpace == null ) nameSpace = "";
            
            ATermAppl st = node2term( s );
            ATermAppl ot = node2term( o );

            if( nameSpace.equals( Namespaces.RDFS ) ) {
                if( localName.equals( "subClassOf" ) ) {
                    if( !defineClass(st) )
                        addWarning("Ignoring subClassOf axiom because the subject is not a class " + 
                            st + " rdfs:subClassOf " + ot );
                    else if( !defineClass(ot) ) 
                        addWarning("Ignoring subClassOf axiom because the object is not a class " + 
                            st + " rdfs:subClassOf " + ot );
                    else
                        kb.addSubClass(st, ot);
                }
                else if( localName.equals( "subPropertyOf" ) ) {
                    if( defineProperties(st, ot) )
                        kb.addSubProperty(st, ot);      
                    else
                        addWarning("Ignoring subproperty axiom between " + 
                            st + " (" + Role.TYPES[kb.getPropertyType(st)] + "Property) and " + 
                            ot + " (" + Role.TYPES[kb.getPropertyType(ot)] + "Property)"); 
                }
                else if( localName.equals( "domain" ) ) {
                    defineProperty(st);
                    defineClass(ot);
                    kb.addDomain(st, ot);
                }
                else if( localName.equals( "range" ) ) {                    
                    if(kb.isDatatype(ot))
                        defineDatatypeProperty(st);
                    else if(kb.isClass(ot))
                        defineObjectProperty(st);
                    else
                        defineProperty(st);

                    if(kb.isDatatypeProperty(st))
                        defineDatatype(ot);
                    else if(kb.isObjectProperty(st)) 
                        defineClass(ot);

                    kb.addRange(st, ot);
                }
            } // startsWith( Namespaces.RDFS )
            else if( nameSpace.equals( Namespaces.OWL ) ) {
                if( localName.equals( "intersectionOf" ) ) {
                    ATermList list = createList( o );

                    for(ATermList l = list; !l.isEmpty(); l = l.getNext()) {
                        ATermAppl c = (ATermAppl) l.getFirst();
                        //For Econnections. owl:ForeignClass
                        if(!resourceLinkTypes.containsKey(c))
                             defineClass(c);
                    } // for
                    
                    defineClass(st);
                    ATermAppl conjunction = ATermUtils.makeAnd(list);

                    kb.addEquivalentClass(st, conjunction);
                }
                else if( localName.equals( "unionOf" ) ) {
                    ATermList list = createList( o );
                    
                    for(ATermList l = list; !l.isEmpty(); l = l.getNext()) {
                        ATermAppl c = (ATermAppl) l.getFirst();
                        //************************************
                        //For Econnections. owl:ForeignClass
                        if(!resourceLinkTypes.containsKey(c))
                        	defineClass(c);
                        //************************************
                    } // for

                    defineClass(st);
                    ATermAppl disjunction = ATermUtils.makeOr(list);
                    kb.addEquivalentClass(st, disjunction);
                    if (kb instanceof EconnectedKB)
                    	((EconnectedKB)kb).getEconnExpressivity().setHasUnion(kb.getOntology());
                }
                else if(localName.equals( "complementOf" )) {
                    //For Econnections. owl:ForeignClass
                    if(!resourceLinkTypes.containsKey(st))
                        defineClass(st);
                    if(!resourceLinkTypes.containsKey(st))
                        defineClass(ot);
                    if(kb instanceof EconnectedKB)
                    	((EconnectedKB)kb).getEconnExpressivity().setHasNegation(kb.getOntology());
                    		
                    kb.addComplementClass(st, ot);
                }

                else if(localName.equals( "equivalentClass" )) {
                    if( !defineClass(st) )
                        addWarning("Ignoring equivalentClass axiom because the subject is not a class " + 
                            st + " owl:equivalentClass " + ot );
                    else if( !defineClass(ot) ) 
                        addWarning("Ignoring equivalentClass axiom because the object is not a class " + 
                            st + " owl:equivalentClass " + ot );
                    else
                        kb.addEquivalentClass(st, ot);
                }
                else if(localName.equals( "disjointWith" )) {
                    if( !defineClass(st) )
                        addWarning("Ignoring disjointWith axiom because the subject is not a class " + 
                            st + " owl:disjointWith " + ot );
                    else if( !defineClass(ot) ) 
                        addWarning("Ignoring disjointWith axiom because the object is not a class " + 
                            st + " owl:disjointWith " + ot );
                    else {
                        kb.addDisjointClass(st, ot);
                        if (kb instanceof EconnectedKB)
                        	((EconnectedKB)kb).getEconnExpressivity().setHasNegation(kb.getOntology());
                    }
                }
                else if( localName.equals( "equivalentProperty" ) ) {
                    if( defineProperties(st, ot) )
                        kb.addEquivalentProperty(st, ot);
                    else
                        addWarning("Ignoring equivalent property axiom between " + 
                            st + " (" + Role.TYPES[kb.getPropertyType(st)] + "Property) and " + 
                            ot + " (" + Role.TYPES[kb.getPropertyType(ot)] + "Property)"); 
                        
                }
                else if( localName.equals( "inverseOf" ) ) {
    	            //*******************************
    	            //Added for Econnections
    	            //*******************************	
    	            if(kb instanceof EconnectedKB) {
    	                EconnectedKB econnKB = (EconnectedKB) kb;	                
                        if(econnKB.isLinkProperty(st)) {
                            Role ro = kb.getProperty(st);
                            String ont = econnKB.getOntology();
                            String foreign = ro.getForeignOntology();
                            if(!econnKB.getTBoxes().keySet().contains(foreign)) {
                                econnKB.addOntology(foreign);
                            }
                            econnKB.setOntology(foreign);
                            if(!econnKB.getRBox().isRole(ot)) {
                                econnKB.addLinkProperty(ot);
                            }
                            Role r = kb.getProperty(ot);
                            if(r != null) {
                                r.setForeignOntology(ont);
                            }

                            econnKB.setOntology(ont);
                            econnKB.addInverseLink(ro, r);
                        }
                    } 	
    	            else {
    	            	if(defineObjectProperty(st) && defineObjectProperty(ot))
    	   	                kb.addInverseProperty(st, ot);
    	            	else
	                        addWarning("Ignoring inverseOf axiom between " + 
	                            st + " (" + Role.TYPES[kb.getPropertyType(st)] + "Property) and " + 
	                            ot + " (" + Role.TYPES[kb.getPropertyType(ot)] + "Property)");     	            	
    	            }
                }
                else if(localName.equals( "sameAs" ) ) {
                    if( defineIndividual(st) && defineIndividual(ot) )
                        kb.addSame(st, ot);
	            	else
                        addWarning("Ignoring sameAs axiom between " + st + " and " + ot );     	            	
                }
                else if(localName.equals( "differentFrom" ) ) {
                    if( defineIndividual(st) && defineIndividual(ot) )
                        kb.addDifferent(st, ot);
	            	else
                        addWarning("Ignoring differentFrom axiom between " + st + " and " + ot );     	            	
                }        
                else if(localName.equals( "distinctMembers" ) ) {
                    List result = new ArrayList();
                    ATermList list = createList( o );

                    for(ATermList l = list; !l.isEmpty(); l = l.getNext()) {
                        ATermAppl c = (ATermAppl) l.getFirst();
                        defineIndividual(c);
                        result.add(c);
                    } // for
                    for(int k = 0; k < result.size(); k++) {
                        for(int j = k + 1; j < result.size(); j++) {
                            kb.addDifferent((ATermAppl) result.get(k), (ATermAppl) result.get(j));
                        } // for
                    } // for
                }
                else if( localName.equals( "oneOf" ) ) {
                    ATermList result = ATermUtils.EMPTY_LIST;

                    if( kb.isDatatype( st ) )
                        continue;

                    // assert the subject is a class
                    defineClass( st );

                    ATermAppl disjunction = null;
                    ATermList list = createList( o );
                    if( o.equals( RDF.nil ) )
                        disjunction = ATermUtils.BOTTOM;
                    else {
                        for( ATermList l = list; !l.isEmpty(); l = l.getNext() ) {
                            ATermAppl c = (ATermAppl) l.getFirst();

                            if( PelletOptions.USE_PSEUDO_NOMINALS ) {
                                ATermAppl nominal = ATermUtils.makeTermAppl( c.getName() + "_nominal" );
                                result = result.insert( nominal );

                                defineClass( nominal );
                                defineIndividual( c );
                                kb.addType( c, nominal );
                            }
                            else {
                                defineIndividual( c );

                                if( kb instanceof EconnectedKB )
                                    ((EconnectedKB) kb).getEconnExpressivity().allNominals.add( ot );

                                result = result.insert( ATermUtils.makeValue( c ) );
                            }
                        }
                        disjunction = ATermUtils.makeOr( result );
                    }
                    kb.addEquivalentClass( st, disjunction );
                }
                // TODO ontology properties
//    			else if (kb.getProperty(pt).getType() == Role.ONTOLOGY) {
//    				Resource r = (Resource) o;
//    				Hashtable props = getOntologyDefinition(s);
//    				Vector propList = (Vector) props.get(p);
//    				if (propList == null)
//    					propList = new Vector();
//    				propList.add(o);
//    				props.put(p, propList);
//    			}
                else {
                    // TODO Handle unknown property that belongs to OWL namespace                    
                }
            } // startsWith( Namespaces.OWL )
            else {
                ATermAppl pt = node2term(p);
                Role role = kb.getProperty(pt);
                int type = (role == null) ? Role.UNTYPED : role.getType();

                if( type == Role.ANNOTATION ) {
                    continue;
                }

                if(o.isLiteral()) {       
                    if( defineDatatypeProperty(pt) ) {
                        String datatypeURI = ((ATermAppl) ot.getArgument( 2 )).getName();

                        if( defineIndividual(st) ) {                            
                            defineDatatypeProperty(pt);
                            if( !datatypeURI.equals("") )
                                defineDatatype(ATermUtils.makeTermAppl(datatypeURI));
    
                            kb.addPropertyValue(pt, st, ot);
                        }
                        else if( type == Role.UNTYPED )
                            defineAnnotationProperty( pt );
                        else
                            addWarning( "Ignoring ObjectProperty used with a class expression: " + triple );
                    }
                    else
                        addWarning( "Ignoring literal value used with ObjectProperty : " + triple );
                }
                else {
                    if( !defineObjectProperty(pt) ) 
                        addWarning( "Ignoring object value used with DatatypeProperty: " + triple );
                    else if( !defineIndividual(st) )
                        addWarning( "Ignoring class expression used in subject position: " + triple );
                    else if( !defineIndividual(ot) )
                        addWarning( "Ignoring class expression used in object position: " + triple );
                    else
	                    kb.addPropertyValue(pt, st, ot);
                }
            }
        }
    }

    private void processUntypedResources() {
        Iterator i = restrictions.keySet().iterator();
        while(i.hasNext()) {
            Node node = (Node) i.next();
            Node o = null;
            if( (o = getObject(node, OWL.onProperty.asNode())) != null ) {
                ATermAppl prop = node2term(o);
                
                defineProperty( prop );
                
                if( kb.isDatatypeProperty( prop ) ) {
                    if( (o = getObject(node, OWL.someValuesFrom.asNode())) != null ) {
                        defineDatatype( node2term(o) );
                    }                    
                    else if( (o = getObject(node, OWL.allValuesFrom.asNode())) != null ) {
                        defineDatatype( node2term(o) );
                    }                    
                }
            }
            
            if( (o = getObject(node, OWL.hasValue.asNode())) != null ) {
                if( !o.isLiteral())
                    defineIndividual(node2term(o));
            }
            
            if(kb instanceof EconnectedKB){
               EconnectedKB econnKB = (EconnectedKB) kb;
               
               if( hasObject( node, OWL.minCardinality.asNode() ) ||
                   hasObject( node, OWL.maxCardinality.asNode() ) ||
                   hasObject( node, OWL.cardinality.asNode() ) )
                   econnKB.getNumberRestrictions().add( restrictions.get( node ) );	
          	}
        }

        i = new ArrayList( kb.getRBox().getRoles() ).iterator();
        while(i.hasNext()) {
            Role r = (Role) i.next();

            if( r.isUntypedRole() ) {
                MultiIterator j = new MultiIterator( r.getSubRoles().iterator(), r.getSuperRoles().iterator() );
                while( j.hasNext() ) {
                    Role sub = (Role) j.next();
                    switch( sub.getType() ) {
                        case Role.OBJECT: 
                            defineObjectProperty( r.getName() ); 
                            break;
                        case Role.DATATYPE: 
                            defineDatatypeProperty( r.getName() ); 
                            break;
                        default: continue;
                    }
                }
            
                // all undefined props are assumed to be object properties
                defineObjectProperty( r.getName() );
            }
        }
    }

//    private void processMultitypedResources() {
//        Iterator i = resourceTypes.entrySet().iterator();
//        while(i.hasNext()) {
//            Map.Entry entry = (Map.Entry) i.next();
//            ATermAppl term = (ATermAppl) entry.getKey();
//            int type = ((Integer) entry.getValue()).intValue();
//
//            Set types = new HashSet();
//            for(int bit = 0; bit < 8; bit++) {
//                if((type & (1 << bit)) == 1) types.add(TYPES[bit]);
//            }
//
//            if(types.size() > 1)
//                addWarning("URI " + term.getName()
//                    + " has been defined/used as " + types);
//        }
//    }

    public void load(String ont, EconnectedKB kb) {       
        load(ont, kb, new ModelReader());
    }
    
    public void load(String ont, EconnectedKB kb, ModelReader reader)
    {       
    	//We make a list with the ontologies in the Econnection 
    	linkedOntologies = new ArrayList();
    	//Add the ontology we want to parse to the list. The list
    	//is going to contain the URIs of the ontologies
    	linkedOntologies.add(ont);
    	
    	//We go through the list of linked ontologies and set the current
    	//ontology to a different member of the list each time. Then, we parse
    	//that ontology
    	for(int i = 0; i < linkedOntologies.size(); i++) {
    		String currOnt = (String) linkedOntologies.get(i);
    		currOnt = FileUtils.toURI(currOnt);
    		Graph graph = reader.read(currOnt).getGraph();
    		if(!kb.getTBoxes().keySet().contains(currOnt))
    			kb.addOntology(currOnt);
    		kb.setOntology(currOnt);
    		load(graph, kb);
    	}    	
    }
        
    private void setKB( KnowledgeBase kb ) {
        this.kb = kb;
    }
    
    public void load( Graph graph, KnowledgeBase kb ) throws UnsupportedFeatureException {
        clear();

        setGraph( graph );
        setKB( kb );        

        resourceLinkTypes = new HashMap();
        
        defineAnnotationProperty(node2term(RDFS.label.asNode()));
        defineAnnotationProperty(node2term(RDFS.comment.asNode()));
        defineAnnotationProperty(node2term(RDFS.seeAlso.asNode()));
        defineAnnotationProperty(node2term(RDFS.isDefinedBy.asNode()));
        defineAnnotationProperty(node2term(OWL.versionInfo.asNode()));
        defineAnnotationProperty(node2term(DC.title.asNode()));
        defineAnnotationProperty(node2term(DC.description.asNode()));        
        defineOntologyProperty(node2term(OWL.backwardCompatibleWith.asNode()));
        defineOntologyProperty(node2term(OWL.priorVersion.asNode()));
        defineOntologyProperty(node2term(OWL.incompatibleWith.asNode()));

//        kb.timers.startTimer("processClassExpressions");
//        processClassExpressions(model); 
//        kb.timers.stopTimer("processClassExpressions");
//        kb.timers.startTimer("processTypes");
        processTypes( graph );
//        kb.timers.stopTimer("processTypes");
        //Added for Econnections
        if(kb instanceof EconnectedKB)
        	processLinkTriples( graph );
//        kb.timers.startTimer("processTriples");
        processTriples( graph );
//        kb.timers.stopTimer("processTriples");
//        kb.timers.startTimer("processUntypedResources");
        processUntypedResources();
//        kb.timers.startTimer("processUntypedResources");
    }


} // OWLParser
