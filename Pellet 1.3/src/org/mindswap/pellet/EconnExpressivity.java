/*
 * Created on Aug 30, 2004
 */
package org.mindswap.pellet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.output.ATermBaseVisitor;
import org.mindswap.pellet.output.ATermVisitor;
import org.mindswap.pellet.tbox.TBox;

import aterm.ATermAppl;
import aterm.ATermInt;
import aterm.ATermList;

/**
 * @author Bernardo Cuenca
 */
public class EconnExpressivity {
    protected static Log log = LogFactory.getLog( EconnExpressivity.class );
    
    EconnectedKB kb;

    Map hasInverse;

    Map hasCardinality;

    Map hasFunctionality;

    Map hasTransitivity;

    Map hasRoleHierarchy;

    Map hasDatatype;

    Map hasNominal;

    Map hasNegation;

    Map hasUnion;

    Map hasInverseLink;

    Map hasCardinalityOnLink;

    Map hasLinkHierarchy;

    Map hasLinkFunctionality;

    String currOnt;

    public Set allNominals;

    Visitor visitor;

    class Visitor extends ATermBaseVisitor implements ATermVisitor {
        public void visitTerm( ATermAppl term ) {
        }

        public void visitAnd( ATermAppl term ) {
            visitList( (ATermList) term.getArgument( 0 ) );
        }

        public void visitOr( ATermAppl term ) {
            hasNegation.put( currOnt, Boolean.TRUE );
            visitList( (ATermList) term.getArgument( 0 ) );
        }

        public void visitNot( ATermAppl term ) {
            hasNegation.put( currOnt, Boolean.TRUE );
            visit( (ATermAppl) term.getArgument( 0 ) );
        }

        public void visitSome( ATermAppl term ) {
            ATermAppl aux = (ATermAppl) term.getArgument( 0 );
            if( kb.isLinkProperty( aux ) ) {
                Role r = kb.getRole( aux );
                currOnt = r.getForeignOntology();
            }
            visit( (ATermAppl) term.getArgument( 1 ) );
        }

        public void visitAll( ATermAppl term ) {
            ATermAppl aux = (ATermAppl) term.getArgument( 0 );
            if( kb.isLinkProperty( aux ) ) {
                Role r = kb.getRole( aux );
                currOnt = r.getForeignOntology();
            }
            visit( (ATermAppl) term.getArgument( 1 ) );
        }

        public void visitMin( ATermAppl term ) {
            int cardinality = ((ATermInt) term.getArgument( 1 )).getInt();
            ATermAppl aux = (ATermAppl) term.getArgument( 0 );

            if( cardinality > 2 ) {
                if( kb.isLinkProperty( aux ) )
                    hasCardinalityOnLink.put( currOnt, Boolean.TRUE );
                else
                    hasCardinality.put( currOnt, Boolean.TRUE );
            }
            else if( cardinality > 0 ) {
                if( kb.isLinkProperty( aux ) )
                    hasLinkFunctionality.put( currOnt, Boolean.TRUE );
                else
                    hasFunctionality.put( currOnt, Boolean.TRUE );
            }

        }

        public void visitMax( ATermAppl term ) {
            int cardinality = ((ATermInt) term.getArgument( 1 )).getInt();
            ATermAppl aux = (ATermAppl) term.getArgument( 0 );

            if( cardinality > 1 ) {
                if( kb.isLinkProperty( aux ) )
                    hasCardinalityOnLink.put( currOnt, Boolean.TRUE );
                else
                    hasCardinality.put( currOnt, Boolean.TRUE );
            }
            else if( cardinality > 0 ) {
                if( kb.isLinkProperty( aux ) )
                    hasLinkFunctionality.put( currOnt, Boolean.TRUE );
                else
                    hasFunctionality.put( currOnt, Boolean.TRUE );
            }

        }

        public void visitHasValue( ATermAppl term ) {
            ATermAppl aux = (ATermAppl) term.getArgument( 0 );
            if( kb.isLinkProperty( aux ) ) {
                Role r = kb.getRole( aux );
                currOnt = r.getForeignOntology();
            }
            hasNominal.put( currOnt, Boolean.TRUE );
        }

        public void visitValue( ATermAppl term ) {
            //     nominals.add(term);
        }

        public void visitOneOf( ATermAppl term ) {
            hasNegation.put( currOnt, Boolean.TRUE );
            visitList( (ATermList) term.getArgument( 0 ) );
        }

        public void visitLiteral( ATermAppl term ) {
        }

        public void visitSubClass( ATermAppl term ) {
            // TODO Auto-generated method stub
        }
    }

    EconnExpressivity(EconnectedKB kb) {
        this.kb = kb;
        hasInverse = new HashMap();
        hasCardinality = new HashMap();
        hasFunctionality = new HashMap();
        hasTransitivity = new HashMap();
        hasRoleHierarchy = new HashMap();
        hasDatatype = new HashMap();
        hasNominal = new HashMap();
        hasNegation = new HashMap();
        hasUnion = new HashMap();

        hasInverseLink = new HashMap();
        hasCardinalityOnLink = new HashMap();
        hasLinkHierarchy = new HashMap();
        hasLinkFunctionality = new HashMap();

        allNominals = new HashSet();
        currOnt = null;

    }

    public void init() {
        Iterator i = kb.getTBoxes().keySet().iterator();
        while( i.hasNext() ) {
            String ontology = (String) i.next();
            //hasNegation = false;
            hasInverse.put( ontology, Boolean.FALSE );
            hasDatatype.put( ontology, Boolean.FALSE );
            hasCardinality.put( ontology, Boolean.FALSE );
            hasFunctionality.put( ontology, Boolean.FALSE );
            hasTransitivity.put( ontology, Boolean.FALSE );
            hasRoleHierarchy.put( ontology, Boolean.FALSE );
            hasNegation.put( ontology, Boolean.FALSE );
            hasUnion.put( ontology, Boolean.FALSE );

            hasInverseLink.put( ontology, Boolean.FALSE );
            hasCardinalityOnLink.put( ontology, Boolean.FALSE );
            hasLinkHierarchy.put( ontology, Boolean.FALSE );
            hasLinkFunctionality.put( ontology, Boolean.FALSE );

        }
        visitor = new Visitor();
    }

    public void compute() {

        init();
        //processIndividuals();
        processClasses();

        //processIndividuals();
        //processClasses();
        processRoles();
    }

    protected void processRoles() {
        Iterator iter = kb.getRBoxes().keySet().iterator();
        while( iter.hasNext() ) {

            currOnt = (String) iter.next();
            kb.setOntology( currOnt );
            RBox t = kb.getRBox();

            //RBox t = (RBox)kb.getRBoxes().get(currOnt);
            for(Iterator i = t.getRoles().iterator(); i.hasNext();) {
                Role r = (Role) i.next();
                if( r.isDatatypeRole() ) hasDatatype.put( currOnt, Boolean.TRUE );
                if( !r.isAnon() && r.hasNamedInverse() ) {
                    if( r.isObjectRole() ) hasInverse.put( currOnt, Boolean.TRUE );
                    if( r.isLinkRole() ) hasInverseLink.put( currOnt, Boolean.TRUE );
                }

                if( r.isAnon() && r.isFunctional() ) {
                    if( r.isObjectRole() ) hasInverse.put( currOnt, Boolean.TRUE );
                    if( r.isLinkRole() ) hasInverseLink.put( currOnt, Boolean.TRUE );
                }

                if( r.isFunctional() ) {
                    if( r.isObjectRole() ) hasFunctionality.put( currOnt, Boolean.TRUE );
                    if( r.isLinkRole() ) hasLinkFunctionality.put( currOnt, Boolean.TRUE );
                }
                if( r.isTransitive() ) hasTransitivity.put( currOnt, Boolean.TRUE );
                if( r.getSubRoles().size() > 1 ) {
                    if( r.isObjectRole() ) hasRoleHierarchy.put( currOnt, Boolean.TRUE );
                    if( r.isLinkRole() ) hasLinkHierarchy.put( currOnt, Boolean.TRUE );
                }

                //  ATermAppl domain = r.getDomain();
                //  if(domain != null)
                //    visitor.visit(domain);

                //     ATermAppl range = r.getRange();
                //   if(range != null)
                //      visitor.visit(range);
            }
        }
    }

    protected void processClasses() {

        Iterator iter = kb.getTBoxes().keySet().iterator();
        while( iter.hasNext() ) {
            currOnt = (String) iter.next();
            kb.setOntology( currOnt );
            //TBox t = (TBox)kb.getTBoxes().get(currOnt);
            TBox t = kb.getTBox();
            
            ATermList UC = t.getUC();
            if( UC != null ) {
                hasNegation.put( currOnt, Boolean.TRUE );
                for(ATermList list = UC; !list.isEmpty(); list = list.getNext())
                    visitor.visit( (ATermAppl) list.getFirst() );
            }

//            TuBox Tu = t.Tu;
//            Iterator i = Tu.normalizedMap.values().iterator();
//            while( i.hasNext()
//                && ((hasNegation.get( currOnt ).equals( Boolean.FALSE ) || hasCardinality.get(
//                    currOnt ).equals( Boolean.FALSE ))) ) {
//                ATermAppl term = (ATermAppl) i.next();
//                visitor.visit( term );
//            }
        }
    }

    public String toString() {
        Map dl = new HashMap();
        String result = "";
        if( kb.getTBoxes().size() > 1 ) result = "C(";
        String link = "";
        boolean inverselink = false;
        boolean linkhierarchy = false;
        boolean linkcardinality = false;
        boolean linkfunctionality = false;
        Iterator i = kb.getTBoxes().keySet().iterator();
        while( i.hasNext() ) {
            String ontology = (String) i.next();
            String s;
            if( (hasNegation.get( ontology ) == Boolean.TRUE)
                || (hasUnion.get( ontology ) == Boolean.TRUE) ) {
                s = "ALC";
            }
            else {
                s = "AL";
            }
            dl.put( ontology, s );
            String aux;

            if( hasTransitivity.get( ontology ) == Boolean.TRUE ) {
                aux = (String) dl.get( ontology );
                aux = aux += "R+";
                dl.put( ontology, aux );

            }

            if( hasRoleHierarchy.get( ontology ) == Boolean.TRUE ) {
                aux = (String) dl.get( ontology );
                aux = aux += "H";
                dl.put( ontology, aux );
            }

            if( hasNominal.get( ontology ) == Boolean.TRUE ) {
                aux = (String) dl.get( ontology );
                aux = aux += "O";
                dl.put( ontology, aux );
            }

            if( hasInverse.get( ontology ) == Boolean.TRUE ) {
                aux = (String) dl.get( ontology );
                aux = aux += "I";
                dl.put( ontology, aux );
            }

            if( hasCardinality.get( ontology ) == Boolean.TRUE ) {
                aux = (String) dl.get( ontology );
                aux = aux += "N";
                dl.put( ontology, aux );
            }
            else if( hasFunctionality.get( ontology ) == Boolean.TRUE ) {
                aux = (String) dl.get( ontology );
                aux = aux += "F";
                dl.put( ontology, aux );
            }
            if( hasDatatype.get( ontology ) == Boolean.TRUE ) {
                aux = (String) dl.get( ontology );
                aux = aux += "(D)";
                dl.put( ontology, aux );
            }
            if( ((String) dl.get( ontology )).startsWith( "ALCR+" ) ) {
                aux = (String) dl.get( ontology );
                int index = aux.lastIndexOf( "+" );
                aux = aux.substring( index + 1 );
                String str = "S";
                aux = str.concat( aux );
                dl.put( ontology, aux );
            }

            if( hasInverseLink.get( ontology ) == Boolean.TRUE ) inverselink = true;
            if( hasLinkHierarchy.get( ontology ) == Boolean.TRUE ) linkhierarchy = true;
            if( hasCardinalityOnLink.get( ontology ) == Boolean.TRUE ) linkcardinality = true;
            if( hasLinkFunctionality.get( ontology ) == Boolean.TRUE ) linkfunctionality = true;

            result += dl.get( ontology );
            if( i.hasNext() ) result += ",";
            if(  log.isDebugEnabled() ) 
                log.debug( "Expressivity of the component ontology " + ontology + " is: "
                    + dl.get( ontology ) );
        }
        if( kb.getTBoxes().size() > 1 ) result += ")";
        if( inverselink == true ) link += "I";
        if( linkhierarchy == true ) link += "H";
        if( linkcardinality == true )
            link += "N";
        else {
            if( linkfunctionality == true ) link += "F";
        }
        if(  log.isDebugEnabled() ) 
            log.debug( "Expressivity of the links is: " + link );
        result = result.concat( link );

        return result;
    }

    // private void processIndividuals() {
    // Iterator i = kb.getABox().getIndIterator();
    // while(i.hasNext()){
    //	while(i.hasNext()) {
    //     Individual ind = (Individual) i.next();
    //    Iterator j = ind.getTypes().iterator();
    //   while(j.hasNext()) {
    //      ATermAppl term = (ATermAppl) j.next();
    //    visitor.visit(term);
    // }
    // }
    // }
    // }

    // private void processRoles() {

    //	for(Iterator j = kb.getRBoxes().keySet().iterator(); j.hasNext(); ){  
    //	String ont = (String)j.next();
    //	RBox rbox = (RBox)kb.getRBoxes().get(ont);
    //Iterator it= kb.getNumberRestrictions().iterator();
    // while(it.hasNext()){
    //	ATermAppl aux = (ATermAppl)it.next();
    //	if(rbox.isRole(aux)){
    //	Role raux= rbox.getRole(aux);
    //	if(raux.isObjectRole()|| raux.isDatatypeRole()){ 
    //      	 hasCardinality.put(ont,Boolean.TRUE);}
    //  else{
    //	if(raux.isLinkRole())
    //	hasCardinalityOnLink.put(ont,Boolean.TRUE);
    // }
    //	}
    // }

    //	for(Iterator i = rbox.getRoles().iterator(); i.hasNext(); ) {
    //		Role r = (Role) i.next();
    //		if(r.isDatatypeRole())
    //       hasDatatype.put(ont,Boolean.TRUE);
    // Each ObjectProperty has one anonymous inverse property
    // assigned by default. but this does not mean that ontology
    // uses inverse properties. KB has inverse properties only 
    // if a named property has a named inverse 
    //   if(r.isObjectRole()){ 
    //		if(!r.isAnon() && r.hasNamedInverse())
    //     hasInverse.put(ont,Boolean.TRUE);

    // InverseFunctionalProperty declaration may mean that a named
    // property has an anonymous inverse property which is functional
    // The following condition checks this case
    //		if(r.isAnon() && r.isFunctional())
    //			hasInverse.put(ont,Boolean.TRUE);            
    //		if(r.isFunctional())
    //			hasFunctionality.put(ont,Boolean.TRUE);
    //		if(r.isTransitive())
    //			hasTransitivity.put(ont,Boolean.TRUE);
    // Each property has itself included in the subroles set. We need
    // at least two properties in the set to conclude there is a role
    // hierarchy defined in the ontology
    // 		if(r.getSubRoles().size() > 1)
    ///			hasRoleHierarchy.put(ont,Boolean.TRUE);
    //}
    // if(r.isLinkRole()){ 
    //	if(!r.isAnon() && r.hasNamedInverse())
    //   hasInverseLink.put(ont,Boolean.TRUE);

    // InverseFunctionalProperty declaration may mean that a named
    // property has an anonymous inverse property which is functional
    // The following condition checks this case
    // 		if(r.isAnon() && r.isFunctional())
    //		hasInverseLink.put(ont,Boolean.TRUE);            
    //	if(r.isFunctional())
    //	hasLinkFunctionality.put(ont,Boolean.TRUE);

    // Each property has itself included in the subroles set. We need
    // at least two properties in the set to conclude there is a role
    // hierarchy defined in the ontology
    //		if(r.getSubRoles().size() > 1)
    //		hasLinkHierarchy.put(ont,Boolean.TRUE);
    // }//endif

    // }//end for
    // }//end for
    // }//end processRoles    

    public boolean hasInverses() {
        return hasInverseLink.containsValue( (Boolean.TRUE) );
    }

    public boolean hasLinkNumberRestrictions() {
        return hasCardinalityOnLink.containsValue( (Boolean.TRUE) );
    }

    public boolean hasLinkFunctionalRestrictions() {
        return hasLinkFunctionality.containsValue( (Boolean.TRUE) );
    }

    public Map getNominals() {
        return hasNominal;
    }

    public Map getInverses() {
        return hasInverse;
    }

    public Map getCardinality() {
        return hasCardinality;
    }

    public void setHasNominal( String foreignOnt ) {
        hasNominal.put( foreignOnt, Boolean.TRUE );
    }

    public void setHasTransitivity( String ont ) {
        hasTransitivity.put( ont, Boolean.TRUE );
    }

    /**
     * @return Returns the hasCardinality.
     */
    /**
     * @return
     */
    public boolean hasNominal( String ontology ) {
        if( (hasNominal.get( ontology )) == (Boolean.TRUE) ) {
            return true;
        }
        else
            return false;
    }

    /**
     * @param string
     */
    public void setHasNegation( String ont ) {
        hasNegation.put( ont, Boolean.TRUE );
    }

    /**
     * @param string
     */
    /**
     * @param string
     */
    public void setHasUnion( String ont ) {
        hasUnion.put( ont, Boolean.TRUE );
    }

}
