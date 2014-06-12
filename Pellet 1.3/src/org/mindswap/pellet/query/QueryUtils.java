/*
 * Created on Jan 8, 2005
 */
package org.mindswap.pellet.query;

import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.QNameProvider;

import aterm.ATermAppl;

import com.hp.hpl.jena.query.Syntax;

/**
 * @author Evren Sirin
 *
 */
public class QueryUtils {
   public static String getVarName( ATermAppl term ) {
       if( ATermUtils.isVar( term ) )
           return ((ATermAppl)term.getArgument(0)).getName();
       
       return null;
   }
   
   public static String formatTerm( ATermAppl term ) {
       return formatTerm( term, null );
   }
   
   public static String formatTerm( ATermAppl term, QNameProvider qnames ) {
	   StringBuffer sb = new StringBuffer();
	   if( term == null )
	       sb.append("<null>");	   
       else if( ATermUtils.isVar( term ) )
           sb.append("?").append(((ATermAppl)term.getArgument(0)).getName());
       else if( ATermUtils.isLiteral( term ) ) {		
           String value = ((ATermAppl) term.getArgument(0)).getName();
           String lang = ((ATermAppl) term.getArgument(1)).getName();
           String datatypeURI = ((ATermAppl) term.getArgument(2)).getName();
                      
           sb.append('"').append(value).append('"');
           if(!lang.equals("")) 
               sb.append('@').append(lang);
           else if(!datatypeURI.equals(""))
               sb.append("^^").append(datatypeURI);
                   
                     
       }
       else if( qnames != null )
           sb.append(qnames.shortForm(term.getName()));
       else
           sb.append(term.getName());
	   
	   return sb.toString(); 
	}

    /**
     * @deprecated Use {@link QueryEngine#parse(String, KnowledgeBase)} instead
     */
   public static Query parse( String queryStr, KnowledgeBase kb ) {
	    return QueryEngine.parse( queryStr, kb, Syntax.syntaxRDQL );	
   }
}
