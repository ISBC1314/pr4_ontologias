package org.mindswap.pellet.rete;

public class Fact extends Triple {

	public Fact(Constant s, Constant p, Constant o) {		
		super(s,p,o);		
	}
	
	public Fact() {
		super();
	}
	public String toString() {
		return "Fact( " + subj + " " + pred + " " + obj + ")";
	}

}
