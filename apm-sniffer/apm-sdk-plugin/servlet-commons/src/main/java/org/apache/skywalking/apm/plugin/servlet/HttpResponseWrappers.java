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

/**
 * Factory producing a {@link HttpResponseWrapper} from either a {@code javax.servlet} or a
 * {@code jakarta.servlet} {@code HttpServletResponse}, selected at runtime.
 * <p>
 * Detection deliberately checks that the {@code getStatus()} method exists (added in Servlet 3.0),
 * not merely that the response class is present, preserving the guard for pre-3.0 containers where
 * status is unavailable. {@link #servletStatusSupported()} lets a caller gate a non-servlet (e.g.
 * reactive) status fallback on the same capability.
 * <p>
 * The same CLASSLOADING INVARIANT as {@link HttpRequestWrappers} applies: keep every
 * {@code javax}/{@code jakarta} reference inside its nested class behind its {@code &&}-guarded
 * flag, and never widen this class's method descriptors to a servlet type.
 */
public final class HttpResponseWrappers {

    private static final boolean IS_JAVAX_STATUS = statusMethodExists("javax.servlet.http.HttpServletResponse");
    private static final boolean IS_JAKARTA_STATUS = statusMethodExists("jakarta.servlet.http.HttpServletResponse");

    private HttpResponseWrappers() {
    }

    /**
     * @return {@code true} when a servlet {@code getStatus()} is available in either namespace.
     */
    public static boolean servletStatusSupported() {
        return IS_JAVAX_STATUS || IS_JAKARTA_STATUS;
    }

    /**
     * @param response any object; typically the servlet response captured by a plugin.
     * @return a wrapper when {@code response} is a javax or jakarta {@code HttpServletResponse} that
     * exposes {@code getStatus()}, or {@code null} otherwise (e.g. a reactive response), letting the
     * caller fall through to its own handling.
     */
    public static HttpResponseWrapper wrap(Object response) {
        if (IS_JAVAX_STATUS && response instanceof javax.servlet.http.HttpServletResponse) {
            return new JavaxHttpServletResponse((javax.servlet.http.HttpServletResponse) response);
        }
        if (IS_JAKARTA_STATUS && response instanceof jakarta.servlet.http.HttpServletResponse) {
            return new JakartaHttpServletResponse((jakarta.servlet.http.HttpServletResponse) response);
        }
        return null;
    }

    private static boolean statusMethodExists(String className) {
        try {
            Class<?> clazz = Class.forName(className, false, HttpResponseWrappers.class.getClassLoader());
            clazz.getMethod("getStatus");
            return true;
        } catch (Throwable ignore) {
            return false;
        }
    }

    private static final class JavaxHttpServletResponse implements HttpResponseWrapper {
        private final javax.servlet.http.HttpServletResponse response;

        private JavaxHttpServletResponse(javax.servlet.http.HttpServletResponse response) {
            this.response = response;
        }

        @Override
        public int getStatus() {
            return response.getStatus();
        }
    }

    private static final class JakartaHttpServletResponse implements HttpResponseWrapper {
        private final jakarta.servlet.http.HttpServletResponse response;

        private JakartaHttpServletResponse(jakarta.servlet.http.HttpServletResponse response) {
            this.response = response;
        }

        @Override
        public int getStatus() {
            return response.getStatus();
        }
    }
}
