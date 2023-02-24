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

package org.apache.skywalking.apm.plugin.spring.resttemplate.async.define;

import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;

import java.net.URI;

/**
 * {@link RestTemplateInstrumentation3x} enhance the <code>doExecute</code> method and <code>createAsyncRequest</code>
 * method of <code>org.springframework.web.client.AsyncRestTemplate</code> by <code>RestExecuteInterceptor</code> and
 * <code>org.springframework.http.client.RestRequestInterceptor</code>.
 *
 * <code>org.springframework.http.client.RestRequestInterceptor</code> set {@link URI} and {@link ContextSnapshot} to
 * <code>org.springframework.web.client.AsyncRestTemplate$ResponseExtractorFuture</code> for propagate trace context
 * after execute <code>doExecute</code> .
 */
public class RestTemplateInstrumentation3x extends RestTemplateInstrumentation {
    @Override
    protected String[] witnessClasses() {
        return new String[] {
            "org.springframework.web.context.support.ServletContextPropertyPlaceholderConfigurer"
        };
    }
}
