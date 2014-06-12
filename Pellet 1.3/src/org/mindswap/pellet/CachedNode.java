/*
 * Created on Oct 1, 2005
 */
package org.mindswap.pellet;


/**
 * @author Evren Sirin
 *
 */
public class CachedNode {    
    Individual node;
    DependencySet depends;
    
    public CachedNode( Individual node, DependencySet depends ) {
        this.node = node;
        this.depends = depends.copy();
    }     
    
    public boolean isIncomplete() {
        return node == ABox.DUMMY_IND;
    }
    
    public boolean isComplete() {
        return node != ABox.DUMMY_IND;
    }
    
    public boolean isTop() {
        return node == ABox.TOP_IND;
    }
    
    public boolean isBottom() {
        return node == ABox.BOTTOM_IND;      
    }
    
    public String toString() {
        return node + " " + depends;
    }
}
