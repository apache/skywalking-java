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

package org.apache.skywalking.apm.plugin.spring.mvc.commons;

import java.util.Enumeration;
import java.util.Map;

public class HttpServletRequestWrappers {

    public static HttpServletRequestWrapper wrap(jakarta.servlet.http.HttpServletRequest request) {
        return new JakartaHttpServletRequest(request);
    }

    public static HttpServletRequestWrapper wrap(javax.servlet.http.HttpServletRequest request) {
        return new JavaxHttpServletRequest(request);
    }

    public static class JakartaHttpServletRequest implements HttpServletRequestWrapper {

        private jakarta.servlet.http.HttpServletRequest jakartaRequest;

        public JakartaHttpServletRequest(jakarta.servlet.http.HttpServletRequest jakartaRequest) {
            this.jakartaRequest = jakartaRequest;
        }

        @Override
        public String getHeader(String name) {
            return jakartaRequest.getHeader(name);
        }

        @Override
        public String getMethod() {
            return jakartaRequest.getMethod();
        }

        @Override
        public StringBuffer getRequestURL() {
            return jakartaRequest.getRequestURL();
        }

        @Override
        public String getRemoteHost() {
            return jakartaRequest.getRemoteHost();
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return jakartaRequest.getParameterMap();
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return jakartaRequest.getHeaders(name);
        }

    }

    public static class JavaxHttpServletRequest implements HttpServletRequestWrapper {
        private javax.servlet.http.HttpServletRequest javaxRequest;

        public JavaxHttpServletRequest(javax.servlet.http.HttpServletRequest javaxRequest) {
            this.javaxRequest = javaxRequest;
        }

        @Override
        public String getHeader(String name) {
            return javaxRequest.getHeader(name);
        }

        @Override
        public String getMethod() {
            return javaxRequest.getMethod();
        }

        @Override
        public StringBuffer getRequestURL() {
            return javaxRequest.getRequestURL();
        }

        @Override
        public String getRemoteHost() {
            return javaxRequest.getRemoteHost();
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return javaxRequest.getParameterMap();
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return javaxRequest.getHeaders(name);
        }

    }
}
