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

/*
 * Created on Oct 9, 2004
 */
package org.mindswap.pellet.jena;

import java.io.StringWriter;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.mindswap.pellet.output.ATermAbstractSyntaxRenderer;
import org.mindswap.pellet.output.OutputFormatter;

import aterm.ATermAppl;

/**
 * Class that stores the messages about OWL species validation. Each message added to 
 * the report contains the level it belong so level of docuemnt is simply found by the
 * level of messages. The report supports printing in HTML format, i.e. URI's are printed 
 * as hyperlinks.
 * 
 * @author Evren Sirin
 */
public class OWLSpeciesReport {    
    public static final int NUM_CODES = 4;
    
    private int level = OWLSpecies.LITE;
    private Set[] messages = new Set[NUM_CODES];

	public OWLSpeciesReport() {
	    for(int i = 0; i < NUM_CODES; i++)
	        messages[i] = new TreeSet();	    
	}
	
	/**
	 * Add a message 
	 * addMessage
	 * 
	 * @param code
	 * @param msg
	 */
	public void addMessage(int code, OWLSpeciesMessage msg) {
	    if(code < 0 || code > NUM_CODES)
	        throw new IllegalArgumentException();
	    
	    if(code > level)
	        level = Math.min(code, OWLSpecies.FULL);
	        
	    messages[code].add(msg);
	}

	public void addMessage(int code, String header, String msg) {
	    addMessage(code, new OWLSpeciesMessage(header, msg, new ATermAppl[0]));
	}
	
	public void addMessage(int code, String header, String msg, ATermAppl term) {
	    addMessage(code, new OWLSpeciesMessage(header, msg, new ATermAppl[] { term }));
	}
	
	public void addMessage(int code, String header, String msg, ATermAppl term1, ATermAppl term2) {
	    addMessage(code, new OWLSpeciesMessage(header, msg, new ATermAppl[] { term1, term2 }));
	}

	public void addMessage(int code, String header, String msg, ATermAppl[] terms) {
	    addMessage(code, new OWLSpeciesMessage(header, msg, terms));
	}

	/**
	 * Return the level of the document. One of OWLSpecies.LITE, OWLSpecies.DL or 
	 * OWLSpecies.FULL.
	 * 
	 * @return
	 */
	public int getLevel() {
	    return level;
	}	
	
	/**
	 * Print the report on standard output.
	 */
	public void print() {
	    print(new OutputFormatter());
	}
	
	/**
	 * Print the report on specified output.
	 */
	public void print(OutputFormatter out) {
	    ATermAbstractSyntaxRenderer renderer = new ATermAbstractSyntaxRenderer();
	    renderer.setWriter(out);
	    
	    String[] headers = {"", "Non OWL-Lite features used:", "Non OWL-DL features used:", "Warnings:"};
		for(int i = NUM_CODES - 1; i > 0; i--) {
			Set set = messages[i];
			if (set.size() > 0) {
				out.printBold(headers[i]).println();
				Iterator iter = set.iterator();
				while (iter.hasNext()) {
				    OWLSpeciesMessage msg = (OWLSpeciesMessage) iter.next(); 
					msg.print(renderer);
					out.println();
				}
			}
		}
		out.flush();
	}	
	
	public String toString() {
		StringWriter sw = new StringWriter();
		print(new OutputFormatter(sw, false));
		
		return sw.toString();
	}
}
