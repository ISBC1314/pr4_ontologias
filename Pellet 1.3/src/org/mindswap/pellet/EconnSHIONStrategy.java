/*
 * Created on Aug 29, 2004
 */
package org.mindswap.pellet;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mindswap.pellet.exceptions.InternalReasonerException;
import org.mindswap.pellet.utils.ATermUtils;

import aterm.ATermAppl;
import aterm.ATermInt;
import aterm.ATermList;

/**
 * @author Bernardo Cuenca
 */
public class EconnSHIONStrategy extends CompletionStrategy {
    /**
     * @param abox
     * @param blocking
     */
    public EconnSHIONStrategy(ABox abox) {
        super(abox, new EconnDoubleBlocking(abox));
    }
    
    boolean supportsPseudoModelCompletion() {
        return false;
    }    

    class FilteredIterator extends IndividualIterator {
        /**
         * @param abox
         */
        private String ontology;

        boolean needToApplyRules;

        FilteredIterator(String ont) {
            super(abox, false);
            ontology = ont;
            needToApplyRules = false;
            findNext();
        }

        protected void findNext() {
            for(; index < stop; index++) {
                if((nodes.get(nodeList.get(index)) instanceof Individual)) {
                    Individual ind = (Individual) nodes.get(nodeList.get(index));
                    String ont1 = ind.getOntology();
                    if((ont1.equals(ontology)) || (!ont1.equals(ontology) && !ind.isRoot())) break;
                }
            }
        }
    }//End Class

    /**
     * apply disjunction rule to the ABox
     *  
     */
    protected void applyDisjunctionRule(IndividualIterator i) {
        i.reset();
        while(i.hasNext()) {
            Individual node = (Individual) i.next();

            node.setChanged(Node.OR, false);

            if(!node.canApply(Node.OR) || blocking.isIndirectlyBlocked(node)) continue;

            List types = node.getTypes(Node.OR);
            int size = types.size();
            for(int j = node.applyNext[Node.OR]; j < size; j++) {
                ATermAppl disjunction = (ATermAppl) types.get(j);

                // disjunction is now in the form not(and([not(d1), not(d2),
                // ...]))
                ATermAppl a = (ATermAppl) disjunction.getArgument(0);
                ATermList disjuncts = (ATermList) a.getArgument(0);
                ATermAppl[] disj = new ATermAppl[disjuncts.getLength()];

                for(int index = 0; !disjuncts.isEmpty(); disjuncts = disjuncts.getNext(), index++) {
                    disj[index] = ATermUtils.negate((ATermAppl) disjuncts.getFirst());
                    if(node.hasType(disj[index])) break;
                }

                if(!disjuncts.isEmpty()) continue;

                DisjunctionBranch newBranch = new DisjunctionBranch(abox, this, node, disjunction,
                    node.getDepends(disjunction), disj);
                addBranch(newBranch);

                if(newBranch.tryNext() == false) return;
            }
            node.applyNext[Node.OR] = size;
        }
    }

    /**
     * 
     * applyMaxRule
     * 
     * @param x
     * @param r
     * @param k
     * @param ds
     * 
     * @return true if more merges are required for this maxCardinality
     */
    protected boolean applyMaxRule(Individual x, Role r, int k, DependencySet ds) {
        EdgeList edges = x.getRNeighborEdges(r);
        // find all distinct R-neighbors of x
        Set neighbors = edges.getNeighbors(x);

        // if restriction was maxCardinality 0 then having any R-neighbor
        // violates the restriction. no merge can fix this. compute the
        // dependency and return
        if(k == 0 && neighbors.size() > 0) {
            for(int e = 0; e < edges.size(); e++) {
                Edge edge = edges.edgeAt(e);
                ds = ds.union(edge.getDepends());
            }

            abox.setClash( Clash.maxCardinality( x, ds, r.getName(), 0 ) );
            
            return false;
        }

        // if there are less than n neighbors than max rule won't be triggered
        // return false beceuse no more merge required for this role
        if(neighbors.size() <= k) return false;

        // create the pairs to be merged
        List mergePairs = findMergeNodes(neighbors, x);

        // if no pairs were found, i.e. all were defined to be different from
        // each
        // other, to be merged then it means this max cardinality restriction is
        // violated. dependency of this clash is on all the neihbors plus the
        // dependency of the restriction type
        if(mergePairs.size() == 0) {
            DependencySet dsEdges = x.hasDistinctRNeighborsForMax(r, k + 1);
            if(dsEdges == null)
                return false;
            else {
                if(ABox.DEBUG)
                    System.out.println("Early clash detection for max rule worked " + x
                        + " has more than " + k + " " + r + " edges " + ds.union(dsEdges) + " "
                        + x.getRNeighborEdges(r).getNeighbors(x));

                if(abox.doExplanation())
                    abox.setClash(Clash.maxCardinality(x, ds.union(dsEdges), r.getName(), k));
                else
                    abox.setClash(Clash.maxCardinality(x, ds.union(dsEdges)));

                return false;
            }
        }

        // add the list of possible pairs to be merged in the branch list
        MaxBranch newBranch = new MaxBranch(abox, this, x, r, k, mergePairs, ds);
        addBranch(newBranch);

        // try a merge that does not trivially fail
        if(newBranch.tryNext() == false) return false;

        if(ABox.DEBUG) abox.printTree();

        // if there were exactly n + 1 neighbors the previous step would
        // eliminate one node and only n neighbors would be left. This means
        // restriction is satisfied. If there were more than n + 1 neighbors
        // merging one pair would not be enough and more merges are required,
        // thus false is returned
        return neighbors.size() > k + 1;
    }

    /**
     * apply max rule to the ABox
     *  
     */
    protected void applyMaxRule(IndividualIterator i) {
        i.reset();
        while(i.hasNext()) {
            Individual x = (Individual) i.next();

            if(!x.isChanged(Node.MAX)) continue;

            List maxCardinality = x.getTypes(Node.MAX);

            if(blocking.isIndirectlyBlocked(x)) continue;

            Iterator j = maxCardinality.iterator();
            while(j.hasNext()) {
                ATermAppl mc = (ATermAppl) j.next();

                // max(r, n) is in normalized form not(min(p, n + 1))
                ATermAppl max = (ATermAppl) mc.getArgument(0);

                //              *****************************************
                Role r = abox.getRole(max.getArgument(0));
                //****************************************

                int n = ((ATermInt) max.getArgument(1)).getInt() - 1;

                DependencySet ds = x.getDepends(mc);

                if(n == 1)
                    applyFunctionalMaxRule(x, r, ds);
                else {
                    boolean hasMore = true;
                    while(hasMore)
                        hasMore = applyMaxRule(x, r, n, ds);
                }
                if(abox.isClosed()) return;
            }
            //Econn

            j = ((EconnectedKB) (abox.getKB())).getRBox().getFunctionalRoles().iterator();
            while(j.hasNext()) {
                Role r = (Role) j.next();

                applyFunctionalMaxRule(x, r, DependencySet.INDEPENDENT);
                if(abox.isClosed()) return;
            }

            x.setChanged(Node.MAX, false);
        }

    }

    /**
     * apply min rule to the ABox
     *  
     */
    protected void applyMinRule(IndividualIterator i) {
        i.reset();
        while(i.hasNext()) {
            Individual x = (Individual) i.next();

            //            x.setChanged(Node.MIN, false);

            if(!x.canApply(Individual.MIN) || blocking.isBlocked(x)) continue;
            //We get all the minCard restrictions in the node and store
            // them in the list ''types''
            List types = x.getTypes(Node.MIN);
            int size = types.size();
            for(int j = x.applyNext[Node.MIN]; j < size; j++) {
                //mc stores the current type (the current minCard restriction)
                ATermAppl mc = (ATermAppl) types.get(j);

                //We retrieve the role associated to the current
                //min restriction

                //*****************************************
                Role r = abox.getRole(mc.getArgument(0));
                //****************************************

                int n = ((ATermInt) mc.getArgument(1)).getInt();

                if(x.hasDistinctRNeighborsForMin(r, n)) continue;

                if(ABox.DEBUG)
                    System.out.println("Apply min rule to " + x + " " + mc + " anon"
                        + (abox.anonCount + 1) + " - anon" + (abox.anonCount + n));

                DependencySet ds = x.getDepends(mc);

                Node[] y = new Node[n];

                for(int c1 = 0; c1 < n; c1++) {
                    if(r.isDatatypeRole())
                        y[c1] = abox.addLiteral();
                    else {
                        y[c1] = abox.addFreshIndividual();
                        y[c1].depth = x.depth + 1;

                        if(x.depth >= abox.treeDepth) abox.treeDepth = x.depth + 1;
                        //Added for Econn
                        if(r.isLinkRole())
                            y[c1].setOntology(r.getForeignOntology());
                        else {
                            y[c1].setOntology(x.getOntology());
                        }

                    }
                    addEdge(x, r, y[c1], ds);
                }

                for(int c1 = 0; c1 < n; c1++)
                    for(int c2 = c1 + 1; c2 < n; c2++)
                        y[c1].setDifferent(y[c2], ds);
            }
            x.applyNext[Node.MIN] = size;
        }
    }

    protected void applyNominalRule(IndividualIterator i) {
        boolean ruleApplied = true;
        while(ruleApplied) {
            ruleApplied = false;

            i.reset();
            while(i.hasNext()) {
                Individual y = (Individual) i.next();

                if(!y.canApply(Individual.NOM) || blocking.isBlocked(y)) continue;

                List types = y.getTypes(Node.NOM);
                int size = types.size();
                for(int j = y.applyNext[Node.NOM]; j < size; j++) {
                    ATermAppl nc = (ATermAppl) types.get(j);
                    DependencySet ds = y.getDepends(nc);

                    // Get the value of mergedTo because of the following
                    // possibility.
                    // Suppose there are three individuals with like this
                    // [x,{}],[y,{value(x)}],[z,{value(y)}]
                    // After we merge x to y, the individual x is now
                    // represented by
                    // the node y. It is too hard to update all references of
                    // value(x) so here we find the actual representative node
                    // by calling getMergedTo()
                    Individual z = abox.getIndividual(nc.getArgument(0));
                    if(z.isMerged()) {
                        //if(z.mergedAt() < abox.getBranch()) 
                        ds = ds.union(z.getMergeDependency(true));

                        z = (Individual) z.getSame();
                    }
                    ATermUtils.assertTrue(abox.getNodes().contains(z));

                    if(y.equals(z) || y.isSame(z)) continue;

                    if(y.isDifferent(z)) {
                        ds = ds.union(y.getDifferenceDependency(z));
                        if(abox.doExplanation())
                            abox.setClash(Clash.nominal(y, ds, z.getName()));
                        else
                            abox.setClash(Clash.nominal(y, ds));
                        return;
                    }

                    if(ABox.DEBUG) System.out.println("Apply nominal rule to  " + y + " -> " + z);

                    if(ABox.DEBUG) abox.printTree();

                    mergeTo(y, z, ds);

                    if(ABox.DEBUG) abox.printTree();

                    if(abox.isClosed()) return;

                    ruleApplied = true;
                    // this node has been merged into another one so go to next
                    // node
                    break;
                }
                y.applyNext[Node.NOM] = size;
                y.setChanged(Node.NOM, false);
            }
        }
    }

    /**
     * apply some values rule to the ABox
     *  
     */

    protected void applySomeValuesRule(IndividualIterator i) {
        i.reset();
        while(i.hasNext()) {
            Individual x = (Individual) i.next();

            //           x.setChanged(Node.SOME, false);

            if(!x.canApply(Individual.SOME) || blocking.isBlocked(x)) continue;

            List types = x.getTypes(Node.SOME);
            int size = types.size();
            for(int j = x.applyNext[Node.SOME]; j < size; j++) {
                ATermAppl sv = (ATermAppl) types.get(j);

                // someValuesFrom is now in the form not(all(p. not(c)))
                ATermAppl a = (ATermAppl) sv.getArgument(0);
                ATermAppl s = (ATermAppl) a.getArgument(0);
                ATermAppl c = (ATermAppl) a.getArgument(1);

                //*****************************************
                //    Role role = abox.getKB().getRole(s);
                Role role = abox.getRole(s);
                //****************************************
                c = ATermUtils.negate(c);

                boolean neighborFound = false;
                // ''y'' is going to be the node we create, and ''edge'' its
                // connection to the
                //current node
                Node y = null;
                Edge edge = null;

                //edges contains all the edges going into of coming out from
                // the node
                //And labelled with the role R
                EdgeList edges = x.getRNeighborEdges(role);
                //We examine all those edges one by one and check if
                //the neighbor has type C, in which case we set neighborFound
                // to
                //true
                for(int e = 0; !neighborFound && e < edges.size(); e++) {

                    edge = edges.edgeAt(e);

                    y = edge.getNeighbor(x);

                    neighborFound = y.hasType(c);
                }

                //If we have found a R-neighbor with type C, continue, do
                // nothing
                if(neighborFound) continue;
                //If not, we have to create it
                DependencySet ds = x.getDepends(sv).copy();
                //If the role is a datatype property...
                if(role.isDatatypeRole()) {
                    if(ABox.DEBUG)
                        System.out.println("Apply some values rule to  " + x + " -> " + y + " " + s
                            + " " + c);

                    Literal literal = null;
                    if(ATermUtils.isNominal(c)) {
                        literal = abox.addLiteral((ATermAppl) c.getArgument(0));
                    }
                    else {
                        literal = abox.addLiteral();
                        literal.addType(c, ds);
                    }
                    addEdge(x, role, literal, ds);
                }
                //If it is an object property
                else {
                    //if(role.isObjectProperty()){
                    //if in some(R,C) C is a nominal, then we reuse an existing
                    // distinguished individual
                    if(ATermUtils.isNominal(c)) {
                        y = abox.getIndividual(c.getArgument(0));
                        if(ABox.DEBUG)
                            System.out.println("Apply has value " + x + " " + s + " " + y + " "
                                + ds);
                        addEdge(x, role, y, ds);
                    }
                    else {
                        boolean useExistingNode = false;
                        boolean useExistingRole = false;
                        DependencySet maxCardDS = role.isFunctional() 
                            ? DependencySet.INDEPENDENT 
                            : x.hasMax1( role );
                        if( maxCardDS != null ) {
                            ds = ds.union( maxCardDS );
                            
                            if( !edges.isEmpty() )
                                useExistingRole = useExistingNode = true;
                            else {
                                Set fs = role.isFunctional() ? 
                                    role.getFunctionalSupers() : 
                                    role.getSubRoles();
                        	    for(Iterator it = fs.iterator(); it.hasNext(); ) {
                                    Role f = (Role) it.next();
                                    edges = x.getRNeighborEdges(f);
                                    if( !edges.isEmpty() ) {
                                        if( useExistingNode ) {	                          
                                            Edge otherEdge = edges.edgeAt(0); 
	                                        Node otherNode = otherEdge.getNeighbor(x);
	                                        DependencySet d = 
	                                            ds.union( edge.getDepends() )
	                                              .union( otherEdge.getDepends() );
                                            mergeTo( y, otherNode, d );
                                        }
                                        else {
	                                        useExistingNode = true;
	                                        edge = edges.edgeAt(0);
	                                        y = edge.getNeighbor(x);
                                        }
                                    }
                                }
                                if( y != null )
                                    y = y.getSame();
                            }
                        }

                        if(useExistingNode) {
                            ds = ds.union(edge.getDepends());
                        }
                        else {
                            //Here we have to consider the links
                            y = abox.addFreshIndividual();
                            //added for Econnections
                            if(role.isLinkRole()) {
                                // System.out.println("I am applying Some Values
                                // to link");
                                // Evren: following line seems redundant and
                                // causing
                                // problems in classification. commented out:
                                // abox.getKB().setOntology(role.getForeignOntology());
                                y.setOntology(role.getForeignOntology());
                            }
                            else {
                                y.setOntology(x.getOntology());
                            }

                            y.depth = x.depth + 1;

                            if(x.depth >= abox.treeDepth) abox.treeDepth = x.depth + 1;
                        }

                        if(ABox.DEBUG)
                            System.out.println("Apply some values rule to  " + x + " -> " + y + " "
                                + s + " " + c + " " + ds + (useExistingNode ? " (old)" : " (new)"));

                        if(!useExistingRole) addEdge(x, role, y, ds);

                        addType(y, c, ds);
                    }
                }

                if(abox.isClosed()) return;
            }
            x.applyNext[Individual.SOME] = size;
        }
    }

    /**
     * Apply the unfolding rule to every concept in every node.
     *  
     */
    protected void applyUnfoldingRule(IndividualIterator i) {
        i.reset();
        while(i.hasNext()) {
            Individual node = (Individual) i.next();
            Map normalizedMap = ((EconnectedKB) abox.getKB()).getTBox(node.getOntology()).getUnfoldingMap();

            if(!node.canApply(Node.ATOM) || blocking.isBlocked(node)) continue;

            List types = node.getTypes(Node.ATOM);
            int size = types.size();
            for(int j = node.applyNext[Node.ATOM]; j < size; j++) {
                ATermAppl c = (ATermAppl) types.get(j);
                ATermAppl unfolded = (ATermAppl) normalizedMap.get(c);

                if(unfolded != null) {
                    DependencySet ds = node.getDepends(c).copy();

                    if(ABox.DEBUG && !node.hasType(unfolded))
                        System.out.println("Apply unfolding rule to  " + node + ": " + c + " -> "
                            + unfolded + " " + ds);

                    addType(node, unfolded, ds);

                    size = types.size();
                }
            }
            node.applyNext[Node.ATOM] = size;

            if(abox.isClosed()) return;
        }
    }

    protected boolean backtrack() {
        boolean branchFound = false;

        while(!branchFound) {
            int lastBranch = abox.getClash().depends.max();

            if(lastBranch <= 0)
                return false;
            else if(lastBranch > abox.getBranches().size())
                throw new InternalReasonerException("Backtrack: Trying to backtrack to branch "
                    + lastBranch + " but has only " + abox.getBranches().size() + " branches");

            List branches = abox.getBranches();
            branches.subList(lastBranch, branches.size()).clear();
            Branch newBranch = (Branch) branches.get(lastBranch - 1);

            if(ABox.DEBUG)
                System.out.println("Backtracking to branch " + lastBranch + " -> " + newBranch);
            if(lastBranch != newBranch.branch)
                throw new InternalReasonerException("Backtrack: Trying to backtrack to branch "
                    + lastBranch + " but got " + newBranch.branch);

            if(newBranch.tryNext < newBranch.tryCount)
                newBranch.setLastClash( abox.getClash().depends );

            newBranch.tryNext++;

            if(newBranch.tryNext < newBranch.tryCount) {
                restore(newBranch);

                branchFound = newBranch.tryNext();
            }
            else
                abox.getClash().depends.remove(lastBranch);
            if(!branchFound) {
                if(ABox.DEBUG) System.out.println("Failed at branch " + lastBranch);
            }
        }

        return branchFound;
    }

    ABox complete() {
        while(!abox.isComplete() && !abox.isClosed()) {

            while(abox.changed) {
                abox.changed = false;

                if(ABox.DEBUG) {
                    System.out.println("Branch: " + abox.getBranch() + ", Depth: " + abox.treeDepth
                        + ", Size: " + abox.getNodes().size());
                    printBlocked();
                    abox.printTree();
                    abox.validate();
                }
                //Here optimization on separating indiciduals in KB

                IndividualIterator i = abox.getIndIterator();
                if(PelletOptions.USE_OPTIMIZEDINDIVIDUALS == true) {
                    if(((EconnectedKB) abox.getKB()).getCheckAll() == false) {
                        String ont2 = abox.getKB().getOntology();
                        IndividualIterator it = new FilteredIterator(ont2);
                        i = it;
                    }
                }
                timers.startTimer("applyUnfoldingRule");
                applyUnfoldingRule(i);
                timers.stopTimer("applyUnfoldingRule");
                if(abox.isClosed()) break;

                timers.startTimer("applyDisjunctionRule");
                applyDisjunctionRule(i);
                timers.stopTimer("applyDisjunctionRule");
                if(abox.isClosed()) break;

                timers.startTimer("applySomeValuesRule");
                applySomeValuesRule(i);
                timers.stopTimer("applySomeValuesRule");
                if(abox.isClosed()) break;

                timers.startTimer("applyMinRule");
                applyMinRule(i);
                timers.stopTimer("applyMinRule");
                // min rule cannot introduce a clash

                applyNominalRule(i);
                if(abox.isClosed()) break;

                timers.startTimer("applyMaxRule");
                applyMaxRule(i);
                timers.stopTimer("applyMaxRule");
                if(abox.isClosed()) break;

//                if(hasClash()) break;
            }

            if(abox.isClosed()) {
                if(ABox.DEBUG)
                    System.out.println("Clash at Branch (" + abox.getBranch() + ") "
                        + abox.getClash());

                if(backtrack())
                    abox.setClash(null);
                else
                    abox.setComplete(true);
            }
            else
                abox.setComplete(true);
        }//End while

        return abox;
    }
}