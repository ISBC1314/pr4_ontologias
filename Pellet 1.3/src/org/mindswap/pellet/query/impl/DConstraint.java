/*
 * Created on Jul 21, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.mindswap.pellet.query.impl;

import org.apache.xerces.impl.dv.XSSimpleType;
import org.mindswap.pellet.datatypes.Datatype;
import org.mindswap.pellet.datatypes.RDFSLiteral;
import org.mindswap.pellet.datatypes.XSDDecimal;
import org.mindswap.pellet.datatypes.XSDAtomicType;
import org.mindswap.pellet.datatypes.XSDString;

import aterm.ATerm;
import aterm.ATermAppl;
/**
 * @author Daniel
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

// TODO only works for decimal and subtypes 
public class DConstraint {
	private ATerm var;
	private String op;
	private Object val;
	private Datatype type; 
	boolean forced;
	
	public DConstraint( ATermAppl var, String op, Object val ) {
		this.var = var;
		this.op = op;
		this.val = val;
		this.forced = false;
		
		if ( this.op.equals( ">" ) ) {
			type = XSDDecimal.instance.deriveByRestriction( XSSimpleType.FACET_MINEXCLUSIVE, val.toString() );
		} else if ( op.equals( "<" ) ) {
			type = XSDDecimal.instance.deriveByRestriction( XSSimpleType.FACET_MAXEXCLUSIVE, val.toString() );
		} else if ( op.equals( ">=" ) ) {
			type = XSDDecimal.instance.deriveByRestriction( XSSimpleType.FACET_MININCLUSIVE, val.toString() );
		} else if ( op.equals( "<=" ) ) {
			type = XSDDecimal.instance.deriveByRestriction( XSSimpleType.FACET_MAXINCLUSIVE, val.toString() );
		} else if ( op.equals( "==" ) ) {
			XSDAtomicType type = XSDDecimal.instance;
			
			type = type.deriveByRestriction( XSSimpleType.FACET_MAXINCLUSIVE, val.toString() );
			type = type.deriveByRestriction( XSSimpleType.FACET_MAXEXCLUSIVE, val.toString() );
		} else if ( op.equalsIgnoreCase( "langEq" ) ) {
			type = XSDString.instance.deriveByRestriction( "xml:lang", val.toString().replaceAll("\"", "") );
			forced = true;
		}
		else {
		    System.err.println("WARNING: Cannot handle the RDQL constraint ?" + var + " " + op + " " + val);
		    type = RDFSLiteral.instance;
		}
	}
	
	public boolean isForced() {
	    return forced;
	}
	
	public Datatype getDerivedDatatype() {
		return type;
	}
	
	/**
	 * @return
	 */
	public String getOp() {
		return op;
	}

	/**
	 * @return
	 */
	public Object getVal() {
		return val;
	}

	/**
	 * @return
	 */
	public ATerm getVar() {
		return var;
	}

	/**
	 * @param string
	 */
	public void setOp(String string) {
		op = string;
	}

	/**
	 * @param object
	 */
	public void setVal(Object object) {
		val = object;
	}

	/**
	 * @param term
	 */
	public void setVar(ATerm term) {
		var = term;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return var.toString() + " " + op + " " + val.toString();
	}

}
