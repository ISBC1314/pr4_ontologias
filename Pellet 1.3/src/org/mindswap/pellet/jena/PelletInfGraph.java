//The MIT License
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
 * Created on Sep 18, 2004
 */
package org.mindswap.pellet.jena;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.Role;
import org.mindswap.pellet.exceptions.InternalReasonerException;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.SetUtils;

import aterm.ATermAppl;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.compose.Union;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.reasoner.BaseInfGraph;
import com.hp.hpl.jena.reasoner.FGraph;
import com.hp.hpl.jena.reasoner.Finder;
import com.hp.hpl.jena.reasoner.InfGraph;
import com.hp.hpl.jena.reasoner.StandardValidityReport;
import com.hp.hpl.jena.reasoner.TriplePattern;
import com.hp.hpl.jena.reasoner.ValidityReport;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.NiceIterator;
import com.hp.hpl.jena.util.iterator.NullIterator;
import com.hp.hpl.jena.util.iterator.SingletonIterator;
import com.hp.hpl.jena.util.iterator.UniqueExtendedIterator;
import com.hp.hpl.jena.util.iterator.WrappedIterator;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.ReasonerVocabulary;

/**
 * Implementation of Jena InfGraph interface which is backed by Pellet reasoner.
 * 
 * @author Evren Sirin
 */
public class PelletInfGraph extends BaseInfGraph implements InfGraph {
    protected static Log log = LogFactory.getLog( PelletInfGraph.class );

    /**
     * @deprecated Edit log4j.properties to turn on debugging
     */     
    public static boolean DEBUG = false;

    /**
     * This is a temporary option to say that while answering a TriplePattern
     * where the predicate is a wildcard only bind the predicate to properties
     * defined in the ontology but not rdf:type.
     */
    public static boolean GET_ONLY_PROPERTIES = false;
    
    public static final byte SUBJ = 0;
    public static final byte PRED = 1;
    public static final byte OBJ = 2;
        
    public static final Node RDF_type = RDF.Nodes.type;
    public static final Node RDF_directType = ReasonerVocabulary.directRDFType.asNode();
    public static final Node RDFS_subClassOf = RDFS.Nodes.subClassOf;
    public static final Node RDFS_directSubClassOf = ReasonerVocabulary.directSubClassOf.asNode();
    public static final Node OWL_equivalentClass = OWL.equivalentClass.asNode();
    public static final Node OWL_complementOf = OWL.complementOf.asNode();
    public static final Node OWL_disjointWith = OWL.disjointWith.asNode();
    public static final Node RDFS_subPropertyOf = RDFS.Nodes.subPropertyOf;
    public static final Node RDFS_directSubPropertyOf = ReasonerVocabulary.directSubPropertyOf.asNode();
    public static final Node OWL_equivalentProperty = OWL.equivalentProperty.asNode();
    public static final Node OWL_Class = OWL.Class.asNode();
    public static final Node RDFS_domain = RDFS.Nodes.domain;
    public static final Node RDFS_range = RDFS.Nodes.range;
    
    final private static Set TBOX_PROPS = SetUtils.create(new Node[] {
        RDFS_subClassOf, 
        RDFS_directSubClassOf,  
        OWL_equivalentClass, 
        OWL_complementOf,
        OWL_disjointWith
    });
    
    final private static Set ABOX_PROPS = SetUtils.create(new Node[] {
        OWL.sameAs.asNode(),
        OWL.differentFrom.asNode(),
    });

    final private static Set RBOX_PROPS = SetUtils.create(new Node[] {
        RDFS_subPropertyOf,
        RDFS_directSubPropertyOf,        
        OWL_equivalentProperty,
    });
    
    final private static Set RBOX_TYPES = SetUtils.create(new Node[] {
        RDF.Property.asNode(),
        OWL.ObjectProperty.asNode(),
        OWL.DatatypeProperty.asNode(),
        OWL.FunctionalProperty.asNode(),
        OWL.InverseFunctionalProperty.asNode(),
        OWL.SymmetricProperty.asNode(),
        OWL.TransitiveProperty.asNode(),
    });    
	
	private OWLReasoner reasoner;
	private KnowledgeBase kb;
	
	private Graph deductionsGraph;	
	private Graph rbox;
    
    private boolean lazyConsistency = false;
	
	private ATermToNodeMapper nodeMapper;	
	
    public PelletInfGraph(Graph graph, PelletReasoner pellet) {
        super(graph, pellet);

        reasoner = new OWLReasoner();
        nodeMapper = new ATermToNodeMapper();
        
        // we want a union of the input graph and reasoner schema
        if (pellet.getSchema() != null) {
            DisjointMultiUnion union = new DisjointMultiUnion( graph );
            union.addGraph( pellet.getSchema() );
            fdata = new FGraph( union );
        }

        kb = reasoner.getKB();

		rebind();
    }

    public boolean isLazyConsistency() {
        return lazyConsistency;
    }

    public void setLazyConsistency( boolean lazyConsistency ) {
        this.lazyConsistency = lazyConsistency;
    }

    public ExtendedIterator find(Node subject, Node property, Node object, Graph param) {
        OWLLoader loader = reasoner.getLoader();
        Graph graph = loader.getGraph();
        Graph union = new Union( graph, param );
        loader.setGraph( union );
        
        ExtendedIterator result = find( subject, property, object );
        
        loader.setGraph( graph );
        
        return result;
    }
    
    public ExtendedIterator findWithContinuation(TriplePattern pattern, Finder finder) {
        prepare();
        
        // TODO what is the difference between Node_Variable and Node_ANY
        Node subject   = pattern.getSubject().isVariable() ? Node.ANY : pattern.getSubject();
        Node predicate = pattern.getPredicate().isVariable() ? Node.ANY : pattern.getPredicate();
        Node object    = pattern.getObject().isVariable() ? Node.ANY : pattern.getObject();
        
//        if( log.isDebugEnabled() ) log.debug("Find " + subject + " " + predicate + " " + object);
        
        ExtendedIterator i =  new MultiIterator(
            findInTBox( subject, predicate, object )).andThen(
            findInRBox( subject, predicate, object ).andThen(        
            findInABox( subject, predicate, object )));
        
        // always look at asserted triples at the end
        if( finder != null ) {
            TriplePattern tp = new TriplePattern(subject, predicate, object );
            i = i.andThen( finder.find( tp ));
        }
        
        // make sure we don't have duplicates
        return UniqueExtendedIterator.create( i );
    }

    
    
    public Graph getSchemaGraph() {
        return ((PelletReasoner) getReasoner()).getSchema();
    }
    
    public boolean isPrepared() {
        return isPrepared;
    }
    
    public void prepare() {
        if(isPrepared)
            return;

        if( log.isDebugEnabled() ) log.debug("Preparing PelletInfGraph...");  

        if( log.isDebugEnabled() ) log.debug("Computing changes...");  

        DisjointMultiUnion union = (DisjointMultiUnion) reasoner.getModel().getGraph();         

        Graph rawGraph = getRawGraph();
        DisjointMultiUnion newUnion = new DisjointMultiUnion( rawGraph );
        DisjointMultiUnion diff = union.isStatementDeleted() ? null : newUnion.minus( union );
        
        if( diff != null ) {
            if( log.isDebugEnabled() ) log.debug("Loading diff..."); 
            reasoner.load( diff );
        }
        else {
            if( log.isDebugEnabled() ) log.debug("Reloading...");
            reasoner.clear();
            reasoner.load( newUnion );
        }                
        newUnion.releaseListeners();
        union.resetChanged();

        if( log.isDebugEnabled() ) log.debug("Consistency..."); 
        if( lazyConsistency )
            kb.prepare();
        else
            kb.isConsistent();
        
        deductionsGraph = null;
        
        ModelExtractor extractor = reasoner.getModelExtractor();
        extractor.setVerbose( true );
        extractor.setIncludeDirects( true );
        
        rbox = extractor.extractPropertyModel().getGraph();;

        if( log.isDebugEnabled() ) log.debug("done.");
        
        isPrepared = true;
        
//        if(DEBUG) {
//            classify();
//            kb.printClassTree();
//            kb.timers.print();
//        	kb.timers.resetAll();        
//        }
    }

    public boolean isConsistent() {
        prepare();

        return kb.isConsistent();
    }
    
    public boolean isClassified() {
        return isPrepared && kb.isRealized();
    }

    public void classify() {
        prepare();
        
        kb.realize();
    }
    
    public Graph getDeductionsGraph() {
        classify();
        
        if( deductionsGraph == null ) {
            if( log.isDebugEnabled() ) log.debug("Realizing PelletInfGraph...");
	        kb.realize();
	        
	        if( log.isDebugEnabled() ) log.debug("Extract model...");
	        ModelExtractor extractor = reasoner.getModelExtractor();
	        extractor.setVerbose( true );
	        extractor.setIncludeDirects( true );
	        
	        Model extractedModel = extractor.extractModel();
	        deductionsGraph = extractedModel.getGraph();
	        
	        if( log.isDebugEnabled() ) log.debug("done.");
	        
//	        if(DEBUG) extractedModel.write(System.out);   
        }
        
        return deductionsGraph;
    }

//    private boolean possibleInTBox( Node subject, Node predicate, Node object ) {
//        return ( !predicate.isURI() || TBOX_PROPS.contains( predicate ) );
//    }
//    
//    private ExtendedIterator findInTBox( Node subject, Node predicate, Node object ) {
//        ExtendedIterator i = NullIterator.instance;
//                
//        if( subject.isConcrete() && predicate.isURI() && object.isConcrete() ) { 
//            String predURI = predicate.getURI();
//            
//            if( predURI.equals( OWL.disjointWith.getURI() )) {
//                if( kb.isDisjoint( JenaUtils.makeTerm( subject ), JenaUtils.makeTerm( object ) ) )
//                    i = new SingletonIterator( new Triple( subject, predicate, object ) );
//            }
//            else if( predURI.equals( OWL.complementOf.getURI() )) {
//                if( kb.isComplement( JenaUtils.makeTerm( subject ), JenaUtils.makeTerm( object ) ) )
//                    i = new SingletonIterator( new Triple( subject, predicate, object ) );
//            }
//            else
//                i = getDeductionsGraph().find( subject, predicate, object );
//        }
//        else
//            i = getDeductionsGraph().find( subject, predicate, object );
//        
//        if( !predicate.isURI() )
//            i = i.filterKeep( keepOrdinaryTriples );
//        
//        return i;
//    }
    
    /**
     * Find the triples in the KB that matches the given parameters. Returns empty iterator
     * if nothing found, returns null if ABox cannot answer the query.
     * 
     * @param subject
     * @param predicate
     * @param object
     * @return
     */
    private ExtendedIterator findInABox(Node subject, Node predicate, Node object) {  
        ATermAppl s = node2term( subject );
        ATermAppl p = node2term( predicate );        
        ATermAppl o = node2term( object );
        
        boolean possibleInABox = true;
        
        possibleInABox &= !subject.isConcrete() || kb.isIndividual( s );

        if( predicate.equals( RDF_type ) || predicate.equals( RDF_directType ) )
            possibleInABox &= !object.isConcrete() || kb.isClass( o );
        else {
            if( predicate.isURI() ) 
                possibleInABox &= ABOX_PROPS.contains( predicate ) || kb.isABoxProperty( p );
            
            possibleInABox &= !object.isConcrete() || kb.isIndividual( o ) || ATermUtils.isLiteral( o );
        }
        
        if( !possibleInABox )
            return NullIterator.instance;

        ExtendedIterator iterator = NullIterator.instance;

        if( !predicate.isURI() || predicate.equals( RDF_type ) ) {
            boolean getAll = !predicate.isURI();
            
            if( subject.isConcrete() ) {
                if( object.isConcrete() ) {
                    if( getAll )
                        iterator = iterator.andThen( propertyIterator( subject, s, object, o ) );     
                    
	                if( !GET_ONLY_PROPERTIES && kb.isType( s, o ) )
	                    iterator = singletonIterator( subject, RDF_type, object );     
                }
                else {
	                if( getAll )
	                    iterator = new ABoxPredObjIterator( subject, s, OBJ );
                    
                    if( !GET_ONLY_PROPERTIES )
                        iterator = iterator.andThen( typeIterator( subject, s ) );
                }
            }
            else if( object.isConcrete() ) {
                if( getAll )
                    iterator = new ABoxPredObjIterator( object, o, SUBJ );  
                
                if( !GET_ONLY_PROPERTIES )
                    iterator = iterator.andThen( instanceIterator( object, o ) );
            }
            else {
                if( getAll )
                    iterator = new ABoxSubjPredObjIterator( ABoxSubjPredObjIterator.PROPS );
                
                if( !GET_ONLY_PROPERTIES )
                    iterator = iterator.andThen( 
                        new ABoxSubjPredObjIterator( ABoxSubjPredObjIterator.TYPES ) );               
            }
        }
        else if( predicate.equals( RDF_directType )) {
            if( subject.isConcrete() ) {
                if( object.isConcrete() ) {
	                if( kb.getTypes( s, true ).contains( o ) )
	                    iterator = singletonIterator( subject, RDF_directType, object );     
                }
                else {
	                iterator = directTypeIterator( subject, s );
                }
            }
            else if( object.isConcrete() ) {
                iterator = directInstanceIterator( object, o );
            }
            else {
                iterator = new ABoxSubjPredObjIterator( ABoxSubjPredObjIterator.DIRECT_TYPES );
            }
        }
        else if( predicate.equals( OWL.sameAs.asNode() )) {
            if( subject.isConcrete() && object.isConcrete() ) { 
	            if( kb.isSameAs( s, o ) )
	                iterator = singletonIterator( subject, predicate, object );
            } else if (subject.isConcrete()) {
            		iterator = sameAsIterator(subject, s, OBJ);
            } else if (object.isConcrete()) {
            		iterator = sameAsIterator(object, o, SUBJ);
            }
        }
        else if( predicate.equals( OWL.differentFrom.asNode() )) {
            if( subject.isConcrete() && object.isConcrete() ) { 
	            if( kb.isDifferentFrom( s, o ) )
	                iterator = singletonIterator( subject, predicate, object );
            }     
        }
        else if( !subject.isConcrete() ) {
            if( !object.isConcrete() ) 	        
                iterator = new ABoxSubjObjIterator( predicate, p );
            else 
                iterator = subjectIterator( object, o, predicate, p ); 
        }
        else if( !object.isConcrete() ) {
            iterator = objectIterator( subject, s, predicate, p ); 
        }
        else if( kb.hasPropertyValue( s, p, o ) ) {
            iterator = singletonIterator( subject, predicate, object );
        }
        
        
        return iterator;
    }

    private ExtendedIterator findInRBox(Node subject, Node predicate, Node object) {  
        boolean possibleInRBox = true;
        
        possibleInRBox &= !subject.isURI() || kb.isProperty( node2term( subject ) );

        if( predicate.equals( RDF_type ) ) {
            possibleInRBox &= !object.isURI() || RBOX_TYPES.contains( object );
//		else if( predicate.equals( RDFS_domain ) ) {
//		    if( !subject.isConcrete() ) {
//			    if( !object.isConcrete() )
//			        return Boolean.TRUE;
//			    else
//			        return !kb.getDomains(o).isEmpty() ? Boolean.TRUE : Boolean.FALSE;
//		    }
//		    else if( !object.isConcrete() )
//		        return !kb.getDomains(s).isEmpty() ? Boolean.TRUE : Boolean.FALSE;
//		    else
//		        return kb.hasDomain(s, o) ? Boolean.TRUE : Boolean.FALSE;
//		}
//		else if( predicate.equals( RDFS_range ) ) {
//		    if( !subject.isConcrete() ) {
//			    if( !object.isConcrete() )
//			        return Boolean.TRUE;
//			    else
//			        return !kb.getRanges(o).isEmpty() ? Boolean.TRUE : Boolean.FALSE;
//		    }
//		    else if( !object.isConcrete() )
//		        return !kb.getRanges(s).isEmpty() ? Boolean.TRUE : Boolean.FALSE;
//		    else
//		        return kb.hasRange(s, o) ? Boolean.TRUE : Boolean.FALSE;
//		}
        }
        else {
            possibleInRBox &= !object.isURI() || kb.isProperty( node2term( object ) );
            if( predicate.isURI() ) 
                possibleInRBox &= RBOX_PROPS.contains( predicate );
        }
        
        if( !possibleInRBox )
            return NullIterator.instance;
        
        return rbox.find( subject, predicate, object );
    }

    private ExtendedIterator findInTBox(Node subject, Node predicate, Node object) {  
        ATermAppl s = node2term( subject );
        ATermAppl o = node2term( object );
        
        boolean possibleInTBox = true;
        
        possibleInTBox &= !subject.isConcrete() || kb.isClass( s );

        if( predicate.equals( RDF_type ) )
            possibleInTBox &= !object.isConcrete() || object.equals( OWL_Class );
        else {
            if( predicate.isURI() ) 
                possibleInTBox &= TBOX_PROPS.contains( predicate );
            possibleInTBox &= !object.isConcrete() || kb.isClass( o );
        }
        
        if( !possibleInTBox )
            return NullIterator.instance;

        ExtendedIterator iterator = NullIterator.instance;
        if( !predicate.isURI() ) {
            if( !subject.isConcrete() ) {
	            if( !object.isConcrete() ) {
	                iterator = new TBoxSubjObjIterator( null );
	            }
	            else {
	                iterator = 
                        eqClassIterator( object, o, SUBJ ).andThen(
                        subClassIterator( object, o, false ).andThen(
                        disjointIterator( object, o, SUBJ ).andThen(
                        complementIterator( object, o, SUBJ ) ) ) );	                
	            }
	        }
	        else if( !object.isConcrete() ) {
	            iterator = 
	                typeClassIterator( subject ).andThen(
                    eqClassIterator( subject, s, OBJ ).andThen(
                    superClassIterator( subject, s, false ).andThen(
                    disjointIterator( subject, s, OBJ ).andThen(
                    complementIterator( subject, s, OBJ ) ) ) ) );
            }
	        else {		            
	            // TODO find( Class1, ?, Class2 )
	        }	        
        }
        else if( predicate.equals( RDFS_subClassOf ) ) {
            if( !subject.isConcrete() ) {
	            if( !object.isConcrete() )
	                iterator = new TBoxSubjObjIterator( RDFS_subClassOf );	            
	            else
	                iterator = subClassIterator( object, o, false );		                	            
	        }
	        else if( !object.isConcrete() ) {
	            iterator = superClassIterator( subject, s, false );
            }
	        else if( kb.isSubClassOf( s, o ) ){		            
	            iterator = singletonIterator( subject, predicate, object );
	        }            
        }
        else if( predicate.equals( RDFS_directSubClassOf ) ) {
            if( !subject.isConcrete() ) {
	            if( !object.isConcrete() )
	                iterator = new TBoxSubjObjIterator( RDFS_directSubClassOf );            
	            else
	                iterator = subClassIterator( object, o, true );		                	            
	        }
	        else if( !object.isConcrete() ) {
	            iterator = superClassIterator( subject, s, true );
            }
	        else if( kb.getSubClasses( s, true ).contains( o ) ) {		            
	            iterator = singletonIterator( subject, RDFS_directSubClassOf, object );
	        }             
        }
        else if( predicate.equals( RDF_type ) ) {
            if( !subject.isConcrete() ) {
	            iterator = WrappedIterator.create( 
		            kb.getAllClasses().iterator() ).
					mapWith( nodeMapper ).
					mapWith( TripleFiller.fillSubj( RDF_type, OWL_Class ) ); 
            }
	        else if( kb.isClass( s ) ) {		            
	            iterator = typeClassIterator( subject );
	        }                
        }
        else if( predicate.equals( OWL_equivalentClass ) ) {
            if( !subject.isConcrete() ) {
	            if( !object.isConcrete() )
	                iterator = new TBoxSubjObjIterator( OWL_equivalentClass );           
	            else
	                iterator = eqClassIterator( object, o, SUBJ );		                	            
	        }
	        else if( !object.isConcrete() ) {
	            iterator = eqClassIterator( subject, s, OBJ );
            }
	        else if( kb.isEquivalentClass( s, o ) ) {		            
	            iterator = singletonIterator( subject, OWL_equivalentClass, object );
	        }            
        }
        else if( predicate.equals( OWL_disjointWith ) ) {
            if( !subject.isConcrete() ) {
	            if( !object.isConcrete() )
	                iterator = new TBoxSubjObjIterator( OWL_disjointWith );             
	            else
	                iterator = disjointIterator( object, o, SUBJ );		                	            
	        }
	        else if( !object.isConcrete() ) {
	            iterator = disjointIterator( subject, s, OBJ );
            }
	        else if( kb.isDisjoint( s, o ) ) {		            
	            iterator = singletonIterator( subject, OWL_disjointWith, object );
	        }             
        }
        else if( predicate.equals( OWL_complementOf ) ) {
            if( !subject.isConcrete() ) {
	            if( !object.isConcrete() )
	                iterator = new TBoxSubjObjIterator( OWL_complementOf );             
	            else
	                iterator = complementIterator( object, o, SUBJ );		                	            
	        }
	        else if( !object.isConcrete() ) {
	            iterator = complementIterator( subject, s, OBJ );
            }
	        else if( kb.isDisjoint( s, o ) ) {		            
	            iterator = singletonIterator( subject, OWL_complementOf, object );
	        }            
        }
        
        return iterator;
    }

    protected boolean graphBaseContains( Triple pattern ) {
        if( getRawGraph().contains( pattern ) )
            return true;
        
        prepare();
        
        Node subject   = pattern.getSubject().isVariable() ? Node.ANY : pattern.getSubject();
        Node predicate = pattern.getPredicate().isVariable() ? Node.ANY : pattern.getPredicate();
        Node object    = pattern.getObject().isVariable() ? Node.ANY : pattern.getObject();
        
        Boolean contains = containedInTBox( subject, predicate, object );
        if( contains != null )
            return contains.booleanValue();
        
        contains = containedInABox( subject, predicate, object );
        if( contains != null )
            return contains.booleanValue();
        
        return false;
    }
    
    private class ABoxSubjObjIterator extends NiceIterator {
        private ATermAppl pred;
        private Node predNode;

        private ATermAppl subj;
        private Node subjNode;

        private Iterator subjects;
        private Iterator objects;
        
        public ABoxSubjObjIterator( Node predNode, ATermAppl pred ) {
            this.predNode = predNode;
            this.pred = pred;
            
            subjects = kb.retrieveIndividualsWithProperty( pred ).iterator();
            findNextSubject();
        }
        
        private void findNextSubject() {
            while( subjects.hasNext() ) {
                subj = (ATermAppl) subjects.next();
                objects = kb.getPropertyValues( pred, subj ).iterator();
                if( objects.hasNext() ) {
                    subjNode = (Node) nodeMapper.map1( subj );
                    return;
                }
            }           
            
            // if no more subjects found
            objects = NullIterator.instance;
        }

        public boolean hasNext() {
            if( objects.hasNext() )
                return true;
            
            findNextSubject();
            
            return objects.hasNext();
        }

        public Object next() {
            ensureHasNext();
            
            ATermAppl obj = (ATermAppl) objects.next();
            Node objNode = (Node) nodeMapper.map1( obj );
            
            Triple triple = new Triple( subjNode, predNode, objNode);
            
            return triple;
        }        
    }
    
    private class ABoxPredObjIterator extends NiceIterator {
        private ATermAppl subj;
        private Node subjNode;

        private ATermAppl pred;
        private Node predNode;
        
        private Iterator predicates;
        private Iterator objects;
        
        private byte which;
        
        public ABoxPredObjIterator( Node subjNode, ATermAppl subj, byte which ) {
            this.subjNode = subjNode;
            this.subj = subj;
            this.which = which;
                        
            predicates = 
                kb.getPossibleProperties( subj ).iterator();
//                kb.getProperties().iterator();
            
            findNextPredicate();
        }
                
        private void findNextPredicate() {
            while( predicates.hasNext() ) {
                pred = ((Role) predicates.next()).getName();
                if( which == OBJ )
                    objects = kb.getPropertyValues( pred, subj ).iterator();
                else
                    objects = kb.getIndividualsWithProperty( pred, subj ).iterator();
                
                if( objects.hasNext() ) {
                    predNode = (Node) nodeMapper.map1( pred ); 
                    return;
                }
            }           
            
            // if no more subjects found
            objects = NullIterator.instance;
        }

        public boolean hasNext() {
            if( objects.hasNext() )
                return true;
            
            findNextPredicate();
            
            return objects.hasNext();
        }

        public Object next() {
            ensureHasNext();

            Object obj = objects.next();
            Node objNode = (Node) nodeMapper.map1( obj );

            Triple triple = ( which == OBJ )
            	? new Triple( subjNode, predNode, objNode)
            	: new Triple( objNode, predNode, subjNode);
            
            return triple;
        }        
    }

    private class ABoxSubjPredObjIterator extends NiceIterator {
        public final static byte TYPES = 0;
        public final static byte DIRECT_TYPES = 1;
        public final static byte PROPS = 2;
                
        private Iterator subjects;
        private ATermAppl subj;
        private Node subjNode;
        
        private Iterator iterator;
        
        private byte which;
        
        public ABoxSubjPredObjIterator( byte which ) {
            this.which = which;
            
            subjects = kb.getIndividuals().iterator();
            
            findNextSubject();
        }
        
        private void findNextSubject() {
            while( subjects.hasNext() ) {
                subj = (ATermAppl) subjects.next();
                subjNode = (Node) nodeMapper.map1( subj );     
                switch(which) {
                	case TYPES:
                	    iterator = typeIterator( subjNode, subj ); break;
                	case DIRECT_TYPES:
                	    iterator = directTypeIterator( subjNode, subj ); break;
                	case PROPS:    
                	    iterator = new ABoxPredObjIterator( subjNode, subj, OBJ ); break;
                	default:
                		throw new InternalReasonerException( "Invalid iterator" );
                }
                              	
                if( iterator.hasNext() ) 
                    return;                
            }           
            
            // if no more subjects found
            iterator = NullIterator.instance;
        }

        public boolean hasNext() {
            if( iterator.hasNext() )
                return true;
            
            findNextSubject();
            
            return iterator.hasNext();
        }

        public Object next() {
            ensureHasNext();
            
            return iterator.next();
        }        
    }
    
    private class TBoxSubjObjIterator extends NiceIterator {
        private Node predNode;
    
        private ATermAppl subj;
        private Node subjNode;
    
        private Iterator subjects;
        private Iterator objects;
        
        public TBoxSubjObjIterator( Node predNode ) {
            this.predNode = predNode;
            
            subjects = kb.getAllClasses().iterator();
            findNextSubject();
        }
        
        private void findNextSubject() {
            while( subjects.hasNext() ) {
                subj = (ATermAppl) subjects.next();
                subjNode = (Node) nodeMapper.map1( subj );
                if( predNode == null ) {
                    objects =
                        eqClassIterator( subjNode, subj, OBJ ).andThen(
                        superClassIterator( subjNode, subj, false ).andThen(
                        disjointIterator( subjNode, subj, OBJ ).andThen(
                        complementIterator( subjNode, subj, OBJ ) ) ) );
                }
                else if( predNode.equals( RDFS_subClassOf ) )
                    objects = superClassIterator( subjNode, subj, false );
                else if( predNode.equals( RDFS_directSubClassOf ) )
                    objects = superClassIterator( subjNode, subj, true );
                else if( predNode.equals( OWL_equivalentClass ) )
                    objects = eqClassIterator( subjNode, subj, OBJ );
                else if( predNode.equals( OWL_disjointWith ) )
                    objects = disjointIterator( subjNode, subj, OBJ );
                else if( predNode.equals( OWL_complementOf ) )
                    objects = complementIterator( subjNode, subj, OBJ );
                else 
                    throw new InternalReasonerException( "Invalid TBox predicate " + predNode ); 
                
                if( objects.hasNext() ) 
                    return;  
            }           
            
            objects = NullIterator.instance;
        }
    
        public boolean hasNext() {
            if( objects.hasNext() )
                return true;
            
            findNextSubject();
            
            return objects.hasNext();
        }
    
        public Object next() {
            ensureHasNext();
            
            return objects.next();
        }
    }    
     
    private SingletonIterator singletonIterator( Node s, Node p, Node o ) {
        return new SingletonIterator( new Triple( s, p , o ) );
    }
    
    private ExtendedIterator instanceIterator( Node objNode, ATermAppl obj ) {
        return WrappedIterator.create( 
            kb.getInstances( obj ).iterator() ).
			mapWith( nodeMapper ).
			mapWith( TripleFiller.fillSubj( RDF_type, objNode ) );         
    }

    private ExtendedIterator directInstanceIterator( Node objNode, ATermAppl obj ) {
        return WrappedIterator.create( 
            kb.getInstances( obj, true ).iterator() ).
			mapWith( nodeMapper ).
			mapWith( TripleFiller.fillSubj( RDF_directType, objNode ) );         
    }
    
    private ExtendedIterator typeIterator( Node subjNode, ATermAppl subj ) {
        return WrappedIterator.create( new CollectionsIterator( 
            kb.getTypes( subj ) ) ).
			mapWith( nodeMapper ).
			mapWith( TripleFiller.fillObj( subjNode, RDF_type ) );         
    }
    
    private ExtendedIterator directTypeIterator( Node subjNode, ATermAppl subj ) {
        return WrappedIterator.create( new CollectionsIterator( 
            kb.getTypes( subj, true ) ) ).
			mapWith( nodeMapper ).
			mapWith( TripleFiller.fillObj( subjNode, RDF_directType ) );         
    }

    private ExtendedIterator propertyIterator( Node subjNode, ATermAppl subj, Node objNode, ATermAppl obj ) {
        return WrappedIterator.create( 
            kb.getProperties( subj, obj ).iterator() ).
			mapWith( nodeMapper ).
			mapWith( TripleFiller.fillPred( subjNode, objNode ) );         
    }
    
    private ExtendedIterator subjectIterator( Node objNode, ATermAppl obj, Node predNode, ATermAppl pred ) {
        return WrappedIterator.create( 
            kb.getIndividualsWithProperty( pred, obj ).iterator() ).
			mapWith( nodeMapper ).
			mapWith( TripleFiller.fillSubj( objNode, predNode ) );         
    }  
    
    private ExtendedIterator objectIterator( Node subjNode, ATermAppl subj, Node predNode, ATermAppl pred ) {
        return WrappedIterator.create( 
            kb.getPropertyValues( pred, subj ).iterator() ).
			mapWith( nodeMapper ).
			mapWith( TripleFiller.fillObj( subjNode, predNode ) );         
    }    

    private ExtendedIterator typeClassIterator( Node clsNode ) {
        return singletonIterator( clsNode, RDF_type, OWL_Class ); 
    } 

    private ExtendedIterator eqClassIterator( Node clsNode, ATermAppl cls, byte which ) {
        return WrappedIterator.create( 
            kb.getAllEquivalentClasses( cls ).iterator() ).
			mapWith( nodeMapper ).
			mapWith( TripleFiller.fillSubjObj( OWL_equivalentClass, clsNode, which ) );         
    } 
    
    private ExtendedIterator subClassIterator( Node clsNode, ATermAppl cls, boolean direct ) {
        ExtendedIterator iterator = WrappedIterator.create(  new CollectionsIterator( 
            kb.getSubClasses( cls, direct ) ) );
        
        if( !direct )
            iterator = iterator.andThen( WrappedIterator.create(
                kb.getAllEquivalentClasses( cls ).iterator() ) );
        
        return iterator.
			mapWith( nodeMapper ).
			mapWith( TripleFiller.fillSubj( RDFS_subClassOf, clsNode ) );      
    } 
    
    private ExtendedIterator superClassIterator( Node clsNode, ATermAppl cls, boolean direct ) {
        ExtendedIterator iterator = WrappedIterator.create(  new CollectionsIterator( 
            kb.getSuperClasses( cls, direct ) ) );
        
        if( !direct )
            iterator = iterator.andThen( WrappedIterator.create(
                kb.getAllEquivalentClasses( cls ).iterator() ) );
        
        return iterator.
			mapWith( nodeMapper ).
			mapWith( TripleFiller.fillObj( clsNode, RDFS_subClassOf ) );      
    } 
    
    private ExtendedIterator disjointIterator( Node clsNode, ATermAppl cls, byte which ) {
        return WrappedIterator.create( new CollectionsIterator( 
            kb.getDisjoints( cls ) ) ).
			mapWith( nodeMapper ).
			mapWith( TripleFiller.fillSubjObj( OWL_disjointWith, clsNode, which ) );          
    }     
    
    private ExtendedIterator complementIterator( Node clsNode, ATermAppl cls, byte which ) {
        return WrappedIterator.create( 
            kb.getComplements( cls ).iterator() ).
			mapWith( nodeMapper ).
			mapWith( TripleFiller.fillSubjObj( OWL_complementOf, clsNode, which ) );          
    }       
    
    private ExtendedIterator sameAsIterator(Node instanceNode, ATermAppl instance, byte which) {
    		return WrappedIterator.create(
    			kb.getAllSames( instance ).iterator()).
				mapWith( nodeMapper).
				mapWith( TripleFiller.fillSubjObj( OWL.sameAs.asNode(), instanceNode, which));
    }
    
    private Boolean containedInABox( Node subject, Node predicate, Node object ) { 
        if( !predicate.isURI() ) 
            return null;

        ATermAppl s = node2term( subject );
        ATermAppl p = node2term( predicate );
        ATermAppl o = node2term( object );

        if( subject.isConcrete() && !kb.isIndividual( s ) )
            return null;
        
        String predURI = predicate.getURI();                      
        if( predURI.equals( RDF.type.getURI() )) {
            if( object.isConcrete() && !kb.isClass( o ) )
                return null;
                        
            if( subject.isConcrete() ) {
                if( object.isConcrete() )
                    return kb.isType( s, o ) ? Boolean.TRUE : Boolean.FALSE;
                else
                    return Boolean.TRUE; // every individual belongs to at least owl:Thing
            }
            else if( object.isConcrete() ) {
                return kb.hasInstance( o ) ? Boolean.TRUE : Boolean.FALSE;
            }
            else
                return Boolean.TRUE;
        }
        
        if( object.isConcrete() && !kb.isIndividual( o ) && !ATermUtils.isLiteral( o ) )
            return null;
        
        if( predURI.equals( OWL.sameAs.getURI() )) {
            if( subject.isConcrete() && object.isConcrete() ) { 
                return kb.isSameAs( s, o ) ? Boolean.TRUE : Boolean.FALSE;
            }

            // every individual is sameAs itself
	        return Boolean.TRUE;
        }
        else if( predURI.equals( OWL.differentFrom.getURI() )) {
            if( subject.isConcrete() && object.isConcrete() ) { 
	            return kb.isDifferentFrom( s, o ) ? Boolean.TRUE : Boolean.FALSE;
            }
            
            // FIXME what is the correct value to return?
            return Boolean.TRUE;        
        }
       
        Role r = kb.getProperty( p );
        if( r == null || r.isAnnotationRole() || r.isOntologyRole())
            return null;
        
        if( !subject.isConcrete() && !object.isConcrete() ) {
            Collection candidates = kb.retrieveIndividualsWithProperty( p );
            return !candidates.isEmpty() ?  Boolean.TRUE : Boolean.FALSE;          
        }
        else if( !subject.isConcrete() ) {
            if( r.isDatatypeRole() )
                return Boolean.FALSE;
            ATermAppl invP = r.getInverse().getName();
            return kb.hasPropertyValue( o, invP, null ) ? Boolean.TRUE : Boolean.FALSE;
        }
        else if( !object.isConcrete() ) {
            return kb.hasPropertyValue( s, p, null ) ? Boolean.TRUE : Boolean.FALSE;
        }
        else {
            return kb.hasPropertyValue( s, p, o ) ? Boolean.TRUE : Boolean.FALSE;
        }
    }            
    
    private Boolean containedInTBox(Node subject, Node predicate, Node object) {    
        if( !predicate.isURI() ) 
            return null;

        ATermAppl s = node2term( subject );
        ATermAppl o = node2term( object );
        
//        if( subject.isConcrete() && (kb.isClass( s ) || !kb.isProperty(s)))
//            return null;        

        String predURI = predicate.getURI(); 
		if(predURI.equals(RDF.type.getURI())) {
            if( !object.isConcrete() ) {
                return Boolean.TRUE;    
            }
			else if(object.equals(OWL.Class.asNode()) || object.equals(RDFS.Class.asNode())) {
			    if( !subject.isConcrete() )
			        return !kb.getClasses().isEmpty() ? Boolean.TRUE : Boolean.FALSE;
			    else
			        return kb.isClass(s) ? Boolean.TRUE : Boolean.FALSE;
			}
			else if(object.equals(RDF.Property.asNode())) {
			    if( !subject.isConcrete() )
			        return !kb.getProperties().isEmpty() ? Boolean.TRUE : Boolean.FALSE;
			    else
			        return kb.isProperty(s) ? Boolean.TRUE : Boolean.FALSE;
			}
			else if(object.equals(OWL.ObjectProperty.asNode())) {
			    if( !subject.isConcrete() )
			        return !kb.getObjectProperties().isEmpty() ? Boolean.TRUE : Boolean.FALSE;
			    else
			        return kb.isObjectProperty(s) ? Boolean.TRUE : Boolean.FALSE;
			}
			else if(object.equals(OWL.DatatypeProperty.asNode())) {
			    if( !subject.isConcrete() )
			        return !kb.getDataProperties().isEmpty() ? Boolean.TRUE : Boolean.FALSE;
			    else
			        return kb.isDatatypeProperty(s) ? Boolean.TRUE : Boolean.FALSE;
			}
			else if(object.equals(OWL.AnnotationProperty.asNode())) {
			    if( !subject.isConcrete() )
			        return Boolean.TRUE;
			    else
			        return kb.isAnnotationProperty(s) ? Boolean.TRUE : Boolean.FALSE;
			}
			else if(object.equals(OWL.TransitiveProperty.asNode())) {
			    if( !subject.isConcrete() )
			        return !kb.getTransitiveProperties().isEmpty() ? Boolean.TRUE : Boolean.FALSE;
			    else
			        return kb.isTransitiveProperty(s) ? Boolean.TRUE : Boolean.FALSE;
			}
			else if(object.equals(OWL.SymmetricProperty.asNode())) {
			    if( !subject.isConcrete() )
			        return !kb.getSymmetricProperties().isEmpty() ? Boolean.TRUE : Boolean.FALSE;
			    else
			        return kb.isSymmetricProperty(s) ? Boolean.TRUE : Boolean.FALSE;
			}
			else if(object.equals(OWL.FunctionalProperty.asNode())) {
			    if( !subject.isConcrete() )
			        return !kb.getFunctionalProperties().isEmpty() ? Boolean.TRUE : Boolean.FALSE;
			    else
			        return kb.isFunctionalProperty(s) ? Boolean.TRUE : Boolean.FALSE;
			}
			else if(object.equals(OWL.InverseFunctionalProperty.asNode())) {
			    if( !subject.isConcrete() )
			        return !kb.getInverseFunctionalProperties().isEmpty() ? Boolean.TRUE : Boolean.FALSE;
			    else
			        return kb.isInverseFunctionalProperty(s) ? Boolean.TRUE : Boolean.FALSE;
			}
			else {
				return null;
			}
		}

		if(predURI.equals(RDFS.subClassOf.getURI())) {
		    if( !subject.isConcrete() ) {
			    if( !object.isConcrete() )
			        return Boolean.TRUE;
			    else
			        return kb.isClass(o) ? Boolean.TRUE : Boolean.FALSE;
		    }
		    else if( !object.isConcrete() )
		        return kb.isClass(s) ? Boolean.TRUE : Boolean.FALSE;
		    else
		        return kb.isSubClassOf(s, o) ? Boolean.TRUE : Boolean.FALSE;
		}
		else if(predURI.equals(OWL.complementOf.getURI())) {
		    if( !subject.isConcrete() ) {
			    if( !object.isConcrete() )
			        return Boolean.TRUE;
			    else
			        return !kb.getComplements(o).isEmpty() ? Boolean.TRUE : Boolean.FALSE;
		    }
		    else if( !object.isConcrete() )
		        return Boolean.TRUE;
		    else
		        return kb.isComplement(s, o) ? Boolean.TRUE : Boolean.FALSE;
		}
		else if(predURI.equals(OWL.equivalentClass.getURI())) {
		    if( !subject.isConcrete() ) {
			    if( !object.isConcrete() )
			        return Boolean.TRUE;
			    else
			        return kb.isClass(o) ? Boolean.TRUE : Boolean.FALSE;
		    }
		    else if( !object.isConcrete() )
		        return kb.isClass(s) ? Boolean.TRUE : Boolean.FALSE;
		    else
		        return kb.isEquivalentClass(s, o) ? Boolean.TRUE : Boolean.FALSE;
		}
		else if(predURI.equals(OWL.disjointWith.getURI())) {
		    if( !subject.isConcrete() ) {
			    if( !object.isConcrete() )
			        return Boolean.TRUE;
			    else
			        return !kb.getDisjoints(o).isEmpty() ? Boolean.TRUE : Boolean.FALSE;
		    }
		    else if( !object.isConcrete() )
		        return Boolean.TRUE;
		    else
		        return kb.isDisjoint(s, o) ? Boolean.TRUE : Boolean.FALSE;
		}
		else if(predURI.equals(OWL.equivalentProperty.getURI())) {
		    if( !subject.isConcrete() ) {
			    if( !object.isConcrete() )
			        return Boolean.TRUE;
			    else
			        return kb.isProperty(o) ? Boolean.TRUE : Boolean.FALSE;
		    }
		    else if( !object.isConcrete() )
		        return kb.isProperty(s) ? Boolean.TRUE : Boolean.FALSE;
		    else
		        return kb.isEquivalentProperty(s, o) ? Boolean.TRUE : Boolean.FALSE;
		}
		else if(predURI.equals(RDFS.subPropertyOf.getURI())) {
		    if( !subject.isConcrete() ) {
			    if( !object.isConcrete() )
			        return Boolean.TRUE;
			    else
			        return kb.isProperty(o) ? Boolean.TRUE : Boolean.FALSE;
		    }
		    else if( !object.isConcrete() )
		        return kb.isProperty(s) ? Boolean.TRUE : Boolean.FALSE;
		    else
		        return kb.isSubPropertyOf(s, o) ? Boolean.TRUE : Boolean.FALSE;
		}
		else if(predURI.equals(OWL.inverseOf.getURI())) {
		    if( !subject.isConcrete() ) {
			    if( !object.isConcrete() )
			        return Boolean.TRUE;
			    else
			        return !kb.getInverses(o).isEmpty() ? Boolean.TRUE : Boolean.FALSE;
		    }
		    else if( !object.isConcrete() )
		        return !kb.getInverses(s).isEmpty() ? Boolean.TRUE : Boolean.FALSE;
		    else
		        return kb.isInverse(s, o) ? Boolean.TRUE : Boolean.FALSE;
		}
		else if(predURI.equals(RDFS.domain.getURI())) {
		    if( !subject.isConcrete() ) {
			    if( !object.isConcrete() )
			        return Boolean.TRUE;
			    else
			        return kb.isClass(o) ? Boolean.TRUE : Boolean.FALSE;
		    }
		    else if( !object.isConcrete() )
		        return !kb.getDomains(s).isEmpty() ? Boolean.TRUE : Boolean.FALSE;
		    else
		        return kb.hasDomain(s, o) ? Boolean.TRUE : Boolean.FALSE;
		}
		else if(predURI.equals(RDFS.range.getURI())) {
		    if( !subject.isConcrete() ) {
			    if( !object.isConcrete() )
			        return Boolean.TRUE;
			    else
			        return (kb.isClass(o) || kb.isDatatype(o)) ? Boolean.TRUE : Boolean.FALSE;
		    }
		    else if( !object.isConcrete() )
		        return !kb.getRanges(s).isEmpty() ? Boolean.TRUE : Boolean.FALSE;
		    else
		        return kb.hasRange(s, o) ? Boolean.TRUE : Boolean.FALSE;
		}
        
		return null;
    }
    
    /**
     * Returns the underlying Pellet OWLReasoner.
     */
    public OWLReasoner getOWLReasoner() {
        return reasoner;
    }
    
    /**
     * Returns the underlying Pellet KnowledgeBase.
     */
    public KnowledgeBase getKB() {
        return kb;
    }
    
    /**
     * <p>Add one triple to the data graph, mark the graph not-prepared,
     * but don't run prepare() just yet.</p>
     * @param t A triple to add to the graph
     */
    public void performAdd(Triple t) {
        fdata.getGraph().add(t);
        isPrepared = false;
    }

    /**
     * <p>Delete one triple from the data graph, mark the graph not-prepared,
     * but don't run prepare() just yet.</p>
     * @param t A triple to remove from the graph
     */
    public void performDelete(Triple t) {
        fdata.getGraph().delete(t);
        isPrepared = false;
    }
    
    /**
     * <p>Test the consistency of the model. This looks for overall inconsistency,
     * and for any unsatisfiable classes.</p>
     * @return a ValidityReport structure
     */
    public ValidityReport validate() {
        checkOpen();
        prepare();
        StandardValidityReport report = new StandardValidityReport();

        kb.setDoExplanation(true);
        boolean consistent = reasoner.isConsistent();
        kb.setDoExplanation(false);
        
        if( !consistent ) {            
            report.add( true, "KB is inconsistent!", kb.getExplanation() );
        }
        else {       
	        Iterator i = kb.getClasses().iterator();
	        while(i.hasNext()) {
	            ATermAppl c = (ATermAppl) i.next();
		        if(!kb.isSatisfiable(c)) {
		            String name = nodeMapper.map1( c ).toString();
		            report.add( false, "Unsatisfiable class", name );	            
		        }
	        }
        }
        
        return report;
    }
    
    public ATermAppl node2term( Node node ) {
        if( node == Node.ANY )
            return null;
        
        return reasoner.getLoader().node2term( node );
    }
    
    public ATermAppl node2term( RDFNode node ) {
        return node2term( node.asNode() );
    }    
}
