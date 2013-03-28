/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.giraph.hive.input.vertex;

import org.apache.giraph.conf.BooleanConfOption;
import org.apache.giraph.conf.ImmutableClassesGiraphConfiguration;
import org.apache.giraph.edge.Edge;
import org.apache.giraph.graph.Vertex;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;

import com.facebook.giraph.hive.HiveReadableRecord;
import com.facebook.giraph.hive.HiveRecord;

import java.util.Iterator;

/**
 * Simple implementation of {@link HiveToVertex} when each vertex is in the one
 * row of the input.
 *
 * @param <I> Vertex ID
 * @param <V> Vertex Value
 * @param <E> Edge Value
 * @param <M> Message Value
 */
public abstract class SimpleHiveToVertex<I extends WritableComparable,
    V extends Writable, E extends Writable, M extends Writable>
    extends AbstractHiveToVertex<I, V, E, M> {
  /** Configuration option for whether to reuse vertex */
  public static final BooleanConfOption REUSE_VERTEX_KEY =
      new BooleanConfOption("giraph.hive.reuse.vertex", false);
  /** Hive records which we are reading from */
  private Iterator<HiveRecord> records;

  /** Reusable vertex object */
  private Vertex<I, V, E, M> reusableVertex = null;

  /**
   * Read the Vertex's ID from the HiveRecord given.
   *
   * @param record HiveRecord to read from.
   * @return Vertex ID
   */
  public abstract I getVertexId(HiveReadableRecord record);

  /**
   * Read the Vertex's Value from the HiveRecord given.
   *
   * @param record HiveRecord to read from.
   * @return Vertex Value
   */
  public abstract V getVertexValue(HiveReadableRecord record);

  /**
   * Read Vertex's edges from the HiveRecord given.
   *
   * @param record HiveRecord to read from.
   * @return iterable of edges
   */
  public abstract Iterable<Edge<I, E>> getEdges(HiveReadableRecord record);

  @Override
  public void setConf(ImmutableClassesGiraphConfiguration<I, V, E, M> conf) {
    super.setConf(conf);
    if (REUSE_VERTEX_KEY.get(conf)) {
      reusableVertex = getConf().createVertex();
    }
  }

  @Override
  public void initializeRecords(Iterator<HiveRecord> records) {
    this.records = records;
  }

  @Override
  public boolean hasNext() {
    return records.hasNext();
  }

  @Override
  public Vertex<I, V, E, M> next() {
    HiveRecord record = records.next();
    I id = getVertexId(record);
    V value = getVertexValue(record);
    Iterable<Edge<I, E>> edges = getEdges(record);
    Vertex<I, V, E, M> vertex = reusableVertex;
    if (vertex == null) {
      vertex = getConf().createVertex();
    }
    vertex.initialize(id, value, edges);
    return vertex;
  }
}
