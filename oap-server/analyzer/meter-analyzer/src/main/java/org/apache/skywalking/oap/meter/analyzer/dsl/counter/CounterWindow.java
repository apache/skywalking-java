/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.oap.meter.analyzer.dsl.counter;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * CounterWindow stores a series of counter samples in order to calculate the increase
 * or instant rate of increase.
 *
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@EqualsAndHashCode
public class CounterWindow {

    public static final CounterWindow INSTANCE = new CounterWindow();

    private final Map<ID, Tuple2<Long, Double>> lastElementMap = Maps.newHashMap();
    private final Map<ID, Queue<Tuple2<Long, Double>>> windows = Maps.newHashMap();

    public Tuple2<Long, Double> increase(String name, ImmutableMap<String, String> labels, Double value, long windowSize, long now) {
        ID id = new ID(name, labels);
        if (!windows.containsKey(id)) {
            windows.put(id, new PriorityQueue<>());
        }

        Queue<Tuple2<Long, Double>> window = windows.get(id);
        window.offer(Tuple.of(now, value));
        long waterLevel = now - windowSize;
        Tuple2<Long, Double> peek = window.peek();
        if (peek._1 > waterLevel) {
            return peek;
        }

        Tuple2<Long, Double> result = peek;
        while (peek._1 < waterLevel) {
            result = window.poll();
            peek = window.element();
        }

        // Choose the closed slot to the expected timestamp
        if (waterLevel - result._1 <= peek._1 - waterLevel) {
            return result;
        }

        return peek;
    }

    public Tuple2<Long, Double> pop(String name, ImmutableMap<String, String> labels, Double value, long now) {
        ID id = new ID(name, labels);

        Tuple2<Long, Double> element = Tuple.of(now, value);
        Tuple2<Long, Double> result = lastElementMap.get(id);
        lastElementMap.put(id, element);
        if (result == null) {
            return element;
        }
        return result;
    }

    public void reset() {
        windows.clear();
    }
}
