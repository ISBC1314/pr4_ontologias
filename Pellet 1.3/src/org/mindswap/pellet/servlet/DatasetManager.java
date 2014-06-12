/*
 * Created on Jul 27, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.mindswap.pellet.servlet;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.WeakHashMap;

import org.mindswap.pellet.jena.PelletReasonerFactory;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.DataSource;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * @author ronwalf
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class DatasetManager {

	private Map onLoan;
	private List cache;
	private int size = 0;
	
	private Set defaultBackground = null;
	private Set defaultNamed = null;
	
	private class DatasetAssociation {
		public Object id;
		public Dataset dataset;
		
		public DatasetAssociation(Object id, Dataset dataset) {
			this.id = id;
			this.dataset = dataset;
		}
	}
	
	public DatasetManager(int size) {
		onLoan = new WeakHashMap();
		cache = new Vector(size);
		this.size = size;
		
		defaultBackground = new HashSet();
		defaultNamed = new HashSet();
	}
	
	
	public Dataset getDataset(Set backgroundGraphs, Set namedGraphs) {
		DatasetAssociation association = null;
		
		if (backgroundGraphs.size() == 0 && namedGraphs.size() == 0) {
			// Use default graphs
			backgroundGraphs = defaultBackground;
			namedGraphs = defaultNamed;
		}
		
		List setId = Arrays.asList(new Set[] {backgroundGraphs, namedGraphs});
        synchronized(cache) {
			for (Iterator iter=cache.iterator(); iter.hasNext();) {
				DatasetAssociation current = (DatasetAssociation) iter.next();
				if (current.id.equals(setId)) {
					System.err.println("Found setId.");
                    association = current;
                    iter.remove();
					break;
				}
			}
		}
		
		if (association == null) {
			Dataset dataset = create(backgroundGraphs, namedGraphs);
			association = new DatasetAssociation(setId, dataset);
		}
		
		synchronized(onLoan) {
			onLoan.put(association.dataset, association);
		}
		return association.dataset;
	}
	
	protected DataSource create(Set background, Set named) {
		System.out.println("Loading graphs: "+background+", "+named);
		DataSource datasource = DatasetFactory.create();
		
		OntModel ontModel = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );
		for (Iterator uriIter = background.iterator(); uriIter.hasNext();) {
			String uri = (String) uriIter.next();
			try {
				ontModel.read(uri);
			} catch (Exception e) {
				System.out.println("Couldn't load graph: "+uri);
			}
		}
		datasource.setDefaultModel(ontModel);
		
		for (Iterator uriIter = named.iterator(); uriIter.hasNext();) {
			String uri = (String) uriIter.next();
			Model model = ModelFactory.createDefaultModel();
			try {
				model.read(uri);
			} catch (Exception e) {
				System.out.println("Couldn't load named graph: "+uri);
			}
			datasource.addNamedModel(uri, model);
		}
		
		/*
		for (Iterator uriIter = background.iterator(); uriIter.hasNext();) {
			String uri = (String) uriIter.next();
			if (!named.contains(uri)) {
				Model model = ModelFactory.createDefaultModel();
				model.read(uri);
				datasource.addNamedModel(uri, model);
			}
		}
		
		for (Iterator uriIter = ontModel.listImportedOntologyURIs(true).iterator(); uriIter.hasNext();) {
			String uri = (String) uriIter.next();
			if (!named.contains(uri) && !background.contains(uri)) {
				Model model = ModelFactory.createDefaultModel();
				model.read(uri);
				datasource.addNamedModel(uri, model);
			}
		}
		*/
		return datasource;
	}
	
	public void reset() {
		onLoan = new WeakHashMap();
		cache = new Vector(size);
	}
	
	public void returnDataset(Dataset dataset) {
		DatasetAssociation association = null;
		synchronized(onLoan) {
			if (!onLoan.containsKey(dataset)) {
				System.out.println("Warning: dataset not on loan!");
				return;
			}
			association = (DatasetAssociation) onLoan.get(dataset);
		}

		synchronized(cache) {
			cache.add(association);
			while (cache.size() > size) {
				System.out.println("Pushing "+((DatasetAssociation)cache.get(0)).id + " off the cache.");
				cache.remove(0);
			}
		}
	}
	
	public void setDefaultDataset(Set background, Set named) {
		defaultBackground = background;
		defaultNamed = named;
	}
}
