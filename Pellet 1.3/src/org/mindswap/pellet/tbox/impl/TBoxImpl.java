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

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.tbox.TBox;
import org.mindswap.pellet.utils.ATermUtils;

import aterm.ATerm;
import aterm.ATermAppl;
import aterm.ATermFactory;
import aterm.ATermList;

public class TBoxImpl implements TBox {
    protected static Log log = LogFactory.getLog( TBox.class );
    
	public static boolean DEBUG = false;
	
	final public static ATermFactory factory = ATermUtils.getFactory();
	
	protected KnowledgeBase kb;
	protected Map termhash = new HashMap();

    protected Set classes = new HashSet();	
    private Set allClasses;
    
    //This is for computing quasi definition order
    public Map refersTo = new HashMap();
    
	// Statistics
	static boolean hookset = false;
	static long splittime = 0;
	static long absorbtime = 0;

	// list of TBox axioms which are ATerms in the form sub(a, b) or
	// same(a, b)
	private List tboxAxioms = new ArrayList();

	public TuBox Tu = null;
	public TgBox Tg = null;

	// Debug - hold exception around to examine it.
	static Exception lastException = null;

	/*
	 * Constructors
	 */

	public TBoxImpl(KnowledgeBase kb) {
		this.kb = kb;
				
		if (DEBUG)
			addPrintHook();
	}

	/*
	 * Statistics
	 */
	public void addPrintHook() {
		if (hookset) {
			return;
		}
		hookset = true;

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				TBoxImpl.printStatistics();
				TuBox.printStatistics();
				TgBox.printStatistics();
			}
		});
	}

	public static void printStatistics() {
		System.out.println("\nTBox Statistics:");
		System.out.println("Time spent in createSplitTBox:" + splittime);
		System.out.println("Time spent in absorb:" + absorbtime);
	}
	
	public Set getAllClasses() {
	    if( allClasses == null ) {
	        allClasses = new HashSet( classes );
	        allClasses.add( ATermUtils.TOP );
	        allClasses.add( ATermUtils.BOTTOM );
	    }
	    return allClasses;
	}
	
	public void addAxiom(ATermAppl axiom) {
	    tboxAxioms.add(axiom);
	}
	
	public void addAxioms(List axioms) {
		tboxAxioms.addAll(axioms);
	}
	
	public void addAxioms(ATermList axioms) {
		while(!axioms.isEmpty()) {
			ATermAppl axiom = (ATermAppl) axioms.getFirst();
			addAxiom(axiom);
		}
	}
	
	public List getAxioms() {
		return tboxAxioms;
	}
	
	public void split() {
		Tu = new TuBox(kb);
		Tu.setAllowEvenLoops(false);
		Tg = new TgBox(kb);
//		Timer timer = kb.timers.startTimer("split");
		
		Iterator i = tboxAxioms.iterator();
		while (i.hasNext()) {
		    kb.timers.checkTimer("preprocessing");
		    
			ATermAppl term = (ATermAppl) i.next();

			if (!Tu.addIfUnfoldable(term)) {
				if (term.getName().equals(ATermUtils.SAME)) {
					// Try reversing the term if it is a 'same' construct
					ATermAppl name = (ATermAppl) term.getArgument(0);
					ATermAppl desc = (ATermAppl) term.getArgument(1);
					ATermAppl reversedTerm = ATermUtils.makeSame(desc, name);

					if (!Tu.addIfUnfoldable(reversedTerm)) {
						Tg.addDef(term);
					}
				} else {
					Tg.addDef(term);
				}
			}
		}
		
//		List sameAxioms = new ArrayList();
//		
//		Iterator i = tboxAxioms.iterator();
//		while (i.hasNext()) {
//		    kb.timers.checkTimer("preprocessing");
//		    
//			ATermAppl term = (ATermAppl) i.next();
//			if (term.getName().equals(ATermUtils.SAME)) 
//			    sameAxioms.add( term );
//			else if (!Tu.addIfUnfoldable(term))
//				Tg.addDef(term);
//		}
//		
//		i = sameAxioms.iterator();
//		while (i.hasNext()) {
//		    kb.timers.checkTimer("preprocessing");
//		    
//			ATermAppl term = (ATermAppl) i.next();
//
//			if (!Tu.addIfUnfoldable(term)) {
//				// Try reversing the term if it is a 'same' construct
//				ATermAppl name = (ATermAppl) term.getArgument(0);
//				ATermAppl desc = (ATermAppl) term.getArgument(1);
//				ATermAppl reversedTerm = ATermUtils.makeSame(desc, name);
//
//				if (!Tu.addIfUnfoldable(reversedTerm)) {
//					Tg.addDef(term);
//				}
//			}
//		}

//		timer.stop();
	}

	//Computing quasi definition order
	
//	public void computeDefOrder(){
//		Iterator it = this.Tu.termhash.entrySet().iterator();
//		while (it.hasNext()) {
//			Set aux = new HashSet();
//			Map.Entry entry = (Map.Entry) it.next();
//			ATerm c = (ATerm) entry.getKey();
//			TermDefinition td = (TermDefinition) entry.getValue();
//            //in "c" we have the name of the class being defined
//			ATermList list = ATermUtils.makeList();
//			//in "td" we have the definition of the "c" class
//			Iterator e = td.samelist.iterator();
//			while (e.hasNext()) {
//				ATermAppl axiom = (ATermAppl) e.next();
//				ATerm same = axiom.getArgument(1);
//				list = list.insert(same);
//			}//end While
//			
//			if (td.sub != null)
//				list = list.insert(td.sub.getArgument(1));
//			
//			while(!list.isEmpty()){
//				 //Process the right hand side of each axiom
//				
//				aux.addAll(ATermUtils.findPrimitives((ATermAppl)(list.getFirst())));
//				list= list.getNext();
//			}//end While
//			refersTo.put(c,aux);
//			
//		}//end While
//		
//		Iterator j= this.classes.iterator();
//			while(j.hasNext()){
//				ATerm cl = (ATerm)j.next();
//				if(!(refersTo.containsKey(cl))){
//					refersTo.put(cl,new HashSet());
//					System.out.println(cl + "was missing");}
//				   
//			}
//		
//		//Now we have to compute the transitive closure
////		boolean changed = true;
//	//	while(changed){
//		//   Iterator iter = refersTo.keySet().iterator();
//		  // while(iter.hasNext()){
//			//	ATerm te = (ATerm)iter.next();
//			//	Set aux = computeTransitiveClosure(te);
//			//	refersTo.put(te,aux);
//             //   if (!aux.equals(refersTo.get(te)))
//               // 	changed = true;
//               // else
//                //	changed=false;
//			//}
//		//}
//		Iterator i = refersTo.keySet().iterator();
//		while(i.hasNext()){
//			ATerm nextkey = (ATerm)i.next();
//			System.out.println("Class: " + nextkey + "refersTo" + refersTo.get(nextkey) );
//		}
//}//end method

		

	
	/**
	 * @param te
	 * @return
	 */
	/*
	private Set computeTransitiveClosure(ATerm te) {
		// TODO Auto-generated method stub
		Set result = new HashSet();
		result = (Set)refersTo.get(te);
		Iterator it = result.iterator();
		while(it.hasNext()){
			ATerm ele = (ATerm)it.next();
			result.addAll((Set)refersTo.get(ele));
			if(!((Set)refersTo.get(ele)).isEmpty() ){
					result.addAll(computeTransitiveClosure(ele));}
			
		}
			
		
		return result;
	}

	/*
	 * TBox Splitting
	 */

	public void absorb() {
		long time = System.currentTimeMillis();
		Tg.absorb(Tu);
		if (DEBUG) {
		    if(!Tu.isUnfoldable()) {
				System.out.println("Error in TgBox.absorb(Tu)");
				throw new RuntimeException("TgBox.absorb(Tu) made a not-unfoldable Tu!");
			}
		}

		absorbtime += System.currentTimeMillis() - time;
	}

	public boolean isEmpty() {
		return (termhash.size() == 0);
	}

	/**
	 * Returns the number of term definitions stored in this TBox.
	 * 
	 * @return
	 */
	public int size() {
		return termhash.size();
	}

	public TermDefinition getTD(ATerm name) {
		return (TermDefinition) termhash.get(name);

	}		

	public boolean contains(ATerm name) {
		if (termhash.containsKey(name)) {
			return true;
		}
		return false;
	}

	public void addDef(ATermAppl def) {
		ATermAppl name = (ATermAppl) def.getArgument(0);
		if (termhash.containsKey(name)) {
			getTD(name).addDef(def);
		} else {
			TermDefinition td = new TermDefinition();
			td.addDef(def);
			termhash.put(name, td);
		}
	}

	public String toString() {
		String str = "[";
		for (Iterator e = termhash.values().iterator(); e.hasNext();) {
			str += e.next() + "\n";
		}
		return str+"]";
	}

	public List toList() {
		ArrayList terms = new ArrayList(termhash.size());

		for (Iterator e = termhash.values().iterator(); e.hasNext();) {
			TermDefinition td = (TermDefinition) e.next();

			terms.addAll(td.toList());
		}
		
		return terms;
	}
	
	public static void main(String[] args) {
		ATermFactory factory = ATermUtils.getFactory();
		TBoxImpl tbox = null;
		ATerm t = null;

		try {
			if (args.length > 0) {
				FileInputStream input = new FileInputStream(args[0]);
				t = factory.readFromTextFile(input);
			} else {
				//t = factory.parse("[sub(a,b), sub(a,c), same(a,d),
				// same(a,e), sub(b,c)]");
				//t = factory.parse("[sub(a,b), sub(a,c), same(a,d),
				// same(a,e), sub(b,c),sub(\"g\",\"h\")]");
				//t = factory.parse("[sub(a,b),sub(b,c),sub(a,d),sub(d,e)]");
				t = factory.parse("[same(or(a,or(b,c)),or(x,y))]");
				t =
					factory.parse(
						"[sub(not(a),and(or(b,or(c,not(y))),or(b,or(c,not(x))))),sub(not(y),and(or(not(b),x),or(x,not(a)))),sub(c,or(x,y))]}");
			}

			System.out.println(t);

			tbox = new TBoxImpl(null);
			tbox.addAxioms((ATermList) t);
			tbox.split();
			tbox.absorb();
			try {
				//unfoldedTBox = tbox.unfold();
				System.out.println("\nUnfolded TBox:\n" + tbox.Tu);
			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				//internalizedTBox = tbox.internalize();
				tbox.Tg.internalize();
				System.out.println(
					"\nInternalized TBox:\n" + tbox.Tg.getUC());
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (args.length > 1) {
				ATermAppl conceptname = ATermUtils.makeTermAppl(args[1]);
				System.out.println("\nConcept for " + conceptname);
				System.out.println(
					ATermUtils.simplify(tbox.Tu.getTD(conceptname).getDef(0)));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * @return Returns the UC.
	 */
	public ATermList getUC() {
		if(Tg == null) return null;
		
		return Tg.getUC();
	}

    public void addClass( ATermAppl term ) {
        classes.add( term );
        if( allClasses != null )
            allClasses = null;
    }

    public Set getClasses() {
        return classes;
    }

    public List getAxioms( ATermAppl term ) {
        List axioms = Collections.EMPTY_LIST;
        TermDefinition def = Tg.getTD( term );
        if( def != null )
            axioms = def.toList();
        def = Tu.getTD( term );
        if( def != null ) {
            if( axioms.isEmpty() )
                axioms = def.toList();
            else
                axioms.addAll( def.toList() );
        }
        
        return axioms;
    }
    
    public void normalize() {
        Tu.normalize();
    }

    public void internalize() {
        Tg.internalize();
    }
    
    public int getTgSize() {
        return Tg.size();
    }

    public int getTuSize() {
        return Tu.size();
    }

    public Map getUnfoldingMap() {
        return Tu.unfoldedMap;
    }
        
    public void print() {
        Tg.print();
        Tu.print();
    }

	public boolean isPrimitive( ATermAppl c ) {
	    TermDefinition def = Tu.getTD( c );
		return def.isPrimitive() && !Tg.contains( c );
	}
}
