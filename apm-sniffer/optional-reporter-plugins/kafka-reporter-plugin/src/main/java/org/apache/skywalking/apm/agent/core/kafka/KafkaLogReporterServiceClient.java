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

package org.apache.skywalking.apm.agent.core.kafka;

import java.util.List;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.utils.Bytes;
import org.apache.skywalking.apm.agent.core.boot.OverrideImplementor;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.remote.LogReportServiceClient;
import org.apache.skywalking.apm.agent.core.util.CollectionUtil;
import org.apache.skywalking.apm.network.logging.v3.LogData;

@OverrideImplementor(LogReportServiceClient.class)
public class KafkaLogReporterServiceClient extends LogReportServiceClient implements KafkaConnectionStatusListener {

    private String topic;
    private KafkaProducer<String, Bytes> producer;

    @Override
    public void prepare() {
        KafkaProducerManager producerManager = ServiceManager.INSTANCE.findService(KafkaProducerManager.class);
        producerManager.addListener(this);
        topic = producerManager.formatTopicNameThenRegister(KafkaReporterPluginConfig.Plugin.Kafka.TOPIC_LOGGING);
    }

    @Override
    public void produce(final LogData.Builder logData) {
        super.produce(logData);
    }

    @Override
    public void consume(final List<LogData.Builder> dataList) {
        if (producer == null || CollectionUtil.isEmpty(dataList)) {
            return;
        }

        for (LogData.Builder data : dataList) {
            // Kafka Log reporter sends one log per time.
            // Every time, service name should be set to keep data integrity.
            data.setService(Config.Agent.SERVICE_NAME);
            producer.send(new ProducerRecord<>(topic, data.getService(), Bytes.wrap(data.build().toByteArray())));
        }
    }

    @Override
    public void onStatusChanged(final org.apache.skywalking.apm.agent.core.kafka.KafkaConnectionStatus status) {
        if (status == KafkaConnectionStatus.CONNECTED) {
            producer = ServiceManager.INSTANCE.findService(KafkaProducerManager.class).getProducer();
        }
    }
}
