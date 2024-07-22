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

package org.apache.skywalking.apm.plugin.kafka37.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.apache.skywalking.apm.plugin.kafka.define.KafkaConsumerInstrumentation;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static org.apache.skywalking.apm.agent.core.plugin.match.NameMatch.byName;

public class Kafka37ConsumerInstrumentation extends KafkaConsumerInstrumentation {

    public static final String ENHANCE_CLASS_37 = "org.apache.kafka.clients.consumer.KafkaConsumer";

    public static final String INTERCEPTOR_CLASS_37 = "org.apache.skywalking.apm.plugin.kafka37.Kafka37ConsumerInterceptor";

    // Kafka 3.7.x's pull message method's name is "poll"
    public static final String ENHANCE_METHOD_37 = "poll";

    // Kafka 3.7.x's pull message method's return type is "ConsumerRecords"
    public static final String ENHANCE_RETURN_TYPE_37 = "org.apache.kafka.clients.consumer.ConsumerRecords";

    @Override
    protected ClassMatch enhanceClass() {
        return byName(ENHANCE_CLASS_37);
    }

    @Override
    public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[]{
                new InstanceMethodsInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return named(ENHANCE_METHOD_37).and(returns(named(ENHANCE_RETURN_TYPE_37)));
                    }

                    @Override
                    public String getMethodsInterceptor() {
                        return INTERCEPTOR_CLASS_37;
                    }

                    @Override
                    public boolean isOverrideArgs() {
                        return false;
                    }
                }
        };
    }
}
