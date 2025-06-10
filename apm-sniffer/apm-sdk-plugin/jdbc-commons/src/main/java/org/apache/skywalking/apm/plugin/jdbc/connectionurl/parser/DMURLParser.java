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

public class DMURLParser extends AbstractURLParser {
    private static final int DEFAULT_PORT = 5236;
    private static final String DB_TYPE = "DM";
    private static final String URL_PARAMS_HOST_KEY = "host";
    private static final String URL_PARAMS_PORT_KEY = "port";
    private static final String URL_PARAMS_SCHEMA_KEY = "schema";

    public DMURLParser(String url) {
        super(url);
    }

    @Override
    protected URLLocation fetchDatabaseHostsIndexRange() {
        int hostLabelStartIndex = url.indexOf("//");
        if (hostLabelStartIndex == -1) {
            return new URLLocation(0, 0);
        }
        int hostLabelEndIndex = url.indexOf("?", hostLabelStartIndex + 2);
        if (hostLabelEndIndex == -1) {
            hostLabelEndIndex = url.length();
        }
        return new URLLocation(hostLabelStartIndex + 2, hostLabelEndIndex);
    }

    @Override
    protected URLLocation fetchDatabaseNameIndexRange() {
        return new URLLocation(0, 0);
    }

    @Override
    public ConnectionInfo parse() {
        URLLocation location = fetchDatabaseHostsIndexRange();
        String hostPortSegment = "";
        if (location.endIndex() > location.startIndex()) {
            hostPortSegment = url.substring(location.startIndex(), location.endIndex());
        }

        String host = "";
        String port = "";

        if (!hostPortSegment.isEmpty()) {
            String[] parts = hostPortSegment.split(":");
            if (parts.length >= 1) {
                host = parts[0];
            }
            if (parts.length == 2) {
                port = parts[1];
            }
        }

        if (host.isEmpty()) {
            host = fetchFromUrlParams(URL_PARAMS_HOST_KEY);
        }
        if (port == null || port.isEmpty()) {
            port = fetchFromUrlParams(URL_PARAMS_PORT_KEY);
        }

        if (port == null || port.isEmpty()) {
            port = String.valueOf(DEFAULT_PORT);
        }

        String schema = fetchFromUrlParams(URL_PARAMS_SCHEMA_KEY);

        return new ConnectionInfo(
                ComponentsDefine.DMDB_JDBC_DRIVER,
                DB_TYPE,
                host,
                Integer.parseInt(port),
                schema == null ? "" : schema
        );
    }

    private String fetchFromUrlParams(String key) {
        int paramIndex = url.indexOf("?");
        if (paramIndex == -1) {
            return null;
        }
        String[] params = url.substring(paramIndex + 1).split("&");
        for (String pair : params) {
            if (pair.startsWith(key + "=")) {
                String[] segments = pair.split("=", 2);
                if (segments.length == 2) {
                    return segments[1];
                }
            }
        }
        return null;
    }
}
