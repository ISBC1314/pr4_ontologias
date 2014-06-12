//The MIT License
//
//Copyright (c) 2003 Ron Alford, Mike Grove, Bijan Parsia, Evren Sirin
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to
//deal in the Software without restriction, including without limitation the
//rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
//sell copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in
//all copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
//FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
//IN THE SOFTWARE.

/*
 * Created on May 5, 2004
 */
package org.mindswap.pellet;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.datatypes.Datatype;
import org.mindswap.pellet.datatypes.DatatypeReasoner;
import org.mindswap.pellet.exceptions.InconsistentOntologyException;
import org.mindswap.pellet.exceptions.InternalReasonerException;
import org.mindswap.pellet.exceptions.UnsupportedFeatureException;
import org.mindswap.pellet.output.OutputFormatter;
import org.mindswap.pellet.query.QueryEngine;
import org.mindswap.pellet.query.QueryResults;
import org.mindswap.pellet.taxonomy.Taxonomy;
import org.mindswap.pellet.taxonomy.TaxonomyBuilder;
import org.mindswap.pellet.tbox.TBox;
import org.mindswap.pellet.tbox.impl.TBoxImpl;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Bool;
import org.mindswap.pellet.utils.SetUtils;
import org.mindswap.pellet.utils.SizeEstimate;
import org.mindswap.pellet.utils.Timer;
import org.mindswap.pellet.utils.Timers;

import aterm.ATerm;
import aterm.ATermAppl;
import aterm.ATermList;

import com.hp.hpl.jena.query.Syntax;

/**
 * @author Evren Sirin
 */
public class KnowledgeBase {
    protected static Log log = LogFactory.getLog( KRSSLoader.class );

    /**
     * @deprecated Edit log4j.properties to turn on debugging
     */
	public static boolean DEBUG = false;
	
	protected ABox abox;
	protected TBox tbox;
	protected RBox rbox;	
	
	private Set individuals;
	
	protected TaxonomyBuilder builder;
	protected Taxonomy taxonomy;
	protected Taxonomy roleTaxonomy;

	private boolean consistent;
    
    private SizeEstimate estimate;

	protected int status;

  	protected static final int UNCHANGED      = 0x0000;
	protected static final int ABOX_CHANGED   = 0x0001;
	protected static final int TBOX_CHANGED   = 0x0002;
	protected static final int RBOX_CHANGED   = 0x0004;
	protected static final int ALL_CHANGED    = 0x0007;
	protected static final int CONSISTENCY    = 0x0008;
	protected static final int CLASSIFICATION = 0x0010;
	protected static final int REALIZATION    = 0x0020;
		
	/**
	 * The URI of the ontology this KB belongs to. Note that most of the time
	 * this value will be null because the KB will not know the exact URI of the
	 * ontology.
	 */
	protected String ontology;
	
	Map instances;
//	private Map typeChecks;

	private Expressivity expressivity;

	/**
	 * Timers used in various different parts fo KB. There may be many different timers
	 * created here depending on the level of debugging or application requirements.
	 * However, there are three major timers that are guaranteed to exist.
	 * <ul>
	 * 	<li> <b>main</b> - This is the main timer that exists in any Timers objects. All
	 * the other timers defined in here will have this timer as its dependant so setting
	 * a timeout on this timer will put a limit on every operation done inside KB.</li>
	 * 	<li> <b>preprocessing</b> - This is the operation where TBox creation, absorbtion and
	 * normalization is done. It alos includes computing hierarchy of properties in RBox and 
	 * merging the individuals in Abox if there are explicit sameAs assertions.</li>
	 * 	<li> <b>consistency</b> - This is the timer for ABox consistency check. Putting a 
	 * timeout will mean that any single consistency check should be completed in a certain
	 * amount of time.</li>
	 *</ul>
	 */
	public Timers timers = new Timers();

	private Set rules;	
	
	/**
	 * 
	 */
	public KnowledgeBase() {
		clear();
		
		timers.createTimer("preprocessing");
		timers.createTimer("consistency");
		
		status = ALL_CHANGED;
	}
	
	/**
	 * Create a KB based on an existing one. New KB has a copy of the ABox 
	 * but TBox and RBox is shared between two. 
	 * 
	 * @param kb
	 */
	public KnowledgeBase(KnowledgeBase kb) {
		abox = kb.abox.copy();
//		abox.pseudoModel = kb.abox.pseudoModel.copy();
		tbox = kb.tbox;
		rbox = kb.rbox;
		
		expressivity = kb.expressivity;
		individuals = new HashSet( kb.individuals );
		
		instances = new HashMap( kb.instances );
		
		status = ALL_CHANGED;
		
		timers = kb.timers;
//		timers.createTimer("preprocessing");
//		timers.createTimer("consistency");
	}
	
	public Expressivity getExpressivity() {
	    prepare();
	    
	    return expressivity;
	}

	public void clear() {
		abox = new ABox(this);
		tbox = new TBoxImpl(this);
		rbox = new RBox();		
		
		expressivity = new Expressivity(this);
		individuals = new HashSet();
		
		instances = new HashMap();
//		typeChecks = new HashMap();
		
		builder = null;
		
		status = ALL_CHANGED;		
	}
	
	public KnowledgeBase copy() {
	    return new KnowledgeBase(this); 	
	}
	
	public void loadKRSS( Reader reader ) throws IOException {
	    KRSSLoader loader = new KRSSLoader();
	    loader.load( reader, this );
	}
	
	public void addClass( ATermAppl c ) {
		if(c.equals(ATermUtils.TOP) || ATermUtils.isComplexClass(c))
			return;
		    
		status |= TBOX_CHANGED;
		tbox.addClass( c );
		
		if( log.isDebugEnabled() ) log.debug("class " + c);
	}
	
	public void addSubClass(ATermAppl c1, ATermAppl c2) {
		if(c1.equals(c2)) return;
		
		status |= TBOX_CHANGED;
		
		if( ATermUtils.isOneOf( c1 ) ) {
            if( !PelletOptions.USE_PSEUDO_NOMINALS ) {
                if( PelletOptions.USE_NOMINAL_ABSORPTION ) {
        			ATermList list = (ATermList) c1.getArgument(0);
        			while(!list.isEmpty()) {
        				ATermAppl nominal = (ATermAppl) list.getFirst();
        				ATermAppl ind = (ATermAppl) nominal.getArgument(0);
        				addIndividual(ind);
        				addType(ind, c2);
        				list = list.getNext();
        			}
                }
            }
		}
		else {
		    ATermAppl subAxiom = ATermUtils.makeSub( c1, c2 );
			tbox.addAxiom(subAxiom);
			if( log.isDebugEnabled() ) log.debug("sub " + c1 + " " + c2);
		}
	}

    /**
     * @deprecated Use {@link #addEquivalentClass(ATermAppl, ATermAppl)} instead 
     */
    public void addSameClass(ATermAppl c1, ATermAppl c2) {
        addEquivalentClass( c1, c2 );
    }    
    
	public void addEquivalentClass(ATermAppl c1, ATermAppl c2) {
		if(c1.equals(c2)) return;
		
		status |= TBOX_CHANGED;
		
		if( PelletOptions.USE_NOMINAL_ABSORPTION && (ATermUtils.isOneOf(c2) || ATermUtils.isOneOf(c1))) {
			addSubClass(c1, c2);
			addSubClass(c2, c1);
		}
		else {
		    ATermAppl sameAxiom = ATermUtils.makeSame(c1, c2);
			tbox.addAxiom(sameAxiom);
			if( log.isDebugEnabled() ) log.debug("same " + c1 + " " + c2);
		}			
	}

	public void addDisjointClass(ATerm c1, ATerm c2) {
		status |= TBOX_CHANGED;		
		ATerm notC2 = ATermUtils.makeNot(c2);
		ATerm notC1 = ATermUtils.makeNot(c1);
		tbox.addAxiom(ATermUtils.makeSub(c1, notC2));
		tbox.addAxiom(ATermUtils.makeSub(c2, notC1));
		if( log.isDebugEnabled() ) log.debug("disjoint " + c1 + " " + c2);		
	}

	public void addComplementClass(ATerm c1, ATerm c2) {
		status |= TBOX_CHANGED;		
		ATerm notC2 = ATermUtils.makeNot(c2);
		
		if( c1.equals( notC2 ) )
		    return;
		
		ATerm notC1 = ATermUtils.makeNot(c1);
		tbox.addAxiom(ATermUtils.makeSame(c1, notC2));
		tbox.addAxiom(ATermUtils.makeSame(c2, notC1));
		if( log.isDebugEnabled() ) log.debug("complement " + c1 + " " + c2);		
	}
	
	/**
	 * 
	 * Add the value of a DatatypeProperty. 
	 * 
	 * @param p Datatype Property
	 * @param ind Individual value being added to
	 * @param literalValue A literal ATerm which should be constructed with one of
	 * ATermUtils.makeXXXLiteral functions
	 * 
	 * @deprecated Use addPropertyValue instead
	 */
	public void addDataPropertyValue(ATermAppl p, ATermAppl s, ATermAppl o) {	
		addPropertyValue( p, s, o );
	}
	
	public Individual addIndividual(ATermAppl i) {
	    Node node = abox.getNode(i);
		if(node == null) {
			status |= ABOX_CHANGED;			
			node = abox.addIndividual(i);
			node.setOntology(ontology);
			individuals.add(i);
			if( log.isDebugEnabled() ) log.debug("individual " + i);			
		}
		else if(node instanceof Literal)
		    throw new UnsupportedFeatureException("Trying to use a literal as an individual. Literal ID: " + i.getName());
		
		return (Individual) node;
	}

	public void addType(ATermAppl i, ATermAppl c) {
		status |= ABOX_CHANGED;
		abox.addType(i, c);
		if( log.isDebugEnabled() ) log.debug("type " + i + " " + c);		
	}
	
	public void addSame(ATermAppl i1, ATermAppl i2) {
		status |= ABOX_CHANGED;
		abox.addSame(i1, i2);
		if( log.isDebugEnabled() ) log.debug("same " + i1 + " " + i2);
	}
	
	public void addDifferent(ATermAppl i1, ATermAppl i2) {
		status |= ABOX_CHANGED;
		abox.addDifferent(i1, i2);
		if( log.isDebugEnabled() ) log.debug("diff " + i1 + " " + i2);
	}
	
	/**
	 * @deprecated Use addPropertyValue instead
	 */
	public void addObjectPropertyValue(ATermAppl p, ATermAppl s, ATermAppl o) {
		addPropertyValue( p, s, o );
	}

	public boolean addPropertyValue(ATermAppl p, ATermAppl s, ATermAppl o) {
		status |= ABOX_CHANGED;
		
		Individual subj = abox.getIndividual(s);
		Role role = getRole(p);
		Node obj = null;

		if( subj == null ) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return false;
		    else		    
		        throw new UnsupportedFeatureException( s + " is not a known individual!" ); 
		}
		
		if( role == null ) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return false;
		    else		    
		        throw new UnsupportedFeatureException( p + " is not a known property!" ); 
		}
		
		if(role.isObjectRole()) {
		    obj = abox.getIndividual( o );
			if( obj == null ) {
			    if( ATermUtils.isLiteral( o ) ) {
			        System.err.println( 
			            "Ignoring literal value " + o + " for object property " + p );
			        return false;
			    }
			    else {
				    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
				        return false;
				    else			        
				        throw new UnsupportedFeatureException( o + " is not a known individual!" );
			    }
			}
		}
		else if(role.isDatatypeRole()) {
		    obj = abox.addLiteral( o );
		}

		subj.addEdge(role, obj, DependencySet.INDEPENDENT);
		
		if( log.isDebugEnabled() ) log.debug("prop-value " + s + " " + p + " " + o);	
		
		return true;
	}
	
	public void addProperty(ATermAppl p) {
		status |= RBOX_CHANGED;
		rbox.addRole(p);
		if( log.isDebugEnabled() ) log.debug("undefined-prop " + p);
	}
	
	/**
	 * Add a new object property. If property was earlier defined to be a datatype 
	 * property then this function will simply return without changing the KB.
	 * 
	 * @param p Name of the property
	 * @return True if property is added, false if not
	 */
	public boolean addObjectProperty(ATerm p) {
		status |= RBOX_CHANGED;
		Role role = rbox.addObjectRole((ATermAppl) p);
		if( log.isDebugEnabled() ) log.debug("object-prop " + p);
		
		return role.isObjectRole();
	}

	/**
	 * Add a new object property. If property was earlier defined to be a datatype 
	 * property then this function will simply return without changing the KB.
	 * 
	 * @param p
	 * @return True if property is added, false if not
	 */
	public boolean addDatatypeProperty(ATerm p) {
		status |= RBOX_CHANGED;
		Role role = rbox.addDatatypeRole((ATermAppl) p);
		if( log.isDebugEnabled() ) log.debug("data-prop " + p);
		
		return role.isDatatypeRole();
	}

	public void addOntologyProperty(ATermAppl p) {
		status |= RBOX_CHANGED;
		rbox.addOntologyRole(p);
		if( log.isDebugEnabled() ) log.debug("onto-prop " + p);
	}
	
	public void addAnnotationProperty(ATermAppl p) {
		status |= RBOX_CHANGED;
		rbox.addAnnotationRole(p);
		if( log.isDebugEnabled() ) log.debug("annotation-prop " + p);
	}
	
	public void addSubProperty(ATermAppl p1, ATermAppl p2) {
		status |= RBOX_CHANGED;
		rbox.addSubRole(p1, p2);

		if( log.isDebugEnabled() ) log.debug("sub-prop " + p1 + " " + p2);		
	}
	
    /**
     * @deprecated Use {@link #addEquivalentClass(ATermAppl, ATermAppl)} instead 
     */
    public void addSameProperty(ATermAppl p1, ATermAppl p2) {
        addEquivalentProperty( p1, p2 );
    }    
    
    public void addEquivalentProperty(ATermAppl p1, ATermAppl p2) {
		status |= RBOX_CHANGED;
		rbox.addSubRole(p1, p2);
		rbox.addSubRole(p2, p1);
		
		if( log.isDebugEnabled() ) log.debug("same-prop " + p1 + " " + p2);
	}

	public void addInverseProperty(ATermAppl p1, ATermAppl p2) {
		status |= RBOX_CHANGED;
		rbox.addInverseRole(p1, p2);
		if( log.isDebugEnabled() ) log.debug("inv-prop " + p1 + " " + p2);
	}
	
	public void addTransitiveProperty(ATermAppl p) {
		status |= RBOX_CHANGED;

		Role r = rbox.getDefinedRole(p);
		
		r.setTransitive(true);
		if( log.isDebugEnabled() ) log.debug("trans-prop " + p);
	}
	
	public void addSymmetricProperty(ATermAppl p) {
		status |= RBOX_CHANGED;
		rbox.addInverseRole(p, p);
		if( log.isDebugEnabled() ) log.debug("sym-prop " + p);
	}

	public void addFunctionalProperty(ATermAppl p) {
		status |= RBOX_CHANGED;
		Role r = rbox.getDefinedRole(p);
		
		r.setFunctional(true);
		if( log.isDebugEnabled() ) log.debug("func-prop " + p);
	}
	
	public void addInverseFunctionalProperty(ATerm p) {
		status |= RBOX_CHANGED;
		
		Role role = rbox.getDefinedRole(p);
		
	    role.setInverseFunctional(true);
		if( log.isDebugEnabled() ) log.debug("inv-func-prop " + p);
	}
	
	public void addDomain(ATerm p, ATermAppl c) {
		status |= RBOX_CHANGED;
		
		Role r = rbox.getDefinedRole(p);
		
		r.addDomain(c);
		
		if( log.isDebugEnabled() ) log.debug("domain " + p + " " + c + " (" + r.getDomain() +")");
	}
	
	public void addRange(ATerm p, ATermAppl c) {
		status |= RBOX_CHANGED;
		
		Role r = rbox.getDefinedRole(p);
		
		r.addRange(c);
		
		if( log.isDebugEnabled() ) log.debug("range " + p + " " + c);
	}
	
	public boolean isDatatype(ATerm p) {
		DatatypeReasoner dtReasoner = getDatatypeReasoner();
		return dtReasoner.isDefined( p.toString() );	    
	}	
		
	public void addDatatype(ATerm p) {
		DatatypeReasoner dtReasoner = getDatatypeReasoner();
		if(!dtReasoner.isDefined(p.toString())) {
			status |= TBOX_CHANGED;
			
			dtReasoner.defineUnknownDatatype( p.toString() );
			if( log.isDebugEnabled() ) log.debug("datatype " + p);
		}
	}

	public void loadDatatype(ATerm p) {
		DatatypeReasoner dtReasoner = getDatatypeReasoner();
		if(!dtReasoner.isDefined(p.toString())) {
			status |= TBOX_CHANGED;
			
			dtReasoner.loadUserDefinedDatatype( p.toString() );
			if( log.isDebugEnabled() ) log.debug("datatype " + p);
		}
	}
	
	public void addDataRange(String datatypeURI, ATermList values) {
		DatatypeReasoner dtReasoner = getDatatypeReasoner();
		if(!dtReasoner.isDefined(datatypeURI.toString())) {
			status |= TBOX_CHANGED;
			
			Datatype dataRange = dtReasoner.enumeration(ATermUtils.listToSet(values));
			getDatatypeReasoner().defineDatatype(datatypeURI.toString(), dataRange);
			if( log.isDebugEnabled() ) log.debug("datarange " + datatypeURI.toString() + " " + values);
		}
	}
	
	public boolean removeObjectPropertyValue(ATermAppl p, ATermAppl i1, ATermAppl i2) {
	    boolean removed = false;
		
		Individual subj = abox.getIndividual(i1);
		Individual obj = abox.getIndividual(i2);
		Role role = getRole(p);

		if(subj == null) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        throw new UnsupportedFeatureException(i1 + " is not an individual!");
		    else
		        return false;
		}
			
		if(obj == null) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return false;
		    else
		        throw new UnsupportedFeatureException(i2 + " is not an individual!");
		}
			
		if(role == null) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return false;
		    else
		        throw new UnsupportedFeatureException(p + " is not a property!");
		}
			
				
		EdgeList edges = subj.getEdgesTo(obj, role);
		for(int i = 0; i < edges.size(); i++) {
		    Edge edge = edges.edgeAt(i);
		    if(edge.getRole().equals(role)) {
		        subj.removeEdge(edge);
		        status |= ABOX_CHANGED;
		        removed = true;
		        break;
		    }
		}		
		
		if( log.isDebugEnabled() ) log.debug("Remove ObjectPropertyValue " + i1 + " " + p + " " + i2);	
		
		return removed;
	}
	
	public void removeType(ATermAppl ind, ATermAppl c) {
		status |= ABOX_CHANGED;
		Individual subj = abox.getIndividual(ind);

		if(subj == null) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return;
		    else
		        throw new UnsupportedFeatureException(ind + " is not an individual!");
		}			
				
		subj.removeType(c);
		
		if( log.isDebugEnabled() ) log.debug("Remove Type " + ind + " " + c);		
	}
	
	public void prepare() {
		if(!isChanged()) return;
		
		boolean explain = abox.doExplanation();
		abox.setDoExplanation(true);
		
		Timer timer = timers.startTimer("preprocessing");
		
		boolean reuseTaxonomy = 
		    (taxonomy != null) && !isTBoxChanged() && 
		    (!expressivity.hasNominal() || PelletOptions.USE_PSEUDO_NOMINALS);

		// size of Tg before absorbption
		int sizeTg = 0;
		if( isTBoxChanged() ) {
			if( log.isDebugEnabled() ) log.debug("Splitting...");
			tbox.split();
			
			sizeTg = tbox.getTgSize();
			
			if( PelletOptions.USE_ABSORPTION ) {
				if( log.isDebugEnabled() ) log.debug("Absorbing...");
				tbox.absorb();		
			}
			
			if( log.isDebugEnabled() ) log.debug("Normalizing...");
			tbox.normalize();
			if( log.isDebugEnabled() ) log.debug("Internalizing...");
			tbox.internalize();
			
			// changes in tbox may cause new domain and ranges to added so process
			// rbox again
			if( log.isDebugEnabled() ) log.debug("Role hierarchy...");
//		    Timer t = timers.startTimer("rbox");
			rbox.computeRoleHierarchy();		
//			t.stop();
		}
		else if( isRBoxChanged() ) {
			if( log.isDebugEnabled() ) log.debug("Role hierarchy...");
			rbox.computeRoleHierarchy();
		}
		
		// The prepartion of TBox and RBox is finished so we set the 
		// status to UNCHANGED now. Expressivity check can only work 
		// with prepared KB's
		status = UNCHANGED;

		if( log.isDebugEnabled() ) log.debug("Expressivity...");
		expressivity.compute();	    
		
		if( log.isDebugEnabled() ) log.debug("ABox init...");
//		abox.initialize();		
		instances.clear();
		if( log.isDebugEnabled() ) log.debug("done.");
        
        estimate = new SizeEstimate( this );
		abox.setDoExplanation( explain );
		
		abox.clearCaches( !reuseTaxonomy );
		if( reuseTaxonomy )
		    status |= CLASSIFICATION;
		else
		    taxonomy = null;

		timer.stop();

		if(PelletOptions.PRINT_SIZE) {		
		    System.out.print("Expressivity: " + expressivity + ", ");
		    System.out.print("Classes: " + getClasses().size() + " ");			
		    System.out.print("Properties: " + getProperties().size() + " ");			
		    System.out.print("Individuals: " + individuals.size());
		    if(sizeTg > 0) {
		        System.out.print(", GCIs: " + tbox.getTgSize() + " (");
		        System.out.print((sizeTg - tbox.getTgSize()) + " absorbed)");
		    }
		    System.out.print(" Strategy: " + chooseStrategy(abox));
			System.out.println();
//			tbox.print();
		}	
	}
    
    public String getInfo() {
        prepare();
        
        StringBuffer buffer = new StringBuffer();
        buffer.append("Expressivity: " + expressivity + " ");
        buffer.append("Classes: " + getClasses().size() + " ");          
        buffer.append("Properties: " + getProperties().size() + " ");            
        buffer.append("Individuals: " + individuals.size() + " ");
        if( expressivity.hasNominal() )
            buffer.append("Nominals: " + expressivity.getNominals().size() + " ");
        if( tbox.getTgSize() > 0 ) {
            buffer.append("GCIs: " + tbox.getTgSize() );
        }
        
        return buffer.toString();
    }
	
	/**
	 * Returns true if the consistency check has been done and nothing in th KB
	 * has changed after that.
	 */
	public boolean isConsistencyDone() {
	    // check if consistency bit is set but none of the change bits
		return (status & (CONSISTENCY | ALL_CHANGED)) == CONSISTENCY;		
	}

	/**
	 * Returns true if the classification check has been done and nothing in th KB
	 * has changed after that.
	 */
	public boolean isClassified() {
		return (status & (CLASSIFICATION | ALL_CHANGED)) == CLASSIFICATION;		
	}
	
	public boolean isRealized() {
		return (status & (REALIZATION | ALL_CHANGED)) == REALIZATION;
	}
	
	public boolean isChanged() {
		return (status & ALL_CHANGED) != 0;
	}

	public boolean isTBoxChanged() {
		return (status & TBOX_CHANGED) != 0;
	}
	
	public boolean isRBoxChanged() {
		return (status & RBOX_CHANGED) != 0;
	}
	
	public boolean isABoxChanged() {
		return (status & ABOX_CHANGED) != 0;
	}
	
	private void consistency() {
		if(isConsistencyDone()) return;
		
		// always turn on explanations for the first consistency check
		boolean explain = abox.doExplanation();
		abox.setDoExplanation(true);

		prepare();
		
		Timer timer = timers.startTimer("consistency");
		
		consistent = abox.isConsistent();
		abox.setDoExplanation(explain);
		
		if(!consistent) {
			System.err.println("WARNING: Inconsistent ontology. Reason: " + getExplanation());
		}		
		
		timer.stop();
        
        status |= CONSISTENCY;
	}
	
	public boolean isConsistent() {
		consistency();
        
		return consistent;
	}
	
	public void ensureConsistency() {
		if(!isConsistent())
			throw new InconsistentOntologyException("Cannot do reasoning with inconsistent ontologies!");		
	}

	public void classify() {
		ensureConsistency();

		if(isClassified()) return;			

		Timer timer = timers.startTimer("classify");

		builder = getTaxonomyBuilder();
		    
		taxonomy = builder.classify();

		timer.stop();

		// if user canceled return
		if( taxonomy == null ) {
		    builder = null;
		    return;	
		}
		
		status |= CLASSIFICATION;
	}

	public void realize() {
		if( isRealized() ) return;
		
		classify();
		
		if( !isClassified() )
		    return;		

		Timer timer = timers.startTimer("realize");

		taxonomy = builder.realize();

		timer.stop();
		
		// if user canceled return
		if( taxonomy == null ) {
		    builder = null;
		    return;	
		}
		
		status |= REALIZATION;		
	}

	/**
	 * Return the set of all named classes. Returned set is unmodifiable!
	 * 
	 * @return
	 */
	public Set getClasses() {
		return Collections.unmodifiableSet( tbox.getClasses() );
	}
	
	/**
	 * Return the set of all named classes including TOP and BOTTOM. Returned set is modifiable.
	 * 
	 * @return
	 */
	public Set getAllClasses() {
		return Collections.unmodifiableSet(tbox.getAllClasses());
	}
	
	/**
	 * Return the set of all properties.
	 * 
	 * @return
	 */
	public Set getProperties() {
		Set set = new HashSet();
		Iterator i = rbox.getRoles().iterator();
		while(i.hasNext()) {
		    Role role = (Role) i.next();
			ATermAppl p = role.getName();
			if(ATermUtils.isPrimitive(p) && (role.isObjectRole() || role.isDatatypeRole()))
				set.add(p);
		}
		return set;
	}
	
	/**
	 * Return the set of all object properties.
	 * 
	 * @return
	 */
	public Set getObjectProperties() {
		Set set = new HashSet();
		Iterator i = rbox.getRoles().iterator();
		while(i.hasNext()) {
		    Role role = (Role) i.next();
			ATermAppl p = role.getName();
			if(ATermUtils.isPrimitive(p) && role.isObjectRole())
				set.add(p);
		}
		return set;
	}
	
	public Set getTransitiveProperties() {
		Set set = new HashSet();
		Iterator i = rbox.getRoles().iterator();
		while(i.hasNext()) {
		    Role role = (Role) i.next();
			ATermAppl p = role.getName();
			if(ATermUtils.isPrimitive(p) && role.isTransitive())
				set.add(p);
		}
		return set;
	}
	
	public Set getSymmetricProperties() {
		Set set = new HashSet();
		Iterator i = rbox.getRoles().iterator();
		while(i.hasNext()) {
		    Role role = (Role) i.next();
			ATermAppl p = role.getName();
			if(ATermUtils.isPrimitive(p) && role.isSymmetric())
				set.add(p);
		}
		return set;
	}
	
	public Set getFunctionalProperties() {
		Set set = new HashSet();
		Iterator i = rbox.getRoles().iterator();
		while(i.hasNext()) {
		    Role role = (Role) i.next();
			ATermAppl p = role.getName();
			if(ATermUtils.isPrimitive(p) && role.isFunctional())
				set.add(p);
		}
		return set;
	}
	
	public Set getInverseFunctionalProperties() {
		Set set = new HashSet();
		Iterator i = rbox.getRoles().iterator();
		while(i.hasNext()) {
		    Role role = (Role) i.next();
			ATermAppl p = role.getName();
			if(ATermUtils.isPrimitive(p) && role.isInverseFunctional())
				set.add(p);
		}
		return set;
	}
	
	/**
	 * Return the set of all object properties.
	 * 
	 * @return
	 */
	public Set getDataProperties() {
		Set set = new HashSet();
		Iterator i = rbox.getRoles().iterator();
		while(i.hasNext()) {
		    Role role = (Role) i.next();
			ATermAppl p = role.getName();
			if(ATermUtils.isPrimitive(p) && role.isDatatypeRole())
				set.add(p);
		}
		return set;
	}
	
	/**
	 * Return the set of all individuals. Returned set is unmodifiable!
	 * 
	 * @return
	 */
	public Set getIndividuals() {
		return Collections.unmodifiableSet(individuals);
	}
	
	public Role getProperty(ATerm r) {
		return rbox.getRole(r);
	}
	
	public int getPropertyType(ATerm r) {
		Role role = getProperty(r);
		return (role == null) ? Role.UNTYPED : role.getType();
	}
	
	public boolean isClass(ATerm c) {
		return tbox.getClasses().contains( c ) ||
			c.equals( ATermUtils.TOP ) ||
			ATermUtils.isComplexClass( c ); 
	}

	public boolean isProperty(ATerm p) {
		return rbox.isRole(p);
	}
	
	public boolean isDatatypeProperty(ATerm p) {
		return getPropertyType(p) == Role.DATATYPE;
	}
	
	public boolean isObjectProperty(ATerm p) {
		return getPropertyType(p) == Role.OBJECT;
	}
	
	public boolean isABoxProperty(ATerm p) {
	    int type = getPropertyType(p);
		return  (type == Role.OBJECT) || (type == Role.DATATYPE);
	}
	
	public boolean isAnnotationProperty(ATerm p) {
		return getPropertyType(p) == Role.ANNOTATION;
	}	
	
	public boolean isOntologyProperty(ATerm p) {
		return getPropertyType(p) == Role.ONTOLOGY;
	}
	
	public boolean isIndividual(ATerm ind) {
		return getIndividuals().contains(ind);
	}
	
	public boolean isTransitiveProperty(ATermAppl r) {
		Role role = getRole( r );
		
		if( role == null )  {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return false;
		    else
		        throw new UnsupportedFeatureException(r + " is not a known property");
		}	
		
		if( role.isTransitive() )
			return true;
		
		ensureConsistency();
		
		ATermAppl c = ATermUtils.makeTermAppl("_C_");
		ATermAppl notC = ATermUtils.makeNot(c);
		ATermAppl test = 
		    ATermUtils.makeAnd(
		        ATermUtils.makeSomeValues(r, ATermUtils.makeSomeValues(r, c)),
		        ATermUtils.makeAllValues(r, notC));
		
		return !abox.isSatisfiable( test );
	}
	
	public boolean isSymmetricProperty( ATermAppl p ) {
		return isInverse(p, p);
	}

	public boolean isFunctionalProperty( ATermAppl p ) {
        Role role = getRole( p );
        
        if( role == null )  {
            if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
                return false;
            else
                throw new UnsupportedFeatureException( p + " is not a known property" );
        }
        
		if( role.isFunctional() )
			return true;
				
		ATermAppl max1P = ATermUtils.makeMax( p, 1 );
        return isSubClassOf( ATermUtils.TOP, max1P );
	}

	public boolean isInverseFunctionalProperty(ATermAppl p) {
        Role role = getRole( p );
        
        if( role == null )  {
            if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
                return false;
            else
                throw new UnsupportedFeatureException( p + " is not a known property" );
        }
        
		if( role.isInverseFunctional() )
			return true;
        else if( !role.isObjectRole() )
            return false;

		ATermAppl invP = role.getInverse().getName();
        ATermAppl max1invP = ATermUtils.makeMax( invP, 1 );
        return isSubClassOf( ATermUtils.TOP, max1invP );
	}

	public boolean isSubPropertyOf(ATermAppl sub, ATermAppl sup) {
		Role roleSub = rbox.getRole(sub);
		Role roleSup = rbox.getRole(sup);

        if( roleSub == null )  {
            if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
                return false;
            else
                throw new UnsupportedFeatureException( roleSub + " is not a known property" );
        }   

        if( roleSup == null )  {
            if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
                return false;
            else
                throw new UnsupportedFeatureException( roleSup + " is not a known property" );
        } 
        
		if( roleSub.isSubRoleOf( roleSup ) )
            return true;

        ensureConsistency();

        ATermAppl c = ATermUtils.makeTermAppl( "_C_" );
        ATermAppl notC = ATermUtils.makeNot( c );
        ATermAppl test = 
		    ATermUtils.makeAnd(
		        ATermUtils.makeSomeValues( sub, c ),
		        ATermUtils.makeAllValues( sup, notC ) );
		
		return !abox.isSatisfiable( test );
	}

	public boolean isEquivalentProperty( ATermAppl p1, ATermAppl p2 ) {
        return isSubPropertyOf( p1, p2 ) && isSubPropertyOf( p2, p1 );
    }

	public boolean isInverse( ATermAppl r1, ATermAppl r2 ) {
        Role role1 = getRole( r1 );
        Role role2 = getRole( r2 );
        
        if( role1 == null )  {
            if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
                return false;
            else
                throw new UnsupportedFeatureException( r1 + " is not a known property" );
        }   

        if( role2 == null )  {
            if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
                return false;
            else
                throw new UnsupportedFeatureException( r2 + " is not a known property" );
        }   

        // the following condition is wrong due to nominals, see OWL test 
        // cases SymmetricProperty-002
//        if( !role1.hasNamedInverse() )
//            return false;
//        else 
        if( role1.getInverse().equals( role2 ) )
			return true;

		ensureConsistency();
		
		ATermAppl c = ATermUtils.makeTermAppl( "_C_" );
        ATermAppl notC = ATermUtils.makeNot( c );
		ATermAppl test = 
		    ATermUtils.makeAnd( c,
		        ATermUtils.makeSomeValues( r1, 
		                ATermUtils.makeAllValues( r2, notC ) ) );
		
		return !abox.isSatisfiable( test );
	}

	public boolean hasDomain(ATermAppl p, ATermAppl c) {
		ATermAppl minP1 = ATermUtils.makeMin(p, 1);
		return isSubClassOf(minP1, c);
	}

	public boolean hasRange(ATermAppl p, ATermAppl c) {
		ATermAppl allValues = ATermUtils.makeAllValues(p, c);
		return isSubClassOf(ATermUtils.TOP, allValues);
	}	
	
	public boolean isDatatype(ATermAppl c) {
		return abox.getDatatypeReasoner().isDefined(c.getName());
	}	
	
	public boolean isSatisfiable(ATermAppl c) {
		ensureConsistency();
		
		if(!isClass(c)) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return false;
		    else
		        throw new UnsupportedFeatureException(c + " is not a known class!");
		}			
				
		return abox.isSatisfiable(c);
	}

	/**
	 * Returns true if there is at leaat one individual that belongs to the given class
	 * 
	 * @param c
	 * @return
	 */
	public boolean hasInstance( ATerm d ) {
		ensureConsistency();
		
		ATermAppl c = ATermUtils.normalize((ATermAppl) d);		
		
		List unknowns = new ArrayList();			
		Iterator i = abox.getIndIterator();
		while(i.hasNext()) {
			ATermAppl x = ((Individual) i.next()).getName();
			
			Bool knownType = abox.isKnownType( x, c );
			if( knownType.isTrue() )
				return true;
			else if( knownType.isUnknown() )
			    unknowns.add( x );
		}

		boolean hasInstance = !unknowns.isEmpty() && abox.isType( unknowns, c );

		return hasInstance;
	}
	
	public boolean isSubTypeOf(ATermAppl d1, ATermAppl d2) {
		if(!isDatatype(d1)) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return false;
		    else
		        throw new UnsupportedFeatureException(d1 + " is not a known datatype");
		}		
			
		if(!isDatatype(d2)) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return false;
		    else
		        throw new UnsupportedFeatureException(d2 + " is not a known datatype");
		}		
		
	    return getDatatypeReasoner().isSubTypeOf(d1, d2);
	}
	
	/**
	 * Check if class c1 is subclass of class c2.
	 * 
	 * @param c1
	 * @param c2
	 * @return
	 */
	public boolean isSubClassOf(ATermAppl c1, ATermAppl c2) {
		ensureConsistency();

        if( !isClass( c1 ) ) {
            if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
                return false;
            else
                throw new UnsupportedFeatureException( c1 + " is not a known class" );
        }

        if( !isClass( c2 ) ) {
            if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
                return false;
            else
                throw new UnsupportedFeatureException( c2 + " is not a known class" );
        }

        if( c1.equals( c2 ) )
            return true;

        // normalize concepts
        c1 = ATermUtils.normalize( c1 );
        c2 = ATermUtils.normalize( c2 );

        if( isClassified() ) {
            Bool isSubNode = taxonomy.isSubNodeOf( c1, c2 );
            if( isSubNode.isKnown() )
                return isSubNode.isTrue();
        }

        return abox.isSubClassOf( c1, c2 );
	}
	
	/**
	 * @deprecated  As of Pellet 1.1.1, replaced by {@link #isSubClassOf(ATermAppl,ATermAppl)}
	 */
	public boolean isSubclassOf(ATermAppl c1, ATermAppl c2) {
	    return isSubClassOf(c1, c2);
	}

	/**
	 * Check if class c1 is equivalent to class c2.	 
	 * 
	 * @param c1
	 * @param c2
	 * @return
	 */
	public boolean isEquivalentClass(ATermAppl c1, ATermAppl c2) {
		return isSubClassOf(c1, c2) && isSubClassOf(c2, c1);
	}

	// Two concepts are disjoint if their intersection is empty
	public boolean isDisjoint(ATermAppl c1, ATermAppl c2) {
	    ATermAppl notC2 = ATermUtils.makeNot(c2);
			
		return isSubClassOf( c1, notC2);
	}

	public boolean isComplement(ATermAppl c1, ATermAppl c2) {
		ATermAppl notC2 = ATermUtils.makeNot(c2);

		return isEquivalentClass(c1, notC2);
	}

	/**
	 * Answers the isType question without doing any satisfiability check. It might return
	 * <code>Bool.TRUE</code>, <code>Bool.FALSE</code>, or <code>Bool.UNKNOWN</code>.
	 * If <code>Bool.UNKNOWN</code> is returned <code>isType</code> function needs to be called to get
	 * the answer.  
	 * 
	 * @param x
	 * @param c
	 * @return
	 */
	public Bool isKnownType(ATermAppl x, ATermAppl c) {
		ensureConsistency();

        c = ATermUtils.normalize( c );

        return abox.isKnownType( x, c );
	}
	
	public boolean isType(ATermAppl x, ATermAppl c) {
		ensureConsistency();

		if(!isIndividual(x)) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return false;
		    else
		        throw new UnsupportedFeatureException(x + " is not an individual!");
		}
		if(!isClass(c)) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return false;
		    else
		        throw new UnsupportedFeatureException(c + " is not a valid class expression");
		}

		if(isRealized() && taxonomy.contains(c))
			return taxonomy.getInstances(c).contains(x);
		
//		Integer count = (Integer) typeChecks.get( c );
//		if( count == null ) 
//		    count = new Integer( 1 );
//		else
//		    count = new Integer( count.intValue() + 1 );
//		
//		if( count.intValue() > PelletOptions.AUTO_RETRIEVE_LIMIT ) {
//		    typeChecks.remove( c );
//		    list = retrieve( c );
//		    return list.contains( c );
//		}		
//		else
//		    typeChecks.put( c, count );
		
		return abox.isType(x, c);
	}
	
	public boolean isSameAs(ATermAppl t1, ATermAppl t2) {
	    ensureConsistency();

		if(!isIndividual(t1)) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )		
		        return false;
		    else
		        throw new UnsupportedFeatureException(t1 + " is not an individual!");
		}
		if(!isIndividual(t2)) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )		
		        return false;
		    else
		        throw new UnsupportedFeatureException(t2 + " is not an individual!");
		}
        
        if( t1.equals( t2 ) )
            return true;
        
	    Set knowns = new HashSet();
	    Set unknowns = new HashSet();

		Individual ind = abox.getPseudoModel().getIndividual( t1 );
        if( ind.isMerged() && !ind.getMergeDependency(true).isIndependent() )
            abox.getSames( (Individual) ind.getSame(), unknowns, unknowns );
        else
            abox.getSames( (Individual) ind.getSame(), knowns, unknowns );
		
		if( knowns.contains( t2 ) )
		    return true;
		else if( !unknowns.contains( t2 ) )
		    return false;
		else
		    return abox.isSameAs( t1, t2 );
	}

	public boolean isDifferentFrom( ATermAppl t1, ATermAppl t2 ) {
		Individual ind1 = abox.getIndividual(t1); 
		Individual ind2 = abox.getIndividual(t2); 

		if(ind1 == null) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return false;
		    else
		        throw new UnsupportedFeatureException(t1 + " is not an individual!");
		}
			
		if(ind2 == null) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return false;
		    else
		        throw new UnsupportedFeatureException(t2 + " is not an individual!");
		}
		
		if( ind1.isDifferent( ind2 ) ) 
		    return true;

		ATermAppl c = ATermUtils.makeNot( ATermUtils.makeValue( t2 ) );

		return isType( t1, c );
	}
	
	public boolean hasPropertyValue(ATermAppl s, ATermAppl p, ATermAppl o) {
		ensureConsistency();
		
		if(!isIndividual(s)) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return false;
		    else
		        throw new UnsupportedFeatureException(s + " is not an individual!");
		}
			
		if(!isProperty(p)) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return false;
		    else
		        throw new UnsupportedFeatureException(p + " is not a known property!");
		}			

		if(o != null) {
			if(isDatatypeProperty(p)) {
			    if(!ATermUtils.isLiteral(o))
			        return false;			    
			}
			else if(!isIndividual(o)) {
			    return false;			    
			}
		}

		return abox.hasPropertyValue( s, p, o );
	}
	
	/**
	 * Answers the hasPropertyValue question without doing any satisfiability check. It might 
	 * return <code>Boolean.TRUE</code>, <code>Boolean.FALSE</code>, or <code>null</code> (unknown).
	 * If the null value is returned <code>hasPropertyValue</code> function needs to be called to 
	 * get the answer.  
	 * 
	 * @param s Subject
	 * @param p Predicate
	 * @param o Object (<code>null</code> can be used as wildcard)
	 * @return
	 */
	public Bool hasKnownPropertyValue(ATermAppl s, ATermAppl p, ATermAppl o) {
		ensureConsistency();

		return abox.hasObviousPropertyValue( s, p ,o );
	}

	/**
	 * @return Returns the abox.
	 */
	public ABox getABox() {
		return abox;
	}

	/**
	 * @return Returns the rbox.
	 */
	public RBox getRBox() {
		return rbox;
	}

	/**
	 * @return Returns the tbox.
	 */
	public TBox getTBox() {
		return tbox;
	}

	/**
	 * @return Returns the DatatypeReasoner
	 */
	public DatatypeReasoner getDatatypeReasoner() {
		return abox.getDatatypeReasoner();
	}

	/**
	 * Returns the (named) superclasses of class c. Depending onthe second parameter the resulting
	 * list will include etiher all or only the direct superclasses.
	 * 
	 * A class d is a direct superclass of c iff
	 * <ol>
	 *   <li> d is superclass of c </li> 
	 *   <li> there is no other class x such that x is superclass of c and d is superclass of x </li>
	 * </ol>
	 * The class c itself is not included in the list but all the other classes that
	 * are sameAs c are put into the list. Also note that the returned
	 * list will always have at least one element. The list will either include one other
	 * concept from the hierachy or the TOP concept if no other class subsumes c. By definition
	 * TOP concept is superclass of every concept. 
	 * 
	 * <p>*** This function will first classify the whole ontology ***</p>
	 * 
	 * @param c class whose superclasses are returned
	 * @return A set of sets, where each set in the collection represents an equivalence 
	 * class. The elements of the inner class are ATermAppl objects. 
	 */
	public Set getSuperClasses(ATermAppl c, boolean direct) {
	    c = ATermUtils.normalize( c );
	    
		if(!isClass(c)) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return Collections.EMPTY_SET;
		    else
		        throw new UnsupportedFeatureException(c + " is not a class!");
		}
		
		classify();		
		
		if( !taxonomy.contains( c ) )
		    builder.classify( c );
		
	    return taxonomy.getSupers(c, direct);
	}

	/** 
	 * Returns all the (named) subclasses of class c. The
	 * class c itself is not included in the list but all the other classes that
	 * are equivalent to c are put into the list. Also note that the returned
	 * list will always have at least one element, that is the BOTTOM concept. By definition
	 * BOTTOM concept is subclass of every concept.
	 * This function is equivalent to calling getSubClasses(c, true).
	 * 
	 * <p>*** This function will first classify the whole ontology ***</p>
	 * 
	 * @param c class whose subclasses are returned 
	 * @return A set of sets, where each set in the collection represents an equivalence 
	 * class. The elements of the inner class are ATermAppl objects. 
	 */
	public Set getSubClasses(ATermAppl c) {		    
		return getSubClasses( c, false );
	}

	public Set getDisjoints(ATermAppl c) {
	    if( !isClass(c) ) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return Collections.EMPTY_SET;
		    else
		        throw new UnsupportedFeatureException(c + " is not a class!");
		}
		
	    ATermAppl notC = ATermUtils.normalize( ATermUtils.makeNot( c ) );
		Set disjoints = getSubClasses( notC );
		
		if( tbox.getAllClasses().contains( notC ) )
		    disjoints.add( getAllEquivalentClasses( notC ) );
		
		return disjoints;
	}
	
	public Set getComplements(ATermAppl c) {
	    if( !isClass(c) ) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return Collections.EMPTY_SET;
		    else
		        throw new UnsupportedFeatureException(c + " is not a class!");
		}
		
		ATermAppl notC = ATermUtils.normalize( ATermUtils.makeNot( c ) );
		Set complements = getEquivalentClasses( notC );
		
		if( tbox.getAllClasses().contains( notC ) )
		    complements.add( notC );
		
		return complements;
	}	

	
	/**
	 * Returns the (named) classes individual belongs to. Depending on the second parameter the 
	 * result will include either all types or only the direct types.
	 * 
	 * @param ind An individual name
	 * @param direct If true return only the direct types, otherwise return all types
	 * @return A set of sets, where each set in the collection represents an equivalence 
	 * class. The elements of the inner class are ATermAppl objects. 
	 */
	public Set getTypes(ATermAppl ind, boolean direct) {
		if(!isIndividual(ind)) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return Collections.EMPTY_SET;
		    else
		        throw new UnsupportedFeatureException(ind + " is not an individual!");
		}
		
		realize();				
		
		return taxonomy.getTypes(ind, direct);
	}

	/**
	 * Get all the (named) classes individual belongs to. 
	 *  
	 * <p>*** This function will first realize the whole ontology ***</p>
	 * 
	 * @param ind An individual name
	 * @return A set of sets, where each set in the collection represents an equivalence 
	 * class. The elements of the inner class are ATermAppl objects. 
	 */
	public Set getTypes(ATermAppl ind) {
		if(!isIndividual(ind)) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return Collections.EMPTY_SET;
		    else
		        throw new UnsupportedFeatureException(ind + " is not an individual!");
		}
		
		realize();
		
		return taxonomy.getTypes(ind);
	}
	
	public ATermAppl getType(ATermAppl ind) {	    
		if(!isIndividual(ind)) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return null;
		    else
		        throw new UnsupportedFeatureException(ind + " is not an individual!");
		}
		
	    // there is always at least one atomic class guranteed to exist (i.e. owl:Thing)
		return (ATermAppl) abox.getIndividual(ind).getTypes(Node.ATOM).iterator().next();
	}
	
	public ATermAppl getType(ATermAppl ind, boolean direct) {
		if(!isIndividual(ind)) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return null;
		    else
		        throw new UnsupportedFeatureException(ind + " is not an individual!");
		}
		
	    realize();
	    
	    Set setOfSets = taxonomy.getTypes(ind, direct);
	    Set set = (Set) setOfSets.iterator().next();
		return (ATermAppl) set.iterator().next();
	}
	
	/** 
	 * Returns all the instances of concept c. If TOP concept is used every individual in the
	 * knowledge base will be returned
	 * 
	 * @param c class whose instanceses are returned 
	 * @return A set of ATerm objects
	 */
	public Set getInstances(ATermAppl c) {
		if(!isClass(c)) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return Collections.EMPTY_SET;
		    else
		        throw new UnsupportedFeatureException(c + " is not a class!");
		}
		
		if( isRealized() && taxonomy.contains( c ) )
			return taxonomy.getInstances( c );
		
		return new HashSet( retrieve( c, individuals ) );
	}

	/**
	 * Returns the instances of class c. Depending on the second parameter the resulting
	 * list will include all or only the direct instances. An individual x is a direct
	 * instance of c iff x is of type c and there is no subclass d of c such that x is of
	 * type d. 
	 * 
	 * <p>*** This function will first realize the whole ontology ***</p>
	 * 
	 * @param c class whose instances are returned
	 * @param direct if true return only the direct instances, otherwise return all the instances
	 * @return A set of ATerm objects
	 */
	public Set getInstances(ATermAppl c, boolean direct) {
		if(!isClass(c)) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return Collections.EMPTY_SET;
		    else
		        throw new UnsupportedFeatureException(c + " is not a class!");
		}
		
	    if( !direct )
	        return getInstances( c );
	    
	    if( ATermUtils.isPrimitive( c ) ) {
			realize();
			
		    return taxonomy.getInstances(c, direct);	
	    }
	    
	    return Collections.EMPTY_SET;
	}
	
	/**
	 * Returns all the classes that are equivalent to class c, excluding c itself. 
	 * 
	 * <p>*** This function will first classify the whole ontology ***</p>
	 * 
	 * @param c class whose equivalent classes are found
	 * @return A set of ATerm objects
	 */
	public Set getEquivalentClasses(ATermAppl c) {
	    c = ATermUtils.normalize( c );
	    
		if(!isClass(c)) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return Collections.EMPTY_SET;
		    else
		        throw new UnsupportedFeatureException(c + " is not a class!");
		}
		
		classify();

		if( !taxonomy.contains( c ) )
		    builder.classify( c );
	
	    return taxonomy.getEquivalents( c );
	}
	
	/**
	 * Returns all the classes that are equivalent to class c, including c itself.
	 * 
	 * <p>*** This function will first classify the whole ontology ***</p>
	 * 
	 * @param c class whose equivalent classes are found
	 * @return A set of ATerm objects
	 */
	public Set getAllEquivalentClasses(ATermAppl c) {
	    c = ATermUtils.normalize( c );
	    
		if(!isClass(c)) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return Collections.EMPTY_SET;
		    else
		        throw new UnsupportedFeatureException(c + " is not a class!");
		}
		
		classify();

		if( !taxonomy.contains( c ) )
		    builder.classify( c );
	
	    return taxonomy.getAllEquivalents( c );
	}

	/** 
	 * Returns all the superclasses (implicitly or explicitly defined) of class c. The
	 * class c itself is not included in the list. but all the other classes that
	 * are sameAs c are put into the list. Also note that the returned
	 * list will always have at least one element, that is TOP concept. By definition
	 * TOP concept is superclass of every concept.
	 * This function is equivalent to calling getSuperClasses(c, true).
	 * 
	 * <p>*** This function will first classify the whole ontology ***</p>
	 * 
	 * @param c class whose superclasses are returned 
	 * @return A set of sets, where each set in the collection represents an equivalence 
	 * class. The elements of the inner class are ATermAppl objects. 
	 */
	public Set getSuperClasses(ATermAppl c) {
	    return getSuperClasses( c, false );	
	}

	/**
	 * Returns the (named) subclasses of class c. Depending onthe second parameter the result
	 * will include either all subclasses or only the direct subclasses.
	 * 
	 * A class d is a direct subclass of c iff
	 * <ol>
	 *   <li>d is subclass of c</li> 
	 *   <li>there is no other class x different from c and d such that x is subclass 
	 *   of c and d is subclass of x</li>
	 * </ol> 
	 * The class c itself is not included in the list but all the other classes that
	 * are sameAs c are put into the list. Also note that the returned
	 * list will always have at least one element. The list will either include one other
	 * concept from the hierachy or the BOTTOM concept if no other class is subsumed by c. 
	 * By definition BOTTOM concept is subclass of every concept. 
	 * 
	 * <p>*** This function will first classify the whole ontology ***</p>
	 * 
	 * @param c class whose subclasses are returned
	 * @param direct If true return only the direct subclasses, otherwise return all the subclasses
	 * @return A set of sets, where each set in the collection represents an equivalence 
	 * class. The elements of the inner class are ATermAppl objects. 
	 */
	public Set getSubClasses(ATermAppl c, boolean direct) {
	    c = ATermUtils.normalize( c );
	    
		if(!isClass(c)) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return Collections.EMPTY_SET;
		    else
		        throw new UnsupportedFeatureException(c + " is not a class!");
		}
		
		classify();
		
		if( !taxonomy.contains( c ) )
		    builder.classify( c );
		    
		return taxonomy.getSubs( c, direct );		
	}
	
	/**
	 * Return all the super properties of p.
	 * 
	 * @param prop
	 * @return A set of sets, where each set in the collection represents a set of equivalent 
	 * properties. The elements of the inner class are Role objects. 
	 */
	public Set getSuperProperties(ATermAppl prop) {
	    return getSuperProperties( prop, false );	
	}
	
	/**
	 * Return the super properties of p. Depending on the second parameter the result
	 * will include either all super properties or only the direct super properties.
	 * 
	 * @param prop
	 * @param direct If true return only the direct super properties, otherwise return all the 
	 * super properties
	 * @return A set of sets, where each set in the collection represents a set of equivalent 
	 * properties. The elements of the inner class are Role objects. 
	 */
	public Set getSuperProperties(ATermAppl prop, boolean direct) {
	    prepare();
	    
	    return rbox.getTaxonomy().getSupers( prop, direct );	
	}
	
	/**
	 * Return all the sub properties of p.
	 * 
	 * @param prop
	 * @return A set of sets, where each set in the collection represents a set of equivalent 
	 * properties. The elements of the inner class are ATermAppl objects. 
	 */
	public Set getSubProperties(ATermAppl prop) {
		return getSubProperties( prop, false );
	}
	
	/**
	 * Return the sub properties of p. Depending on the second parameter the result
	 * will include either all subproperties or only the direct subproperties.
	 * 
	 * @param prop
	 * @param direct If true return only the direct subproperties, otherwise return all the subproperties
	 * @return A set of sets, where each set in the collection represents a set of equivalent 
	 * properties. The elements of the inner class are ATermAppl objects. 
	 */
	public Set getSubProperties(ATermAppl prop, boolean direct) {
	    prepare();
	    
	    return rbox.getTaxonomy().getSubs( prop, direct );		
	}
	
	/**
	 * Return all the properties that are equivalent to p.
	 * 
	 * @param prop
	 * @return A set of ATermAppl objects. 
	 */
	public Set getEquivalentProperties(ATermAppl prop) {
	    prepare();
	    
	    return rbox.getTaxonomy().getEquivalents( prop );
	}
	
	public Set getAllEquivalentProperties(ATermAppl prop) {
	    prepare();
	    
	    return rbox.getTaxonomy().getAllEquivalents( prop );
	}

	
	/**
	 * Return the inverse property and all its equivalent properties.
	 * 
	 * @param prop
	 * @return
	 */
	public Set getInverses(ATerm name) {
		Role prop = rbox.getRole(name);
		if( prop == null ) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return Collections.EMPTY_SET;
		    else
		        throw new UnsupportedFeatureException( name + " is not a property!");
		}
		Role invR = prop.getInverse();
		if(invR != null && !invR.isAnon()) {
		    Set inverses = getAllEquivalentProperties( invR.getName() );
		    return inverses;
		}
			
		return SetUtils.EMPTY_SET;		
	}

	/**
	 * Return the (explicit or implicit) domain restrictions on the property. Unlike
	 * other functions the result may contain unnamed classes (represented as ATerm
	 * objects). If the domain is an intersection then all the elements of the 
	 * intersection will be included in the resulting set. 
	 * 
	 * @param prop
	 * @return
	 */
	public Set getDomains(ATermAppl name) {
		Set set = new HashSet();
		Role prop = rbox.getRole(name);
		if( prop == null ) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return Collections.EMPTY_SET;
		    else
		        throw new UnsupportedFeatureException( name + " is not a property!");
		}
		
		ATermAppl domain = prop.getDomain();
		if( domain != null ) {
            if( ATermUtils.isAnd( domain ) )
                set = ATermUtils.getPrimitives( (ATermList) domain.getArgument( 0 ) );
            else if( ATermUtils.isPrimitive( domain ) ) 
                set = SetUtils.singleton( domain );
		}
		
		return set;		
	}
	
	/**
	 * Return the (explicit or implicit) range restrictions on the property. Unlike
	 * other functions the result may contain unnamed classes (represented as ATerm
	 * objects). If the range is an intersection then all the elements of the 
	 * intersection will be included in the resulting set. 
	 * 
	 * @param prop
	 * @return
	 */
	public Set getRanges(ATerm name) {
        ensureConsistency();
        
		Set set = SetUtils.EMPTY_SET;
		Role prop = rbox.getRole(name);
		if( prop == null ) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return set;
		    else
		        throw new UnsupportedFeatureException( name + " is not a property!");
		}
		ATermAppl range = prop.getRange();
		if(range != null) {
			if( ATermUtils.isAnd( range ) )
				set = ATermUtils.getPrimitives( (ATermList) range.getArgument(0) );
			else if( ATermUtils.isPrimitive( range ) )
				set = SetUtils.singleton(range);
		}
		
		return set;		
	}

	/**
	 * Return all the indviduals asserted to be equal to the given individual inluding
	 * the individual itself.
	 * 
	 * @param name
	 * @return
	 */
	public Set getAllSames(ATermAppl name) {
	    ensureConsistency();
	    
	    Set knowns = new HashSet();
	    Set unknowns = new HashSet();

		Individual ind = abox.getPseudoModel().getIndividual( name );
        if( ind == null ) {
            if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
                return SetUtils.EMPTY_SET;
            else
                throw new UnsupportedFeatureException( name + " is not an individual!");            
        }
        
        if( ind.isMerged() && !ind.getMergeDependency(true).isIndependent() ) { 
            knowns.add( name );
            abox.getSames( (Individual) ind.getSame(), unknowns, unknowns );
            unknowns.remove( name );
        }
        else
            abox.getSames( (Individual) ind.getSame(), knowns, unknowns );
                
//		System.out.print( name + " " + ind + " " + knowns + " + " + unknowns + " = ");
		
        for(Iterator i = unknowns.iterator(); i.hasNext(); ) {
            ATermAppl other = (ATermAppl) i.next();        
            if( abox.isSameAs( name, other ) )
                knowns.add( other );
        }
//		System.out.println( knowns );
            
		return knowns; 	
	}
	
	/**
	 * Return all the indviduals asserted to be equal to the given individual but not the
	 * the individual itself.
	 * 
	 * @param name
	 * @return
	 */	
	public Set getSames(ATermAppl name) {	    
	    Set sames = getAllSames( name );
	    sames.remove( name );
            
		return sames; 	
	}
	
	/**
	 * Run the given RDQL query. 
	 * 
	 * @deprecated Use QueryEngine.exec methods instead
	 *  
	 * @param query
	 * @return
	 */
	public QueryResults runQuery(String queryStr) {
		return QueryEngine.exec( queryStr, this, Syntax.syntaxRDQL );
	}

	/**
	 * Return all literal values for a given dataproperty that belongs to
	 * the specified datatype.
	 * 
	 * @param r
	 * @param x
	 * @param lang
	 * @return List of ATermAppl objects representing literals. These objects are in the form
	 * literal(value, lang, datatypeURI).
	 */
	public List getDataPropertyValues(ATermAppl r, ATermAppl x, Datatype datatype) {
	    ensureConsistency();
	    
		Individual ind = abox.getIndividual(x);
		Role role = rbox.getRole(r);	

		if(ind == null) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return Collections.EMPTY_LIST;
		    else
		        throw new UnsupportedFeatureException(x + " is not an individual!");
		}

		if(role == null || !role.isDatatypeRole()) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return Collections.EMPTY_LIST;
		    else
		        throw new UnsupportedFeatureException(r + " is not a known data property!");
		}	
		
		return abox.getObviousDataPropertyValues(x, role, datatype);
	}
    
    public Set getPossibleProperties( ATermAppl x ) {
        ensureConsistency();
        
        Individual ind = abox.getIndividual(x);

        if(ind == null) {
            if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
                return Collections.EMPTY_SET;
            else
                throw new UnsupportedFeatureException(x + " is not an individual!");
        }

        return abox.getPossibleProperties( x );
    }
	
	/**
	 * Return all literal values for a given dataproperty that has the specified 
	 * language identifier.
	 * 
	 * @param r
	 * @param x
	 * @param lang
	 * @return List of ATermAppl objects.
	 */
	public List getDataPropertyValues(ATermAppl r, ATermAppl x, String lang) {	
		List values = getDataPropertyValues( r, x );
        if( lang == null ) 
            return values;

        List result = new ArrayList();
        Iterator i = values.iterator();
        while( i.hasNext() ) {
            ATermAppl lit = (ATermAppl) i.next();
            String litLang = ((ATermAppl) lit.getArgument( 1 )).getName();

            if( litLang.equals( lang ) ) result.add( lit );
        }

        return result;
	}
	
	/**
	 * Return all literal values for a given dataproperty and subject value.
	 * 
	 * @param r
	 * @param x
	 * @return List of ATermAppl objects.
	 */
	public List getDataPropertyValues(ATermAppl r, ATermAppl x) {
	    return getDataPropertyValues( r, x, (Datatype) null );
	}
		
	/**
	 * Return all property values for a given object property and subject value.
	 * 
	 * @param r
	 * @param x
	 * @return A list of ATermAppl objects
	 */
	public List getObjectPropertyValues(ATermAppl r, ATermAppl x) {
		Role role = rbox.getRole(r);
		
		if(role == null || !role.isObjectRole())
		    throw new UnsupportedFeatureException(
		        "getObjectPropertyValues function can only be used with object properties. " +
		        "Property: " + role);
				
		// TODO  get rid of unnecessary Set + List creation
		Set knowns = new HashSet();
		Set unknowns = new HashSet();			
		
		abox.getObjectPropertyValues(x, role, knowns, unknowns);

		if( !unknowns.isEmpty() ) {
			ATermAppl valueX = ATermUtils.makeHasValue( role.getInverse().getName(), x );
			ATermAppl c = ATermUtils.normalize( valueX );

			binaryInstanceRetrieval(c, new ArrayList( unknowns ), knowns);
		}
		
		return new ArrayList( knowns );		
	}
	
	/**
	 * Return all property values for a given property and subject value. 
	 * 
	 * @param r
	 * @param x
	 * @return List of ATermAppl objects.
	 */
	public List getPropertyValues(ATermAppl r, ATermAppl x) {
	    Role role = rbox.getRole(r);
	    
	    if(role.isObjectRole())
	        return getObjectPropertyValues( r, x );
	    else
	        return getDataPropertyValues( r, x );
	}

	
	/**
	 * List all subjects with a given property and property value.
	 * 
	 * @param r
	 * @param x If property is an object property an ATermAppl object
	 * that is the URI of the individual, if the property is a data property 
	 * an ATerm object that contains the literal value (See
	 * {#link #getIndividualsWithDataProperty(ATermAppl, ATermAppl)} for details)
	 * @return List of ATermAppl objects.
	 */
	public List getIndividualsWithProperty(ATermAppl r, ATermAppl x) {
	    Role role = rbox.getRole(r);
			
		if(role == null) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return Collections.EMPTY_LIST;
		    else
		        throw new UnsupportedFeatureException(r + " is not a known property!");
		}
		
	    if(role.isObjectRole())
	        return getIndividualsWithObjectProperty( r, x );
	    else
	        return getIndividualsWithDataProperty( r, x );
	}
	
	/**
	 * List all subjects with the given literal value for the specified
	 * data property. 
	 * 
	 * @param r An ATerm object that contains the literal value in the form
	 * literal(lexicalValue, langIdentifier, datatypeURI). Should be created
	 * with ATermUtils.makeXXXLiteral() functions.
	 * @param x
	 * @return List of ATermAppl objects.
	 */
	public List getIndividualsWithDataProperty( ATermAppl r, ATermAppl litValue ) {
	    ensureConsistency();
        
		Object value = getDatatypeReasoner().getValue( litValue );
		
		if( value == null ) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return Collections.EMPTY_LIST;
		    else
		        throw new UnsupportedFeatureException(litValue + " is not a valid literal value!");
		}
		
		List knowns = new ArrayList();		
		List unknowns = new ArrayList();
				
		Iterator i = abox.getIndIterator();
		while(i.hasNext()) {
			ATermAppl subj = ((Individual) i.next()).getName();
			
			Bool hasObviousValue = abox.hasObviousDataPropertyValue( subj, r, value );
			if( hasObviousValue.isUnknown() )
				unknowns.add( subj );
			else if( hasObviousValue.isTrue() )
			    knowns.add( subj );
		}
		
		if( !unknowns.isEmpty() ) {
			ATermAppl c = ATermUtils.normalize( ATermUtils.makeHasValue( r, litValue ) );

			binaryInstanceRetrieval(c, unknowns, knowns);
		}
		
		return knowns;
	}
	
	/**
	 * List all subjects with the given value for the specified object
	 * property. 
	 * 
	 * @param r
	 * @param o An ATerm object that is the URI of an individual
	 * @return List of ATermAppl objects.
	 */
	public List getIndividualsWithObjectProperty(ATermAppl r, ATermAppl o) {
	    ensureConsistency();	
	    
		if(!isIndividual(o)) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return Collections.EMPTY_LIST;
		    else
		        throw new UnsupportedFeatureException(o + " is not an individual!");
		}
	    
		Role role = rbox.getRole(r);
		
		ATermAppl invR = role.getInverse().getName();
		
		return getObjectPropertyValues( invR, o );
	}

	/**
	 * List all properties asserted between a subject and object.
	 * 
	 */
	public List getProperties(ATermAppl s, ATermAppl o) {
		if(!isIndividual(s)) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return Collections.EMPTY_LIST;
		    else
		        throw new UnsupportedFeatureException(s + " is not an individual!");
		}

		if(!isIndividual(o)) {
		    if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
		        return Collections.EMPTY_LIST;
		    else
		        throw new UnsupportedFeatureException(o + " is not an individual!");
		}

		List props = new ArrayList();

		Iterator i = ATermUtils.isLiteral( o )
			? getDataProperties().iterator()
			: getObjectProperties().iterator();    
		while( i.hasNext() ) {
            ATermAppl p = (ATermAppl) i.next();
            if( abox.hasPropertyValue( s, p, o ) )
                props.add( p );
        }
		
		return props;
	}
	
	public Map getPropertyValues( ATermAppl pred ) {
	    Map result = new HashMap();
	    
	    Iterator subjects = retrieveIndividualsWithProperty( pred ).iterator();	
	    while( subjects.hasNext() ) {
	        ATermAppl subj = (ATermAppl) subjects.next();
	        List objects = getPropertyValues( pred, subj );
	        if( !objects.isEmpty() ) 
	            result.put( subj, objects );	        
	    }
	    
	    return result;
	}

        
	/**
	 * Return all the indiviuals that belong to the given class which is not
	 * necessarily a named class.
	 * 
	 * @param d
	 * @return
	 */
	public List retrieve( ATermAppl d, Collection individuals ) {
        ensureConsistency();

        ATermAppl c = ATermUtils.normalize( d );

        if( instances.containsKey( c ) )
            return (List) instances.get( c );
        else if( isRealized() && taxonomy.contains( c ) )
            return new ArrayList( getInstances( c ) );

        Timer timer = timers.startTimer( "retrieve" );
        ATermAppl notC = ATermUtils.negate( c );
        List knowns = new ArrayList();
//        Map unknown = new HashMap();

        // this is mostly to ensure that a model for notC is cached
        if( !abox.isSatisfiable( notC ) ) {
            // if negation is unsat c itself is TOP
            knowns.addAll( getIndividuals() );
        }
        else if( abox.isSatisfiable( c ) ) {
            Set subs = isClassified() ? taxonomy.getSubs( c, false, true ) : SetUtils.EMPTY_SET;
            subs.remove( ATermUtils.BOTTOM );
            
            List unknowns = new ArrayList();
            Iterator i = individuals.iterator();
            while( i.hasNext() ) {                
                ATermAppl x = (ATermAppl) i.next();

                Bool isType = abox.isKnownType( x, c, subs );
                if( isType.isTrue() )
                    knowns.add( x );
                else if( isType.isUnknown() ) {
                    abox.isKnownType( x, c, subs );
                    unknowns.add( x );
                }
            }

            if( !unknowns.isEmpty() && abox.isType( unknowns, c ) ) {
                binaryInstanceRetrieval( c, unknowns, knowns ); 
            }
        }
        timer.stop();

        if( PelletOptions.CACHE_RETRIEVAL )
            instances.put( c, knowns );
        
        return knowns;
    }
	
//	private List filterList(ATermAppl c, List candidates, Collection results) {
//	    List filtered = candidates;
//	    
//	    Clash clash = abox.getLastClash();
//	    // if the clash is not dependant on a branch and the node is one of the candidates remove it
//	    if( clash.depends.isIndependent() && clash.isAtomic() && clash.args[0].equals(c) ) {
//	        int index = candidates.indexOf( clash.node.getName() );
//	        if( index >= 0 ) {
//	            System.out.println( 
//	                "Filter obvious instance " + clash.node + " while retrieving " + c );
//	            Collections.swap( candidates, index, 0 );
//	            results.add( candidates.get( 0 ) );
//	            filtered = candidates.subList( 1, candidates.size() );
//	        }
//	    }
//	    
//	    return filtered;
//	}
	
	public List retrieveIndividualsWithProperty(ATermAppl r) {
		ensureConsistency();
		
		List result = new ArrayList();
		Iterator i = abox.getIndIterator();
		while(i.hasNext()) {
			ATermAppl x = ((Individual) i.next()).getName();
			
			if( !abox.hasObviousPropertyValue( x, r, null ).isFalse() )
                result.add( x );
		}

		return result;
	}
	
	public void binaryInstanceRetrieval(ATermAppl c, List candidates, Collection results) {
		if( candidates.isEmpty() )
			return;
		else{
			List[] partitions = partition(candidates);
			partitionInstanceRetrieval(c, partitions, results);
		}
	}
	
	private void partitionInstanceRetrieval(ATermAppl c, List[] partitions, Collection results) {		
		if( partitions[0].size() == 1 ) {
			ATermAppl i = (ATermAppl) partitions[0].get(0);
			binaryInstanceRetrieval(c, partitions[1], results);

			if(isType(i, c))
				results.add(i);
		}
		else if( !abox.isType(partitions[0], c) ) {
			binaryInstanceRetrieval(c, partitions[1], results);
		}
		else {
//		    partitions[0] = filterList(c, partitions[0], results);
		    if( !abox.isType(partitions[1], c) ) {
				binaryInstanceRetrieval(c, partitions[0], results);
			}
			else {
//			    partitions[1] = filterList(c, partitions[1], results);
				binaryInstanceRetrieval(c, partitions[0], results);
				binaryInstanceRetrieval(c, partitions[1], results);
			}
		}
	}
	
	private List[] partition( List candidates ) {
		List[] partitions = new List[2];
		int n = candidates.size(); 
		if(n <= 1) {
			partitions[0] = candidates;
			partitions[1] = new ArrayList();
		}
		else {
			partitions[0] = candidates.subList(0, n/2);
			partitions[1] = candidates.subList(n/2, n);
		}
		
		return partitions;
	}
 
	
//	private List binarySubClassRetrieval(ATermAppl c, List candidates) {
//		if(candidates.isEmpty())
//			return new ArrayList();
//		else{
//			List[] partitions = partition(candidates);
//			return partitionSubClassRetrieval(c, partitions);
//		}
//	}
//	
//	private List partitionSubClassRetrieval(ATermAppl c, List[] partitions) {		
//		if(partitions[0].size() == 1) {
//			ATermAppl d = (ATermAppl) partitions[0].get(0);
//			List l = binarySubClassRetrieval(c, partitions[1]);
//
//			if(isSubclassOf(d, c))
//				l.add(d);
//			
//			return l;
//		}
//		else if(!abox.isSubClassOf(partitions[0], c))
//			return binarySubClassRetrieval(c, partitions[1]);
//		else if(!abox.isSubClassOf(partitions[1], c))
//			return binarySubClassRetrieval(c, partitions[0]);
//		else {
//			List l1 = binarySubClassRetrieval(c, partitions[0]);
//			List l2 = binarySubClassRetrieval(c, partitions[1]);
//			
//			l1.addAll(l2);
//			
//			return l1;
//		}
//	}

	/**
	 * Print the class hierarchy on the standard output.
	 */
	public void printClassTree() {
		classify();
		
		taxonomy.print();
	}
	
	public void printClassTree(OutputFormatter out) {
		classify();
		
		taxonomy.print( out );	    
	}
	
	public boolean doExplanation() {
		return abox.doExplanation();
	}
	
	/**
	 * @param doExplanation The doExplanation to set.
	 */
	public void setDoExplanation(boolean doExplanation) {
		abox.setDoExplanation(doExplanation);
	}
	
	public String getExplanation() {
		return abox.getExplanation();
	}
	
	public void setDoDependencyAxioms(boolean doDepAxioms) {		
	    if( log.isDebugEnabled() )
	        log.debug( "Setting DoDependencyAxioms = " + doDepAxioms);
	}
	
	public boolean getDoDependencyAxioms() {
	    return false;
	}
	
	public Set getExplanationSet() {
		return SetUtils.EMPTY_SET;
	}
	   
    /**
     * @param rbox The rbox to set.
     */
    public void setRBox(RBox rbox) {
        this.rbox = rbox;
    }
    
    /**
     * @param tbox The tbox to set.
     */
    public void setTBox(TBox tbox) {
        this.tbox = tbox;
    }
    
	/**
	 * Choose a completion strategy based on the expressivity of the KB. The
	 * abox given is not necessarily the ABox that belongs to this KB but
	 * can be a derivative.
	 * 
	 * @return
	 */
	CompletionStrategy chooseStrategy(ABox abox) {
		//if there are dl-safe rules present, use RuleStrategy which is a subclass of SHOIN
		// only problem is, we're using SHOIN everytime there are rule-  it is  faster to use SHN + Rules in some cases		
		if (this.getRules() != null) {
			return new RuleStrategy( abox );
		}
	    if( PelletOptions.DEFAULT_COMPLETION_STRATEGY != null ) {
	        Class[] types = new Class[] { ABox.class };
	        Object[] args = new Object[] { abox };
	        try {
	            Constructor cons = PelletOptions.DEFAULT_COMPLETION_STRATEGY.getConstructor( types );
                return (CompletionStrategy) cons.newInstance( args );
            } catch(Exception e) {
                e.printStackTrace();
                throw new InternalReasonerException(
                    "Failed to create the default completion strategy defined in PelletOptions!");
            }	        
	    }
	    else if( PelletOptions.USE_COMPLETION_STRATEGY ) {
            Expressivity expressivity = getExpressivity();

            boolean emptyStrategy = 
                (abox.size() == 1) &&
                ((Individual) abox.getIndIterator().next()).getOutEdges().isEmpty();
                  
            boolean fullDatatypeReasoning =
                PelletOptions.USE_FULL_DATATYPE_REASONING &&
                (expressivity.hasCardinalityD() || expressivity.hasKeys());

            if( !fullDatatypeReasoning ) {
                if( expressivity.hasNominal() ) {
                    if( expressivity.hasInverse() )
                        return new SHOINStrategy( abox );
                    else
                        return new SHONStrategy( abox );
                }
                else if( expressivity.hasInverse() )
                    return new SHINStrategy( abox );
                else if( emptyStrategy && !expressivity.hasCardinalityD() && !expressivity.hasKeys() )
                    return new EmptySHNStrategy( abox );
                else
                    return new SHNStrategy( abox );
            }
        }

        return new SHOINStrategy( abox );
	}
	
    /**
     * Returns the URI of the ontology this KB belongs to. A KB is not always 
     * guaranteed to have this value because 1) Parser does not always know
     * the URI of the onotlogy (i.e. Jena models) 2) Multiple ontologies may
     * be loaded to the same KB (in this case this value may return one of
     * the ontologies arbitrarily)
     * 
     * This value has no siginificance in reasoning except for EconnectedKB's.
     * 
     * @return 
     */
    public String getOntology() {
        return ontology;
    }
    
    /**
     * Set the URI of the ontology this Kb belongs to.
     * 
     * @param ontology 
     */
    public void setOntology(String ontology) {
        this.ontology = ontology;
    }
    
    /**
     * Set a timeout for the main timer. Used to stop an automated test after a
     * reasonable amount of time has passed.
     * 
     * @param timeout
     */
    public void setTimeout(long timeout) {
        timers.mainTimer.setTimeout(timeout);
    }

	/**
	 * @param term
	 * @return
	 */
	Role getRole(ATerm term) {
		return rbox.getRole(term);
	}	
	
	/**
	 * Get the classification results.
	 */
    public Taxonomy getTaxonomy() {
        classify();
        
        return taxonomy;
    }
    
    public TaxonomyBuilder getTaxonomyBuilder() {
        if( builder == null )
            builder = new TaxonomyBuilder( this );
        
        return builder;
    }
    
    public Taxonomy getRoleTaxonomy() {
        prepare();
        
        return rbox.getTaxonomy();
    }

    public SizeEstimate getSizeEstimate() {
        return estimate;
    }


	public void setRules(Set rules) {
		// TODO Auto-generated method stub
		this.rules = rules; 
	}
	
	public Set getRules() {
		// TODO Auto-generated method stub
		return this.rules;
	}
}
