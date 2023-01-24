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

package test.apache.skywalking.apm.testcase.toolkit.controller;

import org.apache.skywalking.apm.toolkit.trace.CarrierItemRef;
import org.apache.skywalking.apm.toolkit.trace.Tracer;
import org.apache.skywalking.apm.toolkit.trace.ContextCarrierRef;
import org.apache.skywalking.apm.toolkit.trace.ContextSnapshotRef;
import org.apache.skywalking.apm.toolkit.trace.SpanRef;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class TestService {
    public void startService(String operationName) {
        SpanRef entrySpanRef = Tracer.createEntrySpan(operationName, null);
        Tracer.stopSpan();
    }

    public void startServiceWithCarrier(String operationName, Map<String, String> map) {
        ContextCarrierRef contextCarrierRef = new ContextCarrierRef();
        CarrierItemRef next = contextCarrierRef.items();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (next.hasNext()) {
                next = next.next();
                if (entry.getKey().equals(next.getHeadKey()))
                    next.setHeadValue(entry.getValue());
            }
        }
        SpanRef entrySpanRef = Tracer.createEntrySpan(operationName, contextCarrierRef);
        Tracer.stopSpan();
    }

    public void startServiceWithExtract(String operationName, Map<String, String> map) {
        SpanRef entrySpanRef = Tracer.createEntrySpan(operationName, null);
        entrySpanRef.tag("enter_position", "startServiceWithExtract");
        doExtract(map);
        Tracer.stopSpan();
    }

    public void doSomething(String operationName) {
        SpanRef localSpanRef = Tracer.createLocalSpan(operationName);
        Tracer.stopSpan();
    }

    public void endService(String operationName, String remotePeer) {
        SpanRef exitSpanRef = Tracer.createExitSpan(operationName, remotePeer);
        Tracer.stopSpan();
    }

    public Map<String, String> endServiceWithCarrier(String operationName, String remotePeer) {
        ContextCarrierRef contextCarrierRef = new ContextCarrierRef();
        SpanRef exitSpanRef = Tracer.createExitSpan(operationName, contextCarrierRef, remotePeer);
        exitSpanRef.tag("exit_position", "endServiceWithCarrier");
        Map<String, String> map = new HashMap<>();
        CarrierItemRef next = contextCarrierRef.items();
        while (next.hasNext()) {
            next = next.next();
            map.put(next.getHeadKey(), next.getHeadValue());
        }
        Tracer.stopSpan();
        return map;
    }

    public Map<String, String> endServiceWithInject(String operationName, String remotePeer) {
        SpanRef exitSpanRef = Tracer.createExitSpan(operationName, remotePeer);
        Map<String, String> map = this.doInject();
        Tracer.stopSpan();
        return map;
    }

    public void callNewThread(String operationName) throws InterruptedException {
        ContextSnapshotRef contextSnapshotRef = Tracer.capture();
        Thread thread = new Thread(() -> {
            SpanRef spanRef = Tracer.createLocalSpan(operationName);
            spanRef.log(new RuntimeException("test-Throwable-log"));
            Tracer.continued(contextSnapshotRef);
            Tracer.stopSpan();
        });
        thread.start();
        thread.join();
    }

    private Map<String, String> doInject() {
        ContextCarrierRef contextCarrierRef = new ContextCarrierRef();
        Tracer.inject(contextCarrierRef);
        Map<String, String> map = new HashMap<>();
        CarrierItemRef next = contextCarrierRef.items();
        while (next.hasNext()) {
            next = next.next();
            map.put(next.getHeadKey(), next.getHeadValue());
        }
        return map;
    }

    private void doExtract(Map<String, String> map) {
        ContextCarrierRef contextCarrierRef = new ContextCarrierRef();
        CarrierItemRef next = contextCarrierRef.items();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (next.hasNext()) {
                next = next.next();
                if (entry.getKey().equals(next.getHeadKey()))
                    next.setHeadValue(entry.getValue());
            }
        }
        Tracer.extract(contextCarrierRef);
    }

    public void asyncPrepareAndFinish() throws InterruptedException {
        SpanRef spanRef = Tracer.createLocalSpan("prepare and finish async");
        spanRef.prepareForAsync();
        Tracer.stopSpan();
        Thread thread = new Thread(() -> {
            Map<String, String> logMap = new HashMap<>();
            logMap.put("event", "info");
            logMap.put("message", "in async thread");
            spanRef.log(logMap);
            spanRef.asyncFinish();
        });
        thread.start();
        thread.join();
    }
}