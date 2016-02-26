package edu.unh.cs.ai.realtimesearch.external;

/**
 * The search domain interface.
 * 
 * @author Matthew Hatem
 *
 * @param <T> state type
 */
public interface Domain<T> {
  
  /**
   * Returns the initial state.
   * 
   * @return the initial state
   */
  public T initial();
  
  /**
   * Returns the heuristic value for the specified state.
   * 
   * @param state the state 
   * @return the heuristic value
   */
  public int h(T state);
  
  /**
   * Returns true if the specified state is the goal state, false otherwise.
   * 
   * @param state the state
   * @return true if s is a goal state, false otherwise
   */
  public boolean isGoal(T state);
  
  /**
   * Returns the number of operators applicable for the specified state.
   * 
   * @param state the state
   * @return  the number of operators 
   */
  public int numActions(T state);
  
  /**
   * Returns the nth operator applicable for the specified state.
   * 
   * @param state the state
   * @param nth the nth operator index
   * @return the nth operator
   */
  public int nthAction(T state, int nth);
    
  /**
   * Applies the specified operator to the specified state and updates the
   * specified edge.
   * 
   * @param edge the edge
   * @param op the operator
   */
  public Edge<T> apply(T state, int op);
  
  /**
   * Returns a copy of the specified state.
   * 
   * @param state the state
   * @return the copy
   */
  public T copy(T state);
   
}
