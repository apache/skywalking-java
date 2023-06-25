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

package net.bytebuddy.agent.builder;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.matcher.ElementMatchers;

import java.util.Collections;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.isBootstrapClassLoader;
import static net.bytebuddy.matcher.ElementMatchers.isExtensionClassLoader;
import static net.bytebuddy.matcher.ElementMatchers.isSynthetic;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.not;

/**
 * A custom AgentBuilder.Default for changing NativeMethodStrategy
 */
public class SWAgentBuilderDefault extends AgentBuilder.Default {

    /**
     * The default circularity lock that assures that no agent created by any agent builder within this
     * class loader causes a class loading circularity.
     */
    private static final CircularityLock DEFAULT_LOCK = new CircularityLock.Default();

    public SWAgentBuilderDefault(ByteBuddy byteBuddy, NativeMethodStrategy nativeMethodStrategy) {
        this(byteBuddy,
                Listener.NoOp.INSTANCE,
                DEFAULT_LOCK,
                PoolStrategy.Default.FAST,
                TypeStrategy.Default.REBASE,
                LocationStrategy.ForClassLoader.STRONG,
                ClassFileLocator.NoOp.INSTANCE,
                nativeMethodStrategy,
                WarmupStrategy.NoOp.INSTANCE,
                TransformerDecorator.NoOp.INSTANCE,
                new InitializationStrategy.SelfInjection.Split(),
                RedefinitionStrategy.DISABLED,
                RedefinitionStrategy.DiscoveryStrategy.SinglePass.INSTANCE,
                RedefinitionStrategy.BatchAllocator.ForTotal.INSTANCE,
                RedefinitionStrategy.Listener.NoOp.INSTANCE,
                RedefinitionStrategy.ResubmissionStrategy.Disabled.INSTANCE,
                InjectionStrategy.UsingReflection.INSTANCE,
                LambdaInstrumentationStrategy.DISABLED,
                DescriptionStrategy.Default.HYBRID,
                FallbackStrategy.ByThrowableType.ofOptionalTypes(),
                ClassFileBufferStrategy.Default.RETAINING,
                InstallationListener.NoOp.INSTANCE,
                new RawMatcher.Disjunction(
                        new RawMatcher.ForElementMatchers(any(), isBootstrapClassLoader().or(isExtensionClassLoader())),
                        new RawMatcher.ForElementMatchers(nameStartsWith("net.bytebuddy.")
                                .and(not(ElementMatchers.nameStartsWith(NamingStrategy.BYTE_BUDDY_RENAME_PACKAGE + ".")))
                                .or(nameStartsWith("sun.reflect.").or(nameStartsWith("jdk.internal.reflect.")))
                                .<TypeDescription>or(isSynthetic()))),
                Collections.<Transformation>emptyList());
    }

    protected SWAgentBuilderDefault(ByteBuddy byteBuddy, Listener listener, CircularityLock circularityLock, PoolStrategy poolStrategy, TypeStrategy typeStrategy, LocationStrategy locationStrategy, ClassFileLocator classFileLocator, NativeMethodStrategy nativeMethodStrategy, WarmupStrategy warmupStrategy, TransformerDecorator transformerDecorator, InitializationStrategy initializationStrategy, RedefinitionStrategy redefinitionStrategy, RedefinitionStrategy.DiscoveryStrategy redefinitionDiscoveryStrategy, RedefinitionStrategy.BatchAllocator redefinitionBatchAllocator, RedefinitionStrategy.Listener redefinitionListener, RedefinitionStrategy.ResubmissionStrategy redefinitionResubmissionStrategy, InjectionStrategy injectionStrategy, LambdaInstrumentationStrategy lambdaInstrumentationStrategy, DescriptionStrategy descriptionStrategy, FallbackStrategy fallbackStrategy, ClassFileBufferStrategy classFileBufferStrategy, InstallationListener installationListener, RawMatcher ignoreMatcher, List<Transformation> transformations) {
        super(byteBuddy, listener, circularityLock, poolStrategy, typeStrategy, locationStrategy, classFileLocator, nativeMethodStrategy, warmupStrategy, transformerDecorator, initializationStrategy, redefinitionStrategy, redefinitionDiscoveryStrategy, redefinitionBatchAllocator, redefinitionListener, redefinitionResubmissionStrategy, injectionStrategy, lambdaInstrumentationStrategy, descriptionStrategy, fallbackStrategy, classFileBufferStrategy, installationListener, ignoreMatcher, transformations);
    }

}
