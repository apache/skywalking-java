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

package org.apache.skywalking.apm.plugin.spring.ldap;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;

public final class LdapEndpointResolver {

    private static final int MAX_UNWRAP_DEPTH = 8;

    private static final int PORT_UNSPECIFIED = -1;

    private static final int PORT_INVALID = -2;

    private static final int MIN_PORT = 1;

    private static final int MAX_PORT = 65535;

    private LdapEndpointResolver() {
    }

    public static SpringLdapEnhanceInfo resolve(Object contextSource) {
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        Object current = contextSource;
        for (int depth = 0; current != null && depth < MAX_UNWRAP_DEPTH && visited.add(current); depth++) {
            SpringLdapEnhanceInfo enhancedInfo = enhancedInfo(current);
            if (enhancedInfo != null) {
                return enhancedInfo;
            }

            String peer = peerFromUrls(invokeNoArg(current, "getUrls"));
            if (peer != null) {
                return new SpringLdapEnhanceInfo(peer);
            }

            Object delegate = invokeNoArg(current, "getContextSource");
            if (delegate == null || delegate == current) {
                delegate = invokeNoArg(current, "getTarget");
            }
            current = delegate;
        }
        return new SpringLdapEnhanceInfo(SpringLdapConstants.UNKNOWN_PEER);
    }

    static String peerFromUrls(Object urls) {
        if (urls == null) {
            return null;
        }

        Set<String> peers = new LinkedHashSet<String>();
        if (urls.getClass().isArray()) {
            for (int i = 0; i < Array.getLength(urls); i++) {
                addPeer(peers, Array.get(urls, i));
            }
        } else if (urls instanceof Iterable) {
            for (Object url : (Iterable<?>) urls) {
                addPeer(peers, url);
            }
        } else {
            addPeer(peers, urls);
        }

        if (peers.isEmpty()) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        for (String peer : peers) {
            if (result.length() > 0) {
                result.append(',');
            }
            result.append(peer);
        }
        return result.toString();
    }

    private static SpringLdapEnhanceInfo enhancedInfo(Object source) {
        if (!(source instanceof EnhancedInstance)) {
            return null;
        }
        Object dynamicField = ((EnhancedInstance) source).getSkyWalkingDynamicField();
        return dynamicField instanceof SpringLdapEnhanceInfo ? (SpringLdapEnhanceInfo) dynamicField : null;
    }

    private static Object invokeNoArg(Object target, String methodName) {
        Method method;
        try {
            method = target.getClass().getMethod(methodName);
        } catch (Exception ignored) {
            return null;
        }
        try {
            return method.invoke(target);
        } catch (IllegalAccessException ignored) {
            try {
                method.setAccessible(true);
                return method.invoke(target);
            } catch (Exception inaccessible) {
                return null;
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void addPeer(Set<String> peers, Object value) {
        if (value == null) {
            return;
        }
        String peer = peerFromUrl(String.valueOf(value));
        if (peer != null) {
            peers.add(peer);
        }
    }

    private static String peerFromUrl(String value) {
        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme();
            if (scheme == null) {
                return null;
            }
            scheme = scheme.toLowerCase(Locale.ROOT);
            if (!"ldap".equals(scheme) && !"ldaps".equals(scheme)) {
                return null;
            }

            HostAndPort hostAndPort = hostAndPort(uri);
            if (hostAndPort.host == null || hostAndPort.host.isEmpty()) {
                return null;
            }
            int port = resolvedPort(scheme, hostAndPort.port);
            if (port == PORT_INVALID) {
                return null;
            }
            return formatHost(hostAndPort.host) + ':' + port;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int resolvedPort(String scheme, int port) {
        if (port == PORT_UNSPECIFIED) {
            return "ldaps".equals(scheme) ? 636 : 389;
        }
        if (port < MIN_PORT || port > MAX_PORT) {
            return PORT_INVALID;
        }
        return port;
    }

    private static HostAndPort hostAndPort(URI uri) {
        if (uri.getHost() != null) {
            int port = uri.getPort() >= 0 ? uri.getPort() : PORT_UNSPECIFIED;
            return new HostAndPort(uri.getHost(), port);
        }
        String authority = uri.getRawAuthority();
        if (authority == null) {
            return new HostAndPort(null, PORT_UNSPECIFIED);
        }
        int userInfoEnd = authority.lastIndexOf('@');
        if (userInfoEnd >= 0) {
            authority = authority.substring(userInfoEnd + 1);
        }
        if (authority.startsWith("[")) {
            int bracket = authority.indexOf(']');
            if (bracket < 0) {
                return new HostAndPort(null, PORT_INVALID);
            }
            String host = authority.substring(1, bracket);
            return new HostAndPort(host, parsePort(authority.substring(bracket + 1)));
        }
        int colon = authority.lastIndexOf(':');
        if (colon > 0 && authority.indexOf(':') == colon) {
            return new HostAndPort(authority.substring(0, colon), parsePort(authority.substring(colon)));
        }
        return new HostAndPort(authority, PORT_UNSPECIFIED);
    }

    private static int parsePort(String value) {
        if (value == null || value.isEmpty()) {
            return PORT_UNSPECIFIED;
        }
        if (value.charAt(0) != ':' || value.length() == 1) {
            return PORT_INVALID;
        }
        for (int i = 1; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character < '0' || character > '9') {
                return PORT_INVALID;
            }
        }
        try {
            int port = Integer.parseInt(value.substring(1));
            return port < 0 ? PORT_INVALID : port;
        } catch (NumberFormatException ignored) {
            return PORT_INVALID;
        }
    }

    private static String formatHost(String host) {
        String normalized = host;
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return normalized.indexOf(':') >= 0 ? '[' + normalized + ']' : normalized;
    }

    private static final class HostAndPort {

        private final String host;

        private final int port;

        private HostAndPort(String host, int port) {
            this.host = host;
            this.port = port;
        }
    }
}
