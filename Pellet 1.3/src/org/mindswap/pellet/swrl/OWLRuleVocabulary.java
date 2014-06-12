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

package org.mindswap.pellet.swrl;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class OWLRuleVocabulary {
	private static String URI = "http://www.w3.org/2002/07/owlRules#";

	public static final Resource Rule = ResourceFactory.createResource(URI+"Rule");	
	public static final Resource Variable = ResourceFactory.createResource(URI+"Variable");
	public static final Resource individualPropertyAtom = ResourceFactory.createResource(URI+"individualPropertyAtom");
	public static final Resource datavaluedPropertyAtom = ResourceFactory.createResource(URI+"Variable");
	public static final Resource classAtom = ResourceFactory.createResource(URI+"classAtom");
	public static final Resource sameIndividualAtom = ResourceFactory.createResource(URI+"sameIndividualAtom");
	public static final Resource differentIndividualAtom = ResourceFactory.createResource(URI+"differentIndividualAtom");

	public static final Property antecedent = ResourceFactory.createProperty(URI, "antecedent");
	public static final Property consequent = ResourceFactory.createProperty(URI, "consequent");
	public static final Property propertyPredicate = ResourceFactory.createProperty(URI, "propertyPredicate");
	public static final Property argument1 = ResourceFactory.createProperty(URI, "argument1");
	public static final Property argument2 = ResourceFactory.createProperty(URI, "argument2");
	public static final Property classPredicate = ResourceFactory.createProperty(URI, "classPredicate");
}
