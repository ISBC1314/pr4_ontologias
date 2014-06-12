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
 * Created on Jan 3, 2005
 */
package org.mindswap.pellet.jena;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.GraphListener;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.TripleMatch;
import com.hp.hpl.jena.graph.compose.MultiUnion;
import com.hp.hpl.jena.reasoner.InfGraph;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.NullIterator;

/**
 * An extension to MultiUnion graph that may return repeated triples from find function. When
 * big composite graphs are being loaded to Pellet, filtering results to return unique triples
 * is slowing things down whereas having triples repeated has less impact on the performance.  
 * 
 * @author Evren Sirin
 *
 */
public class DisjointMultiUnion extends MultiUnion implements GraphListener {
    private boolean listenChanges;
    private boolean deletion = false;
    
    private Set changedGraphs;
    
    public DisjointMultiUnion() {
        this( false );
    }
    
    public DisjointMultiUnion( boolean listenChanges ) {
        super();
        
        changedGraphs = new HashSet();
        this.listenChanges = listenChanges;
    }

    public DisjointMultiUnion(Graph graph) {
        this( false );
        
        addGraph( graph );        
    }
    
//    public DisjointMultiUnion(Graph[] graphs) {
//        for (int i = 0;  i < graphs.length;  i++) {
//            addGraph( graphs[i] );
//        }
//    }

    public boolean isEmpty() {
        for (Iterator i = m_subGraphs.iterator();  i.hasNext();  ) {
            if (!((Graph) i.next()).isEmpty()) {
                return false;
            }
        }
        
        return true;
    }
    
    public ExtendedIterator graphBaseFind(TripleMatch t) {
        Iterator graphs = m_subGraphs.iterator();

        if( !graphs.hasNext() )
        	return NullIterator.instance;
                    
        // create a MultiIterator with the first graph's results
        Graph firstG = (Graph) graphs.next();
        ExtendedIterator i = new MultiIterator( firstG.find( t ) );
        
        // now add the rest of the chain
        while ( graphs.hasNext() ) {
            Graph nextG = (Graph) graphs.next();
            i = i.andThen( nextG.find( t ) );
        }
        
        // this graph does not support .remove function
        return i;  // UniqueExtendedIterator.create( i );  
    }
    
    public void addGraph( Graph graph ) { 
        if (!m_subGraphs.contains( graph )) {
            if (graph instanceof MultiUnion) {
                MultiUnion union = ((MultiUnion) graph);
                if (union.getBaseGraph() != null)
                    addGraph( union.getBaseGraph() );
                
                for (Iterator i = union.getSubGraphs().iterator();  i.hasNext();  )
                   addGraph( (Graph) i.next() );                
            }
            else if( graph instanceof InfGraph ) {
                addGraph(((InfGraph) graph).getRawGraph());
            }
            else {
                m_subGraphs.add( graph );
                if(listenChanges)                    
                    graph.getEventManager().register( this ); 
            }            
        }
    }
    
    public void releaseListeners() {
        for (Iterator graphs = m_subGraphs.iterator(); graphs.hasNext(); ) {
            Graph graph = (Graph) graphs.next();
            graph.getEventManager().unregister( this ); 
        }
    }
    
    public void notifyAddTriple(Graph g, Triple t) {       
        changedGraphs.add( g );
    }

    public void notifyAddArray(Graph g, Triple[] triples) {       
        changedGraphs.add( g );
    }

    public void notifyAddList(Graph g, List triples) {       
        changedGraphs.add( g );
    }

    public void notifyAddIterator(Graph g, Iterator it) {       
        changedGraphs.add( g );
    }

    public void notifyAddGraph(Graph g, Graph added) {        
        changedGraphs.add( g );
    }

    public void notifyDeleteTriple(Graph g, Triple t) {       
        deletion = true;
    }

    public void notifyDeleteList(Graph g, List L) {               
        deletion |= !L.isEmpty();
    }

    public void notifyDeleteArray(Graph g, Triple[] triples) {       
        deletion |= (triples.length > 0);
    }

    public void notifyDeleteIterator(Graph g, Iterator it) {
        deletion |= it.hasNext();        
    }

    public void notifyDeleteGraph(Graph g, Graph removed) {                
        deletion = true;
    }

    public void notifyEvent(Graph source, Object value) {
        deletion = true;
    }

    public boolean isStatementDeleted() {
        return deletion;
    }
    
    public void resetChanged() {
        deletion = false;
        changedGraphs.clear();
    }

    public DisjointMultiUnion minus(DisjointMultiUnion other) {
        if(!m_subGraphs.containsAll(other.m_subGraphs))
            return null;
        
        DisjointMultiUnion diff = new DisjointMultiUnion();
        for (Iterator graphs = m_subGraphs.iterator(); graphs.hasNext(); ) {
            Graph g = (Graph) graphs.next();
            if(!other.m_subGraphs.contains(g) || other.changedGraphs.contains(g))
                diff.addGraph(g);
        }
        
        return diff;
    }
}
