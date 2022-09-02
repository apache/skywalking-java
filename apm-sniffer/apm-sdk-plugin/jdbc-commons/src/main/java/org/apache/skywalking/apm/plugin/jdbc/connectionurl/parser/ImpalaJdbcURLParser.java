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
import org.apache.skywalking.apm.network.trace.component.OfficialComponent;
import org.apache.skywalking.apm.plugin.jdbc.trace.ConnectionInfo;

public class ImpalaJdbcURLParser extends AbstractURLParser {

    private static final int DEFAULT_PORT = 21050;
    private static final String DB_TYPE = "Impala";
    private OfficialComponent component = ComponentsDefine.IMPALA_JDBC_DRIVER;

    public ImpalaJdbcURLParser(String url) {
        super(url);
    }

    @Override
    protected URLLocation fetchDatabaseHostsIndexRange() {
        int hostLabelStartIndex = url.indexOf("//");
        int hostLabelEndIndex = url.length();
        int hostLabelEndIndexWithParameter = url.indexOf(";", hostLabelStartIndex);
        if (hostLabelEndIndexWithParameter != -1) {
            String subUrl = url.substring(0, hostLabelEndIndexWithParameter);
            int schemaIndex = subUrl.indexOf("/", hostLabelStartIndex + 2);
            if (schemaIndex == -1) {
                hostLabelEndIndex = hostLabelEndIndexWithParameter;
            } else {
                hostLabelEndIndex = schemaIndex;
            }
        }
        return new URLLocation(hostLabelStartIndex + 2, hostLabelEndIndex);
    }

    protected String fetchDatabaseNameFromURL(int startSize) {
        URLLocation hostsLocation = fetchDatabaseNameIndexRange(startSize);
        if (hostsLocation == null) {
            return "";
        }
        return url.substring(hostsLocation.startIndex(), hostsLocation.endIndex());
    }

    protected URLLocation fetchDatabaseNameIndexRange(int startSize) {
        int databaseStartTag = url.indexOf("/", startSize);
        int firstParamIndex = url.indexOf(";", startSize);
        int databaseEndTag = url.length();
        if (databaseStartTag == -1 && firstParamIndex == -1) {
            return null;
        } else {
            String subUrl = url.substring(startSize, firstParamIndex);
            int schemaIndex = subUrl.indexOf("/");
            if (schemaIndex == -1) {
                return null;
            } else {
                databaseEndTag = firstParamIndex;
            }
        }
        return new URLLocation(databaseStartTag + 1, databaseEndTag);
    }

    @Override
    protected URLLocation fetchDatabaseNameIndexRange() {
        URLLocation location = fetchDatabaseHostsIndexRange();
        return fetchDatabaseNameIndexRange(location.endIndex());
    }

    @Override
    public ConnectionInfo parse() {
        URLLocation location = fetchDatabaseHostsIndexRange();
        String hosts = url.substring(location.startIndex(), location.endIndex());
        String[] hostSegment = hosts.split(",");
        if (hostSegment.length > 1) {
            StringBuilder sb = new StringBuilder();
            for (String host : hostSegment) {
                if (host.split(":").length == 1) {
                    sb.append(host).append(":").append(DEFAULT_PORT).append(",");
                } else {
                    sb.append(host).append(",");
                }
            }
            return new ConnectionInfo(component, DB_TYPE, sb.substring(0, sb.length() - 1), fetchDatabaseNameFromURL());
        } else {
            String[] hostAndPort = hostSegment[0].split(":");
            if (hostAndPort.length != 1) {
                return new ConnectionInfo(component, DB_TYPE, hostAndPort[0], Integer.valueOf(hostAndPort[1]), fetchDatabaseNameFromURL(location
                        .endIndex()));
            } else {
                return new ConnectionInfo(component, DB_TYPE, hostAndPort[0], DEFAULT_PORT, fetchDatabaseNameFromURL(location
                        .endIndex()));
            }
        }
    }
}
