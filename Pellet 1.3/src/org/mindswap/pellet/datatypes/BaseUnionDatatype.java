/*
 * Created on May 29, 2004
 */
package org.mindswap.pellet.datatypes;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.mindswap.pellet.exceptions.InternalReasonerException;
import org.mindswap.pellet.utils.SetUtils;

import aterm.ATermAppl;

/**
 * @author Evren Sirin
 */
public class BaseUnionDatatype extends BaseDatatype implements UnionDatatype {
	protected Set members;

	BaseUnionDatatype(ATermAppl name, Datatype[] members) {
		super(name);
		
		this.members = SetUtils.create(members);
	}
		
	BaseUnionDatatype(ATermAppl name, Set members) {
		super(name);
		
		this.members = members;
	}
	
	BaseUnionDatatype(Datatype[] members) {
		super(null);
		
		this.members = SetUtils.create(members);
	}
	
	BaseUnionDatatype(Set members) {
		super(null);
		
		this.members = members;
	}

	public Set getMembers() {
		return Collections.unmodifiableSet(members);
	}

	public int size() {
		int size = 0;
		for(Iterator i = members.iterator(); i.hasNext(); ) {
			Datatype dt = (Datatype) i.next();
			size += dt.size();
            if( size < 0 )
                return Integer.MAX_VALUE;
		}
		
		return size;
	}
	
	public boolean contains(Object value) {
		for(Iterator i = members.iterator(); i.hasNext(); ) {
			Datatype dt = (Datatype) i.next();
			if(dt.contains(value))
				return true;			
		}		
		
		return false;
	}

	public boolean contains(Object value, AtomicDatatype datatype) {
		//Datatype valDatatype = (Datatype) datatype;	
		for(Iterator i = members.iterator(); i.hasNext(); ) {
			Datatype dt = (Datatype) i.next();
			if( dt instanceof AtomicDatatype ) {
			    if( !datatype.getPrimitiveType().equals(((AtomicDatatype) dt).getPrimitiveType()) )
			        continue;
			}
			if( dt.contains(value, datatype) )
				return true;			
		}		
		return false;
	}

	public Object getValue(String value, String datatypeURI) {
		Object obj = null;
		for(Iterator i = members.iterator(); obj == null && i.hasNext(); ) {
			Datatype dt = (Datatype) i.next();
			obj = dt.getValue(value, datatypeURI);			
		}		
		
		return obj;
	}

	public Datatype singleton(Object value) {
		Datatype datatype = null;
		for(Iterator i = members.iterator(); datatype == null && i.hasNext(); ) {
			Datatype dt = (Datatype) i.next();
			if(dt.contains(value))
			    datatype = dt.singleton(value);			
		}		
		
		return datatype;
	}

    public ATermAppl getValue( int n ) {
        for(Iterator i = members.iterator(); i.hasNext(); ) {
            Datatype dt = (Datatype) i.next();
            if( n <= dt.size() )
                return dt.getValue( n );
            else
                n -= dt.size();
        }
        
        throw new InternalReasonerException( "No values for this datatype" );
    }
	
	
}
