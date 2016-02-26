package edu.unh.cs.ai.realtimesearch.external;

import java.util.*;

/**
 * The A* class.
 * 
 * @author Matthew Hatem
 *
 * @param <T> the state type
 */
public final class Astar<T> implements SearchAlgorithm<T> {
  
  private HashMap<T, Node> closed = new HashMap<T, Node>();
  private PriorityQueue<Node> open = 
      new PriorityQueue<Node>(3, new NodeComparator());
  private List<T> path = new ArrayList<T>(3);
  private Domain<T> domain;
  private long expanded;
  private long generated;
  
  /**
   * The constructor.
   * 
   * @param domain the search domain
   */
  public Astar(Domain<T> domain) {
    this.domain = domain;
  }

  private long timestamp = 0l;

  /* (non-Javadoc)
   * @see edu.unh.ai.search.SearchAlgorithm#search(java.lang.Object)
   */
  public SearchResult<T> search(T init) {
    Node initNode = new Node (init, null, 0, 0 -1);    
    open.add(initNode);
    while (!open.isEmpty() && path.isEmpty()) {  
      Node n = open.poll();
      if (closed.containsKey(n.state)) {
        continue;
      }
      if (domain.isGoal(n.state)) {
        for (Node p = n; p != null; p = p.parent) {
            path.add(p.state);
        }
        break;
      }
      closed.put(n.state, n);
      expanded++;
      for (int i = 0; i < domain.numActions(n.state); i++) {
          int op = domain.nthAction(n.state, i);
          if (op == n.pop) {
              continue;
          }
          generated++;
       
          T successor = domain.copy(n.state);
          Edge<T> edge = domain.apply(successor, op);      
          Node node = new Node(successor, n, edge.cost, edge.parentAction);
          open.add(node);
      }

      if (expanded % 100000 == 0) {
        System.out.println(System.currentTimeMillis() - timestamp);
        timestamp = System.currentTimeMillis();
      }
    }
    return new SearchResult<T>(path, expanded, generated);
  }
  
  /*
   * The node class
   */
  private final class Node {
    final int f, g, pop;
    final Node parent;
    final T state;
    private Node (T state, Node parent, int cost, int pop) {
      this.g = (parent != null) ? parent.g+cost : cost;
      this.f = g + domain.h(state);
      this.pop = pop;
      this.parent = parent;
      this.state = state;
    } 
  }
  
  /*
   * The node comparator class
   */
  private final class NodeComparator implements Comparator<Node> {
    public int compare(Node a, Node b) {
      if (a.f == b.f) { 
        if (a.g > b.g) return -1;
        if (a.g < b.g) return 1;
        return 0;
      }
      else {
        if (a.f < b.f) return -1;
        if (a.f > b.f) return 1;
        return 0;
      }
    }    
  }
  
}
