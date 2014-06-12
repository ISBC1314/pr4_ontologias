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

package org.mindswap.pellet.jena;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.utils.AlphaNumericComparator;
import org.mindswap.pellet.utils.Cache;
import org.mindswap.pellet.utils.Namespaces;
import org.mindswap.pellet.utils.URIUtils;

import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFErrorHandler;
import com.hp.hpl.jena.rdf.model.RDFReader;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.OWL;

/**
 * Utility class to read ontologies into Jena models. It can be used to read
 * ontologies with all the imports. It will automatically ignore to load 
 * built-in ontologies like "http://www.w3.org/2002/07/owl#".
 * 
 * 
 * @author Evren Sirin
 *
 */
public class ModelReader {
    protected static Log log = LogFactory.getLog( OWLReasoner.class );

    /**
     * @deprecated Edit log4j.properties to turn on debugging
     */     
	public static boolean DEBUG = false;
	
	private Map modelCache = new HashMap();
	private Map modelNames = new HashMap();
	
	private Set ignoreImports = new HashSet();
	
	private RDFErrorHandler handler = null;
	
	private Cache cache = new Cache();
	
	/**
	 * 
	 */
	public ModelReader() {
		addLoaded(ignoreImports, Namespaces.OWL);
		addLoaded(ignoreImports, Namespaces.RDF);
		addLoaded(ignoreImports, Namespaces.RDFS);
	}
	
	private void addLoaded(Set loadedFiles, String uri) {
		uri = URIUtils.getNameSpace(uri);
		
		loadedFiles.add(uri);
	}
	
	private boolean isLoaded(Set loadedFiles, String uri) {
		uri = URIUtils.getNameSpace(uri);
		
		return loadedFiles.contains(uri) || ignoreImports.contains(uri);
	}

	private void readFile(List models, Set loadedFiles, String uri, String format, boolean withImports) {
		if (isLoaded(loadedFiles, uri))
			return;		

		Model model = (Model) modelCache.get(uri);
		if(model == null) {
			model = ModelFactory.createDefaultModel();
			try {
				if( log.isDebugEnabled() ) log.debug("Reading " + uri);
				InputStream in = createInputStream(uri);
				
				RDFReader reader = model.getReader(format);
				if(handler != null)
				    reader.setErrorHandler( handler );
				reader.setProperty( "WARN_REDEFINITION_OF_ID", "EM_IGNORE" );
                reader.setProperty( "WARN_BAD_NAME", "EM_IGNORE" );
				
				reader.read(model, in, uri.toString());
				in.close();
				modelNames.put(model, uri);
				modelCache.put(uri, model);
			} catch (IOException e) {
				if(loadedFiles.isEmpty())
					throw new RuntimeException(e);
				else {
					if(handler != null) 
						handler.error(e);
//					else
//						System.err.println(e);
//					System.err.println("WARNING: The import file " + uri + " cannot be parsed");					
				}
			}
		}
		addLoaded(loadedFiles, uri);
		models.add(model);
		
		if(withImports) {
			StmtIterator i = model.listStatements(null, OWL.imports, (Resource) null);
			while (i.hasNext()) {
				Statement stmt = i.nextStatement();
				String importFile = stmt.getResource().toString();
				
				readFile(models, loadedFiles, importFile, format, withImports);
			} // while
		}
	}

	/* (non-Javadoc)
	 * @see org.mindswap.owl.OWLParser#parseSeperate(java.lang.String)
	 */
	public Model[] readSeparate(String url, String format) {
		List models = new ArrayList();
		
		Set loadedFiles = new HashSet();
		readFile(models, loadedFiles, url, format, true);
				
		return (Model []) models.toArray(new Model[models.size()]);
	}

	/* (non-Javadoc)
	 * @see org.mindswap.owl.OWLParser#parseSeperate(java.lang.String)
	 */
	private Model readSingle(String url, String format) {
		List models = new ArrayList();
		
		Set loadedFiles = new HashSet();
		readFile(models, loadedFiles, url, format, false);
		
		return (Model) models.get(0);
	}
	
	public String getURI(Model model) {
		return (String) modelNames.get(model);
	}
	
	private Model mergeAll(Model[] models) {
		Model model = ModelFactory.createDefaultModel();
		for(int i = 0; i < models.length; i++)
			model.add(models[i], false);
		
		return model;
	}

	private InputStream createInputStream(String uri) throws FileNotFoundException {
		InputStream in = null;		
		if(cache.isForced()) {
			File cachedFile = cache.getCachedFile(uri);
			if(cachedFile != null)  {
				System.err.println("WARNING: Force using cached file " + cachedFile);
				in = new FileInputStream(cachedFile);
			}
			else {
				try {
					in = new URI(uri).toURL().openConnection().getInputStream();
				} catch(Exception e) {
//					System.err.println("WARNING: Cannot read file " + uri);
					throw new FileNotFoundException("The file " + uri + " cannot be parsed");
				}
			}
		}
		else {
			try {
				in = new URI(uri).toURL().openConnection().getInputStream();
			} catch(Exception e) {
				System.err.println("WARNING: Cannot read file " + uri);
				File cachedFile = cache.getCachedFile(uri.toString());
				if(cachedFile != null)  {
					System.err.println("WARNING: Try cached file " + cachedFile);
					in = new FileInputStream(cachedFile);
				}
				else
					throw new FileNotFoundException("The file " + uri + " cannot be parsed");				
			}
		}
				
		return in;
	}

	
	public Model read(String uri) {		
		return read(uri, "RDF/XML");
	}
	
	public Model read(String uri, String format) {		
		return read(uri, format, true);
	}

	public Model read(String uri, boolean withImports) {
		return read(uri, "RDF/XML", withImports);
	}
	
	public Model read(String uri, String format, boolean withImports)  {
		Model model = null;
		
		if(withImports)
			model = mergeAll(readSeparate(uri, format));
		else 
			model = readSingle(uri, format);
		
		return model;
	}
	
	public static Model read( String loc, final String pattern, int limit ) throws RuntimeException {	        
        Model model = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM );
        URL url;
        try {
            url = new URL( loc );
        }
        catch( MalformedURLException e ) {
            throw new RuntimeException( e );
        }
        if( url.getProtocol().equals( "file" ) ) {
            File file = new File( url.getPath() );
            if(file.exists()) {
                if(file.isDirectory()) {
            		File[] files = file.listFiles( new FilenameFilter() {
            			public boolean accept(File dir, String name) {
            				return dir != null && name.matches( pattern );
            			}			
            		});		            
            		Arrays.sort( files, AlphaNumericComparator.CASE_INSENSITIVE );
            		int size = (files.length > limit) ? limit : files.length;
		    		for (int j = 0; j < size; j++) {
		    			String fileURI = files[j].toURI().toString();
//		    			System.out.println("Reading file " + fileURI);	
		    			model.read( fileURI );
		    		}
                }
                else  {
                    model.read( loc );
                }
            }            
        }
        else  {
            model.read( loc );
        }

        return model;
	   }
	   
	/**
	 * 
	 * read
	 * 
	 * @param in
	 * @return
	 */
	public Model read(InputStream in) {
		return read(in, "RDF/XML");
	}

	public Model read(InputStream in, String format) {
		return read(in, format, "");
	}

	public Model read(InputStream in, String format, String base) {	    
		return read(in, format, base, true);
	}
	
	public Model read(InputStream in, String format, String base, boolean withImports) {	    
		Model model = ModelFactory.createDefaultModel();
		RDFReader reader = model.getReader(format);
		reader.setErrorHandler(handler);
		reader.read(model, in, base);
		
		if(withImports) {
			List models = new ArrayList();			
			Set loadedFiles = new HashSet();
			
			StmtIterator i = model.listStatements(null, OWL.imports, (Resource) null);
			while (i.hasNext()) {
				Statement stmt = i.nextStatement();
				String importFile = stmt.getResource().toString();
				
				readFile(models, loadedFiles, importFile, format, withImports);
			} // while
			
			for(int j = 0; j < models.size(); j++)
				model.add((Model) models.get(j), false);
		}
		
		return model;
	}

	/* (non-Javadoc)
	 * @see org.mindswap.owl.OWLReader#setErrorHandler(com.hp.hpl.jena.rdf.model.RDFErrorHandler)
	 */
	public RDFErrorHandler setErrorHandler(RDFErrorHandler errHandler) {
		RDFErrorHandler old = handler;
		handler = errHandler;
		return old;
	}
	
	/**
	 * @return Returns the cache.
	 */
	public Cache getCache() {
		return cache;
	}
	
	/**
	 * @param cache The cache to set.
	 */
	public void setCache(Cache cache) {
		this.cache = cache;
	}
}
