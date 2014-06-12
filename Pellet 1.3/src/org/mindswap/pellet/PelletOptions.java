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
 * Created on May 6, 2004
 */
package org.mindswap.pellet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class contains options used throughout different modules of the reasoner. Setting
 * one of the values should have effect in the behavior of the reasoner regardless of whether
 * it is based on Jena or OWL-API (though some options are applicable only in one implementation). 
 * Some of these options are to control experimental extensions to the reasoner and may be 
 * removed in future releases as these features are completely tested and integrated.
 * 
 * @author Evren Sirin
 */
public class PelletOptions {
    protected static Log log = LogFactory.getLog( PelletOptions.class );

    private static void load( URL configFile ) {
        log.info( "Reading Pellet configuration file " + configFile );
        
        Properties properties = new Properties();
        Boolean value;
        try {
            properties.load( configFile.openStream() );
            
            value = getBooleanProperty( properties, "USE_UNIQUE_NAME_ASSUMPTION" );
            if( value != null ) 
                USE_UNIQUE_NAME_ASSUMPTION = value.booleanValue();
            
            value = getBooleanProperty( properties, "USE_PSEUDO_NOMINALS" );
            if( value != null )
                USE_PSEUDO_NOMINALS = value.booleanValue();
            
            value = getBooleanProperty( properties, "SHOW_CLASSIFICATION_PROGRESS" );
            if( value != null )
                SHOW_CLASSIFICATION_PROGRESS = value.booleanValue();      
            
            value = getBooleanProperty( properties, "USE_NEW_QUERY_PARSER" );
            if( value != null )
                USE_NEW_QUERY_PARSER = value.booleanValue();        
            
            value = getBooleanProperty( properties, "REORDER_QUERY" );
            if( value != null )
                REORDER_QUERY = value.booleanValue();        
        }
        catch( FileNotFoundException e ) {
            log.error( "Pellet configuration file cannot be found" );
        }
        catch( IOException e ) {
            log.error( "I/O error while reading Pellet configuration file" );
        }        
    }    

    private static Boolean getBooleanProperty( Properties properties, String property ) {
        String value = properties.getProperty( property );
        
        if( value != null ) {
            if( value.equalsIgnoreCase( "true" ) )
                return Boolean.TRUE;
            else if( value.equalsIgnoreCase( "false" ) )
                return Boolean.FALSE;
            else
                log.error( "Ignoring invalid value for property " + property );
        }
        
        return null;
    }
    
    /**
     * When this option is set completion will go on even if a clash is detected until the
     * completion graph is saturated. Turning this option has very severe performance effect
     * and right now is only used for experimental purposes to generate explanations. 
     * 
     * <p>
     * <b>*********** DO NOT CHANGE THE VALUE OF THIS OPTION **************</b>
     */
    public static boolean SATURATE_TABLEAU = false;
    
	/**
	 * This option tells Pellet to treat every individual with a distinct URI to be
	 * different from each other. This is against the semantics of OWL but is much 
	 * more efficient than adding an <code><owl:AllDifferent></code> definition with
	 * all the individuals. This option does not affect b-nodes, they can still be
	 * inferred to be same.
	 */
	public static boolean USE_UNIQUE_NAME_ASSUMPTION = false;
	
	/**
     * @deprecated According to SPARQL semantics all variables are distinguished
     * by definition and bnodes in the query are non-distinguished variables so 
     * this option is not used anymore
	 */
	public static boolean TREAT_ALL_VARS_DISTINGUISHED = false;
	
    /**
     * Sort the disjuncts based on the statistics 
     */
	public static boolean USE_DISJUNCT_SORTING = true && !SATURATE_TABLEAU;
	
	public static boolean SHOW_CLASSIFICATION_PROGRESS = false;
	
	public static final String NO_SORTING = "NO";
	public static final String OLDEST_FIRST = "OLDEST_FIRST";	
	public static String USE_DISJUNCTION_SORTING = OLDEST_FIRST;
	
    /**
	 * When this option is enabled all entities (classes, properties, individuals) are 
	 * identified using local names rather than full URI's. This makes the debugging
	 * messages shorter and easier to inspect by eye. This options should be used with
	 * care because it is relatively easy to have local names with different namespaces 
	 * clash.  
	 * 
	 * <b>*** This option should only be used for debugging purposes. ***</b>
	 */
	public static boolean USE_LOCAL_NAME = false;
	public static boolean USE_QNAME = false;
	
	/**
	 * TBox absorption will be used to move some of the General Inclusion Axioms (GCI)
	 * from Tg to Tu.
	 */
	public static boolean USE_ABSORPTION = true;

	/**
	 * Absorb TBox axioms into domain/range restrictions in RBox
	 */
	public static boolean USE_ROLE_ABSORPTION = true;
	
	/**
	 * Absorb TBox axioms about nominals into ABox assertions 
	 */
	public static boolean USE_NOMINAL_ABSORPTION = true;
    
    public static boolean USE_HASVALUE_ABSORPTION = true;

	
	//Optimization specific to Econnections
	public static boolean USE_OPTIMIZEDINDIVIDUALS = false;

	
	/**
	 * Use dependency directed backjumping
	 */
	public static boolean USE_BACKJUMPING = !SATURATE_TABLEAU & true;
	
	/**
	 * Check the cardinality restrictions on datatype properties and
     * handle inverse functional datatype properties
	 */
	public static boolean USE_FULL_DATATYPE_REASONING = true;
	
	/**
	 * Cache the pseudo models for named classes and individuals.
	 */
	public static boolean USE_CACHING = true;
	
	public static boolean USE_ADVANCED_CACHING = true;
	
	/**
	 * To decide if individual <code>i</code> has type class <code>c</code> check 
	 * if the edges from cached model of <code>c</code> to nominal nodes also exists
	 * for the cached model of <code>i</code>.
	 */
	public static boolean CHECK_NOMINAL_EDGES = true;
		
	/**
	 * During backjumping use dependency set information to restore node labels rather
	 * than restoring the label exactly to the previous state.
	 */
	public static boolean USE_SMART_RESTORE = true;
	
	/**
	 * When a consistency check starts in ABox use the cached pseudo model as the starting point
	 * rather than the original ABox. Since all the branching information is already stored in
	 * the pseudo model, this should be logically equivalent but much faster
	 */
    public static boolean USE_PSEUDO_MODEL = true;
    
    /**
     * Treat nominals (classes defined by enumeration) as named atomic concepts rather than
     * individual names. Turning this option improves the performance but soundness and completeness
     * cannot be established. 
     */
    public static boolean USE_PSEUDO_NOMINALS = false;
    
	/**
	 * Dynamically find the best completion strategy for the KB. If disabled
	 * SHION strategy will be used for all the ontologies.
	 */
	public static boolean USE_COMPLETION_STRATEGY = !SATURATE_TABLEAU & true;
	
	/**
	 * Use semantic branching, i.e. add the negation of a disjunct when the next branch is being
	 * tried
	 */
	public static boolean USE_SEMANTIC_BRANCHING = !SATURATE_TABLEAU & true;
		
	/**
	 * The default strategy used for ABox completion. If this values is set, this strategy will
	 * be used for all the KB's regardless of the expressivity.
	 * 
	 * <p>
	 * <b>*********** DO NOT CHANGE THE VALUE OF THIS OPTION **************</b>
	 */
	public static Class DEFAULT_COMPLETION_STRATEGY = null;
    	
	/**
	 * Print the size of the TBox and ABox after parsing. 
	 */
	public static boolean PRINT_SIZE = false;

	/**
	 * Prefix to be added to bnode identifiers
	 */
	public static final String BNODE = "bNode";
	
	/**
	 * Prefix to be added to anonymous individuals tableaux algorithm creates
	 */
	public static final String ANON = "anon";
	
	/**
	 * When doing a satisfiability check for a concept, do not copy the individuals
	 * even if there are nominals in the KB until you hit a nominal rule application.  
	 *
	 */
	public static boolean COPY_ON_WRITE = true;
	
	/**
	 * Control the behavior if a function such as kb.getInstances(), kb.getTypes(), 
	 * kb.getPropertyValues() is called with a parameter that is an undefined class, property or
	 * individual. If this option is set to true then an exception is thrown each time this occurs,
	 * if not set the corresponding function returns a false value (or an empty set where 
	 * appropriate).  
	 */
    public static boolean SILENT_UNDEFINED_ENTITY_HANDLING = true;

    /**
     * Validate ABox structure during completion (Should be used only for debugging purposes). 
     */
    public static boolean VALIDATE_ABOX = false;	

    /**
     * Print completion graph after each iteration (Should be used only for debugging purposes). 
     */
    public static boolean PRINT_ABOX = false;	
    
    public static final boolean DEPTH_FIRST = true;
    public static final boolean BREADTH_FIRST = false;
    public static boolean SEARCH_TYPE = DEPTH_FIRST;
    
    /**
     * Use optimized blocking even for SHOIN. It is not clear that using this blocking method
     * would be sound or complete. It is here just for experimental purposes. 
     * 
     * <p>
     * <b>*********** DO NOT CHANGE THE VALUE OF THIS OPTION **************</b>
     */
    public static boolean FORCE_OPTIMIZED_BLOCKING = false;

    public static boolean SPLIT_QUERY = true;
    
    public static boolean SIMPLIFY_QUERY = true;
    
    public static boolean REORDER_QUERY = false;
    
    public static boolean CACHE_RETRIEVAL = false;

    /**
     * The new query parser recognizes class expressions encoded in SPARQL queries
     * and allows the Pellet query engine to handle more ABox queries.
     */
    public static boolean USE_NEW_QUERY_PARSER = true;
    
    public static String DEFAULT_CONFIGURATION_FILE = "pellet.properties";
    
    static {
        String configFile = System.getProperty( "pellet.configuration" );

        URL url = null;

        // if the user has not specified the pellet.configuration
        // property, we search for the file "pellet.properties"
        if( configFile == null ) {
            url = PelletOptions.class.getClassLoader().getResource( DEFAULT_CONFIGURATION_FILE );
        }
        else {
            try {
                url = new URL( configFile );
            }
            catch( MalformedURLException ex ) {
                ex.printStackTrace();
                
                // so, resource is not a URL:
                // attempt to get the resource from the class path
                url = PelletOptions.class.getClassLoader().getResource( configFile );
            }
            
            if( url == null )
                log.error( "Cannot file Pellet configuration file " + configFile );
        }

        if( url != null  ) 
            load( url );
    }
    
}
