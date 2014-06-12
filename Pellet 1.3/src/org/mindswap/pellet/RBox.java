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


import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.mindswap.pellet.taxonomy.Taxonomy;
import org.mindswap.pellet.utils.ATermUtils;

import aterm.ATerm;
import aterm.ATermAppl;

public class RBox {
	private Map roles = new HashMap();
	private Set functionalRoles = new HashSet();	
	
	private Taxonomy taxonomy;
	
	boolean consistent = true;  

	public RBox() {
	}

	/**
	 * Return the role with the given name
	 * 
	 * @param r Name (URI) of the role
	 * @return
	 */
	public Role getRole(ATerm r) {
		return (Role) roles.get(r);
	}
	
	/**
	 * Return the role with the given name and throw and exception
	 * if it is not found.
	 * 
	 * @param r Name (URI) of the role
	 * @return
	 */
	public Role getDefinedRole(ATerm r) {
		Role role = (Role) roles.get(r);
		
		if(role == null)
			throw new RuntimeException(r + " is not defined as a property");
		
		return role;
	}

	public boolean isConsistent() {
		return consistent;
	}

	public Role addRole(ATermAppl r) {
		Role role = getRole(r);
		
		if(role == null) {
			role = new Role(r, Role.UNTYPED);
			roles.put(r, role);				
		}
		
		return role;
	}
	
	public Role addObjectRole(ATermAppl r) {
		Role role = getRole(r);
		
		if(role == null) {
			role = new Role(r, Role.OBJECT);
			roles.put(r, role);			

			ATermAppl invR = ATermUtils.makeInv(r);
			Role invRole = new Role(invR, Role.OBJECT);
			roles.put(invR, invRole);			

			role.setInverse(invRole);
			invRole.setInverse(role);
		}
		else if(role.getType() == Role.UNTYPED) {
			role.setType(Role.OBJECT);

			ATermAppl invR = ATermUtils.makeInv(r);
			Role invRole = new Role(invR, Role.OBJECT);
			roles.put(invR, invRole);						

			role.setInverse(invRole);
			invRole.setInverse(role);
		}
		else if(role.getType() != Role.OBJECT) {
		    //System.err.println(r + " is defined both as an ObjectProperty and a " + role.getTypeName() + "Property");
			consistent = false;     	
		}
		
		return role;
	}

	public Role addDatatypeRole(ATermAppl r) {
		Role role = getRole(r);
		
		if(role == null) {
			role = new Role(r, Role.DATATYPE);
			roles.put(r, role);			
		}
		else if(role.getType() == Role.UNTYPED) {
			role.setType(Role.DATATYPE);
		}
		else if(role.getType() != Role.DATATYPE) {
			//System.err.println(r + " is defined both as a DatatypeProperty and a " + role.getTypeName() + "Property");
			consistent = false;     	
		}
		
		return role;
	}
	
	//Added for Econnections	
	public Role addLinkRole(ATermAppl r) {
		Role role = getRole(r);
		
		if(role == null) {
			role = new Role(r, Role.LINK);
			roles.put(r, role);			
		}
		else if(role.getType() == Role.UNTYPED) {
			role.setType(Role.LINK);
		}
		else if(role.getType() != Role.LINK) {
			System.err.println(r + " is defined both as a LinkProperty and a " + role.getTypeName() + "Property");
			consistent = false;     	
		}
		
		return role;
	}	
	
	public Role addAnnotationRole(ATermAppl r) {
		Role role = getRole(r);
		
		if(role == null) {
			role = new Role(r, Role.ANNOTATION);
			roles.put(r, role);			
		}
		else if(role.getType() != Role.UNTYPED 
			 && role.getType() != Role.ANNOTATION) {
			System.err.println(r + "is defined both as an AnnotationProperty and a " + role.getTypeName() + "Property");
			 	
			consistent = false;     	
		}
		
		return role;
	}
	
	public Role addOntologyRole(ATermAppl r) {
		Role role = getRole(r);
		
		if(role == null) {
			role = new Role(r, Role.ONTOLOGY);
			roles.put(r, role);			
		}
		else if(role.getType() != Role.UNTYPED 
			 && role.getType() != Role.ONTOLOGY) {
			System.err.println(r + "is defined both as an OntologyProperty and a " + role.getTypeName() + "Property");
			consistent = false;     	
		}
		
		return role;
	}
	
	
//	public Role addTransitiveRole(ATerm r) {
//		Role role = getRole(r);
//		
//		if(role != null && role.getType() == Role.OBJECT) 
//			role.setTransitive(true);					
//		else {
//			System.err.println(r + "is defined both as a TransitiveProperty and a " + role.getTypeName() + "Property");
//			consistent = false;  			
//		}
//   			
//		return role;
//	}
//
//	public Role addFunctionalRole(ATerm r) {
//		Role role = getRole(r);
//		
//		if(role != null) 
//			role.setFunctional(true);					
//		else {
//			System.err.println(r + "is not defined as a property");
//			consistent = false;     						
//		}
//		
//		return role;
//	}	
//
//	public Role addInverseFunctionalRole(ATerm r) {
//		Role role = getRole(r);
//		
//		if(role != null && role.getType() == Role.OBJECT) {		
//			addFunctionalRole(role.getInverse().getName());
//		}
//		else {		
//			System.err.println(role.getTypeName() + "Property" + r + "is not defined as a property");
//			consistent = false;
//		}     	
//		
//		return role;
//	}	
//	
//	public void addDomainRestriction(ATerm r, ATermAppl c) {
//		Role role = getRole(r);
//		
//		if(role != null) 
//			role.addDomain(c);					
//		else {		
//			System.err.println(r + "is not defined as a property");
//			consistent = false;
//		}     			
//	}	
//	
//	public void addRangeRestriction(ATerm r, ATermAppl c) {
//		Role role = getRole(r);
//		
//		if(role != null) 
//			role.addRange(c);					
//		else {		
//			System.err.println(r + "is not defined as a property");
//			consistent = false;
//		}     			
//	}	
		
	public void addSubRole(ATerm s, ATerm r) {
		Role roleS = getRole(s);
		Role roleR = getRole(r);
		
		if(roleS == null || roleR == null) 
			consistent = false;     			
		else {
			roleR.addSubRole(roleS);
			roleS.addSuperRole(roleR);
		}
	}
	
	public void addSameRole(ATerm s, ATerm r) {
		Role roleS = getRole(s);
		Role roleR = getRole(r);
		
		if(roleS == null || roleR == null) 
			consistent = false;     			
		else {
			roleR.addSubRole(roleS);
			roleR.addSuperRole(roleS);
			roleS.addSubRole(roleR);
			roleS.addSuperRole(roleR);
		}
	}
	
	public void addInverseRole(ATerm s, ATerm r) {
		Role roleS = getRole(s);
		Role roleR = getRole(r);
		
		if(roleS == null || roleR == null) 
			consistent = false;     			
		else {
            boolean prevInvRisFunctional = roleR.getInverse().isFunctional();
            boolean prevInvSisFunctional = roleS.getInverse().isFunctional();
			ATermAppl prevInvR = roleR.getInverse().getName();
			ATermAppl prevInvS = roleS.getInverse().getName();
			
			//inverse relation already defined
			if(prevInvR.equals(s) && prevInvS.equals(r))
				return;
				
			// this means r already has another inverse defined 
			if(prevInvR.getArity() == 0) {
				// this means s already has another inverse defined
				if(prevInvS.getArity() == 0) {
					// prevInvR = S and prenInvS = R
					addSameRole(prevInvR, s);
					addSameRole(prevInvS, r);
				}
				else {
					// Set prevInvR = S. we can get rid of prevInvS 
					// because it is not a named property
					addSameRole(prevInvR, s);
                    if( prevInvSisFunctional )
						roleR.setFunctional(true);
					roles.remove(prevInvS);
				}
			}
			else if(prevInvS.getArity() == 0) {
				// Set prevInvS = R. we can get rid of prevInvR 
				// because it is not a named property
				addSameRole(prevInvS, r);
                roleR.setInverse(roleS);
				if( prevInvRisFunctional )
					roleS.setFunctional(true);
				roles.remove(prevInvR);			
            }
			else {
				roleR.setInverse(roleS);
				roleS.setInverse(roleR);
				if( prevInvRisFunctional )
                    roleS.setFunctional( true );
                if( prevInvSisFunctional )
                    roleR.setFunctional( true );
				roles.remove(prevInvR);
				roles.remove(prevInvS);
			}			
		}
	}
				
	/**
	 * check if the term is declared as a role
	 */
	public boolean isRole( ATerm r ) {
	    return roles.containsKey( r );
	}
	
	public void computeRoleHierarchy() {
        // first pass - compute sub roles
        Iterator i = roles.values().iterator();
        while( i.hasNext() ) {
            Role role = (Role) i.next();

            if( role.getType() == Role.OBJECT || role.getType() == Role.DATATYPE ) {
                Set subRoles = new HashSet();
                computeSubRoles( role, subRoles );
                role.setSubRoles( subRoles );
            }
        }

        // second pass - set super roles and propogate domain & range
        i = roles.values().iterator();
        while( i.hasNext() ) {
            Role role = (Role) i.next();

            Role invR = role.getInverse();
            if( invR != null ) {
                // domain of inv role is the range of this role
                Set domains = invR.getDomains();
                if( domains != null ) role.addRanges( domains );
                Set ranges = invR.getRanges();
                if( ranges != null ) role.addDomains( ranges );

                if( invR.isTransitive() )
                    role.setTransitive( true );
                else if( role.isTransitive() ) 
                    invR.setTransitive( true );
                
                if( invR.isFunctional() )
                    role.setInverseFunctional( true );
                if( role.isFunctional() ) 
                    invR.setInverseFunctional( true );
                if( invR.isInverseFunctional() )
                    role.setFunctional( true );
                if( role.isInverseFunctional() ) 
                    invR.setFunctional( true );
            }

            Set domains = role.getDomains();
            Set ranges = role.getRanges();
            Iterator subs = role.getSubRoles().iterator();
            while( subs.hasNext() ) {
                Role s = (Role) subs.next();
                s.addSuperRole( role );

                if( domains != null ) s.addDomains( domains );
                if( ranges != null ) s.addRanges( ranges );
            }
        }
        
        // third pass - set transitivity and functionality
        i = roles.values().iterator();
        while( i.hasNext() ) {
            Role r = (Role) i.next();

            r.normalize();
                
            boolean isTransitive = r.isTransitive();
            Iterator subs = r.getSubRoles().iterator();
            while( subs.hasNext() ) {
                Role s = (Role) subs.next();
                if( s.isTransitive() ) {
                    if( r.isSubRoleOf( s ) ) 
                        isTransitive = true;
                    r.addTransitiveSubRole( s );
                }
            }
            r.setTransitive( isTransitive );

            Iterator supers = r.getSuperRoles().iterator();
            while( supers.hasNext() ) {
                Role s = (Role) supers.next();
                if( s.isFunctional() ) {
                    r.setFunctional( true );
                    r.addFunctionalSuper( s );
                }
            }

            if( r.isFunctional() && r.getFunctionalSupers().contains( r ) )
                functionalRoles.add( r );
        }
        
        // we will compute the taxonomy when we need it
        taxonomy = null;
    }

	private Set computeImmediateSubRoles(Role r) {
		Set subs = null;

		Role invR = r.getInverse();
		if(invR == null) 
		    subs = r.getSubRoles();		
		else {
		    subs = new HashSet(r.getSubRoles());
		 	Iterator i = invR.getSubRoles().iterator();
		 	while(i.hasNext()) {
		 		Role invSubR =  (Role) i.next();
		 		Role subR = invSubR.getInverse();
		 		if(subR == null) {
		 			System.err.println("Property " + invSubR + " was supposed to be an ObjectProperty but it is not!");
		 		}
		 		else
		 			subs.add(subR);
		 	}
		}
		
		return subs;
	}

	private void computeSubRoles(Role r, Set set) {
		// check for loops
		if(set.contains(r))
			return;

		// reflexive	
		set.add(r);
		
		// transitive closure		
		Iterator i = computeImmediateSubRoles(r).iterator(); 
		while(i.hasNext()) {
			Role s = (Role) i.next();
			computeSubRoles(s, set);
		}
	}			

//	private boolean isTransitiveRole(Role r) {
//		if(!r.isObjectRole())
//			return false;
//		if(r.getInverse() == null)
//		    return false;
//		return isTransitiveRole(r, new HashSet());	
//	}
//	
//	/**
//	 * helper functions to check if it can be deduced that a role 
//	 * is transitive
//	 */
//	private boolean isTransitiveRole(Role r, HashSet visited) {
//		if(visited.contains(r))
//			return false;
//		else					
//			visited.add(r);
//		
//		return r.isTransitive() ||
//			hasTransitiveSubRole(r, visited) ||
//			hasTransitiveInverseRole(r, visited);
//	}
//
//	
//	/**
//	 * check if a role r is subrole of s which is transitive
//	 * 
//	 * FIXME this is not a good idea, we should have a separate getTransitiveSubRoles function
//	 */
//	private boolean hasTransitiveSubRole(Role r, HashSet visited) {
//		Iterator sr = r.getSubRoles().iterator();
//		while(sr.hasNext()) {
//			Role s = (Role) sr.next();	
//			
//			if(isTransitiveRole(s, visited))
//				return true;
//		}
//		
//		return false;
//	}
//	
//	/**
//	 * check if r is an inverse role of s which is transitive
//	 */
//	private boolean hasTransitiveInverseRole(Role r, HashSet visited) {
//		Role inv = r.getInverse();
//			
//		// getInverseRoles will return inv(r) in the list. since this
//		// is not really a defined property, there is no meaning to
//		// check this one. so only look for primitive terms 
//		return ATermUtils.isPrimitive(inv.getName()) &&
//			isTransitiveRole(inv, visited);
//	}	

	/**
	 * Returns a string representation of the RBox where for each role subroles,
	 * superroles, and isTransitive information is given
	 */	
	public String toString() {
		return "[RBox " + roles.values() + "]";
	}
	
	/**
	 * for each role in the list finds an inverse role and 
	 * returns the new list. 
	 */
	public Set inverseRoleList(Set roles) {
		Set set = new HashSet();
	 	
	 	Iterator i = roles.iterator();
	 	while(i.hasNext()) {
	 		Role r =  (Role) i.next();
	 		Role invR = r.getInverse();
	 		if(invR == null) {
	 			System.err.println("Property " + r + " was supposed to be an ObjectProperty but it is not!");
	 		}
	 		else
	 			set.add(invR);
	 	}
		
		return set;
   }	
	/**
	 * @return Returns the roles.
	 */
	public Set getRoleNames() {
		return roles.keySet();
	}

	/**
	 * @return Returns the functionalRoles.
	 */
	public Set getFunctionalRoles() {
		return functionalRoles;
	}

    /**
     * getRoles
     * 
     * @return
     */
    public Collection getRoles() {
        return roles.values();
    }
    
    
    public Taxonomy getTaxonomy() {
        if( taxonomy == null ) {
	        RoleTaxonomyBuilder builder = new RoleTaxonomyBuilder( this );
	        taxonomy = builder.classify();
        }
        return taxonomy;
    }
}
