//The MIT License
//
//Copyright (c) 2003 Ron Alford, Mike Grove, Bijan Parsia, Evren Sirin
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to
//deal in the Software without restriction, including without limitation the
//rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
//sell copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in
//all copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
//FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
//IN THE SOFTWARE.

/*
 * Created on Aug 29, 2004
 */
package org.mindswap.pellet;

import aterm.ATermAppl;

/**
 * Stores a pair of nodes to be merged. Order of nodes is important, always first node is 
 * going to be merged to the second one.
 * 
 * @author Evren Sirin
 */
class NodeMerge {
	ATermAppl y;
	ATermAppl z;
	DependencySet ds;
	
	NodeMerge(Node y, Node z) {
		this.y = y.getName();
		this.z = z.getName();
	}
	
	NodeMerge(Node y, Node z, DependencySet ds) {
		this.y = y.getName();
		this.z = z.getName();		
		this.ds = ds;
	}
	
	NodeMerge(ATermAppl y, ATermAppl z) {
		this.y = y;
		this.z = z;
	}
	
	public String toString() {
		return y + " -> " + z + " " + ds;
	}
}