/*
 * Created on May 29, 2004
 */
package org.mindswap.pellet.datatypes;

import org.mindswap.pellet.utils.ATermUtils;


/**
 * @author Evren Sirin
 */
public class XSDSimpleType extends BaseUnionDatatype {
	public static final XSDSimpleType instance = new XSDSimpleType();

	/**
	 * @param members
	 */
	XSDSimpleType() {
		super(ATermUtils.makeTermAppl("XSDSimpleType"),
		new Datatype[] { 
			XSDDecimal.instance, 
			XSDString.instance, 
			XSDBoolean.instance,
			XSDFloat.instance, 
			XSDDouble.instance,
			XSDYear.instance,
			XSDDateTime.instance,
			XSDDay.instance,
			XSDMonthDay.instance,
			XSDMonth.instance,			
			XSDDate.instance,
			XSDYearMonth.instance,
			XSDTime.instance,
			XSDAnyURI.instance 
		});
	}
	
	/**
	 * 
	 * Return a new XSD datatype derived from the current type by restricting a facet. If the datatype
	 * does not support the given facet (e.g. xsd:string does not support numeric facets like 
	 * minInclusive whereas xsd:integer does not support text facets like minLength) an exception
	 * is thrown. Facets are case insensitive. See 
	 * <a href="http://www.w3.org/TR/2001/REC-xmlschema-2-20010502/#rf-facets">facets</a> for a 
	 * complete list of constraining factes available in XSD.
	 * 
	 * @param facet
	 * @param value
	 * @return
	 */
	public Datatype deriveByRestriction(String facet, String value) throws UnsupportedOperationException {
		throw new UnsupportedOperationException(
				"Base type XSDSimpleType cannot be used to derive " +
				"new types. New types can be derived only from primitive types.");
	}
}
