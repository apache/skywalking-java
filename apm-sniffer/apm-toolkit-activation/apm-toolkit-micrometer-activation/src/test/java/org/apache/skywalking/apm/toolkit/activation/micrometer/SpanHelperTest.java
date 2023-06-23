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

package org.apache.skywalking.apm.toolkit.activation.micrometer;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SpanHelperTest {

    @Test
    public void testTryToGetPeer() {
        String remoteName = SpanHelper.tryToGetPeer("http://localhost:8080", "remoteName",
            KeyValues.of(KeyValue.of("http.url", "http://localhost:8081")));
        assertThat(remoteName, is("http://localhost:8080"));
    }

    @Test
    public void testTryToGetPeerWhenRemoteAddressIsNull() {
        String remoteName = SpanHelper.tryToGetPeer(null, "remoteName",
            KeyValues.of(KeyValue.of("http.url", "http://localhost:8080")));
        assertThat(remoteName, is("localhost:8080"));
    }

    @Test
    public void testTryToGetPeerWhenURIWithQueryString() {
        String remoteName = SpanHelper.tryToGetPeer(null, "remoteName",
            KeyValues.of(KeyValue.of("http.url", "http://localhost:8080?a=b")));
        assertThat(remoteName, is("localhost:8080"));
    }

    @Test
    public void testTryToGetPeerWhenLackOfURIComponents() {
        String remoteName = SpanHelper.tryToGetPeer(null, "remoteName",
            KeyValues.empty());
        assertThat(remoteName, is("remoteName"));
    }

    @Test
    public void testTryToGetPeerWhenURIViolatesRFC2396() {
        String remoteName = SpanHelper.tryToGetPeer(null, "remoteName",
            KeyValues.of(KeyValue.of("http.url", "https://www.example.com/path/with spaces")));
        assertThat(remoteName, is("remoteName"));
    }

}
