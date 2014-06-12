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

package org.mindswap.pellet;


import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.SetUtils;

import aterm.ATermAppl;

/*
 * Created on Aug 27, 2003
 *
 */

/**
 * @author Evren Sirin
 *
 */
public class Role {
	final public static String[] TYPES = {"Untyped", "Object", "Datatype", "Annotation", "Ontology"	};
    /**
     * @deprecated Use UNTYPED instead
     */
    final public static int UNDEFINED  = 0;
	final public static int UNTYPED    = 0;
	final public static int OBJECT     = 1;
	final public static int DATATYPE   = 2;
	final public static int ANNOTATION = 3;
	final public static int ONTOLOGY   = 4;
	//Added for Econnections
	final public static int LINK   = 5;
	private String foreignOntology;
	//***************************************************
	
	private ATermAppl   name;
	
	private int  type = UNTYPED; 
	private Role inverse = null;

	private Set subRoles = new HashSet();
	private Set superRoles = new HashSet();
	
	private Set functionalSupers = SetUtils.EMPTY_SET;
	private Set transitiveSubRoles = SetUtils.EMPTY_SET;
	
	private ATermAppl domain = null;
	private ATermAppl range  = null;

	private Set domains = null;
	private Set ranges  = null;
	
	private boolean isTransitive = false;
	private boolean isFunctional = false;
    private boolean isInverseFunctional = false;
	
	public Role(ATermAppl name) {
		this(name, UNTYPED);
	}
   
	public Role(ATermAppl name, int type) {
		this.name = name;
		this.type = type;
		
		addSubRole(this);
		addSuperRole(this);
	}
	
	public boolean equals(Object o) {
		if(o instanceof Role)
			return name == ((Role)o).getName();		
		
		return false;
	}

	public String toString() {
		return name.getArity() == 0 ? name.getName() : name.toString();
	}
	
	public String debugString() {
		String str = "(" + TYPES[type] + "Role " + name;
		if(isTransitive) str += " Transitive";
		if(isSymmetric()) str += " Symmetric";
		if(isFunctional) str += " Functional";
		if(isInverseFunctional()) str += " InverseFunctional";
		if(type == OBJECT || type == DATATYPE) {
			if(domain != null) str += " domain=" + domain;
			if(range != null) str += " range=" + range;
			str += " superPropertyOf=" + subRoles;
			str += " subPropertyOf=" + superRoles;
		}
		str += ")";
		
		return str; 
	}


	/**
	 * r is subrole of this role
	 * @param r
	 */
	public void addSubRole(Role r) {
		subRoles.add(r);
	}
	
	/**
	 * r is superrole of this role
	 * @param r
	 */
	public void addSuperRole(Role r) {		
		superRoles.add(r);
	}
	
	void normalize() {
		if( domains != null ) {
		    if( domains.size() == 1 )
		        domain = (ATermAppl) domains.iterator().next();
		    else
		        domain = ATermUtils.makeSimplifiedAnd( domains );
			domains = null;
		}
		if( ranges != null ) {
		    if( ranges.size() == 1 )
		        range = (ATermAppl) ranges.iterator().next();
		    else
		        range = ATermUtils.makeSimplifiedAnd( ranges );
			ranges = null;
		}
	}
	
	public void addDomain(ATermAppl a) {
	    if( domains == null )
	        domains = new HashSet();
	    domains.add( ATermUtils.normalize( a ) );
	}
	
	public void addRange(ATermAppl a) {
	    if( ranges == null )
	        ranges = new HashSet();
	    ranges.add( ATermUtils.normalize( a ) );
	}

	public void addDomains(Set a) {
	    if( domains == null )
	        domains = new HashSet();
	    domains.addAll( a );
	}
	
	public void addRanges(Set a) {
	    if( ranges == null )
	        ranges = new HashSet();
	    ranges.addAll( a );
	}
	
	public boolean isObjectRole() {
		return type == OBJECT; 
	}	

	//Added for Econnections
	public boolean isLinkRole() {
		return type == LINK; 
	}	

	public boolean isDatatypeRole() {
		return type == DATATYPE; 
	}

	public boolean isOntologyRole() {
		return type == Role.ONTOLOGY; 
	}	
	/**
	 * check if a role is declared as datatype property
	 */
	public boolean isAnnotationRole() {
		return type == Role.ANNOTATION; 
	}
    
    public boolean isUntypedRole() {
        return type == UNTYPED;
    }

	/**
	 * @return
	 */
	public Role getInverse() {
		return inverse;
	}

	public boolean hasNamedInverse() {
		return inverse != null && !inverse.isAnon();
	}
	
	/**
	 * @return
	 */
	public boolean isFunctional() {
		return isFunctional;
	}	

	public boolean isInverseFunctional() {
		return isInverseFunctional; 
	}

	/**
	 * @return
	 */
	public boolean isSymmetric() {
		return inverse != null && inverse.equals(this); 
	}

	/**
	 * @return
	 */
	public boolean isTransitive() {
		return isTransitive;
	}
	
	public boolean isAnon() {
	    return name.getArity() != 0;
	}
	
	/**
	 * @return
	 */
	public ATermAppl getName() {
		return name;
	}

	public ATermAppl getDomain() {
		return domain;
	}

	public ATermAppl getRange() {
		return range;
	}

	public Set getDomains() {
		return domains;
	}

	public Set getRanges() {
		return ranges;
	}

	public Set getSubRoles() {
		return subRoles;
	}

	/**
	 * @return
	 */
	public Set getSuperRoles() {
		return superRoles;
	}

	/**
	 * @return
	 */
	public int getType() {
		return type;
	}

	public String getTypeName() {
		return TYPES[type];
	}
	
	public boolean isSubRoleOf(Role r) {
		return superRoles.contains(r);
	}

	public boolean isSuperRoleOf(Role r) {
		return subRoles.contains(r);
	}


	/**
	 * @param term
	 */
	public void setInverse(Role term) {
		inverse = term;
	}

	/**
	 * @param b
	 */
	public void setFunctional(boolean b) {
		isFunctional = b;
	}

    public void setInverseFunctional(boolean b) {
        isInverseFunctional = b;
    }
    
	/**
	 * @param b
	 */
	public void setTransitive(boolean b) {
		isTransitive = b;
	}
	
	/**
	 * @param type The type to set.
	 */
	public void setType(int type) {
		this.type = type;
	}
	
	/**
	 * @param subRoles The subRoles to set.
	 */
	public void setSubRoles(Set subRoles) {
		this.subRoles = subRoles;
	}
	
	/**
	 * @param superRoles The superRoles to set.
	 */
	public void setSuperRoles(Set superRoles) {
		this.superRoles = superRoles;
	}

	/**
	 * @return Returns the functionalSuper.
	 */
	public Set getFunctionalSupers() {
		return functionalSupers;
	}
	
	/**
	 * @param functionalSuper The functionalSuper to set.
	 */
	public void addFunctionalSuper(Role r) {
	    // to save store space we also use singleton sets (which are immutable)
	    // to store functional supers so we need to take care of special cases
	    // while updatding the set 
	    if( functionalSupers.isEmpty() ) {
	        functionalSupers = SetUtils.singleton( r );
	    }
	    else if( functionalSupers.size() == 1 ) {
            Role fs = (Role) functionalSupers.iterator().next();
	        if( fs.isSubRoleOf( r ) ) {
	            functionalSupers = SetUtils.singleton( r );
	        }
	        else if( !r.isSubRoleOf( fs ) ) {
	            functionalSupers = new HashSet( 2 );
	            functionalSupers.add( fs );
	            functionalSupers.add( r );
	        }	            
        }
        else {
	        for(Iterator i = functionalSupers.iterator(); i.hasNext();) {
                Role fs = (Role) i.next();
		        if( fs.isSubRoleOf( r ) ) {
		            functionalSupers.remove( fs );
		            functionalSupers.add( r );
		            return;
		        }
		        else if( r.isSubRoleOf( fs ) ) {
		            return;
		        }
            }
            functionalSupers.add( r );
        }	    
	}
	
	public boolean isSimple() {
	    return !isTransitive && transitiveSubRoles.isEmpty();
	}
	
	/**
	 * @return Returns transitive sub roles.
	 */
	public Set getTransitiveSubRoles() {
		return transitiveSubRoles;
	}
	
	/**
	 * @param functionalSuper The functionalSuper to set.
	 */
	public void addTransitiveSubRole( Role r ) {
	    if( transitiveSubRoles.isEmpty() ) {
	        transitiveSubRoles = SetUtils.singleton( r );
	    }
	    else if( transitiveSubRoles.size() == 1 ) {
            Role tsr = (Role) transitiveSubRoles.iterator().next();
	        if( tsr.isSubRoleOf( r ) ) {
	            transitiveSubRoles = SetUtils.singleton( r );
	        }
	        else if( !r.isSubRoleOf( tsr ) ) {
	            transitiveSubRoles = new HashSet( 2 );
	            transitiveSubRoles.add( tsr );
	            transitiveSubRoles.add( r );
	        }	            
        }
        else {
	        for(Iterator i = transitiveSubRoles.iterator(); i.hasNext();) {
                Role tsr = (Role) i.next();
		        if( tsr.isSubRoleOf( r ) ) {
		            transitiveSubRoles.remove( tsr );
		            transitiveSubRoles.add( r );
		            return;
		        }
		        else if( r.isSubRoleOf( tsr ) ) {
		            return;
		        }
            }
	        transitiveSubRoles.add( r );
        }	    
	}

	/**
	 * @return Returns the foreignOntology.
	 */
	public String getForeignOntology() {
		return foreignOntology;
	}
	
	/**
	 * @param foreignOntology The foreignOntology to set.
	 */
	public void setForeignOntology(String foreignOntology) {
		this.foreignOntology = foreignOntology;
	}
}
