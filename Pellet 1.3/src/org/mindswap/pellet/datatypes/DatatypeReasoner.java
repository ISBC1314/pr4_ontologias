//The MIT License
//
//Copyright (c) 2003 Ron Alford, Mike Grove, Bijan Parsia, Evren Sirin
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to
//deal in the Software without restriction, including without limitation the
//rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
//sell copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in
//all copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
//FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
//IN THE SOFTWARE.

package org.mindswap.pellet.datatypes;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.xerces.impl.dv.XSSimpleType;
import org.apache.xerces.xs.StringList;
import org.apache.xerces.xs.XSFacet;
import org.apache.xerces.xs.XSImplementation;
import org.apache.xerces.xs.XSLoader;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSNamedMap;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTypeDefinition;
import org.mindswap.pellet.exceptions.UnsupportedFeatureException;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Namespaces;
import org.mindswap.pellet.utils.SetUtils;
import org.mindswap.pellet.utils.URIUtils;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;

import aterm.ATermAppl;
import aterm.ATermList;

/**
 * @author Evren Sirin
 * 
 */
public class DatatypeReasoner {
    public static boolean DEBUG = false;

    private Map uriToDatatype = new Hashtable();

    private Map datatypeToURI = new Hashtable();

    private Map termToDatatype = new Hashtable();

    private Map normalized = new Hashtable();

    private Map loadedSchemas = new HashMap();

    private int datatypeCount = 0;

    public DatatypeReasoner() {
        defineDatatype( Namespaces.RDFS + "Literal", RDFSLiteral.instance );
        defineDatatype( Namespaces.RDF + "XMLLiteral", RDFXMLLiteral.instance );

        // register built-in primitive types
        defineDatatype( Namespaces.XSD + "decimal", XSDDecimal.instance );
        defineDatatype( Namespaces.XSD + "string", XSDString.instance );
        defineDatatype( Namespaces.XSD + "boolean", XSDBoolean.instance );
        defineDatatype( Namespaces.XSD + "float", XSDFloat.instance );
        defineDatatype( Namespaces.XSD + "double", XSDDouble.instance );
        defineDatatype( Namespaces.XSD + "dateTime", XSDDateTime.instance );
        defineDatatype( Namespaces.XSD + "date", XSDDate.instance );
        defineDatatype( Namespaces.XSD + "time", XSDTime.instance );
        defineDatatype( Namespaces.XSD + "gYear", XSDYear.instance );
        defineDatatype( Namespaces.XSD + "gMonth", XSDMonth.instance );
        defineDatatype( Namespaces.XSD + "gDay", XSDDay.instance );
        defineDatatype( Namespaces.XSD + "gYearMonth", XSDYearMonth.instance );
        defineDatatype( Namespaces.XSD + "gMonthDay", XSDMonthDay.instance );
        defineDatatype( Namespaces.XSD + "duration", UnknownDatatype.instance );
        // defineDatatype(Namespaces.XSD + "hexBinary", BaseAtomicDatatype.instance);
        // defineDatatype(Namespaces.XSD + "base64Binary", BaseAtomicDatatype.instance);
        // defineDatatype(Namespaces.XSD + "QName", BaseAtomicDatatype.instance);
        // defineDatatype(Namespaces.XSD + "NOTATION", BaseAtomicDatatype.instance);
        defineDatatype( Namespaces.XSD + "anyURI", UnknownDatatype.instance );

        defineDatatype( Namespaces.XSD + "anySimpleType", XSDSimpleType.instance );

        // register built-in derived types
        XSDDecimal decimal = XSDDecimal.instance;
        
        ValueSpace valueSpace = decimal.getValueSpace();
        Object zero = valueSpace.getMidValue();

        XSDAtomicType integer = XSDDecimal.instance;
        defineDatatype( Namespaces.XSD + "integer", integer );
        
        XSDAtomicType nonPositiveInteger = integer.restrictMaxInclusive( zero );
        defineDatatype( Namespaces.XSD + "nonPositiveInteger", nonPositiveInteger );

        XSDAtomicType negativeInteger = nonPositiveInteger.restrictMaxExclusive( zero );        
        defineDatatype( Namespaces.XSD + "negativeInteger", negativeInteger );
        
        XSDAtomicType nonNegativeInteger = integer.restrictMinInclusive( zero );        
        defineDatatype( Namespaces.XSD + "nonNegativeInteger", nonNegativeInteger );
        
        XSDAtomicType positiveInteger = nonNegativeInteger.restrictMinExclusive( zero );        
        defineDatatype( Namespaces.XSD + "positiveInteger", positiveInteger );
        
        XSDAtomicType xsdLong = integer.
            restrictMinInclusive( new Long( Long.MIN_VALUE ) ).
            restrictMaxInclusive( new Long( Long.MAX_VALUE ) );        
        defineDatatype( Namespaces.XSD + "long", xsdLong );

        XSDAtomicType xsdInt = xsdLong.
            restrictMinInclusive( new Integer( Integer.MIN_VALUE ) ).
            restrictMaxInclusive( new Integer( Integer.MAX_VALUE ) );        
        defineDatatype( Namespaces.XSD + "int", xsdInt );
        
        XSDAtomicType xsdShort = xsdInt.
            restrictMinInclusive( new Short( Short.MIN_VALUE ) ).
            restrictMaxInclusive( new Short( Short.MAX_VALUE ) );        
        defineDatatype( Namespaces.XSD + "short", xsdShort );
        
        XSDAtomicType xsdByte = xsdShort.
            restrictMinInclusive( new Byte( Byte.MIN_VALUE ) ).
            restrictMaxInclusive( new Byte( Byte.MAX_VALUE ) );   
        defineDatatype( Namespaces.XSD + "byte", xsdByte );
                
        XSDAtomicType unsignedLong = nonNegativeInteger.restrictMaxInclusive( valueSpace.getValue( "18446744073709551615" ) );   
        defineDatatype( Namespaces.XSD + "unsignedLong", unsignedLong );

        XSDAtomicType unsignedInt = unsignedLong.restrictMaxInclusive( valueSpace.getValue( "4294967295" ) );   
        defineDatatype( Namespaces.XSD + "unsignedInt", unsignedInt );

        XSDAtomicType unsignedShort = unsignedInt.restrictMaxInclusive( valueSpace.getValue( "65535" ) );   
        defineDatatype( Namespaces.XSD + "unsignedShort", unsignedShort );
        
        XSDAtomicType unsignedByte = unsignedShort.restrictMaxInclusive( valueSpace.getValue( "255" ) );   
        defineDatatype( Namespaces.XSD + "unsignedByte", unsignedByte );

//        defineDatatype( Namespaces.XSD + "normalizedString", XSDString.instance );
//        defineDatatype( Namespaces.XSD + "token", XSDString.instance );
//        defineDatatype( Namespaces.XSD + "language", XSDString.instance );
//        defineDatatype( Namespaces.XSD + "NMTOKEN", XSDString.instance );
//        defineDatatype( Namespaces.XSD + "Name", XSDString.instance );
//        defineDatatype( Namespaces.XSD + "NCName", XSDString.instance );
    }

    final public Set getDatatypeURIs() {
        return datatypeToURI.keySet();
    }

    final public boolean isDefined( String datatypeURI ) {
        return uriToDatatype.containsKey( datatypeURI );
    }

    final public boolean isDefined( Datatype datatype ) {
        return datatypeToURI.containsKey( datatype );
    }

    public void defineDatatype( String name, Datatype dt ) {
        if( uriToDatatype.containsKey( name ) )
            throw new RuntimeException( name + " is already defined" );

        uriToDatatype.put( name, dt );
        datatypeToURI.put( dt, name );

        normalize( dt );
    }

    public void defineUnknownDatatype( String name ) {
        defineDatatype( name, new UnknownDatatype() );
    }

    public XSNamedMap parseXMLSchema( URL url ) throws Exception {
        if( DEBUG )
            System.out.println( "Parsing XML Schema " + url );

        // Use Xerces DOM Implementation
        System.setProperty( DOMImplementationRegistry.PROPERTY,
            "org.apache.xerces.dom.DOMXSImplementationSourceImpl " );
        // Get DOM Implementation Registry
        DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
        // Get DOM Implementation using DOM Registry
        DOMImplementationLS ls = (DOMImplementationLS) registry.getDOMImplementation( "LS" );
        // create input
        LSInput input = ls.createLSInput();
        input.setCharacterStream( new InputStreamReader( url.openStream() ) );

        // Get XS Implementation
        XSImplementation impl = (XSImplementation) registry.getDOMImplementation( "XS-Loader" );
        // Load XMLSchema
        XSLoader schemaLoader = impl.createXSLoader( null );
        XSModel schema = schemaLoader.load( input );
        // Get simple type definitions
        XSNamedMap map = schema.getComponents( XSTypeDefinition.SIMPLE_TYPE );

        return map;
    }

    public void loadUserDefinedDatatype( String name ) {
        try {
            if( uriToDatatype.containsKey( name ) )
                return;

            URL url = new URL( name );

            // Strip off fragment
            url = new URL( url.getProtocol(), url.getHost(), url.getPort(), url.getFile() );

            if( DEBUG )
                System.out.println( "Load " + url );

            XSNamedMap map = (XSNamedMap) loadedSchemas.get( url );
            if( map == null ) {
                map = parseXMLSchema( url );
                loadedSchemas.put( url, map );
            }

            String localName = URIUtils.getLocalName( name );
            String nameSpace = URIUtils.getNameSpace( name );

            // TODO Handle union types
            XSSimpleType simpleType = (XSSimpleType) map.itemByName( nameSpace, localName );
            if( simpleType == null ) {
                simpleType = (XSSimpleType) map.itemByName( "", localName );
                // if( simpleType == null ) {
                // for( int i = 0; true; i++ ) {
                // simpleType = (XSSimpleType) map.item( i );
                // if( simpleType.getName().equals( localName ) ) {
                // break;
                // }
                // }
                // }
            }
            // Print some info
            if( DEBUG )
                System.out.println( "Type: " + simpleType );
            if( DEBUG )
                System.out.println( "Base: " + simpleType.getBaseType() );
            if( DEBUG )
                System.out.println( "MinInclusive value: "
                    + simpleType.getLexicalFacetValue( XSSimpleType.FACET_MININCLUSIVE ) );
            if( DEBUG )
                System.out.println( "MaxInclusive value: "
                    + simpleType.getLexicalFacetValue( XSSimpleType.FACET_MAXINCLUSIVE ) );

            String baseType = Namespaces.XSD + simpleType.getBaseType().getName();
            XSDAtomicType xsdType = (XSDAtomicType) getDatatype( baseType );

            // loop over all the facets
            if( DEBUG )
                System.out.println( "Facets: " );
            XSObjectList facets = simpleType.getFacets();
            for( int i = 0; i < facets.getLength(); i++ ) {
                XSFacet facet = (XSFacet) facets.item( i );
                if( DEBUG )
                    System.out.println( i + ") Facet kind: " + facet.getFacetKind()
                        + " Facet name = " + facet.getName() + " Facet value: "
                        + facet.getLexicalFacetValue() );
                Object facetValue = xsdType.getValue( facet.getLexicalFacetValue(), baseType );
                xsdType = xsdType.deriveByRestriction( facet.getFacetKind(), facetValue );
            }

            // enumeration case
            StringList enumValues = simpleType.getLexicalEnumeration();
            if( enumValues != null && enumValues.getLength() > 0 ) {
                if( DEBUG ) {
                    for( int k = 0; k < enumValues.getLength(); k++ ) {
                        System.out.println( "enum: " + enumValues.item( k ) );
                    }
                }
                xsdType = xsdType.deriveByRestriction( XSSimpleTypeDefinition.FACET_ENUMERATION, enumValues );
            }

            defineDatatype( name, xsdType );
        }
        catch( IOException e ) {
            if( DEBUG )
                e.printStackTrace();
            System.err
                .println( "WARNING: Cannot load XML schema associated with the user-defined datatype "
                    + name );
            defineDatatype( name, new UnknownDatatype() );
        }
        catch( Exception e ) {
            if( DEBUG )
                e.printStackTrace();
            System.err
                .println( "WARNING: Cannot process the definition of the user-defined datatype "
                    + name );
            defineDatatype( name, new UnknownDatatype() );
        }
    }

    public String defineDatatype( Datatype dt ) {
        String name = (dt.getName() == null) ? "datatype" + datatypeCount++ : dt.getName()
            .getName();

        defineDatatype( name, dt );

        return name;
    }

    public void removeDatatype( String name ) {
        Datatype dt = getDatatype( name );

        // clean up the cached results
        uriToDatatype.remove( name );
        datatypeToURI.remove( dt );
        normalized.remove( dt );

        ATermAppl term = ATermUtils.makeTermAppl( name );
        termToDatatype.remove( term );
        ATermAppl not = ATermUtils.makeNot( term );
        termToDatatype.remove( not );
    }

    public Datatype getDatatype( String datatypeURI ) {
        if( datatypeURI == null || datatypeURI.length() == 0 )
            return XSDString.instance;// RDFSPlainLiteral.instance;
        else if( uriToDatatype.containsKey( datatypeURI ) )
            return (Datatype) uriToDatatype.get( datatypeURI );
        else
            return UnknownDatatype.instance;
    }

    public String getDatatypeURI( Datatype datatype ) {
        return (String) datatypeToURI.get( datatype );
    }

    public Object getValue( ATermAppl lit ) {
        if( !ATermUtils.isLiteral( lit ) )
            return null;

        String lexicalValue = ((ATermAppl) lit.getArgument( 0 )).getName();
        String lang = ((ATermAppl) lit.getArgument( 1 )).getName();
        String datatypeURI = ((ATermAppl) lit.getArgument( 2 )).getName();

        if( !lang.equals( "" ) && !datatypeURI.equals( "" ) )
            throw new UnsupportedFeatureException(
                "A literal value cannot have both a datatype URI " + "and a language identifier "
                    + lit );

        Datatype datatype = getDatatype( datatypeURI );
        if( lang.equals( "" ) )
            return datatype.getValue( lexicalValue, datatypeURI );
        else
            return new StringValue( lexicalValue, lang );
    }

    public Datatype singleton( ATermAppl term ) {
        ATermAppl lit = null;
        if( ATermUtils.isNominal( term ) )
            lit = (ATermAppl) term.getArgument( 0 );
        else if( ATermUtils.isLiteral( term ) )
            lit = term;
        else
            throw new RuntimeException( "An invalid data value is found " + term );

        String lexicalValue = ((ATermAppl) lit.getArgument( 0 )).getName();
        String lang = ((ATermAppl) lit.getArgument( 1 )).getName();
        String datatypeURI = ((ATermAppl) lit.getArgument( 2 )).getName();

        if( !lang.equals( "" ) && !datatypeURI.equals( "" ) )
            throw new UnsupportedFeatureException(
                "A literal value cannot have both a datatype URI " + "and a language identifier "
                    + lit );

        Datatype datatype = getDatatype( datatypeURI );
        Object value = lang.equals( "" )
            ? datatype.getValue( lexicalValue, datatypeURI )
            : new StringValue( lexicalValue, lang );

        return datatype.singleton( value );
    }

    public Datatype enumeration( Set values ) {
        Datatype[] enums = new Datatype[values.size()];
        Iterator i = values.iterator();
        for( int index = 0; index < enums.length; index++ )
            enums[index] = singleton( (ATermAppl) i.next() );

        return normalize( new BaseUnionDatatype( enums ) );
    }

    public Datatype getDatatype( ATermAppl datatypeTerm ) {
        Datatype datatype = (Datatype) termToDatatype.get( datatypeTerm );

        if( datatype != null )
            return datatype;
        else if( ATermUtils.isNominal( datatypeTerm ) )
            datatype = singleton( datatypeTerm );
        else if( ATermUtils.isNot( datatypeTerm ) ) {
            ATermAppl negatedDatatype = (ATermAppl) datatypeTerm.getArgument( 0 );

            // FIXME check if negatedDatatype is and(...)
            if( ATermUtils.isAnd( negatedDatatype ) ) {
                ATermList list = ATermUtils.negate( (ATermList) negatedDatatype.getArgument( 0 ) );
                datatype = enumeration( ATermUtils.listToSet( list ) );
            }
            else {
                Datatype norm = normalize( getDatatype( negatedDatatype ) );

                Set atomicTypes = ((UnionDatatype) normalized.get( RDFSLiteral.instance ))
                    .getMembers();
                atomicTypes = SetUtils.create( atomicTypes );
                if( norm instanceof AtomicDatatype ) {
                    AtomicDatatype atomicType = (AtomicDatatype) norm;
                    AtomicDatatype primitiveType = atomicType.getPrimitiveType();

                    atomicTypes.remove( primitiveType );

                    AtomicDatatype not = atomicType.not();
                    if( !not.isEmpty() )
                        atomicTypes.add( not );
                }
                else if( norm instanceof UnionDatatype )
                    atomicTypes.removeAll( ((UnionDatatype) norm).getMembers() );
                else
                    throw new RuntimeException( "Error in datatype reasoning" );

                Datatype[] members = new Datatype[atomicTypes.size()];
                atomicTypes.toArray( members );
                datatype = new BaseUnionDatatype( members );
            }
        }
        else
            datatype = getDatatype( datatypeTerm.getName() );

        if( datatype == null )
            datatype = new UnknownDatatype();

        termToDatatype.put( datatypeTerm, datatype );

        return datatype;
    }

    private Datatype normalize( Datatype datatype ) {
        Datatype norm = (Datatype) normalized.get( datatype );

        if( norm != null )
            return norm;
        else if( datatype instanceof UnionDatatype ) {
            Map groupedTypes = new HashMap();
            UnionDatatype union = (UnionDatatype) datatype;
            for( Iterator i = union.getMembers().iterator(); i.hasNext(); ) {
                Datatype member = (Datatype) i.next();
                Datatype normalizedMember = normalize( member );

                groupedTypes = unionWithGroup( groupedTypes, normalizedMember );

                // if(normalizedMember instanceof UnionDatatype)
                // allAtomicMembers.addAll(((UnionDatatype) normalizedMember).getMembers());
                // else
                // allAtomicMembers.add(member);
            }
            Datatype[] datatypes = new Datatype[groupedTypes.size()];
            groupedTypes.values().toArray( datatypes );
            if( datatypes.length == 1 )
                norm = datatypes[0];
            else
                norm = new BaseUnionDatatype( datatypes );
        }
        else
            norm = datatype;

        normalized.put( datatype, norm );

        return norm;
    }

    /**
     * Check if a datatype is subsumed by another datatype
     * 
     * @param d1
     * @param d2
     * @return
     */
    public boolean isSubTypeOf( ATermAppl d1, ATermAppl d2 ) {
        ATermAppl notD2 = ATermUtils.makeNot( d2 );
        Datatype conjunction = intersection( new ATermAppl[] { d1, notD2 } );

        return conjunction.isEmpty();
    }

    /**
     * Return a datatype that represents the intersection of a set of (possibly negated) datatypes.
     * 
     * @param datatypeTerms
     * @return
     */
    public Datatype intersection( ATermAppl[] datatypeTerms ) {
        if( datatypeTerms.length == 0 )
            return EmptyDatatype.instance;
        else if( datatypeTerms.length == 1 && ATermUtils.isPrimitive( datatypeTerms[0] ) )
            return getDatatype( datatypeTerms[0] );

        ATermList list = ATermUtils.makeList( datatypeTerms );
        ATermAppl and = ATermUtils.normalize( ATermUtils.makeAnd( list ) );
        Datatype intersection = (Datatype) termToDatatype.get( and );
        if( intersection != null )
            return intersection;

        Datatype[] datatypes = null;
        if( ATermUtils.isAnd( and ) ) {
            list = (ATermList) and.getArgument( 0 );
            datatypes = new Datatype[list.getLength()];
            for( int i = 0; !list.isEmpty(); list = list.getNext() )
                datatypes[i++] = getDatatype( (ATermAppl) list.getFirst() );
        }
        else {
            datatypes = new Datatype[1];
            datatypes[0] = getDatatype( (ATermAppl) list.getFirst() );
        }

        // Datatype[] datatypes = new Datatype[datatypeTerms.length];
        // for(int i = 0; i < datatypeTerms.length; i++)
        //			datatypes[i] = getDatatype(datatypeTerms[i]);			

        Map groupedTypes = new HashMap();
        // TODO initialize the groupedTypes with the first datatype in the array
        Set atomicTypes = ((UnionDatatype) normalized.get( RDFSLiteral.instance )).getMembers();
        for( Iterator i = atomicTypes.iterator(); i.hasNext(); ) {
            AtomicDatatype primitiveType = (AtomicDatatype) i.next();
            groupedTypes.put( primitiveType, primitiveType );
        }

        for( int i = 0; i < datatypes.length; i++ )
            groupedTypes = intersectWithGroup( groupedTypes, datatypes[i] );

        if( groupedTypes.size() == 1 )
            intersection = (Datatype) groupedTypes.values().iterator().next();
        else
            intersection = new BaseUnionDatatype( new HashSet( groupedTypes.values() ) );
        termToDatatype.put( and, intersection );

        if( DEBUG && intersection.isEmpty() )
            intersection = intersection( datatypeTerms );

        return intersection;
    }

    private Map intersectWithGroup( Map groupedTypes, Datatype datatype ) {
        Map newGroup = new HashMap();
        if( datatype instanceof AtomicDatatype ) {
            AtomicDatatype atomicType = (AtomicDatatype) datatype;
            AtomicDatatype primitiveType = atomicType.getPrimitiveType();
            AtomicDatatype type = (AtomicDatatype) groupedTypes.get( primitiveType );
            if( type != null ) {
                type = type.intersection( atomicType );
                newGroup.put( primitiveType, type );
            }
        }
        else if( datatype instanceof UnionDatatype ) {
            UnionDatatype union = (UnionDatatype) datatype;
            for( Iterator i = union.getMembers().iterator(); i.hasNext(); ) {
                Datatype member = (Datatype) i.next();

                newGroup.putAll( intersectWithGroup( groupedTypes, member ) );
            }
        }
        else
            throw new RuntimeException( "Error in datatype reasoning" );

        return newGroup;
    }

    private Map unionWithGroup( Map groupedTypes, Datatype datatype ) {
        Map newGroup = groupedTypes;
        if( datatype instanceof AtomicDatatype ) {
            AtomicDatatype atomicType = (AtomicDatatype) datatype;
            AtomicDatatype primitiveType = atomicType.getPrimitiveType();
            AtomicDatatype type = (AtomicDatatype) groupedTypes.get( primitiveType );
            if( type == null )
                type = atomicType;
            else
                type = type.union( atomicType );

            newGroup.put( primitiveType, type );
        }
        else if( datatype instanceof UnionDatatype ) {
            UnionDatatype union = (UnionDatatype) datatype;
            for( Iterator i = union.getMembers().iterator(); i.hasNext(); ) {
                Datatype member = (Datatype) i.next();

                newGroup = unionWithGroup( groupedTypes, member );
            }
        }
        else
            throw new RuntimeException( "Error in datatype reasoning" );

        return newGroup;
    }
}
