/*
 * Created on May 29, 2004
 */
package org.mindswap.pellet.datatypes;

import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Namespaces;


/**
 * @author Evren Sirin
 */
public class RDFSLiteral extends BaseUnionDatatype implements UnionDatatype {
	public static final RDFSLiteral instance = new RDFSLiteral();

	RDFSLiteral() {
		super(
			ATermUtils.makeTermAppl(Namespaces.RDFS + "Literal"), 
//			new Datatype[] { RDFSPlainLiteral.instance, RDFSTypedLiteral.instance });
			new Datatype[] { XSDSimpleType.instance, RDFXMLLiteral.instance });		
	}

}
