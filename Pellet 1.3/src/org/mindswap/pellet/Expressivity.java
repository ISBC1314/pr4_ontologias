//The MIT License
//
//Copyright (c) 2003 Ron Alford, Mike Grove, Bijan Parsia, Evren Sirin
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to
//deal in the Software without restriction, including without limitation the
//rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
//sell copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in
//all copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
//FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
//IN THE SOFTWARE.

/*
 * Created on Aug 30, 2004
 */
package org.mindswap.pellet;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.mindswap.pellet.exceptions.InternalReasonerException;
import org.mindswap.pellet.output.ATermBaseVisitor;
import org.mindswap.pellet.output.ATermVisitor;
import org.mindswap.pellet.tbox.TBox;
import org.mindswap.pellet.utils.ATermUtils;

import aterm.ATermAppl;
import aterm.ATermInt;
import aterm.ATermList;

/**
 * @author Evren Sirin
 */
public class Expressivity {   
    KnowledgeBase kb;

    /**
     * not (owl:complementOf) is used directly or indirectly
     */
    private boolean hasNegation;
    
    /**
     * An inverse property has been defined or a property has been defined
     * as InverseFunctional
     */
    private boolean hasInverse;
    private boolean hasFunctionality;
    private boolean hasCardinality;
    private boolean hasFunctionalityD;
    private boolean hasCardinalityD;
    private boolean hasTransitivity;
    private boolean hasRoleHierarchy;
    private boolean hasDatatype;
    
    private boolean hasKeys;
    
    private boolean hasDomain;
    private boolean hasRange;
    
	/**
	 * The set of individuals in the Abox that have been used as nomianls,
	 * i.e. in an owl:oneOf enumeration or target of owl:hasValue restriction
	 */
    private Set nominals = new HashSet();
    
    private Visitor visitor;
    
    class Visitor extends ATermBaseVisitor implements ATermVisitor {
        public void visitTerm(ATermAppl term) {
        }

        public void visitAnd(ATermAppl term) {
            visitList((ATermList) term.getArgument(0));
        }

        public void visitOr(ATermAppl term) {
            hasNegation = true;
            visitList((ATermList) term.getArgument(0));
        }

        public void visitNot(ATermAppl term) {
            hasNegation = true;
            visit((ATermAppl) term.getArgument(0));
        }

        public void visitSome(ATermAppl term) {
            visit((ATermAppl) term.getArgument(1));
        }

        public void visitAll(ATermAppl term) {
            visit((ATermAppl) term.getArgument(1));
        }

        public void visitMin(ATermAppl term) {
            int cardinality = ((ATermInt) term.getArgument(1)).getInt();
            if(cardinality > 2) {
                hasCardinality = true;
                if( kb.getRole(term.getArgument(0)).isDatatypeRole() )
                    hasCardinalityD = true;    
            }
            else if(cardinality > 0) {
                hasFunctionality = true;
                if( kb.getRole(term.getArgument(0)).isDatatypeRole() )
                    hasFunctionalityD = true;    
            }
        }

        public void visitMax(ATermAppl term) {
            int cardinality = ((ATermInt) term.getArgument(1)).getInt();
            if(cardinality > 1)
                hasCardinality = true;
            else if(cardinality > 0)
                hasFunctionality = true;
        }

        public void visitHasValue(ATermAppl term) {
            nominals.add(term.getArgument(1));
        }

        public void visitValue(ATermAppl term) {
            nominals.add(term);
        }

        public void visitOneOf(ATermAppl term) {
            hasNegation = true;
            visitList((ATermList) term.getArgument(0));
        }

        public void visitLiteral(ATermAppl term) { 
            // nothing to do here
        }

		public void visitSubClass(ATermAppl term) {
		    throw new InternalReasonerException("This function should never be called!");
		}
    }
    
    Expressivity(KnowledgeBase kb) {
        this.kb = kb;
    }
    
    private void init() {
        hasNegation = false;
		hasInverse = false;
		hasDatatype = false;
		hasCardinality = false;
		hasFunctionality = false;
		hasTransitivity = false;
		hasRoleHierarchy = false;	
		hasDomain = false;
		
		nominals = new HashSet();   
		
		visitor = new Visitor();
    }
    
    public void compute() {
        init();
        
        processIndividuals();
        processClasses();
        processRoles();
    }
    
    public String toString() {
        String dl = "";
        
        if(hasNegation)
            dl = "ALC";
        else
            dl = "AL";
        
        if(hasTransitivity)
            dl += "R+";
        
        if(dl.equals("ALCR+"))
            dl = "S";
        
        if(hasRoleHierarchy)
            dl += "H";


        if(hasNominal())
            dl += "O";
        
        if(hasInverse)
            dl += "I";

        if(hasCardinality)
            dl += "N";
        else if(hasFunctionality)
            dl += "F";
        
        if(hasDatatype) {
            if( hasKeys )
                dl += "(Dk)";
            else
                dl += "(D)";
        }
        
        return dl;
    }
    
    protected void processClasses() {
        TBox tbox = kb.getTBox();
        
        ATermList UC = tbox.getUC();        
        if( UC != null ) {
            hasNegation = true;
            for(ATermList list = UC; !list.isEmpty(); list = list.getNext())
                visitor.visit((ATermAppl) list.getFirst());
        }
        
        Map unfoldingMap = tbox.getUnfoldingMap();
        Iterator i = unfoldingMap.values().iterator();
        while(i.hasNext() /*&& (!hasNegation || !hasCardinality)*/) {
            ATermAppl term = (ATermAppl) i.next();
            visitor.visit(term);
        }
    }
    
    protected void processIndividuals() {
        Iterator i = kb.getABox().getIndIterator();
        while(i.hasNext()) {
            Individual ind = (Individual) i.next();
            ATermAppl nominal = ATermUtils.makeValue(ind.getName());
            Iterator j = ind.getTypes().iterator();
            while(j.hasNext()) {
                ATermAppl term = (ATermAppl) j.next();
                
                if(term.equals(nominal))
                    continue;
                visitor.visit(term);
            }
        }
    }
    
    protected void processRoles() {
        for(Iterator i = kb.getRBox().getRoles().iterator(); i.hasNext(); ) {
            Role r = (Role) i.next();
            if(r.isDatatypeRole()) {
                hasDatatype = true;
                if(r.isInverseFunctional())
                    hasKeys = true;
            }
            // Each ObjectProperty has one anonymous inverse property
            // assigned by default. but this does not mean that ontology
            // uses inverse properties. KB has inverse properties only 
            // if a named property has a named inverse 
            if(!r.isAnon() && r.hasNamedInverse())
                hasInverse = true;
            // InverseFunctionalProperty declaration may mean that a named
            // property has an anonymous inverse property which is functional
            // The following condition checks this case
            if(r.isAnon() && r.isFunctional())
                hasInverse = true;            
            if(r.isFunctional())
                hasFunctionality = true;
            if(r.isTransitive())
                hasTransitivity = true;
            // Each property has itself included in the subroles set. We need
            // at least two properties in the set to conclude there is a role
            // hierarchy defined in the ontology
            if(r.getSubRoles().size() > 1)
                hasRoleHierarchy = true;
            
            ATermAppl domain = r.getDomain();
            if(domain != null) {
                hasDomain |= !domain.equals( ATermUtils.TOP );
                visitor.visit(domain);
            }
            
            ATermAppl range = r.getRange();
            if(range != null) {
                hasRange |= !range.equals( ATermUtils.TOP );
                visitor.visit(range);
            }
        }
    }    
    
    /**
     * @return Returns the hasCardinality.
     */
    public boolean hasCardinality() {
        return hasCardinality;
    }

    /**
     * @return Returns the hasFunctionality.
     */
    public boolean hasFunctionality() {
        return hasFunctionality;
    }

    /**
     * @return Returns the hasDatatype.
     */
    public boolean hasDatatype() {
        return hasDatatype;
    }
    
    /**
     * Returns true if a cardinality restriction (greater than 1) is
     * defined on any datatype property 
     */
    public boolean hasCardinalityD() {
        return hasCardinalityD;
    }

    /**
     * Returns true if a cardinality restriction (less than or equal to 1) is
     * defined on any datatype property 
     */
    public boolean hasFunctionalityD() {
        return hasFunctionalityD;
    }
    
    /**
     * @return Returns the hasInverse.
     */
    public boolean hasInverse() {
        return hasInverse;
    }
    /**
     * @return Returns the hasNegation.
     */
    public boolean hasNegation() {
        return hasNegation;
    }
    /**
     * @return Returns the hasNominal.
     */
    public boolean hasNominal() {
        return !nominals.isEmpty();
    }
    /**
     * @return Returns the hasRoleHierarchy.
     */
    public boolean hasRoleHierarchy() {
        return hasRoleHierarchy;
    }
    /**
     * @return Returns the hasTransitivity.
     */
    public boolean hasTransitivity() {
        return hasTransitivity;
    }
    
    public boolean hasDomain() {
        return hasDomain;
    }    
    
    public boolean hasRange() {
        return hasRange;
    }    
    
    public Set getNominals() {
        return nominals;
    }
    
    
    public boolean hasKeys() {
        return hasKeys;
    }  
}
