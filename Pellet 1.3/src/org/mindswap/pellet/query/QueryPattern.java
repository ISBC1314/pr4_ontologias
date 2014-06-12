/*
 * Created on Jan 8, 2005
 */
package org.mindswap.pellet.query;

import aterm.ATermAppl;

/**
 * @author Evren Sirin
 *
 */
public interface QueryPattern {
    public boolean isTypePattern();
    public boolean isEdgePattern();
    public boolean isGround();
    
    public ATermAppl getSubject();
    public ATermAppl getPredicate();
    public ATermAppl getObject();    
    
    public QueryPattern apply( QueryResultBinding binding );
}
