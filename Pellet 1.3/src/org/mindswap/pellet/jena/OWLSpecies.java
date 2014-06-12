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

package org.mindswap.pellet.jena;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * Containf the OWL species information along with a detailed species report. 
 * 
 * @author Evren Sirin
 */
public class OWLSpecies {
	public static boolean DEBUG = false;
	
    public static final String[] LEVELS = {"Lite", "DL", "Full" }; 
    public static final int LITE = 0;
    public static final int DL   = 1;
    public static final int FULL = 2; 	
	    
    public static final int WARNING   = 3;

	private OWLSpeciesReport report;
	
	/**
	 * Triples that need to be added to the original document in order to make 
	 * it OWL DL. If this field is null then the document cannot be converted
	 * to OWL DL (i.e. structure sharing) or it is already DL or LITE. 
	 */
	public Model missingTriples;

	public OWLSpecies(OWLSpeciesReport report) {
	    this.report = report;
	}
	
	public OWLSpecies(OWLSpeciesReport report, Model missingTriples) {
	    this.report = report;
	    this.missingTriples = missingTriples;
	}
	
	/**
	 * Return the level of the document as one of the constants defined here
	 * LITE, DL or FULL.
	 * 
	 * @return
	 */
	public int getLevel() {		
		return report.getLevel();
	}
	
	/**
	 * Get the detailed species report.
	 * 
	 * @return
	 */
	public OWLSpeciesReport getReport() {
	    return report;
	}
	
	/**
	 * Return the string representation of the level, i.e. "Lite", "DL" or "Full"
	 */
	public String toString() {
	    return LEVELS[getLevel()];
	}
} 
