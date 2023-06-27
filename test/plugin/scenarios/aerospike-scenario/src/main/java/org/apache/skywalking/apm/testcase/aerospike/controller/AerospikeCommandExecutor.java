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

package org.apache.skywalking.apm.testcase.aerospike.controller;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Operation;
import com.aerospike.client.policy.WritePolicy;

public class AerospikeCommandExecutor implements AutoCloseable {
    private final AerospikeClient client;
    private final WritePolicy defaultWritePolicy = new WritePolicy();
    private final WritePolicy defaultReadPolicy = new WritePolicy();
    private static final String VALUENAME = "DATA";
    private static final String NAMESPACE = "test";
    private static final String SETNAME = "skywalking";

    public AerospikeCommandExecutor(String host, Integer port) {
        client = new AerospikeClient(host, port);
    }

    public void set(String key, String value) {
        final Key k = new Key(NAMESPACE, SETNAME, key);
        final Bin bin = new Bin(VALUENAME, value);

        client.put(defaultWritePolicy, k, bin);
    }

    public void exists(String key) {
        final Key k = new Key(NAMESPACE, SETNAME, key);

        client.exists(null, k);
    }

    public void get(String key) {
        final Key k = new Key(NAMESPACE, SETNAME, key);

        client.get(defaultReadPolicy, k);
    }

    public void append(String key, String value) {
        final Key k = new Key(NAMESPACE, SETNAME, key);
        final Bin bin = new Bin(VALUENAME, value);

        client.append(defaultWritePolicy, k, bin);
    }

    public void operate(String key, long by) {
        final Key k = new Key(NAMESPACE, SETNAME, key);

        client.operate(defaultWritePolicy, k, Operation.add(new Bin(VALUENAME, by)), Operation.get(VALUENAME));
    }

    public void delete(String key) {
        final Key k = new Key(NAMESPACE, SETNAME, key);

        client.delete(null, k);
    }

    public void close() throws Exception {
        client.close();
    }
}