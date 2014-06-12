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

/*
 * Created on Jan 10, 2004
 */
package org.mindswap.pellet.owlapi;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mindswap.pellet.EconnectedKB;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.Role;
import org.mindswap.pellet.utils.ATermUtils;
import org.semanticweb.owl.inference.OWLClassReasoner;
import org.semanticweb.owl.inference.OWLConsistencyChecker;
import org.semanticweb.owl.inference.OWLIndividualReasoner;
import org.semanticweb.owl.inference.OWLReasoner;
import org.semanticweb.owl.inference.OWLTaxonomyReasoner;
import org.semanticweb.owl.io.vocabulary.XMLSchemaSimpleDatatypeVocabulary;
import org.semanticweb.owl.model.OWLAnnotationProperty;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLDataProperty;
import org.semanticweb.owl.model.OWLDataType;
import org.semanticweb.owl.model.OWLDataValue;
import org.semanticweb.owl.model.OWLDescription;
import org.semanticweb.owl.model.OWLException;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLObject;
import org.semanticweb.owl.model.OWLObjectProperty;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLProperty;
import org.semanticweb.owl.model.change.OntologyChange;
import org.semanticweb.owl.model.change.OntologyChangeListener;

import aterm.ATermAppl;

/**
 * @author evren
 */
public class Reasoner implements 
	OWLReasoner, OWLConsistencyChecker, OWLClassReasoner,
	OWLTaxonomyReasoner, OWLIndividualReasoner, OntologyChangeListener 
{
	/**
     * 
     */
    private static final long serialVersionUID = 8438190652175258123L;

    private static XMLSchemaSimpleDatatypeVocabulary XSD = XMLSchemaSimpleDatatypeVocabulary.INSTANCE;
	
	final public static int UNKNOWN = -1;

	private PelletLoader loader;
	
	private OWLOntology ontology;
	private Set ontologies;
	
	protected KnowledgeBase kb;
	
	int consistent = OWLConsistencyChecker.UNKNOWN;
	
	boolean autoClassify = false;
	boolean autoRealize = false;
	
	public Reasoner() {
	    kb = new KnowledgeBase();
		loader = new PelletLoader(kb);
	}

	public boolean loadImports() {
		return loader.loadImports();
	}

	/**
	 * @param useImports The useImports to set.
	 */
	public void setLoadImports(boolean loadImports, boolean refreshOnt) throws OWLException {
		loader.setLoadImports(loadImports);
		if (refreshOnt) refreshOntology();
	}
	
	public void refreshOntology() throws OWLException {
		if(ontology != null) 
			setOntology(ontology);		
	}
	
	/* (non-Javadoc)
	 * @see org.semanticweb.owl.inference.OWLReasoner#setOntology(org.semanticweb.owl.model.OWLOntology)
	 */
	public void setOntology(OWLOntology ontology) throws OWLException {
		this.ontology = ontology;
	
		if(!ontology.getForeignOntologies().isEmpty())
		    loader.setKB(new EconnectedKB());
		
		loader.reset();
		loader.load(ontology);
				
		kb = loader.getKB();
		
		ontologies = loader.getOntologies();
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owl.inference.OWLReasoner#getOntology()
	 */
	public OWLOntology getOntology() {
		return ontology;
	}

	/**
	 * Returns the set of all loaded ontologies.
	 */
	public Set getOntologies() {
		return ontologies;
	}
	
	/* (non-Javadoc)
	 * @see org.semanticweb.owl.inference.OWLConsistencyChecker#consistency(org.semanticweb.owl.model.OWLOntology)
	 */
	public int consistency(OWLOntology ontology) throws OWLException {
		setOntology(ontology);
			
		return consistency();
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owl.inference.OWLConsistencyChecker#consistency(org.semanticweb.owl.model.OWLOntology)
	 */
	public int consistency() {		
		return consistent;
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owl.inference.OWLTaxonomyReasoner#superClassesOf(org.semanticweb.owl.model.OWLClass)
	 */
	public Set superClassesOf(OWLClass c) throws OWLException {
		return toOWLEntitySetOfSet(kb.getSuperClasses(loader.term(c), true), OWLClass.class);
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owl.inference.OWLTaxonomyReasoner#ancestorClassesOf(org.semanticweb.owl.model.OWLClass)
	 */
	public Set ancestorClassesOf(OWLClass c) throws OWLException {
		return toOWLEntitySetOfSet(kb.getSuperClasses(loader.term(c)), OWLClass.class);
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owl.inference.OWLTaxonomyReasoner#subClassesOf(org.semanticweb.owl.model.OWLClass)
	 */
	public Set subClassesOf(OWLClass c) throws OWLException {
		return toOWLEntitySetOfSet(kb.getSubClasses(loader.term(c), true), OWLClass.class);
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owl.inference.OWLTaxonomyReasoner#descendantClassesOf(org.semanticweb.owl.model.OWLClass)
	 */
	public Set descendantClassesOf(OWLClass c) throws OWLException {
		return toOWLEntitySetOfSet(kb.getSubClasses(loader.term(c)), OWLClass.class);
	}

    public Set equivalentClassesOf(OWLClass c) throws OWLException {
		return toOWLEntitySet(kb.getEquivalentClasses(loader.term(c)), OWLClass.class);
		
	}

	public boolean isInstanceOf(OWLIndividual ind, OWLClass c) throws OWLException {
		return kb.isType(loader.term(ind), loader.term(c));
	}

	public boolean isInstanceOf(OWLIndividual ind, OWLDescription d) throws OWLException {
		return kb.isType(loader.term(ind), loader.term(d));
	}

	
	/**
	 * 
	 * Return all the instances of this class.
	 * 
	 * @param c
	 * @return
	 */
	public Set allInstancesOf(OWLClass c) throws OWLException {
		return toOWLEntitySet(kb.getInstances(loader.term(c)), OWLIndividual.class);
	}
	
	/**
	 * 
	 * Return direct instances of this class
	 * 
	 * @param c
	 * @return
	 */
	public Set instancesOf(OWLClass c) throws OWLException {
		return toOWLEntitySet(kb.getInstances(loader.term(c), true), OWLIndividual.class);
	}

	/**
	 * 
	 * Return all instances of this concept description
	 * 
	 * @param c
	 * @return
	 */
	public Set instancesOf(OWLDescription d) throws OWLException {
		return toOWLEntitySet(kb.getInstances(loader.term(d)), OWLIndividual.class);
	}

	public void ontologyChanged(OntologyChange change) throws OWLException {
		refreshOntology();
	}	
	
	/**
	 * 
	 * Return the set of all named classes defined in any of the ontologies loaded in the reasoner.
	 * 
	 * @return set of OWLClass objects
	 */
	public Set getClasses() {
		try {
			return toOWLEntitySet(kb.getClasses(), OWLClass.class);		
		} catch (OWLException e) {
			e.printStackTrace();
		}
		
		return Collections.EMPTY_SET;
	}
	

	/* (non-Javadoc)
	 * @see org.mindswap.swoop.SwoopReasoner#getProperties()
	 */
	public Set getProperties() {
		try {
			return toOWLEntitySet(kb.getProperties(), OWLProperty.class);
		} catch (OWLException e) {
			e.printStackTrace();
		}
		
		return Collections.EMPTY_SET;
	}

	/* (non-Javadoc)
	 * @see org.mindswap.swoop.SwoopReasoner#getObjectProperties()
	 */
	public Set getObjectProperties() {
		Set set = new HashSet();
		Iterator props = getProperties().iterator();
		while(props.hasNext()) {
			OWLProperty prop = (OWLProperty) props.next();
			if(prop instanceof OWLObjectProperty)
				set.add(prop);
		}
		return set;
	}

	public OWLIndividual getPropertyValue(OWLIndividual ind, OWLObjectProperty prop) throws OWLException {
	    Set values = getPropertyValues(ind, prop);
		return values.isEmpty() ? null : (OWLIndividual) values.iterator().next();
	}
	
	public OWLDataValue getPropertyValue(OWLIndividual ind, OWLDataProperty prop) throws OWLException {
	    Set values = getPropertyValues(ind, prop);
		return values.isEmpty() ? null : (OWLDataValue) values.iterator().next();
	}
	
	public Set getPropertyValues(OWLIndividual ind, OWLObjectProperty prop) throws OWLException {
		return toOWLEntitySet(
		    kb.getObjectPropertyValues(loader.term(prop), loader.term(ind)), OWLIndividual.class);
	}
	
	public Set getPropertyValues(OWLIndividual ind, OWLDataProperty prop) throws OWLException {
		return toOWLEntitySet(
		    kb.getDataPropertyValues(loader.term(prop), loader.term(ind)), OWLDataValue.class);
	}
	
	public Set getPropertyValues(OWLIndividual ind, OWLProperty prop) throws OWLException {
	    if( prop instanceof OWLObjectProperty )
	        return getPropertyValues(ind, (OWLObjectProperty) prop);
	    else if( prop instanceof OWLDataProperty )
	        return getPropertyValues(ind, (OWLDataProperty) prop);
	    else
	        throw new OWLException("Property " + prop + " is neither data nor object property!");
	}
	
	public Map getDataPropertyValues(OWLIndividual ind) throws OWLException {
	    Map values = new HashMap();
	    Set dataProps = getDataProperties();
	    for(Iterator i = dataProps.iterator(); i.hasNext();) {
            OWLDataProperty prop = (OWLDataProperty) i.next();
            Set set = getPropertyValues( ind, prop );
            if( !set.isEmpty() )
                values.put( prop, set );
        }
	    
	    return values;
	}
	
	public Map getObjectPropertyValues(OWLIndividual ind) throws OWLException {
	    Map values = new HashMap();
	    Set objProps = getObjectProperties();
	    for(Iterator i = objProps.iterator(); i.hasNext();) {
            OWLObjectProperty prop = (OWLObjectProperty) i.next();
            Set set = getPropertyValues( ind, prop );
            if( !set.isEmpty() )
                values.put( prop, set );
        }
	    
	    return values;
	}	
	
	public Map getPropertyValues(OWLObjectProperty prop) throws OWLException {
	    Map result = new HashMap();
	    ATermAppl p = loader.term(prop);
	    
	    Map values = kb.getPropertyValues( p );
        for(Iterator i = values.keySet().iterator(); i.hasNext();) {
            ATermAppl subjTerm = (ATermAppl) i.next();
            
            Collection objTerms = ((Collection) values.get( subjTerm ));
            
            OWLIndividual subj = (OWLIndividual)
	            getEntity( URI.create( subjTerm.getName() ), OWLIndividual.class );
            
	        Set objects = toOWLEntitySet( objTerms, OWLIndividual.class);
	        
	        result.put( subj, objects );
        } 
        
		return result;
	}
	
	public Map getPropertyValues(OWLDataProperty prop) throws OWLException {
	    Map map = new HashMap();
	    ATermAppl p = loader.term(prop);
        Collection candidates = kb.retrieveIndividualsWithProperty( p );
        for( Iterator i = candidates.iterator(); i.hasNext(); ) {
            ATermAppl candidate = (ATermAppl) i.next();
            List list = kb.getDataPropertyValues( p, candidate );
            if( list.isEmpty() )
                continue;
            
            OWLIndividual subj = (OWLIndividual)
                getEntity(URI.create(candidate.getName()), OWLIndividual.class);
            Set objects = toOWLEntitySet(list, OWLDataValue.class);
            
            map.put( subj, objects );
        }  
        
		return map;
	}
	
	public Map getPropertyValues(OWLProperty prop) throws OWLException {
	    if( prop instanceof OWLObjectProperty )
	        return getPropertyValues((OWLObjectProperty) prop);
	    else if( prop instanceof OWLDataProperty )
	        return getPropertyValues((OWLDataProperty) prop);
	    else
	        throw new OWLException("Property " + prop + " is neither data nor object property!");
	}
	
	public boolean hasPropertyValue(OWLIndividual subj, OWLObjectProperty prop, OWLIndividual obj) throws OWLException {
		return kb.hasPropertyValue(loader.term(subj), loader.term(prop), loader.term(obj));
	}
	
	public boolean hasPropertyValue(OWLIndividual subj, OWLDataProperty prop, OWLDataValue obj) throws OWLException {
		return kb.hasPropertyValue(loader.term(subj), loader.term(prop), loader.term(obj));
	}


	/* (non-Javadoc)
	 * @see org.mindswap.swoop.SwoopReasoner#getDataProperties()
	 */
	public Set getDataProperties() {
		Set set = new HashSet();
		Iterator props = getProperties().iterator();
		while(props.hasNext()) {
			OWLProperty prop = (OWLProperty) props.next();
			if(prop instanceof OWLDataProperty)
				set.add(prop);
		}
		return set;
	}

	/* (non-Javadoc)
	 * @see org.mindswap.swoop.SwoopReasoner#getAnnotationProperties()
	 */
	public Set getAnnotationProperties() {
		Set set = new HashSet();
		try {
			Iterator ont = getOntologies().iterator();
			while(ont.hasNext()) {
				OWLOntology o = (OWLOntology) ont.next();
				set.addAll(o.getAnnotationProperties());
			}
		} catch (OWLException e) {
			e.printStackTrace();
		}
		return set;
	}
	
	/**
	 * 
	 * Return the set of all individuals defined in any of the ontologies loaded in the reasoner.
	 * 
	 * @return set of OWLClass objects
	 */		
	public Set getIndividuals() {
		try {
			return toOWLEntitySet(kb.getIndividuals(), OWLIndividual.class);
		} catch (OWLException e) {
			e.printStackTrace();
		}
		
		return Collections.EMPTY_SET;
	}
	
	public OWLClass getClass(URI uri) throws OWLException {
		return (OWLClass) getEntity(uri, OWLClass.class);
	}
	
	public OWLObjectProperty getObjectProperty(URI uri) throws OWLException {
		return (OWLObjectProperty) getEntity(uri, OWLObjectProperty.class);
	}
	
	public OWLDataProperty getDataProperty(URI uri) throws OWLException {
		return (OWLDataProperty) getEntity(uri, OWLDataProperty.class);
	}
	
	public OWLIndividual getIndividual(URI uri) throws OWLException {
		return (OWLIndividual) getEntity(uri, OWLIndividual.class);
	}
	
	public OWLObject getEntity(URI uri, Class type) throws OWLException {
		OWLObject entity = null;
		Iterator i = ontologies.iterator();
		while(entity == null && i.hasNext()) {
			OWLOntology o = (OWLOntology) i .next();
			if(entity == null && type.isAssignableFrom(OWLClass.class)) entity = o.getClass(uri);
			if(entity == null && type.isAssignableFrom(OWLDataType.class)) entity = o.getDatatype(uri);
			// XSD datatypes are not put into the ontology so previous statement returns null
			// Let's check manually if the uri belongs to XSD and get the datatype object
			// from the factory directly
			if(entity == null && type.isAssignableFrom(OWLDataType.class) && XSD.getDatatypes().contains(uri.toString())) 
				entity = o.getOWLDataFactory().getOWLConcreteDataType(uri);
			if(entity == null && type.isAssignableFrom(OWLObjectProperty.class)) entity = o.getObjectProperty(uri);
			if(entity == null && type.isAssignableFrom(OWLDataProperty.class)) entity = o.getDataProperty(uri);
			if(entity == null && type.isAssignableFrom(OWLIndividual.class)) entity = o.getIndividual(uri);
			if(entity == null && type.isAssignableFrom(OWLAnnotationProperty.class)) entity = o.getAnnotationProperty(uri);
		}
		
		return entity;
	}
	
	private Set toOWLEntitySetOfSet(Set set, Class type) throws OWLException {
		Set results = new HashSet();
		Iterator i = set.iterator();
		while(i.hasNext()) {
		    Set entitySet = toOWLEntitySet((Set) i.next(), type);
		    if(!entitySet.isEmpty())
		        results.add(entitySet);		
		}

		return results;		
	}	

	protected Set toOWLEntitySet(Collection set, Class type) throws OWLException {
		Set results = new HashSet();
		Iterator i = set.iterator();
		while(i.hasNext()) {
		    Object obj = i.next();
		    
			OWLObject e = null;
		    if(obj instanceof Role) {
				try {
					URI uri = new URI(((Role) obj).getName().getName());
			        e = getEntity(uri, type);
				} catch (URISyntaxException x) {
					throw new OWLException("Cannot create URI from term " + x);
				}		
		    }
		    else {
				ATermAppl term = (ATermAppl) obj;
				
				if(term.equals(ATermUtils.TOP))
					e = ontology.getOWLDataFactory().getOWLThing();
				else if(term.equals(ATermUtils.BOTTOM))
					e = ontology.getOWLDataFactory().getOWLNothing();
				else if(ATermUtils.isLiteral(term)) {
				    String value = ((ATermAppl) term.getArgument( 0 )).getName();
			        String lang = ((ATermAppl) term.getArgument( 1 )).getName();
			        String uri = ((ATermAppl) term.getArgument( 2 )).getName();			    
			        URI datatypeURI = uri.equals( "" ) ? null : URI.create( uri );

			        if( datatypeURI != null )
			            e =	ontology.getOWLDataFactory().getOWLConcreteData(datatypeURI, null, value);
			        else if( lang.equals( "" ) )
			            e =	ontology.getOWLDataFactory().getOWLConcreteData(null, null, value);
			        else
			            e =	ontology.getOWLDataFactory().getOWLConcreteData(null, lang, value);
				}
				else if(term.getArity() == 0) {
					URI uri;
					try {
						uri = new URI(term.getName());
					} catch (URISyntaxException x) {
						throw new OWLException("Cannot create URI from term " + x);
					}		
					
					e = getEntity(uri, type);
				}
		    }
						
			if(e == null) continue;
			results.add(e);
		}

		return results;		
	}	

	/* (non-Javadoc)
	 * @see org.semanticweb.owl.inference.OWLClassReasoner#isSubClassOf(org.semanticweb.owl.model.OWLDescription, org.semanticweb.owl.model.OWLDescription)
	 */
	public boolean isSubClassOf(OWLDescription c1, OWLDescription c2) throws OWLException {
		return kb.isSubClassOf(loader.term(c1), loader.term(c2));
	}
	
	public boolean isSubTypeOf(OWLDataType d1, OWLDataType d2) throws OWLException {
		return kb.isSubClassOf(loader.term(d1), loader.term(d2));
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owl.inference.OWLClassReasoner#isEquivalentClass(org.semanticweb.owl.model.OWLDescription, org.semanticweb.owl.model.OWLDescription)
	 */
	public boolean isEquivalentClass(OWLDescription c1, OWLDescription c2) throws OWLException {
		return kb.isEquivalentClass(loader.term(c1), loader.term(c2));
	}

	/**
	 * Returns true if the loaded ontology is consistent. 
	 * 
	 * @param c
	 * @return
	 * @throws OWLException
	 */
	public boolean isConsistent() {
		return kb.isConsistent();
	}
	
	/**
	 * Returns true if the given class is consistent.
	 * 
	 * @param c
	 * @return
	 * @throws OWLException
	 */
	public boolean isConsistent(OWLDescription d) throws OWLException {
		if (!kb.isConsistent()) return false;
		try {
			return kb.isSatisfiable(loader.term(d));
		} catch (Exception e) {
		    e.printStackTrace();
			throw new OWLException(e.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owl.inference.OWLClassReasoner#superClassesOf(org.semanticweb.owl.model.OWLDescription)
	 */
	public Set superClassesOf(OWLDescription c) throws OWLException {
		return toOWLEntitySetOfSet(kb.getSuperClasses(loader.term(c), true), OWLClass.class);
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owl.inference.OWLClassReasoner#ancestorClassesOf(org.semanticweb.owl.model.OWLDescription)
	 */
	public Set ancestorClassesOf(OWLDescription c) throws OWLException {
		return toOWLEntitySetOfSet(kb.getSuperClasses(loader.term(c)), OWLClass.class);
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owl.inference.OWLClassReasoner#subClassesOf(org.semanticweb.owl.model.OWLDescription)
	 */
	public Set subClassesOf(OWLDescription c) throws OWLException {
		return toOWLEntitySetOfSet(kb.getSubClasses(loader.term(c), true), OWLClass.class);
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owl.inference.OWLClassReasoner#descendantClassesOf(org.semanticweb.owl.model.OWLDescription)
	 */
	public Set descendantClassesOf(OWLDescription c) throws OWLException {
		return toOWLEntitySetOfSet(kb.getSubClasses(loader.term(c)), OWLClass.class);
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owl.inference.OWLClassReasoner#equivalentClassesOf(org.semanticweb.owl.model.OWLDescription)
	 */
	public Set equivalentClassesOf(OWLDescription c) throws OWLException {
		return toOWLEntitySet(kb.getEquivalentClasses(loader.term(c)), OWLClass.class);
	}	
	
	/**
	 * @return Returns the autoClassify.
	 */
	public boolean isAutoClassify() {
		return autoClassify;
	}

	/**
	 * @param autoClassify The autoClassify to set.
	 */
	public void setAutoClassify(boolean autoClassify) {
		this.autoClassify = autoClassify;
	}

	/**
	 * @return Returns the autoRealize.
	 */
	public boolean isAutoRealize() {
		return autoRealize;
	}

	/**
	 * @param autoRealize The autoRealize to set.
	 */
	public void setAutoRealize(boolean autoRealize) {
		this.autoRealize = autoRealize;
	}

	/**
	 * @return Returns the kb.
	 */
	public KnowledgeBase getKB() {
		return kb;
	}
	

	public Set superPropertiesOf(OWLProperty prop) throws OWLException {
		return toOWLEntitySetOfSet(kb.getSuperProperties(loader.term(prop), true), OWLProperty.class);
	}


	public Set ancestorPropertiesOf(OWLProperty prop) throws OWLException {
		return toOWLEntitySetOfSet(kb.getSuperProperties(loader.term(prop)), OWLProperty.class);
	}

	public Set subPropertiesOf(OWLProperty prop) throws OWLException {
		return toOWLEntitySetOfSet(kb.getSubProperties(loader.term(prop), true), OWLProperty.class);
	}

	public Set descendantPropertiesOf(OWLProperty prop) throws OWLException {
		return toOWLEntitySetOfSet(kb.getSubProperties(loader.term(prop)), OWLProperty.class);
	}

	public Set equivalentPropertiesOf(OWLProperty prop) throws OWLException {
		return toOWLEntitySet(kb.getEquivalentProperties(loader.term(prop)), OWLProperty.class);
	}

	public Set inversePropertiesOf(OWLObjectProperty prop) throws OWLException {
		return toOWLEntitySet(kb.getInverses(loader.term(prop)), OWLProperty.class);
	}	

	public Set rangesOf(OWLProperty prop) throws OWLException {
	    if( prop instanceof OWLObjectProperty )
	        return toOWLEntitySet(kb.getRanges(loader.term(prop)), OWLClass.class);
	    else
	        return toOWLEntitySet(kb.getRanges(loader.term(prop)), OWLDataType.class);
	}

	public Set domainsOf(OWLProperty prop) throws OWLException {
        return toOWLEntitySet(kb.getDomains(loader.term(prop)), OWLClass.class);
	}

	/**
	 * 
	 * Return the named class that this individual is a direct type of. If there is more than 
	 * one such class first one is returned.
	 * 
	 * @param ind
	 * @return OWLClass
	 * @throws OWLException
	 */
	public OWLClass typeOf(OWLIndividual ind) throws OWLException {
	    Set types = typesOf(ind);
	    
	    // this is a set of sets so get the first set
	    types = types.isEmpty() ? types : (Set) types.iterator().next();
	    
		return types.isEmpty() ? null : (OWLClass) types.iterator().next();
	}
	
	/**
	 * 
	 * Returns all the named classes that this individual is a direct type of. This returns a set of
	 * sets where each set is an equivalent class.
	 * 
	 * @param ind
	 * @return Set of OWLClass sets
	 * @throws OWLException
	 */
	public Set typesOf(OWLIndividual ind) throws OWLException {
		return toOWLEntitySetOfSet(kb.getTypes(loader.term(ind), true), OWLClass.class);
	}

	/**
	 * 
	 * Returns all the named classes that this individual belongs. This returns a set of
	 * sets where each set is an equivalent class
	 * 
	 * @param ind
	 * @return Set of OWLDescription objects
	 * @throws OWLException
	 */
	public Set allTypesOf(OWLIndividual ind) throws OWLException {
		return toOWLEntitySetOfSet(kb.getTypes(loader.term(ind)), OWLClass.class);
	}
	
	/**
	 * Return a set of sameAs individuals given a specific individual
	 * based on axioms in the ontology
	 * @param ind - specific individual to test
	 * @return
	 * @throws OWLException
	 */
	public Set getSameAsIndividuals(OWLIndividual ind) throws OWLException {
	    return toOWLEntitySet(kb.getSames(loader.term(ind)), OWLIndividual.class);
	}	

    /**
     * Test if two individuals are owl:DifferentFrom each other.
     * 
     * @return
     * @throws OWLException
     */
    public boolean isSameAs(OWLIndividual ind1, OWLIndividual ind2) throws OWLException {
        return kb.isSameAs(loader.term(ind1),loader.term(ind2));
    }

    /**
     * Test if two individuals are owl:DifferentFrom each other.
     * 
     * @return
     * @throws OWLException
     */
    public boolean isDifferentFrom(OWLIndividual ind1, OWLIndividual ind2) throws OWLException {
        return kb.isDifferentFrom(loader.term(ind1),loader.term(ind2));
    }
}
