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

package org.apache.skywalking.apm.plugin.servlet;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpResponseWrappersTest {

    @Test
    public void wrapJakartaResponse() {
        jakarta.servlet.http.HttpServletResponse response = mock(jakarta.servlet.http.HttpServletResponse.class);
        when(response.getStatus()).thenReturn(503);

        HttpResponseWrapper wrapper = HttpResponseWrappers.wrap(response);

        assertNotNull(wrapper);
        assertEquals(503, wrapper.getStatus());
    }

    @Test
    public void wrapJavaxResponse() {
        javax.servlet.http.HttpServletResponse response = mock(javax.servlet.http.HttpServletResponse.class);
        when(response.getStatus()).thenReturn(200);

        HttpResponseWrapper wrapper = HttpResponseWrappers.wrap(response);

        assertNotNull(wrapper);
        assertEquals(200, wrapper.getStatus());
    }

    @Test
    public void servletStatusSupportedWhenApiPresent() {
        // Both servlet APIs (with getStatus, added in Servlet 3.0) are on the test classpath.
        assertTrue(HttpResponseWrappers.servletStatusSupported());
    }

    @Test
    public void wrapNonServletReturnsNull() {
        assertNull(HttpResponseWrappers.wrap(new Object()));
        assertNull(HttpResponseWrappers.wrap(null));
    }
}
