/*
 * Created on Oct 29, 2004
 */
package org.mindswap.pellet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.mindswap.pellet.jena.ModelReader;
import org.mindswap.pellet.jena.OWLReasoner;
import org.mindswap.pellet.output.ATermAbstractSyntaxRenderer;
import org.mindswap.pellet.output.OutputFormatter;
import org.mindswap.pellet.query.Query;
import org.mindswap.pellet.query.QueryEngine;
import org.mindswap.pellet.utils.ATermUtils;

import aterm.ATermAppl;

/**
 * @author Daniel
 */
public class SWRLTest {
	public Set firstVars = new HashSet(); 
	public Vector firstTriples = new Vector();
	public Set secondVars = new HashSet();
	public Vector secondTriples = new Vector();
	public KnowledgeBase kb;
	public ATermAppl firstClass;
	public ATermAppl secondClass;
	public HashMap usings = new HashMap();
	
	public class IneligibleRuleException extends Exception {
		
		public String getMessage() {
			return "Could not convert rule to OWL.";
		}

	}
	
	public class Rule {
		KnowledgeBase kb;
		HashMap usings;
		
		public String head;
		public String tail;
		
		public Set firstVars = new HashSet(); 
		public Set secondVars = new HashSet();
		public Set sharedVars = new HashSet();
		
		public Vector firstTriples = new Vector();
		public Vector secondTriples = new Vector();
		
		public String firstQuery = new String();
		public String secondQuery = new String();
		
		public ATermAppl firstClass;
		public ATermAppl secondClass;
		
		public Rule( String line, HashMap usings, KnowledgeBase kb ) throws IneligibleRuleException {
			this.kb = kb;
			this.usings = usings;
			int index = line.indexOf( ":-" );
				
			head = line.substring( 0, index ).trim();
			tail = line.substring( index + 2, line.length() - 1 ).trim();

			System.out.println( "Rule: " + line );

			System.out.println( "Head: " + head );
			parseIntoQuery( head, true );
			System.out.println( "Tail: " + tail );
			parseIntoQuery( tail, false );
			
			sharedVars.addAll( firstVars );
			sharedVars.retainAll( secondVars );
			
			if ( sharedVars.isEmpty() ) {
				throw new IneligibleRuleException();
			}
			
			String var = sharedVars.iterator().next().toString();
			
			firstClass = generateRollup( var, true );
			secondClass = generateRollup( var, false );
		}
		
		public ATermAppl getHeadClass() {
			return firstClass;
		}
		
		public ATermAppl getTailClass() {
			return secondClass;
		}
		
		public void parseIntoQuery( String expression, boolean first ) {
			String[] items = expression.split( "," );
			Vector test = new Vector();
		
			for ( int i = 0; i < items.length; i++ ) {
				items[i] = items[i].trim();
				if ( items[i].indexOf( "(" ) != -1 && items[i].indexOf( ")" ) == -1 ) {
					test.add( items[i]+ "," + items[i+1].trim() );
					i += 1;
				} else {
					test.add( items[i] );
				}
			}
		
			for ( Iterator i = test.iterator(); i.hasNext(); ) {
				parsePredicate( (String) i.next(), first );
			}
		}
		
		public boolean isVariable( String arg ) {
			if ( arg.indexOf( ":" ) == -1 ) {
				return true;
			} else { 
				return false;
			}
		}
		
		public ATermAppl generateRollup( String var, boolean first ) {
			String query = null;
			Vector triples = null;
			
			if ( first ) {
				query = firstQuery;
				triples = firstTriples;
			} else {
				query = secondQuery;
				triples = secondTriples;
			}
			
			query = query.concat( "SELECT " + var + "\n\nWHERE\n" );
			for ( Iterator i = triples.iterator(); i.hasNext(); ) {
				query = query.concat( (String) i.next() );
				if ( i.hasNext() ) 
					query = query.concat( ",\n" );
			}
		
			query = query.concat( "\n\nUSING " );
			for ( Iterator i = usings.keySet().iterator(); i.hasNext(); ) {
				Object key = i.next();
				query = query.concat( key.toString() + " FOR <" + usings.get( key.toString() ) + ">" );
				if ( i.hasNext() ) {
					query = query.concat( "," );
				}
				query = query.concat( "\n" );
			}
			
			System.out.println( query );
					
			Query q = QueryEngine.parse( query, kb );
			return q.rollUpTo( (ATermAppl) q.getDistObjVars().iterator().next() );
		}
		
		public void parsePredicate( String s, boolean first ) {
				int open = s.indexOf( "(" );
				int closed = s.indexOf( ")" );
				String pred = s.trim().substring( 0, open );
				String args = s.substring( open + 1, closed ).trim();

				Set vars = null;
				Vector triples = null;
				if ( first ) {
					vars = firstVars;
					triples = firstTriples;					
				} else {
					vars = secondVars;
					triples = secondTriples;
				}

				int comma = args.indexOf( "," );
				if ( comma != -1 ) {
					String subject = args.substring( 0, comma ).trim(); 
					String object = args.substring( comma + 1, args.length() ).trim();
					
					if ( isVariable( subject ) ) {
						subject = "?".concat( subject );
						vars.add( subject );
					}
					
					if ( isVariable( object ) ) {
						object = "?".concat( object );
						vars.add( object );
					}
			
					triples.add( "(" + subject + ", " + pred + ", " + object + ")" );
				} else {
					if ( isVariable( args ) ) {
						args = "?".concat( args );
						vars.add( args );
					}
					
					triples.add( "(" + args + ", " + "<rdf:type>" + ", " + pred + ")" );
				}
			}
	}
	
	public void parseRule( String line ) {
		try {
			Rule testRule = new Rule( line, usings, kb );
	
			//KnowledgeBase.DEBUG = true;
			//kb.addSubClass( testRule.getTailClass(), testRule.getHeadClass() );
			//kb.classify();

			StringWriter test = new StringWriter();
			
			System.out.println( "Head Class (ATerm): " + testRule.getHeadClass() );
			System.out.println( "Tail Class (ATerm): " + testRule.getTailClass() );
			
			ATermAppl subclass = ATermUtils.makeSub( testRule.getTailClass(), testRule.getHeadClass() );
			
			ATermAbstractSyntaxRenderer renderer = new ATermAbstractSyntaxRenderer();
			renderer.setWriter(new OutputFormatter(test, false));
			renderer.visit( subclass );
			System.out.println( "Abstract Syntax SubClass: " + test.toString() + "\n\n");
			
			//org.semanticweb.owl.io.abstract_syntax.
			
			//rend.setWriter( new FileWriter( "c:/mindlab/abstest.txt" ) );
		} catch( Exception e ) {
			e.printStackTrace();
		}
	}
	
	public void parseUsing( String line ) {
		line = line.substring( line.indexOf( "(" ) + 1, line.indexOf( ")" ) );
		String shortForm = line.substring( 0, line.indexOf( "," ) ).trim();
		String longForm = line.substring( line.indexOf( "\"" ) + 1, line.length() - 1 ).trim();
		//System.out.println( shortForm + longForm ); 
		
		usings.put( shortForm, longForm );	
	}
	
	public void loadKB( String uri ) {
		OWLReasoner reasoner = new OWLReasoner();
		kb = new KnowledgeBase();

		reasoner.load(new ModelReader().read( uri ));
		if (!reasoner.isConsistent()) {
			//throw new Exception("Dataset isn't consistent!");
		}
		kb = reasoner.getKB();
	}

	public static void main(String[] args) {
		try {
			SWRLTest test = new SWRLTest();
			HashMap usings = new HashMap();
			
			
			test.loadKB( "file:" + args[1] );
// 			test.loadKB( "file:" + "c:/MINDlab/citizen.owl" );
			
			BufferedReader r = new BufferedReader( new FileReader( args[0] ) );
//			BufferedReader r = new BufferedReader( new FileReader( "C:/MINDlab/queries/test.swrl" ) );
			
			String line = r.readLine();
			line = line.trim();
			
			while ( line.indexOf( "using" ) != -1 ) {
				test.parseUsing( line );	
				
				line = r.readLine();	
				line = line.trim();
			}
			
			while ( line.trim().equals( "" ) ) {
				line = r.readLine();	
				line = line.trim();
			}
			
			//while ( line.indexOf( "(" ) != -1 ) {
			while ( line != null ) {
				line = line.trim();
				if ( line.equals( "" ) == false ) {
					test.parseRule( line );		
				}
	
				line = r.readLine();	
			}
	
		} catch (Exception e){
			e.printStackTrace();
		}
	}
}
