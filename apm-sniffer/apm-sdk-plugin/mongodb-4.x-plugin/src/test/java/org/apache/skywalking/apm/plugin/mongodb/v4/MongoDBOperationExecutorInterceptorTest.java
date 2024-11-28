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

package org.apache.skywalking.apm.plugin.mongodb.v4;

import com.mongodb.MongoNamespace;
import com.mongodb.ReadConcern;
import com.mongodb.client.internal.OperationExecutor;
import com.mongodb.internal.operation.AggregateOperation;
import com.mongodb.internal.operation.CreateCollectionOperation;
import com.mongodb.internal.operation.FindOperation;
import com.mongodb.internal.operation.WriteOperation;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.LogDataEntity;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
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
import org.apache.skywalking.apm.plugin.mongodb.v4.interceptor.MongoDBOperationExecutorInterceptor;
import org.apache.skywalking.apm.plugin.mongodb.v4.support.MongoNamespaceInfo;
import org.apache.skywalking.apm.plugin.mongodb.v4.interceptor.operation.OperationNamespaceConstructInterceptor;
import org.apache.skywalking.apm.plugin.mongodb.v4.support.MongoPluginConfig;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.codecs.Decoder;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(TracingSegmentRunner.class)
public class MongoDBOperationExecutorInterceptorTest {

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private EnhancedInstance enhancedInstance;

    private FindOperation enhancedInstanceForFindOperation;

    @Spy
    private EnhancedInstance enhancedObjInstance = new EnhancedInstance() {
        private MongoNamespaceInfo namespace;

        @Override
        public Object getSkyWalkingDynamicField() {
            return namespace;
        }

        @Override
        public void setSkyWalkingDynamicField(Object value) {
            this.namespace = (MongoNamespaceInfo) value;
        }
    };

    private MongoDBOperationExecutorInterceptor interceptor;

    private OperationNamespaceConstructInterceptor constructInterceptor;

    private Object[] arguments;

    private Class[] argumentTypes;

    private Decoder decoder;

    private MongoNamespace mongoNamespace;

    private MongoNamespaceInfo mongoNamespaceInfo;

    @Before
    public void setUp() {

        interceptor = new MongoDBOperationExecutorInterceptor();
        constructInterceptor = new OperationNamespaceConstructInterceptor();
        enhancedInstanceForFindOperation = mock(FindOperation.class, Mockito.withSettings().extraInterfaces(EnhancedInstance.class));
        MongoPluginConfig.Plugin.MongoDB.TRACE_PARAM = true;
        decoder = mock(Decoder.class);
        mongoNamespace = new MongoNamespace("test.user");
        mongoNamespaceInfo =  new MongoNamespaceInfo(mongoNamespace);
        BsonDocument document = new BsonDocument();
        document.append("name", new BsonString("by"));

        FindOperation findOperation = new FindOperation(mongoNamespace, decoder);
        findOperation.filter(document);
        decoder = mock(Decoder.class);
        when(enhancedInstance.getSkyWalkingDynamicField()).thenReturn("127.0.0.1:27017");
        enhancedInstanceForFindOperation = mock(FindOperation.class, Mockito.withSettings().extraInterfaces(EnhancedInstance.class));
        when(((EnhancedInstance) enhancedInstanceForFindOperation).getSkyWalkingDynamicField()).thenReturn(mongoNamespaceInfo);
        when(enhancedInstanceForFindOperation.getFilter()).thenReturn(findOperation.getFilter());
        arguments = new Object[] {enhancedInstanceForFindOperation};
        argumentTypes = new Class[] {findOperation.getClass()};
    }

    @Test
    public void testConstructIntercept() throws Throwable {
        constructInterceptor.onConstruct(enhancedObjInstance, new Object[]{mongoNamespace});
        MatcherAssert.assertThat(enhancedObjInstance.getSkyWalkingDynamicField(), Is.is(new MongoNamespaceInfo(mongoNamespace)));
    }

    @Test
    public void testCreateCollectionOperationIntercept() throws Throwable {
        CreateCollectionOperation createCollectionOperation = new CreateCollectionOperation("test", "user");
        CreateCollectionOperation enhancedInstanceForCreateCollectionOperation = mock(CreateCollectionOperation.class, Mockito.withSettings().extraInterfaces(EnhancedInstance.class));
        when(((EnhancedInstance) enhancedInstanceForCreateCollectionOperation).getSkyWalkingDynamicField()).thenReturn(new MongoNamespaceInfo("test"));
        when(enhancedInstanceForCreateCollectionOperation.getCollectionName()).thenReturn("user");

        Object[] arguments = {enhancedInstanceForCreateCollectionOperation};
        Class[] argumentTypes = {createCollectionOperation.getClass()};
        interceptor.beforeMethod(enhancedInstance, getMethod(), arguments, argumentTypes, null);
        interceptor.afterMethod(enhancedInstance, getMethod(), arguments, argumentTypes, null);

        MatcherAssert.assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertMongoCreateCollectionOperationSpan(spans.get(0));
    }

    @Test
    public void testIntercept() throws Throwable {
        interceptor.beforeMethod(enhancedInstance, getMethod(), arguments, argumentTypes, null);
        interceptor.afterMethod(enhancedInstance, getMethod(), arguments, argumentTypes, null);

        MatcherAssert.assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertMongoFindOperationSpan(spans.get(0));
    }

    @Test
    public void testAggregateOperationIntercept() throws Throwable {
        MongoNamespace mongoNamespace = new MongoNamespace("test.user");
        BsonDocument matchStage = new BsonDocument("$match", new BsonDocument("name", new BsonString("by")));
        List<BsonDocument> pipeline = Collections.singletonList(matchStage);
        AggregateOperation<BsonDocument> aggregateOperation = new AggregateOperation(mongoNamespace, pipeline, decoder);

        AggregateOperation enhancedInstanceForAggregateOperation = mock(AggregateOperation.class, Mockito.withSettings().extraInterfaces(EnhancedInstance.class));
        when(((EnhancedInstance) enhancedInstanceForAggregateOperation).getSkyWalkingDynamicField()).thenReturn(new MongoNamespaceInfo(mongoNamespace));
        when(enhancedInstanceForAggregateOperation.getPipeline()).thenReturn(aggregateOperation.getPipeline());
        Object[] arguments = {enhancedInstanceForAggregateOperation};
        Class[] argumentTypes = {aggregateOperation.getClass()};

        interceptor.beforeMethod(enhancedInstance, getMethod(), arguments, argumentTypes, null);
        interceptor.afterMethod(enhancedInstance, getMethod(), arguments, argumentTypes, null);

        MatcherAssert.assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertMongoAggregateOperationSpan(spans.get(0));
    }

    @Test
    public void testInterceptWithException() throws Throwable {
        interceptor.beforeMethod(enhancedInstance, getMethod(), arguments, argumentTypes, null);
        interceptor.handleMethodException(enhancedInstance, getMethod(), arguments, argumentTypes, new RuntimeException());
        interceptor.afterMethod(enhancedInstance, getMethod(), arguments, argumentTypes, null);

        MatcherAssert.assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertMongoFindOperationSpan(spans.get(0));
        List<LogDataEntity> logDataEntities = SpanHelper.getLogs(spans.get(0));
        assertThat(logDataEntities.size(), is(1));
        SpanAssert.assertException(logDataEntities.get(0), RuntimeException.class);
    }

    private void assertMongoFindOperationSpan(AbstractTracingSpan span) {
        assertThat(span.getOperationName(), startsWith("MongoDB/FindOperation"));
        assertThat(SpanHelper.getComponentId(span), is(42));
        List<TagValuePair> tags = SpanHelper.getTags(span);
        assertThat(tags.get(0).getValue(), is("MongoDB"));
        assertThat(tags.get(1).getValue(), is("test"));
        assertThat(tags.get(2).getValue(), is("user"));
        assertThat(tags.get(3).getValue(), is("{\"name\": \"by\"}"));
        assertThat(span.isExit(), is(true));
        assertThat(SpanHelper.getLayer(span), CoreMatchers.is(SpanLayer.DB));
    }

    private void assertMongoCreateCollectionOperationSpan(AbstractTracingSpan span) {
        assertThat(span.getOperationName(), startsWith("MongoDB/CreateCollectionOperation"));
        assertThat(SpanHelper.getComponentId(span), is(42));
        List<TagValuePair> tags = SpanHelper.getTags(span);
        assertThat(tags.get(0).getValue(), is("MongoDB"));
        assertThat(tags.get(1).getValue(), is("test"));
        assertThat(tags.get(2).getValue(), is("user"));
        assertThat(span.isExit(), is(true));
        assertThat(SpanHelper.getLayer(span), CoreMatchers.is(SpanLayer.DB));
    }

    private void assertMongoAggregateOperationSpan(AbstractTracingSpan span) {
        assertThat(span.getOperationName(), startsWith("MongoDB/AggregateOperation"));
        assertThat(SpanHelper.getComponentId(span), is(42));
        List<TagValuePair> tags = SpanHelper.getTags(span);
        assertThat(tags.get(0).getValue(), is("MongoDB"));
        assertThat(tags.get(1).getValue(), is("test"));
        assertThat(tags.get(2).getValue(), is("user"));
        assertThat(tags.get(3).getValue(), is("{\"$match\": {\"name\": \"by\"}},"));
        assertThat(span.isExit(), is(true));
        assertThat(SpanHelper.getLayer(span), CoreMatchers.is(SpanLayer.DB));
    }

    private Method getMethod() throws Exception {
        return OperationExecutor.class.getMethod("execute", WriteOperation.class, ReadConcern.class);
    }
}
