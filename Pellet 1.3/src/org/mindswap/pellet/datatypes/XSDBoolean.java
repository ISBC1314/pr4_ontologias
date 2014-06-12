/*
 * Created on May 29, 2004
 */
package org.mindswap.pellet.datatypes;

import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Namespaces;

import aterm.ATermAppl;




/**
 * @author Evren Sirin
 */
public class XSDBoolean extends BaseAtomicDatatype implements AtomicDatatype {
	public static final XSDBoolean instance = new XSDBoolean();
	
	protected int NO_VALUES   = 0;
	protected int ONLY_TRUE   = 1;
	protected int ONLY_FALSE  = 2;
	protected int BOTH_VALUES = 3;
	
	protected int status = BOTH_VALUES;
	
	public class XSDDerivedBooleanType extends XSDBoolean {
		protected XSDDerivedBooleanType(int status) {	
			this.status = status;
		}

		public boolean isDerived() {
			return true;
		}
	}

	XSDBoolean() {
		super(ATermUtils.makeTermAppl(Namespaces.XSD + "boolean"));
	}


	public AtomicDatatype not() {
		return new XSDDerivedBooleanType(BOTH_VALUES - status);
	}
	
	/* (non-Javadoc)
	 * @see org.mindswap.pellet.datatypes.AtomicDatatype#intersection(org.mindswap.pellet.datatypes.AtomicDatatype)
	 */
	public AtomicDatatype intersection(AtomicDatatype dt) {
		if(this == dt) return this;

		int result = NO_VALUES;
		if(dt instanceof XSDBoolean) {
			XSDBoolean other = (XSDBoolean) dt;
			result = status & other.status;						
		}
		
		return new XSDDerivedBooleanType(result);
	}

	/* (non-Javadoc)
	 * @see org.mindswap.pellet.datatypes.AtomicDatatype#union(org.mindswap.pellet.datatypes.AtomicDatatype)
	 */
	public AtomicDatatype union(AtomicDatatype dt) {
		if(this == dt) return this;

		int result = NO_VALUES;
		if(dt instanceof XSDBoolean) {
			XSDBoolean other = (XSDBoolean) dt;
			result = status | other.status;						
		}
		
		return new XSDDerivedBooleanType(result);
	}

	/* (non-Javadoc)
	 * @see org.mindswap.pellet.datatypes.AtomicDatatype#difference(org.mindswap.pellet.datatypes.AtomicDatatype)
	 */
	public AtomicDatatype difference(AtomicDatatype dt) {
		return intersection(dt.not());
	}

	/* (non-Javadoc)
	 * @see org.mindswap.pellet.datatypes.Datatype#size()
	 */
	public int size() {
		if(status == NO_VALUES)
			return 0;
		if(status == BOTH_VALUES)
			return 2;
		
		return 1;
	}

	/* (non-Javadoc)
	 * @see org.mindswap.pellet.datatypes.Datatype#contains(java.lang.Object)
	 */
	public boolean contains(Object value) {
		if(value instanceof Boolean) {
			Boolean bool = (Boolean) value;
			return (status == BOTH_VALUES)
				|| (status == ONLY_TRUE && bool.booleanValue())
				|| (status == ONLY_FALSE && !bool.booleanValue());
		}	
		
		return false;	
	}
	
	/* (non-Javadoc)
	 * @see org.mindswap.pellet.datatypes.Datatype#enumeration(java.util.Set)
	 */
	public Datatype singleton(Object value) {
		if(value instanceof Boolean) {
			boolean bool = ((Boolean) value).booleanValue();	
			return new XSDDerivedBooleanType(bool ? ONLY_TRUE : ONLY_FALSE);
		}	
			
		return null;			
	}

	/* (non-Javadoc)
	 * @see org.mindswap.pellet.datatypes.Datatype#getValue(java.lang.String)
	 */
	public Object getValue(String value, String datatypeURI) {
	    String str = value.trim();
		if(str.equalsIgnoreCase("false"))
			return Boolean.FALSE;
		if(str.equalsIgnoreCase("true"))
			return Boolean.TRUE;
		
		return null;
	}

	public AtomicDatatype getPrimitiveType() {
		return instance;
	}
    
    public ATermAppl getValue( int i ) {
        return i == 0 ? 
            ATermUtils.makeTypedLiteral( "false", name.getName()):
            ATermUtils.makeTypedLiteral( "true", name.getName());
    }
}
