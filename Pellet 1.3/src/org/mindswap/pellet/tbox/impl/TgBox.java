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

package org.mindswap.pellet.tbox.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.utils.ATermUtils;

import aterm.ATerm;
import aterm.ATermAppl;
import aterm.ATermInt;
import aterm.ATermList;

public class TgBox extends TBoxImpl {
	// Statistics
	static int totalsizes = 0;
	
	// universal concept
	private ATermList UC = null;
	
	private static ATermList TOP_LIST = ATermUtils.makeList( ATermUtils.TOP );

	/*
	 * Constructors
	 */

	public TgBox(KnowledgeBase kb) {
		super(kb);
	}

	/*
	 * Statistics
	 */
	public static void printStatistics() {
		System.out.println("\nTg Statistics:\nSize of Tg: " + totalsizes);
	}

	/* 
	 * Utility Functions 
	 */

	public void internalize() {
		if( isEmpty() )
			return;
		
//		Timer timer = kb.timers.startTimer("internalize");
		
		ATermList conjuncts = ATermUtils.EMPTY_LIST;

		for (Iterator i = toList().iterator(); i.hasNext(); ) {
			ATermAppl def = (ATermAppl) i.next();
		
			if (def.getName().equals(ATermUtils.SAME)) {
				ATermAppl a = (ATermAppl) def.getArgument(0);
				ATermAppl b = (ATermAppl) def.getArgument(1);

				ATermAppl nota = ATermUtils.makeNot(a);
				ATermAppl notb = ATermUtils.makeNot(b);

				ATerm ora = ATermUtils.makeOr(a, notb);
				ATerm orb = ATermUtils.makeOr(nota, b);

				conjuncts = conjuncts.insert(ora);
				conjuncts = conjuncts.insert(orb);
			}
			else if (def.getName().equals(ATermUtils.SUB)) {
				ATermAppl a = (ATermAppl) def.getArgument(0);
				ATermAppl b = (ATermAppl) def.getArgument(1);

				ATermAppl nota = ATermUtils.makeNot(a);

				ATerm or = ATermUtils.makeOr(nota, b);

				conjuncts = conjuncts.insert(or);
			}
			else {
//				timer.stop();
				throw new RuntimeException("Bad TBox - had term of unknown type (" + def.getName() + ")");
			}
		}

		UC = conjuncts;
		if (UC.getLength() == 1)
			UC = ATermUtils.normalize(UC);
		else {
			ATermAppl and = ATermUtils.makeAnd(UC);
			and = ATermUtils.normalize(and);
			if (ATermUtils.isAnd(and))
				UC = (ATermList) and.getArgument(0);
			else
				UC = ATermUtils.makeList(and);
		}
//		timer.stop();
		
		if( UC.equals( ATermUtils.TOP ) || UC.equals( TOP_LIST ) )
		    UC = null;
	}

	public void absorb(TuBox tu) {
	    log.debug( "Absorption started" );
//		Timer timer = kb.timers.startTimer("absorb");
		
		Tu = tu;
		List terms = new ArrayList();

		for (Iterator i = toList().iterator(); i.hasNext(); ) {
			ATermAppl term = (ATermAppl) i.next();
			if (term.getName().equals(ATermUtils.SUB)) {
				terms.add(term);
			}
			else if (term.getName().equals(ATermUtils.SAME)) {
				ATermAppl sub1 = ATermUtils.makeSub(term.getArgument(0), term.getArgument(1));
				ATermAppl sub2 = ATermUtils.makeSub(term.getArgument(1), term.getArgument(0));
				terms.add(sub1);
				terms.add(sub2);
			}
			else {
				throw new RuntimeException("Term list contains something not a SUB or a SAME: " + term);
			}
		}

		if( log.isDebugEnabled() ) {
			log.debug("Tg.size was " + termhash.size() );
			log.debug("Tg was " + this);
			log.debug("Tu.size was " + Tu.size() );
			log.debug("Tu was " + Tu);
		}

		termhash = new Hashtable();
		//System.out.println("Terms: " + terms);

		for(Iterator i = terms.iterator(); i.hasNext();) {
		    kb.timers.checkTimer("preprocessing");

		    ATermAppl term = (ATermAppl) i.next();
			HashSet set = new HashSet();

			set.add(ATermUtils.nnf((ATermAppl) term.getArgument(0)));
			set.add(ATermUtils.nnf(ATermUtils.makeNot(term.getArgument(1))));
			
			//System.out.println("Absorbing "+set);
			absorbTerm(set);
		}

		if( log.isDebugEnabled() ) {
			log.debug("Tg.size is  " + size() );
			log.debug("Tg is " + this);
			log.debug("Tu.size is " + Tu.size() );
			log.debug("Tu is " + Tu);
			totalsizes += size();
		}
//		timer.stop();
		
		log.debug( "Absorption finished" );
	}

	private boolean absorbTerm(HashSet set) {
	    if(log.isDebugEnabled()) log.debug("Absorbing term "+set);
		while (true) {
		    log.debug("Absorb nominal");
			if (!PelletOptions.USE_PSEUDO_NOMINALS &&
                (PelletOptions.USE_NOMINAL_ABSORPTION || PelletOptions.USE_HASVALUE_ABSORPTION) && absorbNominal(set)){
			    if (DEBUG) System.out.println("Absorbed w/ Nominal: "+set);
				return true;
			}
			log.debug("Absorb II");
			if (absorbII(set)) {
			    log.debug("Absorbed");
				return true;
			}
			log.debug("Absorb III");
			if (absorbIII(set)) {
			    log.debug("Absorb III");
				continue;
			}
//			log.debug("Absorb IV");
//			if (absorbIV(set)) {
//			    log.debug("Absorb IV");
//				continue;
//			}
			log.debug("Absorb V");
			if (absorbV(set)) {
			    log.debug("Absorb V");
				continue;
			}
			log.debug("Absorb VI");
			if (absorbVI(set)) {
			    log.debug("Recursed on OR");
				return true;	
			}	
			log.debug("Absorb role");
			if (PelletOptions.USE_ROLE_ABSORPTION && absorbRole(set)) {
			    log.debug("Absorbed w/ Role");
				return true;
			}
			log.debug("Absorb VII");
			absorbVII(set);
			log.debug("Finished absorbTerm");
			return false;
		}
	}

	
	private boolean absorbNominal(HashSet set) {
		for( Iterator i = set.iterator(); i.hasNext(); ) {
            ATermAppl name = (ATermAppl) i.next();

            if( PelletOptions.USE_NOMINAL_ABSORPTION && (ATermUtils.isOneOf( name ) || ATermUtils.isNominal( name )) ) {
                i.remove();

                ATermList list = null;
                if( ATermUtils.isNominal( name ) )
                    list = ATermUtils.makeList( name );
                else
                    list = (ATermList) name.getArgument( 0 );

                ATermAppl c = ATermUtils.makeNot( 
                    ATermUtils.makeAnd( ATermUtils.makeList( set ) ) );

                if( log.isDebugEnabled() )
                    log.debug( "Absorb into " + list + " nominals (enumeration) " + c );

                while( !list.isEmpty() ) {
                    ATermAppl nominal = (ATermAppl) list.getFirst();
                    ATermAppl ind = (ATermAppl) nominal.getArgument( 0 );
                    kb.addIndividual( ind );
                    kb.addType( ind, c );
                    list = list.getNext();
                }
                return true;
            }
            else if( PelletOptions.USE_HASVALUE_ABSORPTION && ATermUtils.isHasValue( name ) ) {
                ATermAppl p = (ATermAppl) name.getArgument( 0 );                
                if( !kb.isObjectProperty( p ) )
                    continue;
                
                i.remove();
                ATermAppl c = ATermUtils.makeNot( 
                    ATermUtils.makeAnd( ATermUtils.makeList( set ) ) );
                
                ATermAppl nominal = (ATermAppl) name.getArgument( 1 );
                ATermAppl ind = (ATermAppl) nominal.getArgument( 0 );
                
                ATermAppl invP = kb.getProperty( p ).getInverse().getName();
                ATermAppl allInvPC = ATermUtils.makeAllValues( invP, c );
                
                if( log.isDebugEnabled() )
                    log.debug( "Absorb into " + ind + " with inverse of " + p + " for " + c );
                
                kb.addIndividual( ind );
                kb.addType( ind, allInvPC );

                return true;
            }
        }

		return false;
	}

	private boolean absorbRole(HashSet set) {
		for( Iterator i = set.iterator(); i.hasNext(); ) {
            ATermAppl name = (ATermAppl) i.next();

            if( ATermUtils.isSomeValues( name ) ) {
                ATerm r = name.getArgument( 0 );
                ATermAppl domain = ATermUtils.makeNot( ATermUtils.makeAnd( ATermUtils
                    .makeList( set ) ) );
                kb.addDomain( r, domain );
                return true;
            }
            else if( ATermUtils.isMin( name ) ) {
                ATerm r = name.getArgument( 0 );
                int n = ((ATermInt) name.getArgument( 1 )).getInt();

                // if we have min(r,1) sub ... this is also equal to a domain restriction
                if( n == 1 ) {
                    i.remove();
                    ATermAppl domain = ATermUtils.makeNot( 
                        ATermUtils.makeAnd( ATermUtils.makeList( set ) ) );
                    kb.addDomain( r, domain );
                    return true;
                }
            }
        }

		return false;
	}
	
	private boolean absorbII(HashSet set) {
		for (Iterator i = set.iterator(); i.hasNext(); ) {
			ATermAppl term = (ATermAppl) i.next();

			//System.out.println("Checking for term in Tu:"+term);
			if (Tu.contains(term)) {
				TermDefinition td = Tu.getTD(term);
				ATermAppl def = td.getDef(0);
				//System.out.println("Found Def:" + def);
				if (def.getName().equals(ATermUtils.SUB)) {
					set.remove(term);

					ATermList setlist = ATermUtils.makeList(set);
					ATermAppl conjunct = ATermUtils.makeAnd(setlist);
					conjunct = ATermUtils.makeAnd(def.getArgument(1), ATermUtils.makeNot(conjunct));
					ATermAppl sub = ATermUtils.makeSub(term, ATermUtils.nnf(conjunct));
					td.replaceDef(0, sub);

					return true;
				}
			}
			else if(term.getArity() == 0 && set.size() > 1) {
				set.remove(term);

				ATermList setlist = ATermUtils.makeList(set);
				ATermAppl conjunct = ATermUtils.makeAnd(setlist);
				conjunct = ATermUtils.makeNot(conjunct);
				ATermAppl sub = ATermUtils.makeSub(term, ATermUtils.nnf(conjunct));
				Tu.addDef(sub);				
				
				return true;
			}
		}

		return false;
	}

	private boolean absorbIII(HashSet set) {
		for (Iterator i = ((HashSet) set.clone()).iterator(); i.hasNext(); ) {
			ATermAppl term = (ATermAppl) i.next();
			ATermAppl negatedTerm = ATermUtils.nnf(ATermUtils.makeNot(term));

            if (Tu.contains(term)) {
                TermDefinition td = Tu.getTD(term);
                ATermAppl def = td.getDef(0);
                //System.out.println("Found Def:" + def);
                if (def.getName().equals(ATermUtils.SAME)) {

                    set.remove(term);
                    set.add(def.getArgument(1));

                    return true;
                }
            }
            else if (Tu.contains(negatedTerm)) {
				ATermAppl def = Tu.getTD(negatedTerm).getDef(0);
				//System.out.println("Found Def:" + def);
				if (def.getName().equals(ATermUtils.SAME)) {
					set.remove(term);
					set.add(ATermUtils.nnf(ATermUtils.makeNot(def.getArgument(1))));
                    
                    return true;
				}
			}
		}

		return false;
	}

	private boolean absorbV(HashSet set) {		
		for (Iterator iter = set.iterator(); iter.hasNext();) {
			
			ATermAppl term = (ATermAppl) iter.next();
			ATermAppl nnfterm = ATermUtils.nnf(term);
			//System.out.println(term);
			if (nnfterm.getName().equals(ATermUtils.AND)) {
				set.remove(term);
				for (ATermList andlist = (ATermList)nnfterm.getArgument(0); !andlist.isEmpty(); andlist = andlist.getNext()) {
					set.add(andlist.getFirst());
				}
				return true;
			}
		}
		return false;
	}

	private boolean absorbVI(HashSet set) {
		for (Iterator iter= set.iterator(); iter.hasNext();) {
			ATermAppl term = (ATermAppl) iter.next();
			ATermAppl nnfterm = ATermUtils.nnf(term);			
			if (nnfterm.getName().equals(ATermUtils.OR)) {
				set.remove(term);
				for (ATermList orlist = (ATermList) nnfterm.getArgument(0); !orlist.isEmpty(); orlist = orlist.getNext()) {
					HashSet cloned = (HashSet) set.clone();
					cloned.add(orlist.getFirst());
					//System.out.println("Term: "+term);
					//System.out.println("Recursing on "+cloned);
					//System.out.println("--");
					absorbTerm(cloned);
				}
				return true;
			}
		}

		return false;
	}

	private boolean absorbVII(HashSet set) {
		ATermList list = ATermUtils.makeList(set);
		ATermAppl name = ATermUtils.nnf((ATermAppl) list.getFirst());
		list = list.getNext();

		if (list.isEmpty()) {
			addDef(ATermUtils.makeSub(name, ATermUtils.nnf(ATermUtils.makeNot(name))));
		}
		else {
			addDef(ATermUtils.makeSub(name, ATermUtils.nnf(ATermUtils.makeNot(ATermUtils.makeAnd(list)))));
		}
		return true;
	}

	/**
	 * @return Returns the UC.
	 */
	public ATermList getUC() {
		return UC;
	}
	
	public int size() {
	    return UC == null ? 0 : UC.getLength();
	}
	
	public void print() {
	    if( UC != null ) {
	        for(ATermList list = ATermUtils.nnf(UC); !list.isEmpty(); list = list.getNext()) {
				System.out.println( list.getFirst() );
			}	        
	    }
	}	
}
