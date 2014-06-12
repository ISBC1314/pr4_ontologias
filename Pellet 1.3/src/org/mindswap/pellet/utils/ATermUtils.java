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


package org.mindswap.pellet.utils;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.mindswap.pellet.PelletOptions;

import aterm.AFun;
import aterm.ATerm;
import aterm.ATermAppl;
import aterm.ATermFactory;
import aterm.ATermInt;
import aterm.ATermList;
import aterm.pure.PureFactory;


/**
 * This class provides the functions ATerm related functions. Creating terms for URI's
 * and complex class descriptions is done here. There are also functions for normalization, 
 * simplification and conversion to NNF (Normal Negation Form).  
 * 
 * @author Evren Sirin
 */
public class ATermUtils {
	// concept constructors
	public static final String NOT  = "not";
	public static final String AND  = "and";
	public static final String OR   = "or";
	public static final String SOME = "some";
	public static final String ALL  = "all";
	public static final String MIN  = "min";
	public static final String MAX  = "max";
	public static final String CARD = "card";
	public static final String VALUE= "value";
	
	// role
	public static final String INV  = "inv";
	
	//tbox
	public static final String SUB  = "sub";
	public static final String SAME = "same";
	
	private static final ATermFactory factory = new PureFactory();
	
	public static final AFun LITFUN  = factory.makeAFun("literal", 3, false);
	public static final int LIT_VAL_INDEX  = 0;
	public static final int LIT_LANG_INDEX = 1;
	public static final int LIT_URI_INDEX  = 2;

    public static final AFun ANDFUN  = factory.makeAFun(AND, 1, false);
    public static final AFun ORFUN   = factory.makeAFun(OR, 1, false);
    public static final AFun SOMEFUN = factory.makeAFun(SOME, 2, false);
    public static final AFun ALLFUN  = factory.makeAFun(ALL, 2, false);
    public static final AFun NOTFUN  = factory.makeAFun(NOT, 1, false);
	public static final AFun MAXFUN  = factory.makeAFun(MAX, 2, false);
	public static final AFun MINFUN  = factory.makeAFun(MIN, 2, false);
	public static final AFun VALUEFUN= factory.makeAFun(VALUE, 1, false);
	
	public static Set CLASS_FUN = SetUtils.create( new AFun[] { 
	    ALLFUN, SOMEFUN, MAXFUN, MINFUN, ANDFUN, ORFUN, NOTFUN, VALUEFUN    
	} );

	/**
	 * This is not used in the reasoner but kept here to be used for display
	 */
	public static final AFun CARDFUN  = factory.makeAFun(CARD, 2, false);
    
    public static final AFun INVFUN  = factory.makeAFun(INV, 1, false);
	public static final AFun SUBFUN  = factory.makeAFun(SUB, 2, false);
	public static final AFun SAMEFUN = factory.makeAFun(SAME, 2, false);
	
	/**
	 * This is used to represent variables in queries
	 */
	public static final AFun VARFUN  = factory.makeAFun("var", 1, false);
    

	public static final AFun TYPEFUN  = factory.makeAFun("type", 2, false);
	public static final AFun IPFUN  = factory.makeAFun("iptriple", 3, false);
	public static final AFun DPFUN = factory.makeAFun("dptriple", 3, false);
	
	public static final ATermAppl EMPTY = makeTermAppl( "" );
	
	public static final ATermList EMPTY_LIST = factory.makeList();

	// TOP and BOTTOM concepts. TOP is not defined as T or not(T) any 
	// more but added to each node manually. Defining TOP as a primitive
	// concept reduces number of GCIs and makes other reasoning tasks 
	// faster
	public static final ATermAppl TOP    = ATermUtils.makeTermAppl("_TOP_");
	public static final ATermAppl BOTTOM = ATermUtils.makeNot(TOP);
	
    public static final ATermInt ONE = factory.makeInt( 1 );	
	
	static public ATermFactory getFactory() {
		return factory;	
	}			
	
	static public ATermAppl makePlainLiteral(String value) {
		return factory.makeAppl(ATermUtils.LITFUN, 
			makeTermAppl(value), 
			EMPTY, 
			EMPTY);
	}
	
	static public ATermAppl makePlainLiteral(String value, String lang) {
		return factory.makeAppl(ATermUtils.LITFUN, 
			makeTermAppl(value), 
			makeTermAppl(lang), 
			EMPTY);
	}
	
	static public ATermAppl makeTypedLiteral(String value, String dt) {
		return factory.makeAppl(ATermUtils.LITFUN, 
			makeTermAppl(value), 
			EMPTY, 
			makeTermAppl(dt));
	}
    
    static public ATermAppl NO_DATATYPE = makeTermAppl("NO_DATATYPE");
    static public ATermAppl makeLiteral(ATermAppl name) {
        return factory.makeAppl(ATermUtils.LITFUN, 
            name, 
            EMPTY, 
            NO_DATATYPE);
    }

	static public ATermAppl makeTermAppl(String name) {
		return factory.makeAppl(factory.makeAFun(name, 0 , false));	
	}

	static public ATermAppl makeTermAppl(AFun fun, ATerm[] args) {
		return factory.makeAppl(fun, args);	
	}
	
	static public ATermAppl makeNot(ATerm c) {
		return factory.makeAppl(NOTFUN, c);	
	}			

	static public ATerm term(String str) {
		return factory.parse(str);	
	}
	
	// negate all the elements in the list and return the new list	
	static public ATermList negate(ATermList list) {		
		if(list.isEmpty())
			return list;

		ATermAppl a = (ATermAppl) list.getFirst();
		a = isNot(a) ? (ATermAppl) a.getArgument(0): makeNot(a);
		ATermList result = makeList(a, negate(list.getNext()));
		
		return result;
	}

	final static public ATermAppl negate(ATermAppl a) {
		return isNot(a) ? (ATermAppl) a.getArgument(0) : makeNot(a);
	}
	
	final static public ATermAppl makeVar(String name) {
		return factory.makeAppl(VARFUN, makeTermAppl(name));	
	}	
	
	final static public ATermAppl makeVar(ATermAppl name) {
		return factory.makeAppl(VARFUN, name);	
	}	
			
	final static public ATermAppl makeValue(ATerm c) {
		return factory.makeAppl(VALUEFUN, c);	
	}			

	final static public ATermAppl makeInv(ATermAppl r) {
		return factory.makeAppl(INVFUN, r);	
	}			

	final static public ATermAppl makeSub(ATerm a, ATerm b) {
		return factory.makeAppl(SUBFUN, a, b);	
	}			

	final static public ATermAppl makeSame(ATerm a, ATerm b) {
		return factory.makeAppl(SAMEFUN, a, b);	
	}			

	final static public ATermAppl makeAnd(ATerm c1, ATerm c2) {
		return makeAnd(makeList(c2).insert(c1));	
	}		

    static public ATermAppl makeAnd(ATermList list) {
    	if(list == null || list.isEmpty())
    		return BOTTOM;
    	else if(list.getNext().isEmpty())
    		return (ATermAppl) list.getFirst();
    		
		return factory.makeAppl(ANDFUN, list);	
	}		

    final static public ATermAppl makeOr(ATermAppl c1, ATermAppl c2) {
		return makeOr(makeList(c2).insert(c1));	
	}		

	static public ATermAppl makeOr(ATermList list) {
		if(list == null || list.isEmpty())
			return BOTTOM;
		else if(list.getNext().isEmpty())
			return (ATermAppl) list.getFirst();
    		
		return factory.makeAppl(ORFUN, list);	
	}			

	final static public ATermAppl makeAllValues(ATerm r, ATerm c) {
		return factory.makeAppl(ALLFUN, r, c);	
	}			

	final static public ATermAppl makeSomeValues(ATerm r, ATerm c) {
		assertTrue( c instanceof ATermAppl );
		
		return factory.makeAppl(SOMEFUN, r, c);	
	}			

	final static public ATermAppl makeHasValue(ATerm r, ATerm ind) {
	    ATermAppl c = makeValue( ind );
		return factory.makeAppl(SOMEFUN, r, c);	
	}			

	final static public ATermAppl makeDisplayCard(ATerm r, int n) {
	    assertTrue(n >= 0);
        
	    return factory.makeAppl(CARDFUN, r, factory.makeInt(n));
	}

	final static public ATermAppl makeNormalizedMax(ATerm r, int n) {
		assertTrue(n >= 0);

		return makeNot(makeMin(r, n + 1));
	}
	
	final static public ATermAppl makeMax(ATerm r, int n) {
		assertTrue(n >= 0);

		// This was causing nnf to come out wrong
		//return makeNot(makeMin(r, n + 1));
		
		return factory.makeAppl(MAXFUN, r, factory.makeInt(n));	
	}	
			
	final static public ATermAppl makeMin(ATerm r, int n) {
		if(n == 0) return ATermUtils.TOP;
		
		assertTrue(n > 0);

		return factory.makeAppl(MINFUN, r, factory.makeInt(n));	
	}	
	
	
	final static public ATermAppl makeDisplayMax(ATerm r, int n) {
		assertTrue(n >= 0);

		return factory.makeAppl(MAXFUN, r, factory.makeInt(n));
	}	
			
	final static public ATermAppl makeDisplayMin(ATerm r, int n) {
		assertTrue(n >= 0);

		return factory.makeAppl(MINFUN, r, factory.makeInt(n));	
	}
	
	final static public ATermAppl makeCard(ATerm r, int n) {
		ATermAppl max = makeMax(r, n);
        if(n == 0) return max;
        
        ATermAppl min = makeMin(r, n);
        return makeAnd(min, max);
	}	
	
	final static public ATermList makeList(ATerm singleton) {
		return factory.makeList(singleton, EMPTY_LIST);	
	}		

	final static public ATermList makeList(ATerm first, ATermList rest) {
		return factory.makeList(first, rest);	
	}		
    
    public static ATermList makeList(Collection set) {
		ATermList list = EMPTY_LIST;
	
		for (Iterator iter = set.iterator(); iter.hasNext();) {
		    list = list.insert((ATerm)iter.next());
		}
		return list;
    }
    
	final static public ATermList makeList(ATerm[] aTerms) {		
		return makeList(aTerms, 0);	
	}		

	static private ATermList makeList(ATerm[] aTerms, int index) {		
		if(index >= aTerms.length)
			return EMPTY_LIST;
		else if(index == aTerms.length - 1)
			return makeList(aTerms[index]);
				
		return makeList(aTerms[index], makeList(aTerms, index+1));	
	}		

	final static public boolean member(ATerm a, ATermList list) {
		return (list.indexOf(a, 0) != -1);
	}
	
	static public boolean isSet(ATermList list) {
		if(list.isEmpty())
			return true;

		ATerm curr = list.getFirst();
		list = list.getNext();
		while(!list.isEmpty()) {
			ATerm next = list.getFirst();
			if(Comparators.hashCodeComparator.compare(curr, next) >= 0)
				return false;
			curr = next;	
			list = list.getNext();	
		} 
		
		return true;
	}
	
	static public ATermList toSet(ATermList list) {
		if(isSet(list))
			return list;
			
		int size = list.getLength();	
				
		ATerm[] a = toArray(list);
		if(a == null || a.length < size)	
			a = new ATerm[Math.max(100, size)];

		Arrays.sort(a, 0, size, Comparators.hashCodeComparator);
				
		ATermList set = makeList(a[size - 1]);
		for(int i = size - 2; i >= 0; i--) {
			ATerm s = set.getFirst();
			if(!s.equals(a[i]))
				set = set.insert(a[i]);
		}
			
		return set;	
	}
	
	static public ATermList toSet(ATerm[] a, int size) {
		Arrays.sort(a, 0, size, Comparators.hashCodeComparator);
				
		ATermList set = makeList(a[size - 1]);
		for(int i = size - 2; i >= 0; i--) {
			ATerm s = set.getFirst();
			if(!s.equals(a[i]))
				set = set.insert(a[i]);
		}
			
		return set;	
	}

	static public ATermList toSet( Collection coll ) {
		int size = coll.size();	
				
		ATerm[] a = new ATermAppl[ size ];
		coll.toArray( a );

		return toSet( a, size );	
	}
	
	public static String toString(ATermAppl term) {
       if( isVar( term ) )
           return ((ATermAppl)term.getArgument(0)).getName();
       else if( ATermUtils.isLiteral( term ) ) {		
           String value = ((ATermAppl) term.getArgument(0)).getName();
           String lang = ((ATermAppl) term.getArgument(1)).getName();
           String datatypeURI = ((ATermAppl) term.getArgument(2)).getName();
           
           StringBuffer sb = new StringBuffer();
           sb.append('"').append(value).append('"');
           if(!lang.equals("")) 
               sb.append('@').append(lang);
           else if(!datatypeURI.equals(""))
               sb.append("^^").append(datatypeURI);
                   
           return sb.toString();           
       }
       else
           return term.getName();
	}
	
	static public ATerm[] toArray(ATermList list) {
		ATerm[] a = new ATerm[list.getLength()];
		
		for(int i = 0; !list.isEmpty(); list = list.getNext())
			a[i++] = list.getFirst();
		
		return a;
	}

	public final static void assertTrue(boolean condition) {
		if(!condition) {
			throw new RuntimeException("assertion failed.");
		}
	}		

	public static final boolean isPrimitive(ATermAppl c) {
		return c.getArity() == 0;		
	}

	public static final boolean isNegatedPrimitive(ATermAppl c) {
		return isNot(c) && isPrimitive((ATermAppl) c.getArgument(0));		
	}
	
	public static final boolean isPrimitiveOrNegated(ATermAppl c) {
		return isPrimitive(c) || isNegatedPrimitive(c);		
	}
	
	public static final boolean isBnode( ATermAppl name ) {
		return name.getName().startsWith(PelletOptions.BNODE);
	}

	public static final boolean isAnon( ATermAppl name ) {
		return name.getName().startsWith(PelletOptions.ANON);
	}

	public static Set listToSet(ATermList list) {
		Set set = new HashSet();
		while(!list.isEmpty()) {
			set.add(list.getFirst());
			list = list.getNext();
		}
		return set;
	}

	public static Set getPrimitives(ATermList list) {
		Set set = new HashSet();
		while(!list.isEmpty()) {
		    ATermAppl term = (ATermAppl) list.getFirst();
		    if(isPrimitive(term))
		        set.add(term);
			list = list.getNext();
		}
		return set;
	}
    
	public final static boolean isAnd(ATermAppl a) {
		return a.getAFun().equals(ANDFUN);
	}

	public final static boolean isOr(ATermAppl a) {
		return a.getAFun().equals(ORFUN);
	}
	
	public final static boolean isAllValues(ATermAppl a) {
		return a.getAFun().equals(ALLFUN);
	}

	public final static boolean isSomeValues(ATermAppl a) {
		return a.getAFun().equals(SOMEFUN);
	}
	
	public final static boolean isHasValue(ATermAppl a) {
		return a.getAFun().equals(SOMEFUN) && ((ATermAppl) a.getArgument(1)).getAFun().equals(VALUEFUN);
	}
	
	public final static boolean isNominal(ATermAppl a) {
		return a.getAFun().equals(VALUEFUN);
	}
	
	public final static boolean isOneOf(ATermAppl a) {
		if(!a.getAFun().equals(ORFUN))
			 return false;
		
		ATermList list = (ATermList) a.getArgument(0);
		while(!list.isEmpty()) {
			if(!isNominal((ATermAppl) list.getFirst()))
				return false;
			list = list.getNext();
		}
		return true;
	}
	
	public final static boolean isDataRange(ATermAppl a) {
		if(!a.getAFun().equals(ORFUN))
			 return false;
		
		ATermList list = (ATermList) a.getArgument(0);
		while(!list.isEmpty()) {
		    ATermAppl term = (ATermAppl) list.getFirst();
			if(!isNominal(term) || !isLiteral((ATermAppl) term.getArgument(0)))
				return false;
			list = list.getNext();
		}
		return true;
	}
	
	public final static boolean isNot(ATermAppl a) {
		return a.getAFun().equals(NOTFUN);
	}
	
	public final static boolean isMax(ATermAppl a) {
		return a.getAFun().equals(MAXFUN);
	}
	
	public final static boolean isMin(ATermAppl a) {
		return a.getAFun().equals(MINFUN);
	}

	public final static boolean isCard(ATermAppl a) {
	    if( isMin(a) || isMax(a) )
	        return true;
	    else if( isAnd(a) ) {
	        a = (ATermAppl) a.getArgument(0);
	        return isMin(a) || isMax(a);
	    }
	    
		return false;
	}

	public final static boolean isLiteral(ATermAppl a) {
		return a.getAFun().equals(LITFUN);
	}
	
	final static public boolean isVar(ATermAppl a) {
		return a.getAFun().equals(VARFUN);
	}
    
    public static boolean isComplexClass(ATerm c) {
    	if (c instanceof ATermAppl) {
    		ATermAppl a = (ATermAppl) c;
    		AFun f = a.getAFun();
    		return CLASS_FUN.contains( f );
    	}
    	return false;
    }    
	
	public static ATerm nnf(ATerm term) {
		if(term instanceof ATermList)
			return nnf((ATermList) term);
		if(term instanceof ATermAppl)
			return nnf((ATermAppl) term);

		return null;
	}
	
	public static ATermList nnf(ATermList list) {
		ATermList newList = factory.makeList();
		while (!list.isEmpty()) {
			newList = newList.append(nnf((ATermAppl) list.getFirst()));
			list = list.getNext();
		}

		return newList;
	}
	
    /*
     * return the term in NNF form, i.e. negation only occurs in
     * front of atomic concepts
     */
	public static ATermAppl nnf(ATermAppl term) {
		ATermAppl newterm = null;
		
//		if(new Throwable().fillInStackTrace().getStackTrace().length > 200)
//		    throw new InternalReasonerException("DEBUG: Too many nnf calls");
		
		AFun af = term.getAFun();

		if (af.equals(ATermUtils.NOTFUN)) { // Function is a NOT
			// Take the first argument to the NOT, then check
			// the type of that argument to determine what needs to be done.
			ATermUtils.assertTrue(af.getArity() == 1);
			ATermAppl arg = (ATermAppl) term.getArgument(0);
			af = arg.getAFun();
	
			if (arg.getArity() == 0) {
				newterm = term; // Negation is in as far as it can go
			}
			else if (af.equals(ATermUtils.NOTFUN)) { // Double negation.
				newterm = nnf((ATermAppl) arg.getArgument(0));
			}
			else if (af.equals(ATermUtils.VALUEFUN)) {
				newterm = term;
			}
			else if (af.equals(ATermUtils.MAXFUN)) {
				ATermInt n = (ATermInt) arg.getArgument(1);
				newterm = ATermUtils.makeMin(arg.getArgument(0), n.getInt() + 1);
			}
			else if (af.equals(ATermUtils.MINFUN)) {
				ATermInt n = (ATermInt) arg.getArgument(1);
				if (n.getInt() == 0)
					newterm = ATermUtils.BOTTOM;
				else
					newterm = ATermUtils.makeMax(arg.getArgument(0), n.getInt() - 1);
			}
			else if (af.equals(ATermUtils.ANDFUN)) {
				return ATermUtils.makeOr(nnf(negate((ATermList)arg.getArgument(0))));
			}
			else if (af.equals(ATermUtils.ORFUN)) {
				return ATermUtils.makeAnd(nnf(negate((ATermList)arg.getArgument(0))));
			}
			else if (af.equals(ATermUtils.SOMEFUN)) {
				newterm = ATermUtils.makeAllValues(arg.getArgument(0), nnf(makeNot(arg.getArgument(1))));
			}
			else if (af.equals(ATermUtils.ALLFUN)) {
				newterm = ATermUtils.makeSomeValues(arg.getArgument(0), nnf(makeNot(arg.getArgument(1))));
			}
			else {
				ATermUtils.assertTrue(false);
			}
		}
		else if (af.equals(ATermUtils.MINFUN) || af.equals(ATermUtils.MAXFUN)) {
			return term;
		}
		else {
			// Return the term with all of its arguments in nnf
			ATerm args[] = new ATerm[term.getArity()];
			for (int i = 0; i < term.getArity(); i++) {
				args[i] = nnf(term.getArgument(i));
			}
			newterm = factory.makeAppl(af, args);
		}

		ATermUtils.assertTrue(newterm != null);

		return newterm;
	} 

	public static Collection normalize( Collection coll ) {
	    List list = new ArrayList();
	    for( Iterator i = coll.iterator(); i.hasNext(); ) {
            ATermAppl term = (ATermAppl) i.next();
            list.add( normalize( term ) );
        }
		
		return list;
	}
	
	public static ATermList normalize( ATermList list ) {
	    int size = list.getLength();
		ATerm[] terms = new ATerm[ size ];
		for(int i = 0; i < size; i++) {
            terms[i] = normalize( (ATermAppl) list.getFirst() );
            list = list.getNext();
        }

		ATermList set = toSet( terms, size );
		
		return set;
	}

	/**
	 * Normalize the term by making following changes:
	 * <ul>
	 *   <li>or([a1, a2,..., an]) -> not(and[not(a1), not(a2), ..., not(an)]])</li>
	 *   <li>some(p, c) -> all(p, not(c))</li>
	 *   <li>max(p, n) -> not(min(p, n+1))</li>
	 * </ul>
	 * 
	 * @param term
	 * @return
	 */
	public static ATermAppl normalize(ATermAppl term) {
		ATermAppl norm = term;
		AFun      fun  = term.getAFun();
		ATerm     arg1  = (term.getArity() > 0) ? term.getArgument(0) : null;
		ATerm     arg2  = (term.getArity() > 1) ? term.getArgument(1) : null;

		if(fun.equals(NOTFUN)) { 
		    if( !isPrimitive( (ATermAppl) arg1 ) )
		        norm = simplify(makeNot(normalize((ATermAppl) arg1)));
		}
		else if(fun.equals(ANDFUN)) { 
			norm = simplify(makeAnd(normalize((ATermList) arg1)));
		}
		else if(fun.equals(ORFUN)) { 
			ATermList neg = negate((ATermList) arg1);
			ATermAppl and = makeAnd(neg);
			ATermAppl notAnd = makeNot(and);
			norm = normalize(notAnd);
		}
		else if(fun.equals(ALLFUN)) { 
			norm = simplify(makeAllValues(arg1, normalize((ATermAppl) arg2)));
		}
		else if(fun.equals(SOMEFUN)) { 
			norm = normalize(makeNot(makeAllValues(arg1, makeNot(arg2))));
		}
		else if(fun.equals(MAXFUN)) {
			norm = makeNot(makeMin(arg1, ((ATermInt) arg2).getInt() + 1));
		}
		else if(fun.equals(MINFUN)) {
			norm = simplify(norm);
		}

		return norm;	
	}

	/**
	 * simplfiy the term by making following changes:
	 * <ul>
	 *   <li>and([]) -> TOP</li>
	 *   <li>all(p, TOP) -> TOP</li>
	 *   <li>min(p, 0) -> TOP</li>
	 *   <li>and([a1, and([a2,...,an])]) -> and([a1, a2, ..., an]))</li>
	 *   <li>and([a, not(a), ...]) -> BOTTOM</li>
	 *   <li>not(C) -> not(simplify(C))</li>
	 * </ul>
	 * 
	 * @param term
	 * @return
	 */
	public static ATermAppl simplify( ATermAppl term ) {
        ATermAppl simp = term;
        AFun fun = term.getAFun();
        ATerm arg1 = (term.getArity() > 0) ? term.getArgument( 0 ) : null;
        ATerm arg2 = (term.getArity() > 1) ? term.getArgument( 1 ) : null;

        if( fun.equals( NOTFUN ) ) {
            ATermAppl arg = (ATermAppl) arg1;
            if( isNot( arg ) )
                simp = simplify( (ATermAppl) arg.getArgument( 0 ) );
            else if( isMin( arg ) ) {
                ATermInt n = (ATermInt) arg.getArgument( 1 );
                if( n.getInt() == 0 ) simp = BOTTOM;
            }
        }
        else if( fun.equals( ANDFUN ) ) {
            ATermList conjuncts = (ATermList) arg1;
            if( conjuncts.isEmpty() )
                simp = TOP;
            else {
                Set set = new HashSet();
                List negations = new ArrayList();
                MultiListIterator i = new MultiListIterator( conjuncts );
                while( i.hasNext() ) {
                    ATermAppl c = (ATermAppl) i.next();
                    if( c.equals( TOP ) )
                        continue;
                    else if( c.equals( BOTTOM ) )
                        return BOTTOM;
                    else if( isAnd( c ) )
                        i.append( (ATermList) c.getArgument( 0 ) );
                    else if( isNot( c ) )
                        negations.add( c );                    
                    else
                        set.add( c );
                }
                
                for( Iterator j = negations.iterator(); j.hasNext(); ) {
                    ATermAppl notC = (ATermAppl) j.next();
                    ATermAppl c = (ATermAppl) notC.getArgument( 0 );
                    if( set.contains( c ) )
                        return BOTTOM;
                }

                if( set.isEmpty() ) {
                    if( negations.isEmpty() )
                        return TOP;
                    else if( negations.size() == 1 )
                        return (ATermAppl) negations.get( 0 );
                }
                else if( set.size() == 1 && negations.isEmpty() )
                    return (ATermAppl) set.iterator().next();
                
                negations.addAll( set );                
                int size = negations.size();
                ATermAppl[] terms = new ATermAppl[size];
                negations.toArray( terms );
                simp = makeAnd( toSet( terms, size ) );                
            }
        }
        else if( fun.equals( ALLFUN ) ) {
            if( arg2.equals( TOP ) ) simp = TOP;
        }
        else if( fun.equals( MINFUN ) ) {
            ATermInt n = (ATermInt) arg2;
            if( n.getInt() == 0 ) simp = TOP;
        }

        return simp;
    }

	/**
	 * Creates a simplified and assuming that all the elements have already been normalized. 
	 * 
	 * @param conjuncts
	 * @return
	 */
	public static ATermAppl makeSimplifiedAnd( Collection conjuncts ) {
        Set set = new HashSet();
        List negations = new ArrayList();
        MultiListIterator listIt = new MultiListIterator( EMPTY_LIST );
        MultiIterator i = new MultiIterator( conjuncts.iterator(), listIt );
        while( i.hasNext() ) {
            ATermAppl c = (ATermAppl) i.next();
            if( c.equals( TOP ) )
                continue;
            else if( c.equals( BOTTOM ) )
                return BOTTOM;
            else if( isAnd( c ) )
                listIt.append( (ATermList) c.getArgument( 0 ) );
            else if( isNot( c ) )
                negations.add( c );                    
            else
                set.add( c );
        }
        
        for( Iterator j = negations.iterator(); j.hasNext(); ) {
            ATermAppl notC = (ATermAppl) j.next();
            ATermAppl c = (ATermAppl) notC.getArgument( 0 );
            if( set.contains( c ) )
                return BOTTOM;
        }

        if( set.isEmpty() ) {
            if( negations.isEmpty() )
                return TOP;
            else if( negations.size() == 1 )
                return (ATermAppl) negations.get( 0 );
        }
        else if( set.size() == 1 && negations.isEmpty() )
            return (ATermAppl) set.iterator().next();
        
        negations.addAll( set );                
        int size = negations.size();
        ATermAppl[] terms = new ATermAppl[size];
        negations.toArray( terms );
        return makeAnd( toSet( terms, size ) );  	    
	}
	
	/**
	 * @param ter
	 * @return
	 */
	
	//Recursive function that finds all the primitive classes
	//named in a complex concept, not looking inside restrictions
	//Added for topological sorting of quasi definition order.
	public static Set findPrimitives(ATermAppl ter) {
		Set result= new HashSet();
		if (isAnd(ter) || isOr(ter) || isNot(ter)  ){
		   ATermList args = ter.getArguments();
		   
		   while(!args.isEmpty()){
		   	if (args.getFirst() instanceof ATermAppl){  	   
		   	    
		   	    result.addAll(findPrimitives( (ATermAppl)args.getFirst()));
			   	args = args.getNext();
		   		
		     }
		   	if (args.getFirst() instanceof ATermList){
			        ATermList lis =  (ATermList)args.getFirst();
			   	    while(!lis.isEmpty()){
			   	       	result.addAll(	findPrimitives((ATermAppl)lis.getFirst()));
			   	    	lis= lis.getNext();
			   	    }
			   	   args = args.getNext();
			   		
		    }
		  }
		}
		
		if (isAllValues(ter) ||isSomeValues(ter)){
			ATermList args = ter.getArguments();
			args= args.getNext(); 
			if (args.getFirst() instanceof ATermAppl){  	   
		   	    
		   	    result.addAll(findPrimitives( (ATermAppl)args.getFirst()));
			   	args = args.getNext();
		   		
		     }
		   	if (args.getFirst() instanceof ATermList){
			        ATermList lis =  (ATermList)args.getFirst();
			   	    while(!lis.isEmpty()){
			   	       	result.addAll(	findPrimitives((ATermAppl)lis.getFirst()));
			   	    	lis= lis.getNext();
			   	    }
			   	   args = args.getNext();
			   		
		    }
		  }
			   
			 	     
		
		if (isPrimitive(ter) ){
			result.add(ter);
		}
		
		
		
		
		return result;
	}
//**********************************************************
	
}//end class
