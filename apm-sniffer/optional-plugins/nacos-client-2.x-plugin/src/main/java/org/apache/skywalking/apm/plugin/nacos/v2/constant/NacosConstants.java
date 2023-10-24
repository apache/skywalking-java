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

package org.apache.skywalking.apm.plugin.nacos.v2.constant;

import org.apache.skywalking.apm.agent.core.context.tag.AbstractTag;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;

public class NacosConstants {

    public static final String NACOS_PREFIX = "Nacos/";

    public static final String QUERY_SERVICE = "queryService";

    public static final String SUBSCRIBE_SERVICE = "subscribeService";

    public static final String UNSUBSCRIBE_SERVICE = "unsubscribeService";

    public static final String QUERY_CONFIG = "queryConfig";

    public static final String PUBLISH_CONFIG = "publishConfig";

    public static final String REMOVE_CONFIG = "removeConfig";

    public static final String GET_SERVICE_LIST = "getServiceList";

    public static final String NOTIFY_SUBSCRIBE_CHANGE = "notifySubscribeChange";

    public static final String NOTIFY_CONFIG_CHANGE = "notifyConfigChange";

    public static final AbstractTag<String> NAME_SPACE_TAG = Tags.ofKey("namespace");

    public static final AbstractTag<String> GROUP_TAG = Tags.ofKey("group");

    public static final AbstractTag<String> SERVICE_NAME_TAG = Tags.ofKey("serviceName");

    public static final AbstractTag<String> DATA_ID_TAG = Tags.ofKey("dataId");

    public static final AbstractTag<String> TENANT_TAG = Tags.ofKey("tenant");
}
