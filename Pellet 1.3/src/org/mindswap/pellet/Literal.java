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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.mindswap.pellet.datatypes.AtomicDatatype;
import org.mindswap.pellet.datatypes.Datatype;
import org.mindswap.pellet.datatypes.DatatypeReasoner;
import org.mindswap.pellet.datatypes.RDFSLiteral;
import org.mindswap.pellet.datatypes.StringValue;
import org.mindswap.pellet.datatypes.UnionDatatype;
import org.mindswap.pellet.utils.ATermUtils;

import aterm.ATerm;
import aterm.ATermAppl;

/*
 * Created on Aug 27, 2003
 *
 */

/**
 * @author Evren Sirin
 *  
 */
public class Literal extends Node {
    private ATermAppl atermValue;
    
    private Object value;

    private Datatype datatype;
    
    private boolean hasValue;

//    private Edge inEdge;
//    private EdgeList inEdges;
    
    public Literal(ATermAppl name, ATermAppl term, ABox abox) {
        super(name, abox);

        if(term != null) {
            hasValue = !term.getArgument(ATermUtils.LIT_URI_INDEX).equals(ATermUtils.NO_DATATYPE);
            if( hasValue )
                value = abox.dtReasoner.getValue( term );
            
            atermValue = ATermUtils.makeValue( term );
        }
        else
            hasValue = false;
    }

    public Literal(Literal literal, ABox abox) {
        super(literal, abox);

        atermValue = literal.atermValue;
        value = literal.value;
        hasValue = literal.hasValue;
//        inEdge = literal.inEdge;
//        inEdges = literal.inEdges;
    }

    public Node copyTo(ABox abox) {
        return new Literal(this, abox);
    }

//    protected void updateNodeReferences() {
//        super.updateNodeReferences();
//
//        Individual ind = inEdge.getFrom();
//        ATermAppl name = ind.getName();
//        Individual from = abox.getIndividual(name);
//        inEdge = new Edge(inEdge.getRole(), from, this, inEdge.getDepends());
//        inEdges = new EdgeList( inEdge );
//        from.addOutEdge(inEdge);
//    }

    final public boolean isLeaf() {
        return true;
    }
    
    public int getNominalLevel() {
        return isNominal() ? NOMINAL : BLOCKABLE;
    }
    
	public boolean isNominal() {
	    return (value != null);
	}
	
	public boolean isBlockable() {
	    return (value == null);
	}
	
	public boolean isLiteral() {
	    return true;
	}
	
	public boolean isIndividual() {
	    return false;
	}
    
    public boolean isDifferent(Node node) {
        if( super.isDifferent( node ) )
            return true;
        
        Literal literal = (Literal) node;        
        if(value != null && literal.value != null) 
            return !value.equals(literal.value);
        
        return false;
    }
    
    public boolean hasType(ATerm type) {
        return super.hasType(type) || 
        	(atermValue != null && atermValue.equals(type));
    }

	public DependencySet getDifferenceDependency(Node node) {	  
	    DependencySet ds = null; 
	    if(isDifferent(node)) {
	        ds = (DependencySet) differents.get(node);
	        if(ds == null)
	            ds = DependencySet.INDEPENDENT;
	    }
	    
	    return ds;
	}

    public void addType(ATermAppl c, DependencySet d) {
        if( hasType( c ) )
            return;	

        super.addType(c, d);

        // TODO when two literals are being merged this is not efficient
        //if(abox.isInitialized())
        checkClash();
    }

    public void addAllTypes( Map types, DependencySet ds ) {
        Iterator i = types.keySet().iterator();
        while( i.hasNext() ) {
            ATermAppl c = (ATermAppl) i.next();
            
            if( hasType(c) )
                continue;	
            
            DependencySet depends = (DependencySet) types.get( c );

            super.addType( c, depends.union( ds ) );
        }
        
        checkClash();
    }
    
//    public void addInEdge(Edge edge) {
////        if(inEdge != null)
////            throw new InternalReasonerException(
////                "Trying to add multiple edges to a literal. Edge: " +  inEdge + " Edge: " + edge );
//
//        if(!edge.getTo().equals(this))
//            throw new InternalReasonerException(
//                "Trying to add invalid edge to a literal. Literal: " +  this + " Edge: " + edge );
//        
//        if(edge == null)
//            throw new InternalReasonerException( "Adding a null edge to literal "  + this );
//
//        inEdge = edge;
//        inEdges = new EdgeList( edge );
//    }
//
//    public boolean removeInEdge(Edge edge) {
//        if(inEdge == null || !inEdge.equals(edge))
//            throw new InternalReasonerException(
//                "Trying to remove a non-existing edge from a literal. " +
//                "Literal:" + this + " Edge: " + edge);
//
//        inEdge = null;
//        
//        return true;
//    }
//
//    public void removeInEdges() {
//        inEdge = null;
//        inEdges = null;
//    }
//
//    public EdgeList getInEdges() {
//        return inEdges;
//    }
//
//    public Edge getInEdge() {
//        return inEdge;
//    }
//    
//	public Individual getParent() {
//		if( inEdge == null )
//			return null;
//		
//		return inEdge.getFrom();
//	}
	
	public boolean hasSuccessor( Node x ) {
	    return false;
	}

    public Datatype getDatatype() {
        return datatype;
    }

    public ATermAppl getTerm() {
        return hasValue ? (ATermAppl) atermValue.getArgument(0) : null;
    }
    
    public String getDatatypeURI() {
        if( hasValue ) {
            ATermAppl literal = getTerm();
            String datatypeURI = ((ATermAppl) literal.getArgument(2)).getName();
            if( datatypeURI.equals("") )
                return null;
            else
                return datatypeURI;
        }
        
        if( datatype == null ) 
            return null;
        
        if( datatype instanceof UnionDatatype )
            return null;
        
        if(datatype.getURI() != null)
            return datatype.getURI();
        
        AtomicDatatype primitive = ((AtomicDatatype) datatype).getPrimitiveType();
        return primitive.getURI();
    }
    
    public String getLang() {
        if(value != null && value instanceof StringValue)
            return ((StringValue) value).getLang();
        
        return "";
    }
    
    public String getLexicalValue() {
        if( hasValue )
            return value.toString();
        
        return null;
    }
    
    private void checkClash() {
        
        if( hasValue && value == null ) {
            abox.setClash(Clash.invalidLiteral(this, DependencySet.INDEPENDENT, getTerm()));
            return;
        }
        
        if(hasType(ATermUtils.BOTTOM)) {
            abox.setClash(new Clash(this, Clash.EMPTY_DATATYPE, getDepends(ATermUtils.BOTTOM)));
            if(abox.doExplanation())
                System.out.println("1) Literal clash dependency = " + abox.getClash());
        }

        Set types = getTypes();
        if(types.size() == 1) {
            datatype = RDFSLiteral.instance;
            return;
        }

        DatatypeReasoner dtReasoner = abox.getDatatypeReasoner();

        ArrayList primitives = new ArrayList();
        for(Iterator i = types.iterator(); i.hasNext();) {
            ATermAppl type = (ATermAppl) i.next();

//		    if(abox.getKB().isClass(type)) {
//				String exp = null;
//				if(abox.doExplanation())
//					exp = "Literal belongs to class: " + type.getName();
//		        abox.setClash(new Clash(this, Clash.LITERAL_CLASS, getDepends(type), exp));
//		        return;
//		    }

            if(type.equals(RDFSLiteral.instance.getName()) || ATermUtils.isAnd(type))
                continue;
            
            primitives.add( type );
        }
        
        if(primitives.isEmpty()) {
            datatype = RDFSLiteral.instance;
            return;
        }
        
        ATermAppl dt[] = (ATermAppl[]) primitives.toArray(new ATermAppl[primitives.size() - 1]);
        
        datatype = dtReasoner.intersection(dt);
        AtomicDatatype litDatatype = (AtomicDatatype) dtReasoner.getDatatype(this.getDatatypeURI());

        if(datatype.isEmpty()) {
            DependencySet ds = DependencySet.EMPTY;
            for(int i = 0; i < dt.length; i++)
                ds = ds.union(getDepends(dt[i]));

            String exp = null;
            if(abox.doExplanation()) exp = "Types: " + getTypes().toString();

            abox.setClash(new Clash(this, Clash.EMPTY_DATATYPE, ds, exp));
        }        
        // need to pass the type of this literal to datatype.contains()
        else if(hasValue && !datatype.contains(value, litDatatype)) {
            DependencySet ds = DependencySet.EMPTY;
            for(int i = 0; i < dt.length; i++)
                ds = ds.union(getDepends(dt[i]));

            // if this is plain literal then it is missing the rdf:datatype
            // attribute
            if(value instanceof StringValue) {
//                StringValue strVal = (StringValue) value;
//                if(PelletOptions.IGNORE_MISSING_DATATYPE_ATTR && strVal.getLang().equals("")) {                    
//                    ATermAppl newTerm = ATermUtils.makeTypedLiteral(strVal.getValue(), dt[0].getName());
//                    Object newValue = abox.dtReasoner.getValue(newTerm);
//                    if(newValue == null)
//                        abox.setClash(Clash.valueDatatype(this, ds, getTerm(), dt[0]));
//                    else if(!datatype.contains(newValue))
//                        abox.setClash(Clash.valueDatatype(this, ds, getTerm(), dt[0]));
//                    else {
//                        System.err.println("WARNING: " + Clash.missingDatatype(this, ds, getTerm(), dt[0]).detailedString());
//                        this.atermValue = ATermUtils.makeValue(newTerm);
//                        this.value = newValue;
//                    }
//                }
//                else                   
                    abox.setClash(Clash.missingDatatype(this, ds, getTerm(), dt[0]));
            }
            else
                abox.setClash(Clash.valueDatatype(this, ds, getTerm(), dt[0]));
        }        
    }
    
    public Object getValue() {
        return value;
    }

    public String toTypedString() {
        if( value == null )
            return name.toString();
        
        String str = "\"" + value + "\"";

        String lang = getLang();
        if(!lang.equals(""))
            return str + "@" + lang;

        String datatypeURI = getDatatypeURI();
        if(datatypeURI != null)
            return str + "^^" + datatypeURI;

        return str;
    }
    
	
	final public void prune( DependencySet ds ) {
	    pruned = ds;
	}
	
	public void unprune( int branch ) {
	    super.unprune( branch );
        
        checkClash();
    }
//	    pruned  = null;
//	    
//		DependencySet d = inEdge.getDepends();
//		
//		if(d.branch <= branch) {
//			Individual pred = inEdge.getFrom();
//			Role role = inEdge.getRole();
//			
//			if(!pred.hasEdge(role, this)) {
//				pred.addOutEdge( inEdge );
//				
//				if(DEBUG) System.out.println("RESTORE: " + this + " ADD reverse edge " + inEdge);
//			}
//		}
//		else
//		    throw new InternalReasonerException( "Trying to unprune a literal without any incoming edge" );
//	}
//    
//    public boolean restore( int branch ) {
//        boolean restored = super.restore( branch );
//        
//        if( !restored ) {
//            return false;
//        }       
//     
//        DependencySet d = inEdge.getDepends();        
//        if( d.branch > branch ) {           
//            if(DEBUG) System.out.println("RESTORE: " + name + " delete reverse edge " + inEdge);
//            removeInEdges();
//        }  
//        
//        return true;
//    }
    
    public String toString() {
        return toTypedString();
    }

    public String debugString() {
        return name + " = " + getTypes().toString();
    }

}