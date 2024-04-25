/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.skywalking.apm.plugin.c3p0;

public class PoolConstants {
    public static final String POOL_CONNECTION = "C3P0/Connection/";
    public static final String METER_NAME = "datasource";
    public static final String METER_TAG_NAME = "name";
    public static final String METER_TAG_STATUS = "status";

    //==================== METRICS ========================//
    public static final String NUM_TOTAL_CONNECTIONS = "numTotalConnections";
    public static final String NUM_BUSY_CONNECTIONS = "numBusyConnections";
    public static final String NUM_IDLE_CONNECTIONS = "numIdleConnections";
    public static final String MAX_IDLE_TIME = "maxIdleTime";
    public static final String MIN_POOL_SIZE = "minPoolSize";
    public static final String MAX_POOL_SIZE = "maxPoolSize";
    public static final String INITIAL_POOL_SIZE = "initialPoolSize";
}
