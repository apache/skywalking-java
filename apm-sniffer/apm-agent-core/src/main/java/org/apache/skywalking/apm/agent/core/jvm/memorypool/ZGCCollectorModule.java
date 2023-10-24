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

package org.apache.skywalking.apm.agent.core.jvm.memorypool;

import org.apache.skywalking.apm.network.language.agent.v3.MemoryPool;
import org.apache.skywalking.apm.network.language.agent.v3.PoolType;

import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.LinkedList;
import java.util.List;

public class ZGCCollectorModule implements MemoryPoolMetricsAccessor {

    private final List<MemoryPoolMXBean> beans;

    public ZGCCollectorModule(List<MemoryPoolMXBean> beans) {
        this.beans = beans;
    }

    @Override
    public List<MemoryPool> getMemoryPoolMetricsList() {
        List<MemoryPool> poolList = new LinkedList<>();
        for (MemoryPoolMXBean bean : beans) {
            String name = bean.getName();
            PoolType type;
            if (name.equals("ZHeap")) {
                type = PoolType.ZHEAP_USAGE;
            } else if (name.equals("Metaspace")) {
                type = PoolType.METASPACE_USAGE;
            } else if (name.equals("Compressed Class Space")) {
                type = PoolType.COMPRESSED_CLASS_SPACE_USAGE;
            } else if (name.equals("CodeHeap 'non-nmethods'")) {
                type = PoolType.CODEHEAP_NON_NMETHODS_USAGE;
            } else if (name.equals("CodeHeap 'profiled nmethods'")) {
                type = PoolType.CODEHEAP_PROFILED_NMETHODS_USAGE;
            } else if (name.equals("CodeHeap 'non-profiled nmethods'")) {
                type = PoolType.CODEHEAP_NON_PROFILED_NMETHODS_USAGE;
            } else {
                continue;
            }

            MemoryUsage usage = bean.getUsage();
            poolList.add(MemoryPool.newBuilder()
                    .setType(type)
                    .setInit(usage.getInit())
                    .setMax(usage.getMax())
                    .setCommitted(usage.getCommitted())
                    .setUsed(usage.getUsed())
                    .build());
        }
        return poolList;
    }
}