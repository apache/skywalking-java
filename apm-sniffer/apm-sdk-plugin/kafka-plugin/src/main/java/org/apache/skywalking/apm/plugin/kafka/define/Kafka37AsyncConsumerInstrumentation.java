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

package org.apache.skywalking.apm.plugin.kafka.define;

import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;
import static org.apache.skywalking.apm.agent.core.plugin.match.NameMatch.byName;

/**
 * Here is the intercept process steps.
 *
 * <pre>
 *  1. Record the topic when the client invoke <code>subscribed</code> method
 *  2. Create the entry span when the client invoke the method <code>pollOnce</code>.
 *  3. Extract all the <code>Trace Context</code> by iterate all <code>ConsumerRecord</code>
 *  4. Stop the entry span when <code>pollOnce</code> method finished.
 * </pre>
 */
public class Kafka37AsyncConsumerInstrumentation extends KafkaConsumerInstrumentation {

    private static final String ENHANCE_CLASS_37_ASYNC = "org.apache.kafka.clients.consumer.internals.AsyncKafkaConsumer";

    @Override
    protected ClassMatch enhanceClass() {
        return byName(ENHANCE_CLASS_37_ASYNC);
    }
}