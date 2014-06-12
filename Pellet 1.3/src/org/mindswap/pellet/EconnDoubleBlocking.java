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
 * Created on May 4, 2004
 */
package org.mindswap.pellet;

import java.util.Iterator;
import java.util.List;

/**
 * @author Bernardo Cuenca
 */
public class EconnDoubleBlocking extends Blocking {
	private ABox abox;
	/**
	 * @param abox
	 */
	public EconnDoubleBlocking(ABox a) {
	  super();
	  abox=a;
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.mindswap.pellet.Blocking#isDirectlyBlocked(aterm.ATerm)
	 */
	public boolean isDirectlyBlocked(Individual x, List ancestors) {
		// 1) y is not a root node 
		// 2) x1, y and y1 are ancestors of x
		// 3) x is a successor of x1
		// 4) y is a successor of y1
		// 5) types(x) == types(y) && types(x1) == types(y1)
		// 6) edges(x1, x) == edges(y1, y)
		// For Econn also we need to make sure that 
		     // ontology(x)==ontology(y) && ontology(x1)==ontology(y1)
		
		// In the implementation, x1 is iterated over all predecessors of
		// x (2, 3), y is iterated over all the ancestors of x (2), y1 is
		// iterated over all the predecessors of y (2, 4). blockCondition2
		// checks (5), blockCondition3 checks (6)
		
		//blockingcondition4 checks (7)
		
		//We will use this to check if x is a link successor.
		//Note that a node that has incoming link edges cannot have 
		//incoming edges of other type (object or datatype edges)
		EdgeList l = x.getInEdges();
		boolean linkSuccessor = false;
		if(!l.isEmpty()){
			Edge e = l.edgeAt(0);
		    linkSuccessor = e.getRole().isLinkRole();}
		
		
		Iterator i = ancestors.iterator();			
		Iterator predecessors = x.getPredecessors().iterator();
//		If it is a link successor we only need equality blocking
		if (linkSuccessor)
		{
		 while(i.hasNext()){
		 	Individual y = (Individual) i.next();
			if(y.isRoot())
				continue;
            if(linkBlockingCondition(x,y))
            	return true;
		 }
		
		}
		else{
			if(((EconnectedKB)abox.getKB()).getEconnExpressivity().getInverses().get(x.getOntology())==Boolean.FALSE){
				//Apply only subset blocking
				while(i.hasNext()){
				 	Individual y = (Individual) i.next();
					if(y.isRoot() || x.equals(y))
						continue;
		            if(blockingCondition5(x,y))
		            	return true;
				 }
					
			 }//End if
            else{ 
            	if(((EconnectedKB)abox.getKB()).getEconnExpressivity().getCardinality().get(x.getOntology())==Boolean.FALSE){
    				//Apply only equality blocking
    				while(i.hasNext()){
    				 	Individual y = (Individual) i.next();
    					if(y.isRoot())
    						continue;
    		            if(blockingCondition1(x,y))
    		            	return true;
    				 }//end while
    					
    			 }//End if
                 else{
                 	while(predecessors.hasNext()) {
                 		Individual x1 = (Individual) predecessors.next();
                 		while(i.hasNext()) {
                 			Individual y = (Individual) i.next();
                 			if(y.isRoot())
                 				continue;

                 			Iterator y1s = y.getPredecessors().iterator();
                 			while(y1s.hasNext()) {
                 				Individual y1 = (Individual) y1s.next();
        					
                 				// it is not clear if this is required
                 				if(x.equals(y) || y.equals(y1))
                 					continue;					
                 				if(blockCondition2(x, x1, y, y1) && blockCondition3(x, x1, y, y1) && blockCondition4(x, x1, y, y1))
                 					return true;					
                 			}//end while
                 		}//end while
                 	}//end while

             }//end else
           }//end else
	}
		
		return false;
	}

	
	protected boolean linkBlockingCondition(Individual x, Individual y){
		return equals(x, y);
    }
	
	protected boolean blockingCondition1(Individual x, Individual y){
		return equals(x, y);
    }
	
	protected boolean blockingCondition5(Individual x, Individual y){
		return subset(x, y);
    }
	
	protected boolean blockCondition2(Individual x, Individual x1, Individual y, Individual y1) {	
		return equals(x, y) && equals(y1, x1);
	}
	
	protected boolean blockCondition4(Individual x, Individual x1, Individual y, Individual y1) {	
		if(x.getOntology()==y.getOntology() && x1.getOntology()==y1.getOntology())
	       return true;
		else
			return false;
	}    
	
	protected boolean blockCondition3(Individual x, Individual x1, Individual y, Individual y1) {
		EdgeList xEdges = x1.getEdgesTo(x);
		EdgeList yEdges = y1.getEdgesTo(y);
		
		for(int i = 0; i < xEdges.size(); i++)
			if(!yEdges.hasEdge(xEdges.edgeAt(i).getRole()))
				return false;
			
		return true;
	}	
}
