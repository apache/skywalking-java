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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpHost;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.apache.skywalking.apm.plugin.elasticsearch.v6.RestClientEnhanceInfo;
import org.elasticsearch.client.Node;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(TracingSegmentRunner.class)
public class RestHighLevelClientConInterceptorTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private RestClientBuilder restClientBuilder;

    @Mock
    private RestClient restClient;

    private Object[] allArguments;

    private RestHighLevelClientConInterceptor restHighLevelClientConInterceptor;

    @Before
    public void setUp() throws Exception {
        List<Node> nodeList = new ArrayList<Node>();
        nodeList.add(new Node(new HttpHost("127.0.0.1", 9200)));
        nodeList.add(new Node(new HttpHost("127.0.0.1", 9300)));
        restHighLevelClientConInterceptor = new RestHighLevelClientConInterceptor();
        when(restClientBuilder.build()).thenReturn(restClient);
        when(restClient.getNodes()).thenReturn(nodeList);
        allArguments = new Object[] {restClientBuilder};
    }

    @Test
    public void testConstruct() throws Throwable {

        final EnhancedInstance objInst = new EnhancedInstance() {
            private Object object = null;

            @Override
            public Object getSkyWalkingDynamicField() {
                return object;
            }

            @Override
            public void setSkyWalkingDynamicField(Object value) {
                this.object = value;
            }
        };
        restHighLevelClientConInterceptor = new RestHighLevelClientConInterceptor();
        restHighLevelClientConInterceptor.onConstruct(objInst, allArguments);

        assertThat(objInst.getSkyWalkingDynamicField() instanceof RestClientEnhanceInfo, is(true));
        assertThat(((RestClientEnhanceInfo) objInst.getSkyWalkingDynamicField()).getPeers(), is("127.0.0.1:9200,127.0.0.1:9300"));
    }
}
