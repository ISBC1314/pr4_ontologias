/*
 * Created on Oct 12, 2004
 */

package org.mindswap.pellet.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.SetUtils;

import aterm.ATermAppl;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

public class PelletTestCase extends TestCase {
    static ATermAppl TOP = ATermUtils.TOP;
    static ATermAppl BOTTOM = ATermUtils.BOTTOM;

    public static ATermAppl term(String s) {
        return ATermUtils.makeTermAppl(s);
    }    

    public static ATermAppl not(ATermAppl c) {
		return ATermUtils.makeNot(c);
	}

	public static ATermAppl all(ATermAppl r, ATermAppl c) {
		return ATermUtils.makeAllValues(r, c);
	}

	public static ATermAppl some(ATermAppl r, ATermAppl c) {
		return ATermUtils.makeSomeValues(r, c);
	}

	public static ATermAppl min(ATermAppl r, int n) {
		return ATermUtils.makeMin(r, n);
	}

	public static ATermAppl max(ATermAppl r, int n) {
		return ATermUtils.makeMax(r, n);
	}	
	
	public static ATermAppl inv(ATermAppl r) {
		return ATermUtils.makeInv(r);
	}

	public static ATermAppl literal(String value) {
		return ATermUtils.makePlainLiteral(value);
	}
	
	public static ATermAppl plainLiteral(String value, String lang) {
		return ATermUtils.makePlainLiteral(value, lang);
	}

	public static ATermAppl typedLiteral(String value, String dt) {
		return ATermUtils.makeTypedLiteral(value, dt);
	}

	public static ATermAppl value(ATermAppl r) {
		return ATermUtils.makeValue(r);
	}
	
	public static ATermAppl and(ATermAppl c1, ATermAppl c2) {
		return ATermUtils.makeAnd(c1, c2);
	}
	
	public static ATermAppl and(ATermAppl[] c) {
		return ATermUtils.makeAnd(ATermUtils.makeList(c));
	}
	
	public static ATermAppl or(ATermAppl c1, ATermAppl c2) {
		return ATermUtils.makeOr(c1, c2);
	}
	
	public static ATermAppl or(ATermAppl[] c) {
		return ATermUtils.makeOr(ATermUtils.makeList(c));
	}    
	
    protected static boolean isAnonValue( Object n ) {
        return ((n instanceof Resource) && ((Resource) n).isAnon()) ||
               ((n instanceof Statement) && ((Statement) n).getSubject().isAnon()) ||
               ((n instanceof Statement) && isAnonValue( ((Statement) n).getObject() ));
    }

    public static void assertCollectionValues(TestCase testCase, Collection coll, Object[] vals) {
        boolean[] found = new boolean[vals.length];
        
        for (int i = 0; i < vals.length; i++) found[i] = false;
        
        Collection flattened = SetUtils.union( coll );
        Iterator it = flattened.iterator();
        while (it.hasNext()) {
            Object n = it.next();
            boolean gotit = false;
            
            for (int i = 0; i < vals.length; i++) {
                if (n.equals(vals[i])) {
                    gotit = true;
                    found[i] = true;
                }
            }
            TestCase.assertTrue( testCase.getName() + " found unexpected value: " + n, gotit);
        }
        
        // check that no expected values were unfound
        for (int i = 0; i < vals.length; i++) {
            TestCase.assertTrue(testCase.getName() + " failed to find expected value: " + vals[i], found[i]);
        }
    }
    
    public static void assertIteratorContains(TestCase testCase, Iterator it, Object val) {
        boolean found = false;
        while( it.hasNext() && !found ) {
            Object obj = it.next();
            found = obj.equals( val );
        }
        
        TestCase.assertTrue(testCase.getName() + " failed to find expected iterator value: " + val, found );
    }

    public static void assertIteratorValues(TestCase testCase, Iterator it, Object[] vals) {
        boolean[] found = new boolean[vals.length];
        
        for (int i = 0; i < vals.length; i++) found[i] = false;        
        
        while (it.hasNext()) {
            Object n = it.next();
            boolean gotit = false;
            
            for (int i = 0; i < vals.length; i++) {
                if (n.equals(vals[i])) {
                    gotit = true;
                    found[i] = true;
                }
            }
            TestCase.assertTrue( testCase.getName() + " found unexpected iterator value: " + n, gotit);
        }
        
        // check that no expected values were unfound
        for (int i = 0; i < vals.length; i++) {
            TestCase.assertTrue(testCase.getName() + " failed to find expected iterator value: " + vals[i], found[i]);
        }
    }
 
	
	public static void testResultSet( ResultSet results, List ans ) {
	    List answers = new ArrayList( ans );
        while( results.hasNext() ) {
            QuerySolution sol = results.nextSolution();
            assertNotNull( "QuerySolution", sol );

            Map answer = new HashMap();
            for ( Iterator iter = results.getResultVars().iterator() ; iter.hasNext() ; ) {
                String var = (String)iter.next() ; 
                Object val = sol.get( var ) ;
                assertNotNull( "Variable: " + var, val ) ;
                
                answer.put( var, val );
            }
                      
            assertTrue( "Unexpected binding found: " + answer, answers.remove( answer ) );
        }
        
        assertTrue( "Binding not found: " + answers, answers.isEmpty() );        
    }

	public static Map createBinding( Object[] keys, Object[] values ) {
        assertTrue( keys.length == values.length );
        
        Map answer = new HashMap();
        for( int i = 0; i < keys.length; i++ )
            answer.put( keys[i], values[i] );
        
        return answer;
    }
    
	public static List createBindings( Object[] keys, Object[][] values ) {
        List answers = new ArrayList();
        for( int i = 0; i < values.length; i++ ) {        
	        Map answer = new HashMap();
	        for( int j = 0; j < keys.length; j++ )
	            answer.put( keys[j], values[i][j] );
	        answers.add( answer );
    	}        
        
        return answers;
    }
    
	public static void printAll(Iterator i) {
	    while(i.hasNext()) {
	        System.out.println(i.next());
	    }	    
	}
	
	public static void printAll(Iterator i, String head) {
	    System.out.print( head + ": ");
	    if( i.hasNext()) {
	        System.out.println();
		    while(i.hasNext()) {
		        System.out.println(i.next());
		    }	    
	    }
	    else
	        System.out.println( "<EMPTY>" );
	}
	
	
	public static boolean[] testSubClass(KnowledgeBase kb, String c1, String c2) {
	    return testSubClass(kb, term(c1), term(c2));
	}
	
	public static boolean[] testSubClass(KnowledgeBase kb, ATermAppl c1, ATermAppl c2) {
	    boolean result[] = new boolean [2];
	    
	    result[0] = kb.isSubClassOf(c1, c2);
		kb.isSatisfiable(c1);		 
		kb.isSatisfiable(not(c1));	
		kb.isSatisfiable(c2);
		kb.isSatisfiable(not(c2));
		
		long satCount = kb.getABox().satisfiabilityCount;
		result[1] = kb.isSubClassOf(c1, c2);
		boolean cached = (satCount == kb.getABox().satisfiabilityCount);
		
		System.out.println("Subsumption : " + result[0]);
		System.out.println("Cached " + (cached?"(yes)":"(no) ")+ ": " + result[1]);
		
		return result;
	}	

	public static boolean[] testType(KnowledgeBase kb, String c1, String c2) {
	    return testType(kb, term(c1), term(c2));
	}
	
	public static boolean[] testType(KnowledgeBase kb, ATermAppl ind, ATermAppl c) {
	    boolean result[] = new boolean [2];
	    
	    result[0] = kb.isType( ind, c );
		kb.isSatisfiable(c);
		kb.isSatisfiable(not(c));
		
		long satCount = kb.getABox().satisfiabilityCount;
		result[1] = kb.isType( ind, c );
		boolean cached = (satCount == kb.getABox().satisfiabilityCount);
		
		System.out.println("Type      : " + result[0]);
		System.out.println("Cached " + (cached?"(yes)":"(no) ")+ ": " + result[1]);
		
		return result;
	}	
}
