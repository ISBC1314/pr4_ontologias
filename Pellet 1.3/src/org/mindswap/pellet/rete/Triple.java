package org.mindswap.pellet.rete;

import java.util.ArrayList;
import java.util.List;

public class Triple {

	protected Term subj, pred, obj;
	
	public Triple() {
		
	}
	
	public Triple(Term s, Term p, Term o) {
		this.subj = s;
		this.pred = p;
		this.obj = o;				
	}
	
	public List getList() {
		List pattern = new ArrayList();
		pattern.add(subj);
		pattern.add(pred);
		pattern.add(obj);
		return pattern;
	}

	/**
	 * @return Returns the obj.
	 */
	public Term getObj() {
		return obj;
	}

	/**
	 * @param obj The obj to set.
	 */
	public void setObj(Term obj) {
		this.obj = obj;
	}

	/**
	 * @return Returns the pred.
	 */
	public Term getPred() {
		return pred;
	}

	/**
	 * @param pred The pred to set.
	 */
	public void setPred(Term pred) {
		this.pred = pred;
	}

	/**
	 * @return Returns the subj.
	 */
	public Term getSubj() {
		return subj;
	}

	/**
	 * @param subj The subj to set.
	 */
	public void setSubj(Term subj) {
		this.subj = subj;
	}

	public List getVars() {
		List v = new ArrayList();
		if (subj instanceof Variable) v.add(subj);
		if (pred instanceof Variable) v.add(pred);
		if (obj instanceof Variable) v.add(obj);
		
		// TODO Auto-generated method stub
		return v;
	}
	
	public String toString() {
		return subj +"," + pred + "," + obj;
	}


}
