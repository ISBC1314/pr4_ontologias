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

package org.mindswap.pellet.tbox.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.exceptions.NotUnfoldableException;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Timer;

import aterm.ATerm;
import aterm.ATermAppl;
import aterm.ATermList;

public class TuBox extends TBoxImpl {
	/*
	 * Private Variables
	 */
	protected boolean allow_even_loops = false;
	protected Map unfoldedcache = new HashMap();
	protected Set unfoldMisses = new HashSet();
	static int resets = 0;
	static int createtime = 0;
	Map unfoldedMap = null;
	
	/*
	 * Constructors
	 */

	public TuBox(KnowledgeBase kb) {
		super(kb);
	}

	public void setAllowEvenLoops(boolean value) {
		allow_even_loops = value;
	}
	
	/**
	 * Normalize all the definitions in the Tu
	 *  
	 */
	public void normalize() {
//		Timer timer = kb.timers.startTimer("normalize");
		unfoldedMap = new HashMap();
		
		Iterator it = termhash.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			ATerm c = (ATerm) entry.getKey();
			TermDefinition td = (TermDefinition) entry.getValue();

			ATermList list = ATermUtils.EMPTY_LIST;
			Iterator e = td.getSames().iterator();
			while (e.hasNext()) {
				ATermAppl axiom = (ATermAppl) e.next();
				ATerm same = axiom.getArgument(1);
				list = list.insert(same);
			}
			
			if(!list.isEmpty()) {
			    ATermAppl or = ATermUtils.makeOr( list );
			    ATermAppl notOr = ATermUtils.makeNot( or );
				ATermAppl norm = ATermUtils.normalize( notOr );
				unfoldedMap.put( ATermUtils.makeNot( c ), norm );
			}
			
			if (td.getSub() != null)
				list = list.insert( td.getSub().getArgument( 1 ) );
			
			if( !list.isEmpty() ) {
			    ATermAppl and = ATermUtils.makeAnd( list );
				ATerm norm = ATermUtils.normalize( and );
				unfoldedMap.put( c, norm );
			}
		}
		
		if( PelletOptions.USE_ROLE_ABSORPTION ) {
		    ATermAppl sub = (ATermAppl) unfoldedMap.get(ATermUtils.TOP);
            if( sub != null && sub.getAFun().equals( ATermUtils.ANDFUN ) ) {
                ATermList l = (ATermList) sub.getArgument( 0 );
                ATermList newList = ATermUtils.EMPTY_LIST;
                for(; !l.isEmpty(); l = l.getNext()) {
                    ATermAppl term = (ATermAppl) l.getFirst();
                    if( term.getAFun().equals( ATermUtils.ALLFUN ) ) {
                        ATerm r = term.getArgument( 0 );
                        ATermAppl range = (ATermAppl) term.getArgument( 1 );
                        kb.addRange( r, range );
                    }
                    else
                        newList = newList.insert( term );
                }
                if( newList.isEmpty() )
		            unfoldedMap.remove(ATermUtils.TOP);
                else
		            unfoldedMap.put(ATermUtils.TOP, ATermUtils.makeAnd(newList));
            }
        }
//		timer.stop();
	}


	/*
	 * Accessor Methods
	 */

	public boolean addIfUnfoldable(ATermAppl term) {
		ATermAppl name = (ATermAppl) term.getArgument(0);
		ATermAppl body = (ATermAppl) term.getArgument(1);
		TermDefinition td = getTD(name);
		
		if (td == null) {
			td = new TermDefinition();
		}
		
		// Basic Checks
		TermDefinition tdcopy = new TermDefinition(td);
		tdcopy.addDef(term);
		if (tdcopy.isGCI() || !tdcopy.isUnique()) {
			return false;
		}
		
		// Loop Checks
		Set dependencies = ATermUtils.findPrimitives(body);
		Set seen = new HashSet();
		if (!td.dependencies.containsAll(dependencies)) {
			// Fast check failed
			for (Iterator iter = dependencies.iterator(); iter.hasNext();) {
				ATermAppl current = (ATermAppl) iter.next();
				
				boolean result = findTarget(current, name, seen);
				if (result) {
					return false;
				}
			}
		}
		
		
		td.addDef(term);
		termhash.put(name, td);
		
		return true;
	}

	/*
	 * Statistics
	 */

	public static void printStatistics() {
		System.out.println("\nTu Statistics:");
		System.out.println("Number of resets: " + resets);
		System.out.println("Time in createSplitTBox: " + createtime);
	}

	/*
	 * Testing methods
	 */

	public boolean isUnfoldable() {
		resetCache();
		try {
			unfold();
			return true;
		} catch (NotUnfoldableException e) {
			resetCache();
			return false;
		}
	}
	
	/*
	 * Utility methods
	 */
	
	
	protected boolean findTarget(ATermAppl term, ATermAppl target, Set seen) {
		List stack = new Vector();
		stack.add(term);
		
		while (!stack.isEmpty()) {
			kb.timers.checkTimer("preprocessing");
			ATermAppl current = (ATermAppl) stack.remove(0);
			
			if (seen.contains(current)) {
				continue;
			}
			seen.add(current);
			
			if (current.equals(target)) {
				return true;
			}
			
			TermDefinition td = this.getTD(current);
			if (td != null) {
				// Shortcut
				if (td.dependencies.contains(target)) {
					return true;
				}
				stack.addAll(0, td.dependencies);
			}
		}
		
		return false;
	}

	public void resetCache() {
		unfoldedcache = new HashMap();
		unfoldMisses = new HashSet();
		resets++;
	}

	// This is used to add debugging information to the NotUnfoldableException.
	protected void loopConstraintWrap(ATermAppl term, Map seen, int negations) throws NotUnfoldableException {
		try {
			loopConstraint(term, seen, negations);
		} catch (NotUnfoldableException e) {
			// This is mostly to add debugging information to the loops.
			if (termhash.containsKey(term)
				|| term.getAFun().equals(ATermUtils.NOTFUN)) {
				unfoldMisses.add(term);
				e.addTerm(term);
			}
			throw e;
		}
	}

	// loopConstraint is meant to be called only on completely unfolded terms
	protected void loopConstraint(ATermAppl term, Map seen, int negations) throws NotUnfoldableException {
		kb.timers.checkTimer("preprocessing");
		
		if (term.getArity() == 0) {
			if (seen.containsKey(term)) {
				int negationlevel = ((Integer) seen.get(term)).intValue();
				
				if ((negations - negationlevel) % 2 != 0) {
					throw new NotUnfoldableException(
						term,
						"Term "
							+ term
							+ " contains a loop (negations: "
							+ (negations - negationlevel)
							+ ").");
				} else if (!allow_even_loops) {
					throw new NotUnfoldableException(term, "Term "+term+" contains a loop (allow_even_loops=false)");
				}
			}
		} else {
			if (term.getAFun().equals(ATermUtils.MINFUN)
				|| term.getAFun().equals(ATermUtils.MAXFUN)
				|| term.getAFun().equals(ATermUtils.VALUEFUN)) {
				// Do Nothing
			} else if (term.getAFun().equals(ATermUtils.NOTFUN)) {
				loopConstraint(
					(ATermAppl) term.getArgument(0),
					seen,
					negations + 1);
			} else if (
				term.getAFun().equals(ATermUtils.SOMEFUN)
					|| term.getAFun().equals(ATermUtils.ALLFUN)) {
				loopConstraint(
					(ATermAppl) term.getArgument(1),
					seen,
					negations);
			} else if (
				term.getAFun().equals(ATermUtils.ANDFUN)
					|| term.getAFun().equals(ATermUtils.ORFUN)) {

				for (int i = 0; i < term.getArity(); i++) {
					ATerm arg = term.getArgument(i);
					if (arg instanceof ATermAppl)
						loopConstraintWrap((ATermAppl) arg, seen, negations);
					else {
						ATermList list = (ATermList) arg;
						for (ATermList l = list;
							!l.isEmpty();
							l = l.getNext()) {
							loopConstraintWrap(
								(ATermAppl) l.getFirst(),
								seen,
								negations);
						}
					}
				}
			} else {
				throw new RuntimeException(
					"ATermAppl of unknown use in unfolding!: " + term);
			}
		}
	}

	// This is used to add debugging information to the NotUnfoldableException.
	protected ATermAppl unfoldTermWrap(ATermAppl term, Map seen, int negations) throws NotUnfoldableException {
		try {
			return unfoldTerm(term, seen, negations);
		} catch (NotUnfoldableException e) {
			if (termhash.containsKey(term)
				|| term.getAFun().equals(ATermUtils.NOTFUN)) {
				unfoldMisses.add(term);
				e.addTerm(term);
			}
			throw e;
		}
	}

	public ATermAppl unfoldTerm(ATermAppl term) throws NotUnfoldableException {
		return unfoldTermWrap(term, new HashMap(), 0);
	}

	protected ATermAppl unfoldTerm(ATermAppl term, Map seen, int negations) throws NotUnfoldableException {
	    kb.timers.checkTimer("preprocessing");
	    
		ATermAppl unfoldedterm = null;
		if(DEBUG)
		    System.out.println("Unfolding " + term);

		if (unfoldMisses.contains(term)) {
			throw new NotUnfoldableException(
				term,
				"Term already deemed not unfoldable.");
		}

		// Is term a concept or a constructor?
		// If it is a concept, return the unfolded concept
		// If it is a constructor, return the constructor with unfolded
		// arguments
		if (term.getArity() == 0) {

			if (seen.containsKey(term)) {
				// Loop detected, check looping constraints.
				loopConstraint(term, seen, negations);
				// Can't put term in cache - could be other loops
				return term;
			} else if (unfoldedcache.containsKey(term)) {
				// Already unfolded this term before.
				unfoldedterm = (ATermAppl) unfoldedcache.get(term);
				// Check looping constraint
				loopConstraintWrap(unfoldedterm, seen, negations);
			} else if (termhash.containsKey(term)) {
				// Concept has a definition to check.
				seen.put(term, new Integer(negations));
				TermDefinition td = (TermDefinition) termhash.get(term);

				// Check whether the term is unique (only one definition)
				if (!td.isUnique()) {
					throw new NotUnfoldableException(
						term,
						"Term " + td + " is not unique.");
				}
				if (td.isGCI()) {
					throw new NotUnfoldableException(
						term,
						"Term " + td + " is a GCI.");
				}
				// Retreive only term definition
				ATermAppl termdef = td.getDef(0);

				// Go on about unfolding.
				// If def is like SAME(a,<foo>) return unfold(<foo>)
				// If def is like SUB(a, <foo>) return and(prime-a,
				// unfold(<foo>))
				if (termdef.getName().equals(ATermUtils.SAME)) {
					ATermAppl newterm =
						unfoldTermWrap(
							(ATermAppl) termdef.getArgument(1),
							seen,
							negations);
					seen.remove(term);
					unfoldedterm = newterm;
				} else if (termdef.getName().equals(ATermUtils.SUB)) {
					ATermAppl newterm =
						unfoldTermWrap(
							(ATermAppl) termdef.getArgument(1),
							seen,
							negations);
					seen.remove(term);
					// Make unique name

					ATermAppl aprime = ATermUtils.makeTermAppl("prime-" + term.getName());
					ATermAppl intersection = ATermUtils.makeAnd(aprime, newterm);
					unfoldedterm = intersection;

//					unfoldedterm = newterm;

				} else {
					throw new RuntimeException(
						termdef.toString()
							+ " is not a subclass or sameclass definition");
				}
			} else {
				// No concept definition, return as is.
				unfoldedterm = term;
			}

			// Put the term in the cache.
			unfoldedcache.put(term, unfoldedterm);

		} else {

			if (term.getAFun().equals(ATermUtils.MINFUN)
				|| term.getAFun().equals(ATermUtils.MAXFUN)
				|| term.getAFun().equals(ATermUtils.VALUEFUN)) {
				unfoldedterm = term;

			} else if (term.getAFun().equals(ATermUtils.NOTFUN)) {
				ATermAppl newterm =
					unfoldTermWrap(
						(ATermAppl) term.getArgument(0),
						seen,
						negations + 1);
				unfoldedterm = ATermUtils.makeNot(newterm);
			} else if (
				term.getAFun().equals(ATermUtils.SOMEFUN)
					|| term.getAFun().equals(ATermUtils.ALLFUN)) {
				ATerm[] arglist = new ATerm[2];
				arglist[0] = term.getArgument(0);
				arglist[1] =
					unfoldTermWrap(
						(ATermAppl) term.getArgument(1),
						seen,
						negations);
				unfoldedterm = factory.makeAppl(term.getAFun(), arglist);
			} else if (
				term.getAFun().equals(ATermUtils.ANDFUN)
					|| term.getAFun().equals(ATermUtils.ORFUN)) {

				ATerm[] arglist = new ATerm[term.getArity()];
				for (int i = 0; i < term.getArity(); i++) {
					ATerm arg = term.getArgument(i);
					if (arg instanceof ATermAppl)
						arglist[i] =
							unfoldTermWrap((ATermAppl) arg, seen, negations);
					else {
						ATermList unfoldedList = ATermUtils.EMPTY_LIST;
						ATermList list = (ATermList) arg;
						for (ATermList l = list; !l.isEmpty(); l = l.getNext())
							unfoldedList =
								unfoldedList.insert(
									unfoldTermWrap(
										(ATermAppl) l.getFirst(),
										seen,
										negations));
						arglist[i] = unfoldedList;
					}
				}
				ATermAppl newterm = factory.makeAppl(term.getAFun(), arglist);
				unfoldedterm = newterm;
			} else {
				throw new RuntimeException(
					"ATermAppl of unknown use in unfolding!: " + term);
			}
		}

		return unfoldedterm;
	}

	public void unfold() throws NotUnfoldableException {
		Timer currentTimer = kb.timers.startTimer("unfold");
		Map unfoldedMap = new HashMap();
		resetCache();
		try {
			for (Iterator i = termhash.values().iterator(); !i.hasNext(); ) {
				TermDefinition td = (TermDefinition) i.next();
				ATermAppl c = td.getName();
				ATerm notC = ATermUtils.makeNot(c);
				ATermAppl unfolded = ATermUtils.normalize(unfoldTerm(c));
				unfoldedMap.put(c, unfolded);
				unfoldedMap.put(notC, ATermUtils.negate(unfolded));
			}
		} finally {
			currentTimer.stop();
			currentTimer = null;
		}
	}

	public void print() {
		Iterator it = unfoldedMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			
			System.out.println(entry.getKey() + " -> " + entry.getValue());
		}
	}
}

