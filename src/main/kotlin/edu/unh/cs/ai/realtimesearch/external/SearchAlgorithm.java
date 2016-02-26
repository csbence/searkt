package edu.unh.cs.ai.realtimesearch.external;

/**
 * The search algorithm interface.
 * 
 * @author Matthew Hatem
 *
 * @param <T> the state type
 */
public interface SearchAlgorithm<T> {
  
  /**
   * Performs a search beginning at the specified state.
   * 
   * @param state the initial state
   * @return search results
   */
  public SearchResult<T> search(T state);
    
}
