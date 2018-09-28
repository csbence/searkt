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

import java.util.List;

/**
 * The search result interface.
 *
 * @author Matthew Hatem
 */
public interface SearchResult {

    /**
     * Returns the solution path.
     *
     * @return the solution path
     */
    List<Solution> getSolutions();

    /**
     * Returns expanded count.
     *
     * @return expanded count
     */
    double getExpanded();

    /**
     * Returns generated count.
     *
     * @return generated count
     */
    double getGenerated();

    /**
     * Returns the wall time in milliseconds.
     *
     * @return the wall time in milliseconds
     */
    long getWallTimeMillis();

    /**
     * Returns the CPU time in milliseconds.
     *
     * @return the CPU time in milliseconds
     */
    long getCpuTimeMillis();

    /**
     * Interface for search iterations.
     */
    interface Iteration {

        /**
         * Returns the bound for this iteration.
         *
         * @return the bound
         */
        double getBound();

        /**
         * Returns the number of nodes expanded.
         *
         * @return the number of nodes expanded
         */
        long getExpanded();

        /**
         * Returns the number of nodes generated.
         *
         * @return the number of nodes generated
         */
        long getGenerated();

    }

    /**
     * The Solution interface.
     */
    interface Solution<State> {

        /**
         * Returns a list of operators used to construct this solution.
         *
         * @return list of operators
         */
        List<Operator<State>> getOperators();

        /**
         * Returns the cost of the solution.
         *
         * @return the cost of the solution
         */
        double getCost();

        /**
         * Returns the length of the solution.
         */
        int getLength();

    }

}
