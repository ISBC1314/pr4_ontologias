package org.mindswap.pellet.dig;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.xerces.dom.DocumentImpl;
import org.mindswap.pellet.utils.ATermUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import aterm.ATermAppl;

/*
 * Created on Jul 17, 2005
 */

/**
 * @author Evren Sirin
 *
 */
public class DIGResponse {
    private Document doc;
    private Element root;    
    
    public DIGResponse( String rootTag ) {
        doc = new DocumentImpl();
        
        root = doc.createElement( rootTag );
        
        root.setAttribute( "xmlns", DIGConstants.NAMESPACE );
        root.setAttribute( "xmlns:xsi", DIGConstants.XSI );
        root.setAttributeNS( DIGConstants.XSI, "xsi:schemaLocation",
            DIGConstants.NAMESPACE + "      " + DIGConstants.SCHEMA );
        
        doc.appendChild( root );
    }
    
    public Document getDocument() {
        return doc;
    }
    
    protected Element addElement( String tag ) {
        return addElement( tag, root );
    }
    
    protected Element addElement( String tag, Element parent ) {
        Element element = doc.createElement( tag );
        parent.appendChild( element );      
        
        return element;
    }
    
    public static Document createOkResponse() {
        DIGResponse resp = new DIGResponse( DIGConstants.RESPONSE );        
        resp.addElement( DIGConstants.OK );
        
        return resp.getDocument();
    }
    
    public static Document createKBResponse( String uri ) {
        DIGResponse resp = new DIGResponse( DIGConstants.RESPONSE );        
        Element kb = resp.addElement( DIGConstants.KB );
        kb.setAttribute( DIGConstants.URI, uri);
        
        return resp.getDocument();
    }
    
    public static Document createErrorResponse( int code, String details ) {
        DIGResponse resp = new DIGResponse( DIGConstants.RESPONSE );        
        resp.addError( code, details );
        
        return resp.getDocument();
    }
    
    public Element addError( int code, String details ) {
        Element error = addElement( DIGConstants.ERROR );
        error.setAttribute( DIGConstants.CODE, DIGErrors.codes[ 2*code ] );
        error.setAttribute( DIGConstants.MESSAGE, DIGErrors.codes[ 2*code+1 ] );
        
        if( details != null )
            error.appendChild( doc.createTextNode( details ) );
        
        return error;
    }    

    public Element addBoolean( boolean b ) {
        return addElement( b ? "true" : "false" );
    }    

    public Element addConceptSet( Collection concepts ) {
        return addSet( concepts, DIGConstants.CONCEPT_SET, DIGConstants.CATOM );
    }
    
    public Element addRoleSet( Collection roles, boolean attribute ) {
        return addSet( roles, DIGConstants.ROLE_SET, 
            attribute ? DIGConstants.ATTRIBUTE : DIGConstants.RATOM );
    }
    
    public Element addIndividualSet( Collection individuals ) {
        Element set = addElement( DIGConstants.INDIVIDUAL_SET );
        
        for(Iterator i = individuals.iterator(); i.hasNext();) {
            ATermAppl individual = (ATermAppl) i.next();
            
            Element element = addElement( DIGConstants.INDIVIDUAL, set);
            element.setAttribute( DIGConstants.NAME, individual.getName() );
        }         
        
        return set;        
    }

    public void addValues( Collection values, String id ) {
        for(Iterator i = values.iterator(); i.hasNext();) {
            ATermAppl literal = (ATermAppl) i.next();

		    String value = literal.getArgument(0).toString();
		    String datatypeURI = literal.getArgument(2).toString();				    
            
            boolean isInt = !datatypeURI.equals( "" );
            
            Element val = addElement( isInt ? DIGConstants.IVAL : DIGConstants.SVAL );
            val.appendChild( doc.createTextNode( value ) );
            
            val.setAttribute( DIGConstants.ID, id );
        }             
    }
    
    public Element addIndividualPairSet( Map values ) {
        Element set = addElement( DIGConstants.INDIVIDUAL_PAIR_SET );
        
        for(Iterator i = values.keySet().iterator(); i.hasNext();) {
            ATermAppl subj = (ATermAppl) i.next();
            
            Iterator objects = ((Collection) values.get( subj )).iterator();
            while( objects.hasNext() ) {
                ATermAppl obj = (ATermAppl) objects.next();
                
	            Element indPair = addElement( DIGConstants.INDIVIDUAL_PAIR, set);

	            Element subjElement = addElement( DIGConstants.INDIVIDUAL, indPair );
	            subjElement.setAttribute( DIGConstants.NAME, subj.getName() );
	            
	            Element objElement = addElement( DIGConstants.INDIVIDUAL, indPair );
	            objElement.setAttribute( DIGConstants.NAME, obj.getName() );	            
            }
        }         
        
        return set;        
    }

    private Element addSet( Collection values, String tag, String atom ) {
        Element set = addElement( tag );
        
        for(Iterator i = values.iterator(); i.hasNext();) {
            Set synonms = (Set) i.next();
            
            addSynonms( set, synonms, atom );
        }         
        
        return set;        
        
    }

    public void addSynonms( Element parent, Collection elements, String atom ) {
        Element synonms = addElement( DIGConstants.SYNONYMS, parent );
        for(Iterator i = elements.iterator(); i.hasNext();) {
            ATermAppl term = (ATermAppl) i.next();
            
            if( term.equals( ATermUtils.TOP ) ) 
	            addElement( DIGConstants.TOP, synonms );
            else if( term.equals( ATermUtils.BOTTOM ) )
	            addElement( DIGConstants.BOTTOM, synonms );
            else {
	            Element element = addElement( atom, synonms );
	            element.setAttribute( DIGConstants.NAME, term.getName() );
            }
        }
    }
}
