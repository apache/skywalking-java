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

package org.apache.skywalking.apm.agent.bytebuddy.case1;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.SWAgentBuilderDefault;
import net.bytebuddy.agent.builder.SWAsmVisitorWrapper;
import net.bytebuddy.agent.builder.SWNativeMethodStrategy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.SWAuxiliaryTypeNamingStrategy;
import net.bytebuddy.implementation.SWImplementationContextFactory;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import org.apache.skywalking.apm.agent.bytebuddy.ConstructorAdvice;
import org.apache.skywalking.apm.agent.bytebuddy.ConstructorInter;
import org.apache.skywalking.apm.agent.bytebuddy.EnhanceHelper;
import org.apache.skywalking.apm.agent.bytebuddy.InstMethodAdvice;
import org.apache.skywalking.apm.agent.bytebuddy.InstMethodsInter;
import org.apache.skywalking.apm.agent.bytebuddy.Log;
import org.apache.skywalking.apm.agent.bytebuddy.SWClassFileLocator;
import org.apache.skywalking.apm.agent.bytebuddy.biz.BizFoo;
import org.junit.Assert;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.List;

public class AbstractInterceptTest {
    public static final String BIZ_FOO_CLASS_NAME = "org.apache.skywalking.apm.agent.bytebuddy.biz.BizFoo";
    public static final String PROJECT_SERVICE_CLASS_NAME = "org.apache.skywalking.apm.agent.bytebuddy.biz.ProjectService";
    public static final String DOC_SERVICE_CLASS_NAME = "org.apache.skywalking.apm.agent.bytebuddy.biz.DocService";
    public static final String SAY_HELLO_METHOD = "sayHello";
    public static final int BASE_INT_VALUE = 100;
    public static final String CONSTRUCTOR_INTERCEPTOR_CLASS = "constructorInterceptorClass";
    public static final String METHOD_INTERCEPTOR_CLASS = "methodInterceptorClass";
    protected List<String> nameTraits = Arrays.asList("sw2023", "sw2024");
    protected boolean deleteDuplicatedFields = false;

    protected static void callBizFoo(int round) {
        Log.info("-------------");
        Log.info("callBizFoo: " + round);
        // load target class
        int intResult = new BizFoo().sayHello(BASE_INT_VALUE);
        Log.info(intResult);

        String result = new BizFoo("Smith").sayHello("Joe");
        Log.info(result);

        Assert.assertEquals("Int value is unexpected", BASE_INT_VALUE + round, intResult);
        Assert.assertEquals("String value is unexpected", "Hello to John from Smith", result);
    }

    protected static void checkMethodInterceptor(String method, int round) {
        List<String> interceptors = EnhanceHelper.getInterceptors();
        String interceptorName = METHOD_INTERCEPTOR_CLASS + "$" + method + "$" + round;
        Assert.assertTrue("Not found interceptor: " + interceptorName, interceptors.contains(interceptorName));
        Log.info("Found interceptor: " + interceptorName);
    }

    protected static void checkConstructorInterceptor(int round) {
        List<String> interceptors = EnhanceHelper.getInterceptors();
        String interceptorName = CONSTRUCTOR_INTERCEPTOR_CLASS + "$" + round;
        Assert.assertTrue("Not found interceptor: " + interceptorName, interceptors.contains(interceptorName));
        Log.info("Found interceptor: " + interceptorName);
    }

    protected void installMethodInterceptor(String className, String methodName, int round) {
        this.installMethodInterceptor1(className, methodName, round);
//        this.installMethodInterceptor2(className, methodName, round);
    }

    protected void installMethodInterceptor1(String className, String methodName, int round) {
        String interceptorClassName = METHOD_INTERCEPTOR_CLASS + "$" + methodName + "$" + round;
        String nameTrait = getNameTrait(round);
        String fieldName = nameTrait + "_delegate$" + methodName + round;

        newAgentBuilder(nameTrait).type(ElementMatchers.named(className))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) -> {
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
                        System.err.println(String.format("Transform Error: interceptorClassName: %s, typeName: %s, classLoader: %s, module: %s, loaded: %s", interceptorClassName, typeName, classLoader, module, loaded));
                        throwable.printStackTrace();
                    }
                })
                .installOn(ByteBuddyAgent.install());
    }

    protected void installMethodInterceptor2(String className, String methodName, int round) {
        String interceptorClassName = METHOD_INTERCEPTOR_CLASS + "$" + methodName + "$" + round;
        String nameTrait = getNameTrait(round);
        String fieldName = nameTrait + "_delegate$" + methodName + round;

        AgentBuilder agentBuilder = newAgentBuilder(nameTrait);
        agentBuilder.type(ElementMatchers.named(className))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) -> {
                            if (deleteDuplicatedFields) {
                                builder = builder.visit(new SWAsmVisitorWrapper());
                            }
                            return builder
                                    .method(ElementMatchers.nameContainsIgnoreCase(methodName))
                                    .intercept(Advice.to(InstMethodAdvice.class));
                        }
                )
                .with(new AgentBuilder.Listener.Adapter() {
                    @Override
                    public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
                        System.err.println(String.format("Transform Error: interceptorClassName: %s, typeName: %s, classLoader: %s, module: %s, loaded: %s", interceptorClassName, typeName, classLoader, module, loaded));
                        throwable.printStackTrace();
                    }
                })
                .installOn(ByteBuddyAgent.install());
    }

    protected void installConstructorInterceptor(String className, int round) {
        installConstructorInterceptor1(className, round);
//        installConstructorInterceptor2(className, round);
    }

    protected void installConstructorInterceptor1(String className, int round) {
        String interceptorClassName = CONSTRUCTOR_INTERCEPTOR_CLASS + "$" + round;
        String nameTrait = getNameTrait(round);
        String fieldName = nameTrait + "_delegate$constructor" + round;

        AgentBuilder agentBuilder = newAgentBuilder(nameTrait);
        agentBuilder.type(ElementMatchers.named(className))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) -> {
                            if (deleteDuplicatedFields) {
                                builder = builder.visit(new SWAsmVisitorWrapper());
                            }
                            return builder
                                    .constructor(ElementMatchers.any())
                                    .intercept(SuperMethodCall.INSTANCE.andThen(
                                            MethodDelegation.withDefaultConfiguration().to(
                                                    new ConstructorInter(interceptorClassName, classLoader), fieldName)
                                    ));
                        }
                )
                .with(new AgentBuilder.Listener.Adapter() {
                    @Override
                    public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
                        System.err.println(String.format("Transform Error: interceptorClass:%s, typeName: %s, classLoader: %s, module: %s, loaded: %s", interceptorClassName, typeName, classLoader, module, loaded));
                        throwable.printStackTrace();
                    }
                })
                .installOn(ByteBuddyAgent.install());
    }

    protected void installConstructorInterceptor2(String className, int round) {
        String interceptorClassName = CONSTRUCTOR_INTERCEPTOR_CLASS + "$" + round;
        String nameTrait = getNameTrait(round);
        String fieldName = nameTrait + "_delegate$constructor" + round;

        AgentBuilder agentBuilder = newAgentBuilder(nameTrait);
        agentBuilder.type(ElementMatchers.named(className))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) -> {
                            if (deleteDuplicatedFields) {
                                builder = builder.visit(new SWAsmVisitorWrapper());
                            }
                            return builder
                                    .constructor(ElementMatchers.any())
                                    .intercept(Advice.to(ConstructorAdvice.class));
                        }
                )
                .with(new AgentBuilder.Listener.Adapter() {
                    @Override
                    public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
                        System.err.println(String.format("Transform Error: interceptorClass:%s, typeName: %s, classLoader: %s, module: %s, loaded: %s", interceptorClassName, typeName, classLoader, module, loaded));
                        throwable.printStackTrace();
                    }
                })
                .installOn(ByteBuddyAgent.install());
    }

    protected String getNameTrait(int round) {
        return nameTraits.get(round - 1);
    }

    protected AgentBuilder newAgentBuilder(String nameTrait) {
        ByteBuddy byteBuddy = new ByteBuddy()
                .with(new SWAuxiliaryTypeNamingStrategy(nameTrait))
                .with(new SWImplementationContextFactory(nameTrait))
                ;

        return new SWAgentBuilderDefault(byteBuddy, new SWNativeMethodStrategy(nameTrait))
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(AgentBuilder.DescriptionStrategy.Default.POOL_FIRST)
                .with(new SWClassFileLocator(ByteBuddyAgent.install(), getClassLoader()));
    }

    private static ClassLoader getClassLoader() {
        return AbstractInterceptTest.class.getClassLoader();
    }

    protected void installTraceClassTransformer(String msg) {
        ClassFileTransformer classFileTransformer = new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                if (className.endsWith("BizFoo") || className.endsWith("ProjectService") || className.endsWith("DocService")) {
                    Log.error(msg + className);
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    ClassReader cr = new ClassReader(classfileBuffer);
                    cr.accept(new TraceClassVisitor(new PrintWriter(outputStream)), ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                    Log.error(outputStream.toString());
                }
                return null;
            }
        };
        ByteBuddyAgent.install().addTransformer(classFileTransformer, true);
    }
}
