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

package org.apache.skywalking.oap.server.core.source;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

import static org.apache.skywalking.oap.server.core.source.DefaultScopeDefine.ALL;

@ScopeDeclaration(id = ALL, name = "All")
public class All extends Source {
    @Override
    public int scope() {
        return DefaultScopeDefine.ALL;
    }

    @Override
    public String getEntityId() {
        return "";
    }

    @Getter
    @Setter
    private String name;
    @Getter
    @Setter
    private String serviceInstanceName;
    @Getter
    @Setter
    private String endpointName;
    @Getter
    @Setter
    private int latency;
    @Getter
    @Setter
    private boolean status;
    @Getter
    @Setter
    @Deprecated
    private int responseCode;
    @Getter
    @Setter
    private int httpResponseStatusCode;
    @Getter
    @Setter
    private String rpcStatusCode;
    @Getter
    @Setter
    private RequestType type;
    @Getter
    @Setter
    private List<String> tags;
}
