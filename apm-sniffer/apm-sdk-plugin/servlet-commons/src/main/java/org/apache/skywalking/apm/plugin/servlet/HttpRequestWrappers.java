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

import java.util.Enumeration;
import java.util.Map;

/**
 * Factory producing a {@link HttpRequestWrapper} from either a {@code javax.servlet} or a
 * {@code jakarta.servlet} {@code HttpServletRequest}, selected at runtime by the concrete type of
 * the argument. Both servlet namespaces may be present on the classpath at once (e.g. a Jakarta
 * application that also carries {@code javax.servlet-api} transitively); the flags below are
 * computed independently so the live request type — not classpath presence order — decides.
 * <p>
 * CLASSLOADING INVARIANT — DO NOT VIOLATE: every {@code javax}/{@code jakarta} reference lives
 * inside its own nested wrapper class and is reached only through its {@link #IS_JAVAX} /
 * {@link #IS_JAKARTA} flag guarded by a {@code &&} short-circuit. Never move a servlet type into
 * {@link #wrap(Object)}'s shared body, never widen a method descriptor of this class to a servlet
 * type, and never add a typed public {@code wrap(javax...)}/{@code wrap(jakarta...)} overload. Any
 * of those makes the JVM eagerly resolve an absent namespace and reintroduces
 * {@code NoClassDefFoundError} in single-namespace applications. Lazy constant-pool resolution
 * keeps the guarded-but-unexecuted branch safe.
 */
public final class HttpRequestWrappers {

    private static final boolean IS_JAVAX = classExists("javax.servlet.http.HttpServletRequest");
    private static final boolean IS_JAKARTA = classExists("jakarta.servlet.http.HttpServletRequest");

    private HttpRequestWrappers() {
    }

    /**
     * @param request any object; typically the servlet request captured by a plugin.
     * @return a wrapper when {@code request} is a javax or jakarta {@code HttpServletRequest}, or
     * {@code null} otherwise (e.g. a reactive request or an unrecognized type), letting the caller
     * fall through to its own non-servlet handling.
     */
    public static HttpRequestWrapper wrap(Object request) {
        if (IS_JAVAX && request instanceof javax.servlet.http.HttpServletRequest) {
            return new JavaxHttpServletRequest((javax.servlet.http.HttpServletRequest) request);
        }
        if (IS_JAKARTA && request instanceof jakarta.servlet.http.HttpServletRequest) {
            return new JakartaHttpServletRequest((jakarta.servlet.http.HttpServletRequest) request);
        }
        return null;
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className, false, HttpRequestWrappers.class.getClassLoader());
            return true;
        } catch (Throwable ignore) {
            return false;
        }
    }

    private static final class JavaxHttpServletRequest implements HttpRequestWrapper {
        private final javax.servlet.http.HttpServletRequest request;

        private JavaxHttpServletRequest(javax.servlet.http.HttpServletRequest request) {
            this.request = request;
        }

        @Override
        public String getHeader(String name) {
            return request.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return request.getHeaders(name);
        }

        @Override
        public String getMethod() {
            return request.getMethod();
        }

        @Override
        public String getRequestURI() {
            return request.getRequestURI();
        }

        @Override
        public StringBuffer getRequestURL() {
            return request.getRequestURL();
        }

        @Override
        public String getRemoteHost() {
            return request.getRemoteHost();
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return request.getParameterMap();
        }
    }

    private static final class JakartaHttpServletRequest implements HttpRequestWrapper {
        private final jakarta.servlet.http.HttpServletRequest request;

        private JakartaHttpServletRequest(jakarta.servlet.http.HttpServletRequest request) {
            this.request = request;
        }

        @Override
        public String getHeader(String name) {
            return request.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return request.getHeaders(name);
        }

        @Override
        public String getMethod() {
            return request.getMethod();
        }

        @Override
        public String getRequestURI() {
            return request.getRequestURI();
        }

        @Override
        public StringBuffer getRequestURL() {
            return request.getRequestURL();
        }

        @Override
        public String getRemoteHost() {
            return request.getRemoteHost();
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return request.getParameterMap();
        }
    }
}
