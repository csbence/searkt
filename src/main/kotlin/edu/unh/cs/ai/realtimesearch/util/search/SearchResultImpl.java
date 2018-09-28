/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unh.cs.ai.realtimesearch.util.search;

import edu.unh.cs.ai.realtimesearch.environment.Operator;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

/**
 * The search result class.
 *
 * @author Matthew Hatem
 */
public class SearchResultImpl implements SearchResult {

    public long expanded;
    public long generated;
    public long duplicates;
    public long reopened;
    private long startWallTimeMillis;
    private long startCpuTimeMillis;
    private long stopWallTimeMillis;
    private long stopCpuTimeMillis;
    private List<Iteration> iterations = new ArrayList<>();
    private List<Solution> solutions = new ArrayList<>();

    @Override
    public double getExpanded() {
        return this.expanded;
    }

    public void setExpanded(long expanded) {
        this.expanded = expanded;
    }

    @Override
    public double getGenerated() {
        return this.generated;
    }

    public void setGenerated(long generated) {
        this.generated = generated;
    }

    @Override
    public List<Solution> getSolutions() {
        return solutions;
    }

    @Override
    public long getWallTimeMillis() {
        return stopWallTimeMillis - startWallTimeMillis;
    }

    @Override
    public long getCpuTimeMillis() {
        return (long) ((stopCpuTimeMillis - startCpuTimeMillis) * 0.000001);
    }

    public void addSolution(Solution solution) {
        solutions.add(solution);
    }

    public void addIteration(int i, double bound, long expanded, long generated) {
        iterations.add(new Iteration(bound, expanded, generated));
    }

    public void startTimer() {
        this.startWallTimeMillis = System.currentTimeMillis();
        this.startCpuTimeMillis = getCpuTime();
    }

    public void stopTimer() {
        this.stopWallTimeMillis = System.currentTimeMillis();
        this.stopCpuTimeMillis = getCpuTime();
    }

    public long getCpuTime() {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        return bean.isCurrentThreadCpuTimeSupported() ?
                bean.getCurrentThreadCpuTime() : -1L;
    }

    /*
     * Returns the machine Id
     */
  /*private String getMachineId() {
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
  }*/

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Nodes Generated: ");
        sb.append(generated);
        sb.append("\n");
        sb.append("Nodes Expanded: ");
        sb.append(expanded);
        sb.append("\n");
        sb.append("Total Wall Time: " + this.getWallTimeMillis());
        sb.append("\n");
        sb.append("Total CPU Time: " + this.getCpuTimeMillis());
        sb.append("\n");
        sb.append(solutions.get(0));
        return sb.toString();
    }

    /*
     * The iteration class.
     */
    private static class Iteration implements SearchResult.Iteration {
        private double b;
        private long e, g;

        public Iteration(double b, long e, long g) {
            this.b = b;
            this.e = e;
            this.g = g;
        }

        @Override
        public double getBound() {
            return b;
        }

        @Override
        public long getExpanded() {
            return e;
        }

        @Override
        public long getGenerated() {
            return g;
        }
    }

    public static class SolutionImpl<State> implements Solution {

        private double cost;
        private List<Operator<State>> operators = new ArrayList<>();

        @Override
        public List<Operator<State>> getOperators() {
            return operators;
        }

        @Override
        public double getCost() {
            return cost;
        }

        public void setCost(double cost) {
            this.cost = cost;
        }

        @Override
        public int getLength() {
            return operators.size();
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("Cost: ");
            sb.append(getCost());
            sb.append("\n");
            sb.append("Length: ");
            sb.append(getLength());
            sb.append("\n");
            return sb.toString();
        }

        public void addOperator(Operator<State> operator) {
            this.operators.add(operator);
        }

        public void addOperators(List<Operator<State>> operators) {
            this.operators.addAll(operators);
        }

    }

}
