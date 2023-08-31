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

package org.apache.skywalking.apm.plugin.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.ClusterConnection;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class HTable100InterceptorTest {

    @Mock
    private EnhancedInstance objectInstance;

    @Mock
    private ClusterConnection clusterConnection;

    @Test
    public void testOnConstructWithXml() throws Throwable {
        HTable100Interceptor interceptor = new HTable100Interceptor();
        Configuration configuration = HBaseConfiguration.create();
        Object[] args = new Object[]{configuration, null};
        interceptor.onConstruct(objectInstance, args);
        verify(objectInstance).setSkyWalkingDynamicField("127.0.0.1");
    }

    @Test
    public void testOnConstructWithConfiguration() throws Throwable {

        HTable100Interceptor interceptor = new HTable100Interceptor();

        Configuration configuration = new Configuration();
        configuration.set("hbase.zookeeper.quorum", "localhost");

        //test construct: public HTable(Configuration conf, final TableName tableName) throws IOException
        Object[] args = new Object[]{configuration, null};
        interceptor.onConstruct(objectInstance, args);
        verify(objectInstance).setSkyWalkingDynamicField(any());
    }

    @Test
    public void testOnConstructWithConnection() throws Throwable {
        HTable100Interceptor interceptor = new HTable100Interceptor();

        Configuration configuration = new Configuration();
        configuration.set("hbase.zookeeper.quorum", "localhost");
        Mockito.when(clusterConnection.getConfiguration()).thenReturn(configuration);

        //test construct: public HTable(TableName tableName, final ClusterConnection connection,
        //      final ConnectionConfiguration tableConfig,
        //      final RpcRetryingCallerFactory rpcCallerFactory,
        //      final RpcControllerFactory rpcControllerFactory,
        //      final ExecutorService pool) throws IOException

        Object[] args = new Object[]{null, clusterConnection, null, null, null, null};
        interceptor.onConstruct(objectInstance, args);
        verify(objectInstance).setSkyWalkingDynamicField(any());
    }

}