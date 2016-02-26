package edu.unh.cs.ai.realtimesearch.external;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;

/**
 * The search result class.
 * 
 * @author Matthew Hatem
 *
 * @param <T> the state type
 */
public class SearchResult<T> {
  
  private List<T> path;
  private long startWall;
  private long endWall;
  private long expanded;
  private long generated;
  private int initH;
  
  /**
   * The constructor.
   */
  public SearchResult() {
  }
  
  /**
   * The constructor.
   * 
   * @param path the solution path
   * @param expanded the number of nodes expanded
   * @param generated the number of nodes generated
   */
  public SearchResult(List<T> path, long expanded, long generated) {
    this.path = path;
    this.expanded = expanded;
    this.generated = generated;
  }
  
  /**
   * Sets the solution path.
   * 
   * @param path the solution path
   */
  public void setPath(List<T> path) {
    this.path = path;
  }
  
  /**
   * Sets the number of nodes expanded.
   * 
   * @param expanded the number of nodes expanded
   */
  public void setExpanded(long expanded) {
    this.expanded = expanded;
  }
  
  /**
   * Sets the number of nods generated.
   * 
   * @param generated the number of nodes generated
   */
  public void setGenerated(long generated) {
    this.generated = generated;
  }
  
  /**
   * Sets the initial heuristic value.
   * 
   * @param initH the initial heuristic
   */
  public void setInitialH(int initH) {
    this.initH = initH;
  }
  
  /**
   * Returns the solution path.
   * 
   * @return the solution path
   */
  public List<T> getPath() {
    return path;
  }
  
  /**
   * Sets the start time in milliseconds.
   * 
   * @param start the start time
   */
  public void setStartTime(long start) {
    this.startWall = start;
  }
  
  /**
   * Sets the end time in milliseconds.
   * 
   * @param end the end time
   */
  public void setEndTime(long end) {
    this.endWall = end;
  }

  /**
   * Returns the machine Id.
   * 
   * @return the machine id
   */
  public String getMachineId() {
    String uname = "unknown";
    try {
      String switches[] = new String[] {"n", "s", "r", "m"};
      String tokens[] = new String[4];
      for (int i=0; i<switches.length; i++) {
        Process p = Runtime.getRuntime().exec("uname -"+switches[i]);
        p.waitFor();
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(p.getInputStream()));
        tokens[i] = reader.readLine();
      }
      uname = tokens[0]+"-"+tokens[1]+"-"+tokens[2]+"-"+tokens[3];
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    
    return uname;
  }
  
  /**
   * Returns a string representation of the results.
   * 
   * @return a string representation
   */
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("date\"\t\""+new Date(startWall)+"\"\n");
    sb.append("wall start time\"\t\""+startWall+"\"\n");
    sb.append("machine id\"\t\""+getMachineId()+"\"\n");
    sb.append("initial heuristic\"\t\""+initH+"\"\n");
    sb.append("total wall time\"\t\""+((endWall-startWall)/1000.0)+"\"\n");
    sb.append("total cpu time\"\t\""+-1+"\"\n");
    sb.append("total nodes expanded\"\t\""+expanded+"\"\n");
    sb.append("total nodes generated\"\t\""+generated+"\"\n");
    sb.append("solution length\"\t\""+path.size()+"\"\n");    
    sb.append("wall finish date\"\t\""+new Date(endWall)+"\"\n");
    sb.append("wall finish time\"\t\""+endWall+"\"\n"); 
    return sb.toString();
  }
  
}
