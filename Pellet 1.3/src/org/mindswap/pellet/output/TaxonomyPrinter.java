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

/*
 * Created on Jul 28, 2004
 */
package org.mindswap.pellet.output;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.taxonomy.Taxonomy;
import org.mindswap.pellet.utils.ATermUtils;

import aterm.ATermAppl;

/**
 * @author Evren Sirin
 */
public class TaxonomyPrinter {
	// Indentation string used when classification tree is printed
	final static String INDENT = "  ";

	private Taxonomy taxonomy;
	private OutputFormatter out;

	public TaxonomyPrinter() {
	}
	
	public void print(Taxonomy taxonomy) {
		print(taxonomy, new OutputFormatter());
	}

	public void print(Taxonomy taxonomy, OutputFormatter out) {
		this.taxonomy = taxonomy;	
		this.out = out;

		out.println();
		printTree(); 
		out.println();
		out.flush();
		
//		System.out.println(out.qnames);
	}

	private void printTree() {
		LinkedHashSet top = new LinkedHashSet();
		top.add(ATermUtils.TOP);
		top.addAll(taxonomy.getEquivalents(ATermUtils.TOP));
		out.printHTML("<ul>");
		printTree(top, " ");
		out.printHTML("</ul>");
		
		LinkedHashSet bottom = new LinkedHashSet();
		bottom.add(ATermUtils.BOTTOM);
		bottom.addAll(taxonomy.getEquivalents(ATermUtils.BOTTOM));
		if(bottom.size() > 1) {
			out.printHTML("<ul>");
			printNode(bottom, " ");
			out.printHTML("</ul>");
		}
	}

	private void printTree(Set set, String indent) {
	    if(set.contains(ATermUtils.BOTTOM)) return;		
	
		printNode(set, indent);
				
		out.printHTML("<ul>");
		ATermAppl c = (ATermAppl) set.iterator().next();		
		Set subs = taxonomy.getSubs(c, true);
		Iterator j = subs.iterator();
		while(j.hasNext()) {
			Set eqs = (Set) j.next();
			if(eqs.contains(c)) continue;
			printTree(eqs, indent + "   ");
		}		
		
		out.printHTML("</ul>");
	}

	private void printNode(Set set, String indent) {
		if(out.isFormatHTML()) 
		    out.printHTML("<li>");
		else
			out.print(indent);
	
		Iterator i = set.iterator();
		ATermAppl c = (ATermAppl) i.next();
		printURI(out, c);
		while(i.hasNext()) { 
			out.print(" = ");
			printURI(out, (ATermAppl) i.next());
		}
		
		Set instances = taxonomy.getInstances(c, true);
		if(instances.size() > 0) {
			out.print(" - (");
			boolean printed = false;
			int anonCount = 0;
			Iterator ins = instances.iterator();			
			for(int k = 0; ins.hasNext(); k++) {
				ATermAppl x = (ATermAppl) ins.next();
				
				if(x.getName().startsWith(PelletOptions.BNODE))
				    anonCount++;
				else {
				    if(printed) 
				        out.print(", ");
				    else
				        printed = true;
				    printURI(out, x);
				}				
			}
			if(anonCount > 0) {
			    if(printed) out.print(", ");
			    out.print(anonCount + " Anonymous Individual");
			    if(anonCount > 1) out.print("s");
			}
			out.print(")");
		}
			
		if(out.isFormatHTML()) 
			out.printHTML("</li>");
		else
			out.println();		
	}

	private void printURI(OutputFormatter out, ATermAppl c) {
	    String uri = c.getName();
		if(c.equals(ATermUtils.TOP)) 
			uri = "http://www.w3.org/2002/07/owl#Thing";
		else if(c.equals(ATermUtils.BOTTOM))
			uri = "http://www.w3.org/2002/07/owl#Nothing";
		
		out.printURI(uri);
	}
}
