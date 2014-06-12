/*
 * Created on Jul 1, 2005
 */
package org.mindswap.pellet.jena;

import org.mindswap.pellet.exceptions.InternalReasonerException;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.util.iterator.Map1;


class TripleFiller implements Map1 {
    private Node node1;
    private Node node2;
    private byte fill;
    
    private TripleFiller( Node node1, Node node2, byte fill ) {
        this.node1 = node1;
        this.node2 = node2;
        this.fill = fill;
    }
    
    public static TripleFiller fillAny( Node node1, Node node2, byte fill ) {
        return new TripleFiller( node1, node2, fill );
    }

    public static TripleFiller fillSubjObj( Node pred, Node node, byte fill ) {
        return ( fill == PelletInfGraph.SUBJ )
        	? new TripleFiller( pred, node, PelletInfGraph.SUBJ )
        	: new TripleFiller( node, pred, PelletInfGraph.OBJ );
    }

    public static TripleFiller fillSubj( Node pred, Node obj ) {
        return new TripleFiller( pred, obj, PelletInfGraph.SUBJ );
    }
    
    public static TripleFiller fillPred( Node subj, Node obj ) {
        return new TripleFiller( subj, obj, PelletInfGraph.PRED );
    }

    public static TripleFiller fillObj( Node subj, Node pred ) {
        return new TripleFiller( subj, pred, PelletInfGraph.OBJ );
    }        
    
    public Object map1( Object o ) {
        switch( fill ) {
        	case PelletInfGraph.SUBJ:
        	    return new Triple( (Node) o, node1, node2 );
        	case PelletInfGraph.PRED:
        	    return new Triple( node1, (Node) o, node2 );
        	case PelletInfGraph.OBJ:
        	    return new Triple( node1, node2, (Node) o );
        	default:
        	    break;
        }
        
        throw new InternalReasonerException( "Invalid triple filler!" );
    }

}