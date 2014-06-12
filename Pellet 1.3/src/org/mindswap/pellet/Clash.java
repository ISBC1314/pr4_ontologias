/*
 * Created on Aug 29, 2004
 */
package org.mindswap.pellet;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.mindswap.pellet.utils.ATermUtils;

import aterm.ATerm;
import aterm.ATermAppl;

public class Clash {
	public static final String[] TYPES = { 
	/* 0 */	"An individual belongs to a type and its complement", 
    /* 1 */	"An individual contains a minCardinality restriction that is greater than a maxCardinality restriction", 
    /* 2 */	"The maxCardinality restriction is violated", 
    /* 3 */	"An individual contains a minCardinality restriction that is greater than a maxCardinality restriction", 
    /* 4 */	"The maxCardinality(0) restriction is violated", 
    /* 5 */	"An individual is sameAs and differentFrom another individual at the same time",
    /* 6 */	"Range restrictions on a literal is inconsistent", 
    /* 7 */	"The literal value does not satisfy the datatype restriction", 
    /* 8 */	"Plain literal does not satisfy the datatype restriction (literal may be missing the rdf:datatype attribute)", 
    /* 9 */	"Invalid literal for the rdf:datatype attribute", 
    /*10 */	"Cannot explain"
	};
	
	public static final String[] SHORT = { 
		"ATOMIC", "MIN_MAX", "MAX_CARD", 
        "FUNC_MAX_CARD", "MAX_ZERO", "NOMINAL", 
        "EMPTY_DATATYPE", "VALUE_DATATYPE", "MISSING_DATATYPE", 
        "INVALID_LITERAL", "UNEXPLAINED" 
	};
	
	// TODO Make all private
	private static final int ATOMIC           = 0;
	public static final int MIN_MAX           = 1;
	private static final int MAX_CARD         = 2;
	private static final int FUNC_MAX_CARD    = 3;
//	public static final int MAX_ZERO          = 4;
	public static final int NOMINAL           = 5;		
	public static final int EMPTY_DATATYPE    = 6;
	private static final int VALUE_DATATYPE   = 7;
	private static final int MISSING_DATATYPE = 8;
	private static final int INVALID_LITERAL  = 9;
	private static final int UNEXPLAINED      = 10;
//	public static final int LITERAL_CLASS;
//	public static final int INDIVIDUAL_DATATYPE;
	
	DependencySet depends;
	Node node;
	int type;
	ATerm[] args;
	String explanation;
	
	// TODO Make constructor privates and only use public creator functions
	Clash(Node node, int type, DependencySet depends) {
		this.depends = depends;
		this.node = node;
		this.type = type;
	}

	Clash(Node node, int type, DependencySet depends, ATerm[] args) {
		this.depends = depends;
		this.node = node;
		this.type = type;
		this.args = args;
	}
	
	Clash(Node node, int type, DependencySet depends, String explanation) {
		this.depends = depends;
		this.node = node;
		this.type = type;
		this.explanation = explanation;
	}

	public boolean isAtomic() {
	    return type == ATOMIC;
	}
	
	public static Clash unexplained(Node node, DependencySet depends) {
		return new Clash(node, UNEXPLAINED, depends);	    
	}
	
	public static Clash atomic(Node node, DependencySet depends) {
		return new Clash(node, ATOMIC, depends);	    
	}
	
	public static Clash atomic(Node node, DependencySet depends, ATermAppl c) {
		return new Clash(node, ATOMIC, depends, new ATerm[] {c});	    
	}
	
	public static Clash maxCardinality(Node node, DependencySet depends) {
		return new Clash(node, MAX_CARD, depends);	    
	}
	
	public static Clash maxCardinality(Node node, DependencySet depends, ATermAppl r, int n) {
		return new Clash(node, MAX_CARD, depends, new ATerm[] {r, ATermUtils.getFactory().makeInt(n)});	    
	}
	
	public static Clash functionalCardinality(Node node, DependencySet depends) {
		return new Clash(node, FUNC_MAX_CARD, depends);	    
	}
	
	public static Clash functionalCardinality(Node node, DependencySet depends, ATermAppl r) {
		return new Clash(node, FUNC_MAX_CARD, depends, new ATerm[] {r});	    
	}
	
	public static Clash missingDatatype(Node node, DependencySet depends) {
		return new Clash(node, MISSING_DATATYPE, depends);	    
	}
	
	public static Clash missingDatatype(Node node, DependencySet depends, ATermAppl value, ATermAppl datatype) {
		return new Clash(node, MISSING_DATATYPE, depends, new ATermAppl[] {value, datatype});	    
	}	
	
	public static Clash nominal(Node node, DependencySet depends) {
		return new Clash(node, NOMINAL, depends);	    
	}
	
	public static Clash nominal(Node node, DependencySet depends, ATermAppl other) {
		return new Clash(node, NOMINAL, depends, new ATermAppl[] {other});	    
	}
	
	public static Clash valueDatatype(Node node, DependencySet depends) {
		return new Clash(node, VALUE_DATATYPE, depends);	    
	}
	
	public static Clash valueDatatype(Node node, DependencySet depends, ATermAppl value, ATermAppl datatype) {
		return new Clash(node, VALUE_DATATYPE, depends, new ATermAppl[] {value, datatype});	    
	}
	
	public static Clash invalidLiteral(Node node, DependencySet depends) {
		return new Clash(node, INVALID_LITERAL, depends);	    
	}
	
	public static Clash invalidLiteral(Node node, DependencySet depends, ATermAppl value) {
		return new Clash(node, INVALID_LITERAL, depends, new ATermAppl[] {value});	    
	}
	
	public String detailedString() {
		String str;
		
		if(explanation != null)
		    str = explanation;
        else if(type == UNEXPLAINED) 
            str = "No explanation was generated.";     
		else if(args == null) 
		    str = "No specific explanation was generated. Generic explanation: " + TYPES[type];		
		else if(type == ATOMIC)
		    str = atomicExplanation();
		else if(type == MAX_CARD)
		    str = maxCardinalityExplanation();
		else if(type == FUNC_MAX_CARD)
		    str = functionalCardinalityExplanation();
		else if(type == NOMINAL)
		    str = nominalExplanation();
		else if(type == MISSING_DATATYPE)
		    str = missingDatatypeExplanation();
		else if(type == VALUE_DATATYPE)
		    str = valueDatatypeExplanation();
		else if(type == INVALID_LITERAL)
		    str = invalidLiteralExplanation();
		else
		    str = explanation;

		return str;
	}
	
	public String describeNode( Node node ) {
		String str = "";
		if(node.getNameStr().startsWith("Any member of"))
		    str += node.getNameStr();
		else if(node.isNamedIndividual())
		    str += "Individual " + node.getNameStr();
		else {
		    List path = node.getPath();
		    if(path.isEmpty()) {
		        str += "There is an anonymous individual which";
		    }
		    else {
			    ATermAppl first = (ATermAppl) path.get(0);
			    Iterator i = path.iterator();		    
			    String nodeID = "";
			    if(first.getName().startsWith("Any member of")) {
			        nodeID = "Y";
			        str += first.getName() + ", X, is related to some " + nodeID + ", identified by this path (X ";
			        i.next();
			    }
			    else {
			        nodeID = "X";
			        str += "There is an anonymous individual X, identified by this path (" + i.next() + " ";
			    }
			    
			    while(i.hasNext()) {
			        str += i.next() + " ";
			        if(i.hasNext())
			            str += "[ ";
			    }

			    str += nodeID;
			    for(int count = 0; count < path.size() - 2; count++)
			        str += " ]";
			    str += "), which";	
		    }
		}		    

		return str;
	}
	
	public String atomicExplanation() {
		return describeNode( node ) + " is forced to belong to class " + args[0] + " and its complement";
	}

	public String maxCardinalityExplanation() {
		return describeNode( node ) + " has more than " + 
			args[1] + " values for property " + args[0] + 
			" violating the cardinality restriction";
	}
	
	public String functionalCardinalityExplanation() {
		return describeNode( node ) + " has more than " + 
			"one value for the functional property " + args[0];
	}
	
	public String missingDatatypeExplanation() {
	    return 
	    	"Plain literal " + ATermUtils.toString((ATermAppl) args[0]) + " does not belong to datatype " + args[1] + 
	    	". Literal value may be missing the rdf:datatype attribute.";
	}
	
	public String nominalExplanation() {
		return describeNode( node ) + " is sameAs and differentFrom " + args[0] + "  at the same time ";
	}
	
	public String valueDatatypeExplanation() {
	    return "Literal value " + ATermUtils.toString((ATermAppl) args[0]) + " does not belong to datatype " + args[1];
	}
	
	public String invalidLiteralExplanation() {
	    return "Literal value \"" + ((ATermAppl) args[0]).getArgument(0) + "\" is not a valid value for the rdf:datatype " + ((ATermAppl) args[0]).getArgument(2);
	}
	
	public String toString() {
	    // TODO fix formatting
		return "[Clash " + node + " " + SHORT[type] + " " + depends.toString() + " " + 
			((args==null) ? (List)null : Arrays.asList(args)) + "]";
	}
}