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

/* Created on Jul 26, 2004
 */
package org.mindswap.pellet.query;

import java.util.List;
import java.util.Set;

import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.datatypes.Datatype;

import aterm.ATermAppl;
import aterm.ATermList;

/**
 * @author Evren Sirin
 */
public interface Query {
    /**
     * Add a result var to the query whihc makes this variable distinguished and appear in
     * the results.
     * 
     * @param var
     */
    public void addResultVar( ATermAppl var );
    
    /**
     * Add a distiguished variable to the query but not change the result variables. 
     * @param var
     */
    public void addDistVar( ATermAppl var );
    
    /**
     * Return all the variables used in this query.
     * 
     * @return
     */
    public Set getVars();
    
    /**
     * Return all the object variables, i.e. variables that will be replaced with
     * individual names.
     * 
     * @return
     */
    public Set getObjVars();
    
    /**
     * Return all the literal variables, i.e. variables that will be replaced with
     * data values.
     * 
     * @return
     */
    public Set getLitVars();
    
    /**
     * Return all the (individual) constants used in this query.
     * 
     * @return
     */
    public Set getConstants();
        
    /**
     * Return all the variables that will be in the results. For RDQL, these are the
     * variables in the SELECT clause.
     * 
     * @return
     */
    public List getResultVars();
    
    /**
     * Return all the distinguished variables, i.e. variables that will be bound to
     * individuals (or data values). Normally, this is supposed to be equal to the
     * result vars but sometimes variables not in the result may be forced to be 
     * distinguished. For example, in the query 
     * <pre>
     * SELECT ?y WHERE (?x p ?y) AND (?y langEq "en") 
     * </pre>
     * <code>?x</code> is not a result var but it is a distinguised variable because 
     * there is no other way for an individual to have a property value with a language 
     * tag if that value does not already exist in the ABox.
     * 
     * @return
     */
    public Set getDistVars();
    
    /**
     * Return the distinguished object variables.
     * 
     * @return
     */
    public Set getDistObjVars();
    
    /**
     * Return the distinguished literal variables.
     * 
     * @return
     */
    public Set getDistLitVars();
    
    /**
     * Returns true if there are not variables in the query (only constants).
     * @return
     */
    public boolean isGround();
    
    /**
     * Return all the query patterns in this query.
     * 
     * @return
     */
    public List getQueryPatterns();

    /**
     * Find edges in the query graph that matches the pattern (where null matches anything)  
     * 
     * @param subj
     * @param pred
     * @param obj
     * @return
     */
    public List findPatterns(ATermAppl subj, ATermAppl pred, ATermAppl obj);

    public void addPattern( QueryPattern pattern );
    public void addTypePattern( ATermAppl ind, ATermAppl c );
    public void addEdgePattern( ATermAppl s, ATermAppl p, ATermAppl o );
    public void addConstraint( ATermAppl lit, Datatype dt );    
    
    public ATermAppl rollUpTo( ATermAppl term );

    public ATermList getClasses(ATermAppl objTerm);
    public Datatype getDatatype(ATermAppl term);
    
    /**
     * Replace the variables in the query with the values specified in the binding
     * and return a new query instance. 
     * 
     * @param binding
     * @return
     */
    public Query apply(QueryResultBinding binding);
    
    /**
     * The KB that will be used to answer this query.
     * 
     * @return
     */
    public KnowledgeBase getKB();
    
    public void prepare();
}
