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

package org.mindswap.pellet;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.datatypes.Datatype;
import org.mindswap.pellet.datatypes.DatatypeReasoner;
import org.mindswap.pellet.datatypes.XSDAtomicType;
import org.mindswap.pellet.exceptions.UnsupportedFeatureException;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Namespaces;
import org.mindswap.pellet.utils.SetUtils;

import aterm.ATerm;
import aterm.ATermAppl;
import aterm.ATermList;

/**
 * Parse files written in KRSS format and loads into the given KB.
 * 
 * @author Evren Sirin
 */
public class KRSSLoader {
    protected static Log log = LogFactory.getLog( KRSSLoader.class );

    /**
     * @deprecated Edit log4j.properties to turn on debugging
     */
    public static boolean DEBUG = false;

    private StreamTokenizer in;

    private KnowledgeBase kb;
    
    private XSDAtomicType xsdInteger;

    private ArrayList terms;

    private Map disjoints;

    private boolean forceUppercase;

    public KRSSLoader() {
        forceUppercase = false;
    }

    public boolean isForceUppercase() {
        return forceUppercase;
    }

    public void setForceUppercase( boolean forceUppercase ) {
        this.forceUppercase = forceUppercase;
    }

    private void initTokenizer( Reader reader ) {
        in = new StreamTokenizer( reader );
        in.lowerCaseMode( false );
        in.commentChar( ';' );
        in.wordChars( '/', '/' );
        in.wordChars( '_', '_' );
        in.wordChars( '*', '*' );
        in.wordChars( '?', '?' );
        in.wordChars( '%', '%' );
        in.wordChars( '>', '>' );
        in.wordChars( '<', '<' );
        in.wordChars( '=', '=' );
        in.quoteChar( '|' );
    }
    
    private void skipNext() throws IOException {
        in.nextToken();
    }
    
    private void skipNext( int token ) throws IOException {
        ATermUtils.assertTrue( token == in.nextToken() );
    }

    private void skipNext( String token ) throws IOException {
        in.nextToken();
        ATermUtils.assertTrue( token.equals( in.sval ) );
    }

    private boolean peekNext( int token ) throws IOException {
        int next = in.nextToken();
        in.pushBack();
        return (token == next);
    }

    private String nextString() throws IOException {
        in.nextToken();

        return in.sval;
    }

    private int nextInt() throws IOException {
        in.nextToken();

        return (int) in.nval;
    }

    private Object nextNumber() throws IOException {
        in.nextToken();

        String strVal = String.valueOf( (long) in.nval );
        return xsdInteger.getValue( strVal, null );
    }

    private ATermAppl nextTerm() throws IOException {
        String token = nextString();
        if( forceUppercase ) token = token.toUpperCase();
        return ATermUtils.makeTermAppl( token );
    }

    private ATermAppl[] parseExprList() throws IOException {
        int count = 0;
        while( peekNext( '(' ) ) {
            skipNext();
            count++;
        }

        List terms = new ArrayList();
        while( true ) {
            if( peekNext( ')' ) ) {
                if( count == 0 ) break;
                skipNext();
                count--;
                if( count == 0 ) break;
            }
            else if( peekNext( '(' ) ) {
                skipNext();
                count++;
            }
            else
                terms.add( parseExpr() );
        }

        return (ATermAppl[]) terms.toArray( new ATermAppl[terms.size()] );
    }

    private ATermAppl parseExpr() throws IOException {
        ATermAppl a = null;

        int token = in.nextToken();
        String s = in.sval;
        if( token == ':' ) {
            s = nextString();
            if( s.equalsIgnoreCase( "TOP" ) )
                a = ATermUtils.TOP;
            else if( s.equalsIgnoreCase( "BOTTOM" ) )
                a = ATermUtils.BOTTOM;
            else
                throw new RuntimeException( "Parse exception after ':' " + s );
        }
        else if( token == '(' ) {
            token = in.nextToken();
            ATermUtils.assertTrue( token == StreamTokenizer.TT_WORD );

            s = in.sval;
            if( s.equalsIgnoreCase( "NOT" ) ) {
                ATermAppl c = parseExpr();
                a = ATermUtils.makeNot( c );

                if( ATermUtils.isPrimitive( c ) ) kb.addClass( c );
            }
            else if( s.equalsIgnoreCase( "AND" ) ) {
                ATermList list = ATermUtils.EMPTY_LIST;

                while( !peekNext( ')' ) ) {
                    ATermAppl c = parseExpr();

                    if( ATermUtils.isPrimitive( c ) ) kb.addClass( c );
                    list = list.insert( c );
                }
                a = ATermUtils.makeAnd( list );
            }
            else if( s.equalsIgnoreCase( "OR" ) ) {
                ATermList list = ATermUtils.EMPTY_LIST;

                while( !peekNext( ')' ) ) {
                    ATermAppl c = parseExpr();

                    if( ATermUtils.isPrimitive( c ) ) kb.addClass( c );
                    list = list.insert( c );
                }
                a = ATermUtils.makeOr( list );
            }
            else if( s.equalsIgnoreCase( "ONE-OF" ) ) {
                ATermList list = ATermUtils.EMPTY_LIST;

                while( !peekNext( ')' ) ) {
                    ATermAppl c = parseExpr();

                    kb.addIndividual( c );
                    list = list.insert( ATermUtils.makeValue( c ) );
                }
                a = ATermUtils.makeOr( list );
            }
            else if( s.equalsIgnoreCase( "ALL" ) ) {
                ATermAppl r = parseExpr();
                kb.addObjectProperty( r );
                ATermAppl c = parseExpr();
                if( ATermUtils.isPrimitive( c ) ) kb.addClass( c );

                a = ATermUtils.makeAllValues( r, c );
            }
            else if( s.equalsIgnoreCase( "SOME" ) ) {
                ATermAppl r = parseExpr();
                kb.addObjectProperty( r );
                ATermAppl c = parseExpr();
                if( ATermUtils.isPrimitive( c ) ) kb.addClass( c );
                a = ATermUtils.makeSomeValues( r, c );
            }
            else if( s.equalsIgnoreCase( "AT-LEAST" ) || s.equalsIgnoreCase( "ATLEAST" ) ) {
                int n = nextInt();
                ATermAppl r = parseExpr();
                kb.addObjectProperty( r );

                a = ATermUtils.makeMin( r, n );
            }
            else if( s.equalsIgnoreCase( "AT-MOST" ) || s.equalsIgnoreCase( "ATMOST" ) ) {
                int n = nextInt();
                ATermAppl r = parseExpr();
                kb.addObjectProperty( r );
                a = ATermUtils.makeMax( r, n );
            }
            else if( s.equalsIgnoreCase( "A" ) ) {
                ATermAppl r = nextTerm();
                // TODO what does term 'A' stand for
                kb.addProperty( r );
                kb.addFunctionalProperty( r );
                a = ATermUtils.makeMin( r, 1 );
            }
            else if( s.equalsIgnoreCase( "MIN" ) || s.equals( ">=" ) ) {
                ATermAppl r = nextTerm();
                kb.addDatatypeProperty( r );
                Object val = nextNumber();
                DatatypeReasoner dtReasoner = kb.getDatatypeReasoner();
                Datatype dt = xsdInteger.restrictMinInclusive( val );
                String dtName = dtReasoner.defineDatatype( dt );
                ATermAppl datatype = ATermUtils.makeTermAppl( dtName );
                a = ATermUtils.makeAllValues( r, datatype );
            }
            else if( s.equalsIgnoreCase( "MAX" ) || s.equals( "<=" ) ) {
                ATermAppl r = nextTerm();
                kb.addDatatypeProperty( r );
                Object val = nextNumber();
                DatatypeReasoner dtReasoner = kb.getDatatypeReasoner();
                Datatype dt = xsdInteger.restrictMaxInclusive( val );
                String dtName = dtReasoner.defineDatatype( dt );
                ATermAppl datatype = ATermUtils.makeTermAppl( dtName );
                a = ATermUtils.makeAllValues( r, datatype );
            }
            else if( s.equals( "=" ) ) {
                ATermAppl r = nextTerm();
                kb.addDatatypeProperty( r );
                Object val = nextNumber();
                DatatypeReasoner dtReasoner = kb.getDatatypeReasoner();
                Datatype dt = xsdInteger.singleton( val ); 
                String dtName = dtReasoner.defineDatatype( dt );
                ATermAppl datatype = ATermUtils.makeTermAppl( dtName );
                a = ATermUtils.makeAllValues( r, datatype );
            }
            else if( s.equalsIgnoreCase( "EXACTLY" ) ) {
                int n = nextInt();
                ATermAppl r = parseExpr();
                kb.addObjectProperty( r );
                a = ATermUtils.makeAnd( ATermUtils.makeMax( r, n ), ATermUtils.makeMin( r, n ) );
            }
            else if( s.equalsIgnoreCase( "INV" ) ) {
                ATermAppl r = parseExpr();
                kb.addObjectProperty( r );
                a = kb.getProperty( r ).getInverse().getName();
            }
            else {
                throw new RuntimeException( "Unknown expression " + s );
            }

            if( in.nextToken() != ')' ) {
                if( s.equalsIgnoreCase( "AT-LEAST" ) || s.equalsIgnoreCase( "AT-MOST" )
                    || s.equalsIgnoreCase( "ATLEAST" ) || s.equalsIgnoreCase( "ATMOST" ) ) {
                    s = nextString();
                    if( s.equalsIgnoreCase( "TOP" ) || s.equalsIgnoreCase( "*TOP*" )
                        || s.equalsIgnoreCase( ":TOP" ) )
                        skipNext( ')' );
                    else
                        throw new UnsupportedFeatureException( "Qualified cardinality restrictions" );
                }
                else
                    throw new RuntimeException( "Parse exception at term " + s );
            }
        }
        else if( token == '#' ) {
            int n = nextInt();
            if( peekNext( '#' ) ) {
                skipNext();
                a = (ATermAppl) terms.get( n );
                if( a == null )
                    throw new RuntimeException( "Parse exception: #" + n + "# is not defined" );
            }
            else {
                skipNext( "=" );
                a = parseExpr();

                while( terms.size() <= n )
                    terms.add( null );

                terms.set( n, a );
            }
        }
        else if( token == StreamTokenizer.TT_EOF )
            a = null;
        else if( s.equalsIgnoreCase( "TOP" ) || s.equalsIgnoreCase( "*TOP*" )
            || s.equalsIgnoreCase( ":TOP" ) )
            a = ATermUtils.TOP;
        else if( s.equalsIgnoreCase( "BOTTOM" ) || s.equalsIgnoreCase( "*BOTTOM*" ) )
            a = ATermUtils.BOTTOM;
        else {
            if( forceUppercase ) s = s.toUpperCase();
            a = ATermUtils.makeTermAppl( s );
        }

        return a;
    }

    public void load( Reader reader, KnowledgeBase kb ) throws IOException {
        this.kb = kb;
        this.xsdInteger = (XSDAtomicType) kb.getDatatypeReasoner().getDatatype( Namespaces.XSD + "integer" );

        initTokenizer( reader );

        terms = new ArrayList();
        disjoints = new HashMap();

        int token = in.nextToken();
        while( token != StreamTokenizer.TT_EOF ) {
            if( token == '#' ) {
                in.ordinaryChar( '|' );
                token = in.nextToken();
                while( token != '#' )
                    token = in.nextToken();
                in.quoteChar( '|' );
                token = in.nextToken();
                if( token == StreamTokenizer.TT_EOF ) break;
            }
            if( token != '(' )
                throw new RuntimeException( "Expecting '(' but found " + in );

            String str = nextString();
            if( str.equalsIgnoreCase( "DEFINE-PRIMITIVE-ROLE" )
                || str.equalsIgnoreCase( "DEFPRIMROLE" )
                || str.equalsIgnoreCase( "DEFINE-PRIMITIVE-ATTRIBUTE" )
                || str.equalsIgnoreCase( "DEFPRIMATTRIBUTE" )
                || str.equalsIgnoreCase( "DEFINE-DATATYPE-PROPERTY" ) ) {
                ATermAppl r = nextTerm();

                boolean dataProp = str.equalsIgnoreCase( "DEFINE-DATATYPE-PROPERTY" );
                boolean functional = str.equalsIgnoreCase( "DEFINE-PRIMITIVE-ATTRIBUTE" )
                    || str.equalsIgnoreCase( "DEFPRIMATTRIBUTE" );

                if( dataProp ) {
                    kb.addDatatypeProperty( r );
                    if( log.isDebugEnabled() ) log.debug( "DEFINE-DATATYPE-ROLE " + r );
                }
                else {
                    kb.addObjectProperty( r );
                    if( functional ) {
                        kb.addFunctionalProperty( r );
                        if( log.isDebugEnabled() ) log.debug( "DEFINE-PRIMITIVE-ATTRIBUTE " + r );
                    }
                    else if( log.isDebugEnabled() ) log.debug( "DEFINE-PRIMITIVE-ROLE " + r );
                }

                while( !peekNext( ')' ) ) {
                    if( peekNext( ':' ) ) {
                        skipNext( ':' );
                        String cmd = nextString();
                        if( cmd.equalsIgnoreCase( "parents" ) ) {
                            boolean paren = peekNext( '(' );
                            if( paren ) {
                                skipNext( '(' );
                                while( !peekNext( ')' ) ) {
                                    ATermAppl s = nextTerm();
                                    if( !s.getName().equals( "NIL" ) ) {
                                        kb.addObjectProperty( s );
                                        kb.addSubProperty( r, s );
                                        if( log.isDebugEnabled() ) 
                                            log.debug( "PARENT-ROLE " + r + " " + s );
                                    }
                                }
                                skipNext( ')' );
                            }
                            else {
                                ATermAppl s = nextTerm();
                                if( !s.toString().equalsIgnoreCase( "NIL" ) ) {
                                    kb.addObjectProperty( s );
                                    kb.addSubProperty( r, s );
                                    if( log.isDebugEnabled() ) log.debug( "PARENT-ROLE " + r + " " + s );
                                }
                            }
                        }
                        else if( cmd.equalsIgnoreCase( "transitive" ) ) {
                            ATermUtils.assertTrue( nextString().equalsIgnoreCase( "T" ) );
                            kb.addTransitiveProperty( r );
                            if( log.isDebugEnabled() ) log.debug( "TRANSITIVE-ROLE " + r );
                        }
                        else if( cmd.equalsIgnoreCase( "range" ) ) {
                            ATermAppl range = parseExpr();
                            kb.addClass( range );
                            kb.addRange( r, range );
                            if( log.isDebugEnabled() ) log.debug( "RANGE " + r + " " + range );
                        }
                        else if( cmd.equalsIgnoreCase( "domain" ) ) {
                            ATermAppl domain = parseExpr();
                            kb.addClass( domain );
                            kb.addDomain( r, domain );
                            if( log.isDebugEnabled() ) log.debug( "DOMAIN " + r + " " + domain );
                        }
                        else if( cmd.equalsIgnoreCase( "inverse" ) ) {
                            ATermAppl inv = nextTerm();
                            kb.addInverseProperty( r, inv );
                            if( log.isDebugEnabled() ) log.debug( "INVERSE " + r + " " + inv );
                        }
                        else
                            throw new RuntimeException( "Invalid role spec " + cmd );
                    }
                    else if( peekNext( '(' ) ) {
                        skipNext( '(' );
                        String cmd = nextString();
                        if( cmd.equalsIgnoreCase( "domain-range" ) ) {
                            ATermAppl domain = nextTerm();
                            ATermAppl range = nextTerm();

                            kb.addDomain( r, domain );
                            kb.addRange( r, range );
                            if( log.isDebugEnabled() ) 
                                log.debug( "DOMAIN-RANGE " + r + " " + domain + " "
                                    + range );
                        }
                        else
                            throw new RuntimeException( "Invalid role spec" );
                        skipNext( ')' );
                    }
                }
            }
            else if( str.equalsIgnoreCase( "DEFINE-PRIMITIVE-CONCEPT" )
                || str.equalsIgnoreCase( "DEFPRIMCONCEPT" ) ) {
                ATermAppl c = nextTerm();
                kb.addClass( c );

                ATermAppl expr = null;
                if( !peekNext( ')' ) ) {
                    expr = parseExpr();

                    if( !expr.getName().equals( "NIL" ) ) {
                        kb.addClass( expr );
                        kb.addSubClass( c, expr );
                    }
                }

                if( log.isDebugEnabled() ) 
                    log.debug( "DEFINE-PRIMITIVE-CONCEPT " + c + " "
                        + (expr == null ? "" : expr.toString()) );
            }
            else if( str.equalsIgnoreCase( "DEFINE-DISJOINT-PRIMITIVE-CONCEPT" ) ) {
                ATermAppl c = nextTerm();
                kb.addClass( c );

                skipNext( '(' );
                while( !peekNext( ')' ) ) {
                    ATermAppl expr = parseExpr();

                    List prevDefinitions = (List) disjoints.get( expr );
                    if( prevDefinitions == null ) prevDefinitions = new ArrayList();
                    for(Iterator i = prevDefinitions.iterator(); i.hasNext();) {
                        ATermAppl d = (ATermAppl) i.next();
                        kb.addDisjointClass( c, d );
                        if( log.isDebugEnabled() ) 
                            log.debug( "DEFINE-PRIMITIVE-DISJOINT " + c + " " + d );
                    }
                    prevDefinitions.add( c );
                }
                skipNext( ')' );

                ATermAppl expr = parseExpr();
                kb.addSubClass( c, expr );

                if( log.isDebugEnabled() ) log.debug( "DEFINE-PRIMITIVE-CONCEPT " + c + " " + expr );
            }
            else if( str.equalsIgnoreCase( "DEFINE-CONCEPT" )
                || str.equalsIgnoreCase( "DEFCONCEPT" ) || str.equalsIgnoreCase( "EQUAL_C" ) ) {
                ATermAppl c = nextTerm();
                kb.addClass( c );

                ATermAppl expr = parseExpr();
                kb.addEquivalentClass( c, expr );

                if( log.isDebugEnabled() ) log.debug( "DEFINE-CONCEPT " + c + " " + expr );
            }
            else if( str.equalsIgnoreCase( "IMPLIES" ) || str.equalsIgnoreCase( "IMPLIES_C" ) ) {
                ATermAppl c1 = parseExpr();
                ATermAppl c2 = parseExpr();
                kb.addClass( c1 );
                kb.addClass( c2 );
                kb.addSubClass( c1, c2 );

                if( log.isDebugEnabled() ) log.debug( "IMPLIES " + c1 + " " + c2 );
            }
            else if( str.equalsIgnoreCase( "IMPLIES_R" ) ) {
                ATermAppl p1 = parseExpr();
                ATermAppl p2 = parseExpr();
                kb.addProperty( p1 );
                kb.addProperty( p2 );
                kb.addSubProperty( p1, p2 );

                if( log.isDebugEnabled() ) log.debug( "IMPLIES_R " + p1 + " " + p2 );
            }
            else if( str.equalsIgnoreCase( "EQUAL_R" ) ) {
                ATermAppl p1 = parseExpr();
                ATermAppl p2 = parseExpr();
                kb.addObjectProperty( p1 );
                kb.addObjectProperty( p2 );
                kb.addEquivalentProperty( p1, p2 );

                if( log.isDebugEnabled() ) log.debug( "EQUAL_R " + p1 + " " + p2 );
            }
            else if( str.equalsIgnoreCase( "DOMAIN" ) ) {
                ATermAppl p = parseExpr();
                ATermAppl c = parseExpr();
                kb.addProperty( p );
                kb.addClass( c );
                kb.addDomain( p, c );

                if( log.isDebugEnabled() ) log.debug( "DOMAIN " + p + " " + c );
            }
            else if( str.equalsIgnoreCase( "RANGE" ) ) {
                ATermAppl p = parseExpr();
                ATermAppl c = parseExpr();
                kb.addProperty( p );
                kb.addClass( c );
                kb.addRange( p, c );

                if( log.isDebugEnabled() ) log.debug( "RANGE " + p + " " + c );
            }
            else if( str.equalsIgnoreCase( "FUNCTIONAL" ) ) {
                ATermAppl p = parseExpr();
                kb.addProperty( p );
                kb.addFunctionalProperty( p );

                if( log.isDebugEnabled() ) log.debug( "FUNCTIONAL " + p );
            }
            else if( str.equalsIgnoreCase( "TRANSITIVE" ) ) {
                ATermAppl p = parseExpr();
                kb.addObjectProperty( p );
                kb.addTransitiveProperty( p );

                if( log.isDebugEnabled() ) log.debug( "TRANSITIVE " + p );
            }
            else if( str.equalsIgnoreCase( "DISJOINT" ) ) {
                ATermAppl[] list = parseExprList();
                for(int i = 0; i < list.length - 1; i++) {
                    ATermAppl c1 = list[i];
                    for(int j = i + 1; j < list.length; j++) {
                        ATermAppl c2 = list[j];
                        kb.addClass( c2 );
                        kb.addDisjointClass( c1, c2 );
                        if( log.isDebugEnabled() ) log.debug( "DISJOINT " + c1 + " " + c2 );
                    }
                }
            }
            else if( str.equalsIgnoreCase( "DEFINDIVIDUAL" ) ) {
                ATermAppl x = nextTerm();

                kb.addIndividual( x );
                if( log.isDebugEnabled() ) log.debug( "DEFINDIVIDUAL " + x );
            }
            else if( str.equalsIgnoreCase( "INSTANCE" ) ) {
                ATermAppl x = nextTerm();
                ATermAppl c = parseExpr();

                kb.addIndividual( x );
                kb.addType( x, c );
                if( log.isDebugEnabled() ) log.debug( "INSTANCE " + x + " " + c );
            }
            else if( str.equalsIgnoreCase( "RELATED" ) ) {
                ATermAppl x = nextTerm();
                ATermAppl y = nextTerm();
                ATermAppl r = nextTerm();

                kb.addIndividual( x );
                kb.addIndividual( y );
                kb.addPropertyValue( r, x, y );

                if( log.isDebugEnabled() ) log.debug( "RELATED " + x + " - " + r + " -> " + y );
            }
            else if( str.equalsIgnoreCase( "DIFFERENT" ) ) {
                ATermAppl x = nextTerm();
                ATermAppl y = nextTerm();

                kb.addIndividual( x );
                kb.addIndividual( y );
                kb.addDifferent( x, y );

                if( log.isDebugEnabled() ) log.debug( "DIFFERENT " + x + " " + y );
            }
            else if( str.equalsIgnoreCase( "DATATYPE-ROLE-FILLER" ) ) {
                ATermAppl x = nextTerm();
                ATermAppl y = ATermUtils.makePlainLiteral( nextString() );
                ATermAppl r = nextTerm();

                kb.addIndividual( x );
                kb.addIndividual( y );
                kb.addPropertyValue( r, x, y );

                if( log.isDebugEnabled() ) 
                    log.debug( "DATATYPE-ROLE-FILLER " + x + " - " + r + " -> " + y );
            }
            else
                throw new RuntimeException( "Unknown command " + str );
            skipNext( ')' );

            token = in.nextToken();
        }
    }

    public void load( InputStream stream, KnowledgeBase kb ) throws IOException {
        load( new InputStreamReader( stream ), kb );
    }

    public void load( String file, KnowledgeBase kb ) throws IOException {
        load( new FileReader( file ), kb );
    }

    public void verifyTBox( String file, KnowledgeBase kb ) throws Exception {
        initTokenizer( new FileReader( file ) );

        int verifiedCount = 0;
        int token = in.nextToken();
        while( token != ')' && token != StreamTokenizer.TT_EOF ) {
            ATermUtils.assertTrue( token == '(' );

            verifiedCount++;

            ATermAppl c = null;
            if( peekNext( '(' ) ) {
                ATermAppl[] list = parseExprList();
                c = list[0];
                Set eqs = kb.getEquivalentClasses( c );
                for(int i = 1; i < list.length; i++) {
                    ATermAppl t = list[i];

                    if( !eqs.contains( t ) )
                        throw new RuntimeException( t + " is not equivalent to " + c );
                }
            }
            else
                c = parseExpr();

            Set supers = SetUtils.union( kb.getSuperClasses( c, true ) );
            Set subs = SetUtils.union( kb.getSubClasses( c, true ) );

            if( log.isDebugEnabled() ) 
                log.debug( "Verify (" + verifiedCount + ") " + c + " " + supers + " "
                    + subs );

            if( peekNext( '(' ) ) {
                ATermAppl[] terms = parseExprList();
                for(int i = 0; i < terms.length; i++) {
                    ATerm t = terms[i];

                    if( !supers.contains( t ) )
                        throw new RuntimeException( t + " is not a superclass of " + c + " "
                            + supers );
                }
            }
            else
                skipNext();

            if( peekNext( '(' ) ) {
                ATermAppl[] terms = parseExprList();
                for(int i = 0; i < terms.length; i++) {
                    ATermAppl t = terms[i];

                    if( !subs.contains( t ) ) {
                        Set temp = new HashSet( subs );
                        Set sames = kb.getEquivalentClasses( t );
                        temp.retainAll( sames );
                        if( temp.size() == 0 )
                            throw new RuntimeException( t + " is not a subclass of " + c + " "
                                + subs );
                    }
                }
            }

            skipNext();

            token = in.nextToken();
        }

        ATermUtils.assertTrue( in.nextToken() == StreamTokenizer.TT_EOF );
    }

    public void verifyABox( String file, KnowledgeBase kb ) throws Exception {
        initTokenizer( new FileReader( file ) );
        
        boolean longFormat = !peekNext( '(' );
        
        while( !peekNext( StreamTokenizer.TT_EOF ) ) {
            if( longFormat ) {
                skipNext( "Command" );
                skipNext( '=' );
            }
            
            skipNext( '(' );
            skipNext( "INDIVIDUAL-INSTANCE?" );

            ATermAppl ind = nextTerm();
            ATermAppl c = parseExpr();

            if( log.isDebugEnabled() ) 
                log.debug( "INDIVIDUAL-INSTANCE? " + ind + " " + c );

            skipNext( ')' );
            
            boolean isType;
            if( longFormat ) {
                skipNext( '-' );
                skipNext( '>' );
                String result = nextString();
                if( result.equalsIgnoreCase( "T" ) )
                    isType = true;
                else if( result.equalsIgnoreCase( "NIL" ) )
                    isType = false;
                else
                    throw new RuntimeException( "Unknown result " + result );
            }
            else
                isType = true;
            
            if( log.isDebugEnabled() ) log.debug( " -> " + isType );

            if( kb.isType( ind, c ) != isType ) {
                throw new RuntimeException( 
                    "Individual " + ind + " is " + (isType ? "not" : "") + " an instance of " + c );
            }
        }
    }
}
