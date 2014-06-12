/*
 * Created on Jan 8, 2005
 */
package org.mindswap.pellet.query.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.datatypes.Datatype;
import org.mindswap.pellet.datatypes.RDFSLiteral;
import org.mindswap.pellet.exceptions.InternalReasonerException;
import org.mindswap.pellet.query.Query;
import org.mindswap.pellet.query.QueryPattern;
import org.mindswap.pellet.query.QueryResultBinding;
import org.mindswap.pellet.query.QueryUtils;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.QNameProvider;

import aterm.ATermAppl;
import aterm.ATermList;

/**
 * @author Evren Sirin
 *
 */
public class QueryImpl implements Query {    
    private KnowledgeBase kb;
    
    private List patterns;
    private Map types;
    
    private List resultVars;
    private Set distVars;  
    
    private Set vars;
    private Set objVars;
    private Set litVars;

    private Set constants;
    
    private QNameProvider qnames;
    
    public QueryImpl(KnowledgeBase kb) {
        this.kb = kb;
        
        patterns = new ArrayList();     
        types = new HashMap();
        
        resultVars = new ArrayList();
        distVars = new HashSet();
        objVars = new HashSet();  
        litVars = new HashSet();  
        constants = new HashSet(); 
        
        qnames = new QNameProvider();
    }
    
    public void addDistVar( ATermAppl var ) {
        if( !ATermUtils.isVar( var ) )
            return;
        
        distVars.add( var );
    }

    public void addResultVar( ATermAppl var ) {
        if( !ATermUtils.isVar( var ) )
            return;
        
        resultVars.add( var );
        distVars.add( var );
    }
    
    private void addTerm(ATermAppl term, boolean isObj) {        
        Set set = ATermUtils.isVar( term ) 
         	? (isObj ? objVars : litVars)
            : constants;
         	
        if( !set.contains( term ) )
            set.add( term );        
    }

    public void addPattern( QueryPattern pattern ) {
        addPattern( patterns.size(), pattern );
    }
    
    public void addPattern( int index, QueryPattern pattern ) {
	    ATermAppl subj = pattern.getSubject();
	    ATermAppl obj = pattern.getObject();
	    
        addTerm( subj, true );
        
        if( pattern.isTypePattern() ) {
            Set set = (Set) types.get( subj );
            if( set == null ) {
                set = new HashSet();
                types.put( subj, set );
            }
            set.add( obj );            
        }
        else {
    	    ATermAppl pred = pattern.getPredicate();

    	    addTerm( obj, kb.isObjectProperty( pred ) );            
        }
        
        patterns.add( index, pattern );
    }
    
    public void addTypePattern(ATermAppl ind, ATermAppl c) {
        addPattern(new QueryPatternImpl(ind, c));
    }

    public void addEdgePattern(ATermAppl s, ATermAppl p, ATermAppl o) {
        addPattern(new QueryPatternImpl(s, p, o));
    }
    
    public void insertEdgePattern(ATermAppl s, ATermAppl p, ATermAppl o) {
        addPattern(0, new QueryPatternImpl(s, p, o));
    }       
    
    public void addConstraint( ATermAppl lit, Datatype dt ) {
        types.put( lit, dt );
    }

    public Set getVars() {
        if( vars == null ) {
            vars = new HashSet( objVars.size() + litVars.size() );
            vars.addAll( objVars );
            vars.addAll( litVars );
        }
        
        return vars;
    }

    public Set getObjVars() {
        return objVars;
    }

    public Set getLitVars() {
        return litVars;
    }

    public Set getConstants() {
        return constants;
    }    

    public List getResultVars() {
        return resultVars;
    }

    public Set getDistVars() {
        return distVars;
    }

    public Set getDistObjVars() {
        Set distObjVars = new HashSet( objVars );
        distObjVars.retainAll( distVars );

        return distObjVars;
    }

    public Set getDistLitVars() {
        Set distObjVars = new HashSet( litVars );
        distObjVars.retainAll( distVars );

        return distObjVars;
    }
    
    public List getQueryPatterns() {
        return patterns;
    }

    public ATermAppl rollUpTo( ATermAppl var ) {
		if ( litVars.contains( var ) ) {
			throw new InternalReasonerException( "trying to roll up to a Literal variable." );	
		}
		
		//ATermAppl testClass;
		ATermList classParts = ATermUtils.EMPTY_LIST;
		
		//System.out.println( var + " " + varInd );
		Set visited = new HashSet();		
		
		for ( Iterator e = getInEdges( var ).iterator(); e.hasNext(); ) {
			classParts = classParts.append( rollEdgeIn( (QueryPattern) e.next(), visited ) );							
		}
	
		//System.out.println( "outs: " + qbox.getOutEdges( var ).toString() );
		for ( Iterator e = getOutEdges( var ).iterator(); e.hasNext(); ) {
			classParts = classParts.append( rollEdgeOut( (QueryPattern) e.next(), visited ) );							
		}
	
		// Handling the type statements in the query.
		classParts = classParts.concat( getClasses( var ) );

		// assembling the concept definition
		ATermAppl testClass = ATermUtils.makeAnd( classParts );
	
		//System.out.println( "\nConstructed class:\n" + testClass.toString() );
		return testClass;
    }

    /* (non-Javadoc)
     * @see org.mindswap.pellet.newquery.Query#getKB()
     */
    public KnowledgeBase getKB() {
        return kb;
    }


    /* (non-Javadoc)
     * @see org.mindswap.pellet.newquery.Query#applyBinding(org.mindswap.pellet.newquery.QueryResultBinding)
     */
    public Query apply(QueryResultBinding binding) {
        Query query = new QueryImpl( kb );
        
        for(int i = 0; i < patterns.size(); i++) {
            QueryPattern pattern = (QueryPattern) patterns.get( i );
            
            pattern = pattern.apply( binding );
            query.addPattern( pattern );
        }
        
        return query;        
    }

    /* (non-Javadoc)
     * @see org.mindswap.pellet.newquery.Query#isGround()
     */
    public boolean isGround() {
        return objVars.isEmpty() && litVars.isEmpty();
    }

    private List getInEdges( ATermAppl term ) {
        return findPatterns( null, null, term );
    }
    
    private List getOutEdges( ATermAppl term ) {
        return findPatterns( term, null, null );
    }
    
    /* (non-Javadoc)
     * @see org.mindswap.pellet.newquery.Query#findPatterns(aterm.ATermAppl, aterm.ATermAppl, aterm.ATermAppl)
     */
    public List findPatterns(ATermAppl subj, ATermAppl pred, ATermAppl obj) {
        List list = new ArrayList();
        for(int i = 0; i < patterns.size(); i++) {
            QueryPattern pattern = (QueryPattern) patterns.get( i );
            if( pattern.isTypePattern( ) )
                continue;
            
            if( (subj == null || subj.equals( pattern.getSubject() )) &&
                (pred == null || pred.equals( pattern.getPredicate() )) &&
                (obj == null || obj.equals( pattern.getObject() )) )
                list.add( pattern );
        }
        
        return list;
    }
	
	private ATermAppl rollEdgeOut( QueryPattern pattern, Set visited ) {		
	    ATermAppl subj = pattern.getSubject();
	    ATermAppl pred = pattern.getPredicate();
	    ATermAppl obj = pattern.getObject();
	    
		visited.add( subj );
		
		if ( visited.contains( obj ) ) {
			ATermList temp = getClasses( obj );
			if ( temp.getLength() == 0 ) {
				return ATermUtils.makeMin( pred, 1 );
			} else {
				return ATermUtils.makeSomeValues( pred, ATermUtils.makeAnd( temp ) );
			}
			
		}
		
		if ( ATermUtils.isLiteral( obj ) ) {
			ATermAppl type = ATermUtils.makeValue( obj );
			return ATermUtils.makeSomeValues( pred, type );
		} 
		else if ( litVars.contains( obj ) ) {
			Datatype dtype = getDatatype( obj );

			return ATermUtils.makeSomeValues( pred, dtype.getName() );
		}
		
		ATermList targetClasses = getClasses( obj );
		
		for( Iterator ins = getInEdges( obj ).iterator(); ins.hasNext(); ) {
			QueryPattern in = (QueryPattern) ins.next();
					
			if ( !in.equals( pattern ) ) {
				targetClasses = targetClasses.append( rollEdgeIn( in, visited ) );	
			}
		}
		
		List targetOuts = getOutEdges( obj );

		if ( targetClasses.isEmpty() ) {
			if ( targetOuts.size() == 0 ) {
				// this is a simple leaf node
				return ATermUtils.makeMin( pred, 1 );
			} else {
				// not a leaf node, recurse over all outgoing edges
				ATermList outs = ATermUtils.EMPTY_LIST;
				
				for ( Iterator i = targetOuts.iterator(); i.hasNext(); ) {
				    QueryPattern currEdge = (QueryPattern) i.next();
					
					outs = outs.append( rollEdgeOut( currEdge, visited ) );
				}
				
				return ATermUtils.makeSomeValues( pred, ATermUtils.makeAnd( outs ) );
			}
		} else {
			if ( targetOuts.size() == 0 ) {
				// this is a simple leaf node, but with classes specified
				return ATermUtils.makeSomeValues( pred, ATermUtils.makeAnd( targetClasses ) );
			} else {
				// not a leaf node, recurse over all outgoing edges
				ATermList outs = ATermUtils.EMPTY_LIST;
	
				for ( Iterator i = targetOuts.iterator(); i.hasNext(); ) {
				    QueryPattern currEdge = (QueryPattern) i.next();
		
					outs = outs.append( rollEdgeOut( currEdge, visited ) );
				}
	
				for ( int i = 0; i < targetClasses.getLength(); i++ ) {
					outs = outs.append( targetClasses.elementAt( i ) );				
				}
				
				return ATermUtils.makeSomeValues( pred, ATermUtils.makeAnd( outs ) );
				
			}
		}
	}
	
	// TODO this should die if called on a literal node
	private ATermAppl rollEdgeIn( QueryPattern pattern, Set visited ) {
	    ATermAppl subj = pattern.getSubject();
	    ATermAppl pred = pattern.getPredicate();
	    ATermAppl obj = pattern.getObject();
	    ATermAppl invPred = kb.getRBox().getRole( pred ).getInverse().getName();
	    
		visited.add( obj );

		if ( visited.contains( subj ) ) {
			ATermList temp = getClasses( subj );
			if ( temp.getLength() == 0 ) {
				return ATermUtils.makeMin( invPred, 1 );
			} else {
				return ATermUtils.makeSomeValues( invPred, ATermUtils.makeAnd( temp ) );
			}			
		}
		
		ATermList targetClasses = getClasses( subj );
		
		List targetIns = getInEdges( subj );		

		for( Iterator out = getOutEdges( subj ).iterator(); out.hasNext(); ) {
		    QueryPattern o = (QueryPattern) out.next();
			
			if ( !o.equals( pattern ) ) {
				targetClasses = targetClasses.append( rollEdgeOut( o, visited ) );	
			}
		}

		if ( targetClasses.isEmpty() ) {
			if ( targetIns.isEmpty() ) {
				// this is a simple leaf node
				
				return ATermUtils.makeMin( invPred, 1);
			} else {
				// not a leaf node, recurse over all incoming edges
				ATermList ins = ATermUtils.EMPTY_LIST;
			
				for ( Iterator i = targetIns.iterator(); i.hasNext(); ) {
				    QueryPattern currEdge = (QueryPattern) i.next();
				
					ins = ins.append( rollEdgeIn( currEdge, visited ) );
				}
			
				return ATermUtils.makeSomeValues( invPred, ATermUtils.makeAnd( ins ) );
			}
		} else {
			if ( targetIns.isEmpty() ) {
				// this is a simple leaf node, but with classes specified
				
				return ATermUtils.makeSomeValues( invPred, ATermUtils.makeAnd( targetClasses ) );
			} else {
				// not a leaf node, recurse over all outgoing edges
				ATermList ins = ATermUtils.EMPTY_LIST;

				for ( Iterator i = targetIns.iterator(); i.hasNext(); ) {
				    QueryPattern currEdge = (QueryPattern) i.next();
	
					ins = ins.append( rollEdgeIn( currEdge, visited ) );
				}

				for ( int i = 0; i < targetClasses.getLength(); i++ ) {
					ins = ins.append( targetClasses.elementAt( i ) );			
				}
				
				return ATermUtils.makeSomeValues( invPred, ATermUtils.makeAnd( ins ) );
			
			}
		}
	}
	
	public ATermList getClasses( ATermAppl term ) {
	    Set classes = (Set) types.get( term );
	    	    
		ATermList results = (classes == null)
		    ? ATermUtils.EMPTY_LIST
		    : ATermUtils.makeList( classes );

		if ( !ATermUtils.isVar( term ) ) {
		    // insert is always cheaper
			results = results.insert( ATermUtils.makeValue( term ) );
		} 

		return results;
	}

    /* (non-Javadoc)
     * @see org.mindswap.pellet.newquery.Query#getDatatype(aterm.ATermAppl)
     */
    public Datatype getDatatype(ATermAppl term) {
        Datatype type = (Datatype) types.get( term );
        
        if( type == null )
            type = RDFSLiteral.instance;
        
        return type;
    }
    
    
	public void prepare() {
	}
    
    public void printName( ATermAppl term, StringBuffer sb ) {
        if( ATermUtils.isVar( term ) )
            sb.append("?").append(((ATermAppl)term.getArgument(0)).getName());        
        else
            sb.append(qnames.shortForm(term.getName()));
    }

    public String toString() {
        QNameProvider qnames = new QNameProvider();
        
        String indent = "     ";
        StringBuffer sb = new StringBuffer(); 
        
        sb.append( "query(" ) ;
        for( int i = 0; i < resultVars.size(); i++ ) {
            ATermAppl var = (ATermAppl) resultVars.get( i );
            if( i > 0 )
                sb.append(", ");
            sb.append( "?" ).append( QueryUtils.getVarName( var ) );
        }        
        sb.append(")") ;

        if( patterns.size() > 0 ) {
            sb.append(":-\n") ;
            for( int i = 0; i < patterns.size(); i++ ) {
                QueryPattern p = (QueryPattern) patterns.get( i );
                if( i > 0 ) 
                    sb.append(", \n");
                
                sb.append(indent);                   
                if( p.isTypePattern() ) {
                    sb.append(QueryUtils.formatTerm(p.getObject(), qnames));
                    sb.append("(");
                    sb.append(QueryUtils.formatTerm(p.getSubject(), qnames));
                    sb.append(")");
                }
                else {
                    sb.append(QueryUtils.formatTerm(p.getPredicate(), qnames));
                    sb.append("(");
                    sb.append(QueryUtils.formatTerm(p.getSubject(), qnames));
                    sb.append(", ");
                    sb.append(QueryUtils.formatTerm(p.getObject(), qnames));
                    sb.append(")");                   
                }
                
            }
        }

        sb.append(".\n") ;
        return sb.toString() ;
    }
}
