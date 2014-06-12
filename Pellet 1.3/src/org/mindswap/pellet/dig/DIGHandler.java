// The MIT License
//
// Copyright (c) 2003 Ron Alford, Mike Grove, Bijan Parsia, Evren Sirin
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to
// deal in the Software without restriction, including without limitation the
// rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
// sell copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
// IN THE SOFTWARE.

package org.mindswap.pellet.dig;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Namespaces;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import aterm.ATermAppl;
import aterm.ATermList;

/*
 * Created on Jul 17, 2005
 */

/**
 * @author Evren Sirin
 *
 */
public class DIGHandler extends DIGConstants {   
    protected static Log log = LogFactory.getLog( PelletDIGServer.class );
    
    protected KnowledgeBase kb;
    
    public DIGHandler() {        
        kb = null;
    }
    
    public KnowledgeBase getKB() {
        return kb;
    }
    
    public void setKB( KnowledgeBase kb ) {
        this.kb = kb;
    }
    
    public static String getAttributeValue( Element node, String name ) {
        return node.getAttribute( name );
    }
    
    public static Element getElement( Element node ) {
        Element[] nodes = getElementArray( node );
        return ( nodes.length > 0 ) ? nodes[0] : null;
    }

    public static Element[] getElementArray( Element node ) {
        return new ElementList( node.getChildNodes() ).getNodeArray();
    }
    
    public static ElementList getElements( Element node ) {
        return new ElementList( node.getChildNodes() );
    }
    
    public static ElementList getElements( Element node, String tagName ) {
        return new ElementList( node.getElementsByTagName( tagName ) );
    }

    public static String getTagName( Element node ) {
        return node.getTagName();
    }
    
    public static String getURI( Element node ) {
        return getAttributeValue( node, DIGConstants.URI );
    }
    
    public static String getName( Element node ) {
        return getAttributeValue( node, DIGConstants.NAME );
    }
    
    public static int getNum( Element node ) {
        return Integer.parseInt( getAttributeValue( node, DIGConstants.NUM ) );
    }

    public static int getIntVal( Element node ) {
        return Integer.parseInt( getAttributeValue( node, DIGConstants.VAL ) );
    }
    
    public static String getVal( Element node ) {
        return getAttributeValue( node, DIGConstants.VAL );
    }
    
    public static ATermAppl getNameTerm( Element node ) {
        return ATermUtils.makeTermAppl( getAttributeValue( node, DIGConstants.NAME ) );
    }
    
    public static String getId( Element node ) {
        return getAttributeValue( node, DIGConstants.ID );
    }
    
    public static ATermAppl getInverse( Element node ) {
        if( getTagName( node ).equals( DIGConstants.INVERSE ) )
            return property( getElement( node ) );
        
        return null;
    }
    
    public static ATermAppl literal( Element node ) {
        String type = getTagName( node );
        
        ATermAppl term = null;
        if( type.equals( DIGConstants.SVAL ) ) {
            String val = node.getFirstChild().getNodeValue();
            term = ATermUtils.makePlainLiteral( val );
        }
        else if( type.equals( DIGConstants.IVAL ) ) {
            String val = node.getFirstChild().getNodeValue();
            term = ATermUtils.makeTypedLiteral( val, Namespaces.XSD + "int" );
        }
        else
            throw new RuntimeException( "Invalid data value constructor " + type );
        
        return term;
    }
    
    public static ATermAppl property( Element node ) {
        String type = getTagName( node );
        
        ATermAppl term = null;
        if( type.equals( DIGConstants.RATOM ) ) {
            term = getNameTerm( node );
        }
        else if( type.equals( DIGConstants.ATTRIBUTE ) ) {
            term = getNameTerm( node );
        }
        else if( type.equals( DIGConstants.INVERSE ) ) 
            throw new RuntimeException( "<inverse> element can only be used inside <equalr>" );
        else if( type.equals( DIGConstants.FEATURE ) ) 
            throw new RuntimeException( "<feature> nost supported" );
        else {
            try {
                term = getNameTerm( node );
                log.error( type + " is not a valid role constructor" );
            } catch(Exception e) {
                throw new RuntimeException( "Invalid role constructor: " + type );
            }
        }
        
        return term;
    }
    
    public static ATermAppl individual( Element c ) {
        return getNameTerm( c );
    }
    
    public ATermAppl concept( Element c ) {
        String type = getTagName( c );
        
        ATermAppl term = null;
        if( type.equals( DIGConstants.TOP ) ) {
            term = ATermUtils.TOP;
        }
        else if( type.equals( DIGConstants.BOTTOM ) ) {
            term = ATermUtils.BOTTOM;
        }
        else if( type.equals( DIGConstants.CATOM ) ) {
            term = getNameTerm( c );
        }
        else if( type.equals( DIGConstants.AND ) ) {
            ElementList nodes = getElements( c );

            ATermList list = ATermUtils.EMPTY_LIST;
            for(int i = nodes.getLength() - 1; i >= 0; i--) {
                Element node = nodes.item( i );
                
                list = list.append( concept( node ) );
            }
            
            term = ATermUtils.makeAnd( list );
        }
        else if( type.equals( DIGConstants.OR ) ) {
            ElementList nodes = getElements( c );

            ATermList list = ATermUtils.EMPTY_LIST;
            for(int i = nodes.getLength() - 1; i >= 0; i--) {
                Element node = nodes.item( i );
                
                list = list.append( concept( node ) );
            }
            
            term = ATermUtils.makeOr( list );
        }
        else if( type.equals( DIGConstants.NOT ) ) {
            Element node = getElement( c );
            
            term = ATermUtils.makeNot( concept( node ) );
        }        
        else if( type.equals( DIGConstants.ALL ) ) {
            ElementList nodes = getElements( c );
            
            ATermAppl prop = property( nodes.item( 0 ) ) ;
            ATermAppl allValues = concept( nodes.item( 1 ) );
            
            term = ATermUtils.makeAllValues( prop, allValues );
        }
        else if( type.equals( DIGConstants.SOME ) ) {
            ElementList nodes = getElements( c );
            
            ATermAppl prop = property( nodes.item( 0 ) ) ;
            ATermAppl someValues = concept( nodes.item( 1 ) );
            
            term = ATermUtils.makeSomeValues( prop, someValues );
        }
        else if( type.equals( DIGConstants.ATMOST ) ) {
            ElementList nodes = getElements( c );
            
            int num = getNum( c );
            ATermAppl prop = property( nodes.item( 0 ) ) ;
            ATermAppl top = concept( nodes.item( 1 ) );
            
            if( !top.equals( ATermUtils.TOP ) )
                throw new RuntimeException( "Qualified number restrictions are not allowed!" );
            
            term = ATermUtils.makeMax( prop, num );
        }
        else if( type.equals( DIGConstants.ATLEAST ) ) {
            ElementList nodes = getElements( c );
            
            int num = getNum( c );
            ATermAppl prop = property( nodes.item( 0 ) ) ;
            ATermAppl top = concept( nodes.item( 1 ) );
            
            if( !top.equals( ATermUtils.TOP ) )
                throw new RuntimeException( "Qualified number restrictions are not allowed!" );
            
            term = ATermUtils.makeMin( prop, num );
        }
        else if( type.equals( DIGConstants.ISET ) ) {
            ElementList nodes = getElements( c );

            ATermList list = ATermUtils.EMPTY_LIST;
            for(int i = nodes.getLength() - 1; i >= 0; i--) {
                Element node = nodes.item( i );
                
                list = list.append( ATermUtils.makeValue( individual( node ) ) );
            }
            
            term = ATermUtils.makeOr( list );
        }
        else if( type.equals( DIGConstants.INTEQUALS ) ) {
            Element node = getElement( c );
            
            String val = String.valueOf( getIntVal( c ) );
            ATermAppl prop = property( node ) ;

            ATermAppl value = ATermUtils.makeTypedLiteral( val, Namespaces.XSD + "int" );
            
            term = ATermUtils.makeHasValue( prop, value );
        }       
        else if( type.equals( DIGConstants.STRINGEQUALS ) ) {
            Element node = getElement( c );
            
            String val = getVal( c );
            ATermAppl prop = property( node ) ;

            ATermAppl value = ATermUtils.makePlainLiteral( val );
            
            term = ATermUtils.makeHasValue( prop, value );
        }
        else
            throw new RuntimeException( "Invalid concept constructor " + type );
        
        return term;
    }

    public String serialize( Document doc ) {
        return serialize( doc.getDocumentElement() );
    }    
    
    public String serialize( Element el ) {
        try {
	        StringWriter out = new StringWriter();
	        Document doc = el.getOwnerDocument();
	        OutputFormat format  = new OutputFormat( doc ); 
	        format.setIndenting( true );
	        format.setLineWidth( 0 );             
	        format.setPreserveSpace( false );
	        format.setOmitXMLDeclaration( false );
	        
	        XMLSerializer serial = new XMLSerializer( out, format );
            serial.asDOMSerializer();                            

            serial.serialize( el );
            
            return out.toString();
        }
        catch( IOException e ) {
            log.error( "Problem serializing element " + e );
            
            return "Problem serializing element " + e;
        }
    } 
}
