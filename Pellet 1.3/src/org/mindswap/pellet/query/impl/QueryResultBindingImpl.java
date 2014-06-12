/*
 * Created on Jan 8, 2005
 */
package org.mindswap.pellet.query.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mindswap.pellet.query.QueryResultBinding;

import aterm.ATermAppl;

/**
 * @author Evren Sirin
 *
 */
public class QueryResultBindingImpl implements QueryResultBinding {
    private Map bindings;
    
    public QueryResultBindingImpl() {
        bindings = new HashMap();
    }
    
    public QueryResultBindingImpl(Map map) {
        bindings = new HashMap(map);
    }

    public void setValue(ATermAppl var, ATermAppl value) {
        // TODO check var is variable
        bindings.put(var, value);
    }
    
    public void setValues( QueryResultBinding binding ) {
        if( binding instanceof QueryResultBindingImpl ) {
            bindings.putAll( ((QueryResultBindingImpl) binding).bindings );
        }
        else {
            for(Iterator i = binding.getVars().iterator(); i.hasNext();) {
                ATermAppl var = (ATermAppl) i.next();
                setValue( var, binding.getValue( var ) );                
            }
        }
    }
    
    public ATermAppl getValue(ATermAppl var) {
        return (ATermAppl) bindings.get(var);
    }
    
    public boolean hasValue(ATermAppl var) {
        return bindings.containsKey(var);
    }

    public Set getVars() {
        return Collections.unmodifiableSet(bindings.keySet());
    }
    
    public Object clone() {
        return new QueryResultBindingImpl(bindings);
    }
    
    public List getValues(List vars) {
        List list = new ArrayList();
        for(int i = 0; i < vars.size(); i++) {
            ATermAppl var = (ATermAppl) vars.get(i);
            list.add(bindings.get(var));
        }
        
        return list;
    }
    
    public boolean equals(Object other) {
        if(this == other) return true;
        if(!(other instanceof QueryResultBindingImpl)) return false;
        QueryResultBindingImpl that = (QueryResultBindingImpl) other;
        return this.bindings.equals(that.bindings);
    }
    
    public int hashCode() {
        return bindings.hashCode();
    }

    public int size() {
        return bindings.size();
    }

    public boolean isEmpty() {
        return bindings.isEmpty();
    }
    
    public String toString() {
        return bindings.toString();
    }
}
