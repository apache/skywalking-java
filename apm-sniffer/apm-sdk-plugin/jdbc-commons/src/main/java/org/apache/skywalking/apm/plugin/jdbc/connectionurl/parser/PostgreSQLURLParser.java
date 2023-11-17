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

package org.apache.skywalking.apm.plugin.jdbc.connectionurl.parser;

import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.plugin.jdbc.trace.ConnectionInfo;

/**
 * {@link PostgreSQLURLParser} parse connection url of mysql.
 */
public class PostgreSQLURLParser extends AbstractURLParser {

    private static final int DEFAULT_PORT = 5432;
    private static final String DB_TYPE = "PostgreSQL";
    private static final String URL_PARAMS_HOST_KEY = "host";
    private static final String URL_PARAMS_PORT_KEY = "port";

    public PostgreSQLURLParser(String url) {
        super(url);
    }

    @Override
    protected URLLocation fetchDatabaseHostsIndexRange() {
        int hostLabelStartIndex = url.indexOf("//");
        int hostLabelEndIndex = url.indexOf("/", hostLabelStartIndex + 2);
        if (hostLabelEndIndex == -1) {
            hostLabelEndIndex = url.length();
        }
        return new URLLocation(hostLabelStartIndex + 2, hostLabelEndIndex);
    }

    @Override
    protected URLLocation fetchDatabaseNameIndexRange() {
        int databaseLabelStartIndex = url.indexOf("//");
        int databaseStartTag = url.indexOf("/", databaseLabelStartIndex + 2);
        int databaseEndTag = url.indexOf("?", databaseLabelStartIndex + 2);
        if (databaseEndTag < databaseStartTag && databaseEndTag != -1) {
            //database parse fail
            return new URLLocation(0, 0);
        }
        if (databaseStartTag == -1) {
            //database empty
            return new URLLocation(0, 0);
        }
        if (databaseEndTag == -1) {
            databaseEndTag = url.length();
        } else {
            databaseStartTag = url.substring(0, databaseEndTag).lastIndexOf("/");
        }
        return new URLLocation(databaseStartTag + 1, databaseEndTag);
    }

    @Override
    public ConnectionInfo parse() {
        URLLocation location = fetchDatabaseHostsIndexRange();
        String hosts = url.substring(location.startIndex(), location.endIndex());
        String[] hostSegment = hosts.split(",");
        if (hostSegment.length > 1) {
            StringBuilder sb = new StringBuilder();
            for (String host : hostSegment) {
                if (host.substring(host.indexOf("]") + 1).split(":").length == 1) {
                    sb.append(host).append(":").append(DEFAULT_PORT).append(",");
                } else {
                    sb.append(host).append(",");
                }
            }
            return new ConnectionInfo(
                ComponentsDefine.POSTGRESQL_DRIVER, DB_TYPE, sb.substring(0, sb.length() - 1),
                fetchDatabaseNameFromURL()
            );
        } else {
            String[] hostAndPort = getSingleHostAndPort(hostSegment[0]);
            return new ConnectionInfo(
                ComponentsDefine.POSTGRESQL_DRIVER, DB_TYPE, hostAndPort[0], Integer.valueOf(hostAndPort[1]),
                fetchDatabaseNameFromURL()
            );
        }
    }

    /**
     * check the URI if IPv6 pattern matched(enclosed in square brackets, like [2001:db8::1234])
     */
    private boolean isIpv6Url(String hosts) {
        return hosts.contains("[") && url.contains("]");
    }

    /**
     * parse the URL of a single host port and add it from the URL parameters
     */
    private String[] getSingleHostAndPort(String hostSegment) {
        String host = "";
        String port = "";
        if (!isIpv6Url(hostSegment)) {
            String[] hostAndPort = hostSegment.split(":");
            host = hostAndPort[0];
            if (hostAndPort.length != 1) {
                port = hostAndPort[1];
            }
        } else {
            host = hostSegment.substring(0, hostSegment.indexOf("]") + 1);
            String[] ports = hostSegment.substring(hostSegment.indexOf("]") + 1).split(":");
            if (ports.length != 1) {
                port = ports[1];
            }
        }
        if (host.isEmpty()) {
            String additionalHost = fetchFromUrlParams(URL_PARAMS_HOST_KEY);
            if (additionalHost != null) {
                host = additionalHost;
            }
        }
        if (port.isEmpty()) {
            String additionalPort = fetchFromUrlParams(URL_PARAMS_PORT_KEY);
            if (additionalPort != null) {
                port = additionalPort;
            } else {
                port = String.valueOf(DEFAULT_PORT);
            }
        }
        return new String[] {
            host,
            port
        };
    }

    /**
     * fetch value from url parameters with specific key
     */
    private String fetchFromUrlParams(String key) {
        int databaseAdditionalParamIndex = url.indexOf("?");
        if (databaseAdditionalParamIndex == -1) {
            return null;
        }
        String[] paramArr = url.substring(databaseAdditionalParamIndex + 1).split("&");
        for (final String pair : paramArr) {
            if (pair.contains(key)) {
                String[] paramsPair = pair.split("=");
                if (paramsPair.length != 1) {
                    return paramsPair[1];
                }
            }
        }
        return null;
    }
}
