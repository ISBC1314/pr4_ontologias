package org.mindswap.pellet;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.mindswap.pellet.rete.Constant;
import org.mindswap.pellet.rete.Fact;
import org.mindswap.pellet.rete.Interpreter;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.QNameProvider;
import org.mindswap.pellet.utils.Timer;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLDescription;
import org.semanticweb.owl.model.OWLException;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLObjectProperty;
import org.semanticweb.owl.rules.OWLRule;
import org.semanticweb.owl.rules.OWLRuleAtom;
import org.semanticweb.owl.rules.OWLRuleClassAtom;
import org.semanticweb.owl.rules.OWLRuleIObject;
import org.semanticweb.owl.rules.OWLRuleIndividual;
import org.semanticweb.owl.rules.OWLRuleObjectPropertyAtom;
import org.semanticweb.owl.rules.OWLRuleVariable;

import aterm.ATermAppl;

public class RuleStrategy extends SHOINStrategy {
	
	public RuleStrategy(ABox abox) {
		super(abox);
		// TODO Auto-generated constructor stub
	}
	
	public List getVars(OWLRule rule)  throws OWLException {
		ArrayList vars = new ArrayList();    	
		
		//count the vars in the antecedents only (datalog safety)
		Iterator ants = rule.getAntecedents().iterator();    	
		while (ants.hasNext()) {
			OWLRuleAtom atom =(OWLRuleAtom) ants.next();
			if (atom instanceof OWLRuleClassAtom) {    			
				if (((OWLRuleClassAtom)atom).getArgument() instanceof OWLRuleVariable) {
					OWLRuleVariable v = ((OWLRuleVariable)((OWLRuleClassAtom)atom).getArgument());
					if (!(vars.contains(v.getURI()))) {
						vars.add(v.getURI());
					}    				
				}    			
			}
			else if (atom instanceof OWLRuleObjectPropertyAtom) {
				if (((OWLRuleObjectPropertyAtom)atom).getFirstArgument() instanceof OWLRuleVariable) {
					OWLRuleVariable v = ((OWLRuleVariable)((OWLRuleObjectPropertyAtom)atom).getFirstArgument());
					if (!(vars.contains(v.getURI()))) {
						vars.add(v.getURI());
					}    
					
				}
				if (((OWLRuleObjectPropertyAtom)atom).getSecondArgument() instanceof OWLRuleVariable) {
					OWLRuleVariable v = ((OWLRuleVariable)((OWLRuleObjectPropertyAtom)atom).getSecondArgument());
					if (!(vars.contains(v.getURI()))) {
						vars.add(v.getURI());
					}    	    				
				}    			
			}    		
		}
		return vars;
	}
	
	public void applyRULERule() {
		
		HashMap bindings = new HashMap();
		
//		OWLRule rule = null;
		// go through the rule and create the aterms    	 
		Iterator rulesIterator = this.abox.getKB().getRules().iterator();
		while (rulesIterator.hasNext()) {
			
			OWLRule rule = (OWLRule) rulesIterator.next();
			
			
			try {    			 
				List vars = getVars(rule);
				//create a binding
				
				// before enumertaing the bindings for a particular rule, 
				// we can eliminate some of the individuals 
				// if a rule is A(x) ^ B(x,y) -> D(x) and for some ind. a, a:D,
				// then no need to generate any bindings including ind. a
				
				//find eligible individuals for this rule
				total = 0;
				findBinding(0, bindings, vars, rule);
				System.out.println("total bindings:" + total);
				System.out.println("branches:" + abox.getBranch());
			} catch (OWLException e) {
				e.printStackTrace();
				
			}
			
		}
		
		
	}
	int total = 0;
	
	
	//is it legal to bind two variables to the same individual?    
	//let's not do it for now
	private void findBinding(int current, HashMap bindings, List vars, OWLRule rule) throws OWLException {
		Individual ind = null;
		if (current < vars.size()) {
			IndividualIterator i = abox.getIndIterator();            
			
			while (i.hasNext()) {
				
				ind = (Individual) i.next();
				
				if (!ind.isNamedIndividual() || bindings.containsValue(ind)) continue;
				
				/* check whether ind trivially satisfies rule */            	
				/* ind is used in place of current var */
				
//				
//				if (triviallySatisfied(ind, rule, (URI) vars.get(current), bindings)) {
////				System.out.println("TRIVIALLY!");
//				continue;
//				}
				
				bindings.put(vars.get(current),ind);
				if (triviallySatisfiedAllBindings(bindings, rule)) {
					
					bindings.remove(vars.get(current));
					continue;
				}
				
				findBinding(current+1, bindings, vars, rule);
				bindings.remove(vars.get(current));
				
			}
			
		} else {		
			
			// found a binding
			total++;    		
			if (ABox.DEBUG) {
				Iterator keys = bindings.keySet().iterator();
				while (keys.hasNext()) {
					Object k = keys.next();
					System.out.println("key:" + k + " value:" + bindings.get(k) + "-"); 
				}
				System.out.println(total);
			}
			if (!abox.isClosed()) {
				createDisjunctionsFromBinding(bindings, rule);
			}
			
		}	
	}	
	
	
	private boolean triviallySatisfiedAllBindings(HashMap bindings, OWLRule rule) throws OWLException {
		
		OWLRuleAtom head = (OWLRuleAtom) rule.getConsequents().iterator().next();
		if (head instanceof OWLRuleClassAtom) {
			//assumption: named class in the head
			OWLDescription clazz = ((OWLRuleClassAtom)head).getDescription();
			
			//convert owlapi description to an ATerm
			ATermAppl c = getTermForClass((OWLClass)clazz);
			
			//get the pellet individual object from the rule head atom
			Individual ind = getClassAtomInvidiual((OWLRuleClassAtom) head, bindings);
			
			//if the individual has the type already, disjunction is trivially satisfied
			if (ind!= null && ind.hasType(c)) 
				return true;
			
		} else if (head instanceof OWLRuleObjectPropertyAtom) {
			
			
			List inds = getObjectPropertyAtomIndividuals( (OWLRuleObjectPropertyAtom) head, bindings); 
			
			//there should be exactly two inds in the list 
			if ( inds.size() == 2) {
				OWLObjectProperty prop = ((OWLRuleObjectPropertyAtom)head).getProperty();
				ATermAppl p = getTermForProperty((OWLObjectProperty)prop);				
				
				ATermAppl notO = ATermUtils.negate(ATermUtils.makeValue( ((Individual)inds.get(1) ).getTerm()));
				ATermAppl notAllPnotO = ATermUtils.negate(ATermUtils.makeAllValues(p, notO)); 
				
				if ( ((Individual) inds.get(0)).hasType(notAllPnotO)) {
					return true;	 
				}
				
			} 
			
		}		 
		
		Iterator ants = rule.getAntecedents().iterator();
		while (ants.hasNext()) {
			
			OWLRuleAtom atom = (OWLRuleAtom) ants.next();
			if (atom instanceof OWLRuleClassAtom) {
				
				Individual ind = getClassAtomInvidiual((OWLRuleClassAtom) atom, bindings);
				OWLDescription clazz = ((OWLRuleClassAtom)atom).getDescription();
				
				ATermAppl c = getTermForClass((OWLClass)clazz);
				ATermAppl notC = ATermUtils.negate(c);				 
				
				if (ind != null && ind.hasType(notC)) 
					return true;
				
			} else  if (atom instanceof OWLRuleObjectPropertyAtom) {
				
				//return a list of inds, only two entries: [subject, object]
				List inds = getObjectPropertyAtomIndividuals( (OWLRuleObjectPropertyAtom) atom, bindings); 
				
				//there should be exactly two inds in the list 
				if ( inds.size() == 2) {
					
					OWLObjectProperty prop = ((OWLRuleObjectPropertyAtom)atom).getProperty();
					ATermAppl p = getTermForProperty((OWLObjectProperty)prop);
					//you want to add the value forall(p, not({o})) to s
					
					ATermAppl notO = ATermUtils.negate(ATermUtils.makeValue(((Individual)inds.get(1) ).getTerm()));
					ATermAppl allPNotO = ATermUtils.makeAllValues(p, notO); 					
					
					if (((Individual)inds.get(0) ).hasType(allPNotO)) {
						return true;
					} 		
				}
			}
			
		}
		return false;
	}
	
	
	private List getObjectPropertyAtomIndividuals(OWLRuleObjectPropertyAtom atom, HashMap bindings) throws OWLException {
		List inds = new ArrayList();
		
		if (atom.getFirstArgument() instanceof OWLRuleVariable) {
			OWLRuleVariable v = ((OWLRuleVariable)atom.getFirstArgument());
			if (bindings.containsKey(((OWLRuleVariable)v).getURI())) { 
				inds.add( (Individual) bindings.get( ((OWLRuleVariable)v).getURI()) );
			}
			
		} else if (atom.getFirstArgument() instanceof OWLRuleIndividual) {
			OWLIndividual owlInd = ((OWLRuleIndividual)atom.getFirstArgument()).getIndividual();
			inds.add(abox.getIndividual(ATermUtils.makeTermAppl(owlInd.getURI().toString())));
		}  
		
		if (atom.getSecondArgument() instanceof OWLRuleVariable) {
			OWLRuleVariable v = ((OWLRuleVariable)atom.getSecondArgument());
			if (bindings.containsKey(((OWLRuleVariable)v).getURI())) { 
				inds.add( (Individual) bindings.get( ((OWLRuleVariable)v).getURI()) );
			}
			
		} else if (atom.getSecondArgument() instanceof OWLRuleIndividual) {
			OWLIndividual owlInd = ((OWLRuleIndividual)atom.getSecondArgument()).getIndividual();
			inds.add(abox.getIndividual(ATermUtils.makeTermAppl(owlInd.getURI().toString())));
		}  
		
		return inds;
	}
	
	private Individual getClassAtomInvidiual(OWLRuleClassAtom atom, HashMap bindings) throws OWLException {
		//work with IObjects only so far (no datatypes)
		OWLRuleIObject v = atom.getArgument();    				 
		Individual ind = null;		 
		if (v instanceof OWLRuleVariable) {
			ind = (Individual) bindings.get( ((OWLRuleVariable)v).getURI());				 				 
			
		} else if (v instanceof OWLRuleIndividual) {
			OWLIndividual owlInd =((OWLRuleIndividual) v).getIndividual();
			ind = abox.getIndividual(ATermUtils.makeTermAppl(owlInd.getURI().toString()));			 
		}
		
		return ind;
	}
	
	private void createDisjunctionsFromBinding(HashMap bindings, OWLRule rule) throws OWLException {
		ATermAppl disjunction = null;
		ATermAppl[] disj = new ATermAppl[rule.getAntecedents().size()+1];
		Individual[] inds= new Individual[rule.getAntecedents().size()+1];
		int index = 0;		
			
		//head first
		OWLRuleAtom head = (OWLRuleAtom) rule.getConsequents().iterator().next();
		if (head instanceof OWLRuleClassAtom) {
			
			OWLDescription clazz = ((OWLRuleClassAtom)head).getDescription();
			ATermAppl c = getTermForClass((OWLClass)clazz);
			
			Individual ind = getClassAtomInvidiual((OWLRuleClassAtom) head, bindings);
			
			disj[index] = c; 
			inds[index] = ind;
			index++;
			
			if (disjunction==null) 
				disjunction = c;
			else
				disjunction = ATermUtils.makeOr(disjunction,c);
			
			
		} else if (head instanceof OWLRuleObjectPropertyAtom) {
			
			List propertyInds = getObjectPropertyAtomIndividuals((OWLRuleObjectPropertyAtom) head, bindings);
			Individual s = (Individual) propertyInds.get(0);
			Individual o = (Individual) propertyInds.get(1);
			
			OWLObjectProperty prop = ((OWLRuleObjectPropertyAtom)head).getProperty();
			ATermAppl p = getTermForProperty((OWLObjectProperty)prop);				
			
			ATermAppl notO = ATermUtils.negate(ATermUtils.makeValue(o.getTerm()));
			ATermAppl notAllPnotO = ATermUtils.negate(ATermUtils.makeAllValues(p, notO)); 
						
			disj[index] = notAllPnotO; 
			inds[index] = s;
			index++;				   
			if (disjunction==null) 
				disjunction = notAllPnotO;
			else
				disjunction = ATermUtils.makeOr(disjunction,notAllPnotO);	 						 			
		}
		
		Iterator ants = rule.getAntecedents().iterator();
		while (ants.hasNext()) {
			
			OWLRuleAtom atom = (OWLRuleAtom) ants.next();
			if (atom instanceof OWLRuleClassAtom) {
				
				OWLDescription clazz = ((OWLRuleClassAtom)atom).getDescription();
				ATermAppl c = getTermForClass((OWLClass)clazz);
				ATermAppl notC = ATermUtils.negate(c);
								
				Individual ind = getClassAtomInvidiual((OWLRuleClassAtom) atom, bindings);

				disj[index] = notC; 
				inds[index++] = ind;
				
				if (disjunction==null) 
					disjunction = notC;
				else
					disjunction = ATermUtils.makeOr(disjunction,notC);	 						 

			} else  if (atom instanceof OWLRuleObjectPropertyAtom) {
				List propertyInds = getObjectPropertyAtomIndividuals((OWLRuleObjectPropertyAtom) atom, bindings);
				Individual s = (Individual) propertyInds.get(0);
				Individual o = (Individual) propertyInds.get(1);
								
				OWLObjectProperty prop = ((OWLRuleObjectPropertyAtom)atom).getProperty();
				ATermAppl p = getTermForProperty((OWLObjectProperty)prop);
				
				ATermAppl notO = ATermUtils.negate(ATermUtils.makeValue(o.getTerm()));
				ATermAppl allPNotO = ATermUtils.makeAllValues(p, notO); 
				
				
//				if (s.hasType(allPNotO)) {
//				triviallySatisfied = true;
//				} else { 			
				disj[index] = allPNotO; 
				inds[index] = s;
				index++;				   
				if (disjunction==null) 
					disjunction = allPNotO;
				else
					disjunction = ATermUtils.makeOr(disjunction,allPNotO);	 						 				
//				}				
			}
			
		}
				
		
		if (!abox.isClosed()) {						
			//create a ruleBranch with a list of inds and corresponding disjuncts				
			RuleBranch r = new RuleBranch(abox, this, inds[0], inds, disjunction, new DependencySet(abox.getBranch()), disj);			    
			addBranch(r);
			r.tryBranch();
		}		
	}
	
	public ATermAppl term(String s) {
		return ATermUtils.makeTermAppl(s);
	}
	
	public static QNameProvider qnames = new QNameProvider();
	
	private ATermAppl getTermForClass(OWLClass clazz) throws OWLException {
		URI uri = clazz.getURI();
		if(PelletOptions.USE_LOCAL_NAME)
			return term(uri.getFragment());
		else if(PelletOptions.USE_QNAME)
			return term(qnames.shortForm(uri));
		
		return term(uri.toString());
	}
	
	private ATermAppl getTermForProperty(OWLObjectProperty prop) throws OWLException {
		URI uri = prop.getURI();
		if(PelletOptions.USE_LOCAL_NAME)
			return term(uri.getFragment());
		else if(PelletOptions.USE_QNAME)
			return term(qnames.shortForm(uri));
		
		return term(uri.toString());
	}
	
	ABox complete() {
		Timer t;
		
		completionTimer.start();
		
		Expressivity expressivity = abox.getKB().getExpressivity();        
		boolean fullDatatypeReasoning =
			PelletOptions.USE_FULL_DATATYPE_REASONING &&
			(expressivity.hasCardinalityD() || expressivity.hasKeys());
		
		initialize();
		
		//run the RETE once when the rules are not applied 
		if (!abox.ranRete && abox.rulesNotApplied) {
			// initialize and run the rete 
			Interpreter interp = new Interpreter();        	        	
			
			try {
				List rules = interp.rete.convertSWRLRules(abox.getKB().getRules());	        	        	
				interp.rete.compile(rules);
			} catch (OWLException e) {
				System.err.println("Exception while converting rules!");
				System.err.println(e);
			}
			
			Set facts = interp.rete.compileFacts(abox);
			
			interp.addFacts(facts, true);
			interp.run();
			System.out.println();
			System.out.println( interp.inferredFacts.size() +  " inferred fact(s)");
			
			//need to add the inferred facts back to the tableau 
			Iterator it = interp.inferredFacts.iterator();
			DependencySet ds = DependencySet.INDEPENDENT;
			while (it.hasNext()) {
				Fact f = (Fact) it.next();
				
				if (((Constant)f.getPred()).getValue().equals("type")) {        				
					// add a type assertion for the individual
					//TODO: base the rete on aterms, too  - avoid this conversion bs
					Individual ind = abox.getIndividual(ATermUtils.makeTermAppl(f.getSubj().toString()));
					ATermAppl type = ATermUtils.makeTermAppl(f.getObj().toString());
					ind.addType(type, ds );        			
				} else {
					// add code for inferring roles, too
					// TODO:test this
					Individual from = abox.getIndividual(ATermUtils.makeTermAppl(f.getSubj().toString()));
					Individual to = abox.getIndividual(ATermUtils.makeTermAppl(f.getObj().toString()));
					
					Role r = abox.getRole(ATermUtils.makeTermAppl(f.getObj().toString()));
					addEdge(from, r, to, ds);        		
				}
				
			}
			abox.ranRete = true;	
		}
		
		while(!abox.isComplete()) {
			while(abox.changed && !abox.isClosed()) {                
				completionTimer.check();
				
				abox.changed = false;
				
				if(ABox.DEBUG) {
					System.out.println("Branch: " + abox.getBranch() +
							", Depth: " + abox.treeDepth + ", Size: " + abox.getNodes().size() + 
							", Mem: " + (Runtime.getRuntime().freeMemory()/1000) + "kb");
					abox.validate();
//					printBlocked();
					abox.printTree();
				}
				
				IndividualIterator i = abox.getIndIterator();
				
				if( !PelletOptions.USE_PSEUDO_NOMINALS ) {
					t = timers.startTimer( "rule-nominal");
					applyNominalRule(i);
					t.stop();
					if(abox.isClosed()) break;
				}
				
				t = timers.startTimer("rule-guess");
				applyGuessingRule(i);
				t.stop();
				if(abox.isClosed()) break;
				
				t = timers.startTimer("rule-max");
				applyMaxRule(i);
				t.stop();
				if(abox.isClosed()) break;
				
				if( fullDatatypeReasoning ) {
					t = timers.startTimer("check-dt-count");
					checkDatatypeCount(i);
					t.stop();
					if(abox.isClosed()) break;
					
					t = timers.startTimer("rule-lit");
					applyLiteralRule();
					t.stop();
					if(abox.isClosed()) break;
				}
				
				t = timers.startTimer("rule-unfold");
				applyUnfoldingRule(i);
				t.stop();
				if(abox.isClosed()) break;
				
				t = timers.startTimer("rule-disj");
				applyDisjunctionRule(i);
				t.stop();
				if(abox.isClosed()) break;
				
				t = timers.startTimer("rule-some");
				applySomeValuesRule(i);
				t.stop();
				if(abox.isClosed()) break;
				
				t = timers.startTimer("rule-min");
				applyMinRule(i);
				t.stop();
				if(abox.isClosed()) break;
							
				if (abox.DEBUG) {
					System.out.println("Applying RULE rule at branch:" + abox.getBranch());
				}
				if (abox.rulesNotApplied) {
					abox.rulesNotApplied = false;                	
					applyRULERule();  
					
				}
				if(abox.isClosed()) break;
				
				
//				t = timers.startTimer("rule-max");
//				applyMaxRule(i);
//				t.stop();
//				if(abox.isClosed()) break;
//				
//				t = timers.startTimer("rule-lit");
//				applyLiteralRule();
//				t.stop();
//				if(abox.isClosed()) break;
			}
			
			if( abox.isClosed() ) {
				if(ABox.DEBUG)
					System.out.println(
							"Clash at Branch (" + abox.getBranch() + ") " + abox.getClash());
				
				if(backtrack())
					abox.setClash( null );
				else
					abox.setComplete( true );
			}
			else {
				if (PelletOptions.SATURATE_TABLEAU) {
					Branch unexploredBranch = null;
					for (int i=abox.getBranches().size()-1; i>=0; i--) {
						unexploredBranch = (Branch) abox.getBranches().get(i);
						unexploredBranch.tryNext++;
						if (unexploredBranch.tryNext < unexploredBranch.tryCount) {
							restore(unexploredBranch);
							System.out.println("restoring branch "+ unexploredBranch.branch + " tryNext = "+unexploredBranch.tryNext + " tryCount = "+unexploredBranch.tryCount);
							unexploredBranch.tryNext();
							break;
						}
						else { 
							System.out.println("removing branch "+ unexploredBranch.branch);
							abox.getBranches().remove(i);
							unexploredBranch = null;
						}
					}
					if(unexploredBranch == null) {
						abox.setComplete( true );	
					}
				}
				else abox.setComplete( true );
			}
		}
		
		completionTimer.stop();
		
		return abox;
	}
}
