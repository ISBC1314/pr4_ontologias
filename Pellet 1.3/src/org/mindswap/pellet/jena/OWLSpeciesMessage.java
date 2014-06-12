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
 * Created on Oct 10, 2004
 */
package org.mindswap.pellet.jena;

import java.util.Arrays;

import org.mindswap.pellet.output.ATermRenderer;
import org.mindswap.pellet.utils.HashCodeUtil;

import aterm.ATermAppl;

/**
 * Create a message for OWL Species report. The functionality of this class is similar to
 * printf in C. It has a format string and an array of arguments that will be printed
 * inside the message. format strings look like <br>
 * 		"This is the first argument %1% and this is the second %2%"<br>
 * where %n% will be replaced by the nth item in the array. 
 * 
 * The reason arguments are printed late is to support different formats, e.g. terms in 
 * abstract syntax format, HTML output, etc. The format of the output is determined by
 * the ATermRenderer passed to the print function.
 * 
 * @author Evren Sirin
 */
public class OWLSpeciesMessage implements Comparable {
    String header;
    String msg;
    ATermAppl[] terms;
    int hashCode;
    
    public OWLSpeciesMessage(String header, String msg, ATermAppl[] terms) {
        this.header = header;
        this.msg   = msg;
        this.terms = terms;
        
        computeHashCode();
    }
    
    public boolean equals(Object other) {
        if(this == other) return true;
        if(!(other instanceof OWLSpeciesMessage)) return false;
        OWLSpeciesMessage that = (OWLSpeciesMessage) other;
        return header.equals(that.header) && msg.equals(that.msg) && Arrays.equals(terms, that.terms);
    }

    private void computeHashCode() {
        hashCode = HashCodeUtil.SEED;
        hashCode = HashCodeUtil.hash(hashCode, header);
        hashCode = HashCodeUtil.hash(hashCode, msg);
        hashCode = HashCodeUtil.hash(hashCode, terms);            
    }
    
    public int hashCode() {
        return hashCode;
    }

    public int compareTo(Object o) {
        OWLSpeciesMessage other = (OWLSpeciesMessage) o;
        int cmp = header.compareTo(other.header);
        if(cmp == 0)
            cmp = msg.compareTo(other.msg);
        if(cmp == 0)
            cmp = hashCode - other.hashCode;                
        return cmp;
    }
    
    public void print(ATermRenderer renderer) {
        renderer.getWriter().printItalic(header).print(": ");
        String[] parts = msg.split("%");
        for(int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if(part.length() == 1 && Character.isDigit(part.charAt(0))) {
                int index = part.charAt(0) - '1';
                renderer.visit(terms[index]);
            }
            else
                renderer.getWriter().print(part);
        }
    }
}