package edu.unh.cs.ai.realtimesearch.external;

/**
 * The edge class.
 * 
 * @author Matthew Hatem
 *
 * @param <T>
 */
public class Edge<T> {
  
  /**
   * The action used to generate the resulting state.
   */
  public int action;   
  
  /**
   * The action used to generate the originating state.
   */
  public int parentAction;  
  
  /**
   * The cost of traversing this edge.
   */
  public int cost; 
  
  /**
   * The constructor.
   * 
   * @param cost the cost of traversing this edge
   * @param action the action used to generate the resulting state
   * @param parentAction the action used to generate the originating state
   */
  public Edge(int cost, int action, int parentAction) { 
    this.cost = cost;
    this.action = action;
    this.parentAction = parentAction;
  }
  
}
