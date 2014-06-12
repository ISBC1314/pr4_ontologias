/*
 * Created on Sep 28, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.mindswap.pellet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.mindswap.pellet.exceptions.InternalReasonerException;
import org.mindswap.pellet.exceptions.UnsupportedFeatureException;
import org.mindswap.pellet.output.OutputFormatter;
import org.mindswap.pellet.taxonomy.Taxonomy;
import org.mindswap.pellet.tbox.TBox;
import org.mindswap.pellet.tbox.impl.TBoxImpl;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Bool;

import aterm.ATerm;
import aterm.ATermAppl;

/**
 * @author bernardo
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class EconnectedKB extends KnowledgeBase {
	private Map tboxes;
    private Map rboxes;
    Map classif;
    boolean checkAll;
    //This set is used for computing expressivity
    private Set numberRestrictions;
    
    private EconnExpressivity expressive;
    public EconnectedKB(){
    	super();
    	tboxes = new HashMap();
    	rboxes = new HashMap();
       //We are going to create a set of classification objects
    	classif = new HashMap();
    	expressive = new EconnExpressivity(this);
    	expressive.init();
    	
    	classif = new HashMap();
    	checkAll=true;
    	numberRestrictions = new HashSet();
    }

    public TBox getTBox(String ont){
    	return (TBox) tboxes.get(ont);
    }
    
    public RBox getRBox(String ont){
    	return (RBox) rboxes.get(ont);
    }
    
    public Map getTBoxes(){
    	return tboxes;
    }
    
    public boolean getCheckAll(){
    	return checkAll;
    }
    
    public Map getRBoxes(){
    	return rboxes;
    }
    
    public EconnExpressivity getEconnExpressivity() {
	    prepare();
    	
	    return expressive;
	}
    
    public void setOntology(String ont) {
    	if(!tboxes.containsKey(ont))
    		throw new InternalReasonerException("Ontology " + ont + " does not exist in the EconnectedKB!");
    	
        super.setOntology(ont);
        
	  setTBox((TBox)tboxes.get(ont));
	  setRBox((RBox)rboxes.get(ont));	  
	  taxonomy = (Taxonomy) classif.get(ont);
    }

    public void addOntology(String ont){
     	TBox t = new TBoxImpl(this);
    	RBox r = new RBox();
    	tboxes.put(ont,t);
    	rboxes.put(ont,r);
    }
    
	
	public void addLinkProperty(ATerm p) {
		status = RBOX_CHANGED;
		rbox.addLinkRole((ATermAppl) p);
		
		if( log.isDebugEnabled() ) 
            log.debug("link-prop " + p);
	}
	
	public boolean isLinkProperty(ATerm p) {
		return getPropertyType(p) == Role.LINK;
	}
	
	public boolean isProperty(ATerm p) {
		boolean result = false;
		for (Iterator i= rboxes.values().iterator(); i.hasNext();){
		    RBox rbox = (RBox) i.next();
			if(rbox.isRole(p))
				result=true;
		}
		return result;
	}
	
	
	/**
	 * @param r
	 * @param ro
	 */
	public void addInverseLink(Role roleS,Role roleR) {
		// TODO Auto-generated method stub
		status |= RBOX_CHANGED;
		//ATermAppl r = roleR.getName();
		//ATermAppl s = roleS.getName();
		
		//if ((roleR.getInverse().getName())!=null){
			//ATermAppl prevInvR = roleR.getInverse().getName();}
		//if (roleS.getInverse().getName()!=null){
			//ATermAppl prevInvS = roleS.getInverse().getName();}
			//inverse relation already defined
		//if((roleS.getInverse().getName()!=null)&&(roleR.getInverse().getName()!=null) ){
		  // if((roleS.getInverse().getName()).equals(roleR.getInverse().getName())&&(roleR.getInverse().getName()).equals(roleS.getInverse().getName()) ){
			//if(prevInvR.equals(s) && prevInvS.equals(r)){
			//	return;}}
				
			// this means r already has another inverse defined 
		roleR.setInverse(roleS);
	    roleS.setInverse(roleR);
				
		if( log.isDebugEnabled() ) 
            log.debug("inv-prop " + roleR.getName() + " " + roleS.getName());
	}
	
	public boolean isSubClassOf(ATermAppl c1, ATermAppl c2) {
		ensureConsistency();
        
        if( !isClass( c1 ) ) {
            if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
                return false;
            else
                throw new UnsupportedFeatureException( c1 + " is not a known class" );
        }

        if( !isClass( c2 ) ) {
            if( PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING )
                return false;
            else
                throw new UnsupportedFeatureException( c2 + " is not a known class" );
        }
        
		if( c1.equals( c2 ) )
            return true;

        // normalize concepts
        c1 = ATermUtils.normalize( c1 );
        c2 = ATermUtils.normalize( c2 );

        if( isClassified() ) {
            Bool isSubNode = taxonomy.isSubNodeOf( c1, c2 );
            if( isSubNode.isKnown() )
                return isSubNode.isTrue();
        }

        return abox.isSubClassOf( c1, c2 );
	}
	
	public boolean isSameAs(ATermAppl t1, ATermAppl t2) {
		Individual ind1 = abox.getIndividual(t1); 
		Individual ind2 = abox.getIndividual(t2); 

		if(ind1 == null)
			throw new UnsupportedFeatureException(t1 + " is not an individual!");
		if(ind2 == null)
			throw new UnsupportedFeatureException(t2 + " is not an individual!");
		
		//Added for Econnections
		if(ind1.getOntology()!= ind2.getOntology())
			return false;
			
		if(ind1.isSame(ind2))
			return true;
		
		ATermAppl c = ATermUtils.makeNot(ATermUtils.makeValue(t2));

		return !isType(t1, c);
	}
	
	
	
	
	public boolean isDifferentFrom(ATermAppl t1, ATermAppl t2) {
		Individual ind1 = abox.getIndividual(t1); 
		Individual ind2 = abox.getIndividual(t2); 

		if(ind1 == null)
			throw new UnsupportedFeatureException(t1 + " is not an individual!");
		if(ind2 == null)
			throw new UnsupportedFeatureException(t2 + " is not an individual!");
		//Added for Econn
		if(ind1.getOntology()!= ind2.getOntology())
			return true;
		
		if(ind1.isDifferent(ind2))
			return true;

		ATermAppl c = ATermUtils.makeValue(t2);

		return !isType(t1, c);
	}

	
    public void prepare()
    {
    	TBox t;
    	RBox r;
    	if(!isChanged()) return;
		
    	for(Iterator it= tboxes.keySet().iterator();it.hasNext();){
           String ont=(String)it.next();
    		t= (TBox)tboxes.get(ont);     	
            r= (RBox)rboxes.get(ont);     	
             
    		timers.startTimer("preprocessing");
    		t.split();
    		
//    		if(PelletOptions.PRINT_SIZE) {
//    			System.out.println("Ontology" + ont + "before normalization, absortion and internalization");
//    		    System.out.println("Tu is " + "(" + t.Tu.size() + ")");
//    			System.out.println("Tg was " + (t.Tg.isEmpty() ? "empty" : ("(" + t.Tg.size() + ")")));
//    		}		
//    		if(DEBUG) {
//    			System.out.println("Tu " + t.Tu);
//    			System.out.println("Tg " + t.Tg);
//    		
//    		}		
    		
    		t.absorb();    	
    		
//    		Here we call the function for computing the quasi
    		//definition order
    		//int sizeTg = t.Tg.size();
    		//if(sizeTg == 0 && PelletOptions.USE_DEFINITORIAL_ORDER==true){
    			//t.computeDefOrder();}
    		//************************
    		
    		t.normalize();
    		t.internalize();		
    		
    		r.computeRoleHierarchy();


        	// Absorption may change the KB but these are internal changes which 
    		// are not already dealt with. We set the status to UNCHANGED now
    		// because expressivity check can only work with prepared KB's
    		
    		//the expressivity is going to be different for each pair TBox\Rbox

    		 		
    		
    				
    		if(PelletOptions.PRINT_SIZE) {		
    			System.out.println("Ontology" + ont + "AFTER normalization, absortion and internalization");
//    		    System.out.println("Tg is " + (t.Tg.isEmpty() ? "empty" : ("(" + t.Tg.size() + ")")));
//    			if (t.Tg.getUC() != null) {
//    				System.out.println("UC is (" + t.Tg.getUC().getLength() + ")");
//    				System.out.println("UC is " + t.Tg.getUC());
//    			}
    			
    		//	System.out.println("Number of classes in the Econnected KB is (" + this.getClasses().size() + ")");
    		//	System.out.println("Number of nodes in ABox is (" + getABox().nodes.size() + ")");		
    		//	System.out.println("Expressivity " + expressivity);
    		}
    		//if(DEBUG) {
    			//System.out.println("Tu " + t.Tu);
    			//System.out.println("Tg " + t.Tg);
    		//}		
            if ( log.isDebugEnabled() ) 
                log.debug("Number of classes in the Econnected KB is (" + this.getClasses().size() + ")");
    		Iterator i=getABox().getNodes().iterator();
        	int count=0;
    		while (i.hasNext())
    		 {
    			Node aux = (Node)i.next();  
        		if(aux.getOntology()==ont){
    				if( log.isDebugEnabled() ) 
                        log.debug("The individual " + aux.getName() + "belongs to " + ont);
        			count++;}
    		 }
        	if( log.isDebugEnabled() ) 
                log.debug("Number of nodes in the ABox of the ontology " + ont + "is " + count);		
    		 
    	}//End for
    	
    	//expressive.compute();
    	//System.out.println("Expressivity of the Econnection " + expressive);
    	status = UNCHANGED;

		
		expressive.compute();
		
//		abox.initialize();		
		
    }//End Prepare
    
    public void classify(){
		if(isClassified()) return;
		
		prepare();
    	
    	if( log.isDebugEnabled() ) 
            log.debug("The Expressivity is: " + expressive.toString());
    	        
		
		ensureConsistency();
        checkAll=false;
    	//The Econnected KB has a list of TBoxes, each corresponding to
    	//a different ontology   	
    	for (Iterator i= tboxes.keySet().iterator(); i.hasNext();){
		    String ontName = (String)i.next(); 
    		//Set the current ontology
    		setOntology(ontName);
    		if( log.isDebugEnabled() ) 
                log.debug("Classifying "+ ontName);
    		//Debugging purposes
    		
    		//System.out.println("Classifying the following ontology: " + ontology);
    		//Create a new classify object (as it is done in Pellet)
    		builder = getTaxonomyBuilder();
    		
    		// Classify the current ontology
    		taxonomy = builder.classify();
    		
    		if( taxonomy == null )
    		    return;
    		
    		classif.put(ontology, taxonomy);    		    		
        }
    	if( log.isDebugEnabled() ) 
            log.debug(classif);
		status |= CLASSIFICATION;

   }//End classify

    public void printClassTree() {
    	printClassTree(new OutputFormatter());
    }
    
    public void printClassTree(OutputFormatter out) {       	
    	//Classify the Econnected KB
    	classify();
        //Create and Print the resulting class hierarchies
    	Iterator j = classif.keySet().iterator();
    	while (j.hasNext()){
    		String ont = (String)j.next();
        	setOntology(ont);
        	
           	out.printBold("Component Ontology: ").printLink(ontology);
           	taxonomy.print(out);
	     }
     }   
    
    /* (non-Javadoc)
     * @see org.mindswap.pellet.KnowledgeBase#chooseStrategy(org.mindswap.pellet.ABox)
     */
    CompletionStrategy chooseStrategy(ABox abox) {
		//EconnExpressivity e = getEconnExpressivity();
		
		if (expressive.hasInverses()){
		   return new EconnSHIONStrategy(abox);}
		else{
  		   return new PECSHIONStrategy(abox);
		}
    }
    
    private String findTBoxForClass(ATerm c) {
    	for (Iterator i = tboxes.keySet().iterator(); i.hasNext();){
    		String ont = (String) i.next();
		    TBox tbox = (TBox) tboxes.get(ont);
		    if(tbox.getClasses().contains(c))
		    	return ont;
    	}    	
    	
    	return null;
    }

    private String findRBoxForRole(ATerm r) {
    	for (Iterator i= rboxes.keySet().iterator(); i.hasNext();){
     		String ont = (String) i.next();
     		RBox rbox = (RBox) rboxes.get(ont);
		    if(rbox.isRole(r))
		    	return ont;
    	}    	
    	
    	return null;
    }

	/**
	 * @param term
	 * @return
	 */
	Role getRole(ATerm term) {
    	for (Iterator i= rboxes.values().iterator(); i.hasNext();){
		    RBox rbox = (RBox) i.next();
		    Role role = rbox.getRole(term);
		    if(role != null)
		    	return role;
    	}		
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.mindswap.pellet.KnowledgeBase#isSatisfiable(aterm.ATerm)
	 */
	public boolean isSatisfiable(ATermAppl c) {
		String save = ontology;
		if(!ATermUtils.isComplexClass(c)) {
			String ont = findTBoxForClass(c);
			setOntology(ont);
		}
		
		boolean sat = super.isSatisfiable(c);
		
		if(save != ontology && save != null)
			setOntology(save);
		
		return sat;
	}
	
	
	/* (non-Javadoc)
	 * @see org.mindswap.pellet.KnowledgeBase#addDomain(aterm.ATerm, aterm.ATermAppl)
	 */
	public void addDomain(ATerm p, ATermAppl c) {
		String ont = findRBoxForRole(p);
		
		status |= RBOX_CHANGED;
		RBox rbox = (RBox) rboxes.get(ont);
		Role r = rbox.getDefinedRole(p);		
		r.addDomain(c);
		
		if( log.isDebugEnabled() ) 
            log.debug("domain " + p + " " + c);
	}
	/* (non-Javadoc)
	 * @see org.mindswap.pellet.KnowledgeBase#addRange(aterm.ATerm, aterm.ATermAppl)
	 */
	public void addRange(ATerm p, ATermAppl c) {
		String ont = findRBoxForRole(p);
		
		status |= RBOX_CHANGED;
		RBox rbox = (RBox) rboxes.get(ont);
		Role r = rbox.getDefinedRole(p);		
		r.addRange(c);
		
		if( log.isDebugEnabled() ) 
            log.debug("range " + p + " " + c);
	}

	 public Set getNumberRestrictions(){
		return numberRestrictions;
	 }
	 
	/**
	 * @param foreignOnt
	 */
	
}//End EconnectedKB
