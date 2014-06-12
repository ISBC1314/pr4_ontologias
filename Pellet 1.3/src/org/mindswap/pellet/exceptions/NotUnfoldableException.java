/*
 * Created on Oct 9, 2005
 */
package org.mindswap.pellet.exceptions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import aterm.ATerm;
import aterm.ATermAppl;


public class NotUnfoldableException extends TBoxException {
	ATerm offender;
	Set terms = new HashSet();
	List termlist = new ArrayList();
	
	public NotUnfoldableException() {
		super();
	}
	
	public NotUnfoldableException(String e) {
		super(e);
	}
	
	public NotUnfoldableException(ATerm term) {
		super();
		setOffender(term);
	}
	
	public NotUnfoldableException(ATerm term, String e) {
		super(e);
		setOffender(term);
	}

	public void setOffender(ATerm term) {
		offender = term;
	}

	public void addTerm(ATerm term) {
		terms.add(term);
		termlist.add(term);
	}

	public String toString() {
		StringBuffer retBuffer = new StringBuffer( super.toString() );
		retBuffer.append("\nPath to problem:\n");
		for(Iterator i = termlist.iterator(); i.hasNext();) {
            ATermAppl term = (ATermAppl) i.next();

			retBuffer.append("  " + term + "\n");
		}

		return retBuffer.toString();
	}
}