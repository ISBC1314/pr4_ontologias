/*
 * Created on Sep 20, 2005
 */
package org.mindswap.pellet.utils;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
//import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
//import java.awt.Toolkit;

import org.mindswap.pellet.ABox;
import org.mindswap.pellet.Edge;
import org.mindswap.pellet.EdgeList;
import org.mindswap.pellet.Individual;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.Literal;
import org.mindswap.pellet.Node;
import org.mindswap.pellet.jena.OWLReasoner;

import aterm.ATermAppl;

/**
 * @author Evren Sirin
 *
 */
public class GenerateData {
    String ns = "http://www.example.org/test#";
    
    String rdfType  = Namespaces.RDF + "type";
    String owlThing = Namespaces.OWL + "Thing";
    
    PrintWriter out;
    
    KnowledgeBase kb;
    int count;
    int countLiteral;
    int nrOfClassTriples;
    int nrOfRelations;
    int longestChain ;
    
    boolean onlyNamedClassesInOutput = true;
    boolean generateLiterals = true;
    int typeFactor;// = 10 ; //default to all
    int roleFactor;
    
    java.util.Random generator = new java.util.Random();
    Map uris;
    Map classparams = new HashMap();
    
    public static final int RDFXML  = 0;
    public static final int NTRIPLE = 1;
    
    int format = RDFXML;
    
    QNameProvider qnames = new QNameProvider();
    
    
    public GenerateData( KnowledgeBase kb, PrintWriter out ) {
        this.kb = kb;        
        this.out = out;
        
        kb.isConsistent();
        
        for( Iterator i = kb.getProperties().iterator(); i.hasNext(); ) {
            ATermAppl c = (ATermAppl) i.next();
            qnames.shortForm( c.getName() );
        }
    }
    
    void clearURIs() {
        uris = new HashMap();
    }
    
    void printHeader() {
        switch( format ) {
	    	case NTRIPLE:
	    	    break;
		    case RDFXML:    	        
	    	    out.println( "<rdf:RDF" );
	    	    for(Iterator i = qnames.getPrefixSet().iterator(); i.hasNext();) {
	                String prefix = (String) i.next();
	                String uri = qnames.getURI( prefix );
	        	    out.println( "  xmlns:" + prefix + "=\"" + uri + "\"" );                
	            }
	    	    out.println( ">" );
	    	    break;
		    default:
		        throw new RuntimeException( "Unknown format: "  + format );
	    }        
    }
    
    void printFooter() {
        switch( format ) {
	    	case NTRIPLE:
	    	    break;
		    case RDFXML:    	        
	    	    out.println( "</rdf:RDF>" );
	    	    break;
		    default:
		        throw new RuntimeException( "Unknown format: "  + format );
	    }        
    }
    
    void printTriple( String subj, String pred, String obj ) {
        switch( format ) {
        	case NTRIPLE:
        	    out.println( "<" + subj + "> <" + pred + "> <" + obj + "> ." );
        	    break;
    	    case RDFXML:    	        
    	        String qname = qnames.shortForm( pred ) ;
        	    out.println( "<rdf:Description rdf:about=\"" + subj + "\">" );
        	    out.println( "  <" + qname+ " rdf:resource=\"" + obj + "\"/>" );
        	    out.println( "</rdf:Description>" );
        	    break;
    	    default:
    	        throw new RuntimeException( "Unknown format: "  + format );
        }
    }

    public void printTriple( String subj, String pred, String obj, String datatypeURI ) {
        switch( format ) {
	    	case NTRIPLE:
        		out.print( "<" + subj + "> <" + pred + "> \"" + obj + "\"" );
                if( datatypeURI != null )
                    out.print( "^^<" + datatypeURI + ">" );
                out.println( " ." );	
	    	    break;
		    case RDFXML:
    	        String qname = qnames.shortForm( pred ) ;
        	    out.println( "<rdf:Description rdf:about=\"" + subj + "\">" );
        	    out.print( "  <" + qname );
        	    if( datatypeURI != null )
        	        out.print( " rdf:datatype=\"" + datatypeURI + "\"" );
        	    out.println( ">" + obj + "</" + qname+ ">" );
        	    out.println( "</rdf:Description>" );
	    	    break;
		    default:
		        throw new RuntimeException( "Unknown format: "  + format );
	    }        
    }

    String getURI( Node node ) {
        String uri = (String) uris.get( node );
        if( uri == null ) {
            uri = ns + (++count);
            uris.put( node, uri );
        }
        
        return uri;
    }
    

    public void generate(ATermAppl c) {
		ABox abox = kb.getABox();
		abox.setKeepLastCompletion( true );

		boolean sat = abox.isSatisfiable(c);

		if (!sat) {
			// System.err.println( "Cannot generate instances for unsatisfiable
			// " + c );
			return;
		}

		abox = abox.getLastCompletion();
		if (longestChain < abox.treeDepth)
			longestChain = abox.treeDepth;

		clearURIs();

		Iterator i = abox.getIndIterator();
		while (i.hasNext()) {
			Individual ind = (Individual) i.next();
			String subj = getURI(ind);
			String pred = rdfType;
			String obj;
			int filter;
			List types = ind.getTypes(Node.ATOM);

			if (typeFactor == 11)
				filter = generator.nextInt(10);
			else
				filter = typeFactor;
			
			List classes = new ArrayList(kb.getClasses()); // Duped from
															// generate(Aterm...)
			for (int j = 0; j < types.size(); j++) {

				if ((generator.nextInt(9)+1) <= filter) {
					ATermAppl type = (ATermAppl) types.get(j);
					
					if (ATermUtils.isNot(type))
						continue;
					
					if (type.equals(ATermUtils.TOP))
						obj = owlThing;
					else
						obj = type.getName();
					
					if ((obj != owlThing)
							&& ((!onlyNamedClassesInOutput) // are these necessary?
									// I vaguely recall
									// evren saying not
									|| (onlyNamedClassesInOutput && classes.contains(type)))) {
						printTriple(subj, pred, obj);
						nrOfClassTriples++;
					}
				}
			}

			if (roleFactor == 11)
				filter = generator.nextInt(10);
			else
				filter = roleFactor;
			
			EdgeList edges = ind.getOutEdges();
			for (int j = 0; j < edges.size(); j++) {
				if ((generator.nextInt(9)+1) <= filter) {
					Edge edge = edges.edgeAt(j);

					pred = edge.getRole().getName().getName();

					Node node = edge.getTo();
					if (node.isLiteral()) {
						if (generateLiterals) {
							obj = "Literal" + (++countLiteral);
							String datatypeURI = ((Literal) node)
									.getDatatypeURI();
							printTriple(subj, pred, obj, datatypeURI);
							nrOfRelations++;
						} else
							continue;
					} else {
						printTriple(subj, pred, getURI(node));
						nrOfRelations++;
					}
				}
			}
		}
	}
    
    public void generate( int size ) {
        List classes = new ArrayList( kb.getClasses() );
        
        // java.util.Random generator = new java.util.Random();
        // String cname ="";

        printHeader();
        
        while( (nrOfClassTriples + nrOfRelations) < size ) {
        		// System.err.println((nrOfClassTriples + nrOfRelations));
        	    Collections.shuffle( classes );
	        kb.getABox().clearCaches( true );
	        kb.getABox().isConsistent();
        
	        // for( Iterator i = classes.iterator(); i.hasNext(); ) {
	        // int n = generator.nextInt( size / classes.size() );
	        // System.err.print("N == ");
	        // System.err.println(n);
	        // for( int j=1; j < n; j++ ) {
	        		// Iterator i = classes.iterator();
	        		ATermAppl c = (ATermAppl) classes.get(0);// i.next();
	        		// cname = c.getName();
	        		generate( c );
	        //}
    		//System.err.println(cname);
	        //}
        }

        printFooter();
        
        out.flush();
    }
    
    public void generateForEachClass(int maxIndPerClass) {
		List classes = new ArrayList(kb.getClasses());
		int oldClassTrips;
		int oldRoleTrips;
		int max = maxIndPerClass;
		int oldCount ;
		printHeader();
		// System.err.println(maxIndPerClass + " ind per class");
		kb.getABox().clearCaches(true);
		kb.getABox().isConsistent();
		System.err.println("Class\tIndividuals\tNrClassTriples\tNrRoleTriples\tTreeDepth");
		
		for (Iterator i = classes.iterator(); i.hasNext();) {
			oldClassTrips = nrOfClassTriples;
			oldRoleTrips = nrOfRelations;
			oldCount = count;
			ATermAppl c = (ATermAppl) i.next();
			if (classparams.containsKey(c.getName())) {
				max = Integer.parseInt((String) classparams.get(c.getName()));
				System.err.println(max);
			} else {
				if (max > 1)
					max = generator.nextInt(maxIndPerClass);
				if (max < 1)
					max = 1;
			}

			for (int j = 0; j < max; j++) {
				generate(c);
			}
		

			oldClassTrips = nrOfClassTriples - oldClassTrips;
			oldRoleTrips = nrOfRelations -  oldRoleTrips;
			oldCount = count - oldCount;
			/*System.err.println(c.getName());
			System.err.println("NrOfClassTrips: " + oldClassTrips);
			System.err.println("NrOfRelations : " + oldRoleTrips);
			System.err.println("Longest chain: "+kb.getABox().getLastCompletion().treeDepth);
			System.err.println("---------");*/
			System.err.println(c.getName()+"\t"+oldCount+"\t"+oldClassTrips+"\t"+oldRoleTrips+"\t"+kb.getABox().getLastCompletion().treeDepth);
		}

		printFooter();
		out.flush();
	}	

// I'm not really sure how to generate a query by walking the completion graph, if
// that even makes sense. Variable corefing seems hard...always end up with tree structures....
// but that'll be how the data is anyway!
/*    void generateQuery(ATermAppl c) {
    		ABox abox = kb.getABox();
    		boolean sat = abox.isSatisfiable(c);
    		if (!sat)
    			return;
    		abox = abox.getLastCompletion();
    		clearURIs();

    		Iterator i = abox.getIndIterator();
    		while (i.hasNext()) {
    			Individual ind = (Individual) i.next();
    			String subj = getURI(ind);
    			String pred = rdfType;
    			String obj;
    			List types = ind.getTypes(Node.ATOM);

    			for (int j = 0; j < types.size(); j++) {
    				ATermAppl type = (ATermAppl) types.get(j);
					
    				if (ATermUtils.isNot(type))
					continue;
				
				if (type.equals(ATermUtils.TOP))
					obj = owlThing;
				else
					obj = type.getName();
				
				if (obj != owlThing) {
					printTriple(subj, pred, obj);
					nrOfClassTriples++;
				}
			}

		
    			EdgeList edges = ind.getOutEdges();
    			for (int j = 0; j < edges.size(); j++) {
				Edge edge = edges.edgeAt(j);

				pred = edge.getRole().getName().getName();

				Node node = edge.getTo();
				if (node.isLiteral()) {
					if (generateLiterals) {
						obj = "Literal" + (++countLiteral);
						String datatypeURI = ((Literal) node)
								.getDatatypeURI();
						printTriple(subj, pred, obj, datatypeURI);
					} else
						continue;
				} else {
					printTriple(subj, pred, getURI(node));
				}
			}	
	}
}
    
    public void generateAQueryForEachClass() {
		List classes = new ArrayList(kb.getClasses());
		// System.err.println(maxIndPerClass + " ind per class");
		kb.getABox().clearCaches(true);
		kb.getABox().isConsistent();
		for (Iterator i = classes.iterator(); i.hasNext();) {
			ATermAppl c = (ATermAppl) i.next();
			
			generateQuery(c);

			//System.err.println(c.getName());
			//System.err.println("NrOfClassTrips: " + nrOfClassTriples);
			//System.err.println("NrOfRelations : " + nrOfRelations);
		}

		printFooter();
		out.flush();
	}	*/

void readClassOptions(String filename) {
		// classparams = new HashMap();
		try {
			FileReader fr = new FileReader(filename);
			BufferedReader br = new BufferedReader(fr);
			String option = new String();
			String[] parsedOption;
			while ((option = br.readLine()) != null) {
				parsedOption = option.split(" ");
				classparams.put(parsedOption[0], parsedOption[1]);
			}
			// System.err.println(classparams.get("http://www.example.org/selectivity#BranchyQuery"));
		} catch (IOException e) {
			System.out.println("Can't find options file " + filename
					+ ". Ignoring.");
		}
	}
    
    public static void usage() {
		System.out.println("GenerateData");
		System.out.println("");
		System.out.println("Generate random instance data from an ontology.");
		System.out.println("");		
        	System.out.println("usage: GenerateData OPTIONS <ontologyURI>");
        	System.out.println("  -s            Size of the data generated (number of triples) (negative gives individuals per class for all classes)");
        	System.out.println("  -opt          Control file (negative size only)");
        	System.out.println("  -l            Generate literals");
        	System.out.println("  -ns           Namespace of output ontology");
        	System.out.println("  -f            Output file (stdout by default)");
        	System.out.println("  -t            Control the percentage of type triples (0(none)-10(all),11(rand)");
        	System.out.println("  -r            Control the percentage of relation triples (0(none)-10(all),11(rand)"); 
        	System.out.println("  -h            Print this screen"); 

    }
    
    void log(String msg, PrintWriter log){
    		System.err.println(msg);
    		log.println(msg);
    }
    
    public static void main(String[] args) throws Exception {
        String in = null;
        PrintWriter out = new PrintWriter( System.out );
        PrintWriter log = new PrintWriter(new FileWriter( "log.txt",true ), true );
        int size = 1000;
        int tf = 10;
        int rf = 10;
        String filename = "";
        boolean trips = true;
        boolean nc = false;
        System.err.println("Starting!");

		for (int i = 0; i < args.length; i++) {
			String arg = args[i];

			if (arg.equals("-h")) {
				usage();
				System.exit(0);
			} 
			else if (arg.equals("-f")) {
			    out = new PrintWriter( new FileWriter( args[++i] ) );
			}
			else if (arg.equals("-s")) {
			    size = Integer.parseInt( args[++i] );
				if (size < 0){
					size = -size;
					trips = false;
				}
			}
			else if (arg.equals("-t"))
			    	tf = Integer.parseInt( args[++i] );			
			else if (arg.equals("-r"))
				rf = Integer.parseInt( args[++i] );
			else if (arg.equals("-opt"))
				filename = args[++i];
			else if (arg.equals("-nc"))
			    nc = true;
			else if(i == args.length - 1) {
				in = args[i];
			}
			else {
			    System.err.println( "Unknown option; " + arg );
				usage();
				System.exit(1);			    
			}
		}
		
		if( in == null ){
		    System.err.println( "No ontology URI given" );
			usage();
			System.exit(1);			    
		}

        OWLReasoner reasoner = new OWLReasoner();
        
        reasoner.load( in );
        GenerateData gd = new GenerateData( reasoner.getKB(), out );
        gd.readClassOptions("../Ontologies/selectivity.txt");
        gd.typeFactor = tf;
        gd.roleFactor = rf;
        gd.onlyNamedClassesInOutput = nc;
        
        if (!filename.equals(""))
        		gd.readClassOptions(filename);
        
        if ( trips )
        		gd.generate( size );
        else
        		gd.generateForEachClass( size );
        gd.log( "Ontology: " + in, log );
        gd.log( "Trip Limit: " + size, log );
        gd.log( "Generated NrInd: " + gd.count, log );
        gd.log( "Class Trips: " + gd.nrOfClassTriples, log );
        gd.log( "Prop Trips: " + gd.nrOfRelations, log );
        gd.log( "Longest chain: " + gd.longestChain, log );
        gd.log( "===================", log );
        //Toolkit t = Toolkit.getDefaultToolkit();
        //t.beep();
    }    
}
