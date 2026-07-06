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

import java.util.Collections;
import java.util.Enumeration;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Both javax.servlet-api and jakarta.servlet-api are on this module's test classpath, reproducing the
 * exact apache/skywalking#13938 condition: both namespaces present at once. These tests assert that
 * {@link HttpRequestWrappers#wrap(Object)} selects the wrapper matching the live request type rather
 * than the classpath-presence order that the old exclusive detection got wrong.
 */
public class HttpRequestWrappersTest {

    @Test
    public void wrapJakartaRequest() {
        jakarta.servlet.http.HttpServletRequest request = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(request.getHeader("h")).thenReturn("v");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/uri");
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/uri"));
        when(request.getRemoteHost()).thenReturn("remote");
        when(request.getHeaders("h")).thenReturn(Collections.enumeration(Collections.singletonList("v")));

        HttpRequestWrapper wrapper = HttpRequestWrappers.wrap(request);

        assertTrue(wrapper instanceof HttpRequestWrapper);
        assertEquals("v", wrapper.getHeader("h"));
        assertEquals("GET", wrapper.getMethod());
        assertEquals("/uri", wrapper.getRequestURI());
        assertEquals("http://localhost/uri", wrapper.getRequestURL().toString());
        assertEquals("remote", wrapper.getRemoteHost());
        Enumeration<String> headers = wrapper.getHeaders("h");
        assertEquals("v", headers.nextElement());
    }

    @Test
    public void wrapJavaxRequest() {
        javax.servlet.http.HttpServletRequest request = mock(javax.servlet.http.HttpServletRequest.class);
        when(request.getHeader("h")).thenReturn("v");
        when(request.getMethod()).thenReturn("POST");

        HttpRequestWrapper wrapper = HttpRequestWrappers.wrap(request);

        assertTrue(wrapper instanceof HttpRequestWrapper);
        assertEquals("v", wrapper.getHeader("h"));
        assertEquals("POST", wrapper.getMethod());
    }

    @Test
    public void wrapNonServletReturnsNull() {
        assertNull(HttpRequestWrappers.wrap(new Object()));
        assertNull(HttpRequestWrappers.wrap("not a request"));
        assertNull(HttpRequestWrappers.wrap(null));
    }
}
