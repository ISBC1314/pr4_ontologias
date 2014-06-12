//The MIT License
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

package org.mindswap.pellet.tbox.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.mindswap.pellet.utils.ATermUtils;


import aterm.ATerm;
import aterm.ATermAppl;

public class TermDefinition {
	private ATermAppl sub;
	private List samelist;
	private Set seen;
	public Set dependencies;

	public TermDefinition() {
		sub = null;
		samelist = new ArrayList();
		seen = new HashSet();
		updateDependencies();
	}

	public TermDefinition(TermDefinition td) {
		sub = td.sub;
		samelist = new ArrayList(td.samelist);
		seen = new HashSet(td.seen);
		updateDependencies();
	}

	public ATermAppl getName() {
		ATermAppl name = null;
		if (size() > 0) {
			name = (ATermAppl) getDef(0).getArgument(0);
		}
		return name;
	}

	public void addDef(ATermAppl appl) {
		if (seen.contains(appl)) {
			return;
		} else {
			seen.add(appl);
		}

		String name = appl.getName();
		if (name.equals(ATermUtils.SUB)) {
			if (sub != null) {
				ATerm conjunct = ATermUtils.makeAnd(appl.getArgument(1), sub
						.getArgument(1));
				sub = sub.setArgument(conjunct, 1);
			} else {
				sub = appl;
			}
		} else if (name.equals(ATermUtils.SAME)) {
			samelist.add(appl);
		} else {
			throw new RuntimeException("Cannot add non-definition!");
		}
		updateDependencies();
	}

	public void removeDef(int i) {
		if (sub != null) {
			if (i == 0) {
				seen.remove(sub);
				sub = null;
			}
			i--;
		}
		if (i >= 0) {
			seen.remove(samelist.get(i));
			samelist.remove(i);
		}
		updateDependencies();
	}

	public void replaceDef(int i, ATermAppl appl) {
		removeDef(i);
		addDef(appl);
		updateDependencies();
	}

	public boolean isPrimitive() {
		return samelist.isEmpty();
	}
	
	public boolean isUnique() {
		if (((sub != null) && (samelist.size() == 0))
				|| ((sub == null) && (samelist.size() == 1))) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isGCI() {
		ATermAppl term = getDef(0);
		if (((ATermAppl) term.getArgument(0)).getArity() == 0) {
			return false;
		}
		return true;
	}

	public List toList() {
		ArrayList list = new ArrayList();
		if (sub != null)
			list.add(sub);
		list.addAll(samelist);
		return list;
	}

	public ATermAppl getDef(int i) {
		if (sub != null) {
			if (i == 0) {
				return sub;
			}
			i--;
		}
		return (ATermAppl) samelist.get(i);
	}

	public ATermAppl getSub() {
		return sub;
	}

	public List getSames() {
		return samelist;
	}

	public int size() {
		int s = samelist.size();
		if (sub != null)
			s++;
		return s;
	}

	public String toString() {
		return (sub != null) ? sub + "; " + samelist : samelist.toString();
	}
	
	protected void updateDependencies() {
		dependencies = new HashSet();
		if (getSub() != null) {
			dependencies.addAll(ATermUtils.findPrimitives((ATermAppl) getSub().getArgument(1)));
		}
		for (Iterator iter=getSames().iterator(); iter.hasNext();) {
			ATermAppl same = (ATermAppl) iter.next();
			dependencies.addAll(ATermUtils.findPrimitives((ATermAppl)same.getArgument(1)));
		}
	}
}
