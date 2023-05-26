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
import org.apache.skywalking.apm.plugin.spring.resttemplate.async.ResponseCallBackInterceptor;

import java.net.URI;

/**
 * {@link ResponseExtractorFutureInstrumentation3x} enhance the <code>addCallback</code> method and
 * <code>getDefault</code> method of <code>org.springframework.web.client.AsyncRestTemplate$ResponseExtractorFuture</code>
 * by
 * <code>ResponseCallBackInterceptor</code> and
 * <code>FutureGetInterceptor</code>.
 * <p>
 * {@link ResponseCallBackInterceptor} set the {@link URI} and {@link ContextSnapshot} to inherited
 * <code>org.springframework.util.concurrent.SuccessCallback</code> and <code>org.springframework.util.concurrent.FailureCallback</code>
 */
public class ResponseExtractorFutureInstrumentation3x extends ResponseExtractorFutureInstrumentation {
    @Override
    protected String[] witnessClasses() {
        return new String[] {
            "org.springframework.web.context.support.ServletContextPropertyPlaceholderConfigurer"
        };
    }
}
