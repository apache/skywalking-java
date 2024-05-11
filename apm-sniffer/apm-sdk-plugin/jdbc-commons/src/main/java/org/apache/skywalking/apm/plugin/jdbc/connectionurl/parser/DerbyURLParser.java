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

public class DerbyURLParser extends AbstractURLParser {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 1527;
    private static final String DB_TYPE = "Derby";
    private static final String DERBY_JDBC_URL_PREFIX = "jdbc:derby";
    /**
     * Flag that running with directory mode.
     */
    private static final String DIRECTORY_MODE_FLAG = "derby:directory";
    /**
     * Flag that running with memory mode.
     */
    private static final String MEMORY_MODE_FLAG = "derby:memory";
    /**
     * Flag that running with classpath mode.
     */
    private static final String CLASSPATH_MODE_FLAG = "derby:classpath";
    /**
     * Flag that running with jar mode.
     */
    private static final String JAR_MODE_FLAG = "derby:jar";

    public DerbyURLParser(String url) {
        super(url);
    }

    /**
     * Fetch range index that the database name from connection url if Derby database running in client/server
     * environment. eg: jdbc:derby://host[:port]/[databaseName][;attribute=value]*
     *
     * @return range index that the database name.
     */
    @Override
    protected URLLocation fetchDatabaseHostsIndexRange() {
        int hostLabelStartIndex = url.indexOf("//");
        int hostLabelEndIndex = url.indexOf("/", hostLabelStartIndex + 2);
        return new URLLocation(hostLabelStartIndex + 2, hostLabelEndIndex);
    }

    @Override
    protected URLLocation fetchDatabaseNameIndexRange() {
        int databaseEndTag = url.indexOf(";");
        if (databaseEndTag == -1) {
            databaseEndTag = url.length();
        }
        int databaseStartTag = url.lastIndexOf("\\");
        if (databaseStartTag == -1) {
            databaseStartTag = url.lastIndexOf("/");
        }
        if (url.indexOf(":", databaseStartTag) != -1) {
            databaseStartTag = url.indexOf(":", databaseStartTag);
        }
        return new URLLocation(databaseStartTag + 1, databaseEndTag);
    }

    @Override
    public ConnectionInfo parse() {
        int[] databaseNameRangeIndex = fetchDatabaseNameRangeIndexForSubProtocol(DIRECTORY_MODE_FLAG);
        if (databaseNameRangeIndex != null) {
            return defaultConnection(databaseNameRangeIndex);
        }
        databaseNameRangeIndex = fetchDatabaseNameRangeIndexForSubProtocol(MEMORY_MODE_FLAG);
        if (databaseNameRangeIndex != null) {
            return defaultConnection(databaseNameRangeIndex);
        }
        databaseNameRangeIndex = fetchDatabaseNameRangeIndexForSubProtocol(CLASSPATH_MODE_FLAG);
        if (databaseNameRangeIndex != null) {
            return defaultConnection(databaseNameRangeIndex);
        }
        databaseNameRangeIndex = fetchDatabaseNameRangeIndexForSubProtocol(JAR_MODE_FLAG);
        if (databaseNameRangeIndex != null) {
            return defaultConnection(databaseNameRangeIndex);
        }
        databaseNameRangeIndex = fetchDatabaseNameRangeIndexWithoutHosts();
        if (databaseNameRangeIndex != null) {
            return defaultConnection(databaseNameRangeIndex);
        }
        String[] hostAndPort = fetchDatabaseHostsFromURL().split(":");
        if (hostAndPort.length == 1) {
            return new ConnectionInfo(
                ComponentsDefine.DERBY_JDBC_DRIVER, DB_TYPE, hostAndPort[0], DEFAULT_PORT, fetchDatabaseNameFromURL());
        } else {
            return new ConnectionInfo(
                ComponentsDefine.DERBY_JDBC_DRIVER, DB_TYPE, hostAndPort[0], Integer.valueOf(hostAndPort[1]),
                fetchDatabaseNameFromURL()
            );
        }
    }

    /**
     * Fetch range index that the database name from connection url if Derby database running in embedded environment.
     * eg: jdbc:derby:[databaseName][;attribute=value]*
     *
     * @return range index that the database name.
     */
    private int[] fetchDatabaseNameRangeIndexWithoutHosts() {
        if (url.contains("//")) {
            return null;
        }
        int fileLabelIndex = url.indexOf(DERBY_JDBC_URL_PREFIX);
        int parameterLabelIndex = url.indexOf(";");
        if (parameterLabelIndex == -1) {
            parameterLabelIndex = url.length();
        }

        if (fileLabelIndex != -1) {
            int pathLabelIndexForLinux = url.lastIndexOf("/");
            if (pathLabelIndexForLinux != -1 && pathLabelIndexForLinux > fileLabelIndex) {
                return new int[] {
                    pathLabelIndexForLinux + 1,
                    parameterLabelIndex
                };
            }
            int pathLabelIndexForWin = url.lastIndexOf("\\");
            if (pathLabelIndexForWin != -1 && pathLabelIndexForWin > fileLabelIndex) {
                return new int[] {
                    pathLabelIndexForWin + 1,
                    parameterLabelIndex
                };
            }
            return new int[] {
                fileLabelIndex + DERBY_JDBC_URL_PREFIX.length() + 1,
                parameterLabelIndex
            };
        } else {
            return null;
        }
    }

    /**
     * Fetch range index that the database name from connection url if Derby database running with subprotocol. eg:
     * jdbc:derby:subprotocol:[databaseName][;attribute=value]*
     *
     * @return range index that the database name.
     */
    private int[] fetchDatabaseNameRangeIndexForSubProtocol(String mode) {
        int fileLabelIndex = url.indexOf(mode);
        int parameterLabelIndex = url.indexOf(";", fileLabelIndex);
        if (parameterLabelIndex == -1) {
            parameterLabelIndex = url.length();
        }

        if (fileLabelIndex != -1) {
            int pathLabelIndexForLinux = url.lastIndexOf("/");
            if (pathLabelIndexForLinux != -1 && pathLabelIndexForLinux > fileLabelIndex) {
                return new int[] {
                    pathLabelIndexForLinux + 1,
                    parameterLabelIndex
                };
            }
            int pathLabelIndexForWin = url.lastIndexOf("\\");
            if (pathLabelIndexForWin != -1 && pathLabelIndexForWin > fileLabelIndex) {
                return new int[] {
                    pathLabelIndexForWin + 1,
                    parameterLabelIndex
                };
            }
            return new int[] {
                fileLabelIndex + mode.length() + 1,
                parameterLabelIndex
            };
        } else {
            return null;
        }
    }

    private ConnectionInfo defaultConnection(int[] databaseNameRangeIndex) {
        return new ConnectionInfo(
            ComponentsDefine.DERBY_JDBC_DRIVER, DB_TYPE, DEFAULT_HOST, -1,
            fetchDatabaseNameFromURL(databaseNameRangeIndex)
        );
    }
}
