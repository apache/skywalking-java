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

package org.apache.skywalking.apm.agent.bytebuddy.cases;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import org.apache.skywalking.apm.agent.bytebuddy.InstMethodsInter;
import org.apache.skywalking.apm.agent.bytebuddy.SWAsmVisitorWrapper;
import org.apache.skywalking.apm.agent.core.util.FileUtils;
import org.junit.Test;

import java.io.File;

public class MultipleInterceptorTest extends AbstractInterceptTest {

    String dumpFolder = "target/class-dump";

    @Test
    public void test1() {
        String className = BIZ_FOO_CLASS_NAME;
        String methodName = SAY_HELLO_METHOD;
        String nameTrait = getNameTrait(1);

        enableClassDump();

        //newAgentBuilder(nameTrait)
        new AgentBuilder.Default()
                .type(ElementMatchers.named(className))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) -> {
                            int round = 1;
                            String interceptorClassName = METHOD_INTERCEPTOR_CLASS + "$" + methodName + "$" + round;
                            String fieldName = nameTrait + "_delegate$" + methodName + round;

                            if (deleteDuplicatedFields) {
                                builder = builder.visit(new SWAsmVisitorWrapper());
                            }
                            return builder
                                    .method(ElementMatchers.nameContainsIgnoreCase(methodName))
                                    .intercept(MethodDelegation.withDefaultConfiguration()
                                            .to(new InstMethodsInter(interceptorClassName, classLoader), fieldName))
                                    ;
                        }
                )
                .type(ElementMatchers.nameContains(className))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) -> {
                            int round = 2;
                            String interceptorClassName = METHOD_INTERCEPTOR_CLASS + "$" + methodName + "$" + round;
                            String fieldName = nameTrait + "_delegate$" + methodName + round;

                            if (deleteDuplicatedFields) {
                                builder = builder.visit(new SWAsmVisitorWrapper());
                            }
                            return builder
                                    .method(ElementMatchers.nameContainsIgnoreCase(methodName))
                                    .intercept(MethodDelegation.withDefaultConfiguration()
                                            .to(new InstMethodsInter(interceptorClassName, classLoader), fieldName))
                                    ;
                        }
                )
                .with(new AgentBuilder.Listener.Adapter() {
                    @Override
                    public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
                        System.err.println(String.format("Transform Error: typeName: %s, classLoader: %s, module: %s, loaded: %s", typeName, classLoader, module, loaded));
                        throwable.printStackTrace();
                    }
                })
                .installOn(ByteBuddyAgent.install());

        callBizFoo(1);
        // check interceptors
        // checkMethodInterceptor(SAY_HELLO_METHOD, 1);
        checkMethodInterceptor(SAY_HELLO_METHOD, 2);
        checkErrors();
    }

    private void enableClassDump() {
        System.setProperty("net.bytebuddy.dump", dumpFolder);
        File dumpDir = new File(dumpFolder);
        FileUtils.deleteDirectory(dumpDir);
        dumpDir.mkdirs();
    }
}
