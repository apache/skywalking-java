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
 */

package org.apache.skywalking.apm.plugin.elasticsearch.v6.interceptor;

import static org.apache.skywalking.apm.plugin.elasticsearch.v6.ElasticsearchPluginConfig.Plugin.Elasticsearch.TRACE_DSL;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import java.util.List;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.ExitSpan;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.context.util.TagValuePair;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.test.helper.SegmentHelper;
import org.apache.skywalking.apm.agent.test.helper.SpanHelper;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.SpanAssert;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.apache.skywalking.apm.plugin.elasticsearch.common.RestClientEnhanceInfo;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(TracingSegmentRunner.class)
public class RestHighLevelClientDeleteByQueryMethodsInterceptorTest {

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private EnhancedInstance enhancedInstance;

    @Mock
    private DeleteByQueryRequest deleteByQueryRequest;

    private Object[] allArguments;

    @Mock
    private RestClientEnhanceInfo restClientEnhanceInfo;

    private RestHighLevelClientDeleteByQueryMethodsInterceptor interceptor;

    @Before
    public void setUp() throws Exception {
        when(restClientEnhanceInfo.getPeers()).thenReturn("127.0.0.1:9200");
        allArguments = new Object[] {deleteByQueryRequest};
        when(deleteByQueryRequest.indices()).thenReturn(new String[] {"indexName"});
        when(deleteByQueryRequest.toString()).thenReturn("deleteByQueryRequest");
        when(enhancedInstance.getSkyWalkingDynamicField()).thenReturn(restClientEnhanceInfo);
        interceptor = new RestHighLevelClientDeleteByQueryMethodsInterceptor();
    }

    @Test
    public void testMethodsAround() throws Throwable {
        TRACE_DSL = true;
        interceptor.beforeMethod(enhancedInstance, null, allArguments, null, null);
        interceptor.afterMethod(enhancedInstance, null, allArguments, null, null);

        List<TraceSegment> traceSegmentList = segmentStorage.getTraceSegments();
        Assert.assertThat(traceSegmentList.size(), is(1));
        TraceSegment traceSegment = traceSegmentList.get(0);

        AbstractTracingSpan deleteByQuerySpan = SegmentHelper.getSpans(traceSegment).get(0);
        assertDeleteByQuerySpan(deleteByQuerySpan);
    }

    private void assertDeleteByQuerySpan(AbstractTracingSpan deleteByQuerySpan) {
        assertThat(deleteByQuerySpan instanceof ExitSpan, is(true));

        ExitSpan exitSpan = (ExitSpan) deleteByQuerySpan;
        assertThat(exitSpan.getOperationName(), is("Elasticsearch/DeleteByQueryRequest"));
        assertThat(exitSpan.getPeer(), is("127.0.0.1:9200"));
        assertThat(SpanHelper.getComponentId(exitSpan), is(77));

        List<TagValuePair> tags = SpanHelper.getTags(exitSpan);
        assertThat(tags.size(), is(3));
        assertThat(tags.get(0).getValue(), is("Elasticsearch"));
        assertThat(tags.get(1).getValue(), is("[indexName]"));
        assertThat(tags.get(2).getValue(), is("deleteByQueryRequest"));
    }

    @Test
    public void testMethodsAroundError() throws Throwable {
        TRACE_DSL = true;
        interceptor.beforeMethod(enhancedInstance, null, allArguments, null, null);
        interceptor.handleMethodException(enhancedInstance, null, allArguments, null, new RuntimeException());
        interceptor.afterMethod(enhancedInstance, null, allArguments, null, null);

        List<TraceSegment> traceSegmentList = segmentStorage.getTraceSegments();
        Assert.assertThat(traceSegmentList.size(), is(1));
        TraceSegment traceSegment = traceSegmentList.get(0);

        AbstractTracingSpan deleteByQuerySpan = SegmentHelper.getSpans(traceSegment).get(0);
        assertDeleteByQuerySpan(deleteByQuerySpan);

        Assert.assertEquals(true, SpanHelper.getErrorOccurred(deleteByQuerySpan));
        SpanAssert.assertException(SpanHelper.getLogs(deleteByQuerySpan).get(0), RuntimeException.class);
    }
}
