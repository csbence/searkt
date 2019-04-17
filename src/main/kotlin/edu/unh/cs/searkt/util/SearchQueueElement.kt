/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unh.cs.searkt.util

/**
 * The SearchQueueElement interface (see intrusive data structures)
 *
 * @author Matthew Hatem
 */
interface SearchQueueElement<T> {

    /**
     *
     * @return Elements F value;
     */
    val f: Double

    /**
     *
     * @return Elements G value;
     */
    val g: Double

    /**
     *
     * @return Elements Depth value;
     */
    val depth: Double

    /**
     *
     * @return Elements H value;
     */
    val h: Double

    /**
     *
     * @return Elements D value;
     */
    val d: Double

    /**
     *
     * @return Elements Hhat value;
     */
    val hHat: Double

    /**
     *
     * @return Elements Dhat value;
     */
    val dHat: Double

    /**
     *
     * @return Elements parent;
     */
    var parent: T?

    /**
     * Sets the indexes of this object in the data structure.
     *
     * @param key
     * @param index
     */
    fun setIndex(key: Int, index: Int)

    /**
     * Returns the index of this object in the data structure.
     *
     * @return
     */
    fun getIndex(key: Int): Int
}
