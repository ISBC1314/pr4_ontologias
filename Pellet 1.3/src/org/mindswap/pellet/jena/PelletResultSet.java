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
 * Created on Sep 17, 2004
 */
package org.mindswap.pellet.jena;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.mindswap.pellet.query.QueryResultBinding;
import org.mindswap.pellet.query.QueryResults;
import org.mindswap.pellet.query.QueryUtils;

import aterm.ATermAppl;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.core.ResultBinding;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;

/**
 * 
 * @author Evren Sirin
 */
public class PelletResultSet implements ResultSet {
    private Model model;
    private List resultVars;
    private List varTerms;
    
    private QueryResults answers;
    private int index;
    private int size;

    public PelletResultSet( QueryResults answers, Model model ) {
        this.answers = answers;
        this.model = model;
        this.index = 0;
        this.size = answers.size();
        
        varTerms = answers.getResultVars();
    }
    
    /**
     * Return the underlying QueryResults object
     * @return
     */
    public QueryResults getAnswers() {
        return answers;
    }

    public boolean hasNext() {
        return index < size;
    }
    
    public QuerySolution nextSolution() {
        QueryResultBinding binding = answers.get(index++);
        ResultBinding result = new ResultBinding( model ); 
		
		for ( Iterator i = varTerms.iterator(); i.hasNext(); ) {
			ATermAppl var = (ATermAppl) i.next();
			String varName = QueryUtils.getVarName( var );
			
			ATermAppl value = binding.getValue( var );
			
			RDFNode node = JenaUtils.makeRDFNode( value, model );
			result.add( varName, node );
		}
		
        return result;        
    }

    public Object next() {
        return nextSolution();
    }

    public boolean isDistinct() {
        return false;
    }

    public boolean isOrdered() {
        return false;
    }

    public int getRowNumber() {
        return index;
    }

    public List getResultVars() {
        if( resultVars == null ) {
            resultVars = new ArrayList( varTerms.size() );
			for ( Iterator i = varTerms.iterator(); i.hasNext(); ) {
				ATermAppl var = (ATermAppl) i.next();
				String varName = QueryUtils.getVarName( var );
				resultVars.add(varName);
	        }
	    }
         
        return resultVars;
    }


    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Cannot remove from QueryResults");
    }
    
    public String toString() {
        return answers.toString();
    }
    
}