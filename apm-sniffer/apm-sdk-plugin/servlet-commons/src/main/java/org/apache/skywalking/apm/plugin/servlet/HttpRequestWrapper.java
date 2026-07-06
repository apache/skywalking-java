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
 * A namespace-agnostic view over a servlet HTTP request. It abstracts away the javax.servlet vs
 * jakarta.servlet split so servlet plugin code can be written once. Obtain instances via
 * {@link HttpRequestWrappers#wrap(Object)}.
 * <p>
 * The method set is the union of what every servlet plugin actually reads; it intentionally does
 * not expose cookies, sessions, multipart parts or the servlet context, none of which are collected.
 */
public interface HttpRequestWrapper {

    String getHeader(String name);

    Enumeration<String> getHeaders(String name);

    String getMethod();

    String getRequestURI();

    StringBuffer getRequestURL();

    String getRemoteHost();

    Map<String, String[]> getParameterMap();
}
