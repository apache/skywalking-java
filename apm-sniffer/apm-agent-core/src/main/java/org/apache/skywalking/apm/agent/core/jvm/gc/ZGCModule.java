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

package org.apache.skywalking.apm.agent.core.jvm.gc;

import org.apache.skywalking.apm.network.language.agent.v3.GC;
import org.apache.skywalking.apm.network.language.agent.v3.GCPhase;

import java.lang.management.GarbageCollectorMXBean;
import java.util.LinkedList;
import java.util.List;

public class ZGCModule implements GCMetricAccessor {
    private List<GarbageCollectorMXBean> beans;

    private long lastNormalGCCount = 0;
    private long lastNormalGCTime = 0;

    public ZGCModule(List<GarbageCollectorMXBean> beans) {
        this.beans = beans;
    }

    @Override
    public List<GC> getGCList() {
        List<GC> gcList = new LinkedList<GC>();
        for (GarbageCollectorMXBean bean : beans) {
            String name = bean.getName();
            long gcCount = 0;
            long gcTime = 0;
            if (name.equals("ZGC")) {
                long collectionCount = bean.getCollectionCount();
                gcCount = collectionCount - lastNormalGCCount;
                lastNormalGCCount = collectionCount;

                long time = bean.getCollectionTime();
                gcTime = time - lastNormalGCTime;
                lastNormalGCTime = time;
            } else if (name.equals("ZGC Cycles")) {
                long collectionCount = bean.getCollectionCount();
                gcCount = collectionCount - lastNormalGCCount;
                lastNormalGCCount = collectionCount;
            } else if (name.equals("ZGC Pauses")) {
                long time = bean.getCollectionTime();
                gcTime = time - lastNormalGCTime;
                lastNormalGCTime = time;
            } else {
                continue;
            }
            gcList.add(GC.newBuilder().setPhase(GCPhase.NORMAL).setCount(gcCount).setTime(gcTime).build());
        }

        return gcList;
    }
}
