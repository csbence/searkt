package edu.unh.cs.ai.realtimesearch.planner.suboptimal;

import com.carrotsearch.hppc.LongObjectOpenHashMap;
import edu.unh.cs.ai.realtimesearch.MetronomeConfigurationException;
import edu.unh.cs.ai.realtimesearch.MetronomeException;
import edu.unh.cs.ai.realtimesearch.environment.Action;
import edu.unh.cs.ai.realtimesearch.environment.Domain;
import edu.unh.cs.ai.realtimesearch.environment.State;
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle;
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentConfiguration;
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker;
import edu.unh.cs.ai.realtimesearch.planner.classical.ClassicalPlanner;
import edu.unh.cs.ai.realtimesearch.util.ErrorEstimator;
import edu.unh.cs.ai.realtimesearch.util.ErrorTraceable;
import edu.unh.cs.ai.realtimesearch.util.collections.binheap.BinHeap;
import edu.unh.cs.ai.realtimesearch.util.collections.gequeue.GEQueue;
import edu.unh.cs.ai.realtimesearch.util.collections.rbtree.RBTreeElement;
import edu.unh.cs.ai.realtimesearch.util.collections.rbtree.RBTreeNode;
import edu.unh.cs.ai.realtimesearch.util.search.SearchQueueElementImpl;
import edu.unh.cs.ai.realtimesearch.util.search.SearchResult;
import edu.unh.cs.ai.realtimesearch.util.search.SearchResultImpl;
import edu.unh.cs.ai.realtimesearch.util.search.SearchResultImpl.SolutionImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ExplicitEstimationSearchH<StateType extends State<StateType>> extends ClassicalPlanner<StateType> {

    private static final int CLEANUP_ID = 0;
    private static final int FOCAL_ID = 1;

    private ErrorEstimator<Node> errorEstimator = new ErrorEstimator<>();

    private LongObjectOpenHashMap<Node> closed =
            new LongObjectOpenHashMap<Node>();

    private double weight;
    private String errorModel;

    private Domain<StateType> domain;

    // cleanup is implemented as a binary heap
    private BinHeap<Node> cleanup =
            new BinHeap<Node>(new CleanupNodeComparator(), CLEANUP_ID);

    // open is implemented as a RedBlack tree
    private OpenNodeComparator openComparator = new OpenNodeComparator();
    private GEQueue<Node> gequeue = new GEQueue<>(openComparator,
            new GENodeComparator(), new FocalNodeComparator(), FOCAL_ID);

    public ExplicitEstimationSearchH(Domain<StateType> domain, ExperimentConfiguration configuration) {
        if (configuration.getWeight() != null) {
            this.weight = configuration.getWeight();
        } else {
            throw new MetronomeConfigurationException();
        }
        if (configuration.getErrorModel() != null) {
            this.errorModel = configuration.getErrorModel();
        } else {
            throw new MetronomeConfigurationException();
        }
        this.domain = domain;
    }

    public ExplicitEstimationSearchH(double weight) {
        this.weight = weight;
    }

    private void calculateStatistics(Node sourceNode, Node successorNode) {
        if (successorNode != null) {
            errorEstimator.addSample(sourceNode, successorNode);
        }
    }

    /* (non-Javadoc)
     * @see edu.unh.ai.search.SearchAlgorithm#search(java.lang.Object)
     */
    public SearchResult search(StateType initialState, TerminationChecker terminationChecker) {
        Node goal = null;
        SearchResultImpl result = new SearchResultImpl();
        result.startTimer();


        Node initNode = new Node(initialState, null, null, null, 0.0);
        insertNode(initNode, initNode);
        gequeue.updateFocal(null, initNode, 0);

        while (!gequeue.isEmpty()) {
            Node oldBest = gequeue.peekOpen();
            Node n = selectNode();
            if (n == null) {
                break;
            }

            StateType state = domain.unpack(n.packed);
            if (domain.isGoal(state)) {
                goal = n;
                break;
            }

            Node bestChild = null;
            result.expanded++;
            setExpandedNodeCount((int) result.expanded);
            Action parentAction = n.pop;
            List<SuccessorBundle<StateType>> successors = domain.successors(state);
            for (SuccessorBundle<StateType> successorBundle : successors) {
                Action op = successorBundle.getAction();
                if (op.equals(n.pop)) {
                    continue;
                }
                result.generated++;
                Node node = new Node(successorBundle.getState(), n, op, parentAction, successorBundle.getActionCost());

                // merge duplicates
                if (closed.containsKey(node.packed)) {
                    result.duplicates++;
                    Node dup = closed.get(node.packed);
                    if (dup.f > node.f) {
                        if (dup.getIndex(CLEANUP_ID) != -1) {
                            gequeue.remove(dup);
                            cleanup.remove(dup);
                            closed.remove(dup.packed);
                        }
                        insertNode(node, oldBest);
                    }
                } else {
                    insertNode(node, oldBest);
                }

                if (bestChild != null) {
                    double previousLowestError = bestChild.f - n.f;
                    double currentError = node.f - n.f;
                    if (currentError < previousLowestError) {
                        bestChild = node;
                    }
                } else {
                    bestChild = node;
                }

            }

            //update the statistics
            calculateStatistics(n, bestChild);

            Node newBest = gequeue.peekOpen();
            int fHatChange = openComparator.compareIgnoreTies(newBest, oldBest);
            gequeue.updateFocal(oldBest, newBest, fHatChange);
        }
        result.stopTimer();

        if (goal != null) {
            SolutionImpl solution = new SolutionImpl();
            List<Action> path = new ArrayList<>();
            for (Node p = goal; p != null; p = p.parent) {
                path.add(p.op);
            }
            Collections.reverse(path);
            solution.addOperators(path);
            solution.setCost(goal.g);
            result.addSolution(solution);
        }
        setExecutionNanoTime(result.getCpuTimeMillis() * 1000000);

        return result;
    }

    private void insertNode(Node node, Node oldBest) {
        gequeue.add(node, oldBest);
        cleanup.add(node);
        closed.put(node.packed, node);
    }

    private Node selectNode() {
        Node value = null;
        Node bestDHat = gequeue.peekFocal();
        Node bestFHat = gequeue.peekOpen();
        Node bestF = cleanup.peek();

        // best dhat
        if (bestDHat.fHat <= weight * bestF.f) {
            value = gequeue.pollFocal();
            cleanup.remove(value);
        }
        // best fhat
        else if (bestFHat.fHat <= weight * bestF.f) {
            value = gequeue.pollOpen();
            cleanup.remove(value);
        }
        // best f
        else {
            value = cleanup.poll();
            gequeue.remove(value);
        }

        return value;
    }

    @NotNull
    @Override
    public List<Action> plan(@NotNull StateType initialState, @NotNull TerminationChecker terminationChecker) {
        List<Action> solution = this.search(initialState, terminationChecker).getSolutions().get(0).getOperators();
        List<Action> returnSolution = new ArrayList<Action>();
        for (Action action : solution) {
            if (action != null) {
                returnSolution.add(action);
            }
        }
        return returnSolution;
    }

    /*
     * The ExplicitEstimationSearchH node is more complicated than other nodes.  It
     * is currently responsible for computing single step error
     * corrections and dhat and hhat values.  Right now we only
     * have path based single step error(SSE) correction implemented.
     *
     * TODO implement other methods for SSE correction and design
     * the necessary abstractions to move this out of the node class.
     */
    private class Node extends SearchQueueElementImpl
            implements RBTreeElement<Node, Node>, Comparable<Node>, ErrorTraceable {

        double f, g, d, h, sseH, sseD, fHat, hHat, dHat;
        int depth;
        Action op, pop;
        Node parent;
        long packed;
        RBTreeNode<Node, Node> rbnode = null;

        private Node(StateType state, Node parent, Action op,
                     final Action pop, final double actionCost) {

            super(2);
            this.packed = domain.pack(state);
            this.parent = parent;
            this.op = op;
            this.pop = pop;

            double cost = (op != null) ? actionCost : 0;
            this.g = cost;
            if (parent != null) {
                this.g += parent.g;
                this.depth = parent.depth + 1;
            }
            this.h = domain.heuristic(state);
            this.d = domain.distance(state);
            this.f = g + h;

            if (errorModel.equals("global")) {
                // global error estimates
                double dMean = errorEstimator.getMeanDistanceError();
                if (dMean < 1) {
                    this.dHat = d / (1 - dMean);
                    this.hHat = h + (this.dHat * errorEstimator.getMeanHeuristicError());
                } else {
                    this.dHat = Double.MAX_VALUE;
                    this.hHat = Double.MAX_VALUE;
                }
                this.fHat = g + hHat;

                assert fHat >= f;
                assert dHat >= 0;
            } else if (errorModel.equals("path")) {
                // path error estimates
                computePathHats(parent, cost);
            } else {
                throw new MetronomeConfigurationException();
            }
        }

        private void computePathHats(Node parent, double edgeCost) {
            if (parent != null) {
                this.sseH = parent.sseH + ((edgeCost + h) - parent.h);
                this.sseD = parent.sseD + ((1 + d) - parent.d);
            }
            this.hHat = computeHHat();
            this.dHat = computeDHat();
            this.fHat = g + hHat;

            assert fHat >= f;
            assert dHat >= 0;
        }

        private double computeHHat() {
            double hHat = Double.MAX_VALUE;
            double sseMean = (g == 0) ? sseH : sseH / depth;
            double dMean = (g == 0) ? sseD : sseD / depth;
            if (dMean < 1) {
                hHat = h + ((d / (1 - dMean)) * sseMean);
            }
            return hHat;
        }

        private double computeDHat() {
            double dHat = Double.MAX_VALUE;
            double dMean = (g == 0) ? sseD : sseD / depth;
            if (dMean < 1) {
                dHat = d / (1 - dMean);
            }
            return dHat;
        }

        @Override
        public int compareTo(Node o) {
            int diff = (int) (this.f - o.f);
            if (diff == 0) return (int) (o.g - this.g);
            return diff;
        }

        @Override
        public RBTreeNode<Node, Node> getNode() {
            return rbnode;
        }

        @Override
        public void setNode(RBTreeNode<Node, Node> node) {
            this.rbnode = node;
        }

        @Override
        public double getF() {
            return this.f;
        }

        @Override
        public double getG() {
            return this.g;
        }

        @Override
        public double getDepth() {
            return this.depth;
        }

        @Override
        public double getH() {
            return this.h;
        }

        @Override
        public double getD() {
            return this.d;
        }

        @Override
        public double getHHat() {
            return this.hHat;
        }

        @Override
        public double getDHat() {
            return this.dHat;
        }

        @Nullable
        @Override
        public Object getParent() {
            return this.parent;
        }

        @Override
        public void setParent(@Nullable Object o) {
            try {
                this.parent = (Node) o;
            } catch (Exception e) {
                throw new MetronomeException();
            }
        }
    }

    /*
     * Used to sort the cleanup list on f.
     */
    private final class CleanupNodeComparator implements Comparator<Node> {
        public int compare(final Node a, final Node b) {
            if (a.f < b.f) return -1;
            else if (a.f > b.f) return 1;
            else if (b.g < a.g) return -1;
            else if (b.g > a.g) return 1;
            return 0;
        }
    }

    /*
     * Used to sort the focal list on dhat.
     */
    private final class FocalNodeComparator implements Comparator<Node> {
        public int compare(final Node a, final Node b) {
            if (a.dHat < b.dHat) return -1;
            else if (a.dHat > b.dHat) return 1;
                // break ties on low fHat
            else if (a.fHat < b.fHat) return -1;
            else if (a.fHat > b.fHat) return 1;
                // break ties on high g
            else if (a.g > b.g) return -1;
            else if (a.g < b.g) return 1;
            return 0;
        }
    }

    /*
     * Used to sort the open list on fhat.
     */
    private final class OpenNodeComparator implements Comparator<Node> {
        public int compare(final Node a, final Node b) {
            if (a.fHat < b.fHat) return -1;
            else if (a.fHat > b.fHat) return 1;
                // break ties on low d
            else if (a.d < b.d) return -1;
            else if (a.d > b.d) return 1;
                // break ties on high g
            else if (a.g > b.g) return -1;
            else if (a.g < b.g) return 1;
            return 0;
        }

        public int compareIgnoreTies(final Node a, final Node b) {
            if (a.fHat < b.fHat) return -1;
            else if (a.fHat > b.fHat) return 1;
            return 0;
        }
    }

    // sort on a.f and b.f'
    private final class GENodeComparator implements Comparator<Node> {
        public int compare(final Node a, final Node b) {
            if (a.fHat < weight * b.fHat) return -1;
            else if (a.fHat > weight * b.fHat) return 1;
            return 0;
        }
    }

}
