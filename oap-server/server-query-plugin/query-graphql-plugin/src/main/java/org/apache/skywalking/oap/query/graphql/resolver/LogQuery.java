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

package org.apache.skywalking.oap.query.graphql.resolver;

import com.coxautodev.graphql.tools.GraphQLQueryResolver;
import java.io.IOException;
import org.apache.skywalking.oap.server.core.CoreModule;
import org.apache.skywalking.oap.server.core.UnexpectedException;
import org.apache.skywalking.oap.server.core.query.LogQueryService;
import org.apache.skywalking.oap.server.core.query.enumeration.Order;
import org.apache.skywalking.oap.server.core.query.input.LogQueryCondition;
import org.apache.skywalking.oap.server.core.query.type.Logs;
import org.apache.skywalking.oap.server.library.module.ModuleManager;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class LogQuery implements GraphQLQueryResolver {
    private final ModuleManager moduleManager;
    private LogQueryService logQueryService;

    public LogQuery(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    private LogQueryService getQueryService() {
        if (logQueryService == null) {
            this.logQueryService = moduleManager.find(CoreModule.NAME).provider().getService(LogQueryService.class);
        }
        return logQueryService;
    }

    public boolean supportQueryLogsByKeywords() {
        return getQueryService().supportQueryLogsByKeywords();
    }

    public Logs queryLogs(LogQueryCondition condition) throws IOException {
        if (isNull(condition.getQueryDuration()) && isNull(condition.getRelatedTrace())) {
            throw new UnexpectedException("The condition must contains either queryDuration or relatedTrace.");
        }
        long startSecondTB = 0;
        long endSecondTB = 0;
        if (nonNull(condition.getQueryDuration())) {
            startSecondTB = condition.getQueryDuration().getStartTimeBucketInSec();
            endSecondTB = condition.getQueryDuration().getEndTimeBucketInSec();
        }
        Order queryOrder = isNull(condition.getQueryOrder()) ? Order.DES : condition.getQueryOrder();

        return getQueryService().queryLogs(
            condition.getServiceId(),
            condition.getServiceInstanceId(),
            condition.getEndpointId(),
            condition.getEndpointName(),
            condition.getRelatedTrace(),
            condition.getPaging(),
            queryOrder,
            startSecondTB, endSecondTB,
            condition.getTags(),
            condition.getKeywordsOfContent(),
            condition.getExcludingKeywordsOfContent()
        );
    }
}
